# Dev Lease Registry

`infra/leases/registry.tsv` is the single source of truth for active `dev-<scope>` sandboxes.

Rules:
- Every active lease owns one and only one `dev-<scope>`.
- `environments/dev/<scope>.env` is a rendered artifact from the registry row, not the source of truth.
- `infra/bin/mcenv config dev <scope>` resolves the lease through `infra/bin/mc-lease`.
- `up` and `build` remain blocked until the lease's real secret file exists at `runtime/dev-<scope>/secrets/runtime.env`.
- Write operations (`allocate --write`, `sync-env`) take an exclusive lock on `infra/leases/registry.lock`; if `flock` is unavailable they fail closed instead of pretending the allocator is safe.
- Close/reclaim protocol must cover ports, runtime, logs, and secrets.
- Close/reclaim is still declarative in the lease; this helper does not execute runtime teardown.

Common commands:
- `infra/bin/mc-lease validate`
- `infra/bin/mc-lease show example-scope`
- `infra/bin/mc-lease sync-env example-scope`
- `infra/bin/mc-lease allocate --scope my-ticket --owner agent-123 --purpose ticket-abc`
