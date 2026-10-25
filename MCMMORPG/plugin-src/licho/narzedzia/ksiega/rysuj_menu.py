#!/usr/bin/env python3
"""Menu „Licho nie śpi” w stylu Księgi + kafel LICHO w tle strony Drużyna (Gildia przesunięta w lewo, siatka 2 x 3).

  rysuj_menu.py <katalog_wyjscia> [<katalog gui Nexo z obecnym hub_spolecznosc_1..4.png>]

Styl, rysunki wspólne i szablon tytułu bierze z narzędzia Wyścigów (bez kopii). Wyjście jak tam: gui/*.png,
glify_licho.yml, gui_menus/*.yml, ksiega_podstrony.txt, podglad.png. Znaki glifów: U+4C40 + 4*strona + N-1.
"""
import importlib.util
import sys
from pathlib import Path
from PIL import Image

WYSCIGI = Path(__file__).resolve().parents[3] / 'wyscigi/narzedzia/ksiega/rysuj_menu.py'
_spec = importlib.util.spec_from_file_location('wyscigi_menu', WYSCIGI)
M = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(M)
W = M.W
R, L, G, E = W.R, W.L, W.G, W.E
OUT = Path(sys.argv[1]).resolve()
NEXO_GUI = M.NEXO_GUI
ZNAK0 = 0x4C40

