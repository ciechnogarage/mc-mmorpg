#!/bin/bash
# Klon testowy zaklec: Paper 1.21.11 + stos Mythic/MMO/Nexo, plaski swiat, bez portow na hosta.
docker network create zaklecia-net >/dev/null 2>&1 || true
docker rm -f zaklecia-proba >/dev/null 2>&1
docker run -d --name zaklecia-proba --network zaklecia-net --memory 3400m \
  -e EULA=true -e TYPE=PAPER -e VERSION=1.21.11 -e ONLINE_MODE=false -e MAX_MEMORY=2G -e INIT_MEMORY=1G \
  -e ENABLE_RCON=true -e RCON_PASSWORD=zaklecia -e UID=1000 -e GID=1000 -e TZ=Europe/Warsaw \
  -e LEVEL_TYPE=FLAT -e 'GENERATOR_SETTINGS={"layers":[{"block":"bedrock","height":1},{"block":"stone","height":3},{"block":"grass_block","height":1}],"biome":"plains"}' \
  -e SPAWN_PROTECTION=0 -e DIFFICULTY=normal -e MODE=survival -e SPAWN_MONSTERS=false -e SPAWN_ANIMALS=false \
  -e VIEW_DISTANCE=6 -e SKIP_SERVER_PROPERTIES=false \
  -v "$(dirname "$(readlink -f "$0")")/data:/data" itzg/minecraft-server:latest
