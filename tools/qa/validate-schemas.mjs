#!/usr/bin/env node

import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');

const ENUMS = {
  contract_status: ['draft', 'proposed', 'approved', 'blocked'],
  runtime_status: ['not_tested', 'loaded', 'partial', 'verified', 'blocked'],
  release_status: ['blocked', 'candidate', 'released'],
  verdict: ['PASS', 'FAIL', 'BLOCKED', 'INSUFFICIENT_EVIDENCE'],
  release_effect: ['none', 'supports_candidate', 'blocks_release', 'clears_blocker'],
};

const REQUIRED_FILES = {
  evidence: 'runtime-evidence-registry.yaml',
  scenarios: 'e2e-scenarios.yaml',
  gates: 'release-gates.yaml',
  matrix: 'regression-matrix.yaml',
};

class ValidationError extends Error {
  constructor(code, message) {
    super(`${code}: ${message}`);
    this.code = code;
  }
}

function loadYaml(filePath) {
  if (!existsSync(filePath)) {
    throw new ValidationError('MISSING_FILE', `${filePath} does not exist`);
  }

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
    throw new ValidationError('YAML_PARSE_FAILED', `${filePath}: ${detail}`);
  }

  return JSON.parse(result.stdout);
}

function requireObject(value, location) {
  if (!value || Array.isArray(value) || typeof value !== 'object') {
    throw new ValidationError('MISSING_REQUIRED_FIELD', `${location} must be an object`);
  }
}

function requireArray(value, location) {
  if (!Array.isArray(value)) {
    throw new ValidationError('MISSING_REQUIRED_FIELD', `${location} must be an array`);
  }
}

function requireField(object, key, location) {
  if (!(key in object) || object[key] === null || object[key] === '') {
    throw new ValidationError('MISSING_REQUIRED_FIELD', `${location}.${key} is required`);
  }
  return object[key];
}

function requireEnum(value, allowed, location) {
  if (!allowed.includes(value)) {
    throw new ValidationError('UNKNOWN_ENUM', `${location} has unknown value ${JSON.stringify(value)}`);
  }
}

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function requireSchemaVersion(root, location) {
  requireObject(root, location);
  if (root.schema_version !== 1) {
    throw new ValidationError('UNSUPPORTED_SCHEMA_VERSION', `${location}.schema_version must be 1`);
  }
}

function validateScenarioRegistry(root) {
  requireSchemaVersion(root, REQUIRED_FILES.scenarios);
  requireArray(root.scenarios, `${REQUIRED_FILES.scenarios}.scenarios`);

  const scenarioIds = new Set();
  root.scenarios.forEach((scenario, index) => {
    const location = `${REQUIRED_FILES.scenarios}.scenarios[${index}]`;
    requireObject(scenario, location);
    const scenarioId = requireField(scenario, 'scenario_id', location);
    [
      'domain',
      'system',
      'class',
      'release_critical',
      'contract_refs',
      'acceptance_criteria',
      'required_evidence_types',
      'environments',
      'preconditions',
      'steps',
      'assertions',
      'failure_classification',
    ].forEach((key) => requireField(scenario, key, location));
    ['contract_refs', 'acceptance_criteria', 'required_evidence_types', 'environments', 'preconditions', 'steps', 'assertions']
      .forEach((key) => requireArray(scenario[key], `${location}.${key}`));
    requireObject(scenario.failure_classification, `${location}.failure_classification`);
    ['blocked_when', 'fail_when', 'insufficient_evidence_when']
      .forEach((key) => requireArray(requireField(scenario.failure_classification, key, `${location}.failure_classification`), `${location}.failure_classification.${key}`));

    if (scenarioIds.has(scenarioId)) {
      throw new ValidationError('DUPLICATE_ID', `${location}.scenario_id duplicates ${scenarioId}`);
    }
    scenarioIds.add(scenarioId);
  });

  return scenarioIds;
}

function validateScenarioRegistryMap(root) {
  const scenariosById = new Map();
  root.scenarios.forEach((scenario) => scenariosById.set(scenario.scenario_id, scenario));
  return scenariosById;
}

function arrayIncludesAll(actual, required, location, code) {
  const actualSet = new Set(actual);
  const missing = required.filter((value) => !actualSet.has(value));
  if (missing.length > 0) {
    throw new ValidationError(code, `${location} missing ${missing.join(', ')}`);
  }
}

