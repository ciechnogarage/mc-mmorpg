import { createHash } from 'node:crypto';
import { classifyCompatibilityEvidence } from '../compatibility/evidence.mjs';
import { qualifyCandidate } from '../downloads/candidate.mjs';
import { findTrustedSource, validateTrustedSourcePolicy } from '../downloads/source-policy.mjs';
import { secureFetch } from '../downloads/network.mjs';
import { normalizeReleaseChannel, summarizeReleaseVersions, versionMatchesChannel } from './channel.mjs';
import { batchFail } from './errors.mjs';
import { validateBatchSourceRegistrations } from './report.mjs';
import { validateModrinthSearchPolicy } from './search-policy.mjs';
import { discoverVendorMetadata } from './vendor-adapters.mjs';

const TOKEN = /^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$/;
const HEX40 = /^[0-9a-f]{40}$/;
const HEX128 = /^[0-9a-f]{128}$/;
const MAX_VERSIONS = 1000;

export async function discoverBatchOutcomes({
  inventory,
  registrations,
  registry,
  registryInventory,
  policy,
  projectRoot,
  operator,
  lookupImpl,
  fetchImpl,
  voxelClient,
  qualifyCandidateImpl = qualifyCandidate,
  approvedCandidates = new Map(),
  selection = null,
  approvedBy,
  approvedAt,
  channel = 'stable',
}) {
  if (!inventory?.plugins || !registry?.plugins?.installations || !registryInventory?.targets) {
    batchFail('batch-discovery-input-invalid', 'batch inventory plus validated registry inventory are required');
  }
  const sourceRegistry = validateBatchSourceRegistrations(registrations);
  const searchPolicy = registrations.modrinth_search === undefined
    ? null
    : validateModrinthSearchPolicy(registrations.modrinth_search);
  const normalizedChannel = normalizeReleaseChannel(channel);
  if (!(approvedCandidates instanceof Map) || (selection !== null && !(selection instanceof Set))) {
    batchFail('batch-discovery-input-invalid', 'approvedCandidates must be a Map and selection must be a Set');
  }
  const outcomes = new Map();

  for (const entry of inventory.plugins) {
    if (selection && !selection.has(entry.manifest.name)) continue;
    const registration = sourceRegistry.get(entry.manifest.name);
    if (entry.manifest.license_status === 'internal') continue;
    if (!registration && !searchPolicy) continue;
    try {
      const context = {
        entry,
        minecraftVersion: inventory.minecraft_version,
      searchPolicy,
      registry,
      registryInventory,
      policy,
        projectRoot,
        operator,
        lookupImpl,
        fetchImpl,
        voxelClient,
      channel: normalizedChannel,
      qualifyCandidateImpl,
        approval: approvedCandidates.get(entry.manifest.name),
        approvedBy,
        approvedAt,
      };
      outcomes.set(entry.manifest.name, registration?.kind === 'premium-or-login'
        ? await discoverVendorMetadata({ ...context, registration })
        : registration
          ? await discoverRegisteredSource({ ...context, registration })
          : await discoverSearchedSource({ ...context, searchPolicy }));
    } catch (error) {
      outcomes.set(entry.manifest.name, blockedOutcome({
        entry,
        registration,
        minecraftVersion: inventory.minecraft_version,
        error,
      }));
    }
  }
  return outcomes;
}

