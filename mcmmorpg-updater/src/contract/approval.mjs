// Root-owned, single-use approval record with MAC integrity, TTL and nonce.
//
// An approval is NOT the plan hash by itself. It is a signed (HMAC) record that
// binds a specific plan id + content hash to a one-time authorization that:
//   - can only be minted by something holding the shared secret key (anti-spoof),
//   - is declared root-owned (ownerUid === 0) and can be re-checked against the
//     backing store's real owner via an injected `verifyOwner` probe,
//   - expires after `ttlSeconds`,
//   - carries a unique `nonce` that a single-use store consumes exactly once
//     (anti-replay).
//
// The MAC covers the whole record except the `mac` field itself, so tampering
// with any bound field (planId, planHash, ttl, nonce, ownerUid, issuedAt)
// invalidates it.

import { createHmac, timingSafeEqual, randomBytes } from 'node:crypto';
import { canonicalize } from './canonical.mjs';

export const APPROVAL_OWNER_UID_ROOT = 0;

export class NonceAlreadyUsedError extends Error {
  constructor(nonce) {
    super(`nonce already consumed: ${nonce}`);
    this.name = 'NonceAlreadyUsedError';
    this.nonce = nonce;
  }
}

/**
 * Minimal single-use nonce store. `consume` is the atomic commit point:
 * consuming a nonce a second time throws. Swap this for a durable, fsync-backed
 * implementation in the real executor; the contract only depends on this shape.
 */
export class InMemoryNonceStore {
  #used = new Set();

  consume(nonce) {
    if (typeof nonce !== 'string' || nonce.length === 0) {
      throw new TypeError('nonce must be a non-empty string');
    }
    if (this.#used.has(nonce)) {
      throw new NonceAlreadyUsedError(nonce);
    }
    this.#used.add(nonce);
  }

  has(nonce) {
    return this.#used.has(nonce);
  }
}

function macInput(record) {
  // Everything except the mac field participates in the MAC.
  const { mac, ...rest } = record;
  return canonicalize(rest);
}

/**
 * Mint an approval record for a validated plan. `secretKey` is the shared HMAC
 * key held only by the approving (root) party.
 */
export function createApproval({
  plan,
  operator,
  ttlSeconds,
  secretKey,
  issuedAt = Date.now(),
  nonce = randomBytes(16).toString('hex'),
  ownerUid = APPROVAL_OWNER_UID_ROOT,
}) {
  if (!plan || typeof plan.id !== 'string' || typeof plan.hash !== 'string') {
    throw new TypeError('createApproval requires a plan with id and hash');
  }
  if (typeof operator !== 'string' || operator.length === 0) {
    throw new TypeError('operator must be a non-empty string');
  }
  if (!Number.isInteger(ttlSeconds) || ttlSeconds <= 0) {
    throw new TypeError('ttlSeconds must be a positive integer');
  }
  requireKey(secretKey);

  const record = {
    planId: plan.id,
    planHash: plan.hash,
    operator,
    issuedAt,
    ttlSeconds,
    nonce,
    ownerUid,
  };
  const mac = createHmac('sha256', secretKey).update(macInput(record)).digest('hex');
  return { ...record, mac };
}

/**
 * Verify an approval against the plan it should authorize.
 * @returns {{ ok: true } | { ok: false, code: string, reason: string }}
 */
export function verifyApproval(approval, { plan, secretKey, now = Date.now(), expectedOwnerUid = APPROVAL_OWNER_UID_ROOT, verifyOwner } = {}) {
  requireKey(secretKey);
  if (approval === null || approval === undefined) {
    return no('approval-missing', 'no approval record provided');
  }
  if (typeof approval !== 'object' || Array.isArray(approval)) {
    return no('approval-malformed', 'approval must be an object');
  }
  for (const field of ['planId', 'planHash', 'operator', 'nonce', 'mac']) {
    if (typeof approval[field] !== 'string' || approval[field].length === 0) {
      return no('approval-malformed', `approval.${field} must be a non-empty string`);
    }
  }
  if (!Number.isInteger(approval.issuedAt) || !Number.isInteger(approval.ttlSeconds) || approval.ttlSeconds <= 0) {
    return no('approval-malformed', 'approval.issuedAt/ttlSeconds must be valid integers');
  }
  if (!Number.isInteger(approval.ownerUid)) {
    return no('approval-malformed', 'approval.ownerUid must be an integer');
  }

  // 1. Integrity / anti-spoof: recompute the MAC and compare in constant time.
  const expectedMac = createHmac('sha256', secretKey).update(macInput(approval)).digest('hex');
  if (!constantTimeEqualHex(approval.mac, expectedMac)) {
    return no('approval-mac-invalid', 'approval MAC does not verify (spoofed or tampered)');
  }

  // 2. Binding: the approval must name this exact plan and content hash. A plan
  //    modified after approval changes plan.hash and trips this check.
  if (approval.planId !== plan?.id) {
    return no('approval-plan-mismatch', 'approval is for a different plan id');
  }
  if (approval.planHash !== plan?.hash) {
    return no('approval-hash-mismatch', 'approval does not match current plan hash (plan changed after approval)');
  }

  // 3. Root ownership.
  if (approval.ownerUid !== expectedOwnerUid) {
    return no('approval-not-root-owned', `approval must be owned by uid ${expectedOwnerUid}`);
  }
  if (typeof verifyOwner === 'function') {
    let owned = false;
    try {
      owned = verifyOwner(approval) === true;
    } catch (err) {
      return no('approval-owner-check-failed', `owner probe threw: ${err.message}`);
    }
    if (!owned) {
      return no('approval-owner-check-failed', 'approval backing store is not root-owned');
    }
  }

  // 4. TTL.
  if (now < approval.issuedAt) {
    return no('approval-not-yet-valid', 'approval issuedAt is in the future');
  }
  if (now - approval.issuedAt > approval.ttlSeconds * 1000) {
    return no('approval-expired', 'approval TTL has elapsed');
  }

  return { ok: true };
}

function requireKey(secretKey) {
  if (!(typeof secretKey === 'string' && secretKey.length > 0) && !Buffer.isBuffer(secretKey)) {
    throw new TypeError('secretKey must be a non-empty string or Buffer');
  }
}

function constantTimeEqualHex(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length) {
    return false;
  }
  const ba = Buffer.from(a, 'hex');
  const bb = Buffer.from(b, 'hex');
  if (ba.length !== bb.length || ba.length === 0) return false;
  return timingSafeEqual(ba, bb);
}

function no(code, reason) {
  return { ok: false, code, reason };
}
