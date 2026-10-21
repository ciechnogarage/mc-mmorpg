import { canonicalHash, sha256Hex } from '../contract/canonical.mjs';
import { constants as fsConstants } from 'node:fs';
import { chmod, mkdir, open, realpath, stat } from 'node:fs/promises';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { batchFail } from './errors.mjs';
import { normalizeReleaseChannel } from './channel.mjs';
import { compareVersionDirection } from './release-manifest.mjs';

const DISCOVERY_NAME = /^batch-discovery-([0-9a-f]{64})\.report\.json$/;
const MAX_REPORT_BYTES = 16 * 1024 * 1024;

export async function createOperatorDecisionReport({ projectRoot, discoveryReportPath, channel = 'stable', operator, generatedAt = new Date().toISOString() }) {
  const root = await validateRoot(projectRoot);
  const normalizedChannel = normalizeReleaseChannel(channel);
  validateOperator(operator);

  const discovery = await readDiscoveryReport(root, discoveryReportPath);
  const decision = buildOperatorDecisionReport({
    discoveryReport: discovery.report,
    discoveryReportPath: toRelativePath(root, discovery.path),
    discoveryReportSha256: discovery.sha256,
    channel: normalizedChannel,
    operator,
    generatedAt,
  });
  const decisionWritten = await writeImmutableReport(decision, { root, prefix: 'operator-decision' });

  const docsIndex = buildAgentDocsIndex({
    decisionReport: decision,
    decisionReportPath: toRelativePath(root, decisionWritten.path),
    decisionReportSha256: decisionWritten.sha256,
  });
  const docsWritten = await writeImmutableReport(docsIndex, { root, prefix: 'agent-docs-index' });

  return {
    schema_version: 1,
    channel: normalizedChannel,
    decision_report_path: toRelativePath(root, decisionWritten.path),
    decision_report_sha256: decisionWritten.sha256,
    docs_index_path: toRelativePath(root, docsWritten.path),
    docs_index_sha256: docsWritten.sha256,
    summary: decision.summary,
  };
}

export function buildOperatorDecisionReport({ discoveryReport, discoveryReportPath, discoveryReportSha256, channel = 'stable', operator, generatedAt = new Date().toISOString() }) {
  const normalizedChannel = normalizeReleaseChannel(channel);
  validateOperator(operator);
  validateDiscoveryReport(discoveryReport);
  const timestamp = new Date(generatedAt);
  if (!Number.isFinite(timestamp.getTime())) batchFail('batch-decision-generated-at-invalid', 'decision report timestamp invalid');

  const plugins = discoveryReport.plugins.map((plugin) => analyzePlugin(plugin, normalizedChannel));
  const summary = {
    total: plugins.length,
    recommended: plugins.filter((plugin) => plugin.recommendation === 'warto').length,
    no_change: plugins.filter((plugin) => plugin.recommendation === 'nie-ruszac').length,
    blocked: plugins.filter((plugin) => plugin.recommendation === 'blokada').length,
    compatible_candidates: plugins.filter((plugin) => plugin.compatibility.compatible === true).length,
  };

  return {
    schema_version: 1,
    kind: 'operator-update-decision-report',
    generated_at: timestamp.toISOString(),
    operator,
    channel: normalizedChannel,
    minecraft_version: discoveryReport.minecraft_version,
    source_discovery_report: {
      path: discoveryReportPath,
      sha256: discoveryReportSha256,
    },
    policy: {
      stage: 'report-before-download',
      downloads_stack: false,
      creates_deploy_plan: false,
      channel_fallback: false,
      executor_authorization: false,
    },
    summary,
    plugins,
  };
}