async function discoverSearchedSource(context) {
  const identity = localPluginIdentity(context.entry);
  const search = {
    query: identity.name ?? context.entry.manifest.name,
    search_url: null,
    total_hits: 0,
    returned_hits: 0,
    evaluated_project_ids: [],
    required_project_status: 'approved',
    required_loader: 'paper',
    required_minecraft_version: context.minecraftVersion,
  };
  const rejections = [...identity.rejections];
  const searchUrl = new URL(`https://${context.searchPolicy.search.host}${context.searchPolicy.search.path_prefix}`);
  searchUrl.searchParams.set('query', identity.name);
  searchUrl.searchParams.set('limit', String(context.searchPolicy.max_results));
  search.search_url = searchUrl.href;
  const searchBytes = await secureFetch({
    url: searchUrl.href,
    endpoint: context.searchPolicy.search,
    redirects: context.searchPolicy.redirects,
    lookupImpl: context.lookupImpl,
    fetchImpl: context.fetchImpl,
  });
  const response = parseSearchResponse(searchBytes, context.searchPolicy.max_results);
  search.total_hits = response.total_hits;
  search.returned_hits = response.hits.length;

  const projects = [];
  const seenProjects = new Set();
  for (const hit of response.hits) {
    if (!hit || typeof hit !== 'object' || !TOKEN.test(hit.project_id ?? '')
        || typeof hit.title !== 'string' || typeof hit.slug !== 'string') {
      rejections.push({ stage: 'search', project_id: null, reason: 'search-hit-malformed' });
      continue;
    }
    if (seenProjects.has(hit.project_id)) {
      rejections.push({ stage: 'search', project_id: hit.project_id, reason: 'search-project-duplicate' });
      continue;
    }
    seenProjects.add(hit.project_id);
    if (!identityMatches(search.query, hit.title, hit.slug)) {
      rejections.push({ stage: 'search', project_id: hit.project_id, reason: 'search-identity-mismatch' });
      continue;
    }
    const projectUrl = `https://${context.searchPolicy.project.host}${context.searchPolicy.project.path_prefix}${hit.project_id}`;
    let project;
    try {
      const bytes = await secureFetch({
        url: projectUrl,
        endpoint: context.searchPolicy.project,
        redirects: context.searchPolicy.redirects,
        lookupImpl: context.lookupImpl,
        fetchImpl: context.fetchImpl,
      });
      project = parseProject(bytes, hit.project_id);
    } catch (error) {
      rejections.push({ stage: 'project', project_id: hit.project_id, reason: error?.code ?? 'project-metadata-invalid' });
      continue;
    }
    search.evaluated_project_ids.push(project.id);
    if (project.status !== 'approved') {
      rejections.push({ stage: 'project', project_id: project.id, reason: 'project-status-not-approved', status: project.status });
      continue;
    }
    if (!identityMatches(search.query, project.title, project.slug)) {
      rejections.push({ stage: 'project', project_id: project.id, reason: 'project-identity-mismatch' });
      continue;
    }
    projects.push(project);
  }

  const candidates = [];
  let incompatibleProjects = 0;
  for (const project of projects) {
    const source = dynamicSource(context, project, identity.name);
    const metadataUrl = `https://${source.discovery.host}${source.discovery.path_prefix}`;
    let versions;
    try {
      const bytes = await secureFetch({
        url: metadataUrl,
        endpoint: source.discovery,
        redirects: source.redirects,
        lookupImpl: context.lookupImpl,
        fetchImpl: context.fetchImpl,
      });
      versions = parseModrinthVersions(bytes, project.id);
    } catch (error) {
      rejections.push({ stage: 'release', project_id: project.id, reason: error?.code ?? 'release-metadata-invalid' });
      continue;
    }
    const availableVersions = summarizeReleaseVersions(versions, {
      minecraftVersion: context.minecraftVersion,
      loaders: ['paper'],
      channel: context.channel,
    });
    const compatible = versions
      .filter((version) => version.status === 'listed' && versionMatchesChannel(version, context.channel)
      && classifyCompatibilityEvidence({
        minecraftVersion: context.minecraftVersion,
        loaders: version.loaders,
        gameVersions: version.game_versions,
        version: version.version_number,
        title: version.name,
        datePublished: version.date_published,
        source: 'modrinth-project-version-api',
      }).compatible === true && version.loaders.includes('paper'))
      .sort((left, right) => Date.parse(right.date_published) - Date.parse(left.date_published));
    if (compatible.length === 0) {
      incompatibleProjects += 1;
      rejections.push({ stage: 'release', project_id: project.id, reason: 'release-incompatible', available_versions: availableVersions });
      continue;
    }
    const selected = compatible[0];
    const primaryFiles = selected.files.filter((file) => file.primary === true);
    if (primaryFiles.length !== 1) {
      rejections.push({ stage: 'release', project_id: project.id, reason: 'primary-file-invalid' });
      continue;
    }
    const file = primaryFiles[0];
    if (!identity.name) {
      rejections.push({ stage: 'candidate', project_id: project.id, reason: 'local-plugin-identity-unavailable' });
      continue;
    }
    try {
      candidates.push(await qualifySearchedProject({ context, project, source, selected, file, identity, availableVersions }));
    } catch (error) {
      rejections.push({ stage: 'candidate', project_id: project.id, reason: error?.code ?? 'candidate-validation-failed' });
    }
  }

  if (candidates.length > 1) {
    rejections.push({ stage: 'selection', project_id: null, reason: 'ambiguous-qualified-projects', count: candidates.length });
    return searchedBlocked(context, search, rejections, 'Multiple approved projects passed identity and release gates; selection is ambiguous.');
  }
  if (candidates.length === 1) return { ...candidates[0], search, rejections };
  if (projects.length > 0 && incompatibleProjects === projects.length) {
    return {
      kind: 'incompatible',
      source: { registration: 'verified-search', adapter: context.searchPolicy.adapter },
      license: manifestLicense(context.entry),
      compatibility: {
        minecraft_version: context.minecraftVersion,
        compatible: false,
        loaders: ['paper'],
        evidence: 'modrinth-project-and-release-api',
      },
      reason: `Approved exact-identity Modrinth projects have no listed release for Paper ${context.minecraftVersion}.`,
      search,
      rejections,
    };
  }
  return searchedBlocked(context, search, rejections, 'No single approved Modrinth project passed identity, release, and JAR gates.');
}

