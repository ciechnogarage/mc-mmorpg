"""Konfiguracja Nexo wycinka zielarza: bloki kep (STRINGBLOCK, model krzyzowy jak kwiaty vanilli) i karty zblizenia
(PAPER: krzyzowe = segmenty pedow i kwiaty, plaszczyzna = liscie/korzen/ziemia). Uzycie: nexo_klon.py <tex> <wyjscie>
-> <wyjscie>/items/zielarz.yml, <wyjscie>/pack/assets/nexo/{textures/block/zielarz,models/zielarz}/..."""
import json, os, shutil, sys

BLOKI = [  # id, tekstura, wariant stringblocka (v2 zajmuje 1-33)
    ('zielarz_dziurawiec', 'dziurawiec_swiat', 40), ('zielarz_dziurawiec_scieta', 'dziurawiec_swiat_scieta', 41),
    ('zielarz_kozlek', 'kozlek_swiat', 42), ('zielarz_kozlek_wykopany', 'kozlek_swiat_wykopany', 43),
    ('zielarz_szerokolistna', 'szerokolistna_swiat', 44), ('zielarz_szerokolistna_zerwana', 'szerokolistna_swiat_zerwana', 45),
]
KRZYZ = ['dziurawiec_dol', 'dziurawiec_srodek', 'dziurawiec_srodek_chory', 'dziurawiec_gora', 'dziurawiec_gora_chory',
         'dziurawiec_kwiat', 'dziurawiec_pak', 'dziurawiec_przekwit', 'kozlek_lodyga', 'kozlek_baldach',
         'czosnek_lodyzka', 'wzora_dziurawiec']
PRZEDMIOTY = ['rekawice']   # zwykly przedmiot (wyglad w dloni)
# narzedzie w dloni w czasie zblizenia (tylko pakiet do gracza): vanilla handheld (skala 0,68) zajmowal pol ekranu i byl
# wiekszy od rosliny (ocena 24.10, pkt 6) -> te same modele vanilli, mniejsze i nizej w pierwszej osobie
DLON = {'lopatka': 'minecraft:item/iron_shovel', **{f'sierp_{m}': f'minecraft:item/{m}_hoe' for m in ('wooden', 'stone', 'iron', 'golden', 'diamond', 'netherite')}}
DLON_FP = {'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.6, 1.0, 1.0], 'scale': [0.4, 0.4, 0.4]},
           'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.6, 1.0, 1.0], 'scale': [0.4, 0.4, 0.4]}}
MODELE3D = ['korzen3d_a', 'korzen3d_b', 'korzen3d_c']   # korzen kozlka 3D (korzen3d.py; feeling: wyrwanie i lot do reki)
PLASKIE = ['dziurawiec_nasada', 'wzorb_tyl', 'wzorb_srodek', 'wzorb_przod', 'wzor_ziemia', 'kozlek_korzonek', 'czosnek_baldach', 'konwalia_kwiat', 'kozlek_lisc', 'kozlek_korzen', 'kozlek_gleba', 'kozlek_gleba_rozkopana', 'czosnek_lisc', 'czosnek_lisc_stary', 'konwalia_lisc', 'konwalia_lisc_stary']

KOMP = 0.85   # gamma rozjasnienia kart zblizenia (patrz main)
NASYC = 1.08  # nasycenie kart (1,3 dawalo neonowa zielen, recenzja v4)
MNOZ = 1.05   # (zmierzone 19.10: pionowe karty 74% jasnosci kepy; z odchyleniem 28 st. ~90%)

KARTA = {  # jedna plaszczyzna widoczna z obu stron, bez cieniowania (jak rosliny vanilli)
    "ambientocclusion": False,
    "textures": {"particle": "#karta"},
    "elements": [{"from": [0, 0, 8], "to": [16, 16, 8], "shade": False,
                  "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#karta"},
                            "south": {"uv": [16, 0, 0, 16], "texture": "#karta"}}}],
}

