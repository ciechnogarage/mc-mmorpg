# Profesje zbierackie — notatka stanu (gałąź `profesje-zbierackie`)
Stan na 2026-11-20 (drwal 12:05, rybak po południu, myśliwy wieczorem). Gałąź od `zielarz-naprawa` + scalone `rzemioslo-flaga-profilu` (d632d73) w 528a5ff. NIE scalać do main. Na world1 nic.

## Decyzje właściciela (20.11, przez koordynatora, dosłownie)
„1. no chyba wszystkie trzeba zrobić czy co? 2. no raczej każda profesja powinna mieć swoje rzeczy w lochach i wyspach jak to rozkminiasz?? 3. mogą 4. zalezy jaka trudność i koszt ale fajnie jakby każdy widziałk 5. tak może dać 0 6. tak wchodzą 7. no tak ale z głową ma pasować do profesji i mieć sens”
(3 = zawał/upadek drzewa może ranić i zabić; 4 = upadek drzewa / skok ryby widoczny dla wszystkich; 5 = próba może dać 0; 6 = pory roku z farmerem, zegar właściciela: rok = 7,5 dnia; 7 = przeróbka przy każdej profesji.)

## Kolejność i stan
| Profesja | Stan | Commit |
|---|---|---|
| drwal | GOTOWE na gałęzi: scinka.js 16/16, obciążenie 1/4 boty, nagranie QA | df77aa2 |
| rybak | GOTOWE na gałęzi: lowienie.js 18/18 ×2, obciążenie 1/4 boty, klient QA (hol, kamera stoi → bez kontry); klon usunięty | 7354922 |
| myśliwy | GOTOWE na gałęzi: lowy.js 21/21 ×3, obciążenie 1/4 boty, klient QA (trop, podchód, cios, 5 cięć, wędzenie); klon usunięty | (ten commit) |
| górnik | GOTOWE na gałęzi: urobek.js 31/31 ×2 (końcowy jar), obciążenie 1/4 boty (65/104 pkt/s), klient QA (opukanie, pęknięcia, zawał, przebieranie, wynik; 2 błędy znalezione i poprawione); klon usunięty | (ten commit) |
| farmer | nie zaczęte; pory roku: zegar właściciela — źródła jeszcze nie znalazłem (nie sprawdzone) | — |

## Drwal — otwarte
- Przedmioty `PROF_*_WYBORNE` tylko w `drwal/przedmioty-klon.yml` → dodać do generatora (`profesje/tresc.py`) przed wdrożeniem.
- Lochy: na mapie `loch_001` brak stojących drzew (parser regionów 20.11) → las do posadzenia przy budowie mapy; komenda `las loch_001_* … trwale` gotowa.
- Znane ograniczenie: kolizja bloków drzewa na serwerze przy obrazie powietrza u klienta w czasie sceny.

## Rybak — co już było na world1 (sprawdzone 20.11, przed kodem)
| Funkcja | Co już to robi na world1 | Źródło |
|---|---|---|
| minigra holu ryby | MMOCore „tugs” (spam-klik wędką) — tylko z tabel `on-fish`; na world1 brak tabel, `exp-sources: {}` | `MMOCore/professions/fishing.yml:1-12`, `docs/Plugins-wiki-local/mmocore/wiki/md/profession-fishing.md:30,73` |
| statystyki łowienia | MMOCore `FISHING_STRENGTH`, `CRITICAL_FISHING_*` (działają tylko z tugs) | `MMOCore/stats.yml:96-120` |
| łowiska, tier wody, łup | Rzemiosło `Lowienie` + config `lowiska` (główny/poboczny/rzadkie), `lowisko-bloki`, wędka `PROF_WEDKA_1`, zlecenia `fishing` | `Rzemioslo/config.yml:1441,12510,296,10597`, `Lowienie.java` |
| przedmioty łupu | MMOItems `PROF_RAW_CATCH`, `PROF_T2_RYBA` (bez wersji wybornych) | `MMOItems/item/material.yml:1425,1455` |
| osobny plugin łowienia | nic — brak CustomFishing itp. w `plugins/` (`ls`) | — |
Wniosek: scena (branie, zacięcie, hol z kontrą) nie istniała — nowy kod w `Rybak.java`, łup i tier z istniejącego configu.

