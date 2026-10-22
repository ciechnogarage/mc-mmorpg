#!/usr/bin/env python3
"""Generator konfiguracji HANDLU world1 (CoreTools): dom aukcyjny 'rynek', skup 'skup', sklep 'sklep'.

Ceny skupu biora sie z tools/handel/ceny.json (model: tools/handel/ekonomia.py) — NIE wpisywac
ich recznie w YAML, tylko poprawic model i puscic:
    python3 ekonomia.py && python3 generuj.py
Wynik laduje od razu w plugins/CoreTools/{AuctionHouse,Shops,Scripts}. Potem: /ct reload.

Stringi YAML sa pisane jako JSON (json.dumps) — JSON-owy string to poprawny YAML-owy
podwojnie cytowany skalar, wiec polskie znaki, klamry {placeholderow} i znaki prywatnego
obszaru (glif tla, przesuniecia) przechodza bez kombinowania z cudzyslowami.
"""
import json, os

P = os.environ.get('HANDEL_PLUGINS', '/opt/docker/mc/data/world1/plugins') + '/CoreTools'   # klon: HANDEL_PLUGINS=<plugins klonu>
H = os.path.dirname(os.path.abspath(__file__))
CENY = json.load(open(os.path.join(H, 'ceny.json')))
REC = json.load(open(os.path.join(H, 'receptury.json')))
ITEMS = REC['items']
# Profesje v2 (MCMMORPG/profesje/generuj.py --handel): ceny wszystkich przedmiotow profesji liczone tym samym
# modelem (0,2 $/s wysilku, skup 40%) + lista surowcow. Pliki generowane — tu tylko dolaczenie.
_CP = os.path.join(H, 'ceny_profesje.json')
if os.path.exists(_CP):
    _cp = json.load(open(_CP))
    CENY.update(_cp)
    for _i, _c in _cp.items():
        ITEMS.setdefault(_i, {'type': _c['typ']})

# Tytul: bialy PRZED glifem (inaczej tekstura x0.25 — memory glif-panelu-tint-tytulu),
# -8 px, glif 6-rzedowego panelu handlu, -169 px, tekst.
T = '<white>䆐<white>'
q = lambda s: json.dumps(s, ensure_ascii=False)
M = lambda nazwa: f'nexo:hub_{nazwa}'          # model ikony z huba/handlu

SUROWCE = ['PROF_OAK_TIMBER', 'PROF_SPRUCE_RESIN', 'PROF_BIRCH_LATH', 'PROF_MEDICINAL_HERB', 'PROF_MOONLEAF',
           'PROF_REDCAP', 'PROF_FRESH_HARVEST', 'PROF_ROUGH_CRYSTAL', 'PROF_ROUGH_STONE', 'PROF_CLAY_LUMP',
           'PROF_WILD_SPICE', 'PROF_RAW_CATCH', 'PROF_CLAM', 'PROF_RARE_FISH', 'PROF_WATER_ESSENCE', 'PROF_PEARL',
           'PROF_ORE_CHUNK', 'PROF_SILVER_ORE', 'PROF_RAW_GEM', 'PROF_ARCANE_CRYSTAL', 'PROF_BEAST_SCRAP',
           'PROF_BEAST_BONE', 'PROF_RAW_FLEECE', 'PROF_VENOM_SAC', 'PROF_MONSTER_TROPHY']
_SP = os.path.join(H, 'surowce_profesje.json')
if os.path.exists(_SP):
    SUROWCE = sorted(set(SUROWCE) | set(json.load(open(_SP))))
MIKSTURY = ['PROF_ANTIDOTE', 'PROF_COMBAT_TONIC', 'PROF_WEAPON_OIL']
POLPRODUKTY = sorted(i for i, c in CENY.items() if c['typ'] == 'MATERIAL' and i not in SUROWCE and i not in MIKSTURY)
NARZEDZIA = sorted(i for i, c in CENY.items() if c['typ'] == 'TOOL' or (c['typ'] == 'MISCELLANEOUS' and i.startswith('PROF_')))  # + obozowiska, przeprawy
SPRZET_PROF = sorted(i for i, c in CENY.items() if c['typ'] not in ('MATERIAL', 'TOOL', 'CONSUMABLE'))
POTRAWY = sorted(i for i, c in CENY.items() if c['typ'] == 'CONSUMABLE')
# Sprzet na aukcji: wszystkie typy uzbrojenia/zbroi/akcesoriow MMOItems (takze wyroby kuzni),
# NIE: material/consumable/miscellaneous/tool/block — te ida innymi kategoriami albo wcale.
TYPY_SPRZETU = ['armor', 'axe', 'belt', 'bow', 'catalyst', 'charm', 'crossbow', 'dagger', 'gauntlet',
                'gloves', 'greataxe', 'greathammer', 'greatstaff', 'greatsword', 'halberd', 'hammer', 'katana',
                'lance', 'long_sword', 'musket', 'necklace', 'off_catalyst', 'ornament', 'ring',
                'shield', 'spear', 'staff', 'sword', 'talisman', 'thrusting_sword', 'tome', 'wand',
                'whip', 'gem_stone']
for grupa in (SUROWCE, MIKSTURY):
    for i in grupa:
        assert i in CENY, i


def btn(name, slots, *, nazwa=' ', model=None, lore=(), location='top', material='paper', extra=None,
        scripts=None, hide=False, conditions=None):
    if material == 'air':   # CoreTools robi setDisplayName na meta AIR (null) -> NPE; tylko goly slot
        return '\n'.join([f'    {name}:', f'      location: {location}', f'      slots: {json.dumps(slots)}',
                          '      source: vanilla', '      material: air'])
    o = [f'    {name}:', f'      location: {location}', f'      slots: {json.dumps(slots)}',
         '      source: vanilla', f'      material: {material}', f'      name: {q(nazwa)}',
         '      flags: [HIDE_ATTRIBUTES, HIDE_ADDITIONAL_TOOLTIP]']
    if model:
        o.append(f'      item_model: {model}')
    if hide:
        o.append('      hide_tooltip: true')
    o.append('      lore:' + (' []' if not lore else ''))
    o += [f'        - {q(l)}' for l in lore]
    if extra:
        o += ['      ' + e for e in extra]
    if scripts:
        o += ['      click:', '        Scripts:'] + [f'          - {s}' for s in scripts]
    if conditions:
        o += ['      Conditions:'] + [f'        - {q(c)}' for c in conditions]
    return '\n'.join(o)


# W oknach DOMU AUKCYJNEGO CoreTools NIE odpala skryptow 'click' zwyklych przyciskow (tylko przyciski
# "twarde": sell, home, next...). Sprawdzone botem 2026-09-23 (sonda5). Za to warunki przycisku sa
# liczone SYNCHRONICZNIE przed obsluga klikniecia, a 'falseElse <skrypt>' odpala skrypt, gdy warunek
# jest falszywy. Warunek zawsze falszywy = przycisk, ktory po prostu wykonuje skrypt.
def akcja_ah(skrypt):
    return [f'Comparator{{v1="0";v2="1";op="=="}} falseElse {skrypt}']