def main(tex, out):
    t = f'{out}/pack/assets/nexo/textures/block/zielarz'; m = f'{out}/pack/assets/nexo/models/zielarz'
    os.makedirs(t, exist_ok=True); os.makedirs(m, exist_ok=True); os.makedirs(f'{out}/items', exist_ok=True)
    from PIL import Image
    karty = set(KRZYZ + PLASKIE)
    for f in os.listdir(tex):
        if not f.endswith('.png'): continue
        if f[:-4] in karty:
            # karty zblizenia rysuje encja (BlockDisplay) z kierunkowym cieniowaniem jak mob: pionowa plaszczyzna dostaje
            # ~0,5-0,7 swiatla (zmierzone 19.10). Kompensacja: rozjasniona kopia; bloki w swiecie zostaja oryginalne.
            im = Image.open(f'{tex}/{f}').convert('RGBA'); px = im.load()
            for yy in range(im.height):
                for xx in range(im.width):
                    r, g, b_, a = px[xx, yy]
                    # gamma zamiast mnoznika: ciemne tony podnosi mocno, jasne prawie wcale (mnoznik wypalal bez korzenia na szaro)
                    # HSV: jasnosc podniesiona, nasycenie tez (sama gamma prala kolory: S 0,53 vs 0,72 kepy, zmierzone 19.10)
                    if a:
                        import colorsys
                        hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b_ / 255)
                        ss = min(1.0, ss * NASYC); vv = min(1.0, (vv ** KOMP) * MNOZ)
                        px[xx, yy] = tuple(round(c * 255) for c in colorsys.hsv_to_rgb(hh, ss, vv)) + (a,)
            im.save(f'{t}/{f}')
        else:
            shutil.copy(f'{tex}/{f}', f'{t}/{f}')
    json.dump(KARTA, open(f'{m}/karta.json', 'w'), indent=1)
    plaski = {"ambientocclusion": False, "textures": {"particle": "#t"},
              "elements": [{"from": [0, 0.5, 0], "to": [16, 0.5, 16], "shade": False,
                            "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#t"}, "down": {"uv": [0, 16, 16, 0], "texture": "#t"}}}]}
    json.dump(plaski, open(f'{m}/plaski.json', 'w'), indent=1)
    y = ['# Wycinek zielarza (MCMMORPG/profesje/zielarz/grafika/nexo_klon.py) — tylko klon']
    for bid, tx, var in BLOKI:
        y += [f'{bid}:', f'  itemname: <white>{bid}', '  material: PAPER', '  excludeFromInventory: false', '  Pack:',
              *( ['    parent_model: nexo:zielarz/plaski', '    textures:', f'      t: nexo:block/zielarz/{tx}'] if tx == 'kozlek_swiat_wykopany'
                 else ['    parent_model: block/cross', '    textures:', f'      cross: nexo:block/zielarz/{tx}'] ), '  Mechanics:',
              '    custom_block:', '      type: STRINGBLOCK', f'      custom_variation: {var}', '      hardness: -1',
              '      drop:', '        silktouch: false', '        loots: []']
    # karty zblizenia = tez warianty STRINGBLOCK (nigdy nie stawiane): BlockDisplay rysuje je jak blok kepy w swiecie
    # (to samo swiatlo i model); ItemDisplay cieniowal je jak przedmiot — byly ciemniejsze niz kepa
    var = 50
    for k in KRZYZ + PLASKIE:
        rodzic, tex = ('block/cross', 'cross') if k in KRZYZ else ('nexo:zielarz/karta', 'karta')
        y += [f'zielarz_k_{k}:', f'  itemname: <white>{k}', '  material: PAPER', '  excludeFromInventory: true', '  Pack:',
              f'    parent_model: {rodzic}', '    textures:', f'      {tex}: nexo:block/zielarz/{k}', '  Mechanics:',
              '    custom_block:', '      type: STRINGBLOCK', f'      custom_variation: {var}', '      hardness: -1',
              '      drop:', '        silktouch: false', '        loots: []']
        var += 1
    for k in PRZEDMIOTY:
        json.dump({'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'nexo:block/zielarz/{k}'}, 'display': DLON_FP},
                  open(f'{m}/{k}.json', 'w'), indent=1)
        y += [f'zielarz_{k}:', '  itemname: <white>Rękawice zielarza', '  material: PAPER', '  excludeFromInventory: true', '  Pack:',
              f'    model: nexo:zielarz/{k}']
    for k, rodzic in DLON.items():
        json.dump({'parent': rodzic, 'display': DLON_FP}, open(f'{m}/dlon_{k}.json', 'w'), indent=1)
        y += [f'zielarz_dlon_{k}:', f'  itemname: <white>{k}', '  material: PAPER', '  excludeFromInventory: true', '  Pack:',
              f'    model: nexo:zielarz/dlon_{k}']
    for k in MODELE3D:   # ItemDisplay (transform NONE): pelny model z korzen3d.py, bez stringblocka
        y += [f'zielarz_k_kozlek_{k}:', f'  itemname: <white>{k}', '  material: PAPER', '  excludeFromInventory: true', '  Pack:',
              f'    model: nexo:zielarz/{k}']
    open(f'{out}/items/zielarz.yml', 'w').write('\n'.join(y) + '\n')
    print(len(BLOKI), 'blokow,', len(KRZYZ) + len(PLASKIE), 'kart')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
