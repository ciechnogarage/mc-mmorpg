#!/usr/bin/env python3
"""
Generator skorek postaci: 4 RASY x 4 KLASY = 16 plikow PNG 64x64.

DLACZEGO 16, A NIE 4 + "cos na klase"
  Skorka gracza to JEDNA tekstura — nie ma w niej warstwy, ktora serwer moglby podmienic
  osobno w locie. "Podskin klasy" jest wiec wypalany w te sama teksture: RASA bierze glowe
  (karnacja, twarz, wlosy, uszy/kly), KLASA bierze tors, rece, nogi i warstwe zewnetrzna
  (kaptur, maska, naramienniki, tabard). Oba wybory widac naraz i nie walcza o to samo
  miejsce — dokladnie ten sam podzial, ktory PreviewDummyManager robi juz ekwipunkiem
  dummy'ego (helm = rasa, zbroja = klasa).

JASNOSC PALETY TO POMIAR, NIE GUST
  Klient przyciemnia swiat za kazdym otwartym oknem ekwipunku o ok. 53%, a kreator jest
  jednym wielkim oknem. Paleta v1 skorek prototypu zlewala sie w tym przyciemnieniu
  w brazowa plame (patrz docs/kreator-postaci-config-only.md, punkt 3). Dlatego karnacje
  i stroje sa tu jasne i kontrastowe, a nie "realistyczne".

UKLAD TEKSTURY (64x64, format 1.8+)
  Kazda czesc ciala to prostopadloscian rozwiniety zawsze tak samo:
      top(w x d) | bottom(w x d)          <- wiersz gorny, od x = ox+d
      right(d x h) | front(w x h) | left(d x h) | back(w x h)
  Stad jedna funkcja face_origin() zamiast szesciu tablic wspolrzednych na czesc.

URUCHAMIANIE
  ./gen.sh gen_race_skins.py            # -> tools/out/skins/ (domyslnie)
  OUT=<katalog> ./gen.sh gen_race_skins.py
  Na hoscie z pillow dziala tez wprost: python3 gen_race_skins.py
"""
import os
import random
import sys

from PIL import Image

SIZE = 64

# (ox, oy, szerokosc, glebokosc, wysokosc) — wszystkie w pikselach tekstury.
PARTS = {
    'head':     (0, 0, 8, 8, 8),
    'hat':      (32, 0, 8, 8, 8),
    'body':     (16, 16, 8, 4, 12),
    'jacket':   (16, 32, 8, 4, 12),
    'arm_r':    (40, 16, 4, 4, 12),
    'sleeve_r': (40, 32, 4, 4, 12),
    'arm_l':    (32, 48, 4, 4, 12),
    'sleeve_l': (48, 48, 4, 4, 12),
    'leg_r':    (0, 16, 4, 4, 12),
    'pants_r':  (0, 32, 4, 4, 12),
    'leg_l':    (16, 48, 4, 4, 12),
    'pants_l':  (0, 48, 4, 4, 12),
}

SIDES = ('front', 'back', 'right', 'left')
ALL_FACES = ('top', 'bottom', 'front', 'back', 'right', 'left')
ARMS = ('arm_r', 'arm_l')
SLEEVES = ('sleeve_r', 'sleeve_l')
LEGS = ('leg_r', 'leg_l')
PANTS = ('pants_r', 'pants_l')


def face_origin(part, face):
    ox, oy, w, d, h = PARTS[part]
    return {
        'top':    (ox + d, oy, w, d),
        'bottom': (ox + d + w, oy, w, d),
        'right':  (ox, oy + d, d, h),
        'front':  (ox + d, oy + d, w, h),
        'left':   (ox + d + w, oy + d, d, h),
        'back':   (ox + 2 * d + w, oy + d, w, h),
    }[face]


def shade(color, factor):
    """Przyciemnienie/rozjasnienie bez zmiany kanalu alfa."""
    r, g, b = color[:3]
    a = color[3] if len(color) > 3 else 255
    return (
        max(0, min(255, int(r * factor))),
        max(0, min(255, int(g * factor))),
        max(0, min(255, int(b * factor))),
        a,
    )


