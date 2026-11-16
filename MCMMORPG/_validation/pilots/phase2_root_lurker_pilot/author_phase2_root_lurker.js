#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const {
  StreamableHttpMcpClient,
  ToolCallError,
  loadConfiguredEndpoints,
} = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const CONFIG_PATH = path.join(REPO_ROOT, '.mcp.json');
const SPEC_PATH = path.join(REPO_ROOT, 'MCMMORPG/_validation/asset_specs/phase2_root_lurker_pilot.assetspec.yaml');
const LOCK_PATH = path.join(REPO_ROOT, 'MCMMORPG/_validation/blockbench_mcp/ashfox.lock.json');
const SOURCE_ROOT = __dirname;
const EVIDENCE_ROOT = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const SOURCE_PATH = path.join(SOURCE_ROOT, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE_PATH = path.join(SOURCE_ROOT, 'phase2_root_lurker_pilot.png');
const PROJECT_NAME = 'phase2_root_lurker_pilot';
const SPEC_SHA256 = 'f708666ffbec1b68615f0753e23c5d5f83dc3f6959a45c7c12cd563dcad82546';
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
  const endpoints = loadConfiguredEndpoints(CONFIG_PATH);
  const ashfox = endpoints.find((entry) => entry.name === 'blockbench-ashfox');
  assert(ashfox, 'Configured loopback blockbench-ashfox endpoint is missing.');
  assert.strictEqual(new URL(ashfox.url).hostname, '127.0.0.1');
  assert.strictEqual(new URL(ashfox.url).port, '8788');
  return ashfox;
}

function createClient() {
  return new StreamableHttpMcpClient(endpoint(), { timeoutMs: 30000 });
}

async function state(mcp) {
  return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
}

async function mutate(mcp, tool, args) {
  const current = await state(mcp);
  assert(current.active, `Cannot call ${tool} without an active project.`);
  assert.strictEqual(current.name, PROJECT_NAME, `Refusing to mutate active project ${current.name}.`);
  return mcp.callTool(tool, { ...args, ifRevision: current.revision });
}

function id(number) {
  return `71000000-0000-4000-8000-${String(number).padStart(12, '0')}`;
}

const BONES = [
  { id: id(1), name: 'root', pivot: [0, 8, 2] },
  { id: id(2), name: 'chest', parent: 'root', pivot: [0, 11, -1] },
  { id: id(3), name: 'lower_back', parent: 'root', pivot: [0, 10, 9] },
  { id: id(4), name: 'head', parent: 'chest', pivot: [0, 12, -7] },
  { id: id(5), name: 'threat_tip', parent: 'head', pivot: [0, 11, -13] },
  { id: id(6), name: 'left_front_upper', parent: 'chest', pivot: [4.5, 11, 0] },
  { id: id(7), name: 'left_front_lower', parent: 'left_front_upper', pivot: [4.5, 5.5, 0.5] },
  { id: id(8), name: 'left_front_foot', parent: 'left_front_lower', pivot: [4.5, 1.5, -0.5] },
  { id: id(9), name: 'right_front_upper', parent: 'chest', pivot: [-4.5, 11, 0] },
  { id: id(10), name: 'right_front_lower', parent: 'right_front_upper', pivot: [-4.5, 5.5, 0.5] },
  { id: id(11), name: 'right_front_foot', parent: 'right_front_lower', pivot: [-4.5, 1.5, -0.5] },
  { id: id(12), name: 'left_back_upper', parent: 'lower_back', pivot: [4, 10.5, 11.5] },
  { id: id(13), name: 'left_back_lower', parent: 'left_back_upper', pivot: [4, 5.5, 12] },
  { id: id(14), name: 'left_back_foot', parent: 'left_back_lower', pivot: [4, 1.5, 10.5] },
  { id: id(15), name: 'right_back_upper', parent: 'lower_back', pivot: [-4, 10.5, 11.5] },
  { id: id(16), name: 'right_back_lower', parent: 'right_back_upper', pivot: [-4, 5.5, 12] },
  { id: id(17), name: 'right_back_foot', parent: 'right_back_lower', pivot: [-4, 1.5, 10.5] },
  { id: id(18), name: 'tail_base', parent: 'lower_back', pivot: [0, 11, 14] },
  { id: id(19), name: 'tail_mid', parent: 'tail_base', pivot: [0, 8.5, 19] },
  { id: id(20), name: 'tail_tip', parent: 'tail_mid', pivot: [0, 5.5, 24] },
  { id: id(21), name: 'hitbox', pivot: [0, 9, 2], visibility: false },
];

