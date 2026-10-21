// Versioned update-plan schema, structural validation and hash integrity.
//
// A plan envelope separates immutable, hashed CONTENT from the mutable envelope
// (its self-hash and lifecycle state). The canonical hash covers `content` only,
// so recomputing it and comparing to `plan.hash` tells the executor whether the
// approved content is unchanged. Every datum required to execute deterministically
// and to reconstruct the decision lives inside `content`.

import { canonicalHash } from './canonical.mjs';
import { validateOperation, isKnownOperation } from './operation-policy.mjs';

export const PLAN_SCHEMA_VERSION = 1;

export const PLAN_STATES = Object.freeze(['draft', 'approved', 'executed', 'rejected']);

const HEX64 = /^[0-9a-f]{64}$/;
const isSha256 = (v) => typeof v === 'string' && HEX64.test(v);
const isNonEmptyString = (v) => typeof v === 'string' && v.length > 0;

export class PlanValidationError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'PlanValidationError';
    this.code = code;
  }
}

/**
 * Compute the canonical content hash of a plan (over `plan.content` only).
 */
export function computePlanHash(plan) {
  if (plan === null || typeof plan !== 'object' || typeof plan.content !== 'object' || plan.content === null) {
    throw new PlanValidationError('plan-malformed', 'plan.content must be an object');
  }
  return canonicalHash(plan.content);
}

/**
 * Structural validation of a plan envelope and its content. Does not check the
 * self-hash; see {@link validatePlanIntegrity}.
 * @returns {{ ok: true, targets: Set<string> } | { ok: false, code, reason }}
 */
export function validatePlanStructure(plan) {
  if (plan === null || typeof plan !== 'object' || Array.isArray(plan)) {
    return bad('plan-malformed', 'plan must be an object');
  }
  if (plan.schemaVersion !== PLAN_SCHEMA_VERSION) {
    return bad('plan-schema-version', `unsupported schemaVersion: ${plan.schemaVersion}`);
  }
  if (!isNonEmptyString(plan.id)) {
    return bad('plan-malformed', 'plan.id must be a non-empty string');
  }
  if (!PLAN_STATES.includes(plan.state)) {
    return bad('plan-state-invalid', `plan.state must be one of ${PLAN_STATES.join(', ')}`);
  }
  if (!isSha256(plan.hash)) {
    return bad('plan-malformed', 'plan.hash must be a 64-char hex SHA-256');
  }

  const content = plan.content;
  if (content === null || typeof content !== 'object' || Array.isArray(content)) {
    return bad('plan-malformed', 'plan.content must be an object');
  }
  if (!isNonEmptyString(content.operator)) {
    return bad('plan-malformed', 'content.operator must be a non-empty string');
  }
  if (!isNonEmptyString(content.createdAt)) {
    return bad('plan-malformed', 'content.createdAt must be a non-empty string');
  }
  if (!Array.isArray(content.targets) || content.targets.length === 0) {
    return bad('plan-malformed', 'content.targets must be a non-empty array');
  }
  if (!Array.isArray(content.operations) || content.operations.length === 0) {
    return bad('plan-malformed', 'content.operations must be a non-empty array');
  }

  const targetNames = new Set();
  for (const t of content.targets) {
    const r = validateTarget(t);
    if (!r.ok) return r;
    if (targetNames.has(t.target)) {
      return bad('plan-target-duplicate', `duplicate target "${t.target}"`);
    }
    targetNames.add(t.target);
  }

  for (const op of content.operations) {
    const r = validateOperation(op, targetNames);
    if (!r.ok) return r;
  }

  return { ok: true, targets: targetNames };
}

function validateTarget(t) {
  if (t === null || typeof t !== 'object' || Array.isArray(t)) {
    return bad('plan-target-malformed', 'target must be an object');
  }
  if (!isNonEmptyString(t.target)) {
    return bad('plan-target-malformed', 'target.target must be a non-empty string');
  }
  const cur = t.currentJar;
  const cand = t.candidateJar;
  if (!isJarRef(cur)) {
    return bad('plan-target-malformed', `target "${t.target}" currentJar is malformed`);
  }
  if (!isJarRef(cand)) {
    return bad('plan-target-malformed', `target "${t.target}" candidateJar is malformed`);
  }
  if (!isNonEmptyString(cand.source)) {
    return bad('plan-target-malformed', `target "${t.target}" candidateJar.source is required`);
  }
  if (t.expectedValidations !== undefined) {
    if (!Array.isArray(t.expectedValidations) || !t.expectedValidations.every(isNonEmptyString)) {
      return bad('plan-target-malformed', `target "${t.target}" expectedValidations must be strings`);
    }
  }
  return { ok: true };
}

function isJarRef(ref) {
  return (
    ref !== null &&
    typeof ref === 'object' &&
    !Array.isArray(ref) &&
    isNonEmptyString(ref.path) &&
    isSha256(ref.sha256)
  );
}

/**
 * Full validation: structure + content-hash integrity. Throws on any failure.
 * @returns {{ targets: Set<string> }}
 */
export function validatePlanIntegrity(plan) {
  const structural = validatePlanStructure(plan);
  if (!structural.ok) {
    throw new PlanValidationError(structural.code, structural.reason);
  }
  const expected = canonicalHash(plan.content);
  if (plan.hash !== expected) {
    throw new PlanValidationError(
      'plan-hash-mismatch',
      `plan.hash does not match content hash (declared ${plan.hash}, actual ${expected})`,
    );
  }
  return { targets: structural.targets };
}

function bad(code, reason) {
  return { ok: false, code, reason };
}

export { isKnownOperation };
