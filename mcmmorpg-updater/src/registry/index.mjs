import { readFile, realpath, stat } from 'node:fs/promises';
import { resolve } from 'node:path';
import { canonicalHash } from '../contract/canonical.mjs';
import { inspectComposeFile } from './compose.mjs';
import {
  MUTATION_CLASSES,
  MUTATION_POLICY_SCHEMA_VERSION,
  REGISTRY_SCHEMA_VERSION,
  REQUIRED_LIFECYCLE_OPERATIONS,
  REQUIRED_TARGET_IDS,
  TARGET_EXPECTATIONS,
} from './schema.mjs';
import {
  assertCanonicalAbsolutePath,
  assertCanonicalRelativePath,
  assertWithinRoot,
  classifyExcludedPath,
  resolveRegisteredPath,
} from './path-policy.mjs';

const HEX64 = /^[0-9a-f]{64}$/;

export class RegistryValidationError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'RegistryValidationError';
    this.code = code;
  }
}

export async function loadRegistry({ targetsPath, pluginsPath }) {
  const [targets, plugins] = await Promise.all([
    readJson(targetsPath),
    readJson(pluginsPath),
  ]);
  return validateRegistry({ targets, plugins });
}

export async function validateRegistry({ targets, plugins }) {
  requireObject(targets, 'registry-targets-malformed', 'targets registry');
  requireObject(plugins, 'registry-plugins-malformed', 'plugins registry');
  requireVersion(targets.schema_version, 'targets registry');
  requireVersion(plugins.schema_version, 'plugins registry');
  requireObject(targets.compose, 'compose-config-malformed', 'targets.compose');
  requireObject(targets.targets, 'registry-targets-malformed', 'targets.targets');

  const fail = (code, message) => { throw new RegistryValidationError(code, message); };
  const allowedRoot = assertCanonicalAbsolutePath(targets.allowed_host_root, fail);
  const canonicalRoot = await realpath(allowedRoot);
  if (canonicalRoot !== allowedRoot) fail('path-noncanonical', 'allowed_host_root must be its real canonical path');

  const composePath = assertCanonicalAbsolutePath(targets.compose.path, fail);
  assertWithinRoot(allowedRoot, composePath, fail);
  const compose = await inspectComposeFile(composePath);
  if (targets.compose.project !== compose.project) {
    fail('compose-project-mismatch', `registry project ${targets.compose.project} does not match Compose ${compose.project}`);
  }
  if (!HEX64.test(targets.compose.config_sha256 ?? '') || targets.compose.config_sha256 !== compose.configSha256) {
    fail('compose-config-digest-mismatch', 'registry Compose config SHA-256 is stale or malformed');
  }

  validateTargetSet(targets.targets, fail);
  for (const id of REQUIRED_TARGET_IDS) {
    await validateTarget(id, targets.targets[id], { allowedRoot, compose, project: targets.compose.project, fail });
  }
  validatePlugins(plugins, targets.targets, fail);

  return { targets, plugins, compose: { project: compose.project, config_sha256: compose.configSha256 } };
}

async function validateTarget(id, target, context) {
  const { allowedRoot, compose, project, fail } = context;
  requireObject(target, 'target-malformed', `target ${id}`);
  const expected = TARGET_EXPECTATIONS[id];
  if (target.compose_project !== project) fail('compose-project-mismatch', `${id} has wrong Compose project`);
  const service = compose.services.get(target.compose_service);
  if (!service) fail('compose-service-missing', `Compose service does not exist: ${target.compose_service}`);
  if (target.compose_service !== expected.service) fail('compose-service-mismatch', `${id} must use ${expected.service}`);
  if (target.compose_profile !== expected.profile) fail('compose-profile-mismatch', `${id} must use profile ${expected.profile}`);
  if (target.container_data_path !== expected.containerDataPath) fail('container-path-mismatch', `${id} has wrong container data path`);
  if (target.pilot_eligible !== expected.pilotEligible) fail('pilot-eligibility-invalid', `${id} has unsafe pilot eligibility`);
  if (target.blast_radius !== expected.blastRadius) fail('blast-radius-invalid', `${id} has wrong blast radius`);

  if (service.profile !== target.compose_profile) fail('compose-profile-mismatch', `${id} profile does not match Compose`);

  const hostPath = assertCanonicalAbsolutePath(target.host_data_path, fail);
  assertWithinRoot(allowedRoot, hostPath, fail);
  const realHostPath = await realpath(hostPath);
  if (realHostPath !== hostPath) fail('path-noncanonical', `${id} host data path must be its real path`);
  if (!(await stat(hostPath)).isDirectory()) fail('target-path-not-directory', `${id} host data path is not a directory`);
  const mount = service.volumes.find((volume) => volume.container === target.container_data_path);
  if (!mount || mount.host !== hostPath) fail('compose-data-path-mismatch', `${id} data path does not match its Compose bind mount`);

  assertCanonicalRelativePath(target.plugin_directory, fail);
  resolveRegisteredPath(hostPath, target.plugin_directory, fail);
  validateLifecycle(target.lifecycle, fail, id);
}

function validateLifecycle(lifecycle, fail, id) {
  requireObject(lifecycle, 'lifecycle-malformed', `${id}.lifecycle`);
  const allowed = lifecycle.allowed_operations;
  if (!Array.isArray(allowed)
      || allowed.length !== REQUIRED_LIFECYCLE_OPERATIONS.length
      || REQUIRED_LIFECYCLE_OPERATIONS.some((operation) => !allowed.includes(operation))) {
    fail('lifecycle-operations-invalid', `${id} lifecycle must use the closed executor operation allowlist`);
  }
  if (lifecycle.single_service_only !== true
      || lifecycle.no_dependencies !== true
      || lifecycle.recreate !== false
      || lifecycle.require_current_config_digest !== true) {
    fail('lifecycle-restrictions-invalid', `${id} lifecycle must be single-service, no-deps, no-recreate, digest-checked`);
  }
}

