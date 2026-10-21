# mcmmorpg-updater

Host-side controller/executor for MCMMORPG plugin updates. Separate, dependency-free
Node.js project running on the same host as the Docker stack.

This tree implements the approved update-plan contract plus **Tasks 3–5 — the
target/plugin registry, read-only inventory, and candidate qualification**
(`tasks/mcmmorpg-updater-plan.md`). Nothing here performs Compose actions, stages
databases, or runs a deploy/rollback lifecycle.

## Contract surface (`src/contract/`)

| Module | Responsibility |
| --- | --- |
| `canonical.mjs` | Deterministic JSON canonicalization + SHA-256 (reproducible plan hash). |
| `plan-schema.mjs` | Versioned plan schema, structural validation, content-hash integrity. |
| `operation-policy.mjs` | Literal 9-operation allowlist with closed argument schemas. |
| `approval.mjs` | Root-owned, HMAC-signed, TTL'd, nonce'd single-use approval + nonce store. |
| `lock.mjs` | Per-target execution lock abstraction. |
| `report.mjs` | Auditable approval/authorization report. |
| `authorize.mjs` | Fail-closed decision tying the above together. |

The nine executor operations (allowlist): `inventory`, `backup`, `prepare-staging`,
`compose-stop`, `compose-start`, `compose-run-test`, `health-check`, `deploy`,
`rollback`. `rollback` is a first-class, separately-audited operation. There is no
field carrying a shell command and no `advanced`/passthrough escape hatch: argument
schemas are closed, so an undeclared key is rejected by construction.

## Plan / approval shape

```
plan = {
  schemaVersion: 1,
  id: "plan-0001",
  state: "draft" | "approved" | "executed" | "rejected",  // informational
  hash: "<sha256 of content>",                              // envelope self-hash
  content: {            // immutable, hashed
    operator, createdAt,
    targets:    [{ target, currentJar:{path,sha256}, candidateJar:{path,sha256,source}, expectedValidations? }],
    operations: [{ op, target, args? }]
  }
}

approval = {            // minted only by the root secret holder
  planId, planHash, operator, issuedAt, ttlSeconds, nonce, ownerUid: 0,
  mac: "<hmac-sha256 over the record minus mac>"
}
```

The plan `hash` covers `content` only, so the executor recomputes it to detect any
post-approval change. Authorization is granted only when the plan is structurally
valid, its content hash matches, the approval MAC verifies, the approval binds this
exact plan id + hash, is root-owned, within TTL, its nonce is consumed exactly once
(anti-replay), and per-target locks are acquired (anti-concurrent-execution).

## Test

```
cd mcmmorpg-updater
npm test        # node --test
```

## Registry and inventory

`registry/targets.json` pins the four real targets (`world`, `items`, `hub`, and
`velocity`) to their Compose project/service/profile, host/container data roots,
current Compose SHA-256, lifecycle restrictions, pilot eligibility, and blast
radius. `registry/plugins.json` stores versioned installations and fail-closed
mutation policies per plugin/target. The dependency-free validator compares the
registry to `MCMMORPG/docker/compose.yml` and rejects stale digests, unknown
services, profile/mount drift, traversal, and mutation-policy violations.

Run a local inventory from this project directory:

```
npm run inventory
```

The scanner hashes regular, non-symlink JARs directly under each registered
plugin directory and only the configuration files explicitly registered for an
installation. Secret-like paths, worlds, and player data are excluded before any
file open. Snapshots in `data/` and reports in `reports/` use the snapshot
SHA-256 in their filename, are created with exclusive-create semantics, and are
made read-only after fsync.

## Candidate qualification (`src/downloads/`)

Candidate retrieval is fail-closed and injectable: tests use fake DNS and HTTPS
transports and never call a vendor network. Runtime callers can use the native
`createHttpsTransport()` adapter; it connects through the public IP addresses
already validated by `secureFetch()` so DNS is not resolved a second time.

The CLI uses the user-owned `config/trusted-sources.json` policy. On its first
run it creates that file automatically from the reviewed
`config/trusted-sources.json.example` template with mode `0600`; there is no
provisioning or manual copy step. The loader requires a regular non-symlink file,
checks that it belongs to the current UID on platforms that expose UIDs, and
rejects group/world-writable permissions. The operator account can intentionally
edit its own allowlist; the private-file checks keep ownership and permission
failures closed. Every source pins one plugin name/id, owner/project, HTTPS host,
project path, byte limit, same-host redirect limit, optional independent manifest
host, and optional upstream public keys.

`qualifyCandidate()` validates source policy, all DNS answers, redirects, response
size, SHA-256, the ZIP central directory, exactly one root `plugin.yml`, its plugin
name/version, and each requested registry/inventory target before writing. The JAR
is stored read-only at `downloads/<plugin_id>/<sha256>.jar` only after integrity
or exact-hash manual approval succeeds; a caller cannot point the download root
into any MC server path.

