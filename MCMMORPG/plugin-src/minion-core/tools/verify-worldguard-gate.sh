#!/usr/bin/env bash
# Structural gate is safe on a shared workspace. --boot performs an isolated
# Docker-world restart only when that backend is currently stopped.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_ROOT="$(cd "$ROOT/../.." && pwd)"
WORLD_PLUGIN_DIR="$SERVER_ROOT/docker/servers/world/plugins"
JAR_NAME="MinionCore-0.1.0-SNAPSHOT.jar"
JAR="$ROOT/build/libs/minioncore-0.1.0-SNAPSHOT.jar"

"$ROOT/tools/build-minion-core.sh"
[[ -f "$JAR" ]] || { echo "Missing built jar: $JAR" >&2; exit 1; }
unzip -p "$JAR" plugin.yml | grep -Fx '  - WorldGuard' >/dev/null
unzip -p "$JAR" plugin.yml | grep -Fx '  - MythicMobs' >/dev/null
! unzip -Z1 "$JAR" | grep -Eq '^(com/sk89q/|io/lumine/|org/bukkit/)'
echo "STRUCTURAL_GATE_PASS MinionCore declares WorldGuard/MythicMobs correctly and embeds no vendor classes"

if [[ "${1:-}" != "--boot" ]]; then
  echo "BOOT_GATE_NOT_RUN pass --boot only with an idle Docker world backend"
  exit 0
fi

if docker ps --format '{{.Names}}' | grep -Fx 'srv-world' >/dev/null; then
  echo "BOOT_GATE_BLOCKED srv-world is already running; refusing to disrupt a shared backend" >&2
  exit 2
fi

target="$WORLD_PLUGIN_DIR/$JAR_NAME"
backup="$(mktemp)"
had_target=false
if [[ -e "$target" ]]; then
  cp -a "$target" "$backup"
  had_target=true
fi

cleanup() {
  "$SERVER_ROOT/docker/mc" down world >/dev/null 2>&1 || true
  if "$had_target"; then
    cp -a "$backup" "$target"
  else
    rm -f "$target"
  fi
  rm -f "$backup"
}
trap cleanup EXIT

cp -a "$JAR" "$target"
# Paper caches remapped plugin classes by filename. Invalidate the staging copy
# so a changed MinionCore jar (especially telemetry/guards) is actually loaded.
rm -f "$WORLD_PLUGIN_DIR/.paper-remapped/$JAR_NAME"
"$SERVER_ROOT/docker/mc" up world >/dev/null
for _ in $(seq 1 45); do
  logs="$(docker logs srv-world 2>&1 || true)"
  if grep -F "[MinionCore] WorldGuard capability 'minion-pvp' is registered (default: deny)." <<<"$logs" >/dev/null; then
    echo "BOOT_GATE_PASS MinionCore loaded after WorldGuard and registered minion-pvp"
    exit 0
  fi
  if grep -Eq "Could not load 'plugins/$JAR_NAME'|Error occurred while loading MinionCore|Error occurred while enabling MinionCore" <<<"$logs"; then
    echo "BOOT_GATE_FAIL MinionCore failed to load; inspect docker logs srv-world" >&2
    exit 1
  fi
  sleep 2
done

echo "BOOT_GATE_FAIL MinionCore load success was not observed within 90 seconds" >&2
exit 1
