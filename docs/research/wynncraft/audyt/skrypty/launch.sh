#!/bin/bash
set -euo pipefail
export DISPLAY=:99
Xvfb :99 -screen 0 1280x720x24 -nolisten tcp > /review/xvfb.log 2>&1 &
for attempt in {1..30}; do xdpyinfo >/dev/null 2>&1 && break; sleep 0.2; done
CP=$(cat /mc/classpath.txt)
MAIN=$(cat /mc/main_class.txt)
ASSET_INDEX=$(cat /mc/asset_index_id.txt)
NAME=WynnAuditPL
UUIDV=$(python3 -c 'import uuid,hashlib; print(uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:WynnAuditPL").digest(),version=3))')
cd /review/instance
exec java -Xmx2G -Djava.io.tmpdir=/review/native-tmp -Djava.library.path=/mc/natives -Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true \
  -cp "$CP" "$MAIN" --username "$NAME" --version 1.21.11 \
  --gameDir /review/instance --assetsDir /mc/assets --assetIndex "$ASSET_INDEX" \
  --uuid "$UUIDV" --accessToken 0 --userType legacy --versionType release \
  --width 1280 --height 720 --quickPlayMultiplayer play.wynncraft.com \
  > /review/instance/client.log 2>&1
