#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');
const DEFAULT_QA_ROOT = path.join(REPO_ROOT, 'docs/qa');
const DEFAULT_REPORT_DIR = path.join(DEFAULT_QA_ROOT, 'reports');

const SOURCE_FILES = {
  scenarios: 'e2e-scenarios.yaml',
  gates: 'release-gates.yaml',
  evidence: 'runtime-evidence-registry.yaml',
};

const RELEASE_STATUS_RANK = new Map([
  ['released', 0],
  ['candidate', 1],
  ['blocked', 2],
]);

const RUNTIME_STATUS_RANK = new Map([
  ['verified', 0],
  ['partial', 1],
  ['loaded', 2],
  ['not_tested', 3],
  ['blocked', 4],
]);

const CONTRACT_STATUS_RANK = new Map([
  ['approved', 0],
  ['proposed', 1],
  ['draft', 2],
  ['blocked', 3],
]);

class ReadinessError extends Error {
  constructor(code, message) {
    super(`${code}: ${message}`);
    this.code = code;
  }
}

function parseArgs(argv) {
  const args = {
    env: 'staging',
    root: DEFAULT_QA_ROOT,
    output: null,
    markdownOutput: null,
    now: null,
    write: true,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (token === '--no-write') {
      args.write = false;
      continue;
    }

    if (!token.startsWith('--')) {
      throw new ReadinessError('ARGUMENT_ERROR', `unexpected argument ${token}`);
    }

    const key = token.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) {
      throw new ReadinessError('ARGUMENT_ERROR', `missing value for --${key}`);
    }

    if (key === 'env') args.env = value;
    else if (key === 'root') args.root = path.resolve(value);
    else if (key === 'output') args.output = path.resolve(value);
    else if (key === 'markdown-output') args.markdownOutput = path.resolve(value);
    else if (key === 'now') args.now = value;
    else throw new ReadinessError('ARGUMENT_ERROR', `unknown option --${key}`);

    index += 1;
  }

  return args;
}

function repoRelative(filePath) {
  const relative = path.relative(REPO_ROOT, filePath);
  return relative.startsWith('..') ? filePath : relative;
}

function readText(filePath) {
  if (!existsSync(filePath)) {
    throw new ReadinessError('MISSING_FILE', `${filePath} does not exist`);
  }
  return readFileSync(filePath, 'utf8');
}

function sha256Text(text) {
  return createHash('sha256').update(text).digest('hex');
}

function loadYaml(filePath) {
  readText(filePath);
  const parser = [
    'import json, sys, yaml',
    'with open(sys.argv[1], "r", encoding="utf-8") as handle:',
    '    data = yaml.safe_load(handle)',
    'print(json.dumps(data if data is not None else {}))',
  ].join('\n');

  const result = spawnSync('python3', ['-c', parser, filePath], {
    encoding: 'utf8',
    maxBuffer: 1024 * 1024,
  });

  if (result.status !== 0) {
    const detail = (result.stderr || result.stdout || '').trim();
    throw new ReadinessError('YAML_PARSE_FAILED', `${filePath}: ${detail}`);
  }

  return JSON.parse(result.stdout);
}

