import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { access, mkdtemp, readFile, stat, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import {
  NEXO_RESOURCE_ID,
  VoxelApiError,
  createVoxelApiClient,
  normalizeDownloadUrlResponse,
  normalizeVoxelUpdates,
  redactVoxelSecrets,
} from '../src/voxel/index.mjs';
import { runVoxelCli } from '../src/voxel/cli.mjs';
import { buildJar } from './jar-fixtures.mjs';

const SECRET_TOKEN = 'eyJpZCI6IjcyNjMxIiwidmFsIjoic2VjcmV0LXRlc3QtdG9rZW4ifQ';

function updatesResponse() {
  return {
    request: {},
    response: {
      success: true,
      result_count: '2',
      total: '2',
      resource: { id: String(NEXO_RESOURCE_ID) },
      updates: [
        {
          id: '9002',
          version: '1.24.0',
          title: 'Nexo 1.24.0',
          description: 'Release notes for Nexo 1.24.0. Explicit Paper 1.21.11 support.',
          time: '1781708716',
          url: 'https://voxel.shop/product/6901/nexo/update/9002',
          snapshot: '0',
          beta: '0',
          downloadReady: '1',
        },
        {
          id: '9001',
          version: '1.23.9',
          title: 'Nexo 1.23.9',
          description: 'Maintenance update.',
          time: '1781608716',
          url: 'https://voxel.shop/product/6901/nexo/update/9001',
          snapshot: '0',
          beta: '0',
          downloadReady: '1',
        },
      ],
    },
  };
}

test('Voxel client posts official actions with mockable transport', async () => {
  const calls = [];
  const client = createVoxelApiClient({
    transport: async (request) => {
      calls.push(request);
      return { statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify(updatesResponse())) };
    },
  });

  const raw = await client.getResourceUpdates({ resource_id: NEXO_RESOURCE_ID, limit: 50 });
  const updates = normalizeVoxelUpdates(raw, { resourceId: NEXO_RESOURCE_ID, minecraftVersion: '1.21.11' });
  assert.equal(calls[0].url, 'https://api.voxel.shop/v1/getResourceUpdates');
  assert.equal(calls[0].method, 'POST');
  assert.match(calls[0].body, /resource_id=6901/);
  assert.equal(updates.updates[0].version, '1.24.0');
  assert.equal(updates.updates[0].compatible, true);
  assert.match(updates.content_sha256, /^[0-9a-f]{64}$/);
});

test('Voxel updates auth barrier is reported as auth_required boundary', () => {
  assert.throws(
    () => normalizeVoxelUpdates({ response: { success: false, message: 'AUTH_REQUIRED' } }, { resourceId: NEXO_RESOURCE_ID }),
    (error) => error instanceof VoxelApiError && error.code === 'voxel-auth-required' && error.boundary === 'auth_required',
  );
});

test('Voxel redaction preserves hashes but removes auth tokens', () => {
  const hash = 'a'.repeat(64);
  const redacted = redactVoxelSecrets({
    content_sha256: hash,
    token: SECRET_TOKEN,
    url: `https://voxel.shop/download/?token=${SECRET_TOKEN}&resource=6901`,
  });
  assert.equal(redacted.content_sha256, hash);
  assert.equal(redacted.token, '[REDACTED]');
  assert.doesNotMatch(redacted.url, new RegExp(SECRET_TOKEN));
});

test('Voxel download URL qualification rejects bad ids and unpinned URLs', async () => {
  const client = createVoxelApiClient({
    transport: async () => ({ statusCode: 200, headers: {}, body: Buffer.from(JSON.stringify({ response: { success: true, result: {} } })) }),
  });
  assert.throws(
    () => client.getDownloadURL({ resource_id: 4, token: SECRET_TOKEN }),
    (error) => error.code === 'voxel-resource-id-forbidden',
  );
  assert.throws(
    () => normalizeDownloadUrlResponse({ response: { success: true, result: { url: 'https://evil.example/download/x.jar' } } }),
    (error) => error.code === 'voxel-download-url-forbidden',
  );
});

test('Voxel CLI stores token outside repo with 0600 mode and redacts output', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'voxel-token-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const tokenPath = join(root, 'voxel-token.json');
  const chunks = [];
  const stdout = { write: (chunk) => chunks.push(chunk) };
  const result = await runVoxelCli(['authorize', '--token-file', tokenPath], {
    stdout,
    client: {
      authorizeUser: async () => ({ response: { success: true, result: { url: 'https://voxel.shop/authorize/mock' } } }),
    },
    listenImpl: async () => ({
      returnUrl: 'http://127.0.0.1:39123/callback',
      waitForToken: async () => SECRET_TOKEN,
      close: async () => true,
    }),
  });

  assert.equal(result.token_stored, true);
  assert.equal((await stat(tokenPath)).mode & 0o777, 0o600);
  assert.equal(JSON.parse(await readFile(tokenPath, 'utf8')).token, SECRET_TOKEN);
  assert.doesNotMatch(chunks.join(''), new RegExp(SECRET_TOKEN));
});

test('Voxel CLI refuses missing and unverified tokens before download URL qualification', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'voxel-token-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const tokenPath = join(root, 'voxel-token.json');
  await assert.rejects(
    runVoxelCli(['download-url', '--token-file', tokenPath], { client: {} }),
    (error) => error.code === 'voxel-token-missing' && error.boundary === 'auth_required',
  );

  await writeFile(tokenPath, `${JSON.stringify({ schema_version: 1, token: SECRET_TOKEN })}\n`, { mode: 0o600 });
  await assert.rejects(
    runVoxelCli(['download-url', '--token-file', tokenPath], {
      client: { verifyAuthToken: async () => ({ response: { success: false } }) },
    }),
    (error) => error.code === 'voxel-token-unverified' && error.boundary === 'auth_required',
  );
});

