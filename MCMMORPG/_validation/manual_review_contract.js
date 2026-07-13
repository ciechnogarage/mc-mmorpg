const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const CLAIM_TYPES = new Set(['source_fact', 'visual_observation', 'runtime_observation', 'design_inference']);
const ARTIFACT_TYPES = new Set(['render', 'bbmodel', 'texture', 'yaml', 'runtime', 'quality_manifest']);
const BINDING_STATUSES = new Set(['confirmed', 'not_applicable', 'unknown']);
const PHASE_METHODS = new Set(['timeline', 'yaml', 'keyframe_motion', 'manual']);
const SYNC_CHANNELS = ['damage', 'hitbox', 'sound', 'vfx'];
const SOURCE_CLASSES = new Set(['reference_corpus', 'active_runtime_repo', 'generated_review', 'runtime_probe']);

function sha1(file) {
  return crypto.createHash('sha1').update(fs.readFileSync(file)).digest('hex');
}

function resolveEvidenceSource(source, reviewRoot) {
  if (!source || typeof source !== 'string') return null;
  if (path.isAbsolute(source)) return source;
  return path.resolve(reviewRoot, source);
}

function validateEvidence(evidence, context, reviewRoot, errors) {
  for (const field of ['artifact', 'sourceClass', 'source', 'locator', 'detail', 'sourceHash']) {
    if (!Object.hasOwn(evidence || {}, field) || evidence[field] === '') {
      errors.push(`${context}: evidence missing ${field}`);
    }
  }
  if (!ARTIFACT_TYPES.has(evidence?.artifact)) {
    errors.push(`${context}: invalid evidence artifact ${evidence?.artifact || '?'}`);
  }
  if (!SOURCE_CLASSES.has(evidence?.sourceClass)) {
    errors.push(`${context}: invalid evidence sourceClass ${evidence?.sourceClass || '?'}`);
  }
  if (!/^[a-f0-9]{40}$/.test(evidence?.sourceHash || '')) {
    errors.push(`${context}: evidence sourceHash must be SHA-1`);
  }
  if (typeof evidence?.locator !== 'string' || !/(\bline\b|\bframe\b|\bkeyframe\b|\bview\b|\btick\b|\bpath\b|\bnode\b|\bfield\b|\bcommand\b|\bevent\b)/i.test(evidence.locator)) {
    errors.push(`${context}: evidence locator is not concrete`);
  }

  const source = resolveEvidenceSource(evidence?.source, reviewRoot);
  if (!source || !fs.existsSync(source)) {
    errors.push(`${context}: evidence source does not exist: ${evidence?.source || '?'}`);
    return;
  }
  if (/^[a-f0-9]{40}$/.test(evidence.sourceHash) && sha1(source) !== evidence.sourceHash) {
    errors.push(`${context}: stale evidence hash for ${evidence.source}`);
  }
}

function validateReviewedSources(review, context, reviewRoot, errors) {
  if (!Array.isArray(review?.reviewedSources) || review.reviewedSources.length === 0) {
    errors.push(`${context}: missing reviewedSources`);
    return;
  }
  for (const [index, sourceRef] of review.reviewedSources.entries()) {
    const resolved = resolveEvidenceSource(sourceRef, reviewRoot);
    if (!resolved || !fs.existsSync(resolved)) {
      errors.push(`${context}: reviewedSources[${index}] does not exist: ${sourceRef || '?'}`);
    }
  }
}

function validatePhaseTimes(animation, context, errors) {
  const phases = ['start', 'anticipation', 'impact', 'recovery', 'end'];
  const values = phases.map((phase) => animation?.phaseTimes?.[phase]);
  if (values.some((value) => typeof value !== 'number' || !Number.isFinite(value))) {
    errors.push(`${context}: phaseTimes must contain five finite numbers`);
    return;
  }
  for (let i = 1; i < values.length; i += 1) {
    if (values[i] < values[i - 1]) {
      errors.push(`${context}: phaseTimes are not chronological`);
      break;
    }
  }
  if (!PHASE_METHODS.has(animation?.phaseMethod)) {
    errors.push(`${context}: invalid or missing phaseMethod`);
  }
  if (animation.phaseMethod === 'manual' && !animation.phaseRationale?.trim()) {
    errors.push(`${context}: manual phases require phaseRationale`);
  }
}

