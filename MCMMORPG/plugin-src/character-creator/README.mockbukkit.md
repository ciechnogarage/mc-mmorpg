# Testy MockBukkit (pilotaz)

MockBukkit `org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.116.3` (paper-api 1.21.11). Bez klona swiata.
Jary pluginow z build.gradle.kts (ModelEngine, MMOProfiles...) kopiujesz do katalogu `<JARY>` i podajesz `-PccServerPlugins=<JARY>`.

```
docker run --rm --name mockbukkit-pilot-gradle --memory 2g -u "$(id -u):$(id -g)" \
  -v <REPO>:<REPO> -v <TMP>:<TMP> -w <REPO>/MCMMORPG/plugin-src/character-creator \
  -e GRADLE_USER_HOME=<TMP>/gradle-home gradle:8.12-jdk21 \
  gradle --no-daemon -PccServerPlugins=<JARY> test
```
Ograniczenie: klasa pluginu nie moze byc `final` (MockBukkit robi proxy bytebuddy). Plugin nie wstaje w calosci
(twarde depend na MMOProfiles/ModelEngine) - testujemy load, config, walidacje, komende.
