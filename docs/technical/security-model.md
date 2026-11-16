# Domain 16 Security Model

contract_status: proposed
runtime_status: partially_verified
release_status: blocked
status_vocabulary_ref: docs/technical/status-and-evidence-vocabulary.md#canonical-status-and-evidence-rules

evidence_refs:
  - evidence_id: EVID-VELOCITY-CONFIG-2026-07-22
    source_type: config
    source_path: MCMMORPG/docker/velocity/velocity.toml
    observed_at: 2026-07-22
    scope: proxy bind, offline-mode posture, forwarding mode and backend routing
    summary: Velocity proxy is configured in offline-mode with modern forwarding and a shared forwarding secret file.
  - evidence_id: EVID-PAPER-PROXY-CONFIG-2026-07-22
    source_type: config
    source_path: MCMMORPG/config/paper-global.yml
    observed_at: 2026-07-22
    scope: Paper proxy settings and built-in packet limiter
    summary: Paper source config shows velocity proxy support fields and packet-limiter defaults before per-backend secret injection.
  - evidence_id: EVID-BUILD-SERVERS-FORWARDING-2026-07-22
    source_type: config
    source_path: MCMMORPG/docker/build-servers.sh
    observed_at: 2026-07-22
    scope: forwarding secret injection into backend Paper configs
    summary: Build script enables velocity forwarding and injects a shared secret into backend Paper config during server assembly.
  - evidence_id: EVID-SERVER-PROPERTIES-2026-07-22
    source_type: config
    source_path: MCMMORPG/server.properties
    observed_at: 2026-07-22
    scope: online-mode, RCON and built-in operator levels
    summary: Current server properties keep offline-mode, RCON and operator permission levels enabled.
  - evidence_id: EVID-OPS-JSON-2026-07-22
    source_type: config
    source_path: MCMMORPG/ops.json
    observed_at: 2026-07-22
    scope: explicit operator principals in the runtime tree
    summary: Runtime tree contains an operator list with multiple level-4 entries.
  - evidence_id: EVID-PERMISSIONS-YML-2026-07-22
    source_type: config
    source_path: MCMMORPG/permissions.yml
    observed_at: 2026-07-22
    scope: default permission grants outside plugin-managed permissions
    summary: Permissions file exists and grants several gameplay-facing permissions by default.
  - evidence_id: EVID-LUCKPERMS-CONFIG-2026-07-22
    source_type: config
    source_path: MCMMORPG/plugins/LuckPerms/config.yml
    observed_at: 2026-07-22
    scope: permissions backend defaults and sync behavior
    summary: LuckPerms is present but currently configured with local `h2` storage and no active scheduled sync.
  - evidence_id: EVID-PLATFORM-ARCH-2026-07-22
    source_type: document
    source_path: docs/technical/platform-architecture.md
    observed_at: 2026-07-22
    scope: runtime components, proxy/backend boundaries and evidence surfaces
    summary: Platform architecture is the canonical runtime topology input for this security model.
  - evidence_id: EVID-DEPENDENCY-GRAPH-2026-07-22
    source_type: document
    source_path: docs/technical/dependency-graph.md
    observed_at: 2026-07-22
    scope: packet middleware and proxy-adjacent plugin failure points
    summary: Dependency graph identifies packet and permissions middleware as shared failure points.

blockers:
  - blocker_id: BLK-SEC-001
    status_family: release_status
    summary: Current repo proves built-in proxy and packet-limit guardrails, but not a dedicated end-to-end anti-cheat system with evidence, exception and punishment workflow.
    required_evidence: Canonical anti-cheat implementation evidence or explicit documented decision to remain without dedicated anti-cheat for the reviewed scope.
    owner: domain-16
    scope: anti-cheat
  - blocker_id: BLK-SEC-002
    status_family: release_status
    summary: Secret surfaces are proven to exist, but no canonical secret-rotation or CI/CD secret-handling procedure is yet documented.
    required_evidence: Reviewed secret-management process and rotation policy.
    owner: domain-16
    scope: secrets
  - blocker_id: BLK-SEC-003
    status_family: runtime_status
    summary: Permissions authority is split across ops.json, permissions.yml and LuckPerms, but the repo does not yet prove the intended precedence and operational policy.
    required_evidence: Reviewed permission authority model with effective precedence rules.
    owner: domain-16
    scope: admin-surface

## Security Stance

The current runtime has a **proxy-mediated offline-mode posture**:

- Velocity runs in offline-mode for local MVP/dev-style access.
- Paper backends are assembled to trust Velocity modern forwarding through a shared secret.
- Built-in server operator powers and RCON remain active.
- LuckPerms exists, but local file-backed storage means permissions durability and cross-server sync are not yet proven to be centralized.

This is a workable dev/staging stance, not a finished production security posture.

## Actor / Capability / Boundary Matrix

