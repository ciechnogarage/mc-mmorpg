#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generator systemu przedmiotow (bazy, rarity, affiksy, jakosc, sety) dla MMOItems.

Uzycie (host ma python3 bez pyyaml — generator go nie potrzebuje; walidacja: sprawdz.py w dockerze):
    python3 tools/przedmioty/generuj.py --plugins <katalog plugins> [--katalog out.json]
Domyslnie pisze do world1/plugins. Na klon: --plugins $CLAUDE_JOB_DIR/tmp/itm/data/plugins

Pisze WYLACZNIE bloki `# >>> PRZEDMIOTY` / `# <<< PRZEDMIOTY` (wycina stary blok i dokleja na koniec pliku)
oraz caly plik modifiers/przedmioty.yml. Szablony zaczynaja sie od `option:`, wiec tresc_startowa.py
(auto-waga zbroi) ich nie dotyka. Id startowe z tools/klasy-rasowe zostaja nietkniete.
"""
import argparse, json, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tresc import *  # noqa

ZN_A, ZN_B = '# >>> PRZEDMIOTY (tools/przedmioty/generuj.py — nie edytuj recznie)', '# <<< PRZEDMIOTY'

def q(s):
    return "'" + str(s).replace("'", "''") + "'"

def num(x):
    x = round(float(x), 4)
    return str(int(x)) + '.0' if x == int(x) else str(x)

def formula(v1, s, spread=0.08, maxs=0.2):
    """wartosc przy ilvl 1 = v1, przyrost s na poziom; MMOItems liczy base + ilvl*scale"""
    if not s:
        return f'{{base: {num(v1)}, spread: {spread}, max-spread: {maxs}}}' if spread else num(v1)
    return f'{{base: {num(v1 - s)}, scale: {num(s)}, spread: {spread}, max-spread: {maxs}}}'

def forma(rodz, formy):
    m, f, n = formy
    return {'m': m, 'f': f, 'n': n, 'p': n}[rodz]

def wstaw_blok(plik, linie, wciecie=''):
    a, b = wciecie + ZN_A, wciecie + ZN_B
    t = open(plik, encoding='utf-8').read() if os.path.exists(plik) else ''
    t = re.sub(r'\n?' + re.escape(a) + r'.*?' + re.escape(b) + r'\n?', '\n', t, flags=re.S).rstrip('\n')
    blok = '\n'.join([a] + linie + [b])
    t = (t + '\n\n' if t else '') + blok + '\n'
    open(plik, 'w', encoding='utf-8').write(t)

def wstaw_po(plik, kotwica_re, linie, wciecie='  '):
    """blok wewnatrz mapy (np. recipes:) — zaraz po linii kotwicy"""
    a, b = wciecie + ZN_A, wciecie + ZN_B
    t = open(plik, encoding='utf-8').read()
    t = re.sub(re.escape(a) + r'.*?' + re.escape(b) + r'\n', '', t, flags=re.S)
    m = re.search(kotwica_re, t, flags=re.M)
    if not m:
        raise SystemExit(f'brak kotwicy {kotwica_re} w {plik}')
    i = t.index('\n', m.end()) + 1
    t = t[:i] + '\n'.join([a] + linie + [b]) + '\n' + t[i:]
    open(plik, 'w', encoding='utf-8').write(t)

class Gen:
    def __init__(self, plugins):
        self.P = plugins
        self.mi = os.path.join(plugins, 'MMOItems')
        attr_dir = os.path.join(plugins, 'MMOCore', 'attributes')
        self.atrybuty = set()
        for f in os.listdir(attr_dir):
            if f.endswith('.yml'):
                self.atrybuty |= set(re.findall(r'^([a-z_]+):', open(os.path.join(attr_dir, f), encoding='utf-8').read(), flags=re.M))
        self.wiara = 'wiara' in self.atrybuty
        self.ilvl_stat = self._ilvl_stat()
        self.per_typ = {}      # typ MMOItems -> linie yml
        self.katalog = {'przedmioty': [], 'sety': [], 'affiksy': {}, 'rarity': [], 'kamienie': [], 'wiara': self.wiara}

    def _ilvl_stat(self):
        t = open(os.path.join(self.mi, 'custom-stats.yml'), encoding='utf-8').read()
        return 'CUSTOM_ILVL' in t

    def atr_ok(self, stat):
        return not (stat.endswith('wiara') and not self.wiara)

    # ---------------------------------------------------------- wspolne pola szablonu
    def szablon(self, pid, typ, mat, nazwa, lore, staty, poziom, wym_atr, arch, kat, rodz, dodatki=None, tiered=True, tier=None, zestaw=None):
        L = [f'{pid}:', '  option:']
        if tiered:
            L += ['    level-item: true', '    roll-modifier-check-order: true', '    capacity: {base: 100, scale: 0, spread: 0}']
        else:
            L += ['    level-item: false']
        L += ['  base:', f'    material: {mat}', f'    name: {q(nazwa)}']
        L.append('    lore:')
        L += [f'    - {q(x)}' for x in lore]
        if tier:
            L.append(f'    tier: {tier}')
        if tiered:
            L.append(f'    required-level: {{base: 0, scale: 0.8, min: {poziom}}}')
            if self.ilvl_stat:
                L.append('    custom-ilvl: {base: 0, scale: 1}')
        else:
            L.append(f'    required-level: {num(poziom)}')
        atr = ATRYBUT[arch]
        if wym_atr and self.atr_ok(atr):
            L.append(f'    required-{atr}: {num(wym_atr)}')
        for k, v in staty.items():
            L.append(f'    {k}: {v}')
        for k, v in (dodatki or {}).items():
            L.append(f'    {k}: {v}')
        L.append(f'    upgrade: {{template: jakosc, reference: {kat}, max: {JAKOSC_MAX}, success: 100}}')
        if zestaw:
            L.append(f'    set: {zestaw}')
        if tiered:
            L += ['  modifiers:', f'    rz_{arch}_{kat}_{rodz if rodz != "p" else "n"}: {{}}']
        self.per_typ.setdefault(typ, []).extend(L + [''])

    # ---------------------------------------------------------- zbroja
    def zbroje(self):
        for arch in ARCH:
            for i, (waga, dye, trim, impl, nazwy) in enumerate(ZBROJE[arch]):
                poz, mn = POZIOMY[i], MNOZNIK_BAZY[i]
                for (sid, smat, udz), (nazwa, rodz) in zip(SLOTY_ZB, nazwy):
                    pid = f'PRZ_{arch.upper()}_{sid}_{i + 1}'
                    v1, s = WAGA_PANCERZ[waga]
                    st = {'armor': formula(v1 * udz * mn, s * udz * mn)}
                    for k, (a, b) in impl.items():
                        st[k] = formula(a * mn, b * mn)
                    dod = {}
                    if waga != 'tkanina':
                        dod[f'required-bieglosc-{waga}'] = '1.0'
                    if dye and MAT_WAGA[waga] == 'LEATHER':
                        dod['dye-color'] = q(f'{dye[0]} {dye[1]} {dye[2]}'); dod['hide-dye'] = 'true'
                    if trim:
                        dod['trim-material'] = trim[0]; dod['trim-pattern'] = trim[1]; dod['hide-armor-trim'] = 'true'
                    lore = [f'&7{SLOT_PL[sid]} &8· zbroja {WAGA_PL[waga]} · {ARCH_PL[arch]}']
                    self.szablon(pid, 'ARMOR', f'{MAT_WAGA[waga]}_{smat}', f'<tier-color>{nazwa}', lore, st, poz, WYM_ATR[i], arch, 'zbroja', rodz, dod)
                    self.katalog['przedmioty'].append({'id': pid, 'typ': 'ARMOR', 'arch': arch, 'slot': sid, 'nazwa': nazwa, 'rodz': rodz,
                                                       'poziom': poz, 'waga': waga, 'kat': 'zbroja', 'baza': i + 1})

    # ---------------------------------------------------------- bron
    def bron(self):
        for arch in ARCH:
            wsp, ras = BRON_ARCH[arch]
            for rodzaj in wsp + ras:
                typ, d1, ds, spd, dwu, extra, mats, opis = RODZAJE[rodzaj]
                grupa = GRUPA.get(rodzaj, rodzaj)
                for i, (nazwa, rodz) in enumerate(NAZWY_BRONI[rodzaj]):
                    poz, mn = POZIOMY[i], MNOZNIK_BAZY[i]
                    pid = f'PRZ_{arch.upper()}_{rodzaj.upper()}_{i + 1}'
                    st = {}
                    if d1:
                        st['attack-damage'] = formula(d1 * mn, ds * mn)
                    if spd:
                        st['attack-speed'] = num(spd)
                    for k, (a, b) in extra.items():
                        st[k] = formula(a * mn, b * mn) if k != 'arrow-velocity' else num(a)
                    dod = {f'required-bieglosc-{grupa.replace("_", "-")}': '1.0'}
                    if dwu:
                        dod['two-handed'] = 'true'
                    lore = [f'&7Broń: {opis} &8· {ARCH_PL[arch]}' + (f' · rasowa: {RASA_RODZAJU[rodzaj]}' if rodzaj in RASA_RODZAJU else '')]
                    self.szablon(pid, typ, mats[i], f'<tier-color>{nazwa}', lore, st, poz, WYM_ATR[i], arch, 'bron', rodz, dod)
                    self.katalog['przedmioty'].append({'id': pid, 'typ': typ, 'arch': arch, 'slot': 'BRON', 'rodzaj': rodzaj, 'grupa': grupa,
                                                       'rasa': RASA_RODZAJU.get(rodzaj), 'nazwa': nazwa, 'rodz': rodz, 'poziom': poz,
                                                       'kat': 'bron', 'baza': i + 1, 'dwureczna': dwu})

    # ---------------------------------------------------------- akcesoria
    def akcesoria(self):
        for arch in ARCH:
            for typ in AKC_TYPY:
                for i, (rz, rodz) in enumerate(AKC_RDZ[typ]):
                    poz, mn = POZIOMY[i], MNOZNIK_BAZY[i]
                    pid = f'PRZ_{arch.upper()}_{typ}_{i + 1}'
                    nazwa = f'{rz} {forma(rodz, MATERIAL_ARCH[arch])}'
                    st = {}
                    for k, (a, b) in AKC_IMPL[typ][arch].items():
                        if not self.atr_ok(k):
                            k, a, b = 'skill-damage', 2, 0.2
                        st[k] = formula(a * mn, b * mn)
                    lore = [f'&7Ozdoba: {AKC_PL[typ]} &8· {ARCH_PL[arch]}']
                    self.szablon(pid, typ, AKC_MAT[typ], f'<tier-color>{nazwa}', lore, st, poz, WYM_ATR[i], arch, 'ozdoba', rodz)
                    self.katalog['przedmioty'].append({'id': pid, 'typ': typ, 'arch': arch, 'slot': typ, 'nazwa': nazwa, 'rodz': rodz,
                                                       'poziom': poz, 'kat': 'ozdoba', 'baza': i + 1})

    # ---------------------------------------------------------- sety
    def sety(self):
        linie = []
        for arch in ARCH:
            for s in SETY[arch]:
                sid, poz = s['id'], s['poziom']
                czesci = []
                if 'waga' in s:   # set A: zbroja
                    waga = s['waga']
                    for (slot, smat, udz), (nazwa, rodz) in zip(SLOTY_ZB, s['czesci']):
                        pid = f'PRZ_SET_{sid}_{slot}'
                        v1, sc = WAGA_PANCERZ[waga]
                        st = {'armor': formula((v1 + sc * poz) * udz * 1.2, 0, 0.06, 0.12)}
                        for k, (a, b) in ZBROJE[arch][2][3].items():   # cecha bazowa nadklasy (baza poz. 5)
                            st[k] = formula((a + b * poz) * 1.2, 0, 0.06, 0.12)
                        dod = {}
                        if waga != 'tkanina':
                            dod[f'required-bieglosc-{waga}'] = '1.0'
                        if s.get('dye'):
                            dod['dye-color'] = q(' '.join(map(str, s['dye']))); dod['hide-dye'] = 'true'
                        if s.get('trim'):
                            dod['trim-material'] = s['trim'][0]; dod['trim-pattern'] = s['trim'][1]; dod['hide-armor-trim'] = 'true'
                        lore = [f'&7{SLOT_PL[slot]} &8· zbroja {WAGA_PL[waga]} · {ARCH_PL[arch]}']
                        self.szablon(pid, 'ARMOR', f'{s["mat"]}_{smat}', f'&a❖ {nazwa}', lore, st, poz, 3, arch, 'zbroja', rodz, dod,
                                     tiered=False, tier='ZESTAW', zestaw=sid)
                        czesci.append({'id': pid, 'typ': 'ARMOR', 'slot': slot, 'nazwa': nazwa})
                else:             # set B: bron + 3 ozdoby
                    rodzaj = s['bron']
                    typ, d1, ds, spd, dwu, extra, mats, opis = RODZAJE[rodzaj]
                    grupa = GRUPA.get(rodzaj, rodzaj)
                    nazwa, rodz = s['czesci'][0]
                    pid = f'PRZ_SET_{sid}_BRON'
                    st = {}
                    if d1:
                        st['attack-damage'] = formula((d1 + ds * poz) * 1.2, 0, 0.06, 0.12)
                    if spd:
                        st['attack-speed'] = num(spd)
                    for k, (a, b) in extra.items():
                        st[k] = formula((a + b * poz) * 1.2, 0, 0.06, 0.12)
                    dod = {f'required-bieglosc-{grupa.replace("_", "-")}': '1.0'}
                    self.szablon(pid, typ, mats[4], f'&a❖ {nazwa}', [f'&7Broń: {opis} &8· {ARCH_PL[arch]}'], st, poz, 4,
                                 arch, 'bron', rodz, dod, tiered=False, tier='ZESTAW', zestaw=sid)
                    czesci.append({'id': pid, 'typ': typ, 'slot': 'BRON', 'nazwa': nazwa})
                    for t, (nazwa, rodz) in zip(SET_B_TYPY, s['czesci'][1:]):
                        pid = f'PRZ_SET_{sid}_{t}'
                        st = {}
                        for k, (a, b) in AKC_IMPL[t][arch].items():
                            if not self.atr_ok(k):
                                k, a, b = 'skill-damage', 2, 0.2
                            st[k] = formula((a + b * poz) * 1.3, 0, 0.06, 0.12)
                        self.szablon(pid, t, AKC_MAT[t], f'&a❖ {nazwa}', [f'&7Ozdoba: {AKC_PL[t]} &8· {ARCH_PL[arch]}'],
                                     st, poz, 4, arch, 'ozdoba', rodz, tiered=False, tier='ZESTAW', zestaw=sid)
                        czesci.append({'id': pid, 'typ': t, 'slot': t, 'nazwa': nazwa})
                # definicja setu
                linie += [f'{sid}:', f'  name: {q("&a" + s["nazwa"])}', '  bonuses:']
                for prog, bon in s['progi'].items():
                    linie.append(f"    '{prog}':")
                    for k, v in bon.items():
                        if k == 'ability':
                            linie.append(f'      ability-1: {{type: {v[0]}, mode: {v[1]}, cooldown: {v[2]}}}')
                        else:
                            linie.append(f'      {k}: {num(v)}')
                linie.append('  lore-tag:')
                linie.append(f"  - {q('&2❖ &a&l' + s['nazwa'] + ' &8— zestaw, ' + str(len(czesci)) + ' części')}")
                linie.append(f"  - {q('&8   ' + ' · '.join(c['nazwa'].split(' ')[0] for c in czesci))}")
                for prog in s['progi']:
                    linie.append(f"  - {q('&a (' + str(prog) + ') &7' + s['opis'][prog])}")
                linie.append('')
                self.katalog['sety'].append({'id': sid, 'arch': arch, 'nazwa': s['nazwa'], 'poziom': poz, 'czesci': czesci,
                                             'progi': {str(k): v for k, v in s['opis'].items()}, 'docelowo': s['docelowo'],
                                             'bonusy': {str(k): {kk: (list(vv) if isinstance(vv, tuple) else vv) for kk, vv in b.items()} for k, b in s['progi'].items()}})
        wstaw_blok(os.path.join(self.mi, 'item-sets.yml'), linie)

    # ---------------------------------------------------------- affiksy (modifiers/przedmioty.yml)
    def affiksy(self):
        E = lambda x: f'{x * 0.001:.6f}'   # chance do dolosowania proporcjonalnego (min-fill wazony po chance)
        out = ['# Affiksy i rzadkosc — PLIK GENEROWANY: tools/przedmioty/generuj.py (nie edytuj recznie).',
               '# rz_<arch>_<kat>_<rodz>: rzadkosc (stats.tier) -> wariant skladu -> grupy pref/suf z min=max.',
               '# Rodzina a_<affiks>[_rodz][_pasmo] = max 1 (bez duplikatow); dzieci T3/T2/T1, szanse z pasma (n/e/l).', '']
        uzyte = set()
        for pule in PULE.values():
            for rod in pule.values():
                uzyte |= set(rod)
        pasma_sfx = {'n': '', 'e': '_e', 'l': '_l'}
        for fi, f in enumerate(sorted(uzyte)):
            if f not in AFFIKSY:
                raise SystemExit(f'nieznany affiks {f}')
            typ, formy, stat, wart, sc = AFFIKSY[f]
            if not self.atr_ok(stat):
                continue
            for rd in (['m', 'f', 'n'] if typ == 'P' else ['']):
                for pas, sfx in pasma_sfx.items():
                    c3, c2 = PASMA[pas]
                    fid = f'a_{f}' + (f'_{rd}' if rd else '') + sfx
                    out += [f'{fid}:', '  chance: 0.7', '  min: 1', '  max: 1', '  modifiers:']
                    napis = forma(rd, formy) if typ == 'P' else formy
                    klucz = 'prefix' if typ == 'P' else 'suffix'
                    for tn, ch, v in [('T3', E(c3), wart[2]), ('T2', E((1 - c3) * c2), wart[1]), ('T1', E((1 - c3) * (1 - c2)), wart[0])]:
                        tnum = int(tn[1]); s_t = sc * (1 + 0.5 * (tnum - 1))
                        out += [f'    {fid}_{tn.lower()}:', f'      chance: {ch}',
                                f'      {klucz}: {{format: {q(("<tier-color>" if typ == "P" else "") + napis)}, priority: {tnum * 100 + fi}}}',
                                '      stats:', f'        {stat}: {formula(v, s_t, 0.1, 0.25)}',
                                f'        lore: [{q(PIPS[tn] + " &7" + napis + " &8· " + STAT_KROTKO.get(stat, stat))}]']
                    out.append('')
                    if pas == 'n':
                        self.katalog['affiksy'][f'a_{f}' + (f'_{rd}' if rd else '')] = {'typ': typ, 'nazwa': napis, 'stat': stat, 'wartosci': list(wart), 'przyrost': sc}
        # efekty legendarne
        for arch, efekty in LEGENDARNE.items():
            out += [f'leg_{arch}:', '  chance: 1', '  max: 1', '  modifiers:']
            for eid, nazwa, typ, tryb, cd, opis in efekty:
                out += [f'    leg_{arch}_{eid}:', f'      chance: {E(1)}', '      stats:', '        ability:', f'          leg:',
                        f'            type: {typ}', f'            mode: {tryb}', f'            cooldown: {cd}',
                        f'        lore: [{q("&6✦ " + nazwa + " &8— &7" + opis)}]']
            out.append('')
        self.katalog['legendarne'] = {a: [{'nazwa': x[1], 'typ': x[2], 'tryb': x[3], 'cd': x[4], 'opis': x[5]} for x in v] for a, v in LEGENDARNE.items()}
        # grupy prefiksow/sufiksow na pasmo
        for arch, pule in PULE.items():
            for kat, rod in pule.items():
                for pas, sfx in pasma_sfx.items():
                    for rd in ['m', 'f', 'n']:
                        pref = [f for f in rod if AFFIKSY[f][0] == 'P' and self.atr_ok(AFFIKSY[f][2])]
                        out += [f'pref_{arch}_{kat}_{rd}{sfx}:', '  chance: 1', f'  max: {MAKS_PREF}', '  modifiers:']
                        out += [f'    a_{f}_{rd}{sfx}: {{}}' for f in pref] + ['']
                    suf = [f for f in rod if AFFIKSY[f][0] == 'S' and self.atr_ok(AFFIKSY[f][2])]
                    out += [f'suf_{arch}_{kat}{sfx}:', '  chance: 1', f'  max: {MAKS_SUF}', '  modifiers:']
                    out += [f'    a_{f}{sfx}: {{}}' for f in suf] + ['']
                # grupa rzadkosci
                for rd in ['m', 'f', 'n']:
                    out += [f'rz_{arch}_{kat}_{rd}:', '  chance: 1', '  min: 1', '  max: 1', '  modifiers:']
                    for tid, nazwa, kol, _, glow, udz in RARITY:
                        if not udz:
                            continue
                        out += [f'    rz_{tid.lower()}:', f'      chance: {E(udz)}', '      stats:', f'        tier: {tid}']
                        if tid != 'ZWYKLY':   # tier z modyfikatora nie niesie ramki tieru — ustawiamy ja wprost (QA 30.09)
                            out.append(f"        tooltip-style: 'przedmioty:{tid.lower()}'")
                        if tid in WARIANTY:
                            warianty, pas = WARIANTY[tid]; sfx = pasma_sfx[pas]
                            out += ['      min: 1', '      max: 1', '      modifiers:']
                            for i, (np_, ns_, w) in enumerate(warianty):
                                grupy = []
                                if np_: grupy.append(f'pref_{arch}_{kat}_{rd}{sfx}: {{min: {np_}, max: {np_}}}')
                                if ns_: grupy.append(f'suf_{arch}_{kat}{sfx}: {{min: {ns_}, max: {ns_}}}')
                                if tid == 'LEGENDARNY' and LEGENDARNE.get(arch): grupy.append(f'leg_{arch}: {{min: 1, max: 1}}')
                                out += [f'        w{i}:', f'          chance: {E(w)}', f'          min: {len(grupy)}', f'          max: {len(grupy)}', '          modifiers:']
                                out += [f'            {g}' for g in grupy]
                    out.append('')
        open(os.path.join(self.mi, 'modifiers', 'przedmioty.yml'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
        self.katalog['pule'] = PULE
        self.katalog['warianty'] = {k: {'sklad': [list(x) for x in v[0]], 'pasmo': v[1]} for k, v in WARIANTY.items()}
        self.katalog['pasma'] = PASMA

    # ---------------------------------------------------------- rarity (item-tiers.yml)
    def rarity(self):
        plik = os.path.join(self.mi, 'item-tiers.yml')
        t = open(plik, encoding='utf-8').read()
        # tiery demo nie biora udzialu w losowaniu (nasze szablony dostaja rarity z naszych szans)
        t0 = re.sub(r'\n?' + re.escape(ZN_A) + r'.*?' + re.escape(ZN_B) + r'\n?', '\n', t, flags=re.S)
        t0 = re.sub(r'(\n\s+chance:\s*)[0-9.]+', r'\g<1>0.0', t0)
        open(plik, 'w', encoding='utf-8').write(t0)
        L = []
        for tid, nazwa, kol, (cb, cs), glow, udz in RARITY:
            L += [f'{tid}:', f'  name: {q(kol + nazwa)}'] + ([f"  tooltip-style: 'przedmioty:{tid.lower()}'"] if tid != 'ZWYKLY' else []) + ['  unidentification:', f'    name: {q(nazwa)}', f'    prefix: {q(kol)}',
                  '    range: 2', '  item-glow:', '    hint: true', f'    color: {glow}', '  generation:', '    chance: 0.0',
                  '    capacity:', f'      base: {cb}', f'      scale: {cs}', '      spread: 0']
            if tid in ROZBIORKA:
                L += ['  deconstruct-item:', '    success:', '      coef: 1', '      items:', '        MATERIAL:']
                L += [f'          {i}: {sz},{il},0' for i, sz, il in ROZBIORKA[tid]]
            L.append('')
            self.katalog['rarity'].append({'id': tid, 'nazwa': nazwa, 'kolor': kol, 'pojemnosc': [cb, cs], 'udzial': udz})
        wstaw_blok(plik, L)

    # ---------------------------------------------------------- jakosc + kamienie + receptury
    def jakosc(self):
        L = ['jakosc:'] + [f'  {s}: {JAKOSC_KROK}' for s in JAKOSC_STATY]
        wstaw_blok(os.path.join(self.mi, 'upgrade-templates.yml'), L)
        # sufiks nazwy dla ulepszen (globalny) -> jakosc
        cfg = os.path.join(self.mi, 'config.yml')
        t = open(cfg, encoding='utf-8').read()
        t = re.sub(r"(\n\s+name-suffix: ).*", r"\1' &8[&bJakość +#lvl#%&8]'", t, count=1)
        open(cfg, 'w', encoding='utf-8').write(t)
        kons, receptury = [], {}
        for kid, nazwa, mat, ref, stacja, prof, skl, opis in KAMIENIE:
            kons += [f'{kid}:', '  base:', f'    material: {mat}', f'    name: {q("&b" + nazwa)}', '    lore:', f'    - {q("&7" + opis)}',
                     '    - \'&8Materiał jakości · z profesji\'', f'    upgrade: {{reference: {ref}, success: 100}}', '    inedible: true', '']
            rec = [f"  'prz_{kid.lower()}':", f"    output: 'mmoitems{{type=CONSUMABLE,id={kid}}}'", '    crafting-time: 1',
                   '    conditions:', f"      - 'profession{{profession={prof},level=1}}'", '    ingredients:']
            rec += [f"      - 'mmoitem{{type=MATERIAL,id={i},amount={n}}}'" for i, n in skl]
            # bez exp{}: XP rzemiosla liczy plugin Rzemioslo (profesje v2, MCMMORPG/profesje) — trigger dawal podwojne XP
            receptury.setdefault(stacja, []).extend(rec)
            self.katalog['kamienie'].append({'id': kid, 'nazwa': nazwa, 'ref': ref, 'stacja': stacja, 'skladniki': skl})
        kid, nazwa, mat, stacja, prof, skl, ile, opis = NOZ
        kons += [f'{kid}:', '  base:', f'    material: {mat}', f'    name: {q("&7" + nazwa)}', '    lore:', f'    - {q("&7" + opis)}',
                 '    can-deconstruct: true', '    inedible: true', '']
        rec = [f"  'prz_{kid.lower()}':", f"    output: 'mmoitems{{type=CONSUMABLE,id={kid},amount={ile}}}'", '    crafting-time: 1',
               '    conditions:', f"      - 'profession{{profession={prof},level=1}}'", '    ingredients:']
        rec += [f"      - 'mmoitem{{type=MATERIAL,id={i},amount={n}}}'" for i, n in skl]   # XP: plugin Rzemioslo (profesje v2)
        receptury.setdefault(stacja, []).extend(rec)
        for st_, linie in receptury.items():   # jeden blok na stacje
            wstaw_po(os.path.join(self.mi, 'crafting-stations', st_ + '.yml'), r'^recipes:', linie)
        self.katalog['rozbiorka'] = {k: [list(x) for x in v] for k, v in ROZBIORKA.items()}
        wstaw_blok(os.path.join(self.mi, 'item', 'consumable.yml'), kons)

    # ---------------------------------------------------------- lore: poziom przedmiotu (custom stat, wymaga restartu)
    def lore(self):
        cs = os.path.join(self.mi, 'custom-stats.yml')
        wstaw_blok(cs, ['CUSTOM_ILVL:', "  name: 'Poziom przedmiotu'", '  type: double', '  lore:', "    - 'Poziom przedmiotu'"])
        # nowa statystyka dziala dopiero po restarcie; BEZ_ILVL=1 -> szablony bez custom-ilvl (bez bledow w logu po reloadzie)
        self.ilvl_stat = os.environ.get('BEZ_ILVL') != '1'

    # ---------------------------------------------------------- jezyk: polskie etykiety statystyk + linie Wiary
    def jezyk(self):
        st = os.path.join(self.mi, 'language', 'stats.yml')
        t = open(st, encoding='utf-8').read()
        t = re.sub(r'\n?' + re.escape(ZN_A) + r'.*?' + re.escape(ZN_B) + r'\n?', '\n', t, flags=re.S)
        nowe = []
        for k, v in ETYKIETY.items():
            linia = f"{k}: {q(v)}"
            if re.search(rf'^{re.escape(k)}:', t, flags=re.M):
                t = re.sub(rf'^{re.escape(k)}:.*$', lambda m: linia, t, count=1, flags=re.M)
            else:
                nowe.append(linia)
        open(st, 'w', encoding='utf-8').write(t.rstrip('\n') + '\n')
        wstaw_blok(st, nowe + ["custom-ilvl: '&8Poziom przedmiotu: &7{value}'"])
        lf = os.path.join(self.mi, 'language', 'lore-format.yml')
        t = open(lf, encoding='utf-8').read()
        # uklad (krytyka 30.09): 1. rzadkosc, 2. blok setu, kreska, wymagania + poziom przedmiotu, statystyki, opis/affiksy
        t = re.sub(r"\n\s*- '#(item-type|tier|set|custom-ilvl|abilities)#'[^\n]*", '', t)
        # wymagania (w tym biegłości z tools/klasy-rasowe) przenosimy pod '#lore#', w tej samej kolejności
        wym = re.findall(r"\n\s*- '#(?:required-[a-z-]+|profession-[a-z]+)#'[^\n]*", t)
        for w in wym:
            t = t.replace(w, '', 1)
        t = t.replace("lore-format:\n", "lore-format:\n  - '#tier#'\n  - '#set#'\n  - '{bar}'\n", 1)
        t = re.sub(r"(\n(\s*)- '#lore#')", lambda m: m.group(1) + '\n' + m.group(2) + "- '{bar}'\n" + m.group(2) + "- '#custom-ilvl#'" + ''.join(wym), t, count=1)
        # biegłości (od 30.09 ten układ należy do przedmiotów; tools/klasy-rasowe nie pisze już lore-format/stats):
        # brakujące linie '#required-bieglosc-<g>#' i etykiety w stats.yml dopisujemy z oczekiwane.json, istniejących nie ruszamy
        oczek = os.path.join(os.path.dirname(self.P), 'tools', 'klasy-rasowe', 'oczekiwane.json')
        if not os.path.exists(oczek):
            oczek = '/home/dev/Desktop/Przemek/MC/tools/klasy-rasowe/oczekiwane.json'
        grupy = [g.replace('_', '-') for g in json.load(open(oczek, encoding='utf-8')).get('bieglosci', [])]
        for g in grupy:
            if f"'#required-bieglosc-{g}#'" not in t:
                t = re.sub(r"(\n(\s*)- '#required-wiara#')", lambda m: m.group(1) + '\n' + m.group(2) + f"- '#required-bieglosc-{g}#'", t, count=1)
        st_plik = os.path.join(self.mi, 'language', 'stats.yml'); st_t = open(st_plik, encoding='utf-8').read()
        opis_g = {k.replace('_', '-'): v[7] for k, v in RODZAJE.items()}
        opis_g.update({'lekka': 'zbroja lekka', 'srednia': 'zbroja średnia', 'ciezka': 'zbroja ciężka'})
        braki = [g for g in grupy if not re.search(rf'^required-bieglosc-{re.escape(g)}:', st_t, flags=re.M)]
        if braki:
            open(st_plik, 'a', encoding='utf-8').write(''.join(f"required-bieglosc-{g}: '&eWymaga biegłości: &c{opis_g.get(g, g)}'\n" for g in braki))
        for po, nowa in [("'#required-intelligence#'", "'#required-wiara#'"), ("'#additional-intelligence#'", "'#additional-wiara#'")]:
            if nowa not in t:
                t = re.sub(r"(\n(\s*)- " + re.escape(po) + r")", lambda m: m.group(1) + '\n' + m.group(2) + '- ' + nowa, t, count=1)
        t = re.sub(r"(\n\s*- '\{bar\}')(?:\n\s*- '\{bar\}')+", r"\1", t)   # idempotencja: bez powtórzonych kresek
        open(lf, 'w', encoding='utf-8').write(t)


    # ---------------------------------------------------------- lup: droptabele MythicMobs + wpiecie w bestiariusz
    def lup(self):
        mm = os.path.join(self.P, 'MythicMobs', 'droptables')
        prz = self.katalog['przedmioty']
        def wpisy(arch, poziomy):
            return [f"  - mmoitem{{type={x['typ']};id={x['id']}}} 1 1" for x in prz if x['poziom'] in poziomy and (arch is None or x['arch'] == arch)]
        L = ['# Lup systemu przedmiotow — PLIK GENEROWANY: tools/przedmioty/generuj.py (nie edytuj recznie).',
             '# Typ dropu `mmoitem` (liczba pojedyncza, MMOItemsSupport w MythicMobs 5.11.2). Poziom przedmiotu = poziom zabojcy +-2,',
             '# rarity losowana z generation.chance tierow (item-tiers.yml). TotalItems -> szanse wpisow sa wagami.',
             '# isplayer: bez gracza-zabojcy (ogien, upadek, inny mob) przedmiot mialby poziom 0 i brak rzadkosci — wtedy nic nie wypada.', '']
        for pas, cfg in LUP.items():
            nazwa = f'PRZ_LUP_{pas}'
            if self.smart:
                L += [f'{nazwa}:', '  TriggerConditions:', '  - isplayer true', '  TotalItems: 1', '  Drops:', f'  - {nazwa}_MOJ 1 {SMART_UDZIAL}', f'  - {nazwa}_DOWOLNY 1 {round(1 - SMART_UDZIAL, 2)}', '']
                L += [f'{nazwa}_MOJ:', '  Drops:'] + [f'  - {nazwa}_{a.upper()} 1 1' for a in ARCH] + ['']
                for a in ARCH:
                    L += [f'{nazwa}_{a.upper()}:', '  TriggerConditions:', f'  - stringequals{{value1="{ARCHETYP_PH}";value2={a}}}', '  TotalItems: 1', '  Drops:'] + wpisy(a, cfg['bazy']) + ['']
                L += [f'{nazwa}_DOWOLNY:', '  TotalItems: 1', '  Drops:'] + wpisy(None, cfg['bazy']) + ['']
            else:
                L += [f'{nazwa}:', '  TriggerConditions:', '  - isplayer true', '  TotalItems: 1', '  Drops:'] + wpisy(None, cfg['bazy']) + ['']
        if self.katalog.get('unikaty'):
            L += ['PRZ_LUP_UNIKAT:', '  TriggerConditions:', '  - isplayer true', '  TotalItems: 1', '  Drops:'] + \
                 [f"  - mmoitem{{type={u['typ']};id={u['id']}}} 1 1" for u in self.katalog['unikaty']] + ['']
        for lit, sz in LUP_SET.items():
            czesci = [c for s in self.katalog['sety'] for c in s['czesci'] if (s['poziom'] == 5) == (lit == 'A')]
            L += [f'PRZ_LUP_SET_{lit}:', '  TriggerConditions:', '  - isplayer true', '  TotalItems: 1', '  Drops:'] + [f"  - mmoitem{{type={c['typ']};id={c['id']}}} 1 1" for c in czesci] + ['']
        open(os.path.join(mm, 'Przedmioty.yml'), 'w', encoding='utf-8').write('\n'.join(L) + '\n')
        # wpiecie: jedna linia na tabele bestiariusza, oznaczona komentarzem (latwe wycofanie)
        best = os.path.join(mm, 'Bestiariusz.yml')
        if os.path.exists(best):
            linie = [l for l in open(best, encoding='utf-8').read().split('\n') if '# PRZEDMIOTY' not in l]
            out, tab = [], None
            for l in linie:
                m = re.match(r'^([A-Z0-9_]+):', l)
                if m: tab = m.group(1)
                out.append(l)
                if l.strip() == 'Drops:' and tab:
                    pas = next((v for k, v in WPIECIE.items() if tab.startswith(k)), None)
                    if pas:
                        out += [f'  - PRZ_LUP_{pas} 1 {LUP[pas]["szansa"]}  # PRZEDMIOTY'] * LUP[pas]['ile']
                        if pas == 'BOSS':
                            out += [f'  - PRZ_LUP_SET_{k} 1 {v}  # PRZEDMIOTY' for k, v in LUP_SET.items()]
                            out.append(f'  - PRZ_LUP_UNIKAT 1 {LUP_UNIKAT}  # PRZEDMIOTY')
            open(best, 'w', encoding='utf-8').write('\n'.join(out))
        self.katalog['lup'] = {'pasma': LUP, 'sety': LUP_SET, 'smart': ARCHETYP_PH if self.smart else None, 'wpiecie': WPIECIE}

    def unikaty(self):
        for arch, u in UNIKATY.items():
            typ, *_rest = RODZAJE[u['rodzaj']]
            grupa = GRUPA.get(u['rodzaj'], u['rodzaj'])
            st = {k: (num(v) if k == 'attack-speed' else f'{{base: {num(v)}, spread: 0.05, max-spread: 0.1}}') for k, v in u['staty'].items()}
            typu, tryb, cd, nazwa_um, opis_um = u['umiejetnosc']
            dod = {f'required-bieglosc-{grupa.replace("_", "-")}': '1.0',
                   'ability': f'{{uni: {{type: {typu}, mode: {tryb}, cooldown: {cd}}}}}'}
            lore = [f'&c✦ {nazwa_um} &8— &7{opis_um}', f'&8&o„{u["fabula"]}”', f'&8{ARCH_PL[arch]} · unikat']
            self.szablon(u['id'], typ, u['mat'], f'&c✦ {u["nazwa"]}', lore, st, UNIKAT_POZIOM, 4, arch, 'bron', 'm', dod,
                         tiered=False, tier='UNIKATOWY')
            self.katalog.setdefault('unikaty', []).append({'id': u['id'], 'typ': typ, 'arch': arch, 'nazwa': u['nazwa'],
                                                           'umiejetnosc': nazwa_um, 'opis': opis_um, 'fabula': u['fabula']})

    def liczby(self):
        cfg = os.path.join(self.P, 'MythicLib', 'config.yml')
        t = open(cfg, encoding='utf-8').read()
        t = re.sub(r"(\n\s*decimal-separator:) *\S+", r"\1 ','", t, count=1)
        open(cfg, 'w', encoding='utf-8').write(t)

    def ramki(self):
        import ramki
        cel = os.path.join(self.P, 'Nexo', 'pack', 'assets', 'przedmioty')
        sys.argv = ['ramki.py', cel]; ramki.main()

    def zapisz_typy(self):
        for typ, linie in self.per_typ.items():
            wstaw_blok(os.path.join(self.mi, 'item', typ.lower() + '.yml'), linie)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--plugins', default='/opt/docker/mc/data/world1/plugins')
    ap.add_argument('--smart', action='store_true', help='lup pod archetyp gracza (wymaga placeholdera ARCHETYP_PH na serwerze)')
    ap.add_argument('--katalog', default=os.path.join(os.path.dirname(os.path.abspath(__file__)), 'katalog.json'))
    a = ap.parse_args()
    g = Gen(a.plugins)
    g.smart = a.smart
    g.lore()
    g.jezyk()
    g.liczby()
    g.ramki()
    g.rarity()
    g.affiksy()
    g.zbroje(); g.bron(); g.akcesoria(); g.sety(); g.unikaty()
    g.jakosc()
    g.lup()
    g.zapisz_typy()
    json.dump(g.katalog, open(a.katalog, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    n = len(g.katalog['przedmioty'])
    print(f'bazy: {n}, sety: {len(g.katalog["sety"])} ({sum(len(s["czesci"]) for s in g.katalog["sety"])} części), '
          f'affiksy: {len(g.katalog["affiksy"])}, wiara: {g.wiara}, typy: {len(g.per_typ)}')

if __name__ == '__main__':
    main()
