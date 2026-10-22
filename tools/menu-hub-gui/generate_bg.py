# ============================================================================
# NIEAKTUALNY — to jest stalowo-niebieski panel, odrzucony 2026-09-22.
# Aktualny generator tla:   generate_bg2.py   (cieply: dab + mosiadz + zielona skora)
# Aktualny generator ikon:  generate_icons2.py
# Plik zostaje jako odniesienie; nie generuj z niego do packa.
# ============================================================================
#!/usr/bin/env python3
"""Generuje tlo panelu menu-huba (glif Nexo 'menu_hub_bg').

CALA TECHNIKA TO JEDEN NALOZONY OBRAZEK. Glif wstawiony w tytul okna rysuje sie NAD
vanillowa tekstura skrzyni i POD ikonami przedmiotow (zmierzone na zywym kliencie,
output/menu-hub-qa/captures/25-glif-test.png). Nie ma tu zadnej innej magii — o wygladzie
menu decyduje wylacznie to, co jest na tym PNG. Dlatego ten plik jest dluzszy niz sam
mechanizm: to nie kod interfejsu, to rysunek.

ROZMIAR 176x168 = dokladny rozmiar GUI skrzyni 3-rzedowej w pikselach GUI (1x).
MUSI byc <= 256 px w kazdym wymiarze — Nexo odrzuca wieksze tekstury i podmienia je na
placeholder, a w logu jest tylko INFO, nie blad.

GDZIE TO LADUJE (liczone, nie zgadywane): tytul rysuje sie w (guiLeft+8, guiTop+6), glif
bitmapowy ma gore w 'y_tytulu + 7 - ascent' -> dla obrazka na caly panel ascent = 13.
Poziomo ustawia go sam tytul: shift -8, glif, shift -169 (advance glifu = 177).

PULAPKA: ImageDraw NIE MIESZA kolorow z tlem — kolor z alfa nadpisuje piksel prawie
przezroczystym, czyli robi w panelu dziure na swiat. Wszystko polprzezroczyste liczymy sami
(blend) i zapisujemy jako w pelni nieprzezroczyste.

Uzycie: python3 generate_bg.py [sciezka-wyjsciowa]
"""
import math
import random
import sys
from PIL import Image, ImageDraw, ImageFilter

W, H = 176, 168
OUT = sys.argv[1] if len(sys.argv) > 1 else "menu_hub_bg.png"

# Paleta wspolna z panelem ekwipunku i ikonami — menu ma byc jednym systemem.
INK        = (5, 7, 10)       # najciemniejszy cien
BODY_TOP   = (30, 41, 52)     # gora korpusu
BODY_BOT   = (12, 17, 23)     # dol korpusu
STAGE_TOP  = (9, 13, 18)      # gora sceny (wglebienie)
STAGE_BOT  = (5, 8, 11)
BEVEL_HI   = (104, 146, 172)  # swiatlo na fazie
BEVEL_LO   = (26, 38, 48)     # cien na fazie
FRAME      = (72, 108, 132)
GOLD       = (198, 162, 96)   # akcent ornamentu

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()
d = ImageDraw.Draw(img)


def blend(x, y, rgb, a):
    if not (0 <= x < W and 0 <= y < H):
        return
    r0, g0, b0, a0 = px[x, y]
    k = a / 255.0
    px[x, y] = (int(r0 + (rgb[0] - r0) * k),
                int(g0 + (rgb[1] - g0) * k),
                int(b0 + (rgb[2] - b0) * k),
                max(a0, 255))


def hline(x0, x1, y, rgb, a=255):
    for x in range(x0, x1 + 1):
        blend(x, y, rgb, a)


def vline(x, y0, y1, rgb, a=255):
    for y in range(y0, y1 + 1):
        blend(x, y, rgb, a)


def diament(cx, cy, r, rgb, a=255):
    """Pelny romb. Wczesniejsze reczne petle rysowaly polowe ksztaltu — na panelu
    wychodzilo z tego '<' i 'v' zamiast ornamentu."""
    for dy in range(-r, r + 1):
        w = r - abs(dy)
        for dx in range(-w, w + 1):
            blend(cx + dx, cy + dy, rgb, a)


def pion_gradient(x0, y0, x1, y1, top, bot, gamma=1.0):
    for y in range(y0, y1 + 1):
        t = ((y - y0) / max(1, (y1 - y0))) ** gamma
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        for x in range(x0, x1 + 1):
            px[x, y] = c + (255,)


# ============================================================================== KORPUS
pion_gradient(0, 0, W - 1, H - 1, BODY_TOP, BODY_BOT, gamma=0.9)

# Faktura: bardzo slaby szum, zeby plaszczyzna nie byla martwa. Ziarno stale, zeby kolejne
# generowanie dawalo IDENTYCZNY plik — inaczej kazdy build to nowy pack do pobrania.
random.seed(7)
for y in range(H):
    for x in range(W):
        n = random.randint(-4, 4)
        r, g, b, _ = px[x, y]
        px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)),
                    max(0, min(255, b + n)), 255)

