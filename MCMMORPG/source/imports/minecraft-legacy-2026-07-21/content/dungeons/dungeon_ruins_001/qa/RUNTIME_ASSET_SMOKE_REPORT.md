# DR001 Runtime Asset Smoke Report

Date: 2026-05-30
Target: `Server — kopia/1.21.11`
Scope: staging-only final intake candidate runtime smoke

## Summary

DR001 final intake candidates were imported to staging in review-candidate mode without changing final approval status. The Nexo resource pack regenerated, ModelEngine imported the DR001 blueprints, MythicMobs reloaded, the QA bot accepted the regenerated resource pack, three DR001 bosses were spawned through the narrow staging RCON allowlist, and all four DR001 custom sounds were played to the QA bot.

The runtime smoke is technically useful but not final art approval. The intake manifest remains `human_review_required`, and visual quality/build review remains required.

## Asset Sync

- Dry-run: `import_assets_to_staging.ps1 -UseFinalIntake -AllowReviewCandidates` reported a clean staging-only plan.
- Apply: `import_assets_to_staging.ps1 -UseFinalIntake -AllowReviewCandidates -Apply` copied 57 files.
- Follow-up dry-run after apply: copied `0`, skipped `57`, blocked `0`.
- Review-candidate mode used `validate_final_assets.mjs --allow-review-candidates`; strict final approval was not bypassed.

## Nexo Resource Pack

- `nexo reload pack` returned `Resourcepack successfully regenerated`.
- Runtime logs show:
  - `Nexo | Generating resourcepack...`
  - `Nexo | Imported ModelEngine pack successfully!`
  - `Nexo | Validating ResourcePack files...`
  - `Nexo | Copied generated ResourcePack to plugins\Nexo\pack\pack.zip`
  - `Nexo | Finished generating resourcepack!`
- The generated `pack.zip` contains final `assets/nexo/models/item/dr001`, `assets/nexo/textures/item/dr001`, `assets/nexo/sounds/dr001`, and `assets/modelengine/.../dr001_*` entries.
- Stale `dungeon_ruins_001_*` staging assets were found during the first reload, archived to staging backup, and removed from the regenerated active pack.

## ModelEngine

- `meg reload models` was run using locally documented syntax.
- First reload loaded 28 models and exposed stale legacy `dungeon_ruins_001_*` blueprints in staging.
- After archiving legacy files, reload imported 15 `dr001_*` blueprints plus 2 unrelated existing blueprints (`internal_fire`, `rust_raider`), then reported `17 models loaded`.
- No DR001 missing model, missing texture, missing bone, or JSON parse errors were found in the post-cleanup reload log window.

## Boss Smoke

| Boss ID | Spawn | Model/visual evidence | Runtime errors | Cleanup | Notes |
| --- | ---: | --- | ---: | ---: | --- |
| `DR001_GatewardenVaros` | PASS | MythicMobs info resolved `IRON_GOLEM`; spawn returned success; QA bot snapshots saw spawned unknown entity during hold | 0 DR001 asset errors found | PASS | Vanilla probe used the wrong y-level first; active list later confirmed boss presence before cleanup. |
| `DR001_CisternOracleNhalruun` | PASS | MythicMobs info resolved `DROWNED`; spawn returned success; active list confirmed presence | 0 DR001 asset errors found | PASS | Spawn settled near existing floor around `y=59`. |
| `DR001_RuinedKingAurelion` | PASS | MythicMobs info resolved `WITHER_SKELETON`; spawn returned success; active list confirmed presence | 0 DR001 asset errors found | PASS | QA bot observed boss chat/mechanic text during hold. |

Cleanup evidence: `mm mobs listactive` returned `ActiveMobs Found (0 total)` after scoped cleanup.

## Nexo Prop/Item Smoke

| Asset | Runtime test | Result | Notes |
| --- | ---: | ---: | --- |
| `dr001` model JSON/texture set | Resource-pack generation and zip inspection | PASS | 13 JSON models and 13 PNG textures landed in pack paths and generated pack. |
| Nexo item/block/furniture placement | Not run | REVIEW REQUIRED | No already-approved scoped Nexo give/place command existed for this smoke. |

## Schematic/Structure Smoke

| Schematic | Runtime test | Result | Notes |
| --- | ---: | ---: | --- |
| 10 DR001 `.schem` files | Import path and validator checks | PASS, NOT PASTED | Files landed under `plugins/Iris/packs/dungeon_ruins_001/schematics`; no safe paste command was used. |

