# Damage & Defense Foundation v0.0.1

## Cel dokumentu

Ten dokument ustala fundament obrazen, defensyw, statusow, statow z levelowania
i itemizacji dla serwera MCMMORPG. To warstwa pomiedzy Combat Foundation
v0.0.1 a pozniejszym projektowaniem klas bazowych i podklas.

Celem jest system PoE-like, ale prostszy i czytelny pod Minecraft PvE/PvP. Po
tym etapie ma byc jasne, czym postacie bija, co je kontruje, jakie staty rozwija
gracz, jakie itemy maja sens i jak projektowac klasy bez dokladania losowych
typow obrazen.

## Core Damage Types

Startowy zestaw typow obrazen:

- Physical - miecze, topory, luki, sztylety, czesc pulapek, bleed.
- Fire - ogien, podpalenia, eksplozje, swiety ogien Inkwizytora.
- Cold - mroz, spowolnienia, zamrozenia, kontrola ruchu.
- Lightning - szybki burst, shock, chain effects, krotkie okna wysokiego damage.
- Chaos - nekromancja, klatwy, mroczna magia, obrazenia trudniejsze do blokowania.
- Poison - trucizny, jady, stacking DoT, dlugie walki.

Na tym etapie nie dodawac osobnych typow typu Holy, Psychic, Illusion albo
Arcane. Jesli jakas klasa ma taki klimat, jej skille musza wpadac w jeden z
powyzszych typow. Przyklady:

- Inkwizytor uzywa Fire + Physical.
- Nekromanta uzywa Chaos.
- Mag uzywa Fire, Cold albo Lightning.
- Lotrzyk uzywa Physical i Poison.

## Defensive Layers

Glowne warstwy defensywne:

- Armor - redukuje glownie Physical.
- Evasion - pozwala unikac atakow fizycznych/projektili albo zmniejsza ich skutecznosc.
- Ward - magiczna oslona przeciw spell burstowi i czesci magicznych obrazen.
- Block - aktywna defensywa z przodu, glownie przeciw Physical i projectile.
- Resistances - procentowa redukcja obrazen elemental/chaos/poison.
- Tenacity - skraca CC w PvP.
- Poise - odpornosc na stagger/interrupt.
- Guard - zasob bloku, ktory mozna przelamac przez heavy attack/guard break.

Startowy zestaw resistow:

- Fire Resistance.
- Cold Resistance.
- Lightning Resistance.
- Chaos Resistance.
- Poison Resistance.

Physical nie dostaje klasycznego resistu. Jego glowna kontra jest Armor, Block,
Evasion, pozycjonowanie, parry i guard break.

## Status Effects

Statusy musza byc przypisane do typow obrazen i miec counterplay.

Proponowane statusy:

- Bleed - Physical DoT; moze byc mocniejszy przeciw celom w ruchu albo slabiej opancerzonym.
- Burn - Fire DoT; stabilne obrazenia w czasie.
- Chill - Cold; spowalnia ruch i/lub attack speed.
- Freeze - Cold; bardzo krotka, silna kontrola z duzymi ograniczeniami w PvP.
- Shock - Lightning; cel otrzymuje zwiekszone obrazenia przez krotki czas.
- Poison - Poison DoT; moze stackowac sie w dluzszych walkach.
- Curse - Chaos; oslabienie celu, np. mniej healingu, mniej resistu albo wiekszy koszt zasobow.
- Stagger - physical/guard effect; krotkie przerwanie rytmu walki.

Kazdy status musi miec przynajmniej jedna kontre:

- resist,
- cleanse,
- tenacity,
- poise,
- movement,
- block/parry,
- item affix,
- cooldown,
- diminishing returns.

## Level-Up Stat Points

Po kazdym poziomie gracz dostaje punkty statystyk do rozdania. Te staty wplywaja
na czesc ofensywy, defensywy i zasobow, ale nie zastepuja itemow. Maja budowac
kierunek postaci razem z gearem, skillami i podklasa.

Glowne staty:

- Strength - zwieksza melee physical damage, armor scaling, poise i czesciowo max stamina.
- Dexterity - zwieksza projectile damage, attack speed, crit chance, evasion i stealth.
- Intelligence - zwieksza spell damage, max mana, mana regen i ward scaling.
- Faith - zwieksza healing power, buff/debuff strength, fire/holy-style skille Akolity i czesc cleanse.
- Vitality - zwieksza max health, odpornosc na burst i czesciowo tenacity.

Zasady:

- Co level gracz dostaje okreslona liczbe punktow, np. 3-5.
- Staty maja wplywac na build, ale nie moga same robic calej postaci.
- Itemy nadal sa glownym zrodlem specjalizacji, affixow i resistow.
- Klasa moze miec naturalna preferencje statow, ale nie powinna calkowicie blokowac hybryd.
- Stat points powinny miec respec, ale z kosztem.
- Niektore itemy/skille moga miec wymagania statow, np. ciezki mlot wymaga Strength, mocny kostur Intelligence, relikwia Faith.

Docelowy build sklada sie z trzech warstw:

    staty z levelowania + itemy + skille/podklasa

## Item Stats

Pierwsza lista statow, ktore moga pojawiac sie na itemach.

Podstawowe staty:

- Max Health.
- Max Mana.
- Max Stamina.
- Health Regen.
- Mana Regen.
- Stamina Regen.

