#!/usr/bin/env node

import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');
const DEFAULT_SUBJECT = 'item.weapon.sword.ashen_vanguard';
const DEFAULT_ENVIRONMENT = 'staging';
const VALID_VERDICTS = new Set(['pass', 'fail']);

class AuditError extends Error {
  constructor(code, message) {
    super(`${code}: ${message}`);
    this.code = code;
  }
}

function parseArgs(argv) {
  const args = {
    repoRoot: process.env.MC_CONTENT_ROOT ? path.resolve(process.env.MC_CONTENT_ROOT) : REPO_ROOT,
    subjectId: DEFAULT_SUBJECT,
    environment: DEFAULT_ENVIRONMENT,
  };

  for (let index = 2; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith('--')) {
      throw new AuditError('ARGUMENT_ERROR', `unexpected argument ${token}`);
    }

    const key = token.slice(2);
    if (key === 'help') {
      args.help = true;
      continue;
    }

    const value = argv[index + 1];
    if (!value || value.startsWith('--')) {
      throw new AuditError('ARGUMENT_ERROR', `missing value for --${key}`);
    }

    if (key === 'repo-root') args.repoRoot = path.resolve(value);
    else if (key === 'subject-id') args.subjectId = value;
    else if (key === 'build-id') args.buildId = value;
    else if (key === 'environment') args.environment = value;
    else if (key === 'reload-proof') args.reloadProofPath = path.resolve(value);
    else if (key === 'effective-runtime') args.effectiveRuntimePath = path.resolve(value);
    else if (key === 'output') args.outputPath = path.resolve(value);
    else throw new AuditError('ARGUMENT_ERROR', `unknown option --${key}`);

    index += 1;
  }

  if (args.help) {
    return args;
  }

  for (const required of ['buildId', 'reloadProofPath', 'effectiveRuntimePath', 'outputPath']) {
    if (!args[required]) {
      throw new AuditError('ARGUMENT_ERROR', `missing required option ${required}`);
    }
  }

  return args;
}

function loadYaml(filePath) {
  if (!existsSync(filePath)) {
    throw new AuditError('MISSING_FILE', `${filePath} does not exist`);
  }

  const parser = [
    'import json, sys, yaml',
    'from datetime import date, datetime',
    'def default(value):',
    '    if isinstance(value, (date, datetime)):',
    '        return value.isoformat()',
    '    raise TypeError(type(value).__name__)',
    'with open(sys.argv[1], "r", encoding="utf-8") as handle:',
    '    data = yaml.safe_load(handle)',
    'print(json.dumps(data if data is not None else {}, default=default))',
  ].join('\n');

  const result = spawnSync('python3', ['-c', parser, filePath], {
    encoding: 'utf8',
    maxBuffer: 1024 * 1024,
  });

  if (result.status !== 0) {
    const detail = (result.stderr || result.stdout || '').trim();
    throw new AuditError('YAML_PARSE_FAILED', `${filePath}: ${detail}`);
  }

  return JSON.parse(result.stdout);
}

function relative(repoRoot, filePath) {
  return path.relative(repoRoot, filePath).split(path.sep).join('/');
}

function sha256(text) {
  return `sha256:${crypto.createHash('sha256').update(text, 'utf8').digest('hex')}`;
}

