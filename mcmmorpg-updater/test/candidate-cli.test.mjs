import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { access, chmod, mkdir, readFile, readdir, stat, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import {
  CandidateCliError,
  parseCandidateArguments,
  runCandidateQualificationCli,
} from '../src/downloads/candidate-cli.mjs';
import { main } from '../src/downloads/cli.mjs';
import { validExampleJar } from './jar-fixtures.mjs';
import { buildRegistryFixture } from './registry-fixtures.mjs';

const ARTIFACT_URL = 'https://releases.example.test/projects/acme/example/releases/2.0.0.jar';
const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];

function policyFixture() {
  return {
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
      manifest: null,
      redirects: { max: 0, same_host_only: true },
      signature_keys: {},
    }],
  };
}

function validArgs() {
  return [
    '--source-id', 'example-release',
    '--artifact-id', 'pIvQcXW8',
    '--url', ARTIFACT_URL,
    '--plugin', 'example',
    '--targets', 'world,items',
    '--operator', 'alice',
  ];
}

async function buildCliFixture({ withDefaultPolicyTemplate = false } = {}) {
  const fixture = await buildRegistryFixture();
  const projectRoot = join(fixture.root, 'updater');
  const registryRoot = join(projectRoot, 'registry');
  const policyPath = join(fixture.root, 'trusted-sources.json');
  await mkdir(registryRoot, { recursive: true });
  await Promise.all([
    writeFile(join(registryRoot, 'targets.json'), `${JSON.stringify(fixture.targets, null, 2)}\n`),
    writeFile(join(registryRoot, 'plugins.json'), `${JSON.stringify(fixture.plugins, null, 2)}\n`),
    writeFile(policyPath, `${JSON.stringify(policyFixture(), null, 2)}\n`, { mode: 0o640 }),
  ]);
  if (withDefaultPolicyTemplate) {
    const configRoot = join(projectRoot, 'config');
    await mkdir(configRoot, { recursive: true });
    await writeFile(
      join(configRoot, 'trusted-sources.json.example'),
      `${JSON.stringify(policyFixture(), null, 2)}\n`,
    );
  }
  await chmod(policyPath, 0o640);
  return {
    fixture,
    projectRoot,
    policyPath,
    policyOwnerUid: (await stat(policyPath)).uid,
    jar: validExampleJar(),
  };
}

test('first CLI run auto-initializes the default project policy as mode 0600 and qualifies the hash', async () => {
  const context = await buildCliFixture({ withDefaultPolicyTemplate: true });
  const output = await runCandidateQualificationCli(validArgs(), {
    projectRoot: context.projectRoot,
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async () => ({ statusCode: 200, headers: {}, body: context.jar }),
  });

  const policyPath = join(context.projectRoot, 'config', 'trusted-sources.json');
  assert.deepEqual(JSON.parse(await readFile(policyPath, 'utf8')), policyFixture());
  assert.equal((await stat(policyPath)).mode & 0o777, 0o600);
  assert.equal(output.manual_approval_required, true);
  assert.equal(output.candidate_persisted, false);
  assert.equal(output.draft_plan_created, false);
  await assert.rejects(access(join(context.projectRoot, 'downloads')), (error) => error.code === 'ENOENT');
  await assert.rejects(access(join(context.projectRoot, 'data')), (error) => error.code === 'ENOENT');
});

test('operator CLI loads local policy/registry and fresh inventory, then emits a report without persisting or planning', async () => {
  const context = await buildCliFixture();
  const serverPluginEntriesBefore = Object.fromEntries(await Promise.all(
    Object.entries(context.fixture.dataPaths).map(async ([target, root]) => [
      target,
      await readdir(join(root, 'plugins')),
    ]),
  ));
  let fetchCount = 0;
  const output = await runCandidateQualificationCli(validArgs(), {
    projectRoot: context.projectRoot,
    policyPath: context.policyPath,
    expectedPolicyOwnerUid: context.policyOwnerUid,
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async ({ url }) => {
      fetchCount += 1;
      assert.equal(url, ARTIFACT_URL);
      return { statusCode: 200, headers: {}, body: context.jar };
    },
  });

  const expectedSha256 = createHash('sha256').update(context.jar).digest('hex');
  assert.equal(fetchCount, 1);
  assert.deepEqual(output, {
    schema_version: 1,
    candidate_sha256: expectedSha256,
    source_id: 'example-release',
    artifact_id: 'pIvQcXW8',
    plugin_id: 'example',
    targets: ['world', 'items'],
    eligible_for_plan: false,
    manual_approval_required: true,
    candidate_persisted: false,
    draft_plan_created: false,
    report_path: `reports/candidate-${expectedSha256}.report.json`,
  });
  const reportPath = join(context.projectRoot, output.report_path);
  const report = JSON.parse(await readFile(reportPath, 'utf8'));
  assert.equal(report.hashes.sha256, expectedSha256);
  assert.equal(report.plan_status, 'excluded-pending-manual-hash-approval');
  assert.deepEqual(report.targets, ['world', 'items']);
  assert.equal((await stat(reportPath)).mode & 0o777, 0o444);
  await assert.rejects(access(join(context.projectRoot, 'downloads')), (error) => error.code === 'ENOENT');
  await assert.rejects(access(join(context.projectRoot, 'data')), (error) => error.code === 'ENOENT');
  assert.deepEqual(await readdir(context.projectRoot), ['registry', 'reports']);

  const serverPluginEntriesAfter = Object.fromEntries(await Promise.all(
    Object.entries(context.fixture.dataPaths).map(async ([target, root]) => [
      target,
      await readdir(join(root, 'plugins')),
    ]),
  ));
  assert.deepEqual(serverPluginEntriesAfter, serverPluginEntriesBefore);
});