Ofensywne staty:

- Physical Damage.
- Spell Damage.
- Attack Power.
- Ability Power.
- Crit Chance.
- Crit Damage.
- Attack Speed.
- Cast Speed.
- Cooldown Reduction.
- Damage over Time.
- Projectile Damage.
- Melee Damage.
- Area Damage.

Defensywne staty:

- Armor.
- Evasion.
- Ward.
- Block Strength.
- Guard.
- Poise.
- Tenacity.
- Fire Resistance.
- Cold Resistance.
- Lightning Resistance.
- Chaos Resistance.
- Poison Resistance.

Specjalne staty:

- Armor Penetration.
- Elemental Penetration.
- Chaos Penetration.
- Poison Penetration.
- Healing Power.
- Anti-Heal.
- Cleanse Strength.
- Stealth.
- Detection.
- Movement Speed.

## Scaling Rules

Podstawowe zasady skalowania obrazen:

- Bron daje bazowy damage dla atakow fizycznych.
- Spelle maja bazowy damage z poziomu skilla.
- Stat glowny wzmacnia archetyp, ale nie jest jedynym zrodlem sily.
- Itemy wzmacniaja tagi skilli, nie tylko klasy.
- Skille moga miec tagi typu Melee, Projectile, Spell, AoE, DoT, Minion, Fire, Cold, Lightning, Chaos, Poison, Physical.

Przyklady skalowania:

- Sztyletowy backstab skaluje sie z Physical Damage, Crit Damage, Melee Damage, Stealth i czesciowo Dexterity.
- Fireball skaluje sie z Spell Damage, Fire Damage, Cast Speed, Elemental Penetration i czesciowo Intelligence.
- Poison trap skaluje sie z Poison Damage, DoT, Area Damage i czesciowo Dexterity.
- Ward spell skaluje sie z Ward, Ability Power, Mana i czesciowo Intelligence.
- Heal skaluje sie z Healing Power, Faith i czasem Ability Power.

## PvP Balance Rules

Osobne zasady pod PvP:

- Resisty maja cap, zeby nie dalo sie byc odpornym na caly typ obrazen.
- Penetracja nie moze calkowicie ignorowac defensywy.
- Healing w PvP ma obnizona skutecznosc.
- Poison/DoT nie moze stackowac sie bez limitu w PvP.
- Freeze/stun maja bardzo krotkie czasy i diminishing returns.
- Ward nie moze byc drugim pelnym paskiem HP bez counterplayu.
- Armor nie moze robic melee tanka niesmiertelnym.
- Evasion nie moze dawac losowej nietykalnosci bez kontr.
- Stealth/backstab musi miec counterplay przez detection, reveal, AoE i pozycjonowanie.
- Staty z levelowania nie moga tworzyc niegrywalnych roznic w PvP bez udzialu gearu i skilli.

## Test Cases And Scenarios

Kazdy typ obrazen i defensywa powinny przejsc testy projektowe:

- Physical vs Armor - ciezki wojownik powinien redukowac duzo obrazen fizycznych, ale byc slabszy przeciw magii/chaos.
- Fire vs Fire Resistance - ognisty caster powinien miec czytelna kontre przez resist, ale nadal dzialac przez penetracje.
- Poison vs Cleanse - poison build powinien wygrywac dlugie walki, ale przegrywac z dobrym cleanse/poison resist.
- Ward vs Burst - ward powinien chronic przed burstem, ale pekac pod presja.
- Evasion vs AoE - evasion powinno pomagac na projectile/melee, ale nie calkowicie negowac AoE.
- Backstab vs Detection - stealth opener ma byc mocny, ale mozliwy do skontrowania.
- CC vs Tenacity - gracz z tenacity powinien odczuwac CC, ale nie byc perma-kontrolowany.
- Stat build vs item build - gracz inwestujacy punkty w staty ma czuc roznice, ale nie moze ignorowac gearu.
- PvP low gear vs high gear - roznica gearu daje przewage, ale nie powinna oznaczac natychmiastowego one-shota.

## Powiazania z Combat Foundation

Ten dokument musi pozostac spojny z docs/combat-foundation-v0.0.1.md.

W szczegolnosci:

- Dodge, Block, Parry i Counter korzystaja z tych defensyw oraz zasobow.
- Stealth, Backstab, Noise i Detection korzystaja ze statow Dexterity, Stealth i Detection.
- Ward, Mana, Interrupt i Silence sa podstawa balansu casterow.
- Tenacity, Poise i diminishing returns sa podstawa balansu CC w PvP.
- Armor Weight powinien wplywac na Armor, Evasion, Ward, Stealth, Dash i Poise.

Nie wdrazac jeszcze konfiguracji pluginow ani klas. To nadal etap projektowy.

## Assumptions

- System ma byc PoE-like, ale mniej skomplikowany niz pelne PoE.
- PvP jest kluczowe, wiec kazda defensywa i kazdy status musi miec counterplay.
- Nie dodajemy dziwnych typow obrazen tylko dla klimatu klasy.
- Itemy maja wspierac buildy przez tagi skilli, typy obrazen i defensywy.
- Staty z levelowania sa czescia buildu, ale nie zastepuja itemizacji.
- Klasy bazowe projektujemy dopiero po zaakceptowaniu tego fundamentu.
