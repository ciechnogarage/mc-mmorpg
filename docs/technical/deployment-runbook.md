# Domain 16 Deployment Runbook

contract_status: proposed
runtime_status: partially_verified
release_status: blocked
status_vocabulary_ref: docs/technical/status-and-evidence-vocabulary.md#canonical-status-and-evidence-rules

evidence_refs:
  - evidence_id: EVID-MC-LEGACY-HELPER-2026-07-22
    source_type: config
    source_path: MCMMORPG/docker/mc
    observed_at: 2026-07-22
    scope: docker helper commands and legacy runtime selectors
    summary: Legacy helper still operates world/items/hub-style docker profiles and exposes up/down/stop-all/ps/logs/rcon/stats.
  - evidence_id: EVID-MCENV-2026-07-22
    source_type: config
    source_path: MCMMORPG/infra/bin/mcenv
    observed_at: 2026-07-22
    scope: target environment interface for staging and leased dev scopes
    summary: `mcenv` codifies target staging/dev selector semantics and rejects legacy framing.
  - evidence_id: EVID-START-HERE-DOCKER-2026-07-22
    source_type: document
    source_path: MCMMORPG/START_HERE_DOCKER.md
    observed_at: 2026-07-22
    scope: docker runtime QA and isolation flow
    summary: Docker runtime guide documents the current local flow through Velocity plus backend helpers.
  - evidence_id: EVID-PERF-BUDGETS-2026-07-22
    source_type: document
    source_path: docs/technical/performance-budgets.md
    observed_at: 2026-07-22
    scope: memory ceilings and backend concurrency constraints
    summary: Performance budgets record configured ceilings but keep runtime baselines blocked.
  - evidence_id: EVID-BACKUP-RECOVERY-2026-07-22
    source_type: document
    source_path: docs/technical/backup-and-recovery.md
    observed_at: 2026-07-22
    scope: canonical restore and DR gate semantics
    summary: Backup and recovery doc is the source of truth for restore/recoverability gates.
  - evidence_id: EVID-SECURITY-MODEL-2026-07-22
    source_type: document
    source_path: docs/technical/security-model.md
    observed_at: 2026-07-22
    scope: proxy trust boundary, secret surfaces, and admin paths
    summary: Security model defines forwarding trust, secret locations, and admin surfaces.

blockers:
  - blocker_id: BLK-DEPLOY-001
    status_family: release_status
    summary: Deployment path remains anchored to legacy selectors without an explicit mapping into target-state `mcenv` semantics.
    required_evidence: Canonical mapping or cutover artifact from `world/items/hub/minioncore` to target selectors.
    owner: domain-16
    scope: deployment-selector-model
  - blocker_id: BLK-DEPLOY-002
    status_family: release_status
    summary: Required recovery gates remain blocked, so deployment cannot be treated as release-safe.
    required_evidence: Recovery gates cleared or explicitly scoped down for the reviewed slice.
    owner: domain-16
    scope: deployment-preconditions
  - blocker_id: BLK-DEPLOY-003
    status_family: runtime_status
    summary: Current repo proves helper commands and container profiles, but not a tested deployment exercise with rollback evidence.
    required_evidence: Named staging deployment exercise showing the intended flow and rollback triggers.
    owner: domain-16
    scope: deployment-execution

## Scope

This runbook is deployment-facing only.

It:

- defines step order for staging deployment and maintenance restart
- defines rollback triggers and deployment blockers
- references recovery gates from `docs/technical/backup-and-recovery.md`

It does not redefine restore or disaster-recovery semantics.

## Deployment Paths

> **Legacy evidence notice:** references to `world/items/hub` and the
> `docker/mc` commands below document an observed former interface. Current
> repository policy forbids using that routing. Do not execute those commands
> for new work; use an approved `mcenv` target after the missing selector
> mapping is resolved.

The repository records two interfaces, but only the target interface is
eligible for future approved operations:

| Path | Role | Status |
| --- | --- | --- |
| `MCMMORPG/docker/mc` | historical evidence for the former backend split | prohibited for new work |
| `MCMMORPG/infra/bin/mcenv` | target environment interface for staging and leased dev scopes | proven target interface |

Domain 16 interpretation:

- use legacy `docker/mc` only as read-only historical evidence
- prefer `mcenv` semantics for target-state deployment language in any new environment framing
- do not normalize `world/items/hub` as the durable deployment taxonomy

Current limitation:

- no canonical selector mapping exists yet from legacy `world/items/hub/minioncore` slices to target `mcenv` selectors
- any `mcenv` rollout claim therefore remains blocked until the mapping/cutover artifact required by `BLK-DEPLOY-001` exists

