# Runda 1 — PvP i wielu graczy

## N1
Pełny przebieg FAIL → PASS z dodatnią kontrolą miecza PvP i wilkiem jest w `krytyk/n1-r1.md`: `b-1-16.out` md5 `a67ff450ea05` wywołał `StackOverflowError`, a ta sama sonda `b-1-17.out` md5 `1a1994fbc239` zadała 2 HP przy obecnym wilku bez błędu logu. Test przy WorldGuard `pvp deny` w `b-1-15.out` md5 `a67ff450ea05` dał 0 obrażeń; sonda `PW` wymusiła `allow` dla kontroli dodatniej.

## Niezależne interakcje 3–4 botów
`kolejka/b-1-25.out`, JAR md5 `1a1994fbc239`, każda grupa z osobną świeżą sesją MagTest i kontrolą dodatnią Płomieni:

- `PS` (MagTest, MagWspol, MagCel): dwóch właścicieli przywołało jednocześnie 2 wilki, mana każdego 20→17 (PASS).
- `PH` (MagTest, MagCel, MagBlok): kontrola samoleczenia 12→13,7 HP (PASS); `Uzdrowienie innego` leczy MagCel 14→20 HP za 2 many (PASS). Starszy `P-leczenie-innego` FAIL był skutkiem długiego, zależnego od stanu scenariusza i nie dowodzi błędu N5.
- `PB` (MagTest, MagCel, MagBlok, MagWspol): strumień bez zasłony uszkodził huska 20→18 HP (kontrola PASS). Gdy MagBlok stanął między MagTest i huskiem na tej samej osi, husk ponownie dostał 2 obrażenia (`PB-zaslanianie` FAIL) — gracz nie zasłania stożka. Po zmianie celownika podczas aktywnego strumienia dwa oddalone od siebie huski miały 18 i 19 HP (`PB-zmiana-celu` PASS).

W `bot/scenariusze/PB.js` zasłaniający stoi w (0,80,2), husk w (0,80,5), rzucający w (0,80,0). `Rzucanie.stozek` używa `p.hasLineOfSight(e)`, które w tej obserwacji nie wyklucza celu ukrytego za innym graczem; waga błędu gry ŚR, jeśli oczekiwane jest blokowanie strumienia przez sojusznika. Pytanie do właściciela: czy sojusznik ma fizycznie zasłaniać cel strumienia?

W późniejszej pełnej macierzy `b-1-39.out`, JAR md5 `ca82ed86037b`, jedna z pięciu prób wilka przy MagCel z `pvp deny` miała spadek 3,3 HP. Izolowana sonda `b-1-44.out`, JAR md5 `35a17b265e15`, 13 PASS / 0 FAIL / 0 BLAD, przywołała wilka 12/12 razy obok zdrowego MagCel; HP sojusznika pozostało 18→18 w 12/12, 0 naruszeń niezmienników. Incydent z macierzy nie odtworzył się; wersje JAR są różne, więc nie jest to dowód naprawy ani powtarzalnego friendly fire.
