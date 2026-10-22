# Loch 001 — Kwietna Polana (karta szczegółowa v2, Projektant C, etap 5)

Wiąże: 05-synteza-v2 (R1–R5), 07-wspólne-liczby (L1–L7), recenzje 06-Y i 06-P (P6: pierwszy loch gry, odchudzony).
Zachowane z obecnego 001: GAJ_WILK / GAJ_KORZENIAK / GAJ_LUCZNIK v8 (`forge/tools/przeciwnicy/README.md`), Sowiedź,
nastroje dnia i ocena (przepisane), świt po bossie, kafelek w menu Przygoda.
Zastąpione: teren (kręty grzbiet nad pustką = „puste przestrzenie”), latarnie (motyw Aktu 4), 13 paczek bez motywu,
Stary Korzeń. Liczby wg L2: poziom 1 = Gaj v8 ×1 (Wilk/Korzeniak HP 40, Łucznik 26); boss 360, elita 90.

## 0. Nagłówek
| | |
|---|---|
| Poziom / otwarcie | 1 / od razu (pierwszy loch gry) |
| Czas Normalny solo | 12–14 min (L4) |
| Fantazja | „Wchodzę do sadu, który zakwitł za bardzo, i uczę się, że tu nawet śmierć kwitnie.” |
| Emocja | zachwyt z rysą niepokoju → triumf przy Bramie → rozkwit po bossie |
| Motyw | **Kiełki: deptać czy pozwolić wyrosnąć.** Loch uczy prawa aktu w czystej postaci. |
| Skutek dojrzałego pąka (L5) | z MAŁEGO poległego (wilk, łucznik, Siewca, Pniaczek, młode) wyrasta **Pniaczek**; z DUŻEGO (Korzeniak, Korowiec, konar bossa) wyrasta **Korzeń**. Korzeń to krąg 3×3 na 8 s, który spowalnia i trzyma WSZYSTKO, co w nim stoi, także wrogów; przy murze działa jak taran, pod bossem pęta. Pąk jest ten sam (model, barwa, dźwięk, czas). Duży poległy zostawia pąk w skali ×1,5 (P6: dozwolone, bez innej barwy). |

**Protokół R1 (wspólny, wykonanie w §13):**
- Jeden pąk na zgon. Czasy dojrzewania stałe: 5 s (Łatwy 8, Trudny 4); żaden nastrój ich nie zmienia. Ostatnią 1 s pąk
  pęcznieje i świeci.
- Dowolne trafienie usuwa pąk: LPM, PPM, zaklęcie, pocisk, przywołaniec, Chowaniec. Przywołańce i Chowaniec depczą też,
  przechodząc obok (r1).
- Limit 12 na instancję w kolejce według czasu powstania: 13. pąk wypiera NAJSTARSZY, który widocznie usycha (1 s
  szarzenia + `block.azalea_leaves.break`).
- Pąk poległego gracza dojrzewa w Cierń (przeszkoda), a kto go zdepcze, dostaje Płatek ochrony. Do czasu testu przepływu
  śmierci gracza obowiązuje plan B: śmierć gracza nie zostawia pąka.

## 1. Kishōtenketsu motywu (bez tekstu)
| Etap | Sala | Co gracz widzi i robi | Jak rozumie bez napisu |
|---|---|---|---|
| Wstęp | S1 Obóz | Manekin (`lr_trainingdummy_2`) „pada” od 3 ciosów i zostawia pąk. Po 5 s pąk pęcznieje i wyskakuje Pniaczek (HP 6, przez 10 s nie bije). Przy drugim manekinie gracz już wie, że może pąk trafić. | pęcznienie i świecenie w ostatniej 1 s; Pniaczek wybiega wprost z pąka |
| Rozwinięcie | S2 Rozstaje, S3W Ruiny | Wilki → pąki → Pniaczki, czyli deptanie pod presją. **Pierwszy duży pąk to pokaz**: Korzeniak stoi pod spróchniałą belką, gracz tnie linę (dowolne trafienie), belka zabija Korzeniaka. Z dużego pąka wyrasta Korzeń, w który wbiega wilk z zasadzki i zostaje przyszpilony. | gracz widzi duży pąk bez presji walki, a potem wroga uwięzionego przez dojrzały pąk |
| ZWROT | S5 Brama Wzgórza | Mur z korzeni nie ma klucza ani dźwigni. Pilnuje go Korowiec. Zabity przy murze zostawia duży pąk; jeśli go NIE zdepczesz, Korzeń-Taran rozsadza mur. Ale obok padają Pniaczki z korzeni muru i ich pąki też dojrzeją. | pęknięcia muru pulsują; w S3W gracz widział Korzeń kruszący skałę (sekret) |
| Egzamin | S8 Dzwonnica | Strażnik wciąga do siebie Pniaczki i się nimi leczy (depcz małe pąki). Korzeń dojrzały przy nim pęta go na 4 s (zostaw duże). | korona Strażnika rozkwita przy leczeniu; spętany klęczy, serce świeci |