const CUBES = [
  { name: 'shoulder_core', bone: 'chest', from: [-5.5, 9, -5], to: [5.5, 17, 4], color: '#3a2c25' },
  { name: 'shoulder_shelf', bone: 'chest', from: [-6.5, 14, -4], to: [6.5, 18.5, 3], origin: [0, 14, 0], rotation: [0, 0, -2], color: '#4b4a31' },
  { name: 'left_shoulder_plate', bone: 'chest', from: [4.7, 10.5, -4.5], to: [7, 17.5, 2.5], origin: [4.7, 13, -1], rotation: [0, 0, -8], color: '#2c2420' },
  { name: 'right_shoulder_plate', bone: 'chest', from: [-7, 10.5, -4.5], to: [-4.7, 17.5, 2.5], origin: [-4.7, 13, -1], rotation: [0, 0, 8], color: '#2c2420' },
  { name: 'rib_taper', bone: 'root', from: [-4, 8.5, 3], to: [4, 14.5, 10], origin: [0, 11, 6], rotation: [2, 0, 0], color: '#38402c' },
  { name: 'belly_root', bone: 'root', from: [-2.8, 7, 3.5], to: [2.8, 10, 9.5], color: '#211a18' },
  { name: 'hip_core', bone: 'lower_back', from: [-4.8, 8.5, 9], to: [4.8, 16.5, 15], origin: [0, 11, 11], rotation: [-2, 0, 0], color: '#3c2e24' },
  { name: 'left_haunch', bone: 'lower_back', from: [3.8, 9, 9.5], to: [6, 16, 14.5], origin: [4, 11, 11], rotation: [0, 0, -7], color: '#49352a' },
  { name: 'right_haunch', bone: 'lower_back', from: [-6, 9, 9.5], to: [-3.8, 16, 14.5], origin: [-4, 11, 11], rotation: [0, 0, 7], color: '#49352a' },
  { name: 'mask_core', bone: 'head', from: [-4.6, 9.5, -12], to: [4.6, 16.5, -5.5], origin: [0, 12, -7], rotation: [-5, 0, 0], color: '#4a3628' },
  { name: 'mask_brow', bone: 'head', from: [-5.2, 13, -10.5], to: [5.2, 18, -6], origin: [0, 14, -8], rotation: [3, 0, 0], color: '#2f2520' },
  { name: 'snout_lower', bone: 'threat_tip', from: [-3.1, 8.5, -16], to: [3.1, 12.5, -11.2], origin: [0, 11, -13], rotation: [5, 0, 0], color: '#3d2d24' },
  { name: 'attack_side_root_hook', bone: 'threat_tip', from: [-4.4, 10.2, -17], to: [-1.4, 13.4, -11.5], origin: [-2.3, 11.5, -13], rotation: [4, -10, 8], color: '#8b4054' },
  { name: 'left_front_upper_mass', bone: 'left_front_upper', from: [3.2, 5.2, -1.3], to: [5.8, 12.5, 2.3], origin: [4.5, 11, 0], rotation: [-5, 0, -2], color: '#3a2b23' },
  { name: 'left_front_lower_root', bone: 'left_front_lower', from: [3.45, 1.3, -0.2], to: [5.55, 6.5, 2.1], origin: [4.5, 5.5, 0.5], rotation: [7, 0, 1], color: '#2c221d' },
  { name: 'left_front_contact', bone: 'left_front_foot', from: [3.15, 0, -2.4], to: [6.1, 2.2, 1.5], origin: [4.5, 1.5, -0.5], rotation: [0, 0, -2], color: '#211916' },
  { name: 'right_front_upper_mass', bone: 'right_front_upper', from: [-5.8, 5.2, -1.3], to: [-3.2, 12.5, 2.3], origin: [-4.5, 11, 0], rotation: [-5, 0, 2], color: '#3a2b23' },
  { name: 'right_front_lower_root', bone: 'right_front_lower', from: [-5.55, 1.3, -0.2], to: [-3.45, 6.5, 2.1], origin: [-4.5, 5.5, 0.5], rotation: [7, 0, -1], color: '#2c221d' },
  { name: 'right_front_contact', bone: 'right_front_foot', from: [-6.1, 0, -2.4], to: [-3.15, 2.2, 1.5], origin: [-4.5, 1.5, -0.5], rotation: [0, 0, 2], color: '#211916' },
  { name: 'left_back_upper_mass', bone: 'left_back_upper', from: [2.7, 5.2, 10], to: [5.5, 12, 14.2], origin: [4, 10.5, 11.5], rotation: [8, 0, -3], color: '#423128' },
  { name: 'left_back_lower_root', bone: 'left_back_lower', from: [3, 1.3, 10.4], to: [5.1, 6.2, 13.4], origin: [4, 5.5, 12], rotation: [-9, 0, 2], color: '#2b211c' },
  { name: 'left_back_contact', bone: 'left_back_foot', from: [2.8, 0, 9], to: [5.7, 2.2, 13], origin: [4, 1.5, 10.5], rotation: [0, 0, -2], color: '#201815' },
  { name: 'right_back_upper_mass', bone: 'right_back_upper', from: [-5.5, 5.2, 10], to: [-2.7, 12, 14.2], origin: [-4, 10.5, 11.5], rotation: [8, 0, 3], color: '#423128' },
  { name: 'right_back_lower_root', bone: 'right_back_lower', from: [-5.1, 1.3, 10.4], to: [-3, 6.2, 13.4], origin: [-4, 5.5, 12], rotation: [-9, 0, -2], color: '#2b211c' },
  { name: 'right_back_contact', bone: 'right_back_foot', from: [-5.7, 0, 9], to: [-2.8, 2.2, 13], origin: [-4, 1.5, 10.5], rotation: [0, 0, 2], color: '#201815' },
  { name: 'tail_root_mass', bone: 'tail_base', from: [-3.2, 8, 13], to: [3.2, 13, 19.5], origin: [0, 11, 14], rotation: [15, 0, 0], color: '#36271f' },
  { name: 'tail_mid_mass', bone: 'tail_mid', from: [-2.4, 5, 18], to: [2.4, 10, 24.5], origin: [0, 8.5, 19], rotation: [21, 0, 0], color: '#2d211b' },
  { name: 'tail_tip_root', bone: 'tail_tip', from: [-1.6, 2.2, 23], to: [1.6, 7, 28.5], origin: [0, 5.5, 24], rotation: [28, 0, 0], color: '#241a17' },
  { name: 'hitbox', bone: 'hitbox', from: [-3.5, 0, -1.5], to: [3.5, 17, 5.5], visibility: false, color: null },
].map((cube, index) => ({ id: id(100 + index), boxUv: false, ...cube }));

