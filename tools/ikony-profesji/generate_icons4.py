#!/usr/bin/env python3
"""Partia 4: narzedzia profesyjne (tool.yml) i bizuteria (ring/necklace/charm).

DWIE DECYZJE PROJEKTOWE:

1. Narzedzia celuja GLOWICA W LEWY GORNY ROG, odwrotnie niz vanilla. Powod jest
   praktyczny: plakietka tieru siedzi w prawym gornym rogu, a vanillowe narzedzia
   maja tam wlasnie ostrze - plakietka zjadalaby najwazniejszy element (dokladnie
   to stalo sie wczesniej ze staff_core i moonleaf). Wszystkie piec narzedzi jest
   odwrocone tak samo, wiec zestaw czyta sie spojnie.

2. Wersja MISTRZOWSKA ma TE SAMA SYLWETKE co podstawowa, zmienia sie material:
   stal -> jasne srebro, drewno/rzemien -> zloto. Do tego automatycznie zlota
   plakietka T3. Gracz ma od razu widziec, ze to ten sam przedmiot, tylko lepszy -
   przeprojektowywanie sylwetki niszczyloby ten zwiazek.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_icons import PAL, mk, render  # noqa: E402
import generate_icons2  # noqa: E402,F401  (dokłada tony do wspólnego PAL)
import generate_icons3  # noqa: E402,F401  (j.w.)
import tier as TIER  # noqa: E402
from classify_tiers import scan as scan_recipes, tier_of  # noqa: E402

OUT = os.environ.get(
    "OUT", "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/profesje")

ICONS = {}


def icon(name, tone, accent, rows):
    ICONS[name] = (mk(tone, accent), rows)


def para(base_name, rows, tone="steel", accent="wood",
         master_tone="silver", master_accent="gold"):
    """Rejestruje wersje podstawowa i mistrzowska tej samej sylwetki."""
    icon(base_name, tone, accent, rows)
    icon("master_" + base_name, master_tone, master_accent, rows)


# ============================ NARZEDZIA ============================

# Siekiera drwala - KLIN ostrza rozszerzajacy sie w lewo (okragla glowica czytala sie
# jak mlotek), trzonek na ukos w dol w prawo
para("lumber_axe", [
    "................",
    "..oooooo........",
    ".ohhhhhho.......",
    "ohhhhhhbho......",
    "ohhhhhbbbo......",
    "ohhhhbbbaAo.....",
    "ohhhbbbo.aAo....",
    "ohhbbbo...aAo...",
    ".obbbo.....aAo..",
    "..obo.......aAo.",
    "...o.........aAo",
    "..............Ao",
    "..............oo",
    "................",
    "................",
    "................",
])

# Motyka farmera - waskie ostrze poziome, trzonek na ukos
para("farmer_hoe", [
    "oooooooo........",
    "ohhhhhhho.......",
    "obbbbbbbho......",
    "obsbbbbbbo......",
    "ossssbbbaAo.....",
    "oooo.obo.aAo....",
    "......o...aAo...",
    "...........aAo..",
    "............aAo.",
    ".............aAo",
    "..............Ao",
    "..............oo",
    "................",
    "................",
    "................",
    "................",
])

# Sierp zielarza - polksiezyc ostrza otwarty w prawo, trzonek od dolnego rogu
para("herb_sickle", [
    "................",
    "...ooooo........",
    "..ohhhhho.......",
    ".ohhbbbbho......",
    ".ohbooooobo.....",
    ".obo....oobo....",
    ".obo.......o....",
    ".obo............",
    ".obbo...........",
    "..obbo..........",
    "..oobbo.........",
    "....obbo........",
    ".....obbo.aAo...",
    "......ooboaAo...",
    "........oaAo....",
    ".........oo.....",
])

# Noz mysliwski - krotkie, szerokie ostrze
para("hunter_knife", [
    "..ooo...........",
    ".ohhho..........",
    ".ohhbho.........",
    ".ohbbbho........",
    ".obbbbbho.......",
    ".obsbbbbho......",
    "..obsbbbbho.....",
    "...obsbbbbo.....",
    "....obsbbaAo....",
    ".....oooaAAAo...",
    "........aAAAAo..",
    ".........aAAAo..",
    "..........aAAo..",
    "..........oaao..",
    "...........oo...",
    "................",
])

# Wedka rybaka - GRUBSZY kij (cienki patyk ginal w slocie) + zylka z haczykiem
# spadajaca od czubka przy lewej krawedzi
para("fishing_rod", [
    ".ooo............",
    "ohbbo...........",
    "ohbbo...........",
    "oohbbo..........",
    "o.ohbbo.........",
    "o..ohbbo........",
    "o...ohbbo.......",
    "o....ohbbo......",
    "o.....ohbbo.....",
    "o......ohbbo....",
    "o.......ohbbo...",
    "o........oaAo...",
    "o.........oaAo..",
    "oo.........oaAo.",
    "oao.........ooo.",
    ".ao.............",
], tone="wood", accent="fleece", master_tone="oak", master_accent="gold")


# ============================ BIZUTERIA ============================

icon("silver_signet", "silver", "rune", [          # OBRACZKA z tarczka sygnetu.
    # Poprzednia wersja czytala sie jak klodka - teraz widac otwarty pierscien.
    "................",
    "................",
    ".....oooooo.....",
    "....ohhaahho....",
    "...ohhaAAahho...",
    "...ohaAAAAaho...",
    "...ohhaAAahho...",
    "....ohhaahho....",
    "...oobbbbbboo...",
    "..obbooooooobbo.",
    "..obo......obo..",
    "..obo......obo..",
    "..obso....osbo..",
    "...obsooosbbo...",
    "....oossssoo....",
    "......oooo......",
])

icon("moonleaf_amulet", "moon", "silver", [        # lancuszek + zawieszka w ksztalcie LISCIA
    # (wachlarzowata zawieszka nie mowila nic o ksiezycowym lisciu).
    "..oo........oo..",
    ".oao........oao.",
    "..oao......oao..",
    "...oao....oao...",
    "....oaooooao....",
    ".....oaaaao.....",
    "......oooo......",
    ".....ohhbo......",
    "....ohhbbbo.....",
    "...ohhbAbbbo....",
    "...ohbAbbbbo....",
    "...obAbbbbbo....",
    "....oAbbbbo.....",
    ".....obbbo......",
    "......obo.......",
    ".......o........",
])

icon("luck_charm", "green", "gold", [              # czterolistna koniczyna na kolku
    "................",
    "....oo..oo......",
    "...ohho.ohho....",
    "..ohbbhoohbbho..",
    "..obbbbhhbbbbo..",
    "...obbbbbbbbo...",
    "....obbbbbbo....",
    "..ohhoobboohho..",
    ".ohbbho..ohbbho.",
    ".obbbbhoohbbbbo.",
    "..obbbbhhbbbbo..",
    "...obbbbbbbbo...",
    "....oosbsoo.....",
    "......oao.......",
    "......oaAo......",
    ".......oo.......",
])


def main():
    os.makedirs(OUT, exist_ok=True)
    print(f"ikon w partii 4: {len(ICONS)}")
    assert len(ICONS) == 14, f"oczekiwano 14, jest {len(ICONS)}"
    outputs = scan_recipes()
    built = {}
    for name, (cmap, rows) in ICONS.items():
        assert len(rows) == 16, f"{name}: {len(rows)} wierszy"
        im = render(rows, cmap)
        t = tier_of("PROF_" + name.upper(), outputs)
        TIER.stamp(im, t)
        built[name] = (im, t)
        im.save(os.path.join(OUT, f"{name}.png"))
    print("tiery:", {n: t for n, (_, t) in built.items()})

    from PIL import Image
    cols, S = 5, 96
    rows_n = (len(built) + cols - 1) // cols
    sheet = Image.new("RGBA", (S * cols, S * rows_n), (38, 38, 46, 255))
    for i, (name, (im, t)) in enumerate(built.items()):
        sheet.alpha_composite(im.resize((S, S), Image.NEAREST), ((i % cols) * S, (i // cols) * S))
    sheet.save("/tmp/ikony-work/sheet4.png")
    print("arkusz: /tmp/ikony-work/sheet4.png")
    print("kolejnosc:", list(built))


if __name__ == "__main__":
    main()
