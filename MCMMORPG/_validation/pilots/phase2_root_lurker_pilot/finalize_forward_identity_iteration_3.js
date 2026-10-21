#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { StreamableHttpMcpClient, loadConfiguredEndpoints } = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const CONFIG = path.join(REPO_ROOT, '.mcp.json');
const SOURCE = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE = path.join(__dirname, 'phase2_root_lurker_pilot.png');
const EVIDENCE = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const LIVE = path.join(EVIDENCE, 'live_supplemental/iteration_3/exact_source');
const PLAYBACK = path.join(EVIDENCE, 'moving_playback/iteration_3');
const OLD_SOURCE_SHA = '83838dac643a6c55ff630a4fb131b26299cb6577cc3ebcfea1b8ac9fd3d931a3';
const PROJECT_NAME = 'phase2_root_lurker_pilot';
const APPIMAGE = '/home/przemek/.local/opt/blockbench/5.1.6/Blockbench_5.1.6.AppImage';
const PROFILE = '/home/przemek/.local/state/mc-ashfox-conformance';

function sha256(filePath) { return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex'); }
function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonical(value[key])]));
  return value;
}
function hashJson(value) { return crypto.createHash('sha256').update(`${JSON.stringify(canonical(value))}\n`).digest('hex'); }
function writeJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
}
function projectFrom(value) { return value?.project || value?.state || value; }
function endpoint() {
  const found = loadConfiguredEndpoints(CONFIG).find((entry) => entry.name === 'blockbench-ashfox');
  assert(found?.url === 'http://127.0.0.1:8788/mcp');
  return found;
}
function client() { return new StreamableHttpMcpClient(endpoint(), { timeoutMs: 30000 }); }
async function state(mcp) { return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true })); }

