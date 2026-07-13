# Starter Skills & Class Progression Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje, jak gracz przechodzi od startowych skilli do wyboru klasy, podklasy i pozniejszego upgrade podklasy.

Korekta modelu: level 1-10 nie jest gole neutralne intro i nie jest zestawem mini-klas. To faza testerow stylu gry. Gracz dostaje uproszczone mechaniki, ktore pozwalaja sprawdzic, czy lubi melee, aktywna obrone, strzelanie, mobilnosc, stealth, magie, support, debuffy, summony albo pulapki.

System ma wspierac:

- wybor klasy na level 10,
- wybor podklasy na level 25,
- upgrade wybranej podklasy na level 50,
- starter skille jako testery gameplayu,
- wstepne mastery przez uzywanie starterow,
- sugestie klasy na podstawie stylu gry,
- brak chaosu na hotbarze,
- brak dominujacych starter skilli w PvP,
- logiczna droge do summoner/warlocka pod Akolita.

To nadal etap projektowy. Nie wdrazamy konfiguracji pluginow, konkretnych liczb ani finalnych cooldownow.

## Core Idea

Starter skill nie jest pelnym skillem klasy. Starter skill jest prototypem fundamentalnej decyzji gameplayowej.

Pytania, na ktore startery maja odpowiedziec graczowi:

- Czy lubie walczyc blisko celu?
- Czy lubie blokowac, parowac i kontrowac?
- Czy lubie grac dystansem i aimem?
- Czy lubie pozycjonowanie, dash i uniki?
- Czy lubie skradanie i flankowanie?
- Czy lubie castowac spelle?
- Czy lubie chronic i leczyc?
- Czy lubie oslabianie celu debuffami?
- Czy lubie zarzadzac summonem?
- Czy lubie przygotowac teren pulapkami?

Zasada: najpierw funkcja gameplayowa, potem nazwa fantasy.

## Level Progression

### Level 1-10: Starter Phase

Gracz testuje style gry.

Zasady:

- Gracz ma dostep do kilku starter archetype skills.
- Gracz nie powinien miec wszystkich aktywnych naraz.
- Hotbar powinien wymuszac wybor, np. 4-5 slotowanych starter skilli.
- Uzywanie starterow daje wstepne mastery albo progres kierunku.
- System moze na levelu 10 zasugerowac klase na podstawie najczesciej uzywanych starterow.
- Sugestia nie zmusza do wyboru.

Starter phase ma uczyc:

- basic attack,
- heavy attack,
- block/guard,
- dodge/dash,
- line of sight,
- resource cost,
- cast timing,
- simple counterplay,
- podstaw skilli i tagow.

### Level 10: Base Class Choice

Na levelu 10 gracz wybiera klase bazowa:

- Wojownik.
- Lotrzyk.
- Lowca.
- Mag.
- Akolita.

Efekt wyboru klasy:

- odblokowanie core class skills,
- pierwsza pasywka klasy,
- mocniejsza wersja czesci starterow,
- wyrazny skok identity,
- dostep do klasowych questow/progresji,
- jasny kierunek statow i mastery.

Klasa bazowa nie zamyka wszystkich hybryd, ale najmocniejsze narzedzia wymagaja inwestycji w staty, mastery, gear i pozniejsza podklase.

### Level 25: Subclass Choice

Na levelu 25 gracz wybiera podklase w ramach klasy bazowej.

Efekt wyboru podklasy:

- signature skill,
- pasywka podklasy,
- mocniejsze identity,
- nowe wymagania mastery/stat,
- wyrazny tradeoff,
- bardziej konkretna rola PvE/PvP.

Przyklady:

- Akolita wybiera Kapelana, Inkwizytora albo Rytualiste / Warlocka.
- Mag wybiera Piromante, Kriomante albo Burzomante.
- Lotrzyk wybiera Zabojce, Sabotazyste albo Cienia.

### Level 50: Subclass Upgrade

Na levelu 50 gracz rozwija wybrana podklase.

Zasady:

- To nie jest druga pelna podklasa.
- To upgrade aktualnej podklasy.
- Upgrade daje endgame identity.
- Upgrade nie usuwa counterplayu.
- Upgrade powinien wymagac levelu, mastery, questa/proby i contentu.

Przyklad:

- Rytualista / Warlock moze wybrac upgrade typu Demonolog albo Klatwomistrz.
- Kapelan moze wybrac upgrade typu Hierophant albo Saint.
- Berserker moze wybrac upgrade typu Ravager albo Bloodrager.

## Starter Skill Ideology

Kazdy starter skill musi spelniac zasady:

- testuje jeden fundamentalny styl gry,
- mapuje sie na kilka przyszlych klas albo podklas,
- ma niski scaling,
- ma prosty counterplay,
- nie zastapi klasy po level 10,
- nie zastapi podklasy po level 25,
- nie jest wymagany do jednej konkretnej sciezki,
- moze zostac pozniej zastapiony mocniejsza wersja klasowa.

Nie dobieramy starterow po nazwach klas. Dobieramy je po sposobach grania, ktore gracz ma przetestowac przed wyborem klasy.

## Starter Archetype Skills

Ponizsze nazwy sa robocze. Najwazniejsza jest funkcja gameplayowa.

### Melee Tester

Funkcja:

- testuje close combat,
- uczy ryzyka wejscia w melee,
- uczy timing attack/heavy attack,
- pokazuje roznice miedzy zwyklym atakiem a skillem.

Roboczy koncept:

- mocniejszy cios blisko celu,
- Physical damage,
- krotki cooldown,
- lepszy, jesli gracz trafi po bledzie przeciwnika.

Mapowanie:

- Wojownik,
- Lotrzyk,
- Gladiator,
- Berserker,
- Inkwizytor.

Counterplay:

- block,
- dodge,
- parry,
- kite,
- nie dac wejsc w melee.

### Defense Tester

Funkcja:

- testuje aktywna obrone,
- uczy block/guard/parry window,
- pokazuje, ze defensywa kosztuje zasob albo timing.

Roboczy koncept:

- krotkie defensywne okno block/guard,
- moze obnizyc incoming damage,
- moze dac maly counter bonus po dobrym timingu.

Mapowanie:

- Wojownik,
- Straznik,
- Gladiator,
- Kapelan,
- defensive Akolita,
- tanky hybrids.

Counterplay:

- guard break,
- flank,
- DoT,
- magic pressure,
- bait defensywy.

### Projectile Tester

Funkcja:

- testuje range,
- uczy aim,
- uczy line of sight,
- pokazuje, ze projectile moze byc unikniety albo zablokowany.

Roboczy koncept:

- prosty strzal albo rzut,
- Physical damage,
- wymaga line of sight.

Mapowanie:

- Lowca,
- Strzelec,
- Tropiciel,
- Lotrzyk hybrid,
- projectile item builds.

Counterplay:

- line of sight,
- block,
- dodge,
- terrain,
- gap close.

### Mobility Tester

Funkcja:

- testuje dash/unik,
- uczy pozycjonowania,
- pokazuje wartosc cooldownu movementu.

Roboczy koncept:

- krotki dash albo unik,
- bez wysokiego damage,
- kosztuje stamina/focus,
- ma cooldown.

Mapowanie:

- Lotrzyk,
- Cien,
- Gladiator,
- Lowca,
- Burzomanta,
- mobile hybrids.

Counterplay:

- root/slow z diminishing returns,
- przewidzenie kierunku,
- pressure po cooldownie,
- terrain.

### Stealth Tester

Funkcja:

- testuje skradanie,
- uczy flankowania,
- pokazuje detection/reveal,
- nie daje pelnego assassin burstu.

Roboczy koncept:

- bardzo krotki reduced detection albo vanish,
- bez mocnego damage bonusu,
- przerwany przez obrazenia, reveal albo glosne akcje.

Mapowanie:

- Lotrzyk,
- Zabojca,
- Cien,
- Tropiciel,
- scout builds.

Counterplay:

- reveal,
- detection,
- AoE,
- tracking,
- combat timer,
- dobre pozycjonowanie.

### Spell Tester

Funkcja:

- testuje castowanie,
- uczy mana cost,
- uczy cast/windup,
- pokazuje roznice miedzy ranged physical a spell.

Roboczy koncept:

- prosty elemental spell,
- moze miec wariant Fire, Cold albo Lightning,
- niski burst,
- jasny cast albo projectile travel.

Mapowanie:

- Mag,
- Piromanta,
- Kriomanta,
- Burzomanta,
- Inkwizytor hybrid,
- caster hybrids.

Counterplay:

- interrupt,
- line of sight,
- resist,
- dodge projectile,
- pressure podczas castu.

### Support Tester

Funkcja:

- testuje ochrone, heal albo ward,
- pokazuje, ze support kosztuje zasob,
- uczy decyzji: leczyc siebie, sojusznika albo zachowac zasob.

Roboczy koncept:

- slaby heal albo minor ward,
- niski scaling,
- mocno ograniczony w PvP,
- nie dziala jako immortal button.

Mapowanie:

- Akolita,
- Kapelan,
- defensive Mag,
- support hybrids.

