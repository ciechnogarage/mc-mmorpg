# Lumberjack Profession Contract

Status: `PROF-02` content contract. This document materializes
Lumberjack-specific domain-09 content without changing parent profession
framework semantics.

## Registry Wiring

Source registry: `docs/contracts/profession-list.yaml`

| Registry field | Lumberjack value | Contract interpretation |
| --- | --- | --- |
| `profession_id` | `prof.lumberjack` | Base gathering profession identity. |
| `display_name` | `Lumberjack` | Player-facing profession name. |
| `progression_profile_ref` | `progression.default` | Uses shared three-axis progression framework. |
| `organization_profile_ref` | `organization.default` | Uses shared one-active-organization framework. |
| `minigame_profile_ref` | `minigame.harvest` | Valuable work uses harvest minigame profile. |
| `rng_profile_ref` | `rng.gathering-standard` | RNG shapes result quality bands, not base output existence. |
| `salvage_profile_ref` | `none` | Lumberjack has no salvage process surface in this contract. |

Capability flags confirmed from registry:

| Capability | Registry value | Lumberjack contract consequence |
| --- | --- | --- |
| `supports_routine_work` | `true` | Routine forestry work is allowed. |
| `supports_valuable_work` | `true` | Valuable forestry work is allowed and requires active harvest play. |
| `supports_workshop_queue` | `false` | No Lumberjack queue-craft or workshop queue surface is authored here. |
| `supports_salvage_basic` | `false` | No basic salvage surface is authored here. |
| `supports_salvage_professional` | `false` | No professional salvage surface is authored here. |
| `supports_active_minigame` | `true` | Harvest minigame profile applies to valuable work. |
| `supports_signature_methods` | `true` | Signature methods require active organization membership. |

Registry note: the Lumberjack registry section is authoritative for IDs and refs.
This contract does not repair registry formatting and does not infer new parent
semantics from it.

## Namespace

Lumberjack content-layer IDs use these namespaces:

| Content type | Namespace |
| --- | --- |
| Recipe families | `recipe-family.lumberjack.*` |
| Common methods | `method.lumberjack.common.*` |
| Organization methods | `method.lumberjack.org.<org-slug>.*` |
| Signature methods | `method.lumberjack.signature.<org-slug>.*` |

Organization scope refs and minigame delta refs remain exactly the registry refs:
`methods.org.lumberjack.*`, `methods.signature.lumberjack.*`, and
`minigame-delta.lumberjack.*`.

## Organization Identities

Lumberjack has exactly 3 organizations, matching registry IDs.

| Organization ID | Registry ref surface | Identity | Distinction |
| --- | --- | --- | --- |
| `org.lumberjack.ironbark-compact` | `methods.org.lumberjack.ironbark-compact`, `methods.signature.lumberjack.ironbark-compact`, `minigame-delta.lumberjack.ironbark-compact` | Charter foresters and hard-timber crews. | Favors structural reads, disciplined felling plans, durable beam-grade timber, and low-waste extraction. |
| `org.lumberjack.greenring-lodge` | `methods.org.lumberjack.greenring-lodge`, `methods.signature.lumberjack.greenring-lodge`, `minigame-delta.lumberjack.greenring-lodge` | Steward lodges maintaining renewable groves. | Favors selective cuts, regrowth-safe work, balanced yields, and strong routine forestry reliability. |
| `org.lumberjack.embergrain-circles` | `methods.org.lumberjack.embergrain-circles`, `methods.signature.lumberjack.embergrain-circles`, `minigame-delta.lumberjack.embergrain-circles` | Resin burners and heat-cured wood specialists. | Favors volatile resin channels, char-preserved materials, high-upside harvest windows, and stricter rank-gated authority. |

These identities vary work style only. They do not replace `prof.lumberjack`,
erase inactive organization progress, alter the one-active-organization rule, or
change already snapshotted process legality.

## Recipe Families

Recipe families are profession-owned content categories. They are not economy
prices, biome placement rules, or domain-05 equipment definitions.

