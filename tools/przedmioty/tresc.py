# -*- coding: utf-8 -*-
"""Tabele tresci systemu przedmiotow (Akt 1, poziomy 1-10). Zrodlo prawdy — zmiany TU, nie w yml.

Projekt: docs/research/itemizacja/ (01 gry, 02 MMOItems, 03 stan) + strona przegladu.
Rodzaj gramatyczny: m (meski), f (zenski), n (nijaki), p (liczba mnoga niemeskoosobowa = forma n przymiotnika).
"""

ARCH = ['woj', 'nin', 'mag', 'ako']
ARCH_PL = {'woj': 'Wojownik', 'nin': 'Ninja', 'mag': 'Mag', 'ako': 'Akolita'}
# glowny atrybut archetypu (D3); wiara wchodzi, gdy atrybut istnieje w MMOCore/attributes
ATRYBUT = {'woj': 'strength', 'nin': 'dexterity', 'mag': 'intelligence', 'ako': 'wiara'}
ATRYBUT_PL = {'strength': 'Siła', 'dexterity': 'Zręczność', 'intelligence': 'Intelekt', 'wiara': 'Wiara'}

# 5 baz na slot: minimalny poziom przedmiotu (i wymagany poziom) oraz wymaganie glownego atrybutu
POZIOMY = [1, 3, 5, 7, 10]
WYM_ATR = [0, 0, 2, 3, 4]
MNOZNIK_BAZY = [1.00, 1.04, 1.08, 1.12, 1.18]   # lepsza baza = troche wiecej statystyk przy tym samym ilvl

# ---------------------------------------------------------------- ZBROJA
SLOTY_ZB = [('HELM', 'HELMET', 0.20), ('KLATA', 'CHESTPLATE', 0.35), ('NOGI', 'LEGGINGS', 0.28), ('BUTY', 'BOOTS', 0.17)]
# pancerz calego kompletu: przy ilvl 1 i przyrost na poziom przedmiotu
WAGA_PANCERZ = {'tkanina': (5.0, 0.35), 'lekka': (7.0, 0.45), 'srednia': (11.0, 0.55), 'ciezka': (15.0, 0.65)}
WAGA_PL = {'tkanina': 'tkanina', 'lekka': 'lekka', 'srednia': 'średnia', 'ciezka': 'ciężka'}
# materialy vanilli wg wagi (wyglad do czasu tekstur): LEATHER farbowane
MAT_WAGA = {'tkanina': 'LEATHER', 'lekka': 'LEATHER', 'srednia': 'CHAINMAIL', 'ciezka': 'IRON'}

# (waga, kolor farby R,G,B dla skory lub None, trim (material, wzor) lub None, implicit, nazwy[hełm, klata, nogi, buty] jako (nazwa, rodzaj))
ZBROJE = {
 'woj': [
  ('srednia', None, None, {'max-health': (0.5, 0.05)},
   [('Czepiec kolczy', 'm'), ('Kolczuga', 'f'), ('Kolcze nogawice', 'p'), ('Kolcze trzewiki', 'p')]),
  ('lekka', (110, 80, 50), None, {'dodge-rating': (2.0, 0.15)},
   [('Hełm skórzany', 'm'), ('Kaftan skórzany', 'm'), ('Skórzane spodnie', 'p'), ('Skórzane buty', 'p')]),
  ('srednia', None, ('copper', 'sentry'), {'damage-reduction': (1.0, 0.05)},
   [('Szyszak', 'm'), ('Brygantyna', 'f'), ('Nagolenniki', 'p'), ('Sabatony', 'p')]),
  ('srednia', None, ('iron', 'ward'), {'max-health': (0.8, 0.08), 'knockback-resistance': (0.02, 0.0)},
   [('Hełm łuskowy', 'm'), ('Zbroja łuskowa', 'f'), ('Łuskowe nagolenniki', 'p'), ('Okute buty', 'p')]),
  ('ciezka', None, ('gold', 'rib'), {'damage-reduction': (1.5, 0.08), 'armor-toughness': (0.5, 0.05)},
   [('Przyłbica', 'f'), ('Napierśnik płytowy', 'm'), ('Płytowe nagolenniki', 'p'), ('Płytowe sabatony', 'p')]),
 ],
 'nin': [
  ('lekka', (40, 40, 48), None, {'dodge-rating': (2.0, 0.15)},
   [('Chusta', 'f'), ('Kurta', 'f'), ('Luźne spodnie', 'p'), ('Miękkie buty', 'p')]),
  ('lekka', (85, 95, 55), None, {'movement-speed': (0.002, 0.0002)},
   [('Kaptur', 'm'), ('Kaftan', 'm'), ('Nogawice', 'p'), ('Buty z cholewą', 'p')]),
  ('lekka', (60, 20, 25), ('redstone', 'vex'), {'critical-strike-chance': (1.0, 0.1)},
   [('Maska', 'f'), ('Kamizela', 'f'), ('Obcisłe spodnie', 'p'), ('Ciżmy', 'p')]),
  ('lekka', (25, 30, 60), ('amethyst', 'silence'), {'dodge-rating': (3.0, 0.2)},
   [('Kaptur łotrzyka', 'm'), ('Kurta łotrzyka', 'f'), ('Spodnie łotrzyka', 'p'), ('Buty łotrzyka', 'p')]),
  ('lekka', (15, 15, 18), ('netherite', 'eye'), {'critical-strike-power': (4.0, 0.3)},
   [('Maska mroku', 'f'), ('Pancerz mroku', 'm'), ('Nogawice mroku', 'p'), ('Ciżmy mroku', 'p')]),
 ],
 'mag': [
  ('tkanina', (120, 110, 160), None, {'max-mana': (3.0, 0.3)},
   [('Kaptur', 'm'), ('Szata', 'f'), ('Spodnie', 'p'), ('Sandały', 'p')]),
  ('tkanina', (70, 60, 140), None, {'mana-regeneration': (0.05, 0.01)},
   [('Czapka', 'f'), ('Toga', 'f'), ('Szarawary', 'p'), ('Trzewiki', 'p')]),
  ('tkanina', (40, 90, 150), ('lapis', 'wayfinder'), {'magic-damage': (2.0, 0.2)},
   [('Kaptur runiczny', 'm'), ('Szata runiczna', 'f'), ('Spodnie runiczne', 'p'), ('Trzewiki runiczne', 'p')]),
  ('tkanina', (130, 40, 140), ('amethyst', 'spire'), {'cooldown-reduction': (1.0, 0.1)},
   [('Diadem', 'm'), ('Szata haftowana', 'f'), ('Nogawice haftowane', 'p'), ('Buty haftowane', 'p')]),
  ('tkanina', (230, 225, 240), ('gold', 'flow'), {'magic-damage': (3.0, 0.25), 'max-mana': (2.0, 0.2)},
   [('Korona', 'f'), ('Szata gwiezdna', 'f'), ('Spodnie gwiezdne', 'p'), ('Trzewiki gwiezdne', 'p')]),
 ],
 'ako': [
  ('lekka', (90, 85, 75), None, {'health-regeneration': (0.05, 0.005)},
   [('Kaptur', 'm'), ('Habit', 'm'), ('Spodnie', 'p'), ('Sandały', 'p')]),
  ('lekka', (120, 100, 70), None, {'max-health': (0.5, 0.05)},
   [('Kaptur podróżny', 'm'), ('Płaszcz podróżny', 'm'), ('Spodnie podróżne', 'p'), ('Buty podróżne', 'p')]),
  ('lekka', (35, 35, 35), ('quartz', 'dune'), {'skill-damage': (2.0, 0.2)},
   [('Welon', 'm'), ('Habit żałobny', 'm'), ('Nogawice żałobne', 'p'), ('Trzewiki żałobne', 'p')]),
  ('lekka', (150, 20, 20), ('redstone', 'host'), {'lifesteal': (0.5, 0.05)},
   [('Kaptur święcony', 'm'), ('Ornat', 'm'), ('Spodnie święcone', 'p'), ('Buty święcone', 'p')]),
  ('srednia', None, ('emerald', 'wild'), {'skill-damage': (3.0, 0.25), 'max-health': (0.8, 0.08)},
   [('Hełm kapłański', 'm'), ('Kolczy ornat', 'm'), ('Kolcze nagolenniki', 'p'), ('Okute sandały', 'p')]),
 ],
}