def mix(a, b, t):
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
        255,
    )


class Skin:
    """Plotno 64x64 z adresowaniem przez (czesc, sciana, u, v)."""

    def __init__(self):
        self.img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
        self.buf = self.img.load()

    def put(self, x, y, color):
        if color is None or not (0 <= x < SIZE and 0 <= y < SIZE):
            return
        self.buf[x, y] = color if len(color) == 4 else (*color, 255)

    def get(self, x, y):
        return self.buf[x, y]

    def rect(self, part, face, x, y, w, h, color):
        fx, fy, fw, fh = face_origin(part, face)
        for j in range(max(0, y), min(y + h, fh)):
            for i in range(max(0, x), min(x + w, fw)):
                self.put(fx + i, fy + j, color)

    def px(self, part, face, x, y, color):
        self.rect(part, face, x, y, 1, 1, color)

    def fill(self, part, color, faces=ALL_FACES):
        for face in faces:
            _, _, fw, fh = face_origin(part, face)
            self.rect(part, face, 0, 0, fw, fh, color)

    def band(self, part, y, h, color, faces=SIDES):
        """Opaska dookola czesci — pas, rabek, mankiet, cholewka buta."""
        for face in faces:
            _, _, fw, _ = face_origin(part, face)
            self.rect(part, face, 0, y, fw, h, color)

    def column(self, part, face, x, y, h, color):
        self.rect(part, face, x, y, 1, h, color)

    def save(self, path):
        self.img.save(path)


# ---------------------------------------------------------------- RASY ------
# skin/skin_dark  — karnacja i jej cien (podbrodek, zageby)
# hair/hair_dark  — wlosy
# eye_white/eye   — bialko i tecowka; nieumarly ma "bialko" swiecace
# fringe/side/back— ile rzedow wlosow wchodzi na przod / boki / tyl glowy
RACES = {
    'czlowiek': {
        'skin': (222, 170, 126), 'skin_dark': (186, 136, 98),
        'hair': (126, 78, 40), 'hair_dark': (92, 55, 27),
        'eye_white': (246, 244, 238), 'eye': (74, 48, 26),
        'fringe': 2, 'side': 4, 'back': 5,
        'ear': None, 'brow': (150, 98, 60),
    },
    'ork': {
        'skin': (138, 182, 96), 'skin_dark': (104, 142, 70),
        'hair': (48, 42, 40), 'hair_dark': (32, 28, 27),
        'eye_white': (250, 228, 176), 'eye': (146, 88, 28),
        'fringe': 2, 'side': 3, 'back': 4,
        'ear': 'wide', 'brow': (86, 118, 52),
        'tusk': (238, 236, 218),
    },
    'elf': {
        'skin': (242, 219, 196), 'skin_dark': (212, 185, 162),
        # Wlosy CHLODNE (srebro), a nie kremowe: przy karnacji kosci sloniowej blond
        # zlewal sie z twarza w jedna jasna bryle — sprawdzone na zblizeniu glowy.
        'hair': (206, 220, 232), 'hair_dark': (158, 178, 196),
        'eye_white': (250, 250, 248), 'eye': (72, 158, 196),
        'fringe': 2, 'side': 6, 'back': 8,
        'ear': 'pointed', 'brow': (176, 194, 210),
    },
    'nieumarly': {
        'skin': (166, 186, 176), 'skin_dark': (126, 148, 140),
        'hair': (72, 78, 78), 'hair_dark': (52, 57, 58),
        'eye_white': (126, 226, 200), 'eye': (30, 62, 58),
        'fringe': 2, 'side': 3, 'back': 4,
        'ear': None, 'brow': (108, 130, 124),
        'rot': (120, 140, 110), 'wound': (112, 74, 76), 'bone': (226, 224, 206),
    },
}

