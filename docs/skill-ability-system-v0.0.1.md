# Skill & Ability System v0.0.1

## Cel dokumentu

Ten dokument definiuje, jak gracz zdobywa, rozwija, slotuje i uzywa skilli na
serwerze MCMMORPG. System ma laczyc Skyrimowe rozwijanie przez uzywanie z
PoE-like tagami, scalingiem i buildami.

Skille sa lacznikiem pomiedzy combat systemem, statami, itemami i pozniejszymi
klasami. Celem jest unikniecie sytuacji, w ktorej klasy sa tylko nazwami bez
konkretnego gameplayu.

## Skill Structure

Kazdy skill powinien miec staly zestaw danych projektowych:

- Name - nazwa skilla.
- Type - active, passive, toggle, aura, summon, movement, ultimate/signature.
- Tags - np. Melee, Projectile, Spell, AoE, DoT, Fire, Physical, Poison.
- Damage Type - jeden lub kilka typow obrazen z Damage & Defense Foundation.
- Resource Cost - stamina, mana, rage, focus, faith albo brak kosztu.
- Cooldown - czas odnowienia.
- Cast Time albo Windup - jesli skill ma byc mozliwy do przerwania.
- Range - melee, short, medium, long.
- Targeting - self, target, cone, line, projectile, ground AoE, aura.
- Scaling - z czego skill rosnie: bron, staty, itemy, tagi.
- Counterplay - jak przeciwnik moze na niego zareagowac.
- PvP Modifier - jesli skill wymaga osobnych zasad w PvP.

To ma byc standard dla kazdego przyszlego skilla.

## Skill Bar

Startowy model skill bara:

- Basic Attack - zalezny od broni.
- Heavy Attack - zalezny od broni, mocniejszy i wolniejszy.
- Defensive Action - block/parry/dodge zaleznie od buildu.
- 3 Active Skills - glowne skille rotacji.
- 1 Movement/Utility Skill - dash, blink, roll, charge, disengage.
- 1 Signature Skill - mocny skill z dluzszym cooldownem.

Gracz ma miec ograniczona liczbe aktywnych skilli, zeby build mial wybory i
slabosci. Nie powinien miec dostepu do calego zestawu naraz.

## Skill Sources

Skille moga pochodzic z kilku zrodel:

- Weapon Skills - wynikaja z uzywanej broni, np. miecz, topor, luk, sztylet, kostur.
- Class Skills - wynikaja z klasy bazowej.
- Subclass Skills - odblokowane po wyborze specjalizacji.
- Magic School Skills - np. Fire, Cold, Lightning, Chaos, Healing.
- Utility Skills - stealth, reveal, cleanse, tracking, movement.
- Item Skills - rzadkie skille nadawane przez unikalne itemy.

Na start projektowo dopuszczamy wszystkie zrodla, ale implementacyjnie
najbezpieczniej zaczac od Weapon Skills + Class Skills, a reszte traktowac jako
pozniejsze rozszerzenia.

## Skill Progression

System ma laczyc PoE i Skyrim:

- Skill moze levelowac sie przez uzywanie.
- Uzywanie typu skilla rozwija powiazane mastery.
- Level postaci daje punkty statystyk.
- Klasa/podklasa odblokowuje dostep do nowych skilli.
- Itemy wzmacniaja tagi i scaling, ale nie zastepuja progresji.

Przyklady mastery:

- One-Handed.
- Two-Handed.
- Archery.
- Daggers.
- Destruction.
- Restoration.
- Chaos.
- Stealth.
- Shield.
- Alchemy/Poison.

Mastery nie musi byc klasa. To osobna warstwa rozwoju przez uzywanie.

## Skill Tags

Tagi sa kluczowe, bo pozniej itemy beda je wzmacniac.

Podstawowe tagi mechaniczne:

- Melee.
- Projectile.
- Spell.
- AoE.
- DoT.
- Minion.
- Trap.
- Aura.
- Movement.
- Defensive.
- Control.
- Heal.
- Curse.
- Stealth.

Tagi obrazen:

- Physical.
- Fire.
- Cold.
- Lightning.
- Chaos.
- Poison.

Tagi powinny byc uzywane konsekwentnie. Przyklad: poison trap moze miec tagi
Trap, AoE, DoT i Poison.

## Resource Model

Kazdy skill powinien zuzywac zasob zgodny ze stylem gry:

- Stamina - melee, block, dash, heavy attack, physical mobility.
- Mana - spell damage, ward, teleport, magic utility.
- Rage - agresywne melee skille, glownie przyszly Berserker.
- Focus - precyzyjne strzaly, stealth, backstab, traps.
- Faith - heal, cleanse, judgement, Inkwizytor/Akolita.

Resource cost ma wymuszac decyzje, a nie tylko byc formalnoscia. Gracz nie
powinien moc spamowac najmocniejszych skilli bez ryzyka.

## Cooldown And Timing Rules

Skille powinny miec czytelne okna reakcji.

Kategorie timingowe:

- Instant - szybkie, slabsze, trudniejsze do przerwania.
- Short Windup - mocniejsze, ale mozliwe do przewidzenia.
- Cast Time - silne spelle, mozliwe do interruptu.
- Channel - mocny efekt przez czas trwania, ryzykowny w PvP.
- Long Cooldown - signature/ultimate, bardzo mocne, ale rzadkie.