function fixtureLikeValue(value) {
  return typeof value === 'string'
    && /(^|[/\\])(tools[/\\]qa[/\\]tests|fixtures)([/\\]|$)|(^|[-_/])fixture([-_/]|$)|test-only/i.test(value);
}

function validateReleaseGradePromotion(record, location) {
  if (record.contract_status !== 'approved') {
    throw new ValidationError('PROMOTION_CONTRACT_STATUS_NOT_APPROVED', `${location}.contract_status must be approved for promotion records`);
  }

  const environmentControl = record.execution?.environment_control || {};
  if (environmentControl.runtime_touched !== true) {
    throw new ValidationError('PROMOTION_RUNTIME_NOT_TOUCHED', `${location}.execution.environment_control.runtime_touched must be true for promotion records`);
  }

  const lease = environmentControl.lease;
  if (typeof lease !== 'string' || !/^dev-[a-z0-9][a-z0-9_-]*$/i.test(lease)) {
    throw new ValidationError('PROMOTION_MISSING_DEV_LEASE', `${location}.execution.environment_control.lease must be a concrete dev-* lease`);
  }

  const rollback = environmentControl.rollback || {};
  const provenanceValues = [
    record.evidence_id,
    record.test_type,
    record.execution?.runner,
    record.execution?.executed_by,
    record.execution?.backend,
    record.execution?.docker_volume_id,
    ...asArray(record.execution?.log_paths),
    ...asArray(rollback.proof_paths),
    ...asArray(record.artifacts).flatMap((artifact) => [artifact.path, artifact.kind]),
  ];
  const fixtureValues = provenanceValues.filter(fixtureLikeValue);
  if (fixtureValues.length > 0) {
    throw new ValidationError('PROMOTION_FIXTURE_OR_TEST_PROVENANCE', `${location} has fixture/test-only provenance ${fixtureValues.join(', ')}`);
  }
}

function validatePromotionRecord(record, location, scenario, requiredContracts, requiredMetadata) {
  const promotes = record.result?.verdict === 'PASS'
    || record.runtime_status === 'verified'
    || record.release_effect === 'supports_candidate';
  if (!promotes) return;
  validateReleaseGradePromotion(record, location);

  ['evidence_types', 'contract_refs'].forEach((key) => {
    requireField(record, key, location);
    requireArray(record[key], `${location}.${key}`);
  });
  requireObject(requireField(record, 'metadata', location), `${location}.metadata`);

  arrayIncludesAll(
    record.evidence_types,
    scenario.required_evidence_types,
    `${location}.evidence_types`,
    'MISSING_REQUIRED_EVIDENCE_TYPE',
  );
  arrayIncludesAll(
    record.contract_refs,
    [...new Set([...scenario.contract_refs, ...requiredContracts])],
    `${location}.contract_refs`,
    'MISSING_REQUIRED_CONTRACT',
  );

  const missingMetadata = requiredMetadata.filter((key) => !(key in record.metadata) || record.metadata[key] === null || record.metadata[key] === '');
  if (missingMetadata.length > 0) {
    throw new ValidationError('MISSING_REQUIRED_METADATA', `${location}.metadata missing ${missingMetadata.join(', ')}`);
  }
}

function validateEvidenceRegistry(root) {
  requireSchemaVersion(root, REQUIRED_FILES.evidence);
  requireArray(root.records, `${REQUIRED_FILES.evidence}.records`);

  const evidenceIds = new Set();
  root.records.forEach((record, index) => {
    const location = `${REQUIRED_FILES.evidence}.records[${index}]`;
    requireObject(record, location);
    const evidenceId = requireField(record, 'evidence_id', location);
    [
      'scenario_id',
      'domain',
      'system',
      'environment',
      'test_type',
      'contract_status',
      'runtime_status',
      'release_effect',
      'preconditions',
      'steps',
      'assertions',
      'artifacts',
      'build',
      'execution',
      'result',
    ].forEach((key) => requireField(record, key, location));

    requireEnum(record.contract_status, ENUMS.contract_status, `${location}.contract_status`);
    requireEnum(record.runtime_status, ENUMS.runtime_status, `${location}.runtime_status`);
    if ('release_status' in record) {
      requireEnum(record.release_status, ENUMS.release_status, `${location}.release_status`);
    }
    requireEnum(record.release_effect, ENUMS.release_effect, `${location}.release_effect`);

    ['preconditions', 'steps', 'assertions', 'artifacts'].forEach((key) => {
      requireArray(record[key], `${location}.${key}`);
    });

    validateBuildContext(record.build, `${location}.build`);
    validateExecution(record.execution, `${location}.execution`);
    requireObject(record.result, `${location}.result`);
    requireEnum(requireField(record.result, 'verdict', `${location}.result`), ENUMS.verdict, `${location}.result.verdict`);

    if (evidenceIds.has(evidenceId)) {
      throw new ValidationError('DUPLICATE_ID', `${location}.evidence_id duplicates ${evidenceId}`);
    }
    evidenceIds.add(evidenceId);
  });

  return evidenceIds;
}

