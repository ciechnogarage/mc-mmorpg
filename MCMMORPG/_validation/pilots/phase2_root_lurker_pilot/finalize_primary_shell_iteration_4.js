#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { StreamableHttpMcpClient, loadConfiguredEndpoints } = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const SOURCE = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE = path.join(__dirname, 'phase2_root_lurker_pilot.png');
const EVIDENCE = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const LIVE = path.join(EVIDENCE, 'live_supplemental/iteration_4/exact_source');
const PREVIOUS_SOURCE_SHA = '609d35b4bf4c9af45b72e7611565ce828e754fecd6576f952c90278395f4d798';
const CHECKPOINT_SOURCE_SHA = 'ec5d04320484091dd3d0a42477d79e86bdab4d4c1e8e9e7bcddf0ec451a84236';
const APPIMAGE = '/home/przemek/.local/opt/blockbench/5.1.6/Blockbench_5.1.6.AppImage';
const PROFILE = '/home/przemek/.local/state/mc-ashfox-conformance';

function sha256(filePath) { return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex'); }
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonical(value[key])]));
  return value;
}
function hashJson(value) { return crypto.createHash('sha256').update(`${JSON.stringify(canonical(value))}\n`).digest('hex'); }
function writeJson(name, value) { fs.writeFileSync(path.join(EVIDENCE, name), `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 }); }
function projectFrom(value) { return value?.project || value?.state || value; }
function endpoint() {
  const found = loadConfiguredEndpoints(path.join(REPO_ROOT, '.mcp.json')).find((entry) => entry.name === 'blockbench-ashfox');
  assert(found?.url === 'http://127.0.0.1:8788/mcp');
  return found;
}
function client() { return new StreamableHttpMcpClient(endpoint(), { timeoutMs: 30000 }); }
async function state(mcp) { return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true })); }

function normalize(project) {
  const boneNames = new Map((project.bones || []).map((bone) => [bone.id, bone.name]));
  const textureNames = new Map((project.textures || []).map((texture) => [texture.id, texture.name]));
  const face = (value) => canonical({ ...value, texture: textureNames.get(value?.texture) || (project.textures?.length === 1 ? project.textures[0].name : value?.texture) });
  return canonical({
    name: project.name, format: project.format, formatId: project.formatId ?? null, counts: project.counts,
    bones: (project.bones || []).map((bone) => ({
      name: bone.name, parent: boneNames.get(bone.parent) || bone.parent || null,
      pivot: bone.pivot, rotation: bone.rotation, scale: bone.scale, visibility: bone.visibility,
    })).sort((a, b) => a.name.localeCompare(b.name)),
    cubes: (project.cubes || []).map((cube) => ({
      name: cube.name, bone: boneNames.get(cube.bone) || cube.bone,
      from: cube.from, to: cube.to, origin: cube.origin, rotation: cube.rotation,
      inflate: cube.inflate, mirror: cube.mirror, visibility: cube.visibility, boxUv: cube.boxUv,
      faces: Object.fromEntries(Object.entries(cube.faces || {}).map(([key, value]) => [key, face(value)])),
    })).sort((a, b) => a.name.localeCompare(b.name)),
    textures: (project.textures || []).map((texture) => ({ name: texture.name, width: texture.width, height: texture.height })),
    animations: (project.animations || []).map((animation) => ({
      name: animation.name, length: animation.length, loop: animation.loop, fps: animation.fps,
      channels: (animation.channels || []).map((channel) => ({
        bone: boneNames.get(channel.bone) || channel.bone, channel: channel.channel,
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
    for (let index = 0; index < Math.max(before.length, after.length); index += 1) differences(before[index], after[index], `${currentPath}[${index}]`, output);
    return output;
  }
  if (before && after && typeof before === 'object' && typeof after === 'object') {
    for (const key of [...new Set([...Object.keys(before), ...Object.keys(after)])].sort()) differences(before[key], after[key], `${currentPath}.${key}`, output);
    return output;
  }
  output.push({ path: currentPath, before: before ?? null, after: after ?? null });
  return output;
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
function launchSource() {
  const child = spawn(APPIMAGE, [SOURCE, '--ozone-platform=x11', '--disable-vulkan', '--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'], {
    env: {
      ...process.env,
      ASHFOX_HOST: '127.0.0.1', ASHFOX_PORT: '8788', ASHFOX_PATH: '/mcp',
      XDG_CONFIG_HOME: path.join(PROFILE, 'xdg-config'), XDG_CACHE_HOME: path.join(PROFILE, 'xdg-cache'), XDG_DATA_HOME: path.join(PROFILE, 'xdg-data'),
    }, detached: true, stdio: 'ignore',
  });
  child.unref();
}
async function waitForProject(mcp) {
  for (let attempt = 0; attempt < 40; attempt += 1) {
    try {
      const current = await state(mcp);
      if (current.active && current.name === 'phase2_root_lurker_pilot') return current;
    } catch (error) { if (attempt === 39) throw error; }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error('Timed out waiting for native reopen.');
}

async function main() {
  assert.strictEqual(sha256(SOURCE), CHECKPOINT_SOURCE_SHA, 'Tuned checkpoint source identity changed before finalization.');
  let mcp = client();
  const health = await mcp.connect();
  const capabilities = await mcp.callTool('list_capabilities', {});
  assert.strictEqual(health.serverInfo?.version, '0.2.0');
  assert.strictEqual(capabilities.blockbenchVersion, '5.1.6');
  assert.strictEqual(capabilities.toolRegistry.hash, '8043388b');
  let current = await state(mcp);
  assert(current.active && current.name === 'phase2_root_lurker_pilot');
  assert.deepStrictEqual(current.counts, { bones: 21, cubes: 29, meshes: 0, meshVertices: 0, meshFaces: 0, textures: 1, animations: 2 });
  const validationBefore = await mcp.callTool('validate', { includeState: true, ifRevision: current.revision });
  assert.deepStrictEqual(validationBefore.findings || [], []);
  writeJson('iteration_4_validate_before_export.json', validationBefore);
  const semanticBefore = normalize(await state(mcp));
  writeJson('iteration_4_semantic_before.json', semanticBefore);
  const rawTexture = await mcp.request('tools/call', { name: 'read_texture', arguments: { id: current.textures[0].id } });
  png(rawTexture, TEXTURE);
  const textureSha = sha256(TEXTURE);
  current = await state(mcp);
  const exported = await mcp.callTool('export', {
    format: 'native_codec', codecId: 'project', destPath: SOURCE,
    options: { fallback: 'strict', includeDiagnostics: true }, includeState: true, ifRevision: current.revision,
  });
  writeJson('iteration_4_native_export.json', exported);
  const sourceSha = sha256(SOURCE);

  current = await state(mcp);
  await mcp.callTool('ensure_project', { action: 'delete', target: { name: current.name }, force: true, ifRevision: current.revision });
  await mcp.close();
  launchSource();
  mcp = client();
  await mcp.connect();
  let reopened = await waitForProject(mcp);
  const rawValidation = await mcp.callTool('validate', { includeState: true, ifRevision: reopened.revision });
  writeJson('iteration_4_validate_after_reopen_raw.json', rawValidation);
  await mcp.callTool('ensure_project', {
    action: 'ensure', format: 'Generic Model', name: 'phase2_root_lurker_pilot',
    match: 'format_and_name', onMismatch: 'reuse', onMissing: 'error', uvPixelsPerBlock: 4,
    ifRevision: reopened.revision, includeState: true,
  });
  reopened = await state(mcp);
  const validationAfter = await mcp.callTool('validate', { includeState: true, ifRevision: reopened.revision });
  assert.deepStrictEqual(validationAfter.findings || [], []);
  writeJson('iteration_4_validate_after_reopen.json', validationAfter);
  const semanticAfter = normalize(reopened);
  const roundtrip = { beforeSha256: hashJson(semanticBefore), afterSha256: hashJson(semanticAfter), differences: differences(semanticBefore, semanticAfter) };
  assert.deepStrictEqual(roundtrip.differences, []);
  writeJson('iteration_4_semantic_after.json', semanticAfter);
  writeJson('iteration_4_reopen_semantic_diff.json', roundtrip);

  const previous = JSON.parse(fs.readFileSync(path.join(EVIDENCE, 'iteration_3_semantic_after.json'), 'utf8'));
  const persistedShape = (value) => JSON.parse(JSON.stringify(value));
  assert.deepStrictEqual(persistedShape(semanticAfter.bones), previous.bones, 'Rig/bone drift during primary-shell rebuild.');
  assert.deepStrictEqual(persistedShape(semanticAfter.animations), previous.animations, 'Animation clip data drift during primary-shell rebuild.');
  const geometry = (cube) => ({ name: cube.name, bone: cube.bone, from: cube.from, to: cube.to, origin: cube.origin, rotation: cube.rotation, inflate: cube.inflate, mirror: cube.mirror, visibility: cube.visibility, boxUv: cube.boxUv });
  const oldGeometry = new Map(previous.cubes.map((cube) => [cube.name, geometry(cube)]));
  const changedGeometryNames = semanticAfter.cubes.filter((cube) => JSON.stringify(oldGeometry.get(cube.name)) !== JSON.stringify(geometry(cube))).map((cube) => cube.name).sort();
  const expectedChanged = ['attack_side_root_hook', 'belly_root', 'hip_core', 'left_haunch', 'left_shoulder_plate', 'mask_brow', 'mask_core', 'rib_taper', 'right_haunch', 'right_shoulder_plate', 'shoulder_core', 'shoulder_shelf', 'snout_lower', 'tail_mid_mass', 'tail_root_mass'];
  assert.deepStrictEqual(changedGeometryNames, expectedChanged, 'Primary-shell rebuild scope differs from the declared mass chain.');
  const scopeDiff = {
    previousSourceSha256: PREVIOUS_SOURCE_SHA, sourceSha256: sourceSha, changedAxis: 'primary_shell_read',
    changedGeometryNames, changedGeometryCount: changedGeometryNames.length,
    frozenGeometryNames: semanticAfter.cubes.map((cube) => cube.name).filter((name) => !changedGeometryNames.includes(name)).sort(),
    boneDataUnchanged: true, animationDataUnchanged: true, rigNamesUnchanged: true, hitboxUnchanged: true,
  };
  writeJson('iteration_4_scope_diff.json', scopeDiff);
  const liveRenders = [];
  for (const [id, angle] of [['front', [-8, 0, 0]], ['side', [-8, 90, 0]], ['back', [-8, 180, 0]], ['three_quarter', [-10, 35, 0]]]) liveRenders.push(await capture(mcp, id, angle));
  writeJson('iteration_4_authoring_receipt.json', {
    status: 'READY_FOR_VISUAL_REVIEW_REVISION_4', generatedAt: new Date().toISOString(),
    previousSourceSha256: PREVIOUS_SOURCE_SHA,
    source: { path: path.relative(REPO_ROOT, SOURCE), sha256: sourceSha },
    texture: { path: path.relative(REPO_ROOT, TEXTURE), sha256: textureSha },
    approvedWriter: { id: 'ashfox-v0.2.0-native-roundtrip+mc.2', ashfoxVersion: health.serverInfo.version, blockbenchVersion: capabilities.blockbenchVersion, toolRegistryHash: capabilities.toolRegistry.hash },
    changedAxis: 'primary_shell_read', scopeDiff, counts: reopened.counts,
    validationBeforeExport: validationBefore, validationImmediatelyAfterReopen: rawValidation, validationAfterUvDensityRestore: validationAfter,
    semanticRoundtrip: roundtrip, liveSupplementalRenders: liveRenders,
    texturePolicy: 'diagnostic_dark_bark_one_restrained_non_glow_corruption_accent_no_polish',
    claims: { artPass: false, integrationPass: false, runtimePass: false },
  });
  fs.writeFileSync(path.join(EVIDENCE, 'source.sha256'), `${sourceSha}  ${path.relative(REPO_ROOT, SOURCE)}\n`, { mode: 0o600 });
  console.log(JSON.stringify({ status: 'READY_FOR_VISUAL_REVIEW_REVISION_4', sourceSha256: sourceSha, textureSha256: textureSha, semanticRoundtrip: roundtrip, scopeDiff, liveRenders }, null, 2));
  await mcp.close();
}

main().catch((error) => { console.error(error.stack || error.message || String(error)); process.exitCode = 1; });
