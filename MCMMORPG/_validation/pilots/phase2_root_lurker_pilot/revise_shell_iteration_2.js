#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const {
  StreamableHttpMcpClient,
  loadConfiguredEndpoints,
} = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const CONFIG_PATH = path.join(REPO_ROOT, '.mcp.json');
const SOURCE_PATH = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE_PATH = path.join(__dirname, 'phase2_root_lurker_pilot.png');
const EVIDENCE_ROOT = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const LIVE_ROOT = path.join(EVIDENCE_ROOT, 'live_supplemental', 'iteration_2');
const OLD_SOURCE_SHA256 = '66c8a0ed08aca929b0ce28d551bc304b8de72bd5171fa8c1e4eda76539708386';
const PROJECT_NAME = 'phase2_root_lurker_pilot';
const APPIMAGE_PATH = '/home/przemek/.local/opt/blockbench/5.1.6/Blockbench_5.1.6.AppImage';
const PROFILE_ROOT = '/home/przemek/.local/state/mc-ashfox-conformance';

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
}

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonical(value[key])]));
  }
  return value;
}

function hashJson(value) {
  return crypto.createHash('sha256').update(`${JSON.stringify(canonical(value))}\n`).digest('hex');
}

function writeJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
}

function projectFrom(value) {
  return value?.project || value?.state || value;
}

function endpoint() {
  const result = loadConfiguredEndpoints(CONFIG_PATH).find((entry) => entry.name === 'blockbench-ashfox');
  assert(result && result.url === 'http://127.0.0.1:8788/mcp');
  return result;
}

function client() {
  return new StreamableHttpMcpClient(endpoint(), { timeoutMs: 30000 });
}

async function state(mcp) {
  return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
}

async function mutate(mcp, tool, args) {
  const current = await state(mcp);
  assert(current.active && current.name === PROJECT_NAME, `Refusing ${tool} on ${current.name}.`);
  return mcp.callTool(tool, { ...args, ifRevision: current.revision });
}

function normalizeState(project) {
  const boneName = new Map((project.bones || []).map((bone) => [bone.id, bone.name]));
  const textureName = new Map((project.textures || []).map((texture) => [texture.id, texture.name]));
  const normalizeFace = (face) => canonical({
    ...face,
    texture: textureName.get(face?.texture) || (project.textures?.length === 1 ? project.textures[0].name : face?.texture),
  });
  return canonical({
    name: project.name,
    format: project.format,
    formatId: project.formatId ?? null,
    counts: project.counts,
    bones: (project.bones || []).map((bone) => ({
      name: bone.name,
      parent: boneName.get(bone.parent) || bone.parent || null,
      pivot: bone.pivot,
      rotation: bone.rotation,
      scale: bone.scale,
      visibility: bone.visibility,
    })).sort((a, b) => a.name.localeCompare(b.name)),
    cubes: (project.cubes || []).map((cube) => ({
      name: cube.name,
      bone: boneName.get(cube.bone) || cube.bone,
      from: cube.from,
      to: cube.to,
      origin: cube.origin,
      rotation: cube.rotation,
      inflate: cube.inflate,
      mirror: cube.mirror,
      visibility: cube.visibility,
      boxUv: cube.boxUv,
      faces: Object.fromEntries(Object.entries(cube.faces || {}).map(([key, face]) => [key, normalizeFace(face)])),
    })).sort((a, b) => a.name.localeCompare(b.name)),
    textures: (project.textures || []).map((texture) => ({ name: texture.name, width: texture.width, height: texture.height })),
    animations: (project.animations || []).map((animation) => ({
      name: animation.name,
      length: animation.length,
      loop: animation.loop,
      fps: animation.fps,
      channels: (animation.channels || []).map((channel) => ({
        bone: boneName.get(channel.bone) || channel.bone,
        channel: channel.channel,
        keys: (channel.keys || []).map((key) => ({ time: key.time, value: key.value, interp: key.interp })),
      })).sort((a, b) => `${a.bone}:${a.channel}`.localeCompare(`${b.bone}:${b.channel}`)),
      triggers: animation.triggers || [],
    })).sort((a, b) => a.name.localeCompare(b.name)),
  });
}

