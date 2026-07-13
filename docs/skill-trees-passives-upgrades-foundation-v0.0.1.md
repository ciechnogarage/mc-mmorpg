# Skill Trees, Passives & Ability Upgrades Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje, jak skille, pasywki i buildy rozwijaja sie po wyborze klasy, podklasy i upgrade podklasy.

Po `class-skill-kits-foundation-v0.0.1.md` wiemy, czym klasy graja. Ten dokument odpowiada na kolejne pytanie: jak gracz rozwija te skille, jakie wybory podejmuje, czego nie moze miec naraz i jak pasywki nie niszcza PvP counterplayu.

Zakres:

- class passives po Level 10,
- subclass passives po Level 25,
- upgrade passives po Level 50,
- skill upgrades dla core i signature skilli,
- mastery nodes,
- gear modifiers,
- ograniczenia wyborow,
- PvP sanity rules.

Nie definiujemy jeszcze finalnych liczb balansu, pelnych drzewek ani konfiguracji pluginow.

## Core Progression Model

Warstwy rozwoju:

- Starter skills: Level 1-10, testery stylow gry.
- Class skills: Level 10, core class kit.
- Class passive: Level 10, bazowa pasywka kierunku klasy.
- Subclass signature: Level 25, glowny skill podklasy.
- Subclass passive: Level 25, pasywka nadajaca styl podklasy.
- Skill upgrades: modyfikatory konkretnych skilli.
- Mastery nodes: bonusy za realne uzywanie stylu gry.
- Gear modifiers: itemy wzmacniaja tagi, skille i buildy.
- Upgrade passive: Level 50, advanced specialization wybranej podklasy.

Zasada: gracz rozwija wybrany kierunek, ale nie odblokowuje wszystkiego naraz.

## Passive Types

### Class Passive

Class passive odblokowuje sie po wyborze klasy bazowej na Level 10.

Funkcja:

- wzmacnia bazowy styl klasy,
- pokazuje kierunek statow i mastery,
- nie zamyka jeszcze podklasy,
- nie daje pelnej specjalizacji.

Przyklady:

- Wojownik: bonus do stamina tempo po block/counter.
- Lotrzyk: bonus do mobility/crit po flankowaniu.
- Lowca: bonus do projectile pressure i tracking.
- Mag: bonus do mana/ward tempo.
- Akolita: bonus do Faith utility, heal/ward/debuff baseline.

### Subclass Passive

Subclass passive odblokowuje sie po wyborze podklasy na Level 25.

Funkcja:

- nadaje konkretny styl,
- wzmacnia signature skill,
- wymusza tradeoff,
- tworzy wyrazna roznice miedzy podklasami tej samej klasy.

Przyklady:

- Straznik: mocniejszy guard i ally protection, nizszy kill pressure.
- Berserker: wiecej rage pressure, slabsza defensywa po burst window.
- Zabojca: mocniejszy opener, slabszy sustain po nieudanym engage.
- Kapelan: mocniejszy heal/ward, nizszy direct damage.
- Rytualista / Warlock: mocniejsze curse/summon loop, slabsza mobilnosc.

### Upgrade Passive

Upgrade passive odblokowuje sie na Level 50.

Funkcja:

- rozwija wybrana podklase,
- nie daje drugiej pelnej podklasy,
- daje endgame identity,
- zachowuje counterplay.

Przyklady:

- Demonolog: mocniejsze summony i pact windows, wieksza zaleznosc od owner positioning.
- Klatwomistrz: mocniejsze curse/debuff/drain, slabszy direct summon pressure.
- Deadeye: mocniejszy precision single-target, mniejsza elastycznosc defensive.
- Cryoguard: wiecej ward/control, nizszy burst.

## Skill Upgrade Types

Skill upgrade to wybor modyfikujacy konkretny skill.

Typy upgrade:

- Damage Upgrade: wiekszy damage kosztem zasobu, cast time albo cooldownu.
- Utility Upgrade: dodatkowy reveal, cleanse, slow, mark, interrupt albo command.
- Defensive Upgrade: wiekszy ward, guard, sustain albo damage reduction z tradeoffem.
- Control Upgrade: mocniejsze CC, ale z PvP limitami i Tenacity counterplay.
- Resource Upgrade: lepsze tempo many/staminy/rage/focus/faith/soul.
- Tag Upgrade: dodaje albo wzmacnia tag, np. AoE, DoT, Minion, Trap.
- Conversion Upgrade: zmienia kierunek skilla, np. direct damage w DoT albo summon command w debuff.
- Tradeoff Upgrade: mocny efekt za wiekszy koszt, self-debuff albo dluzszy cooldown.

Zasady:

- Kazdy core skill moze miec 2-3 warianty upgrade.
- Gracz nie powinien moc wziac wszystkich wariantow naraz.
- Mocny upgrade musi miec koszt albo tradeoff.
- Upgrade nie moze usunac glownego counterplayu skilla.
- PvP modifier moze byc inny niz PvE modifier.

