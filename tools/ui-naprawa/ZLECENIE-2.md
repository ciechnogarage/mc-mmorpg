# Runda 2 — poprawki po recenzji (nadzorca + recenzent huba)

Kontekst, zasady, kolejka i zakazy: bez zmian, patrz `/home/dev/Desktop/Przemek/MC/tools/ui-naprawa/ZLECENIE.md` (przeczytaj ponownie). Twój raport rundy 1: `$U/RAPORT.md`. Twoje zmiany w kodzie są już zacommitowane (gałęzie ui-*, src/chowance) — pracuj dalej na nich.

Werdykt rundy 1: zarzuty „odhaczone”, ale w kilku miejscach kosztem nowego bałaganu — gracz dostał WIĘCEJ tekstu na ekranie i czacie. To jest sprzeczne z Twoimi własnymi źródłami (S1 obciążenie poznawcze, S10 redukcja zagracenia). Runda 2 ma to odkręcić bez cofania korzyści.

## A. Najpierw: rzeczy GORSZE niż przed rundą 1 (bramka „nie gorzej”)
Pełna recenzja huba: `$U/hub-uwagi.md`, sekcja „Runda 1” — punkty R1-1…R1-7 wykonaj WSZYSTKIE dokładnie jak opisano (to druga sesja Claude recenzująca hub; jej bramka na końcu sekcji obowiązuje). Najważniejsze:
1. R1-1: przywróć kb_1/2/4.png z produkcji (jednowierszowe podpisy kafli). Różnica bliźniaczych kafli w 1. linii dymku.
2. R1-2: usuń wszystkie `[message]` z open_commands (spam czatu przy każdym Q). #10 zostaje CZĘŚCIOWO, jeśli nie ma innej drogi.
3. R1-3: USTAWIENIA od nowa: jedna opcja = jeden przełącznik w stylu Księgi ze stanem w dymku, bez BARRIER, bez gołych przedmiotów vanilla, bez ARROW.

## B. Uwagi nadzorcy (z Twoich zrzutów)
4. Ściana tekstu w walce (hudm-63-combat, GUI 4): pasek moba ogromny z bliska, cel „Pierwsze kroki” nachodzi na liczby obrażeń, dwa bloki „[OK] … · Gotowa / Koszt: N many” na środku ekranu, plus linia „HP/Mana/Siły”. Cel: w walce nad hotbarem maks. 2 linie tekstu łącznie.
   - stan czaru: bez ASCII „[OK]”/„[...]”; gdy czar gotowy i nie ładujesz — nie pokazuj nic albo jeden mały znacznik/ikonę; koszt pokazuj tylko przy ładowaniu i przy odmowie; postęp ładowania jako pasek/glif w stylu HUD, nie „1,1 s (12%)” tekstem.
   - pasek moba: skala zależna od odległości nie może dawać giganta z bliska — ogranicz maksimum (pomiar szerokości w px przy GUI 2/3/4, z bliska i z 10 bl.).
   - cel na HUD nie może nachodzić na pasek moba ani liczby obrażeń przy żadnym GUI scale.
5. Ładowanie czaru (test-53): 4 linie tekstu jedna nad drugą (status, koszt, „X w lewej dłoni”, HP/Mana/Siły). Zredukuj.
6. Preset „wysoki kontrast” (test-09): na zrzucie nie widać żadnej różnicy względem zwykłego. Albo realna, widoczna różnica (zrzut obok siebie), albo usuń opcję — atrapa jest gorsza niż brak.
7. Tryb kompaktowy usuwa całą kartę (HP/mana gracza znikają z narożnika?). Sprawdź, że w kompakcie gracz nadal widzi HP i zasób, i że to jest mniejsze, nie puste.
8. „Siły” jako nazwa staminy: sprawdź, jak zasób nazywa się w reszcie gry (karta, MMOCore, opisy) — jedna nazwa wszędzie.
9. Starter Chowańca (test-118): ściana drobnego tekstu w dialogu. Skróć: na przycisk 1 linia (nazwa · typ · rola), ruchy w dymku/po wybraniu, nie wszystko naraz.
10. Podstrony (H1): zostajemy przy TWOJEJ implementacji w UkrywanieEkwipunku (gałęzi menu-reszta NIE scalamy). Sprawdź KAŻDĄ stronę z `podstrony.strony` + `identyfikatory` w KsiegaPostaci/config.yml: dół MENU widoczny, EQ gracza niewidoczne i nieklikalne, „Wróć…” działa, po zamknięciu EQ wraca nietknięte (zrzut + odczyt `rcon data get entity UiQA Inventory` przed/po). Na test-12 na dole Dziennika są 2 puste niebieskie kwadraty w pasku stanu — sprawdź, czy to H2 na podstronach.

## C. Dowody
- Dla każdego ekranu zmienionego w rundach 1–2: para PRZED (zrzut produkcyjnej konfiguracji — `zrzuty-audytu/` albo Twoje `baza-*`) / PO przy GUI 3, plus GUI 2 i GUI 4 dla HUD. Złóż je w jeden arkusz `$U/zrzuty/ARKUSZ-runda2.png` (PIL, skala całkowita, podpisy), max ~12 par. Sam oceń każdą parę: lepiej / tak samo / gorzej — „gorzej” nie wychodzi z rundy.
- Bramka huba z hub-uwagi.md (MENU GUI 3 i 2, POMOC, USTAWIENIA po 2 przełącznikach, quest-list z dołem i „Wróć do menu”, czat po 3× Q pusty).
- Końcowe `wgraj` bez nowych WARN/ERROR.

## Wynik
Dopisz do `$U/RAPORT.md` sekcję „Runda 2” (maks. 40 linii): co zmieniono (A1–A3, B4–B10, R1-4…R1-7), ocena par przed/po, otwarte. Zaktualizuj statusy w tabeli 29 zarzutów, jeśli się zmieniły (np. #10). Odpowiedź do mnie: 5 linii.
