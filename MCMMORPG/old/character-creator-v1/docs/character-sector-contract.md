# Character Sector Contract

Status: foundation v0.4 hardening target, 2026-07-04.

Scope: items, menus, classes, races, inventory, profiles, forge, city-acquired first equipment, skills, professions, and the first equipment loop.

## Creator Visual Language

Foundation creator is no longer a flat list of menus. The canonical flow is:

`creator intro -> race gallery -> race spotlight -> class gallery -> class spotlight -> oath screen -> mentor roster -> city roster`

Rules:

- gallery screens do not finalize the choice;
- spotlight screens own fantasy, cost, immediate payoff, and next step;
- oath screens own the final cost of class choice;
- mentor roster is the first required payoff after class confirmation;
- city roster is the first product-facing hub, not a link board.

Every premium creator screen must expose:

- one fantasy statement,
- one real immediate payoff,
- one explicit weakness/cost,
- one visible next step.

## Canonical Class IDs

Backend IDs are lowercase MMOCore class IDs. Display names are player-facing Polish names.

| Backend ID | Display | Family | Primary resource | Tree |
| --- | --- | --- | --- | --- |
| `human` | Origin | starter only | none | `loop` |
| `warrior` | Wojownik | front/rage | Rage via MMOCore mana bar | `warrior-paladin` |
| `rogue` | Lotrzyk | bleed/momentum | tempo windows | `rogue-marksman` |
| `marksman` | Lowca | mark/trap/range | stamina | `rogue-marksman` |
| `mage` | Mag | mana/overload | mana | `mage-arcane-mage` |
| `paladin` | Akolita | faith/ward/sustain | faith-flavored mana | `warrior-paladin` |

Item restrictions must use backend IDs or confirmed plugin aliases only. Legacy names such as `Wizard` and `Archer` are not canonical for new foundation items.

## Race Contract

Race choice is currently identity and routing metadata, not starter power. Race sigils are allowed to carry lore and future hooks, but no menu may imply that race passives are already active unless a matching passive implementation exists.

Foundation v0.4.1 adds one live layer before full combat/runtime passives: a small race-linked `city boon` paid through the first-contract loop. This boon is intentionally light and does not replace the planned passive matrix.

Allowed race IDs:

- `CZLOWIEK`
- `ORK`
- `ELF`
- `KRASNOLUD`
- `NIEUMARLY`

Future race mechanics must be added as an explicit matrix: passive, trigger, PvE effect, PvP cap, counterplay, and affected systems.

## Starter Progression Policy

Foundation v0.4.1 uses direct race selection, direct class selection, spotlight review, oath confirmation, and a city onboarding roster. This is the live contract for the current config.

The earlier style-test recommendation flow is retired from the canonical creator path. Do not describe any recommendation card, quiz, or inferred class selection as part of the player-facing foundation creator unless it is explicitly reintroduced as an optional side feature.

Class respec is not a casual menu button in v0.4. The safe policy is:

- before confirmation: player may go back and choose another archetype;
- after confirmation: use a new MMOProfiles profile for a different identity;
- future respec requires a dedicated token/NPC/cost and must clear or migrate gear assumptions explicitly.

## Race Passive Matrix

Race passives are intentionally small first-act nudges. They must never replace class identity.

| Race ID | Passive | Trigger | PvE effect | PvP cap | Counterplay | Affected systems |
| --- | --- | --- | --- | --- | --- | --- |
| `CZLOWIEK` | Kontrakt Ocalalego | contract/reward turn-in | +2% money/reputation rewards | no combat bonus | none needed; economy only | economy, reputation |
| `ORK` | Krwawy Nacisk | first combat window | short melee pressure bonus | capped burst window | kite, slow, disengage | melee, threat |
| `ELF` | Pierwszy Trop | first ranged hit or mark | small precision/opening bonus | one opener only | force melee, break line | ranged, tracking |
| `KRASNOLUD` | Dlug Stali | forge or blocked hit | minor armor/forge-quality bonus | defensive only | mobility pressure | armor, forge |
| `NIEUMARLY` | Odmowa Grobu | dark/curse pressure | minor darkness/curse resistance | healing penalty remains | anti-heal, light effects | curse, sustain |

Implementation rule: if a passive cannot be expressed safely yet, its menu copy must say `contracted/passive pending runtime hook` and not claim live combat power.

Live v0.4.1 rule: race menus may claim a small `city boon` only if the first-contract scripts actually grant it.

## Class Selection Rules

The current creator is direct-selection only. The player chooses class from the gallery, inspects spotlight, then confirms it on the oath screen.

Rules:

- no recommendation card in the canonical path;
- no automatic class inference from race or earlier clicks;
- class is locked only after the oath screen confirm;
- reconnect before oath must resume gallery, spotlight, or oath screen, not skip the decision.

## City NPC Roster

