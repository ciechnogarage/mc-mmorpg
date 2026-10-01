#!/bin/bash
# scena.sh -> rzad mobow kazdej rzadkosci przed PasekQA (NoAI), statusy, cios w jednego (duch), zrzuty w /review/captures.
# Wymaga: pm-test + mc-client-pm (gracz PasekQA na serwerze, tryb kreatywny).
X="docker exec -e DISPLAY=:99 mc-client-pm"; R="docker exec pm-test rcon-cli"
$R "minecraft:time set noon" >/dev/null; $R "minecraft:weather clear" >/dev/null
$R "minecraft:kill @e[type=!player]" >/dev/null
$R "minecraft:tp PasekQA 0.5 -60 -6.5 0 5" >/dev/null
i=0
for m in L001_PNIACZEK BEAST_DIRE_WOLF ELITE_ALPHA_WOLF WIEZA_P01_ALPHA BOSS_CRYPT_LORD; do
  x=$(( -8 + i * 4 )); $R "mm mobs spawn $m 1 world,$x.5,-60,4.5,180,0" >/dev/null; i=$((i+1))
done
$R "minecraft:summon zombie 12.5 -60 4.5 {Rotation:[180f,0f]}" >/dev/null
sleep 1
$R "minecraft:execute as @e[type=!player] run data merge entity @s {NoAI:1b}" >/dev/null
sleep 2
shot(){ $X import -window root /review/captures/$1.png; }
shot a-pelne
$R "minecraft:effect give @e[type=wolf] minecraft:poison 30 0" >/dev/null
$R "minecraft:effect give @e[type=wither_skeleton] minecraft:slowness 30 0" >/dev/null
$R "minecraft:effect give @e[type=wither_skeleton] minecraft:strength 30 0" >/dev/null
$R "minecraft:damage @e[type=wither_skeleton,limit=1] 400 minecraft:generic" >/dev/null
$R "minecraft:damage @e[type=zombie,limit=1] 8 minecraft:generic" >/dev/null
sleep 0.3; shot b-duch
sleep 2.5; shot c-po-duchu
