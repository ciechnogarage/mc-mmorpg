# Work Packet — level_1_moss_stalker — art brief (production standard)

Copy, fill, then hand the packet to the correct lane owner. This version is aligned to:
- `docs/ai/mc-mob-work-packet-template.md`
- `docs/ai/mc-creature-agent-production-runbook.md`
- `MCMMORPG/_validation/model_studies/direwolf_family_reference_extraction_packet.md`

## Header

- mob_id: `level_1_moss_stalker`
- pack/backend: `world`
- lane owner: `art`
- mode: `read-only`
- previous baseline: none; new test mob
- next owner: `art` for first baseline build, then `mobs`

## Goal

Produce a reference-led art brief that defines a strong, original level_1 support mob reading as a fast grove predator / stalker rather than a humanoid miniboss, sprout reskin, or decorative plant creature.

## Scope

- allowed files:
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_study.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_silhouette_candidates.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_parity.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_shell_lock.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_translation_plan.md`
  - `MCMMORPG/_validation/model_reviews/level_1_moss_stalker/`
- forbidden files:
  - `MCMMORPG/plugins/MythicMobs/**`
  - `MCMMORPG/plugins/ModelEngine/**`
  - reward/drop configs
  - dungeon/world placement configs
  - runtime/live server files
- exact repo root: `/home/przemek/projects/MC`

## Required Inputs

- exact local references:
  - `MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/corrupted_sprout.mob.yml`
  - `MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml`
  - `MCMMORPG/_validation/model_quality/level_1_grove_guardian.quality.json`
  - `MCMMORPG/_validation/model_studies/grove_guardian_reference_study.md`
  - `MCMMORPG/_validation/model_studies/grove_guardian_silhouette_candidates.md`
  - `MCMMORPG/_validation/model_reviews/level_1_grove_guardian/runtime-probe.md`
- exact direct corpus references from `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)`:
  - `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/direwolf/lrd_direwolf.bbmodel`
  - `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/owlbear/lr_owlbear.bbmodel`
  - `/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)/ModelEngine/blueprints/littleroom/parasitic_abomination/parasitic_abomination.bbmodel`
- external benchmark principles only:
  - Littleroom -> encounter-ready packaging discipline
  - SamusDev -> class/role clarity
  - Toro Toro -> creature-first silhouette pressure
- exact IDs already known:
  - existing local level_1 IDs: `level_1_corrupted_sprout`, `level_1_grove_guardian`
  - target new ID: `level_1_moss_stalker`
- exact family reference packet path:
  - `MCMMORPG/_validation/model_studies/direwolf_family_reference_extraction_packet.md`
- target exit bucket for this pass:
  - `PASS_TO_STYLE` preparation only; this read-only brief cannot itself claim an art exit bucket, but it must define the rule/evidence package the next art pass needs to earn `PASS_TO_STYLE`
- current accepted baseline evidence paths:
  - none yet; first baseline does not exist

## Required Output

- files to create/update:
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_study.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_silhouette_candidates.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_parity.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_reference_shell_lock.md`
  - `MCMMORPG/_validation/model_studies/level_1_moss_stalker_translation_plan.md`
- evidence to produce:
  - 3 local references with exact paths
  - 3 direct corpus references with exact paths
  - 1-3 external principle references with exact reason-to-borrow
  - 5-10 extracted principles
  - 3-5 forbidden traits
  - 2-3 silhouette directions
  - one recommended baseline direction
  - reference mass map
  - landmark list
  - joint-chain map
  - 4 motion-stress poses the shell must survive
  - one explicit Layer A vs Layer B split
  - concrete Blockbench review plan for the next art pass: captures, side-by-side setup, drift checks
- validation expected:
  - critique-ready against `docs/ai/mc-model-mob-critique-rubric.md`
  - anti-pattern watchlist from `docs/ai/mc-model-mob-anti-pattern-catalog.md`
  - explicit reminder that future art `PASS` is forbidden without `docs/ai/mc-blockbench-review-gate.md`
  - explicit citation of the direwolf packet rule IDs that should transfer vs the ones that should stay wolf-specific
- blockbench review gate required?: `yes` for the next art build pass
- side-by-side concept/reference proof required?: `yes`

## Creature Intent

- combat role: fast flank pressure / chase add for `level_1`
- target distant read: low, prowling, root-vine predator with one clear forward attack side
- dominant visual promise: `corrupted grove hunter`
- must not be confused with:
  - humanoid druid / treant boss
  - tiny static plant turret
  - armored zombie with leaves glued on
  - bark-first sculpture with paws attached
- likely gameplay hooks later:
  - leap or rush angle
  - thorn or lash attack origin
  - weakpoint / focal corruption node

## Constraints

- one-axis-only change: this pass is study/brief only; no model creation yet
- no-copy rule: borrow principles, never vendor geometry or exact anatomy
- rollback note needed?: `no`, read-only
- stop condition after two failed passes: return to side-by-side silhouette studies before any detail work
- PASS allowed without Blockbench review?: `no` for later creature art lanes
- direct-source rule: derived docs may help organize notes, but the brief is invalid if it does not open and extract the real corpus `.bbmodel` files first
- packet-rule rule: later baseline work must cite transferred family rules explicitly rather than vaguely saying “wolf-ish” or “predator read”

## Done When

- [ ] exact reference paths are named
- [ ] extracted principles are explicit
- [ ] forbidden traits are explicit
- [ ] one recommended baseline direction is chosen
- [ ] family-packet transfer notes are explicit
- [ ] Layer A vs Layer B split is explicit
- [ ] next owner is named
- [ ] blockers are explicit if uncertainty remains

## Prompt Body

```text
read-only brief for level_1_moss_stalker in /home/przemek/projects/MC; backend world; first read MCMMORPG/_validation/model_studies/direwolf_family_reference_extraction_packet.md plus /home/przemek/projects/MC/docs/ai/mc-creature-agent-production-runbook.md and /home/przemek/projects/MC/docs/ai/mc-mob-model-agent-pipeline.md; inspect local refs in MCMMORPG plus direct corpus refs in /home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2); use grove_guardian and corrupted_sprout only as local anchor points, not direct templates; treat the direwolf packet as the family-rule source and explicitly say which rule IDs transfer cleanly, which need translation for a grove-corruption stalker, and which must not be copied literally; borrow Littleroom-grade integration discipline, SamusDev-grade role clarity, and Toro-grade silhouette pressure without copying anatomy; return 3 local references with exact paths, 3 direct corpus references with exact paths, 1-3 external principle references, 5-10 extracted principles, 3-5 forbidden traits, 2-3 silhouette directions, one recommended baseline direction, a reference mass map, landmark list, joint-chain map, 4 motion-stress poses, a Layer A vs Layer B split, exact artifact paths under MCMMORPG/_validation/model_studies, and a concrete Blockbench review plan for the next art pass (captures, side-by-side setup, drift checks); create/update level_1_moss_stalker_reference_study.md, level_1_moss_stalker_silhouette_candidates.md, level_1_moss_stalker_reference_parity.md, level_1_moss_stalker_reference_shell_lock.md, and level_1_moss_stalker_translation_plan.md; judge against /home/przemek/projects/MC/docs/ai/mc-model-mob-learning-spec.md, /home/przemek/projects/MC/docs/ai/mc-model-mob-critique-rubric.md, /home/przemek/projects/MC/docs/ai/mc-model-mob-anti-pattern-catalog.md, and /home/przemek/projects/MC/docs/ai/mc-blockbench-review-gate.md; do not edit gameplay files
```

## Production Handoff Contract

The next owner must be able to quote back:
- transferred packet rule IDs it is implementing
- packet rule IDs intentionally untouched
- anti-patterns actively avoided
- target exit bucket for the next art build: `PASS_TO_STYLE`
- exact evidence paths it must capture

If the brief does not supply that, the handoff is incomplete.

## Immediate Next Commands

Dry-run first from `/home/przemek`:

```bash
npm run collab:task -- --dry-run "art direction read-only brief for level_1_moss_stalker in /home/przemek/projects/MC; backend world; Blockbench/reference packet prep; first read MCMMORPG/_validation/work_packets/level_1_moss_stalker_art_brief_packet.md and execute it as the source-of-truth packet; route to the correct MC art agent and show the exact worker path"
```

Then, if the dry-run looks sane, the direct worker command is:

```bash
npm run mc:agent -- art --write-policy read-only --role primary "read-only brief for level_1_moss_stalker in /home/przemek/projects/MC; backend world; first read MCMMORPG/_validation/model_studies/direwolf_family_reference_extraction_packet.md plus /home/przemek/projects/MC/docs/ai/mc-creature-agent-production-runbook.md and /home/przemek/projects/MC/docs/ai/mc-mob-model-agent-pipeline.md; inspect local refs in MCMMORPG plus direct corpus refs in /home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2); use grove_guardian and corrupted_sprout only as local anchor points, not direct templates; treat the direwolf packet as the family-rule source and explicitly say which rule IDs transfer cleanly, which need translation for a grove-corruption stalker, and which must not be copied literally; borrow Littleroom-grade integration discipline, SamusDev-grade role clarity, and Toro-grade silhouette pressure without copying anatomy; return 3 local references with exact paths, 3 direct corpus references with exact paths, 1-3 external principle references, 5-10 extracted principles, 3-5 forbidden traits, 2-3 silhouette directions, one recommended baseline direction, a reference mass map, landmark list, joint-chain map, 4 motion-stress poses, a Layer A vs Layer B split, exact artifact paths under MCMMORPG/_validation/model_studies, and a concrete Blockbench review plan for the next art pass (captures, side-by-side setup, drift checks); create/update level_1_moss_stalker_reference_study.md, level_1_moss_stalker_silhouette_candidates.md, level_1_moss_stalker_reference_parity.md, level_1_moss_stalker_reference_shell_lock.md, and level_1_moss_stalker_translation_plan.md; judge against /home/przemek/projects/MC/docs/ai/mc-model-mob-learning-spec.md, /home/przemek/projects/MC/docs/ai/mc-model-mob-critique-rubric.md, /home/przemek/projects/MC/docs/ai/mc-model-mob-anti-pattern-catalog.md, and /home/przemek/projects/MC/docs/ai/mc-blockbench-review-gate.md; do not edit gameplay files"
```