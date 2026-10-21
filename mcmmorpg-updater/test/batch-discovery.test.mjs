import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { access, rm } from 'node:fs/promises';
import { join } from 'node:path';
import { discoverBatchOutcomes } from '../src/batch/index.mjs';
import { scanInventory } from '../src/inventory/index.mjs';
import { validateRegistry } from '../src/registry/index.mjs';
import { validateTrustedSourcePolicy } from '../src/downloads/index.mjs';
import { validExampleJar } from './jar-fixtures.mjs';
import { buildRegistryFixture } from './registry-fixtures.mjs';

const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];
const PROJECT_URL = 'https://api.modrinth.com/v2/project/exampleProject';
const DISCOVERY_URL = 'https://api.modrinth.com/v2/project/exampleProject/version';
const ARTIFACT_URL = 'https://cdn.modrinth.com/data/exampleProject/versions/versionABC/ExamplePlugin-2.0.0.jar';

function policyFixture() {
  return {
    schema_version: 1,
    sources: [{
      id: 'example-modrinth',
      plugin_id: 'example',
      plugin_name: 'ExamplePlugin',
      adapter: 'pinned-https-release',
      project: 'official/example',
      discovery: {
        adapter: 'modrinth-project-versions',
        host: 'api.modrinth.com',
        project_path: '/v2/project/exampleProject',
        path_prefix: '/v2/project/exampleProject/version',
        max_bytes: 1024 * 1024,
      },
      artifact: {
        host: 'cdn.modrinth.com',
        project_path: '/data/exampleProject',
        path_prefix: '/data/exampleProject/versions/',
        max_bytes: 1024 * 1024,
      },
      manifest: null,
      redirects: { max: 0, same_host_only: true },
      signature_keys: {},
    }],
  };
}

function registrationsFixture() {
  return {
    schema_version: 1,
    registrations: [{
      manifest_name: 'ExamplePlugin',
      plugin_id: 'example',
      kind: 'public-source',
      source_id: 'example-modrinth',
      adapter: 'modrinth-project-versions',
      project: 'official/example',
      project_id: 'exampleProject',
      official_url: 'https://modrinth.com/plugin/example',
      compatibility: { loaders: ['paper'], release_types: ['release'] },
    }],
    modrinth_search: {
      adapter: 'modrinth-search-v2',
      max_results: 10,
      search: { host: 'api.modrinth.com', project_path: '/v2', path_prefix: '/v2/search', max_bytes: 1024 * 1024 },
      project: { host: 'api.modrinth.com', project_path: '/v2', path_prefix: '/v2/project/', max_bytes: 2 * 1024 * 1024 },
      artifact: { host: 'cdn.modrinth.com', max_bytes: 1024 * 1024 },
      redirects: { max: 0, same_host_only: true },
    },
  };
}

function projectMetadata() {
  return Buffer.from(JSON.stringify({
    id: 'exampleProject',
    slug: 'exampleplugin',
    title: 'ExamplePlugin',
    project_type: 'plugin',
    status: 'approved',
    license: { id: 'MIT', name: 'MIT License', url: 'https://example.test/license' },
    monetization_status: 'monetized',
  }));
}

function batchInventory(registryInventory) {
  const installations = registryInventory.targets.flatMap((target) => target.installations
    .filter((entry) => entry.plugin_id === 'example')
    .map((entry) => ({ target: target.target, installed_version: entry.installed_version, jar: entry.jar })));
  return {
    schema_version: 1,
    scanned_at: '2026-07-20T10:00:00.000Z',
    minecraft_version: '1.21.11',
    manifest: { path: '/fixtures/plugin_manifest.yaml', sha256: 'b'.repeat(64), plugin_count: 1 },
    installation_count: installations.length,
    targets: registryInventory.targets.map((target) => ({
      target: target.target,
      installation_count: installations.filter((entry) => entry.target === target.target).length,
    })),
    plugins: [{
      manifest: {
        name: 'ExamplePlugin',
        jar: 'Example-1.0.0.jar',
        current_version: '1.0.0',
        license_status: 'unknown',
      },
      installations,
    }],
  };
}

function modrinthVersions({ gameVersions = ['1.21.11'], url = ARTIFACT_URL } = {}) {
  return Buffer.from(JSON.stringify([{
    id: 'versionABC',
    project_id: 'exampleProject',
    version_number: '2.0.0',
    version_type: 'release',
    status: 'listed',
    date_published: '2026-07-20T09:00:00.000Z',
    game_versions: gameVersions,
    loaders: ['paper'],
    files: [{
      id: 'fileABC',
      filename: 'ExamplePlugin-2.0.0.jar',
      primary: true,
      size: 1234,
      url,
      hashes: { sha1: 'c'.repeat(40), sha512: 'd'.repeat(128) },
    }],
  }]));
}

async function discoveryFixture() {
  const fixture = await buildRegistryFixture();
  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  const registryInventory = await scanInventory(registry, { scannedAt: '2026-07-20T10:00:00.000Z' });
  return { fixture, registry, registryInventory, inventory: batchInventory(registryInventory) };
}

