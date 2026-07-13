# Ekonomia, rzemioslo i lup fundament v0.0.1

## Cel dokumentu

Ten dokument definiuje fundament ekonomii, rzemiosla, lupu, handlu, ulepszania, celowego farmienia i sinkow dla serwera MCMMORPG.

Celem jest domkniecie zasad przed projektowaniem finalnych klas. Itemizacja, progresja i endgame musza miec jasne odpowiedzi na pytania: skad biora sie przedmioty, jak gracz je ulepsza, co usuwa nadmiar waluty z serwera, co mozna sprzedac, co powinno byc przypisane do postaci i jak uniknac inflacji albo jednej najlepszej farmy.

System ma wspierac:

- PvE solo.
- PvE party.
- Dungeony i boss celowe farmienie.
- Open-world PvP i contested zones.
- Guild wars i territory resources.
- Rzemioslo jako realna sciezke progresji.
- Handel miedzy graczami.
- Anty-inflacje i item sinki.
- PvP balance bez pay-to-win i bez alt-farmingu.

To nadal etap projektowy. Nie wdrazamy konfiguracji pluginow, konkretnych cen, szans na wypadniecie ani finalnych klas.

## Cele ekonomii

Ekonomia powinna:

- dawac graczom kilka sensownych drog zarabiania,
- wspierac celowe farmienie,
- usuwac walute i przedmioty przez stale sinki,
- ograniczac jedna najlepsza farme,
- wspierac handel bez zalania rynku,
- dawac rzemiosloowi realna wartosc,
- nie niszczyc PvP przez niekontrolowany ekwipunek gap,
- nie blokowac casual PvE za twarda ekonomia,
- dawac gildii cele ekonomiczne bez snowballu,
- byc czytelna dla gracza.

Zasada: ekonomia ma wzmacniac gameplay, nie zastepowac go. Najlepsza droga progresji nie powinna polegac tylko na siedzeniu przy markecie albo farmieniu jednego exploita.

## Currency Types

Waluty powinny miec jasne zrodla i sinki.

### Common Currency

Glowna waluta codzienna.

Zrodla:

- questy,
- moby,
- dungeony,
- vendor sell,
- world events,
- handel z graczami.

Zastosowania:

- repair,
- basic rzemioslo,
- fast travel / services,
- market tax,
- respec cost,
- basic ulepszanias,
- NPC vendors.

Zasady:

- Powinna byc tradeable.
- Powinna miec stale sinki.
- Nie moze byc jedynym kosztem najlepszych ulepszaniaow.

### Gameplay Premium Currency

Rzadsza waluta gameplayowa, zdobywana w grze.

Zrodla:

- bossy,
- challenge dungeony,
- world events,
- seasonal goals,
- reputacje.

Zastosowania:

- mocniejsze rzemiosloi,
- high-tier ulepszanias,
- cosmetic prestige,
- reroll wybranych elementow itemu,
- specjalne uslugi NPC.

Zasady:

- Nie powinna byc kupowana za realne pieniadze, jesli daje combat power.
- Moze byc przypisane do postaci albo ograniczona w handlu.
- Powinna miec tygodniowe albo aktywnosciowe limity, jesli jest bardzo mocna.

### PvP Currency

Waluta za aktywnosci PvP.

Zrodla:

- capture points,
- battleground/arena,
- bounty,
- contested objectives,
- guild wars,
- PvP events.

Zastosowania:

- PvP ekwipunek bases,
- cosmetics,
- consumables PvP,
- ranking nagrody,
- utility items do PvP.

Zasady:

- Nagroda powinien isc za objective i ryzyko, nie za farmienie slabszych.
- Musza istniec limity anti-alt farming.
- PvP currency nie powinna dawac niekontrolowanej przewagi w PvE.

### Dungeon / Boss Tokens

Tokeny powiazane z konkretnym contentem.

Zrodla:

- dungeon clear,
- boss kill,
- challenge mode,
- weekly boss objective.

Zastosowania:

- zabezpieczenie przed pechem system,
- celowe farmienie,
- zakup wybranych base itemow,
- rzemioslo materials,
- ulepszania boss-specific itemow.

Zasady:

- Token powinien pomagac przy pechu lupuym.
- Token nie powinien natychmiast kupowac najlepszego itemu bez wysilku.
- Tokeny moga byc przypisane do postaci, zeby ograniczyc market flooding.

### Faction Currency

Waluta frakcyjna.

Zrodla:

