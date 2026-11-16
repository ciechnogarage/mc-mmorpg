#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const {
  StreamableHttpMcpClient,
  ToolCallError,
  loadConfiguredEndpoints,
} = require('../../blockbench_mcp_worker');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const EVIDENCE_ROOT = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const SOURCE_PATH = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE_PATH = path.join(__dirname, 'phase2_root_lurker_pilot.png');
const SPEC_PATH = path.join(REPO_ROOT, 'MCMMORPG/_validation/asset_specs/phase2_root_lurker_pilot.assetspec.yaml');
const CONFIG_PATH = path.join(REPO_ROOT, '.mcp.json');
const PROJECT_NAME = 'phase2_root_lurker_pilot';
const SPEC_SHA256 = 'f708666ffbec1b68615f0753e23c5d5f83dc3f6959a45c7c12cd563dcad82546';

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
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
}

function projectFrom(value) {
  return value?.project || value?.state || value;
}

async function state(mcp) {
  return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
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

function writePng(raw, filePath) {
  const block = raw?.content?.find((entry) => entry.type === 'image' && entry.data);
  assert(block && block.mimeType === 'image/png', `Missing PNG image content for ${path.basename(filePath)}.`);
  fs.writeFileSync(filePath, Buffer.from(block.data, 'base64'), { mode: 0o600 });
  return raw.structuredContent || null;
}

async function capture(mcp, fileName, options) {
  const current = await state(mcp);
  assert.strictEqual(current.name, PROJECT_NAME);
  const raw = await mcp.request('tools/call', {
    name: 'render_preview',
    arguments: { mode: 'fixed', output: 'single', ...options, includeState: true, ifRevision: current.revision },
  });
  if (raw?.isError) throw new ToolCallError(`render_preview failed for ${fileName}`, raw.structuredContent || {});
  const filePath = path.join(EVIDENCE_ROOT, fileName);
  const metadata = writePng(raw, filePath);
  return { file: fileName, sha256: sha256(filePath), revision: current.revision, metadata };
}

async function main() {
  assert.strictEqual(sha256(SPEC_PATH), SPEC_SHA256);
  assert(fs.existsSync(SOURCE_PATH), 'Native MCP source export is missing.');
  assert(fs.existsSync(TEXTURE_PATH), 'MCP texture snapshot is missing.');
  const endpoints = loadConfiguredEndpoints(CONFIG_PATH);
  const endpoint = endpoints.find((entry) => entry.name === 'blockbench-ashfox');
  assert(endpoint && endpoint.url === 'http://127.0.0.1:8788/mcp');
  const mcp = new StreamableHttpMcpClient(endpoint, { timeoutMs: 30000 });
  const health = await mcp.connect();
  const capabilities = await mcp.callTool('list_capabilities', {});
  assert.strictEqual(health.serverInfo?.name, 'ashfox');
  assert.strictEqual(health.serverInfo?.version, '0.2.0');
  assert.strictEqual(capabilities.blockbenchVersion, '5.1.6');

  let current = await state(mcp);
  assert(current.active && current.name === PROJECT_NAME, `Expected reopened ${PROJECT_NAME}, found ${current.name}.`);
  const beforeRestore = { revision: current.revision, uvPixelsPerBlock: current.uvPixelsPerBlock ?? null };
  const ensure = await mcp.callTool('ensure_project', {
    action: 'ensure',
    format: 'Generic Model',
    name: PROJECT_NAME,
    match: 'format_and_name',
    onMismatch: 'reuse',
    onMissing: 'error',
    uvPixelsPerBlock: 4,
    ifRevision: current.revision,
    includeState: true,
  });
  current = await state(mcp);
  assert.strictEqual(current.uvPixelsPerBlock, 4);
  writeJson(path.join(EVIDENCE_ROOT, 'uv_density_restore.json'), {
    reason: 'native project codec omits optional session uvPixelsPerBlock metadata',
    sourceChanged: false,
    before: beforeRestore,
    after: { revision: current.revision, uvPixelsPerBlock: current.uvPixelsPerBlock },
    ensure,
  });
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: current.revision });
  writeJson(path.join(EVIDENCE_ROOT, 'validate_after_reopen_restored_uv_density.json'), validation);
  assert.deepStrictEqual(validation.findings || [], []);

  const authoredSemantic = JSON.parse(fs.readFileSync(path.join(EVIDENCE_ROOT, 'authored_semantic_state.json'), 'utf8'));
  const reopenedSemantic = normalizeState(current);
  const semanticDiff = differences(authoredSemantic, reopenedSemantic);
  writeJson(path.join(EVIDENCE_ROOT, 'reopened_semantic_state.json'), reopenedSemantic);
  writeJson(path.join(EVIDENCE_ROOT, 'reopen_structural_diff.json'), {
    beforeSha256: hashJson(authoredSemantic),
    afterSha256: hashJson(reopenedSemantic),
    differences: semanticDiff,
  });
  assert.deepStrictEqual(semanticDiff, []);

  const renders = [];
  for (const [fileName, options] of [
    ['front.png', { angle: [-8, 0, 0] }],
    ['side.png', { angle: [-8, 90, 0] }],
    ['back.png', { angle: [-8, 180, 0] }],
    ['three_quarter.png', { angle: [-10, 35, 0] }],
  ]) renders.push(await capture(mcp, fileName, options));

  const sourceSha = sha256(SOURCE_PATH);
  const finalState = await state(mcp);
  const summary = {
    status: 'READY_FOR_VISUAL_REVIEW',
    generatedAt: new Date().toISOString(),
    assetSpec: { path: path.relative(REPO_ROOT, SPEC_PATH), sha256: SPEC_SHA256 },
    source: { path: path.relative(REPO_ROOT, SOURCE_PATH), sha256: sourceSha },
    texture: { path: path.relative(REPO_ROOT, TEXTURE_PATH), sha256: sha256(TEXTURE_PATH), width: 32, height: 32 },
    approvedWriter: {
      id: 'ashfox-v0.2.0-native-roundtrip+mc.2',
      ashfoxVersion: health.serverInfo.version,
      blockbenchVersion: capabilities.blockbenchVersion,
      protocolVersion: health.protocolVersion,
      toolRegistryHash: capabilities.toolRegistry.hash,
    },
    counts: finalState.counts,
    nativeExportResult: JSON.parse(fs.readFileSync(path.join(EVIDENCE_ROOT, 'native_export_result.json'), 'utf8')),
    validationBeforeExport: JSON.parse(fs.readFileSync(path.join(EVIDENCE_ROOT, 'validate_before_export.json'), 'utf8')),
    validationImmediatelyAfterReopen: JSON.parse(fs.readFileSync(path.join(EVIDENCE_ROOT, 'validate_after_reopen.json'), 'utf8')),
    validationAfterUvDensityRestore: validation,
    semanticRoundtrip: { beforeSha256: hashJson(authoredSemantic), afterSha256: hashJson(reopenedSemantic), differences: semanticDiff },
    renders,
    missingEvidence: [
      { id: 'player_scale', reason: 'Ashfox render_preview has no standard-player comparison primitive; no substitute was fabricated.' },
      { id: 'black_three_quarter', reason: 'Ashfox render_preview has no non-mutating flat-black material override; no substitute was fabricated.' },
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
  fs.writeFileSync(path.join(EVIDENCE_ROOT, 'source.sha256'), `${sourceSha}  ${path.relative(REPO_ROOT, SOURCE_PATH)}\n`, { mode: 0o600 });
  const lines = [
    'schema_version: 1',
    'status: READY_FOR_VISUAL_REVIEW',
    `asset_spec_path: ${path.relative(REPO_ROOT, SPEC_PATH)}`,
    `asset_spec_sha256: ${SPEC_SHA256}`,
    `source_path: ${path.relative(REPO_ROOT, SOURCE_PATH)}`,
    `source_sha256: ${sourceSha}`,
    `source_revision_after_reopen: ${finalState.revision}`,
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
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
