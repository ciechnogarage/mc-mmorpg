#!/usr/bin/env node

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const {
  StreamableHttpMcpClient,
  ToolCallError,
  TransactionalBlockbenchWorker,
  loadConfiguredEndpoints,
} = require('./blockbench_mcp_worker');

const ROOT = path.resolve(__dirname, '..', '..');
const CONFIG_PATH = path.join(ROOT, '.mcp.json');
const LOCK_PATH = path.join(__dirname, 'blockbench_mcp', 'ashfox.lock.json');
const STATE_DIR = path.join(__dirname, 'runs', 'blockbench-mcp');
const EVIDENCE_DIR = path.join(STATE_DIR, 'phase1-conformance');
const PROJECT_NAME = 'phase1_ashfox_conformance';
const OUTPUT_PATH = path.join(EVIDENCE_DIR, `${PROJECT_NAME}.bbmodel`);
const INTERNAL_OUTPUT_PATH = path.join(EVIDENCE_DIR, `${PROJECT_NAME}.generic.json`);
const APPIMAGE_PATH = '/home/przemek/.local/opt/blockbench/5.1.6/Blockbench_5.1.6.AppImage';
const PLUGIN_PATH = '/home/przemek/.local/share/blockbench-plugins/ashfox/v0.2.0-mc.2/ashfox.js';
const SIDECAR_PATH = '/home/przemek/.local/share/blockbench-plugins/ashfox/v0.2.0-mc.2/ashfox-sidecar.js';

const IDS = {
  root: '11111111-1111-4111-8111-111111111111',
  body: '22222222-2222-4222-8222-222222222222',
  cube: '33333333-3333-4333-8333-333333333333',
  idle: '44444444-4444-4444-8444-444444444444',
  rollback: '55555555-5555-4555-8555-555555555555',
};

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

function structuralSnapshot(project) {
  return canonical({
    name: project.name,
    format: project.format,
    formatId: project.formatId ?? null,
    uvPixelsPerBlock: project.uvPixelsPerBlock ?? null,
    counts: project.counts,
    bones: project.bones ?? [],
    cubes: project.cubes ?? [],
    meshes: project.meshes ?? [],
    textures: project.textures ?? [],
    animations: project.animations ?? [],
  });
}

function semanticSnapshot(project) {
  const singleTextureName = project.textures?.length === 1 ? project.textures[0].name : null;
  const normalizeFace = (face) => canonical({
    ...face,
    texture: typeof face?.texture === 'string' && singleTextureName ? singleTextureName : face?.texture,
  });
  const normalizeAnimation = (animation) => canonical({
    name: animation.name,
    length: animation.length,
    loop: animation.loop,
    fps: animation.fps,
    channels: (animation.channels ?? []).map((channel) => ({
      bone: channel.bone,
      channel: channel.channel,
      keys: channel.keys.map((key) => ({
        time: Math.round(key.time * 1e9) / 1e9,
        value: key.value.map((entry) => Math.round(entry * 1e9) / 1e9),
        interp: key.interp,
      })),
    })).sort((a, b) => `${a.bone}:${a.channel}`.localeCompare(`${b.bone}:${b.channel}`)),
    triggers: animation.triggers ?? [],
  });
  return canonical({
    name: project.name,
    format: project.format,
    formatId: project.formatId ?? null,
    bones: (project.bones ?? []).map((bone) => ({
      name: bone.name,
      parent: bone.parent ?? null,
      pivot: bone.pivot,
      rotation: bone.rotation,
      scale: bone.scale,
      visibility: bone.visibility,
    })).sort((a, b) => a.name.localeCompare(b.name)),
    cubes: (project.cubes ?? []).map((cube) => ({
      name: cube.name,
      bone: cube.bone,
      from: cube.from,
      to: cube.to,
      origin: cube.origin,
      rotation: cube.rotation,
      uv: cube.uv,
      uvOffset: cube.uvOffset,
      inflate: cube.inflate,
      mirror: cube.mirror,
      visibility: cube.visibility,
      boxUv: cube.boxUv,
      shade: cube.shade,
      lightEmission: cube.lightEmission,
      rescale: cube.rescale,
      faces: Object.fromEntries(
        Object.entries(cube.faces ?? {}).map(([direction, face]) => [direction, normalizeFace(face)]),
      ),
    })).sort((a, b) => a.name.localeCompare(b.name)),
    meshes: (project.meshes ?? []).map((mesh) => ({
      name: mesh.name,
      bone: mesh.bone ?? null,
      origin: mesh.origin,
      rotation: mesh.rotation,
      visibility: mesh.visibility,
      uvPolicy: mesh.uvPolicy,
      vertices: mesh.vertices.map((vertex) => vertex.pos),
      faces: mesh.faces.map((face) => ({
        vertexCount: face.vertices.length,
        uv: face.uv?.map((entry) => entry.uv),
        texture: typeof face.texture === 'string' && singleTextureName ? singleTextureName : face.texture,
      })),
    })).sort((a, b) => a.name.localeCompare(b.name)),
    textures: (project.textures ?? []).map((texture) => ({
      name: texture.name,
      width: texture.width,
      height: texture.height,
    })).sort((a, b) => a.name.localeCompare(b.name)),
    animations: (project.animations ?? []).map(normalizeAnimation).sort((a, b) => a.name.localeCompare(b.name)),
  });
}

