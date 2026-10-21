#!/usr/bin/env node

import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');
const SUBJECT_ID = 'item.weapon.sword.ashen_vanguard';
const EVIDENCE_ROOT = 'evidence/runtime/ashen-vanguard';

class RollbackError extends Error {
  constructor(code, message) {
    super(`${code}: ${message}`);
    this.code = code;
  }
}

function sha256(text) {
  return `sha256:${createHash('sha256').update(text, 'utf8').digest('hex')}`;
}

function parseArgs(argv) {
  const command = argv[2];
  if (!command || command.startsWith('--')) {
    throw new RollbackError('ARGUMENT_ERROR', 'missing command capture-baseline|restore-section');
  }

  const args = {
    command,
    repoRoot: process.env.MC_CONTENT_ROOT ? path.resolve(process.env.MC_CONTENT_ROOT) : REPO_ROOT,
  };

  for (let index = 3; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith('--')) {
      throw new RollbackError('ARGUMENT_ERROR', `unexpected argument ${token}`);
    }

    const key = token.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith('--')) {
      throw new RollbackError('ARGUMENT_ERROR', `missing value for --${key}`);
    }

    if (key === 'subject-id') args.subjectId = value;
    else if (key === 'build-id') args.buildId = value;
    else if (key === 'repo-root') args.repoRoot = path.resolve(value);
    else throw new RollbackError('ARGUMENT_ERROR', `unknown option --${key}`);
    index += 1;
  }

  if (!['capture-baseline', 'restore-section'].includes(args.command)) {
    throw new RollbackError('ARGUMENT_ERROR', `unsupported command ${args.command}`);
  }
  if (args.subjectId !== SUBJECT_ID) {
    throw new RollbackError('ARGUMENT_ERROR', `only ${SUBJECT_ID} is supported in v1`);
  }
  if (!args.buildId) {
    throw new RollbackError('ARGUMENT_ERROR', 'missing --build-id');
  }

  return args;
}

function loadYaml(filePath) {
  if (!existsSync(filePath)) {
    throw new RollbackError('MISSING_FILE', `${filePath} does not exist`);
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
    throw new RollbackError('YAML_PARSE_FAILED', `${filePath}: ${detail}`);
  }

  return JSON.parse(result.stdout);
}

function repoRelative(repoRoot, filePath) {
  return path.relative(repoRoot, filePath).split(path.sep).join('/');
}

function readText(filePath) {
  if (!existsSync(filePath)) {
    throw new RollbackError('MISSING_FILE', `${filePath} does not exist`);
  }
  return readFileSync(filePath, 'utf8');
}

function sectionLocations(lines, sectionKey) {
  const matches = [];
  for (let index = 0; index < lines.length; index += 1) {
    if (lines[index] === `${sectionKey}:`) {
      matches.push(index);
    }
  }
  return matches;
}

function extractSection(text, sectionKey) {
  const lines = text.split('\n');
  const matches = sectionLocations(lines, sectionKey);

  if (matches.length === 0) {
    throw new RollbackError('MISSING_SECTION', `${sectionKey} is missing in runtime target`);
  }
  if (matches.length > 1) {
    throw new RollbackError('DUPLICATE_SECTION', `${sectionKey} appears ${matches.length} times`);
  }

  const startLine = matches[0];
  let endLine = lines.length;
  for (let index = startLine + 1; index < lines.length; index += 1) {
    const line = lines[index];
    if (line.length > 0 && !line.startsWith(' ')) {
      endLine = index;
      break;
    }
  }

  const prefix = lines.slice(0, startLine).join('\n');
  const section = lines.slice(startLine, endLine).join('\n').replace(/\n+$/, '');
  const suffix = lines.slice(endLine).join('\n').replace(/^\n+/, '');

  return {
    prefix: prefix.length > 0 ? `${prefix}\n` : '',
    section: `${section}\n`,
    suffix: suffix.length > 0 ? `${suffix}\n` : '',
  };
}

