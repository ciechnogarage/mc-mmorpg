import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const ACCOUNT_CHARACTER_PATH = path.join(ROOT, 'docs/contracts/account-character-contract.yaml');

function run(args) {
  return spawnSync('python3', ['scripts/check-contracts-v2.py', ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
}

function runWithTempYaml(contents) {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'contracts-v2-'));
  const tempPath = path.join(tempDir, 'contract.yaml');
  fs.writeFileSync(tempPath, contents, 'utf8');
  return run([tempPath]);
}

function readAccountCharacter() {
  return fs.readFileSync(ACCOUNT_CHARACTER_PATH, 'utf8');
}

function replaceOrThrow(contents, searchValue, replaceValue) {
  const next = contents.replace(searchValue, replaceValue);
  assert.notEqual(next, contents, `mutation did not apply: ${String(searchValue)}`);
  return next;
}

test('contracts v2 validator accepts account-character contract', () => {
  const result = run(['docs/contracts/account-character-contract.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /account-character-contract\.yaml: ok/);
});

test('contracts v2 validator rejects wrong schema_version', () => {
  const contract = replaceOrThrow(readAccountCharacter(), 'schema_version: 2', 'schema_version: 3');
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /schema_version must be 2/);
});

test('contracts v2 validator rejects list-shaped contract_owner', () => {
  const contract = replaceOrThrow(readAccountCharacter(), 'contract_owner: "02"', 'contract_owner:\n  - "02"');
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /contract_owner.*non-empty string/);
});

test('contracts v2 validator rejects identity namespace mismatch', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '  profile: mcmmorpg:mmoprofiles:profile:v1\n',
    '  profile: mcmmorpg:mmoprofiles:profile:wrong\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /identity_namespace\.profile must match profile namespace/);
});

test('contracts v2 validator rejects non-forbidden silent key rewrite', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    silent_key_rewrite: forbidden\n',
    '    silent_key_rewrite: allowed\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /silent_key_rewrite must be forbidden/);
});

test('contracts v2 validator rejects missing account source principal in api section', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    source_principal: velocity_forwarded_profile\n',
    '',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /source_principal.*non-empty string/);
});

test('contracts v2 validator rejects account issuer drift from approved dev-offline plan', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    issuer: mcmmorpg-dev-offline\n',
    '    issuer: wrong-issuer\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /canonical_key\.account_id\.issuer must be mcmmorpg-dev-offline/);
});

test('contracts v2 validator rejects account namespace drift from approved dev-offline plan', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    namespace: mcmmorpg:account:dev-offline:v1\n',
    '    namespace: mcmmorpg:account:wrong:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /canonical_key\.account_id\.namespace must be mcmmorpg:account:dev-offline:v1/);
});

test('contracts v2 validator rejects account source principal drift from approved dev-offline plan', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    source_principal: velocity_forwarded_profile\n',
    '    source_principal: wrong_source\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /canonical_key\.account_id\.source_principal must be velocity_forwarded_profile/);
});

test('contracts v2 validator rejects account assurance drift from approved dev-offline plan', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    assurance_level: dev_offline_unverified\n',
    '    assurance_level: wrong_assurance\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /canonical_key\.account_id\.assurance_level must be dev_offline_unverified/);
});

test('contracts v2 validator rejects production online namespace drift in account api policy', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(account_identity_policy:\n(?:[^\n]*\n){0,6}?\s+)production_online_namespace: mcmmorpg:account:online:v1\n/,
    '$1production_online_namespace: mcmmorpg:account:wrong-online:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /account_identity_policy\.production_online_namespace.*canonical_key\.account_id\.production_online_namespace/);
});

test('contracts v2 validator rejects offline-to-online policy drift from approved plan', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    offline_to_online_policy: explicit_mapping_required\n',
    '    offline_to_online_policy: remap_account_id_only_via_domain01\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /offline_to_online_policy must be explicit_mapping_required/);
});

test('contracts v2 validator rejects missing player_uuid forbidden source', () => {
  const contract = replaceOrThrow(readAccountCharacter(), '      - player_uuid\n', '');
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /forbidden_sources must include player_uuid/);
});

test('contracts v2 validator rejects character identity source drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    source: MMOProfiles.getCurrent().getUniqueId()\n',
    '    source: wrong-character-source\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /canonical_key\.character_id\.source must be MMOProfiles\.getCurrent\(\)\.getUniqueId\(\)/);
});

test('contracts v2 validator rejects profile identity source drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    source: MMOProfiles.profileId\n',
    '    source: wrong-profile-source\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /canonical_key\.profile_id\.source must be MMOProfiles\.profileId/);
});

test('contracts v2 validator rejects mismatched profile mapping record name', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '  mmoprofiles_profile_mapping_record:\n    persistence_boundary: domain02_mmoprofiles_profile_mapping_store\n',
    '  profile_mapping_record:\n    persistence_boundary: domain02_mmoprofiles_profile_mapping_store\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /store_records\.mmoprofiles_profile_mapping_record must be a mapping/);
});