def pusty(name, slots):
    return btn(name, slots, model=M('handel_pusty'), hide=True)


def fmt(x):
    return f'{x:.2f}'.rstrip('0').rstrip('.') if x < 10 else (f'{x:.1f}'.rstrip('0').rstrip('.') if x < 100 else f'{x:.0f}')


# =============================================================================== DOM AUKCYJNY
KAT = [  # id, nazwa zakladki, model, slot, opis
    ('all', 'Wszystko', M('handel_aukcje'), 0, 'Wszystkie oferty graczy.'),
    ('surowce', 'Surowce', 'nexo:prof_oak_timber', 1, 'Drewno, zioła, ruda, połów, łupy z bestii.'),
    ('polprodukty', 'Półprodukty', 'nexo:prof_metal_plate', 2, 'Przetworzone materiały z warsztatów.'),
    ('narzedzia', 'Narzędzia', 'nexo:prof_lumber_axe', 3, 'Narzędzia profesji.'),
    ('sprzet', 'Sprzęt', 'nexo:prof_leather_gloves', 4, 'Broń, zbroje, akcesoria, klejnoty.'),
    ('potrawy', 'Potrawy i mikstury', 'nexo:prof_harvest_feast', 5, 'Jedzenie, antidota, toniki, oleje.'),
]
AH_ID = 'mmoitems_example'   # patrz komentarz w naglowku rynek.yml
LISTING_FEE = 'clamp({unit_price} * {amount} * (0.01 + 0.005 * {day}), 0.1, 1000000)'
SELL_TAX = '{unit_price} * 0.06'


def kategoria_ah(cid, allow_id=(), allow_type=()):
    ids = [i.lower() for i in allow_id]
    return '\n'.join([
        f'    {cid}:',
        '      minimum_bid_increment: -1',
        f'      name: {q(T + "Aukcje · " + dict((k[0], k[1]) for k in KAT)[cid] + " [{page_no}]")}',
        '      restriction_advanced:',
        '        allow:',
        '          source: [mmoitems]',
        f'          id: {json.dumps(ids)}',
        f'          type: {json.dumps(list(allow_type))}',
        '          tier: []', '          material: []', '          vanilla_material: []',
        '        block:',
        '          id: []', '          type: []', '          tier: []', '          material: []', '          vanilla_material: []',
    ])


