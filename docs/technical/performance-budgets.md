# Domain 16 Performance Budgets

contract_status: proposed
runtime_status: partially_verified
release_status: blocked
status_vocabulary_ref: docs/technical/status-and-evidence-vocabulary.md#canonical-status-and-evidence-rules

evidence_refs:
  - evidence_id: EVID-COMPOSE-LIMITS-2026-07-22
    source_type: config
    source_path: MCMMORPG/docker/compose.yml
    observed_at: 2026-07-22
    scope: container memory limits and backend runtime sizing
    summary: Compose defines distinct init/max memory and container mem_limit values for MariaDB, Velocity and backend containers.
  - evidence_id: EVID-SERVER-PROPERTIES-2026-07-22
    source_type: config
    source_path: MCMMORPG/server.properties
    observed_at: 2026-07-22
    scope: max players, simulation distance and network-compression defaults
    summary: Server properties set `max-players=20`, `simulation-distance=6` and `network-compression-threshold=256`.
  - evidence_id: EVID-PAPER-PACKET-LIMITER-2026-07-22
    source_type: config
    source_path: MCMMORPG/config/paper-global.yml
    observed_at: 2026-07-22
    scope: built-in packet-limiter thresholds
    summary: Paper global config sets packet limiter kick behavior and packet-rate thresholds.
  - evidence_id: EVID-START-HERE-DOCKER-2026-07-22
    source_type: document
    source_path: MCMMORPG/START_HERE_DOCKER.md
    observed_at: 2026-07-22
    scope: runtime operational constraints for heavy backends and QA load isolation
    summary: Docker start guide warns against running too many heavy backends at once and describes isolated QA expectations.
  - evidence_id: EVID-DEPENDENCY-GRAPH-2026-07-22
    source_type: document
    source_path: docs/technical/dependency-graph.md
    observed_at: 2026-07-22
    scope: shared failure points for packet middleware and world/MMO stacks
    summary: Dependency graph identifies shared middleware and high-blast-radius plugin groups.

blockers:
  - blocker_id: BLK-PERF-001
    status_family: runtime_status
    summary: No canonical staging or production MSPT baseline is currently attached to Domain 16 evidence.
    required_evidence: Named runtime measurement showing steady-state and stressed MSPT for the reviewed backend slice.
    owner: domain-16
    scope: mspt-budget
  - blocker_id: BLK-PERF-002
    status_family: release_status
    summary: Current repo proves configured memory ceilings, but not runtime-safe concurrency for all backend combinations.
    required_evidence: Controlled load evidence for the intended backend combination or an explicit operational restriction.
    owner: domain-16
    scope: backend-concurrency
  - blocker_id: BLK-PERF-003
    status_family: release_status
    summary: No canonical packet-traffic or chunk-loading baseline is recorded for production-like flows.
    required_evidence: Runtime evidence for packet pressure and chunk-loading behavior on the intended slice.
    owner: domain-16
    scope: packet-and-chunk-baselines

## Budget Model

This document records two kinds of budgets:

- **configured ceilings**: values already proven by repo configuration;
- **runtime budgets**: values that should gate release claims, but remain blocked until measurement evidence exists.

Configured ceilings are not automatically release-safe budgets.

## Resource Budget Table