test('Voxel CLI qualifies download URL without downloading a JAR', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'voxel-token-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const tokenPath = join(root, 'voxel-token.json');
  await writeFile(tokenPath, `${JSON.stringify({ schema_version: 1, token: SECRET_TOKEN })}\n`, { mode: 0o600 });
  const result = await runVoxelCli(['download-url', '--token-file', tokenPath], {
    client: {
      verifyAuthToken: async () => ({ response: { success: true, result: { user: { id: '72631' }, expires: '1784300716' } } }),
      getDownloadURL: async () => ({
        response: {
          success: true,
          result: { url: 'https://voxel.shop/download/?resource=6901&sig=abc', version: '1.24.0', expires: '1781708776' },
        },
      }),
    },
  });

  assert.equal(result.boundary, 'download_url_qualified_only');
  assert.equal(result.version, '1.24.0');
  assert.match(result.url, /^https:\/\/voxel\.shop\/download\//);
});

test('Voxel download-candidate persists Nexo JAR content-addressed read-only without server writes', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'voxel-download-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const tokenPath = join(root, 'voxel-token.json');
  await writeFile(tokenPath, `${JSON.stringify({ schema_version: 1, token: SECRET_TOKEN })}\n`, { mode: 0o600 });
  const jar = nexoJar('1.21');
  const sha256 = createHash('sha256').update(jar).digest('hex');
  const result = await runVoxelCli([
    'download-candidate',
    '--resource-id',
    '6901',
    '--version',
    '1.21',
    '--token-file',
    tokenPath,
  ], {
    projectRoot: root,
    client: verifiedDownloadClient({ version: '1.21' }),
    lookupImpl: async () => [{ address: '93.184.216.34', family: 4 }],
    fetchImpl: async () => ({ statusCode: 200, headers: {}, body: jar }),
    stdout: { write() {} },
  });
  assert.equal(result.path, `downloads/nexo/${sha256}.jar`);
  assert.equal(result.sha256, sha256);
  assert.equal(result.plugin_yml.name, 'Nexo');
  assert.equal((await stat(join(root, result.path))).mode & 0o777, 0o444);
  assert.deepEqual(await readFile(join(root, result.path)), jar);
  await assert.rejects(access(join(root, 'MCMMORPG')), (error) => error.code === 'ENOENT');
});

test('Voxel download-candidate auth_required boundary makes no writes', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'voxel-download-auth-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const tokenPath = join(root, 'missing-token.json');
  await assert.rejects(
    runVoxelCli(['download-candidate', '--resource-id', '6901', '--token-file', tokenPath], {
      projectRoot: root,
      client: {},
      stdout: { write() {} },
    }),
    (error) => error.code === 'voxel-token-missing' && error.boundary === 'auth_required',
  );
  await assert.rejects(access(join(root, 'downloads')), (error) => error.code === 'ENOENT');
});

test('Voxel download-candidate blocks bad resource id URL private DNS and plugin mismatch', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'voxel-download-blocked-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const tokenPath = join(root, 'voxel-token.json');
  await writeFile(tokenPath, `${JSON.stringify({ schema_version: 1, token: SECRET_TOKEN })}\n`, { mode: 0o600 });
  const base = {
    projectRoot: root,
    stdout: { write() {} },
    client: verifiedDownloadClient({ version: '1.21' }),
  };

  await assert.rejects(
    runVoxelCli(['download-candidate', '--resource-id', '6902', '--token-file', tokenPath], base),
    (error) => error.code === 'voxel-resource-id-forbidden',
  );
  await assert.rejects(
    runVoxelCli(['download-candidate', '--resource-id', '6901', '--token-file', tokenPath], {
      ...base,
      client: {
        ...base.client,
        getDownloadURL: async () => ({ response: { success: true, result: { url: 'https://evil.example.test/download/nexo.jar', version: '1.21' } } }),
      },
    }),
    (error) => error.code === 'voxel-download-url-forbidden',
  );
  await assert.rejects(
    runVoxelCli(['download-candidate', '--resource-id', '6901', '--token-file', tokenPath], {
      ...base,
      lookupImpl: async () => [{ address: '10.0.0.1', family: 4 }],
      fetchImpl: async () => ({ statusCode: 200, headers: {}, body: nexoJar('1.21') }),
    }),
    (error) => error.code === 'source-address-forbidden',
  );
  await assert.rejects(
    runVoxelCli(['download-candidate', '--resource-id', '6901', '--token-file', tokenPath], {
      ...base,
      lookupImpl: async () => [{ address: '93.184.216.34', family: 4 }],
      fetchImpl: async () => ({ statusCode: 200, headers: {}, body: buildJar([{ name: 'plugin.yml', body: 'name: Other\nversion: 1.21\n' }]) }),
    }),
    (error) => error.code === 'plugin-name-mismatch',
  );
});

function verifiedDownloadClient({ version }) {
  return {
    verifyAuthToken: async () => ({ response: { success: true, result: { user: { id: '72631' } } } }),
    getDownloadURL: async () => ({ response: { success: true, result: { url: 'https://voxel.shop/download/nexo.jar?resource=6901&sig=abc', version } } }),
  };
}

function nexoJar(version) {
  return buildJar([
    { name: 'plugin.yml', body: `name: Nexo\nversion: ${version}\nmain: com.nexomc.NexoPlugin\n` },
    { name: 'META-INF/MANIFEST.MF', body: 'Manifest-Version: 1.0\n' },
  ]);
}