const CLIPS = [
  { id: id(300), name: 'idle', length: 2, loop: true, fps: 20 },
  { id: id(301), name: 'lunge', length: 1.1, loop: false, fps: 20 },
];

const IDLE_POSES = [
  { frame: 0, bones: [
    { name: 'chest', rot: [-2, 0, 0] }, { name: 'head', rot: [2, -2, 0] },
    { name: 'lower_back', rot: [1, 0, 0] }, { name: 'tail_base', rot: [0, -3, 0] },
    { name: 'tail_mid', rot: [0, 4, 0] }, { name: 'threat_tip', rot: [1, 0, -1] },
  ] },
  { frame: 20, bones: [
    { name: 'chest', rot: [-4, 1, 0], pos: [0, 0.25, -0.2] }, { name: 'head', rot: [4, -4, -1] },
    { name: 'lower_back', rot: [2, -1, 0] }, { name: 'tail_base', rot: [-2, 5, 0] },
    { name: 'tail_mid', rot: [1, -7, 0] }, { name: 'tail_tip', rot: [0, 5, 0] },
    { name: 'threat_tip', rot: [3, -3, -2] },
  ] },
  { frame: 40, bones: [
    { name: 'chest', rot: [-2, 0, 0] }, { name: 'head', rot: [2, -2, 0] },
    { name: 'lower_back', rot: [1, 0, 0] }, { name: 'tail_base', rot: [0, -3, 0] },
    { name: 'tail_mid', rot: [0, 4, 0] }, { name: 'tail_tip', rot: [0, 0, 0] },
    { name: 'threat_tip', rot: [1, 0, -1] },
  ] },
];

