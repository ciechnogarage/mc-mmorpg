#!/usr/bin/env node

import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');

const VERDICTS = ['PASS', 'FAIL', 'BLOCKED', 'INSUFFICIENT_EVIDENCE'];
const PROOF_LEVELS = ['STATIC_CONTRACT', 'RUNTIME_PROOF', 'INTEGRATION_PROOF', 'PLAYER_PROOF'];
const PROOF_RANK = new Map(PROOF_LEVELS.map((level, index) => [level, index]));
const SCENARIO_DEFAULTS = {
  character_creator_login_persistence: {
    domain: 'character',
    system: 'character_creator_login_persistence',
    test_type: 'foundation_runtime_adapter',
  },
  reward_restart_recovery: {
    domain: 'economy',
    system: 'quest_reward',
    test_type: 'foundation_runtime_adapter',
  },
};

class AdapterError extends Error {
  constructor(code, message) {
    super(`${code}: ${message}`);
    this.code = code;
  }
}

function parseArgs(argv) {
  const args = {};
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith('--')) {
      throw new AdapterError('ARGUMENT_ERROR', `unexpected argument ${token}`);
    }
    const key = token.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) {
      throw new AdapterError('ARGUMENT_ERROR', `missing value for --${key}`);
    }
    args[key] = value;
    index += 1;
  }

  for (const key of ['input', 'scenario', 'output']) {
    if (!args[key]) {
      throw new AdapterError('ARGUMENT_ERROR', `--${key} is required`);
    }
  }
  return args;
}

function repoRelative(filePath) {
  const absolute = path.resolve(filePath);
  const relative = path.relative(REPO_ROOT, absolute);
  return relative.startsWith('..') ? absolute : relative;
}

function sha256(filePath) {
  return createHash('sha256').update(readFileSync(filePath)).digest('hex');
}

function readReport(inputPath) {
  const raw = readFileSync(inputPath, 'utf8');
  const parsed = JSON.parse(raw);
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new AdapterError('REPORT_SHAPE_ERROR', 'foundation report must be a JSON object');
  }
  return parsed;
}

function normalizeProof(values) {
  if (!Array.isArray(values)) return [];
  return [...new Set(values.filter((value) => PROOF_LEVELS.includes(value)))];
}

function missingProof(requiredProof, achievedProof) {
  const achieved = new Set(achievedProof);
  return requiredProof.filter((level) => !achieved.has(level));
}

function highestProof(achievedProof) {
  return achievedProof
    .slice()
    .sort((left, right) => PROOF_RANK.get(right) - PROOF_RANK.get(left))[0] || 'STATIC_CONTRACT';
}

function normalizeVerdict(value) {
  return VERDICTS.includes(value) ? value : 'BLOCKED';
}

function runtimeStatusFor(verdict, missingProof = []) {
  if (verdict === 'PASS' && missingProof.length === 0) return 'verified';
  if (verdict === 'PASS') return 'partial';
  if (verdict === 'BLOCKED') return 'blocked';
  if (verdict === 'INSUFFICIENT_EVIDENCE') return 'partial';
  return 'partial';
}

function releaseEffectFor(verdict) {
  return verdict === 'PASS' ? 'supports_candidate' : 'blocks_release';
}

function buildParserFailureReport(error, inputPath) {
  return {
    report_id: 'foundation-runtime-parser-failure',
    verdict: 'BLOCKED',
    domain: undefined,
    system: undefined,
    environment: 'tools-only',
    test_type: 'foundation_runtime_adapter_parser_failure',
    contract_status: 'blocked',
    requiredProof: ['STATIC_CONTRACT'],
    achievedProof: [],
    blockers: ['foundation_runtime_parser_failure'],
    proofArtifacts: [],
    build: {
      commit: 'baseline-no-runtime',
      paper: 'baseline-no-runtime',
      plugin_manifest_hash: 'baseline-no-runtime',
    },
    execution: {
      runner: 'tools/qa/adapters/foundation-runtime.mjs',
      started_at: new Date(0).toISOString(),
      finished_at: new Date(0).toISOString(),
      executed_by: 'foundation-runtime-adapter',
      log_paths: [repoRelative(inputPath)],
      environment_control: {
        lease: 'none',
        workspace_root: REPO_ROOT,
        runtime_touched: false,
        staging_touched: false,
        rollback: {
          method: 'not_required_parser_failure',
          proof_paths: [repoRelative(inputPath)],
        },
      },
    },
    observations: [`Parser failure: ${error.message}`],
  };
}

