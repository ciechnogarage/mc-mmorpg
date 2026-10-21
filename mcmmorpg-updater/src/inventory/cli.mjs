import { fileURLToPath } from 'node:url';
import { join } from 'node:path';
import { loadRegistry } from '../registry/index.mjs';
import { scanInventory, writeInventoryArtifacts } from './index.mjs';

const projectRoot = fileURLToPath(new URL('../../', import.meta.url));
const registry = await loadRegistry({
  targetsPath: join(projectRoot, 'registry', 'targets.json'),
  pluginsPath: join(projectRoot, 'registry', 'plugins.json'),
});
const snapshot = await scanInventory(registry);
const result = await writeInventoryArtifacts(snapshot, {
  dataDir: join(projectRoot, 'data'),
  reportsDir: join(projectRoot, 'reports'),
});

process.stdout.write(`${JSON.stringify(result)}\n`);
