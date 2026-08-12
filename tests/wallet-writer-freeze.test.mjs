import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const runValidator = (args = []) => spawnSync(
  'python3',
  ['scripts/check-wallet-writer-freeze.py', ...args],
  { cwd: ROOT, encoding: 'utf8' }
);

test('current writer-freeze manifest is structurally valid and readiness blocked', () => {
  const result = runValidator();

  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /STRUCTURE PASS/);
  assert.match(result.stdout, /READINESS BLOCKED/);
});