# --------------------------------------------------------------- KLASY ------
# cloth/cloth_dark— tkanina/plyta torsu i rak
# pants/boot      — nogi i buty
# accent/trim     — kolor rozpoznawczy klasy (pas, tabard, stula, szarfa)
CLASSES = {
    'wojownik': {
        'cloth': (158, 166, 178), 'cloth_dark': (112, 120, 134),
        'cloth_light': (196, 204, 216),
        'pants': (96, 104, 118), 'boot': (78, 58, 44),
        'accent': (170, 52, 48), 'trim': (214, 178, 84),
    },
    'mag': {
        'cloth': (96, 78, 158), 'cloth_dark': (66, 52, 118),
        'cloth_light': (132, 114, 196),
        'pants': (60, 48, 106), 'boot': (72, 54, 44),
        'accent': (232, 196, 96), 'trim': (244, 226, 160),
    },
    'akolita': {
        'cloth': (238, 232, 214), 'cloth_dark': (202, 194, 172),
        'cloth_light': (250, 248, 238),
        'pants': (206, 198, 176), 'boot': (146, 118, 78),
        'accent': (226, 178, 62), 'trim': (168, 126, 40),
    },
    'ninja': {
        # Paleta jest JASNIEJSZA niz "czarny ninja", i to nie jest przeoczenie: okno
        # kreatora przyciemnia scene o ~53%, a wersja na czerni zlewala sie w plame
        # (ten sam blad, co paleta v1 skorek prototypu).
        'cloth': (76, 82, 98), 'cloth_dark': (50, 54, 66),
        'cloth_light': (120, 128, 146),
        'pants': (62, 67, 82), 'boot': (42, 45, 56),
        'accent': (198, 58, 62), 'trim': (162, 168, 180),
    },
}