export function buildAgentDocsIndex({ decisionReport, decisionReportPath, decisionReportSha256 }) {
  if (!decisionReport || decisionReport.kind !== 'operator-update-decision-report') {
    batchFail('batch-docs-index-input-invalid', 'decision report required for docs index');
  }

  const entries = [];
  for (const plugin of decisionReport.plugins) {
    const urls = [
      plugin.source.official_url,
      plugin.source.metadata_url,
      plugin.source.license_url,
    ].filter((value, index, values) => typeof value === 'string' && value.startsWith('https://') && values.indexOf(value) === index);

    for (const url of urls) {
      const evidence = {
        plugin: plugin.name,
        channel: decisionReport.channel,
        current_version: plugin.current_version,
        candidate_version: plugin.candidate_version,
        recommendation: plugin.recommendation,
        url,
      };
      entries.push({
        plugin: plugin.name,
        channel: decisionReport.channel,
        url,
        fetched_at: decisionReport.generated_at,
        content_sha256: sha256Hex(JSON.stringify(evidence)),
        version: plugin.candidate_version ?? plugin.current_version,
        compatibility: {
          compatible: plugin.compatibility?.compatible ?? null,
          class: plugin.compatibility?.class ?? 'unknown',
          confidence: plugin.compatibility?.confidence ?? 'none',
          reason: plugin.compatibility?.reason ?? 'no structured compatibility evidence recorded',
        },
        trusted_use: 'informational-only',
        rag_ready: true,
      });
    }
  }

  return {
    schema_version: 1,
    kind: 'agent-docs-index',
    generated_at: decisionReport.generated_at,
    source_decision_report: {
      path: decisionReportPath,
      sha256: decisionReportSha256,
    },
    policy: {
      trusted_use: 'informational-only',
      may_authorize_jar: false,
      may_authorize_executor: false,
      rag_ready: true,
    },
    summary: {
      plugins: decisionReport.plugins.length,
      documents: entries.length,
    },
    documents: entries,
  };
}

function analyzePlugin(plugin, channel) {
  const candidate = plugin.candidate ?? null;
  const compatible = plugin.compatibility ?? { compatible: null };
  const availableVersions = buildAvailableVersions(plugin, channel);
  const risks = buildRisks(plugin);
  const versionDirection = candidate ? compareVersionDirection(plugin.current_version, candidate.version) : null;
  if (versionDirection?.direction === 'downgrade') risks.push('candidate-downgrade');
  if (versionDirection?.direction === 'version-order-unknown') risks.push('version-order-unknown');
  const recommendation = chooseRecommendation(plugin, candidate, compatible, risks, versionDirection);

  return {
    name: plugin.name,
    status: plugin.status,
    current_version: plugin.current_version,
    candidate_version: candidate?.version ?? null,
    version_direction: versionDirection?.direction ?? null,
    channel,
    current_jar: plugin.current_jar,
    installations: plugin.installations,
    source: {
      registration: plugin.source?.registration ?? null,
      adapter: plugin.source?.adapter ?? null,
      official_url: plugin.source?.official_url ?? null,
      metadata_url: plugin.source?.metadata_url ?? null,
      license_url: plugin.license?.url ?? null,
      project_status: plugin.source?.project_status ?? null,
    },
    vendor_metadata: plugin.vendor_metadata ?? null,
    license: plugin.license,
    compatibility: compatible,
    available_versions: availableVersions,
    candidate,
    risks,
    recommendation,
    why: recommendationReason(plugin, recommendation, risks),
  };
}

function buildAvailableVersions(plugin, channel) {
  if (Array.isArray(plugin.available_versions)) return structuredClone(plugin.available_versions);
  if (!plugin.candidate) return [];
  return [{
    version: plugin.candidate.version,
    artifact_id: plugin.candidate.artifact_id,
    channel,
    compatible: plugin.compatibility?.compatible === true,
    compatibility: plugin.compatibility ?? { compatible: null, class: 'unknown', confidence: 'none' },
    source_url: plugin.candidate.source_url,
    sha256: plugin.candidate.sha256,
    selected: true,
  }];
}

function buildRisks(plugin) {
  const risks = [];
  if (plugin.status === 'incompatible') risks.push('paper-compatibility-blocked');
  if (plugin.status === 'source-not-found') risks.push('official-source-not-verified');
  if (plugin.status === 'premium-or-login') risks.push('manual-vendor-auth-required');
  if (plugin.vendor_metadata?.download_authorized === false) risks.push('vendor-download-not-authorized');
  if (Array.isArray(plugin.vendor_metadata?.risk_notes)) risks.push(...plugin.vendor_metadata.risk_notes);
  if (plugin.status === 'internal-build') risks.push('internal-build-pipeline-required');
  if (plugin.candidate?.manual_approval_required === true) risks.push('manual-sha-approval-required');
  return risks;
}

