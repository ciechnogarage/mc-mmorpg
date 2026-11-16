# Księga postaci — okno E (Paper 1.21.11, Java 21)

Plugin utrzymuje cztery sloty biżuterii w siatce craftingu okna E: naszyjnik, dwa pierścienie, charm. Przedmioty są zapisane w `plugins/KsiegaPostaci/profiles/<uuid-profilu>.yml` i rejestrowane jako własny `InventorySupplier` MMOItems. Gdy MMOProfiles jest aktywne, UUID pochodzi z wybranego profilu; bez niego z UUID gracza. Zmiana slotu zapisuje plik od razu. Wymagania klasy i poziomu sprawdza `EquippedItem.isUsable`.

Wynik craftingu jest przyciskiem Sakwy (`MISCELLANEOUS/UI_SAKWA`); klik wykonuje z konsoli `core-playervault sakwa <gracz>` po jednym ticku. Przycisk książki receptur resetuje stan receptur i uruchamia u gracza `/player`.

## Ukrywanie ekwipunku w menu Księgi

`ukryj-ekwipunek.wlaczone: true` w `config.yml` włącza maskowanie dolnych 36 slotów
gracza w oknie DeluxeMenus. Plugin rozpoznaje okno po dowolnym znaku z listy
`ukryj-ekwipunek.znaki-tytulu` lub kluczy `podstrony.strony` w tytule widoku
(domyślnie `䅠`, `䅨` oraz glify podstron). Tytuł menu
DeluxeMenus musi zawierać jeden z tych glifów. Dla tego okna ProtocolLib zamienia wyłącznie
wychodzące `WINDOW_ITEMS` i `SET_SLOT` na puste sloty gracza (lub przyciski panelu Q); nie zmienia
ekwipunku serwera ani kursora. Kliknięcia dolnej części, przeciąganie,
shift-klik, cyfry, F i Q są anulowane, żeby niewidocznych przedmiotów nie
przenosić.

**Q włącza i wyłącza menu.** Klient wysyła Q w otwartym oknie (klik `DROP`/`CONTROL_DROP`)
tylko nad slotem z przedmiotem, dlatego maska wkłada do pustych slotów (góra i dolny panel)
niewidoczny wypełniacz: `PAPER` z `item_model` `ksiega:klon/pusty` (model `minecraft:empty`
w packu Nexo) i ukrytym dymkiem. Istnieje tylko w pakietach. Q/Ctrl+Q nad dowolnym slotem
okna Księgi zamyka je w następnym ticku. Klik w wypełniacz (klient bierze go na kursor,
serwer nic nie odsyła) kończy się `updateInventory()` w następnym ticku. Ograniczenie
klienta: Q w przerwie między siatką a panelem i poza oknem nie dociera do serwera.
Sprawdzone 16.11.2026 klientem QA na klonie (20/20: 10 cykli, 9 miejsc okna, Ctrl+Q,
podstrony, kliknięcia LPM/PPM/shift przed Q, ekwipunek bez zmian).

Po zamknięciu okna stan maskowania znika; tick później
`updateInventory()` przywraca klientowi prawdziwą zawartość. Wyjście gracza
również usuwa stan. Wymagany jest `ProtocolLib.jar` na backendzie `items`.

## Menu Q Księga

`wdrozenie/menu-ksiega/hub_glowne.yml` zastępuje
`plugins/DeluxeMenus/gui_menus/hub_glowne.yml` na backendzie `items`; wpis
`hub_glowne` w `plugins/DeluxeMenus/config.yml` już istnieje. Menu ma 54 sloty i
12 kafli 2×2 według `wdrozenie/menu-ksiega/k12/sloty.txt`.
Po wymianie JAR dopisz sekcję `panel-dolny` i glif `䅨` z
`src/main/resources/config.yml` do istniejącego
`plugins/KsiegaPostaci/config.yml`: `saveDefaultConfig()` nie nadpisze starej konfiguracji.
Tytuł jest dokładnym ciągiem z `test_ksiega_b.yml`: `kb_1..4` (`䅨`–`䅫`) z
przesunięciami Nexo. Tekstura `wdrozenie/menu-ksiega/k12/ksiega_2x.png` wymaga
podziału/wgrania do paczki jako `nexo:gui/kb_1..4` i definicji glifów z
`plugins/Nexo/glyphs/ksiega_koncepty.yml`; niewidoczny item
`nexo-mmo_icon_blank` musi być dostępny w Nexo. Skróty Q/MenuKey i komendy
`menu`, `menuglowne` pozostają wejściami do `hub_glowne`.

