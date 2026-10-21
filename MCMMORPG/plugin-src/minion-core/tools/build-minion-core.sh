#!/usr/bin/env bash
# Builds MinionCore against the exact Paper, WorldGuard, FAWE, and MythicMobs
# jars available in the local server tree. Vendor jars remain compile-only and
# are never unpacked into the plugin jar.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_ROOT="$(cd "$ROOT/../.." && pwd)"
RUNTIME_ROOT="$SERVER_ROOT/docker/servers/world"
VERSION="0.1.0-SNAPSHOT"
OUT_JAR="$ROOT/build/libs/minioncore-$VERSION.jar"

require_file() {
  [[ -f "$1" ]] || {
    echo "Missing required file: $1" >&2
    exit 1
  }
}

require_same_sha256() {
  local label="$1" source="$2" runtime="$3"
  require_file "$source"
  require_file "$runtime"

  local source_sha runtime_sha
  source_sha="$(sha256sum "$source" | awk '{print $1}')"
  runtime_sha="$(sha256sum "$runtime" | awk '{print $1}')"
  [[ "$source_sha" == "$runtime_sha" ]] || {
    echo "$label source/runtime SHA-256 mismatch:" >&2
    echo "  source:  $source_sha  $source" >&2
    echo "  runtime: $runtime_sha  $runtime" >&2
    exit 1
  }
}

SOURCE_PAPER_API="$SERVER_ROOT/libraries/io/papermc/paper/paper-api/1.21.11-R0.1-SNAPSHOT/paper-api-1.21.11-R0.1-SNAPSHOT.jar"
SOURCE_WORLDGUARD="$SERVER_ROOT/plugins/worldguard-bukkit-7.0.15.jar"
SOURCE_FAWE="$SERVER_ROOT/plugins/FastAsyncWorldEdit-Paper-2.15.1-SNAPSHOT-1283.jar"
SOURCE_MYTHICMOBS="$SERVER_ROOT/plugins/MythicMobs-5.11.2.jar"
RUNTIME_PAPER_API="$RUNTIME_ROOT/libraries/io/papermc/paper/paper-api/1.21.11-R0.1-SNAPSHOT/paper-api-1.21.11-R0.1-SNAPSHOT.jar"
RUNTIME_WORLDGUARD="$RUNTIME_ROOT/plugins/worldguard-bukkit-7.0.15.jar"
RUNTIME_FAWE="$RUNTIME_ROOT/plugins/FastAsyncWorldEdit-Paper-2.15.1-SNAPSHOT-1283.jar"
RUNTIME_MYTHICMOBS="$RUNTIME_ROOT/plugins/MythicMobs-5.11.2.jar"

require_same_sha256 "Paper API" "$SOURCE_PAPER_API" "$RUNTIME_PAPER_API"
require_same_sha256 "WorldGuard" "$SOURCE_WORLDGUARD" "$RUNTIME_WORLDGUARD"
require_same_sha256 "FastAsyncWorldEdit" "$SOURCE_FAWE" "$RUNTIME_FAWE"
require_same_sha256 "MythicMobs" "$SOURCE_MYTHICMOBS" "$RUNTIME_MYTHICMOBS"

docker run --rm --name "build-minion-core-$$" --memory 2g \
  --user "$(id -u):$(id -g)" \
  --volume "$SERVER_ROOT:/server" \
  --workdir /server/plugin-src/minion-core \
  eclipse-temurin:21-jdk \
  bash -euc '
    rm -rf build/classes build/test-classes build/libs
    mkdir -p build/classes build/test-classes build/libs

    mapfile -d "" sources < <(find src/main/java -type f -name "*.java" -print0 | sort -z)
    (( ${#sources[@]} > 0 )) || { echo "No Java sources found" >&2; exit 1; }

    libraries="$(find /server/libraries -type f -name "*.jar" -print | sort | paste -sd: -)"
    [[ -n "$libraries" ]] || { echo "No Paper runtime libraries found" >&2; exit 1; }
    vendor_cp="/server/plugins/worldguard-bukkit-7.0.15.jar:/server/plugins/FastAsyncWorldEdit-Paper-2.15.1-SNAPSHOT-1283.jar:/server/plugins/MythicMobs-5.11.2.jar"

    javac --release 21 \
      -proc:none \
      -classpath "$libraries:$vendor_cp" \
      -d build/classes "${sources[@]}"

    if [[ -d src/test/java ]]; then
      mapfile -d "" tests < <(find src/test/java -type f -name "*.java" -print0 | sort -z)
      if (( ${#tests[@]} > 0 )); then
        javac --release 21 \
          -proc:none \
          -classpath "build/classes:$libraries:$vendor_cp" \
          -d build/test-classes "${tests[@]}"

        for test_class in \
          pl.mcmmorpg.minioncore.MinionCoreFlagGateTest \
          pl.mcmmorpg.minioncore.MinionGroupRegistryTest \
          pl.mcmmorpg.minioncore.MinionMeleeDefinitionTest \
          pl.mcmmorpg.minioncore.GroupLifecycleSchedulerTest \
          pl.mcmmorpg.minioncore.PvpCompatibilityGateTest \
          pl.mcmmorpg.minioncore.ProvisionalDamageGuardTest \
          pl.mcmmorpg.minioncore.RoleBehaviorTest \
          pl.mcmmorpg.minioncore.TargetResolverTest
        do
          java -ea \
            -classpath "build/test-classes:build/classes:$libraries:/server/plugins/worldguard-bukkit-7.0.15.jar:/server/plugins/MythicMobs-5.11.2.jar" \
            "$test_class"
        done
      fi
    fi

    sed "s/\${version}/0.1.0-SNAPSHOT/g" src/main/resources/plugin.yml > build/classes/plugin.yml
    jar --create --file build/libs/minioncore-0.1.0-SNAPSHOT.jar -C build/classes .
  '

[[ -f "$OUT_JAR" ]] || {
  echo "Build did not produce $OUT_JAR" >&2
  exit 1
}

if unzip -Z1 "$OUT_JAR" | grep -Eq '^(com/sk89q/|com/fastasyncworldedit/|io/lumine/|org/bukkit/)'; then
  echo "Refusing vendor classes packaged in $OUT_JAR" >&2
  exit 1
fi

echo "BUILT $OUT_JAR"
echo "COMPILE_GATE_PASS source/runtime jars match and vendor classes are excluded"