The character sector must route first equipment, skill choices, professions, forge, and storage through city-facing NPCs or explicit city menus. Class confirmation may set identity, but it must not grant equipment directly. Current `foundation_city_roster` menus are an implementation stand-in for physical NPCs until hub NPC placement and runtime NPC proof exist.

Foundation v0.4.1 adds `mentor roster` as a required player-facing seam between class fantasy and class confirmation. Mentor flow is allowed to be menu-backed, but it must expose live loop, weakness, anchor gear direction, and subclass preview.

Minimum roster:

- Class Mentors: explain class identity, locks, class skills, and future subclass path.
- Skill Trainer: exposes class, generic, profession, and gear skill categories with lock reasons.
- Profession Trainers: introduce gathering, crafting, service systems, and profession skill rules.
- Quartermaster: owns the first equipment claim after class confirmation.
- Forge/Salvage NPC: owns item improvement and material conversion.
- Bank/Storage NPC: owns storage entry and must obey safe-zone/combat-lock rules.

Mentor minimums:

- live loop statement,
- main weakness statement,
- anchor gear direction,
- visible preview of the three subclass paths.

## Skill Roster Rules

Every player-facing skill must have a category, source, lock, and counterplay statement before it is treated as live.

| Category | Source | Lock |
| --- | --- | --- |
| `class_skill` | class mentor, class progression | class/subclass |
| `generic_skill` | skill trainer | slot limit and level/tutorial gate |
| `profession_skill` | profession trainer, profession use | profession progress and recipe/tool source |
| `gear_skill` | item, forge, dungeon reward | item equipped and item restrictions |

Generic skills are allowed to teach movement, guard, focus, field support, and simple utility. They must not replace class identity. Profession skills are allowed to support preparation, crafting, gathering, traps, consumables, repair, salvage, and utility, but they must not become stronger class kits.

## City-Acquired First Equipment

The first equipment loop is city-acquired, not class auto-granted.

Rules:

- Class confirmation only records race/class/profile state and opens the city roster.
- First equipment comes from the Quartermaster or an equivalent city NPC/menu after class confirmation.
- The first equipment source must be visible to the player as `city/quartermaster/first_contract`.
- Follow-up power must come from forge, professions, dungeon rewards, salvage, and material sources.
- Any direct `mi give` inside class finalization is a forbidden pattern.

## Active Item Pool

The first foundation loop allows only project-facing content in player rewards, starter flow, forge outputs, and Nexus-facing explanations.

Allowed foundation set IDs:

- `PIONIER`
- `WEDROWIEC`
- `ADEPT`
- `ZWIADOWCA`

Legacy/demo sets such as `ARCANE`, `DRAGON`, `PSYCHIC`, `SPELLCASTER`, `OMNIELEMENTAL`, `UNDEADSLAYER`, `STEEL`, `GINGERBREAD`, and `HATRED` are not valid first-loop rewards.

Legacy/demo crafting stations are retained only as reference config. Every legacy station must be visibly marked `[LEGACY OFF]` and every recipe must require `foundation.legacy.disabled`. The only player-facing station in foundation v0.4 is `foundation-forge`.

Allowed foundation gem IDs:

- `RUBIN_NATARCIA`
- `GRANAT_BASTIONU`
- `SZAFIR_SKUPIENIA`
- `PERLA_PRZEPLYWU`
- `SZMARAGD_TROPU`
- `AGAT_ZWINNOSCI`
- `OPAL_RZEMIOSLA`

Allowed foundation off-hand/catalyst IDs:

- `LOWCA_INITIATE_SCOPE`
- `MAG_INITIATE_FOCUS`
- `AKOLITA_INITIATE_RELIC`
- `KATALIZATOR_RUNICZNY`
- `BURZOMANTA_CORE`

## Equipment Gate Policy

Use gates for player clarity, not accidental lockouts.

- Starter rewards must have no stat requirement and `required-level` at most `1`.
- Forge replacements may start at level `8` to `10`.
- Subclass or advanced identity items may start at level `20` to `25`.
- `required-class` must use canonical backend IDs or a documented confirmed alias.
- Accessories should use raw stat gates only when the item teaches a clear stat identity.
- Off-hand items must state whether they are starter teaching tools, permanent class anchors, or optional build pieces.

## Material Sources

| Material | Source contract |
| --- | --- |
| `ZLOM_ZELAZNY` | overworld monster drops, salvage |
| `GARBOWANA_SKORA` | spider/beast drops, level_1 shared drops |
| `SUROWA_SKORA` | level_1 shared drops |
| `MALA_KOSC` | level_1 shared drops |
| `WILCZY_KIEL` | level_1 shared drops |
| `DZIKIE_ZIOLO` | level_1 shared drops and zone rewards |
| `USZKODZONY_KORZEN` | level_1 shared drops and zone rewards |
| `RDZEN_RUNICZNY` | witch/blaze drops, level_1 boss and loot tables |
| `ODLAMEK_KLEJNOTU` | skeleton/witch drops, level_1 boss and shared drops |
| `SLABA_ESENCJA` | level_1 shared drops and zone rewards |
| `SLABY_FRAGMENT_RUNY` | level_1 boss loot |
| `FRAGMENT_RELIKTU` | blaze drops, level_1 boss loot, forge conversion |
| `ZETON_GAJU` | level_1 boss loot |
| `RDZEN_STRAZNIKA` | level_1 boss loot |
| `MAGICAL_WEAPON_ESSENCE` | deconstruction of magical-tier items |
| `RARE_WEAPON_ESSENCE` | deconstruction of rare-tier items |

