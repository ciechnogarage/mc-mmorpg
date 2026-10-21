import { canonicalHash } from '../contract/canonical.mjs';
import { constants as fsConstants } from 'node:fs';
import { chmod, lstat, mkdir, open, realpath } from 'node:fs/promises';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { loadTrustedSourcePolicy } from '../downloads/source-policy.mjs';
import { scanInventory } from '../inventory/index.mjs';
import { loadRegistry } from '../registry/index.mjs';
import { normalizeReleaseChannel } from './channel.mjs';
import { discoverBatchOutcomes } from './discovery.mjs';
import { batchFail } from './errors.mjs';
import { importManifestInventory } from './inventory.mjs';
import { loadPluginManifest } from './manifest.mjs';
import { buildBatchReport, validateBatchSourceRegistrations } from './report.mjs';

const MAX_CONFIG_BYTES = 2 * 1024 * 1024;

export async function runBatchDiscovery({
  projectRoot,
  manifestPath,
  targetsPath,
  pluginsPath,
  registrationsPath,
  policyPath,
  operator,
  channel = 'stable',
  generatedAt = new Date().toISOString(),
  expectedPolicyOwnerUid = typeof process.getuid === 'function' ? process.getuid() : undefined,
  lookupImpl,
  fetchImpl,
}) {
  if (typeof operator !== 'string' || operator.trim() !== operator || operator.length === 0) {
    batchFail('batch-operator-invalid', 'operator must be a non-empty string without surrounding whitespace');
  }
  const normalizedChannel = normalizeReleaseChannel(channel);
  const [manifest, registry, registrations, policy] = await Promise.all([
    loadPluginManifest(manifestPath),
    loadRegistry({ targetsPath, pluginsPath }),
    loadBatchSourceRegistrations(registrationsPath),
    loadTrustedSourcePolicy(policyPath, { expectedOwnerUid: expectedPolicyOwnerUid }),
  ]);
  const root = await validateBatchOutputRoot(projectRoot, registry);
  const [inventory, registryInventory] = await Promise.all([
    importManifestInventory({ manifest, targets: registry.targets.targets, scannedAt: generatedAt }),
    scanInventory(registry, { scannedAt: generatedAt }),
  ]);
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
    channel: normalizedChannel,
  });
  const report = buildBatchReport({ inventory, registrations, outcomes, generatedAt, channel: normalizedChannel });
  if (!report.coverage.complete) batchFail('batch-report-coverage-incomplete', 'batch report does not cover the full manifest inventory');
  const reportPath = await writeBatchReport(report, { projectRoot: root });
  return {
    schema_version: 1,
    report,
    report_path: toRelativePath(root, reportPath),
  };
}

export async function loadBatchSourceRegistrations(path) {
  const metadata = await lstat(path);
  if (!metadata.isFile() || metadata.isSymbolicLink() || metadata.size > MAX_CONFIG_BYTES) {
    batchFail('batch-source-registry-file-unsafe', 'batch source registry must be a bounded regular non-symlink file');
  }
  let handle;
  try {
    handle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    const handleMetadata = await handle.stat();
    if (!handleMetadata.isFile() || handleMetadata.size > MAX_CONFIG_BYTES) {
      batchFail('batch-source-registry-file-unsafe', 'batch source registry changed to an unsafe file');
    }
    const bytes = await handle.readFile();
    let config;
    try { config = JSON.parse(bytes.toString('utf8')); } catch { batchFail('batch-source-registry-malformed', 'batch source registry is not valid JSON'); }
    validateBatchSourceRegistrations(config);
    return config;
  } finally {
    await handle?.close().catch(() => {});
  }
}

export async function writeBatchReport(report, { projectRoot }) {
  if (!report?.coverage?.complete || report.schema_version !== 1) {
    batchFail('batch-report-invalid', 'only a complete schema v1 batch report may be written');
  }
  const root = resolve(projectRoot);
  if (!isAbsolute(projectRoot) || await realpath(root) !== root) {
    batchFail('batch-report-root-unsafe', 'batch report root must be canonical');
  }
  const reportsRoot = join(root, 'reports');
  await mkdir(reportsRoot, { recursive: true, mode: 0o700 });
  if (await realpath(reportsRoot) !== reportsRoot) batchFail('batch-report-root-unsafe', 'reports directory resolves through a symlink');
  const hash = canonicalHash(report);
  const path = join(reportsRoot, `batch-discovery-${hash}.report.json`);
  const handle = await open(
    path,
    fsConstants.O_WRONLY | fsConstants.O_CREAT | fsConstants.O_EXCL | fsConstants.O_NOFOLLOW,
    0o400,
  );
  try {
    await handle.writeFile(`${JSON.stringify(report, null, 2)}\n`, 'utf8');
    await handle.sync();
  } finally {
    await handle.close();
  }
  await chmod(path, 0o444);
  return path;
}

async function validateBatchOutputRoot(projectRoot, registry) {
  if (typeof projectRoot !== 'string' || !isAbsolute(projectRoot)) {
    batchFail('batch-report-root-unsafe', 'batch output root must be absolute');
  }
  const root = resolve(projectRoot);
  if (await realpath(root) !== root) batchFail('batch-report-root-unsafe', 'batch output root must be canonical');
  const forbidden = [registry.targets.allowed_host_root,
    ...Object.values(registry.targets.targets).map((target) => target.host_data_path)];
  if (forbidden.some((path) => isWithin(path, root) || isWithin(root, path))) {
    batchFail('batch-report-root-unsafe', 'batch output root overlaps an active MC server path');
  }
  return root;
}

function toRelativePath(root, path) {
  const rel = relative(root, path);
  if (rel === '' || rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) {
    batchFail('batch-report-root-unsafe', 'batch report escaped its output root');
  }
  return rel.split(sep).join('/');
}

function isWithin(root, candidate) {
  const rel = relative(resolve(root), resolve(candidate));
  return rel === '' || (!rel.startsWith(`..${sep}`) && rel !== '..' && !isAbsolute(rel));
}