Zasady:

- Im mocniejszy skill, tym wiekszy tell, windup, koszt albo cooldown.
- Burst musi miec counterplay.
- CC musi miec cooldown albo warunek trafienia.
- Movement skille nie moga byc spamowane bez ograniczen.
- Instant skille nie powinny miec najwyzszego burstu.

## PvP Counterplay Rules

Kazdy skill musi miec jasny counterplay.

Przyklady:

- Projectile mozna ominac, zablokowac albo przerwac linie wzroku.
- Cast mozna przerwac interruptem albo presja melee.
- AoE mozna opuscic, jesli ma widoczny obszar.
- DoT mozna oczyscic cleanse albo zredukowac resistem.
- Stealth opener mozna skontrowac revealem, detection albo AoE.
- Heal mozna skontrowac anti-healem, silence albo presja na healera.
- Summonera mozna skontrowac zabiciem summonow albo presja na wlasciciela.
- Block mozna skontrowac guard breakiem, flanka albo DoT.

Skill bez counterplayu nie powinien przejsc do implementacji.

## Skill Mixing Rules

Gracz moze mieszac style, ale system musi ograniczac broken buildy.

Zasady:

- Klasa bazowa daje preferowane skille, ale nie musi blokowac wszystkich hybryd.
- Bron moze ograniczac dostep do czesci skilli.
- Niektore skille wymagaja konkretnego stat minimum.
- Niektore skille wymagaja mastery level.
- Najmocniejsze skille moga wymagac klasy albo podklasy.
- Hybrydy sa dozwolone, ale placa cene w statach, itemach albo zasobach.

Przyklad: gracz moze byc Wojownikiem z prostymi spellami utility, ale nie
powinien miec pelnego burstu Maga bez inwestycji w Intelligence, Mana i
odpowiednie itemy.

## Skill Examples For Testing

Testowe skille jako wzorce projektowe:

- Power Slash - Melee, Physical, heavy hit, guard pressure.
- Firebolt - Spell, Fire, projectile, cast/windup, prosty magic damage.
- Poison Trap - Trap, AoE, DoT, Poison, kontrola terenu.
- Ward Pulse - Defensive, Ward, Magic, krotkie zabezpieczenie przeciw burstowi.
- Backstab - Melee, Physical, Stealth, bonus przy trafieniu w plecy.
- Cleanse - Utility, Heal, usuwa Poison/Burn/Curse.
- Reveal - Utility, Detection, kontra na stealth.
- Shield Bash - Melee, Physical, Stagger, interrupt/guard pressure.

Te skille nie musza byc finalnymi skillami klas. Maja sluzyc do sprawdzenia,
czy system tagow, kosztow, cooldownow i counterplayu dziala.

## Test Cases And Scenarios

Kazdy skill powinien przejsc testy projektowe:

- Czy ma jasny typ obrazen albo funkcje?
- Czy ma poprawne tagi?
- Czy wiadomo, z czego sie skaluje?
- Czy koszt zasobu pasuje do stylu gry?
- Czy cooldown/windup pasuje do mocy?
- Czy przeciwnik ma counterplay?
- Czy skill dziala w PvE solo?
- Czy skill nie psuje PvP?
- Czy itemy moga go wzmacniac przez tagi?
- Czy skill nie dubluje innego skilla bez powodu?

Scenariusze balansowe:

- Melee vs Caster - melee musi miec narzedzia presji, caster musi miec ward/kite.
- Stealth vs Detection - opener ma byc mocny, ale nie darmowy.
- Healer vs Anti-Heal - healer ma byc wartosciowy, ale nie niesmiertelny.
- Trap vs Mobility - traper kontroluje teren, ale mobilny gracz moze go outplayowac.
- Burst vs Ward - ward chroni przed burstem, ale peka pod presja.
- CC vs Tenacity - CC dziala, ale nie robi perma-locka.

## Powiazania z fundamentami

Ten dokument musi byc spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/pet-companion-minion-system-foundation-v0.0.1.md.

W szczegolnosci:

- Damage Type musi korzystac tylko z ustalonych typow obrazen.
- Scaling musi korzystac z ustalonych statow, item statow i tagow.
- Skill counterplay musi respektowac Combat Foundation.
- Stealth, reveal, detection, block, parry, ward, interrupt i CC musza dzialac tak samo we wszystkich dokumentach.

Nie wdrazac jeszcze konfiguracji pluginow ani klas. To nadal etap projektowy.

## Assumptions

- Skill system ma laczyc Skyrimowe rozwijanie przez uzywanie z PoE-like tagami.
- Itemy beda wzmacnialy tagi i scaling skilli, dlatego tagi musza byc ustalone przed pelna itemizacja.
- PvP jest kluczowe, wiec kazdy skill musi miec counterplay.
- Hybrydy maja byc mozliwe, ale wymagaja inwestycji w staty, mastery, itemy albo zasoby.
- Klasy bazowe projektujemy dopiero po tym, jak wiadomo, jak dzialaja skille.