function loadInputs(rootDir) {
  const paths = Object.fromEntries(
    Object.entries(SOURCE_FILES).map(([key, fileName]) => [key, path.join(rootDir, fileName)]),
  );

  const sourceFiles = Object.fromEntries(
    Object.entries(paths).map(([key, filePath]) => {
      const text = readText(filePath);
      return [key, {
        path: repoRelative(filePath),
        sha256: sha256Text(text),
      }];
    }),
  );

  return {
    rootDir,
    paths,
    sourceFiles,
    scenarios: loadYaml(paths.scenarios),
    gates: loadYaml(paths.gates),
    evidence: loadYaml(paths.evidence),
  };
}

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function asObject(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function maxRankStatus(values, rank, fallback) {
  const present = values.filter((value) => rank.has(value));
  if (present.length === 0) return fallback;
  return present.reduce((worst, value) => (rank.get(value) > rank.get(worst) ? value : worst));
}

function parseTime(value) {
  const timestamp = Date.parse(value);
  return Number.isFinite(timestamp) ? timestamp : null;
}

function latestTimestamp(records) {
  const timestamps = records
    .map((record) => parseTime(record?.execution?.finished_at || record?.execution?.started_at))
    .filter((timestamp) => timestamp !== null);
  if (timestamps.length === 0) return Date.parse('1970-01-01T00:00:00Z');
  return Math.max(...timestamps);
}

function compareEvidence(a, b) {
  const aTime = parseTime(a?.execution?.finished_at || a?.execution?.started_at) || 0;
  const bTime = parseTime(b?.execution?.finished_at || b?.execution?.started_at) || 0;
  if (aTime !== bTime) return bTime - aTime;
  return String(a.evidence_id).localeCompare(String(b.evidence_id));
}

function buildContextMatches(record, buildContext, requiredFields) {
  const recordBuild = asObject(record.build);
  const mismatches = [];

  requiredFields.forEach((field) => {
    if (recordBuild[field] !== buildContext[field]) {
      mismatches.push({
        field,
        expected: buildContext[field],
        actual: recordBuild[field],
      });
    }
  });

  return mismatches;
}

function promotionClaimed(record) {
  return record?.result?.verdict === 'PASS'
    || record.runtime_status === 'verified'
    || record.release_effect === 'supports_candidate';
}

function fixtureLikeValue(value) {
  return typeof value === 'string'
    && /(^|[/\\])(tools[/\\]qa[/\\]tests|fixtures)([/\\]|$)|(^|[-_/])fixture([-_/]|$)|test-only/i.test(value);
}

function releaseGradeProvenanceFailures(record) {
  const reasons = [];
  const environmentControl = asObject(record.execution?.environment_control);
  const lease = environmentControl.lease;

  if (record.contract_status !== 'approved') {
    reasons.push({
      code: 'promotion_contract_status_not_approved',
      detail: record.contract_status,
    });
  }

  if (environmentControl.runtime_touched !== true) {
    reasons.push({
      code: 'promotion_runtime_not_touched',
      detail: environmentControl.runtime_touched,
    });
  }

  if (typeof lease !== 'string' || !/^dev-[a-z0-9][a-z0-9_-]*$/i.test(lease)) {
    reasons.push({
      code: 'promotion_missing_dev_lease',
      detail: lease ?? null,
    });
  }

  const provenanceValues = [
    record.evidence_id,
    record.test_type,
    record.execution?.runner,
    record.execution?.executed_by,
    record.execution?.backend,
    record.execution?.docker_volume_id,
    ...asArray(record.execution?.log_paths),
    ...asArray(environmentControl.rollback?.proof_paths),
    ...asArray(record.artifacts).flatMap((artifact) => [artifact.path, artifact.kind]),
  ];
  const fixtureValues = provenanceValues.filter(fixtureLikeValue);
  if (fixtureValues.length > 0) {
    reasons.push({
      code: 'promotion_fixture_or_test_provenance',
      detail: fixtureValues,
    });
  }

  return reasons;
}

function missingArrayValues(actual, required) {
  const actualSet = new Set(asArray(actual));
  return asArray(required).filter((value) => !actualSet.has(value));
}

function promotionProofFailures(record, requirements) {
  if (!promotionClaimed(record)) return [];

  const reasons = releaseGradeProvenanceFailures(record);
  const missingEvidenceTypes = missingArrayValues(record.evidence_types, requirements.evidence_types);
  if (missingEvidenceTypes.length > 0) {
    reasons.push({
      code: 'missing_required_evidence_type',
      detail: missingEvidenceTypes,
    });
  }

  const missingContracts = missingArrayValues(record.contract_refs, requirements.contracts);
  if (missingContracts.length > 0) {
    reasons.push({
      code: 'missing_required_contract',
      detail: missingContracts,
    });
  }

  const metadata = asObject(record.metadata);
  const missingMetadata = requirements.metadata.filter((key) => !(key in metadata) || metadata[key] === null || metadata[key] === '');
  if (missingMetadata.length > 0) {
    reasons.push({
      code: 'missing_required_metadata',
      detail: missingMetadata,
    });
  }

  return reasons;
}

function evidenceAgeHours(record, nowMs) {
  const finishedAt = parseTime(record?.execution?.finished_at || record?.execution?.started_at);
  if (finishedAt === null) return null;
  return (nowMs - finishedAt) / (1000 * 60 * 60);
}

function classifyEvidence(record, context) {
  const reasons = [];
  const buildMismatches = buildContextMatches(record, context.buildContext, context.requiredBuildFields);
  if (buildMismatches.length > 0) {
    reasons.push({
      code: 'stale_build_context',
      detail: buildMismatches,
    });
  }

  const ageHours = evidenceAgeHours(record, context.nowMs);
  if (ageHours === null) {
    reasons.push({
      code: 'missing_finished_at',
      detail: 'execution.finished_at or execution.started_at is required for stale evidence checks',
    });
  } else if (Number.isFinite(context.maxAgeHours) && ageHours > context.maxAgeHours) {
    reasons.push({
      code: 'stale_age',
      detail: {
        age_hours: Number(ageHours.toFixed(3)),
        max_age_hours: context.maxAgeHours,
      },
    });
  }

  const scenarioRequirements = context.requirementsByScenario.get(record.scenario_id);
  if (scenarioRequirements) {
    reasons.push(...promotionProofFailures(record, scenarioRequirements));
  }

  return {
    valid: reasons.length === 0,
    reasons,
  };
}

function buildScenarioRequirements(scenarios, gates) {
  const requirementsByScenario = new Map();

  scenarios.forEach((scenario) => {
    requirementsByScenario.set(scenario.scenario_id, {
      evidence_types: asArray(scenario.required_evidence_types),
      contracts: [...new Set(asArray(scenario.contract_refs))],
      metadata: [],
    });
  });

  asArray(gates).forEach((gate) => {
    asArray(gate.required_scenarios).forEach((scenarioId) => {
      const existing = requirementsByScenario.get(scenarioId) || {
        evidence_types: [],
        contracts: [],
        metadata: [],
      };
      existing.contracts = [...new Set([...existing.contracts, ...asArray(gate.required_contracts)])];
      existing.metadata = [...new Set([...existing.metadata, ...asArray(gate.required_metadata)])];
      requirementsByScenario.set(scenarioId, existing);
    });
  });

  return requirementsByScenario;
}

function isCrossEnvironmentBlocker(record, gatePolicy) {
  const verdict = record?.result?.verdict;
  return (
    verdict === gatePolicy.aggregation_rules?.environment_blocker_verdict
    || record.runtime_status === 'blocked'
    || record.release_effect === 'blocks_release'
  );
}

function eligibleEvidenceForScenario(record, scenarioId, targetEnv, gatePolicy) {
  if (record.scenario_id !== scenarioId) return false;
  if (record.environment === targetEnv) return true;
  return isCrossEnvironmentBlocker(record, gatePolicy);
}

function selectScenarioEvidence({ scenario, records, targetEnv, gatePolicy, context }) {
  const candidates = records.filter((record) => eligibleEvidenceForScenario(
    record,
    scenario.scenario_id,
    targetEnv,
    gatePolicy,
  ));

  const rejected = [];
  const valid = [];
  candidates.forEach((record) => {
    const classification = classifyEvidence(record, context);
    if (classification.valid) {
      valid.push(record);
    } else {
      rejected.push({
        evidence_id: record.evidence_id,
        scenario_id: record.scenario_id,
        environment: record.environment,
        reasons: classification.reasons,
      });
    }
  });

  valid.sort(compareEvidence);

  const exactEnvironment = valid.filter((record) => record.environment === targetEnv);
  const selected = exactEnvironment[0] || valid[0] || null;
  const selectionReason = selected
    ? (selected.environment === targetEnv ? 'latest_valid_target_environment_evidence' : 'latest_valid_cross_environment_blocker')
    : 'missing_valid_evidence';

  return {
    selected,
    rejected,
    candidate_count: candidates.length,
    valid_count: valid.length,
    selection_reason: selectionReason,
  };
}

function scenarioReadiness({ scenario, selection }) {
  const record = selection.selected;
  if (!record) {
    return {
      scenario_id: scenario.scenario_id,
      domain: scenario.domain,
      system: scenario.system,
      release_critical: scenario.release_critical === true,
      selected_evidence_id: null,
      evidence_environment: null,
      selection_reason: selection.selection_reason,
      contract_status: 'blocked',
      runtime_status: 'not_tested',
      result_verdict: 'BLOCKED',
      release_effect: 'blocks_release',
      blockers: ['missing_runtime_evidence'],
      why_not_pass: 'No valid evidence record matched the required build context.',
    };
  }

  const blockers = unique([
    ...asArray(record?.result?.blocker_ids),
    record.result?.verdict === 'BLOCKED' ? 'environment_blocker' : null,
    record.release_effect === 'blocks_release' ? 'release_effect_blocks_release' : null,
  ]);

  return {
    scenario_id: scenario.scenario_id,
    domain: scenario.domain,
    system: scenario.system,
    release_critical: scenario.release_critical === true,
    selected_evidence_id: record.evidence_id,
    evidence_environment: record.environment,
    selection_reason: selection.selection_reason,
    contract_status: record.contract_status,
    runtime_status: record.runtime_status,
    result_verdict: record.result?.verdict || 'INSUFFICIENT_EVIDENCE',
    release_effect: record.release_effect,
    blockers,
    why_not_pass: record.result?.verdict === 'PASS'
      ? ''
      : asArray(record.result?.observations).join(' '),
  };
}

function scenarioPassesGate(scenarioResult, gate) {
  if (!scenarioResult.selected_evidence_id) return false;
  if (gate.required_verdict && scenarioResult.result_verdict !== gate.required_verdict) return false;
  if (gate.required_runtime_status && scenarioResult.runtime_status !== gate.required_runtime_status) return false;
  if (gate.required_release_effect_after_evidence && scenarioResult.release_effect !== gate.required_release_effect_after_evidence) return false;
  if (scenarioResult.result_verdict !== 'PASS') return false;
  if (scenarioResult.release_effect === 'blocks_release') return false;
  if (scenarioResult.contract_status === 'blocked' || scenarioResult.runtime_status === 'blocked') return false;
  return true;
}

function evaluateGate({ gate, scenarioResultsById, gatePolicy }) {
  const requiredScenarioIds = asArray(gate.required_scenarios);
  const requiredEvidenceIds = asArray(gate.required_evidence_ids);
  const scenarioResults = requiredScenarioIds.map((scenarioId) => scenarioResultsById.get(scenarioId));
  const missingScenarioRefs = requiredScenarioIds.filter((scenarioId, index) => !scenarioResults[index]);
  const blockers = [];
  const reasons = [];

  if (missingScenarioRefs.length > 0) {
    blockers.push('missing_scenario_ref');
    reasons.push(`Gate references missing scenario(s): ${missingScenarioRefs.join(', ')}`);
  }

  scenarioResults.filter(Boolean).forEach((scenarioResult) => {
    if (!scenarioResult.selected_evidence_id) {
      blockers.push(...asArray(gate.blocker_ids_when_missing), 'missing_runtime_evidence');
      reasons.push(`${scenarioResult.scenario_id} has no valid evidence for the required build context.`);
      return;
    }

    if (requiredEvidenceIds.length > 0 && !requiredEvidenceIds.includes(scenarioResult.selected_evidence_id)) {
      blockers.push('required_evidence_id_not_selected');
      reasons.push(`${scenarioResult.scenario_id} selected ${scenarioResult.selected_evidence_id}, not one of required_evidence_ids.`);
      return;
    }

    blockers.push(...scenarioResult.blockers);

    if (scenarioResult.result_verdict === 'BLOCKED') {
      reasons.push(`${scenarioResult.scenario_id} is BLOCKED by ${scenarioResult.selected_evidence_id}.`);
    } else if (scenarioResult.result_verdict !== 'PASS') {
      reasons.push(`${scenarioResult.scenario_id} verdict is ${scenarioResult.result_verdict}.`);
    }

    if (scenarioResult.release_effect === 'blocks_release') {
      reasons.push(`${scenarioResult.scenario_id} release_effect blocks_release.`);
    }

    if (scenarioResult.contract_status === 'blocked') {
      reasons.push(`${scenarioResult.scenario_id} contract_status blocked.`);
    }

    if (scenarioResult.runtime_status === 'blocked') {
      reasons.push(`${scenarioResult.scenario_id} runtime_status blocked.`);
    }
  });

  const allRequiredScenariosPass = scenarioResults.length > 0
    && missingScenarioRefs.length === 0
    && blockers.length === 0
    && scenarioResults.every((scenarioResult) => scenarioPassesGate(scenarioResult, gate));

  const configuredBlocked = gate.release_status === 'blocked';
  const effectiveReleaseStatus = allRequiredScenariosPass && !configuredBlocked ? gate.release_status : 'blocked';
  const effectiveReleaseEffect = allRequiredScenariosPass && !configuredBlocked
    ? gate.release_effect
    : 'blocks_release';

  if (configuredBlocked && !allRequiredScenariosPass) {
    blockers.push(...asArray(gate.current_blockers), ...gatePolicy.default_blockers || []);
    reasons.push('Gate is configured fail-closed until prerequisite/runtime evidence clears it.');
  }

  if (gate.gate_type === 'prerequisite' && !allRequiredScenariosPass) {
    reasons.push('Prerequisite gate blocks candidate support until required scenario evidence is PASS.');
  }

  return {
    gate_id: gate.gate_id,
    gate_type: gate.gate_type || 'release',
    configured_release_status: gate.release_status,
    effective_release_status: effectiveReleaseStatus,
    effective_release_effect: effectiveReleaseEffect,
    required_scenarios: requiredScenarioIds,
    required_evidence_ids: requiredEvidenceIds,
    required_verdict: gate.required_verdict || 'PASS',
    required_runtime_status: gate.required_runtime_status || null,
    scenario_results: scenarioResults.filter(Boolean).map((scenarioResult) => ({
      scenario_id: scenarioResult.scenario_id,
      selected_evidence_id: scenarioResult.selected_evidence_id,
      result_verdict: scenarioResult.result_verdict,
      contract_status: scenarioResult.contract_status,
      runtime_status: scenarioResult.runtime_status,
      release_effect: scenarioResult.release_effect,
    })),
    blockers: unique(blockers),
    reasons: unique(reasons),
    candidate_rule: gate.candidate_rule || null,
  };
}

function writeJson(filePath, data) {
  mkdirSync(path.dirname(filePath), { recursive: true });
  writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, 'utf8');
}

