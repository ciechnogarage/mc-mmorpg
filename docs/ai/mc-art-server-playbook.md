# MC Art Server Playbook

Use this when the asset should be created against the real MC staging stack, not as a loose idea draft.

## Runtime Rule

- Use Docker backends only.
- `world` for blocks, furniture, ruins, world props, dungeon props, mob or pet presentation, and ModelEngine resource-pack visuals.
- `items` for weapons, armor, item icons, class-linked appearance, and login or item presentation.
- `hub` for lobby cosmetics, social presentation, and hub-only decorative assets.
- Cross-area work needs evidence from every affected backend.
- Player-facing checks go through Velocity on `localhost:25565`.

## Execution Order

1. Run `art` as primary with `workspace-write`.
2. Route gameplay review to the owning secondary domain if the asset changes mechanic context.
3. Run `qa` as validation before calling the asset ready.
4. Start only the affected backend containers.

## Task Template

Run from `$HOME`:

```bash
npm run mc:agent -- art --write-policy workspace-write --role primary "<task>"
```

Task body should always include:

- target backend: `world`, `items`, or `hub`
- asset family: weapon, furniture, block, skin, cosmetic, icon, mob visual
- target region, faction, biome, dungeon, or system
- repo scope: `$MC_ROOT` when the task may pass through top-level collab routing
- required runtime files and source files
- required evidence for QA

## Ready-to-Run Prompts

### Ruins Furniture and Blocks

```text
stworz zestaw ruin furniture i custom blokow dla [region_or_dungeon] w $MC_ROOT; backend world; klimat [faction_or_biome]; dostarcz source files, runtime files, stable IDs, preview evidence i handoff do qa; uwzglednij placement, collision footprint, rotations i resource-pack integration
```

### Weapon Family

```text
stworz rodzine [weapon_family] dla [class_or_faction] w $MC_ROOT; backend items; dostarcz modele, tekstury, ikony, source files, runtime files, stable IDs i evidence do qa; zachowaj tier readability, held-item transforms i zgodnosc z visual_style_bible
```

### Cosmetic or Skin

```text
stworz [skin_or_cosmetic_type] dla [role_or_faction] w $MC_ROOT; backend hub; dostarcz source files, runtime files, preview evidence i handoff do qa; zachowaj player readability, body alignment i multiplayer performance
```

### Mob or Pet Visual Package

```text
stworz visual package dla [mob_or_pet_id] w $MC_ROOT; backend world; dostarcz source files, runtime resource-pack files, preview evidence i handoff do qa; behavior i hitbox contracts zostaw dla mobs, ale przygotuj asset pod ich integracje
```

## Validation Handoff

After `art` finishes, validate with:

```bash
npm run mc:agent -- qa --write-policy read-only --role validation "<same task plus: validate runtime evidence on backend world/items/hub>"
```

Minimum evidence expected:

- changed files list
- backend used
- Docker and Velocity runtime evidence
- previews or screenshots when the asset is visual-first
- explicit `PASS`, `FAIL`, `BLOCKED`, or `INSUFFICIENT_EVIDENCE` from QA
