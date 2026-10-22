# Paczka HUD L1 (18.10): okno postaci lewy gorny rog + pas przetrwania nad hotbarem.
# python3 -I /w/build.py   (obraz pixelart; /hud = tools/hud ro, /w = ten katalog)
# Wynik: /w/out/MythicHUD/{hud_assets/hud,layouts,source-pack/...} gotowe do skopiowania do plugins/MythicHUD.
# Geometria 1:1 z makiety L1 zaakceptowanej 18.10; tekstury zapisywane 2x (scale 0.5 w YAML).
from pathlib import Path
from PIL import Image, ImageDraw

W = Path('/w'); OUT = W/'out'/'MythicHUD'
TEX = OUT/'source-pack/assets/mythichud/textures/assets/l1'
for p in (TEX, OUT/'hud_assets/hud', OUT/'layouts', OUT/'listeners'): p.mkdir(parents=True, exist_ok=True)
N = Image.Resampling.NEAREST; T = (0,0,0,0)
P = {'ink':'#251C2B','leather0':'#39232E','leather1':'#684338','leather2':'#8C532C',
     'gold0':'#8A6A30','gold1':'#D5A96B','gold2':'#FFE7AE','cream':'#FFF1CC',
     'navy0':'#183650','navy1':'#286287','blue':'#3F98CB','red':'#CE4042',
     'redhi':'#F36D62','green':'#61964A','green0':'#3E6A33','soul':'#B9E6E0','soul0':'#4E8C88'}
def c(k): return P.get(k,k)
def rect(d,b,col):
    x0,y0,x1,y1=b; d.rectangle((min(x0,x1),min(y0,y1),max(x0,x1),max(y0,y1)),fill=c(col))
def line(d,pts,col,w=1): d.line(pts,fill=c(col),width=w)
def corner(d,x,y,sx,sy):
    rect(d,(x,y,x+sx*4,y),'gold0'); rect(d,(x,y,x,y+sy*4),'gold0')
    rect(d,(x+sx,y+sy,x+sx*3,y+sy),'gold2'); rect(d,(x+sx,y+sy,x+sx,y+sy*3),'gold2')
def panel(d,b):
    x0,y0,x1,y1=b
    rect(d,(x0+2,y0,x1-2,y1),'ink'); rect(d,(x0,y0+2,x1,y1-2),'ink')
    rect(d,(x0+2,y0+1,x1-2,y1-1),'leather0'); rect(d,(x0+1,y0+2,x1-1,y1-2),'leather0')
    rect(d,(x0+3,y0+3,x1-3,y1-3),'leather1'); line(d,[(x0+5,y0+2),(x1-5,y0+2)],'leather2')
    for xx,sx in ((x0+2,1),(x1-2,-1)):
        for yy,sy in ((y0+2,1),(y1-2,-1)): corner(d,xx,yy,sx,sy)
def recess(d,b):
    x0,y0,x1,y1=b
    rect(d,(x0+1,y0,x1-1,y1),'ink'); rect(d,(x0,y0+1,x1,y1-1),'ink'); rect(d,(x0+1,y0+1,x1-1,y1-1),'leather0')
def new(w,h): im=Image.new('RGBA',(w,h),T); return im, ImageDraw.Draw(im)
def save(im,name,skala=2):
    im=(im.resize((im.width*skala,im.height*skala),N) if skala>1 else im)
    # MythicHUD 1.3.4 przycina przezroczyste brzegi (TextureFile.trim) przed pocieciem function i przed
    # pozycjonowaniem; piksel alfa 1/255 w dwoch rogach trzyma pelny rozmiar i geometrie z makiety.
    for x,y in ((0,0),(im.width-1,im.height-1)):
        if im.getpixel((x,y))[3]==0: im.putpixel((x,y),(0,0,0,1))
    im.save(TEX/f'{name}.png')