## 2. Historia środowiskiem
- Osada Sadowników: zrujnowane chaty, pnie ze śladami szczepienia (nacięcia owinięte łykiem), dzwonnica oplatana sakurą
  na szczycie wzgórza. Dzwon wzywał na zbiór; dziś bije, gdy korzeń szarpnie linę.
- Gradient: dół doliny to zdrowy sad, przy Bramie kwiaty przerośnięte (płatki jak dłonie, spuchnięte pnie), szczyt jest
  różowy za bardzo. Po bossie sakura gubi nadmiar i zostaje zwykły kwitnący sad.
- **Odkrycie wątku (samodzielne):** przy Kotwie Zachodniej leżą zwłoki Ogrodnika wrośnięte w pień. W dłoni nóż
  szczepiarski z pierścieniem-gałęzią (znak Bractwa), w drugiej ręce sadzonka, która wciąż kwitnie. Opis przedmiotu:
  „Nóż Ogrodnika. Ktoś szczepił ten sad czymś, co nie przestaje rosnąć.” → Kronika 1 na Mapie Granicy.
- NPC **Jaro, ranny zielarz** (przy ognisku, dialog Papera, maks. 3 kwestie):
  1. „Nie zostawiaj tego, co padło. Tu wszystko wraca — tylko inne.”
  2. (po Bramie) „Pozwoliłeś mu urosnąć? Odważnie. Albo głupio.”
  3. (po bossie) „Słyszysz? Dzwon ucichł. Pierwszy raz od żniw.”

## 3. Mapa (1 znak ≈ 4 bl., północ = góra = −Z)
```
                    [S8 DZWONNICA — arena 30x30, y+18]  <- sakura i dzwon widoczne od S1 (weenie)
                              |  S7 ścieżka-cisza, 24 bl. pod górę
                    [S6 ZBOCZE SOWIEDZIA 28x28, y+10..+16] --(skrót: korzeń-winda)--> ognisko2
                              |
                 [S5 BRAMA WZGÓRZA plac 26x20, y+8]  *ognisko2 po otwarciu*
                /       (Źródełko 12x10, odpoczynek)        \
   [S4W KOTWA ZACH. 22x22, y+6]                 [S4E KOTWA WSCH. 22x22, y+6]
              |                                          |
   [S3W RUINY OSADY 30x24, y+2] ====kładka 24 bl.==== [S3E SAD NA TARASACH 32x26, y+3/+6/+9]
                \                                       /
                 \------[S2 ROZSTAJE 20x16, y+1]-------/
                              |
                     [S1 OBÓZ 24x20, y0] *ognisko1*  (lądowanie od południa)
```
- **Widły**: dwie doliny w dowolnej kolejności (wybór „co, nie jak”). Każda kończy się Kotwą, czyli pulsującym węzłem
  korzeni (nieruchomy cel, HP 30, nie atakuje), który karmi mur Bramy. Dopóki stoją obie Kotwy, mur jest gruby
  i pęknięcia są zgaszone (Taran nie przebije). Kotwy widać z Rozstajów jako dwie różowe łuny.
- **Pętla**: kładka S3W↔S3E na wysokości Rozstajów. **Odpoczynek**: Źródełko między Kotwami a Bramą (woda, płatki, brak
  wrogów). **Skrót**: po S6 korzeń-winda (MD, platforma 3×3) do ogniska2.
- **Pionowość**: Obóz y0, tarasy Sadu do y+9 (łucznicy wyżej), Brama y+8, arena y+18; sakura 22 bl. nad areną. Upadek
  ze zbocza = powrót do ostatniego ogniska z −30% HP.
