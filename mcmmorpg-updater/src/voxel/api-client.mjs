import { createHash, randomBytes } from 'node:crypto';
import { request as httpsRequest } from 'node:https';
import { isIP } from 'node:net';
import { classifyCompatibilityEvidence } from '../compatibility/evidence.mjs';

export const VOXEL_API_ORIGIN = 'https://api.voxel.shop';
export const VOXEL_API_ENDPOINT = Object.freeze({
  host: 'api.voxel.shop',
  path_prefix: '/v1/',
  max_bytes: 2 * 1024 * 1024,
});
export const NEXO_RESOURCE_ID = 6901;

const ACTION = /^[A-Za-z][A-Za-z0-9]*$/;
const TOKEN = /^[A-Za-z0-9._~+/=-]{1,254}$/;
const RESOURCE_ID = /^[1-9][0-9]{0,9}$/;
const SAFE_STATE = /^[A-Za-z0-9_-]{24,128}$/;

export class VoxelApiError extends Error {
  constructor(code, message, { boundary, statusCode } = {}) {
    super(message);
    this.name = 'VoxelApiError';
    this.code = code;
    this.boundary = boundary ?? null;
    this.statusCode = statusCode ?? null;
  }
}

export function createVoxelApiClient({
  transport = createVoxelHttpsTransport(),
  apiOrigin = VOXEL_API_ORIGIN,
} = {}) {
  if (typeof transport !== 'function') {
    throw new TypeError('Voxel API client requires a transport function');
  }

  const origin = normalizeApiOrigin(apiOrigin);

  return Object.freeze({
    authorizeUser(params) {
      return callVoxelApi({ origin, transport, action: 'authorizeUser', params: normalizeAuthorizeParams(params) });
    },
    verifyAuthToken(token) {
      return callVoxelApi({ origin, transport, action: 'verifyAuthToken', params: { token: normalizeToken(token) } });
    },
    invalidateAuthToken(token) {
      return callVoxelApi({ origin, transport, action: 'invalidateAuthToken', params: { token: normalizeToken(token) } });
    },
    getResourceUpdates(params) {
      const resourceId = normalizeResourceId(params?.resource_id);
      return callVoxelApi({
        origin,
        transport,
        action: 'getResourceUpdates',
        params: {
          resource_id: String(resourceId),
          start: String(normalizeNonNegativeInt(params?.start ?? 0, 'start')),
          limit: String(normalizeLimit(params?.limit ?? 50)),
        },
      });
    },
    getDownloadURL(params) {
      const resourceId = normalizeResourceId(params?.resource_id);
      if (resourceId !== NEXO_RESOURCE_ID) {
        throw new VoxelApiError('voxel-resource-id-forbidden', 'Voxel download URL qualification is pinned to Nexo resource 6901');
      }
      return callVoxelApi({
        origin,
        transport,
        action: 'getDownloadURL',
        params: {
          resource_id: String(resourceId),
          token: normalizeToken(params?.token),
          allow_redirects: '0',
        },
      });
    },
  });
}

export async function callVoxelApi({ origin = VOXEL_API_ORIGIN, transport, action, params = {} }) {
  if (typeof transport !== 'function') {
    throw new TypeError('callVoxelApi requires a transport function');
  }
  if (typeof action !== 'string' || !ACTION.test(action)) {
    throw new VoxelApiError('voxel-action-invalid', 'Voxel API action is invalid');
  }

  const url = new URL(`/v1/${action}`, normalizeApiOrigin(origin));
  const body = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null) body.set(key, String(value));
  }

  const response = await transport({
    url: url.href,
    method: 'POST',
    headers: {
      accept: 'application/json',
      'content-type': 'application/x-www-form-urlencoded',
      'user-agent': 'mcmmorpg-updater/0.1',
    },
    body: body.toString(),
    maxBytes: VOXEL_API_ENDPOINT.max_bytes,
  });

  if (!response || typeof response !== 'object' || !Number.isInteger(response.statusCode) || !Buffer.isBuffer(response.body)) {
    throw new VoxelApiError('voxel-transport-malformed', 'Voxel API transport returned malformed response');
  }
  if (response.statusCode !== 200) {
    throw new VoxelApiError('voxel-http-status', `Voxel API returned HTTP ${response.statusCode}`, { statusCode: response.statusCode });
  }

  let parsed;
  try {
    parsed = JSON.parse(response.body.toString('utf8'));
  } catch {
    throw new VoxelApiError('voxel-json-malformed', 'Voxel API response was not valid JSON');
  }

  return parsed;
}