function differences(before, after, currentPath = '$', output = []) {
  if (output.length >= 300 || Object.is(before, after)) return output;
  if (Array.isArray(before) && Array.isArray(after)) {
    if (before.length !== after.length) output.push({ path: `${currentPath}.length`, before: before.length, after: after.length });
    for (let index = 0; index < Math.max(before.length, after.length); index += 1) {
      differences(before[index], after[index], `${currentPath}[${index}]`, output);
    }
    return output;
  }
  if (before && after && typeof before === 'object' && typeof after === 'object') {
    for (const key of [...new Set([...Object.keys(before), ...Object.keys(after)])].sort()) {
      differences(before[key], after[key], `${currentPath}.${key}`, output);
    }
    return output;
  }
  output.push({ path: currentPath, before: before ?? null, after: after ?? null });
  return output;
}

function pngFromResult(raw, filePath) {
  const image = raw?.content?.find((entry) => entry.type === 'image' && entry.data);
  assert(image?.mimeType === 'image/png');
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, Buffer.from(image.data, 'base64'), { mode: 0o600 });
}

async function capture(mcp, name, angle) {
  const current = await state(mcp);
  const raw = await mcp.request('tools/call', {
    name: 'render_preview',
    arguments: { mode: 'fixed', output: 'single', angle, includeState: true, ifRevision: current.revision },
  });
  const filePath = path.join(LIVE_ROOT, `${name}.png`);
  pngFromResult(raw, filePath);
  return { id: name, path: path.relative(REPO_ROOT, filePath), sha256: sha256(filePath), revision: current.revision };
}

async function saveTexture(mcp) {
  const current = await state(mcp);
  const raw = await mcp.request('tools/call', { name: 'read_texture', arguments: { id: current.textures[0].id } });
  pngFromResult(raw, TEXTURE_PATH);
}

function launchSource() {
  const child = spawn(APPIMAGE_PATH, [SOURCE_PATH], {
    env: {
      ...process.env,
      ASHFOX_HOST: '127.0.0.1',
      ASHFOX_PORT: '8788',
      ASHFOX_PATH: '/mcp',
      XDG_CONFIG_HOME: path.join(PROFILE_ROOT, 'xdg-config'),
      XDG_CACHE_HOME: path.join(PROFILE_ROOT, 'xdg-cache'),
      XDG_DATA_HOME: path.join(PROFILE_ROOT, 'xdg-data'),
    },
    detached: true,
    stdio: 'ignore',
  });
  child.unref();
}