def rynek():
    L = []
    a = L.append
    a('# DOM AUKCYJNY world1 — WYGENEROWANE przez tools/handel/generuj.py. Nie poprawiac recznie.')
    a('# Otwiera go konsola z menu Handel (DeluxeMenus hub_ekonomia):  ah mmoitems_example stolica <gracz>')
    a('# Komenda /ah ma w commands.yml permission OP — gracz nie otwiera okna z czatu (i nie otworzy go')
    a('# INNEMU graczowi, bo AuctionHouseCommand bierze nick z 3. argumentu bez sprawdzenia nadawcy).')
    a('#')
    a('# DECYZJE BEZPIECZENSTWA (dowody: bajtkod CoreTools 1.4.2, patrz raport w tools/handel/README.md):')
    a('#  disable_partial_purchase: true — PurchaseItem sprawdza ilosc tylko dla statusu "" (a oferta ma')
    a('#    status "active"), wiec przy zakupach czesciowych dwoch kupcow moglo kupic wiecej niz bylo.')
    a('#  default_sales_mode: fixed, brak przyciskow licytacji — odbior wygranej licytacji nie ma locka.')
    a('#  delivery.purchase_mode: 1, playervault: [] — pelny plecak = zakup odrzucony i zwrot pieniedzy')
    a('#    (skarbcow graczy nie ma; tryb 3 szukalby nieistniejacego skarbca).')
    a('#  kategorie = BIALA LISTA: przedmiot spoza list nie wejdzie w slot wystawiania (vanilla, ikony')
    a('#    GUI Nexo, przedmioty Chowancow, esencje, questowe) — AuctionUtils.restriction().')
    a('#')
    a('# ID "mmoitems_example" JEST CELOWE (nazwa techniczna, gracz jej nie widzi). CoreTools 1.4.2 zaklada')
    a('# tabele SQL, laduje oferty do pamieci i uruchamia harmonogram wygasania TYLKO przy starcie pluginu')
    a('# (AuctionHouse.enableAuctionHouse) — dla ID dopisanego po starcie /ct reload wczytuje sam config i')
    a('# pierwsze otwarcie konczy sie NPE. Ten dom aukcyjny wszedl bez restartu, wiec dostal ID, ktore')
    a('# istnialo przy starcie. Zmiana ID po restarcie jest mozliwa, ale PORZUCA aktywne oferty w starych')
    a('# tabelach — nie zmieniac, gdy sa oferty. Pliki przykladow producenta sa w backups (katalog')
    a('# AuctionHouse/ istnieje, wiec CoreTools ich nie odtworzy — FileFolder.saveDefaultFile).')
    a(f'{AH_ID}:')
    a("  permission: ''")
    a('  multi_server: single')
    a('  bottom_inventory: false')
    a("  fill_material: 'AIR'")
    a(f'  name: {q(T + "Dom aukcyjny")}')
    a('  size: 6')
    a('  default_category: all')
    a('  default_regions: [stolica]')
    a('  default_minimum_bid_increment: 5')
    a("  default_sales_mode: 'fixed'")
    a("  default_currency: 'dollar'")
    a('  # Domyslnie 3 dni: dzieki temu przycisk potwierdzenia liczy oplaty na zywo od pierwszej cyfry.')
    a('  default_days: 3')
    a('  # Limit aktywnych ofert na postac.')
    a('  player_max_item:')
    a('    default: 12')
    a('  disable_partial_purchase: true')
    a('  start_script: []')
    a('  close_script: []')
    a('  player_input:')
    a('    signgui: {material: OAK_WALL_SIGN, dye_color: BLACK, line1: "", line2: "Wpisz cenę", line3: "", line4: "", input_line: 1, max_try: 3, regex: "\\\\d+(\\\\.\\\\d+)?"}')
    a('    anvilgui:')
    a('      title: "Wpisz cenę"')
    a('      text: "0"')
    a('      slots: [2]')
    a('      left_item: {source: vanilla, name: " ", material: paper, flags: [HIDE_ATTRIBUTES]}')
    a('      right_item: {source: vanilla, name: " ", material: paper, flags: [HIDE_ATTRIBUTES]}')
    a('      result_item: {source: vanilla, name: "Zatwierdź", material: paper, flags: [HIDE_ATTRIBUTES]}')
    a('      max_try: 3')
    a('      regex: "\\\\d+(\\\\.\\\\d+)?"')
    P_ = '<gold><bold>Dom aukcyjny</bold></gold> <dark_gray>»</dark_gray> '
    msgs = {
        'item-listed': P_ + "<green>Wystawiono {amount}x <white>{item_name}</white> po <gold>{unit_price}{currency_symbol}</gold>/szt.",
        'item-listed-bid': P_ + "<green>Wystawiono {amount}x <white>{item_name}</white>.",
        'item-listed-not-enough-money': P_ + "<red>Nie masz {total}{currency_symbol} na opłatę za wystawienie.",
        'item-listed-max-limit': P_ + "<red>Masz już maksymalną liczbę ofert. Zdejmij którąś w „Moje oferty”.",
        'item-purchased-online-seller': P_ + "<green>Sprzedano {amount}x <white>{item_name}</white> za {total}{currency_symbol}. Po podatku: <gold>{total_after_tax}{currency_symbol}</gold>.",
        'item-purchased-online-buyer': P_ + "<green>Kupiono {amount}x <white>{item_name}</white> za <gold>{total}{currency_symbol}</gold>.",
        'item-won-offline-buyer': '', 'item-won-buyer-online': '',
        'expired-bid-item-success-online-seller': '', 'expired-bid-item-fail-online-seller': '',
        'expired-fixed-item-online-seller': P_ + "<yellow>Oferta {amount}x <white>{item_name}</white> wygasła. Odbierz przedmiot w „Moje oferty”.",
        'expired-fixed-item-offline-seller': P_ + "<yellow>Część Twoich ofert wygasła. Odbierz przedmioty w „Moje oferty”.",
        'already-sold': P_ + "<red>Ktoś kupił tę ofertę przed Tobą.",
        'expired': P_ + "<red>Ta oferta właśnie wygasła.",
        'cancelled': P_ + "<red>Sprzedawca zdjął tę ofertę.",
        'not-enough-amount': P_ + "<red>Tej oferty zostało za mało.",
        'not-enough-money': P_ + "<red>Nie masz {total}{currency_symbol}.",
        'money-player-join': P_ + "<green>Gdy Cię nie było, sprzedałeś przedmioty za <gold>{total}{currency_symbol}</gold>. Pieniądze są już na Twoim koncie.",
        'collect-money-from-tax-system': P_ + "<green>Zebrano {total}{currency_symbol} podatku.",
        'bid-cancel-new-bid': '', 'bid-place': '',
        'player-cancel-item': P_ + "<green>Zdjęto ofertę {amount}x <white>{item_name}</white>. Przedmiot wrócił do plecaka.",
        'player-take-expired-item': '', 'player-take-won-item': '',
        'delivery-successful-inventory': '', 'delivery-successful-playervault': '',
        'delivery-fail-inventory': P_ + "<red>Nie masz miejsca w plecaku — nic nie pobrano, zwolnij miejsce i spróbuj jeszcze raz.",
        'delivery-fail-playervault': P_ + "<red>Brak miejsca.",
        'delivery-fail-inventory-playervault': P_ + "<red>Nie masz miejsca w plecaku — nic nie pobrano.",
    }
    a('  messages:')
    for k, v in msgs.items():
        a(f'    {k}: {q(v)}')
    a('  broadcast:')
    for k in ('item-listed', 'item-listed-bid', 'item-purchased-online-buyer', 'bid-place'):
        a(f"    {k}: ''")
    a('  sound:')
    ok, no = '[ minecraft:entity.experience_orb.pickup,master,0.6,1.2 ]', '[ minecraft:entity.villager.no,master,0.8,1 ]'
    for k in msgs:
        a(f'    {k}: {no if any(w in k for w in ("fail", "not-enough", "already", "expired", "cancelled", "max-limit")) else ok}')
    a('  discord:')
    a("    webhookurl: ''")
    a('    regex: "[^a-zA-Z0-9/■\\\\s]"')
    a('    contain: []')
    for k in ('item-listed', 'item-purchased', 'item-expired', 'item-expired-taken', 'bid-place', 'player-cancel-item', 'player-take-expired-item', 'player-take-won-item'):
        a(f"    {k}: ''")
    a('  sorting:')
    a('    default_ascending: ["<gray>▷ Najstarsze", "<green><bold>▶ Najstarsze"]')
    a('    default_descending: ["<gray>▷ Najnowsze", "<green><bold>▶ Najnowsze"]')
    a('    price_ascending: ["<gray>▷ Najtańsze", "<green><bold>▶ Najtańsze"]')
    a('    price_descending: ["<gray>▷ Najdroższe", "<green><bold>▶ Najdroższe"]')
    a('    expire_ascending: ["<gray>▷ Najkrócej do końca", "<green><bold>▶ Najkrócej do końca"]')
    a('  sorting_custom:')
    a('    stats: []')
    a('    priority: {}')
    a('  delivery:')
    a('    deliver_offline_items_on_join: true')
    a('    purchase_mode: 1')
    a('    playervault: []')
    a('  event_actions:')
    a('    purchase_successful: category')
    a('    purchase_failed: category')
    a('    sell_successful: category')
    a('  display_lore:')
    for l in ['', '<dark_gray>─────────────────', '<gray>Sprzedawca: <white>{seller}',
              '<gray>Cena: <gold>{price} {currency_symbol}</gold><gray> za sztukę',
              '<gray>Kupujesz cały stos — sumę pokaże potwierdzenie.',
              '<gray>Do końca: <white>{days} d {hours} h {minutes} min', '', '<yellow>Kliknij, aby kupić.']:
        a(f'    - {q(l)}')
    a('  display_lore_bid:')
    a("    - ''")
    a('  category_switcher:')
    for cid, nm, *_ in KAT:
        a(f'    {cid}: [{q("<gray>▷ " + nm)}, {q("<green><bold>▶ " + nm)}]')
    # ---------------------------------------------------------------- przyciski glowne
    a('  buttons:')
    for cid, nm, model, slot, opis in KAT:
        a(btn(f'category_{cid}', [slot], nazwa=f'<gold><bold>{nm}', model=model,
              lore=['', f'<gray>{opis}', '', '<yellow>Kliknij, aby pokazać.']))
    a(pusty('pusty_gora', [6, 7]))
    a(btn('saldo', [8], nazwa='<gold><bold>Twoje złoto', model=M('ekonomia'),
          lore=['', '<gray>Na koncie: <gold>%coretools_eco_dollar% $', '',
                '<gray>Pieniądze za sprzedane przedmioty trafiają', '<gray>tu same — także gdy jesteś offline.']))
    a(btn('wstecz', [45], nazwa='<gray>Wstecz', model=M('wstecz'), lore=['', '<gray>Wróć do menu Handel.'],
          conditions=akcja_ah('handel_do_menu')))
    a(btn('previous', [46], nazwa='<gray>Poprzednia strona', model=M('handel_strona_wstecz')))
    a(btn('sorting', [47], nazwa='<gold><bold>Sortowanie', model=M('statystyki'),
          lore=['', '{sorting}', '', '<yellow>LPM: dalej  <gray>·  <yellow>PPM: wstecz']))
    a(btn('player_items_all', [48], nazwa='<gold><bold>Moje oferty', model=M('zadania'),
          lore=['', '<gray>Aktywne oferty, zakupy, historia', '<gray>i odbiór przedmiotów z wygasłych ofert.', '',
                '<yellow>Kliknij, aby otworzyć.']))
    a(btn('sell', [49], nazwa='<green><bold>Wystaw przedmiot', model=M('handel_wystaw'),
          lore=['', '<gray>Połóż przedmiot, wpisz cenę za sztukę', '<gray>i wybierz czas wystawienia.', '',
                '<gray>Opłata za wystawienie: <white>1,5% / 2,5% / 4,5%', '<gray>wartości (1 / 3 / 7 dni), min. 0,10 $ — nie wraca.',
                '<gray>Podatek od sprzedaży: <white>6%', '<gray>Limit: <white>12 ofert naraz', '',
                '<yellow>Kliknij, aby wystawić.']))
    a(btn('next', [50], nazwa='<gray>Następna strona', model=M('handel_strona_dalej')))
    a(pusty('pusty_dol', [51]))
    a(btn('pomoc', [52], nazwa='<yellow><bold>Jak działa dom aukcyjny', model=M('pomoc'),
          lore=['', '<gray>• Kupujesz od graczy cały wystawiony stos.', '<gray>• Sprzedajesz po swojej cenie za sztukę.',
                '<gray>• Oferta wisi 1, 3 albo 7 dni; niesprzedany', '<gray>  przedmiot odbierasz w „Moje oferty”.',
                '<gray>• Pieniądze ze sprzedaży wpływają same,', '<gray>  także gdy jesteś offline.',
                '<gray>• Skup NPC płaci mniej niż gracze —', '<gray>  tu zwykle dostaniesz więcej.']))
    a(btn('zamknij', [53], nazwa='<red><bold>Zamknij', model=M('zamknij'), conditions=akcja_ah('handel_zamknij')))
    # ---------------------------------------------------------------- moje oferty
    a('  player_items:')
    a(f'    name: {q(T + "Moje oferty [{page_no}]")}')
    a('    size: 6')
    a('    bottom_inventory: false')
    a('    bottom_inventory_use_default: false')
    a('    active_display_lore:')
    for l in ['', '<yellow>Kliknij, aby zdjąć ofertę.', '<gray>Przedmiot wróci do plecaka,', '<gray>opłata za wystawienie nie wraca.']:
        a(f'      - {q(l)}')
    a('    expired_display_lore:')
    for l in ['', '<red>Oferta wygasła.', '<yellow>Kliknij, aby odebrać przedmiot.']:
        a(f'      - {q(l)}')
    a('    buttons:')
    tabs = [('player_items_all', 'Wszystkie', 'zadania', 0, 'Wszystko, co wystawiłeś i kupiłeś.'),
            ('player_items_active', 'Aktywne', 'handel_wystaw', 1, 'Oferty, które wiszą. Kliknij ofertę, aby ją zdjąć.'),
            ('player_items_expired_not_taken', 'Do odbioru', 'handel_skup', 2, 'Wygasłe oferty. Kliknij, aby odebrać przedmiot.'),
            ('player_items_purchased', 'Kupione', 'handel_sklep', 3, 'Twoje zakupy.'),
            ('player_items_old', 'Historia', 'statystyki', 4, 'Oferty zakończone.')]
    for k, nm, ic, slot, op in tabs:
        a('\n'.join('  ' + l for l in btn(k, [slot], nazwa=f'<gold><bold>{nm}', model=M(ic), lore=['', f'<gray>{op}']).split('\n')))
    extra_player = [
        btn('pusty_gora', [5, 6, 7, 8], model=M('handel_pusty'), hide=True),
        btn('home', [45], nazwa='<gray>Wróć do domu aukcyjnego', model=M('wstecz')),
        btn('player_items_previous', [46], nazwa='<gray>Poprzednia strona', model=M('handel_strona_wstecz')),
        btn('pusty_dol', [47, 48, 49, 51, 52], model=M('handel_pusty'), hide=True),
        btn('player_items_next', [50], nazwa='<gray>Następna strona', model=M('handel_strona_dalej')),
        btn('zamknij', [53], nazwa='<red><bold>Zamknij', model=M('zamknij'), conditions=akcja_ah('handel_zamknij')),
    ]
    for b in extra_player:
        a('\n'.join('  ' + l for l in b.split('\n')))
    # ---------------------------------------------------------------- zakup
    a('  purchase:')
    a(f'    name: {q(T + "Kupno")}')
    a('    size: 6')
    a('    bottom_inventory: false')
    a('    bottom_inventory_use_default: false')
    a('    buttons:')
    zakup_lore = ['', '<gray>Ilość: <white>{amount}', '<gray>Cena za sztukę: <white>{unit_price} $',
                  '<gold><bold>Razem: {total} $', '']
    pb = [
        btn('fill', [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 17, 18, 19, 20, 21, 23, 24, 25, 26, 27, 28,
                     29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 41, 42, 43, 44, 46, 47, 48, 50, 51, 52],
            model=M('handel_pusty'), hide=True),
        btn('purchase_item', [22], material='air'),
        btn('purchase_confirm', [40], nazwa='<green><bold>Kup', model=M('handel_zatwierdz'),
            lore=zakup_lore + ['<yellow>Kliknij, aby przygotować zakup.'],
            extra=['name2: ' + q('<gold><bold>Kliknij ponownie, aby kupić'), 'material2: paper',
                   'item_model2: ' + M('handel_zatwierdz_gotowe'), 'empty_slot_material: paper', 'lore2:'] +
                  [f'  - {q(l)}' for l in zakup_lore + ['<gold>Drugie kliknięcie kupuje. Bez zwrotów.']]),
        btn('info', [13], nazwa='<gold><bold>Kupno oferty', model=M('pomoc'),
            lore=['', '<gray>Kupujesz cały stos widoczny niżej.', '<gray>Przedmiot trafi do plecaka.',
                  '<gray>Brak miejsca = zakup się nie uda', '<gray>i nic nie zostanie pobrane.']),
        btn('home', [45], nazwa='<gray>Wróć do domu aukcyjnego', model=M('wstecz')),
        btn('saldo', [49], nazwa='<gold><bold>Twoje złoto', model=M('ekonomia'), lore=['', '<gray>Na koncie: <gold>%coretools_eco_dollar% $']),
        btn('zamknij', [53], nazwa='<red><bold>Zamknij', model=M('zamknij'), conditions=akcja_ah('handel_zamknij')),
    ]
    for b in pb:
        a('\n'.join('  ' + l for l in b.split('\n')))
    # ---------------------------------------------------------------- wystawianie
    a('  sell:')
    a(f'    name: {q(T + "Wystaw przedmiot")}')
    a('    size: 6')
    a('    buttons:')
    sell_lore = ['', '<gray>Ilość: <white>{amount}', '<gray>Cena za sztukę: <white>{unit_price} $',
                 '<gray>Opłata za wystawienie: <red>{listing_fee} $ <dark_gray>(pobierana teraz)',
                 '<gray>Podatek od sprzedaży: <red>{tax_per_unit} $/szt. <dark_gray>({tax_percent}%)',
                 '<gray>Dostaniesz za sztukę: <green>{earning_per_unit} $',
                 '<gold><bold>Za cały stos: {amount} × {earning_per_unit} $', '']
    # sloty wypelniacza: WSZYSTKO poza slotem przedmiotu i przyciskami — shift-klik nie moze
    # wrzucic przedmiotu do pustego slotu, z ktorego zamkniecie okna go nie zwraca.
    uzyte = {13, 19, 20, 21, 28, 29, 30, 37, 38, 39, 46, 47, 48, 24, 25, 26, 42, 45, 16, 53}
    sb = [
        btn('fill', sorted(set(range(54)) - uzyte), model=M('handel_pusty'), hide=True),
        btn('sell_player_item', [13], material='air'),
        btn('info', [16], nazwa='<gold><bold>Jak wystawić', model=M('pomoc'),
            lore=['', '<gray>1. Połóż przedmiot w polu obok', '<gray>   (albo shift+klik w plecaku).',
                  '<gray>2. Wpisz cenę ZA SZTUKĘ klawiszami.', '<gray>3. Wybierz czas: 1, 3 albo 7 dni.',
                  '<gray>4. Kliknij „Wystaw” dwa razy.', '',
                  '<gray>Wystawić można surowce, półprodukty,', '<gray>narzędzia, sprzęt, potrawy i mikstury.',
                  '<gray>Cena najmniej 1 $ (pierwsza cyfra nie 0).']),
        btn('sell_day_1', [24], nazwa='<white>1 dzień', model=M('handel_dni'),
            lore=['', '<gray>Opłata: 1,5% wartości.'],
            extra=['name2: ' + q('<green><bold>1 dzień ✔'), 'material2: paper', 'item_model2: ' + M('handel_dni_wybrane'),
                   'empty_slot_material: paper', 'lore2:', f'  - {q("")}', f'  - {q("<gray>Opłata: 1,5% wartości.")}']),
        btn('sell_day_3', [25], nazwa='<white>3 dni', model=M('handel_dni'),
            lore=['', '<gray>Opłata: 2,5% wartości.'],
            extra=['name2: ' + q('<green><bold>3 dni ✔'), 'material2: paper', 'item_model2: ' + M('handel_dni_wybrane'),
                   'empty_slot_material: paper', 'lore2:', f'  - {q("")}', f'  - {q("<gray>Opłata: 2,5% wartości.")}']),
        btn('sell_day_7', [26], nazwa='<white>7 dni', model=M('handel_dni'),
            lore=['', '<gray>Opłata: 4,5% wartości.'],
            extra=['name2: ' + q('<green><bold>7 dni ✔'), 'material2: paper', 'item_model2: ' + M('handel_dni_wybrane'),
                   'empty_slot_material: paper', 'lore2:', f'  - {q("")}', f'  - {q("<gray>Opłata: 4,5% wartości.")}']),
        btn('sell_confirm', [42], nazwa='<green><bold>Wystaw', model=M('handel_zatwierdz'),
            lore=sell_lore + ['<yellow>Kliknij, aby przygotować.'],
            extra=['name2: ' + q('<gold><bold>Kliknij ponownie, aby wystawić'), 'material2: paper',
                   'item_model2: ' + M('handel_zatwierdz_gotowe'), 'empty_slot_material: paper', 'lore2:'] +
                  [f'  - {q(l)}' for l in sell_lore + ['<gold>Drugie kliknięcie wystawia i pobiera opłatę.']]),
        btn('home', [45], nazwa='<gray>Wróć do domu aukcyjnego', model=M('wstecz'),
            lore=['', '<gray>Przedmiot wróci do plecaka.']),
        btn('zamknij', [53], nazwa='<red><bold>Zamknij', model=M('zamknij'), lore=['', '<gray>Przedmiot wróci do plecaka.'],
            conditions=akcja_ah('handel_zamknij')),
    ]
    klaw = {'7': 19, '8': 20, '9': 21, '4': 28, '5': 29, '6': 30, '1': 37, '2': 38, '3': 39, 'dot': 46, '0': 47, 'backspace': 48}
    for k, s in klaw.items():
        model = {'dot': M('handel_kropka'), 'backspace': M('handel_cofnij')}.get(k, M(f'handel_cyfra_{k}'))
        nm = {'dot': '<white>Kropka  <gray>( , )', 'backspace': '<white>Cofnij'}.get(k, f'<white><bold>{k}')
        sb.append(btn(f'sell_{k}', [s], nazwa=nm, model=model))
    for b in sb:
        a('\n'.join('  ' + l for l in b.split('\n')))
    # ---------------------------------------------------------------- podatki (tylko OP; bez przycisku)
    a('  tax_system:')
    a('    name: "Podatki"')
    a('    size: 6')
    a('    bottom_inventory: false')
    a('    bottom_inventory_use_default: false')
    a('    tax_mode: 1')
    a('    default:')
    a(f'      listing_fee: {q(LISTING_FEE)}')
    a(f'      sell_tax: {q(SELL_TAX)}')
    a('    buttons:')
    a('      collect_tax: {location: top, slots: [45], source: vanilla, material: paper, name: "Zbierz podatek"}')
    a('      reset_tax: {location: top, slots: [36], source: vanilla, material: paper, name: "Resetuj podatki"}')
    a('      next: {location: top, slots: [50], source: vanilla, material: paper, name: "Dalej"}')
    a('      previous: {location: top, slots: [48], source: vanilla, material: paper, name: "Wstecz"}')
    a('    categories:')
    for cid, nm, *_ in KAT[1:]:
        a(f'      {cid}:')
        a(f'        name: {q("{name=" + json.dumps(nm) + ";material=paper}")}')
        a(f'        listing_fee: [{q("{formula=" + json.dumps(LISTING_FEE) + ";name=" + json.dumps("Opłata") + ";material=paper}")}]')
        a(f'        sell_tax: [{q("{formula=" + json.dumps(SELL_TAX) + ";name=" + json.dumps("Podatek 6%") + ";material=paper}")}]')
    # ---------------------------------------------------------------- kategorie (biala lista)
    a('  categories:')
    a('    all:')
    a(f'      name: {q(T + "Dom aukcyjny [{page_no}]")}')
    a(kategoria_ah('surowce', SUROWCE))
    a(kategoria_ah('polprodukty', POLPRODUKTY))
    a(kategoria_ah('narzedzia', NARZEDZIA, ['tool']))
    a(kategoria_ah('sprzet', [], TYPY_SPRZETU))
    a(kategoria_ah('potrawy', POTRAWY + MIKSTURY))
    return '\n'.join(L) + '\n'


