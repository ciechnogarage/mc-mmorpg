"""Uzycie: tools/pixelart/uruchom.sh tools/modele/koncepty/straznik_gaju_szkice.py OUT.png
Szkice koncepcyjne bossa 2D: sylwetka + 4 walory + jedno swiatlo. Skala: 4 px obrazu = 1 px modelu (gracz 32 px = 128)."""
import sys
from PIL import Image, ImageDraw, ImageFilter


def _sh(xy):
    if isinstance(xy, (list, tuple)) and xy and isinstance(xy[0], (list, tuple)):
        return [(p[0], p[1] + OY) for p in xy]
    return [v + (OY if i % 2 else 0) for i, v in enumerate(xy)]


class _D:
    def __init__(self, im): self.d = ImageDraw.ImageDraw(im)
    def __getattr__(self, n):
        f = getattr(self.d, n)
        return lambda xy, **kw: f(_sh(xy), **kw)


def _Draw(im): return _D(im)
import math

W, H, G = 520, 660, 530
OY = 100  # margines u gory: wszystko przesuniete o OY w dol          # kanwa, linia ziemi
C1, C2, C3, C4 = (46, 44, 40), (78, 74, 64), (120, 112, 96), (206, 200, 186)   # walory: cien, cialo, jasne, maska/kosc
SW = (255, 190, 80)              # swiatlo (jedyny kolor)
KW = (150, 120, 128)             # zwiedle kwiaty (przygaszony roz)


def gracz(d, x):
    s = 4
    d.rectangle([x, G - 32 * s, x + 8 * s, G - 24 * s], fill=(60, 60, 60))
    d.rectangle([x, G - 24 * s, x + 8 * s, G - 12 * s], fill=(60, 60, 60))
    d.rectangle([x - 4 * s, G - 24 * s, x, G - 12 * s], fill=(60, 60, 60)); d.rectangle([x + 8 * s, G - 24 * s, x + 12 * s, G - 12 * s], fill=(60, 60, 60))
    d.rectangle([x, G - 12 * s, x + 8 * s, G], fill=(60, 60, 60))


def blask(im, xy, r, sila=1.0):
    g = Image.new('RGBA', im.size, (0, 0, 0, 0)); dg = ImageDraw.Draw(g)
    dg.ellipse([xy[0] - r, xy[1] + OY - r, xy[0] + r, xy[1] + OY + r], fill=SW + (int(110 * sila),))
    im.alpha_composite(g.filter(ImageFilter.GaussianBlur(r / 2)))


def galaz(d, x, y, dl, kat, grub, kolor, rozw=2, kw=None):
    """rekurencyjny konar (kat w stopniach od pionu); kw = kepy kwiatow na koncach"""
    x2 = x + dl * math.sin(math.radians(kat)); y2 = y - dl * math.cos(math.radians(kat))
    d.line([x, y, x2, y2], fill=kolor, width=int(grub))
    if rozw and dl > 18:
        galaz(d, x2, y2, dl * 0.62, kat - 28, grub * 0.65, kolor, rozw - 1, kw)
        galaz(d, x2, y2, dl * 0.55, kat + 24, grub * 0.6, kolor, rozw - 1, kw)
    elif kw:
        d.ellipse([x2 - kw, y2 - kw * 0.7, x2 + kw, y2 + kw * 0.7], fill=KW)


