# World & Content Loop Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje, co gracz faktycznie robi na serwerze MCMMORPG: eksploracje, regiony, questy, dungeony, bossy, open-world PvP, frakcje, guild wars, reputacje i endgame.

Celem jest ustalenie petli gry przed finalnym projektowaniem klas. Klasy, skille, itemy i progresja musza miec realne scenariusze uzycia, inaczej beda projektowane w prozni.

System ma wspierac:

- PvE solo.
- PvE party.
- PvP 1v1, male grupy, open world i wojny gildii.
- Rozwoj mastery przez realne aktywnosci.
- Target farming itemow.
- Endgame bez jednej obowiazkowej sciezki.
- Ochrone nowych graczy przed griefem.
- Ryzykowne aktywnosci z lepszymi rewardami.

To nadal etap projektowy. Nie wdrazamy konfiguracji pluginow ani finalnych klas.

## Core Game Loop

Glowna petla gry:

- Gracz startuje w bezpiecznej strefie.
- Uczy sie podstaw walki, skilli, itemow, statow i mastery.
- Robi early questy i rozwija pierwszy styl walki.
- Zdobywa pierwsze sensowne itemy i odblokowania skilli.
- Wchodzi do trudniejszych regionow, dungeonow i bossow.
- Decyduje, czy chce isc bardziej w PvE, PvP, crafting, reputacje albo guild content.
- Odblokowuje subclass / ascendancy przez level, mastery i probe gameplayowa.
- W endgame farmi bossow, dungeony, reputacje, PvP ranking, guild wars i seasonal goals.

Zasada: kazda aktywnosc powinna wspierac przynajmniej jedna z glownych warstw systemu: combat, skill system, itemization, progression, mastery albo PvP.

## Region Types

Swiat powinien byc podzielony na czytelne typy regionow.

### Safe Zone

Safe zone to miejsce startu, handlu, craftingu, respecu i organizacji.

Zasady:

- PvP disabled.
- Brak agresywnych mobow albo tylko tutorialowe.
- NPC od questow, vendorow, respecu, banku, craftingu.
- Mozliwa zmiana loadoutow.
- Brak ryzyka utraty loot bag.

Rola:

- Chroni nowych graczy.
- Jest hubem spolecznosci.
- Daje miejsce na przygotowanie przed ryzykownymi aktywnosciami.

### PvE Zone

PvE zone to podstawowy obszar progresji.

Zasady:

- PvP disabled albo mocno ograniczone.
- Moby, questy, farming, world events PvE.
- Dropy powiazane z regionem.
- Dobre miejsce do rozwoju mastery.

Rola:

- Glowna sciezka dla casual i solo graczy.
- Bezpieczne uczenie systemu.
- Progress bez wymuszania PvP.

### Contested Zone

Contested zone to region wyzszego ryzyka i lepszych rewardow.

Zasady:

- PvP enabled warunkowo albo w okreslonych obszarach.
- Lepsze resource nodes, rare mobs, eventy i dropy.
- Combat timer aktywny.
- Anti-grief rules musza chronic duze roznice leveli albo spawn camp.

Rola:

- Most miedzy PvE i PvP.
- Ryzyko za lepszy loot.
- Miejsce na male starcia, zasadzki, escorty i resource fights.

### PvP Zone

PvP zone to region jawnej walki gracz kontra gracz.

Zasady:

- PvP enabled.
- Jasne ostrzezenie przed wejsciem.
- Lepsze rewardy niz bezpieczne regiony.
- Loot risk zalezy od aktywnosci.
- Combat timer i anti-logout.
- Safe exits ograniczone, zeby nie abuseowac ucieczek.

Rola:

- Miejsce dla graczy szukajacych walki.
- Zrodlo PvP currency, reputacji albo ranking progression.
- Test realnego balansu klas, itemow i counterplayu.

### Dungeon Zone

Dungeon zone to instancjonowana albo pol-instancjonowana aktywnosc PvE.

Zasady:

- Jasny party size.
- Trudniejsze moby i bossy.
- Lepszy loot niz open world PvE.
- Death penalty wieksza niz casual PvE.
- Lockout albo reset rules.
- PvP domyslnie disabled, chyba ze dungeon jest specjalnie oznaczony jako PvPvE.