# =============================================================================== SKUP
GRUPY_SKUPU = [  # id, nazwa, model zakladki, profesje (stacje) ktorych wyroby tu ida
    ('drewno', 'Drewno', 'nexo:prof_oak_timber', ['woodworking'], ['PROF_OAK_TIMBER', 'PROF_SPRUCE_RESIN', 'PROF_BIRCH_LATH']),
    ('ziola', 'Zioła i alchemia', 'nexo:prof_moonleaf', ['herbalism', 'alchemy'], ['PROF_MEDICINAL_HERB', 'PROF_MOONLEAF', 'PROF_REDCAP']),
    ('plony', 'Plony i hodowla', 'nexo:prof_fresh_harvest', ['farming'], ['PROF_FRESH_HARVEST']),
    ('polow', 'Połów', 'nexo:prof_raw_catch', ['fishing'], ['PROF_RAW_CATCH', 'PROF_CLAM', 'PROF_RARE_FISH', 'PROF_WATER_ESSENCE', 'PROF_PEARL']),
    ('kamien', 'Ruda i kamień', 'nexo:prof_ore_chunk', ['mining', 'building'], ['PROF_ORE_CHUNK', 'PROF_SILVER_ORE', 'PROF_ROUGH_STONE', 'PROF_RAW_GEM', 'PROF_ARCANE_CRYSTAL', 'PROF_CLAY_LUMP']),
    ('lowy', 'Łowy', 'nexo:prof_beast_scrap', ['hunting'], ['PROF_BEAST_SCRAP', 'PROF_BEAST_BONE', 'PROF_RAW_FLEECE', 'PROF_VENOM_SAC', 'PROF_MONSTER_TROPHY']),
    ('klejnoty', 'Klejnoty i runy', 'nexo:prof_cut_gem', ['jewelcrafting', 'enchanting'], ['PROF_ROUGH_CRYSTAL']),
    ('kuznia', 'Kuźnia i krawiectwo', 'nexo:prof_metal_plate', ['smithing', 'tailoring'], []),
    ('kuchnia', 'Kuchnia', 'nexo:prof_harvest_feast', ['cooking'], ['PROF_WILD_SPICE']),
]


