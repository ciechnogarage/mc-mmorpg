# Loch 001 — Kwietna Polana (specyfikacja v0.1, 2026-09-25)

Ulepszona wersja starego projektu (`level-1-dungeon-island-foundation`, `mob-boss-encounter-001`,
`loot-reward-table-001` w `~/Desktop/Przemek/MC/docs/`), przeniesiona na nasz stos. Parasol: `akt1-wyspa-i-lochy.md`.
Stan wyjściowy: `poziom_01` przechodzi e2e 19/19 w kuźni (wyspa, 3 zasadzki, boss-wilk, wyjście), bez treści własnej.

## 0. Co ulepszamy względem starego projektu

| Stary projekt | Tutaj |
|---|---|
| Osobne Training Grounds w Stolicy (której nie ma) | Manekin treningowy w obozie przybycia — uczy uniku i bloku przed pierwszą walką |
| Moby i boss bez wyglądu, nazwy angielskie | Konkretne modele LittleRoom (§3), polskie nazwy, nasze skille |
| Materiały wymyślone od zera | Najpierw istniejące `PROF_*`; nowe tylko tam, gdzie liczy się tożsamość lochu (§6) |
| Wejście przez Portal Nexus | Menu Q → Lochy → Loch 001 z wyborem trudności (Portal Nexus dojdzie ze Stolicą) |
| Trudności opisane słownie | Poziomy trudności MythicDungeons + warunek `dungeondifficulty` w skillach (§5) |
| Brak powiązania ze światem | Para ze strefą Kwietne Zbocza, opcjonalny sekret za klucz z wyspy (§7) |
| Brak punktów kontrolnych | Ognisko = punkt kontrolny MD (funkcja Checkpoint) w obozie i przed areną |
| Liczby „później” | Liczby z pomiaru botem (`forge/gracz/sonda_dps.js`) względem progów systemu walki (§4) |

## 1. Rola lochu

Pierwszy loch, poziom 1–5, 10–15 min solo na Normalnym. Uczy przez starcia, bez ściany tekstu: unik, blok,
parowanie, przerwanie, zasięg widzenia, kolejność celów, pozycja na arenie. Solo na Łatwym ma być realne dla nowego gracza;
drużyna ma łatwiej, ale nie jest wymagana. Klasa nie jest wymagana.

## 2. Układ wyspy (nowy obraz sterujący)

**Wersja 2 (2026-09-26, po przejściu starego poziom_01 przez właściciela, recenzji krytyka i researchu Wynn/Hypixel/Monumenta).**
Motyw lochu: **Światło kontra korzenie.** Gaj zamyka drogi korzeniami, światło latarni zmusza je do posłuszeństwa.
Strona przeglądu (mapa, oceny, rytm): https://claude.ai/artifact/Te4mF17Jcjku5HgEdbgavD (części „Poza walką — wersja 2” i „Przeciwnicy na mapie”).

- **Latarnie ×3 opcjonalne**: las (56,90), szczyt wieży Ciernia (48,18), Strefa Zasobów (-72,-2). Każda zapalona = filar światła na arenie
  (jednorazowo przerywa Korę albo wypala korzenie). Brama jest otwarta. `3 × TriggerInteract → InstanceVariable → MythicSkill`.
- **Boss z charakterem**: krata przy wejściu na arenę, sakura gaśnie, kwestia, przebudzenie z animacji; arena r≈20 z 4 osłonami;
  faza 2 korzenie zarastają dwie osłony, faza 3 zwężają brzeg (BlockEditor przy progach HP).
