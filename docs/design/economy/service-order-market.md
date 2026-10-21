---
title: Domain 10 Service Order Market
status: proposed
updated_at: 2026-08-03
---

# Domain 10 Service Order Market

## Purpose

This document defines the market behavior for player-to-player services in
Domain 10.
It covers:

- `Fixed Price` contracts;
- `Request for Offers`;
- service tender versus customer-supplied operation fuel;
- provider reputation and capacity limits;
- acceptance, cancellation, expiry, refund, and dispute behavior;
- the economic side of service settlement.

It does not define the underlying crafting or modification minigame.
That remains with Domain 09.

## Source Of Truth

Canonical authority:

- `docs/design/economy/service-order-market.md`
- `docs/design/economy/economy-model.md`
- `docs/design/economy/binding-policy.md`
- `docs/design/economy/escrow-contract.md`
- `docs/design/economy/market-eligibility-matrix.yaml`
- `docs/design/economy/currency-registry.yaml`
- `docs/design/economy/sources-and-sinks.yaml`

Upstream dependencies:

- Domain 09 crafting and service-process contracts
- Domain 05 item and binding-state contracts
- Domain 02 identity and custody contracts

## Domain Boundary

Domain 09 owns:

- what operation is being performed;
- which materials and fuel are technically valid;
- what success or failure outcomes exist;
- whether the resulting output is bound or transferable;
- the state machine of the actual craft or modification process.

Domain 10 owns:

- who pays;
- who supplies inputs;
- when funds and assets enter escrow;
- what fees are charged;
- who receives payment;
- when cancellation is allowed;
- how tender, fuel, materials, and outputs settle;
- when dispute or manual review is justified.

## Governing Intent

The service market should allow specialization and trust-based commerce without
creating:

- invisible scam surfaces;
- ambiguous custody;
- refund logic that overrides crafting rules;
- unlimited provider queue abuse;
- easy laundering of progression-locked outputs into open trade.

The service market is meant to turn player skill, access, and specialization
into a real economy role.
It is not a substitute for direct player market trading.

## Service Contract Types

### `Fixed Price`

Provider publishes a defined service with a fixed quoted `gold` price and, if
needed, named input requirements.

Use when:

- the provider already knows the price;
- the service is repeatable enough to publish openly;
- the provider wants faster conversion and less negotiation overhead.

### `Request for Offers`

Customer publishes a request and providers respond with offers.

Use when:

- the value of the job depends on context or rarity;
- the customer wants competing offers;
- the service is too situational for a static listing.

## Core Economic Concepts

### Service tender

Service tender is what pays the provider.

In v1:

- service tender is `gold`;
- premium balance is forbidden;
- itemized currency is not standard service tender.

### Operation fuel

Operation fuel is not the provider's payment.
It is customer-supplied process input consumed by the underlying service.

Typical operation fuel:

- reroll currency items;
- upgrade currency items;
- craft modifier currency items.

This distinction must remain visible in the contract:

- provider earns `gold`;
- customer may also supply materials and itemized fuel;
- fuel may be consumed by the process without ever becoming the provider's
  wage.

## Market Roles

### Customer

The player requesting the service.

### Provider

The player offering to perform the service.

### Platform

The Domain 10 market and escrow layer that:

- reserves custody;
- charges fees;
- validates contract shape;
- executes settlement outcomes;
- routes ambiguous cases to `manual_review`.

## Contract Shape

Every service listing or request must at minimum declare:

```yaml
service_order_id:
service_type:
contract_mode: FIXED_PRICE | REQUEST_FOR_OFFERS
provider_character_id:
customer_character_id:
quoted_tender_currency: gold
quoted_tender_amount:
required_material_categories: []
required_operation_fuel_categories: []
expected_input_asset_categories: []
expected_output_policy:
  transferability:
  binding_outcome_mode:
status:
created_at:
expires_at:
```

Notes:

- `binding_outcome_mode` does not define crafting outcome.
  It defines which transfer classes are legal after Domain 09 finishes.
- `required_operation_fuel_categories` must point to category rows from
  `market-eligibility-matrix.yaml`, not to vague free-text.

## Allowed Input Classes

Service contracts may accept only categories that are both:

- resolved to an exact row with `service_input: allowed` in
  `docs/design/economy/market-eligibility-matrix.yaml`;
- admitted by the matching escrow surface in
  `docs/design/economy/escrow-contract.md`.

The validator rejects missing or unknown categories, `service_input` values
other than `allowed`, and any input whose escrow value is `forbidden` or whose
binding/custody state is unknown. Validation occurs before the contract enters
escrow.

The current service-input rows are exactly:

- `gold` as tender;
- `ordinary_materials`;
- `progression_bottleneck_materials` (input-only escrow handling);
- `reroll_currency_items`;
- `upgrade_currency_items`;
- `craft_modifier_currency_items`;
- `market_gear_bases`;
- `intermediate_crafted_outputs`;
- `final_progression_gear` (customer-owned input only; never a free-market
  deliverable).
- `service_input_items` (owning-contract input only).

Forbidden as service inputs:

- premium balance;
- categories marked `service_input: forbidden`;
- assets with ambiguous identity or unresolved compensation state.

## Output Classes

Service contracts may end with outputs that are:

1. still marketable;
2. `account_bound`;
3. `character_bound`;
4. returned unchanged because the service failed or was cancelled.

Service contracts may not end with:

- unknown binding state;
- contradictory ownership;
- both original input and transformed output released as success;
- unrestricted market release of categories that the binding matrix forbids.

## Fixed Price Behavior

### Publication

Provider creates a listing that names:

- quoted `gold` price;
- required customer inputs;
- service category;
- expiry window;
- optional provider capacity consumption.

No full customer tender is reserved at publication time.

### Acceptance

Customer accepts the listing.

At acceptance:

- `gold` tender enters escrow;
- required materials and operation fuel enter escrow if the service requires
  them;
- required service-input items enter escrow if needed;
- the service order becomes active;
- the provider consumes one active service slot.

### Completion

On successful completion:

- provider receives `gold` tender minus service fee if applicable;
- platform keeps the `service_fee`;
- customer receives the final output allowed by Domain 09 plus binding policy;
- consumed fuel or materials do not return unless the process contract says
  otherwise.

## Request for Offers Behavior

### Publication

Customer creates a request that names:

- what service is needed;
- what inputs the customer can provide;
- expiry window;
- optional budget guidance.

Approved v1 rule:

- publication does not reserve the full `gold` tender;
- a small anti-spam deposit may exist later, but is not required by this
  document;
- multiple providers may respond while the request is open.

### Offer submission

Providers may submit offers that include:

- `gold` price;
- availability or completion expectation;
- optional notes about required customer readiness.

### Offer acceptance

When the customer chooses one provider:

- `gold` tender enters escrow;
- required materials, fuel, and service-input items enter escrow;
- all other offers become closed without settlement;
- the chosen provider consumes one active service slot.

## Provider Capacity And Active Orders

Approved v1 policy allows providers to hold multiple active jobs, but not
unboundedly.

Provider capacity must be limited by at least:

- active order slot count;
- total active escrow value;
- optional per-service-type cap.

Recommended baseline policy:

```yaml
provider_capacity:
  active_order_slots:
    base: 2
    can_scale_with_reputation: true
  active_escrow_value_cap:
    required: true
  per_service_type_cap:
    enabled: true
```

Reason:

- one job at a time is too restrictive for a healthy service economy;
- unlimited parallel jobs create ghost-order and backlog abuse.

## Reputation Model

Reputation is not purely social flair.
It is an economic trust signal.

At minimum the system should track:

- completed service count;
- cancelled-after-accept count;
- timeout or expiry count;
- dispute or `manual_review` frequency;
- provider-side fault count;
- customer-side fault count.

