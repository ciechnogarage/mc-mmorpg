const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { spawnSync } = require('child_process');
const { PHASES } = require('./modelengine_phase_utils');

const DEFAULT_CORPUS = '/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)';
const DEFAULT_OUTPUT = path.join(__dirname, 'reference_corpus', 'visual_atlas_v2');
const SINGLE_RENDER_SCRIPT = path.join(__dirname, 'render_single_model_reference.js');
const QUALITY_ROOT = path.join(__dirname, 'model_quality');

function arg(name, fallback = null) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 ? process.argv[index + 1] : fallback;
}

function safeName(value) {
  return String(value).replace(/[^A-Za-z0-9_.-]+/g, '_');
}

function animationPhaseTimes(animation) {
  const animators = Object.values(animation.animators || {});
  const keyframes = animators.flatMap((animator) => animator.keyframes || []);
  const length = Number(animation.length || Math.max(0, ...keyframes.map((frame) => Number(frame.time || 0))));
  const timeline = keyframes
    .filter((frame) => frame.channel === 'timeline')
    .map((frame) => Number(frame.time || 0))
    .filter(Number.isFinite);
  const motion = keyframes
    .filter((frame) => ['rotation', 'position', 'scale'].includes(frame.channel))
    .map((frame) => ({
      time: Number(frame.time || 0),
      energy: (frame.data_points || []).reduce((sum, point) => sum +
        ['x', 'y', 'z'].reduce((axisSum, axis) => axisSum + Math.abs(Number(point[axis]) || 0), 0), 0),
    }))
    .filter((frame) => frame.time > 0 && frame.time < length);
  const impact = timeline.length
    ? [...timeline].sort((a, b) => Math.abs(a - length * 0.6) - Math.abs(b - length * 0.6))[0]
    : motion.length
      ? [...motion].sort((a, b) => b.energy - a.energy)[0].time
      : length * 0.8;
  const times = [...new Set(keyframes.map((frame) => Number(frame.time || 0)).filter(Number.isFinite))].sort((a, b) => a - b);
  const before = times.filter((time) => time < impact);
  const after = times.filter((time) => time > impact);
  const anticipation = before.length ? before[Math.max(0, before.length - 2)] : impact * 0.5;
  const recovery = after.length ? after[Math.min(after.length - 1, 1)] : impact + (length - impact) * 0.5;
  return {
    start: 0,
    anticipation,
    impact,
    recovery,
    end: length,
  };
}

function walkBlueprints(root, acc = []) {
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) walkBlueprints(full, acc);
    else if (entry.isFile() && entry.name.toLowerCase().endsWith('.bbmodel')) acc.push(full);
  }
  return acc;
}

