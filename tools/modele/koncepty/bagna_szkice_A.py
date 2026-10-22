"""Pięć plansz sylwetek Bagien; 4 px rysunku = 1 px modelu.
Użycie: PYTHONPATH=. python3 koncepty/bagna_szkice_A.py [katalog_wynikowy]
"""
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFont

OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('/opt/docker/mc/data/world1/output/lochy/bagna/szkice')
OUT.mkdir(parents=True, exist_ok=True)
W, H, G = 420, 530, 326
BG, DARK, MID, LIGHT, PALE, TUR = '#858585', '#222222', '#4b4b4b', '#737373', '#b7b7b7', '#3fd1b4'
try:
    FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 13)
    BOLD = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 17)
except OSError:
    FONT = BOLD = ImageFont.load_default()


def line(d, pts, w=8, c=MID): d.line(pts, fill=c, width=w, joint='curve')
def ell(d, box, c=MID): d.ellipse(box, fill=c)
def poly(d, pts, c=MID): d.polygon(pts, fill=c)
def joint(d, p, r=5, c=PALE): ell(d, (p[0]-r,p[1]-r,p[0]+r,p[1]+r),c)
def limb(d, pts, w=13):
    line(d, pts, w, DARK)
    for p in pts[1:-1]: joint(d,p,max(4,w//3))
def weed(d, x, y, n=3):
    for i in range(n): line(d,[(x+i*8,y),(x+i*8-3,y+16+i%2*8)],2,DARK)
def mud(d, x, y):
    for dx in (-12,0,12): line(d,[(x+dx,y-8),(x+dx+2,y)],3,DARK)

def player(d, ground=G):
    x=382; y=ground; c=DARK
    d.rectangle((x-7,y-128,x+7,y-100),fill=c)
    d.rectangle((x-9,y-99,x+9,y-48),fill=c)
    d.rectangle((x-15,y-97,x-10,y-58),fill=c)
    d.rectangle((x+10,y-97,x+15,y-58),fill=c)
    d.rectangle((x-8,y-47,x-2,y),fill=c)
    d.rectangle((x+2,y-47,x+8,y),fill=c)
    d.text((357, ground+5),'gracz',font=FONT,fill=DARK)

def leech(d,v):
    # Wysokość 0.45 gracza; 5 segmentów i dwie przyssawki.
    if v==0: centers=[(76,295,22),(112,289,29),(151,284,31),(191,287,28),(228,292,24)]
    elif v==1: centers=[(70,305,18),(103,297,24),(136,283,28),(169,272,27),(199,280,21)]
    else: centers=[(77,307,13),(106,303,18),(139,296,25),(174,287,31),(211,278,35)]
    for x,y,r in centers:
        ell(d,(x-r,y-r//2,x+r,y+r//2), DARK)
        ell(d,(x-r+4,y-r//2+3,x+r-4,y+2), MID)
        line(d,[(x-r+5,y-r//3),(x+r-4,y-r//3)],3,LIGHT)
    x,y,r=centers[-1]
    if v==1:
        # wygięcie przed wyskokiem
        line(d,[(201,278),(226,257),(241,242)],15,DARK)
        x,y=243,240
    elif v==2:
        # klin: gruby przód wysoko ponad wodą, przyssawka ku kamerze
        x,y=258,274
        line(d,[(218,278),(x-6,y)],22,DARK)
    else: x=257
    ell(d,(x-17,y-18,x+17,y+18),PALE)
    ell(d,(x-12,y-13,x+12,y+13),TUR)
    ell(d,(x-8,y-9,x+8,y+9),DARK)
    for dx,dy in ((-8,-3),(0,-8),(8,-3),(-7,5),(7,5)): joint(d,(x+dx,y+dy),2,PALE)
    ell(d,(48,296,64,308),PALE)
    weed(d,118,278,2); mud(d,146,314)
    if v==2: line(d,[(139,309),(112,321),(90,320)],3,PALE)

def mudman(d,v):
    # 2.2 bl = 282 px; bark/brzuch, krótkie nogi, prawa łopata.
    if v==0: top=48; shoulders=(112,278); belly=(130,270); head=(189,77)
    elif v==1: top=52; shoulders=(99,292); belly=(137,271); head=(191,87)
    else: top=40; shoulders=(120,269); belly=(135,274); head=(186,69)
    for x in (154,222):
        limb(d,[(x,246),(x+(8 if x==154 else -8),282),(x,G)],28)
        mud(d,x,G)
    ell(d,(shoulders[0],top+36,shoulders[1],234),DARK)
    ell(d,(shoulders[0]+15,top+47,shoulders[1]-18,222),MID)
    ell(d,(belly[0],144,belly[1],275),MID)
    poly(d,[(146,95),(220,95),(205,141),(154,140)],LIGHT)
    ell(d,(head[0]-23,head[1]-22,head[0]+23,head[1]+22),DARK)
    # ramiona ze stawami i łopata wrośnięta w prawą rękę
    l_el=(85,203) if v!=1 else (72,225)
    l_end=(90,294) if v!=1 else (101,279)
    limb(d,[(116,107),l_el,l_end],29)
    r_el=(303,206) if v!=2 else (315,151)
    r_end=(316,287) if v!=2 else (335,215)
    limb(d,[(266,101),r_el,r_end],31)
    line(d,[r_end,(r_end[0]+8,r_end[1]+35)],10,PALE)
    poly(d,[(r_end[0]-10,r_end[1]+29),(r_end[0]+30,r_end[1]+29),(r_end[0]+23,r_end[1]+51),(r_end[0]-2,r_end[1]+54)],LIGHT)
    if v==1: poly(d,[(90,130),(120,75),(130,145)],DARK)  # bark jak falochron
    if v==2: poly(d,[(225,70),(264,42),(275,132)],DARK) # garb
    ell(d,(178,156,202,183),DARK); ell(d,(184,160,197,178),TUR)
    weed(d,114,100,3); weed(d,260,104,3); mud(d,181,268)

def skeleton(d,v):
    # 2.0 bl = 256 px; pionowy chudy strzelec.
    x=177 if v!=1 else 195
    hip=(x,216); shoulder=(x+5,105)
    for dx in (-14,14):
        knee=(x+dx+9,270) if v!=2 else (x+dx+(-30 if dx<0 else 28),282)
        foot=(x+dx+4,G) if v!=2 else (x+dx+(-45 if dx<0 else 24),G)
        limb(d,[(hip[0]+dx,hip[1]),knee,foot],9)
        mud(d,foot[0],G)
    line(d,[(x,216),(x+3,151),shoulder],11,PALE)
    for y in (133,151,169,187):
        line(d,[(x-26,y),(x+25,y+5)],5,LIGHT)
    ell(d,(x-24,69,x+23,110),PALE)
    if v==1:
        # Korzeń przebija czaszkę i tworzy asymetryczny grzebień celownika.
        line(d,[(x+10,83),(x+30,55),(x+51,48)],7,DARK)
        line(d,[(x+30,55),(x+36,35)],4,DARK)
    ell(d,(x-17,82,x-7,93),TUR); ell(d,(x+6,82,x+16,93),TUR)
    line(d,[(x+7,72),(x+12,90)],3,DARK)
    # gięty łuk z trzciny, naciąg i kołczan w różnych pozach
    if v==0: bx=285; by=125; hand=(247,157); elbow=(218,138)
    elif v==1: bx=285; by=94; hand=(254,130); elbow=(219,121)
    else: bx=270; by=170; hand=(244,188); elbow=(222,166)
    limb(d,[shoulder,elbow,hand],9)
    limb(d,[(x-6,111),(x-47,151),(x-25,179)],8)
    line(d,[(bx,by-68),(bx+20,by),(bx,by+75)],8,DARK)
    line(d,[(bx,by-68),(bx-3,by+75)],2,PALE)
    line(d,[hand,(bx+15,by)],4,LIGHT)
    ell(d,(bx+12,by-4,bx+24,by+5),TUR)
    poly(d,[(x-35,111),(x-52,119),(x-40,186),(x-25,177)],MID)
    for yy in (124,139,154): line(d,[(x-48,yy),(x-34,yy-23)],2,PALE)
    weed(d,x-20,138,2); mud(d,x,322)

def witch(d,v):
    # 1.8 bl = 230 px; trójkątny płaszcz, chochla i kocioł.
    x=185
    base=(73,310,290,326) if v==0 else ((82,308,293,326) if v==1 else (101,309,292,326))
    poly(d,[(x-25,110),(x+22,110),(base[2],base[3]),(base[0],base[3])],DARK)
    poly(d,[(x-13,128),(x+8,122),(260,307),(108,307)],MID)
    # dwie osobne nogi pod płaszczem
    limb(d,[(155,278),(146,306),(140,G)],8)
    limb(d,[(219,280),(227,307),(234,G)],8)
    poly(d,[(146,114),(x,79),(222,119),(213,147),(151,143)],DARK)
    ell(d,(160,107,211,157),LIGHT)
    poly(d,[(170,126),(141,139),(171,143)],PALE)
    ell(d,(172,120,178,126),TUR); ell(d,(194,120,200,126),TUR)
    if v==0: pot=(272,235); arm=[(217,160),(241,205),(264,223)]; staff=[(242,170),(269,252)]
    elif v==1: pot=(116,271); arm=[(214,159),(246,189),(251,220)]; staff=[(256,108),(246,225)]
    else: pot=(276,180); arm=[(216,155),(245,132),(268,162)]; staff=[(274,95),(274,181)]
    limb(d,arm,10)
    limb(d,[(153,163),(123,208),(133,248)],9)
    line(d,staff,7,PALE)
    ell(d,(pot[0]-22,pot[1]-8,pot[0]+22,pot[1]+23),DARK)
    ell(d,(pot[0]-18,pot[1]-12,pot[0]+18,pot[1]+2),TUR)
    if v==1: line(d,[(111,242),(100,207),(118,187)],3,LIGHT) # kocioł na plecach
    if v==2: poly(d,[(96,230),(107,253),(80,282)],LIGHT) # flaszka na pasie
    for px in (115,167,220,259): weed(d,px,242,2)
    mud(d,177,316)

def spider(d,v):
    # 1.6 bl wysokości = 205 px; rozpiętość 3.5 bl ograniczona planszą.
    body=(182,224) if v!=1 else (172,238)
    abdomen=(266,190) if v==0 else ((275,217) if v==1 else (254,164))
    for side in (-1,1):
        for i in range(4):
            sx=body[0]+side*(10+i*8)
            knee=(sx+side*(35+i*9), 116+(i%2)*18 if v!=1 else 139+(i%2)*14)
            foot=(sx+side*(56+i*12), G-6+(i%2)*5)
            limb(d,[(sx,body[1]),knee,foot],7)
    ell(d,(body[0]-50,body[1]-29,body[0]+49,body[1]+29),DARK)
    ell(d,(abdomen[0]-66,abdomen[1]-55,abdomen[0]+66,abdomen[1]+57),PALE)
    ell(d,(abdomen[0]-55,abdomen[1]-46,abdomen[0]+55,abdomen[1]+48),LIGHT)
    # kokony mają pasma, rząd oczu na głowie
    for off in (-37,-15,12,36):
        line(d,[(abdomen[0]+off,abdomen[1]-43),(abdomen[0]+off+14,abdomen[1]+45)],4,MID)
    ell(d,(abdomen[0]-11,abdomen[1]-14,abdomen[0]+13,abdomen[1]+14),TUR)
    for xx in (body[0]-36,body[0]-23,body[0]-10,body[0]+3,body[0]+16,body[0]+29):
        ell(d,(xx,body[1]-18,xx+5,body[1]-13),TUR)
    poly(d,[(body[0]-29,body[1]+8),(body[0]-13,body[1]+8),(body[0]-25,body[1]+35)],PALE)
    poly(d,[(body[0]+13,body[1]+8),(body[0]+29,body[1]+8),(body[0]+25,body[1]+35)],PALE)
    if v==1: line(d,[(abdomen[0],abdomen[1]+55),(abdomen[0]+15,G)],3,PALE)
    if v==2: poly(d,[(abdomen[0]+40,abdomen[1]-35),(abdomen[0]+68,abdomen[1]-3),(abdomen[0]+42,abdomen[1]+33)],DARK)
    weed(d,abdomen[0]-44,abdomen[1]-34,2); mud(d,body[0],body[1]+24)

MOBS={
 'pijawka':(leech,[('P1 Torpeda pod wodą','Niski grzbiet zdradza rój tuż przed wyskokiem.'),('P2 Łuk sprężyny','Uniesiony przód czytelnie zapowiada skok.'),('P3 Rozwarta przyssawka','Szeroki otwór atakuje z poziomu wody.')]),
 'mulowiec':(mudman,[('M1 Robotnik-golem','Łopata wrośnięta w prawą dłoń ciąży ku ziemi.'),('M2 Falochron z mułu','Jedno barkowe spiętrzenie osłania ranę.'),('M3 Garb kopacza','Prawa ręka unosi łopatę do zamachu.')]),
 'kosciej':(skeleton,[('K1 Celownik trzciny','Pionowy łucznik mierzy ponad ramieniem.'),('K2 Napięty łuk','Wyższy łuk i kołczan podbijają zasięg.'),('K3 Strzał z mułu','Obniżony łuk sugeruje zasadzkę z wody.')]),
 'szeptucha':(witch,[('S1 Kocioł w dłoni','Wywar jest przed ciałem i łatwy do przerwania.'),('S2 Kocioł na plecach','Zgarbiona zielarka miesza chochlą zza barku.'),('S3 Uniesiony wywar','Kocioł nad głową zapowiada kanał osłony.')]),
 'kokoniara':(spider,[('O1 Kokon za tułowiem','Wysokie kolana ramują pulsujący odwłok.'),('O2 Kokon przy ziemi','Ciężki odwłok i nić zapowiadają oplatanie.'),('O3 Rozcięty kokon','Pęknięcie odwłoka zapowiada wylęg młodych.')]),
}

for mob,(draw_fn,variants) in MOBS.items():
    board=Image.new('RGB',(W*3,H),BG)
    bd=ImageDraw.Draw(board)
    for i,(title,hook) in enumerate(variants):
        layer=Image.new('RGBA',(W,345),(0,0,0,0)); d=ImageDraw.Draw(layer)
        draw_fn(d,i)
        panel=Image.new('RGB',(W,H),BG); p=ImageDraw.Draw(panel)
        p.line((15,G+70,W-20,G+70),fill=PALE,width=2)
        p.text((16,12),title,font=BOLD,fill=DARK)
        p.text((16,38),hook,font=FONT,fill=DARK)
        p.text((16,56),'4 px obrazu = 1 px modelu',font=FONT,fill=DARK)
        panel.paste(layer,(0,70),layer)
        p=ImageDraw.Draw(panel); player(p,G+70)
        # Czarny test jest dokładną maską postaci pomniejszoną do 40%.
        alpha=layer.getchannel('A').crop((18,65,352,G+1)).resize((134,105),Image.Resampling.NEAREST)
        icon=Image.new('RGB',alpha.size,DARK)
        panel.paste(icon,(68,417),alpha)
        p=ImageDraw.Draw(panel)
        p.text((219,448),'TEST KSZTAŁTU',font=FONT,fill=DARK)
        if i: p.line((0,0,0,H),fill=DARK,width=2)
        board.paste(panel,(i*W,0))
    board.save(OUT/f'{mob}.png')
    print(OUT/f'{mob}.png')
