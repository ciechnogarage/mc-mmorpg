import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const SCRIPT = 'scripts/check-domain-04-skill-record.py';
const MECHANIC_A = 'combat:direct_damage';
const MECHANIC_B = 'combat:periodic_damage';
const DIGEST_A = 'a'.repeat(64);
const DIGEST_B = 'b'.repeat(64);
const READY_A_REF = 'docs/contracts/domain-03-authority-snapshots/ready-direct-damage.yaml@ff5b5762bd670abde7d6591bc5ca5440292099943ab8cfc36b851998526dc5ee';
const READY_AB_REF = 'docs/contracts/domain-03-authority-snapshots/ready-direct-periodic-damage.yaml@4bcd616b7a42e7b2ab34266f00434c4deb3b330eeac261d5c670868118441f52';
const DOWNGRADED_A_REF = 'docs/contracts/domain-03-authority-snapshots/downgraded-direct-damage.yaml@a3cde3e867216b4e798e3927b87034fec513f401bf34ca8f1c0707de361190e7';
const REVOKED_A_REF = 'docs/contracts/domain-03-authority-snapshots/revoked-direct-damage.yaml@532e73473ba73e4c7fbdc64d00d61ea2a7393314977c9f54c9ce213d620eb5f4';
const FAIL_CLOSED = {
  admission: 'REJECT_MECHANIC',
  lifecycle_ceiling: 'validated',
  executable_provider: 'DISABLED',
  fallback: 'FORBIDDEN',
};

function binding(mechanicId = MECHANIC_A) {
  return {
    mechanic_id: mechanicId,
    mechanic_contract_version: 1,
    required_readiness: 'RUNTIME_VERIFIED',
    mechanic_contract_ref: `contracts/${mechanicId}@${DIGEST_A}`,
    gate_transition_envelope_ref: `evidence/${mechanicId}@${DIGEST_A}`,
    contract_bundle_digest: DIGEST_A,
    build_digest: DIGEST_A,
  };
}

function request({
  status = 'runtime_verified',
  provider = 'custom',
  reachable = [MECHANIC_A],
  bindings = [binding()],
  snapshotRef = READY_A_REF,
  fallbackAttempts = [],
} = {}) {
  const executable = status === 'runtime_verified' || status === 'released';
  return {
    schema_version: 1,
    request_kind: 'domain_04_skill_record_validation',
    skill_record: {
      skill_id: 'class:review_fixture',
      schema_version: 1,
      ownership: { source_type: 'class', source_id: 'class:review' },
      classification: {
        activation_type: 'active',
        ability_tags: [],
        mechanic_tags: [],
        catalogued_mechanic_ids: reachable,
        damage_family_id: null,
        damage_school_id: null,
        effect_ids: [],
      },
      targeting: { target_type: 'unit', range_profile_id: null, target_cap: 1 },
      requirements: {
        character_level_requirement_id: null,
        class_ids: [],
        subclass_ids: [],
        weapon_school_ids: [],
        state_requirement_ids: [],
        resource_requirement_ids: [],
      },
      execution: {
        cast_time_rule_id: null,
        channel_time_rule_id: null,
        global_cooldown_rule_id: 'cooldown:global',
        cooldown_rule_id: 'cooldown:fixture',
        charge_rule_id: null,
        resource_costs: [],
        snapshot_rule: 'cast',
      },
      scaling: { formula_id: 'formula:fixture', coefficient_set_id: 'coefficients:fixture' },
      control: {
        interruptible: true,
        cancellable: true,
        movement_rule: 'allowed',
        cc_category_id: null,
      },
      d03_consumer_bindings: bindings,
      runtime: {
        implementation_provider: provider,
        implementation_ref: executable && provider !== 'none' ? `runtime:${provider}:fixture` : null,
        provider_skill_id: executable && provider !== 'none' ? `${provider}:fixture` : null,
      },
      lifecycle: { status, validation_refs: [] },
    },
    d03_authority_snapshot_ref: snapshotRef,
    fallback_attempted_mechanic_ids: fallbackAttempts,
  };
}