# ---------------------------------------------------------------- BRON
# grupa bieglosci -> (typ MMOItems, dmg przy ilvl 1, przyrost/ilvl, szybkosc, dwureczna, statystyki dodatkowe, materialy x5, opis grupy)
RODZAJE = {
 'miecz':            ('SWORD', 5.5, 0.45, 1.6, False, {}, ['STONE_SWORD', 'IRON_SWORD', 'IRON_SWORD', 'DIAMOND_SWORD', 'NETHERITE_SWORD'], 'miecze'),
 'drzewcowa':        ('HALBERD', 11.9, 0.95, 1.0, True, {}, ['IRON_AXE', 'IRON_AXE', 'DIAMOND_AXE', 'DIAMOND_AXE', 'NETHERITE_AXE'], 'broń drzewcowa'),
 'klinga':           ('KATANA', 5.5, 0.45, 1.8, False, {'critical-strike-chance': (3, 0.1)}, ['IRON_SWORD', 'IRON_SWORD', 'GOLDEN_SWORD', 'DIAMOND_SWORD', 'NETHERITE_SWORD'], 'klingi elfów'),
 'topor':            ('AXE', 7.0, 0.55, 0.9, False, {'critical-strike-power': (8, 0.5)}, ['STONE_AXE', 'IRON_AXE', 'IRON_AXE', 'DIAMOND_AXE', 'NETHERITE_AXE'], 'topory'),
 'miecz_dwureczny':  ('GREATSWORD', 13.2, 1.05, 0.9, True, {}, ['IRON_SWORD', 'IRON_SWORD', 'DIAMOND_SWORD', 'DIAMOND_SWORD', 'NETHERITE_SWORD'], 'miecze dwuręczne'),
 'sztylet':          ('DAGGER', 3.5, 0.30, 2.2, False, {'critical-strike-chance': (5, 0.2)}, ['IRON_SWORD', 'IRON_SWORD', 'GOLDEN_SWORD', 'DIAMOND_SWORD', 'NETHERITE_SWORD'], 'sztylety'),
 'luk':              ('BOW', 6.0, 0.50, None, False, {}, ['BOW'] * 5, 'łuki'),
 'kusza':            ('CROSSBOW', 7.0, 0.55, None, False, {'critical-strike-power': (6, 0.4)}, ['CROSSBOW'] * 5, 'kusze'),
 'luk_dlugi':        ('GREATBOW', 8.0, 0.65, None, True, {'arrow-velocity': (1.3, 0.0)}, ['BOW'] * 5, 'łuki długie'),
 'wlocznia':         ('SPEAR', 6.0, 0.50, 1.1, False, {}, ['IRON_SPEAR', 'IRON_SPEAR', 'DIAMOND_SPEAR', 'DIAMOND_SPEAR', 'NETHERITE_SPEAR'], 'włócznie'),
 'lancuch':          ('WHIP', 4.5, 0.38, 1.7, False, {}, ['IRON_CHAIN'] * 5, 'łańcuchy'),
 'rozdzka':          ('WAND', 3.0, 0.25, 1.15, False, {'magic-damage': (5, 0.4)}, ['STICK', 'BLAZE_ROD', 'BLAZE_ROD', 'BREEZE_ROD', 'END_ROD'], 'różdżki'),
 'kostur':           ('STAFF', 4.5, 0.35, 1.0, False, {'magic-damage': (7, 0.5)}, ['STICK', 'BAMBOO', 'BLAZE_ROD', 'BREEZE_ROD', 'END_ROD'], 'kostury'),
 'grimuar':          ('TOME', None, 0, None, False, {'magic-damage': (5, 0.4), 'max-mana': (5, 0.5)}, ['BOOK', 'BOOK', 'WRITABLE_BOOK', 'ENCHANTED_BOOK', 'ENCHANTED_BOOK'], 'grimuary'),
 'kostur_dwureczny': ('GREATSTAFF', 6.0, 0.45, 0.9, True, {'magic-damage': (10, 0.7)}, ['STICK', 'BAMBOO', 'BLAZE_ROD', 'BREEZE_ROD', 'END_ROD'], 'kostury dwuręczne'),
 'totem':            ('TALISMAN', None, 0, None, False, {'skill-damage': (8, 0.5), 'max-mana': (5, 0.5)}, ['BONE', 'BONE', 'SKELETON_SKULL', 'GOAT_HORN', 'WITHER_SKELETON_SKULL'], 'totemy'),
 'filakterium':      ('FILAKTERIUM', None, 0, None, False, {'magic-damage': (4, 0.4), 'max-mana': (5, 0.5), 'spell-vampirism': (1, 0.1)}, ['BONE_MEAL', 'DECORATED_POT', 'SKELETON_SKULL', 'SCULK_CATALYST', 'WITHER_SKELETON_SKULL'], 'filakteria'),
 # B13 (paczka B 25.10): berło = druga wspólna broń akolity (grupa biegłości 'bulawa', jak różdżka w grupie
 # 'kostur' maga). Akolita miał 2 rodzaje broni (buława + rasowa), inne archetypy 3: lekki akolita (Wyrocznia,
 # Pasterz Ciszy, Kapłan Krwi) do 10 poz. widział 10 broni wobec 15. Słabszy cios, premia do obrażeń umiejętności.
 'berlo':            ('HAMMER', 4.0, 0.30, 1.2, False, {'skill-damage': (3, 0.15)}, ['WOODEN_SHOVEL', 'GOLDEN_SHOVEL', 'GOLDEN_SHOVEL', 'DIAMOND_SHOVEL', 'NETHERITE_SHOVEL'], 'berła'),
 'bulawa':           ('HAMMER', 5.5, 0.45, 1.2, False, {}, ['STONE_SHOVEL', 'IRON_SHOVEL', 'IRON_SHOVEL', 'DIAMOND_SHOVEL', 'MACE'], 'buławy'),
 'tarcza':           ('SHIELD', None, 0, None, False, {'armor': (2, 0.2), 'block-power': (30, 1.0), 'block-rating': (10, 1.0)}, ['SHIELD'] * 5, 'tarcze'),
 'sierp':            ('SIERP', 5.0, 0.40, 1.6, False, {'critical-strike-chance': (3, 0.1)}, ['IRON_HOE', 'IRON_HOE', 'GOLDEN_HOE', 'DIAMOND_HOE', 'NETHERITE_HOE'], 'sierpy'),
 'mlot_dwureczny':   ('GREATHAMMER', 14.9, 1.20, 0.8, True, {}, ['STONE_AXE', 'IRON_AXE', 'IRON_AXE', 'MACE', 'MACE'], 'młoty dwuręczne'),
 'kosa':             ('KOSA', 11.9, 0.95, 1.0, True, {'lifesteal': (1, 0.1)}, ['IRON_HOE', 'IRON_HOE', 'DIAMOND_HOE', 'DIAMOND_HOE', 'NETHERITE_HOE'], 'kosy'),
}
# grupa bieglosci dla rodzaju (rozdzka i kostur to jedna grupa "kostur")
GRUPA = {'rozdzka': 'kostur', 'berlo': 'bulawa'}

