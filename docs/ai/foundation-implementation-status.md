# Foundation Implementation Status

Status ledger for turning foundation v0.0.1 into a staging MVP.

Status values:

- `planned`: documented but not yet inspected for implementation
- `read-only`: agent inspection ready or in progress
- `staging-ready`: read-only report is enough for scoped staging edits
- `implemented`: staging config or content exists
- `validated`: QA evidence exists
- `blocked`: blocker named with owner and next evidence needed

## Solo First-Pass Policy

- `staging-ready` means eligible for scoped staging work after the full solo
  read-only sweep, not permission to skip earlier milestones.
- The first Codex-only pass should traverse `M0-M7` in order and normalize
  every milestone into an integrator packet before any shared-file writes.
- Use this file for milestone state and `foundation-agent-reports.md` for
  evidence summaries; do not mix raw notes into status rows.

## Current Status

Aktualizacja 2026-06-29: statusy zsynchronizowane z realnym stanem configów.
Plan wykonawczy rozbity na atomowe zadania w `docs/ai/foundation-mvp-task-board.md`,
wartości w `docs/ai/foundation-mvp-spec-values.md`.

| Milestone / zadanie | Status | Owner | Next action |
| --- | --- | --- | --- |
| M0 inventory/runtime baseline | `staging-ready` | `inventory` | keep baseline evidence current; MCPets fallback staging-only |
| A1 spec twardych wartości | `implemented` | integrator | done: `foundation-mvp-spec-values.md` |
| A2 harness walidacji (realna walka) | `planned` | `qa` | przerobić `foundation_bot.js` na realny atak; blocker dla M7 |
| M1 hub → B1 build świata | `planned` | `dungeon`/`ui`+FAWE | budować 7 dystryktów wg §1 spec |
| M1 hub → B2 NPC+portal+board | `planned` | `mobs`+`ui` | 17 NPC wg §2, portal/board wg §3 |
| M1 hub → B3 orientacja spawn | `planned` | `ui` | holo/signs/ścieżki spawn→Nexus |
| M2 starter RPG → C1 | `blocked` | `rpg` | backend startera jest juz potwierdzony przez claimed-state/menu path; nastepny blocker to runtime seam w `level_1`: realny `md play` laduje instancje bez zadnych encji bojowych, wiec nie da sie jeszcze zrobic bezposredniego proofu wczesnej walki swiezym graczem (§6) |
| M3 dungeon flow → C3 | `implemented` (runtime niepotwierdzony) | `dungeon` | potwierdzić entry/exit/reset/return (§7) |
| M4 mobs/boss → C2 | `implemented` (death→finish niepotwierdzony) | `mobs` | potwierdzić realny kill→finish (§4,§5) |
| M5 permissions/economy → D1 | `validated` | `economy` | utrzymać `dungeons.play` + `dungeons.play.send`; exact GUI/E2E zostaje po stronie M7 |
| M6 runtime/storage safety → D2 | `validated` | `ops` | utrzymać brak nowych critical errors; znane wyjątki stagingowe tylko monitorować |
| M7 release QA → E | `blocked` | `qa` | czeka na A2 + B + C + D; pełny E2E zielony |

## Active Blockers

- HuskSync is disabled for the staging MVP and still requires a production DB path.
- MCPets SQL fallback to YAML remains accepted for staging only.
- M3, M4, and M7 still lack concrete runtime evidence for entry, exit, reset,
  regions, mob IDs, model IDs, boss death, loot, and full vertical-slice completion.

## Next Safe Work

- Full first pass: run `M0-M7` as read-only and normalize each result into a
  milestone packet.
- First staging slice after that sweep: `M1 + M2`.
- Keep `M3 + M4` read-only until dungeon flow, reset behavior, and concrete
  mob or model IDs are proven.
- Keep `M7` blocked until startup log, in-game journey, dungeon clear, reward,
  permissions, economy, rollback, and known runtime issue evidence exist.

## Foundation Inputs

Primary MVP design docs:

- `docs/player-journey-milestone-roadmap-v0.0.1.md`
- `docs/onboarding-tutorial-foundation-v0.0.1.md`
- `docs/stolica-wyspy-hub-foundation-v0.0.1.md`
- `docs/combat-foundation-v0.0.1.md`
- `docs/class-subclass-foundation-v0.0.1.md`
- `docs/starter-skills-class-progression-foundation-v0.0.1.md`
- `docs/skill-ability-system-v0.0.1.md`
- `docs/itemization-foundation-v0.0.1.md`
- `docs/economy-crafting-loot-foundation-v0.0.1.md`
- `docs/level-1-dungeon-island-foundation-v0.0.1.md`
- `docs/mob-boss-encounter-001-foundation-v0.0.1.md`
- `docs/loot-reward-table-001-foundation-v0.0.1.md`
- `docs/ai/city-hub-mvp-concept.md`
- `docs/ai/city-hub-blockout-v0.0.1.md`

## Update Rules

- Move milestone state only when evidence exists in `foundation-agent-reports.md`
  or in a clearly named local verification artifact.
- Use `blocked` only when the owner, blocker, and next evidence step are explicit.
- Keep status rows short; detailed findings belong in reports, not in this ledger.
