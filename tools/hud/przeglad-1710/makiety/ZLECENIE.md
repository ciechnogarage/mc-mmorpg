# ZLECENIE — makiety nowego HUD-a (17.10)

Kontekst: przegląd 17.10, źródło strony `/home/dev/Desktop/Przemek/MC/tools/hud/przeglad-1710/index.html`
(przeczytaj sekcje „Sześć przyczyn” i „Rekomendacja”). Właściciel wybrał wariant B: przebudowa, ZACZYNAMY OD MAKIET.
Właściciel: „podoba mi się okno postaci” — Karta Wędrowca (portret, nick, klasa, poziom) ma ZOSTAĆ jako okno postaci.
Pasek przeżycia (HP, zasób klasy, stamina, tlen) schodzi nad hotbar (jak Wynncraft).

## Dwie rodziny wariantów (obie rysujesz)

- **L — okno postaci w lewym górnym rogu.** Karta odchudzona: portret, nick, klasa, poziom, cienki EXP, rząd ikon
  efektów z czasem (trucizna, „Rany po śmierci”, buff). BEZ pasków HP/zasobu/staminy (te są na dole). W walce karta
  NIE znika i NIE zamienia się w tekst; może się zwinąć do samego portretu + poziomu + efektów.
- **P — okno postaci w prawym górnym rogu, połączone z minimapą w jedno okno** (jedna rama: mapa + obok/pod nią
  portret, nick, klasa, poziom, EXP, efekty; pod spodem cel zadania max 2 wiersze). Lewy górny róg pusty.
  Minimapa NMinimap ma STAŁY rozmiar fizyczny 254×254 px (127 px × scale 2) przy 1920×1080, przy każdej skali GUI
  (offset 60/60 px fizycznych od prawej/góry; sprawdź `plugins/NMinimap/config.yml`). Rama musi obejmować mapę przy
  GUI 2, 3 i 4 — policz to i pokaż. Jeśli przy którejś skali się nie da, pokaż jak to wygląda i napisz wprost.

W obu: **pas przetrwania nad hotbarem** — HP najgrubszy z liczbą; zasób klasy z liczbą; stamina; tlen tylko pod wodą;
medalion poziomu może być w środku pasa ALBO w karcie (wybierz i uzasadnij). Wolny pas wokół celownika.
Zaproponuj 2 podwarianty pasa (L1/L2, P1/P2) — różna forma, nie tylko kolor.

## Stany (każdy wariant, GUI 3 = 1920×1080, 3 px na jednostkę GUI)

1. `spokoj` — HP 64/64, mana 80/120, stamina pełna, poziom 37, klasa „Miecz Korony”, nick „Przemidar”.
2. `walka` — HP 14/64 (22%), stamina 3/10, w walce, efekt „Rany po śmierci 2:41”. Alarm niskiego HP wyraźny.
3. `akolita` — klasa „Nosiciel Relikwii”, zasób = **Dusze 3/10** (u Akolity mana MMOCore nazywa się Dusze, max 10,
   bez regeneracji — narysuj jako 10 kulek/segmentów, inna ikona niż kropla many), trucizna 0:12 (pasek HP inny).
4. `woda` — pod wodą, tlen 6/10.
5. `skrajne` — poziom 100, klasa „Wołający Przodków”, nick 16 znaków `Wędrowiec1234567` — nic nie wychodzi z ramy.
Dodatkowo `spokoj` przy **GUI 2** i **GUI 4** (ten sam ekran 1920×1080) dla każdego wariantu.

## Czego trzymać się bezwzględnie

- Paleta Księgi: słownik `P` z `/hud/makiety2_gen.py` (14 barw), jeden ciemny kontur, płaskie tony, zero szumu.
  Grafika w gęstości 2× (1 teksel = pół piksela GUI), każda pojedyncza tekstura ≤256 px (limit Nexo) — większe dziel
  na kawałki i napisz, z ilu się składa.
- Napisy WYŁĄCZNIE bitmapową czcionką Minecrafta z `/hud/mcfont` (ascii.png + accented.png, polskie znaki), jak
  w `makiety2_gen.py`. Skalowanie tylko NEAREST, tylko całkowite.
