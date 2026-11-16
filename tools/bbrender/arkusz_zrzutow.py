import json,sys
from pathlib import Path
from PIL import Image, ImageDraw
d=Path(sys.argv[1]); m=json.load(open(d/'manifest.json'))
rola={'lrd_direwolf':'Wilk Gajowy? (wersja lrd)','direwolf':'Wilk Gajowy? (stara wersja)','parry_knight':'Korowy Straznik','boreal_druid':'Zaklinacz Gaju (do przemalowania)','lr_owlbear':'Sowiedz - elita','lr_owlbear_cub':'mlode Sowiedzia','dreadroot':'Straznik Gaju - boss','slimecreep':'Trujacy Pelzacz','gravelcreep':'Korzeniak? (kandydat)','parasite_bud':'Korzeniak? (kandydat)','lono':'Cierniowy Lucznik? (kandydat)'}
C=300; rows=[]
for e in m:
    tiles=[]
    for f in ['przod','tyl','atak']:
        p=d/f"{e['model']}__{f}.png"
        t=Image.new('RGB',(C,C),(139,139,139))
        if p.exists():
            im=Image.open(p).convert('RGBA'); a=im.split()[3]
            px=im.load()
            # tlo zrzutu jest biale-nieprzezroczyste? przytnij po nie-bialych
            bb=a.point(lambda v:255 if v>20 else 0).getbbox() or (0,0,*im.size)
            im=im.crop(bb); im.thumbnail((C-16,C-16))
            t.paste(im.convert('RGB'),((C-im.width)//2,(C-im.height)//2),im.split()[3])
            ImageDraw.Draw(t).text((4,4),f+(' '+str(e.get('atak')) if f=='atak' else ''),fill=(20,20,20))
        tiles.append(t)
    r=Image.new('RGB',(C*3+260,C),(40,42,48))
    for i,t in enumerate(tiles): r.paste(t,(260+i*C,0))
    dr=ImageDraw.Draw(r); dr.text((8,10),e['model'],fill=(255,255,255)); dr.text((8,30),rola.get(e['model'],''),fill=(255,220,120))
    dr.text((8,50),f"elementy {e['elementy']}",fill=(200,200,200))
    dr.text((8,70),'rozm. %.0fx%.0fx%.0f px' % tuple(e['rozmiar']),fill=(200,200,200))
    an=', '.join(e['animacje']); y=95
    for i in range(0,len(an),38): dr.text((8,y),an[i:i+38],fill=(170,170,170)); y+=14
    rows.append(r)
S=Image.new('RGB',(rows[0].width,sum(r.height+4 for r in rows)),(20,20,24)); y=0
for r in rows: S.paste(r,(0,y)); y+=r.height+4
S.save(d/(sys.argv[2] if len(sys.argv)>2 else 'arkusz_loch001.png')); print(S.size)