const LUNGE_POSES = [
  { frame: 0, bones: [
    { name: 'root', pos: [0, 0, 0] }, { name: 'chest', rot: [0, 0, 0], pos: [0, 0, 0] },
    { name: 'lower_back', rot: [0, 0, 0] }, { name: 'head', rot: [0, 0, 0], pos: [0, 0, 0] },
    { name: 'threat_tip', rot: [0, 0, 0] }, { name: 'tail_base', rot: [0, 0, 0] },
  ] },
  { frame: 4, bones: [
    { name: 'root', pos: [0, -0.35, 1.2] }, { name: 'chest', rot: [7, 0, 0], pos: [0, -0.2, 0.7] },
    { name: 'lower_back', rot: [-7, 0, 0] }, { name: 'head', rot: [-7, 0, 0], pos: [0, 0, 0.5] },
    { name: 'threat_tip', rot: [-6, 6, 3] }, { name: 'left_back_upper', rot: [-16, 0, 0] },
    { name: 'right_back_upper', rot: [-16, 0, 0] }, { name: 'tail_base', rot: [-10, 0, 0] },
  ] },
  { frame: 9, bones: [
    { name: 'root', pos: [0, 0.35, -2.8] }, { name: 'chest', rot: [-9, 0, 0], pos: [0, 0.45, -1.7] },
    { name: 'lower_back', rot: [8, 0, 0] }, { name: 'head', rot: [5, -3, 0], pos: [0, 0.2, -0.8] },
    { name: 'threat_tip', rot: [7, -8, -3] }, { name: 'left_front_upper', rot: [12, 0, 0] },
    { name: 'right_front_upper', rot: [12, 0, 0] }, { name: 'left_back_upper', rot: [14, 0, 0] },
    { name: 'right_back_upper', rot: [14, 0, 0] }, { name: 'tail_base', rot: [8, 0, 0] },
  ] },
  { frame: 12, bones: [
    { name: 'root', pos: [0, 0.6, -5] }, { name: 'chest', rot: [-12, 0, 0], pos: [0, 0.55, -2.6] },
    { name: 'lower_back', rot: [11, 0, 0] }, { name: 'head', rot: [8, -5, 0], pos: [0, 0.1, -1.4] },
    { name: 'threat_tip', rot: [10, -13, -5] }, { name: 'left_front_upper', rot: [18, 0, 0] },
    { name: 'right_front_upper', rot: [18, 0, 0] }, { name: 'left_back_upper', rot: [20, 0, 0] },
    { name: 'right_back_upper', rot: [20, 0, 0] }, { name: 'tail_base', rot: [13, 0, 0] },
    { name: 'tail_mid', rot: [5, -8, 0] },
  ] },
  { frame: 16, bones: [
    { name: 'root', pos: [0, 0.15, -3.4] }, { name: 'chest', rot: [-5, 0, 0], pos: [0, 0.15, -1.2] },
    { name: 'lower_back', rot: [7, 0, 0] }, { name: 'head', rot: [4, 2, 0], pos: [0, 0, -0.6] },
    { name: 'threat_tip', rot: [4, 5, 2] }, { name: 'left_front_upper', rot: [8, 0, 0] },
    { name: 'right_front_upper', rot: [8, 0, 0] }, { name: 'left_back_upper', rot: [10, 0, 0] },
    { name: 'right_back_upper', rot: [10, 0, 0] }, { name: 'tail_base', rot: [7, 7, 0] },
    { name: 'tail_mid', rot: [3, -10, 0] },
  ] },
  { frame: 22, bones: [
    { name: 'root', pos: [0, 0, 0] }, { name: 'chest', rot: [0, 0, 0], pos: [0, 0, 0] },
    { name: 'lower_back', rot: [0, 0, 0] }, { name: 'head', rot: [0, 0, 0], pos: [0, 0, 0] },
    { name: 'threat_tip', rot: [0, 0, 0] }, { name: 'left_front_upper', rot: [0, 0, 0] },
    { name: 'right_front_upper', rot: [0, 0, 0] }, { name: 'left_back_upper', rot: [0, 0, 0] },
    { name: 'right_back_upper', rot: [0, 0, 0] }, { name: 'tail_base', rot: [0, 0, 0] },
    { name: 'tail_mid', rot: [0, 0, 0] },
  ] },
];

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

