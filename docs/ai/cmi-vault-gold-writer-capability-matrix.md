# CMI/Vault gold writer capability matrix

Status: pre-cutover inventory, configuration and source inspection only

Snapshot: 2026-08-12

Target contract: `docs/contracts/central-account-wallet-contract.yaml` v2

## Scope and confidence

This matrix inventories every gold mutation surface evidenced by the checked-in
CMI/Vault configuration and first-party source. It also records configured but
dormant surfaces, ambiguous command routes, and read-only Vault consumers so
that they are not mistaken for writers during cutover.

No server was started and no configuration, plugin data, database, player data,
or permission grants were changed or inspected. Vendor plugin JARs declared in
`docs/plugin_manifest.yaml` are not present in this worktree. The only plugin
JARs present are locally built first-party runtime adapters and their MariaDB
driver. Consequently:

- `ACTIVE-CONFIGURED` means the local config enables the capability; it does not
  prove that a player currently has permission or that the exact vendor binary
  executed it.
- `DORMANT-CONFIGURED` means a mutation path is configured but its local feature
  flag, amount, or all schedules prevent a current write.
- `SOURCE-CONFIRMED-DISABLED` means first-party source contains a writer but its
  checked-in runtime switch is off.
- `POTENTIAL-UNVERIFIED` means local config exposes a money-shaped capability,
  but the missing vendor JAR or command map prevents proof that it reaches the
  selected Vault gold provider.
- `READ-ONLY/NOT-GOLD` means the surface must be retained in the inventory but
  is not a current gold writer on the checked-in configuration.

The cutover contract requires every generic Vault write to fail with
`WRITE_CONTEXT_REQUIRED`, and every first-party mutation to use the authenticated
`WalletMutationPort`. Therefore none of the legacy paths below may remain an
unfenced writer in `CENTRAL_ACTIVE`.

Declared inventory versions used to name the scan scope are CMI `9.8.4.14`,
CMILib `1.5.9.5`, Vault `1.7.4`, CoreTools `1.4.1`, DeluxeMenus
`1.14.1-Release`, MMOCore `1.13.1-20260330.110342-58`, MMOItems
`6.10.1-20260330.094756-58`, MythicDungeons `2.0.1-SNAPSHOT`, and
Multiverse-Core `5.5.3`. These are manifest declarations, not JAR hashes or
fresh boot observations.

## Backend/provider topology before cutover

| Backend | Assembled economy-related plugins | Locally intended provider state | Cutover observation |
| --- | --- | --- | --- |
| `items` | Vault, CMI, CoreTools, MMOCore, MMOItems, DeluxeMenus, ProfessionRuntimeAdapter | CMI economy enabled; `Vault.Money: true`; CoreTools `economy.vault: false` | CMI is the intended legacy Vault gold provider. This is not a shared-wallet proof. |
| `hub` | Vault, CMI | CMI economy enabled; full CMI data folder is assembled | A second CMI provider instance can mutate backend-local legacy state. |
| `world` | Vault, CoreTools, MythicDungeons, Multiverse-Core | no CMI; CoreTools economy enabled but `economy.vault: false` | No selected gold provider is proven from static config. Vault consumers can be present without a usable gold provider. |

Evidence: `MCMMORPG/docker/build-servers.sh:13-22,62-66,103-109`,
`MCMMORPG/plugins/CMI/config.yml:10-20,582-585`, and
`MCMMORPG/plugins/CoreTools/economy.yml:1-10`.

The assembler copies the complete checked-in CMI data folder only to `hub`.
For `items` it copies CMI's economy-enabled `config.yml` and lets the vendor
plugin create any missing `Settings/` and `Kits/` files at runtime. Rows backed
by those subfolders are therefore confirmed configured on `hub`, while the same
capability on `items` is a vendor-default risk whose exact activation must be
captured from the assembled backend before freeze.

## Confirmed configured writers

