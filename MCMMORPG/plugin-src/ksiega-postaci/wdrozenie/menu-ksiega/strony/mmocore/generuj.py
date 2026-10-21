#!/usr/bin/env python3
"""Karty MMOCore dla Księgi: wyłącznie eksport, bez zmian na serwerze."""
from collections import Counter
from pathlib import Path
import re
import sys

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE.parent))
import ksiega_wspolne as W  # noqa: E402

SOURCE = Path('/opt/docker/mc/data/world1/plugins/MMOCore/gui')
BLANK_TEXTURE = Path('/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/mmo_equipment/fill.png')
OUT = HERE / 'out'
IDS = ('player-stats', 'attribute-view', 'skill-list', 'quest-list',
       'party-creation', 'party-view', 'friend-list', 'friend-removal',
       'guild-creation', 'guild-view')
TITLES = ('Statystyki', 'Atrybuty', 'Umiejętności', 'Zadania',
          'Tworzenie drużyny', 'Drużyna', 'Znajomi', 'Usuwanie znajomego',
          'Tworzenie gildii', 'Gildia')
PARENTS = ('hub_postac', 'hub_postac', 'hub_umiejetnosci', 'hub_glowne',
           'hub_spolecznosc', 'hub_spolecznosc', 'hub_spolecznosc',
           'hub_spolecznosc', 'hub_spolecznosc', 'hub_spolecznosc')
ITEM = re.compile(r'^  ([\w-]+):\s*$', re.M)
SLOT_LINE = re.compile(r'^(    slots: )\[[^\n]*\]', re.M)
PLACEHOLDER = re.compile(r'(?<!\{)\{!?[a-zA-Z_][\w-]*\}')
RIGHT = [9*r+c for r in range(1, 6) for c in range(5, 9)]


def emblem_slot(slot):
    return slot//9 < 3 and slot%9 < 4

POSITION = {
    'player-stats': {
        'powrot':[27], 'umiejetnosci':[29],
        **dict(zip(('profil','rasa','wiara','zasoby'),([45,46,47,48]))),
        **dict(zip(('naglowek_postac','naglowek_walka','naglowek_profesje',
                    'atak','obrona','magia','krytyk','pancernosc','mana',
                    'szybkosc_ataku','zycie','umiejetnosci_bojowe',
                    'obrazenia_fizyczne','regeneracja','odnowienie',
                    'profesje_surowce','profesje_teren','profesje_rzemioslo',
                    'profesje_wiedza','ekwipunek'), RIGHT))},
    'attribute-view': {'reallocate':[37], 'str':[23], 'dex':[24],
                       'int':[25], 'wiara':[26]},
    'skill-list': {'skill':[14,15,16,17,23,24,25,26,32,33,34,35],
                   'slot':[27,28,36,37,45,46],
                   'skill-level':[41,42,43,50,51,52],
                   'reallocate':[29], 'previous':[47], 'next':[48],
                   'upgrade':[53]},
    'quest-list': {'skill':[14,15,16,17,23,24,25,26,32,33,34,35,
                            41,42,43,50,51,52,45,46,47],
                   'previous':[27], 'next':[29]},
    'party-creation': {'create':[23], 'back':[25]},
    'party-view': {'member':[23,24,25,26,32,33,34,35],
                   'request':[27], 'leave':[29]},
    'friend-list': {'friend':[14,15,16,17,23,24,25,26,32,33,34,35,
                              41,42,43,50,51,52,45,46,47], 'request':[27]},
    'friend-removal': {'yes':[23], 'back':[25]},
    'guild-creation': {'create':[23], 'back':[25]},
    'guild-view': {'member':[14,15,16,17,23,24,25,26,32,33,34,35,
                             41,42,43,50,51,52,45,46,47],
                   'request':[27], 'previous':[29], 'next':[30],
                   'leave':[38], 'disband':[39]},
}