# ------------------------------------------------------------------ obrazki (52 x 39, paleta Księgi)
def beczka(x, y, bx, by, oczy=True, w=18, h=22):
    """Beczka w x h (domyślnie 18 x 22) z bocznymi obręczami; oczy = licho w środku."""
    X, Y, sx, sy = x+bx, y+by, w/18, h/22
    G([(X+round(2*sx), Y), (X+w-round(2*sx), Y), (X+w, Y+h//2), (X+w-round(2*sx), Y+h), (X+round(2*sx), Y+h), (X, Y+h//2)], 'wood2', 'ink')
    for dx in (w//3, 2*w//3): L([(X+dx, Y+1), (X+dx, Y+h-1)], 'wood1')
    for dy in (round(4*sy), h-round(4*sy)): L([(X+1, Y+dy), (X+w-1, Y+dy)], 'stone1', 2)
    if oczy:
        o, r, cy = round(5*sx), max(1, round(sx)), round(10*sy)
        for ox in (o, w-o-2*r):
            R((X+ox, Y+cy-r, X+ox+2*r, Y+cy+r), 'gold2'); R((X+ox+r, Y+cy, X+ox+r, Y+cy), 'ink')

def snopek(x, y, sx, sy):
    R((x+sx, y+sy, x+sx+13, y+sy+12), 'gold1', 'gold0')
    for dx in (3, 7, 10): L([(x+sx+dx, y+sy+1), (x+sx+dx, y+sy+11)], 'gold2')
    L([(x+sx+1, y+sy+6), (x+sx+12, y+sy+6)], 'wood1')

def hero_licho(x, y):
    """Zagroda o zmierzchu: płot, snopek, beczka, z której łypie licho; Zamawiacz z dymiącym pękiem ziół."""
    R((x, y, x+51, y+9), 'blue1'); R((x, y+10, x+51, y+16), 'red0')
    E((x+40, y+2, x+46, y+8), 'paper3')
    R((x, y+17, x+51, y+38), 'green0')
    for px in range(1, 52, 7): R((x+px, y+12, x+px+1, y+22), 'wood1')
    L([(x, y+15), (x+51, y+15)], 'wood1'); L([(x, y+19), (x+51, y+19)], 'wood1')
    snopek(x, y, 2, 24)
    beczka(x, y, 17, 15)
    # Zamawiacz: kaptur, płaszcz, pęk ziół w dłoni i smuga dymu
    G([(x+40, y+16), (x+44, y+14), (x+47, y+17), (x+48, y+36), (x+37, y+36)], 'stone0', 'ink')
    E((x+40, y+16, x+46, y+22), 'paper1', 'ink'); R((x+41, y+18, x+44, y+21), 'ink')
    L([(x+38, y+26), (x+33, y+23)], 'green1', 2); R((x+31, y+21, x+33, y+23), 'fire')
    for i, (dx, dy) in enumerate(((31, 18), (29, 14), (31, 10), (28, 6))):
        R((x+dx, y+dy, x+dx+1+i % 2, y+dy+1), 'stone2')

def licho_ikona(x, y):
    M.field(x, y)
    beczka(x, y, 13, 4, w=26, h=30)                                  # skala pucharu REKORDÓW (~36 x 30)
    for dx, dy in ((6, 6), (44, 9), (7, 31), (45, 30)): R((x+dx, y+dy, x+dx, y+dy), 'gold2')

def ziola(x, y):
    M.field(x, y)
    for k, (dx, kol) in enumerate(((13, 'green1'), (21, 'green0'), (31, 'green1'), (39, 'green0'))):
        L([(x+26, y+35), (x+dx, y+13+k % 2 * 3)], 'wood2', 2)
        E((x+dx-5, y+5+k % 2 * 3, x+dx+5, y+19+k % 2 * 3), kol, 'ink')
    R((x+22, y+25, x+30, y+29), 'red1', 'ink')                       # wiązanie pęku
    for i, (dx, dy) in enumerate(((42, 26), (45, 21), (47, 25), (49, 18))):
        R((x+dx, y+dy, x+dx+1, y+dy+1), 'stone2' if i % 2 else 'paper3')

def dolacz(x, y):
    M.field(x, y)
    R((x+6, y+10, x+20, y+36), 'wood1', 'ink')                    # brama zagrody
    for dy in (14, 22, 30): L([(x+7, y+dy), (x+19, y+dy)], 'wood2')
    beczka(x, y, 28, 14, oczy=False)
    G([(x+21, y+20), (x+27, y+20), (x+27, y+16), (x+33, y+23), (x+27, y+30), (x+27, y+26), (x+21, y+26)], 'gold2', 'ink')

# ------------------------------------------------------------------ strony
def strona_glowna():
    W.hero_frame(14, 34, hero_licho)
    M.opis(('Licho chowa się', 'w sprzętach.', 'Zamawiacze', 'je tropią.'))
    kafle = [((194, 34), 'dolacz', 'DOŁĄCZ', dolacz), ((266, 34), 'licho', 'LICHO', licho_ikona),
             ((194, 106), 'zamawiacz', 'ŁOWY', ziola), ((266, 106), 'rekordy', 'REKORDY', M.puchar)]
    for (x, y), _, podpis, p in kafle: W.tile(x, y, podpis, p, podpis)
    return [(M.sloty(x, y, 2), k) for (x, y), k, _, _ in kafle]

def strona_pauza():
    W.hero_frame(14, 34, hero_licho)
    M.opis(('Gra trwa,', 'gdy tu jesteś.', 'Wróć albo', 'opuść zagrodę.'))
    kafle = [((194, 34), 'wracaj', 'WRACAJ', M.strzalka), ((266, 34), 'opusc', 'OPUŚĆ', M.drzwi)]
    for (x, y), _, podpis, p in kafle: W.tile(x, y, podpis, p, podpis)
    return [(M.sloty(x, y, 2), k) for (x, y), k, _, _ in kafle]

STRONY = [  # ident, tytuł, rysunek, napis "wstecz", menu "wstecz"
    ('licho', 'Licho nie śpi', strona_glowna, 'WRÓĆ DO MENU', 'hub_spolecznosc'),
    ('licho_pauza', 'Pauza', strona_pauza, 'WRACAJ', 'licho_zamknij'),
]

# ------------------------------------------------------------------ DeluxeMenus
def menu_yml(ident, tytul_znaki, uklad):
    tytul = M.szablon_tytulu()
    for stary, nowy in zip(map(chr, range(0x4168, 0x416C)), tytul_znaki): tytul = tytul.replace(stary, nowy)
    s = [f"menu_title: '{tytul}'", 'size: 54', 'update_interval: 20', 'items:']
    if ident == 'licho':
        s += M.yml_item('info', M.LEWA, '&2Licho nie śpi', ['&7Zagroda Zielarki o zmierzchu.', '&7Licho chowa się w beczkach', '&7i snopkach, Zamawiacze je tropią.',
                                                             '&7Od 2 do 12 graczy, runda ok. 4 min.', '&7Bez łupu: punkty idą do rekordów.'])
        tekst = {
            'dolacz': ('&aDołącz do gry', ['&7Wchodzisz do poczekalni.', '&7Odliczanie rusza przy 2 graczach.', '&7Role losuje gra: 1 Zamawiacz', '&7na każdych 4 graczy.', '',
                                           '&fKliknij, by dołączyć.'], ['[close]', '[console] licho dolacz %player_name%']),
            'licho': ('&6Jak grać Lichem', ['&fPPM &7na beczkę/snopek = wcielenie', '&7Stój &f1,5 s &7= wskakujesz w siatkę', '&7i wyglądasz jak prawdziwy sprzęt.',
                                            '&fF &7psota: trzęsiesz się, &e+5 pkt', '&7Co 40 s sam &7szepczesz (słychać).', '&7Masz &f3 iskry&7: trafienie zabiera jedną.',
                                            '&7Przetrwaj do świtu: &e+15 pkt'], None),
            'zamawiacz': ('&bJak grać Zamawiaczem', ['&fLPM &7w sprzęt = trafienie', '&7Pudło w prawdziwy sprzęt', '&7zabiera &f1 tchu&7; bez tchu zadyszka.',
                                                    '&fF &7okadzanie: Licho w dymie', '&7po chwili kicha i się zdradza.', '&7Złapane Licho dołącza do Ciebie.'], None),
            'rekordy': ('&eRekordy zagrody', ['&7Najlepsze wyniki i Twoje miejsce.'], ['[close]', '[console] licho rekordy %player_name%']),
        }
    else:  # pauza
        s += M.yml_item('info', M.LEWA, '&6Pauza', ['&7Gra trwa dalej, gdy tu jesteś.'])
        tekst = {'wracaj': ('&aWracaj', ['&7Zamknij i graj dalej.'], ['[close]']),
                 'opusc': ('&cOpuść zagrodę', ['&7Wracasz tam, skąd przyszedłeś.', '&7Licho, które wychodzi,', '&7liczy się jako złapane.'],
                           ['[close]', '[console] licho wyjdz %player_name%'])}
    for sl, k in uklad:
        n, lore, kom = tekst[k]
        s += M.yml_item(k, sl, n, lore, kom)
    return '\n'.join(s) + '\n'

# ------------------------------------------------------------------ kafel LICHO w Drużynie
GILDIA = (230, 106)        # dziś sama na środku rzędu 2-3 (sloty 24 25 33 34)
GILDIA_NOWA = (194, 106)   # -> 23 24 32 33
KAFEL = (266, 106)         # LICHO -> 25 26 34 35
OBSZAR = (194, 106, 337, 177)

def kafel_druzyna():
    pasy = [Image.open(NEXO_GUI / f'hub_spolecznosc_{i}.png') for i in range(1, 5)]
    plotno = Image.new('RGB', (352, 444))
    for im, poz in zip(pasy, ((0, 0), (176, 0), (0, 222), (176, 222))): plotno.paste(im.convert('RGB'), poz)
    przed = plotno.copy()
    gildia = przed.crop((GILDIA[0], GILDIA[1], GILDIA[0]+72, GILDIA[1]+72))
    # czyste tło kartki pod oba kafle (świeża księga), potem Gildia 1:1 i nowy kafel
    ksiega = W.new_canvas(); W.draw_book()
    W.set_canvas(plotno)
    plotno.paste(ksiega.crop((OBSZAR[0], OBSZAR[1], OBSZAR[2]+1, OBSZAR[3]+1)), OBSZAR[:2])
    plotno.paste(gildia, GILDIA_NOWA)
    W.tile(*KAFEL, 'LICHO', licho_ikona, 'LICHO')
    x0, y0, x1, y1 = OBSZAR
    for yy in range(444):
        for xx in range(352):
            if not (x0 <= xx <= x1 and y0 <= yy <= y1) and plotno.getpixel((xx, yy)) != przed.getpixel((xx, yy)):
                raise AssertionError(f'zmiana poza kaflami {xx},{yy}')
    assert plotno.crop((GILDIA_NOWA[0], GILDIA_NOWA[1], GILDIA_NOWA[0]+72, GILDIA_NOWA[1]+72)).tobytes() == gildia.tobytes()
    for i, (im, poz) in enumerate(zip(pasy, ((0, 0), (176, 0), (0, 222), (176, 222))), 1):
        plotno.crop((poz[0], poz[1], poz[0]+176, poz[1]+222)).convert(im.mode).save(OUT / 'gui' / f'hub_spolecznosc_{i}.png')
    return plotno

def main():
    (OUT / 'gui').mkdir(parents=True, exist_ok=True); (OUT / 'gui_menus').mkdir(exist_ok=True)
    glify = ['# Licho nie spi: cztery cwiartki na strone; char U+4C40 + 4*strona + N-1.']
    ksiega, podglady = [], []
    for i, (ident, tytul, rysuj, wstecz, menu_wstecz) in enumerate(STRONY):
        obraz = W.new_canvas()
        W.draw_book(); W.draw_title(tytul)
        uklad = rysuj()
        W.draw_footer(); W.draw_lower(wstecz)
        W.clean_orphans()
        assert obraz.size == (352, 444)
        assert {c for _, c in obraz.getcolors(352*444)} <= set(W.C.values())
        znaki = [chr(ZNAK0 + 4*i + j) for j in range(4)]
        for n, (lx, ty) in enumerate(((0, 0), (176, 0), (0, 222), (176, 222)), 1):
            obraz.crop((lx, ty, lx+176, ty+222)).save(OUT / 'gui' / f'{ident}_{n}.png')
            glify += [f'{ident}_{n}:', '  font: minecraft:default', f'  texture: nexo:gui/{ident}_{n}',
                      f'  ascent: {13 if n <= 2 else -98}', '  height: 111', f'  char: {znaki[n-1]}']
        (OUT / 'gui_menus' / f'{ident}.yml').write_text(menu_yml(ident, ''.join(znaki), uklad), encoding='utf-8')
        ksiega.append((znaki[0], ident, menu_wstecz))
        podglady.append(obraz)
    (OUT / 'gui_menus' / 'licho_zamknij.yml').write_text(
        "# Cel przycisku 'wstecz' pauzy: menu, ktore sie nie otwiera, tylko zamyka okno (warunek nigdy niespelniony).\n"
        "menu_title: '&8'\nsize: 9\nopen_requirement:\n  requirements:\n    nigdy:\n      type: string equals\n"
        "      input: 'a'\n      output: 'b'\n  deny_commands:\n    - '[close]'\nitems:\n  nic:\n    material: nexo-mmo_icon_blank\n    slot: 0\n", encoding='utf-8')
    (OUT / 'glify_licho.yml').write_text('\n'.join(glify) + '\n', encoding='utf-8')
    (OUT / 'ksiega_podstrony.txt').write_text(''.join(f'{z}\t{i}\t{m}\n' for z, i, m in ksiega), encoding='utf-8')
    druzyna = kafel_druzyna()
    arkusz = Image.new('RGB', (352*3, 444), W.C['table0'])
    for k, im in enumerate(podglady + [druzyna]): arkusz.paste(im, (352*k, 0))
    arkusz.resize((352*3*2, 444*2), Image.Resampling.NEAREST).save(OUT / 'podglad.png')
    print('ok', [s[0] for s in STRONY])

if __name__ == '__main__':
    main()
