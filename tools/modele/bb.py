"""Wspolna biblioteka generatorow modeli ModelEngine (.bbmodel, format 4.10 = konwencja, w ktorej czyta ME).

Zasady ME wymuszane tutaj (wiki.mythiccraft.io/modelengine/Modeling/Creating-a-Model i Bone-Behaviors):
- kostka: obrot w jednej osi i tylko 0/±22.5/±45 -> inny obrot = kostka we wlasnej kosci `o_<nazwa>`,
- kazda kosc z kostkami = osobna encja w grze -> kosci `o_*` dozwolone tylko, gdy sa animowane (waliduj()),
- hitbox: X == Z, pivot Y = wysokosc oczu; shadow: najwiekszy wymiar kostki = srednica cienia.
"""
import os
import base64
import io
import json
import uuid

try:
    from PIL import Image
except ImportError:  # host bez Pillow: tekstura z gotowego PNG w base64 ponizej
    Image = None

SZAROSCI = [(38, 38, 38), (74, 74, 74), (112, 112, 112), (156, 156, 156), (214, 214, 214)]


def tekstura_blockout():
    """16x16: pieć pol 3x3 w rzedzie y=0..3; walor k -> pole u=3k..3k+3."""
    if Image is None:
        raise SystemExit('uruchom w obrazie pixelart (Pillow)')
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for k, c in enumerate(SZAROSCI):
        for x in range(3 * k, 3 * k + 3):
            for y in range(0, 3):
                im.putpixel((x, y), c + (255,))
    b = io.BytesIO()
    im.save(b, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def uid():
    return str(uuid.uuid4())


ELEMENTY = []  # kostki biezacego modelu (wyczysc() przed nastepnym)


DOZWOLONE_KATY = (0, 22.5, -22.5, 45, -45)


def legalny_obrot(rot):
    """ModelEngine: obrot kostki tylko w jednej osi i tylko o 0/±22.5/±45 (limit modeli przedmiotow MC)."""
    niezerowe = [a for a in rot if abs(a) > 1e-9]
    return len(niezerowe) <= 1 and all(any(abs(a - k) < 1e-6 for k in DOZWOLONE_KATY) for a in niezerowe)


def szescian(nazwa, od, do, walor, origin=None, rot=(0, 0, 0), walory_scian=None):
    """walor 1..5 dla calego szescianu; walory_scian nadpisuje wybrane sciany (np. gora jasniej).
    Obrot niedozwolony dla kostki w ME -> kostka bez obrotu wewnatrz wlasnej kosci z tym obrotem."""
    origin = list(origin or [(od[i] + do[i]) / 2 for i in range(3)])
    legal = legalny_obrot(rot)
    e = {'name': nazwa, 'box_uv': False, 'rescale': False, 'locked': False, 'render_order': 'default',
         'allow_mirror_modeling': True, 'from': list(od), 'to': list(do), 'autouv': 0, 'color': 0,
         'origin': origin, 'rotation': list(rot) if legal else [0, 0, 0],
         'faces': {}, 'type': 'cube', 'uuid': uid()}
    for f in ('north', 'east', 'south', 'west', 'up', 'down'):
        w = (walory_scian or {}).get(f, walor) - 1
        e['faces'][f] = {'uv': [3 * w + 0.5, 0.5, 3 * w + 2.5, 2.5], 'texture': 0}
    ELEMENTY.append(e)
    if legal:
        return e['uuid']
    return kosc('o_' + nazwa, origin, [e['uuid']], rot=rot)


KOSCI = {}


def kosc(nazwa, origin, dzieci, rot=(0, 0, 0)):
    k = _kosc(nazwa, origin, dzieci, rot)
    KOSCI[nazwa] = k['uuid']
    return k


def _kosc(nazwa, origin, dzieci, rot):
    return {'name': nazwa, 'origin': list(origin), 'rotation': list(rot), 'color': 0, 'uuid': uid(),
            'export': True, 'mirror_uv': False, 'isOpen': True, 'locked': False, 'visibility': True,
            'autouv': 0, 'children': dzieci}




WARIANT = int(os.environ.get('WARIANT', '0') or 0)  # 0 = wersja biezaca; 1-3 = warianty projektu do wyboru


def wariant(slownik, domyslnie=None):
    """Wartosc zalezna od wariantu: wariant({2: 'a', 4: 'b'}, 'x') -> 'a' w W2, 'b' w W4, 'x' w pozostalych."""
    return slownik.get(WARIANT, domyslnie)


def wyczysc():
    ELEMENTY.clear()
    KOSCI.clear()


def waliduj(animowane_kosci):
    """Zatrzymuje zapis modelu, ktory zlamie zasady ME albo doda encje bez potrzeby."""
    zle = [e['name'] for e in ELEMENTY if not legalny_obrot(e['rotation'])]
    assert not zle, f'kostki z obrotem niedozwolonym w ME: {zle}'
    zbedne = [n for n in KOSCI if n.startswith('o_') and n not in animowane_kosci]
    assert not zbedne, f'kosci-opakowania bez animacji (kazda = encja w grze): {zbedne}'
    # siatka 1:1 jak w LR: kazdy wymiar >= 1 px albo 0 (plaszczyzna, najwyzej jedna os), bez odwroconych kostek
    zle_wym = []
    for e in ELEMENTY:
        d = [e['to'][i] - e['from'][i] for i in range(3)]
        if any(x < 0 for x in d) or sum(1 for x in d if x == 0) > 1 or any(0 < x < 1 - 1e-6 for x in d):
            zle_wym.append((e['name'], [round(x, 2) for x in d]))
    assert not zle_wym, f'wymiary poza siatka 1:1 / odwrocone: {zle_wym}'
    hb = [e for e in ELEMENTY if e['name'] == 'hitbox_box']
    assert hb and abs((hb[0]['to'][0] - hb[0]['from'][0]) - (hb[0]['to'][2] - hb[0]['from'][2])) < 1e-6, 'hitbox: X musi == Z'


def zapisz(sciezka, nazwa, outliner, tex_src, tex_w, animacje):
    model = {
        'meta': {'format_version': '4.10', 'model_format': 'free', 'box_uv': False},
        'name': nazwa + (f'_w{WARIANT}' if WARIANT else ''), 'model_identifier': '', 'visible_box': [1, 1, 0],
        'variable_placeholders': '', 'variable_placeholder_buttons': [], 'timeline_setups': [],
        'unhandled_root_fields': {}, 'resolution': {'width': tex_w, 'height': tex_w},
        'elements': ELEMENTY, 'outliner': outliner,
        'textures': [{'path': '', 'name': nazwa + '.png', 'folder': '', 'namespace': '', 'id': '0',
                      'width': tex_w, 'height': tex_w, 'uv_width': tex_w, 'uv_height': tex_w, 'particle': False,
                      'render_mode': 'default', 'render_sides': 'auto', 'frame_time': 1,
                      'frame_order_type': 'loop', 'frame_order': '', 'frame_interpolate': False,
                      'visible': True, 'internal': True, 'saved': False, 'uuid': uid(), 'source': tex_src}],
        'animations': animacje,
    }
    # format_version 4.10: BB 5.1.6 wczytuje go w konwencji BB4 tak samo jak ModelEngine,
    # wiec podglad tego pliku w Blockbenchu = ruch w grze (zmierzone 2026-09-25)
    with open(sciezka, 'w') as f:
        json.dump(model, f, indent=1)
    encje = sum(1 for k in _wszystkie_kosci(outliner) if any(isinstance(c, str) for c in k['children']))
    print(f'{sciezka}: {len(ELEMENTY)} kostek, {len(KOSCI)} kosci ({encje} encji w grze), {len(animacje)} animacji')


def _wszystkie_kosci(outliner):
    for o in outliner:
        if isinstance(o, dict):
            yield o
            yield from _wszystkie_kosci(o['children'])