function replaceSection(text, sectionKey, nextSection) {
  const { prefix, suffix } = extractSection(text, sectionKey);
  return `${prefix}${nextSection}${suffix}`.replace(/\n+$/, '\n');
}

function loadSubjectContext(repoRoot) {
  const registryPath = path.join(repoRoot, 'registry/content/items/item.weapon.sword.ashen_vanguard.yaml');
  const contractPath = path.join(repoRoot, 'docs/production/contracts/mmoitems-item-section-schema.yaml');
  const record = loadYaml(registryPath);
  const contract = loadYaml(contractPath);

  if (record.id !== SUBJECT_ID) {
    throw new RollbackError('SUBJECT_MISMATCH', `${registryPath} does not match ${SUBJECT_ID}`);
  }
  if (record.runtime_identity?.target_tuple !== contract.target?.tuple) {
    throw new RollbackError('TARGET_TUPLE_MISMATCH', 'registry and contract target_tuple differ');
  }
  if (contract.target?.rollback_unit !== 'section_only') {
    throw new RollbackError('INVALID_ROLLBACK_UNIT', 'section schema is not pinned to section_only');
  }

  return {
    registryPath,
    contractPath,
    record,
    contract,
    targetFile: path.join(repoRoot, record.runtime_identity.target_file),
    targetTuple: record.runtime_identity.target_tuple,
    sectionKey: record.runtime_identity.section_key,
  };
}

function evidencePaths(repoRoot, buildId) {
  const dir = path.join(repoRoot, EVIDENCE_ROOT, buildId);
  return {
    dir,
    baseline: path.join(dir, 'section-baseline.yaml'),
    proof: path.join(dir, 'section-rollback-proof.yaml'),
    log: path.join(dir, 'section-rollback.log'),
  };
}

function writeIfMissingOrEqual(filePath, content) {
  if (existsSync(filePath)) {
    const current = readFileSync(filePath, 'utf8');
    if (current !== content) {
      throw new RollbackError('BASELINE_MISMATCH', `${filePath} already exists with different content`);
    }
    return 'no-op';
  }
  mkdirSync(path.dirname(filePath), { recursive: true });
  writeFileSync(filePath, content, 'utf8');
  return 'created';
}

function captureBaseline({ repoRoot, buildId }) {
  const context = loadSubjectContext(repoRoot);
  const targetText = readText(context.targetFile);
  const extracted = extractSection(targetText, context.sectionKey);
  const outputs = evidencePaths(repoRoot, buildId);
  const action = writeIfMissingOrEqual(outputs.baseline, extracted.section);

  mkdirSync(outputs.dir, { recursive: true });
  writeFileSync(
    outputs.log,
    [
      `captured_at=${new Date().toISOString()}`,
      `subject_id=${SUBJECT_ID}`,
      `build_id=${buildId}`,
      `target_tuple=${context.targetTuple}`,
      `action=${action}`,
      `baseline_sha256=${sha256(extracted.section)}`,
    ].join('\n') + '\n',
    'utf8',
  );

  return {
    command: 'capture-baseline',
    subject_id: SUBJECT_ID,
    build_id: buildId,
    target_tuple: context.targetTuple,
    baseline_path: repoRelative(repoRoot, outputs.baseline),
    baseline_sha256: sha256(extracted.section),
    action,
  };
}

