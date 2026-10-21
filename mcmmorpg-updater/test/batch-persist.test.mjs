import test from 'node:test';
import assert from 'node:assert/strict';
import {
  access,
  chmod,
  mkdir,
  readFile,
  readdir,
  rm,
  stat,
  writeFile,
} from 'node:fs/promises';
import { join } from 'node:path';
import {
  buildBatchReport,
  discoverBatchOutcomes,
  importManifestInventory,
  loadPluginManifest,
  persistVerifiedModrinthBatch,
  writeBatchReport,
} from '../src/batch/index.mjs';
import { scanInventory } from '../src/inventory/index.mjs';
import { validateRegistry } from '../src/registry/index.mjs';
import { buildJar } from './jar-fixtures.mjs';
import { buildRegistryFixture } from './registry-fixtures.mjs';

const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];

const DEFINITIONS = Object.freeze({
  ExamplePlugin: Object.freeze({
    jar: 'Example-1.0.0.jar',
    currentVersion: '1.0.0',
    candidateVersion: '2.0.0',
    projectId: 'exampleProject',
    versionId: 'exampleVersion',
    fileId: 'exampleFile',
  }),
  BrokenPlugin: Object.freeze({
    jar: 'Broken-1.0.0.jar',
    currentVersion: '1.0.0',
    candidateVersion: '2.0.0+build.7',
    releaseVersion: '2.0.0',
    projectId: 'brokenProject',
    versionId: 'brokenVersion',
    fileId: 'brokenFile',
  }),
});

function searchConfig() {
  return {
    schema_version: 1,
    registrations: [],
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

function jarFor(name, version) {
  return buildJar([{ name: 'plugin.yml', body: `name: ${name}\nversion: ${version}\nmain: example.${name}\n` }]);
}

function artifactUrl(definition) {
  return `https://cdn.modrinth.com/data/${definition.projectId}/versions/${definition.versionId}/${definition.projectId}.jar`;
}

function createTransport({ driftPlugin = null, driftReleasePlugin = null } = {}) {
  const requests = [];
  const transport = async ({ url }) => {
    requests.push(url);
    const parsed = new URL(url);
    if (parsed.pathname === '/v2/search') {
      const name = parsed.searchParams.get('query');
      const definition = DEFINITIONS[name];
      return { statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify({
        offset: 0,
        limit: 10,
        total_hits: definition ? 1 : 0,
        hits: definition ? [{ project_id: definition.projectId, slug: name.toLowerCase(), title: name, project_type: 'plugin' }] : [],
      })) };
    }
    const project = Object.entries(DEFINITIONS).find(([, definition]) => `/v2/project/${definition.projectId}` === parsed.pathname);
    if (project) {
      const [name, definition] = project;
      return { statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify({
        id: definition.projectId,
        slug: name.toLowerCase(),
        title: name,
        project_type: 'plugin',
        status: 'approved',
        license: { id: 'MIT', name: 'MIT License', url: 'https://example.test/license' },
        monetization_status: 'monetized',
      })) };
    }
    const versions = Object.entries(DEFINITIONS).find(([, definition]) => `/v2/project/${definition.projectId}/version` === parsed.pathname);
    if (versions) {
      const [name, definition] = versions;
      const versionId = name === driftReleasePlugin ? `${definition.versionId}Drift` : definition.versionId;
      const url = name === driftReleasePlugin
        ? `https://cdn.modrinth.com/data/${definition.projectId}/versions/${versionId}/${definition.projectId}.jar`
        : artifactUrl(definition);
      return { statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify([{
        id: versionId,
        project_id: definition.projectId,
        version_number: definition.releaseVersion ?? definition.candidateVersion,
        version_type: 'release',
        status: 'listed',
        date_published: '2026-07-20T09:00:00.000Z',
        game_versions: ['1.21.11'],
        loaders: ['paper'],
        files: [{
          id: definition.fileId,
          filename: `${definition.projectId}.jar`,
          primary: true,
          size: 1024,
          url,
          hashes: { sha1: 'c'.repeat(40), sha512: 'd'.repeat(128) },
        }],
      }])) };
    }
    const artifact = Object.entries(DEFINITIONS).find(([, definition]) => artifactUrl(definition) === url);
    if (artifact) {
      const [name, definition] = artifact;
      const version = name === driftPlugin ? '9.9.9-drift' : definition.candidateVersion;
      return { statusCode: 200, headers: {}, body: jarFor(name, version) };
    }
    throw new Error(`unexpected URL: ${url}`);
  };
  return { requests, transport };
}

