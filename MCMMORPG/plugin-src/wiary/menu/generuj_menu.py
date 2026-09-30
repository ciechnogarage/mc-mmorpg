#!/usr/bin/env python3
"""Generuje menu DeluxeMenus 'wiara' (27 slotów, tło huba) z configu pluginu Wiary.

Uklad: slot 4 = stan postaci; 10-12 = Trzej Starsi; 14-16 = trzy bostwa rasy (kazdy slot ma 4 warianty,
po jednym na rase, pokazywany ten z %wiary_dostepny_<id>% == tak); 22 = modlitwa (3 warianty), 18 wstecz, 26 zamknij.
Bez [close] przed komendami (memory deluxemenus-pulapki); komendy jako konsola, bo /wiary ma permisje wiary.admin.

Uzycie: python3 generuj_menu.py <config.yml pluginu> <wyjscie wiara.yml>
"""
import sys

import yaml

MATERIAL = {
    'ordhun': 'ROOTED_DIRT', 'nysara': 'STRING', 'vharun': 'WITHER_ROSE',
    'solenna': 'LANTERN', 'valdris': 'SHIELD', 'heldra': 'WHEAT',
    'aelyra': 'SEA_LANTERN', 'sylvaen': 'FEATHER', 'iridel': 'ENCHANTED_BOOK',
    'gorrath': 'IRON_AXE', 'kharzuk': 'LIGHTNING_ROD', 'mogra': 'BONE',
    'morvhena': 'SKELETON_SKULL', 'vhorax': 'ECHO_SHARD', 'ossyra': 'WHITE_CANDLE',
}
RASY = ['czlowiek', 'elf', 'ork', 'nieumarly']


def q(s):
    return "'" + str(s).replace("'", "''") + "'"


def bostwo(bid, b, slot, priorytet):
    kolor = b['kolor']
    out = [
        f"  'b_{bid}':",
        f"    material: {MATERIAL[bid]}",
        f"    slot: {slot}",
        f"    priority: {priorytet}",
        "    update: true",
        "    item_flags:",
        "      - HIDE_ATTRIBUTES",
        f"    display_name: {q(kolor + '&l' + b['imie'])}",
        "    lore:",
        f"      - {q('&7' + b['tytul'])}",
        f"      - {q('&8' + b['domena'])}",
        "      - ''",
        f"      - {q('&fŁaska przy Wierze %wiary_wiara%:')}",
        f"      - {q('&7' + '%wiary_opis_' + bid + '%')}",
        "      - ''",
        f"      - {q('%wiary_status_' + bid + '%')}",
        "    view_requirement:",
        "      requirements:",
        "        dostepny:",
        "          type: string equals",
        f"          input: '%wiary_dostepny_{bid}%'",
        "          output: 'tak'",
        "    left_click_commands:",
        f"      - '[console] wiary wybierz %player_name% {bid}'",
        "      - '[refresh]'",
    ]
    return out


