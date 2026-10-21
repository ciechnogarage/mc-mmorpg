#!/usr/bin/env node

import { mkdirSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');
const SUBJECT_ID = 'item.weapon.sword.ashen_vanguard';
const TARGET_TUPLE = 'MMOItems:MCMMORPG/plugins/MMOItems/item/sword.yml#ASHEN_VANGUARD';
const SCENARIO_ID = 'ashen_vanguard_obtainable_identity';

class AdapterError extends Error {
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
      throw new AdapterError('ARGUMENT_ERROR', `unexpected argument ${token}`);
    }
    const key = token.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) {
      throw new AdapterError('ARGUMENT_ERROR', `missing value for --${key}`);
    }
    if (key === 'input') args.input = path.resolve(value);
    else if (key === 'runtime-loaded') args.runtimeLoaded = path.resolve(value);
    else if (key === 'output') args.output = path.resolve(value);
    else if (key === 'repo-root') args.repoRoot = path.resolve(value);
    else throw new AdapterError('ARGUMENT_ERROR', `unknown option --${key}`);
    index += 1;
  }
  for (const key of ['input', 'runtimeLoaded', 'output']) {
    if (!args[key]) {
      throw new AdapterError('ARGUMENT_ERROR', `missing required --${key}`);
    }
  }
  return args;
}

function loadYaml(filePath) {
  if (!existsSync(filePath)) {
    throw new AdapterError('MISSING_FILE', `${filePath} does not exist`);
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
    throw new AdapterError('YAML_PARSE_FAILED', `${filePath}: ${detail}`);
  }
  return JSON.parse(result.stdout);
}

function loadJson(filePath) {
  if (!existsSync(filePath)) {
    throw new AdapterError('MISSING_FILE', `${filePath} does not exist`);
  }
  return JSON.parse(readFileSync(filePath, 'utf8'));
}

function repoRelative(repoRoot, filePath) {
  return path.relative(repoRoot, filePath).split(path.sep).join('/');
}

function normalizeRuntimeVerdict(report, runtimeLoaded) {
  const assertions = [];
  const blockers = [];
  const failures = [];

  if (runtimeLoaded.verdict !== 'pass') blockers.push('runtime_loaded_not_pass');
  if (report.report_kind !== 'mineflayer-item-e2e') blockers.push('invalid_report_kind');
  if (report.scenario_id !== SCENARIO_ID) blockers.push('scenario_id_mismatch');
  if (report.subject_id !== SUBJECT_ID) blockers.push('subject_id_mismatch');
  if (report.target_tuple !== TARGET_TUPLE) blockers.push('target_tuple_mismatch');
  if (report.environment !== runtimeLoaded.environment) blockers.push('environment_mismatch');
  if (report.build_id !== runtimeLoaded.build_id) blockers.push('build_id_mismatch');
  if (report.effective_runtime_digest !== runtimeLoaded.effective_runtime_digest) {
    blockers.push('effective_runtime_digest_mismatch');
  }

  if (blockers.length === 0) {
    assertions.push('runtime-loaded-build-match');
    assertions.push('effective-runtime-digest-match');
  }

  if (report.obtainable === true) assertions.push('item-obtainable-by-player');
  else failures.push('item_not_obtainable');

  if (report.identity_matched === true) assertions.push('runtime-identity-matches-target-tuple');
  else failures.push('runtime_identity_mismatch');

  if (report.player_visible === true) assertions.push('player-visible-item-state-confirmed');
  else failures.push('player_visible_state_not_confirmed');

  if (blockers.length > 0) {
    return { verdict: 'blocked', runtimeStatus: 'blocked', releaseStatus: 'blocked', assertions, reasons: blockers };
  }
  if (failures.length > 0) {
    return { verdict: 'fail', runtimeStatus: 'blocked', releaseStatus: 'blocked', assertions, reasons: failures };
  }
  return { verdict: 'pass', runtimeStatus: 'verified', releaseStatus: 'blocked', assertions, reasons: [] };
}

export function adaptAshenVanguardE2E({ input, runtimeLoaded, output, repoRoot = REPO_ROOT }) {
  const report = loadJson(input);
  const runtimeLoadedEvidence = loadJson(runtimeLoaded);
  const result = normalizeRuntimeVerdict(report, runtimeLoadedEvidence);

  const record = {
    schema_version: 1,
    evidence_id: `runtime-verified.${SUBJECT_ID}.${report.build_id}`,
    gate: 'runtime_verified',
    producer_domain: 'domain-18',
    producer_tool: 'tools/qa/adapters/ashen-vanguard-e2e.mjs',
    verdict: result.verdict,
    environment: report.environment,
    build_id: report.build_id,
    subject_id: runtimeLoadedEvidence.subject_id,
    content_type: runtimeLoadedEvidence.content_type,
    source_path: runtimeLoadedEvidence.source_path,
    subject_digest: runtimeLoadedEvidence.subject_digest,
    dependency_digest: runtimeLoadedEvidence.dependency_digest,
    runtime_plugin: runtimeLoadedEvidence.runtime_plugin,
    target_file: runtimeLoadedEvidence.target_file,
    section_key: runtimeLoadedEvidence.section_key,
    generated_projection_digest: runtimeLoadedEvidence.generated_projection_digest,
    committed_target_digest: runtimeLoadedEvidence.committed_target_digest,
    effective_runtime_digest: runtimeLoadedEvidence.effective_runtime_digest,
    assertions: result.assertions,
    captured_at: report.captured_at,
    artifact_path: repoRelative(repoRoot, output),
    scenario_id: SCENARIO_ID,
    validation_channel: 'Mineflayer',
    reasons: result.reasons,
    artifacts: {
      input_report: repoRelative(repoRoot, input),
      runtime_loaded_evidence: repoRelative(repoRoot, runtimeLoaded),
    },
    runtime_status: result.runtimeStatus,
    release_status: result.releaseStatus,
  };

  mkdirSync(path.dirname(output), { recursive: true });
  writeFileSync(output, `${JSON.stringify(record, null, 2)}\n`, 'utf8');
  return record;
}

function main() {
  try {
    const args = parseArgs(process.argv);
    const record = adaptAshenVanguardE2E(args);
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
