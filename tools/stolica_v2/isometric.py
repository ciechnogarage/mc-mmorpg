#!/usr/bin/env python3
"""Software isometric inspection of the deployed block plan (not a game screenshot)."""
from PIL import Image, ImageDraw
from pathlib import Path
import build

build.main()
top={}
for (x,y,z),b in build.B.items():
    if (x,z) not in top or top[(x,z)][0]<y:
        top[(x,z)]=(y,b)

def color(b):
    if 'leaves' in b or 'moss' in b:return (60,113,72)
    if 'grass' in b:return (104,137,76)
    if 'amethyst' in b:return (138,91,178)
    if 'blackstone' in b or 'deepslate' in b:return (49,52,61)
    if 'copper' in b:return (66,141,134)
    if 'dark_oak' in b or 'spruce' in b:return (99,69,49)
    if 'mud' in b:return (125,100,79)
    if 'tuff' in b:return (109,129,116)
    if 'calcite' in b or 'diorite' in b or 'quartz' in b:return (219,219,202)
    if 'red_wool' in b:return (181,59,52)
    if 'orange_wool' in b:return (218,128,58)
    if 'yellow_wool' in b:return (220,174,70)
    if 'stone' in b or 'andesite' in b:return (146,149,143)
    if 'lantern' in b or 'lava' in b:return (247,192,76)
    return (118,104,84)

def shade(c,f):return tuple(max(0,min(255,int(v*f))) for v in c)

S=5
W,H=1700,1000
img=Image.new('RGB',(W,H),(34,48,67))
d=ImageDraw.Draw(img)
def xy(x,y,z):return (W//2+(x-z)*S,300+(x+z)*S//2-(y-100)*S)

for x,z in sorted(top,key=lambda q:q[0]+q[1]):
    y,b=top[(x,z)]
    if y<93:continue
    c=color(b)
    A=xy(x,y+1,z);E=xy(x+1,y+1,z);C=xy(x+1,y+1,z+1);D=xy(x,y+1,z+1)
    ex=top.get((x+1,z),(92,''))[0];sz=top.get((x,z+1),(92,''))[0]
    if ex<y:
        bot1=xy(x+1,ex+1,z);bot2=xy(x+1,ex+1,z+1)
        d.polygon([E,C,bot2,bot1],fill=shade(c,.63))
    if sz<y:
        bot1=xy(x,sz+1,z+1);bot2=xy(x+1,sz+1,z+1)
        d.polygon([D,C,bot2,bot1],fill=shade(c,.79))
    d.polygon([A,E,C,D],fill=shade(c,1.08))

out=Path('/opt/docker/mc/data/world1/output/stolica_v2/isometric.png')
img.save(out)
print(out)