- reputation quests,
- faction events,
- region activities,
- faction dungeons,
- delivery / caravan tasks.

Zastosowania:

- vendor unlocks,
- recipes,
- cosmetics,
- faction bases,
- utility items,
- dungeon keys.

Zasady:

- Powinna wspierac identity frakcji.
- Nie powinna byc obowiazkowym best-in-slot dla kazdego style gryu.
- Mocne nagrody powinny miec reputation requirement.

### Guild Materials

Materialy gildyjne.

Zrodla:

- guild wars,
- territory resources,
- guild bosses,
- siege events,
- group rzemioslo.

Zastosowania:

- siege equipment,
- guild ulepszanias,
- territory defenses,
- guild rzemioslo,
- guild cosmetics.

Zasady:

- Powinny byc zarzadzane przez gildie.
- Nie moga blokowac calej ekonomii serwera.
- Dominujaca gildia powinna miec anti-snowball limity.

## Zrodla lupu

Lup musi miec jasna tozsamosc.

Glowne zrodla lupu:

- normal mobs,
- elite mobs,
- rare spawns,
- dungeony,
- bossy,
- world events,
- contested zones,
- PvP objectives,
- faction vendors,
- guild wars,
- rzemioslo,
- odzysk,
- treasure / exploration.

Zasady:

- Normal mobs daja common currency, basic materials, common/magic items.
- Elite mobs daja lepsze materialy, szansa na rzadki przedmiot i region-specific drops.
- Dungeony daja item bases, rzadki przedmiots, rzemioslo mats i boss tokens.
- Bossy daja unique, high-tier mats, boss tokens i target farm identity.
- Contested zones daja lepsze resources za ryzyko PvP.
- PvP objectives daja PvP currency i ograniczone materialy, ale nie powinny byc najlepszym zrodlem wszystkiego.
- Rzemioslo powinien tworzyc albo ulepszac przedmioty, ale wymagac materialow z aktywnosci.

## Celowe farmienie

Celowe farmienie jest kluczowy, zeby uniknac globalnego randomu.

Kazdy wazny item albo material powinien miec:

- primary source,
- secondary source,
- zabezpieczenie przed pechem/token path,
- trade/bind rule,
- drop identity.

Przyklady:

- Fire caster bases dropia czesciej z ognistych dungeonow i bossow.
- Poison materials dropia z bagien, jadowitych mobow i Alchemy / Poison aktywnosci.
- Shield/tank bases dropia z fortec, wojownikow NPC i dungeonow defensywnych.
- Stealth/dagger bases dropia z bandit zones, stealth trials i assassin bosses.
- PvP utility items kupuje sie za PvP currency i contested resources.

Zasady:

- Gracz powinien rozumiec, gdzie farmic item pod style gry.
- Boss nie powinien byc jedynym zrodlem calego archetypu.
- Zabezpieczenie przed pechem powinno pomagac przy pechu, nie zastapic lupu.
- Celowe farmienie powinien wspierac world/content loop.

## Rzemioslo System

Rzemioslo powinien wspierac style gry, nie byc losowym dodatkiem.

Rzemioslo sklada sie z:

- item bases,
- materials,
- recipes,
- stations,
- quality,
- affix interaction,
- ulepszanias,
- odzysk.

### Item Bases

Item base okresla podstawowa tozsamosc itemu.

Przyklady:

- dagger base,
- heavy armor base,
- robe base,
- shield base,
- staff base,
- bow base,
- ring base,
- catalyst base.

Zasady:

- Base powinien miec wymagania statow albo mastery, jesli jest mocniejszy.
- Base powinien pochodzic z konkretnego contentu.
- Base powinien determinowac mozliwe affixy i role itemu.

### Materials

Materialy powinny miec tier i zrodlo.

Typy materialow:

- common materials,
- region materials,
- dungeon materials,
- materialy z bossow,
- faction materials,
- PvP materials,
- guild materials,
- rare catalysts,
- unique fragments.

Zasady:

- Material powinien miec jasne zrodlo.
- Rzadkie materialy powinny miec celowe farmienie.
- Materialy wysokiego tieru powinny miec sinki przez rzemioslo i upgrading.

### Recipes

Recipe okresla, co gracz moze stworzyc.

Recipe moze wymagac:

- materialow,
- item base,
- rzemioslo station,
- level,
- mastery,
- reputation,
- quest unlock,
- guild permission,
- currency cost.

Zasady:

- Recipe unlock powinien byc nagroda za content.
- Recipe nie powinien calkowicie omijac dungeonow, bossow i PvP.
- Mocne recipe powinny wymagac kilku typow zasobow.

## Upgrading System

Upgrade powinien byc glownym item/currency sinkiem.

Typy ulepszania:

- item level ulepszania,
- rzadkosc ulepszania,
- affix reroll,
- affix lock / partial control,
- quality ulepszania,
- socket/rune/gem slot, jesli system zostanie dodany,
- durability repair,
- unique-specific ulepszania.

Zasady:

- Upgrade ma kosztowac walute i materialy.
- Upgrade mocnych itemow powinien wymagac content-specific materials.
- Reroll nie powinien byc calkowicie darmowym szukaniem best-in-slot.
- Partial control jest lepszy niz pelny chaos, bo gracz musi rozumiec decyzje.
- Upgrade nie moze omijac PvP soft capow.
- Unique ulepszania musi miec PvP sanity check.

Ryzyko ulepszania:

- Early ulepszania powinien byc bezpieczny.
- Mid ulepszania moze byc drozszy, ale przewidywalny.
- Endgame ulepszania moze miec wiekszy koszt, rare material albo limit.
- Unikac twardego niszczenia itemu bez ostrzezenia, bo to frustruje.

## Item Sinks

Item sinki sa konieczne, zeby rynek nie zalal sie itemami.

Glowne item sinki:

- odzysk,
- rzemioslo consumption,
- ulepszania materials,
- repair/durability,
- bind-on-equip,
- bind-on-pickup dla wybranych lupu,
- market tax/listing fee,
- transmute/reroll,
- faction donation,
- guild projects,
- cosmetic conversion.

Zasady:

- Odzysk powinien dawac materialy, ale mniej niz koszt stworzenia itemu.
- Repair powinien byc regularnym currency sinkiem, nie kara niszczaca gre.
- Bind rules powinny chronic najwazniejsze dropy przed market flooding.
- Guild projects moga zjadac duze ilosci materialow bez dawania broken combat power.

## Trading And Market

Handel jest wazny, ale musi miec ograniczenia.

Typy handlu:

- direct trade,
- auction/market,
- guild trade,
- vendor sell,
- buy orders, jesli system pozwoli.

Trade rules:

- Common currency tradeable.
- Basic materials tradeable.
- Wiekszosc rzadki przedmiotow tradeable przed equipem.
- Wybrane boss unique bind-on-pickup albo bind-on-equip.
- PvP nagrody czesciowo przypisane do postaci, zeby ograniczyc alt farming.
- Guild materials moga byc guild-przypisane do postaci.

Market sinki:

- listing fee,
- sale tax,
- premium listing cost,
- cancellation fee dla wybranych itemow,
- repair/rzemioslo costs powiazane z handlem.

Anti-abuse:

- logowanie duzych transferow,
- cooldown na handel nowego konta,
- limity nagrod za zabijanie tego samego gracza,
- sanity check dupe exploitow,
- brak real-money power shopu.

## PvP Economy

PvP economy musi nagradzac ryzyko i skill, ale nie farmienie altow.

Nagrody PvP:

- PvP currency,
- ranking points,
- cosmetics,
- contested resources,
- bounty nagrody,
- guild war nagrody,
- limited PvP utility items.

Zasady:

- Nagroda za objective wazniejszy niz nagroda za sam kill.
- Zabijanie tego samego gracza powinno szybko dawac diminishing returns.
- Duza roznica leveli/ekwipunku powinna obnizac nagroda albo blokowac farmienie.
- PvP lup risk dotyczy glownie resource bag, nie calego ekwipunku.
- PvP currency nie powinna robic PvE mandatory grind.
- PvP ekwipunek powinien miec PvP identity, ale respektowac soft capy.

Lup risk:

- Contested zone: ograniczona utrata resource bag.
- PvP zone: wiekszy resource risk, ale ekwipunek bezpieczny albo tylko durability loss.
- Guild war: osobne zasady, najczesciej koszt napraw i siege resources.
- Bounty: nagroda za cel, ale z anti-abuse trackingiem.

## Guild Economy

Guild economy ma wspierac wojny i wspolne cele.

Zrodla:

- territory resources,
- guild war wins,
- guild events,
- guild bosses,
- member contributions,
- contested objectives.

Sposoby wydawania:

- siege equipment,
- territory ulepszanias,
- defense structures,
- guild rzemioslo,
- guild bank ulepszanias,
- cosmetics,
- event activation.

