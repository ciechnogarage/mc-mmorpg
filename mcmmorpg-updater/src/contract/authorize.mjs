// Authorization decision: does this plan + approval permit execution right now?
//
// This ties the pieces together and is the one function the executor calls before
// performing any operation. It fails closed: any structural, integrity, approval,
// replay or locking problem raises AuthorizationError and produces a rejection
// report. On success it returns the audit report plus a `release` handle that
// holds the per-target locks for the duration of execution.
//
// Ordering rationale:
//   1. validate plan structure + content-hash integrity
//   2. require the plan to be approved
//   3. verify the approval (MAC, plan binding, root ownership, TTL)
//   4. acquire per-target locks (transient contention → reject, nonce untouched)
//   5. consume the nonce (single-use commit; replay → reject, locks released)
// Locks are taken before the nonce is consumed so a mere lock conflict never
// burns a still-valid single-use approval.

import { validatePlanIntegrity } from './plan-schema.mjs';
import { verifyApproval } from './approval.mjs';
import { acquireAll, TargetLockedError } from './lock.mjs';
import { buildApprovalReport } from './report.mjs';

export class AuthorizationError extends Error {
  constructor(code, message, { report = null } = {}) {
    super(message);
    this.name = 'AuthorizationError';
    this.code = code;
    this.report = report;
  }
}

/**
 * @returns {{ authorized: true, report: object, release: () => void }}
 * @throws {AuthorizationError} on any rejection (carries a rejection `report`).
 */
export function authorizeExecution({
  plan,
  approval,
  secretKey,
  nonceStore,
  lockManager,
  now = Date.now(),
  expectedOwnerUid = 0,
  verifyOwner,
}) {
  if (!nonceStore || typeof nonceStore.consume !== 'function') {
    throw new TypeError('nonceStore with a consume(nonce) method is required');
  }
  if (!lockManager || typeof lockManager.acquire !== 'function') {
    throw new TypeError('lockManager with an acquire(target) method is required');
  }

  // 1. Plan structure + hash integrity.
  let targets;
  try {
    ({ targets } = validatePlanIntegrity(plan));
  } catch (err) {
    throw reject(err.code ?? 'plan-invalid', err.message, { plan, approval, now });
  }

  // 2. Only approved plans may proceed to approval verification or mutate
  // authorization state. All other valid lifecycle states fail closed here.
  if (plan.state !== 'approved') {
    throw reject('plan-not-approved', 'plan.state must be approved for execution', { plan, approval, now });
  }

  // 3. Approval verification (also enforces plan binding via hash/id).
  const av = verifyApproval(approval, { plan, secretKey, now, expectedOwnerUid, verifyOwner });
  if (!av.ok) {
    throw reject(av.code, av.reason, { plan, approval, now });
  }

  // 4. Per-target locks (all-or-nothing).
  const targetList = [...targets];
  let locks;
  try {
    locks = acquireAll(lockManager, targetList);
  } catch (err) {
    const code = err instanceof TargetLockedError ? 'target-locked' : 'lock-failed';
    throw reject(code, err.message, { plan, approval, now });
  }

  // 5. Single-use nonce commit.
  try {
    nonceStore.consume(approval.nonce);
  } catch (err) {
    locks.release();
    throw reject('approval-replayed', `single-use approval already consumed: ${err.message}`, { plan, approval, now });
  }

  const report = buildApprovalReport({ plan, approval, decision: 'authorized', now });
  return { authorized: true, report, release: locks.release };
}

function reject(code, reason, { plan, approval, now }) {
  const report = buildApprovalReport({ plan, approval, decision: 'rejected', reasonCode: code, reason, now });
  return new AuthorizationError(code, reason, { report });
}