def grupa_skupu():
    """Przypisuje kazdy przedmiot z ceny.json do jednej zakladki skupu: surowce wg listy,
    wyroby wg stacji, ktora je robi (pierwsza pasujaca droga z modelu)."""
    przydzial = {}
    for gid, _, _, stacje, surowe in GRUPY_SKUPU:
        for i in surowe:
            przydzial[i] = gid
    for i, c in CENY.items():
        if i in przydzial:
            continue
        if not c.get('do_skupu', True):
            continue  # profesje v2: gotowe wyroby (narzedzia, mikstury, klejnoty...) tylko na rynku graczy
        stacja = c['droga'].split(' ')[0]
        for gid, _, _, stacje, _ in GRUPY_SKUPU:
            if stacja in stacje:
                przydzial[i] = gid
                break
        else:
            raise SystemExit(f'brak zakladki skupu dla {i} ({c["droga"]})')
    return przydzial


def mi_typ(i):
    return ITEMS[i]['type']


def skup():
    przydzial = grupa_skupu()
    L = []
    a = L.append
    a('# SKUP NPC world1 — WYGENEROWANE przez tools/handel/generuj.py z tools/handel/ceny.json.')
    a('# Ceny = 40% wartosci liczonej z czasu zdobycia (model: tools/handel/ekonomia.py).')
    a('# Skup TYLKO kupuje od gracza (options.sell: false) — surowcow PROF_* nie sprzedajemy za pieniadze.')
    a('# Otwiera go konsola z menu Handel:  core-shops skup <gracz>   (komenda ma permission OP).')
    a('# Kliki (Shops$ShopConfig.items_click): LPM = sprzedaj 1, SHIFT+LPM = sprzedaj wszystkie takie,')
    a('# PPM = okno ilosci. Obsluga klikniec w trybie normalnym idzie na watku glownym i ZABIERA')
    a('# przedmioty przed wyplata (Check_Buy/BuyAll), pas szybkiego dostepu jest pomijany.')
    a('skup:')
    a("  permission: ''")
    a('  bottom_inventory: false')
    a(f'  name: {q(T + "Skup")}')
    a('  size: 6')
    a("  fill_material: 'AIR'")
    a(f'  default_category: {GRUPY_SKUPU[0][0]}')
    a("  default_animation: ''")
    # false: surowce z wezlow laduja NAJPIERW w pasku szybkiego dostepu (mi give / zbior) — przy true
    # swiezo zebrane drewno bylo "niewidoczne" dla skupu. Sprzedaz narzedzia z paska wymaga
    # swiadomego kliku na TE narzedzie w skupie. Zbroja i druga reka (sloty 36-40) sa pomijane zawsze.
    a('  ignore_hotbar_slots: false')
    P_ = '<gold><bold>Skup</bold></gold> <dark_gray>»</dark_gray> '
    msgs = {
        'insufficient-money': P_ + '<red>Brak środków.',
        'not-allowed-to-sell': P_ + '<red>Skup niczego nie sprzedaje — kupisz w domu aukcyjnym.',
        'not-allowed-to-buy': P_ + '<red>Tego skup nie przyjmuje.',
        'successful-sell': P_ + '<green>Kupiono {amount}x {item_name}<green> za {total_cost}{currency_symbol}.',
        'successful-buy': P_ + '<green>Sprzedano <white>{amount}x {item_name}</white> za <gold>{total_cost}{currency_symbol}</gold>.',
        'not-enough-item': P_ + '<red>Nie masz {amount}x {item_name}<red>.',
        'not-enough-item-buy-all': P_ + '<red>Nie masz {item_name}<red> w plecaku.',
        'no-space-inventory': P_ + '<red>Brak miejsca w plecaku.',
        'no-space-playervault': P_ + '<red>Brak miejsca.',
        'no-permission': P_ + '<red>Brak dostępu.',
    }
    a('  messages:')
    for k, v in msgs.items():
        a(f'    {k}: {q(v)}')
    a('  sound:')
    for k in ('no_permission', 'insufficient_money', 'not_allowed_to_sell', 'not_allowed_to_buy', 'not_enough_item',
              'not_enough_item_buy_all', 'no_space_inventory', 'no_space_playervault'):
        a(f'    {k}: [minecraft:entity.villager.no,master,0.8,1]')
    a('    successful_sell: [minecraft:entity.experience_orb.pickup,master,0.6,1.2]')
    a('    successful_buy: [minecraft:entity.experience_orb.pickup,master,0.6,1.2]')
    a('  sorting:')
    a('    default: ["<gray>▷ Kolejność", "<green><bold>▶ Kolejność"]')
    a('    money_descending: ["<gray>▷ Najdroższe", "<green><bold>▶ Najdroższe"]')
    a('    money_ascending: ["<gray>▷ Najtańsze", "<green><bold>▶ Najtańsze"]')
    a('  delivery:')
    a('    mode: 1')
    a("    playervault: ''")
    a('  items_click:')
    a('    left_click: quick_buy')
    a('    right_click: advanced')
    a("    swap_hand: ''")
    a('    shift_left_click: buy_all')
    a("    shift_right_click: ''")
    a('  amount:')
    a('    quick_sell: 1')
    a('    quick_buy: 1')
    a('  bottom_display_lore:')
    for l in ['', '<dark_gray>─────────────────', '<gray>Skup płaci: <gold>{money_buy_dollar} $</gold><gray> za sztukę', '',
              '<yellow>LPM<gray> — sprzedaj 1', '<yellow>Shift+LPM<gray> — sprzedaj wszystkie takie z plecaka',
              '<yellow>PPM<gray> — wybierz ilość', '<dark_gray>Założona zbroja i druga ręka są pomijane.']:
        a(f'    - {q(l)}')
    a('  bottom_item_lore: []')
    # okno ilosci
    a('  advanced:')
    a(f'    name: {q(T + "Skup · ilość")}')
    a('    size: 6')
    a('    fill_material: AIR')
    a("    animation: ''")
    a('    bottom_inventory: false')
    a("    view_bottom_inventory: 'default'")
    a('    buttons:')
    adv = [
        btn('item_to_be_purchased', [13], material='air'),
        btn('missing_purchase', [40], model=M('handel_pusty'), hide=True),
        btn('back_to_category', [45], nazwa='<gray>Wróć do skupu', model=M('wstecz')),
        btn('remove_16', [19], nazwa='<red><bold>-16', model=M('handel_strona_wstecz')),
        btn('remove_1', [20], nazwa='<red><bold>-1', model=M('handel_strona_wstecz')),
        btn('add_1', [24], nazwa='<green><bold>+1', model=M('handel_strona_dalej')),
        btn('add_16', [25], nazwa='<green><bold>+16', model=M('handel_strona_dalej')),
        btn('set_1', [30], nazwa='<white>Ustaw 1', model=M('handel_cyfra_1'), extra=['amount: 1']),
        btn('set_64', [32], nazwa='<white>Ustaw 64', model=M('handel_cyfra_6'), extra=['amount: 64']),
        btn('buy_dollar', [40], nazwa='<green><bold>Sprzedaj', model=M('handel_zatwierdz'),
            lore=['', '<gray>Dostaniesz: <gold>{money_buy_dollar} $']),
        btn('zamknij', [53], nazwa='<red><bold>Zamknij', model=M('zamknij'), scripts=['handel_zamknij']),
    ]
    zajete = {13, 40, 45, 19, 20, 24, 25, 30, 32, 53}
    adv.insert(0, btn('empty', sorted(set(range(54)) - zajete), model=M('handel_pusty'), hide=True))
    for b in adv:
        a('\n'.join('  ' + l for l in b.split('\n')))
    # zakladki + nawigacja
    a('  buttons:')
    for n, (gid, nm, model, _, _) in enumerate(GRUPY_SKUPU):
        a(btn(gid, [n], nazwa=f'<gold><bold>{nm}', model=model, lore=['', '<yellow>Kliknij, aby pokazać.']))
    a(btn('wstecz', [45], nazwa='<gray>Wstecz', model=M('wstecz'), lore=['', '<gray>Wróć do menu Handel.'], scripts=['handel_do_menu']))
    a(btn('previous', [46], nazwa='<gray>Poprzednia strona', model=M('handel_strona_wstecz')))
    a(pusty('pusty_dol', [47, 48, 50, 51]))
    a(btn('pomoc', [49], nazwa='<yellow><bold>Jak działa skup', model=M('pomoc'),
          lore=['', '<gray>Skup kupuje wszystko, co wydobędziesz', '<gray>i wytworzysz w profesjach — zawsze,',
                '<gray>ale taniej niż gracze w domu aukcyjnym', '<gray>(zwykle ok. 40% wartości rynkowej).', '',
                '<gray>Skup niczego nie sprzedaje.', '<gray>Na koncie: <gold>%coretools_eco_dollar% $']))
    a(btn('next', [52], nazwa='<gray>Następna strona', model=M('handel_strona_dalej')))
    a(btn('zamknij', [53], nazwa='<red><bold>Zamknij', model=M('zamknij'), scripts=['handel_zamknij']))
    # kategorie
    a('  categories:')
    for gid, nm, _, _, _ in GRUPY_SKUPU:
        a(f'    {gid}:')
        a(f'      name: {q(T + "Skup · " + nm)}')
        items = sorted((i for i, g in przydzial.items() if g == gid), key=lambda i: CENY[i]['wartosc'])
        # profesje v2: 10 tierow -> zakladka ma kilka stron (przyciski previous/next w slotach 46/52)
        assert len(items) <= 36 * 4, (gid, len(items))
        for i in items:
            c = CENY[i]
            a(f'      {i.lower()}:')
            a('        options: {sell: false, buy: true}')
            a('        prices:')
            a('          buy:')
            a(f"            dollar: {c['skup']}")
            a('        display:')
            a('          source: mmoitems')
            a(f'          type: {mi_typ(i)}')
            a(f'          id: {i}')
            a('        item:')
            a('          source: mmoitems')
            a(f'          type: {mi_typ(i)}')
            a(f'          id: {i}')
    return '\n'.join(L) + '\n'


