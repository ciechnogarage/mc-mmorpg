import test from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const CONTRACT = path.join(ROOT, 'docs', 'design', 'classes', 'domain-04-validation-contract.yaml');
const SKILL_CONTRACT = path.join(ROOT, 'docs', 'design', 'classes', 'skill-library-contract.yaml');
const SCRIPT = 'scripts/check-domain-04-validation-contract.py';

function run(args) {
  return spawnSync('python3', [SCRIPT, ...args], { cwd: ROOT, encoding: 'utf8' });
}

/**
 * Writes a mutated copy of the real contract to a temp file and validates it.
 * Mutating the accepted document keeps the negative fixtures honest: they only
 * differ from the real contract by the single defect under test.
 */
function runMutated(name, mutate) {
  const source = readFileSync(CONTRACT, 'utf8');
  const mutated = mutate(source);
  assert.notEqual(mutated, source, `mutation ${name} did not change the contract`);
  const dir = mkdtempSync(path.join(tmpdir(), 'd04-validation-'));
  const target = path.join(dir, 'domain-04-validation-contract.yaml');
  writeFileSync(target, mutated);
  return run([target]);
}

/**
 * Runs the real validation contract against one adversarial skill-contract
 * fixture. The second checker argument exists only to keep negative fixtures
 * isolated from the accepted source document.
 */
function runSkillMutated(name, mutate) {
  const source = readFileSync(SKILL_CONTRACT, 'utf8');
  const mutated = mutate(source);
  assert.notEqual(mutated, source, `mutation ${name} did not change the skill contract`);
  const dir = mkdtempSync(path.join(tmpdir(), 'd04-skill-contract-'));
  const target = path.join(dir, 'skill-library-contract.yaml');
  writeFileSync(target, mutated);
  return run([CONTRACT, target]);
}

test('domain 04 validator accepts the validation contract', () => {
  const result = run(['docs/design/classes/domain-04-validation-contract.yaml']);
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /invariants validated/);
  assert.match(result.stdout, /runtime evidence gates/);
  assert.match(result.stdout, /domain-04-validation-contract\.yaml: ok/);
});

