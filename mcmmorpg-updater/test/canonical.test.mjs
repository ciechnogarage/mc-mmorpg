import test from 'node:test';
import assert from 'node:assert/strict';
import { canonicalize, canonicalHash } from '../src/contract/canonical.mjs';
import { computePlanHash } from '../src/contract/plan-schema.mjs';
import { buildPlan, buildPlanContent } from './fixtures.mjs';

test('canonicalization is stable under key reordering', () => {
  const a = { z: 1, a: { d: 4, c: 3 }, m: [{ y: 2, x: 1 }] };
  const b = { a: { c: 3, d: 4 }, m: [{ x: 1, y: 2 }], z: 1 };
  assert.equal(canonicalize(a), canonicalize(b));
  assert.equal(canonicalHash(a), canonicalHash(b));
});

test('array order is significant', () => {
  assert.notEqual(canonicalHash([1, 2, 3]), canonicalHash([3, 2, 1]));
});

test('plan content hash is reproducible regardless of member insertion order', () => {
  const ordered = buildPlanContent();
  const reordered = {
    operations: ordered.operations,
    targets: ordered.targets,
    createdAt: ordered.createdAt,
    operator: ordered.operator,
  };
  const planA = { schemaVersion: 1, id: 'p', state: 'draft', content: ordered, hash: '' };
  const planB = { schemaVersion: 1, id: 'p', state: 'draft', content: reordered, hash: '' };
  assert.equal(computePlanHash(planA), computePlanHash(planB));
});

test('any content change alters the hash', () => {
  const base = buildPlan();
  const mutated = buildPlan({ content: buildPlanContent({ operator: 'mallory' }) });
  assert.notEqual(base.hash, mutated.hash);
});

test('non-finite numbers are rejected as non-canonicalizable', () => {
  assert.throws(() => canonicalize({ x: Number.POSITIVE_INFINITY }), /non-finite/);
});
