// Executor operation allowlist and closed argument schemas.
//
// This is the single source of truth for which operations the executor will ever
// perform. It is a *literal* allowlist: an operation name that is not one of the
// nine declared here is rejected, and each operation accepts only the closed set
// of scalar arguments defined for it. There is deliberately no field that carries
// a shell command, and no "advanced"/passthrough escape hatch — an unknown key is
// rejected by construction because the schema is closed.
//
// Service IDs, compose projects/profiles and filesystem roots are intentionally
// NOT arguments here: the executor draws those from its own root-owned registry,
// never from the (controller-authored) plan.

/**
 * The nine executor operations. Order is stable and meaningful only as
 * documentation; membership is what the contract enforces.
 */
export const EXECUTOR_OPERATIONS = Object.freeze([
  'inventory',
  'backup',
  'prepare-staging',
  'compose-stop',
  'compose-start',
  'compose-run-test',
  'health-check',
  'deploy',
  'rollback',
]);

const OPERATION_SET = new Set(EXECUTOR_OPERATIONS);

const HEX64 = /^[0-9a-f]{64}$/;

const isBoundedInt = (min, max) => (v) => Number.isInteger(v) && v >= min && v <= max;
const isSha256 = (v) => typeof v === 'string' && HEX64.test(v);

/**
 * Closed argument schema per operation. Each entry maps an allowed argument key
 * to `{ required, valid }`. Any key not present here is forbidden. An empty
 * object means the operation takes no arguments at all.
 */
export const OPERATION_ARG_SCHEMAS = Object.freeze({
  inventory: {},
  backup: {},
  'prepare-staging': {},
  'compose-stop': {},
  'compose-start': {},
  'compose-run-test': {
    timeoutSeconds: { required: false, valid: isBoundedInt(1, 3600) },
  },
  'health-check': {
    graceSeconds: { required: false, valid: isBoundedInt(0, 3600) },
    timeoutSeconds: { required: false, valid: isBoundedInt(1, 3600) },
  },
  deploy: {
    // Bind the deploy to a specific candidate artifact by content hash. The
    // executor cross-checks this against the plan target's candidate SHA-256.
    candidateSha256: { required: true, valid: isSha256 },
  },
  rollback: {},
});

export function isKnownOperation(name) {
  return OPERATION_SET.has(name);
}

/**
 * Validate one plan operation against the allowlist and its closed arg schema.
 * `knownTargets` is the set/array of target names declared by the plan; an
 * operation may only reference a declared target.
 *
 * @returns {{ ok: true } | { ok: false, code: string, reason: string }}
 */
export function validateOperation(op, knownTargets) {
  if (op === null || typeof op !== 'object' || Array.isArray(op)) {
    return fail('operation-malformed', 'operation must be an object');
  }
  if (typeof op.op !== 'string') {
    return fail('operation-malformed', 'operation.op must be a string');
  }
  if (!OPERATION_SET.has(op.op)) {
    return fail('operation-not-allowed', `operation "${op.op}" is not in the allowlist`);
  }
  if (typeof op.target !== 'string') {
    return fail('operation-malformed', `operation "${op.op}" is missing a target`);
  }
  const targets = knownTargets instanceof Set ? knownTargets : new Set(knownTargets ?? []);
  if (!targets.has(op.target)) {
    return fail('unknown-target', `operation "${op.op}" references unknown target "${op.target}"`);
  }

  const schema = OPERATION_ARG_SCHEMAS[op.op];
  const args = op.args ?? {};
  if (args === null || typeof args !== 'object' || Array.isArray(args)) {
    return fail('operation-args-malformed', `operation "${op.op}" args must be an object`);
  }

  // Closed: reject any key not declared in the schema (this is what blocks a
  // smuggled `command`, `shell`, or `advanced` argument).
  for (const key of Object.keys(args)) {
    if (!Object.prototype.hasOwnProperty.call(schema, key)) {
      return fail('operation-arg-unknown', `operation "${op.op}" has forbidden argument "${key}"`);
    }
  }
  // Required present, and every provided value passes its validator.
  for (const [key, rule] of Object.entries(schema)) {
    const present = Object.prototype.hasOwnProperty.call(args, key);
    if (!present) {
      if (rule.required) {
        return fail('operation-arg-missing', `operation "${op.op}" is missing required argument "${key}"`);
      }
      continue;
    }
    if (!rule.valid(args[key])) {
      return fail('operation-arg-invalid', `operation "${op.op}" argument "${key}" is invalid`);
    }
  }
  return { ok: true };
}

function fail(code, reason) {
  return { ok: false, code, reason };
}
