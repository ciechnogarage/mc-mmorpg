# Master Product Bible

Status: active

## Product Definition

MCMMORPG targets a persistent Minecraft MMORPG product, not a one-off starter
dungeon or isolated demo slice. The intended play shape is long-term
character-based play across multiple connected systems: progression, world
content, PvE, PvP, social play, cosmetics, and release seasons.

This document defines product-governance truth for Domain `00`. It does not, by
itself, approve runtime behavior, plugin readiness, balance, or implementation
completeness.

## Target Audience And Use Shape

The canonical audience hierarchy, jobs-to-be-done, distinctive promise, and
anti-goals are defined in
[00-tier-0-product-identity.md](00-tier-0-product-identity.md). Its owner-approved
intent is promoted through `PRODUCT-GOV-006..009`; market validation, runtime
fulfilment, and Tier 0 completion remain separate status axes. The exact
thirteen-path scope is canonical in
[00-first-class-path-registry.yaml](00-first-class-path-registry.yaml) through
`PRODUCT-GOV-010`.

Primary audience summary:

- self-directed inhabitants who want persistent MMO-style progression inside
  Minecraft;
- players who want to choose and deepen a meaningful path without mandatory
  combat-first progression or the need to consume all content;
- players who accept specialization and a shared player ecosystem while
  retaining a satisfying individual session loop;
- operators and implementers who need one product-governance reference while
  system-specific contracts still exist downstream

Expected use shape:

- player identity and character progression persist across repeated sessions
- product slices are released as governed loops, not disconnected feature
  experiments
- seasonal systems may reset scoped competitive state, but must not silently
  redefine durable player state

## Product Pillars

1. Persistent character progression across repeat sessions.
2. Thirteen explicitly registered first-class paths without one mandatory path.
3. Clear cross-domain ownership for shared systems, data, and contracts.
4. Release decisions based on explicit gates and evidence, not legacy document
   status labels.
5. Product truth is distinct from runtime truth, contract truth, and observed
   source material.
6. Player identity is earned through path-appropriate practice, decisions,
   mastery, and lasting accomplishment rather than a label or number alone.
7. Product anti-goals are binding boundaries and cannot be bypassed by local
   retention, revenue, schedule, or experiment waivers.

## Explicit Non-Goals

- This document does not approve detailed class, dungeon, quest, economy, PvP,
  pet, or content design.
- This document does not convert imported `accepted`, `partial`, or `blocked`
  labels into Domain 00 truth.
- This document does not declare staging or runtime ready for release.
- This document does not replace domain contracts, ADRs, runtime evidence, or
  release validation artifacts.
- Tier 0 intent and path scope do not prove market demand, path delivery,
  establish anti-goal thresholds, or prove runtime fulfilment.

## Source-Of-Truth Boundary

Observed inputs for Domain `00` governance include:

- [../foundation-index.md](../foundation-index.md)
- [../current-server-plan.md](../current-server-plan.md)
- [../config_authority_registry.yaml](../config_authority_registry.yaml)
- [00-tier-0-product-identity.md](00-tier-0-product-identity.md)
- [00-first-class-path-registry.yaml](00-first-class-path-registry.yaml)
- contracts, ADRs, audits, and legacy design documents under `docs/`

Promotion rule:

- imported material enters as observed input
- unresolved or over-asserted claims remain quarantined until owner review,
  conflict review, and explicit governance decision
- Domain `00` decisions define product-governance truth; contracts and runtime
  evidence define their own axes
- owner-approved planning intent becomes canonical only when promoted into a
  Domain 00 target artifact and decision registry entry

## Domain Map Summary

Domain `00` governs domains `01` through `19`. It owns product definition,
ownership boundaries, cross-domain dependencies, open governance questions, and
product release stages.

| Domain | Scope | Governance state |
| --- | --- | --- |
| `00` | Product governance | `approved` |
| `01` | Data contracts integration | `proposed` |
| `02` | Account, character, persistence | `proposed` |
| `03` | Combat core | `proposed` |
| `04` | Classes builds | `proposed` |
| `05` | Itemization | `proposed` |
| `06` | World progression | `proposed` |
| `07` | PvE endgame | `proposed` |
| `08` | Quests and narrative | `approved_ownership` |
| `09` | Professions and crafting | `approved_ownership` |
| `10` | Economy | `proposed` |
| `11` | PvP and seasons | `approved_ownership` |
| `12` | Companion systems | `proposed_with_blocked_runtime` |
| `13` | LiveOps, monetization, and season programs | `approved_ownership` |
| `14` | UX and onboarding | `proposed` |
| `15` | Asset pipeline | `proposed` |
| `16` | Platform security | `proposed` |
| `17` | QA and release gates | `proposed` |
| `18` | Content production pipeline | `proposed` |
| `19` | Social and guilds | `approved_ownership` |

Approved ownership for 08, 09, 11, 13, and 19 removes the older unresolved
placeholder map. Remaining governance questions are now downstream contract,
persistence, release, or runtime-evidence questions rather than owner ambiguity.

Within approved split, Domain `11` owns competitive-state,
eligibility, reward-envelope, and reset-matrix semantics only.
Domain `13` owns season lifecycle policy, season-program
packaging policy, and title/prestige/cosmetic
persistence-policy framing. Domain `19` owns guild identity
workflow semantics without ownership guild-economy semantics.
## Governance Cross-Links

- Governance decision index: [00-decision-registry.yaml](00-decision-registry.yaml)
- Tier 0 product identity: [00-tier-0-product-identity.md](00-tier-0-product-identity.md)
- First-class path registry: [00-first-class-path-registry.yaml](00-first-class-path-registry.yaml)
- Tier 0 approval evidence: [00-tier-0-owner-approval-record.md](00-tier-0-owner-approval-record.md)
- Ownership registry: [00-domain-ownership.yaml](00-domain-ownership.yaml)
- Open governance questions: [00-open-questions.yaml](00-open-questions.yaml)
- Product release stages: [00-release-roadmap.md](00-release-roadmap.md)
- Cross-domain dependency map: [00-dependency-map.md](00-dependency-map.md)
