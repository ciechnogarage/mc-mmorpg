import { createHash } from 'node:crypto';
import { constants as fsConstants } from 'node:fs';
import { chmod, lstat, mkdir, open, realpath } from 'node:fs/promises';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { canonicalHash, sha256Hex } from '../contract/canonical.mjs';
import { batchFail } from './errors.mjs';

const AUDIT_NAME = /^batch-download-([0-9a-f]{64})\.report\.json$/;
const DISCOVERY_NAME = /^batch-discovery-([0-9a-f]{64})\.report\.json$/;
const SHA256 = /^[0-9a-f]{64}$/;
const MAX_REPORT_BYTES = 16 * 1024 * 1024;
const MAX_DOWNLOAD_BYTES = 512 * 1024 * 1024;

export function compareVersionDirection(currentVersion, candidateVersion) {
  if (!isVersionString(currentVersion) || !isVersionString(candidateVersion)) {
    return unknown('version is missing, empty, or contains surrounding whitespace');
  }
  if (currentVersion === candidateVersion) {
    return result('same-version', 'version strings are identical');
  }
  const current = parseComparableVersion(currentVersion);
  const candidate = parseComparableVersion(candidateVersion);
  if (!current || !candidate) {
    return unknown('one or both versions use an unrecognized vendor qualifier');
  }

  const coreOrder = compareNumericParts(current.core, candidate.core);
  if (coreOrder !== 0) {
    return result(coreOrder < 0 ? 'upgrade' : 'downgrade', 'numeric version core determines direction');
  }
  const qualifierOrder = compareQualifiers(current.qualifier, candidate.qualifier);
  if (qualifierOrder === null) {
    return unknown('prerelease qualifiers cannot be ordered conservatively');
  }
  if (qualifierOrder === 0) {
    return result('same-version', 'versions have equal precedence; build metadata is ignored');
  }
  return result(qualifierOrder < 0 ? 'upgrade' : 'downgrade', 'recognized prerelease precedence determines direction');
}

export function buildOperatorReleaseManifest({
  batchAudit,
  batchAuditPath,
  batchAuditSha256,
  sourceReport,
  sourceReportPath,
}) {
  validateReportPair(batchAudit, sourceReport);
  const sourceByName = new Map(sourceReport.plugins.map((plugin) => [plugin.name, plugin]));
  const plugins = batchAudit.results.map((download) => {
    const source = sourceByName.get(download.plugin_name);
    const direction = compareVersionDirection(source.current_version, source.candidate.version);
    const available = download.status === 'available';
    const upgradeReady = available && direction.direction === 'upgrade';
    return {
      plugin: source.name,
      current_version: source.current_version,
      candidate_version: source.candidate.version,
      direction: direction.direction,
      direction_reason: direction.reason,
      targets: [...source.candidate.targets].sort(),
      source: {
        adapter: source.source.adapter,
        project_id: source.source.project_id,
        project_status: source.source.project_status,
        release_id: source.source.release_id,
        official_url: source.source.official_url,
        artifact_url: source.candidate.source_url,
      },
      sha256: source.candidate.sha256,
      path: available ? download.path : null,
      status: available ? 'available' : 'failed',
      upgrade_ready: upgradeReady,
      gate: upgradeReady
        ? 'eligible-for-future-staging'
        : available ? 'blocked-version-direction' : 'blocked-download-unavailable',
    };
  });
  const count = (direction) => plugins.filter((plugin) => plugin.direction === direction).length;
  const futureSelection = selectFutureUpgradeCandidates(plugins);
  return {
    schema_version: 1,
    kind: 'operator-release-manifest',
    generated_at: batchAudit.approval.approved_at,
    policy: {
      version_direction_gate: 'fail-closed',
      future_selection_requires: ['status:available', 'direction:upgrade'],
      performs_staging_or_deploy: false,
    },
    batch_audit: { path: batchAuditPath, sha256: batchAuditSha256 },
    source_discovery_report: { path: sourceReportPath, sha256: batchAudit.source_report.sha256 },
    summary: {
      total: plugins.length,
      upgrade: count('upgrade'),
      same_version: count('same-version'),
      downgrade: count('downgrade'),
      version_order_unknown: count('version-order-unknown'),
      upgrade_ready: futureSelection.length,
      blocked: plugins.length - futureSelection.length,
    },
    plugins,
    future_selection: futureSelection,
  };
}

