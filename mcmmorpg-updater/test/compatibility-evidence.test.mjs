import test from 'node:test';
import assert from 'node:assert/strict';
import { classifyCompatibilityEvidence } from '../src/compatibility/evidence.mjs';

test('compatibility evidence classifies exact-listed metadata', () => {
  const compatibility = classifyCompatibilityEvidence({
    minecraftVersion: '1.21.11',
    gameVersions: ['1.21.11'],
    loaders: ['paper'],
    source: 'test',
  });
  assert.equal(compatibility.class, 'exact-listed');
  assert.equal(compatibility.compatible, true);
});

test('compatibility evidence classifies family-inferred metadata', () => {
  const compatibility = classifyCompatibilityEvidence({
    minecraftVersion: '1.21.11',
    gameVersions: ['1.21.x'],
    loaders: ['paper'],
    source: 'test',
  });
  assert.equal(compatibility.class, 'family-inferred');
  assert.equal(compatibility.compatible, true);
});

test('compatibility evidence classifies changelog-derived support', () => {
  const compatibility = classifyCompatibilityEvidence({
    minecraftVersion: '1.21.11',
    version: '2.0.0',
    datePublished: '2026-07-21T10:00:00.000Z',
    description: 'Added support for Paper 1.21.11.',
    source: 'test-changelog',
  });
  assert.equal(compatibility.class, 'changelog-derived');
  assert.equal(compatibility.compatible, true);
  assert.deepEqual(compatibility.source_update, { version: '2.0.0', date: '2026-07-21T10:00:00.000Z' });
});

test('compatibility evidence classifies negative-listed exact metadata', () => {
  const compatibility = classifyCompatibilityEvidence({
    minecraftVersion: '1.21.11',
    gameVersions: ['1.20', '1.21'],
    loaders: ['paper'],
    source: 'test',
  });
  assert.equal(compatibility.class, 'negative-listed');
  assert.equal(compatibility.compatible, false);
});

test('compatibility evidence classifies unknown when metadata is absent', () => {
  const compatibility = classifyCompatibilityEvidence({
    minecraftVersion: '1.21.11',
    gameVersions: [],
    loaders: ['paper'],
    source: 'test',
  });
  assert.equal(compatibility.class, 'unknown');
  assert.equal(compatibility.compatible, null);
});

test('Nexo newer versions are newer but not exact-compatible evidence for Paper 1.21.11', () => {
  const newer = ['1.26', '1.25.1'].map((version) => classifyCompatibilityEvidence({
    minecraftVersion: '1.21.11',
    gameVersions: [version],
    loaders: ['paper'],
    version,
    source: 'voxel-getResourceUpdates',
  }));
  const exact = ['1.21', '1.20'].map((version) => classifyCompatibilityEvidence({
    minecraftVersion: version,
    gameVersions: [version],
    loaders: ['paper'],
    version,
    source: 'voxel-getResourceUpdates',
  }));
  assert.deepEqual(newer.map((entry) => entry.class), ['negative-listed', 'negative-listed']);
  assert.deepEqual(newer.map((entry) => entry.compatible), [false, false]);
  assert.deepEqual(exact.map((entry) => entry.class), ['exact-listed', 'exact-listed']);
  assert.deepEqual(exact.map((entry) => entry.compatible), [true, true]);
});