function validateBuildContext(build, location) {
  requireObject(build, location);
  ['paper', 'plugin_manifest_hash', 'commit'].forEach((key) => requireField(build, key, location));
}

function validateExecution(execution, location) {
  requireObject(execution, location);
  ['runner', 'started_at', 'finished_at', 'executed_by', 'log_paths', 'environment_control'].forEach((key) => requireField(execution, key, location));
  requireArray(execution.log_paths, `${location}.log_paths`);
  requireObject(execution.environment_control, `${location}.environment_control`);
  ['lease', 'workspace_root', 'runtime_touched', 'staging_touched', 'rollback'].forEach((key) => {
    requireField(execution.environment_control, key, `${location}.environment_control`);
  });
  if (typeof execution.environment_control.runtime_touched !== 'boolean') {
    throw new ValidationError('MISSING_REQUIRED_FIELD', `${location}.environment_control.runtime_touched must be boolean`);
  }
  if (typeof execution.environment_control.staging_touched !== 'boolean') {
    throw new ValidationError('MISSING_REQUIRED_FIELD', `${location}.environment_control.staging_touched must be boolean`);
  }
  requireObject(execution.environment_control.rollback, `${location}.environment_control.rollback`);
  ['method', 'proof_paths'].forEach((key) => requireField(execution.environment_control.rollback, key, `${location}.environment_control.rollback`));
  requireArray(execution.environment_control.rollback.proof_paths, `${location}.environment_control.rollback.proof_paths`);
}

function validateReleaseGates(root, scenarioIds, evidenceIds, records) {
  requireSchemaVersion(root, REQUIRED_FILES.gates);
  requireObject(root.gate_policy, `${REQUIRED_FILES.gates}.gate_policy`);
  requireObject(root.build_context, `${REQUIRED_FILES.gates}.build_context`);
  validateBuildContext(root.build_context, `${REQUIRED_FILES.gates}.build_context`);
  requireArray(root.gates, `${REQUIRED_FILES.gates}.gates`);

  root.gates.forEach((gate, index) => {
    const location = `${REQUIRED_FILES.gates}.gates[${index}]`;
    requireObject(gate, location);
    ['gate_id', 'release_status', 'release_effect', 'required_scenarios'].forEach((key) => requireField(gate, key, location));
    requireEnum(gate.release_status, ENUMS.release_status, `${location}.release_status`);
    requireEnum(gate.release_effect, ENUMS.release_effect, `${location}.release_effect`);
    requireArray(gate.required_scenarios, `${location}.required_scenarios`);

    gate.required_scenarios.forEach((scenarioId) => {
      if (!scenarioIds.has(scenarioId)) {
        throw new ValidationError('DANGLING_SCENARIO_REF', `${location}.required_scenarios references missing scenario ${scenarioId}`);
      }
    });

    if ('required_evidence_ids' in gate) {
      requireArray(gate.required_evidence_ids, `${location}.required_evidence_ids`);
      gate.required_evidence_ids.forEach((evidenceId) => {
        if (!evidenceIds.has(evidenceId)) {
          throw new ValidationError('DANGLING_EVIDENCE_REF', `${location}.required_evidence_ids references missing evidence ${evidenceId}`);
        }
      });
    }
  });

  records.forEach((record, index) => {
    ['commit', 'paper', 'plugin_manifest_hash'].forEach((key) => {
      if (record.build[key] !== root.build_context[key]) {
        throw new ValidationError(
          'STALE_EVIDENCE',
          `${REQUIRED_FILES.evidence}.records[${index}].build.${key} does not match ${REQUIRED_FILES.gates}.build_context.${key}`,
        );
      }
    });
  });
}