function buildRecord(report, inputPath, scenarioId, parserFailure = false) {
  const scenarioDefaults = SCENARIO_DEFAULTS[scenarioId] || {};
  const requiredProof = normalizeProof(report.requiredProof);
  const achievedProof = normalizeProof(report.achievedProof);
  const missing = missingProof(requiredProof, achievedProof);
  let verdict = normalizeVerdict(report.verdict);
  const observations = Array.isArray(report.observations) ? [...report.observations] : [];

  if (verdict === 'PASS' && missing.length > 0) {
    verdict = 'INSUFFICIENT_EVIDENCE';
    observations.push(`Adapter degraded PASS to INSUFFICIENT_EVIDENCE because missing required proof: ${missing.join(', ')}`);
  }

  if (!VERDICTS.includes(report.verdict)) {
    observations.push(`Adapter mapped invalid or missing verdict ${JSON.stringify(report.verdict)} to BLOCKED.`);
  }

  const inputRel = repoRelative(inputPath);
  const inputHash = existsSync(inputPath) ? sha256(inputPath) : 'missing-input';
  const artifacts = Array.isArray(report.proofArtifacts) ? [...report.proofArtifacts] : [];
  artifacts.unshift({
    kind: parserFailure ? 'parser-failure-input' : 'foundation-runtime-report',
    path: inputRel,
    sha256: inputHash,
  });

  const build = report.build || {};
  const execution = report.execution || {};
  const environmentControl = execution.environment_control || {};
  const rollback = environmentControl.rollback || {};

  return {
    evidence_id: `${scenarioId}_${report.report_id || 'foundation_runtime'}_${verdict.toLowerCase()}`.replace(/[^a-zA-Z0-9_]+/g, '_'),
    scenario_id: scenarioId,
    domain: report.domain || scenarioDefaults.domain || 'qa',
    system: report.system || scenarioDefaults.system || scenarioId,
    environment: report.environment || 'tools-only',
    test_type: report.test_type || scenarioDefaults.test_type || 'foundation_runtime_adapter',
    contract_status: report.contract_status || (parserFailure ? 'blocked' : 'approved'),
    runtime_status: runtimeStatusFor(verdict, missing),
    release_effect: releaseEffectFor(verdict),
    migration_snapshot_ref: report.migration_snapshot_ref || 'domain-17-legacy-evidence-migration-snapshot-2026-07-31',
    legacy_source_ids: report.legacy_source_ids || ['runtime_evidence_register_2026_07_13'],
    legacy_sha256: report.legacy_sha256 || [],
    legacy_status_mapping: report.legacy_status_mapping || [],
    evidence_types: Array.isArray(report.evidence_types) ? report.evidence_types : [],
    contract_refs: Array.isArray(report.contract_refs) ? report.contract_refs : [],
    metadata: report.metadata && typeof report.metadata === 'object' && !Array.isArray(report.metadata)
      ? report.metadata
      : {},
    proof_level_mapping: {
      supported_levels: PROOF_LEVELS,
      requiredProof,
      achievedProof,
      missingProof: missing,
      highest_achieved: highestProof(achievedProof),
    },
    preconditions: Array.isArray(report.preconditions) ? report.preconditions : [],
    steps: Array.isArray(report.steps) ? report.steps : [],
    assertions: Array.isArray(report.assertions) ? report.assertions : [],
    artifacts,
    build: {
      paper: build.paper || 'baseline-no-runtime',
      plugin_manifest_hash: build.plugin_manifest_hash || 'baseline-no-runtime',
      commit: build.commit || 'baseline-no-runtime',
      plugin_jar_inventory_hash: build.plugin_jar_inventory_hash || 'not_provided_by_foundation_runtime_adapter',
      configuration_hashes: build.configuration_hashes || {},
    },
    execution: {
      runner: execution.runner || 'MCMMORPG/_validation/foundation_runtime.js',
      backend: execution.backend || 'not_run_tools_only',
      docker_volume_id: execution.docker_volume_id || 'not_run_tools_only',
      test_account_uuid: execution.test_account_uuid || 'not_run_tools_only',
      started_at: execution.started_at || new Date(0).toISOString(),
      finished_at: execution.finished_at || new Date(0).toISOString(),
      executed_by: execution.executed_by || 'foundation-runtime-adapter',
      log_paths: Array.isArray(execution.log_paths) && execution.log_paths.length ? execution.log_paths : [inputRel],
      environment_control: {
        lease: environmentControl.lease || 'none',
        workspace_root: environmentControl.workspace_root || REPO_ROOT,
        runtime_touched: environmentControl.runtime_touched === true,
        staging_touched: environmentControl.staging_touched === true,
        rollback: {
          method: rollback.method || 'not_required_tools_only_adapter',
          proof_paths: Array.isArray(rollback.proof_paths) && rollback.proof_paths.length ? rollback.proof_paths : [inputRel],
        },
      },
    },
    result: {
      verdict,
      proof_level: highestProof(achievedProof),
      blocker_ids: Array.isArray(report.blockers) ? report.blockers : [],
      observations,
    },
  };
}

export function adaptFoundationRuntimeReport({ input, scenario, output }) {
  let report;
  let parserFailure = false;
  try {
    report = readReport(input);
  } catch (error) {
    parserFailure = true;
    report = buildParserFailureReport(error, input);
  }

  const record = buildRecord(report, input, scenario, parserFailure);
  mkdirSync(path.dirname(path.resolve(output)), { recursive: true });
  writeFileSync(output, `${JSON.stringify(record, null, 2)}\n`, 'utf8');
  return record;
}

function runCli() {
  try {
    const args = parseArgs(process.argv.slice(2));
    const record = adaptFoundationRuntimeReport(args);
    console.log(`foundation runtime adapter wrote ${args.output} with verdict ${record.result.verdict}`);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`foundation runtime adapter failed: ${message}`);
    process.exitCode = 1;
  }
}

if (process.argv[1] === __filename) {
  runCli();
}
