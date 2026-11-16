data merge entity @s {IsBaby:1b}
scoreboard players add l1r.nr l1r_pak 1
scoreboard players operation @s l1r_pak = l1r.nr l1r_pak
execute store result score l1r.ile l1r_pak if entity @e[tag=l1r_pq,tag=!l1r_usycha,scores={l1r_pak=1..},distance=..300]
execute if score l1r.ile l1r_pak matches 13.. run function l1r:najstarszy
