"""Kolory map Minecrafta (MapColor, 1.21) i przypisanie blokow powierzchni do nich.

Bajt koloru na mapie = baza*4 + odcien; odcien 0..3 mnozy RGB przez 180/220/255/135 (/255).
Tabela blokow obejmuje to, co faktycznie lezy na powierzchni `world` (teren.py wypisuje licznik).
"""
BAZA = [None, (127,178,56), (247,233,163), (199,199,199), (255,0,0), (160,160,255), (167,167,167),
 (0,124,0), (255,255,255), (164,168,184), (151,109,77), (112,112,112), (64,64,255), (143,119,72),
 (255,252,245), (216,127,51), (178,76,216), (102,153,216), (229,229,51), (127,204,25), (242,127,165),
 (76,76,76), (153,153,153), (76,127,153), (127,63,178), (51,76,178), (102,76,51), (102,127,51),
 (153,51,51), (25,25,25), (250,238,77), (92,219,213), (74,128,255), (0,217,58), (129,86,49),
 (112,2,0), (209,177,161), (159,82,36), (149,87,108), (112,108,138), (186,133,36), (103,117,53),
 (160,77,78), (57,41,35), (135,107,98), (87,92,92), (122,73,88), (76,62,92), (76,50,35), (76,82,42),
 (142,60,46), (37,22,16), (189,48,49), (148,63,97), (92,25,29), (22,126,134), (58,142,140),
 (86,44,62), (20,180,133), (100,100,100), (216,175,147), (127,167,150)]
MNOZ = [180, 220, 255, 135]

def rgb(bajt):
    b, o = divmod(bajt, 4)
    r, g, bl = BAZA[b]
    m = MNOZ[o]
    return (r * m // 255, g * m // 255, bl * m // 255)

# blok -> baza; dopasowanie po pelnej nazwie, potem po koncowkach
DOKLADNE = {
 "grass_block": 1, "sand": 2, "sandstone": 2, "cobweb": 3, "lava": 4, "fire": 4,
 "short_grass": 7, "tall_grass": 7, "fern": 7, "large_fern": 7, "bamboo": 7, "vine": 7,
 "big_dripleaf": 7, "bush": 7, "wildflowers": 7, "snow": 8, "snow_block": 8, "clay": 9,
 "dirt": 10, "coarse_dirt": 10, "granite": 10, "rooted_dirt": 10, "leaf_litter": 10,
 "stone": 11, "gravel": 11, "andesite": 11, "cobblestone": 11, "mossy_cobblestone": 11,
 "rail": 11, "water": 12, "dead_bush": 13, "oak_log": 13, "short_dry_grass": 18,
 "tall_dry_grass": 18, "diorite": 14, "terracotta": 15, "red_sand": 15, "red_sandstone": 15,
 "melon": 19, "dark_oak_planks": 26, "moss_block": 27, "moss_carpet": 27, "sea_pickle": 27,
 "podzol": 34, "netherrack": 35, "white_terracotta": 36, "orange_terracotta": 37,
 "yellow_terracotta": 40, "light_gray_terracotta": 44, "brown_terracotta": 48,
 "red_terracotta": 50, "deepslate": 59,
}
KONCOWKI = [("_leaves", 7), ("_ore", 11), ("_log", 13), ("_planks", 13), ("_wood", 13),
            ("_flower", 7), ("_sapling", 7), ("_carpet", 3), ("_wool", 3)]
KWIATY = {"poppy", "dandelion", "cornflower", "azure_bluet", "oxeye_daisy", "allium", "blue_orchid",
          "lily_of_the_valley", "pink_petals"}

# budowle (Stolica): kolor materialu; schody/plyty/murki -> material bazowy
MATERIALY = [("deepslate", 59), ("blackstone", 29), ("tuff", 45), ("mud_brick", 44), ("andesite", 11),
             ("diorite", 14), ("calcite", 36), ("stone_brick", 11), ("cobblestone", 11), ("stone", 11),
             ("weathered", 56), ("oxidized", 58), ("exposed", 44), ("copper", 15),
             ("dark_oak", 26), ("spruce", 34), ("birch", 2), ("jungle", 10), ("oak", 13), ("mangrove", 28),
             ("cherry", 36), ("bamboo", 18), ("amethyst", 24), ("obsidian", 29), ("quartz", 14),
             ("sandstone", 2), ("brick", 28), ("lantern", 6), ("hay", 18), ("campfire", 26)]
WELNA = {"white": 8, "orange": 15, "magenta": 16, "light_blue": 17, "yellow": 18, "lime": 19, "pink": 20,
         "gray": 21, "light_gray": 22, "cyan": 23, "purple": 24, "blue": 25, "brown": 26, "green": 27,
         "red": 28, "black": 29}

def baza(nazwa):
    if not nazwa: return 0                      # pustka (VoidGen) / brak danych
    n = nazwa.replace("minecraft:", "")
    if n in DOKLADNE: return DOKLADNE[n]
    for kol, b in WELNA.items():
        if n in (kol + "_wool", kol + "_carpet", kol + "_concrete"): return b
    for rdzen, b in MATERIALY:
        if rdzen in n and not n.endswith("_leaves"): return b
    if n in KWIATY: return 7
    for k, b in KONCOWKI:
        if n.endswith(k): return b
    return 11
