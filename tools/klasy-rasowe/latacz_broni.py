#!/usr/bin/env python3
"""Broń MMOItems -> biegłości broni (2026-09-26). Idempotentny.

1. Nowe typy SIERP, KOSA, FILAKTERIUM w item-types.yml + po 1 przedmiocie startowym (item/<typ>.yml).
2. Każdy przedmiot typu z generuj_klasy.BRON dostaje `required-bieglosc-<grupa>: 1` w base.
3. `required-class` znika ze WSZYSTKICH przedmiotów (także zestawów TEST_*): wskazywał klasy
   archetypów (Warrior, Mage...), których po przejściu na klasy rasowe NIKT nie ma — skrypt
   CoreTools anuluje nawet `class force WARRIOR` — więc zablokowałby przedmiot każdemu; część
   wpisów (Archer, Brute, Assassin, Wizard) i tak była martwa.
Po zmianie: `mi reload` (typy i przedmioty); nowe atrybuty MMOCore wymagają RESTARTU.
"""
import os, re, sys
sys.path.insert(0, os.path.dirname(__file__))
from generuj_klasy import BRON, P

TYPY_NOWE = {
    'SIERP': ('IRON_HOE', 'Sierp', 'SWORD', 'slashing_attack_effect'),
    'KOSA': ('NETHERITE_HOE', 'Kosa', 'SWORD', 'slashing_attack_effect'),
    'FILAKTERIUM': ('ECHO_SHARD', 'Filakterium', 'CATALYST', None),
}
STARTOWE = {
    'SIERP': """# Sierpy — broń rasowa Wyroczni (Elf x Akolita). Typ SIERP: item-types.yml (2026-09-26).
SIERP_KSIEZYCOWY:
  base:
    material: IRON_HOE
    name: '&fSierp księżycowy'
    lore:
      - '&7Srebrzysty sierp kapłanek księżyca.'
    required-level: 1.0
    tier: COMMON
    attack-damage: 5.0
    attack-speed: 1.6
    required-bieglosc-sierp: 1.0
""",
    'KOSA': """# Kosy — broń rasowa Spaczonego Akolity (Nieumarły x Akolita). Typ KOSA: item-types.yml (2026-09-26).
KOSA_SPACZONA:
  base:
    material: NETHERITE_HOE
    name: '&5Spaczona kosa'
    lore:
      - '&7Ostrze tnie ciało i wiarę.'
    required-level: 1.0
    tier: COMMON
    attack-damage: 7.0
    attack-speed: 1.0
    two-handed: true
    required-bieglosc-kosa: 1.0
""",
    'FILAKTERIUM': """# Filakteria — fokus rasowy Maga Grobu (Nieumarły x Mag), dzierżony w ręce jak katalizator.
# Osobny typ, bo CATALYST to ogólne przedmioty-statystyki dla każdego. Typ: item-types.yml (2026-09-26).
FILAKTERIUM_KOSCIANE:
  base:
    material: ECHO_SHARD
    name: '&5Kościane filakterium'
    lore:
      - '&7Naczynie na duszę lisza.'
    required-level: 1.0
    tier: COMMON
    magic-damage: 4.0
    max-mana: 5.0
    required-bieglosc-filakterium: 1.0
""",
}

def typy():
    f = P('MMOItems', 'item-types.yml')
    t = open(f, encoding='utf-8').read()
    for tid, (disp, nazwa, rodzic, atak) in TYPY_NOWE.items():
        if re.search(rf'^{tid}:', t, re.M):
            continue
        t = t.rstrip('\n') + f"""

# Klasy rasowe (2026-09-26): broń rasowa, biegłość bieglosc_{tid.lower()} (tools/klasy-rasowe/latacz_broni.py).
{tid}:
    display: {disp}
    name: '{nazwa}'
    parent: '{rodzic}'
    unident-item:
        name: '&f#prefix#Niezidentyfikowany: {nazwa}'
        lore:
        - '&7Ten przedmiot jest niezidentyfikowany.'
        - '{{tier}}'
        - '{{tier}}&8Item Info:'
        - '{{range}}&8- &7Lvl Range: &e#range#'
        - '{{tier}}&8- &7Item Tier: #prefix##tier#'
""" + (f'    on-attack: {atak}\n' if atak else '')
    open(f, 'w', encoding='utf-8').write(t)
    for tid, tresc in STARTOWE.items():
        plik = P('MMOItems', 'item', tid.lower() + '.yml')
        if not os.path.exists(plik):
            open(plik, 'w', encoding='utf-8').write(tresc)

def przedmioty():
    typ_grupa = {typ: g for g, (_, ts) in BRON.items() for typ in ts}
    raport = {}
    katalog = P('MMOItems', 'item')
    for plik in sorted(os.listdir(katalog)):
        typ = plik[:-4].upper()
        linie = open(os.path.join(katalog, plik), encoding='utf-8').read().split('\n')
        out, pid, i, zm = [], None, 0, 0
        while i < len(linie):
            l = linie[i]
            m = re.match(r'^([A-Za-z0-9_]+):', l)
            if m:
                pid = m.group(1)
            # usuń required-class (poza TEST_*) razem z listą pod spodem
            if re.match(r'^    required-class:', l) and pid:
                i += 1
                while i < len(linie) and re.match(r'^    ( *)- ', linie[i]):
                    i += 1
                zm += 1; raport.setdefault(plik, []).append(f'{pid}: -required-class')
                continue
            out.append(l)
            if l == '  base:' and typ in typ_grupa:
                klucz = f'required-bieglosc-{typ_grupa[typ].replace("_", "-")}'
                # czy przedmiot już ma to wymaganie?
                j = i + 1
                blok = []
                while j < len(linie) and not re.match(r'^[A-Za-z0-9_]+:', linie[j]):
                    blok.append(linie[j]); j += 1
                if not any(b.strip().startswith(klucz + ':') for b in blok):
                    out.append(f'    {klucz}: 1.0')
                    zm += 1; raport.setdefault(plik, []).append(f'{pid}: +{klucz}')
            i += 1
        if zm:
            open(os.path.join(katalog, plik), 'w', encoding='utf-8').write('\n'.join(out))
    return raport

if __name__ == '__main__':
    typy()
    r = przedmioty()
    for plik, zm in r.items():
        print(plik, '|', '; '.join(zm))
    print('plikow:', len(r), 'zmian:', sum(len(z) for z in r.values()))
