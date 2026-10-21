# ModelEngine baseline remediation evidence — 2026-07-20

## Scope and verdict

Ticket targets: `level_1_briarwolf`, `level_1_glimmer_wisp`, and
`level_1_moss_stalker`.

Overall verdict: **BLOCKED_TOOLING / INSUFFICIENT_EVIDENCE**. The safe
generation/cache remediation is complete for Glimmer Wisp and Moss Stalker,
but Blockbench-first source fixes and the complete live behavior gate are not.

| Model | Binding | Quality | Ecosystem (direct checker) | Live gate |
| --- | --- | --- | --- | --- |
| `level_1_briarwolf` | FAIL — source blueprint missing | FAIL — `BLUEPRINT_MISSING` | PASS | not attempted; source is invalid |
| `level_1_glimmer_wisp` | PASS — 18 cache/assets | FAIL — UV bounds | PASS | insufficient evidence |
| `level_1_moss_stalker` | PASS — 61 cache/assets | PASS | PASS | insufficient evidence |

The generated baseline remains FAIL and records the target gate details in
`modelengine-baseline-2026-07-20.json`.

## Applied remediation

- Synchronized the ModelEngine-generated model JSON for Glimmer Wisp (18
  files) and Moss Stalker (61 files) from the sanctioned `world` backend.
- Synchronized `.data/cache.json` from that runtime generation. Semantic cache
  comparison before the copy found 103 changed entries and all 103 belonged
  to the three ticket targets: stale Briarwolf entries were removed, while
  Glimmer Wisp and Moss Stalker entries were added. No unrelated cache entry
  differed.
- Moved Moss Stalker provenance and render-log JSON out of the ModelEngine
  blueprint import directory into
  `_validation/model_provenance/level_1_moss_stalker/`. ModelEngine had tried
  to import both sidecars and logged `Unknown format`; the evidence was
  preserved without changing the `.bbmodel`.
- Did not edit any `.bbmodel` because native Blockbench MCP health failed.

## Static verification

Commands run from `MCMMORPG/`:

```text
node _validation/check_modelengine_binding.js
node _validation/check_modelengine_quality.js --model level_1_briarwolf
node _validation/check_modelengine_quality.js --model level_1_glimmer_wisp
node _validation/check_modelengine_quality.js --model level_1_moss_stalker
node _validation/check_modelengine_ecosystem.js --model level_1_briarwolf
node _validation/check_modelengine_ecosystem.js --model level_1_glimmer_wisp
node _validation/check_modelengine_ecosystem.js --model level_1_moss_stalker
node _validation/check_modelengine_baseline_matrix.js
```

Results:

- Binding: Briarwolf FAIL; Glimmer Wisp PASS (18 entries/assets); Moss Stalker
  PASS (61 entries/assets).
- Quality: Briarwolf FAIL `BLUEPRINT_MISSING`; Glimmer Wisp FAIL
  `UV_OUT_OF_BOUNDS` for `core`, the north/east/west/south plates, `cap`, and
  the left/right antler parts; Moss Stalker PASS.
- Direct ecosystem checker: PASS for all three models.
- Baseline generator: FAIL overall. It still labels ecosystem evidence
  insufficient despite the corresponding direct checker exiting 0; treat
  that as a validator/reporting mismatch, not ecosystem runtime proof.

## Runtime evidence and boundary

Actual `world` backend evidence from `docker/servers/world/logs/latest.log`:

- Full backend boot imported `level_1_glimmer_wisp.bbmodel` at 09:17:55 and
  `level_1_moss_stalker.bbmodel` at 09:17:55; Paper reached `Done` at 09:17:55.
- `meg reload` re-imported Glimmer Wisp and Moss Stalker at 09:18:07.
- `mm reload` completed at 09:18:08.
- The source blueprints used by the backend matched the repository hashes:
  Glimmer Wisp `5ac21c08bd2af1226808e8958f2c29e865380d510806d51dc8759ec0ff4a7161`;
  Moss Stalker `aa07eb1c16a7d17e5d3265faa96489a767c44daf5f901d6057dff9d2e31feca4`.

This is import/restart/reload evidence only. It is not a live visual or
behavior PASS. A final post-sidecar-cleanup reload and bounded spawn attempt at
11:21:44 failed before RCON because `srv-world` had been stopped concurrently.
No claim is made for spawn, animation, death, unload/load, relog, or visual
inspection.

## Exact blockers

1. `BLOCKED_TOOLING`: configured Blockbench MCP endpoints
   `127.0.0.1:8788/mcp` and `localhost:3000/bb-mcp` both refused connections.
2. Briarwolf has no authored source blueprint. Reconstructing one from cache
   would violate Blockbench-first and provenance requirements.
3. Glimmer Wisp's UV corrections require native Blockbench inspection/editing;
   raw `.bbmodel` mutation is prohibited.
4. Runtime acceptance remains incomplete because the backend became
   unavailable before spawn and state-flow checks. Fresh SHA-bound evidence is
   still required for spawn, animations, death, unload/load, relog, and a full
   post-fix restart.
5. The baseline matrix's ecosystem-manifest classification disagrees with the
   direct ecosystem checker and should be corrected separately before relying
   on that column as an authoritative verdict.
