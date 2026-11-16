# Loch 002 — Bagna Zgniłych Korzeni (specyfikacja v0.2, 2026-09-30)

Komórka 002 siatki (`plan-100-lochow-10-wysp.md`): kolumna K2 **Rozkład** (statusy z jasną kontrą), Akt 1, poziom 2
(otwarcie od 1, zalecany 2–3), 10–15 min solo na Normalnym. Para na wyspie: strefa **Mokradła Rechotu**.
Źródło: stary `dungeon-ladder-002-010` (Mirebound Matriarch, Bog Leech, Rotroot Shambler, Mire Spitter, Bog Witchling)
przeniesione na nasz stos i poprawione listą kontrolną z krytyki Lochu 001.

## 1. Motyw: śluzy i Topielica

Rybacy z Przystani zbudowali groble i trzy śluzy. Topielica — Matka Mułu — zamknęła je i zalała bagno zgniłą wodą.
**Kto otwiera śluzy, osusza bagno i odbiera jej kryjówki.** Mechanika, której nie było w 001 (tam: światło latarni):
kołowrót osusza basen → odsłania sekret na dnie → zasypuje jedną ze studni, przez które Topielica wynurza się na arenie.

- **Zgniła woda**: stanie w wodzie = trucizna (odnawiana co 1 s). Kładki, groble i wysepki są bezpieczne. Lekcja
  statusu z kontrą widoczną od pierwszej minuty.
- **Śluza 1 (wymagana)** — krata w tamie przez przesmyk; otwarcie osusza basen pod mostem (wrak na dnie) i studnię A.
- **Śluza 2 (opcjonalna, zachód)** — osusza basen ze Skrzynią Topielca i studnię B.
- **Śluza 3 (opcjonalna, wschód)** — zardzewiała; rusza ją tylko **korba od rybaka Borka**, uwięzionego w kokonie
  w Kokoniarni. Osusza basen z Dzwonem Topielców i studnię C. Studnia D zostaje zawsze.
- **Łańcuch zależności**: Wela (start) prosi o znalezienie męża → Borek w kokonie (elita obok) → korba → śluza 3 →
  Dzwon → **odporność na truciznę do końca lochu** (nagroda-kontra dla lochu o statusach).
- **Po bossie** woda z jeziora na północy opada i odsłania całą zatopioną wieżę — **wejście do Lochu 003 (Archiwum)**.

## 2. Mapa (generator `forge/tools/make_loch002.py`, podgląd `forge/levels/loch_002_podglad.png`)

Teren w całości proceduralny (bez Irisa): baseny o dokładnych brzegach, groble, kładki, tama przez przesmyk.
Woda nigdy nie styka się z pustką (asercja generatora); wypiek `forge/tools/piec002.py` kontroluje grunt parserem regionów.
Gracz stoi na y=102 (ląd i kładki), lustro wody y=100. Mapa ~180 × 250 bl., trasa główna ~260 bl.

| # | Sekcja (x, z) | Co się dzieje | Lekcja |
|---|---|---|---|
| 1 | Przystań Rybaków (−56, 78) | lądowanie na pomoście, ognisko = punkt kontrolny, **Wela** przy ognisku, 3 Pijawki przy beczkach | bezpieczny start, pierwsza łatwa walka |
| 2 | Kładki Pijawek (−28, 62) | zasadzka: Pijawki wyskakują z wody po obu stronach kładki | zostań na deskach, woda truje |
| 3 | Wysepka Martwych Drzew (0, 52) | Kościej Bagienny na osobnej wysepce (14, 68) strzela zatrutymi strzałami; martwe pnie = osłony | zasięg widzenia |
| 4 | Przed Tamą (0, 38, plac r=10) | Mułowiec (ślad mułu spowalnia) + 2 Pijawki; kołowrót śluzy 1 | wyjdź ze śladu, bij z boku |
| 5 | Rozstaj (0, 0) | punkt kontrolny; odnoga zachodnia do śluzy 2 | wybór drogi |
| 6 | Chata Szeptuchy (34, −8; chata 45, 2) | Szeptucha osłania wywarem 2 Mułowce; kocioł z wywarem (sekret) | przerwanie kanału, kolejność celów |
| 7 | Śluza Zachodnia (−32, −6) | 2 Kościeje + 3 Pijawki; basen ze Skrzynią Topielca | opcjonalne, nagroda |
| 8 | Kokoniarnia (54, −44) | **Kokoniara** (elita): kokon unieruchamia gracza — rozbij go ciosami; młode przy 50%; kokon Borka | status z kontrą, elita |
| 9 | Śluza Wschodnia (66, −8) | Mułowiec + Szeptucha; kołowrót wymaga korby Borka | zależność |
| 10 | Grobla (22, −66) | ognisko = punkt kontrolny przed bossem, cisza | oddech |
| 11 | Topiel (0, −92) | arena r=22, zgniła sadzawka r=6 w środku, 4 studnie, 4 osłony; olbrzymie „ramię bagna” jako znak | wszystko naraz |

