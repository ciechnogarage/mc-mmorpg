# Domain 16 Database Topology

contract_status: proposed
runtime_status: partially_verified
release_status: blocked
status_vocabulary_ref: docs/technical/status-and-evidence-vocabulary.md#canonical-status-and-evidence-rules

evidence_refs:
  - evidence_id: EVID-COMPOSE-DB-2026-07-22
    source_type: config
    source_path: MCMMORPG/docker/compose.yml
    observed_at: 2026-07-22
    scope: shared MariaDB service, healthcheck and durable volume
    summary: Docker compose defines one MariaDB 11.8 service backed by the `mariadb_data` volume.
  - evidence_id: EVID-DOCKER-ENV-2026-07-22
    source_type: config
    source_path: MCMMORPG/docker/.env
    observed_at: 2026-07-22
    scope: secret-backed runtime DB credentials exist for docker stack
    summary: Docker environment file provides MariaDB credential variables, but values are intentionally not restated here.
  - evidence_id: EVID-LUCKPERMS-STORAGE-2026-07-22
    source_type: config
    source_path: MCMMORPG/plugins/LuckPerms/config.yml
    observed_at: 2026-07-22
    scope: LuckPerms storage backend and table prefix defaults
    summary: LuckPerms is configured for `storage-method: h2`; remote SQL settings are present but not active.
  - evidence_id: EVID-CMI-STORAGE-2026-07-22
    source_type: config
    source_path: MCMMORPG/plugins/CMI/Settings/DataBaseInfo.yml
    observed_at: 2026-07-22
    scope: CMI storage backend and SQL naming defaults
    summary: CMI currently defaults to `method: sqlite` with a `CMI_` table prefix for optional MySQL mode.
  - evidence_id: EVID-GUILDS-STORAGE-2026-07-22
    source_type: config
    source_path: MCMMORPG/plugins/Guilds/config.yml
    observed_at: 2026-07-22
    scope: Guilds storage backend defaults
    summary: Guilds currently defaults to `database.type: sqlite`; remote SQL parameters are present but inactive.
  - evidence_id: EVID-MMO-STACK-STORAGE-2026-07-22
    source_type: config
    source_path: MCMMORPG/plugins/MMOCore/config.yml
    observed_at: 2026-07-22
    scope: representative MMO stack MySQL defaults and table names
    summary: MMOCore, MMOItems, MMOInventory and MMOProfiles expose MySQL blocks and table names, but `mysql.enabled: false`.
  - evidence_id: EVID-RUNTIME-LAYOUT-2026-07-22
    source_type: document
    source_path: docs/technical/platform-architecture.md
    observed_at: 2026-07-22
    scope: runtime boundaries for MariaDB, backend `/data`, logs and cache surfaces
    summary: Platform architecture defines shared MariaDB plus backend-local runtime state.

blockers:
  - blocker_id: BLK-DB-001
    status_family: release_status
    summary: Current repository proves a shared MariaDB service exists, but not that the whole runtime stack is already using it as one durable source of truth.
    required_evidence: Per-plugin runtime validation or canonical config showing which plugins are actually pointed at MariaDB in the active backend assembly.
    owner: domain-16
    scope: storage-authority
  - blocker_id: BLK-DB-002
    status_family: runtime_status
    summary: Cache surfaces are not yet described by one canonical runtime contract; only broad runtime directories are proven.
    required_evidence: Cache inventory with named entities, TTL, invalidation and restart behavior.
    owner: domain-16
    scope: cache-contract
  - blocker_id: BLK-DB-003
    status_family: release_status
    summary: Connection-pool and recovery behavior are only partially proven from defaults; active staging behavior is not yet runtime-verified.
    required_evidence: Runtime DB evidence for active pool usage, reconnect behavior and restore validation.
    owner: domain-16
    scope: connection-pooling-and-recovery

## Topology Summary

The current repo proves a **mixed storage topology**, not a finished single-source MariaDB architecture:

- the docker runtime exposes one shared `MariaDB 11.8` service with one durable volume;
- several plugins still default to local file-backed storage (`h2` or `sqlite`);
- the MMO stack exposes MySQL blocks and canonical table names, but the visible configs still show MySQL disabled;
- backend `/data` trees remain important runtime state independent of database durability.

This document therefore distinguishes **observed storage defaults** from **target runtime intent**.

## Storage Classes

| Class | Meaning | Current status |
| --- | --- | --- |
| `shared_mariadb` | One docker-managed MariaDB service intended for shared runtime persistence. | proven |
| `local_file_storage` | Plugin-local storage such as `h2` or `sqlite` inside backend/runtime directories. | proven |
| `backend_runtime_state` | `/data`, worlds, local plugin files, generated artifacts. | proven |
| `cache_or_ephemeral_runtime` | Auxiliary state that may be rebuilt or refreshed. | partially verified |

