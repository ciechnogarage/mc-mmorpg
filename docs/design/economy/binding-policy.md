---
title: Domain 10 Binding Policy
status: proposed
updated_at: 2026-08-03
---

# Domain 10 Binding Policy

## Purpose

This document defines the economy-facing binding policy for Domain 10.
It determines how binding affects:

- direct trade;
- Auction House listing;
- escrow eligibility;
- account versus character custody;
- service settlement;
- speculative market behavior;
- progression protection.

This is a policy document, not runtime proof and not the final item-state
schema.

## Source Of Truth

Canonical authority:

- `docs/design/economy/binding-policy.md` for governing binding rules.
- `docs/design/economy/economy-model.md` for the economy model.
- `docs/design/economy/currency-registry.yaml` for currency class behavior.
- `docs/design/economy/sources-and-sinks.yaml` for allowed value-flow
  assumptions.
- `docs/design/economy/market-eligibility-matrix.yaml` for exact category-level
  market, trade, and escrow eligibility.

Upstream dependencies:

- `docs/design/items/itemization-framework.md`
- Domain 05 item-state and binding-state contract work
- Domain 09 crafting and service-order process contracts
- Domain 02 account and character custody contracts

## Authority Split

Domain 10 owns:

- whether a binding state may enter the market;
- whether a bound state may enter escrow;
- whether a result may transfer after a service;
- whether a category may be speculative;
- how binding protects progression and economy stability.

Domain 05 owns:

- the semantic `binding state` carried by the item instance;
- item-side durable representation of that state;
- item-instance invariants and mutation semantics.

Domain 09 owns:

- whether a crafting or service process creates a bound output;
- which process states may produce transfer-restricted outputs;
- process-side rules for service order completion.

Binding policy consumes upstream state.
It must not invent hidden item semantics outside those domains.

## Governing Intent

Binding is an economy-control lever.
It exists to solve several problems at once:

- protect progression from full market capture;
- stop every valuable output from becoming a pure commodity stock;
- preserve chase value and identity of earned gear;
- allow healthy trade in materials and economy instruments;
- prevent service systems from laundering restricted outputs into the market.

The goal is controlled liquidity, not maximal liquidity.

## Economy Design Principles

1. Ordinary trade goods should usually stay liquid.
2. Progression-critical outputs should often be partially or fully protected.
3. High-end crafting and modification may consume tradeable inputs and still
   produce transfer-restricted outputs.
4. Escrow must only hold assets whose binding state makes post-settlement
   ownership unambiguous.
5. Binding should regulate marketability before the design reaches for
   drop-rate or crafting-odds distortion.

## Binding Outcome Axes

Every economy-facing asset class must be reasoned across these axes:

- equip or use scope;
- direct-trade eligibility;
- Auction House eligibility;
- escrow eligibility;
- service-input eligibility;
- service-output transferability;
- storage scope;
- speculation risk tier.

These are separate decisions.
An asset may, for example:

- be AH-forbidden;
- still be allowed as a service input;
- still be usable by the owning character;
- still be movable inside narrow account custody.

## Approved Binding States For Economy Policy

This document defines economy-facing binding states.
Domain 05 may later map them to exact runtime fields or enums.

### `unbound`

Meaning:

- free market candidate;
- no ownership lock beyond ordinary custody rules.

Default economy effect:

- direct trade allowed;
- Auction House allowed only when the category row says
  `auction_house: allowed`;
- escrow allowed only when the category row says `escrow: allowed`;
- speculation allowed only when the category row says `speculation: allowed`.

### `account_bound`

Meaning:

- locked from the open player market;
- usable across the owning account only where Domain 02 custody rules permit.

Default economy effect:

- no direct player trade;
- no Auction House;
- no third-party escrow payout transfer;
- account custody allowed only where the category explicitly supports it.

### `character_bound`

Meaning:

- locked to one character identity for use and ownership.

Default economy effect:

- no direct player trade;
- no Auction House;
- no third-party escrow settlement as a deliverable output;
- not eligible for speculative circulation.

### `service_locked`

Meaning:

- temporarily fenced to a service or market contract flow;
- not freely marketable while the owning contract is active.

Default economy effect:

- no general direct trade during active lock;
- no Auction House during active lock;
- escrow allowed only inside the owning contract;
- release path must end in returned ownership, transformed output, or explicit
  failure handling.

### `non_tradeable_economy_item`

Meaning:

- economy-facing asset that can exist and be consumed but cannot circulate
  through player trade.

Default economy effect:

- no direct trade;
- no Auction House;
- no third-party escrow unless a later contract names a narrow exception.

## Category-Level Eligibility Authority

Exact category-level decisions for:

- direct trade;
- Auction House listing;
- escrow;
- service-input eligibility;
- service-output transferability;
- speculation

live in:

- `docs/design/economy/market-eligibility-matrix.yaml`

This file remains the governing rulebook.
The matrix is the exact category table that downstream docs must consume.
Unknown categories, binding states, or eligibility values fail closed on every
surface. No downstream document may infer permission from a default binding
state, UI marker, lore, or an unlisted category.

## Currency-Specific Binding Policy

### `gold`

`gold` is not a tradeable item.
It is account-wide balance money.

Rules:

- may be reserved in escrow;
- may settle Auction House and service contracts;
- may not be listed as a market item;
- may not be converted directly from premium balance.

### Premium balance

Premium balance is outside the standard player economy.

Rules:

- non-tradeable;
- non-listable;
- not escrow-eligible for ordinary player services;
- not valid tender for ordinary goods or standard economy taxes.

### Itemized currency

PoE-like currency items are economically special:

