# Lobby — wyspa w pustce

Plugin produkcyjnego lobby (`/opt/docker/mc/data/lobby/plugins/lobby.jar`).

- Świat pusty z konfiguracji kontenera (`docker-compose.yml`, sekcja `lobby`: `LEVEL=wyspa`, preset The Void).
- Wyspę buduje plugin raz (znacznik `lobby:wersja` w danych świata, `BUILD_VERSION` w `LobbyPlugin`);
  `/lobby przebuduj` stawia ją od nowa, `/lobby przeladuj` wczytuje config (portale, hologramy).
- Portale: `config.yml` → `portale.polnoc|wschod|zachod`. Pusty `serwer` = łuk „Wkrótce”.
  Przenoszenie przez kanał `BungeeCord` → `Connect` (Velocity: `bungee-plugin-message-channel = true`).
- Status nad portalem i w menu: Server List Ping wprost do backendu co 2 s; „gotowy” po 3 udanych pingach.
- Kolejka: serwer nie działa → BossBar i automatyczne przeniesienie, gdy wstanie. Wchodzący do lobby
  w czasie restartu `auto-kolejka` trafia do niej sam.
- Bajery: podwójny skok, wyrzutnia (złota płyta) pod portal, wodospad, świetliki, wysepki, powrót z pustki.

Build: `./build.sh`. Evale w `eval/` (bot mineflayer; `lobby.js`/`queue-dbg.js` na izolowanym serwerze
testowym bez velocity, `proxy.js` przez proxy). Bot musi zarejestrować kanał `bungeecord:main`,
inaczej Bukkit nie wyśle mu `Connect` (za proxy robi to Velocity).
