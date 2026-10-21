# Asset Review: mcmmo_elite_crypt_warden

Date:
2026-05-30

Reviewer:
Codex Blockbench validation attempt

Asset type:
Dungeon Boss

Status:
BLOCKBENCH_VALIDATION_FAIL

Files checked:
- `assets/blockbench/source/mcmmo_elite_crypt_warden/mcmmo_elite_crypt_warden.bbmodel`
- `assets/blockbench/source/mcmmo_elite_crypt_warden/textures/mcmmo_elite_crypt_warden.png`
- `assets/blockbench/source/mcmmo_elite_crypt_warden/README.md`
- `assets/blockbench/export/mcmmo_elite_crypt_warden/`
- `assets/modelengine/mcmmo_elite_crypt_warden/`
- `assets/nexo/mcmmo_elite_crypt_warden/`
- `assets/mythicmobs/mcmmo_elite_crypt_warden/`
- `qa/mcmmo_elite_crypt_warden_staging_smoke.md`
- `qa/mcmmo_elite_crypt_warden_blockbench_validation_report.md`
- `qa/evidence/mcmmo_elite_crypt_warden/blockbench_validation.json`
- `scripts/assets/validate_mcmmo_elite_crypt_warden_packet.py`

Blockbench validation:
- result: FAIL - BLOCKBENCH_OPENED_BUT_ASSET_INVALID
- executable found: yes
- executable path: `C:\Users\przemek\AppData\Local\Programs\Blockbench\Blockbench.exe`
- launch attempted: yes, with `C:\Codex\Minecraft\assets\blockbench\source\mcmmo_elite_crypt_warden\mcmmo_elite_crypt_warden.bbmodel`
- screenshot: `qa/evidence/mcmmo_elite_crypt_warden/blockbench_validation_screenshot_topmost.png`
- window capture: `qa/evidence/mcmmo_elite_crypt_warden/blockbench_window_capture.png`
- evidence json: `qa/evidence/mcmmo_elite_crypt_warden/blockbench_validation.json`
- report: `qa/mcmmo_elite_crypt_warden_blockbench_validation_report.md`
- important: Blockbench opened the `.bbmodel`, but the captured UI evidence does
  not show a visible model, visible texture, animation panel, or required
  animations. This is a failed asset validation, not an approval.

Visual review:
- silhouette: Needs fixes. Rebuilt draft now has a humanoid boss silhouette:
  broad torso/chest plate, distinct helmet/head, massive shoulders, two arms,
  two legs/feet, visible hammer/maul weapon, rune chest core, back plate, and
  void/echo crystal accents. It still needs manual Blockbench art review.
- readability: Needs fixes. Major combat parts are named and separated, but
  proportions need manual inspection against a player scale reference.
- texture: Needs fixes. 64x64 draft texture exists with dark stone, old metal,
  purple/blue void accents, and bright rune accents; UV/art pass is still needed.
- scale: planned 3.5-4.5 blocks, not verified in-game.
- style match: promising crypt/ruins fantasy direction, pending art pass.

Technical review:
- format: Needs fixes. `.bbmodel` JSON exists in Blockbench free/model style and
  was rebuilt to match existing DR001 animated examples: top-level `groups`,
  UUID-based `outliner`, and animation animators keyed to group UUIDs. It should
  still be manually opened in Blockbench before import approval.
- bones: Draft includes required groups: `root`, `body`, `chest_core`, `head`,
  `left_shoulder`, `left_arm`, `right_shoulder`, `right_arm`, `weapon`,
  `fx_core`, `left_leg`, `right_leg`, `hitbox_core`.
- pivots: Draft pivots exist for movable parts; manual Blockbench pivot review is
  required before ModelEngine import.
- animations: Draft includes `idle`, `walk`, `attack_01`, `attack_02`, `cast`,
  `hurt`, `death`, `spawn`, and `special_01` in the same object/UUID animator
  structure used by existing repo `.bbmodel` files with visible Blockbench UI
  animations.
- export: not exported.
- ModelEngine packet: prepared with import notes, manifest, animation mapping,
  expected model id, texture references, and pending runtime items.
- MythicMobs packet: prepared with `MCMMOEliteCryptWarden` mob stub and matching
  skill stubs for spawn, attacks, void cast, special tell, hurt, and death.
- Nexo/resource pack packet: prepared with ModelEngine pack merge notes and an
  optional `mcmmo_elite_crypt_shard` item stub. Boss entity model remains a
  ModelEngine model, not a separate Nexo entity item.
- import: not imported.

Gameplay review:
- hitbox: Needs fixes. `hitbox_core` is a visual/reference helper only; actual
  runtime hitbox must be configured and tested through ModelEngine/MythicMobs.
- attack readability: Needs fixes. Draft wind-ups exist, but combat readability
  must be reviewed in Blockbench and staging.
- damage timing: Draft suggestions are documented in README; not verified.
- player fairness: not tested.
- boss tells: Draft core pulse and shoulder/body tells exist; not validated.

Performance review:
- texture size: 64x64 draft PNG.
- cube/bone count: 26 cubes, 13 groups/bones.
- expected simultaneous instances: 1-3.
- TPS/MSPT notes: no staging evidence yet.

Staging evidence:
- server: not tested.
- date: not tested.
- commands: none.
- latest.log: not checked.
- screenshots: none.
- issues: manual Blockbench open, ModelEngine import, resource-pack generation,
  and staging spawn test are still required.
- smoke packet: `qa/mcmmo_elite_crypt_warden_staging_smoke.md` prepared.

Decision:
BLOCKBENCH_VALIDATION_FAIL

Required fixes:
1. Rebuild the `.bbmodel` from a valid animated Blockbench template and verify
   that the model, texture, animation panel, and required animations are visible
   in Blockbench.
2. Open the `.bbmodel` in Blockbench and correct any import warnings, pivots, UVs,
   or animation interpolation issues.
3. Perform an art pass on proportions, silhouette, material breakup, and texture.
4. Export/import through ModelEngine v4 and verify all required animations only
   after Blockbench validation passes.
5. Copy prepared MythicMobs draft files into staging and verify skill hooks and
   final damage timings.
6. Generate/load ModelEngine resource pack assets and verify Nexo pack merge if
   this boss is shipped through the server resource pack.
7. Run staging spawn test, capture screenshot/log evidence, and review `latest.log`
   before any approval.
