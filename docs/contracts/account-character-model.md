# Account Character Model Contract

Status: proposed
Contract owner: Project B, domain 02
Runtime status: not implemented
Release status: blocked
Date: 2026-07-22

## Purpose

Define the domain-02 persistence boundary for account identity linkage, character lifecycle,
active session state, slot accounting, deletion lifecycle, and the durable
`MMOProfiles.profileId -> character_id` mapping record.

## Identity Chain

```text
minecraft_uuid -> account_id -> many character_id
```

Rules:

- `minecraft_uuid` is account-resolution input, not a character identifier.
- `account_id` is issued by domain 01.
- `character_id` is issued by domain 01 and must not be derived from `minecraft_uuid`.
- `MMOProfiles.profileId` is a technical plugin key mapped to `character_id`.
- `CharacterStage` is presentation only and does not own creator completion or character identity.

## Lifecycle States

```text
CREATING -> PLAYABLE -> DELETE_PENDING -> DELETED -> PURGED
```

Semantics:

- `CREATING` reserves a slot and is not activatable.
- `PLAYABLE` is the only lifecycle state eligible for activation.
- `DELETE_PENDING` remains soft-deleted-by-default and still occupies a slot until delete flow completion.
- `DELETED` releases the slot but preserves restore metadata.
- `PURGED` is terminal and remains out of scope for default runtime behavior in this ticket.

## Creator States

```text
CREATED -> IDENTITY_SELECTED -> RACE_SELECTED -> CLASS_SELECTED -> APPEARANCE_SELECTED -> VALIDATING -> READY
```

`READY` is required before activation. Creator progression is stored in domain 02 even when rendered by `CharacterStage`.

## Adapter Boundary

- `MMOProfilesAdapter` may expose technical `profile_id` plus presentation metadata only.
- The resolver boundary owns `profile_id -> character_id` by consuming the durable domain-02 mapping record and failing closed on unresolved, ambiguous, inconsistent, or unavailable results.
- `CharacterStage` reads domain-02 `lifecycle_status` and `creator_stage`; it must not infer creator completion from a populated MMOProfiles slot.
- `MMOCore` and other character-scoped consumers must receive resolved `character_id` from the boundary adapter, not raw `MMOProfiles.profileId`.

## Activation Gate

```text
lifecycle_status == PLAYABLE
AND creator_stage == READY
AND schema_version_supported
AND session_lock_acquired
AND all_required_profiles_loaded
AND no_concurrent_activation
AND session_epoch_current
```

Activation failures reject closed. A conflicting active session lock or stale `session_epoch`
must block activation rather than degrade into a partial runtime load.

## Slot Accounting

```text
total_character_slots =
    base_character_slots
  + purchased_character_slots
  + granted_character_slots
  + temporary_character_slots
```

```text
available_character_slots =
    total_character_slots
  - occupied_character_count
  - reserved_character_count
```

| State | Slot result |
| --- | --- |
| `CREATING` | reserved |
| `PLAYABLE` | occupied |
| `DELETE_PENDING` | occupied |
| `DELETED` | released |
| `PURGED` | released |

Rules:

- Slot provenance stays auditable as `base`, `purchased`, `granted`, and optional `temporary`.
- Activation and monetization contracts must not collapse slot provenance into a single unauditable cap.
- `reserved_character_count` is derived from `CREATING`.
- `occupied_character_count` is derived from `PLAYABLE` and `DELETE_PENDING`.
- `available_character_slots` is derived data, not an independently authoritative write target.

## Persistence Boundaries

This ticket persists state into four records:

| Record | Boundary | Primary key | Purpose |
| --- | --- | --- | --- |
| `account_record` | `domain02_account_store` | `account_id` | Account linkage, active character pointer, and slot counters |
| `character_record` | `domain02_character_store` | `character_id` | Lifecycle state, creator state, schema version, and deletion lifecycle |
| `active_session_record` | `domain02_active_session_store` | `character_id` | Live session state only |
| `mmoprofiles_profile_mapping_record` | `domain02_mmoprofiles_profile_mapping_store` | `profile_id` | Durable `MMOProfiles.profileId -> character_id` binding |

Boundary rules:

- Lifecycle state and live session state stay in distinct records and must not collapse into one status.
- The durable profile mapping record is technical linkage, not semantic character identity.
- Slot counters live in the account record, not the session record.
- Soft-delete remains the deletion path.

## Canonical Store Schema

Machine-readable record shapes are defined in [account-character-contract.yaml](/home/przemek/projects/MC/docs/contracts/account-character-contract.yaml).

Important sections:

- `state_vocabularies.lifecycle_status`
- `state_vocabularies.creator_stage`
- `state_vocabularies.profile_binding_status`
- `store_records.account_record`
- `store_records.character_record`
- `store_records.active_session_record`
- `store_records.mmoprofiles_profile_mapping_record`

## Ownership Split

- Domain 01 issues `account_id` and `character_id`.
- Domain 02 owns persisted account state, character lifecycle, active session state, and durable profile mapping.
- `MMOProfiles.profileId` remains a technical plugin key.
- Profile resolution behavior belongs to `domain-02-profile-resolution-boundary`, not this ticket.

## Downstream Consumption Contract

Downstream code and docs should consume the store model from two sources only:

1. The human-readable boundary semantics in this document.
2. The machine-readable schema in [account-character-contract.yaml](/home/przemek/projects/MC/docs/contracts/account-character-contract.yaml).

Anything outside those two sources is non-authoritative for the domain-02 store model.
