# Hub Q — uwagi recenzenta (sesja codex-hub-menu-review-loop)

Właściciel: „ma nie być gorzej niż jest, ma być przemyślane i z sensem”. Każda runda: Codex poprawia →
recenzent ocenia w kliencie → nowa sekcja poniżej. Status każdego punktu odhacza recenzent, nie Codex.

## Runda 0 — stan bazowy (zrzuty zrzuty/baza-07-pomoc, baza-07-ustawienia, baza-26-gui3-q, baza-32-gui2-q)

### Nie wolno pogorszyć (bramka regresji)
- MENU: 12 kafli 2×2 z ilustracjami, ostre podpisy wersalikami, jedna strona, nazwy kafli bez zmian.
- Dół MENU: ? / zębatka / ZMIEŃ POSTAĆ / X, pasek stanu, rząd szybkiej podróży, SZYBKA PODRÓŻ.
- Q z przedmiotem i z pustą ręką otwiera MENU; tytuł = `&f` + same glify (pełna jasność tła).
- Każdy kafel otwiera swoje okno klikiem gracza bez OP i da się wrócić do MENU bez komendy.
- Zero nowych WARN/ERROR po `wgraj`.

### Wady do naprawy
| # | waga | gdzie | wada | oczekiwany wynik |
|---|---|---|---|---|
| H1 | KRYT | wszystkie podstrony | Na dole widać PRAWDZIWY EQ gracza: przedmioty leżą na narysowanej tabliczce SZYBKA PODRÓŻ, dymki „Rękawice/Pas — przeciągnij tu”. Brak WRÓĆ DO MENU. Przyczyna: jar bez kodu `podstrony` (merge 41469ea). | Dół podstrony = dół MENU + działający „Wróć do menu”; EQ niewidoczne i nieklikalne; po zamknięciu EQ wraca nietknięte. |
| H2 | WYS | MENU, pasek stanu | Dwa puste niebieskie kwadraty (2. i 3. pozycja paska) — dziury bez znaczenia. | Każda pozycja paska coś znaczy albo jest narysowana jako tło (nie pusty slot). |
| H3 | ŚR | MENU, szybka podróż | Nowy gracz widzi 7 pustych kółek bez wyjaśnienia (wygląda na zepsute). | Puste miejsce mówi, jak je odblokować (dymek/zarys), bez nowej strony. |
| H4 | ŚR | MENU, kafle | Zadania vs Dziennik, Podróże vs Portal — różnica tylko w dymku (#28). | Pierwsza linia dymku nazywa różnicę wprost (np. „Lochy i cele wypraw” vs „Szybka podróż między odkrytymi punktami”); nazw kafli nie zmieniać. |
| H5 | ŚR | dymki kafli | „Na razie dostępny jest Loch 001”, „Sprawdź zadanie i chowańca”, „Otwórz ich menu” — doraźne/puste. | Dymek = co tu znajdę + stan na żywo (placeholder), max 3 linie, bez obietnic bez treści. |
| H6 | ŚR | POMOC | „Q otwiera Księgę” (nazwa to MENU); kafel WKRÓTCE = martwy kafel. | Aktualne nazwy, prawdziwe sterowanie (Q, Shift+Q, E, F — sprawdzone w kliencie); zero martwych kafli. |
| H7 | ŚR | USTAWIENIA | Jedna pozycja (PvP) na pustej stronie. | Tylko opcje, które REALNIE działają (sprawdzone w kliencie); lepiej 3 prawdziwe niż 6 atrap. |
| H8 | NIS | MENU, dół | ? i zębatka małe, sens tylko po najechaniu. | Dymek z nazwą + 1 zdaniem; ewentualnie wyraźniejsza ikona w tej samej rodzinie. |

### Dowód wymagany od Codexa (per punkt)
Zrzut klienta GUI 3 (i GUI 2 dla MENU) przed/po, nazwa pliku w raporcie. Bez zrzutu = niezrobione.

## Runda 1 — ocena wyniku Codexa (zrzuty menu-16-q, test-06-ustawienia + YAML-e stagingu)

Werdykt: część lepiej (H2 pasek stanu wypełniony, H6 sterowanie w POMOCY), ale **trzy rzeczy są GORZEJ niż baza** — te
cofnąć/przerobić w pierwszej kolejności. H1 (podstrony) czeka na merge 41469ea — ocenię po rundzie 2.

| # | waga | gdzie | ocena | co zrobić |
|---|---|---|---|---|
| R1-1 | KRYT (gorzej) | MENU, kafle kb_{1,2,4}.png | Podpisy dwuwierszowe „ZADANIA/MISJE”, „DZIENNIK/POSTĘP”, „POCZTA/WYPRAWY”, „PORTAL/TELEPORT”: mniejsze, ściśnięte, dwukolorowe, niespójne z 8 pozostałymi; „POCZTA WYPRAWY” jest merytorycznie błędne (poczta to listy, nie wyprawy). Właściciel odrzucił zmianę nazw kafli bez potrzeby. | Przywrócić kb_1/2/4.png z produkcji (jednowierszowe podpisy). Różnicę Zadania/Dziennik i Podróże/Portal nieść PIERWSZĄ linią dymku (H4), nie grafiką. |
| R1-2 | WYS (gorzej) | open_commands w hub_glowne, hub_pomoc, mapa_swiata, chowance_menazeria, ksiega_zaklec | Każde wciśnięcie Q i każde wejście w te okna wypluwa linię na czat = spam (gracz otwiera MENU dziesiątki razy na sesję); czat jest pod oknem i i tak go nie czyta. #10 nie tędy. | Usunąć wszystkie `[message]` z open_commands. Nazwa strony jest w glifie tytułu, opis w dymku emblematu/kafla. Jeśli #10 wymaga tekstu dla gracza bez paczki — zostaw CZĘŚCIOWO, nie spamuj. |
| R1-3 | WYS (gorzej wizualnie) | USTAWIENIA | 10 gołych przedmiotów vanilla (książka, papier, glowstone, kompas, mapy, oko pająka, ender eye) rozsypanych na stronie bez ramek i podpisów, dwa BARRIER (czerwony zakaz) wyglądają jak błąd; każda opcja to DWA przyciski wł./wył., a gracz nie widzi, która jest aktywna; ARROW „Menu główne” w slocie 44 dubluje „Wróć do menu” z dołu podstron. | Jedna pozycja = jedna opcja-przełącznik: ikona w stylu Księgi (nexo-…, ta sama rodzina co TRYB PVP), nazwa, w dymku stan na żywo („Teraz: kompaktowa”, z placeholdera meta LuckPerms / NMinimap) i „Kliknij, aby zmienić”; cykl wartości po kliknięciu. Bez BARRIER. Rozkład na siatce strony (wyrównane, jak kafle). Usunąć ARROW (powrót robi dół podstrony). Każdy przełącznik sprawdzić w kliencie (zrzut przed/po + stan po relogu). |
| R1-4 | ŚR | dymki kafli | Lepsze, ale: „Loch 001 znajdziesz tutaj.” nadal doraźne; „Tu zarządzasz listą zadań.” pusty frazes. | Dymek = 1. linia: czym ten kafel różni się od bliźniaka; 2. linia: stan na żywo (np. aktywne zadanie %…%, odkryte punkty N/9 jak w Portalu). Bez nazw konkretnych lochów. |
| R1-5 | ŚR | POMOC | Kafel „Wkrótce” nadal martwy („Puste miejsce to miejsce na to, co powstaje”). Sterowanie bez E (okno postaci) i F (pasek umiejętności) / Shift+F (zamiana zestawu broni). | Usunąć „Wkrótce” albo zastąpić prawdziwą treścią (np. „Pierwsze kroki” = aktualny cel z dziennika). Uzupełnić sterowanie o E, F, Shift+F — każdy klawisz sprawdzić w kliencie przed wpisaniem. |
| R1-6 | ŚR | MENU, szybka podróż (H3) | Bez zmian: 7 pustych kółek. | Puste kółko = przedmiot z dymkiem „Nieodkryty punkt — odkryjesz go, docierając do …/podczas wyprawy”; jeśli plugin rysuje je sam, to w KsiegaPostaci. |
| R1-7 | OK | pasek stanu (H2) | Wypełniony, brak dziur. | Potwierdzić dymkami w kolejnym zrzucie, że każda ikona ma nazwę i sens. |

Bramka rundy 2 (bez tego nie oceniam): zrzut MENU GUI 3 i GUI 2 (porównanie z baza-26/baza-32), POMOC, USTAWIENIA
po kliknięciu 2 przełączników, dowolna podstrona MMOCore (np. quest-list) z dołem i „Wróć do menu”, czat po 3× Q (pusty).

### Dopisek do bramki rundy 2 (po decyzji: własna implementacja podstron zamiast merge 41469ea — przyjęte)
- R1-8 (WYS, znana pułapka z menu-reszta): ekrany BIORĄCE przedmiot z EQ (Wystawienie na rynku/AH, warsztaty MMOItems)
  NIE mogą mieć ukrytego EQ — gracz musi widzieć i klikać swój plecak. Zrzut: Handel → Rynek → Wystawienie, EQ widoczne
  i przedmiot da się wystawić. Odwrotnie: każda strona z podstrony.strony ma EQ ukryte i NIEKLIKALNE (klik w dół nie
  przenosi przedmiotu), a po zamknięciu EQ jest identyczne (porównać zawartość przed/po).
- Klik w „Wróć do …” 5× szybko pod rząd nie może zostawić gracza z niewidocznym EQ poza menu (sprawdzić E po zamknięciu).

## Runda 2 — ocena (świeże zrzuty recenzenta: zrzuty/hubrev-02-*.png, ui-klon 12.10 ~10:40)

Werdykt: duży postęp, **H1 (podstrony) NAPRAWIONE** — dół z „WRÓĆ”, EQ ukryte, dymek „Wróć do menu głównego”.
MENU wróciło do jednowierszowych podpisów (R1-1 OK), czat po Q czysty (R1-2 OK), szybka podróż ma kłódki z opisem
odkrycia (R1-6 OK), pasek stanu z nazwą/wartością/akcją (R1-7 OK), dymki przełączników ze stanem „Teraz: …” (dobre).
Usunięcie TRYB PVP — SŁUSZNE, ale z innego powodu niż w raporcie: MMOCore `pvp_mode.enabled: false` (config.yml:373),
więc /pvpmode nic nie robi; `%cmi_user_inpvp%` to flaga walki CMI, nie stan przełącznika — popraw uzasadnienie w RAPORT.md.

| # | waga | gdzie | ocena | co zrobić |
|---|---|---|---|---|
| R2-1 | WYS | POMOC, tekstury hub_pomoc_1/2 (góra) | Na tle nadal wypalone „Q otwiera Księgę.” i kafel z rysunkiem przekreślonej skrzyni + podpis „WKRÓTCE”, a dymek tego kafla mówi „Pierwsze kroki” — obraz przeczy dymkowi (hubrev-02-pomoc, -pomoc-kroki). Zmieniono tylko ćwiartki 3/4. Zrzut r2-gui3-help w raporcie jest z 02:39, sprzed zmiany YAML (02:58). | Przerysować górne ćwiartki: opis „Q otwiera MENU. Najedź na kafel, by poznać opis.” i kafel PIERWSZE KROKI (ilustracja w rodzinie kafli, np. ślady stóp/drogowskaz, podpis wersalikami jak KLAWISZE), bitmapowa czcionka MC, siatka 2×2 teksele. Przejrzeć tła WSZYSTKICH podstron pod kątem wypalonego słowa „Księga”/„Księgę” jako nazwy menu i nieaktualnych kafli. |
| R2-2 | WYS | USTAWIENIA, układ | 4 ikony 16 px bez podpisów, ściśnięte u góry pustej strony, nierówne odstępy kolumn; jedyna podstrona bez kafli 2×2 z podpisem — sens tylko po najechaniu (przegrywa z dawnym kaflem TRYB PVP). | 4 kafle 2×2 jak w POMOCY (KARTA, CEL, MAPA, RADAR) narysowane w teksturze strony, ilustracje 32 px w rodzinie kafli; stan zostaje w dymku. Rozkład 2×2 kafli na prawej stronie, wyśrodkowany jak w MENU. |
| R2-3 | ŚR (proces) | RAPORT.md | Dowód musi być NOWSZY niż ostatnia zmiana pliku, którego dotyczy (R2-1 przeszło z nieaktualnym zrzutem). | Przy każdym punkcie: nazwa zrzutu + jego godzina ≥ mtime zmienionego pliku. |
| R2-4 | NIS (poza hubem, do HUD) | karta kompaktowa | „HP 25/25 Mana 0 / Wytrzymałość 20” jako goły tekst w ramce — działa, ale wygląda jak debug; „Mana 0” u klasy bez many? | Zgłaszam właścicielowi HUD; nie blokuje huba. |

Bramka rundy 3: hubrev-style zrzuty POMOC (bez dymka i z dymkiem PIERWSZE KROKI), USTAWIENIA (bez dymka + 1 dymek),
MENU GUI 3 (regresja), jedna podstrona MMOCore z WRÓĆ; godziny zrzutów po ostatnich zmianach plików.

## Runda 3 — ocena (zrzuty r3-gui3-help/-settings/-menu 11:21–11:24, siatka slotów nałożona przez recenzenta)

Werdykt: kierunek dobry (POMOC mówi „Q otwiera MENU”, USTAWIENIA mają kafle z podpisami, stan w dymku), ale
**wykonanie kafli jest niechlujne** — przy MENU, gdzie kafle leżą idealnie na siatce, USTAWIENIA wyglądają na zepsute.
Metoda pomiaru (powtórz sam przed oddaniem): zrzut GUI 3, linie co 54 px od lewego górnego slotu (717,262) — każda
krawędź kafla MUSI leżeć na linii, tak jak w r3-gui3-menu.

| # | waga | gdzie | ocena | co zrobić |
|---|---|---|---|---|
| R3-1 | WYS | USTAWIENIA, kafle | Każdy kafel UCIĘTY z prawej: brak prawej ramki i narożnych nitów, resztki ramki widać jako „⋮” przy KARTA/CEL/MAPA/RADAR; prawa kolumna dodatkowo przycięta brzegiem strony. | Rysować kafel DOKŁADNIE tą samą funkcją/szablonem co KLAWISZE/O MENU w POMOCY i kafle MENU (2×2 sloty = 108×108 px GUI 3 = 72×72 teksele 2×), bez przycinania. |
| R3-2 | WYS | USTAWIENIA, dolny rząd | Rząd MAPA/RADAR przesunięty ~1/3 slotu w dół; podpisy wchodzą w kolejny rząd slotów — rysunek nie pokrywa się z polem kliknięcia. | Kafle w rzędach slotów 0–1 i 2–3 (jak MENU: górny rząd 262–370, dolny 370–478 px przy GUI 3), bez przerwy między rzędami. Sprawdzić siatką. |
| R3-3 | ŚR | USTAWIENIA, ilustracje | Stara ikonka 16 px wklejona w dużą czarną skrzynkę — reszta kafli w grze ma pełnokadrowe sceny na niebieskim tle. | Ilustracje w rodzinie kafli MENU (pełny kadr, ta sama paleta): KARTA = portret na karcie, CEL = chorągiewka/tarcza celu, MAPA = rozwinięta mapa, RADAR = kompas z kropkami. |
| R3-4 | ŚR | POMOC, kafel PIERWSZE KROKI | Ilustracja nieczytelna (dwie jasne plamy na granacie, brązowy pas), podpis dwuwierszowy — jedyny taki w hubie. | Czytelna scena (np. ślady stóp na ścieżce do drogowskazu), podpis JEDNOWIERSZOWY (np. „NA START”, ≤ 8 znaków jak PROFESJE). |
| R3-5 | NIS | POMOC, opis po lewej | Pierwsza linia „Q otwiera MENU.” wyrównana inaczej niż dwie kolejne (zaczyna się przy krawędzi), za kropką jaśniejszy prostokąt — ślad po wymazanym tekście. | Wszystkie linie wyśrodkowane jak w USTAWIENIACH („Dopasuj grę do siebie.”); tło karty jednolite. |

Bramka rundy 4: r4-gui3-settings i r4-gui3-help z nałożoną siatką slotów (PNG obok zrzutu), GUI 2 obu stron,
dymek dowolnego kafla ustawień trafiony w ŚRODEK narysowanego kafla, MENU bez regresji.

## Runda 4 — ocena (zrzuty r4-* 13:23–13:33 z klon-ui-naprawa, siatka slotów nałożona przez recenzenta na surowe kadry)

Werdykt: **hub przechodzi bramkę — nigdzie nie jest gorzej niż na produkcji, w większości wyraźnie lepiej.**
R3-1/R3-2: kafle USTAWIEŃ pełne (ramki, nity), oba rzędy dokładnie na siatce 54 px (262–370–478), tak jak MENU.
R3-3: pełnokadrowe ilustracje w rodzinie MENU. R3-4: „NA START” czytelny, jednowierszowy, na siatce (sloty 24/25/33/34).
GUI 2 USTAWIEŃ czyste; dymek CEL ze stanem („Teraz: widoczny / Kliknij, aby ukryć”); MENU bez regresji.

| # | waga | gdzie | ocena | co zrobić |
|---|---|---|---|---|
| R4-1 | NIS (do poprawki przed wdrożeniem, bez nowej rundy) | POMOC, karta opisu (hub_pomoc_1/3) | Jasny prostokąt pod trzema liniami tekstu zamalowuje PRAWĄ ramkę karty (wewnętrzna jasna linia + brązowy kontur znikają od góry karty do końca 3. linii); 2. linia „Najedź na kafel,” jest szersza niż wnętrze karty. W USTAWIENIACH ta sama karta ma ramkę całą. | Przywrócić ramkę karty z wersji produkcyjnej i zmieścić tekst we wnętrzu (np. 4 wiersze: „Q otwiera MENU.” / „Najedź na kafel,” → skrócić do „Najedź na kafel” / „i poznaj opis.”). Dowód: jeden kadr POMOCY GUI 3 — recenzentowi wystarczy ten jeden zrzut. |
| R4-2 | INFO (sprawdzić na produkcji po wdrożeniu) | MENU, rząd szybkiej podróży | Na nowym klonie ostatnie miejsce to kłódka zamiast zielonego oka (Nexus) z poprzednich rund — prawdopodobnie skutek klona bez stolica_odnowa/creator_limbo, nie zmiany plików. | Po wdrożeniu: jeden zrzut MENU kontem QA na world1 — rząd = ognisko + punkty + Nexus jak wcześniej. |
| R4-3 | NIS (opcjonalnie) | USTAWIENIA KARTA/MAPA | Ilustracje to te same obrazki co POSTAĆ i MAPA w MENU — spójne, ale ten sam obraz znaczy dwie różne rzeczy. | Nie blokuje; ewentualnie później. |

Stan oceny: runda 1 → 3× gorzej; runda 2 → podstrony naprawione, 2 wady; runda 3 → kafle niechlujne; runda 4 → przechodzi.
Rekomendacja recenzenta: WDROŻYĆ hub po R4-1 (poprawka tekstury, bez pełnej rundy) + R4-2 jako kontrola po wdrożeniu.

### R4-1 — sprawdzone (r5-gui3-pomoc.png 14:08, tekstura hub_pomoc_1 14:05): NAPRAWIONE
Ramka karty opisu cała, tekst we wnętrzu. NIS na później (nie blokuje): wokół kafla NA START lekko jaśniejszy prostokąt
tła strony (pozostałość po dawnym WKRÓTCE). **Hub zatwierdzony przez recenzenta do wdrożenia;** po wdrożeniu R4-2 (zrzut MENU na world1).
