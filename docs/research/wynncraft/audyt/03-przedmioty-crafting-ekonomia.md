# 03. Przedmioty, identyfikacje, crafting, profesje, ekonomia

Źródło liczb: `GET /v3/item/database?fullResult` (6 725 rekordów, 6,1 MB, 23.09.2026), `/v3/item/recipe/database?fullResult` (720 receptur), `/v3/item/sets`, `/v3/item/metadata`, `/v3/map/gathering-nodes`. Wszystko liczone skryptem `skrypty/stats_api.py`, wyniki w `dane/`.

---

## 1. Baza przedmiotów: ile i jakie

**[F]** 6 725 wpisów (`dane/items_typ_podtyp.csv`):

| Typ | Liczba | Podtypy |
|---|---|---|
| weapon | 2 390 | bow 496, dagger 490, spear 488, wand 478, relik 438 (po jednym typie na klasę) |
| armour | 2 099 | helmet 571, chestplate 544, boots 493, leggings 491 |
| accessory | 924 | ring 348, bracelet 294, necklace 282 |
| ingredient | 972 | dla 8 profesji rzemieślniczych |
| tome | 172 | weapon 60, armour 41, mysticism 17, expertise 17, guild 16, lootrun 12, marathon 9 |
| material | 112 | po 28 na każdą z 4 profesji zbierackich |
| tool | 52 | axe/pickaxe/rod/scythe po 13 tierów |
| charm | 4 | Void, Stone, Light, Worm |

**Rzadkość ekwipunku bojowego (5 413 szt.)**: unique 2 059, rare 1 628, legendary 920, normal 469, fabled 252, mythic 85.
- **[F]** Rzadkość „set” usunięto w 2.2.0 („Removed the Set rarity… Sets still give a bonus”, Version 2.2.0). W API zostało 71 setów, najczęściej 2–4 części.

**Rozkład poziomów** (`dane/gear_poziom_x_rzadkosc.csv`):

| Poziom | Unique | Rare | Legendary | Fabled | Mythic | Razem |
|---|---|---|---|---|---|---|
| 0–9 | 112 | 37 | 19 | 0 | 0 | 219 |
| 30–39 | 170 | 123 | 61 | 7 | 0 | 395 |
| 60–69 | 192 | 134 | 50 | 24 | 2 | 439 |
| 90–99 | 164 | 236 | 190 | 36 | 43 | **709** |
| 100–109 | 193 | 135 | 121 | 58 | 16 | 553 |
| 110–119 | 233 | 170 | 98 | 43 | 19 | 598 |
| 120 | 22 | 15 | 13 | 16 | 1 | 73 |

- **[F]** Na każdy przedział 10 poziomów przypada 220–700 przedmiotów, z pikiem na 90–99. Najpopularniejsze poziomy to 95 (135 szt.) i 98, 80, 55, 100.
- **[F]** Legendarne zaczynają się już na poziomie 1, fabled od 30, mythic od 65.
- **[W]** Gęsty strumień przedmiotów na każdym etapie sprawia, że gracz „ciągle wymienia sprzęt” (film Selek). U nas MMOItems + generator (tools/gen.sh) mogą produkować podobne serie. Warunek: tabele dropów per strefa.

## 2. Identyfikacje (losowane statystyki)

