#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { StreamableHttpMcpClient, loadConfiguredEndpoints } = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const SOURCE = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE = path.join(__dirname, 'phase2_root_lurker_pilot.png');
const EVIDENCE = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');

function sha256(filePath) { return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex'); }
function projectFrom(value) { return value?.project || value?.state || value; }
function png(raw, filePath) {
  const block = raw?.content?.find((entry) => entry.type === 'image' && entry.data);
  assert(block?.mimeType === 'image/png');
  fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
}
async function main() {
  const endpoint = loadConfiguredEndpoints(path.join(REPO_ROOT, '.mcp.json')).find((entry) => entry.name === 'blockbench-ashfox');
  assert(endpoint?.url === 'http://127.0.0.1:8788/mcp');
  const mcp = new StreamableHttpMcpClient(endpoint, { timeoutMs: 30000 });
  await mcp.connect();
  const state = projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
  assert(state.active && state.name === 'phase2_root_lurker_pilot');
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: state.revision });
  assert.deepStrictEqual(validation.findings || [], []);
  const textureRaw = await mcp.request('tools/call', { name: 'read_texture', arguments: { id: state.textures[0].id } });
  png(textureRaw, TEXTURE);
  const exported = await mcp.callTool('export', {
    format: 'native_codec', codecId: 'project', destPath: SOURCE,
    options: { fallback: 'strict', includeDiagnostics: true }, includeState: true, ifRevision: state.revision,
  });
  const receipt = { status: 'PRIMARY_SHELL_CHECKPOINT_NATIVE_EXPORTED', sourceSha256: sha256(SOURCE), textureSha256: sha256(TEXTURE), validation, export: exported };
  fs.writeFileSync(path.join(EVIDENCE, 'iteration_4_checkpoint_native_export.json'), `${JSON.stringify(receipt, null, 2)}\n`, { mode: 0o600 });
  console.log(JSON.stringify({ status: receipt.status, sourceSha256: receipt.sourceSha256, revision: exported.revision }, null, 2));
  await mcp.close();
}
main().catch((error) => { console.error(error.stack || error.message || String(error)); process.exitCode = 1; });
