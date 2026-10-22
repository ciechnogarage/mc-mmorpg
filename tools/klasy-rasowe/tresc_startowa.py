#!/usr/bin/env python3
"""Treść bazowa klas rasowych (2026-09-26) — zbroje z wagą, broń startowa każdej grupy, domknięcie
dziur w biegłościach. Idempotentny: każdy plik dostaje blok między znacznikami ZNACZNIK_*, podmieniany
przy ponownym uruchomieniu. Uruchomienie z world1: python3 tools/klasy-rasowe/tresc_startowa.py,
potem `mi reload` i `ct reload`.

Co robi:
 1. MMOItems/item/*.yml: zestawy zbroi (tkanina, lekka, średnia, ciężka) i broń startowa dla 21 grup.
 2. armor.yml: wagi dla zbroi-przykładów MMOItems wg materiału (skóra=lekka, kolczuga/złoto=średnia,
    żelazo/diament/netheryt=ciężka; głowy i elytry bez wagi).
 3. language/lore-format.yml + stats.yml: wymaganie biegłości WIDAĆ w opisie przedmiotu.
 4. CoreTools/item-replacer.yml: vanillowa zbroja/broń (drop, skrzynia, handel, kowadło, wejście)
    zamienia się w nasz odpowiednik z wagą — inaczej żelazna zbroja z moba omija biegłości.
 5. MMOItems/config.yml: vanillowe receptury zbroi i broni wyłączone (treść z profesji, nie z warsztatu).
 6. CMI: przykładowy zestaw 'Newbie' (kamień z Cierniami V, deski) nie jest już dawany nowym graczom.
"""
import os, re, sys
sys.path.insert(0, os.path.dirname(__file__))
from generuj_klasy import BRON, P

ZN_A, ZN_B = '# >>> KLASY-RASOWE: treść generowana (tools/klasy-rasowe/tresc_startowa.py) — NIE EDYTOWAĆ RĘCZNIE', '# <<< KLASY-RASOWE'
WAGA_PL = {'tkanina': 'tkanina', 'lekka': 'lekka', 'srednia': 'średnia', 'ciezka': 'ciężka'}

# ---- zbroje: id, waga, materiał, nazwy (hełm, pierś, nogi, buty), pancerz, wytrzymałość, extra ----
SLOTY = [('HELM', 'HELMET'), ('NAPIERSNIK', 'CHESTPLATE'), ('NOGI', 'LEGGINGS'), ('BUTY', 'BOOTS')]
ZESTAWY = [
    ('SZATA_LNIANA', 'tkanina', 'LEATHER', ['Lniany kaptur', 'Lniana szata', 'Lniane spodnie', 'Lniane trzewiki'], [1, 2, 1, 1], 0, {'max-mana': 3.0, 'dye-color': '225 215 190', 'hide-dye': 'true'}, None),
    ('ZBROJA_SKORZANA', 'lekka', 'LEATHER', ['Skórzany kaptur', 'Skórzana kurta', 'Skórzane spodnie', 'Skórzane buty'], [1, 3, 2, 1], 0, {'dye-color': '120 80 45', 'hide-dye': 'true'}, 'LEATHER'),
    ('KOLCZUGA', 'srednia', 'CHAINMAIL', ['Kolczy czepiec', 'Kolczuga', 'Kolcze nogawice', 'Kolcze buty'], [2, 5, 4, 1], 0, {}, 'CHAINMAIL'),
    ('ZBROJA_ZLOCONA', 'srednia', 'GOLDEN', ['Złocony hełm', 'Złocony napierśnik', 'Złocone nagolenniki', 'Złocone buty'], [2, 5, 3, 1], 0, {}, 'GOLDEN'),
    ('ZBROJA_ZELAZNA', 'ciezka', 'IRON', ['Żelazny hełm', 'Żelazny napierśnik', 'Żelazne nagolenniki', 'Żelazne buty'], [2, 6, 5, 2], 0, {}, 'IRON'),
    ('ZBROJA_DIAMENTOWA', 'ciezka', 'DIAMOND', ['Diamentowy hełm', 'Diamentowy napierśnik', 'Diamentowe nagolenniki', 'Diamentowe buty'], [3, 8, 6, 3], 2, {}, 'DIAMOND'),
    ('ZBROJA_NETHERYTOWA', 'ciezka', 'NETHERITE', ['Netherytowy hełm', 'Netherytowy napierśnik', 'Netherytowe nagolenniki', 'Netherytowe buty'], [3, 8, 6, 3], 3, {'knockback-resistance': 0.1}, 'NETHERITE'),
]
# zestaw startowy wg najcięższej wagi klasy (ciężkiej nie ma żadna klasa bazowa)
START_ZBROJA = {'tkanina': 'SZATA_LNIANA', 'lekka': 'ZBROJA_SKORZANA', 'srednia': 'KOLCZUGA'}

