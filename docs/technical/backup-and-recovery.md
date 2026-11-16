# Domain 16 Backup And Recovery

contract_status: proposed
runtime_status: partially_verified
release_status: blocked
status_vocabulary_ref: docs/technical/status-and-evidence-vocabulary.md#canonical-status-and-evidence-rules

evidence_refs:
  - evidence_id: EVID-DB-TOPOLOGY-2026-07-22
    source_type: document
    source_path: docs/technical/database-topology.md
    observed_at: 2026-07-22
    scope: mixed storage topology and recovery-relevant runtime surfaces
    summary: Database topology proves shared MariaDB plus backend-local storage defaults and cache uncertainty.
  - evidence_id: EVID-SECURITY-MODEL-2026-07-22
    source_type: document
    source_path: docs/technical/security-model.md
    observed_at: 2026-07-22
    scope: secret surfaces, admin paths, and proxy trust boundary
    summary: Security model proves secret locations and administrative surfaces without claiming production-ready hardening.
  - evidence_id: EVID-COMPOSE-DB-2026-07-22
    source_type: config
    source_path: MCMMORPG/docker/compose.yml
    observed_at: 2026-07-22
    scope: MariaDB service, volume, and backend runtime containers
    summary: Compose file defines the DB service and backend runtime mounts.
  - evidence_id: EVID-BACKUP-ARTIFACT-2026-07-22
    source_type: inventory
    source_path: MCMMORPG/backups/level_1_template_20260705_184530.tgz
    observed_at: 2026-07-22
    scope: presence of at least one backup-like runtime artifact
    summary: Repository contains one template archive under `MCMMORPG/backups`, but no canonical restore procedure is attached.
  - evidence_id: EVID-RECOVERY-DIR-2026-07-22
    source_type: inventory
    source_path: MCMMORPG/recovery
    observed_at: 2026-07-22
    scope: recovery assets directory presence
    summary: No `MCMMORPG/recovery` directory is present in the current runtime tree.
  - evidence_id: EVID-START-HERE-DOCKER-2026-07-22
    source_type: document
    source_path: MCMMORPG/START_HERE_DOCKER.md
    observed_at: 2026-07-22
    scope: runtime operations and rollback-sensitive QA constraints
    summary: Docker guide requires isolated QA, known rollback, and minimal heavy-backend concurrency.
  - evidence_id: EVID-IRIS-VENDOR-NOTE-2026-06-24
    source_type: document
    source_path: docs/plugin_manifest.yaml
    observed_at: 2026-06-24
    scope: Iris vendor-content restore prerequisite
    summary: Legacy manifest notes that vendor Iris packs may be git-ignored and require restore handling outside repo-tracked configs.

blockers:
  - blocker_id: BLK-RECOVERY-001
    status_family: release_status
    summary: No tested restore procedure is currently attached to the runtime baseline.
    required_evidence: Named restore test result that covers current DB plus backend/runtime state scope.
    owner: domain-16
    scope: restore-test
  - blocker_id: BLK-RECOVERY-002
    status_family: release_status
    summary: Recovery scope is mixed across MariaDB, backend `/data`, local plugin storage, vendor assets, and worlds, but no canonical end-to-end recovery drill is documented.
    required_evidence: Reviewed recovery runbook or drill proving the intended scope can be brought back coherently.
    owner: domain-16
    scope: end-to-end-recoverability
  - blocker_id: BLK-RECOVERY-003
    status_family: runtime_status
    summary: Cache recovery semantics are still incomplete, so cache loss safety cannot yet be treated as proven.
    required_evidence: Canonical cache contract with restart behavior and failure safety.
    owner: domain-16
    scope: cache-recovery

## Recoverability Scope

This document is the canonical source of truth for restore and disaster-recovery gate semantics in Domain 16.

Current recovery scope must treat all of these as load-bearing:

- `mariadb_data` volume
- backend `/data` trees under `MCMMORPG/docker/servers/<backend>`
- runtime-local plugin storage still using `h2` or `sqlite`
- world folders and other runtime world state
- non-repo vendor assets needed for runtime parity, such as Iris vendor packs under `plugins/Iris`
- disabled blockers inventory when it affects release truth

