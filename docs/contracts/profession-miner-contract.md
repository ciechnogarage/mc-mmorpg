# Miner Profession Contract

Status: `PROF-01` content contract. This document materializes Miner-specific
domain-09 content without changing parent profession framework semantics.

## Registry Wiring

Source registry: `docs/contracts/profession-list.yaml`

| Registry field | Miner value | Contract interpretation |
| --- | --- | --- |
| `profession_id` | `prof.miner` | Base gathering profession identity. |
| `display_name` | `Miner` | Player-facing profession name. |
| `progression_profile_ref` | `progression.default` | Uses shared three-axis progression framework. |
| `organization_profile_ref` | `organization.default` | Uses shared one-active-organization framework. |
| `minigame_profile_ref` | `minigame.extraction` | Valuable work uses extraction minigame profile. |
| `rng_profile_ref` | `rng.gathering-standard` | RNG shapes result quality bands, not base output existence. |
| `salvage_profile_ref` | `none` | Miner has no salvage process surface in this contract. |

Capability flags confirmed from registry:

| Capability | Registry value | Miner contract consequence |
| --- | --- | --- |
| `supports_routine_work` | `true` | Routine mining work is allowed. |
| `supports_valuable_work` | `true` | Valuable mining work is allowed and requires active extraction play. |
| `supports_workshop_queue` | `false` | No Miner queue-craft or workshop queue surface is authored here. |
| `supports_salvage_basic` | `false` | No basic salvage surface is authored here. |
| `supports_salvage_professional` | `false` | No professional salvage surface is authored here. |
| `supports_active_minigame` | `true` | Extraction minigame profile applies to valuable work. |
| `supports_signature_methods` | `true` | Signature methods require active organization membership. |

Registry note: the Miner registry section is authoritative for IDs and refs, but
some scalar fields in the current YAML are sparsely formatted. This contract does
not repair registry formatting and does not infer new parent semantics from it.

## Namespace

Miner content-layer IDs use these namespaces:

| Content type | Namespace |
| --- | --- |
| Recipe families | `recipe-family.miner.*` |
| Common methods | `method.miner.common.*` |
| Organization methods | `method.miner.org.<org-slug>.*` |
| Signature methods | `method.miner.signature.<org-slug>.*` |

Organization scope refs and minigame delta refs remain exactly the registry refs:
`methods.org.miner.*`, `methods.signature.miner.*`, and
`minigame-delta.miner.*`.

## Organization Identities

Miner has exactly 3 organizations, matching registry IDs.

| Organization ID | Registry ref surface | Identity | Distinction |
| --- | --- | --- | --- |
| `org.miner.deep-vein-guild` | `methods.org.miner.deep-vein-guild`, `methods.signature.miner.deep-vein-guild`, `minigame-delta.miner.deep-vein-guild` | Surveyors and deep-shaft extractors. | Favors patient reading of vein structure, controlled depth work, and rare-node identification. |
| `org.miner.stoneward-union` | `methods.org.miner.stoneward-union`, `methods.signature.miner.stoneward-union`, `minigame-delta.miner.stoneward-union` | Safety-first crews and public works miners. | Favors stable yields, hazard mitigation, reinforced tunnels, and predictable routine extraction. |
| `org.miner.blackcore-order` | `methods.org.miner.blackcore-order`, `methods.signature.miner.blackcore-order`, `minigame-delta.miner.blackcore-order` | High-risk ore hunters working volatile seams. | Favors pressure reads, dangerous deposits, high-upside scoring, and strict rank-gated authority. |

These identities vary work style only. They do not replace `prof.miner`, erase
inactive organization progress, alter the one-active-organization rule, or change
already snapshotted process legality.

## Recipe Families

Recipe families are profession-owned content categories. They are not economy
prices, world-node placement rules, or domain-05 equipment definitions.