# --- ikony 9x9 (z makiety) ---
def ikona(kind):
    im,d=new(9,9)
    if kind=='serce':
        for y,a,b in ((1,1,3),(1,5,7),(2,0,8),(3,0,8),(4,1,7),(5,2,6),(6,3,5),(7,4,4)): rect(d,(a,y,b,y),'red')
        rect(d,(1,2,2,2),'redhi'); rect(d,(2,1,2,1),'cream')
    elif kind=='kropla':
        for y,a,b in ((0,4,4),(1,3,5),(2,3,5),(3,2,6),(4,1,7),(5,1,7),(6,1,7),(7,2,6),(8,3,5)): rect(d,(a,y,b,y),'blue' if y<5 else 'navy1')
        rect(d,(3,4,3,5),'gold2')
    elif kind=='dusza':
        for y,a,b in ((0,3,5),(1,2,6),(2,1,7),(3,1,7),(4,1,7),(5,2,6),(6,2,7),(7,3,8),(8,5,8)): rect(d,(a,y,b,y),'soul')
        rect(d,(3,3,3,4),'ink'); rect(d,(5,3,5,4),'ink'); rect(d,(6,7,7,8),'soul0')
    elif kind=='szal':   # plomien
        for y,a,b in ((0,4,4),(1,3,5),(2,3,6),(3,2,6),(4,1,7),(5,1,7),(6,1,7),(7,2,6),(8,3,5)): rect(d,(a,y,b,y),'redhi' if y<4 else 'leather2')
        rect(d,(4,5,4,7),'gold2')
    elif kind=='skupienie':  # oko/tarcza celu
        d.ellipse((0,1,8,7),fill=c('gold1')); d.ellipse((2,2,6,6),fill=c('green')); rect(d,(4,4,4,4),'ink')
    elif kind=='trucizna':
        rect(d,(3,0,5,1),'gold1'); rect(d,(2,2,6,8),'green'); rect(d,(3,3,4,4),'gold2'); line(d,[(2,8),(6,8)],'green0')
    elif kind=='uschniecie':
        for y,a,b in ((0,2,6),(1,1,7),(2,1,7),(3,1,7),(4,2,6),(5,2,6),(6,3,5)): rect(d,(a,y,b,y),'leather2')
        rect(d,(2,2,3,3),'ink'); rect(d,(5,2,6,3),'ink'); rect(d,(3,7,5,8),'leather2')
    elif kind=='rany':
        for y,a,b in ((0,2,6),(1,1,7),(2,1,7),(3,1,7),(4,2,6),(5,2,6),(6,3,5)): rect(d,(a,y,b,y),'cream')
        rect(d,(2,2,3,3),'ink'); rect(d,(5,2,6,3),'ink'); rect(d,(3,7,5,8),'cream'); rect(d,(4,5,4,5),'ink')
    elif kind=='babel':
        d.ellipse((1,1,7,7),fill=c('blue')); d.ellipse((2,2,6,6),fill=c('navy1')); rect(d,(3,2,4,3),'cream')
    return im

# ================= PAS =================
for k in ('serce','kropla','dusza','szal','skupienie'):
    im,d=new(11,11); recess(d,(0,0,10,10)); rect(d,(1,1,9,9),'leather1'); im.alpha_composite(ikona(k),(1,1)); save(im,f'kieszen_{k}')
im,d=new(69,11); recess(d,(0,0,68,10)); save(im,'pasek_tlo')
def fill_tex(name,tones):
    im,d=new(67,9); hi,mid,lo=tones
    rect(d,(0,0,66,8),mid); line(d,[(0,0),(66,0)],hi); line(d,[(0,8),(66,8)],lo); save(im,name)