- Całość ~120 × 150 bl.; żadnego odcinka >25 bl. bez celu (starcie, łup, widok, wybór).

## 4. Beat chart (Normalny solo)
| Sala | Rola | Min | Skład (dlaczego) | Start starcia | Tempo | Poza walką |
|---|---|---|---|---|---|---|
| S1 Obóz | nauka, WOW | 0–1,5 | 2 manekiny, Pniaczek z pąka | brak | niskie | Jaro (1), WOW dzwonu w 1. min |
| S2 Rozstaje | nauka | 1,5–2,5 | 2 Wilki śpiące pod pagórkiem — 1 typ = lekcja skoku; ich pąki uczą deptania | gracz zaskakuje z góry | średnie | dwie łuny Kotw |
| S3W Ruiny | rozwinięcie | 2,5–4 | Korzeniak pod belką (pokaz dużego pąka) + 1 Wilk z zasadzki w Korzeń | widok, potem wróg zaskakuje | średnie | sekret: pęknięta skała |
| S4W Kotwa Zach. | test | 4–5,5 | Kotwa + **Siewca** (jedyne miejsce) + Korzeniak | gracz wybiera (Siewca stoi) | wysokie | zwłoki Ogrodnika (odkrycie) |
| kładka | odpoczynek | 5,5–6 | — | — | niskie | łup z pnia |
| S3E Sad | rozwinięcie | 6–7,5 | 2 Łuczników na tarasach +3/+6 i 2 Pniaczki budzone strzałami — 2 typy | widok i wejście pod ostrzałem (pnie, kosze) | wysokie | sekret: dziupla |
| S4E Kotwa Wsch. | test | 7,5–8,5 | Kotwa + Łucznik na pniu + 3 Pniaczki z kwiatów | wróg zaskakuje | wysokie | ul na sakurze widoczny z pnia |
| Źródełko | odpoczynek | 8,5–9 | — | — | niskie | woda, płatki, widok na Bramę |
| S5 Brama | ZWROT | 9–10 | Korowiec Bramy (jedyny Korowiec) + 3 Pniaczki z muru | widok i podejście (budzi się na placu) | szczytowe | ognisko2 po przebiciu |
| S6 Zbocze | elita | 10–11 | Sowiedź + 2 młode | wejście bez powrotu (krata z korzeni) | wysokie | wybór z młodymi; skrót |
| S7 Ścieżka | odpoczynek | 11–11,5 | — | — | niskie | Kwiat bez cienia (sekret) |
| S8 Dzwonnica | egzamin | 11,5–13,5 | Strażnik Sadu (§6); maks. 1 Korzeniak, Pniaczki z korony | widok i wejście bez powrotu | szczytowe | — |
| S9 Rozkwit | nagroda | 13,5–14 | — | — | niskie | skrzynia, ul z pieczęcią Pasieki |
Zabójstwa w 10 s (R1.5): S2 ≤2, S3W ≤2, S4W ≤3, S4E ≤4, S5 ≤4, boss ≤4. Pąków żywych naraz ≤6 (limit 12 to bezpiecznik).
Typy wrogów pojawiają się po jednym: Wilk (S2), Korzeniak (S3W), Siewca (S4W), Łucznik (S3E), Korowiec (S5), elita (S6).

