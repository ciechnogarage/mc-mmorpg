---
name: mc-asset-visual-qa
description: Perform independent, read-only visual QA of Minecraft assets using the accepted brief, profiles, references, current source identity, and render evidence. Use after an authoring pass to diagnose visual defects and issue an evidence-bound `ITERATE`, `REBUILD`, `ART_PASS`, `BLOCKED`, or `INSUFFICIENT_EVIDENCE` verdict.
---

# MC asset visual QA

Judge the current asset from fresh context. Do not edit files, operate Blockbench, or accept the builder's explanation as visual evidence.

## Establish independent context

Read only the accepted brief or `AssetSpec`, style and rig profiles, direct references, current capture set, and source/render hashes. Do not read builder rationale, self-scores, claimed improvements, or proposed verdicts before making the first assessment.

Require views appropriate to the profile, including front, side, three-quarter, player-scale, and key animation poses; require rear views when rear rhythm matters. Missing or stale evidence cannot produce `ART_PASS`.

## Review

1. Identify the dominant visible failure before discussing detail.
2. Assess silhouette, proportions, form hierarchy, negative space, character and role read, material and palette hierarchy, gameplay-distance readability, rig deformation, and animation clarity as applicable.
3. Compare current evidence side-by-side with the accepted direction and direct references. Name drift, not just preference.
4. Distinguish a local repair from a shell failure. After two passes without improvement to the dominant read, require `REBUILD` rather than more polish.
5. Bind every verdict to the exact source hash and render hashes or paths. A source change invalidates the verdict.

## Load references selectively

- Read `docs/ai/mc-blockbench-review-gate.md` for evidence requirements and hard visual fail conditions.
- Read only the relevant quality and review sections of `docs/ai/modelengine-model-production.md` for the active asset profile.
- Load `docs/ai/mc-model-mob-anti-pattern-catalog.md` only for rescue work or to classify an observed failure.

## Verdict contract

Return one of `ITERATE`, `REBUILD`, `ART_PASS`, `BLOCKED`, or `INSUFFICIENT_EVIDENCE`, followed by the dominant defect, concrete evidence, one next axis, source/render identity, and any remaining known issue. Grant `ART_PASS` only when all required visual criteria pass and no blocking known issue remains.

Remain read-only. Do not change source, captures, manifests, plugin configuration, staging, or live state. Do not infer `INTEGRATION_PASS` or `RUNTIME_PASS` from visual quality.