function runRequest(name, value, contractPath = null) {
  const directory = mkdtempSync(path.join(tmpdir(), 'd04-skill-record-'));
  const target = path.join(directory, `${name}.json`);
  writeFileSync(target, JSON.stringify(value));
  const args = contractPath ? [SCRIPT, target, contractPath] : [SCRIPT, target];
  const result = spawnSync('python3', args, { cwd: ROOT, encoding: 'utf8' });
  let output;
  try {
    const lines = result.stdout.trim().split('\n').filter(Boolean);
    assert.equal(lines.length, 1, `validator must emit exactly one JSON object: ${result.stdout}`);
    output = JSON.parse(lines[0]);
  } catch (error) {
    assert.fail(`validator did not emit JSON: ${error.message}\nstdout=${result.stdout}\nstderr=${result.stderr}`);
  }
  return { ...result, output };
}

function runRawRequest(name, rawRequest, contractPath = null) {
  const directory = mkdtempSync(path.join(tmpdir(), 'd04-skill-record-raw-'));
  const target = path.join(directory, `${name}.yaml`);
  writeFileSync(target, rawRequest);
  const args = contractPath ? [SCRIPT, target, contractPath] : [SCRIPT, target];
  const result = spawnSync('python3', args, { cwd: ROOT, encoding: 'utf8' });
  const lines = result.stdout.trim().split('\n').filter(Boolean);
  assert.equal(lines.length, 1, `validator must emit exactly one JSON object: ${result.stdout}`);
  assert.doesNotMatch(result.stderr, /Traceback/);
  return { ...result, output: JSON.parse(lines[0]) };
}

function assertRejected(result, reasonCode) {
  assert.equal(result.status, 1, `${result.stdout}\n${result.stderr}`);
  assert.equal(result.output.accepted, false);
  assert.equal(result.output.reason_code, reasonCode);
  for (const [key, value] of Object.entries(FAIL_CLOSED)) {
    assert.equal(result.output[key], value, `${reasonCode}.${key}`);
  }
}

test('record validator executes the full 4x4 lifecycle/provider matrix', () => {
  const statuses = ['draft', 'validated', 'runtime_verified', 'released'];
  const providers = ['mmocore', 'mythicmobs', 'custom', 'none'];
  let accepted = 0;
  let rejected = 0;

  for (const status of statuses) {
    for (const provider of providers) {
      const planning = status === 'draft' || status === 'validated';
      const shouldAccept = planning ? provider === 'none' : provider !== 'none';
      const value = request({
        status,
        provider,
        reachable: planning ? [] : [MECHANIC_A],
        bindings: planning ? [] : [binding()],
        snapshotRef: planning ? null : READY_A_REF,
      });
      const result = runRequest(`matrix-${status}-${provider}`, value);
      if (shouldAccept) {
        assert.equal(result.status, 0, `${status}/${provider}: ${result.stdout}\n${result.stderr}`);
        assert.equal(result.output.reason_code, 'D04_SKILL_RECORD_ACCEPTED');
        accepted += 1;
      } else {
        assertRejected(result, 'D04_SKILL_LIFECYCLE_PROVIDER_INVALID');
        rejected += 1;
      }
    }
  }

  assert.equal(accepted, 8);
  assert.equal(rejected, 8);
});

test('lifecycle/provider validation also fences implementation refs', () => {
  const cases = [];
  const planningImplementationRef = request({ status: 'validated', provider: 'none', reachable: [], bindings: [], snapshotRef: null });
  planningImplementationRef.skill_record.runtime.implementation_ref = 'runtime:forbidden';
  cases.push(['planning-implementation-ref', planningImplementationRef]);
  const planningProviderSkillId = request({ status: 'draft', provider: 'none', reachable: [], bindings: [], snapshotRef: null });
  planningProviderSkillId.skill_record.runtime.provider_skill_id = 'provider:forbidden';
  cases.push(['planning-provider-skill-id', planningProviderSkillId]);
  const executableImplementationRef = request();
  executableImplementationRef.skill_record.runtime.implementation_ref = null;
  cases.push(['executable-missing-implementation-ref', executableImplementationRef]);
  const executableProviderSkillId = request();
  executableProviderSkillId.skill_record.runtime.provider_skill_id = null;
  cases.push(['executable-missing-provider-skill-id', executableProviderSkillId]);

  for (const [name, value] of cases) {
    assertRejected(runRequest(name, value), 'D04_SKILL_LIFECYCLE_PROVIDER_INVALID');
  }
});