fill_tex('hp_fill',('redhi','red','leather2'))
fill_tex('hp_trucizna',('gold2','green','green0'))
fill_tex('hp_uschniecie',('leather2','leather0','ink'))
im,d=new(67,2); rect(d,(0,0,66,1),'gold2'); line(d,[(0,1),(66,1)],'gold1'); save(im,'absorpcja_fill')
fill_tex('mana_fill',('blue','navy1','navy0'))
fill_tex('szal_fill',('gold1','leather2','leather0'))
fill_tex('skupienie_fill',('gold2','gold1','gold0'))
# Dusze: 10 kulek co 6 px (zgodne z przycinaniem function: 10% = 6,7 px; kulka 4 px + przerwa)
for name,on in (('dusze_tlo',False),('dusze_fill',True)):
    im,d=new(67,9)
    for i in range(10):
        bx=1+(i*67)//10
        if on: rect(d,(bx,2,bx+3,6),'soul'); rect(d,(bx,2,bx,3),'cream')
        else: rect(d,(bx,2,bx+3,6),'leather1')
    save(im,name)
# Alarm niskiego HP: ramka 83x13 wokol kieszeni i paska
im,d=new(83,13)
for b in ((0,0,82,0),(0,12,82,12),(0,0,0,12),(82,0,82,12)): rect(d,b,'redhi')
save(im,'hp_alarm')
# Stamina: 10 jednostek na szerokosci hotbara, rysowana 1x (182 px > 128 przy 2x)
for name,col,hi in (('stamina_tlo',None,None),('stamina_fill','gold1','gold2'),('stamina_niska','red','redhi')):
    im,d=new(182,4)
    for i in range(10):
        bx=i*18+(1 if i else 0); bw=17 if i else 16
        if col is None: recess(d,(bx,0,bx+bw-1,3))
        else: rect(d,(bx+1,1,bx+bw-2,2),col); line(d,[(bx+1,1),(bx+bw-2,1)],hi)
    save(im,name,skala=1)
# Medalion 24x24
im,d=new(24,24)
d.ellipse((0,0,23,23),fill=c('ink')); d.ellipse((1,1,22,22),fill=c('gold0'))
d.ellipse((2,2,21,21),fill=c('gold1')); d.ellipse((3,3,20,20),fill=c('navy0'))
save(im,'medal')
for k in ('rany','trucizna','uschniecie'): save(ikona(k),f'efekt_{k}')
im,d=new(45,9)
for i in range(10):
    bx=(i*9)//2; d.ellipse((bx,3,bx+3,6),fill=c('blue')); rect(d,(bx+1,3,bx+1,3),'cream')
save(im,'tlen')

# ================= OKNO POSTACI v2 (19.10) =================
# 162x56: portret, nick, ikona rasy + klasa, EXP z procentem, rzad plakietek (sakiewka, Wiara/laska, punkty do rozdania).
w=145; h=55
im,d=new(w+1,h+1); panel(d,(0,0,w,h))
recess(d,(5,5,38,38)); rect(d,(6,6,37,37),'ink')
rect(d,(42,5,w-5,16),'gold0'); rect(d,(43,6,w-6,15),'navy0'); line(d,[(44,6),(w-7,6)],'gold1')
recess(d,(42,31,112,35))
line(d,[(6,41),(w-6,41)],'leather0'); line(d,[(6,42),(w-6,42)],'leather2')
save(im.crop((0,0,128,h+1)),'okno_a'); save(im.crop((128,0,w+1,h+1)),'okno_b')
im,d=new(69,3)
rect(d,(0,0,68,2),'gold1'); line(d,[(0,0),(68,0)],'gold2'); line(d,[(0,2),(68,2)],'gold0'); save(im,'exp_fill')
im,d=new(44,44); panel(d,(0,0,43,43)); recess(d,(5,5,38,38)); rect(d,(6,6,37,37),'ink'); save(im,'okno_zwin')
im,d=new(34,34)
for b_ in ((0,0,33,0),(0,33,33,33),(0,0,0,33),(33,0,33,33)): rect(d,b_,'redhi')
save(im,'portret_alarm')
im,d=new(2,2); save(im,'dane')
def ik(rysuj):
    im,d=new(9,9); rysuj(d); return im