async function waitForProject(mcp) {
  const deadline = Date.now() + 20000;
  while (Date.now() < deadline) {
    const current = await state(mcp);
    if (current.active && current.name === PROJECT_NAME) return current;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error('Timed out waiting for revision 2 native reopen.');
}

const CUBE_UPDATES = [
  ['mask_core', [-3.8, 8.2, -14], [3.8, 14.5, -6.5], [0, 11.5, -8], [-8, 0, 0]],
  ['mask_brow', [-4.5, 11.5, -12.5], [4.5, 15.5, -7], [0, 13, -8], [4, 0, 0]],
  ['snout_lower', [-2.4, 7.8, -18.5], [2.4, 11.2, -12.5], [0, 10, -14], [7, 0, 0]],
  ['attack_side_root_hook', [-3.8, 8.8, -19.5], [-1.2, 11.8, -13], [-2.2, 10.2, -14], [5, -12, 7]],
  ['shoulder_core', [-6, 8.5, -6], [6, 17, 0.5], [0, 12, -2], [-2, 0, 0]],
  ['shoulder_shelf', [-7.5, 13, -6], [7.5, 19.5, 0.5], [0, 14, -2], [-3, 0, 0]],
  ['left_shoulder_plate', [5, 9.5, -5.5], [7.4, 17, 0], [5.4, 13, -2], [0, 0, -12]],
  ['right_shoulder_plate', [-7.4, 9.5, -5.5], [-5, 17, 0], [-5.4, 13, -2], [0, 0, 12]],
  ['rib_taper', [-2.8, 8, 0.5], [2.8, 13, 9.5], [0, 10.5, 5], [3, 0, 0]],
  ['belly_root', [-1.8, 6.5, 1.5], [1.8, 9, 8.5], [0, 8, 5], [0, 0, 0]],
  ['hip_core', [-3.8, 8, 9], [3.8, 14.5, 14], [0, 10.5, 11], [-2, 0, 0]],
  ['left_haunch', [3.1, 8.5, 9.5], [4.9, 14, 14], [3.7, 10.5, 11], [0, 0, -8]],
  ['right_haunch', [-4.9, 8.5, 9.5], [-3.1, 14, 14], [-3.7, 10.5, 11], [0, 0, 8]],
  ['tail_root_mass', [-2.7, 7.2, 14], [2.7, 11, 22], [0, 9.5, 15], [11, 0, 0]],
  ['tail_mid_mass', [-2, 4.3, 21], [2, 8.2, 29.5], [0, 7, 22], [15, 0, 0]],
  ['tail_tip_root', [-1.35, 1.6, 28.5], [1.35, 5.2, 37], [0, 4.8, 29.5], [19, 0, 0]],
  ['left_front_upper_mass', [3.7, 5.2, -3.2], [6.7, 12.8, 0.6], [5.2, 11, -1.5], [-7, 0, -3]],
  ['left_front_lower_root', [4.1, 1.4, -2.4], [6.3, 6.4, 0.2], [5.2, 5.5, -1], [8, 0, 2]],
  ['left_front_contact', [3.6, 0, -4.2], [6.8, 2.1, 0], [5.2, 1.5, -1.5], [0, 0, -2]],
  ['right_front_upper_mass', [-6.7, 5.2, -3.2], [-3.7, 12.8, 0.6], [-5.2, 11, -1.5], [-7, 0, 3]],
  ['right_front_lower_root', [-6.3, 1.4, -2.4], [-4.1, 6.4, 0.2], [-5.2, 5.5, -1], [8, 0, -2]],
  ['right_front_contact', [-6.8, 0, -4.2], [-3.6, 2.1, 0], [-5.2, 1.5, -1.5], [0, 0, 2]],
  ['left_back_upper_mass', [3.1, 5, 10], [5.9, 11.5, 13.6], [4.5, 10, 11.5], [10, 0, -4]],
  ['left_back_lower_root', [3.4, 1.4, 10.6], [5.6, 6, 13.1], [4.5, 5.3, 12], [-10, 0, 3]],
  ['left_back_contact', [3, 0, 9], [6.2, 2.1, 13.2], [4.5, 1.5, 10.5], [0, 0, -2]],
  ['right_back_upper_mass', [-5.9, 5, 10], [-3.1, 11.5, 13.6], [-4.5, 10, 11.5], [10, 0, 4]],
  ['right_back_lower_root', [-5.6, 1.4, 10.6], [-3.4, 6, 13.1], [-4.5, 5.3, 12], [-10, 0, -3]],
  ['right_back_contact', [-6.2, 0, 9], [-3, 2.1, 13.2], [-4.5, 1.5, 10.5], [0, 0, 2]],
];

const BONE_PIVOTS = [
  ['head', [0, 11.5, -7]], ['threat_tip', [0, 10, -14]],
  ['left_front_upper', [5.2, 11, -1.5]], ['left_front_lower', [5.2, 5.5, -1]], ['left_front_foot', [5.2, 1.5, -1.5]],
  ['right_front_upper', [-5.2, 11, -1.5]], ['right_front_lower', [-5.2, 5.5, -1]], ['right_front_foot', [-5.2, 1.5, -1.5]],
  ['left_back_upper', [4.5, 10, 11.5]], ['left_back_lower', [4.5, 5.3, 12]], ['left_back_foot', [4.5, 1.5, 10.5]],
  ['right_back_upper', [-4.5, 10, 11.5]], ['right_back_lower', [-4.5, 5.3, 12]], ['right_back_foot', [-4.5, 1.5, 10.5]],
  ['tail_base', [0, 9.5, 15]], ['tail_mid', [0, 7, 22]], ['tail_tip', [0, 4.8, 29.5]],
];

const COLORS = {
  mask_core: '#30241d', mask_brow: '#3d2d22', snout_lower: '#2a1f1a', attack_side_root_hook: '#251b17',
  shoulder_core: '#3b2b22', shoulder_shelf: '#473328', left_shoulder_plate: '#2b211b', right_shoulder_plate: '#2b211b',
  rib_taper: '#2c241d', belly_root: '#1f1916', hip_core: '#30251f', left_haunch: '#352820', right_haunch: '#352820',
  tail_root_mass: '#2b211b', tail_mid_mass: '#241b17', tail_tip_root: '#1d1614',
  left_front_upper_mass: '#32251e', right_front_upper_mass: '#32251e', left_front_lower_root: '#261d19', right_front_lower_root: '#261d19',
  left_front_contact: '#1d1714', right_front_contact: '#1d1714', left_back_upper_mass: '#30231d', right_back_upper_mass: '#30231d',
  left_back_lower_root: '#241b17', right_back_lower_root: '#241b17', left_back_contact: '#1c1613', right_back_contact: '#1c1613',
};

async function main() {
  assert.strictEqual(sha256(SOURCE_PATH), OLD_SOURCE_SHA256, 'Revision 1 source identity changed before revision 2 authoring.');
  fs.mkdirSync(LIVE_ROOT, { recursive: true });
  let mcp = client();
  const health = await mcp.connect();
  const capabilities = await mcp.callTool('list_capabilities', {});
  assert.strictEqual(health.serverInfo?.name, 'ashfox');
  assert.strictEqual(health.serverInfo?.version, '0.2.0');
  assert.strictEqual(capabilities.blockbenchVersion, '5.1.6');
  assert.strictEqual(capabilities.toolRegistry.hash, '8043388b');
  let current = await state(mcp);
  assert(current.active && current.name === PROJECT_NAME);
  assert.deepStrictEqual(current.counts, { bones: 21, cubes: 29, meshes: 0, meshVertices: 0, meshFaces: 0, textures: 1, animations: 2 });
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_preflight.json'), current);
  await mcp.callTool('ensure_project', {
    action: 'ensure', format: 'Generic Model', name: PROJECT_NAME,
    match: 'format_and_name', onMismatch: 'error', onMissing: 'error', uvPixelsPerBlock: 4,
    ifRevision: current.revision, includeState: true,
  });

  for (const [name, pivot] of BONE_PIVOTS) await mutate(mcp, 'update_bone', { name, pivot });
  for (const [name, from, to, origin, rotation] of CUBE_UPDATES) {
    await mutate(mcp, 'update_cube', { name, from, to, origin, rotation });
  }
  current = await state(mcp);
  const textureName = current.textures[0].name;
  for (const [cubeName, color] of Object.entries(COLORS)) {
    await mutate(mcp, 'paint_faces', {
      textureName,
      target: { cubeName },
      coordSpace: 'face',
      op: { op: 'fill_rect', x: 0, y: 0, width: 64, height: 64, color, shade: { enabled: true, intensity: 0.1, edge: 0.05, noise: 0, lightDir: 'top_bottom' } },
      mapping: 'stretch',
    });
  }

  for (const [frame, bones] of [
    [4, [
      { name: 'left_front_upper', rot: [-12, 0, -7] }, { name: 'right_front_upper', rot: [-12, 0, 7] },
      { name: 'left_back_upper', rot: [-18, 0, -6] }, { name: 'right_back_upper', rot: [-18, 0, 6] },
      { name: 'tail_base', rot: [-9, 0, 0] },
    ]],
    [9, [
      { name: 'left_front_upper', rot: [10, 0, -9] }, { name: 'right_front_upper', rot: [10, 0, 9] },
      { name: 'left_back_upper', rot: [13, 0, -7] }, { name: 'right_back_upper', rot: [13, 0, 7] },
      { name: 'tail_base', rot: [7, 0, 0] },
    ]],
    [12, [
      { name: 'left_front_upper', rot: [16, 0, -11] }, { name: 'right_front_upper', rot: [16, 0, 11] },
      { name: 'left_front_lower', rot: [-7, 0, 0] }, { name: 'right_front_lower', rot: [-7, 0, 0] },
      { name: 'left_back_upper', rot: [18, 0, -8] }, { name: 'right_back_upper', rot: [18, 0, 8] },
      { name: 'left_back_lower', rot: [-10, 0, 0] }, { name: 'right_back_lower', rot: [-10, 0, 0] },
      { name: 'tail_base', rot: [12, 0, 0] }, { name: 'tail_mid', rot: [6, -8, 0] },
    ]],
    [16, [
      { name: 'left_front_upper', rot: [7, 0, -7] }, { name: 'right_front_upper', rot: [7, 0, 7] },
      { name: 'left_front_lower', rot: [-3, 0, 0] }, { name: 'right_front_lower', rot: [-3, 0, 0] },
      { name: 'left_back_upper', rot: [9, 0, -6] }, { name: 'right_back_upper', rot: [9, 0, 6] },
      { name: 'left_back_lower', rot: [-4, 0, 0] }, { name: 'right_back_lower', rot: [-4, 0, 0] },
      { name: 'tail_base', rot: [6, 8, 0] }, { name: 'tail_mid', rot: [3, -10, 0] },
    ]],
    [22, [
      { name: 'left_front_upper', rot: [0, 0, 0] }, { name: 'right_front_upper', rot: [0, 0, 0] },
      { name: 'left_front_lower', rot: [0, 0, 0] }, { name: 'right_front_lower', rot: [0, 0, 0] },
      { name: 'left_back_upper', rot: [0, 0, 0] }, { name: 'right_back_upper', rot: [0, 0, 0] },
      { name: 'left_back_lower', rot: [0, 0, 0] }, { name: 'right_back_lower', rot: [0, 0, 0] },
      { name: 'tail_base', rot: [0, 0, 0] }, { name: 'tail_mid', rot: [0, 0, 0] },
    ]],
  ]) await mutate(mcp, 'set_frame_pose', { clip: 'lunge', frame, bones, interp: 'linear' });

  const authored = await state(mcp);
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: authored.revision });
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_validate_before_export.json'), validation);
  assert.deepStrictEqual(validation.findings || [], []);
  const liveRenders = [];
  for (const [name, angle] of [['front', [-8, 0, 0]], ['side', [-8, 90, 0]], ['back', [-8, 180, 0]], ['three_quarter', [-10, 35, 0]]]) {
    liveRenders.push(await capture(mcp, name, angle));
  }
  await saveTexture(mcp);
  const semanticBefore = normalizeState(await state(mcp));
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_semantic_before.json'), semanticBefore);
  current = await state(mcp);
  const exported = await mcp.callTool('export', {
    format: 'native_codec', codecId: 'project', destPath: SOURCE_PATH,
    options: { fallback: 'strict', includeDiagnostics: true }, includeState: true, ifRevision: current.revision,
  });
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_native_export.json'), exported);
  const newSourceSha256 = sha256(SOURCE_PATH);
  assert.notStrictEqual(newSourceSha256, OLD_SOURCE_SHA256);
  current = await state(mcp);
  await mcp.callTool('ensure_project', { action: 'delete', target: { name: PROJECT_NAME }, force: true, ifRevision: current.revision });
  await mcp.close();
  launchSource();
  mcp = client();
  await mcp.connect();
  const reopened = await waitForProject(mcp);
  const rawReopenValidation = await mcp.callTool('validate', { includeState: true, ifRevision: reopened.revision });
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_validate_after_reopen_raw.json'), rawReopenValidation);
  await mcp.callTool('ensure_project', {
    action: 'ensure', format: 'Generic Model', name: PROJECT_NAME,
    match: 'format_and_name', onMismatch: 'reuse', onMissing: 'error', uvPixelsPerBlock: 4,
    ifRevision: reopened.revision, includeState: true,
  });
  const reopenedRestored = await state(mcp);
  const restoredValidation = await mcp.callTool('validate', { includeState: true, ifRevision: reopenedRestored.revision });
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_validate_after_reopen.json'), restoredValidation);
  assert.deepStrictEqual(restoredValidation.findings || [], []);
  const semanticAfter = normalizeState(reopenedRestored);
  const diff = differences(semanticBefore, semanticAfter);
  const roundtrip = { beforeSha256: hashJson(semanticBefore), afterSha256: hashJson(semanticAfter), differences: diff };
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_semantic_after.json'), semanticAfter);
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_reopen_semantic_diff.json'), roundtrip);
  assert.deepStrictEqual(diff, []);
  writeJson(path.join(EVIDENCE_ROOT, 'iteration_2_authoring_receipt.json'), {
    status: 'READY_FOR_VISUAL_REVIEW_REVISION_2',
    generatedAt: new Date().toISOString(),
    previousSourceSha256: OLD_SOURCE_SHA256,
    source: { path: path.relative(REPO_ROOT, SOURCE_PATH), sha256: newSourceSha256 },
    approvedWriter: { id: 'ashfox-v0.2.0-native-roundtrip+mc.2', ashfoxVersion: health.serverInfo.version, blockbenchVersion: capabilities.blockbenchVersion, toolRegistryHash: capabilities.toolRegistry.hash },
    changedAxis: 'family_shell_mass_hierarchy_only',
    counts: reopenedRestored.counts,
    validationBeforeExport: validation,
    validationAfterReopen: restoredValidation,
    semanticRoundtrip: roundtrip,
    liveSupplementalRenders: liveRenders,
    claims: { artPass: false, integrationPass: false, runtimePass: false },
  });
  fs.writeFileSync(path.join(EVIDENCE_ROOT, 'source.sha256'), `${newSourceSha256}  ${path.relative(REPO_ROOT, SOURCE_PATH)}\n`, { mode: 0o600 });
  console.log(JSON.stringify({ status: 'READY_FOR_VISUAL_REVIEW_REVISION_2', sourceSha256: newSourceSha256, liveRenders, roundtrip }, null, 2));
  await mcp.close();
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
