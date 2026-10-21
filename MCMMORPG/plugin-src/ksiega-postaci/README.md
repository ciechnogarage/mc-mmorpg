# Księga postaci — okno E (Paper 1.21.11, Java 21)

Plugin utrzymuje cztery sloty biżuterii w siatce craftingu okna E: naszyjnik, dwa pierścienie, charm. Przedmioty są zapisane w `plugins/KsiegaPostaci/profiles/<uuid-profilu>.yml` i rejestrowane jako własny `InventorySupplier` MMOItems. Gdy MMOProfiles jest aktywne, UUID pochodzi z wybranego profilu; bez niego z UUID gracza. Zmiana slotu zapisuje plik od razu. Wymagania klasy i poziomu sprawdza `EquippedItem.isUsable`.

Wynik craftingu jest przyciskiem Sakwy (`MISCELLANEOUS/UI_SAKWA`); klik wykonuje z konsoli `core-playervault sakwa <gracz>` po jednym ticku. Przycisk książki receptur resetuje stan receptur i uruchamia u gracza `/player`.

## Ukrywanie ekwipunku w menu Księgi

`ukryj-ekwipunek.wlaczone: true` w `config.yml` włącza maskowanie dolnych 36 slotów
gracza w oknie DeluxeMenus. Plugin rozpoznaje okno po dowolnym znaku z listy
`ukryj-ekwipunek.znaki-tytulu` w tytule widoku (domyślnie `䅠`, `䅨`). Tytuł menu
DeluxeMenus musi zawierać jeden z tych glifów. Dla tego okna ProtocolLib zamienia wyłącznie
wychodzące `WINDOW_ITEMS` i `SET_SLOT` na puste sloty gracza (lub przyciski panelu Q); nie zmienia
ekwipunku serwera ani kursora. Kliknięcia dolnej części, przeciąganie,
shift-klik, cyfry, F i Q są anulowane, żeby niewidocznych przedmiotów nie
przenosić. Po zamknięciu okna stan maskowania znika; tick później
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

`panel-dolny` w `config.yml` działa tylko dla 54-slotowego okna z glifem `䅨`.
Sloty panelu 0–8 to pierwszy widoczny rząd dolnego ekwipunku (raw 54–62),
9–17 to drugi, 18–26 trzeci, 27–35 hotbar (raw 81–89). Wiersz A ma Pomoc,
Ustawienia, Zmień postać i Zamknij; wiersz B ma siedem ikon stanu. Przedmioty
są tworzone na głównym wątku przy otwarciu i odświeżane co 20 ticków;
ProtocolLib podmienia tylko pakiety. Lewy/prawy klik w aktywny przycisk jest
anulowany i uruchamia komendę w następnym ticku. Shift, cyfry, F, Q, przeciąganie,
podwójny klik i kursor z przedmiotem nie przenoszą wyposażenia.

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

Sloty 2–4 przyjmują typy i statystyki z `typy-mikstur-i-uzytkowych` oraz `staty-mikstur` w `config.yml`. Shift-klik z plecaka umieszcza pasujący stos najpierw w pustym 2, 3 lub 4. Można też użyć zwykłego kliknięcia lub cyfry; niepasujący przedmiot jest odrzucany. Prawdziwy przedmiot po wyborze slotu działa normalnie po PPM. Po zużyciu ostatniej sztuki zaślepka wraca w następnym ticku. Przy odświeżeniu ekwipunku niepasujące rzeczy z 2–4 trafiają do plecaka 14–35 albo pod nogi; pasujące zostają. Stary prawdziwy przedmiot ze slotu 13 podlega tej samej migracji.

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
Tekstura okna E: `wdrozenie/inventory.png` (generator `wdrozenie/ksiega_tekstura.py`) -> pack Nexo
`assets/minecraft/textures/gui/container/inventory.png`.
