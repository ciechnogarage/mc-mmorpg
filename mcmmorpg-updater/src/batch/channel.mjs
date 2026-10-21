import { classifyCompatibilityEvidence } from '../compatibility/evidence.mjs';
import { batchFail } from './errors.mjs';

export const RELEASE_CHANNELS = Object.freeze(['stable', 'snapshot']);

export function normalizeReleaseChannel(value = 'stable') {
  if (!RELEASE_CHANNELS.includes(value)) {
    batchFail('batch-channel-invalid', 'release channel must be stable or snapshot');
  }
  return value;
}

export function classifyModrinthVersionChannel(version) {
  const type = String(version?.version_type ?? '').toLowerCase();
  if (type === 'release') return 'stable';
  if (type === 'beta' || type === 'alpha') return 'snapshot';

  const label = `${version?.version_number ?? ''} ${version?.name ?? ''}`.toLowerCase();
  if (/\b(snapshot|dev|nightly|alpha|beta|rc)\b/.test(label)) return 'snapshot';
  return 'unknown';
}

export function versionMatchesChannel(version, channel) {
  const normalized = normalizeReleaseChannel(channel);
  return classifyModrinthVersionChannel(version) === normalized;
}

export function summarizeReleaseVersions(versions, { minecraftVersion, loaders = [], channel = 'stable' } = {}) {
  const normalized = normalizeReleaseChannel(channel);
  if (!Array.isArray(versions)) {
    batchFail('batch-channel-versions-invalid', 'versions must be an array');
  }

  return versions.map((version) => {
    const releaseChannel = classifyModrinthVersionChannel(version);
    const compatibility = classifyCompatibilityEvidence({
      minecraftVersion,
      loaders: version.loaders,
      gameVersions: version.game_versions,
      version: version.version_number,
      title: version.name,
      datePublished: version.date_published,
      source: 'modrinth-project-version-api',
    });
    const compatible = version?.status === 'listed'
      && Array.isArray(version.loaders)
      && version.loaders.some((loader) => loaders.includes(loader))
      && compatibility.compatible === true;

    return {
      id: version.id,
      version: version.version_number,
      channel: releaseChannel,
      selected_channel: normalized,
      status: version.status,
      date_published: version.date_published,
      compatible,
      compatibility,
      game_versions: [...(version.game_versions ?? [])],
      loaders: [...(version.loaders ?? [])],
    };
  }).sort((left, right) => Date.parse(right.date_published) - Date.parse(left.date_published));
}