# ----------------------------------------------------------- GLOWA (RASA) ---
def paint_head(s, r):
    skin, dark = r['skin'], r['skin_dark']
    s.fill('head', skin)
    s.fill('head', shade(dark, 0.94), faces=('bottom',))

    hair, hair_dark = r['hair'], r['hair_dark']
    s.fill('head', hair, faces=('top',))
    s.rect('head', 'back', 0, 0, 8, r['back'], hair)
    s.rect('head', 'right', 0, 0, 8, r['side'], hair)
    s.rect('head', 'left', 0, 0, 8, r['side'], hair)
    s.rect('head', 'front', 0, 0, 8, r['fringe'], hair)
    # Grzywka nie moze byc plaska kreska — jeden zab w dol z kazdej strony
    # wystarcza, zeby wlosy czytaly sie jako wlosy, a nie jako czapka.
    s.px('head', 'front', 1, r['fringe'], hair_dark)
    s.px('head', 'front', 6, r['fringe'], hair_dark)
    s.rect('head', 'back', 0, r['back'], 8, 1, hair_dark)

    # Brwi / wal nadoczodolowy — to on daje rasie wyraz twarzy.
    s.rect('head', 'front', 1, 2, 2, 1, r['brow'])
    s.rect('head', 'front', 5, 2, 2, 1, r['brow'])

    # Oczy: bialko + tecowka, po 2 px na oko.
    s.px('head', 'front', 1, 3, r['eye_white'])
    s.px('head', 'front', 2, 3, r['eye'])
    s.px('head', 'front', 5, 3, r['eye'])
    s.px('head', 'front', 6, 3, r['eye_white'])

    # Cien szczeki — bez niego twarz jest plaska plama karnacji.
    s.rect('head', 'front', 0, 7, 8, 1, shade(dark, 0.96))
    s.px('head', 'front', 0, 6, dark)
    s.px('head', 'front', 7, 6, dark)

    # Nos i usta.
    s.px('head', 'front', 3, 4, dark)
    s.px('head', 'front', 4, 4, shade(skin, 1.05))
    s.rect('head', 'front', 3, 6, 2, 1, shade(dark, 0.86))

    if r['ear'] == 'pointed':
        # Ucho elfa: szpic na WARSTWIE ZEWNETRZNEJ glowy, czyli 1 px poza czaszka —
        # inaczej nie bylo go widac z boku, bo scianka boczna jest plaska.
        for face in ('right', 'left'):
            s.rect('hat', face, 2, 3, 2, 2, skin)
            s.px('hat', face, 2, 2, skin)
            s.px('hat', face, 3, 4, dark)
    elif r['ear'] == 'wide':
        for face in ('right', 'left'):
            s.rect('hat', face, 2, 3, 3, 2, skin)
            s.px('hat', face, 4, 4, dark)

    if 'tusk' in r:
        # Kly wychodza spod dolnej wargi — 1 px w twarzy + 1 px w warstwie zewnetrznej,
        # zeby sterczaly przed linia szczeki.
        s.px('head', 'front', 2, 6, r['tusk'])
        s.px('head', 'front', 5, 6, r['tusk'])
        s.px('hat', 'front', 2, 6, r['tusk'])
        s.px('hat', 'front', 5, 6, r['tusk'])
        s.px('hat', 'front', 2, 5, shade(r['tusk'], 0.9))
        s.px('hat', 'front', 5, 5, shade(r['tusk'], 0.9))

    if 'rot' in r:
        # Nieumarly: zapadniete oczodoly, szwy zamiast ust, platy skory.
        s.rect('head', 'front', 1, 2, 2, 1, shade(r['eye'], 0.8))
        s.rect('head', 'front', 5, 2, 2, 1, shade(r['eye'], 0.8))
        s.px('head', 'front', 1, 3, r['eye'])
        s.px('head', 'front', 6, 3, r['eye'])
        s.px('head', 'front', 2, 3, r['eye_white'])
        s.px('head', 'front', 5, 3, r['eye_white'])
        # Szczeka bez warg: rzad zebow zamiast ust i odsloniety kosciec policzka.
        s.rect('head', 'front', 2, 6, 4, 1, shade(r['wound'], 0.9))
        for x in (2, 3, 4, 5):
            s.px('head', 'front', x, 6, r['bone'] if x % 2 else shade(r['wound'], 0.8))
        s.px('head', 'front', 6, 5, r['bone'])
        s.px('head', 'front', 6, 4, shade(r['bone'], 0.9))
        s.px('head', 'right', 5, 4, r['rot'])
        s.px('head', 'right', 6, 5, r['rot'])
        s.px('head', 'left', 2, 5, r['rot'])
        s.px('head', 'back', 3, 5, r['wound'])


# ------------------------------------------------------- CIALO (KLASA) ------
def paint_outfit(s, r, c):
    """Wspolna baza stroju: tors, rekawy, dlonie, nogawki, buty."""
    skin = r['skin']
    s.fill('body', c['cloth'])
    s.fill('body', c['cloth_dark'], faces=('bottom',))
    s.fill('body', shade(c['cloth_light'], 0.98), faces=('top',))
    # Cienie po bokach torsu — bez nich tors jest jednym plaskim prostokatem.
    s.column('body', 'front', 0, 0, 12, c['cloth_dark'])
    s.column('body', 'front', 7, 0, 12, c['cloth_dark'])
    s.column('body', 'back', 0, 0, 12, c['cloth_dark'])
    s.column('body', 'back', 7, 0, 12, c['cloth_dark'])

    for arm in ARMS:
        s.fill(arm, c['cloth'])
        s.fill(arm, c['cloth_dark'], faces=('bottom',))
        s.fill(arm, shade(c['cloth_light'], 0.98), faces=('top',))
        # Dlon: trzy dolne rzedy wracaja do karnacji RASY — zeby po stroju bylo widac,
        # ze pod nim stoi czlowiek/ork/elf/nieumarly, a nie sam kostium.
        s.band(arm, 9, 3, skin)
        s.fill(arm, r['skin_dark'], faces=('bottom',))

    for leg in LEGS:
        s.fill(leg, c['pants'])
        s.fill(leg, shade(c['pants'], 0.88), faces=('top',))
        s.band(leg, 9, 3, c['boot'])
        s.fill(leg, shade(c['boot'], 0.8), faces=('bottom',))
        s.band(leg, 9, 1, shade(c['boot'], 1.15))


