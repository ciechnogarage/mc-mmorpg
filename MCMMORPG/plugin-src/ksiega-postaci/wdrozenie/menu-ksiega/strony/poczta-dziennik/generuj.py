#!/usr/bin/env python3
"""Dwie karty Księgi: Dziennik przygód i Poczta. Uruchom z katalogu MK."""
from pathlib import Path
import re
import sys

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.dont_write_bytecode = True
sys.path.insert(0, str(ROOT / 'strony'))
import ksiega_wspolne as W  # noqa: E402
from siatka import siatka as S  # noqa: E402

OUT = HERE / 'out'
EMBLEM = [0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21]
JOURNAL = {'postep': 38, 'zadanie': 23, 'chowaniec': 24,
           'bestiariusz': 25, 'odznaki': 32, 'wieza': 33, 'wiara': 34}
MAIL = {'stan': 38, 'napisz': [23, 24, 32, 33], 'odebrane': [25, 26, 34, 35]}
GLYPHS = {'dziennik_przygod': range(0x4B00, 0x4B04),
          'poczta': range(0x4B04, 0x4B08)}


def book_and_quill(x, y):
    W.R((x, y, x+51, y+38), 'blue0')
    W.L([(x+3,y+32),(x+49,y+32)],'blue1',2)
    # Open chronicle, visible folded leaves and ruled pages.
    W.G([(x+4,y+12),(x+11,y+9),(x+22,y+11),(x+26,y+15),
         (x+26,y+35),(x+18,y+30),(x+4,y+29)],'paper1','ink')
    W.G([(x+26,y+15),(x+34,y+11),(x+47,y+9),(x+48,y+29),
         (x+35,y+30),(x+26,y+35)],'paper2','ink')
    W.L([(x+26,y+16),(x+26,y+34)],'gold0',2)
    for yy in (16,20,24):
        W.L([(x+8,y+yy),(x+20,y+yy+1)],'gold0')
        W.L([(x+32,y+yy+1),(x+43,y+yy)],'gold0')
    W.L([(x+13,y+5),(x+39,y+27)],'ink',4)
    W.L([(x+14,y+4),(x+37,y+24)],'gold2',2)
    W.G([(x+9,y+1),(x+17,y+2),(x+21,y+8),(x+16,y+13),
         (x+13,y+8)],'paper4','gold0')
    W.L([(x+35,y+22),(x+41,y+29)],'red1',2)
    W.R((x+40,y+27,x+42,y+30),'ink')


def sealed_letter(x, y):
    W.R((x,y,x+51,y+38),'blue0')
    W.R((x+4,y+31,x+48,y+35),'wood1','ink')
    W.G([(x+5,y+9),(x+43,y+6),(x+48,y+27),(x+10,y+31)],'paper1','ink')
    W.G([(x+5,y+9),(x+25,y+21),(x+43,y+6)],'paper4','gold0')
    W.G([(x+10,y+30),(x+25,y+19),(x+48,y+27)],'paper2','gold0')
    W.E((x+21,y+16,x+30,y+25),'red1','red0')
    W.R((x+24,y+19,x+27,y+22),'gold2')
    W.L([(x+7,y+5),(x+13,y+3)],'blue2',2)


def feather_page(x, y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+8,y+11),(x+39,y+11),(x+44,y+34),(x+13,y+34)],'paper2','ink')
    for yy in (18,22,26,30):
        W.L([(x+16,y+yy),(x+38,y+yy)],'gold0')
    W.G([(x+12,y+3),(x+22,y+1),(x+29,y+7),(x+21,y+16),
         (x+16,y+22),(x+13,y+15)],'paper4','gold0')
    W.L([(x+12,y+26),(x+25,y+6)],'blue1',2)
    W.R((x+10,y+28,x+13,y+31),'red0','ink')


def open_mailbox(x, y):
    W.R((x,y,x+51,y+38),'blue0')
    W.R((x+5,y+28,x+46,y+35),'wood1','ink')
    W.R((x+10,y+9,x+40,y+30),'wood2','ink')
    W.G([(x+10,y+10),(x+13,y+5),(x+36,y+5),(x+40,y+10)],'wood3','ink')
    W.R((x+15,y+13,x+36,y+26),'paper2','ink')
    W.G([(x+15,y+13),(x+26,y+20),(x+36,y+13)],'paper4','gold0')
    W.G([(x+10,y+10),(x+25,y+23),(x+40,y+10),(x+40,y+29),
         (x+10,y+29)],'wood1','ink')
    W.L([(x+14,y+13),(x+25,y+22),(x+36,y+13)],'gold1',2)
    W.R((x+42,y+12,x+45,y+21),'red1','ink')
    W.R((x+41,y+11,x+48,y+13),'gold2','gold0')