TRANSLATE = {
    'skill-list': {
        'Selected Skill:': 'Wybrana umiejętność:', 'Requires Level': 'Wymaga poziomu',
        'Maximum Level Hit!': 'Osiągnięto maksymalny poziom!',
        "'&aNext'": "'&aNastępna strona'", "'&aPrevious'": "'&aPoprzednia strona'",
        'Reallocate Skill Points': 'Rozdziel punkty umiejętności od nowa',
        'You have spent a total of &6{total}&7 skill points.': 'Wydano łącznie &6{total}&7 punktów umiejętności.',
        'Right click to reallocate them.': 'Prawy klik: rozdziel punkty od nowa.',
        'Costs 1 skill reallocation point.': 'Koszt: 1 punkt zmiany umiejętności.',
        'Skill Reallocation Points:': 'Punkty zmiany umiejętności:',
        'Skill Slot': 'Miejsce umiejętności', "'&cNone'": "'&cBrak'",
        'Current Skill:': 'Przypisana umiejętność:',
        'Cast this spell by pressing [F] followed': 'Użyj: naciśnij [F], a potem',
        'by the keybind displayed on the action bar.': 'klawisz pokazany na pasku akcji.',
        'Left click to bind': 'Lewy klik: przypisz', 'Right click to unbind.': 'Prawy klik: usuń przypisanie.',
        'Shift left click to select.': 'Shift + lewy klik: wybierz.',
        ' Level {roman}': ' Poziom {roman}', 'UPGRADE {skill_caps}': 'ULEPSZ {skill_caps}',
        'Costs 1 skill point.': 'Koszt: 1 punkt umiejętności.',
        'Current Skill Points:': 'Dostępne punkty umiejętności:'},
    'quest-list': {
        "date-format: 'MMM d yyyy'": "date-format: 'dd.MM.yyyy'",
        'Unavailable': 'Niedostępne', 'Requires Level': 'Wymaga poziomu',
        'Requires {profession} Level': 'Wymaga {profession} na poziomie',
        'Quest Started!': 'Zadanie rozpoczęte!', 'Progression:': 'Postęp:',
        'Level Requirements': 'Wymagany poziom',
        "You''ve completed this quest on the {date}.": 'Ukończono zadanie dnia {date}.',
        "You can''t do this quest twice.": 'Tego zadania nie można powtórzyć.',
        'You can start the quest in {delay}.': 'Zadanie dostępne za {delay}.',
        'You can start this quest.': 'Możesz rozpocząć to zadanie.',
        'Right click to cancel.': 'Prawy klik: anuluj.',
        'Left click to start.': 'Lewy klik: rozpocznij.',
        "'&aNext'": "'&aNastępna strona'", "'&aPrevious'": "'&aPoprzednia strona'"},
    'party-creation': {'Create a party!':'Utwórz drużynę!', "'&aBack'":"'&aAnuluj'"},
    'party-view': {'No Player':'Brak gracza', 'Level {level}':'Poziom {level}',
                   'Online Since:':'Online od:', 'Leave Party':'Opuść drużynę',
                   'Invite a player..':'Zaproś gracza...'},
    'friend-list': {'No Friend':'Brak znajomych', '[Online]':'[Dostępny]',
                    '[Offline]':'[Niedostępny]', 'Level {level}':'Poziom {level}',
                    'Online Since:':'Online od:', 'Right click to remove.':'Prawy klik: usuń.',
                    'Last Seen: {last_seen} ago':'Ostatnio widziany: {last_seen} temu',
                    'New Friend Request':'Zaproś znajomego'},
    'friend-removal': {'Remove {name}':'Usuń {name}', 'Keep {name}':'Zachowaj {name}'},
    'guild-creation': {'Create a guild!':'Utwórz gildię!', "'&aBack'":"'&aAnuluj'"},
    'guild-view': {'No Player':'Brak gracza', 'Level {level}':'Poziom {level}',
                   'Online Since:':'Online od:', '[&aLeader&c]':'[&aLider&c]',
                   'Next Page':'Następna strona', 'Previous Page':'Poprzednia strona',
                   'Leave Guild':'Opuść gildię', 'Disband Guild':'Rozwiąż gildię',
                   'Invite a player..':'Zaproś gracza...'},
}


def blocks(raw):
    head, body = raw.split('items:\n', 1)
    matches = list(ITEM.finditer(body))
    assert matches and not body[:matches[0].start()].strip()
    return head, {m.group(1):body[m.start():matches[i+1].start() if i+1<len(matches) else len(body)]
                  for i,m in enumerate(matches)}


