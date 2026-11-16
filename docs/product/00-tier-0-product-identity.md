# Tier 0 Product Identity

## Authority and status

This document is the canonical Domain `00` source for the owner-approved Tier 0
product intent. It is governed by `PRODUCT-GOV-006` through
`PRODUCT-GOV-010` in `00-decision-registry.yaml`.

| Axis | Status | Meaning |
| --- | --- | --- |
| `intent_status` | `approved` | The owner has approved the direction and it binds downstream product decisions. |
| `domain_00_status` | `promoted` | The intent is part of canonical Domain 00 product truth. |
| `market_validation_status` | `unvalidated` | Demand, differentiation, and segment fit are still hypotheses requiring player evidence. |
| `runtime_fulfilment_status` | `not_evaluated` | This document does not prove that any path or promise works in runtime. |
| `path_scope_status` | `approved` | The exact thirteen-path set and its classifications are canonical in `00-first-class-path-registry.yaml`. |
| `tier_completion_status` | `revision_required` | Tier 0 is not closed until the blocker set below is resolved. |

Norm revision: `tier0-r2`

Effective from: `2026-08-12`

Owner: project owner through Domain `00`

This document supersedes the Tier 0 artifacts as product-semantic authority.
Those artifacts remain decision evidence and review history; they do not form a
parallel source of truth.

## Open revision blockers

Promotion of product intent and path scope does not close these findings:

- `B-T0-OPERABILITY`: Tier 0 claims and anti-goals do not yet have complete
  decision tables, countermetrics, and repeatable unknown semantics.
- `B-T0-ACCOUNT-LOOPHOLE`: account, alt, and paid-slot semantics may still
  bypass specialization and monetization boundaries.

Until those blockers are resolved, Tier 0 may guide design but may not by
itself support a claim that the product is complete, market-validated,
runtime-fulfilled, or ready for release.

## 0.1 Player segments and audience hierarchy

### Segment promise

MC MMORPG is a persistent world in which a player can begin a meaningful path
from the first sessions and enjoy it without consuming all content. Segments
are defined by the desired relationship with the world, not by one activity or
one fixed number of hours.

### Primary segment

The primary segment is **self-directed inhabitants of a persistent MMO world**.

A primary player:

- wants to choose a meaningful role and deepen it into real mastery;
- may focus on combat PvE, professions and trade, pets and collection,
  guild/social play, or PvP;
- does not need to complete all content for their time and progression to have
  meaning;
- can begin the chosen path without a mandatory global combat-first core;
- accepts specialization and the need for goods or services from other roles;
- participates in a shared economic and social ecosystem;
- may play in short, weekend, or hardcore rhythms without losing the right to
  meaningful participation.

The exact path identities, owners, minimum progression-fantasy cards, required
surfaces, and separate scope/delivery statuses are canonical in
`00-first-class-path-registry.yaml` through `PRODUCT-GOV-010`.

### Equal paths

Equal standing does not mean identical rewards or statistics.

- Paths may matter through different kinds of progression: power, wealth,
  influence, access, knowledge, collection, reputation, or prestige.
- Mastery requires depth, effort, decisions, and path-appropriate challenge.
- Mastery in one path must not require personally levelling unrelated loops as
  a checklist.
- Goods from other loops may be required through trade, services, or
  cooperation; the player need not personally perform the source activity.

### Canonical first-class path scope

Tier 0 contains exactly thirteen first-class paths:

1. Islands;
2. Dungeons;
3. PvP;
4. Guild;
5. Pets;
6. Miner;
7. Lumberjack;
8. Harvester;
9. Blacksmith;
10. Armorer;
11. Alchemist;
12. Artificer;
13. Cook.

The eight professions retain independent status. One working profession does
not make the profession family complete. PvP is one path with separately
reported duel, arena/ranked, open-world/contested, and guild-war surfaces. Pets
is one path containing acquisition, breeding, training, collection, and pet
battles.

Builder / Architect is a candidate, not a current first-class path. Trade and
market, collection, hybrid building, quests and narrative, classes and builds,
itemization, economy, basic social tools, public events, and raids are
supporting capabilities. Promotion from candidate or supporting status requires
a new explicit Domain 00 decision and registry revision.

Scope approval does not prove delivery. An unqualified full-scope claim requires
all thirteen paths to pass the same declared release envelope; a bounded pilot,
alpha, or release may cover fewer paths only by naming them explicitly and not
claiming full Tier 0 coverage.

### Play rhythms

More active time may provide a large advantage, faster development, greater
scale, mastery, and prestige. It must not automatically create permanent
exclusion of less-active rhythms from meaningful participation.

### PvP audience doctrine

- Level, build, and gear provide real advantage.
- Skill, timing, positioning, decisions, and counterplay may overcome a
  reasonable progression difference.
- Skill does not erase an arbitrary progression chasm.
- A highly developed level-60 character should normally defeat a much weaker,
  lower-level character in a comparable open progression context.
- Fairness means context-appropriate balance and counterplay, not universal
  character symmetry.

