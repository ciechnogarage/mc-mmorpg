#!/usr/bin/env python3
"""PANEL menu-huba 176x168 — przejscie z blokowania 3b na kolor.

PLAN SWIATLA, wypisany zanim cokolwiek narysuje (nauka ze swiecy):
  * Fazy KONSTRUKCYJNE (gniazda, wglebienia, rama) — swiatlo z lewej-gory.
    To jest konwencja vanilli i to ona sprawia, ze gniazdo czyta sie jako dziura.
    Tego nie ruszam, bo to funkcja, nie ozdoba.
  * Latarnia w prawym gornym rogu to akcent LOKALNY, nakladany wylacznie na
    OSOBNE ELEMENTY (okucie, ornament, listwa), nigdy jako gradient po plaskiej
    powierzchni. Trzy razy z rzedu poswiata liczona promieniem dala twarda plame
    z widoczna krawedzia — w pixel-arcie swiatlo siedzi w KRAWEDZIACH.

PALETY — rampy z rotacja odcienia, indeks 0 = najjasniejszy.
"""
import sys
sys.path.insert(0, '/pxlib')
from PIL import Image
from warsztat import rampa

W, H = 176, 168

DAB   = rampa(34, 0.30, 0.80, 21, 0.78, 0.20, 7)   # dab — korpus panelu
# UWAGA: te wartosci sa dobrane do panelu renderowanego BEZ tintu. Glif panelu
# siedzi w tytule okna, a tytul skrzyni ma domyslnie kolor 0x404040 — jesli przed
# glifem zabraknie '&f', cala tekstura leci przez mnoznik ~0.25 i panel jest mulisty.
MOS   = rampa(50, 0.35, 0.98, 24, 0.95, 0.22, 7)   # mosiadz — okucia i listwy
_W = sys.argv[1] if len(sys.argv) > 1 else 'zielen'
SKORY = {
    'braz':   rampa(24, 0.55, 0.34, 15, 0.78, 0.07, 6),
    'zielen': rampa(126, 0.50, 0.42, 148, 0.78, 0.12, 6),
    'krew':   rampa(14, 0.60, 0.42, 2, 0.88, 0.10, 6),
}
SKORA = SKORY[_W]   # wglebienia
CIEN  = (0x0d, 0x07, 0x04, 255)

img = Image.new('RGBA', (W, H), DAB[3])
px = img.load()


def blok(x0, y0, x1, y1, c):
    for y in range(max(0, y0), min(H, y1 + 1)):
        for x in range(max(0, x0), min(W, x1 + 1)):
            px[x, y] = c


def wneka(x0, y0, x1, y1, pal, dno, gora, dol):
    """Wglebienie: ciemno u gory i z lewej, jasniej u dolu i z prawej."""
    blok(x0, y0, x1, y1, pal[dno])
    blok(x0, y0, x1, y0, pal[gora])
    blok(x0, y0, x0, y1, pal[gora])
    blok(x0, y1, x1, y1, pal[dol])
    blok(x1, y0, x1, y1, pal[dol])


# ============================================================ KORPUS I SLOJE
blok(0, 0, W - 1, H - 1, DAB[3])
# SLOJE POZIOME USUNIETE: przy 54 gniazdach deska widac tylko w 2-px szczelinach,
# wiec smugi albo chowaly sie pod gniazdami, albo czytaly sie jak zabladzone rysy.
# Material niesie zamiast nich PIONOWA spoina desek — a te mieszcza sie w szczelinie.
SPOINY = (60, 114)

# ============================================================ RAMA
for y in range(H):
    for i, v in enumerate((1, 2, 2, 4, 3, 6)):
        px[i, y] = DAB[v]
    for i, v in enumerate((6, 4, 5, 4, 3, 5)):
        px[W - 6 + i, y] = DAB[v]
for i, v in enumerate((1, 2, 2, 4, 3, 6)):
    blok(0, i, W - 1, i, DAB[v])
for i, v in enumerate((6, 4, 5, 4, 3, 5)):
    blok(0, H - 6 + i, W - 1, H - 6 + i, DAB[v])

# ============================================================ OKUCIA NAROZNE
for cx, cy in ((0, 0), (W - 8, 0), (0, H - 8), (W - 8, H - 8)):
    blok(cx, cy, cx + 7, cy + 7, MOS[3])
    blok(cx, cy, cx + 7, cy, MOS[1])
    blok(cx, cy, cx, cy + 7, MOS[1])
    blok(cx, cy + 7, cx + 7, cy + 7, MOS[5])
    blok(cx + 7, cy, cx + 7, cy + 7, MOS[5])
    blok(cx + 2, cy + 2, cx + 5, cy + 5, MOS[5])
    blok(cx + 3, cy + 3, cx + 4, cy + 4, MOS[0])
