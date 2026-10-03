#!/usr/bin/env python3
"""Księga Handlu: kopia generatora CoreTools 1.4.2 z warstwą oprawy K12.

Czyta ceny i receptury z serwera tylko do odczytu. Uruchom:
    python3 strony/handel/generuj.py
Wszystkie pliki wynikowe trafiają do strony/handel/out/gen/.
"""
import json, os

P = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out', 'gen')
H = '/opt/docker/mc/data/world1/tools/handel'
CENY = json.load(open(os.path.join(H, 'ceny.json')))
REC = json.load(open(os.path.join(H, 'receptury.json')))
ITEMS = REC['items']

# Prefiks starego generatora sluzy wyłącznie jako znacznik do podmiany.
# Widoczny tytul okna zawiera tylko &f, glify tla i glify przesuniecia.
T = '<white>䆐<white>'
q = lambda s: json.dumps(s, ensure_ascii=False)
M = lambda nazwa: f'nexo:hub_{nazwa}'          # model ikony z huba/handlu

SUROWCE = ['PROF_OAK_TIMBER', 'PROF_SPRUCE_RESIN', 'PROF_BIRCH_LATH', 'PROF_MEDICINAL_HERB', 'PROF_MOONLEAF',
           'PROF_REDCAP', 'PROF_FRESH_HARVEST', 'PROF_ROUGH_CRYSTAL', 'PROF_ROUGH_STONE', 'PROF_CLAY_LUMP',
           'PROF_WILD_SPICE', 'PROF_RAW_CATCH', 'PROF_CLAM', 'PROF_RARE_FISH', 'PROF_WATER_ESSENCE', 'PROF_PEARL',
           'PROF_ORE_CHUNK', 'PROF_SILVER_ORE', 'PROF_RAW_GEM', 'PROF_ARCANE_CRYSTAL', 'PROF_BEAST_SCRAP',
           'PROF_BEAST_BONE', 'PROF_RAW_FLEECE', 'PROF_VENOM_SAC', 'PROF_MONSTER_TROPHY']
MIKSTURY = ['PROF_ANTIDOTE', 'PROF_COMBAT_TONIC', 'PROF_WEAPON_OIL']
POLPRODUKTY = sorted(i for i, c in CENY.items() if c['typ'] == 'MATERIAL' and i not in SUROWCE and i not in MIKSTURY)
NARZEDZIA = sorted(i for i, c in CENY.items() if c['typ'] == 'TOOL')
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
        f'      name: {q(T)}',
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
    a(f'  name: {q(T)}')
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
    a(f'    name: {q(T)}')
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
    a(f'    name: {q(T)}')
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
    a(f'    name: {q(T)}')
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
    a(f'      name: {q(T)}')
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
    a(f'  name: {q(T)}')
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
    a(f'    name: {q(T)}')
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
        a(f'      name: {q(T)}')
        items = sorted((i for i, g in przydzial.items() if g == gid), key=lambda i: CENY[i]['wartosc'])
        assert len(items) <= 36, (gid, len(items))
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
    a(f'  name: {q(T)}')
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
    a(f'    name: {q(T)}')
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
        a(f'      name: {q(T)}')
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


# Warstwa Księgi: zachowuje ekonomię i twarde identyfikatory CoreTools z generatora
# powyżej. Źródłowe JSON są tylko czytane; cały eksport trafia do out/gen.
from collections import Counter
from pathlib import Path
import re
import sys
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
import ksiega_wspolne as W
from siatka import siatka as S

SCREENS = ('sklep', 'sklep_ilosc', 'skup', 'skup_ilosc',
           'rynek', 'moje_oferty', 'kupno', 'wystawienie')
LABELS = ('Sklep', 'Ilość zakupu', 'Skup', 'Ilość sprzedaży',
          'Dom aukcyjny', 'Moje oferty', 'Kupno oferty', 'Wystawienie')
SPINE = {4, 13, 22, 31, 40, 49}
EMBLEM = {9*r+c for r in range(3) for c in range(4)}
LEFT = [27, 28, 29, 30, 36, 37, 38, 39, 45, 46, 47, 48]
RIGHT = {9*r+c for r in range(1, 6) for c in (5, 6, 7, 8)}
FILLERS = {'fill', 'empty', 'pusty_gora', 'pusty_dol'}
REMOVED = {'wstecz', 'zamknij'}