- **Tyrolka** z wieży Ciernia (48,18) → (-30,4), 79 bl. nad lądem (`littleroom_zipline` + pakiet MM „littleroom ziplines” — jest w paczce).
- **Most z korzeni** ze Strefy Zasobów (-58,-8) do Bramy (-18,-42) nad pustką, po zapaleniu latarni w Zasobach (odnoga przestaje być ślepa).
- **Ocena D–S** (czas, śmierci, przerwane kanały, sekrety) decyduje, ile skrzyń otworzy się pod sakurą; rzadki drop ogłaszany na czacie.
- **Upadek w pustkę** = teleport do ostatniego ogniska z karą 30% HP, nie koniec przejścia.
- **Kwiaty**: dekoratory łąki ×~0,2; gradient zdrowa łąka → zwiędła (od Rytuału) → szary gaj (za Bramą); rozkwit po bossie.
- **P2**: losowe wydarzenia, modyfikator tygodnia, trwała nagroda za pierwsze przejście, mechanika dla 2+ graczy.
- **Skreślone**: półka nad urwiskiem (pomysł na późniejszy loch), latarnie jako warunek Bramy, medal czasu.
- **Moby**: wszystkie typy zostają (decyzja właściciela), wymieszane w 13 starciach według cech — §3.2. Modele mogą się zmienić
  (przegląd stylu: górna półka LittleRoom), starcia są opisane rolami.

Obraz Iris jak dotąd: jeden kanał, strefy w równych pasmach (bez `rarity`, pierwszy region = pustka).
Obecne 7 stref przebudowujemy na 9 sekcji przebiegu. Kolejność to ścieżka gracza z południa na północ.

| # | Sekcja | Co się dzieje | Lekcja | Rekwizyty |
|---|---|---|---|---|
| 1 | **Obóz Przybycia** | lądowanie (powolne opadanie jak dziś), ognisko = punkt kontrolny, tytuł, manekin | bezpieczny start; manekin reaguje na unik i blok | bonfire_lit, lr_trainingdummy_2, lr_fence, lr_lamppost |
| 2 | **Leśna Ścieżka** | 2–3 Wilki Gajowe + 2 Korzeniaki | odstęp, unik po skoku wilka | — |
| 3 | **Zakręt Łuczników** | 2 Cierniowych Łuczników za przesłonami z kamieni i pni | zasięg widzenia, podejście od boku | skały, powalone pnie (obiekty Iris) |
| 4 | **Polana Rytuału** | Zaklinacz Gaju kanałuje osłonę na 2 wilkach | przerwanie, kolejność celów | krąg kamieni, magic_lantern |
| 5 | **Strefa Zasobów** (boczna, opcjonalna) | węzły ziół i kryształów, 2 Trujące Pełzacze, sekretne przejście | profesje, lekki status | hidden_treasure (za kluczem, §7) |
| 6 | **Złamana Brama** | Korowy Strażnik w wąskim przejściu | parowanie, atak od tyłu | ruiny bramy |
| 7 | **Legowisko Sowiedzia** (elita) | Sowiedź + 2 młode; ognisko-punkt kontrolny za nim | 2–3 lekcje naraz | bonfire |
| 8 | **Serce Gaju** (arena r=32, płaska jak dziś) | Strażnik Gaju, 3 fazy | wszystko naraz | korzenie na brzegach areny |
| 9 | **Wyjście z Nagrodą** | skrzynia (loot_chest → lootchest_glow po zwycięstwie), portal powrotu | domknięcie przebiegu | loot_chest, portal |

**Stan 2026-09-26:** teren upieczony w kuźni (`forge/tools/make_loch001.py`, ziarno 1001, `loch_metrics.py` 15/15 PASS,
zrzuty `output/lochy/loch001-teren/`). Wyspa to kręty szlak nad pustką — skrót na przełaj kończy się urwiskiem.
Osłony łuczników i gardziel bramy **nie** powstają z terenu (mieszanie wysokości ~14 bl. rozmywa wąskie formy) —
to budowle wklejane FAWE po upieczeniu. Teren będzie **zamrożony** w szablonie MD (gotowe regiony), więc Iris nie
generuje nic na produkcji (zatwierdzone przez właściciela 2026-09-26). Obecne `iris:poziom_01` generuje przy każdym wejściu — koszt CPU i ryzyko crasha.

