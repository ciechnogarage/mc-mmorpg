import { isAbsolute, normalize, relative, resolve, sep } from 'node:path';

const PLAYER_DATA_SEGMENTS = new Set(['playerdata', 'advancements', 'stats']);
const PLAYER_DATA_FILES = new Set([
  'ops.json',
  'usercache.json',
  'whitelist.json',
  'banned-players.json',
  'banned-ips.json',
]);
const SECRET_FILE_NAMES = new Set([
  '.rcon-cli.yaml',
  '.rcon-cli.yml',
  'secrets.json',
  'secrets.yml',
  'secrets.yaml',
  'credentials.json',
  'credentials.yml',
  'credentials.yaml',
]);

export function assertCanonicalAbsolutePath(value, fail) {
  if (typeof value !== 'string' || !isAbsolute(value)) {
    fail('path-outside-root', 'path must be absolute');
  }
  if (hasTraversalSegment(value) || normalize(value) !== value) {
    fail('path-noncanonical', `path is not canonical: ${value}`);
  }
  return value;
}

export function assertCanonicalRelativePath(value, fail) {
  if (typeof value !== 'string' || value.length === 0 || isAbsolute(value)) {
    fail('path-outside-root', 'path must be a non-empty relative path');
  }
  if (value.includes('\\') || hasTraversalSegment(value) || normalize(value) !== value) {
    fail('path-noncanonical', `path is not canonical: ${value}`);
  }
  return value;
}

export function assertWithinRoot(root, candidate, fail, { allowRoot = false } = {}) {
  const rel = relative(root, candidate);
  if ((!allowRoot && rel === '') || rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) {
    fail('path-outside-root', `path escapes allowed root ${root}: ${candidate}`);
  }
  return candidate;
}

export function resolveRegisteredPath(root, registeredPath, fail) {
  assertCanonicalRelativePath(registeredPath, fail);
  const candidate = resolve(root, registeredPath);
  return assertWithinRoot(root, candidate, fail);
}

export function classifyExcludedPath(registeredPath) {
  const normalized = registeredPath.replaceAll('\\', '/');
  const segments = normalized.split('/').filter(Boolean);
  const lower = segments.map((part) => part.toLowerCase());
  const basename = lower.at(-1) ?? '';

  if (basename === '.env' || basename.startsWith('.env.') || SECRET_FILE_NAMES.has(basename)) {
    return 'secret file';
  }
  if (/^(secret|credentials?)([-_.]|$)/i.test(basename) || /\.(pem|key|p12|pfx)$/i.test(basename)) {
    return 'secret file';
  }
  if (lower.some((part) => PLAYER_DATA_SEGMENTS.has(part)) || PLAYER_DATA_FILES.has(basename)) {
    return 'player data';
  }
  if (/^world(?:$|_)/i.test(lower[0] ?? '')) {
    return 'world data';
  }
  return null;
}

function hasTraversalSegment(value) {
  return value.split(/[\\/]/).some((part) => part === '.' || part === '..');
}
