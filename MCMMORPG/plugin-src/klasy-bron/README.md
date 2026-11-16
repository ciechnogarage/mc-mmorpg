# KlasyBron (`plugin-src/klasy-bron`)
Wersja w źródle: `0.1.1-${rev}` (16.11: + targeter `@Celownik`; opis poniżej, sekcja „Celownik”; wcześniejszy stan dotyczy 0.1.0) (`gradle.properties:1`, `build.gradle.kts:27`, plugin.yml:2 = `${version}`). Jar na world1: `/opt/docker/mc/data/world1/plugins/klasy-bron-0.1.0-202610020918.jar`. Zgodność źródła z jarem: zgodna co do plugin.yml (name, main, depend, description identyczne; `unzip -p` jara); bajtkod sprawdzony 25.10: czysty build ze źródła w gałęzi vs jar na world1 — ten sam zestaw plików, `KlasyBron.class` bajt w bajt identyczny (5019 B), różni się tylko linia `version:` w plugin.yml. Źródło w gałęzi = to, co wdrożone. README vs kod: numery linii poprawione (były przesunięte), reszta twierdzeń (komendy brak, config brak, 52 wystąpienia `bronRasowa` w `MythicLib/skill/`) potwierdzona.
Opis stanu z dnia: 2026-10-25, commit 8c79ff8 (worktree docs-pluginy).

## Po co (co widzi gracz)
Gracz nie widzi pluginu bezpośrednio. Daje on jeden warunek MythicMobs `bronRasowa{typ=..}`, którym sygnatury klas rasowych (MythicLib skille: akolita, mag, ninja, wojownik, specjalizacje) włączają premię tylko wtedy, gdy gracz trzyma w ręce broń MMOItems danego typu (`src/main/java/pl/mcmmorpg/klasybron/KlasyBron.java:31-39`). Zastępuje placeholder Crucible `<caster.item.hand.nbt.MMOITEMS_ITEM_TYPE>`, który po restarcie jest "Invalid Placeholder" do czasu `mm reload` (KlasyBron.java:21-22).

## Celownik (`@Celownik{r=20;s=0.3}`, od 0.1.1)
Targeter MythicMobs: istota pod celownikiem gracza (`Celownik.java`). `World.rayTrace` z oczu, zasięg `r`/`range` (domyślnie 20, zakres 1–64), hitbox celu powiększony o `s`/`size` (domyślnie 0,3, zakres 0–1), bloki zasłaniają, płyny nie. Pomija: rzucającego, martwe, ArmorStand, gracza w trybie widza, sojuszników (`Warunki.sojusznik`: gracz w tej samej drużynie MMOCore, mob MM frakcji `chowance`/`akolita_slugi`/`zaklecia_przywolane` należący do gracza lub członka jego drużyny). Caster-mob zwraca swój `getTarget()`. Poza wątkiem głównym (skill async) namierzanie idzie przez `callSyncMethod` z limitem 1 s; po przekroczeniu brak celu + WARN.
Po co: `@Target` w MM 5.11.2 (`MythicUtil.getTargetedEntity`) bierze pierwszą istotę z `getNearbyEntities` w pudle wokół każdego bloku linii wzroku, więc sługa stojący obok gracza przechwytywał Rozkaz akolity (FAIL `narzedzia/bot/scenariusze/klasy/skille.js` rozkaz, 15.11). Używa go `AKO_Rozkaz` w `tools/klasy-rasowe/akolita/MythicLib/skill/akolita.yml`.
Zależność miękka: MMOCore (drużyny; bez niego sojusznik = tylko własne moby). Kompilacja z przypiętym `MMOCore-1.13.1-20260330.110342-58.jar` (`build.gradle.kts`).
Pełniejsza wersja (warunki `druzyna{}`, `celWrogi{}`, listener osłon) leży na gałęzi `klasy-p1p2` (0.2.0), niewdrożona.

## Komendy i uprawnienia
Brak. plugin.yml nie ma sekcji `commands` ani `permissions` (`src/main/resources/plugin.yml:1-6`).

## Konfiguracja
Brak configu: plugin nie ma `saveDefaultConfig` ani pliku w `plugins/KlasyBron/` (na world1 katalogu nie ma; `ls plugins`). Cała konfiguracja siedzi w skillach, które wołają warunek.
Składnia warunku (KlasyBron.java:35-36): parametr `typ` lub skrót `t`, wartości po przecinku, bez rozróżniania wielkości liter (`toUpperCase`), puste wartości pomijane. Przykład z naszych skilli: `setvariable{var=caster.kr_premia;type=INTEGER;value=1;duration=100} @self ?bronRasowa{typ=SHIELD}` (`tools/klasy-rasowe/*/MythicLib/skill/*.yml`).
Użycie na world1 (policzone grepem w `/opt/docker/mc/data/world1/plugins/MythicLib/skill/`): 52 wystąpienia `bronRasowa` (akolita 4, ninja 4, mag 4, wojownik 6, specjalizacje 34).

## Jak działa
- Jedyny listener: `MythicConditionLoadEvent` (domyślny priorytet NORMAL, KlasyBron.java:31-32). Reaguje tylko na nazwę warunku `bronRasowa` (bez rozróżniania wielkości liter, linia 33).
- Rejestruje `IEntityCondition`: prawda, gdy cel jest graczem (`instanceof Player`) i item w głównej LUB drugiej ręce ma NBT `MMOITEMS_ITEM_TYPE` należący do zbioru `typ` (KlasyBron.java:37-38, 41-45). Cel nie-gracz = fałsz; pusty zbiór typów = zawsze fałsz (linia 42).
- Typ odczytywany przez `io.lumine.mythic.lib.api.item.NBTItem` (MythicLib), czyli tag MMOItems, nie nazwa ani model itemu.
- Zależności twarde: MythicMobs, MythicLib (plugin.yml:5); miękka MMOCore (od 0.1.1). Pinowane wersje przy kompilacji: MythicLib-dist-1.7.1-20260330.110510-95, MythicMobs-5.11.2 (`build.gradle.kts:12-16`).

## Koszt dla serwera
Brak zadań okresowych. Warunek liczy się na żądanie przy wykonaniu skilla: jedno odczytanie NBT 1-2 itemów + sprawdzenie zbioru. Zbiór typów jest budowany raz przy wczytaniu skilla (linie 35-36). Nie mierzone.

## Dane
Nie zapisuje nic (ani plik, ani baza, ani PDC). Brak odwołań po UUID.

## Znane pułapki i decyzje
- Powód powstania (Crucible Invalid Placeholder po restarcie): `~/.claude/skills/mc-walka-klasy-zaklecia/SKILL.md:23`, `~/.claude/skills/mc-walka-klasy-zaklecia/historia.md:27`.
- Etap 5 (16 sygnatur klas rasowych używa KlasyBron): `~/.claude/skills/mc-walka-klasy-zaklecia/stan.md:12`.
- Warunek nie sprawdza, czy item to MMOItems poza tagiem NBT; item bez tagu = fałsz. Testów jednostkowych brak (w katalogu tylko `src/main`).

## Budowanie i wdrożenie
`./build.sh` w katalogu pluginu: kontener `gradle:8.12-jdk21`, `--memory 2g`, jary world1 montowane tylko do odczytu pod `/serverplugins` (`build.sh`, `gradle.properties:2`); nazwa wersji = `<kbVersion>-<RRRRMMDDGGMM>`. Wynik: `build/libs/klasy-bron-<wersja>.jar` do `plugins/` world1 (kopia + `cofnij.sh` wg zasad hosta). Build wymaga obecności obu pinowanych jarów (`build.gradle.kts:16-17`).
