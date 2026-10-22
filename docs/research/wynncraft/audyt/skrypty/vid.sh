#!/bin/bash
# usage: vid.sh ID  -> /w/video/ID/{v.mp4,info.json,f/*.jpg,sheet_*.jpg}
id=$1; d=/w/video/$id; mkdir -p $d/f; cd $d
[ -f v.mp4 ] || yt-dlp -q --no-warnings -f "bv*[height<=720][ext=mp4]/bv*[height<=720]" -o v.mp4 --write-info-json --no-playlist "https://www.youtube.com/watch?v=$id" || exit 1
mv -f v.info.json info.json 2>/dev/null
# 1 klatka co 10 s z sygnatura czasu w nazwie
ffmpeg -loglevel error -i v.mp4 -vf "fps=1/10,scale=640:-1" -q:v 3 f/t_%04d.jpg
# arkusze 4x4 z podpisem czasu
python3 - <<'PY'
import glob,os
from PIL import Image,ImageDraw
fs=sorted(glob.glob('f/t_*.jpg'))
for s in range(0,len(fs),16):
    W=Image.new('RGB',(4*480,4*284),(0,0,0)); d=ImageDraw.Draw(W)
    for i,f in enumerate(fs[s:s+16]):
        im=Image.open(f).resize((480,270)); x=(i%4)*480; y=(i//4)*284
        W.paste(im,(x,y+14)); n=int(f[-8:-4]); t=(n-1)*10
        d.text((x+3,y+1),f'{t//60}:{t%60:02d}',fill=(255,255,0))
    W.save(f'sheet_{s//16:02d}.jpg',quality=80)
PY
echo "$id done $(ls f | wc -l) frames"
