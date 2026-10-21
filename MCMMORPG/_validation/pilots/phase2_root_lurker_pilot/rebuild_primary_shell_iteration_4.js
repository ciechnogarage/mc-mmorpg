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
const LIVE = path.join(EVIDENCE, 'live_supplemental/iteration_4/checkpoint');
const EXPECTED_SOURCE_SHA = '609d35b4bf4c9af45b72e7611565ce828e754fecd6576f952c90278395f4d798';
const PROJECT = 'phase2_root_lurker_pilot';

function sha256(filePath) { return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex'); }
function writeJson(name, value) { fs.writeFileSync(path.join(EVIDENCE, name), `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 }); }
function projectFrom(value) { return value?.project || value?.state || value; }
function endpoint() {
  const found = loadConfiguredEndpoints(path.join(REPO_ROOT, '.mcp.json')).find((entry) => entry.name === 'blockbench-ashfox');
  assert(found?.url === 'http://127.0.0.1:8788/mcp');
  return found;
}
async function state(mcp) { return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true })); }
async function mutate(mcp, tool, args) {
  const current = await state(mcp);
  assert(current.active && current.name === PROJECT, `Refusing ${tool} on ${current.name}.`);
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

// Full primary-shell remap. Gaps are deliberate along Z: mask->shoulder, shoulder->ribs, ribs->hips.
const SHELL = [
  ['mask_core', [-2.7, 7.7, -15.0], [2.5, 12.8, -9.4], [-0.1, 10.5, -11.4], [-13, -9, -4]],
  ['mask_brow', [-3.2, 10.1, -13.7], [2.1, 13.8, -9.3], [-0.3, 11.8, -11.2], [7, -13, -8]],
  ['snout_lower', [-1.7, 6.9, -19.3], [1.5, 10.0, -13.9], [-0.1, 8.8, -15.1], [11, -10, -5]],
  ['attack_side_root_hook', [-3.9, 7.4, -19.8], [-1.7, 10.2, -14.1], [-2.4, 8.8, -15.2], [9, -18, 12]],

  ['shoulder_core', [-4.4, 8.8, -5.6], [4.1, 15.3, -1.0], [-0.1, 11.8, -3.0], [-4, -3, -3]],
  ['shoulder_shelf', [-5.0, 12.8, -4.5], [4.6, 17.2, -0.5], [-0.2, 14.6, -2.3], [1, -5, -7]],
  ['left_shoulder_plate', [3.3, 9.8, -6.4], [5.1, 15.6, -2.0], [4.0, 12.4, -3.6], [-4, -11, -13]],
  ['right_shoulder_plate', [-4.9, 9.2, -5.3], [-3.1, 14.4, -1.1], [-4.0, 11.7, -3.0], [5, 9, 10]],

  ['rib_taper', [-2.0, 7.3, 0.7], [1.8, 11.6, 8.3], [-0.1, 9.2, 4.5], [-3, 3, 1]],
  ['belly_root', [-1.15, 5.8, 1.6], [1.1, 7.9, 7.7], [-0.1, 7.0, 4.6], [2, -2, -1]],

  ['hip_core', [-2.9, 7.5, 9.4], [2.7, 12.9, 14.0], [-0.1, 10.0, 11.6], [-5, 3, 2]],
  ['left_haunch', [2.4, 8.0, 9.9], [4.0, 13.1, 14.1], [3.1, 10.3, 11.6], [2, -5, -10]],
  ['right_haunch', [-4.0, 7.8, 9.6], [-2.4, 12.5, 13.8], [-3.1, 9.9, 11.4], [-3, 5, 9]],

  ['tail_root_mass', [-2.2, 7.0, 14.5], [2.2, 10.5, 22.0], [0, 9.3, 15.3], [12, 0, 0]],
  ['tail_mid_mass', [-1.75, 4.2, 21.4], [1.75, 7.9, 29.6], [0, 6.8, 22.2], [16, 0, 0]],
];

const COLORS = {
  mask_core: '#1d1512', mask_brow: '#2a1d16', snout_lower: '#15100e', attack_side_root_hook: '#21140f',
  shoulder_core: '#291d17', shoulder_shelf: '#34241a', left_shoulder_plate: '#201713', right_shoulder_plate: '#1b1411',
  rib_taper: '#241a15', belly_root: '#17120f', hip_core: '#281d17', left_haunch: '#2d2018', right_haunch: '#211814',
  tail_root_mass: '#251a15', tail_mid_mass: '#1e1612',
};

async function paintCube(mcp, textureName, cubeName, color) {
  await mutate(mcp, 'paint_faces', {
    textureName, target: { cubeName }, coordSpace: 'face', mapping: 'stretch',
    op: { op: 'fill_rect', x: 0, y: 0, width: 64, height: 64, color, shade: { enabled: true, intensity: 0.12, edge: 0.06, noise: 0, lightDir: 'top_bottom' } },
  });
}

async function main() {
  assert.strictEqual(sha256(SOURCE), EXPECTED_SOURCE_SHA, 'Revision 3 source identity changed before rebuild.');
  const mcp = new StreamableHttpMcpClient(endpoint(), { timeoutMs: 30000 });
  const health = await mcp.connect();
  const tools = await mcp.request('tools/list', {});
  const capabilities = await mcp.callTool('list_capabilities', {});
  assert.strictEqual(health.serverInfo?.version, '0.2.0');
  assert.strictEqual(capabilities.blockbenchVersion, '5.1.6');
  assert.strictEqual(capabilities.toolRegistry.hash, '8043388b');
  let current = await state(mcp);
  assert(current.active && current.name === PROJECT);
  assert.deepStrictEqual(current.counts, { bones: 21, cubes: 29, meshes: 0, meshVertices: 0, meshFaces: 0, textures: 1, animations: 2 });
  writeJson('iteration_4_live_tools.json', tools);
  writeJson('iteration_4_live_capabilities.json', capabilities);
  writeJson('iteration_4_preflight.json', current);
  await mcp.callTool('ensure_project', {
    action: 'ensure', format: 'Generic Model', name: PROJECT,
    match: 'format_and_name', onMismatch: 'error', onMissing: 'error', uvPixelsPerBlock: 4,
    ifRevision: current.revision, includeState: true,
  });

  for (const [name, from, to, origin, rotation] of SHELL) await mutate(mcp, 'update_cube', { name, from, to, origin, rotation });
  current = await state(mcp);
  const textureName = current.textures[0].name;
  for (const [cubeName, color] of Object.entries(COLORS)) await paintCube(mcp, textureName, cubeName, color);
  // Restrained non-glow corruption mark on the single attack-side break.
  await mutate(mcp, 'paint_faces', {
    textureName, target: { cubeName: 'attack_side_root_hook', face: 'north' }, coordSpace: 'face', mapping: 'stretch',
    op: { op: 'fill_rect', x: 0, y: 0, width: 64, height: 64, color: '#657048', shade: false },
  });

  current = await state(mcp);
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: current.revision });
  assert.deepStrictEqual(validation.findings || [], []);
  writeJson('iteration_4_checkpoint_validate.json', validation);
  const renders = [];
  for (const [id, angle] of [['front', [-8, 0, 0]], ['side', [-8, 90, 0]], ['back', [-8, 180, 0]], ['three_quarter', [-10, 35, 0]]]) renders.push(await capture(mcp, id, angle));
  const rawTexture = await mcp.request('tools/call', { name: 'read_texture', arguments: { id: current.textures[0].id } });
  png(rawTexture, TEXTURE);
  writeJson('iteration_4_checkpoint.json', {
    status: 'PRIMARY_SHELL_REBUILD_CHECKPOINT',
    previousSourceSha256: EXPECTED_SOURCE_SHA,
    changedAxis: 'primary_shell_read',
    rebuiltCubeNames: SHELL.map((entry) => entry[0]),
    frozen: ['rig_names', 'bone_hierarchy', 'pivots', 'hidden_hitbox', 'player_scale_envelope', 'idle_clip_data', 'lunge_clip_data', 'limb_chain_geometry'],
    project: await state(mcp), validation, renders,
    texturePolicy: 'diagnostic_dark_bark_with_one_restrained_non_glow_corruption_accent_no_polish',
  });
  console.log(JSON.stringify({ status: 'PRIMARY_SHELL_REBUILD_CHECKPOINT', revision: (await state(mcp)).revision, renders }, null, 2));
  await mcp.close();
}

main().catch((error) => { console.error(error.stack || error.message || String(error)); process.exitCode = 1; });