Rola:

- Glowny test buildow PvE.
- Zrodlo target farmingu.
- Miejsce na party synergy.

### Boss Arena

Boss arena to walka z konkretnym bossem.

Zasady:

- Telegraphed attacks.
- Fazy walki.
- Czytelne resist profile.
- Mechaniki wymagajace ruchu, blocku, parry, interruptu, cleanse albo target swapu.
- Target farming unikalnych dropow.

Rola:

- Sprawdza combat i buildy.
- Daje mocne, ale kontrolowane rewardy.
- Wymusza counterplay i przygotowanie.

### Guild Territory

Guild territory to region powiazany z kontrola gildii.

Zasady:

- Walka o objective, nie tylko zabijanie.
- Siege windows zamiast permanentnego griefu.
- Defender advantage, ale nie nie do przejscia.
- Rewardy dla gildii, ale z limitami snowballu.
- Solo gracze nie powinni byc calkowicie odcieci od podstawowej progresji.

Rola:

- Endgame grupowy.
- Powod do polityki, konfliktow i ekonomii.
- Naturalne miejsce dla PvP buildow, scoutow, tankow, healerow i siege roles.

## Quest System

Questy nie powinny byc tylko lista zabij 10 mobow. Maja prowadzic gracza przez systemy.

Typy questow:

- Tutorial quest.
- Progression quest.
- Story / region quest.
- Daily / weekly quest.
- Reputation quest.
- Dungeon unlock quest.
- Subclass trial.
- PvP objective quest.
- Guild quest.
- Event quest.

Zasady:

- Tutorial questy ucza walki, blocku, dodge, parry, statow, skilli, itemow i mastery.
- Progression questy odblokowuja regiony, dungeony, vendorow i aktywnosci.
- Subclass trial testuje realny styl gry, nie tylko level.
- Reputation questy buduja relacje z frakcjami.
- Daily/weekly nie powinny byc obowiazkowa praca, tylko dodatkowa motywacja.
- PvP questy nie powinny zmuszac PvE graczy do stania sie darmowym celem.

Przyklady:

- Tutorial block/parry: gracz musi zablokowac i sparowac atak NPC.
- Stealth trial: przejscie obszaru bez wykrycia i wykonanie backstabu na celu.
- Dungeon unlock: pokonanie mini-bossa regionu i zdobycie klucza.
- Reputation quest: obrona karawany albo zbieranie rzadkich materialow.
- PvP quest: przejecie punktu w contested zone, ale z limitem nagrod za farmienie tego samego celu.

## Dungeon Design

Dungeony powinny byc jednym z glownych filarow PvE.

Typy dungeonow:

- Solo dungeon.
- Small party dungeon.
- Full party dungeon.
- Challenge dungeon.
- PvPvE dungeon.
- Keyed / timed dungeon.

Zasady:

- Solo dungeony testuja samowystarczalnosc buildu.
- Party dungeony testuja role, utility, CC, cleanse, tankiness i damage windows.
- Challenge dungeony moga miec modyfikatory i lepsze rewardy.
- PvPvE dungeony musza miec jasne ostrzezenie i osobne rewardy.
- Dungeon powinien miec moby, mini-bossy, boss, reward chest i jasne warunki resetu.
- Loot powinien byc powiazany z regionem, bossem albo typem przeciwnikow.

Death penalty:

- Casual dungeon: durability loss, checkpoint albo czasowy debuff.
- Challenge dungeon: ograniczone revive, slabszy reward po wipe albo reset.
- PvPvE dungeon: mozliwy loot risk, ale bez kasowania calego ekwipunku.

## Boss Design

Bossy musza byc projektowane pod mechaniki walki, nie tylko duze HP.

Kazdy boss powinien miec:

- damage profile,
- resist profile,
- phases,
- telegraphed attacks,
- counterplay,
- punish za bledy,
- okna damage,
- loot identity,
- target farming reason.

Przyklady mechanik:

- Heavy slam do dodge albo blocku.
- Channel spell do interruptu.
- Projectile volley do line of sight albo blocku.
- Poison phase wymagajaca cleanse albo poison resist.
- Shield phase wymagajaca guard break.
- Add phase wymagajaca AoE albo target priority.
- Enrage phase wymagajaca burstu albo defensywy.

Zasady:

- Boss nie powinien karac tylko jednej klasy albo jednego stylu.
- Boss moze premiowac przygotowanie, ale nie wymagac jednego konkretnego buildu.
- Unique drop powinien miec tradeoff i PvP sanity check.
- Boss powinien miec czytelna droge target farmingu.

## Open-World PvP

Open-world PvP jest wazne, ale musi byc kontrolowane.

Zasady:

- PvP nie powinno byc aktywne wszedzie.
- Gracz musi wiedziec, ze wchodzi w ryzykowny region.
- Combat timer blokuje logout abuse, teleport abuse i natychmiastowe loadout swap.
- Anti-grief rules powinny ograniczac spawn camp, low-level farming i farmienie tego samego gracza.
- Rewardy powinny zachecac do walki o cele, nie do losowego zabijania slabszych.

Mozliwe aktywnosci:

- Resource rush.
- Capture point.
- Caravan escort / raid.
- Relic capture.
- Bounty target.
- World boss in contested zone.
- PvPvE dungeon entrance control.

Loot risk:

- Safe/PvE zones: brak PvP loot risk.
- Contested zones: ograniczony risk, np. czesc resource bag.
- PvP zones: wyzszy risk, ale bez utraty calego gearu.
- Guild wars: osobne reguly zalezne od formatu wojny.

## Factions And Reputation

Frakcje i reputacje maja dawac dlugoterminowe cele poza samym levelem.

Reputacja moze dawac:

- vendor unlocks,
- crafting recipes,
- dungeon keys,
- cosmetic rewards,
- faction gear bases,
- region access,
- daily/weekly quests,
- PvP identity,
- unique utility items.

Zasady:

- Reputacja nie powinna dawac obowiazkowego best-in-slot dla kazdego.
- Frakcje moga wspierac style gry, ale nie powinny blokowac podstawowych mechanik.
- Reputacja powinna miec tygodniowy albo dzienny limit, jesli rewardy sa mocne.
- Gracz solo powinien miec sensowna droge reputacji.
- PvP frakcje powinny miec anty-abuse rules dla farmienia altow.

## World Events

World events maja robic swiat zywszy i dawac powody do ruchu po mapie.

Typy eventow:

- Invasion.
- Caravan.
- Resource rush.
- Relic capture.
- Rare elite spawn.
- World boss.
- Faction conflict.
- Corrupted zone.
- Dungeon surge.

Zasady:

- Event powinien miec jasny cel.
- Event powinien miec limit czasu.
- Event powinien miec rewardy proporcjonalne do ryzyka.
- PvP eventy musza byc oznaczone.
- Eventy nie powinny byc jedynym zrodlem kluczowego progressu.
- Eventy powinny wspierac rozne role: damage, tank, healer, scout, controller, crafter/logistics.

## Guild Wars

Guild wars to endgame grupowy.

Zasady:

- Wojny powinny miec siege windows, nie calodobowy grief.
- Walka powinna byc o objective, nie tylko kill count.
- Terytoria moga dawac bonusy, ale nie moga zamknac serwera dla slabszych gildii.
- Defender advantage powinien istniec, ale attacker musi miec realna szanse.
- Rewardy powinny wspierac gildie, crafting, ekonomie i prestiz.
- Dominujaca gildia powinna miec soft anti-snowball limity.

Mozliwe objective:

- Capture core.
- Hold point.
- Destroy gate.
- Escort siege engine.
- Control resource node.
- Relic extraction.
- Boss kill race w contested territory.

Role w guild wars:

- Frontline tank.
- Bruiser.
- Ranged pressure.
- Caster AoE.
- Healer/support.
- Scout/stealth.
- Siege utility.
- Anti-stealth/detection.
- Objective runner.

## Endgame Loop

Endgame powinien miec kilka rownoleglych drog.

Glowne drogi:

