#!/usr/bin/env bash
# Klon world1 do QA animacji broni (ab-test): pluginy world1 bez baz/userdata + nasz jar/config/modele, plaski swiat.
# Uzycie: klon.sh <katalog_roboczy> [--fresh]. Po testach: docker stop ab-test mc-client-ab
set -euo pipefail
H="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; W=/opt/docker/mc/data/world1
D=${1:?katalog roboczy}; D=$(realpath -m "$D")
if [[ "${2:-}" == "--fresh" ]]; then docker rm -f ab-test >/dev/null 2>&1 || true; rm -rf "$D/data"; fi
mkdir -p "$D/data"
if [[ ! -d "$D/data/plugins" ]]; then
  tar -C "$W" -cf - \
    --exclude='plugins/Iris*' --exclude='plugins/spark' --exclude='plugins/voicechat*' --exclude='plugins/Chunky*' \
    --exclude='plugins/CharacterCreator' --exclude='plugins/character-creator-*.jar' \
    --exclude='plugins/MMOProfiles' --exclude='plugins/MMOProfiles-*.jar' \
    --exclude='plugins/Lobby*' --exclude='plugins/lobby-*.jar' \
    --exclude='*/userdata' --exclude='*/backups' --exclude='*.db' --exclude='*.sqlite*' --exclude='*.mv.db' \
    --exclude='plugins/CMI/Logs' --exclude='plugins/CMI/DatabaseBackups' --exclude='plugins/MythicMobs/data' \
    --exclude='plugins/.paper-remapped' \
    plugins paper-1.21.11-132.jar | tar -C "$D/data" -xf -
  sed -i 's/^force-class-selection:.*/force-class-selection: false/' "$D/data/plugins/MMOCore/config.yml" || true
fi
P=$D/data/plugins
rm -f "$P"/animacje-broni-*.jar; cp "$(ls -t "$H"/build/libs/animacje-broni-*.jar | head -1)" "$P/"
mkdir -p "$P/AnimacjeBroni"; cp "$H/wynik/config.yml" "$P/AnimacjeBroni/config.yml"
rm -rf "$P/Nexo/pack/assets/bron"; cp -a "$H/wynik/pack/assets/bron" "$P/Nexo/pack/assets/bron"
docker network inspect ab-net >/dev/null 2>&1 || docker network create --subnet ${AB_SUBNET:-10.99.40.0/24} ab-net >/dev/null   # nie 192.168.64/20 (LAN wlasciciela); po testach: docker network rm ab-net
docker rm -f ab-test >/dev/null 2>&1 || true
docker run -d --name ab-test --network ab-net --memory 3400m -u 1000:1000 -v "$D/data:/data" \
  -e EULA=true -e TYPE=CUSTOM -e CUSTOM_SERVER=/data/paper-1.21.11-132.jar -e VERSION=1.21.11 \
  -e ONLINE_MODE=false -e ENABLE_RCON=true -e RCON_PASSWORD=abtest -e MAX_MEMORY=2G -e INIT_MEMORY=1G \
  -e TZ=Europe/Warsaw -e DIFFICULTY=peaceful -e MODE=survival -e SPAWN_MONSTERS=false -e SPAWN_ANIMALS=false \
  -e SPAWN_NPCS=false -e GENERATE_STRUCTURES=false -e VIEW_DISTANCE=4 -e SIMULATION_DISTANCE=4 -e SPAWN_PROTECTION=0 \
  -e LEVEL_TYPE=minecraft:flat \
  -e 'GENERATOR_SETTINGS={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":3},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}' \
  itzg/minecraft-server >/dev/null
echo "start: ab-test (dane $D/data)"
