import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash, createSign, generateKeyPairSync } from 'node:crypto';
import { access, mkdir, readdir, readFile, stat, symlink, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import {
  CandidateSafetyError,
  qualifyCandidate,
  validateTrustedSourcePolicy,
} from '../src/downloads/index.mjs';
import { validatePlanIntegrity } from '../src/contract/plan-schema.mjs';
import { scanInventory } from '../src/inventory/index.mjs';
import { validateRegistry } from '../src/registry/index.mjs';
import { buildRegistryFixture } from './registry-fixtures.mjs';
import { buildJar, validExampleJar } from './jar-fixtures.mjs';

const ARTIFACT_URL = 'https://releases.example.test/projects/acme/example/releases/2.0.0.jar';
const MANIFEST_URL = 'https://checksums.example.test/projects/acme/example/manifests/2.0.0.json';
const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];

function sourcePolicy({ signatureKeys = {} } = {}) {
  return validateTrustedSourcePolicy({
    schema_version: 1,
    sources: [{
      id: 'example-release',
      adapter: 'pinned-https-release',
      plugin_id: 'example',
      plugin_name: 'ExamplePlugin',
      project: 'acme/example',
      artifact: {
        host: 'releases.example.test',
        project_path: '/projects/acme/example',
        path_prefix: '/projects/acme/example/releases/',
        max_bytes: 1024 * 1024,
      },
      manifest: {
        host: 'checksums.example.test',
        project_path: '/projects/acme/example',
        path_prefix: '/projects/acme/example/manifests/',
        max_bytes: 4096,
      },
      redirects: { max: 1, same_host_only: true },
      signature_keys: signatureKeys,
    }],
  });
}

async function candidateFixture() {
  const fixture = await buildRegistryFixture();
  const registry = await validateRegistry({ targets: fixture.targets, plugins: fixture.plugins });
  const inventory = await scanInventory(registry, { scannedAt: '2026-07-19T12:00:00.000Z' });
  const projectRoot = join(fixture.root, 'updater');
  await mkdir(projectRoot);
  const jar = validExampleJar();
  const sha256 = createHash('sha256').update(jar).digest('hex');
  const manifest = Buffer.from(JSON.stringify({
    schema_version: 1,
    project: 'acme/example',
    artifacts: {
      'example-2.0.0': { source_url: ARTIFACT_URL, sha256 },
    },
  }));
  const fetchImpl = async ({ url }) => {
    if (url === ARTIFACT_URL) return { statusCode: 200, headers: {}, body: jar };
    if (url === MANIFEST_URL) return { statusCode: 200, headers: {}, body: manifest };
    throw new Error(`unexpected fake URL: ${url}`);
  };
  return { fixture, registry, inventory, projectRoot, jar, sha256, manifest, fetchImpl };
}

function requestFixture(overrides = {}) {
  return {
    source_id: 'example-release',
    artifact_id: 'example-2.0.0',
    plugin_id: 'example',
    url: ARTIFACT_URL,
    targets: ['world'],
    integrity: { kind: 'trusted-manifest-sha256', url: MANIFEST_URL },
    ...overrides,
  };
}

function expectCode(promise, code) {
  return assert.rejects(promise, (error) => {
    assert.ok(error instanceof CandidateSafetyError);
    assert.equal(error.code, code);
    return true;
  });
}