test('record validator executes 0/1/2/extra/partial binding cardinality', () => {
  const cases = [
    ['zero', request({ bindings: [] }), 1, 'D03_BINDING_MISSING'],
    ['exact-one', request(), 0, 'D04_SKILL_RECORD_ACCEPTED'],
    ['duplicate', request({ bindings: [binding(), binding()] }), 1, 'D03_BINDING_DUPLICATE'],
    [
      'extra',
      request({ bindings: [binding(), binding(MECHANIC_B)] }),
      1,
      'D04_BINDING_COVERAGE_EXTRA',
    ],
    [
      'partial',
      request({
        reachable: [MECHANIC_A, MECHANIC_B],
        bindings: [binding()],
        snapshotRef: READY_AB_REF,
      }),
      1,
      'D03_BINDING_MISSING',
    ],
  ];

  for (const [name, value, status, reasonCode] of cases) {
    const result = runRequest(`cardinality-${name}`, value);
    assert.equal(result.status, status, `${name}: ${result.stdout}\n${result.stderr}`);
    assert.equal(result.output.reason_code, reasonCode, name);
  }
});

test('all seven consumer-binding failures return their exact structured outcome', () => {
  const stale = binding();
  stale.mechanic_contract_version = 2;
  const digestMismatch = binding();
  digestMismatch.contract_bundle_digest = DIGEST_B;

  const cases = [
    ['missing', request({ bindings: [] }), 'D03_BINDING_MISSING'],
    ['duplicate', request({ bindings: [binding(), binding()] }), 'D03_BINDING_DUPLICATE'],
    ['stale', request({ bindings: [stale] }), 'D03_BINDING_STALE'],
    ['digest-mismatch', request({ bindings: [digestMismatch] }), 'D03_BINDING_DIGEST_MISMATCH'],
    ['downgraded', request({ snapshotRef: DOWNGRADED_A_REF }), 'D03_BINDING_READINESS_DOWNGRADED'],
    ['revoked', request({ snapshotRef: REVOKED_A_REF }), 'D03_BINDING_REVOKED'],
    ['fallback', request({ fallbackAttempts: [MECHANIC_A] }), 'D03_BINDING_FALLBACK_FORBIDDEN'],
  ];

  for (const [name, value, reasonCode] of cases) {
    assertRejected(runRequest(`failure-${name}`, value), reasonCode);
  }
});

test('simultaneous binding failures follow the closed precedence', () => {
  const fallbackBeforeDuplicate = request({
    reachable: [MECHANIC_A, MECHANIC_B],
    bindings: [binding(), binding()],
    snapshotRef: READY_AB_REF,
    fallbackAttempts: [MECHANIC_A],
  });
  assertRejected(
    runRequest('precedence-fallback', fallbackBeforeDuplicate),
    'D03_BINDING_FALLBACK_FORBIDDEN',
  );

  const duplicateBeforeMissing = request({
    reachable: [MECHANIC_A, MECHANIC_B],
    bindings: [binding(), binding()],
    snapshotRef: READY_AB_REF,
  });
  assertRejected(
    runRequest('precedence-duplicate', duplicateBeforeMissing),
    'D03_BINDING_DUPLICATE',
  );

  const staleAndDigest = binding();
  staleAndDigest.mechanic_contract_version = 2;
  staleAndDigest.contract_bundle_digest = DIGEST_B;
  assertRejected(
    runRequest('precedence-stale', request({ bindings: [staleAndDigest] })),
    'D03_BINDING_STALE',
  );
});

