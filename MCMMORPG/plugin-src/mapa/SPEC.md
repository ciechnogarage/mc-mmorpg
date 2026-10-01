# Plugin Mapa — specyfikacja (Paper 1.21.11, Java 21, tylko paper-api)

Serwer MMORPG po polsku. Klient vanilla. Gracz nigdy nie wpisuje komend — wszystko klikiem.
Pakiet `pl.mcmmorpg.mapa`, klasa główna `MapaPlugin`. Wszystkie teksty dla gracza po polsku.
Komentarze w kodzie po polsku, krótko, bez polskich znaków w identyfikatorach.

## Dane wejściowe (dostarcza generator, plugin tylko czyta)

`plugins/Mapa/swiaty/<swiat>.bin` + `<swiat>.json`
- json: `{"x0": int, "z0": int, "w": int, "h": int}`
- bin: `w*h` bajtów, wiersz po wierszu (z rośnie w dół): bajt koloru mapy Minecrafta (baza*4+odcień,
  jak w `colors` mapy vanilli) dla bloku (x0+i, z0+j). 0 = brak danych (na mapie przezroczyste = tło pergaminu).

`plugins/Mapa/kafle.json` — siatka kafelków okna (atlasu):
```json
{"swiaty": {"world": {"x0": -368, "z0": -656, "poziomy": [
   {"bpp": 3, "kol": 22, "wier": 27}, {"bpp": 6, "kol": 11, "wier": 14}, {"bpp": 12, "kol": 6, "wier": 7}]}}}
```
Kafelek (tx, tz) na poziomie o `bpp` (bloków na piksel) obejmuje bloki
x ∈ [x0 + tx*18*bpp, x0 + (tx+1)*18*bpp), analogicznie z. Model przedmiotu kafelka:
`mapa:<swiat>/b<bpp>/<tx>_<tz>` (NamespacedKey). Poza siatką albo brak świata: `mapa:pusty`.
Kafelek, w którym stoi gracz, dostaje komponent custom_model_data ze stringiem `g0`..`g7`
(kierunek patrzenia, 8 kierunków; `dir = Math.floorMod(Math.round(yaw / 45f), 8)`, yaw Minecrafta:
0 = południe (+z), 90 = zachód, 180 = północ, 270 = wschód). Pack sam dorysuje strzałkę (select po
custom_model_data). Ikony przycisków: `mapa:ui/lewo`, `mapa:ui/prawo`, `mapa:ui/gora`, `mapa:ui/dol`,
`mapa:ui/srodek`, `mapa:ui/plus`, `mapa:ui/minus`, `nexo:hub_wstecz`, `nexo:hub_zamknij`.

`plugins/Mapa/config.yml` (domyślny w jarze, generator może nadpisać):
```yaml
mapa-id: 30000            # id mapy vanilli z przedmiotu UI_MAPA (slot 7)
zoom-reka: [1, 2, 4, 8]   # bloków na piksel mapy w ręce
zoom-reka-domyslny: 2
odswiezanie-ms: 250       # min. odstęp przerysowania terenu mapy w ręce (na gracza)
znaczniki:
  - {swiat: world, x: 149, z: 197, nazwa: "Start", typ: start, opis: ["Tu zaczyna każda nowa postać."]}
  - {swiat: world, x: 28, z: -188, nazwa: "Gromowładca", typ: legendarny, opis: ["Legendarny Chowaniec."]}
  # ... typ: start | legendarny | strefa | miasto | loch
tytul-okna: "䅐"   # znaki przesunięć + glif tła (chw_panel_6), patrz niżej
```

## 1. Mapa w ręce (dynamiczna)

- `onEnable`: `MapView v = Bukkit.getMap(mapa-id)`; jeśli null — log WARN i pomiń tę część.
  Usuń wszystkie renderery, dodaj własny `MapRenderer(true)` (contextual = osobne płótno na gracza).
  `v.setTrackingPosition(false)`, `v.setUnlimitedTracking(false)`, `v.setLocked(true)`.
- `render(view, canvas, player)` jest wołane co tick dla trzymającego. Stan per gracz (mapa UUID ->
  stan): ostatni środek (cx, cz), zoom, świat, czas ostatniego przerysowania.
  - Przerysuj teren tylko gdy: zmienił się świat albo zoom, albo gracz przesunął się o ≥ zoom bloków
    od ostatniego środka; i minęło ≥ odswiezanie-ms. Teren: piksel (px, py), px,py ∈ [0,128):
    blok (cx + (px-64)*zoom, cz + (py-64)*zoom) -> bajt z `<swiat>.bin` (0 poza danymi) ->
    `canvas.setPixel(px, py, bajt)` (deprecated, ale daje dokładny bajt palety — użyj z @SuppressWarnings).
  - Brak danych dla świata (np. instancja lochu): wyczyść płótno bajtem 0 i napisz na środku
    `canvas.drawText(…, MinecraftFont.Font, "Brak mapy")` (tylko raz po zmianie świata).
  - Kursory (co tick, tanie): nowa `MapCursorCollection`; gracz w środku (0,0) typ PLAYER,
    kierunek `Math.floorMod(Math.round(yaw / 22.5f), 16)`. Znaczniki z configu w tym świecie, w zasięgu
    mapy: współrzędne kursora = `(mx - cx) * 2 / zoom` (zakres -128..127, odrzuć poza), typ:
    start -> BANNER_WHITE, legendarny -> BANNER_RED, strefa -> BANNER_LIME, miasto -> BANNER_YELLOW,
    loch -> BANNER_PURPLE; caption = nazwa (Adventure Component / String — co daje API 1.21.11).
    `canvas.setCursors(...)`.
