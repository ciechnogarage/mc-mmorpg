#!/usr/bin/env python3
"""Makiety pasa akcji (HUD) wg /opt/docker/mc/backups/hud-pas-akcji-20261115/PLAN.md.

Tylko obraz do decyzji wlasciciela - nic tu nie trafia do gry.
  python3 makiety_gen.py <tlo.png 1280x720 GUI x3> <katalog wyjscia>
Tlo: kadr klienta QA z klona (HUD v5, hotbar Ksiegi). Wanilia (hotbar, przedmioty) z client.jar
klienta QA, ikony zaklec z paczki Nexo world1 (tylko odczyt). Font MC z tools/hud/mcfont (poza gitem).
Wszystko w pikselach GUI, skalowane NEAREST x3 (= GUI 3 na 1280x720, najciasniejszy przypadek).
"""
import json, sys, zipfile, io
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance

TLO = Path(sys.argv[1]); OUT = Path(sys.argv[2]); OUT.mkdir(parents=True, exist_ok=True)
CLIENT = Path('/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar')
ZAK = Path('/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/zaklecia/textures/item')
MCFONT = Path('/home/dev/Desktop/Przemek/MC/tools/hud/mcfont')
S = 3
N = Image.Resampling.NEAREST
T = (0, 0, 0, 0)
# Paleta Ksiegi (ta sama co makiety2_gen.py / HUD v5).
P = {'ink':'#251C2B','leather0':'#39232E','leather1':'#684338','leather2':'#8C532C',
     'gold0':'#8A6A30','gold1':'#D5A96B','gold2':'#FFE7AE','cream':'#FFF1CC',
     'navy0':'#183650','navy1':'#286287','blue':'#3F98CB','red':'#CE4042',
     'redhi':'#F36D62','green':'#61964A','grey':'#8B7F86'}
def c(k): return P.get(k, k)
def rgba(k, a=255):
    h = c(k).lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (a,)

_jar = zipfile.ZipFile(CLIENT)
def vanilla(name):
    return Image.open(io.BytesIO(_jar.read(f'assets/minecraft/textures/item/{name}.png'))).convert('RGBA')
def spell(name): return Image.open(ZAK/f'ikona_{name}.png').convert('RGBA')

# ---------- font MC (ascii + accented, jak makiety2_gen.py) ----------
FONT = {}
for p in json.loads((MCFONT/'assets/minecraft/font/include/default.json').read_text())['providers']:
    if p['file'] not in ('minecraft:font/ascii.png', 'minecraft:font/accented.png'): continue
    sheet = Image.open(MCFONT/'assets/minecraft/textures/font'/p['file'].split('/')[-1]).convert('RGBA')
    cw = sheet.width//16; ch = sheet.height//len(p['chars'])
    for row, chars in enumerate(p['chars']):
        for col, char in enumerate(chars):
            if char == '\0' or char in FONT: continue
            g = sheet.crop((col*cw, row*ch, (col+1)*cw, (row+1)*ch))
            bb = g.getchannel('A').getbbox()
            FONT[char] = (g.crop((0, 0, bb[2], ch)), bb[2]+1) if bb else (Image.new('RGBA', (0, ch)), 4)
def tw(s): return sum(FONT.get(ch, FONT['?'])[1] for ch in s) - 1
def text(im, x, y, s, col='cream', shadow='ink'):
    """Tekst MC w pikselach GUI (8 px wysokosci), cien 1 px jak w grze."""
    for ch in s:
        g, adv = FONT.get(ch, FONT['?'])
        if g.width:
            a = g.getchannel('A')
            if shadow:
                sh = Image.new('RGBA', g.size, c(shadow)); sh.putalpha(a); im.alpha_composite(sh, (x+1, y+1))
            t = Image.new('RGBA', g.size, c(col)); t.putalpha(a); im.alpha_composite(t, (x, y))
        x += adv

# ---------- mikro-font 3x5 na znak klawisza ----------
MIKRO = {
 '1': [' # ','## ',' # ',' # ','###'], '2': ['## ','  #',' # ','#  ','###'],
 '3': ['## ','  #',' # ','  #','## '], '4': ['# #','# #','###','  #','  #'],
 '5': ['###','#  ','## ','  #','## '], '6': [' ##','#  ','## ','# #',' # '],
 'L': ['#  ','#  ','#  ','#  ','###'], 'P': ['## ','# #','## ','#  ','#  '],
 'F': ['###','#  ','## ','#  ','#  '], 'Q': [' # ','# #','# #','## ',' ##'],
}
def mikro(d, x, y, s, col):
    for ch in s:
        for j, row in enumerate(MIKRO[ch]):
            for i, v in enumerate(row):
                if v == '#': d.point((x+i, y+j), fill=c(col))
        x += 4

