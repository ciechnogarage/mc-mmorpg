#!/usr/bin/env python3
"""Partia 5: pozostale cztery PROF_* poza material/tool/bizuteria - czesci stroju
(belt, gloves) i dwa kostury (staff.yml).

Te przedmioty nie byly w pierwotnym zleceniu, ale to jedyne PROF_*, ktore zostalyby
z vanillowa ikona. Domkniecie calej przestrzeni PROF_ jest tansze teraz niz osobna
runda pozniej.

UWAGA NA NAZWY: Nexo auto-wykrywa w ID slowa slotow zbroi (helmet/chestplate/
leggings/boots/elytra) i wstrzykuje komponent 'equippable', ktory siega po warstwy
zbroi, ktorych nie dostarczamy. Zadne ID ponizej takiego slowa nie zawiera
(gloves/belt sa bezpieczne).

Kostur przebudzony powtarza sylwetke nieprzebudzonego - ta sama zasada co przy
narzedziach mistrzowskich: gracz ma widziec ten sam przedmiot w lepszej wersji.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_icons import PAL, mk, render  # noqa: E402
import generate_icons2  # noqa: E402,F401
import generate_icons3  # noqa: E402,F401
import tier as TIER  # noqa: E402
from classify_tiers import scan as scan_recipes, tier_of  # noqa: E402

OUT = os.environ.get(
    "OUT", "/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/item/profesje")

ICONS = {}


def icon(name, tone, accent, rows):
    ICONS[name] = (mk(tone, accent), rows)


icon("traveler_belt", "leather", "gold", [          # pas z klamra
    "................",
    "................",
    "................",
    "....oooooooooo..",
    "..oohhhhhhhhhho.",
    ".ohhbbbbbbbbbbo.",
    "ohaAaobbbbbbbbo.",
    "ohaAAaobbbbbbbo.",
    "ohaAAaobbbbbbbo.",
    "ohaAaobsbbbbbbo.",
    ".ossbbsssssssso.",
    "..oossssssssoo..",
    "....oooooooo....",
    "................",
    "................",
    "................",
])

icon("leather_gloves", "leather", "dry", [          # rekawica
    "................",
    "...oo..oo..oo...",
    "..ohho.ohho.oho.",
    "..ohbhoohbhoohbo",
    "ooohbbhhbbbhhbbo",
    "ohhobbbbbbbbbbbo",
    "ohbhbbbbbbbbbbbo",
    "ohbbbbbbbbbbbbbo",
    ".obbbbbbbbbbbbo.",
    ".obsbbbbbbbbbbo.",
    "..obssbbbbbbbbo.",
    "..oaaaaaaaaaaao.",
    "..oAAAAAAAAAAAo.",
    "...ossssssssso..",
    "....oooooooooo..",
    "................",
])

icon("unawakened_staff", "oak", "stone", [          # zwykly kostur, glowica w lewym gornym rogu
    "..oooo..........",
    ".oaaaao.........",
    ".oaAAao.........",
    ".oaaaao.........",
    "..ohbo..........",
    "...ohbo.........",
    "....ohbo........",
    ".....ohbo.......",
    "......ohbo......",
    ".......ohbo.....",
    "........ohbo....",
    ".........ohbo...",
    "..........ohbo..",
    "...........ohbo.",
    "............obso",
    ".............oo.",
])

icon("awakened_staff", "oak", "arcane", [           # ta sama sylwetka, glowica ROZSWIETLONA
    ".A.oooo......A..",
    ".oaaaao.........",
    "AoaAAao..A......",
    ".oaaaao.........",
    "..ohbo.....A....",
    "...ohbo.........",
    "....ohbo........",
    ".....ohbo.......",
    "......ohbo......",
    ".......ohbo.....",
    "........ohbo....",
    ".........ohbo...",
    "..........ohbo..",
    "...........ohbo.",
    "............obso",
    ".............oo.",
])


def main():
    os.makedirs(OUT, exist_ok=True)
    print(f"ikon w partii 5: {len(ICONS)}")
    assert len(ICONS) == 6, f"oczekiwano 6, jest {len(ICONS)}"
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
    sheet = Image.new("RGBA", (S * cols, S), (38, 38, 46, 255))
    for i, (name, (im, t)) in enumerate(built.items()):
        sheet.alpha_composite(im.resize((S, S), Image.NEAREST), (i * S, 0))
    sheet.save("/tmp/ikony-work/sheet5.png")
    print("arkusz: /tmp/ikony-work/sheet5.png", list(built))


if __name__ == "__main__":
    main()
