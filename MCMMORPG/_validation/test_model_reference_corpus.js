const assert = require('assert');
const fs = require('fs');
const path = require('path');

const reportPath = path.join(__dirname, 'reference_corpus', 'modelengine-reference-corpus.json');
assert(fs.existsSync(reportPath), 'Generate the corpus report before running this test');

const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
assert(report.schemaVersion >= 2, 'Corpus schema must include the learning layer');
assert(report.summary.blueprints.total > 0, 'No blueprints were analyzed');
assert.strictEqual(
  report.knowledge.profiles.length,
  report.summary.blueprints.total,
  'Every blueprint needs a learning profile',
);
assert(report.knowledge.topReferences.length >= 25, 'Reference ranking is too small');
assert(report.knowledge.evidenceRules.length >= 8, 'Evidence-backed rules are missing');
assert(report.knowledge.vocabularies.animations.length >= 20, 'Animation vocabulary is too small');
assert(report.knowledge.vocabularies.bones.length >= 20, 'Bone vocabulary is too small');
assert(report.knowledge.vocabularies.mechanics.length >= 20, 'Mechanic vocabulary is too small');
assert(report.knowledge.coverage.archetypes, 'Archetype coverage is missing');
assert(Number.isInteger(report.knowledge.coverage.referencedBlueprints), 'Referenced blueprint coverage is missing');
assert(Array.isArray(report.integrity.duplicateBlueprintIds), 'Duplicate blueprint IDs are not exposed');

let animationCards = 0;
for (const profile of report.knowledge.profiles) {
  assert(profile.id, 'Profile without model id');
  assert(['basic', 'developed', 'production', 'integrated'].includes(profile.tier), `Invalid tier for ${profile.id}`);
  assert(Number.isFinite(profile.score), `Invalid score for ${profile.id}`);
  assert(profile.archetype, `Missing archetype for ${profile.id}`);
  assert(Array.isArray(profile.artisticObservations) && profile.artisticObservations.length > 0, `Missing artistic synthesis for ${profile.id}`);
  assert(Array.isArray(profile.animationsDetail), `Missing animation cards for ${profile.id}`);
  animationCards += profile.animationsDetail.length;
  for (const animation of profile.animationsDetail) {
    assert(animation.role, `Missing animation role for ${profile.id}:${animation.name}`);
    assert(animation.sampleTimes.length === 5, `Missing animation phase samples for ${profile.id}:${animation.name}`);
    assert(animation.phaseCoverage, `Missing animation phase coverage for ${profile.id}:${animation.name}`);
    assert(animation.phaseTimes?.method, `Missing phase derivation method for ${profile.id}:${animation.name}`);
    assert(animation.phaseTimes.impact >= animation.phaseTimes.anticipation, `Invalid impact ordering for ${profile.id}:${animation.name}`);
  }
  for (const binding of profile.bindingEvidence) {
    assert(['mob_binding', 'supporting_reference'].includes(binding.bindingType), `Invalid binding type for ${profile.id}`);
    assert(Number.isInteger(binding.line) && binding.line > 0, `Missing binding line for ${profile.id}`);
    assert(binding.path, `Missing binding path for ${profile.id}`);
  }
  for (const animation of profile.animationsDetail) {
    for (const runtime of animation.runtimeEvidence) {
      assert(runtime.path, `Missing runtime evidence path for ${profile.id}:${animation.name}`);
    }
  }
}

assert(animationCards > 2500, `Animation coverage is unexpectedly low: ${animationCards}`);
console.log(`MODEL_REFERENCE_CORPUS_TEST_PASS: ${report.knowledge.profiles.length} model profiles, ${animationCards} animation cards`);
