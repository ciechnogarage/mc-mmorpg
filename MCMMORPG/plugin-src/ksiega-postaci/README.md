# Księga postaci — okno E (Paper 1.21.11, Java 21)

Plugin utrzymuje cztery sloty biżuterii w siatce craftingu okna E: naszyjnik, dwa pierścienie, charm. Przedmioty są zapisane w `plugins/KsiegaPostaci/profiles/<uuid-profilu>.yml` i rejestrowane jako własny `InventorySupplier` MMOItems. Gdy MMOProfiles jest aktywne, UUID pochodzi z wybranego profilu; bez niego z UUID gracza. Zmiana slotu zapisuje plik od razu. Wymagania klasy i poziomu sprawdza `EquippedItem.isUsable`.

Wynik craftingu jest przyciskiem Sakwy (`MISCELLANEOUS/UI_SAKWA`); klik wykonuje z konsoli `core-playervault sakwa <gracz>` po jednym ticku. Przycisk książki receptur resetuje stan receptur i uruchamia u gracza `/player`.

## Układ slotów gracza w oknie E

| Slot | Rola |
| --- | --- |
| Offhand (przy postaci, 77,62) | Lewa aktywna, prawdziwa broń |
| Hotbar 0 | Prawa aktywna, prawdziwa broń używana do ataku |
| Plecak 13 (80,84) | Obraz prawej aktywnej, PDC `ksiega:obraz` |
| Hotbar 1 | Obraz lewej aktywnej, PDC `ksiega:klon` |
| 11, 12 | Prawa i lewa zapasowa, prawdziwe bronie |
| 9, 10 | Rękawice i pas MMOInventory |
| 14–17, 18–35 | Plecak; hotbar 8: Dziennik |

Obrazy nie mają danych MMOItems i nigdy nie są przedmiotami wyposażenia. Klik obrazu zamienia kursor z prawdziwym przedmiotem odpowiedniej ręki; shift-klik z plecaka zakłada broń kolejno w 0, offhand, 11, 12. Shift-klik z ręki lub zapasu przenosi broń do plecaka 14–35. Shift+F w E zamienia cały zestaw. Obie ręce i zapas przyjmują tylko typy z `typy-broni` w `config.yml`; gdy prawa aktywna broń jest dwuręczna, lewa aktywna musi być pusta. Niedozwolony typ jest odrzucany z komunikatem na pasku akcji. Klawisz cyfry nad obrazem jest blokowany. Stary prawdziwy przedmiot ze slotu 13 jest odkładany do 14–35 albo pod nogi; dawna zaślepka jest usuwana.

## Pliki wdrożeniowe

- `wdrozenie/vanilla_inventory.yml` → `plugins/MMOInventory/inventory/vanilla_inventory.yml`: tylko GLOVES 9, BELT 10.
- `wdrozenie/stale-sloty.yml` → `plugins/CoreTools/Scripts/stale-sloty.yml`: dziennik nadal w 8, Sakwa usunięta z 18.
- `wdrozenie/assets/minecraft/lang/pl_pl.json` i `en_us.json` → odpowiednie pliki językowe paczki Nexo: dodać klucz `container.crafting` do istniejących JSON, zachowując pozostałe tłumaczenia.

## Kolejność wdrożenia

1. Zrób kopię konfiguracji, profili i ekwipunków staging. Zatrzymaj backend `items`.
2. Wgraj nową konfigurację MMOInventory i skrypt CoreTools, dodaj tłumaczenia do paczki. Nie nadpisuj całych plików lang, jeśli mają inne klucze.
3. Nadzorca buduje `./build.sh` i instaluje JAR. Uruchamia tylko backend `items` za Velocity.
4. Na testowym profilu sprawdź migrację 9–14, obrazy w 1 i 13, klik/shift-klik/cyfry/drag/Q/double-click, zapisy po relogu/przełączeniu profilu, staty MMOItems, F i Shift+F, broń dwuręczną, śmierć i Sakwe. Przejrzyj `./mc logs items` oraz `MCMMORPG/logs/latest.log`.
5. Po potwierdzeniu działania można usunąć stary EqMigracja. Nie uruchamiaj go równocześnie z tą migracją.

## Cofnięcie

Zatrzymaj backend, usuń JAR Księgi, przywróć kopie konfiguracji, paczki oraz danych graczy/profili z punktu 1. Samo usunięcie JAR nie przywraca biżuterii z wirtualnych slotów do starego ekwipunku. Nie usuwaj `plugins/KsiegaPostaci/profiles/` przed odzyskaniem przedmiotów.

## Ograniczenia do sprawdzenia na staging

Nadzorca powinien potwierdzić kompilację sygnatur Paper i MMOItems oraz zachowanie NBT obrazu, zwłaszcza flagę `TWO_HANDED`. Test awaryjnego zatrzymania w trakcie migracji wymaga osobnej kopii profilu. Ikony zapasu i pustych rąk tymczasowo używają `nexo:mmo_icon_gloves`; przygotować własne tekstury.
