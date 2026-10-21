#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { StreamableHttpMcpClient, loadConfiguredEndpoints } = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const SOURCE = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const EVIDENCE = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const LIVE = path.join(EVIDENCE, 'live_supplemental/iteration_4/checkpoint_tuned');
const CHECKPOINT_SOURCE_SHA = '89625b49688d4f070467dc4bdd7512fb5ca8fdafbb82cd7a9abc9cae1f7103f7';

function sha256(filePath) { return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex'); }
function projectFrom(value) { return value?.project || value?.state || value; }
async function state(mcp) { return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true })); }
async function mutate(mcp, tool, args) {
  const current = await state(mcp);
  assert(current.active && current.name === 'phase2_root_lurker_pilot');
  return mcp.callTool(tool, { ...args, ifRevision: current.revision });
}
function png(raw, filePath) {
  const block = raw?.content?.find((entry) => entry.type === 'image' && entry.data);
  assert(block?.mimeType === 'image/png');
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
}
async function capture(mcp, id, angle) {
  const current = await state(mcp);
  const raw = await mcp.request('tools/call', { name: 'render_preview', arguments: { mode: 'fixed', output: 'single', angle, includeState: true, ifRevision: current.revision } });
  const filePath = path.join(LIVE, `${id}.png`);
  png(raw, filePath);
  return { id, path: path.relative(REPO_ROOT, filePath), sha256: sha256(filePath), revision: current.revision };
}

const TUNING = [
  ['shoulder_core', [-3.6, 8.8, -5.6], [3.5, 14.8, -1.0], [-0.1, 11.6, -3.0], [-4, -4, -4]],
  ['shoulder_shelf', [-4.0, 12.5, -4.5], [3.7, 16.6, -0.5], [-0.2, 14.1, -2.3], [1, -7, -9]],
  ['left_shoulder_plate', [3.0, 9.5, -6.4], [4.5, 14.7, -2.0], [3.7, 12.0, -3.6], [-4, -13, -15]],
  ['right_shoulder_plate', [-4.3, 8.9, -5.3], [-2.9, 13.9, -1.1], [-3.6, 11.3, -3.0], [5, 11, 12]],
];

async function main() {
  assert.strictEqual(sha256(SOURCE), CHECKPOINT_SOURCE_SHA, 'Checkpoint source identity changed before tuning.');
  const endpoint = loadConfiguredEndpoints(path.join(REPO_ROOT, '.mcp.json')).find((entry) => entry.name === 'blockbench-ashfox');
  assert(endpoint?.url === 'http://127.0.0.1:8788/mcp');
  const mcp = new StreamableHttpMcpClient(endpoint, { timeoutMs: 30000 });
  await mcp.connect();
  const initial = await state(mcp);
  assert(initial.active && initial.name === 'phase2_root_lurker_pilot');
  for (const [name, from, to, origin, rotation] of TUNING) await mutate(mcp, 'update_cube', { name, from, to, origin, rotation });
  const current = await state(mcp);
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: current.revision });
  assert.deepStrictEqual(validation.findings || [], []);
  const renders = [];
  for (const [id, angle] of [['front', [-8, 0, 0]], ['side', [-8, 90, 0]], ['back', [-8, 180, 0]], ['three_quarter', [-10, 35, 0]]]) renders.push(await capture(mcp, id, angle));
  fs.writeFileSync(path.join(EVIDENCE, 'iteration_4_checkpoint_tuned.json'), `${JSON.stringify({ status: 'PRIMARY_SHELL_TUNED_CHECKPOINT', previousCheckpointSourceSha256: CHECKPOINT_SOURCE_SHA, tunedCubeNames: TUNING.map((entry) => entry[0]), project: await state(mcp), validation, renders }, null, 2)}\n`, { mode: 0o600 });
  console.log(JSON.stringify({ status: 'PRIMARY_SHELL_TUNED_CHECKPOINT', revision: (await state(mcp)).revision, renders }, null, 2));
  await mcp.close();
}
main().catch((error) => { console.error(error.stack || error.message || String(error)); process.exitCode = 1; });