def slots(block):
    m = SLOT_LINE.search(block)
    assert m, block[:80]
    return [int(n) for n in re.findall(r'\d+', m.group(0))]


def title(glyphs):
    template = (ROOT/'strony/out/dm/hub_postac.yml').read_text(encoding='utf-8')
    template = re.search(r"^menu_title: '([^']+)'",template,re.M).group(1)
    for a,b in zip(range(0x4A00,0x4A04),glyphs):
        template = template.replace(chr(a),chr(b))
    return template


def create_yaml(ident, raw, codes):
    head, items = blocks(raw)
    assert set(items) == set(POSITION[ident]), (ident,set(items)^set(POSITION[ident]))
    assert not re.search(r'^    slots: (?!\[)', raw, re.M)
    old_title = re.search(r'^name:\s*(.*)$', head,re.M).group(1).strip().strip("'\"")
    new_title = title(codes)
    head = re.sub(r'^name:.*$',lambda m:"name: '"+new_title.replace("'","''")+"'",head,count=1,flags=re.M)
    head = re.sub(r'^slots:\s*\d+', 'slots: 54',head,count=1,flags=re.M)
    out = {}
    for name, block in items.items():
        place = POSITION[ident][name]
        if isinstance(place,int): place=[place]
        block = SLOT_LINE.sub(lambda m:m.group(1)+'[ '+', '.join(map(str,place))+' ]',block,count=1)
        for a,b in TRANSLATE.get(ident,{}).items():
            block = block.replace(a,b)
        if ident == 'skill-list' and name == 'upgrade':
            # UpgradeItem rozwija {skill} wybranej umiejętności (MMOCore jar).
            block = block.replace("    lore:\n", "    lore:\n      - '&7Wybrana umiejętność: &6{skill}'\n", 1)
        if name in {tile[0] for tile in TILES[ident]} or any(map(emblem_slot,place)):
            block = blank_icon(block)
        out[name] = block.rstrip()+'\n'
    generated = head+'items:\n'+''.join(out.values())
    return generated, old_title


