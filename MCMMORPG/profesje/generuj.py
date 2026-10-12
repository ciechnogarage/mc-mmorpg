# -*- coding: utf-8 -*-
"""Generator profesji v2. Pisze do katalogu pluginow (najpierw KLON, potem produkcja przez wdroz.sh).

  python3 generuj.py --plugins <plugins> --katalog-v2 <tools/przedmioty/katalog.json> \
      --ikony-v1 <tools/ikony-profesji> --mistrzostwa <pakiet mistrzostw> --handel <tools/handel> [--sucho]

Zasady:
- przedmioty, ktorymi zarzadza generator, zyja w blokach '# >>> PROFESJE v2' na koncu plikow item/<typ>.yml;
  ich stare definicje v1 (te same id) sa usuwane z reszty pliku (jedno zrodlo prawdy);
- relikty v1 (tresc.RELIKTY) zostaja tam, gdzie sa — gracze moga je miec;
- stacje profession-<prof>.yml (T1) zachowuja blok '# >>> PRZEDMIOTY' generatora v2 (kamienie jakosci);
- wynik jest idempotentny: drugi przebieg nie zmienia plikow.
"""
import argparse
import copy
import glob
import json
import os
import re
import shutil
import sys

import yaml

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tresc as T  # noqa: E402
from model import Model, PROF_PL, MISTRZ_PL, RZEMIOSLO, ZBIERACKIE  # noqa: E402

ZNACZNIK_START = '# >>> PROFESJE v2 (MCMMORPG/profesje/generuj.py — nie edytuj recznie)'
ZNACZNIK_KONIEC = '# <<< PROFESJE v2'
DEMO_STACJE = ['mythical-forge', 'arcane-forge', 'steel-crafting-station']
STACJE_ZBIERACKIE = ['profession-mining', 'profession-herbalism', 'profession-farming', 'profession-fishing',
                     'profession-hunting']
MATERIAL_BUFFA = {'MIKSTURA_LECZNICZA': 'POTION', 'MIKSTURA_MANY': 'POTION', 'ODTRUTKA': 'POTION',
                  'ELIKSIR_SILY': 'POTION', 'ELIKSIR_UMYSLU': 'POTION', 'ELIKSIR_SKORY': 'POTION',
                  'ELIKSIR_ZWINNOSCI': 'POTION', 'OLEJ': 'HONEY_BOTTLE', 'POSILEK_SYTY': 'COOKED_BEEF',
                  'POSILEK_ROSOL': 'RABBIT_STEW', 'POSILEK_PROWIANT': 'BREAD', 'UCZTA': 'PUMPKIN_PIE',
                  'DANIE_MISTRZA': 'GOLDEN_CARROT', 'OPATRUNEK': 'PAPER'}
JEDZENIE = {'POSILEK_SYTY': (8, 10), 'POSILEK_ROSOL': (6, 8), 'POSILEK_PROWIANT': (5, 6), 'UCZTA': (8, 10),
            'DANIE_MISTRZA': (10, 12)}
WYCZERPANY = {1: 'STONE', 2: 'SMOOTH_STONE', 3: 'SANDSTONE', 4: 'DEEPSLATE', 5: 'PACKED_ICE', 6: 'CALCITE',
              7: 'COBBLESTONE', 8: 'BLACKSTONE', 9: 'DIORITE', 10: 'OBSIDIAN'}
DZWIEK = {'mining': ('block.stone.hit', 'block.stone.break'), 'woodworking': ('block.wood.hit', 'block.wood.break'),
          'herbalism': ('block.grass.hit', 'block.grass.break'), 'farming': ('block.crop.break', 'block.crop.break')}


def dump(d):
    return yaml.safe_dump(d, allow_unicode=True, sort_keys=False, width=200, default_flow_style=False)


def czytaj(f):
    with open(f, encoding='utf-8') as h:
        return h.read()


def pisz(f, s, raport):
    os.makedirs(os.path.dirname(f), exist_ok=True)
    stary = czytaj(f) if os.path.exists(f) else None
    if stary == s:
        return
    with open(f, 'w', encoding='utf-8') as h:
        h.write(s)
    raport.append(('zapis', f))