**Budowle v4 (2026-09-26):** `forge/tools/budowle/loch001_budowle.py` — obóz (camp2/4), głazy łuczników (boulder6/13/4) + pnie,
krąg 6 obelisków, Złamana Brama (ruin3 + mur przez całą szyję wyspy, wyrwa ~6 bl.). Zrzuty `output/lochy/loch001-budowle/`.
Wklejane vanillowym `place template` z konsoli (bez FAWE).

Wymagania terenu (sprawdzane parserem `mca.py` i `soak.sh` na 8 ziarnach): ścieżka ciągła i przejezdna bez skakania
po blokach > 1; każda sekcja ma miejsce na uniki (min. 8×8 płaskiego gruntu); arena zakryta ≤ 5%; lity blok w pustce = 0.

## 3. Przeciwnicy

Model = z paczki LittleRoom (geometria, animacje, dźwięk). Statystyki, skille walki, łup i nazwy — nasze.
Przegląd paczki: `output/lochy/przeglad-lr/`. Własne modele: `tools/modele/` (README). Ostateczna ocena w kliencie QA po wdrożeniu.

| Nasz mob | Rola | Model | Zachowanie (Normalny) | Kontra |
|---|---|---|---|---|
| **Wilk Gajowy** | szybki, zwarcie | `direwolf` (LR) | okrąża, skok z zapowiedzią | unik w bok, okno po skoku |
| **Pniaczek** | rój | własny `pniaczek` (gremlin z pnia, zielone oczy) | szybkie dreptanie, stadem | obszarowe ciosy, ruch |
| **Korzeniak** | strefa | własny `korzeniak` + `korzeniak_strefa` (czerwone oko jak boss) | przysiada i rozlewa strefę korzeni (slow) | wyjść ze strefy, zabić w czasie przysiadu |
| **Cierniowy Łucznik** | dystans | własny `cierniowy_lucznik` (naciąg 0,7 s, odskok) | strzela z widocznym naciągiem, odskakuje | zasłona, unik, doskok |
| **Muchomorzec** | zaklinacz | własny `muchomorzec` (kanał osłony z kulą zarodników) | kanałuje osłonę na sojusznikach | przerwanie, kolejność celów |
| **Korowiec** | tarcza | własny `korowiec` (garda korowymi tarczami) | garda z przodu, ciężkie uderzenie z góry (0,6 s zapowiedzi) | obejście, parowanie, mocny atak |
| **Purchawiec** | status | własny `purchawiec` (nabrzmiewa i wypluwa chmurę, rzuca purchawką) | chmura trucizny, rzut | nie stać w chmurze, unik rzutu |
| **Purchawka** | kamikaze | własny `purchawka` (0,8 s nabrzmiewania, pęka) | podskakuje do gracza i wybucha chmurą | odejść w czasie nabrzmiewania |
| **Sowiedź** (elita) | mini-boss | `lr_owlbear` + młode (LR) | jak wyżej | jak wyżej |
| **Strażnik Gaju** (boss) | boss | `dreadroot` (LR) | §3.1 | §3.1 |

Wilki i dziki z obecnego `poziom_01` (`WIEZA_P01_*`, vanilla Type) zastępujemy tymi mobami.

### 3.1 Strażnik Gaju

Model `dreadroot` ma gotowe: budzenie przy zbliżeniu, 2 zamachy ramionami z zapowiedzią, korzenie-więzienie wokół gracza,
gaz bagienny. Dopisujemy fazę 2 i szał.

