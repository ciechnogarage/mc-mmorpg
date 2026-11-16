scoreboard players set l1r.min l1r_pak 2147483647
execute as @e[tag=l1r_pq,tag=!l1r_usycha,scores={l1r_pak=1..},distance=..300] run scoreboard players operation l1r.min l1r_pak < @s l1r_pak
execute as @e[tag=l1r_pq,tag=!l1r_usycha,scores={l1r_pak=1..},distance=..300] if score @s l1r_pak = l1r.min l1r_pak run tag @s add l1r_usycha