def usun_klucze(tekst, klucze):
    """Usuwa z YAML (poziom 0) bloki o podanych kluczach; komentarze i reszta zostaja."""
    out, pomin = [], False
    for linia in tekst.split('\n'):
        m = re.match(r"^['\"]?([A-Za-z0-9_\-]+)['\"]?:\s*(#.*)?$", linia) or re.match(r"^['\"]?([A-Za-z0-9_\-]+)['\"]?:\s", linia)
        if linia and not linia.startswith((' ', '\t', '#')):
            pomin = bool(m and m.group(1) in klucze)
        if not pomin:
            out.append(linia)
    return '\n'.join(out)


def wytnij_blok(tekst, start, koniec):
    a = tekst.find(start)
    if a < 0:
        return tekst, None
    b = tekst.find(koniec, a)
    if b < 0:
        raise SystemExit(f'niedomkniety blok {start}')
    b += len(koniec)
    return (tekst[:a].rstrip('\n') + '\n' + tekst[b:].lstrip('\n')), tekst[a:b]


class Generator:
    def __init__(self, a):
        self.a = a
        self.P = a.plugins
        self.m = Model(self.P, a.katalog_v2)
        self.raport = []
        from ikony import Ikony
        self.ik = Ikony(a.ikony_v1)

    # ======================================================= MMOItems: przedmioty
    def model_nexo(self, it):
        if it.get('stary_model') and (it['ikona'] or {}).get('stara'):
            return it['stary_model']
        if (it['ikona'] or {}).get('stara'):
            raise SystemExit(f"{it['id']}: przedmiot v1 bez modelu i bez ikony v2")
        return 'nexo:prof_' + it['id'].lower().replace('prof_', '', 1)

    def base_przedmiotu(self, it):
        kind, typ = it['kind'], it['typ']
        b = {}
        b['model'] = self.model_nexo(it)
        mat = it['base'].get('material') or it.get('stary_material')
        if kind == 'buff':
            mat = MATERIAL_BUFFA[self.m.buffy[it['id']]['rodzaj']]
        b['material'] = mat or {'GEM_STONE': 'AMETHYST_SHARD', 'TOOL': 'IRON_PICKAXE'}.get(typ, 'PAPER')
        kolor = {'rzadki': '&d', 'klejnot': '&b', 'buff': '&a', 'narzedzie': '&e'}.get(kind, '&f')
        b['name'] = kolor + it['nazwa']
        b['lore'] = list(it['lore'])
        if kind == 'narzedzie':
            n = self.m.narz[it['id']]
            b['max-item-damage'] = float(n['wytrzymalosc'])
            b['attack-damage'] = 1.0
            b['attack-speed'] = 1.0
            b['repair-reference'] = 'narzedzie-profesji'
        elif kind == 'buff':
            bf = self.m.buffy[it['id']]
            b['vanilla-eating'] = True
            b['can-always-eat'] = True
            b['consume-seconds'] = 1.2 if b['material'] in ('POTION', 'HONEY_BOTTLE') else 1.6
            if bf['rodzaj'] in JEDZENIE:
                f, s = JEDZENIE[bf['rodzaj']]
                b['restore-food'] = float(f)
                b['restore-saturation'] = float(s)
            if bf['rodzaj'] == 'OPATRUNEK':
                b['material'] = 'PAPER'
        elif kind == 'naprawa':
            b['repair-percent'] = T.NAPRAWA_PROCENT * 100
            b['repair-reference'] = 'narzedzie-profesji'
            b['inedible'] = True
        elif kind == 'zwoj':
            b['random-unsocket'] = 1.0
            b['inedible'] = True
        elif kind == 'klejnot':
            b.update({k: v for k, v in it['base'].items()})
            b['success-rate'] = 100.0
        b['disable-crafting'] = True
        b['disable-smelting'] = True
        if typ == 'MATERIAL':
            b.setdefault('max-stack-size', 64.0) if False else None
        return b

    def przedmioty(self):
        zarzadzane = {i for i, it in self.m.items.items()}
        bloki = {}
        for iid, it in self.m.items.items():
            if it['kind'] == 'sprzet':
                typ, dane = self.rzm(it)
            else:
                typ, dane = it['typ'], {'base': self.base_przedmiotu(it)}
            bloki.setdefault(typ, {})[iid] = dane
        for f in glob.glob(self.P + '/MMOItems/item/*.yml'):
            typ = os.path.basename(f)[:-4].upper()
            s = czytaj(f)
            s, _ = wytnij_blok(s, ZNACZNIK_START, ZNACZNIK_KONIEC)
            s = usun_klucze(s, zarzadzane).rstrip('\n') + '\n'
            if typ in bloki:
                s += '\n' + ZNACZNIK_START + '\n' + dump(bloki.pop(typ)) + ZNACZNIK_KONIEC + '\n'
            pisz(f, s, self.raport)
        for typ, d in bloki.items():
            f = f'{self.P}/MMOItems/item/{typ.lower()}.yml'
            pisz(f, ZNACZNIK_START + '\n' + dump(d) + ZNACZNIK_KONIEC + '\n', self.raport)

    def rzm(self, it):
        b = it['baza_v2']
        typ, oryg = self.m.istniejace[b['id']]
        dane = copy.deepcopy(oryg)
        base = dane.setdefault('base', {})
        base['gem-sockets'] = ['Uncolored']
        lore = list(base.get('lore') or [])
        lore += ['&6Wykonane w warsztacie: &fMagiczne + gniazdo na klejnot lub runę.']
        base['lore'] = lore
        mods = dane.get('modifiers') or {}
        nowe = {}
        for k, v in mods.items():
            if k.startswith('rz_'):
                nowe['rzm_' + k[3:]] = {}
                self.rzm_grupy.add(k)
            else:
                nowe[k] = v
        dane['modifiers'] = nowe
        return typ, dane

    def modyfikatory(self):
        src = yaml.safe_load(czytaj(self.P + '/MMOItems/modifiers/przedmioty.yml')) or {}
        out = {}
        for g in sorted(self.rzm_grupy):
            grupa = src.get(g)
            if not grupa:
                raise SystemExit(f'brak grupy rzadkosci v2 {g}')
            mag = copy.deepcopy(grupa['modifiers']['rz_magiczny'])
            warianty = mag['modifiers']
            w2 = max(warianty.values(), key=lambda w: len(w.get('modifiers', {})))
            out['rzm_' + g[3:]] = {'chance': 1, 'min': 1, 'max': 1, 'modifiers': {
                'rzm_magiczny': {'chance': 1, 'stats': mag['stats'], 'min': 1, 'max': 1,
                                 'modifiers': {'w': dict(w2, chance=1)}}}}
        naglowek = ('# Sprzet z rzemiosla (RZM_*) — PLIK GENEROWANY: MCMMORPG/profesje/generuj.py.\n'
                    '# Rzadkosc tylko MAGICZNA: 1 prefiks + 1 sufiks z tych samych pul co lup v2 (pref_*/suf_* z przedmioty.yml).\n')
        pisz(self.P + '/MMOItems/modifiers/rzemioslo.yml', naglowek + dump(out), self.raport)

    # ======================================================= stacje
    def stacje(self):
        T1 = {}
        for prof in RZEMIOSLO:
            f = f'{self.P}/MMOItems/crafting-stations/profession-{prof}.yml'
            T1[prof] = czytaj(f) if os.path.exists(f) else None
        typy = {i: it['typ'] for i, it in self.m.items.items()}
        for i, (typ, _) in self.m.istniejace.items():
            typy.setdefault(i, typ)
        po = {}
        for r in self.m.rec:
            po.setdefault((r['prof'], r['tier']), []).append(r)
        for prof in RZEMIOSLO:
            szablon = yaml.safe_load(T1[prof]) if T1[prof] else None
            if not szablon:
                raise SystemExit(f'brak stacji profession-{prof}.yml do skopiowania wygladu')
            _, blok_v2 = wytnij_blok(T1[prof], '  # >>> PRZEDMIOTY', '  # <<< PRZEDMIOTY')
            if blok_v2:
                blok_v2 = self.blok_v2_bez_xp(prof, blok_v2)
            for t in range(1, 11):
                st = {'name': f'Warsztat {MISTRZ_PL[prof]} {T.ROMAN[t]}', 'max-queue-size': szablon.get('max-queue-size', 10)}
                if t == 1 and szablon.get('command'):
                    st['command'] = szablon['command']
                rec = {}
                for r in sorted(po.get((prof, t), []), key=lambda r: (r['lv'], r['id'])):
                    rec[r['id']] = self.receptura(r, typy)
                st['recipes'] = rec
                st['gui-layout'] = szablon['gui-layout']
                tekst = dump(st)
                if t == 1 and blok_v2:
                    tekst = tekst.replace('recipes:\n', 'recipes:\n' + blok_v2 + '\n', 1)
                pisz(f'{self.P}/MMOItems/crafting-stations/{self.m.stacja(prof, t)}.yml',
                     f'# PLIK GENEROWANY: MCMMORPG/profesje/generuj.py (tier {t}: poziomy {T.poziom_startu(t)}-{10 * t}).\n' + tekst,
                     self.raport)
        for s in DEMO_STACJE + STACJE_ZBIERACKIE:
            f = f'{self.P}/MMOItems/crafting-stations/{s}.yml'
            if os.path.exists(f):
                os.remove(f)
                self.raport.append(('usun', f))

    def blok_v2_bez_xp(self, prof, blok):
        """Kamienie jakosci v2 (generator tools/przedmioty): XP liczy plugin, wiec wycinamy exp{} z wyzwalaczy
        i dopisujemy receptury do mapy XP pluginu (ten sam ekwiwalent co reszta)."""
        linie, i = blok.split('\n'), 0
        out = []
        while i < len(linie):
            l = linie[i]
            if re.match(r'^\s+triggers:\s*$', l):
                j = i + 1
                reszta = []
                while j < len(linie) and re.match(r'^\s+- ', linie[j]):
                    if 'exp{' not in linie[j]:
                        reszta.append(linie[j])
                    j += 1
                if reszta:
                    out.append(l)
                    out += reszta
                i = j
                continue
            out.append(l)
            i += 1
        blok = '\n'.join(out)
        d = yaml.safe_load('recipes:\n' + '\n'.join(x for x in blok.split('\n') if not x.strip().startswith('#'))) or {}
        for rid, r in (d.get('recipes') or {}).items():
            ing = [(i, int(n)) for i, n in re.findall(r'id=([A-Z0-9_]+)[,;]amount=(\d+)', ' '.join(r.get('ingredients') or []))]
            ekw = sum(n * self.m.ekw.get(i, 1.0) for i, n in ing)
            out_id = re.search(r'id=([A-Z0-9_]+)', r.get('output', '')).group(1)
            self.v2_rec.append(dict(id=rid, prof=prof, tier=1, xp=int(round(ekw * T.g(1) * T.XP_RZEMIOSLA)),
                                    st=f'profession-{prof}', out=out_id, ing=ing))
        return blok

    def receptura(self, r, typy):
        out = f"mmoitem{{type={r['typ_out']},id={r['out']},amount={r['ile']}"
        if r.get('poziom_przedmiotu'):
            out += f",level={r['poziom_przedmiotu']}"
        out += '}'
        sumy = {}
        for i, n in r['ing']:
            sumy[i] = sumy.get(i, 0) + n
        ing = [f"mmoitem{{type={typy[i]},id={i},amount={n}}}" for i, n in sumy.items()]
        czas = 1 + (r['lv'] - T.poziom_startu(r['tier'])) // 2 + (3 if r['out'].startswith('RZM_') else 0)
        return {'output': out, 'crafting-time': czas,
                'conditions': [f"profession{{profession={r['prof']},level={r['lv']}}}"],
                'ingredients': ing, 'options': {'hide-when-locked': False}}

    # ======================================================= MMOCore
    def mmocore(self):
        krzywa = []
        for l in range(1, 100):
            krzywa.append(str(int(round(T.akcje(l) * T.g(T.tier_poziomu(l))))))
        pisz(self.P + '/MMOCore/exp-curves/profesje_v2.txt', '\n'.join(krzywa) + '\n', self.raport)
        tabela = self.a.mistrzostwa and os.path.join(self.a.mistrzostwa, 'plugins/MMOCore/exp-tables/mistrzostwa.yml')
        if tabela and os.path.exists(tabela):
            pisz(self.P + '/MMOCore/exp-tables/mistrzostwa.yml', czytaj(tabela), self.raport)
        for prof in list(PROF_PL):
            f = f'{self.P}/MMOCore/professions/{prof}.yml'
            stare = yaml.safe_load(czytaj(f)) or {}
            nowe = {'name': stare.get('name', PROF_PL[prof]), 'max-level': 100, 'experience': stare.get('experience', {'base': 8, 'per-level': 1}),
                    'exp-curve': 'profesje_v2', 'exp-sources': {}}
            if tabela and os.path.exists(tabela):
                nowe['exp-table'] = f'mistrz_{prof}'
            naglowek = ('# PLIK GENEROWANY: MCMMORPG/profesje/generuj.py (profesje v2, PROJEKT.md).\n'
                        '# XP daje WYLACZNIE plugin Rzemioslo (wezly, lowiska, zwloki, receptury, zlecenia).\n'
                        '# Zadnych zrodel vanilli (statyw, stol zaklec, kowadlo, on-fish) — audyt.py bramka "vanilla".\n'
                        '# Krzywa profesje_v2.txt: poziom L kosztuje (20+2L) akcji biezacego tieru.\n')
            pisz(f, naglowek + dump(nowe), self.raport)

    # ======================================================= Nexo: ikony i bloki
    def nexo(self):
        tex = self.P + '/Nexo/pack/assets/nexo/textures/item/profesje_v2'
        os.makedirs(tex, exist_ok=True)
        wpisy = {}
        for iid, it in self.m.items.items():
            if it['kind'] == 'sprzet':
                continue
            im = self.ik.ikona(it['ikona'])
            if im is None:
                continue
            klucz = 'prof_' + iid.lower().replace('prof_', '', 1)
            plik = f'{tex}/{klucz}.png'
            self.zapisz_png(im, plik)
            wpisy[klucz] = {'itemname': '<white>' + it['nazwa'], 'material': 'PAPER', 'excludeFromInventory': True,
                            'excludeFromCommands': True, 'Pack': {'texture': f'nexo:item/profesje_v2/{klucz}'}}
        naglowek = ('# Ikony profesji v2 — GENEROWANE: MCMMORPG/profesje/generuj.py (ksztalty: tools/ikony-profesji).\n'
                    '# Nie sa przedmiotami do zdobycia: MMOItem wskazuje model `nexo:<id>`.\n')
        pisz(self.P + '/Nexo/items/profesje_v2_ikony.yml', naglowek + dump(wpisy), self.raport)
        self.bloki_nexo()

    def zapisz_png(self, im, plik):
        import io
        buf = io.BytesIO()
        im.save(buf, 'PNG')
        dane = buf.getvalue()
        if os.path.exists(plik) and open(plik, 'rb').read() == dane:
            return
        with open(plik, 'wb') as h:
            h.write(dane)
        self.raport.append(('png', plik))

    def bloki_nexo(self):
        from tekstury import tekstura_bloku
        btex = self.P + '/Nexo/pack/assets/nexo/textures/block/profesje_v2'
        os.makedirs(btex, exist_ok=True)
        wpisy = {}
        nb, sb = 20, 1
        for nid, w in list(self.m.wezly.items()) + [(l['blok'], dict(profesja='fishing', tier=t, nowy=True, typ_bloku='NOTEBLOCK',
                                                                     nazwa=f'Łowisko {T.ROMAN[t]}'))
                                                    for t, l in self.m.lowiska.items() if l['blok']]:
            if not w.get('nowy'):
                continue
            pliki = tekstura_bloku(nid, w, self.ik, self.m, btex)
            for p, im in pliki.items():
                self.zapisz_png(im, p)
            if w['typ_bloku'] == 'NOTEBLOCK':
                mech = {'type': 'NOTEBLOCK', 'custom_variation': nb, 'hardness': -1,
                        'drop': {'silktouch': False, 'loots': []}}
                nb += 1
                if w['profesja'] == 'woodworking':
                    mech['model'] = f'nexo:block/profesje_v2/{nid}'
                    pack = {'parent_model': 'block/cube_column', 'textures': {
                        'end': f'nexo:block/profesje_v2/{nid}_top', 'side': f'nexo:block/profesje_v2/{nid}'}}
                else:
                    pack = {'parent_model': 'block/cube_all', 'texture': f'nexo:block/profesje_v2/{nid}'}
                wpisy[nid] = {'itemname': '<white>' + w['nazwa'], 'material': 'PAPER', 'excludeFromInventory': False,
                              'Pack': pack, 'Mechanics': {'custom_block': mech}}
            else:
                mech = {'type': 'STRINGBLOCK', 'custom_variation': sb, 'hardness': -1,
                        'drop': {'silktouch': False, 'loots': []}}
                sb += 1
                wpisy[nid] = {'itemname': '<white>' + w['nazwa'], 'material': 'PAPER', 'excludeFromInventory': False,
                              'Pack': {'parent_model': 'block/cross', 'textures': {'cross': f'nexo:block/profesje_v2/{nid}'}},
                              'Mechanics': {'custom_block': mech}}
        naglowek = ('# Wezly zbieractwa i znaczniki lowisk v2 — GENEROWANE: MCMMORPG/profesje/generuj.py.\n'
                    '# Stawiaja je projektanci lokacji (place / paleta Iris nexo:<id>). Zbieranie obsluguje plugin Rzemioslo:\n'
                    '# PPM z narzedziem w plecaku; blok jest niezniszczalny (hardness -1), drop pusty.\n'
                    '# NOTEBLOCK custom_variation 20+ (1-8 zajmuje pakiet3), STRINGBLOCK 1+.\n')
        pisz(self.P + '/Nexo/items/profesje_v2_wezly.yml', naglowek + dump(wpisy), self.raport)
        # pakiet3: XP i drop przejmuje plugin (bloki moga stac w swiecie)
        f = self.P + '/Nexo/items/pakiet3_bloki.yml'
        if os.path.exists(f):
            d = yaml.safe_load(czytaj(f)) or {}
            zmiana = False
            for k, v in d.items():
                mech = (v or {}).get('Mechanics', {})
                if 'custom' in mech:
                    del mech['custom']
                    zmiana = True
                cb = mech.get('custom_block', {})
                if cb.get('drop', {}).get('loots'):
                    cb['drop']['loots'] = []
                    zmiana = True
                if cb.get('hardness', 0) != -1:
                    cb['hardness'] = -1
                    zmiana = True
            if zmiana:
                naglowek = ('# Pakiet 3 — dawne zloza v1. Od profesji v2 to wezly gornicze T1 obslugiwane przez plugin Rzemioslo\n'
                            '# (XP, urobek, narzedzie). Mechanika custom i drop usuniete przez MCMMORPG/profesje/generuj.py.\n')
                pisz(f, naglowek + dump(d), self.raport)

    # ======================================================= MythicMobs
    def mythicmobs(self):
        tab = {}
        for t, rz in self.m.rzadkie_tabele.items():
            pyl, s_pyl = rz['pyl']
            es, s_es = rz['esencja']
            se, s_se = rz['serce']
            tab[f'PROF_RZADKIE_A{t}_ZWYKLE'] = {'TotalItems': 1, 'Drops': [f'mmoitem{{type=MATERIAL;id={pyl}}} 1 {s_pyl}'],
                                                'TriggerConditions': ['isplayer true']}
            tab[f'PROF_RZADKIE_A{t}_ELITA'] = {'Drops': [f'mmoitem{{type=MATERIAL;id={es}}} 1 {s_es}',
                                                         f'mmoitem{{type=MATERIAL;id={pyl}}} 1-2 0.5'],
                                               'TriggerConditions': ['isplayer true']}
            tab[f'PROF_RZADKIE_A{t}_BOSS'] = {'Drops': [f'mmoitem{{type=MATERIAL;id={se}}} 1 {s_se}',
                                                        f'mmoitem{{type=MATERIAL;id={es}}} 1 0.5',
                                                        f'mmoitem{{type=MATERIAL;id={pyl}}} 2-3 1'],
                                              'TriggerConditions': ['isplayer true']}
        naglowek = ('# Rzadkie skladniki profesji (pkt 3 audytu) — GENEROWANE: MCMMORPG/profesje/generuj.py.\n'
                    '# Mob aktu T podpina tabele linia w Drops:  - PROF_RZADKIE_A<T>_ZWYKLE | _ELITA | _BOSS\n')
        pisz(self.P + '/MythicMobs/droptables/ProfesjeRzadkie.yml', naglowek + dump(tab), self.raport)
        # podpiecie aktu 1 + zdjecie dropu surowcow mysliwskich i XP mysliwstwa za zabicie (teraz: oprawianie)
        mysliwskie = {'PROF_BEAST_SCRAP', 'PROF_BEAST_BONE', 'PROF_RAW_FLEECE', 'PROF_VENOM_SAC', 'PROF_MONSTER_TROPHY',
                      'PROF_RAW_TIMBER'}
        rola = {}
        for k, lst in T.RZADKIE_A1_MOBY.items():
            for mob in lst:
                rola[mob] = {'zwykle': 'PROF_RZADKIE_A1_ZWYKLE', 'elita': 'PROF_RZADKIE_A1_ELITA', 'boss': 'PROF_RZADKIE_A1_BOSS'}[k]
        for f in [self.P + '/MythicMobs/mobs/Bestiariusz.yml', self.P + '/MythicMobs/mobs/Loch_001.yml',
                  self.P + '/MythicMobs/droptables/Bestiariusz.yml']:
            if not os.path.exists(f):
                continue
            s = czytaj(f)
            linie, zm = [], False
            for linia in s.split('\n'):
                if any(i in linia for i in mysliwskie) and ('mmoitem' in linia):
                    zm = True
                    continue
                if 'exp give' in linia and ' hunting ' in linia:
                    zm = True
                    continue
                linie.append(linia)
            s2 = '\n'.join(linie)
            if f.endswith('/mobs/Bestiariusz.yml') or f.endswith('/mobs/Loch_001.yml'):
                s2 = self.podepnij_rzadkie(s2, rola)
            if s2 != s:
                pisz(f, s2, self.raport)
        for f in [self.P + '/MythicMobs/mobs/WezlyZasobow.yml', self.P + '/MythicMobs/skills/WezlySkills.yml',
                  self.P + '/MythicMobs/spawners/demo_wezel_dab.yml']:
            if os.path.exists(f):
                os.remove(f)
                self.raport.append(('usun', f))

    def podepnij_rzadkie(self, s, rola):
        """Dopisuje '- PROF_RZADKIE_A1_*' do sekcji Drops mobow aktu 1 (idempotentnie)."""
        d = yaml.safe_load(s) or {}
        linie = s.split('\n')
        for mob, tabela in rola.items():
            if mob not in d or not isinstance(d[mob], dict):
                continue
            drops = d[mob].get('Drops') or []
            if any(tabela in str(x) for x in drops):
                continue
            # znajdz linie naglowka moba i jego Drops:
            try:
                i = next(n for n, l in enumerate(linie) if l.startswith(mob + ':'))
            except StopIteration:
                continue
            j = i + 1
            while j < len(linie) and (linie[j].startswith(' ') or not linie[j].strip()):
                if re.match(r'^  Drops:\s*$', linie[j]):
                    linie.insert(j + 1, f'  - {tabela}')
                    break
                j += 1
            else:
                linie.insert(i + 1, f'  Drops:\n  - {tabela}')
        return '\n'.join(linie)

    # ======================================================= plugin Rzemioslo
    def rzemioslo(self):
        m = self.m
        cfg = {
            'wersja': 2,
            'krzywa': {'g_t1': 10, 'g_wzrost': 1.25, 'kara_nizszego_tieru': T.KAR_NIZSZEGO_TIERU},
            'zbieranie': {'czas-bazowy': T.CZAS_BAZOWY, 'czas-oprawiania': T.CZAS_OPRAWIANIA, 'czas-min': 1.2,
                          'czas-na-tier-narzedzia': 0.08, 'czas-na-poziom-w-tierze': 0.01, 'zasieg-ruchu': 1.5,
                          'wyczerpanie-s': T.WYCZERPANIE_S, 'zwloki-s': T.ZWLOKI_S, 'machniecie-co-tickow': 7},
            'tiery': {t: {'nazwa': T.KRAINY[t][0], 'start': T.poziom_startu(t), 'rzymska': T.ROMAN[t]} for t in range(1, 11)},
            'profesje': {p: {'nazwa': PROF_PL[p], 'zbieracka': p in ZBIERACKIE,
                             'narzedzie': T.PROF_ZBIERACKIE[p][0] if p in ZBIERACKIE else None} for p in PROF_PL},
            'surowce': list(m.surowce),
            'narzedzia': {i: dict(n) for i, n in m.narz.items()},
            'wezly': {k: dict(v, wyczerpany=('AIR' if v['typ_bloku'] == 'STRINGBLOCK' else
                                             'STRIPPED_OAK_LOG' if v['profesja'] == 'woodworking' else WYCZERPANY[v['tier']]),
                              dzwiek=DZWIEK[v['profesja']][0], dzwiek_koniec=DZWIEK[v['profesja']][1]) for k, v in m.wezly.items()},
            'lowiska': {t: dict(l) for t, l in m.lowiska.items()},
            'zwierzyna': {k: dict(v) for k, v in m.zwierzyna.items()},
            'buffy': {i: dict(b) for i, b in m.buffy.items()},
            'obozowisko': {i: {'tier': m.items[i]['tier'], 'czas': T.OBOZOWISKO['czas'], 'promien': T.OBOZOWISKO['promien'],
                               'staty': {s: round(v[0] + v[1] * (m.items[i]['tier'] - 1), 3) for s, v in
                                         T.OBOZOWISKO.items() if isinstance(v, tuple)}}
                           for i, k in m.uzycia.items() if k == 'obozowisko'},
            'uzycia': dict(m.uzycia),
            'haki-swiata': [i for i, k in m.uzycia.items() if k == 'przeprawa'],
            'receptury': {r['id']: {'profesja': r['prof'], 'tier': r['tier'], 'xp': r['xp'], 'stacja': r['st'],
                                    'wynik': r['out'], 'najdrozszy': max(r['ing'], key=lambda x: m.ekw.get(x[0], 1) * x[1])[0]}
                          for r in m.rec + self.v2_rec},
            'xp-receptur': {r['id']: r['xp'] for r in m.rec + self.v2_rec},
            'perki': T.PERKI,
            'perki-progi': {l: 'perk' for l in range(10, 101, 10)},
            'zlecenia': {p: {t: lst for t, lst in d.items()} for p, d in m.zlecenia.items()},
            'zlecenia-dziennie': T.ZLECENIA['dziennie'],
            'zlecenia-startowe': m.zlecenia_startowe,
            'relikty': list(T.RELIKTY),
            'nazwy': {i: it['nazwa'] for i, it in m.items.items()},
        }
        naglowek = ('# Plugin Rzemioslo — dane GENEROWANE: MCMMORPG/profesje/generuj.py z tresc.py (PROJEKT.md).\n'
                    '# Recznie zmieniaj tylko sekcje "komunikaty" (zachowywana przez generator).\n')
        f = self.P + '/Rzemioslo/config.yml'
        if os.path.exists(f):
            stary = yaml.safe_load(czytaj(f)) or {}
            if stary.get('komunikaty'):
                cfg['komunikaty'] = stary['komunikaty']
        pisz(f, naglowek + dump(cfg), self.raport)

    def mistrzostwa(self):
        src = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'plugin-src', 'mistrzostwa', 'src', 'main',
                           'resources', 'config.yml')
        f = self.P + '/Mistrzostwa/config.yml'
        if not os.path.exists(f):
            pisz(f, czytaj(src), self.raport)

    # ======================================================= DeluxeMenus
    def menu(self):
        from menu import Menu
        Menu(self).pisz()

    # ======================================================= ceny (tools/handel)
    def ceny(self):
        if not self.a.handel:
            return
        wyj = {}
        for iid, c in self.m.ceny.items():
            it = self.m.items[iid]
            st = next((r['st'] for r in self.m.rec if r['out'] == iid), None)
            droga = (st or ('profesja-' + (it['prof'] or 'lochy'))) + f": {it['kind']} T{it['tier']}"
            wyj[iid] = {'id': iid, 'typ': it['typ'], 'nazwa': it['nazwa'], 'wartosc': c['wartosc'], 'skup': c['skup'],
                        'k': T.SKUP, 'droga': droga, 'kind': it['kind'], 'tier': it['tier']}
        pisz(os.path.join(self.a.handel, 'ceny_profesje.json'), json.dumps(wyj, ensure_ascii=False, indent=1) + '\n', self.raport)

    # ======================================================= KATALOG.md
    def katalog_md(self):
        from katalog import pisz_katalog
        pisz_katalog(self)

    def run(self):
        self.rzm_grupy = set()
        self.v2_rec = []
        self.przedmioty()
        self.modyfikatory()
        self.stacje()
        self.mmocore()
        self.nexo()
        self.mythicmobs()
        self.rzemioslo()
        self.mistrzostwa()
        self.menu()
        self.ceny()
        self.katalog_md()
        zap = sum(1 for k, _ in self.raport if k == 'zapis')
        png = sum(1 for k, _ in self.raport if k == 'png')
        usu = [f for k, f in self.raport if k == 'usun']
        print(f'zapisane pliki: {zap}, tekstury: {png}, usuniete: {len(usu)}')
        for f in usu:
            print('  usunieto', os.path.relpath(f, self.P))


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--plugins', required=True)
    ap.add_argument('--katalog-v2', required=True)
    ap.add_argument('--ikony-v1', required=True)
    ap.add_argument('--mistrzostwa')
    ap.add_argument('--handel')
    Generator(ap.parse_args()).run()
