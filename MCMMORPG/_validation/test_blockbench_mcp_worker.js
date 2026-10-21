const assert = require('assert');
const fs = require('fs');
const http = require('http');
const os = require('os');
const path = require('path');

const {
  OperationJournal,
  StreamableHttpMcpClient,
  TransactionalBlockbenchWorker,
  ToolCallError,
  loadConfiguredEndpoints,
} = require('./blockbench_mcp_worker');

const tmpDir = () => fs.mkdtempSync(path.join(os.tmpdir(), 'blockbench-mcp-worker-'));

function tool(name, mutating = false) {
  return {
    name,
    inputSchema: {
      type: 'object',
      properties: mutating ? { ifRevision: { type: 'string' } } : {},
    },
  };
}

class FakeMcpClient {
  constructor({ revisionMismatch = false, timeoutAfterApply = false, exportFailure = false } = {}) {
    this.sessionId = 'mcp-session-test';
    this.endpointName = 'blockbench-ashfox';
    this.tools = [
      tool('list_capabilities'),
      tool('ensure_project', true),
      tool('get_project_state'),
      tool('add_bone', true),
      tool('delete_bone', true),
      tool('validate', true),
      tool('render_preview'),
      tool('export', true),
    ];
    this.state = {
      project: {
        id: 'project-1',
        active: true,
        name: 'fixture',
        format: 'generic',
        revision: 'rev-1',
        bones: [],
        cubes: [],
        meshes: [],
        textures: [],
        animations: [],
      },
    };
    this.revisionMismatch = revisionMismatch;
    this.timeoutAfterApply = timeoutAfterApply;
    this.exportFailure = exportFailure;
    this.calls = [];
  }

  async connect() {
    return { sessionId: this.sessionId, tools: this.tools };
  }

  async close() {}

  bumpRevision() {
    const current = Number(this.state.project.revision.split('-')[1]);
    this.state.project.revision = `rev-${current + 1}`;
  }

  async callTool(name, args) {
    this.calls.push({ name, args: structuredClone(args) });
    if (name === 'list_capabilities') return { revisionGuard: true };
    if (name === 'get_project_state') return structuredClone(this.state);
    if (name === 'ensure_project') return structuredClone(this.state);
    if (name === 'add_bone') {
      if (this.revisionMismatch) {
        this.revisionMismatch = false;
        this.bumpRevision();
        throw new ToolCallError('Project revision mismatch', {
          code: 'invalid_state_revision_mismatch',
        });
      }
      assert.strictEqual(args.ifRevision, this.state.project.revision);
      if (!this.state.project.bones.some((bone) => bone.name === args.name)) {
        this.state.project.bones.push({ id: args.id, name: args.name, pivot: args.pivot });
        this.bumpRevision();
      }
      if (this.timeoutAfterApply) {
        this.timeoutAfterApply = false;
        throw new ToolCallError('Synthetic response timeout', { code: 'timeout' });
      }
      return { revision: this.state.project.revision };
    }
    if (name === 'delete_bone') {
      assert.strictEqual(args.ifRevision, this.state.project.revision);
      this.state.project.bones = this.state.project.bones.filter((bone) => bone.name !== args.name);
      this.bumpRevision();
      return { revision: this.state.project.revision };
    }
    if (name === 'validate') return { valid: true };
    if (name === 'render_preview') return { image: { mimeType: 'image/png' } };
    if (name === 'export') {
      if (this.exportFailure) throw new ToolCallError('Synthetic export failure', { code: 'io_error' });
      return { path: args.destPath };
    }
    throw new Error(`Unexpected tool: ${name}`);
  }
}

function job(overrides = {}) {
  return {
    jobId: 'job-fixture',
    specRevisionId: 'spec-rev-fixture',
    project: { name: 'fixture' },
    batches: [{
      batchId: 'rig',
      operations: [{
        operationId: 'add-root-bone',
        tool: 'add_bone',
        arguments: { id: 'bone-root', name: 'root', pivot: [0, 0, 0] },
        expect: { collection: 'bones', id: 'bone-root', name: 'root' },
        inverse: { tool: 'delete_bone', arguments: { name: 'root' } },
      }],
    }],
    validate: {},
    render: { mode: 'fixed', output: 'single' },
    export: { format: 'native_codec', codecId: 'project', destPath: '/tmp/fixture.bbmodel' },
    ...overrides,
  };
}

async function testEndpointOrdering() {
  const dir = tmpDir();
  const configPath = path.join(dir, '.mcp.json');
  fs.writeFileSync(configPath, JSON.stringify({
    mcpServers: {
      'blockbench-jj': { type: 'http', url: 'http://localhost:3000/bb-mcp' },
      'blockbench-ashfox': { type: 'http', url: 'http://127.0.0.1:8788/mcp' },
    },
  }));
  const endpoints = loadConfiguredEndpoints(configPath);
  assert.deepStrictEqual(endpoints.map((entry) => entry.name), ['blockbench-ashfox', 'blockbench-jj']);
}

