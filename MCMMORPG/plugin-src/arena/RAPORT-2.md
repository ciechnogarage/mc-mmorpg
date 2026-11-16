# RAPORT 2 — Otwarta Arena, 17.10.2026

## Zmiany
- `src/main/java/pl/mcmmorpg/arena/ArenaPlugin.java:355–445`: domyślna blokada PvP poza ringiem we wszystkich światach; ciosy, skille `PlayerAttackEvent`, pociski, oswojone zwierzęta i moby MythicMobs z właścicielem. UUID właściciela pocisku zostaje zapisany także na wypadek wylogowania.
- `src/main/java/pl/mcmmorpg/arena/ArenaPlugin.java:571–605`: szkodliwe mikstury i chmury filtrują ofiary graczy; leczenie pozostaje dozwolone poza areną, samozranienie nie jest blokowane.
- `src/main/resources/config.yml:2–3`: `pvp-tylko-w-ringu: true`, `sonda-pvp: false`; `Sonda.java` bez zmian.
- `wdrozenie/pliki/MythicLib/config.yml.PATCH.md:1`: dokładnie dwa klucze `party_other` i `guild_enemy`; `wdrozenie/swiat/wg-pvp-deny.txt:1`: cztery globalne flagi WG jako druga zapora.
- `src/main/java/pl/mcmmorpg/arena/ArenaPlugin.java:479–494`: `onRespawn` na HIGHEST, kontrola pozycji tick później; `wdrozenie/swiat/ustaw.txt:2–4`: vanilla world spawn i osobny spawn Multiverse na platformie.
- `wdrozenie/swiat/regions.yml:50`: `damage-animals: allow` dla manekinów IRON_GOLEM; `mob-damage` wg WG dotyczy mob→gracz i nie jest potrzebne.
- `src/main/java/pl/mcmmorpg/arena/ArenaPlugin.java:143–188,248–331`: wybrany profil z MMOProfiles, ewakuacja przy unload/quit/restart, swobodne obce teleporty z areny bez combat tag, czyszczenie stanu.
- `src/main/resources/plugin.yml:6`: miękka zależność MMOProfiles bez dodawania nowej zależności kompilacji.
- `src/main/java/pl/mcmmorpg/arena/ArenaPlugin.java:446–489`: przywracanie MMOCore EXP zachowane; po odrodzeniu pasek priorytetu 40 wyjaśnia brak straty.
- `src/main/java/pl/mcmmorpg/arena/ArenaPlugin.java:496–510`, `src/main/resources/config.yml:35`: seria resetowana po 6 s; DPS z czasu serii, nowe brzmienie paska.
- `wdrozenie/klon-przygotuj.sh:10–38`, `wdrozenie/wdroz-szkic.sh:18–22`: klon dodaje dwa klucze MythicLib i flagi WG; spawn MV ustawia dopiero po zbudowaniu platformy. Szkic operatora podaje tę samą kolejność.

## Ustalenia i granice
- MMOProfiles 1.2 tworzy migawkę postaci **przed** `ProfileUnloadEvent`; handler po teleportacji poprawia lokalizację tej migawki przez `LocationMap.setCurrent`. Potwierdzono kolejność i API w jarze world1 metodą `javap`.
- MythicLib nasłuchuje `PlayerQuitEvent` również na LOWEST i ładuje się wcześniej; sama kolejność quit listenerów nie daje gwarancji. Poprawa migawki przy `ProfileUnloadEvent` zabezpiecza ten przypadek.
- MMOCore `DeathExperienceLoss` odejmuje procent EXP w `PlayerDeathEvent` na HIGH, bez sprawdzenia świata ani uprawnienia; jedyna opcja `death-exp-loss.enabled` jest globalna. Nie da się wyłączyć tylko dla areny tym API, więc pozostaje przywracanie.
- Nie budowano, nie uruchamiano serwera i nie wykonywano testów zgodnie ze zleceniem. `git diff --check` nie wykazał błędów białych znaków; zachowanie wymaga odbioru przez Claude na klonie.

## Źródła dokumentacji
- Respawn: [CMI Spawn](https://www.zrips.net/cmi/commands/spawn/), [Multiverse setspawn](https://mvplugins.org/core/fundamentals/commands-usage/).
- Profil: [MMOProfiles Profile API](https://gitlab.com/phoenix-dvpmt/mmoprofiles/-/wikis/Profile-API); szczegóły kolejności ustalone z jara 1.2.
- EXP: [MMOCore permissions](https://docs.phoenixdevt.fr/mmocore/general/permissions.html), [MythicLib player messages](https://docs.phoenixdevt.fr/mythiclib/misc/player-messages); warunek `DeathExperienceLoss` sprawdzony w jarze 1.13.1.
- Manekiny: [WorldGuard region flags](https://worldguard.enginehub.org/en/latest/regions/flags/).
