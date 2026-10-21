import { createHash } from 'node:crypto';
import { lookup } from 'node:dns/promises';
import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import {
  NEXO_RESOURCE_ID,
  VoxelApiError,
  createVoxelApiClient,
  createVoxelHttpsTransport,
  generateVoxelState,
  normalizeDownloadUrlResponse,
  normalizeResourceId,
  normalizeVoxelUpdates,
  redactVoxelSecrets,
} from './api-client.mjs';
import { persistVoxelNexoDownloadCandidate } from './download-candidate.mjs';
import {
  defaultVoxelTokenPath,
  deleteVoxelToken,
  readVoxelToken,
  writeVoxelToken,
} from './token-store.mjs';

const defaultProjectRoot = fileURLToPath(new URL('../../', import.meta.url));

export class VoxelCliError extends Error {
  constructor(code, message, exitCode = 1) {
    super(message);
    this.name = 'VoxelCliError';
    this.code = code;
    this.exitCode = exitCode;
  }
}

export function parseVoxelArguments(argv) {
  if (!Array.isArray(argv) || argv.length === 0) throw usage();
  const [command, ...rest] = argv;
  const options = { command, resource_id: NEXO_RESOURCE_ID, token_file: null, timeout_ms: 120_000, port: 0, open: false, version: null };
  for (let index = 0; index < rest.length; index += 1) {
    const key = rest[index];
    if (key === '--open') {
      options.open = true;
      continue;
    }
    const value = rest[index + 1];
    if (value === undefined) throw usage();
    index += 1;
    if (key === '--resource-id') options.resource_id = normalizeResourceId(value);
    else if (key === '--token-file') options.token_file = value;
    else if (key === '--timeout-ms') options.timeout_ms = parsePositiveInt(value, 'timeout-ms');
    else if (key === '--port') options.port = parseNonNegativeInt(value, 'port');
    else if (key === '--version') options.version = value;
    else throw usage();
  }
  if (!['authorize', 'verify', 'invalidate', 'updates', 'download-url', 'download-candidate'].includes(command)) throw usage();
  return options;
}

export async function runVoxelCli(argv, {
  client = createVoxelApiClient({ transport: createVoxelHttpsTransport({ lookupImpl: lookup }) }),
  tokenPath,
  projectRoot = defaultProjectRoot,
  lookupImpl = lookup,
  fetchImpl,
  openBrowserImpl = openBrowser,
  listenImpl = listenForTokenCallback,
  stdout = process.stdout,
} = {}) {
  const options = parseVoxelArguments(argv);
  const effectiveTokenPath = options.token_file ?? tokenPath ?? defaultVoxelTokenPath();

  if (options.command === 'authorize') {
    const state = generateVoxelState();
    const listener = await listenImpl({ port: options.port, state, timeoutMs: options.timeout_ms });
    const auth = await client.authorizeUser({
      service: '127.0.0.1',
      return_url: listener.returnUrl,
      state,
    });
    const authorizeUrl = auth?.response?.result?.url;
    if (typeof authorizeUrl !== 'string' || authorizeUrl.length === 0) {
      await listener.close();
      throw new VoxelApiError('voxel-authorize-response-malformed', 'Voxel authorize response missing browser URL');
    }
    stdout.write(`${JSON.stringify({ ok: true, command: 'authorize', authorize_url: authorizeUrl, token_file: effectiveTokenPath, waiting_ms: options.timeout_ms })}\n`);
    if (options.open) openBrowserImpl(authorizeUrl);
    const token = await listener.waitForToken();
    await writeVoxelToken(token, { tokenPath: effectiveTokenPath });
    await listener.close();
    return { ok: true, command: 'authorize', token_file: effectiveTokenPath, token_stored: true };
  }

  if (options.command === 'updates') {
    const raw = await client.getResourceUpdates({ resource_id: options.resource_id, limit: 50 });
    const updates = normalizeVoxelUpdates(raw, { resourceId: options.resource_id, minecraftVersion: '1.21.11' });
    return {
      ok: true,
      command: 'updates',
      resource_id: updates.resource_id,
      boundary: null,
      content_sha256: updates.content_sha256,
      versions: updates.updates.map((update) => publicUpdate(update)),
    };
  }

  const token = await readVoxelToken({ tokenPath: effectiveTokenPath });
  const verified = await client.verifyAuthToken(token);
  if (verified?.response?.success !== true && verified?.response?.success !== 'true') {
    throw new VoxelApiError('voxel-token-unverified', 'Voxel token could not be verified', { boundary: 'auth_required' });
  }

  if (options.command === 'verify') {
    return {
      ok: true,
      command: 'verify',
      token_file: effectiveTokenPath,
      token_verified: true,
      expires: verified?.response?.result?.expires ?? null,
      user_id: verified?.response?.result?.user?.id ?? null,
    };
  }

  if (options.command === 'invalidate') {
    const invalidated = await client.invalidateAuthToken(token);
    await deleteVoxelToken({ tokenPath: effectiveTokenPath });
    return {
      ok: true,
      command: 'invalidate',
      token_file: effectiveTokenPath,
      token_deleted: true,
      result: redactVoxelSecrets(invalidated?.response?.result ?? {}),
    };
  }

  if (options.command === 'download-url') {
    const raw = await client.getDownloadURL({ resource_id: options.resource_id, token });
    const qualified = normalizeDownloadUrlResponse(raw, { resourceId: options.resource_id });
    return {
      ok: true,
      command: 'download-url',
      ...qualified,
      note: 'JAR download and persistence intentionally not performed by this command.',
    };
  }

  if (options.command === 'download-candidate') {
    return persistVoxelNexoDownloadCandidate({
      client,
      resourceId: options.resource_id,
      token,
      version: options.version ?? undefined,
      projectRoot,
      lookupImpl,
      fetchImpl: fetchImpl ?? createVoxelDownloadFetch(),
    });
  }

  throw usage();
}