## 3. Przeciwnicy (wygląd zastępczy vanilla → modele później, jak w 001)

| Mob | Rola | Zastępczy wygląd | Zachowanie (Normalny) | Kontra |
|---|---|---|---|---|
| **Pijawka** | rój | endermit (oddycha pod wodą) | szybka, 1 obrażeń, ukąszenie czasem (20%) zatruwa | obszarowe ciosy, nie wchodź do wody |
| **Mułowiec** | bruiser | utopiec (drowned) | wolny, zostawia ślad mułu (spowolnienie) | wyjdź ze śladu, zabij, zanim zablokuje przejście |
| **Kościej Bagienny** | dystans | bogged (stoi w miejscu) | zatrute strzały z wysepek | osłona za pniem, podejście od boku |
| **Szeptucha** | zaklinacz | wiedźma | mikstury statusów + kanał **Wywaru** (3 s) osłaniający sojuszników | 3 ciosy w trakcie przerywają, zabij pierwszą |
| **Kokoniara** (elita) | mini-boss | pająk ×1,6 | **Kokon**: unieruchamia cel (4 s), potem trucizna; skok; przy 50% młode | rozbij kokon (3 ciosy), unik skoku |
| **Topielica, Matka Mułu** (boss) | boss | utopiec ×2,4 | §3.1 | §3.1 |

### 3.1 Topielica, Matka Mułu

| Faza | Próg | Mechaniki | Kontra |
|---|---|---|---|
| 1 | 100–60% | **Włosy Topieli** — linia z zapowiedzią 1 s, przyciąga cel (przeciw kitowaniu); **Kałuża** — krąg 1,2 s, potem trucizna 3 s; **Zanurzenie** — znika w losowej aktywnej studni, bąble 1,5 s, wynurza się z falą r=3 | zejdź z linii, wyjdź z kręgu, odsuń się od bulgoczącej studni |
| 2 | 60–25% | + **Skrzek** — przy każdej aktywnej studni kiść jaj; po 6 s wykluwa 2 Pijawki | rozbij jaja (kolejność celów); mniej studni = mniej jaj |
| 3 | < 25% | Gniew Topieli: częstsze Włosy | spokój, osłony |

**Zależność śluz**: studnie A/B/C osuszone przez śluzy 1/2/3 nie działają — Topielica ma mniej kryjówek
(bardziej przewidywalne Zanurzenie) i składa mniej Skrzeku. Pełen komplet śluz = studnia D jako jedyna.

## 4. Sekrety (5) i nagroda

| Sekret | Gdzie | Warunek | Daje |
|---|---|---|---|
| Wrak Rybaków | dno basenu 1 | śluza 1 | materiały + EXP |
| Skrzynia Topielca | dno basenu 2 | śluza 2 | materiały + EXP |
| Rybak Borek | Kokoniarnia | rozbić jego kokon | korba do śluzy 3 + EXP |
| Dzwon Topielców | dno basenu 3 | śluza 3 (korba) | odporność na truciznę do końca lochu + EXP |
| Wywar Szeptuchy | kocioł przy chacie | Szeptucha z chaty nie żyje | +4 serca osłony dla drużyny |

Nagroda końcowa dla całej drużyny: EXP + materiały bagienne; premia za 3, 4 i 5/5 sekretów (perła, esencja wody).
EXP całego przejścia ~1100 na Normalnym (krzywa klasy `poziom × 200`: to ~poziom 3 → 4). Łup tylko nasz (`PROF_*`).

