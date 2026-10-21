import test from 'node:test';
import assert from 'node:assert/strict';
import { buildBatchReport, parsePluginManifest } from '../src/batch/index.mjs';

function inventoryFixture() {
  const manifest = parsePluginManifest(`server:
  target_minecraft_version: 1.21.11
plugins:
  - name: QualifiedPlugin
    jar: QualifiedPlugin-1.0.jar
    current_version: 1.0
    license_status: unknown
  - name: ManualPlugin
    jar: ManualPlugin-1.0.jar
    current_version: 1.0
    license_status: unknown
  - name: PremiumPlugin
    jar: PremiumPlugin-1.0.jar
    current_version: 1.0
    license_status: unknown
  - name: InternalPlugin
    jar: InternalPlugin-1.0.jar
    current_version: 1.0
    license_status: internal
    source_path: MCMMORPG/plugin-src/internal-plugin
  - name: MissingPlugin
    jar: MissingPlugin-1.0.jar
    current_version: 1.0
    license_status: unknown
  - name: IncompatiblePlugin
    jar: IncompatiblePlugin-1.0.jar
    current_version: 1.0
    license_status: unknown
`, { path: '/fixtures/plugin_manifest.yaml' });
  return {
    schema_version: 1,
    scanned_at: '2026-07-20T10:00:00.000Z',
    manifest: { ...manifest.provenance, plugin_count: manifest.plugins.length },
    minecraft_version: manifest.server.target_minecraft_version,
    installation_count: 2,
    targets: [
      { target: 'items', installation_count: 1 },
      { target: 'world', installation_count: 1 },
    ],
    plugins: manifest.plugins.map((plugin, index) => ({
      manifest: { ...plugin },
      installations: index < 2 ? [{ target: index === 0 ? 'world' : 'items' }] : [],
    })),
  };
}

function publicRegistration(manifestName, pluginId) {
  return {
    manifest_name: manifestName,
    plugin_id: pluginId,
    kind: 'public-source',
    source_id: `${pluginId}-official`,
    adapter: 'modrinth-project-versions',
    project: `official/${pluginId}`,
    project_id: `${pluginId}Project`,
    official_url: `https://modrinth.com/plugin/${pluginId}`,
    compatibility: {
      loaders: ['paper'],
      release_types: ['release'],
    },
  };
}

function candidateOutcome(pluginId, eligible) {
  return {
    kind: 'candidate',
    source: {
      source_id: `${pluginId}-official`,
      adapter: 'modrinth-project-versions',
      project: `official/${pluginId}`,
      project_id: `${pluginId}Project`,
      official_url: `https://modrinth.com/plugin/${pluginId}`,
      metadata_url: `https://api.modrinth.com/v2/project/${pluginId}Project/version`,
    },
    license: { status: 'unknown', provenance: 'plugin-manifest' },
    compatibility: {
      minecraft_version: '1.21.11',
      compatible: true,
      loaders: ['paper'],
      evidence: 'official-release-metadata',
    },
    candidate: {
      version: '2.0.0',
      artifact_id: `${pluginId}Version`,
      source_url: `https://cdn.modrinth.com/data/${pluginId}Project/versions/${pluginId}Version/${pluginId}.jar`,
      sha256: 'a'.repeat(64),
      plugin_yml: { name: pluginId, version: '2.0.0' },
      targets: pluginId === 'qualified' ? ['world'] : ['items'],
      eligible_for_plan: eligible,
      manual_approval_required: !eligible,
    },
  };
}

test('batch report classifies every plugin and installation into the six closed statuses', () => {
  const report = buildBatchReport({
    inventory: inventoryFixture(),
    registrations: {
      schema_version: 1,
      registrations: [
        publicRegistration('QualifiedPlugin', 'qualified'),
        publicRegistration('ManualPlugin', 'manual'),
        {
          manifest_name: 'PremiumPlugin',
          plugin_id: 'premium',
kind: 'premium-or-login',
adapter: 'mythiccraft-resource',
          reason: 'Official source requires a purchased license and authenticated download.',
          official_url: 'https://vendor.example.test/premium',
        },
        publicRegistration('IncompatiblePlugin', 'incompatible'),
      ],
    },
    outcomes: new Map([
      ['QualifiedPlugin', candidateOutcome('qualified', true)],
      ['ManualPlugin', candidateOutcome('manual', false)],
      ['IncompatiblePlugin', {
        kind: 'incompatible',
        source: candidateOutcome('incompatible', false).source,
        license: { status: 'unknown', provenance: 'plugin-manifest' },
        compatibility: {
          minecraft_version: '1.21.11',
          compatible: false,
          loaders: ['paper'],
          evidence: 'official-release-metadata',
        },
        reason: 'No listed release supports Minecraft 1.21.11 and an approved loader.',
      }],
      ['MissingPlugin', {
        kind: 'source-not-found',
        source: { registration: 'verified-search', adapter: 'modrinth-search-v2' },
        license: { status: 'unknown', provenance: 'plugin-manifest' },
        compatibility: { minecraft_version: '1.21.11', compatible: null, loaders: ['paper'], evidence: null },
        reason: 'No approved exact-identity project passed all gates.',
        search: { query: 'MissingPlugin', total_hits: 1, returned_hits: 1 },
        rejections: [{ stage: 'project', project_id: 'badProject', reason: 'project-status-not-approved' }],
      }],
    ]),
    generatedAt: '2026-07-20T10:05:00.000Z',
  });

  assert.equal(report.schema_version, 1);
  assert.deepEqual(report.coverage, {
    manifest_plugins: 6,
    reported_plugins: 6,
    manifest_installations: 2,
    reported_installations: 2,
    complete: true,
  });
  assert.deepEqual(report.plugins.map((plugin) => [plugin.name, plugin.status]), [
    ['QualifiedPlugin', 'qualified'],
    ['ManualPlugin', 'manual-hash-approval-required'],
    ['PremiumPlugin', 'premium-or-login'],
    ['InternalPlugin', 'internal-build'],
    ['MissingPlugin', 'source-not-found'],
    ['IncompatiblePlugin', 'incompatible'],
  ]);
  assert.deepEqual(report.summary.status_counts, {
    qualified: 1,
    'manual-hash-approval-required': 1,
    'premium-or-login': 1,
    'internal-build': 1,
    'source-not-found': 1,
    incompatible: 1,
  });
  assert.deepEqual(report.summary.candidate_counts, {
    found: 2,
    qualified: 1,
    manual_hash_approval_required: 1,
    incompatible: 1,
  });
  assert.ok(report.plugins.every((plugin) => plugin.source && plugin.license && plugin.compatibility));
  assert.equal(report.plugins[0].candidate.sha256, 'a'.repeat(64));
  assert.equal(report.plugins[2].candidate, null);
  assert.equal(report.plugins[3].source.source_path, 'MCMMORPG/plugin-src/internal-plugin');
  assert.equal(report.plugins[4].search.query, 'MissingPlugin');
  assert.equal(report.plugins[4].rejections[0].reason, 'project-status-not-approved');
});
