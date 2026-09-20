#!/usr/bin/env python3
"""
Ikony kreatora postaci — recznie ulozony pixel art 16x16.

Kazda ikona to siatka znakow + paleta. Przy GUI scale 3 item renderuje sie 48x48 px,
wiec 16x16 skalowane nearest daje ostry, czytelny obrazek.

Podpiete przez komponent item_model (1.21.4+), NIE przez CustomModelData — dzieki temu
nie ma kolizji numerow z lootem ani z Nexo. Wlasna przestrzen nazw 'cc', zeby Nexo nie
zarzadzalo tymi plikami przy regeneracji packa.
  assets/cc/textures/item/<name>.png
  assets/cc/models/item/<name>.json
  assets/cc/items/<name>.json          <- definicja itemu (nowy format 1.21.4+)
Java: meta.setItemModel(NamespacedKey.fromString("cc:<name>"))
"""
import json, os

# ---------------------------------------------------------------- rasy (popiersia)
HUMAN = [
    "................",
    "................",
    ".....HHHHHH.....",
    "....HHHHHHHH....",
    "...HHSSSSSSHH...",
    "...HSSSSSSSSH...",
    "...HSEESSEESH...",
    "...HSSSSSSSSH...",
    "....SSSMMSSS....",
    ".....SSSSSS.....",
    "......SSSS......",
    "....CCCCCCCC....",
    "...CCCCCCCCCC...",
    "..CCCCAAAACCCC..",
    "..CCCCAAAACCCC..",
    "..CCCCCCCCCCCC..",
]
ORC = [
    "................",
    "................",
    ".....HHHHHH.....",
    "....HHHHHHHH....",
    "...HHSSSSSSHH...",
    "...HSSSSSSSSH...",
    "...HSEESSEESH...",
    "...HSSSSSSSSH...",
    "...TSSSMMSSST...",
    ".....SSSSSS.....",
    "......SSSS......",
    "....CCCCCCCC....",
    "...CCCCCCCCCC...",
    "..CCCCAAAACCCC..",
    "..CCCCAAAACCCC..",
    "..CCCCCCCCCCCC..",
]
ELF = [
    "................",
    "................",
    ".....HHHHHH.....",
    "....HHHHHHHH....",
    "..SHHSSSSSSHHS..",
    ".SSHSSSSSSSSHSS.",
    "..SHSEESSEESHS..",
    "...HSSSSSSSSH...",
    "....SSSMMSSS....",
    ".....SSSSSS.....",
    "......SSSS......",
    "....CCCCCCCC....",
    "...CCCCCCCCCC...",
    "..CCCCAAAACCCC..",
    "..CCCCAAAACCCC..",
    "..CCCCCCCCCCCC..",
]
UNDEAD = [
    "................",
    "................",
    ".....HHHHHH.....",
    "....HHHHHHHH....",
    "...HHSSSSSSHH...",
    "...HSSSSSSSSH...",
    "...HSEESSEESH...",
    "...HSSSSSSSSH...",
    "....SSMMMMSS....",
    ".....SSSSSS.....",
    "......SSSS......",
    "....CCCCCCCC....",
    "...CCC.CC.CCC...",
    "..CCCCAAAACCCC..",
    "..CCC.CCCC.CCC..",
    "..CCCCCC.CCCCC..",
]

# ---------------------------------------------------------------- klasy (symbole)
WARRIOR = [
    "................",
    "..EEEEEEEEEEEE..",
    "..EDDDDDDDDDDE..",
    "..EDDDDAADDDDE..",
    "..EDDDDAADDDDE..",
    "..EDDAAAAAADDE..",
    "..EDDAAAAAADDE..",
    "..EDDDDAADDDDE..",
    "..EDDDDAADDDDE..",
    "..EDDDDAADDDDE..",
    "...EDDDDDDDDE...",
    "....EDDDDDDE....",
    ".....EDDDDE.....",
    "......EDDE......",
    ".......EE.......",
    "................",
]
MAGE = [
    "................",
    "......OOOO......",
    ".....OOLLOO.....",
    ".....OLLLLO.....",
    ".....OOLLOO.....",
    "......OOOO......",
    ".......SS.......",
    ".......SS.......",
    "......GSSG......",
    ".......SS.......",
    ".......SS.......",
    ".......SS.......",
    "......GSSG......",
    ".......SS.......",
    ".......SS.......",
    "................",
]
ACOLYTE = [
    "................",
    ".......AA.......",
    ".......AA.......",
    ".R.....AA.....R.",
    "..R....AA....R..",
    "...R..AAAA..R...",
    "...AAAAAAAAAA...",
    "...AAAAAAAAAA...",
    "...R..AAAA..R...",
    "..R....AA....R..",
    ".R.....AA.....R.",
    ".......AA.......",
    ".......AA.......",
    ".......AA.......",
    ".......AA.......",
    "................",
]
ROGUE = [
    "................",
    "..B..........B..",
    "..BB........BB..",
    "...BB......BB...",
    "....BB....BB....",
    ".....BB..BB.....",
    "......BBBB......",
    ".......BB.......",
    "......GGGG......",
    ".....GG..GG.....",
    "....HH....HH....",
    "...HH......HH...",
    "..HH........HH..",
    "..H..........H..",
    "................",
    "................",
]

