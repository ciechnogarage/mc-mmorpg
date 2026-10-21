import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';
import { evaluateReleaseReadiness } from '../reports/release-readiness.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');
const ADAPTER = path.join(REPO_ROOT, 'tools/qa/adapters/foundation-runtime.mjs');
const READINESS_CLI = path.join(REPO_ROOT, 'tools/qa/reports/release-readiness.mjs');

function writeJsonYaml(filePath, data) {
  writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, 'utf8');
}

function releaseGradeExecution(finishedAt = '2026-08-03T00:00:00Z') {
  return {
    runner: 'MCMMORPG/_validation/release_readiness_runtime_probe.mjs',
    backend: 'staging-release-backend',
    docker_volume_id: 'staging-release-volume',
    test_account_uuid: '00000000-0000-4000-8000-000000000001',
    started_at: finishedAt,
    finished_at: finishedAt,
    executed_by: 'qa-release-evidence-runner',
    log_paths: ['docs/qa/evidence/release-grade/server.log'],
    environment_control: {
      lease: 'dev-qa',
      workspace_root: REPO_ROOT,
      runtime_touched: true,
      staging_touched: false,
      rollback: {
        method: 'runtime_snapshot_restore',
        proof_paths: ['docs/qa/evidence/release-grade/rollback-proof.md'],
      },
    },
  };
}

function record(overrides = {}) {
  const output = {
    evidence_id: overrides.evidence_id || 'reward-dev-blocker',
    scenario_id: overrides.scenario_id || 'reward_restart_recovery',
    domain: 'economy',
    system: 'quest_reward',
    environment: overrides.environment || 'dev-qa',
    test_type: overrides.test_type || 'release_readiness_fixture',
    contract_status: overrides.contract_status || 'blocked',
    runtime_status: overrides.runtime_status || 'blocked',
    release_effect: overrides.release_effect || 'blocks_release',
    build: {
      commit: overrides.commit || 'c1',
      paper: overrides.paper || 'p1',
      plugin_manifest_hash: overrides.plugin_manifest_hash || 'm1',
    },
    execution: {
      finished_at: overrides.finished_at || '2026-08-03T00:00:00Z',
    },
    result: {
      verdict: overrides.verdict || 'BLOCKED',
      blocker_ids: overrides.blocker_ids || ['D17-REWARD-IDEMPOTENCY-NO-DEV-QA-LEASE'],
      observations: overrides.observations || ['fixture observation'],
    },
  };
  if ('execution' in overrides) output.execution = overrides.execution;
  if ('evidence_types' in overrides) output.evidence_types = overrides.evidence_types;
  if ('contract_refs' in overrides) output.contract_refs = overrides.contract_refs;
  if ('metadata' in overrides) output.metadata = overrides.metadata;
  return output;
}

function withFixture({ records, gateOverrides = {}, policyOverrides = {} }, callback) {
  const rootDir = mkdtempSync(path.join(os.tmpdir(), 'release-readiness-'));
  try {
    mkdirSync(rootDir, { recursive: true });
    writeJsonYaml(path.join(rootDir, 'e2e-scenarios.yaml'), {
      schema_version: 1,
      scenarios: [
        {
          scenario_id: 'reward_restart_recovery',
          domain: 'economy',
          system: 'quest_reward',
          release_critical: true,
          contract_refs: ['docs/contracts/account-character-contract.yaml'],
          required_evidence_types: ['server-log', 'bot-transcript'],
        },
      ],
    });
    writeJsonYaml(path.join(rootDir, 'release-gates.yaml'), {
      schema_version: 1,
      gate_policy: {
        evidence_max_age_hours: 168,
        required_build_context_fields: ['commit', 'paper', 'plugin_manifest_hash'],
        aggregation_rules: {
          environment_blocker_verdict: 'BLOCKED',
        },
        default_blockers: ['missing_runtime_evidence'],
        ...policyOverrides,
      },
      build_context: {
        commit: 'c1',
        paper: 'p1',
        plugin_manifest_hash: 'm1',
      },
      gates: [
        {
          gate_id: 'reward_gate',
          gate_type: 'release',
          release_status: 'candidate',
          release_effect: 'supports_candidate',
          required_scenarios: ['reward_restart_recovery'],
          required_verdict: 'PASS',
          required_runtime_status: 'verified',
          required_release_effect_after_evidence: 'supports_candidate',
          required_contracts: ['docs/contracts/account-character-contract.yaml'],
          required_metadata: ['test_account_uuid'],
          current_blockers: ['missing_runtime_evidence'],
          ...gateOverrides,
        },
      ],
    });
    writeJsonYaml(path.join(rootDir, 'runtime-evidence-registry.yaml'), {
      schema_version: 1,
      records,
    });
    return callback(rootDir);
  } finally {
    rmSync(rootDir, { recursive: true, force: true });
  }
}