## Sound Smoke

| Sound | Runtime/path/playback test | Result | Notes |
| --- | ---: | ---: | --- |
| `nexo:dr001.roots_whisper` | Pack path, Nexo registry, `/playsound` to QA bot | PASS | RCON: played to `Dungeon001QA_01`. |
| `nexo:dr001.cistern_geyser` | Pack path, Nexo registry, `/playsound` to QA bot | PASS | RCON: played to `Dungeon001QA_01`. |
| `nexo:dr001.void_crown_pulse` | Pack path, Nexo registry, `/playsound` to QA bot | PASS | RCON: played to `Dungeon001QA_01`. |
| `nexo:dr001.aurelion_enrage` | Pack path, Nexo registry, `/playsound` to QA bot | PASS | RCON: played to `Dungeon001QA_01`. |

## Runtime Log Findings

| Severity | Finding | Evidence |
| --- | --- | --- |
| WARNING | Initial reload included stale legacy `dungeon_ruins_001_*` staging assets. | ModelEngine imported legacy blueprints; generated pack contained legacy Nexo paths. Archived and retested clean. |
| ACCEPTABLE_REVIEW_ITEM | Nexo uploaded staging pack through normal plugin behavior. | `Nexo | ResourcePack has been uploaded to https://atlas.nexomc.com/...`; no manual publish was run. |
| ACCEPTABLE_REVIEW_ITEM | ModelEngine reports `Skin URL is null` when the Mineflayer QA bot joins. | Repeated join-time warning tied to bot skin lookup, not DR001 model import. |
| ACCEPTABLE_REVIEW_ITEM | Existing unrelated update notices appear in bot chat. | CoreTools/Guilds/FAWE/CMI/DecentHolograms update notices. |

## Human Review Checklist

- Builder review of 10 schematic candidates in an isolated QA paste area.
- Art review of 15 generated ModelEngine candidates in client.
- Art review of 13 Nexo textures; prior notes still flag texture quality review.
- Nexo block/item/furniture placement smoke with exact safe commands once item config path is approved.
- Optional player screenshot pass with a real client for final visual approval.

## Recommendation

Runtime technical loading is good enough to continue review, but not enough for `approved_final`. Keep `human_review_required` until art/build review accepts the generated candidates and schematic placement is visually checked.

## Real Client Prism Attempt

Date: 2026-05-30
Verdict: `REAL_CLIENT_VISUAL_REVIEW_BLOCKED_NO_INSTANCE`

Prism Launcher was found at `C:\Codex\PrismLauncher-Windows-MinGW-w64-Portable-11.0.2\prismlauncher.exe` and launched the only local instance, `26.1.2`. Auth/session refresh succeeded and a real Minecraft client attempted to connect through Velocity at `127.0.0.1:25566`.

The client was rejected before joining staging: `Unable to connect to loch: Outdated server! I'm still on 1.21.11`. Local Prism inspection found no compatible `1.21.11` instance or cached `1.21.11` client files, so no boss/Nexo/schematic in-world screenshots could be captured in this pass.

Evidence report: `content/dungeons/dungeon_ruins_001/qa/REAL_CLIENT_VISUAL_REVIEW.md`.
Blocking screenshot: `content/dungeons/dungeon_ruins_001/qa/visual_evidence/real_client/screen_after_direct_connect.png`.

## DR001 Human-Visual / Build-Review Staging Pass

Date: 2026-05-30
Verdict: `FINAL_VISUAL_REVIEW_PARTIAL_HUMAN_REVIEW_REQUIRED`

### Scope

- Target: `Server — kopia/1.21.11` staging only.
- QA area: `minecraft:overworld` / `world`, center `0.5 128 64.5`, allowed radius `48`.
- Temporary platform: `fill -8 127 56 9 127 73 minecraft:smooth_stone replace minecraft:air`; cleanup later removed `324` smooth stone blocks.
- Production/live, rewards/economy, public pack publish, and full dungeon flow were not run.

### Boss Runtime / Visual Evidence

The QA bot connected through local Velocity, accepted the Nexo resource pack, and held the arena while all three DR001 bosses were spawned one at a time. RCON spawn commands returned success for all three bosses. The height-specific entity probe at `y=128` missed all three after they fell/settled, so the boss script reported `SPAWN_EVIDENCE_MISSING_CLEANUP_OK`; however, the QA bot observed boss mechanic chat and nearby ModelEngine display entities below the spawn height. `mm mobs listactive` reported `ActiveMobs Found (0 total)` after scoped cleanup.