| ID | Backend(s) | Writer/capability | Mutation and trigger | Identity used today | Replay/rollback quality | Required cutover disposition | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GOLD-WRITER-001 | items, hub | CMI economy provider through generic Vault API | Arbitrary credit/debit from any plugin calling `depositPlayer`/`withdrawPlayer` | Depends on caller; commonly Bukkit `OfflinePlayer` UUID or name | Vault response only; no durable business-leg fence or reversible receipt | Disable CMI economy as provider; central provider must reject generic writes | `CMI/config.yml:10-20,582-585` |
| GOLD-WRITER-002 | items, hub | CMI implicit starting balance | Creates a balance with `100.00` when legacy CMI initializes a player | CMI legacy player record; canonical account identity not attested | Implicit grant, no wallet ledger receipt | Replace only with explicit `OPENING_GRANT`; central account opens at `0.00` | `CMI/config.yml:81-95`; wallet contract `420-425` |
| GOLD-WRITER-003 | hub; items default-unverified | CMI `/money` operator surface | Administrative `pay/give/take/set` balance management; alias enabled | Player name, `all`, `alloffline`, or `allonline` command target | No contract idempotency key, source leg, or reversal receipt | Disable direct command; replace approved operator flows with privileged, audited wallet commands | `CMI/Settings/Alias.yml:743-746`; `CMI/Translations/Locale_EN.yml:2731-2751` |
| GOLD-WRITER-004 | items, hub | CMI `/pay`, including offline payments | P2P debit plus credit; tax capability exists but current tax type is `None` | Sender session plus target player name/offline lookup | No central atomic receipt or canonical account attestation | Disable; P2P transfer is outside wallet v1 and name/offline fallback is forbidden | `CMI/Settings/Alias.yml:806-809`; `CMI/config.yml:19-20,114-132` |
| GOLD-WRITER-005 | hub; items default-unverified | CMI money cheques | Withdraws gold into an item and later credits on redemption | Player/session plus bearer item; optional withdraw permission is not required locally | Bearer item is not a wallet business-leg fence; duplicate/recovery semantics unproven | Disable or replace with a first-party, receipt-backed instrument | `CMI/Settings/Modules.yml:116-118`; `CMI/config.yml:28-41`; `CMI/Settings/Alias.yml:159-162` |
| GOLD-WRITER-006 | hub; items default-unverified | CMI playtime rewards | Auto credits `20` hourly and `2000` daily; manual milestones credit `500` through `500000` | `[playerName]` in console command | Command execution has no durable wallet idempotency key or reversal receipt | Freeze before snapshot; replace with a first-party offline-capable writer using immutable account principal and durable reward leg | `CMI/Settings/Modules.yml:124-126`; `CMI/Settings/PlayTimeRewards.yml:1-73` |
| GOLD-WRITER-007 | hub; items default-unverified | CMI rank lifecycle | Auto-rank debits `100/300/150/100/100`; final rank command credits `100` | CMI player/rank record and `[playerName]` command | Rank state and balance leg are not proven atomic or replay-safe | Freeze rank money legs; replace each with explicit wallet transaction legs before re-enabling ranks | `CMI/Settings/Modules.yml:156-158`; `CMI/Settings/Ranks.yml:114-242`; `CMI/Settings/Alias.yml:878-881` |
| GOLD-WRITER-008 | hub; items default-unverified | CMI Newbie kit | Debits `100` on kit claim | Player command/session; kit permission is `cmi.kit.newbie` by local comment | No durable wallet receipt correlated with item grant | Remove money leg or coordinate kit delivery and wallet debit through a first-party saga | `CMI/Settings/Modules.yml:172-174`; `CMI/Kits/Kits.yml:1-13`; `CMI/Settings/Alias.yml:652-655` |
| GOLD-WRITER-009 | hub; items default-unverified | CMI `/sell` | Credits configured item worth | Player command/session | Item removal and credit atomicity, replay fence, and reversal receipt are unproven | Disable or replace with an owned sale transaction coordinator | `CMI/Settings/Alias.yml:1018-1021`; `CMI/config.yml:586-604`; populated `CMI/Saves/Worth.yml` |
| GOLD-WRITER-010 | hub; items default-unverified | CMI flight-charge purchase/auto-recharge | Debits `1.00` per recharge point | Player command/session | No wallet mutation context or business-leg receipt | Disable money recharge or replace with a first-party contextual debit | `CMI/Settings/Modules.yml:188-190`; `CMI/config.yml:1132-1160`; `CMI/Settings/Alias.yml:368-375` |
| GOLD-WRITER-011 | hub; items default-unverified | CMI scavenge | Debits base `100.00` plus `5%` of enchant/item worth | Player command/session | Item transformation and debit transaction semantics are unproven | Disable or replace with a first-party item/economy saga | `CMI/Settings/Scavenge.yml:59-65`; `CMI/Settings/Alias.yml:988-995` |
| GOLD-WRITER-012 | items | DeluxeMenus `requirements_menu` Vault actions | Debits `100/6400`, credits `50/3200` after item commands | Player name/session; menu permission `deluxemenus.shop` | Multi-command sequence is not atomic; `[takemoney]`/`[givemoney]` carry no operation context | Remove from loaded production menus or replace with owned shop transaction; generic Vault writes must fail closed | `DeluxeMenus/config.yml:11-19`; `DeluxeMenus/gui_menus/requirements_menu.yml:13-31,51-75,102-164` |
| GOLD-WRITER-013 | items, world | CoreTools shops (`vanilla_simple_example`, `mmoitems_example`) | Vault debit on buy and credit on sell; many price entries | CoreTools caster/player model; exact UUID/profile mapping unproven | Vendor transaction/retry behavior cannot be inspected without exact JAR | Freeze all Vault-priced shop entries; replace/adapter only after authenticated mutation and item-leg atomicity are proven | `CoreTools/config.yml:145-153`; `CoreTools/Shops/vanilla_simple_example.yml:1-3,235-255,297-322`; `CoreTools/Shops/mmoitems_example.yml:1-3,206-251,552-573` |
| GOLD-WRITER-014 | items, world | CoreTools stations (11 configured station files) | Ten transformation stations debit Vault; `selling_station` credits Vault | CoreTools caster; station permissions are blank, while main open command is `OP` | Vendor rollback/idempotency and item mutation ordering are unproven | Freeze Vault buttons or whole stations; replace with owned sagas before re-enable | `CoreTools/config.yml:162-165`; `CoreTools/commands.yml:77-82`; `CoreTools/Stations/{deconstruct,gemstone,identification,merge,modifiers,reforge,repair,reroll,selling,transmog,upgrading}_station.yml` (`left_click: 'vault'` and `vault:` formulas) |
| GOLD-WRITER-015 | items, world | CoreTools AuctionHouse (two configured markets) | Listing-fee debit, buyer debit, seller payout, tax, bid/refund/offline settlement surfaces | Buyer/seller names and vendor player/profile model; exact canonical mapping unproven | Multi-leg persistent flow exists by configuration, but provider receipt, exactly-once fences, and recovery semantics are unverified | Keep disabled/frozen for cutover; requires a dedicated escrow/market wallet integration, not generic Vault writes | `CoreTools/config.yml:167-171`; `CoreTools/commands.yml:105-110`; `CoreTools/AuctionHouse/{vanilla_example,mmoitems_example}.yml:1-11` plus Vault/listing-fee sections |

