# Database Validation Report

## Status

`NOT LIVE-VALIDATED`.

MariaDB infrastructure is prepared in Docker staging, but no plugin has an approved and proven migration path. `MMOProfiles`, `MMOInventory`, and `MMOCore` still have `mysql.enabled: false`.

## Required Evidence

- connection and schema creation for one selected consumer;
- pre-migration backup;
- idempotent migration report;
- write, restart, crash, and reconnect checks;
- restore into a clean staging volume;
- player-data rollback evidence;
- explicit ownership decision for inventory, balance, permissions, and profile-scoped data.