function hashJson(value) {
  return crypto.createHash('sha256').update(`${JSON.stringify(value)}\n`).digest('hex');
}

function structuralDifferences(before, after, currentPath = '$', differences = []) {
  if (differences.length >= 200) return differences;
  if (Object.is(before, after)) return differences;
  if (Array.isArray(before) && Array.isArray(after)) {
    if (before.length !== after.length) {
      differences.push({ path: `${currentPath}.length`, before: before.length, after: after.length });
    }
    const length = Math.max(before.length, after.length);
    for (let index = 0; index < length; index += 1) {
      structuralDifferences(before[index], after[index], `${currentPath}[${index}]`, differences);
    }
    return differences;
  }
  if (before && after && typeof before === 'object' && typeof after === 'object') {
    const keys = [...new Set([...Object.keys(before), ...Object.keys(after)])].sort();
    for (const key of keys) {
      structuralDifferences(before[key], after[key], `${currentPath}.${key}`, differences);
    }
    return differences;
  }
  differences.push({ path: currentPath, before: before ?? null, after: after ?? null });
  return differences;
}

function projectFrom(value) {
  return value?.project || value?.state || value;
}

function endpoint() {
  const endpoints = loadConfiguredEndpoints(CONFIG_PATH);
  const ashfox = endpoints.find((entry) => entry.name.includes('ashfox'));
  assert(ashfox, 'The local .mcp.json must define a loopback Ashfox endpoint.');
  return ashfox;
}

function client() {
  return new StreamableHttpMcpClient(endpoint(), { timeoutMs: 15000 });
}

async function state(mcp) {
  return projectFrom(await mcp.callTool('get_project_state', { detail: 'full', includeUsage: true }));
}

async function mutation(mcp, name, args) {
  const current = await state(mcp);
  return mcp.callTool(name, { ...args, ifRevision: current.revision });
}

function copyEvidence(sourcePath, targetName) {
  assert(sourcePath && fs.existsSync(sourcePath), `Ashfox evidence file is missing: ${sourcePath}`);
  const targetPath = path.join(EVIDENCE_DIR, targetName);
  fs.copyFileSync(sourcePath, targetPath);
  return targetPath;
}