function normalizeState(project) {
  const boneNames = new Map((project.bones || []).map((bone) => [bone.id, bone.name]));
  const textureNames = new Map((project.textures || []).map((texture) => [texture.id, texture.name]));
  const normalizeFace = (face) => canonical({ ...face, texture: textureNames.get(face?.texture) || (project.textures?.length === 1 ? project.textures[0].name : face?.texture) });
  return canonical({
    name: project.name,
    format: project.format,
    formatId: project.formatId ?? null,
    counts: project.counts,
    bones: (project.bones || []).map((bone) => ({
      name: bone.name, parent: boneNames.get(bone.parent) || bone.parent || null,
      pivot: bone.pivot, rotation: bone.rotation, scale: bone.scale, visibility: bone.visibility,
    })).sort((a, b) => a.name.localeCompare(b.name)),
    cubes: (project.cubes || []).map((cube) => ({
      name: cube.name, bone: boneNames.get(cube.bone) || cube.bone,
      from: cube.from, to: cube.to, origin: cube.origin, rotation: cube.rotation,
      inflate: cube.inflate, mirror: cube.mirror, visibility: cube.visibility, boxUv: cube.boxUv,
      faces: Object.fromEntries(Object.entries(cube.faces || {}).map(([key, face]) => [key, normalizeFace(face)])),
    })).sort((a, b) => a.name.localeCompare(b.name)),
    textures: (project.textures || []).map((texture) => ({ name: texture.name, width: texture.width, height: texture.height })),
    animations: (project.animations || []).map((animation) => ({
      name: animation.name, length: animation.length, loop: animation.loop, fps: animation.fps,
      channels: (animation.channels || []).map((channel) => ({
        bone: boneNames.get(channel.bone) || channel.bone,
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

function pngBlock(raw, filePath, index = 0) {
  const blocks = (raw?.content || []).filter((entry) => entry.type === 'image' && entry.data);
  const block = blocks[index];
  assert(block?.mimeType === 'image/png', `Missing PNG block ${index}.`);
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
}

async function captureFixed(mcp, id, angle) {
  const current = await state(mcp);
  const raw = await mcp.request('tools/call', { name: 'render_preview', arguments: { mode: 'fixed', output: 'single', angle, includeState: true, ifRevision: current.revision } });
  const filePath = path.join(LIVE, `${id}.png`);
  pngBlock(raw, filePath);
  return { id, path: path.relative(REPO_ROOT, filePath), sha256: sha256(filePath), revision: current.revision };
}

async function captureSequence(mcp, id, clip) {
  const current = await state(mcp);
  const args = { mode: 'turntable', output: 'sequence', durationSeconds: 2, fps: 8, includeState: true, ifRevision: current.revision };
  if (clip) args.clip = clip;
  const raw = await mcp.request('tools/call', { name: 'render_preview', arguments: args });
  const blocks = (raw?.content || []).filter((entry) => entry.type === 'image' && entry.data);
  assert.strictEqual(blocks.length, 16, `${id} did not return 16 frames.`);
  const root = path.join(PLAYBACK, id);
  const frames = blocks.map((block, index) => {
    const filePath = path.join(root, `frame_${String(index + 1).padStart(2, '0')}.png`);
    fs.mkdirSync(root, { recursive: true });
    fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
    return { index: index + 1, path: path.relative(REPO_ROOT, filePath), sha256: sha256(filePath) };
  });
  return { id, clip: clip || null, producer: 'ashfox_render_preview_turntable_sequence', revision: current.revision, frameCount: frames.length, fps: 8, durationSeconds: 2, frames, metadata: raw.structuredContent };
}

async function captureFixedSamples(mcp) {
  const output = [];
  for (const timeSeconds of [0, 0.5, 1, 1.5, 2]) {
    const current = await state(mcp);
    const raw = await mcp.request('tools/call', { name: 'render_preview', arguments: { mode: 'fixed', output: 'single', clip: 'idle', timeSeconds, angle: [-10, 35, 0], includeState: true, ifRevision: current.revision } });
    const filePath = path.join(PLAYBACK, 'fixed_idle_samples', `idle_${timeSeconds.toFixed(2).replace('.', '_')}.png`);
    pngBlock(raw, filePath);
    output.push({ timeSeconds, path: path.relative(REPO_ROOT, filePath), sha256: sha256(filePath) });
  }
  return output;
}

function launchSource() {
  const child = spawn(APPIMAGE, [SOURCE, '--ozone-platform=x11', '--disable-vulkan', '--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'], {
    env: {
      ...process.env,
      ASHFOX_HOST: '127.0.0.1', ASHFOX_PORT: '8788', ASHFOX_PATH: '/mcp',
      XDG_CONFIG_HOME: path.join(PROFILE, 'xdg-config'),
      XDG_CACHE_HOME: path.join(PROFILE, 'xdg-cache'),
      XDG_DATA_HOME: path.join(PROFILE, 'xdg-data'),
    },
    detached: true, stdio: 'ignore',
  });
  child.unref();
}

async function waitForProject(mcp) {
  for (let attempt = 0; attempt < 40; attempt += 1) {
    try {
      const current = await state(mcp);
      if (current.active && current.name === PROJECT_NAME) return current;
    } catch (error) {
      if (attempt === 39) throw error;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error('Timed out waiting for native reopen.');
}

async function main() {
  assert.strictEqual(sha256(SOURCE), OLD_SOURCE_SHA, 'Revision 2 source on disk changed before native export.');
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
  const validationBefore = await mcp.callTool('validate', { includeState: true, ifRevision: current.revision });
  assert.deepStrictEqual(validationBefore.findings || [], []);
  writeJson(path.join(EVIDENCE, 'iteration_3_validate_before_export.json'), validationBefore);
  const semanticBefore = normalizeState(await state(mcp));
  writeJson(path.join(EVIDENCE, 'iteration_3_semantic_before.json'), semanticBefore);
  const textureRaw = await mcp.request('tools/call', { name: 'read_texture', arguments: { id: current.textures[0].id } });
  pngBlock(textureRaw, TEXTURE);
  const textureSha = sha256(TEXTURE);
  current = await state(mcp);
  const exported = await mcp.callTool('export', {
    format: 'native_codec', codecId: 'project', destPath: SOURCE,
    options: { fallback: 'strict', includeDiagnostics: true }, includeState: true, ifRevision: current.revision,
  });
  writeJson(path.join(EVIDENCE, 'iteration_3_native_export.json'), exported);
  const sourceSha = sha256(SOURCE);
  assert.notStrictEqual(sourceSha, OLD_SOURCE_SHA, 'Native export did not change source identity.');

  current = await state(mcp);
  await mcp.callTool('ensure_project', { action: 'delete', target: { name: PROJECT_NAME }, force: true, ifRevision: current.revision });
  await mcp.close();
  launchSource();
  mcp = client();
  await mcp.connect();
  let reopened = await waitForProject(mcp);
  const rawValidation = await mcp.callTool('validate', { includeState: true, ifRevision: reopened.revision });
  writeJson(path.join(EVIDENCE, 'iteration_3_validate_after_reopen_raw.json'), rawValidation);
  await mcp.callTool('ensure_project', {
    action: 'ensure', format: 'Generic Model', name: PROJECT_NAME,
    match: 'format_and_name', onMismatch: 'reuse', onMissing: 'error', uvPixelsPerBlock: 4,
    ifRevision: reopened.revision, includeState: true,
  });
  reopened = await state(mcp);
  const restoredValidation = await mcp.callTool('validate', { includeState: true, ifRevision: reopened.revision });
  assert.deepStrictEqual(restoredValidation.findings || [], []);
  writeJson(path.join(EVIDENCE, 'iteration_3_validate_after_reopen.json'), restoredValidation);
  const semanticAfter = normalizeState(reopened);
  const semanticDiff = { beforeSha256: hashJson(semanticBefore), afterSha256: hashJson(semanticAfter), differences: differences(semanticBefore, semanticAfter) };
  assert.deepStrictEqual(semanticDiff.differences, []);
  writeJson(path.join(EVIDENCE, 'iteration_3_semantic_after.json'), semanticAfter);
  writeJson(path.join(EVIDENCE, 'iteration_3_reopen_semantic_diff.json'), semanticDiff);

  const liveRenders = [];
  for (const [id, angle] of [['front', [-8, 0, 0]], ['side', [-8, 90, 0]], ['back', [-8, 180, 0]], ['three_quarter', [-10, 35, 0]]]) liveRenders.push(await captureFixed(mcp, id, angle));
  const fixedSamples = await captureFixedSamples(mcp);
  const idleSequence = await captureSequence(mcp, 'idle_clip_sequence', 'idle');
  const neutralSequence = await captureSequence(mcp, 'neutral_control_sequence', null);
  const pairedDifferences = idleSequence.frames.map((frame, index) => ({ index: frame.index, idleSha256: frame.sha256, neutralSha256: neutralSequence.frames[index].sha256, different: frame.sha256 !== neutralSequence.frames[index].sha256 }));
  const differentFrameCount = pairedDifferences.filter((entry) => entry.different).length;
  const fixedSampleUniqueHashes = new Set(fixedSamples.map((entry) => entry.sha256)).size;
  const playbackStatus = differentFrameCount > 0 ? 'MOVING_SEQUENCE_CAPTURED' : 'NOT_EVALUABLE';
  const playbackReceipt = {
    status: playbackStatus,
    sourceSha256: sourceSha,
    producer: 'ashfox_render_preview_turntable_sequence_exact_source',
    proofLogic: 'idle and neutral control use identical turntable duration/fps; paired differences isolate clip evaluation from camera movement',
    limitations: ['turntable_camera_motion_is_present', 'sequence_is_local_Blockbench_preview_not_runtime', 'fixed_samples_are_supporting_stills_not_moving_proof'],
    differentFrameCount,
    fixedSampleUniqueHashes,
    fixedSamples,
    idleSequence,
    neutralSequence,
    pairedDifferences,
  };
  writeJson(path.join(PLAYBACK, 'playback_receipt.json'), playbackReceipt);
  writeJson(path.join(EVIDENCE, 'iteration_3_authoring_receipt.json'), {
    status: 'READY_FOR_VISUAL_REVIEW_REVISION_3',
    generatedAt: new Date().toISOString(),
    previousSourceSha256: OLD_SOURCE_SHA,
    source: { path: path.relative(REPO_ROOT, SOURCE), sha256: sourceSha },
    texture: { path: path.relative(REPO_ROOT, TEXTURE), sha256: textureSha },
    approvedWriter: { id: 'ashfox-v0.2.0-native-roundtrip+mc.2', ashfoxVersion: health.serverInfo.version, blockbenchVersion: capabilities.blockbenchVersion, toolRegistryHash: capabilities.toolRegistry.hash },
    changedAxis: 'forward_threat_identity_only',
    frozen: ['five_mass_side', 'player_scale', 'limb_chains', 'idle_clip_authored_data', 'lunge_clip_authored_data', 'rear_gap_contact'],
    geometry: { headShoulderProw: 'asymmetric_root_wedge', headNeckNotch: 'open', attackSideBreakCount: 1 },
    material: { primary: 'dark_bark_root_planes', secondary: 'two_muted_single_pixel_moss_accents', focal: 'one_highest_contrast_corruption_face_at_attack_side_threat_origin', tertiaryDetailAdded: false, pinkNosePresent: false },
    counts: reopened.counts,
    validationBeforeExport: validationBefore,
    validationImmediatelyAfterReopen: rawValidation,
    validationAfterUvDensityRestore: restoredValidation,
    semanticRoundtrip: semanticDiff,
    liveSupplementalRenders: liveRenders,
    movingPlayback: { status: playbackStatus, receiptPath: path.relative(REPO_ROOT, path.join(PLAYBACK, 'playback_receipt.json')), differentFrameCount, fixedSampleUniqueHashes },
    claims: { artPass: false, integrationPass: false, runtimePass: false },
  });
  fs.writeFileSync(path.join(EVIDENCE, 'source.sha256'), `${sourceSha}  ${path.relative(REPO_ROOT, SOURCE)}\n`, { mode: 0o600 });
  console.log(JSON.stringify({ status: 'READY_FOR_VISUAL_REVIEW_REVISION_3', sourceSha256: sourceSha, textureSha256: textureSha, semanticDiff, movingPlayback: { status: playbackStatus, differentFrameCount, fixedSampleUniqueHashes }, liveRenders }, null, 2));
  await mcp.close();
}

main().catch((error) => { console.error(error.stack || error.message || String(error)); process.exitCode = 1; });
