import test from 'node:test';
import assert from 'node:assert/strict';
import {
  EXECUTOR_OPERATIONS,
  isKnownOperation,
  validateOperation,
} from '../src/contract/operation-policy.mjs';
import { SHA_B } from './fixtures.mjs';

const TARGETS = new Set(['world']);

test('allowlist contains exactly the nine declared operations', () => {
  assert.equal(EXECUTOR_OPERATIONS.length, 9);
  assert.deepEqual(
    [...EXECUTOR_OPERATIONS].sort(),
    [
      'backup',
      'compose-run-test',
      'compose-start',
      'compose-stop',
      'deploy',
      'health-check',
      'inventory',
      'prepare-staging',
      'rollback',
    ],
  );
});

test('a tenth (unknown) operation name is rejected', () => {
  assert.equal(isKnownOperation('exec-shell'), false);
  const r = validateOperation({ op: 'exec-shell', target: 'world' }, TARGETS);
  assert.equal(r.ok, false);
  assert.equal(r.code, 'operation-not-allowed');
});

test('rollback is a first-class operation, not a deploy argument', () => {
  assert.equal(validateOperation({ op: 'rollback', target: 'world' }, TARGETS).ok, true);
  // Passing rollback as a deploy arg is a forbidden (closed-schema) key.
  const r = validateOperation(
    { op: 'deploy', target: 'world', args: { candidateSha256: SHA_B, rollback: true } },
    TARGETS,
  );
  assert.equal(r.ok, false);
  assert.equal(r.code, 'operation-arg-unknown');
});

test('closed args reject a smuggled shell command / advanced escape hatch', () => {
  for (const badKey of ['command', 'shell', 'advanced', 'cmd']) {
    const r = validateOperation(
      { op: 'backup', target: 'world', args: { [badKey]: 'rm -rf /' } },
      TARGETS,
    );
    assert.equal(r.ok, false, `${badKey} should be rejected`);
    assert.equal(r.code, 'operation-arg-unknown');
  }
});

test('operation referencing an unknown target is rejected', () => {
  const r = validateOperation({ op: 'backup', target: 'nether' }, TARGETS);
  assert.equal(r.ok, false);
  assert.equal(r.code, 'unknown-target');
});

test('deploy requires a valid candidate sha256', () => {
  assert.equal(validateOperation({ op: 'deploy', target: 'world' }, TARGETS).code, 'operation-arg-missing');
  assert.equal(
    validateOperation({ op: 'deploy', target: 'world', args: { candidateSha256: 'nope' } }, TARGETS).code,
    'operation-arg-invalid',
  );
  assert.equal(
    validateOperation({ op: 'deploy', target: 'world', args: { candidateSha256: SHA_B } }, TARGETS).ok,
    true,
  );
});

test('bounded integer args are range-checked', () => {
  assert.equal(
    validateOperation({ op: 'health-check', target: 'world', args: { timeoutSeconds: 0 } }, TARGETS).code,
    'operation-arg-invalid',
  );
  assert.equal(
    validateOperation({ op: 'health-check', target: 'world', args: { graceSeconds: 30, timeoutSeconds: 60 } }, TARGETS).ok,
    true,
  );
});
