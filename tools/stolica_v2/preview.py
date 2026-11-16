#!/usr/bin/env python3
"""Top-down inspection image from exactly the blocks deployed by build.py."""
from pathlib import Path
from PIL import Image, ImageDraw
import build

build.main()
top={}
for (x,y,z),b in build.B.items():
    if (x,z) not in top or top[(x,z)][0]<y: top[(x,z)]=(y,b)

def rgb(b):
    if 'leaves' in b or 'moss' in b: return (56,113,72)
    if 'grass' in b: return (86,130,75)
    if 'amethyst' in b: return (135,87,176)
    if 'blackstone' in b or 'deepslate' in b: return (46,49,60)
    if 'copper' in b: return (62,139,128)
    if 'dark_oak' in b or 'spruce' in b: return (94,63,45)
    if 'mud' in b: return (128,103,83)
    if 'tuff' in b: return (105,123,113)
    if 'calcite' in b or 'diorite' in b or 'quartz' in b: return (215,215,198)
    if 'red_wool' in b: return (182,64,52)
    if 'orange_wool' in b: return (205,125,53)
    if 'yellow_wool' in b: return (213,168,66)
    if 'stone' in b or 'andesite' in b: return (139,142,137)
    if 'lantern' in b or 'lava' in b: return (250,189,73)
    return (116,100,81)

s=6
img=Image.new('RGB',(192*s,192*s),(24,35,53))
d=ImageDraw.Draw(img)
for (x,z),(y,b) in top.items():
    if not (-96<=x<96 and -96<=z<96): continue
    c=rgb(b)
    # Modest height shade reveals terraces and roof silhouette.
    f=max(.72,min(1.14,1+(y-100)*.012))
    c=tuple(min(255,round(v*f)) for v in c)
    X=(x+96)*s;Z=(z+96)*s
    d.rectangle((X,Z,X+s-1,Z+s-1),fill=c)
labels=[(0,59,'PRZYSTAŃ'),(0,0,'NEXUS'),(-45,20,'RYNEK'),(-52,-3,'SAKWA'),
        (-31,52,'TRENING'),(39,48,'GILDIE'),(48,20,'RZEMIOSŁO'),
        (-43,-34,'KLASY'),(43,-35,'CHOWAŃCE'),(0,-49,'ŚWIĄTYNIA'),(13,42,'TABLICA')]
for x,z,label in labels:
    X=(x+96)*s;Z=(z+96)*s
    d.ellipse((X-3,Z-3,X+3,Z+3),fill=(248,100,72))
    d.text((X+7,Z-7),label,fill=(255,255,255),stroke_width=2,stroke_fill=(10,18,25))
out=Path('/opt/docker/mc/data/world1/output/stolica_v2');out.mkdir(parents=True,exist_ok=True)
img.save(out/'mapa.png')
print(out/'mapa.png')