function validateGameplaySync(animation, context, reviewRoot, errors) {
  for (const channel of SYNC_CHANNELS) {
    const sync = animation?.gameplaySync?.[channel];
    if (!sync || !BINDING_STATUSES.has(sync.status)) {
      errors.push(`${context}: gameplaySync.${channel} requires confirmed, not_applicable, or unknown`);
      continue;
    }
    if (sync.status === 'confirmed') {
      if (!Array.isArray(sync.evidence) || sync.evidence.length === 0) {
        errors.push(`${context}: confirmed gameplaySync.${channel} lacks evidence`);
      } else {
        sync.evidence.forEach((item, index) =>
          validateEvidence(item, `${context}:gameplaySync.${channel}[${index}]`, reviewRoot, errors));
      }
    }
    if (sync.status === 'unknown' && !sync.reason?.trim()) {
      errors.push(`${context}: unknown gameplaySync.${channel} requires a reason`);
    }
  }
}

function validateStrictReview(review, card, standard, reviewRoot) {
  const errors = [];
  const context = review?.id || '<unknown>';

  for (const field of standard.requiredModelFields || []) {
    if (!Object.hasOwn(review || {}, field)) errors.push(`${context}: missing ${field}`);
  }
  if (!/^[a-f0-9]{40}$/.test(review?.sourceHash || '')) {
    errors.push(`${context}: invalid sourceHash`);
  } else if (card?.absoluteFile && fs.existsSync(card.absoluteFile) && sha1(card.absoluteFile) !== review.sourceHash) {
    errors.push(`${context}: stale sourceHash`);
  }
  validateReviewedSources(review, context, reviewRoot, errors);
  if (typeof review?.stale !== 'boolean') errors.push(`${context}: stale must be boolean`);

  if (!Array.isArray(review?.claims) || review.claims.length === 0) {
    errors.push(`${context}: missing evidence-backed claims`);
  } else {
    review.claims.forEach((claim, claimIndex) => {
      const claimContext = `${context}:claim[${claimIndex}]`;
      for (const field of standard.requiredClaimFields || []) {
        if (!Object.hasOwn(claim || {}, field)) errors.push(`${claimContext}: missing ${field}`);
      }
      if (!CLAIM_TYPES.has(claim?.type)) errors.push(`${claimContext}: invalid claim type`);
      if (!claim?.statement?.trim()) errors.push(`${claimContext}: empty statement`);
      if (!Array.isArray(claim?.evidence) || claim.evidence.length === 0) {
        errors.push(`${claimContext}: claim without evidence`);
      } else {
        claim.evidence.forEach((item, evidenceIndex) =>
          validateEvidence(item, `${claimContext}:evidence[${evidenceIndex}]`, reviewRoot, errors));
        if (claim.type === 'runtime_observation' && !claim.evidence.some((item) => item?.artifact === 'runtime')) {
          errors.push(`${claimContext}: runtime_observation requires runtime evidence`);
        }
      }
    });
  }

  if (!Array.isArray(review?.animationFindings)) {
    errors.push(`${context}: missing animationFindings`);
    return errors;
  }
  const expectedAnimations = new Set((card?.animationsDetail || card?.animations || []).map((item) =>
    typeof item === 'string' ? item : item.name));
  const foundAnimations = new Set();
  review.animationFindings.forEach((animation, animationIndex) => {
    const animationContext = `${context}:${animation?.name || `animation[${animationIndex}]`}`;
    for (const field of standard.requiredAnimationFields || []) {
      if (!Object.hasOwn(animation || {}, field)) errors.push(`${animationContext}: missing ${field}`);
    }
    if (foundAnimations.has(animation?.name)) errors.push(`${animationContext}: duplicate animation finding`);
    foundAnimations.add(animation?.name);
    if (!expectedAnimations.has(animation?.name)) errors.push(`${animationContext}: animation not present in source card`);
    validatePhaseTimes(animation, animationContext, errors);
    if (!Array.isArray(animation?.evidence) || animation.evidence.length === 0) {
      errors.push(`${animationContext}: lacks direct evidence`);
    } else {
      animation.evidence.forEach((item, evidenceIndex) =>
        validateEvidence(item, `${animationContext}:evidence[${evidenceIndex}]`, reviewRoot, errors));
    }
    validateGameplaySync(animation, animationContext, reviewRoot, errors);
  });
  for (const animationName of expectedAnimations) {
    if (!foundAnimations.has(animationName)) errors.push(`${context}:${animationName}: missing individual analysis`);
  }

  return errors;
}