## Source-confirmed writers disabled by default

| ID | Backend | Writer/capability | Current state | Identity/replay gap | Required cutover disposition | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| GOLD-WRITER-016 | items | Profession steel-sword service fee | Adapter `enabled: false`; live craft calls Vault `withdrawPlayer`, rollback calls `depositPlayer` | Equates `account_id` with Bukkit UUID; transaction/leg IDs appear only in a synthetic local receipt and do not reach provider idempotency | Replace bridge with `WalletMutationPort`; reverse by original wallet receipt; do not enable in central mode beforehand | `ProfessionRuntimeAdapter/config.yml:1-9`; `VaultCurrencyAccountBridge.java:35-71`; `ProfessionRuntimePlugin.java:313-367` |
| GOLD-WRITER-017 | items | Profession validation balance setup | Adapter and validation both disabled; admin validation can deposit or withdraw to a target balance | Same Bukkit UUID identity; “set” is inferred from current balance and a second delta | Keep production-disabled; if retained for QA, use a dedicated staging-only principal and explicit wallet transactions | `ProfessionRuntimeAdapter/config.yml:1-9`; `ProfessionRuntimePlugin.java:379-390`; `plugin.yml:15-22` |

## Dormant, ambiguous, and unverified mutation capabilities

| ID | Backend(s) | Surface | Current classification | Why it is not counted as an active confirmed gold writer | Cutover gate | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| GOLD-CAP-001 | items, hub | CMI Votifier extra gold rewards | `DORMANT-CONFIGURED` | Money commands exist, but `ExtraRewardsEnabled: false`; normal vote commands currently give items only | Keep disabled or replace before ever enabling | `CMI/config.yml:692-710` |
| GOLD-CAP-002 | items, hub | CMI command costs | `DORMANT-CONFIGURED` | Global `Enabled: false` | Keep disabled; no cutover exception | `CMI/Settings/CommandCost.yml:1-16` |
| GOLD-CAP-003 | items, hub | CMI repair cost | `DORMANT-CONFIGURED` | `CostsMoney: false` | Keep disabled or replace with an item/economy saga | `CMI/config.yml:1471-1492` |
| GOLD-CAP-004 | items, hub | CMI ranged-chat shout cost | `DORMANT-CONFIGURED` | `ShoutCost: 0` | Keep zero or replace before non-zero activation | `CMI/Settings/Chat.yml:73-85` |
| GOLD-CAP-005 | items, hub | CMI schedules with `moneycost:` support | `DORMANT-CONFIGURED` | Schedule module is enabled, but all four schedule records are disabled and `moneycost:` occurs only in documentation comments | Boot assertion must reject any enabled schedule containing a money cost or money command | `CMI/Settings/Modules.yml:176-178`; `CMI/Settings/Schedules.yml:36,45,56,87,110` |
| GOLD-CAP-006 | world | MythicDungeons entry fee | `DORMANT-CONFIGURED` | Both checked-in dungeon configs use Vault but have `Cost: 0` | Keep zero or migrate to contextual wallet debit before non-zero cost | `MythicDungeons/maps/default-config.yml:112-115`; `MythicDungeons/maps/level_1/config.yml:127-130` |
| GOLD-CAP-007 | world | Multiverse-Core entry fees | `DORMANT-CONFIGURED` | Four worlds specify `@vault-economy`, but every fee is disabled and `0.0` | Keep disabled or migrate before activation | `Multiverse-Core/worlds.yml:13-16,91-94,169-172,247-250` |
| GOLD-CAP-008 | items | MMOCore `/withdraw` and `/deposit` | `POTENTIAL-UNVERIFIED` | Commands and gold coin/note/deposit items are configured, but no checked-in config establishes whether version `1.13.1-...-58` mutates Vault gold or a separate currency; vendor JAR is absent | Exact-JAR bytecode scan plus staging mutation trace is mandatory; freeze commands until classified | `MMOCore/commands.yml:73-94`; `MMOCore/items.yml:16-26,46-60` |
| GOLD-CAP-009 | items | MMOItems crafting-station money requirement | `POTENTIAL-UNVERIFIED` | Language contains a money requirement, but no checked-in MMOItems recipe/station config contains an active money amount | Exact-JAR/config expansion scan before cutover; current local config gives no active writer evidence | `MMOItems/language/crafting-stations.yml:8-10`; no non-language `money:` hits under `MMOItems/` |
| GOLD-CAP-010 | items | DeluxeMenus advanced example `eco take` | `POTENTIAL-UNVERIFIED / LIKELY NOT-GOLD` | The loaded menu checks Vault balance but dispatches console command `eco take`. CoreTools owns local `eco` alias while its Vault-provider flag is false, so the debit likely targets CoreTools `dollar`, not CMI gold; command ownership is not runtime-proven | Resolve command ownership and remove the example from production exposure; never treat this as a valid wallet debit | `DeluxeMenus/config.yml:11-19`; `DeluxeMenus/gui_menus/advanced_menu.yml:3-19,72-96`; `CoreTools/commands.yml:36-47`; `CoreTools/economy.yml:1-5` |
| GOLD-CAP-011 | items | Foundation starter grant documented as `cmi money give ... 25` | `STALE-DOCUMENTATION / NOT CURRENTLY CONFIGURED` | The spec and old QA evidence mention the command, but current `foundation_nexus.yml` contains no gold, starter-item, or claim command | Do not freeze a nonexistent writer; fix the separate documentation drift and ensure no generated/runtime menu reintroduces it | `docs/ai/foundation-mvp-spec-values.md:121-132`; `_validation/foundation_bot.js:592-608`; current `DeluxeMenus/gui_menus/foundation_nexus.yml:1-226` |

