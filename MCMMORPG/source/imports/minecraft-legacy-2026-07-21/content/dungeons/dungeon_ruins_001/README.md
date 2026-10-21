# Ruiny Złamanej Korony (`dungeon_ruins_001`)

## Archive-Only Import Status

This directory is a legacy import snapshot under `source/imports/`.
It is not the canonical DR001 source of truth and it is not approved for
staging, promotion, or runtime use.

| Path/group | Labels | Meaning |
| --- | --- | --- |
| `legacy_candidate_archive/` | `archive-only`, `do-not-promote` | Historical candidate assets kept only for audit/recovery context. |
| `legacy_candidate_archive/nexo/` | `archive-only`, `shadow-copy-only` | Contains 24 duplicate relative paths already present under `resourcepack/assets/nexo/`; authority is resolved to `resourcepack/assets/nexo/`, and this tree remains audit-only. |
| `qa/visual_evidence/` | `archive-reference`, `extract-only` | Historical screenshots, waveforms, and logs; useful for review, not active proof of release readiness. |
| `qa/*.md`, `ASSET_POLISH_REPORT.md`, `IMPLEMENTATION_NOTES.md`, `CHANGELOG.md` | `extract-only` | Notes and reports may contain useful snippets, but must be reviewed and rewritten before any promotion. |

Runtime helper commands and staging wording that appear elsewhere in this
import remain historical context only.

`dungeon_ruins_001` is the working folder and MythicDungeons map ID for the
DR001 dungeon release-prep package. The player-facing dungeon is **Ruiny
Złamanej Korony**: an instanced MMO RPG dungeon that moves from overgrown ruins,
through sunken catacombs, into a void throne sanctum.

This package is source and release-prep content only. It does not run live
commands, does not deploy to production, and does not edit staging plugin
folders unless an operator explicitly approves a staging import.

## Dungeon Identity

| Field | Value |
| --- | --- |
| Folder / map ID | `dungeon_ruins_001` |
| Display name | `Ruiny Złamanej Korony` |
| Public ID namespace | `DR001_` / `dr001_` |
| Entry key | `DR001_RUINED_GATE_KEY` |
| Currency | `DR001_ANCIENT_COIN`, `DR001_RUNE_DUST` |
| Weekly reward | `DR001_CROWN_CORE` |
| Party size | 1-5 |
| Level bands | Normal 12+, Veteran 20+, Mythic 30+ |
| Target run time | 18-35 minutes |

## Stage Flow

| Stage | Biome | Player Goal |
| --- | --- | --- |
| START | `dr001_start_overgrown_ruins` | Collect 3 relic shards, defeat 18 courtyard mobs, kill `DR001_GatewardenVaros`, unlock the collapsed stairwell checkpoint. |
| MID | `dr001_mid_sunken_catacombs` | Activate 4 aqueduct runes, survive the flood phase, defeat `DR001_CisternOracleNhalruun`, unlock Crown Bridge. |
| END | `dr001_end_void_sanctum` | Destroy 3 `dr001_void_anchor` objectives, defeat `DR001_RuinedKingAurelion`, claim `dr001_loot_shrine`, exit safely. |

## Active Source Layers

- `iris/` defines the DR001 biome/object/structure contracts.
- `mythicdungeons/` defines editor-owned stage, checkpoint, trap, and loot
  contracts for the `dungeon_ruins_001` map.
- `mythicmobs/` defines DR001 mobs, bosses, skills, and drop tables.
- `nexo/` defines `dr001_` blocks, furniture, key items, and resource-pack
  placeholders.
- `mmoitems/` and `mmocore/` define DR001 currencies, gear, level gates, and
  reward hooks.
- `modelengine/` defines required `dr001_*.bbmodel` files and animations.
- `qa/` contains the release-prep gate, live runner, handoffs, and player-test
  scripts.

## Legacy Scaffold

Older `dungeon_ruins_001_*` assets and generated files are retained only as
legacy scaffold/reference evidence. Active configs, validators, and release
gates must use `DR001_` or `dr001_` IDs. Do not promote legacy scaffold assets as
final production assets.

## Safe Validation

Run these from the repo root before any staging runtime approval:

```powershell
node content\dungeons\dungeon_ruins_001\tools\validate_references.mjs
node content\dungeons\dungeon_ruins_001\tools\validate_final_assets.mjs
powershell -NoProfile -ExecutionPolicy Bypass -File .\content\dungeons\dungeon_ruins_001\tools\import_assets_to_staging.ps1
git -c safe.directory=C:/Codex/Minecraft status --short -- content/dungeons/dungeon_ruins_001 tasks
```

Expected pre-release state: reference validation passes, final asset validation
blocks until approved final intake assets exist, and the import helper runs in
dry-run mode only.

## Runtime Approval

Staging runtime testing requires explicit operator approval. The documented
MythicDungeons command syntax comes from the local docs mirror:
`.github/agent-knowledge/minecraft-server-docs/mythicdungeons/wiki/md/commands-and-permissions.md`.

Approved staging operators may later run `/md reload dungeon_ruins_001` and
`/md play dungeon_ruins_001` after import, editor wiring, and log-watch setup.
