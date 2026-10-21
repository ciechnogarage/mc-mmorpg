// Per-target execution lock abstraction.
//
// Only one execution may touch a given target at a time. The contract depends on
// this shape: `acquire(target)` returns a handle with `release()`, and acquiring
// a target that is already held throws `TargetLockedError`. Different targets do
// not contend. The in-memory implementation is sufficient for the contract and
// its tests; the real executor substitutes a filesystem/flock-backed lock with
// the same interface.

export class TargetLockedError extends Error {
  constructor(target) {
    super(`target is already locked: ${target}`);
    this.name = 'TargetLockedError';
    this.target = target;
  }
}

export class InMemoryLockManager {
  #held = new Set();

  /**
   * @returns {{ target: string, release: () => void }}
   * @throws {TargetLockedError} if the target is currently held.
   */
  acquire(target) {
    if (typeof target !== 'string' || target.length === 0) {
      throw new TypeError('target must be a non-empty string');
    }
    if (this.#held.has(target)) {
      throw new TargetLockedError(target);
    }
    this.#held.add(target);
    let released = false;
    return {
      target,
      release: () => {
        if (released) return;
        released = true;
        this.#held.delete(target);
      },
    };
  }

  isLocked(target) {
    return this.#held.has(target);
  }
}

/**
 * Acquire locks for many targets, releasing any already taken if one fails, so
 * acquisition is all-or-nothing.
 * @returns {{ release: () => void }} aggregate handle releasing every lock.
 */
export function acquireAll(lockManager, targets) {
  const handles = [];
  try {
    for (const target of targets) {
      handles.push(lockManager.acquire(target));
    }
  } catch (err) {
    for (const h of handles) h.release();
    throw err;
  }
  return {
    handles,
    release: () => {
      for (const h of handles) h.release();
    },
  };
}
