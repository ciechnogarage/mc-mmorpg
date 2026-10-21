import test from 'node:test';
import assert from 'node:assert/strict';
import { access, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import {
  discoverBatchOutcomes,
  importManifestInventory,
  parsePluginManifest,
} from '../src/batch/index.mjs';
import { scanInventory } from '../src/inventory/index.mjs';
import { validateRegistry } from '../src/registry/index.mjs';
import { buildJar } from './jar-fixtures.mjs';
import { buildRegistryFixture } from './registry-fixtures.mjs';

const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];
const SEARCH_URL = 'https://api.modrinth.com/v2/search?query=ExamplePlugin&limit=10';
const PROJECT_OK_URL = 'https://api.modrinth.com/v2/project/approvedProject';
const PROJECT_REJECTED_URL = 'https://api.modrinth.com/v2/project/rejectedProject';
const VERSIONS_URL = 'https://api.modrinth.com/v2/project/approvedProject/version';
const ARTIFACT_URL = 'https://cdn.modrinth.com/data/approvedProject/versions/versionABC/ExamplePlugin-2.0.0.jar';

function discoveryConfig() {
  return {
    schema_version: 1,
    registrations: [],
    modrinth_search: {
      adapter: 'modrinth-search-v2',
      max_results: 10,
      search: {
        host: 'api.modrinth.com',
        project_path: '/v2',
        path_prefix: '/v2/search',
        max_bytes: 1024 * 1024,
      },
      project: {
        host: 'api.modrinth.com',
        project_path: '/v2',
        path_prefix: '/v2/project/',
        max_bytes: 2 * 1024 * 1024,
      },
      artifact: { host: 'cdn.modrinth.com', max_bytes: 1024 * 1024 },
      redirects: { max: 0, same_host_only: true },
    },
  };
}

function baselinePolicy() {
  return {
    schema_version: 1,
    sources: [{
      id: 'baseline-source',
      plugin_id: 'baseline',
      plugin_name: 'Baseline',
      adapter: 'pinned-https-release',
      project: 'baseline/project',
      artifact: {
        host: 'cdn.modrinth.com',
        project_path: '/data/baseline',
        path_prefix: '/data/baseline/versions/',
        max_bytes: 1024,
      },
      manifest: null,
      redirects: { max: 0, same_host_only: true },
      signature_keys: {},
    }],
  };
}

function searchResponse() {
  return Buffer.from(JSON.stringify({
    offset: 0,
    limit: 10,
    total_hits: 3,
    hits: [
      { project_id: 'arbitraryFirst', slug: 'unrelated', title: 'Unrelated Result', project_type: 'mod' },
      { project_id: 'approvedProject', slug: 'exampleplugin', title: 'ExamplePlugin', project_type: 'mod' },
      { project_id: 'rejectedProject', slug: 'exampleplugin', title: 'ExamplePlugin', project_type: 'plugin' },
    ],
  }));
}

function projectResponse({ id, status }) {
  return Buffer.from(JSON.stringify({
    id,
    slug: 'exampleplugin',
    title: 'ExamplePlugin',
    project_type: 'mod',
    status,
    license: { id: 'MIT', name: 'MIT License', url: 'https://example.test/license' },
    monetization_status: 'monetized',
  }));
}

function versionsResponse({
  gameVersions = ['1.21.11'],
  projectId = 'approvedProject',
  versionId = 'versionABC',
  artifactUrl = ARTIFACT_URL,
} = {}) {
  return Buffer.from(JSON.stringify([{
    id: versionId,
    project_id: projectId,
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
      size: 1024,
      url: artifactUrl,
      hashes: { sha1: 'c'.repeat(40), sha512: 'd'.repeat(128) },
    }],
  }]));
}

