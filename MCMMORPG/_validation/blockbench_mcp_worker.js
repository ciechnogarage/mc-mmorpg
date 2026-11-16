#!/usr/bin/env node

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const PREFERRED_PROTOCOL_VERSIONS = ['2025-11-25', '2025-06-18', '2025-03-26', '2024-11-05'];
const CORE_TOOLS = ['list_capabilities', 'ensure_project', 'get_project_state', 'validate', 'render_preview', 'export'];
const COLLECTIONS = ['bones', 'cubes', 'meshes', 'textures', 'animations'];

class ToolCallError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'ToolCallError';
    this.details = details;
    this.code = details.code;
  }
}

class WorkerFailure extends Error {
  constructor(status, message, details = {}) {
    super(message);
    this.name = 'WorkerFailure';
    this.status = status;
    this.details = details;
  }
}

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonical(value[key])]));
  }
  return value;
}

function digest(value) {
  return crypto.createHash('sha256').update(JSON.stringify(canonical(value))).digest('hex');
}

function safeId(value, field) {
  if (typeof value !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/.test(value)) {
    throw new WorkerFailure('NEEDS_INPUT', `${field} must be a non-empty filesystem-safe identifier.`);
  }
  return value;
}

function atomicWriteJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  const tempPath = `${filePath}.${process.pid}.${crypto.randomUUID()}.tmp`;
  fs.writeFileSync(tempPath, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
  fs.renameSync(tempPath, filePath);
}

function loadConfiguredEndpoints(configPath) {
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  const servers = config.mcpServers || {};
  const endpoints = Object.entries(servers)
    .filter(([, entry]) => entry && entry.type === 'http' && typeof entry.url === 'string')
    .map(([name, entry]) => {
      const url = new URL(entry.url);
      const loopbackHosts = new Set(['localhost', '127.0.0.1', '[::1]', '::1']);
      if (url.protocol !== 'http:' || !loopbackHosts.has(url.hostname) || url.username || url.password) {
        throw new WorkerFailure('NEEDS_INPUT', `MCP endpoint ${name} must be unauthenticated loopback HTTP.`);
      }
      return { name, url: url.toString() };
    });
  const priority = (name) => name.includes('ashfox') ? 0 : name.includes('jj') ? 1 : 2;
  return endpoints.sort((left, right) => priority(left.name) - priority(right.name));
}

function parseSse(text) {
  const messages = [];
  let data = [];
  for (const line of text.split(/\r?\n/)) {
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
    if (line === '' && data.length) {
      messages.push(JSON.parse(data.join('\n')));
      data = [];
    }
  }
  if (data.length) messages.push(JSON.parse(data.join('\n')));
  return messages.at(-1) || null;
}

function parseResponseBody(contentType, text) {
  if (!text.trim()) return null;
  return contentType.includes('text/event-stream') ? parseSse(text) : JSON.parse(text);
}

function toolResultData(result) {
  if (result?.isError) {
    const details = result.structuredContent || {};
    const text = result.content?.find((entry) => entry.type === 'text')?.text;
    throw new ToolCallError(details.message || text || 'MCP tool call failed.', details);
  }
  if (result?.structuredContent !== undefined) return result.structuredContent;
  const text = result?.content?.find((entry) => entry.type === 'text')?.text;
  if (text) {
    try {
      return JSON.parse(text);
    } catch {
      return { text };
    }
  }
  return result;
}

class StreamableHttpMcpClient {
  constructor(endpoint, { fetchImpl = globalThis.fetch, timeoutMs = 5000 } = {}) {
    this.endpointName = endpoint.name;
    this.url = endpoint.url;
    this.fetchImpl = fetchImpl;
    this.timeoutMs = timeoutMs;
    this.sessionId = null;
    this.protocolVersion = null;
    this.tools = [];
    this.nextId = 1;
  }

