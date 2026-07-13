import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

test('audit evidence register validates structure and exposes missing external hash', () => {
  const strict = spawnSync('python3', ['scripts/check-audit-evidence.py'], {
    cwd: ROOT,
    encoding: 'utf8'
  });
  assert.notEqual(strict.status, 0);
  assert.match(`${strict.stdout}\n${strict.stderr}`, /SHA-256/);

  const auditMode = spawnSync('python3', ['scripts/check-audit-evidence.py', '--allow-missing-external-hash'], {
    cwd: ROOT,
    encoding: 'utf8'
  });
  assert.equal(auditMode.status, 0, `${auditMode.stdout}\n${auditMode.stderr}`);
  assert.match(auditMode.stdout, /audit evidence valid/);
  assert.match(auditMode.stdout, /BLOCKED/);
});
