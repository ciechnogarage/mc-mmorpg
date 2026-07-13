# Progression & Respec Foundation v0.0.1

## Cel dokumentu

Ten dokument definiuje progresje postaci, mastery, odblokowywanie skilli, podklas, respec, loadouty, catch-up, endgame oraz kary za smierc dla serwera MCMMORPG.

Kierunek v0.0.1: progresja ma byc bardziej mastery-first, inspirowana Skyrimem, ale z PoE-like wymaganiami buildowymi. Gracz rozwija postac nie tylko przez level, ale tez przez realne uzywanie broni, magii, defensywy i utility.

System musi wspierac:

- PvE solo.
- PvE party.
- PvP 1v1, male grupy, open world i wojny gildii.
- Hybrydy, ale z realnym kosztem inwestycji.
- Pozniejsze klasy bazowe i podklasy bez zamykania projektu za wczesnie.

To nadal etap projektowy. Nie wdrazamy jeszcze konfiguracji pluginow ani finalnych klas.

## Core Progression Model

Progresja sklada sie z kilku warstw:

- Character Level - ogolny poziom postaci.
- Stat Points - punkty rozdawane co poziom.
- Mastery - rozwoj przez uzywanie konkretnego stylu.
- Skill Unlocks - odblokowania skilli przez wymagania.
- Subclass / Ascendancy - specjalizacja odblokowywana pozniej.
- Gear Progression - progresja przez itemy, affixy, rarity i unique.
- Reputation / Endgame Progression - reputacje, bossy, PvP ranking, sezonowe cele.

Zasada: level postaci daje rame rozwoju, ale to mastery i staty mowia, czym gracz faktycznie umie grac.

## Character Level

Character Level jest glowna rama progresji.

Level powinien dawac:

- stat point co poziom,
- bazowe HP/resource scaling,
- dostep do regionow,
- dostep do questow,
- dostep do dungeonow,
- milestone rewardy,
- dostep do subclass/ascendancy,
- odblokowania bazowych tierow skilli.

Level nie powinien sam z siebie dawac pelnej mocy buildu. Gracz z wysokim levelem, ale bez mastery w danym stylu, moze uzywac tego stylu, ale nie powinien byc w nim tak mocny jak gracz, ktory realnie go rozwijal.

Przykladowy rytm:

- Level 1-10: starter phase, czyli testery stylow gry: melee, defense, projectile, mobility, stealth, spell, support, debuff, summon i trap.
- Level 10: wybor klasy bazowej.
- Level 10-25: core class skills, pierwsze pasywki, podstawowe dungeony i kierunek stat/mastery.
- Level 25: wybor podklasy, signature skill i pierwsza prawdziwa specjalizacja.
- Level 25-50: rozwijanie podklasy przez mastery, gear, questy, dungeony, bossy i PvP.
- Level 50: upgrade wybranej podklasy, czyli advanced specialization.
- Endgame: gear, mastery, reputacje, boss farming, PvP ranking, guild wars i unique item skills.

## Stat Points

Po kazdym poziomie gracz dostaje punkty statystyk do rozdania.

Startowe staty:

- Strength.
- Dexterity.
- Intelligence.
- Faith.
- Vitality.

Zasady:

- Co level gracz dostaje 1 stat point.
- Co 10 leveli gracz dostaje milestone bonus, np. dodatkowy stat point, pasywka albo wybor rewardu.
- Staty z levelowania licza sie do wymagan itemow, skilli, podklas i buildow.
- Staty z itemow rowniez moga liczyc sie do wymagan, ale zdjecie itemu moze dezaktywowac skill, item lub bonus.
- Staty nie moga calkowicie zastepowac gearu, mastery i counterplayu.
- Staty powinny wzmacniac kierunek postaci, nie byc jedynym zrodlem mocy.

Przyklady wymagan:

- Skill wymaga Level 18, Dexterity 25 i Archery Mastery 12.
- Item wymaga Strength 30 i Two-Handed Mastery 8.
- Podklasa wymaga Level 25, odpowiedni quest, staty i minimum mastery w powiazanym stylu.

## Mastery Progression

Mastery to rozwoj przez realne uzywanie stylu gry.

Mastery nie jest klasa. To osobna warstwa rozwoju, ktora mowi, w czym postac ma doswiadczenie.

Startowe mastery:

- One-Handed.
- Two-Handed.
- Archery.
- Daggers.
- Shield.
- Destruction.
- Restoration.
- Chaos.
- Stealth.
- Alchemy / Poison.

Zasady:

- Mastery rosnie przez sensowne uzywanie w realnej walce albo aktywnosci.
- Mastery nie powinno rosnac od afk farmienia, spamowania w powietrze ani bezpiecznego exploita.
- Mastery powinno miec diminishing returns na powtarzalne, niskowartosciowe akcje.
- Mastery nie powinno byc zbyt drobne. Lepiej miec Archery niz osobne mastery dla kazdego luku.
- Mastery powinno odblokowywac skille, pasywki, drobne bonusy i wymagania podklas.
- Mastery powinno wspierac hybrydy, ale z kosztem czasu i inwestycji.

Przyklady rozwoju:

- One-Handed rosnie od walki mieczem jednorocznym, parowania, kontr i weapon skilli.
- Archery rosnie od trafien projectile, headshotow, walki na dystans i pozycyjnych skilli.
- Daggers rosnie od szybkich melee trafien, backstabow, stealth openerow i flankowania.
- Destruction rosnie od ofensywnych spelli Fire, Cold i Lightning.
- Restoration rosnie od leczenia, cleanse, wardow defensywnych i ochrony sojusznikow.
- Chaos rosnie od curse, chaos damage, summonow albo mrocznych efektow.
- Stealth rosnie od skutecznego skradania, omijania detekcji, openerow i scoutingu.

Mastery XP powinno byc liczone od wartosciowej akcji, np. trafienie przeciwnika na podobnym poziomie, skuteczny block/parry, udany heal w walce, ujawnienie stealthed gracza, a nie od samego klikniecia.

## Skill Unlocks

Skill unlock powinien korzystac z kombinacji:

- level requirement,
- stat requirement,
- mastery requirement,
- weapon/item requirement,
- class/subclass requirement,
- quest requirement dla mocniejszych skilli.

Przykladowy format projektowy:

- Name.
- Tags.
- Required Level.
- Required Stats.
- Required Mastery.
- Required Weapon / Item.
- Required Class / Subclass.
- PvP Restrictions.

Przyklady:

- Power Strike: Level 8, Strength 12, One-Handed 4 albo Two-Handed 4, weapon melee.
- Backstab: Level 10, Dexterity 18, Daggers 6, Stealth 4, dagger equipped.
- Frost Nova: Level 16, Intelligence 22, Destruction 8, Spell tag.
- Cleanse: Level 14, Faith 18, Restoration 7.
- Smoke Dash: Level 20, Dexterity 25, Stealth 10, Movement + Stealth tags.

Zasada: im mocniejszy skill, tym wiecej warstw wymagania. To ogranicza broken buildy, ale nie zamyka kreatywnych hybryd.

## Subclass / Ascendancy Unlock

Subclass / Ascendancy to specjalizacja wybierana na Level 25, nie startowa decyzja i nie Level 50 upgrade.

Odblokowanie powinno wymagac:

- Level 25,
- specjalnego questa albo proby,
- mastery powiazanego z wybranym stylem,
- minimalnych statow,
- czasem testu gameplayowego, np. dungeon, arena, boss, stealth trial.

Przykladowe wymaganie:

- Level 25.
- Dexterity 35.
- Daggers Mastery 15.
- Stealth Mastery 10.
- Completed subclass trial.

Podklasa powinna:

- wzmacniac konkretny styl,
- odblokowywac signature skill,
- dawac unikalna pasywke,
- dawac tradeoff albo ograniczenie,
- miec jasny counterplay w PvP.

Podklasa nie powinna:

- byc tylko bonusowym stat stickiem,
- kasowac znaczenia mastery,
- byc permanentna pulapka bez opcji respecu,
- dawac wszystkich narzedzi naraz.

## Subclass Upgrade Level 50

Level 50 odblokowuje upgrade wybranej podklasy.

Zasady:

- Level 50 nie daje drugiej pelnej podklasy.
- Upgrade rozwija wybrana podklase w advanced specialization.
- Upgrade powinien wymagac levelu, mastery, questa/proby i contentu.
- Upgrade daje endgame identity, ale nie usuwa counterplayu.

## Respec Rules

Respec ma pozwalac naprawiac build, ale nie moze robic z decyzji czegos bez znaczenia.

