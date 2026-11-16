"""Offline arkusze i wycinki kontrolne CODEX-A v3, bez serwera."""
import json
import sys
from pathlib import Path
from PIL import Image, ImageDraw

sys.path.insert(0,str(Path(__file__).resolve().parent))
from render_bbmodel import renderuj

ROOT=Path('/opt/docker/mc/data/world1/output/lochy/bagna')
NAMES=('mulowiec','pijawka','kosciej','szeptucha','kokoniara')
W,H=420,480
CROPS={
    'mulowiec':{'glowa':(470,65,650,205),'brzuch_rana':(475,155,660,310),
                'barki_rzesa':(475,65,735,235),'lopata_chwyt':(485,100,825,450),
                'knykcie':(465,220,715,365)},
    'pijawka':{'pierscienie':(460,145,790,315),'przyssawka_wneka':(455,190,610,325),
               'spod_sluz':(485,220,770,330)},
    'kosciej':{'luk_cieciwa':(570,85,770,420),'rzesa_zebra':(505,150,675,315),
               'oczodol_korzen':(490,80,630,210)},
    'szeptucha':{'kaptur_twarz':(485,65,680,235),'kociol_dlonie':(465,200,700,420),
                 'plaszcz_siec':(955,100,1180,400)},
    'kokoniara':{'nogi_stawy':(455,135,810,410),'jajo_oplot_szczelina':(565,105,715,285),
                 'glowa_kly_oczy':(515,180,680,335),'szew_tyl':(990,100,1150,295)},
}

def panel(img,label):
    out=Image.new('RGB',(W,H),(102,103,100));d=ImageDraw.Draw(out)
    img.thumbnail((W-20,H-54),Image.Resampling.LANCZOS)
    out.paste(img,((W-img.width)//2,38+(H-40-img.height)//2),img if img.mode=='RGBA' else None)
    d.text((12,10),label,fill=(240,239,224))
    return out

def build(name):
    path=ROOT/'modele'/name/f'bagna_{name}.bbmodel'
    model=json.loads(path.read_text())
    concept=Image.open(ROOT/'szkice'/f'{name}_k1.png').convert('RGB')
    images=[panel(concept,'KONCEPT')]
    for yaw,pitch,label,scale in ((145,20,'PRZÓD 3/4',1),(-35,20,'TYŁ 3/4',1),(145,5,'OCZY GRACZA · 4 BL.',.7)):
        img,_=renderuj(model,yaw,pitch,430)
        if img is None:raise RuntimeError(f'brak renderu {name}')
        if scale<1:img=img.resize((int(img.width*scale),int(img.height*scale)),Image.Resampling.LANCZOS)
        images.append(panel(img,label))
    sheet=Image.new('RGB',(4*W,H))
    for i,img in enumerate(images):sheet.paste(img,(i*W,0))
    dest=ROOT/'modele'/name/'podglad_v3.png';sheet.save(dest)
    cropdir=ROOT/'modele'/name/'cropy';cropdir.mkdir(exist_ok=True)
    for label,box in {
        'przod':(W+55,60,2*W-55,H-45),
        'tyl':(2*W+55,60,3*W-55,H-45),
        'skala':(3*W+55,60,4*W-55,H-45),
    }.items():sheet.crop(box).save(cropdir/f'{label}.png')
    for label,box in CROPS[name].items():sheet.crop(box).save(cropdir/f'{label}.png')
    return sheet

def main():
    combo=Image.new('RGB',(4*W,H*len(NAMES)))
    for i,name in enumerate(NAMES):
        combo.paste(build(name),(0,i*H));print(name,flush=True)
    out=ROOT/'modele/zestawienie_CODEX-A_v3.jpg'
    combo.save(out,quality=92)
    print(out)

if __name__=='__main__':main()
