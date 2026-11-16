import test from 'node:test';
import assert from 'node:assert/strict';
import { access, mkdtemp, readFile, readdir, rm, stat } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { BATCH_STATUSES, runBatchDiscovery } from '../src/batch/index.mjs';
import { buildJar } from './jar-fixtures.mjs';

const UPDATER_ROOT = fileURLToPath(new URL('../', import.meta.url));
const PROJECT_ROOT = fileURLToPath(new URL('../../', import.meta.url));
const PROJECT_URL = 'https://api.modrinth.com/v2/project/lKEzGugV';
const DISCOVERY_URL = 'https://api.modrinth.com/v2/project/lKEzGugV/version';
const ARTIFACT_URL = 'https://cdn.modrinth.com/data/lKEzGugV/versions/pIvQcXW8/PlaceholderAPI-2.12.3.jar';
const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];

function metadataFixture() {
  return Buffer.from(JSON.stringify([{
    id: 'pIvQcXW8',
    project_id: 'lKEzGugV',
    version_number: '2.12.3',
    version_type: 'release',
    status: 'listed',
    date_published: '2026-07-01T09:00:00.000Z',
    game_versions: ['1.21.11'],
    loaders: ['paper', 'bukkit', 'spigot'],
    files: [{
      id: 'placeholderFile',
      filename: 'PlaceholderAPI-2.12.3.jar',
      primary: true,
      size: 4096,
      url: ARTIFACT_URL,
      hashes: { sha1: 'c'.repeat(40), sha512: 'd'.repeat(128) },
    }],
  }]));
}

function projectFixture() {
  return Buffer.from(JSON.stringify({
    id: 'lKEzGugV',
    slug: 'placeholderapi',
    title: 'PlaceholderAPI',
    project_type: 'plugin',
    status: 'approved',
    license: { id: 'MIT', name: 'MIT License', url: 'https://example.test/license' },
    monetization_status: 'monetized',
  }));
}

async function serverPluginListings() {
  const docker = join(PROJECT_ROOT, 'MCMMORPG', 'docker');
  const roots = {
    world: join(docker, 'servers', 'world', 'plugins'),
    items: join(docker, 'servers', 'items', 'plugins'),
    hub: join(docker, 'servers', 'hub', 'plugins'),
    velocity: join(docker, 'velocity', 'plugins'),
  };
  return Object.fromEntries(await Promise.all(Object.entries(roots).map(async ([target, path]) => [
    target,
    (await readdir(path)).sort(),
  ])));
}

test('current manifest batch report has complete coverage and writes only below updater reports', async (t) => {
  const outputRoot = await mkdtemp(join(tmpdir(), 'mcmmorpg-batch-output-'));
  t.after(() => rm(outputRoot, { recursive: true, force: true }));
  const serverBefore = await serverPluginListings();
  const jar = buildJar([
    { name: 'plugin.yml', body: 'name: PlaceholderAPI\nversion: 2.12.3\nmain: me.clip.placeholderapi.PlaceholderAPIPlugin\n' },
    { name: 'META-INF/MANIFEST.MF', body: 'Manifest-Version: 1.0\n' },
  ]);
  const requests = [];
  const result = await runBatchDiscovery({
    projectRoot: outputRoot,
    manifestPath: join(PROJECT_ROOT, 'docs', 'plugin_manifest.yaml'),
    targetsPath: join(UPDATER_ROOT, 'registry', 'targets.json'),
    pluginsPath: join(UPDATER_ROOT, 'registry', 'plugins.json'),
    registrationsPath: join(UPDATER_ROOT, 'config', 'batch-sources.json'),
    policyPath: join(UPDATER_ROOT, 'config', 'trusted-sources.json'),
    operator: 'batch-test',
    generatedAt: '2026-07-20T10:10:00.000Z',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      requests.push(url);
      if (url === PROJECT_URL) return { statusCode: 200, headers: {}, body: projectFixture() };
      if (url === DISCOVERY_URL) return { statusCode: 200, headers: {}, body: metadataFixture() };
      if (url === ARTIFACT_URL) return { statusCode: 200, headers: {}, body: jar };
      if (url.startsWith('https://api.modrinth.com/v2/search?')) {
        return { statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify({
          offset: 0,
          limit: 10,
          total_hits: 0,
          hits: [],
        })) };
      }
      throw new Error(`unexpected URL: ${url}`);
    },
  });

  assert.ok(requests.includes(DISCOVERY_URL));
  assert.ok(requests.includes(ARTIFACT_URL));
  assert.equal(requests.filter((url) => url.startsWith('https://api.modrinth.com/v2/search?')).length, 22);
  assert.equal(result.report.coverage.manifest_plugins, 35);
  assert.equal(result.report.coverage.reported_plugins, 35);
  assert.equal(result.report.coverage.complete, true);
  assert.ok(result.report.plugins.every((plugin) => BATCH_STATUSES.includes(plugin.status)));
  assert.equal(result.report.plugins.find((plugin) => plugin.name === 'PlaceholderAPI').status, 'manual-hash-approval-required');
  assert.equal(result.report.plugins.find((plugin) => plugin.name === 'CharacterStage').status, 'internal-build');
  assert.equal(result.report.plugins.find((plugin) => plugin.name === 'MythicDungeons').status, 'premium-or-login');
  assert.equal(result.report.plugins.find((plugin) => plugin.name === 'MMOItems').status, 'premium-or-login');
  assert.equal(result.report.plugins.find((plugin) => plugin.name === 'CMI').status, 'source-not-found');
  assert.match(result.report_path, /^reports\/batch-discovery-[0-9a-f]{64}\.report\.json$/);
  assert.equal((await stat(join(outputRoot, result.report_path))).mode & 0o777, 0o444);
  assert.deepEqual(JSON.parse(await readFile(join(outputRoot, result.report_path), 'utf8')), result.report);
  await assert.rejects(access(join(outputRoot, 'downloads')), (error) => error.code === 'ENOENT');
  assert.deepEqual(await readdir(outputRoot), ['reports']);
  assert.deepEqual(await serverPluginListings(), serverBefore);
});
