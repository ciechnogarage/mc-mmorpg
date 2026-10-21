import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, stat } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import {
  buildAgentDocsIndex,
  buildOperatorDecisionReport,
  classifyModrinthVersionChannel,
  createOperatorDecisionReport,
  normalizeReleaseChannel,
  summarizeReleaseVersions,
  versionMatchesChannel,
} from '../src/batch/index.mjs';
import { canonicalHash } from '../src/contract/canonical.mjs';
import { writeBatchReport } from '../src/batch/runner.mjs';

test('release channel helpers separate stable from snapshot without fallback', () => {
  assert.equal(normalizeReleaseChannel(), 'stable');
  assert.equal(normalizeReleaseChannel('snapshot'), 'snapshot');
  assert.throws(() => normalizeReleaseChannel('latest'), /release channel/);

  const stable = { version_type: 'release', version_number: '1.2.0' };
  const snapshot = { version_type: 'beta', version_number: '1.3.0-SNAPSHOT' };
  assert.equal(classifyModrinthVersionChannel(stable), 'stable');
  assert.equal(classifyModrinthVersionChannel(snapshot), 'snapshot');
  assert.equal(versionMatchesChannel(stable, 'stable'), true);
  assert.equal(versionMatchesChannel(stable, 'snapshot'), false);
  assert.equal(versionMatchesChannel(snapshot, 'snapshot'), true);
  assert.equal(versionMatchesChannel(snapshot, 'stable'), false);
});

test('summarizeReleaseVersions sorts by vendor date and records compatibility', () => {
  const rows = summarizeReleaseVersions([
    {
      id: 'old',
      version_number: '1.0.0-beta.1',
      version_type: 'beta',
      status: 'listed',
      date_published: '2026-01-01T00:00:00.000Z',
      game_versions: ['1.21.11'],
      loaders: ['paper'],
    },
    {
      id: 'new',
      version_number: '1.0.0-beta.2',
      version_type: 'beta',
      status: 'listed',
      date_published: '2026-02-01T00:00:00.000Z',
      game_versions: ['1.21.11'],
      loaders: ['paper'],
    },
  ], { minecraftVersion: '1.21.11', loaders: ['paper'], channel: 'snapshot' });

  assert.equal(rows[0].id, 'new');
  assert.equal(rows[0].channel, 'snapshot');
  assert.equal(rows[0].compatible, true);
});

test('operator decision report is report-before-download and docs index is informational RAG input', () => {
  const discovery = fixtureDiscoveryReport();
  const decision = buildOperatorDecisionReport({
    discoveryReport: discovery,
    discoveryReportPath: 'reports/batch-discovery-a.report.json',
    discoveryReportSha256: 'a'.repeat(64),
    channel: 'stable',
    operator: 'alice',
    generatedAt: '2026-07-21T10:00:00.000Z',
  });
  assert.equal(decision.policy.downloads_stack, false);
  assert.equal(decision.policy.creates_deploy_plan, false);
  assert.equal(decision.summary.recommended, 1);
  assert.equal(decision.summary.no_change, 1);
  assert.equal(decision.summary.blocked, 1);
  assert.equal(decision.plugins.find((plugin) => plugin.name === 'UpgradeMe').recommendation, 'warto');

  const docs = buildAgentDocsIndex({
    decisionReport: decision,
    decisionReportPath: 'reports/operator-decision-a.report.json',
    decisionReportSha256: 'b'.repeat(64),
  });
  assert.equal(docs.policy.may_authorize_jar, false);
  assert.equal(docs.policy.may_authorize_executor, false);
  assert.equal(docs.policy.rag_ready, true);
  assert.ok(docs.documents.every((document) => document.trusted_use === 'informational-only'));
});

