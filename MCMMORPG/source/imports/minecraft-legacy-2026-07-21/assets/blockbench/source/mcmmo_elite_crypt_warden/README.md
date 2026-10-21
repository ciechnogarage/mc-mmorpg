# Model: mcmmo_elite_crypt_warden

Status:
DRAFT - TECHNICAL PACKET PREPARED

Type:
Dungeon Boss

Purpose:
Production draft source asset for an elite crypt warden boss. The model is a
real Blockbench `.bbmodel` draft intended as a base for art pass, ModelEngine v4
import, MythicMobs animation hooks, and Nexo/resource-pack integration.

Biome/Dungeon/Faction:
crypt / ruins / dark dungeon warden

Pipeline:
Blockbench -> ModelEngine v4 -> MythicMobs -> Nexo/resource pack -> Paper/Purpur 1.21.x

Scale:
Draft target: about 3.5-4.5 blocks tall. Exact scale still needs a Blockbench
player reference screenshot and staging spawn comparison.

Files:
- source: `assets/blockbench/source/mcmmo_elite_crypt_warden/mcmmo_elite_crypt_warden.bbmodel`
- texture: `assets/blockbench/source/mcmmo_elite_crypt_warden/textures/mcmmo_elite_crypt_warden.png`
- export: `assets/blockbench/export/mcmmo_elite_crypt_warden/` (not exported yet)
- ModelEngine: `assets/modelengine/mcmmo_elite_crypt_warden/` (packet prepared, not imported yet)
- Nexo: `assets/nexo/mcmmo_elite_crypt_warden/` (ModelEngine pack merge notes plus optional shard stub prepared)
- MythicMobs: `assets/mythicmobs/mcmmo_elite_crypt_warden/` (mob and skill stubs prepared)
- QA staging smoke: `qa/mcmmo_elite_crypt_warden_staging_smoke.md`
- review: `docs/assets/reviews/mcmmo_elite_crypt_warden_review.md`

Pipeline status:
- Blockbench draft: exists
- Texture draft: exists
- ModelEngine packet: prepared
- MythicMobs stub: prepared
- Nexo/resource pack: prepared as notes plus optional `mcmmo_elite_crypt_shard` item stub; boss entity model is expected to ship through ModelEngine's generated pack and Nexo's ModelEngine pack import/merge
- Staging: pending
- Approval: no

Bones/groups:
- `root`
- `body`
- `chest_core`
- `head`
- `left_shoulder`
- `left_arm`
- `right_shoulder`
- `right_arm`
- `weapon`
- `fx_core`
- `left_leg`
- `right_leg`
- `hitbox_core`

Blockbench format notes:
- Rebuilt as Blockbench `free` model JSON with top-level `groups`, UUID-based
  `outliner`, local/default texture, and animation animators keyed to group UUIDs.
- Animation structure was matched to existing DR001 `.bbmodel` examples that
  expose animations in the Blockbench Animations panel.
- Texture is embedded as a data URL for Blockbench visibility and also saved as
  a local PNG beside the source model.

Animations:
| Name | Duration | Loop | Used by skill | Notes |
| --- | --- | --- | --- | --- |
| `idle` | 1.6s | yes | TBD | Subtle body lift, head motion, and core pulse. |
| `walk` | 1.0s | yes | TBD | Basic opposing arm/leg locomotion cycle. |
| `attack_01` | 1.2s | no | TBD | Maul swing; suggested damage timing around 0.7s. |
| `attack_02` | 1.6s | no | TBD | Heavy war-fist slam; suggested damage timing around 0.95s. |
| `cast` | 1.5s | no | TBD | Arms draw inward while `fx_core` charges. |
| `hurt` | 0.45s | no | TBD | Short root recoil and body reaction. |
| `death` | 2.2s | no | TBD | Collapse/drop with core fade. Cleanup still must be handled in MythicMobs. |
| `spawn` | 1.8s | no | TBD | Rises from below and straightens. |
| `special_01` | 2.0s | no | TBD | Boss tell with shoulder spread and amplified core pulse. |

Gameplay notes:
- hitbox: `hitbox_core` is a visual/reference helper only; final hitbox belongs
  in ModelEngine/MythicMobs config.
- attack range: TBD after combat design pass.
- damage frame: draft suggestions are documented above, but final timings must
  be verified in staging.
- visual tells: chest core pulse and shoulder/body wind-up are present as draft
  tells for cast and special attacks.
- sound hooks: use MythicMobs skill hooks after import.
- particle hooks: use MythicMobs skill hooks around `fx_core` after import.

Performance:
- texture size: 64x64 PNG draft.
- approximate cube/bone count: 26 cubes, 13 groups/bones.
- intended max simultaneous instances: 1-3.

Review:
- art approved: no
- technical approved: no
- gameplay approved: no
- imported in staging: no

Notes:
- This is a real draft asset, not a final approved model.
- Manual Blockbench open/save review is still required before ModelEngine import.
- ModelEngine import, resource-pack generation, and staging spawn test are still
  pending because they require GUI/runtime staging.
- MythicMobs and Nexo draft packet files are prepared for staging copy/import.
