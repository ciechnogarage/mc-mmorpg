import { batchFail } from './errors.mjs';
import { normalizeReleaseChannel } from './channel.mjs';
import { validateModrinthSearchPolicy } from './search-policy.mjs';

export const BATCH_STATUSES = Object.freeze([
  'qualified',
  'manual-hash-approval-required',
  'premium-or-login',
  'internal-build',
  'source-not-found',
  'incompatible',
]);

const ID = /^[a-z0-9][a-z0-9._-]{0,127}$/;
const TOKEN = /^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/;
const VENDOR_ADAPTERS = Object.freeze([
  'mythiccraft-resource',
  'phoenixdevt-devbuilds',
  'voxel-shop-updates',
]);

export function buildBatchReport({ inventory, registrations, outcomes = new Map(), generatedAt = new Date().toISOString(), channel = 'stable' }) {
  validateInventory(inventory);
  const normalizedChannel = normalizeReleaseChannel(channel);
  const sourceRegistry = validateBatchSourceRegistrations(registrations);
  if (!(outcomes instanceof Map)) batchFail('batch-report-input-invalid', 'batch outcomes must be a Map keyed by manifest name');
  const timestamp = new Date(generatedAt);
  if (!Number.isFinite(timestamp.getTime())) batchFail('batch-report-input-invalid', 'generatedAt is invalid');

  const plugins = inventory.plugins.map((entry) => classifyPlugin({
    entry,
    minecraftVersion: inventory.minecraft_version,
    registration: sourceRegistry.get(entry.manifest.name),
    outcome: outcomes.get(entry.manifest.name),
  }));
  const statusCounts = Object.fromEntries(BATCH_STATUSES.map((status) => [status, 0]));
  for (const plugin of plugins) statusCounts[plugin.status] += 1;
  const reportedInstallations = plugins.reduce((total, plugin) => total + plugin.installations.length, 0);

  const candidateCounts = {
    found: statusCounts.qualified + statusCounts['manual-hash-approval-required'],
    qualified: statusCounts.qualified,
    manual_hash_approval_required: statusCounts['manual-hash-approval-required'],
    incompatible: statusCounts.incompatible,
  };
  const adapterCoverage = buildAdapterCoverage(plugins);
  return {
    schema_version: 1,
    generated_at: timestamp.toISOString(),
    minecraft_version: inventory.minecraft_version,
    channel: normalizedChannel,
    policy: {
      stage: 'discovery-report',
      downloads_stack: false,
      creates_deploy_plan: false,
      channel_fallback: false,
    },
    provenance: {
      manifest: { ...inventory.manifest },
      inventory_scanned_at: inventory.scanned_at,
      target_inventory: inventory.targets.map((target) => ({ ...target })),
    },
    coverage: {
      manifest_plugins: inventory.manifest.plugin_count,
      reported_plugins: plugins.length,
      manifest_installations: inventory.installation_count,
      reported_installations: reportedInstallations,
      complete: plugins.length === inventory.manifest.plugin_count
        && reportedInstallations === inventory.installation_count,
    },
    summary: {
      status_counts: statusCounts,
      candidate_counts: candidateCounts,
      adapter_coverage: adapterCoverage,
      channel: normalizedChannel,
    },
    plugins,
  };
}

function buildAdapterCoverage(plugins) {
  const adapters = new Map();
  const missing_source_authority = [];
  let registered = 0;
  let default_search = 0;
  let internal = 0;

  for (const plugin of plugins) {
    const adapter = plugin.source?.adapter ?? null;
    if (adapter) adapters.set(adapter, (adapters.get(adapter) ?? 0) + 1);
    if (plugin.source?.registration === 'approved') registered += 1;
    else if (plugin.source?.registration === 'verified-search') default_search += 1;
    else if (plugin.source?.registration === 'manifest-internal') internal += 1;
    else missing_source_authority.push(plugin.name);
  }

  return {
    registered,
    default_search,
    internal,
    missing: missing_source_authority.length,
    missing_source_authority,
    adapters: Object.fromEntries([...adapters.entries()].sort(([left], [right]) => left.localeCompare(right))),
  };
}

