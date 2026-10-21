#!/usr/bin/env python3
"""Menu Wyścigów w stylu Księgi (jak reszta menu Q) + kafel WYŚCIGI w tle strony Drużyna.

  rysuj_menu.py <katalog_wyjscia> [<katalog gui Nexo z obecnym hub_spolecznosc_1..4.png>]

Wyjście: gui/*.png (cztery ćwiartki na stronę), glify_wyscigi.yml (Nexo), gui_menus/*.yml (DeluxeMenus),
ksiega_podstrony.txt (wpisy do KsiegaPostaci), podglad.png. Znaki glifów: U+4C00 + 4*strona + N-1.
"""
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent / 'strony'))
import ksiega_wspolne as W  # noqa: E402

R, L, G, E = W.R, W.L, W.G, W.E
OUT = Path(sys.argv[1]).resolve()
NEXO_GUI = Path(sys.argv[2]) if len(sys.argv) > 2 else Path('/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/gui')
BLANK = 'nexo-mmo_icon_blank'

# ------------------------------------------------------------------ obrazki (52 x 39, paleta Księgi)
def field(x, y, bg='blue0'): R((x, y, x+51, y+38), bg)

def flaga(x, y, ox, oy, skala=1):
    L([(x+ox, y+oy), (x+ox, y+oy+26*skala)], 'wood0', 2)
    for r in range(3):
        for c in range(4):
            kol = 'paper4' if (r + c) % 2 == 0 else 'ink'
            R((x+ox+2+c*4, y+oy+r*4, x+ox+5+c*4, y+oy+3+r*4), kol)
    d = W.d
    d.rectangle((x+ox+1, y+oy-1, x+ox+18, y+oy+12), outline=W.C['gold0'])