function pngFromToolResult(raw, filePath) {
  const block = raw?.content?.find((entry) => entry.type === 'image' && entry.data);
  assert(block, `MCP response did not contain image content for ${path.basename(filePath)}.`);
  assert.strictEqual(block.mimeType, 'image/png');
  fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
  return raw.structuredContent || null;
}

async function capture(mcp, fileName, options) {
  const current = await state(mcp);
  assert.strictEqual(current.name, PROJECT_NAME);
  const raw = await mcp.request('tools/call', {
    name: 'render_preview',
    arguments: {
      mode: 'fixed',
      output: 'single',
      ...options,
      includeState: true,
      ifRevision: current.revision,
    },
  });
  if (raw?.isError) throw new ToolCallError(`render_preview failed for ${fileName}`, raw.structuredContent || {});
  const filePath = path.join(EVIDENCE_ROOT, fileName);
  const metadata = pngFromToolResult(raw, filePath);
  return { file: fileName, sha256: sha256(filePath), revision: current.revision, metadata };
}

async function readTexture(mcp) {
  const current = await state(mcp);
  assert.strictEqual(current.textures.length, 1);
  const raw = await mcp.request('tools/call', {
    name: 'read_texture',
    arguments: { id: current.textures[0].id },
  });
  if (raw?.isError) throw new ToolCallError('read_texture failed', raw.structuredContent || {});
  pngFromToolResult(raw, TEXTURE_PATH);
  return { path: TEXTURE_PATH, sha256: sha256(TEXTURE_PATH), width: current.textures[0].width, height: current.textures[0].height };
}

