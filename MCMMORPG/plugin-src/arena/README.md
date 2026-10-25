# Arena (`plugin-src/arena`)
Wersja w źródle: `0.1.0-<kbRev>` (`gradle.properties:1` `kbVersion=0.1.0`, `build.gradle.kts:31`; `plugin.yml:2` ma `${version}`). Jar na world1: `/opt/docker/mc/data/world1/plugins/arena-0.1.0-202610212230.jar`.
Zgodność źródła z jarem: **RÓŻNA**. Jar zawiera klasę `StrefyPvp` i klucz `pvp-poza-ringiem` w swoim `config.yml` (sprawdzone `unzip` + `grep -c`), a źródło w tym worktree ich NIE ma. Jar jest zbudowany z gałęzi `walka-poprawki` (commit `a999b41`, `git merge-base --is-ancestor a999b41 HEAD` = nie). `plugin.yml` jaru jest identyczny ze źródłem (poza `version`). Opis niżej = kod z `main` (docs-pluginy); różnice gałęzi oznaczone „[walka-poprawki]”, źródło: `git show walka-poprawki:MCMMORPG/plugin-src/arena/...`.
Opis stanu z dnia: 2026-10-25, commit 8c79ff8.

## Po co (co widzi gracz)
Otwarta Arena to osobny świat `arena` (PvP tylko w ringu, manekiny treningowe z paskiem DPS) i jednocześnie plugin, który pilnuje PvP w całym grach: kto może bić gracza, gdzie. Wejście i wyjście tylko przez menu (komenda konsolowa), pojedynek jest zapraszany klawiszem Q na gracza (dialog z przyciskami: pojedynek, drużyna, znajomi, gildia). Na arenie nie ma kary śmierci (ekwipunek, poziom, EXP zostają), nie ma EXP-a, dropu ani budowania.

## Komendy i uprawnienia
| komenda | kto | co robi | źródło |
|---|---|---|---|
| `/arena wejdz <gracz>` | konsola/RCON (kod odrzuca resztę, mimo `permission: arena.console`, domyślnie `op`) | teleport na `spawn`, zapis punktu powrotu | `ArenaPlugin.java:264-272`, `plugin.yml:8-11` |
| `/arena wyjdz <gracz>` | j.w. | wyjście do punktu powrotu; odmowa gdy „w walce” (`combat.seconds`) | `ArenaPlugin.java:291-298` |
| `/arena manekiny` | konsola | jednorazowy przebieg `ensureDummies` z raportem każdej stacji (diagnostyka) | `ArenaPlugin.java:267`, `:571` |
| `/menugracza q <gracz>` | konsola/RCON | raytrace z oczu gracza (`menu-gracza.zasieg`); trafiony gracz → dialog, brak celu → komenda `menu-gracza.zwykle` (`dm open hub_glowne`) | `MenuGracza.java:96-108`, `:110-115`, `:144-147` |
Uprawnienia: jedno, `arena.console` (default op) — `plugin.yml:16-18`. Liczba komend w `plugin.yml`: 2 (kontrola parserem poniżej). Wywołuje je DeluxeMenus oraz plugin Zaklecia (klucz `klawisz-q.zwykle: 'menugracza q %gracz%'`, `wdrozenie/pliki/Zaklecia/config.yml.PATCH.md`); `checkQKey` loguje WARN po 5 s od startu, gdy ten klucz zostanie cofnięty (`ArenaPlugin.java:108`, `:227-231`).

