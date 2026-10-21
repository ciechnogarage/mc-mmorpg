# ADR-001: Player Data And Runtime Authority

- contract_status: accepted
- runtime_status: partial
- release_status: blocked
- Date: 2026-07-13
- Scope: first vertical slice and shared MMORPG data boundaries
- Supersedes: draft decisions that assign the same concept to multiple plugins

## Decision

Each shared concept has one durable authority. Plugin configuration is an
adapter or presentation layer unless this document explicitly assigns it
ownership. Runtime verification is still required before release approval.

| Concept | Authority | Adapter/consumers | Persistence boundary |
| --- | --- | --- | --- |
| Player profile | MMOProfiles | CharacterStage, MMOCore, MMOInventory, Guilds | persistent profile |
| Character stage and story progression | CharacterStage | MMOProfiles, CoreTools, UI | profile-scoped |
| Class, level and skills | MMOCore | MythicLib, MMOItems, MythicHUD | profile-scoped |
| Combat formulas | MythicLib | MMOCore, MMOItems, MythicMobs, MythicDungeons | formula/config; player state persisted by profile authority |
| RPG item definition | MMOItems | MMOCore, MMOInventory, MythicCrucible, CMI | config plus player-owned state |
| Item visual representation | Nexo | MMOItems, resource pack clients | generated pack; no gameplay authority |
| Inventory and storage | MMOInventory | MMOItems, MMOCore, CMI, Guilds | player storage |
| Balance | CMI economy | Vault, MMOItems, MMOInventory, Guilds, MythicDungeons | economy storage |
| Permissions | LuckPerms | CMI, CharacterStage, commands | permission storage |
| Mob and encounter logic | MythicMobs | ModelEngine, MythicLib, MythicDungeons | runtime/config |
| Mob model and animation | ModelEngine | MythicMobs, Nexo/resource pack | generated model assets |
| Dungeon lifecycle | MythicDungeons | MythicMobs, Iris, WorldGuard, Multiverse, ModelEngine | template plus instance state |
| Encounter result/reward approval | MythicDungeons | MythicMobs, MMOItems, MMOInventory, CMI | reward transaction and player state |

## Invariants

- No plugin may become a second authority by copying mutable state into lore,
  PlaceholderAPI values, GUI metadata, or ad-hoc command output.
- Stable IDs are the contract between adapters. Display names and lore are not
  identity keys.
- Presentation plugins may read authority state but may not decide progression,
  reward ownership, or persistence.
- A dependency outage must fail closed for rewards and state mutation, with an
  auditable error, rather than silently creating an alternate source of truth.
- Seasonal state remains separate from persistent character and cosmetic state.
- This ADR does not enable MariaDB or migrate existing data.

## Runtime acceptance

Release approval is independent from the architectural decision. This ADR can
remain `contract_status: accepted` while runtime verification is partial and
the release remains blocked. `release_status: approved` is valid only when
`runtime_status: verified` is set and the evidence register contains passing
login, restart, reconnect, and data-integrity evidence for the affected backend.

## Consequences

The reward flow needs an explicit idempotency boundary. MMOProfiles,
MMOInventory, and MMOCore cannot be migrated together as an unverified batch.
Future cross-domain changes must update this ADR or add a superseding ADR before
implementation.
