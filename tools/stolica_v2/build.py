#!/usr/bin/env python3
"""Build the second, isolated capital candidate from auditable block geometry.

The script writes a datapack only. It never edits an existing world directly.
"""

from __future__ import annotations

import math
import random
import sys
from pathlib import Path

sys.path.insert(0, '/opt/docker/mc/forge/tools/budowle')
from struktura import zapisz
from iob import czytaj

ROOT = Path('/opt/docker/mc/data/world1/world/datapacks/stolica_odnowa')
STRUCTURES = ROOT / 'data/stolica_odnowa/structure'
FUNCTIONS = ROOT / 'data/stolica_odnowa/function'
WORLD = 'minecraft:stolica_odnowa'
B: dict[tuple[int, int, int], str] = {}
R = random.Random(93281)


def p(x, y, z, b):
    B[round(x), round(y), round(z)] = 'minecraft:' + b if ':' not in b else b


def box(x1, y1, z1, x2, y2, z2, b):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                p(x, y, z, b)


def disk(cx, y, cz, rad, b):
    for x in range(cx-rad, cx+rad+1):
        for z in range(cz-rad, cz+rad+1):
            if (x-cx)**2 + (z-cz)**2 <= rad*rad:
                p(x, y, z, b)


def ring(cx, y, cz, rad, thick, b):
    for x in range(cx-rad, cx+rad+1):
        for z in range(cz-rad, cz+rad+1):
            d = math.hypot(x-cx, z-cz)
            if rad-thick <= d <= rad:
                p(x, y, z, b)


def path(points, width=3, material='stone_bricks'):
    for (x1,z1),(x2,z2) in zip(points,points[1:]):
        steps=max(abs(x2-x1),abs(z2-z1))*2+1
        for i in range(steps+1):
            t=i/steps
            x=round(x1+(x2-x1)*t); z=round(z1+(z2-z1)*t)
            disk(x,100,z,width,material)