  async request(method, params, { notification = false } = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);
    const body = { jsonrpc: '2.0', method };
    if (!notification) body.id = this.nextId++;
    if (params !== undefined) body.params = params;
    const headers = {
      'Content-Type': 'application/json',
      Accept: 'application/json, text/event-stream',
    };
    if (this.sessionId) headers['Mcp-Session-Id'] = this.sessionId;
    if (this.protocolVersion) headers['MCP-Protocol-Version'] = this.protocolVersion;
    try {
      const response = await this.fetchImpl(this.url, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
        signal: controller.signal,
        redirect: 'error',
      });
      const sessionId = response.headers.get('mcp-session-id');
      if (sessionId) this.sessionId = sessionId;
      const text = await response.text();
      const message = parseResponseBody(response.headers.get('content-type') || '', text);
      if (!response.ok) {
        const remote = message?.error || message || {};
        throw new ToolCallError(remote.message || `MCP HTTP ${response.status}`, {
          ...remote,
          httpStatus: response.status,
        });
      }
      if (message?.error) throw new ToolCallError(message.error.message, message.error);
      return message?.result ?? null;
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new ToolCallError(`MCP request timed out after ${this.timeoutMs}ms.`, { code: 'timeout' });
      }
      throw error;
    } finally {
      clearTimeout(timer);
    }
  }

  async connect() {
    let initialized = null;
    let lastProtocolError = null;
    for (const protocolVersion of PREFERRED_PROTOCOL_VERSIONS) {
      this.sessionId = null;
      this.protocolVersion = null;
      try {
        initialized = await this.request('initialize', {
          protocolVersion,
          capabilities: {},
          clientInfo: { name: 'mc-transactional-blockbench-worker', version: '1.1.0' },
        });
        this.protocolVersion = initialized?.protocolVersion || protocolVersion;
        break;
      } catch (error) {
        const unsupported = error instanceof ToolCallError
          && /unsupported protocol version/i.test(error.message);
        if (!unsupported) throw error;
        lastProtocolError = error;
      }
    }
    if (!initialized) throw lastProtocolError || new ToolCallError('MCP protocol negotiation failed.');
    if (!this.sessionId) {
      throw new ToolCallError('MCP initialize did not return Mcp-Session-Id.', { code: 'session_missing' });
    }
    await this.request('notifications/initialized', undefined, { notification: true });
    const listed = await this.request('tools/list', {});
    this.tools = Array.isArray(listed?.tools) ? listed.tools : [];
    return {
      endpointName: this.endpointName,
      protocolVersion: this.protocolVersion,
      serverInfo: initialized?.serverInfo || null,
      sessionId: this.sessionId,
      tools: this.tools,
    };
  }

  async callTool(name, args = {}) {
    return toolResultData(await this.request('tools/call', { name, arguments: args }));
  }

  async close() {
    if (!this.sessionId) return;
    try {
      await this.fetchImpl(this.url, {
        method: 'DELETE',
        headers: {
          'Mcp-Session-Id': this.sessionId,
          'MCP-Protocol-Version': this.protocolVersion,
        },
      });
    } catch {
      // Session cleanup is best-effort; the server TTL remains the fallback.
    }
  }
}

function schemaHasProperty(tool, property) {
  return Boolean(tool?.inputSchema?.properties && Object.hasOwn(tool.inputSchema.properties, property));
}

async function discoverMcpEndpoint(endpoints, options = {}) {
  const failures = [];
  for (const endpoint of endpoints) {
    const client = options.clientFactory
      ? options.clientFactory(endpoint)
      : new StreamableHttpMcpClient(endpoint, options);
    try {
      const health = await client.connect();
      const names = new Set(health.tools.map((entry) => entry.name));
      const missing = (options.requiredTools || []).filter((name) => !names.has(name));
      const missingRevision = (options.revisionTools || []).filter((name) => {
        const definition = health.tools.find((entry) => entry.name === name);
        return !schemaHasProperty(definition, 'ifRevision');
      });
      if (missing.length || missingRevision.length) {
        failures.push({ endpoint: endpoint.name, missing, missingRevision });
        await client.close();
        continue;
      }
      return { client, health };
    } catch (error) {
      failures.push({ endpoint: endpoint.name, error: error.message, code: error.code });
      await client.close();
    }
  }
  throw new WorkerFailure('BLOCKED_TOOLING', 'No configured Blockbench MCP endpoint passed health/session discovery.', {
    failures,
  });
}

class OperationJournal {
  constructor(filePath, context) {
    this.filePath = filePath;
    this.context = context;
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    this.entries = this.load();
  }

  load() {
    if (!fs.existsSync(this.filePath)) return [];
    return fs.readFileSync(this.filePath, 'utf8')
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => JSON.parse(line));
  }

  readAll() {
    return [...this.entries];
  }

  append(event, data = {}) {
    const previous = this.entries.at(-1);
    const entry = {
      sequence: (previous?.sequence || 0) + 1,
      timestamp: new Date().toISOString(),
      job_id: this.context.jobId,
      spec_revision_id: this.context.specRevisionId,
      mcp_session_id: this.context.mcpSessionId || null,
      event,
      ...data,
      previous_hash: previous?.entry_hash || null,
    };
    entry.entry_hash = digest(entry);
    fs.appendFileSync(this.filePath, `${JSON.stringify(entry)}\n`, { mode: 0o600 });
    this.entries.push(entry);
    return entry;
  }
}

