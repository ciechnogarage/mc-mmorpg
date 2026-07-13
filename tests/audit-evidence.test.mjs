import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

test('audit evidence register validates recorded external checksum and local evidence', () => {
  const strict = spawnSync('python3', ['scripts/check-audit-evidence.py'], {
    cwd: ROOT,
    encoding: 'utf8'
  });
  assert.equal(strict.status, 0, `${strict.stdout}\n${strict.stderr}`);
  assert.match(strict.stdout, /audit evidence valid/);
});