def tile_slots(row, col):
    """Jedna aktywna miniatura Księgi zajmuje dokładnie 2 × 2 sloty."""
    assert col in (0, 1, 2, 5, 6, 7) and 0 <= row <= 4
    return [9*row+col, 9*row+col+1, 9*(row+1)+col, 9*(row+1)+col+1]

MAPS = {
    'sklep': {'narzedzia': 28, 'wyprawa': 29, 'pomoc': tile_slots(4, 1)},
    'sklep_ilosc': {'item_to_be_purchased': 23,
                    'missing_purchase': tile_slots(3, 0),
                    'sell_dollar': tile_slots(3, 0),
                    'back_to_category': tile_slots(3, 2)},
    'skup': {**{g[0]: s for g, s in zip(GRUPY_SKUPU,
                                       (27,28,29,36,37,38,45,46,47))},
             'previous': 30, 'pomoc': 39, 'next': 48},
    'skup_ilosc': {'item_to_be_purchased': 23,
                   'missing_purchase': tile_slots(3, 5),
                   'buy_dollar': tile_slots(3, 5),
                   'back_to_category': tile_slots(3, 7),
                   'remove_16': 27, 'remove_1': 28, 'add_1': 29, 'add_16': 30,
                   'set_1': 37, 'set_64': 38},
    'rynek': {**{f'category_{g[0]}': s for g, s in zip(KAT,
                                                   (27,28,29,30,36,37))},
              'saldo': 14, 'previous': 38, 'sorting': 39,
              'player_items_all': 45, 'sell': 46, 'next': 47, 'pomoc': 48},
    'moje_oferty': {'player_items_all': 27, 'player_items_active': 28,
                     'player_items_expired_not_taken': 29,
                     'player_items_purchased': 30, 'player_items_old': 36,
                     'home': 37, 'player_items_previous': 38, 'player_items_next': 39},
    'kupno': {'purchase_item': 23,
              'purchase_confirm': tile_slots(3, 0),
              'home': tile_slots(3, 2),
              'info': tile_slots(3, 5), 'saldo': 43},
    'wystawienie': {'sell_player_item': 23, 'info': 17,
                    'sell_day_1': 14, 'sell_day_3': 15, 'sell_day_7': 16,
                    'sell_confirm': tile_slots(3, 5),
                    'home': tile_slots(4, 7),
                    'zamknij': tile_slots(2, 7),
                    **{f'sell_{k}': s for k, s in zip(
                        ('7','8','9','4','5','6','1','2','3','dot','0','backspace'),
                        LEFT)}},
}
LIST_SCREENS = {'sklep', 'skup', 'rynek', 'moje_oferty'}


def title_prefix(index):
    # Wzorowane na tytule DeluxeMenus hub_ekonomia: bez tekstu po glifach.
    template = '&f䨈䨉䨊䨋'
    for old, new in zip('䨈䨉䨊䨋', (chr(0x4A60 + 4*index+n) for n in range(4))):
        template = template.replace(old, new)
    return template


BLANK_MODEL = 'nexo:mmo_icon_blank'


def blank_button_model(block):
    changed, count = re.subn(r'(^\s+item_model2?:\s*)[^\n]+',
                             lambda match: match.group(1) + BLANK_MODEL,
                             block, flags=re.M)
    assert count >= 1, block[:100]
    return changed


def segment_text(raw, start, end=None):
    p = re.search(start, raw, re.M)
    assert p, start
    q = re.search(end, raw[p.end():], re.M) if end else None
    stop = p.end() + q.start() if q else len(raw)
    return raw[:p.start()], raw[p.start():stop], raw[stop:]


