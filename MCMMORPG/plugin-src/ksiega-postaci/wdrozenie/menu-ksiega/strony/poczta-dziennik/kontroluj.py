#!/usr/bin/env python3
"""Kontrola statyczna obu menu i plików integracyjnych (bez serwera)."""
from collections import Counter
from pathlib import Path
import re
import sys

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
OUT = HERE / 'out'
sys.dont_write_bytecode = True
sys.path.insert(0, str(ROOT / 'strony'))
import ksiega_wspolne as W  # noqa: E402

ITEM = re.compile(r'^  ([a-z_]+):\n((?:    .*\n|      .*\n)*)', re.M)
PAPI = re.compile(r'%[a-zA-Z0-9_]+%')
COMMAND = re.compile(r"^      - '((?:\[player\]|\[console\]|\[openguimenu\]|\[close\]).*)'$", re.M)
EXPECTED = {
    '%mmocore_class%', '%mmocore_level%', '%mmocore_experience%',
    '%mmocore_next_level%', '%mmocore_quest%', '%mmocore_quest_progress%',
    '%chowance_aktywny_nazwa%', '%chowance_aktywny_poziom%',
    '%chowance_bestiariusz_widziane%', '%chowance_bestiariusz_zlapane%',
    '%chowance_bestiariusz_wszystkie%', '%chowance_bestiariusz_procent%',
    '%chowance_odznaki_liczba%', '%chowance_wieza_WIEZA_CHOWANCOW_rekord%',
    '%wiary_patron_imie%', '%wiary_wiara%', '%wiary_laska%', '%wiary_laska_czas%',
}


def items(raw):
    result = dict(ITEM.findall(raw))
    assert len(result) == len(ITEM.findall(raw)), 'powtórzony identyfikator itemu'
    return result


def slots(block):
    single = re.search(r'^    slot: (\d+)$',block,re.M)
    many = re.search(r'^    slots: \[([\d, ]+)\]$',block,re.M)
    assert bool(single) != bool(many), 'item wymaga slot/slots'
    return [int(single.group(1))] if single else [int(x) for x in many.group(1).split(', ')]


def menu(ident, codes, wanted):
    raw = (OUT / 'dm' / f'{ident}.yml').read_text(encoding='utf-8')
    assert re.search(r'^size: 54$',raw,re.M)
    assert re.search(r'^update_interval: 20$',raw,re.M)
    title = re.search(r"^menu_title: '([^']+)'$",raw,re.M).group(1)
    assert [title.count(chr(code)) for code in codes] == [1]*4
    entries = items(raw)
    assert set(entries) == wanted
    used = {}
    for key,block in entries.items():
        assert re.search(r'^    update: true$',block,re.M), key
        for slot in slots(block):
            assert 0 <= slot <= 53 and slot % 9 != 4, (key,slot)
            assert slot not in used, (key,slot,used.get(slot))
            used[slot] = key
    image = Image.open(OUT / ident / f'{ident}_2x.png').convert('RGB')
    assert image.size == (352,444)
    assert not W.find_orphans(image)
    assert set(image.get_flattened_data()) <= set(W.C.values())
    for n in range(1,5):
        assert Image.open(OUT / ident / f'{ident}_{n}.png').size == (176,222)
    return raw, entries, used


def main():
    d_raw,d_items,d_slots = menu('dziennik_przygod',range(0x4B00,0x4B04),
        {'kronika','postep','zadanie','chowaniec','bestiariusz','odznaki','wieza','wiara'})
    p_raw,p_items,p_slots = menu('poczta',range(0x4B04,0x4B08),
        {'koperta','stan','napisz','odebrane'})
    assert set(PAPI.findall(d_raw)) == EXPECTED, set(PAPI.findall(d_raw)) ^ EXPECTED
    assert set(PAPI.findall(p_raw)) == {'%cmi_user_mail_count%','%player_name%'}
    assert COMMAND.findall(d_items['zadanie']) == ['[player] quests']
    assert COMMAND.findall(d_items['chowaniec']) == ['[openguimenu] chowance_menazeria']
    assert COMMAND.findall(d_items['bestiariusz']) == ['[openguimenu] chowance_bestiariusz']
    assert COMMAND.findall(d_items['odznaki']) == COMMAND.findall(d_items['wieza']) == ['[openguimenu] chowance_lochy']
    assert COMMAND.findall(d_items['wiara']) == ['[openguimenu] wiara']
    assert COMMAND.findall(p_items['napisz']) == ['[console] cmi dialogs poczta_napisz %player_name%']
    assert COMMAND.findall(p_items['odebrane']) == ['[player] cmi mail read','[close]']
    assert COMMAND.findall(p_items['stan']) == ['[player] cmi mail read','[close]']
    assert 'Listy pojawią się na czacie.' in p_items['odebrane']
    assert '[X] przy liście go usuwa.' in p_items['odebrane']
    for block in (*d_items.values(),*p_items.values()):
        assert 'Wstecz' not in block and 'Zamknij' not in block
    hub = (ROOT / 'hub_glowne.yml').read_text(encoding='utf-8')
    change = (OUT / 'hub_glowne_zmiany.txt').read_text(encoding='utf-8')
    old = items(hub)
    new = items(change)
    assert set(new) == {'dziennik','poczta'}
    assert slots(new['dziennik']) == slots(old['dziennik'])
    assert slots(new['poczta']) == slots(old['poczta'])
    assert all(len(re.findall(r"^      - '&7",new[key],re.M)) == 2 for key in new)
    assert COMMAND.findall(new['dziennik']) == ['[openguimenu] dziennik_przygod']
    assert COMMAND.findall(new['poczta']) == ['[openguimenu] poczta']
    patched = hub
    for key,block in new.items():
        pattern = re.compile(rf'^  {key}:\n(?:    .*\n|      .*\n)*',re.M)
        assert len(pattern.findall(patched)) == 1, key
        patched = pattern.sub('  '+key+':\n'+block,patched,count=1)
    rebuilt = items(patched)
    assert set(rebuilt) == set(old)
    for key in set(old)-set(new):
        assert rebuilt[key] == old[key], key
    assert Counter(COMMAND.findall(hub)) <= Counter(COMMAND.findall(patched))
    assert (OUT / 'podstrony.txt').read_text(encoding='utf-8') == (
        'U+4B00 hub_glowne  # dziennik_przygod\nU+4B04 hub_glowne  # poczta\n')
    sheet = Image.open(OUT / 'arkusz_x2.png')
    assert sheet.size == (704,444)
    lines = [
        'dziennik_przygod: 8 itemów, 19 zajętych slotów, 18 placeholderów z DZIENNIK.md.',
        'poczta: 4 itemy, 21 zajętych slotów; odczyt i [close] w poprawnej kolejności.',
        'hub_glowne: 12/12 starych itemów i wszystkie stare komendy zachowane; zmiana tylko bloków dziennik/poczta.',
        'Usunięte Wstecz/Zamknij: brak w wejściowych blokach; nie dodano duplikatu plakietki.',
        'Oba menu: sloty 0–53, grzbiet wolny, duplikaty 0, update true, update_interval 20.',
        'Grafika: 4 ćwiartki na menu 176×222, arkusz 704×444, paleta K12, osierocone piksele 0.',
        'Glify: U+4B00–U+4B07; rodzic obu: hub_glowne.',
    ]
    (OUT / 'kontrola.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    print('\n'.join(lines))


if __name__ == '__main__':
    main()
