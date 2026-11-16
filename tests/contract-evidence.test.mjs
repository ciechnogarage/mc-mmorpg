import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const FIXTURES = path.join(ROOT, 'tests', 'fixtures', 'contract-evidence');

function run(args) {
  return spawnSync('python3', ['scripts/check-contract-evidence.py', ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
}

test('contract evidence validator accepts scaffold register', () => {
  const result = run(['docs/contracts/runtime-critical-evidence-register.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /runtime-critical-evidence-register\.yaml: ok \(0 records\)/);
});

test('contract evidence validator rejects missing evidence_hash', () => {
  const result = run([path.join(FIXTURES, 'invalid-missing-evidence-hash.yaml')]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /missing fields: evidence_hash/);
});

test('contract evidence validator rejects invalid configuration hash', () => {
  const result = run([path.join(FIXTURES, 'invalid-configuration-hash.yaml')]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /configuration_hashes\[coretools\] must be a lowercase sha256/);
});

test('contract evidence validator rejects negative duplicate_count', () => {
  const result = run([path.join(FIXTURES, 'invalid-negative-duplicate-count.yaml')]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /duplicate_count must be a non-negative integer/);
});