export function validateBatchSourceRegistrations(config) {
  if (!config || typeof config !== 'object' || Array.isArray(config)
      || config.schema_version !== 1 || !Array.isArray(config.registrations)) {
    batchFail('batch-source-registry-malformed', 'batch source registry must be schema v1');
  }
  if (config.modrinth_search !== undefined) validateModrinthSearchPolicy(config.modrinth_search);
  const registrations = new Map();
  for (const registration of config.registrations) {
    if (!registration || typeof registration !== 'object' || Array.isArray(registration)
        || typeof registration.manifest_name !== 'string' || registration.manifest_name.length === 0
        || !ID.test(registration.plugin_id ?? '')) {
      batchFail('batch-source-registry-malformed', 'source registration has invalid manifest_name or plugin_id');
    }
    if (registrations.has(registration.manifest_name)) {
      batchFail('batch-source-registration-duplicate', `duplicate source registration: ${registration.manifest_name}`);
    }
    requireHttps(registration.official_url, 'official_url');
    if (registration.kind === 'premium-or-login' && !VENDOR_ADAPTERS.includes(registration.adapter)) {
      batchFail('batch-source-registry-malformed', 'premium-or-login registration requires supported adapter');
    }
    if (registration.kind === 'premium-or-login') {
      if (typeof registration.reason !== 'string' || registration.reason.length === 0) {
        batchFail('batch-source-registry-malformed', 'premium-or-login registration requires a reason');
      }
      if (registration.resource_id !== undefined && (!Number.isInteger(registration.resource_id) || registration.resource_id < 1)) {
        batchFail('batch-source-registry-malformed', 'premium-or-login resource_id must be a positive integer');
      }
    } else if (registration.kind === 'public-source') {
      if (!ID.test(registration.source_id ?? '') || registration.adapter !== 'modrinth-project-versions'
          || typeof registration.project !== 'string'
          || !/^[A-Za-z0-9._-]+\/[A-Za-z0-9._-]+$/.test(registration.project)
          || !TOKEN.test(registration.project_id ?? '')
          || !registration.compatibility || !Array.isArray(registration.compatibility.loaders)
          || registration.compatibility.loaders.length === 0
          || !Array.isArray(registration.compatibility.release_types)
          || registration.compatibility.release_types.length === 0
          || !registration.compatibility.loaders.every((value) => ID.test(value))
          || !registration.compatibility.release_types.every((value) => ['release', 'beta', 'alpha'].includes(value))) {
        batchFail('batch-source-registry-malformed', 'public source registration is incomplete or uses an unsupported adapter');
      }
    } else {
      batchFail('batch-source-kind-forbidden', `unsupported batch source kind: ${registration.kind}`);
    }
    registrations.set(registration.manifest_name, Object.freeze(structuredClone(registration)));
  }
  return registrations;
}

