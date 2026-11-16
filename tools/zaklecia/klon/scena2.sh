#!/bin/bash
# scena2.sh <prefiks> : 1os spoczynek, 1os rzut, zza plecow rzut, z przodu rzut (bez zmiany slotu) + arkusz
N=$1; X="docker exec -e DISPLAY=:99 mc-client-zaklecia"; R="docker exec zaklecia-proba rcon-cli"
reset(){ $R "minecraft:kill @e[type=zombie]" >/dev/null; $R "mm mobs spawn ZAK_KUKLA 1 world,0.5,-60,6.5" >/dev/null; $R "minecraft:tp MagQA 0.5 -60 0.5 0 8" >/dev/null; $R "mmocore admin resource-mana set MagQA 100" >/dev/null; sleep 0.8; }
shot(){ $X import -window root /review/captures/$N-$1.png; }
reset; sleep 0.6; shot a
$X xdotool mousedown 3; sleep 1.1; shot b; $X xdotool mouseup 3; sleep 0.8
reset; $X xdotool key F5; sleep 0.6; $X xdotool mousedown 3; sleep 1.1; shot c; $X xdotool mouseup 3; sleep 0.8
reset; $X xdotool key F5; sleep 0.6; $X xdotool mousedown 3; sleep 1.1; shot d; $X xdotool mouseup 3; $X xdotool key F5; sleep 0.5
cd /home/dev/.claude/jobs/b0bfedc4/tmp/qa/captures && python3 -c "
from PIL import Image
ims=[Image.open('$N-'+c+'.png').convert('RGB').resize((640,360)) for c in 'abcd']
s=Image.new('RGB',(1280,720)); [s.paste(im,((i%2)*640,(i//2)*360)) for i,im in enumerate(ims)]; s.save('$N-arkusz.png')"