# Rasy
save(ik(lambda d:(rect(d,(1,3,7,7),'gold1'),rect(d,(1,1,1,3),'gold1'),rect(d,(4,0,4,3),'gold1'),rect(d,(7,1,7,3),'gold1'),rect(d,(2,5,6,6),'gold0'))),'rasa_czlowiek')
save(ik(lambda d:(d.polygon([(4,0),(8,4),(4,8),(0,4)],fill=c('green')),line(d,[(4,1),(4,7)],'green0'),rect(d,(3,2,3,2),'gold2'))),'rasa_elf')
save(ik(lambda d:(rect(d,(1,2,7,7),'leather2'),rect(d,(2,0,2,3),'cream'),rect(d,(6,0,6,3),'cream'),rect(d,(2,4,3,4),'ink'),rect(d,(5,4,6,4),'ink'))),'rasa_ork')
save(ik(lambda d:(rect(d,(1,0,7,5),'cream'),rect(d,(2,6,6,7),'cream'),rect(d,(2,2,3,3),'ink'),rect(d,(5,2,6,3),'ink'),rect(d,(3,7,3,8),'cream'),rect(d,(5,7,5,8),'cream'))),'rasa_nieumarly')
im,d=new(11,11); recess(d,(0,0,10,10)); rect(d,(1,1,9,9),'leather1'); save(im,'rasa_tlo')
# Plakietki
save(ik(lambda d:(d.ellipse((0,1,8,8),fill=c('gold0')),d.ellipse((1,1,7,7),fill=c('gold1')),rect(d,(3,3,5,5),'gold2'),rect(d,(4,4,4,4),'gold0'))),'pl_sakiewka')
save(ik(lambda d:(d.polygon([(4,0),(7,4),(6,8),(2,8),(1,4)],fill=c('gold1')),d.polygon([(4,3),(5,6),(4,8),(3,6)],fill=c('cream')))),'pl_wiara')
save(ik(lambda d:(d.ellipse((0,0,8,8),outline=c('gold2')),d.polygon([(4,1),(6,4),(5,7),(3,7),(2,4)],fill=c('gold2')),rect(d,(4,4,4,6),'cream'))),'pl_laska')
save(ik(lambda d:(d.polygon([(4,0),(5,3),(8,3),(6,5),(7,8),(4,6),(1,8),(2,5),(0,3),(3,3)],fill=c('gold2')),)),'pl_atrybuty')
save(ik(lambda d:(rect(d,(2,0,6,2),'red'),d.ellipse((1,3,7,8),fill=c('gold1')),d.ellipse((2,4,6,7),fill=c('gold2')),rect(d,(4,5,4,6),'gold0'))),'pl_odznaki')
save(ik(lambda d:(d.polygon([(4,0),(7,4),(6,8),(2,8),(1,4)],fill=c('leather2')),d.polygon([(4,3),(5,6),(4,8),(3,6)],fill=c('leather1')))),'pl_wiara_brak')
save(ik(lambda d:(rect(d,(1,1,7,7),'ink'),rect(d,(2,1,7,6),'navy1'),rect(d,(2,1,2,6),'navy0'),rect(d,(3,7,7,7),'cream'),rect(d,(4,3,6,3),'gold2'))),'pl_umiejetnosci')
# ================= CEL ZADANIA v2: przezroczysta wstega =================
# 230x30: ciemny tusz z alfa malejaca w prawo, zloty akcent po lewej; dwa kawalki po 115 (2x = 230 px <= 256).
im=Image.new('RGBA',(230,30),T)
for i in range(230):
    a_=int(150*max(0,1-i/230)**1.2)
    for j in range(30): im.putpixel((i,j),(37,28,43,a_))
d=ImageDraw.Draw(im); rect(d,(0,0,1,29),'gold1'); rect(d,(0,0,1,0),'gold2')
save(im.crop((0,0,115,30)),'cel_a'); save(im.crop((115,0,230,30)),'cel_b')
save(ik(lambda d:(rect(d,(1,1,7,7),'ink'),rect(d,(2,1,7,6),'gold1'),rect(d,(2,1,2,6),'gold0'),rect(d,(3,7,7,7),'cream'),rect(d,(4,3,6,3),'leather2'))),'cel_ikona')
save(ik(lambda d:(d.polygon([(2,1),(6,4),(2,7)],fill=c('gold1')),)),'cel_strzalka')
print('tekstury', len(list(TEX.glob('*.png'))))