function buildProof({
  repoRoot,
  buildId,
  context,
  outputs,
  baselineText,
  previousSection,
  restoredSection,
  targetFileBefore,
  targetFileAfter,
}) {
  return [
    'schema_version: 1',
    'proof_kind: mmoitems-section-rollback',
    'environment: staging',
    `build_id: ${buildId}`,
    `subject_id: ${SUBJECT_ID}`,
    `target_tuple: ${context.targetTuple}`,
    'rollback_unit: section_only',
    'tooling_status: supported',
    'status: success',
    `captured_at: ${new Date().toISOString()}`,
    'observations:',
    `  - baseline_path: ${repoRelative(repoRoot, outputs.baseline)}`,
    `  - target_file: ${repoRelative(repoRoot, context.targetFile)}`,
    `  - previous_section_sha256: ${sha256(previousSection)}`,
    `  - restored_section_sha256: ${sha256(restoredSection)}`,
    `  - baseline_section_sha256: ${sha256(baselineText)}`,
    `  - target_file_before_sha256: ${sha256(targetFileBefore)}`,
    `  - target_file_after_sha256: ${sha256(targetFileAfter)}`,
    '  - collateral_write_outside_section: false',
  ].join('\n') + '\n';
}

function restoreSection({ repoRoot, buildId }) {
  const context = loadSubjectContext(repoRoot);
  const outputs = evidencePaths(repoRoot, buildId);
  const baselineText = readText(outputs.baseline);
  const targetFileBefore = readText(context.targetFile);
  const current = extractSection(targetFileBefore, context.sectionKey);

  const baselineExtracted = extractSection(baselineText, context.sectionKey);
  if (baselineExtracted.prefix !== '' || baselineExtracted.suffix !== '') {
    throw new RollbackError('INVALID_BASELINE', 'baseline must contain only the target section');
  }
  if (!baselineText.startsWith(`${context.sectionKey}:\n`)) {
    throw new RollbackError('INVALID_BASELINE', 'baseline does not start with the authoritative section key');
  }

  const targetFileAfter = replaceSection(targetFileBefore, context.sectionKey, baselineText);
  const restored = extractSection(targetFileAfter, context.sectionKey);

  if (restored.section !== baselineText) {
    throw new RollbackError('RESTORE_MISMATCH', 'restored section does not match baseline snapshot');
  }

  const currentPrefixSuffix = `${current.prefix}__SPLIT__${current.suffix}`;
  const restoredPrefixSuffix = `${restored.prefix}__SPLIT__${restored.suffix}`;
  if (currentPrefixSuffix !== restoredPrefixSuffix) {
    throw new RollbackError('COLLATERAL_WRITE', 'restore would modify content outside the target section');
  }

  mkdirSync(outputs.dir, { recursive: true });
  writeFileSync(context.targetFile, targetFileAfter, 'utf8');

  const proof = buildProof({
    repoRoot,
    buildId,
    context,
    outputs,
    baselineText,
    previousSection: current.section,
    restoredSection: restored.section,
    targetFileBefore,
    targetFileAfter,
  });
  writeFileSync(outputs.proof, proof, 'utf8');
  writeFileSync(
    outputs.log,
    [
      `restored_at=${new Date().toISOString()}`,
      `subject_id=${SUBJECT_ID}`,
      `build_id=${buildId}`,
      `target_tuple=${context.targetTuple}`,
      `baseline_sha256=${sha256(baselineText)}`,
      `previous_section_sha256=${sha256(current.section)}`,
      `restored_section_sha256=${sha256(restored.section)}`,
      `collateral_write_outside_section=false`,
    ].join('\n') + '\n',
    'utf8',
  );

  return {
    command: 'restore-section',
    subject_id: SUBJECT_ID,
    build_id: buildId,
    target_tuple: context.targetTuple,
    baseline_path: repoRelative(repoRoot, outputs.baseline),
    proof_path: repoRelative(repoRoot, outputs.proof),
    restored_section_sha256: sha256(restored.section),
  };
}

export function runRollback(rawArgs = process.argv) {
  const args = parseArgs(rawArgs);
  if (args.command === 'capture-baseline') {
    return captureBaseline(args);
  }
  return restoreSection(args);
}

function main() {
  try {
    const result = runRollback(process.argv);
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    process.stderr.write(`${message}\n`);
    process.exit(1);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main();
}