Forge UI and Nexus copy must route players back to these sources.

## Skill Catalog

The active project allows built-in MMOCore skills only through this catalog until custom skills are implemented. This catalog is not enough by itself: live player-facing skills also need a category and lock from `Skill Roster Rules`.

| Skill ID | Current use | Contract |
| --- | --- | --- |
| `WEAKEN` | Warrior, Marksman | opening debuff / mark |
| `COMBO_ATTACK` | Warrior, Marksman | pressure chain |
| `EMPOWERED_ATTACK` | Warrior, Gladiator | heavy hit / front pressure |
| `DEEP_WOUND` | Warrior, Rogue, Paladin | bleed / punishment |
| `FIRE_BERSERKER` | Warrior | rage signature |
| `FURTIVE_STRIKE` | Rogue | opener / burst window |
| `SNEAKY_PICKY` | Rogue | utility / setup |
| `EVADE` | Marksman, Rogue paths | survival / reposition |
| `MINOR_HEALINGS` | support fallback | minor sustain |
| `GREATER_HEALINGS` | Paladin | support sustain |
| `HUMAN_SHIELD` | Paladin | ward/front protection |
| `AMBERS` | Mage/Paladin | burning pressure / ritual tool |
| `POWER_MARK` | Mage | setup marker |
| `ICE_SPIKES` | Mage | control |
| `FIREBALL` | Mage | burst |
| `FIRE_STORM` | Mage | AoE pressure |
| `WARP` | Mage | mobility |

Each new class skill must define: function, tags, damage type, resource, scaling, PvE use, PvP use, counterplay, and weakness before being treated as a custom foundation skill.

## Mastery Gates

Subclass and advanced identity unlocks require more than level. Minimum v0.4 gate model:

| Gate | Required signal | Applies to |
| --- | --- | --- |
| Level gate | MMOCore level threshold | all subclasses |
| Weapon use | class-family weapon or off-hand used in content | martial/ranged subclasses |
| Skill use | at least one cataloged core skill used repeatedly | all subclasses |
| Survival pressure | survives a teaching encounter or boss mechanic | front/sustain subclasses |
| Class trial | completes a role-specific trial objective | subclass unlock |

If a gate is not runtime-counted yet, it must stay documented as `pending runtime hook` and must not be presented as live unlock logic.

## Storage Access Rules

Account/profile storage is foundation convenience, not combat power.

- Player-facing storage may open only from hub/safe-zone flow.
- Storage must refuse combat-locked use.
- Until a safe-zone/combat-lock hook exists, player-facing storage buttons must refuse opening instead of calling `openvault` directly.
- Profile vault is per-character; account vault is account-wide convenience.
- If the storage plugin cannot express safe-zone/combat conditions directly, CoreTools must proxy the open command and own the refusal message.

## Validation Levels

Required checks:

- Static config contracts: menus, scripts, variables, item IDs, starter safety, duplicate YAML keys, permissions, profile settings, active item pool, spotlight files, hero-card markers, and creator asset mapping.
- Access checks: default player can open Nexus paths for race/class, city roster, skill roster, profession roster, first contract, profiles, inventory, class panel, skills, skill trees, forge, and dungeon entry.
- Player path proof: fresh profile can complete race -> class -> city roster -> skill roster -> profession intro -> quartermaster first equipment -> inventory -> forge preview -> level_1 reward use.

The current validator is responsible for static contracts. Runtime access and full player path checks need a separate harness.

## Product Proof

Static green is necessary but not sufficient. Product proof for the creator requires:

- every race has a gallery entry and a dedicated spotlight;
- every class has a gallery entry, spotlight, and oath screen;
- every spotlight names a real immediate payoff and a real cost;
- every class confirmation leads into mentor payoff before city utility;
- the creator asset contract maps MMOItems UI tokens to reserved CMD IDs and GUI fallbacks.

## Runtime Acceptance Path

The required player proof path for this sector is:

`fresh profile -> creator intro -> race selector -> race spotlight -> class gallery -> class spotlight -> class oath -> mentor roster -> city NPC roster -> skill roster -> profession intro -> quartermaster first equipment + race city boon -> RPG inventory -> foundation forge preview -> level_1 reward item use`

Passing static validation is not enough. Runtime validation must report PASS, FAIL, BLOCKED, or INSUFFICIENT_EVIDENCE with the exact failing seam.