`panel-dolny` w `config.yml` działa dla 54-slotowego okna z glifem `䅨` lub glifem podstrony.
Sloty panelu 0–8 to pierwszy widoczny rząd dolnego ekwipunku (raw 54–62),
9–17 to drugi, 18–26 trzeci, 27–35 hotbar (raw 81–89). Wiersz A ma Pomoc,
Ustawienia, Zmień postać i Zamknij; wiersz B ma siedem ikon stanu. Przedmioty
są tworzone na głównym wątku przy otwarciu i odświeżane co 20 ticków;
ProtocolLib podmienia tylko pakiety. Lewy/prawy klik w aktywny przycisk jest
anulowany i uruchamia komendę w następnym ticku. Shift, cyfry, F, Q, przeciąganie,
podwójny klik i kursor z przedmiotem nie przenoszą wyposażenia.

## Podstrony Księgi

Każda podstrona DeluxeMenus ma 54 sloty i cztery własne glify tła w tytule.
Dodaj pierwszy glif jako klucz w `podstrony.strony` w `config.yml`, a jako wartość
podaj nazwę menu powrotu, np. `"䨀": hub_glowne`. Pozostałe trzy glify nie wymagają
wpisów. Wszystkie glify podstron są automatycznie dodawane do maskowania ekwipunku;
nie dopisuj ich do `ukryj-ekwipunek.znaki-tytulu`. Gdy tytuł zawiera kilka
skonfigurowanych glifów panelu, decyduje pierwszy w tytule. Sloty 3–5 na
podstronie używają ikon panelu Q, lecz pokazują `podstrony.wstecz-nazwa` i
`podstrony.wstecz-lore` oraz otwierają wskazane menu. Pozostałe przyciski
działają jak w Q. Na istniejącym serwerze dopisz sekcję `podstrony` ręcznie,
ponieważ `saveDefaultConfig()` nie uzupełnia istniejącego pliku.

Tła i menu podstron generuje `wdrozenie/menu-ksiega/strony/rysuj_strony.py`
(uruchamiaj zwykłym `python3`, bez `-I` — Pillow jest pakietem użytkownika; obok
`menu-ksiega` musi leżeć `mcfont`). Wynik: `strony/out/<strona>/<strona>_1..4.png`
(ćwiartki 176×222, 3–4 jako RGBA) i `strony/out/dm/<strona>.yml`; sloty kafli w
`out/<strona>/sloty.txt`. Podpis kafla mieści się w 32 px (`narrow_width`, bez kropek).

Q → Postać ma piąty kafel **Ścieżka** (sloty 41, 42, 50, 51) w trzech stanach
`view_requirement`/`priority`: przed 10. poziomem opis „Na 10. poziomie wybierzesz…”
(klik nic nie robi); poziom ≥10 i punkt klasy MMOCore → klik `[player] class`
(okno „Wybierz specjalizację”, gracz potrzebuje `mmocore.class-select`); po wyborze
„Twoja ścieżka: <klasa>”. Test botem (droga gracza bez OP, Q → Postać → kafel):
`wdrozenie/menu-ksiega/testy/specjalizacja-kafel.js akolita|ninja` przez
`/opt/docker/mc/narzedzia/bot/run.sh <klon --z-kreatorem>`; na klonie najpierw
`lp group default permission set mmocore.class-select true`.
Test sprawdza też, że okno potwierdzenia MMOCore (`MMOCore/gui/class-confirm/`) jest
po polsku. Pozostałe okna MMOCore otwierane komendą gracza (`/class`, `/waypoints`,
`/skilltrees <id>`) sprawdza `testy/okna-mmocore-pl.js` (na klonie dodatkowo
`mmocore.waypoints` i `mmocore.skilltrees` dla grupy `default`).

Do kompilacji potrzeba także `PlaceholderAPI-2.12.2.jar` i `nexo-1.20.jar` z
`/opt/docker/mc/data/world1/plugins/` (zależności `compileOnly`); runtime ma
`softdepend` i działa bez nich na materiałach zapasowych. Liczniki bez PAPI
traktowane są jako zero. W JAR MMOCore 1.13.1 są `%mmocore_experience%` i
`%mmocore_next_level%`, a `%mmocore_quest%` bez zadania zwraca pusty tekst;
`%mmocore_quest_progress%` zwraca wtedy `0`. Warto potwierdzić to w grze po
załadowaniu PlaceholderAPI oraz sprawdzić rzeczywisty wygląd i klikalność
wszystkich kafli/panelu, odświeżenie punktów, brak zmian w prawdziwym ekwipunku
oraz powrót hotbara po zamknięciu. Walidacja odbywa się na backendzie `items`
przez Velocity (`localhost:25565`); przejrzyj `./mc logs items`.