## Konfiguracja
Plik: `/opt/docker/mc/data/world1/plugins/Arena/config.yml` (klucze najwyższego poziomu: 16). Domyślne = `src/main/resources/config.yml` (wartości dziś na world1 są takie same, poza `pvp-poza-ringiem`).
| klucz | znaczenie | world1 / domyślna | źródło |
|---|---|---|---|
| `world` | nazwa świata areny | arena / arena | `ArenaPlugin.java:131-132` |
| `pvp-tylko-w-ringu` | true = PvP w całej grze tylko w ringu (poza ringiem anulowane) | true / true | `ArenaPlugin.java:407`, `:475`, `:622` |
| `pvp-poza-ringiem` | **w kodzie `main` NIEUŻYWANY** (0 trafień w `src`). [walka-poprawki] `strefy` = poza areną PvP tylko gdy region WG w miejscu ofiary i atakującego ma jawnie `pvp: allow` (`StrefyPvp.java:12-16`, `ArenaPlugin.java` w gałęzi `illegalPvp`/`pvpPozaRingiemZakazane`); `domyslnie` = wg `pvp-tylko-w-ringu` | **strefy** / (w jarze) domyślnie | `unzip -p arena-*.jar config.yml` |
| `sonda-pvp` | true rejestruje `Sonda` (log kto anuluje cios) i logi `dbg` | false / false | `ArenaPlugin.java:92`, `:438`, `Sonda.java` |
| `entry-worlds` | światy, z których wolno wejść (i do których wolno wrócić) | world, stolica_odnowa, stolica | `ArenaPlugin.java:201`, `:249`, `:277` |
| `fallback-worlds` | świat awaryjnego powrotu (spawn pierwszego istniejącego) | stolica_odnowa, stolica, world | `ArenaPlugin.java:256-262` |
| `blocked-player-commands` | komendy zablokowane graczowi w świecie areny (porównanie pierwszego słowa) | 25 komend (/tp, /spawn, /kill, /execute…) | `ArenaPlugin.java:376-380` |
| `service.startup-delay-ticks` / `player-check-ticks` / `number-cleanup-delay-ticks` / `exp-restore-delay-ticks` | opóźnienie pętli, okres kontroli graczy, sprzątanie liczb po starcie, opóźnienie przywrócenia EXP | 40 / 20 / 80 / 1 | `ArenaPlugin.java:112,120,121,520` |
| `ring.x/z/pvp-radius/min-y/max-y` | walec ringu (promień 19,5, y 63–74) | 0,0,19.5,63,74 | `ArenaPlugin.java:137-143` |
| `spawn.x/y/z/yaw/pitch`, `spawn.invulnerable-seconds` | miejsce wejścia/respawnu, nietykalność 3 s | 0.5/65/-28.5, 3 | `ArenaPlugin.java:136`, `:288`, `:527` |
| `boundary.radius`, `boundary.min-y` | poza promieniem 64 / pod y 45 → teleport na spawn | 64 / 45 | `ArenaPlugin.java:353-362` |
| `entry.max-fall-distance` | maks. dystans spadania przy wejściu | 0.1 | `ArenaPlugin.java:202`, `:278` |
| `menu-gracza.podpowiedz/zasieg/zwykle/cooldown/waznosc-pojedynku` | podpowiedź [Q] na action barze, zasięg raytrace, komenda zapasowa, cooldown zaproszeń (s), ważność wyzwania (s) | true / 4.5 / `dm open hub_glowne %gracz%` / 15 / 30 | `MenuGracza.java:112,121,145,236,246` |
| `menu-gracza.ring.pierwszy/drugi` | punkty startowe pojedynku | (0.5,64,-8.5) / (0.5,64,9.5) | `ArenaPlugin.java:220` |
| `combat.seconds`, `combat.logout-penalty-seconds` | znacznik walki (blokuje wyjście) / kara za wyjście w walce | 10 / 60 | `ArenaPlugin.java:235`, `:317` |
| `ui.*` (actionbar-ticks, series-reset-seconds, number-cooldown-ms, max-numbers, number-height, number-lifetime-ticks) | pasek DPS i liczby `TextDisplay` nad manekinem | 60/6/120/80/2.3/20 | `ArenaPlugin.java:145,544,556,558,559,566` |
| `dummy.*` (mob-id, check-ticks, owner-window-seconds, max-fire-seconds, search-radius-xz/y, max-displacement, positions.one/two/three) | manekin = mob MythicMobs `ARENA_MANEKIN`, stacje x=35.5, z=-5.5/0.5/6.5 | wg tabeli config | `ArenaPlugin.java:111,455,574-597` |
Kontrola kluczy najwyższego poziomu z configu world1 (grep `"klucz` w `src`): wszystkie 16 użyte w kodzie `main`, wyjątek `pvp-poza-ringiem` (0).