# archetyp: wspolne rodzaje + 4 rodzaje rasowe (czlowiek, elf, ork, nieumarly) — decyzja wlasciciela 29.09
BRON_ARCH = {
 'woj': (['miecz'], ['drzewcowa', 'klinga', 'topor', 'miecz_dwureczny']),
 'nin': (['sztylet', 'luk'], ['kusza', 'luk_dlugi', 'wlocznia', 'lancuch']),
 'mag': (['rozdzka', 'kostur'], ['grimuar', 'kostur_dwureczny', 'totem', 'filakterium']),
 'ako': (['bulawa', 'berlo'], ['tarcza', 'sierp', 'mlot_dwureczny', 'kosa']),
}
RASA_RODZAJU = {'drzewcowa': 'Człowiek', 'klinga': 'Elf', 'topor': 'Ork', 'miecz_dwureczny': 'Nieumarły',
                'kusza': 'Człowiek', 'luk_dlugi': 'Elf', 'wlocznia': 'Ork', 'lancuch': 'Nieumarły',
                'grimuar': 'Człowiek', 'kostur_dwureczny': 'Elf', 'totem': 'Ork', 'filakterium': 'Nieumarły',
                'tarcza': 'Człowiek', 'sierp': 'Elf', 'mlot_dwureczny': 'Ork', 'kosa': 'Nieumarły'}

NAZWY_BRONI = {
 'miecz': [('Krótki miecz', 'm'), ('Miecz żołnierski', 'm'), ('Pałasz', 'm'), ('Szabla', 'f'), ('Miecz rycerski', 'm')],
 'drzewcowa': [('Gizarma', 'f'), ('Halabarda strażnicza', 'f'), ('Berdysz', 'm'), ('Glewia', 'f'), ('Kopia korony', 'f')],
 'klinga': [('Ostrze wierzby', 'n'), ('Klinga zmierzchu', 'f'), ('Ostrze jesionu', 'n'), ('Klinga pieśni', 'f'), ('Ostrze starszyzny', 'n')],
 'topor': [('Toporek', 'm'), ('Topór rzeźnika', 'm'), ('Czekan', 'm'), ('Topór wodza', 'm'), ('Wielki topór krwi', 'm')],
 'miecz_dwureczny': [('Kościany dwuręcznik', 'm'), ('Miecz katowski', 'm'), ('Espadon krypty', 'm'), ('Miecz żałobny', 'm'), ('Ostrze kurhanu', 'n')],
 'sztylet': [('Kordzik', 'm'), ('Sztylet', 'm'), ('Mizerykordia', 'f'), ('Sztylet jadowity', 'm'), ('Kieł nocy', 'm')],
 'luk': [('Łuk krótki', 'm'), ('Łuk refleksyjny', 'm'), ('Łuk tropiciela', 'm'), ('Łuk kompozytowy', 'm'), ('Łuk cienia', 'm')],
 'kusza': [('Kusza ręczna', 'f'), ('Kusza wojskowa', 'f'), ('Arbalet', 'm'), ('Kusza z zapadką', 'f'), ('Kusza oblężnicza', 'f')],
 'luk_dlugi': [('Łuk długi', 'm'), ('Łuk z cisu', 'm'), ('Łuk wierzbowy', 'm'), ('Łuk z rogu', 'm'), ('Łuk księżyca', 'm')],
 'wlocznia': [('Oszczep myśliwski', 'm'), ('Włócznia stepowa', 'f'), ('Dzida', 'f'), ('Włócznia z kości', 'f'), ('Grot stepowy', 'm')],
 'lancuch': [('Łańcuch więzienny', 'm'), ('Kiścień', 'm'), ('Łańcuch z hakiem', 'm'), ('Łańcuch żałobny', 'm'), ('Okowy ciszy', 'p')],
 'berlo': [('Berło nowicjusza', 'n'), ('Kropidło', 'n'), ('Berło pielgrzyma', 'n'), ('Buzdygan kapłański', 'm'), ('Berło relikwiarza', 'n')],
 'rozdzka': [('Różdżka z leszczyny', 'f'), ('Różdżka runiczna', 'f'), ('Różdżka iskier', 'f'), ('Różdżka szronu', 'f'), ('Berło adepta', 'n')],
 'kostur': [('Kostur wędrowca', 'm'), ('Kostur runiczny', 'm'), ('Kostur żaru', 'm'), ('Kostur burzy', 'm'), ('Laska arcymaga', 'f')],
 'grimuar': [('Księga zaklęć', 'f'), ('Grimuar żywiołów', 'm'), ('Kodeks wieży', 'm'), ('Foliał czasu', 'm'), ('Księga świtu', 'f')],
 'kostur_dwureczny': [('Kostur z jesionu', 'm'), ('Kostur gaju', 'm'), ('Laska zwierciadeł', 'f'), ('Kostur pieśni burz', 'm'), ('Kostur starszyzny', 'm')],
 'totem': [('Totem kości', 'm'), ('Totem wilka', 'm'), ('Bęben wojenny', 'm'), ('Totem burzy', 'm'), ('Totem przodków', 'm')],
 'filakterium': [('Urna popiołów', 'f'), ('Filakterium zarazy', 'n'), ('Czaszka lisza', 'f'), ('Serce krypty', 'n'), ('Filakterium ciszy', 'n')],
 'bulawa': [('Kropacz', 'm'), ('Buława', 'f'), ('Pernacz', 'm'), ('Buława kolczasta', 'f'), ('Gwiazda poranna', 'f')],
 'tarcza': [('Puklerz', 'm'), ('Tarcza okuta', 'f'), ('Pawęż', 'f'), ('Tarcza z relikwią', 'f'), ('Tarcza świętych', 'f')],
 'sierp': [('Sierp żniwny', 'm'), ('Sierp srebrny', 'm'), ('Sierp księżyca', 'm'), ('Sierp rytualny', 'm'), ('Półksiężyc', 'm')],
 'mlot_dwureczny': [('Młot kapłański', 'm'), ('Młot krwi', 'm'), ('Młot przodków', 'm'), ('Młot wojenny', 'm'), ('Młot kurhanu', 'm')],
 'kosa': [('Kosa żniwiarza', 'f'), ('Kosa grobowa', 'f'), ('Kosa ciszy', 'f'), ('Kosa cmentarna', 'f'), ('Kosa otchłani', 'f')],
}