async function buildSearchFixture() {
  const fixture = await buildRegistryFixture();
  const currentJar = buildJar([
    { name: 'plugin.yml', body: 'name: ExamplePlugin\nversion: 1.0.0\nmain: example.Plugin\n' },
  ]);
  await writeFile(join(fixture.dataPaths.world, 'plugins', 'Example-1.0.0.jar'), currentJar);
  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  const registryInventory = await scanInventory(registry, { scannedAt: '2026-07-20T10:00:00.000Z' });
  const manifest = parsePluginManifest(`server:
  target_minecraft_version: 1.21.11
plugins:
  - name: ExamplePlugin
    jar: Example-1.0.0.jar
    current_version: 1.0.0
    license_status: unknown
`, { path: '/fixtures/plugin_manifest.yaml' });
  const inventory = await importManifestInventory({
    manifest,
    targets: registry.targets.targets,
    scannedAt: '2026-07-20T10:00:00.000Z',
  });
  return { fixture, registry, registryInventory, inventory };
}

test('search does not trust the first hit and accepts only approved exact identity with Paper 1.21.11 and matching plugin.yml', async (t) => {
  const context = await buildSearchFixture();
  t.after(() => rm(context.fixture.root, { recursive: true, force: true }));
  const candidateJar = buildJar([
    { name: 'plugin.yml', body: 'name: ExamplePlugin\nversion: 2.0.0\nmain: example.Plugin\n' },
  ]);
  const requests = [];
  const outcomes = await discoverBatchOutcomes({
    inventory: context.inventory,
    registrations: discoveryConfig(),
    registry: context.registry,
    registryInventory: context.registryInventory,
    policy: baselinePolicy(),
    projectRoot: context.fixture.root,
    operator: 'search-test',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      requests.push(url);
      if (url === SEARCH_URL) return { statusCode: 200, headers: {}, body: searchResponse() };
      if (url === PROJECT_OK_URL) return { statusCode: 200, headers: {}, body: projectResponse({ id: 'approvedProject', status: 'approved' }) };
      if (url === PROJECT_REJECTED_URL) return { statusCode: 200, headers: {}, body: projectResponse({ id: 'rejectedProject', status: 'rejected' }) };
      if (url === VERSIONS_URL) return { statusCode: 200, headers: {}, body: versionsResponse() };
      if (url === ARTIFACT_URL) return { statusCode: 200, headers: {}, body: candidateJar };
      throw new Error(`unexpected URL: ${url}`);
    },
  });

  const outcome = outcomes.get('ExamplePlugin');
  assert.equal(outcome.kind, 'candidate');
  assert.equal(outcome.candidate.manual_approval_required, true);
  assert.equal(outcome.candidate.path, null);
  assert.equal(outcome.source.project_id, 'approvedProject');
  assert.deepEqual(outcome.license, {
    status: 'MIT',
    name: 'MIT License',
    url: 'https://example.test/license',
    provenance: 'modrinth-project-api',
  });
  assert.equal(outcome.search.query, 'ExamplePlugin');
  assert.equal(outcome.search.total_hits, 3);
  assert.deepEqual(outcome.rejections.map((rejection) => rejection.reason), [
    'search-identity-mismatch',
    'project-status-not-approved',
  ]);
  assert.ok(requests.indexOf(PROJECT_OK_URL) > requests.indexOf(SEARCH_URL));
  assert.ok(requests.indexOf(ARTIFACT_URL) > requests.indexOf(VERSIONS_URL));
  assert.equal(requests.includes('https://api.modrinth.com/v2/project/arbitraryFirst'), false);
  await assert.rejects(access(join(context.fixture.root, 'downloads')), (error) => error.code === 'ENOENT');
});