Typy respecu:

- Stat respec.
- Skill respec.
- Passive respec.
- Mastery refund / conversion.
- Subclass respec.

### Stat Respec

Zasady:

- Partial stat respec powinien byc dostepny stosunkowo tanio.
- Full stat respec powinien byc drozszy i miec cooldown albo specjalny koszt.
- Respec nie powinien byc mozliwy w trakcie walki.
- Respec powinien byc mozliwy w safe zone, u NPC albo przez specjalny item.
- Zmiana statow moze zdjac wymagania itemow, skilli i podklas.

### Skill Respec

Zasady:

- Skille mozna wymieniac w safe zone albo poza walka.
- Skill bar nie powinien byc dowolnie zmieniany w trakcie PvP.
- Gracz moze eksperymentowac, ale nie powinien miec idealnej odpowiedzi na kazdego przeciwnika bez przygotowania.

### Mastery Respec

Mastery wynika z uzywania, wiec nie powinno byc latwo resetowane.

Zasady:

- Mastery raczej nie resetuje sie w pelni.
- Mozna rozwazac ograniczona konwersje mastery przy duzym koszcie.
- Catch-up moze pomagac w rozwijaniu nowego mastery, ale nie powinien natychmiast dawac pelnej mocy.
- Gracz zmieniajacy styl musi czuc, ze zaczyna uczyc sie nowej broni/magii.

### Subclass Respec

Subclass respec powinien byc mozliwy, ale istotny.

Zasady:

- Wymaga specjalnego questa, itemu albo NPC.
- Ma wysoki koszt.
- Nie moze byc spamowany pod kazde PvP.
- Moze miec cooldown.
- Nie powinien kasowac postaci ani calego progressu.

## Loadouts

Loadouty pozwalaja zapisac kilka przygotowanych setupow.

Loadout moze zawierac:

- skill bar,
- weapon set,
- armor set,
- akcesoria,
- quick items,
- ewentualnie preset pasywek, jesli system pasywek na to pozwoli.

Zasady:

- Zmiana loadoutu tylko poza walka albo w safe zone.
- W PvP moze byc combat lockout, np. brak zmiany przez kilka sekund po otrzymaniu lub zadaniu obrazen.
- Loadout nie omija wymagan statow, mastery, itemow ani cooldownow.
- Loadout nie resetuje cooldownow.
- Loadout jest narzedziem wygody, nie instant counter-pickiem.

## Catch-Up

Catch-up ma pomagac nowym i wracajacym graczom, ale nie moze niszczyc sensu progresji.

Mozliwe mechaniki:

- XP boost do okreslonego progu wzgledem sredniej serwera.
- Daily / weekly bonusy dla niskich leveli.
- Szybsze early mastery do okreslonego progu.
- Eventy dla nowych graczy.
- Starter gear, ktory pomaga wejsc do gry, ale nie zastapi endgame itemow.

Zasady:

- Catch-up nie powinien omijac subclass trial.
- Catch-up nie powinien dawac pelnego endgame mastery.
- Catch-up powinien skracac nudny grind, nie anulowac calego rozwoju.
- Catch-up musi byc ostrozny w PvP, zeby alt konta nie abuseowaly bonusow.

## PvP Progression Rules

PvP jest kluczowe, dlatego progresja musi miec ograniczenia.

Zasady:

- Gear i progresja daja przewage, ale nie natychmiastowy one-shot.
- Level roznica nie moze calkowicie kasowac skill expression.
- PvP powinno miec soft capy albo normalization na ekstremalne wartosci.
- Najmocniejsze PvP staty musza miec limity albo diminishing returns.

Staty wymagajace kontroli PvP:

- resistances,
- penetration,
- healing power,
- anti-heal,
- cooldown reduction,
- movement speed,
- stealth,
- detection,
- burst damage,
- ward scaling,
- tenacity,
- poise.

PvP sanity check:

- Tank powinien przezyc dluzej, ale miec nizszy kill pressure.
- Burst build powinien miec okno wejscia i okno slabosci po nieudanym wejsciu.
- Healer powinien byc wartosciowy, ale kontrowany przez anti-heal, interrupt i pressure.
- Stealth powinien miec mocny opener, ale reveal, AoE, detection i combat timer musza byc realna kontra.
- Caster powinien miec ward/kite, ale byc podatny na interrupt, line of sight i melee pressure.
- Low gear gracz powinien miec szanse przez parry, block, dash, counter i dobre pozycjonowanie.