async function buildPersistFixture(t) {
  const fixture = await buildRegistryFixture();
  t.after(() => rm(fixture.root, { recursive: true, force: true }));
  const projectRoot = join(fixture.root, 'updater');
  const registryRoot = join(projectRoot, 'registry');
  const configRoot = join(projectRoot, 'config');
  await Promise.all([
    mkdir(registryRoot, { recursive: true }),
    mkdir(configRoot, { recursive: true }),
  ]);
  for (const [name, definition] of Object.entries(DEFINITIONS)) {
    await writeFile(join(fixture.dataPaths.world, 'plugins', definition.jar), jarFor(name, definition.currentVersion));
  }
  const manifestPath = join(fixture.root, 'plugin_manifest.yaml');
  await writeFile(manifestPath, `server:
  target_minecraft_version: 1.21.11
plugins:
${Object.entries(DEFINITIONS).map(([name, definition]) => `  - name: ${name}
    jar: ${definition.jar}
    current_version: ${definition.currentVersion}
    license_status: unknown`).join('\n')}
`);
  const targetsPath = join(registryRoot, 'targets.json');
  const pluginsPath = join(registryRoot, 'plugins.json');
  const registrationsPath = join(configRoot, 'batch-sources.json');
  const policyPath = join(configRoot, 'trusted-sources.json');
  await Promise.all([
    writeFile(targetsPath, `${JSON.stringify(fixture.targets, null, 2)}\n`),
    writeFile(pluginsPath, `${JSON.stringify(fixture.plugins, null, 2)}\n`),
    writeFile(registrationsPath, `${JSON.stringify(searchConfig(), null, 2)}\n`),
    writeFile(policyPath, `${JSON.stringify(baselinePolicy(), null, 2)}\n`, { mode: 0o600 }),
  ]);
  await chmod(policyPath, 0o600);

  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  const manifest = await loadPluginManifest(manifestPath);
  const inventory = await importManifestInventory({ manifest, targets: registry.targets.targets, scannedAt: '2026-07-20T10:00:00.000Z' });
  const registryInventory = await scanInventory(registry, { scannedAt: '2026-07-20T10:00:00.000Z' });
  const discoveryTransport = createTransport();
  const outcomes = await discoverBatchOutcomes({
    inventory,
    registrations: searchConfig(),
    registry,
    registryInventory,
    policy: baselinePolicy(),
    projectRoot,
    operator: 'discovery-test',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: discoveryTransport.transport,
  });
  const report = buildBatchReport({
    inventory,
    registrations: searchConfig(),
    outcomes,
    generatedAt: '2026-07-20T10:05:00.000Z',
  });
  const reportPath = await writeBatchReport(report, { projectRoot });
  return {
    fixture,
    projectRoot,
    report,
    reportPath,
    manifestPath,
    targetsPath,
    pluginsPath,
    registrationsPath,
    policyPath,
  };
}

function persistInput(context, fetchImpl) {
  return {
    projectRoot: context.projectRoot,
    reportPath: context.reportPath,
    manifestPath: context.manifestPath,
    targetsPath: context.targetsPath,
    pluginsPath: context.pluginsPath,
    registrationsPath: context.registrationsPath,
    policyPath: context.policyPath,
    operator: 'root-operator',
    approvedAt: '2026-07-20T10:10:00.000Z',
    confirmDownloadAll: true,
    lookupImpl: PUBLIC_DNS,
    fetchImpl,
  };
}

async function serverListings(fixture) {
  return Object.fromEntries(await Promise.all(Object.entries(fixture.dataPaths).map(async ([target, root]) => [
    target,
    (await readdir(join(root, 'plugins'))).sort(),
  ])));
}

