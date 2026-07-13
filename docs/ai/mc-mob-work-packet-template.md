# MC Mob Work Packet Template

Copy, fill, then hand the packet to the correct agent lane.

## Header

- mob_id:
- pack/backend: world | items | hub
- lane owner: art | mobs | dungeon | rpg | qa
- mode: read-only | workspace-write
- previous baseline:
- next owner:

## Goal

One sentence only.

## Scope

- allowed files:
- forbidden files:
- exact repo root: `/home/przemek/projects/MC`

## Required Inputs

- exact references:
- exact current local files:
- exact IDs already known:
- exact family reference packet path:
- target exit bucket for this pass:
- current accepted baseline evidence paths:

## Required Output

- files to create/update:
- evidence to produce:
- validation expected:
- blockbench review gate required?: yes/no
- side-by-side concept/reference proof required?: yes/no

## Constraints

- one-axis-only change:
- no-copy rule:
- rollback note needed?: yes/no
- stop condition after two failed passes:
- PASS allowed without Blockbench review?: no for creature/mob art lanes

## Done When

- [ ] artifact paths exist
- [ ] exact IDs named
- [ ] evidence paths named
- [ ] next owner named
- [ ] Blockbench review gate satisfied when this is a creature/mob art lane
- [ ] blockers explicit if incomplete

## Prompt Body

```text
<write the exact agent task here>
```

## Example — art brief

```text
read-only brief for level_1_moss_stalker in /home/przemek/projects/MC; backend world; inspect local refs in MCMMORPG and corpus refs in /home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2); return distant read, 3 references, what to borrow, what not to copy, 3 silhouette directions, exact artifact paths under MCMMORPG/_validation/model_studies, and a Blockbench review plan for the next art pass; do not edit gameplay files
```

## Example — mobs integration

```text
zintegruj gotowy model level_1_moss_stalker w /home/przemek/projects/MC; backend world; accept handoff only if the art packet includes Blockbench front/side/three-quarter/player-scale captures plus side-by-side concept/reference proof; bind blueprint to MythicMobs, resolve states, helper bones, spawn/death hooks, and runtime proof paths; keep rewards as separate rpg handoff unless exact reward file is already in scope
```
