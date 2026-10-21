---
title: "Promotion decisions for legacy source/assets/content/scripts import"
status: "decision-manifest"
date: "2026-07-21"
---

# Promotion decisions for legacy source/assets/content/scripts import

Import root: `/home/przemek/projects/MC/MCMMORPG/source/imports/minecraft-legacy-2026-07-21`

Source root: `/home/przemek/projects/Minecraft`

This manifest is the controlling decision map for this import. It classifies imported groups for later cleanup/review tickets.

## Non-execution guard

This document does not authorize moving, deleting, archiving, promoting, copying into active runtime, Docker actions, staging sync, RCON, Mineflayer, or edits to `/home/przemek/projects/Minecraft`.

All write/runtime work still requires the current MC standard:

- canonical workspace root: `/home/przemek/projects/MC`
- write/runtime work: explicit `dev-<scope>` lease
- staging: integration/promotion only, never parallel agent editing

## Controlled decision vocabulary

| Decision | Meaning | Allowed next action |
| --- | --- | --- |
| `promote-candidate` | Source-like material worth a focused promotion review. | Read-only review, then separate promotion ticket if approved. |
| `blocked-quality-review` | Potentially useful material, but current metadata/quality/provenance blocks promotion. | Quality/provenance review only. |
| `archive-only` | Historical/duplicate/legacy material that should not become active source. | Label/archive planning only; no active promotion. |
| `extract-only` | Text/report material may contain useful lessons, but must not be promoted as current truth. | Extract curated lessons into active docs. |
| `dedup-needed` | Multiple candidate copies exist; choose one source of truth before promotion. | Dedup/parity decision only. |
| `do-not-promote` | Operationally unsafe/stale material that must not enter active workflow. | Keep quarantined; optionally document why. |

No other decision values are valid in this manifest.

## Group decisions