# kapitele i bazy na bokach ramy — zeby pion mial poczatek i koniec
for cy in (18, 33, 74, 160):
    blok(0, cy, 5, cy, MOS[2])
    blok(W - 6, cy, W - 1, cy, MOS[4])

# ============================================================ NAGLOWEK
# Tabliczka tytulu konczy sie w x=142 — dalej zostaje drewno, na ktorym wisi
# latarnia. Recesja na cala szerokosc dawala plaski czarny pas i latarnie
# zawieszona nad dziura.
wneka(6, 6, 142, 16, SKORA, 5, 5, 2)
blok(7, 7, 141, 7, CIEN)
for pxp, pyp in ((8, 8), (140, 8), (8, 14), (140, 14)):
    px[pxp, pyp] = MOS[2]
blok(6, 17, W - 7, 17, MOS[1])
blok(6, 18, W - 7, 18, MOS[5])

# ============================================================ FRYZ
blok(6, 19, W - 7, 33, DAB[4])
blok(6, 19, W - 7, 19, MOS[3])            # mosiezna listwa nad fryzem
blok(6, 20, W - 7, 20, MOS[5])
blok(6, 21, W - 7, 21, DAB[6])            # cien rzucany przez listwe
# DENTIKULY — rzad kostek. Rytm, ktory czyta sie przy 1x, i realny powod, zeby
# pas nie byl plaski. Cien po prawej i pod spodem, zeby kostka miala grubosc.
for kx in range(9, W - 12, 8):
    blok(kx, 23, kx + 4, 29, DAB[2])
    blok(kx, 23, kx + 4, 23, DAB[1])
    blok(kx + 5, 23, kx + 5, 30, DAB[6])
    blok(kx, 30, kx + 5, 30, DAB[6])
blok(6, 31, W - 7, 31, DAB[5])
blok(6, 32, W - 7, 32, MOS[4])            # listwa pod fryzem
blok(6, 33, W - 7, 33, MOS[6] if len(MOS) > 6 else MOS[5])

# ============================================================ POLE KAFELKOW
# PROJEKT IDZIE ZA TRESCIA, nie odwrotnie. Hub uzywa 3-10 slotow z 27:
#   rzad 1 (sloty 10..16) = TRESC, 1..7 kafelkow wokol srodka
#   rzad 2 = tylko slot 18 (Wstecz, kol 0), 26 (Zamknij, kol 8) i 22 (kol 4, tylko
#            na ekranie glownym)
# Rysowanie 18 jednakowych gniazd dawalo graczowi siatke pustych dziur z trzema
# ikonami w srodku. Zamiast tego: gladka plyta pod trescia + belka nawigacyjna.
wneka(6, 34, W - 7, 72, SKORA, 2, 4, 0)
blok(7, 35, W - 8, 35, CIEN)

# --- plyta rzedu 1: bez gniazd, ikony stoja na polce
blok(8, 36, W - 9, 51, SKORA[2])
blok(8, 36, W - 9, 36, SKORA[4])          # cien pod gorna krawedzia wneki
blok(8, 51, W - 9, 51, SKORA[1])          # swiatlo tuz nad polka
blok(8, 52, W - 9, 52, MOS[2])            # POLKA — ikony maja na czym stac
blok(8, 53, W - 9, 53, MOS[5])
# TLOCZENIE: linia wgnieciona 3 px od krawedzi + szew. To, a nie gradient,
# sprawia, ze plaski prostokat czyta sie jako skora.
blok(11, 39, W - 12, 39, SKORA[4])
blok(11, 48, W - 12, 48, SKORA[4])
blok(11, 39, 11, 48, SKORA[4])
blok(W - 12, 39, W - 12, 48, SKORA[4])
blok(11, 40, W - 12, 40, SKORA[1])        # swiatlo tuz pod wgnieceniem
for x in range(13, W - 13, 4):            # szew, przerywany
    px[x, 38] = SKORA[0]
    px[x, 49] = SKORA[0]
for cxp, cyp in ((11, 39), (W - 12, 39), (11, 48), (W - 12, 48)):
    blok(cxp - 1, cyp - 1, cxp + 1, cyp + 1, MOS[3])   # cwieki w rogach tloczenia
    px[cxp, cyp] = MOS[5]