class ProjectLock {
  constructor(filePath, jobId) {
    this.filePath = filePath;
    this.jobId = jobId;
    this.owned = false;
  }

  acquire() {
    fs.mkdirSync(path.dirname(this.filePath), { recursive: true });
    for (let attempt = 0; attempt < 2; attempt += 1) {
      try {
        const descriptor = fs.openSync(this.filePath, 'wx', 0o600);
        fs.writeFileSync(descriptor, JSON.stringify({ job_id: this.jobId, pid: process.pid }));
        fs.closeSync(descriptor);
        this.owned = true;
        return;
      } catch (error) {
        if (error.code !== 'EEXIST') throw error;
        let existing = {};
        try { existing = JSON.parse(fs.readFileSync(this.filePath, 'utf8')); } catch {}
        let alive = false;
        if (Number.isInteger(existing.pid)) {
          try { process.kill(existing.pid, 0); alive = true; } catch (probe) { alive = probe.code === 'EPERM'; }
        }
        if (alive) {
          throw new WorkerFailure('BLOCKED_TOOLING', `Project already has active job ${existing.job_id || 'unknown'}.`);
        }
        fs.rmSync(this.filePath, { force: true });
      }
    }
    throw new WorkerFailure('BLOCKED_TOOLING', 'Could not acquire the project job lock.');
  }

  release() {
    if (this.owned) fs.rmSync(this.filePath, { force: true });
    this.owned = false;
  }
}

function projectFrom(value) {
  const project = value?.project || value?.state || value;
  if (!project || typeof project !== 'object' || typeof project.revision !== 'string') {
    throw new WorkerFailure('BLOCKED_TOOLING', 'get_project_state returned no usable project revision.');
  }
  return project;
}

function assertUniqueProjectEntities(project) {
  for (const collection of COLLECTIONS) {
    const entries = Array.isArray(project[collection]) ? project[collection] : [];
    for (const key of ['id', 'uuid']) {
      const values = entries.map((entry) => entry?.[key]).filter(Boolean);
      if (new Set(values).size !== values.length) {
        throw new WorkerFailure('BLOCKED_TOOLING', `Project contains duplicate ${collection} ${key} values.`);
      }
    }
    const names = entries.map((entry) => entry?.name).filter(Boolean);
    if (new Set(names).size !== names.length) {
      throw new WorkerFailure('BLOCKED_TOOLING', `Project contains duplicate ${collection} names.`);
    }
  }
}

function expectationState(project, expect) {
  if (!expect) return 'unknown';
  const entries = Array.isArray(project[expect.collection]) ? project[expect.collection] : [];
  const matches = entries.filter((entry) => {
    if (expect.id !== undefined && entry.id !== expect.id && entry.uuid !== expect.id) return false;
    if (expect.name !== undefined && entry.name !== expect.name) return false;
    return true;
  });
  if (matches.length > 1) return 'ambiguous';
  return matches.length === 1 ? 'present' : 'absent';
}

function mutationDigest(operation) {
  return digest({ tool: operation.tool, arguments: operation.arguments, expect: operation.expect, inverse: operation.inverse });
}

function flattenOperations(job) {
  const operations = [];
  const ids = new Set();
  for (const batch of job.batches || []) {
    safeId(batch.batchId, 'batchId');
    for (const operation of batch.operations || []) {
      safeId(operation.operationId, 'operationId');
      if (ids.has(operation.operationId)) {
        throw new WorkerFailure('NEEDS_INPUT', `Duplicate operationId: ${operation.operationId}`);
      }
      ids.add(operation.operationId);
      if (!operation.tool || !operation.inverse?.tool || !operation.expect?.collection) {
        throw new WorkerFailure('NEEDS_INPUT', `Operation ${operation.operationId} requires tool, expect, and inverse.`);
      }
      if (!COLLECTIONS.includes(operation.expect.collection)) {
        throw new WorkerFailure('NEEDS_INPUT', `Operation ${operation.operationId} has unsupported expectation collection.`);
      }
      operations.push({ ...operation, batchId: batch.batchId, digest: mutationDigest(operation) });
      if (operations.length > 1000) {
        throw new WorkerFailure('NEEDS_INPUT', 'A Blockbench MCP job may contain at most 1000 operations.');
      }
    }
  }
  return operations;
}