async function qualifySearchedProject({ context, project, source, selected, file, identity, availableVersions = [] }) {
  const overlay = candidateOverlay(context, identity);
  const qualifiedSource = { ...source, plugin_id: overlay.pluginId };
  const result = await context.qualifyCandidateImpl({
    registry: overlay.registry,
    inventory: overlay.inventory,
    policy: { schema_version: 1, sources: [qualifiedSource] },
    request: {
      source_id: qualifiedSource.id,
      plugin_id: overlay.pluginId,
      artifact_id: selected.id,
      url: file.url,
      targets: overlay.targets,
    },
    projectRoot: context.projectRoot,
    operator: context.operator,
    lookupImpl: context.lookupImpl,
    fetchImpl: context.fetchImpl,
    manualApproval: approvedManualHash(context, {
      sourceId: qualifiedSource.id,
      pluginId: overlay.pluginId,
      project,
      selected,
      file,
      targets: overlay.targets,
      expectedPluginName: identity.name,
    }),
  });
  return {
    kind: 'candidate',
    source: {
      registration: 'verified-search',
      adapter: 'modrinth-search-v2',
      project: qualifiedSource.project,
      project_id: project.id,
      project_status: project.status,
      official_url: `https://modrinth.com/project/${project.slug}`,
      metadata_url: `https://${source.discovery.host}${source.discovery.path_prefix}`,
      release_id: selected.id,
      file_id: file.id,
      upstream_hashes: { ...file.hashes },
    },
    license: {
      status: project.license.id,
      name: project.license.name,
      url: project.license.url,
      provenance: 'modrinth-project-api',
    },
    compatibility: {
      minecraft_version: context.minecraftVersion,
      compatible: true,
      loaders: [...selected.loaders],
      evidence: 'modrinth-project-and-release-api',
    },
    available_versions: availableVersions,
    candidate: candidateFromResult(result),
  };
}

