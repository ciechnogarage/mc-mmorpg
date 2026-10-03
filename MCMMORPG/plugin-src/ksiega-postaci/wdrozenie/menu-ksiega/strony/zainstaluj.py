#!/usr/bin/env python3
"""Instaluje strony Ksiegi do katalogu plugins (klon albo world1).

Uzycie: zainstaluj.py <katalog plugins> <grupa> [<grupa> ...]
Grupy: podstrony, siatka, poczta-dziennik, mmocore, chowance, handel.
Kopiuje tekstury cwiartek do Nexo/pack/assets/nexo/textures/gui, glify do Nexo/glyphs/ksiega_<grupa>.yml,
pliki menu do wlasciwych pluginow i dopisuje mapowanie powrotu do KsiegaPostaci/config.yml (podstrony.strony).
Niczego nie przeladowuje — po instalacji: restart serwera (glify Nexo + config pluginu).
"""
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent

# grupa -> (katalog z out, plik glifow, [(zrodlo wzgledem out, cel wzgledem plugins)])
GRUPY = {
    'podstrony': ('out', 'glify_strony.yml', [('dm/*.yml', 'DeluxeMenus/gui_menus/')]),
    'siatka': ('siatka/out', 'glify_siatka.yml', [('dm/*.yml', 'DeluxeMenus/gui_menus/')]),
    'poczta-dziennik': ('poczta-dziennik/out', 'glify_poczta_dziennik.yml',
                        [('dm/*.yml', 'DeluxeMenus/gui_menus/'), ('../cmi_dialogs/*.yml', 'CMI/Dialogs/'), ('../../../hub_glowne.yml', 'DeluxeMenus/gui_menus/')]),
    'mmocore': ('mmocore/out', 'glify_mmocore.yml', [('gui/*.yml', 'MMOCore/gui/')]),
    'chowance': ('chowance/out', 'glify_chowance.yml', [('gen/*.yml', 'DeluxeMenus/gui_menus/')]),
    'handel': ('handel/out/gen', 'glify_handel.yml',
               [('AuctionHouse/*.yml', 'CoreTools/AuctionHouse/'), ('Shops/*.yml', 'CoreTools/Shops/')]),
}

# Strony z grup podstrony/siatka nie maja podstrony.txt — mapowanie powrotu ustalone przy przegladzie.
POWROTY_STALE = {
    'podstrony': [(0x4A00 + 4 * i, 'hub_glowne', n) for i, n in enumerate(
        ['hub_postac', 'hub_umiejetnosci', 'hub_ekonomia', 'hub_spolecznosc', 'hub_ustawienia', 'hub_pomoc',
         'stolica_podroze'])],
    'siatka': [(0x4A40, 'hub_postac', 'wiara'), (0x4A44, 'hub_umiejetnosci', 'ksiega_zaklec'),
               (0x4A48, 'hub_glowne', 'profession_main')],
}


def powroty(grupa, out):
    if grupa in POWROTY_STALE:
        return POWROTY_STALE[grupa]
    wynik = []
    plik = out / 'podstrony.txt'
    for linia in plik.read_text(encoding='utf-8').splitlines():
        m = re.match(r'\s*U\+([0-9A-Fa-f]{4})\s+(\S+)\s*(?:#\s*(.*))?$', linia)
        if m:
            wynik.append((int(m.group(1), 16), m.group(2), (m.group(3) or '').strip()))
    return wynik


def dopisz_powroty(plugins, wpisy):
    cfg = plugins / 'KsiegaPostaci/config.yml'
    tekst = cfg.read_text(encoding='utf-8')
    if '\npodstrony:' not in tekst:
        blok = ("podstrony:\n  wstecz-nazwa: '&eWróć do Księgi'\n  wstecz-lore: ['&7Wróć do poprzedniej strony.']\n"
                "  # Klucz to pierwszy glif tytulu podstrony; wartosc to menu otwierane po kliknieciu 3-5.\n  strony:\n")
        i = tekst.index('typy-broni:')
        tekst = tekst[:i] + blok + tekst[i:]
    for kod, menu, opis in wpisy:
        znak = chr(kod)
        linia = f'    "{znak}": {menu} # {opis}\n'
        if f'"{znak}":' in tekst:
            tekst = re.sub(rf'    "{znak}":[^\n]*\n', linia, tekst)
        else:
            i = tekst.index('typy-broni:')
            tekst = tekst[:i] + linia + tekst[i:]
    cfg.write_text(tekst, encoding='utf-8')


def zarejestruj_menu(plugins, nazwy):
    """DeluxeMenus wczytuje tylko menu wpisane w config.yml (gui_menus: <id>: file: <plik>)."""
    cfg = plugins / 'DeluxeMenus/config.yml'
    tekst = cfg.read_text(encoding='utf-8')
    dodane = []
    for nazwa in nazwy:
        if re.search(rf'(?m)^\s+file:\s*{re.escape(nazwa)}\.yml\s*$', tekst):
            continue
        i = tekst.index('gui_menus:\n') + len('gui_menus:\n')
        tekst = tekst[:i] + f'  {nazwa}:\n    file: {nazwa}.yml\n' + tekst[i:]
        dodane.append(nazwa)
    cfg.write_text(tekst, encoding='utf-8')
    return dodane


def main():
    plugins = Path(sys.argv[1])
    assert (plugins / 'Nexo').is_dir() and (plugins / 'KsiegaPostaci').is_dir(), plugins
    for grupa in sys.argv[2:]:
        katalog, glify, pliki = GRUPY[grupa]
        out = ROOT / katalog
        n = 0
        for png in out.rglob('*_[1-4].png'):
            shutil.copy2(png, plugins / 'Nexo/pack/assets/nexo/textures/gui' / png.name)
            n += 1
        if glify:
            shutil.copy2(out / glify, plugins / 'Nexo/glyphs' / f'ksiega_{grupa}.yml')
        for wzor, cel in pliki:
            for plik in sorted(out.glob(wzor)):
                shutil.copy2(plik, plugins / cel / plik.name)
                n += 1
                if cel == 'DeluxeMenus/gui_menus/':
                    nowe = zarejestruj_menu(plugins, [plik.stem])
                    if nowe:
                        print('  zarejestrowano w DeluxeMenus:', *nowe)
        wpisy = powroty(grupa, out)
        dopisz_powroty(plugins, wpisy)
        print(f'{grupa}: {n} plikow, {len(wpisy)} powrotow')


if __name__ == '__main__':
    main()