# ---------------------------------------------------------------- AKCESORIA (5 typow x 5 na archetyp)
AKC_TYPY = ['NECKLACE', 'RING', 'GLOVES', 'BELT', 'CHARM']
AKC_PL = {'NECKLACE': 'naszyjnik', 'RING': 'pierścień', 'GLOVES': 'dłonie', 'BELT': 'pas', 'CHARM': 'charm'}
AKC_RDZ = {  # 5 rzeczownikow na poziomy
 'NECKLACE': [('Rzemyk', 'm'), ('Naszyjnik', 'm'), ('Amulet', 'm'), ('Wisior', 'm'), ('Medalion', 'm')],
 'RING': [('Obrączka', 'f'), ('Pierścień', 'm'), ('Sygnet', 'm'), ('Pierścień z oczkiem', 'm'), ('Pierścień pieczętny', 'm')],
 'GLOVES': [('Rękawice', 'p'), ('Mitenki', 'p'), ('Rękawice okute', 'p'), ('Karwasze', 'p'), ('Rękawice wysadzane', 'p')],
 'BELT': [('Rzemień', 'm'), ('Pas', 'm'), ('Pas z sakwami', 'm'), ('Pas bojowy', 'm'), ('Pas z klamrą', 'm')],
 'CHARM': [('Talizman', 'm'), ('Amulet ochronny', 'm'), ('Znak', 'm'), ('Relikwia', 'f'), ('Pieczęć', 'f')],
}
# przymiotnik materialu nadklasy (m, f, n/p) — zamiast motywow w dopelniaczu (krytyka 30.09: kolizje z setami)
MATERIAL_ARCH = {'woj': ('stalowy', 'stalowa', 'stalowe'), 'nin': ('czarny', 'czarna', 'czarne'),
                 'mag': ('srebrny', 'srebrna', 'srebrne'), 'ako': ('kościany', 'kościana', 'kościane')}
AKC_MAT = {'NECKLACE': 'STRING', 'RING': 'GOLD_NUGGET', 'GLOVES': 'LEATHER',
           'BELT': 'LEAD', 'CHARM': 'PRISMARINE_CRYSTALS'}
# implicit: typ -> archetyp -> {stat: (przy ilvl 1, przyrost/ilvl)}; mana tylko u maga
_OF = {'woj': {'physical-damage': (3, 0.25)}, 'nin': {'critical-strike-chance': (2, 0.15)},
       'mag': {'magic-damage': (3, 0.25)}, 'ako': {'skill-damage': (3, 0.25)}}
AKC_IMPL = {
 'NECKLACE': {'woj': {'max-health': (1, 0.1)}, 'nin': {'max-health': (1, 0.1)}, 'mag': {'max-mana': (5, 0.4)}, 'ako': {'max-health': (1, 0.1)}},
 'RING': _OF,
 'GLOVES': {'woj': {'attack-damage': (0.5, 0.05)}, 'nin': {'critical-strike-power': (4, 0.3)}, 'mag': {'magic-damage': (2, 0.2)}, 'ako': {'skill-damage': (2, 0.2)}},
 'BELT': {'woj': {'max-health': (1, 0.1), 'knockback-resistance': (0.03, 0.0)}, 'nin': {'max-health': (0.5, 0.05), 'movement-speed': (0.002, 0.0002)},
          'mag': {'max-health': (0.5, 0.05), 'mana-regeneration': (0.05, 0.01)}, 'ako': {'max-health': (1, 0.1), 'health-regeneration': (0.03, 0.003)}},
 'CHARM': {a: {'additional-' + ATRYBUT[a]: (1, 0.1)} for a in ARCH},
}