| Faza | Próg HP | Mechaniki | Kontra |
|---|---|---|---|
| 1. Przebudzenie | 100–60% | **Uderzenie Korzeni** (zamach z przodu, zapowiedź 1,2 s); **Linia Pnączy** (liniowa strefa korzeni); przywołanie 1–2 Korzeniaków | unik/blok, zejście z linii, najpierw addy |
| 2. Rytuał Gaju | 60–25% | **Rozkwit** (kręgi na ziemi, wybuch po 1,5 s); **Kora** — kanał osłony/leczenia 4 s, przerywalny; **Salwa Cierni** (wolne pociski) | ruch, przerwanie, zasłona za korzeniami na brzegu areny |
| 3. Gniew (lekki) | < 25% | szybsze uderzenie (zapowiedź 0,9 s), puls korzeni (spowolnienie 2 s), max 3 Korzeniaki naraz | spokój, zasoby, znane kontry |

Zasady (ze starego projektu, bez zmian): brak losowego one-shota, brak łańcucha unieruchomień, addy nie rosną w nieskończoność,
kanał zawsze widoczny, arena z miejscem na ruch.

### 3.2 Starcia (wersja 2)

Zasada: każdy mob najpierw sam albo w prostym zestawie (lekcja kontry), potem wraca w grupach, gdzie cechy dają nowy problem.
Warianty B losuje funkcja MD `Random` przy każdym wejściu, osobno per starcie. Łatwy: każda grupa o 1 moba mniej, K i L bez dodatkowej fali.
Wystąpienia w składzie głównym: Pniaczek 6, Korzeniak 5, Purchawka 4, Korowiec 3, Łucznik 3, Wilk 2, Purchawiec 2, Muchomorzec 2, elita 1.

| | Miejsce (x,z) | Skład (Normalny) | Dlaczego razem | Wariant B |
|---|---|---|---|---|
| A | wejście do lasu (24,98) | 12 Pniaczków | chwila mocy: rój pada od 1–2 ciosów | 8 Pniaczków + ukryta Purchawka |
| B | polana w lesie (38,74) | 2 Wilki + 3 Pniaczki | Pniaczki oblepiają, wilk czeka na okno do skoku | 3 Wilki (sfora okrąża) |
| C | wyjście z lasu, szkielet (55,57) | 1 Korzeniak | lekcja strefy; losowo klucz ze szkieletu budzi 2 Wilki | Korzeniak + zasadzka 2 Wilków |
| D | łąka przed obozem (58,42) | 1 Korowiec | garda z przodu, miejsce na obejście | Korowiec + 2 Pniaczki |
| E | Obóz Ciernia (46,22) | Korowiec w bramie + 2 Łuczników (1 na wieży) + 3 Pniaczki | tarcza z przodu, łucznicy za nią: front to pułapka | 3 Łuczników + Korzeniak w bramie |
| F | lądowanie tyrolki (-34,-2) | Korzeniak + 2 Purchawki | spowolnienie trzyma, kamikaze nadbiega | 2 Korzeniaki na krzyż |
| G | Strefa Zasobów (-62,4) | Purchawiec + 3 Purchawki | chmury miotacza, Purchawki toczą się przez nie | 2 Purchawce na krzyż |
| H | Polana Rytuału (6,-14) | Muchomorzec + 2 Wilki + Korzeniak przy zaklinaczu | osłonięci szybcy, strefa broni zaklinacza | Muchomorzec osłania Korowca |
| I | droga do Bramy (2,-32) | Korowiec + Purchawiec za nim | obejście tanka pod gradem chmur | Korowiec + 2 Łuczników |
| J | Złamana Brama (0,-48) | 2 Łuczników na murze + 6 Pniaczków w wyrwie + 1 Purchawka | przesmyk pod ostrzałem; za graczem korzenie zamykają powrót | 2 Łuczników + 2 Wilki |
| K | szary gaj (-10,-62) | Muchomorzec + Łucznik + Korzeniak („trzy linie”) | egzamin: strefa, dystans, osłona | Muchomorzec + Purchawiec + 3 Pniaczki |
| L | Legowisko Elity (0,-72) | Sowiedź + 2 młode; przy 50% HP 4 Pniaczki z drzew | czysta siła + flanki + zmiana planu | przy 50% 2 Purchawki |
| M | Serce Gaju (0,-112) | f1: 1–2 Korzeniaki · f2: Purchawki przez kręgi Rozkwitu · f3: fale Pniaczków, max 3 Korzeniaki | finał z rodzin, które gracz zna | f2: Muchomorzec próbuje osłonić bossa |