## Rybak — otwarte
- Przedmioty `PROF_RAW_CATCH_WYBORNE`, `PROF_CLAM_WYBORNE` tylko w `rybak/przedmioty-klon.yml` → do `tresc.py` przed wdrożeniem (jak drwal). Bez nich wyborny połów daje zwykły przedmiot + ostrzeżenie w logu.
- Lochy: parser `rybak/woda_lochow.py` (20.11): `loch_001` 0 bloków wody, 5 map z terenem Iris (pusty region, nie do policzenia z pliku) → woda do zbudowania przy budowie map.
- Kontra: bot przy aktywnej kontrze trafia ~55% ticków zrywu (q 0,88 = wyborne) — próg 0,8 w `nagroda` do oceny przez właściciela po graniu.
- Bot mineflayer nie zmienia slotu paska na klonie (przyczyna niezbadana) → test „schowana wędka” zdejmuje wędkę RCON-em.
- Przeróbka (decyzja 7): rybak bez patroszenia — wybór projektowy do potwierdzenia u właściciela.

## Myśliwy — otwarte
- Przedmioty `PROF_DZICZYZNA`, `PROF_DZICZYZNA_WYBORNE`, `PROF_SUSZONA_DZICZYZNA(_WYBORNE)`, `PROF_BEAST_SCRAP_WYBORNE`, `PROF_RAW_FLEECE_WYBORNE`, `PROF_VENOM_SAC_WYBORNE` tylko w `mysliwy/przedmioty-klon*.yml` → do `tresc.py` przed wdrożeniem (jak drwal/rybak). Jadalność surowej dziczyzny: nie sprawdzona.
- Lochy: `mysliwy/rewiry_lochow.py` (20.11, tylko odczyt): `loch_001` ma proste linie gruntu 65–105 bl. (starcie_A, populacja, zasadzka_C, środek obszaru) — rewir do postawienia komendą `rzemioslo rewir <loch_001_*> x y z <mob> trwale` (Komenda.java:268) przy budowie mapy; 5 map Iris: brak regionów w pliku. Trop nie sprawdza ścian (w lochu może prowadzić przez mur) — do poprawy, jeśli rewir wypadnie w korytarzach.
- Do decyzji właściciela: wędzenie 90 s (testowe), próg CEL 0,2 przy cięciach, nóż `attack-damage 1` (czysty cios = cios z podchodu ×N, nie siła noża), reguły zająca („pończocha”) i pająka (worek jadowy) bez źródła.
- UI (klient QA 20.11): tropy (DUST) niewidoczne przy cząsteczkach „minimalne” — gracz z tym ustawieniem nie zobaczy śladów (rozważyć dźwięk/blok); podpowiedź podchodu na pasku była dłuższa niż ekran 1280 przy GUI ×3 → skrócona do „wiatr od zwierza na ciebie, kucaj”, po skróceniu nie oglądana w kliencie.
- Garbowanie zostaje krawcowi (`tailoring_t1_skora_w`).

## Myśliwy — co już było na world1 (sprawdzone 20.11, przed kodem)
| Funkcja | Co już to robi na world1 | Źródło |
|---|---|---|
| zwierzyna (moby) | MythicMobs: 8 bestii `BEAST_*` (zając RABBIT 12 HP, owca SHEEP, dzik PIG, wilk WOLF, niedźwiedź POLAR_BEAR, pająki CAVE_SPIDER/SPIDER), wilki lochu `L001_*` (WOLF); losowe spawny REPLACE 4% w biomach `world` | `MythicMobs/mobs/Bestiariusz.yml:23-139`, `mobs/Loch_001.yml:34,155`, `randomspawns/Bestiariusz.yml:27-60` |
| wilki w lochu | `summon L001_WILK/WILCZYCA/WILCZE/WILK_WATAHA` w skillach lochu 001 (walka, nie łowy) | `MythicMobs/skills/Loch_001.yml:61-126` |
| zwłoki i oprawianie | Rzemiosło `Oprawianie`: zwłoki 30 s po śmierci moba z `zwierzyna:` (14 mobów, wszystkie tier 1), PPM nożem → pasek 2,5 s (silnik węzłów) → łup | `Rzemioslo/config.yml:8-17,1524-1615`, `Oprawianie.java` |
| XP myślistwa | tylko Rzemiosło (`exp-sources: {}`) | `MMOCore/professions/hunting.yml:1-12` |
| tropienie, wiatr, podchód | nic — brak pluginu łowów w `plugins/` (`ls`), MMOCore bez źródeł | `ls world1/plugins`, `hunting.yml` |
| garbowanie / wyprawa skóry | KRAWIEC: `tailoring_t1_skora_w` 3× `PROF_BEAST_SCRAP` → `PROF_CURED_HIDE` | `MMOItems/crafting-stations/profession-tailoring.yml:190-197`, `tresc.py:247` |
| pieczeń z dziczyzny | KUCHARZ: `PROF_HUNTER_ROAST` itd. z `PROF_BEAST_SCRAP` | `profession-cooking.yml:32-59` |
| suszarnia | zielarz `ZSuszarnia` (pęki ziół, cień/słońce/deszcz) — dla ziół, nie mięsa | `ZSuszarnia.java:38-44` |
| przedmioty | `PROF_BEAST_SCRAP` „Łup z bestii”, `PROF_BEAST_BONE`, `PROF_MONSTER_TROPHY`, `PROF_RAW_FLEECE`, `PROF_VENOM_SAC`; nóż `PROF_NOZ_1`; brak mięsa i wersji wybornych | `MMOItems/item/material.yml:1725-1750,2085-2100`, `tool.yml:355` |
| „węzeł stado” (z polecenia) | NIE ISTNIEJE — `grep -i stado` w repo i world1 Rzemioslo/MythicMobs: 0 | — |
Wniosek: tropy, wiatr, podchód, scena cięć, spalona skóra i wędzenie mięsa nie istniały → nowy kod `Mysliwy.java`; zwierzyna, tiery, łup, zwłoki z istniejącego configu i `Oprawianie`. Garbowania NIE robię (należy do krawca). Obróbka myśliwego = wędzenie dziczyzny nad ogniskiem.

