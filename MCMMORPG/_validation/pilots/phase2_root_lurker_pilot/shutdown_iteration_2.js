#!/usr/bin/env node

'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { StreamableHttpMcpClient, loadConfiguredEndpoints } = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const EVIDENCE = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const CONFIG = path.join(REPO_ROOT, '.mcp.json');

function writeJson(name, value) {
  fs.writeFileSync(path.join(EVIDENCE, name), `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
}

function projectFrom(value) {
  return value?.project || value?.state || value;
}

async function main() {
  const endpoint = loadConfiguredEndpoints(CONFIG).find((entry) => entry.name === 'blockbench-ashfox');
  assert(endpoint?.url === 'http://127.0.0.1:8788/mcp');
  const mcp = new StreamableHttpMcpClient(endpoint, { timeoutMs: 30000 });
  const tools = await mcp.request('tools/list', {});
  const capabilities = await mcp.callTool('list_capabilities', {});
  const state = projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
  assert(state.active && state.name === 'phase2_root_lurker_pilot');
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: state.revision });
  assert.deepStrictEqual(validation.findings || [], []);
  writeJson('iteration_2_final_live_check.json', {
    endpoint: endpoint.url,
    toolCount: tools.tools?.length,
    toolRegistryHash: capabilities.toolRegistryHash,
    ashfoxVersion: capabilities.ashfoxVersion,
    blockbenchVersion: capabilities.blockbenchVersion,
    protocolVersion: capabilities.protocolVersion,
    project: state,
    validation,
  });
  const closed = await mcp.callTool('ensure_project', {
    action: 'delete',
    target: { name: state.name },
    force: true,
    ifRevision: state.revision,
  });
  writeJson('iteration_2_close_project.json', closed);
  console.log(JSON.stringify({ status: 'ASHFOX_PROJECT_CLOSED', revision: state.revision, findings: validation.findings || [] }, null, 2));
}

main().catch((error) => {
  console.error(error.stack || error.message || String(error));
  process.exitCode = 1;
});
