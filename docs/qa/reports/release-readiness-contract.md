---
title: "Domain 17 Release Readiness Evaluator Contract"
---

# Domain 17 Release Readiness Evaluator Contract

Command:

```sh
node tools/qa/reports/release-readiness.mjs --env staging
```

The evaluator consumes `docs/qa/e2e-scenarios.yaml`, `docs/qa/release-gates.yaml`, and `docs/qa/runtime-evidence-registry.yaml` directly. It writes deterministic stage-1 readiness artifacts to `docs/qa/reports/release-readiness-<env>.json` and `docs/qa/reports/release-readiness-<env>.md`.

Contract guarantees:

- Treats checked-in gates as evidence-driven candidate contracts. Candidate support is determined by evidence and gate requirements, not by manually rewriting `release_status` after evidence appears.
- Selects the latest valid evidence per scenario for the release-gate build context.
- Uses current time (`Date.now()`) for stale-age evaluation unless `--now <iso>` is provided.
- Rejects stale evidence when build context differs or the evidence age exceeds `gate_policy.evidence_max_age_hours`.
- Enforces `required_evidence_ids` semantically during gate evaluation: a gate with required evidence IDs cannot pass unless the selected valid scenario evidence is one of those IDs.
- Propagates BLOCKED prerequisite and environment blocker records across target environments, so no manual reinterpretation is needed for no-lease blocker evidence.
- Aggregates `contract_status`, `runtime_status`, and `release_status` fail-closed.
- Rejects promotion-shaped records (`PASS`, `runtime_status: verified`, or `release_effect: supports_candidate`) unless they include every scenario `required_evidence_types`, every scenario/gate contract ref, and every gate `required_metadata` field.
- Rejects promotion-shaped records unless `contract_status: approved`, `execution.environment_control.runtime_touched: true`, `execution.environment_control.lease` is a concrete `dev-*` lease, and runner/log/artifact/proof provenance is not fixture/test-only.
- Never touches runtime or staging state.

The JSON report is the evaluator input/output contract for downstream automation. The Markdown report is a human-readable rendering of the same result.

Positive path:

- `tools/qa/adapters/foundation-runtime.mjs` can emit `runtime_status: verified` and `release_effect: supports_candidate` from fixture/runtime reports when `verdict: PASS` and all required proof levels are achieved.
- Stage-1 candidate support still requires evaluator-valid `evidence_types`, `contract_refs`, `metadata`, build context, runtime status, verdict, release effect, approved contract status, real runtime touch, approved `dev-*` lease, and non-fixture provenance.
- Tests use runtime-shaped temporary fixtures for this path. Do not create real PASS records in `docs/qa/runtime-evidence-registry.yaml` without lease-backed runtime evidence.
