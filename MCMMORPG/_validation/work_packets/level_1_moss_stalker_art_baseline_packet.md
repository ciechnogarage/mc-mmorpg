# Work Packet — level_1_moss_stalker — 5 large rescue iterations

## Header

- mob_id: `level_1_moss_stalker`
- backend: `world`
- lane owner: `art`
- validation lane: `qa`
- mode: `workspace-write`
- next owner after final art approval: `mobs`

## Goal

Rescue `level_1_moss_stalker` to a server-worthy baseline through 5 large structural iterations, not micro-polish.

## Mandatory references

- direct shell teacher:
  - `$HOME/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/direwolf/lrd_direwolf.bbmodel`
- autopsy:
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_lrd_direwolf_reference_autopsy.md`
- rule packet:
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_rule_packet.md`
- parity / shell lock:
- `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_parity.md`
- `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_shell_lock.md`
- `MCMMORPG/_validation/model_studies/level_1_moss_stalker_translation_plan.md`

## Live Blockbench entrypoint

- host-side launch command:
  - `npm run blockbench:open -- $MC_ROOT/MCMMORPG/plugins/ModelEngine/blueprints/level_1_moss_stalker/level_1_moss_stalker.bbmodel`
- environment note:
  - sandboxed Codex launch failures around FUSE or Electron sandbox are not valid evidence that Blockbench is unavailable; live review must be judged from the host-side window.

## Forbidden reads

- slab with a face
- plank with legs
- furniture / shrine object
- generic armored beast
- decorative moss rescue over broken shell

## Required evidence every iteration

- current Blockbench front capture
- current Blockbench side capture
- current Blockbench three-quarter capture
- current Blockbench player-scale capture
- one side-by-side reference comparison sheet
- one QA critique note for that iteration
- one updated `iteration_log` entry

## Iteration contract

### Iteration 1 — family shell rebuild
- change only:
  - overall low-long predator shell
  - abdomen taper
  - removal of slab/plank read
- success:
  - family shell is no longer wrong-object or slab/plank

### Iteration 2 — forequarter thrust / threat origin
- change only:
  - chest drive
  - head/neck projection
  - attack-side threat read
- success:
  - danger begins from the whole forequarter, not a face tile

### Iteration 3 — backline + hip rhythm
- change only:
  - lower-back break
  - hindquarter propulsion read
  - rear finish rhythm
- success:
  - rear no longer reads detached

### Iteration 4 — asymmetry + focal hierarchy
- change only:
  - one attack-side emphasis
  - front focal hierarchy
- success:
  - front no longer reads broad/generic after family shell is already stable

### Iteration 5 — planted-foot tension
- change only:
  - front support logic
  - rear spring-loaded support
  - crouch tension
- success:
  - stance reads ready to spring rather than statically parked

## QA contract between iterations

QA must describe:
- dominant visual failure
- exact structural reason the model still looks wrong
- whether family read improved, premium read improved, both improved, or neither improved
- one required next axis only
- whether the current model would embarrass the server in current form

## Stop conditions

- if family read still fails after iteration 3: restart shell
- if premium front read still fails after iteration 5: remain blocked in `art`
- no `mobs` handoff until final art approval is explicit

## Done

- [ ] all 5 rescue iterations completed or explicitly stopped by a stop condition
- [ ] current evidence set exists
- [ ] QA critique exists between every counted iteration
- [ ] final art verdict explicit
- [ ] `mobs` handoff blocked or allowed explicitly