An upstream signature or an independent trusted-manifest checksum makes the
candidate eligible for a draft plan. Otherwise the candidate report explicitly
requires a manual approval binding the exact plugin, source, artifact ID and
SHA-256, and `draft_plan` is `null`. Generated plans always have state `draft` and
still require the separate root approval contract before any executor could use
them.

Run the user-level hash-qualification pass directly:

```bash
npm run qualify-candidate -- \
  --source-id placeholderapi-modrinth \
  --artifact-id pIvQcXW8 \
  --url https://cdn.modrinth.com/data/lKEzGugV/versions/pIvQcXW8/PlaceholderAPI-2.12.3.jar \
  --plugin placeholderapi \
  --targets world,items,hub \
  --operator <operator-name>
```

The command always performs a fresh registry-backed inventory scan. This minimal
CLI intentionally accepts no checksum, signature, approval, plan, staging,
Compose, deploy, or shell-command arguments. It writes one read-only candidate
report below `reports/`, emits one JSON record containing the exact candidate
SHA-256 and manual-approval status, and does not persist the JAR or create a draft
plan. A later, separate approval workflow must bind the exact reported hash.

### PlaceholderAPI / Modrinth

`config/trusted-sources.json.example` contains the approved PlaceholderAPI
artifact boundary from the supplied Modrinth project: CDN host
`cdn.modrinth.com`, project data path `/data/lKEzGugV`, and version artifact
path `/data/lKEzGugV/versions/`. It deliberately has neither a trusted manifest
nor a public signature key, because the supplied Modrinth release metadata is
not an independent integrity channel. Therefore every PlaceholderAPI candidate
from this source requires a manual approval bound to its exact SHA-256 before it
is persisted or can produce a draft plan.

## Batch source discovery

Run a full manifest pass with:

```bash
npm run discover-candidates -- --operator <operator-name>
```

The batch command reads `../docs/plugin_manifest.yaml`, imports exact current
manifest JARs from every registered target without writing to those targets,
and emits one immutable report under `reports/`. Every manifest plugin receives
exactly one status: `qualified`, `manual-hash-approval-required`,
`premium-or-login`, `internal-build`, `source-not-found`, or `incompatible`.
Unregistered vendors remain `source-not-found`; the tool never guesses a vendor
or falls back to a mirror.

`config/batch-sources.json` keeps the explicit PlaceholderAPI registration and
also enables a bounded Modrinth-only search policy for unresolved manifest
plugins. The search uses Modrinth's official [Search
projects](https://docs.modrinth.com/api/operations/searchprojects/), [Get a
project](https://docs.modrinth.com/api/operations/getproject/), and [List
project's versions](https://docs.modrinth.com/api/operations/getprojectversions/)
operations. Search hits are hints only: the controller rejects non-exact
title/slug identities without following them, then fetches project metadata and
requires `status: approved`. It evaluates every bounded exact-identity hit and
rejects an ambiguous result instead of trusting the first search result.

An approved project must expose a listed `release` supporting loader `paper` and
the manifest's exact Minecraft version. The selected file is pinned to the
project's official Modrinth CDN path, and its `plugin.yml` name must equal the
verified `plugin.yml` name from the exact current manifest JAR. Project status,
license, search query/hits, and every rejection are recorded in the report.
Missing or ambiguous local JAR identity blocks artifact retrieval.

Modrinth's same-channel file hashes are provenance only, not independent
integrity evidence. Every candidate still passes through the existing candidate
policy; without an independent signature or manifest it is hashed and reported
for manual exact-hash approval but is not persisted and cannot create a draft
plan.

The batch CLI intentionally accepts only `--operator`. It exposes no vendor URL,
target, approval, authentication, download-root, Compose, staging, deploy, or
shell override.

### Explicitly persist a verified batch

After an operator has reviewed an immutable discovery report and explicitly
authorized downloading every manual-hash candidate, run:

```bash
npm run persist-verified-batch -- \
  --report reports/batch-discovery-<sha256>.report.json \
  --operator <operator-name> \
  --confirm-download-all
```

The command accepts no URL, target, hash, source, approval-file, Compose,
staging, deploy, or shell override. It revalidates the report's content-address,
current manifest and target inventory, then repeats the live Modrinth Search,
Project (`status: approved`), Paper/Minecraft release, official CDN, JAR,
`plugin.yml`, target mapping, and exact SHA-256 checks. Only candidates whose
bytes still match the reviewed report hash are persisted.

Successful JARs are read-only files below
`downloads/<derived-plugin-id>/<sha256>.jar`. A failure for one plugin is
reported without deleting earlier successes. Each success has an immutable
per-plugin report, and the run has an immutable batch-download report binding
the operator to every selected SHA-256. Repeating the same approved run
revalidates live state, returns matching existing JARs/reports, and never
overwrites them.
