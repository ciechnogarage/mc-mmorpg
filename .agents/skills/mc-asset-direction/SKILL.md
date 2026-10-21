---
name: mc-asset-direction
description: Direct Minecraft visual-asset briefs, profile selection, reference retrieval, silhouette choices, and evidence-bound art criteria. Use when defining or revising an AssetSpec for a mob, boss, NPC, pet, mount, item, weapon, armor, furniture, prop, or effect before Blockbench authoring or visual review begins.
---

# MC asset direction

Define what to build and how its art will be judged. Do not author geometry, edit plugin configuration, or issue `ART_PASS`.

## Direct the asset

1. Confirm the exact asset ID, asset type, delivery profile, allowed paths, evidence directory, accepted baseline, gameplay role, and intended viewing distance. Report `BLOCKED` instead of silently inventing a material choice that changes the product direction.
2. Declare versioned style, rig, and delivery profile references. Select one applicable rig archetype: `biped`, `quadruped`, `flying`, `serpentine`, `multipart`, or `static`. Treat a new region- or faction-wide style as a product decision requiring an explicit brief.
3. Retrieve only references matched to anatomy, role, construction, animation, material, or delivery. State what each reference teaches and where it must not be copied.
4. Define three materially different silhouette directions before detailed authoring. Select one with falsifiable checks for family read, role read, scale, dominant shape, negative space, and player-distance readability.
5. Hand off a compact `AssetSpec` containing the source brief, versioned profile references, exact references, required views and poses, forbidden traits, required animations or static states, integration owners, and acceptance criteria.

## Load references selectively

- For a creature-family brief or reference packet, read the applicable parts of `docs/ai/mc-model-mob-learning-spec.md` and use `docs/ai/mc-primary-read-learning-template.md` only when a new extraction packet is needed.
- To locate comparable local models, search `docs/ai/modelengine-creation-atlas.md` by archetype or asset ID; do not load the atlas wholesale.
- For silhouette and original-synthesis criteria, read only sections 1-3 and 16 of `docs/ai/modelengine-model-production.md`.
- Load `docs/ai/mc-model-mob-anti-pattern-catalog.md` only when directing rescue work or when a named failure needs diagnosis.

## Handoff boundary

Return the selected direction, unresolved decisions, exact evidence requirements, and the next authoring owner. Do not mutate `.bbmodel`, Nexo, ModelEngine, MythicMobs, staging, or live state. A director may request iteration or rebuild, but only an independent visual critic may grant `ART_PASS`.