## 5. Bestiariusz (poziom 1 = Gaj v8 ×1)
| Mob | Rola | Zastępczo / kandydat | HP / obrażenia | Zapowiedź | Atak karzący | Kontra | Nieuważny vs uważny |
|---|---|---|---|---|---|---|---|
| Wilk Gajowy | zwarcie | WOLF / `direwolf` | =GAJ_WILK v8: 40, skok 9 | `entity.wolf.growl` p0.5, 0,9 s, cel zamknięty | skok w punkt | krok w bok, 1,2 s oszołomienia | v8: n traci 3–5× więcej |
| Korzeniak | strefa | ZOMBIE / własny `korzeniak` | =GAJ_KORZENIAK v8: 40, Uścisk 10 r3,5 | `block.hanging_roots.break` 1 s | Uścisk wokół siebie | odskok ≥3,5, 1,5 s odsłony | v8: n 20–23 HP i 2/4 zgony, u 0 |
| Cierniowy Łucznik (jedyny humanoid) | dystans | SKELETON / własny `cierniowy_lucznik` | =GAJ_LUCZNIK v8: 26, strzał 7 + dobicie 4 w ten sam punkt | `item.crossbow.loading_start` 1,1 s | 2 strzały w zamknięty punkt | osłona, krok w bok, doskok | v8: n 6–11% / u 2% |
| Pniaczek | rój z pąków | mały HUSK Baby / własny `pniaczek` | 6; ugryzienie 1 co 1,2 s | `block.azalea.step` + podskok 0,5 s | oblepia (3 naraz = 3/s) | obszarowe, deptanie pąków | n zalany falą, u depcze |
| Siewca | priorytet | nieruchomy HUSK z hełmem-dmuchawcem / własny | 20; Puch: 3 kule w zamknięty punkt, 2 + Spowolnienie I 2 s | `block.big_dripleaf.tilt_down` 0,8 s | spowalnia, wilk dopada | podejście bokiem; **jego pąk dojrzewa w 3 Pniaczki** (kula puchu widoczna nad pąkiem = zapowiedź) | n: fala 3 Pniaczków |
| Korowiec | tarcza | ZOMBIE ×1,3 / własny `korowiec` | 60; Odgórne uderzenie 8 | `entity.iron_golem.attack` p0.6, 1,0 s | pole 2×3 przed nim | garda −80% z przodu 120°; obejście; 1,5 s bez gardy po ciosie | n 40 s w tarczę; u 12–18 s |
| Kotwa | cel | niewidzialny ZOMBIE z blokiem / `dreadroot_root` | 30, bez ataku; co 10 s budzi 1 Pniaczka | puls `block.chorus_flower.grow` | — | dowolne obrażenia | — |
Siewca a R1: zostawia JEDEN pąk (jak każdy). Skutek dojrzenia tego pąka to 3 Pniaczki (krewni z puchu), bo nad pąkiem
unosi się jego puchowa głowa. Gracz widzi przyczynę na poległym, a liczba pąków się zgadza.

## 6. Elita i Boss
### Sowiedź (elita, S6) — HP 90 (L2: 25% bossa)
- Funkcja: pierwsza „ściana” przed bossem, test ruchu.
- Haczyk: **po pudle skoku odsłania bok na 2 s, a młode próbują ją osłonić** (kolejność celów albo okno).
- Wygląd: `lr_owlbear` + 2× `lr_owlbear_cub`; słaby punkt to jasna pierś.
| Zdolność | Zapowiedź | Czas | Cel zamknięty | Kara | Okno |
|---|---|---|---|---|---|
| Skok | `entity.polar_bear.warning` + wspięcie | 1,0 s | tak | 9 + odrzut | 2 s po pudle |
| Zamach łapą (90°, r4) | przysiad | 0,8 s | tak | 6 | 1 s |
| Wołanie młodych (50%) | `entity.parrot.imitate.ravager` p0.6 | 1,5 s | — | 2 młode (HP 12, gryzą 2) | młode giną → matka odsłonięta |
Arena 28×28 z 3 pniami-osłonami; cel 25–35 s uważnie.
**Wybór**: młode po śmierci matki uciekają do dziupli, jeśli gracz przez 10 s ich nie trafi. Materiały profesji z elity
są w obu przypadkach te same. Różni się tylko kosmetyk (pióro za łaskę, kieł za dobicie) i kwestia Jaro.

### Strażnik Sadu (boss, S8) — HP 360 (L2: ~2 min uważnie przy DPS 3)
- **Funkcja**: egzamin z prawa: decyzja o pąkach w chaosie walki. Emocja: groza, potem triumf.
- **Haczyk**: „Strażnik wciąga do siebie Pniaczki i się nimi leczy — depcz małe pąki; ale Korzeń dojrzały przy nim pęta
  go na ziemi — duże pąki zostaw.” Pętanie jest bonusem (4 s, +50% obrażeń), nie warunkiem: bossa da się pokonać samymi
  oknami po ataku.
