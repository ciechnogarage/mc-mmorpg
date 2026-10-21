import test from 'node:test';
import assert from 'node:assert/strict';
import { stat } from 'node:fs/promises';
import { join } from 'node:path';
import { scanInventory, writeInventoryArtifacts } from '../src/inventory/index.mjs';
import { validateRegistry } from '../src/registry/index.mjs';
import { buildRegistryFixture, expectedSha256 } from './registry-fixtures.mjs';

test('inventory preserves divergent plugin versions per target and hashes JAR/config bytes with SHA-256', async () => {
  const fixture = await buildRegistryFixture();
  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  const snapshot = await scanInventory(registry, { scannedAt: '2026-07-19T12:00:00.000Z' });

  const world = snapshot.targets.find((target) => target.target === 'world');
  const items = snapshot.targets.find((target) => target.target === 'items');
  const worldPlugin = world.installations.find((entry) => entry.plugin_id === 'example');
  const itemsPlugin = items.installations.find((entry) => entry.plugin_id === 'example');

  assert.equal(worldPlugin.installed_version, '1.0.0');
  assert.equal(itemsPlugin.installed_version, '2.0.0');
  assert.equal(worldPlugin.jar.sha256, expectedSha256('world-jar-v1\n'));
  assert.equal(itemsPlugin.jar.sha256, expectedSha256('items-jar-v2\n'));
  assert.equal(worldPlugin.configs[0].sha256, expectedSha256('enabled: true\n'));
  assert.equal(itemsPlugin.configs[0].sha256, expectedSha256('enabled: false\n'));
  assert.notEqual(worldPlugin.configs[0].sha256, itemsPlugin.configs[0].sha256);
  assert.equal(worldPlugin.jar.size, Buffer.byteLength('world-jar-v1\n'));
  assert.match(worldPlugin.jar.mtime, /^\d{4}-\d{2}-\d{2}T/);
  assert.notEqual(worldPlugin.jar.sha256, itemsPlugin.jar.sha256);
});

test('scanner excludes secret and player-data paths without hashing their contents', async () => {
  const fixture = await buildRegistryFixture();
  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  registry.plugins.installations[0].config_paths.push('.env', 'world/playerdata/player.dat');

  const snapshot = await scanInventory(registry, { scannedAt: '2026-07-19T12:00:00.000Z' });
  const worldPlugin = snapshot.targets
    .find((target) => target.target === 'world')
    .installations.find((entry) => entry.plugin_id === 'example');

  assert.deepEqual(worldPlugin.configs.map((entry) => entry.path), ['plugins/Example/config.yml']);
  assert.deepEqual(worldPlugin.excluded.map((entry) => entry.path), ['.env', 'world/playerdata/player.dat']);
  assert.ok(worldPlugin.excluded.every((entry) => !('sha256' in entry)));
  assert.match(worldPlugin.excluded[0].reason, /secret/i);
  assert.match(worldPlugin.excluded[1].reason, /player data/i);
});

test('snapshot and report are exclusive-create, content-addressed, and read-only', async () => {
  const fixture = await buildRegistryFixture();
  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  const snapshot = await scanInventory(registry, { scannedAt: '2026-07-19T12:00:00.000Z' });
  const dataDir = join(fixture.root, 'data');
  const reportsDir = join(fixture.root, 'reports');

  const written = await writeInventoryArtifacts(snapshot, { dataDir, reportsDir });
  assert.match(written.snapshotPath, /inventory-[0-9a-f]{64}\.snapshot\.json$/);
  assert.match(written.reportPath, /inventory-[0-9a-f]{64}\.report\.json$/);
  assert.equal((await stat(written.snapshotPath)).mode & 0o777, 0o444);
  assert.equal((await stat(written.reportPath)).mode & 0o777, 0o444);

  await assert.rejects(
    writeInventoryArtifacts(snapshot, { dataDir, reportsDir }),
    (error) => error.code === 'EEXIST',
  );
});
