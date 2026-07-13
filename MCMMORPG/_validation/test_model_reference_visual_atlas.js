const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, 'reference_corpus', 'visual_atlas_v2');
const manifestPath = path.join(root, 'manifest.json');
assert(fs.existsSync(manifestPath), 'Visual atlas manifest is missing');

const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
assert(manifest.completedAt, 'Visual atlas run is incomplete');
assert(manifest.schemaVersion >= 2, 'Visual atlas manifest lacks source invalidation');
assert(manifest.renderContract?.version >= 7, 'Visual atlas was generated with the obsolete camera/phase contract');
assert.strictEqual(manifest.renderContract?.stableGround, true, 'Visual atlas must keep a fixed ground baseline');
assert.strictEqual(manifest.renderContract?.frontFacingYaw, 180, 'Visual atlas front view must use the corrected facing');
assert.strictEqual(manifest.renderContract?.backFacingYaw, 0, 'Visual atlas back view must use the corrected facing');
assert.strictEqual(manifest.renderContract?.rendererIsolation, 'child_process_per_model', 'Visual atlas must isolate per-model renders');
assert.strictEqual(typeof manifest.qualityStatus, 'string', 'Visual atlas must report quality status');
assert(manifest.fallbackReportPath, 'Visual atlas must publish fallback report path');
assert(manifest.memoryProfilePath, 'Visual atlas must publish memory profile path');
assert.deepStrictEqual(
  manifest.renderContract?.animationPhases,
  ['start', 'anticipation', 'impact', 'recovery', 'end'],
  'Visual atlas phase contract must include recovery',
);
assert.strictEqual(manifest.modelsTotal, 542, 'Every blueprint in the corpus must be rendered');
assert.strictEqual(manifest.modelsCompleted, 542, 'Some model cards were not rendered');
assert.strictEqual(manifest.animationsTotal, 2777, 'Every animation must be represented');
assert.strictEqual(
  manifest.framesExpected,
  manifest.modelsTotal * 8 + manifest.animationsTotal * 5,
  'Expected eight model views and five phases per animation',
);
assert.strictEqual(manifest.failures.length, 0, 'Visual atlas contains model failures');
assert.strictEqual(manifest.fallbackFrames, manifest.fallbacks.length, 'Every mapped-texture fallback must be auditable');
assert(manifest.fallbackSummary, 'Visual atlas must expose fallback summary');
assert.strictEqual(manifest.fallbackSummary.totalFallbacks, manifest.fallbackFrames, 'Fallback summary must match fallback frame count');
for (const fallback of manifest.fallbacks) {
  assert(['memory_pressure', 'uv_mapping', 'texture_decode', 'render_error'].includes(fallback.reason), `Unknown fallback reason for ${fallback.file}`);
  assert(fallback.modelId, `Missing modelId for ${fallback.file}`);
  assert(fallback.familyId, `Missing familyId for ${fallback.file}`);
  assert(fallback.targetKind, `Missing targetKind for ${fallback.file}`);
}

for (const record of manifest.models) {
  assert(/^[a-f0-9]{40}$/.test(record.sourceHash), `Missing source hash for ${record.id}`);
  assert(record.frames === 8 + record.animations * 5, `Incomplete frame contract for ${record.id}`);
  assert(record.familyId, `Missing familyId for ${record.id}`);
}

console.log(
  `MODEL_REFERENCE_VISUAL_ATLAS_TEST_PASS: ${manifest.modelsCompleted} models, ` +
  `${manifest.animationsTotal} animations, ${manifest.framesExpected} frames`,
);