def buttons_of(segment, indent):
    prefix = ' ' * indent
    head = re.search(r'^' + prefix + r'buttons:\s*$', segment, re.M)
    assert head
    tail = segment[head.end():]
    boundary = re.search(r'^' + prefix + r'\w[\w-]*:\s*$', tail, re.M)
    body = tail[:boundary.start()] if boundary else tail
    rest = tail[boundary.start():] if boundary else ''
    starts = list(re.finditer(r'^' + ' '*(indent+2) + r'([\w-]+):\s*$', body, re.M))
    assert starts
    blocks = {}
    for i, m in enumerate(starts):
        n = starts[i+1].start() if i+1 < len(starts) else len(body)
        blocks[m.group(1)] = body[m.start():n].rstrip('\n')
    return segment[:head.end()], blocks, rest


def read_slots(block):
    m = re.search(r'^\s+slots:\s*\[([^]]*)\]', block, re.M)
    assert m, block[:80]
    return [int(x) for x in re.findall(r'\d+', m.group(1))]


def put_slots(block, slots):
    val = ', '.join(map(str, slots))
    changed, count = re.subn(r'^(\s+slots:)\s*\[[^]]*\]',
                              lambda m: m.group(1)+' ['+val+']', block, count=1, flags=re.M)
    assert count == 1
    return changed


def rewrite_screen(raw, ident, index, marker, next_marker, indent):
    before, segment, after = segment_text(raw, marker, next_marker)
    pre, blocks, post = buttons_of(segment, indent)
    removed = REMOVED - ({'zamknij'} if ident == 'wystawienie' else set())
    original = {k: v for k, v in blocks.items() if k not in FILLERS | removed}
    mapping = MAPS[ident]
    assert set(mapping) == set(original), (ident, set(mapping)-set(original), set(original)-set(mapping))
    # Listy CoreTools trafiają do wolnych slotów. Ukryty rezerwujący przycisk
    # chroni emblemat i grzbiet przed ofertami/towarami; nie rysujemy tam gniazd.
    used = {s for pos in mapping.values() for s in (pos if isinstance(pos,list) else [pos])}
    free = RIGHT - used if ident in LIST_SCREENS else set()
    reserve = sorted(set(range(54)) - free - used)
    filler_names = [n for n in blocks if n in FILLERS]
    if not filler_names:
        filler_names = ['empty']
        blocks['empty'] = btn('empty', [0], model=M('handel_pusty'), hide=True)
        blocks['empty'] = '\n'.join(' '*(indent-2)+line for line in blocks['empty'].split('\n'))
    chunks = [reserve[i::len(filler_names)] for i in range(len(filler_names))]
    rewritten = []
    for name, block in blocks.items():
        if name in removed:
            continue
        if name in FILLERS:
            slots = chunks[filler_names.index(name)]
        else:
            pos = mapping[name]
            slots = pos if isinstance(pos,list) else [pos]
        moved = put_slots(block, slots)
        # Kafle 2x2 i emblemat mają ikonę wypaloną w teksturze.
        if name in FILLERS or isinstance(mapping.get(name), list):
            moved = blank_button_model(moved)
        if name in original:
            expected = put_slots(original[name], slots)
            if isinstance(mapping[name], list):
                expected = blank_button_model(expected)
            assert moved == expected, (ident, name)
        rewritten.append(moved)
    # Nowe filler pochodzą z szablonu `btn`, pozostałe pozycje są oryginalne.
    segment = pre + '\n' + '\n'.join(rewritten) + '\n' + post
    old_prefix = T
    new_prefix = title_prefix(index)
    segment = segment.replace(old_prefix, new_prefix)
    assert old_prefix not in segment
    # Podatki zachowujemy bajt w bajt jako panel administracyjny.
    result = before + segment + after
    return result, original, {k: read_slots(v) for k, v in zip(
        [n for n in blocks if n not in removed], rewritten)}, free, reserve