function createWorkerJob(jobId, exportPath) {
  return {
    jobId,
    specRevisionId: 'phase1-conformance-spec-v1',
    project: { name: PROJECT_NAME },
    batches: [
      {
        batchId: 'rig',
        operations: [
          {
            operationId: 'add-phase1-root',
            tool: 'add_bone',
            arguments: { id: IDS.root, name: 'phase1_root', pivot: [0, 0, 0] },
            expect: { collection: 'bones', name: 'phase1_root' },
            inverse: { tool: 'delete_bone', arguments: { name: 'phase1_root' } },
          },
          {
            operationId: 'add-phase1-body',
            tool: 'add_bone',
            arguments: {
              id: IDS.body,
              name: 'phase1_body',
              parentId: IDS.root,
              pivot: [0, 8, 0],
            },
            expect: { collection: 'bones', name: 'phase1_body' },
            inverse: { tool: 'delete_bone', arguments: { name: 'phase1_body' } },
          },
        ],
      },
      {
        batchId: 'geometry-animation',
        operations: [
          {
            operationId: 'add-phase1-body-cube',
            tool: 'add_cube',
            arguments: {
              id: IDS.cube,
              name: 'phase1_body_cube',
              from: [-4, 0, -2],
              to: [4, 8, 2],
              origin: [0, 8, 0],
              boneId: IDS.body,
              boxUv: true,
              uvOffset: [0, 0],
            },
            expect: { collection: 'cubes', name: 'phase1_body_cube' },
            inverse: { tool: 'delete_cube', arguments: { name: 'phase1_body_cube' } },
          },
          {
            operationId: 'add-phase1-idle',
            tool: 'create_animation_clip',
            arguments: {
              id: IDS.idle,
              name: 'phase1_idle',
              length: 1,
              loop: true,
              fps: 20,
            },
            expect: { collection: 'animations', name: 'phase1_idle' },
            inverse: { tool: 'delete_animation_clip', arguments: { name: 'phase1_idle' } },
          },
        ],
      },
    ],
    validate: { includeState: true },
    render: { mode: 'fixed', output: 'single', angle: [20, 35, 0] },
    export: {
      format: 'generic_model_json',
      destPath: exportPath,
      options: { fallback: 'strict', includeDiagnostics: true },
    },
  };
}

function rollbackJob(jobId) {
  return {
    jobId,
    specRevisionId: 'phase1-rollback-spec-v1',
    project: { name: PROJECT_NAME },
    batches: [{
      batchId: 'forced-rollback',
      operations: [{
        operationId: 'add-phase1-rollback-bone',
        tool: 'add_bone',
        arguments: { id: IDS.rollback, name: 'phase1_rollback_probe', pivot: [0, 0, 0] },
        expect: { collection: 'bones', name: 'phase1_rollback_probe' },
        inverse: { tool: 'delete_bone', arguments: { name: 'phase1_rollback_probe' } },
      }],
    }],
    validate: {},
    render: { mode: 'fixed', output: 'single' },
    export: {
      format: 'generic_model_json',
      destPath: '/proc/ashfox-phase1-denied.json',
      options: { fallback: 'strict', includeDiagnostics: true },
    },
  };
}

async function ensureIsolatedProject(mcp) {
  const current = await state(mcp);
  if (current.active) {
    assert.strictEqual(current.name, PROJECT_NAME, 'Refusing to touch a non-conformance Blockbench project.');
    return current;
  }
  await mcp.callTool('ensure_project', {
    action: 'ensure',
    format: 'Generic Model',
    name: PROJECT_NAME,
    match: 'format_and_name',
    onMismatch: 'create',
    onMissing: 'create',
    uvPixelsPerBlock: 16,
    ifRevision: current.revision,
  });
  const created = await state(mcp);
  assert.strictEqual(created.active, true);
  assert.strictEqual(created.name, PROJECT_NAME);
  assert.strictEqual(created.format, 'Generic Model');
  assert.strictEqual(created.textures.length, 1, 'ensure_project must auto-create one texture.');
  return created;
}

async function exerciseRevisionGuard(mcp) {
  const first = await state(mcp);
  await mcp.callTool('update_cube', { id: IDS.cube, inflate: 0.125, ifRevision: first.revision });
  const afterFirst = await state(mcp);
  let mode = 'client_retry';
  try {
    await mcp.callTool('update_cube', { id: IDS.cube, inflate: 0, ifRevision: first.revision });
    mode = 'server_retry';
  } catch (error) {
    assert(error instanceof ToolCallError);
    assert.strictEqual(error.code, 'invalid_state_revision_mismatch');
    await mcp.callTool('update_cube', { id: IDS.cube, inflate: 0, ifRevision: afterFirst.revision });
  }
  const final = await state(mcp);
  assert.strictEqual(final.cubes.filter((entry) => entry.id === IDS.cube).length, 1);
  return { mode, staleRevision: first.revision, refreshedRevision: afterFirst.revision, finalRevision: final.revision };
}

