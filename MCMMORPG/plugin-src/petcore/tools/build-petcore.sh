#!/usr/bin/env bash
# Isolated Java 21 build/test harness. The workspace has no host JDK or Gradle.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_ROOT="$(cd "$ROOT/../.." && pwd)"
VERSION="0.1.0-SNAPSHOT"

docker run --rm \
  --user "$(id -u):$(id -g)" \
  --volume "$SERVER_ROOT:/server" \
  --workdir /server/plugin-src/petcore \
  eclipse-temurin:21-jdk \
  bash -euc '
    rm -rf build/classes build/test-classes build/libs
    mkdir -p build/classes build/test-classes build/libs
    mapfile -d "" main_sources < <(find src/main/java -type f -name "*.java" -print0 | sort -z)
    (( ${#main_sources[@]} > 0 )) || { echo "No PetCore main sources" >&2; exit 1; }
    libraries="$(find /server/libraries -type f -name "*.jar" -print | sort | paste -sd: -)"
    vendor="/server/plugins/MythicMobs-5.11.2.jar:/server/plugins/MMOProfiles-1.2-20260325.193503-28.jar:/server/plugins/MythicLib-dist-1.7.1-20260330.110510-95.jar:/server/plugins/PlaceholderAPI-2.12.2.jar"
    libraries="$libraries:$vendor"
    javac --release 21 -proc:none -classpath "$libraries" -d build/classes "${main_sources[@]}"
    mapfile -d "" tests < <(find src/test/java -type f -name "*.java" -print0 | sort -z)
    (( ${#tests[@]} > 0 )) || { echo "No PetCore tests" >&2; exit 1; }
    javac --release 21 -proc:none -classpath "build/classes:$libraries" -d build/test-classes "${tests[@]}"
    java -ea -classpath "build/test-classes:build/classes:$libraries" pl.mcmmorpg.petcore.PetCoreKernelTest
    java -ea -classpath "build/test-classes:build/classes:$libraries" pl.mcmmorpg.petcore.PetCoreGlobalPetTest
    java -ea -classpath "build/test-classes:build/classes:$libraries" pl.mcmmorpg.petcore.PetCoreUiStatusTest
    cp src/main/resources/config.yml build/classes/config.yml
    sed "s/\${version}/0.1.0-SNAPSHOT/g" src/main/resources/plugin.yml > build/classes/plugin.yml
    jar --create --file build/libs/petcore-0.1.0-SNAPSHOT.jar -C build/classes .
  '

JAR="$ROOT/build/libs/petcore-$VERSION.jar"
[[ -f "$JAR" ]] || { echo "Build did not produce $JAR" >&2; exit 1; }
if unzip -Z1 "$JAR" | grep -Eq '^(org/bukkit/|io/papermc/|io/lumine/)'; then
  echo "Refusing vendor classes packaged in PetCore" >&2
  exit 1
fi
echo "BUILT $JAR"