## Myśliwy — źródła wiedzy (20.11)
| Reguła gry | Źródło | Status |
|---|---|---|
| podchód pod wiatr; wiatr niosący zapach do zwierzęcia płoszy | TPWD hunter-ed „Hunting Strategies”, Field & Stream „downwind vs upwind” | sprawdzone (wyszukiwarka) |
| kolejność patroszenia grubej zwierzyny: odbyt → brzuch do mostka → przełyk/tchawica → przepona (uwaga na żołądek) → trzewia; skóra potem | Montana State Univ. „Field Care of Harvested Big Game” | sprawdzone (WebFetch) |
| skóra: solić obficie, zwinąć | j.w. | sprawdzone; solenie/wyprawa zostaje krawcowi |
| suszone mięso wymaga ciepła / dymu | USDA FSIS „Jerky and Food Safety”, MSU „Making safe venison jerky” | sprawdzone (wyszukiwarka) |
| zając: skóra „pończochą” od łap; pająk: worek jadowy | — | NIE sprawdzone (reguła gry) |
| ogień niszczy skórę | — | reguła gry, bez źródła |

## Górnik — co już było na world1 (sprawdzone 20.11, przed kodem)
| Funkcja | Co już to robi na world1 | Źródło |
|---|---|---|
| źródło T1 (złoże) w świecie | NIC — `rzemioslo postaw` w skillach MM tylko dla ziół, grzybów, drewna (`skills/Loch_001.yml:99-102`); pakiet Iris w folderze świata `world/iris/pack/dimensions/overworld.json` bez `nexo:`; `Rzemioslo/dane` pusty | MM `skills/Loch_001.yml:99-102`, `world/iris/pack/...overworld.json`, plik SAD_ZRODLO `backups/stolica-v3-20261019/blockout-a2/npc-cytadela/generuj.py:121-138` („PLACEHOLDER”) |
| złoża Iris `nexo:ruda_urobku(_glebinowa)` | są tylko w `plugins/Iris/packs/overworld/dimensions/overworld.json:690-730` (nie w pakiecie świata `world`); który świat z niego generuje — nie sprawdzone do końca (MV `world` generator pusty) | j.w. |
| węzeł górniczy (blok Nexo, PPM 3 s) | Rzemiosło `Zbieranie` + config `wezly.wezel_t1_mining`, `ruda_urobku`, `ruda_urobku_glebinowa`, `kamien_kamieniolomu` (główny `PROF_ORE_CHUNK`, poboczny `PROF_ROUGH_CRYSTAL` 0,15, rzadki `PROF_RAW_GEM` 0,03) — pasek, bez sceny | `Rzemioslo/config.yml:572,1329,1343`, `Zbieranie.java:38-40` |
| kopanie z regeneracją bloków | MMOCore custom-mining włączone (`config.yml:50-53`), ale `professions/mining.yml` bez `on-mine` (12 linii, `exp-sources: {}`) → nic nie robi | `MMOCore/config.yml:50-70`, `MMOCore/professions/mining.yml:1-12`, `docs/Plugins-wiki-local/mmocore/wiki/md/features-mining.md` |
| narzędzie | `PROF_KILOF_1..10` (rodzaj `kilof`, `profesje.mining.narzedzie`), kowal robi kilof `smithing_t1_kilof` | `MMOItems/item/tool.yml:275`, `profession-smithing.yml:36` |
| przetop rudy | KOWAL: `smithing_t1_sztabka` 3× `PROF_ORE_CHUNK` → `PROF_REFINED_IRON` | `MMOItems/crafting-stations/profession-smithing.yml:28-35` |
| przedmioty | `PROF_ORE_CHUNK` „Bryła urobku”, `PROF_ROUGH_CRYSTAL`, `PROF_RAW_GEM`, `PROF_ROUGH_STONE`; brak wersji wybornych | `MMOItems/item/material.yml:525-555,2035` |
| opukiwanie, zawał, przebieranie | nic — brak pluginu górnictwa w `plugins/` (`ls`), MythicMobs bez mechanik złóż | `ls world1/plugins` |
Wniosek: scena (opukiwanie, praca wzdłuż warstwy, skała twarda/miękka, obryw stropu, zawał, przebieranie urobku) nie istniała → nowy kod `Gornik.java`; tier, łup i XP z istniejącego configu (`wezel_t1_mining`). Przetopu NIE robię (kowal). Przeróbka górnika = rozbijanie i przebieranie urobku (cobbing) — oddziela płonną skałę, zanim urobek trafi do kowala.

