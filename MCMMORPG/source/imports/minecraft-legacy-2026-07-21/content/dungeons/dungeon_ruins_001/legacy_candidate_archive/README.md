# DR001 Legacy Candidate Archive

`legacy_candidate_archive/` is an archive-only holding area inside the
Minecraft legacy import for `dungeon_ruins_001`.

## Labels

- `archive-only`: keep for historical comparison or recovery context only.
- `do-not-promote`: do not copy these files into active source, staging, or
  runtime paths as-is.
- `shadow-copy-only`: non-authoritative duplicate retained only for
  audit/provenance.

## Current blocked groups

| Path/group | Labels | Reason |
| --- | --- | --- |
| `schematics/` | `archive-only` | Legacy candidate schematics are preserved for comparison only. |
| `modelengine/` | `archive-only` | Historical candidate blueprints; not authoritative final assets. |
| `sounds/` | `archive-only` | Legacy audio candidates retained only as reference. |
| `nexo/` | `archive-only`, `shadow-copy-only` | 24 duplicate relative paths overlap with `../resourcepack/assets/nexo/`; authority is resolved in favor of `../resourcepack/assets/nexo/`, so this tree stays only as a non-authoritative audit copy. |

## Duplicate overlap

The blocked `nexo/` overlap includes duplicated block, furniture, and item
model/texture files such as:

- `models/item/dungeon_ruins_001/block/ashen_throne_brick.json`
- `models/item/dungeon_ruins_001/furniture/reward_vault_chest.json`
- `models/item/dungeon_ruins_001/item/mossbound_relic.json`
- `textures/item/dungeon_ruins_001/block/overgrown_rune_stone.png`

Treat this directory as audit material only. Authority is already resolved to
`../resourcepack/assets/nexo/`; this copy remains only for provenance and
later cleanup.