test('createOperatorDecisionReport writes immutable decision and docs artifacts', async () => {
  const projectRoot = await mkdtemp(join(tmpdir(), 'mcmmorpg-updater-decision-'));
  const reports = join(projectRoot, 'reports');
  const report = fixtureDiscoveryReport();
  const reportPath = await writeBatchReport(report, { projectRoot });
  const result = await createOperatorDecisionReport({
    projectRoot,
    discoveryReportPath: reportPath.replace(`${projectRoot}/`, ''),
    channel: 'snapshot',
    operator: 'alice',
    generatedAt: '2026-07-21T10:00:00.000Z',
  });

  assert.equal(result.channel, 'snapshot');
  assert.match(result.decision_report_path, /^reports\/operator-decision-[0-9a-f]{64}\.report\.json$/);
  assert.match(result.docs_index_path, /^reports\/agent-docs-index-[0-9a-f]{64}\.report\.json$/);
  assert.equal((await stat(join(projectRoot, result.decision_report_path))).mode & 0o777, 0o444);
  assert.equal((await stat(join(projectRoot, result.docs_index_path))).mode & 0o777, 0o444);
  const decision = JSON.parse(await readFile(join(projectRoot, result.decision_report_path), 'utf8'));
  assert.equal(canonicalHash(decision), result.decision_report_sha256);
  assert.equal(decision.policy.downloads_stack, false);
  assert.equal((await stat(reports)).isDirectory(), true);
});

function fixtureDiscoveryReport() {
  const basePlugin = {
    current_jar: 'plugins/example.jar',
    installations: [{ target: 'world', installed_version: '1.0.0' }],
    license: { status: 'MIT', url: 'https://example.test/license' },
    compatibility: { minecraft_version: '1.21.11', compatible: true, loaders: ['paper'], evidence: 'fixture' },
    source: {
      registration: 'verified-search',
      adapter: 'modrinth-search-v2',
      official_url: 'https://modrinth.com/plugin/example',
      metadata_url: 'https://api.modrinth.com/v2/project/example/version',
      project_status: 'approved',
    },
    search: null,
    rejections: [],
  };

  return {
    schema_version: 1,
    generated_at: '2026-07-21T09:00:00.000Z',
    minecraft_version: '1.21.11',
    channel: 'stable',
    provenance: {
      manifest: { plugin_count: 3, installation_count: 3, sha256: 'm'.repeat(64) },
      inventory_scanned_at: '2026-07-21T09:00:00.000Z',
      target_inventory: [],
    },
    coverage: {
      manifest_plugins: 3,
      reported_plugins: 3,
      manifest_installations: 3,
      reported_installations: 3,
      complete: true,
    },
    summary: {
      status_counts: { qualified: 0, 'manual-hash-approval-required': 2, 'premium-or-login': 0, 'internal-build': 0, 'source-not-found': 1, incompatible: 0 },
      candidate_counts: { found: 2, qualified: 0, manual_hash_approval_required: 2, incompatible: 0 },
    },
    plugins: [
      {
        ...basePlugin,
        name: 'UpgradeMe',
        status: 'manual-hash-approval-required',
        current_version: '1.0.0',
        reason: 'fixture',
        candidate: {
          version: '1.1.0',
          artifact_id: 'release1',
          source_url: 'https://cdn.modrinth.com/data/example/versions/release1/example.jar',
          sha256: 'a'.repeat(64),
          size: 100,
          path: null,
          integrity: { kind: 'manual-approval-required', sha256: 'a'.repeat(64) },
          plugin_yml: { name: 'UpgradeMe', version: '1.1.0' },
          targets: ['world'],
          eligible_for_plan: false,
          manual_approval_required: true,
        },
      },
      {
        ...basePlugin,
        name: 'SameVersion',
        status: 'manual-hash-approval-required',
        current_version: '1.0.0',
        reason: 'fixture',
        candidate: {
          version: '1.0.0',
          artifact_id: 'release2',
          source_url: 'https://cdn.modrinth.com/data/example/versions/release2/example.jar',
          sha256: 'b'.repeat(64),
          size: 100,
          path: null,
          integrity: { kind: 'manual-approval-required', sha256: 'b'.repeat(64) },
          plugin_yml: { name: 'SameVersion', version: '1.0.0' },
          targets: ['world'],
          eligible_for_plan: false,
          manual_approval_required: true,
        },
      },
      {
        ...basePlugin,
        name: 'Blocked',
        status: 'source-not-found',
        current_version: '1.0.0',
        reason: 'fixture',
        compatibility: { minecraft_version: '1.21.11', compatible: null, loaders: [], evidence: null },
        source: { registration: 'unresolved' },
        candidate: null,
      },
    ],
  };
}