## Read-only and non-gold Vault surfaces

These are not gold writers on the inspected state, but they matter when proving
that disabling writes does not accidentally remove presentation or unrelated
capabilities.

| Surface | Classification | Evidence/notes |
| --- | --- | --- |
| CMI `/balance`, `/baltop`, tab-list balance placeholders | `READ-ONLY` | `CMI/Settings/Alias.yml:69-79`; `CMI/Settings/TabList.yml:43,77` |
| MMOInventory gold display | `READ-ONLY` | `%vault_eco_balance%` at `MMOInventory/inventory/default_mmoinventory.yml:275` |
| Guilds Vault references | `READ-ONLY / PERMISSIONS` | Only soft-dependency comments and `%vault_prefix%` were found; no economy field or money command in local Guilds YAML |
| LuckPerms Vault integration | `NOT-GOLD` | Permission/group provider, not an economy writer; permission storage was not inspected |
| CoreTools `core-economy` and physical currency | `NOT-CMI-GOLD ON CURRENT CONFIG` | Own `dollar` economy is enabled while `economy.vault: false`; the `eco` admin command mutates that separate model unless runtime command ownership proves otherwise |
| CoreTools PlayerVault references | `NOT-ECONOMY` | Inventory storage named “vault”, not Vault economy |
| MMOItems Vault mention | `NOT-GOLD` | Checked-in mention concerns a Vault-compatible permission plugin, not balance mutation |