## Recovery Asset Inventory

| Surface | Observed state | Recovery meaning | Status |
| --- | --- | --- | --- |
| `mariadb_data` volume | present in compose | durable DB asset requiring backup and restore path | proven |
| backend `/data` trees | present via compose mounts | backend runtime state requiring snapshot or rebuild strategy | proven |
| `MCMMORPG/world*` | present in runtime tree | world-state recovery scope | proven |
| `MCMMORPG/plugins/Iris/packs/overworld` and similar vendor content | legacy docs note some vendor packs may be git-ignored | restore scope may be incomplete even when repo-tracked configs are restored | partially verified |
| `MCMMORPG/backups/level_1_template_20260705_184530.tgz` | one backup-like artifact present | proves backup artifacts exist, not that restore is documented or tested | partially verified |
| `MCMMORPG/recovery` | absent | no canonical recovery asset directory exists today | blocked |

## Recovery Gate Table

| Recoverability gate | Required evidence | Blocker when missing |
| --- | --- | --- |
| `DB_RESTORE_SCOPE_DEFINED` | named mapping of MariaDB assets and plugin-local DB/file state in recovery scope | `BLK-RECOVERY-002` |
| `DB_RESTORE_TESTED` | restore test result for current DB scope | `BLK-RECOVERY-001` |
| `BACKEND_DATA_RESTORE_DEFINED` | documented recovery treatment for backend `/data` trees and world state | `BLK-RECOVERY-002` |
| `VENDOR_ASSET_RESTORE_DEFINED` | documented treatment for non-repo vendor assets required by active runtime slices | `BLK-RECOVERY-002` |
| `CACHE_LOSS_POLICY_DEFINED` | cache restart/failure-safety contract | `BLK-RECOVERY-003` |
| `SECRETS_REENTRY_DEFINED` | secret and forwarding-sensitive components can be reintroduced after restore | `BLK-RECOVERY-002` |

## Restore Preconditions

Before a restore can be treated as reviewable:

1. The exact runtime slice must be named: staging, dev backend, or a narrower isolated QA backend.
2. Recovery scope must explicitly include which of these are in play:
   - MariaDB volume
   - backend `/data`
   - local plugin storage defaults
   - world state
   - non-repo vendor assets if the slice depends on them
   - disabled blockers inventory if relevant
3. Recovery operator must know which secret surfaces must still match post-restore:
   - forwarding trust boundary
   - DB credentials
   - RCON path if used for checks
4. Recovery test must record evidence refs instead of only prose.

## Disaster Recovery Assumptions

Current assumptions downstream docs may consume:

- restoring only the MariaDB volume is insufficient if the active runtime still relies on local `h2` or `sqlite` plugin storage
- restoring only backend `/data` is insufficient if the MariaDB volume or world state has diverged
- restoring repo-tracked configs is insufficient if active runtime behavior still depends on git-ignored vendor assets such as Iris packs
- cache state cannot yet be assumed safely rebuildable for release proof because the cache contract is incomplete
- a missing or stale forwarding secret after restore is a security and availability issue, not cosmetic config drift

## Minimal Recovery Scenarios

| Scenario | Covered today | Notes |
| --- | --- | --- |
| recover one archived template artifact | partially verified | One template archive exists, but no tested restore walkthrough is attached. |
| recover MariaDB-backed staging persistence | blocked | No tested restore evidence is attached. |
| recover a mixed local-file + MariaDB backend slice | blocked | Mixed storage topology increases recovery scope and is not yet drilled. |
| recover proxy + backend trust boundary after restore | blocked | Secret/forwarding re-entry is not yet documented as a tested flow. |

## Recovery Rules

- No document may call restore capability `verified` without a named restore test result.
- Recovery claims must cover both durable DB scope and backend-local runtime state when those stores are active together.
- Downstream deployment docs may reference these recovery gates as preconditions, but must not redefine them.

## Review Notes

- Current repo state is enough to prove that recovery is a real concern with mixed assets.
- It is not enough to prove that restore or DR is operationally ready.
