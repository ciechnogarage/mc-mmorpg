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
| górnik | nie zaczęte (węzły Nexo w `Zbieranie.java`) | — |
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

## Jak wznowić
Klon: `klon.sh lista` → `klon.sh start <profesja> --z-kreatorem` → `MCMMORPG/profesje/<profesja>/przygotuj_klon.sh` (wzór: `drwal/`). Klony usunięte 20.11 (drwal, rybak, myśliwy).
Pułapka botów: `bot.lookAt(p, true)` nie wysyła samego pitchu → `lookAt(p, false)` + 160 ms (skill mc-host-testy-wdrozenia, pułapka 13).
