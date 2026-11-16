import { canonicalHash } from '../contract/canonical.mjs';
import { constants as fsConstants } from 'node:fs';
import { chmod, lstat, mkdir, open, readdir, realpath } from 'node:fs/promises';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { loadTrustedSourcePolicy } from '../downloads/source-policy.mjs';
import { normalizeReleaseChannel } from './channel.mjs';
import { scanInventory } from '../inventory/index.mjs';
import { loadRegistry } from '../registry/index.mjs';
import { discoverBatchOutcomes } from './discovery.mjs';
import { batchFail } from './errors.mjs';
import { importManifestInventory } from './inventory.mjs';
import { loadPluginManifest } from './manifest.mjs';
import { loadBatchSourceRegistrations } from './runner.mjs';
import { createOperatorReleaseManifest } from './release-manifest.mjs';

const REPORT_NAME = /^batch-discovery-([0-9a-f]{64})\.report\.json$/;
const MAX_REPORT_BYTES = 16 * 1024 * 1024;

export async function persistVerifiedModrinthBatch({
  projectRoot,
  reportPath,
  manifestPath,
  targetsPath,
  pluginsPath,
  registrationsPath,
  policyPath,
  operator,
  channel = 'stable',
  approvedAt = new Date().toISOString(),
  confirmDownloadAll = false,
  expectedPolicyOwnerUid = typeof process.getuid === 'function' ? process.getuid() : undefined,
  lookupImpl,
  fetchImpl,
}) {
  if (confirmDownloadAll !== true) {
    batchFail('batch-persist-confirmation-required', 'explicit download-all confirmation is required');
  }
  if (typeof operator !== 'string' || operator.trim() !== operator || operator.length === 0) {
    batchFail('batch-persist-operator-invalid', 'operator must be a non-empty string without surrounding whitespace');
  }
  const normalizedChannel = normalizeReleaseChannel(channel);
  const timestamp = new Date(approvedAt);
  if (!Number.isFinite(timestamp.getTime())) batchFail('batch-persist-approval-invalid', 'approvedAt is invalid');

  const [sourceReport, manifest, registry, registrations, policy] = await Promise.all([
    loadImmutableDiscoveryReport({ projectRoot, reportPath }),
    loadPluginManifest(manifestPath),
    loadRegistry({ targetsPath, pluginsPath }),
    loadBatchSourceRegistrations(registrationsPath),
    loadTrustedSourcePolicy(policyPath, { expectedOwnerUid: expectedPolicyOwnerUid }),
  ]);
  const root = await validatePersistRoot(projectRoot, registry);
  const [inventory, registryInventory, existingDownloads] = await Promise.all([
    importManifestInventory({ manifest, targets: registry.targets.targets, scannedAt: timestamp.toISOString() }),
    scanInventory(registry, { scannedAt: timestamp.toISOString() }),
    listExistingDownloads(root),
  ]);
  const selected = validateSourceReportCurrent(sourceReport.report, { manifest, inventory });
  const selection = new Set(selected.map((entry) => entry.name));
  const approvedCandidates = new Map(selected.map((entry) => [entry.name, entry]));
  const outcomes = await discoverBatchOutcomes({
    inventory,
    registrations,
    registry,
    registryInventory,
    policy,
    projectRoot: root,
    operator,
    lookupImpl,
    fetchImpl,
    approvedCandidates,
    selection,
    approvedBy: operator,
    approvedAt: timestamp.toISOString(),
    channel: normalizedChannel,
  });

  const results = [];
  for (const approved of selected) {
    const outcome = outcomes.get(approved.name);
    if (outcome?.kind !== 'candidate' || outcome.candidate?.path === null
        || outcome.candidate?.eligible_for_plan !== true
        || outcome.candidate?.integrity?.kind !== 'manual-hash-approval') {
      results.push({
        plugin_name: approved.name,
        status: 'failed',
        candidate: null,
        report_path: null,
        report_reused: false,
        error: outcome?.error ?? {
          code: outcome?.rejections?.at(-1)?.reason ?? 'batch-persist-candidate-failed',
          message: outcome?.reason ?? 'candidate did not pass live revalidation',
        },
      });
      continue;
    }
    const persistedBefore = existingDownloads.has(outcome.candidate.path);
    const pluginReport = {
      schema_version: 1,
      kind: 'verified-modrinth-batch-plugin',
      source_report_sha256: sourceReport.sha256,
      plugin_name: approved.name,
      source: structuredClone(outcome.source),
      compatibility: structuredClone(outcome.compatibility),
      candidate: {
        ...structuredClone(outcome.candidate),
        integrity: { kind: 'manual-hash-approval', sha256: outcome.candidate.sha256 },
      },
      status: 'available',
    };
    const written = await writeImmutableReport(pluginReport, {
      projectRoot: root,
      prefix: 'batch-download-plugin',
    });
    results.push({
      plugin_name: approved.name,
      status: persistedBefore ? 'already-present' : 'persisted',
      candidate: structuredClone(outcome.candidate),
      report_path: toRelativePath(root, written.path),
      report_reused: written.reused,
      error: null,
    });
  }

  const summary = {
    selected: selected.length,
    persisted: results.filter((result) => result.status === 'persisted').length,
    already_present: results.filter((result) => result.status === 'already-present').length,
    failed: results.filter((result) => result.status === 'failed').length,
  };
  const batchReport = {
    schema_version: 1,
    kind: 'verified-modrinth-batch-download',
    source_report: {
      path: toRelativePath(root, sourceReport.path),
      sha256: sourceReport.sha256,
    },
    approval: {
      scope: 'download-all-manual-hash-candidates',
      operator,
      channel: normalizedChannel,
      approved_at: timestamp.toISOString(),
      selected_sha256: selected.map((entry) => ({ plugin_name: entry.name, sha256: entry.candidate.sha256 })),
    },
    summary: {
      selected: summary.selected,
      available: summary.persisted + summary.already_present,
      failed: summary.failed,
    },
    results: results.map((result) => result.status === 'failed'
      ? { plugin_name: result.plugin_name, status: 'failed', error: result.error }
      : {
        plugin_name: result.plugin_name,
        status: 'available',
        path: result.candidate.path,
        sha256: result.candidate.sha256,
        source_url: result.candidate.source_url,
        plugin_yml: result.candidate.plugin_yml,
        plugin_report_path: result.report_path,
      }),
  };
  const batchWritten = await writeImmutableReport(batchReport, { projectRoot: root, prefix: 'batch-download' });
  const releaseManifest = await createOperatorReleaseManifest({
    projectRoot: root,
    batchAuditPath: batchWritten.path,
  });
  return {
    schema_version: 1,
    source_report_sha256: sourceReport.sha256,
    summary,
    results,
    batch_report_path: toRelativePath(root, batchWritten.path),
    batch_report_reused: batchWritten.reused,
    release_manifest_json_path: releaseManifest.json_path,
    release_manifest_markdown_path: releaseManifest.markdown_path,
    release_manifest_json_reused: releaseManifest.json_reused,
    release_manifest_markdown_reused: releaseManifest.markdown_reused,
  };
}