def emblem(kind,x,y):
    """Jedna duża sylwetka na emblemat; ramkę 4×3 rysuje hero_frame."""
    W.R((x,y,x+51,y+38),'blue0')
    if kind == 0:
        W.G([(x+7,y+5),(x+43,y+5),(x+40,y+35),(x+10,y+35)],'paper2','ink')
        W.G([(x+10,y+8),(x+40,y+8),(x+38,y+32),(x+12,y+32)],'paper4')
        for yy in (13,19,25): W.L([(x+15,y+yy),(x+22,y+yy)],'gold0')
        W.L([(x+25,y+27),(x+30,y+22),(x+34,y+24),(x+39,y+12)],'red1',3)
    elif kind == 1:
        for xx,yy,col in ((12,19,'red1'),(26,12,'blue2'),(40,19,'green1')):
            W.G([(x+xx,y+yy-10),(x+xx+8,y+yy),(x+xx,y+yy+11),(x+xx-8,y+yy)],'gold1','ink')
            W.G([(x+xx,y+yy-6),(x+xx+4,y+yy),(x+xx,y+yy+7),(x+xx-4,y+yy)],col,'paper4')
    elif kind == 2:
        # Miecz skrzyżowany z różdżką, rozpoznawalne już w małym podglądzie.
        W.L([(x+9,y+34),(x+40,y+3)],'ink',8)
        W.L([(x+9,y+34),(x+40,y+3)],'stone2',5)
        W.G([(x+36,y+5),(x+45,y+1),(x+42,y+10)],'paper4','ink')
        W.L([(x+7,y+28),(x+17,y+38)],'gold2',4)
        W.L([(x+10,y+4),(x+43,y+34)],'ink',7)
        W.L([(x+10,y+4),(x+43,y+34)],'wood2',4)
        W.star(x+10,y+4,'gold2')
        W.L([(x+35,y+32),(x+45,y+22)],'gold2',3)
    elif kind == 3:
        W.G([(x+3,y+9),(x+19,y+5),(x+26,y+9),(x+34,y+5),(x+49,y+9),
             (x+46,y+34),(x+28,y+31),(x+26,y+35),(x+24,y+31),(x+6,y+34)],'gold1','ink')
        W.G([(x+6,y+12),(x+19,y+8),(x+24,y+12),(x+24,y+29),(x+8,y+31)],'paper4')
        W.G([(x+28,y+12),(x+34,y+8),(x+46,y+12),(x+44,y+31),(x+28,y+29)],'paper3')
        W.L([(x+26,y+10),(x+26,y+34)],'ink',2)
        W.L([(x+34,y+21),(x+37,y+24),(x+43,y+16)],'red1',3)
    elif kind in (4,5):
        for xx,yy,coat in ((5,15,'red1'),(20,6,'gold1'),(35,15,'blue1')):
            W.E((x+xx+2,y+yy,x+xx+10,y+yy+9),'paper2','ink')
            W.G([(x+xx,y+yy+10),(x+xx+12,y+yy+10),
                 (x+xx+15,y+yy+23),(x+xx-2,y+yy+23)],coat,'ink')
        if kind == 4: W.star(x+43,y+7,'paper4')
    elif kind in (6,7):
        W.G([(x+1,y+23),(x+14,y+15),(x+25,y+23),(x+20,y+36),(x+3,y+36)],'red1','ink')
        W.G([(x+50,y+15),(x+39,y+11),(x+27,y+20),(x+32,y+34),(x+50,y+30)],'blue1','ink')
        W.G([(x+14,y+17),(x+21,y+14),(x+28,y+19),(x+36,y+26),
             (x+32,y+30),(x+24,y+27),(x+18,y+31),(x+11,y+25)],'paper4','ink')
        W.G([(x+39,y+13),(x+32,y+13),(x+25,y+20),(x+20,y+25),
             (x+29,y+28),(x+36,y+31),(x+43,y+23)],'paper2','ink')
        if kind == 7:
            W.L([(x+19,y+5),(x+34,y+35)],'red1',4)
            W.L([(x+34,y+5),(x+19,y+35)],'red1',4)
    else:
        W.R((x+7,y+3,x+11,y+37),'wood2','ink')
        W.G([(x+10,y+5),(x+44,y+5),(x+42,y+29),(x+27,y+36),
             (x+11,y+29)],'gold1','ink')
        W.G([(x+14,y+9),(x+40,y+9),(x+38,y+26),(x+27,y+31),
             (x+15,y+26)],'red0','gold2')
        W.G([(x+27,y+11),(x+31,y+19),(x+37,y+20),(x+31,y+25),
             (x+27,y+28),(x+23,y+25),(x+17,y+20),(x+23,y+19)],'paper4')
        if kind == 8: W.star(x+43,y+5,'paper4')


def socket(slot):
    c,r=slot%9,slot//9
    x,y=14+36*c,34+36*r
    W.R((x+3,y+3,x+32,y+32),'leather0','gold0')
    W.R((x+5,y+5,x+30,y+30),'blue0','gold0')
    W.L([(x+7,y+7),(x+28,y+7)],'blue1')
    W.L([(x+7,y+28),(x+28,y+28)],'gold0')