# ---------- ikony skilli w 3 stylach ----------
MAT = {  # kontur, ciemny, baza, swiatlo - 4 tony na material, kolorowy kontur jak ikony zaklec
 'steel': ('#3B4560','#6F7F99','#A9B8CC','#EEF4FA'), 'wood': ('#3F241C','#6B3F27','#9A6237','#C08A55'),
 'gold': ('#6E4F1F','#A97E3A','#D5A96B','#FFE7AE'), 'rage': ('#5A1520','#A32D35','#E04A45','#FF9C7A'),
 'bone': ('#5E4630','#9C7B52','#CFAE7A','#F4E2B8'), 'arc': ('#34245A','#5E3F9A','#8E6AD0','#D3BDFF'),
 'ice': ('#173452','#2A6493','#4A9FD3','#BFE6FF'), 'fire': ('#6B2412','#C2501F','#F0902E','#FFE08A'),
}
def rysuj(warstwy):
    """warstwy: [(material, funkcja(draw))] -> 16x16 z auto-cieniem (swiatlo z lewej-gory) i kolorowym konturem."""
    own = [[None]*16 for _ in range(16)]
    for mat, f in warstwy:
        m = Image.new('L', (16, 16), 0); f(ImageDraw.Draw(m))
        for y in range(16):
            for x in range(16):
                if m.getpixel((x, y)) > 127: own[y][x] = mat
    def get(x, y): return own[y][x] if 0 <= x < 16 and 0 <= y < 16 else None
    im = Image.new('RGBA', (16, 16), T)
    for y in range(16):
        for x in range(16):
            m = own[y][x]
            if m:
                t = MAT[m]; tone = t[2]
                if get(x-1, y) != m or get(x, y-1) != m: tone = t[3]
                elif get(x+1, y) != m or get(x, y+1) != m: tone = t[1]
                im.putpixel((x, y), rgba(tone))
            else:
                ns = [get(x+dx, y+dy) for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))]
                ns = [n for n in ns if n]
                if ns: im.putpixel((x, y), rgba(MAT[ns[0]][0]))
    return im

IKONY = {  # rysowane (styl B): kształty w siatce 16 px, kontur doklada rysuj()
 'pchniecie': [('wood', lambda d: d.line([(2,13),(9,6)], 255, 2)),
               ('gold', lambda d: d.rectangle((8,6,9,7), 255)),
               ('steel', lambda d: d.polygon([(9,4),(13,2),(11,6)], 255))],
 'rozplatanie': [('steel', lambda d: d.arc((1,1,14,14), 200, 330, 255, 1)),
                 ('wood', lambda d: d.line([(4,13),(9,7)], 255, 2)),
                 ('steel', lambda d: d.polygon([(8,4),(12,4),(13,9),(10,9)], 255))],
 'okrzyk': [('bone', lambda d: d.polygon([(1,14),(4,12),(7,10),(9,7),(10,4),(14,4),(14,8),(11,11),(7,13),(3,15)], 255)),
            ('wood', lambda d: d.rectangle((11,5,13,7), 255)),
            ('gold', lambda d: d.line([(7,9),(9,12)], 255, 1)),
            ('gold', lambda d: (d.point([(9,1),(12,0),(15,1)], 255)))],
 'gniew': [('fire', lambda d: d.polygon([(6,1),(8,4),(10,2),(11,6),(14,5),(13,10),(9,12),(5,10)], 255)),
           ('wood', lambda d: d.line([(3,14),(8,8)], 255, 2)),
           ('rage', lambda d: d.polygon([(7,4),(11,3),(13,8),(9,9)], 255))],
 'pieczec': [('arc', lambda d: d.ellipse((2,2,13,13), outline=255, width=2)),
             ('gold', lambda d: d.polygon([(7,4),(10,7),(7,10),(4,7)], 255))],
 'bariera': [('ice', lambda d: d.polygon([(3,2),(12,2),(12,8),(8,13),(7,13),(3,8)], 255)),
             ('gold', lambda d: (d.line([(7,4),(7,10)], 255, 2), d.line([(5,6),(10,6)], 255, 1)))],
 'wybuch': [('arc', lambda d: d.polygon([(7,1),(9,5),(14,4),(10,8),(14,12),(9,11),(7,14),(6,10),(1,12),(4,8),(1,4),(5,5)], 255)),
            ('gold', lambda d: d.ellipse((6,6,9,9), 255))],
 'mrugniecie': [('arc', lambda d: (d.point([(2,8),(4,7),(4,9)], 255), d.rectangle((6,7,7,9), 255))),
                ('arc', lambda d: d.polygon([(9,3),(14,8),(9,13),(9,10),(8,10),(8,6),(9,6)], 255))],
}
WANILIA = {'pchniecie': 'iron_sword', 'rozplatanie': 'iron_axe', 'okrzyk': 'goat_horn', 'gniew': 'netherite_axe',
           'pieczec': 'book', 'bariera': None, 'wybuch': 'fire_charge', 'mrugniecie': 'ender_pearl'}