function validatePlugins(plugins, targets, fail) {
  if (!Array.isArray(plugins.installations)) fail('registry-plugins-malformed', 'plugins.installations must be an array');
  const identities = new Set();
  for (const installation of plugins.installations) {
    requireObject(installation, 'plugin-installation-malformed', 'plugin installation');
    for (const field of ['plugin_id', 'target', 'installed_version', 'jar_path']) {
      if (typeof installation[field] !== 'string' || installation[field].length === 0) {
        fail('plugin-installation-malformed', `plugin installation.${field} must be a non-empty string`);
      }
    }
    const target = targets[installation.target];
    if (!target) fail('plugin-target-missing', `unknown plugin target: ${installation.target}`);
    const identity = `${installation.plugin_id}\0${installation.target}`;
    if (identities.has(identity)) fail('plugin-installation-duplicate', `duplicate plugin/target: ${installation.plugin_id}/${installation.target}`);
    identities.add(identity);

    validateInventoryPath(installation.jar_path, target, fail, { jar: true });
    if (!Array.isArray(installation.config_paths)) fail('plugin-installation-malformed', 'config_paths must be an array');
    for (const configPath of installation.config_paths) validateInventoryPath(configPath, target, fail);
    validateMutationPolicy(installation.mutation_policy, target, fail);
  }
}

function validateInventoryPath(registeredPath, target, fail, { jar = false } = {}) {
  assertCanonicalRelativePath(registeredPath, fail);
  resolveRegisteredPath(target.host_data_path, registeredPath, fail);
  if (jar && (!registeredPath.endsWith('.jar') || !registeredPath.startsWith(`${target.plugin_directory}/`))) {
    fail('plugin-jar-path-invalid', `JAR must be under ${target.plugin_directory}: ${registeredPath}`);
  }
  const excluded = classifyExcludedPath(registeredPath);
  if (excluded) fail('inventory-path-protected', `inventory path is excluded (${excluded}): ${registeredPath}`);
}

function validateMutationPolicy(policy, target, fail) {
  if (!policy || typeof policy !== 'object' || Array.isArray(policy)) {
    fail('mutation-policy-missing', 'every plugin/target installation needs a mutation policy');
  }
  if (policy.schema_version !== MUTATION_POLICY_SCHEMA_VERSION || policy.default_class !== 'excluded') {
    fail('mutation-policy-version', 'mutation policy must be schema v1 and default to excluded');
  }
  if (!Array.isArray(policy.entries)) fail('mutation-policy-malformed', 'mutation policy entries must be an array');
  const { manifest_sha256: ignored, ...manifest } = policy;
  if (!HEX64.test(policy.manifest_sha256 ?? '') || canonicalHash(manifest) !== policy.manifest_sha256) {
    fail('mutation-manifest-hash-mismatch', 'mutation policy manifest hash does not match its content');
  }

  const paths = new Set();
  for (const entry of policy.entries) {
    requireObject(entry, 'mutation-entry-malformed', 'mutation policy entry');
    assertCanonicalRelativePath(entry.path, fail);
    resolveRegisteredPath(target.host_data_path, entry.path, fail);
    if (paths.has(entry.path)) fail('mutation-path-duplicate', `duplicate mutation path: ${entry.path}`);
    paths.add(entry.path);
    if (!MUTATION_CLASSES.includes(entry.class)) fail('mutation-class-invalid', `invalid mutation class: ${entry.class}`);
    const protectedReason = classifyExcludedPath(entry.path);
    if (entry.class === 'rollback' && protectedReason) {
      fail('protected-path-rollback', `${entry.path} is ${protectedReason} and must remain excluded`);
    }
    if (entry.class === 'rollback'
        && (entry.backup_method !== 'copy' || entry.restore_method !== 'replace-from-backup')) {
      fail('mutation-method-invalid', 'rollback entries require copy/replace-from-backup');
    }
    if (entry.class === 'excluded' && (entry.backup_method !== 'none' || entry.restore_method !== 'none')) {
      fail('mutation-method-invalid', 'excluded entries require none/none methods');
    }
  }
}

function validateTargetSet(targets, fail) {
  const ids = Object.keys(targets).sort();
  const required = [...REQUIRED_TARGET_IDS].sort();
  if (ids.length !== required.length || ids.some((id, index) => id !== required[index])) {
    fail('target-set-invalid', `registry must contain exactly: ${REQUIRED_TARGET_IDS.join(', ')}`);
  }
}

function requireVersion(version, label) {
  if (version !== REGISTRY_SCHEMA_VERSION) {
    throw new RegistryValidationError('registry-schema-version', `${label} must use schema version ${REGISTRY_SCHEMA_VERSION}`);
  }
}

function requireObject(value, code, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new RegistryValidationError(code, `${label} must be an object`);
  }
}

async function readJson(path) {
  return JSON.parse(await readFile(path, 'utf8'));
}

export {
  REGISTRY_SCHEMA_VERSION,
  MUTATION_POLICY_SCHEMA_VERSION,
  REQUIRED_TARGET_IDS,
  TARGET_EXPECTATIONS,
} from './schema.mjs';
export { classifyExcludedPath } from './path-policy.mjs';
