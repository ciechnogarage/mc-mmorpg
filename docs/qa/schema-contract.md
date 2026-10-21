---
title: "Domain 17 QA Schema Contract"
status: baseline
---

# Domain 17 QA Schema Contract

This directory is a schema baseline only. It does not become the runtime source
of truth until `DOMAIN_17_QA_SOURCE_OF_TRUTH_CUTOVER` in
`docs/qa/cutover-checklist.md` is fully satisfied.

## Status Vocabulary

| Field | Allowed values |
| --- | --- |
| `contract_status` | `draft`, `proposed`, `approved`, `blocked` |
| `runtime_status` | `not_tested`, `loaded`, `partial`, `verified`, `blocked` |
| `release_status` | `blocked`, `candidate`, `released` |
| `result.verdict` | `PASS`, `FAIL`, `BLOCKED`, `INSUFFICIENT_EVIDENCE` |
| `release_effect` | `none`, `supports_candidate`, `blocks_release`, `clears_blocker` |

`PASS`, `FAIL`, `BLOCKED`, and `INSUFFICIENT_EVIDENCE` are result verdicts.
They must not be promoted directly to `runtime_status` or `release_status`
without an explicit gate aggregation.

`contract_status: proposed` is a legal transitional Domain 17 migration value.
Legacy `contract_status: accepted` remains illegal in this schema and must be
mapped to `approved` before entering `docs/qa`.

## Schema Surfaces

The executable validator is `node tools/qa/validate-schemas.mjs`.

### `runtime-evidence-registry.yaml`

Required root:

```yaml
schema_version: 1
records: []
```

Each evidence record requires:

| Field | Contract |
| --- | --- |
| `evidence_id` | Stable unique evidence id. |
| `scenario_id` | Must reference `e2e-scenarios.yaml`. |
| `domain`, `system`, `environment`, `test_type` | Non-empty strings. |
| `contract_status`, `runtime_status`, `release_effect` | Must use the allowed vocabulary. |
| `preconditions`, `steps`, `assertions`, `artifacts` | Arrays, present even when empty. |
| `build.paper`, `build.plugin_manifest_hash`, `build.commit` | Required build context. |
| `execution.runner`, `execution.started_at`, `execution.finished_at`, `execution.executed_by`, `execution.log_paths` | Required execution context. |
| `execution.environment_control.lease`, `workspace_root`, `runtime_touched`, `staging_touched`, `rollback` | Required isolation and rollback context. |
| `execution.environment_control.rollback.method`, `proof_paths` | Required rollback metadata. |
| `result.verdict` | Must use the allowed verdict vocabulary. |

When `release-gates.yaml` declares a `build_context`, every evidence record must
match `commit`, `paper`, and `plugin_manifest_hash`.

### `e2e-scenarios.yaml`

Required root:

```yaml
schema_version: 1
scenarios: []
```

Each scenario requires `scenario_id`, `domain`, `system`, `class`,
`release_critical`, `contract_refs`, `acceptance_criteria`,
`required_evidence_types`, `environments`, `preconditions`, `steps`,
`assertions`, and `failure_classification`.

### `release-gates.yaml`

Required root:

```yaml
schema_version: 1
gate_policy: {}
build_context: {}
gates: []
```

Each gate requires `gate_id`, `release_status`, `release_effect`, and
`required_scenarios`. `required_scenarios` must only name scenarios present in
`e2e-scenarios.yaml`. Optional `required_evidence_ids` must only name records
present in `runtime-evidence-registry.yaml`.

### `regression-matrix.yaml`

Required root:

```yaml
schema_version: 1
entries: []
```

Each matrix entry requires `matrix_id`, `scenario_id`, `release_status`,
`release_effect`, and `coverage_level`. `scenario_id` must reference
`e2e-scenarios.yaml`.
