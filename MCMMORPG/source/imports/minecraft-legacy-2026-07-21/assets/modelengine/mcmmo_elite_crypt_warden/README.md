# ModelEngine Packet: mcmmo_elite_crypt_warden

Status: prepared for manual ModelEngine v4 import

Model id:
`mcmmo_elite_crypt_warden`

Source:
- Blockbench source: `assets/blockbench/source/mcmmo_elite_crypt_warden/mcmmo_elite_crypt_warden.bbmodel`
- Texture: `assets/blockbench/source/mcmmo_elite_crypt_warden/textures/mcmmo_elite_crypt_warden.png`
- Export staging folder: `assets/blockbench/export/mcmmo_elite_crypt_warden/`

Expected ModelEngine handling:
- Import the `.bbmodel` into ModelEngine v4 with model id `mcmmo_elite_crypt_warden`.
- Use the state machine animation system. MythicMobs draft uses `model{mid=mcmmo_elite_crypt_warden;usestatemachine=true;save=true;damagetint=true;showshadow=true}`.
- Keep the base entity visible only through the model. Final hitbox scale must be verified in staging.

Animation states expected by MythicMobs:
- `spawn`
- `idle`
- `walk`
- `attack_01`
- `attack_02`
- `cast`
- `hurt`
- `death`
- `special_01`

Import notes:
- Manual Blockbench open/save review is still required before import approval.
- Confirm all animation names after import; do not rename in ModelEngine unless the MythicMobs skill packet is updated.
- Confirm `hitbox_core` is not shipped as a visible gameplay part unless intentionally mapped.
- Confirm texture resolves from the local texture path and in the generated resource pack.

Known pending items:
- Blockbench GUI visual inspection.
- ModelEngine GUI/import.
- Generated ModelEngine runtime files.
- Staging server reload/spawn evidence.
- Approval remains blocked until staging evidence exists.