export async function main({
  argv = process.argv.slice(2),
  stdout = process.stdout,
  stderr = process.stderr,
  runImpl = runVoxelCli,
} = {}) {
  try {
    const result = await runImpl(argv, { stdout });
    stdout.write(`${JSON.stringify(redactVoxelSecrets(result))}\n`);
    return 0;
  } catch (error) {
    stderr.write(`${JSON.stringify(redactVoxelSecrets({
      ok: false,
      error: {
        code: error?.code ?? 'voxel-cli-failed',
        message: error?.message ?? 'Voxel CLI failed',
        boundary: error?.boundary ?? null,
      },
    }))}\n`);
    return Number.isInteger(error?.exitCode) ? error.exitCode : 1;
  }
}

export async function listenForTokenCallback({ port = 0, state, timeoutMs = 120_000 } = {}) {
  let server;
  let timer;
  let resolved = false;
  const waitForToken = new Promise((resolve, reject) => {
    server = createServer(async (request, response) => {
      try {
        const { token, callbackState } = await readCallbackPayload(request);
        if (callbackState !== state) throw new VoxelApiError('voxel-authorize-state-mismatch', 'Voxel authorize callback state mismatch');
        resolved = true;
        response.writeHead(200, { 'content-type': 'text/plain; charset=utf-8' });
        response.end('Voxel authorization received. You can close this window.\n');
        resolve(token);
      } catch (error) {
        response.writeHead(400, { 'content-type': 'text/plain; charset=utf-8' });
        response.end('Voxel authorization failed.\n');
        reject(error);
      }
    });
    server.once('error', reject);
    timer = setTimeout(() => {
      reject(new VoxelApiError('voxel-authorize-timeout', 'Timed out waiting for Voxel authorize callback'));
    }, timeoutMs);
  });
  await new Promise((resolve, reject) => {
    server.listen({ host: '127.0.0.1', port }, resolve);
    server.once('error', reject);
  });
  const address = server.address();
  return {
    returnUrl: `http://127.0.0.1:${address.port}/callback`,
    waitForToken: () => waitForToken.finally(() => clearTimeout(timer)),
    close: () => new Promise((resolve) => server.close(() => resolve(resolved))),
  };
}

function openBrowser(url) {
  if (process.platform === 'linux') {
    const child = spawn('xdg-open', [url], { detached: true, stdio: 'ignore' });
    child.unref();
  }
}

async function readCallbackPayload(request) {
  const url = new URL(request.url, 'http://127.0.0.1');
  let params = url.searchParams;
  if (request.method === 'POST') {
    const chunks = [];
    for await (const chunk of request) chunks.push(chunk);
    params = new URLSearchParams(Buffer.concat(chunks).toString('utf8'));
  }
  const token = params.get('token');
  const callbackState = params.get('state');
  if (!token) throw new VoxelApiError('voxel-authorize-token-missing', 'Voxel authorize callback did not include token');
  return { token, callbackState };
}

function publicUpdate(update) {
  return {
    id: update.id,
    version: update.version,
    channel: update.channel,
    version_type: update.version_type,
    title: update.title,
    description_sha256: update.description ? createHash('sha256').update(update.description).digest('hex') : null,
    date_published: update.date_published,
    url: update.url,
    download_ready: update.download_ready,
    compatible: update.compatible,
    compatibility: update.compatibility,
    game_versions: update.game_versions,
    loaders: update.loaders,
  };
}

function parsePositiveInt(value, label) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) throw new VoxelCliError('voxel-cli-arguments-invalid', `${label} must be positive`, 2);
  return parsed;
}

function parseNonNegativeInt(value, label) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 0) throw new VoxelCliError('voxel-cli-arguments-invalid', `${label} must be non-negative`, 2);
  return parsed;
}

function createVoxelDownloadFetch() {
  return async function voxelDownloadFetch({ url, maxBytes }) {
    const response = await fetch(url, { method: 'GET', redirect: 'manual' });
    const declaredLength = response.headers.get('content-length');
    if (declaredLength !== null && /^\d+$/.test(declaredLength) && Number(declaredLength) > maxBytes) {
      return { statusCode: 413, headers: response.headers, body: Buffer.alloc(0) };
    }
    const body = Buffer.from(await response.arrayBuffer());
    if (body.length > maxBytes) {
      return { statusCode: 413, headers: response.headers, body };
    }
    return { statusCode: response.status, headers: response.headers, body };
  };
}

function usage() {
  return new VoxelCliError(
    'voxel-cli-arguments-invalid',
    'usage: npm run voxel -- authorize|verify|invalidate|updates|download-url|download-candidate [--resource-id 6901] [--version version] [--token-file path] [--timeout-ms ms] [--port port] [--open]',
    2,
  );
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  process.exitCode = await main();
}