# ---- broń: grupa -> startowa (typ, id, materiał, nazwa, statystyki) ------------------------------
def s(**k): return k
START_BRON = {
    'miecz':            ('SWORD', 'MIECZ_KAMIENNY', 'STONE_SWORD', '&fKamienny miecz', s(**{'attack-damage': 5.0, 'attack-speed': 1.6})),
    'sztylet':          ('DAGGER', 'SZTYLET_REKRUTA', 'IRON_SWORD', '&fSztylet rekruta', s(**{'attack-damage': 3.5, 'attack-speed': 2.2, 'critical-strike-chance': 5.0})),
    'luk':              ('BOW', 'LUK_MYSLIWSKI', 'BOW', '&fŁuk myśliwski', s(**{'attack-damage': 6.0})),
    'kostur':           ('WAND', 'ROZDZKA_UCZNIA', 'STICK', '&fRóżdżka ucznia', s(**{'attack-damage': 3.0, 'attack-speed': 1.15, 'range': 20.0, 'magic-damage': 5.0})),
    'bulawa':           ('HAMMER', 'BULAWA_NOWICJUSZA', 'IRON_SHOVEL', '&fBuława nowicjusza', s(**{'attack-damage': 5.5, 'attack-speed': 1.2})),
    'tarcza':           ('SHIELD', 'TARCZA_DREWNIANA', 'SHIELD', '&fDrewniana tarcza', s(**{'armor': 2.0, 'block-power': 30.0, 'block-rating': 10.0})),
    'drzewcowa':        ('HALBERD', 'HALABARDA_STRAZNIKA', 'IRON_AXE', '&fHalabarda strażnika', s(**{'attack-damage': 7.0, 'attack-speed': 1.0, 'two-handed': 'true'})),
    'kusza':            ('CROSSBOW', 'KUSZA_PROSTA', 'CROSSBOW', '&fProsta kusza', s(**{'attack-damage': 7.0})),
    'grimuar':          ('TOME', 'GRIMUAR_UCZNIA', 'BOOK', '&fGrimuar ucznia', s(**{'magic-damage': 5.0, 'max-mana': 5.0})),
    'klinga':           ('KATANA', 'KLINGA_GAJU', 'IRON_SWORD', '&aKlinga gaju', s(**{'attack-damage': 5.5, 'attack-speed': 1.8})),
    'luk_dlugi':        ('GREATBOW', 'LUK_DLUGI_KNIEI', 'BOW', '&aŁuk długi kniei', s(**{'attack-damage': 8.0, 'arrow-velocity': 1.3, 'two-handed': 'true'})),
    'kostur_dwureczny': ('GREATSTAFF', 'KOSTUR_ZYWEGO_DREWNA', 'STICK', '&aKostur z żywego drewna', s(**{'attack-damage': 6.0, 'attack-speed': 0.9, 'magic-damage': 8.0, 'two-handed': 'true'})),
    'sierp':            ('SIERP', 'SIERP_KSIEZYCOWY', None, None, None),   # już jest (latacz_broni.py)
    'topor':            ('AXE', 'TOPOR_BOJOWY', 'STONE_AXE', '&cTopór bojowy', s(**{'attack-damage': 7.0, 'attack-speed': 0.9})),
    'wlocznia':         ('SPEAR', 'OSZCZEP', 'TRIDENT', '&cOszczep', s(**{'attack-damage': 6.0, 'attack-speed': 1.1})),
    'totem':            ('TALISMAN', 'TOTEM_PLEMIENNY', 'BONE', '&cTotem plemienny', s(**{'skill-damage': 10.0, 'max-mana': 5.0, 'disable-interaction': 'true'})),
    'mlot_dwureczny':   ('GREATHAMMER', 'MLOT_BOJOWY', 'MACE', '&cMłot bojowy', s(**{'attack-damage': 8.0, 'attack-speed': 0.8, 'two-handed': 'true'})),
    'miecz_dwureczny':  ('GREATSWORD', 'MIECZ_RUNICZNY', 'NETHERITE_SWORD', '&8Runiczny miecz', s(**{'attack-damage': 8.5, 'attack-speed': 0.9, 'two-handed': 'true'})),
    'lancuch':          ('WHIP', 'LANCUCH_GROBOWY', 'IRON_CHAIN', '&8Łańcuch grobowy', s(**{'attack-damage': 4.5, 'attack-speed': 1.7})),
    'filakterium':      ('FILAKTERIUM', 'FILAKTERIUM_KOSCIANE', None, None, None),
    'kosa':             ('KOSA', 'KOSA_SPACZONA', None, None, None),
}
# odpowiedniki vanillowej broni (zamiennik) — miecze wg materiału, reszta = broń startowa grupy
MIECZE = [('WOODEN', 'MIECZ_DREWNIANY', 'Drewniany miecz', 4.0), ('STONE', 'MIECZ_KAMIENNY', None, 5.0), ('IRON', 'MIECZ_ZELAZNY', 'Żelazny miecz', 6.0),
          ('GOLDEN', 'MIECZ_ZLOTY', 'Złoty miecz', 4.0), ('DIAMOND', 'MIECZ_DIAMENTOWY', 'Diamentowy miecz', 7.0), ('NETHERITE', 'MIECZ_NETHERYTOWY', 'Netherytowy miecz', 8.0)]