- **Historia**: ostatnia drzewna straż osady, przerośnięty Szczepem dąb. Pilnuje dzwonnicy, bo kazano mu pilnować żniw.
- **Wygląd**: `dreadroot` z koroną sakury; jedyny najjaśniejszy punkt to różowe serce-pąk w piersi.
- **Arena**: plac 30×30 (r≈14), dzwonnica i sakura na północnym brzegu; 4 osłony (2 kamienne ławy, ruina chaty,
  studnia). Mur korzeni zamyka wejście (MD BlockEditor) do końca walki.
| # | Zdolność | Faza | Zapowiedź | Czas | Cel zamknięty | Kara | Okno |
|---|---|---|---|---|---|---|---|
| 1 | Zamach Korony (stożek 100°, r5) | 1–3 | `entity.ravager.attack` p0.5, ramię w tył | 1,2 s (F3 1,0) | tak | 10 | 1,5 s |
| 2 | Linia Pnączy (2×14) | 1–3 | `block.rooted_dirt.break` wzdłuż linii | 1,0 s | tak; co 5 s w gracza >8 bl. (anty-kitowanie F1) | 7 + Spowolnienie 2 s | — |
| 3 | Tupnięcie (krąg r4) | 1–3 | korzenie wokół pnia drżą, `block.rooted_dirt.place` | 0,9 s | wokół bossa | 6 | kara za stanie przy nim |
| 4 | Wezwanie Korzeniaka (maks. 1) | 1–2 | `block.hanging_roots.break` ×3 | 2 s | — | Korzeniak | jego pąk dojrzały ≤4 bl. od bossa pęta go |
| 5 | Owoce z Korony (2 Pniaczki) | 2–3 | korona drży, `block.cherry_leaves.break` | 1,5 s | — | Pniaczki idą DO bossa, każdy, który dojdzie, leczy go o 5% | zabity Pniaczek = pąk do zdeptania |
| 6 | Dzwon (pierścień fali) | 2–3 | `block.bell.use` + linia płatków | 1,5 s | od bossa | 5 + odrzut | osłona przerywa falę |
| 7 | Salwa Puchu | 2–3 | jak Siewca, 3 kule ×2 | 0,8 s | tak; co 6 s, gdy gracz >10 bl. | 2+2+2, Spowolnienie | — |
| 8 | Upadły Konar | 3 | trzask, cień 3×3 **zawsze między bossem a graczem, ≤3 bl. od bossa** | 1,5 s | tak | 10 | konar → duży pąk → Korzeń przy bossie (pętanie bez wabienia) |
Fazy: **F1 Przebudzenie 100–65%** (1–4: uczy wzorców i pierwszego pętania). **F2 Owocowanie 65–30%** (+5, 6, 7: deptać
czy bić). **F3 Przekwit <30%** (+8, bez 4): brzeg areny zarasta (r 14→10), konar co 12 s. Anty-kitowanie: mur, Linia
Pnączy w F1 co 5 s, Salwa w F2–F3 co 6 s, kurczenie w F3.

**Tabela R2 (solo, bez specjalizacji)**
| | Deptanie pąków | Korzeń przy bossie | Pniaczki idące do bossa | Ryzyko |
|---|---|---|---|---|
| Wojownik | LPM w pąk po drodze | stoi przy bossie; konar F3 spada obok | zamach na linii | Tupnięcie przy staniu w zwarciu |
| Ninja | szybkie LPM | ściąga Korzeniaka pod bossa | biegnie na przecięcie | Zamach Korony |
| Mag | dowolne zaklęcie (pąk ma HP 1) | dobija Korzeniaka z dystansu, gdy ten stoi ≤4 bl. od bossa | zaklęcie obszarowe | kitowanie karane Linią i Salwą |
| Akolita | sługi i Chowaniec depczą przechodząc | Chowaniec trzyma Korzeniaka przy bossie | sługi przechwytują | sługi giną od Dzwonu |

## 7. WOW (L6: zdarzenie nad całą mapą)
**1. minuta, S1, z miejsca lądowania, patrząc na północ.** Na szczycie wzgórza (40 bl. dalej, 18 wyżej) sakura oplata
dzwonnicę. Korzeń (`dreadroot_root` jako display) szarpie linę i dzwon bije raz (`block.bell.use` + `block.bell.resonate`,
słychać w całym lochu). Przez mapę przetacza się z północy na gracza fala różowych płatków: skill MM, 6 emiterów
`cherry_leaves`, ściana 40 bl. przesuwana co 0,25 s przez 6 s; budżet ≤200 cząsteczek na klatkę (pomiar spark przy
4 graczach). Złota godzina (`minecraft:time set 11600`). Po fali płatki leżą na wodzie i ścieżce (pink_petals w szablonie).