## Choice Rules

System musi wymuszac wybory.

Zasady:

- Gracz ma ograniczona liczbe punktow pasywek.
- Gracz ma ograniczona liczbe aktywnych skill upgrades.
- Upgrade z jednej galezi moze blokowac upgrade z innej galezi.
- Level 50 wzmacnia wybrana podklase, nie pozwala wziac drugiej pelnej podklasy.
- Gear moze wspierac wybor, ale nie powinien omijac klasy/mastery.
- Respec jest mozliwy, ale nie powinien pozwalac na instant counter-pick w PvP.

Przyklad:

- Warlock moze isc mocniej w summony albo mocniej w curse/drain.
- Nie powinien miec maksimum summon damage, maksimum curse uptime, maksimum drain sustain i mocnego healingu naraz bez ogromnego kosztu.

## Mastery Nodes

Mastery nodes wynikaja z realnego uzywania stylu gry.

Funkcja:

- nagradzaja praktyke,
- wzmacniaja style bez zamykania ich tylko do klas,
- wspieraja hybrydy z kosztem czasu.

Przyklady:

- One-Handed: lepszy counter follow-up albo mniejszy koszt melee skilli.
- Two-Handed: lepszy guard damage albo cleave.
- Archery: lepszy projectile handling albo mark uptime.
- Daggers: lepszy flank/backstab setup.
- Shield: mocniejszy guard albo block stamina efficiency.
- Destruction: lepsze spell tempo.
- Restoration: lepszy heal/cleanse efficiency.
- Chaos: lepszy curse/drain/summon interaction.
- Stealth: lepszy reduced detection, ale nadal counterowany revealem.
- Alchemy / Poison: lepszy poison/trap uptime, ale nadal counterowany cleanse/resist.

PvP sanity:

- Mastery nie moze usunac reveal, cleanse, interrupt, Tenacity, guard break ani summon killing counterplayu.

## Gear Modifiers

Gear modifiers wzmacniaja tagi i wybory buildowe.

Zasady:

- Itemy wzmacniaja tagi, nie same nazwy klas.
- Gear moze zmieniac build, ale nie powinien zastepowac klasy/podklasy/mastery.
- Unique itemy moga zmieniac skill behavior, ale musza miec tradeoff.
- PvP soft capy nadal obowiazuja.

Przyklady:

- +% Minion Damage wzmacnia Wladce Bestii i Warlocka, ale inaczej przez role summonow.
- +% Curse Effect wzmacnia Rytualiste / Warlocka i czesc Inkwizytora.
- +% Trap Damage wzmacnia Sabotazyste i Tropiciela.
- +% Ward wzmacnia Maga i Akolite defensive variants.
- +% Fire Damage wzmacnia Piromante i Inkwizytora.

## Class Examples

### Wojownik

Glowne wybory:

- Guard path: wiecej block/guard/ally protection.
- Rage path: wiecej burst/guard break, wieksze ryzyko.
- Counter path: wiecej parry/riposte/timing reward.

Przykladowe passive/upgrades:

- Cleaving Strike moze isc w wiekszy cleave albo single-target pressure.
- Guard Brace moze isc w ally protection albo personal guard.
- Breaker Blow moze isc w guard break albo stagger.
- Battle Momentum moze isc w stamina sustain albo rage generation.

PvP sanity:

- tank path ma nizszy kill pressure,
- burst path ma punish window,
- counter path wymaga timingu.

### Lotrzyk

Glowne wybory:

- Burst/backstab path.
- Poison/trap path.
- Stealth mobility path.

Przykladowe passive/upgrades:

- Quick Cut moze isc w crit albo bleed/poison setup.
- Fade Step moze isc w disengage albo flank setup.
- Venom Edge moze isc w stronger DoT albo anti-sustain.
- Cheap Trick moze isc w interrupt albo minor control.

PvP sanity:

- stealth ma reveal/detection/AoE counterplay,
- poison ma cleanse/resist counterplay,
- burst ma failed engage punish.

### Lowca

Glowne wybory:

- Projectile precision path.
- Tracking/anti-stealth path.
- Companion/trap utility path.

Przykladowe passive/upgrades:

- Focused Shot moze isc w precision albo stable DPS.
- Snare Trap moze isc w slow/control albo utility reveal.
- Tracking Mark moze isc w anti-stealth albo team focus.
- Companion Command moze isc w harass albo utility.

PvP sanity:

- projectile ma line of sight/block/dodge counterplay,
- traps sa wykrywalne/omijalne,
- companion jest zabijalny i zalezy od ownera.

### Mag

Glowne wybory:

- Fire pressure path.
- Cold control path.
- Lightning burst path.
- Ward/mana path.

Przykladowe passive/upgrades:

- Elemental Bolt moze isc w Fire burn, Cold slow albo Lightning shock.
- Lesser Ward moze isc w stronger burst protection albo mana efficiency.
- Arcane Step moze isc w safer reposition albo offensive window.
- Mana Focus moze isc w faster regen albo lower cost windows.

PvP sanity:

- casty maja interrupt/line of sight counterplay,
- burst ma cooldown i punish window,
- CC ma Tenacity/cleanse counterplay,
- ward peka pod sustained pressure.

### Akolita

Glowne wybory:

- Heal/protection path.
- Judgment/disrupt path.
- Curse/summon/ritual path.

Przykladowe passive/upgrades:

- Minor Mending moze isc w stronger single heal albo ward interaction.
- Purifying Ward moze isc w cleanse albo protection.
- Judgment Spark moze isc w anti-heal albo caster punish.
- Lesser Rite moze isc w debuff albo summon teaser.

PvP sanity:

- heal ma anti-heal/silence/interrupt counterplay,
- judgment ma cleanse/kite counterplay,
- summon/curse ma kill summon/cleanse/owner pressure counterplay.

## Rytualista / Warlock Special Rules

Warlock musi miec realny summon/curse/drain loop, ale nie moze miec wszystkiego naraz.

Glowne sciezki:

- Summon path: mocniejsze miniony, pact windows, summon utility.
- Curse path: mocniejsze debuffy, curse uptime, anti-sustain.
- Drain path: resource/health drain, attrition, sustain przez presje.

Zasady:

- Summon path nie powinna miec maksymalnego curse uptime.
- Curse path nie powinna miec maksymalnego summon damage.
- Drain path nie powinna robic Warlocka niesmiertelnym.
- Owner zawsze musi byc realnym celem.
- Summony musza byc zabijalne.
- Curse musi miec cleanse/counterplay.
- Ritual/pact windows musza miec interrupt albo punish window.

Level 50 upgrade:

- Demonolog: priorytet summony i pact windows.
- Klatwomistrz: priorytet curse, debuff i drain.

## Cross-System PvP Sanity

Pasywki i upgrade nie moga usuwac counterplayu.

Hard rules:

- Stealth zawsze ma reveal/detection/AoE/tracking counterplay.
- Healing zawsze ma anti-heal/silence/interrupt/pressure counterplay.
- Summons zawsze sa limitowane, zabijalne i zalezne od ownera.
- CC zawsze ma Tenacity, cleanse, diminishing returns albo movement counterplay.
- Burst zawsze ma setup, cooldown albo punish window.
- Tankiness zawsze ma kontry: guard break, flank, DoT, magic pressure albo resource pressure.
- Mobility zawsze ma cooldown i nie resetuje kazdej walki bez ryzyka.
- Ward zawsze moze zostac przebity sustained pressure albo resource pressure.

## Test Cases And Scenarios

Skill Trees, Passives & Ability Upgrades v0.0.1 powinno przejsc ponizsze scenariusze:

- Gracz nie moze wziac wszystkich najlepszych pasywek naraz.
- Level 50 wzmacnia wybrana podklase, ale nie daje drugiej pelnej podklasy.
- Warlock moze isc bardziej w summony albo klatwy, ale nie maksimum obu bez kosztu.
- Warlock drain nie robi go niesmiertelnym w PvP.
- Healer nie staje sie niesmiertelny przez pasywki.
- Stealth build nadal ma reveal/detection counterplay.
- Burst build nadal ma punish window.
- Tank nadal ma nizszy kill pressure albo kontry typu guard break/DoT/flank.
- Mag z ward upgrade nadal przegrywa pod sustained pressure albo mana pressure.
- Wladca Bestii i Warlock moga oba korzystac z minion modifiers, ale maja inny gameplay.
- Itemy wzmacniaja wybrane tagi, ale nie omijaja wymagan klasy/mastery.
- Respec pozwala poprawic build, ale nie pozwala na instant PvP counter-pick.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/class-subclass-foundation-v0.0.1.md.
- docs/starter-skills-class-progression-foundation-v0.0.1.md.
- docs/class-skill-kits-foundation-v0.0.1.md.

W szczegolnosci:

- Skill upgrades korzystaja z istniejacych skill tags.
- Damage types zostaja bez zmian.
- Pasywki nie moga usuwac PvP counterplayu.
- Gear wzmacnia tagi i buildy, nie omija progresji.
- Level model zostaje 1-10 / 10 / 25 / 50.

## Assumptions

- Dokument jest projektowy, bez plugin configow.
- Nie wpisujemy finalnych liczb balansu.
- Pelne drzewka punkt po punkcie beda osobnym etapem.
- Ten dokument definiuje zasady i typy wyborow, nie kompletna liste node'ow.
- `class-skill-kits-foundation-v0.0.1.md` jest baza dla listy skilli.
