# MCMMORPG Excellence Playbook

This file is the quality bar for turning the foundation slice into a serious MMO-RPG instead of a menu-driven kit server.

## Reference Set

Use these games for pattern-level inspiration only. Do not copy names, text, art, quests, exact mechanics, or proprietary presentation.

- World of Warcraft: readable roles, dungeon clarity, talent identity, long-term expansion cadence.
- Final Fantasy XIV: strong job fantasy, main-scenario gravity, duty structure, accessible party expectations.
- Guild Wars 2: dynamic events, weapon/profession identity, low-friction cooperation, exploration rewards.
- Elder Scrolls Online: freeform exploration, race/class/world fantasy, crafting skill lines, solo-friendly MMO path.
- Star Wars: The Old Republic: class story framing, companions, moral flavor, cinematic mission beats.
- RuneScape / Old School RuneScape: long-horizon skills, self-set goals, economy, challenge modes.
- EVE Online: player economy, scarcity, social stakes, corporations, risk-driven stories.
- Black Desert Online: action feel, life skills, mounts, strong character identity, world systems.
- Lost Ark: spectacle, encounter telegraphs, satisfying dungeon and boss cadence.
- EverQuest / Pantheon: grouping value, role interdependence, hard-earned progression, community reputation.
- Albion / Ultima Online / ArcheAge: sandbox economy, crafting relevance, territory and player agency.
- LOTRO / Secret World / SWTOR: world tone, narrative hooks, investigation and lore depth.

## Non-Negotiable Design Bar

- Every visible UI screen must answer: what fantasy is this selling, what decision is being made, and what consequence follows?
- Every class must have a resource or rhythm, a strength, a weakness, a useful defensive answer, and one signature gameplay promise.
- Every race must start as identity and world hook first. Add power later only after combat balance is proven.
- Every item must have a reason to exist beyond numbers: origin, build direction, salvage/craft use, set hook, or prestige value.
- Every dungeon boss must have readable tells, phase movement, punishment, recovery windows, and difficulty-specific reward logic.
- Every system must point into another system: menu -> identity -> dungeon -> loot -> forge -> build -> harder contract.
- No milestone is done from config existence. It needs static contract proof, runtime proof, integration proof, and player-path proof when applicable.

## Vertical Slice Target

The first high-quality slice is:

1. Player opens the Nexus and sees a world-facing contract board, not a tutorial panel.
2. Player binds a race as a rodowod/pieczec decision.
3. Player binds a class as an archetype/przysiega decision.
4. Player receives starter gear that explains playstyle and weakness.
5. Player enters Kwietna Polana.
6. Dungeon proves combat entities, boss phases, add pressure, kill -> finish, reward, and return.
7. Loot feeds the forge into a visible build upgrade.

## Level 1 Next Quality Gaps

`level_1` / `Kwietna Polana` already has a technical route: start camp, path ambushes, trial waves, Grove Warden, root gate, Grove Guardian, reward chamber, and leave portal. The missing bar is making that route feel like an event and proving it end to end.

Priority order:

1. Prove or repair the exact player path: `Portal Nexus -> md play level_1 -> waves -> Grove Warden -> root gate -> Grove Guardian -> finish -> reward -> leave`.
2. Make `Drzewo-Serce Gaju` the dominant scenic anchor behind the boss, with corruption visibly pushing inward from the edges.
3. Add corruption objectives, not only kill gates: cleanse roots, break growth nodes, activate sigils, or weaken the boss through arena interaction.
4. Give trash and gatekeeper clearer roles: wolf = pressure, sprout = poison/control, warden = bridge test, boss = phase exam.
5. Turn victory into a reward scene: death finale, opened chamber, named loot moment, forge hint, and HARD-only chase rewards.

## Class Direction

- Wojownik: Guard / Rage / frontline pressure. Strong sustain and threat. Weak against kiting and poor target access.
- Lotrzyk: Momentum / Bleed / burst windows. Strong tempo and execution. Weak when timing fails.
- Lowca: Mark / Trap / distance control. Strong preparation and safe damage. Weak in forced melee chaos.
- Mag: Mana / Overload / controlled burst. Strong damage windows and control. Weak under interruption and pressure.
- Akolita: Faith / Ward / sustain-control. Strong shields, recovery and group safety. Weak in kill speed.

## Itemization Direction

- Starter items are archetype declarations, not free power fantasies.
- Level 1 loot should form named families: Gajowe Relikty, Pekniete Runy, Zelazna Wyprawa, Runiczna Wyprawa.
- Affix families should be readable: sustain, burst, cooldown, ward, bleed, focus, threat, movement.
- Forge recipes must convert dungeon proof into build direction.

## Content Review Checklist

- Does this feel like a serious RPG world, or like a plugin demo?
- Would a player understand what fantasy they are buying into within five seconds?
- Is there a real choice, weakness, risk, or cost?
- Does the reward change how the player thinks about the next run?
- Is the exact player path validated, not just the YAML?
