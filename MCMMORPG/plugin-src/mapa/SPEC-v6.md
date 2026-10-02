# Plugin Mapa v6 — etap 2: WŁASNE ZNACZNIKI GRACZA + PROWADZENIE (dodatek; poprzednie SPEC obowiązują)

## 1. Postać = właściciel znaczników
- Klucz postaci: MMOProfiles (softdepend MMOProfiles, `compileOnly(files(".../MMOProfiles-1.2-20260325.193503-28.jar"))`
  — przypnij jar z `/opt/docker/mc/data/world1/plugins` jak w plugin-src/wiary/build.gradle.kts, przez gradleProperty
  `mapaServerPlugins`, domyślnie `/serverplugins`; build.sh montuje `/opt/docker/mc/data/world1/plugins:/serverplugins:ro`):
  `Bukkit.getServicesManager().load(fr.phoenixdevt.profiles.ProfileProvider.class)
     .getPlayerData(uuid).getCurrent().getUniqueId()`; gdy brak MMOProfiles / null -> UUID gracza.
  Klasy MMOProfiles tylko w osobnej klasie ładowanej, gdy plugin włączony. NIE rejestruj ProfileDataModule.
- Plik: `plugins/Mapa/gracze/<klucz>.yml`, lista `znaczniki:` {id (8 znaków), swiat, x, z, nazwa, ikona}.
  Wczytanie leniwe przy pierwszym użyciu, zapis asynchroniczny po każdej zmianie (kopia listy z wątku głównego).
  Limit `wlasne.limit` (domyślnie 30) — przy przekroczeniu komunikat na action bar.
- Ikony do wyboru (config `wlasne.ikony`, kolejność = kolejność w dialogu):
  `pin_czerwona: "Czerwona pinezka"`, `pin_niebieska`, `pin_zielona`, `pin_zolta`, `pin_fioletowa`, `pin_biala`,
  `nether_star: "Gwiazda"`, `gold_ingot: "Skarb"`, `oak_door: "Dom"`, `iron_sword: "Walka"`. Pliki PNG dostarcza generator
  (ten sam katalog `ikony/` i `NMinimap/markers/`).

## 2. Własne znaczniki na wszystkich mapach (tylko dla właściciela)
`MapaData.Marker` + nowe pole `typ: "wlasny"`. Funkcja `List<Marker> znacznikiDla(Player, String world)` = stałe
znaczniki świata + własne gracza w tym świecie (także instancje lochów przez `klucz()`). Używaj jej w:
mapie w ręce (kursor BANNER_* wg koloru pinezki: czerwona->RED, niebieska->BLUE, zielona->LIME, zolta->YELLOW,
fioletowa->PURPLE, biała/pozostałe->WHITE, caption = nazwa), pełnej mapie (ikona + nazwa), minimapie NMinimap
(LocationMarker z ikoną), atlasie (w tooltipie kafelka linia „Twój znacznik: nazwa” — kafelków nie przerysowujemy).

## 3. Pełna mapa: LPM w obszarze mapy (poza paskami)
- Znajdź znacznik (stały lub własny) najbliżej kursora w promieniu 10 px obrazu.
  - własny -> dialog EDYCJI,
  - stały -> ustaw/zdejmij PROWADZENIE do niego (komunikat „Prowadzę do: X” / „Prowadzenie wyłączone”),
  - brak -> dialog DODAWANIA w punkcie świata pod kursorem (przelicz (u,v) -> x,z wg środka i zoomu).
- Dialogi (Paper Dialog API 1.21.11, `io.papermc.paper.dialog`, `io.papermc.paper.registry.data.dialog`):
  - Dodaj: tytuł „Nowy znacznik”, pole tekstowe `nazwa` (max 24, domyślnie „Mój znacznik”), wybór `ikona`
    (single option z listy ikon), przyciski „Zapisz”, „Anuluj”.
  - Edycja: tytuł „Znacznik: <nazwa>”, te same pola wypełnione, przyciski „Zapisz”, „Prowadź”, „Usuń”, „Anuluj”.
  - **Pułapki (zmierzone u nas):** wbudowane callbacki Papera gubią kliki — używaj akcji `custom` z własnym kluczem
    (`mapa:dodaj`, `mapa:zapisz/<id>`, `mapa:usun/<id>`, `mapa:prowadz/<id>`) i obsłuż `PlayerCustomClickEvent`
    (odczyt pól z `DialogResponseView`); pusty multiAction rzuca wyjątek; dialog nie może pauzować gry
    (`pause(false)` / odpowiednik w buildzie). Po zamknięciu dialogu sesja pełnej mapy trwa dalej; po zmianie
    znaczników `session.dirty = true`.
- Pełna mapa pokazuje cel prowadzenia: czerwony „X” (6×6 px) w miejscu celu.

## 4. Prowadzenie (cel per gracz, w pamięci; czyści się przy wyjściu z gry)
- Cel = świat + x + z + nazwa. Ustawiany z pełnej mapy, z dialogu edycji oraz komendą
  `/mapa cel <gracz> <swiat> <x> <z> <nazwa…>` i `/mapa cel <gracz> wyczysc` (konsola/OP).
- Co 10 ticków dla graczy z celem w TYM SAMYM świecie (lub instancji tego szablonu):
  - action bar: `➤ <nazwa> · <odl> m <strzałka>` (strzałka z ↑↗→↘↓↙←↖ względem kierunku patrzenia gracza).
    Jeśli plugin MMOCore włączony — wyślij przez `net.Indyuce.mmocore.api.player.PlayerData.get(player)
    .displayActionBar(String)` (MMOCore nadpisuje zwykły action bar co 0,25 s), inaczej `sendActionBar`.
    Klasy MMOCore w osobnej klasie, softdepend; jar przypnij jak MMOProfiles (`MMOCore-1.13.1-20260330.110342-58.jar`,
    plus `MythicLib-dist-1.7.1-20260330.110510-95.jar`).
  - pasek lokalizatora: packetevents (softdepend; jar `packetevents-spigot-2.11.2.jar` compileOnly):
    `WrapperPlayServerWaypoint` operacja TRACK/UPDATE z `TrackedWaypoint` (stałe UUID na gracza, ikona domyślna,
    kolor czerwony, pozycja Vec3i celu); przy zmianie celu/wyczyszczeniu UNTRACK.
  - ≤ 8 bloków od celu: komunikat „Jesteś na miejscu: <nazwa>”, cel wyczyszczony.
- Mapa w ręce: kursor RED_X w miejscu celu (jeśli w zasięgu). Minimapa: LocationMarker ikona `cel`.

## Testy JUnit
(u,v)->(x,z) dla pełnej mapy; najbliższy znacznik w promieniu; strzałka kierunku (8 przypadków); zapis/odczyt
pliku znaczników (YamlConfiguration w pamięci); limit.