test('qualified JAR is content-addressed below downloads and yields an auditable draft plan/report', async () => {
  const context = await candidateFixture();
  const result = await qualifyCandidate({
    registry: context.registry,
    inventory: context.inventory,
    policy: sourcePolicy(),
    request: requestFixture(),
    projectRoot: context.projectRoot,
    operator: 'alice',
    createdAt: '2026-07-19T13:00:00.000Z',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: context.fetchImpl,
  });

  assert.equal(result.candidate.sha256, context.sha256);
  assert.equal(result.candidate.eligible_for_plan, true);
  assert.equal(result.candidate.integrity.kind, 'trusted-manifest-sha256');
  assert.equal(result.candidate.path, `downloads/example/${context.sha256}.jar`);
  assert.deepEqual(await readFile(join(context.projectRoot, result.candidate.path)), context.jar);
  assert.equal((await stat(join(context.projectRoot, result.candidate.path))).mode & 0o777, 0o444);
  assert.deepEqual(result.report.source, {
    id: 'example-release',
    artifact_id: 'example-2.0.0',
    url: ARTIFACT_URL,
  });
  assert.equal(result.report.hashes.sha256, context.sha256);
  assert.deepEqual(result.report.version_differences, [
    { target: 'world', current_version: '1.0.0', candidate_version: '2.0.0' },
  ]);
  assert.equal(result.draft_plan.state, 'draft');
  assert.doesNotThrow(() => validatePlanIntegrity(result.draft_plan));
  assert.equal(result.draft_plan.content.targets[0].target, 'world');
  assert.equal(result.draft_plan.content.targets[0].candidateJar.sha256, context.sha256);
});

test('incorrect checksum, source, plugin name, or target mapping blocks the candidate before persistence', async (t) => {
  await t.test('checksum', async () => {
    const context = await candidateFixture();
    const badManifest = Buffer.from(JSON.stringify({
      schema_version: 1,
      project: 'acme/example',
      artifacts: { 'example-2.0.0': { source_url: ARTIFACT_URL, sha256: '0'.repeat(64) } },
    }));
    await expectCode(qualifyCandidate({
      registry: context.registry,
      inventory: context.inventory,
      policy: sourcePolicy(),
      request: requestFixture(),
      projectRoot: context.projectRoot,
      operator: 'alice',
      lookupImpl: PUBLIC_DNS,
      fetchImpl: async ({ url }) => ({ statusCode: 200, headers: {}, body: url === ARTIFACT_URL ? context.jar : badManifest }),
    }), 'candidate-checksum-mismatch');
    await assert.rejects(access(join(context.projectRoot, 'downloads')), (error) => error.code === 'ENOENT');
  });

  await t.test('source', async () => {
    const context = await candidateFixture();
    await expectCode(qualifyCandidate({
      registry: context.registry,
      inventory: context.inventory,
      policy: sourcePolicy(),
      request: requestFixture({ url: 'https://evil.example.test/example.jar' }),
      projectRoot: context.projectRoot,
      operator: 'alice',
      lookupImpl: PUBLIC_DNS,
      fetchImpl: context.fetchImpl,
    }), 'source-host-forbidden');
  });

  await t.test('plugin name', async () => {
    const context = await candidateFixture();
    const wrong = buildJar([
      { name: 'plugin.yml', body: 'name: OtherPlugin\nversion: 2.0.0\n' },
    ]);
    await expectCode(qualifyCandidate({
      registry: context.registry,
      inventory: context.inventory,
      policy: sourcePolicy(),
      request: requestFixture({ integrity: undefined }),
      projectRoot: context.projectRoot,
      operator: 'alice',
      lookupImpl: PUBLIC_DNS,
      fetchImpl: async () => ({ statusCode: 200, headers: {}, body: wrong }),
    }), 'plugin-name-mismatch');
  });

  await t.test('target mapping', async () => {
    const context = await candidateFixture();
    await expectCode(qualifyCandidate({
      registry: context.registry,
      inventory: context.inventory,
      policy: sourcePolicy(),
      request: requestFixture({ targets: ['hub'] }),
      projectRoot: context.projectRoot,
      operator: 'alice',
      lookupImpl: PUBLIC_DNS,
      fetchImpl: context.fetchImpl,
    }), 'candidate-target-unregistered');
  });
});

