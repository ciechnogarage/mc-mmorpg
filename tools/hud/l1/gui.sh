#!/bin/bash
# Restart klienta z inna skala GUI i zrzuty spokoj + niskie HP. Uzycie: gui.sh <skala>
J=/home/dev/.claude/jobs/70c19f2b/tmp; g=$1; R="docker exec klon-hud-l1 rcon-cli"; D="docker exec -e DISPLAY=:99 mc-client-hud-l1"; r(){ $R "$@" >/dev/null; }
sed -i "s/^guiScale:.*/guiScale:$g/" $J/qa3/instance/options.txt; docker rm -f mc-client-hud-l1 >/dev/null
IP=$(docker inspect klon-hud-l1 -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
docker run -d --name mc-client-hud-l1 --memory 3g --network klon-hud-l1-net -u 1000:1000 -e HOST=$IP -v /home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client:/mc:ro -v $J/qa3:/review mc-client-test:latest bash -c 'rm -f /tmp/.X99-lock; exec bash /review/launch.sh' >/dev/null
sleep 45; $D bash -c 'xdotool mousemove 725 653; sleep .3; xdotool click --delay 200 1'
for i in $(seq 40); do $R "minecraft:list" | grep -q Notch && break; sleep 3; done; sleep 12
r "minecraft:teleport Notch 14 -58 -16 -40 8"; r "time set 3000"; sleep 3; $D import -window root /review/captures/g${g}_spokoj.png
r "difficulty easy"; r 'summon zombie 17 -59 -12 {NoAI:1b,PersistenceRequired:1b,equipment:{head:{id:"minecraft:iron_helmet",count:1}}}'; r "minecraft:effect clear Notch"
r "minecraft:damage Notch 1 minecraft:mob_attack by @e[type=zombie,limit=1]"; r "minecraft:effect give Notch minecraft:resistance 99999 4 true"
r "mmocore admin resource-stamina set Notch 2"; r "mmocore admin resource-health set Notch 3"; sleep 0.6; $D import -window root /review/captures/g${g}_niskie.png
r "minecraft:kill @e[type=zombie]"; r "difficulty peaceful"; r "mmocore admin resource-health set Notch 20"; r "mmocore admin resource-stamina set Notch 20"
echo gui$g-ok
