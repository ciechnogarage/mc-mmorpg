// Deterministic canonicalization and hashing for the update-plan contract.
//
// The plan hash must be reproducible byte-for-byte from the plan content alone,
// independent of key ordering or insignificant whitespace, so that the executor
// can decide whether an approved plan is unchanged. We therefore serialize with
// recursively sorted object keys and no whitespace, then SHA-256 the result.

import { createHash } from 'node:crypto';

/**
 * Produce a canonical JSON string: object keys sorted lexicographically at every
 * depth, `undefined` members dropped, arrays preserved in order. Rejects values
 * that cannot hash deterministically (non-finite numbers, functions, symbols,
 * bigint) so a plan can never carry a silently non-reproducible field.
 */
export function canonicalize(value) {
  return JSON.stringify(normalize(value));
}

function normalize(value) {
  if (value === null) return null;
  const type = typeof value;
  if (type === 'string' || type === 'boolean') return value;
  if (type === 'number') {
    if (!Number.isFinite(value)) {
      throw new TypeError(`non-finite number is not canonicalizable: ${value}`);
    }
    return value;
  }
  if (Array.isArray(value)) {
    return value.map(normalize);
  }
  if (type === 'object') {
    const out = {};
    for (const key of Object.keys(value).sort()) {
      const member = value[key];
      if (member === undefined) continue;
      out[key] = normalize(member);
    }
    return out;
  }
  throw new TypeError(`value of type "${type}" is not canonicalizable`);
}

export function sha256Hex(input) {
  return createHash('sha256').update(input, 'utf8').digest('hex');
}

/** Canonical SHA-256 of an arbitrary JSON-like value. */
export function canonicalHash(value) {
  return sha256Hex(canonicalize(value));
}