# ================= YAML =================
# Konwencje MythicHUD 1.3.4 (pamiec mythichud-warunki-i-wspolrzedne): 'X wynik hide' chowa, gdy X NIE daje wyniku;
# pierwsza warstwa na wierzchu; warunki na warstwach tekstu ignorowane; offset warstwy: x w prawo, y ujemne w dol.
SPEC = "- placeholder{ph=%player_gamemode%;val=SPECTATOR} false hide"
ZYJE = "- placeholder{ph=%player_health%;v=0;o=>} true hide"
def tex(path, x, y, extra=''):
    return f"      texture: {{path: assets/l1/{path}.png}}\n      scale: {0.5 if path not in ('stamina_tlo','stamina_fill','stamina_niska') else 1}\n      offset: {{x: {x}, y: {-y}}}\n{extra}"
def fn(listener, inc): return f"      function: {{listener: {listener}, increment: {inc}, orientation: horizontal}}\n"
def warunki(*w): return "      conditions:\n" + "".join(f"        {x}\n" for x in w)
def txt(t, x, y, align='center'):
    return f"      text: '{t}'\n      font: default\n      outlined: true\n      align: {align}\n      offset: {{x: {x}, y: {-y}}}\n"
import re
MINX = {}
def asset(name, conds, layers):
    # MythicHUD 1.3.4 normalizuje asset do najbardziej lewej warstwy (zmierzone 18.10: tekstura x=50 sama w assecie
    # rysuje sie na 25). Dlatego najmniejsze x przesuwamy do layoutu, a warstwy zaczynaja sie od 0.
    xs = [int(m) for _, b in layers for m in re.findall(r'offset: \{x: (-?\d+)', b)]
    mn = min(xs) if xs else 0; MINX[name] = mn
    s = f"{name}:\n  conditions:\n" + "".join(f"    {x}\n" for x in conds) + "  layers:\n"
    for k, body in layers:
        body = re.sub(r'offset: \{x: (-?\d+)', lambda m: f"offset: {{x: {int(m.group(1))-mn}", body)
        s += f"    {k}:\n{body}"
    return s

# Pas: kotwica = lewa krawedz hotbara (x -91 od srodka), y = 51 GUI nad dolem ekranu.
ZASOBY = [('mana','kropla','mana_fill'),('szal','szal','szal_fill'),('skupienie','skupienie','skupienie_fill')]
NAZWA = {'mana':None,'szal':'Szał','skupienie':'Skupienie'}
pas = []
pas.append(asset('l1-pas', [SPEC, ZYJE], [
  ('medal', tex('medal', 79, 1)),
  ('absorpcja', tex('absorpcja_fill', 13, 13, fn('absorpcja', 67) + warunki('- progress{l=absorpcja;o=>;v=0} true hide'))),
  ('hp-trucizna', tex('hp_trucizna', 13, 13, fn('health', 67) + warunki('- effect{e=poison} true hide'))),
  ('hp-uschniecie', tex('hp_uschniecie', 13, 13, fn('health', 67) + warunki('- effect{e=wither} true hide'))),
  ('hp', tex('hp_fill', 13, 13, fn('health', 67))),
  ('hp-tlo', tex('pasek_tlo', 12, 12)),
  ('serce', tex('kieszen_serce', 0, 12)),
  ('stamina-niska', tex('stamina_niska', 0, 24, fn('stamina', 10) + warunki('- progress{l=stamina-niska;o=<;v=31%} true hide'))),
  ('stamina', tex('stamina_fill', 0, 24, fn('stamina', 10))),
  ('stamina-tlo', tex('stamina_tlo', 0, 24)),
]))
LV = '- placeholder{ph=%mmocore_level%;'
for nazwa, warunek, x in (('1',[LV+'v=10;o=<} true hide'],89),('2',[LV+'v=10;o=>=} true hide',LV+'v=100;o=<} true hide'],86),('3',[LV+'v=100;o=>=} true hide'],83)):
    pas.append(asset(f'l1-poziom-{nazwa}', [SPEC]+warunek, [
      ('tekst', txt('<#FFF1CC>%mmocore_level%', x, 12, 'right')),
    ]))
