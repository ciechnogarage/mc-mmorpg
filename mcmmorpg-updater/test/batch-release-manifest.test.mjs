import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { chmod, mkdir, mkdtemp, readFile, rm, stat, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import {
  buildOperatorReleaseManifest,
  compareVersionDirection,
  createOperatorReleaseManifest,
  renderOperatorReleaseManifestMarkdown,
} from '../src/batch/index.mjs';
import { canonicalHash } from '../src/contract/canonical.mjs';
import { parseReleaseManifestArguments, runReleaseManifestCli } from '../src/batch/release-manifest-cli.mjs';

test('release manifest CLI accepts only a content-addressed batch audit path', async () => {
  const hash = 'a'.repeat(64);
  assert.deepEqual(parseReleaseManifestArguments(['--batch-audit', `reports/batch-download-${hash}.report.json`]), {
    batchAudit: `reports/batch-download-${hash}.report.json`,
  });
  assert.throws(() => parseReleaseManifestArguments(['--batch-audit', '../server/plugins/a.jar']), {
    code: 'batch-release-cli-arguments-invalid',
  });
  const output = await runReleaseManifestCli(['--batch-audit', `reports/batch-download-${hash}.report.json`], {
    projectRoot: '/safe/updater',
    runImpl: async (input) => ({
      manifest: { summary: { total: 0 } },
      json_path: 'reports/release.json',
      markdown_path: 'reports/release.md',
      json_reused: false,
      markdown_reused: false,
      input,
    }),
  });
  assert.equal(output.summary.total, 0);
  assert.equal(output.json_path, 'reports/release.json');
});

test('version direction compares ordinary numeric versions and ignores build metadata', () => {
  assert.equal(compareVersionDirection('1.2.3', '1.3.0').direction, 'upgrade');
  assert.equal(compareVersionDirection('2.0.0', '1.99.99').direction, 'downgrade');
  assert.equal(compareVersionDirection('1.2', '1.2.0+build.7').direction, 'same-version');
});

test('version direction handles recognized prerelease and snapshot forms conservatively', () => {
  assert.equal(compareVersionDirection('2.0.1-SNAPSHOT', '2.0.0').direction, 'downgrade');
  assert.equal(compareVersionDirection('2.0.0-SNAPSHOT', '2.0.0').direction, 'upgrade');
  assert.equal(compareVersionDirection('2.0.0', '2.0.0-rc.1').direction, 'downgrade');
  assert.equal(compareVersionDirection('2.0.0-beta.2', '2.0.0-beta.3').direction, 'upgrade');
});

test('version direction is unknown for incomparable vendor formats', () => {
  assert.equal(compareVersionDirection('2026-R2-custom', 'release-seven').direction, 'version-order-unknown');
  assert.equal(compareVersionDirection('1.0.0-paper', '1.0.1-paper').direction, 'version-order-unknown');
});

test('release manifest maps every selected download and gates future selection to upgrades', () => {
  const sourceReport = sourceFixture();
  const batchAudit = batchFixture();
  const manifest = buildOperatorReleaseManifest({
    batchAudit,
    batchAuditPath: 'reports/batch-download-audit.report.json',
    batchAuditSha256: 'a'.repeat(64),
    sourceReport,
    sourceReportPath: batchAudit.source_report.path,
  });

  assert.equal(manifest.plugins.length, 4);
  assert.deepEqual(manifest.summary, {
    total: 4,
    upgrade: 1,
    same_version: 1,
    downgrade: 1,
    version_order_unknown: 1,
    upgrade_ready: 1,
    blocked: 3,
  });
  assert.deepEqual(manifest.future_selection.map((entry) => entry.plugin), ['UpgradePlugin']);
  assert.equal(manifest.plugins.find((entry) => entry.plugin === 'MythicDungeons').direction, 'downgrade');
  assert.ok(manifest.plugins.filter((entry) => entry.direction !== 'upgrade').every((entry) => entry.upgrade_ready === false));
  assert.match(renderOperatorReleaseManifestMarkdown(manifest), /\| MythicDungeons \| 2\.0\.1-SNAPSHOT \| 2\.0\.0 \| downgrade \|/);
});

test('release manifest files are immutable, content-addressed, idempotent, and do not write server paths', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'mcmmorpg-release-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const serverRoot = join(root, 'active-server');
  const updaterRoot = join(root, 'updater');
  await mkdir(join(serverRoot, 'plugins'), { recursive: true });
  await mkdir(join(updaterRoot, 'reports'), { recursive: true });
  await writeFile(join(serverRoot, 'plugins', 'sentinel.jar'), 'server-sentinel');

  const sourceReport = sourceFixture();
  const batchAudit = batchFixture();
  for (const result of batchAudit.results.filter((entry) => entry.status === 'available')) {
    const candidate = sourceReport.plugins.find((entry) => entry.name === result.plugin_name).candidate;
    const bytes = Buffer.from(`jar:${result.plugin_name}`);
    result.sha256 = sha256(bytes);
    candidate.sha256 = result.sha256;
    candidate.integrity.sha256 = result.sha256;
    result.path = `downloads/${result.plugin_name}/${result.sha256}.jar`;
    await mkdir(join(updaterRoot, 'downloads', result.plugin_name), { recursive: true });
    await writeFile(join(updaterRoot, result.path), bytes, { mode: 0o400 });
    await chmod(join(updaterRoot, result.path), 0o444);
  }
  batchAudit.approval.selected_sha256 = batchAudit.results.map((entry) => ({
    plugin_name: entry.plugin_name,
    sha256: sourceReport.plugins.find((plugin) => plugin.name === entry.plugin_name).candidate.sha256,
  }));
  const sourcePath = await writeContentAddressedJson(updaterRoot, 'batch-discovery', sourceReport);
  batchAudit.source_report = { path: sourcePath.relative, sha256: sourcePath.hash };
  const auditPath = await writeContentAddressedJson(updaterRoot, 'batch-download', batchAudit);

  const first = await createOperatorReleaseManifest({ projectRoot: updaterRoot, batchAuditPath: auditPath.absolute });
  const second = await createOperatorReleaseManifest({ projectRoot: updaterRoot, batchAuditPath: auditPath.absolute });

  assert.equal(first.json_path, second.json_path);
  assert.equal(first.markdown_path, second.markdown_path);
  assert.equal(first.json_reused, false);
  assert.equal(second.json_reused, true);
  assert.equal((await stat(join(updaterRoot, first.json_path))).mode & 0o777, 0o444);
  assert.equal((await stat(join(updaterRoot, first.markdown_path))).mode & 0o777, 0o444);
  const json = JSON.parse(await readFile(join(updaterRoot, first.json_path), 'utf8'));
  assert.equal(json.plugins.length, 4);
  assert.equal(json.future_selection.length, 1);
  assert.equal(await readFile(join(serverRoot, 'plugins', 'sentinel.jar'), 'utf8'), 'server-sentinel');
});

