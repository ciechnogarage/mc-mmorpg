#!/bin/bash
# efekty.sh <TYP> <ID> <nazwa> -> zrzuty: t1/t2 = trafienie w huska (efekt wg rodzaju obrazen), p = poza PPM z przodu
X="docker exec -e DISPLAY=:99 mc-client-ab"; R="docker exec ab-test rcon-cli"
r(){ $R "$1" | sed 's/\x1b\[[0-9;]*m//g'; }
r "minecraft:kill @e[type=husk]" >/dev/null; r "minecraft:clear BronQA" >/dev/null; $X xdotool key 1
r "mi give $1 $2 BronQA" >/dev/null; sleep 0.5; $X xdotool key 2; sleep 0.3; $X xdotool key 1; sleep 1
r "minecraft:summon husk 0.5 -59 3.2 {NoAI:1b,Silent:1b,Tags:[\"t\"],attributes:[{id:\"minecraft:max_health\",base:500}],Health:500f}" >/dev/null
r "minecraft:tp BronQA 0.5 -59 0.5 0 12" >/dev/null; sleep 1.8
$X xdotool click 1; sleep 0.05; $X import -window root /review/captures/$3-t1.png; $X import -window root /review/captures/$3-t2.png
r "minecraft:kill @e[type=husk]" >/dev/null; sleep 0.8
$X xdotool key F5; sleep 0.2; $X xdotool key F5; sleep 0.5; $X xdotool mousedown 3; sleep 0.9
$X import -window root /review/captures/$3-p.png; $X xdotool mouseup 3; $X xdotool key F5; sleep 0.4
