import { createHash } from 'node:crypto';
import { constants as fsConstants } from 'node:fs';
import { chmod, lstat, mkdir, open, readdir, realpath, unlink } from 'node:fs/promises';
import { join } from 'node:path';
import { canonicalHash } from '../contract/canonical.mjs';
import { REQUIRED_TARGET_IDS } from '../registry/schema.mjs';
import {
  assertCanonicalRelativePath,
  assertWithinRoot,
  classifyExcludedPath,
  resolveRegisteredPath,
} from '../registry/path-policy.mjs';

const INVENTORY_SCHEMA_VERSION = 1;
const READ_BUFFER_SIZE = 64 * 1024;

export async function scanInventory(registry, { scannedAt = new Date().toISOString() } = {}) {
  if (!registry?.targets?.targets || !registry?.plugins?.installations) {
    throw new TypeError('scanInventory requires a validated registry');
  }
  const timestamp = normalizeTimestamp(scannedAt);
  const targets = [];

  for (const targetId of REQUIRED_TARGET_IDS) {
    const target = registry.targets.targets[targetId];
    const installations = registry.plugins.installations.filter((entry) => entry.target === targetId);
    targets.push(await scanTarget(targetId, target, installations));
  }

  const content = {
    schema_version: INVENTORY_SCHEMA_VERSION,
    scanned_at: timestamp,
    registry_sha256: canonicalHash({ targets: registry.targets, plugins: registry.plugins }),
    compose_config_sha256: registry.targets.compose.config_sha256,
    targets,
  };
  return { ...content, snapshot_sha256: canonicalHash(content) };
}

export async function writeInventoryArtifacts(snapshot, { dataDir, reportsDir }) {
  verifySnapshotHash(snapshot);
  if (typeof dataDir !== 'string' || typeof reportsDir !== 'string') {
    throw new TypeError('dataDir and reportsDir are required');
  }

  await Promise.all([
    mkdir(dataDir, { recursive: true }),
    mkdir(reportsDir, { recursive: true }),
  ]);

  const base = `inventory-${snapshot.snapshot_sha256}`;
  const snapshotPath = join(dataDir, `${base}.snapshot.json`);
  const reportPath = join(reportsDir, `${base}.report.json`);
  const report = buildInventoryReport(snapshot);

  await writeExclusiveReadOnlyJson(snapshotPath, snapshot);
  try {
    await writeExclusiveReadOnlyJson(reportPath, report);
  } catch (error) {
    await unlink(snapshotPath).catch(() => {});
    throw error;
  }
  return { snapshotPath, reportPath, snapshotSha256: snapshot.snapshot_sha256 };
}

export function buildInventoryReport(snapshot) {
  return {
    schema_version: INVENTORY_SCHEMA_VERSION,
    snapshot_sha256: snapshot.snapshot_sha256,
    scanned_at: snapshot.scanned_at,
    targets: snapshot.targets.map((target) => ({
      target: target.target,
      compose_service: target.compose_service,
      jar_count: target.jars.length,
      installation_count: target.installations.length,
      config_file_count: target.installations.reduce(
        (count, installation) => count + installation.configs.filter((entry) => entry.status === 'present').length,
        0,
      ),
      missing_count: target.installations.reduce(
        (count, installation) => count
          + (installation.jar.status === 'missing' ? 1 : 0)
          + installation.configs.filter((entry) => entry.status === 'missing').length,
        0,
      ),
      excluded: target.installations.flatMap((installation) => installation.excluded.map((entry) => ({
        plugin_id: installation.plugin_id,
        ...entry,
      }))),
    })),
  };
}

async function scanTarget(targetId, target, installations) {
  const jars = await scanJarDirectory(target);
  const jarsByPath = new Map(jars.map((jar) => [jar.path, jar]));
  const scannedInstallations = [];

  for (const installation of installations) {
    const excluded = [];
    const configs = [];
    for (const configPath of installation.config_paths) {
      const reason = classifyExcludedPath(configPath);
      if (reason) {
        excluded.push({ path: configPath, reason });
        continue;
      }
      configs.push(await hashRegisteredFile(target.host_data_path, configPath));
    }

    const jar = jarsByPath.get(installation.jar_path)
      ?? await hashRegisteredFile(target.host_data_path, installation.jar_path);
    scannedInstallations.push({
      plugin_id: installation.plugin_id,
      installed_version: installation.installed_version,
      jar,
      configs,
      excluded,
      mutation_policy_manifest_sha256: installation.mutation_policy.manifest_sha256,
    });
  }

  return {
    target: targetId,
    compose_project: target.compose_project,
    compose_service: target.compose_service,
    compose_profile: target.compose_profile,
    host_data_path: target.host_data_path,
    container_data_path: target.container_data_path,
    jars,
    installations: scannedInstallations,
  };
}

