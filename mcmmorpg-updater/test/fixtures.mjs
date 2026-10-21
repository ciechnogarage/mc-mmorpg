// Shared fixtures for the contract tests: a valid plan and a valid approval.
import { computePlanHash } from '../src/contract/plan-schema.mjs';
import { createApproval } from '../src/contract/approval.mjs';

export const SECRET_KEY = 'test-shared-hmac-secret';
const SHA_A = 'a'.repeat(64);
const SHA_B = 'b'.repeat(64);

export function buildPlanContent(overrides = {}) {
  return {
    operator: 'alice',
    createdAt: '2026-07-19T10:00:00Z',
    targets: [
      {
        target: 'world',
        currentJar: { path: 'plugins/Foo.jar', sha256: SHA_A },
        candidateJar: { path: 'downloads/Foo-2.jar', sha256: SHA_B, source: 'vendor:foo/2.0.0' },
        expectedValidations: ['plugin-name', 'no-fatal-errors'],
      },
    ],
    operations: [
      { op: 'inventory', target: 'world' },
      { op: 'backup', target: 'world' },
      { op: 'compose-run-test', target: 'world', args: { timeoutSeconds: 120 } },
      { op: 'deploy', target: 'world', args: { candidateSha256: SHA_B } },
    ],
    ...overrides,
  };
}

export function buildPlan(overrides = {}) {
  const plan = {
    schemaVersion: 1,
    id: overrides.id ?? 'plan-0001',
    state: overrides.state ?? 'approved',
    content: overrides.content ?? buildPlanContent(),
    hash: '',
  };
  plan.hash = computePlanHash(plan);
  return plan;
}

export function buildApproval(plan, overrides = {}) {
  return createApproval({
    plan,
    operator: overrides.operator ?? 'alice',
    ttlSeconds: overrides.ttlSeconds ?? 3600,
    secretKey: overrides.secretKey ?? SECRET_KEY,
    issuedAt: overrides.issuedAt ?? Date.now(),
    nonce: overrides.nonce,
    ownerUid: overrides.ownerUid ?? 0,
  });
}

export { SHA_A, SHA_B };
