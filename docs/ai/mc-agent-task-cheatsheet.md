# MC Agent Task Cheatsheet

MC nie jest agentem.

| Task | Start | Typical handoff |
| --- | --- | --- |
| Plugin inventory, version, ownership, unknown JAR | `minecraft-plugin-inventory` | Owning domain agent |
| Startup errors, SQL, storage, sync, packet, voice, performance | `minecraft-ops-sync` | `minecraft-release-qa` |
| Classes, stats, drops, loot, progression | `minecraft-rpg-systems` | Permissions/economy, mobs/models |
| Weapons, blocks, furniture, skins, cosmetics, icons, textures, resource-pack art | `minecraft-visual-assets` | RPG systems, mobs/models, dungeon/world, UI |
| Mobs, bosses, pets, behavior, ModelEngine runtime binding | `minecraft-mobs-models` | RPG systems, dungeon/world |
| Dungeons, instances, generated worlds, region flags, pregeneration | `minecraft-dungeon-world` | Release QA |
| Menus, HUD, holograms, placeholders, player-facing text | `minecraft-ui-hud-menus` | Permissions/economy |
| Permission nodes, ranks, economy, guilds, command exposure | `minecraft-permissions-economy` | Release QA |
| Final staging/release verdict | `minecraft-release-qa` | Owning domain agent |

## Mechanic Shortcuts

- `level_1`, MythicDungeons trigger, instance cleanup: `dungeon`
- `GroveGuardian`, MythicMob skills, ModelEngine model ID: `mobs`
- MMOItems weapons, affixes, crafting, rewards: `rpg`
- Blockbench assets, furniture, skins, textures: `art`
- DeluxeMenus buttons, HUD placeholders, holograms: `ui`
- LuckPerms gates, CMI economy, Guilds: `economy`

## Fast Start

- Real asset creation for `world/items/hub`: [mc-art-server-playbook.md](/home/przemek/projects/MC/docs/ai/mc-art-server-playbook.md)
- New mob/model pipeline: [mc-mob-model-agent-pipeline.md](/home/przemek/projects/MC/docs/ai/mc-mob-model-agent-pipeline.md)
- Learning spec + critique bar: [mc-model-mob-learning-spec.md](/home/przemek/projects/MC/docs/ai/mc-model-mob-learning-spec.md), [mc-model-mob-critique-rubric.md](/home/przemek/projects/MC/docs/ai/mc-model-mob-critique-rubric.md), [mc-model-mob-anti-pattern-catalog.md](/home/przemek/projects/MC/docs/ai/mc-model-mob-anti-pattern-catalog.md)
- Start with `art` as primary and `workspace-write` when implementation is intended.
- Validate with `qa` before calling the asset ready.
- Put the backend name directly in the task text.