export function selectFutureUpgradeCandidates(plugins) {
  if (!Array.isArray(plugins)) batchFail('batch-release-selection-invalid', 'release plugins must be an array');
  return plugins.filter((plugin) => plugin?.status === 'available' && plugin?.direction === 'upgrade'
    && plugin?.upgrade_ready === true).map((plugin) => ({
    plugin: plugin.plugin,
    candidate_version: plugin.candidate_version,
    targets: [...plugin.targets],
    sha256: plugin.sha256,
    path: plugin.path,
  }));
}

export function renderOperatorReleaseManifestMarkdown(manifest) {
  const lines = [
    '# Operator release manifest',
    '',
    `Batch audit: \`${escapeInline(manifest.batch_audit.path)}\` (SHA-256 \`${manifest.batch_audit.sha256}\`)`,
    '',
    `Source discovery: \`${escapeInline(manifest.source_discovery_report.path)}\` (SHA-256 \`${manifest.source_discovery_report.sha256}\`)`,
    '',
    `Version gate: **fail-closed**. Upgrade-ready: **${manifest.summary.upgrade_ready}/${manifest.summary.total}**. This manifest does not stage or deploy files.`,
    '',
    '| Plugin | Current | Candidate | Direction | Targets | Source | SHA-256 | Path | Status |',
    '| --- | --- | --- | --- | --- | --- | --- | --- | --- |',
    ...manifest.plugins.map((plugin) => [
      plugin.plugin,
      plugin.current_version,
      plugin.candidate_version,
      plugin.direction,
      plugin.targets.join(', '),
      `${plugin.source.adapter}:${plugin.source.project_id}/${plugin.source.release_id}`,
      plugin.sha256,
      plugin.path ?? '—',
      `${plugin.status}; ${plugin.gate}`,
    ].map(escapeTable).join(' | ')).map((row) => `| ${row} |`),
    '',
    '## Future staging selection',
    '',
    ...(manifest.future_selection.length === 0
      ? ['None.']
      : manifest.future_selection.map((entry) => `- ${escapeInline(entry.plugin)} ${escapeInline(entry.candidate_version)} — \`${entry.sha256}\` — \`${escapeInline(entry.path)}\``)),
    '',
  ];
  return `${lines.join('\n')}\n`;
}

export async function createOperatorReleaseManifest({ projectRoot, batchAuditPath }) {
  const root = await validateRoot(projectRoot);
  const audit = await readImmutableJson({
    root,
    path: batchAuditPath,
    filenamePattern: AUDIT_NAME,
    kind: 'verified-modrinth-batch-download',
    errorPrefix: 'batch-release-audit',
  });
  const sourcePath = safeRelativeReportPath(root, audit.value.source_report?.path, DISCOVERY_NAME);
  const source = await readImmutableJson({
    root,
    path: sourcePath,
    filenamePattern: DISCOVERY_NAME,
    errorPrefix: 'batch-release-source',
  });
  if (source.sha256 !== audit.value.source_report.sha256) {
    batchFail('batch-release-source-drift', 'source discovery report hash differs from batch audit provenance');
  }
  validateReportPair(audit.value, source.value);
  await verifyAvailableDownloads(root, audit.value, source.value);
  const manifest = buildOperatorReleaseManifest({
    batchAudit: audit.value,
    batchAuditPath: toRelativePath(root, audit.path),
    batchAuditSha256: audit.sha256,
    sourceReport: source.value,
    sourceReportPath: toRelativePath(root, source.path),
  });
  const markdown = renderOperatorReleaseManifestMarkdown(manifest);
  const jsonWritten = await writeImmutableText({
    root,
    filename: `release-manifest-${canonicalHash(manifest)}.manifest.json`,
    content: `${JSON.stringify(manifest, null, 2)}\n`,
  });
  const markdownWritten = await writeImmutableText({
    root,
    filename: `release-manifest-${sha256Hex(markdown)}.manifest.md`,
    content: markdown,
  });
  return {
    manifest,
    json_path: toRelativePath(root, jsonWritten.path),
    markdown_path: toRelativePath(root, markdownWritten.path),
    json_reused: jsonWritten.reused,
    markdown_reused: markdownWritten.reused,
  };
}