def split_rewrite(raw, file_ident):
    if file_ident in ('sklep', 'skup'):
        ids = (file_ident, file_ident+'_ilosc')
        raw, old_adv, slots_adv, free_adv, res_adv = rewrite_screen(
            raw, ids[1], SCREENS.index(ids[1]), r'^  advanced:\s*$',
            r'^  buttons:\s*$', 4)
        raw, old_main, slots_main, free_main, res_main = rewrite_screen(
            raw, ids[0], SCREENS.index(ids[0]), r'^  buttons:\s*$',
            r'^  categories:\s*$', 2)
        # Nagłówki kategorii też są nazwami okna głównego.
        raw = raw.replace(T, title_prefix(SCREENS.index(ids[0])))
        return raw, [(ids[0],old_main,slots_main,free_main,res_main),
                     (ids[1],old_adv,slots_adv,free_adv,res_adv)]
    specs = [
        ('wystawienie',r'^  sell:\s*$',r'^  tax_system:\s*$',4),
        ('kupno',r'^  purchase:\s*$',r'^  sell:\s*$',4),
        ('moje_oferty',r'^  player_items:\s*$',r'^  purchase:\s*$',4),
        ('rynek',r'^mmoitems_example:\s*$',r'^  player_items:\s*$',2),
    ]
    records = []
    for ident, marker, following, depth in specs:
        raw, old, slots, free, reserve = rewrite_screen(
            raw, ident, SCREENS.index(ident), marker, following, depth)
        records.append((ident,old,slots,free,reserve))
    # Nazwy kategorii mają ten sam obraz co lista główna. Podatki pozostają oryginalne.
    pre, tax, post = segment_text(raw, r'^  tax_system:\s*$', r'^  categories:\s*$')
    raw = pre.replace(T, title_prefix(SCREENS.index('rynek'))) + tax + post.replace(
        T, title_prefix(SCREENS.index('rynek')))
    return raw, records


def emblem_shop(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.R((x+5,y+11,x+46,y+34),'wood2','ink')
    W.R((x+8,y+14,x+43,y+32),'paper2','gold0')
    W.G([(x+3,y+11),(x+9,y+3),(x+42,y+3),(x+48,y+11)],'red1','ink')
    for xx in (10,18,26,34,42): W.R((x+xx-3,y+6,x+xx+2,y+11),'paper4','red0')
    W.R((x+18,y+17,x+33,y+31),'blue1','gold1')
    W.G([(x+25,y+17),(x+30,y+23),(x+25,y+29),(x+20,y+23)],'gold2','gold0')

def emblem_sell(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+3,y+15),(x+25,y+4),(x+48,y+15),(x+45,y+36),(x+5,y+36)],'paper2','gold0')
    W.R((x+10,y+14,x+41,y+31),'wood2','ink')
    for xx in (14,24,34):
        W.G([(x+xx,y+10),(x+xx+4,y+15),(x+xx,y+21),(x+xx-4,y+15)],'green1','green0')
    W.R((x+19,y+27,x+31,y+31),'gold1','gold0')

def emblem_shop_amount(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+8,y+8),(x+34,y+8),(x+40,y+35),(x+4,y+35)],'paper2','ink')
    W.L([(x+9,y+8),(x+33,y+8)],'gold1',3)
    W.G([(x+22,y+14),(x+27,y+18),(x+22,y+25),(x+17,y+18)],'gold2','gold0')
    for xx,yy in ((38,7),(42,14),(44,23)):
        W.E((x+xx-3,y+yy-3,x+xx+3,y+yy+3),'gold2','gold0')

def emblem_sell_amount(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.R((x+25,y+5,x+28,y+33),'gold1','ink')
    W.L([(x+7,y+12),(x+46,y+12)],'gold2',3)
    for xx in (13,40):
        W.L([(x+xx,y+12),(x+xx-5,y+26)],'gold0')
        W.L([(x+xx,y+12),(x+xx+5,y+26)],'gold0')
        W.G([(x+xx-8,y+26),(x+xx+8,y+26),(x+xx+5,y+31),(x+xx-5,y+31)],'paper2','ink')
    W.R((x+20,y+33,x+34,y+36),'wood2','ink')

def emblem_auction(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.R((x+5,y+31,x+47,y+36),'wood2','ink')
    W.R((x+10,y+26,x+41,y+31),'gold1','gold0')
    W.L([(x+16,y+26),(x+38,y+6)],'wood2',4)
    W.G([(x+30,y+3),(x+44,y+11),(x+39,y+18),(x+25,y+10)],'stone2','ink')
    W.L([(x+29,y+5),(x+40,y+11)],'paper4')
    W.star(x+15,y+12,'gold2')

def emblem_offers(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+9,y+3),(x+38,y+3),(x+42,y+7),(x+42,y+34),(x+10,y+34)],'paper2','ink')
    W.R((x+7,y+5,x+11,y+36),'wood2','ink')
    W.L([(x+16,y+11),(x+35,y+11)],'gold0',2)
    W.L([(x+16,y+18),(x+35,y+18)],'gold0',2)
    W.L([(x+16,y+25),(x+28,y+25)],'gold0',2)
    W.G([(x+34,y+22),(x+45,y+26),(x+39,y+37),(x+29,y+32)],'red1','ink')
    W.R((x+35,y+27,x+38,y+30),'gold2')