test('multiple approved exact-identity projects that pass JAR gates are rejected as ambiguous', async (t) => {
  const context = await buildSearchFixture();
  t.after(() => rm(context.fixture.root, { recursive: true, force: true }));
  const secondProject = 'approvedOther';
  const secondVersion = 'versionXYZ';
  const secondVersionsUrl = `https://api.modrinth.com/v2/project/${secondProject}/version`;
  const secondArtifactUrl = `https://cdn.modrinth.com/data/${secondProject}/versions/${secondVersion}/ExamplePlugin-2.0.0.jar`;
  const candidateJar = buildJar([
    { name: 'plugin.yml', body: 'name: ExamplePlugin\nversion: 2.0.0\nmain: example.Plugin\n' },
  ]);
  const outcomes = await discoverBatchOutcomes({
    inventory: context.inventory,
    registrations: discoveryConfig(),
    registry: context.registry,
    registryInventory: context.registryInventory,
    policy: baselinePolicy(),
    projectRoot: context.fixture.root,
    operator: 'search-test',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      if (url === SEARCH_URL) return { statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify({
        offset: 0,
        limit: 10,
        total_hits: 2,
        hits: [
          { project_id: 'approvedProject', slug: 'exampleplugin', title: 'ExamplePlugin', project_type: 'mod' },
          { project_id: secondProject, slug: 'exampleplugin', title: 'ExamplePlugin', project_type: 'mod' },
        ],
      })) };
      if (url === PROJECT_OK_URL) return { statusCode: 200, headers: {}, body: projectResponse({ id: 'approvedProject', status: 'approved' }) };
      if (url === `https://api.modrinth.com/v2/project/${secondProject}`) {
        return { statusCode: 200, headers: {}, body: projectResponse({ id: secondProject, status: 'approved' }) };
      }
      if (url === VERSIONS_URL) return { statusCode: 200, headers: {}, body: versionsResponse() };
      if (url === secondVersionsUrl) return { statusCode: 200, headers: {}, body: versionsResponse({
        projectId: secondProject,
        versionId: secondVersion,
        artifactUrl: secondArtifactUrl,
      }) };
      if (url === ARTIFACT_URL || url === secondArtifactUrl) return { statusCode: 200, headers: {}, body: candidateJar };
      throw new Error(`unexpected URL: ${url}`);
    },
  });

  const outcome = outcomes.get('ExamplePlugin');
  assert.equal(outcome.kind, 'source-not-found');
  assert.equal(outcome.rejections.at(-1).reason, 'ambiguous-qualified-projects');
  assert.equal(outcome.rejections.at(-1).count, 2);
  await assert.rejects(access(join(context.fixture.root, 'downloads')), (error) => error.code === 'ENOENT');
});

test('approved project without a Paper 1.21.11 release is incompatible and never fetches an artifact', async (t) => {
  const context = await buildSearchFixture();
  t.after(() => rm(context.fixture.root, { recursive: true, force: true }));
  const requests = [];
  const outcomes = await discoverBatchOutcomes({
    inventory: context.inventory,
    registrations: {
      ...discoveryConfig(),
      modrinth_search: { ...discoveryConfig().modrinth_search, max_results: 1 },
    },
    registry: context.registry,
    registryInventory: context.registryInventory,
    policy: baselinePolicy(),
    projectRoot: context.fixture.root,
    operator: 'search-test',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      requests.push(url);
      if (url.startsWith('https://api.modrinth.com/v2/search?')) {
        return { statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify({
          offset: 0,
          limit: 1,
          total_hits: 1,
          hits: [{ project_id: 'approvedProject', slug: 'exampleplugin', title: 'ExamplePlugin', project_type: 'mod' }],
        })) };
      }
      if (url === PROJECT_OK_URL) return { statusCode: 200, headers: {}, body: projectResponse({ id: 'approvedProject', status: 'approved' }) };
      if (url === VERSIONS_URL) return { statusCode: 200, headers: {}, body: versionsResponse({ gameVersions: ['1.21.10'] }) };
      throw new Error(`artifact must not be fetched: ${url}`);
    },
  });

  const outcome = outcomes.get('ExamplePlugin');
  assert.equal(outcome.kind, 'incompatible');
  assert.equal(outcome.compatibility.compatible, false);
  assert.equal(requests.includes(ARTIFACT_URL), false);
  assert.ok(outcome.rejections.some((rejection) => rejection.reason === 'release-incompatible'));
});