function sourceFixture() {
  const definitions = [
    ['UpgradePlugin', '1.0.0', '2.0.0'],
    ['SamePlugin', '1.0.0', '1.0.0+build.2'],
    ['MythicDungeons', '2.0.1-SNAPSHOT', '2.0.0'],
    ['VendorPlugin', '1.0.0-paper', '1.0.1-paper'],
  ];
  return {
    schema_version: 1,
    coverage: { complete: true },
    plugins: definitions.map(([name, current, candidate], index) => ({
      name,
      current_version: current,
      status: 'manual-hash-approval-required',
      installations: [{ target: index % 2 ? 'items' : 'world' }],
      source: {
        adapter: 'modrinth-search-v2',
        project_id: `project-${index}`,
        project_status: 'approved',
        release_id: `release-${index}`,
        official_url: `https://modrinth.com/project/project-${index}`,
      },
      candidate: {
        version: candidate,
        source_url: `https://cdn.modrinth.com/data/project-${index}/versions/release-${index}/plugin.jar`,
        sha256: String(index + 1).repeat(64),
        integrity: { kind: 'manual-approval-required', sha256: String(index + 1).repeat(64) },
        targets: [index % 2 ? 'items' : 'world'],
      },
    })),
  };
}

function batchFixture() {
  const plugins = sourceFixture().plugins;
  return {
    schema_version: 1,
    kind: 'verified-modrinth-batch-download',
    source_report: { path: 'reports/source.report.json', sha256: 'b'.repeat(64) },
    approval: {
      scope: 'download-all-manual-hash-candidates',
      operator: 'test-operator',
      approved_at: '2026-07-20T10:00:00.000Z',
      selected_sha256: plugins.map((plugin) => ({ plugin_name: plugin.name, sha256: plugin.candidate.sha256 })),
    },
    summary: { selected: 4, available: 4, failed: 0 },
    results: plugins.map((plugin) => ({
      plugin_name: plugin.name,
      status: 'available',
      path: `downloads/${plugin.name}/${plugin.candidate.sha256}.jar`,
      sha256: plugin.candidate.sha256,
      source_url: plugin.candidate.source_url,
      plugin_yml: { name: plugin.name, version: plugin.candidate.version },
    })),
  };
}

async function writeContentAddressedJson(root, prefix, value) {
  const hash = canonicalHash(value);
  const relative = `reports/${prefix}-${hash}.report.json`;
  const absolute = join(root, relative);
  await writeFile(absolute, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o400 });
  await chmod(absolute, 0o444);
  return { hash, relative, absolute };
}

function sha256(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}