Exact fairness rules for ranked, open-world, duel, and guild-war contexts remain
downstream decisions.

### Secondary segments

The product may serve, without subordinating the whole world to:

- completionists who consume many or all loops;
- hardcore optimizers who maximize multiple systems;
- friend-driven players who arrive mainly for their social group;
- players attracted by one strong subsystem but indifferent to the broader
  persistent world.

### Deliberately not primary

The product is not optimized for:

1. full self-sufficiency without trade, services, or other specializations;
2. unlimited permanent dominance produced by time alone;
3. mandatory combat-first progression for every player;
4. pure esports PvP requiring universal character symmetry.

These are conflict priorities, not bans on those players joining the product.

## 0.2 Jobs-to-be-done and unmet needs

### Main job-to-be-done

> When I enter a persistent MMORPG world, I want to choose my own valuable path
> and develop it at my own rhythm, so that I feel progress in play, build a
> lasting body of work over time, and let the results of my mastery matter to
> other players without being forced to play somebody else's style.

The common promise is **autonomous progress now, lasting accomplishment over
time, and a real place in the shared world**.

### Functional jobs

The primary player wants to:

1. understand the available possibilities and choose the session goal;
2. make perceptible personal progress during short or solitary sessions;
3. encounter deeper decisions, skills, and specializations in the chosen path;
4. turn development into visible and lasting path-appropriate accomplishment;
5. provide real value to players through direct or indirect trade, services,
   knowledge, influence, or cooperation;
6. return after a break, recognize their place, and continue meaningfully;
7. build a legible identity and optionally show credible signs of mastery.

The game should show projects, opportunities, and plausible next steps while
leaving the choice to the player. Guidance must not become a mandatory
checklist.

### Emotional and social jobs

The player wants autonomy, competence, agency, trust that invested time matters,
and the ability to play calmly without turning the game into overdue work. The
player may be useful to a shared world without mandatory direct socialization
in every session.

Public hierarchy is not universal. Profession reputation, PvP rating, guild
influence, collection display, and PvE achievements may express mastery in
different ways. Public exposure depends on the path and player choice unless
fairness, safety, or competitive integrity requires disclosure.

### Session and long-term horizons

A solitary session may be satisfying through personal development alone.
Sale, conversation, grouping, or public recognition are not mandatory every
evening.

Over the long term, levels and capabilities alone are insufficient. Progress
should create a path-appropriate accomplishment such as a farm, collection,
equipment set, wealth, discovered knowledge, guild position, or history of
victories. A path also needs a credible route to ecosystem contribution, but
that does not yet define a mandatory transaction or population/liquidity
envelope.

The world may move on during absence. A player may miss events or opportunities,
but the foundation of identity, progression, and lasting accomplishment retains
meaning and the game provides an understandable return path.

### Unmet-needs hypothesis

The product intends to answer the lack of a persistent world that combines:

- deep paths rather than side minigames;
- access without unrelated mandatory grind;
- progression joined to lasting accomplishment;
- results that may matter to other players;
- helpful orientation without daily chores and harmful FOMO;
- protection from arbitrary resets, uncontrolled inflation, and deliberate
  invalidation;
- return after absence without treating play as debt;
- solitary satisfaction connected to a real MMO ecosystem;
- path-appropriate expressions of mastery rather than one universal ranking.

These are owner-approved hypotheses, not established market facts.

## 0.3 Distinctive promise and progression fantasy

### Distinctive promise

> **Choose your own full path in a persistent Minecraft MMORPG world. Become
> what you practice, build an accomplishment that exists in the world, and take
> your own place in the player ecosystem.**

The promise combines five properties:

1. **Earned identity:** practice, decisions, and mastery shape what the player
   can do and who the character becomes.
2. **Multiple meaningful paths:** no single global path is mandatory for all
   players.
3. **Real interdependence:** specializations can create results useful to other
   players while individual sessions remain independently satisfying.
4. **Remembered accomplishment:** effort creates persistent history and value,
   not only a temporary reward or a larger number.
5. **Minecraft world embodiment:** where appropriate to the path, progression
   has visible, spatial, visitable, or otherwise perceptible consequences in
   the world rather than existing only in menus.

The minimum path-appropriate world manifestation is defined per approved path
in `00-first-class-path-registry.yaml`. Repeatable thresholds and automated
verdict semantics remain unresolved by `B-T0-OPERABILITY`.

### Shared progression grammar, different peaks

There is no universal endgame peak. Paths share a progression grammar:

1. discover possibilities;
2. choose a direction;
3. take a real first step;
4. practice and deepen competence;
5. specialize;
6. build path-appropriate accomplishment;
7. reach path-specific mastery, influence, or fulfilment.

A warrior, farmer, breeder, trader, collector, PvP player, and guild leader may
therefore reach fundamentally different conclusions.

### Time horizons

Every approved first-class path must allow a genuine first step in its fantasy
in the first session. The player need not receive a major reward or
specialization immediately, but must not finish several sessions of unrelated
content before beginning the chosen path.

