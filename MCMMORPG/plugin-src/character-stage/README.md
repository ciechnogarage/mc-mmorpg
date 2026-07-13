# CharacterStage

`CharacterStage` is the Paper plugin layer for the 3D pre-spawn character stage.

Scope of this scaffold:
- intercept join and move player into a stage room
- support physical slot anchors in the world
- support focus-first, confirm-second interaction flow
- run config-defined actions for occupied or empty character slots
- avoid hard dependency on unknown MMOProfiles internals

Non-goals in this scaffold:
- direct MMOProfiles Java API integration
- dynamic actor rendering
- packet actor spawning
- ModelEngine actor ownership

The plugin is intentionally command-driven at the integration seam. Slot actions are
configured as console or player commands so the runtime scene can be wired to the
existing profile backend without guessing vendor APIs.

Build note:
- this repository does not expose a shared plugin build system
- use this module as the source tree for the upcoming custom plugin jar