Anti-snowball:

- territory nagroda caps,
- upkeep cost,
- diminishing returns za zbyt wiele terenow,
- siege windows,
- catch-up objectives dla slabszych gildii,
- brak blokowania podstawowych materialow przez jedna gildie.

Zasada: gildia moze miec przewage organizacyjna i ekonomiczna, ale nie moze zabetonowac calego serwera.

## Economy Balance Rules

Zasady balansu:

- Nie moze istniec jedna najlepsza farma dla wszystkich.
- Najlepszy ekwipunek powinien wymagac kilku typow aktywnosci albo handlu.
- Safe farming daje mniej, risk farming daje wiecej.
- Market powinien usuwac walute przez tax.
- Upgrade powinien usuwac walute i materialy.
- Rzemioslo powinien zuzywac wiecej niz odzysk oddaje.
- PvP nagrody powinny miec anti-abuse.
- Guild nagrody powinny miec anti-snowball.
- Unique przedmioty powinny miec tradeoff i trade/bind rules.
- Ekwipunek advantage ma dawac przewage, ale PvP soft capy musza ograniczac one-shot meta.

## Test Cases And Scenarios

Economy, Rzemioslo & Lup v0.0.1 powinno przejsc ponizsze scenariusze:

- Nowy gracz zdobywa pierwsze materialy i rozumie, do czego sluza.
- Solo PvE gracz moze progresowac przez questy, regiony, rzemioslo i dungeony.
- Party graczy ma powod do dungeonow i boss celowego farmienia.
- PvP gracz ma nagrody z objectives, ale nie moze farmic altow.
- Crafter moze zarabiac i ulepszac przedmioty, ale potrzebuje materialow z contentu.
- Boss ma drop identity i zabezpieczenie przed pechem/token path.
- Contested zone daje lepsze materialy, ale ryzyko PvP jest jasne.
- Market tax i ulepszania costs realnie usuwaja walute.
- Odzysk usuwa przedmioty z rynku, ale nie drukuje pelnej wartosci itemu.
- Unique item zmienia gameplay, ale trade/bind rule ogranicza zalanie rynku.
- Guilda kontrolujaca teren ma bonus, ale nie blokuje calej ekonomii.
- High ekwipunek kosztuje czas i zasoby, ale PvP soft capy chronia przed one-shot meta.

## Powiazania z fundamentami

Ten dokument musi pozostac spojny z:

- docs/combat-foundation-v0.0.1.md.
- docs/damage-defense-foundation-v0.0.1.md.
- docs/skill-ability-system-v0.0.1.md.
- docs/itemization-foundation-v0.0.1.md.
- docs/professions-foundation-v0.0.1.md.
- docs/model-lupu-z-lochow-foundation-v0.0.1.md.
- docs/ekwipunek-pierwszego-aktu-foundation-v0.0.1.md.
- docs/loot-reward-table-001-foundation-v0.0.1.md.
- docs/progression-respec-foundation-v0.0.1.md.
- docs/world-content-loop-foundation-v0.0.1.md.

W szczegolnosci:

- Itemy dalej korzystaja z ustalonych slotow, rzadkosc, affixow, stat requirements i tag skalowanie.
- Waluty i nagrody musza wspierac world/content loop.
- Profesje musza levelowac przez uzywanie, ale endgame rzemioslo wymaga limitow specjalizacji.
- Salvaging jest wspolnym systemem item sinku, nie osobna profesja.
- Pierwsza konkretna tabela lupu dla `loch_001` jest opisana w loot-reward-table-001.
- Rzemioslo i ulepszania musza respektowac stat requirements, mastery i PvP sanity checks.
- PvP economy musi respektowac combat timer, anti-grief, stealth/detection counterplay i PvP soft capy.
- Guild economy musi wspierac guild wars bez snowballu.
- Death penalty moze generowac koszt, ale nie powinna kasowac calego ekwipunku.

## Assumptions

- Konkretne ceny, szans na wypadniecie, tax, ulepszania cost i limity beda osobnym etapem balansu.
- Nie projektujemy jeszcze finalnych klas ani podklas.
- Nie dodajemy real-money combat power.
- Najmocniejsze przedmioty moga miec bind rules, jesli trade niszczylby ekonomie.
- Rzemioslo ma byc wazny, ale nie ma calkowicie zastepowac bossow, dungeonow, PvP i reputacji.
