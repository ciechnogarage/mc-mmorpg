# Release Roadmap

Status: active

## Purpose

- Track release-facing product milestones and dependency checkpoints for Domain `00`.
- Provide product sequencing without converting unresolved claims into commitments.

## Planning Assumptions

- This roadmap is a product-governance release model, not an implementation backlog.
- Advancing a stage means Domain `00` can make a stronger governance statement,
  not that downstream runtime work is automatically done.
- Open contract, persistence, and blocked-runtime claims must stay visible even
  after ownership ambiguity is removed.
- Evidence for runtime, contracts, and release verification comes from
  downstream domains, especially `01`, `02`, and `17`.

## Release Stages

| Stage | What it proves at product-governance level | Depends on | Gate owner | Evidence needed | Current status |
| --- | --- | --- | --- | --- | --- |
| `R0 - Governance scaffolded` | Domain 00 artifacts separate imported sources from approved truth. | Domain 00 scaffolds plus intake/conflict flow | `00` | active registries, source-of-truth rules, approved governance decision | complete |
| `R1 - Product frame defined` | Product has a stable top-level definition, explicit non-goals, domain map, and release-stage model. | `R0` | `00` | master product bible, ownership registry, roadmap, open questions | complete |
| `R2 - Governance risks visible` | Ownership, contract, persistence, and runtime risks are named rather than hidden in legacy source material. | `R1` | `00` | ownership registry, open questions, dependency map, runtime blockers | complete |
| `R3 - Core release boundaries proposed` | Product-level contract and release boundaries are framed across quests, professions, seasons, guilds, and companion runtime limits. | `R2` | `00` with `17` input | revised dependency map, revised open questions, approved ownership plan references | complete |
| `R4 - Cross-domain ownership closed` | Domain 00 no longer relies on placeholder ownership for 08, 09, 11, 13, and 19. | `R3` | `00` | updated ownership registry, updated product bible, approved `PRODUCT-GOV-003` | complete |
| `R5 - Runtime claims reconciled` | Product claims that depend on runtime state, including companions and persistence-sensitive season transitions, are reconciled with current evidence. | `R4` plus downstream runtime evidence | `17` and `00` | runtime evidence reconciled with target artifacts | blocked |
| `R6 - Product release gate ready` | Domain 00 can make a bounded release-governance statement that supports a controlled release decision. | `R5` plus downstream verification | `00` and `17` | release checklist closure, rollback framing, downstream evidence references | blocked |

## Gate Definitions

`R0` gate:

- required artifacts exist under `docs/product/`
- imported statuses are treated as observed-source metadata only

`R1` gate:

- product definition is concise and stable
- explicit non-goals prevent Domain 00 from becoming a system-design document

`R2` gate:

- governance risks are visible in ownership, open-question, and dependency artifacts
- blocked runtime truth and persistence boundaries are visible without reintroducing placeholder ownership

`R3` gate:

- design, contract, runtime, release, and plugin dependencies are separated
- revised ownership plan is reflected in release-facing dependency edges
- shared plugin use is not mistaken for product dependency proof

`R4` gate:

- ownership registry no longer relies on placeholder ownership for 08, 09, 11, 13, or 19
- product governance has adopted the new ownership split explicitly

`R5` gate:

- blocked runtime directions are reconciled with current evidence
- product wording no longer implies runtime truth where verification is missing
- persistence-sensitive seasonal claims are reconciled with Domain 02 review
- season lifecycle policy and season-program packaging claims no longer blur the 11 versus 13 boundary

`R6` gate:

- Domain 17 verification evidence exists for release-critical downstream domains
- Domain 00 can make a bounded release-governance statement without relying on quarantined claims

## Cross-Domain Readiness Checks

| Check | Domains involved | Why it matters |
| --- | --- | --- |
| Quest contract boundary check | `00`, `06`, `08`, `10`, `14` | `quest-domain-handoff-v1` closes the semantic handoff; implementation and release evidence still gate runtime claims. |
| Profession contract boundary check | `00`, `05`, `09`, `10`, `11` | `profession-domain-handoff-v1` closes output, economy, and PvP overlay semantics; implementation and release evidence remain pending. |
| Seasonal reward envelope check | `00`, `10`, `11`, `13`, `19` | Season-close reward, guild projection, season-program packaging, and guild treasury payout paths must be contract-safe. |
| Persistence boundary check | `00`, `01`, `02`, `11`, `13` | Seasonal competitive reset must stay separate from durable player state. |
| Companion runtime reconciliation | `00`, `02`, `12`, `17` | Companion direction and runtime truth still need explicit reconciliation. |
| Release evidence check | `00`, `17` | Product release framing depends on downstream verification, not prose alone. |

## Risks And Blockers

- `PRODUCT-QUESTION-001` and `PRODUCT-QUESTION-002` are resolved by accepted handoff contracts; `PRODUCT-QUESTION-003` through `PRODUCT-QUESTION-005` remain open.
- Domain `12` companion direction still cannot support stronger release claims while runtime remains blocked.
- Seasonal persistence boundaries still require explicit review with Domain `02`.
- Guild-facing season-program packaging still depends on explicit handoff across Domains `19`, `11`, and `13`.

## Deferred Work

- Downstream contract authoring for reward envelopes, guild projections, guild-facing season-program packaging, and guild treasury payouts.
- Downstream runtime verification and release evidence closure, especially for companions.