- Rozpoznanie przedmiotu: `FILLED_MAP` z `MapMeta` i `getMapId() == mapa-id`.
- Interakcja (PlayerInteractEvent, ręka główna, przedmiot = nasza mapa, każda akcja):
  - ze Shiftem (isSneaking): następny zoom z `zoom-reka` (w kółko), action bar
    `Przybliżenie: 1 piksel = N bl.`; cancel.
  - bez Shifta: otwórz okno (atlas) wyśrodkowane na graczu; cancel.
- Wyjście gracza: wyczyść stan.

## 2. Okno „Mapa świata” (atlas z kafelków)

Inventory 54 sloty, tytuł = Component tekstowy: najpierw biały kolor (WAŻNE: kolor na CAŁYM
komponencie, inaczej glif ciemnieje), potem `tytul-okna` z configu, potem `Mapa świata · <nazwa świata>`.
Holder własny (klasa implementująca InventoryHolder), żeby rozpoznawać okno.

- Sloty 0..44 (9 kol. × 5 wierszy) = kafelki. Stan okna per gracz: świat, poziom (indeks w `poziomy`),
  lewy górny kafelek (tx0, tz0). Slot s: tx = tx0 + s%9, tz = tz0 + s/9.
  Przedmiot: PAPER z `DataComponentTypes.ITEM_MODEL` = klucz kafelka; nazwa (ITEM_NAME / customName
  bez kursywy): nazwy znaczników leżących w tym kafelku (połączone „, ”) albo „Okolica x…, z…” (środek
  kafelka); lore: opisy znaczników + szara linia `x ..., z ...`; ostatnia linia `Klik — wyśrodkuj tutaj`.
  Kafelek gracza: dodatkowo CUSTOM_MODEL_DATA ze stringiem `g<dir>` i nazwa „Tu jesteś”.
  Ukryj zbędne linie tooltipu (TOOLTIP_DISPLAY / hide additional tooltip) jeśli API pozwala.
- Wiersz 6 (sloty 45..53): 45 Wstecz (`nexo:hub_wstecz`, wykonaj jako konsola
  `dm open hub_przygoda <gracz>`), 46 ← (`mapa:ui/lewo`), 47 ↑, 48 ↓, 49 →, 50 Wyśrodkuj na mnie
  (`mapa:ui/srodek`), 51 Przybliż (`mapa:ui/plus`), 52 Oddal (`mapa:ui/minus`), 53 Zamknij (`nexo:hub_zamknij`).
  Strzałki przesuwają o 3 kafelki (w granicach siatki; okno może wystawać poza siatkę o max 4 kafelki,
  tam `mapa:pusty`). Zoom zmienia poziom, zachowując środek widoku w blokach.
- Klik w kafelek: wyśrodkuj widok na tym kafelku.
- Otwarcie: środek widoku = pozycja gracza (jeśli świat ma kafle), inaczej świat `world` wyśrodkowany
  na znaczniku `Start`. Domyślny poziom = indeks 1.
- Co 20 ticków (jedno zadanie dla wszystkich otwartych okien) odśwież TYLKO kafelek gracza (stary
  przywróć do zwykłego, nowy oznacz), żeby strzałka szła za graczem i obracała się.
- Bezpieczeństwo: InventoryClickEvent i InventoryDragEvent z naszym holderem na górze -> zawsze
  cancel (także shift-klik z dolnego ekwipunku i klawisze numeryczne). Zamknięcie -> usuń stan.

## 3. Komendy (tylko konsola / OP: permission `mapa.admin`, default op)
- `/mapa otworz <gracz>` — otwórz okno graczowi (używane przez menu Q i CoreTools).
- `/mapa przeladuj` — przeładuj config.yml, kafle.json i swiaty/*.bin.

## Wymagania jakości
- Zero pracy na wątku głównym poza tym, co konieczne; wczytanie .bin przy starcie/przeładowaniu.
- Żadnych wyjątków przy braku plików (log WARN, funkcja wyłączona).
- Testy JUnit dla czystej logiki: przeliczenie kierunków (yaw -> g0..g7 i 0..15), współrzędne kursora,
  slot -> kafelek, przesuwanie/zoom z zachowaniem środka.
- plugin.yml: `api-version: '1.21'`, `version: ${version}`, komenda mapa.