## Preconditions

Before a staging deployment claim may advance:

1. Recovery gates from `docs/technical/backup-and-recovery.md` are referenced and not contradicted.
2. If `mcenv` is used, the target selector already has a canonical mapping from the legacy slice being changed.
3. Performance ceilings in `docs/technical/performance-budgets.md` are not exceeded by the intended backend combination.
4. Secret-sensitive surfaces are in the expected state:
   - forwarding secret source
   - MariaDB credentials source
   - RCON path if used for checks
5. Validation channel plan is named:
   - RCON for admin/server checks
   - logs for diagnostics
   - Mineflayer/companion/plugin API for player behavior or structured state when that surface is in scope

## Deployment Step Table

| Step | Precondition | Rollback trigger | Required evidence |
| --- | --- | --- | --- |
| Select target deployment path | exact approved `mcenv staging/dev` environment named | selector mismatch or environment ambiguity | command path + target scope reference |
| Confirm backend isolation | intended backend combination does not violate current concurrency rules | unrelated heavy backend already running or player traffic present | `mc ps`/compose state or `mcenv ps` evidence |
| Confirm recovery prerequisites | recovery gates referenced and unresolved blockers acknowledged | missing restore scope or unknown backup state | links to recovery gates/blockers |
| Start or refresh target backend | helper command chosen and target backend identified | startup failure, config mismatch, or container health failure | command record + logs + container state |
| Verify proxy path | Velocity routes intended backend and trust boundary still holds | routing failure, forwarding mismatch, or auth/session anomaly | proxy/backend logs + selected runtime evidence |
| Run smoke validation | only scoped checks for the slice are executed | critical warnings, missing provider, or integrity mismatch | log evidence + RCON/bot evidence |
| Decide continue or rollback | no release-blocking anomalies found in the slice | blocking anomaly discovered | named decision + blocker/evidence refs |

## Maintenance Window Flow

For staging or leased dev runtime:

1. Name the target slice and expected blast radius.
2. Stop unrelated heavy backends if current concurrency risk is unclear.
3. Apply the minimal runtime change for the selected slice.
4. Restart only the components needed for that slice.
5. Review logs first, then run scoped smoke checks.
6. Escalate rollback if deployment triggers hit.

## Rollback Triggers

Deployment rollback is triggered by deployment-facing evidence such as:

- backend fails to start or remain healthy
- proxy cannot route or forwarding trust appears broken
- critical provider/dependency missing in the active slice
- log review shows blocking runtime error for the changed slice
- current memory/packet ceilings are clearly exceeded by the deployment attempt
- smoke validation contradicts the intended runtime contract

These triggers do not redefine restore/DR policy. They only decide whether to abandon the attempted deployment path.

## Rollback Paths

| Situation | Rollback path | Notes |
| --- | --- | --- |
| evidence refers to a former legacy-helper deployment | stop; obtain a migration/cutover decision instead of operating the legacy selector | recovery semantics still come from backup/recovery doc |
| backend change on `mcenv` path | stop or down target selector, revert environment-specific runtime state, re-up prior selector | target-state path still blocked on selector mapping |
| proxy/routing issue | stop changed backend or proxy-related change, restore last known-good routing inputs | forwarding trust boundary is load-bearing |
| provider/plugin regression | revert changed jar/config set for affected slice only | do not widen rollback scope unless evidence forces it |

## Commands

The former helper command surface remains inspectable in
`MCMMORPG/docker/mc` for migration evidence. It is deliberately not reproduced
as an executable runbook here.

Target-state environment interface:

```bash
MCMMORPG/infra/bin/mcenv preflight
MCMMORPG/infra/bin/mcenv config staging game
MCMMORPG/infra/bin/mcenv config dev <scope>
MCMMORPG/infra/bin/mcenv up|down|stop|logs|ps|build <environment> <selector>
```

## Deployment Blockers For `release_status`

| Deployment blocker | Meaning |
| --- | --- |
| `BLK-DEPLOY-001` | deployment path still anchored to legacy selectors without explicit target-state mapping |
| `BLK-DEPLOY-002` | required recovery gates still blocked |
| `BLK-DEPLOY-003` | no tested deployment exercise proves the intended path |

## Review Notes

- This runbook is intentionally conservative and staging-focused.
- Recovery remains referenced from the recovery contract; this file does not claim restore readiness.
- `mcenv` commands in this file describe the target interface shape, not a currently proven selector mapping for each legacy backend slice.