test('consumes current blocker records without manual mapping', () => {
  const report = evaluateReleaseReadiness({
    rootDir: path.join(REPO_ROOT, 'docs/qa'),
    env: 'staging',
  });

  assert.equal(report.summary.release_status, 'blocked');
  assert.equal(report.summary.release_effect, 'blocks_release');
  assert.equal(report.summary.selected_evidence_records, 2);
  assert.ok(report.gates.every((gate) => gate.configured_release_status === 'candidate'));
  assert.equal(report.runtime_touched, false);
  assert.equal(report.staging_touched, false);
  assert.ok(report.blockers.includes('D17-REWARD-IDEMPOTENCY-NO-DEV-QA-LEASE'));
  assert.ok(report.evidence_selection.latest_valid_per_build_context.some((selected) => (
    selected.scenario_id === 'reward_restart_recovery'
    && selected.selection_reason === 'latest_valid_cross_environment_blocker'
  )));
});

test('rejects evidence when build context differs', () => {
  withFixture({
    records: [
      record({
        evidence_id: 'reward-stale-build',
        commit: 'different',
        environment: 'staging',
      }),
    ],
  }, (rootDir) => {
    const report = evaluateReleaseReadiness({
      rootDir,
      env: 'staging',
      now: '2026-08-03T00:00:00Z',
    });

    assert.equal(report.summary.selected_evidence_records, 0);
    assert.equal(report.summary.rejected_evidence_records, 1);
    assert.equal(report.evidence_selection.rejected[0].reasons[0].code, 'stale_build_context');
    assert.ok(report.blockers.includes('missing_runtime_evidence'));
  });
});

test('rejects evidence older than gate policy max age', () => {
  withFixture({
    records: [
      record({
        evidence_id: 'reward-stale-age',
        environment: 'staging',
        finished_at: '2026-08-01T00:00:00Z',
      }),
    ],
    policyOverrides: {
      evidence_max_age_hours: 1,
    },
  }, (rootDir) => {
    const report = evaluateReleaseReadiness({
      rootDir,
      env: 'staging',
      now: '2026-08-03T00:00:00Z',
    });

    assert.equal(report.summary.selected_evidence_records, 0);
    assert.equal(report.evidence_selection.rejected[0].reasons[0].code, 'stale_age');
    assert.ok(report.blockers.includes('stale_age'));
  });
});

test('CLI rejects stale age without --now', () => {
  withFixture({
    records: [
      record({
        evidence_id: 'reward-stale-age-without-now',
        environment: 'staging',
        finished_at: '2000-01-01T00:00:00Z',
      }),
    ],
    policyOverrides: {
      evidence_max_age_hours: 1,
    },
  }, (rootDir) => {
    const output = path.join(rootDir, 'readiness.json');
    const markdownOutput = path.join(rootDir, 'readiness.md');
    const result = spawnSync('node', [
      READINESS_CLI,
      '--env',
      'staging',
      '--root',
      rootDir,
      '--output',
      output,
      '--markdown-output',
      markdownOutput,
    ], {
      cwd: REPO_ROOT,
      encoding: 'utf8',
    });
    assert.equal(result.status, 0, result.stderr || result.stdout);
    const report = JSON.parse(readFileSync(output, 'utf8'));
    assert.equal(report.summary.selected_evidence_records, 0);
    assert.equal(report.summary.rejected_evidence_records, 1);
    assert.equal(report.evidence_selection.rejected[0].reasons[0].code, 'stale_age');
  });
});

