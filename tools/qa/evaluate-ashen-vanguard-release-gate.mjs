#!/usr/bin/env node

import { mkdirSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');
const SUBJECT_ID = 'item.weapon.sword.ashen_vanguard';
const TARGET_TUPLE = 'MMOItems:MCMMORPG/plugins/MMOItems/item/sword.yml#ASHEN_VANGUARD';

class GateError extends Error {
  constructor(code, message) {
    super(`${code}: ${message}`);
    this.code = code;
  }
}

function parseArgs(argv) {
  const args = { repoRoot: REPO_ROOT };
  for (let index = 2; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith('--')) {
      throw new GateError('ARGUMENT_ERROR', `unexpected argument ${token}`);
    }
    const key = token.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) {
      throw new GateError('ARGUMENT_ERROR', `missing value for --${key}`);
    }
    if (key === 'runtime-loaded') args.runtimeLoaded = path.resolve(value);
    else if (key === 'runtime-verified') args.runtimeVerified = path.resolve(value);
    else if (key === 'rollback-proof') args.rollbackProof = path.resolve(value);
    else if (key === 'output') args.output = path.resolve(value);
    else if (key === 'repo-root') args.repoRoot = path.resolve(value);
    else throw new GateError('ARGUMENT_ERROR', `unknown option --${key}`);
    index += 1;
  }
  for (const key of ['runtimeLoaded', 'runtimeVerified', 'rollbackProof', 'output']) {
    if (!args[key]) {
      throw new GateError('ARGUMENT_ERROR', `missing required --${key}`);
    }
  }
  return args;
}

function loadJson(filePath) {
  if (!existsSync(filePath)) {
    throw new GateError('MISSING_FILE', `${filePath} does not exist`);
  }
  return JSON.parse(readFileSync(filePath, 'utf8'));
}

function loadYaml(filePath) {
  if (!existsSync(filePath)) {
    throw new GateError('MISSING_FILE', `${filePath} does not exist`);
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
    throw new GateError('YAML_PARSE_FAILED', `${filePath}: ${detail}`);
  }
  return JSON.parse(result.stdout);
}

function repoRelative(repoRoot, filePath) {
  return path.relative(repoRoot, filePath).split(path.sep).join('/');
}

function evaluate({ runtimeLoaded, runtimeVerified, rollbackProof }) {
  const assertions = [];
  const blockers = [];
  const failures = [];

  if (runtimeLoaded.verdict !== 'pass') blockers.push('runtime_loaded_not_pass');
  if (runtimeVerified.verdict !== 'pass') blockers.push('runtime_verified_not_pass');
  if (runtimeLoaded.subject_id !== SUBJECT_ID || runtimeVerified.subject_id !== SUBJECT_ID) {
    blockers.push('subject_id_mismatch');
  }
  if (runtimeLoaded.build_id !== runtimeVerified.build_id) blockers.push('build_id_mismatch');
  if (runtimeLoaded.effective_runtime_digest !== runtimeVerified.effective_runtime_digest) {
    blockers.push('effective_runtime_digest_mismatch');
  }
  if (
    rollbackProof.subject_id !== SUBJECT_ID ||
    rollbackProof.target_tuple !== TARGET_TUPLE ||
    rollbackProof.build_id !== runtimeLoaded.build_id
  ) {
    blockers.push('rollback_proof_identity_mismatch');
  }

  if (rollbackProof.rollback_unit !== 'section_only') blockers.push('rollback_unit_not_section_only');
  if (rollbackProof.tooling_status !== 'supported') blockers.push('section_restore_tooling_unsupported');
  if (rollbackProof.status !== 'success') failures.push('rollback_proof_not_success');

  if (blockers.length === 0) {
    assertions.push('runtime-loaded-and-runtime-verified-share-build');
    assertions.push('effective-runtime-digest-shared-across-gates');
  }
  if (
    rollbackProof.rollback_unit === 'section_only' &&
    rollbackProof.tooling_status === 'supported' &&
    rollbackProof.status === 'success'
  ) {
    assertions.push('section-only-rollback-proof-confirmed');
  }

  if (blockers.length > 0) {
    return { verdict: 'blocked', releaseStatus: 'blocked', assertions, reasons: blockers };
  }
  if (failures.length > 0) {
    return { verdict: 'fail', releaseStatus: 'blocked', assertions, reasons: failures };
  }
  return { verdict: 'pass', releaseStatus: 'candidate', assertions, reasons: [] };
}

export function evaluateAshenVanguardReleaseGate({
  runtimeLoaded,
  runtimeVerified,
  rollbackProof,
  output,
  repoRoot = REPO_ROOT,
}) {
  const loaded = loadJson(runtimeLoaded);
  const verified = loadJson(runtimeVerified);
  const rollback = loadYaml(rollbackProof);
  const result = evaluate({ runtimeLoaded: loaded, runtimeVerified: verified, rollbackProof: rollback });

  const record = {
    schema_version: 1,
    evidence_id: `release-candidate.${SUBJECT_ID}.${loaded.build_id}`,
    gate: 'release_candidate',
    producer_domain: 'domain-18',
    producer_tool: 'tools/qa/evaluate-ashen-vanguard-release-gate.mjs',
    verdict: result.verdict,
    environment: loaded.environment,
    build_id: loaded.build_id,
    subject_id: loaded.subject_id,
    content_type: loaded.content_type,
    source_path: loaded.source_path,
    subject_digest: loaded.subject_digest,
    dependency_digest: loaded.dependency_digest,
    runtime_plugin: loaded.runtime_plugin,
    target_file: loaded.target_file,
    section_key: loaded.section_key,
    generated_projection_digest: loaded.generated_projection_digest,
    committed_target_digest: loaded.committed_target_digest,
    effective_runtime_digest: loaded.effective_runtime_digest,
    assertions: result.assertions,
    captured_at: rollback.captured_at,
    artifact_path: repoRelative(repoRoot, output),
    release_status: result.releaseStatus,
    rollback_target: {
      target_tuple: rollback.target_tuple,
      rollback_unit: rollback.rollback_unit,
      status: rollback.status,
      tooling_status: rollback.tooling_status,
    },
    evidence_inputs: {
      runtime_loaded: repoRelative(repoRoot, runtimeLoaded),
      runtime_verified: repoRelative(repoRoot, runtimeVerified),
      rollback_proof: repoRelative(repoRoot, rollbackProof),
    },
    reasons: result.reasons,
  };

  mkdirSync(path.dirname(output), { recursive: true });
  writeFileSync(output, `${JSON.stringify(record, null, 2)}\n`, 'utf8');
  return record;
}

function main() {
  try {
    const args = parseArgs(process.argv);
    const record = evaluateAshenVanguardReleaseGate(args);
    process.stdout.write(`${JSON.stringify(record, null, 2)}\n`);
    process.exit(record.verdict === 'pass' ? 0 : 1);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    process.stderr.write(`${message}\n`);
    process.exit(1);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main();
}
