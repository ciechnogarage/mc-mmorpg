#!/usr/bin/env python3
"""Nexo item icons for each map waypoint, copied from the Mapa icon source."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance
out=Path(__file__).resolve().parent.parent/'portal-ikony'
out.mkdir(parents=True,exist_ok=True)
source=Path('/opt/docker/mc/data/world1/plugins/Mapa/ikony')
names={
 'start':'campfire',
 'szepczacy_gaj':'chw_pedzik',
 'wilcze_ostepy':'chw_wilkor',
 'swietlisty_gaszcz':'chw_promiennik',
 'wietrzne_turnie':'chw_wichrzan',
 'perlowa_zatoka':'chw_muszlik',
 'rdzawe_wawozy':'chw_glazogrzbiet',
 'wygasle_palenisko':'chw_iskrzyk',
 'nexus':'ender_eye',
}
lines=['# Ikony kolorowe i szare sylwetki Portalu Q; źródło: Mapa/ikony.']
for id, filename in names.items():
 color=Image.open(source/(filename+'.png')).convert('RGBA')
 assert color.size==(16,16)
 color.save(out/(id+'.png'))
 locked=ImageEnhance.Brightness(ImageEnhance.Color(color).enhance(0)).enhance(.8)
 # A small brass padlock makes the state readable even on dark map terrain.
 draw=ImageDraw.Draw(locked)
 draw.rectangle((8,8,15,15),fill=(32,39,45,255),outline=(245,198,87,255))
 draw.arc((9,4,14,12),180,360,fill=(245,198,87,255),width=2)
 draw.rectangle((10,10,13,13),fill=(206,145,53,255))
 draw.point((11,11),fill=(32,39,45,255))
 locked.save(out/(id+'_locked.png'))
 for suffix in ('','_locked'):
  item='portal_wp_'+id+suffix
  lines.extend([
   item+':',
   "  itemname: '<gray>Punkt podróży'",
   '  material: PAPER',
   '  excludeFromInventory: true',
   '  excludeFromCommands: true',
   '  Pack:',
   '    texture: nexo:item/portal/'+id+suffix,
   '',
  ])
(out.parent/'portal_waypoints.yml').write_text('\n'.join(lines),encoding='utf-8')
print('Nexo:',len(names)*2,'ikon')