test('contracts v2 validator rejects profile mapping persistence boundary drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '  mmoprofiles_profile_mapping_record:\n    persistence_boundary: domain02_mmoprofiles_profile_mapping_store\n',
    '  mmoprofiles_profile_mapping_record:\n    persistence_boundary: wrong_profile_mapping_store\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(
    result.stderr,
    /store_records\.mmoprofiles_profile_mapping_record\.persistence_boundary must match persistence_owner\.mmoprofiles_profile_mapping_record/,
  );
});

test('contracts v2 validator rejects profile mapping namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(\s+profile_id:\n\s+type: string\n)\s+namespace: mcmmorpg:mmoprofiles:profile:v1\n(\s+technical_key_only: true\n)/,
    '$1        namespace: mcmmorpg:mmoprofiles:profile:wrong\n$2',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(
    result.stderr,
    /store_records\.mmoprofiles_profile_mapping_record\.fields\.profile_id\.namespace must match canonical_key\.profile_id\.namespace/,
  );
});

test('contracts v2 validator rejects profile mapping character namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(mmoprofiles_profile_mapping_record:\n(?:[^\n]*\n){0,20}?\s+character_id:\n\s+type: string\n)\s+namespace: mcmmorpg:character:mmoprofiles-profile:v1\n/,
    '$1        namespace: mcmmorpg:character:wrong:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(
    result.stderr,
    /store_records\.mmoprofiles_profile_mapping_record\.fields\.character_id\.namespace must match canonical_key\.character_id\.namespace/,
  );
});

test('contracts v2 validator rejects profile mapping account namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(mmoprofiles_profile_mapping_record:\n(?:[^\n]*\n){0,20}?\s+account_id:\n\s+type: string\n)\s+namespace: mcmmorpg:account:dev-offline:v1\n/,
    '$1        namespace: mcmmorpg:account:wrong:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(
    result.stderr,
    /store_records\.mmoprofiles_profile_mapping_record\.fields\.account_id\.namespace must match canonical_key\.account_id\.namespace/,
  );
});

test('contracts v2 validator rejects account record account namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(account_record:\n(?:[^\n]*\n){0,10}?\s+account_id:\n\s+type: string\n)\s+namespace: mcmmorpg:account:dev-offline:v1\n/,
    '$1        namespace: mcmmorpg:account:online:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /store_records\.account_record\.fields\.account_id\.namespace must match canonical_key\.account_id\.namespace/);
});

test('contracts v2 validator rejects account record source principal field drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '      velocity_forwarded_profile:\n        type: string\n        source: Domain01AccountService\n',
    '      minecraft_uuid:\n        type: string\n        source: Domain01AccountService\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /store_records\.account_record\.fields\.velocity_forwarded_profile must be a mapping/);
});

test('contracts v2 validator rejects character record character namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(character_record:\n(?:[^\n]*\n){0,10}?\s+character_id:\n\s+type: string\n)\s+namespace: mcmmorpg:character:mmoprofiles-profile:v1\n/,
    '$1        namespace: mcmmorpg:character:wrong:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /store_records\.character_record\.fields\.character_id\.namespace must match canonical_key\.character_id\.namespace/);
});

test('contracts v2 validator rejects character record account namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(character_record:\n(?:[^\n]*\n){0,20}?\s+account_id:\n\s+type: string\n)\s+namespace: mcmmorpg:account:dev-offline:v1\n/,
    '$1        namespace: mcmmorpg:account:wrong:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /store_records\.character_record\.fields\.account_id\.namespace must match canonical_key\.account_id\.namespace/);
});

test('contracts v2 validator rejects active session account namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(active_session_record:\n(?:[^\n]*\n){0,10}?\s+account_id:\n\s+type: string\n)\s+namespace: mcmmorpg:account:dev-offline:v1\n/,
    '$1        namespace: mcmmorpg:account:wrong:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /store_records\.active_session_record\.fields\.account_id\.namespace must match canonical_key\.account_id\.namespace/);
});

test('contracts v2 validator rejects active session character namespace drift', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    /(active_session_record:\n(?:[^\n]*\n){0,15}?\s+character_id:\n\s+type: string\n)\s+namespace: mcmmorpg:character:mmoprofiles-profile:v1\n/,
    '$1        namespace: mcmmorpg:character:wrong:v1\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /store_records\.active_session_record\.fields\.character_id\.namespace must match canonical_key\.character_id\.namespace/);
});

test('contracts v2 validator rejects stale session vocabulary drift between api and store record', () => {
  const contract = replaceOrThrow(
    readAccountCharacter(),
    '    stale_session_epoch_policy: fail_closed_and_require_reactivation\n',
    '    stale_session_epoch_policy: fail_closed_and_require_fresh_activation\n',
  );
  const result = runWithTempYaml(contract);
  assert.notEqual(result.status, 0);
  assert.match(
    result.stderr,
    /activation_session_policy\.stale_session_epoch_policy must be fail_closed_and_require_reactivation/,
  );
});