| Recipe family ID | Purpose | Complexity band | Typical process |
| --- | --- | --- | --- |
| `recipe-family.miner.surface-stone` | Stone, gravel, common construction inputs. | `basic` | Routine work. |
| `recipe-family.miner.common-ore` | Iron-class and common utility ores. | `basic` to `standard` | Routine or valuable work. |
| `recipe-family.miner.gem-pocket` | Small gem pockets and precision extraction targets. | `standard` to `advanced` | Valuable work. |
| `recipe-family.miner.deep-vein-ore` | Deep deposits requiring vein reading and control. | `advanced` to `expert` | Valuable work. |
| `recipe-family.miner.volatile-core` | Unstable, high-risk rare deposits. | `expert` to `master` | Valuable work. |

Complexity interpretation:

| Complexity band | Miner meaning | Progression reading |
| --- | --- | --- |
| `basic` | Safe, shallow, repeatable extraction. | Early `Profession Level` access, routine work eligible. |
| `standard` | Common ore work with moderate tool control. | Routine or valuable depending on recipe value class. |
| `advanced` | Quality-sensitive extraction where errors lower result band. | Requires stronger profession proficiency and method literacy. |
| `expert` | Deep or unstable deposits requiring organization-aware control. | Uses higher `Profession Level`, `Common Profession Rank`, and active organization gates where declared. |
| `master` | Authority-bearing rare deposit work. | Requires explicit exams, master projects, or signature eligibility when scoped to an organization. |

## Method Library Subset

This subset establishes the Miner authoring pattern. It contains 9 common methods
and 6 organization-scoped methods, including signatures. That keeps Miner within
the framework distribution target: 60% common and 40% organization-scoped.

### Common Methods

Common methods belong to `prof.miner` and may not require a specific active
organization.

| Method ID | Slot compatibility | Process compatibility | Hook category |
| --- | --- | --- | --- |
| `method.miner.common.vein-reading` | `Core Technique`, `Wildcard` | routine, valuable | `precision` |
| `method.miner.common.face-bracing` | `Control Technique`, `Wildcard` | routine, valuable | `stability` |
| `method.miner.common.pick-rhythm` | `Throughput Technique`, `Wildcard` | routine, valuable | `throughput` |
| `method.miner.common.clean-split` | `Finish Technique`, `Wildcard` | routine, valuable | `finish` |
| `method.miner.common.ore-sorting` | `Finish Technique`, `Wildcard` | routine, valuable | `precision` |
| `method.miner.common.dust-control` | `Control Technique`, `Wildcard` | routine, valuable | `recovery_assist` |
| `method.miner.common.seam-mapping` | `Core Technique`, `Wildcard` | valuable | `specialization_gate` |
| `method.miner.common.load-haul-flow` | `Throughput Technique`, `Wildcard` | routine | `throughput` |
| `method.miner.common.micro-fracture-listen` | `Control Technique`, `Wildcard` | valuable | `stability_curve` |

Common unlocks may read `Profession Level`, `Common Profession Rank`, and
profession-wide exam or milestone flags. They must not require a fixed active
organization.

### Organization Methods

Organization methods require the matching active organization snapshot at process
start.

| Registry method scope ref | Method ID | Organization ID | Slot compatibility | Hook category |
| --- | --- | --- | --- | --- |
| `methods.org.miner.deep-vein-guild` | `method.miner.org.deep-vein-guild.depth-triangulation` | `org.miner.deep-vein-guild` | `Core Technique`, `Wildcard` | `precision_bonus` |
| `methods.org.miner.stoneward-union` | `method.miner.org.stoneward-union.reinforced-face` | `org.miner.stoneward-union` | `Control Technique`, `Wildcard` | `recovery_assist` |
| `methods.org.miner.blackcore-order` | `method.miner.org.blackcore-order.pressure-break` | `org.miner.blackcore-order` | `Throughput Technique`, `Wildcard` | `risk_profile` |

### Signature Methods

Signature methods are part of the organization-scoped portion of the method
library. Each requires active organization membership, sufficient
`Organization Rank`, and explicit signature eligibility or approval.