function openExportInBlockbench() {
  const env = {
    ...process.env,
    ASHFOX_HOST: '127.0.0.1',
    ASHFOX_PORT: '8788',
    ASHFOX_PATH: '/mcp',
    XDG_CONFIG_HOME: '/home/przemek/.local/state/mc-ashfox-conformance/xdg-config',
    XDG_CACHE_HOME: '/home/przemek/.local/state/mc-ashfox-conformance/xdg-cache',
    XDG_DATA_HOME: '/home/przemek/.local/state/mc-ashfox-conformance/xdg-data',
  };
  const child = spawn(APPIMAGE_PATH, [
    OUTPUT_PATH,
    '--ozone-platform=x11',
    '--disable-vulkan',
    '--use-gl=angle',
    '--use-angle=swiftshader',
    '--enable-unsafe-swiftshader',
  ], { env, detached: true, stdio: 'ignore' });
  child.unref();
}

async function waitForReopen(mcp) {
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    const current = await state(mcp);
    if (current.active && current.name === PROJECT_NAME) return current;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error('Timed out waiting for Blockbench to reopen the exported .bbmodel.');
}

async function closeConformanceProject(mcp) {
  const current = await state(mcp);
  if (!current.active) return;
  assert.strictEqual(current.name, PROJECT_NAME, 'Refusing to close a non-conformance Blockbench project.');
  await mcp.callTool('ensure_project', {
    action: 'delete',
    target: { name: PROJECT_NAME },
    force: true,
    ifRevision: current.revision,
  });
}

