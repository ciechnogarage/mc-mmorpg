# MC Repository Instructions

These rules extend the global agent instructions for
`/home/przemek/projects/MC`.

## Repository shape

This is a mixed Minecraft project containing:

- server configuration and data;
- Java plugin sources;
- Node validation and bot tooling;
- Python RAG services;
- operational and architectural documentation;
- visual-asset source and integration contracts.

There is no single command that validates every part of the repository.

Before changing a subsystem, inspect:

1. its nearest manifest or build definition;
2. its tests or validation scripts;
3. its README or authoritative local documentation;
4. at least one nearby working example.

Treat generated worlds, runtime state, lease state, logs, caches, compiled
artifacts, exports, vendor JARs, credentials, and secrets as data or operational
state rather than ordinary source. Do not edit or regenerate them unless the
task explicitly requires it.

## Sources and truthfulness

Use this evidence order for plugin syntax and runtime behavior:

1. Active local configuration, source, schema, or nearby working example.
2. Current repository documentation and tests.
3. Official vendor documentation.
4. Trusted community sources only when authoritative sources do not answer.

Do not guess:

- permission nodes;
- placeholders;
- plugin commands;
- model or asset identifiers;
- database behavior;
- economy effects;
- lease ownership;
- runtime topology;
- deployment state.

Distinguish design documentation from behavior observed in an actual runtime.

## Runtime targets and leases

The supported interface for new runtime work is
`MCMMORPG/infra/bin/mcenv` using an explicit `staging` or `dev` target.

The names `world`, `items`, and `hub` remain present in historical and observed
runtime evidence. They are not valid selectors or authorized targets for new
work.

Do not start, stop, modify, migrate, or rely on a legacy target unless the task
explicitly concerns legacy operation or topology cutover. For such a task,
inspect the exact current mapping and obtain authority before mutating runtime
state.

### Common runtime preflight

Before runtime work:

1. Identify the exact environment, selector or scope, and behavioral claim to
   be tested.
2. Read the relevant current runtime documentation and target definition.
3. Run `MCMMORPG/infra/bin/mcenv preflight` as a baseline diagnostic.

`mcenv preflight` currently inspects only baseline ports and does not prove that
all ports required by a particular dev lease or staging configuration are free
or owned by the expected process.

### Dev targets

Before using or mutating a `dev` target:

1. Confirm that `MCMMORPG/infra/leases/registry.tsv` already exists before
   running `mc-lease` or `mcenv config dev`. If it is missing, stop and report
   the blocker because the current lease helper may create the registry.
2. Run `MCMMORPG/infra/bin/mc-lease validate`.
3. Resolve the exact scope with
   `MCMMORPG/infra/bin/mc-lease show <scope>`.
4. Confirm that exactly one current lease is accepted by the current
   `mc-lease show` or `mc-lease export-env` implementation. Do not assume that
   only the literal status `active` is resolvable; use the helper's current
   status semantics.
5. Inspect the lease's allowed paths, forbidden paths, validation channels,
   secret source, rollback method, and close/reclaim protocol.
6. Inspect current process, socket, Docker, and container ownership for every
   port declared by the lease—currently proxy, RCON, and database.
7. Render the exact configuration with
   `MCMMORPG/infra/bin/mcenv config dev <scope>`.
8. Inspect every additional host-published port present in the rendered Compose
   configuration and verify its current ownership.
9. Obtain authority for the exact dev runtime mutation before starting,
   stopping, rebuilding, resetting, removing, or otherwise changing it.

Do not read secret contents merely because a lease exposes their path. Access
them only when the requested operation requires them and that access is
authorized.

### Staging targets

Before using or mutating a `staging` target:

1. Confirm that the exact staging selector is supported and its definition
   exists.
2. Render the configuration with
   `MCMMORPG/infra/bin/mcenv config staging <selector>`.
3. Inspect every host-published port declared by the staging definition or
   rendered Compose configuration.
4. Inspect current process, socket, Docker, and container ownership for every
   such port.
5. Obtain explicit approval before starting, stopping, rebuilding, reloading,
   resetting, removing, or otherwise changing staging.

Staging does not use the dev lease registry. Do not run `mc-lease show` for a
staging selector or present a staging target as a dev lease.

### Runtime authority

Read-only inspection such as configuration rendering, process inspection,
socket inspection, logs, or status queries does not by itself authorize a
runtime mutation.

Starting, stopping, rebuilding, resetting, removing, or otherwise changing a
runtime requires authority for that exact target. Confirmation environment
variables or command-line flags are technical safety gates, not substitutes
for user authorization.

Staging is reserved for approved integration, promotion, acceptance, or release
work. Do not start, stop, rebuild, reload, or change staging without explicit
approval.

Lease allocation, activation, close, reclaim, environment synchronization, and
registry changes are operational mutations. Do not perform them unless runtime
provisioning or lease maintenance is in scope and authorized.

## Validation routing

Run checks appropriate to the changed paths and claims.

Common entry points include:

- repository contracts and validators:
  `node --test tests/*.test.mjs`;
- updater:
  `npm --prefix mcmmorpg-updater test`;
- ModelEngine and repository validation:
  select the relevant script from `MCMMORPG/_validation/package.json` and run
  it with `npm --prefix MCMMORPG/_validation run <script>`;
- bots:
  use the relevant script in `bots/package.json`;
- RAG services:
  use the relevant service's test environment and pytest suite;
- Java plugins:
  use the module's checked-in build or validation script.

Do not assume a working Gradle wrapper when none exists.

Do not describe a bot smoke or acceptance command as a unit-test suite.

A parser, schema check, dry run, successful build, generated configuration, or
clean startup log does not prove:

- gameplay behavior;
- visual quality;
- plugin activation;
- correct cross-plugin integration;
- persistence behavior;
- client-visible behavior;
- live readiness.

Those claims require the corresponding isolated runtime, client, logs,
screenshots, protocol evidence, or structured runtime evidence.

Ask before changing or restarting:

- staging or a live server;
- external services;
- worlds or player data;
- economy or permissions;
- secrets or credentials;
- lease and runtime registries.

## Repository skills

Use a repository-specific skill only when its exact domain matches the task.

Use `minecraft-dungeon-artistry` only for dungeon scenes, encounters, pacing,
environmental storytelling, and visual-quality direction. Its references are
domain guidance, not general repository policy.

Empty or removed Minecraft skill directories are not capabilities and must not
be presented as available.

When a repository-specific workflow applies, use it instead of stacking a
generic planning, specification, or review workflow covering the same concern.

## Visual assets

Route new or materially changed visual assets through
`docs/contracts/visual-asset-domain-handoff-contract.yaml` and load only the
role-specific asset skill for the current stage.

Do not patch ModelEngine `.bbmodel` JSON directly. Authoring requires an
available approved transactional Blockbench MCP writer and explicit authority
to mutate the asset source.

Keep `ART_PASS`, `INTEGRATION_PASS`, and `RUNTIME_PASS` separate and bound to
exact source revisions. The asset builder cannot issue its own `ART_PASS`.

Do not install or run Blockbench integrations or mutate staging, runtime,
worlds, player data, economy, permissions, or secrets without the required
approval.

## Completion

Keep changes scoped to the request and preserve unrelated worktree changes.

Finish with concrete evidence:

- changed paths;
- checks run and their results;
- relevant negative or regression checks;
- runtime or visual evidence when such claims are made;
- checks not run and why;
- unresolved limitations;
- any runtime, lease, or external state changed.