pas.append(asset('l1-hp-tekst', [SPEC, '- progress{l=hp;o=>=;v=30%} true hide'], [
  ('tekst', txt('<#FFF1CC>%player_health_rounded%/%player_max_health_rounded%', 16, 14, 'right')),
  ('hp', tex('dane', 0, 0, fn('health', 1))),
]))
pas.append(asset('l1-hp-niskie', [SPEC, '- progress{l=hp;o=<;v=30%} true hide'], [
  ('tekst', txt('<#FFF1CC>%player_health_rounded%/%player_max_health_rounded%', 16, 14, 'right')),
  ('alarm', tex('hp_alarm', -1, 11)),
  ('hp', tex('dane', 0, 0, fn('health', 1))),
]))
for key, ik, fl in ZASOBY:
    if NAZWA[key] is None:
        cond = [SPEC] + [f"- placeholder{{ph=%mmocore_mana_name%;val={n}}} false hide" for n in ('Szał','Skupienie','Dusze')]
    else:
        cond = [SPEC, f"- placeholder{{ph=%mmocore_mana_name%;val={NAZWA[key]}}} true hide"]
    pas.append(asset(f'l1-zasob-{key}', cond, [
      ('tekst', txt('<#FFF1CC>%mana-value%/%mana-max%', 106, 14, 'right')),
      ('mana', tex(fl, 103, 13, fn('mana', 67))),
      ('tlo', tex('pasek_tlo', 102, 12)),
      ('ikona', tex(f'kieszen_{ik}', 172, 12)),
    ]))
pas.append(asset('l1-zasob-dusze', [SPEC, "- placeholder{ph=%mmocore_mana_name%;val=Dusze} true hide"], [
  ('kulki', tex('dusze_fill', 103, 13, fn('mana', 10))),
  ('kulki-tlo', tex('dusze_tlo', 103, 13)),
  ('tlo', tex('pasek_tlo', 102, 12)),
  ('ikona', tex('kieszen_dusza', 172, 12)),
]))
pas.append(asset('l1-tlen', [SPEC, '- progress{l=tlen;o=<;v=100%} true hide'], [
  ('tlen', tex('tlen', 137, 0, fn('air', 10))),
]))
(OUT/'hud_assets/hud/l1-pas.yml').write_text('# Pas przetrwania L1 nad hotbarem (18.10). Kotwica: lewa krawedz hotbara, 51 GUI nad dolem.\n' + '\n'.join(pas))

# Okno postaci
BAZA = [SPEC, ZYJE, '- placeholder{ph=%luckperms_meta_ui_hud%;val=off} false hide']
okno = []
OKNO_W = [
    '- placeholder{ph=%mmocore_in_combat%;val=true} false hide',
    '- placeholder{ph=%chowance_w_walce%;val=tak} false hide',
    '- placeholder{ph=%luckperms_meta_ui_hud%;val=compact} false hide']