# ---------------------------------------------------------------- AFFIKSY
# rodzina: (typ P/S, formy nazwy (m,f,n) lub sufiks, statystyka, (T1, T2, T3) wartosci przy ilvl 1, przyrost/ilvl dla T1)
# Prefiksy = ofensywa i zasob, sufiksy = obrona i uzytecznosc (konwencja PoE).
AFFIKSY = {
 'ostry':      ('P', ('Ostry', 'Ostra', 'Ostre'), 'attack-damage', (0.8, 1.4, 2.2), 0.10),
 'okrutny':    ('P', ('Okrutny', 'Okrutna', 'Okrutne'), 'physical-damage', (3, 5, 8), 0.20),
 'krwawy':     ('P', ('Krwawy', 'Krwawa', 'Krwawe'), 'lifesteal', (1, 2, 3), 0.05),
 'druzgocacy': ('P', ('Druzgocący', 'Druzgocąca', 'Druzgocące'), 'critical-strike-power', (5, 9, 14), 0.30),
 'zabojczy':   ('P', ('Zabójczy', 'Zabójcza', 'Zabójcze'), 'critical-strike-chance', (2, 3, 5), 0.10),
 'celny':      ('P', ('Celny', 'Celna', 'Celne'), 'projectile-damage', (3, 5, 8), 0.20),
 'chyzy':      ('P', ('Chyży', 'Chyża', 'Chyże'), 'attack-speed', (0.05, 0.1, 0.15), 0.0),
 'tajemny':    ('P', ('Tajemny', 'Tajemna', 'Tajemne'), 'magic-damage', (4, 6, 9), 0.25),
 'wieszczy':   ('P', ('Wieszczy', 'Wieszcza', 'Wieszcze'), 'max-mana', (4, 7, 10), 0.40),
 'plynny':     ('P', ('Płynny', 'Płynna', 'Płynne'), 'mana-regeneration', (0.1, 0.2, 0.3), 0.01),
 'zarliwy':    ('P', ('Żarliwy', 'Żarliwa', 'Żarliwe'), 'skill-damage', (3, 5, 8), 0.20),
 'krzepki':    ('P', ('Krzepki', 'Krzepka', 'Krzepkie'), 'max-health', (1, 2, 3), 0.08),
 'pancerny':   ('P', ('Pancerny', 'Pancerna', 'Pancerne'), 'armor', (1, 2, 3), 0.10),
 # sufiksy
 'zolwia':       ('S', 'Żółwia', 'armor-toughness', (0.5, 1, 1.5), 0.03),
 'twierdzy':     ('S', 'Twierdzy', 'damage-reduction', (1, 2, 3), 0.05),
 'skaly':        ('S', 'Skały', 'knockback-resistance', (0.05, 0.10, 0.15), 0.0),
 'zrodla':       ('S', 'Źródła', 'health-regeneration', (0.05, 0.10, 0.20), 0.005),
 'wiatru':       ('S', 'Wiatru', 'movement-speed', (0.002, 0.004, 0.006), 0.0),
 'cienia':       ('S', 'Cienia', 'dodge-rating', (2, 3, 5), 0.15),
 'medrca':       ('S', 'Mędrca', 'cooldown-reduction', (2, 4, 6), 0.10),
 'byka':         ('S', 'Byka', 'additional-strength', (1, 2, 3), 0.10),
 'lisa':         ('S', 'Lisa', 'additional-dexterity', (1, 2, 3), 0.10),
 'sowy':         ('S', 'Sowy', 'additional-intelligence', (1, 2, 3), 0.10),
 'pielgrzyma':   ('S', 'Pielgrzyma', 'additional-wiara', (1, 2, 3), 0.10),
}
# pule: archetyp -> kategoria (bron / zbroja / ozdoba) -> rodziny
PULE = {
 'woj': {'bron':   ['ostry', 'okrutny', 'krwawy', 'druzgocacy', 'byka', 'skaly', 'twierdzy'],
         'zbroja': ['krzepki', 'pancerny', 'okrutny', 'zolwia', 'twierdzy', 'skaly', 'zrodla', 'byka'],
         'ozdoba': ['okrutny', 'krwawy', 'druzgocacy', 'krzepki', 'twierdzy', 'zrodla', 'wiatru', 'byka']},
 'nin': {'bron':   ['ostry', 'zabojczy', 'druzgocacy', 'celny', 'chyzy', 'lisa', 'wiatru', 'cienia'],
         'zbroja': ['krzepki', 'pancerny', 'zabojczy', 'cienia', 'wiatru', 'zolwia', 'lisa'],
         'ozdoba': ['zabojczy', 'druzgocacy', 'celny', 'krzepki', 'cienia', 'wiatru', 'lisa']},
 'mag': {'bron':   ['tajemny', 'wieszczy', 'plynny', 'zarliwy', 'sowy', 'medrca', 'zrodla'],
         'zbroja': ['krzepki', 'wieszczy', 'plynny', 'tajemny', 'zolwia', 'medrca', 'cienia', 'sowy'],
         'ozdoba': ['tajemny', 'wieszczy', 'plynny', 'zarliwy', 'krzepki', 'medrca', 'sowy', 'zrodla']},
 'ako': {'bron':   ['ostry', 'okrutny', 'zarliwy', 'krwawy', 'pielgrzyma', 'zrodla', 'twierdzy'],
         'zbroja': ['krzepki', 'pancerny', 'zarliwy', 'zolwia', 'twierdzy', 'zrodla', 'pielgrzyma'],
         'ozdoba': ['zarliwy', 'krwawy', 'krzepki', 'twierdzy', 'zrodla', 'medrca', 'pielgrzyma']},
}

# rarity (tiery MMOItems). Od 30.09 rzadkosc wybiera MODYFIKATOR (stats.tier), nie generation.chance:
# grupa rz_<arch>_<kat>_<rodz> (min1 max1) -> rzadkosc -> wariant skladu (np. 2P+1S) -> grupy pref/suf z min=max.
# Zmierzone na klonie 30.09: tier z modyfikatora dziala, min=max daje dokladna liczbe, dolosowanie do min idzie
# proporcjonalnie do chance (dlatego chance = udzial x 0.001).
RARITY = [
 # id, nazwa, kolor, (nieuzywane), glow, udzial w dropie
 ('ZWYKLY', 'Zwykły', '&f', (0, 0), 'WHITE', 0.60),
 ('MAGICZNY', 'Magiczny', '&9', (0, 0), 'BLUE', 0.28),
 ('RZADKI', 'Rzadki', '&e', (0, 0), 'YELLOW', 0.09),
 ('EPICKI', 'Epicki', '&d', (0, 0), 'LIGHT_PURPLE', 0.025),
 ('LEGENDARNY', 'Legendarny', '&6', (0, 0), 'GOLD', 0.005),
 ('UNIKATOWY', 'Unikatowy', '&c', (0, 0), 'RED', 0.0),
 ('ZESTAW', 'Część zestawu', '&a', (0, 0), 'GREEN', 0.0),
]
# sklad affiksow: rzadkosc -> [(prefiksy, sufiksy, waga)], pasmo tierow affiksow
WARIANTY = {
 'MAGICZNY':   ([(1, 0, 0.3), (0, 1, 0.3), (1, 1, 0.4)], 'n'),
 'RZADKI':     ([(2, 1, 0.35), (1, 2, 0.35), (2, 2, 0.30)], 'n'),
 'EPICKI':     ([(2, 2, 0.40), (3, 2, 0.30), (2, 3, 0.30)], 'e'),
 'LEGENDARNY': ([(3, 2, 0.35), (2, 3, 0.35), (3, 3, 0.30)], 'l'),
}
# pasmo -> szanse tierow affiksu (T3, T2); T1 bierze reszte. Wyzszy = lepszy (Last Epoch).
PASMA = {'n': (0.08, 0.30), 'e': (0.25, 0.60), 'l': (0.55, 0.85)}
MAKS_PREF, MAKS_SUF = 3, 3
TIERY_AFF = [('T3', 0, 0), ('T2', 0, 0), ('T1', 0, 1.0)]   # szanse z PASMA