def main(cfg_path, out_path):
    cfg = yaml.safe_load(open(cfg_path, encoding='utf-8'))
    bostwa = cfg['bostwa']
    starsi = [k for k, v in bostwa.items() if v['grupa'] == 'starsi']
    assert len(starsi) == 3, starsi
    for r in RASY:
        n = [k for k, v in bostwa.items() if v['grupa'] == r]
        assert len(n) == 3, (r, n)
    assert set(MATERIAL) == set(bostwa), set(MATERIAL) ^ set(bostwa)

    L = [
        "# WYGENEROWANE przez plugin-src/wiary/menu/generuj_menu.py — nie edytować ręcznie, tylko generator.",
        "# Menu Wiary: patron postaci (3 Starszych + 3 bóstwa rasy) i modlitwa o łaskę.",
        "# Wejście: hub pod Q -> Postać -> Wiara (hub_postac, slot 14).",
        "menu_title: '&f䄐&fWiara'",
        "size: 27",
        "update_interval: 20",
        "items:",
        "",
        "  'stan':",
        "    material: nexo-hub_wiara",
        "    slot: 4",
        "    update: true",
        "    display_name: '&6&lTwoja wiara'",
        "    lore:",
        "      - ''",
        "      - '&7Patron: %wiary_patron_imie%'",
        "      - '&7Rasa: &f%wiary_rasa_nazwa%'",
        "      - '&7Wiara (atrybut): &f%wiary_wiara%'",
        "      - '&7Łaska: %wiary_laska%'",
        "      - '&7Pozostało: &f%wiary_laska_czas%'",
        "      - ''",
        "      - '&8Im więcej Wiary, tym silniejsza łaska.'",
        "      - '&8Punkty Wiary rozdasz w oknie Atrybuty.'",
        "",
    ]
    for i, bid in enumerate(starsi):
        L += bostwo(bid, bostwa[bid], 10 + i, 1) + [""]
    # Kazda rasa ma WLASNY priorytet w slotach 14-16: przy rownym priorytecie DeluxeMenus sprawdza tylko
    # jeden przedmiot na slot (zmierzone na klonie 30.09 — sloty rasowe byly puste).
    for ri, r in enumerate(RASY):
        rasowe = [k for k, v in bostwa.items() if v['grupa'] == r]
        for i, bid in enumerate(rasowe):
            L += bostwo(bid, bostwa[bid], 14 + i, 1 + ri) + [""]

    L += [
        "  'modlitwa':",
        "    material: nexo-hub_wiara",
        "    slot: 22",
        "    priority: 0",
        "    update: true",
        "    display_name: '&6&lMódl się'",
        "    lore:",
        "      - ''",
        "      - '&7Łaska patrona przy Wierze %wiary_wiara%:'",
        "      - '&f%wiary_opis_patrona%'",
        "      - ''",
        "      - '&eKliknij, aby się pomodlić.'",
        "    view_requirement:",
        "      requirements:",
        "        mozna:",
        "          type: string equals",
        "          input: '%wiary_modlitwa%'",
        "          output: 'tak'",
        "    left_click_commands:",
        "      - '[console] wiary modlitwa %player_name%'",
        "      - '[refresh]'",
        "",
        "  'modlitwa_trwa':",
        "    material: nexo-hub_wiara",
        "    slot: 22",
        "    priority: 1",
        "    update: true",
        "    display_name: '&7Łaska trwa'",
        "    lore:",
        "      - ''",
        "      - '%wiary_laska%'",
        "      - '&7Pozostało: &f%wiary_laska_czas%'",
        "      - ''",
        "      - '&8Kolejną modlitwę odmówisz, gdy łaska minie.'",
        "    view_requirement:",
        "      requirements:",
        "        trwa:",
        "          type: '!string equals'",
        "          input: '%wiary_laska_czas%'",
        "          output: 'brak'",
        "",
        "  'modlitwa_brak_patrona':",
        "    material: GRAY_DYE",
        "    slot: 22",
        "    priority: 2",
        "    display_name: '&7Módl się'",
        "    lore:",
        "      - ''",
        "      - '&7Najpierw obierz patrona:'",
        "      - '&7kliknij jedno z bóstw powyżej.'",
        "",
        "  'wstecz':",
        "    material: nexo-hub_wstecz",
        "    slot: 18",
        "    display_name: '&7Wstecz'",
        "    left_click_commands:",
        "      - '[openguimenu] hub_postac'",
        "",
        "  'zamknij':",
        "    material: nexo-hub_zamknij",
        "    slot: 26",
        "    display_name: '&c&lZamknij'",
        "    left_click_commands:",
        "      - '[close]'",
    ]
    open(out_path, 'w', encoding='utf-8').write("\n".join(L) + "\n")
    print(f"menu: {out_path}, bóstw: {len(bostwa)}")


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
