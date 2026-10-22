#!/bin/bash
# Wgraj paczke L1 na klon hud-l1, restart, podlacz klienta (Notch), ustaw scene. Uzycie: cykl.sh
J=/home/dev/.claude/jobs/70c19f2b/tmp; K=/opt/docker/mc/klony/hud-l1/data/plugins/MythicHUD; LOG=/opt/docker/mc/klony/hud-l1/data/logs/latest.log
docker rm -f mc-client-hud-l1 >/dev/null 2>&1
docker stop klon-hud-l1 >/dev/null
rm -rf $K/source-pack/assets/mythichud/textures/assets/l1; cp -r $J/l1/out/MythicHUD/. $K/
I0=$(stat -c %i $LOG); docker start klon-hud-l1 >/dev/null
for i in $(seq 150); do I1=$(stat -c %i $LOG 2>/dev/null); [ "$I1" != "$I0" ] && grep -q 'Done (' $LOG && break; sleep 4; done
grep -c 'Error occurred while enabling MythicHUD' $LOG | sed 's/^/bledy MythicHUD: /'
sleep 15; R="docker exec klon-hud-l1 rcon-cli"; IP=$(docker inspect klon-hud-l1 -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
$R "difficulty peaceful" >/dev/null; $R "gamerule fallDamage false" >/dev/null; $R "gamerule doDaylightCycle false" >/dev/null; $R "time set 3000" >/dev/null
docker run -d --name mc-client-hud-l1 --memory 3g --network klon-hud-l1-net -u 1000:1000 -e HOST=$IP -v /home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client:/mc:ro -v $J/qa3:/review mc-client-test:latest bash -c 'rm -f /tmp/.X99-lock; exec bash /review/launch.sh' >/dev/null
sleep 45; D="docker exec -e DISPLAY=:99 mc-client-hud-l1"; $D bash -c 'xdotool mousemove 725 653; sleep .3; xdotool click --delay 200 1'
for i in $(seq 40); do $R "minecraft:list" | grep -q Notch && break; sleep 3; done; sleep 12
$R "op Notch" >/dev/null; $R "gamemode survival Notch" >/dev/null; $R "effect give Notch minecraft:resistance 99999 4 true" >/dev/null
$R "minecraft:teleport Notch 14 -58 -16 -40 8" >/dev/null; $R "clear Notch" >/dev/null; $R "give Notch minecraft:iron_sword" >/dev/null
echo gotowe