function approvedManualHash(context, current) {
  if (context.approval === undefined) return undefined;
  const approved = context.approval;
  const candidate = approved?.candidate;
  const source = approved?.source;
  if (!approved || approved.status !== 'manual-hash-approval-required'
      || source?.project_id !== current.project.id || source.project_status !== 'approved'
      || source.release_id !== current.selected.id || source.file_id !== current.file.id
      || candidate?.artifact_id !== current.selected.id || candidate.source_url !== current.file.url
      || candidate.plugin_yml?.name !== current.expectedPluginName
      || candidate.path !== null || candidate.manual_approval_required !== true
      || candidate.eligible_for_plan !== false || !/^[0-9a-f]{64}$/.test(candidate.sha256 ?? '')
      || !sameStrings(candidate.targets, current.targets)
      || typeof context.approvedBy !== 'string' || context.approvedBy.length === 0
      || !Number.isFinite(Date.parse(context.approvedAt))) {
    batchFail('batch-report-candidate-drift', `approved candidate no longer matches ${approved?.name ?? 'report entry'}`);
  }
  return {
    plugin_id: current.pluginId,
    source_id: current.sourceId,
    artifact_id: current.selected.id,
    sha256: candidate.sha256,
    approved_by: context.approvedBy,
    approved_at: new Date(context.approvedAt).toISOString(),
  };
}

function sameStrings(left, right) {
  if (!Array.isArray(left) || !Array.isArray(right) || left.length !== right.length) return false;
  const sortedLeft = [...left].sort();
  const sortedRight = [...right].sort();
  return sortedLeft.every((value, index) => value === sortedRight[index]);
}

function localPluginIdentity(entry) {
  const names = new Set(entry.installations
    .map((installation) => installation.jar?.plugin_yml)
    .filter((metadata) => metadata?.status === 'verified')
    .map((metadata) => metadata.name));
  if (names.size === 1) return { name: [...names][0], rejections: [] };
  if (names.size === 0) {
    return {
      name: null,
      rejections: [{ stage: 'local-inventory', project_id: null, reason: 'local-plugin-identity-unavailable' }],
    };
  }
  return {
    name: null,
    rejections: [{ stage: 'local-inventory', project_id: null, reason: 'local-plugin-identity-ambiguous', count: names.size }],
  };
}

function parseSearchResponse(bytes, maxResults) {
  let payload;
  try { payload = JSON.parse(bytes.toString('utf8')); } catch { batchFail('batch-search-response-malformed', 'Modrinth search response is not valid JSON'); }
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)
      || !Array.isArray(payload.hits) || payload.hits.length > maxResults
      || !Number.isInteger(payload.total_hits) || payload.total_hits < 0) {
    batchFail('batch-search-response-malformed', 'Modrinth search response is malformed or exceeds its result bound');
  }
  return payload;
}

function parseProject(bytes, expectedId) {
  let project;
  try { project = JSON.parse(bytes.toString('utf8')); } catch { batchFail('batch-project-response-malformed', 'Modrinth project response is not valid JSON'); }
  if (!project || typeof project !== 'object' || Array.isArray(project)
      || project.id !== expectedId || !TOKEN.test(project.id)
      || typeof project.slug !== 'string' || project.slug.length === 0 || project.slug.length > 256
      || typeof project.title !== 'string' || project.title.length === 0 || project.title.length > 256
      || typeof project.status !== 'string' || project.status.length > 64
      || !['mod', 'plugin'].includes(project.project_type)
      || !project.license || typeof project.license.id !== 'string' || project.license.id.length === 0
      || typeof project.license.name !== 'string' || project.license.name.length === 0
      || !validOptionalHttps(project.license.url)) {
    batchFail('batch-project-response-malformed', 'Modrinth project response failed identity/status/license validation');
  }
  return {
    id: project.id,
    slug: project.slug,
    title: project.title,
    status: project.status,
    project_type: project.project_type,
    license: { id: project.license.id, name: project.license.name, url: project.license.url ?? null },
    monetization_status: typeof project.monetization_status === 'string' ? project.monetization_status : null,
  };
}