function comparableProject(project) {
  const selected = {};
  for (const collection of COLLECTIONS) {
    selected[collection] = [...(project[collection] || [])].sort((left, right) => {
      const leftKey = left.id || left.uuid || left.name || '';
      const rightKey = right.id || right.uuid || right.name || '';
      return leftKey.localeCompare(rightKey);
    });
  }
  return canonical(selected);
}

class TransactionalBlockbenchWorker {
  constructor(options = {}) {
    this.client = options.client || null;
    this.clientFactory = options.clientFactory;
    this.configPath = options.configPath || path.resolve(__dirname, '..', '..', '.mcp.json');
    this.stateDir = options.stateDir || path.join(__dirname, 'runs', 'blockbench-mcp');
    this.timeoutMs = options.timeoutMs || 5000;
    this.journal = null;
    this.currentRevision = null;
  }

  async connect(requiredTools, revisionTools) {
    if (this.client) {
      const health = await this.client.connect();
      return { client: this.client, health };
    }
    const endpoints = loadConfiguredEndpoints(this.configPath);
    if (!endpoints.length) throw new WorkerFailure('BLOCKED_TOOLING', 'No HTTP MCP endpoints are configured.');
    const discovered = await discoverMcpEndpoint(endpoints, {
      clientFactory: this.clientFactory,
      timeoutMs: this.timeoutMs,
      requiredTools,
      revisionTools,
    });
    this.client = discovered.client;
    return discovered;
  }

  validateToolContract(tools, requiredTools, revisionTools) {
    const byName = new Map(tools.map((entry) => [entry.name, entry]));
    const missing = requiredTools.filter((name) => !byName.has(name));
    const missingRevision = revisionTools.filter((name) => !schemaHasProperty(byName.get(name), 'ifRevision'));
    if (missing.length || missingRevision.length) {
      throw new WorkerFailure('NEEDS_INPUT', 'Detected MCP does not support the requested transactional operation set.', {
        missing_tools: missing,
        tools_without_ifRevision: missingRevision,
      });
    }
  }

  async readProject() {
    const project = projectFrom(await this.client.callTool('get_project_state', { detail: 'full', includeUsage: true }));
    assertUniqueProjectEntities(project);
    this.currentRevision = project.revision;
    return project;
  }

  async callMutation(toolName, args, operationId) {
    const execute = () => this.client.callTool(toolName, { ...args, ifRevision: this.currentRevision });
    try {
      return await execute();
    } catch (error) {
      const code = error.code || error.details?.code;
      if (code !== 'invalid_state_revision_mismatch') throw error;
      const before = this.currentRevision;
      await this.readProject();
      this.journal.append('revision_recovered', {
        operation_id: operationId,
        previous_revision: before,
        recovered_revision: this.currentRevision,
      });
      return execute();
    }
  }

  writeCheckpoint(checkpointPath, job, project) {
    if (fs.existsSync(checkpointPath)) {
      const checkpoint = JSON.parse(fs.readFileSync(checkpointPath, 'utf8'));
      if (checkpoint.spec_revision_id !== job.specRevisionId) {
        throw new WorkerFailure('NEEDS_INPUT', 'Existing checkpoint belongs to a different spec_revision_id.');
      }
      return checkpoint;
    }
    const checkpoint = {
      schema_version: 1,
      job_id: job.jobId,
      spec_revision_id: job.specRevisionId,
      mcp_session_id: this.client.sessionId,
      created_at: new Date().toISOString(),
      project: structuredClone(project),
      project_hash: digest(comparableProject(project)),
    };
    atomicWriteJson(checkpointPath, checkpoint);
    this.journal.append('clone_checkpoint_created', {
      checkpoint_path: checkpointPath,
      checkpoint_revision: project.revision,
      checkpoint_hash: checkpoint.project_hash,
    });
    return checkpoint;
  }

  priorOperation(entries, operationId) {
    return entries.filter((entry) => entry.operation_id === operationId && entry.operation_digest).at(-1);
  }