- they are tradeable;
- they are consumed by item operations;
- they may be escrowed when a later service or operation contract explicitly
  supports them;
- they do not become universal settlement tender.

Default policy:

- `unbound` while circulating as market goods;
- may enter direct trade and AH;
- may enter escrow as supported service input fuel;
- may be consumed into outputs that become `character_bound` or
  `account_bound`.

Tradeable input does not guarantee tradeable output.

## Materials Policy

### Ordinary materials

Default category:

- `ordinary_materials` in `market-eligibility-matrix.yaml`

### Progression bottleneck materials

Default category:

- `progression_bottleneck_materials` in
  `market-eligibility-matrix.yaml`

Binding is an approved way to stop core progression from becoming pure market
rent extraction.

## Gear And Crafted Output Policy

### Market gear

Tradable gear is limited to categories meant to circulate, especially:

- `market_gear_bases`
- `intermediate_crafted_outputs`

### Progression-protected gear

Higher-risk outputs should resolve toward protected categories such as:

- `final_progression_gear`
- `service_bound_outputs`

Typical triggers:

- output represents major personalized progression;
- output consumed rare itemized currency and the game should not permit
  infinite resale chaining;
- output is too central to progression to stay a pure speculative good;
- output results from a service where free resale would undermine the intended
  progression loop.

### Modified-item rule

The model explicitly allows:

- unbound input item;
- tradeable itemized-currency input;
- gold operation cost;
- bound output item.

This is one of the main ways the economy converts open-market resources into
progression-locked outcomes.

## Service Order Policy

### Allowed service-input classes

A service contract may accept as inputs only assets that are:

- unambiguously identifiable;
- escrow-safe;
- recoverable under restart or rollback rules;
- legally transferable into and out of service custody.

For economy policy, this means:

- `gold` yes as tender;
- category rows marked `service_input: allowed` yes;
- premium balance no.

### Service output rules

Service outputs must declare one of these outcomes:

1. Returned as still marketable.
2. Returned as `account_bound`.
3. Returned as `character_bound`.
4. Returned as original input because the service failed or rolled back.

Forbidden ambiguity:

- a service output whose binding state is unknown at settlement time;
- a service that can accidentally pay out both transformed output and original
  input;
- a service that can move a bound output into unrestricted market circulation
  without explicit policy.

### Service lock behavior

While a contract is active:

- inputs become `service_locked`;
- they cannot be relisted, retraded, or re-escrowed elsewhere;
- ownership outcome must resolve atomically with contract settlement.

## Auction House Policy

An asset may enter Auction House only if all are true:

- its category resolves to an exact matrix row whose `auction_house` value is
  `allowed`;
- its binding state permits market listing;
- it is not `service_locked`;
- its custody state is not ambiguous.

AH-forbidden by default:

- premium balance;
- `character_bound` outputs;
- active service inputs;
- unknown or unresolved compensation assets;
- categories whose matrix row forbids AH.

## Direct Trade Policy

Direct trade can be broader than AH, but not broader than binding.

This means:

- some assets may be direct-trade-allowed while AH-forbidden;
- no `character_bound` or `account_bound` asset may be direct-traded to another
  player;
- `service_locked` assets may not leave their active contract by side trade;
- itemized currency may be direct-traded if the matrix and registry allow it.

## Escrow Eligibility Policy

An asset is escrow-eligible only if:

- its identity is durable;
- the binding state allows custody transfer into the contract;
- the release path is deterministic enough to avoid ownership ambiguity;
- rollback behavior is defined;
- its category row in `market-eligibility-matrix.yaml` allows escrow.

Escrow-safe by policy:

- `gold`;
- categories explicitly marked escrow-eligible;
- service-input-only categories as inputs, never as unrestricted deliverables.

Not escrow-safe by default:

- premium balance;
- unknown-state outputs;
- assets with unresolved binding transitions;
- assets whose rollback would require inventing ownership.

## Speculation Policy

Binding is a core speculation-control tool.

Allowed speculative surface should mostly live in:

- ordinary trade goods;
- some commodity materials;
- tradeable itemized currency;
- selected market gear.

Restricted or forbidden speculative surface should include:

- core progression bottlenecks;
- final personal progression gear;
- service-locked assets;
- outputs intended to stay attached to earned progression.

If a category is causing cornering or progression denial, binding and market
eligibility are preferred before distorting success rates or raw item power.

## Storage And Custody Interaction

Binding does not automatically imply storage scope.

Approved custody assumptions:

- currencies stay account-wide as balances;
- most gear stays character-scoped;
- material bank is shared only for approved material categories;
- `account_bound` does not automatically mean globally account-bankable forever
  unless Domain 02 custody and the category both allow that path.

## Anti-Abuse Rules

Binding policy must prevent:

- laundering a progression-locked output through a service contract into free
  market circulation;
- turning a service handoff into duplicate ownership on rollback;
- using account custody to bypass intended character-bound outcomes;
- using compensation or recovery items as hidden market goods;
- silently changing marketability through lore or UI-only markers.

## Required Follow-Up Contracts

This policy depends on:

- `docs/design/economy/escrow-contract.md`
- `docs/design/economy/market-eligibility-matrix.yaml`
- `docs/design/economy/service-order-market.md`
- Domain 05 item-state and binding-state scaffolds
- Domain 09 service-order and crafting-output contracts

Those files must not reopen these high-level rules:

- tradeable input can produce bound output;
- premium is outside ordinary market tender;
- service lock blocks free circulation during active contracts;
- progression protection may override pure market liquidity.

## Non-Goals

- Exact item-by-item binding tables.
- Runtime plugin field names.
- Exact crafting recipes or success chances.
- The final service-order dispute process.