async function testHandshakeAndToolsList() {
  let initialized = false;
  const protocolAttempts = [];
  const server = http.createServer(async (request, response) => {
    if (request.method === 'DELETE') {
      response.writeHead(204).end();
      return;
    }
    const chunks = [];
    for await (const chunk of request) chunks.push(chunk);
    const message = JSON.parse(Buffer.concat(chunks).toString('utf8'));
    if (message.method === 'initialize') {
      protocolAttempts.push(message.params.protocolVersion);
      assert.strictEqual(request.headers['mcp-protocol-version'], undefined);
      if (message.params.protocolVersion === '2025-11-25') {
        response.writeHead(400, { 'Content-Type': 'application/json' });
        response.end(JSON.stringify({
          jsonrpc: '2.0',
          id: message.id,
          error: { code: -32600, message: 'Unsupported protocol version: 2025-11-25' },
        }));
        return;
      }
      assert.strictEqual(message.params.protocolVersion, '2025-06-18');
      response.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Mcp-Session-Id': 'session-http-fixture',
      });
      response.end(`event: message\ndata: ${JSON.stringify({
        jsonrpc: '2.0',
        id: message.id,
        result: {
          protocolVersion: '2025-06-18',
          capabilities: { tools: {} },
          serverInfo: { name: 'fixture', version: '1.0.0' },
        },
      })}\n\n`);
      return;
    }
    assert.strictEqual(request.headers['mcp-session-id'], 'session-http-fixture');
    if (message.method === 'notifications/initialized') {
      initialized = true;
      response.writeHead(202).end();
      return;
    }
    if (message.method === 'tools/list') {
      assert.strictEqual(initialized, true);
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({
        jsonrpc: '2.0',
        id: message.id,
        result: { tools: [tool('get_project_state')] },
      }));
      return;
    }
    response.writeHead(404).end();
  });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  const client = new StreamableHttpMcpClient({
    name: 'fixture',
    url: `http://127.0.0.1:${address.port}/mcp`,
  });
  try {
    const health = await client.connect();
    assert.strictEqual(health.sessionId, 'session-http-fixture');
    assert.strictEqual(health.protocolVersion, '2025-06-18');
    assert.deepStrictEqual(protocolAttempts, ['2025-11-25', '2025-06-18']);
    assert.deepStrictEqual(health.tools.map((entry) => entry.name), ['get_project_state']);
  } finally {
    await client.close();
    await new Promise((resolve) => server.close(resolve));
  }
}

async function testRevisionRecoveryAndIdempotentResume() {
  const stateDir = tmpDir();
  const client = new FakeMcpClient({ revisionMismatch: true });
  const worker = new TransactionalBlockbenchWorker({ client, stateDir });
  const first = await worker.run(job());
  assert.strictEqual(first.status, 'COMPLETED');
  assert.strictEqual(client.state.project.bones.length, 1);
  assert.strictEqual(client.calls.filter((call) => call.name === 'add_bone').length, 2);

  const resumed = await new TransactionalBlockbenchWorker({ client, stateDir }).run(job());
  assert.strictEqual(resumed.status, 'COMPLETED');
  assert.strictEqual(client.state.project.bones.length, 1, 'resume must not duplicate a bone');
  assert.strictEqual(new Set(client.state.project.bones.map((bone) => bone.id)).size, 1);
  assert.strictEqual(client.calls.filter((call) => call.name === 'add_bone').length, 2);

  const journal = new OperationJournal(path.join(stateDir, 'jobs', 'job-fixture', 'operations.jsonl'), {
    jobId: 'job-fixture',
    specRevisionId: 'spec-rev-fixture',
  });
  const entries = journal.readAll();
  assert(entries.length > 0);
  assert(entries.every((entry) => entry.job_id === 'job-fixture'));
  assert(entries.every((entry) => entry.spec_revision_id === 'spec-rev-fixture'));
  assert(entries.some((entry) => entry.event === 'revision_recovered'));
  assert(entries.some((entry) => entry.event === 'operation_skipped_idempotent'));
}

async function testTimeoutAfterMutationReconcilesWithoutDuplicate() {
  const stateDir = tmpDir();
  const client = new FakeMcpClient({ timeoutAfterApply: true });
  const result = await new TransactionalBlockbenchWorker({ client, stateDir }).run(job());
  assert.strictEqual(result.status, 'COMPLETED');
  assert.strictEqual(client.state.project.bones.length, 1);
  const journal = new OperationJournal(path.join(stateDir, 'jobs', 'job-fixture', 'operations.jsonl'), {
    jobId: 'job-fixture',
    specRevisionId: 'spec-rev-fixture',
  });
  assert(journal.readAll().some((entry) => entry.event === 'operation_reconciled'));
}

async function testExportFailureRestoresCheckpointAndReleasesLock() {
  const stateDir = tmpDir();
  const client = new FakeMcpClient({ exportFailure: true });
  const result = await new TransactionalBlockbenchWorker({ client, stateDir }).run(job());
  assert.strictEqual(result.status, 'BLOCKED_TOOLING');
  assert.strictEqual(result.restored, true);
  assert.deepStrictEqual(client.state.project.bones, []);
  assert.strictEqual(fs.existsSync(path.join(stateDir, 'locks', 'fixture.lock')), false);

  const checkpoint = JSON.parse(fs.readFileSync(
    path.join(stateDir, 'jobs', 'job-fixture', 'checkpoint.json'),
    'utf8',
  ));
  assert.strictEqual(checkpoint.spec_revision_id, 'spec-rev-fixture');
  assert.deepStrictEqual(checkpoint.project.bones, []);
}

async function main() {
  await testEndpointOrdering();
  await testHandshakeAndToolsList();
  await testRevisionRecoveryAndIdempotentResume();
  await testTimeoutAfterMutationReconcilesWithoutDuplicate();
  await testExportFailureRestoresCheckpointAndReleasesLock();
  console.log('BLOCKBENCH_MCP_WORKER_TEST_PASS: handshake, journal, resume, revision recovery, and restore verified.');
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