Screenshot capture was not available in this tooling pass. This is runtime evidence, not human visual acceptance.

Artifact: `qa/dr001-runtime-smoke/boss-spawn-smoke-20260530T173934Z.json`

### Nexo Block / Item / Furniture Review

Staging had DR001 Nexo pack assets but no active DR001 item YAML under `plugins/Nexo/items`. For this staging-only QA pass, the source candidate configs were copied to `plugins/Nexo/items/dr001_qa_review/`, and Nexo was reloaded.

Findings:

- Initial block config used `type: note_block`; Nexo rejected it and logged valid types `NOTEBLOCK`, `STRINGBLOCK`, `CHORUSBLOCK`.
- Source and staging QA config were corrected to `type: NOTEBLOCK`.
- `nexo reload items` then succeeded without the custom-block type error.
- `nexo iteminfo` resolved:
  - `dr001_rune_cracked_stone` -> `nexo:item/dr001/block/rune_cracked_stone`
  - `dr001_relic_shard` -> `nexo:item/dr001/item/relic_shard`
  - `dr001_rune_pedestal` -> `nexo:item/dr001/furniture/rune_pedestal`
  - `dr001_void_anchor` -> `nexo:item/dr001/furniture/void_anchor`
- `nexo blockinfo dr001_rune_cracked_stone` resolved the item ID.
- `nexo give` successfully gave the four representative assets to `Dungeon001QA_01`.
- Mineflayer placement attempts for `dr001_rune_cracked_stone` and `dr001_rune_pedestal` failed: no expected block update fired, and post-probes found no `item_display`, `block_display`, or `interaction` entity near the placement points.

Result: Nexo registry/give/pack path review is PASS, but Nexo placement is NOT ACCEPTED for final status.

Artifact: `qa/dr001-runtime-smoke/nexo-placement-review.json`

### Schematic Review

Representative DR001 final schematics exist in source and staging Iris paths, including `gatehouse_broken_01.schem`, `varos_gate_arena_01.schem`, `nhalruun_oracle_chamber_01.schem`, and `void_sanctum_arena_01.schem`. FAWE schematic command syntax was checked in the local docs mirror, but no safe scoped paste command was executed in this pass because player-context WorldEdit paste, origin control, and cleanup were not made sufficiently deterministic for the approved QA area.

Result: technical file/path review only; builder visual review still required.

### Sound Review

Previous runtime evidence remains valid: all four DR001 sounds played successfully to the QA bot. A repeat `playsound` for `nexo:dr001.roots_whisper` was attempted after the later bot had disconnected and returned `No player was found`; this does not invalidate the earlier successful sound smoke.

Result: audio technical status PASS; subjective audio review still requires human acceptance.

### Runtime Findings

| Severity | Finding | Evidence |
| --- | --- | --- |
| BLOCK_FINAL_APPROVAL | Screenshot/human visual acceptance is still missing. | No render screenshots were available from Mineflayer/RCON tooling. |
| BLOCK_FINAL_APPROVAL | Nexo placement is not proven. | `dr001_rune_cracked_stone` and `dr001_rune_pedestal` give succeeded, but placement calls timed out waiting for block updates and probes found no placed display/interaction entities. |
| BLOCK_FINAL_APPROVAL | Schematics were not pasted or visually reviewed. | FAWE syntax inspected; paste skipped due cleanup/origin safety. |
| FIXED_FOR_SOURCE | DR001 Nexo block config used invalid `note_block` type. | Nexo runtime rejected it; source now uses documented `NOTEBLOCK`. |
| ACCEPTABLE_REVIEW_ITEM | Boss spawns returned success and bot observed DR001 mechanics/model display entities, but y=128 probes missed settled entities. | Boss artifact and Mineflayer arena hold JSON. |
| ACCEPTABLE_REVIEW_ITEM | ModelEngine `Skin URL is null` repeats on Mineflayer joins. | Join-time warning tied to bot skin lookup, not DR001 asset import. |
| PASS | Active Nexo pack scan found zero legacy `dungeon_ruins_001_*` entries. | `LEGACY_NAMESPACE_ENTRY_COUNT=0`. |

### Decision

Keep `human_review_required`. The bundle must not move to `approved_final` because runtime placement, schematic visual review, and screenshot/human art acceptance are still missing.