test('all seven binding fields reject null, boolean and string/integer coercion', () => {
  const mutations = {
    mechanic_id: [null, true, 1],
    mechanic_contract_version: [null, true, '1'],
    required_readiness: [null, true, 1],
    mechanic_contract_ref: [null, true, 1],
    gate_transition_envelope_ref: [null, true, 1],
    contract_bundle_digest: [null, true, 1],
    build_digest: [null, true, 1],
  };

  for (const [field, values] of Object.entries(mutations)) {
    for (const [index, coerced] of values.entries()) {
      const invalidBinding = binding();
      invalidBinding[field] = coerced;
      const result = runRequest(`coercion-${field}-${index}`, request({ bindings: [invalidBinding] }));
      assertRejected(result, 'D04_SKILL_RECORD_INVALID');
    }
  }
});

test('immutable binding refs reject mutable or non-content-addressed strings', () => {
  for (const field of ['mechanic_contract_ref', 'gate_transition_envelope_ref']) {
    for (const value of ['latest', 'contracts/mechanic@v1', `contracts/mechanic@${'A'.repeat(64)}`]) {
      const invalidBinding = binding();
      invalidBinding[field] = value;
      assertRejected(
        runRequest(`mutable-ref-${field}-${value.length}`, request({ bindings: [invalidBinding] })),
        'D04_SKILL_RECORD_INVALID',
      );
    }
  }
});

test('record binding key sets are closed', () => {
  const extraBinding = binding();
  extraBinding.allow_fallback = true;
  assertRejected(
    runRequest('record-extra-binding-key', request({ bindings: [extraBinding] })),
    'D04_SKILL_RECORD_INVALID',
  );

  const missingBinding = binding();
  delete missingBinding.build_digest;
  assertRejected(
    runRequest('record-missing-binding-key', request({ bindings: [missingBinding] })),
    'D04_SKILL_RECORD_INVALID',
  );

});

test('runtime_verified and released records cannot declare an empty executable mechanic universe', () => {
  for (const status of ['runtime_verified', 'released']) {
    const result = runRequest(
      `empty-executable-universe-${status}`,
      request({ status, reachable: [], bindings: [], snapshotRef: null }),
    );
    assertRejected(result, 'D03_BINDING_MISSING');
  }
});

test('full skill record validation rejects wrong types and nested extra keys in every section', () => {
  const mutations = [
    ['record-extra', (value) => { value.skill_record.fail_open_override = true; }],
    ['display-extra', (value) => {
      value.skill_record.display = {
        name_key: 'skill.fixture.name',
        description_key: 'skill.fixture.description',
        icon_ref: 'icon:fixture',
        formula_text: 'damage * 2',
      };
    }],
    ['ownership-type', (value) => { value.skill_record.ownership.source_type = true; }],
    ['ownership-extra', (value) => { value.skill_record.ownership.provider = 'custom'; }],
    ['classification-type', (value) => { value.skill_record.classification.effect_ids = 'effect:burn'; }],
    ['classification-extra', (value) => { value.skill_record.classification.local_damage = 3; }],
    ['targeting-type', (value) => { value.skill_record.targeting.target_cap = true; }],
    ['targeting-extra', (value) => { value.skill_record.targeting.fail_open = true; }],
    ['requirements-type', (value) => { value.skill_record.requirements.class_ids = 'class:mage'; }],
    ['requirements-extra', (value) => { value.skill_record.requirements.stat_definition = {}; }],
    ['execution-type', (value) => { value.skill_record.execution.resource_costs = {}; }],
    ['execution-extra', (value) => { value.skill_record.execution.formula_text = 'mana * 0.2'; }],
    ['resource-cost-extra', (value) => {
      value.skill_record.execution.resource_costs = [{
        resource_id: 'resource:mana',
        cost_rule_id: 'cost:fixture',
        cost_value_id: null,
        inline_math: '10',
      }];
    }],
    ['scaling-type', (value) => { value.skill_record.scaling.formula_id = false; }],
    ['scaling-extra', (value) => { value.skill_record.scaling.formula_text = 'attack * 1.5'; }],
    ['control-type', (value) => { value.skill_record.control.interruptible = 1; }],
    ['control-extra', (value) => { value.skill_record.control.poise_rule = 'local'; }],
    ['runtime-type', (value) => { value.skill_record.runtime.implementation_provider = false; }],
    ['runtime-extra', (value) => { value.skill_record.runtime.fail_open_override = true; }],
    ['lifecycle-type', (value) => { value.skill_record.lifecycle.validation_refs = 'evidence:one'; }],
    ['lifecycle-extra', (value) => { value.skill_record.lifecycle.additional_fields_allowed = true; }],
  ];

  for (const [name, mutate] of mutations) {
    const value = request();
    mutate(value);
    assertRejected(runRequest(`full-record-${name}`, value), 'D04_SKILL_RECORD_INVALID');
  }
});