## 8. Paleta
cherry_leaves, cherry_log, pink_petals, flowering_azalea_leaves, moss_block, mossy_cobblestone, stripped_oak_log,
short_grass. Pora: złota godzina, bezchmurnie. Światło ciepłe, niskie z zachodu; korona sakury z shroomlightem.
Dźwięk: `entity.bee.loop` przy kwiatach, strumień, dzwon raz na salę i nigdy w walce.

## 9. Sekrety i wybór
| Sekret | Wskazówka w świecie | Warunek | Daje |
|---|---|---|---|
| Rozsadzona skała (S3W) | pęknięta skała z korzeniami, w szczelinie róg skrzyni | zostawić duży pąk Korzeniaka przy skale | materiały + EXP sekretu |
| Dziupla Tarasów (S3E) | ślad płatków urywa się przy dziupli na tarasie +9 | dojść na górny taras | Płatek Szczepu ×2, EXP sekretu |
| Zwłoki Ogrodnika (S4W) | sadzonka kwitnie w martwej dłoni | interakcja (wejście, bez PPM) | Nóż Ogrodnika (Kronika 1) |
| Kwiat bez cienia (S7) | jedyny biały kwiat na różowej ścieżce | trafić go | Napar Jaro: +4 serca osłony dla drużyny przed bossem |
| Ul z pieczęcią (S9) | widoczny od S4E na sakurze, zasłonięty kwiatami | po bossie sakura gubi kwiaty | zapowiedź 003, materiał |
Wybory: kolejność dolin (zmienia, czego gracz uczy się najpierw) i młode Sowiedzia (kosmetyk i kwestia, §6).

## 10. Powtarzalność
Losowane przy wejściu (funkcja MD `Random`). Żaden wariant nie zmienia czasów pąka ani reguł R1:
- **Nastrój dnia** (z v7, przepisany na taktykę, bez nocy):
  1. *Wybuchowe Pniaczki* — Pniaczek przy śmierci pęka (r1,5, 3 obr.), więc deptanie z bliska kosztuje.
  2. *Wściekła Wataha* — w S2 i S3W wilki chodzą trójkami, Korzeń staje się główną bronią.
  3. *Wietrzny Sad* — puch Siewcy i Salwa lecą łukiem z wiatrem: osłona od strony nawietrznej, nie od bossa.
  4. *Głodny Strażnik* — Owoce co 15 s zamiast 20, za to pętanie trwa 5 s.
- Strona, po której stoi Siewca w S4W, i układ dwóch łuczników w S3E (taras +3/+6 albo +6/+9).
- Boss F2: Owoce albo Korzeniak co 20 s (zmienia, czy grasz „deptanie”, czy „pętanie”).

**Ocena S–C** (po 1 pkt: czas ≤13 min · 0 śmierci · Strażnik spętany ≥2 razy · ≤1 Pniaczek doszedł do bossa · sekrety
≥3/5). S = 5, A = 4, B = 2–3, C = 0–1. Ocena daje wyłącznie wygląd i tytuł (S: kosmetyk „Wianek Sadu”). Skrzynia
końcowa, EXP i materiały są takie same dla każdej oceny.

## 11. Nagroda (L3)
- EXP postaci za pierwsze przejście na Normalnym: **120** (0,6 × 200 × 1): ~48 moby i elita, ~48 boss, ~24 sekrety.
  Powtórka 50; Łatwy ×0,7, Trudny ×1,3. Każdy członek drużyny dostaje osobno (`@PlayersInWorld`).
- Rodzina „Sadownicza” (`L001_*`, nazwy robocze): Sierp Sadownika, Korowa Tarcza, Kaptur Zielarza, Laska z Kwitnącej
  Gałęzi, Pas Ogrodnika (tier aktu 1).
- Materiały: `PROF_RAW_TIMBER`, `PROF_MEDICINAL_HERB`, **Spaczony Korzeń** (nowy), **Płatek Szczepu** (tylko tu),
  **Żeton Sadu** (gwarantowany z bossa, zabezpieczenie przed pechem).
