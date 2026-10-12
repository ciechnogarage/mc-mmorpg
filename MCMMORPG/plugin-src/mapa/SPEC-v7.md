# Plugin Mapa v7 — etap 3: PANEL MAPY ZAMIAST EKWIPUNKU W ATLASIE (dodatek; poprzednie SPEC obowiązują)

Cel: gdy gracz ma otwarty atlas, dolna część okna (jego ekwipunek: 27 slotów plecaka + 9 paska) pokazuje
panel mapy zamiast jego przedmiotów. Prawdziwy ekwipunek na serwerze NIE jest zmieniany — podmieniamy tylko
pakiety wysyłane do tego gracza (packetevents, softdepend; bez packetevents atlas działa jak dotąd).

## Pakiety (klasa `PanelPakiety`, ładowana tylko gdy packetevents włączony)
- Atlas zna swój window id: odczytaj go z wychodzącego `OPEN_WINDOW` dla gracza z otwartym atlasem albo z
  NMS-free sposobu dostępnego w API (np. pierwszy `WINDOW_ITEMS` po `openInventory` o liczbie slotów 54+36).
- Wychodzące do gracza z aktywnym panelem:
  - `WrapperPlayServerWindowItems` dla tego okna: elementy o indeksach 54..89 zastąp przedmiotami panelu
    (54..80 = plecak w kolejności wierszy, 81..89 = pasek), wartość „carried” bez zmian.
  - `WrapperPlayServerSetSlot` dla tego okna i slotu ≥ 54: zastąp przedmiotem panelu.
  - Pakiety dla okna 0 (własny ekwipunek) w czasie trwania panelu: przepuszczaj bez zmian.
- Zamknięcie atlasu (InventoryCloseEvent / wyjście / otwarcie innego okna): wyłącz panel i w następnym ticku
  `player.updateInventory()` — klient trzyma sloty 54..89 w TYM SAMYM modelu co swój ekwipunek, więc bez tego
  zobaczyłby przedmioty panelu w swoim ekwipunku.
- Przedmioty panelu budowane po stronie serwera jako `ItemStack` (Paper data components: ITEM_MODEL, ITEM_NAME, LORE,
  TOOLTIP_DISPLAY) i konwertowane `SpigotConversionUtil.fromBukkitItemStack`.

## Zawartość panelu
- Plecak (27 slotów, 3 rzędy): lista miejsc bieżącej mapy atlasu wg filtra — stałe znaczniki świata + własne gracza
  (funkcja `znacznikiDla`), sortowanie po odległości od gracza (gdy w tym świecie), inaczej alfabetycznie.
  Przedmiot: model `mapa:ikona/<ikona>` (generator dostarcza modele dla każdej ikony), nazwa = nazwa znacznika
  (własne: złota, stałe: biała), lore: opis (stałe) / „Twój znacznik” (własne), `Odległość: N m` (jeśli ten świat),
  `Klik — pokaż na mapie`, `Shift+klik — prowadź`, dla własnych `PPM — edytuj`.
  Więcej niż 27 -> ostatni slot = `mapa:ui/prawo` „Następna strona (n/m)”, a na stronach >1 pierwszy = `mapa:ui/lewo`.
- Pasek (9 slotów):
  0 `mapa:ui/plus` „Dodaj znacznik tutaj” — dialog dodawania w pozycji gracza (jeśli atlas pokazuje świat gracza;
    inaczej w środku widoku atlasu),
  1 filtr (cykl): Wszystko / Chowańce (`strefa`, `legendarny`) / Miasto (`miasto`, `start`) / Moje (`wlasny`);
    model zależny od filtra (`mapa:ikona/pin_biala`, `mapa:ikona/chw_wilkor`, `mapa:ikona/ender_eye`, `mapa:ikona/pin_czerwona`),
    nazwa „Pokaż: …”, lore „Klik — następny filtr”,
  2 prowadzenie: jeśli cel — `mapa:ikona/cel` „Prowadzę do: X”, lore „Klik — wyłącz prowadzenie”; inaczej pusty,
  3 minimapa: `minecraft:compass` „Minimapa: włączona/wyłączona”, klik -> `player.performCommand("minimap disable|enable")`
    (stan z PlaceholderAPI `%nminimap_enabled%` jeśli dostępny, inaczej zapamiętany przez nas),
  7 `mapa:ui/pelny` „Pełny ekran” (zamknij atlas, otwórz pełną mapę na tym widoku),
  8 `nexo:hub_zamknij` „Zamknij”.
- Kliknięcia: InventoryClickEvent z naszym holderem i rawSlot 54..89 (już anulowane) -> akcje wyżej; „pokaż” =
  przełącz świat atlasu (zakładka) i wyśrodkuj na znaczniku; po każdej zmianie przebuduj panel i wyślij
  `WindowItems` ponownie (np. `player.updateInventory()` gdy panel aktywny -> nasz filtr podmieni).
- Dialog dodawania/edycji z atlasu: `ZnacznikDialog` dziś wymaga aktywnej pełnej mapy (`fullActive`) — rozszerz o
  kontekst atlasu (Pending zapamiętuje skąd; po zapisie odśwież atlas i panel, jeśli nadal otwarty; jeśli dialog
  zamknął atlas — otwórz atlas ponownie na tym samym widoku).

## Testy JUnit
podział listy na strony (0, 27, 28, 60 elementów); cykl filtra; mapowanie rawSlot -> akcja paska.