| Actor | Capability | Boundary | Evidence source |
| --- | --- | --- | --- |
| Client / bot | connect via proxy | must enter through Velocity, not directly through backend trust assumptions | velocity config + compose |
| Velocity proxy | route sessions and forward player identity | trusted front-door for backend session identity | velocity config + build script |
| Backend Paper server | enforce built-in server rules, RCON and ops levels | trusts forwarding secret and local server/operator settings | paper config + server.properties + ops.json |
| LuckPerms | plugin-managed permissions surface | coexists with built-in ops and permissions.yml | LuckPerms config + permissions.yml |
| Docker operator | compose/env/volume control | controls DB creds, forwarding secret source, container lifecycle | compose + docker env |
| Domain 17 reviewer | consume evidence for release gates | should not inherit release truth from config presence alone | canonical status/evidence vocabulary |

## Administrative Surface

| Surface | Observed state | Security meaning | Status |
| --- | --- | --- | --- |
| `ops.json` | present with multiple level-4 operators | high-privilege built-in admin path exists | proven |
| `permissions.yml` | present with default grants | built-in permission grants exist outside LuckPerms | proven |
| LuckPerms | present, local `h2` storage | permission plugin exists, but not yet proven as sole central authority | partially verified |
| RCON | enabled in server properties and compose env | remote administrative command path exists | proven |
| Docker env and compose control | present | container and secret operator surface exists | proven |

## Proxy Trust Boundary

Observed trust boundary today:

1. Client connects to Velocity on `:25565`.
2. Velocity runs with `player-info-forwarding-mode = "modern"` and uses a forwarding secret file.
3. Backend Paper configs are rewritten during server assembly so Velocity forwarding is enabled and the shared secret is injected.
4. Backends run with `online-mode=false`, which makes the forwarding trust boundary load-bearing.

Implication:

- direct backend exposure or forwarding-secret drift would undermine identity trust quickly;
- this boundary is strong enough for documented dev/staging use, but still sensitive to secret handling and container exposure mistakes.

## Secret Surfaces

This document intentionally records **where** secrets live, not their values.

| Surface | Purpose | Observed location | Status |
| --- | --- | --- | --- |
| forwarding secret | Velocity-to-Paper trust boundary | `MCMMORPG/docker/.env`, `MCMMORPG/docker/velocity/forwarding.secret`, injected by build script | proven |
| RCON password | remote command path to backend containers | docker env + compose | proven |
| MariaDB user credentials | DB service access | docker env + compose | proven |
| CI/CD secret handling | deployment automation | not proven in current repo | blocked |

Rules:

- Secret values must never be copied into docs.
- Canonical docs should describe secret locations, consumers and rotation responsibilities only.
- Release claims stay blocked until `BLK-SEC-002` is cleared.

## Built-In Guardrails And Anti-Cheat Boundary

The repo currently proves **guardrails**, not a full anti-cheat stack:

| Surface | Proven today | Notes |
| --- | --- | --- |
| Paper packet limiter | yes | `packet-limiter` is configured in `paper-global.yml` with kick behavior and thresholds. |
| Paper anticheat/obfuscation section | yes | Built-in obfuscation/packet settings exist, but are not a full gameplay exception model. |
| Dedicated anti-cheat plugin | not proven | No dedicated anti-cheat component is proven by current inventory or configs. |
| Packet middleware | yes | `ProtocolLib` and `PacketEvents` are active shared runtime surfaces. |

### Anti-Cheat Contract

Domain 16 currently defines the **policy boundary**, not final gameplay exception content:

| Area | Current statement |
| --- | --- |
| detection | built-in packet limiting is proven; dedicated anti-cheat detection remains unproven |
| mitigation | packet limiter kick behavior is proven; broader mitigation workflow is not |
| evidence | runtime logs and packet/plugin failure evidence surfaces exist |
| automatic punishment | not canonically proven beyond packet-limiter kicks |
| manual review | not yet formalized in repo |
| exceptions | must be handled through an intake contract, not improvised in plugin-specific prose |

### Exception Intake Contract

For custom movement, mounts, skills and packet-facing systems:

- Domain 16 owns:
  - exception owner field
  - approval path
  - evidence path
  - labeling rules
- Gameplay domains own:
  - semantic justification for the exception
  - player-facing behavior
  - design intent

This prevents Domain 16 from silently owning gameplay mechanics while still controlling runtime risk.

## Database Access Boundary

Current DB access posture is mixed:

- docker runtime exposes MariaDB credentials and one shared DB service;
- LuckPerms is still configured for local `h2`;
- CMI and Guilds default to `sqlite`;
- MMO stack configs expose MySQL fields and table names, but still show MySQL disabled in visible configs.

Security implication:

- database credentials are a real secret surface even before the full plugin stack is cut over;
- not every plugin should automatically be modeled as an active MariaDB consumer yet;
- least-privilege DB access policy cannot be considered complete until the DB topology contract stabilizes actual consumers.

## Review Notes

- This file intentionally describes the current security boundary as **partially centralized and partly legacy/local**.
- It is sufficient for downstream recovery and deployment docs to reference, but not for a production-ready security claim.