# =============================================================================== SKLEP
# Sklep sprzedaje TYLKO podstawy vanilla, ktore nie wchodza do skupu ani na aukcje (kategorie
# domu aukcyjnego to biala lista MMOItems) — nie ma wiec zadnej drogi kup-u-NPC -> sprzedaj.
# Ceny w skali zarobku: skup daje ~300 $/h na starcie, zelazny kilof = ~12 min takiej pracy.
SKLEP = [
    ('narzedzia', 'Narzędzia', 'minecraft:iron_pickaxe', [
        ('kilof_kamienny', 'STONE_PICKAXE', 1, 15, 'Wystarczy na zwykły urobek i kamień.', 'Kamienny kilof'),
        ('kilof_zelazny', 'IRON_PICKAXE', 1, 60, 'Szybciej kopie twardsze żyły.', 'Żelazny kilof'),
        ('siekiera_kamienna', 'STONE_AXE', 1, 12, 'Podstawowe narzędzie.', 'Kamienna siekiera'),
        ('wedka', 'FISHING_ROD', 1, 20, 'Bez wędki nie ma połowu.', 'Wędka'),
    ]),
    ('wyprawa', 'Na wyprawę', 'minecraft:torch', [
        ('pochodnia', 'TORCH', 16, 0.5, 'Pakiet 16 sztuk, cena za sztukę.', 'Pochodnie'),
        ('chleb', 'BREAD', 8, 2, 'Pakiet 8 sztuk, cena za sztukę. Lepsze jedzenie gotuje Kucharz.', 'Chleb'),
    ]),
]