export function createVoxelHttpsTransport({
  requestImpl = httpsRequest,
  lookupImpl,
  timeoutMs = 30_000,
} = {}) {
  if (typeof requestImpl !== 'function') throw new TypeError('requestImpl must be a function');
  if (!Number.isInteger(timeoutMs) || timeoutMs < 1) throw new TypeError('timeoutMs must be a positive integer');

  return async ({ url, method, headers, body, maxBytes }) => {
    const parsed = validateVoxelApiUrl(url);
    const allowedAddresses = lookupImpl ? await resolvePublicAddresses(parsed.hostname, lookupImpl) : null;
    return new Promise((resolve, reject) => {
      const lookup = allowedAddresses
        ? (queriedHostname, options, callback) => {
            if (queriedHostname !== parsed.hostname) {
              callback(new VoxelApiError('voxel-dns-rebind', 'Voxel transport attempted to resolve an unvalidated hostname'));
              return;
            }
            if (options?.all) callback(null, allowedAddresses);
            else callback(null, allowedAddresses[0].address, allowedAddresses[0].family);
          }
        : undefined;

      const request = requestImpl({
        protocol: 'https:',
        hostname: parsed.hostname,
        port: 443,
        method,
        path: `${parsed.pathname}${parsed.search}`,
        servername: parsed.hostname,
        lookup,
        headers: {
          ...headers,
          'content-length': Buffer.byteLength(body ?? ''),
        },
      }, (response) => {
        const chunks = [];
        let length = 0;
        response.on('data', (chunk) => {
          length += chunk.length;
          if (length > maxBytes) {
            response.destroy(new VoxelApiError('voxel-response-oversized', 'Voxel API body exceeds byte limit'));
            return;
          }
          chunks.push(chunk);
        });
        response.once('error', reject);
        response.once('end', () => {
          resolve({ statusCode: response.statusCode, headers: response.headers, body: Buffer.concat(chunks) });
        });
      });
      request.setTimeout(timeoutMs, () => request.destroy(new VoxelApiError('voxel-timeout', 'Voxel API request timed out')));
      request.once('error', reject);
      request.end(body ?? '');
    });
  };
}

export function normalizeVoxelUpdates(apiResponse, { resourceId, minecraftVersion } = {}) {
  const response = apiResponse?.response;
  if (!response || typeof response !== 'object') {
    throw new VoxelApiError('voxel-response-malformed', 'Voxel updates response missing response object');
  }
  if (response.success !== true && response.success !== 'true') {
    throw new VoxelApiError('voxel-auth-required', 'Voxel resource updates endpoint did not return a successful public update list', {
      boundary: 'auth_required',
    });
  }
  const actualResourceId = normalizeResourceId(response.resource?.id ?? resourceId);
  if (resourceId !== undefined && actualResourceId !== normalizeResourceId(resourceId)) {
    throw new VoxelApiError('voxel-resource-id-mismatch', 'Voxel updates response resource id did not match request');
  }
  if (!Array.isArray(response.updates)) {
    throw new VoxelApiError('voxel-response-malformed', 'Voxel updates response missing updates list');
  }

  const updates = response.updates.map((update) => normalizeUpdate(update, minecraftVersion));
  const canonical = JSON.stringify({
    resource_id: actualResourceId,
    updates: updates.map((update) => ({
      id: update.id,
      version: update.version,
      title: update.title,
      description: update.description,
      time: update.time,
      url: update.url,
      downloadReady: update.download_ready,
      snapshot: update.snapshot,
      beta: update.beta,
    })),
  });

  return {
    resource_id: actualResourceId,
    result_count: numberOrNull(response.result_count),
    total: numberOrNull(response.total),
    more: boolOrNull(response.more),
    next_start: numberOrNull(response.next_start),
    remaining: numberOrNull(response.remaining),
    updates,
    content_sha256: createHash('sha256').update(canonical).digest('hex'),
  };
}

export function normalizeDownloadUrlResponse(apiResponse, { resourceId = NEXO_RESOURCE_ID } = {}) {
  const response = apiResponse?.response;
  if (!response || typeof response !== 'object') {
    throw new VoxelApiError('voxel-response-malformed', 'Voxel download URL response missing response object');
  }
  if (response.success !== true && response.success !== 'true') {
    throw new VoxelApiError('voxel-download-not-authorized', 'Voxel download URL request was not authorized', {
      boundary: 'auth_required',
    });
  }
  const result = response.result;
  if (!result || typeof result !== 'object' || typeof result.url !== 'string' || result.url.length === 0) {
    throw new VoxelApiError('voxel-response-malformed', 'Voxel download URL response missing URL');
  }
  const url = validateVoxelDownloadUrl(result.url);
  return {
    resource_id: normalizeResourceId(resourceId),
    url: url.href,
    version: typeof result.version === 'string' ? result.version : null,
    expires: numberOrNull(result.expires),
    allow_redirects: 0,
    boundary: 'download_url_qualified_only',
  };
}

