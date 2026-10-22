# Blockbench desktop MCP test

Application: `/home/dev/.local/opt/blockbench-5.1.6/blockbench`
Separate profile: `/home/dev/.local/share/blockbench-codex/profile`
Plugin: `/home/dev/.local/share/blockbench-codex/mcp.js`
Source: https://jasonjgardner.github.io/blockbench-mcp-plugin/mcp.js
The local plugin copy changes its listening address to `127.0.0.1`.

Start on the local X11 desktop (defaults to display :1):

```bash
./tools/blockbench/start-desktop.sh
```

The MCP endpoint is expected at `http://127.0.0.1:3000/bb-mcp` after the plugin loads.
Desktop plugin installation and permission prompts must be completed before this endpoint is available.
Verified 2026-09-18: Blockbench MCP 1.7.0 initialized successfully; created a Generic Model project, one group, four cubes, two filled textures, and a 2-second rotation animation through MCP. Captured visually different frames at 0 and 1 seconds and exported a valid .bbmodel.

Test artifacts: `output/blockbench-desktop-check/`.
Codex connection: `blockbench-desktop` (Streamable HTTP). Restart/resume Codex to load its dedicated tools; the current session was tested using direct local MCP JSON-RPC requests.

Observed integration details:
- create_animation named the test `animation.test_turn`, adding the `animation.` prefix.
- Enter Animate mode and select the actual animation before using animation_timeline.
- create_texture with width=32, height=32 and fill_color produced a 16x16 texture in this test. Verify actual exported dimensions; do not assume requested dimensions were honored.
- The initial setup application has local debugging port 9224 enabled. The normal launcher above does not enable this port.


## Verified review pipeline (2026-09-18)

Use an explicit source and a **new** output directory. Rendering and measurement
require the matching project already selected; they never create tabs. Identity
ignores only tab name, animation selection, local texture save paths and empty
editor-created bone tracks with default flags. Unknown fields remain significant.
Legacy generator JSON may differ from an editor export: do not weaken the guard
to make an unreviewed mismatch pass.

```bash
python3 -m unittest discover -s tools/blockbench/tests -v
python3 tools/blockbench/validate_undead.py output/undead-player-v5/motion-repair-01/race_nieumarly.bbmodel
python3 tools/blockbench/inspect_review_project.py --source output/undead-player-v5/motion-repair-01/race_nieumarly.bbmodel
python3 tools/blockbench/measure_undead_motion.py --source output/undead-player-v5/motion-repair-01/race_nieumarly.bbmodel --out output/undead-player-v5/reviews/new-measurement --animations jump_start jump_end
python3 tools/blockbench/render_undead_motion.py --source output/undead-player-v5/motion-repair-01/race_nieumarly.bbmodel --out output/undead-player-v5/reviews/new-render --clips jump_start jump_end
```

`validate_undead.py` exits 0 for file checks, 1 for invalid data/policy and 2
for unsupported features or tool failure. `FILE_CHECKS_OK` explicitly excludes
motion, art and runtime. The former hardcoded leg calculations were removed.

`measure_undead_motion.py` samples actual transformed editor cube vertices at
120 Hz and key times; it exits 1 for detected flat-ground penetration below
-0.04 model pixels. It does not prove support, horizontal foot locking, blend
behavior or runtime correctness. `check_death_pose.py` now delegates to this
read-only measurement, never writes historical correction files.

MCP `risky_eval` itself adds Undo history entries even for inspection. Therefore
"read-only" here means no authored asset changes, not an untouched Undo stack.
Rendering restores the project/mode/timeline state, but its camera is currently
left at the last review view; manifests state that limitation.

Authoring scripts create snapshots and editor-exported candidates, never patch
canonical bbmodel JSON. A candidate is not ART_PASS or an installed blueprint.