Recommended reputation effects:

- higher reputation may raise active slot capacity;
- repeated provider-side failures may reduce visibility or slot capacity;
- repeated customer-side no-fund or no-input failures may restrict request
  publication.

Reputation should change only from durable service outcomes, not from arbitrary
chat reports.

## Cancellation Policy

### Before acceptance

Allowed:

- provider may remove a fixed-price listing;
- customer may remove an `RFO`;
- no escrow refund complexity exists because no full tender is reserved yet.

### After acceptance, before irreversible process commit

Allowed only if:

- the underlying process has not crossed its irreversible point;
- escrow can safely return tender, fuel, materials, and service-input custody.

Outcome:

- service moves through escrow cancellation or rollback path;
- provider slot is released;
- service fee is not charged unless a later narrow rule explicitly says so.

### After irreversible process commit

Ordinary cancellation is no longer allowed.

From that point, the flow must resolve through:

- `committed`;
- `rolled_back` if the process contract allows a safe rollback;
- `manual_review` when custody is ambiguous.

## Expiry Policy

### Listing or request expiry before acceptance

Allowed and expected.

Outcome:

- listing or request closes;
- no full tender refund problem exists because full tender was not reserved.

### Active accepted order expiry

An accepted order may expire only through explicit timeout semantics.

If expiry occurs after reservation:

- escrow must unwind tender and asset custody or escalate to `manual_review`;
- provider slot is released only after durable resolution state is reached.

## Fee Model

The service market uses `gold` fees only.

Supported fee classes:

- `service_fee`
- optional publication deposit for `RFO` if later enabled

Rules:

- fixed-price publication itself does not charge the normal market listing fee
  unless a later marketplace integration explicitly adds one;
- `service_fee` is charged at successful settlement;
- cancelled or expired unstarted service orders do not charge final
  service-settlement fees;
- any anti-spam deposit must be modeled as a separate fee leg, not hidden inside
  quoted provider payment.

## Dispute Policy

The service market may dispute economic correctness, not the legitimacy of a
valid poor roll.

### Refund is valid when

- service was not executed according to the contract;
- required inputs were mishandled;
- funds or assets were reserved but no valid settlement occurred;
- system or custody state became contradictory.

### Refund is not valid when

- the process was valid and RNG produced an unfavorable but legitimate result;
- the customer simply dislikes the valid outcome;
- a bound output was legitimately produced and the contract already warned that
  the output would bind.

### Manual review is valid when

- output ownership cannot be reconstructed safely;
- provider and customer records contradict escrow state;
- operation fuel or materials were partially consumed but final state is unclear;
- duplicate acceptance or duplicate payout risk exists.

## User-Visible State Model

Every service order should be intelligible to both sides through a small number
of visible states:

```yaml
service_order_visible_states:
  - open
  - offers_open
  - awaiting_customer_inputs
  - in_escrow
  - in_progress
  - completed
  - cancelled
  - expired
  - disputed
  - manual_review
```

These are not the same as low-level escrow states.
They are the player-facing compression of that lower-level lifecycle.

## Anti-Abuse Rules

The service market must prevent:

- providers accepting more work than their capacity rules permit;
- customers publishing requests they cannot realistically fund forever after
  acceptance;
- laundering progression-locked outputs into open market categories;
- hidden substitution of customer-supplied fuel into provider payment logic;
- duplicate resolution of the same accepted service intent;
- silent timeout states where both sides believe the other still holds value.

## Marketplace Boundary

This document defines the service market only.

It does not define:

- general commodity listing UI;
- direct trade UX;
- non-service item marketplace categories;
- broad AH discovery rules outside service orders.

Those belong in `docs/design/economy/marketplace.md`.

## Non-Goals

- Exact crafting or modification process steps.
- Runtime plugin implementation details.
- Final numerical fee percentages.
- Final reputation formulas.
- General Auction House behavior for non-service goods.