def draw(ident):
    W.new_canvas()
    W.draw_book()
    W.draw_title('DZIENNIK' if ident == 'dziennik_przygod' else 'POCZTA')
    W.hero_frame(14,34,book_and_quill if ident == 'dziennik_przygod' else sealed_letter)
    if ident == 'dziennik_przygod':
        S.heading('Postęp',22,157,146)
        S.group_frame(45,127,176,218,[38])
        S.foot_ornament(86,235)
        S.heading('Twoja przygoda',194,330,77)
        S.group_frame(198,326,103,177,list(JOURNAL.values())[1:])
        S.foot_ornament(262,226)
    else:
        S.heading('Skrzynka',22,157,146)
        S.group_frame(45,127,176,218,[38])
        S.foot_ornament(86,235)
        S.heading('Listy',194,330,77)
        W.R((191,102,336,181),'paper1','gold0')
        W.R((193,104,334,179),'paper3','paper0')
        W.tile(194,106,'Napisz list',feather_page,caption='Napisz')
        W.tile(266,106,'Odebrane',open_mailbox,caption='Odebrane')
        S.foot_ornament(262,226)
    W.draw_footer()
    W.draw_lower('WRÓĆ DO MENU')
    W.clean_orphans()
    assert not W.find_orphans(W.im)
    assert {color for _, color in W.im.getcolors(352*444)} <= set(W.C.values())
    return W.im.copy()


def title(ident):
    raw = (ROOT / 'hub_glowne.yml').read_text(encoding='utf-8')
    template = re.search(r"^menu_title: '([^']+)'",raw,re.M).group(1)
    for old, code in zip(range(0x4168,0x416c),GLYPHS[ident]):
        assert template.count(chr(old)) == 1
        template = template.replace(chr(old),chr(code))
    return template


def item(material, placement, name, lore, commands=()):
    key = 'slots' if isinstance(placement,list) else 'slot'
    val = '[' + ', '.join(map(str,placement)) + ']' if isinstance(placement,list) else str(placement)
    lines = [f'    material: {material}',f'    {key}: {val}',
             '    item_flags: [HIDE_ADDITIONAL_TOOLTIP]',
             '    update: true',f"    display_name: '{name}'",'    lore:']
    lines += [f"      - '{line}'" for line in lore]
    if commands:
        lines += ['    left_click_commands:']
        lines += [f"      - '{command}'" for command in commands]
    return '\n'.join(lines)


def menu(ident):
    content = {}
    if ident == 'dziennik_przygod':
        content['kronika'] = item('nexo-mmo_icon_blank',EMBLEM,'&6&lDziennik przygód',[
            '&7Klasa: &f%mmocore_class%', '&7Poziom: &f%mmocore_level%',
            '&7EXP: &f%mmocore_experience% &7/ &f%mmocore_next_level%'])
        content['postep'] = item('WRITABLE_BOOK',38,'&ePostęp zadania',[
            '&7Zadanie: &f%mmocore_quest%', '&7Postęp: &f%mmocore_quest_progress%%',
            '&eKliknij, aby otworzyć zadania.'],['[player] quests'])
        content['zadanie'] = item('PAPER',23,'&eAktywne zadanie',[
            '&f%mmocore_quest%', '&7Postęp: &f%mmocore_quest_progress%%',
            '&eKliknij, aby otworzyć zadania.'],['[player] quests'])
        content['chowaniec'] = item('LEAD',24,'&aAktywny chowaniec',[
            '&7Imię: &f%chowance_aktywny_nazwa%',
            '&7Poziom: &f%chowance_aktywny_poziom%'],
            ['[openguimenu] chowance_menazeria'])
        content['bestiariusz'] = item('BOOK',25,'&bBestiariusz',[
            '&7Widziane: &f%chowance_bestiariusz_widziane%',
            '&7Złapane: &f%chowance_bestiariusz_zlapane%&7/&f%chowance_bestiariusz_wszystkie%',
            '&7Ukończono: &f%chowance_bestiariusz_procent%%'],
            ['[openguimenu] chowance_bestiariusz'])
        content['odznaki'] = item('GOLD_NUGGET',32,'&6Odznaki mistrzów',[
            '&7Zdobyte: &f%chowance_odznaki_liczba%'],['[openguimenu] chowance_lochy'])
        content['wieza'] = item('LADDER',33,'&6Rekord wieży',[
            '&7Piętro: &f%chowance_wieza_WIEZA_CHOWANCOW_rekord%'],
            ['[openguimenu] chowance_lochy'])
        content['wiara'] = item('LANTERN',34,'&eWiara i patron',[
            '&7Patron: &f%wiary_patron_imie%', '&7Wiara: &f%wiary_wiara%',
            '&7Łaska: &f%wiary_laska%', '&7Pozostało: &f%wiary_laska_czas%'],
            ['[openguimenu] wiara'])
    else:
        content['koperta'] = item('nexo-mmo_icon_blank',EMBLEM,'&e&lPoczta',[
            '&7Listy w skrzynce: &f%cmi_user_mail_count%'])
        content['stan'] = item('PAPER',38,'&eListy w skrzynce',[
            '&7Listy w skrzynce: &f%cmi_user_mail_count%',
            '&7Otwórz odebrane na czacie.'],
            ['[player] cmi mail read','[close]'])
        content['napisz'] = item('nexo-mmo_icon_blank',MAIL['napisz'],'&eNapisz list',[
            '&7Wpisz odbiorcę i treść w oknach.',
            '&eKliknij, aby napisać list.'],
            ['[console] cmi dialogs poczta_napisz %player_name%'])
        content['odebrane'] = item('nexo-mmo_icon_blank',MAIL['odebrane'],'&eOdebrane',[
            '&7Listy pojawią się na czacie.',
            '&7[X] przy liście go usuwa.'],
            ['[player] cmi mail read','[close]'])
    return (f"# Wygenerowane przez strony/poczta-dziennik/generuj.py.\n"
            f"menu_title: '{title(ident)}'\nsize: 54\nupdate_interval: 20\nitems:\n"
            + '\n'.join(f'  {key}:\n{value}' for key,value in content.items()) + '\n')


