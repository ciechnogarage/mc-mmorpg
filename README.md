# MC MMORPG Configuration and Content

This repository contains the versioned configuration, content, validation
helpers, and design documentation for the MC MMORPG Paper server.

## Scope

- `MCMMORPG/` contains server configuration, plugin content, ModelEngine source
  assets, Docker orchestration, and validation helpers.
- `docs/` contains the product foundation, runtime constraints, plugin
  inventory, and compatibility documentation.
- `.agents/` contains repository-local task-agent procedures.

Runtime data, binaries, worlds, player data, caches, generated packs, and
credentials are intentionally excluded by `.gitignore`.

## Start Here

Read [the Docker startup guide](MCMMORPG/START_HERE_DOCKER.md) before starting
the local staging environment. The current server and plugin constraints are
documented in [the server environment](docs/server_environment.md), while
[the foundation index](docs/foundation-index.md) explains the status of the
design documents.

## Validation

Run the repository checks from the repository root:

```bash
node scripts/repo-hygiene.mjs --audit
node --test tests/repo-hygiene.test.mjs tests/plugin-inventory.test.mjs tests/contract-registry.test.mjs
```

RCON helpers require `RCON_PASS` in the environment. Do not place passwords,
tokens, player data, or generated runtime state in version control.
