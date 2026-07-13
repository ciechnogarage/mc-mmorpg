const assert = require('assert');
const fs = require('fs');
const path = require('path');

const validationRoot = __dirname;
const pipeline = JSON.parse(fs.readFileSync(path.join(validationRoot, 'reference_corpus', 'boss_review_pipeline.json'), 'utf8'));
const document = JSON.parse(fs.readFileSync(path.join(validationRoot, 'active_runtime_reviews', 'runtime_boss_review_templates.json'), 'utf8'));

assert.strictEqual(document.schemaVersion, 1, 'Template schema mismatch');
assert.strictEqual(
  document.templates.length,
  pipeline.activeRuntimeBosses.length,
  'Template count must match active runtime boss count',
);

for (const template of document.templates) {
  assert(
    ['pending_manual_review', 'completed_manual_review'].includes(template.status),
    `${template.mobId}: invalid template status`,
  );
  assert.strictEqual(template.sourceClass, 'active_runtime_repo', `${template.mobId}: invalid sourceClass`);
  assert(Array.isArray(template.reviewedSources) && template.reviewedSources.length > 0, `${template.mobId}: missing reviewedSources`);
  assert(template.evidenceAnchors.qualityManifest, `${template.mobId}: missing quality manifest anchor`);
  assert(Array.isArray(template.animationTemplates) && template.animationTemplates.length > 0, `${template.mobId}: missing animation templates`);
  const animationNames = new Set();
  for (const animation of template.animationTemplates) {
    assert.strictEqual(
      animation.status,
      template.queueStatus === 'strict_complete' ? 'completed_manual_review' : 'pending_manual_review',
      `${template.mobId}:${animation.name}: invalid animation status`,
    );
    assert(!animationNames.has(animation.name), `${template.mobId}:${animation.name}: duplicate animation template`);
    animationNames.add(animation.name);
    assert(Array.isArray(animation.manualChecklist) && animation.manualChecklist.length >= 4, `${template.mobId}:${animation.name}: incomplete checklist`);
  }
}

const grove = document.templates.find((template) => template.mobId === 'level_1_grove_guardian');
assert(grove, 'Missing Grove Guardian active template');
assert.strictEqual(grove.queueStatus, 'strict_complete', 'Grove Guardian template should be strict-complete');
assert.strictEqual(grove.status, 'completed_manual_review', 'Grove Guardian template completion is missing');
assert(grove.evidenceAnchors.runtimeProbe, 'Grove Guardian template must include runtime probe anchor');
assert(grove.animationTemplates.some((animation) => animation.name === 'attack'), 'Grove Guardian template must include attack animation');
assert(grove.animationTemplates.some((animation) => animation.name === 'root_smash'), 'Grove Guardian template must include root_smash animation');

console.log(`ACTIVE_RUNTIME_REVIEW_TEMPLATES_TEST_PASS: ${document.templates.length} templates`);
