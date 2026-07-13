import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const REQUIRED_FIELDS = [
  'name',
  'jar',
  'current_version',
  'role',
  'owner',
  'license_status',
  'update_policy',
  'criticality'
];
const REPOSITORY_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

function scalar(value) {
  const trimmed = value.trim();
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1);
  }
  return trimmed;
}

export function parsePluginManifest(manifestText) {
  const plugins = [];
  let inPlugins = false;
  let currentPlugin = null;

  for (const [index, line] of manifestText.split(/\r?\n/).entries()) {
    const lineNumber = index + 1;
    if (!inPlugins) {
      if (line === 'plugins:') inPlugins = true;
      continue;
    }
    if (!line.trim() || line.trimStart().startsWith('#')) continue;

    if (/^[^\s#]/.test(line)) break;

    const item = line.match(/^  - name:\s*(.+)$/);
    if (item) {
      currentPlugin = { name: scalar(item[1]) };
      plugins.push(currentPlugin);
      continue;
    }

    const field = line.match(/^    ([a-z_]+):\s*(.*)$/);
    if (field) {
      if (!currentPlugin) {
        throw new Error(`Plugin field on line ${lineNumber} appears before a plugin item.`);
      }
      currentPlugin[field[1]] = scalar(field[2]);
      continue;
    }

    if (/^  [a-z_]+:/.test(line)) {
      throw new Error(`Plugin field on line ${lineNumber} must be indented inside its plugin item.`);
    }
    throw new Error(`Unsupported plugin manifest syntax on line ${lineNumber}: ${line}`);
  }

  if (!inPlugins) throw new Error('Plugin manifest is missing the plugins: section.');
  return plugins;
}

export function auditPluginInventory(plugins, runtimeJars) {
  const activePlugins = plugins.filter(plugin => plugin.runtime_status !== 'disabled_staging_mvp');
  const activeManifestJars = [...new Set(activePlugins.map(plugin => plugin.jar).filter(Boolean))].sort();
  const actualRuntimeJars = [...new Set(runtimeJars)].sort();
  const activeJarSet = new Set(activeManifestJars);
  const runtimeJarSet = new Set(actualRuntimeJars);
  const invalidEntries = plugins
    .map(plugin => ({
      name: plugin.name || '<unnamed>',
      missingFields: REQUIRED_FIELDS.filter(field => !plugin[field])
    }))
    .filter(entry => entry.missingFields.length);

  return {
    activeManifestJars,
    runtimeJars: actualRuntimeJars,
    missingFromManifest: actualRuntimeJars.filter(jar => !activeJarSet.has(jar)),
    missingFromRuntime: activeManifestJars.filter(jar => !runtimeJarSet.has(jar)),
    invalidEntries
  };
}

function main() {
  const root = REPOSITORY_ROOT;
  const manifestPath = path.join(root, 'docs/plugin_manifest.yaml');
  const pluginsPath = path.join(root, 'MCMMORPG/plugins');
  const manifest = parsePluginManifest(fs.readFileSync(manifestPath, 'utf8'));
  const runtimeJars = fs.readdirSync(pluginsPath).filter(name => name.endsWith('.jar'));
  const audit = auditPluginInventory(manifest, runtimeJars);
  const valid = !audit.missingFromManifest.length && !audit.missingFromRuntime.length && !audit.invalidEntries.length;

  if (process.argv.includes('--json')) {
    console.log(JSON.stringify(audit, null, 2));
  } else if (valid) {
    console.log(`Plugin inventory check passed: ${audit.runtimeJars.length} active runtime jars reconciled.`);
  } else {
    console.error(JSON.stringify(audit, null, 2));
  }

  if (!valid) process.exitCode = 1;
}

if (import.meta.url === `file://${process.argv[1]}`) main();
