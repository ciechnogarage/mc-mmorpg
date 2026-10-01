# Księga postaci — okno E (Paper 1.21.11, Java 21)

Plugin utrzymuje cztery sloty biżuterii w siatce craftingu okna E: naszyjnik, dwa pierścienie, charm. Przedmioty są zapisane w `plugins/KsiegaPostaci/profiles/<uuid-profilu>.yml` i rejestrowane jako własny `InventorySupplier` MMOItems. Gdy MMOProfiles jest aktywne, UUID pochodzi z wybranego profilu; bez niego z UUID gracza. Zmiana slotu zapisuje plik od razu. Wymagania klasy i poziomu sprawdza `EquippedItem.isUsable`.

Wynik craftingu jest przyciskiem Sakwy (`MISCELLANEOUS/UI_SAKWA`); klik wykonuje z konsoli `core-playervault sakwa <gracz>` po jednym ticku. Przycisk książki receptur resetuje stan receptur i uruchamia u gracza `/player`. Hotbar 0 i offhand są aktywnymi rękami. Hotbar 1 jest obrazem offhandu, bez tagów MMOItems, oznaczonym PDC `ksiega:klon`. Sloty 11 i 12 są zapasem; Shift+F w E zamienia cały zestaw. Lista akceptowanych typów broni jest w `config.yml`.

## Pliki wdrożeniowe

- `wdrozenie/vanilla_inventory.yml` → `plugins/MMOInventory/inventory/vanilla_inventory.yml`: tylko GLOVES 9, BELT 10.
- `wdrozenie/stale-sloty.yml` → `plugins/CoreTools/Scripts/stale-sloty.yml`: dziennik nadal w 8, Sakwa usunięta z 18.
- `wdrozenie/assets/minecraft/lang/pl_pl.json` i `en_us.json` → odpowiednie pliki językowe paczki Nexo: dodać klucz `container.crafting` do istniejących JSON, zachowując pozostałe tłumaczenia.

## Kolejność wdrożenia

1. Zrób kopię konfiguracji, profili i ekwipunków staging. Zatrzymaj backend `items`.
2. Wgraj nową konfigurację MMOInventory i skrypt CoreTools, dodaj tłumaczenia do paczki. Nie nadpisuj całych plików lang, jeśli mają inne klucze.
3. Nadzorca buduje `./build.sh` i instaluje JAR. Uruchamia tylko backend `items` za Velocity.
4. Na testowym profilu sprawdź migrację 9–14, ikony 0–4, zapis po relogu/przełączeniu profilu, staty MMOItems, F i Shift+F, śmierć i Sakwe. Przejrzyj `./mc logs items` oraz `MCMMORPG/logs/latest.log`.
5. Po potwierdzeniu działania można usunąć stary EqMigracja. Nie uruchamiaj go równocześnie z tą migracją.

## Cofnięcie

Zatrzymaj backend, usuń JAR Księgi, przywróć kopie konfiguracji, paczki oraz danych graczy/profili z punktu 1. Samo usunięcie JAR nie przywraca biżuterii z wirtualnych slotów do starego ekwipunku. Nie usuwaj `plugins/KsiegaPostaci/profiles/` przed odzyskaniem przedmiotów.

## Ograniczenia do sprawdzenia na staging

Gradle i Docker nie były uruchamiane w sandboxie. Nadzorca powinien potwierdzić kompilację sygnatur Paper i MMOItems oraz zachowanie NBT klonu, zwłaszcza flagę `TWO_HANDED`. Test awaryjnego zatrzymania w trakcie migracji wymaga osobnej kopii profilu. Ikony zapasu i pustej lewej ręki tymczasowo używają `nexo:mmo_icon_gloves`; przygotować własne tekstury.
