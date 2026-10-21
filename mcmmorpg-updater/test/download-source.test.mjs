import test from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';
import { mkdtemp, readFile, rm, stat, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import {
  CandidateSafetyError,
  createHttpsTransport,
  initializeTrustedSourcePolicy,
  loadTrustedSourcePolicy,
  secureFetch,
  validateTrustedSourcePolicy,
} from '../src/downloads/index.mjs';

const PUBLIC_DNS = async () => [{ address: '93.184.216.34', family: 4 }];

function policyFixture() {
  return {
    schema_version: 1,
    sources: [
      {
        id: 'example-release',
        adapter: 'pinned-https-release',
        plugin_id: 'example',
        plugin_name: 'ExamplePlugin',
        project: 'acme/example',
        artifact: {
          host: 'releases.example.test',
          project_path: '/projects/acme/example',
          path_prefix: '/projects/acme/example/releases/',
          max_bytes: 1024,
        },
        manifest: {
          host: 'checksums.example.test',
          project_path: '/projects/acme/example',
          path_prefix: '/projects/acme/example/manifests/',
          max_bytes: 4096,
        },
        redirects: { max: 1, same_host_only: true },
        signature_keys: {},
      },
    ],
  };
}

function expectCode(promise, code) {
  return assert.rejects(promise, (error) => {
    assert.ok(error instanceof CandidateSafetyError);
    assert.equal(error.code, code);
    return true;
  });
}

test('trusted source policy pins HTTPS host, project path, redirects, and byte limits', () => {
  const validated = validateTrustedSourcePolicy(policyFixture());
  assert.equal(validated.sources[0].artifact.host, 'releases.example.test');

  const bad = policyFixture();
  bad.sources[0].artifact.path_prefix = '/other-project/releases/';
  assert.throws(
    () => validateTrustedSourcePolicy(bad),
    (error) => error.code === 'source-project-path-mismatch',
  );
});

test('policy initializer creates a private user-owned policy from the approved template once', async (t) => {
  const root = await mkdtemp(join(tmpdir(), 'mcmmorpg-policy-'));
  t.after(() => rm(root, { recursive: true, force: true }));
  const templatePath = join(root, 'trusted-sources.json.example');
  const policyPath = join(root, 'config', 'trusted-sources.json');
  await writeFile(templatePath, `${JSON.stringify(policyFixture(), null, 2)}\n`);

  assert.equal(await initializeTrustedSourcePolicy(policyPath, { templatePath }), true);
  assert.equal((await stat(policyPath)).mode & 0o777, 0o600);
  assert.deepEqual(JSON.parse(await readFile(policyPath, 'utf8')), policyFixture());

  await writeFile(templatePath, '{"schema_version":1,"sources":[]}\n');
  assert.equal(await initializeTrustedSourcePolicy(policyPath, { templatePath }), false);
  assert.deepEqual(JSON.parse(await readFile(policyPath, 'utf8')), policyFixture());
});

test('policy loader accepts a regular policy owned by the current user', async () => {
  const text = JSON.stringify(policyFixture());
  const safeMetadata = {
    uid: 1000,
    mode: 0o100600,
    isFile: () => true,
    isSymbolicLink: () => false,
  };
  const loaded = await loadTrustedSourcePolicy('/workspace/config/trusted-sources.json', {
    expectedOwnerUid: 1000,
    lstatImpl: async () => safeMetadata,
    readFileImpl: async () => text,
  });
  assert.equal(loaded.sources[0].id, 'example-release');
});

test('policy loader rejects a policy owned by another user', async () => {
  const text = JSON.stringify(policyFixture());
  const safeMetadata = {
    uid: 1001,
    mode: 0o100600,
    isFile: () => true,
    isSymbolicLink: () => false,
  };
  await expectCode(loadTrustedSourcePolicy('/workspace/config/trusted-sources.json', {
    expectedOwnerUid: 1000,
    lstatImpl: async () => safeMetadata,
    readFileImpl: async () => text,
  }), 'source-policy-ownership');
});

test('policy loader rejects a symlink even when it targets a regular private file', async () => {
  await expectCode(loadTrustedSourcePolicy('/workspace/config/trusted-sources.json', {
    expectedOwnerUid: 1000,
    lstatImpl: async () => ({
      uid: 1000,
      mode: 0o120777,
      isFile: () => false,
      isSymbolicLink: () => true,
    }),
    readFileImpl: async () => {
      throw new Error('symlink contents must not be read');
    },
  }), 'source-policy-file-unsafe');
});

test('policy loader rejects group-writable and world-writable policy files', async (t) => {
  const text = JSON.stringify(policyFixture());
  const metadata = {
    uid: 1000,
    mode: 0o100600,
    isFile: () => true,
    isSymbolicLink: () => false,
  };

  for (const [label, mode] of [['group-writable', 0o100660], ['world-writable', 0o100642]]) {
    await t.test(label, async () => {
      await expectCode(loadTrustedSourcePolicy('/workspace/config/trusted-sources.json', {
        expectedOwnerUid: 1000,
        lstatImpl: async () => ({ ...metadata, mode }),
        readFileImpl: async () => text,
      }), 'source-policy-ownership');
    });
  }
});

test('secure fetch accepts only the pinned HTTPS source and passes resolved public IPs to transport', async () => {
  const source = validateTrustedSourcePolicy(policyFixture()).sources[0];
  let transportRequest;
  const body = await secureFetch({
    url: 'https://releases.example.test/projects/acme/example/releases/2.0.0.jar',
    endpoint: source.artifact,
    redirects: source.redirects,
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async (request) => {
      transportRequest = request;
      return { statusCode: 200, headers: { 'content-length': '3' }, body: Buffer.from('jar') };
    },
  });

  assert.equal(body.toString(), 'jar');
  assert.deepEqual(transportRequest.allowedAddresses, [{ address: '93.184.216.34', family: 4 }]);
});

test('native HTTPS transport connects through the already-validated pinned DNS answers', async () => {
  let requestOptions;
  const requestImpl = (options, onResponse) => {
    requestOptions = options;
    const request = new EventEmitter();
    request.setTimeout = () => request;
    request.destroy = (error) => request.emit('error', error);
    request.end = () => {
      const response = new PassThrough();
      response.statusCode = 200;
      response.headers = { 'content-length': '3' };
      onResponse(response);
      response.end('jar');
    };
    return request;
  };
  const transport = createHttpsTransport({ requestImpl });
  const response = await transport({
    url: 'https://releases.example.test/projects/acme/example/releases/2.0.0.jar',
    hostname: 'releases.example.test',
    allowedAddresses: [{ address: '93.184.216.34', family: 4 }],
    maxBytes: 1024,
  });
  assert.equal(response.body.toString(), 'jar');

  const resolved = await new Promise((resolve, reject) => {
    requestOptions.lookup('releases.example.test', { all: true }, (error, addresses) => {
      if (error) reject(error); else resolve(addresses);
    });
  });
  assert.deepEqual(resolved, [{ address: '93.184.216.34', family: 4 }]);
});

test('secure fetch blocks SSRF DNS answers before transport is called', async () => {
  const source = validateTrustedSourcePolicy(policyFixture()).sources[0];
  let called = false;
  await expectCode(secureFetch({
    url: 'https://releases.example.test/projects/acme/example/releases/2.0.0.jar',
    endpoint: source.artifact,
    redirects: source.redirects,
    lookupImpl: async () => [
      { address: '93.184.216.34', family: 4 },
      { address: '169.254.169.254', family: 4 },
    ],
    fetchImpl: async () => { called = true; },
  }), 'source-address-forbidden');
  assert.equal(called, false);
});

test('secure fetch blocks IPv4-mapped IPv6 loopback answers', async () => {
  const source = validateTrustedSourcePolicy(policyFixture()).sources[0];
  await expectCode(secureFetch({
    url: 'https://releases.example.test/projects/acme/example/releases/2.0.0.jar',
    endpoint: source.artifact,
    redirects: source.redirects,
    lookupImpl: async () => [{ address: '::ffff:7f00:1', family: 6 }],
    fetchImpl: async () => ({ statusCode: 200, headers: {}, body: Buffer.from('jar') }),
  }), 'source-address-forbidden');
});

test('secure fetch blocks cross-host redirects even when the destination resolves publicly', async () => {
  const source = validateTrustedSourcePolicy(policyFixture()).sources[0];
  await expectCode(secureFetch({
    url: 'https://releases.example.test/projects/acme/example/releases/2.0.0.jar',
    endpoint: source.artifact,
    redirects: source.redirects,
    lookupImpl: PUBLIC_DNS,
    fetchImpl: async () => ({
      statusCode: 302,
      headers: { location: 'https://cdn.example.test/projects/acme/example/releases/2.0.0.jar' },
      body: Buffer.alloc(0),
    }),
  }), 'source-redirect-host-forbidden');
});

test('secure fetch blocks declared and actual bodies above the source byte limit', async (t) => {
  const source = validateTrustedSourcePolicy(policyFixture()).sources[0];
  await t.test('content-length', async () => {
    await expectCode(secureFetch({
      url: 'https://releases.example.test/projects/acme/example/releases/big.jar',
      endpoint: source.artifact,
      redirects: source.redirects,
      lookupImpl: PUBLIC_DNS,
      fetchImpl: async () => ({
        statusCode: 200,
        headers: { 'content-length': '1025' },
        body: Buffer.alloc(0),
      }),
    }), 'source-response-oversized');
  });

  await t.test('actual bytes', async () => {
    await expectCode(secureFetch({
      url: 'https://releases.example.test/projects/acme/example/releases/big.jar',
      endpoint: source.artifact,
      redirects: source.redirects,
      lookupImpl: PUBLIC_DNS,
      fetchImpl: async () => ({ statusCode: 200, headers: {}, body: Buffer.alloc(1025) }),
    }), 'source-response-oversized');
  });
});
