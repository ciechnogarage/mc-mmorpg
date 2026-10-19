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
         'czosnek_kwiat', 'konwalia_kwiat']
PLASKIE = ['kozlek_lisc', 'kozlek_korzen', 'kozlek_gleba', 'kozlek_gleba_rozkopana', 'czosnek_lisc', 'czosnek_lisc_stary', 'konwalia_lisc', 'konwalia_lisc_stary']

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
    for f in os.listdir(tex):
        if f.endswith('.png'): shutil.copy(f'{tex}/{f}', f'{t}/{f}')
    json.dump(KARTA, open(f'{m}/karta.json', 'w'), indent=1)
    y = ['# Wycinek zielarza (MCMMORPG/profesje/zielarz/grafika/nexo_klon.py) — tylko klon']
    for bid, tx, var in BLOKI:
        y += [f'{bid}:', f'  itemname: <white>{bid}', '  material: PAPER', '  excludeFromInventory: false', '  Pack:',
              '    parent_model: block/cross', '    textures:', f'      cross: nexo:block/zielarz/{tx}', '  Mechanics:',
              '    custom_block:', '      type: STRINGBLOCK', f'      custom_variation: {var}', '      hardness: -1',
              '      drop:', '        silktouch: false', '        loots: []']
    for k in KRZYZ:
        y += [f'zielarz_k_{k}:', f'  itemname: <white>{k}', '  material: PAPER', '  excludeFromInventory: true', '  Pack:',
              '    parent_model: block/cross', '    textures:', f'      cross: nexo:block/zielarz/{k}']
    for k in PLASKIE:
        y += [f'zielarz_k_{k}:', f'  itemname: <white>{k}', '  material: PAPER', '  excludeFromInventory: true', '  Pack:',
              '    parent_model: nexo:zielarz/karta', '    textures:', f'      karta: nexo:block/zielarz/{k}']
    open(f'{out}/items/zielarz.yml', 'w').write('\n'.join(y) + '\n')
    print(len(BLOKI), 'blokow,', len(KRZYZ) + len(PLASKIE), 'kart')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
