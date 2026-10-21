import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync, spawnSync } from 'node:child_process';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, '../../..');
const E2E_ADAPTER = path.join(REPO_ROOT, 'tools/qa/adapters/ashen-vanguard-e2e.mjs');
const RELEASE_GATE = path.join(REPO_ROOT, 'tools/qa/evaluate-ashen-vanguard-release-gate.mjs');

function withFixture(callback) {
  const root = mkdtempSync(path.join(os.tmpdir(), 'ashen-vanguard-gate-'));
  try {
    mkdirSync(path.join(root, 'evidence/runtime'), { recursive: true });
    mkdirSync(path.join(root, 'evidence/e2e'), { recursive: true });
    callback(root);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
}

function writeJson(filePath, value) {
  mkdirSync(path.dirname(filePath), { recursive: true });
  writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

function writeYaml(filePath, content) {
  mkdirSync(path.dirname(filePath), { recursive: true });
  writeFileSync(filePath, `${content.trim()}\n`, 'utf8');
}

function runtimeLoadedEvidence(overrides = {}) {
  return {
    schema_version: 1,
    evidence_id: 'runtime-load.item.weapon.sword.ashen_vanguard.staging-2026.08.03.1',
    gate: 'runtime_loaded',
    producer_domain: 'domain-18',
    producer_tool: 'tools/qa/audit-mmoitems-runtime-load-pilot.mjs',
    verdict: 'pass',
    environment: 'staging',
    build_id: 'staging-2026.08.03.1',
    subject_id: 'item.weapon.sword.ashen_vanguard',
    content_type: 'item',
    source_path: 'content/items/weapons/ashen_vanguard.yaml',
    subject_digest: 'sha256:subject',
    dependency_digest: 'sha256:deps',
    runtime_plugin: 'MMOItems',
    target_file: 'MCMMORPG/plugins/MMOItems/item/sword.yml',
    section_key: 'ASHEN_VANGUARD',
    generated_projection_digest: 'sha256:projection',
    committed_target_digest: 'sha256:committed',
    effective_runtime_digest: 'sha256:effective',
    assertions: ['fixture'],
    captured_at: '2026-08-03T12:00:00Z',
    artifact_path: 'evidence/runtime/runtime-load.json',
    ...overrides,
  };
}

function e2eInput(overrides = {}) {
  return {
    schema_version: 1,
    report_kind: 'mineflayer-item-e2e',
    scenario_id: 'ashen_vanguard_obtainable_identity',
    environment: 'staging',
    build_id: 'staging-2026.08.03.1',
    subject_id: 'item.weapon.sword.ashen_vanguard',
    target_tuple: 'MMOItems:MCMMORPG/plugins/MMOItems/item/sword.yml#ASHEN_VANGUARD',
    effective_runtime_digest: 'sha256:effective',
    obtainable: true,
    identity_matched: true,
    player_visible: true,
    captured_at: '2026-08-03T12:05:00Z',
    ...overrides,
  };
}

function rollbackProof(overrides = {}) {
  return [
    'schema_version: 1',
    'proof_kind: mmoitems-section-rollback',
    'environment: staging',
    'build_id: staging-2026.08.03.1',
    'subject_id: item.weapon.sword.ashen_vanguard',
    'target_tuple: MMOItems:MCMMORPG/plugins/MMOItems/item/sword.yml#ASHEN_VANGUARD',
    'rollback_unit: section_only',
    `tooling_status: ${overrides.tooling_status || 'supported'}`,
    `status: ${overrides.status || 'success'}`,
    'captured_at: 2026-08-03T12:10:00Z',
  ].join('\n');
}

test('adapts a passing Mineflayer report into runtime_verified evidence', () =>
  withFixture((root) => {
    const runtimeLoadedPath = path.join(root, 'evidence/runtime/runtime-load.json');
    const inputPath = path.join(root, 'evidence/e2e/mineflayer-report.json');
    const outputPath = path.join(root, 'evidence/e2e/runtime-verified.json');

    writeJson(runtimeLoadedPath, runtimeLoadedEvidence());
    writeJson(inputPath, e2eInput());

    execFileSync('node', [E2E_ADAPTER, '--input', inputPath, '--runtime-loaded', runtimeLoadedPath, '--output', outputPath], {
      cwd: root,
      encoding: 'utf8',
    });

    const record = JSON.parse(readFileSync(outputPath, 'utf8'));
    assert.equal(record.verdict, 'pass');
    assert.equal(record.gate, 'runtime_verified');
    assert.equal(record.runtime_status, 'verified');
    assert.ok(record.assertions.includes('runtime-loaded-build-match'));
    assert.ok(record.assertions.includes('item-obtainable-by-player'));
  }));

test('blocks runtime_verified when Mineflayer report build mismatches runtime_loaded', () =>
  withFixture((root) => {
    const runtimeLoadedPath = path.join(root, 'evidence/runtime/runtime-load.json');
    const inputPath = path.join(root, 'evidence/e2e/mineflayer-report.json');
    const outputPath = path.join(root, 'evidence/e2e/runtime-verified.json');

    writeJson(runtimeLoadedPath, runtimeLoadedEvidence());
    writeJson(inputPath, e2eInput({ build_id: 'staging-2026.08.03.2' }));

    const result = spawnSync('node', [E2E_ADAPTER, '--input', inputPath, '--runtime-loaded', runtimeLoadedPath, '--output', outputPath], {
      cwd: root,
      encoding: 'utf8',
    });

    const record = JSON.parse(readFileSync(outputPath, 'utf8'));
    assert.equal(result.status, 1);
    assert.equal(record.verdict, 'blocked');
    assert.ok(record.reasons.includes('build_id_mismatch'));
  }));

test('blocks runtime_verified when runtime_loaded is not pass', () =>
  withFixture((root) => {
    const runtimeLoadedPath = path.join(root, 'evidence/runtime/runtime-load.json');
    const inputPath = path.join(root, 'evidence/e2e/mineflayer-report.json');
    const outputPath = path.join(root, 'evidence/e2e/runtime-verified.json');

    writeJson(runtimeLoadedPath, runtimeLoadedEvidence({ verdict: 'blocked', effective_runtime_digest: null }));
    writeJson(inputPath, e2eInput({ effective_runtime_digest: null }));

    const result = spawnSync('node', [E2E_ADAPTER, '--input', inputPath, '--runtime-loaded', runtimeLoadedPath, '--output', outputPath], {
      cwd: root,
      encoding: 'utf8',
    });

    const record = JSON.parse(readFileSync(outputPath, 'utf8'));
    assert.equal(result.status, 1);
    assert.equal(record.verdict, 'blocked');
    assert.ok(record.reasons.includes('runtime_loaded_not_pass'));
  }));

test('promotes to release_candidate when runtime_verified and section-only rollback proof are aligned', () =>
  withFixture((root) => {
    const runtimeLoadedPath = path.join(root, 'evidence/runtime/runtime-load.json');
    const runtimeVerifiedPath = path.join(root, 'evidence/e2e/runtime-verified.json');
    const rollbackPath = path.join(root, 'evidence/runtime/rollback-proof.yaml');
    const outputPath = path.join(root, 'evidence/runtime/release-candidate.json');

    writeJson(runtimeLoadedPath, runtimeLoadedEvidence());
    writeJson(runtimeVerifiedPath, {
      ...runtimeLoadedEvidence({
        evidence_id: 'runtime-verified.item.weapon.sword.ashen_vanguard.staging-2026.08.03.1',
        gate: 'runtime_verified',
        producer_tool: 'tools/qa/adapters/ashen-vanguard-e2e.mjs',
        assertions: ['runtime-loaded-build-match', 'item-obtainable-by-player'],
      }),
      verdict: 'pass',
    });
    writeYaml(rollbackPath, rollbackProof());

    execFileSync(
      'node',
      [
        RELEASE_GATE,
        '--runtime-loaded',
        runtimeLoadedPath,
        '--runtime-verified',
        runtimeVerifiedPath,
        '--rollback-proof',
        rollbackPath,
        '--output',
        outputPath,
      ],
      { cwd: root, encoding: 'utf8' },
    );

    const record = JSON.parse(readFileSync(outputPath, 'utf8'));
    assert.equal(record.verdict, 'pass');
    assert.equal(record.gate, 'release_candidate');
    assert.equal(record.release_status, 'candidate');
    assert.ok(record.assertions.includes('section-only-rollback-proof-confirmed'));
  }));

test('blocks release_candidate when only unsupported rollback tooling exists', () =>
  withFixture((root) => {
    const runtimeLoadedPath = path.join(root, 'evidence/runtime/runtime-load.json');
    const runtimeVerifiedPath = path.join(root, 'evidence/e2e/runtime-verified.json');
    const rollbackPath = path.join(root, 'evidence/runtime/rollback-proof.yaml');
    const outputPath = path.join(root, 'evidence/runtime/release-candidate.json');

    writeJson(runtimeLoadedPath, runtimeLoadedEvidence());
    writeJson(runtimeVerifiedPath, {
      ...runtimeLoadedEvidence({
        evidence_id: 'runtime-verified.item.weapon.sword.ashen_vanguard.staging-2026.08.03.1',
        gate: 'runtime_verified',
        producer_tool: 'tools/qa/adapters/ashen-vanguard-e2e.mjs',
      }),
      verdict: 'pass',
    });
    writeYaml(rollbackPath, rollbackProof({ tooling_status: 'unsupported' }));

    const result = spawnSync(
      'node',
      [
        RELEASE_GATE,
        '--runtime-loaded',
        runtimeLoadedPath,
        '--runtime-verified',
        runtimeVerifiedPath,
        '--rollback-proof',
        rollbackPath,
        '--output',
        outputPath,
      ],
      { cwd: root, encoding: 'utf8' },
    );

    const record = JSON.parse(readFileSync(outputPath, 'utf8'));
    assert.equal(result.status, 1);
    assert.equal(record.verdict, 'blocked');
    assert.ok(record.reasons.includes('section_restore_tooling_unsupported'));
  }));