test('candidate without signature/manifest is excluded from plans until manual approval binds its exact hash', async () => {
  const context = await candidateFixture();
  const base = {
    registry: context.registry,
    inventory: context.inventory,
    policy: sourcePolicy(),
    request: requestFixture({ integrity: undefined }),
    projectRoot: context.projectRoot,
    operator: 'alice',
    createdAt: '2026-07-19T13:00:00.000Z',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: context.fetchImpl,
  };
  const unverified = await qualifyCandidate(base);
  assert.equal(unverified.candidate.eligible_for_plan, false);
  assert.equal(unverified.candidate.manual_approval_required, true);
  assert.equal(unverified.candidate.path, null);
  assert.equal(unverified.draft_plan, null);
  await assert.rejects(access(join(context.projectRoot, 'downloads')), (error) => error.code === 'ENOENT');

  await expectCode(qualifyCandidate({
    ...base,
    manualApproval: {
      plugin_id: 'example',
      source_id: 'example-release',
      artifact_id: 'example-2.0.0',
      sha256: '0'.repeat(64),
      approved_by: 'root-operator',
      approved_at: '2026-07-19T13:05:00.000Z',
    },
  }), 'manual-approval-mismatch');

  const approved = await qualifyCandidate({
    ...base,
    manualApproval: {
      plugin_id: 'example',
      source_id: 'example-release',
      artifact_id: 'example-2.0.0',
      sha256: context.sha256,
      approved_by: 'root-operator',
      approved_at: '2026-07-19T13:05:00.000Z',
    },
  });
  assert.equal(approved.candidate.eligible_for_plan, true);
  assert.equal(approved.candidate.integrity.kind, 'manual-hash-approval');
  assert.equal(approved.candidate.path, `downloads/example/${context.sha256}.jar`);
  assert.equal(approved.draft_plan.state, 'draft');
});

test('pinned upstream public key signature independently qualifies the exact JAR bytes', async () => {
  const context = await candidateFixture();
  const { privateKey, publicKey } = generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
  const signer = createSign('SHA256');
  signer.update(context.jar);
  signer.end();
  const signature = signer.sign(privateKey, 'base64');
  const result = await qualifyCandidate({
    registry: context.registry,
    inventory: context.inventory,
    policy: sourcePolicy({
      signatureKeys: { release: publicKey.export({ type: 'spki', format: 'pem' }) },
    }),
    request: requestFixture({
      integrity: { kind: 'upstream-signature', key_id: 'release', signature },
    }),
    projectRoot: context.projectRoot,
    operator: 'alice',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: context.fetchImpl,
  });

  assert.equal(result.candidate.integrity.kind, 'upstream-signature');
  assert.equal(result.candidate.eligible_for_plan, true);
  assert.equal(result.draft_plan.state, 'draft');
});

test('download root cannot be redirected into MCMMORPG docker server paths and fetch never starts', async () => {
  const context = await candidateFixture();
  let fetched = false;
  await expectCode(qualifyCandidate({
    registry: context.registry,
    inventory: context.inventory,
    policy: sourcePolicy(),
    request: requestFixture(),
    projectRoot: context.projectRoot,
    downloadsRoot: '/home/przemek/projects/MC/MCMMORPG/docker/servers',
    operator: 'alice',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async () => { fetched = true; },
  }), 'candidate-download-root-forbidden');
  assert.equal(fetched, false);

  const contents = await readdir(context.projectRoot);
  assert.deepEqual(contents, []);
});

test('an existing symlink cannot impersonate a content-addressed candidate', async () => {
  const context = await candidateFixture();
  const outside = join(context.projectRoot, 'outside.jar');
  const candidateDirectory = join(context.projectRoot, 'downloads', 'example');
  await writeFile(outside, context.jar);
  await mkdir(candidateDirectory, { recursive: true });
  await symlink(outside, join(candidateDirectory, `${context.sha256}.jar`));

  await expectCode(qualifyCandidate({
    registry: context.registry,
    inventory: context.inventory,
    policy: sourcePolicy(),
    request: requestFixture(),
    projectRoot: context.projectRoot,
    operator: 'alice',
    lookupImpl: PUBLIC_DNS,
    fetchImpl: context.fetchImpl,
  }), 'candidate-existing-file-unsafe');
});
