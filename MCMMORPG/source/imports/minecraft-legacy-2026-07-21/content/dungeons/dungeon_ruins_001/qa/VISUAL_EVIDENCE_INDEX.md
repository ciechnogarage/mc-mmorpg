# DR001 Visual Evidence Index

Date: 2026-05-30
Target: `Server — kopia/1.21.11` staging only
Decision: `human_review_required`
Verdict: `VISUAL_EVIDENCE_PARTIAL_NEEDS_FIXES`

## Preview Capability Discovery

Checked local preview paths before generating evidence:

| Capability | Local result | Used |
| --- | --- | ---: |
| Real Minecraft client / launcher | `MinecraftInstaller` folder found, no scriptable `minecraft-launcher` command found on PATH | NO |
| Prismarine viewer / Mineflayer | `tools/playtest-bot/node_modules` contains `mineflayer`, `prismarine-viewer`, and `canvas`; previous QA bot runtime artifacts exist | PARTIAL |
| Browser automation | no local Playwright/Puppeteer package in the playtest bot; no local viewer harness for DR001 screenshots found | NO |
| Blockbench | no `blockbench` / `Blockbench` command found on PATH | NO |
| Blender | `C:\Program Files\Blender Foundation\Blender 5.1\blender.exe` found; no local `.bbmodel` import pipeline found | NO |
| Python imaging tools | Python exists; `PIL`, `numpy`, `matplotlib`, `scipy`, `nbtlib`, `amulet`, and `litemapy` not installed | NO |
| Schematic preview tools | repo-local `prismarine-nbt` can parse `.schem` files | YES |
| Existing screenshot/playtest scripts | Mineflayer runtime scripts exist; previous runtime evidence has no true screenshot capture | PARTIAL |

Chosen preview strategy: generated contact-sheet and preview renders from local asset files, plus prior staging runtime log evidence. This is the strongest safe local path found without downloading tools or using internet services.

## Generated Evidence

Manifest: `content/dungeons/dungeon_ruins_001/qa/visual_evidence/metadata/visual-evidence-manifest.json`