- Dungeon progression.
- Boss target farming.
- PvP ranking.
- Guild wars.
- Reputation maxing.
- Crafting/upgrading.
- Mastery progression.
- Seasonal goals.
- Cosmetic prestige.
- Economy/trading.

Zasady:

- PvE gracz ma co robic bez wymuszonego PvP.
- PvP gracz ma co robic bez farmienia tylko PvE.
- Najlepszy gear nie powinien pochodzic z jednej aktywnosci.
- Build-defining unique moga miec konkretne zrodla, ale powinny miec tradeoff.
- Endgame ma rozszerzac mozliwosci buildow, nie tylko podnosic liczby.

## Activity Reward Rules

Rewardy musza byc powiazane z ryzykiem i aktywnoscia.

Zasady:

- Safe PvE daje stabilne, slabsze rewardy.
- Dungeony daja gear i target farming.
- Bossy daja unique, rare crafting mats i prestiz.
- Contested zones daja lepsze resources za ryzyko PvP.
- PvP zones daja PvP currency, ranking, cosmetics, resource risk reward.
- Guild wars daja guild materials, territory bonuses i prestiz.
- Reputacje daja unlocki, nie tylko stat power.

Unikac:

- globalnego randomu bez target farmingu,
- jednej najlepszej farmy,
- daily chores jako obowiazkowego endgame,
- rewardow PvP, ktore niszcza PvE balance,
- rewardow PvE, ktore niszcza PvP balance.

## Test Cases And Scenarios

World & Content Loop v0.0.1 powinien przejsc ponizsze scenariusze:

- Nowy gracz po starcie wie, gdzie isc, jak walczyc i jak zaczac rozwijac pierwszy build.
- Solo gracz ma sensowna petle progresji bez stalej druzyny.
- Party graczy ma powod do dungeonow, bossow i synergii buildow.
- PvP gracz ma miejsca do walki, ale nie moze bezkarnie griefowac nowych graczy.
- Gracz PvE nie musi wchodzic w PvP, zeby miec sensowny progress.
- Gracz PvP nie musi farmic tylko PvE, zeby grac konkurencyjnie.
- Contested zone daje lepsze rewardy, ale ryzyko jest jasne przed wejsciem.
- Guilda ma powod do walki o teren, ale dominujaca gildia nie zamyka calego serwera.
- Boss dropi rzeczy, ktore da sie target farmic, ale unique item nie jest prostym best-in-slot.
- Subclass trial testuje realny styl gry, nie tylko level i klikniecie NPC.
- Death penalty jest lekka w casual PvE, mocniejsza w dungeonach i risk zones.
- World event generuje ruch na mapie, ale nie jest jedyna droga progresji.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/economy-crafting-loot-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/stolica-wyspy-hub-foundation-v0.0.1.md.
- docs/level-1-dungeon-island-foundation-v0.0.1.md.
- docs/dungeon-progression-difficulty-theme-ladder-foundation-v0.0.1.md.
- docs/dungeon-ladder-002-010-foundation-v0.0.1.md.
- docs/quest-contract-objective-foundation-v0.0.1.md.

W szczegolnosci:

- Dungeony i bossy musza testowac combat loop: attack, heavy attack, block, parry, counter, dash, ward, interrupt.
- Regiony musza wspierac progression, mastery i itemization.
- PvP zones musza respektowac PvP soft capy, combat timer, stealth counterplay i anti-grief.
- Questy i subclass trials musza wykorzystywac mastery oraz stat requirements.
- Loot musi wspierac item tags, affixy, rarity i target farming.
- Death penalty musi byc zgodne z progression/respec foundation.

## Assumptions

- Dokladna mapa swiata, nazwy regionow, lore i frakcje beda osobnym etapem.
- Konkretne wartosci rewardow, lockoutow, rankingow i drop rate beda etapem balansu.
- Klasy i podklasy beda projektowane po domknieciu petli swiata i aktywnosci.
- PvP jest kluczowe, ale nie powinno niszczyc early game ani casual PvE.
- Kazda ryzykowna aktywnosc powinna miec czytelne ostrzezenie i proporcjonalny reward.
