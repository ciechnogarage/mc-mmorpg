# Polecenie Claude dla Agenta A — runda 7 (otwarte błędy, zgoda właściciela)

Zmiany środowiska:
- Klon `klon-zaklecia` postawiony OD NOWA z dzisiejszego world1 (reguły rąk Księgi, DwieBronie v2.2, profesje v2).
- KsiegaPostaci: worktree `ksiega-naprawy-zaklecia` jest teraz na gałęzi **`ksiega-naprawy-zaklecia-2`** = `107b60a`
  (reguły rąk, bajtowo = world1) + `ec4fa21` (Twój filtr broni). Pracujesz na niej. Zaklecia bez zmian (`7b62798`).
- Paczka wdrożenia czeka na uruchomienie przez właściciela; Twoje dzisiejsze zmiany NIE wchodzą do niej.
- Klonu nie zwijasz. Nie commitujesz.

## Zakres (błędy z końcowej regresji B, `krytyk/raport-r4.md`)
1. **C: zwykły przedmiot (chleb, mikstura, łuk, kilof, blok, mapa) w slocie 5 + zaklęcie w lewej → po PPM serwer ma w slocie 5
   `debug_stick`.** Twoja wcześniejsza `N3B` mówiła, że chleb zostaje — rozstrzygnij sprzeczność: gdzie jest przedmiot po
   PPM (plecak? znika? duplikat?), co go zdejmuje (Nosnik / Ksiega reguły rąk / DwieBronie / użycie przedmiotu vanilla).
   Sonda z inwentarzem serwera przed/po (pełne 41 slotów). Jeśli przedmiot znika — to KRYT, napraw najpierw.
2. **Rozjazd ekwipunku klient/serwer w szybkich gestach** (fuzzer B, ziarno 271828, krok 185: serwer `stone_sword`, klient
   pusty; ślad `/opt/docker/mc/narzedzia/bot/wyniki/zaklecia/fuzz-271828.json`). Znajdź, kto wysyła zmianę slotu bez
   synchronizacji klienta (brak `updateInventory`/set_slot po anulowaniu zdarzenia?). Napraw, jeśli to nasze pluginy.
3. **Przywołany miecz oburącz kosztuje ×1** (~1,7 many) zamiast mnożnika oburącz (×2,8) — przyczyna + poprawka albo
   uzasadnienie, że celowo (wtedy opis w Księdze ma to mówić).
4. **Pasek Shift+Q za głośny pakietowo:** wybór z paska ~33 set_slot/s gracza, 5 akcji ≈ 123–135 pakietów BossBar.
   Zmniejsz: set_slot tylko gdy zmienia się zawartość dłoni, BossBar tylko przy zmianie treści (część zrobiona w N4 —
   zmierz, co jeszcze wysyła). Cel: ≤ 1 set_slot na zmianę wyboru, ≤ 2 aktualizacje paska na akcję.
Dla każdego: sonda FAIL (bazowy = paczka do wdrożenia, czyli `build` z obecnym stanem przed Twoimi zmianami — zapisz md5)
→ PASS. Regresja na końcu: A, C, D, F, DB3, N_melee2, N_damage, STAT3 (osobne przebiegi). Szczegóły `naprawy/szczegoly-r7.md`.
STOP po raporcie.