| Decision | Path group | Files | Evidence / reason | Allowed next action | Forbidden action |
| --- | --- | ---: | --- | --- | --- |
| `promote-candidate` | `assets/blockbench/source/mcmmo_elite_crypt_warden` | 4 | Editable Blockbench/source-like bundle. | `minecraft-mcmmo-elite-crypt-warden-promotion-review` read-only quality/path review. | Promote without ModelEngine/Blockbench quality validation. |
| `promote-candidate` | `assets/modelengine/mcmmo_elite_crypt_warden` | 4 | Source-like ModelEngine metadata and manifest. | Same crypt warden promotion review. | Treat as runtime-proven model without current validation. |
| `promote-candidate` | `assets/mythicmobs/mcmmo_elite_crypt_warden` | 3 | Source-like MythicMobs mob/skill YAML. | Same crypt warden promotion review plus plugin compatibility check. | Copy into active plugin config or runtime. |
| `promote-candidate` | `assets/nexo/mcmmo_elite_crypt_warden` | 3 | Source-like Nexo item/resourcepack config. | Same crypt warden promotion review plus namespace check. | Promote without Nexo namespace/resourcepack validation. |
| `promote-candidate` | `assets/textures/mcmmo_elite_crypt_warden` | 1 | Placeholder/source texture group; tiny, likely supporting asset. | Include in crypt warden review as supporting material. | Promote independently from model bundle. |
| `promote-candidate` | `scripts/assets/validate_mcmmo_elite_crypt_warden_packet.py` | 1 | Script dependency scan reported `none`; likely useful validator. | Review path assumptions and convert to active helper only in a separate ticket. | Run/promote as current workflow without path review. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/intake/modelengine` | part of 66 intake files | DR001 intake manifest marks assets `human_review_required` / `blocked_quality_review`. | `minecraft-dr001-asset-quality-review`. | Treat generated/procedural candidates as approved active source. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/intake/nexo` | part of 66 intake files | Candidate Nexo assets; prior review says quality/provenance validation required. | DR001 asset-quality review. | Promote to active resourcepack/plugin config. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/intake/schematics` | part of 66 intake files | Candidate schematics; manifest requires review before staging import. | DR001 asset-quality review. | Copy into Iris/WorldEdit/runtime paths. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/intake/sounds` | part of 66 intake files | Candidate audio; provenance/resourcepack validation required. | DR001 asset-quality review. | Promote without audio/resourcepack validation. |
| `do-not-promote` | `content/dungeons/dungeon_ruins_001/intake/nexo_runtime` | 0 imported / 1 skipped | `skipped-manifest.tsv` excluded `nexo_runtime/sounds.yml` as runtime-like. | Keep skip record only. | Import, recreate, or promote runtime-like subtree in this ticket series. |
| `dedup-needed` | `content/dungeons/dungeon_ruins_001/resourcepack` | 29 | Prior review found duplicate Nexo candidates vs `legacy_candidate_archive/nexo`. | Dedup/parity review; choose one source of truth. | Promote together with duplicate archive candidates. |
| `archive-only` | `content/dungeons/dungeon_ruins_001/legacy_candidate_archive` | 53 | Legacy/archive candidates; internal duplicate groups exist. | `minecraft-dr001-archive-only-labels`. | Promote to active source. |
| `do-not-promote` | `content/dungeons/dungeon_ruins_001/tools/*.ps1` | 2 of 16 tools files | PowerShell staging apply/sync scripts encode old staging/server-copy flow. | `minecraft-dr001-staging-tools-do-not-promote-labels`; optional extraction of validation ideas only. | Execute, promote to active `scripts/`, or use as current staging workflow. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/tools` non-`.ps1` helpers/reports | remaining tool files | May contain validation ideas, but lives beside legacy staging tooling. | Extract ideas into a new lease-aware design only after review. | Promote whole tools folder. |
| `archive-only` | `content/dungeons/dungeon_ruins_001/qa` | 61 | Historical screenshots/logs/spectrograms/evidence; not current validation. | Label archive-reference. | Treat as current QA proof. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/*.md` reports/notes | mixed | Reports contain useful lessons and stale runtime claims. | Extract curated design/QA lessons only. | Promote as active operational truth. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/assets_to_create` | 4 | Requirements/checklists, not actual approved assets. | Extract requirements into active docs if still useful. | Treat as delivered asset source. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/iris` | 29 | Source-like Iris config/schematics, but tied to DR001 candidate quality gate. | DR001 asset-quality review and dedup. | Copy into active Iris/runtime pack. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/modelengine` | 13 | Source-like plugin configs/models, but not separated from stale DR001 state. | DR001 asset-quality review. | Promote without parity against intake/resourcepack. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/mythicdungeons` | 5 | Source-like dungeon config, but current behavior not validated. | DR001 asset-quality review. | Treat historical staging load as current proof. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/mythicmobs` | 4 | Source-like mobs/skills, but current plugin compatibility not proven. | DR001 asset-quality review. | Copy to active MythicMobs runtime/config. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/nexo` | 4 | Source-like Nexo config, but needs namespace/resourcepack validation. | DR001 asset-quality review. | Promote without namespace review. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/mmocore` | 3 | Source-like RPG config, but tied to unvalidated DR001 pack. | DR001 asset-quality review. | Promote into active RPG config. |
| `blocked-quality-review` | `content/dungeons/dungeon_ruins_001/mmoitems` | 6 | Source-like item config, but tied to unvalidated DR001 pack. | DR001 asset-quality review. | Promote into active MMOItems config. |
| `do-not-promote` | `content/dungeons/dungeon_ruins_001/staging` | 2 | Name and contents are staging-specific. | Keep quarantined or label do-not-promote. | Promote as active source/runtime workflow. |

## Global promotion rules for this import

1. No whole-tree promotion is allowed.
2. No DR001 folder is promotable until archive-only labels and staging-tool labels exist.
3. No `content/dungeons/dungeon_ruins_001/tools` material may become active tooling without a new lease-aware design.
4. No historical `qa` evidence counts as current validation.
5. No `blocked-quality-review` group may be promoted without a dedicated review artifact and follow-up ticket.
6. `promote-candidate` means “review next”, not “copy now”.

## Manifest inputs checked

- `README.md`: confirms quarantine/source-import and no runtime/staging promotion.
- `import-manifest.tsv`: 321 imported files.
- `script-legacy-dependencies.tsv`: script dependency scan reported no known legacy marker for `scripts/assets/validate_mcmmo_elite_crypt_warden_packet.py`.
- `skipped-manifest.tsv`: records excluded runtime-like `content/dungeons/dungeon_ruins_001/intake/nexo_runtime/sounds.yml`.

## Next tickets unblocked

- `minecraft-dr001-archive-only-labels`
- `minecraft-dr001-staging-tools-do-not-promote-labels`
- `minecraft-mcmmo-elite-crypt-warden-promotion-review`

## DR001 decision override (2026-07-22)

This section is the controlling decision map for
`content/dungeons/dungeon_ruins_001`.
For DR001, use the statuses below instead of the broader manifest vocabulary:
`keep`, `dedup-needed`, `reject`, `extract-only`.

### Canonical candidates

The following DR001 groups are the only canonical candidates for any future
promotion work:

- `content/dungeons/dungeon_ruins_001/intake/`
- `content/dungeons/dungeon_ruins_001/mythicdungeons/`
- `content/dungeons/dungeon_ruins_001/mythicmobs/`
- `content/dungeons/dungeon_ruins_001/mmoitems/`
- `content/dungeons/dungeon_ruins_001/mmocore/`
- `content/dungeons/dungeon_ruins_001/nexo/blocks.yml`
- `content/dungeons/dungeon_ruins_001/nexo/furniture.yml`
- `content/dungeons/dungeon_ruins_001/nexo/items.yml`
- `content/dungeons/dungeon_ruins_001/intake/nexo/sounds.json`

No other DR001 path is a canonical candidate until a separate decision says so.

### DR001 canonical candidate confirmation (2026-07-22)

Confirmed canonical candidates for future DR001 promotion review are limited to:

- `content/dungeons/dungeon_ruins_001/intake/`
- `content/dungeons/dungeon_ruins_001/mythicdungeons/`
- `content/dungeons/dungeon_ruins_001/mythicmobs/`
- `content/dungeons/dungeon_ruins_001/mmoitems/`
- `content/dungeons/dungeon_ruins_001/mmocore/`
- `content/dungeons/dungeon_ruins_001/nexo/blocks.yml`
- `content/dungeons/dungeon_ruins_001/nexo/furniture.yml`
- `content/dungeons/dungeon_ruins_001/nexo/items.yml`
- `content/dungeons/dungeon_ruins_001/intake/nexo/sounds.json`

Everything else in `content/dungeons/dungeon_ruins_001/` is non-canonical for
this ticket and must remain `extract-only`, `reject`, or legacy-reference-only
according to the DR001 override below.

This confirmation does not authorize promotion to active source, staging, or
runtime. It only freezes the candidate set for future focused review tickets.

### DR001 canonical quality-gate snapshot (2026-07-22)

The confirmed canonical candidates remain quality-gated. Current intake
manifest state:

- pack status: `human_review_required`
- approval status: `human_review_required`
- asset status counts:
  - `blocked_quality_review`: 38
  - `review_ready`: 13
  - `human_review_required`: 4
  - `source_wav_prepared_ogg_blocked`: 4

Canonical-source clarification for Nexo audio:

- `content/dungeons/dungeon_ruins_001/intake/nexo/sounds.json` is part of the
  canonical candidate set as the authoritative sound-event registry for DR001
  Nexo audio within this import.
- The four Ogg files under `intake/sounds/dr001/` remain canonical candidates
  through `intake/`, but they are still quality-gated by the intake manifest.
- This does not authorize runtime or staging promotion.

### DR001 path decisions

| Status | Path/group | Reason | Next allowed action | Forbidden action |
| --- | --- | --- | --- | --- |
| `keep` | `content/dungeons/dungeon_ruins_001/intake/schematics` | `intake/README.md` declares intake as the accepted final builder/artist/audio source. | Future focused quality/promotion review only. | Copy into runtime, Iris, WorldEdit, or staging paths now. |
| `keep` | `content/dungeons/dungeon_ruins_001/intake/nexo` | Final `dr001` namespace asset candidate set lives here. | Future focused quality/promotion review only. | Treat as already approved resource-pack output. |
| `keep` | `content/dungeons/dungeon_ruins_001/intake/modelengine` | Accepted final candidate set for `dr001_*.bbmodel`. | Future focused quality/promotion review only. | Promote as final art/runtime-ready models now. |
| `keep` | `content/dungeons/dungeon_ruins_001/intake/sounds` | Final audio candidate set lives here. | Future focused quality/promotion review only. | Treat as approved final audio now. |
| `keep` | `content/dungeons/dungeon_ruins_001/intake/sounds/dr001_source` | Source cues back the final intake audio set. | Preserve as source context for review. | Drop or replace without a separate source decision. |
| `keep` | `content/dungeons/dungeon_ruins_001/intake/manifest.yml` | Inventory and approval gate for the accepted intake set. | Use as intake review input only. | Treat as runtime promotion approval by itself. |
| `keep` | `content/dungeons/dungeon_ruins_001/intake/README.md` | Defines intake authority and approval gate. | Use as intake review input only. | Treat as production approval by itself. |
| `keep` | `content/dungeons/dungeon_ruins_001/mythicdungeons` | Source-like dungeon config, not identified as legacy scaffold. | Future focused quality/promotion review only. | Copy to active plugin/runtime paths now. |
| `keep` | `content/dungeons/dungeon_ruins_001/mythicmobs` | Source-like mobs/skills/drops config tied to DR001 IDs. | Future focused quality/promotion review only. | Copy to active plugin/runtime paths now. |
| `keep` | `content/dungeons/dungeon_ruins_001/mmoitems` | Source-like DR001 item/material config. | Future focused quality/promotion review only. | Copy to active plugin/runtime paths now. |
| `keep` | `content/dungeons/dungeon_ruins_001/mmocore` | Source-like DR001 RPG/progression hooks. | Future focused quality/promotion review only. | Copy to active plugin/runtime paths now. |
| `keep` | `content/dungeons/dungeon_ruins_001/nexo/blocks.yml` | Source config for DR001 block assets, distinct from generated pack output. | Future focused quality/promotion review only. | Treat generated pack output as the authority instead. |
| `keep` | `content/dungeons/dungeon_ruins_001/nexo/furniture.yml` | Source config for DR001 furniture assets. | Future focused quality/promotion review only. | Treat generated pack output as the authority instead. |
| `keep` | `content/dungeons/dungeon_ruins_001/nexo/items.yml` | Source config for DR001 item assets. | Future focused quality/promotion review only. | Treat generated pack output as the authority instead. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/README.md` | Useful orientation, but the file itself says this import is not canonical and not approved for staging/runtime use. | Extract concise context only. | Promote as active source-of-truth. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/manifest.yml` | Useful inventory/context, but mixed with quarantine labels and old staging context. | Extract inventory facts only. | Treat as canonical promotion manifest by itself. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/assets_to_create` | Requirements/checklists, not delivered approved assets. | Mine gaps/requirements only. | Treat as asset source. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/qa/*.md` | Historical decision and review context. | Extract lessons/snippets only. | Treat as current proof. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence` | Historical screenshots/waveforms/logs only. | Use as archive reference only. | Treat as current validation proof. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/ASSET_POLISH_REPORT.md` | Review narrative only. | Mine lessons/snippets only. | Promote whole file as truth. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/HYBRID_FINAL_ASSET_REPORT.md` | Historical staging-oriented review report. | Mine lessons/snippets only. | Promote as truth or runtime proof. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/CHANGELOG.md` | Historical execution log. | Mine lessons/snippets only. | Promote as authority. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/IMPLEMENTATION_NOTES.md` | Rich context, but mixed with staging/runtime procedure residue. | Mine lessons/snippets only. | Promote as active operational doc. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/README_ASSETS.md` | Asset requirement summary, not authority over accepted intake. | Extract requirement snippets only. | Treat as canonical source manifest. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/tools/validate_final_assets.mjs` | Potentially reusable validator logic. | Extract/rewrite into lease-aware tooling only. | Promote current script wholesale. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/tools/validate_references.mjs` | Potentially reusable validator logic. | Extract/rewrite into lease-aware tooling only. | Promote current script wholesale. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/tools/validate_mythicdungeons_map_readiness.mjs` | Potentially reusable validator logic. | Extract/rewrite into lease-aware tooling only. | Promote current script wholesale. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/tools/validate_mythicmobs_targeters.mjs` | Potentially reusable validator logic. | Extract/rewrite into lease-aware tooling only. | Promote current script wholesale. |
| `extract-only` | `content/dungeons/dungeon_ruins_001/tools/write_final_asset_reports.mjs` | Reporting logic may be reusable after rewrite. | Extract/rewrite into lease-aware tooling only. | Promote current script wholesale. |
| `dedup-needed` | `content/dungeons/dungeon_ruins_001/legacy_candidate_archive/nexo` | 24 duplicate relative paths overlap `resourcepack/assets/nexo`; authority unresolved. | Separate dedup/parity decision only. | Promote either side as authoritative now. |
| `dedup-needed` | `content/dungeons/dungeon_ruins_001/resourcepack/assets/nexo` | Overlaps archive candidate paths and is not yet the chosen authority. | Separate dedup/parity decision only. | Promote as authoritative source now. |
| `reject` | `content/dungeons/dungeon_ruins_001/legacy_candidate_archive` except `legacy_candidate_archive/nexo` | Already labeled archive-only historical candidate material. | Keep quarantined until later cleanup ticket. | Promote to active source/runtime. |
| `reject` | `content/dungeons/dungeon_ruins_001/modelengine/blueprints` | Explicitly identified as legacy/generated scaffold, not final intake. | Keep quarantined until later cleanup ticket. | Promote as active model source. |
| `reject` | `content/dungeons/dungeon_ruins_001/iris` | Scaffold/reference layer, not canonical candidate set for current promotion path. | Keep quarantined until later cleanup ticket. | Promote to active Iris/runtime paths. |
| `reject` | `content/dungeons/dungeon_ruins_001/resourcepack` except `resourcepack/assets/nexo` | Generated/output-oriented pack layer, not chosen source-of-truth. | Keep quarantined until later cleanup ticket. | Promote as canonical source. |
| `reject` | `content/dungeons/dungeon_ruins_001/staging` | Staging-specific residue only. | Keep quarantined until later cleanup ticket. | Promote to active source/runtime workflow. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/import_assets_to_staging.ps1` | Direct staging mutation helper tied to old flow. | Keep quarantined only. | Execute or promote into active scripts. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/prepare_config_only_staging_sync.ps1` | Direct staging mutation helper tied to old flow. | Keep quarantined only. | Execute or promote into active scripts. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/generate_assets.mjs` | Historical generator pipeline residue. | Keep quarantined only. | Promote as canonical source tooling. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/polish_assets.mjs` | Historical generator pipeline residue. | Keep quarantined only. | Promote as canonical source tooling. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/generate_hybrid_final_assets.mjs` | Historical generator pipeline residue. | Keep quarantined only. | Promote as canonical source tooling. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/generate_dr001_final_candidates.mjs` | Historical generator pipeline residue. | Keep quarantined only. | Promote as canonical source tooling. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/generate_visual_evidence.mjs` | Historical evidence-generation residue. | Keep quarantined only. | Promote as canonical source tooling. |
| `reject` | `content/dungeons/dungeon_ruins_001/tools/generate_dr001_sound_sources.mjs` | Historical generator pipeline residue. | Keep quarantined only. | Promote as canonical source tooling. |
| `reject` | `content/dungeons/dungeon_ruins_001/qa/*.log` | Ephemeral execution residue. | Keep quarantined only. | Treat as durable source/evidence. |
| `reject` | `content/dungeons/dungeon_ruins_001/qa/*last-result*.json` | Ephemeral execution residue. | Keep quarantined only. | Treat as durable source/evidence. |

### DR001 guardrails

1. No DR001 path may be copied into active source or runtime from this manifest alone.
2. `keep` means canonical candidate for future review, not approved promotion.
3. DR001 legacy Nexo authority is resolved: `resourcepack/assets/nexo` is the reference authority and `legacy_candidate_archive/nexo` is a non-authoritative shadow copy.
4. `reject` groups stay quarantined and may only move in a later cleanup/archive ticket.
5. `extract-only` material may be mined by snippet, never promoted wholesale.
6. This DR001 override supersedes older DR001 rows above where the statuses differ.
- `minecraft-dr001-asset-quality-review`

### DR001 Nexo authority override (2026-07-22)

This section supersedes earlier DR001 `dedup-needed` rows for
`legacy_candidate_archive/nexo` and `resourcepack/assets/nexo`.

- reference authority: `content/dungeons/dungeon_ruins_001/resourcepack/assets/nexo`
- non-authoritative shadow copy: `content/dungeons/dungeon_ruins_001/legacy_candidate_archive/nexo`

Resolution basis:

- 24 overlapping relative paths were found between the two trees.
- 16 overlapping files are byte-identical.
- 8 overlapping files differ, concentrated in furniture JSON and PNG texture files.
- `resourcepack/assets/nexo` preserves the packaged resource-pack-side legacy output and carries the extra `sounds.json` context file.
- `legacy_candidate_archive/` is already explicitly labeled as archived candidate material.

Decision effect:

- `resourcepack/assets/nexo` remains rejected for promotion, but it is the single authority for legacy-reference comparison of the old `dungeon_ruins_001` Nexo namespace.
- `legacy_candidate_archive/nexo` remains rejected for promotion and is now only an audit/provenance shadow copy.
- Neither tree is a canonical candidate and neither may be promoted to source, staging, or runtime from this decision.
