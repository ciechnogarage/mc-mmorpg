"""Kosiarz (Loch 004 Łany Strachów, Akt 1 v2) — 3 sylwetki 2D: 4 walory + jedno światło (oczy-świetliki jak u Strachów).
Skala: 4 px obrazu = 1 px modelu (gracz 32 px modelu = 128 px). Uruchom: python3 kosiarz_szkice.py OUT.png"""
import sys, math
from PIL import Image, ImageDraw, ImageFilter

W, H, G = 520, 700, 650
C1, C2, C3, C4 = (44, 42, 38), (82, 76, 64), (128, 118, 96), (204, 186, 132)   # cień, płótno, jasne, słoma
SW = (255, 186, 70)


def gracz(d, x):
    s = 4; c = (60, 60, 60)
    d.rectangle([x, G - 32 * s, x + 8 * s, G - 24 * s], fill=c); d.rectangle([x, G - 24 * s, x + 8 * s, G - 12 * s], fill=c)
    d.rectangle([x - 4 * s, G - 24 * s, x, G - 12 * s], fill=c); d.rectangle([x + 8 * s, G - 24 * s, x + 12 * s, G - 12 * s], fill=c)
    d.rectangle([x, G - 12 * s, x + 8 * s, G], fill=c)


def blask(im, xy, r, sila=1.0):
    g = Image.new('RGBA', im.size, (0, 0, 0, 0)); ImageDraw.Draw(g).ellipse([xy[0] - r, xy[1] - r, xy[0] + r, xy[1] + r], fill=SW + (int(120 * sila),))
    im.alpha_composite(g.filter(ImageFilter.GaussianBlur(r / 2)))


def wiechec(d, x, y, kat, dl=26, n=5, kolor=C4):
    for i in range(n):
        a = math.radians(kat + (i - n / 2) * 11)
        d.line([(x, y), (x + dl * math.sin(a), y + dl * math.cos(a))], fill=kolor, width=3)


def kosa(d, uch, kat, dl, ostrze_w_lewo=True):
    """kosisko od punktu uchwytu w kierunku kat (stopnie od pionu w dół), ostrze na końcu"""
    a = math.radians(kat); x2, y2 = uch[0] + dl * math.sin(a), uch[1] + dl * math.cos(a)
    d.line([uch, (x2, y2)], fill=C1, width=9)
    s = -1 if ostrze_w_lewo else 1
    pts = [(x2, y2), (x2 + s * 70, y2 + 6), (x2 + s * 150, y2 + 34), (x2 + s * 70, y2 + 18)]
    d.polygon(pts, fill=(170, 170, 160)); d.line(pts[:3], fill=(226, 226, 214), width=3)
    return x2, y2


def k1():
    """Żniwiarz w pochyle: wysoki, zgarbiony, kosa nisko w poprzek (postawa koszenia), szeroki kapelusz z pędami,
    twarz-worek w cieniu ronda z dwoma świetlikami, słoma z mankietów i kolan. Haczyk: zawsze kosi — pas przed nim."""
    im = Image.new('RGBA', (W, H), (139, 139, 139, 255)); d = ImageDraw.Draw(im)
    gracz(d, 470)
    # nogi: długie, ugięte w kolanach, onuce z powrósłem
    d.line([(225, 410), (205, 520), (215, G)], fill=C2, width=30); d.line([(275, 410), (300, 520), (290, G)], fill=C2, width=30)
    for (x, y) in ((205, 520), (300, 520)): wiechec(d, x, y, 0, 22, 5); d.line([(x - 14, y + 30), (x + 14, y + 34)], fill=C1, width=4)
    d.rectangle([195, G - 14, 232, G], fill=C1); d.rectangle([272, G - 14, 312, G], fill=C1)
    # tułów: chudy, pochylony do przodu (w lewo), koszula z płótna, pas z powrósła
    d.polygon([(205, 420), (300, 420), (310, 300), (270, 220), (190, 235), (180, 320)], fill=C2)
    d.polygon([(190, 235), (270, 220), (262, 250), (196, 262)], fill=C3)
    d.line([(200, 395), (305, 390)], fill=C4, width=8)
    wiechec(d, 300, 300, 160, 20, 4)                       # słoma z dziury pod pachą
    # ręce: długie, obie na kosisku przed sobą, nisko
    d.line([(200, 250), (150, 330), (120, 400)], fill=C2, width=22); d.line([(268, 238), (240, 330), (185, 395)], fill=C2, width=22)
    wiechec(d, 120, 400, 20, 18, 5); wiechec(d, 185, 395, 30, 18, 5)
    kosa(d, (205, 360), 225 - 180 + 200, 0)
    a = math.radians(-58); x0, y0 = 255, 330
    d.line([(x0, y0), (x0 - 300 * math.cos(math.radians(32)), y0 + 300 * math.sin(math.radians(32)))], fill=C1, width=10)   # kosisko w poprzek
    xe, ye = x0 - 300 * math.cos(math.radians(32)), y0 + 300 * math.sin(math.radians(32))
    blade = [(xe, ye), (xe + 60, ye + 22), (xe + 150, ye + 26), (xe + 80, ye + 10)]
    d.polygon(blade, fill=(170, 170, 160)); d.line(blade[:3], fill=(226, 226, 214), width=3)
    # głowa-worek nisko przed barkami, kapelusz szeroki
    d.ellipse([150, 175, 215, 245], fill=C3)
    d.polygon([(110, 190), (255, 168), (250, 182), (115, 205)], fill=C4)                  # rondo
    d.polygon([(150, 186), (220, 175), (212, 140), (160, 146)], fill=C4)                  # główka kapelusza
    d.line([(175, 145), (165, 105)], fill=(70, 96, 44), width=5); d.line([(195, 142), (215, 110)], fill=(70, 96, 44), width=5)  # pędy
    d.polygon([(150, 200), (215, 190), (212, 215), (152, 222)], fill=C1)                  # cień ronda na twarzy
    blask(im, (170, 210), 14); blask(im, (196, 206), 14); d = ImageDraw.Draw(im)
    d.rectangle([166, 207, 173, 213], fill=SW); d.rectangle([192, 203, 199, 209], fill=SW)
    return im