function validateReportPair(batchAudit, sourceReport) {
  if (!batchAudit || batchAudit.schema_version !== 1 || batchAudit.kind !== 'verified-modrinth-batch-download'
      || batchAudit.approval?.scope !== 'download-all-manual-hash-candidates'
      || typeof batchAudit.approval?.approved_at !== 'string'
      || !Array.isArray(batchAudit.approval?.selected_sha256) || !Array.isArray(batchAudit.results)
      || batchAudit.summary?.selected !== batchAudit.results.length
      || !Number.isFinite(new Date(batchAudit.approval.approved_at).getTime())) {
    batchFail('batch-release-audit-malformed', 'batch audit is incomplete or has an unsupported schema');
  }
  if (!sourceReport || sourceReport.schema_version !== 1 || sourceReport.coverage?.complete !== true
      || !Array.isArray(sourceReport.plugins)) {
    batchFail('batch-release-source-malformed', 'source discovery report is incomplete or has an unsupported schema');
  }
  const selected = uniqueMap(batchAudit.approval.selected_sha256, 'plugin_name', 'batch-release-audit-malformed');
  const results = uniqueMap(batchAudit.results, 'plugin_name', 'batch-release-audit-malformed');
  if (selected.size !== results.size || selected.size !== batchAudit.summary.selected) {
    batchFail('batch-release-audit-malformed', 'batch selected/result coverage is incomplete');
  }
  const available = batchAudit.results.filter((entry) => entry.status === 'available').length;
  const failed = batchAudit.results.filter((entry) => entry.status === 'failed').length;
  if (batchAudit.summary.available !== available || batchAudit.summary.failed !== failed) {
    batchFail('batch-release-audit-malformed', 'batch summary does not match result statuses');
  }
  const source = uniqueMap(sourceReport.plugins, 'name', 'batch-release-source-malformed');
  for (const [name, approval] of selected) {
    const download = results.get(name);
    const discovered = source.get(name);
    if (!download || !discovered || discovered.status !== 'manual-hash-approval-required'
        || !isVersionString(discovered.current_version) || !isVersionString(discovered.candidate?.version)
        || !SHA256.test(discovered.candidate?.sha256 ?? '')
        || discovered.candidate?.integrity?.kind !== 'manual-approval-required'
        || discovered.candidate?.integrity?.sha256 !== discovered.candidate.sha256
        || approval.sha256 !== discovered.candidate.sha256
        || !Array.isArray(discovered.candidate.targets) || discovered.candidate.targets.length === 0
        || !discovered.candidate.targets.every((target) => typeof target === 'string' && target.length > 0)
        || discovered.source?.project_status !== 'approved'
        || typeof discovered.source?.adapter !== 'string' || typeof discovered.source?.project_id !== 'string'
        || typeof discovered.source?.release_id !== 'string' || !isHttpsUrl(discovered.source?.official_url)
        || !isHttpsUrl(discovered.candidate?.source_url)) {
      batchFail('batch-release-provenance-drift', `release provenance is incomplete or inconsistent for ${name}`);
    }
    if (download.status === 'available') {
      if (download.sha256 !== approval.sha256 || download.source_url !== discovered.candidate.source_url
          || typeof download.path !== 'string' || download.path.length === 0) {
        batchFail('batch-release-provenance-drift', `download result differs from approved candidate for ${name}`);
      }
    } else if (download.status !== 'failed') {
      batchFail('batch-release-audit-malformed', `unsupported download status for ${name}`);
    }
  }
}

