# Plugin Mapa v4 — MAPA NA CAŁY EKRAN (dodatek; SPEC.md i SPEC-v3.md obowiązują)

Zależność: plugin **MapEngine 1.8.12** (Paper, `softdepend: [MapEngine]`, `compileOnly("de.pianoman911:mapengine-api:1.8.12")`
z repo `https://repo.minceraft.dev/releases/` — sprawdź dokładne współrzędne w README/javadocu MapEngine i dopisz do
build.gradle.kts). Brak MapEngine = funkcja wyłączona (WARN w logu), reszta pluginu działa jak dotąd.

## Otwieranie / zamykanie
- `/mapa pelna <gracz>` (konsola/OP), przycisk w atlasie: wiersz zakładek, slot 8 = „Pełny ekran” (`mapa:ui/pelny`),
  zamyka atlas i otwiera pełną mapę na świecie/widoku atlasu.
- Na starcie sesji: obróć gracza do najbliższej strony świata (yaw ∈ {0, 90, 180, 270}), pitch 0 (teleport do
  tej samej pozycji z nową rotacją). Ruch zablokowany: PlayerMoveEvent ze zmianą x/y/z -> `setTo(from` z yaw/pitch z
  `to)`. Obrót głowy dozwolony (to kursor).
- Ekran: MapEngine display o rozmiarze `pelna.szerokosc` × `pelna.wysokosc` map (config, domyślnie 4 × 3), na
  płaszczyźnie prostopadłej do kierunku patrzenia, środek w odległości `pelna.odleglosc` (domyślnie 2.2) bloku od
  oczu gracza, na wysokości oczu. Widoczny TYLKO dla tego gracza (receivers). Rozdzielczość: 128·szer × 128·wys px.
- Zamknięcie: Shift (PlayerToggleSneakEvent, wciśnięcie), klik LPM w przycisk „✕” na górnym pasku, wyjście z gry,
  zmiana świata, śmierć, teleport (PlayerTeleportEvent innego powodu niż nasz). Po zamknięciu: zniszcz display, odblokuj ruch.

## Wejście (wszystko dla graczy w sesji; zdarzenia anulowane, żeby inne pluginy nie reagowały)
- **Kursor** = przecięcie linii wzroku (oczy + kierunek) z płaszczyzną ekranu, przeliczone na piksel (u, v) obrazu;
  poza ekranem = brak kursora. Liczone co tick (zadanie co 1 tick dla wszystkich sesji).
- **Przeciąganie** = trzymany PPM: klient przy trzymanym PPM wysyła „użyj” co ~4 ticki (PlayerInteractEvent
  RIGHT_CLICK_AIR / RIGHT_CLICK_BLOCK, ręka główna). Stan „trzymany” = ostatnie takie zdarzenie ≤ 6 ticków temu.
  Podczas trzymania przesuwaj środek mapy o `-(Δu, Δv) * zoom` bloków (Δ kursora między tickami).
- **Kółko myszy** = PlayerItemHeldEvent: `d = floorMod(nowy - stary + 4, 9) - 4`; d < 0 -> przybliż, d > 0 -> oddal
  (lista zoomów jak `zoom-reka`, zachowaj punkt pod kursorem w miejscu, jeśli prosto, inaczej środek); anuluj event.
- **LPM** = PlayerAnimationEvent (ARM_SWING) albo PlayerInteractEvent LEFT_CLICK_*: klik w punkcie kursora
  (zakładki/✕ na pasku, znacznik -> pokaż jego opis w action barze, puste miejsce -> nic). Anuluj interakcje,
  obrażenia zadawane przez gracza (EntityDamageByEntityEvent) i niszczenie bloków.

## Obraz (BufferedImage ARGB, rysowany na wątku asynchronicznym, wysyłany przez MapEngine z ditheringiem WYŁĄCZONYM)
- Teren: dla każdego piksela blok wg zoom (jak w ręce: zoom < 1 -> tekstura 4×4 z `.blk`/`tekstury.bin`,
  zoom ≥ 1 -> bajt z `.bin`), kolor bajtu -> RGB przez tablicę palety map (`org.bukkit.map.MapPalette.getColor(byte)`).
  Brak danych = pergamin #C4B288.
- Znaczniki świata: ikona PNG z `plugins/Mapa/ikony/<ikona>.png` (pole `ikona` w znacznikach configu; 16×16 RGBA,
  rysuj 2× = 32×32 px gdy zoom ≤ 1, inaczej 16×16), pod nią nazwa (biały tekst z czarnym cieniem, font `Dialog`
  BOLD 11, antyaliasing OFF).
- Gracz: biała strzałka z czarnym konturem (trójkąt, obrót wg yaw) w jego pozycji, jeśli w tym świecie.
- Górny pasek (wysokość 20 px, tło półprzezroczyste ciemne): zakładki z configu `zakladki` (nazwa; aktywna
  podkreślona złotem #E0C38C), po prawej „✕”. Dolny pasek (14 px): `Przybliżenie: …  ·  PPM — przeciągnij · kółko —
  zoom · Shift — wyjście`.
- Kursor: mały celownik 7×7 px (biały z czarnym konturem) w (u, v).
- Pełne przerysowanie tylko gdy zmienił się widok (środek/zoom/zakładka) albo co 10 ticków (ruch gracza/strzałka);
  sam kursor -> tylko jego obszar (albo całość — MapEngine i tak wyśle tylko zmienione piksele). Max 10 wysyłek/s.

## Config (dopisz do config.yml)
```yaml
pelna:
  szerokosc: 4
  wysokosc: 3
  odleglosc: 2.2
```
Znaczniki: pole `ikona` (nazwa pliku bez .png) — generator wypełni.

## Testy JUnit (czysta logika)
przecięcie promienia z płaszczyzną -> (u, v) dla 4 kierunków; kółko d dla przejść 8->0 i 0->8; przeliczenie
przeciągania na przesunięcie; trafienie zakładki/✕ na pasku.