  async applyOperation(operation) {
    const project = await this.readProject();
    const state = expectationState(project, operation.expect);
    if (state === 'ambiguous') {
      throw new WorkerFailure('BLOCKED_TOOLING', `Operation ${operation.operationId} reconciled to multiple entities.`);
    }
    const prior = this.priorOperation(this.journal.readAll(), operation.operationId);
    if (prior && prior.operation_digest !== operation.digest) {
      throw new WorkerFailure('NEEDS_INPUT', `operationId ${operation.operationId} was reused with a different payload.`);
    }
    if (state === 'present') {
      this.journal.append('operation_skipped_idempotent', {
        batch_id: operation.batchId,
        operation_id: operation.operationId,
        operation_digest: operation.digest,
        revision: project.revision,
      });
      return;
    }

    this.journal.append('operation_started', {
      batch_id: operation.batchId,
      operation_id: operation.operationId,
      operation_digest: operation.digest,
      tool: operation.tool,
      revision: project.revision,
    });
    try {
      await this.callMutation(operation.tool, operation.arguments || {}, operation.operationId);
    } catch (error) {
      const reconciled = await this.readProject();
      const reconciledState = expectationState(reconciled, operation.expect);
      if (reconciledState === 'present') {
        this.journal.append('operation_reconciled', {
          batch_id: operation.batchId,
          operation_id: operation.operationId,
          operation_digest: operation.digest,
          revision: reconciled.revision,
          after_error: error.message,
        });
        return;
      }
      throw error;
    }
    const reconciled = await this.readProject();
    if (expectationState(reconciled, operation.expect) !== 'present') {
      throw new WorkerFailure('BLOCKED_TOOLING', `Operation ${operation.operationId} returned success without its expected state.`);
    }
    this.journal.append('operation_applied', {
      batch_id: operation.batchId,
      operation_id: operation.operationId,
      operation_digest: operation.digest,
      tool: operation.tool,
      revision: reconciled.revision,
    });
  }

  async restoreCheckpoint(checkpoint, operations) {
    this.journal.append('restore_started', { checkpoint_hash: checkpoint.project_hash });
    for (const operation of [...operations].reverse()) {
      const project = await this.readProject();
      const now = expectationState(project, operation.expect);
      const before = expectationState(checkpoint.project, operation.expect);
      if (now === before) continue;
      if (now !== 'present' || before !== 'absent') {
        throw new WorkerFailure('BLOCKED_TOOLING', `Cannot deterministically restore operation ${operation.operationId}.`);
      }
      await this.callMutation(operation.inverse.tool, operation.inverse.arguments || {}, `restore:${operation.operationId}`);
      const restored = await this.readProject();
      if (expectationState(restored, operation.expect) !== 'absent') {
        throw new WorkerFailure('BLOCKED_TOOLING', `Inverse for ${operation.operationId} did not restore checkpoint state.`);
      }
      this.journal.append('operation_restored', {
        operation_id: operation.operationId,
        inverse_tool: operation.inverse.tool,
        revision: restored.revision,
      });
    }
    const project = await this.readProject();
    if (digest(comparableProject(project)) !== checkpoint.project_hash) {
      throw new WorkerFailure('BLOCKED_TOOLING', 'Restore completed but project state does not match the clone checkpoint.');
    }
    this.journal.append('restore_completed', { revision: project.revision, checkpoint_hash: checkpoint.project_hash });
    return true;
  }

