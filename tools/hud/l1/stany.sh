#!/bin/bash
# Przejscie przez stany HUD L1 na klonie; zrzuty do qa3/captures/<prefiks>_<stan>.png
P=${1:-g3}; R="docker exec klon-hud-l1 rcon-cli"; D="docker exec -e DISPLAY=:99 mc-client-hud-l1"
shot(){ sleep ${2:-3}; $D import -window root /review/captures/${P}_$1.png; }
r(){ $R "$@" >/dev/null; }
r "time set 3000"; r "minecraft:effect clear Notch"; r "minecraft:effect give Notch minecraft:resistance 99999 4 true"; r "mmocore admin class Notch HUMAN"
r "mmocore admin level set Notch main 37"; r "minecraft:teleport Notch 14 -58 -16 -40 8"
shot spokoj 4
r "minecraft:effect give Notch minecraft:poison 60 0 true"; r "mmocore admin resource-health set Notch 14"; shot trucizna; r "minecraft:effect clear Notch minecraft:poison"
r "minecraft:effect give Notch minecraft:wither 60 0 true"; shot uschniecie; r "minecraft:effect clear Notch minecraft:wither"
r "minecraft:effect give Notch minecraft:unluck 180 0 true"; shot rany; r "minecraft:effect clear Notch minecraft:unluck"
r "mmocore admin resource-health set Notch 20"
# walka: obrazenie od moba wlacza in_combat, potem niskie HP i niska stamina
r "difficulty easy"; r 'summon zombie 17 -59 -12 {NoAI:1b,PersistenceRequired:1b,equipment:{head:{id:"minecraft:iron_helmet",count:1}}}'
r "minecraft:effect clear Notch minecraft:resistance"; r "minecraft:damage Notch 1 minecraft:mob_attack by @e[type=zombie,limit=1]"
r "minecraft:effect give Notch minecraft:resistance 99999 4 true"; r "mmocore admin resource-health set Notch 3"; r "mmocore admin resource-stamina set Notch 2"
echo "walka: $($R 'papi parse Notch %mmocore_in_combat% %player_health%' | head -1)"
shot walka 2
r "minecraft:kill @e[type=zombie]"; r "difficulty peaceful"; r "mmocore admin resource-health set Notch 20"; r "mmocore admin resource-stamina set Notch 20"
for i in $(seq 20); do [ "$($R 'papi parse Notch %mmocore_in_combat%' | head -1 | tr -dc a-z)" = false ] && break; sleep 1; done
# zasoby klas
r "mmocore admin class Notch SWIETY_AKOLITA"; r "mmocore admin resource-mana set Notch 3"; shot akolita
r "mmocore admin class Notch TYRAN"; r "mmocore admin resource-mana set Notch 40"; shot szal
r "mmocore admin class Notch LOWCA_KOSCI"; r "mmocore admin resource-mana set Notch 60"; shot skupienie
r "mmocore admin class Notch HUMAN"
# woda
r "fill 10 -59 -20 18 -55 -12 minecraft:water"; r "minecraft:teleport Notch 14 -58 -16 -40 8"; shot woda 9
r "fill 10 -59 -20 18 -55 -12 minecraft:air replace minecraft:water"
# poziom 100 i kompakt
r "mmocore admin level set Notch main 100"; shot poziom100
r "lp user Notch meta set ui_hud compact"; shot kompakt 6; r "lp user Notch meta unset ui_hud"
r "lp user Notch meta set ui_hud off"; shot wylaczone 6; r "lp user Notch meta unset ui_hud"
r "mmocore admin level set Notch main 37"
echo stany-ok