function dynamicSource(context, project, expectedPluginName) {
  const hash = createHash('sha256').update(`${context.entry.manifest.name}\0${project.id}`).digest('hex').slice(0, 20);
  return {
    id: `modrinth-search-${hash}`,
    plugin_id: `manifest-${hash}`,
    plugin_name: expectedPluginName,
    adapter: 'pinned-https-release',
    project: `modrinth/${project.id}`,
    discovery: {
      adapter: 'modrinth-project-versions',
      host: context.searchPolicy.project.host,
      project_path: `/v2/project/${project.id}`,
      path_prefix: `/v2/project/${project.id}/version`,
      max_bytes: context.searchPolicy.project.max_bytes,
    },
    artifact: {
      host: context.searchPolicy.artifact.host,
      project_path: `/data/${project.id}`,
      path_prefix: `/data/${project.id}/versions/`,
      max_bytes: context.searchPolicy.artifact.max_bytes,
    },
    manifest: null,
    redirects: { ...context.searchPolicy.redirects },
    signature_keys: {},
  };
}

function candidateOverlay(context, identity) {
  const hash = createHash('sha256').update(`${context.entry.manifest.name}\0${identity.name}`).digest('hex').slice(0, 20);
  const pluginId = `manifest-${hash}`;
  const current = context.entry.installations.filter(
    (installation) => installation.jar?.plugin_yml?.status === 'verified'
      && installation.jar.plugin_yml.name === identity.name,
  );
  if (current.length === 0) batchFail('batch-candidate-targets-missing', 'no exact current installation has a verified plugin.yml identity');
  const targets = [...new Set(current.map((installation) => installation.target))];
  const additions = current.map((installation) => ({
    plugin_id: pluginId,
    target: installation.target,
    installed_version: installation.installed_version,
    jar_path: installation.jar.path,
    config_paths: [],
  }));
  const registry = {
    ...context.registry,
    plugins: {
      ...context.registry.plugins,
      installations: [...context.registry.plugins.installations, ...additions],
    },
  };
  const inventory = {
    ...context.registryInventory,
    targets: context.registryInventory.targets.map((target) => {
      const installation = current.find((entry) => entry.target === target.target);
      if (!installation) return target;
      return {
        ...target,
        installations: [...target.installations, {
          plugin_id: pluginId,
          installed_version: installation.installed_version,
          jar: { status: 'present', path: installation.jar.path, sha256: installation.jar.sha256 },
        }],
      };
    }),
  };
  return { registry, inventory, pluginId, targets };
}

function candidateFromResult(result) {
  return {
    version: result.candidate.version,
    artifact_id: result.candidate.artifact_id,
    source_url: result.candidate.source_url,
    sha256: result.candidate.sha256,
    size: result.candidate.size,
    path: result.candidate.path,
    integrity: { ...result.candidate.integrity },
    plugin_yml: { name: result.candidate.plugin_name, version: result.candidate.version },
    targets: [...result.candidate.targets],
    eligible_for_plan: result.candidate.eligible_for_plan,
    manual_approval_required: result.candidate.manual_approval_required,
  };
}

function searchedBlocked(context, search, rejections, reason) {
  return {
    kind: 'source-not-found',
    source: { registration: 'verified-search', adapter: context.searchPolicy.adapter },
    license: manifestLicense(context.entry),
    compatibility: { minecraft_version: context.minecraftVersion, compatible: null, loaders: ['paper'], evidence: null },
    reason,
    search,
    rejections,
  };
}

function identityMatches(expected, ...candidates) {
  const normalized = normalizeIdentity(expected);
  return normalized.length > 0 && candidates.some((candidate) => normalizeIdentity(candidate) === normalized);
}

function normalizeIdentity(value) {
  return typeof value === 'string' ? value.normalize('NFKC').toLowerCase().replace(/[^a-z0-9]/g, '') : '';
}

function validOptionalHttps(value) {
  if (value === null || value === undefined) return true;
  let url;
  try { url = new URL(value); } catch { return false; }
  return url.protocol === 'https:' && !url.username && !url.password;
}

