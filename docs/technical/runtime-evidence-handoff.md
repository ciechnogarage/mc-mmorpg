# Domain 16 Runtime Evidence Handoff

contract_status: proposed
runtime_status: partially_verified
release_status: blocked
status_vocabulary_ref: docs/technical/status-and-evidence-vocabulary.md#canonical-status-and-evidence-rules

evidence_refs:
  - evidence_id: EVID-STATUS-VOCAB-2026-07-22
    source_type: document
    source_path: docs/technical/status-and-evidence-vocabulary.md
    observed_at: 2026-07-22
    scope: canonical status meanings, blocker schema, and evidence-reference rules
    summary: Status vocabulary doc is the canonical contract for Domain 16 status and evidence terms.
  - evidence_id: EVID-RUNTIME-REGISTER-2026-07-13
    source_type: document
    source_path: docs/audits/2026-07-13/runtime-evidence-register.yaml
    observed_at: 2026-07-13
    scope: existing runtime evidence register examples
    summary: Runtime evidence register already shows static checks and blocked or insufficient runtime findings.
  - evidence_id: EVID-PERSISTENCE-MATRIX-2026-07-22
    source_type: document
    source_path: docs/tests/persistence-restart-matrix.md
    observed_at: 2026-07-22
    scope: persistence test shape and expected proof inputs
    summary: Persistence matrix is a test-plan input, not standalone proof that persistence checks were executed.
  - evidence_id: EVID-VERTICAL-SLICE-2026-07-22
    source_type: document
    source_path: docs/tests/vertical-slice-e2e.md
    observed_at: 2026-07-22
    scope: end-to-end runtime gate shape through Velocity
    summary: Vertical-slice E2E doc defines expected proof shape for a closed player-visible loop, not standalone runtime evidence.
  - evidence_id: EVID-PERF-BUDGETS-2026-07-22
    source_type: document
    source_path: docs/technical/performance-budgets.md
    observed_at: 2026-07-22
    scope: configured ceilings and blocked runtime baseline gates
    summary: Performance budgets record configured ceilings while keeping release-facing runtime claims blocked.

blockers:
  - blocker_id: BLK-HANDOFF-001
    status_family: runtime_status
    summary: No canonical runtime evidence bundle has yet been produced for the current baseline.
    required_evidence: Named evidence bundle with verdicts, logs, blockers, and reviewed scope.
    owner: domain-16
    scope: runtime-bundle
  - blocker_id: BLK-HANDOFF-002
    status_family: release_status
    summary: Domain 17 cannot use canonical document presence alone as release evidence.
    required_evidence: Runtime bundle containing validation results, blockers metadata, and the reviewed slice.
    owner: domain-16
    scope: release-gating

## Purpose

This document defines what Domain 16 must hand to Domain 17 before claiming:

- contract stable enough to consume
- runtime slice verified
- release gate ready for review

Domain 16 prepares evidence. Domain 17 decides release readiness.

## Canonical Inputs

Every Domain 16 handoff should reference, not duplicate, these canonical documents:

- `docs/technical/status-and-evidence-vocabulary.md`
- `docs/plugin_manifest.yaml`
- `docs/technical/dependency-graph.md`
- `docs/technical/platform-architecture.md`
- `docs/technical/database-topology.md`
- `docs/technical/security-model.md`
- `docs/technical/performance-budgets.md`
- `docs/technical/backup-and-recovery.md`
- `docs/technical/deployment-runbook.md`

## Status Transition Table

This table is derivative. The status vocabulary file remains authoritative.

| Transition | Required evidence | Owner to approve |
| --- | --- | --- |
| `contract_status: draft -> proposed` | canonical document exists, scoped claims have named evidence refs, and blockers are named | domain-16 author/reviewer |
| `contract_status: proposed -> approved` | explicit reviewable artifact exists and contract-scope blockers are resolved | domain-16 owner/reviewer |
| `runtime_status: not_verified -> partially_verified` | runtime evidence exists, but limitations and incomplete scope are named | domain-16 owner/reviewer |
| `runtime_status: partially_verified -> verified` | required runtime evidence for the named slice is present and current | domain-16 owner/reviewer |
| `release_status: blocked -> conditional` | explicit gate list, named evidence requirements, and finite named blockers exist | domain-16 owner/reviewer |
| `release_status: conditional -> approved` | Domain 17 receives a full evidence bundle and all relevant release gates are satisfied | domain-17 gatekeeper/reviewer |

## Minimum Evidence Bundle

Every handoff bundle to Domain 17 should include:

| Field | Meaning |
| --- | --- |
| `scope_id` | stable name for the runtime slice under review |
| `backend` | exact backend or environment under review |
| `commit` | repository commit or working-state reference |
| `paper_version` | runtime Paper version |
| `java_version` | runtime Java version if relevant to the claim |
| `plugin_inventory_ref` | canonical plugin manifest reference |
| `config_refs` | specific files or generated runtime configs reviewed |
| `started_at` / `finished_at` | observation window |
| `runner` | command, script, or actor that produced the evidence |
| `log_paths` | exact logs reviewed |
| `evidence_refs` | full evidence objects attached to the bundle using the canonical schema |
| `evidence_ref_ids` | named evidence IDs reused from canonical docs when only references are needed |
| `blockers` | explicit blocker IDs still open |
| `verdict` | `PASS`, `FAIL`, `BLOCKED`, or `INSUFFICIENT_EVIDENCE` for the scenario |

## Scenario Mapping

| Scenario type | Canonical source reference |
| --- | --- |
| plugin/runtime inventory | `docs/plugin_manifest.yaml` |
| dependency failure or blast radius | `docs/technical/dependency-graph.md` |
| backend/proxy/storage topology | `docs/technical/platform-architecture.md`, `docs/technical/database-topology.md` |
| permissions, forwarding, secret/admin surface | `docs/technical/security-model.md` |
| memory, packet, concurrency, pregeneration limits | `docs/technical/performance-budgets.md` |
| restore/DR readiness | `docs/technical/backup-and-recovery.md` |
| deployment path, maintenance, rollback trigger | `docs/technical/deployment-runbook.md` |
| persistence checks | `docs/tests/persistence-restart-matrix.md` |
| closed gameplay loop | `docs/tests/vertical-slice-e2e.md` |

The two `docs/tests/*` references above are test-plan inputs and scenario templates. They do not count as runtime evidence until an executed bundle names concrete results, logs, timings, and verdicts.

## Handoff Protocol

1. Name the review scope and target backend or environment.
2. Collect full `evidence_refs` objects for new observations and use `evidence_ref_ids` when only reusing already-declared canonical evidence.
3. Record blockers instead of hiding missing proof in prose.
4. State the status transition being requested.
5. Hand the bundle to Domain 17 together with:
   - canonical document refs
   - exact verdicts
   - explicit residual blockers

Do not attach a planned test document by itself as proof that the test ran. The handoff bundle must include execution-specific evidence, not only the scenario definition.

## Current Release Blockers

For the current baseline, Domain 16 should assume `release_status: blocked` until a scope-specific bundle clears these classes of issues:

- missing runtime-safe performance baseline
- missing tested restore path
- mixed storage topology with unresolved active DB consumers
- incomplete anti-cheat or permission authority proof
- missing deployment exercise and rollback evidence where deployment claims are made

## Review Notes

- Existing `docs/audits/2026-07-13/runtime-evidence-register.yaml` is already a useful precedent for evidence formatting.
- Planned test docs are scenario definitions only; they do not become runtime proof until an execution-specific bundle attaches actual verdicts and evidence refs.
- This is not yet a complete Domain 16 handoff bundle by itself; it defines the envelope around one.