| Recipe family ID | Purpose | Complexity band | Typical process |
| --- | --- | --- | --- |
| `recipe-family.lumberjack.common-timber` | Logs, rough planks, bark, and routine construction timber. | `basic` | Routine work. |
| `recipe-family.lumberjack.clean-cut-board` | Better board stock, straight-grain pieces, and carpentry-ready wood. | `basic` to `standard` | Routine or valuable work. |
| `recipe-family.lumberjack.heartwood-billet` | Dense heartwood, bow-stave blanks, and quality-sensitive billets. | `standard` to `advanced` | Valuable work. |
| `recipe-family.lumberjack.living-grove-harvest` | Selective grove materials requiring health-preserving extraction. | `advanced` to `expert` | Valuable work. |
| `recipe-family.lumberjack.ember-resin-wood` | Resin-rich, heat-cured, or char-stabilized rare timber. | `expert` to `master` | Valuable work. |

Complexity interpretation:

| Complexity band | Lumberjack meaning | Progression reading |
| --- | --- | --- |
| `basic` | Safe, repeatable cutting and trimming of common timber. | Early `Profession Level` access, routine work eligible. |
| `standard` | Common timber work with meaningful grain, knot, and tool-angle control. | Routine or valuable depending on recipe value class. |
| `advanced` | Quality-sensitive cutting where poor reads reduce board grade or usable length. | Requires stronger profession proficiency and method literacy. |
| `expert` | Grove or rare-tree work requiring organization-aware planning and harvest control. | Uses higher `Profession Level`, `Common Profession Rank`, and active organization gates where declared. |
| `master` | Authority-bearing rare timber or resin work. | Requires explicit exams, master projects, or signature eligibility when scoped to an organization. |

## Method Library Subset

This subset establishes the Lumberjack authoring pattern. It contains 9 common
methods and 6 organization-scoped methods, including signatures. That keeps
Lumberjack within the framework distribution target: 60% common and 40%
organization-scoped.

### Common Methods

Common methods belong to `prof.lumberjack` and may not require a specific active
organization.

| Method ID | Slot compatibility | Process compatibility | Hook category |
| --- | --- | --- | --- |
| `method.lumberjack.common.grain-reading` | `Core Technique`, `Wildcard` | routine, valuable | `precision` |
| `method.lumberjack.common.felling-line` | `Control Technique`, `Wildcard` | routine, valuable | `stability` |
| `method.lumberjack.common.axe-rhythm` | `Throughput Technique`, `Wildcard` | routine, valuable | `throughput` |
| `method.lumberjack.common.clean-limb` | `Finish Technique`, `Wildcard` | routine, valuable | `finish` |
| `method.lumberjack.common.board-sorting` | `Finish Technique`, `Wildcard` | routine, valuable | `precision` |
| `method.lumberjack.common.wedge-control` | `Control Technique`, `Wildcard` | routine, valuable | `recovery_assist` |
| `method.lumberjack.common.canopy-survey` | `Core Technique`, `Wildcard` | valuable | `specialization_gate` |
| `method.lumberjack.common.bundle-haul-flow` | `Throughput Technique`, `Wildcard` | routine | `throughput` |
| `method.lumberjack.common.split-listen` | `Control Technique`, `Wildcard` | valuable | `stability_curve` |

Common unlocks may read `Profession Level`, `Common Profession Rank`, and
profession-wide exam or milestone flags. They must not require a fixed active
organization.

### Organization Methods

Organization methods require the matching active organization snapshot at process
start.

| Registry method scope ref | Method ID | Organization ID | Slot compatibility | Hook category |
| --- | --- | --- | --- | --- |
| `methods.org.lumberjack.ironbark-compact` | `method.lumberjack.org.ironbark-compact.load-bearing-read` | `org.lumberjack.ironbark-compact` | `Core Technique`, `Wildcard` | `precision_bonus` |
| `methods.org.lumberjack.greenring-lodge` | `method.lumberjack.org.greenring-lodge.selective-cut` | `org.lumberjack.greenring-lodge` | `Control Technique`, `Wildcard` | `recovery_assist` |
| `methods.org.lumberjack.embergrain-circles` | `method.lumberjack.org.embergrain-circles.resin-pressure-cut` | `org.lumberjack.embergrain-circles` | `Throughput Technique`, `Wildcard` | `risk_profile` |

### Signature Methods

Signature methods are part of the organization-scoped portion of the method
library. Each requires active organization membership, sufficient
`Organization Rank`, and explicit signature eligibility or approval.

