# Polecenie Claude dla Agenta B — runda 3

## Ocena rundy 2 (Claude)
Bardzo dobra: A/B skalowania Wiary (1,60× → 1,00×) przesądził odrzucenie obejścia A; uczciwe obalenie „podwójnego
leczenia”; dowód bajtkodowy `Bron.damage`. Runda przerwała się na PEŁNYM DYSKU (`/` 100%, dysk wspólny z produkcją!).
Zasady od teraz: nie trzymasz kopii jarów/rozpakowań > 20 MB (`krytyk/jary` po użyciu kasuj do samych plików .txt),
nie generujesz plików > 50 MB; kolejka odmówi pracy (kod 8) przy <3 GB wolnego — wtedy STOP i raport.
Raport `krytyk/raport-r3.md` piszesz SAM jako plik; -o to `krytyk/koniec-r3.txt`.

## Odpowiedzi na Twoje pytania
1/2. Sojusznik zasłaniający strumień i leczenie neutralnych graczy w PvP — DECYZJE WŁAŚCICIELA; zostaw jako pytania.
3. „Stop” bez strumienia: leczenie = ostatni wzrost HP celu (pakiet `update_health` dla siebie, `entity_metadata` zdrowia /
   `damage_event` dla innych), tarcza = zniknięcie efektu/atrybutu (pakiety `update_attributes`/`entity_effect`/`remove_entity_effect`),
   koncentracja = ostatni pakiet dźwięku/cząsteczek tego czaru przypisany po źródle, przywołania = spawn encji. Gdy nie ma
   żadnego znacznika — zapisz „brak sygnału” i to jest wniosek o feelingu (gracz też nic nie widzi).
4. debug przełączony na `false` w nakładce; po najbliższym buildzie A jest aktywny.

## Runda 3 — zakres
1. **Recenzja rundy 3 A** (diff Zaklec względem `24cc940` i KsiegaPostaci względem `1e1c108` w worktree
   `/home/dev/Desktop/Przemek/MC/.claude/worktrees/ksiega-naprawy-zaklecia/`): werdykty AKCEPT/ODRZUĆ z dowodem.
   Kluczowe: poprawka `Bron.damage` — zaklęcia (wszystkie typy) z mieczem w slocie 5 trafiają, Wiara 1,60× zachowana
   (`STAT2` na buildzie A), a ZASADA BRONI zostaje (zwykły cios ze slotu 5 nadal 0, ze slotu 0 działa). Szukaj dziur:
   czy gracz może teraz bić bronią z dowolnego slotu przez skill/pocisk/łuk, czy MMOCore skille klas działają.
2. **Nowy plugin DwieBronie** (world1 od 10.10, jest na klonie, `data/plugins/DwieBronie/`): pełna macierz interakcji z
   zaklęciami — broń w lewej + zaklęcie w prawej i odwrotnie, przełączanie, PPM/LPM, Shift+Q, F, śmierć. To nowe źródło
   konfliktów rąk; traktuj jak grupę C.
3. **Opóźnienia start/stop** wg odpowiedzi 3, n=10 na komórkę, mediana/p95 w tikach, każdy typ × prawa/lewa/obie.
4. **Wydajność debug:false**: 10/20 strumieni, MSPT, pakiety/s z podziałem na typy, spark; porównanie z `build bazowy`
   — zgłoś mi w raporcie, czy chcesz bazowy (zlecę go wtedy osobno, żeby nie mieszać z A).
5. **Szum na akcję** (tylko paski Zaklec po UUID, z uwzględnieniem pustego ADD MythicHUD jako zamknięcia) — tabela 10 akcji.
6. **Fuzzer** 3 ziarna × 1000 kroków na ostatnim buildzie A (z DwieBronie w puli akcji: broń do lewej ręki).
7. **Feeling** — oceny 1–10 per typ i per czynność + 10 najbardziej irytujących rzeczy, krytycznie.
STOP po raporcie.
