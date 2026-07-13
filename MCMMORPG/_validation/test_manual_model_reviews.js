const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { auditReviewDatabase, validateStrictReview } = require('./manual_review_contract');

const root = path.join(__dirname, 'reference_corpus');
const index = JSON.parse(fs.readFileSync(path.join(root, 'manual_visual_reviews.json'), 'utf8'));
const cardsDocument = JSON.parse(fs.readFileSync(path.join(root, 'modelengine-model-cards.json'), 'utf8'));
const cards = cardsDocument.cards.map((card) => ({
  ...card,
  absoluteFile: path.resolve(cardsDocument.corpus, card.file),
}));
const batches = (index.batchFiles || []).map((batchFile) =>
  JSON.parse(fs.readFileSync(path.join(root, batchFile), 'utf8')));

assert(index.schemaVersion >= 3, 'Manual review contract must use strict schema v3');
assert.deepStrictEqual(
  index.reviewStandard.animationPhases,
  ['start', 'anticipation', 'impact', 'recovery', 'end'],
  'Manual review contract must use recovery-aware phases',
);
assert.deepStrictEqual(
  index.reviewStandard.gameplaySyncChannels,
  ['damage', 'hitbox', 'sound', 'vfx'],
  'Manual review contract must require all gameplay synchronization channels',
);
assert(
  index.reviewStandard.evidenceTypes.includes('runtime_observation'),
  'Manual review contract must distinguish runtime observations',
);
assert(
  index.reviewStandard.requiredEvidenceFields.includes('sourceClass'),
  'Manual review contract must require evidence sourceClass',
);

const report = auditReviewDatabase(index, batches, cards, root);
assert.deepStrictEqual(report.errors, [], report.errors.join('\n'));
assert.strictEqual(
  report.counts.strictModels,
  index.progress.modelsReviewed,
  'Strict model progress does not match valid batch reviews',
);
assert.strictEqual(
  report.counts.strictAnimations,
  index.progress.animationsReviewed,
  'Strict animation progress does not match individual findings',
);
assert.strictEqual(
  report.counts.legacyModels,
  index.progress.legacyModelsReviewed,
  'Legacy model progress does not match batch files',
);
assert.strictEqual(
  report.counts.legacyAnimations,
  index.progress.legacyAnimationsReviewed,
  'Legacy animation progress does not match batch files',
);

// Regression: confidence and broad artistic labels are never evidence.
const fixtureCard = {
  id: 'fixture',
  file: __filename,
  absoluteFile: __filename,
  animations: ['attack'],
};
const invalidReview = {
  id: 'fixture',
  modelKey: `fixture::${__filename}`,
  sourceHash: '0'.repeat(40),
  reviewedSources: [__filename],
  stale: false,
  identity: 'fixture',
  silhouette: 'present',
  proportions: 'present',
  composition: 'present',
  materials: 'present',
  palette: 'present',
  atmosphere: 'present',
  rig: 'present',
  gameplay: 'present',
  transferableTechniques: [],
  weaknesses: [],
  claims: [{
    type: 'visual_observation',
    statement: 'Silhouette and confidence are enough.',
    evidence: [],
  }],
  animationFindings: [],
  confidence: 'high',
  unknowns: [],
};
const invalidErrors = validateStrictReview(
  invalidReview,
  fixtureCard,
  index.reviewStandard,
  root,
);
assert(
  invalidErrors.some((error) => error.includes('claim without evidence')),
  'Empty evidence must fail even when confidence and silhouette are present',
);
assert(
  invalidErrors.some((error) => error.includes('missing individual analysis')),
  'Listing no per-animation finding must fail',
);
assert(
  invalidErrors.some((error) => error.includes('stale sourceHash')),
  'A mismatched source hash must fail',
);

const invalidRuntimeReview = {
  ...invalidReview,
  claims: [{
    type: 'runtime_observation',
    statement: 'The attack definitely lands in runtime.',
    evidence: [{
      artifact: 'yaml',
      sourceClass: 'active_runtime_repo',
      source: __filename,
      locator: 'line 1',
      detail: 'Not actually runtime proof.',
      sourceHash: '0'.repeat(40),
    }],
  }],
  animationFindings: [{
    name: 'attack',
    phaseTimes: { start: 0, anticipation: 0.1, impact: 0.2, recovery: 0.3, end: 0.4 },
    phaseMethod: 'manual',
    phaseRationale: 'fixture',
    poseRead: 'fixture',
    massTransfer: 'fixture',
    contactRead: 'fixture',
    recoveryRead: 'fixture',
    runtimeBinding: 'fixture',
    gameplaySync: {
      damage: { status: 'unknown', reason: 'fixture' },
      hitbox: { status: 'unknown', reason: 'fixture' },
      sound: { status: 'unknown', reason: 'fixture' },
      vfx: { status: 'unknown', reason: 'fixture' },
    },
    evidence: [{
      artifact: 'yaml',
      sourceClass: 'active_runtime_repo',
      source: __filename,
      locator: 'line 1',
      detail: 'fixture',
      sourceHash: '0'.repeat(40),
    }],
    confidence: 'low',
    unknowns: [],
  }],
};
const invalidRuntimeErrors = validateStrictReview(
  invalidRuntimeReview,
  fixtureCard,
  index.reviewStandard,
  root,
);
assert(
  invalidRuntimeErrors.some((error) => error.includes('runtime_observation requires runtime evidence')),
  'Runtime claims must require runtime evidence',
);

console.log(
  `MANUAL_MODEL_REVIEWS_PASS: strict=${report.counts.strictModels}/${index.progress.modelsTotal} models, ` +
  `${report.counts.strictAnimations}/${index.progress.animationsTotal} animations; ` +
  `legacy=${report.counts.legacyModels} models, ${report.counts.legacyAnimations} animations`,
);
