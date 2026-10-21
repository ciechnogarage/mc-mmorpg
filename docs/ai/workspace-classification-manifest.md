# Workspace Classification Manifest

Version: 2026-07-21

Scope: read-only classification of:

- `/home/przemek/projects/MC`
- `/home/przemek/projects/MCMMORPG`
- `/home/przemek/projects/Minecraft`

Method: `du`, `stat`, `git`, `find`, and `rg` over names and metadata only. No runtime, secret, player-data, Docker, Paper, RCON, or Mineflayer actions were performed.

## Summary

| Path | Size | Git repo | Classification | Recommended action |
| --- | --- | --- | --- | --- |
| `/home/przemek/projects/MC` | `6.0G` | `true` | `active-source` | `keep`; candidate canonical only after sibling roots are resolved |
| `/home/przemek/projects/MCMMORPG` | `20K` | `false` | `import-candidate` | `block archive/move`; confirm ownership/import need |
| `/home/przemek/projects/Minecraft` | `8.4G` | `true` | `import-candidate` | `block archive/move`; audit authoritative consolidation |

## `/home/przemek/projects/MC`

| Field | Value |
| --- | --- |
| path | `/home/przemek/projects/MC` |
| size | `6.0G` |
| git_repo | `true` |
| last_modified_summary | Snapshot from `2026-07-21`; top-level includes project documentation and runtime/source trees. |
| contains_runtime_data | `true` |
| contains_source_assets | `true` |
| contains_docs_or_design | `true` |
| contains_secrets_or_player_data_risk | `true` |
| known_active_references | Active worktree; `MCMMORPG/` subtree contains current server paths. |
| classification | `active-source` |
| recommended_action | `keep`; do not mark final `canonical` until sibling roots are resolved. |
| evidence | `du -sh` returned `6.0G`; `git rev-parse --is-inside-work-tree` succeeded; worktree contains source, docs, Docker, runtime-like paths, worlds, plugin configs, and known sensitive filename risks such as `ops.json`, `banned-players.json`, `usercache.json`, `whitelist.json`, and `world/playerdata`. |

## `/home/przemek/projects/MCMMORPG`

| Field | Value |
| --- | --- |
| path | `/home/przemek/projects/MCMMORPG` |
| size | `20K` |
| git_repo | `false` |
| last_modified_summary | Root mtime `2026-07-20 10:49:38 +0200`; observed shallow tree contains `plugins/MythicMobs`. |
| contains_runtime_data | `true` |
| contains_source_assets | `true` |
| contains_docs_or_design | `false` |
| contains_secrets_or_player_data_risk | `false` |
| known_active_references | Traycer workspace-standard plan and ticket explicitly include this path in read-only audit scope before archive/import decisions. No current operational reference from `/home/przemek/projects/MC/docs` was found in this audit. |
| classification | `import-candidate` |
| recommended_action | `block archive/move`; decide whether `plugins/MythicMobs` is authoritative plugin content, import material, or legacy. |
| evidence | `du -sh` returned `20K`; `git rev-parse --is-inside-work-tree` failed; shallow `find` found `plugins` and `plugins/MythicMobs`; no `world*`, `logs`, `runtime`, `source`, `docs`, `docker`, or secret/player-data filenames were observed at inspected depth. |

## `/home/przemek/projects/Minecraft`

| Field | Value |
| --- | --- |
| path | `/home/przemek/projects/Minecraft` |
| size | `8.4G` |
| git_repo | `true` |
| last_modified_summary | Root mtime `2026-06-22 00:56:40 +0200`; top-level includes `.git`, `wiki`, `tasks`, `ops`, `docs`, `assets`, `modele`, `content`, `design`, and `Server — kopia`. |
| contains_runtime_data | `true` |
| contains_source_assets | `true` |
| contains_docs_or_design | `true` |
| contains_secrets_or_player_data_risk | `true` |
| known_active_references | `/home/przemek/projects/MC/docs/superpowers/specs/2026-7-20-mc-asset-dataset-pipeline-design.md` references `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)`. Traycer critique artifacts also identified this tree as not safe to treat as dead archive. |
| classification | `import-candidate` |
| recommended_action | `block archive/move`; audit authoritative source/assets/docs/runtime before any consolidation decision. |
| evidence | `du -sh` returned `8.4G`; `git rev-parse --is-inside-work-tree` succeeded; shallow tree includes source-like and design directories plus runtime-copy markers. Risk-name markers observed include `.tools/mariadb-data`, `.tools/velocity-loch-local/forwarding.secret`, and `Server — kopia/1.21.11/{banned-players.json,ops.json,usercache.json,whitelist.json}`. |

## Blockers

- `/home/przemek/projects/Minecraft` blocks declaring `/home/przemek/projects/MC` fully `canonical`: it is a separate git repo with source-like directories and an active documented reference from `/home/przemek/projects/MC/docs`.
- `/home/przemek/projects/MCMMORPG` blocks cleanup/archive decisions: even though it is small, it still contains a plugin subtree outside `/home/przemek/projects/MC` and cannot be treated as safe archive without ownership/import review.

## Decision Boundary

This manifest classifies the three roots for follow-up planning only. It does not authorize deletion, move, rename, archive, import, runtime changes, Docker changes, or live-server changes.
