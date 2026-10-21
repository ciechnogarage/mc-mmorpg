# -*- coding: utf-8 -*-
"""Profesje v2 — ZRODLO PRAWDY tresci (decyzje: PROJEKT.md). Generator: generuj.py, kontrola: audyt.py.

Konwencje id:
  T1 = istniejace id v1 (PROF_ORE_CHUNK ...) — ekwipunki graczy i receptury Chowancow dzialaja dalej.
  T2..T10 = PROF_T<t>_<KLUCZ>. Rzadkie z tresci: PROF_A<t>_PYL / _ESENCJA / _SERCE.
  Narzedzia: PROF_<RODZAJ>_<t>. Mikstury/eliksiry/posilki/klejnoty/runy: PROF_<KLUCZ>_<t>.
  Sprzet z rzemiosla (bazy v2): RZM_<id bazy v2>.
"""

ROMAN = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X']


def g(t):
    """XP jednej akcji zbierackiej tieru t (PROJEKT 2)."""
    return 10 * 1.25 ** (t - 1)


def akcje(l):
    """Liczba akcji biezacego tieru na poziom l -> l+1."""
    return 20 + 2 * l


def tier_poziomu(l):
    return min(10, max(1, (l - 1) // 10 + 1))


def poziom_startu(t):
    return 10 * t - 9


KAR_NIZSZEGO_TIERU = 0.5          # XP x 0.5^(roznica tierow)
XP_RZEMIOSLA = 0.9                # XP receptury = ekwiwalent surowcowy x g(T) x 0.9
XP_ZLECENIA = 1.5
WARTOSC_SUROWCA_T1 = 1.0          # waluta; ok. 0,2/s wysilku jak tools/handel (akcja ~5 s)
WZROST_WARTOSCI = 1.25            # na tier, jak g()
SKUP = 0.4                        # skup placi 40% wartosci (tools/handel)

# ---------------------------------------------------------------- KRAINY (akty planu 100 v2)
# t: (nazwa, paleta materialow: s/b/h kamienia krainy, akcent: s/b/h)
KRAINY = {
    1: ('Zielona Granica', ((74, 96, 58), (104, 134, 76), (146, 178, 104)), ((150, 112, 26), (201, 162, 39), (240, 208, 110))),
    2: ('Archipelag Słonych Mgieł', ((70, 98, 112), (104, 138, 152), (160, 192, 204)), ((196, 196, 188), (226, 226, 218), (248, 248, 244))),
    3: ('Złote Piaski', ((150, 112, 58), (198, 160, 92), (236, 206, 140)), ((172, 92, 40), (214, 128, 60), (244, 172, 98))),
    4: ('Korzenie Świata', ((60, 50, 44), (92, 78, 66), (128, 112, 96)), ((96, 70, 142), (136, 104, 190), (182, 152, 230))),
    5: ('Mroźna Korona', ((118, 148, 172), (164, 196, 218), (214, 234, 246)), ((70, 120, 170), (100, 160, 214), (160, 206, 244))),
    6: ('Srebrna Knieja', ((52, 60, 92), (82, 94, 136), (126, 140, 186)), ((168, 176, 192), (204, 212, 226), (238, 242, 250))),
    7: ('Ziemie Paktu', ((92, 86, 80), (132, 124, 116), (172, 164, 156)), ((128, 30, 36), (176, 52, 56), (220, 98, 92))),
    8: ('Popielna Korona', ((56, 52, 54), (88, 82, 84), (124, 118, 118)), ((196, 82, 28), (236, 128, 44), (252, 186, 86))),
    9: ('Podniebie', ((124, 158, 176), (170, 204, 220), (222, 240, 248)), ((54, 150, 156), (80, 196, 196), (150, 234, 228))),
    10: ('Kres', ((36, 28, 48), (62, 48, 82), (98, 80, 126)), ((160, 60, 196), (204, 100, 236), (236, 170, 252))),
}

# ---------------------------------------------------------------- SUROWCE ZBIERACKIE
# profesja -> rodzaj narzedzia, rodzaj zrodla (wezel/lowisko/zwloki), ksztalt ikon glowny/poboczny/rzadki
PROF_ZBIERACKIE = {
    'mining':      ('kilof', 'wezel', 'ore_chunk', 'rough_crystal', 'raw_gem'),
    'woodworking': ('siekiera', 'wezel', 'oak_timber', 'spruce_resin', None),
    'herbalism':   ('sierp', 'wezel', 'medicinal_herb', 'moonleaf', None),
    'farming':     ('motyka', 'wezel', 'fresh_harvest', 'flax_fiber', None),
    'fishing':     ('wedka', 'lowisko', 'raw_catch', 'clam', 'pearl'),
    'hunting':     ('noz', 'zwloki', 'beast_scrap', 'beast_bone', 'monster_trophy'),
}
# (glowny, poboczny, rzadki) — nazwy dla T2..T10; T1 = istniejace id (nizej)
NAZWY = {
 'mining': {
  2: ('Bryła solnego urobku', 'Kryształ soli morskiej', 'Surowy klejnot mgieł'),
  3: ('Bryła złotonośnego urobku', 'Bursztyn pustynny', 'Surowy klejnot słońca'),
  4: ('Bryła głębinowego urobku', 'Kryształ echa', 'Surowy klejnot korzeni'),
  5: ('Bryła szronowego urobku', 'Kryształ szronu', 'Surowy klejnot zimy'),
  6: ('Bryła księżycowego urobku', 'Kamień księżycowy', 'Surowy klejnot pełni'),
  7: ('Bryła chorągwianego urobku', 'Krzemień bitewny', 'Surowy klejnot paktu'),
  8: ('Bryła żarowego urobku', 'Obsydian popielny', 'Surowy klejnot żaru'),
  9: ('Bryła wietrznego urobku', 'Kryształ burzy', 'Surowy klejnot nieba'),
  10: ('Bryła urobku rysy', 'Odłamek rysy', 'Surowy klejnot kresu')},
 'woodworking': {
  2: ('Drewno namorzynowe', 'Żywica solna', None), 3: ('Drewno akacji pustynnej', 'Żywica bursztynowa', None),
  4: ('Drewno korzenne', 'Sok korzeni', None), 5: ('Drewno śnieżnego modrzewia', 'Żywica lodowa', None),
  6: ('Drewno srebrnej brzozy', 'Sok księżycowy', None), 7: ('Drewno dębu sztandarowego', 'Smoła obozowa', None),
  8: ('Drewno opalone', 'Żywica żaru', None), 9: ('Drewno chmurnej sosny', 'Żywica wiatru', None),
  10: ('Drewno z rysy', 'Żywica echa', None)},
 'herbalism': {
  2: ('Morska trawa', 'Soliród', None), 3: ('Pustynny piołun', 'Kwiat słońca', None),
  4: ('Bladonóżka jaskiniowa', 'Mech echa', None), 5: ('Szronowiec', 'Lodowa paproć', None),
  6: ('Srebrnik księżycowy', 'Nocny powój', None), 7: ('Krwawnik polowy', 'Chwast pobojowisk', None),
  8: ('Popielny oset', 'Żarnik', None), 9: ('Chmurnik', 'Wietrzna koniczyna', None),
  10: ('Ziele rysy', 'Kwiat echa', None)},
 'farming': {
  2: ('Wodorost jadalny', 'Włókno konopi morskich', None), 3: ('Daktyle', 'Włókno palmowe', None),
  4: ('Bulwy jaskiniowe', 'Włókno grzybni', None), 5: ('Zimowe żyto', 'Włókno śnieżnego lnu', None),
  6: ('Księżycowe jagody', 'Nić księżycowa', None), 7: ('Proso obozowe', 'Konopie wojenne', None),
  8: ('Popielne bulwy', 'Włókno popielne', None), 9: ('Chmurne gruszki', 'Puch wiatrowy', None),
  10: ('Owoc rysy', 'Nić echa', None)},
 'fishing': {
  2: ('Śledź mgielny', 'Krab przybrzeżny', 'Perła mgieł'), 3: ('Karp oazy', 'Rak piaskowy', 'Złoty łuskowiec'),
  4: ('Ślepa ryba jaskiń', 'Węgorz echa', 'Perła ciemności'), 5: ('Dorsz lodowy', 'Kryl szronowy', 'Perła szronu'),
  6: ('Pstrąg srebrny', 'Rak nocny', 'Perła księżycowa'), 7: ('Szczupak fosowy', 'Raki fosy', 'Perła fosy'),
  8: ('Żarołusk', 'Krab popielny', 'Perła żaru'), 9: ('Latająca ryba', 'Meduza wiatru', 'Perła burzy'),
  10: ('Ryba rysy', 'Węgorz echa rysy', 'Perła kresu')},
 'hunting': {
  2: ('Skóra foki', 'Kość morska', 'Trofeum mgieł'), 3: ('Pancerz skorpiona', 'Kość pustynna', 'Żądło pustyni'),
  4: ('Skóra ryjkowca', 'Kość jaskiniowa', 'Trofeum ciemności'), 5: ('Futro lodowe', 'Kość kurhanu', 'Kieł zimy'),
  6: ('Futro srebrne', 'Kość księżycowa', 'Trofeum pełni'), 7: ('Skóra wierzchowca', 'Kość pobojowiska', 'Trofeum chorążego'),
  8: ('Łuska salamandry', 'Kość popielna', 'Gruczoł żaru'), 9: ('Skóra gryfa', 'Kość lotna', 'Pióro burzy'),
  10: ('Skóra z rysy', 'Kość echa', 'Trofeum kresu')},
}
KLUCZE = {'mining': ('UROBEK', 'KRYSZTAL', 'KLEJNOT'), 'woodworking': ('DREWNO', 'ZYWICA', None),
          'herbalism': ('ZIOLO', 'ZIELE', None), 'farming': ('PLON', 'WLOKNO', None),
          'fishing': ('RYBA', 'DAR_WODY', 'PERLA'), 'hunting': ('SKORA', 'KOSC', 'TROFEUM')}
# T1: istniejace id (glowny, poboczny, rzadki)
T1 = {'mining': ('PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL', 'PROF_RAW_GEM'),
      'woodworking': ('PROF_OAK_TIMBER', 'PROF_SPRUCE_RESIN', None),
      'herbalism': ('PROF_MEDICINAL_HERB', 'PROF_MOONLEAF', None),
      'farming': ('PROF_FRESH_HARVEST', 'PROF_FLAX_FIBER', None),
      'fishing': ('PROF_RAW_CATCH', 'PROF_CLAM', 'PROF_PEARL'),
      'hunting': ('PROF_BEAST_SCRAP', 'PROF_BEAST_BONE', 'PROF_MONSTER_TROPHY')}
SZANSA_POBOCZNY = 0.15
SZANSA_RZADKI = 0.03

# Wezly T1 dodatkowe (zrodla id, ktorych potrzebuja Chowance/Akt 1). (nexo id, profesja, nazwa wezla, glowny, poboczny)
WEZLY_T1_DODATKOWE = [
    ('wezel_t1_glina', 'mining', 'Gliniasta skarpa', 'PROF_CLAY_LUMP', 'PROF_ROUGH_STONE'),
    ('wezel_t1_grzyby', 'herbalism', 'Krąg grzybów', 'PROF_REDCAP', 'PROF_MEDICINAL_HERB'),
    ('wezel_t1_przyprawy', 'herbalism', 'Dzikie przyprawy', 'PROF_WILD_SPICE', 'PROF_MEDICINAL_HERB'),
    ('wezel_t1_zboze', 'farming', 'Łan zboża', 'PROF_QUALITY_GRAIN', 'PROF_FRESH_HARVEST'),
]
# lowisko T1: dodatkowe rzadkie (Chowance: esencja wody, kucharz v1: rzadka ryba)
LOWISKO_T1_EXTRA = [('PROF_WATER_ESSENCE', 0.02)]
# Istniejace bloki Nexo pakiet3 -> wezly T1 (moga juz stac w swiatach; XP i drop przejmuje plugin)
PAKIET3_JAKO_WEZLY = {
    'ruda_urobku': ('mining', 'PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL'),
    'ruda_urobku_glebinowa': ('mining', 'PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL'),
    'kamien_kamieniolomu': ('mining', 'PROF_ROUGH_STONE', 'PROF_CLAY_LUMP'),
    'zyla_klejnotow': ('mining', 'PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL'),
    'zyla_srebra': ('mining', 'PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL'),
    'zyla_srebra_glebinowa': ('mining', 'PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL'),
    'druza_arkany': ('mining', 'PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL'),
    'serce_gory': ('mining', 'PROF_ORE_CHUNK', 'PROF_ROUGH_CRYSTAL'),
}
NAZWA_WEZLA = {'mining': 'Złoże', 'woodworking': 'Powalony pień', 'herbalism': 'Kępa', 'farming': 'Grządka'}

# ---------------------------------------------------------------- ZWIERZYNA (myslistwo, oprawianie zwlok)
# id moba MythicMobs -> (tier, nadpisanie glownego, nadpisanie rzadkiego)
ZWIERZYNA = {
    'BEAST_FOREST_HARE': (1, None, None), 'BEAST_MOOR_SHEEP': (1, 'PROF_RAW_FLEECE', None),
    'BEAST_WILD_BOAR': (1, None, None), 'BEAST_CAVE_SPIDERLING': (1, None, 'PROF_VENOM_SAC'),
    'BEAST_DIRE_WOLF': (1, None, None), 'BEAST_MARSH_LURKER': (1, None, 'PROF_VENOM_SAC'),
    'BEAST_FROST_BEAR': (1, None, None), 'BEAST_VENOM_STALKER': (1, None, 'PROF_VENOM_SAC'),
    'ELITE_ALPHA_WOLF': (1, None, None), 'L001_WILK': (1, None, None), 'L001_WILK_WATAHA': (1, None, None),
    'L001_WILCZYCA': (1, None, None), 'L001_WILCZE': (1, None, None),
}
SZANSA_RZADKI_ZWLOKI = 0.06       # zwloki to rzadsze zrodlo niz wezel — wieksza szansa

# ---------------------------------------------------------------- RZADKIE Z TRESCI (pkt 3 audytu)
# akt -> (pyl, esencja, serce): pyl z moba aktu 3%, esencja z elity 30%, serce z bossa 100%
RZADKIE = {
    1: ('Pyłek Korzenia', 'Esencja kiełkowania', 'Serce Sadu'),
    2: ('Sól mgieł', 'Esencja przypływu', 'Serce Latarni'),
    3: ('Złoty pył', 'Esencja słońca', 'Serce Skarabeusza'),
    4: ('Pył echa', 'Esencja ciszy', 'Serce Gongu'),
    5: ('Szron wieczny', 'Esencja bezruchu', 'Serce Kurhanu'),
    6: ('Srebrzysty pył', 'Esencja odbicia', 'Serce Pełni'),
    7: ('Proch bitewny', 'Esencja sztandaru', 'Serce Chorążego'),
    8: ('Popiół wieczny', 'Esencja żaru', 'Serce Feniksa'),
    9: ('Pył chmur', 'Esencja wiatru', 'Serce Gryfa'),
    10: ('Nić Echa', 'Esencja rysy', 'Serce Tkacza'),
}
SZANSE_RZADKICH = {'pyl': 0.03, 'esencja': 0.30, 'serce': 1.0}
# waga rzadkich w ekwiwalencie (XP i cena): walka to inny wysilek niz zbieranie; wiecej = receptura z sercem
# dawalaby kilka poziomow naraz (bramka audytu)
EKW_RZADKICH = {'pyl': 5.0, 'esencja': 10.0, 'serce': 20.0}
# podpiecie aktu 1 do istniejacych mobow (pozostale akty: projektanci lokacji, linia Drops:)
RZADKIE_A1_MOBY = {
    'zwykle': ['L001_PNIACZEK', 'L001_WILK', 'L001_WILK_WATAHA', 'L001_KORZENIAK', 'L001_WILCZE', 'BEAST_DIRE_WOLF',
               'BEAST_WILD_BOAR', 'BEAST_MARSH_LURKER', 'BEAST_VENOM_STALKER', 'UNDEAD_GRAVE_CRAWLER',
               'UNDEAD_BONE_ARCHER', 'UNDEAD_PLAGUE_BEARER', 'UNDEAD_CRYPT_GUARD', 'HUMANOID_ROAD_BANDIT',
               'MAGIC_WANDERING_WISP'],
    'elita': ['L001_WILCZYCA', 'L001_DRUID', 'ELITE_ALPHA_WOLF', 'ELITE_SWAMP_HAG', 'ELITE_BONE_CHAMPION',
              'ELITE_TROLL_MAULER', 'ELITE_ARCANE_WRAITH'],
    'boss': ['L001_STARY_KORZEN', 'BOSS_FOREST_WARDEN', 'BOSS_CRYPT_LORD', 'BOSS_VENOM_MATRIARCH'],
}

# ---------------------------------------------------------------- NARZEDZIA
NARZEDZIA = {  # rodzaj -> (nazwa, material vanilla, ksztalt ikony, profesja, kto robi)
    'kilof': ('Kilof górnika', 'IRON_PICKAXE', 'kilof', 'mining', 'smithing'),
    'siekiera': ('Siekiera drwala', 'IRON_AXE', 'lumber_axe', 'woodworking', 'woodworking'),
    'sierp': ('Sierp zielarza', 'IRON_HOE', 'herb_sickle', 'herbalism', 'smithing'),
    'motyka': ('Motyka farmera', 'IRON_HOE', 'farmer_hoe', 'farming', 'smithing'),
    'wedka': ('Wędka rybaka', 'FISHING_ROD', 'fishing_rod', 'fishing', 'woodworking'),
    'noz': ('Nóż myśliwski', 'IRON_SWORD', 'hunter_knife', 'hunting', 'smithing'),
}
WYTRZYMALOSC = lambda t: 150 + 60 * t
NAPRAWA_PROCENT = 0.40
CZAS_BAZOWY = 3.0
CZAS_OPRAWIANIA = 2.5
WYCZERPANIE_S = 45
ZWLOKI_S = 30

# ---------------------------------------------------------------- BUFFY (statystyki MythicLib, plugin)
# id -> (nazwa, grupa, czas s, cooldown s, {stat: (T1, przyrost/tier)}, opis)
BUFFY = {
 'MIKSTURA_LECZNICZA': ('Mikstura lecznicza', 'mikstura', 0, 30, {'leczenie': (6.0, 1.8)}, 'Natychmiast leczy.'),
 'MIKSTURA_MANY': ('Mikstura many', 'mikstura', 0, 30, {'mana': (8.0, 3.0)}, 'Natychmiast przywraca manę.'),
 'ODTRUTKA': ('Odtrutka', 'mikstura', 8, 30, {'oczyszczenie': (1, 0)}, 'Zdejmuje truciznę, obumarcie, spowolnienie i osłabienie; chroni przez 8 s.'),
 'ELIKSIR_SILY': ('Eliksir siły', 'eliksir', 300, 0, {'physical-damage': (4.0, 1.0)}, 'Zwiększa obrażenia fizyczne.'),
 'ELIKSIR_UMYSLU': ('Eliksir umysłu', 'eliksir', 300, 0, {'magic-damage': (3.0, 0.8), 'skill-damage': (3.0, 0.8)}, 'Zwiększa obrażenia magiczne i umiejętności.'),
 'ELIKSIR_SKORY': ('Eliksir żelaznej skóry', 'eliksir', 300, 0, {'damage-reduction': (3.0, 0.6)}, 'Zmniejsza otrzymywane obrażenia.'),
 'ELIKSIR_ZWINNOSCI': ('Eliksir zwinności', 'eliksir', 300, 0, {'dodge-rating': (2.0, 0.5), 'movement-speed': (0.004, 0.0006)}, 'Zwiększa unik i szybkość ruchu.'),
 'OLEJ': ('Olej do broni', 'olej', 600, 0, {'critical-strike-chance': (2.0, 0.4)}, 'Zwiększa szansę na trafienie krytyczne.'),
 'POSILEK_SYTY': ('Sycący posiłek', 'posilek', 1200, 0, {'max-health': (2.0, 0.6)}, 'Zwiększa maksymalne zdrowie.'),
 'POSILEK_ROSOL': ('Krzepiący wywar', 'posilek', 1200, 0, {'health-regeneration': (0.05, 0.02)}, 'Zwiększa regenerację zdrowia.'),
 'POSILEK_PROWIANT': ('Prowiant wędrowca', 'posilek', 1200, 0, {'max-stamina': (2.0, 0.8), 'stamina-regeneration': (0.05, 0.02)}, 'Zwiększa wytrzymałość i jej odnowę.'),
 'UCZTA': ('Uczta drużynowa', 'posilek', 1200, 0, {'max-health': (1.5, 0.5), 'health-regeneration': (0.04, 0.015)}, 'Karmi całą drużynę w promieniu 8 bloków.'),
 'OPATRUNEK': ('Opatrunek', 'opatrunek', 8, 20, {'leczenie_w_czasie': (4.0, 1.5)}, 'Leczy w ciągu 8 s; przerywa go otrzymanie obrażeń.'),
}
GRUPY_BUFFOW = ('mikstura', 'eliksir', 'olej', 'posilek', 'opatrunek')
# T1: posilki reuzywaja ikon/id v1 (bez efektow vanilli)
BUFFY_T1_ID = {'POSILEK_SYTY': 'PROF_HUNTER_ROAST', 'POSILEK_ROSOL': 'PROF_FISHER_STEW',
               'POSILEK_PROWIANT': 'PROF_TRAIL_RATION', 'UCZTA': 'PROF_HARVEST_FEAST'}
OBOZOWISKO = {'czas': 600, 'promien': 8, 'health-regeneration': (0.3, 0.1), 'mana-regeneration': (0.2, 0.08)}

# ---------------------------------------------------------------- KLEJNOTY I RUNY (GEM_STONE, gniazdo Uncolored)
# id -> (nazwa, profesja, statystyka, (T1, przyrost/tier)) — rzad affiksu T1-T2 v2 na poziomie aktu
KLEJNOTY = {
 'KLEJNOT_ZDROWIA': ('Klejnot zdrowia', 'jewelcrafting', 'max-health', (1.5, 0.5)),
 'KLEJNOT_OCHRONY': ('Klejnot ochrony', 'jewelcrafting', 'damage-reduction', (1.5, 0.3)),
 'KLEJNOT_ODNOWY': ('Klejnot odnowy', 'jewelcrafting', 'health-regeneration', (0.08, 0.02)),
 'KLEJNOT_ZWINNOSCI': ('Klejnot zwinności', 'jewelcrafting', 'dodge-rating', (2.5, 0.6)),
 'KLEJNOT_CELNOSCI': ('Klejnot celności', 'jewelcrafting', 'critical-strike-chance', (2.0, 0.3)),
 'RUNA_SILY': ('Runa siły', 'enchanting', 'physical-damage', (3.0, 0.8)),
 'RUNA_MOCY': ('Runa mocy', 'enchanting', 'magic-damage', (3.0, 0.8)),
 'RUNA_ZARU': ('Runa żaru', 'enchanting', 'skill-damage', (3.0, 0.8)),
 'RUNA_GROZY': ('Runa grozy', 'enchanting', 'critical-strike-power', (5.0, 1.2)),
 'RUNA_POSPIECHU': ('Runa pośpiechu', 'enchanting', 'cooldown-reduction', (2.0, 0.4)),
}

# ---------------------------------------------------------------- PRZETWORZONE (polprodukty) per tier
# klucz -> (profesja, nazwa T2+ (wzor z {k} = nazwa krainy w dopelniaczu?), skladniki [(zrodlo, ilosc)], ksztalt, id T1)
# zrodla: 'S:<prof>:g|p|r' = surowiec tieru, 'P:<klucz>' = polprodukt tieru, 'A:pyl|esencja|serce' = rzadki aktu
PRZETWORZONE = {
 'SZTABKA':   ('smithing', 'Sztabka', [('S:mining:g', 3)], 'refined_iron', 'PROF_REFINED_IRON'),
 'OKUCIE':    ('smithing', 'Okucie', [('P:SZTABKA', 1)], 'carved_fitting', None),
 'PLYTA':     ('smithing', 'Płyta metalowa', [('P:SZTABKA', 3)], 'metal_plate', 'PROF_METAL_PLATE'),
 'DESKA':     ('woodworking', 'Deska', [('S:woodworking:g', 3)], 'seasoned_timber', 'PROF_SEASONED_TIMBER'),
 'TRZONEK':   ('woodworking', 'Trzonek', [('P:DESKA', 1)], 'weapon_handle', 'PROF_WEAPON_HANDLE'),
 'TKANINA':   ('tailoring', 'Tkanina', [('S:farming:p', 3)], 'cloth_bolt', 'PROF_CLOTH_BOLT'),
 'SKORA_W':   ('tailoring', 'Skóra wyprawiona', [('S:hunting:g', 3)], 'cured_hide', 'PROF_CURED_HIDE'),
 'CIECIWA':   ('tailoring', 'Cięciwa', [('S:farming:p', 2), ('S:woodworking:p', 1)], 'bowstring', 'PROF_BOWSTRING'),
 'PLACHTA':   ('tailoring', 'Płachta obozowa', [('P:TKANINA', 2), ('S:woodworking:p', 1)], 'robe_lining', None),
 'KAMIEN_O':  ('jewelcrafting', 'Kamień oszlifowany', [('S:mining:p', 2)], 'cut_gem', 'PROF_CUT_GEM'),
 'EKSTRAKT':  ('alchemy', 'Ekstrakt', [('S:herbalism:g', 3)], 'herbal_extract', 'PROF_HERBAL_EXTRACT'),
 'PYL_Z':     ('enchanting', 'Pył zaklęć', [('S:mining:p', 1), ('S:herbalism:p', 1)], 'enchant_dust', 'PROF_ENCHANT_DUST'),
 'KAMIEN_C':  ('building', 'Kamień ciosany', [('S:mining:g', 2)], 'cut_stone', 'PROF_CUT_STONE'),
 'BELKA':     ('building', 'Belka', [('P:DESKA', 2)], 'support_beam', 'PROF_SUPPORT_BEAM'),
 'ZAPRAWA':   ('building', 'Zaprawa', [('P:KAMIEN_C', 1), ('S:woodworking:p', 1)], 'mortar', 'PROF_MORTAR'),
}
# Zarowka nazw T2+: '<Nazwa> <ROMAN>' (np. 'Sztabka III') + lore z kraina — jednoznaczne w slocie i w recepturze

# ---------------------------------------------------------------- RECEPTURY PER TIER
# profesja -> [(offset poziomu 0/2/4/6/8, typ, klucz, skladniki, ilosc wyniku)]
#  typ: 'P' polprodukt, 'N' narzedzie, 'B' buff (konsumpcyjny), 'K' klejnot/runa, 'O' obozowisko, 'H' hak swiata
RECEPTURY = {
 'smithing': [
   (0, 'P', 'SZTABKA', None, 1),
   (2, 'N', 'kilof', [('P:SZTABKA', 3), ('P:TRZONEK', 1)], 1),
   (2, 'N', 'noz', [('P:SZTABKA', 1), ('P:SKORA_W', 1)], 1),
   (4, 'N', 'sierp', [('P:SZTABKA', 2), ('P:TRZONEK', 1)], 1),
   (4, 'N', 'motyka', [('P:SZTABKA', 2), ('P:TRZONEK', 1)], 1),
   (6, 'P', 'OKUCIE', None, 2),
   (6, 'P', 'PLYTA', None, 1),
   (8, 'B', 'ZESTAW_NAPRAWCZY', [('P:SZTABKA', 2), ('P:TKANINA', 1)], 2),
 ],
 'woodworking': [
   (0, 'P', 'DESKA', None, 1),
   (2, 'P', 'TRZONEK', None, 2),
   (4, 'N', 'siekiera', [('P:OKUCIE', 1), ('P:TRZONEK', 1), ('P:DESKA', 1)], 1),
   (6, 'N', 'wedka', [('P:TRZONEK', 1), ('P:CIECIWA', 1), ('P:OKUCIE', 1)], 1),
 ],
 'tailoring': [
   (0, 'P', 'TKANINA', None, 1),
   (2, 'P', 'SKORA_W', None, 1),
   (4, 'P', 'CIECIWA', None, 1),
   (6, 'B', 'OPATRUNEK', [('P:TKANINA', 1), ('P:EKSTRAKT', 1)], 3),
   (8, 'P', 'PLACHTA', None, 1),
 ],
 'jewelcrafting': [
   (0, 'P', 'KAMIEN_O', None, 1),
   (2, 'K', 'KLEJNOT_ZDROWIA', [('P:KAMIEN_O', 2), ('P:SZTABKA', 1)], 1),
   (4, 'K', 'KLEJNOT_OCHRONY', [('P:KAMIEN_O', 2), ('P:SZTABKA', 1)], 1),
   (6, 'K', 'KLEJNOT_ODNOWY', [('P:KAMIEN_O', 2), ('S:herbalism:p', 2)], 1),
   (6, 'K', 'KLEJNOT_ZWINNOSCI', [('P:KAMIEN_O', 2), ('S:hunting:p', 2)], 1),
   (8, 'K', 'KLEJNOT_CELNOSCI', [('P:KAMIEN_O', 2), ('S:mining:r', 1), ('A:pyl', 1)], 1),
 ],
 'alchemy': [
   (0, 'P', 'EKSTRAKT', None, 1),
   (2, 'B', 'MIKSTURA_LECZNICZA', [('P:EKSTRAKT', 1), ('S:fishing:p', 1)], 2),
   (4, 'B', 'MIKSTURA_MANY', [('P:EKSTRAKT', 1), ('S:herbalism:p', 1)], 2),
   (4, 'B', 'ODTRUTKA', [('P:EKSTRAKT', 1), ('S:hunting:p', 1)], 2),
   (6, 'B', 'ELIKSIR_SILY', [('P:EKSTRAKT', 2), ('S:hunting:g', 2)], 1),
   (6, 'B', 'ELIKSIR_UMYSLU', [('P:EKSTRAKT', 2), ('S:mining:p', 1)], 1),
   (8, 'B', 'ELIKSIR_SKORY', [('P:EKSTRAKT', 2), ('S:hunting:r', 1), ('A:esencja', 1)], 1),
   (8, 'B', 'ELIKSIR_ZWINNOSCI', [('P:EKSTRAKT', 2), ('S:fishing:r', 1), ('A:esencja', 1)], 1),
   (8, 'B', 'OLEJ', [('P:EKSTRAKT', 1), ('S:woodworking:p', 2)], 2),
 ],
 'enchanting': [
   (0, 'P', 'PYL_Z', None, 1),
   (2, 'K', 'RUNA_SILY', [('P:PYL_Z', 2), ('P:KAMIEN_O', 1)], 1),
   (4, 'K', 'RUNA_MOCY', [('P:PYL_Z', 2), ('P:KAMIEN_O', 1)], 1),
   (6, 'K', 'RUNA_ZARU', [('P:PYL_Z', 2), ('P:KAMIEN_O', 1), ('S:herbalism:p', 1)], 1),
   (6, 'K', 'RUNA_GROZY', [('P:PYL_Z', 2), ('P:KAMIEN_O', 1), ('S:hunting:p', 1)], 1),
   (8, 'K', 'RUNA_POSPIECHU', [('P:PYL_Z', 3), ('P:KAMIEN_O', 1), ('A:pyl', 1)], 1),
   (8, 'B', 'ZWOJ_WYJECIA', [('P:PYL_Z', 2), ('P:TKANINA', 1)], 1),
 ],
 'cooking': [
   (0, 'B', 'POSILEK_PROWIANT', [('S:farming:g', 2), ('S:herbalism:g', 1)], 2),
   (2, 'B', 'POSILEK_ROSOL', [('S:fishing:g', 2), ('S:herbalism:p', 1)], 2),
   (4, 'B', 'POSILEK_SYTY', [('S:hunting:g', 2), ('S:farming:g', 1)], 2),
   (6, 'B', 'UCZTA', [('S:hunting:g', 2), ('S:fishing:g', 2), ('S:farming:g', 2), ('S:herbalism:p', 1)], 1),
   (8, 'B', 'DANIE_MISTRZA', [('S:hunting:g', 2), ('S:fishing:r', 1), ('A:serce', 1)], 1),
 ],
 'building': [
   (0, 'P', 'KAMIEN_C', None, 1),
   (2, 'P', 'BELKA', None, 1),
   (4, 'P', 'ZAPRAWA', None, 2),
   (6, 'O', 'OBOZOWISKO', [('P:BELKA', 2), ('P:PLACHTA', 1), ('P:ZAPRAWA', 1)], 1),
   (8, 'H', 'PRZEPRAWA_MOST', [('P:BELKA', 3), ('P:ZAPRAWA', 2), ('A:esencja', 1)], 1),
   (8, 'H', 'PRZEPRAWA_DRABINA', [('P:BELKA', 2), ('P:CIECIWA', 2)], 1),
   (8, 'H', 'PRZEPRAWA_PODPORA', [('P:BELKA', 2), ('P:PLYTA', 1)], 1),
 ],
}
INNE_PRODUKTY = {  # konsumpcyjne bez buffa: id -> (nazwa, opis)
 'ZESTAW_NAPRAWCZY': ('Zestaw naprawczy', 'Przeciągnij na narzędzie: przywraca 40% wytrzymałości.'),
 'ZWOJ_WYJECIA': ('Zwój wyjęcia klejnotu', 'Przeciągnij na przedmiot: wyjmuje losowy klejnot lub runę z gniazda (klejnot przepada).'),
 'DANIE_MISTRZA': ('Danie mistrza', 'Posiłek: zdrowie i regeneracja naraz.'),
 'OBOZOWISKO': ('Obozowisko', 'PPM na ziemi: rozbija obóz na 10 min — drużyna odpoczywa szybciej poza walką.'),
 'PRZEPRAWA_MOST': ('Zestaw przeprawy: most', 'Klucz skrótu: buduje most w miejscu oznaczonym w lochu lub na wyspie.'),
 'PRZEPRAWA_DRABINA': ('Zestaw przeprawy: drabina', 'Klucz skrótu: drabina na oznaczonej ścianie.'),
 'PRZEPRAWA_PODPORA': ('Zestaw przeprawy: podpora', 'Klucz skrótu: podpiera oznaczone osuwisko.'),
}
BUFFY['DANIE_MISTRZA'] = ('Danie mistrza', 'posilek', 1200, 0, {'max-health': (1.8, 0.5), 'health-regeneration': (0.045, 0.016)},
                          'Zdrowie i regeneracja naraz.')

# ---------------------------------------------------------------- RECEPTURY TYLKO T1 (zrodla dla Chowancow i v2)
# (stacja/profesja, poziom, id wyniku, nazwa (None = istniejaca), skladniki [(id, ilosc)], ilosc)
T1_EXTRA = [
 ('alchemy', 1, 'PROF_DRIED_HERBS', None, [('PROF_MEDICINAL_HERB', 2)], 1),
 ('alchemy', 3, 'PROF_ALCHEMICAL_BASE', None, [('PROF_HERBAL_EXTRACT', 1), ('PROF_SPRUCE_RESIN', 1)], 1),
 ('alchemy', 5, 'PROF_ANTIDOTE', None, [('PROF_HERBAL_EXTRACT', 1), ('PROF_REDCAP', 1)], 1),
 ('enchanting', 3, 'PROF_RUNE_BLANK', None, [('PROF_CUT_STONE', 1), ('PROF_ENCHANT_DUST', 1)], 1),
 ('enchanting', 5, 'PROF_CHARGED_RUNE', None, [('PROF_RUNE_BLANK', 1), ('PROF_ENCHANT_DUST', 2)], 1),
 ('enchanting', 5, 'FIRE_ESSENCE', 'Esencja ognia', [('PROF_ENCHANT_DUST', 2), ('PROF_SPRUCE_RESIN', 1)], 1),
 ('enchanting', 7, 'WITHER_ESSENCE', 'Esencja obumarcia', [('PROF_ENCHANT_DUST', 2), ('PROF_BEAST_BONE', 2)], 1),
 ('enchanting', 7, 'PROF_ARCANE_CRYSTAL', None, [('PROF_RAW_GEM', 1), ('PROF_ENCHANT_DUST', 2)], 1),
 ('enchanting', 9, 'PROF_AWAKENING_SEAL', None, [('PROF_CHARGED_RUNE', 1), ('PROF_ARCANE_CRYSTAL', 1), ('PROF_A1_ESENCJA', 1)], 1),
 ('smithing', 7, 'PROF_STEEL_MECHANISM', None, [('PROF_REFINED_IRON', 3), ('PROF_CARVED_FITTING', 1)], 1),
 ('tailoring', 5, 'PROF_LEATHER_PANEL', None, [('PROF_CURED_HIDE', 2), ('PROF_FLAX_FIBER', 1)], 1),
 ('tailoring', 7, 'PROF_FINE_WOOL', None, [('PROF_RAW_FLEECE', 3)], 1),
]
# T1: id istniejacych przedmiotow dla polproduktow/klejnotow, ktorych v1 nie mial pod tym kluczem
T1_ID_OKUCIE = 'PROF_CARVED_FITTING'
T1_ID_PLACHTA = 'PROF_ROBE_LINING'   # „Podszycie szaty" -> T1 plachta obozowa (ta sama ikona, nowa nazwa)

# ---------------------------------------------------------------- SPRZET Z RZEMIOSLA (bazy v2)
# rodzaj broni v2 -> profesja; zbroja wg wagi; ozdoby -> jubilerstwo
BRON_PROFESJA = {
 'miecz': 'smithing', 'drzewcowa': 'smithing', 'klinga': 'smithing', 'topor': 'smithing', 'miecz_dwureczny': 'smithing',
 'sztylet': 'smithing', 'bulawa': 'smithing', 'mlot_dwureczny': 'smithing', 'kosa': 'smithing', 'sierp': 'smithing',
 'lancuch': 'smithing', 'wlocznia': 'smithing',
 'luk': 'woodworking', 'kusza': 'woodworking', 'luk_dlugi': 'woodworking', 'kostur': 'woodworking',
 'rozdzka': 'woodworking', 'kostur_dwureczny': 'woodworking', 'tarcza': 'woodworking',
 'grimuar': 'enchanting', 'totem': 'enchanting', 'filakterium': 'enchanting',
}
ZBROJA_PROFESJA = {'ciezka': 'smithing', 'srednia': 'smithing', 'lekka': 'tailoring', 'tkanina': 'tailoring'}
# ktore bazy (numer bazy 1..5 w rodzinie v2) maja wariant z rzemiosla i na jakim offsecie tieru
BAZY_RZEMIOSLA = {2: 2, 4: 6}      # baza 2 (poziom 3) -> offset 2; baza 4 (poziom 7) -> offset 6 (+ serce aktu)
# skladniki sprzetu: material glowny wg profesji i slotu
SKLAD_SPRZETU = {
 ('smithing', 'bron1'): [('P:SZTABKA', 3), ('P:TRZONEK', 1)],
 ('smithing', 'bron2'): [('P:SZTABKA', 5), ('P:TRZONEK', 2)],
 ('woodworking', 'bron1'): [('P:DESKA', 3), ('P:CIECIWA', 1)],
 ('woodworking', 'bron2'): [('P:DESKA', 5), ('P:CIECIWA', 1), ('P:KAMIEN_O', 1)],
 ('woodworking', 'tarcza'): [('P:DESKA', 3), ('P:OKUCIE', 2)],
 ('enchanting', 'bron1'): [('P:PYL_Z', 2), ('P:TKANINA', 2)],
 ('smithing', 'zbroja'): [('P:SZTABKA', 1), ('P:SKORA_W', 1)],     # x waga slotu (HELM 2, KLATA 4, NOGI 3, BUTY 2)
 ('tailoring', 'zbroja'): [('P:TKANINA', 1), ('P:SKORA_W', 1)],
 ('jewelcrafting', 'ozdoba'): [('P:KAMIEN_O', 1), ('P:SZTABKA', 1)],
}
MNOZNIK_SLOTU = {'HELM': 2, 'KLATA': 4, 'NOGI': 3, 'BUTY': 2}
SERCE_DO_LEPSZEJ_BAZY = 1          # baza 4 wymaga 1 serca aktu (boss)

# ---------------------------------------------------------------- PERKI (plugin)
PERKI = {'zbieractwo': {'co': 10, 'podwojny': 0.04, 'czas': 0.01},
         'rzemioslo': {'co': 10, 'zwrot': 0.02}}

# ---------------------------------------------------------------- ZLECENIA
ZLECENIA = {'dziennie': 3, 'ekwiwalent_cel': 30, 'nagroda_xp': XP_ZLECENIA, 'nagroda_pieniadze': 1.3}
ZLECENIA_STARTOWE_ILE = 5

# ---------------------------------------------------------------- RELIKTY v1 (zostaja zdefiniowane, bez receptur)
RELIKTY = ['PROF_RAW_TIMBER', 'PROF_BIRCH_LATH', 'PROF_RARE_FISH', 'PROF_SILVER_ORE', 'PROF_ALCHEMICAL_CROP', 'PROF_ALCHEMICAL_SOLVENT', 'PROF_ARROWHEADS',
           'PROF_AXE_HEAD', 'PROF_BAIT_MIX', 'PROF_BOW_LIMB', 'PROF_CARVED_STONE', 'PROF_COMBAT_TONIC',
           'PROF_DECORATIVE_FITTING', 'PROF_EMPTY_SOCKET', 'PROF_FABRIC_REPAIR_KIT', 'PROF_FISH_OIL',
           'PROF_MASTER_BLADE_BLANK', 'PROF_MASTER_CHARM', 'PROF_METAL_REPAIR_KIT', 'PROF_PEARL_SETTING',
           'PROF_REINFORCED_BLOCK', 'PROF_SILVER_SETTING', 'PROF_SINEW', 'PROF_STAFF_CORE', 'PROF_STAFF_FOCUS',
           'PROF_WEAPON_OIL', 'PROF_MASTER_BANQUET', 'PROF_LUMBER_AXE', 'PROF_MASTER_LUMBER_AXE', 'PROF_FARMER_HOE',
           'PROF_MASTER_FARMER_HOE', 'PROF_FISHING_ROD', 'PROF_MASTER_FISHING_ROD', 'PROF_HERB_SICKLE',
           'PROF_MASTER_HERB_SICKLE', 'PROF_HUNTER_KNIFE', 'PROF_MASTER_HUNTER_KNIFE', 'PROF_SILVER_SIGNET',
           'PROF_MOONLEAF_AMULET', 'PROF_LUCK_CHARM', 'PROF_TRAVELER_BELT', 'PROF_LEATHER_GLOVES',
           'PROF_UNAWAKENED_STAFF', 'PROF_AWAKENED_STAFF']
