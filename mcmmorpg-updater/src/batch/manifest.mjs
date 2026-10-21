import { createHash } from 'node:crypto';
import { constants as fsConstants } from 'node:fs';
import { lstat, open } from 'node:fs/promises';
import { isAbsolute } from 'node:path';
import { batchFail } from './errors.mjs';

const MAX_MANIFEST_BYTES = 2 * 1024 * 1024;
const PLUGIN_FIELDS = new Set(['name', 'jar', 'current_version', 'license_status', 'source_path']);

export async function loadPluginManifest(path) {
  const metadata = await lstat(path);
  if (!metadata.isFile() || metadata.isSymbolicLink() || metadata.size > MAX_MANIFEST_BYTES) {
    batchFail('batch-manifest-file-unsafe', 'plugin manifest must be a bounded regular non-symlink file');
  }
  let handle;
  try {
    handle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
    const handleMetadata = await handle.stat();
    if (!handleMetadata.isFile() || handleMetadata.size > MAX_MANIFEST_BYTES) {
      batchFail('batch-manifest-file-unsafe', 'plugin manifest changed to an unsafe file');
    }
    return parsePluginManifest((await handle.readFile()).toString('utf8'), { path });
  } catch (error) {
    if (error?.code === 'ELOOP') batchFail('batch-manifest-file-unsafe', 'plugin manifest cannot be a symlink');
    throw error;
  } finally {
    await handle?.close().catch(() => {});
  }
}

export function parsePluginManifest(text, { path = null } = {}) {
  if (typeof text !== 'string' || Buffer.byteLength(text) > MAX_MANIFEST_BYTES
      || text.includes('\0') || text.includes('\uFFFD')) {
    batchFail('batch-manifest-malformed', 'plugin manifest must be bounded safe UTF-8 text');
  }

  let minecraftVersion;
  const plugins = [];
  let current = null;
  for (const line of text.replace(/^\uFEFF/, '').split(/\r?\n/)) {
    const minecraft = /^  target_minecraft_version:\s*(.*?)\s*$/.exec(line);
    if (minecraft) minecraftVersion = scalar(minecraft[1], 'target_minecraft_version');

    const pluginStart = /^  - name:\s*(.*?)\s*$/.exec(line);
    if (pluginStart) {
      if (current) plugins.push(validatePlugin(current));
      current = { name: scalar(pluginStart[1], 'plugin.name') };
      continue;
    }
    if (!current) continue;
    const field = /^    ([a-z][a-z0-9_]*):\s*(.*?)\s*$/.exec(line);
    if (field && PLUGIN_FIELDS.has(field[1])) {
      if (field[1] in current) batchFail('batch-manifest-malformed', `duplicate plugin field: ${field[1]}`);
      current[field[1]] = scalar(field[2], `plugin.${field[1]}`);
    }
  }
  if (current) plugins.push(validatePlugin(current));
  if (!minecraftVersion || plugins.length === 0) {
    batchFail('batch-manifest-malformed', 'manifest requires target_minecraft_version and at least one plugin');
  }
  const names = new Set();
  for (const plugin of plugins) {
    if (names.has(plugin.name)) batchFail('batch-manifest-plugin-duplicate', `duplicate manifest plugin: ${plugin.name}`);
    names.add(plugin.name);
  }

  return Object.freeze({
    schema_version: 1,
    server: Object.freeze({ target_minecraft_version: minecraftVersion }),
    plugins: Object.freeze(plugins.map(Object.freeze)),
    provenance: Object.freeze({
      path,
      sha256: createHash('sha256').update(text).digest('hex'),
    }),
  });
}

function validatePlugin(plugin) {
  for (const field of ['name', 'jar', 'current_version', 'license_status']) {
    if (typeof plugin[field] !== 'string' || plugin[field].length === 0) {
      batchFail('batch-manifest-malformed', `manifest plugin requires ${field}`);
    }
  }
  if (plugin.jar.includes('/') || plugin.jar.includes('\\') || !plugin.jar.toLowerCase().endsWith('.jar')
      || plugin.jar === '.' || plugin.jar === '..') {
    batchFail('batch-manifest-jar-invalid', `manifest JAR must be a filename: ${plugin.jar}`);
  }
  if (plugin.source_path !== undefined) {
    if (isAbsolute(plugin.source_path) || plugin.source_path.includes('\\')
        || plugin.source_path.split('/').some((segment) => segment === '' || segment === '.' || segment === '..')) {
      batchFail('batch-manifest-source-path-invalid', `manifest source_path is unsafe: ${plugin.source_path}`);
    }
  }
  return { ...plugin };
}

function scalar(value, label) {
  let parsed = value.replace(/\s+#.*$/, '').trim();
  if (parsed.startsWith('"')) {
    try { parsed = JSON.parse(parsed); } catch { batchFail('batch-manifest-malformed', `${label} has an invalid quoted scalar`); }
  } else if (parsed.startsWith("'") && parsed.endsWith("'")) {
    parsed = parsed.slice(1, -1).replace(/''/g, "'");
  }
  if (typeof parsed !== 'string' || parsed.length === 0 || parsed.length > 512 || /[\r\n\0]/.test(parsed)) {
    batchFail('batch-manifest-malformed', `${label} must be a short scalar`);
  }
  return parsed;
}