def paint_wojownik(s, r, c):
    steel, dark, light = c['cloth'], c['cloth_dark'], c['cloth_light']
    # Plyta: kolnierz, linia napiersnika i pas.
    s.band('body', 0, 1, light)
    s.band('body', 4, 1, dark)
    s.band('body', 8, 2, c['boot'])
    s.band('body', 9, 1, c['trim'])
    # Tabard w barwie klasy — to on niesie kolor na dystans.
    s.rect('body', 'front', 2, 1, 4, 7, c['accent'])
    s.rect('body', 'front', 2, 1, 4, 1, shade(c['accent'], 1.2))
    s.rect('body', 'back', 2, 1, 4, 7, shade(c['accent'], 0.92))
    s.px('body', 'front', 3, 3, c['trim'])
    s.px('body', 'front', 4, 3, c['trim'])
    s.px('body', 'front', 3, 4, c['trim'])
    s.px('body', 'front', 4, 4, c['trim'])
    for arm in ARMS:
        s.band(arm, 6, 1, dark)
        s.band(arm, 8, 1, c['trim'])
    # Naramienniki na warstwie zewnetrznej — sylwetka "szeroka w barach".
    for sleeve in SLEEVES:
        s.band(sleeve, 0, 3, light)
        s.band(sleeve, 3, 1, dark)
        s.fill(sleeve, light, faces=('top',))
    for pant in PANTS:
        s.band(pant, 0, 2, steel)
        s.band(pant, 8, 2, steel)
    s.fill('hat', None)


def paint_mag(s, r, c):
    robe, dark, light = c['cloth'], c['cloth_dark'], c['cloth_light']
    # Szata: zlota lamowka z przodu i pas.
    s.column('body', 'front', 3, 0, 12, c['accent'])
    s.column('body', 'front', 4, 0, 12, shade(c['accent'], 0.85))
    s.band('body', 7, 1, c['accent'])
    s.band('body', 8, 1, dark)
    s.rect('body', 'front', 2, 0, 4, 1, light)
    for arm in ARMS:
        s.band(arm, 0, 9, robe)
        s.band(arm, 7, 1, c['accent'])
    # Szerokie rekawy i dolna czesc szaty.
    for sleeve in SLEEVES:
        s.band(sleeve, 2, 7, light)
        s.band(sleeve, 8, 1, c['accent'])
    for pant in PANTS:
        s.band(pant, 0, 8, robe)
        s.band(pant, 7, 1, c['accent'])
    # Kaptur: warstwa zewnetrzna glowy, z otworem na twarz — rase ma byc widac.
    s.fill('hat', robe, faces=('top', 'back', 'right', 'left'))
    s.rect('hat', 'front', 0, 0, 8, 2, robe)
    s.column('hat', 'front', 0, 0, 8, robe)
    s.column('hat', 'front', 7, 0, 8, robe)
    s.rect('hat', 'front', 0, 0, 8, 1, c['accent'])
    s.rect('hat', 'back', 0, 0, 8, 1, c['accent'])
    s.rect('hat', 'right', 0, 0, 8, 1, c['accent'])
    s.rect('hat', 'left', 0, 0, 8, 1, c['accent'])