function chooseRecommendation(plugin, candidate, compatibility, risks, versionDirection) {
  if (!candidate || compatibility.compatible !== true) return 'blokada';
  if (versionDirection?.direction === 'same-version') return 'nie-ruszac';
  if (versionDirection?.direction === 'downgrade') return 'blokada';
  if (versionDirection?.direction === 'version-order-unknown') return 'blokada';
  if (plugin.status === 'manual-hash-approval-required' || plugin.status === 'qualified') return 'warto';
  return 'blokada';
}

function recommendationReason(plugin, recommendation, risks) {
  if (recommendation === 'warto') return 'Kompatybilny kandydat istnieje; wymaga standardowej walidacji SHA i dalszych testow przed deployem.';
  if (recommendation === 'nie-ruszac') return 'Dostepny kandydat nie zmienia wersji lokalnej.';
  if (risks.length > 0) return `Blokada: ${risks.join(', ')}.`;
  return `Blokada: status ${plugin.status} nie daje bezpiecznego kandydata do pobrania stacku.`;
}

async function readDiscoveryReport(root, path) {
  const absolute = resolve(root, path);
  if (typeof path !== 'string' || isAbsolute(path) || !isWithin(join(root, 'reports'), absolute)) {
    batchFail('batch-decision-report-path-unsafe', 'discovery report must be a relative updater reports path');
  }
  const match = DISCOVERY_NAME.exec(path.split('/').at(-1));
  if (!match) batchFail('batch-decision-report-path-unsafe', 'discovery report filename must be content-addressed');
  const metadata = await stat(absolute);
  if (!metadata.isFile() || metadata.size > MAX_REPORT_BYTES || (metadata.mode & 0o222) !== 0) {
    batchFail('batch-decision-report-file-unsafe', 'discovery report must be immutable bounded file');
  }
  let handle;
  try {
    handle = await open(absolute, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    const report = JSON.parse((await handle.readFile()).toString('utf8'));
    const hash = canonicalHash(report);
    if (hash !== match[1]) batchFail('batch-decision-report-drift', 'discovery report hash no longer matches filename');
    return { report, sha256: hash, path: absolute };
  } finally {
    await handle?.close().catch(() => {});
  }
}

async function writeImmutableReport(report, { root, prefix }) {
  const reportsRoot = join(root, 'reports');
  await mkdir(reportsRoot, { recursive: true, mode: 0o700 });
  if (await realpath(reportsRoot) !== reportsRoot) batchFail('batch-decision-report-root-unsafe', 'reports directory resolves through symlink');
  const hash = canonicalHash(report);
  const path = join(reportsRoot, `${prefix}-${hash}.report.json`);
  let handle;
  try {
    handle = await open(path, fsConstants.O_WRONLY | fsConstants.O_CREAT | fsConstants.O_EXCL | fsConstants.O_NOFOLLOW, 0o400);
    await handle.writeFile(`${JSON.stringify(report, null, 2)}\n`, 'utf8');
    await handle.sync();
    await handle.close();
    handle = null;
    await chmod(path, 0o444);
    return { path, sha256: hash, reused: false };
  } catch (error) {
    await handle?.close().catch(() => {});
    if (error?.code !== 'EEXIST') throw error;
    return { path, sha256: hash, reused: true };
  }
}

function validateDiscoveryReport(report) {
  if (!report || report.schema_version !== 1 || report.coverage?.complete !== true || !Array.isArray(report.plugins)) {
    batchFail('batch-decision-discovery-malformed', 'complete discovery report required');
  }
}

function validateOperator(operator) {
  if (typeof operator !== 'string' || operator.trim() !== operator || operator.length === 0) {
    batchFail('batch-decision-operator-invalid', 'operator non-empty string without surrounding whitespace');
  }
}

async function validateRoot(projectRoot) {
  if (typeof projectRoot !== 'string' || !isAbsolute(projectRoot)) batchFail('batch-decision-root-unsafe', 'project root must be absolute');
  const root = resolve(projectRoot);
  if (await realpath(root) !== root) batchFail('batch-decision-root-unsafe', 'project root must be canonical');
  return root;
}

function toRelativePath(root, path) {
  const rel = relative(root, path);
  if (rel === '' || rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) batchFail('batch-decision-root-unsafe', 'path escaped updater root');
  return rel.split(sep).join('/');
}

function isWithin(root, candidate) {
  const rel = relative(resolve(root), resolve(candidate));
  return rel === '' || (!rel.startsWith(`..${sep}`) && rel !== '..' && !isAbsolute(rel));
}
