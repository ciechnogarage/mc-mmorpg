---
title: "Domain 17 Foundation Runtime Adapter Contract"
status: baseline
---

# Domain 17 Foundation Runtime Adapter Contract

The adapter boundary is:

```bash
node tools/qa/adapters/foundation-runtime.mjs \
  --input <foundation-report.json> \
  --scenario <scenario_id> \
  --output <evidence-record.json>
```

This command is tools-only. It reads a captured foundation runtime report and
writes one runtime evidence record compatible with
`docs/qa/runtime-evidence-registry.yaml`. It does not run Minecraft, Docker,
RCON, Mineflayer, staging, or a dev backend.

## Input Contract

The input is JSON. Required or defaulted fields are:

| Field | Behavior |
| --- | --- |
| `verdict` | Must be `PASS`, `FAIL`, `BLOCKED`, or `INSUFFICIENT_EVIDENCE`; missing or unknown values map to `BLOCKED`. |
| `requiredProof` | Array of proof levels required by the report. |
| `achievedProof` | Array of proof levels achieved by the report. |
| `build.commit`, `build.paper`, `build.plugin_manifest_hash` | Required for real evidence. Fixture/parser failure outputs default to `baseline-no-runtime`. |
| `execution.*` | Preserved when present; tools-only defaults set `runtime_touched: false` and `staging_touched: false`. |
| `proofArtifacts[]` | Artifact pointers are preserved; large logs are not embedded. |

Supported proof levels are:

```yaml
- STATIC_CONTRACT
- RUNTIME_PROOF
- INTEGRATION_PROOF
- PLAYER_PROOF
```

## Mapping Rules

- Parser failure writes a valid `BLOCKED` record instead of throwing away the
  source path.
- `achievedProof` missing any `requiredProof` degrades `PASS` to
  `INSUFFICIENT_EVIDENCE`.
- Script-level `PASS` does not by itself promote `runtime_status: verified`.
  The adapter emits `runtime_status: partial` for `PASS` until downstream
  evidence-record tickets prove the scenario-specific prerequisites.
- `FAIL` stays product failure evidence.
- `BLOCKED` stays environment or prerequisite blocker evidence and must not be
  reclassified as product `FAIL`.
- The output preserves input path and SHA-256 in `artifacts[0]`.

## Output Contract

The output is a single JSON object that can be embedded under
`runtime-evidence-registry.yaml.records[]`. It includes:

- `migration_snapshot_ref`
- `legacy_source_ids`
- `legacy_sha256`
- `legacy_status_mapping`
- `proof_level_mapping`
- build context
- execution context
- artifact pointers
- `result.verdict`, `result.proof_level`, `result.blocker_ids`, and
  `result.observations`

Fixture outputs validate through `node tools/qa/validate-schemas.mjs` when
placed in a temporary registry with the current scenario, gate, and matrix
contracts.