Later progression has three common signals: growing capabilities, increasingly
legible identity, and accumulating accomplishment worth returning to. Exact
milestones after several sessions and after a month must be path-specific.

### Accomplishment and exposure

The preferred product model gives accomplishment a world presence appropriate
to the path. Farms, workshops, headquarters, or exhibitions may be spatial;
combat and PvP may manifest through equipment, access, trophies, titles, and
history; trade, knowledge, reputation, and influence may combine system records
with visible world consequences.

This does not grant unrestricted editing of the shared map. Ownership,
instances, plots, protection, and world mutation remain downstream contracts.

Accomplishment and transformation must be legible to the player. Public display
is an option rather than a universal obligation, except where social trust,
competitive integrity, or safety requires visibility.

## 0.4 Product anti-goals

### Normative force

Anti-goals are binding product boundaries. A team may present evidence and
request an amendment, but may not bypass an anti-goal with a local waiver or a
player-facing experiment. Retention, revenue, schedule, and local metrics do
not override them. An amendment requires a new explicit Domain 00 decision and
must not retroactively turn a historical violation into `PASS`.

Operational thresholds and the complete amendment protocol remain blocked by
`B-T0-OPERABILITY`.

### AG-01 — Retention through obligation

The product rejects daily/weekly systems, streaks, time windows, or FOMO when
they become the dominant progression route, create material attendance debt,
or invalidate a meaningful return path. Optional recurring content and
moderate non-dominant bonuses remain allowed.

### AG-02 — Artificial social gate

Guild membership and grouping must not become universal gates for meaningful
long-term progression. Naturally group-based goals such as raids, guild wars,
and social leadership paths may require coordination.

### AG-03 — Purchased outcome

Payment must not buy combat power, PvP advantage, mastery, competitive standing,
prestige presented as achievement, durable economic advantage, market access,
or a bypass around meaningful time, risk, trial, or choice. Cosmetics,
presentation, and bounded convenience may be sold only when they do not replace
those values. Account/alt/paid-slot enforcement remains blocked by
`B-T0-ACCOUNT-LOOPHOLE`.

### AG-04 — Arbitrary invalidation of durable accomplishment

Durable character identity, mastery, and core accomplishment must not be
silently reset or reclassified as seasonal. Explicitly seasonal competitive
state may reset. Balance, migration, transformation, and system retirement are
allowed with explicit policy and protection of material player value.

### AG-05 — Empty grind as the best strategy

Long effort is valid, but AFK, bot-like repetition, alt exploitation, and safe
spam of low-value actions must not outperform meaningful path-appropriate
practice. Calm repetition, collection, production, training, and target farming
remain valid when active effort and decisions retain meaning.

### AG-06 — False completeness

Documents, menus, XP bars, and roadmap promises do not make a path complete.
Narrow pilots are allowed, but public scope and language must match the paths
actually evidenced. Missing runtime or player evidence yields
`CANNOT_JUDGE`, not `PASS`.

### AG-07 — False difficulty and unreliable server execution

High mechanical difficulty is allowed and desired. PvP may be twitch-heavy and
PvE may demand precise execution. Difficulty must not come only from health and
damage scaling, unreadable chaos, unstable server tick rate, server-side lag,
or desynchronization. Optimization and capacity boundaries are part of the
design rather than optional polish.

Dungeon difficulty may use explicit selectable levels. The approved owner
direction is that Islands should provide corresponding challenge primarily
through world and regional construction rather than being a flat map with
statistically stronger mobs. The detailed Island contract remains a downstream
Domain 06/Tier 1–2 decision and is not closed here.

### AG-08 — One mandatory fantasy for everyone

The product rejects mandatory combat-first access, a self-sufficient master of
all loops, one universal endgame, one success measure, mandatory public ranking
for all paths, and reduction of Minecraft to a background for menus and numbers.

### AG-09 — Permanent exclusion through time alone

Large active effort may create large advantages, wealth, mastery, scale, and
prestige. Historical time advantage must not become a permanent monopoly over
meaningful participation without counterplay, renewed entry opportunities, or
other constraints on irreversible snowball.

### AG-10 — Permanent maintenance of every mechanic

Player value is durable; every implementation is not. Dead, duplicative, or
harmful systems may be simplified, merged, replaced, or retired through an
explicit lifecycle that accounts for dependencies and protects material player
value through migration, conversion, or proportionate compensation.

## Evidence boundary

This document and the path registry authorize product intent and exact path
scope only. They do not prove:

- delivery, completeness, or release coverage of any first-class path;
- market demand or competitive differentiation;
- runtime behavior, balance, performance, accessibility, or operability;
- exact anti-goal thresholds or repeatable automated classification;
- account-wide resistance to alt and paid-slot exploitation;
- release readiness.

Those claims require the appropriate downstream decisions and evidence. The
Project Health Monitor must preserve `CANNOT_JUDGE` whenever those observations
are missing or incomplete.