export function redactVoxelSecrets(value) {
  if (typeof value === 'string') {
    return looksLikeAuthToken(value) ? '[REDACTED]' : redactTokenQueryParam(value);
  }
  if (Array.isArray(value)) return value.map((item) => redactVoxelSecrets(item));
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [
      key,
      /token/i.test(key) ? '[REDACTED]' : redactVoxelSecrets(item),
    ]));
  }
  return value;
}

function looksLikeAuthToken(value) {
  return value.startsWith('eyJ') && TOKEN.test(value) && value.length >= 24;
}

function redactTokenQueryParam(value) {
  try {
    const url = new URL(value);
    if (url.searchParams.has('token')) url.searchParams.set('token', '[REDACTED]');
    return url.href;
  } catch {
    return value.replace(/([?&]token=)[^&\s]+/gi, '$1[REDACTED]');
  }
}

export function generateVoxelState() {
  return randomBytes(24).toString('base64url');
}

export function normalizeResourceId(value) {
  const text = String(value ?? '');
  if (!RESOURCE_ID.test(text)) {
    throw new VoxelApiError('voxel-resource-id-invalid', 'Voxel resource id must be a positive integer');
  }
  const parsed = Number(text);
  if (!Number.isSafeInteger(parsed) || parsed < 1) {
    throw new VoxelApiError('voxel-resource-id-invalid', 'Voxel resource id must be a positive integer');
  }
  return parsed;
}

export function normalizeToken(value) {
  if (typeof value !== 'string' || !TOKEN.test(value)) {
    throw new VoxelApiError('voxel-token-invalid', 'Voxel auth token is missing or invalid');
  }
  return value;
}

export function validateVoxelDownloadUrl(value) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new VoxelApiError('voxel-download-url-malformed', 'Voxel download URL is malformed');
  }
  if (url.protocol !== 'https:' || url.hostname !== 'voxel.shop' || !url.pathname.startsWith('/download/')) {
    throw new VoxelApiError('voxel-download-url-forbidden', 'Voxel download URL is outside the pinned voxel.shop download path');
  }
  return url;
}

function normalizeApiOrigin(value) {
  const url = new URL(value);
  if (url.protocol !== 'https:' || url.hostname !== VOXEL_API_ENDPOINT.host) {
    throw new VoxelApiError('voxel-api-origin-forbidden', 'Voxel API origin must be https://api.voxel.shop');
  }
  return `${url.protocol}//${url.hostname}`;
}

function validateVoxelApiUrl(value) {
  const url = new URL(value);
  if (url.protocol !== 'https:' || url.hostname !== VOXEL_API_ENDPOINT.host || !url.pathname.startsWith(VOXEL_API_ENDPOINT.path_prefix)) {
    throw new VoxelApiError('voxel-api-url-forbidden', 'Voxel API URL must stay under https://api.voxel.shop/v1/');
  }
  return url;
}

function normalizeAuthorizeParams(params) {
  const service = params?.service;
  const returnUrl = params?.return_url;
  const state = params?.state;
  if (typeof service !== 'string' || service !== '127.0.0.1') {
    throw new VoxelApiError('voxel-authorize-service-invalid', 'Voxel authorize service must be 127.0.0.1');
  }
  let url;
  try {
    url = new URL(returnUrl);
  } catch {
    throw new VoxelApiError('voxel-authorize-return-url-invalid', 'Voxel authorize return_url is invalid');
  }
  if (url.protocol !== 'http:' || url.hostname !== service) {
    throw new VoxelApiError('voxel-authorize-return-url-invalid', 'Voxel authorize return_url must use localhost 127.0.0.1');
  }
  if (typeof state !== 'string' || !SAFE_STATE.test(state)) {
    throw new VoxelApiError('voxel-authorize-state-invalid', 'Voxel authorize state must be a secure URL-safe value');
  }
  return { service, return_url: url.href, return_token: '1', state };
}

