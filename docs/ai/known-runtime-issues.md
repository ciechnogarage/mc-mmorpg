# Known Runtime Issues

Purpose: keep current MC runtime problems diagnosable without agents repeating
blind fixes or exposing secrets.

## HuskSync MySQL Failure

Owner: `minecraft-ops-sync`

Symptom: `HuskSync has failed to load`, `Failed to initialize MySQL database
connection`, or `SQLNonTransientConnectionException` in `MCMMORPG/logs/latest.log`.

Current staging MVP status: disabled by moving the HuskSync JAR to
`MCMMORPG/disabled-runtime-blockers/plugins/`. Re-enable only after a real
database baseline exists and boot validation passes.

Read first: `MCMMORPG/logs/latest.log`, HuskSync config names, service/runtime
status evidence. Redact credentials and hostnames.

Do not: edit DB endpoints, credentials, sync schema, production service state,
or player data without explicit approval.

Smallest validation: confirm current log still shows the failure and classify it
as connectivity, credentials, service unavailable, schema, dependency, or plugin
bug before proposing a fix.

## MCPets SQL Fallback

Owner: `minecraft-ops-sync` with `minecraft-mobs-models`

Symptom: `[MCPets] Could not reach SQL database` followed by YAML fallback.

Read first: `MCMMORPG/logs/latest.log`, MCPets config files, pet/model asset
references.

Do not: assume pet gameplay is broken only because SQL failed; verify whether
YAML fallback is acceptable for staging.

Current staging MVP status: YAML fallback is accepted only if pets are not a
required dependency for the first vertical slice.

Smallest validation: classify whether the issue is expected staging fallback or
a release blocker for persistence.

## MythicMobs Duplicate Model Warning

Owner: `minecraft-mobs-models`

Symptom: `[MythicMobs] Entity already contains model with ID {}` or targeter
config warnings.

Read first: `MythicMobs/mobs`, `MythicMobs/skills`,
`ModelEngine/blueprints`, and resource-pack paths.

Do not: keep changing model IDs by guesswork; trace mob ID, model ID, skill ID,
and spawned entity lifecycle.

Smallest validation: inspect the specific mob/skill references and reproduce
with the narrowest spawn/model check on staging.

## MythicDungeons PlayerQuitEvent Error

Owner: `minecraft-dungeon-world` with `minecraft-release-qa`

Symptom: `Could not pass event PlayerQuitEvent to MythicDungeons` during dungeon
instance cleanup.

Read first: `MCMMORPG/logs/latest.log`, MythicDungeons maps/config, instance
world folders, and WorldGuard/Multiverse world names.

Do not: delete instance worlds or generated world data without backup and human
approval.

Smallest validation: confirm whether cleanup errors reproduce on a focused
dungeon enter/exit or disconnect path before changing map/world config.
