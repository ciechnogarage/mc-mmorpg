#!/bin/bash
# garda.sh <TYP> <ID> <nazwa> -> zrzuty gardy: przod (3. osoba), 1os (1. osoba), iskry (1. osoba, cios huska w garde)
X="docker exec -e DISPLAY=:99 mc-client-ab"; R="docker exec ab-test rcon-cli"; r(){ $R "$1" | sed 's/\x1b\[[0-9;]*m//g'; }
r "minecraft:kill @e[type=husk]" >/dev/null; r "minecraft:clear BronQA" >/dev/null; $X xdotool key 1
r "mi give $1 $2 BronQA" >/dev/null; sleep 0.5; $X xdotool key 2; sleep 0.3; $X xdotool key 1; sleep 1.2
r "minecraft:tp BronQA 0.5 -59 0.5 0 0" >/dev/null; sleep 0.4
$X xdotool key F5; sleep 0.3; $X xdotool key F5; sleep 0.4; $X xdotool mousedown 3; sleep 1.0
$X import -window root /review/captures/$3-przod.png; $X xdotool key F5; sleep 0.5
$X import -window root /review/captures/$3-1os.png
r "minecraft:summon husk 0.5 -59 2.2 {NoAI:1b,Silent:1b,Tags:[\"a\"],attributes:[{id:\"minecraft:max_health\",base:1000}],Health:1000f}" >/dev/null; sleep 0.4
r "minecraft:damage BronQA 4 minecraft:mob_attack by @e[tag=a,limit=1]" >/dev/null; sleep 0.05
$X import -window root /review/captures/$3-iskry.png
r "minecraft:kill @e[type=husk]" >/dev/null; $X xdotool mouseup 3; sleep 0.5