function classifyPlugin({ entry, minecraftVersion, registration, outcome }) {
  const manifest = entry.manifest;
  const base = {
    name: manifest.name,
    current_version: manifest.current_version,
    current_jar: manifest.jar,
    installations: entry.installations.map((installation) => ({ ...installation })),
    available_versions: Array.isArray(outcome?.available_versions) ? structuredClone(outcome.available_versions) : [],
    vendor_metadata: outcome?.vendor_metadata ? structuredClone(outcome.vendor_metadata) : null,
    search: outcome?.search ? structuredClone(outcome.search) : null,
    rejections: Array.isArray(outcome?.rejections) ? structuredClone(outcome.rejections) : [],
  };
  const manifestLicense = { status: manifest.license_status, provenance: 'plugin-manifest' };
  const unknownCompatibility = {
    minecraft_version: minecraftVersion,
    compatible: null,
    loaders: [],
    evidence: null,
  };

  if (manifest.license_status === 'internal') {
    return {
      ...base,
      status: 'internal-build',
      reason: 'Manifest marks this plugin as an internal build; only its local source/build pipeline is allowed.',
      source: { registration: 'manifest-internal', type: 'internal-build', source_path: manifest.source_path ?? null },
      license: manifestLicense,
      compatibility: unknownCompatibility,
      candidate: null,
    };
  }
  if (!registration && !outcome) {
    return {
      ...base,
      status: 'source-not-found',
      reason: 'No approved official source registration exists; vendor/source discovery was not guessed.',
      source: { registration: 'unresolved' },
      license: manifestLicense,
      compatibility: unknownCompatibility,
      candidate: null,
    };
  }
  const registeredSource = registration ? sourceFromRegistration(registration) : { registration: 'verified-search' };
  if (registration?.kind === 'premium-or-login') {
    return {
      ...base,
      status: 'premium-or-login',
      reason: outcome?.reason ?? registration.reason,
      source: outcome?.source ?? registeredSource,
      license: outcome?.license ?? manifestLicense,
      compatibility: outcome?.compatibility ?? unknownCompatibility,
      candidate: null,
    };
  }
  if (!outcome || outcome.kind === 'source-not-found') {
    return {
      ...base,
      status: 'source-not-found',
      reason: outcome?.reason ?? 'Approved source is registered, but discovery produced no verified release metadata.',
      source: outcome?.source ?? registeredSource,
      license: outcome?.license ?? manifestLicense,
      compatibility: outcome?.compatibility ?? unknownCompatibility,
      candidate: null,
    };
  }
  if (outcome.kind === 'incompatible') {
    return {
      ...base,
      status: 'incompatible',
      reason: outcome.reason,
      source: outcome.source ?? registeredSource,
      license: outcome.license ?? manifestLicense,
      compatibility: outcome.compatibility,
      candidate: null,
    };
  }
  if (outcome.kind !== 'candidate') batchFail('batch-outcome-kind-forbidden', `unsupported outcome for ${manifest.name}`);
  validateCandidateOutcome(outcome.candidate, registration?.manifest_name ?? manifest.name);
  const qualified = outcome.candidate.eligible_for_plan === true;
  return {
    ...base,
    status: qualified ? 'qualified' : 'manual-hash-approval-required',
    reason: qualified
      ? 'Candidate passed the existing trusted-source and integrity policy gates.'
      : 'Official public artifact was validated, but this exact SHA-256 requires manual approval before persistence or planning.',
    source: outcome.source ?? registeredSource,
    license: outcome.license ?? manifestLicense,
    compatibility: outcome.compatibility,
    candidate: { ...outcome.candidate },
  };
}

function validateCandidateOutcome(candidate, manifestName) {
  if (!candidate || typeof candidate !== 'object' || !TOKEN.test(candidate.artifact_id ?? '')
      || !/^[0-9a-f]{64}$/.test(candidate.sha256 ?? '') || typeof candidate.version !== 'string'
      || candidate.version.length === 0 || typeof candidate.eligible_for_plan !== 'boolean'
      || typeof candidate.manual_approval_required !== 'boolean'
      || candidate.manual_approval_required !== !candidate.eligible_for_plan) {
    batchFail('batch-candidate-outcome-malformed', `candidate outcome is malformed for ${manifestName}`);
  }
  requireHttps(candidate.source_url, 'candidate.source_url');
}

function sourceFromRegistration(registration) {
  return {
    registration: 'approved',
    type: registration.kind,
    plugin_id: registration.plugin_id,
    source_id: registration.source_id ?? null,
    adapter: registration.adapter ?? null,
    project: registration.project ?? null,
    project_id: registration.project_id ?? null,
    official_url: registration.official_url,
  };
}

function validateInventory(inventory) {
  if (!inventory || inventory.schema_version !== 1 || !Array.isArray(inventory.plugins)
      || !Array.isArray(inventory.targets) || typeof inventory.minecraft_version !== 'string'
      || !inventory.manifest || inventory.manifest.plugin_count !== inventory.plugins.length
      || !Number.isInteger(inventory.installation_count)) {
    batchFail('batch-report-input-invalid', 'validated manifest inventory is required');
  }
}

function requireHttps(value, label) {
  let url;
  try { url = new URL(value); } catch { batchFail('batch-source-registry-malformed', `${label} must be a valid URL`); }
  if (url.protocol !== 'https:' || url.username || url.password) {
    batchFail('batch-source-registry-malformed', `${label} must use credential-free HTTPS`);
  }
}
