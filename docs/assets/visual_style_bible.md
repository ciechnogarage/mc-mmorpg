# MC MMORPG Visual Style Bible

Status: baseline for new visual assets. Extend this document only with decisions
confirmed by shipped assets or an explicit art-direction review.

## Core Direction

- Build readable Minecraft-native fantasy/MMORPG assets, not realistic miniatures.
- Make silhouette and dominant material readable before adding texture noise, glow,
  particles, or tiny details.
- Tie every asset to a gameplay purpose and a region, faction, biome, dungeon, or
  global family. An asset without that context remains a draft.
- Reuse established shape, palette, material, and ornament language from nearby
  approved assets. Do not assemble unrelated marketplace styles.
- Use one dominant idea, one supporting idea, and restrained accents. Rarity should
  increase deliberate contrast and construction quality rather than visual clutter.

## Visual Hierarchy

- Judge silhouette at gameplay distance and at inventory/icon scale when relevant.
- Separate materials primarily by value, then hue and texture.
- Reserve emissive color for magic state, interaction, rarity focal point, or combat
  telegraph. Do not use glow as a substitute for construction.
- Keep attachment points, held-item transforms, furniture footprints, player scale,
  and mob hit/read areas visually credible.

## Asset Families

- Weapons/items: share grip scale, material language, icon framing, and tier cues
  within one family. Mechanics and rarity remain owned by the RPG agent.
- Blocks/furniture/props: define footprint, rotations, collision, interaction state,
  density budget, and a small reusable kit rather than isolated hero props.
- Skins/cosmetics: preserve player readability, body-part alignment, multiplayer
  performance, and the target faction/role identity.
- Mobs/pets/NPCs: visual source is owned here; behavior, hitboxes, skills, and combat
  timing require the mobs agent and the ModelEngine production workflow.
- GUI/icons/fonts: deliver palette, iconography, states, and source files; menu/HUD
  interaction remains owned by the UI agent.
- VFX: specify purpose, anchor, duration, color, density, and performance budget;
  synchronize gameplay effects with the owning domain agent.

## Required Asset Brief

Record before production:

```md
asset_id:
asset_type:
gameplay_purpose:
region_faction_biome_or_dungeon:
existing_asset_or_id_to_extend:
distant_read_or_icon_read:
dominant_shape:
materials_and_palette:
scale_and_use_context:
states_or_variants:
performance_budget:
integration_owner:
references_and_license:
```

References describe principles to synthesize, not files to copy. Record provenance
and license for every external source; paid or proprietary source art is forbidden
unless the task provides explicit compatible rights.

## Delivery and Approval

- Follow `blockbench_model_standard.md` for models, textures, naming, source layout,
  Nexo/ModelEngine integration, performance, review, and staging evidence.
- Deliver editable source, exported/runtime files, preview renders, stable ID map,
  asset README, and review evidence.
- Route stats/loot to RPG, behavior/hitboxes to mobs, world placement to dungeon,
  interaction logic to UI, and final runtime evidence to QA.
- Report `INSUFFICIENT_EVIDENCE` instead of approval when the current source lacks
  matching previews, resource-pack load proof, or staging inspection.
- Run staging only in the affected Docker backend behind Velocity: `world` for
  dungeon/world/mob/model/furniture/block work, `items` for item/equipment/class/
  login work, and `hub` for hub/social presentation. Cross-area assets require
  separate evidence from every affected backend. Never start Paper natively.
