# Plugin Mapa v3 — zmiany względem SPEC.md (wszystko z SPEC.md obowiązuje, o ile niżej nie zmieniono)

## A. Mapa w ręce „z bloków” (zbliżenia poniżej 1 bloku na piksel)

Nowe pliki od generatora (tylko odczyt):
- `plugins/Mapa/swiaty/<swiat>.blk` — `w*h` liczb u16 little-endian (ten sam układ i ten sam `<swiat>.json`
  co `.bin`). Wartość: `indeks | (odcien << 14)`, indeks 0 = brak bloku, odcień 0 = ciemny, 1 = zwykły, 2 = jasny.
- `plugins/Mapa/tekstury.bin` — dla indeksu `i` (od 0) blok `i*48` bajtów: 3 odcienie × 16 bajtów, każdy
  odcień to tekstura 4×4 jako bajty kolorów mapy (wiersz po wierszu). Indeks 0 = same zera.

Config: `zoom-reka` przyjmuje ułamki (double): domyślnie `[0.25, 0.5, 1, 2, 4, 8]` = bloków na piksel;
`zoom-reka-domyslny: 1`; `zoom-reka-swiat: {stolica_odnowa: 0.5, loch_001: 0.5}`.
Rysowanie dla zoom < 1 (k = 1/zoom = 4 albo 2 piksele na blok):
blok = `cx + floorDiv(px - 64, k)`, analogicznie z; piksel w teksturze 4×4: `sx = floorMod(px - 64, k) * (4 / k)`,
`sy` analogicznie; bajt = `tekstury[idx*48 + odcien*16 + sy*4 + sx]`. Brak `.blk` lub indeks 0 -> jak dla zoom 1
(bajt z `.bin`). Zoom ≥ 1 bez zmian. Kursory: współrzędne `(m - c) * 2 / zoom` (już double), zakres jak wcześniej.
Przerysowanie: gdy blok gracza (floor x/z) się zmienił albo zmienił się zoom/świat (próg ruchu = max(1, zoom)).
Komunikat zoomu: `Przybliżenie: 4 piksele na blok` / `2 piksele na blok` / `1 piksel = N bl.`

## B. Lochy (instancje MythicDungeons)
Świat instancji ma nazwę `<szablon>_<liczba>` (np. `loch_001_0`). Wszędzie, gdzie plugin szuka danych świata
(teren, kafle, znaczniki, nazwy, zoom-reka-swiat), jeśli brak wpisu dla pełnej nazwy, a nazwa pasuje do
`^(.+)_\d+$`, użyj nazwy szablonu (grupa 1). Jedna funkcja `MapaData.klucz(String world)`.

## C. Atlas: zakładki
- Wiersz 0 (sloty 0..8) = zakładki z configu `zakladki:` (lista, max 9):
  `- {swiat: world, nazwa: "Kraina", ikona: "minecraft:grass_block"}`
  `- {swiat: stolica_odnowa, nazwa: "Stolica", ikona: "minecraft:bell"}`
  `- {swiat: loch_001, nazwa: "Loch 001 · Kwietna Polana", ikona: "minecraft:flowering_azalea"}`
  Przedmiot zakładki: materiał/model z `ikona` (ITEM_MODEL), nazwa, lore `Klik — pokaż tę mapę`; aktywna
  zakładka: `ENCHANTMENT_GLINT_OVERRIDE = true` i lore `Oglądasz tę mapę`. Puste sloty wiersza 0 = `mapa:pusty`
  z nazwą „ ” (bez tooltipu: TOOLTIP_DISPLAY hideTooltip).
- Kafelki: wiersze 1..4 (sloty 9..44) — siatka 9×4. `tileZ(slot) = top + (slot/9 - 1)`. Centrowanie i
  przesuwanie liczone dla 4 wierszy (środek = top + 2.0), granice: top ∈ [-4, rows - 1].
- Klik w zakładkę: przełącz świat okna; jeśli gracz jest w tym świecie (także instancji tego szablonu) —
  wyśrodkuj na nim, inaczej na pierwszym znaczniku tego świata, a bez znaczników na środku siatki.
  Poziom zoomu = domyślny (indeks 1, przycięty do liczby poziomów).
- Strzałka gracza i odświeżanie co sekundę: jak dotąd, uwzględniając instancje (B).
- Wiersz 5 bez zmian.

## Testy JUnit
klucz() dla `loch_001_0`/`world`/`stolica_odnowa`; piksel tekstury dla zoom 0.25 i 0.5 (także ujemne px-64);
slot -> kafel dla wiersza 1 i 4; zakładka poza listą.
