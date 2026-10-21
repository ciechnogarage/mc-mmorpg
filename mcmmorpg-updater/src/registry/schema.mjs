import { EXECUTOR_OPERATIONS } from '../contract/operation-policy.mjs';

export const REGISTRY_SCHEMA_VERSION = 1;
export const MUTATION_POLICY_SCHEMA_VERSION = 1;
export const REQUIRED_TARGET_IDS = Object.freeze(['world', 'items', 'hub', 'velocity']);
export const MUTATION_CLASSES = Object.freeze(['rollback', 'excluded']);
export const BACKUP_METHODS = Object.freeze(['copy', 'none']);
export const RESTORE_METHODS = Object.freeze(['replace-from-backup', 'none']);
export const REQUIRED_LIFECYCLE_OPERATIONS = EXECUTOR_OPERATIONS;

export const TARGET_EXPECTATIONS = Object.freeze({
  world: Object.freeze({
    service: 'srv-world',
    profile: 'world',
    containerDataPath: '/data',
    pilotEligible: true,
    blastRadius: 'single-backend',
  }),
  items: Object.freeze({
    service: 'srv-items',
    profile: 'items',
    containerDataPath: '/data',
    pilotEligible: true,
    blastRadius: 'single-backend',
  }),
  hub: Object.freeze({
    service: 'srv-hub',
    profile: 'hub',
    containerDataPath: '/data',
    pilotEligible: true,
    blastRadius: 'single-backend',
  }),
  velocity: Object.freeze({
    service: 'velocity',
    profile: null,
    containerDataPath: '/server',
    pilotEligible: false,
    blastRadius: 'global-entrypoint',
  }),
});