def emblem_purchase(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+5,y+28),(x+18,y+19),(x+30,y+22),(x+37,y+17),(x+46,y+21),
         (x+39,y+31),(x+20,y+35)],'paper1','ink')
    W.R((x+6,y+28,x+17,y+37),'red0','ink')
    W.E((x+21,y+5,x+39,y+23),'gold2','gold0',2)
    W.E((x+25,y+9,x+35,y+19),'gold1','gold0')
    W.R((x+29,y+11,x+31,y+17),'paper4')

def emblem_listing(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.R((x+5,y+31,x+46,y+36),'wood2','ink')
    W.R((x+12,y+26,x+39,y+31),'gold1','gold0')
    W.G([(x+16,y+12),(x+26,y+5),(x+37,y+12),(x+34,y+26),(x+18,y+26)],'paper2','ink')
    W.G([(x+26,y+10),(x+32,y+16),(x+26,y+22),(x+20,y+16)],'red1','gold0')
    W.L([(x+38,y+5),(x+44,y+5),(x+44,y+15)],'gold2',2)

def emblem_for(ident):
    return {'sklep':emblem_shop, 'sklep_ilosc':emblem_shop_amount,
            'skup':emblem_sell, 'skup_ilosc':emblem_sell_amount,
            'rynek':emblem_auction, 'moje_oferty':emblem_offers,
            'kupno':emblem_purchase, 'wystawienie':emblem_listing}[ident]

def miniature_help(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+11,y+7),(x+25,y+4),(x+40,y+8),(x+40,y+31),
         (x+26,y+27),(x+11,y+31)],'paper2','gold0')
    W.L([(x+26,y+7),(x+26,y+30)],'gold0',2)
    W.R((x+19,y+11,x+22,y+21),'gold1')
    W.R((x+19,y+24,x+22,y+26),'gold1')
    W.star(x+44,y+6,'gold2')

def miniature_confirm(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+7,y+20),(x+17,y+29),(x+43,y+7),(x+45,y+14),
         (x+17,y+35),(x+5,y+25)],'green1','gold2')
    W.L([(x+12,y+22),(x+18,y+28),(x+40,y+10)],'paper4',2)