async function loadImmutableDiscoveryReport({ projectRoot, reportPath }) {
  const root = resolve(projectRoot);
  const absolute = resolve(reportPath);
  if (!isAbsolute(projectRoot) || !isAbsolute(reportPath) || await realpath(root) !== root
      || !isWithin(join(root, 'reports'), absolute)) {
    batchFail('batch-persist-report-unsafe', 'discovery report must be an absolute path below updater reports');
  }
  const match = REPORT_NAME.exec(absolute.split(sep).at(-1));
  if (!match) batchFail('batch-persist-report-unsafe', 'discovery report filename is not content-addressed');
  const metadata = await lstat(absolute);
  if (!metadata.isFile() || metadata.isSymbolicLink() || metadata.size > MAX_REPORT_BYTES || (metadata.mode & 0o222) !== 0) {
    batchFail('batch-persist-report-unsafe', 'discovery report must be bounded, immutable, and non-symlink');
  }
  let handle;
  try {
    handle = await open(absolute, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    const bytes = await handle.readFile();
    let report;
    try { report = JSON.parse(bytes.toString('utf8')); } catch { batchFail('batch-persist-report-malformed', 'discovery report is not valid JSON'); }
    const hash = canonicalHash(report);
    if (hash !== match[1]) batchFail('batch-persist-report-drift', 'discovery report content does not match its filename hash');
    return { report, sha256: hash, path: absolute };
  } finally {
    await handle?.close().catch(() => {});
  }
}

function validateSourceReportCurrent(report, { manifest, inventory }) {
  if (!report || report.schema_version !== 1 || report.coverage?.complete !== true
      || report.provenance?.manifest?.sha256 !== manifest.provenance.sha256
      || report.minecraft_version !== manifest.server.target_minecraft_version
      || report.coverage.manifest_plugins !== manifest.plugins.length
      || report.coverage.manifest_installations !== inventory.installation_count
      || !Array.isArray(report.plugins)) {
    batchFail('batch-persist-report-drift', 'discovery report no longer matches current manifest/inventory coverage');
  }
  const selected = report.plugins.filter((entry) => entry.status === 'manual-hash-approval-required');
  if (selected.length === 0) batchFail('batch-persist-report-empty', 'discovery report has no manual-hash candidates');
  for (const approved of selected) {
    if (approved.source?.project_status !== 'approved'
        || !['modrinth-search-v2', 'modrinth-project-versions'].includes(approved.source?.adapter)
        || approved.compatibility?.compatible !== true || !approved.compatibility.loaders?.includes('paper')
        || approved.compatibility.minecraft_version !== manifest.server.target_minecraft_version
        || approved.candidate?.path !== null || approved.candidate?.manual_approval_required !== true
        || approved.candidate?.eligible_for_plan !== false
        || approved.candidate?.integrity?.kind !== 'manual-approval-required'
        || approved.candidate.integrity.sha256 !== approved.candidate.sha256
        || !/^[0-9a-f]{64}$/.test(approved.candidate.sha256 ?? '')) {
      batchFail('batch-persist-report-drift', `report candidate is not eligible for verified persist: ${approved.name}`);
    }
    const current = inventory.plugins.find((entry) => entry.manifest.name === approved.name);
    if (!current || !sameInstallations(approved.installations, current.installations)) {
      batchFail('batch-persist-report-drift', `current target inventory drifted for ${approved.name}`);
    }
  }
  return selected;
}

function sameInstallations(approved, current) {
  if (!Array.isArray(approved) || !Array.isArray(current) || approved.length !== current.length) return false;
  const compact = (entries) => entries.map((entry) => ({
    target: entry.target,
    installed_version: entry.installed_version,
    path: entry.jar?.path,
    sha256: entry.jar?.sha256,
    plugin_name: entry.jar?.plugin_yml?.name,
    plugin_version: entry.jar?.plugin_yml?.version,
    plugin_status: entry.jar?.plugin_yml?.status,
  })).sort((left, right) => left.target.localeCompare(right.target));
  return JSON.stringify(compact(approved)) === JSON.stringify(compact(current));
}

async function listExistingDownloads(projectRoot) {
  const root = join(projectRoot, 'downloads');
  const result = new Set();
  let pluginDirectories;
  try { pluginDirectories = await readdir(root, { withFileTypes: true }); } catch (error) {
    if (error?.code === 'ENOENT') return result;
    throw error;
  }
  for (const plugin of pluginDirectories) {
    if (!plugin.isDirectory() || plugin.isSymbolicLink()) continue;
    for (const file of await readdir(join(root, plugin.name), { withFileTypes: true })) {
      if (file.isFile() && !file.isSymbolicLink()) result.add(`downloads/${plugin.name}/${file.name}`);
    }
  }
  return result;
}

async function writeImmutableReport(report, { projectRoot, prefix }) {
  const reportsRoot = join(projectRoot, 'reports');
  await mkdir(reportsRoot, { recursive: true, mode: 0o700 });
  if (await realpath(reportsRoot) !== reportsRoot) batchFail('batch-persist-report-root-unsafe', 'reports root resolves through a symlink');
  const hash = canonicalHash(report);
  const path = join(reportsRoot, `${prefix}-${hash}.report.json`);
  let handle;
  try {
    handle = await open(path, fsConstants.O_WRONLY | fsConstants.O_CREAT | fsConstants.O_EXCL | fsConstants.O_NOFOLLOW, 0o400);
  } catch (error) {
    if (error?.code !== 'EEXIST') throw error;
    const existing = await readExistingImmutable(path);
    if (canonicalHash(existing) !== hash) batchFail('batch-persist-existing-report-mismatch', 'existing immutable report content is inconsistent');
    return { path, reused: true };
  }
  try {
    await handle.writeFile(`${JSON.stringify(report, null, 2)}\n`, 'utf8');
    await handle.sync();
  } finally {
    await handle.close();
  }
  await chmod(path, 0o444);
  return { path, reused: false };
}

async function readExistingImmutable(path) {
  const metadata = await lstat(path);
  if (!metadata.isFile() || metadata.isSymbolicLink() || (metadata.mode & 0o222) !== 0) {
    batchFail('batch-persist-existing-report-mismatch', 'existing report is not immutable');
  }
  let handle;
  try {
    handle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    return JSON.parse((await handle.readFile()).toString('utf8'));
  } catch {
    batchFail('batch-persist-existing-report-mismatch', 'existing report cannot be verified');
  } finally {
    await handle?.close().catch(() => {});
  }
}

async function validatePersistRoot(projectRoot, registry) {
  if (typeof projectRoot !== 'string' || !isAbsolute(projectRoot)) batchFail('batch-persist-root-unsafe', 'project root must be absolute');
  const root = resolve(projectRoot);
  if (await realpath(root) !== root) batchFail('batch-persist-root-unsafe', 'project root must be canonical');
  const forbidden = [registry.targets.allowed_host_root,
    ...Object.values(registry.targets.targets).map((target) => target.host_data_path)];
  if (forbidden.some((path) => isWithin(path, root) || isWithin(root, path))) {
    batchFail('batch-persist-root-unsafe', 'project root overlaps an MC server path');
  }
  return root;
}

function toRelativePath(root, path) {
  const rel = relative(root, path);
  if (rel === '' || rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) {
    batchFail('batch-persist-root-unsafe', 'persisted path escaped updater root');
  }
  return rel.split(sep).join('/');
}

function isWithin(root, candidate) {
  const rel = relative(resolve(root), resolve(candidate));
  return rel === '' || (!rel.startsWith(`..${sep}`) && rel !== '..' && !isAbsolute(rel));
}