function auditReviewDatabase(index, batches, cards, reviewRoot) {
  const errors = [];
  const strict = [];
  const legacy = [];
  const reviewedKeys = new Set();
  const cardsById = new Map();
  const cardsByKey = new Map();
  for (const card of cards) {
    const modelKey = `${card.id}::${card.file}`;
    cardsByKey.set(modelKey, card);
    if (!cardsById.has(card.id)) cardsById.set(card.id, []);
    cardsById.get(card.id).push(card);
  }

  for (const batch of batches) {
    const isLegacy = batch.reviewStatus?.startsWith('legacy_');
    for (const review of batch.models || []) {
      const candidates = cardsById.get(review.id) || [];
      const modelKey = review.modelKey || (candidates.length === 1
        ? `${candidates[0].id}::${candidates[0].file}`
        : null);
      if (!modelKey && candidates.length > 1) {
        errors.push(`${review.id}: duplicate corpus ID requires modelKey`);
      }
      if (modelKey && reviewedKeys.has(modelKey)) errors.push(`duplicate manual review: ${modelKey}`);
      if (modelKey) reviewedKeys.add(modelKey);
      const card = modelKey ? cardsByKey.get(modelKey) : null;
      if (!card) errors.push(`${review.id}: model is absent from corpus cards or modelKey is invalid`);
      if (isLegacy) {
        legacy.push(review);
        continue;
      }
      const reviewErrors = validateStrictReview(
        review,
        card,
        index.reviewStandard || {},
        reviewRoot,
      );
      errors.push(...reviewErrors);
      strict.push(review);
    }
  }

  const strictAnimations = strict.reduce((sum, review) => sum + review.animationFindings.length, 0);
  const legacyAnimations = legacy.reduce((sum, review) => sum + (review.animationsReviewed || []).length, 0);
  const staleModels = strict.filter((review) => review.stale).map((review) => review.id);
  const missingModels = [...cardsByKey.keys()].filter((modelKey) => !reviewedKeys.has(modelKey));
  const familyCoverage = {};
  for (const card of cards) {
    const family = card.familyId || card.id;
    familyCoverage[family] ||= { total: 0, strict: 0, legacy: 0, missing: 0 };
    familyCoverage[family].total += 1;
    const modelKey = `${card.id}::${card.file}`;
    const matchingStrict = strict.some((review) =>
      (review.modelKey || (cardsById.get(review.id)?.length === 1
        ? `${review.id}::${cardsById.get(review.id)[0].file}`
        : null)) === modelKey);
    const matchingLegacy = legacy.some((review) =>
      (review.modelKey || (cardsById.get(review.id)?.length === 1
        ? `${review.id}::${cardsById.get(review.id)[0].file}`
        : null)) === modelKey);
    if (matchingStrict) familyCoverage[family].strict += 1;
    else if (matchingLegacy) familyCoverage[family].legacy += 1;
    else familyCoverage[family].missing += 1;
  }

  return {
    errors,
    counts: {
      strictModels: strict.length,
      strictAnimations,
      legacyModels: legacy.length,
      legacyAnimations,
      staleModels: staleModels.length,
      missingModels: missingModels.length,
    },
    staleModels,
    missingModels,
    familyCoverage,
  };
}

module.exports = {
  ARTIFACT_TYPES,
  CLAIM_TYPES,
  PHASE_METHODS,
  SOURCE_CLASSES,
  SYNC_CHANNELS,
  auditReviewDatabase,
  sha1,
  validateStrictReview,
};
