import sys, glob
from PIL import Image, ImageDraw
out=sys.argv[1]; cell=int(sys.argv[2]); files=[]
for g in sys.argv[3:]: files+=sorted(glob.glob(g,recursive=True))
files=[f for f in files if f.endswith('.png')][:120]
cols=6; rows=(len(files)+cols-1)//cols
W=Image.new('RGBA',(cols*cell,rows*(cell+14)),(70,70,80,255)); d=ImageDraw.Draw(W)
for i,f in enumerate(files):
    im=Image.open(f).convert('RGBA'); w,h=im.size
    s=min(cell/w,cell/h); s = int(s) if s>=1 else s
    im=im.resize((max(1,int(w*s)),max(1,int(h*s))), Image.NEAREST)
    x=(i%cols)*cell; y=(i//cols)*(cell+14)
    W.paste(im,(x,y+14),im); d.text((x+2,y+1),'/'.join(f.split('/')[-2:])[:34]+f' {w}x{h}',fill=(255,255,120,255))
W.save(out); print(out,len(files))