RASY = ('czlowiek','elf','ork','nieumarly')
okno.append(asset('l1-okno', BAZA + OKNO_W, [
  ('nick', txt('<#FFF1CC>%player_name%', 46, 8, 'right')),
  ('klasa', txt('<#D5A96B>%mmocore_class%', 43, 20, 'right')),
  ('exp-proc', txt('<#D5A96B>%mmocore_level_percent%%', 116, 30, 'right')),
  ] + [(f'rasa-{r}', tex(f'rasa_{r}', 31, 31, warunki(f'- placeholder{{ph=%wiary_rasa%;val={r}}} true hide'))) for r in RASY] + [
  ('rasa-tlo', tex('rasa_tlo', 30, 30, warunki('- placeholder{ph=%wiary_rasa%;val=brak} false hide'))),
  ('exp', tex('exp_fill', 43, 32, fn('doswiadczenie', 69))),
  ('glowa', "      head: {size: 4}\n      align: right\n      offset: {x: 6, y: -34}\n"),
  ('tlo-b', tex('okno_b', 128, 0)),
  ('tlo', tex('okno_a', 0, 0)),
]))
# Plakietki warunkowe: osobne assety (warunki na tekstach sa ignorowane).
PATRON_BRAK = '- placeholder{ph=%wiary_patron%;val=brak} true hide'
okno.append(asset('l1-pl-patron-brak', BAZA + OKNO_W + [PATRON_BRAK], [
  ('tekst', txt('<#B39066>brak patrona', 19, 44, 'right')),
  ('ikona', tex('pl_wiara_brak', 8, 43)),
]))
okno.append(asset('l1-pl-wiara', BAZA + OKNO_W + ['- placeholder{ph=%wiary_patron%;val=brak} false hide',
                                                  '- placeholder{ph=%wiary_laska_czas%;val=brak} true hide'], [
  ('tekst', txt('<#D5A96B>Wiara %wiary_wiara%', 19, 44, 'right')),
  ('ikona', tex('pl_wiara', 8, 43)),
]))
okno.append(asset('l1-pl-laska', BAZA + OKNO_W + ['- placeholder{ph=%wiary_laska_czas%;val=brak} false hide'], [
  ('tekst', txt('<#FFE7AE>Łaska %wiary_laska_czas%', 19, 44, 'right')),
  ('ikona', tex('pl_laska', 8, 43)),
]))
okno.append(asset('l1-pl-odznaki', BAZA + OKNO_W + ['- placeholder{ph=%chowance_odznaki_liczba%;v=0;o=>} true hide'], [
  ('tekst', txt('<#FFE7AE>%chowance_odznaki_liczba%', 86, 44, 'right')),
  ('ikona', tex('pl_odznaki', 75, 43)),
]))
okno.append(asset('l1-pl-atrybuty', BAZA + OKNO_W + ['- placeholder{ph=%mmocore_attribute_points%;v=0;o=>} true hide'], [
  ('tekst', txt('<#FFE7AE>+%mmocore_attribute_points%', 108, 44, 'right')),
  ('ikona', tex('pl_atrybuty', 97, 43)),
]))
okno.append(asset('l1-pl-umiejetnosci', BAZA + OKNO_W + ['- placeholder{ph=%mmocore_skill_points%;v=0;o=>} true hide'], [
  ('tekst', txt('<#B8E4FF>+%mmocore_skill_points%', 130, 44, 'right')),
  ('ikona', tex('pl_umiejetnosci', 119, 43)),
]))
def zwin(name, conds):
    return asset(name, BAZA + conds, [
      ('alarm', tex('portret_alarm', 5, 5, warunki('- progress{l=hp;o=<;v=30%} true hide'))),
      ('glowa', "      head: {size: 4}\n      align: right\n      offset: {x: 6, y: -34}\n"),
      ('hp', tex('dane', 0, 0, fn('health', 1))),
      ('tlo', tex('okno_zwin', 0, 0)),
    ])
okno.append(zwin('l1-okno-walka', ['- placeholder{ph=%mmocore_in_combat%;val=true} true hide']))
okno.append(zwin('l1-okno-chowance', ['- placeholder{ph=%chowance_w_walce%;val=tak} true hide']))
okno.append(zwin('l1-okno-kompakt', ['- placeholder{ph=%luckperms_meta_ui_hud%;val=compact} true hide']))
(OUT/'hud_assets/hud/l1-okno.yml').write_text('# Okno postaci L1 (18.10). W walce, w walce Chowancow i w trybie kompakt zwija sie do portretu.\n'
  '# Trzy elementy zwiniete, bo warunki MythicHUD lacza sie jak "i" (nie ma "lub"); rysuja sie w tym samym miejscu.\n' + '\n'.join(okno))