function normalizeUpdate(update, minecraftVersion) {
  if (!update || typeof update !== 'object') {
    throw new VoxelApiError('voxel-response-malformed', 'Voxel update entry is malformed');
  }
  const channel = inferChannel(update);
  const gameVersions = explicitMinecraftVersions(update);
  const compatibility = classifyCompatibilityEvidence({
    minecraftVersion,
    loaders: gameVersions.length > 0 ? ['paper'] : [],
    gameVersions,
    version: update.version,
    title: update.title,
    description: update.description,
    datePublished: update.time === undefined || update.time === null ? null : new Date(Number(update.time) * 1000).toISOString(),
    source: 'voxel-getResourceUpdates',
  });
  return {
    id: String(update.id ?? ''),
    version: String(update.version ?? ''),
    version_type: channel === 'dev' ? 'alpha' : channel === 'snapshot' ? 'beta' : 'release',
    channel,
    title: typeof update.title === 'string' ? update.title : null,
    description: typeof update.description === 'string' ? update.description : null,
    date_published: update.time === undefined || update.time === null ? null : new Date(Number(update.time) * 1000).toISOString(),
    time: numberOrNull(update.time),
    url: typeof update.url === 'string' ? update.url : null,
    download_ready: boolOrNull(update.downloadReady),
    snapshot: boolOrNull(update.snapshot),
    beta: boolOrNull(update.beta),
    compatible: compatibility.compatible,
    compatibility,
    game_versions: gameVersions,
    loaders: gameVersions.length > 0 ? ['paper'] : [],
  };
}

function inferChannel(update) {
  if (truthy(update.snapshot)) return 'snapshot';
  if (truthy(update.beta)) return 'dev';
  const text = `${update.version ?? ''} ${update.title ?? ''}`.toLowerCase();
  if (/\bsnapshot\b/.test(text)) return 'snapshot';
  if (/\b(alpha|beta|dev|development|rc)\b/.test(text)) return 'dev';
  return 'stable';
}

function explicitMinecraftVersions(update) {
  const fields = [update.supportedMinecraftVersions, update.minecraftVersions, update.game_versions, update.description, update.title];
  const versions = new Set();
  for (const field of fields) {
    const values = Array.isArray(field) ? field : [field];
    for (const value of values) {
      if (typeof value !== 'string') continue;
      for (const match of value.matchAll(/\b(?:paper|minecraft|mc)?\s*(?:version|versions)?\s*v?(1\.\d+(?:\.\d+)?)\b/gi)) {
        versions.add(match[1]);
      }
    }
  }
  return [...versions].slice(0, 20);
}

function normalizeNonNegativeInt(value, label) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 0) {
    throw new VoxelApiError(`voxel-${label}-invalid`, `Voxel ${label} must be a non-negative integer`);
  }
  return parsed;
}

function normalizeLimit(value) {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 5 || parsed > 50) {
    throw new VoxelApiError('voxel-limit-invalid', 'Voxel update limit must be between 5 and 50');
  }
  return parsed;
}

function numberOrNull(value) {
  if (value === undefined || value === null || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function boolOrNull(value) {
  if (value === undefined || value === null || value === '') return null;
  if (value === true || value === 'true' || value === 1 || value === '1') return true;
  if (value === false || value === 'false' || value === 0 || value === '0') return false;
  return null;
}

function truthy(value) {
  return value === true || value === 'true' || value === 1 || value === '1';
}

async function resolvePublicAddresses(hostname, lookupImpl) {
  let answers;
  try {
    answers = await lookupImpl(hostname, { all: true, verbatim: true });
  } catch {
    throw new VoxelApiError('voxel-dns-failed', `DNS resolution failed for ${hostname}`);
  }
  if (!Array.isArray(answers) || answers.length === 0) {
    throw new VoxelApiError('voxel-dns-failed', `DNS returned no addresses for ${hostname}`);
  }
  const normalized = answers.map((answer) => ({ address: answer.address, family: answer.family ?? isIP(answer.address) }));
  if (normalized.some((answer) => !isPublicAddress(answer.address))) {
    throw new VoxelApiError('voxel-address-forbidden', 'Voxel API DNS resolved to a private, local, or reserved address');
  }
  return normalized;
}

function isPublicAddress(address) {
  const family = isIP(address);
  if (family === 4) {
    const [a, b, c] = address.split('.').map(Number);
    return !(a === 0 || a === 10 || a === 127 || a >= 224
      || (a === 100 && b >= 64 && b <= 127)
      || (a === 169 && b === 254)
      || (a === 172 && b >= 16 && b <= 31)
      || (a === 192 && b === 168)
      || (a === 192 && b === 0)
      || (a === 192 && b === 0 && c === 2)
      || (a === 198 && b >= 18 && b <= 19)
      || (a === 198 && b === 51 && c === 100)
      || (a === 203 && b === 0 && c === 113));
  }
  if (family === 6) {
    const lower = address.toLowerCase();
    return !(lower === '::' || lower === '::1' || lower.startsWith('fc') || lower.startsWith('fd') || lower.startsWith('fe80:'));
  }
  return false;
}