function writeMarkdown(filePath, report) {
  mkdirSync(path.dirname(filePath), { recursive: true });
  const blockers = report.blockers.map((blocker) => `- ${blocker}`).join('\n') || '- none';
  const gates = report.gates.map((gate) => (
    `- ${gate.gate_id}: ${gate.effective_release_status} / ${gate.effective_release_effect}`
  )).join('\n');
  const scenarios = report.scenarios.map((scenario) => (
    `- ${scenario.scenario_id}: ${scenario.result_verdict} via ${scenario.selected_evidence_id || 'no valid evidence'}`
  )).join('\n');

  const text = [
    '# Domain 17 Stage 1 Release Readiness',
    '',
    `Report id: \`${report.report_id}\``,
    `Generated at: \`${report.generated_at}\``,
    `Target environment: \`${report.target_env}\``,
    `Release status: \`${report.summary.release_status}\``,
    `Release effect: \`${report.summary.release_effect}\``,
    `Readiness claim allowed: \`${report.summary.readiness_claim_allowed}\``,
    '',
    '## Scenarios',
    scenarios,
    '',
    '## Gates',
    gates,
    '',
    '## Blockers',
    blockers,
    '',
    '## Why Not Pass',
    report.why_not_pass,
    '',
    'Runtime touched: `false`',
    'Staging touched: `false`',
    'Lease required: `none`',
    '',
  ].join('\n');

  writeFileSync(filePath, text, 'utf8');
}

