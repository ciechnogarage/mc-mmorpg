import test from 'node:test';
import assert from 'node:assert/strict';
import { authorizeExecution, AuthorizationError } from '../src/contract/authorize.mjs';
import { InMemoryNonceStore } from '../src/contract/approval.mjs';
import { InMemoryLockManager } from '../src/contract/lock.mjs';
import { buildPlan, buildApproval, buildPlanContent, SECRET_KEY } from './fixtures.mjs';

function harness() {
  return { nonceStore: new InMemoryNonceStore(), lockManager: new InMemoryLockManager() };
}

function authorize(plan, approval, ctx, extra = {}) {
  return authorizeExecution({
    plan,
    approval,
    secretKey: SECRET_KEY,
    nonceStore: ctx.nonceStore,
    lockManager: ctx.lockManager,
    ...extra,
  });
}

// Capture a thrown error (assert.throws does not return it).
function grab(fn) {
  try {
    fn();
  } catch (err) {
    return err;
  }
  throw new assert.AssertionError({ message: 'expected function to throw, but it did not' });
}

test('valid path: approved, unchanged plan authorizes and reports operator/time/id/hash', () => {
  const ctx = harness();
  const plan = buildPlan();
  const approval = buildApproval(plan);
  const { authorized, report, release } = authorize(plan, approval, ctx);
  assert.equal(authorized, true);
  assert.equal(report.decision, 'authorized');
  assert.equal(report.operator, 'alice');
  assert.equal(report.planId, plan.id);
  assert.equal(report.planHash, plan.hash);
  assert.match(report.decidedAt, /^\d{4}-\d{2}-\d{2}T/);
  assert.deepEqual(report.targets, ['world']);
  release();
});

for (const state of ['draft', 'executed', 'rejected']) {
  test(`${state} plan is rejected without consuming its nonce or retaining a target lock`, () => {
    const ctx = harness();
    const plan = buildPlan({ state });
    const approval = buildApproval(plan);

    const err = grab(() => authorize(plan, approval, ctx));

    assert.ok(err instanceof AuthorizationError);
    assert.equal(err.code, 'plan-not-approved');
    assert.equal(err.report.reasonCode, 'plan-not-approved');
    assert.equal(ctx.nonceStore.has(approval.nonce), false);
    assert.equal(ctx.lockManager.isLocked('world'), false);
  });
}

test('non-approved plan state takes precedence over approval verification', () => {
  const ctx = harness();
  const plan = buildPlan({ state: 'draft' });
  const forged = buildApproval(plan, { secretKey: 'attacker-key' });

  assert.equal(grab(() => authorize(plan, forged, ctx)).code, 'plan-not-approved');
});

test('missing approval is rejected with an audit report', () => {
  const ctx = harness();
  const plan = buildPlan();
  const err = grab(() => authorize(plan, null, ctx));
  assert.ok(err instanceof AuthorizationError);
  assert.equal(err.code, 'approval-missing');
  assert.equal(err.report.decision, 'rejected');
  assert.equal(err.report.reasonCode, 'approval-missing');
});

test('plan modified after hashing is rejected (integrity)', () => {
  const ctx = harness();
  const plan = buildPlan();
  const approval = buildApproval(plan);
  plan.content.operator = 'mallory'; // hash now stale
  assert.equal(grab(() => authorize(plan, approval, ctx)).code, 'plan-hash-mismatch');
});

test('plan re-hashed after approval no longer matches the approval (bound hash change)', () => {
  const ctx = harness();
  const plan = buildPlan();
  const approval = buildApproval(plan);
  // Attacker swaps in different content AND recomputes the self-hash so integrity
  // passes — but the approval still binds the original hash.
  const tampered = buildPlan({ id: plan.id, content: buildPlanContent({ operator: 'mallory' }) });
  assert.equal(grab(() => authorize(tampered, approval, ctx)).code, 'approval-hash-mismatch');
});

test('unknown operation in plan is rejected', () => {
  const ctx = harness();
  const content = buildPlanContent({ operations: [{ op: 'exec-shell', target: 'world' }] });
  const plan = buildPlan({ content });
  const approval = buildApproval(plan);
  assert.equal(grab(() => authorize(plan, approval, ctx)).code, 'operation-not-allowed');
});

test('unknown target reference in plan is rejected', () => {
  const ctx = harness();
  const content = buildPlanContent({ operations: [{ op: 'inventory', target: 'nether' }] });
  const plan = buildPlan({ content });
  const approval = buildApproval(plan);
  assert.equal(grab(() => authorize(plan, approval, ctx)).code, 'unknown-target');
});

test('spoofed approval (wrong key) is rejected', () => {
  const ctx = harness();
  const plan = buildPlan();
  const forged = buildApproval(plan, { secretKey: 'attacker-key' });
  assert.equal(grab(() => authorize(plan, forged, ctx)).code, 'approval-mac-invalid');
});

test('expired approval is rejected', () => {
  const ctx = harness();
  const plan = buildPlan();
  const approval = buildApproval(plan, { ttlSeconds: 60, issuedAt: Date.now() - 3600 * 1000 });
  assert.equal(grab(() => authorize(plan, approval, ctx)).code, 'approval-expired');
});

test('replay of a single-use approval is rejected the second time', () => {
  const ctx = harness();
  const plan = buildPlan();
  const approval = buildApproval(plan);
  const first = authorize(plan, approval, ctx);
  first.release(); // release the lock so only the nonce blocks the replay
  assert.equal(grab(() => authorize(plan, approval, ctx)).code, 'approval-replayed');
});

test('concurrent execution of the same target is rejected while locked', () => {
  const ctx = harness();
  const planA = buildPlan({ id: 'plan-A' });
  const planB = buildPlan({ id: 'plan-B' }); // also targets "world"
  const first = authorize(planA, buildApproval(planA), ctx); // holds world lock
  assert.equal(grab(() => authorize(planB, buildApproval(planB), ctx)).code, 'target-locked');
  // A lock conflict must NOT consume planB's single-use approval.
  const approvalB = buildApproval(planB);
  first.release();
  const second = authorize(planB, approvalB, ctx);
  assert.equal(second.authorized, true);
  second.release();
});

test('a target lock conflict does not burn the blocked plan\'s nonce', () => {
  const ctx = harness();
  const planA = buildPlan({ id: 'plan-A' });
  const planB = buildPlan({ id: 'plan-B' });
  const approvalB = buildApproval(planB);
  const first = authorize(planA, buildApproval(planA), ctx);
  assert.equal(grab(() => authorize(planB, approvalB, ctx)).code, 'target-locked'); // blocked
  first.release();
  // Same approvalB now succeeds -> its nonce was never consumed by the conflict.
  const ok = authorize(planB, approvalB, ctx);
  assert.equal(ok.authorized, true);
  ok.release();
});

test('non-root approval is rejected end-to-end', () => {
  const ctx = harness();
  const plan = buildPlan();
  const approval = buildApproval(plan, { ownerUid: 1000 });
  assert.equal(grab(() => authorize(plan, approval, ctx)).code, 'approval-not-root-owned');
});

test('verifyOwner probe failure is rejected end-to-end', () => {
  const ctx = harness();
  const plan = buildPlan();
  const approval = buildApproval(plan);
  assert.equal(grab(() => authorize(plan, approval, ctx, { verifyOwner: () => false })).code, 'approval-owner-check-failed');
});