test('trusted policy accepts a separately pinned official discovery endpoint', () => {
  const policy = validateTrustedSourcePolicy(policyFixture());
  assert.deepEqual(policy.sources[0].discovery, {
    adapter: 'modrinth-project-versions',
    host: 'api.modrinth.com',
    project_path: '/v2/project/exampleProject',
    path_prefix: '/v2/project/exampleProject/version',
    max_bytes: 1024 * 1024,
  });
});

test('official Modrinth metadata selects a compatible primary file and uses existing candidate policy gates', async (t) => {
  const context = await discoveryFixture();
  t.after(() => rm(context.fixture.root, { recursive: true, force: true }));
  const jar = validExampleJar('2.0.0');
  const requests = [];
  const outcomes = await discoverBatchOutcomes({
    inventory: context.inventory,
    registrations: registrationsFixture(),
    registry: context.registry,
    registryInventory: context.registryInventory,
    policy: policyFixture(),
    projectRoot: context.fixture.root,
    operator: 'batch-test',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      requests.push(url);
      if (url === PROJECT_URL) return { statusCode: 200, headers: {}, body: projectMetadata() };
      if (url === DISCOVERY_URL) return { statusCode: 200, headers: {}, body: modrinthVersions() };
      if (url === ARTIFACT_URL) return { statusCode: 200, headers: {}, body: jar };
      throw new Error(`unexpected URL: ${url}`);
    },
  });

  const outcome = outcomes.get('ExamplePlugin');
  assert.deepEqual(requests, [PROJECT_URL, DISCOVERY_URL, ARTIFACT_URL]);
  assert.equal(outcome.kind, 'candidate');
  assert.equal(outcome.candidate.sha256, createHash('sha256').update(jar).digest('hex'));
  assert.equal(outcome.candidate.eligible_for_plan, false);
  assert.equal(outcome.candidate.manual_approval_required, true);
  assert.equal(outcome.candidate.path, null);
  assert.deepEqual(outcome.candidate.plugin_yml, { name: 'ExamplePlugin', version: '2.0.0' });
  assert.equal(outcome.candidate.integrity.kind, 'manual-approval-required');
  assert.deepEqual(outcome.candidate.targets, ['world', 'items']);
  assert.equal(outcome.source.metadata_url, DISCOVERY_URL);
  assert.equal(outcome.source.project_status, 'approved');
  assert.deepEqual(outcome.source.upstream_hashes, { sha1: 'c'.repeat(40), sha512: 'd'.repeat(128) });
  assert.equal(outcome.compatibility.compatible, true);
  await assert.rejects(access(join(context.fixture.root, 'downloads')), (error) => error.code === 'ENOENT');
});

test('no matching Minecraft/loader release is incompatible and does not fetch a JAR', async (t) => {
  const context = await discoveryFixture();
  t.after(() => rm(context.fixture.root, { recursive: true, force: true }));
  const requests = [];
  const outcomes = await discoverBatchOutcomes({
    inventory: context.inventory,
    registrations: registrationsFixture(),
    registry: context.registry,
    registryInventory: context.registryInventory,
    policy: policyFixture(),
    projectRoot: context.fixture.root,
    operator: 'batch-test',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      requests.push(url);
      if (url === PROJECT_URL) return { statusCode: 200, headers: {}, body: projectMetadata() };
      return { statusCode: 200, headers: {}, body: modrinthVersions({ gameVersions: ['1.21.10'] }) };
    },
  });

  assert.deepEqual(requests, [PROJECT_URL, DISCOVERY_URL]);
  assert.equal(outcomes.get('ExamplePlugin').kind, 'incompatible');
  assert.equal(outcomes.get('ExamplePlugin').compatibility.compatible, false);
});

test('unofficial artifact URLs are blocked by the existing pinned source policy without persistence', async (t) => {
  const context = await discoveryFixture();
  t.after(() => rm(context.fixture.root, { recursive: true, force: true }));
  const outcomes = await discoverBatchOutcomes({
    inventory: context.inventory,
    registrations: registrationsFixture(),
    registry: context.registry,
    registryInventory: context.registryInventory,
    policy: policyFixture(),
    projectRoot: context.fixture.root,
    operator: 'batch-test',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      if (url === PROJECT_URL) return { statusCode: 200, headers: {}, body: projectMetadata() };
      return {
        statusCode: 200,
        headers: {},
        body: url === DISCOVERY_URL
          ? modrinthVersions({ url: 'https://unofficial.example.test/ExamplePlugin.jar' })
          : validExampleJar(),
      };
    },
  });

  assert.equal(outcomes.get('ExamplePlugin').kind, 'source-not-found');
  assert.match(outcomes.get('ExamplePlugin').reason, /source-host-forbidden/);
  await assert.rejects(access(join(context.fixture.root, 'downloads')), (error) => error.code === 'ENOENT');
});
