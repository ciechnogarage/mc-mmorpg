# Current Server Plan

Status: current coordination plan, reviewed 2026-08-12.

This is the single starting point for deciding what to work on next in the
MC MMORPG repository. It does not replace runtime evidence, plugin
configuration, or domain contracts. Domain 00 product-governance
artifacts under [`docs/product/`](product/) remain the product-truth
surface. It turns the existing foundation library into an ordered, safe
delivery sequence.

## Source Of Truth

| Question | Authoritative source |
| --- | --- |
| What is running in staging? | `server_environment.md`, `compatibility_matrix.md`, current `MCMMORPG` logs/configuration, and the single canonical plugin inventory at `plugin_manifest.yaml`. |
| Who owns product semantics? | `product/00-domain-ownership.yaml` and approved entries in `product/00-decision-registry.yaml`. |
| Who implements a shared contract, and what blocks it? | `config_authority_registry.yaml` for observed/proposed implementation authority; `product/00-open-questions.yaml` and `product/00-conflict-registry.yaml` for unresolved handoffs, persistence, and release boundaries. |
| Which authority decisions or reward guarantees apply? | `adr/ADR-001-player-data-authority.md` and `contracts/reward-contract.yaml`; use `product/00-master-product-bible.md` and `product/00-decision-registry.yaml` for product-governance decisions only. |
| Which design decisions are still usable? | `foundation-index.md` and linked foundation documents as advisory input; `product/00-current-state-intake.yaml` and `product/00-conflict-registry.yaml` track whether imported claims stay quarantined or can inform Domain 00. |
| How is local MariaDB prepared? | `database-staging-baseline.md` and `MCMMORPG/docker/compose.yml`. |
| How is change validated? | Repository tests, the affected leased `dev-<scope>` runtime, current logs, and the authoritative validation channel for the claim. Staging is reserved for integration, promotion, and release validation. |

When these sources disagree, observed runtime evidence wins for runtime
claims. A foundation document is design input until its decision is copied
into a current domain contract and has implementation evidence. Domain 00
artifacts decide product-governance truth only; they do not override
runtime evidence or contract-specific authority on their own.
`docs/plugin_manifest.yaml` is the only current plugin inventory manifest;
`docs/technical/plugin-manifest.yaml` is a superseded 2026-07-22 review
snapshot and must not be used as current inventory truth.

## Delivery Order

### 0. Close P0 With Runtime Proof

The next work package is runtime proof, not another broad feature audit:

1. Keep the authority registry at `contract_status: accepted`,
   `runtime_status: partial`, `release_status: blocked` until the
   selected backend proves login, restart, reconnect, and data ownership.
2. Validate the adapter-first reward contract. Add a bridge plugin only if
   existing APIs cannot prove idempotency and duplicate rejection.
3. Run the controlled MMOProfiles persistence matrix before enabling or
   migrating additional plugin databases.
4. Classify ModelEngine binding/cache failures with the current validation
   flow and record findings in
   `docs/audits/2026-07-13/runtime-evidence-register.yaml`.

P0 documents are:

- `docs/adr/ADR-001-player-data-authority.md`
- `docs/contracts/reward-contract.yaml`
- `docs/tests/persistence-restart-matrix.md`
- `docs/tests/vertical-slice-e2e.md`
- `docs/audits/2026-07-13/modelengine-binding-remediation.md`
- `docs/audits/2026-07-13/runtime-evidence-register.yaml`

The initial Docker access failure tracked as `VAL-RUNTIME-001` has now
been resolved through host-side staging access. It was not evidence that
product runtime failed; actual runtime findings are recorded separately in
the evidence register.

### 1. Keep Repository And Staging Baseline Trustworthy

Before adding gameplay content, keep the source tree free of runtime data
and confirm the target stack still matches documented Paper, Java, and
plugin inventory. Run repository hygiene checks after changes to ignore
rules, docs, tracked configuration, or validation helpers.

Required baseline checks:

```bash
node scripts/repo-hygiene.mjs --audit
node --test tests/repo-hygiene.test.mjs tests/plugin-inventory.test.mjs tests/contract-registry.test.mjs
```

Do not call staging release-ready while documented persistence/runtime
blockers, quest and profession contract evidence, seasonal reward/payout
contracts, or persistence/reset boundaries remain unresolved.

### 2. Turn Draft Foundations Into Current Domain Contracts

The immediate product work is consolidation, not broad feature production.
For each domain, create one current contract that records owner, stable
IDs, persistence/reset rules, plugin adapters, consumers, validation
command, evidence path, and open blockers.

Consolidate in dependency order:

1. Player profile and character stage.
2. Combat formulas, class progression, item definitions, and inventory.
3. Currency, economy, loot, and reward provenance.
4. World/dungeon instances, mobs/bosses, and reward handoff.
5. Quests (Domain 08), professions (Domain 09), PvP competitive seasons
   (Domain 11), liveops/season policy (Domain 13), and social/guild systems
   (Domain 19), with their approved product owners preserved while the
   remaining handoff, persistence, payout, and reset boundaries are closed.

Do not implement a cross-domain feature while its technical implementation
authority in `config_authority_registry.yaml` is unresolved, its handoff is
split without a contract, or its required runtime evidence is missing. Quest
and profession handoffs are now accepted contracts, but their runtime services
and adapters remain blocked pending implementation and evidence.

### 3. Validate One Closed Gameplay Slice At A Time

After dependencies for a slice have current contracts, build and validate
one player-visible loop rather than configuring multiple systems in
parallel. Each slice must prove login/character state, progression, reward
persistence, and relevant world or UI behavior on its leased `dev-<scope>`
runtime before any staging integration gate.

For ModelEngine content, structural binding, quality, current multi-view
renders, and live spawn inspection are all required. YAML presence or a
successful reload alone is not acceptance evidence.

### 4. Expand Only With Evidence And Rollback

Promote a validated staging slice only after its affected backend has clean
enough logs, compatibility documentation refreshed, runtime evidence
stored, and rollback defined. Production changes additionally require a
backup and explicit approval.

## Current Blockers

- Product-semantic ownership is approved for quests (08), professions (09),
  PvP competitive state (11), season policy (13), and social/guild systems
  (19). Quest and profession technical authorities and handoffs are selected;
  their runtime services and adapters remain unimplemented and unverified.
- Quest and profession release claims require the runtime evidence named in
  `quest-domain-handoff-v1` and `profession-domain-handoff-v1`.
- Seasonal reward envelopes, guild projections/treasury payouts, and the
  02/11/13 persistence-reset boundary remain open.
- Several persistence targets are documented but not verified against a
  staging database.
- Combat formulas, rewards, dungeon resets, and model bindings require
  live evidence before they become implementation truth.
- HuskSync remains disabled, and MCPets is retired from active inventory by
  ADR-003; player-sync or PetCore-dependent release claims remain blocked
  until their selected runtime paths are implemented and evidenced.

Related Domain 00 trackers:

- `product/00-open-questions.yaml` for contract, persistence, and
  release-boundary blockers.
- `product/00-release-roadmap.md` for release-stage framing.
- `product/00-conflict-registry.yaml` when imported source wording
  over-asserts unresolved truth.

## Document Rules

- Add new design ideas to the relevant domain foundation or content
  proposal; do not treat them as current implementation requirements.
- Update this file only when delivery order, source-of-truth boundary, or
  blocker changes.
- Update `foundation-index.md` when disposition of a foundation document
  changes.
- Update the registry and add evidence when a domain contract becomes
  current.
- Archive or delete old material only after a reference scan and
  extraction of still-valid decisions.