## Górnik — źródła wiedzy (20.11)
| Reguła gry | Źródło | Status |
|---|---|---|
| opukiwanie stropu: lita skała dźwięczy, luźna („drummy”) brzmi głucho; luźny strop zbija się drągiem (obrywka) | NIOSH „Ground Monitoring”, Bureau of Mines RI 5251, decyzja FMSHRC 86081146 | sprawdzone (wyszukiwarka) |
| odgłos pękania/trzeszczenia skały ostrzega przed obrywem | NIOSH „Mining Topic: Ground Monitoring” | sprawdzone (wyszukiwarka) |
| twarda żyła: klin/żelazko pod młot, nie kilof; ogień na najtwardszą | Agricola, De re metallica ks. V (tłum. Hoover), Ruhr-Uni Bochum (DBM) | sprawdzone (wyszukiwarka) |
| ręczne rozbijanie i przebieranie urobku (cobbing) oddziela płonną skałę | Cornish Mining WHS „Dressing the Ore”, Wikipedia „Mineral processing” | sprawdzone (wyszukiwarka) |
| dźwięk prowadzi do żyły (ruda dźwięczy inaczej) | — | NIE sprawdzone — reguła gry |
| praca wzdłuż warstwy (żyła biegnie pasem, uskok o blok) | — | reguła gry (źródło o „following the vein” nie znalezione) |
| przekruszona ruda = strata („miał”) | — | reguła gry |

## Górnik — otwarte
- Przedmioty `PROF_ORE_CHUNK_WYBORNE`, `PROF_KLIN_GORNICZY` tylko w `gornik/przedmioty-klon.yml` → do `tresc.py` przed wdrożeniem; klin potrzebuje receptury u kowala (nie zrobione).
- Lochy: `gornik/sciany_lochow.py` (20.11, tylko odczyt): `loch_001` ma 1 nadającą się ścianę (37,103,11, andezyt, bez stropu → bez zawału); reszta ścian to liście/drewno/cegły → przodki trzeba dobudować przy budowie mapy (wnęka ze skały ze stropem) i wpisać `rzemioslo przodek loch_001* x y z trwale`. 5 map Iris: brak regionów w pliku. Wysp jeszcze nie ma.
- Do decyzji właściciela: odrost wyrobiska 4 min per konto (testowe), obrażenia zawału (środek 75% HP — zabija przy niskim HP), czy ruda odsłonięta opukaniem ma być widoczna dla wszystkich ≤ 48 bl. (koszt zmierzony: mały).
- **Drwal ma tę samą lukę co górnik przed poprawką**: `Drwal.java:204` pomija machnięcie tylko 80 ms po PPM, a klient QA przy 10 FPS macha 98 ms po PPM (górnik 20.11) → u drwala PPM może liczyć się jako cios przy niskim FPS. Nie poprawione (poza zakresem górnika), nie sprawdzone klientem na drwalu.
- Klient QA: w prawym górnym rogu ekranu stoi zielony kwadrat z szarą kreską (nie z Rzemiosła; nie sprawdziłem, który plugin/pakiet go rysuje).
- Kara „poza warstwą” podniesiona do 0,1/blok (max 0,3) po FAIL testu (2 bloki obok żyły dawały jeszcze „wyborny”).

## Jak wznowić
Klon: `klon.sh lista` → `klon.sh start <profesja> --z-kreatorem` → `MCMMORPG/profesje/<profesja>/przygotuj_klon.sh` (wzór: `drwal/`). Klony usunięte 20.11 (drwal, rybak, myśliwy, górnik).
Pułapka botów: `bot.lookAt(p, true)` nie wysyła samego pitchu → `lookAt(p, false)` + 160 ms (skill mc-host-testy-wdrozenia, pułapka 13).