def paint_akolita(s, r, c):
    cloth, dark = c['cloth'], c['cloth_dark']
    # Stula: dwa zlote pasy przez piers, symbol miedzy nimi.
    s.column('body', 'front', 2, 0, 9, c['accent'])
    s.column('body', 'front', 5, 0, 9, c['accent'])
    s.column('body', 'back', 2, 0, 9, shade(c['accent'], 0.9))
    s.column('body', 'back', 5, 0, 9, shade(c['accent'], 0.9))
    s.rect('body', 'front', 3, 2, 2, 1, c['trim'])
    s.px('body', 'front', 3, 3, c['trim'])
    s.px('body', 'front', 4, 3, c['trim'])
    s.px('body', 'front', 3, 1, c['trim'])
    s.px('body', 'front', 4, 1, c['trim'])
    s.band('body', 9, 1, c['accent'])
    s.band('body', 10, 2, dark)
    for arm in ARMS:
        s.band(arm, 7, 1, c['accent'])
        s.band(arm, 8, 1, dark)
    for sleeve in SLEEVES:
        s.band(sleeve, 0, 2, c['cloth_light'])
        s.band(sleeve, 5, 4, cloth)
        s.band(sleeve, 8, 1, c['accent'])
    for pant in PANTS:
        s.band(pant, 0, 7, cloth)
        s.band(pant, 6, 1, c['accent'])
    # Kaptur zsuniety na kark: tyl i boki, przod odsloniety.
    s.fill('hat', cloth, faces=('back',))
    s.rect('hat', 'right', 5, 0, 3, 8, cloth)
    s.rect('hat', 'left', 0, 0, 3, 8, cloth)
    s.rect('hat', 'back', 0, 0, 8, 1, c['accent'])


def paint_ninja(s, r, c):
    cloth, dark, light = c['cloth'], c['cloth_dark'], c['cloth_light']
    # Szarfa w pasie i pasy uprzezy na piersi.
    s.band('body', 7, 2, c['accent'])
    s.px('body', 'front', 1, 9, c['accent'])
    s.px('body', 'front', 0, 10, c['accent'])
    # Pas przez piers po SKOSIE. Dwa pionowe pasy dawaly na torsie litere "H"
    # (widoczne na stykowce), a nie uprzaz.
    for x, y in ((2, 0), (2, 1), (3, 2), (3, 3), (4, 4), (4, 5), (5, 6)):
        s.px('body', 'front', x, y, dark)
        s.px('body', 'front', x, y + 1, shade(dark, 1.25))
    s.px('body', 'front', 3, 3, light)
    s.rect('body', 'back', 2, 0, 1, 7, dark)
    s.rect('body', 'back', 5, 0, 1, 7, dark)
    s.band('body', 0, 1, dark)
    for arm in ARMS:
        # Bandaze na przedramionach.
        s.band(arm, 6, 3, light)
        s.band(arm, 7, 1, dark)
    for sleeve in SLEEVES:
        s.band(sleeve, 0, 2, dark)
        s.band(sleeve, 6, 3, light)
    for pant in PANTS:
        s.band(pant, 6, 3, dark)
        s.band(pant, 0, 2, cloth)
    # Maska: zaslania USTA, nie oczy — rasa ma zostac czytelna.
    s.fill('hat', cloth, faces=('top', 'back'))
    s.rect('hat', 'right', 0, 0, 8, 3, cloth)
    s.rect('hat', 'left', 0, 0, 8, 3, cloth)
    s.rect('hat', 'front', 0, 0, 8, 2, cloth)
    s.rect('hat', 'front', 0, 5, 8, 3, cloth)
    s.rect('hat', 'right', 0, 5, 8, 3, cloth)
    s.rect('hat', 'left', 0, 5, 8, 3, cloth)
    s.rect('hat', 'front', 0, 5, 8, 1, dark)
    s.rect('hat', 'back', 0, 2, 8, 1, c['accent'])
    s.rect('hat', 'right', 0, 2, 8, 1, c['accent'])
    s.rect('hat', 'left', 0, 2, 8, 1, c['accent'])


CLASS_PAINTERS = {
    'wojownik': paint_wojownik,
    'mag': paint_mag,
    'akolita': paint_akolita,
    'ninja': paint_ninja,
}


