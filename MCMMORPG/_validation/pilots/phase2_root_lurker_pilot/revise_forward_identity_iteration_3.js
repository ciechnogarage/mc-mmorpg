#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { StreamableHttpMcpClient, loadConfiguredEndpoints } = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const CONFIG_PATH = path.join(REPO_ROOT, '.mcp.json');
const SOURCE_PATH = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE_PATH = path.join(__dirname, 'phase2_root_lurker_pilot.png');
const EVIDENCE_ROOT = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const LIVE_ROOT = path.join(EVIDENCE_ROOT, 'live_supplemental', 'iteration_3', 'checkpoint');
const EXPECTED_SOURCE_SHA256 = '83838dac643a6c55ff630a4fb131b26299cb6577cc3ebcfea1b8ac9fd3d931a3';
const PROJECT_NAME = 'phase2_root_lurker_pilot';

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
}

function writeJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
}

function endpoint() {
  const found = loadConfiguredEndpoints(CONFIG_PATH).find((entry) => entry.name === 'blockbench-ashfox');
  assert(found?.url === 'http://127.0.0.1:8788/mcp');
  return found;
}

function projectFrom(value) {
  return value?.project || value?.state || value;
}

async function state(mcp) {
  return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
}

async function mutate(mcp, tool, args) {
  const current = await state(mcp);
  assert(current.active && current.name === PROJECT_NAME, `Refusing ${tool} on ${current.name}.`);
  return mcp.callTool(tool, { ...args, ifRevision: current.revision });
}

function pngFromResult(raw, filePath) {
  const block = raw?.content?.find((entry) => entry.type === 'image' && entry.data);
  assert(block?.mimeType === 'image/png', `Missing PNG content for ${filePath}.`);
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
}

async function capture(mcp, id, angle) {
  const current = await state(mcp);
  const raw = await mcp.request('tools/call', {
    name: 'render_preview',
    arguments: { mode: 'fixed', output: 'single', angle, includeState: true, ifRevision: current.revision },
  });
  const filePath = path.join(LIVE_ROOT, `${id}.png`);
  pngFromResult(raw, filePath);
  return { id, path: path.relative(REPO_ROOT, filePath), sha256: sha256(filePath), revision: current.revision };
}

const GEOMETRY = [
  // The compressed shoulder mass stays dominant, but its slight cant breaks the old rectangular front/back read.
  ['shoulder_core', [-5.9, 8.6, -5.8], [6.3, 17, 0.5], [0, 12.5, -2], [0, -3, -2]],
  ['shoulder_shelf', [-6.8, 13, -5.8], [6.4, 19.2, 0.2], [0, 15.5, -2], [0, -3, -5]],
  // Head masses end ahead of the shoulder front, leaving a real head-neck notch.
  ['mask_core', [-3.0, 8.4, -14.6], [3.1, 14.1, -8.2], [0, 11.5, -9.3], [-9, -7, -2]],
  ['mask_brow', [-3.7, 11.2, -13.5], [2.8, 15.2, -8.1], [-0.3, 13, -9.2], [5, -9, -5]],
  ['snout_lower', [-1.8, 7.9, -19.1], [2.0, 11.0, -13.2], [0, 10, -14.2], [8, -8, -3]],
  // Exactly one separate attack-side break remains on the negative-X side.
  ['attack_side_root_hook', [-4.5, 8.9, -19.7], [-1.5, 12.0, -13.4], [-2.4, 10.2, -14.2], [7, -16, 11]],
];

const BARK_PLANES = {
  shoulder_core: '#261c17',
  shoulder_shelf: '#302219',
  left_shoulder_plate: '#1b1512',
  right_shoulder_plate: '#1b1512',
  mask_core: '#211713',
  mask_brow: '#35251b',
  snout_lower: '#17110f',
  attack_side_root_hook: '#241611',
};

async function paintWholeCube(mcp, textureName, cubeName, color) {
  await mutate(mcp, 'paint_faces', {
    textureName,
    target: { cubeName },
    coordSpace: 'face',
    op: {
      op: 'fill_rect', x: 0, y: 0, width: 64, height: 64, color,
      shade: { enabled: true, intensity: 0.16, edge: 0.08, noise: 0, lightDir: 'top_bottom' },
    },
    mapping: 'stretch',
  });
}