def miniature_back(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.G([(x+8,y+19),(x+25,y+5),(x+25,y+13),(x+44,y+13),
         (x+44,y+27),(x+25,y+27),(x+25,y+34)],'gold1','ink')
    W.L([(x+21,y+19),(x+39,y+19)],'paper4',2)

def miniature_close(x,y):
    W.R((x,y,x+51,y+38),'blue0')
    W.E((x+11,y+4,x+41,y+34),'red0','gold2',2)
    W.L([(x+20,y+13),(x+32,y+25)],'paper4',3)
    W.L([(x+32,y+13),(x+20,y+25)],'paper4',3)

TILE_ART = {
    'sklep': {'pomoc':('Pomoc',miniature_help)},
    'sklep_ilosc': {'sell_dollar':('Kup',miniature_confirm),
                     'back_to_category':('Wstecz',miniature_back)},
    'skup_ilosc': {'buy_dollar':('Sprzedaj',miniature_confirm),
                   'back_to_category':('Wstecz',miniature_back)},
    'kupno': {'purchase_confirm':('Kup',miniature_confirm),
              'home':('Wstecz',miniature_back), 'info':('Pomoc',miniature_help)},
    'wystawienie': {'sell_confirm':('Wystaw',miniature_confirm),
                    'home':('Wstecz',miniature_back),
                    'zamknij':('Anuluj',miniature_close)},
}

def description(lines,x,y=146):
    W.R((x,y,x+137,y+100),'paper1','gold0')
    W.R((x+3,y+3,x+134,y+97),'paper4','gold0')
    W.gui_body(lines,x+69,y+10,124,84,frame=(x,y,x+137,y+100))
    S.foot_ornament(x+69,y+82)

def left_heading(label):
    # Emblemat zajmuje y=34..141, gniazda y=142..249; pas poniżej jest wolny.
    W.R((22,112,157,141),'gold0','leather0')
    W.R((24,114,155,139),'blue0','gold1')
    W.gui_text(label,89,120,'gold2',max_gui=61,frame=(22,112,157,141))

def bag_socket(x,y):
    W.R((x+2,y+2,x+33,y+33),'leather0','gold0')
    W.R((x+4,y+4,x+31,y+31),'blue0','gold1')
    W.L([(x+7,y+7),(x+28,y+7)],'blue1')
    W.L([(x+7,y+28),(x+28,y+28)],'gold0')

def draw_backpack():
    W.R((5,263,346,441),'leather0','gold0')
    W.R((9,275,342,388),'leather1','gold0')
    W.R((9,391,342,431),'leather1','gold0')
    W.R((136,262,216,276),'gold0','leather0')
    W.R((139,264,213,274),'blue0','gold1')
    W.bitmap_text('PLECAK',176,272,'gold2',max_width=68,frame=(136,262,216,276),margin=2)
    for y in (278,314,350,394):
        for c in range(9):
            bag_socket(14+36*c,y)


def draw_page(ident, title, slots, free):
    image = W.new_canvas()
    W.draw_book()
    W.draw_title(title)
    W.hero_frame(14,34,emblem_for(ident))
    headings = {'sklep':'Towary', 'sklep_ilosc':'Towar',
                'skup':'Towary skupu', 'skup_ilosc':'Decyzja',
                'rynek':'Oferty graczy', 'moje_oferty':'Twoje oferty',
                'kupno':'Oferta', 'wystawienie':'Oferta'}
    S.heading(headings[ident],194,330,43)
    real = {s for name, arr in slots.items() if name not in FILLERS for s in arr}
    sockets = sorted(real | free)
    left = [s for s in sockets if s % 9 < 4 and s not in EMBLEM]
    right = [s for s in sockets if s % 9 > 4]
    tiles = TILE_ART.get(ident,{})
    tile_slots_all = {s for name in tiles for s in slots[name]}
    # Zwarte, powtarzalne gniazda podążają wyłącznie za realnymi przedmiotami/listą.
    left_sockets = [s for s in left if s not in tile_slots_all]
    if len(left_sockets) >= 6:
        bottom = 216 if max(s//9 for s in left_sockets)==4 else 252
        S.group_frame(16,161,141,bottom,left_sockets)
    left_labels = {'sklep':'Kategorie', 'sklep_ilosc':'Kup lub wróć',
                   'skup':'Kategorie', 'skup_ilosc':'Wybierz ilość',
                   'rynek':'Kategorie', 'moje_oferty':'Zakładki ofert',
                   'kupno':'Akcje', 'wystawienie':'Wpisz cenę'}
    for s in (set(left+right)-tile_slots_all):
        S.socket(s)
    for name,(caption,painter) in tiles.items():
        first = slots[name][0]
        W.tile(14+36*(first%9),34+36*(first//9),caption,painter)
    if ident == 'sklep_ilosc':
        description(('Wybierz liczbę sztuk.', 'Sprawdź cenę.', 'Zatwierdź zakup.'),194)
    if ident == 'wystawienie':
        draw_backpack()
    else:
        W.draw_footer()
        W.draw_lower('WRÓĆ DO MENU')
    left_heading(left_labels[ident])
    W.clean_orphans()
    assert not W.find_orphans(image)
    return image


def validate_record(ident, old, new_slots, free, reserve):
    assert set(old) == set(new_slots)-set(FILLERS)
    assert all(0 <= s <= 53 for arr in new_slots.values() for s in arr)
    active = [(n,s) for n,arr in new_slots.items() if n not in FILLERS for s in arr]
    assert not any(s in SPINE for _,s in active)
    assert not any(s in EMBLEM for _,s in active)
    overlap = {s:[n for n,t in active if t==s] for _,s in active}
    overlap = {s:n for s,n in overlap.items() if len(n)>1}
    allowed_pair = {'sklep_ilosc':{'missing_purchase','sell_dollar'},
                    'skup_ilosc':{'missing_purchase','buy_dollar'}}.get(ident)
    assert all(set(names)==allowed_pair for names in overlap.values()) if overlap else True, (ident,overlap)
    assert len(overlap) == (4 if allowed_pair else 0), (ident,overlap)
    assert set(reserve).isdisjoint(set(free)|{s for _,s in active})
    assert len(free) == (20 if ident in LIST_SCREENS else 0) - (1 if ident=='rynek' else 0)
    return len(old), len(active), len(free), len(reserve), overlap


def validate_export(paths):
    title_count = 0
    button_count = 0
    specs = {
        'sklep': [('sklep', r'^  buttons:\s*$', r'^  categories:\s*$', 2),
                  ('sklep_ilosc', r'^  advanced:\s*$', r'^  buttons:\s*$', 4)],
        'skup': [('skup', r'^  buttons:\s*$', r'^  categories:\s*$', 2),
                 ('skup_ilosc', r'^  advanced:\s*$', r'^  buttons:\s*$', 4)],
        'rynek': [('rynek', r'^mmoitems_example:\s*$', r'^  player_items:\s*$', 2),
                  ('moje_oferty', r'^  player_items:\s*$', r'^  purchase:\s*$', 4),
                  ('kupno', r'^  purchase:\s*$', r'^  sell:\s*$', 4),
                  ('wystawienie', r'^  sell:\s*$', r'^  tax_system:\s*$', 4)],
    }
    for filename, path in paths.items():
        raw = path.read_text(encoding='utf-8')
        for line in raw.splitlines():
            match = re.match(r'^\s+name: ("&f.*")$', line)
            if not match:
                continue
            value = json.loads(match.group(1))
            plain = re.sub(r'&[0-9a-fk-or]', '', value, flags=re.I)
            plain = re.sub(r'[\uE000-\uF8FF\u4A60-\u4A7F]', '', plain)
            assert not plain, (path, value, plain)
            title_count += 1
        for ident, marker, following, indent in specs[filename]:
            _, segment, _ = segment_text(raw, marker, following)
            _, blocks, _ = buttons_of(segment, indent)
            tiles = {slot for pos in MAPS[ident].values() if isinstance(pos, list) for slot in pos}
            for name, block in blocks.items():
                if not set(read_slots(block)) & (tiles | EMBLEM):
                    continue
                assert re.search(r'^\s+material: paper\s*$', block, re.M), (ident, name)
                models = re.findall(r'^\s+item_model2?:\s*(\S+)', block, re.M)
                assert models and all(model == BLANK_MODEL for model in models), (ident, name, models)
                button_count += 1
    assert title_count >= 8 and button_count >= 8, (title_count, button_count)
    return title_count, button_count


def main_ksiega():
    out = Path(P)
    (out/'Shops').mkdir(parents=True,exist_ok=True)
    (out/'AuctionHouse').mkdir(parents=True,exist_ok=True)
    raw = {'sklep':sklep(), 'skup':skup(), 'rynek':rynek()}
    glyphs = ['# Cztery ćwiartki na ekran; U+4A60..U+4A7F.']
    records = []
    images = []
    paths = {}
    for filename in ('sklep','skup','rynek'):
        changed, rs = split_rewrite(raw[filename],filename)
        dest = out/('AuctionHouse' if filename=='rynek' else 'Shops')/(filename+'.yml')
        dest.write_text(changed,encoding='utf-8')
        paths[filename] = dest
        records.extend(rs)
    title_count, blank_count = validate_export(paths)
    for ident, old, slots, free, reserve in sorted(records,key=lambda x:SCREENS.index(x[0])):
        index = SCREENS.index(ident)
        result = validate_record(ident,old,slots,free,reserve)
        image = draw_page(ident,LABELS[index],slots,free)
        d = out/ident
        d.mkdir(exist_ok=True)
        image.save(d/(ident+'_2x.png'))
        for n,(x,y) in enumerate(((0,0),(176,0),(0,222),(176,222)),1):
            image.crop((x,y,x+176,y+222)).save(d/(ident+f'_{n}.png'))
            code = 0x4A60+4*index+n-1
            glyphs += [f'{ident}_{n}:','  font: minecraft:default',
                       f'  texture: nexo:gui/{ident}_{n}',
                       f'  ascent: {13 if n<=2 else -98}','  height: 111',
                       f'  char: "{chr(code)}"']
        images.append(image)
        (d/'sloty.txt').write_text('\n'.join(
            f'{s:02d} | ' + ('grzbiet (rezerwa techniczna)' if s in SPINE
            else ', '.join(n for n,arr in slots.items() if s in arr) or ('lista' if s in free else 'rezerwa'))
            for s in range(54))+'\n',encoding='utf-8')
    sheet = Image.new('RGB',(352*4,444*2),W.C['table0'])
    for i,img in enumerate(images): sheet.paste(img,((i%4)*352,(i//4)*444))
    sheet.save(out/'arkusz_x2.png')
    sheet.save(out.parent/'arkusz_x2.png')
    studies = Image.new('RGB',(960,630),W.C['table0'])
    icons = [(ident,emblem_for(ident)) for ident in SCREENS]
    icons += [('Pomoc',miniature_help),('Zatwierdź',miniature_confirm),
              ('Wstecz',miniature_back),('Anuluj',miniature_close)]
    for j,(caption,painter) in enumerate(icons):
        scene=Image.new('RGB',(52,39),W.C['blue0'])
        W.set_canvas(scene)
        painter(0,0)
        x,y=(j%4)*240,(j//4)*210
        studies.paste(scene.resize((208,156),Image.Resampling.NEAREST),(x+16,y+10))
        W.set_canvas(studies)
        W.bitmap_text(caption,x+120,y+190,'gold2',sx=2,sy=2,max_width=224,frame=(x,y+164,x+240,y+202))
    studies.save(out/'kafle_x4.png')
    studies.save(out.parent/'kafle_x4.png')
    (out/'glify_handel.yml').write_text('\n'.join(glyphs)+'\n',encoding='utf-8')
    podstrony = ''.join(
        f'U+{0x4A60+4*i:04X} hub_ekonomia  # {ident}\n'
        for i,ident in enumerate(SCREENS) if ident != 'wystawienie')
    (out/'podstrony.txt').write_text(podstrony,encoding='utf-8')
    (out.parent/'podstrony.txt').write_text(podstrony,encoding='utf-8')
    lines = []
    for ident,old,slots,free,reserve in sorted(records,key=lambda x:SCREENS.index(x[0])):
        n,act,listing,blocked,overlap=validate_record(ident,old,slots,free,reserve)
        lines.append(f'{ident}: stare funkcjonalne {n}, nowe {act}; lista {listing}; rezerwa {blocked}; kol4 aktywne 0; duplikaty {overlap or "brak"}')
    lines += [
        'Usunięte: wstecz (sklep, skup, rynek); zamknij (sklep, skup, skup_ilosc, rynek, moje_oferty, kupno).',
        'Zachowane: zamknij i home w wystawieniu, prawdziwy plecak; wystawienie poza podstrony.txt.',
        'Skup i ilość sprzedaży: klikają wybrany towar/kategorię CoreTools, a plugin sam pobiera pasujące sztuki z ekwipunku.',
        'Każdy zachowany blok przycisku porównano ze źródłem: zmieniono tylko slots, modele kafli i prefiks tytułu.',
        f'Tytuły: {title_count} bez tekstu po usunięciu koloru i glifów; modele niewidoczne: {blank_count} bloków na kaflach/emblemacie.',
        'Zmienne {page_no}/kategoria pominięto w lore: brak lokalnego potwierdzenia rozwijania ich w lore przycisku CoreTools.',
        'Komendy usunięte tylko z tych przycisków: handel_do_menu ×3; handel_zamknij ×6.',
        'Rezerwa techniczna CoreTools: niewidoczny przycisk na grzbiecie blokuje automatyczne listy.'
    ]
    (out/'kontrola.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    print('\n'.join(lines))

if __name__ == '__main__':
    main_ksiega()
