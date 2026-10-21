#!/usr/bin/env node

import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../..');
const CONTENT_ROOT = path.join(REPO_ROOT, 'registry/content');
const CONTENT_REGISTRY = path.join(REPO_ROOT, 'registry/content-registry.yaml');
const PILOT_PLUGIN = 'MMOItems';
const RUNTIME_REQUIRED_STAGES = new Set([
  'runtime_loaded',
  'runtime_verified',
  'release_candidate',
  'released',
]);

export class ValidationError extends Error {
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
    throw new ValidationError('YAML_PARSE_FAILED', `${filePath}: ${detail}`);
  }

  return JSON.parse(result.stdout);
}

function listYamlFiles(rootDir) {
  if (!existsSync(rootDir)) {
    throw new ValidationError('MISSING_CONTENT_ROOT', `${rootDir} does not exist`);
  }

  const files = [];
  const stack = [rootDir];

  while (stack.length > 0) {
    const currentDir = stack.pop();
    for (const entry of readdirSync(currentDir)) {
      const entryPath = path.join(currentDir, entry);
      const stats = statSync(entryPath);
      if (stats.isDirectory()) {
        stack.push(entryPath);
        continue;
      }
      if (stats.isFile() && /\.(ya?ml)$/i.test(entry)) {
        files.push(entryPath);
      }
    }
  }

  return files.sort();
}

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isActiveRecord(record) {
  if (!isObject(record)) {
    return false;
  }
  if (record.deprecated === true) {
    return false;
  }
  const lifecycleStage = record.lifecycle?.stage ?? record.lifecycle_stage;
  return lifecycleStage !== 'retired';
}

function buildTuple(runtimeIdentity) {
  if (!isObject(runtimeIdentity)) {
    return null;
  }

  if (typeof runtimeIdentity.target_tuple === 'string' && runtimeIdentity.target_tuple.length > 0) {
    return runtimeIdentity.target_tuple;
  }

  if (
    typeof runtimeIdentity.plugin === 'string' &&
    typeof runtimeIdentity.target_file === 'string' &&
    typeof runtimeIdentity.section_key === 'string'
  ) {
    return `${runtimeIdentity.plugin}:${runtimeIdentity.target_file}#${runtimeIdentity.section_key}`;
  }

  return null;
}

function requiresRuntimeTarget(record) {
  return RUNTIME_REQUIRED_STAGES.has(record.lifecycle?.stage ?? record.lifecycle_stage);
}

function pushGrouped(map, key, value) {
  if (!map.has(key)) {
    map.set(key, []);
  }
  map.get(key).push(value);
}

function getSubjectId(record) {
  if (typeof record.id === 'string' && record.id.length > 0) {
    return record.id;
  }
  if (isObject(record.subject) && typeof record.subject.id === 'string' && record.subject.id.length > 0) {
    return record.subject.id;
  }
  return null;
}

function collectActivePilotSubjects(rootDir) {
  const subjects = [];

  for (const filePath of listYamlFiles(path.join(rootDir, 'registry/content'))) {
    const record = loadYaml(filePath);
    if (!isActiveRecord(record)) {
      continue;
    }

    const runtimeIdentity = isObject(record.runtime_identity) ? record.runtime_identity : null;
    if (!runtimeIdentity || runtimeIdentity.plugin !== PILOT_PLUGIN) {
      continue;
    }

    const subjectId = getSubjectId(record);
    if (!subjectId) {
      throw new ValidationError('MISSING_SUBJECT_ID', `${filePath} must declare a non-empty id`);
    }

    subjects.push({
      subjectId,
      tuple: buildTuple(runtimeIdentity),
      targetFile: runtimeIdentity.target_file ?? null,
      sectionKey: runtimeIdentity.section_key ?? null,
      runtimeRequired: requiresRuntimeTarget(record),
      filePath,
    });
  }

  return subjects;
}