## Identity and permission exposure gaps

The local writer set mixes at least four non-equivalent identity surfaces:

1. player-name command arguments (`cmi money`, rewards, ranks, menu console
   commands),
2. Bukkit `OfflinePlayer`/UUID generic Vault calls,
3. CoreTools caster/profile behavior, including its configured MMOProfiles
   modes, and
4. the canonical Domain 01 `account_id` plus current session attestation required
   by `account-session-resolution-contract`.

Only the fourth is valid for central-wallet mutation. Player name, Bukkit UUID,
MMOProfiles profile UUID, and HuskSync state cannot be silently promoted to
wallet ownership.

The checked-in files also do not prove effective LuckPerms grants. Operator-only
or permission-gated is therefore an exposure hint, not a freeze proof. A
pre-cutover audit must produce aggregate allow/deny evidence for every command
and menu capability without exporting raw player data or credentials.

## Mandatory writer-freeze manifest for cutover

The cutover gate should not be satisfied by a prose claim. It needs a generated,
version-pinned manifest containing at least:

| Field | Required value/evidence |
| --- | --- |
| `backend_id` | exactly `items`, `world`, or `hub` |
| `plugin_name` and `plugin_version` | exact loaded JAR identity and SHA-256 |
| `capability_id` | one ID from this matrix or a newly discovered ID |
| `mutation_route` | API method, command, menu action, scheduler, or lifecycle hook |
| `effective_exposure` | disabled, permission-denied, context-required, or migrated first-party writer |
| `selected_vault_provider` | exact provider name observed at boot |
| `provider_contract_version` | `2` for the central provider |
| `cutover_epoch` | the same durable epoch on all backends |
| `proof` | boot assertion, exact-JAR scan, permission aggregate, and focused mutation rejection test |

Minimum pass conditions:

1. CMI economy and every CMI writer in `GOLD-WRITER-002` through `011` are
   frozen on both `items` and `hub`.
2. DeluxeMenus and CoreTools generic Vault writers reject/fail closed or are
   removed from player exposure; no item leg can execute after a rejected debit.
3. Profession runtime uses `WalletMutationPort`; its old Vault bridge cannot be
   loaded in central mode.
4. MMOCore currency semantics and the `eco` command owner are resolved from the
   exact shipped JARs and a focused staging trace.
5. All three backends select exactly
   `MCMMORPGCentralWalletVaultProvider`, version `2`, with generic writes failing
   `WRITE_CONTEXT_REQUIRED`.
6. No permission, console, scheduler, offline-payment, or lifecycle path can
   bypass the authenticated account/session boundary.
7. The legacy count/checksum and backend acknowledgements required by the wallet
   contract are recorded before `CENTRAL_ACTIVE`.

## Current verdict

The writer inventory is sufficient to define the freeze scope, but not to pass
the cutover gate. There are 15 active/configured legacy writer families, two
disabled first-party writer families, seven dormant mutation families, and four
ambiguous or vendor-unverified surfaces. The highest-risk active paths are CMI
playtime auto-rewards, the dual-backend CMI authority, DeluxeMenus direct Vault
actions, and CoreTools shop/station/auction flows. Exact vendor JAR scanning,
effective-permission proof, and per-backend provider boot evidence remain
mandatory before configuration changes or migration execution.