## Układ slotów gracza w oknie E

| Slot | Rola |
| --- | --- |
| Offhand (przy postaci, 77,62) | Lewa aktywna, prawdziwa broń |
| Hotbar 0 | Prawa aktywna, prawdziwa broń używana do ataku |
| Plecak 13 (80,84) | Obraz prawej aktywnej, PDC `ksiega:obraz` |
| Hotbar 1 | Obraz lewej aktywnej, PDC `ksiega:klon` |
| Hotbar 2 | Mikstura życia: `CONSUMABLE` z `RESTORE_HEALTH > 0` |
| Hotbar 3 | Mikstura many: `CONSUMABLE` z `RESTORE_MANA > 0` |
| Hotbar 4 | Przedmiot użytkowy: dowolny `CONSUMABLE` |
| Hotbar 5–7 | Wolne sloty |
| Hotbar 8 | Dziennik |
| 11, 12 | Prawa i lewa zapasowa, prawdziwe bronie |
| 9, 10 | Rękawice i pas MMOInventory |
| 14–17, 18–35 | Plecak |

Obrazy i zaślepki nie mają danych MMOItems i nigdy nie są przedmiotami wyposażenia. Nie można ich użyć, wyrzucić ani przenieść; znikają z listy przedmiotów po śmierci. Klik obrazu zamienia kursor z prawdziwym przedmiotem odpowiedniej ręki. Lewy klik w zapasie wkłada lub wyjmuje broń, a prawy klik **pustym kursorem** na 11 zmienia tylko prawą rękę, na 12 tylko lewą. Prawy klik z przedmiotem na kursorze dalej wkłada broń z walidacją. Shift+F w E zamienia obie ręce i oba zapasy. Po pojedynczej zmianie pasek akcji pokazuje nową broń. Shift-klik z plecaka zakłada broń kolejno w 0, offhand, 11, 12; z ręki lub zapasu przenosi ją do 14–35.

Broń dwuręczna w prawej aktywnej lub zapasowej zajmuje odpowiadającą jej lewą rękę: offhand lub slot 12 musi być pusty, a obraz pokazuje „zajęta”. Próba włożenia tam przedmiotu jest odrzucana. Przy osobnej zmianie prawej na dwuręczną dotychczasowa lewa aktywna trafia do pustego zapasu L; zajęty zapas blokuje zmianę. Przy zwykłym założeniu dwuręcznej do 0 lewa broń trafia najpierw do zapasu L, potem do plecaka 14–35; przy braku miejsca operacja jest odrzucana. Zmiana prawej z dwuręcznej na jednoręczną pozostawia lewą aktywną pustą. Jeżeli zapas L był zajęty, jego przedmiot jest przenoszony do plecaka albo upuszczany pod nogi, aby zapas z dwuręczną pozostał spójny. Każda zmiana zestawu sprawdza obie pary przed zapisem.

Sloty 2–4 przyjmują typy i statystyki z `typy-mikstur-i-uzytkowych` oraz `staty-mikstur` w `config.yml`. Shift-klik z plecaka umieszcza pasujący stos najpierw w pustym 2, 3 lub 4. Można też użyć zwykłego kliknięcia lub cyfry; niepasujący przedmiot jest odrzucany. Klawisz cyfry 3/4/5 używa przedmiotu od razu bez zmiany slotu (sekcja „Szybkie mikstury”); wybrany kółkiem działa normalnie po PPM. Po zużyciu ostatniej sztuki zaślepka wraca w następnym ticku. Przy odświeżeniu ekwipunku niepasujące rzeczy z 2–4 trafiają do plecaka 14–35 albo pod nogi; pasujące zostają. Stary prawdziwy przedmiot ze slotu 13 podlega tej samej migracji.

## Pliki wdrożeniowe

