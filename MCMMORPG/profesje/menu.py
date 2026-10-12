# -*- coding: utf-8 -*-
"""DeluxeMenus profesji v2: profession_main (przepisany na v2), warsztat_<prof> (10 tierow),
zlecenia_<prof> (3 dzienne + startowe), okna mistrzostw (pakiet 23.09). Ten sam glif tla i gniazda co
profession_main (styl Ksiegi, strony/siatka) — kafle tylko na gniazdach 1x1. Nowe menu sa rejestrowane
w DeluxeMenus/config.yml (inaczej 'Could not find and open menu')."""
import os
import re

import yaml

import tresc as T
from model import PROF_PL, MISTRZ_PL, RZEMIOSLO, ZBIERACKIE

GNIAZDA = [23, 24, 25, 32, 33, 34, 41, 42, 43, 37, 38, 46, 47]   # gniazda 1x1 rysunku profession_main
EMBLEMAT = [0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21]
ROLA = {
 'mining': ['Rudy i kryształy dla kowala,', 'jubilera, zaklinacza i budowniczego.'],
 'woodworking': ['Drewno; deski, trzonki, siekiery,', 'wędki i broń drewniana.'],
 'herbalism': ['Zioła dla alchemika i kucharza,', 'przysmaki Chowańców.'],
 'farming': ['Plony i włókna dla kucharza', 'i krawca.'],
 'fishing': ['Ryby i dary wody dla kucharza,', 'alchemika i jubilera.'],
 'hunting': ['Skóry, kości i trofea z oprawiania', 'zwłok bestii.'],
 'smithing': ['Sztabki, narzędzia, broń i zbroja', 'metalowa, zestawy naprawcze.'],
 'tailoring': ['Tkaniny, skóry, cięciwy, zbroja', 'lekka i tkana, opatrunki.'],
 'jewelcrafting': ['Ozdoby i klejnoty do gniazd:', 'zdrowie, ochrona, odnowa, celność.'],
 'alchemy': ['Mikstury, eliksiry i oleje', 'do broni.'],
 'enchanting': ['Runy do gniazd, grimuary, totemy,', 'pieczęcie Chowańców.'],
 'cooking': ['Posiłki przed wyprawą i uczty', 'dla całej drużyny.'],
 'building': ['Obozowiska i zestawy przepraw', 'otwierające skróty w lochach.'],
}
NARZ_PL = {'kilof': 'kilof', 'siekiera': 'siekiera', 'sierp': 'sierp', 'motyka': 'motyka', 'wedka': 'wędka', 'noz': 'nóż'}
ZRODLO_PL = {'mining': 'złoża skalne', 'woodworking': 'powalone pnie', 'herbalism': 'kępy ziół',
             'farming': 'grządki i łany', 'fishing': 'woda (łowiska tierów II+)', 'hunting': 'zwłoki bestii (PPM nożem)'}


def nexo_id(iid):
    return 'nexo-prof_' + iid.lower().replace('prof_', '', 1)


