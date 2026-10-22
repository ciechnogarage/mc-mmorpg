# ============================================================================
# NIEAKTUALNY — to jest zimne granatowe ikony, odrzucony 2026-09-22.
# Aktualny generator tla:   generate_bg2.py   (cieply: dab + mosiadz + zielona skora)
# Aktualny generator ikon:  generate_icons2.py
# Plik zostaje jako odniesienie; nie generuj z niego do packa.
# ============================================================================
#!/usr/bin/env python3
"""Generuje ikony kafelkow menu-huba (przedmioty Nexo 'hub_*').

PO CO: domyslnie kafelki uzywaly vanillowych przedmiotow — glowa gracza, skrzynia, totem,
komparator. Kazdy z nich jest z innej bajki i razem wygladaja jak przypadkowy zlepek.
Te ikony sa jednej rodziny: ta sama plakietka, ta sama ramka, rozni je tylko symbol i kolor
akcentu. Paleta jest ta sama co w tle panelu (generate_bg.py) i w panelu ekwipunku.

ROZMIAR 32x32 — dwa razy wiecej detalu niz vanillowe 16x16, a i tak daleko do limitu 256 px,
powyzej ktorego Nexo podmienia teksture na placeholder.

Uzycie: python3 generate_icons.py [katalog-wyjsciowy]
"""
import os
import sys
from PIL import Image, ImageDraw

S = 32
OUT = sys.argv[1] if len(sys.argv) > 1 else "icons"
os.makedirs(OUT, exist_ok=True)

PLATE      = (14, 20, 27)
PLATE_TOP  = (24, 34, 44)
RIM        = (86, 126, 150)
RIM_DARK   = (40, 60, 74)

IKONY = {
    "postac":       (232, 202, 112),
    "ekwipunek":    (222, 162, 92),
    "umiejetnosci": (122, 202, 232),
    "profesje":     (216, 166, 82),
    "przygoda":     (142, 206, 132),
    "ekonomia":     (236, 206, 112),
    "spolecznosc":  (220, 152, 210),
    "ustawienia":   (172, 186, 196),
    "pomoc":        (142, 166, 192),
    "wstecz":       (152, 166, 180),
    "zamknij":      (216, 96, 96),
    # --- kafelki w sekcjach ---------------------------------------------------------------
    "statystyki":   (232, 202, 112),
    "atrybuty":     (122, 202, 232),
    "zmien_postac": (222, 162, 92),
    "drzewka":      (172, 210, 140),
    "rzucanie":     (196, 150, 230),
    "zadania":      (142, 206, 132),
    "znajomi":      (232, 140, 160),
    "gildia":       (216, 176, 96),
    "pvp":          (216, 96, 96),
    "wbudowie":     (196, 180, 120),
}


def plakietka(img, d):
    """Wspolne tlo kazdej ikony: zaokraglony kwadrat z GRADIENTEM, ramka i swiatlem u gory.
    Plaski kolor wygladal jak naklejka — panel ma glebie, wiec ikony tez musza ja miec."""
    grad = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gp = grad.load()
    for y in range(S):
        t = y / (S - 1)
        c = tuple(int(PLATE_TOP[i] + (PLATE[i] - PLATE_TOP[i]) * (t ** 0.8)) for i in range(3))
        for x in range(S):
            gp[x, y] = c + (255,)
    maska = Image.new("L", (S, S), 0)
    ImageDraw.Draw(maska).rounded_rectangle([1, 1, S - 2, S - 2], radius=6, fill=255)
    img.paste(grad, (0, 0), maska)
    d.rounded_rectangle([1, 1, S - 2, S - 2], radius=6, outline=RIM_DARK + (255,), width=1)
    d.rounded_rectangle([3, 3, S - 4, S - 4], radius=5, outline=RIM + (95,), width=1)
    d.arc([3, 3, S - 4, S - 4], start=200, end=340, fill=(120, 160, 190, 190), width=1)


def cien_symbolu(img, rysuj):
    """Symbol dostaje ciemny odrzut o 1 px w dol i w prawo — odkleja go od plakietki."""
    warstwa = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rysuj(ImageDraw.Draw(warstwa), (0, 0, 0, 150))
    img.alpha_composite(warstwa, (0, 0), (0, 0), (S - 1, S - 1))


def sym_postac(d, c):
    d.ellipse([13, 8, 19, 14], fill=c)                      # glowa
    d.polygon([(10, 24), (12, 17), (20, 17), (22, 24)], fill=c)  # barki


def sym_ekwipunek(d, c):
    d.polygon([(9, 9), (23, 9), (23, 18), (16, 24), (9, 18)], outline=c, width=2)
    d.line([(16, 12), (16, 20)], fill=c, width=2)


def sym_umiejetnosci(d, c):
    d.polygon([(16, 6), (18, 14), (26, 16), (18, 18), (16, 26), (14, 18), (6, 16), (14, 14)],
              fill=c)


def sym_profesje(d, c):
    # MLOT, nie skrzyzowane narzedzia: skrzyzowanie czytalo sie jak "X" i mylilo z ikona
    # zamkniecia, ktora tez jest iksem. Roznica ma byc w KSZTALCIE, nie tylko w kolorze.
    d.rectangle([8, 8, 24, 14], fill=c)          # glowica
    d.rectangle([14, 14, 18, 26], fill=c)        # trzonek
    d.rectangle([10, 10, 13, 12], fill=PLATE + (255,))   # faza na glowicy


