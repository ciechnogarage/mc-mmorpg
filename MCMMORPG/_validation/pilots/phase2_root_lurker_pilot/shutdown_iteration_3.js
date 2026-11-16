#!/usr/bin/env node

'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { StreamableHttpMcpClient, loadConfiguredEndpoints } = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const EVIDENCE = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');

function projectFrom(value) { return value?.project || value?.state || value; }
function writeJson(name, value) { fs.writeFileSync(path.join(EVIDENCE, name), `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 }); }

async function main() {
  const endpoint = loadConfiguredEndpoints(path.join(REPO_ROOT, '.mcp.json')).find((entry) => entry.name === 'blockbench-ashfox');
  assert(endpoint?.url === 'http://127.0.0.1:8788/mcp');
  const mcp = new StreamableHttpMcpClient(endpoint, { timeoutMs: 30000 });
  const health = await mcp.connect();
  const tools = await mcp.request('tools/list', {});
  const capabilities = await mcp.callTool('list_capabilities', {});
  const current = projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
  assert(current.active && current.name === 'phase2_root_lurker_pilot');
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: current.revision });
  assert.deepStrictEqual(validation.findings || [], []);
  writeJson('iteration_3_final_live_check.json', {
    endpoint: endpoint.url,
    protocolVersion: health.protocolVersion,
    toolCount: tools.tools?.length,
    toolRegistryHash: capabilities.toolRegistry.hash,
    ashfoxVersion: health.serverInfo?.version,
    blockbenchVersion: capabilities.blockbenchVersion,
    project: current,
    validation,
  });
  const closed = await mcp.callTool('ensure_project', { action: 'delete', target: { name: current.name }, force: true, ifRevision: current.revision });
  writeJson('iteration_3_close_project.json', closed);
  console.log(JSON.stringify({ status: 'ASHFOX_PROJECT_CLOSED_REVISION_3', revision: current.revision, findings: validation.findings || [] }, null, 2));
  await mcp.close();
}

main().catch((error) => { console.error(error.stack || error.message || String(error)); process.exitCode = 1; });