async function verifyAvailableDownloads(root, batchAudit, sourceReport) {
  const sources = new Map(sourceReport.plugins.map((plugin) => [plugin.name, plugin]));
  for (const download of batchAudit.results) {
    if (download.status !== 'available') continue;
    const expected = sources.get(download.plugin_name).candidate.sha256;
    const path = safeDownloadPath(root, download.path, expected);
    const metadata = await lstat(path);
    if (!metadata.isFile() || metadata.isSymbolicLink() || metadata.size > MAX_DOWNLOAD_BYTES
        || (metadata.mode & 0o222) !== 0) {
      batchFail('batch-release-download-unsafe', `download is not a bounded immutable regular file: ${download.plugin_name}`);
    }
    let handle;
    try {
      handle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
      const hash = createHash('sha256');
      for await (const chunk of handle.createReadStream({ autoClose: false })) hash.update(chunk);
      if (hash.digest('hex') !== expected) {
        batchFail('batch-release-download-drift', `download hash no longer matches audit: ${download.plugin_name}`);
      }
    } finally {
      await handle?.close().catch(() => {});
    }
  }
}

async function readImmutableJson({ root, path, filenamePattern, kind, errorPrefix }) {
  if (typeof path !== 'string' || !isAbsolute(path) || !isWithin(join(root, 'reports'), path)) {
    batchFail(`${errorPrefix}-unsafe`, 'report must be an absolute path below updater reports');
  }
  const match = filenamePattern.exec(path.split(sep).at(-1));
  if (!match) batchFail(`${errorPrefix}-unsafe`, 'report filename is not content-addressed');
  const metadata = await lstat(path);
  if (!metadata.isFile() || metadata.isSymbolicLink() || metadata.size > MAX_REPORT_BYTES || (metadata.mode & 0o222) !== 0) {
    batchFail(`${errorPrefix}-unsafe`, 'report must be bounded, immutable, and non-symlink');
  }
  let handle;
  try {
    handle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    let value;
    try { value = JSON.parse((await handle.readFile()).toString('utf8')); } catch {
      batchFail(`${errorPrefix}-malformed`, 'report is not valid JSON');
    }
    const hash = canonicalHash(value);
    if (hash !== match[1]) batchFail(`${errorPrefix}-drift`, 'report content does not match filename hash');
    if (kind && value.kind !== kind) batchFail(`${errorPrefix}-malformed`, 'report kind is unsupported');
    return { value, sha256: hash, path };
  } finally {
    await handle?.close().catch(() => {});
  }
}

async function writeImmutableText({ root, filename, content }) {
  const reportsRoot = join(root, 'reports');
  await mkdir(reportsRoot, { recursive: true, mode: 0o700 });
  if (await realpath(reportsRoot) !== reportsRoot) batchFail('batch-release-report-root-unsafe', 'reports root resolves through a symlink');
  const path = join(reportsRoot, filename);
  let handle;
  try {
    handle = await open(path, fsConstants.O_WRONLY | fsConstants.O_CREAT | fsConstants.O_EXCL | fsConstants.O_NOFOLLOW, 0o400);
  } catch (error) {
    if (error?.code !== 'EEXIST') throw error;
    const existing = await readImmutableText(path);
    if (!existing.equals(Buffer.from(content, 'utf8'))) {
      batchFail('batch-release-existing-manifest-mismatch', 'existing immutable release manifest content is inconsistent');
    }
    return { path, reused: true };
  }
  try {
    await handle.writeFile(content, 'utf8');
    await handle.sync();
  } finally {
    await handle.close();
  }
  await chmod(path, 0o444);
  return { path, reused: false };
}

async function readImmutableText(path) {
  const metadata = await lstat(path);
  if (!metadata.isFile() || metadata.isSymbolicLink() || metadata.size > MAX_REPORT_BYTES || (metadata.mode & 0o222) !== 0) {
    batchFail('batch-release-existing-manifest-mismatch', 'existing release manifest is not immutable');
  }
  let handle;
  try {
    handle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    return await handle.readFile();
  } finally {
    await handle?.close().catch(() => {});
  }
}