## Jak działa
- **Wejście** (`enter`): sprawdza `entry-worlds`, stoi na ziemi, nie jest w walce MMOCore; zapisuje `ReturnPoint` (świat, xyz, kąty, **bieżący profil MMOProfiles**), `saveState()`, teleportuje (`ArenaPlugin.java:274-290`). Teleport do/z areny inną drogą jest anulowany (`onTeleport`, `:333-352`); wewnątrz areny (perła) wolno; wyjście po `combat.seconds` bez walki wolno.
- **Wyjście/ewakuacja**: `leave`/`evacuate` (restart, quit, zmiana postaci, join w arenie); w walce → `forfeitUntil` = kara `logout-penalty-seconds` (zakaz ringu), `:313-331`. Punkt powrotu nieprawidłowy (inny profil, zablokowany blok) → `fallback-worlds`.
- **PvP** (`PlayerAttackEvent` HIGHEST `:382`, `EntityDamageEvent` HIGHEST `:446`): `illegalPvp` (`:405-409`), nietykalność po wejściu/respawnie, zakaz pocisków wystrzelonych spoza ringu (`launchRingKey`), minionów (`DamageType.MINION`) na arenie, mikstur/chmur (`onSplash` `:617`, `onCloud` `:627`, `onLingering` `:644`). Właściciel pocisku/zwierzaka/miniona jest ustalany przez `playerOwner` (`:421-437`, w tym `Tameable` i MythicMobs `getOwnerUUID`).
- **Manekin** (`isDummy`: PDC `arena:dummy`): każdy cios jest anulowany, lecz zliczany do `Meter` gracza (seria 6 s → action bar „Seria · DPS”) i wyświetlany jako `TextDisplay`; DOT przypisany do ostatniego bijącego w oknie `owner-window-seconds` (`:386-395`, `:448-461`, `:539-567`).
- **Śmierć na arenie**: `setKeepInventory`, brak dropu/EXP, własny komunikat na czacie świata areny; EXP MMOCore zapisany `LOWEST`, przywrócony `MONITOR` po 1 ticku (`:496-521`); respawn na `spawn` z nietykalnością (`:522-533`). Zdarzenia EXP MMOCore/vanilla i osiągnięcia na arenie anulowane (`:487-495`). Blokady: budowanie, wiaderka, upuszczanie/podnoszenie, wybuchy (blockList czyszczone), pojazdy, obrazy (`:605-616`).
- **Menu Q** (`MenuGracza`): dialog Paper (`Dialog.create`, `DialogAction.customClick`) z jednorazowymi kluczami `arena:menu_gracza/<uuid>/<rodzaj>` przypisanymi do widza, celu i profili obu (`:148-160`, `:209-229`). Pojedynek: wyzwanie → dialog u celu → `enterForDuel` → `placeForDuel` → odliczanie 3-2-1-WALCZ (`Countdown`, `:54-85`, `:273-305`), przerwanie przy wyjściu/śmierci/zmianie postaci (`:335-361`). Zaproszenia drużyna/znajomi/gildia przez API MMOCore (`:306-334`). Linia „Patron” z PlaceholderAPI `%wiary_patron%` (`:189-200`, plugin Wiary).
- **Integracje**: MythicLib (`PlayerAttackEvent`, `DamageMetadata`), MMOCore (`PlayerData`, `PlayerExperienceGainEvent`, party/guild), MythicMobs (spawn manekina), WorldGuard (`depend`; w main bez użycia API — [walka-poprawki] `StrefyPvp`), MMOProfiles przez refleksję (`getPlayerData().getCurrent().getUniqueId()`, `fr.phoenixdevt.profiles.event.ProfileUnloadEvent`, `:149-197`), PlaceholderAPI, Zaklecia (odczyt `klawisz-q.zwykle`).