## Observed Storage Components

| Component | Kind | Source of truth | Current evidence | Notes |
| --- | --- | --- | --- | --- |
| `mariadb` service | shared DB service | `MCMMORPG/docker/compose.yml` | proven | Backed by `mariadb_data` volume and healthcheck. |
| `mariadb_data` volume | durable DB volume | docker runtime | proven | Compose defines the persistent volume, not the table-level ownership map. |
| backend `/data` trees | runtime-local state | `MCMMORPG/docker/servers/<backend>` | proven | Built from live repo state by `build-servers.sh`. |
| `MCMMORPG/world*` | world runtime state | repo runtime tree | proven | World data is runtime state, not design source. |
| `MCMMORPG/logs/` | evidence surface | runtime logs | proven | Needed for runtime and release evidence, not durable business state. |
| `MCMMORPG/cache/` | cache surface | repo runtime tree | partially verified | Directory exists, but current cache semantics are not yet canonical. |

## Plugin Storage Matrix

| Plugin / group | Observed storage mode | Canonical table or namespace | Source of truth today | Status |
| --- | --- | --- | --- | --- |
| LuckPerms | `h2` | `luckperms_` prefix only for remote SQL mode | local file-backed plugin storage | proven |
| CMI | `sqlite` | `CMI_` table prefix if remote SQL is later enabled | local plugin storage | proven |
| Guilds | `sqlite` | `guilds`, `players`, `ranks` table names in config | local plugin storage | proven |
| MMOCore | `mysql.enabled: false` | `mmocore_playerdata` | unresolved between local/default and target shared DB intent | partially verified |
| MMOItems | `mysql.enabled: false` | `mmoitems_playerdata` | unresolved between local/default and target shared DB intent | partially verified |
| MMOInventory | `mysql.enabled: false` | `mmoinventory_inventories` | unresolved between local/default and target shared DB intent | partially verified |
| MMOProfiles | `mysql.enabled: false` | `mmoprofiles_playerdata` | unresolved between local/default and target shared DB intent | partially verified |
| HuskSync | disabled runtime blocker | not active in runtime baseline | disabled plugin path | blocked |

## MariaDB Service

| Field | Observed state | Evidence |
| --- | --- | --- |
| engine | MariaDB 11.8 container | compose |
| network | `mcnet` | compose |
| durable backing | `mariadb_data` volume | compose |
| liveness | `mariadb-admin ping` healthcheck | compose |
| credentials | provided through docker env vars | docker `.env` and compose |

Rules for this document:

- Secret **values** are intentionally omitted.
- The existence of credentials does not prove that every plugin is currently consuming shared MariaDB.
- Later release claims must point to runtime evidence, not just the presence of a container.

## Connection Pooling And Access

Current evidence shows three distinct patterns:

| Pattern | Where observed | Meaning | Status |
| --- | --- | --- | --- |
| explicit connection-pool settings for optional MySQL mode | LuckPerms and MMO stack configs | Plugins are ready for pooled SQL usage if MySQL is enabled | partially verified |
| local file storage active today | LuckPerms `h2`, CMI `sqlite`, Guilds `sqlite` | Runtime may currently avoid the shared MariaDB service for these plugins | proven |
| shared service exists independently of plugin activation state | docker compose | Infrastructure path exists even where plugin-level cutover is incomplete | proven |

## Cache Contract

The current repo does **not** yet prove a complete cache inventory. The canonical contract below therefore records what is known and leaves the rest blocked.

| Field | Current contract |
| --- | --- |
| `cached_entities` | not yet canonical; at minimum includes runtime-adjacent rebuildable state outside durable worlds and DB volumes |
| `source_of_truth` | durable sources are MariaDB service, backend `/data` trees where plugins still use local storage, and world/runtime state where applicable |
| `ttl` | not proven |
| `invalidation` | not proven |
| `restart_behavior` | partially proven: backend-local runtime state is rebuilt by docker/server assembly; cache-specific behavior is not yet canonical |
| `stale_detection` | not proven |
| `failure_safety` | partially verified only; no canonical proof yet that every cache loss is safe |

This intentionally leaves `BLK-DB-002` open instead of inventing semantics.

## Recovery Assumptions

These are storage assumptions downstream recovery work may consume:

- MariaDB durability depends on the `mariadb_data` volume, not on backend `/data` trees alone.
- Plugins still using `h2` or `sqlite` introduce backend-local recovery scope even if a shared MariaDB service exists.
- World folders and backend `/data` trees remain recovery-relevant runtime state.
- Cache recovery semantics are not yet strong enough to support `release_status: approved`.

## Review Notes

- This file is the current source for **observed storage topology**, not proof that the runtime has already completed a DB unification program.
- Any later security or recovery document must reference this mixed-state reality rather than assume "everything is already on MariaDB".
