# MMORPG Design Foundation Index

Status: consolidation map, 2026-07-12.

This file is the navigation layer for many `*-foundation-v0.0.1.md`
documents. Those documents are design inputs, not runtime truth. A decision
is authoritative only after it is copied into a current domain contract and
linked to implementation evidence.

For active delivery sequence, source-of-truth boundaries, and current
blockers, read [the current server plan](current-server-plan.md).

Domain 00 owns product-governance truth, ownership placeholders, open
questions, and release-stage framing. Use `docs/product/` artifacts,
especially [00-master-product-bible.md](product/00-master-product-bible.md),
[00-domain-ownership.yaml](product/00-domain-ownership.yaml),
[00-open-questions.yaml](product/00-open-questions.yaml), and
[00-release-roadmap.md](product/00-release-roadmap.md).

## Product Decisions That Survive

These surviving statements remain foundation-level inputs until Domain 00
explicitly promotes them. Treat them as advisory source material, not
approved product truth on their own.

- The server is a broad persistent PvE/PvP MMORPG, not a starter dungeon demo.
- Characters, long-term progression, and cosmetics persist across test seasons.
- PvP rating, seasonal currency, and ladder progress reset by season.
- Pets are a second progression track for daily, gathering, training, and
  combat utility. They cannot replace the player class or become a mandatory
  multiplier for top content.
- PvE and competitive PvP are first-class systems and must be balanced together.
- Discovery should offer direction without forcing a single starter route.
- Plugin configs are adapters. Shared concepts need one declared authority,
  stable IDs, persistence rules, and runtime verification.

## Disposition Rules

| Status | Meaning |
| --- | --- |
| `canonical` | Current contract or operational source of truth. Changes require integrator review. |
| `merge` | Valuable design input. Extract decisions into a domain contract; keep file history until merged. |
| `content-candidate` | Useful concrete content, numbers, names, or encounter ideas; not a global rule. |
| `legacy` | Superseded framing, especially starter-only foundation-MVP sequencing. Keep traceability, do not extend. |
| `blocked` | Requires runtime, plugin, balance, or persistence evidence before adoption. |

## Domain Map

### Combat RPG

`merge`: `combat-foundation-v0.0.1.md`,
`damage-defense-foundation-v0.0.1.md`,
`skill-ability-system-v0.0.1.md`,
`skill-trees-passives-upgrades-foundation-v0.0.1.md`,
`progression-respec-foundation-v0.0.1.md`.

`content-candidate`: `class-subclass-foundation-v0.0.1.md`,
`class-skill-kits-foundation-v0.0.1.md`.

Extract first: stat ownership, resource model, skill tags, defensive
layers, respec rules, and player-versus-player counterplay. Do not freeze
these drafts until runtime PvP normalization is tested.

### Itemization, Loot, Professions, Economy

`merge`: `itemization-foundation-v0.0.1.md`,
`economy-crafting-loot-foundation-v0.0.1.md`,
`professions-foundation-v0.0.1.md`,
`loadout-bank-storage-foundation-v0.0.1.md`.

`content-candidate`: `ekwipunek-pierwszego-aktu-foundation-v0.0.1.md`,
`foundation-typy-ekwipunku-v0.0.1.md`,
`foundation-sloty-i-ekwipunek-v0.0.1.md`,
`loot-reward-table-001-foundation-v0.0.1.md`,
`model-lupu-z-lochow-foundation-v0.0.1.md`,
`prefiksy-sufiksy-pierwszego-aktu-foundation-v0.0.1.md`.

Extract first: item ID ownership, currency sinks, profession inputs and
outputs, storage boundaries, reward provenance, and anti-inflation
controls. First-act loot tables remain examples until the global economy
contract exists.

### World, PvE, Dungeons

`canonical`: `contracts/world-progression-model.md`,
`contracts/unlock-rules.yaml`,
`contracts/dungeon-floor-contract.yaml`,
`contracts/travel-and-discovery.md`,
`contracts/island-sector-mapping.yaml`,
`contracts/act-map.yaml`.

`canonical-migration`: `contracts/migration/domain-06-difficulty-alias-register.yaml`.

`merge`: `world-content-loop-foundation-v0.0.1.md`,
`world-events-foundation-v0.0.1.md`,
`dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md`,
`quest-contract-objective-foundation-v0.0.1.md`.

