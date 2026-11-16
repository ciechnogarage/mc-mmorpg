import { createHash } from 'node:crypto';
import { classifyCompatibilityEvidence } from '../compatibility/evidence.mjs';
import { secureFetch } from '../downloads/network.mjs';
import {
  NEXO_RESOURCE_ID,
  VoxelApiError,
  createVoxelApiClient,
  createVoxelHttpsTransport,
  normalizeVoxelUpdates,
} from '../voxel/index.mjs';

const ADAPTERS = Object.freeze({
  'mythiccraft-resource': Object.freeze({
    channel: 'stable',
    risk: 'mythiccraft-account-resource-download-not-authorized',
  }),
  'phoenixdevt-devbuilds': Object.freeze({
    channel: 'dev',
    risk: 'phoenixdevt-devbuild-account-auth-required',
  }),
  'voxel-shop-updates': Object.freeze({
    channel: 'stable',
    risk: 'voxel-shop-token-auth-required',
  }),
});

const MAX_VENDOR_PAGE_BYTES = 2 * 1024 * 1024;
const VERSION_PATTERN = /\b(?:version|build|release|latest)\s*[:#-]?\s*v?(?<version>\d+(?:\.\d+){1,3}(?:[-+][A-Za-z0-9._-]+)?)\b/gi;
const PAPER_PATTERN = /\b(?:paper|minecraft|mc)\s*(?:version|versions|mc)?\s*[:#-]?\s*v?(?<version>1\.\d+(?:\.\d+)?)\b/gi;

export function supportsVendorAdapter(adapter) {
  return Object.hasOwn(ADAPTERS, adapter);
}

export async function discoverVendorMetadata({
  entry,
  registration,
  minecraftVersion,
  channel,
  lookupImpl,
  fetchImpl,
  voxelClient,
}) {
  const adapter = ADAPTERS[registration?.adapter];
  if (!adapter) return null;

  const source = sourceFromRegistration(registration);
  const rejections = [];
  let page = null;

  if (registration.adapter === 'voxel-shop-updates' && registration.resource_id !== undefined) {
    page = await fetchVoxelUpdatePage({
      registration,
      minecraftVersion,
      lookupImpl,
      voxelClient,
      rejections,
    });
  } else {
    try {
    const url = new URL(registration.official_url);
    const bytes = await secureFetch({
      url: url.href,
      endpoint: {
        host: url.hostname,
        project_path: url.pathname,
        path_prefix: url.pathname,
        max_bytes: MAX_VENDOR_PAGE_BYTES,
      },
      redirects: { max: 2, same_host_only: true },
      lookupImpl,
      fetchImpl,
    });
    const text = bytes.toString('utf8');
    page = {
      url: url.href,
      content_sha256: createHash('sha256').update(bytes).digest('hex'),
      title: extractTitle(text),
      description: extractDescription(text),
      versions: extractVersions(text, adapter.channel),
      paper_versions: extractPaperVersions(text),
      fetched: true,
    };
  } catch (error) {
    rejections.push({
      stage: 'vendor-metadata',
      project_id: null,
      reason: error?.code ?? 'vendor-metadata-fetch-failed',
    });
  }
  }

  const availableVersions = buildAvailableVersions({
    page,
    fallbackVersion: entry.manifest.current_version,
    channel: adapter.channel,
    minecraftVersion,
  });
  const compatibility = inferCompatibilityEvidence({ page, availableVersions, minecraftVersion });

  return {
    kind: 'vendor-metadata',
    source,
    license: { status: entry.manifest.license_status, provenance: 'plugin-manifest' },
    compatibility,
    available_versions: availableVersions,
    vendor_metadata: {
      adapter: registration.adapter,
      official_url: registration.official_url,
      metadata_url: page?.url ?? registration.official_url,
      fetched: page?.fetched ?? false,
      content_sha256: page?.content_sha256 ?? null,
      title: page?.title ?? null,
      description: page?.description ?? null,
      docs_urls: [registration.official_url],
      changelog_urls: page?.changelog_urls?.length ? page.changelog_urls : [registration.official_url],
      resource_id: registration.resource_id ?? null,
      api_url: page?.api_url ?? null,
      auth_boundary: authBoundary(registration.adapter),
      download_authorized: false,
      channel_policy: 'No fallback between stable, snapshot, and dev channels.',
      risk_notes: [
        adapter.risk,
        'metadata-is-informational-only',
        'jar-selection-still-requires-pinned-source-plugin-yml-sha256-approved-plan',
      ],
    },
    reason: registration.reason,
    rejections,
  };
}

async function fetchVoxelUpdatePage({ registration, minecraftVersion, lookupImpl, voxelClient, rejections }) {
  if (Number(registration.resource_id) !== NEXO_RESOURCE_ID) {
    rejections.push({
      stage: 'vendor-metadata',
      project_id: String(registration.resource_id),
      reason: 'voxel-resource-id-forbidden',
    });
    return null;
  }

  const client = voxelClient ?? createVoxelApiClient({
    transport: createVoxelHttpsTransport({ lookupImpl }),
  });

  try {
    const raw = await client.getResourceUpdates({ resource_id: registration.resource_id, limit: 50 });
    const updates = normalizeVoxelUpdates(raw, { resourceId: registration.resource_id, minecraftVersion });
    return {
      url: 'https://api.voxel.shop/v1/getResourceUpdates',
      api_url: 'https://api.voxel.shop/v1/getResourceUpdates',
      content_sha256: updates.content_sha256,
      title: null,
      description: null,
      versions: updates.updates.map((update) => ({ ...update })),
      paper_versions: [...new Set(updates.updates.flatMap((update) => update.game_versions))],
      fetched: true,
      changelog_urls: updates.updates.map((update) => update.url).filter((url) => typeof url === 'string' && url.length > 0).slice(0, 50),
    };
  } catch (error) {
    rejections.push({
      stage: 'vendor-metadata',
      project_id: String(registration.resource_id),
      reason: error?.boundary === 'auth_required' ? 'auth_required' : error?.code ?? 'voxel-updates-fetch-failed',
    });
    if (error instanceof VoxelApiError && error.boundary === 'auth_required') return null;
    return null;
  }
}

function sourceFromRegistration(registration) {
  return {
    registration: 'approved',
    type: registration.kind,
    plugin_id: registration.plugin_id,
    source_id: registration.source_id ?? null,
    adapter: registration.adapter,
    project: registration.project ?? null,
    project_id: registration.project_id ?? null,
    official_url: registration.official_url,
    metadata_url: registration.official_url,
  };
}

function extractTitle(text) {
  return firstMatch(text, /<title[^>]*>(?<value>[\s\S]*?)<\/title>/i)
    ?? firstMatch(text, /<h1[^>]*>(?<value>[\s\S]*?)<\/h1>/i);
}

function extractDescription(text) {
  return firstMatch(text, /<meta\s+[^>]*(?:name|property)=["'](?:description|og:description)["'][^>]*content=["'](?<value>[^"']+)["'][^>]*>/i)
    ?? firstMatch(text, /<p[^>]*>(?<value>[\s\S]{20,500}?)<\/p>/i);
}

function firstMatch(text, pattern) {
  const match = pattern.exec(text);
  if (!match?.groups?.value) return null;
  return normalizeText(match.groups.value).slice(0, 500) || null;
}

function extractVersions(text, channel) {
  const versions = new Set();
  for (const match of text.matchAll(VERSION_PATTERN)) {
    versions.add(match.groups.version);
  }
  return [...versions].slice(0, 40).map((version) => ({ version, channel }));
}

function extractPaperVersions(text) {
  const versions = new Set();
  for (const match of text.matchAll(PAPER_PATTERN)) {
    versions.add(match.groups.version);
  }
  return [...versions].slice(0, 20);
}

function buildAvailableVersions({ page, fallbackVersion, channel, minecraftVersion }) {
  const versions = page?.versions.length ? page.versions : [{ version: fallbackVersion, channel }];
  return versions.map(({ version, channel: releaseChannel, id, version_type, date_published, compatible, compatibility, game_versions, loaders, title, description }) => {
    const effectiveGameVersions = game_versions ?? page?.paper_versions ?? [];
    const effectiveLoaders = loaders ?? (effectiveGameVersions.length ? ['paper'] : []);
    const evidence = compatibility ?? classifyCompatibilityEvidence({
      minecraftVersion,
      loaders: effectiveLoaders,
      gameVersions: effectiveGameVersions,
      version,
      title,
      description,
      datePublished: date_published,
      source: page?.api_url ? 'voxel-getResourceUpdates' : 'vendor-page-metadata',
    });
    return {
      id: id || `vendor-${version}`,
      version,
      version_type: version_type ?? (releaseChannel === 'dev' ? 'alpha' : releaseChannel === 'snapshot' ? 'beta' : 'release'),
      channel: releaseChannel,
      date_published: date_published ?? null,
      compatible: compatible ?? evidence.compatible,
      compatibility: evidence,
      game_versions: effectiveGameVersions,
      loaders: effectiveLoaders,
    };
  });
}

function inferCompatibilityEvidence({ page, availableVersions, minecraftVersion }) {
  const compatible = availableVersions.find((version) => version.compatibility?.compatible === true);
  if (compatible) return compatible.compatibility;
  const negative = availableVersions.find((version) => version.compatibility?.class === 'negative-listed');
  if (negative) return negative.compatibility;
  if (!page?.paper_versions.length) {
    return classifyCompatibilityEvidence({
      minecraftVersion,
      loaders: [],
      gameVersions: [],
      source: page?.api_url ? 'voxel-getResourceUpdates' : 'vendor-page-metadata',
    });
  }
  return classifyCompatibilityEvidence({
    minecraftVersion,
    loaders: ['paper'],
    gameVersions: page.paper_versions,
    source: page?.api_url ? 'voxel-getResourceUpdates' : 'vendor-page-metadata',
  });
}

function authBoundary(adapter) {
  if (adapter === 'voxel-shop-updates') {
    return 'Official token flow is required before download qualification; token/auth plumbing is outside this metadata-only adapter.';
  }
  return 'Authenticated vendor account/session is required before download qualification; this adapter records metadata only.';
}

function normalizeText(value) {
  return value
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&#39;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/\s+/g, ' ')
    .trim();
}