| Category | Asset | Evidence path | Result | Notes |
| --- | --- | --- | ---: | --- |
| Boss model | `DR001_GatewardenVaros` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/bosses/DR001_GatewardenVaros.png` | PASS_PREVIEW | Rendered from `intake/modelengine/dr001_gatewarden_varos.bbmodel` with embedded texture shown. |
| Boss model | `DR001_CisternOracleNhalruun` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/bosses/DR001_CisternOracleNhalruun.png` | PASS_PREVIEW | Rendered from `intake/modelengine/dr001_nhalruun.bbmodel` with embedded texture shown. |
| Boss model | `DR001_RuinedKingAurelion` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/bosses/DR001_RuinedKingAurelion.png` | PASS_PREVIEW | Rendered from `intake/modelengine/dr001_ruined_king.bbmodel` with embedded texture shown. |
| Nexo | `dr001_rune_cracked_stone` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/nexo/dr001_rune_cracked_stone.png` | PASS_PREVIEW | Model JSON and texture preview generated from final intake path. Runtime placement remains unproven. |
| Nexo | `dr001_rune_pedestal` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/nexo/dr001_rune_pedestal.png` | PASS_PREVIEW | Furniture model and texture preview generated. Runtime placement remains unproven. |
| Nexo | `dr001_void_anchor` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/nexo/dr001_void_anchor.png` | PASS_PREVIEW | Furniture model and texture preview generated. Runtime placement remains unproven. |
| Nexo | `dr001_relic_shard` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/nexo/dr001_relic_shard.png` | PASS_PREVIEW | Item model fallback cube and texture preview generated. |
| Nexo textures | all DR001 final-intake Nexo textures | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/nexo/dr001_nexo_texture_contact_sheet.png` | PASS_PREVIEW | Contact sheet covers all final-intake DR001 Nexo PNG textures. |
| Schematic | `gatehouse_broken_01.schem` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/schematics/gatehouse_broken_01_overview.png` | PASS_PREVIEW | Top-down highest-solid-block overview from parsed `.schem`; not a pasted in-game review. |
| Schematic | `varos_gate_arena_01.schem` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/schematics/varos_gate_arena_01_overview.png` | PASS_PREVIEW | Boss arena schematic overview from parsed `.schem`; not a pasted in-game review. |
| Sound | `nexo:dr001.roots_whisper` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/roots_whisper_waveform.png`, `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/roots_whisper_spectrogram.png` | PASS_PREVIEW | Waveform/spectrogram generated from final Ogg. Previous runtime playback succeeded. |
| Sound | `nexo:dr001.cistern_geyser` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/cistern_geyser_waveform.png`, `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/cistern_geyser_spectrogram.png` | PASS_PREVIEW | Waveform/spectrogram generated from final Ogg. Previous runtime playback succeeded. |
| Sound | `nexo:dr001.void_crown_pulse` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/void_crown_pulse_waveform.png`, `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/void_crown_pulse_spectrogram.png` | PASS_PREVIEW | Waveform/spectrogram generated from final Ogg. Previous runtime playback succeeded. |
| Sound | `nexo:dr001.aurelion_enrage` | `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/aurelion_enrage_waveform.png`, `content/dungeons/dungeon_ruins_001/qa/visual_evidence/audio/aurelion_enrage_spectrogram.png` | PASS_PREVIEW | Waveform/spectrogram generated from final Ogg. Previous runtime playback succeeded. |

## Boss Runtime Visual Test

Runtime source: `qa/dr001-runtime-smoke/boss-spawn-smoke-20260530T173934Z.json`

| Boss | Runtime spawn | Visual evidence | Cleanup | Result |
| --- | ---: | --- | ---: | ---: |
| `DR001_GatewardenVaros` | PASS | generated `.bbmodel` preview PNG | PASS | PARTIAL_OWNER_REVIEW |
| `DR001_CisternOracleNhalruun` | PASS | generated `.bbmodel` preview PNG | PASS | PARTIAL_OWNER_REVIEW |
| `DR001_RuinedKingAurelion` | PASS | generated `.bbmodel` preview PNG | PASS | PARTIAL_OWNER_REVIEW |

Notes: staging spawn commands returned success and cleanup checks found no remaining scoped boss entities. The prior runtime probe still reports `SPAWN_EVIDENCE_MISSING_CLEANUP_OK`, so generated previews do not replace real client screenshots for final approval.

## Nexo Visual Test

Runtime source: `qa/dr001-runtime-smoke/nexo-placement-review.json`

| Asset | Runtime placement/preview | Evidence | Result | Notes |
| --- | --- | --- | ---: | --- |
| `dr001_rune_cracked_stone` | Registry/give passed; placement not proven | generated model/texture preview | PARTIAL | Needs client or corrected placement proof. |
| `dr001_rune_pedestal` | Registry/give passed; placement not proven | generated model/texture preview | PARTIAL | Needs furniture placement proof. |
| `dr001_void_anchor` | Registry/give passed; placement not proven | generated model/texture preview | PARTIAL | Needs furniture placement proof. |
| `dr001_relic_shard` | Registry/give passed | generated item/texture preview | PASS_PREVIEW | Placement is not required for simple item visual preview. |

## Schematic Visual Test

| Schematic | Paste/preview | Evidence | Result | Notes |
| --- | --- | --- | ---: | --- |
| `gatehouse_broken_01.schem` | parsed preview only | `visual_evidence/schematics/gatehouse_broken_01_overview.png` | PARTIAL | Dimensions `43x25x35`; safe paste was not executed. |
| `varos_gate_arena_01.schem` | parsed preview only | `visual_evidence/schematics/varos_gate_arena_01_overview.png` | PARTIAL | Dimensions `61x27x51`; selected as representative boss arena. |

## Sound Evidence

Runtime playback source: `content/dungeons/dungeon_ruins_001/qa/RUNTIME_ASSET_SMOKE_REPORT.md`

| Sound | Playback/waveform evidence | Result |
| --- | --- | ---: |
| `nexo:dr001.roots_whisper` | previous `/playsound` succeeded; waveform and spectrogram generated | PASS_PREVIEW |
| `nexo:dr001.cistern_geyser` | previous `/playsound` succeeded; waveform and spectrogram generated | PASS_PREVIEW |
| `nexo:dr001.void_crown_pulse` | previous `/playsound` succeeded; waveform and spectrogram generated | PASS_PREVIEW |
| `nexo:dr001.aurelion_enrage` | previous `/playsound` succeeded; waveform and spectrogram generated | PASS_PREVIEW |

## Remaining Blockers

| Blocker | Required fix |
| --- | --- |
| Prism real-client pass blocked by no compatible local instance | Create or approve a `1.21.11` Prism staging QA instance, then rerun real-client screenshots. Evidence: `visual_evidence/real_client/screen_after_direct_connect.png`; report: `REAL_CLIENT_VISUAL_REVIEW.md`. |
| No real client screenshot evidence | Run a staging client screenshot pass or add a local automated client/viewer screenshot harness. |
| Nexo placement not proven | Fix or document exact Nexo placement path, then capture block/furniture placement and cleanup evidence. |
| Schematics not pasted in staging | Use a deterministic isolated WorldEdit/FAWE paste and cleanup flow, or have a builder approve generated schematic previews as sufficient. |
| Generated model/texture quality remains subjective | Owner/art review must accept or request replacement for procedural candidates. |

## Files Generated

The preview artifacts under `content/dungeons/dungeon_ruins_001/qa/visual_evidence/` are evidence files for owner visual review, not fake client screenshots.