async function main() {
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
  for (const target of [
    OUTPUT_PATH,
    INTERNAL_OUTPUT_PATH,
    path.join(EVIDENCE_DIR, 'structural-before.json'),
    path.join(EVIDENCE_DIR, 'structural-after.json'),
    path.join(EVIDENCE_DIR, 'structural-diff.json'),
    path.join(EVIDENCE_DIR, 'semantic-before.json'),
    path.join(EVIDENCE_DIR, 'semantic-after.json'),
    path.join(EVIDENCE_DIR, 'semantic-diff.json'),
    path.join(EVIDENCE_DIR, 'reopened-preview.png'),
    path.join(EVIDENCE_DIR, 'reopened-texture.png'),
  ]) {
    fs.rmSync(target, { force: true });
  }
  const lock = JSON.parse(fs.readFileSync(LOCK_PATH, 'utf8'));
  const artifactHashes = {
    blockbench: sha256(APPIMAGE_PATH),
    ashfox: sha256(PLUGIN_PATH),
    ashfoxSidecar: sha256(SIDECAR_PATH),
    ashfoxProfileLoader: sha256(lock.local_patch.isolated_profile_loader_path),
    ashfoxCachedUpstream: sha256(lock.local_patch.cached_upstream_plugin_path),
  };
  assert.strictEqual(artifactHashes.blockbench, lock.blockbench.appimage_sha256);
  assert.strictEqual(artifactHashes.ashfox, lock.local_patch.plugin_sha256);
  assert.strictEqual(artifactHashes.ashfoxSidecar, lock.local_patch.sidecar_sha256);
  assert.strictEqual(artifactHashes.ashfoxProfileLoader, lock.local_patch.plugin_sha256);
  assert.strictEqual(artifactHashes.ashfoxCachedUpstream, lock.ashfox.plugin_sha256);
  assert.strictEqual(
    fs.readlinkSync(lock.local_patch.isolated_profile_loader_path),
    lock.local_patch.isolated_profile_loader_target,
  );
  for (const patch of lock.local_patch.patches) {
    assert.strictEqual(sha256(path.join(ROOT, patch.patch_path)), patch.patch_sha256);
    const manifest = JSON.parse(fs.readFileSync(path.join(ROOT, patch.manifest_path), 'utf8'));
    assert.strictEqual(manifest.patch.sha256, patch.patch_sha256);
  }

  let mcp = client();
  const health = await mcp.connect();
  assert.strictEqual(health.serverInfo?.name, 'ashfox');
  assert.strictEqual(health.serverInfo?.version, lock.ashfox.version);
  assert.strictEqual(health.tools.length, lock.expected_tool_count);
  const capabilities = await mcp.callTool('list_capabilities', {});
  assert.strictEqual(capabilities.pluginVersion, lock.ashfox.version);
  assert.strictEqual(capabilities.blockbenchVersion, lock.blockbench.version);
  assert.strictEqual(capabilities.toolRegistry.hash, lock.expected_tool_registry_hash);
  assert(capabilities.exportTargets.some((entry) => entry.id === 'project' && entry.available));
  await ensureIsolatedProject(mcp);
  await mcp.close();

  const runId = Date.now();
  const rollback = await new TransactionalBlockbenchWorker({
    configPath: CONFIG_PATH,
    stateDir: STATE_DIR,
    timeoutMs: 15000,
  }).run(rollbackJob(`phase1-rollback-${runId}`));
  assert.strictEqual(rollback.status, 'BLOCKED_TOOLING');
  assert.strictEqual(rollback.restored, true);

  const job = createWorkerJob(`phase1-success-${runId}`, INTERNAL_OUTPUT_PATH);
  const firstRun = await new TransactionalBlockbenchWorker({
    configPath: CONFIG_PATH,
    stateDir: STATE_DIR,
    timeoutMs: 15000,
  }).run(job);
  assert.strictEqual(firstRun.status, 'COMPLETED');
  const resumedRun = await new TransactionalBlockbenchWorker({
    configPath: CONFIG_PATH,
    stateDir: STATE_DIR,
    timeoutMs: 15000,
  }).run(job);
  assert.strictEqual(resumedRun.status, 'COMPLETED');

  mcp = client();
  await mcp.connect();
  const revisionGuard = await exerciseRevisionGuard(mcp);
  const current = await state(mcp);
  const texture = current.textures[0];
  await mutation(mcp, 'assign_texture', { textureId: texture.id, cubeIds: [IDS.cube] });
  for (const [face, color, width, height] of [
    ['north', '#5f9b45', 8, 8],
    ['south', '#4f7f39', 8, 8],
    ['east', '#78b75a', 4, 8],
    ['west', '#426b32', 4, 8],
    ['up', '#91ca70', 8, 4],
    ['down', '#304f27', 8, 4],
  ]) {
    await mutation(mcp, 'paint_faces', {
      textureId: texture.id,
      target: { cubeId: IDS.cube, face },
      coordSpace: 'face',
      op: { op: 'fill_rect', x: 0, y: 0, width, height, color, shade: false },
    });
  }
  await mutation(mcp, 'set_frame_pose', {
    clipId: IDS.idle,
    clip: 'phase1_idle',
    frame: 0,
    bones: [{ name: 'phase1_body', rot: [0, 0, 0], interp: 'linear' }],
  });
  await mutation(mcp, 'set_frame_pose', {
    clipId: IDS.idle,
    clip: 'phase1_idle',
    frame: 10,
    bones: [{ name: 'phase1_body', rot: [0, 10, 0], interp: 'linear' }],
  });
  const beforeExport = await state(mcp);
  const validation = await mcp.callTool('validate', { includeState: true, ifRevision: beforeExport.revision });
  const preview = await mcp.callTool('render_preview', {
    mode: 'fixed',
    output: 'single',
    angle: [20, 35, 0],
    saveToTmp: true,
    tmpPrefix: 'phase1',
    tmpName: 'authored',
  });
  const textureRead = await mcp.callTool('read_texture', {
    id: texture.id,
    saveToTmp: true,
    tmpPrefix: 'phase1',
    tmpName: 'texture',
  });
  await mcp.callTool('export', {
    format: 'generic_model_json',
    destPath: INTERNAL_OUTPUT_PATH,
    options: { fallback: 'strict', includeDiagnostics: true },
    ifRevision: beforeExport.revision,
  });
  assert(fs.existsSync(INTERNAL_OUTPUT_PATH));
  const authoredPreview = copyEvidence(preview.saved?.image?.path, 'authored-preview.png');
  const textureEvidence = copyEvidence(textureRead.saved?.texture?.path, 'texture.png');
  await mcp.callTool('export_trace_log', {
    mode: 'writeFile',
    destPath: path.join(EVIDENCE_DIR, 'ashfox-trace.ndjson'),
  });

  const authoredState = await state(mcp);
  let nativeExportError = null;
  try {
    await mcp.callTool('export', {
      format: 'native_codec',
      codecId: 'project',
      destPath: OUTPUT_PATH,
      options: { fallback: 'strict', includeDiagnostics: true },
      ifRevision: authoredState.revision,
    });
  } catch (error) {
    nativeExportError = {
      code: error.code || null,
      message: error.message,
      details: error.details || null,
    };
    assert.strictEqual(error.code, 'unsupported_format');
    assert.match(error.message, /Native codec not available: project/);
  }

  let reopenedState = null;
  let reopenedValidation = null;
  let reopenedPreview = null;
  let reopenedTextureEvidence = null;
  let reopenedResume = null;
  if (!nativeExportError) {
    assert(fs.existsSync(OUTPUT_PATH));
    await closeConformanceProject(mcp);
    await mcp.close();
    openExportInBlockbench();

    mcp = client();
    await mcp.connect();
    reopenedState = await waitForReopen(mcp);
    reopenedValidation = await mcp.callTool('validate', {
      includeState: true,
      ifRevision: reopenedState.revision,
    });
    const reopenedPreviewResult = await mcp.callTool('render_preview', {
      mode: 'fixed',
      output: 'single',
      angle: [20, 35, 0],
      saveToTmp: true,
      tmpPrefix: 'phase1',
      tmpName: 'reopened',
    });
    reopenedPreview = copyEvidence(reopenedPreviewResult.saved?.image?.path, 'reopened-preview.png');
    const reopenedTexture = await mcp.callTool('read_texture', {
      id: reopenedState.textures[0].id,
      saveToTmp: true,
      tmpPrefix: 'phase1',
      tmpName: 'reopened-texture',
    });
    reopenedTextureEvidence = copyEvidence(reopenedTexture.saved?.texture?.path, 'reopened-texture.png');
    await mcp.close();
    reopenedResume = await new TransactionalBlockbenchWorker({
      configPath: CONFIG_PATH,
      stateDir: STATE_DIR,
      timeoutMs: 15000,
    }).run(job);
    assert.strictEqual(reopenedResume.status, 'COMPLETED');
    mcp = client();
    await mcp.connect();
    reopenedState = await state(mcp);
    assert.deepStrictEqual(reopenedState.counts, authoredState.counts);
  }

  let structuralDiff = {
    status: 'NOT_RUN_BLOCKED_BY_NATIVE_EXPORT',
    before_sha256: null,
    after_sha256: null,
    differences: [],
  };
  let semanticDiff = {
    status: 'NOT_RUN_BLOCKED_BY_NATIVE_EXPORT',
    before_sha256: null,
    after_sha256: null,
    differences: [],
  };
  if (reopenedState) {
    const authoredStructure = structuralSnapshot(authoredState);
    const reopenedStructure = structuralSnapshot(reopenedState);
    const differences = structuralDifferences(authoredStructure, reopenedStructure);
    structuralDiff = {
      status: differences.length === 0 ? 'PASS' : 'FAIL',
      before_sha256: hashJson(authoredStructure),
      after_sha256: hashJson(reopenedStructure),
      differences,
    };
    const authoredSemantics = semanticSnapshot(authoredState);
    const reopenedSemantics = semanticSnapshot(reopenedState);
    const semanticDifferences = structuralDifferences(authoredSemantics, reopenedSemantics);
    semanticDiff = {
      status: semanticDifferences.length === 0 ? 'PASS' : 'FAIL',
      before_sha256: hashJson(authoredSemantics),
      after_sha256: hashJson(reopenedSemantics),
      differences: semanticDifferences,
    };
    fs.writeFileSync(
      path.join(EVIDENCE_DIR, 'structural-before.json'),
      `${JSON.stringify(authoredStructure, null, 2)}\n`,
      { mode: 0o600 },
    );
    fs.writeFileSync(
      path.join(EVIDENCE_DIR, 'structural-after.json'),
      `${JSON.stringify(reopenedStructure, null, 2)}\n`,
      { mode: 0o600 },
    );
    fs.writeFileSync(
      path.join(EVIDENCE_DIR, 'structural-diff.json'),
      `${JSON.stringify(structuralDiff, null, 2)}\n`,
      { mode: 0o600 },
    );
    fs.writeFileSync(
      path.join(EVIDENCE_DIR, 'semantic-before.json'),
      `${JSON.stringify(authoredSemantics, null, 2)}\n`,
      { mode: 0o600 },
    );
    fs.writeFileSync(
      path.join(EVIDENCE_DIR, 'semantic-after.json'),
      `${JSON.stringify(reopenedSemantics, null, 2)}\n`,
      { mode: 0o600 },
    );
    fs.writeFileSync(
      path.join(EVIDENCE_DIR, 'semantic-diff.json'),
      `${JSON.stringify(semanticDiff, null, 2)}\n`,
      { mode: 0o600 },
    );
    assert.strictEqual(reopenedState.bones.length, 2);
    assert.strictEqual(reopenedState.cubes.length, 1);
    assert.strictEqual(reopenedState.textures.length, 1);
    assert.strictEqual(reopenedState.animations.length, 1);
  }

  await closeConformanceProject(mcp);
  await mcp.close();

  if (!nativeExportError) {
    const bbmodel = JSON.parse(fs.readFileSync(OUTPUT_PATH, 'utf8'));
    assert(Array.isArray(bbmodel.elements) && bbmodel.elements.length === 1);
    assert(Array.isArray(bbmodel.outliner) && bbmodel.outliner.length >= 1);
    assert(Array.isArray(bbmodel.animations) && bbmodel.animations.length === 1);
  }

  const textureRoundtrip = reopenedTextureEvidence
    ? { status: sha256(textureEvidence) === sha256(reopenedTextureEvidence) ? 'PASS' : 'FAIL', before_sha256: sha256(textureEvidence), after_sha256: sha256(reopenedTextureEvidence) }
    : { status: 'NOT_RUN_BLOCKED_BY_NATIVE_EXPORT', before_sha256: sha256(textureEvidence), after_sha256: null };
  const passed = nativeExportError === null && semanticDiff.status === 'PASS' && textureRoundtrip.status === 'PASS';
  const summary = {
    schema_version: 1,
    status: passed ? 'CONFORMANCE_PASS' : 'CONFORMANCE_FAIL',
    generated_at: new Date().toISOString(),
    isolation: {
      project: PROJECT_NAME,
      endpoint: endpoint().url,
      staging_touched: false,
      server_started: false,
    },
    pins: lock,
    observed: {
      protocol_version: health.protocolVersion,
      server_info: health.serverInfo,
      tool_count: health.tools.length,
      tool_registry_hash: capabilities.toolRegistry.hash,
      artifact_hashes: artifactHashes,
      revision_guard: revisionGuard,
      rollback: { status: rollback.status, restored: rollback.restored, journal_path: rollback.journal_path },
      transactional_run: firstRun,
      idempotent_resume: resumedRun,
      reopened_idempotent_resume: reopenedResume,
      authored_counts: authoredState.counts,
      internal_export_sha256: sha256(INTERNAL_OUTPUT_PATH),
      native_bbmodel_export: nativeExportError
        ? { status: 'FAIL', error: nativeExportError }
        : { status: 'PASS', bbmodel_sha256: sha256(OUTPUT_PATH) },
      reopened_counts: reopenedState?.counts || null,
      roundtrip_status: semanticDiff.status,
      strict_structural_diff: structuralDiff,
      semantic_structural_diff: semanticDiff,
      texture_roundtrip: textureRoundtrip,
      authored_preview_sha256: sha256(authoredPreview),
      reopened_preview_sha256: reopenedPreview ? sha256(reopenedPreview) : null,
      texture_sha256: sha256(textureEvidence),
      reopened_texture_sha256: reopenedTextureEvidence ? sha256(reopenedTextureEvidence) : null,
      validation,
      reopened_validation: reopenedValidation,
    },
    claims: {
      tooling_conformance: passed,
      partial_tooling_checks: true,
      art_pass: false,
      integration_pass: false,
      runtime_pass: false,
    },
  };
  fs.writeFileSync(path.join(EVIDENCE_DIR, 'summary.json'), `${JSON.stringify(summary, null, 2)}\n`, { mode: 0o600 });
  fs.rmSync(path.join(EVIDENCE_DIR, 'failure.json'), { force: true });
  console.log(JSON.stringify(summary, null, 2));
  if (!passed) process.exitCode = 2;
}

main().catch(async (error) => {
  try {
    const cleanupClient = client();
    await cleanupClient.connect();
    await closeConformanceProject(cleanupClient);
    await cleanupClient.close();
  } catch {
    // The isolated profile is the containment boundary when cleanup cannot connect.
  }
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true });
  fs.writeFileSync(path.join(EVIDENCE_DIR, 'failure.json'), `${JSON.stringify({
    status: 'CONFORMANCE_FAIL',
    generated_at: new Date().toISOString(),
    error: error.message,
    stack: error.stack,
  }, null, 2)}\n`, { mode: 0o600 });
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
