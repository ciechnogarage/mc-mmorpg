# Minecraft environment foundation

This directory holds the additive foundation for isolated Minecraft environments.

- `infra/compose/foundation.compose.yml` is the parameterized Compose stack for new `staging` and `dev-<scope>` namespaces.
- `infra/bin/mcenv` is the operator entrypoint for the new standard.
- `infra/bin/mc-preflight` reports read-only port ownership for `25565` and `25566`.

The stack under `docker/` is retained only as evidence of the previously observed
`world/items/hub` topology. Current repository instructions forbid using that
routing for new work; use the `mcenv` staging/dev interface only after its
selector mapping and the requested runtime approval exist.