def k1():
    """Straznik: zgarbiony drzewiec, brzozowa maska nisko przed barkami, klatka zeber z bursztynowym sercem, rece-klody
    do ziemi, na garbie korona zwiedlej sakury. Haczyk: SKRADZIONE SWIATLO w piersi - latarnie je oslabiaja."""
    im = Image.new('RGBA', (W, H), (139, 139, 139, 255)); d = _Draw(im)
    gracz(d, 470)
    # korona za garbem
    for x, k, dl in ((230, -38, 110), (270, -8, 130), (300, 26, 115), (250, -62, 70), (320, 55, 70)):
        galaz(d, x, 150, dl, k, 14, C2, 2, 16)
    # nogi krotkie, szeroko
    d.polygon([(180, G), (230, G), (240, 420), (205, 400), (170, 440)], fill=C1)
    d.polygon([(300, G), (350, G), (345, 440), (315, 400), (290, 420)], fill=C1)
    # tulow: szerokie barki -> waska talia, garb
    d.polygon([(150, 250), (190, 160), (270, 130), (360, 150), (395, 240), (360, 330), (330, 430), (215, 430), (185, 330)], fill=C2)
    d.polygon([(190, 160), (270, 130), (360, 150), (330, 180), (260, 170)], fill=C3)       # swiatlo na garbie
    # lewa reka (wieksza) - kloda do ziemi, piesc-korzen
    d.polygon([(150, 250), (190, 230), (175, 380), (160, 470), (110, 470), (115, 380)], fill=C2)
    d.polygon([(95, 470), (175, 470), (185, G), (85, G)], fill=C1)
    d.line([(112, 380), (80, 440)], fill=C1, width=10); d.line([(130, 330), (100, 300)], fill=C2, width=8)
    # prawa reka chudsza, dluzsze palce-korzenie
    d.polygon([(365, 240), (395, 240), (410, 360), (400, 470), (370, 470), (372, 360)], fill=C2)
    for dx in (-10, 4, 18, 30):
        d.line([(385, 465), (385 + dx, G)], fill=C1, width=6)
    # klatka zeber z sercem
    d.rectangle([232, 270, 312, 350], fill=C1)
    blask(im, (272, 310), 70, 1.0); d = _Draw(im)
    d.ellipse([252, 290, 292, 330], fill=SW)
    for y in (280, 300, 320, 340):
        d.line([(228, y), (316, y + 4)], fill=C3, width=6)
    # leb nisko przed barkami: brzozowa maska z jedna szpara
    d.polygon([(245, 185), (300, 185), (305, 250), (272, 272), (240, 250)], fill=C4)
    d.line([(252, 200), (262, 196)], fill=C1, width=3); d.line([(285, 230), (296, 226)], fill=C1, width=3)  # lenticele brzozy
    d.rectangle([255, 214, 290, 219], fill=SW)
    # mech zwisajacy z barkow i reki
    for x, dl in ((160, 60), (178, 40), (370, 50), (340, 70)):
        d.line([(x, 250), (x, 250 + dl)], fill=(70, 86, 50), width=5)
    return im


def k2():
    """Jelen Gaju: czworonozny olbrzym z korzeni, poroze z galezi sakury, na porozu wisza TRZY LATARNIE lochu
    (gracz je zapala = boss slabnie; w fazie 3 poroze plonie). Pysk: wydrazona kloda z jednym swiatlem w srodku."""
    im = Image.new('RGBA', (W, H), (139, 139, 139, 255)); d = _Draw(im)
    gracz(d, 470)
    # nogi: cienkie, dlugie, kolana do tylu (tylne)
    for (x1, y1, x2, y2, x3) in ((150, 330, 140, 430, 150), (205, 340, 215, 440, 200), (330, 330, 355, 420, 330), (375, 320, 400, 410, 385)):
        d.line([(x1, y1), (x2, y2), (x3, G)], fill=C1, width=20)
        d.polygon([(x3 - 16, G), (x3 + 16, G), (x3 + 8, G - 14), (x3 - 8, G - 14)], fill=C1)
    # tulow: beczka pochylona do przodu (lewo = przod), grzywa mchu
    d.polygon([(120, 250), (180, 215), (330, 225), (410, 260), (400, 330), (300, 350), (170, 345), (120, 310)], fill=C2)
    d.polygon([(180, 215), (330, 225), (300, 240), (190, 238)], fill=C3)
    for x in range(125, 230, 14):
        d.line([(x, 255), (x - 4, 300 + (x % 3) * 12)], fill=(70, 86, 50), width=6)
    # szyja i leb nisko z przodu
    d.polygon([(130, 250), (175, 225), (140, 170), (105, 180)], fill=C2)
    d.polygon([(70, 170), (140, 150), (150, 190), (120, 230), (80, 225)], fill=C4)           # pysk-kloda (jasny)
    d.ellipse([88, 180, 118, 210], fill=C1); blask(im, (103, 195), 30); d = _Draw(im); d.ellipse([96, 188, 110, 202], fill=SW)
    # poroze: wielkie, szerokie, z latarniami
    galaz(d, 118, 155, 120, -35, 12, C2, 2, 12); galaz(d, 135, 150, 130, 20, 12, C2, 2, 12)
    for (x, y) in ((58, 70), (175, 60), (222, 110)):
        d.line([(x, y - 22), (x, y)], fill=C1, width=3)
        blask(im, (x, y + 10), 26, 0.8); d = _Draw(im)
        d.rectangle([x - 8, y, x + 8, y + 20], fill=C1); d.rectangle([x - 5, y + 4, x + 5, y + 16], fill=SW)
    return im