def sym_przygoda(d, c):
    d.polygon([(16, 6), (25, 16), (16, 26), (7, 16)], outline=c, width=2)
    d.ellipse([14, 14, 18, 18], fill=c)


def sym_ekonomia(d, c):
    d.ellipse([8, 8, 24, 24], outline=c, width=2)
    d.ellipse([13, 13, 19, 19], fill=c)


def sym_spolecznosc(d, c):
    d.ellipse([8, 9, 14, 15], fill=c)
    d.polygon([(6, 24), (7, 18), (15, 18), (16, 24)], fill=c)
    d.ellipse([18, 9, 24, 15], fill=c)
    d.polygon([(16, 24), (17, 18), (25, 18), (26, 24)], fill=c)


def sym_ustawienia(d, c):
    d.ellipse([9, 9, 23, 23], outline=c, width=2)
    d.ellipse([14, 14, 18, 18], fill=c)
    for x0, y0, x1, y1 in ((15, 4, 17, 9), (15, 23, 17, 28),
                           (4, 15, 9, 17), (23, 15, 28, 17)):
        d.rectangle([x0, y0, x1, y1], fill=c)


def sym_pomoc(d, c):
    d.ellipse([14, 7, 18, 11], fill=c)     # kropka
    d.rectangle([14, 13, 18, 25], fill=c)  # slupek — znak informacji


def sym_wstecz(d, c):
    d.polygon([(10, 16), (18, 8), (18, 24)], fill=c)
    d.rectangle([18, 14, 24, 18], fill=c)


def sym_zamknij(d, c):
    d.line([(10, 10), (22, 22)], fill=c, width=3)
    d.line([(22, 10), (10, 22)], fill=c, width=3)


def sym_statystyki(d, c):
    d.rectangle([8, 20, 12, 25], fill=c)
    d.rectangle([14, 14, 18, 25], fill=c)
    d.rectangle([20, 8, 24, 25], fill=c)


def sym_atrybuty(d, c):
    d.polygon([(16, 6), (25, 17), (19, 17), (19, 26), (13, 26), (13, 17), (7, 17)], fill=c)


def sym_zmien_postac(d, c):
    d.polygon([(8, 12), (16, 7), (16, 17)], fill=c)     # strzalka w lewo (gora)
    d.rectangle([16, 10, 25, 14], fill=c)
    d.polygon([(24, 20), (16, 25), (16, 15)], fill=c)   # strzalka w prawo (dol)
    d.rectangle([7, 18, 16, 22], fill=c)


def sym_drzewka(d, c):
    d.rectangle([15, 19, 17, 27], fill=c)
    d.polygon([(16, 5), (25, 20), (7, 20)], fill=c)
    d.polygon([(16, 11), (22, 20), (10, 20)], fill=PLATE + (255,))
    d.polygon([(16, 13), (21, 20), (11, 20)], fill=c)


def sym_rzucanie(d, c):
    d.line([(9, 24), (20, 12)], fill=c, width=3)
    d.polygon([(22, 5), (24, 10), (29, 12), (24, 14), (22, 19), (20, 14), (15, 12), (20, 10)],
              fill=c)


def sym_zadania(d, c):
    d.rounded_rectangle([9, 6, 23, 26], radius=2, outline=c, width=2)
    for y in (12, 16, 20):
        d.line([(13, y), (19, y)], fill=c, width=2)


def sym_znajomi(d, c):
    d.ellipse([8, 9, 17, 18], fill=c)
    d.ellipse([15, 9, 24, 18], fill=c)
    d.polygon([(8, 15), (24, 15), (16, 26)], fill=c)


def sym_gildia(d, c):
    d.rectangle([9, 6, 11, 27], fill=c)               # drzewce
    d.polygon([(11, 7), (24, 7), (24, 19), (17, 15), (11, 19)], fill=c)


def sym_pvp(d, c):
    d.line([(9, 24), (22, 8)], fill=c, width=2)
    d.line([(23, 24), (10, 8)], fill=c, width=2)
    d.line([(7, 20), (13, 26)], fill=c, width=3)      # rekojesc lewa
    d.line([(25, 20), (19, 26)], fill=c, width=3)     # rekojesc prawa


def sym_wbudowie(d, c):
    d.polygon([(9, 6), (23, 6), (16, 16)], fill=c)    # klepsydra: gora
    d.polygon([(9, 26), (23, 26), (16, 16)], fill=c)  # dol
    d.rectangle([8, 5, 24, 7], fill=c)
    d.rectangle([8, 25, 24, 27], fill=c)


SYMBOLE = {k: globals()["sym_" + k] for k in IKONY}

for nazwa, kolor in IKONY.items():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    plakietka(img, d)
    # cien symbolu: ten sam ksztalt, ciemny, przesuniety o 1 px
    cien = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    SYMBOLE[nazwa](ImageDraw.Draw(cien), (0, 0, 0, 160))
    img.alpha_composite(cien.crop((0, 0, S - 1, S - 1)), (1, 1))
    SYMBOLE[nazwa](d, kolor + (255,))
    sciezka = os.path.join(OUT, f"{nazwa}.png")
    img.save(sciezka)
    print(f"  {sciezka}")
print(f"gotowe: {len(IKONY)} ikon {S}x{S}")