# ---------------------------------------------------------------- przyciski
CONFIRM = [
    "................",
    ".....GGGGGG.....",
    "...GGEEEEEEGG...",
    "..GGEELLLLEEGG..",
    "..GEELLLLLLEEG..",
    ".GGELLLLLLLLEGG.",
    ".GEELLLLLLLLEEG.",
    ".GEELLLLLLLLEEG.",
    ".GGELLLLLLLLEGG.",
    "..GEELLLLLLEEG..",
    "..GGEELLLLEEGG..",
    "...GGEEEEEEGG...",
    ".....GGGGGG.....",
    "................",
    "................",
    "................",
]
SCROLL = [
    "................",
    "................",
    "...PPPPPPPPPP...",
    "..PSSSSSSSSSSP..",
    "..PSIIIIIIIISP..",
    "..PSSSSSSSSSSP..",
    "..PSIIIIIISSSP..",
    "..PSSSSSSSSSSP..",
    "..PSIIIIIIIISP..",
    "..PSSSSSSSSSSP..",
    "..PSIIIISSSSSP..",
    "..PSSSSSSSSSSP..",
    "...PPPPPPPPPP...",
    "................",
    "................",
    "................",
]
SKIN_H = (226, 180, 140)
ICONS = {
    "race_czlowiek": (HUMAN, {"H": (94, 60, 34), "S": SKIN_H, "E": (38, 30, 26),
                              "M": (156, 96, 88), "C": (58, 86, 142), "A": (214, 182, 92)}),
    "race_ork":      (ORC,   {"H": (32, 28, 26), "S": (108, 146, 78), "E": (206, 72, 48),
                              "M": (62, 46, 42), "T": (238, 234, 214),
                              "C": (98, 68, 42), "A": (176, 166, 140)}),
    "race_elf":      (ELF,   {"H": (230, 206, 138), "S": (240, 218, 200), "E": (72, 132, 98),
                              "M": (188, 138, 138), "C": (66, 122, 82), "A": (206, 194, 142)}),
    "race_nieumarly":(UNDEAD,{"H": (58, 58, 66), "S": (150, 162, 158), "E": (94, 212, 198),
                              "M": (52, 54, 58), "C": (62, 58, 68), "A": (114, 108, 120)}),
    "class_wojownik":(WARRIOR,{"E": (54, 58, 68), "D": (128, 140, 158), "A": (226, 188, 88)}),
    "class_mag":     (MAGE,  {"O": (104, 66, 168), "L": (206, 168, 248), "S": (110, 78, 48),
                              "G": (226, 188, 88)}),
    "class_akolita": (ACOLYTE,{"A": (232, 196, 96), "R": (250, 238, 178)}),
    "class_ninja":   (ROGUE, {"B": (176, 186, 200), "G": (216, 180, 88), "H": (72, 52, 40)}),
    "ui_confirm":    (CONFIRM,{"G": (226, 188, 88), "E": (32, 86, 52), "L": (96, 196, 110)}),
    "ui_name":       (SCROLL,{"P": (166, 132, 86), "S": (232, 214, 172), "I": (86, 66, 46)}),
}


def render(grid, palette):
    from PIL import Image
    for i, row in enumerate(grid):
        assert len(row) == 16, f"wiersz {i} ma {len(row)} znakow zamiast 16: {row!r}"
    assert len(grid) == 16, f"siatka ma {len(grid)} wierszy zamiast 16"
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                assert ch in palette, f"brak koloru dla '{ch}'"
                img.putpixel((x, y), palette[ch] + (255,))
    return img


if __name__ == "__main__":
    # Domyslnie ./out, nie produkcyjny pack Nexo — patrz komentarz w gen_creator_art.py.
    pack = os.environ.get("PACK", os.path.join(os.getcwd(), "out", "pack", "assets", "cc"))
    for sub in ("textures/item", "models/item", "items"):
        os.makedirs(f"{pack}/{sub}", exist_ok=True)
    for name, (grid, palette) in ICONS.items():
        render(grid, palette).save(f"{pack}/textures/item/{name}.png")
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": f"cc:item/{name}"}},
                  open(f"{pack}/models/item/{name}.json", "w"), indent=2)
        json.dump({"model": {"type": "minecraft:model", "model": f"cc:item/{name}"}},
                  open(f"{pack}/items/{name}.json", "w"), indent=2)
        print(f"  cc:{name}")
    print(f"gotowe: {len(ICONS)} ikon 16x16")
