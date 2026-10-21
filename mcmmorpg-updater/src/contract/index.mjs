// Public surface of the update-plan contract (Task 1).
export { canonicalize, canonicalHash, sha256Hex } from './canonical.mjs';
export {
  EXECUTOR_OPERATIONS,
  OPERATION_ARG_SCHEMAS,
  isKnownOperation,
  validateOperation,
} from './operation-policy.mjs';
export {
  PLAN_SCHEMA_VERSION,
  PLAN_STATES,
  PlanValidationError,
  computePlanHash,
  validatePlanStructure,
  validatePlanIntegrity,
} from './plan-schema.mjs';
export {
  APPROVAL_OWNER_UID_ROOT,
  InMemoryNonceStore,
  NonceAlreadyUsedError,
  createApproval,
  verifyApproval,
} from './approval.mjs';
export { InMemoryLockManager, TargetLockedError, acquireAll } from './lock.mjs';
export { buildApprovalReport } from './report.mjs';
export { AuthorizationError, authorizeExecution } from './authorize.mjs';