`content-candidate`: `level-1-dungeon-island-foundation-v0.0.1.md`,
`mob-boss-encounter-001-foundation-v0.0.1.md`,
`dungeon-ladder-002-010-foundation-v0.0.1.md`.

Current source of truth for domain `06` is the contract set in
`docs/contracts/`, not the older foundation library.

Use `world-progression-model.md` for `ACT/ISL/DNG/FLOOR` semantics,
difficulty naming, and world-vs-dungeon parity.

Use `unlock-rules.yaml` for `unlock_next_act`, island gating boundaries,
and character-scoped progression unlocks.

Use `dungeon-floor-contract.yaml` for sector structure, `FLOOR-001..010`,
per-floor difficulty availability, and `FLOOR-010 != raid`.

Use `travel-and-discovery.md` for hubs, waypoint taxonomy, discovery
states, and service-access guardrails.

Use `island-sector-mapping.yaml` for `ISL-xx <-> DNG-xx` pairing and
shared theme/conflict/mechanical language.

Use `act-map.yaml` for act topology and production-status tracking.

Use `domain-06-difficulty-alias-register.yaml` for migration from
`easy/normal/hard/very_hard` to `story/standard/veteran/mythic`.

Interpret older world/dungeon foundation docs as source material only:

- `dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md`
  remains a historical merge input, not active difficulty canon.
- `level-1-dungeon-island-foundation-v0.0.1.md` is a content example and
  vertical-slice reference, not the global architecture.
- `dungeon-ladder-002-010-foundation-v0.0.1.md` remains a content
  candidate for future sectors, not the active sector contract.

Extract from older material only when adding content detail that remains
compatible with the current domain `06` contracts.

### PvP Seasonal Competition

`merge`: `pvp-foundation-v0.0.1.md`,
`seasons-prestige-cosmetics-foundation-v0.0.1.md`.

Extract first: match modes, rating authority, season boundaries,
normalization, anti-abuse rules, reward reset scope, and persistence
guarantees. PvP currency must be explicitly seasonal; character cosmetics
must not reset.

### Pets Companions

`merge`: `pet-companion-minion-system-foundation-v0.0.1.md`.

Extract first: pet progression authority, utility categories, activity
hooks, training costs, combat limits, and the rule that pets complement
rather than replace player builds.

### Social, Factions, Community

`merge`: `guilds-foundation-v0.0.1.md`,
`reputation-faction-foundation-v0.0.1.md`.

Extract first: party and guild ownership, social permissions, reputation
provenance, group content access, guild projects, and PvP territory rules.

### UX, Hub, Discovery, Onboarding

`merge`: `discovery-npc-board-loop-001-foundation-v0.0.1.md`,
`stolica-wyspy-hub-foundation-v0.0.1.md`,
`onboarding-tutorial-foundation-v0.0.1.md`.

`legacy`: `starter-skills-class-progression-foundation-v0.0.1.md`.

Do not read the starter-only progression draft as a mandatory server
progression path. Its useful skill-discovery and class-teaching ideas
remain content candidates.

Extract first: navigation, discovery signals, quest and board contracts,
character selection, first-session information density, and optional route
ordering.

### Runtime Operations

`canonical`: `plugin_manifest.yaml`, `compatibility_matrix.md`,
`server_environment.md`, `server_properties_standard.md`.

These files describe observed runtime state and operational constraints.
They must not be replaced by foundation design drafts. Runtime claims
require logs, backend validation, or explicit evidence paths.

## Legacy Archive Candidates

The following material should not be extended as current plan:

- foundation-MVP documents that define a single starter-to-level-1
  vertical slice as the whole product
- one-off implementation prompts that assign the entire server to
  sequential starter phases
- duplicated first-act documents that repeat the same item, loot, or
  dungeon values without a shared ID contract

No file is deleted by this index. Before archiving or removing a document,
run a reference scan and copy any still-valid decision into the relevant
domain contract.

## Next Consolidation Deliverable

Create one current contract per domain, then add a cross-domain registry
for:

- authority and owner
- stable IDs and namespaces
- persistence and reset scope
- plugin adapters and consumers
- validation command and evidence path
- unresolved decisions and blockers

Until that registry exists, foundation documents are a library of ideas,
not permission to implement every paragraph.

When a foundation claim conflicts with current product-governance
handling, route it through
[00-current-state-intake.yaml](product/00-current-state-intake.yaml) and
[00-conflict-registry.yaml](product/00-conflict-registry.yaml) instead of
treating the foundation wording as self-approving.
