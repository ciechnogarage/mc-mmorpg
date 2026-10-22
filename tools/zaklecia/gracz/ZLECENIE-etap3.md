# Zlecenie dla Codexa — etap 3: domknięcie testu + wyniki prawdziwego klienta + ocena końcowa

Klon został zrestartowany po awarii (kolejka działa jak w etapie 2; nowe nazwy `e3-01`…). Claude potwierdził:
- KRYT rekurencja `pomocSlug`: `DamageCheckEvent` MythicLib odpala się tylko, gdy cel to GRACZ i świat ma PvP
  (bajtkod `EntityManager.canInteract`). Na world1 wszystkie światy Multiverse `pvp: true`, WorldGuard `pvp deny` tylko
  region Stolicy → na produkcji gracz z przywołańcem, który uderzy innego gracza / zostanie uderzony, kładzie serwer.
  **NIE uruchamiaj ponownie scenariusza przywołaniec + drugi gracz** (zablokuj go flagą w harnessie); przywołania testuj
  tylko z huskami.
- Sakwa: `CoreTools/Scripts/stale-sloty.yml` `stale_sloty_wstaw` nie woła `stale_sloty_wstaw_sakwa` (może być celowe —
  oznacz „do decyzji właściciela”).

## Prawdziwy klient (MagQA, Human poz. 1, bez OP) — zrzuty `klient/captures/` (obejrzyj je: K01a–f, K04-pas, K0506, K08, K08b2, K02-10)
1. Ścieżka: Q → MENU → Zdolności → Zaklęcia = 2 kliknięcia, Księga czytelna, opis zgodny z kosztem (1 many/s).
2. Po przypisaniu LPM/PPM w Księdze panel „Twoje dłonie” NIE odświeża się (dalej pusty), komunikat „Odmrożenie w lewej
   dłoni” prawie schowany pod dolną krawędzią okna. W podstronach huba ikony z ekwipunku gracza nakładają się na
   przyciski „Wróć do menu” i „Szybka podróż” (K01c, K01f).
3. Odmrożenie PPM 0,85 s: husk 20 → 6,2 HP, mana 20 → 18,3; strumień w 1. osobie zasłania ~połowę ekranu dużymi
   biało-niebieskimi płaszczyznami (K04-pas); 0,15 s po puszczeniu strumień jeszcze widać, po 0,75 s czysto.
   Angielskie „You are now in combat!”; vanillowy toast + czat „Monster Hunter” przy pierwszym zabiciu.
4. LPM prawa = przełącznik działa (włącz/wyłącz). PPM + klik LPM = obie działa, ALE po puszczeniu PPM prawa (ogień)
   pali się dalej, bo LPM przełączył prawą — gracz musi dokliknąć LPM. Sprawdź w kodzie, czy to zamierzone (v2.8:
   „obie do puszczenia PPM”).
5. **WYS/KRYT: broń w prawej + zaklęcie w lewej = zero obrażeń.** Postać Human bez biegłości: „Nie możesz tego użyć —
   biegłość miecze” (start bez biegłości to osobna sprawa klasy). Po `mmocore admin attribute give MagQA bieglosc-miecz 1`:
   MIECZ_KAMIENNY w prawej, Odmrożenie w lewej — 3×LPM mieczem: 0 obrażeń; PPM odmrożenie: mana schodzi, strumień trafia,
   0 obrażeń. Kontrola na tym samym husku sekundę wcześniej z nośnikiem w prawej: 20 → 9,7. Znajdź przyczynę w kodzie
   (Zaklecia: atrybuty/komponenty trybu zaklęć, `attack_range`, sprawca obrażeń; AnimacjeBroni z worktree `kom-ab`;
   MythicLib/MMOItems) i odtwórz botem (scenariusz C01 z biegłością nadaną w przygotowaniu sceny).
6. Shift+Q: pasek BossBar to drobny tekst u góry obok karty gracza — mało czytelny w walce; kółko i „[pusta]” działają.
7. Zmiana slotu (3) i F w trakcie rzucania lewą: lewa dalej rzuca (mana schodzi), dłonie bez zmian — spójne.

## Do zrobienia
- Napraw w harnessie to, co zostało (B po poprawce komponentów, C01 z bronią + biegłością, C tarcza/mikstura/kilof
  z czekaniem na serwerowy SelectedItem, B21 limity) i przepuść A–F 2× przez kolejkę; zero BLAD.
- Diagnoza punktu 5 (plik:linia, mechanizm) — bez poprawiania pluginu.
- Zaktualizuj `OCENA.md`: oceny feelingu z uwzględnieniem obrazu z klienta, lista błędów wagą (z dowodem: przebieg
  kolejki albo zrzut), osobno „decyzje właściciela” (np. punkt 4, Sakwa) i „poza zaklęciami” (biegłość startowa,
  nakładanie ikon w hubie, angielskie komunikaty, toast osiągnięć). Na końcu TOP 5 poprawek w kolejności.
- `README.md` w katalogu roboczym: jak postawić klon (`klon.sh`), uruchomić bota (`bot/run.sh [A-F]`), kolejkę, klienta
  (`klient/start.sh`, `klient/k.sh`), czego bot nie widzi.
Zasady jak wcześniej. Raport `-o` (`/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/raport-etap3.md`) max 40 linii po
polsku: wyniki przebiegów liczbowo, przyczyna punktu 5, TOP błędy, oceny w skrócie. Bez kodu i diffów.
