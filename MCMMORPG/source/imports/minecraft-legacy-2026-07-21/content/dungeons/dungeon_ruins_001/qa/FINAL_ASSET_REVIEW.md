# DR001 Final Asset Review

Date: 2026-05-30
Verdict: FINAL_AUDIO_READY_HUMAN_REVIEW_REQUIRED

## Summary

- Created DR001-named local technical candidates for schematics, ModelEngine, and Nexo resource-pack assets.
- Archived legacy `dungeon_ruins_001` intake candidates under `legacy_candidate_archive/` so final intake no longer leaks the old namespace.
- Prepared four synthetic local WAV source cues under `intake/sounds/dr001_source/`.
- Converted the four required final Ogg Vorbis files under `intake/sounds/dr001/` with local `ffmpeg` / `libvorbis` q5.
- Did not mark the manifest `approved_final`; human build/art/audio review remains required.

## Promoted Or Created

- Schematics: 10 gzipped `.schem` files with final required names.
- ModelEngine: 15 `dr001_*.bbmodel` technical candidates with required animation aliases and explicit hitboxes.
- Nexo: 13 final-namespace JSON models and 13 PNG textures under `intake/nexo/.../dr001/`.
- Sounds: 4 final Ogg Vorbis files plus 4 WAV source cues with real audio content, peak/RMS measurements, and SHA-256 checksums.
- Manifest: real SHA-256 values for created files, source WAVs, and final Ogg files.

## Rejected

- Legacy ModelEngine `dungeon_ruins_001_*.bbmodel` files: archived, not final.
- Legacy Nexo `dungeon_ruins_001` model/texture paths: archived, not final.
- Legacy generated Ogg candidates: archived, not final and not treated as real authored audio.
- Old unprefixed schematic candidates not in the final validator list: archived as reference only.

## Structured Quality Review

| Area | Result | Evidence | Review Status |
| --- | --- | --- | --- |
| Schematics | 10/10 present, non-empty, gzip-inspectable `.schem` files with expected final names. | `validate_final_assets.mjs --allow-review-candidates` checked all 10. | `blocked_quality_review`: builder review still required for scale, routing, landmarks, and encounter readability. |
| ModelEngine | 15/15 `.bbmodel` files parse as JSON, include required animation aliases, and include non-empty hitbox data. | `validate_final_assets.mjs --allow-review-candidates` checked all 15. | `blocked_quality_review`: technical import candidates only; final silhouette/art pass is not acceptable yet for boss/mob approval. |
| Nexo models | 13/13 required model JSON files parse; `sounds.json` also parses. | Custom JSON/texture reference scan found no bad JSON and no missing texture refs. | `review_ready` for JSON structure only. |
| Nexo textures | 13/13 PNGs have valid PNG signatures and referenced paths resolve. | Custom PNG scan found no invalid PNG headers. | `blocked_quality_review`: generated procedural textures remain art-review candidates; at least the weakest texture pass should be replaced or explicitly accepted by a human reviewer before final approval. |
| Sounds | 4/4 final `.ogg` files present, non-zero size, Vorbis codec, mono 44.1 kHz, duration preserved, decoded peak/RMS non-silent. | `ffprobe`, `ffmpeg astats`, `qa/DR001_SOURCE_AUDIO_NOTES.md`, and `validate_final_assets.mjs --allow-review-candidates`. | `human_review_required`: technically valid final Ogg files, still need human audio acceptance before approved_final. |
| Namespace | No `dungeon_ruins_001` legacy files remain in active final intake. | Custom intake scan reported `legacyIntake: []`; validator `legacyRejected: []`. | Pass for active intake namespace. |

## Quality Concerns

- ModelEngine files are technical import candidates, not final bespoke boss/mob art.
- Procedural schematics need builder inspection for gameplay scale, landmark composition, and route readability.
- Procedural Nexo textures are coherent with the DR001 palette but still need art review.
- Audio technical finalization is complete; audio acceptance remains human review.

## Remaining Work

- Human review or replace final Ogg files: `roots_whisper`, `cistern_geyser`, `void_crown_pulse`, `aurelion_enrage`.
- Human review or replace ModelEngine silhouettes for Varos, Nhal'Ruun, Aurelion, and trash/elite mobs.
- Builder review or replace schematics against the dungeon layout and encounter pacing.
- Approve final manifest only after quality review passes and all checksums are present.

## Real Client Review Attempt

Date: 2026-05-30
Verdict: `REAL_CLIENT_VISUAL_REVIEW_BLOCKED_NO_INSTANCE`

Prism was launched from `C:\Codex\PrismLauncher-Windows-MinGW-w64-Portable-11.0.2\prismlauncher.exe`, using the only local instance, `26.1.2`. The real client attempted `127.0.0.1:25566` through Velocity, but staging rejected it as incompatible with `1.21.11`.

No final-asset status changed. DR001 remains `human_review_required` until a compatible real client captures boss, Nexo, schematic, and owner-review screenshots.

## Staging Human-Visual / Build-Review Addendum

Date: 2026-05-30
Verdict: `FINAL_VISUAL_REVIEW_PARTIAL_HUMAN_REVIEW_REQUIRED`

What changed:

- Corrected `content/dungeons/dungeon_ruins_001/nexo/blocks.yml` from invalid `type: note_block` to documented/runtime-accepted `type: NOTEBLOCK`.
- Installed the DR001 Nexo source candidate configs into staging under `plugins/Nexo/items/dr001_qa_review/` for this bounded QA pass.
- Confirmed Nexo item registry/give for representative block, item, and furniture IDs.
- Attempted block/furniture placement with Mineflayer; placement did not produce block update or display/entity evidence.
- Spawned all three DR001 bosses while the QA bot observed; spawn commands succeeded, bot observed DR001 mechanic chat and ModelEngine display entities, but no screenshot capture was available.
- Confirmed zero active legacy `dungeon_ruins_001_*` namespace entries in the generated Nexo pack.

Decision:

Do not promote to `approved_final`. The current evidence supports continued staging review only. Required fixes before promotion:

- Real client screenshot pass or equivalent visual capture for boss models and representative Nexo assets.
- Working Nexo placement path for at least one block and one furniture/decor asset, with cleanup evidence.
- Safe FAWE/WorldEdit or builder-accepted schematic preview for representative room and boss/transition structure.
- Human acceptance of procedural model/texture quality.