# --------------------------------------------------------- WYKONCZENIE ------
def texture_pass(s, seed):
    """
    Szum i cienie krawedzi. Bez tego kazda sciana jest plaska plama jednego koloru —
    to jest ta roznica, ktora na zrzucie z klienta widac jako "skorka" kontra "bryla".
    Ziarno jest STALE per skorka, zeby kolejne uruchomienia generatora dawaly ten sam plik.
    """
    rng = random.Random(seed)
    for part in PARTS:
        for face in ALL_FACES:
            fx, fy, fw, fh = face_origin(part, face)
            for j in range(fh):
                for i in range(fw):
                    x, y = fx + i, fy + j
                    pixel = s.get(x, y)
                    if pixel[3] == 0:
                        continue
                    factor = 1.0
                    if i == 0:
                        factor *= 0.93
                    if i == fw - 1:
                        factor *= 0.96
                    if j == fh - 1:
                        factor *= 0.92
                    if j == 0:
                        factor *= 1.05
                    factor *= 1.0 + rng.uniform(-0.035, 0.035)
                    s.put(x, y, shade(pixel, factor))


def build(race_id, class_id):
    r, c = RACES[race_id], CLASSES[class_id]
    s = Skin()
    paint_head(s, r)
    paint_outfit(s, r, c)
    CLASS_PAINTERS[class_id](s, r, c)
    texture_pass(s, hash((race_id, class_id)) & 0xFFFF)
    return s


# ------------------------------------------------------------- PODGLAD ------
def compose_view(img, back=False):
    """Plaski podglad postaci 16x32 px (przod albo tyl) — do ogladania stykowki."""
    face = 'back' if back else 'front'
    view = Image.new('RGBA', (16, 32), (0, 0, 0, 0))

    def paste(part, x, y):
        fx, fy, fw, fh = face_origin(part, face)
        view.alpha_composite(img.crop((fx, fy, fx + fw, fy + fh)), (x, y))

    # Kolejnosc ma znaczenie: warstwa zewnetrzna idzie po bazowej.
    order = [('head', 4, 0), ('hat', 4, 0), ('body', 4, 8), ('jacket', 4, 8)]
    left_arm = [('arm_l', 12, 8), ('sleeve_l', 12, 8)]
    right_arm = [('arm_r', 0, 8), ('sleeve_r', 0, 8)]
    legs = [('leg_r', 4, 20), ('pants_r', 4, 20), ('leg_l', 8, 20), ('pants_l', 8, 20)]
    if back:
        # Z tylu rece zamieniaja sie stronami.
        left_arm = [(p, 0, y) for p, _, y in left_arm]
        right_arm = [(p, 12, y) for p, _, y in right_arm]
    for part, x, y in order + left_arm + right_arm + legs:
        paste(part, x, y)
    return view


def contact_sheet(skins, scale=7):
    """Stykowka 4 rasy (kolumny) x 4 klasy (wiersze), przod i tyl obok siebie."""
    cell_w, cell_h = 16 * 2 + 4, 32
    pad = 6
    races = list(RACES)
    classes = list(CLASSES)
    sheet = Image.new('RGBA', (
        len(races) * (cell_w + pad) + pad,
        len(classes) * (cell_h + pad) + pad,
    ), (26, 28, 32, 255))
    for col, race in enumerate(races):
        for row, cls in enumerate(classes):
            img = skins[(race, cls)].img
            x = pad + col * (cell_w + pad)
            y = pad + row * (cell_h + pad)
            sheet.alpha_composite(compose_view(img), (x, y))
            sheet.alpha_composite(compose_view(img, back=True), (x + 20, y))
    return sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.environ.get('OUT') or os.path.join(here, 'out', 'skins')
    os.makedirs(out, exist_ok=True)

    skins = {}
    for race in RACES:
        for cls in CLASSES:
            skin = build(race, cls)
            skins[(race, cls)] = skin
            path = os.path.join(out, f'cc_{race}_{cls}.png')
            skin.save(path)
    contact_sheet(skins).save(os.path.join(out, '_podglad.png'))
    print(f'{len(skins)} skorek + _podglad.png -> {out}')


if __name__ == '__main__':
    sys.exit(main())
