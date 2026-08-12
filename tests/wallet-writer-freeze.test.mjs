import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const MANIFEST = path.join(ROOT, 'docs/audits/2026-08-12/wallet-writer-freeze-manifest.yaml');
const canonical = readFileSync(MANIFEST, 'utf8');

const runValidator = (args = []) => spawnSync(
  'python3',
  ['scripts/check-wallet-writer-freeze.py', ...args],
  { cwd: ROOT, encoding: 'utf8' }
);

const runManifest = (contents, args = []) => {
  const directory = mkdtempSync(path.join(os.tmpdir(), 'wallet-writer-freeze-'));
  const manifest = path.join(directory, 'manifest.yaml');
  writeFileSync(manifest, contents);
  try {
    return runValidator(['--manifest', manifest, ...args]);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
};

test('current writer-freeze manifest is structurally valid and readiness blocked', () => {
  const result = runValidator();

  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /STRUCTURE PASS/);
  assert.match(result.stdout, /READINESS BLOCKED/);
});

test('rejects capability coverage drift from the human matrix', () => {
  const altered = canonical.replace(
    /\n  - capability_id: GOLD-CAP-011\n[\s\S]*?(?=\nfreeze_records:)/,
    '\n'
  );
  const result = runManifest(altered);

  assert.equal(result.status, 1);
  assert.match(result.stderr, /capability catalog does not exactly cover matrix IDs/);
});

test('rejects a duplicate backend and capability freeze tuple', () => {
  const duplicate = '  - {backend_id: items, capability_id: GOLD-WRITER-001, plugin_name: CMI, plugin_version: 9.8.4.14, mutation_route: generic_vault_api, effective_exposure: pending, vendor_jar_sha256: null, freeze_status: PENDING, evidence_refs: [], proofs: *pending_proofs}\n';
  const altered = canonical.replace('\nbackend_acknowledgements:', `\n${duplicate}backend_acknowledgements:`);
  const result = runManifest(altered);

  assert.equal(result.status, 1);
  assert.match(result.stderr, /duplicate freeze tuple: items GOLD-WRITER-001/);
});

test('rejects capability matrix hash drift', () => {
  const altered = canonical.replace(
    /sha256: [0-9a-f]{64}/,
    `sha256: ${'f'.repeat(64)}`
  );
  const result = runManifest(altered);

  assert.equal(result.status, 1);
  assert.match(result.stderr, /capability matrix SHA-256 mismatch/);
});
