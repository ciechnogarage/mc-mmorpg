# Profesje zbierackie — notatka stanu (gałąź `profesje-zbierackie`)
Stan na 2026-11-20 12:05. Gałąź od `zielarz-naprawa` + scalone `rzemioslo-flaga-profilu` (d632d73) w 528a5ff. NIE scalać do main. Na world1 nic.

## Decyzje właściciela (20.11, przez koordynatora, dosłownie)
„1. no chyba wszystkie trzeba zrobić czy co? 2. no raczej każda profesja powinna mieć swoje rzeczy w lochach i wyspach jak to rozkminiasz?? 3. mogą 4. zalezy jaka trudność i koszt ale fajnie jakby każdy widziałk 5. tak może dać 0 6. tak wchodzą 7. no tak ale z głową ma pasować do profesji i mieć sens”
(3 = zawał/upadek drzewa może ranić i zabić; 4 = upadek drzewa / skok ryby widoczny dla wszystkich; 5 = próba może dać 0; 6 = pory roku z farmerem, zegar właściciela: rok = 7,5 dnia; 7 = przeróbka przy każdej profesji.)

## Kolejność i stan
| Profesja | Stan | Commit |
|---|---|---|
| drwal | GOTOWE na gałęzi: scinka.js 16/16, obciążenie 1/4 boty, nagranie QA | df77aa2 |
| rybak | nie zaczęte (jest `Lowienie.java`, 114 linii — punkt wyjścia) | — |
| myśliwy | nie zaczęte (jest `Oprawianie.java`, 146 linii) | — |
| górnik | nie zaczęte (węzły Nexo w `Zbieranie.java`) | — |
| farmer | nie zaczęte; pory roku: zegar właściciela — źródła jeszcze nie znalazłem (nie sprawdzone) | — |

## Drwal — otwarte
- Przedmioty `PROF_*_WYBORNE` tylko w `drwal/przedmioty-klon.yml` → dodać do generatora (`profesje/tresc.py`) przed wdrożeniem.
- Lochy: na mapie `loch_001` brak stojących drzew (parser regionów 20.11) → las do posadzenia przy budowie mapy; komenda `las loch_001_* … trwale` gotowa.
- Znane ograniczenie: kolizja bloków drzewa na serwerze przy obrazie powietrza u klienta w czasie sceny.

## Jak wznowić
Klon: `klon.sh lista` → `klon.sh start <profesja> --z-kreatorem` → `MCMMORPG/profesje/<profesja>/przygotuj_klon.sh` (wzór: `drwal/`). Klony usunięte 20.11 (drwal).
Pułapka botów: `bot.lookAt(p, true)` nie wysyła samego pitchu → `lookAt(p, false)` + 160 ms (skill mc-host-testy-wdrozenia, pułapka 13).
