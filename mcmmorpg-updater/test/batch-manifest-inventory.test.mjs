import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { mkdtemp, mkdir, readFile, readdir, rm, symlink, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  BatchSafetyError,
  importManifestInventory,
  loadPluginManifest,
  parsePluginManifest,
} from '../src/batch/index.mjs';
import { buildJar } from './jar-fixtures.mjs';

const REAL_MANIFEST_PATH = fileURLToPath(new URL('../../docs/plugin_manifest.yaml', import.meta.url));

function manifestFixture() {
  return `server:
  target_minecraft_version: 1.21.11
plugins:
  - name: PublicPlugin
    jar: PublicPlugin-1.0.0.jar
    current_version: 1.0.0
    license_status: unknown
  - name: CharacterStage
    jar: CharacterStage-0.1.0-SNAPSHOT.jar
    current_version: 0.1.0-SNAPSHOT
    license_status: internal
    source_path: MCMMORPG/plugin-src/character-stage
`;
}

test('dependency-free manifest parser covers every current plugin and preserves source evidence', async () => {
  const manifest = parsePluginManifest(await readFile(REAL_MANIFEST_PATH, 'utf8'), {
    path: REAL_MANIFEST_PATH,
  });

  assert.equal(manifest.schema_version, 1);
  assert.equal(manifest.server.target_minecraft_version, '1.21.11');
  assert.equal(manifest.plugins.length, 35);
  assert.equal(new Set(manifest.plugins.map((plugin) => plugin.name)).size, 35);
  assert.deepEqual(
    manifest.plugins.find((plugin) => plugin.name === 'CharacterStage'),
    {
      name: 'CharacterStage',
      jar: 'CharacterStage-0.1.0-SNAPSHOT.jar',
      current_version: '0.1.0-SNAPSHOT',
      license_status: 'internal',
      source_path: 'MCMMORPG/plugin-src/character-stage',
    },
  );
  assert.match(manifest.provenance.sha256, /^[0-9a-f]{64}$/);
});

test('per-target importer records exact manifest JARs read-only and retains plugins with no installation', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'mcmmorpg-batch-inventory-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const world = join(root, 'world');
  const items = join(root, 'items');
  await Promise.all([
    mkdir(join(world, 'plugins'), { recursive: true }),
    mkdir(join(items, 'plugins'), { recursive: true }),
  ]);
  const jarBytes = buildJar([
    { name: 'plugin.yml', body: 'name: PublicPlugin\nversion: 1.0.0\nmain: example.PublicPlugin\n' },
  ]);
  await writeFile(join(world, 'plugins', 'PublicPlugin-1.0.0.jar'), jarBytes);

  const before = {
    world: await readdir(join(world, 'plugins')),
    items: await readdir(join(items, 'plugins')),
  };
  const inventory = await importManifestInventory({
    manifest: parsePluginManifest(manifestFixture(), { path: '/fixtures/plugin_manifest.yaml' }),
    targets: {
      world: { host_data_path: world, plugin_directory: 'plugins' },
      items: { host_data_path: items, plugin_directory: 'plugins' },
    },
    scannedAt: '2026-07-20T10:00:00.000Z',
  });

  assert.equal(inventory.plugins.length, 2);
  assert.equal(inventory.installation_count, 1);
  assert.deepEqual(inventory.targets.map((target) => [target.target, target.installation_count]), [
    ['items', 0],
    ['world', 1],
  ]);
  assert.deepEqual(inventory.plugins[0].installations, [{
    target: 'world',
    installed_version: '1.0.0',
    jar: {
      path: 'plugins/PublicPlugin-1.0.0.jar',
      sha256: createHash('sha256').update(jarBytes).digest('hex'),
      size: jarBytes.length,
      plugin_yml: { status: 'verified', name: 'PublicPlugin', version: '1.0.0' },
    },
    match: 'exact-manifest-jar',
  }]);
  assert.deepEqual(inventory.plugins[1].installations, []);
  assert.deepEqual(await readdir(join(world, 'plugins')), before.world);
  assert.deepEqual(await readdir(join(items, 'plugins')), before.items);
});

test('inventory importer rejects manifest traversal and symlinked current JARs', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'mcmmorpg-batch-safety-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const world = join(root, 'world');
  await mkdir(join(world, 'plugins'), { recursive: true });
  const outside = join(root, 'outside.jar');
  await writeFile(outside, 'outside');
  await symlink(outside, join(world, 'plugins', 'PublicPlugin-1.0.0.jar'));

  await assert.rejects(
    importManifestInventory({
      manifest: parsePluginManifest(manifestFixture(), { path: '/fixtures/plugin_manifest.yaml' }),
      targets: { world: { host_data_path: world, plugin_directory: 'plugins' } },
    }),
    (error) => error instanceof BatchSafetyError && error.code === 'batch-inventory-jar-unsafe',
  );

  assert.throws(
    () => parsePluginManifest(manifestFixture().replace('PublicPlugin-1.0.0.jar', '../escape.jar')),
    (error) => error instanceof BatchSafetyError && error.code === 'batch-manifest-jar-invalid',
  );
});

test('manifest loader rejects a symlink before reading its contents', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'mcmmorpg-manifest-safety-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const target = join(root, 'manifest-target.yaml');
  const link = join(root, 'plugin_manifest.yaml');
  await writeFile(target, manifestFixture());
  await symlink(target, link);

  await assert.rejects(
    loadPluginManifest(link),
    (error) => error instanceof BatchSafetyError && error.code === 'batch-manifest-file-unsafe',
  );
});