test('operator decision report preserves vendor metadata and auth risk boundaries', () => {
  const discoveryReport = {
    schema_version: 1,
    generated_at: '2026-07-21T09:00:00.000Z',
    minecraft_version: '1.21.11',
    channel: 'stable',
    provenance: {
      manifest: { plugin_count: 1, installation_count: 1, sha256: 'm'.repeat(64) },
      inventory_scanned_at: '2026-07-21T09:00:00.000Z',
      target_inventory: [],
    },
    coverage: {
      manifest_plugins: 1,
      reported_plugins: 1,
      manifest_installations: 1,
      reported_installations: 1,
      complete: true,
    },
    summary: {
      status_counts: {
        qualified: 0,
        'manual-hash-approval-required': 0,
        'premium-or-login': 1,
        'internal-build': 0,
        'source-not-found': 0,
        incompatible: 0,
      },
      candidate_counts: {
        found: 0,
        qualified: 0,
        manual_hash_approval_required: 0,
        incompatible: 0,
      },
    },
    plugins: [{
      name: 'nexo',
      status: 'premium-or-login',
      current_version: '1.20',
      current_jar: 'plugins/nexo.jar',
      installations: [{ target: 'items', installed_version: '1.20' }],
      source: {
        registration: 'approved',
        adapter: 'voxel-shop-updates',
        official_url: 'https://updates.voxelplugins.com/nexo',
        metadata_url: 'https://updates.voxelplugins.com/nexo',
      },
      license: { status: 'unknown', provenance: 'plugin-manifest' },
          compatibility: {
            minecraft_version: '1.21.11',
            compatible: false,
            class: 'negative-listed',
            confidence: 'high',
            reason: 'listed versions do not include 1.21.11',
            source: 'voxel-getResourceUpdates',
            loaders: ['paper'],
            evidence: 'vendor-page-metadata-unverified',
          },
      available_versions: [{
        id: 'vendor-1.20',
        version: '1.20',
        version_type: 'release',
            channel: 'stable',
            compatible: false,
            compatibility: {
              minecraft_version: '1.21.11',
              compatible: false,
              class: 'negative-listed',
              confidence: 'high',
              reason: 'listed versions do not include 1.21.11',
              source: 'voxel-getResourceUpdates',
            },
            game_versions: [],
            loaders: [],
          }],
      vendor_metadata: {
        adapter: 'voxel-shop-updates',
        official_url: 'https://updates.voxelplugins.com/nexo',
        metadata_url: 'https://updates.voxelplugins.com/nexo',
        fetched: false,
        content_sha256: null,
        title: null,
        description: null,
        docs_urls: ['https://updates.voxelplugins.com/nexo'],
        changelog_urls: ['https://updates.voxelplugins.com/nexo'],
        auth_boundary: 'Official token flow is required before download qualification.',
        download_authorized: false,
        channel_policy: 'No fallback between stable, snapshot, and dev channels.',
        risk_notes: ['voxel-shop-token-auth-required'],
      },
      candidate: null,
      reason: 'fixture',
      search: null,
      rejections: [],
    }],
  };

  const decision = buildOperatorDecisionReport({
    discoveryReport,
    discoveryReportPath: 'reports/batch-discovery-' + 'a'.repeat(64) + '.report.json',
    discoveryReportSha256: 'a'.repeat(64),
    channel: 'stable',
    operator: 'codex',
    generatedAt: '2026-07-21T09:10:00.000Z',
  });

  assert.equal(decision.plugins[0].vendor_metadata.download_authorized, false);
  assert.ok(decision.plugins[0].risks.includes('vendor-download-not-authorized'));
  assert.ok(decision.plugins[0].risks.includes('voxel-shop-token-auth-required'));
  assert.equal(decision.plugins[0].compatibility.class, 'negative-listed');
  assert.match(decision.plugins[0].compatibility.reason, /1\.21\.11/);
  assert.equal(decision.plugins[0].available_versions[0].compatibility.class, 'negative-listed');
  const docs = buildAgentDocsIndex({
    decisionReport: decision,
    decisionReportPath: 'reports/operator-decision-' + 'b'.repeat(64) + '.report.json',
    decisionReportSha256: 'b'.repeat(64),
  });
  assert.equal(docs.documents[0].compatibility.class, 'negative-listed');
  assert.match(docs.documents[0].compatibility.reason, /1\.21\.11/);
});
