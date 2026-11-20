# Profesje zbierackie — notatka stanu (gałąź `profesje-zbierackie`)
Stan na 2026-11-20 (drwal 12:05, rybak po południu). Gałąź od `zielarz-naprawa` + scalone `rzemioslo-flaga-profilu` (d632d73) w 528a5ff. NIE scalać do main. Na world1 nic.

## Decyzje właściciela (20.11, przez koordynatora, dosłownie)
„1. no chyba wszystkie trzeba zrobić czy co? 2. no raczej każda profesja powinna mieć swoje rzeczy w lochach i wyspach jak to rozkminiasz?? 3. mogą 4. zalezy jaka trudność i koszt ale fajnie jakby każdy widziałk 5. tak może dać 0 6. tak wchodzą 7. no tak ale z głową ma pasować do profesji i mieć sens”
(3 = zawał/upadek drzewa może ranić i zabić; 4 = upadek drzewa / skok ryby widoczny dla wszystkich; 5 = próba może dać 0; 6 = pory roku z farmerem, zegar właściciela: rok = 7,5 dnia; 7 = przeróbka przy każdej profesji.)

## Kolejność i stan
| Profesja | Stan | Commit |
|---|---|---|
| drwal | GOTOWE na gałęzi: scinka.js 16/16, obciążenie 1/4 boty, nagranie QA | df77aa2 |
| rybak | GOTOWE na gałęzi: lowienie.js 18/18 ×2, obciążenie 1/4 boty, klient QA (hol, kamera stoi → bez kontry); klon usunięty | (ten commit) |
| myśliwy | nie zaczęte (jest `Oprawianie.java`, 146 linii) | — |
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

## Jak wznowić
Klon: `klon.sh lista` → `klon.sh start <profesja> --z-kreatorem` → `MCMMORPG/profesje/<profesja>/przygotuj_klon.sh` (wzór: `drwal/`). Klony usunięte 20.11 (drwal, rybak).
Pułapka botów: `bot.lookAt(p, true)` nie wysyła samego pitchu → `lookAt(p, false)` + 160 ms (skill mc-host-testy-wdrozenia, pułapka 13).