async function validateRoot(projectRoot) {
  if (typeof projectRoot !== 'string' || !isAbsolute(projectRoot)) batchFail('batch-release-root-unsafe', 'project root must be absolute');
  const root = resolve(projectRoot);
  if (await realpath(root) !== root) batchFail('batch-release-root-unsafe', 'project root must be canonical');
  return root;
}

function safeRelativeReportPath(root, value, pattern) {
  if (typeof value !== 'string' || isAbsolute(value) || !pattern.test(value.split('/').at(-1))) {
    batchFail('batch-release-source-unsafe', 'source report path is not a safe content-addressed relative path');
  }
  const path = resolve(root, value);
  if (!isWithin(join(root, 'reports'), path)) batchFail('batch-release-source-unsafe', 'source report escaped updater reports');
  return path;
}

function safeDownloadPath(root, value, sha256) {
  if (typeof value !== 'string' || isAbsolute(value) || value.split('/').at(-1) !== `${sha256}.jar`) {
    batchFail('batch-release-download-unsafe', 'download path is not content-addressed');
  }
  const path = resolve(root, value);
  if (!isWithin(join(root, 'downloads'), path)) batchFail('batch-release-download-unsafe', 'download path escaped updater downloads');
  return path;
}

function parseComparableVersion(value) {
  const match = /^v?(\d+(?:\.\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$/.exec(value);
  if (!match) return null;
  const qualifier = parseQualifier(match[2]);
  if (!qualifier) return null;
  return { core: match[1].split('.').map((part) => BigInt(part)), qualifier };
}

function parseQualifier(value) {
  if (value === undefined || /^(release|final|ga)$/i.test(value)) return { channel: 'stable', number: null };
  const match = /^(snapshot|alpha|beta|rc)(?:[.-]?(\d+))?$/i.exec(value);
  if (!match) return null;
  return { channel: match[1].toLowerCase(), number: match[2] === undefined ? null : BigInt(match[2]) };
}

function compareNumericParts(left, right) {
  for (let index = 0; index < Math.max(left.length, right.length); index += 1) {
    const a = left[index] ?? 0n;
    const b = right[index] ?? 0n;
    if (a < b) return -1;
    if (a > b) return 1;
  }
  return 0;
}

function compareQualifiers(left, right) {
  const ranks = { snapshot: 0, alpha: 1, beta: 2, rc: 3, stable: 4 };
  if (ranks[left.channel] !== ranks[right.channel]) return ranks[left.channel] < ranks[right.channel] ? -1 : 1;
  if (left.channel === 'stable') return 0;
  if (left.number === null && right.number === null) return 0;
  if (left.number === null) return -1;
  if (right.number === null) return 1;
  if (left.number < right.number) return -1;
  if (left.number > right.number) return 1;
  return 0;
}

function result(direction, reason) {
  return { direction, comparable: direction !== 'version-order-unknown', reason };
}

function unknown(reason) {
  return result('version-order-unknown', reason);
}

function isVersionString(value) {
  return typeof value === 'string' && value.length > 0 && value.trim() === value;
}

function isHttpsUrl(value) {
  try { return typeof value === 'string' && new URL(value).protocol === 'https:'; } catch { return false; }
}

function uniqueMap(entries, key, code) {
  const result = new Map();
  for (const entry of entries) {
    const value = entry?.[key];
    if (typeof value !== 'string' || value.length === 0 || result.has(value)) {
      batchFail(code, `entries must have unique non-empty ${key}`);
    }
    result.set(value, entry);
  }
  return result;
}

function escapeInline(value) {
  return String(value).replaceAll('`', '\\`').replaceAll('\n', ' ');
}

function escapeTable(value) {
  return String(value).replaceAll('\\', '\\\\').replaceAll('|', '\\|').replaceAll('\n', ' ');
}

function toRelativePath(root, path) {
  const rel = relative(root, path);
  if (rel === '' || rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) {
    batchFail('batch-release-root-unsafe', 'manifest path escaped updater root');
  }
  return rel.split(sep).join('/');
}

function isWithin(root, candidate) {
  const rel = relative(resolve(root), resolve(candidate));
  return rel === '' || (!rel.startsWith(`..${sep}`) && rel !== '..' && !isAbsolute(rel));
}