export function evaluateReleaseReadiness(options = {}) {
  const rootDir = path.resolve(options.rootDir || DEFAULT_QA_ROOT);
  const targetEnv = options.env || 'staging';
  const inputs = loadInputs(rootDir);
  const scenarios = asArray(inputs.scenarios.scenarios);
  const records = asArray(inputs.evidence.records);
  const gatePolicy = {
    ...asObject(inputs.gates.gate_policy),
    aggregation_rules: asObject(inputs.gates.gate_policy?.aggregation_rules),
    default_blockers: asArray(inputs.gates.gate_policy?.default_blockers),
  };
  const buildContext = asObject(inputs.gates.build_context);
  const requiredBuildFields = asArray(gatePolicy.required_build_context_fields)
    .filter((field) => typeof field === 'string');
  const maxAgeHours = Number(gatePolicy.evidence_max_age_hours);
  const nowMs = options.now ? Date.parse(options.now) : Date.now();
  const requirementsByScenario = buildScenarioRequirements(scenarios, inputs.gates.gates);

  if (!Number.isFinite(nowMs)) {
    throw new ReadinessError('INVALID_NOW', `cannot parse evaluation time ${options.now}`);
  }

  const context = {
    buildContext,
    requiredBuildFields,
    maxAgeHours,
    nowMs,
    requirementsByScenario,
  };

  const scenarioSelections = new Map();
  const rejectedEvidence = [];
  const scenarioResults = scenarios.map((scenario) => {
    const selection = selectScenarioEvidence({
      scenario,
      records,
      targetEnv,
      gatePolicy,
      context,
    });
    scenarioSelections.set(scenario.scenario_id, selection);
    rejectedEvidence.push(...selection.rejected);
    return scenarioReadiness({ scenario, selection });
  });

  const scenarioResultsById = new Map(scenarioResults.map((scenario) => [scenario.scenario_id, scenario]));
  const gates = asArray(inputs.gates.gates).map((gate) => evaluateGate({
    gate,
    scenarioResultsById,
    gatePolicy,
  }));

  const blockers = unique([
    ...scenarioResults.flatMap((scenario) => scenario.blockers),
    ...gates.flatMap((gate) => gate.blockers),
    ...rejectedEvidence.flatMap((entry) => entry.reasons.map((reason) => reason.code)),
  ]);

  const releaseBlocked = gates.some((gate) => (
    gate.effective_release_status === 'blocked'
    || gate.effective_release_effect === 'blocks_release'
  ));

  const contractStatusAggregate = maxRankStatus(
    scenarioResults.map((scenario) => scenario.contract_status),
    CONTRACT_STATUS_RANK,
    'blocked',
  );
  const runtimeStatusAggregate = maxRankStatus(
    scenarioResults.map((scenario) => scenario.runtime_status),
    RUNTIME_STATUS_RANK,
    'blocked',
  );
  const releaseStatusAggregate = releaseBlocked
    ? 'blocked'
    : maxRankStatus(gates.map((gate) => gate.effective_release_status), RELEASE_STATUS_RANK, 'blocked');

  const selectedEvidence = scenarioResults
    .filter((scenario) => scenario.selected_evidence_id)
    .map((scenario) => ({
      scenario_id: scenario.scenario_id,
      evidence_id: scenario.selected_evidence_id,
      environment: scenario.evidence_environment,
      selection_reason: scenario.selection_reason,
    }));

  const blockerSummary = blockers.length > 0
    ? `Stage 1 is not passable: ${blockers.join(', ')}.`
    : 'No blockers detected by evaluator.';

  return {
    schema_version: 1,
    report_id: `domain-17-stage-1-release-readiness-${targetEnv}`,
    generated_at: new Date(nowMs).toISOString(),
    command_contract: 'node tools/qa/reports/release-readiness.mjs --env <environment> [--root docs/qa] [--output <json>] [--markdown-output <md>] [--now <iso>] [--no-write]',
    target_env: targetEnv,
    workspace_root: REPO_ROOT,
    runtime_touched: false,
    staging_touched: false,
    lease_required: 'none',
    source_files: inputs.sourceFiles,
    build_context: buildContext,
    policy: {
      evidence_max_age_hours: gatePolicy.evidence_max_age_hours,
      required_build_context_fields: requiredBuildFields,
      aggregation_rules: gatePolicy.aggregation_rules,
      proof_requirements: Object.fromEntries(requirementsByScenario),
    },
    summary: {
      release_status: releaseStatusAggregate,
      release_effect: releaseBlocked ? 'blocks_release' : 'supports_candidate',
      readiness_claim_allowed: !releaseBlocked && blockers.length === 0,
      contract_status: contractStatusAggregate,
      runtime_status: runtimeStatusAggregate,
      selected_evidence_records: selectedEvidence.length,
      rejected_evidence_records: rejectedEvidence.length,
      gate_count: gates.length,
      scenario_count: scenarios.length,
    },
    evidence_selection: {
      latest_valid_per_build_context: selectedEvidence,
      rejected: rejectedEvidence,
    },
    scenarios: scenarioResults,
    gates,
    blockers,
    risks: [
      'Stage 1 remains non-authoritative until the Domain 17 QA source-of-truth cutover checklist completes.',
      'BLOCKED environment/prerequisite evidence is evaluator input only and does not prove product runtime behavior.',
    ],
    why_not_pass: releaseBlocked
      ? blockerSummary
      : 'All configured gates passed evaluator checks.',
  };
}

function runCli() {
  try {
    const args = parseArgs(process.argv.slice(2));
    const report = evaluateReleaseReadiness({
      rootDir: args.root,
      env: args.env,
      now: args.now,
    });

    const output = args.output || path.join(DEFAULT_REPORT_DIR, `release-readiness-${args.env}.json`);
    const markdownOutput = args.markdownOutput || path.join(DEFAULT_REPORT_DIR, `release-readiness-${args.env}.md`);
    if (args.write) {
      writeJson(output, report);
      writeMarkdown(markdownOutput, report);
    }

    console.log(`release readiness ${args.env}: ${report.summary.release_status} (${report.summary.release_effect})`);
    console.log(`selected evidence records: ${report.summary.selected_evidence_records}`);
    console.log(`blockers: ${report.blockers.length ? report.blockers.join(', ') : 'none'}`);
    if (args.write) {
      console.log(`wrote ${repoRelative(output)}`);
      console.log(`wrote ${repoRelative(markdownOutput)}`);
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`release readiness evaluator failed: ${message}`);
    process.exitCode = 1;
  }
}

if (process.argv[1] === __filename) {
  runCli();
}