- Rzadko z bossa: Pieczęć Lochowa (Chowańce), Pąk Strażnika (kosmetyk).
- Odblokowuje: Kronikę 1, cofnięcie plamy „Sady” na Mapie Granicy, powrót Jaro do Stolicy (sprzedaje napar).

## 12. Trudności
| | Łatwy | Normalny | Trudny |
|---|---|---|---|
| Dojrzewanie pąka | 8 s | 5 s | 4 s |
| Zapowiedzi | +30% | bazowe | bazowe; Linia Pnączy razem z Dzwonem |
| Leczenie z Pniaczka | 3% | 5% | 8% |
| Pętanie bossa | 5 s | 4 s | 3 s, Korzeń ≤3 bl. |
| Wrogowie | −1 w grupach, bez Salwy | pełny skład | Korowiec Bramy z 2 młodymi Korzeniakami; Sowiedź woła młode od 70% |

## 13. Wykonanie
Stos: MM 5.11.2 (darmowy), MD (funkcje, BlockEditor, Checkpoint, Random), `place template`, teren pieczony
i zamrożony (generator „widły” z `make_loch001.py`), ModelEngine dla `dreadroot` i `lr_owlbear`.
| # | Element | Ryzyko | Plan A | Plan B |
|---|---|---|---|---|
| 1 | Pąk, dojrzewanie, kolejka 12 | delay, kolejność, wiele instancji | `~onDeath` → `summon PAK{level=1}` (`Despawn: persistent`, tag `pak`). W PAK `~onSpawn`: `scoreboard players add #pak_<caster.l.w> l_pak 1` i `scoreboard players operation <caster.uuid> l_pak = #pak_<caster.l.w> l_pak` (numer kolejny). Potem **kontrola limitu na żywych pąkach**: `execute in <świat> store result score #ile_<świat> l_pak if entity @e[tag=pak,tag=!usycha]`; jeśli `#ile > 12`: `scoreboard players set #min_<świat> l_pak 2147483647`, `execute in <świat> as @e[tag=pak,tag=!usycha] run scoreboard players operation #min_<świat> l_pak < @s l_pak` (minimum spośród AKTYWNYCH), `execute in <świat> as @e[tag=pak,tag=!usycha] if score @s l_pak = #min_<świat> l_pak run tag @s add usycha` → skill usychania 1 s i `remove`. Pąk z tagiem `usycha` wypada z kolejki w chwili oznaczenia (nie jest liczony ani wybierany ponownie, nie dojrzewa); kontrola biegnie po każdym przyroście, więc aktywnych jest zawsze ≤ 12. Zdeptane i dojrzałe pąki znikają z encji, więc nie biorą udziału w wyborze. Dalej `delay 80` → pęcznienie, `delay 20` → skutek i `remove`. Trafienie: `~onDamaged` → remove. | te same komendy kolejki, ale wołane przez jednego niewidzialnego kontrolera instancji `~onTimer:5` zamiast z `~onSpawn` pąka (gdyby onSpawn zawodził); usychanie i wybór najstarszego aktywnego bez zmian |
| 2 | Taran i pętanie | wykrycie „przy murze/bossie” | Korzeń przy dojrzewaniu: `@MobsInRadius{types=ZNACZNIK_MURU;r=4}` → `command{c="execute in <caster.l.w> run place template l001:brama_rozbita X Y Z"}`. Przy bossie: `@MobsInRadius{types=L001_STRAZNIK;r=4}` → `signal{s=petaj}` → boss `~onSignal` (stan klęczenia, `DamageModifiers` przez 4 s). | Korowiec Bramy nie odchodzi od muru dalej niż 6 bl., a mur pęka od każdego Korzenia w strefie (znacznik r6) |
| 3 | Pniaczki idą do bossa | ścieżka MM | `goto{}` do bossa co 1 s; ≤1,5 bl. → `signal{s=wsiakam}` → boss `heal{amount=5%}` | korzeń-lina (cząsteczki) przyciąga Pniaczka `pull` przez 4 s: widoczne i trafialne |
Każdą nową mechanikę sprawdzić mobem-jednorazówką (pamięć: cicha walidacja MM).
**Test obowiązkowy**: 13 zgonów w dwóch równoległych instancjach. Usycha najstarszy pąk w swojej instancji, druga
instancja jest nietknięta.
Encje naraz: S5 = Korowiec + 3 Pniaczki + 4 pąki + Korzeń ≈ 9; boss = 1 + Korzeniak + 2 Pniaczki + 4 pąki + 4 display ≈ 12.

