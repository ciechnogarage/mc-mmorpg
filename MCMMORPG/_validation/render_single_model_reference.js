const fs = require('fs');
const path = require('path');
const { render, releaseModelTextures } = require('./render_bbmodel_review');
const { PHASES, animationPhaseTimesFromAnimation } = require('./modelengine_phase_utils');

function arg(name, fallback = null) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 ? process.argv[index + 1] : fallback;
}

function safeName(value) {
  return String(value).replace(/[^A-Za-z0-9_.-]+/g, '_');
}

function classifyFallback(error) {
  const message = String(error || '').toLowerCase();
  if (message.includes('out of memory')) return 'memory_pressure';
  if (message.includes('uv')) return 'uv_mapping';
  if (message.includes('texture')) return 'texture_decode';
  return 'render_error';
}

function memorySnapshot() {
  const usage = process.memoryUsage();
  return {
    rss: usage.rss,
    heapUsed: usage.heapUsed,
    external: usage.external,
  };
}

function recordTelemetry(samples, meta, stage) {
  samples.push({
    ...meta,
    stage,
    memory: memorySnapshot(),
  });
}

async function renderIfMissing(model, file, options, resume, diagnostics) {
  if (resume && fs.existsSync(file)) return { written: false, fallback: false };
  const telemetryEnabled = options.view === 'side' || options.captureTelemetry === true;
  if (telemetryEnabled) recordTelemetry(diagnostics, options.telemetryMeta, 'before_mapped');
  try {
    await render(model, file, { ...options, textureMode: 'mapped' });
    if (telemetryEnabled) recordTelemetry(diagnostics, options.telemetryMeta, 'after_mapped');
    return { written: true, fallback: false };
  } catch (error) {
    recordTelemetry(diagnostics, options.telemetryMeta, 'mapped_failed');
    if (global.gc) global.gc();
    await render(model, file, { ...options, textureMode: 'average' });
    recordTelemetry(diagnostics, options.telemetryMeta, 'after_fallback_average');
    return {
      written: true,
      fallback: true,
      mappedError: error.message,
      reason: classifyFallback(error.message),
      meta: options.telemetryMeta,
    };
  } finally {
    if (global.gc) global.gc();
  }
}

async function main() {
  const blueprint = path.resolve(arg('blueprint'));
  const output = path.resolve(arg('output'));
  const size = Number(arg('size', 320));
  const sourceHash = arg('source-hash');
  const renderContractVersion = Number(arg('render-contract-version', 7));
  const resume = process.argv.includes('--resume');
  if (!blueprint || !fs.existsSync(blueprint)) throw new Error(`Blueprint not found: ${blueprint}`);

  const model = JSON.parse(fs.readFileSync(blueprint, 'utf8'));
  fs.mkdirSync(output, { recursive: true });

  let framesExpected = 0;
  let framesWritten = 0;
  let fallbackFrames = 0;
  const fallbacks = [];
  const diagnostics = [];
  const modelId = path.basename(blueprint, '.bbmodel');

  try {
    const views = [
      ['front', 180, -8],
      ['side', 90, -8],
      ['back', 0, -8],
      ['three_quarter', 35, -10],
    ];
    for (const [name, yaw, pitch] of views) {
      const target = path.join(output, `${name}.png`);
      const result = await renderIfMissing(model, target, {
        title: name,
        view: name,
        yaw,
        pitch,
        size,
        stableScale: true,
        stableGround: true,
        telemetryMeta: {
          modelId,
          blueprint,
          output: target,
          targetKind: 'static_view',
          view: name,
          animation: null,
          phase: null,
        },
      }, resume, diagnostics);
      framesExpected += 1;
      if (result.written) framesWritten += 1;
      if (result.fallback) {
        fallbackFrames += 1;
        fallbacks.push({
          file: target,
          error: result.mappedError,
          reason: result.reason,
          targetKind: 'static_view',
          modelId,
          view: name,
          animation: null,
          phase: null,
        });
      }
    }
    for (const [name, extra] of [
      ['silhouette', { silhouette: true }],
      ['player_scale', { playerScale: true }],
      ['hitbox', { hitbox: true }],
      ['helpers', { helpers: true }],
    ]) {
      const target = path.join(output, `${name}.png`);
      const result = await renderIfMissing(model, target, {
        title: name,
        view: name,
        yaw: 35,
        pitch: -10,
        size,
        stableScale: true,
        stableGround: true,
        captureTelemetry: false,
        telemetryMeta: {
          modelId,
          blueprint,
          output: target,
          targetKind: 'diagnostic_view',
          view: name,
          animation: null,
          phase: null,
        },
        ...extra,
      }, resume, diagnostics);
      framesExpected += 1;
      if (result.written) framesWritten += 1;
      if (result.fallback) {
        fallbackFrames += 1;
        fallbacks.push({
          file: target,
          error: result.mappedError,
          reason: result.reason,
          targetKind: 'diagnostic_view',
          modelId,
          view: name,
          animation: null,
          phase: null,
        });
      }
    }

    for (const animation of model.animations || []) {
      const animationName = safeName(animation.name);
      const phaseTimes = animationPhaseTimesFromAnimation(animation);
      for (const phase of PHASES) {
        const target = path.join(output, `animation_${animationName}_${phase}.png`);
        const result = await renderIfMissing(model, target, {
          title: `${animation.name} ${phase}`,
          view: 'three_quarter',
          yaw: 35,
          pitch: -10,
          size,
          animation: animation.name,
          time: phaseTimes[phase],
          stableScale: true,
          stableGround: true,
          captureTelemetry: false,
          telemetryMeta: {
            modelId,
            blueprint,
            output: target,
            targetKind: 'animation_phase',
            view: 'three_quarter',
            animation: animation.name,
            phase,
          },
        }, resume, diagnostics);
        framesExpected += 1;
        if (result.written) framesWritten += 1;
        if (result.fallback) {
          fallbackFrames += 1;
          fallbacks.push({
            file: target,
            error: result.mappedError,
            reason: result.reason,
            targetKind: 'animation_phase',
            modelId,
            view: 'three_quarter',
            animation: animation.name,
            phase,
          });
        }
      }
    }
  } finally {
    releaseModelTextures(model);
    if (global.gc) global.gc();
  }

  process.stdout.write(`${JSON.stringify({
    schemaVersion: 1,
    renderContractVersion,
    sourceHash,
    animations: (model.animations || []).length,
    framesExpected,
    framesWritten,
    fallbackFrames,
    fallbacks,
    diagnostics,
  })}\n`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
