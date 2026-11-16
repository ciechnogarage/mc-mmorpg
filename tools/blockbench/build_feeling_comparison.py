"""Build technical side-by-side review sheets from captured editor frames."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
import argparse
p=argparse.ArgumentParser();p.add_argument('--revision', choices=['01','02'], default='02');args=p.parse_args()
ROOT=Path('/opt/docker/mc/data/world1/output/undead-player-v5/reviews')
OUT=ROOT/('feeling-comparison-'+args.revision)
OUT.mkdir(exist_ok=False)
variants=[('feeling-control-01','BAZA'),('feeling-subtle-'+args.revision,'SUBTELNY'),('feeling-strong-'+args.revision,'MOCNIEJSZY')]
for clip in ('idle','walk','hurt'):
 sources=[[p for p in sorted((ROOT/folder).glob(clip+'-*.png'))] for folder,_ in variants]
 if not sources[0] or len({len(x) for x in sources})!=1:raise ValueError('Missing or unequal frames')
 boxes=[Image.open(p).getbbox() for paths in sources for p in paths];boxes=[b for b in boxes if b]
 crop=(min(b[0] for b in boxes)-12,min(b[1] for b in boxes)-12,max(b[2] for b in boxes)+12,max(b[3] for b in boxes)+12)
 scale=360/(crop[3]-crop[1]);width=round((crop[2]-crop[0])*scale);cell=max(width+24,210)
 frames=[]
 for i in range(len(sources[0])):
  sheet=Image.new('RGB',(3*cell,406),(31,36,43));d=ImageDraw.Draw(sheet)
  for v,(_,label) in enumerate(variants):
   im=Image.open(sources[v][i]).convert('RGBA').crop(crop).resize((width,360),Image.Resampling.NEAREST)
   sheet.paste(im,(v*cell+(cell-width)//2,32),im);d.text((v*cell+12,10),label,fill='white')
  frames.append(sheet)
 frames[0].save(OUT/(clip+'.gif'),save_all=True,append_images=frames[1:],duration=50,loop=0)
 frames[len(frames)//4].save(OUT/(clip+'-still.png'))
(OUT/'index.html').write_text('''<!doctype html><html lang="pl"><meta charset="utf-8"><title>Nieumarły — feeling</title><style>body{background:#1f242b;color:#eee;font:18px system-ui;max-width:1100px;margin:30px auto;padding:20px}img{max-width:100%;display:block}p{line-height:1.5}a{color:#76dbd6}</style><h1>Nieumarły — porównanie feelingu</h1><p>Ta sama geometria i tekstury. Od lewej: baza, subtelny, mocniejszy. Prototypy do oceny — bez odbioru w Minecraft. Klipy pokazano osobno; nie są testem przejść.</p><h2>Chód</h2><img src="walk.gif"><h2>Bezczynność</h2><img src="idle.gif"><h2>Obrażenia</h2><img src="hurt.gif"><h2>Nowy prototyp śmierci</h2><img src="../death-repair-02-render/death.gif"><p>Skoki i upadek mają pomiar geometrii. Integracja, blending, wyposażenie i feeling FPP nadal wymagają sprawdzenia.</p></html>''')
print(OUT/'index.html')
