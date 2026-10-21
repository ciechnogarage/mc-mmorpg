const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');

const {
  aggregateVerdict,
  hashDirectory,
} = require('./check_modelengine_baseline_matrix');

function testAggregateVerdict() {
  assert.strictEqual(aggregateVerdict([
    { verdict: 'PASS' },
    { verdict: 'PASS' },
  ]), 'PASS');
  assert.strictEqual(aggregateVerdict([
    { verdict: 'PASS' },
    { verdict: 'INSUFFICIENT_EVIDENCE' },
  ]), 'INSUFFICIENT_EVIDENCE');
  assert.strictEqual(aggregateVerdict([
    { verdict: 'INSUFFICIENT_EVIDENCE' },
    { verdict: 'FAIL' },
  ]), 'FAIL');
}

function testHashDirectoryIsStableAndContentSensitive() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'modelengine-baseline-'));
  try {
    fs.mkdirSync(path.join(root, 'nested'));
    fs.writeFileSync(path.join(root, 'b.json'), '{"b":2}\n');
    fs.writeFileSync(path.join(root, 'nested', 'a.json'), '{"a":1}\n');
    const first = hashDirectory(root);
    const second = hashDirectory(root);
    assert.strictEqual(first.sha256, second.sha256);
    assert.deepStrictEqual(first.files, ['b.json', 'nested/a.json']);

    fs.writeFileSync(path.join(root, 'nested', 'a.json'), '{"a":3}\n');
    assert.notStrictEqual(first.sha256, hashDirectory(root).sha256);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

testAggregateVerdict();
testHashDirectoryIsStableAndContentSensitive();
console.log('MODELENGINE_BASELINE_MATRIX_TEST_PASS');