test('static enforcement may not grant runtime_verified', () => {
  const result = runMutated('static-grants-runtime', (source) =>
    source.replace(/max_status_granted: validated/, 'max_status_granted: runtime_verified'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /static parsing must never grant runtime_verified/);
});

test('slot model mirror must match combat-loadout.md', () => {
  const result = runMutated('slot-drift', (source) =>
    source.replace(/active_skill: 4/, 'active_skill: 5'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /loadout_slot_model\.slots drifted from combat-loadout\.md/);
});

test('invariant coverage must match the source validation requirements', () => {
  const result = runMutated('coverage-drift', (source) =>
    source.replace(
      /covers_source_requirement: combat lockout blocks all loadout edits/,
      'covers_source_requirement: combat lockout blocks some loadout edits',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /group loadout covers_source_requirement drifted/);
});

test('runtime evidence gates must cover every required topic', () => {
  const result = runMutated('missing-topic', (source) =>
    source.replace(/topic: party/, 'topic: party_lobby'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /runtime evidence gates must cover topics/);
  assert.match(result.stderr, /missing \['party'\]/);
});

test('a runtime evidence gate may not cover a statically enforced invariant', () => {
  const result = runMutated('gate-covers-static', (source) =>
    source.replace(/- D04-INV-LOADOUT-005/, '- D04-INV-RACE-001'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /may only cover runtime or hybrid invariants/);
});

test('runtime evidence gates require an allocated dev lease', () => {
  const result = runMutated('lease-not-required', (source) =>
    source.replace(/lease_required: true/, 'lease_required: false'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /lease_required must be true/);
});

test('cross-domain invariants must cover Domains 02, 03, 05 and 14', () => {
  const result = runMutated('missing-cross-domain', (source) =>
    source.replace(/- '14'/g, "- '15'"),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /cross_domain invariants must cover Domains/);
  assert.match(result.stderr, /missing \['14'\]/);
});

test('this contract may not open CLS-* class work', () => {
  const result = runMutated('cls-gate-opened', (source) =>
    source.replace(
      'concrete_class_definitions_in_scope: false',
      'concrete_class_definitions_in_scope: true',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /must not open CLS-\* work/);
});

test('Domain 03 consumer binding gate is mandatory', () => {
  const result = runMutated('missing-d03-consumer-binding-gate', (source) =>
    source.replace(/topic: d03_consumer_binding/, 'topic: d03_binding_removed'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /runtime evidence gates must cover topics/);
  assert.match(result.stderr, /d03_consumer_binding/);
});

test('consumer binding schema cannot be removed even when release-gate prose remains', () => {
  const result = runSkillMutated('binding-schema-removed', (source) =>
    source
      .replace('    - d03_consumer_bindings\n', '')
      .replace(/\n    d03_consumer_bindings:\n[\s\S]*?\n    runtime:/, '\n    runtime:'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /skill_record_schema\.required_fields must be exactly/);
});

test('consumer binding item schema requires all seven typed lineage fields', () => {
  const result = runSkillMutated('binding-build-digest-weakened', (source) =>
    source.replace('        build_digest: sha256', '        build_digest: string'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /skill_record_schema\.fields\.d03_consumer_bindings must be exactly/);
});

test('consumer binding coverage must be exact over the declared reachable mechanics', () => {
  const result = runSkillMutated('binding-coverage-weakened', (source) =>
    source.replace(
      '  coverage_mode: EXACTLY_ONE_PER_REACHABLE_MECHANIC',
      '  coverage_mode: AT_LEAST_ONE_IF_AVAILABLE',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /consumer_binding_policy must be exactly/);
});

test('consumer binding policy pins authority registry, source contract and exact scope', () => {
  const result = runSkillMutated('authority-registry-pin-weakened', (source) =>
    source.replace(
      /authority_registry_ref: docs\/contracts\/domain-03-combat-mechanic-authority-registry\.yaml@[0-9a-f]{64}/,
      'authority_registry_ref: docs/contracts/domain-03-combat-mechanic-authority-registry.yaml@' + '0'.repeat(64),
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /consumer_binding_policy must be exactly/);
});

test('planning records with empty bindings must keep the provider disabled', () => {
  const result = runSkillMutated('planning-provider-enabled', (source) =>
    source.replace(
      '    implementation_provider: none\n    implementation_ref: null',
      '    implementation_provider: custom\n    implementation_ref: custom:unbound',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /consumer_binding_policy must be exactly/);
});

test('executable lifecycle cannot admit an empty Domain 03 binding set', () => {
  const result = runSkillMutated('executable-empty-bindings', (source) =>
    source.replace('    empty_bindings_allowed: false', '    empty_bindings_allowed: true'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /consumer_binding_policy must be exactly/);
});

test('each structured consumer-binding failure keeps its canonical reason code', () => {
  const result = runSkillMutated('missing-reason-code-weakened', (source) =>
    source.replace('      reason_code: D03_BINDING_MISSING', '      reason_code: D03_BINDING_WARNING'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /consumer_binding_policy must be exactly/);
});

test('consumer-binding failures cannot raise the lifecycle ceiling', () => {
  const result = runSkillMutated('failure-lifecycle-ceiling-raised', (source) =>
    source.replace('      lifecycle_ceiling: validated', '      lifecycle_ceiling: runtime_verified'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /consumer_binding_policy must be exactly/);
});

test('consumer-binding fallback sources remain a closed forbidden set', () => {
  const result = runSkillMutated('provider-fallback-allowed', (source) =>
    source.replace('    - provider_skill_id\n  failure_precedence:', '  failure_precedence:'),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /consumer_binding_policy must be exactly/);
});

for (const [name, mutate] of [
  [
    'policy-schema-version-bool',
    (source) => source.replace(
      'consumer_binding_policy:\n  schema_version: 1',
      'consumer_binding_policy:\n  schema_version: true',
    ),
  ],
  [
    'policy-schema-version-string',
    (source) => source.replace(
      'consumer_binding_policy:\n  schema_version: 1',
      'consumer_binding_policy:\n  schema_version: "1"',
    ),
  ],
  [
    'policy-schema-version-null',
    (source) => source.replace(
      'consumer_binding_policy:\n  schema_version: 1',
      'consumer_binding_policy:\n  schema_version: null',
    ),
  ],
  [
    'planning-empty-bindings-int',
    (source) => source.replace('    empty_bindings_allowed: true', '    empty_bindings_allowed: 1'),
  ],
  [
    'planning-empty-bindings-string',
    (source) => source.replace('    empty_bindings_allowed: true', '    empty_bindings_allowed: "true"'),
  ],
  [
    'planning-empty-bindings-null',
    (source) => source.replace('    empty_bindings_allowed: true', '    empty_bindings_allowed: null'),
  ],
  [
    'executable-empty-bindings-int',
    (source) => source.replace('    empty_bindings_allowed: false', '    empty_bindings_allowed: 0'),
  ],
  [
    'all-mechanics-resolve-int',
    (source) => source.replace('    all_reachable_mechanics_must_resolve: true', '    all_reachable_mechanics_must_resolve: 1'),
  ],
  [
    'policy-extra-fail-open-key',
    (source) => source.replace('consumer_binding_policy:\n', 'consumer_binding_policy:\n  fail_open_override: true\n'),
  ],
  [
    'policy-omitted-precedence',
    (source) => source.replace(/  failure_precedence:\n(?:    - [a-z_]+\n)+/, ''),
  ],
]) {
  test(`consumer binding policy rejects strict-type or closed-key violation: ${name}`, () => {
    const result = runSkillMutated(name, mutate);
    assert.equal(result.status, 1);
    assert.match(result.stderr, /consumer_binding_policy must be exactly/);
  });
}

test('consumer binding schema rejects extra keys', () => {
  const result = runSkillMutated('binding-schema-extra-key', (source) =>
    source.replace(
      '    d03_consumer_bindings:\n      type:',
      '    d03_consumer_bindings:\n      additional_item_fields_allowed: true\n      type:',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /skill_record_schema\.fields\.d03_consumer_bindings must be exactly/);
});

test('parent skill record schema and fields mappings are closed', () => {
  const parentExtra = runSkillMutated('parent-skill-schema-extra-key', (source) =>
    source.replace(
      'skill_record_schema:\n  required_fields:',
      'skill_record_schema:\n  additional_fields_allowed: true\n  required_fields:',
    ),
  );
  assert.equal(parentExtra.status, 1);
  assert.match(parentExtra.stderr, /skill_record_schema keys must be exactly/);

  const fieldsExtra = runSkillMutated('parent-skill-fields-extra-key', (source) =>
    source.replace(
      '  fields:\n    skill_id:',
      '  fields:\n    fail_open_override: boolean\n    skill_id:',
    ),
  );
  assert.equal(fieldsExtra.status, 1);
  assert.match(fieldsExtra.stderr, /skill_record_schema\.fields keys must be exactly/);
});

test('consumer binding schema rejects omitted or contradictory rules', () => {
  const contradictory = runSkillMutated('binding-rule-contradiction', (source) =>
    source.replace(
      '        - missing, duplicate, stale, digest-mismatched, downgraded, or revoked binding suppresses or rejects that mechanic fail-closed',
      '        - missing binding may use a local provider fallback',
    ),
  );
  assert.equal(contradictory.status, 1);
  assert.match(contradictory.stderr, /skill_record_schema\.fields\.d03_consumer_bindings must be exactly/);

  const omitted = runSkillMutated('binding-rule-omitted', (source) =>
    source.replace(
      '        - every catalogued combat mechanic executable by the skill has exactly one binding for its mechanic_id\n',
      '',
    ),
  );
  assert.equal(omitted.status, 1);
  assert.match(omitted.stderr, /skill_record_schema\.fields\.d03_consumer_bindings must be exactly/);
});

for (const [name, field] of [
  ['gate-empty-bindings-int', 'executable_empty_bindings_allowed'],
  ['gate-provider-none-int', 'executable_implementation_provider_none_allowed'],
]) {
  test(`runtime evidence gate rejects boolean/integer coercion: ${name}`, () => {
    const result = runMutated(name, (source) =>
      source.replace(`      ${field}: false`, `      ${field}: 0`),
    );
    assert.equal(result.status, 1);
    assert.match(result.stderr, /D04-RG-D03-CONSUMER-BINDING\.evidence_contract must be exactly/);
  });
}

test('consumer binding runtime gate rejects placeholder evidence prose', () => {
  const result = runMutated('binding-placeholder-evidence', (source) =>
    source.replace(
      /  proves: >\n    an executable skill[\s\S]*?provider fallback\n  evidence_contract:/,
      '  proves: placeholder evidence only\n  evidence_contract:',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /D04-RG-D03-CONSUMER-BINDING\.proves must state the complete/);
});

test('the complete Domain 03 consumer-binding runtime gate mapping is closed', () => {
  const result = runMutated('binding-gate-extra-key', (source) =>
    source.replace(
      '- id: D04-RG-D03-CONSUMER-BINDING\n  topic:',
      '- id: D04-RG-D03-CONSUMER-BINDING\n  fail_open_override: true\n  topic:',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /D04-RG-D03-CONSUMER-BINDING keys must be exactly/);
});

test('record validator contract cannot weaken authority content verification', () => {
  const result = runMutated('authority-content-verification-weakened', (source) =>
    source.replace(
      'authority_content_verification: registry_snapshot_and_contract_sha256',
      'authority_content_verification: caller_assertion',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /record_validator_contract must be exactly/);
});

test('consumer binding runtime gate requires structured negative outcomes', () => {
  const result = runMutated('binding-negative-outcome-weakened', (source) =>
    source.replace(
      '      missing: D03_BINDING_MISSING',
      '      missing: D03_BINDING_WARNING',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /D04-RG-D03-CONSUMER-BINDING\.evidence_contract must be exactly/);
});

test('consumer binding runtime gate preserves planning provider constraints', () => {
  const result = runMutated('binding-gate-planning-provider-enabled', (source) =>
    source.replace(
      '      planning_implementation_provider: none',
      '      planning_implementation_provider: custom',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /D04-RG-D03-CONSUMER-BINDING\.evidence_contract must be exactly/);
});

test('consumer binding failure policy may not become fail-open', () => {
  const result = runMutated('binding-fail-open', (source) =>
    source.replace(
      /covers_source_requirement: a missing, stale, mismatched, downgraded, or revoked binding blocks executable mapping and lifecycle above validated without fallback/,
      'covers_source_requirement: a missing binding may use provider fallback',
    ),
  );
  assert.equal(result.status, 1);
  assert.match(result.stderr, /group skill covers_source_requirement drifted/);
});
