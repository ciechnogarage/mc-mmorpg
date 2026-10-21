import { constants as fsConstants } from 'node:fs';
import { chmod, lstat, mkdir, open, readFile, unlink } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { normalizeToken, VoxelApiError } from './api-client.mjs';

export function defaultVoxelTokenPath({ env = process.env, home = env.HOME } = {}) {
  const stateHome = env.XDG_STATE_HOME && env.XDG_STATE_HOME.startsWith('/')
    ? env.XDG_STATE_HOME
    : home
      ? join(home, '.local', 'state')
      : null;
  if (!stateHome) {
    throw new VoxelApiError('voxel-token-path-unavailable', 'Set XDG_STATE_HOME or HOME to locate the Voxel token file');
  }
  return join(stateHome, 'mcmmorpg-updater', 'voxel-token.json');
}

export async function readVoxelToken({ tokenPath = defaultVoxelTokenPath(), lstatImpl = lstat, readFileImpl = readFile } = {}) {
  let metadata;
  try {
    metadata = await lstatImpl(tokenPath);
  } catch (error) {
    if (error?.code === 'ENOENT') {
      throw new VoxelApiError('voxel-token-missing', `Voxel token missing; run: npm run voxel -- authorize --token-file ${tokenPath}`, {
        boundary: 'auth_required',
      });
    }
    throw error;
  }
  if (!metadata.isFile() || metadata.isSymbolicLink()) {
    throw new VoxelApiError('voxel-token-file-unsafe', 'Voxel token path must be a regular non-symlink file');
  }
  if ((metadata.mode & 0o077) !== 0) {
    throw new VoxelApiError('voxel-token-file-permissions', 'Voxel token file must be mode 0600');
  }
  let parsed;
  try {
    parsed = JSON.parse(await readFileImpl(tokenPath, 'utf8'));
  } catch {
    throw new VoxelApiError('voxel-token-file-malformed', 'Voxel token file is not valid JSON');
  }
  return normalizeToken(parsed.token);
}

export async function writeVoxelToken(token, {
  tokenPath = defaultVoxelTokenPath(),
  mkdirImpl = mkdir,
  openImpl = open,
  chmodImpl = chmod,
} = {}) {
  const normalized = normalizeToken(token);
  await mkdirImpl(dirname(tokenPath), { recursive: true, mode: 0o700 });
  const handle = await openImpl(tokenPath, fsConstants.O_WRONLY | fsConstants.O_CREAT | fsConstants.O_TRUNC | fsConstants.O_NOFOLLOW, 0o600);
  try {
    await handle.writeFile(`${JSON.stringify({ schema_version: 1, token: normalized, created_at: new Date().toISOString() }, null, 2)}\n`, 'utf8');
    await handle.sync();
  } finally {
    await handle.close();
  }
  await chmodImpl(tokenPath, 0o600);
  return tokenPath;
}

export async function deleteVoxelToken({ tokenPath = defaultVoxelTokenPath(), unlinkImpl = unlink } = {}) {
  try {
    await unlinkImpl(tokenPath);
    return true;
  } catch (error) {
    if (error?.code === 'ENOENT') return false;
    throw error;
  }
}
