const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { validateLearningEntry } = require('./model_design_learning_contract');

const corpusDir = path.join(__dirname, 'reference_corpus');
const router = JSON.parse(fs.readFileSync(path.join(corpusDir, 'modelengine-design-router.json'), 'utf8'));
const risks = JSON.parse(fs.readFileSync(path.join(corpusDir, 'modelengine-negative-patterns.json'), 'utf8'));
const cards = JSON.parse(fs.readFileSync(path.join(corpusDir, 'modelengine-model-cards.json'), 'utf8')).cards;
const learning = JSON.parse(fs.readFileSync(path.join(__dirname, 'active_runtime_reviews', 'model_design_learning.json'), 'utf8'));

assert.strictEqual(router.coverage.cards, cards.length, 'Router must cover every model card');
assert(router.coverage.cards >= 542, 'Corpus coverage regressed');
assert(router.coverage.subsystems >= 11, 'Subsystem routing is incomplete');
assert(Object.keys(router.archetypeRoutes).length === router.coverage.archetypes, 'Archetype coverage mismatch');

for (const [subsystem, route] of Object.entries(router.subsystems)) {
  assert(route.intent, `${subsystem} is missing intent`);
  assert(route.candidates.length >= 5, `${subsystem} has too few candidates`);
  assert(new Set(route.candidates.map((candidate) => candidate.id)).size === route.candidates.length, `${subsystem} contains duplicate candidates`);
  for (const candidate of route.candidates) {
    assert(candidate.evidence.file, `${subsystem}:${candidate.id} is missing source provenance`);
    assert(Array.isArray(candidate.limitations), `${subsystem}:${candidate.id} is missing limitations`);
  }
}

for (const [archetype, routes] of Object.entries(router.archetypeRoutes)) {
  for (const [subsystem, candidates] of Object.entries(routes)) {
    for (const candidate of candidates) {
      assert.strictEqual(candidate.evidence.archetype, archetype, `${archetype}:${subsystem} route leaked another archetype`);
    }
  }
}

assert(risks.patterns.length >= 5, 'Negative corpus is too small');
for (const pattern of risks.patterns) {
  assert(pattern.description && pattern.avoidWhen && pattern.acceptance, `${pattern.id} is not decision-ready`);
  assert(pattern.prevalence.total === cards.length, `${pattern.id} coverage mismatch`);
  for (const example of pattern.examples) {
    assert(example.file && example.evidence, `${pattern.id}:${example.id} lacks evidence`);
  }
}

for (const entry of learning.entries) {
  assert.deepStrictEqual(validateLearningEntry(entry), [], `Invalid learning entry ${entry.id}`);
  for (const decision of entry.decisions) {
    for (const reference of decision.references) {
      assert(cards.some((card) => card.id === reference), `${entry.id} references unknown model ${reference}`);
    }
    for (const evidence of decision.acceptanceEvidence) {
      if (evidence.path) {
        assert(fs.existsSync(path.join(__dirname, evidence.path)), `${entry.id} evidence does not exist: ${evidence.path}`);
      }
    }
  }
}

const universalTop = Object.values(router.subsystems).map((route) => route.candidates[0].id);
assert(new Set(universalTop).size >= 4, 'Router collapsed into a universal-template ranking');

const invalidCopy = {
  id: 'copy-test',
  modelId: 'example',
  createdAt: new Date().toISOString(),
  outcome: 'confirmed',
  decisions: [{
    subsystem: 'silhouette',
    references: ['one-model'],
    intent: 'test',
    principle: 'copy',
    limitation: 'none',
    deliberateDifference: 'none',
    hypothesis: 'same result',
    acceptanceEvidence: [],
  }],
  reusableLesson: 'copy it',
};
assert(validateLearningEntry(invalidCopy).some((error) => error.includes('at least two')), 'Contract accepted one universal template');

console.log(`MODEL_DESIGN_KNOWLEDGE_TEST_PASS: ${cards.length} cards, ${router.coverage.subsystems} subsystems, ${risks.patterns.length} risks`);
