# PetCore platform kernel

This isolated module is the only planned durable companion authority. It has
no MythicMobs pet definitions, entity spawning, policies, commands or MCPets /
CompanionCore dependency.

It provides a policy-free kernel for:

- `PetSubjectId` session epochs and stale-session fencing;
- MariaDB profile/content-run projections with transaction locks and
  idempotency keys;
- a testable `ContentEligibilityProvider` seam, monotonic social-run state and
  reconnect/restart-safe projection reads;
- synchronous admission, bounded work queues and telemetry; and
- immediate action denial plus fenced recall within five scheduler ticks.

`database.enabled` defaults to `false`. In that state the Paper shell loads
only to expose a deliberately fail-closed platform: it cannot summon, execute
abilities, or fall back to YAML/MCPets. Do not enable it until the deployed
MariaDB namespace and installed MMOProfiles/MythicDungeons APIs have been
verified; this ticket intentionally does not guess either adapter.

Build and run the dependency-free contract tests:

```bash
bash MCMMORPG/plugin-src/petcore/tools/build-petcore.sh
```

## Domain 02 Phase-1 Compatibility

- Phase 1 keeps `PetSubjectId = MMOProfiles.profileId` as the durable PetCore key.
- Every summon and active companion action must fail closed unless that legacy key read-through resolves to exactly one domain-02 `character_id`.
- `unresolved`, `ambiguous`, `inconsistent`, and `unavailable` domain-02 mapping states are blocking states, not fallback paths.

Phase-2 rewrite is explicitly out of scope here. Before any future subject rewrite from `MMOProfiles.profileId` to `character_id`, three preconditions must already be true:

- a live domain-02 mapping adapter exists and proves unique read-through resolution for active PetCore subjects,
- blocked-state evidence exists for unresolved and ambiguous mappings, and
- a one-time migration plan exists that rewrites stored PetCore subjects without introducing dual authority between the legacy key and `character_id`.
