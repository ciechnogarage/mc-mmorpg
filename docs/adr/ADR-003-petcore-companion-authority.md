# ADR-003: PetCore as companion authority

- contract_status: accepted
- runtime_status: blocked
- release_status: blocked
- Date: 2026-07-20
- Scope: all companion/pet policies (Global Pet, Beast Companion, Summoned
  Minions, Adventurer Companion); platform gate only, not gameplay delivery
- Supersedes: `ADR-002-mythicmobs-companion-runtime.md`

## Context

ADR-002 assigned character-scoped companion authority to `CompanionCore`, a
single-policy tank companion MVP. Separately, `MCPets` has been running as an
unmanaged, config-only pet plugin: its `MySQL` block points at `localhost`
outside the Docker network, no `mcpets_db` database exists on the staging
MariaDB, and it operates on the undocumented YAML fallback path recorded as
`pet_progression: yaml_fallback_only_staging_limits_unverified` in the prior
authority registry. Neither plugin implements the full companion product
scope (Global Pet, Beast Companion, Summoned Minions, Adventurer Companion)
defined in `petcore-unified-companion-runtime-tech-plan`, and having two
undeclared candidate authorities (`CompanionCore`, `MCPets`) for overlapping
companion concepts violates the one-durable-authority rule in
`ADR-001-player-data-authority.md`.

## Decision

`PetCore` is the single durable authority for companion profiles, runtime
state, lifecycle and policy across all four companion policies (`GLOBAL_PET`,
`BEAST_COMPANION`, `SUMMONED_MINION`, `ADVENTURER_COMPANION`), as specified in
`docs/contracts/petcore-identity-and-content-run-contract.yaml` (the
repository-normative copy; the accepted Traycer planning artifact
`petcore-unified-companion-runtime-tech-plan/index.md` and its sibling
`identity-and-content-run-contract/index.md` remain valid planning context
but are not a dependency for this ADR's normative rules).
MythicMobs remains the executor for every companion's physical entity,
navigation, animation and skills, invoked only through a narrow
`MythicMobsAdapter`; PetCore never calls MythicMobs APIs directly.

`PetSubjectId = MMOProfiles.profileId` is the sole durable identity for
PetCore profiles, progression, equipment, policy eligibility and companion
roster. A player UUID is an online session principal only and never becomes a
substitute durable key. This decision is normative; the full identity,
authority-supersession, legacy-claim and content-run projection rules are
defined in `docs/contracts/petcore-identity-and-content-run-contract.yaml`,
referenced above, which this ADR incorporates rather than restates.

`MCPets` is retired as a runtime authority. It becomes legacy-only: its
current data is read-only migration input for a one-time, player-confirmed
claim into the currently selected `PetSubjectId` (see
`docs/contracts/petcore-legacy-claim-contract.yaml`), never a live dependency
and never an automatic migration target. `MCPets` must be disabled before any
PetCore vertical-slice testing; coexistence between `MCPets` and PetCore is
not a supported runtime mode. This ADR's implementation cuts over `MCPets`
(back up legacy data, disable/remove its runtime paths, prove a clean
affected-backend boot) in the same delivery as the registry update below; it
does not implement PetCore entity spawning, admission, scheduling or any
gameplay policy — those remain gated behind their own tickets and evidence.

`CompanionCore` (the ADR-002 plugin, `plugin-src/companion-core`) is likewise
retired as a live durable companion authority. Its source, configuration and
build output are preserved for later PetCore migration reference, but its
world-backend JAR and Paper remap-cache JAR are removed from all active plugin
loading paths in this delivery. The exact backup, disabled paths and rollback
commands are recorded in `docs/audits/2026-07-20/mcpets-cutover-evidence.md`.
No new companion-authority work may target or reactivate `CompanionCore` after
this ADR.

The same delivery updates `pet_progression` in
`docs/config_authority_registry.yaml`: `authority: PetCore`,
`current_adapter: none` (no PetCore runtime exists yet — this is a platform
gate, not a delivery), `target_persistence` names a PetCore-owned MariaDB
namespace keyed by `PetSubjectId`, and `MCPets` is marked disabled/legacy in
the linked foundation document.

## Alternatives considered

### Keep `CompanionCore` and add `MCPets` migration as a separate track

Rejected. Leaves two partially-implemented companion authorities live at
once, which is exactly the split-authority state ADR-001 forbids, and
neither implements the four-policy product scope the tech plan requires.

### Migrate `MCPets` data automatically into `CompanionCore` without a new ADR

Rejected. `CompanionCore` only implements a single tank policy; there is no
receiving authority for Beast Companion, Summoned Minion or Adventurer
Companion state, and an automatic migration would violate the tech plan's
explicit "no automatic migration to every character" constraint and the
identity contract's one-time, explicit-claim requirement.

### Leave `MCPets` as the pet authority and layer PetCore beside it

Rejected. `MCPets` has no working SQL persistence in staging (no `mcpets_db`
database, `localhost`-only MySQL config unreachable from the Docker network)
and cannot enforce the policy/admission/lifecycle contract the tech plan
requires. Dual runtime/dual write is explicitly out of scope for this
platform gate.

## Consequences

- `PetCore` must be built from scratch; no existing plugin in this repository
  satisfies its runtime contract. Until it exists, `pet_progression` remains
  `runtime_status: blocked` in the authority registry.
- `MCPets` cannot auto-spawn, mount or teleport an entity after this ADR's
  cutover lands; any future MCPets-derived content requires an explicit,
  reasoned exception recorded against this ADR.
- `CompanionCore` cannot load, register `/companion`, or spawn an entity after
  this cutover. Its preserved source/config/build are migration reference only;
  a later PetCore ticket may selectively absorb code into its
  `MythicMobsAdapter` delivery but must not restore a parallel runtime.
- Future companion/pet work must implement against
  `docs/contracts/petcore-identity-and-content-run-contract.yaml` (and the
  `petcore-unified-companion-runtime-tech-plan` planning artifact it is
  derived from), not against `ADR-002`, `CompanionCore`, or `MCPets` config.