def hero_wyscigi(x, y):
    R((x, y, x+51, y+14), 'sky')
    R((x, y+15, x+51, y+38), 'green1')
    for tx in (4, 13, 40, 47):
        E((x+tx-3, y+7, x+tx+3, y+14), 'green0'); R((x+tx, y+13, x+tx, y+16), 'wood1')
    G([(x+14, y+38), (x+22, y+16), (x+30, y+16), (x+44, y+38)], 'wood2')
    G([(x+20, y+38), (x+24, y+16), (x+28, y+16), (x+36, y+38)], 'paper0')
    for yy in (20, 26, 32):
        L([(x+21+(yy-16)//4, y+yy), (x+23+(yy-16)//4, y+yy)], 'red1', 1)
    flaga(x, y, 32, 3)
    # wierzchowiec z jeźdźcem w biegu
    for lx, kx in ((8, 5), (11, 13), (15, 13), (18, 21)): L([(x+lx, y+30), (x+kx, y+36)], 'ink', 1)
    E((x+5, y+24, x+21, y+32), 'stone1', 'ink')
    L([(x+5, y+26), (x+1, y+22)], 'stone0', 2)                      # ogon w biegu
    G([(x+18, y+24), (x+24, y+20), (x+28, y+24), (x+22, y+28)], 'stone1', 'ink')   # łeb z pyskiem
    G([(x+21, y+21), (x+22, y+17), (x+24, y+20)], 'stone0')                         # ucho
    R((x+25, y+23, x+25, y+23), 'ink')
    R((x+10, y+18, x+13, y+25), 'red1', 'ink'); E((x+9, y+14, x+14, y+19), 'paper2', 'ink')
    for yy in (23, 27, 31): L([(x+1, y+yy), (x+4, y+yy)], 'paper4')

def stoper(x, y):
    field(x, y)
    E((x+13, y+6, x+39, y+33), 'gold1', 'ink'); E((x+16, y+9, x+36, y+30), 'paper4', 'gold0')
    R((x+23, y+2, x+29, y+5), 'gold2', 'ink')
    L([(x+26, y+20), (x+26, y+11)], 'ink', 2); L([(x+26, y+20), (x+32, y+23)], 'red1', 2)
    for a, b in ((26, 10), (35, 20), (26, 29), (17, 20)): R((x+a, y+b, x+a, y+b), 'gold0')

def flagi(x, y):
    field(x, y)
    flaga(x, y, 8, 6); flaga(x, y, 26, 10)
    W.star(x+44, y+30, 'gold2')

def puchar(x, y):
    field(x, y)
    G([(x+14, y+5), (x+38, y+5), (x+35, y+20), (x+28, y+25), (x+24, y+25), (x+17, y+20)], 'gold2', 'ink')
    G([(x+17, y+7), (x+25, y+7), (x+23, y+19), (x+19, y+17)], 'paper4')
    L([(x+14, y+8), (x+8, y+9), (x+10, y+16), (x+17, y+18)], 'gold1', 2)
    L([(x+38, y+8), (x+44, y+9), (x+42, y+16), (x+35, y+18)], 'gold1', 2)
    R((x+24, y+25, x+28, y+30), 'gold1', 'ink'); R((x+17, y+30, x+35, y+35), 'wood2', 'ink')
    W.star(x+26, y+14, 'paper4')

def klawisz(x, y, kx, ky, w, h, jasny=False):
    R((x+kx, y+ky, x+kx+w, y+ky+h), 'paper2' if jasny else 'stone2', 'ink')
    R((x+kx+1, y+ky+h-2, x+kx+w-1, y+ky+h-1), 'stone1')

def klawisze(x, y):
    field(x, y)
    klawisz(x, y, 20, 4, 11, 10, True)
    for kx in (7, 20, 33): klawisz(x, y, kx, 16, 11, 10, kx != 20)
    klawisz(x, y, 4, 29, 26, 7)
    for ch, kx, ky in (('W', 20, 4), ('A', 7, 16), ('D', 33, 16)):
        W.bitmap_text(ch, x+kx+6, y+ky+9, 'ink', frame=(x+kx, y+ky, x+kx+11, y+ky+10), margin=0)

def strzalka(x, y):
    field(x, y)
    G([(x+8, y+15), (x+28, y+15), (x+28, y+7), (x+44, y+19), (x+28, y+31), (x+28, y+23), (x+8, y+23)], 'gold2', 'ink')
    for yy in (12, 26): L([(x+3, y+yy), (x+7, y+yy)], 'paper4')

def odnowa(x, y):
    field(x, y)
    W.d.arc((x+12, y+6, x+40, y+33), 40, 330, fill=W.C['gold2'], width=4)
    G([(x+36, y+4), (x+44, y+14), (x+32, y+15)], 'gold2', 'ink')

def drzwi(x, y):
    field(x, y)
    R((x+12, y+5, x+32, y+36), 'wood1', 'ink'); R((x+15, y+8, x+29, y+36), 'wood3')
    R((x+26, y+20, x+27, y+22), 'gold2')
    G([(x+31, y+17), (x+40, y+17), (x+40, y+12), (x+48, y+20), (x+40, y+28), (x+40, y+23), (x+31, y+23)], 'red1', 'ink')

GATUNKI = [  # id, nazwa, kolor sierści, cień, żywioł, opis
    ('wilkor', 'Wilkor', 'stone1', 'stone0', 'kiel', 'Zrywny: najszybciej rusza i wychodzi z zakrętów.'),
    ('pozogar', 'Pożogar', 'red1', 'red0', 'ogien', 'Najszybszy na prostej, ale długo się rozpędza.'),
    ('glebiniec', 'Głębiniec', 'blue2', 'blue1', 'woda', 'Wszechstronny: bez słabych stron.'),
    ('puszczorog', 'Puszczoróg', 'green1', 'green0', 'lisc', 'Zwrotny: najciaśniej bierze zakręty.'),
    ('glazogrzbiet', 'Głazogrzbiet', 'stone2', 'stone1', 'kamien', 'Ciężki: wolno rusza, rozpycha innych.'),
    ('zelaznorog', 'Żelaznoróg', 'gold1', 'gold0', 'rog', 'Pewny: krótszy poślizg ładuje turbo szybciej.'),
]

def emblemat(kolor, cien, zywiol):
    def paint(x, y):
        field(x, y)
        # łapa
        E((x+9, y+16, x+27, y+33), kolor, 'ink'); E((x+12, y+19, x+24, y+30), cien)
        for tx, ty in ((6, 9), (13, 4), (21, 4), (28, 9)):
            E((x+tx, y+ty, x+tx+6, y+ty+8), kolor, 'ink')
        # żywioł po prawej
        ox, oy = x+34, y+8
        if zywiol == 'ogien':
            G([(ox+7, oy), (ox+13, oy+12), (ox+11, oy+21), (ox+3, oy+21), (ox+1, oy+12)], 'fire', 'red0')
            G([(ox+7, oy+8), (ox+10, oy+15), (ox+7, oy+20), (ox+4, oy+15)], 'gold2')
        elif zywiol == 'woda':
            G([(ox+7, oy), (ox+13, oy+13), (ox+10, oy+20), (ox+4, oy+20), (ox+1, oy+13)], 'blue2', 'ink')
            R((ox+4, oy+12, ox+5, oy+15), 'paper4')
        elif zywiol == 'lisc':
            G([(ox+1, oy+20), (ox+3, oy+8), (ox+13, oy+1), (ox+12, oy+12)], 'green1', 'ink')
            L([(ox+2, oy+19), (ox+11, oy+4)], 'green0')
        elif zywiol == 'kamien':
            G([(ox+1, oy+19), (ox+2, oy+8), (ox+8, oy+3), (ox+14, oy+9), (ox+13, oy+19)], 'stone1', 'ink')
            L([(ox+4, oy+11), (ox+8, oy+8)], 'stone2')
        elif zywiol == 'rog':
            L([(ox+2, oy+21), (ox+5, oy+10), (ox+12, oy+1)], 'ink', 5)
            L([(ox+2, oy+21), (ox+5, oy+10), (ox+12, oy+1)], 'paper3', 3)
        else:  # kieł
            G([(ox+2, oy+2), (ox+13, oy+2), (ox+8, oy+21)], 'paper4', 'ink')
            L([(ox+5, oy+4), (ox+8, oy+15)], 'stone2')
    return paint

# ------------------------------------------------------------------ kafle
def kafel_szeroki(x, y, podpis, painter):
    """Kafel 3 x 2 sloty (108 x 72): miejsce na długie nazwy wierzchowców."""
    R((x, y, x+107, y+71), 'paper1', 'gold0')
    R((x+3, y+3, x+104, y+68), 'paper3', 'paper0')
    R((x+6, y+4, x+101, y+50), 'gold0', 'ink')
    R((x+8, y+6, x+99, y+48), 'gold2')
    R((x+10, y+8, x+97, y+46), 'blue0')
    for px, py in ((x+7, y+5), (x+100, y+5), (x+7, y+49), (x+100, y+49)):
        R((px-2, py-2, px+2, py+2), 'gold2', 'gold0'); R((px, py, px, py), 'paper4')
    painter(x+28, y+8)
    R((x, y+44, x+107, y+70), 'paper2', 'gold0')
    R((x+1, y+45, x+106, y+69), 'paper4')
    W.narrow_text(podpis.upper(), x+54, y+48, 'blue0', max_gui=50, frame=(x, y+44, x+107, y+70))

def opis(lines, x=14, y=142):
    R((x, y, x+143, y+107), 'paper1', 'gold0')
    R((x+3, y+3, x+140, y+104), 'paper3', 'gold0')
    R((x+6, y+6, x+137, y+101), 'paper4')
    W.gui_body(lines, x+72, y+8, 126, 82, frame=(x, y, x+143, y+107))
    L([(x+43, 226), (x+63, 226)], 'gold0'); L([(x+81, 226), (x+101, 226)], 'gold0')
    G([(x+72, 222), (x+76, 226), (x+72, 230), (x+68, 226)], 'gold1', 'gold0')

def sloty(x, y, kol, rz=2):
    c, r = (x-14)//36, (y-34)//36
    return [9*(r+dr)+c+dc for dr in range(rz) for dc in range(kol)]

LEWA = [9*r+c for r in range(6) for c in range(4)]

# ------------------------------------------------------------------ strony
def strona_glowna():
    W.hero_frame(14, 34, hero_wyscigi)
    opis(('Ścigaj się', 'na Chowańcach.', 'Równe szanse.'))
    kafle = [((194, 34), 'Czasówka', 'NA CZAS', stoper), ((266, 34), 'Wyścig', 'WYŚCIG', flagi),
             ((194, 106), 'Rekordy', 'REKORDY', puchar), ((266, 106), 'Sterowanie', 'KLAWISZE', klawisze)]
    for (x, y), _, podpis, p in kafle: W.tile(x, y, podpis, p, podpis)
    return [(sloty(x, y, 2), nazwa) for (x, y), nazwa, _, _ in kafle]

POZ_SZEROKIE = [(50, 34), (194, 34), (50, 106), (194, 106), (50, 178), (194, 178)]

def strona_gatunki():
    wynik = []
    for (x, y), (gid, nazwa, kol, cien, zyw, _) in zip(POZ_SZEROKIE, GATUNKI):
        kafel_szeroki(x, y, nazwa, emblemat(kol, cien, zyw))
        wynik.append((sloty(x, y, 3), gid))
    return wynik

def strona_pauza():
    W.hero_frame(14, 34, hero_wyscigi)
    opis(('Wyścig trwa.', 'Wróć na tor', 'albo go opuść.'))
    kafle = [((194, 34), 'jedz', 'JEDŹ', strzalka), ((266, 34), 'odnowa', 'OD NOWA', odnowa), ((230, 106), 'opusc', 'OPUŚĆ', drzwi)]
    for (x, y), _, podpis, p in kafle: W.tile(x, y, podpis, p, podpis)
    return [(sloty(x, y, 2), k) for (x, y), k, _, _ in kafle]

STRONY = [  # ident, tytuł, rysunek, napis "wstecz", menu "wstecz"
    ('wyscigi', 'Wyścigi', strona_glowna, 'WRÓĆ DO MENU', 'hub_spolecznosc'),
    ('wyscigi_czasowka', 'Czasówka', strona_gatunki, 'WRÓĆ', 'wyscigi'),
    ('wyscigi_wyscig', 'Wyścig', strona_gatunki, 'WRÓĆ', 'wyscigi'),
    ('wyscigi_pauza', 'Pauza', strona_pauza, 'JEDŹ DALEJ', 'wyscigi_zamknij'),
]

# ------------------------------------------------------------------ DeluxeMenus
def yml_item(klucz, sloty_, nazwa, lore, komendy=None):
    s = [f'  {klucz}:', f'    material: {BLANK}', f'    slots: [{", ".join(map(str, sloty_))}]',
         '    item_flags: [HIDE_ADDITIONAL_TOOLTIP]', f"    display_name: '{nazwa}'", '    lore:']
    s += [f"      - '{l}'" for l in lore]
    if komendy:
        s.append('    left_click_commands:'); s += [f"      - '{k}'" for k in komendy]
    return s

def szablon_tytulu():
    """Tytuł Księgi = &f + glify przeplatane znakami przesunięć (U+E10x) — wzór z hub_glowne na serwerze."""
    import re
    src = (NEXO_GUI.parents[5] / 'DeluxeMenus/gui_menus/hub_glowne.yml').read_text(encoding='utf-8')
    tytul = re.search(r"^menu_title: '(.*)'", src, re.M).group(1)
    for c in range(0x4168, 0x416C): assert tytul.count(chr(c)) == 1, hex(c)
    return tytul

def menu_yml(ident, tytul_znaki, uklad):
    tytul = szablon_tytulu()
    for stary, nowy in zip(map(chr, range(0x4168, 0x416C)), tytul_znaki): tytul = tytul.replace(stary, nowy)
    s = [f"menu_title: '{tytul}'", 'size: 54', 'update_interval: 20', 'items:']
    if ident == 'wyscigi':
        s += yml_item('info', LEWA, '&6Wyścigi Chowańców', ['&7Wierzchowiec z wypożyczalni,', '&7wszyscy mają równe szanse.', '&7Wynik zależy tylko od jazdy.'])
        tekst = {
            'Czasówka': ('&eCzasówka', ['&7Sam na torze, walczysz z zegarem.', '&7Najlepszy czas trafia do rekordów.', '', '&fKliknij, by wybrać wierzchowca.'], ['[openguimenu] wyscigi_czasowka']),
            'Wyścig': ('&6Wyścig z innymi', ['&7Do 8 jeźdźców. Na torze skrzynki', '&7z bonusami, LPM = cios bronią.', '&7Poczekalnia 20 s.', '', '&fKliknij, by wybrać wierzchowca.'], ['[openguimenu] wyscigi_wyscig']),
            'Rekordy': ('&bRekordy toru', ['&7Najlepsze przejazdy i okrążenie.'], ['[console] wyscigi rekordy %player_name%', '[close]']),
            'Sterowanie': ('&fJak jeździć', ['&fW &7gaz, &fS &7hamulec, &fA/D &7skręt', '&fShift + A/D &7= poślizg; im dłużej,', '&7tym mocniejsze turbo po puszczeniu', '&7(iskry: &fbiałe &7→ &9niebieskie &7→ &6pomarańczowe&7)',
                                                    '&6» &7na torze przyspiesza', '&fF &7bonus, &fLPM &7cios, &fQ &7pauza', '&fW &7tuż przed &aSTART! &7= turbo startowe', '&7Widok z boku: &fF5'], None),
        }
        for sl, nazwa in uklad:
            n, lore, kom = tekst[nazwa]
            s += yml_item(nazwa.lower().replace('ó', 'o'), sl, n, lore, kom)
    elif ident in ('wyscigi_czasowka', 'wyscigi_wyscig'):
        tryb = 'czasowka' if ident.endswith('czasowka') else 'wyscig'
        staty = paski_statystyk()
        for sl, gid in uklad:
            g = next(x for x in GATUNKI if x[0] == gid)
            lore = [f'&7{g[5]}', ''] + staty[gid] + ['', '&fKliknij, by ruszyć.']
            s += yml_item(gid, sl, f'&e{g[1]}', lore, [f'[console] wyscigi {tryb} %player_name% {gid}', '[close]'])
    else:  # pauza
        s += yml_item('info', LEWA, '&6Pauza', ['&7Wyścig trwa dalej, gdy tu jesteś.'])
        tekst = {'jedz': ('&aJedź dalej', ['&7Zamknij i wracaj na tor.'], ['[close]']),
                 'odnowa': ('&eOd nowa', ['&7Czasówka: nowy przejazd', '&7na tym samym wierzchowcu.'], ['[close]', '[console] wyscigi odnowa %player_name%']),
                 'opusc': ('&cOpuść wyścig', ['&7Wracasz tam, skąd przyszedłeś.'], ['[close]', '[console] wyscigi wyjdz %player_name%'])}
        for sl, k in uklad:
            n, lore, kom = tekst[k]
            s += yml_item(k, sl, n, lore, kom)
    return '\n'.join(s) + '\n'

def paski_statystyk():
    import re
    cfg = (Path(__file__).resolve().parents[2] / 'src/main/resources/config.yml').read_text()
    wart = {}
    for m in re.finditer(r"^  (\w+):\s*\{(.*)\}\s*$", cfg, re.M):
        pola = dict(re.findall(r"(predkosc|przyspieszenie|skret): ([\d.]+)", m.group(2)))
        if pola: wart[m.group(1)] = {k: float(v) for k, v in pola.items()}
    def pasek(v, lo, hi):
        n = 2 + round((v - lo) / (hi - lo) * 6) if hi > lo else 5
        return '&a' + '▮' * n + '&8' + '▮' * (8 - n)
    wynik = {}
    for gid, w in wart.items():
        lin = []
        for k, nazwa in (('predkosc', 'Prędkość      '), ('przyspieszenie', 'Przyspieszenie'), ('skret', 'Skręt          ')):
            vals = [x[k] for x in wart.values()]
            lin.append(f'&7{nazwa} {pasek(w[k], min(vals), max(vals))}')
        wynik[gid] = lin
    return wynik

# ------------------------------------------------------------------ kafel WYŚCIGI w Drużynie
def kafel_druzyna():
    pasy = [Image.open(NEXO_GUI / f'hub_spolecznosc_{i}.png') for i in range(1, 5)]
    plotno = Image.new('RGB', (352, 444))
    for im, poz in zip(pasy, ((0, 0), (176, 0), (0, 222), (176, 222))): plotno.paste(im.convert('RGB'), poz)
    przed = plotno.copy()
    W.set_canvas(plotno)
    W.tile(266, 178, 'WYŚCIGI', flagi, 'WYŚCIGI')
    for yy in range(444):
        for xx in range(352):
            if not (266 <= xx <= 337 and 178 <= yy <= 249) and plotno.getpixel((xx, yy)) != przed.getpixel((xx, yy)):
                raise AssertionError(f'zmiana poza kaflem {xx},{yy}')
    (OUT / 'gui').mkdir(parents=True, exist_ok=True)
    for i, (im, poz) in enumerate(zip(pasy, ((0, 0), (176, 0), (0, 222), (176, 222))), 1):
        plotno.crop((poz[0], poz[1], poz[0]+176, poz[1]+222)).convert(im.mode).save(OUT / 'gui' / f'hub_spolecznosc_{i}.png')
    return plotno

def main():
    (OUT / 'gui').mkdir(parents=True, exist_ok=True); (OUT / 'gui_menus').mkdir(exist_ok=True)
    glify = ['# Wyscigi Chowancow: cztery cwiartki na strone; char U+4C00 + 4*strona + N-1.']
    ksiega = []
    podglady = []
    for i, (ident, tytul, rysuj, wstecz, menu_wstecz) in enumerate(STRONY):
        obraz = W.new_canvas()
        W.draw_book(); W.draw_title(tytul)
        uklad = rysuj()
        W.draw_footer(); W.draw_lower(wstecz)
        W.clean_orphans()
        assert obraz.size == (352, 444)
        assert {c for _, c in obraz.getcolors(352*444)} <= set(W.C.values())
        znaki = [chr(0x4C00 + 4*i + j) for j in range(4)]
        for n, (lx, ty) in enumerate(((0, 0), (176, 0), (0, 222), (176, 222)), 1):
            obraz.crop((lx, ty, lx+176, ty+222)).save(OUT / 'gui' / f'{ident}_{n}.png')
            glify += [f'{ident}_{n}:', '  font: minecraft:default', f'  texture: nexo:gui/{ident}_{n}',
                      f'  ascent: {13 if n <= 2 else -98}', '  height: 111', f'  char: {znaki[n-1]}']
        (OUT / 'gui_menus' / f'{ident}.yml').write_text(menu_yml(ident, ''.join(znaki), uklad), encoding='utf-8')
        ksiega.append((znaki[0], ident, menu_wstecz))
        podglady.append(obraz)
    (OUT / 'gui_menus' / 'wyscigi_zamknij.yml').write_text(
        "# Cel przycisku 'wstecz' pauzy: menu, ktore sie nie otwiera, tylko zamyka okno (warunek nigdy niespelniony).\n"
        "menu_title: '&8'\nsize: 9\nopen_requirement:\n  requirements:\n    nigdy:\n      type: string equals\n"
        "      input: 'a'\n      output: 'b'\n  deny_commands:\n    - '[close]'\nitems:\n  nic:\n    material: nexo-mmo_icon_blank\n    slot: 0\n", encoding='utf-8')
    (OUT / 'glify_wyscigi.yml').write_text('\n'.join(glify) + '\n', encoding='utf-8')
    (OUT / 'ksiega_podstrony.txt').write_text(''.join(f'{z}\t{i}\t{m}\n' for z, i, m in ksiega), encoding='utf-8')
    druzyna = kafel_druzyna()
    arkusz = Image.new('RGB', (352*5, 444), W.C['table0'])
    for k, im in enumerate(podglady + [druzyna]): arkusz.paste(im, (352*k, 0))
    arkusz.resize((352*5*2, 444*2), Image.Resampling.NEAREST).save(OUT / 'podglad.png')
    print('ok', [s[0] for s in STRONY])

if __name__ == '__main__':
    main()