Counterplay:

- anti-heal,
- interrupt,
- silence,
- pressure,
- resource drain.

### Debuff Tester

Funkcja:

- testuje oslabianie celu,
- uczy curse/mark gameplay,
- pokazuje cleanse i debuff uptime.

Roboczy koncept:

- prosta klatwa, mark albo oslabienie,
- moze obnizac sustain, damage albo resist na krotko,
- niski scaling.

Mapowanie:

- Akolita,
- Rytualista / Warlock,
- Inkwizytor,
- Sabotazysta,
- support/control builds.

Counterplay:

- cleanse,
- line of sight,
- interrupt,
- przeczekanie okna,
- pressure na casterze.

### Summon Tester

Funkcja:

- testuje zarzadzanie minionem,
- pokazuje, czy gracz lubi dodatkowa jednostke,
- daje logiczna zapowiedz Rytualisty / Warlocka i Wladcy Bestii.

Roboczy koncept:

- slaby czasowy summon,
- ograniczony damage,
- zabijalny,
- zalezy od pozycji ownera,
- nie wygrywa walki sam.

Mapowanie:

- Akolita,
- Rytualista / Warlock,
- Lowca,
- Wladca Bestii,
- minion/companion builds.

Counterplay:

- zabic summona,
- AoE,
- pressure ownera,
- line of sight,
- interrupt cast/recast.

### Trap Tester

Funkcja:

- testuje setup gameplay,
- uczy kontroli terenu,
- pokazuje roznice miedzy direct damage a przygotowaniem pola.

Roboczy koncept:

- mala pulapka slow/control,
- krotki czas dzialania,
- widoczna albo wykrywalna,
- niski damage.

Mapowanie:

- Lowca,
- Tropiciel,
- Sabotazysta,
- zone-control builds.

Counterplay:

- detection,
- ominiecie,
- cleanse/root break,
- ranged pressure,
- wymuszenie walki przed setupem.

## Starter To Class Mapping

Startery powinny prowadzic do kilku kierunkow.

Mapping:

- Melee Tester: Wojownik, Lotrzyk, Gladiator, Berserker, Inkwizytor.
- Defense Tester: Wojownik, Straznik, Gladiator, Kapelan, defensive hybrids.
- Projectile Tester: Lowca, Strzelec, Tropiciel, Lotrzyk hybrid.
- Mobility Tester: Lotrzyk, Cien, Gladiator, Lowca, Burzomanta.
- Stealth Tester: Lotrzyk, Zabojca, Cien, Tropiciel.
- Spell Tester: Mag, Piromanta, Kriomanta, Burzomanta, Inkwizytor hybrid.
- Support Tester: Akolita, Kapelan, defensive Mag, support hybrid.
- Debuff Tester: Akolita, Rytualista / Warlock, Inkwizytor, Sabotazysta.
- Summon Tester: Akolita, Rytualista / Warlock, Lowca, Wladca Bestii.
- Trap Tester: Lowca, Tropiciel, Sabotazysta, control builds.

## Class Suggestion At Level 10

Na levelu 10 system moze zasugerowac klase na podstawie uzywanych starterow.

Przyklady:

- Duze uzycie Melee + Defense: sugeruj Wojownika.
- Duze uzycie Mobility + Stealth + Melee: sugeruj Lotrzyka.
- Duze uzycie Projectile + Trap + Mobility: sugeruj Lowce.
- Duze uzycie Spell + Support/Defense: sugeruj Maga albo Akolite zalezne od proporcji.
- Duze uzycie Support + Debuff + Summon: sugeruj Akolite.

Zasady:

- Sugestia nie blokuje wyboru.
- Gracz moze wybrac dowolna klase.
- Wstepne mastery moze ulatwic start w sugerowanej klasie.
- Nie wolno karac gracza za eksperymentowanie.

## Starter Replacement Rules

Po wyborze klasy startery moga:

- zostac jako basic utility,
- zostac zastapione mocniejsza wersja klasowa,
- przestac skalowac sie sensownie poza early game,
- stac sie wymaganiem do pozniejszego unlocku,
- zostac przypisane do weapon skill albo generic skill.

Przyklady:

- Defense Tester moze przejsc w mocniejszy Wojownik block/counter tool.
- Spell Tester moze przejsc w elemental spell Maga.
- Support Tester moze przejsc w heal/ward Akolity.
- Summon Tester moze przejsc w Rytualista / Warlock summon system albo Wladca Bestii companion path.
- Trap Tester moze przejsc w Sabotazysta albo Tropiciel trap kit.