## Endgame Progression

Endgame nie powinien byc tylko podnoszeniem liczb.

Warstwy endgame:

- rare / epic / unique gear,
- target farming bossow,
- mastery progression,
- subclass upgrades,
- reputacje frakcji,
- dungeon progression,
- PvP ranking,
- guild war rewards,
- sezonowe cele,
- crafting/upgrading,
- kosmetyczne prestize.

Zasady:

- Najlepsze itemy powinny miec zrodla, ktore da sie target farmic.
- Unique itemy moga zmieniac gameplay, ale musza miec tradeoff.
- PvP rewards nie powinny dawac nieuczciwej przewagi w PvE ani odwrotnie.
- Endgame powinien dawac kilka drog progresji, nie jedna obowiazkowa farme.
- Mastery moze dalej rosnac, ale z diminishing returns.

## Death And Penalty

Kara za smierc zalezy od trybu.

Tryby:

- Casual PvE.
- Dungeon PvE.
- Boss arena.
- Open-world PvP.
- Guild war.

Zasady:

- Casual PvE ma lekka kare, zeby nie zniechecac.
- Dungeony moga miec wieksze ryzyko: utrata durability, czesc zasobow, checkpoint, czasowy debuff.
- Boss arena moze miec ograniczone retry albo koszt wejscia.
- Open-world PvP moze miec ryzyko utraty czesci loot bag/resource, ale nie powinno kasowac calego ekwipunku.
- Guild war moze miec osobne zasady zalezne od formatu wojny.
- Kara za smierc nie powinna byc tak duza, zeby gracze bali sie testowac buildy.

## Test Cases And Scenarios

Progression v0.0.1 powinno przejsc ponizsze scenariusze projektowe:

- Nowy gracz rozwija miecz przez realna walke i po kilku poziomach widzi progres bez znajomosci mety.
- Gracz zmienia z luku na daggery i moze to zrobic, ale musi nadrobic mastery zamiast dostac pelna sile od razu.
- Hybryda wojownik plus proste spelle dziala, ale nie ma pelnego burstu maga bez inwestycji w Intelligence, mana i Destruction.
- Caster z wysokim wardem przezywa burst, ale pada pod dluzsza presja, interruptem albo dobrym timingiem melee.
- Stealth build ma mocny opener, ale reveal, AoE, detection i combat timer sa realnymi kontrami.
- Healer ma sens w PvP, ale anti-heal, interrupt i presja moga go zatrzymac.
- Tank ma przewage przezywalnosci, ale nie zabija szybko i jest podatny na guard break, DoT albo flankowanie.
- Full respec pozwala naprawic postac, ale koszt i ograniczenia blokuja spamowanie kontr pod kazdego przeciwnika.
- Subclass unlock wymaga levela, questa i mastery, wiec nie jest losowym wyborem z menu.
- Low gear vs high gear PvP: high gear ma przewage, ale low gear moze wygrac przez block, dash, parry, counter i pozycjonowanie.
- Death penalty nie niszczy casualowego PvE, ale w dungeonach, open-world PvP i wojnach gildii dodaje realne ryzyko.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.

W szczegolnosci:

- Damage type zostaja bez zmian: Physical, Fire, Cold, Lightning, Chaos, Poison.
- Staty zostaja bez zmian: Strength, Dexterity, Intelligence, Faith, Vitality.
- Mastery korzysta z istniejacych tagow skilli i stylow walki.
- Itemy moga spelniac wymagania statow, ale zdjecie itemu moze dezaktywowac zalezne bonusy.
- PvP musi respektowac counterplay z Combat Foundation.
- Respec i loadouty nie moga omijac cooldownow, combat timerow ani wymagan buildowych.

## Assumptions

- Level cap zostanie ustalony pozniej.
- Konkretne wartosci XP, koszty respecu, cooldowny i soft capy beda osobnym etapem balansu.
- Podklasy beda projektowane dopiero po domknieciu progresji, ekonomii i glownych aktywnosci.
- Mastery-first oznacza, ze level jest wazny, ale styl gry wynika przede wszystkim z tego, czego gracz realnie uzywa.
- Wszystkie mechaniki PvP musza miec counterplay zanim trafia do implementacji.