test('rejects hand-authored PASS missing proof requirements', () => {
  withFixture({
    records: [
      record({
        evidence_id: 'reward-fake-pass',
        environment: 'staging',
        test_type: 'release_readiness_runtime_evidence',
        contract_status: 'approved',
        runtime_status: 'verified',
        release_effect: 'supports_candidate',
        verdict: 'PASS',
        blocker_ids: [],
        execution: releaseGradeExecution(),
      }),
    ],
  }, (rootDir) => {
    const report = evaluateReleaseReadiness({
      rootDir,
      env: 'staging',
      now: '2026-08-03T00:00:00Z',
    });
    assert.equal(report.summary.release_status, 'blocked');
    assert.equal(report.summary.selected_evidence_records, 0);
    assert.ok(report.blockers.includes('missing_required_evidence_type'));
    assert.ok(report.blockers.includes('missing_required_contract'));
    assert.ok(report.blockers.includes('missing_required_metadata'));
  });
});

test('required_evidence_ids must match selected evidence', () => {
  withFixture({
    gateOverrides: {
      required_evidence_ids: ['reward-required-pass'],
    },
    records: [
      record({
        evidence_id: 'reward-other-pass',
        environment: 'staging',
        test_type: 'release_readiness_runtime_evidence',
        contract_status: 'approved',
        runtime_status: 'verified',
        release_effect: 'supports_candidate',
        verdict: 'PASS',
        blocker_ids: [],
        execution: releaseGradeExecution(),
        evidence_types: ['server-log', 'bot-transcript'],
        contract_refs: ['docs/contracts/account-character-contract.yaml'],
        metadata: {
          test_account_uuid: '00000000-0000-4000-8000-000000000001',
        },
      }),
    ],
  }, (rootDir) => {
    const report = evaluateReleaseReadiness({
      rootDir,
      env: 'staging',
      now: '2026-08-03T00:00:00Z',
    });
    assert.equal(report.summary.release_status, 'blocked');
    assert.ok(report.blockers.includes('required_evidence_id_not_selected'));
  });
});

test('promotion records require approved contract status', () => {
  withFixture({
    records: [
      record({
        evidence_id: 'reward-proposed-pass',
        environment: 'staging',
        test_type: 'release_readiness_runtime_evidence',
        contract_status: 'proposed',
        runtime_status: 'verified',
        release_effect: 'supports_candidate',
        verdict: 'PASS',
        blocker_ids: [],
        execution: releaseGradeExecution(),
        evidence_types: ['server-log', 'bot-transcript'],
        contract_refs: ['docs/contracts/account-character-contract.yaml'],
        metadata: {
          test_account_uuid: '00000000-0000-4000-8000-000000000001',
        },
      }),
    ],
  }, (rootDir) => {
    const report = evaluateReleaseReadiness({
      rootDir,
      env: 'staging',
      now: '2026-08-03T00:00:00Z',
    });
    assert.equal(report.summary.release_status, 'blocked');
    assert.ok(report.blockers.includes('promotion_contract_status_not_approved'));
  });
});

test('promotion records reject fixture and no-runtime provenance', () => {
  withFixture({
    records: [
      record({
        evidence_id: 'reward-fixture-pass',
        environment: 'staging',
        test_type: 'release_readiness_fixture',
        contract_status: 'approved',
        runtime_status: 'verified',
        release_effect: 'supports_candidate',
        verdict: 'PASS',
        blocker_ids: [],
        execution: {
          ...releaseGradeExecution(),
          runner: 'tools/qa/tests/release-readiness.test.mjs',
          environment_control: {
            ...releaseGradeExecution().environment_control,
            lease: 'none',
            runtime_touched: false,
          },
        },
        evidence_types: ['server-log', 'bot-transcript'],
        contract_refs: ['docs/contracts/account-character-contract.yaml'],
        metadata: {
          test_account_uuid: '00000000-0000-4000-8000-000000000001',
        },
      }),
    ],
  }, (rootDir) => {
    const report = evaluateReleaseReadiness({
      rootDir,
      env: 'staging',
      now: '2026-08-03T00:00:00Z',
    });
    assert.equal(report.summary.release_status, 'blocked');
    assert.ok(report.blockers.includes('promotion_runtime_not_touched'));
    assert.ok(report.blockers.includes('promotion_missing_dev_lease'));
    assert.ok(report.blockers.includes('promotion_fixture_or_test_provenance'));
  });
});