  async run(job) {
    safeId(job.jobId, 'jobId');
    safeId(job.specRevisionId, 'specRevisionId');
    safeId(job.project?.name, 'project.name');
    const operations = flattenOperations(job);
    const requiredTools = [...new Set([
      ...CORE_TOOLS,
      ...operations.flatMap((operation) => [operation.tool, operation.inverse.tool]),
    ])];
    const revisionTools = [...new Set([
      ...operations.flatMap((operation) => [operation.tool, operation.inverse.tool]),
      'validate',
      'export',
    ])];
    const jobDir = path.join(this.stateDir, 'jobs', job.jobId);
    const checkpointPath = path.join(jobDir, 'checkpoint.json');
    const lock = new ProjectLock(path.join(this.stateDir, 'locks', `${job.project.name}.lock`), job.jobId);
    let checkpoint = null;
    let restored = false;
    let journal = null;
    try {
      lock.acquire();
      const { health } = await this.connect(requiredTools, revisionTools);
      this.validateToolContract(health.tools || this.client.tools || [], requiredTools, revisionTools);
      journal = new OperationJournal(path.join(jobDir, 'operations.jsonl'), {
        jobId: job.jobId,
        specRevisionId: job.specRevisionId,
        mcpSessionId: this.client.sessionId,
      });
      this.journal = journal;
      journal.append('job_started', {
        endpoint: health.endpointName || this.client.endpointName,
        protocol_version: health.protocolVersion || null,
      });
      await this.client.callTool('list_capabilities', {});
      let project = await this.readProject();
      if (!project.active || project.name !== job.project.name) {
        await this.client.callTool('ensure_project', {
          action: 'ensure',
          target: { name: job.project.name },
          name: job.project.name,
          match: 'name',
          onMismatch: 'error',
          onMissing: 'error',
        });
        project = await this.readProject();
      }
      if (!project.active || project.name !== job.project.name) {
        throw new WorkerFailure('NEEDS_INPUT', `Expected active Blockbench project ${job.project.name}.`);
      }
      checkpoint = this.writeCheckpoint(checkpointPath, job, project);

      for (const batch of job.batches || []) {
        journal.append('batch_started', { batch_id: batch.batchId });
        for (const operation of operations.filter((entry) => entry.batchId === batch.batchId)) {
          await this.applyOperation(operation);
        }
        journal.append('batch_committed', { batch_id: batch.batchId, revision: this.currentRevision });
      }

      project = await this.readProject();
      await this.client.callTool('validate', { ...(job.validate || {}), ifRevision: project.revision });
      journal.append('validation_completed', { revision: project.revision });
      await this.client.callTool('render_preview', job.render || { mode: 'fixed', output: 'single' });
      journal.append('render_completed', { revision: project.revision });
      await this.callMutation('export', job.export || {}, 'export');
      project = await this.readProject();
      journal.append('export_completed', { revision: project.revision, dest_path: job.export?.destPath || null });
      journal.append('job_completed', { status: 'COMPLETED', revision: project.revision });
      return {
        status: 'COMPLETED',
        job_id: job.jobId,
        spec_revision_id: job.specRevisionId,
        mcp_session_id: this.client.sessionId,
        endpoint: health.endpointName || this.client.endpointName,
        journal_path: journal.filePath,
        checkpoint_path: checkpointPath,
      };
    } catch (error) {
      const status = error.status || 'BLOCKED_TOOLING';
      if (journal) journal.append('job_failed', { status, error: error.message, details: error.details || null });
      if (checkpoint && journal) {
        try {
          restored = await this.restoreCheckpoint(checkpoint, operations);
        } catch (restoreError) {
          journal.append('restore_failed', { error: restoreError.message, details: restoreError.details || null });
          return {
            status: 'BLOCKED_TOOLING',
            restored: false,
            error: error.message,
            restore_error: restoreError.message,
            job_id: job.jobId,
            spec_revision_id: job.specRevisionId,
            journal_path: journal.filePath,
            checkpoint_path: checkpointPath,
          };
        }
      }
      return {
        status,
        restored,
        error: error.message,
        details: error.details || null,
        job_id: job.jobId,
        spec_revision_id: job.specRevisionId,
        mcp_session_id: this.client?.sessionId || null,
        journal_path: journal?.filePath || null,
        checkpoint_path: checkpoint ? checkpointPath : null,
      };
    } finally {
      lock.release();
      if (this.client) await this.client.close();
    }
  }
}

async function healthConfiguredMcp(configPath, options = {}) {
  const endpoints = loadConfiguredEndpoints(configPath);
  const discovered = await discoverMcpEndpoint(endpoints, options);
  const result = {
    status: 'OK',
    endpoint: discovered.health.endpointName,
    protocol_version: discovered.health.protocolVersion,
    mcp_session_id: discovered.health.sessionId,
    tool_count: discovered.health.tools.length,
  };
  await discovered.client.close();
  return result;
}

async function main() {
  const command = process.argv[2] || 'health';
  if (command !== 'health') throw new WorkerFailure('NEEDS_INPUT', `Unknown command: ${command}`);
  const configPath = path.resolve(process.argv[3] || path.resolve(__dirname, '..', '..', '.mcp.json'));
  try {
    console.log(JSON.stringify(await healthConfiguredMcp(configPath), null, 2));
  } catch (error) {
    console.log(JSON.stringify({
      status: error.status || 'BLOCKED_TOOLING',
      error: error.message,
      details: error.details || null,
    }, null, 2));
    process.exitCode = 2;
  }
}

if (require.main === module) main();

module.exports = {
  OperationJournal,
  StreamableHttpMcpClient,
  ToolCallError,
  TransactionalBlockbenchWorker,
  WorkerFailure,
  discoverMcpEndpoint,
  healthConfiguredMcp,
  loadConfiguredEndpoints,
};