ROLA = {'pchniecie': 'red', 'rozplatanie': 'red', 'gniew': 'red', 'okrzyk': 'gold0',
        'pieczec': 'navy1', 'bariera': 'navy1', 'wybuch': 'red', 'mrugniecie': '#5E3F9A'}
def znak(k):
    """Styl C: jednobarwny znak (sylwetka ikony B) na krazku w kolorze roli."""
    im = Image.new('RGBA', (16, 16), T); d = ImageDraw.Draw(im)
    d.ellipse((0, 0, 15, 15), fill=c('ink')); d.ellipse((1, 1, 14, 14), fill=c(ROLA[k]))
    d.arc((1, 1, 14, 14), 120, 300, fill=c('gold1'))
    m = Image.new('L', (16, 16), 0); dm = ImageDraw.Draw(m)
    for _, f in IKONY[k]: f(dm)   # sama sylwetka, bez konturu - kontur zlewa znak w plame
    m = m.crop(m.getbbox()); sk = min(1, 11/max(m.size)); m = m.resize((max(1, round(m.width*sk)), max(1, round(m.height*sk))), N)
    x, y = 8 - m.width//2, 8 - m.height//2
    sh = Image.new('RGBA', m.size, c('ink')); sh.putalpha(m); im.alpha_composite(sh, (x+1, y+1))
    g = Image.new('RGBA', m.size, c('cream')); g.putalpha(m); im.alpha_composite(g, (x, y))
    return im
def ikona(k, styl):
    if styl == 'wanilia': return vanilla(WANILIA[k]) if WANILIA[k] else rysuj(IKONY[k])
    if styl == 'rysowana': return rysuj(IKONY[k])
    return znak(k)

# ---------- kafel 20x20 GUI ----------
def szary(im, jasn):
    g = ImageEnhance.Color(im).enhance(0.15); return ImageEnhance.Brightness(g).enhance(jasn)