function launchSource() {
  const child = spawn(APPIMAGE_PATH, [
    SOURCE_PATH,
    '--ozone-platform=x11',
    '--disable-vulkan',
    '--use-gl=angle',
    '--use-angle=swiftshader',
    '--enable-unsafe-swiftshader',
  ], {
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

async function waitForReopen(mcp) {
  const deadline = Date.now() + 20000;
  while (Date.now() < deadline) {
    const current = await state(mcp);
    if (current.active && current.name === PROJECT_NAME) return current;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error('Timed out waiting for native .bbmodel reopen.');
}

async function main() {
  fs.mkdirSync(SOURCE_ROOT, { recursive: true });
  fs.mkdirSync(EVIDENCE_ROOT, { recursive: true });
  assert.strictEqual(sha256(SPEC_PATH), SPEC_SHA256, 'AssetSpec hash mismatch.');
  const lock = JSON.parse(fs.readFileSync(LOCK_PATH, 'utf8'));
  assert.strictEqual(lock.local_patch.id, 'ashfox-v0.2.0-native-roundtrip+mc.2');
  assert.strictEqual(sha256(APPIMAGE_PATH), lock.blockbench.appimage_sha256);
  assert.strictEqual(sha256(lock.local_patch.plugin_path), lock.local_patch.plugin_sha256);
  assert.strictEqual(sha256(lock.local_patch.sidecar_path), lock.local_patch.sidecar_sha256);

  let mcp = createClient();
  const health = await mcp.connect();
  const capabilities = await mcp.callTool('list_capabilities', {});
  assert.strictEqual(health.serverInfo?.name, 'ashfox');
  assert.strictEqual(health.serverInfo?.version, '0.2.0');
  assert.strictEqual(health.protocolVersion, '2025-11-25');
  assert.strictEqual(health.tools.length, lock.expected_tool_count);
  assert.strictEqual(capabilities.toolRegistry.hash, lock.expected_tool_registry_hash);
  assert(capabilities.exportTargets.some((entry) => entry.id === 'project' && entry.available));
  writeJson(path.join(EVIDENCE_ROOT, 'live_tools.json'), {
    capturedAt: new Date().toISOString(),
    serverInfo: health.serverInfo,
    protocolVersion: health.protocolVersion,
    tools: health.tools,
  });
  writeJson(path.join(EVIDENCE_ROOT, 'live_capabilities.json'), capabilities);

  const preflight = await state(mcp);
  writeJson(path.join(EVIDENCE_ROOT, 'preflight_project_state.json'), preflight);
  assert(!preflight.active, `Preflight expected no active project, found ${preflight.name}.`);
  await mcp.callTool('ensure_project', {
    action: 'ensure',
    format: 'Generic Model',
    name: PROJECT_NAME,
    match: 'format_and_name',
    onMismatch: 'error',
    onMissing: 'create',
    uvPixelsPerBlock: 4,
    ifRevision: preflight.revision,
  });

  for (const bone of BONES) await mutate(mcp, 'add_bone', bone);
  for (const cube of CUBES) {
    const { color, ...args } = cube;
    await mutate(mcp, 'add_cube', args);
  }

  const afterGeometry = await state(mcp);
  assert.strictEqual(afterGeometry.counts.bones, BONES.length);
  assert.strictEqual(afterGeometry.counts.cubes, CUBES.length);
  const visibleCubes = CUBES.filter((cube) => cube.visibility !== false);
  for (const cube of visibleCubes) {
    const extents = cube.to.map((value, axis) => value - cube.from[axis]);
    assert(extents.every((value) => value >= 2), `${cube.name} is too thin for the 4 px/block UV atlas: ${extents.join('x')}`);
  }
  assert.strictEqual(afterGeometry.textures.length, 1);
  await mutate(mcp, 'assign_texture', {
    textureId: afterGeometry.textures[0].id,
    cubeIds: visibleCubes.map((cube) => cube.id),
  });
  for (const cube of visibleCubes) {
    await mutate(mcp, 'paint_faces', {
      textureName: afterGeometry.textures[0].name,
      target: { cubeName: cube.name },
      coordSpace: 'face',
      op: {
        op: 'fill_rect',
        x: 0,
        y: 0,
        width: 64,
        height: 64,
        color: cube.color,
        shade: { enabled: true, intensity: 0.14, edge: 0.08, noise: 0.025, seed: 7100 + CUBES.indexOf(cube), lightDir: 'top_bottom' },
      },
      mapping: 'stretch',
    });
  }
  for (const clip of CLIPS) await mutate(mcp, 'create_animation_clip', clip);
  for (const pose of IDLE_POSES) {
    await mutate(mcp, 'set_frame_pose', { clip: 'idle', frame: pose.frame, bones: pose.bones, interp: 'catmullrom' });
  }
  for (const pose of LUNGE_POSES) {
    await mutate(mcp, 'set_frame_pose', { clip: 'lunge', frame: pose.frame, bones: pose.bones, interp: 'linear' });
  }

  const staleBefore = await state(mcp);
  await mcp.callTool('update_cube', { name: 'shoulder_core', inflate: 0.01, ifRevision: staleBefore.revision });
  const staleAfterFirst = await state(mcp);
  let staleGuardMode = 'client_retry_after_rejection';
  try {
    await mcp.callTool('update_cube', { name: 'shoulder_core', inflate: 0, ifRevision: staleBefore.revision });
    staleGuardMode = 'server_auto_retry';
  } catch (error) {
    assert(error instanceof ToolCallError);
    assert.strictEqual(error.code, 'invalid_state_revision_mismatch');
    await mcp.callTool('update_cube', { name: 'shoulder_core', inflate: 0, ifRevision: staleAfterFirst.revision });
  }
  const afterGuard = await state(mcp);
  writeJson(path.join(EVIDENCE_ROOT, 'revision_guard.json'), {
    mode: staleGuardMode,
    staleRevision: staleBefore.revision,
    refreshedRevision: staleAfterFirst.revision,
    finalRevision: afterGuard.revision,
  });

  const texture = await readTexture(mcp);
  assert(texture.width <= 64 && texture.height <= 64, `Texture budget exceeded: ${texture.width}x${texture.height}.`);
  const authoredState = await state(mcp);
  const authoredSemantic = normalizeState(authoredState);
  writeJson(path.join(EVIDENCE_ROOT, 'authored_project_state.json'), authoredState);
  writeJson(path.join(EVIDENCE_ROOT, 'authored_semantic_state.json'), authoredSemantic);
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: authoredState.revision });
  writeJson(path.join(EVIDENCE_ROOT, 'validate_before_export.json'), validation);
  assert.deepStrictEqual(validation.findings || [], [], 'Ashfox validate returned findings before export.');
  const exportResult = await mcp.callTool('export', {
    format: 'native_codec',
    codecId: 'project',
    destPath: SOURCE_PATH,
    options: { fallback: 'strict', includeDiagnostics: true },
    includeState: true,
    ifRevision: authoredState.revision,
  });
  assert(fs.existsSync(SOURCE_PATH), 'Native MCP export did not create the .bbmodel.');
  writeJson(path.join(EVIDENCE_ROOT, 'native_export_result.json'), exportResult);
  const sourceSha = sha256(SOURCE_PATH);
  fs.writeFileSync(path.join(EVIDENCE_ROOT, 'source.sha256'), `${sourceSha}  ${path.relative(REPO_ROOT, SOURCE_PATH)}\n`, { mode: 0o600 });

  const closeState = await state(mcp);
  await mcp.callTool('ensure_project', {
    action: 'delete',
    target: { name: PROJECT_NAME },
    force: true,
    ifRevision: closeState.revision,
  });
  await mcp.close();
  launchSource();

  mcp = createClient();
  await mcp.connect();
  const reopenedState = await waitForReopen(mcp);
  const reopenedSemantic = normalizeState(reopenedState);
  const semanticDiff = differences(authoredSemantic, reopenedSemantic);
  writeJson(path.join(EVIDENCE_ROOT, 'reopened_project_state.json'), reopenedState);
  writeJson(path.join(EVIDENCE_ROOT, 'reopened_semantic_state.json'), reopenedSemantic);
  writeJson(path.join(EVIDENCE_ROOT, 'reopen_structural_diff.json'), {
    beforeSha256: hashJson(authoredSemantic),
    afterSha256: hashJson(reopenedSemantic),
    differences: semanticDiff,
  });
  assert.deepStrictEqual(semanticDiff, [], 'Semantic structural diff after native reopen is not empty.');
  const reopenedValidation = await mcp.callTool('validate', { includeState: true, ifRevision: reopenedState.revision });
  writeJson(path.join(EVIDENCE_ROOT, 'validate_after_reopen.json'), reopenedValidation);
  await mcp.callTool('ensure_project', {
    action: 'ensure',
    format: 'Generic Model',
    name: PROJECT_NAME,
    match: 'format_and_name',
    onMismatch: 'reuse',
    onMissing: 'error',
    uvPixelsPerBlock: 4,
    ifRevision: reopenedState.revision,
    includeState: true,
  });
  const restoredUvState = await state(mcp);
  const restoredUvValidation = await mcp.callTool('validate', { includeState: true, ifRevision: restoredUvState.revision });
  writeJson(path.join(EVIDENCE_ROOT, 'validate_after_reopen_restored_uv_density.json'), restoredUvValidation);
  assert.deepStrictEqual(restoredUvValidation.findings || [], [], 'Ashfox validate returned findings after restoring session UV density.');

  const renders = [];
  for (const [fileName, options] of [
    ['front.png', { angle: [-8, 0, 0] }],
    ['side.png', { angle: [-8, 90, 0] }],
    ['back.png', { angle: [-8, 180, 0] }],
    ['three_quarter.png', { angle: [-10, 35, 0] }],
  ]) renders.push(await capture(mcp, fileName, options));

  const finalState = await state(mcp);
  const summary = {
    status: 'READY_FOR_VISUAL_REVIEW',
    generatedAt: new Date().toISOString(),
    assetSpec: { path: path.relative(REPO_ROOT, SPEC_PATH), sha256: SPEC_SHA256 },
    source: { path: path.relative(REPO_ROOT, SOURCE_PATH), sha256: sourceSha },
    texture: { path: path.relative(REPO_ROOT, TEXTURE_PATH), ...texture },
    approvedWriter: {
      id: lock.local_patch.id,
      ashfoxVersion: health.serverInfo.version,
      blockbenchVersion: capabilities.blockbenchVersion,
      protocolVersion: health.protocolVersion,
      toolRegistryHash: capabilities.toolRegistry.hash,
    },
    authoredRevision: authoredState.revision,
    reopenedRevision: reopenedState.revision,
    finalRenderRevision: finalState.revision,
    counts: finalState.counts,
    validationBeforeExport: validation,
    validationImmediatelyAfterReopen: reopenedValidation,
    validationAfterUvDensityRestore: restoredUvValidation,
    nativeExport: exportResult,
    semanticRoundtrip: {
      beforeSha256: hashJson(authoredSemantic),
      afterSha256: hashJson(reopenedSemantic),
      differences: semanticDiff,
    },
    renders,
    missingEvidence: [
      { id: 'player_scale', reason: 'Ashfox render_preview has no standard-player comparison primitive; no substitute was fabricated.' },
      { id: 'black_three_quarter', reason: 'Ashfox render_preview has no non-mutating flat-black material override; no stale or post-processed substitute was fabricated.' },
      { id: 'animation_idle_start', reason: 'Ashfox render_preview did not update timeSeconds within a selected clip; identical-frame output was rejected.' },
      { id: 'animation_idle_peak', reason: 'Ashfox render_preview did not update timeSeconds within a selected clip; identical-frame output was rejected.' },
      { id: 'animation_idle_end', reason: 'Ashfox render_preview did not update timeSeconds within a selected clip; identical-frame output was rejected.' },
      { id: 'animation_lunge_anticipation', reason: 'Ashfox render_preview did not update timeSeconds within a selected clip; identical-frame output was rejected.' },
      { id: 'animation_lunge_contact', reason: 'Ashfox render_preview did not update timeSeconds within a selected clip; identical-frame output was rejected.' },
      { id: 'animation_lunge_follow_through', reason: 'Ashfox render_preview did not update timeSeconds within a selected clip; identical-frame output was rejected.' },
      { id: 'animation_lunge_recovery', reason: 'Ashfox render_preview did not update timeSeconds within a selected clip; identical-frame output was rejected.' },
      { id: 'independent_visual_verdict', reason: 'Builder cannot issue independent visual QA or ART_PASS.' },
    ],
    claims: { artPass: false, integrationPass: false, runtimePass: false },
  };
  writeJson(path.join(EVIDENCE_ROOT, 'authoring_summary.json'), summary);
  const lines = [
    'schema_version: 1',
    'status: READY_FOR_VISUAL_REVIEW',
    `asset_spec_path: ${path.relative(REPO_ROOT, SPEC_PATH)}`,
    `asset_spec_sha256: ${SPEC_SHA256}`,
    `source_path: ${path.relative(REPO_ROOT, SOURCE_PATH)}`,
    `source_sha256: ${sourceSha}`,
    `source_revision_after_reopen: ${reopenedState.revision}`,
    'renders:',
    ...renders.map((render) => `  - id: ${path.basename(render.file, '.png')}\n    path: ${path.relative(REPO_ROOT, path.join(EVIDENCE_ROOT, render.file))}\n    sha256: ${render.sha256}\n    source_sha256: ${sourceSha}\n    project_revision: ${render.revision}\n    producer: ashfox_render_preview`),
    'missing_evidence:',
    '  - id: player_scale',
    '    reason: ashfox_has_no_standard_player_comparison_primitive',
    '  - id: black_three_quarter',
    '    reason: ashfox_has_no_non_mutating_flat_black_material_override',
    '  - id: animation_idle_start',
    '    reason: ashfox_time_seconds_not_applied_within_selected_clip',
    '  - id: animation_idle_peak',
    '    reason: ashfox_time_seconds_not_applied_within_selected_clip',
    '  - id: animation_idle_end',
    '    reason: ashfox_time_seconds_not_applied_within_selected_clip',
    '  - id: animation_lunge_anticipation',
    '    reason: ashfox_time_seconds_not_applied_within_selected_clip',
    '  - id: animation_lunge_contact',
    '    reason: ashfox_time_seconds_not_applied_within_selected_clip',
    '  - id: animation_lunge_follow_through',
    '    reason: ashfox_time_seconds_not_applied_within_selected_clip',
    '  - id: animation_lunge_recovery',
    '    reason: ashfox_time_seconds_not_applied_within_selected_clip',
    '  - id: independent_visual_verdict',
    '    reason: pending_fresh_independent_visual_critic',
    'gate_claims:',
    '  art_pass: false',
    '  integration_pass: false',
    '  runtime_pass: false',
  ];
  fs.writeFileSync(path.join(EVIDENCE_ROOT, 'render_manifest.yaml'), `${lines.join('\n')}\n`, { mode: 0o600 });
  console.log(JSON.stringify(summary, null, 2));
  await mcp.close();
}

main().catch((error) => {
  try {
    writeJson(path.join(EVIDENCE_ROOT, 'authoring_failure.json'), {
      status: 'BLOCKED',
      generatedAt: new Date().toISOString(),
      error: error.message,
      code: error.code || null,
      details: error.details || null,
      stack: error.stack,
    });
  } catch {
    // Preserve the original failure if evidence writing also fails.
  }
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
