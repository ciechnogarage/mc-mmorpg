import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createApproval,
  verifyApproval,
  InMemoryNonceStore,
  NonceAlreadyUsedError,
  APPROVAL_OWNER_UID_ROOT,
} from '../src/contract/approval.mjs';
import { buildPlan, buildApproval, SECRET_KEY } from './fixtures.mjs';

test('a freshly minted approval verifies against its plan', () => {
  const plan = buildPlan();
  const approval = buildApproval(plan);
  assert.equal(verifyApproval(approval, { plan, secretKey: SECRET_KEY }).ok, true);
  assert.equal(approval.ownerUid, APPROVAL_OWNER_UID_ROOT);
});

test('missing approval is reported, not thrown', () => {
  const plan = buildPlan();
  assert.equal(verifyApproval(null, { plan, secretKey: SECRET_KEY }).code, 'approval-missing');
});

test('approval minted with a different key does not verify (spoof)', () => {
  const plan = buildPlan();
  const forged = createApproval({ plan, operator: 'mallory', ttlSeconds: 3600, secretKey: 'attacker-key' });
  assert.equal(verifyApproval(forged, { plan, secretKey: SECRET_KEY }).code, 'approval-mac-invalid');
});

test('tampering any bound field invalidates the MAC', () => {
  const plan = buildPlan();
  const approval = buildApproval(plan);
  assert.equal(verifyApproval({ ...approval, operator: 'mallory' }, { plan, secretKey: SECRET_KEY }).code, 'approval-mac-invalid');
  assert.equal(verifyApproval({ ...approval, ttlSeconds: 999999 }, { plan, secretKey: SECRET_KEY }).code, 'approval-mac-invalid');
  assert.equal(verifyApproval({ ...approval, ownerUid: 0, nonce: 'x'.repeat(32) }, { plan, secretKey: SECRET_KEY }).code, 'approval-mac-invalid');
});

test('approval bound to a different plan hash is rejected', () => {
  const plan = buildPlan();
  const approval = buildApproval(plan);
  const otherPlan = buildPlan({ id: plan.id }); // same id, but mutate hash below
  otherPlan.hash = 'c'.repeat(64);
  assert.equal(verifyApproval(approval, { plan: otherPlan, secretKey: SECRET_KEY }).code, 'approval-hash-mismatch');
});

test('non-root approval is rejected', () => {
  const plan = buildPlan();
  const approval = buildApproval(plan, { ownerUid: 1000 });
  assert.equal(verifyApproval(approval, { plan, secretKey: SECRET_KEY }).code, 'approval-not-root-owned');
});

test('verifyOwner probe failure is rejected', () => {
  const plan = buildPlan();
  const approval = buildApproval(plan);
  const r = verifyApproval(approval, { plan, secretKey: SECRET_KEY, verifyOwner: () => false });
  assert.equal(r.code, 'approval-owner-check-failed');
});

test('expired approval (TTL elapsed) is rejected', () => {
  const plan = buildPlan();
  const issuedAt = Date.now() - 2 * 3600 * 1000;
  const approval = buildApproval(plan, { ttlSeconds: 3600, issuedAt });
  assert.equal(verifyApproval(approval, { plan, secretKey: SECRET_KEY }).code, 'approval-expired');
});

test('approval from the future is rejected', () => {
  const plan = buildPlan();
  const approval = buildApproval(plan, { issuedAt: Date.now() + 60_000 });
  assert.equal(verifyApproval(approval, { plan, secretKey: SECRET_KEY }).code, 'approval-not-yet-valid');
});

test('nonce store consumes exactly once', () => {
  const store = new InMemoryNonceStore();
  store.consume('n1');
  assert.equal(store.has('n1'), true);
  assert.throws(() => store.consume('n1'), NonceAlreadyUsedError);
});
