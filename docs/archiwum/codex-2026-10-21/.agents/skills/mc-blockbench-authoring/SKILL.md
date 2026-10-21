---
name: mc-blockbench-authoring
description: Author and revise Minecraft visual assets in Blockbench through an explicitly approved MCP writer, covering geometry, UVs, textures, rigs, animations, revision checkpoints, and render evidence. Use for creating or materially changing a ModelEngine `.bbmodel` after an AssetSpec and authoring scope are accepted.
---

# MC Blockbench authoring

Build the asset against the accepted `AssetSpec`. Produce a review candidate, never a self-approved art result.

## Preconditions

Require the exact source path, allowed write scope, accepted reference and baseline, versioned style, rig, and delivery profile references, evidence directory, and required views or poses. Confirm that the current MCP writer and version are explicitly approved for the task and inspect its live tool schemas before calling it.

If the approved writer is unavailable or cannot preserve Blockbench project semantics, stop with `BLOCKED`. Never create, patch, merge, or repair `.bbmodel` as raw JSON, including through shell scripts or generic text editors.

## Authoring loop

1. Open the real project through the approved Blockbench MCP and inspect the outliner, pivots, textures, UV mode, animations, and current revision.
2. Work in narrow checkpoints: silhouette and primary masses, secondary construction, rig and pivots, UV and texture, then animation. Keep non-target axes frozen between critique rounds.
3. Use revision guards and refresh project state after a conflict. Validate before export; do not bypass an MCP error with direct file editing.
4. Review live front, side, back when relevant, three-quarter, player-scale, and required extreme poses. Export current captures and bind them to the exact source revision or hash.
5. Run applicable repository static validators, but treat their result as technical evidence rather than art approval.
6. Hand the current source hash, render hashes or paths, changed scope, known defects, and next review question to a fresh visual critic.

## Load references selectively

- Read `docs/ai/blockbench-mcp-mob-workflow.md` for the current MCP and ModelEngine authoring procedure; verify live capabilities instead of copying remembered tool signatures.
- Read only the relevant construction, rig, texture, animation, and review sections of `docs/ai/modelengine-model-production.md`.
- Read `docs/ai/mc-model-mob-reference-cloning-protocol.md` only for reference-faithful shell reuse or rescue work.
- Read `docs/ai/mc-blockbench-review-gate.md` when preparing the live capture set.

## Hard boundaries

Use `READY_FOR_VISUAL_REVIEW` or `BLOCKED` as the authoring outcome. The builder cannot grant `ART_PASS`, weaken a critic verdict, package a release, or use staging/live, server reloads, world changes, or runtime installation without explicit approval.