def kafel(ico, stan='gotowa', klawisz=None, cd=None, frac=0.0, tryb=None):
    """stan: gotowa | odnowienie (frac = ile zostalo, cd = sekundy) | brak (zasobu) | laduje (frac postepu)
    | trwa (koncentracja) | pusta | zablokowana.  tryb: None | 'f' (grupa F podswietlona) | 'przygas'."""
    im = Image.new('RGBA', (22, 22), T); d = ImageDraw.Draw(im)
    o = 1  # 1 px zapasu na poswiate trybu F - kafel nie zmienia rozmiaru (nie 'skacze')
    if tryb == 'f':
        d.rounded_rectangle((0, 0, 21, 21), 2, fill=rgba('gold2', 150))
    # kontur + rama: rama jest w KAZDYM stanie, zmienia sie tylko kolor
    d.rectangle((o+1, o, o+18, o+19), fill=c('ink')); d.rectangle((o, o+1, o+19, o+18), fill=c('ink'))
    rama = {'gotowa': ('gold2', 'gold1', 'gold0'), 'odnowienie': ('leather2', 'leather1', 'leather0'),
            'brak': ('blue', 'navy1', 'navy0'), 'laduje': ('leather2', 'leather1', 'leather0'),
            'trwa': ('cream', 'gold2', 'gold1'), 'pusta': ('leather1', 'leather0', 'leather0'),
            'zablokowana': ('leather1', 'leather0', 'leather0')}[stan]
    if tryb == 'f' and stan == 'gotowa': rama = ('cream', 'gold2', 'gold1')
    if tryb == 'przygas': rama = ('leather2', 'leather1', 'leather0')
    hi, mid, lo = rama
    d.rectangle((o+1, o+1, o+18, o+18), fill=c(mid))
    d.line([(o+1, o+1), (o+18, o+1)], fill=c(hi)); d.line([(o+1, o+1), (o+1, o+18)], fill=c(hi))
    d.line([(o+2, o+18), (o+18, o+18)], fill=c(lo)); d.line([(o+18, o+2), (o+18, o+18)], fill=c(lo))
    d.rectangle((o+2, o+2, o+17, o+17), fill=c('leather0'))
    if stan not in ('pusta', 'zablokowana'):
        ic = ico.copy()
        if stan == 'brak': ic = szary(ic, 0.8)
        if tryb == 'przygas': ic = ImageEnhance.Brightness(ic).enhance(0.5)
        if stan == 'odnowienie':
            h = round(16*frac)  # ciemne od gory, jasnieje od dolu, krok 2 px = 8 krokow
            ciemna = szary(ic, 0.45).crop((0, 0, 16, h)); ic.paste(ciemna, (0, 0))
        im.alpha_composite(ic, (o+2, o+2))
        if stan == 'odnowienie' and h:
            im.alpha_composite(Image.new('RGBA', (16, h), rgba('ink', 120)), (o+2, o+2))
        if stan == 'brak':
            niebo = Image.new('RGBA', (16, 16), rgba('navy1', 110)); im.alpha_composite(niebo, (o+2, o+2))
        if stan == 'odnowienie' and 0 < h < 16:
            d.line([(o+2, o+2+h), (o+17, o+2+h)], fill=c('gold1'))
    if stan == 'zablokowana':  # klodka 5x6
        d.rectangle((o+8, o+6, o+11, o+8), outline=c('leather2')); d.rectangle((o+7, o+9, o+12, o+13), fill=c('leather2'))
        d.point((o+9, o+11), fill=c('leather0')); d.point((o+10, o+11), fill=c('leather0'))
    if stan == 'laduje':  # postep obiega rame zgodnie z zegarem od gory-lewej
        obw = [(x, o+1) for x in range(o+1, o+19)] + [(o+18, y) for y in range(o+2, o+19)] + \
              [(x, o+18) for x in range(o+17, o, -1)] + [(o+1, y) for y in range(o+17, o+1, -1)]
        for pt in obw[:round(len(obw)*frac)]: d.point(pt, fill=c('cream'))
    if cd is not None and stan == 'odnowienie':
        s = str(cd); text(im, o+10-tw(s)//2, o+6, s, 'cream')
    if klawisz:  # znaczek klawisza w prawym dolnym rogu, na plytce
        w = 4*len(klawisz)+1
        x0, y0 = o+19-w, o+13
        bg = 'gold2' if tryb == 'f' else 'ink'; fg = 'ink' if tryb == 'f' else ('grey' if tryb == 'przygas' else 'gold1')
        d.rectangle((x0, y0, o+19, o+19), fill=c(bg))
        mikro(d, x0+1, y0+1, klawisz, fg)
    return im

# ---------- tlo ----------
def tlo():
    im = Image.open(TLO).convert('RGBA')
    # Kafle v5 obok hotbara (stare wskazniki umiejetnosci) - zamalowane trawa spod nich; nowy pas je zastepuje.
    # Zrodlo latki: czysta trawa przy krawedziach kadru (x 0-130 i 1150-1280), ten sam pas wysokosci.
    for x0, x1, src in ((128, 244, 0), (924, 1042, 1150)):
        im.paste(im.crop((src, 648, src+x1-x0, 713)), (x0, 648))
    return im
def wkoleruj_szal(im):
    """Prawy pasek v5 dla wojownika: Szal (MMOCore mana 'Szal', DARK_RED) zamiast niebieskiej many."""
    px = im.load()
    for y in range(600, 640):
        for x in range(672, 910):
            r, g, b, a = px[x, y]
            if b > r + 25 and b > g:
                k = (b - 40) / 215; px[x, y] = (int(90+150*k), int(25+40*k), int(30+35*k), a)
    return im
HB_X, HB_Y = 122, 218  # hotbar w GUI (182x22, srodek ekranu 426 GUI, dol)

def wklej(ekran_gui, im, x, y): ekran_gui.alpha_composite(im, (x-1, y-1))

def plyta(d, box):
    """Podkladka grupy: skora Ksiegi, ta sama co plyty v5."""
    x0, y0, x1, y1 = box
    d.rectangle((x0+1, y0, x1-1, y1), fill=c('ink')); d.rectangle((x0, y0+1, x1, y1-1), fill=c('ink'))
    d.rectangle((x0+1, y0+1, x1-1, y1-1), fill=c('leather0'))
    d.line([(x0+2, y0+1), (x1-2, y0+1)], fill=c('leather1'))

# ---------- stany wg scen ----------
MAG_LP = [('kula_ognia', 'L'), ('plomienie', 'P')]
MAG_F = ['pieczec', 'bariera', 'wybuch', 'mrugniecie']
WOJ_F = ['pchniecie', 'rozplatanie', 'okrzyk', 'gniew']
KLAW = ['2', '3', '4', '5']   # przyklad: klawisz liczy serwer (MMOCore use-lowest-keybinds, offset slotu)
SCENY = {
 'walka':  {'lp': [dict(stan='laduje', frac=0.6), dict(stan='trwa')],
            'f': [dict(stan='gotowa'), dict(stan='odnowienie', frac=0.5, cd=6), dict(stan='brak'), dict(stan='odnowienie', frac=0.875, cd=7)]},
 'tryb_f': {'lp': [dict(stan='gotowa', tryb='przygas'), dict(stan='gotowa', tryb='przygas')],
            'f': [dict(stan='gotowa', tryb='f'), dict(stan='odnowienie', frac=0.5, cd=6, tryb='f'), dict(stan='brak', tryb='f'), dict(stan='gotowa', tryb='f')]},
}
def przedmioty():
    """Miejsce na przedmioty (np. mikstury na klawisz) - dwa kafle, jeden pusty."""
    pot = vanilla('potion'); tint = Image.new('RGBA', pot.size, rgba('red', 255))
    m = pot.getchannel('A'); pot2 = Image.composite(Image.blend(pot, tint, 0.45), pot, m)
    return [kafel(pot2, 'gotowa', 'Q'), kafel(None, 'pusta')]

def uklad(wariant, klasa, scena, styl='wanilia'):
    gui = Image.new('RGBA', (426, 240), T); d = ImageDraw.Draw(gui)
    sc = SCENY[scena]
    lp = [kafel(spell(n), klawisz=k, **st) for (n, k), st in zip(MAG_LP, sc['lp'])] if klasa == 'mag' else []
    fk = MAG_F if klasa == 'mag' else WOJ_F
    f = [kafel(ikona(k, styl), klawisz=KLAW[i], **sc['f'][i]) for i, k in enumerate(fk)]
    it = przedmioty()
    y = HB_Y + 1  # dolna krawedz kafli = dolna krawedz hotbara; kafle 20 px przy hotbarze 22 px
    if wariant == 'A':   # jeden pas po lewej: [L][P] | [F x4], przedmioty po prawej
        grupa = lp + ([None] if lp else []) + f   # rozstaw 19: sasiednie kafle dziela kontur
        szer = sum(3 if g is None else 19 for g in grupa) + 1
        x = HB_X - 2 - szer; plyta(d, (x-1, y-2, x+szer, y+20))
        xx = x
        for g in grupa:
            if g is None:
                d.line([(xx+1, y), (xx+1, y+18)], fill=c('gold0')); xx += 3; continue
            wklej(gui, g, xx, y-1); xx += 19
        print('A', 'szerokosc pasa', szer+2, 'GUI px, miejsce 122, x od', x-1)
        x = HB_X + 182 + 3; plyta(d, (x-1, y-2, x+41, y+20))
        for i, g in enumerate(it): wklej(gui, g, x+1+i*20, y-1)
    elif wariant == 'B':  # skrzydla: przedmioty + [L][P] po lewej, F po prawej - symetrycznie
        lewa = it + ([None] if lp else []) + lp
        szer = sum(4 if g is None else 20 for g in lewa) + 2
        x = HB_X - 6 - szer; plyta(d, (x-1, y-2, x+szer, y+20)); xx = x+1
        for g in lewa:
            if g is None:
                d.line([(xx+1, y), (xx+1, y+18)], fill=c('gold0')); xx += 4; continue
            wklej(gui, g, xx, y-1); xx += 20
        x = HB_X + 182 + 6; plyta(d, (x-1, y-2, x+82, y+20))
        for i, g in enumerate(f): wklej(gui, g, x+1+i*20, y-1)
    else:  # C: przyklejone do hotbara - jeden dlugi pas, bez plytek; L/P i F po bokach, przedmioty na skrajach
        lewa = it[:1] + lp
        x = HB_X - 20*len(lewa)
        for i, g in enumerate(lewa): wklej(gui, g, x+i*20, y-1)
        d.line([(HB_X-1, y-1), (HB_X-1, y+19)], fill=c('gold0'))
        x = HB_X + 182
        for i, g in enumerate(f): wklej(gui, g, x+i*20, y-1)
        d.line([(HB_X+182, y-1), (HB_X+182, y+19)], fill=c('gold0'))
    return gui

def ekran(gui, klasa):
    bg = tlo()
    if klasa == 'woj': bg = wkoleruj_szal(bg)
    bg.alpha_composite(gui.resize((426*S, 240*S), N), (1, 0))
    return bg

def main():
    wyniki = []
    for w in 'ABC':
        for klasa, scena in (('mag', 'walka'), ('mag', 'tryb_f'), ('woj', 'walka')):
            im = ekran(uklad(w, klasa, scena), klasa)
            nazwa = f'{w}_{klasa}_{scena}'
            im.save(OUT/f'{nazwa}_pelny.png'); im.crop((0, 520, 1280, 720)).save(OUT/f'{nazwa}.png'); wyniki.append(nazwa)
    # Arkusz stanow kafla x6
    st = [('gotowa', dict(stan='gotowa')), ('odnowienie 7/8', dict(stan='odnowienie', frac=0.875, cd=7)),
          ('4/8', dict(stan='odnowienie', frac=0.5, cd=4)), ('1/8', dict(stan='odnowienie', frac=0.125, cd=1)),
          ('brak many', dict(stan='brak')), ('ładowanie 30%', dict(stan='laduje', frac=0.3)),
          ('ładowanie 80%', dict(stan='laduje', frac=0.8)), ('trwa (koncentr.)', dict(stan='trwa')),
          ('tryb F', dict(stan='gotowa', tryb='f')), ('przygaszona', dict(stan='gotowa', tryb='przygas')),
          ('zablokowana', dict(stan='zablokowana'))]
    ark = Image.new('RGBA', (len(st)*26+4, 26), rgba('#2E2A26'))
    for i, (_, kw) in enumerate(st):
        ico = spell('kula_ognia') if kw['stan'] in ('laduje', 'trwa', 'brak') else vanilla('iron_axe')
        ark.alpha_composite(kafel(ico, klawisz='L' if kw['stan'] in ('laduje', 'trwa') else '3', **kw), (3+i*26, 2))
    ark.resize((ark.width*6, ark.height*6), N).save(OUT/'stany_x6.png')
    (OUT/'stany.json').write_text(json.dumps([n for n, _ in st], ensure_ascii=False))
    # Kroki odnowienia 8/8..0/8 (x6)
    ko = Image.new('RGBA', (9*26+4, 26), rgba('#2E2A26'))
    for i in range(9):
        fr = (8-i)/8
        kw = dict(stan='odnowienie', frac=fr, cd=8-i) if fr > 0 else dict(stan='gotowa')
        ko.alpha_composite(kafel(vanilla('iron_axe'), klawisz='3', **kw), (3+i*26, 2))
    ko.resize((ko.width*6, ko.height*6), N).save(OUT/'odnowienie_kroki_x6.png')
    # Style ikon skilli: wanilia / rysowane / znak - wojownik i mag, x6
    for klasa, fk in (('woj', WOJ_F), ('mag', MAG_F)):
        sh = Image.new('RGBA', (4*22+2, 3*24+2), rgba('#2E2A26'))
        for r, styl in enumerate(('wanilia', 'rysowana', 'znak')):
            for i, k in enumerate(fk): sh.alpha_composite(kafel(ikona(k, styl), klawisz=KLAW[i]), (1+i*22, 1+r*24))
        sh.resize((sh.width*6, sh.height*6), N).save(OUT/f'style_{klasa}_x6.png')
    # Ten sam pas B w trzech stylach ikon (pelny ekran, dol), wojownik
    for styl in ('wanilia', 'rysowana', 'znak'):
        im = ekran(uklad('B', 'woj', 'walka', styl), 'woj'); im.crop((0, 520, 1280, 720)).save(OUT/f'styl_{styl}_B_woj.png')
    # Szerokosc pasa vs miejsce obok hotbara przy GUI 3 na 1280x720 (122 px GUI na bok)
    print('OK', len(wyniki), 'scen ->', OUT)

main()