# Cel zadania v2: wstega + ikona + tytul + strzalka i krok. Warunki jak w cel.yml z 13.10.
CEL_WSP = ['- placeholder{ph=%player_gamemode%;val=SPECTATOR} false hide', ZYJE,
           '- placeholder{ph=%chowance_w_walce%;val=tak} false hide',
           '- placeholder{ph=%mmocore_in_combat%;val=true} false hide',
           '- placeholder{ph=%luckperms_meta_ui_goal%;val=off} false hide']
def cel(name, conds, tytul, krok):
    return asset(name, conds + CEL_WSP, [
      ('tytul', txt(tytul, 18, 5, 'right')),
      ('krok', txt(krok, 18, 17, 'right')),
      ('ikona', tex('cel_ikona', 6, 4)),
      ('strzalka', tex('cel_strzalka', 7, 16)),
      ('tlo-b', tex('cel_b', 115, 0)),
      ('tlo', tex('cel_a', 0, 0)),
    ])
cele = [
  cel('cel-chowance', ['- placeholder{ph=%chowance_kroki_aktywne%;val=true} true hide',
                       '- placeholder{ph=%chowance_ma_zadanie_mmocore%;val=tak} false hide'],
      '<#FFE7AE>Pierwsze kroki <#D5A96B>%chowance_kroki_postep%', '<#FFF1CC>%chowance_kroki_opis%'),
  cel('cel-mmocore', ['- placeholder{ph=%chowance_ma_zadanie_mmocore%;val=tak} true hide'],
      '<#FFE7AE>%mmocore_quest% <#D5A96B>%mmocore_quest_progress%%', '<#FFF1CC>%mmocore_quest_objective%'),
]
(OUT/'hud_assets/hud/cel.yml').write_text('# Cel zadania v2 (19.10): przezroczysta wstega pod oknem postaci, bez ciezkiego panelu.\n'
  '# Aktywnie podjete zadanie MMOCore ma pierwszenstwo przed samouczkiem Chowancow (boolean z Chowance).\n' + '\n'.join(cele))

# Layouty
def el(name, asset_, px, py, ox, oy):
    ox += MINX.get(asset_, 0)
    return f"    {name}:\n      asset: {asset_}\n      align: right\n      position: {{x: {px}, y: {py}}}\n      offset: {{x: {ox}, y: {oy}}}\n"
lay = "l1-pas-layout:\n  elements:\n"
# Elementy layoutu: PIERWSZY rysuje sie na wierzchu, jak warstwy w assecie (zmierzone 18.10).
for a in ('l1-poziom-1','l1-poziom-2','l1-poziom-3','l1-hp-niskie','l1-hp-tekst','l1-zasob-dusze','l1-zasob-mana','l1-zasob-szal','l1-zasob-skupienie','l1-tlen','l1-pas'):
    lay += el(a.replace('l1-',''), a, 50.0, 0.0, -91, 51)
lay += "\nl1-okno-layout:\n  elements:\n"
for a in ('l1-pl-patron-brak','l1-pl-wiara','l1-pl-laska','l1-pl-odznaki','l1-pl-atrybuty','l1-pl-umiejetnosci','l1-okno','l1-okno-walka','l1-okno-chowance','l1-okno-kompakt'):
    lay += el(a.replace('l1-',''), a, 0.0, 100.0, 4, -4)
(OUT/'layouts/l1.yml').write_text('# HUD L1 (18.10): okno postaci lewy gorny rog + pas przetrwania nad hotbarem.\n' + lay)
print('yaml ok')
(OUT/'layouts/cel.yml').write_text('cel-layout:\n  elements:\n' + el('samouczek','cel-chowance',0.0,100.0,4,-63) + el('zadanie','cel-mmocore',0.0,100.0,4,-63))
import shutil; shutil.copy(W/'chowance_layout.yml', OUT/'layouts/chowance.yml')
exec(open(W/'v3.py').read())
exec(open(W/'v4.py').read())
print('v3 ok')
