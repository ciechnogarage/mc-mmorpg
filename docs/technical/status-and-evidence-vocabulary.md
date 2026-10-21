# Canonical Status And Evidence Rules

This file is the canonical source of truth for Domain 16 status labels, blocker semantics, and evidence-reference shape.

Every downstream Domain 16 technical document must reference this file instead of redefining status meanings locally.

## Scope

This vocabulary governs:

- `contract_status`
- `runtime_status`
- `release_status`
- blocker records
- evidence references used by Domain 16 technical documents

It does not by itself prove runtime correctness. It defines how proof is described.

## Status Table

| Status family | Allowed value | Meaning | Minimum evidence to advance | Who may advance |
| --- | --- | --- | --- | --- |
| `contract_status` | `draft` | Working document shape; not ready for downstream dependency. | none | document author |
| `contract_status` | `proposed` | Baseline contract exists but still contains open gaps or review debt. | named evidence refs for claims already made | domain-16 author |
| `contract_status` | `approved` | Contract is stable enough for downstream tickets to consume without redefining the surface. | explicit reviewable artifact plus resolved blockers for contract scope | domain-16 owner/reviewer |
| `contract_status` | `superseded` | Contract replaced by a newer canonical artifact. | link to replacement artifact | domain-16 owner/reviewer |
| `contract_status` | `rejected` | Contract shape or claim explicitly not accepted. | rejection reason | domain-16 owner/reviewer |
| `runtime_status` | `not_verified` | Inventory/config exists, but no trustworthy runtime proof is attached. | none | document author |
| `runtime_status` | `partially_verified` | Some runtime evidence exists, but scope is incomplete, conditional, or staging-only. | named runtime evidence refs with limited scope called out | domain-16 owner/reviewer |
| `runtime_status` | `verified` | Required runtime proof for this scope is present and current enough for the claim being made. | named runtime evidence refs covering stated scope | domain-16 owner/reviewer |
| `runtime_status` | `blocked` | Runtime proof cannot currently advance because of an explicit blocker. | blocker record plus any available partial evidence | domain-16 owner/reviewer |
| `runtime_status` | `disabled` | Component intentionally not active in the runtime under review. | inventory/config evidence proving disabled state | domain-16 owner/reviewer |
| `release_status` | `blocked` | Release cannot advance because one or more release gates are unmet. | blocker records referencing missing evidence | domain-16 owner/reviewer |
| `release_status` | `conditional` | Release could advance only if explicitly named gates complete successfully. | named gate list and evidence requirements | domain-16 owner/reviewer |
| `release_status` | `approved` | Required release gates for this scope have evidence attached and no blocking gaps remain. | named release-gate evidence refs | domain-17 gatekeeper or delegated reviewer |

## Blocker Rules

| Field | Required | Meaning |
| --- | --- | --- |
| `blocker_id` | yes | Stable identifier for the blocker |
| `status_family` | yes | Which status family the blocker prevents from advancing |
| `summary` | yes | Short statement of what is missing or broken |
| `required_evidence` | yes | What evidence must exist to clear the blocker |
| `owner` | yes | Who is responsible for clearing or reviewing it |
| `scope` | yes | Which document/component/runtime slice is affected |
| `notes` | no | Optional extra context |

Rules:

- A blocker must be specific enough that another reader can tell what evidence is missing.
- A blocker may not be cleared by prose alone if the blocker explicitly requires runtime evidence.
- Downstream documents may reference blocker ids from another canonical file, but must not silently rename them.

## Evidence Reference Schema

Every full `evidence_refs` entry in Domain 16 documents should use this shape:

| Field | Required | Meaning |
| --- | --- | --- |
| `evidence_id` | yes | Stable identifier used by other docs |
| `source_type` | yes | One of `document`, `inventory`, `config`, `log`, `command`, `runtime_test`, `audit` |
| `source_path` | yes | Canonical repo-relative path or runtime path |
| `observed_at` | yes | Date of observation in `YYYY-MM-DD` form |
| `scope` | yes | What this evidence actually proves |
| `summary` | yes | Short factual summary of the observation |
| `command` | no | Command used to obtain the evidence when relevant |
| `notes` | no | Extra limitations or caveats |

When a nested record only needs to point at already-declared evidence objects, use `evidence_ref_ids` for a list of `evidence_id` strings instead of overloading `evidence_refs`.

## Canonical Usage Rules

Downstream Domain 16 technical documents must follow these rules:

1. Reference this file for status definitions instead of copying the meanings into local status sections.
2. Reserve `evidence_refs` for full evidence objects and `evidence_ref_ids` for lists of previously declared evidence IDs.
3. Name blockers explicitly when a status remains `blocked`.
4. Do not mark anything `verified` or `approved` without at least one named evidence reference.
5. Do not treat an observed boot, installed jar, or config file as automatic release proof.

## Minimal Examples

```yaml
contract_status: proposed
runtime_status: partially_verified
release_status: blocked
blockers:
  - blocker_id: BLK-RESTORE-001
    status_family: release_status
    summary: Restore procedure has not been tested on current staging baseline.
    required_evidence: Named restore test result with current storage scope.
    owner: domain-16
    scope: backup-and-recovery
```

```yaml
evidence_refs:
  - evidence_id: EVID-PLUGIN-DIR-2026-07-22
    source_type: inventory
    source_path: MCMMORPG/plugins
    observed_at: 2026-07-22
    scope: active plugin jars present in runtime inventory
    summary: Active plugin directory contains the jars listed in the baseline manifest.
```