## 4. Liczby

Źródło progów: `docs/superpowers/specs/2026-09-20-combat-system-design.md`.

- zwykły mob ginie po 4–6 trafieniach gracza poziomu 1–3 w startowym sprzęcie;
- gracz przeżywa 5–7 zwykłych trafień;
- zapowiedziany ciężki atak zabiera 25–35% HP (Sowiedź, boss); nigdy > 40% na Normalnym;
- boss solo na Normalnym: 2–3 min; Sowiedź: 45–75 s.

HP i obrażenia **mierzymy**, nie zgadujemy: `sonda_dps.js` mierzy obrażenia na sekundę bota w startowym sprzęcie,
z tego wyliczamy HP; potem przejście botem sprawdza czasy.

## 5. Trudności

Wbudowane w MythicDungeons (`Difficulty.EnableDifficultyLevels` + menu wyboru). Na start dwa poziomy:

| | Łatwy | Normalny |
|---|---|---|
| Mnożniki MD | HP ×0,7, obrażenia ×0,7, liczba mobów ×1 | ×1 / ×1 / ×1 |
| Zachowanie (`?dungeondifficulty` w skillach) | dłuższe zapowiedzi (+30%), bez Salwy Cierni, max 2 Korzeniaki | pełny zestaw |
| Łup | niższa wartość, bez rzadkiego przedmiotu z bossa | pełna tabela |

Trudny i Bardzo Trudny projektujemy później: nowe zachowania (zmyłki, synchronizacja addów), nie tylko mnożniki.

## 6. Łup

Zasada serwera: łup tylko nasz (MMOItems / `PROF_*`), zero vanilli.

| Źródło | Zwykły łup | Rzadszy | Powiązanie |
|---|---|---|---|
| Wilk Gajowy | `PROF_BEAST_SCRAP`, `PROF_BEAST_BONE` | **Kieł Wilka Gajowego** (nowy, zadanie) | Łowiectwo, Krawiectwo |
| Korzeniak / Strażnik | `PROF_RAW_TIMBER` | **Spaczony Korzeń** (nowy) | Zielarstwo, Alchemia |
| Zaklinacz Gaju | `PROF_ENCHANT_DUST` | `PROF_ROUGH_CRYSTAL`, `PROF_RUNE_BLANK` | Zaklinanie, Jubilerstwo |
| Korowy Strażnik | `PROF_OAK_TIMBER`, `PROF_SPRUCE_RESIN` | baza tarczy | Stolarstwo, Kowalstwo |
| Trujący Pełzacz | `PROF_VENOM_SAC` | — | Alchemia (antidotum) |
| Strefa Zasobów | `PROF_MEDICINAL_HERB`, `PROF_MOONLEAF`, `PROF_ROUGH_CRYSTAL` | mała skrzynka materiałów | profesje zbieracze |
| Sowiedź | paczka materiałów, złoto | sprzęt magiczny, fragment Żetonu Gaju | wiele |
| Strażnik Gaju | **Żeton Gaju** (gwarantowany), materiały | **Rdzeń Strażnika**, rzadki przedmiot, **Pieczęć Lochowa** (Chowańce, rzadko) | ulepszanie, receptury Aktu 1 |

Przed wdrożeniem sprawdzić w recepturach, czy każdy użyty `PROF_*` to surowiec, a nie wynik rzemiosła.
Sprzęt: rodzina „lochowa” — równoważna tierem i siłą rodzinie „wyspiarskiej” ze strefy Kwietne Zbocza, inne przedmioty i wygląd.
Zwykłe i magiczne bazy istniejących typów MMOItems z prefiksem `L001_`. Nie najlepszy w slocie; bez one-shota i bez leczenia bez końca w PvP.
Żeton Gaju działa jak zabezpieczenie przed pechem: wymiana u handlarza na bazę albo materiał, nie na najlepszy przedmiot.
Łup ze skrzyni = tabela łupu MD (bonus zależny od trudności); łup z mobów = tabele dropów MM z warunkiem `dungeondifficulty`.

