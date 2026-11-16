import assert from 'node:assert/strict';
import { cpSync, mkdtempSync, rmSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

import { validateQaSchemas } from '../validate-schemas.mjs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FIXTURES = path.join(__dirname, 'fixtures');

function fixtureDir(name) {
  return path.join(FIXTURES, name);
}

function withFixture(name, callback) {
  const tempRoot = mkdtempSync(path.join(os.tmpdir(), 'qa-schema-'));
  try {
    cpSync(fixtureDir('valid'), tempRoot, { recursive: true });
    if (name !== 'valid') {
      cpSync(fixtureDir(name), tempRoot, { recursive: true });
    }
    return callback(tempRoot);
  } finally {
    rmSync(tempRoot, { recursive: true, force: true });
  }
}

test('accepts a complete schema fixture', () => {
  withFixture('valid', (rootDir) => {
    const result = validateQaSchemas(rootDir);
    assert.equal(result.ok, true);
    assert.equal(result.scenarios, 1);
    assert.equal(result.evidence_records, 1);
    assert.equal(result.gates, 1);
    assert.equal(result.matrix_entries, 1);
  });
});

test('accepts proposed as a transitional contract status', () => {
  withFixture('proposed-contract-status', (rootDir) => {
    const result = validateQaSchemas(rootDir);
    assert.equal(result.ok, true);
    assert.equal(result.evidence_records, 1);
  });
});

test('rejects an unknown enum value', () => {
  withFixture('unknown-enum', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /UNKNOWN_ENUM/);
  });
});

test('rejects a missing required field', () => {
  withFixture('missing-required-field', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /MISSING_REQUIRED_FIELD/);
  });
});

test('rejects stale evidence from a different build context', () => {
  withFixture('stale-evidence', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /STALE_EVIDENCE/);
  });
});

test('rejects a dangling scenario reference', () => {
  withFixture('dangling-scenario-ref', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /DANGLING_SCENARIO_REF/);
  });
});

test('rejects missing build context', () => {
  withFixture('missing-build-context', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /MISSING_REQUIRED_FIELD/);
  });
});

test('rejects missing rollback metadata', () => {
  withFixture('missing-rollback-metadata', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /MISSING_REQUIRED_FIELD/);
  });
});

test('rejects promotion record missing required evidence types', () => {
  withFixture('missing-required-evidence-type', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /MISSING_REQUIRED_EVIDENCE_TYPE/);
  });
});

test('rejects promotion record missing required contracts', () => {
  withFixture('missing-required-contract', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /MISSING_REQUIRED_CONTRACT/);
  });
});

test('rejects promotion record missing required metadata', () => {
  withFixture('missing-required-metadata', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /MISSING_REQUIRED_METADATA/);
  });
});

test('rejects promotion record with proposed contract status', () => {
  withFixture('promotion-contract-proposed', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /PROMOTION_CONTRACT_STATUS_NOT_APPROVED/);
  });
});

test('rejects promotion record without runtime touch', () => {
  withFixture('promotion-runtime-not-touched', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /PROMOTION_RUNTIME_NOT_TOUCHED/);
  });
});

test('rejects promotion record without dev lease', () => {
  withFixture('promotion-lease-none', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /PROMOTION_MISSING_DEV_LEASE/);
  });
});

test('rejects promotion record with fixture provenance', () => {
  withFixture('promotion-fixture-provenance', (rootDir) => {
    assert.throws(() => validateQaSchemas(rootDir), /PROMOTION_FIXTURE_OR_TEST_PROVENANCE/);
  });
});
