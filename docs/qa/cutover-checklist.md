---
title: "DOMAIN_17_QA_SOURCE_OF_TRUTH_CUTOVER"
status: baseline
---

# DOMAIN_17_QA_SOURCE_OF_TRUTH_CUTOVER

This is the named cutover checklist for allowing `docs/qa` to become the
normative source of truth for Domain 17 QA release gates. Downstream tickets may
mark each condition met or blocked; they must not redefine the checklist.

## Preconditions

| ID | Condition | Evidence required | Status |
| --- | --- | --- | --- |
| D17-CUTOVER-01 | `docs/qa/schema-contract.md`, `runtime-evidence-registry.yaml`, `e2e-scenarios.yaml`, `release-gates.yaml`, and `regression-matrix.yaml` exist. | Passing `node tools/qa/validate-schemas.mjs`. | open |
| D17-CUTOVER-02 | Legacy evidence inputs are hash-pinned in the migration snapshot. | `docs/qa/legacy-evidence-migration-snapshot.yaml`. | met |
| D17-CUTOVER-03 | Runtime evidence registry contains records for the approved initial release-gated flows. | Evidence ids and validator output. | open |
| D17-CUTOVER-04 | Scenario registry, release gates, and regression matrix have no dangling scenario or evidence references. | Passing `node tools/qa/validate-schemas.mjs` and exemplar scenario reference check. | met |
| D17-CUTOVER-05 | QA adapters can collect or read the required runtime evidence without changing schema semantics. | Adapter command output and evidence paths. | open |
| D17-CUTOVER-06 | Release readiness report consumes `docs/qa` and fails closed on missing proof. | Report path and command output. | open |
| D17-CUTOVER-07 | `docs/current-server-plan.md` explicitly names `docs/qa` as the active Domain 17 QA source of truth. | Commit or diff reference. | open |

## Cutover Rule

Until every precondition is met, the active runtime truth remains the legacy
inputs named in the Domain 17 technical plan. Creating or validating this
baseline does not promote `docs/qa` to source-of-truth status.