- `wdrozenie/vanilla_inventory.yml` → `plugins/MMOInventory/inventory/vanilla_inventory.yml`: tylko GLOVES 9, BELT 10.
- `wdrozenie/stale-sloty.yml` → `plugins/CoreTools/Scripts/stale-sloty.yml`: dziennik nadal w 8, Sakwa usunięta z 18.
- `wdrozenie/assets/minecraft/lang/pl_pl.json` i `en_us.json` → odpowiednie pliki językowe paczki Nexo: dodać klucz `container.crafting` do istniejących JSON, zachowując pozostałe tłumaczenia.

## Kolejność wdrożenia

1. Zrób kopię konfiguracji, profili i ekwipunków staging. Zatrzymaj backend `items`.
2. Wgraj nową konfigurację MMOInventory i skrypt CoreTools, dodaj tłumaczenia do paczki. Nie nadpisuj całych plików lang, jeśli mają inne klucze.
3. Nadzorca buduje `./build.sh` i instaluje JAR. Uruchamia tylko backend `items` za Velocity; sprawdza obecność ProtocolLib.
4. Na testowym profilu sprawdź migrację 9–14 oraz 2–4, obrazy w 1 i 13, klik/shift-klik/cyfry/drag/Q/double-click, PPM konsumpcji z 2–4, zapisy po relogu/przełączeniu profilu, staty MMOItems, F i Shift+F, osobne zmiany P/L, broń dwuręczną, śmierć i Sakwe. Otwórz menu z glifem `䅠`: 36 dolnych slotów ma wyglądać na puste, po zamknięciu zawartość i hotbar mają wrócić, a przedmioty po relogu pozostać nietknięte. Przejrzyj `./mc logs items` oraz `MCMMORPG/logs/latest.log`.
5. Po potwierdzeniu działania można usunąć stary EqMigracja. Nie uruchamiaj go równocześnie z tą migracją.

## Cofnięcie

Zatrzymaj backend, usuń JAR Księgi, przywróć kopie konfiguracji, paczki oraz danych graczy/profili z punktu 1. Samo usunięcie JAR nie przywraca biżuterii z wirtualnych slotów do starego ekwipunku. Nie usuwaj `plugins/KsiegaPostaci/profiles/` przed odzyskaniem przedmiotów.

## Ograniczenia do sprawdzenia na staging

Nadzorca powinien potwierdzić kompilację sygnatur Paper i MMOItems oraz zachowanie NBT obrazu i statów `TWO_HANDED`, `RESTORE_HEALTH`, `RESTORE_MANA`. Test awaryjnego zatrzymania w trakcie migracji wymaga osobnej kopii profilu. Ikony zapasu, pustych i zajętych rąk oraz slotów 2–4 tymczasowo używają `nexo:mmo_icon_gloves`. Docelowo potrzebne są: `nexo:mmo_icon_right_hand`, `nexo:mmo_icon_left_hand`, `nexo:mmo_icon_spare_right`, `nexo:mmo_icon_spare_left`, `nexo:mmo_icon_left_occupied`, `nexo:mmo_icon_spare_left_occupied`, `nexo:mmo_icon_health_potion`, `nexo:mmo_icon_mana_potion`, `nexo:mmo_icon_utility`.

## Wymagana zmiana MMOCore (zmierzone klientem 2026-10-01)
`plugins/MMOCore/config.yml` -> `skill-casting.ignore-sneak: true`. Przy `false` Shift+F oprócz zamiany broni
włącza też tryb SKILL CASTING. Z `true`: Shift+F = zamiana zestawu broni, samo F = pasek umiejętności (bez zmian).

## Zaklęcie w ręce i zestaw startowy (`ksiegareka`, 16.11)
Komenda konsoli (`ksiegapostaci.portal.admin`), umowa z pluginem Zaklecia bez zależności między pluginami (jak `mapa zamknij`):
- `ksiegareka <gracz> odloz <prawa|lewa>` — `Zaklecia/Nosnik.przypisz` przed włożeniem nośnika. Broń z tej ręki idzie
  do pustego zapasu tej ręki (11/12, z regułą dwuręczności; flaga PDC `ksiega:pod_zakleciem_*`), inaczej do plecaka 14–35.
  Brak miejsca → broń zostaje, Zaklecia pokazuje „Zwolnij miejsce…”. Prawe zaklęcie zawsze w slocie 0, lewe w offhandzie;
  obrazy 1/13 pokazują zaklęcie (wcześniej `addItem` przenosił broń na pasek → dwie te same bronie, slot 1 dalej z tarczą).
