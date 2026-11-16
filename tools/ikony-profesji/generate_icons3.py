#!/usr/bin/env python3
"""Partia 3: pozostale 36 surowcow PROF_* (alchemia, kowalstwo, krawiectwo,
jubilerstwo, zaklinanie, budownictwo, kuchnia).

Ta sama rodzina co partie 1-2. Swiadomie ROZNICUJEMY SYLWETKI, nie tylko odcien:
uwaga z przegladu brzmiala, ze brazowe prostokaty (deski/klody/plyty) czytaja sie
podobnie. Dlatego plyta metalowa jest wypukla i nitowana, belka nosna ma zastrzal,
a panel skorzany ma ksztalt tarczy z dziurkami - zadne z nich nie jest prostokatem.

Naczynia alchemiczne tez rozroznione ksztaltem, bo jest ich piec: kolba stozkowa,
przysadzisty sloik, okragla fiolka, szescioboczna butla, kanka z dziobkiem.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_icons import PAL, mk, render  # noqa: E402
# Import dla EFEKTU UBOCZNEGO: generate_icons2 dokłada do wspólnego PAL tony
# (glass, silver, steel-podobne...), z których korzysta też ta partia.
# Jedno zrodlo palety = rodzina nie rozjedzie sie kolorystycznie.
import generate_icons2  # noqa: E402,F401
import tier as TIER  # noqa: E402
from classify_tiers import scan as scan_recipes, tier_of  # noqa: E402

OUT = os.environ.get(
    "OUT", "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/profesje")

PAL.update({
    "cloth":  {"s": (74, 84, 120), "b": (110, 124, 168), "h": (152, 166, 208)},
    "meat":   {"s": (120, 56, 48), "b": (168, 84, 70), "h": (206, 126, 108)},
    "bread":  {"s": (150, 112, 62), "b": (196, 156, 96), "h": (230, 198, 142)},
    "rune":   {"s": (62, 64, 74), "b": (92, 95, 108), "h": (126, 130, 146)},
    "wax":    {"s": (118, 34, 40), "b": (168, 52, 58), "h": (208, 96, 100)},
    "steel":  {"s": (104, 112, 126), "b": (150, 158, 174), "h": (196, 204, 218)},
})

ICONS = {}


def icon(name, tone, accent, rows):
    ICONS[name] = (mk(tone, accent), rows)


# ============================ ALCHEMIA ============================
icon("alchemical_base", "glass", "venom", [        # kolba STOZKOWA
    "................",
    ".....oooooo.....",
    ".....ohbbho.....",
    "......ohbo......",
    "......ohbo......",
    "......ohbo......",
    ".....oohboo.....",
    "....ohhbbbho....",
    "...ohhbaaabho...",
    "..ohbaaaaaaabo..",
    "..obaAAAAAAabo..",
    ".obaAAAAAAAAabo.",
    ".obaaAAAAAAaabo.",
    ".obssaaaaaassbo.",
    "..oossssssssoo..",
    "....oooooooo....",
])

icon("alchemical_solvent", "glass", "water", [     # przysadzisty SLOIK z korkiem
    "................",
    "................",
    "....oooooooo....",
    "....obbbbbbo....",
    "....oooooooo....",
    "...ohhhhhhhho...",
    "..ohhbaaaaabho..",
    "..obbaAAAAAabo..",
    "..obbaAAAAAabo..",
    "..obbaAAAAAabo..",
    "..obsaAAAAAabo..",
    "..obsaaaaaaabo..",
    "..obssaaaaasbo..",
    "...oossssssoo...",
    "....oooooooo....",
    "................",
])

icon("antidote", "glass", "green", [               # okragla FIOLKA z bialym krzyzem
    "................",
    "......oooo......",
    "......ohbo......",
    "......ohbo......",
    ".....oohboo.....",
    "....ohhbbbho....",
    "...ohbaaaaabo...",
    "..ohbaaAhAaabo..",
    "..obbaaAhAaabo..",
    "..obbahhhhhabo..",
    "..obsaaAhAaabo..",
    "..obsaaAhAaabo..",
    "...obsaaaaabo...",
    "....oossssoo....",
    "......oooo......",
    "................",
])

icon("combat_tonic", "glass", "ruby", [            # SZESCIOBOCZNA butla
    "................",
    "......oooo......",
    "......obbo......",
    "......ohbo......",
    ".....oohboo.....",
    "....ohhbbbho....",
    "...ohbaaaaabo...",
    "..ohbaAAAAAabo..",
    ".obbaAAAAAAAabo.",
    ".obbaAAAAAAAabo.",
    ".obsaAAAAAAAabo.",
    "..obsaAAAAAabo..",
    "...obsaaaaabo...",
    "....oossssoo....",
    "......oooo......",
    "................",
])

icon("weapon_oil", "steel", "oil", [              # KANKA z dlugim dziobkiem.
    # Poprzednia wersja byla nieczytelna - teraz sylwetka rozni sie od pozostalych
    # czterech naczyn alchemicznych nie kolorem, tylko ksztaltem (dziobek w gore).
    "................",
    "...........ooo..",
    "..........ohbo..",
    ".........ohbo...",
    "........ohbo....",
    ".......ohbo.....",
    "....oooobo......",
    "..oohhhhho......",
    ".ohhbaaaabho....",
    ".obbaAAAAabbo...",
    ".obbaAAAAAabo...",
    ".obsaAAAAAabo...",
    ".obssaaaaaabo...",
    "..oossssssboo...",
    "....oooooooo....",
    "................",
])

# ============================ KOWALSTWO ============================
icon("arrowheads", "steel", "leather", [           # TRZY groty zwiazane w peczek.
    # Luzne elementy rozsypane po kadrze czytaly sie jak szum; peczek ma jedna sylwetke.
    "................",
    "..o....o....o...",
    ".oho..oho..oho..",
    "ohbhoohbhoohbho.",
    "obbboobbboobbbo.",
    "osbsoosbsoosbso.",
    ".obo...obo...obo",
    ".oao...oao...oao",
    ".oao...oao...oao",
    ".oao...oao...oao",
    "oaaaaaaaaaaaaao.",
    "oAAAAAAAAAAAAAo.",
    "oaaaaaaaaaaaaao.",
    ".oao...oao...oao",
    ".oso...oso...oso",
    "..o.....o.....o.",
])

icon("axe_head", "steel", "oak", [                 # glowica siekiery
    "................",
    "................",
    "....oooo........",
    "...ohhhho.......",
    "..ohhhhhhoo.....",
    ".ohhhbbbbbho....",
    "ohhbbbbbbbbho...",
    "ohbbbbbbbbbbho..",
    "ohbbbbbbbbbaAo..",
    "ohsbbbbbbbbaAo..",
    ".ohsbbbbbbbaAo..",
    "..ohssbbbbbaAo..",
    "...ohssbbbbaAo..",
    "....ooossbbaAo..",
    ".......oooooo...",
    "................",
])

icon("master_blade_blank", "steel", "gold", [      # glownia SZERSZA, ze zbroczem.
    # Wersja 2-pikselowa ginela w slocie - ten sam blad co przy staff_core.
    "................",
    "......oooo......",
    ".....ohbbho.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    ".....ohbsbo.....",
    "....oooooooo....",
    "....oaAAAAao....",
    "....oaaaaaao....",
    ".....oooooo.....",
])

icon("metal_plate", "steel", "gold", [             # plyta z nitami w naroznikach.
    # Ukosny blik z poprzedniej wersji dawal zagiecie - calosc czytala sie jak koperta.
    # Blik tylko w gornym rzedzie, korpus rowny stalowy, nity mosiezne w czterech rogach.
    "................",
    "................",
    "...oooooooooo...",
    "..ohhhhhhhhhho..",
    ".ohaAhhhhhhAaho.",
    ".obbbbbbbbbbbbo.",
    ".obbbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".obssbbbbbbbbbo.",
    ".obsaAssssssAao.",
    "..osssssssssso..",
    "...oooooooooo...",
    "................",
    "................",
    "................",
])

icon("metal_repair_kit", "steel", "gold", [        # SKRZYNKA narzedziowa z okuciami.
    # Mlotek z kowadlem zlewal sie w nieczytelna plame; skrzynka ma czysta sylwetke
    # i pasuje do fabric_repair_kit jako para "zestawow".
    "................",
    ".....oooooo.....",
    "....oho..oho....",
    "....oo....oo....",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    ".obaAbbbbbbaAbo.",
    ".obaAbbbbbbaAbo.",
    ".obbbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".obssbbbbbbbbbo.",
    ".ossssssssssso..",
    "..oooooooooooo..",
    "................",
    "................",
])

icon("steel_mechanism", "steel", "gold", [         # zebatka
    "................",
    "...o..oooo..o...",
    "..ohooohhhooho..",
    "..ohhhhhhhhhho..",
    "...ohhhbbbhho...",
    "oooohbbbbbbhoooo",
    "ohhhbbbaaabbbhho",
    "obbbbbaAAAabbbbo",
    "obbbbbaAAAabbbbo",
    "obsbbbbaaabbbsbo",
    "oooosbbbbbbsoooo",
    "...osbbbbbbso...",
    "..obssbbbbssbo..",
    "..oboosssssobo..",
    "..oho..oooo..oho",
    "...o..........o.",
])

# ============================ KRAWIECTWO ============================
icon("cloth_bolt", "cloth", "gold", [              # BELA tkaniny ze zwisajacym koncem
    "................",
    "...oooooooooo...",
    "..ohhhhhhhhhho..",
    "..obbbbbbbbbbo..",
    "..obaaaaaaaabo..",
    "..obbbbbbbbbbo..",
    "..obbbbbbbbbbo..",
    "..obaaaaaaaabo..",
    "..obsbbbbbbsbo..",
    "..ossssssssbso..",
    "..oooooooobbbo..",
    "........obbbbo..",
    "........obsbbo..",
    ".......obssbbo..",
    ".......obsssbo..",
    "........oooooo..",
])

icon("leather_panel", "leather", "bone", [         # panel w ksztalcie TARCZY z dziurkami
    "................",
    "..oooooooooooo..",
    ".ohaAhhhhhhAaho.",
    ".obbbbbbbbbbbbo.",
    ".oabbbbbbbbbbao.",
    ".oAbbbbbbbbbbAo.",
    ".obbbbbbbbbbbbo.",
    ".oabsbbbbbbbbao.",
    ".oAbsbbbbbbbbAo.",
    "..obssbbbbbbbo..",
    "..oabsssbbbbao..",
    "...oAbsssbbAo...",
    "....obsssbbo....",
    ".....obsssbo....",
    "......oosso.....",
    ".......oo.......",
])

icon("robe_lining", "cloth", "gold", [             # zlozona podszewka ze zlotym lamowaniem
    "................",
    "................",
    "..oooooooooooo..",
    ".oaAAAAAAAAAAao.",
    ".oaaaaaaaaaaaao.",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    ".oooooooooooooo.",
    "..ohhhhhhhhhho..",
    "..obbbbbbbbbbo..",
    "..obsbbbbbbsbo..",
    "..oooooooooooo..",
    "...ohhhhhhhho...",
    "...obssssssbo...",
    "...oaaaaaaaao...",
    "....oooooooo....",
])

icon("fabric_repair_kit", "cloth", "silver", [     # szpulka nici + igla
    "................",
    "............oo..",
    "...........oAo..",
    "..........oAo...",
    ".........oAo....",
    "...oooo.oAo.....",
    "..ohhhhoAo......",
    "..obbbbAo.......",
    "..obbbbbo.......",
    "..obbbbbo.......",
    "..obsbbbo.......",
    "..obssbbo.......",
    "..ossssso.......",
    "..oooooooo......",
    "................",
    "................",
])

icon("bowstring", "dry", "birch", [               # SKRECONY sznur cieciwy, po ukosie.
    # Petla czytala sie jak klamra. Skret (naprzemienne jasne/ciemne segmenty) to
    # najczytelniejszy sygnal "to jest sznur", a ukos daje sylwetke inna niz krag sinew.
    "................",
    "............ooo.",
    "..........ooaAo.",
    ".........oaAho..",
    "........oaAho...",
    ".......ohbaAo...",
    "......ohbaAo....",
    ".....oaAhbo.....",
    "....oaAhbo......",
    "...ohbaAo.......",
    "..ohbaAo........",
    "..oaAho.........",
    ".oaAho..........",
    ".oAho...........",
    ".ooo............",
    "................",
])

# ============================ JUBILERSTWO ============================
icon("cut_gem", "ruby", "fleece", [                # SZLIFOWANY brylant (symetryczny, fasety)
    "................",
    "................",
    "...oooooooooo...",
    "..ohhhhhhhhhho..",
    ".ohAhbhbhbhbAho.",
    "oooooooooooooooo",
    "obbbsbbbbbbsbbbo",
    ".obbbsbbbbsbbbo.",
    ".oobbbsbbsbbbo..",
    "..oobbbssbbbo...",
    "...oobbssbbo....",
    "....oobssbo.....",
    ".....oossoo.....",
    "......ooo.......",
    ".......o........",
    "................",
])

icon("silver_setting", "silver", None, [           # PUSTA oprawa z lapkami
    "................",
    "..o..o....o..o..",
    ".oho.oho.oho.oho",
    ".obho.obhoobho..",
    "..obhoooooohbo..",
    "..obbo....obbo..",
    ".obbo......obbo.",
    ".obbo......obbo.",
    ".obbo......obbo.",
    ".obsbo....obsbo.",
    "..obssoooossbo..",
    "..oobssbbssboo..",
    "....ossssssо....".replace("о", "o"),
    ".....oooooo.....",
    "................",
    "................",
])

icon("pearl_setting", "silver", "fleece", [        # ta sama oprawa, ale Z PERLA
    "................",
    "..o..o....o..o..",
    ".oho.oho.oho.oho",
    ".obho.oaaao.bho.",
    "..obhoaAAAaohbo.",
    "..obboaAAAaobbo.",
    ".obboaAAAAAaobbo",
    ".obboaAAAAAaobbo",
    ".obboaaAAAaaobbo",
    ".obsboaaaaaobsbo",
    "..obssoooooossbo",
    "..oobssbbbssboo.",
    "....oossssoo....",
    ".....oooooo.....",
    "................",
    "................",
])

icon("empty_socket", "rune", "silver", [           # ciemne gniazdo w metalowym pierscieniu
    "................",
    "................",
    "....oaaaaaao....",
    "..ooaAAAAAAaoo..",
    ".oaAAoooooooAao.",
    ".oaAooobbbooaAo.",
    "oaAooobsssbooaAo",
    "oaAoobssssssboAo",
    "oaAoobssssssboAo",
    "oaAooobsssbooaAo",
    ".oaAooobbbooaAo.",
    ".oaAAoooooooAao.",
    "..ooaAAAAAAaoo..",
    "....oaaaaaao....",
    "................",
    "................",
])

icon("master_charm", "gold", "ruby", [             # ozdobny wisior z klejnotem
    "................",
    ".......oo.......",
    "......ohho......",
    "......obbo......",
    "....oooooooo....",
    "..oohhhhhhhhoo..",
    ".ohhbbaaaabbhho.",
    ".obbbaAAAAabbbo.",
    "obbbaAAAAAAabbbo",
    "obsbaAAAAAAabsbo",
    ".obsbaAAAAabsbo.",
    ".oobssaaaassboo.",
    "...obsssssbo....",
    "....oossoo......",
    "......oo........",
    "................",
])

icon("awakening_seal", "wax", "gold", [            # pieczec z runem
    "................",
    "................",
    "....oooooooo....",
    "..oohhhhhhhhoo..",
    ".ohhbbbbbbbbhho.",
    "ohhbbaAAAAabbhho",
    "obbbaAoooAAabbbo",
    "obbbaAoobAAabbbo",
    "obsbaAoobAAabsbo",
    "obsbaAoooAAabsbo",
    ".obsbaAAAAabsbo.",
    ".oobssaaaassboo.",
    "...oosssssoo....",
    ".....oooooo.....",
    "................",
    "................",
])

# ============================ ZAKLINANIE ============================
icon("rune_blank", "rune", None, [                 # gladka tabliczka runiczna
    "................",
    "....oooooooo....",
    "...ohhhhhhhho...",
    "..ohhbbbbbbhho..",
    "..obbbbbbbbbbo..",
    "..obbbbbbbbbbo..",
    "..obsbbbbbbbbo..",
    "..obsbbbbbbbbo..",
    "..obsbbbbbbbbo..",
    "..obssbbbbbbbo..",
    "..obssbbbbbbbo..",
    "..obsssbbbbbbo..",
    "..obsssssbbbbo..",
    "..osssssssssso..",
    "...oooooooooo...",
    "................",
])

icon("charged_rune", "rune", "crystal", [          # ta sama tabliczka, ROZSWIETLONY znak
    "................",
    "....oooooooo....",
    "...ohhhhhhhho...",
    "..ohhbaAaAabho..",
    "..obbbaAAAabbo..",
    "..obbaAobAAabo..",
    "..obsaAobAAabo..",
    "..obsaAAAAAabo..",
    "..obsaAobAAabo..",
    "..obsaAobAAabo..",
    "..obssaAAAabbo..",
    "..obsssaAabbbo..",
    "..obsssssbbbbo..",
    "..osssssssssso..",
    "...oooooooooo...",
    "................",
])

icon("enchant_dust", "crystal", "fleece", [        # sypki pyl z iskrami
    "..A.............",
    "......A....A....",
    "...A.......A....",
    ".......A........",
    "....A.......A...",
    "................",
    "......oooo......",
    ".....ohhhhoo....",
    "...oohhbbbbho...",
    "..ohhbbbbbbbho..",
    ".ohbbbbbbbbbbho.",
    "obbbbbbbbbbbbbbo",
    "obsbbbbbbbbbbsbo",
    ".obssbbbbbbssbo.",
    "..oossssssssoo..",
    "....oooooooo....",
])

icon("staff_focus", "arcane", "gold", [            # kula-ognisko w zlotym pierscieniu
    "................",
    "....oaaaaaao....",
    "..ooaAAAAAAaoo..",
    ".oaAoooooooоAao.".replace("о", "o"),
    ".oaAohhhhhhoAao.",
    "oaAoohhbbbbhoaAo",
    "oaAohhbbbbbbhaAo",
    "oaAohbbbbbbbbaAo",
    "oaAohbbbbbbbbaAo",
    "oaAoobbbbbbsoaAo",
    ".oaAobsbbbsboAo.",
    ".oaAoobsssboaAo.",
    "..ooaAoooooAaoo.",
    "....oaaaaaao....",
    "................",
    "................",
])

# ============================ BUDOWNICTWO ============================
icon("mortar", "clay", "wood", [                   # KIELNIA z zaprawa, trzonek w LEWO w gore
    # (w prawym gornym rogu siedzi plakietka tieru - tam nie ma czego stawiac).
    "................",
    ".oo.............",
    ".oao............",
    ".oaAo...........",
    "..oaAo..........",
    "...oaAo.........",
    "....oaAoooooo...",
    ".....oAhhhhhhoo.",
    "......ohbbbbbbho",
    "......ohbbbbbbho",
    ".......obbbbbho.",
    "........obbbho..",
    ".........obho...",
    "..........oo....",
    "................",
    "................",
])

icon("carved_stone", "stone", "gold", [            # blok z reliefem (rozeta)
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    ".obbooaaaaoobbo.",
    ".obboaAhhAaobbo.",
    ".obboaAhhAaobbo.",
    ".obsoaahhaaobbo.",
    ".obsoaahhaaobso.",
    ".obsboaAAaobsbo.",
    ".obssbooooobsbo.",
    ".obsssbbbbsssbo.",
    ".ossssssssssso..",
    "..oooooooooooo..",
    "................",
    "................",
])

icon("support_beam", "oak", "steel", [             # belka z ZASTRZALEM (nie prostokat)
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    ".ossssssssssso..",
    "..ooaAoooooooo..",
    "...oaAo.........",
    "...ohbAo........",
    "....ohbAo.......",
    "....ohbbAo......",
    ".....ohbbAo.....",
    ".....ohbbo......",
    "......ohbbo.....",
    "......oooooo....",
    "................",
    "................",
])

icon("reinforced_block", "stone", "steel", [       # blok spiety DWIEMA opaskami z nitami.
    # Poprzednia wersja byla wyblakla - ciemniejszy kamien i wyrazne opaski daja kontrast.
    "................",
    "..oooooooooooo..",
    ".ohhhhhhhhhhhho.",
    ".obbbbbbbbbbbbo.",
    ".oooooooooooooo.",
    ".oaAaaaaaaaaAao.",
    ".oAAAAAAAAAAAAo.",
    ".oooooooooooooo.",
    ".obsbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".oooooooooooooo.",
    ".oaAaaaaaaaaAao.",
    ".oAAAAAAAAAAAAo.",
    ".oooooooooooooo.",
    ".ossssssssssso..",
    "..oooooooooooo..",
])

icon("decorative_fitting", "gold", "oak", [        # ozdobne okucie - zawijas
    "................",
    "....oooo........",
    "...ohhhho.......",
    "..ohbooobho.....",
    "..obo...obho....",
    "..obo....obo....",
    "..obo....obo....",
    "...obo..obo.....",
    "....oboobo......",
    ".....obbo.......",
    "....obbbbo......",
    "...obbooobo.....",
    "..obbo...obo....",
    "..obo.....obo...",
    "..oho......oo...",
    "...o............",
])

# ============================ KUCHNIA ============================
icon("trail_ration", "dry", "leather", [           # zawiniatko przewiazane sznurkiem
    "................",
    "................",
    "...ooooooooo....",
    "..ohhhhahhhho...",
    "..ohbbbabbbbo...",
    ".ohbbbbabbbbho..",
    ".obbbbbabbbbbo..",
    ".oaaaaaaaaaaao..",
    ".oAAAAAAAAAAAo..",
    ".obbbbbabbbbbo..",
    ".obsbbbabbbbbo..",
    ".obssbbabbbbbo..",
    ".obsssbabbbsbo..",
    "..osssssssssо...".replace("о", "o"),
    "...ooooooooo....",
    "................",
])

icon("fisher_stew", "shell", "fish", [             # miska z rybim ogonem
    "................",
    "..........oo....",
    ".......ooohho...",
    "......oaAaAbo...",
    "....oooaAaAoo...",
    "..oooaaaaaaooo..",
    ".ohhaaaaaaaahho.",
    "ohhbaaaaaaaabhho",
    "obbbbaaaaaabbbbo",
    "obsbbbbbbbbbbsbo",
    ".obssbbbbbbssbo.",
    "..oobsssssboo...",
    "....oosssoo.....",
    "......oooo......",
    "................",
    "................",
])

icon("hunter_roast", "meat", "bone", [             # pieczen NA KOSCI (kosc wystaje z obu stron).
    # Sama brazowa bryla nie mowila, ze to mieso z polowania.
    "................",
    "................",
    "..o..........o..",
    ".oao........oao.",
    ".oaAo......oaAo.",
    "..oaAooooooaAo..",
    "...ohhhhhhhho...",
    "..ohhbbbbbbhho..",
    ".ohbbbbbbbbbbho.",
    ".obbbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    ".obssbbbbbbbbbo.",
    "..obsssbbbbbbo..",
    "...oossssssoo...",
    ".....oooooo.....",
    "................",
])

icon("harvest_feast", "bread", "green", [          # polmisek z chlebem i jarzynami
    "................",
    "................",
    "......oooo......",
    "....ooaAAaoo....",
    "..ooaAaaaaAaoo..",
    ".ohhhbbbbbbhho..",
    "ohhbbbbbbbbbbho.",
    "ohbbbbbbbbbbbho.",
    "obbbbbbbbbbbbbo.",
    "oooooooooooooooo",
    "ohhhhhhhhhhhhhho",
    "obbbbbbbbbbbbbbo",
    ".ossssssssssss o".replace(" ", "."),
    "..oooooooooooo..",
    "................",
    "................",
])

icon("master_banquet", "gold", "bread", [          # polmisek pod KLOSZEM
    "................",
    ".......oo.......",
    "......oaAo......",
    ".....ooaaoo.....",
    "...oohhhhhhoo...",
    "..ohhhbbbbbhho..",
    ".ohhbbbbbbbbbho.",
    ".obbbbbbbbbbbbo.",
    "obbbbbbbbbbbbbbo",
    "obsbbbbbbbbbbsbo",
    "oooooooooooooooo",
    "oaAAAAAAAAAAAAao",
    "oaaaaaaaaaaaaaao",
    ".ossssssssssss o".replace(" ", "."),
    "..oooooooooooo..",
    "................",
])


def main():
    os.makedirs(OUT, exist_ok=True)
    print(f"ikon w partii 3: {len(ICONS)}")
    assert len(ICONS) == 36, f"oczekiwano 36, jest {len(ICONS)}"
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
    sheet.save("/tmp/ikony-work/sheet3.png")
    print("arkusz: /tmp/ikony-work/sheet3.png")


if __name__ == "__main__":
    main()
