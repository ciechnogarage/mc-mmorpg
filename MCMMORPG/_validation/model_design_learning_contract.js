const REQUIRED_DECISION_FIELDS = [
  'intent',
  'principle',
  'limitation',
  'deliberateDifference',
  'hypothesis',
  'acceptanceEvidence',
];

const VALID_OUTCOMES = new Set(['confirmed', 'rejected', 'inconclusive']);
const VALID_EVIDENCE = new Set(['render_comparison', 'editor_observation', 'runtime_observation']);

function validateLearningEntry(entry) {
  const errors = [];
  if (!entry?.id) errors.push('id is required');
  if (!entry?.modelId) errors.push('modelId is required');
  if (!entry?.createdAt) errors.push('createdAt is required');
  if (!VALID_OUTCOMES.has(entry?.outcome)) errors.push('outcome must be confirmed, rejected, or inconclusive');
  if (!Array.isArray(entry?.decisions) || !entry.decisions.length) {
    errors.push('at least one subsystem decision is required');
  } else {
    for (const [index, decision] of entry.decisions.entries()) {
      if (!decision.subsystem) errors.push(`decisions[${index}].subsystem is required`);
      for (const field of REQUIRED_DECISION_FIELDS) {
        if (!decision[field] || (Array.isArray(decision[field]) && !decision[field].length)) {
          errors.push(`decisions[${index}].${field} is required`);
        }
      }
      if (!Array.isArray(decision.references) || decision.references.length < 2) {
        errors.push(`decisions[${index}] needs at least two independent references`);
      } else if (new Set(decision.references).size !== decision.references.length) {
        errors.push(`decisions[${index}] references must be unique`);
      }
      for (const evidence of decision.acceptanceEvidence || []) {
        if (!VALID_EVIDENCE.has(evidence.sourceClass)) {
          errors.push(`decisions[${index}] has invalid evidence sourceClass`);
        }
        if (!evidence.path && !evidence.observation) {
          errors.push(`decisions[${index}] evidence needs path or observation`);
        }
      }
    }
  }
  if (entry?.outcome === 'confirmed' && !entry?.reusableLesson) {
    errors.push('confirmed learning requires reusableLesson');
  }
  return errors;
}

module.exports = {
  REQUIRED_DECISION_FIELDS,
  VALID_EVIDENCE,
  VALID_OUTCOMES,
  validateLearningEntry,
};
