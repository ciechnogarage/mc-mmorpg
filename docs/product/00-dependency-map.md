# Dependency Map

Status: active

## Purpose

Describe cross-domain dependencies across design, contracts, runtime, plugins,
and release gates, while keeping blockers and authority handoffs visible
without turning unresolved claims into approved truth.

## Dependency Principles

- `design` means one domain needs another domain's intended product boundary.
- `contract` means a downstream domain needs a declared interface, schema, or
  authority handoff.
- `runtime` means a running system or persistence path is required before a
  claim can advance.
- `release` means release framing depends on upstream evidence closure.
- `plugin` means shared implementation tooling exists, but that alone does not
  prove product ownership.

## Domain-To-Domain Dependency Table

| Upstream | Downstream | Dependency type | Why it matters | Authority basis | Blocking state | Evidence / question |
| --- | --- | --- | --- | --- | --- | --- |
| `00` | `01-19` | design | Downstream domains inherit product-governance boundaries and release-stage framing. | `PRODUCT-GOV-001`, `PRODUCT-GOV-003` | active_governance | [00-decision-registry.yaml](00-decision-registry.yaml) |
| `01` | `02` | contract | Account and character systems need stable cross-domain contract boundaries. | existing contract work plus proposed Domain 01 scope | partial | `docs/contracts/account-character-contract.yaml` |
| `02` | `04` | contract | Class and build systems need durable character-state boundaries. | observed repo contracts | partial | `docs/contracts/character-scope-registry.yaml` |
| `02` | `11` | contract | Seasonal competitive state cannot claim durable-player-state behavior without explicit persistence boundary. | approved ownership plus open persistence question | open | `PRODUCT-QUESTION-004` |
| `03` | `04` | design | Class design must fit combat-core rules. | proposed domain map | non_blocking_proposed | [00-master-product-bible.md](00-master-product-bible.md) |
| `03` | `07` | contract | PvE encounter systems need shared combat semantics. | observed foundation and registry inputs | open | `docs/config_authority_registry.yaml` |
| `05` | `15` | design | Item visuals require item identity, rarity, material, and presentation constraints without transferring item-stat or economy authority. | canonical visual-asset handoff | static_contract | `docs/contracts/visual-asset-domain-handoff-contract.yaml` |
| `05` | `10` | contract | Economy consumes item semantics and reward provenance without taking ownership of item state. | `profession-domain-handoff-v1` plus existing item/economy contracts | accepted_contract_runtime_pending | `docs/contracts/profession-domain-handoff-contract.yaml` |
| `06` | `15` | design | Region, world context, availability, and progression constraints feed the visual brief while Domain 15 owns visual realization. | canonical visual-asset handoff | static_contract | `docs/contracts/visual-asset-domain-handoff-contract.yaml` |
| `06` | `08` | contract | World facts hand off into quest state and objective credit without Domain 06 owning quest progression. | `quest-domain-handoff-v1` | accepted_contract_runtime_pending | `docs/contracts/quest-domain-handoff-contract.yaml` |
| `07` | `15` | design | Combat role, encounter intent, telegraph requirements, and skill timing constrain the asset without making Domain 15 their gameplay authority. | canonical visual-asset handoff | static_contract | `docs/contracts/visual-asset-domain-handoff-contract.yaml` |
| `08` | `10` | contract | Quest reward entitlement hands off to economy policy without reassigning quest ownership. | `quest-domain-handoff-v1` | accepted_contract_runtime_pending | `docs/contracts/quest-domain-handoff-contract.yaml` |
| `09` | `05` | contract | Profession results hand off into item and custody semantics without Domain 09 minting item state directly. | `profession-domain-handoff-v1` | accepted_contract_runtime_pending | `docs/contracts/profession-domain-handoff-contract.yaml` |
| `09` | `10` | contract | Profession economic legs hand off to economy policy and Domain 01 transaction coordination. | `profession-domain-handoff-v1` | accepted_contract_runtime_pending | `docs/contracts/profession-domain-handoff-contract.yaml` |
| `09` | `11` | contract | PvP applies a contextual, non-mutating overlay to profession outputs without rewriting profession or item truth. | `profession-domain-handoff-v1` | accepted_contract_runtime_pending | `docs/contracts/profession-domain-handoff-contract.yaml` |
| `11` | `10` | contract | Season-end reward envelopes and economic payout semantics must align. | approved ownership plan | open | `PRODUCT-QUESTION-003` |
| `11` | `13` | release | Competitive-state outcomes, eligibility results, reward-envelope results, and reset-matrix outputs feed Domain 13 season lifecycle and season-program packaging policy. | approved ownership open persistence question | open | `PRODUCT-QUESTION-004` |
| `19` | `11` | contract | Guild season projections, roster snapshots, and season-close audit paths depend on explicit handoff. | approved ownership plan | open | `PRODUCT-QUESTION-003` |
| `19` | `10` | contract | Guild-material value semantics, guild treasury payouts, and guild-war reward valuation need explicit economy rows and payout contracts. | approved ownership plan | open | `PRODUCT-QUESTION-003` |
| `19` | `13` | release | Guild-facing season-program packaging, prestige/title/cosmetic presentation, and release messaging depend on Domain 13 once Domain 11 season outcomes are defined. | approved ownership plan | open | `PRODUCT-QUESTION-003` |
| `12` | `02` | runtime | Companion progression depends on durable player identity and runtime proof. | ADR and registry direction without runtime proof | blocked | `PRODUCT-QUESTION-005` |
| `12` | `15` | design | Pet and mount behavior, persistence, and eligibility constrain the visual brief while Domain 15 owns model authoring and delivery. | canonical visual-asset handoff | static_contract_runtime_blocked | `docs/contracts/visual-asset-domain-handoff-contract.yaml`, `PRODUCT-QUESTION-005` |
| `12` | `17` | release | Companion systems cannot support stronger release claims while runtime remains blocked. | explicit governance boundary | blocked | `PRODUCT-QUESTION-005` |
| `14` | `02` | contract | Onboarding UX depends on account and character semantics. | observed repo contracts | partial | `docs/contracts/account-character-model.md` |
| `14` | `15` | design | Icon, menu, readability, and presentation constraints enter the asset brief without transferring 3D authoring authority to Domain 14. | canonical visual-asset handoff | static_contract | `docs/contracts/visual-asset-domain-handoff-contract.yaml` |
| `15` | `18` | contract | Domain 18 needs a complete AssetSpec plus asset, style, rig, and delivery profiles before it may orchestrate a production job. | canonical visual-asset handoff | static_contract | `docs/contracts/visual-asset-domain-handoff-contract.yaml` |
| `15` | `17` | contract | Art and integration evidence must identify the exact Domain 15 source and render hashes before Domain 17 can evaluate a gate. | canonical visual-asset handoff | static_contract_runtime_pending | `docs/contracts/visual-asset-domain-handoff-contract.yaml` |
| `18` | `17` | release | Production-job receipts and hash-bound evidence feed Domain 17 gates; orchestration success cannot approve release. | canonical visual-asset handoff | static_contract_runtime_pending | `docs/contracts/visual-asset-domain-handoff-contract.yaml` |
| `17` | `00` | release | Domain 00 release statements depend on downstream verification evidence. | explicit governance and release framing | active_governance | [00-release-roadmap.md](00-release-roadmap.md) |

