#!/usr/bin/env python3
"""Okno "Mapa swiata" pod Q: panel PNG (glif Nexo) + menu DeluxeMenus + testowy pack klienta.

okno.py <render dir> <znaczniki.json> <wyjscie dir>

Uklad: okno skrzyni 6 rzedow (176x222). Obraz terenu wypelnia siatke slotow 9x6 (162x108 px,
od (7,17)); 1 slot = KOM x KOM blokow. Znaczniki to banery w slotach (kolory jak banery na
mapie w rece). "Tu jestes" = kompas w slocie gracza, z warunkow na %player_x%/%player_z%.
Tytul: '&f' + E104(-8) + glif + E108 E106 E104 E101 (-169) + tekst (pamiec gui-panel-glif-w-tytule;
'&f' NA POCZATKU, inaczej glif ciemnieje x4 - glif-panelu-tint-tytulu).
"""
import sys, os, json
from collections import Counter
from PIL import Image, ImageDraw
from paleta import rgb

PANEL_WZOR = "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/gui/chw_panel_6.png"
ZNAK = "\u4180"                       # wolny (sprawdzone z Nexo/glyphs 2026-10-01)
KOM = 148                             # blokow na slot (= 148/18 ~ 8,2 bloku na piksel)
X0, Z0 = -422, -300                   # lewy gorny rog siatki 9x6 -> x -422..910, z -300..588
GLIF = "mapa_swiata_bg"
MENU = "mapa_swiata"
SWIAT = "world"
BANER = {"white": "WHITE_BANNER", "red": "RED_BANNER", "lime": "LIME_BANNER"}
KOLOR_TXT = {"white": "&f", "red": "&c", "lime": "&a"}
MGLA = (14, 20, 28)                   # niewygenerowane = ciemnosc panelu ("nieznane")

def teren(render, w_px, h_px):
    meta = json.load(open(os.path.join(render, "teren_1x.json")))
    src = open(os.path.join(render, "teren_1x.bin"), "rb").read()
    w, h, x0, z0 = meta["w"], meta["h"], meta["x0"], meta["z0"]
    img = Image.new("RGB", (w_px, h_px), MGLA)
    px = img.load()
    bpp = KOM * 9 / w_px
    for py in range(h_px):
        for pxx in range(w_px):
            c = Counter()
            xa, xb = int(X0 + pxx * bpp), int(X0 + (pxx + 1) * bpp)
            za, zb = int(Z0 + py * bpp), int(Z0 + (py + 1) * bpp)
            for z in range(za, zb, 2):
                if not 0 <= z - z0 < h: continue
                for x in range(xa, xb, 2):
                    if 0 <= x - x0 < w: c[src[(z - z0) * w + x - x0]] += 1
            c.pop(0, None)
            if c: px[pxx, py] = rgb(c.most_common(1)[0][0])
    return img

def panel(render):
    p = Image.open(PANEL_WZOR).convert("RGBA")
    mapa = teren(render, 162, 108)
    d = ImageDraw.Draw(p)
    d.rectangle([5, 15, 170, 127], fill=(10, 14, 20, 255))            # ciemna ramka
    d.rectangle([6, 16, 169, 126], outline=(176, 141, 76, 255))        # zlota linia jak we wzorze
    p.paste(mapa, (7, 17))
    return p

def slot(x, z):
    c, r = (x - X0) // KOM, (z - Z0) // KOM
    if not (0 <= c < 9 and 0 <= r < 6): return None
    return r * 9 + c

def q(s): return "'" + s.replace("'", "''") + "'"

