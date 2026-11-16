import test from 'node:test';
import assert from 'node:assert/strict';
import {
  PLAN_SCHEMA_VERSION,
  PlanValidationError,
  validatePlanIntegrity,
  validatePlanStructure,
  computePlanHash,
} from '../src/contract/plan-schema.mjs';
import { buildPlan, buildPlanContent, SHA_A, SHA_B } from './fixtures.mjs';

test('a well-formed plan passes structure and integrity', () => {
  const plan = buildPlan();
  assert.equal(validatePlanStructure(plan).ok, true);
  const { targets } = validatePlanIntegrity(plan);
  assert.ok(targets.has('world'));
});

test('unsupported schema version is rejected', () => {
  const plan = buildPlan();
  plan.schemaVersion = PLAN_SCHEMA_VERSION + 1;
  assert.equal(validatePlanStructure(plan).code, 'plan-schema-version');
});

test('content mutated after hashing fails integrity (hash change)', () => {
  const plan = buildPlan();
  plan.content.operator = 'mallory'; // hash no longer matches
  assert.throws(() => validatePlanIntegrity(plan), (err) => {
    assert.ok(err instanceof PlanValidationError);
    assert.equal(err.code, 'plan-hash-mismatch');
    return true;
  });
});

test('unknown operation in the plan is rejected', () => {
  const content = buildPlanContent({
    operations: [{ op: 'wormhole', target: 'world' }],
  });
  const plan = buildPlan({ content });
  assert.equal(validatePlanStructure(plan).code, 'operation-not-allowed');
});

test('operation targeting an undeclared target is rejected', () => {
  const content = buildPlanContent({
    operations: [{ op: 'inventory', target: 'ghost' }],
  });
  const plan = buildPlan({ content });
  assert.equal(validatePlanStructure(plan).code, 'unknown-target');
});

test('target with a malformed jar reference is rejected', () => {
  const content = buildPlanContent({
    targets: [
      {
        target: 'world',
        currentJar: { path: 'plugins/Foo.jar', sha256: 'short' },
        candidateJar: { path: 'downloads/Foo-2.jar', sha256: SHA_B, source: 'vendor:foo/2.0.0' },
      },
    ],
    operations: [{ op: 'inventory', target: 'world' }],
  });
  const plan = buildPlan({ content });
  assert.equal(validatePlanStructure(plan).code, 'plan-target-malformed');
});

test('candidate jar without a source is rejected', () => {
  const content = buildPlanContent({
    targets: [
      {
        target: 'world',
        currentJar: { path: 'plugins/Foo.jar', sha256: SHA_A },
        candidateJar: { path: 'downloads/Foo-2.jar', sha256: SHA_B },
      },
    ],
    operations: [{ op: 'inventory', target: 'world' }],
  });
  assert.equal(validatePlanStructure(buildPlan({ content })).code, 'plan-target-malformed');
});

test('duplicate targets are rejected', () => {
  const tgt = {
    target: 'world',
    currentJar: { path: 'plugins/Foo.jar', sha256: SHA_A },
    candidateJar: { path: 'downloads/Foo-2.jar', sha256: SHA_B, source: 's' },
  };
  const content = buildPlanContent({ targets: [tgt, { ...tgt }], operations: [{ op: 'inventory', target: 'world' }] });
  assert.equal(validatePlanStructure(buildPlan({ content })).code, 'plan-target-duplicate');
});

test('computePlanHash on a malformed plan throws', () => {
  assert.throws(() => computePlanHash({}), /plan\.content/);
});
