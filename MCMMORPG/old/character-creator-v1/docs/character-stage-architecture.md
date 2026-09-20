# Character Stage Architecture

Status: source module scaffolded and runtime jar present in `plugins/`; current v1 uses the 3D stage as the entry point and CoreTools creator menus as detail/fallback screens.

## Goal

Replace GUI-first profile selection with a world-first 3D character stage:
- join -> stage room
- physical slot anchors around the campfire
- first click focuses a slot
- second click or confirm pedestal enters that slot flow
- empty slots route into character creation

## Source Truth

- backend profiles: `plugins/MMOProfiles`
- stage source module: `plugin-src/character-stage`
- deployed runtime jar: `plugins/CharacterStage-0.1.0-SNAPSHOT.jar`
- existing creator fallback/detail screens: `plugins/CoreTools/MenuCreator/foundation_*`
- current E2E seam: `_validation/character_sector_e2e.js`

## Runtime Contract

`CharacterStage` owns:
- join interception
- stage teleport
- focus/confirm interaction state
- command-driven integration seams
- empty-slot routing to `foundation_creator_intro`

`CharacterStage` does not yet own:
- actor spawning
- packet NPC rendering
- direct MMOProfiles profile activation
- ModelEngine puppet ownership

Current v1 behavior:
- empty stage slots run `characterstage leave` and open `core-menu foundation_creator_intro`
- occupied stage slots run `characterstage leave` and open MMOProfiles until a verified direct activation API is added
- `config.yml` is synced across source resources, build classes, build jar, and runtime jar

## Runtime Shape

Stage config defines:
- `stage.spawn`
- `stage.exit`
- `stage.confirm_anchor`
- `stage.back_anchor`
- `slots.<slot_id>.anchor`
- `slots.<slot_id>.focus`
- `slots.<slot_id>.occupied_actions`
- `slots.<slot_id>.create_actions`

Slot actions stay command-driven for now so the stage can connect profile selection and creator routing without guessing plugin internals.

## Next Implementation Steps

1. Replace `mmoprofiles open %player%` with a verified direct profile activation path.
2. Add actor rendering or NPC/model puppets for occupied slots.
3. Add runtime proof through RCON or the character-sector E2E harness when an owned server session exposes RCON.
