# MCMMORPG Audit Verification

Date: 2026-07-13  
Repository: `/home/przemek/projects/MC`  
Scope: verification of the supplied production-readiness audit against the current checkout

## Verdict

The audit is directionally correct: the repository has solid configuration-as-code and validation foundations, but the runtime is not production-ready. The strongest blockers remain persistent-data ownership, unverified database migration and restore, unresolved cross-domain authorities, and missing runtime/gameplay evidence.

The audit is not fully current. MariaDB infrastructure has since been added to the Docker staging stack, but it is explicitly marked as prepared and not live-validated. The vendor-content archive is an external supplied source, not part of the Git checkout; its reported statistics are therefore not reproducible from repository files alone, but are reproducible from the supplied archive evidence.

Current maturity remains approximately **2.4/5**, with the important qualification that this is an evidence maturity assessment, not a claim that every documented target is implemented.

## Proved Facts

### Persistent data

- `MMOProfiles`, `MMOInventory`, and `MMOCore` all have `mysql.enabled: false` in their tracked configuration.
- MMOProfiles currently synchronizes inventory, ender chest, balance, and permissions; see `MCMMORPG/plugins/MMOProfiles/config.yml`.
- MariaDB is present as a private Docker service with a healthcheck and no host port mapping in `MCMMORPG/docker/compose.yml`.
- The database baseline explicitly says the infrastructure is “prepared, not yet live-validated” and that no plugin SQL migration has been enabled; see `docs/database-staging-baseline.md:3-8`.
- The current contract registry therefore correctly uses statuses such as `adapter_observed_target_db_unverified` rather than claiming a working database path; see `docs/config_authority_registry.yaml:16-23` and `61-68`.

### Ownership and contracts

- The registry contains 16 contracts and 4 unresolved blockers.
- The unresolved contracts are `pvp_season`, `professions`, `quests_and_objectives`, and `seasons_prestige_cosmetics`; see `docs/config_authority_registry.yaml:124-158`.
- The registry is still explicitly marked `status: draft`; it is a design boundary, not a production approval.
- `python3 scripts/check-contract-registry.py` passes with: `16 contracts validated; 4 unresolved blocker(s).`

### CharacterStage

- CharacterStage uses Java 21 but compiles against `paper-api:1.21.1-R0.1-SNAPSHOT`; see `MCMMORPG/plugin-src/character-stage/build.gradle.kts:8-20`.
- MMOProfiles integration is implemented through a reflection adapter with controlled fallback logging; this remains a compatibility seam requiring an end-to-end runtime test.

### Repository and tests

- The current checkout has four tracked test files covering repository hygiene, plugin inventory, contract registry, and database staging baseline.
- Direct execution with `node --test tests/*.test.mjs` reports 5 top-level test-file subtests, 5 pass, and 0 fail.
- There is no `npm test` script. Running `npm test` fails with `Missing script: "test"`; this is a tooling gap, not a test failure.
- The working tree already contains user changes and untracked files related to the database baseline, Docker Compose, server plan, bots, and documentation. These were preserved.

### Logs and runtime evidence

- `_validation/_server_boot.log` contains startup warnings, including MCPets fallback behavior and a DeluxeMenus NMS-hook warning.
- `MCMMORPG/logs/latest.log` contains a failed port bind and an unexpected server exception. This log cannot serve as a clean release-startup evidence artifact without separating the failed probe from a current clean boot.

## Semantic Drift And Unsupported Claims

### MariaDB wording

The statement that MariaDB does not exist is now stale. The accurate statement is:

> MariaDB staging infrastructure exists, but no plugin migration, persistence flow, backup, restore, or rollback has been live-validated.

### Vendor content statistics

The checkout contains no `vendor-content/littleroom` directory. The supplied archive `OUTPUT — kopia (2).zip` is an external source and is not tracked in Git. Based on the supplied archive evidence, it contains approximately 147 MB and 8,066 entries:

| Type | Count |
| --- | ---: |
| `.yml` | 542 |
| `.bbmodel` | 542 |
| `.png` | 914 |
| `.ogg` | 2,818 |
| `.json` | 1,835 |

The supplied static validation also reports 27 duplicate YAML keys across 17 files, 2 invalid `sounds.json` files, 8 duplicate item IDs, 10 duplicate mob IDs, 44 duplicate skill IDs, and 572 `~onTimer:1` occurrences across 113 files.

These findings are valid for the external archive, not for the active checkout. The repository currently contains about 63 `.bbmodel` files and no `.ogg` files under `MCMMORPG`; that is a different set: active/tracked content versus quarantined vendor library.

### Content validation coverage

The proposed validators for duplicate YAML keys, MythicMobs IDs, model bindings, resource packs, high-frequency skills, and cross-plugin references are not present in `scripts/`. Existing validation is primarily repository/configuration validation plus specialized runtime/model probes.

### Build identity

The manifest records detailed plugin build strings and a Paper runtime hash, which is stronger than semantic versions alone. It does not provide cryptographic hashes for every JAR, so the audit recommendation to pin artifact hashes remains valid.

## External Blockers

These require runtime state, plugin documentation, or a supplied archive and cannot be proved by static repository inspection alone:

- MariaDB connection and schema creation for each plugin.
- File-to-database migration correctness and idempotency.
- Backup, restore, crash recovery, and player-data rollback.
- Whether MMOProfiles inventory/balance/permission synchronization conflicts with MMOInventory, CMI/Vault, or LuckPerms during profile switching.
- Full CharacterStage flow across join, profile selection, class selection, reconnect, restart, and failed MMOProfiles calls.
- Runtime quest/profession fit-gap analysis inside MMOCore.
- MythicMobs/ModelEngine binding, high-frequency skill cost, orphan entity cleanup, and boss MSPT budgets.
- Current clean startup status after the failed-port log was isolated.

## Priority Findings

### P0

1. Approve a player-data ownership contract before enabling any plugin SQL migration or cross-server synchronization.
2. Select one database consumer and prove connect, write, restart, crash, backup, restore, and rollback in staging.
3. Resolve or explicitly defer the four unresolved contract authorities.
4. Produce a clean staging boot artifact with zero new errors and a classified warning baseline.
5. Do not activate vendor content as production content. Keep the supplied archive in quarantine until namespace rewriting, static validation, model binding checks, isolated spawn tests, and performance review pass.

### P1

1. Add content validators and tests for YAML/JSON syntax, duplicate IDs, model bindings, references, and high-frequency skills.
2. Complete CharacterStage end-to-end smoke coverage.
3. Add release manifest checksums and a reproducible release-candidate evidence directory.
4. Establish Spark/MSPT evidence for one vertical slice, including one boss and one dungeon lifecycle.

## Verification Commands

Successful checks:

```text
node --test tests/*.test.mjs
tests: 5
pass: 5
fail: 0

python3 scripts/check-contract-registry.py
16 contracts validated; 4 unresolved blocker(s).
```

Non-successful but informative check:

```text
npm test
Missing script: "test"
```

The proper project-level test entrypoint should be added only as a separate tooling change; this audit does not modify `package.json`.

## Conclusion

The supplied audit should be accepted as a reliable direction-setting assessment, with the corrections above. This document is an audit verification/errata, not a complete replacement for the original 49-point production-readiness audit. The production blocker is not repository organization. It is the absence of runtime evidence proving data ownership, persistence, recovery, performance, and release rollback for one complete vertical slice. The external vendor archive is real and statically problematic, but it is not active checkout content and must remain quarantined. The evidence package remains blocked until the archive's actual SHA-256 is recorded.