# --- belka rzedu 2: nawigacja
blok(8, 54, W - 9, 70, SKORA[3])
blok(8, 54, W - 9, 54, SKORA[5])
blok(8, 70, W - 9, 70, SKORA[1])
for x in (8, 152):                         # wneki pod Wstecz / Zamknij
    blok(x, 54, x + 15, 69, SKORA[5])
    blok(x, 54, x + 15, 54, SKORA[5])
    blok(x, 54, x, 69, SKORA[5])
    blok(x, 69, x + 15, 69, MOS[4])
    blok(x + 15, 54, x + 15, 69, MOS[4])
    blok(x + 1, 55, x + 14, 55, CIEN)
# Rozety w przerwach miedzy wnekami nawigacji. Sloty rzedu 2 to kolumny 0, 4 i 8
# (x = 8, 80, 152), wiec zdobienie siada w x 44 i 124 — tam nigdy nic nie stanie.
for cx in (44, 124):
    cy = 62
    for y in range(cy - 5, cy + 6):
        for x in range(cx - 5, cx + 6):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= 5.2:
                px[x, y] = MOS[4] if d > 3.6 else (MOS[2] if d > 1.8 else MOS[5])
            if 3.6 < d <= 5.2 and (y - cy) + (x - cx) < -2:
                px[x, y] = MOS[1]

# ============================================================ LISTWA PODZIALU
blok(6, 73, W - 7, 74, DAB[3])
blok(6, 75, W - 7, 75, MOS[1])
blok(6, 76, W - 7, 77, MOS[3])
blok(6, 78, W - 7, 78, MOS[5])
blok(6, 79, W - 7, 79, CIEN)

# ============================================================ POLE GRACZA
for ry in (86, 104, 122, 144):
    for c in range(9):
        x = 8 + 18 * c
        blok(x, ry, x + 15, ry + 15, DAB[6])
        blok(x, ry, x + 15, ry, DAB[6])
        blok(x, ry, x, ry + 15, DAB[6])
        blok(x, ry + 16, x + 15, ry + 16, DAB[1])
        blok(x + 16, ry, x + 16, ry + 16, DAB[1])

# ============================================================ SPOINY DESEK
# Rysowane PO gniazdach: 2-px szczelina miedzy kolumnami to jedyne miejsce, gdzie
# deska jest widoczna na calej wysokosci pola gracza.
for sx in SPOINY:
    for y in range(81, 161):
        px[sx, y] = DAB[6]
        px[sx + 1, y] = DAB[2]

# ============================================================ LATARNIA
# Rysowana i oceniona osobno w czesci/latarnia.py. Tu tylko wklejona i oswietlona.
SZKLO = (0x1b, 0x12, 0x0a, 255)
PLO = rampa(52, 0.10, 1.00, 16, 1.00, 0.45, 5)
LPAL = {'.': None, 'd': SZKLO, 'x': (0x12, 0x09, 0x05, 255)}
for i, c in enumerate(MOS): LPAL['abcdefg'[i]] = c
for i, c in enumerate(PLO): LPAL['TSRQP'[i]] = c
LATARNIA = [
    '......bc......', '.....b..e.....', '.....b..e.....', '......ce......',
    '....bbccee....', '...bbaaccee...', '..bbaaaacceee.', '..cbbbbccceee.',
    '..c.ffffff.e..', '..c.dPQQPd.e..', '..c.dQSTQd.e..', '..c.dQSTQd.e..',
    '..c.dPQSPd.e..', '..c.dPQQPd.e..', '..c.dddddd.e..', '..bbbbccceeee.',
    '...bbaacceee..', '....cbbccee...', '......ce......',
]
LX, LY = 149, 4
for yy, row in enumerate(LATARNIA):
    for xx, ch in enumerate(row):
        c = LPAL.get(ch)
        if c:
            px[LX + xx, LY + yy] = c

# SWIATLO LATARNI — wylacznie na OSOBNYCH elementach, nigdy jako gradient po
# plaskiej powierzchni. Trzy poprzednie podejscia liczyly poswiate promieniem
# i za kazdym razem wychodzila twarda plama z krawedzia obciecia.
for kx in range(9, W - 12, 8):            # dentikuly pod latarnia jasniejsze
    if kx >= 116:
        blok(kx, 23, kx + 4, 29, DAB[1])
        blok(kx, 23, kx + 4, 23, MOS[2])
for x in range(116, W - 6):               # listwy mosiezne rozgrzane po prawej
    px[x, 19] = MOS[1]
    px[x, 32] = MOS[2]
for y in range(0, 44):                    # kant prawej ramy lapie swiatlo
    px[W - 5, y] = MOS[3]
    px[W - 4, y] = MOS[5]

img.save(f'/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/gui/menu_hub_bg.png')
n = len({p for p in list(img.getdata())})
print(f'panel {W}x{H}, kolorow={n}')