test('prefers target environment PASS evidence over cross-environment blocker', () => {
  withFixture({
    records: [
      record({
        evidence_id: 'reward-dev-blocker',
        environment: 'dev-qa',
      }),
      record({
        evidence_id: 'reward-staging-pass',
        environment: 'staging',
        test_type: 'release_readiness_runtime_evidence',
        contract_status: 'approved',
        runtime_status: 'verified',
        release_effect: 'supports_candidate',
        verdict: 'PASS',
        blocker_ids: [],
        execution: releaseGradeExecution('2026-08-02T00:00:00Z'),
        evidence_types: ['server-log', 'bot-transcript'],
        contract_refs: ['docs/contracts/account-character-contract.yaml'],
        metadata: {
          test_account_uuid: '00000000-0000-4000-8000-000000000001',
        },
        finished_at: '2026-08-02T00:00:00Z',
      }),
    ],
  }, (rootDir) => {
    const report = evaluateReleaseReadiness({
      rootDir,
      env: 'staging',
      now: '2026-08-03T00:00:00Z',
    });

    assert.equal(report.summary.release_status, 'candidate');
    assert.equal(report.summary.readiness_claim_allowed, true);
    assert.equal(report.evidence_selection.latest_valid_per_build_context[0].evidence_id, 'reward-staging-pass');
    assert.equal(report.scenarios[0].selection_reason, 'latest_valid_target_environment_evidence');
  });
});

test('adapter output supports stage-1 candidate path when all proof requirements are present', () => {
  const tempRoot = mkdtempSync(path.join(os.tmpdir(), 'release-readiness-adapter-'));
  try {
    const input = path.join(tempRoot, 'foundation-pass.json');
    const output = path.join(tempRoot, 'foundation-pass.record.json');
    writeJsonYaml(input, {
      report_id: 'reward-stage-1-positive-path',
      verdict: 'PASS',
      domain: 'economy',
      system: 'quest_reward',
      environment: 'staging',
      test_type: 'release_readiness_runtime_evidence',
      contract_status: 'approved',
      requiredProof: ['STATIC_CONTRACT', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
      achievedProof: ['STATIC_CONTRACT', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'],
      evidence_types: ['server-log', 'bot-transcript'],
      contract_refs: ['docs/contracts/account-character-contract.yaml'],
      metadata: {
        test_account_uuid: '00000000-0000-4000-8000-000000000001',
      },
      build: {
        commit: 'c1',
        paper: 'p1',
        plugin_manifest_hash: 'm1',
      },
      execution: {
        runner: 'MCMMORPG/_validation/release_readiness_runtime_probe.mjs',
        backend: 'staging-release-backend',
        docker_volume_id: 'staging-release-volume',
        test_account_uuid: '00000000-0000-4000-8000-000000000001',
        started_at: '2026-08-03T00:00:00Z',
        finished_at: '2026-08-03T00:00:00Z',
        executed_by: 'qa-release-evidence-runner',
        log_paths: [input],
        environment_control: {
          lease: 'dev-qa',
          workspace_root: REPO_ROOT,
          runtime_touched: true,
          staging_touched: false,
          rollback: {
            method: 'runtime_snapshot_restore',
            proof_paths: [input],
          },
        },
      },
      observations: ['Positive runtime-shaped record for evaluator contract test.'],
    });

    const adapter = spawnSync('node', [
      ADAPTER,
      '--input',
      input,
      '--scenario',
      'reward_restart_recovery',
      '--output',
      output,
    ], {
      cwd: REPO_ROOT,
      encoding: 'utf8',
    });
    assert.equal(adapter.status, 0, adapter.stderr || adapter.stdout);
    const emittedRecord = JSON.parse(readFileSync(output, 'utf8'));
    assert.equal(emittedRecord.runtime_status, 'verified');
    assert.equal(emittedRecord.release_effect, 'supports_candidate');

    withFixture({
      records: [emittedRecord],
    }, (rootDir) => {
      const report = evaluateReleaseReadiness({
        rootDir,
        env: 'staging',
        now: '2026-08-03T00:00:00Z',
      });
      assert.equal(report.summary.release_status, 'candidate');
      assert.equal(report.summary.release_effect, 'supports_candidate');
      assert.equal(report.summary.readiness_claim_allowed, true);
      assert.equal(report.scenarios[0].selected_evidence_id, emittedRecord.evidence_id);
    });
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
});