| Źródło | Łup |
|---|---|
| Pijawka | `PROF_BEAST_SCRAP`, rzadko `PROF_VENOM_SAC` |
| Mułowiec | `PROF_CLAY_LUMP`, `PROF_FLAX_FIBER` |
| Kościej | `PROF_BEAST_BONE` |
| Szeptucha | `PROF_MEDICINAL_HERB`, `PROF_REDCAP` |
| Kokoniara | `PROF_SINEW`, `PROF_VENOM_SAC` |
| Topielica | `PROF_WATER_ESSENCE`, `PROF_CLAM`, rzadko `PROF_PEARL` |

## 5. Lista kontrolna z krytyki 001 (spełniona od v0.1)

- wejście z menu Q (kafelek w `hub_przygoda`) — przygotowane, wchodzi razem z wdrożeniem;
- wskazówki prawdziwe (kierunki sprawdzane z mapą), jedna wiadomość naraz, zero intro w środku walki;
- nagroda + EXP dla całej drużyny (`@PlayersInWorld`), sekrety liczone per instancja;
- boss nie do kitowania (Włosy przyciągają, Kałuża i Zanurzenie karzą stanie w miejscu);
- punkt kontrolny przed bossem (Grobla), upadek w pustkę = powrót do ogniska;
- polecenia MM z `execute in` + `distance` — nie trafiają w bossa z innej instancji.

## 6. Na później

Modele (Topielica, Pijawka, Mułowiec — wg `metoda-modelu-od-pomyslu`), dialogi Papera dla Weli i Borka, trudności
Łatwy/Trudny, strefa Mokradła Rechotu na wyspie (klucz do sekretnej komnaty lochu, mapa skarbu z lochu).

## 7. Stan budowy (2026-09-30)

Zbudowane i sprawdzone na serwerze testowym `mc-l002` (klon kuźni, `/opt/docker/mc/forge-l002`, zatrzymany po testach).
**Na world1 NIE wdrożone** — nowa mapa MD wymaga restartu; czeka na zgodę właściciela.

| Co | Gdzie |
|---|---|
| Teren (generator + asercje: woda szczelna, 2 bloki wolne nad trasą, budowle poza ścieżką, 16 celów osiągalnych suchą nogą) | `forge/tools/make_loch002.py` → `forge/levels/loch_002_spec.json`, podgląd `loch_002_podglad.png` |
| Wypiek w kuźni (świat-pustka `pustka_002`, kafelki `.nbt` z unikalną nazwą, kontrola parserem) | `forge/tools/piec002.py`; ostatni świat `loch_002_010512` |
| Mapa MD + moby i skille MM | `forge/tools/build_loch002_md.py <świat>` → `forge/levels/loch_002_md/` |
| Sonda mechanik (31 sprawdzeń) i test trasy botem | `forge/tools/sonda_l002/{uruchom.sh,sonda.js,trasa.js}` |
| Wdrożenie / wycofanie na world1 (mapa, pliki MM, kafelek w menu Przygoda, kopia w backups) | `forge/tools/wdrozenie_l002/{wdroz.sh,wycofaj.sh}` |
| Strona przeglądu | https://claude.ai/artifact/4pffaufBhMv7aJtpxCmKDg (źródła `output/lochy/siatka-strona/`) |

Wyniki: sonda 31/31 w dwóch przebiegach pod rząd, 0 ostrzeżeń MM, 0 linii trucizny w dzienniku. Bot z pathfinderem
przechodzi przystań → kładki → wysepkę → sam kręci kołowrotem → brama → most → Rozstaj → Chata; w gęstym mangrowcu
przed Kokoniarnią pathfinder grzęźnie (droga istnieje — potwierdza test przechodniości w generatorze).

Poprawki z testów (v0.1 → v0.2): korzenie mangrowców na kładkach (190 bloków), martwe drzewa i chata na ścieżce,
pajęczyna na trasie, staw przed bramą, kładki 3 → 5 bloków (skośne „ząbki” zrzucały do wody), Pijawki tonęły
(oddychanie pod wodą), rój Pijawek zabijał stojącego gracza w kilka sekund (1,5 → 1 obr.), wskazówka Borka
(śluza 3 jest na POŁUDNIE od Kokoniarni), trucizna wody przez metaskill z `TargetConditions` (inline `?inliquid`
sprawdzał znacznik, nie gracza), emitery wody przyzywane przy graczu, a nie z populacji.