- `ksiegareka <gracz> przywroc <prawa|lewa>` — po wyborze „Pusta” broń odłożona do zapasu wraca do ręki.
- Shift+F z zaklęciem w ręce: zaklęcia zdjęte, potem zwykła zamiana z zapasem (gdy oba zapasy puste — nic).
- `ksiegareka <gracz> uzbroj` — koniec zestawu startowego (`tools/klasy-rasowe/generuj_klasy.py` → CoreTools
  `klasy-rasowe.yml`). `mi give` kładzie w pierwszy wolny slot; u świeżej postaci ikon Księgi jeszcze nie ma, więc
  tarcza trafiała do 1, druga broń do 2, a odświeżenie wyrzucało je do plecaka (lewa pusta). Uruchamia się 5 ticków
  później; przegląda też sloty 1–4 (pomija obrazy i ikony). Pusta prawa dostaje broń dozwoloną w prawej, pusta lewa
  najpierw tarczę/broń tylko-lewą (`typy-zawsze-jednoreczne`), potem drugą jednoręczną; dwuręczna i nie-broń zostają.
  W logu linia `uzbroj <gracz>: lewa<-N [prawa=… lewa=… swiat=…]` (co przełożono i stan rąk przed).
Sprawdzono 16.11 na klonie `pasek-eq` klientem QA (konto bez OP): LPM/PPM z buławą+tarczą, przy wybranym slocie 1,
„Pusta”, pełny zapas i plecak, Claymore (dwuręczna), nowa postać wojownik i akolita.
Znane: przy dwuręcznej w prawej lewe zaklęcie może zostać w offhandzie (tak jak przed zmianą) — czeka na decyzję.

## Szybkie mikstury: klawisz 3/4/5 (decyzja właściciela 16.11)
Klawisz cyfry slotu 2–4 z prawdziwym przedmiotem MMOItems używa go od razu, a wybrany slot (broń w ręce) się nie zmienia.
Robi to samo co PPM w MMOItems 6.10.1 (`listener/ItemUse`): wymagania, cooldown (`item-cooldown`, komunikat
„Przedmiot odnawia się jeszcze…”), `Consumable.useOnPlayer`, −1 sztuka, komendy przedmiotu.
- Pusty slot (ikona) → komunikat `brak-mikstury` na pasku akcji, wybór slotu się nie zmienia.
- Przedmiot wielorazowy (`max-consume`, np. RECALL_STAR) i przedmiot spoza MMOItems → zwykły wybór slotu, potem PPM
  (MMOItems zapisałby zużycie do slotu ręki, czyli na broń).
- Kółko myszy: Paper nie odróżnia kółka od cyfry (`PlayerItemHeldEvent`), więc krok ±1 slot traktowany jest jak kółko
  i daje zwykły wybór. Skutek: cyfra sąsiedniego slotu (np. 3 przy wybranym slocie 1, 4 przy wybranym 2) wybiera slot zamiast pić.
- Wyłącznik: `szybkie-mikstury: false` w `config.yml`.
Sprawdzono 16.11 na klonie `pasek-eq` klientem QA (konto bez OP, wojownik `QA_tarcza3`): mikstury przełożone Shift+klikiem
z plecaka do 2/3, klawisz 3 przy mieczu w ręce: HP 12,8 → 23,9, stos 3 → 2, wybrany slot 0; drugi raz w 1 s: „odnawia się
jeszcze 9,1 s”, stos bez zmian; klawisz 4 (mana): stos 2 → 1, mana wzrosła; klawisz 5 na pustym: komunikat; kółko 0→3→0:
nic nie wypite; RECALL_STAR w 4: zwykły wybór. Tick śr. 1,8 ms, P95 4,3 ms; 0 wyjątków, 0 WARN Księgi.

## Klawisz F (decyzja właściciela 15.11: każdy klawisz robi jedną rzecz)
Samo F = wyłącznie tryb skilli MMOCore: Księga anuluje `PlayerSwapHandItemsEvent` i nie rusza rąk (wcześniej
F przekładało broń z prawej do lewej przy zaklęciu w lewej i zamieniało ręce, a MMOCore w tym samym naciśnięciu
włączał tryb skilli). Przekładanie broni: Shift+F (komplet zapasowy) albo okno E. `Bron.swap`.
Tekstura okna E: `wdrozenie/inventory.png` (generator `wdrozenie/ksiega_tekstura.py`) -> pack Nexo
`assets/minecraft/textures/gui/container/inventory.png`.