class Menu:
    def __init__(self, g):
        self.g = g
        self.m = g.m
        self.D = g.P + '/DeluxeMenus/gui_menus'

    def glif(self):
        s = open(f'{self.D}/profession_main.yml', encoding='utf-8').read()
        return re.search(r"^menu_title: '(.*)'", s, re.M).group(1)

    def tekst(self, naglowek, d):
        return naglowek + yaml.safe_dump(d, allow_unicode=True, sort_keys=False, width=200)

    # ---------------------------------------------------- profession_main
    def glowne(self):
        stary = yaml.safe_load(open(f'{self.D}/profession_main.yml', encoding='utf-8'))
        items = {}
        for p, it in stary['items'].items():
            if p not in PROF_PL:
                continue
            lore = [f'&7Poziom: &f%mmocore_profession_{p}%&7/100',
                    f'&7Postęp poziomu: &f%mmocore_profession_percent_{p}%%',
                    f'%mistrz_opis_{p}%', ''] + ['&7' + x for x in ROLA[p]]
            if p in ZBIERACKIE:
                lore += [f'&7Narzędzie: &f{NARZ_PL[T.PROF_ZBIERACKIE[p][0]]} &8(w plecaku)', f'&7Źródła: &f{ZRODLO_PL[p]}', '',
                         '&eLPM: &fzlecenia i pierwsze kroki']
                lewy = [f'[openguimenu] zlecenia_{p}']
            else:
                lore += ['', '&eLPM: &fwarsztaty i zlecenia']
                lewy = [f'[openguimenu] warsztat_{p}']
            if p not in ('cooking', 'building'):
                lore.append('&ePPM: &fmistrzostwo (od poziomu 50)')
            items[p] = {'material': it['material'], 'slot': it['slot'], 'item_flags': ['HIDE_ATTRIBUTES'],
                        'display_name': f'%mistrz_znak_{p}% &6&l{PROF_PL[p]}', 'lore': lore,
                        'left_click_commands': lewy,
                        'right_click_commands': [f'[console] mistrzostwo okno %player_name% {p}']}
        items['zasady'] = {
            'material': 'nexo-mmo_icon_blank', 'slots': EMBLEMAT, 'display_name': '&e&lJak działają profesje',
            'lore': ['', '&7Każda postać rozwija &fwszystkie 13&7 profesji', '&7swobodnie do poziomu &f50&7.',
                     '&7Od &f50&7 do &f100&7 rosną tylko &f2 mistrzostwa', '&7(PPM na profesji) oraz gotowanie',
                     '&7i budownictwo — te są ogólne.', '',
                     '&7Co &f10&7 poziomów nowy tier: nowa kraina,', '&7nowe surowce i narzędzia, receptury co 2 poziomy.',
                     '&7Co 10 poziomów premia: więcej urobku', '&7albo zwrot składnika przy rzemiośle.', '',
                     '&7Zbieranie: &fPPM w złoże, pień lub roślinę', '&7z narzędziem w plecaku.',
                     '&7Najlepsze receptury tieru wymagają', '&7rzadkich składników z lochów.']}
        d = {'menu_title': stary['menu_title'], 'open_command': stary.get('open_command', 'profesje'),
             'size': stary.get('size', 54), 'update_interval': 20, 'items': items}
        self.g_pisz('profession_main', '# PLIK GENEROWANY: MCMMORPG/profesje/menu.py (profesje v2; uklad gniazd z strony/siatka).\n', d)

    # ---------------------------------------------------- warsztat_<prof>
    def warsztat(self, p):
        items = {}
        for t in range(1, 11):
            st = self.m.stacja(p, t)
            rec = [r for r in self.m.rec if r['st'] == st]
            klucz_ikony = {'smithing': 'SZTABKA', 'woodworking': 'DESKA', 'tailoring': 'TKANINA', 'jewelcrafting': 'KAMIEN_O',
                           'alchemy': 'EKSTRAKT', 'enchanting': 'PYL_Z', 'cooking': None, 'building': 'KAMIEN_C'}[p]
            ikona = nexo_id(self.m.polprodukt(klucz_ikony, t) if klucz_ikony else self.m.produkt('B', 'POSILEK_SYTY', t))
            start = T.poziom_startu(t)
            sprzet = sum(1 for r in rec if r['out'].startswith('RZM_'))
            lore = [f'&7Poziomy: &f{start}–{10 * t}', f'&7Kraina: &f{T.KRAINY[t][0]}', f'&7Receptur: &f{len(rec)}'
                    + (f' &8(w tym sprzęt: {sprzet})' if sprzet else ''), '', '&eKliknij: otwórz warsztat']
            items[f'tier_{t}'] = {'material': ikona, 'slot': GNIAZDA[t - 1], 'priority': 1,
                                  'display_name': f'&6&lWarsztat {MISTRZ_PL[p]} {T.ROMAN[t]}', 'lore': lore,
                                  'view_requirement': {'requirements': {'poziom': {
                                      'type': '>=', 'input': f'%mmocore_profession_{p}%', 'output': str(start)}}},
                                  'left_click_commands': [f'[console] mi stations open {st} %player_name%']}
            items[f'tier_{t}_zamk'] = {'material': 'nexo-mmo_icon_blank', 'slot': GNIAZDA[t - 1], 'priority': 2,
                                       'display_name': f'&8Warsztat {MISTRZ_PL[p]} {T.ROMAN[t]}',
                                       'lore': [f'&cOd poziomu {start}', f'&7Kraina: &f{T.KRAINY[t][0]}']}
        items['zlecenia'] = {'material': 'WRITABLE_BOOK', 'slot': GNIAZDA[10], 'display_name': '&e&lZlecenia dnia',
                             'lore': ['&7Trzy zlecenia z twojego tieru.', '&7Nagroda: XP profesji i pieniądze.'],
                             'left_click_commands': [f'[openguimenu] zlecenia_{p}']}
        if p not in ('cooking', 'building'):
            items['mistrz'] = {'material': 'NETHER_STAR', 'slot': GNIAZDA[11], 'display_name': '&6&lMistrzostwo',
                               'lore': [f'%mistrz_opis_{p}%', f'%mistrz_akcja_{p}%'],
                               'left_click_commands': [f'[console] mistrzostwo okno %player_name% {p}']}
        items['wstecz'] = {'material': 'ARROW', 'slot': GNIAZDA[12], 'display_name': '&f« Profesje',
                           'left_click_commands': ['[openguimenu] profession_main']}
        items['info'] = {'material': 'nexo-mmo_icon_blank', 'slots': EMBLEMAT, 'display_name': f'&e&l{PROF_PL[p]}',
                         'lore': [f'&7Poziom: &f%mmocore_profession_{p}%&7/100', f'%mistrz_opis_{p}%', '']
                         + ['&7' + x for x in ROLA[p]] + ['', '&7Każdy tier to nowa kraina i jej surowce.',
                                                         '&7Receptury co 2 poziomy; ostatnie w tierze',
                                                         '&7wymagają rzadkich składników z lochów.']}
        d = {'menu_title': self.glif(), 'size': 54, 'update_interval': 20, 'items': items}
        self.g_pisz(f'warsztat_{p}', f'# PLIK GENEROWANY: MCMMORPG/profesje/menu.py — warsztaty {PROF_PL[p]} (10 tierow).\n', d)

    # ---------------------------------------------------- zlecenia_<prof>
    def zlecenia(self, p):
        items = {}
        for n in range(1, T.ZLECENIA['dziennie'] + 1):
            z = f'%rzemioslo_zlecenie_{p}_{n}'
            items[f'zlecenie_{n}'] = {
                'material': 'PAPER', 'slot': GNIAZDA[n - 1], 'display_name': f'{z}_tytul%',
                'lore': [f'{z}_opis%', f'&7Masz: &f{z}_masz%&7/&f{z}_ilosc%', f'&7Nagroda: &a{z}_xp% XP &7i &e{z}_kasa%',
                         '', f'{z}_akcja%'],
                'left_click_commands': [f'[console] rzemioslo zlecenie oddaj %player_name% {p} {n}', '[refresh]']}
        if p in ZBIERACKIE:
            s = f'%rzemioslo_start_{p}'
            items['start'] = {'material': 'WRITABLE_BOOK', 'slot': GNIAZDA[3], 'display_name': '&e&lPierwsze kroki',
                              'lore': [f'{s}_opis%', f'{s}_postep%', '', f'{s}_akcja%'],
                              'left_click_commands': [f'[console] rzemioslo start %player_name% {p}', '[refresh]']}
        items['wstecz'] = {'material': 'ARROW', 'slot': GNIAZDA[12], 'display_name': '&f« Wróć',
                           'left_click_commands': ['[openguimenu] ' + (f'warsztat_{p}' if p in RZEMIOSLO else 'profession_main')]}
        items['info'] = {'material': 'nexo-mmo_icon_blank', 'slots': EMBLEMAT, 'display_name': f'&e&lZlecenia: {PROF_PL[p]}',
                         'lore': ['', '&7Trzy zlecenia dziennie z twojego', '&7bieżącego tieru. Odnawiają się o północy.',
                                  '', '&7Kliknij zlecenie, gdy masz przedmioty', '&7w plecaku — oddasz je od razu.',
                                  '&7Nagroda: XP profesji i pieniądze.']}
        d = {'menu_title': self.glif(), 'size': 54, 'update_interval': 20, 'items': items}
        self.g_pisz(f'zlecenia_{p}', f'# PLIK GENEROWANY: MCMMORPG/profesje/menu.py — zlecenia {PROF_PL[p]}.\n', d)

    def g_pisz(self, nazwa, naglowek, d):
        self.g_pisane.append(nazwa)
        from generuj import pisz
        pisz(f'{self.D}/{nazwa}.yml', self.tekst(naglowek, d), self.g.raport)

    def okna_mistrzostw(self):
        pak = self.g.a.mistrzostwa
        if not pak:
            return
        for f in ('mistrz_wybor', 'mistrz_zmiana', 'mistrz_zmiana_potw'):
            src = os.path.join(pak, 'plugins/DeluxeMenus/gui_menus', f + '.yml')
            if os.path.exists(src):
                from generuj import pisz
                pisz(f'{self.D}/{f}.yml', open(src, encoding='utf-8').read(), self.g.raport)
                self.g_pisane.append(f)

    def rejestracja(self):
        f = self.g.P + '/DeluxeMenus/config.yml'
        s = open(f, encoding='utf-8').read()
        kotwica = '  profession_main:\n    file: profession_main.yml\n'
        assert kotwica in s, 'brak wpisu profession_main w DeluxeMenus/config.yml'
        dodatki = ''
        for m in self.g_pisane:
            if m == 'profession_main':
                continue
            if f'  {m}:\n' not in s:
                dodatki += f'  {m}:\n    file: {m}.yml\n'
        if dodatki:
            s = s.replace(kotwica, kotwica + '  # PROFESJE v2 (MCMMORPG/profesje/menu.py)\n' + dodatki, 1)
            from generuj import pisz
            pisz(f, s, self.g.raport)

    def pisz(self):
        self.g_pisane = []
        self.glowne()
        for p in RZEMIOSLO:
            self.warsztat(p)
        for p in list(PROF_PL):
            self.zlecenia(p)
        self.okna_mistrzostw()
        self.rejestracja()
