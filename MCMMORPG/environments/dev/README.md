# Dev Environment Files

Files in this directory are not the source of truth for active sandboxes.

- Active `dev-<scope>` allocations live in `infra/leases/registry.tsv`.
- `example-scope.env` is rendered from that registry via `infra/bin/mc-lease sync-env`.
- `template.env` is documentation only; it is not a lease.
- `infra/bin/mc-lease sync-env` uses `infra/leases/registry.lock` and fails if `flock` is unavailable.
- `infra/bin/mcenv config dev <scope>` fails if the scope has no active lease.
