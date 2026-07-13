const assert = require('assert');
const fs = require('fs');
const path = require('path');
const { validateStrictReview } = require('./manual_review_contract');

const ROOT = __dirname;
const storePath = path.join(ROOT, 'active_runtime_reviews', 'completed_runtime_boss_reviews.json');
assert(fs.existsSync(storePath), 'Completed runtime review store is missing');

const store = JSON.parse(fs.readFileSync(storePath, 'utf8'));
const standard = JSON.parse(
  fs.readFileSync(path.join(ROOT, 'reference_corpus', 'manual_visual_reviews.json'), 'utf8'),
).reviewStandard;

assert.strictEqual(store.reviewStatus, 'strict_runtime', 'Runtime review store must be strict');
assert(store.models.length > 0, 'Runtime review store is empty');

for (const review of store.models) {
  const blueprintPath = path.resolve(ROOT, review.modelKey.split('::')[1]);
  const blueprint = JSON.parse(fs.readFileSync(blueprintPath, 'utf8'));
  const card = {
    id: review.id,
    file: review.modelKey.split('::')[1],
    absoluteFile: blueprintPath,
    animations: blueprint.animations.map((animation) => animation.name),
  };
  const errors = validateStrictReview(review, card, standard, ROOT);
  assert.deepStrictEqual(errors, [], errors.join('\n'));
  assert(
    review.claims.some((claim) => claim.type === 'runtime_observation'),
    `${review.id}: strict runtime review lacks runtime claim`,
  );
  assert(
    review.unknowns.some((unknown) => unknown.includes('Live execution')),
    `${review.id}: runtime evidence boundary is not explicit`,
  );
  assert.strictEqual(
    review.animationFindings.length,
    blueprint.animations.length,
    `${review.id}: every animation needs exactly one finding`,
  );
}

console.log(`COMPLETED_RUNTIME_BOSS_REVIEWS_TEST_PASS: ${store.models.length} models`);
