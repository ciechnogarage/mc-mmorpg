const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, 'reference_corpus');
const pipeline = JSON.parse(fs.readFileSync(path.join(root, 'boss_review_pipeline.json'), 'utf8'));
const cardsDocument = JSON.parse(fs.readFileSync(path.join(root, 'modelengine-model-cards.json'), 'utf8'));
const allModelKeys = new Set(cardsDocument.cards.map((card) => `${card.id}::${card.file}`));

assert.strictEqual(pipeline.schemaVersion, 1, 'Boss review pipeline schema mismatch');
assert(pipeline.bossFamilies.length > 0, 'No evidence-confirmed boss families were detected');
assert(Array.isArray(pipeline.activeRuntimeBosses), 'Active runtime boss queue is missing');
assert(pipeline.activeRuntimeBosses.length > 0, 'No active runtime bosses were detected');
assert(pipeline.queues, 'Boss review queues are missing');
assert(Array.isArray(pipeline.queues.activeRuntimeStrictQueue), 'Active runtime strict queue is missing');
assert(Array.isArray(pipeline.queues.confirmedFamilyStrictQueue), 'Confirmed family strict queue is missing');
assert(Array.isArray(pipeline.queues.recommendedConfirmedFamilyBatch), 'Recommended confirmed family batch is missing');
assert(Array.isArray(pipeline.queues.candidateFamilyConfirmationQueue), 'Candidate family confirmation queue is missing');

const bossKeys = new Set();
for (const family of pipeline.bossFamilies) {
  assert.strictEqual(family.classification, 'confirmed_boss', `${family.familyId}: invalid classification`);
  assert(
    family.classificationEvidence.forcedInclude ||
      family.classificationEvidence.signals.some((signal) => signal.explicitBossbar.length > 0),
    `${family.familyId}: confirmed without explicit evidence`,
  );
  assert(family.models.length > 0, `${family.familyId}: empty family`);
  for (const model of family.models) {
    assert(allModelKeys.has(model.modelKey), `${model.modelKey}: absent from corpus`);
    assert(!bossKeys.has(model.modelKey), `${model.modelKey}: duplicated across boss families`);
    bossKeys.add(model.modelKey);
    assert(/^[a-f0-9]{40}$/.test(model.source.sourceHash || ''), `${model.modelKey}: invalid source hash`);
    assert.strictEqual(model.renders.staticViews.length, 8, `${model.modelKey}: incomplete static renders`);
    assert.strictEqual(model.renders.animations.length, model.animations, `${model.modelKey}: animation mismatch`);
    for (const animation of model.renders.animations) {
      assert.strictEqual(animation.frames.length, 5, `${model.modelKey}:${animation.name}: incomplete phases`);
      for (const frame of animation.frames) {
        assert(fs.existsSync(path.join(root, frame)), `${model.modelKey}:${animation.name}: missing ${frame}`);
      }
    }
  }
}

const sample = pipeline.qa.deterministicNonBossSample;
const expectedSampleSize = Math.ceil((allModelKeys.size - bossKeys.size) * pipeline.policy.qaSampleRate);
assert.strictEqual(sample.length, expectedSampleSize, 'QA sample is not exactly 10% rounded up');
assert.strictEqual(new Set(sample.map((item) => item.modelKey)).size, sample.length, 'QA sample contains duplicates');
for (const item of sample) {
  assert(allModelKeys.has(item.modelKey), `${item.modelKey}: QA sample model absent from corpus`);
  assert(!bossKeys.has(item.modelKey), `${item.modelKey}: boss leaked into non-boss QA sample`);
}

const grove = pipeline.activeRuntimeBosses.find((boss) => boss.mobId === 'level_1_grove_guardian');
assert(grove, 'Active queue must include level_1_grove_guardian');
assert.strictEqual(grove.queueStatus, 'strict_complete', 'Grove Guardian strict review should be complete');
assert.strictEqual(grove.blockers.length, 0, 'Grove Guardian should not have queue blockers');
assert(/\.bbmodel$/.test(grove.blueprint), 'Grove Guardian blueprint is missing');
assert(/runtime-probe\.md$/.test(grove.runtimeProbe || ''), 'Grove Guardian runtime probe is missing');
assert(grove.renderEvidence.length >= 8, 'Grove Guardian render evidence is incomplete');
assert(grove.skillBindings.length >= 5, 'Grove Guardian skill bindings are unexpectedly sparse');
assert(grove.graph.nodes.length > 0, 'Grove Guardian graph is empty');
assert(grove.graph.edges.some((edge) => edge.type === 'runtime_probe'), 'Grove Guardian graph lacks runtime proof edge');
assert(
  !pipeline.queues.activeRuntimeStrictQueue.some((boss) => boss.mobId === 'level_1_grove_guardian'),
  'Completed Grove Guardian review must leave the runtime strict queue',
);
assert.strictEqual(pipeline.summary.activeRuntimeBossesComplete, 1, 'Completed runtime review count mismatch');
assert(
  pipeline.queues.recommendedConfirmedFamilyBatch.length > 0,
  'Recommended confirmed family batch should not be empty',
);
assert(
  pipeline.queues.recommendedConfirmedFamilyBatch.every((family) => family.animations > 0),
  'Recommended confirmed family batch should exclude zero-animation families',
);
assert(
  pipeline.queues.candidateFamilyConfirmationQueue.length === pipeline.candidateFamilies.length,
  'Candidate confirmation queue should cover every candidate family',
);

console.log(
  `BOSS_REVIEW_PIPELINE_TEST_PASS: ${pipeline.bossFamilies.length} families, ` +
  `${bossKeys.size} corpus boss models, ${pipeline.activeRuntimeBosses.length} active runtime bosses, ` +
  `${sample.length} deterministic QA models`,
);