def island():
    # Asymmetric coast, deep underside, small rocky outcrops and a quiet north cove.
    for x in range(-84,85):
        for z in range(-84,85):
            a=math.atan2(z,x)
            d=math.hypot(x,z)
            edge=75+3.5*math.sin(a*5+0.3)+2.4*math.sin(a*9-0.9)+1.2*math.sin(a*17)
            if d>edge: continue
            top=100 if d<66 else 100-max(0,int((d-65)/3))
            bottom=max(64, int(64+29*(d/edge)**1.7))
            for y in range(bottom,top+1):
                if y==top: b='grass_block' if d<68 else 'moss_block'
                elif y>=top-3: b='dirt'
                elif y<78: b=R.choice(['deepslate']*5+['tuff'])
                else: b=R.choice(['stone']*5+['andesite','tuff'])
                p(x,y,z,b)
    for x,z,length in [(-44,-23,19),(38,36,17),(-12,21,23),(24,-50,21),(-57,18,12)]:
        for y in range(64-length,65):
            disk(x,y,z,max(0,(y-(64-length))//6),'deepslate')
    # Coast wall follows the irregular cliff at a safe, visibly built distance.
    for deg in range(360):
        a=math.radians(deg)
        r=69+2*math.sin(a*5+0.3)
        x=round(r*math.cos(a)); z=round(r*math.sin(a))
        p(x,101,z,'tuff_brick_wall')
        if deg%15==0:
            box(x,101,z,x,103,z,'polished_andesite')
            p(x,104,z,'lantern')


def trees_and_gardens():
    # Existing Iris objects from the project have natural, uneven branching.
    source=Path('/opt/docker/mc/data/world1/plugins/Iris/packs/overworld/objects/trees/oak')
    for i,(x,z) in enumerate([(-59,47),(-61,-46),(-21,-60),(59,-52),(61,43),
                              (-19,61),(19,59),(-19,-12),(19,-18),(-58,-3),
                              (57,1),(-3,-65),(30,65)]):
        name=('truegeneric14','hoakgeneric3','truegeneric14')[i%3]
        obj=czytaj(source/f'{name}.iob')['bloki']
        min_y=min(y for _,y,_ in obj)
        for (dx,dy,dz),b in obj.items():
            if 'player_' in b or b.endswith(':air'):continue
            if 'leaves' in b and i%3==1:
                b='minecraft:flowering_azalea_leaves[persistent=true]'
            p(x+dx,101+dy-min_y,z+dz,b)
    for x,z in [(-52,56),(-11,54),(48,55),(59,-15),(-57,-28),(8,-58)]:
        disk(x,100,z,5,'moss_block')
        for dx,dz in [(-2,1),(2,-1),(0,3),(-3,-1)]:
            p(x+dx,101,z+dz,'flowering_azalea')


def streets():
    # The main street narrows toward the Nexus. District roads branch naturally.
    path([(0,62),(0,49),(2,35),(0,21),(0,13)],4,'cobbled_deepslate')
    ring(0,100,0,25,5,'stone_bricks')
    path([(-22,17),(-32,20),(-46,17),(-54,7)],3,'mud_bricks')
    path([(-19,18),(-36,31),(-43,37)],3,'mud_bricks')
    path([(-29,33),(-31,43),(-31,52)],2,'stone_bricks')
    path([(21,17),(33,21),(46,17),(54,10)],3,'tuff_bricks')
    path([(22,15),(30,34),(36,48)],3,'tuff_bricks')
    path([(-19,-17),(-32,-26),(-43,-33)],3,'stone_bricks')
    path([(17,-19),(31,-28),(43,-35)],3,'mossy_stone_bricks')
    path([(0,-22),(0,-35),(0,-58)],4,'polished_diorite')
    path([(-20,-22),(-25,-41),(-12,-53)],2,'stone_bricks')
    path([(20,-20),(27,-43),(11,-55)],2,'stone_bricks')
    path([(-35,-17),(-50,-18),(-57,-18)],2,'mud_bricks')
    path([(37,-17),(51,-15),(57,-15)],2,'tuff_bricks')
    path([(-7,43),(-14,48)],2,'stone_bricks')
    path([(7,53),(15,58)],2,'stone_bricks')
    path([(-6,-53),(-15,-57)],2,'polished_diorite')
    path([(6,-53),(15,-57)],2,'polished_diorite')
    # Irregular paving, never a whole-island grey cross.
    for x,z in list((x,z) for x in range(-60,61) for z in range(-60,61) if B.get((x,100,z)) in
                    {'minecraft:stone_bricks','minecraft:mud_bricks','minecraft:tuff_bricks','minecraft:cobbled_deepslate'}):
        if (x*37+z*73)%17==0:
            p(x,100,z,'cracked_stone_bricks' if x<10 else 'polished_andesite')
    # Arrival pier and market quay.
    box(-8,100,55,8,100,66,'spruce_planks')
    for x in (-8,8):
        for z in (55,61,66):
            box(x,97,z,x,101,z,'dark_oak_log[axis=y]')
            p(x,102,z,'lantern')
    box(-6,100,52,6,100,58,'polished_andesite')


def hall(cx,cz,w,d,roof,wall='mud_bricks',door='south',height=7):
    x1,x2=cx-w//2,cx+w//2
    z1,z2=cz-d//2,cz+d//2
    box(x1,101,z1,x2,101,z2,'spruce_planks')
    for y in range(102,102+height):
        for x in range(x1,x2+1):
            p(x,y,z1,wall); p(x,y,z2,wall)
        for z in range(z1,z2+1):
            p(x1,y,z,wall); p(x2,y,z,wall)
    for x in (x1,x2):
        for z in (z1,z2):
            box(x,102,z,x,104+height,z,'stripped_dark_oak_wood')
    # Proper steep gables instead of a flat concentric lid. Roof axis follows
    # the entry orientation, so neighboring silhouettes are not identical.
    clean=roof.split('[')[0]
    roof_parts={
        'dark_oak_planks':('dark_oak_stairs','dark_oak_slab'),
        'deepslate_tiles':('deepslate_tile_stairs','deepslate_tile_slab'),
        'waxed_exposed_cut_copper':('waxed_exposed_cut_copper_stairs','waxed_exposed_cut_copper_slab'),
        'waxed_weathered_cut_copper':('waxed_weathered_cut_copper_stairs','waxed_weathered_cut_copper_slab'),
        'mossy_stone_bricks':('mossy_stone_brick_stairs','mossy_stone_brick_slab'),
    }
    stair,slab=roof_parts.get(clean,('dark_oak_stairs','dark_oak_slab'))
    if door in ('south','north'):
        for h in range((d+3)//2):
            zl=z1-2+h;zr=z2+2-h;yy=109+h
            for x in range(x1-2,x2+3):
                if zl<zr:
                    p(x,yy,zl,f'{stair}[facing=south,half=bottom]')
                    p(x,yy,zr,f'{stair}[facing=north,half=bottom]')
                else:p(x,yy,zl,f'{slab}[type=top]')
            for x in (x1,x2):
                for z in range(zl+1,zr):
                    if (z-cz)**2<(max(1,(d//2-h)))**2:p(x,yy-1,z,wall)
    else:
        for h in range((w+3)//2):
            xl=x1-2+h;xr=x2+2-h;yy=109+h
            for z in range(z1-2,z2+3):
                if xl<xr:
                    p(xl,yy,z,f'{stair}[facing=east,half=bottom]')
                    p(xr,yy,z,f'{stair}[facing=west,half=bottom]')
                else:p(xl,yy,z,f'{slab}[type=top]')
            for z in (z1,z2):
                for x in range(xl+1,xr):
                    if (x-cx)**2<(max(1,(w//2-h)))**2:p(x,yy-1,z,wall)
    # Chimney and small roof windows make service buildings recognizable.
    chimney_x=x1+4 if door in ('south','north') else x2-4
    chimney_z=cz-2 if door in ('south','north') else z1+4
    box(chimney_x,112,chimney_z,chimney_x+1,118,chimney_z+1,'stone_bricks')
    p(chimney_x,119,chimney_z,'campfire[lit=true]')
    # Doors are explicit air so a rebuild remains walkable.
    for q in (-1,0,1):
        x,z={'south':(cx+q,z2),'north':(cx+q,z1),
             'east':(x2,cz+q),'west':(x1,cz+q)}[door]
        for y in (102,103,104): p(x,y,z,'air')
    for x in (x1+3,x2-3):
        for z in (z1,z2):
            if B.get((x,102,z))!='minecraft:air':
                p(x,104,z,'glass_pane'); p(x,105,z,'glass_pane')
    for z in (z1+3,z2-3):
        for x in (x1,x2):
            if B.get((x,102,z))!='minecraft:air':
                p(x,104,z,'glass_pane'); p(x,105,z,'glass_pane')
    for x in range(x1+4,x2-3,6):
        p(x,108,cz,'lantern')
    return x1,x2,z1,z2


def pavilion(cx,cz,rad,roof='waxed_weathered_cut_copper'):
    disk(cx,101,cz,rad,'polished_andesite')
    for i in range(8):
        a=2*math.pi*i/8
        x=round(cx+(rad-1)*math.cos(a)); z=round(cz+(rad-1)*math.sin(a))
        box(x,102,z,x,108,z,'polished_andesite')
        p(x,109,z,'sea_lantern')
    for h in range(4):
        disk(cx,110+h,cz,rad+1-h,roof)
    p(cx,114,cz,'lightning_rod')


def nexus():
    disk(0,101,0,14,'polished_blackstone_bricks')
    ring(0,101,0,16,2,'chiseled_tuff_bricks')
    ring(0,101,0,11,1,'amethyst_block')
    disk(0,101,0,5,'crying_obsidian')
    for x,z in [(-9,-9),(-9,9),(9,-9),(9,9)]:
        box(x,102,z,x,114,z,'polished_blackstone_bricks')
        box(x-1,102,z-1,x+1,103,z+1,'chiseled_tuff_bricks')
        p(x,115,z,'sea_lantern')
        p(x,116,z,'lightning_rod')
    for y in range(114,119):
        r=10-(y-114)*2
        ring(0,y,0,r,1,'polished_blackstone_bricks')
    box(-2,119,-2,2,121,2,'amethyst_block')
    p(0,122,0,'sea_lantern')
    # Two visually distinct closed pylons and one live dungeon gate.
    for cx,cz,accent in [(-12,0,'cracked_deepslate_bricks'),(12,0,'cracked_deepslate_bricks'),
                         (0,13,'flowering_azalea_leaves[persistent=true]')]:
        for dx in (-2,2):
            box(cx+dx,102,cz,cx+dx,109,cz,'polished_blackstone_bricks')
        box(cx-2,110,cz,cx+2,111,cz,'chiseled_tuff_bricks')
        p(cx,107,cz,accent)


def market():
    hall(-46,20,21,17,'waxed_exposed_cut_copper','mud_bricks','east')
    hall(-52,-2,19,15,'dark_oak_planks','stone_bricks','east')
    hall(-51,37,17,13,'dark_oak_planks','mud_bricks','east')
    # Three separate storefronts with awnings and counters.
    for z,wool in [(8,'red_wool'),(19,'orange_wool'),(30,'yellow_wool')]:
        box(-27,102,z-3,-27,106,z+3,'dark_oak_fence')
        box(-31,107,z-4,-23,107,z+4,wool)
        box(-29,102,z-3,-25,102,z-3,'spruce_slab[type=top]')
        p(-26,103,z,'lantern')
    # Forge/repair/deconstruction visual anchors.
    box(-43,102,34,-43,104,34,'deepslate_bricks')
    p(-43,105,34,'lava')
    p(-39,102,34,'anvil')
    p(-35,102,34,'grindstone[face=floor,facing=north]')
    for x in (-56,-52,-48):
        p(x,102,-2,'barrel[facing=up]')
    p(-52,103,-2,'ender_chest')


def crafts():
    # Six shops around a working court: the 13 real runtime professions share it.
    hall(48,20,21,17,'deepslate_tiles','tuff_bricks','west')
    hall(50,-3,17,13,'dark_oak_planks','mud_bricks','west')
    hall(37,47,17,13,'waxed_weathered_cut_copper','mud_bricks','north')
    for x,z,b in [(28,12,'anvil'),(31,12,'smithing_table'),(35,12,'blast_furnace[facing=south]'),
                  (29,22,'brewing_stand'),(34,22,'enchanting_table'),(39,22,'loom[facing=south]'),
                  (47,29,'crafting_table'),(51,29,'smoker[facing=west]'),(47,34,'stonecutter[facing=west]'),
                  (52,34,'cartography_table'),(33,31,'composter'),(42,5,'fletching_table'),
                  (55,5,'barrel[facing=up]')]: p(x,102,z,b)
    for x,z in [(33,35),(37,35),(41,35)]: p(x,102,z,'hay_block')
    # Every public profession has a real counter, not just a villager on grass.
    counters=[(26,7,'gray_wool'),(34,7,'brown_wool'),(24,24,'green_wool'),
              (27,32,'yellow_wool'),(36,34,'blue_wool'),(41,6,'red_wool'),
              (55,6,'purple_wool'),(28,17,'black_wool'),(28,27,'magenta_wool'),
              (52,32,'orange_wool'),(55,36,'light_gray_wool')]
    for cx,cz,canvas in counters:
        for dx in (-2,2):
            for dz in (-2,2):
                box(cx+dx,102,cz+dz,cx+dx,105,cz+dz,'dark_oak_fence')
        box(cx-2,106,cz-2,cx+2,106,cz+2,canvas)
        p(cx,105,cz,'lantern')
    # Small quarry and herb beds make gathering visible, without granting free resources.
    for x,z,b in [(59,25,'iron_ore'),(61,25,'copper_ore'),(59,27,'coal_ore'),
                  (25,28,'moss_block'),(25,30,'flowering_azalea'),(27,30,'fern')]: p(x,101,z,b)


def north():
    hall(-44,-34,25,21,'deepslate_tiles','stone_bricks','east')
    hall(43,-35,21,19,'mossy_stone_bricks','mud_bricks','west')
    hall(0,-49,31,21,'waxed_weathered_cut_copper','calcite','south',9)
    disk(0,101,-38,9,'polished_diorite')
    # Wiary has three Elders and twelve race gods. The four outer plinths
    # represent races; the menu reveals the correct three gods for the player.
    shrines=[(-9,-55,'rooted_dirt'),(0,-55,'cobweb'),(9,-55,'wither_rose'),
             (-9,-35,'sea_lantern'),(-3,-33,'flowering_azalea'),
             (3,-33,'redstone_block'),(9,-35,'soul_lantern')]
    for x,z,b in shrines:
        box(x,102,z,x,104,z,'quartz_pillar[axis=y]')
        p(x,105,z,b)
    for x,z in [(-46,-22),(-37,-22),(-46,-45),(-37,-45)]:
        box(x,102,z,x,106,z,'polished_andesite')
        p(x,107,z,'lantern')
    # Four class heraldic bays match the four classes currently in the creator.
    for z,cloth in [(-43,'red_wool'),(-38,'purple_wool'),(-33,'yellow_wool'),(-28,'black_wool')]:
        box(-29,102,z,-29,105,z,'polished_deepslate')
        box(-29,106,z,-29,108,z,cloth)
        p(-28,102,z,'chiseled_stone_bricks')
    # Pet garden, intentionally outside the class hall.
    disk(42,101,-51,8,'moss_block')
    for x,z in [(40,-53),(45,-55),(39,-48),(47,-48)]: p(x,102,z,'flowering_azalea')
    p(42,102,-51,'hay_block')


def south():
    # Arrival arch frames the road and the much taller Nexus beyond it.
    for x in (-6,6):
        box(x,101,50,x,108,50,'stripped_dark_oak_wood')
        box(x-1,101,49,x+1,102,51,'stone_bricks')
        p(x,106,51,'lantern')
    box(-6,109,50,6,110,50,'dark_oak_planks')
    box(-3,111,50,3,111,50,'waxed_exposed_cut_copper')
    p(0,112,50,'sea_lantern')
    hall(39,48,19,17,'waxed_exposed_cut_copper','stone_bricks','west')
    # Real practice court: open, fenced, with clear line of sight to the road.
    disk(-31,101,52,9,'polished_andesite')
    ring(-31,102,52,9,1,'stone_brick_wall')
    for z in (50,51,52,53,54): p(-22,102,z,'air')
    for x,z in [(-35,51),(-29,48),(-27,55)]:
        box(x,102,z,x,104,z,'dark_oak_fence')
        p(x,105,z,'target')
    # Physical notice board beside arrival street.
    box(13,102,38,13,106,46,'dark_oak_planks')
    for z in (40,42,44): p(12,104,z,'chiseled_bookshelf')
    for x,z in [(-11,37),(10,51),(-19,13),(18,13),(-18,-13),(17,-14)]:
        box(x,101,z,x,104,z,'polished_andesite')
        p(x,105,z,'lantern')


def smaller_places():
    # Residential/service fabric closes the empty gaps between civic landmarks.
    hall(-58,-18,11,9,'dark_oak_planks','mud_bricks','east')
    hall(58,-15,11,9,'waxed_weathered_cut_copper','tuff_bricks','west')
    hall(-15,48,9,9,'dark_oak_planks','stone_bricks','east')
    hall(15,58,9,9,'waxed_exposed_cut_copper','stone_bricks','west')
    hall(-16,-57,9,9,'deepslate_tiles','stone_bricks','east')
    hall(16,-57,9,9,'waxed_weathered_cut_copper','calcite','west')
    for x,z in [(-58,-18),(58,-15),(-15,48),(15,58)]:
        p(x,102,z,'barrel[facing=up]')
        p(x+2,102,z+2,'crafting_table')
    for x,z in [(-16,-57),(16,-57)]:
        p(x,102,z,'bookshelf')
        p(x+2,102,z+2,'lectern[facing=south,has_book=false]')


def main():
    B.clear()
    island(); streets(); nexus(); market(); crafts(); north(); south(); smaller_places(); trees_and_gardens()
    STRUCTURES.mkdir(parents=True,exist_ok=True)
    FUNCTIONS.mkdir(parents=True,exist_ok=True)
    (ROOT/'pack.mcmeta').write_text('{"pack":{"description":"Stolica Wyspy v2 — dzielnice i uslugi","min_format":[94,1],"max_format":[94,1]}}\n')
    commands=[]
    for ty in (32,72,112):
        for tx in range(-96,96,32):
            for tz in range(-96,96,32):
                tile={(x,y,z):b for (x,y,z),b in B.items()
                      if tx<=x<tx+32 and tz<=z<tz+32 and ty<=y<ty+40}
                if not tile: continue
                name=f'tile_{tx+96}_{tz+96}_{ty}'
                info=zapisz(tile,STRUCTURES/f'{name}.nbt',4671)
                x0,y0,z0=info['offset']
                commands.append(f'execute in {WORLD} run place template stolica_odnowa:{name} {x0} {y0} {z0}')
    signs=[
        (0,109,51,'south','STOLICA','NEXUS PROSTO'),
        (-35,104,20,'east','RYNEK','HANDEL'),
        (37,104,20,'west','WARSZTATY','13 PROFESJI'),
        (-30,104,-34,'east','POSTAC','UMIEJETNOSCI'),
        (32,104,-35,'west','CHOWANCE','MENAZERIA'),
        (0,104,-38,'south','SWIATYNIA','WIARA'),
        (28,104,48,'west','DRUZYNY','GILDIE'),
        (-12,102,2,'standing','WYSPY','TRASA ZAMKNIETA'),
        (12,102,2,'standing','KOLEJNE LOCHY','ZAMKNIETE'),
        (-9,102,-52,'standing','ORDHUN','PIERWSZY KORZEN'),
        (0,102,-52,'standing','NYSARA','TKACZKA LOSU'),
        (9,102,-52,'standing','VHARUN','SAD UMARLYCH'),
        (-9,102,-31,'standing','CZLOWIEK','PATRONI RASY'),
        (-3,102,-29,'standing','ELF','PATRONI RASY'),
        (3,102,-29,'standing','ORK','PATRONI RASY'),
        (9,102,-31,'standing','NIEUMARLY','PATRONI RASY'),
    ]
    for x,y,z,facing,title,sub in signs:
        block=('minecraft:oak_sign[rotation=0]' if facing=='standing'
               else f'minecraft:oak_wall_sign[facing={facing}]')
        commands.append(f'execute in {WORLD} run setblock {x} {y} {z} {block}')
        nbt=('{front_text:{has_glowing_text:1b,messages:['
             f"'{{\"text\":\"{title}\",\"color\":\"gold\",\"bold\":true}}',"
             f"'{{\"text\":\"{sub}\",\"color\":\"white\"}}',"
             "'\"\"','\"\"']}}")
        commands.append(f'execute in {WORLD} run data merge block {x} {y} {z} {nbt}')
    (FUNCTIONS/'build.mcfunction').write_text('\n'.join(commands)+'\n')
    print(f'{len(B)} blocks, {len(commands)} tiles')


if __name__=='__main__': main()