| Registry signature ref | Signature method ID | Organization ID | Slot compatibility | Intent |
| --- | --- | --- | --- | --- |
| `methods.signature.lumberjack.ironbark-compact` | `method.lumberjack.signature.ironbark-compact.ironbark-cant` | `org.lumberjack.ironbark-compact` | `Core Technique`, `Wildcard` | Secures beam-grade outputs during valuable hard-timber harvests. |
| `methods.signature.lumberjack.greenring-lodge` | `method.lumberjack.signature.greenring-lodge.grovekeepers-mark` | `org.lumberjack.greenring-lodge` | `Control Technique`, `Wildcard` | Improves recovery and regrowth-safe scoring without bypassing harvest scoring. |
| `methods.signature.lumberjack.embergrain-circles` | `method.lumberjack.signature.embergrain-circles.ember-resin-draw` | `org.lumberjack.embergrain-circles` | `Wildcard` | Raises upside and downside volatility for resin-rich timber harvests. |

All methods are snapshotted into `ProcessSnapshot` by method ID, assigned slot
role, selected variant, active organization ID, and method hook bundle.

## Harvest Minigame Profile

Lumberjack uses registry `minigame_profile_ref: minigame.harvest`.

Shared harvest profile interpretation:

| Component | Lumberjack meaning |
| --- | --- |
| Core loop | Read tree grain and canopy load, choose cut timing, manage fall control, lock harvest quality band. |
| Score bands | Uses shared `critical`, `poor`, `standard`, `strong`, `excellent` ladder. |
| Routine work | May bypass active minigame where recipe value class allows routine work. |
| Valuable work | Requires active harvest minigame participation. |
| Failure handling | Low bands degrade result quality or waste profile but do not remove the base output path after committed result generation. |
| Recovery | Uses parent interruption model and minigame checkpoint/event-log replay. |

Organization deltas remain bounded changes on top of `minigame.harvest`.

| Minigame delta ref | Organization ID | Delta emphasis | Framework limit |
| --- | --- | --- | --- |
| `minigame-delta.lumberjack.ironbark-compact` | `org.lumberjack.ironbark-compact` | More structural-grain checks, narrower high-score windows for beam-grade timber precision. | Changes scoring emphasis and pacing only. |
| `minigame-delta.lumberjack.greenring-lodge` | `org.lumberjack.greenring-lodge` | Softer fall-control curve, stronger recovery from partial mistakes, lower peak waste volatility. | Changes stability and recovery only. |
| `minigame-delta.lumberjack.embergrain-circles` | `org.lumberjack.embergrain-circles` | Higher resin pressure, faster cut timing, larger strong/excellent upside with harsher low-band penalties. | Changes risk tolerance and pacing only. |

No organization delta changes lifecycle states, parent snapshot rules, base output
existence, or the requirement that valuable work uses active minigame play.

## Progression Interpretation

Lumberjack uses the shared progression axes without redefining them.

| Axis | Lumberjack-specific interpretation | Grants within Lumberjack |
| --- | --- | --- |
| `Profession Level` | Practical forestry proficiency earned through completed lumberjack processes. | Access to higher recipe family complexity bands and baseline harvest eligibility. |
| `Common Profession Rank` | Profession-wide standing as a recognized lumberjack. | Access to higher common-method tiers, profession-wide exams, and master-track milestones. |
| `Organization Rank` | Trust and authority inside one active Lumberjack organization. | Access to organization methods, signature methods, organization contracts, and organization-scoped projects. |

Award interpretation:

| Award source | Axis affected |
| --- | --- |
| Completed routine or valuable Lumberjack process | `Profession Level` progress. |
| Lumberjack competency exams, milestone bundles, profession-wide master projects | `Common Profession Rank` progress or promotion. |
| Active-organization contracts, exams, approvals, and projects | Matching organization's `Organization Rank` only. |

Organization switching preserves inactive organization progress. Already started
processes keep the active organization snapshot captured at process start.

## Contract Invariants

- This contract authors only `prof.lumberjack` content.
- Exactly 3 Lumberjack organizations are authored, and all use registry IDs.
- Lumberjack remains a gathering profession with routine work, valuable work,
  active harvest minigame support, and signature method support.
- Lumberjack does not author workshop queue, basic salvage, or professional
  salvage surfaces because registry flags disable those capabilities.
- Lumberjack methods stay in the approved method families: common, organization,
  and signature.
- Lumberjack method distribution is 9 common methods and 6 organization-scoped
  methods, including signatures.
- Lumberjack minigame deltas vary scoring emphasis, risk, pacing, and recovery
  only; they do not replace the shared minigame runtime.
- No parent lifecycle state, transition, progression axis, active-organization
  rule, loadout slot rule, or RNG/base-output rule is changed here.