## Koszt dla serwera
- Zadanie co 1 tick (licznik `tick`); `ensureDummies` co `dummy.check-ticks`=100 ticków (ładuje chunk, `world.getEntities()` świata arena — `:591`); kontrola graczy świata arena co 20 ticków (`:109-120`).
- `menuGracza.hints()` co 5 ticków dla KAŻDEGO gracza online (raytrace do 4,5 bloku) niezależnie od świata (`ArenaPlugin.java:107`, `MenuGracza.java:120-131`; pomija walkę/okno/ring). Nie mierzone.
- Każdy cios w manekina: `getEntitiesByClass(TextDisplay)` całego świata arena (`:558`) — liczone przy każdym trafieniu (cooldown liczb 120 ms). Nie mierzone.
- Zapis na dysk: `returns.yml` przy każdym wejściu/wyjściu/ewakuacji (zapis przez plik tymczasowy + ATOMIC_MOVE, `:664-677`). Pozostałe stany w pamięci.

## Dane
`plugins/Arena/returns.yml` (dziś pusty, 0 B): `returns.<UUID>.{world,x,y,z,yaw,pitch,profile}` i `forfeit.<UUID>` = ms do końca kary (`:654-677`). Klucz = UUID gracza Bukkit — MMOProfiles daje wszystkim postaciom gracza jeden UUID, dlatego w punkcie powrotu jest dodatkowo `profile` (UUID bieżącej postaci); powrót z innego profilu jest odrzucany (`profilesDiffer`, `validReturn` `:162-164`, `:247-255`), a `ProfileUnloadEvent` ewakuuje i poprawia lokację w migawce MMOProfiles (`:175-197`). **Kara `forfeit` jest po UUID konta, nie po postaci** (obejmuje wszystkie postacie gracza).

## Znane pułapki i decyzje
- `~/.claude/skills/mc-walka-klasy-zaklecia/arena-trafienia.md:5` — Arena: manekiny po pierwszym starcie nie powstały (po restarcie 3), WG nadpisuje regions.yml, CMI respawn nadpisuje miejsce odrodzenia.
- `…/arena-trafienia.md:7` — test Trafień z produkcyjną walką na klonie (21.10), klon `--z-kreatorem` trzyma bota w scenie wyboru.
- `…/arena-trafienia.md:8` — `pvp-poza-ringiem: domyslnie|strefy` (gałąź walka-poprawki a999b41, decyzja 21.10 21:46); `strefy` = PvP poza areną tylko gdy region WG ofiary i atakującego ma `pvp allow`.
- `…/arena-trafienia.md:9` — `combat.seconds: 10`: `arena wyjdz` odmawia 10 s po ciosie; w scenariuszu czekaj >= 11 s.

## Budowanie i wdrożenie
Gradle (`build.gradle.kts`), toolchain Java 21, `compileOnly` Paper 1.21.11 + przypięte jary z `-PserverPlugins=/serverplugins` (MythicLib, MMOItems, MMOCore, MythicMobs, worldguard; `:12-22`); na hoście brak `java/gradle`, buduje się w kontenerze `gradle:8.12-jdk21` (`~/.claude/skills/mc-host-testy-wdrozenia/testy-narzedzia.md:46`). [walka-poprawki] dodatkowo FAWE jar (`BukkitAdapter`). Brak `build.sh`. Wdrożenie: `wdrozenie/wdroz.sh` (`--sucho`, kopia świata, `backups/arena-<czas>/cofnij.sh`, restart przy 0 graczy; md5 12 plików w `wdrozenie/baza/md5.txt`); tylko po zgodzie właściciela. Testy: `testy/` (scenariusze A–K, `testy/README.md`, uruchamiane `narzedzia/bot/run.sh <klon> testy/<plik>.js`) i `testy-bot/`.

## Podejrzane (nie poprawiane)
- `onEnable` rejestruje `Sonda` (`:92`) przed `saveDefaultConfig()` (`:93`) — działa dzięki domyślnym z jaru, ale kolejność odwrotna do zamierzonej.
- Źródło w `main` nie zawiera kodu, który działa na world1 (`StrefyPvp`, `pvp-poza-ringiem`) — gałąź `walka-poprawki` niewcielona; następna budowa z `main` cofnie tryb `strefy`.
- `permission: arena.console` nie chroni komend: realną barierą jest sprawdzenie typu nadawcy w kodzie.