def panel(label,x1,x2,y1,y2,where):
    W.R((x1,y1,x2,y2),'paper1','gold0')
    W.R((x1+3,y1+3,x2-3,y2-3),'paper3','paper0')
    for s in where: socket(s)
    if label:
        W.R((x1,y1-29,x2,y1-1),'gold0','leather0')
        W.R((x1+2,y1-27,x2-2,y1-3),'blue0','gold1')
        W.gui_text(label,(x1+x2)//2,y1-23,'gold2',max_gui=(x2-x1-8)//2,frame=(x1,y1-29,x2,y1-1))


TILES = {
    'player-stats': [('powrot',14,142,'Menu'),('umiejetnosci',86,142,'Zdolności')],
    'attribute-view': [('reallocate',50,178,'Reset')],
    'skill-list': [('reallocate',86,142,'Reset')],
    'quest-list': [('previous',14,142,'Wstecz'),('next',86,142,'Dalej')],
    'party-creation': [('create',194,106,'Utwórz'),('back',266,106,'Anuluj')],
    'party-view': [('request',14,142,'Zaproś'),('leave',86,142,'Opuść')],
    'friend-list': [('request',14,142,'Zaproś')],
    'friend-removal': [('yes',194,106,'Usuń'),('back',266,106,'Anuluj')],
    'guild-creation': [('create',194,106,'Utwórz'),('back',266,106,'Anuluj')],
    'guild-view': [('request',14,142,'Zaproś')],
}

BLANK_MODEL = "nexo:mmo_icon_blank"


def blank_icon(block):
    """Zostaw logikę itemu, ukryj model na kaflu lub emblemacie."""
    block = re.sub(r'^    (?:item_model|hide-flags|texture):.*\n', '', block, flags=re.M)
    block, count = re.subn(r'^    item: [^\n]+$',
                           "    item: PAPER\n    item_model: '" + BLANK_MODEL + "'\n    hide-flags: true",
                           block, count=1, flags=re.M)
    assert count == 1, block[:80]
    return block

DESCRIPTIONS = {
    'attribute-view': ('Rozdziel punkty', 'między atrybuty.', 'Reset zmienia wybór.'),
    'party-creation': ('Zbierz wędrowców', 'i ruszcie razem', 'na wyprawę.'),
    'party-view': ('Zaproś graczy', 'lub opuść swoją', 'drużynę.'),
    'friend-removal': ('Potwierdź wybór', 'albo zachowaj', 'znajomego.'),
    'guild-creation': ('Załóż gildię', 'i zaproś do niej', 'innych graczy.'),
}

RIGHT_HEADINGS = {
    'player-stats': 'Statystyki',
    'attribute-view': 'Atrybuty postaci',
    'skill-list': 'Umiejętności',
    'quest-list': 'Dziennik zadań',
    'party-creation': 'Załóż drużynę',
    'party-view': 'Członkowie',
    'friend-list': 'Lista znajomych',
    'friend-removal': 'Decyzja',
    'guild-creation': 'Załóż gildię',
    'guild-view': 'Członkowie',
}


def action_icon(name,x,y):
    W.R((x,y,x+51,y+38),'blue0')
    if name in ('create','request'):
        W.E((x+8,y+5,x+29,y+26),'paper2','ink')
        W.G([(x+5,y+36),(x+8,y+25),(x+29,y+25),(x+32,y+36)],'blue1','ink')
        W.L([(x+39,y+10),(x+39,y+31)],'gold2',4)
        W.L([(x+30,y+20),(x+48,y+20)],'gold2',4)
    elif name in ('back','leave','disband','yes'):
        W.G([(x+26,y+3),(x+45,y+11),(x+42,y+29),(x+26,y+37),
             (x+10,y+29),(x+7,y+11)],'red0','gold1')
        if name == 'yes':
            W.L([(x+16,y+20),(x+24,y+28),(x+37,y+12)],'paper4',4)
        else:
            W.L([(x+17,y+13),(x+35,y+29)],'paper4',4)
            W.L([(x+35,y+13),(x+17,y+29)],'paper4',4)
    elif name in ('previous','next','powrot'):
        direction = -1 if name in ('previous','powrot') else 1
        if direction < 0:
            pts=[(x+7,y+19),(x+27,y+5),(x+27,y+13),(x+45,y+13),
                 (x+45,y+26),(x+27,y+26),(x+27,y+34)]
        else:
            pts=[(x+45,y+19),(x+25,y+5),(x+25,y+13),(x+7,y+13),
                 (x+7,y+26),(x+25,y+26),(x+25,y+34)]
        W.G(pts,'gold2','ink')
    elif name == 'reallocate':
        W.E((x+10,y+4,x+42,y+36),'gold1','ink')
        W.E((x+16,y+10,x+36,y+30),'blue1','gold2')
        W.L([(x+9,y+15),(x+16,y+8),(x+23,y+14)],'paper4',3)
        W.L([(x+43,y+25),(x+36,y+32),(x+29,y+26)],'paper4',3)
    else:
        W.L([(x+9,y+31),(x+39,y+5)],'stone2',5)
        W.L([(x+35,y+28),(x+42,y+34)],'gold2',4)
        W.star(x+16,y+9,'paper4')


def description(lines,x=194,y=174,w=136,h=75):
    W.R((x,y,x+w-1,y+h-1),'paper1','gold0')
    W.R((x+3,y+3,x+w-4,y+h-4),'paper4','paper0')
    W.gui_body(lines,x+w//2,y+6,w-12,h-20,frame=(x,y,x+w-1,y+h-1))
    W.L([(x+w//2-23,y+h-13),(x+w//2-7,y+h-13)],'gold0')
    W.L([(x+w//2+7,y+h-13),(x+w//2+23,y+h-13)],'gold0')
    W.G([(x+w//2,y+h-17),(x+w//2+4,y+h-13),
         (x+w//2,y+h-9),(x+w//2-4,y+h-13)],'gold1','gold0')


def draw(ident,index):
    im=W.new_canvas()
    W.draw_book()
    W.draw_title(TITLES[index])
    W.hero_frame(14,34,lambda x,y:emblem(index,x,y))
    tiled={name for name,_,_,_ in TILES[ident]}
    all_slots={s for name,ss in POSITION[ident].items() if name not in tiled
               for s in (ss if isinstance(ss,list) else [ss])}
    left=sorted(s for s in all_slots if s%9<4)
    right=sorted(s for s in all_slots if s%9>4)
    if right:
        first=min(s//9 for s in right)
        last=max(s//9 for s in right)
        panel(RIGHT_HEADINGS[ident],194,330,34+36*first+4,34+36*last+35,right)
    elif ident in ('party-creation','friend-removal','guild-creation'):
        panel(RIGHT_HEADINGS[ident],194,330,106,177,[])
    if left:
        first=min(s//9 for s in left)
        last=max(s//9 for s in left)
        panel('',22,158,34+36*first+4,34+36*last+35,left)
    for name,x,y,label in TILES[ident]:
        W.tile(x,y,label,lambda px,py,n=name:action_icon(n,px,py))
    if ident in DESCRIPTIONS:
        if ident in ('party-creation','friend-removal','guild-creation'):
            description(DESCRIPTIONS[ident],22,143,136,106)
        elif ident == 'attribute-view':
            description(DESCRIPTIONS[ident],194,179,136,70)
        elif ident == 'party-view':
            description(DESCRIPTIONS[ident],194,179,136,70)
    W.draw_footer()
    W.draw_lower('WRÓĆ DO MENU')
    W.clean_orphans()
    assert not W.find_orphans(im)
    assert {c for _,c in im.getcolors(im.width*im.height)}<=set(W.C.values())
    return im


def verify(ident,source,result,codes):
    _,old=blocks(source)
    _,new=blocks(result)
    removed=set(old)-set(new)
    assert not removed, (ident,removed)
    assert set(old)==set(new)==set(POSITION[ident])
    old_slots={k:slots(v) for k,v in old.items()}
    new_slots={k:slots(v) for k,v in new.items()}
    for k in old:
        assert len(old_slots[k])==len(new_slots[k]),(ident,k)
        for line in re.findall(r'^\s+(?:function|script|offset|upgrade_on_click|disable_click):.*$',old[k],re.M):
            assert line in new[k],(ident,k,line)
        added = Counter({'{skill}': 1}) if (ident,k)==('skill-list','upgrade') else Counter()
        assert Counter(PLACEHOLDER.findall(old[k]))+added==Counter(PLACEHOLDER.findall(new[k])),(ident,k,'placeholder')
    occupied=[s for ss in new_slots.values() for s in ss]
    assert all(0<=s<54 and s%9!=4 for s in occupied),(ident,'spine')
    assert len(occupied)==len(set(occupied)),(ident,'duplicate')
    assert re.search(r'^slots: 54$',result,re.M)
    new_title=re.search(r'^name:\s*(.*)$',result,re.M).group(1)
    assert new_title == "'"+title(codes)+"'", (ident,'text in title')
    assert not PLACEHOLDER.search(new_title), (ident,'placeholder in title')
    # Polecenia z oryginału pozostają dosłownie obecne; nie ma kasowanych przycisków.
    for line in re.findall(r'^\s+-\s+[\'\"]?command\{.*$',source,re.M):
        assert line in result,(ident,line)
    occupied=set(occupied)
    covered={tile[0] for tile in TILES[ident]}
    covered.update(name for name,ss in new_slots.items() if any(map(emblem_slot,ss)))
    for name in covered:
        block=new[name]
        assert re.search(r'^    item: PAPER$',block,re.M),(ident,name,'visible material')
        assert re.search(r"^    item_model: 'nexo:mmo_icon_blank'$",block,re.M),(ident,name,'visible model')
        assert re.search(r'^    hide-flags: true$',block,re.M),(ident,name,'flags')
        assert not re.search(r'^    texture:',block,re.M),(ident,name,'skull texture')
    for name,x,y,_ in TILES[ident]:
        anchor=new_slots[name][0]
        sx,sy=14+36*(anchor%9),34+36*(anchor//9)
        assert x<=sx<x+72 and y<=sy<y+72,(ident,name,'tile anchor')
        for other in occupied-{anchor}:
            sx,sy=14+36*(other%9),34+36*(other//9)
            assert not (x<=sx<x+72 and y<=sy<y+72),(ident,name,other)
    return len(old),len(occupied),len(covered)


def main():
    with Image.open(BLANK_TEXTURE) as blank_texture:
        assert blank_texture.convert('RGBA').getchannel('A').getextrema() == (0,0), 'Nexo blank icon is visible'
    OUT.mkdir(parents=True,exist_ok=True)
    (OUT/'gui').mkdir(exist_ok=True)
    glyph_lines=['# MMOCore: cztery ćwiartki na okno, U+4A80–U+4AA7.']
    return_lines=[]
    checks=[]
    images=[]
    for i,ident in enumerate(IDS):
        source=(SOURCE/f'{ident}.yml').read_text(encoding='utf-8')
        codes=[0x4A80+i*4+n for n in range(4)]
        result,old_title=create_yaml(ident,source,codes)
        count,occupied,covered=verify(ident,source,result,codes)
        (OUT/'gui'/f'{ident}.yml').write_text(result,encoding='utf-8')
        im=draw(ident,i)
        folder=OUT/ident
        folder.mkdir(exist_ok=True)
        im.save(folder/f'{ident}_2x.png')
        for n,(x,y) in enumerate(((0,0),(176,0),(0,222),(176,222)),1):
            im.crop((x,y,x+176,y+222)).save(folder/f'{ident}_{n}.png')
        for n,c in enumerate(codes,1):
            glyph_lines.extend((f'{ident.replace("-","_")}_{n}:',
                                '  font: minecraft:default',
                                f'  texture: nexo:gui/{ident}_{n}',
                                f'  ascent: {13 if n<=2 else -98}',
                                '  height: 111',f'  char: "{chr(c)}"'))
        return_lines.append(f'U+{codes[0]:04X} {PARENTS[i]}  # {ident}')
        checks.append(f'{ident}: {count} itemów, {occupied} pól, sloty 0–53, grzbiet wolny, duplikaty 0, kafle bez kolizji; żaden item na kaflu/emblemacie nie ma widocznego materiału ({covered}); tytuł bez tekstu, glify U+{codes[0]:04X}–U+{codes[-1]:04X}; stary tytuł: {old_title}')
        images.append(im)
    (OUT/'glify_mmocore.yml').write_text('\n'.join(glyph_lines)+'\n',encoding='utf-8')
    (OUT/'podstrony.txt').write_text('\n'.join(return_lines)+'\n',encoding='utf-8')
    (OUT/'kontrola.txt').write_text('\n'.join(checks)+'\n',encoding='utf-8')
    sheet=Image.new('RGB',(352*5,444*2),W.C['table0'])
    for i,im in enumerate(images):
        sheet.paste(im,((i%5)*352,(i//5)*444))
    sheet.save(OUT/'arkusz_x2.png')
    # Arkusz szczegółów: każda miniatura i każdy pełny emblemat w skali x4.
    details=[]
    for ident,im in zip(IDS,images):
        details.append(im.crop((14,34,158,142)).resize((576,432),Image.Resampling.NEAREST))
        for _,x,y,_ in TILES[ident]:
            thumb=im.crop((x,y,x+72,y+72)).resize((288,288),Image.Resampling.NEAREST)
            cell=Image.new('RGB',(576,432),W.C['table0'])
            cell.paste(thumb,(144,72))
            details.append(cell)
    contact=Image.new('RGB',(576*4,432*((len(details)+3)//4)),W.C['table0'])
    for i,detail in enumerate(details):
        contact.paste(detail,((i%4)*576,(i//4)*432))
    contact.save(OUT/'kafle_x4.png')
    print('\n'.join(checks))


if __name__=='__main__':
    main()
