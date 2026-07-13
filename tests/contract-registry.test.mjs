import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

test('contract registry passes structural and path validation', () => {
  const result = spawnSync('python3', ['scripts/check-contract-registry.py'], {
    cwd: ROOT,
    encoding: 'utf8'
  });

  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /16 contracts validated/);
  assert.match(result.stdout, /4 unresolved blocker/);
});
