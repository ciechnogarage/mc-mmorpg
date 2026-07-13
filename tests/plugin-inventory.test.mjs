import assert from 'node:assert/strict';
import test from 'node:test';

import {
  auditPluginInventory,
  parsePluginManifest
} from '../scripts/plugin-inventory.mjs';

const manifest = `plugins:
  - name: ActivePlugin
    jar: ActivePlugin-1.0.0.jar
    current_version: 1.0.0
    role: test
    owner: test_owner
    license_status: internal
    update_policy: staging_required
    criticality: high
  - name: DisabledPlugin
    jar: DisabledPlugin-1.0.0.jar
    current_version: 1.0.0
    role: test
    owner: test_owner
    license_status: internal
    update_policy: staging_required
    criticality: low
    runtime_status: disabled_staging_mvp
    runtime_location: MCMMORPG/disabled-runtime-blockers/plugins
`;

test('parses the constrained plugin manifest format', () => {
  assert.deepEqual(parsePluginManifest(manifest), [
    {
      name: 'ActivePlugin',
      jar: 'ActivePlugin-1.0.0.jar',
      current_version: '1.0.0',
      role: 'test',
      owner: 'test_owner',
      license_status: 'internal',
      update_policy: 'staging_required',
      criticality: 'high'
    },
    {
      name: 'DisabledPlugin',
      jar: 'DisabledPlugin-1.0.0.jar',
      current_version: '1.0.0',
      role: 'test',
      owner: 'test_owner',
      license_status: 'internal',
      update_policy: 'staging_required',
      criticality: 'low',
      runtime_status: 'disabled_staging_mvp',
      runtime_location: 'MCMMORPG/disabled-runtime-blockers/plugins'
    }
  ]);
});

test('rejects a plugin field outside its list item', () => {
  assert.throws(
    () => parsePluginManifest(manifest.replace('    owner: test_owner', '  owner: test_owner')),
    /must be indented inside its plugin item/
  );
});

test('reconciles active manifest entries with runtime jars', () => {
  assert.deepEqual(
    auditPluginInventory(parsePluginManifest(manifest), [
      'ActivePlugin-1.0.0.jar'
    ]),
    {
      activeManifestJars: ['ActivePlugin-1.0.0.jar'],
      runtimeJars: ['ActivePlugin-1.0.0.jar'],
      missingFromManifest: [],
      missingFromRuntime: [],
      invalidEntries: []
    }
  );
});

test('reports untracked runtime jars and incomplete entries', () => {
  const incomplete = parsePluginManifest(manifest.replace('    owner: test_owner\n', ''));
  assert.deepEqual(
    auditPluginInventory(incomplete, ['ActivePlugin-1.0.0.jar', 'UnknownPlugin-1.0.0.jar']),
    {
      activeManifestJars: ['ActivePlugin-1.0.0.jar'],
      runtimeJars: ['ActivePlugin-1.0.0.jar', 'UnknownPlugin-1.0.0.jar'],
      missingFromManifest: ['UnknownPlugin-1.0.0.jar'],
      missingFromRuntime: [],
      invalidEntries: [{ name: 'ActivePlugin', missingFields: ['owner'] }]
    }
  );
});