async function discoverRegisteredSource(context) {
  const { entry, registration, minecraftVersion, policy, lookupImpl, fetchImpl, searchPolicy } = context;
  const trustedPolicy = validateTrustedSourcePolicy(policy);
  const source = findTrustedSource(trustedPolicy, registration.source_id);
  if (source.plugin_id !== registration.plugin_id || source.project !== registration.project) {
    batchFail('batch-source-policy-mismatch', 'batch registration does not match the trusted source policy');
  }
  if (!source.discovery || source.discovery.adapter !== registration.adapter) {
    batchFail('batch-source-discovery-missing', 'trusted source has no matching official discovery endpoint');
  }
  if (!searchPolicy) batchFail('batch-search-policy-missing', 'Modrinth project status verification policy is required');
  const projectUrl = `https://${searchPolicy.project.host}${searchPolicy.project.path_prefix}${registration.project_id}`;
  const projectBytes = await secureFetch({
    url: projectUrl,
    endpoint: searchPolicy.project,
    redirects: searchPolicy.redirects,
    lookupImpl,
    fetchImpl,
  });
  const project = parseProject(projectBytes, registration.project_id);
  if (project.status !== 'approved') batchFail('batch-project-status-not-approved', 'registered Modrinth project is not approved');
  if (!identityMatches(source.plugin_name, project.title, project.slug)) {
    batchFail('batch-project-identity-mismatch', 'registered Modrinth project identity does not match plugin.yml');
  }
  // Official operation contract: https://docs.modrinth.com/api/operations/getprojectversions/
  const metadataUrl = `https://${source.discovery.host}${source.discovery.path_prefix}`;
  const bytes = await secureFetch({
    url: metadataUrl,
    endpoint: source.discovery,
    redirects: source.redirects,
    lookupImpl,
    fetchImpl,
  });
  const versions = parseModrinthVersions(bytes, registration.project_id);
  const compatible = versions
    .filter((version) => version.status === 'listed'
      && registration.compatibility.release_types.includes(version.version_type)
      && version.game_versions.includes(minecraftVersion)
      && version.loaders.some((loader) => registration.compatibility.loaders.includes(loader)))
    .sort((left, right) => Date.parse(right.date_published) - Date.parse(left.date_published));

  if (compatible.length === 0) {
    return {
      kind: 'incompatible',
      source: sourceProvenance(registration, metadataUrl, project),
      license: projectLicense(project),
      compatibility: {
        minecraft_version: minecraftVersion,
        compatible: false,
        loaders: [...registration.compatibility.loaders],
        evidence: 'official-release-metadata',
      },
      reason: `No listed official release supports Minecraft ${minecraftVersion} and an approved loader/release type.`,
    };
  }

  const selected = compatible[0];
  const primaryFiles = selected.files.filter((file) => file.primary === true);
  if (primaryFiles.length !== 1) batchFail('batch-source-primary-file-invalid', 'official release must identify exactly one primary file');
  const file = primaryFiles[0];
  const targets = [...new Set(entry.installations.map((installation) => installation.target))]
    .filter((target) => context.registry.plugins.installations.some(
      (installation) => installation.plugin_id === registration.plugin_id && installation.target === target,
    ));
  if (targets.length === 0) {
    batchFail('batch-candidate-targets-missing', 'no current installation is registered for candidate qualification');
  }

  const result = await context.qualifyCandidateImpl({
    registry: context.registry,
    inventory: context.registryInventory,
    policy,
    request: {
      source_id: registration.source_id,
      plugin_id: registration.plugin_id,
      artifact_id: selected.id,
      url: file.url,
      targets,
    },
    projectRoot: context.projectRoot,
    operator: context.operator,
    lookupImpl,
    fetchImpl,
    manualApproval: approvedManualHash(context, {
      sourceId: registration.source_id,
      pluginId: registration.plugin_id,
      project,
      selected,
      file,
      targets,
      expectedPluginName: source.plugin_name,
    }),
  });
  const sourceDetails = sourceProvenance(registration, metadataUrl, project);
  sourceDetails.release_id = selected.id;
  sourceDetails.file_id = file.id;
  sourceDetails.upstream_hashes = { ...file.hashes };
  return {
    kind: 'candidate',
    source: sourceDetails,
    license: projectLicense(project),
    compatibility: {
      minecraft_version: minecraftVersion,
      compatible: true,
      loaders: [...selected.loaders],
      evidence: 'official-release-metadata',
    },
    candidate: {
      version: result.candidate.version,
      artifact_id: result.candidate.artifact_id,
      source_url: result.candidate.source_url,
      sha256: result.candidate.sha256,
      size: result.candidate.size,
      path: result.candidate.path,
      integrity: { ...result.candidate.integrity },
      plugin_yml: { name: result.candidate.plugin_name, version: result.candidate.version },
      targets: [...result.candidate.targets],
      eligible_for_plan: result.candidate.eligible_for_plan,
      manual_approval_required: result.candidate.manual_approval_required,
    },
  };
}

