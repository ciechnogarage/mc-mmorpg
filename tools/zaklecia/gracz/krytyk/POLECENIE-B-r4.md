# Polecenie Claude dla Agenta B — runda 4 (końcowa regresja)

Na klonie jest OSTATECZNA wersja (A runda 6): Zaklecia commit `7b62798`, KsiegaPostaci `1433dfb`, nakładka (N5
party_other, N6 [refresh] + ikony dłoni, N8 spolszczenie, dźwięki strumienia 8→4), `debug:false`. Nie zlecaj `build`
(chyba że do A/B: `build bazowy`, a po nim KONIECZNIE `build` przywracający wersję A). Klonu nie zwijasz.

## Ustalenia rundy 5/6 (A + Claude), które zmieniają Twoje wnioski z rundy 3
- „Miecz + zaklęcie w lewej: LPM = 0” — FAŁSZYWY ALARM harnessu: `DB3` wracał na slot 1 (obraz lewej dłoni Księgi,
  `k.cyfra(2)`) zamiast 0 i nie nadawał biegłości `bieglosc-miecz`. Sonda A `N_melee2`: LPM 20→11,6, PPM dalej działa,
  miecz zostaje po 0→5→0. Popraw `DB3`/grupę C (slot 0, biegłość) i zweryfikuj niezależnie.
- Osłona i Uspokojenie DZIAŁAJĄ (A `N_effects`: cios 4 → 2,63–2,70 pod Osłoną; husk z AI przestaje atakować po
  Uspokojeniu) — tylko nie wysyłają pakietu efektu/atrybutu. Mierz skutek, nie pakiet.
- CME naprawione (`8d7d23f`), dowód A `N_cme` baza→A.
- Prawdziwy klient (Claude): panel dłoni pokazuje ikony, wąski strumień nie zasłania celu (`klient/captures/M01k.png`,
  `porownanie-N7-r6.png`).
- Uwagi do harnessu: pełny filtr w jednym procesie bota padł na `JavaScript heap out of memory` (512 MB) — uruchamiaj
  grupy osobnymi przebiegami. Grupy B/C mają chwiejne timeouty przypisania przez GUI — napraw przyczynę (czekanie na stan,
  nie na czas), zanim ocenisz grę.

## Runda 4 — zakres
1. Napraw harness B/C (slot 0 + biegłość w scenach broni, stabilne przypisanie, osobne procesy), aż te same testy dają
   ten sam wynik 2× z rzędu.
2. Pełna regresja na ostatecznej wersji: A–F, wszystkie `N_*` A, Twoje `DB3`, `BR3M`, `STAT3`, `NOISE`, `N_effects`,
   `PW` (N1), fuzzer 1 ziarno × 1000 — 2 przebiegi. Każdy pozostały FAIL: gra czy harness, z dowodem.
3. Recenzja commitów od `b8a73aa` do `7b62798` + nakładki — werdykty.
4. Zaktualizuj `../OCENA.md` (ocena końcowa: feeling per typ i czynność, TOP błędy pozostałe, decyzje właściciela:
   zasłanianie strumienia przez sojusznika, obie dłonie po puszczeniu PPM, Sakwa slot 18, broń w lewej przy zaklęciu
   — 2 warianty z `naprawy/szczegoly-r5.md`).
Raport `krytyk/raport-r4.md` (max 50 linii, sam plik), -o `koniec-r4.txt`. Load zapisuj przy pomiarach czasu. STOP.