def k3():
    """Matka Korzeni: pol-olbrzymka wyrastajaca z wielkiego pnia (nie chodzi - arena jest jej cialem), welon z mchu
    zaslania twarz, widac tylko dwa swiatelka; rece z dlugimi palcami-korzeniami; w dloniach trzyma ostatnia zywa
    kwiatowa latarnie (Serce Gaju) - gracz ja odbiera. Korzenie z pnia rozchodza sie po arenie (ataki z ziemi)."""
    im = Image.new('RGBA', (W, H), (139, 139, 139, 255)); d = _Draw(im)
    gracz(d, 470)
    # pien-tron + korzenie po ziemi
    d.polygon([(130, G), (390, G), (360, 400), (330, 360), (190, 360), (160, 400)], fill=C1)
    for x2 in (40, 90, 440, 500):
        d.line([(260, 520), (x2, G)], fill=C1, width=14)
    # tulow wyrastajacy z pnia, lekko pochylony
    d.polygon([(200, 370), (320, 370), (330, 250), (300, 190), (220, 190), (190, 250)], fill=C2)
    d.polygon([(220, 190), (300, 190), (310, 215), (212, 215)], fill=C3)
    # glowa + welon z mchu do pasa
    d.ellipse([228, 110, 292, 180], fill=C2)
    for x in range(222, 300, 8):
        d.line([(x, 150), (x + (x % 5) - 2, 230 + (x * 7) % 50)], fill=(64, 80, 46), width=6)
    blask(im, (250, 150), 16, 0.7); blask(im, (272, 150), 16, 0.7); d = _Draw(im)
    d.rectangle([246, 147, 252, 152], fill=SW); d.rectangle([268, 147, 274, 152], fill=SW)
    # korona: galezie z glowy jak rogi/wlosy
    galaz(d, 240, 118, 80, -30, 9, C2, 2, 12); galaz(d, 280, 118, 85, 28, 9, C2, 2, 12)
    # rece: dlugie, lokcie na zewnatrz, dlonie przed soba trzymaja latarnie-kwiat
    d.line([(205, 210), (130, 280), (205, 320)], fill=C2, width=22); d.line([(315, 210), (390, 280), (315, 320)], fill=C2, width=22)
    for dx in (-12, -4, 4, 12):
        d.line([(205, 320), (240 + dx, 300 + abs(dx))], fill=C1, width=5); d.line([(315, 320), (280 + dx, 300 + abs(dx))], fill=C1, width=5)
    blask(im, (260, 300), 60); d = _Draw(im)
    d.polygon([(248, 285), (272, 285), (278, 305), (260, 318), (242, 305)], fill=SW)
    return im


out = Image.new('RGB', (3 * W, H + 40), (139, 139, 139)); dr = ImageDraw.Draw(out)
for i, (f, t) in enumerate(((k1, 'K1 Straznik: skradzione swiatlo w piersi'), (k2, 'K2 Jelen Gaju: latarnie na porozu'), (k3, 'K3 Matka Korzeni: arena jest jej cialem'))):
    out.paste(f().convert('RGB'), (i * W, 40)); dr.text((i * W + 10, 12), t, fill=(255, 255, 255))
out.save(sys.argv[1])
