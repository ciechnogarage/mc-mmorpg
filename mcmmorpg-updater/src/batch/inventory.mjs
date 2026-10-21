import { createHash } from 'node:crypto';
import { constants as fsConstants } from 'node:fs';
import { lstat, open, realpath } from 'node:fs/promises';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { readPluginJarMetadata } from '../downloads/jar.mjs';
import { batchFail } from './errors.mjs';

export async function importManifestInventory({ manifest, targets, scannedAt = new Date().toISOString() }) {
  if (manifest?.schema_version !== 1 || !Array.isArray(manifest.plugins)) {
    batchFail('batch-inventory-input-invalid', 'a parsed schema v1 plugin manifest is required');
  }
  if (!targets || typeof targets !== 'object' || Array.isArray(targets)) {
    batchFail('batch-inventory-input-invalid', 'target inventory registry is required');
  }
  const timestamp = new Date(scannedAt);
  if (!Number.isFinite(timestamp.getTime())) batchFail('batch-inventory-input-invalid', 'scannedAt is invalid');

  const installations = new Map(manifest.plugins.map((plugin) => [plugin.name, []]));
  const targetReports = [];
  for (const targetId of Object.keys(targets).sort()) {
    const pluginDirectory = await validatePluginDirectory(targetId, targets[targetId]);
    let count = 0;
    for (const plugin of manifest.plugins) {
      const jar = await readCurrentJar(pluginDirectory, plugin.jar);
      if (!jar) continue;
      installations.get(plugin.name).push({
        target: targetId,
        installed_version: plugin.current_version,
        jar: { path: `${targets[targetId].plugin_directory}/${plugin.jar}`, ...jar },
        match: 'exact-manifest-jar',
      });
      count += 1;
    }
    targetReports.push({ target: targetId, installation_count: count });
  }

  const plugins = manifest.plugins.map((plugin) => ({
    manifest: { ...plugin },
    installations: installations.get(plugin.name),
  }));
  return Object.freeze({
    schema_version: 1,
    scanned_at: timestamp.toISOString(),
    minecraft_version: manifest.server.target_minecraft_version,
    manifest: { ...manifest.provenance, plugin_count: manifest.plugins.length },
    installation_count: plugins.reduce((total, plugin) => total + plugin.installations.length, 0),
    targets: targetReports,
    plugins,
  });
}

async function validatePluginDirectory(targetId, target) {
  if (!target || typeof target !== 'object' || !isAbsolute(target.host_data_path)
      || resolve(target.host_data_path) !== target.host_data_path
      || typeof target.plugin_directory !== 'string' || target.plugin_directory !== 'plugins') {
    batchFail('batch-inventory-target-invalid', `target ${targetId} has an unsafe plugin path`);
  }
  const root = await realpath(target.host_data_path);
  if (root !== target.host_data_path) batchFail('batch-inventory-target-invalid', `target ${targetId} root is not canonical`);
  const directory = join(root, target.plugin_directory);
  const metadata = await lstat(directory);
  if (!metadata.isDirectory() || metadata.isSymbolicLink() || await realpath(directory) !== directory) {
    batchFail('batch-inventory-target-invalid', `target ${targetId} plugin directory is unsafe`);
  }
  return directory;
}

async function readCurrentJar(pluginDirectory, filename) {
  const path = join(pluginDirectory, filename);
  if (!isWithin(pluginDirectory, path)) batchFail('batch-inventory-jar-unsafe', 'manifest JAR escaped the plugin directory');
  let metadata;
  try {
    metadata = await lstat(path);
  } catch (error) {
    if (error?.code === 'ENOENT') return null;
    throw error;
  }
  if (!metadata.isFile() || metadata.isSymbolicLink() || await realpath(path) !== path) {
    batchFail('batch-inventory-jar-unsafe', `current JAR is not a canonical regular file: ${filename}`);
  }
  let handle;
  try {
    handle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    const handleMetadata = await handle.stat();
    if (!handleMetadata.isFile()) batchFail('batch-inventory-jar-unsafe', `current JAR is not a regular file: ${filename}`);
    const bytes = await handle.readFile();
    let pluginYml;
    try {
      const metadata = readPluginJarMetadata(bytes);
      pluginYml = { status: 'verified', name: metadata.plugin_name, version: metadata.plugin_version };
    } catch (error) {
      pluginYml = { status: 'rejected', error_code: error?.code ?? 'jar-inspection-failed' };
    }
    return {
      sha256: createHash('sha256').update(bytes).digest('hex'),
      size: bytes.length,
      plugin_yml: pluginYml,
    };
  } finally {
    await handle?.close().catch(() => {});
  }
}

function isWithin(root, candidate) {
  const rel = relative(resolve(root), resolve(candidate));
  return rel === '' || (!rel.startsWith(`..${sep}`) && rel !== '..' && !isAbsolute(rel));
}