async function paintFace(mcp, textureName, cubeName, face, color) {
  await mutate(mcp, 'paint_faces', {
    textureName,
    target: { cubeName, face },
    coordSpace: 'face',
    op: { op: 'fill_rect', x: 0, y: 0, width: 64, height: 64, color, shade: false },
    mapping: 'stretch',
  });
}

async function main() {
  assert.strictEqual(sha256(SOURCE_PATH), EXPECTED_SOURCE_SHA256, 'Revision 2 source identity changed before revision 3 checkpoint.');
  const mcp = new StreamableHttpMcpClient(endpoint(), { timeoutMs: 30000 });
  const health = await mcp.connect();
  const tools = await mcp.request('tools/list', {});
  const capabilities = await mcp.callTool('list_capabilities', {});
  assert.strictEqual(health.serverInfo?.name, 'ashfox');
  assert.strictEqual(health.serverInfo?.version, '0.2.0');
  assert.strictEqual(capabilities.blockbenchVersion, '5.1.6');
  assert.strictEqual(capabilities.toolRegistry.hash, '8043388b');
  let current = await state(mcp);
  assert(current.active && current.name === PROJECT_NAME);
  assert.deepStrictEqual(current.counts, { bones: 21, cubes: 29, meshes: 0, meshVertices: 0, meshFaces: 0, textures: 1, animations: 2 });
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_3_live_tools.json'), tools);
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_3_live_capabilities.json'), capabilities);
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_3_preflight.json'), current);

  await mcp.callTool('ensure_project', {
    action: 'ensure', format: 'Generic Model', name: PROJECT_NAME,
    match: 'format_and_name', onMismatch: 'error', onMissing: 'error', uvPixelsPerBlock: 4,
    ifRevision: current.revision, includeState: true,
  });
  for (const [name, from, to, origin, rotation] of GEOMETRY) {
    await mutate(mcp, 'update_cube', { name, from, to, origin, rotation });
  }

  current = await state(mcp);
  const textureName = current.textures[0].name;
  for (const [cubeName, color] of Object.entries(BARK_PLANES)) {
    await paintWholeCube(mcp, textureName, cubeName, color);
  }
  // Secondary material only: two muted upper planes, no tertiary noise/detail.
  await paintFace(mcp, textureName, 'shoulder_shelf', 'up', '#566044');
  await paintFace(mcp, textureName, 'mask_brow', 'up', '#4b563d');
  // Sole highest-contrast corruption focal point at the one attack-side threat origin.
  await paintFace(mcp, textureName, 'attack_side_root_hook', 'north', '#c7df4b');

  current = await state(mcp);
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: current.revision });
  assert.deepStrictEqual(validation.findings || [], []);
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_3_checkpoint_validate.json'), validation);
  const renders = [];
  for (const [id, angle] of [['front', [-8, 0, 0]], ['side', [-8, 90, 0]], ['back', [-8, 180, 0]], ['three_quarter', [-10, 35, 0]]]) {
    renders.push(await capture(mcp, id, angle));
  }
  const textureRaw = await mcp.request('tools/call', { name: 'read_texture', arguments: { id: current.textures[0].id } });
  pngFromResult(textureRaw, TEXTURE_PATH);
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_3_checkpoint.json'), {
    status: 'CHECKPOINT_READY_FOR_LIVE_REVIEW',
    sourceIdentityOnDisk: EXPECTED_SOURCE_SHA256,
    changedAxis: 'forward_threat_identity_only',
    frozen: ['five_mass_side', 'player_scale', 'limb_chains', 'idle_clip', 'lunge_clip', 'rear_gap_contact'],
    project: await state(mcp),
    validation,
    renders,
    materialPlan: {
      primary: 'dark_bark_root_planes',
      secondary: 'two_muted_moss_upper_planes',
      focal: 'one_highest_contrast_corruption_face_on_attack_side_root_hook',
      tertiaryDetailAdded: false,
      pinkNosePresent: false,
    },
  });
  console.log(JSON.stringify({ status: 'CHECKPOINT_READY_FOR_LIVE_REVIEW', revision: (await state(mcp)).revision, renders }, null, 2));
  await mcp.close();
}

main().catch((error) => {
  console.error(error.stack || error.message || String(error));
  process.exitCode = 1;
});