# UNIKATY (osobna galaz, nie drabina rzadkosci): nazwany przedmiot, stale statystyki (z rozrzutem) i umiejetnosc.
# Umiejetnosci inne niz w bonusach setow (krytyka 30.09). Drop: PRZ_LUP_UNIKAT z bossow. Poziom 8.
UNIKATY = {
 'woj': {'id': 'PRZ_UNI_KLINGA_TYSIACA_BLIZN', 'nazwa': 'Klinga Tysiąca Blizn', 'rodzaj': 'miecz', 'mat': 'NETHERITE_SWORD',
         'staty': {'attack-damage': 11.5, 'attack-speed': 1.6, 'physical-damage': 10, 'lifesteal': 2},
         'umiejetnosc': ('CIRCULAR_SLASH', 'ON_HIT', 8, 'Rozpłatanie', 'co 8 s cios tnie wszystkich wokół'),
         'fabula': 'Każda blizna to lekcja. Ta klinga nauczyła wielu.'},
 'nin': {'id': 'PRZ_UNI_SZEPT_OSTATNIEGO_TCHU', 'nazwa': 'Szept Ostatniego Tchu', 'rodzaj': 'sztylet', 'mat': 'NETHERITE_SWORD',
         'staty': {'attack-damage': 6.5, 'attack-speed': 2.2, 'critical-strike-chance': 10, 'critical-strike-power': 15},
         'umiejetnosc': ('DEATH_MARK', 'ON_HIT', 10, 'Znak śmierci', 'co 10 s trafienie znaczy cel na śmierć'),
         'fabula': 'Nikt nie słyszał, jak nadchodzi. Wszyscy słyszeli, jak odchodzi.'},
 'mag': {'id': 'PRZ_UNI_KOSTUR_SPADAJACEGO_NIEBA', 'nazwa': 'Kostur Spadającego Nieba', 'rodzaj': 'kostur', 'mat': 'END_ROD',
         'staty': {'attack-damage': 7.5, 'attack-speed': 1.0, 'magic-damage': 15, 'max-mana': 10},
         'umiejetnosc': ('ARCANE_HAIL', 'ON_HIT', 10, 'Grad arkanów', 'co 10 s trafienie sprowadza grad arkanów'),
         'fabula': 'Gwiazdy też kiedyś spadają. Ten kostur pomaga im wybrać cel.'},
 'ako': {'id': 'PRZ_UNI_MLOT_SEDZIEGO_UMARLYCH', 'nazwa': 'Młot Sędziego Umarłych', 'rodzaj': 'bulawa', 'mat': 'MACE',
         'staty': {'attack-damage': 9.5, 'attack-speed': 1.2, 'skill-damage': 12, 'health-regeneration': 0.2},
         'umiejetnosc': ('SMITE', 'ON_HIT', 8, 'Wyrok', 'co 8 s trafienie ściąga piorun na wroga'),
         'fabula': 'Umarli też mają swój sąd. I tylko jednego sędziego.'},
}
UNIKAT_POZIOM = 8
LEGENDARNE = {}   # od 30.09 Legendarny = wiecej i lepszych affiksow, bez umiejetnosci (decyzja wlasciciela)

# ---------------------------------------------------------------- SETY (2 na archetyp)
# A: 4 sloty zbroi (progi 2/4), poziom 5. B: bron wspolna + naszyjnik, pierscien, charm (progi 2/3/4), poziom 10.
# Bonusy dzialaja DZIS (statystyki + umiejetnosci MythicLib). Kolumna 'docelowo' = wersja pod zasoby klas (etap 3 planu).
SETY = {
 'woj': [
  {'id': 'SLUB_ZELAZNEJ_STRAZY', 'nazwa': 'Ślub Żelaznej Straży', 'poziom': 5, 'waga': 'srednia', 'mat': 'CHAINMAIL', 'trim': ('iron', 'sentry'),
   'czesci': [('Hełm ślubowania', 'm'), ('Kolczuga ślubowania', 'f'), ('Nagolenniki ślubowania', 'p'), ('Sabatony ślubowania', 'p')],
   'progi': {2: {'max-health': 4, 'armor': 2}, 4: {'damage-reduction': 5, 'ability': ('SHOCKWAVE', 'WHEN_HIT', 12)}},
   'opis': {2: '+4 życia, +2 pancerza', 4: '-5% otrzymywanych obrażeń; gdy obrywasz, co 12 s fala uderzeniowa odrzuca wrogów'},
   'docelowo': '4 cz.: przyjęty cios daje podwójny Szał'},
  {'id': 'REGALIA_PIERWSZEGO_WODZA', 'nazwa': 'Regalia Pierwszego Wodza', 'poziom': 10, 'bron': 'miecz',
   'czesci': [('Miecz Pierwszego Wodza', 'm'), ('Naszyjnik Pierwszego Wodza', 'm'), ('Sygnet Pierwszego Wodza', 'm'), ('Znak Pierwszego Wodza', 'm')],
   'progi': {2: {'physical-damage': 6}, 3: {'critical-strike-power': 12, 'max-health': 4}, 4: {'ability': ('DEEP_WOUND', 'ON_HIT', 6)}},
   'opis': {2: '+6% obrażeń fizycznych', 3: '+12% mocy krytyka, +4 życia', 4: 'co 6 s trafienie zadaje głęboką ranę (krwawienie)'},
   'docelowo': '4 cz.: po wydaniu 100 Szału następny cios ogłusza'},
 ],
 'nin': [
  {'id': 'CALUN_NOCNEGO_LISA', 'nazwa': 'Całun Nocnego Lisa', 'poziom': 5, 'waga': 'lekka', 'mat': 'LEATHER', 'dye': (30, 30, 45), 'trim': ('netherite', 'silence'),
   'czesci': [('Kaptur Nocnego Lisa', 'm'), ('Kurta Nocnego Lisa', 'f'), ('Spodnie Nocnego Lisa', 'p'), ('Ciżmy Nocnego Lisa', 'p')],
   'progi': {2: {'dodge-rating': 5}, 4: {'critical-strike-chance': 6, 'movement-speed': 0.01, 'ability': ('SHADOW_VEIL', 'WHEN_HIT', 20)}},
   'opis': {2: '+5% szansy uniku', 4: '+6% szansy na krytyk, szybszy ruch; gdy obrywasz, co 20 s znikasz w cieniu'},
   'docelowo': '4 cz.: detonacja 3 Znamion zwraca 20 Skupienia'},
  {'id': 'BRACTWO_BEZIMIENNYCH', 'nazwa': 'Bractwo Bezimiennych', 'poziom': 10, 'bron': 'sztylet',
   'czesci': [('Kieł Bezimiennych', 'm'), ('Wisior Bezimiennych', 'm'), ('Pierścień Bezimiennych', 'm'), ('Znak Bezimiennych', 'm')],
   'progi': {2: {'critical-strike-chance': 4}, 3: {'projectile-damage': 8, 'critical-strike-power': 10}, 4: {'ability': ('BACKSTAB', 'ON_HIT', 5)}},
   'opis': {2: '+4% szansy na krytyk', 3: '+8% obrażeń pocisków, +10% mocy krytyka', 4: 'co 5 s trafienie w plecy zadaje podwójne obrażenia'},
   'docelowo': '4 cz.: trafienie krytyczne nakłada Znamię'},
 ],
 'mag': [
  {'id': 'SZATY_KREGU_POPIOLOW', 'nazwa': 'Szaty Kręgu Popiołów', 'poziom': 5, 'waga': 'tkanina', 'mat': 'LEATHER', 'dye': (60, 40, 40), 'trim': ('redstone', 'flow'),
   'czesci': [('Kaptur Kręgu Popiołów', 'm'), ('Szata Kręgu Popiołów', 'f'), ('Spodnie Kręgu Popiołów', 'p'), ('Trzewiki Kręgu Popiołów', 'p')],
   'progi': {2: {'max-mana': 8, 'mana-regeneration': 0.2}, 4: {'magic-damage': 10, 'ability': ('FIREBOLT', 'ON_HIT', 8)}},
   'opis': {2: '+8 many, +0,2 regeneracji many', 4: '+10% obrażeń magicznych; co 8 s trafienie wystrzeliwuje ognisty pocisk'},
   'docelowo': '4 cz.: zaklęcie za ≥40 many ma 25% szansy na darmowe echo'},
  {'id': 'INSYGNIA_WIEZY_SWITU', 'nazwa': 'Insygnia Wieży Świtu', 'poziom': 10, 'bron': 'kostur',
   'czesci': [('Kostur Wieży Świtu', 'm'), ('Medalion Wieży Świtu', 'm'), ('Pierścień Wieży Świtu', 'm'), ('Pieczęć Wieży Świtu', 'f')],
   'progi': {2: {'cooldown-reduction': 5}, 3: {'magic-damage': 8, 'max-mana': 6}, 4: {'ability': ('MAGICAL_SHIELD', 'WHEN_HIT', 25)}},
   'opis': {2: '-5% czasu odnowienia', 3: '+8% obrażeń magicznych, +6 many', 4: 'gdy obrywasz, co 25 s magiczna tarcza pochłania ciosy'},
   'docelowo': '4 cz.: nadwyżka many ponad maksimum zamienia się w tarczę'},
 ],
 'ako': [
  {'id': 'ODZIENIE_PASTERZA_DUSZ', 'nazwa': 'Odzienie Pasterza Dusz', 'poziom': 5, 'waga': 'lekka', 'mat': 'LEATHER', 'dye': (70, 75, 70), 'trim': ('quartz', 'wild'),
   'czesci': [('Kaptur Pasterza Dusz', 'm'), ('Ornat Pasterza Dusz', 'm'), ('Spodnie Pasterza Dusz', 'p'), ('Sandały Pasterza Dusz', 'p')],
   'progi': {2: {'max-health': 3, 'health-regeneration': 0.1}, 4: {'skill-damage': 8, 'ability': ('MINOR_HEALINGS', 'WHEN_HIT', 15)}},
   'opis': {2: '+3 życia, +0,1 regeneracji', 4: '+8% obrażeń umiejętności; gdy obrywasz, co 15 s leczysz się'},
   'docelowo': '4 cz.: śmierć sługi daje Duszę, a sługa wybucha'},
  {'id': 'RELIKWIARZ_MILCZACYCH', 'nazwa': 'Relikwiarz Milczących', 'poziom': 10, 'bron': 'bulawa',
   'czesci': [('Buława Milczących', 'f'), ('Różaniec Milczących', 'm'), ('Obrączka Milczących', 'f'), ('Relikwia Milczących', 'f')],
   'progi': {2: {'skill-damage': 6}, 3: {'max-health': 4, 'lifesteal': 2}, 4: {'ability': ('VAMPIRISM', 'ON_HIT', 8)}},
   'opis': {2: '+6% obrażeń umiejętności', 3: '+4 życia, +2% kradzieży życia', 4: 'co 8 s trafienie wysysa życie z wroga'},
   'docelowo': '4 cz.: +1 maks. sługa, słudzy dziedziczą 20% twojej odporności'},
 ],
}
# kolejnosc typow ozdob w secie B (po broni)
SET_B_TYPY = ['NECKLACE', 'RING', 'CHARM']

