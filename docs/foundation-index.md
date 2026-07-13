# MMORPG Design Foundation Index

Status: consolidation map, 2026-07-12.

This file is the navigation layer for the many `*-foundation-v0.0.1.md`
documents. Those documents are design inputs, not runtime truth. A decision is
authoritative only after it is copied into a current domain contract and linked
to implementation evidence.

## Product Decisions That Survive

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
| `merge` | Valuable design input. Extract decisions into a domain contract; keep this file as history until merged. |
| `content-candidate` | Useful concrete content, numbers, names, or encounter ideas; not a global rule. |
| `legacy` | Superseded framing, especially starter-only or foundation-MVP sequencing. Keep for traceability, do not extend. |
| `blocked` | Requires runtime, plugin, balance, or persistence evidence before adoption. |

## Domain Map

### Combat and RPG

`merge`: `combat-foundation-v0.0.1.md`,
`damage-defense-foundation-v0.0.1.md`, `skill-ability-system-v0.0.1.md`,
`skill-trees-passives-upgrades-foundation-v0.0.1.md`,
`progression-respec-foundation-v0.0.1.md`.

`content-candidate`: `class-subclass-foundation-v0.0.1.md`,
`class-skill-kits-foundation-v0.0.1.md`.

Extract first: stat ownership, resource model, skill tags, defensive layers,
respec rules, and player-versus-player counterplay. Do not freeze class values
from these drafts until runtime formulas and PvP normalization are tested.

### Itemization, Loot, Professions, and Economy

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
outputs, storage boundaries, reward provenance, and anti-inflation controls.
First-act loot tables remain examples until the global economy contract exists.

### World, PvE, and Dungeons

`merge`: `world-content-loop-foundation-v0.0.1.md`,
`world-events-foundation-v0.0.1.md`,
`dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md`,
`quest-contract-objective-foundation-v0.0.1.md`.

`content-candidate`: `level-1-dungeon-island-foundation-v0.0.1.md`,
`mob-boss-encounter-001-foundation-v0.0.1.md`,
`dungeon-ladder-002-010-foundation-v0.0.1.md`.

Extract first: region and dungeon identity, encounter contracts, objective
credit, reset rules, reward handoff, and world-event participation. Level 1 is
an implementation sample, not the architecture for the whole game.

### PvP and Seasonal Competition

`merge`: `pvp-foundation-v0.0.1.md`,
`seasons-prestige-cosmetics-foundation-v0.0.1.md`.

Extract first: match modes, rating authority, season boundaries, normalization,
anti-abuse rules, reward reset scope, and persistence guarantees. PvP currency
must be explicitly seasonal; characters and cosmetics must not reset.

### Pets and Companions

`merge`: `pet-companion-minion-system-foundation-v0.0.1.md`.

Extract first: pet progression authority, utility categories, activity hooks,
training costs, combat limits, and the rule that pets complement rather than
replace player builds.

### Social, Factions, and Community

`merge`: `guilds-foundation-v0.0.1.md`,
`reputation-faction-foundation-v0.0.1.md`.

Extract first: party and guild ownership, social permissions, reputation
provenance, group content access, guild projects, and PvP territory rules.

### UX, Hub, Discovery, and Onboarding

`merge`: `discovery-npc-board-loop-001-foundation-v0.0.1.md`,
`stolica-wyspy-hub-foundation-v0.0.1.md`,
`onboarding-tutorial-foundation-v0.0.1.md`.

`legacy`: `starter-skills-class-progression-foundation-v0.0.1.md` when it is
read as the mandatory server progression path. Its useful skill-discovery and
class-teaching ideas remain content candidates.

Extract first: navigation, discovery signals, quest/board contracts, character
selection, first-session information density, and optional route ordering.

### Runtime and Operations

`canonical`: `plugin_manifest.yaml`, `compatibility_matrix.md`,
`server_environment.md`, `server_properties_standard.md`.

These files describe observed runtime state and operational constraints. They
must not be replaced by foundation design drafts. Runtime claims require logs,
backend validation, or explicit evidence paths.

## Legacy and Archive Candidates

The following material should not be extended as the current plan:

- foundation-MVP documents that define a single starter-to-level-1 vertical
  slice as the whole product;
- one-off implementation prompts that assign the entire server to sequential
  starter phases;
- duplicated first-act documents that repeat the same item, loot, or dungeon
  values without a shared ID contract.

No file is deleted by this index. Before archiving or removing a document, run
a reference scan and copy any still-valid decision into the relevant domain
contract.

## Next Consolidation Deliverable

Create one current contract per domain, then add a cross-domain registry for:

- authority and owner;
- stable IDs and namespaces;
- persistence and reset scope;
- plugin adapters and consumers;
- validation command and evidence path;
- unresolved decisions and blockers.

Until that registry exists, the foundation documents are a library of ideas,
not a permission to implement every paragraph.