test('authority snapshot must be uniquely registered, hash-pinned and exact-scope', () => {
  const unregistered = request({
    snapshotRef: `docs/contracts/domain-03-authority-snapshots/ready-direct-damage.yaml@${'0'.repeat(64)}`,
  });
  assertRejected(runRequest('authority-unregistered', unregistered), 'D04_SKILL_RECORD_INVALID');

  const extraAuthorityRow = request({ snapshotRef: READY_AB_REF });
  assertRejected(runRequest('authority-extra-row', extraAuthorityRow), 'D04_SKILL_RECORD_INVALID');

  const missingAuthorityRow = request({
    reachable: [MECHANIC_A, MECHANIC_B],
    bindings: [binding(), binding(MECHANIC_B)],
    snapshotRef: READY_A_REF,
  });
  assertRejected(runRequest('authority-missing-row', missingAuthorityRow), 'D04_SKILL_RECORD_INVALID');

  const callerCertified = request();
  callerCertified.d03_authority_snapshot = {
    scope_mechanic_ids: [MECHANIC_A],
    bindings: [{ ...binding(), readiness: 'RUNTIME_VERIFIED', revoked: false }],
  };
  assertRejected(runRequest('authority-caller-self-certified', callerCertified), 'D04_SKILL_RECORD_INVALID');
});

test('every malformed input path emits exactly one fail-closed JSON outcome', () => {
  const invalidUtf8Request = runRawRequest(
    'invalid-utf8-request',
    Buffer.from([0xff, 0xfe, 0xfd]),
  );
  assertRejected(invalidUtf8Request, 'D04_SKILL_RECORD_INVALID');

  const duplicateKeyRequest = runRawRequest(
    'duplicate-key-request',
    Buffer.from('schema_version: 1\nschema_version: 1\n', 'utf8'),
  );
  assertRejected(duplicateKeyRequest, 'D04_SKILL_RECORD_INVALID');

  const directory = mkdtempSync(path.join(tmpdir(), 'd04-invalid-contract-'));
  const invalidContract = path.join(directory, 'skill-library-contract.yaml');
  writeFileSync(invalidContract, Buffer.from([0xff, 0xfe, 0xfd]));
  const invalidUtf8Contract = runRequest('invalid-utf8-contract', request(), invalidContract);
  assert.doesNotMatch(invalidUtf8Contract.stderr, /Traceback/);
  assertRejected(invalidUtf8Contract, 'D04_SKILL_RECORD_INVALID');

  const missingRequest = spawnSync(
    'python3',
    [SCRIPT, path.join(directory, 'missing-request.yaml')],
    { cwd: ROOT, encoding: 'utf8' },
  );
  const lines = missingRequest.stdout.trim().split('\n').filter(Boolean);
  assert.equal(lines.length, 1);
  assert.doesNotMatch(missingRequest.stderr, /Traceback/);
  assertRejected({ ...missingRequest, output: JSON.parse(lines[0]) }, 'D04_SKILL_RECORD_INVALID');

  for (const [name, args] of [
    ['missing-argv', [SCRIPT]],
    ['extra-argv', [SCRIPT, 'one', 'two', 'three']],
  ]) {
    const malformedInvocation = spawnSync('python3', args, { cwd: ROOT, encoding: 'utf8' });
    const invocationLines = malformedInvocation.stdout.trim().split('\n').filter(Boolean);
    assert.equal(invocationLines.length, 1, name);
    assert.doesNotMatch(malformedInvocation.stderr, /Traceback/, name);
    assertRejected(
      { ...malformedInvocation, output: JSON.parse(invocationLines[0]) },
      'D04_SKILL_RECORD_INVALID',
    );
  }
});