async function scanJarDirectory(target) {
  const fail = inventoryPathFailure;
  const directory = resolveRegisteredPath(target.host_data_path, target.plugin_directory, fail);
  let entries;
  try {
    const directoryStat = await lstat(directory);
    if (directoryStat.isSymbolicLink() || !directoryStat.isDirectory()) {
      throw new InventorySafetyError('inventory-directory-unsafe', `plugin directory is not a real directory: ${directory}`);
    }
    const realDirectory = await realpath(directory);
    assertWithinRoot(target.host_data_path, realDirectory, fail);
    entries = await readdir(realDirectory, { withFileTypes: true });
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }

  const jars = [];
  for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
    if (!entry.isFile() || !entry.name.toLowerCase().endsWith('.jar')) continue;
    jars.push(await hashRegisteredFile(target.host_data_path, `${target.plugin_directory}/${entry.name}`));
  }
  return jars;
}

async function hashRegisteredFile(root, registeredPath) {
  const fail = inventoryPathFailure;
  assertCanonicalRelativePath(registeredPath, fail);
  const reason = classifyExcludedPath(registeredPath);
  if (reason) throw new InventorySafetyError('inventory-path-excluded', `${registeredPath} is excluded: ${reason}`);
  const absolutePath = resolveRegisteredPath(root, registeredPath, fail);

  let pathStat;
  try {
    pathStat = await lstat(absolutePath);
  } catch (error) {
    if (error.code === 'ENOENT') return { path: registeredPath, status: 'missing' };
    throw error;
  }
  if (pathStat.isSymbolicLink() || !pathStat.isFile()) {
    throw new InventorySafetyError('inventory-file-unsafe', `registered file is not a regular non-symlink file: ${registeredPath}`);
  }
  const realPath = await realpath(absolutePath);
  assertWithinRoot(root, realPath, fail);
  if (realPath !== absolutePath) {
    throw new InventorySafetyError('inventory-file-noncanonical', `registered file resolves elsewhere: ${registeredPath}`);
  }

  const handle = await open(absolutePath, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
  try {
    const handleStat = await handle.stat();
    if (!handleStat.isFile()) throw new InventorySafetyError('inventory-file-unsafe', `${registeredPath} is not a regular file`);
    const sha256 = await hashFileHandle(handle);
    return {
      path: registeredPath,
      status: 'present',
      sha256,
      size: handleStat.size,
      mtime: handleStat.mtime.toISOString(),
    };
  } finally {
    await handle.close();
  }
}

async function hashFileHandle(handle) {
  const hash = createHash('sha256');
  const buffer = Buffer.allocUnsafe(READ_BUFFER_SIZE);
  let position = 0;
  while (true) {
    const { bytesRead } = await handle.read(buffer, 0, buffer.length, position);
    if (bytesRead === 0) break;
    hash.update(buffer.subarray(0, bytesRead));
    position += bytesRead;
  }
  return hash.digest('hex');
}

async function writeExclusiveReadOnlyJson(path, value) {
  const handle = await open(path, fsConstants.O_WRONLY | fsConstants.O_CREAT | fsConstants.O_EXCL, 0o400);
  try {
    await handle.writeFile(`${JSON.stringify(value, null, 2)}\n`, 'utf8');
    await handle.sync();
  } finally {
    await handle.close();
  }
  await chmod(path, 0o444);
}

function verifySnapshotHash(snapshot) {
  if (!snapshot || typeof snapshot !== 'object') throw new TypeError('snapshot must be an object');
  const { snapshot_sha256: supplied, ...content } = snapshot;
  if (!/^[0-9a-f]{64}$/.test(supplied ?? '') || canonicalHash(content) !== supplied) {
    throw new InventorySafetyError('snapshot-hash-mismatch', 'inventory snapshot content hash does not verify');
  }
}

function normalizeTimestamp(value) {
  const date = value instanceof Date ? value : new Date(value);
  if (!Number.isFinite(date.getTime())) throw new TypeError('scannedAt must be a valid date');
  return date.toISOString();
}

function inventoryPathFailure(code, message) {
  throw new InventorySafetyError(code, message);
}

export class InventorySafetyError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'InventorySafetyError';
    this.code = code;
  }
}

export { INVENTORY_SCHEMA_VERSION };
