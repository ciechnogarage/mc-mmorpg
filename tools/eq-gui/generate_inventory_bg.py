#!/usr/bin/env python3
"""Tlo vanillowego ekranu ekwipunku (klawisz E) — kierunek "KUTA STAL I BRAZ".
   -> assets/minecraft/textures/gui/container/inventory.png

Panel = 176x166 px w lewym gornym rogu tekstury 256x256 (zmierzone na client.jar 1.21.11).
Gra skaluje teksture x3 przy 1280x720 (guiScale auto), wiec 1 px = 3 px na ekranie:
detale 1-pikselowe PRZEZYJA, szum losowy zamieni sie w brud. Dlatego zero random().

KIERUNEK ARTYSTYCZNY (spisany PRZED rysowaniem):
  MATERIAL   kuta plyta stalowa, szczotkowana skosnie, z odlewanymi okuciami z brazu.
  SWIATLO    jedno zrodlo, gora-lewo, 45 st. Element WYPUKLY: jasna krawedz gora/lewo,
             ciemna dol/prawo. Element WKLESLY: odwrotnie. Bez wyjatkow.
  PALETA     10 kolorow. Braz WYLACZNIE tam, gdzie chodzi o cialo postaci
             (portret, zbroja, akcesoria, hotbar, tabliczka). Plecak = zimna stal.
  ORNAMENT   tylko na stykach powierzchni: nity 2x2, narozne okucia L, szyna brazowa.
  HIERARCHIA 1. model gracza  2. szyna akcesoriow  3. zbroja + druga reka
             4. hotbar  5. plecak  6. crafting

RYTM: strefy na przemian WKLESLE (postac, crafting, polka, plecak) i WYPUKLE
(szyna akcesoriow, hotbar).

OGRANICZENIA KLIENTA (pack ich nie dotyka):
  * napis "Crafting" rysuje klient kolorem #404040 w y 6..13, x 97..136 — na ciemnym
    tle znika. Dlatego lezy pod nim JASNA TABLICZKA Z BRAZU: jedyny jasny element
    na ekranie jest uzasadniony konstrukcyjnie (szyld), a nie przypadkowy.
  * przycisk ksiegi receptur (x ~104..123, y ~61..79) rysuje klient ze sprite'ow
    gui/sprites/recipe_book/* — poza zakresem tego pliku.
"""
import sys
from PIL import Image, ImageDraw

W = H = 256
PW, PH = 176, 166
OUT = sys.argv[1] if len(sys.argv) > 1 else "inventory.png"

# ---------------------------------------------------------------- PALETA (10)
INK      = (  7,   9,  13)
VOID     = ( 26,  32,  40)
WELL_LO  = ( 46,  56,  67)
PLATE_D  = ( 44,  53,  63)
PLATE    = ( 58,  68,  80)
PLATE_L  = ( 88, 101, 116)
STEEL_HI = (140, 152, 166)
BRONZE_D = ( 74,  55,  24)
BRONZE   = (138, 106,  48)
BRONZE_L = (216, 185, 120)

BRUSH    = (-3, 0, 2, -1, 3, 1, -2, 1)

RECESS_T = (38, 46, 56)
RECESS_B = (30, 37, 45)

COLS    = [8, 26, 44, 62, 80, 98, 116, 134, 152]
ARMOR_Y = [8, 26, 44, 62]
ACC_Y   = 84
ACC_COLS = COLS[:6]             # indeksy 9..14; pozostale trzy to zwykly plecak
BAG_Y   = [102, 120]
HOT_Y   = 142

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()
d = ImageDraw.Draw(img)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def put(x, y, c):
    if 0 <= x < PW and 0 <= y < PH:
        px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (255,)


def hline(x0, x1, y, c):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        put(x, y, c)


def vline(x, y0, y1, c):
    for y in range(min(y0, y1), max(y0, y1) + 1):
        put(x, y, c)