Zasada: starter nie powinien byc najlepsza wersja skilla po level 10. Ma byc fundamentem, nie endgame narzedziem.

## Level 50 Upgrade Examples

Robocze przyklady upgrade wybranej podklasy:

- Straznik: Bastion albo Protector.
- Berserker: Ravager albo Bloodrager.
- Gladiator: Duelmaster albo Blademaster.
- Zabojca: Executioner albo Nightblade.
- Sabotazysta: Plague Engineer albo Trapmaster.
- Cien: Phantom albo Shadowrunner.
- Strzelec: Deadeye albo Marksman.
- Tropiciel: Pathfinder albo Stalker.
- Wladca Bestii: Packlord albo Primal Caller.
- Piromanta: Infernalist albo Ashcaller.
- Kriomanta: Frostbinder albo Cryoguard.
- Burzomanta: Stormcaller albo Tempest.
- Kapelan: Hierophant albo Saint.
- Inkwizytor: Justicar albo Witch Hunter.
- Rytualista / Warlock: Demonolog albo Klatwomistrz.

Te nazwy sa robocze. Na tym etapie wazna jest funkcja: level 50 rozwija wybrana podklase, nie dodaje drugiej pelnej podklasy.

## Balance Rules

Starter balance:

- starter skille maja niski scaling,
- starter skille maja proste wymagania,
- starter skille maja jasny counterplay,
- starter skille nie powinny dominowac po level 10,
- starter skille nie powinny dawac pelnej fantazji podklasy.

Specjalne limity:

- Starter summon jest czasowy, slaby, limitowany i zabijalny.
- Starter heal/ward pomaga w PvE, ale nie robi immortala.
- Starter stealth jest krotki, latwy do reveal i bez pelnego backstab burstu.
- Starter CC/trap ma krotki czas dzialania i jasny counterplay.
- Starter spell nie ma wysokiego burstu ani mocnego AoE.
- Starter projectile wymaga line of sight i moze byc zablokowany albo unikniety.
- Starter mobility ma cooldown i nie moze resetowac calej walki.
- Starter debuff ma krotki czas i cleanse/counterplay.

Progression feel:

- Po wyborze klasy gracz powinien czuc wyrazny skok identity.
- Po wyborze podklasy gracz powinien czuc prawdziwa specjalizacje.
- Po level 50 upgrade gracz powinien dostac endgame identity.
- Kazdy skok mocy musi zachowac counterplay.

## Test Cases And Scenarios

Starter Skills & Class Progression v0.0.1 powinno przejsc ponizsze scenariusze:

- Nowy gracz moze przetestowac summon zanim wybierze Akolite albo Lowce.
- Nowy gracz moze przetestowac spell zanim wybierze Maga.
- Nowy gracz moze przetestowac stealth/mobility zanim wybierze Lotrzyka.
- Nowy gracz moze przetestowac block/guard zanim wybierze Wojownika.
- Nowy gracz moze przetestowac support/debuff zanim wybierze Akolite.
- Startery nie zalewaja hotbara, bo gracz slotuje ograniczona liczbe.
- System moze zasugerowac klase na levelu 10 na bazie uzywanych starterow.
- Wybor klasy na levelu 10 daje wyrazny skok mocy i identity.
- Wybor podklasy na levelu 25 daje signature gameplay.
- Upgrade na levelu 50 rozwija wybrana podklase, nie dodaje druga pelna podklase.
- Akolita ma logiczna droge do Rytualisty / Warlocka, bo summon/debuff istnieje jako tester.
- Wladca Bestii ma logiczna droge z summon/companion testera, ale rozni sie od Warlocka rola i scalingiem.
- Starter summon, stealth, heal i trap nie dominuja early PvP.
- Gracz eksperymentujacy roznymi starterami nie jest karany zlym permanentnym wyborem.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/class-subclass-foundation-v0.0.1.md.

W szczegolnosci:

- Startery musza korzystac z istniejacych tagow skilli.
- Startery nie dodaja nowych damage types.
- Startery musza miec PvP counterplay.
- Summon tester musi respektowac summon PvP rules.
- Healing/ward tester musi respektowac PvP healing/protection limits.
- Level 10/25/50 model powinien pozniej zaktualizowac progression i class foundation.

## Assumptions

- Level wyboru klasy: 10.
- Level wyboru podklasy: 25.
- Level upgrade podklasy: 50.
- Level 50 to upgrade wybranej podklasy, nie druga pelna podklasa.
- Starter phase ma byc testem stylow gry, nie pelnym tutorialem kazdej podklasy.
- Konkretne nazwy starter skilli moga zostac zmienione pozniej po ustaleniu lore i UI.
