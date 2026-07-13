# ModelEngine Design Routing

This guide routes design questions to evidence. It does not prescribe a house style or authorize copying a source model.

## Synthesis Contract

1. Write the intended distant read, role, mood, scale, dominant shape, and player-facing mechanic.
2. Choose independent references for each relevant subsystem and inspect their source files, renders, and bindings.
3. Record what principle is being borrowed, its limitation, and the deliberate way the new design differs.
4. Form a falsifiable hypothesis about the expected visual or runtime improvement.
5. Validate at equivalent camera scale and in runtime; preserve failed hypotheses as learning evidence.
6. Promote a lesson only after repeated or strong render/runtime confirmation.

## Subsystem Routes

### silhouette

dominant mass, distant read, proportion hierarchy, and focal asymmetry

- `item_press_items` (60.928): no direct MythicMobs linkage; no runtime binding evidence.
- `pet_ghastling_portal` (60.067): no structural limitation detected.
- `boost` (59.668): no structural limitation detected.
- `lr_stringlights` (58.627): no structural limitation detected.
- `chameleon_fly` (57.813): no structural limitation detected.

### anatomy

proportions, hierarchy depth, articulated structure, and anatomy fit

- `deadbeard2_ship` (36): no structural limitation detected.
- `ignevar` (36): no structural limitation detected.
- `kobold_assassin_2` (36): no structural limitation detected.
- `korrvag_5seg` (36): no structural limitation detected.
- `korrvag_6seg` (36): no structural limitation detected.

### materials

palette, value separation, material contrast, and emissive restraint

- `item_press_items` (21.733): no direct MythicMobs linkage; no runtime binding evidence.
- `ethereal_reaver_beam` (20): no structural limitation detected.
- `pomni_ground_piece` (20): no structural limitation detected.
- `azriel_parts` (19.973): no direct MythicMobs linkage; no runtime binding evidence.
- `azriel` (19.797): no structural limitation detected.

### rig

readable hierarchy, purposeful helper bones, and gameplay anchors

- `lr_hydra` (51.833): no structural limitation detected.
- `korrvag_8seg` (49.5): no structural limitation detected.
- `lrd_witherstorm` (49.25): no structural limitation detected.
- `korrvag_7seg` (47.083): no structural limitation detected.
- `lrd_kraken` (45.5): no structural limitation detected.

### locomotion

weight transfer, cycle coverage, and secondary motion

- `oog` (29.75): no structural limitation detected.
- `kobold_assassin` (29.418): no structural limitation detected.
- `ethereal_reaver` (29.328): no structural limitation detected.
- `LR_minotaur` (29.25): no structural limitation detected.
- `armoros` (29.22): no structural limitation detected.

### attacks

anticipation, impact, recovery, and distinct attack silhouettes

- `cog_sentinel` (39.889): no structural limitation detected.
- `oog` (39.875): no structural limitation detected.
- `piglin_mech` (38.807): no structural limitation detected.
- `talus_knight` (38.595): no structural limitation detected.
- `piglin_mech_mount` (38.157): no structural limitation detected.

### hitVolumes

local moving hit volumes and contact-part alignment

- `lr_hydra` (38.333): no structural limitation detected.
- `korrvag_8seg` (37.433): no structural limitation detected.
- `talus_knight` (36.5): no structural limitation detected.
- `korrvag_7seg` (33.533): no structural limitation detected.
- `korrvag_6seg` (29.633): no structural limitation detected.

### vfx

stable effect anchors, readable charge states, and synchronized effects

- `lrd_direwolf` (21.467): no structural limitation detected.
- `lrd_hollow_keeper` (21.467): no structural limitation detected.
- `piglin_mech_mount` (20.714): no structural limitation detected.
- `cog_sentinel` (19.429): no structural limitation detected.
- `talus_knight` (19.429): no structural limitation detected.

### sound

layered cues synchronized to anticipation, movement, and impact

- `talus_knight` (34): no structural limitation detected.
- `golem_king_cubes` (30.4): no structural limitation detected.
- `cog_sentinel` (30): no structural limitation detected.
- `LR_minotaur` (30): no structural limitation detected.
- `piglin_mech` (30): no structural limitation detected.

### states

state coverage, transitions, phases, reactions, and transformations

- `golem_king` (36): no structural limitation detected.
- `bufo_bufo` (30): no structural limitation detected.
- `talus_knight` (29.5): no structural limitation detected.
- `cog_sentinel` (29): no structural limitation detected.
- `ignevar` (29): no structural limitation detected.

### death

readable defeat, collapse or detachment, and encounter closure

- `armoros` (32.48): no structural limitation detected.
- `phoenix` (31.813): no structural limitation detected.
- `lr_gryffin` (31.52): no structural limitation detected.
- `lr_gryffin_corvid` (31.52): no structural limitation detected.
- `kriger` (31.419): no structural limitation detected.

## Evidence-Backed Risks

- **complexity_without_integration** (1/542): High structural complexity without direct runtime linkage is not production proof. Require resolved YAML bindings and runtime observation.
- **animation_count_without_combat_coverage** (27/542): Many animations can coexist with missing attack, reaction, transition, or death coverage. Check role coverage and each required encounter state.
- **rig_without_gameplay_anchors** (6/542): A deep or large rig without helpers and hitboxes may animate well but expose weak gameplay architecture. Design explicit anchors and local hit volumes for the new encounter.
- **timeline_assumed_from_keyframes** (53/542): Dense keyframes do not prove explicit gameplay synchronization. Inspect animation phases and linked skill timing directly.
- **detail_without_dominant_mass** (8/542): Large element counts with weak dominant-volume hierarchy can produce visual noise. Prove one dominant form or a deliberate grouped-mass hierarchy in silhouette renders.

Machine-readable sources: `modelengine-design-router.json` and `modelengine-negative-patterns.json`.
