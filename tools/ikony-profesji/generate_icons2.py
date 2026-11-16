#!/usr/bin/env python3
"""Partia 2 ikon profesyjnych: przetworzone surowce szesciu profesji (30 sztuk).

Ta sama rodzina co partia 1 (generate_icons.py): wspolny kontur (24,22,30),
3-tonowe cieniowanie s/b/h ze swiatlem z gory-lewej, akcent a/A, 16x16 RGBA.
Palete i helpery importujemy stamtad, zeby rodzina nie rozjechala sie kolorystycznie.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_icons import PAL, mk, render  # noqa: E402
import tier as TIER  # noqa: E402
from classify_tiers import scan as scan_recipes, tier_of  # noqa: E402

OUT = os.environ.get(
    "OUT", "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/profesje")

# --- dodatkowe tony, w tej samej stonowanej konwencji co partia 1 -----------
PAL.update({
    "iron":    {"s": (120, 124, 132), "b": (168, 172, 180), "h": (212, 216, 224)},
    "silver":  {"s": (140, 146, 158), "b": (190, 196, 208), "h": (232, 236, 244)},
    "ruby":    {"s": (124, 28, 44), "b": (184, 48, 68), "h": (226, 96, 112)},
    "moon":    {"s": (96, 124, 158), "b": (140, 174, 208), "h": (196, 222, 244)},
    "mush":    {"s": (132, 30, 34), "b": (186, 48, 52), "h": (224, 92, 92)},
    "dry":     {"s": (104, 84, 44), "b": (146, 122, 66), "h": (186, 164, 100)},
    "bone":    {"s": (158, 150, 128), "b": (206, 200, 180), "h": (238, 234, 220)},
    "venom":   {"s": (58, 110, 44), "b": (96, 158, 62), "h": (142, 200, 96)},
    "oil":     {"s": (140, 96, 22), "b": (196, 142, 40), "h": (238, 192, 88)},
    "shell":   {"s": (144, 130, 124), "b": (188, 176, 168), "h": (226, 218, 212)},
    "water":   {"s": (40, 96, 140), "b": (70, 140, 192), "h": (128, 194, 236)},
    "flax":    {"s": (124, 132, 104), "b": (170, 178, 146), "h": (212, 218, 192)},
    "arcane":  {"s": (52, 140, 150), "b": (84, 196, 204), "h": (150, 236, 240)},
    "glass":   {"s": (108, 124, 130), "b": (152, 172, 178), "h": (200, 218, 224)},
})

def lustro(rows):
    """Odbija wzor w poziomie i zamienia cien z podswietleniem, zeby swiatlo dalej
    padalo z gory-lewej. Uzywane tam, gdzie szczyt przedmiotu kolidowal z plakietka
    tieru w prawym gornym rogu."""
    trans = str.maketrans({"s": "h", "h": "s"})
    return [r[::-1].translate(trans) for r in rows]


ICONS = {}


def icon(name, tone, accent, rows):
    ICONS[name] = (mk(tone, accent), rows)


# ======================= GORNIK =======================
icon("refined_iron", "iron", None, [                      # sztabka: gorna scianka + czolo
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...ohhhhhhhho...",
    "..ohhhhhhhhhho..",
    "..oooooooooooo..",
    ".obbbbbbbbbbbbo.",
    ".obbbbbbbbbbbbo.",
    ".obsbbbbbbbbsbo.",
    ".obssbbbbbbssbo.",
    ".ossssssssssso..",
    "..ooooooooooo...",
    "................",
    "................",
    "................",
])

icon("silver_ore", "stone", "silver", [                   # skala z zylami srebra
    "................",
    "................",
    "......oooo......",
    ".....ohhhbo.....",
    "...oohhbbbboo...",
    "..obhbbAAbbbbo..",
    "..obbbaAabbbbo..",
    ".obsbbbbbbAAbbo.",
    ".obsbbbbbbaAbbo.",
    ".obssbbAAbbbbbo.",
    "..obssbaAbbbbo..",
    "..oobsssbbbbo...",
    "....oosssbbo....",
    "......oooooo....",
    "................",
    "................",
])

icon("raw_gem", "ruby", None, [                           # nieoszlifowany klejnot
    "................",
    "................",
    "......oooo......",
    ".....ohhhho.....",
    "....ohhbbbho....",
    "...ohhbbbbbho...",
    "..obhbbbbbbbbo..",
    "..obbbbbbbbsbo..",
    "...obbbbbbsso...",
    "...obsbbbssso...",
    "....obsssssо....".replace("о", "o"),
    "....oobssso.....",
    "......ooso......",
    ".......oo.......",
    "................",
    "................",
])

icon("cut_stone", "stone", None, [                        # kostka konstrukcyjna
    "................",
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    ".obbbbobbbbbbbo.",
    ".obbbbobbbbbbbo.",
    ".ooooooooooooo o".replace(" ", "."),
    ".obbbbbbbbobbbo.",
    ".obbbbbbbbobbbo.",
    ".obsbbbbbbobbbo.",
    ".obsssbbbbobsbo.",
    ".ossssssssssoso.",
    "..oooooooooooo..",
    "................",
    "................",
])

icon("arcane_crystal", "arcane", "crystal", [             # jasniejszy, "naladowany" krysztal
    "................",
    ".......oo.......",
    "......ohho......",
    "......ohho......",
    ".....ohhbbo.....",
    "....oohhbbso....",
    "...oaAhhbbso....",
    "..oAAohhbbsoo...",
    "..oAaooohbsbho..",
    "..obbbsohbsbbo..",
    "..obbsssobsbbo..",
    "...obsssoossbo..",
    "...oossso.osbo..",
    ".....oso...oso..",
    "......o.....o...",
    "................",
])

# ======================= DRWAL-STOLARZ =======================
icon("seasoned_timber", "birch", None, [                  # STOS struganych desek.
    # Pojedyncza deska byla nijaka i zlewala sie z reszta brazowych prostokatow;
    # stos ma wlasna sylwetke (przesuniete warstwy, widoczne czola).
    "................",
    "................",
    "...ooooooooooo..",
    "..ohhhhhhhhhhbo.",
    "..ossssssssssso.",
    ".ooooooooooooo..",
    ".ohhhhhhhhhhbo..",
    ".ossssssssssso..",
    "..ooooooooooooo.",
    "..ohhhhhhhhhhbo.",
    "..obbbbbbbbbbbo.",
    "..ossssssssssso.",
    "...ooooooooooo..",
    "................",
    "................",
    "................",
])

icon("weapon_handle", "wood", "leather", [                # rekojesc owinieta rzemieniem
    "................",
    "........oo......",
    ".......ohho.....",
    "......ohbbo.....",
    "......ohbbo.....",
    ".....oaAAao.....",
    ".....oAAAAo.....",
    ".....oaAAao.....",
    "......ohbbo.....",
    "......ohbbo.....",
    ".....oaAAao.....",
    ".....oAAAAo.....",
    "......ohbso.....",
    "......obsso.....",
    ".....ohbbsso....",
    ".....oooooo.....",
])

icon("bow_limb", "birch", None, [                         # wygiete leczysko
    ".....oooo.......",
    "....ohhbbo......",
    "...ohhbo.obo....",
    "...ohbo...obo...",
    "..ohbo.....obo..",
    "..obbo......obo.",
    "..obbo.......oo.",
    "..obso..........",
    "..obso..........",
    "..obbo.......oo.",
    "..obbo......obo.",
    "..ohbo.....obo..",
    "...ohbo...obo...",
    "...ohhbo.obo....",
    "....ohhbbo......",
    ".....oooo.......",
])

icon("staff_core", "oak", "arcane", lustro([                     # korpus kostura, UKOSNY i grubszy.
    # Pionowy patyk 2 px szerokosci ginal w slocie GUI - ukos wypelnia kadr,
    # a zwienczenie z krysztalem daje czytelny punkt zaczepienia.
    "............oo..",
    "...........oaAo.",
    "..........oaAAao",
    "..........oaAAo.",
    "...........oao..",
    ".........ohhbo..",
    "........ohhbo...",
    ".......ohhbo....",
    "......ohhbo.....",
    ".....ohbbo......",
    "....ohbbo.......",
    "...ohbbo........",
    "..ohbso.........",
    "..obso..........",
    "..osso..........",
    "..oo............",
]))

icon("carved_fitting", "oak", "gold", [                   # rzezbione okucie
    "................",
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbaAbbbbaAbbo.",
    ".obbaAbbbbaAbbo.",
    ".obbbbbaAbbbbbo.",
    ".obsbbbaAbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".obbaAbbbbaAbbo.",
    ".obbaAbbbbaAbbo.",
    ".ossssssssssso..",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
])

# ======================= ZIELARZ =======================
icon("medicinal_herb", "green", None, [                   # zielona galazka
    "................",
    ".......oo.......",
    "....oohhho......",
    "...ohbbbhho.....",
    "..ohbbbsbho.....",
    "..obbbsobho.....",
    "...obbsohbo.....",
    "....oosohbo.....",
    "....oohsohoo....",
    "...ohbbsohhbo...",
    "..ohbbbsobbbho..",
    "..obbbsoosbbbo..",
    "...obssoossbo...",
    "....oosoosso....",
    "......oso.......",
    ".......o........",
])

icon("moonleaf", "moon", "green", lustro([                      # lisc: spiczasty owal + nerw i ogonek.
    # Wersja "diament" czytala sie jak klejnot, nie jak roslina - stad wyrazny ksztalt liscia.
    "................",
    "...........ooo..",
    ".........oohhho.",
    "........ohhhbbo.",
    ".......ohhbAbbo.",
    "......ohhbAbbbo.",
    ".....ohhbAbbbbo.",
    "....ohhbAbbbbbo.",
    "...ohhbAbbbbbo..",
    "..ohhbAbbbbbo...",
    "..obbAbbbbbo....",
    "..obAbbbbso.....",
    "..oAbbbsso......",
    ".oaAoosso.......",
    ".oaao...........",
    "..oo............",
]))

icon("redcap", "mush", "fleece", [                        # czerwony kapelusz w kropki
    "................",
    "................",
    ".....oooooo.....",
    "...oohhhhhhoo...",
    "..ohhAhhhAhhho..",
    ".ohhbbAbbbAbbho.",
    ".obbbbbbbbbbbbo.",
    ".obsbAbbbbbAbso.",
    ".ossssssssssso..",
    "...ooooooooo....",
    "....oaaaao......",
    "....oaAAao......",
    "....oaAAao......",
    "....oaaaao......",
    "....ooooo.......",
    "................",
])

icon("dried_herbs", "dry", "leather", [                   # CIASNY zwiazany peczek suszu.
    # Rozstrzelona wersja byla na granicy czytelnosci w slocie 16x16.
    "................",
    "....o..o..o.....",
    "...ohooho oho...".replace(" ", "."),
    "...ohbohbohbo...",
    "...obhbohbhbo...",
    "...ohbhbhbhbo...",
    "...obhbhbhbbo...",
    "..oaaaaaaaaaao..",
    "..oAAAAAAAAAAo..",
    "..oaaaaaaaaaao..",
    "...obhbhbhbbo...",
    "...ohbhbhbhbo...",
    "...obsbsbsbso...",
    "...oso oso oso..".replace(" ", "."),
    "....o..o..o.....",
    "................",
])

icon("herbal_extract", "glass", "green", [                # fiolka z zielonym wyciagiem
    "................",
    "......oooo......",
    "......ohbo......",
    "......ohbo......",
    ".....oohboo.....",
    "....ohhbbbho....",
    "...ohhbbbbbho...",
    "...obaaaaabo....",
    "...obaAAAaabo...",
    "...obaAAAaabo...",
    "...obaAAAaabo...",
    "...obaaaaaabo...",
    "....obsaaasbo...",
    "....oossssoo....",
    "......oooo......",
    "................",
])

# ======================= MYSLIWY =======================
icon("cured_hide", "leather", "dry", [                    # arkusz skory z zawinietym rogiem,
    # ktory odslania jasniejszy spod. Rulon zlewal sie z klodami - plat z zagieciem nie.
    "................",
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    ".obbbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".obssbbbbbbbbbo.",
    ".obsssbbbbbbbbo.",
    ".obsssooobbbbbo.",
    ".obssoaAAobbbbo.",
    ".ossoaAAAAobbbo.",
    "..ooaaaaaaaooоo.".replace("о", "o"),
    "...ooooooooooo..",
    "................",
])

icon("sinew", "dry", None, [                              # zwoj sznura/sciegna - krag liny.
    # Poprzednia wersja (haczyk) nie czytala sie wcale; zwiniety krag jest jednoznaczny.
    "................",
    "................",
    "....oooooooo....",
    "..oohhhhhhhhoo..",
    ".ohhbbbbbbbbhho.",
    ".ohboooooooobho.",
    "obbooohhhhooobbo",
    "obbooohbbhooobbo",
    "obsooohbbhooosbo",
    "obsooohhhhooosbo",
    ".obsooooooooobo.",
    ".obssbbbbbbssbo.",
    "..oobssssssboo..",
    "....oooooooo....",
    "................",
    "................",
])

icon("beast_bone", "bone", None, [                        # kosc
    "................",
    "..oo........oo..",
    ".ohho......ohho.",
    ".ohbho....ohbho.",
    "..obbooooobbbo..",
    "...obbbbbbbbo...",
    "....obbbbbbo....",
    ".....obbbbo.....",
    ".....obbbbo.....",
    "....obbbbbbo....",
    "...obbbbbbbbo...",
    "..obssooooossbo.",
    ".obsso....obsso.",
    ".osso......osso.",
    "..oo........oo..",
    "................",
])

icon("monster_trophy", "bone", "ruby", [                  # rogata czaszka
    "................",
    ".oo..........oo.",
    "ohho........ohho",
    "ohbho......ohbho",
    ".obbhoooooohbbo.",
    "..ohhhhhhhhhho..",
    "..ohhbbbbbbhho..",
    "..obbaAbbaAbbo..",
    "..obbaAbbaAbbo..",
    "..obbbbbbbbbbo..",
    "...obbbobbbbo...",
    "...obsbobobso...",
    "....obobobobo...",
    "....ossoossoo...",
    ".....oooooo.....",
    "................",
])

icon("venom_sac", "venom", None, [                        # woreczek jadowy
    "................",
    "......oooo......",
    ".....ohbbo......",
    ".....ohbbo......",
    "....oohbboo.....",
    "...ohhbbbbho....",
    "..ohhbbbbbbho...",
    ".obhbbbbbbbbho..",
    ".obbbbbbbbbbbo..",
    ".obsbbbbbbbbbo..",
    ".obssbbbbbbbbo..",
    "..obsssbbbbbo...",
    "..oobssssssbo...",
    "....oossssoo....",
    "......oooo......",
    "................",
])

# ======================= RYBAK =======================
icon("fish_oil", "glass", "oil", [                        # butelka oleju
    "................",
    ".....oooooo.....",
    ".....obbbbo.....",
    "......ohbo......",
    "......ohbo......",
    ".....oohboo.....",
    "....ohhbbbho....",
    "...ohhbaaabho...",
    "...obaAAAaabo...",
    "...obaAAAAabo...",
    "...obaAAAAabo...",
    "...obaaAAaabo...",
    "....obsaaasbo...",
    "....oossssoo....",
    "......oooo......",
    "................",
])

icon("clam", "shell", "fleece", [                         # muszla malza
    "................",
    "................",
    "................",
    "......oooo......",
    "....oohhhhoo....",
    "..oohhbhhbhhoo..",
    ".ohhbbhbbhbbhho.",
    "obhbbbhbbhbbbhbo",
    "obbbbbbbbbbbbbbo",
    "obsbbbbbbbbbbsbo",
    ".obssbbbbbbssbo.",
    "..oobsssssssboo.",
    "....ooossssoo...",
    "......oooooo....",
    "................",
    "................",
])

icon("pearl", "fleece", "clay", [                        # perla w polowce muszli.
    # Sama biala kula byla nieodroznialna od raw_fleece - ciemniejsza muszla
    # pod spodem daje kontrast i jednoznaczny kontekst.
    "................",
    "................",
    ".....oooooo.....",
    "....ohhhhhho....",
    "...ohhhhhhhbo...",
    "...ohhbbbbbbo...",
    "...obbbbbbbso...",
    "....obbbbbso....",
    ".....oossoo.....",
    "..ooaaaaaaaaoo..",
    ".oaAaaaaaaaaAao.",
    ".oaAAaaaaaaAAao.",
    "..oaaAAAAAAaao..",
    "...ooaaaaaaoo...",
    ".....oooooo.....",
    "................",
])

icon("rare_fish", "gold", "fleece", [                     # zlota ryba
    "................",
    "................",
    "...........oo...",
    "....oooo..ohho..",
    "..oohhhhoohbbo..",
    ".ohhbAbbbhbbbo..",
    ".obhbAbbbbhbbbo.",
    "ohbbbbbbbbbhbbo.",
    "obsbbbbbbbbbbbo.",
    ".obsbbbbbbbhbbo.",
    ".obssbbbbsbbbo..",
    "..oossbbsobbo...",
    "....ooossoosso..",
    "........ooooo...",
    "................",
    "................",
])

icon("water_essence", "water", "fleece", [                # kropla-esencja
    "................",
    ".......oo.......",
    "......ohho......",
    "......ohho......",
    ".....ohhbbo.....",
    "....ohhAbbbo....",
    "...ohhbAbbbbo...",
    "..ohhbbAbbbbbo..",
    "..obhbbbbbbbbo..",
    ".obbbbbbbbbbbbo.",
    ".obsbbbbbbbbsbo.",
    ".obssbbbbbbssbo.",
    "..obsssbbbsssbo.",
    "..oobssssssssbo.",
    "....ooossssooo..",
    "......oooo......",
])

# ======================= FARMER =======================
icon("quality_grain", "wheat", "green", [                 # KOPIEC wyluskanego ziarna.
    # Jako snopek roznil sie od fresh_harvest tylko odcieniem - za malo.
    # Ziarno luzem to inna postac tego samego motywu: widac, ze to juz plon obrobiony.
    "................",
    "................",
    "................",
    "......oaAo......",
    ".....ooaAoo.....",
    "....oohhhhoo....",
    "...ohAhbAhbho...",
    "..ohAbhbAbhbho..",
    "..obAhbhbAbhbo..",
    ".obhbAbhbAbhbbo.",
    ".obAbhbAbhbAbbo.",
    ".obsbAbhbAbhsbo.",
    ".obssbbbbbbssbo.",
    "..oossssssssoo..",
    "....oooooooo....",
    "................",
])

icon("flax_fiber", "flax", "leather", [                   # rowny, zwiazany peczek wlokien.
    # Wersja rozstrzelona czytala sie w slocie jak owad - stad ciasny, pionowy snopek.
    "................",
    ".....o.o.o......",
    "....ohohoho.....",
    "....ohbhbho.....",
    "....obhbhbo.....",
    "....ohbhbho.....",
    "....obhbhbo.....",
    "...oaaaaaaao....",
    "...oAAAAAAAo....",
    "...oaaaaaaao....",
    "....obhbhbo.....",
    "....ohbhbho.....",
    "....obhbhbo.....",
    "....osbsbso.....",
    ".....o.o.o......",
    "................",
])

icon("fine_wool", "fleece", "moon", [                     # motek delikatnej welny
    "................",
    "................",
    "....oooooooo....",
    "..oohhhhhhhhoo..",
    ".ohhbbhhbbhhbho.",
    "obhbbbhbbhbbbho.",
    "obbbhbbhbbhbbbo.",
    "obaAaAaAaAaAabo.",
    "obaAaAaAaAaAabo.",
    "obbbhbbhbbhbbbo.",
    "obsbbbhbbhbbsbo.",
    ".obssbbbbbbssbo.",
    "..oobsssssssboo.",
    "....oooooooo....",
    "................",
    "................",
])

icon("bait_mix", "dry", "venom", [                        # sakiewka z zaneta
    "................",
    "......oooo......",
    ".....oaaao......",
    "....ooaaaoo.....",
    "...ohhbbbbho....",
    "..ohhbAbbbbho...",
    ".obhbbbbAbbbho..",
    ".obbbAbbbbbbbo..",
    ".obbbbbbbAbbbo..",
    ".obsbbAbbbbbbo..",
    ".obssbbbbbAbbo..",
    "..obsssbbbbbo...",
    "..oobssssssbo...",
    "....oossssoo....",
    "......oooo......",
    "................",
])

icon("alchemical_crop", "crystal", "venom", [             # swiecaca bulwa z nacia.
    # Wersja z "kielichem" czytala sie jak fioletowa miska, nie jak plon.
    "................",
    ".......oo.......",
    "......oaAo......",
    ".....oaAAao.....",
    "....oaAoAAao....",
    ".....ooaAoo.....",
    ".......oo.......",
    "....oooooooo....",
    "...ohhhhhhhho...",
    "..ohhbbbbbbbho..",
    "..obbbbbbbbbbo..",
    "..obsbbbbbbbbo..",
    "...obssbbbbbo...",
    "....obsssbbo....",
    ".....oossoo.....",
    ".......oo.......",
])


def main():
    os.makedirs(OUT, exist_ok=True)
    print(f"ikon w partii 2: {len(ICONS)}")
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
    cols, S = 6, 96
    rows_n = (len(built) + cols - 1) // cols
    sheet = Image.new("RGBA", (S * cols, S * rows_n), (38, 38, 46, 255))
    for i, (name, (im, t)) in enumerate(built.items()):
        sheet.alpha_composite(im.resize((S, S), Image.NEAREST), ((i % cols) * S, (i // cols) * S))
    sheet.save("/tmp/ikony-work/sheet2.png")
    print("arkusz: /tmp/ikony-work/sheet2.png")


if __name__ == "__main__":
    main()
