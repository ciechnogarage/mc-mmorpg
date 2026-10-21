# Persistence And Restart Matrix

Status: planned runtime gate  
Scope: first controlled persistence test; no production migration

## System under test

The first migration candidate is the player profile path through MMOProfiles.
MMOCore and MMOInventory remain read-only participants until the profile test
has passed and their storage ownership is separately verified.

## Required evidence

Every row must record backend, commit, Paper/plugin JAR versions, configuration
hashes, test account UUID, timestamps, pre/post snapshots, and log paths.

| ID | Scenario | Expected result |
| --- | --- | --- |
| PERSIST-001 | Create synthetic profile | Profile has unique test UUID and known baseline fields |
| PERSIST-002 | Write profile state | Write completes without fallback or silent error |
| PERSIST-003 | Graceful full stop/start | Profile reload equals pre-stop snapshot |
| PERSIST-004 | Reconnect after restart | Same profile, stage, class and progression are visible |
| PERSIST-005 | Forced process crash | Recovery does not create a second profile or lose committed fields |
| PERSIST-006 | Restore to clean storage | Restored profile matches the backup snapshot |
| PERSIST-007 | Concurrent/repeated login | One profile owner; no duplicate player state |
| PERSIST-008 | Dependency/storage unavailable | Mutation fails closed and produces an auditable error |

## Procedure

1. Capture repository commit, JAR inventory, relevant config hashes and Docker
   volume identifiers.
2. Create a synthetic account and record profile, class, stage, inventory and
   balance snapshots.
3. Execute a controlled write and capture plugin logs/database evidence.
4. Stop the selected backend through the Docker helper, preserving the volume.
5. Restart the same backend and compare snapshots field-by-field.
6. Reconnect the same account and repeat the comparison through RCON/bot
   observation.
7. Run one crash recovery test in disposable staging storage.
8. Restore a clean copy and verify the original snapshot.

## Pass criteria

- No committed field is lost after graceful restart or crash recovery.
- No duplicate profile, inventory item, balance, or progression is created.
- A storage failure is visible in logs and does not silently fall back to a
  second mutable authority.
- The evidence is reproducible on a clean staging volume.