# ============================================================ 1. PLYTA BAZOWA
for y in range(PH):
    t = y / (PH - 1)
    base = mix(PLATE, PLATE_D, t ** 0.85)
    for x in range(PW):
        brush = BRUSH[(y * 5 + x // 9) % 8]
        if y % 12 == 0:
            brush -= 4          # przekucie: poziomy slad mlota co 12 px
        elif y % 12 == 1:
            brush += 3          # i jego rozswietlona dolna krawedz
        put(x, y, (base[0] + brush, base[1] + brush, base[2] + brush))

# ============================================================ 2. RAMA ZEWNETRZNA
d.rectangle([0, 0, PW - 1, PH - 1], outline=INK + (255,))
hline(1, PW - 2, 1, STEEL_HI)
vline(1, 1, PH - 2, STEEL_HI)
hline(1, PW - 2, PH - 2, INK)
vline(PW - 2, 1, PH - 2, INK)
hline(2, PW - 3, 2, PLATE_L)
vline(2, 2, PH - 3, PLATE_L)
hline(2, PW - 3, PH - 3, PLATE_D)
vline(PW - 3, 2, PH - 3, PLATE_D)
for cx, cy in ((0, 0), (PW - 1, 0), (0, PH - 1), (PW - 1, PH - 1)):
    px[cx, cy] = (0, 0, 0, 0)


def bolt(x, y, mid=BRONZE, lit=BRONZE_L, dark=BRONZE_D):
    put(x, y, lit); put(x + 1, y, mid)
    put(x, y + 1, mid); put(x + 1, y + 1, dark)


def bracket(x, y, dx, dy, n=11):
    for i in range(n):
        c = BRONZE if i < n - 3 else BRONZE_D
        put(x + dx * i, y, c)
        put(x, y + dy * i, c)
        if i < n - 4:
            put(x + dx * i, y + dy, BRONZE_D)
            put(x + dx, y + dy * i, BRONZE_D)
    put(x, y, BRONZE_L)
    put(x + dx, y + dy, BRONZE)


for (bx, by, dx, dy) in ((2, 2, 1, 1), (PW - 3, 2, -1, 1),
                         (2, PH - 3, 1, -1), (PW - 3, PH - 3, -1, -1)):
    bracket(bx, by, dx, dy)


# ============================================================ 3. HELPERY STREF
def recess(x0, y0, x1, y1, top=RECESS_T, bot=RECESS_B):
    for y in range(y0, y1 + 1):
        t = (y - y0) / max(1, y1 - y0)
        c = mix(top, bot, t)
        for x in range(x0, x1 + 1):
            b = BRUSH[(y * 5 + x // 9) % 8] // 2
            if y % 12 == 0:
                b -= 2
            elif y % 12 == 1:
                b += 2
            put(x, y, (c[0] + b, c[1] + b, c[2] + b))
    hline(x0, x1, y0, INK)
    vline(x0, y0, y1, INK)
    hline(x0 + 1, x1, y1, PLATE_L)
    vline(x1, y0 + 1, y1, PLATE_L)


def raised(x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        t = (y - y0) / max(1, y1 - y0)
        c = mix(mix(PLATE, PLATE_L, 0.35), PLATE_D, t * 0.8)
        for x in range(x0, x1 + 1):
            b = BRUSH[(y * 5 + x // 9) % 8]
            put(x, y, (c[0] + b, c[1] + b, c[2] + b))
    hline(x0, x1, y0, STEEL_HI)
    vline(x0, y0, y1, STEEL_HI)
    hline(x0, x1, y1, INK)
    vline(x1, y0, y1, INK)


def well(x, y, w=16, h=16, rim=PLATE_L):
    for yy in range(h):
        t = yy / max(1, h - 1)
        c = mix(VOID, WELL_LO, t ** 0.75)
        for xx in range(w):
            put(x + xx, y + yy, c)
    hline(x, x + w - 1, y, mix(VOID, INK, 0.55))
    vline(x, y, y + h - 1, mix(VOID, INK, 0.35))
    hline(x - 1, x + w - 1, y - 1, INK)
    vline(x - 1, y - 1, y + h - 1, INK)
    rim2 = mix(PLATE, rim, 0.45) if rim == PLATE_L else rim
    hline(x, x + w, y + h, rim2)
    vline(x + w, y, y + h, rim2)
    put(x - 1, y + h, mix(INK, rim2, 0.5))
    put(x + w, y - 1, mix(INK, rim2, 0.5))


def engrave_h(x0, x1, y, cut=PLATE_D, lip=PLATE_L):
    hline(x0, x1, y, cut)
    hline(x0, x1, y + 1, lip)


# ============================================================ 4. STREFA POSTAC
recess(3, 3, 93, 78)

# --- okno portretu: ciemne, z miekkim stozkiem swiatla od gory ---
for yy in range(8, 78):
    for xx in range(26, 76):
        dx = (xx - 50.5) / 26.0
        dy = (yy - 26.0) / 52.0
        g = max(0.0, 1.0 - (dx * dx + dy * dy) ** 0.5)
        put(xx, yy, (7 + 21 * g, 9 + 18 * g, 12 + 13 * g))

# --- cokol: postac ma na czym stac; brazowa krawedz lapie swiatlo z gory ---
hline(34, 67, 72, (40, 33, 20))
hline(33, 68, 73, BRONZE)
hline(33, 68, 74, mix(BRONZE_D, BRONZE, 0.35))
for i, yy in enumerate(range(75, 78)):
    hline(32 - i, 69 + i, yy, mix((26, 22, 16), (11, 10, 9), i / 2.0))

# --- rama portretu: najmocniejszy braz na ekranie ---
hline(25, 76, 7, BRONZE_L)
vline(25, 7, 78, BRONZE)
vline(76, 7, 78, BRONZE_D)
hline(25, 76, 78, BRONZE_D)
put(25, 7, BRONZE_L); put(76, 7, BRONZE)

# --- kolumna zbroi: pionowa szyna mocujaca z brazu, stalowe wneki ---
vline(5, 7, 78, BRONZE_L)
vline(6, 7, 78, BRONZE_D)
for y in ARMOR_Y:
    well(8, y)
for ay in ARMOR_Y:
    bolt(5, ay + 6)

# --- druga reka ---
for ry in (14, 28, 42, 56):
    bolt(76, ry)
# ITERACJA 4: wyryty herb tarczy lezal 4 px nad duchem tarczy w slocie — dwie
# tarcze jedna pod druga czytaly sie jak pomylka. Zamiast ornamentu: uczciwy
# STOJAK. Wpuszczony kanal na calej wysokosci, brazowa szyna z nitami po lewej,
# a u jego dolu wisi slot tarczy. Pusta plyta przestaje byc niedokonczona.
recess(78, 8, 92, 59, top=(30, 37, 45), bot=(24, 30, 37))
well(77, 62)

# ============================================================ 5. STREFA CRAFTING
recess(95, 3, 172, 55, top=(33, 40, 49), bot=(26, 32, 40))

# --- BELKA NAGLOWKA sekcji: jedyne miejsce, gdzie klient pisze #404040.
#     Pelna szerokosc sekcji, zeby czytala sie jak naglowek, a nie jak lata. ---
# ITERACJA 2: byla to belka przez cala sekcje (76x12) i krzyczala glosniej niz
# postac — odwrocona hierarchia. Teraz tabliczka ma szerokosc SAMEGO napisu.
for yy in range(4, 16):
    t = (yy - 4) / 11.0
    hline(95, 141, yy, mix((206, 176, 112), (176, 146, 86), t))
hline(95, 141, 4, (234, 210, 156))
vline(95, 4, 15, (228, 204, 150))
hline(95, 141, 15, BRONZE_D)
vline(141, 4, 15, BRONZE_D)
put(95, 15, BRONZE); put(141, 4, BRONZE)
hline(96, 142, 16, INK)          # cien rzucany przez tabliczke
vline(142, 5, 16, INK)

# --- gniazda 2x2 we wlasnym, glebszym wcieciu ---
recess(96, 17, 133, 53, top=(28, 34, 42), bot=(22, 27, 34))
for cy in (18, 36):
    for cx in (98, 116):
        well(cx, cy)

# --- strzalka: WYRYTA w plycie, nie naklejona ---
# strzalka WYPUKLA: swiatlo gora/lewo, cien dol/prawo — wyryta ginela na wcieciu
for i in range(4):
    hline(137, 144, 30 + i, PLATE_L if i == 0 else PLATE)
hline(137, 144, 34, INK)
for i in range(6):
    vline(144 + i, 27 + i, 37 - i, PLATE)
for i in range(6):
    put(144 + i, 27 + i, PLATE_L)
    put(144 + i, 37 - i, INK)
put(150, 32, STEEL_HI)

# --- wynik: wneka w plytkiej oprawie, zeby nie gubila sie przy krawedzi ---
recess(152, 25, 171, 46, top=(28, 34, 42), bot=(22, 27, 34))
well(154, 28)

# ============================================================ 6. POLKA (prawa)
recess(95, 57, 172, 78, top=(36, 44, 53), bot=(28, 35, 43))
bolt(97, 60)
bolt(168, 60)

# ============================================================ 7. SZYNA AKCESORIOW
# Szesciopolowa szyna: prawa krawedz pokrywa sie z obrzezem szostej wneki.
# Nastepna wneka zaczyna sie zaraz obok, wiec zadnej kolumny nie przesuwamy.
hline(3, 114, 78, BRONZE)
put(3, 78, BRONZE_L); put(114, 78, BRONZE_L)
recess(115, 79, 172, 100, top=(32, 39, 48), bot=(25, 31, 39))
raised(3, 79, 114, 100)
hline(4, 113, 80, BRONZE_D)
vline(4, 80, 100, BRONZE)
for x in ACC_COLS:
    well(x, ACC_Y)
for x in COLS[6:]:
    well(x, ACC_Y)
# dolna szyna rysowana PO wnekach, zeby byla ciagla (obrzeza wnek by ja przerwaly)
hline(4, 114, 100, BRONZE)
vline(114, 80, 100, BRONZE_D)
put(4, 100, BRONZE_L); put(114, 100, BRONZE_D)
bolt(5, 90); bolt(111, 90)
# Dawny nit w prawym gornym rogu zastap takim samym metalem jak po lewej.
for yy in (81, 82):
    for xx in (112, 113):
        put(xx, yy, px[5 + (xx - 112), yy][:3])

# ============================================================ 8. PLECAK
recess(3, 101, 172, 137, top=(32, 39, 48), bot=(25, 31, 39))
for y in BAG_Y:
    for x in COLS:
        well(x, y)

hline(3, 172, 138, INK)

# ============================================================ 9. HOTBAR
raised(3, 139, 172, 162)
for x in COLS:
    well(x, HOT_Y)
bolt(4, 148); bolt(170, 148)
hline(5, 170, 159, PLATE_D)
hline(5, 170, 160, PLATE_L)
for bxx in (30, 60, 90, 120, 145):
    bolt(bxx, 160, mid=mix(BRONZE, PLATE, 0.45), lit=BRONZE, dark=BRONZE_D)

img.save(OUT)
print(OUT + ": panel %dx%d w teksturze %dx%d" % (PW, PH, W, H))