# ---------------------------------------------------------------- JAKOSC (quality 0-20%)
JAKOSC_MAX = 20
JAKOSC_KROK = '0.9158%'   # skladane: 20 x 0.9158% = +20%
JAKOSC_STATY = ['attack-damage', 'armor', 'armor-toughness', 'max-health', 'physical-damage', 'magic-damage', 'projectile-damage',
                'skill-damage', 'critical-strike-chance', 'critical-strike-power', 'dodge-rating', 'damage-reduction', 'max-mana',
                'mana-regeneration', 'health-regeneration', 'cooldown-reduction', 'lifesteal', 'spell-vampirism', 'block-power', 'block-rating']
# kamienie: id, nazwa, material, referencja (kategoria), stacja, skladniki PROF_*
KAMIENIE = [
 ('PRZ_OSELKA', 'Osełka płatnerza', 'FLINT', 'bron', 'profession-smithing', 'smithing', [('PROF_REFINED_IRON', 2), ('PROF_CUT_STONE', 1)],
  'Przeciągnij na broń: +1% jakości (maks. 20%)'),
 ('PRZ_LATA', 'Łata płatnerska', 'LEATHER', 'zbroja', 'profession-tailoring', 'tailoring', [('PROF_LEATHER_PANEL', 1), ('PROF_CLOTH_BOLT', 1)],
  'Przeciągnij na zbroję: +1% jakości (maks. 20%)'),
 ('PRZ_PYL_JUBILERSKI', 'Pył jubilerski', 'GLOWSTONE_DUST', 'ozdoba', 'profession-jewelcrafting', 'jewelcrafting', [('PROF_CUT_GEM', 1), ('PROF_ENCHANT_DUST', 1)],
  'Przeciągnij na ozdobę: +1% jakości (maks. 20%)'),
]