def sklep():
    L = []
    a = L.append
    a('# SKLEP NPC world1 — WYGENEROWANE przez tools/handel/generuj.py. Tylko podstawy vanilla.')
    a('# Nie sprzedaje surowcow ani wyrobow PROF_* (zabiloby zbieractwo i rzemioslo).')
    a('# Skup nie przyjmuje vanilli, a dom aukcyjny jej nie wystawi -> brak petli kup/sprzedaj NPC.')
    a('# Otwiera go konsola z menu Handel:  core-shops sklep <gracz>')
    a('sklep:')
    a("  permission: ''")
    a('  bottom_inventory: false')
    a(f'  name: {q(T + "Sklep")}')
    a('  size: 6')
    a("  fill_material: 'AIR'")
    a(f'  default_category: {SKLEP[0][0]}')
    a("  default_animation: ''")
    a('  ignore_hotbar_slots: false')
    P_ = '<gold><bold>Sklep</bold></gold> <dark_gray>»</dark_gray> '
    msgs = {
        'insufficient-money': P_ + '<red>Nie masz {total_cost}{currency_symbol}.',
        'not-allowed-to-sell': P_ + '<red>Tego nie sprzedajemy.',
        'not-allowed-to-buy': P_ + '<red>Sklep nie skupuje — sprzedawaj w Skupie albo w domu aukcyjnym.',
        'successful-sell': P_ + '<green>Kupiono <white>{amount}x {item_name}</white> za <gold>{total_cost}{currency_symbol}</gold>.',
        'successful-buy': P_ + '<green>Sprzedano {amount}x {item_name}.',
        'not-enough-item': P_ + '<red>Brak przedmiotu.',
        'not-enough-item-buy-all': P_ + '<red>Brak przedmiotu.',
        'no-space-inventory': P_ + '<red>Brak miejsca w plecaku — nic nie pobrano.',
        'no-space-playervault': P_ + '<red>Brak miejsca.',
        'no-permission': P_ + '<red>Brak dostępu.',
    }
    a('  messages:')
    for k, v in msgs.items():
        a(f'    {k}: {q(v)}')
    a('  sound:')
    for k in ('no_permission', 'insufficient_money', 'not_allowed_to_sell', 'not_allowed_to_buy', 'not_enough_item',
              'not_enough_item_buy_all', 'no_space_inventory', 'no_space_playervault'):
        a(f'    {k}: [minecraft:entity.villager.no,master,0.8,1]')
    a('    successful_sell: [minecraft:entity.experience_orb.pickup,master,0.6,1.2]')
    a('    successful_buy: [minecraft:entity.experience_orb.pickup,master,0.6,1.2]')
    a('  sorting:')
    a('    default: ["<gray>▷ Kolejność", "<green><bold>▶ Kolejność"]')
    a('    money_descending: ["<gray>▷ Najdroższe", "<green><bold>▶ Najdroższe"]')
    a('    money_ascending: ["<gray>▷ Najtańsze", "<green><bold>▶ Najtańsze"]')
    a('  delivery:')
    a('    mode: 1')
    a("    playervault: ''")
    a('  items_click:')
    a('    left_click: quick_sell')
    a("    right_click: ''")
    a("    swap_hand: ''")
    a("    shift_left_click: ''")
    a("    shift_right_click: ''")
    a('  amount:')
    a('    quick_sell: 1')
    a('    quick_buy: 1')
    a('  bottom_display_lore:')
    for l in ['', '<dark_gray>─────────────────', '<gray>Cena: <gold>{money_sell_dollar} $', '', '<yellow>Kliknij, aby kupić.']:
        a(f'    - {q(l)}')
    a('  bottom_item_lore: []')
    a('  advanced:')
    a(f'    name: {q(T + "Sklep")}')
    a('    size: 6')
    a('    fill_material: AIR')
    a("    animation: ''")
    a('    bottom_inventory: false')
    a("    view_bottom_inventory: 'default'")
    a('    buttons:')
    for b in [btn('item_to_be_purchased', [13], material='air'), btn('missing_purchase', [40], model=M('handel_pusty'), hide=True),
              btn('back_to_category', [45], nazwa='<gray>Wróć', model=M('wstecz')),
              btn('sell_dollar', [40], nazwa='<green><bold>Kup', model=M('handel_zatwierdz'), lore=['', '<gray>Cena: <gold>{money_sell_dollar} $'])]:
        a('\n'.join('  ' + l for l in b.split('\n')))
    a('  buttons:')
    for n, (cid, nm, model, _) in enumerate(SKLEP):
        a(btn(cid, [n], nazwa=f'<gold><bold>{nm}', model=model, lore=['', '<yellow>Kliknij, aby pokazać.']))
    a(pusty('pusty_gora', list(range(len(SKLEP), 9))))
    a(btn('wstecz', [45], nazwa='<gray>Wstecz', model=M('wstecz'), lore=['', '<gray>Wróć do menu Handel.'], scripts=['handel_do_menu']))
    a(pusty('pusty_dol', [46, 47, 48, 50, 51, 52]))
    a(btn('pomoc', [49], nazwa='<yellow><bold>Sklep', model=M('pomoc'),
          lore=['', '<gray>Podstawowe narzędzia i zapasy.', '<gray>Surowce i wyroby profesji kupujesz', '<gray>od graczy w domu aukcyjnym.', '',
                '<gray>Na koncie: <gold>%coretools_eco_dollar% $']))
    a(btn('zamknij', [53], nazwa='<red><bold>Zamknij', model=M('zamknij'), scripts=['handel_zamknij']))
    a('  categories:')
    for cid, nm, _, towary in SKLEP:
        a(f'    {cid}:')
        a(f'      name: {q(T + "Sklep · " + nm)}')
        for tid, mat, ilosc, cena, opis, pl in towary:
            a(f'      {tid}:')
            a('        options: {sell: true, buy: false}')
            a(f'        quick_sell_amount: {ilosc}')
            a('        prices:')
            a('          sell:')
            a(f'            dollar: {cena}')
            a('        display:')
            a('          source: vanilla')
            a(f'          material: {mat.lower()}')
            a(f'          amount: {ilosc}')
            a(f'          name: {q("<white>" + pl)}')
            a('          flags: [HIDE_ATTRIBUTES]')
            a('          lore:')
            a(f'            - {q("<gray>" + opis)}')
            a('        item:')
            a('          source: vanilla')
            a(f'          material: {mat.lower()}')
    return '\n'.join(L) + '\n'