VANILLA_BRON = {'bow': ('BOW', 'LUK_MYSLIWSKI'), 'crossbow': ('CROSSBOW', 'KUSZA_PROSTA'), 'shield': ('SHIELD', 'TARCZA_DREWNIANA'),
                'trident': ('SPEAR', 'OSZCZEP'), 'mace': ('GREATHAMMER', 'MLOT_BOJOWY')}
# vanillowych siekier NIE zamieniamy: to narzędzie do drewna (profesje), a zamiana dałaby im wymaganie biegłości

def yaml_val(v):
    return v if isinstance(v, str) else (repr(float(v)))

def przedmiot(pid, material, nazwa, lore, stat, grupa=None, waga=None):
    out = [f'{pid}:', '  base:', f'    material: {material}', f"    name: '{nazwa}'", '    lore:'] + [f"      - '{l}'" for l in lore]
    out += ['    required-level: 1.0', '    tier: COMMON']
    if waga and waga != 'tkanina':
        out.append(f'    required-bieglosc-{waga}: 1.0')
    if grupa:
        out.append(f'    required-bieglosc-{grupa.replace("_", "-")}: 1.0')
    for k, v in stat.items():
        out.append(f'    {k}: {yaml_val(v)}')
    return out

def wstaw_blok(plik, linie):
    t = open(plik, encoding='utf-8').read() if os.path.exists(plik) else ''
    t = re.sub(re.escape(ZN_A) + r'.*?' + re.escape(ZN_B) + r'\n?', '', t, flags=re.S).rstrip('\n')
    t = (t + '\n\n' if t else '') + '\n'.join([ZN_A] + linie + [ZN_B]) + '\n'
    open(plik, 'w', encoding='utf-8').write(t)