def menu(znaczniki):
    L = ["# WYGENEROWANE przez tools/mapa/okno.py (repo MC) - poprawiac TAM.",
         "# Mapa swiata pod Q (Przygoda -> Mapa swiata). Tlo = glif Nexo '%s' (obraz terenu)." % GLIF,
         "menu_title: '&f\ue104%s\ue108\ue106\ue104\ue101&fMapa świata'" % ZNAK,
         "size: 54", "update_interval: 1", "items:"]
    zajete = set()
    for i, z in enumerate(znaczniki):
        s = slot(z["x"], z["z"])
        if s is None or s in zajete: raise SystemExit("znacznik poza siatka albo kolizja: %s" % z["nazwa"])
        zajete.add(s)
        # ikona okna: model z packa (item_model dziala bez pluginu Nexo); na mapie w rece zostaja banery
        mat = ["    material: PAPER", "    item_model: '%s'" % z["ikona"]] if z.get("ikona") else ["    material: %s" % BANER[z["kolor"]]]
        L += ["  'z_%d':" % i] + mat + ["    slot: %d" % s, "    priority: 2",
              "    display_name: %s" % q(KOLOR_TXT[z["kolor"]] + "&l" + z["nazwa"]), "    lore:", "      - ''"]
        L += ["      - %s" % q("&7" + o) for o in z.get("opis", [])]
        L += ["      - ''", "      - %s" % q("&8x %d, z %d" % (z["x"], z["z"])), "    item_flags: [HIDE_ATTRIBUTES, HIDE_ADDITIONAL_TOOLTIP]"]
    # przyciski w rogach, ktore leza poza wygenerowanym swiatem (ciemne pola); priority 0 = nad wszystkim
    for nazwa, sl, model, txt, kom in (("wstecz", 45, "nexo:hub_wstecz", "&7Wstecz", "[openguimenu] hub_przygoda"),
                                       ("zamknij", 53, "nexo:hub_zamknij", "&cZamknij", "[close]")):
        if sl in zajete: raise SystemExit("przycisk %s na znaczniku" % nazwa)
        L += ["  '%s':" % nazwa, "    material: PAPER", "    item_model: '%s'" % model, "    slot: %d" % sl,
              "    priority: 0", "    display_name: '%s'" % txt, "    left_click_commands:", "      - '%s'" % kom]
    # "Tu jestes": jeden wpis na slot, widoczny tylko gdy gracz stoi w jego kwadracie swiata `world`.
    for s in range(54):
        c, r = s % 9, s // 9
        xa, za = X0 + c * KOM, Z0 + r * KOM
        L += ["  'gracz_%d':" % s, "    material: COMPASS", "    slot: %d" % s, "    priority: 1", "    update: true",
              "    display_name: '&e&lTu jesteś'", "    lore:", "      - ''", "      - '&7x %player_x%, z %player_z%'",
              "    view_requirement:", "      requirements:",
              "        swiat: {type: string equals, input: '%player_world%', output: '" + SWIAT + "'}",
              "        x1: {type: '>=', input: '%player_x%', output: '" + str(xa) + "'}",
              "        x2: {type: '<', input: '%player_x%', output: '" + str(xa + KOM) + "'}",
              "        z1: {type: '>=', input: '%player_z%', output: '" + str(za) + "'}",
              "        z2: {type: '<', input: '%player_z%', output: '" + str(za + KOM) + "'}"]
    return "\n".join(L) + "\n", zajete

def main(render, znaczniki_plik, wyj):
    znaczniki = json.load(open(znaczniki_plik))
    os.makedirs(wyj, exist_ok=True)
    tex = os.path.join(wyj, "Nexo/pack/assets/nexo/textures/gui"); os.makedirs(tex, exist_ok=True)
    p = panel(render); p.save(os.path.join(tex, GLIF + ".png"))
    p.convert("RGB").resize((176 * 3, 222 * 3), Image.NEAREST).save(os.path.join(wyj, "podglad_okna.png"))
    os.makedirs(os.path.join(wyj, "Nexo/glyphs"), exist_ok=True)
    open(os.path.join(wyj, "Nexo/glyphs/mapa.yml"), "w", encoding="utf8").write(
        "# WYGENEROWANE przez tools/mapa/okno.py. Tlo okna 'Mapa swiata' (6 rzedow, 176x222).\n"
        "# font minecraft:default OBOWIAZKOWY (tytul okna), ascent 13 / height 222 jak chw_panel_6.\n"
        "%s:\n  font: minecraft:default\n  texture: nexo:gui/%s\n  ascent: 13\n  height: 222\n  char: %s\n" % (GLIF, GLIF, ZNAK))
    m, zajete = menu(znaczniki)
    os.makedirs(os.path.join(wyj, "DeluxeMenus/gui_menus"), exist_ok=True)
    open(os.path.join(wyj, "DeluxeMenus/gui_menus", MENU + ".yml"), "w", encoding="utf8").write(m)
    # testowy pack klienta QA: tylko ten glif (reszta z prawdziwego pack.zip Nexo)
    tp = os.path.join(wyj, "testpack"); os.makedirs(os.path.join(tp, "assets/minecraft/font"), exist_ok=True)
    os.makedirs(os.path.join(tp, "assets/nexo/textures/gui"), exist_ok=True)
    p.save(os.path.join(tp, "assets/nexo/textures/gui", GLIF + ".png"))
    json.dump({"providers": [{"type": "bitmap", "file": "nexo:gui/%s.png" % GLIF, "ascent": 13, "height": 222,
               "chars": [ZNAK]}]}, open(os.path.join(tp, "assets/minecraft/font/default.json"), "w"))
    json.dump({"pack": {"description": "mapa test", "min_format": 75, "max_format": 199}},
              open(os.path.join(tp, "pack.mcmeta"), "w"))
    print("panel, glif, menu (%d znacznikow, sloty %s), testpack -> %s" % (len(znaczniki), sorted(zajete), wyj))

if __name__ == "__main__":
    main(*sys.argv[1:4])
