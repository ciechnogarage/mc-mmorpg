# Polecenie Claude dla Agenta B — runda 2

## Ocena rundy 1 (Claude)
Dobra robota: N1 udowodnione FAIL→PASS (b-1-16/17), uczciwe obalenie własnych fałszywych alarmów Inventory, macierz
1500 prób, fuzzer z minimalizacją. Uwagi:
- Raport -o był 1 linią — od teraz `krytyk/raport-r2.md` piszesz SAM jako plik (max 50 linii), a -o to
  `krytyk/koniec-r2.txt`.
- Opóźnienia start/stop z macierzy są bezużyteczne (start w 136/300 komórek, koniec w 64/300). To był główny miernik
  feelingu — trzeba go zrobić porządnie.
- Pomiar wydajności z `debug:true` nie nadaje się do wniosków.

## Runda 2 — zakres (kolejność = priorytet)
1. **Recenzja rundy 2 Agenta A** (pracuje teraz; diff względem `24cc940` w worktree `zaklecia-naprawy`, nakładka
   `naprawy/plugins-overlay/`). SZCZEGÓLNIE: A zamienił w nakładce `mmodamage` na `damage{dc=MAGIC…}` w skillach
   Płomieni/Odmrożenia. Sprawdź, czy to nie zabija skalowania obrażeń statystykami (MMOCore/MythicLib: `skill_damage`,
   `magic_damage`, żywioły FIRE/ICE, obrona celu; Wiara daje skill_damage). Test A/B: ta sama postać z nadanym
   atrybutem/statystyką (np. `mmocore admin attribute give` albo stat MythicLib) — obrażenia na `build bazowy` vs build A.
   Werdykt AKCEPT/ODRZUĆ dla każdej zmiany.
2. **Opóźnienia naprawdę:** start = pierwszy pakiet `damage_event`/`entity_status` (hurt) celu albo spawn
   `item_display` strumienia należącego do rzucającego (identyfikuj po modelu `zaklecia:strumien_*`) albo zmiana many
   w pakiecie — od chwili wysłania pakietu wejścia; stop = `entity_destroy` TEGO strumienia / ostatni hurt. Każdy typ
   zaklęcia, prawa/lewa/obie, min. 10 powtórzeń, mediana i p95 w tikach, n=10 obowiązkowo. Kontrola: harness musi
   pokazać opóźnienie także dla zwykłego ciosu i dla puszczenia łuku (pomiar umie zobaczyć zdarzenie).
3. **Nowy błąd „podwójne leczenie”:** Leczenie dłoni / Wielkie uzdrowienie / Leczenie w trybie podwójnym nie leczą i nie
   pobierają many (MXF, MX3B, MXL2). Minimalna powtarzalna sonda + porównanie z innymi typami podwójnymi + plik:linia
   podejrzanej przyczyny w kodzie (bez naprawiania). To samo dla Odmrożenia 0 HP na buildzie 5d7400d4e847.
4. **Polityka obrażeń („invulnerable to player_attack”):** ustal, który plugin/ustawienie sprawia, że `/damage … by MagTest
   player_attack` na husku = „invulnerable”, a cios wręcz zwykłym mieczem nic nie robi (to prawdopodobnie ta sama
   przyczyna co N2). Metoda: rozpakuj jary z `/opt/docker/mc/data/world1/plugins` do `krytyk/jary/` i czytaj javap; HandlerList
   z logu A (jeśli A doda sondę priorytetów — skorzystaj). Wynik do Claude.
5. **Wydajność ponownie, debug:false:** `build bazowy` NIE zlecasz — poproś o porównanie w raporcie; zmierz na buildzie A z
   nakładką z debug:false (jeśli nakładka ma debug:true, zanotuj; Claude przełączy). 10/20 strumieni, MSPT, pakiety/s do
   obserwatora z podziałem na typy pakietów, spark: udział Zaklec. Wskaż 3 najdroższe miejsca w kodzie (plik:linia).
6. **Szum na akcję:** licz BossBar tylko Zaklec (po UUID pasków Zaklec), dźwięki po nazwie, komunikaty po treści. Tabela
   „akcja gracza → ile sygnałów” dla 10 typowych akcji. Budżet: ≤1 dźwięk, ≤1 komunikat, ≤1 aktualizacja paska na akcję.
7. **Fuzzer po poprawkach A:** 3 nowe ziarna × 1000 kroków na ostatnim buildzie A; każde naruszenie zminimalizowane;
   rozjazd papieru w slotach 14/15 — sprawdź pakietowo, czy to Zaklecia (set_slot wysyłany przez plugin) czy menu.
8. **Feeling oczami gracza** (krytycznie, bez łagodzenia): uaktualnij oceny 1–10 per typ zaklęcia i per czynność
   (wybór, rzut, łączenie z bronią, przerwania) z liczbami z pkt 2 i 6; porównaj z `../BADANIE_SKYRIM.md`; wypisz 10
   najbardziej irytujących rzeczy dla gracza, od najgorszej.
Pytania do właściciela (np. czy sojusznik ma zasłaniać strumień) zbierz osobno. STOP po raporcie.