def main():
    item = lambda typ: P('MMOItems', 'item', typ.lower() + '.yml')
    # 1a. zbroje
    zb = []
    for pid, waga, mat, nazwy, pancerz, tough, extra, _ in ZESTAWY:
        for (sid, smat), n, a in zip(SLOTY, nazwy, pancerz):
            st = {'armor': float(a)}
            if tough: st['armor-toughness'] = float(tough)
            st.update(extra)
            zb += przedmiot(f'{pid}_{sid}', f'{mat}_{smat}', f'&f{n}', [f'&7Zbroja: &f{WAGA_PL[waga]}'], st, waga=waga)
    zb += przedmiot('HELM_ZOLWI', 'TURTLE_HELMET', '&fŻółwi hełm', ['&7Zbroja: &flekka'], {'armor': 2.0}, waga='lekka')
    wstaw_blok(item('ARMOR'), zb)
    # 1b. broń startowa + miecze
    per_typ = {}
    for g, (typ, pid, mat, nazwa, st) in START_BRON.items():
        if mat is None: continue
        per_typ.setdefault(typ, []).extend(przedmiot(pid, mat, nazwa, [f'&7Broń: &f{BRON[g][0]}'], st, grupa=g))
    for mat, pid, nazwa, dmg in MIECZE:
        if nazwa is None: continue
        per_typ.setdefault('SWORD', []).extend(przedmiot(pid, f'{mat}_SWORD', f'&f{nazwa}', ['&7Broń: &fmiecze'], {'attack-damage': dmg, 'attack-speed': 1.6}, grupa='miecz'))
    for typ, linie in per_typ.items():
        wstaw_blok(item(typ), linie)

    # 2. wagi zbroi-przykładów i starych zestawów TEST_<KLASA> (poza TEST_ZBROJA_* z wagą ręczną i naszym blokiem)
    f = item('ARMOR'); t = open(f, encoding='utf-8').read()
    glowa, blok = t.split(ZN_A)
    WAGI_MAT = {'LEATHER': 'lekka', 'CHAINMAIL': 'srednia', 'GOLDEN': 'srednia', 'IRON': 'ciezka', 'DIAMOND': 'ciezka', 'NETHERITE': 'ciezka', 'TURTLE': 'lekka'}
    linie, out, i, zm = glowa.split('\n'), [], 0, []
    while i < len(linie):
        l = linie[i]; out.append(l)
        m = re.match(r'^([A-Z0-9_]+):', l)
        if m and not m.group(1).startswith('TEST_ZBROJA_') and i + 1 < len(linie) and linie[i + 1] == '  base:':
            j = i + 2; blk = []
            while j < len(linie) and not re.match(r'^[A-Za-z0-9_#]', linie[j]):
                blk.append(linie[j]); j += 1
            mat = next((re.match(r'^    material: (\S+)', b).group(1) for b in blk if re.match(r'^    material: ', b)), '')
            waga = next((w for k, w in WAGI_MAT.items() if mat.startswith(k + '_')), None)
            if waga and not any('required-bieglosc-' in b for b in blk):
                out.append('  base:'); out.append(f'    required-bieglosc-{waga}: 1.0'); i += 2; zm.append(f'{m.group(1)}={waga}'); continue
        i += 1
    open(f, 'w', encoding='utf-8').write('\n'.join(out) + ZN_A + blok)

    # 3. opis przedmiotu: linie wymagań biegłości — OD 30.09 WYŁĄCZONE (domyślnie). Układ opisu
    # (language/lore-format.yml, stats.yml) należy do generatora przedmiotów v2 (tools/przedmioty/generuj.py),
    # który przeniósł #required-bieglosc-* pod #lore#. Ten krok usuwał je i wstawiał za #required-intelligence#
    # — ponowne uruchomienie rozsypałoby układ v2 albo skasowało wymagania z opisów. Zmiana grup broni:
    # najpierw ten skrypt, potem tools/przedmioty/generuj.py. Wymuszenie starego zachowania: LORE_FORMAT_Z_KLAS=1.
    if not os.environ.get('LORE_FORMAT_Z_KLAS'):
        print('pominięto krok 3 (lore-format/stats.yml): należy do tools/przedmioty/generuj.py')
    else:
        lf = P('MMOItems', 'language', 'lore-format.yml'); t = open(lf, encoding='utf-8').read()
        wiersze = [f"  - '#required-bieglosc-{x.replace('_', '-')}#'" for x in ['lekka', 'srednia', 'ciezka'] + list(BRON)]
        t = re.sub(r"\n  - '#required-bieglosc-[^\n]*", '', t)
        t = t.replace("  - '#required-intelligence#'\n", "  - '#required-intelligence#'\n" + '\n'.join(wiersze) + '\n', 1)
        open(lf, 'w', encoding='utf-8').write(t)
        sf = P('MMOItems', 'language', 'stats.yml'); t = open(sf, encoding='utf-8').read()
        t = re.sub(r'\nrequired-bieglosc-[^\n]*', '', t).rstrip('\n')
        nazwy = {**{'lekka': 'zbroja lekka', 'srednia': 'zbroja średnia', 'ciezka': 'zbroja ciężka'}, **{g: n for g, (n, _) in BRON.items()}}
        t += '\n' + '\n'.join(f"required-bieglosc-{k.replace('_', '-')}: '&eWymaga biegłości: &c{v}'" for k, v in nazwy.items()) + '\n'
        open(sf, 'w', encoding='utf-8').write(t)

    # 4. zamiennik vanilla -> nasze
    zrodla, cele = [], []
    for pid, waga, mat, nazwy, *_ , vmat in ZESTAWY:
        if not vmat: continue
        for sid, smat in SLOTY:
            cel = f'kr_{pid}_{sid}'.lower()
            zrodla.append(f'    - vanilla{{id={vmat.lower()}_{smat.lower()};targets="{cel}"}}')
            cele.append(f'    - {cel}{{plugin=mmoitems;category=ARMOR;id={pid}_{sid};amount=0;weigh=100}}')
    zrodla.append('    - vanilla{id=turtle_helmet;targets="kr_helm_zolwi"}')
    cele.append('    - kr_helm_zolwi{plugin=mmoitems;category=ARMOR;id=HELM_ZOLWI;amount=0;weigh=100}')
    for mat, pid, *_ in MIECZE:
        zrodla.append(f'    - vanilla{{id={mat.lower()}_sword;targets="kr_{pid.lower()}"}}')
        cele.append(f'    - kr_{pid.lower()}{{plugin=mmoitems;category=SWORD;id={pid};amount=0;weigh=100}}')
    for vm, (typ, pid) in VANILLA_BRON.items():
        zrodla.append(f'    - vanilla{{id={vm};targets="kr_{pid.lower()}"}}')
        cele.append(f'    - kr_{pid.lower()}{{plugin=mmoitems;category={typ};id={pid};amount=0;weigh=100}}')
    ir = P('CoreTools', 'item-replacer.yml')
    open(ir, 'w', encoding='utf-8').write('\n'.join([
        '# ZAMIENNIK PRZEDMIOTÓW (2026-09-26) — PLIK GENEROWANY: tools/klasy-rasowe/tresc_startowa.py.',
        '# Vanillowa zbroja i broń nie ma wymagań biegłości (MMOItems ich nie zna), więc żelazny napierśnik',
        '# z moba albo ze skrzyni zakładała każda klasa. Każdy vanillowy element zbroi/broni zamienia się w nasz',
        '# odpowiednik z wagą/grupą. Siekiery zostają vanillowe (narzędzie do drewna). Docs:',
        '# https://gitlab.com/ranetdev/CoreTools/-/wikis/features/item_replacer',
        'item_replacer:', '  enabled: true', '', '  Conditions_all_need_true: true', '  Conditions: []', '',
        '  triggers:', '    pickup: true', '    pickup_item: false', '    clicked: true', '    join: true',
        '    bucket: false', '    craft: true', '    smelt: false', '    smith: true',
        '    chest: [chest, ender_chest, trapped_chest, chest_minecart, barrel, dispenser, hopper, hopper_minecart, _raft, _boat, _shulker_box ]',
        '    chest_check_one_time: true', '    lootin: false', '    justlootit: false', '    inventory: false', '    timer: 120  # zapas: skan ekwipunku co 2 min (np. handel z osadnikiem prosto do ręki)', '',
        '  keeper:', '    enabled: true', '    keep:', '      enchantments: true', '      display_name: false',
        '    blacklists:', '      materials: []', '      enchantments: []', '',
        '  source_items:', *zrodla, '', '  target_items:', *cele, '']))

    # 5. vanillowe receptury zbroi i broni wyłączone
    cf = P('MMOItems', 'config.yml'); t = open(cf, encoding='utf-8').read()
    mats = [f'{v}_{sm}' for *_, v in ZESTAWY if v for _, sm in SLOTY] + ['TURTLE_HELMET'] + \
           [f'{m}_SWORD' for m, *_ in MIECZE] + ['BOW', 'CROSSBOW', 'SHIELD', 'MACE']
    nowa = 'disable-vanilla-recipes:\n' + ''.join(f'- {m}\n' for m in mats)
    t = re.sub(r'disable-vanilla-recipes:(?: \[\])?\n(?:- [A-Z_]+\n)*', nowa, t, count=1)
    if '# KLASY-RASOWE: receptury' not in t:
        t = t.replace(nowa, '# KLASY-RASOWE: receptury zbroi i broni wyłączone (tools/klasy-rasowe/tresc_startowa.py) —\n# zbroja i broń mają pochodzić z profesji/łupów jako przedmioty MMOItems z wagą.\n' + nowa, 1)
    open(cf, 'w', encoding='utf-8').write(t)

    # 6. CMI: zestaw Newbie nie jest dawany nowym graczom
    cmi = P('CMI', 'config.yml'); t = open(cmi, encoding='utf-8').read()
    t2 = re.sub(r"(\nNewbie:\n  # Kit name to give for new players joining server\n  Kit: )Newbie", r"\1''", t)
    open(cmi, 'w', encoding='utf-8').write(t2)

    print(f'zbroje: {len(ZESTAWY) * 4 + 1}, broń startowa+miecze: {sum(1 for v in START_BRON.values() if v[2]) + 5}, '
          f'wagi przykładów: {len(zm)} ({", ".join(zm[:6])}...), zamiennik: {len(zrodla)} reguł, receptury: {len(mats)}, '
          f'CMI Newbie wyłączony: {t != t2 or "Kit: " + chr(39)*2 in t}')

if __name__ == '__main__':
    main()
