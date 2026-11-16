#!/bin/bash
# klient QA (obraz mc-client-test): Xvfb + vanilla 1.21.11 prosto na pm-test
set -euo pipefail
export DISPLAY=:99
Xvfb :99 -screen 0 1280x720x24 -nolisten tcp > /review/xvfb.log 2>&1 &
for attempt in {1..30}; do xdpyinfo >/dev/null 2>&1 && break; sleep 0.2; done
CP=$(cat /mc/classpath.txt); MAIN=$(cat /mc/main_class.txt); ASSET_INDEX=$(cat /mc/asset_index_id.txt)
NAME=PasekQA
UUID=$(python3 -c 'import uuid,hashlib; print(uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:PasekQA").digest(),version=3))')
mkdir -p /review/instance /review/native-tmp /review/captures
printf 'resourcePacks:["vanilla","server"]\nguiScale:2\nfov:0.0\npauseOnLostFocus:false\nonboardAccessibility:false\ntutorialStep:none\n' > /review/instance/options.txt
cd /review/instance
exec java -Xmx2G -Djava.io.tmpdir=/review/native-tmp -Djava.library.path=/mc/natives -Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true \
  -cp "$CP" "$MAIN" --username "$NAME" --version 1.21.11 \
  --gameDir /review/instance --assetsDir /mc/assets --assetIndex "$ASSET_INDEX" \
  --uuid "$UUID" --accessToken 0 --userType legacy --versionType release \
  --width 1280 --height 720 --quickPlayMultiplayer pm-test:25565 \
  > /review/instance/client.log 2>&1
