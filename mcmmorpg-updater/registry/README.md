# Registry v1

`targets.json` is the root-owned source of Compose identity and filesystem
boundaries. It is validated against the current bytes of
`MCMMORPG/docker/compose.yml`; a changed config digest, service, profile, project,
or bind mount fails closed.

Every target declares one service, no dependency traversal, no recreate, and a
required current-config digest check. `velocity` is a global-entrypoint blast
radius and is never pilot eligible.

`plugins.json` models an installation by `(plugin_id, target)`, so versions,
JAR paths, configuration paths, and mutation policy may differ by target. Each
mutation policy is schema v1, defaults undeclared paths to `excluded`, and binds
its content with `manifest_sha256`. Explicit entries accept only:

- `rollback` with `copy` / `replace-from-backup`;
- `excluded` with `none` / `none`.

Paths must be canonical and below the target data root. World directories and
player-data segments (`playerdata`, `advancements`, `stats`, player/operator
lists) can never be `rollback`. Secret-like paths are not inventory inputs.