function writeManifest(output, manifest) {
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(path.join(output, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
}

function sha1(file) {
  return crypto.createHash('sha1').update(fs.readFileSync(file)).digest('hex');
}

function sha1Text(value) {
  return crypto.createHash('sha1').update(String(value)).digest('hex');
}

function normalizeName(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/^animation\.[^.]+\./, '')
    .replace(/[0-9]+/g, '#')
    .replace(/[.\s-]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function familyRoot(id) {
  const lower = normalizeName(id);
  return lower
    .replace(/(?:^|_)(parts?|damage|damaged|pet|pets|minion|summon|summons|vfx|fx|projectile|projectiles|gibs?|telegraph|ground(?:_?fx|_?crack)?|splash|portal|whirlpool|star|book|book_page|sword|default|egg)$/g, '')
    .replace(/(?:_variant|_phase#|_#seg|_#|_d)$/g, '')
    .replace(/^littleroom_/, '')
    .replace(/^lr_/, 'lr_')
    .replace(/__+/g, '_')
    .replace(/^_+|_+$/g, '') || lower;
}

function loadBossIds() {
  if (!fs.existsSync(QUALITY_ROOT)) return new Set();
  const files = fs.readdirSync(QUALITY_ROOT).filter((file) => file.endsWith('.json'));
  const bossIds = new Set();
  for (const file of files) {
    const full = path.join(QUALITY_ROOT, file);
    try {
      const manifest = JSON.parse(fs.readFileSync(full, 'utf8'));
      if (manifest.profile !== 'boss') continue;
      const basename = path.basename(file, '.quality.json');
      bossIds.add(normalizeName(basename));
    } catch {
      continue;
    }
  }
  return bossIds;
}

function buildFallbackReport(fallbacks, bossIds, framesExpected) {
  const totals = {
    totalFallbacks: fallbacks.length,
    fallbackRate: Number((fallbacks.length / Math.max(framesExpected, 1)).toFixed(6)),
    byReason: {},
    byView: {},
    byModel: {},
    byFamily: {},
    bossFallbackFrames: 0,
    staticViewFallbackFrames: 0,
  };
  for (const fallback of fallbacks) {
    totals.byReason[fallback.reason] = (totals.byReason[fallback.reason] || 0) + 1;
    totals.byView[fallback.view || 'unknown'] = (totals.byView[fallback.view || 'unknown'] || 0) + 1;
    totals.byModel[fallback.modelId] = (totals.byModel[fallback.modelId] || 0) + 1;
    totals.byFamily[fallback.familyId] = (totals.byFamily[fallback.familyId] || 0) + 1;
    if (fallback.targetKind === 'static_view') totals.staticViewFallbackFrames += 1;
    if (bossIds.has(normalizeName(fallback.modelId)) || bossIds.has(normalizeName(fallback.familyId))) {
      totals.bossFallbackFrames += 1;
    }
  }
  return totals;
}

function atlasQualityStatus(report, thresholds) {
  if (report.totalFallbacks === 0) return 'clean';
  if (
    report.bossFallbackFrames > 0 ||
    (thresholds.staticViewsMustBeFallbackFree && report.staticViewFallbackFrames > 0) ||
    report.fallbackRate > thresholds.maxFallbackRate
  ) {
    return 'fallback_degraded';
  }
  return 'clean';
}

function writeDiagnostics(output, manifest, memorySamples) {
  const fallbackReport = {
    schemaVersion: 1,
    generatedAt: manifest.completedAt || new Date().toISOString(),
    sourceManifest: path.join(output, 'manifest.json'),
    thresholds: manifest.qualityThresholds,
    totals: manifest.fallbackSummary,
    fallbacks: manifest.fallbacks,
  };
  fs.writeFileSync(path.join(output, 'fallback_report.json'), `${JSON.stringify(fallbackReport, null, 2)}\n`);
  const profilePath = path.join(output, 'render_memory_profile.jsonl');
  const lines = memorySamples.map((sample) => JSON.stringify(sample)).join('\n');
  fs.writeFileSync(profilePath, `${lines}${lines ? '\n' : ''}`);
}

function classifyFallback(error) {
  const message = String(error || '').toLowerCase();
  if (message.includes('out of memory')) return 'memory_pressure';
  if (message.includes('uv')) return 'uv_mapping';
  if (message.includes('texture')) return 'texture_decode';
  return 'render_error';
}
function renderModelInChild({ blueprint, output, size, sourceHash, resume, renderContractVersion }) {
  const args = [
    '--expose-gc',
    SINGLE_RENDER_SCRIPT,
    '--blueprint', blueprint,
    '--output', output,
    '--size', String(size),
    '--source-hash', sourceHash,
    '--render-contract-version', String(renderContractVersion),
  ];
  if (resume) args.push('--resume');
  const result = spawnSync(process.execPath, args, {
    cwd: __dirname,
    encoding: 'utf8',
    maxBuffer: 1024 * 1024 * 32,
  });
  if (result.error) {
    throw new Error(`child_render_spawn_failed: ${result.error.message}`);
  }
  if (result.status !== 0) {
    throw new Error((result.stderr || result.stdout || 'single-model render failed').trim());
  }
  return JSON.parse(result.stdout.trim());
}

function main() {
  const corpus = path.resolve(arg('corpus', DEFAULT_CORPUS));
  const blueprintDir = path.join(corpus, 'ModelEngine', 'blueprints');
  const output = path.resolve(arg('output', DEFAULT_OUTPUT));
  const size = Number(arg('size', 320));
  const resume = process.argv.includes('--resume');
  const previousManifestPath = path.join(output, 'manifest.json');
  const previousManifest = resume && fs.existsSync(previousManifestPath)
    ? JSON.parse(fs.readFileSync(previousManifestPath, 'utf8'))
    : null;
  const previousModels = new Map((previousManifest?.models || []).map((model) => [model.blueprint, model]));
  const bossIds = loadBossIds();
  const renderContract = {
    version: 7,
    size,
    stableScale: true,
    stableGround: true,
    stableFraming: 'per_model_world_bounds',
    frontFacingYaw: 180,
    backFacingYaw: 0,
    rendererIsolation: 'child_process_per_model',
    phaseSelection: 'timeline_or_motion_peak_with_recovery',
    animationPhases: PHASES,
  };
  const blueprints = walkBlueprints(corpus)
    .sort();
  const manifest = {
    schemaVersion: 2,
    corpus,
    output,
    size,
    renderContract,
    startedAt: new Date().toISOString(),
    completedAt: null,
    modelsTotal: blueprints.length,
    modelsCompleted: 0,
    animationsTotal: 0,
    framesExpected: 0,
    framesWritten: 0,
    qualityThresholds: {
      maxFallbackRate: 0.01,
      staticViewsMustBeFallbackFree: true,
      bossesMustBeFallbackFree: true,
    },
    qualityStatus: 'incomplete',
    fallbackFrames: 0,
    fallbackSummary: null,
    fallbackReportPath: path.join(output, 'fallback_report.json'),
    memoryProfilePath: path.join(output, 'render_memory_profile.jsonl'),
    fallbacks: [],
    failures: [],
    models: [],
  };
  const memorySamples = [];
  writeManifest(output, manifest);

  for (const [index, blueprint] of blueprints.entries()) {
    const relative = blueprint.startsWith(`${blueprintDir}${path.sep}`)
      ? path.relative(blueprintDir, blueprint)
      : path.relative(corpus, blueprint);
    const id = path.basename(blueprint, '.bbmodel');
    const modelOutput = path.join(output, safeName(relative.replace(/\.bbmodel$/, '')));
    fs.mkdirSync(modelOutput, { recursive: true });
    try {
      const sourceHash = sha1(blueprint);
      const previous = previousModels.get(blueprint);
      const modelResume = Boolean(
        resume &&
        previousManifest?.renderContract?.version === renderContract.version &&
        previousManifest?.renderContract?.size === renderContract.size &&
        previousManifest?.renderContract?.frontFacingYaw === renderContract.frontFacingYaw &&
        previousManifest?.renderContract?.stableFraming === renderContract.stableFraming &&
        previous?.sourceHash === sourceHash,
      );
      const child = renderModelInChild({
        blueprint,
        output: modelOutput,
        size,
        sourceHash,
        resume: modelResume,
        renderContractVersion: renderContract.version,
      });
      const modelRecord = {
        id,
        blueprint,
        sourceHash,
        familyId: familyRoot(id),
        animations: child.animations,
        frames: child.framesExpected,
        framesWritten: child.framesWritten,
      };
      manifest.framesWritten += child.framesWritten;
      manifest.fallbackFrames += child.fallbackFrames;
      for (const fallback of child.fallbacks || []) {
        manifest.fallbacks.push({
          file: path.relative(output, fallback.file),
          error: fallback.error,
          reason: fallback.reason || classifyFallback(fallback.error),
          targetKind: fallback.targetKind,
          modelId: fallback.modelId || id,
          familyId: familyRoot(fallback.modelId || id),
          view: fallback.view || null,
          animation: fallback.animation || null,
          phase: fallback.phase || null,
          sourceHash,
        });
      }
      for (const diagnostic of child.diagnostics || []) {
        memorySamples.push({
          timestamp: new Date().toISOString(),
          sourceHash,
          familyId: familyRoot(id),
          ...diagnostic,
        });
      }
      manifest.animationsTotal += child.animations;
      manifest.framesExpected += child.framesExpected;
      manifest.models.push(modelRecord);
      manifest.modelsCompleted += 1;
    } catch (error) {
      manifest.failures.push({ id, blueprint, error: error.message });
    }
    if ((index + 1) % 10 === 0 || index + 1 === blueprints.length) {
      writeManifest(output, manifest);
      console.log(`MODEL_CORPUS_RENDER_PROGRESS: ${index + 1}/${blueprints.length}`);
    }
  }
  manifest.completedAt = new Date().toISOString();
  manifest.fallbackSummary = buildFallbackReport(manifest.fallbacks, bossIds, manifest.framesExpected);
  manifest.qualityStatus = manifest.failures.length
    ? 'incomplete'
    : atlasQualityStatus(manifest.fallbackSummary, manifest.qualityThresholds);
  writeManifest(output, manifest);
  writeDiagnostics(output, manifest, memorySamples);
  if (manifest.failures.length) {
    console.error(`MODEL_CORPUS_RENDER_FAIL: ${manifest.failures.length} models`);
    process.exitCode = 1;
    return;
  }
  console.log(`MODEL_CORPUS_RENDER_PASS: ${manifest.modelsCompleted} models, ${manifest.animationsTotal} animations, ${manifest.framesExpected} frames`);
}

main();
