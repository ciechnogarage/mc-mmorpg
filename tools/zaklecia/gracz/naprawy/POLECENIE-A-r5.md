# Polecenie Claude dla Agenta A — runda 5

Runda 4 przyjęta i zacommitowana przez Claude: Zaklecia `b8a73aa`, KsiegaPostaci `1433dfb`. Dalej pracujesz bez commitów.

## ZMIANA ŚRODOWISKA (zasady właściciela — przeczytaj `/home/dev/.claude/CLAUDE.md`)
- Klon to teraz `klon-zaklecia` (wspólne `klon.sh`), dane `/opt/docker/mc/klony/zaklecia/data` (log do czytania:
  `.../data/logs/latest.log`). Kolejka działa jak dotąd (`build`, `build bazowy`, `test <filtr>`).
- Testy biegną przez wspólnego bota: `bot/` montowane TYLKO DO ODCZYTU, node_modules z `/bot/node_modules`, wyniki JSON
  w `/opt/docker/mc/narzedzia/bot/wyniki/zaklecia/`. Nowe WARN/ERROR Zaklec/Ksiegi z logu klona runner dopisuje na końcu
  `.out` w sekcji `BLEDY-LOGU` — każda poprawka ma tam dać zero nowych linii.
- Własne sondy dalej jako `bot/scenariusze/N_*.js` (piszesz plik na hoście, kontener czyta).

## Runda 5 — błędy z rundy 3 Agenta B (`krytyk/raport-r3.md` — przeczytaj całość)
1. **WYS `ConcurrentModificationException` w `Rzucanie.tick` (pętla po `rzucajacy`, ok. linii 418)** — potwierdzone przez
   Claude: tik zaklęcia rzuca skill, skill zabija innego gracza synchronicznie, `PlayerDeathEvent` → `stop` →
   `rzucajacy.remove(...)` w trakcie iteracji. Przejrzyj WSZYSTKIE pętle po mapach stanu (Rzucanie, RzutyOdrzutowe,
   EfektyCzasowe, Strumien, Wybor) pod ten wzór. Sonda N_cme: dwóch graczy, PvP allow, jeden zabija drugiego Płomieniami
   → `BLEDY-LOGU` puste na Twoim buildzie, wyjątek na bazowym.
2. **WYS miecz w prawej + zaklęcie w lewej: cios LPM mieczem = 0** (B: `b-3-28f`, ten sam miecz bez zaklęcia w lewej
   9,84 — `BR3M`; Claude widział to samo w prawdziwym kliencie 09.10). Diagnoza obowiązkowo `DiagnostykaObrazen`
   (debug tylko na tę sondę) — kto anuluje / zeruje / czy cios w ogóle powstaje (attack_range nośnika? tryb LPM Zaklec?
   AnimacjeBroni? Ksiega?). Poprawka przyczyny + sonda FAIL→PASS (cios mieczem przy zaklęciu w lewej zadaje obrażenia,
   zaklęcie w lewej dalej działa PPM).
3. **WYS DwieBronie × Zaklęcia:** (a) przełączenie slotu tam i z powrotem → w ręce nośnik zamiast miecza (miecz znika
   z aktywnej dłoni) — błąd do naprawy; (b) przy aktywnym zaklęciu w lewej nie da się wziąć broni do lewej (slot 45 / F)
   — NIE projektuj zasad sam: ustal, co dokładnie blokuje, i opisz 2 warianty rozwiązania dla właściciela (np. broń w
   lewej zdejmuje zaklęcie z lewej z komunikatem). Źródło DwieBronie: gałąź `dwie-bronie` w repo MC (tylko czytasz).
4. **ŚR brak widocznego efektu Osłony i Uspokojenia** (mana schodzi, 30/30 prób bez znacznika). Sprawdź w skillach MM, czy
   jest jakakolwiek oprawa (cząsteczki/dźwięk/efekt) i czy realnie działa (redukcja obrażeń Osłony: zmierz cios przed/po;
   Uspokojenie: mob z AI przestaje atakować?). Jeśli oprawy brak — dodaj skromną w nakładce (aura przy rzucającym /
   znak nad celem), bez spamu.
5. **ŚR szum dźwięków:** krótki strumień PPM 0,85 s = 8 dźwięków, oburącz 9. Budżet: dźwięk startu + najwyżej 1 dźwięk
   pętli na 10 tików + dźwięk końca. Zmiana w nakładce skilli MM, A/B liczone sondą NOISE B (`test NOISE`).
6. **ŚR fuzzer: odtwarzalny rozjazd ekwipunku** — `/opt/docker/mc/narzedzia/bot/wyniki/zaklecia/fuzz-161803.json`
   (skrócona sekwencja 52 akcji, `paper` ×1 serwer / pusty klient). Ustal, kto wysyła zły stan (Zaklecia set_slot,
   Ksiega, CoreTools, MMOInventory) — tylko diagnoza + poprawka, jeśli to nasze pluginy.
7. **NIS** 2 nieustalone encje po sprzątaniu w `b-3-36d` (PERF20, boty ginęły) — zidentyfikuj (typ/tag), czy to wyciek
   Zaklec (strumień `ItemDisplay`, przywołaniec, znacznik runy).
Na koniec: `build bazowy` + `test ABCDEF,N_cme,N_melee2,NOISE` → `build` → ten sam test; tabela A/B. Ostatni build = Twój,
`debug:false`. Szczegóły `naprawy/szczegoly-r5.md`, krótki raport -o. Kolejka wspólna z B (B teraz stoi). STOP po raporcie.