def k2():
    """Strach zerwany z krzyża: na barkach dalej belka krzyża, ręce przywiązane wzdłuż niej (sylwetka T), nogi-tyczki,
    kosa przywiązana do końca belki jak trzecie ramię. Haczyk: obraca się całym krzyżem — zamach = obrót belki."""
    im = Image.new('RGBA', (W, H), (139, 139, 139, 255)); d = ImageDraw.Draw(im)
    gracz(d, 470)
    d.line([(235, 430), (215, G)], fill=C1, width=16); d.line([(265, 430), (290, G)], fill=C1, width=16)
    d.polygon([(205, 440), (295, 440), (300, 300), (200, 300)], fill=C2)
    for x in range(208, 296, 16): d.line([(x, 440), (x - 4, 470)], fill=C4, width=4)      # słomiana frędzla płaszcza
    d.rectangle([60, 285, 440, 305], fill=C1)                                            # belka
    d.line([(200, 300), (90, 300)], fill=C2, width=24); d.line([(300, 300), (400, 300)], fill=C2, width=24)
    wiechec(d, 85, 305, 10, 24, 6); wiechec(d, 405, 305, -10, 24, 6)
    d.line([(440, 295), (470, 520)], fill=C1, width=8)
    blade = [(470, 520), (420, 540), (340, 548), (410, 530)]; d.polygon(blade, fill=(170, 170, 160))
    d.ellipse([215, 210, 285, 290], fill=C3)                                              # dynia/worek
    d.polygon([(185, 225), (315, 225), (300, 200), (200, 200)], fill=C4); d.polygon([(220, 205), (280, 205), (270, 160), (230, 160)], fill=C4)
    blask(im, (238, 255), 14); blask(im, (262, 255), 14); d = ImageDraw.Draw(im)
    d.rectangle([234, 251, 242, 259], fill=SW); d.rectangle([258, 251, 266, 259], fill=SW)
    return im


def k3():
    """Snopiarz: tułów to wiązany snop (beczka słomy z powrósłem w pasie), cienkie ręce i nogi z płótna, mały kapelusz,
    kosa na ramieniu ostrzem do przodu. Haczyk: brzuch-snop chroni — cios w tułów grzęźnie w słomie."""
    im = Image.new('RGBA', (W, H), (139, 139, 139, 255)); d = ImageDraw.Draw(im)
    gracz(d, 470)
    d.line([(225, 470), (215, G)], fill=C2, width=22); d.line([(285, 470), (295, G)], fill=C2, width=22)
    d.ellipse([170, 290, 340, 490], fill=C4)                                              # snop
    for x in range(180, 335, 12): d.line([(x, 300 + abs(x - 255) * .3), (x + 2, 480 - abs(x - 255) * .3)], fill=C3, width=2)
    d.line([(178, 395), (334, 395)], fill=C1, width=10)
    d.line([(185, 330), (150, 410), (175, 470)], fill=C2, width=18); d.line([(325, 320), (365, 260), (335, 220)], fill=C2, width=18)
    wiechec(d, 175, 470, 0, 18, 5)
    d.line([(335, 225), (180, 120)], fill=C1, width=9)
    blade = [(180, 120), (150, 170), (120, 250), (165, 170)]; d.polygon(blade, fill=(170, 170, 160))
    d.ellipse([225, 220, 285, 285], fill=C3)
    d.polygon([(205, 238), (305, 232), (296, 220), (212, 224)], fill=C4); d.polygon([(228, 226), (282, 222), (276, 196), (232, 198)], fill=C4)
    blask(im, (243, 255), 12); blask(im, (266, 254), 12); d = ImageDraw.Draw(im)
    d.rectangle([239, 251, 246, 257], fill=SW); d.rectangle([262, 250, 269, 256], fill=SW)
    return im


out = Image.new('RGB', (3 * W, H + 40), (139, 139, 139)); dr = ImageDraw.Draw(out)
for i, (f, t) in enumerate(((k1, 'K1 Zniwiarz w pochyle: zawsze kosi'), (k2, 'K2 Strach zerwany z krzyza: belka = zamach'),
                            (k3, 'K3 Snopiarz: cios w snop grzeznie'))):
    out.paste(f().convert('RGB'), (i * W, 40)); dr.text((i * W + 10, 12), t, fill=(255, 255, 255))
out.save(sys.argv[1])
