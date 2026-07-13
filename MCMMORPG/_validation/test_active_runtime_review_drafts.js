const assert = require('assert');
const fs = require('fs');
const path = require('path');

const validationRoot = __dirname;
const templates = JSON.parse(fs.readFileSync(path.join(validationRoot, 'active_runtime_reviews', 'runtime_boss_review_templates.json'), 'utf8'));
const document = JSON.parse(fs.readFileSync(path.join(validationRoot, 'active_runtime_reviews', 'runtime_boss_review_drafts.json'), 'utf8'));

assert.strictEqual(document.schemaVersion, 1, 'Draft schema mismatch');
assert.strictEqual(
  document.drafts.length,
  templates.templates.filter((template) => template.queueStatus === 'strict_ready').length,
  'Draft count must match strict-ready runtime template count',
);

for (const draft of document.drafts) {
  assert.strictEqual(draft.status, 'pending_manual_authoring', `${draft.mobId}: invalid draft status`);
  assert(Array.isArray(draft.reviewedSources) && draft.reviewedSources.length > 0, `${draft.mobId}: missing reviewedSources`);
  assert(Array.isArray(draft.authoringOrder) && draft.authoringOrder.length >= 4, `${draft.mobId}: missing authoring order`);
  assert(draft.strictReviewScaffold, `${draft.mobId}: missing strict review scaffold`);
  assert(Array.isArray(draft.strictReviewScaffold.claims) && draft.strictReviewScaffold.claims.length > 0, `${draft.mobId}: missing claim scaffolds`);
  assert(Array.isArray(draft.strictReviewScaffold.animationFindings), `${draft.mobId}: missing animation scaffolds`);
  for (const animation of draft.strictReviewScaffold.animationFindings) {
    assert.strictEqual(animation.draft.phaseMethod, 'manual', `${draft.mobId}:${animation.name}: invalid phase method default`);
    assert(Array.isArray(animation.manualChecklist) && animation.manualChecklist.length >= 4, `${draft.mobId}:${animation.name}: incomplete manual checklist`);
    assert(animation.draft.gameplaySync.damage.status === 'unknown', `${draft.mobId}:${animation.name}: damage sync must default to unknown`);
  }
}

assert(
  !document.drafts.some((draft) => draft.mobId === 'level_1_grove_guardian'),
  'Completed Grove Guardian review must not regenerate a pending draft',
);

console.log(`ACTIVE_RUNTIME_REVIEW_DRAFTS_TEST_PASS: ${document.drafts.length} drafts`);
