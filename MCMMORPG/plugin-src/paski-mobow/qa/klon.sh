#!/usr/bin/env bash
# Klon world1 do QA paskow mobow (pm-test): pluginy world1 bez baz/userdata + nasz jar/config/czcionka, plaski swiat.
# Uzycie: klon.sh <katalog_roboczy> [--fresh]. Po testach: docker stop pm-test mc-client-pm; docker network rm pm-net
set -euo pipefail
H="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; W=/opt/docker/mc/data/world1
D=${1:?katalog roboczy}; D=$(realpath -m "$D")
if [[ "${2:-}" == "--fresh" ]]; then docker rm -f pm-test >/dev/null 2>&1 || true; rm -rf "$D/data"; fi
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
rm -f "$P"/paski-mobow-*.jar; cp "$(ls -t "$H"/build/libs/paski-mobow-*.jar | head -1)" "$P/"
mkdir -p "$P/PaskiMobow"; cp "$H/src/main/resources/config.yml" "$P/PaskiMobow/config.yml"
rm -rf "$P/Nexo/pack/assets/paskimobow"; cp -a "$H/wynik/pack/assets/paskimobow" "$P/Nexo/pack/assets/paskimobow"
docker network inspect pm-net >/dev/null 2>&1 || docker network create --subnet ${PM_SUBNET:-10.99.47.0/24} pm-net >/dev/null   # nie 192.168.64/20 (LAN wlasciciela)
docker rm -f pm-test >/dev/null 2>&1 || true
docker run -d --name pm-test --network pm-net --memory 3400m -u 1000:1000 -v "$D/data:/data" \
  -e EULA=true -e TYPE=CUSTOM -e CUSTOM_SERVER=/data/paper-1.21.11-132.jar -e VERSION=1.21.11 \
  -e ONLINE_MODE=false -e ENABLE_RCON=true -e RCON_PASSWORD=pmtest -e MAX_MEMORY=2G -e INIT_MEMORY=1G \
  -e TZ=Europe/Warsaw -e DIFFICULTY=easy -e MODE=creative -e SPAWN_MONSTERS=false -e SPAWN_ANIMALS=false \
  -e SPAWN_NPCS=false -e GENERATE_STRUCTURES=false -e VIEW_DISTANCE=4 -e SIMULATION_DISTANCE=4 -e SPAWN_PROTECTION=0 \
  -e LEVEL_TYPE=minecraft:flat \
  -e 'GENERATOR_SETTINGS={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":3},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}' \
  itzg/minecraft-server >/dev/null
echo "start: pm-test (dane $D/data)"