function collectRegistryOccupancy(rootDir) {
  const root = loadYaml(path.join(rootDir, 'registry/content-registry.yaml'));
  const entries = Array.isArray(root.entries) ? root.entries : root.records;

  if (!Array.isArray(entries)) {
    throw new ValidationError(
      'INVALID_REGISTRY',
      `${CONTENT_REGISTRY} must contain an entries array or legacy records array`,
    );
  }

  return entries
    .filter((record) => isObject(record) && isActiveRecord(record) && record.runtime_plugin === PILOT_PLUGIN)
    .map((record, index) => ({
      subjectId: record.subject_id ?? null,
      tuple: typeof record.target_tuple === 'string' ? record.target_tuple : null,
      registryPath: record.registry_path ?? null,
      index,
    }));
}

export function validateReverseBijection(rootDir = REPO_ROOT) {
  const sourceSubjects = collectActivePilotSubjects(rootDir);
  const registryOccupancy = collectRegistryOccupancy(rootDir);
  const failures = [];

  const subjectsById = new Map();
  const subjectsByTuple = new Map();

  for (const subject of sourceSubjects) {
    subjectsById.set(subject.subjectId, subject);

    if (subject.runtimeRequired && (!subject.tuple || !subject.targetFile || !subject.sectionKey)) {
      failures.push(
        `MISSING_RUNTIME_TARGET: ${subject.subjectId} in ${subject.filePath} requires runtime occupancy metadata`,
      );
      continue;
    }

    if (subject.tuple) {
      pushGrouped(subjectsByTuple, subject.tuple, subject.subjectId);
    }
  }

  for (const [tuple, subjectIds] of subjectsByTuple.entries()) {
    if (subjectIds.length > 1) {
      failures.push(`DUPLICATE_TARGET: ${tuple} claimed by active subjects ${subjectIds.join(', ')}`);
    }
  }

  const occupancyByTuple = new Map();
  for (const entry of registryOccupancy) {
    if (!entry.subjectId) {
      failures.push(`ORPHAN_TARGET: content-registry entry ${entry.index} is missing subject_id`);
      continue;
    }
    if (!entry.tuple) {
      failures.push(`MISSING_RUNTIME_TARGET: content-registry entry ${entry.index} is missing target_tuple`);
      continue;
    }
    pushGrouped(occupancyByTuple, entry.tuple, entry.subjectId);
  }

  for (const [tuple, subjectIds] of occupancyByTuple.entries()) {
    if (subjectIds.length > 1) {
      failures.push(`DUPLICATE_TARGET: registry occupancy ${tuple} maps to ${subjectIds.join(', ')}`);
    }
  }

  for (const entry of registryOccupancy) {
    if (!entry.subjectId || !entry.tuple) {
      continue;
    }

    const sourceSubject = subjectsById.get(entry.subjectId);
    if (!sourceSubject) {
      failures.push(`ORPHAN_TARGET: registry occupancy ${entry.tuple} points to inactive subject ${entry.subjectId}`);
      continue;
    }

    if (sourceSubject.tuple !== entry.tuple) {
      failures.push(
        `MISSING_RUNTIME_TARGET: registry occupancy ${entry.tuple} mismatches source tuple ${sourceSubject.tuple} for ${entry.subjectId}`,
      );
    }
  }

  for (const subject of sourceSubjects) {
    if (!subject.runtimeRequired) {
      continue;
    }

    const occupancy = registryOccupancy.find((entry) => entry.subjectId === subject.subjectId);
    if (!occupancy) {
      failures.push(`MISSING_RUNTIME_TARGET: ${subject.subjectId} is missing aggregate occupancy`);
      continue;
    }

    if (occupancy.tuple !== subject.tuple) {
      failures.push(
        `MISSING_RUNTIME_TARGET: ${subject.subjectId} aggregate tuple ${occupancy.tuple} mismatches ${subject.tuple}`,
      );
    }
  }

  if (failures.length > 0) {
    throw new ValidationError('REVERSE_BIJECTION_FAILED', failures.join('\n'));
  }

  return {
    ok: true,
    plugin: PILOT_PLUGIN,
    active_subjects: sourceSubjects.length,
    occupied_targets: registryOccupancy.length,
  };
}

function main() {
  try {
    const rootDir = process.argv[2] ? path.resolve(process.argv[2]) : REPO_ROOT;
    const result = validateReverseBijection(rootDir);
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