## 7. Powiązanie z wyspą (opcjonalne)

- **Klucz Starego Dębu** — znajdowany w strefie Kwietne Zbocza. Otwiera sekretne przejście w Strefie Zasobów
  (`KeyItems` MD + `hidden_treasure`). Nagroda: dodatkowa skrzynka materiałów i kosmetyk. Bez klucza loch jest pełny.
- **Mapa Kwietnych Zboczy** — rzadki łup z Sowiedzia; prowadzi do skarbu na wyspie.
- Receptura mieszana: Rdzeń Strażnika (loch) + zioło z wyspy → lepszy wariant mikstury lub amuletu.

## 8. Wejście i wyjście

- Menu Q → kafelek **Lochy** → okno z Lochem 001: opis, zalecany poziom, wybór trudności, podgląd łupu → `md play` z konsoli.
  Zero komend wpisywanych przez gracza.
- Wyjście: portal przy skrzyni → powrót na spawn świata (później: Stolica).
- Cooldown na łup ze skrzyni (MD `LootCooldown`): propozycja 1 pełna nagroda na 20 h na trudność, mniejsza nagroda bez limitu.

## 9. Assety do przygotowania

| Asset | Źródło | Uwagi |
|---|---|---|
| 8 modeli mobów + fx | LittleRoom | tylko te 8; dźwięki razem z modelem |
| Przemalowanie boreal_druid, slime_creep, (Korzeniak) | nasz warsztat | wg karty stylu |
| Rekwizyty: ognisko, manekin, skrzynia, latarnia, skarb | LittleRoom | |
| Obiekty Iris: ruiny bramy, krąg kamieni, powalone pnie, skały | nowe | przez edycję istniejących obiektów, nie od zera |
| Ikony nowych przedmiotów (Kieł, Korzeń, Żeton, Rdzeń, Klucz, Mapa) | render 3D → piksele, 16×16 | wg `rozmiary-tekstur-menu-vs-przedmioty` |
| Kafelek menu „Lochy” + okno lochu | Nexo, 32×32 | |

## 10. Kolejność budowy (każdy krok kończy się oceną właściciela)

1. Przegląd modeli w kliencie QA: arkusz zrzutów 8 kandydatów + wybór Korzeniaka i Łucznika.
2. Nowy obraz układu + pack Iris (mutacja `poziom_01`) + obiekty; `soak.sh` na 8 ziarnach; zrzuty klientem.
3. Moby zwykłe (4) z łupem; pomiar liczb botem.
4. Sowiedź.
5. Strażnik Gaju, 3 fazy.
6. Trudności, skrzynia, portal, punkty kontrolne.
7. Menu wejścia + ikony.
8. Test na klonie (`chw-proba`) botem bez OP: pełne przejście na obu trudnościach, łup w ekwipunku, wyjście.
9. Gra właściciela → poprawki → produkcja.

## 11. Test gotowości (definicja „dopieszczony”)

- Bot bez OP wchodzi z menu, przechodzi 9 sekcji na Łatwym i Normalnym; każda sekcja ma sprawdzenie PASS/FAIL.
- Każdy przeciwnik ma zapowiedź ataku widoczną w pakietach (stan animacji / cząsteczki) przed obrażeniami.
- Boss przechodzi przez 3 fazy; kanał Kory da się przerwać; brak obrażeń > 40% HP jednym trafieniem na Normalnym.
- Łup trafia do ekwipunku, tylko przedmioty nasze; skrzynia respektuje cooldown.
- Zero błędów MM/MD/ME w logu podczas przebiegu; TPS bez spadków w trakcie walki z bossem.
- Właściciel przeszedł sam i zaakceptował.