## Runtime Dependency Notes

- `12 -> 02` remains the primary runtime blocker in Domain 00.
- Shared plugin references in `docs/config_authority_registry.yaml` remain useful
  implementation signals, but they do not by themselves prove product
  ownership, contract ownership, or dependency direction.
- The visual-asset contract is a static routing boundary only. It does not
  unblock Domain 12 runtime, prove Blockbench MCP conformance, or establish an
  `ART_PASS`, `INTEGRATION_PASS`, or `RUNTIME_PASS` for any asset.
- Runtime evidence wins runtime claims when current behavior contradicts
  historical design or registry wording.

## Release Dependency Notes

- Product release framing in Domain `00` depends on Domain `17` verification
  gates, rollback paths, and evidence hygiene.
- Ownership is now explicit for 08, 09, 11, 13, and 19; remaining blockers are
  contract bundle closure, persistence boundaries, and blocked runtime truth.
- Release readiness must not be inferred from legacy documents that say a
  contract is `accepted` while runtime remains `partial` or `blocked`.

## Resolved Governance Contracts

| Question | Affected edges | Resolution |
| --- | --- | --- |
| `PRODUCT-QUESTION-001` | `06 -> 08`, `08 -> 10`, `08 -> 14` | Resolved by `PRODUCT-GOV-004` and `docs/contracts/quest-domain-handoff-contract.yaml`; runtime and release evidence remain downstream gates. |
| `PRODUCT-QUESTION-002` | `09 -> 05`, `09 -> 10`, `09 -> 11` | Resolved by `PRODUCT-GOV-005` and `docs/contracts/profession-domain-handoff-contract.yaml`; runtime and release evidence remain downstream gates. |

## Known Governance Blockers

| Blocker | Affected edges | Why blocked |
| --- | --- | --- |
| `PRODUCT-QUESTION-003` | `11 -> 10`, `19 -> 11`, `19 -> 10`, `19 -> 13` | Seasonal reward envelopes, guild projections, guild-facing season-program packaging, and guild treasury payout contracts still need closure. |
| `PRODUCT-QUESTION-004` | `02 -> 11`, `11 -> 13` | Seasonal persistence-policy framing remains with Domain 13, while durable-player-state boundaries and competitive reset semantics still need explicit closure. |
| `PRODUCT-QUESTION-005` | `12 -> 02`, `12 -> 17` | Companion direction exists, but runtime and release truth remain blocked. |

## Question Registry

See [00-open-questions.yaml](00-open-questions.yaml) for resolved contract
questions and the remaining blocking persistence, release, and runtime questions.