- **[F]** Na ekwipunku występuje **90 różnych identyfikacji** (`dane/identyfikacje_czestosc.csv`). Najczęstsze: walkSpeed (32,8% przedmiotów), 5× raw skill point (18–20% każdy), healthRegenRaw, manaSteal, manaRegen, lifeSteal (po 16–19%), combatExperience 15,7%, spellDamage 15%, lootBonus 9,7%.
- **[F]** Średnio ID na przedmiot: unique 5,3, rare 5,6, legendary 5,9, fabled 5,9, **mythic 7,0** (maks. 10–15). Normal ma 0.
- **[F]** Wartości ID: **17 536 losowanych** (`{min, raw, max}`) i 9 976 stałych (np. skill pointy są stałe).
- **[F]** Zakres losowania policzony ze wszystkich ID: dla dodatnich `min` ≈ **30% wartości bazowej** (mediana 0,30), a `max` ≈ **130%** (mediana 1,30). Dla ujemnych odwrotnie: od 130% do 70%. **[W]** Formuła Wynna to „baza × [0,3 ; 1,3]” dla bonusów i „× [1,3 ; 0,7]” dla kar.
- **[F]** Wypadnięte przedmioty (od unique wzwyż) są **niezidentyfikowane**. Identifier NPC (43 punkty na mapie, znaczniki API) identyfikuje za emeraldy, a ponowna identyfikacja kosztuje 5× cenę (wiki Identifier).
- **[F]** Od 2.2 tooltip pokazuje, **jak dobrze wylosowało się ID**, bez modów: „you can see how good that stat roll is without mods… how much the little circles are filled” (Reddit 1sryesc). Pack ma paski `tooltip/identification/meter*.png` (`klatki/P_m_tooltip_nameplate.png`, `klatki/V11_…`).
- **[F]** **Major ID** (zmiana mechaniki, np. „Ignition Whirlwind: Blazing Powder adds +8 rotations to spin attack…”) ma 267 przedmiotów z 165 różnymi major ID: fabled 206, mythic 45, legendary 13, rare 3.
- **[W]** Losowe rolle z paskiem jakości to rdzeń „loot shootera” (Reddit: „borderlands-esque loot shoot mechanics”). MMOItems ma losowe staty (RNG w templates) i „unidentified items”. Pasek jakości w lore trzeba zrobić sami (glif Nexo + placeholder MMOItems). Do sprawdzenia w docs MMOItems: czy lore ma dostęp do „procent rolla”.

## 3. Statystyki a poziom (krzywe)

**[F]** Mediany na przedział 5 poziomów (`dane/krzywa_hp_napiersnika.csv`, `krzywa_dps_broni.csv`):

| Poziom | HP napierśnika (mediana / max) | DPS broni (mediana / max) |
|---|---|---|
| 0–4 | 25 / 25 | 11 / 34 |
| 20–24 | 100 / 185 | 68 / 154 |
| 50–54 | 630 / 850 | 202 / 389 |
| 80–84 | 2 150 / 4 250 | 430 / 676 |
| 100–104 | 3 500 / 5 850 | 482 / 930 |
| 115–119 | 4 238 / 5 900 | 532 / 901 |

- **[F]** HP rośnie mniej więcej liniowo, o ~40 HP/poziom od 20 do 100, i spłaszcza się po 100. DPS broni rośnie szybko do ~95, potem stoi (482–544). **[W]** Po 95 siła przychodzi z ID, major ID, aspektów i powderów, a nie z bazy broni. Gracz wysokopoziomowy ma ~21 000 HP (klatki 2026), głównie z ID i setów.
- **[F]** Rozkład szybkości ataku (7 kategorii, od superSlow do superFast): najwięcej normal 505, slow 493, fast 473.
- **[F]** Elementy: 21% ekwipunku jest neutralne, 38% ma 1 żywioł, 33% 2, 3,4% ma 5 („rainbow”).

## 4. Powdery (gniazda), upgrade, ascension

- **[F]** 5 żywiołów × 7 tierów = 35 powderów. Tiery VI–VII dodano w 2.2.0 („Powders can now go up to Tier VII and can now be directly applied to items”).
- **[F]** Sloty powderów (broń i zbroja): 0 slotów 506 szt., 1 824, **2 1 673**, **3 1 270**, 4 141, 5 62. Wyjątki mają 6–18 slotów (craftowane albo specjalne).
- **[F]** Kolejność nakładania ma znaczenie: pierwszy powder wyznacza „powder special” broni (wiki Powders). Upgrade to 4 powdery → 1 wyższy w **Item Upgrader** (zastąpił Powder Mastera w 2.2.0). Są też zakładki Ascension (mythiki: po jednym Wardzie każdego koloru, 30% szansy, z drugim mythikiem 100%), Pouches i Tools (`klatki/P_m_interface_b.png`).

## 5. Crafting (8 profesji rzemieślniczych)