def main():
    (OUT / 'dm').mkdir(parents=True,exist_ok=True)
    glyph_lines = ['# Cztery ćwiartki na stronę; U+4B00–U+4B07.']
    previews = []
    for ident in GLYPHS:
        raw = menu(ident)
        (OUT / 'dm' / f'{ident}.yml').write_text(raw,encoding='utf-8')
        pic = draw(ident)
        dest = OUT / ident
        dest.mkdir(exist_ok=True)
        pic.save(dest / f'{ident}_2x.png')
        for n,(x,y) in enumerate(((0,0),(176,0),(0,222),(176,222)),1):
            pic.crop((x,y,x+176,y+222)).save(dest / f'{ident}_{n}.png')
        for n,code in enumerate(GLYPHS[ident],1):
            glyph_lines += [f'{ident}_{n}:','  font: minecraft:default',
                            f'  texture: nexo:gui/{ident}_{n}',
                            f'  ascent: {13 if n <= 2 else -98}',
                            '  height: 111',f'  char: "{chr(code)}"']
        previews.append(pic)
    (OUT / 'glify_poczta_dziennik.yml').write_text('\n'.join(glyph_lines)+'\n',encoding='utf-8')
    sheet = Image.new('RGB',(704,444),W.C['table0'])
    for i,pic in enumerate(previews):
        sheet.paste(pic,(i*352,0))
    sheet.save(OUT / 'arkusz_x2.png')
    (OUT / 'podstrony.txt').write_text(
        'U+4B00 hub_glowne  # dziennik_przygod\n'
        'U+4B04 hub_glowne  # poczta\n',encoding='utf-8')
    (OUT / 'hub_glowne_zmiany.txt').write_text(
        "  dziennik:\n"
        "    material: nexo-mmo_icon_blank\n"
        "    slots: [7, 8, 16, 17]\n"
        "    item_flags: [HIDE_ADDITIONAL_TOOLTIP]\n"
        "    display_name: '&6Dziennik przygód'\n"
        "    lore:\n"
        "      - '&7Sprawdź zadanie i chowańca.'\n"
        "      - '&7Zobacz postęp przygody.'\n"
        "    left_click_commands:\n"
        "      - '[openguimenu] dziennik_przygod'\n\n"
        "  poczta:\n"
        "    material: nexo-mmo_icon_blank\n"
        "    slots: [41, 42, 50, 51]\n"
        "    item_flags: [HIDE_ADDITIONAL_TOOLTIP]\n"
        "    display_name: '&ePoczta'\n"
        "    lore:\n"
        "      - '&7Napisz list do gracza.'\n"
        "      - '&7Odczytaj listy na czacie.'\n"
        "    left_click_commands:\n"
        "      - '[openguimenu] poczta'\n",encoding='utf-8')


if __name__ == '__main__':
    main()