test('CLI fails closed with actionable custom-policy and permission errors', async (t) => {
  const context = await buildCliFixture();

  await t.test('missing policy', async () => {
    await assert.rejects(
      runCandidateQualificationCli(validArgs(), {
        projectRoot: context.projectRoot,
        policyPath: join(context.fixture.root, 'missing-policy.json'),
        expectedPolicyOwnerUid: context.policyOwnerUid,
        lookupImpl: async () => { throw new Error('network must not run'); },
        fetchImpl: async () => { throw new Error('network must not run'); },
      }),
      (error) => {
        assert.ok(error instanceof CandidateCliError);
        assert.equal(error.code, 'trusted-source-policy-missing');
        assert.match(error.message, /regular user-owned file.*0600/i);
        return true;
      },
    );
  });

  await t.test('permission denied', async () => {
    await assert.rejects(
      runCandidateQualificationCli(validArgs(), {
        projectRoot: context.projectRoot,
        policyPath: context.policyPath,
        loadPolicyImpl: async () => {
          const error = new Error('permission denied');
          error.code = 'EACCES';
          throw error;
        },
        lookupImpl: async () => { throw new Error('network must not run'); },
        fetchImpl: async () => { throw new Error('network must not run'); },
      }),
      (error) => {
        assert.ok(error instanceof CandidateCliError);
        assert.equal(error.code, 'trusted-source-policy-permission-denied');
        assert.match(error.message, /current user.*mode 0600/i);
        return true;
      },
    );
  });
});

test('invalid arguments stop before policy, inventory, or transport access', async () => {
  let policyLoaded = false;
  await assert.rejects(
    runCandidateQualificationCli([
      '--source-id', 'example-release',
      '--artifact-id', 'example-2.0.0',
      '--url', ARTIFACT_URL,
      '--plugin', 'example',
      '--targets', 'world,world',
    ], {
      projectRoot: '/does/not/matter',
      loadPolicyImpl: async () => { policyLoaded = true; },
    }),
    (error) => error.code === 'candidate-cli-arguments-invalid' && error.exitCode === 2,
  );
  assert.equal(policyLoaded, false);
  assert.throws(
    () => parseCandidateArguments([...validArgs(), '--shell', 'rm -rf /']),
    (error) => error.code === 'candidate-cli-arguments-invalid',
  );
});

test('artifact IDs are case-sensitive safe tokens while source and plugin IDs stay lowercase-only', () => {
  assert.equal(parseCandidateArguments(validArgs()).artifactId, 'pIvQcXW8');

  for (const artifactId of ['contains space', '../traversal', 'path/segment', 'path\\segment', '.', '..', `control\u0000char`]) {
    const args = validArgs();
    args[3] = artifactId;
    assert.throws(
      () => parseCandidateArguments(args),
      (error) => error.code === 'candidate-cli-arguments-invalid',
      `artifact ID should be rejected: ${JSON.stringify(artifactId)}`,
    );
  }

  for (const [index, value] of [[1, 'Example-release'], [7, 'Example']]) {
    const args = validArgs();
    args[index] = value;
    assert.throws(
      () => parseCandidateArguments(args),
      (error) => error.code === 'candidate-cli-arguments-invalid' && /lowercase identifier/.test(error.message),
    );
  }
});

test('CLI entrypoint writes one machine-readable JSON record and maps argument errors to exit code 2', async () => {
  const stdout = [];
  const stderr = [];
  const result = { candidate_sha256: 'a'.repeat(64), eligible_for_plan: false, manual_approval_required: true };
  assert.equal(await main({
    argv: validArgs(),
    stdout: { write: (value) => stdout.push(value) },
    stderr: { write: (value) => stderr.push(value) },
    runImpl: async () => result,
  }), 0);
  assert.deepEqual(JSON.parse(stdout.join('')), result);
  assert.deepEqual(stderr, []);

  assert.equal(await main({
    argv: [],
    stdout: { write: (value) => stdout.push(value) },
    stderr: { write: (value) => stderr.push(value) },
    runImpl: async () => { throw new CandidateCliError('candidate-cli-arguments-invalid', 'bad args', { exitCode: 2 }); },
  }), 2);
  assert.deepEqual(JSON.parse(stderr.join('')), {
    ok: false,
    error: { code: 'candidate-cli-arguments-invalid', message: 'bad args' },
  });
});
