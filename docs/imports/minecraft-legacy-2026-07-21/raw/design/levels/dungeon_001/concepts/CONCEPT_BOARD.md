# Concept board — dungeon_001

Brief: cold flooded ruin — mossy `dunstart`, blue-ice `dunmid`, blackstone `dunend` ([`DESIGN_BRIEF.md`](../DESIGN_BRIEF.md), [`LEVEL_BLUEPRINT.md`](../LEVEL_BLUEPRINT.md)).

Zone colors (Iris blue channel): `#000040` start · `#000080` mid · `#0000C0` end — see [`zone-legend.png`](zone-legend.png).

All maps **256×512** RGB, void = black `(0,0,0)`.

## Concept A — S-route (`concept-a-s-route.png`)

Refined S-shaped path from current `dungeon_001_layout`: compact start platform, diagonal mid band, boss pit at south.

| Pros | Cons |
| --- | --- |
| Closest to proven loch routing | Mid still relatively narrow for `fluidHeight: 62` flood read |
| Minimal MD spawn moves | S-turns can feel samey vs brief “flooded ruin” |
| Fastest to validate in playtest | Less arena drama at boss |

## Concept B — Flooded corridor (`concept-b-flooded-corridor.png`) **recommended**

Wide central mid channel (double-width blue band) + shorter connectors; reads as submerged passage at `fluidHeight: 62`.

| Pros | Cons |
| --- | --- |
| Strongest match to brief (flooded mid pressure) | Wider mid = more Iris gen time / chunk surface |
| Clear three-beat pacing (entry → channel → pit) | Requires MD objective markers shifted into channel |
| Synced to staging `dungeon_001_layout.png` | Walkthrough still needed after server reload |

## Concept C — Ruin ring (`concept-c-ruin-ring.png`)

Start arena + ring path in mid wrapping to south boss pit.

| Pros | Cons |
| --- | --- |
| Memorable start orientation | Ring path harder to wire for linear MD objectives |
| Good boss reveal | More layout art iteration before QA stable |
| Strong fantasy “ruin” | Higher risk of spawn edge cases on ring |

## Recommendation

**Concept B** — best brief fit (cold **flooded** ruin), already synced to staging Iris pack. Validate with `npm run capture:dungeon-001-preview` after `/iris reload` or server restart.

## Files

| File | Role |
| --- | --- |
| `concept-a-s-route.png` | Baseline S improvement |
| `concept-b-flooded-corridor.png` | **Chosen** → staging layout |
| `concept-c-ruin-ring.png` | Alternate ring exploration |
| `zone-legend.png` | Zone color reference strip |
