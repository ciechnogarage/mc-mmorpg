# Travel And Discovery Contract

Status: proposed
Contract owner: Project B, domain 06
Runtime status: not implemented
Release status: blocked
Date: 2026-07-22

## Purpose

This contract defines product-level travel, access, and discovery rules
for domain `06`.

It is the semantic owner of:

- hub taxonomy
- waypoint taxonomy
- discovery states
- reveal rules
- service access guardrails
- relationship between exploration and milestone gating

It does not define runtime transport commands, coordinates, plugin
behavior, or layout implementation.

## Travel Vocabulary

| Term | Meaning |
| --- | --- |
| `capital_hub` | primary regional service and orientation hub for an act or island |
| `field_hub` | forward-operating world hub near active regional space |
| `dungeon_camp` | pre-instance staging point for a dungeon sector |
| `event_node` | special-purpose location tied to escalation or event state |
| `waypoint` | discoverable travel anchor or return point |
| `discovery_state` | semantic state describing player awareness and access to a location |

## Core Rules

- Travel must support exploration, not replace it with menu-only progression.
- Islands do not expose difficulty selection through travel surfaces.
- Discovery should improve orientation and anticipation, not force a
  single corridor path.
- Baseline services must remain available without deep campaign progression.
- Discovery and travel rules must respect `docs/contracts/unlock-rules.yaml`.

## Regional Access Model

The default island access model is:

- `70-80%` of the island region available on initial access
- `20-30%` of the island region gated by regional milestones

Gated access should focus on:

- enclaves
- finale spaces
- shortcuts
- special events
- escalation zones

Baseline access must preserve:

- core services
- baseline exploration
- primary activity loop

## Hub Taxonomy

| Hub type | Role | Required expectations |
| --- | --- | --- |
| `capital_hub` | entry orientation, services, progression briefing | must provide baseline services and routing clarity |
| `field_hub` | support regional progression near active content | should reduce friction without bypassing discovery |
| `dungeon_camp` | staging for `DNG-xx` access and return | should frame sector entry, recovery, and replay loop |
| `event_node` | focal point for escalations or special beats | may be temporary, stateful, or milestone-gated |

## Waypoint Taxonomy

| Waypoint type | Meaning |
| --- | --- |
| `entry_waypoint` | initial act or island arrival point |
| `hub_waypoint` | stable service-oriented travel anchor |
| `field_waypoint` | progression-support anchor deeper in a region |
| `camp_waypoint` | dungeon-adjacent staging anchor |
| `event_waypoint` | event-linked anchor, optionally state-dependent |

## Discovery State Model

```text
UNSEEN -> VISIBLE -> DISCOVERED -> ENTERED -> RESOLVED
                              \-> ESCALATED
```

### Discovery States

| State | Meaning |
| --- | --- |
| `UNSEEN` | player has no in-world knowledge or map-level exposure |
| `VISIBLE` | player has learned that the location exists |
| `DISCOVERED` | player has identified the location as reachable or relevant |
| `ENTERED` | player has physically reached the location |
| `RESOLVED` | player has completed the location's current expected beat |
| `ESCALATED` | location has entered a higher-risk or altered state |

## Reveal Rules

- A location may become `VISIBLE` before it becomes traversable.
- A location may become `DISCOVERED` through proximity, quest direction,
  world signage, NPC guidance, or environmental foreshadowing.
- Reveal should support regional fantasy and conflict identity.
- Discovery should not depend on hidden UI-only logic as the primary source
  of orientation.
- Escalated spaces may reframe previously discovered locations without
  invalidating prior discovery.

## Service Access Guardrails

| Service category | Access rule |
| --- | --- |
| repair/storage/crafting baseline | must remain available from baseline progression |
| travel return points | must support recovery from ordinary exploration or dungeon entry |
| primary progression routing | must remain understandable without full campaign completion |
| prestige or advanced loops | may require deeper discovery or milestone progress |

## Relationship To Unlock Rules

This contract consumes `docs/contracts/unlock-rules.yaml` and must not
redefine:

- `unlock_next_act`
- milestone taxonomy ownership
- gate object authority
- floor unlock semantics

Travel and discovery may describe how gated spaces are surfaced to the
player, but not whether the gate semantically exists.

## Template Obligations For Future `ISL-*`

Every future island specification should define:

1. capital hub presence or equivalent entry structure
2. field progression hubs, if any
3. dungeon camp relationship to its paired `DNG-xx`
4. waypoint categories used on the island
5. discovery-state transitions relevant to the region
6. what is open on first access
7. what is gated by milestones
8. how regional escalation changes traversal or visibility

## Ownership Boundaries

This contract owns:

- travel semantics
- discovery semantics
- hub taxonomy
- service access guardrails

This contract does not own:

- act topology
- `ISL/DNG` pairing
- unlock semantics
- dungeon floor sequencing
- runtime fast-travel implementation

## Contract Dependencies

This contract depends on:

- `docs/contracts/world-progression-model.md`
- `docs/contracts/unlock-rules.yaml`

This contract informs:

- `docs/contracts/island-sector-mapping.yaml`
- future `ISL-*` content specs
