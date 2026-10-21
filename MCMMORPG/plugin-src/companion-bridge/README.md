# CompanionBridge

Read-only HTTP bridge dla `~/projects/MC/bots/packages/mmo-bridge`
(companion-provider) — docelowo zastepuje polling przez RCON jako zrodlo
stanu gracza/serwera. Kontrakt: `bots/contracts/companion-bridge/v1/`.

Scope tego szkicu (transza 1 z planu companion-bridge):

- `GET /v1/health`
- `GET /v1/capabilities` — raportuje wylacznie OBECNOSC Vault/MMOCore/
  MMOProfiles/spark (`Bukkit.getPluginManager().getPlugin(...)`), bez
  wywolywania ich API.
- `GET /v1/server/performance` — `Server#getTPS()` / `getAverageTickTime()`
  / `getCurrentTick()` (zweryfikowane refleksja wzgledem paper-api
  1.21.11 w `MCMMORPG/libraries/`, nie zgadywane).
- `GET /v1/players/{uuid}/snapshot` — tylko pola z czystego Bukkit/Paper API
  (online/world/location/health/food/gameMode). `integrations.*` = zawsze
  `{"available": false}` w tej transzy.

Non-goals tego szkicu:

- adaptery Vault/MMOCore/MMOProfiles (realne odczyty economy/klasy/profilu)
  — wymagaja inspekcji API zainstalowanych jarow, nastepna transza
- wystawienie portu poza localhost hosta — izolacja jest realizowana
  wylacznie prefiksem `127.0.0.1:` w publikacji portu w `docker/compose.yml`
- WebSockety/custom packety — polling HTTP wystarcza do testow E2E

Gotcha zweryfikowany na zywym `srv-world` (2026-07-14): bind pluginu na
`127.0.0.1` WEWNATRZ kontenera jest NIEOSIAGALNY przez publikowany port
Dockera — `docker-proxy` przekierowuje na interfejs mostka kontenera, nie
na jego loopback (`docker exec ... curl 127.0.0.1:25580` dziala, curl z
hosta dostaje "connection reset" mimo poprawnego `docker port`). Dlatego
domyslny bind to `0.0.0.0` (patrz `config.yml`), a jedyna realna izolacja
do hosta to `127.0.0.1:PORT:PORT` w `docker/compose.yml`.

Build note (jak w `character-stage/`):

- ten workspace nie ma zainstalowanego Gradle; `build.gradle.kts` jest
  wylacznie dokumentacja deklarowanych zaleznosci
- realny build: `java tools/BuildCompanionBridge.java` (single-file source
  launch, kompiluje wzgledem `../../libraries/*.jar` jako classpath, pakuje
  `build/libs/companion-bridge-0.1.0-SNAPSHOT.jar`)
- deploy: skopiowac jar do `docker/servers/<domain>/plugins/`, zrestartowac
  kontener backendu (`docker/mc down <domena> && docker/mc up <domena>`)
- HTTP server: `com.sun.net.httpserver.HttpServer` (JDK wbudowany) — brak
  mozliwosci shade'owania wlasnych zaleznosci HTTP bez Gradle/shadow-jar,
  wiec swiadomie zero zewnetrznych bibliotek poza gson (juz obecny na
  classpath serwera przez `adventure-text-serializer-gson`)
