import assert from 'node:assert/strict';
import { cpSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

import { validateQaSchemas } from '../validate-schemas.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');
const ADAPTER = path.join(REPO_ROOT, 'tools/qa/adapters/foundation-runtime.mjs');
const FIXTURE_DIR = path.join(__dirname, 'fixtures/foundation-runtime');

function runAdapter(fixtureName) {
  const tempRoot = mkdtempSync(path.join(os.tmpdir(), 'foundation-runtime-adapter-'));
  const output = path.join(tempRoot, `${fixtureName}.record.json`);
  const fixtureInput = path.join(FIXTURE_DIR, `${fixtureName}.json`);
  const input = path.join(tempRoot, `${fixtureName}.json`);
  cpSync(fixtureInput, input);
  const result = spawnSync('node', [
    ADAPTER,
    '--input',
    input,
    '--scenario',
    'character_creator_login_persistence',
    '--output',
    output,
  ], {
    cwd: REPO_ROOT,
    encoding: 'utf8',
  });

  return { tempRoot, input, output, result };
}

function assertOutputRecordValid(recordPath) {
  const qaRoot = mkdtempSync(path.join(os.tmpdir(), 'qa-schema-record-'));
  try {
    cpSync(path.join(REPO_ROOT, 'docs/qa/e2e-scenarios.yaml'), path.join(qaRoot, 'e2e-scenarios.yaml'));
    cpSync(path.join(REPO_ROOT, 'docs/qa/release-gates.yaml'), path.join(qaRoot, 'release-gates.yaml'));
    cpSync(path.join(REPO_ROOT, 'docs/qa/regression-matrix.yaml'), path.join(qaRoot, 'regression-matrix.yaml'));
    const record = JSON.parse(readFileSync(recordPath, 'utf8'));
    const registry = {
      schema_version: 1,
      records: [record],
    };
    writeFileSync(path.join(qaRoot, 'runtime-evidence-registry.yaml'), JSON.stringify(registry, null, 2));
    const validation = validateQaSchemas(qaRoot);
    assert.equal(validation.evidence_records, 1);
  } finally {
    rmSync(qaRoot, { recursive: true, force: true });
  }
}

for (const [fixtureName, expectedVerdict] of [
  ['pass', 'PASS'],
  ['fail', 'FAIL'],
  ['blocked', 'BLOCKED'],
  ['insufficient-evidence', 'INSUFFICIENT_EVIDENCE'],
]) {
  test(`foundation runtime adapter emits valid ${expectedVerdict} record`, () => {
    const { tempRoot, input, output, result } = runAdapter(fixtureName);
    try {
      assert.equal(result.status, 0, result.stderr || result.stdout);
      const record = JSON.parse(readFileSync(output, 'utf8'));
      assert.equal(record.result.verdict, expectedVerdict);
      assert.equal(record.scenario_id, 'character_creator_login_persistence');
      assert.equal(record.artifacts[0].path, input);
      assert.equal(record.result.proof_level, expectedVerdict === 'PASS' ? 'PLAYER_PROOF' : record.result.proof_level);
      assertOutputRecordValid(output);
    } finally {
      rmSync(tempRoot, { recursive: true, force: true });
    }
  });
}

test('foundation runtime adapter degrades missing proof to insufficient evidence', () => {
  const { tempRoot, output, result } = runAdapter('pass-missing-proof');
  try {
    assert.equal(result.status, 0, result.stderr || result.stdout);
    const record = JSON.parse(readFileSync(output, 'utf8'));
    assert.equal(record.result.verdict, 'INSUFFICIENT_EVIDENCE');
    assert.equal(record.runtime_status, 'partial');
    assert.match(record.result.observations.join('\n'), /missing required proof/i);
    assertOutputRecordValid(output);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
});

test('foundation runtime adapter emits blocked record on parser failure', () => {
  const tempRoot = mkdtempSync(path.join(os.tmpdir(), 'foundation-runtime-parser-'));
  const input = path.join(tempRoot, 'invalid.json');
  const output = path.join(tempRoot, 'parser-failure.record.json');
  writeFileSync(input, '{not valid json', 'utf8');
  const result = spawnSync('node', [
    ADAPTER,
    '--input',
    input,
    '--scenario',
    'character_creator_login_persistence',
    '--output',
    output,
  ], {
    cwd: REPO_ROOT,
    encoding: 'utf8',
  });

  try {
    assert.equal(result.status, 0, result.stderr || result.stdout);
    const record = JSON.parse(readFileSync(output, 'utf8'));
    assert.equal(record.result.verdict, 'BLOCKED');
    assert.equal(record.runtime_status, 'blocked');
    assert.match(record.result.observations.join('\n'), /parser failure/i);
    assertOutputRecordValid(output);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
});