| Registry signature ref | Signature method ID | Organization ID | Slot compatibility | Intent |
| --- | --- | --- | --- | --- |
| `methods.signature.miner.deep-vein-guild` | `method.miner.signature.deep-vein-guild.echo-lode-survey` | `org.miner.deep-vein-guild` | `Core Technique`, `Wildcard` | Identifies rare-node opportunities during valuable deep-vein extraction. |
| `methods.signature.miner.stoneward-union` | `method.miner.signature.stoneward-union.safe-yield-protocol` | `org.miner.stoneward-union` | `Control Technique`, `Wildcard` | Reduces severe degradation risk on valuable work without bypassing scoring. |
| `methods.signature.miner.blackcore-order` | `method.miner.signature.blackcore-order.blackcore-pulse` | `org.miner.blackcore-order` | `Wildcard` | Raises upside and downside volatility for volatile-core extraction. |

All methods are snapshotted into `ProcessSnapshot` by method ID, assigned slot
role, selected variant, active organization ID, and method hook bundle.

## Extraction Minigame Profile

Miner uses registry `minigame_profile_ref: minigame.extraction`.

Shared extraction profile interpretation:

| Component | Miner meaning |
| --- | --- |
| Core loop | Read a deposit pattern, control strike timing, manage instability, lock extraction quality band. |
| Score bands | Uses shared `critical`, `poor`, `standard`, `strong`, `excellent` ladder. |
| Routine work | May bypass active minigame where recipe value class allows routine work. |
| Valuable work | Requires active extraction minigame participation. |
| Failure handling | Low bands degrade result quality or waste profile but do not remove the base output path after committed result generation. |
| Recovery | Uses parent interruption model and minigame checkpoint/event-log replay. |

Organization deltas remain bounded changes on top of `minigame.extraction`.

| Minigame delta ref | Organization ID | Delta emphasis | Framework limit |
| --- | --- | --- | --- |
| `minigame-delta.miner.deep-vein-guild` | `org.miner.deep-vein-guild` | More pattern-reading checks, narrower high-score windows for rare-node precision. | Changes scoring emphasis and pacing only. |
| `minigame-delta.miner.stoneward-union` | `org.miner.stoneward-union` | Softer instability curve, stronger recovery from partial mistakes, lower peak volatility. | Changes stability and recovery only. |
| `minigame-delta.miner.blackcore-order` | `org.miner.blackcore-order` | Higher instability, faster extraction pressure, larger strong/excellent upside with harsher low-band penalties. | Changes risk tolerance and pacing only. |

No organization delta changes lifecycle states, parent snapshot rules, base output
existence, or the requirement that valuable work uses active minigame play.

## Progression Interpretation

Miner uses the shared progression axes without redefining them.

| Axis | Miner-specific interpretation | Grants within Miner |
| --- | --- | --- |
| `Profession Level` | Practical mining proficiency earned through completed mining processes. | Access to higher recipe family complexity bands and baseline extraction eligibility. |
| `Common Profession Rank` | Profession-wide standing as a recognized miner. | Access to higher common-method tiers, profession-wide exams, and master-track milestones. |
| `Organization Rank` | Trust and authority inside one active Miner organization. | Access to organization methods, signature methods, organization contracts, and organization-scoped projects. |

Award interpretation:

| Award source | Axis affected |
| --- | --- |
| Completed routine or valuable Miner process | `Profession Level` progress. |
| Miner competency exams, milestone bundles, profession-wide master projects | `Common Profession Rank` progress or promotion. |
| Active-organization contracts, exams, approvals, and projects | Matching organization's `Organization Rank` only. |

Organization switching preserves inactive organization progress. Already started
processes keep the active organization snapshot captured at process start.

## Contract Invariants

- This contract authors only `prof.miner` content.
- Exactly 3 Miner organizations are authored, and all use registry IDs.
- Miner remains a gathering profession with routine work, valuable work, active
  extraction minigame support, and signature method support.
- Miner does not author workshop queue, basic salvage, or professional salvage
  surfaces because registry flags disable those capabilities.
- Miner methods stay in the approved method families: common, organization, and
  signature.
- Miner method distribution is 9 common methods and 6 organization-scoped
  methods, including signatures.
- Miner minigame deltas vary scoring emphasis, risk, pacing, and recovery only;
  they do not replace the shared minigame runtime.
- No parent lifecycle state, transition, progression axis, active-organization
  rule, loadout slot rule, or RNG/base-output rule is changed here.