function parseModrinthVersions(bytes, projectId) {
  let payload;
  try { payload = JSON.parse(bytes.toString('utf8')); } catch { batchFail('batch-source-metadata-malformed', 'official release metadata is not valid JSON'); }
  if (!Array.isArray(payload) || payload.length > MAX_VERSIONS) {
    batchFail('batch-source-metadata-malformed', 'official release metadata must be a bounded array');
  }
  return payload.map((version) => validateVersion(version, projectId));
}

function validateVersion(version, projectId) {
  if (!version || typeof version !== 'object' || Array.isArray(version)
      || !TOKEN.test(version.id ?? '') || version.project_id !== projectId
      || typeof version.version_number !== 'string' || version.version_number.length === 0
      || !['release', 'beta', 'alpha'].includes(version.version_type)
      || typeof version.status !== 'string' || !Number.isFinite(Date.parse(version.date_published))
      || !shortStringArray(version.game_versions) || !shortStringArray(version.loaders)
      || !Array.isArray(version.files) || version.files.length === 0 || version.files.length > 100) {
    batchFail('batch-source-metadata-malformed', 'official release metadata contains an invalid version');
  }
  return { ...version, files: version.files.map(validateFile) };
}

function validateFile(file) {
  let url;
  try { url = new URL(file?.url); } catch { batchFail('batch-source-metadata-malformed', 'official release file URL is invalid'); }
  if (!file || typeof file !== 'object' || !TOKEN.test(file.id ?? '')
      || typeof file.filename !== 'string' || file.filename.length === 0 || file.filename.length > 512
      || typeof file.primary !== 'boolean' || !Number.isInteger(file.size) || file.size < 1
      || url.protocol !== 'https:' || url.username || url.password
      || !file.hashes || !HEX40.test(file.hashes.sha1 ?? '') || !HEX128.test(file.hashes.sha512 ?? '')) {
    batchFail('batch-source-metadata-malformed', 'official release metadata contains an invalid file');
  }
  return { ...file, url: url.href, hashes: { sha1: file.hashes.sha1, sha512: file.hashes.sha512 } };
}

function shortStringArray(value) {
  return Array.isArray(value) && value.length > 0 && value.length <= 100
    && value.every((entry) => typeof entry === 'string' && entry.length > 0 && entry.length <= 128);
}

function blockedOutcome({ entry, registration, minecraftVersion, error }) {
  const code = typeof error?.code === 'string' ? error.code : 'batch-source-discovery-failed';
  return {
    kind: 'source-not-found',
    source: registration
      ? sourceProvenance(registration, null)
      : { registration: 'verified-search', adapter: 'modrinth-search-v2' },
    license: manifestLicense(entry),
    compatibility: { minecraft_version: minecraftVersion, compatible: null, loaders: [], evidence: null },
    reason: `Official source discovery was blocked (${code}); no unofficial mirror or authentication bypass was attempted.`,
    error: { code, message: 'Verified source revalidation failed.' },
  };
}

function sourceProvenance(registration, metadataUrl, project = null) {
  return {
    source_id: registration.source_id,
    adapter: registration.adapter,
    project: registration.project,
    project_id: registration.project_id,
    official_url: registration.official_url,
    metadata_url: metadataUrl,
    project_status: project?.status ?? null,
  };
}

function manifestLicense(entry) {
  return { status: entry.manifest.license_status, provenance: 'plugin-manifest' };
}

function projectLicense(project) {
  return {
    status: project.license.id,
    name: project.license.name,
    url: project.license.url,
    provenance: 'modrinth-project-api',
  };
}
