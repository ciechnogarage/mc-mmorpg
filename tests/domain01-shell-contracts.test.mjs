import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

function run(args) {
  return spawnSync('python3', ['scripts/check-domain01-shell-contracts.py', ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
}

function runWithTempYaml(contents) {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'domain01-shell-'));
  const tempPath = path.join(tempDir, 'contract.yaml');
  fs.writeFileSync(tempPath, contents, 'utf8');
  return run([tempPath]);
}

test('domain-01 shell checker accepts reward transaction shell', () => {
  const result = run(['docs/contracts/reward-transaction-contract.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /reward-transaction-contract\.yaml: ok/);
});

test('domain-01 shell checker accepts economy currency shell', () => {
  const result = run(['docs/contracts/economy-currency-contract.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /economy-currency-contract\.yaml: ok/);
});

test('domain-01 shell checker accepts reputation standing shell', () => {
  const result = run(['docs/contracts/reputation-standing-contract.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /reputation-standing-contract\.yaml: ok/);
});

test('domain-01 shell checker rejects reputation shell with wrong v1 character namespace', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace(
      'namespace: mcmmorpg:character:mmoprofiles-profile:v1',
      'namespace: mcmmorpg:character:domain01:v1',
    );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /canonical_key\.character_id\.namespace must match the approved v1 MMOProfiles namespace/);
});

test('domain-01 shell checker rejects reputation shell without pdc_keys', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace(
      "pdc_keys:\n  - domain08.reputation.{organization_category}.{organization_id}.standing_tier\n  - domain08.reputation.{organization_category}.{organization_id}.hostility_state\n  - domain08.reputation.last_delta_reference\n\n",
      '',
    );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /pdc_keys must be a non-empty list/);
});

test('domain-01 shell checker rejects reputation shell with wrong identity namespace', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace(
      'character: mcmmorpg:character:mmoprofiles-profile:v1',
      'character: mcmmorpg:character:domain01:v1',
    );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /identity_namespace\.character must match the approved v1 MMOProfiles namespace/);
});

test('domain-01 shell checker rejects reputation shell with broken boundary policy', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace(
      'reputation_is_not_membership: true',
      'reputation_is_not_membership: false',
    );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /api\.boundary_policy\.reputation_is_not_membership must be true/);
});

test('domain-01 shell checker rejects reputation shell without required forbidden placeholder authority', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace('  - menu_projection\n', '');
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /forbidden_placeholder_authorities must include menu_projection/);
});

test('domain-01 shell checker rejects reputation shell with drifted delta reason vocabulary', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace('  - DELIVERY\n', '');
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /state_vocabularies\.delta_reason must match the agreed reputation source set/);
});

test('domain-01 shell checker rejects reputation shell without runtime authority migration blocker', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace('    - REPUTATION-001_domain08_runtime_authority_not_implemented\n', '');
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /migration_requirements\.cutover_blockers must include a runtime authority not implemented blocker/);
});

test('domain-01 shell checker rejects reputation shell without legacy source mapping blocker', () => {
  const contract = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace('    - REPUTATION-002_legacy_reputation_sources_not_mapped_to_business_references\n', '');
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, /migration_requirements\.cutover_blockers must include a legacy reputation sources mapping blocker/);
});

test('domain-01 shell checker rejects reputation shell when referenced account-character namespace drifts', () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'domain01-shell-pair-'));
  const tempAccountPath = path.join(tempDir, 'account-character-contract.yaml');
  const tempReputationPath = path.join(tempDir, 'reputation-standing-contract.yaml');
  const accountCharacter = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/account-character-contract.yaml'), 'utf8')
    .replace(/mcmmorpg:character:mmoprofiles-profile:v1/g, 'mcmmorpg:character:domain01:v1');
  const reputation = fs
    .readFileSync(path.join(ROOT, 'docs/contracts/reputation-standing-contract.yaml'), 'utf8')
    .replace(
      'contract_ref: docs/contracts/account-character-contract.yaml',
      `contract_ref: ${tempAccountPath}`,
    );
  fs.writeFileSync(tempAccountPath, accountCharacter, 'utf8');
  fs.writeFileSync(tempReputationPath, reputation, 'utf8');

  const result = run([tempReputationPath]);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(
    result.stderr,
    /canonical_key\.character_id\.namespace must match .* canonical character namespace/,
  );
});