- Vanilla, która zostaje: hotbar 182×22 GUI na dole środka (tekstura `/hud/praca/pack/assets/minecraft/textures/gui/sprites/hud/hotbar.png`),
  nazwa trzymanego przedmiotu ~59 GUI nad dołem, action bar ~68 GUI nad dołem, nazwy zaklęć MythicHUD 104 GUI nad
  dołem (`plugins/MythicHUD/layouts/zaklecia.yml`), bossbar u góry środka, czat lewy dół (x 0–~330 GUI, od ~40 GUI
  nad dołem w górę). Pas nie może kolidować z hotbarem, nazwą przedmiotu ani action barem — jeśli coś przesuwasz,
  napisz jak.
- Wykonalność w MythicHUD 1.3.4: position 0–100 od lewego DOLNEGO rogu, offset w px GUI, y w górę; pierwsza warstwa
  na wierzchu; warunki na warstwach TEKSTU są ignorowane (napis warunkowy = osobny asset); paski = `function` +
  `progress`; ikona efektu z czasem wymaga placeholdera, którego dziś nie ma (w makiecie rysuj, w raporcie oznacz).
  Tekst i tekstury MythicHUD skalują się z GUI — przy GUI 2 dzisiejsza karta się rozsypała, więc mierz GUI 2.
- Tło: dopóki `/w/tla/` jest puste, rysuj na prostym zastępczym tle (niebo + trawa). Gdy pojawią się
  `tla/dzien.png`, `tla/noc.png`, `tla/walka.png`, `tla/woda.png` (1920×1080, kadry z gry bez HUD-a), używaj ich.

## Jak pracujesz

- Generator: `/home/dev/.claude/jobs/70c19f2b/tmp/mk/gen.py` (w kontenerze `/w/gen.py`; `tools/hud` jako `/hud`
  tylko do odczytu; bez sieci). Wyniki do `/w/out/`.
- Nie masz dockera. Uruchamiasz przez kolejkę: zapisz `/home/dev/.claude/jobs/70c19f2b/tmp/mk/kolejka/<n>.req`
  z jedną linią argumentów dla gen.py (n = 1, 2, 3…, każdy przebieg nowe n). Runner odpala
  `python3 -I /w/gen.py <argumenty>` w obrazie z PIL 12.3 (limit 300 s) i tworzy `<n>.out` (stdout+stderr+exit)
  oraz `<n>.done`. Czekaj na `.done` (sleep w pętli), czytaj `.out` i OGLĄDAJ wynikowe PNG.
- Iteruj sam: po każdym przebiegu obejrzyj makiety w powiększeniu, napraw wady (wychodzące napisy, krzywe ramy,
  kolizje, nierówne odstępy) i puść jeszcze raz. Do 12 przebiegów.
- Wyniki: `out/<wariant>_<stan>_gui<N>.png` (1920×1080) + `out/<wariant>_<stan>_zoom.png` (karta i pas ×2 NEAREST)
  + `out/plansza.png` (wszystkie warianty w `spokoj` i `walka` obok klatki Wynna; zmniejszanie tylko ×0.5 NEAREST).
  Klatka Wynna: `/home/dev/Desktop/Przemek/MC/docs/research/wynncraft/klatki/16_hud_gry_hotbar_hp_mana_A2m50.jpg`
  (skopiuj do `/home/dev/.claude/jobs/70c19f2b/tmp/mk/`, jeśli potrzebujesz jej w kontenerze).
- NIC nie zmieniasz w `/opt/docker/mc/data/world1` (tylko czytasz). Żadnych reloadów, restartów, RCON.

## Raport

Zapisz do `/home/dev/.claude/jobs/70c19f2b/tmp/mk/raport.md` (max 60 linii): lista plików; dla każdego wariantu
2–3 zdania (co dobre, co słabe); wymiary w px GUI przy GUI 3 (karta, pas, odstępy od hotbara); ile tekstur i jakie
rozmiary; czego brakuje w danych (placeholdery); uczciwie: co nie wyszło.