test('explicit batch persist downloads every current manual candidate and reruns idempotently without server writes', async (t) => {
  const context = await buildPersistFixture(t);
  const before = await serverListings(context.fixture);
  const firstTransport = createTransport();
  const first = await persistVerifiedModrinthBatch(persistInput(context, firstTransport.transport));

  assert.deepEqual(first.summary, { selected: 2, persisted: 2, already_present: 0, failed: 0 });
  assert.ok(first.results.every((result) => result.status === 'persisted'));
  assert.equal((await stat(join(context.projectRoot, first.release_manifest_json_path))).mode & 0o777, 0o444);
  assert.equal((await stat(join(context.projectRoot, first.release_manifest_markdown_path))).mode & 0o777, 0o444);
  const releaseManifest = JSON.parse(await readFile(join(context.projectRoot, first.release_manifest_json_path), 'utf8'));
  assert.equal(releaseManifest.plugins.length, 2);
  assert.deepEqual(releaseManifest.future_selection.map((entry) => entry.plugin), ['ExamplePlugin', 'BrokenPlugin']);
  for (const result of first.results) {
    const jarPath = join(context.projectRoot, result.candidate.path);
    assert.equal((await stat(jarPath)).mode & 0o777, 0o444);
    assert.equal((await stat(join(context.projectRoot, result.report_path))).mode & 0o777, 0o444);
  }
  assert.deepEqual(await serverListings(context.fixture), before);
  const jarMetadata = await Promise.all(first.results.map(async (result) => {
    const metadata = await stat(join(context.projectRoot, result.candidate.path));
    return [result.plugin_name, metadata.ino, metadata.mtimeMs, result.candidate.sha256];
  }));

  const secondTransport = createTransport();
  const second = await persistVerifiedModrinthBatch({
    ...persistInput(context, secondTransport.transport),
    approvedAt: '2026-07-20T10:11:00.000Z',
  });
  assert.deepEqual(second.summary, { selected: 2, persisted: 0, already_present: 2, failed: 0 });
  assert.ok(second.results.every((result) => result.status === 'already-present' && result.report_reused === true));
  assert.notEqual(second.batch_report_path, first.batch_report_path);
  assert.equal(second.batch_report_reused, false);
  assert.deepEqual(await Promise.all(second.results.map(async (result) => {
    const metadata = await stat(join(context.projectRoot, result.candidate.path));
    return [result.plugin_name, metadata.ino, metadata.mtimeMs, result.candidate.sha256];
  })), jarMetadata);
  assert.deepEqual(await serverListings(context.fixture), before);
});

test('one drifted JAR fails without removing a previously persisted candidate', async (t) => {
  const context = await buildPersistFixture(t);
  const transport = createTransport({ driftPlugin: 'BrokenPlugin' });
  const result = await persistVerifiedModrinthBatch(persistInput(context, transport.transport));

  assert.deepEqual(result.summary, { selected: 2, persisted: 1, already_present: 0, failed: 1 });
  assert.equal(result.results.find((entry) => entry.plugin_name === 'ExamplePlugin').status, 'persisted');
  const failed = result.results.find((entry) => entry.plugin_name === 'BrokenPlugin');
  assert.equal(failed.status, 'failed');
  assert.match(failed.error.code, /manual-approval-mismatch|candidate-drift/);
  await access(join(context.projectRoot, result.results[0].candidate.path));
  const downloads = await readdir(join(context.projectRoot, 'downloads'));
  assert.equal(downloads.length, 1);
});

test('report provenance drift rejects the batch before any candidate persistence', async (t) => {
  const context = await buildPersistFixture(t);
  const drifted = structuredClone(context.report);
  drifted.provenance.manifest.sha256 = 'f'.repeat(64);
  const driftedPath = await writeBatchReport(drifted, { projectRoot: context.projectRoot });

  await assert.rejects(
    persistVerifiedModrinthBatch({ ...persistInput(context, createTransport().transport), reportPath: driftedPath }),
    (error) => error.code === 'batch-persist-report-drift',
  );
  await assert.rejects(access(join(context.projectRoot, 'downloads')), (error) => error.code === 'ENOENT');
});

test('live release/source drift is rejected before the changed artifact is fetched', async (t) => {
  const context = await buildPersistFixture(t);
  const transport = createTransport({ driftReleasePlugin: 'BrokenPlugin' });
  const result = await persistVerifiedModrinthBatch(persistInput(context, transport.transport));

  assert.equal(result.results.find((entry) => entry.plugin_name === 'ExamplePlugin').status, 'persisted');
  const failed = result.results.find((entry) => entry.plugin_name === 'BrokenPlugin');
  assert.equal(failed.status, 'failed');
  assert.equal(failed.error.code, 'batch-report-candidate-drift');
  assert.equal(transport.requests.some((url) => url.includes('brokenVersionDrift') && url.includes('cdn.modrinth.com')), false);
});