## 14. Samokontrola
Brief §4:
- Wskazówki prawdziwe (łuny Kotw, pęknięcia muru, puch nad pąkiem Siewcy); kierunki w tekstach opisowe ✓
- Najwyżej 1 komunikat w walce (Jaro tylko przy ognisku) ✓
- Tekst nie obiecuje rzeczy, których nie ma ✓
- Nagroda równa dla ocen i wyborów, a różnica jest tylko kosmetyczna ✓
- Gracz wie, co robić ✓
- Walka karze ignorowanie mechaniki (leczenie bossa) ✓
- Kitowanie zamknięte (Linia F1, Salwa F2–F3, mur) ✓
- Nie ma „boss + fale” (każdy add ma funkcję) ✓
- Sekrety mają wskazówki ✓
- Losowość zmienia taktykę, nie stałe R1 ✓
- Pierwszy loch: typy wrogów pojawiają się pojedynczo, boss ma w F1 4 zdolności (P6) ✓

| Fun | Mechanika/zwrot | Czytelność | Topologia | WOW | Sens | Powtarzalność | Nagroda | Wykonalność | Różnorodność |
|---|---|---|---|---|---|---|---|---|---|
| 7 | 8 | 7 | 7 | 8 | 8 | 7 | 7 | 7 | 7 |

## 15. Odpowiedź na recenzję (etap 5)
| Poprawka | Źródło | Zmiana |
|---|---|---|
| Jeden pąk na zgon; Siewca dawał 3 pąki | Y1 | Siewca zostawia 1 pąk, którego skutkiem jest 3 Pniaczki; przyczyna widoczna (puchowa głowa nad pąkiem) §5 |
| Inny model dużego pąka | Y1 | Zostaje skala ×1,5 bez innej barwy — rozstrzygnięcie prowadzącego P6. Odrzucam pełną identyczność z Y1, bo prowadzący jest wiążący, a wielkość wynika z poległego. |
| Nastrój „Kwitnący Sad” zmieniał 5 s | Y1, Y-powtarzalność | usunięty; za niego *Wietrzny Sad* i *Głodny Strażnik* (zmieniają taktykę, nie stałe) |
| Kolejka „sort=furthest”, licznik globalny | Y2 | kolejka scoreboard per świat instancji, wypiera NAJSTARSZY; test 13 zgonów w 2 instancjach (§13) |
| Kitowanie Maga, F3 zależne od toru bossa | Y3 | Linia Pnączy co 5 s na >8 bl. w F1, Salwa co 6 s w F2–F3; konar zawsze ≤3 bl. od bossa; pętanie to bonus, wygrana możliwa bez niego |
| Łup zależny od oceny i wyboru | Y4 | ocena = tylko wygląd i tytuł; młode: te same materiały, różny kosmetyk |
| Za gęsty dla poz. 1 | P6 | Siewca tylko S4W, Korowiec tylko Brama, F1 bossa = 4 zdolności, pierwszy duży pąk pokazowy (belka), dodane Źródełko-odpoczynek |
| Wspólne liczby | L2, L3 | boss 360, elita 90, Kotwa 30, EXP 120, czas 12–14 min |
| Seria szczytów bez odpoczynku | Y-fun | kładka + Źródełko + ognisko2 + S7 rozdzielają szczyty |
| Kolejka mogła zostawić 13 aktywnych (szukała numeru „#pak−12”, który mógł być już zdeptany) | Y-runda2 | wybór minimum numeru spośród AKTYWNYCH pąków po każdym summon, usychanie przy >12; plan B ta sama kolejka z kontrolera (§13) |
| Usychający pąk liczony w kolejce (13 aktywnych przy szybkim 14. pąku) | Y-runda3 (poprawka prowadzącego) | liczenie i wybór tylko `@e[tag=pak,tag=!usycha]`; oznaczony wypada z kolejki od razu (§13) |
