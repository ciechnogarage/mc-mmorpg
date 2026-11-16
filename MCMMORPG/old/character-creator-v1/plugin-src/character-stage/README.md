# CharacterStage

`CharacterStage` is a Paper plugin layer for the 3D pre-spawn character stage.

Scope of this scaffold:
- intercept join and move the player into the stage room
- support physical slot anchors in world
- support focus-first, confirm-second interaction flow
- run config-defined actions for playable and empty slots
- read technical `MMOProfiles.profileId` from MMOProfiles without binding semantic identity to it

Non-goals in this scaffold:
- direct domain-02 persistence implementation in this plugin
- dynamic actor rendering
- packet actor spawning
- ModelEngine actor ownership

This plugin intentionally keeps a command-driven integration seam. Slot actions
are configured as console or player commands so the runtime scene can be wired
to existing flows without guessing vendor APIs.

Boundary rules:
- `MMOProfiles.profileId` stays technical and is not treated as semantic character identity.
- `CharacterStage` only presents domain-02 state after resolving `profile_id -> character_id`.
- A slot is playable only when the resolver returns exactly one character with
  `binding_status=BOUND`, `lifecycle_status=PLAYABLE`, and `creator_stage=READY`.
- Missing, ambiguous, inconsistent, or unavailable mappings fail closed and block slot actions.
- `CharacterStage` does not own creator completion; incomplete characters stay in domain-02 state.

Current runtime note:
- The live domain-02 mapping adapter is not wired in this module yet.
- Until that adapter exists, occupied MMOProfiles slots resolve through a fail-closed seam instead of falling back to plugin-owned readiness.

Build note:
- the repository does not expose a shared plugin build system
- use this module source tree as the input for the custom plugin jar build