SKRYPTY = '''# Skrypty okien HANDLU (dom aukcyjny, skup, sklep). WYGENEROWANE przez tools/handel/generuj.py.
# Tresc akcji jest w Scripts/, wiec poprawki wchodza po /ct reload (EventsManager wymaga restartu).

# Wstecz z okna CoreTools do menu Handel (DeluxeMenus). Z konsoli — DeluxeMenus nie rejestruje
# swoich open_command w dispatcherze (memory deluxemenus-pulapki).
handel_do_menu:
  Conditions: []
  Mechanics:
    - command{c="dm open hub_ekonomia %player_name%";runas=console} @Self

# delay=2: w SKLEPIE/SKUPIE CoreTools po skrypcie przycisku odswieza okno (ShopUtils.havetoRefresh) —
# zamkniecie w tym samym ticku bylo od razu cofane (sprawdzone botem, sonda5).
handel_zamknij:
  Conditions: []
  Mechanics:
    - closeinventory{delay=2} @Self
'''

if __name__ == '__main__':
    open(f'{P}/AuctionHouse/rynek.yml', 'w').write(rynek())
    open(f'{P}/Shops/skup.yml', 'w').write(skup())
    open(f'{P}/Shops/sklep.yml', 'w').write(sklep())
    open(f'{P}/Scripts/handel.yml', 'w').write(SKRYPTY)
    print('ok: rynek.yml, skup.yml, sklep.yml, Scripts/handel.yml')
    print('polprodukty:', len(POLPRODUKTY), 'narzedzia:', len(NARZEDZIA), 'sprzet PROF:', len(SPRZET_PROF), 'potrawy:', len(POTRAWY))
