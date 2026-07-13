#!/usr/bin/env bash
# Assembler samowystarczalnych /data per domena z ŻYWEGO serwera MCMMORPG (read-only kopia).
# Nie rusza uruchomionego serwera: tylko czyta plugins/, libraries/, world/, config/.
# Idempotentny: czyści servers/<domena> przed odtworzeniem.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$(cd "$HERE/.." && pwd)"           # ~/projects/MC/MCMMORPG
SERVERS="$HERE/servers"
PLUG="$SRC/plugins"

# --- Podział JARÓW (globy wg nazw z plugins/) ---
SHARED_JARS=(ProtocolLib packetevents PlaceholderAPI Vault- LuckPerms MythicLib CMILib PixelLibs)
WORLD_JARS=(FastAsyncWorldEdit Iris- VoidGen MythicDungeons MythicMobs MythicCrucible ModelEngine MEG-Molang MCPets Chunky CoreTools worldguard multiverse-core LibsDisguises)
ITEMS_JARS=(CharacterStage CoreTools MMOCore MMOItems MMOInventory MMOProfiles MythicHUD DeluxeMenus nexo DecentHolograms)
HUB_JARS=(CMI- Guilds voicechat DecentHolograms)

# --- Podział DANYCH pluginów (nazwy katalogów w plugins/) ---
SHARED_DATA=(ProtocolLib PlaceholderAPI Vault LuckPerms MythicLib CMILib bStats spark)
WORLD_DATA=(FastAsyncWorldEdit Iris VoidGen MythicDungeons MythicMobs MythicCrucible ModelEngine MEG-Molang MCPets Chunky CoreTools WorldGuard Multiverse-Core LibsDisguises)
ITEMS_DATA=(CoreTools MMOCore MMOItems MMOInventory MMOProfiles MythicHUD DeluxeMenus Nexo DecentHolograms)
HUB_DATA=(CMI Guilds voicechat DecentHolograms)

copy_jars() {  # $1=domena, dalej: globy
  local dom="$1"; shift
  local dst="$SERVERS/$dom/plugins"
  for pat in "$@"; do
    # dopasuj po prefiksie nazwy pliku
    find "$PLUG" -maxdepth 1 -type f -name "${pat}*.jar" -exec cp -a {} "$dst/" \; 2>/dev/null || true
  done
}
copy_data() {  # $1=domena, dalej: nazwy katalogów
  local dom="$1"; shift
  local dst="$SERVERS/$dom/plugins"
  for d in "$@"; do
    [ -d "$PLUG/$d" ] && cp -a "$PLUG/$d" "$dst/" || true
  done
}

build_domain() {  # $1=domena
  local dom="$1"
  local dir="$SERVERS/$dom"
  echo ">> buduję domenę: $dom"
  rm -rf "$dir"
  mkdir -p "$dir/plugins" "$dir/config"

  # rdzeń: paper.jar + libraries (samowystarczalne, offline)
  cp -a "$SRC/paper.jar" "$dir/paper.jar"
  cp -a "$SRC/libraries" "$dir/libraries"

  # wspólne configi serwera (server.properties zarządza itzg z env — NIE kopiujemy go)
  cp -a "$SRC/bukkit.yml" "$dir/bukkit.yml" 2>/dev/null || true
  cp -a "$SRC/spigot.yml" "$dir/spigot.yml" 2>/dev/null || true
  cp -a "$SRC/config/paper-global.yml"        "$dir/config/paper-global.yml"
  cp -a "$SRC/config/paper-world-defaults.yml" "$dir/config/paper-world-defaults.yml" 2>/dev/null || true

  # velocity modern forwarding ON w paper-global.yml (sekret wstrzyknie compose przez env? nie —
  # paper czyta z pliku; wstawiamy sekret tutaj z $FORWARDING_SECRET)
  python3 - "$dir/config/paper-global.yml" "$FORWARDING_SECRET" <<'PY'
import sys, re
p, secret = sys.argv[1], sys.argv[2]
s = open(p).read()
# blok:
#   velocity:
#     enabled: false
#     online-mode: true
#     secret: ''
s = re.sub(r'(  velocity:\n    enabled: )false', r'\1true', s)
s = re.sub(r'(  velocity:\n    enabled: true\n    online-mode: )true', r'\1false', s)
s = re.sub(r"(  velocity:\n    enabled: true\n    online-mode: false\n    secret: )''", r"\1'%s'" % secret, s)
open(p,'w').write(s)
print("   velocity forwarding: enabled + secret set")
PY

  # jary + dane wg domeny (shared zawsze)
  case "$dom" in
    world) copy_jars "$dom" "${SHARED_JARS[@]}" "${WORLD_JARS[@]}"; copy_data "$dom" "${SHARED_DATA[@]}" "${WORLD_DATA[@]}";;
    items) copy_jars "$dom" "${SHARED_JARS[@]}" "${ITEMS_JARS[@]}"; copy_data "$dom" "${SHARED_DATA[@]}" "${ITEMS_DATA[@]}";;
    hub)   copy_jars "$dom" "${SHARED_JARS[@]}" "${HUB_JARS[@]}";   copy_data "$dom" "${SHARED_DATA[@]}" "${HUB_DATA[@]}";;
  esac

  # światy: tylko world dostaje realne światy + mapy lochów; items/hub generują świeży flat
  if [ "$dom" = "world" ]; then
    for w in world world_nether world_the_end; do
      [ -d "$SRC/$w" ] && cp -a "$SRC/$w" "$dir/$w" || true
    done
  fi

  echo -n "   jary: "; ls "$dir/plugins"/*.jar 2>/dev/null | wc -l
  echo -n "   rozmiar /data: "; du -sh "$dir" | cut -f1
}

: "${FORWARDING_SECRET:?ustaw FORWARDING_SECRET (z docker/.env)}"
mkdir -p "$SERVERS"
for d in world items hub; do build_domain "$d"; done
echo "== gotowe =="