# ============================================================================== RAMA
# Trzy warstwy: cien zewnetrzny, jasna faza od gory-lewej, ciemna od dolu-prawej.
d.rectangle([0, 0, W - 1, H - 1], outline=INK + (255,), width=2)
hline(2, W - 3, 2, BEVEL_HI, 210)
vline(2, 2, H - 3, BEVEL_HI, 210)
hline(2, W - 3, H - 3, BEVEL_LO, 230)
vline(W - 3, 2, H - 3, BEVEL_LO, 230)
d.rectangle([3, 3, W - 4, H - 4], outline=FRAME + (255,), width=1)
# Zaokraglenie naroznikow — kasujemy skrajne piksele, zeby rog nie byl ostry.
for cx, cy in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
    for dx, dy in ((0, 0), (1, 0), (0, 1)):
        x, y = (cx + dx if cx == 0 else cx - dx), (cy + dy if cy == 0 else cy - dy)
        px[x, y] = (0, 0, 0, 0)

# ============================================================================== NAGLOWEK
pion_gradient(5, 5, W - 6, 15, (34, 47, 60), (16, 23, 30))
hline(5, W - 6, 5, BEVEL_HI, 120)
hline(5, W - 6, 16, INK, 220)
hline(5, W - 6, 17, BEVEL_HI, 90)
# Zloty akcent pod poczatkiem napisu + romb na koncu — kotwiczy tytul po lewej.
hline(8, 60, 17, GOLD, 235)
diament(64, 17, 2, GOLD, 220)
blend(64, 17, (250, 232, 190), 255)

# ============================================================================== ORNAMENT
# Pas miedzy naglowkiem a scena. Rzad 0 kontenera (y 18..33) NIE JEST UZYWANY w zadnym
# ekranie huba — wszystkie kafelki siedza w rzedach 1 i 2 — wiec to miejsce moze byc
# dekoracja zamiast pustego pola.
hline(26, W - 27, 26, (52, 76, 94), 150)
diament(W // 2, 26, 3, (58, 86, 106), 210)
diament(W // 2, 26, 1, GOLD, 230)
for dx in (-1, 1):
    diament(W // 2 + dx * 12, 26, 1, (58, 86, 106), 190)

# ============================================================================== SCENA
# Wglebienie, w ktorym stoja kafelki — obejmuje DOKLADNIE rzedy 1 i 2 (y 36..69),
# z marginesem. Wczesniej scena siegala az do rzedu 0 i byla przez to pustawa.
SX0, SY0, SX1, SY1 = 7, 33, W - 8, 73
pion_gradient(SX0, SY0, SX1, SY1, STAGE_TOP, STAGE_BOT)
# cien wewnetrzny u gory i z lewej, swiatlo u dolu i z prawej = wglebienie
for i, a in enumerate((190, 110, 55)):
    hline(SX0, SX1, SY0 + i, INK, a)
    vline(SX0 + i, SY0, SY1, INK, a)
hline(SX0, SX1, SY1, BEVEL_HI, 120)
vline(SX1, SY0, SY1, BEVEL_HI, 90)
# Poswiata na srodku sceny — zeby ciemne pole nie bylo plaska plama.
for y in range(SY0 + 1, SY1):
    ty = abs((y - (SY0 + SY1) / 2) / ((SY1 - SY0) / 2))
    for x in range(SX0 + 1, SX1):
        tx = abs((x - W / 2) / ((SX1 - SX0) / 2))
        a = int(52 * max(0.0, 1 - ty * ty) * max(0.0, 1 - tx * tx))
        if a:
            blend(x, y, (86, 126, 156), a)

# Cienkie prowadnice miedzy rzedami kafelkow (slot rzedu 1: y 36..51, rzedu 2: y 54..69).
hline(16, W - 17, 53, (58, 84, 104), 110)

# ============================================================================== PODZIAL
# Miedzy menu a ekwipunkiem gracza — belka z ornamentem, nie goła kreska.
hline(6, W - 7, 78, INK, 220)
hline(6, W - 7, 79, BEVEL_HI, 110)
diament(W // 2, 78, 3, (14, 20, 26), 255)
diament(W // 2, 78, 2, GOLD, 200)

# ============================================================================== SLOTY GRACZA
# Plecak: y = 86, 104, 122; hotbar: y = 144. Wglebienie = ciemne pole + faza.
def socket(x, y, s=16):
    pion_gradient(x, y, x + s - 1, y + s - 1, (7, 10, 14), (3, 5, 7))
    hline(x, x + s - 1, y, INK, 200)
    vline(x, y, y + s - 1, INK, 200)
    hline(x, x + s - 1, y + s - 1, BEVEL_HI, 95)
    vline(x + s - 1, y, y + s - 1, BEVEL_HI, 75)


for row_y in (86, 104, 122, 144):
    for col in range(9):
        socket(8 + col * 18, row_y)
# Hotbar oddzielony samym odstepem — zlota nitka w tym miejscu czytala sie jak zabladzona
# kreska, a nie jak podzial.

# ============================================================================== CIEN POD PANELEM
# Miekki cien przy dolnej krawedzi — panel ma "lezec na czyms", a nie wisiec.
cien = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(cien).rectangle([6, H - 6, W - 7, H - 1], fill=(0, 0, 0, 120))
img.alpha_composite(cien.filter(ImageFilter.GaussianBlur(2)))

img.save(OUT)
print(f"{OUT}: {img.size[0]}x{img.size[1]}")