function validatePromotionEvidenceContracts(records, scenariosById, gates) {
  const requirementsByScenario = new Map();

  gates.forEach((gate) => {
    asArray(gate.required_scenarios).forEach((scenarioId) => {
      const existing = requirementsByScenario.get(scenarioId) || {
        contracts: new Set(),
        metadata: new Set(),
      };
      asArray(gate.required_contracts).forEach((contractRef) => existing.contracts.add(contractRef));
      asArray(gate.required_metadata).forEach((metadataKey) => existing.metadata.add(metadataKey));
      requirementsByScenario.set(scenarioId, existing);
    });
  });

  records.forEach((record, index) => {
    const scenario = scenariosById.get(record.scenario_id);
    if (!scenario) return;
    const requirements = requirementsByScenario.get(record.scenario_id) || {
      contracts: new Set(),
      metadata: new Set(),
    };
    validatePromotionRecord(
      record,
      `${REQUIRED_FILES.evidence}.records[${index}]`,
      scenario,
      [...requirements.contracts],
      [...requirements.metadata],
    );
  });
}

function validateRegressionMatrix(root, scenarioIds) {
  requireSchemaVersion(root, REQUIRED_FILES.matrix);
  requireArray(root.entries, `${REQUIRED_FILES.matrix}.entries`);

  root.entries.forEach((entry, index) => {
    const location = `${REQUIRED_FILES.matrix}.entries[${index}]`;
    requireObject(entry, location);
    ['matrix_id', 'scenario_id', 'release_status', 'release_effect', 'coverage_level'].forEach((key) => requireField(entry, key, location));
    requireEnum(entry.release_status, ENUMS.release_status, `${location}.release_status`);
    requireEnum(entry.release_effect, ENUMS.release_effect, `${location}.release_effect`);
    if (!scenarioIds.has(entry.scenario_id)) {
      throw new ValidationError('DANGLING_SCENARIO_REF', `${location}.scenario_id references missing scenario ${entry.scenario_id}`);
    }
  });
}

function validateCrossReferences(records, scenarioIds) {
  records.forEach((record, index) => {
    if (!scenarioIds.has(record.scenario_id)) {
      throw new ValidationError('DANGLING_SCENARIO_REF', `${REQUIRED_FILES.evidence}.records[${index}].scenario_id references missing scenario ${record.scenario_id}`);
    }
  });
}

export function validateQaSchemas(rootDir = path.join(REPO_ROOT, 'docs/qa')) {
  const loaded = Object.fromEntries(
    Object.entries(REQUIRED_FILES).map(([key, fileName]) => [key, loadYaml(path.join(rootDir, fileName))]),
  );

  const scenarioIds = validateScenarioRegistry(loaded.scenarios);
  const scenariosById = validateScenarioRegistryMap(loaded.scenarios);
  const evidenceIds = validateEvidenceRegistry(loaded.evidence);
  validateCrossReferences(loaded.evidence.records, scenarioIds);
  validateReleaseGates(loaded.gates, scenarioIds, evidenceIds, loaded.evidence.records);
  validatePromotionEvidenceContracts(loaded.evidence.records, scenariosById, loaded.gates.gates);
  validateRegressionMatrix(loaded.matrix, scenarioIds);

  return {
    ok: true,
    files: Object.values(REQUIRED_FILES).map((fileName) => path.join(rootDir, fileName)),
    scenarios: scenarioIds.size,
    evidence_records: evidenceIds.size,
    gates: loaded.gates.gates.length,
    matrix_entries: loaded.matrix.entries.length,
  };
}

function runCli() {
  const rootDir = process.argv[2] ? path.resolve(process.argv[2]) : path.join(REPO_ROOT, 'docs/qa');
  try {
    const result = validateQaSchemas(rootDir);
    console.log(`QA schema validation passed: ${result.scenarios} scenarios, ${result.evidence_records} evidence records, ${result.gates} gates, ${result.matrix_entries} matrix entries.`);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`QA schema validation failed: ${message}`);
    process.exitCode = 1;
  }
}

if (process.argv[1] === __filename) {
  runCli();
}
