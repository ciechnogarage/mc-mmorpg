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

const readyManifest = (extraPython = '') => {
  const script = `
import sys
import yaml

data = yaml.safe_load(sys.stdin.read())
evidence = "docs/ai/cmi-vault-gold-writer-capability-matrix.md"
catalog = {row["capability_id"]: row for row in data["capabilities"]}
data["readiness"] = "READY"
for record in data["freeze_records"]:
    capability = catalog[record["capability_id"]]
    required = capability["required_cutover_state"]
    if capability["binary_origin"] == "first_party" and "migrated_first_party" in required:
        record["effective_exposure"] = "migrated_first_party"
        record["target_mutation_protocol"] = "WalletMutationPort"
    elif "not_gold_proven" in required:
        record["effective_exposure"] = "not_gold_proven"
    else:
        record["effective_exposure"] = "disabled"
    record["freeze_status"] = "VERIFIED"
    record["evidence_refs"] = [evidence]
    if capability["binary_origin"] == "vendor":
        record["vendor_jar_sha256"] = "a" * 64
    for proof in record["proofs"].values():
        proof["status"] = "VERIFIED"
        proof["evidence_ref"] = evidence
for acknowledgement in data["backend_acknowledgements"]:
    acknowledgement.update({
        "status": "VERIFIED",
        "selected_provider": "MCMMORPGCentralWalletVaultProvider",
        "provider_contract_version": 2,
        "write_mode": "context_required",
        "generic_write_failure_code": "WRITE_CONTEXT_REQUIRED",
        "cutover_epoch": "cutover-2026-08-12-001",
        "evidence_ref": evidence,
    })
data["legacy_snapshot"].update({
    "status": "VERIFIED",
    "account_count": 0,
    "balance_checksum_sha256": "b" * 64,
    "snapshot_watermark": "legacy-freeze-2026-08-12-001",
    "evidence_ref": evidence,
})
${extraPython}
sys.stdout.write(yaml.safe_dump(data, sort_keys=False))
`;
  const result = spawnSync('python3', ['-c', script], {
    cwd: ROOT,
    encoding: 'utf8',
    input: canonical
  });
  assert.equal(result.status, 0, result.stderr);
  return result.stdout;
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

test('strict mode rejects the current blocked snapshot with stable blocker codes', () => {
  const result = runValidator(['--require-ready']);

  assert.equal(result.status, 1);
  assert.match(result.stderr, /READINESS BLOCKED/);
  assert.match(result.stderr, /MISSING_VENDOR_JAR_PROOF/);
  assert.match(result.stderr, /PROVIDER_ACK_NOT_VERIFIED/);
});

test('strict mode accepts a coherent synthetic ready snapshot', () => {
  const result = runManifest(readyManifest(), ['--require-ready']);

  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /STRUCTURE PASS/);
  assert.match(result.stdout, /READINESS READY/);
});

test('strict mode forbids permission-only freeze for an automatic writer', () => {
  const manifest = readyManifest(`
data["freeze_records"][0]["effective_exposure"] = "permission_denied"
`);
  const result = runManifest(manifest, ['--require-ready']);

  assert.equal(result.status, 1);
  assert.match(result.stderr, /PERMISSION_ONLY_FREEZE_FORBIDDEN/);
});