| Budget | Current threshold | Evidence source | Gate consequence |
| --- | --- | --- | --- |
| backend heap: `srv-world` | `INIT_MEMORY=1G`, `MAX_MEMORY=4G`, `mem_limit=5g` | compose | If exceeded or OOM-prone, world slice claims stay blocked. |
| backend heap: `srv-items` | `INIT_MEMORY=1G`, `MAX_MEMORY=2G`, `mem_limit=2600m` | compose | Items slice load or UI/progression validations stay blocked if memory pressure is unresolved. |
| backend heap: `srv-hub` | `INIT_MEMORY=512M`, `MAX_MEMORY=1536M`, `mem_limit=2g` | compose | Hub/admin slice remains conditional if hub memory spikes break rollback confidence. |
| backend heap: `srv-minioncore` | `INIT_MEMORY=512M`, `MAX_MEMORY=1536M`, `mem_limit=2g` | compose | Auxiliary runtime claims remain limited to this observed ceiling. |
| proxy heap: `velocity` | `mem_limit=768m` | compose | Proxy release claims blocked if proxy stability under expected session load is unproven. |
| database heap: `mariadb` | `mem_limit=512m` | compose | DB-backed claims blocked if service health or connection churn exceeds this ceiling. |
| player concurrency | `max-players=20` by default backend baseline; `srv-minioncore` overrides to `70` | server.properties + compose | Higher concurrency claims require separate evidence. |
| simulation distance | `6` | server.properties | World/performance claims beyond this baseline are out of scope until revalidated. |
| network compression threshold | `256` | server.properties | Packet-cost assumptions must be reviewed if this changes. |
| packet limiter: all packets | `max-packet-rate=500`, `action=KICK`, `interval=7s` | paper-global.yml | Packet-heavy runtime paths remain blocked if normal gameplay exceeds limiter. |
| packet limiter: incoming packets | `incoming-packet-threshold=300` | paper-global.yml | Packet middleware or bot profiles must not assume higher safe burst without evidence. |
| MSPT steady-state | **not yet proven** | blocked by `BLK-PERF-001` | No release-safe performance claim may treat MSPT as green. |
| CPU utilization | **not yet proven** | blocked by missing runtime measurements | Production-like concurrency claims remain blocked. |
| chunk-generation throughput | **not yet proven** | blocked by `BLK-PERF-003` | Pregeneration and world rollout remain conditional. |

## Backend Concurrency Rules

Observed operational rule from current repo:

- do not run more heavy backends than necessary;
- current docs explicitly warn that the stack can run into OOM at around `7 GB` when too many heavy backends are active together;
- plugin/load QA should use an isolated backend with the minimum dependency set and its own `/data`.

Current Domain 16 rule:

| Scenario | Current stance | Status |
| --- | --- | --- |
| `world` alone | allowed as observed runtime slice | partially verified |
| `items` alone | allowed as observed runtime slice | partially verified |
| `hub` alone | allowed as observed runtime slice | partially verified |
| `world + items + hub` together | operationally sensitive; no release-safe concurrency proof | blocked |
| plugin load/QA on shared gameplay backend | disallowed by current operational guidance | proven policy |

## Pregeneration Policy

`Chunky` is present in the runtime inventory baseline, but pregeneration remains a governed operation rather than an open-ended background task.

Rules:

- pregeneration belongs on the world-facing runtime slice or a dedicated QA/backend isolation surface;
- do not combine pregeneration with unrelated heavy backend tests unless evidence proves the combined load is safe;
- pregeneration changes that alter chunk-loading pressure must be reviewed against current memory ceilings and missing MSPT baseline.

Pregeneration-specific status:

| Area | Current state | Status |
| --- | --- | --- |
| pregeneration tool present | `Chunky-Bukkit 1.4.40` is in active runtime inventory | proven |
| pregeneration throughput budget | not yet measured canonically | blocked |
| pregeneration rollback expectation | must be tied to recovery/runbook docs, not assumed | partially verified |

## Failure Mapping

| Failure point | First break | Related budget |
| --- | --- | --- |
| `world` slice memory saturation | OOM or degraded PvE/world systems | `srv-world` heap ceiling |
| packet middleware overload | disconnects or degraded packet-facing behavior | packet limiter thresholds |
| DB service pressure | persistence-backed systems stall or fail | MariaDB memory ceiling |
| chunk-heavy world operations | world slice stalls without a measured MSPT baseline | MSPT + chunk-generation baseline |

## Review Notes

- This document is intentionally conservative: where runtime measurement is missing, the budget stays blocked instead of being invented.
- Downstream recovery and deployment docs may consume these ceilings as preconditions, but not as proof of healthy runtime under load.
