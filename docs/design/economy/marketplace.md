---
title: Domain 10 Marketplace
status: proposed
updated_at: 2026-08-03
---

# Domain 10 Marketplace

## Purpose

This document defines the general player marketplace for Domain 10.
It covers:

- Auction House behavior for non-service goods;
- direct trade behavior outside service contracts;
- listing, purchase, cancellation, expiry, and return rules;
- market access by asset category;
- fee timing and settlement behavior;
- stack-lot handling for itemized currency and stackable goods.

This document does not redefine:

- service-specific contracting, which lives in
  `docs/design/economy/service-order-market.md`;
- low-level escrow state mechanics, which live in
  `docs/design/economy/escrow-contract.md`;
- item-side binding semantics, which remain upstream of Domain 10.

## Source Of Truth

Canonical authority:

- `docs/design/economy/marketplace.md`
- `docs/design/economy/economy-model.md`
- `docs/design/economy/market-eligibility-matrix.yaml`
- `docs/design/economy/sources-and-sinks.yaml`
- `docs/design/economy/escrow-contract.md`
- `docs/design/economy/binding-policy.md`
- `docs/design/economy/currency-registry.yaml`
- `docs/contracts/stack-lot-contract.yaml`

Upstream dependencies:

- Domain 02 identity and custody contracts
- Domain 05 item identity and binding-state contracts
- Domain 01 transaction and idempotency shell contracts

## Marketplace Scope

The marketplace covers two user-facing exchange modes:

### Auction House

Global listing and price-discovery surface for market-eligible goods.

### Direct Trade

Player-to-player exchange path for categories that are tradeable but do not
need to be globally listed.

Marketplace scope includes:

- `gold`-denominated buying and selling;
- tradable materials;
- tradable market gear;
- tradable itemized currency stack lots;
- category-based listing restrictions;
- economic fees and taxes.

Marketplace scope excludes:

- premium balance trading;
- service-order negotiation and provider reputation rules;
- non-durable barter with ambiguous identity;
- categories marked forbidden in the eligibility matrix.

## Governing Intent

The marketplace should:

- make useful goods liquid;
- make prices discoverable;
- preserve player trust in settlement;
- keep progression bottlenecks from becoming easy monopoly surfaces;
- avoid hidden minting or loss during stack splits, partial fills, expiry, or
  retry.

It should not:

- become a backdoor around binding policy;
- become a laundering path for service-locked or progression-locked outputs;
- convert premium value into ordinary economy tender.

## Core Marketplace Model

### Market tender

In v1, standard marketplace tender is `gold`.

Rules:

- ordinary listings price in `gold`;
- ordinary purchases settle in `gold`;
- taxes and listing fees charge in `gold`;
- premium balance is forbidden as normal marketplace tender;
- itemized currency may be listed and sold, but is not itself the required
  default tender for ordinary goods.

### Market goods

A good may enter the marketplace only if:

1. its category resolves to an exact row in
   `market-eligibility-matrix.yaml`;
2. the row says `auction_house: allowed` for an AH listing or
   `direct_trade: allowed` for direct trade;
3. its binding state allows that mode of trade;
4. its custody is not ambiguous;
5. its identity is durable enough for escrow and settlement.

Missing or unknown categories, binding states, or eligibility values are
rejected before custody reservation. No UI, lore, or plugin default may create
an exception.

## Category Access Rules

The exact category authority lives in
`docs/design/economy/market-eligibility-matrix.yaml`.

Marketplace must consume that matrix directly.
It must not invent category exceptions ad hoc.

### Auction House admission

An AH listing is admitted only when the resolved matrix row has
`auction_house: allowed`, the asset is not `service_locked`, and custody and
identity checks pass. The current admitted rows are:

- `ordinary_materials`
- `reroll_currency_items`
- `upgrade_currency_items`
- `craft_modifier_currency_items`
- `market_gear_bases`
- `intermediate_crafted_outputs`

Every other current row is rejected by this surface, including:

- `progression_bottleneck_materials`
- `final_progression_gear`
- `service_input_items`
- `service_bound_outputs`
- `recovery_or_compensation_items`
- premium balance and any unknown category

### Direct trade admission

Direct trade is admitted only when the resolved matrix row has
`direct_trade: allowed`, the asset is not account-bound, character-bound, or
service-locked, and custody and identity checks pass. `contract_only` and
`forbidden` are rejected. Direct trade is not broader than the matrix; an
unknown category is rejected.

## Auction House Behavior

### Listing creation

Creating a listing requires:

- seller identity;
- durably identified listed asset or stack lot;
- quantity when stackable;
- `gold` asking price;
- listing duration;
- category validation against the eligibility matrix;
- immediate payment of `listing_fee`.

At listing creation:

- the listing fee is charged upfront;
- the listed asset enters escrow custody;
- the asset becomes unavailable for other trade or service flows;
- the marketplace creates a durable listing-linked escrow record.

### Listing duration

Every listing requires an explicit expiry time.

If the listing reaches expiry without sale:

- listed asset or residual stack lot returns to the seller;
- already charged listing fee is not automatically refunded;
- optional extra expiry penalty remains `reserved_not_enabled` unless a later
  accepted policy enables it.

### Listing cancellation

Seller may cancel only while the listing has not committed to a sale.

On cancellation:

- the listed asset or residual stack lot returns to seller;
- listing fee remains spent;
- no transaction tax is charged because no sale committed;
- optional extra cancellation penalty remains `reserved_not_enabled` unless a
  later policy enables it.

### Purchase

A purchase succeeds only when:

- the buyer has sufficient `gold`;
- the listing is still active and unclaimed;
- the listed asset revision or lot identity still matches the reserved state;
- the purchase wins the concurrency race for that listing.

On successful purchase:

- buyer `gold` enters settlement;
- `transaction_tax` is deducted;
- seller receives net `gold`;
- buyer receives the purchased asset or purchased stack-lot quantity;
- any residual stack-lot quantity remains with the listing seller as exact
  remainder custody if the sale was partial.

## Direct Trade Behavior

Direct trade is allowed only for categories whose matrix row says
`direct_trade: allowed`; all other values are rejected before confirmation.

Direct trade must still obey:

- binding restrictions;
- custody restrictions;
- identity durability;
- retry-safe settlement.

Direct trade may be used for:

- eligible materials;
- eligible market gear;
- eligible itemized currency lots.

Direct trade may not be used for:

- premium balance;
- `account_bound` or `character_bound` assets moving to another player;
- `service_locked` assets leaving an active contract;
- compensation or recovery goods.

## Stack-Lot Handling

Stack-lot behavior is required for:

- itemized currency;
- stackable materials;
- any later stackable market good that uses quantity-based partial sales.

### Listing a stack lot

Listing must record:

- source stack-lot id;
- listed quantity;
- residual quantity if the full lot is not listed;
- owner account;
- expected revision or equivalent concurrency fence.
- listing must use the canonical `stack_lot_id`; `item_definition_id`, lore, or
  an opaque stack amount cannot identify custody.

### Partial fills

If a stackable listing sells partially:

- the sold quantity moves to the buyer through a new or updated destination lot;
- the unsold quantity remains durably linked to a remainder lot;
- sold and unsold quantities each receive lineage events referencing the source
  lot and listing transaction;
- telemetry must be able to prove no quantity was minted or lost.

### Unsold return

On cancellation or expiry:

- the exact residual quantity returns to seller custody;
- return uses exact lot accounting, not best-effort equivalent quantity.
- return is idempotent by transaction id and must preserve parent-lot lineage.

## Fee And Tax Timing

Marketplace uses the timing already defined in
`docs/design/economy/sources-and-sinks.yaml`.

### `listing_fee`

- charged at listing creation;
- not delayed until sale settlement;
- not automatically refunded on cancel or expiry.

### `transaction_tax`

- charged only on successful sale settlement;
- deducted atomically with purchase commit.

### Optional cancel/expiry penalties

- currently `reserved_not_enabled`;
- must not be silently introduced by UI or config alone;
- require an explicit accepted policy before activation.

## Escrow Integration

Marketplace relies on `docs/design/economy/escrow-contract.md` for settlement.

### Auction House requires escrow for

- active listings;
- in-flight purchases;
- stack-lot partial-fill accounting;
- cancellation and expiry return paths.

### Direct trade may use escrow when

- the trade involves reserved custody before confirmation;
- the system needs durable two-sided commit rather than immediate handoff.

Marketplace must not bypass escrow where custody ambiguity exists.

## User-Visible State Model

Marketplace flows should compress low-level settlement into understandable
player states:

```yaml
auction_house_visible_states:
  - draft
  - listed
  - sold
  - cancelled
  - expired
  - settlement_pending
  - manual_review

direct_trade_visible_states:
  - draft
  - awaiting_confirmation
  - settled
  - cancelled
  - failed
  - manual_review
```

These are player-facing states, not the full escrow state machine.

## Failure And Recovery Rules

Marketplace must remain safe under:

- duplicate purchase requests;
- retry after timeout;
- restart during active listing settlement;
- partial-fill interruption;
- listing expiry races;
- stale revision or stale lot ownership.

Recovery rules:

- no double payout to seller;
- no double delivery to buyer;
- no hidden loss of unsold remainder quantity;
- unresolved ambiguity escalates to `manual_review`;
- `manual_review` is not terminal and must resolve through the escrow contract.

## Telemetry And Abuse Signals

Marketplace should explicitly monitor:

- listing creation volume by category;
- cancellation and expiry rate by category;
- completed sale rate by category;
- partial-fill frequency for stack-lot goods;
- unsold-return mismatches;
- duplicate purchase rejection rate;
- manual-review rate;
- price spikes for category-protected goods;
- concentrated seller dominance in a category;
- itemized-currency volume and turnover.

## Anti-Abuse Rules

Marketplace must prevent:

- listing forbidden categories through UI-only loopholes;
- relisting a service-locked or already-reserved asset;
- silent conversion of premium value into ordinary market tender;
- quantity drift across split, merge, sale, cancel, or expiry flows;
- speculative cornering of protected progression categories through
  misclassification;
- retry-driven double settlement.

## Boundary With Service Market

This document governs goods trading.

`docs/design/economy/service-order-market.md` governs:

- provider-customer contracting;
- `Fixed Price` and `RFO`;
- service reputation;
- service capacity;
- tender versus operation-fuel behavior for player-performed work.

Marketplace must not duplicate those service-specific rules.

## Non-Goals

- General crafting process rules.
- Provider reputation formulas.
- Final UI layout.
- Final fee percentages.
- Plugin-specific implementation details.