function readText(filePath) {
  if (!existsSync(filePath)) {
    throw new AuditError('MISSING_FILE', `${filePath} does not exist`);
  }
  return readFileSync(filePath, 'utf8');
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function sortValue(value) {
  if (Array.isArray(value)) {
    return value.map(sortValue);
  }
  if (isObject(value)) {
    return Object.keys(value)
      .sort()
      .reduce((acc, key) => {
        acc[key] = sortValue(value[key]);
        return acc;
      }, {});
  }
  return value;
}

function canonicalString(value) {
  return JSON.stringify(sortValue(value));
}

function canonicalNumberLike(value) {
  if (typeof value === 'number') {
    return value;
  }
  if (typeof value === 'string' && value.trim() !== '') {
    const numeric = Number(value);
    if (Number.isFinite(numeric)) {
      return numeric;
    }
  }
  return value;
}

function formatScalar(value, key, numericKeys) {
  const normalized = numericKeys.has(key) ? canonicalNumberLike(value) : value;

  if (typeof normalized === 'string') {
    return `'${normalized.replaceAll("'", "''")}'`;
  }
  if (typeof normalized === 'boolean') {
    return normalized ? 'true' : 'false';
  }
  return String(normalized);
}

function renderList(key, values, numericKeys) {
  return [
    `  ${key}:`,
    ...values.map((value) => `    - ${formatScalar(value, key, numericKeys)}`),
  ];
}

function renderSection(sectionKey, section, numericKeys, preferredOrder = []) {
  const lines = [`${sectionKey}:`];
  const rendered = new Set();
  const orderedKeys = [...preferredOrder, ...Object.keys(section).filter((key) => !preferredOrder.includes(key))];

  for (const key of orderedKeys) {
    if (!(key in section) || rendered.has(key)) {
      continue;
    }

    const value = section[key];
    rendered.add(key);

    if (Array.isArray(value)) {
      lines.push(...renderList(key, value, numericKeys));
      continue;
    }

    lines.push(`  ${key}: ${formatScalar(value, key, numericKeys)}`);
  }

  return `${lines.join('\n')}\n`;
}

function valuesEqual(left, right, key, numericKeys) {
  if (numericKeys.has(key)) {
    return canonicalNumberLike(left) === canonicalNumberLike(right);
  }
  return canonicalString(left) === canonicalString(right);
}

function assertObject(value, message) {
  if (!isObject(value)) {
    throw new AuditError('INVALID_SHAPE', message);
  }
}

function findRegistryRecord(repoRoot, subjectId) {
  const registryPath = path.join(repoRoot, 'registry/content/items', `${subjectId}.yaml`);
  const record = loadYaml(registryPath);
  if (record.id !== subjectId) {
    throw new AuditError('SUBJECT_MISMATCH', `${registryPath} does not match subject ${subjectId}`);
  }
  return { registryPath, record };
}

function findOutputPath(record, kind, repoRoot) {
  const outputs = Array.isArray(record.output_manifest?.outputs) ? record.output_manifest.outputs : [];
  const entry = outputs.find((candidate) => candidate.kind === kind);
  if (!entry || typeof entry.path !== 'string') {
    throw new AuditError('MISSING_OUTPUT', `registry record is missing output_manifest kind ${kind}`);
  }
  return path.join(repoRoot, entry.path);
}

function extractSection(filePath, sectionKey) {
  const root = loadYaml(filePath);
  assertObject(root, `${filePath} must parse to a YAML object`);
  const section = root[sectionKey];
  if (!isObject(section)) {
    throw new AuditError('MISSING_SECTION', `${filePath} is missing section ${sectionKey}`);
  }
  return section;
}

function extractRawSectionText(filePath, sectionKey) {
  const lines = readText(filePath).split('\n');
  const startIndex = lines.findIndex((line) => line === `${sectionKey}:`);
  if (startIndex === -1) {
    throw new AuditError('MISSING_SECTION', `${filePath} is missing section ${sectionKey}`);
  }

  const sectionLines = [`${sectionKey}:`];
  for (let index = startIndex + 1; index < lines.length; index += 1) {
    const line = lines[index];
    if (line.length > 0 && !line.startsWith(' ')) {
      break;
    }
    sectionLines.push(line);
  }

  return `${sectionLines.join('\n').replace(/\n+$/, '')}\n`;
}

function loadReloadProof(filePath, expected) {
  const proof = loadYaml(filePath);
  assertObject(proof, `${filePath} must parse to an object`);

  const requiredFields = [
    'schema_version',
    'proof_kind',
    'plugin',
    'environment',
    'build_id',
    'target_file',
    'section_key',
    'target_tuple',
    'status',
    'captured_at',
  ];
  for (const key of requiredFields) {
    if (!(key in proof)) {
      throw new AuditError('INVALID_RELOAD_PROOF', `${filePath} is missing ${key}`);
    }
  }

  if (proof.proof_kind !== 'mmoitems-reload-boundary') {
    throw new AuditError('INVALID_RELOAD_PROOF', `${filePath} has unsupported proof_kind ${proof.proof_kind}`);
  }
  if (proof.plugin !== expected.plugin) {
    throw new AuditError('INVALID_RELOAD_PROOF', `${filePath} plugin ${proof.plugin} != ${expected.plugin}`);
  }
  if (proof.environment !== expected.environment) {
    throw new AuditError(
      'INVALID_RELOAD_PROOF',
      `${filePath} environment ${proof.environment} != ${expected.environment}`,
    );
  }
  if (proof.build_id !== expected.buildId) {
    throw new AuditError('INVALID_RELOAD_PROOF', `${filePath} build_id ${proof.build_id} != ${expected.buildId}`);
  }
  if (proof.target_file !== expected.targetFile) {
    throw new AuditError(
      'INVALID_RELOAD_PROOF',
      `${filePath} target_file ${proof.target_file} != ${expected.targetFile}`,
    );
  }
  if (proof.section_key !== expected.sectionKey) {
    throw new AuditError(
      'INVALID_RELOAD_PROOF',
      `${filePath} section_key ${proof.section_key} != ${expected.sectionKey}`,
    );
  }
  if (proof.target_tuple !== expected.targetTuple) {
    throw new AuditError(
      'INVALID_RELOAD_PROOF',
      `${filePath} target_tuple ${proof.target_tuple} != ${expected.targetTuple}`,
    );
  }
  if (proof.status !== 'success') {
    throw new AuditError('INVALID_RELOAD_PROOF', `${filePath} status ${proof.status} is not success`);
  }

  return proof;
}

function runReverseBijection(repoRoot) {
  const validatorPath = path.join(repoRoot, 'tools/qa/validate-reverse-bijection.mjs');
  const result = spawnSync('node', [validatorPath, repoRoot], {
    cwd: repoRoot,
    encoding: 'utf8',
    maxBuffer: 1024 * 1024,
  });

  return {
    ok: result.status === 0,
    detail: (result.stdout || result.stderr || '').trim(),
  };
}

function loadSectionContract(repoRoot) {
  const filePath = path.join(repoRoot, 'docs/production/contracts/mmoitems-item-section-schema.yaml');
  const contract = loadYaml(filePath);
  const keys = isObject(contract.keys) ? contract.keys : {};
  const generatorOwnedKeys = new Set();
  const requiredGeneratorKeys = new Set();
  const pluginDerivedWhitelist = new Set(Array.isArray(contract.plugin_derived_whitelist_v1) ? contract.plugin_derived_whitelist_v1 : []);
  const numericKeys = new Set();

  for (const [key, definition] of Object.entries(keys)) {
    if (!isObject(definition)) {
      continue;
    }
    if (definition.owner === 'generator') {
      generatorOwnedKeys.add(key);
      if (definition.required === true) {
        requiredGeneratorKeys.add(key);
      }
      if (definition.type === 'number' || definition.type === 'integer') {
        numericKeys.add(key);
      }
    }
    if (definition.owner === 'plugin-derived' && (definition.type === 'number' || definition.type === 'integer')) {
      numericKeys.add(key);
    }
  }

  return {
    filePath,
    contract,
    keys,
    generatorOwnedKeys,
    requiredGeneratorKeys,
    pluginDerivedWhitelist,
    numericKeys,
  };
}

function validateCommittedTarget({ projection, committed, sectionContract, sectionKey }) {
  const assertions = ['section-present', 'exact-runtime-id-match'];
  const failures = [];

  if (!sectionKey || typeof sectionKey !== 'string') {
    failures.push('section-key-missing');
  }

  for (const key of sectionContract.requiredGeneratorKeys) {
    if (!(key in committed)) {
      failures.push(`missing-required-generator-key:${key}`);
    }
  }
  if (!failures.some((entry) => entry.startsWith('missing-required-generator-key:'))) {
    assertions.push('required-generator-keys-present');
  }

  let conflictDetected = false;
  for (const key of sectionContract.generatorOwnedKeys) {
    if (!(key in projection) || !(key in committed)) {
      continue;
    }
    if (!valuesEqual(projection[key], committed[key], key, sectionContract.numericKeys)) {
      conflictDetected = true;
      failures.push(`generator-owned-conflict:${key}`);
    }
  }

  for (const key of sectionContract.pluginDerivedWhitelist) {
    if (!(key in committed)) {
      continue;
    }
    const definition = sectionContract.keys[key];
    if (definition?.allowed_values && !definition.allowed_values.includes(committed[key])) {
      conflictDetected = true;
      failures.push(`plugin-derived-conflict:${key}`);
    }
  }

  if (!conflictDetected) {
    assertions.push('no-conflict-on-generator-owned-or-whitelisted-plugin-derived-keys');
  }

  return { assertions, failures };
}

function validateEffectiveRuntime({ committed, effective, sectionContract }) {
  const assertions = [];
  const failures = [];

  for (const key of sectionContract.generatorOwnedKeys) {
    if (!(key in committed)) {
      continue;
    }
    if (!(key in effective)) {
      failures.push(`generator-owned-key-missing:${key}`);
      continue;
    }
    if (!valuesEqual(committed[key], effective[key], key, sectionContract.numericKeys)) {
      failures.push(`generator-owned-value-changed:${key}`);
    }
  }

  if (!failures.some((entry) => entry.startsWith('generator-owned-'))) {
    assertions.push('generator-owned-values-preserved');
  }

  const unexpectedKeys = [];
  for (const key of Object.keys(effective)) {
    if (key in committed) {
      continue;
    }
    if (!sectionContract.pluginDerivedWhitelist.has(key)) {
      unexpectedKeys.push(key);
      continue;
    }

    const definition = sectionContract.keys[key];
    if (definition?.allowed_values && !definition.allowed_values.includes(effective[key])) {
      failures.push(`plugin-derived-value-outside-contract:${key}`);
      continue;
    }
  }

  if (unexpectedKeys.length > 0) {
    failures.push(`unknown-key-added:${unexpectedKeys.join(',')}`);
  }

  if (
    unexpectedKeys.length === 0 &&
    !failures.some((entry) => entry.startsWith('plugin-derived-value-outside-contract:'))
  ) {
    assertions.push('plugin-derived-whitelist-v1-within-normalization-contract');
  }

  return { assertions, failures };
}

function buildEvidence({
  repoRoot,
  outputPath,
  args,
  record,
  registryPath,
  projection,
  committed,
  effective,
  reverseBijection,
  reloadProof,
  committedResult,
  effectiveResult,
  sectionContract,
  projectionPath,
  targetPath,
}) {
  const runtimeIdentity = record.runtime_identity;
  const preferredOrder = Object.keys(projection);
  const generatedProjectionDigest = sha256(readText(projectionPath));
  const committedTargetDigest = sha256(extractRawSectionText(targetPath, runtimeIdentity.section_key));
  const effectiveRuntimeDigest = sha256(
    renderSection(runtimeIdentity.section_key, effective, sectionContract.numericKeys, preferredOrder),
  );
  const assertions = [];
  const failures = [];

  if (reverseBijection.ok) assertions.push('reverse-bijection-green');
  else failures.push('reverse-bijection-red');

  assertions.push('reload-succeeded');

  if (record.runtime_identity.generated_projection_digest === generatedProjectionDigest) {
    assertions.push('generated-projection-digest-match');
  } else {
    failures.push('generated-projection-digest-mismatch');
  }

  if (record.runtime_identity.committed_target_digest === committedTargetDigest) {
    assertions.push('committed-target-digest-match');
  } else {
    failures.push('committed-target-digest-mismatch');
  }

  assertions.push(...committedResult.assertions, ...effectiveResult.assertions);
  failures.push(...committedResult.failures, ...effectiveResult.failures);

  const verdict = failures.length === 0 ? 'pass' : 'fail';
  if (!VALID_VERDICTS.has(verdict)) {
    throw new AuditError('INTERNAL_ERROR', `unsupported verdict ${verdict}`);
  }

  return {
    schema_version: 1,
    evidence_id: `runtime-load.${record.id}.${args.buildId}`,
    gate: 'runtime_loaded',
    producer_domain: 'domain-18',
    producer_tool: 'tools/qa/audit-mmoitems-runtime-load-pilot.mjs',
    verdict,
    environment: args.environment,
    build_id: args.buildId,
    subject_id: record.id,
    content_type: record.content_type,
    source_path: record.source_path,
    subject_digest: record.digests.subject_digest,
    dependency_digest: record.digests.dependency_digest,
    runtime_plugin: runtimeIdentity.plugin,
    target_file: runtimeIdentity.target_file,
    section_key: runtimeIdentity.section_key,
    generated_projection_digest: generatedProjectionDigest,
    committed_target_digest: committedTargetDigest,
    effective_runtime_digest: effectiveRuntimeDigest,
    assertions,
    captured_at: new Date().toISOString(),
    artifact_path: relative(repoRoot, outputPath),
    evidence_contract: relative(repoRoot, path.join(repoRoot, 'docs/production/evidence-envelope.yaml')),
    reload_boundary: {
      proof_path: relative(repoRoot, args.reloadProofPath),
      proof_kind: reloadProof.proof_kind,
      captured_at: reloadProof.captured_at,
      status: reloadProof.status,
    },
    reverse_bijection: {
      ok: reverseBijection.ok,
      detail: reverseBijection.detail,
    },
    source_runtime_diff: {
      generated_projection_matches_registry: record.runtime_identity.generated_projection_digest === generatedProjectionDigest,
      committed_target_matches_registry: record.runtime_identity.committed_target_digest === committedTargetDigest,
    },
    comparison: {
      generated_projection: {
        path: relative(repoRoot, findOutputPath(record, 'runtime_projection', repoRoot)),
      },
      committed_target: {
        path: runtimeIdentity.target_file,
        assertions: committedResult.assertions,
        failures: committedResult.failures,
      },
      effective_runtime: {
        path: relative(repoRoot, args.effectiveRuntimePath),
        assertions: effectiveResult.assertions,
        failures: effectiveResult.failures,
      },
    },
    registry_record: relative(repoRoot, registryPath),
  };
}

export function auditRuntimeLoad(rawArgs = process.argv) {
  const args = parseArgs(rawArgs);
  if (args.help) {
    process.stdout.write(
      [
        'Usage:',
        '  node tools/qa/audit-mmoitems-runtime-load-pilot.mjs \\',
        '    --build-id staging-YYYY.MM.DD.N \\',
        '    --reload-proof path/to/reload-proof.yaml \\',
        '    --effective-runtime path/to/effective-runtime.yaml \\',
        '    --output evidence/runtime/<file>.json',
      ].join('\n') + '\n',
    );
    return null;
  }

  const { registryPath, record } = findRegistryRecord(args.repoRoot, args.subjectId);
  assertObject(record.runtime_identity, `${registryPath} is missing runtime_identity`);
  const runtimeIdentity = record.runtime_identity;

  const sectionContract = loadSectionContract(args.repoRoot);
  const projectionPath = findOutputPath(record, 'runtime_projection', args.repoRoot);
  const targetPath = path.join(args.repoRoot, runtimeIdentity.target_file);
  const projection = extractSection(projectionPath, runtimeIdentity.section_key);
  const committed = extractSection(targetPath, runtimeIdentity.section_key);
  const effective = extractSection(args.effectiveRuntimePath, runtimeIdentity.section_key);

  const reloadProof = loadReloadProof(args.reloadProofPath, {
    plugin: runtimeIdentity.plugin,
    environment: args.environment,
    buildId: args.buildId,
    targetFile: runtimeIdentity.target_file,
    sectionKey: runtimeIdentity.section_key,
    targetTuple: runtimeIdentity.target_tuple,
  });

  const reverseBijection = runReverseBijection(args.repoRoot);
  const committedResult = validateCommittedTarget({
    projection,
    committed,
    sectionContract,
    sectionKey: runtimeIdentity.section_key,
  });
  const effectiveResult = validateEffectiveRuntime({
    committed,
    effective,
    sectionContract,
  });

  const evidence = buildEvidence({
    repoRoot: args.repoRoot,
    outputPath: args.outputPath,
    args,
    record,
    registryPath,
    projection,
    committed,
    effective,
    reverseBijection,
    reloadProof,
    committedResult,
    effectiveResult,
    sectionContract,
    projectionPath,
    targetPath,
  });

  mkdirSync(path.dirname(args.outputPath), { recursive: true });
  writeFileSync(args.outputPath, `${JSON.stringify(evidence, null, 2)}\n`, 'utf8');
  return evidence;
}

function main() {
  try {
    const evidence = auditRuntimeLoad(process.argv);
    if (evidence) {
      process.stdout.write(`${JSON.stringify(evidence, null, 2)}\n`);
      process.exit(evidence.verdict === 'pass' ? 0 : 1);
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    process.stderr.write(`${message}\n`);
    process.exit(1);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main();
}
