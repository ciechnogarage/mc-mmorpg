# Content Lifecycle

## Stage Flow

`draft -> schema_validated -> contract_approved -> runtime_loaded -> runtime_verified -> release_candidate -> released`

## Promotion Rules

- Promotion is blocked when any required evidence record is missing.
- Promotion is blocked when any required evidence record has `verdict: stale`.
- Promotion is blocked when evidence digests no longer match the current subject, dependency, runtime, or build identity.
- `runtime_loaded` requires matching `generated_projection_digest`, `committed_target_digest`, and `effective_runtime_digest`.
- `runtime_verified` requires matching `effective_runtime_digest` for the same build that passed `runtime_loaded`.

## Invalidation Rules By Gate

### `schema_validated`

Becomes stale when:

- source file changes
- subject digest changes
- schema version changes
- template version changes

### `contract_approved`

Becomes stale when:

- source file changes
- subject digest changes
- dependency digest changes
- governing contract changes

### `runtime_loaded`

Becomes stale when:

- source file changes
- subject digest changes
- dependency digest changes
- generated projection digest changes
- committed target digest changes
- effective runtime digest changes
- staging build changes
- allowed normalization contract changes

### `runtime_verified`

Becomes stale when:

- source file changes
- subject digest changes
- dependency digest changes
- effective runtime digest changes
- staging build changes
- E2E scenario changes

### `release_candidate`

Becomes stale when:

- subject digest changes
- dependency digest changes
- effective runtime digest changes
- rollback target changes
- migration record changes
- performance budget contract changes

### `released`

Becomes stale when:

- release manifest changes
- deploy evidence changes
- content is rolled back to a prior release

## Evidence Handling

- Stale evidence is retained for audit history.
- Stale evidence must not be overwritten in place.
- A fresh promotion requires a new evidence record for the current identity tuple.

## Notes

- This document defines lifecycle promotion and invalidation only.
- Runtime audit implementation, Mineflayer scenarios, and release manifests are tracked in separate tickets.