- **[F]** 720 receptur: tailoring 96, armouring 96, weaponsmithing 96, woodworking 144, jeweling 144, cooking 48, alchemism 48, scribing 48. Na każdy typ wyniku przypada **48 receptur** (15 typów: 4 zbroje, 5 broni, 3 akcesoria, food, potion, scroll).
- **[F]** Receptury idą w przedziałach po 2–3 poziomy (1–3, 3–5, …, 117–119). Wymagają 2 materiałów, np. „Boots 117–119: 7× Refined Cinnabar Ingot + 14× Refined Heather String”, i dowolnie do **6 składników** (wiki Crafting).
- **[F]** Składniki: 972, tiery 0–3 (233/250/294/195). Przypisane do profesji: armouring 255, tailoring 252, weaponsmithing 226, woodworking 207, alchemism 186, cooking 160, scribing 148, jeweling 138. **824 z 972 wypada z potworów**, łącznie z 1 196 różnych mobów.
- **[F]** 47 składników ma **modyfikatory pozycji** (left/right/above/under/touching/notTouching), więc układ w siatce 2×3 zmienia efekt (`ingredientPositionModifiers`).
- **[F]** Składnik to ID dla przedmiotu (np. +3–5 do obrażeń żywiołów) i koszt (durabilityModifier, wymagania SP, czas trwania konsumpcji).
- **[F]** Craftowane przedmioty mają trwałość i trzeba je naprawiać u Blacksmitha. Blacksmith stoi też w pokojach buffów raidu. Tryb Craftsman pozwala nosić tylko craftowane (plus 253 przedmioty z flagą `allowCraftsman`).
- **[F]** Gracze chwalą: „I genuinely think this game has the best gathering / crafting out of any other MMO” (Reddit 1qs4rl6, 49 pkt). Krytyka: „Crafting Professions are a Nightmare” (film Wynncraft Completionist, https://youtu.be/VQ9dAXZsxJI).

## 6. Profesje zbierackie i mapa surowców

- **[F]** 4 profesje: mining, woodcutting, fishing, farming. **52 rodzaje surowca** (po 13 na profesję), nowe co 10 poziomów, od 1 do 115 (`dane/wezly_zbierackie.csv`), np. Copper/Oak/Gudgeon/Wheat (1) → Cinnabar/Redwood/Mahseer/Heather (115).
- **[F]** Na mapie jest **16 787 węzłów zbierackich** (NODE 15 381, CORNER 843, WALL 563). Najwięcej: Oak 2 836, Pine 1 521, Light 1 372, Jungle 1 000.
- **[F]** Narzędzia T1–T13. Nowy surowiec da się zbierać dowolnym narzędziem, lepsze tylko przyspiesza (wiki Professions). Rafinacja zaczyna od 70% sukcesu, rośnie o 2% na poziom do 90%. Materiał wypada w 3 tierach jakości: 70% / 10% / 1% (`chances` w API).
- **[F]** Profesje mają limit 132. Ostatnie odblokowanie to 115 (zbierackie) i 117 (rzemieślnicze).
- **[F]** Materiały mają kilka zastosowań: „Feed to your Mount… Can be used to purchase blocks in Housing, or to craft Rings…” (lore Cinnabar Gem).
- **[W]** Jeden surowiec z trzema zastosowaniami (crafting, wierzchowiec, housing) chroni wartość zbierania. U nas Iris generuje bloki Nexo (`iris-generuje-bloki-nexo`), więc węzły można rozsiać generatorem, a MMOCore professions obsłuży XP.

## 7. Waluta i ekonomia

### Jednostki
- **[F]** Emerald ²; Emerald Block (EB) = 64 E; Liquid Emerald (LE) = 64 EB = 4 096 E; stack LE (STX) = 64 LE = 262 144 E (Wynntils `EmeraldUnits.java`). Emeraldy leżą fizycznie w ekwipunku. Emerald Pouch w tierach I–X zbiera je i kompresuje (runda 2).

### Źródła (faucets)
- **[F]** Sprzedaż u Blacksmitha, questy (~64 LE na klasę za wszystkie według wiki Emeralds), dungeony (piramidki emeraldów przy wejściu), raidy (nagrody z „Liquified Emerald”), lootruny, nagrody sezonów gildii (2 048–102 400 E), terytoria gildii (9 000 E/h bazowo z terytorium), nurkowanie w Seavale Reef (~2 LE/h).

### Upusty (sinks), które udało się zmierzyć
| Upust | Koszt | Źródło |
|---|---|---|
| Strony banku (21) | 11 E (str. 2) → 251 189 E (str. 21), razem 1 539 094 E ≈ 5,9 STX; próg poziomu 5–100 | wiki Bank |
| Identyfikacja | opłata za przedmiot, ponowna ×5 | wiki Identifier |
| Wierzchowiec | 24 EB | wiki Mounts |
| Wyspa housingu | 4 EB za każdą dodatkową, przeniesienie 4 EB | wiki Housing |
| Założenie gildii | 4 EB (od 2.2.4, poziom 5); odblokowanie wojen 20 LE od poziomu gildii 50 | ogłoszenie 2.2.4 |
| Atak na terytorium | 0 / 200 / 800 / 2 000 / 4 000 E emeraldów gildii za kolejne | film TheNelston, 18.02.2024 |
| Scroll na Legendary Island | 7 168 E | wiki Legendary Island |
| Teleport scroll | emeraldy + 1 Soul Point | wiki Soul Points |
| Runy (klucze do raidów i skorumpowanych dungeonów) | rynek | wiki Runes |
| Naprawa craftowanych | Blacksmith | wiki |
| Opłata rynku | 5% od kupującego | wiki Trade Market |

### Trade Market
- **[F]** Globalny rynek z NPC w dużych miastach (17 lokacji crowdsourcingowo, znaczniki: Trade Market 11 + Trading Market 18). Sloty ofert: 5 bez rangi, 10 VIP+, 15 HERO, 18 HERO+, 21 CHAMPION (wiki). Są zlecenia kupna i sprzedaży, „price check” (Highest Seller, Lowest Buyer, 2 Week Average) i płatne „Reveal Item Names” dla niezidentyfikowanych.
- **[F]** Okna mają 7 stanów z osobnymi glifami (overview/buy/sell/order/filter/list, `dane/wynntils_okna.csv`) i stylizację **straganu z markizą** (`klatki/P_m_interface_a.png`).
- **[F]** **Booth Shops**: tymczasowe stoiska graczy w świecie (109 lokacji) sprzedające to, co gracz ma na rynku. Czas trwania zależy od rangi. HERO+ ma stałe stoisko na wyspie housingu.
- **[F]** Wynntils i mod Wynnventory crowdsourcingowo zbierają ceny, bo gra nie pokazuje historii („There's a mod called wynnventory that shows market data on each item”, Reddit 1sryesc).

### Inflacja i opinie
- **[F]** Ten sam mythik (Discoverer) przed aktualizacją lootrunów miał ofertę 26 STX, a po niej był wystawiony za 6 STX (film LunaCrash, 21.09.2024). „we just went from scraping for scraps to straight up Venezuela”.
- **[F]** Szanse: mythik z moba lub skrzyni 1/142 857; z pulla lootrunu ~1/2 500; shiny 1/100 000 na pull; Forgery 1,5% z mechanizmem pity (wiki Mythic Items).
- **[F]** Silverbull Shares ($1 = 1 share) sprzedaje się za emeraldy na rynku po ~20–32 LE (Reddit 1uw4ek3, lipiec 2026), więc da się **kupić rangę za emeraldy**. Szczegóły w `06-…`.

## 8. Magazyn gracza

- **[F]** Bank konta (21 stron), bank postaci (12 stron, HERO+ +8), block bank, misc bucket i tome bookshelf (2–12 stron według rangi), island block bank, Ingredient Pouch (27 slotów), Emerald Pouch (runda 2, wiki Ranks). W 2.2 bank podzielono na Personal i Account (Reddit 1r0rnaq).
- **[W]** Magazyn to istotna dźwignia monetyzacji: postaci jako magazyn (runda 2), strony zależne od rangi. U nas MMOProfiles + CMI/MMOInventory. Uwaga na UX: 6 różnych banków to dużo.

## 9. Wnioski dla nas [W]

1. **Rolle ID z widocznym paskiem jakości** i „niezidentyfikowany” drop z opłatą za identyfikację: dwa w jednym (napięcie przy dropie + upust emeraldów). MMOItems ma natywnie „unidentified”.
2. **Sloty gniazd z kolejnością** (powdery) → MMOItems gem stones mają sloty kolorów. „Special” zależny od pierwszego kamienia: tylko skryptem.
3. **Receptury per przedział poziomu + dowolne składniki z modyfikatorami pozycji**: MMOItems crafting stations mają receptury, ale bez „swobodnych składników”. Pełny model Wynna wymaga własnego pluginu. Na start: stałe receptury.
4. **Jeden materiał, trzy zastosowania** (crafting, mounty, housing), żeby zbieranie się opłacało.
5. **Rozpisać upusty emeraldów przed startem** (bank, identyfikacja, teleport, naprawy). Wynn ma ich kilkanaście, a mimo to walczy z inflacją mythików.
6. **Rynek z price-checkiem** (średnia 2 tyg.) oszczędza graczom modów. Na naszym stacku potrzebny plugin rynku, nie mamy go (kandydaci do sprawdzenia w docs: CMI ma `/cmi sell`? Nie jest to aukcja).
