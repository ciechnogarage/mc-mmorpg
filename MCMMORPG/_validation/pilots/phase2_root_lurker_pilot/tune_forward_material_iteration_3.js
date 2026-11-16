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
const LIVE = path.join(EVIDENCE, 'live_supplemental/iteration_3/checkpoint_tuned');
const SOURCE_SHA = '83838dac643a6c55ff630a4fb131b26299cb6577cc3ebcfea1b8ac9fd3d931a3';

function sha256(filePath) { return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex'); }
function projectFrom(value) { return value?.project || value?.state || value; }
function png(raw, filePath) {
  const block = raw?.content?.find((entry) => entry.type === 'image' && entry.data);
  assert(block?.mimeType === 'image/png');
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
}
async function state(mcp) { return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true })); }
async function mutate(mcp, tool, args) {
  const current = await state(mcp);
  assert(current.active && current.name === 'phase2_root_lurker_pilot');
  return mcp.callTool(tool, { ...args, ifRevision: current.revision });
}
async function paint(mcp, textureName, cubeName, face, op) {
  await mutate(mcp, 'paint_faces', { textureName, target: { cubeName, face }, coordSpace: 'face', op, mapping: 'stretch' });
}
async function capture(mcp, id, angle) {
  const current = await state(mcp);
  const raw = await mcp.request('tools/call', { name: 'render_preview', arguments: { mode: 'fixed', output: 'single', angle, includeState: true, ifRevision: current.revision } });
  const filePath = path.join(LIVE, `${id}.png`);
  png(raw, filePath);
  return { id, path: path.relative(REPO_ROOT, filePath), sha256: sha256(filePath), revision: current.revision };
}
async function main() {
  assert.strictEqual(sha256(SOURCE), SOURCE_SHA);
  const endpoint = loadConfiguredEndpoints(path.join(REPO_ROOT, '.mcp.json')).find((entry) => entry.name === 'blockbench-ashfox');
  assert(endpoint?.url === 'http://127.0.0.1:8788/mcp');
  const mcp = new StreamableHttpMcpClient(endpoint, { timeoutMs: 30000 });
  await mcp.connect();
  const current = await state(mcp);
  assert(current.active && current.name === 'phase2_root_lurker_pilot');
  const textureName = current.textures[0].name;
  const base = (color) => ({ op: 'fill_rect', x: 0, y: 0, width: 64, height: 64, color, shade: false });
  await paint(mcp, textureName, 'shoulder_shelf', 'up', base('#302219'));
  await paint(mcp, textureName, 'shoulder_shelf', 'up', { op: 'set_pixel', x: 1, y: 0, color: '#455039' });
  await paint(mcp, textureName, 'mask_brow', 'up', base('#35251b'));
  await paint(mcp, textureName, 'mask_brow', 'up', { op: 'set_pixel', x: 0, y: 0, color: '#414a35' });
  const after = await state(mcp);
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: after.revision });
  assert.deepStrictEqual(validation.findings || [], []);
  const renders = [];
  for (const [id, angle] of [['front', [-8, 0, 0]], ['side', [-8, 90, 0]], ['back', [-8, 180, 0]], ['three_quarter', [-10, 35, 0]]]) renders.push(await capture(mcp, id, angle));
  const textureRaw = await mcp.request('tools/call', { name: 'read_texture', arguments: { id: after.textures[0].id } });
  png(textureRaw, TEXTURE);
  fs.writeFileSync(path.join(EVIDENCE, 'iteration_3_checkpoint_tuned.json'), `${JSON.stringify({ status: 'CHECKPOINT_TUNED', project: await state(mcp), validation, renders, moss: 'two_muted_single_pixel_accents', corruption: 'one_highest_contrast_attack_side_face' }, null, 2)}\n`, { mode: 0o600 });
  console.log(JSON.stringify({ status: 'CHECKPOINT_TUNED', renders }, null, 2));
  await mcp.close();
}
main().catch((error) => { console.error(error.stack || error.message || String(error)); process.exitCode = 1; });
