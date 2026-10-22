# Polecenie Claude dla Agenta A — runda 9 (decyzje właściciela 17.10 wieczór)

## Stan
Runda 8 (zasłanianie, PPM gasi obie) jest NA WORLD1 od 17.10 18:35 (`zaklecia-zaslanianie` 20f2ed8, scalone z ui-zaklecia).
Worktree:
- Zaklecia: `/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-zaslanianie/MCMMORPG/plugin-src/zaklecia/` (HEAD 20f2ed8 = world1).
- KsiegaPostaci: `/home/dev/Desktop/Przemek/MC/.claude/worktrees/ksiega-zaklecia-lewa/MCMMORPG/plugin-src/ksiega-postaci/`
  (gałąź `ksiega-zaklecia-lewa` od `hub-przywrocenie-1710` 9542518 = world1, bajtowo). `build` w kolejce buduje OBA.
- Nakładka `naprawy/plugins-overlay/` = tylko `Zaklecia/config.yml` z world1; nowe pliki kopiuj z AKTUALNEGO world1
  (`/opt/docker/mc/data/world1/plugins/`). Klon `klon-zaklecia` świeży z world1. Nie commitujesz, klonu nie zwijasz.

## Zakres
1. **Zasłanianie: wszystko zasłania, także własny przywołaniec — ale liczy się WIDOCZNA CZĘŚĆ celu.** Właściciel: „przywołaniec
   nie zasłania całego modelu, więc da się trafić — to daje więcej skilla graczowi”. Zmień `Zaslanianie.pierwszyCel`: zamiast
   jednego promienia do środka — kilka punktów sylwetki celu (np. głowa, środek, nogi, lewy/prawy bok ~0,8 szerokości hitboxu).
   Cel trafiony, jeśli DLA KTÓREGOKOLWIEK punktu pierwszą przeszkodą (blok lub istota, także własny przywołaniec) jest ten cel.
   Ustaw `zaslanianie.wlasne-przywolania-zaslaniaja: true` (config w źródle i nakładce). Model strumienia (`Strumien`) — długość do
   pierwszej przeszkody na linii wzroku (bez zmian poza tym, że własny przywołaniec też ją skraca).
   Wydajność: liczba promieni × kandydaci stożka co 10 tików — zmierz (spark albo licznik w debug), ma być pomijalne.
   Sondy: wilk przed huskiem na linii środka, ale głowa huska odsłonięta (celowanie w głowę) → trafienie; wilk/husk zasłaniający
   całość (np. drugi husk przed) → brak; własny wilk tuż przed na linii środka → brak przy celowaniu w środek.
2. **Sakwa w slocie 18:** w `CoreTools/Scripts/stale-sloty.yml` funkcja `stale_sloty_wstaw` woła Dziennik i mapę, ale nie
   `stale_sloty_wstaw_sakwa` (wypadła ~02.10 przy dodawaniu mapy). Przywróć wywołanie w nakładce (plik z aktualnego world1).
   KONIECZNIE sprawdź kolizje slotu 18 z tym, co dziś robi KsiegaPostaci (okno E, „ukryte sloty gracza”, panel MENU) i MMOInventory
   (akcesoria w rzędzie 9–17): Sakwa ma się pojawić u NOWEGO gracza (nowe konto bota), klik otwiera schowek, nic innego nie znika.
   Jeśli slot 18 jest dziś zajęty przez coś innego — STOP i opisz (decyzja właściciela), nie przenoś Sakwy sam.
3. **Broń do lewej ręki przy przypisanym zaklęciu w lewej (wariant „a” właściciela):** włożenie broni do lewej (przeciągnięcie
   w oknie E, F, shift-klik — każda droga, którą Księga/DwieBronie dziś pozwalają włożyć broń do lewej) ODKŁADA zaklęcie z lewej
   i wkłada broń, z jednym komunikatem „Odłożono zaklęcie z lewej ręki” (styl Komunikat/pasek MythicLib). Dziś blokują to
   `Nosnik.klik`/`zamianaRak` (Zaklecia) i reguły `Bron.validPair/swap` (Księga). Zero duplikatów i zgubionych przedmiotów:
   sonda z pełnym odczytem 41 slotów przed/po, także szybkie powtórki i F tam i z powrotem. Reguły rąk Księgi (dwuręczne tylko
   w prawej, tarcza w obu) zostają nietknięte.
Dla każdego: sonda FAIL (bazowy = world1) → PASS. Regresja: A, C, D, F, DB3, N_zaslona (zaktualizuj pod nową regułę), N_ppm1,
N_damage, STAT3, PW. Szczegóły `naprawy/szczegoly-r9.md`, krótki raport -o. STOP po raporcie.
