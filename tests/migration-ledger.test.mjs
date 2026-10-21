import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const FIXTURES = path.join(ROOT, 'tests', 'fixtures', 'migration-ledger');

function run(args) {
  return spawnSync('python3', ['scripts/check-contract-migration-ledger.py', ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
}

test('migration ledger validator accepts the scaffold ledger', () => {
  const result = run(['docs/contracts/migration/config-authority-registry-migration-ledger.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /config-authority-registry-migration-ledger\.yaml: ok \(0 entries\)/);
});

test('migration ledger validator rejects source registry sha mismatch', () => {
  const result = run([path.join(FIXTURES, 'invalid-source-registry-sha-mismatch.yaml')]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /source_registry\.sha256 must match snapshot source_registry\.sha256/);
});

test('migration ledger validator rejects non-canonical allowed dispositions', () => {
  const result = run([path.join(FIXTURES, 'invalid-allowed-dispositions.yaml')]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /allowed_dispositions must match the published disposition list exactly/);
});

test('migration ledger validator rejects entry digest mismatch vs snapshot', () => {
  const result = run([path.join(FIXTURES, 'invalid-entry-source-digest-mismatch.yaml')]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /source_digest does not match snapshot digest/);
});

test('migration ledger validator rejects mapped entry without target contract metadata', () => {
  const result = run([path.join(FIXTURES, 'invalid-mapped-without-target.yaml')]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /target_contract_id must be a non-empty string/);
});