# ---------------------------------------------------------- polskie etykiety statystyk (language/stats.yml)
ETYKIETY = {
 'attack-damage': '&3 &7➸ Obrażenia: &f{value}',
 'attack-speed': '&3 &7➸ Szybkość ataku: &f{value}',
 'critical-strike-chance': '&3 &7■ Szansa na krytyk: &f<plus>{value}%',
 'critical-strike-power': '&3 &7■ Moc krytyka: &f<plus>{value}%',
 'arrow-velocity': '&3 &7■ Prędkość strzały: &f{value}',
 'two-handed': '&3 &7■ Dwuręczna',
 'lifesteal': '&3 &7■ Kradzież życia: &c+{value}%',
 'spell-vampirism': '&3 &7■ Wampiryzm czarów: &c+{value}%',
 'magic-damage': '&3 &7■ Obrażenia magiczne: &f<plus>{value}%',
 'skill-damage': '&3 &7■ Obrażenia umiejętności: &f<plus>{value}%',
 'physical-damage': '&3 &7■ Obrażenia fizyczne: &f<plus>{value}%',
 'projectile-damage': '&3 &7■ Obrażenia pocisków: &f<plus>{value}%',
 'block-power': '&3 &7■ Siła bloku: &f<plus>{value}%',
 'block-rating': '&3 &7■ Szansa bloku: &f<plus>{value}%',
 'dodge-rating': '&3 &7■ Szansa uniku: &f<plus>{value}%',
 'armor': '&3 &7✠ Pancerz: &f<plus>{value}',
 'armor-toughness': '&3 &7✠ Wytrzymałość pancerza: &f<plus>{value}',
 'knockback-resistance': '&3 &7✠ Odporność na odrzut: &f<plus>{value}%',
 'max-health': '&3 &7❤ Życie:&c <plus>{value}',
 'movement-speed': '&3 &7■ Szybkość ruchu: &f<plus>{value}',
 'defense': '&3 &7■ Obrona: &f<plus>{value}',
 'damage-reduction': '&3 &7✠ Redukcja obrażeń: &f<plus>{value}%',
 'health-regeneration': '&3 &7■ Regeneracja życia: &f<plus>{value}',
 'max-mana': '&3 &7■ Mana: &f<plus>{value}',
 'mana-regeneration': '&3 &7■ Regeneracja many: &f<plus>{value}',
 'cooldown-reduction': '&3 &7■ Skrócenie odnowienia: &f<plus>{value}%',
 'tier': '{value}',
 'required-level': '&eWymaga poziomu {value}',
 'required-dexterity': '&eWymaga &c{value} &eZręczności',
 'required-strength': '&eWymaga &c{value} &eSiły',
 'required-intelligence': '&eWymaga &c{value} &eIntelektu',
 'required-wiara': '&eWymaga &c{value} &eWiary',
 'additional-dexterity': '&3 &7■ Zręczność: &f<plus>{value}',
 'additional-strength': '&3 &7■ Siła: &f<plus>{value}',
 'additional-intelligence': '&3 &7■ Intelekt: &f<plus>{value}',
 'additional-wiara': '&3 &7■ Wiara: &f<plus>{value}',
}

# ---------------------------------------------------------- LUP (MythicMobs droptables/Przedmioty.yml)
# pasmo tieru moba -> poziomy baz, szansa na przedmiot przy zabiciu (wpis w DT_BEST_*), liczba przedmiotow
LUP = {
 'T1':   {'bazy': [1, 3], 'szansa': 0.05, 'ile': 1},
 'T2':   {'bazy': [3, 5, 7], 'szansa': 0.10, 'ile': 1},
 'T3':   {'bazy': [5, 7, 10], 'szansa': 0.30, 'ile': 1},
 'BOSS': {'bazy': [7, 10], 'szansa': 1.0, 'ile': 2},
}
# czesci setow z bossow: set A (poz. 5) i set B (poz. 10) — szansa na jedna czesc
LUP_SET = {'A': 0.20, 'B': 0.08}
LUP_UNIKAT = 0.04   # szansa na unikat z bossa
# placeholder archetypu gracza (woj|nin|mag|ako) do smart loot; None = lup bez preferencji
ARCHETYP_PH = '%coretools_var_player_klasa_archetyp%'  # wlaczany flaga --smart (generuj.py), gdy placeholder dziala na serwerze
SMART_UDZIAL = 0.75
# wpiecie: prefiks droptabel bestiariusza -> pasmo
WPIECIE = {'DT_BEST_T1_': 'T1', 'DT_BEST_T2_': 'T2', 'DT_BEST_T3_': 'T3', 'DT_BEST_BOSS_': 'BOSS'}

# krotkie nazwy statystyk do linii affiksu (◆◆◇ Okrutny · obrazenia fizyczne)
STAT_KROTKO = {'attack-damage': 'obrażenia', 'physical-damage': 'obrażenia fizyczne', 'lifesteal': 'kradzież życia',
 'critical-strike-power': 'moc krytyka', 'critical-strike-chance': 'szansa na krytyk', 'projectile-damage': 'obrażenia pocisków',
 'attack-speed': 'szybkość ataku', 'magic-damage': 'obrażenia magiczne', 'max-mana': 'mana', 'mana-regeneration': 'regeneracja many',
 'skill-damage': 'obrażenia umiejętności', 'max-health': 'życie', 'armor': 'pancerz', 'damage-reduction': 'redukcja obrażeń',
 'knockback-resistance': 'odporność na odrzut', 'health-regeneration': 'regeneracja życia', 'movement-speed': 'szybkość ruchu',
 'dodge-rating': 'unik', 'armor-toughness': 'wytrzymałość pancerza', 'cooldown-reduction': 'skrócenie odnowienia', 'additional-strength': 'Siła',
 'additional-dexterity': 'Zręczność', 'additional-intelligence': 'Intelekt', 'additional-wiara': 'Wiara'}
PIPS = {'T1': '&6◆&8◇◇', 'T2': '&6◆◆&8◇', 'T3': '&6◆◆◆'}

SLOT_PL = {'HELM': 'Hełm', 'KLATA': 'Napierśnik', 'NOGI': 'Nogawice', 'BUTY': 'Buty'}

# ---------------------------------------------------------- ROZBIORKA (deconstruct MMOItems): tier -> [(id PROF_*, szansa %, ilosc)]
# Zasila kamienie jakosci (oselka: REFINED_IRON+CUT_STONE, lata: LEATHER_PANEL+CLOTH_BOLT, pyl: CUT_GEM+ENCHANT_DUST).
ROZBIORKA = {
 'ZWYKLY':     [('PROF_ORE_CHUNK', 100, '1-1'), ('PROF_ENCHANT_DUST', 20, '1-1')],
 'MAGICZNY':   [('PROF_ENCHANT_DUST', 100, '1-2'), ('PROF_REFINED_IRON', 50, '1-1'), ('PROF_LEATHER_PANEL', 30, '1-1')],
 'RZADKI':     [('PROF_ENCHANT_DUST', 100, '2-3'), ('PROF_CUT_GEM', 50, '1-1'), ('PROF_REFINED_IRON', 60, '1-2'), ('PROF_CLOTH_BOLT', 40, '1-1')],
 'EPICKI':     [('PROF_ENCHANT_DUST', 100, '3-4'), ('PROF_CUT_GEM', 100, '1-2'), ('PROF_CHARGED_RUNE', 30, '1-1')],
 'LEGENDARNY': [('PROF_AWAKENING_SEAL', 100, '1-1'), ('PROF_CUT_GEM', 100, '2-3'), ('PROF_ENCHANT_DUST', 100, '4-6')],
}
NOZ = ('PRZ_NOZ_ROZBIORKI', 'Nóż rozbiórkowy', 'SHEARS', 'profession-smithing', 'smithing', [('PROF_ORE_CHUNK', 2)], 4,
       'Przeciągnij na przedmiot z łupu: rozbiera go na surowce profesji (nie działa na zestawy i unikaty).')
