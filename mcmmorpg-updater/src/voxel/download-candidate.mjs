import { createHash } from 'node:crypto';
import { constants as fsConstants } from 'node:fs';
import { lstat, mkdir, open, realpath, unlink } from 'node:fs/promises';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { inspectPluginJar } from '../downloads/jar.mjs';
import { secureFetch } from '../downloads/network.mjs';
import { fail } from '../downloads/errors.mjs';
import { NEXO_RESOURCE_ID, normalizeDownloadUrlResponse, normalizeResourceId } from './api-client.mjs';

const NEXO_PLUGIN_ID = 'nexo';
const NEXO_PLUGIN_NAME = 'Nexo';
const VOXEL_DOWNLOAD_ENDPOINT = Object.freeze({
  host: 'voxel.shop',
  path_prefix: '/download/',
  max_bytes: 128 * 1024 * 1024,
});
const VOXEL_REDIRECTS = Object.freeze({ max: 0, same_host_only: true });

export async function persistVoxelNexoDownloadCandidate({
  client,
  resourceId = NEXO_RESOURCE_ID,
  token,
  version,
  projectRoot,
  downloadsRoot = join(projectRoot, 'downloads'),
  lookupImpl,
  fetchImpl,
}) {
  const normalizedResourceId = normalizeResourceId(resourceId);
  if (normalizedResourceId !== NEXO_RESOURCE_ID) {
    fail('voxel-resource-id-forbidden', 'Voxel download candidate persistence is limited to Nexo resource_id 6901');
  }
  if (!client || typeof client.getDownloadURL !== 'function') {
    throw new TypeError('Voxel client with getDownloadURL required');
  }
  if (typeof token !== 'string' || token.length === 0) {
    fail('voxel-token-required', 'verified Voxel token required before download candidate persistence');
  }

  const raw = await client.getDownloadURL({ resource_id: normalizedResourceId, token });
  const qualified = normalizeDownloadUrlResponse(raw, { resourceId: normalizedResourceId });
  if (version !== undefined && qualified.version !== null && String(version) !== String(qualified.version)) {
    fail('voxel-download-version-mismatch', 'Voxel download URL version did not match requested version');
  }

  const bytes = await secureFetch({
    url: qualified.url,
    endpoint: VOXEL_DOWNLOAD_ENDPOINT,
    redirects: VOXEL_REDIRECTS,
    lookupImpl,
    fetchImpl,
  });
  const sha256 = createHash('sha256').update(bytes).digest('hex');
  const jar = inspectPluginJar(bytes, { expectedPluginName: NEXO_PLUGIN_NAME });
  const relativeCandidatePath = await persistImmutableCandidate({
    downloadsRoot,
    pluginId: NEXO_PLUGIN_ID,
    sha256,
    bytes,
  });

  return {
    ok: true,
    command: 'download-candidate',
    resource_id: normalizedResourceId,
    plugin_id: NEXO_PLUGIN_ID,
    version: qualified.version ?? version ?? jar.plugin_version,
    source: {
      adapter: 'voxel-shop-getDownloadURL',
      resource_id: normalizedResourceId,
      url: redactedDownloadUrl(qualified.url),
      url_sha256: createHash('sha256').update(qualified.url).digest('hex'),
      boundary: qualified.boundary,
      expires: qualified.expires,
    },
    path: relativeCandidatePath,
    sha256,
    size_bytes: bytes.length,
    plugin_yml: {
      name: jar.plugin_name,
      version: jar.plugin_version,
    },
    provenance: {
      immutable_content_addressed: true,
      manual_approval_required_for_stack_selection: true,
      writes_server_paths: false,
      deploy_plan_created: false,
    },
  };
}

function redactedDownloadUrl(value) {
  const url = new URL(value);
  url.search = '';
  return url.href;
}

async function persistImmutableCandidate({ downloadsRoot, pluginId, sha256, bytes }) {
  await mkdir(downloadsRoot, { recursive: true, mode: 0o700 });
  const canonicalDownloads = await realpath(downloadsRoot);
  if (canonicalDownloads !== downloadsRoot) {
    fail('candidate-download-root-forbidden', 'downloads root resolved through symlink');
  }
  const pluginDirectory = join(downloadsRoot, pluginId);
  await mkdir(pluginDirectory, { recursive: true, mode: 0o700 });
  const directoryStat = await lstat(pluginDirectory);
  if (!directoryStat.isDirectory() || directoryStat.isSymbolicLink() || await realpath(pluginDirectory) !== pluginDirectory) {
    fail('candidate-download-root-forbidden', 'candidate plugin directory unsafe');
  }
  const path = join(pluginDirectory, `${sha256}.jar`);
  let handle;
  try {
    handle = await open(path, fsConstants.O_WRONLY | fsConstants.O_CREAT | fsConstants.O_EXCL | fsConstants.O_NOFOLLOW, 0o400);
    await handle.writeFile(bytes);
    await handle.sync();
    await handle.chmod(0o444);
    await handle.close();
    handle = null;
  } catch (error) {
    await handle?.close().catch(() => {});
    if (error.code === 'EEXIST') {
      let existingHandle;
      try {
        existingHandle = await open(path, fsConstants.O_RDONLY | fsConstants.O_NOFOLLOW);
        const metadata = await existingHandle.stat();
        if (!metadata.isFile() || metadata.nlink !== 1) {
          fail('candidate-existing-file-unsafe', 'existing candidate must be a single-link file');
        }
        const existing = await existingHandle.readFile();
        if (createHash('sha256').update(existing).digest('hex') !== sha256) {
          fail('candidate-existing-file-mismatch', 'existing content-addressed candidate hash mismatch');
        }
        await existingHandle.close();
        existingHandle = null;
      } finally {
        await existingHandle?.close().catch(() => {});
      }
    } else {
      await unlink(path).catch(() => {});
      throw error;
    }
  }
  if (!isWithin(downloadsRoot, path)) {
    fail('candidate-path-escape', 'candidate path escaped downloads root');
  }
  return relative(resolve(downloadsRoot, '..'), path).split(sep).join('/');
}

function isWithin(root, candidate) {
  const rel = relative(resolve(root), resolve(candidate));
  return rel === '' || (!rel.startsWith(`..${sep}`) && rel !== '..' && !isAbsolute(rel));
}
