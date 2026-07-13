# Direwolf Blockbench session — 2026-07-07

Reference source opened:
- `$HOME/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/direwolf/lrd_direwolf.bbmodel`

Blockbench runtime used:
- AppImage: `$HOME/.local/bin/Blockbench_5.1.4.AppImage`
- launch command:
  - `'$HOME/.local/bin/Blockbench_5.1.4.AppImage' --no-sandbox '$HOME/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/direwolf/lrd_direwolf.bbmodel'`
- environment seen during launch:
  - `DISPLAY=:0`
  - `WAYLAND_DISPLAY=wayland-0`
  - `XDG_SESSION_TYPE=wayland`
- result:
  - Blockbench was launched against the direwolf source path.
  - CLI process logs later showed a GPU/process failure for one launched process, but the user confirmed Blockbench with direwolf was still open on the desktop. So the shell-side process log was not sufficient to determine the true GUI state of the desktop session.

Important shell-side blockers:
- GNOME screenshot D-Bus calls from this CLI session were denied with `AccessDenied`, so shell-driven window capture was not available here.
- The captured process log is not a trustworthy proxy for the exact visible Blockbench window state; future runs should verify with real exported captures from the active GUI session.
- Practical rule: do not claim a successful native Blockbench review session until real captures/exports are produced from that exact visible run.

## Structural facts confirmed from the source package

- format: Blockbench `free`
- `meta.format_version`: `4.10`
- `meta.box_uv`: `false`
- top-level outliner roots:
  - `hitbox`
  - `root`
  - `slash_root`
  - `sword_death`
- total elements: `136`
- total nested group nodes in outliner: `107`
- max hierarchy depth observed: `14`
- animations: `22`
- textures embedded:
  - `lrd_direwolf.png` `256x256`
  - `lrd_slash_1.png` .. `lrd_slash_9.png` `256x256`
  - `lrd_direwolf_shadow_tail.png` `128x128`

## Wolf-shell truths locked from this reference

1. Wolf first, style second.
   - The shell is predator-readable before decoration or texture drama.
2. Forequarter dominance.
   - Head wedge + shoulder/chest must visually lead the read.
3. Low-long axis.
   - Length clearly beats height; the body stays low and tense.
4. Articulated backline.
   - Neck rise -> shoulder shelf -> back plane -> lower-back transition -> tail launch.
5. True head package.
   - Head is not a box. It needs brows, cheek mass, jaw separation, and a forward snoot that does not overpower the skull.
6. Real leg chain.
   - Front: bicep -> forearm -> foot.
   - Back: bicep -> forearm -> ankle -> foot.
   - Posts/support-sticks are auto-fail.
7. Tail is a silhouette system.
   - Segmented tail contributes to balance, aggression, and motion arcs.
8. Motion is part of the model contract.
   - This shell is built to survive idle tension, run, attack, dodge, leap-slam, recovery, and death.

## Named anti-patterns to reject immediately

- rat/log drift
- root-corridor / snout-stick muzzle
- post legs
- barrel torso
- decoration-first wolf
- texture rescue over broken shell

## Mandatory process for future wolf-family work

- Open `lrd_direwolf.bbmodel` directly in Blockbench before touching geometry.
- Keep the accepted concept and the direwolf reference side-by-side while editing.
- Review and correct in Blockbench continuously in:
  - front
  - side
  - three-quarter
  - player-scale
- Do not claim `PASS` without Blockbench-origin captures and a drift note.
- If front/head/forequarter read fails, stop all decorative work and return to shell rebuild.

## What must be written into the next wolf packet

- exact reference path above
- shell-lock criteria derived from this session
- mandatory Blockbench capture list
- explicit fail language for rat/log drift and post-leg drift
- rule that Layer B corruption/briar treatment is forbidden until Layer A wolf shell passes
