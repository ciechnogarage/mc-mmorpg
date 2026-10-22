# Itemizacja 03 — stan u nas (inwentaryzacja 2026-09-29)

Tylko odczyt. Liczby z parsera (pyyaml w dockerze `python:3.12-slim`), nie z grepa. Ścieżki względem
`/opt/docker/mc/data/world1`. Wartości statystyk MMOCore liczone jako `base + per-level × (poz − 1)` (LinearValue).

---

## 1. Klasy, biegłości, grupy broni

Źródło prawdy: `tools/klasy-rasowe/generuj_klasy.py` (tabele: `BRON` l.22-44, `ARCH` l.47-52, `STAT` l.55-69,
`KLASY` l.82-134, `DOMYSLNE` l.174, `rasowe()` l.186-196, zestaw startowy l.371-386). Wynik dla evali:
`tools/klasy-rasowe/oczekiwane.json` (klucze `klasy` 48, `mapa` l.734, `bieglosci` l.752 = 3 zbroje + 21 broni).
Opis: `docs/klasy-rasowe/rozpiska-16-klas.md`.

**Zbroja archetypu:** Wojownik L+Ś, Ninja L, Mag tylko tkanina, Akolita L. Tkanina NIE ma wymagania.
Ciężką (C) mają WYŁĄCZNIE specjalizacje obronne: Bastion, Serafin, Strażnik Cierni, Tyran, Strażnik Grobu.

### 16 klas bazowych (+ 2 specjalizacje na 10 poz.)

| Rasa | Arch. | Klasa | Zbroja | Grupy broni | Spec A (stat, +dodatki) | Spec B (stat, +dodatki) |
|---|---|---|---|---|---|---|
| Człowiek | wojownik | Rycerz | L Ś | miecz, drzewcowa | Bastion (defense, +C +tarcza) | Czempion (physical-damage) |
| Człowiek | ninja | Agent | L | sztylet, luk, kusza | Widmo (crit-chance) | Jastrząb (projectile-damage) |
| Człowiek | mag | Mag Akademii | – | kostur, grimuar | Elementalista (magic-damage) | Przywoływacz (cooldown-reduction) |
| Człowiek | akolita | Święty Akolita | L Ś | bulawa, tarcza | Serafin (defense, +C) | Hierofant (max-mana) |
| Elf | wojownik | Ostrze Gaju | L Ś | miecz, klinga | Strażnik Cierni (defense, +C +tarcza) | Tancerz Ostrzy (dodge-rating) |
| Elf | ninja | Strażnik Kniei | L | sztylet, luk, luk_dlugi | Cień Liści (crit) | Sokół (projectile) |
| Elf | mag | Tkacz Arkanów | – | kostur, kostur_dwureczny | Tkacz Żywiołów (magic-damage) | Strażnik Żywiołów (magic-damage-reduction, +L) |
| Elf | akolita | Wyrocznia | L | bulawa, sierp | Ostrze Księżyca (crit, +Ś) | Opiekun Duchów (mana-regeneration) |
| Ork | wojownik | Wódz Wojny | L Ś | miecz, topor | Tyran (defense, +C +tarcza) | Krwioburca (crit) |
| Ork | ninja | Tropiciel | L Ś | sztylet, luk, wlocznia | Łowca Bestii (max-health) | Łowca Kości (projectile) |
| Ork | mag | Szaman | L | kostur, totem | Burzownik (magic-damage) | Totemista (cooldown-reduction) |
| Ork | akolita | Kapłan Krwi | L | bulawa, mlot_dwureczny | Awatar Wojny (physical-damage, +Ś) | Szept Przodków (max-mana) |
| Nieumarły | wojownik | Rycerz Śmierci | L Ś | miecz, miecz_dwureczny | Strażnik Grobu (defense, +C +tarcza) | Żniwiarz (lifesteal) |
| Nieumarły | ninja | Widmo Grobu | L | sztylet, luk, lancuch | Cień (crit) | Strzelec Grobu (projectile) |
| Nieumarły | mag | Mag Grobu | – | kostur, filakterium | Duszożerca (spell-vampirism) | Nekromanta (max-mana) |
| Nieumarły | akolita | Spaczony Akolita | L | bulawa, kosa | Awatar Otchłani (lifesteal, +Ś) | Pasterz Umarłych (mana-regeneration) |

Statystyka-podpis spec (`STAT`, l.55-69): defense 5/+0.3, crit 5/+0.2, projectile 5/+0.2, magic-damage 5/+0.2,
CDR 5/+0.1, max-mana +8/+0.4, mana-regen +0.2/+0.02, physical 5/+0.2, dodge 5/+0.1, lifesteal 2/+0.05,
spell-vamp 2/+0.05, max-health +4/+0.2, MDR 5/+0.1. Delty rasy: Człowiek CDR+5; Elf ruch×1.1, mana-regen+0.1, HP×0.9;
Ork HP×1.15, physical+10, mana×0.8; Nieumarły lifesteal (mag/akolita: spell-vamp) +3, ruch×0.95.

### 21 grup broni → typy MMOItems (`generuj_klasy.py` l.22-44)

| Grupa (atrybut `bieglosc_<g>`) | Typy MMOItems | Kto (klasy bazowe) | Ręce |
|---|---|---|---|
| miecz | SWORD, LONG_SWORD, THRUSTING_SWORD | 4 wojowników | 1H |
| sztylet | DAGGER, GAUNTLET | 4 ninja | 1H |
| luk | BOW | 4 ninja | łuk |
| kostur | WAND, STAFF, STAVE | 4 magów | 1H (STAVE parent HAMMER) |
| bulawa | HAMMER | 4 akolitów | 1H |
| tarcza | SHIELD (parent CATALYST) | Święty Akolita + 4 spec obronne | druga ręka |
| drzewcowa | HALBERD, LANCE | Rycerz | 2H (halabarda startowa) |
| kusza | CROSSBOW, MUSKET | Agent | |
| grimuar | TOME (parent CATALYST) | Mag Akademii | druga ręka / fokus |
| klinga | KATANA | Ostrze Gaju | 1H |
| luk_dlugi | GREATBOW | Strażnik Kniei | 2H |
| kostur_dwureczny | GREATSTAFF | Tkacz Arkanów | 2H |
| sierp | SIERP (nowy, parent SWORD) | Wyrocznia | 1H |
| topor | AXE, GREATAXE | Wódz Wojny | 1H / GREATAXE |
| wlocznia | SPEAR | Tropiciel | 1H |
| totem | TALISMAN (parent CATALYST) | Szaman | fokus, `disable-interaction` |
| mlot_dwureczny | GREATHAMMER | Kapłan Krwi | 2H |
| miecz_dwureczny | GREATSWORD | Rycerz Śmierci | 2H |
| lancuch | WHIP | Widmo Grobu | 1H |
| filakterium | FILAKTERIUM (nowy, parent CATALYST) | Mag Grobu | fokus |
| kosa | KOSA (nowy, parent SWORD) | Spaczony Akolita | 2H |

Typy WOLNE (bez biegłości, każdy może nosić): LUTE, CATALYST, MAIN_CATALYST, OFF_CATALYST, ORNAMENT, TOOL,
akcesoria. `two-handed` to statystyka PRZEDMIOTU, nie typu — dziś mają ją: HALABARDA_STRAZNIKA, LUK_DLUGI_KNIEI,
KOSTUR_ZYWEGO_DREWNA, MLOT_BOJOWY, MIECZ_RUNICZNY, KOSA_SPACZONA. Katalizatory (TOME/TALISMAN/FILAKTERIUM) bez
biegłości da się trzymać, ale nie dają statystyk; tarcza bez biegłości nie blokuje (rozpiska, "Zmierzone").
Typy w `plugins/MMOItems/item-types.yml`: bazowe l.12-392, podtypy l.409-771, SIERP l.786, KOSA l.801, FILAKTERIUM l.816.
Wymagania: `required-bieglosc-<g>: 1` (podkreślenie → myślnik, np. `required-bieglosc-luk-dlugi`), zbroja
`required-bieglosc-lekka|srednia|ciezka`. `required-class` zdjęty ze wszystkich przedmiotów. Atrybuty biegłości:
`plugins/MMOCore/attributes/bieglosci_zbroi.yml`, `bieglosci_broni.yml` (max-points 0, wartość tylko z klasy). Zasada:
**żaden przedmiot nie daje biegłości** (`bieglosci_zbroi.yml` l.10-14).

### Jak piszą generatory (żeby nowy nie kolidował)

| Skrypt | Co pisze | Sposób |
|---|---|---|
| `generuj_klasy.py` | 48 plików `MMOCore/classes/<rasa>/*.yml`, `attributes/bieglosci_broni.yml`, `gui/subclass-select.yml` (kafelki sub-*), `CoreTools/Scripts/klasy-rasowe.yml`, `CoreTools/Variables/klasy-rasowe.yml`, sekcje `races:` w `CharacterCreator/config.yml`, `oczekiwane.json` | całe pliki nadpisywane; importuje `START_BRON/START_ZBROJA` z tresc_startowa |
| `latacz_broni.py` | dopisuje typy SIERP/KOSA/FILAKTERIUM do `item-types.yml` (jeśli brak), tworzy `item/{sierp,kosa,filakterium}.yml` (jeśli brak) | **KAŻDEMU** przedmiotowi typu z `BRON` w `base:` dopisuje `required-bieglosc-<g>: 1.0` (jeśli brak); **usuwa `required-class`** z każdego przedmiotu (regex `^    required-class:`) |
| `tresc_startowa.py` | blok `# >>> KLASY-RASOWE: …` / `# <<< KLASY-RASOWE` (l.21) w `item/<typ>.yml` (20 plików, np. armor.yml l.1638-1957, sword.yml l.479-546) | `wstaw_blok` (l.84-88) WYCINA blok i DOKLEJA go na KONIEC pliku |
| `tresc_startowa.py` §2 | wagi dla zbroi w `armor.yml` PRZED markerem | każdy przedmiot bez `required-bieglosc-*` dostaje wagę wg materiału: LEATHER/TURTLE→lekka, CHAINMAIL/GOLDEN→średnia, IRON/DIAMOND/NETHERITE→ciężka (l.113-130); wyjątek tylko `TEST_ZBROJA_*` |
| `tresc_startowa.py` §3 | `language/lore-format.yml` (linie `#required-bieglosc-*#` wstawiane PO `'#required-intelligence#'`), `language/stats.yml` (usuwa i dopisuje `required-bieglosc-*`) | tekstowo |
| `tresc_startowa.py` §4-6 | `CoreTools/item-replacer.yml` (CAŁY plik), `MMOItems/config.yml` `disable-vanilla-recipes` (l.304), CMI kit Newbie | nadpisanie |
| `tools/handel/generuj.py` | `CoreTools/AuctionHouse/rynek.yml`, `Shops/sklep.yml`, `Shops/skup.yml` | całe pliki; typy sprzętu na aukcji zaszyte na sztywno (`TYPY_SPRZETU` l.38-43) |

---

## 2. Statystyki klas MMOCore i atrybuty

Szkielety: `MMOCore/classes/warrior.yml` (mana "Rage" l.97-104, attributes l.153), `rogue.yml`, `mage/mage.yml`
(mana "Mana" l.101-108), `paladin.yml`. Klucz w pliku klasy ZASTĘPUJE domyślną MMOCore (`MMOCore/stats.yml`:
HP 20, mana 20, mana-regen 0.166 +0.03/poz., attack-damage 1, attack-speed 4, ruch 0.1). Pancerza (`armor`) nie ma
ŻADNA klasa — cały pancerz pochodzi z przedmiotów; `defense` tylko u 5 spec obronnych (5 +0.3/poz.).

| Archetyp (rasa) | HP poz.1/5/10 | Zasób poz.1/5/10 | attack-damage | inne |
|---|---|---|---|---|
| Wojownik (Czł./Nieum.) | 24 / 26 / 28.5 | Rage 20/24/29, regen −0.5 | 2 / 2.4 / 2.9 | att-speed 4.2+0.05, ruch 0.12, speed-malus-red. 50 |
| Wojownik Elf | 21.6 / 23.4 / 25.65 | Rage 20/24/29 | j.w. | ruch 0.132 |
| Wojownik Ork | 27.6 / 29.9 / 32.8 | Rage 16/19.2/23.2 | j.w. | physical +10% |
| Ninja (Czł./Nieum.) | 18 stałe | brak sekcji mana → domyślna 20 | 1 (domyślne) | att-speed 4.2, ruch 0.105 |
| Ninja Elf / Ork | 16.2 / 20.7 stałe | 20 / 16 | 1 | Ork physical +10% |
| Mag (Czł./Nieum.) | 18 stałe | Mana 27 / 31.8 / 37.8, regen 0.2+0.04 | 1 | hp-regen 0.13 |
| Mag Elf / Ork | 16.2 / 20.7 | 27…37.8 / 21.6…30.24 | 1 | |
| Akolita (Czł./Nieum.) | 22 / 23.2 / 24.7 | brak sekcji → domyślna 20 | 1 | att-speed 3.7, KB-res 0.3, ruch 0.095 |
| Akolita Elf / Ork | 19.8…22.2 / 25.3…28.4 | 20 / 16 | 1 | |

Wszystkie klasy: max-level 100, `exp-curve: '{level} * 200'`, `exp-table: class_exp_table`
(`MMOCore/exp-tables/default_exp_tables.yml` l.46-67: **1 punkt atrybutu co poziom**, punkt drzewka co 2 poz.,
punkt klasy na 10 poz.). Gracz na poz. 10 ma więc 9 punktów atrybutów.

**Atrybuty** (`MMOCore/attributes/default_attributes.yml`, widoczne w `gui/attribute-view.yml` l.44/60/78), max 40 pkt:

| Atrybut | Buff za 1 pkt |
|---|---|
| strength (Strength) | weapon_damage +2 (%), max_health +1% |
| dexterity (Dexterity) | physical_damage +1.5%, projectile_damage +1%, attack_speed +0.5% |
| intelligence (Intelligence) | magic_damage +2%, cooldown_reduction +1% |
| bieglosc_* (24 szt.) | ukryte, bez buffów |

`wiara` NIE istnieje. Wymagania atrybutów w przedmiotach: tylko 2 demo — SKILLED_SWORD `required-dexterity: 14`
(`item/thrusting_sword.yml` l.22), MASTER_KATANA `required-dexterity: 20` (`item/katana.yml` l.59).

---

## 3. Przedmioty bojowe NASZE (bez demo, bez TEST_) — poziom mocy na poz. 1

Wszystkie z bloku KLASY-RASOWE: `tier: COMMON`, `required-level: 1`, **bez `model`/grafiki** (materiał vanilla).

### Broń startowa (`tresc_startowa.py` l.40-65, `latacz_broni.py` l.16-63)

| Id | Typ | attack-damage | attack-speed | inne |
|---|---|---|---|---|
| MIECZ_DREWNIANY / KAMIENNY / ZELAZNY / ZLOTY / DIAMENTOWY / NETHERYTOWY | SWORD | 4 / 5 / 6 / 4 / 7 / 8 | 1.6 | odpowiedniki vanilli (zamiennik) |
| SZTYLET_REKRUTA | DAGGER | 3.5 | 2.2 | crit 5 |
| LUK_MYSLIWSKI | BOW | 6 | – | |
| KUSZA_PROSTA | CROSSBOW | 7 | – | |
| LUK_DLUGI_KNIEI | GREATBOW | 8 | – | arrow-velocity 1.3, 2H |
| ROZDZKA_UCZNIA | WAND | 3 | 1.15 | range 20, magic-damage 5 |
| KOSTUR_ZYWEGO_DREWNA | GREATSTAFF | 6 | 0.9 | magic-damage 8, 2H |
| GRIMUAR_UCZNIA | TOME | – | – | magic-damage 5, max-mana 5 |
| TOTEM_PLEMIENNY | TALISMAN | – | – | skill-damage 10, max-mana 5 |
| FILAKTERIUM_KOSCIANE | FILAKTERIUM | – | – | magic-damage 4, max-mana 5 |
| BULAWA_NOWICJUSZA | HAMMER | 5.5 | 1.2 | |
| MLOT_BOJOWY | GREATHAMMER | 8 | 0.8 | 2H |
| SIERP_KSIEZYCOWY | SIERP | 5 | 1.6 | |
| KOSA_SPACZONA | KOSA | 7 | 1.0 | 2H |
| KLINGA_GAJU | KATANA | 5.5 | 1.8 | |
| TOPOR_BOJOWY | AXE | 7 | 0.9 | |
| HALABARDA_STRAZNIKA | HALBERD | 7 | 1.0 | 2H |
| OSZCZEP | SPEAR | 6 | 1.1 | |
| MIECZ_RUNICZNY | GREATSWORD | 8.5 | 0.9 | 2H |
| LANCUCH_GROBOWY | WHIP | 4.5 | 1.7 | |
| TARCZA_DREWNIANA | SHIELD | – | – | armor 2, block-power 30, block-rating 10 |

Uwaga: `magic-damage` w MMOItems to % (lore "Magic Damage: +{value}%"), nie płaskie obrażenia.

### Zbroje z wagą (`tresc_startowa.py` l.26-34) — pancerz hełm/pierś/nogi/buty = suma

| Zestaw | Waga | Pancerz | Suma | Toughness | Extra |
|---|---|---|---|---|---|
| SZATA_LNIANA | tkanina | 1/2/1/1 | 5 | – | max-mana 3 na element (12) |
| ZBROJA_SKORZANA | lekka | 1/3/2/1 | 7 | – | (+HELM_ZOLWI lekka, 2) |
| KOLCZUGA | średnia | 2/5/4/1 | 12 | – | |
| ZBROJA_ZLOCONA | średnia | 2/5/3/1 | 11 | – | |
| ZBROJA_ZELAZNA | ciężka | 2/6/5/2 | 15 | – | |
| ZBROJA_DIAMENTOWA | ciężka | 3/8/6/3 | 20 | 2×4 | |
| ZBROJA_NETHERYTOWA | ciężka | 3/8/6/3 | 20 | 3×4 | KB-res 0.1×4 |

= liczby vanilli (poza tkaniną). Zestaw startowy: najcięższa waga klasy bez ciężkiej (tkanina / skórzana / kolczuga).

### Nasze przedmioty profesji (bojowo istotne)

| Id | Typ | poz. | Statystyki | Tier |
|---|---|---|---|---|
| PROF_LINEN_SHIRT | SHIRT | 1 | max-health 6, armor 1 | COMMON |
| PROF_LEATHER_GLOVES | GLOVES | 10 | attack-damage 2, armor 1 | COMMON |
| PROF_TRAVELER_BELT | BELT | 10 | max-health 10, ruch +0.005 | COMMON |
| PROF_SILVER_SIGNET | RING | 10 | max-mana 15, CDR 3 | COMMON |
| PROF_GEM_EARRING | EARRING | 10 | magic-damage 4, max-mana 10 | COMMON |
| PROF_STEEL_PAULDRONS | PAULDRONS | 15 | armor 3, toughness 1, max-health 8 | COMMON |
| PROF_MOONLEAF_AMULET | NECKLACE | 20 | max-health 20, mana-regen 0.5 | UNCOMMON |
| PROF_LUCK_CHARM | CHARM | 30 | crit 5, max-health 10 | RARE |
| PROF_UNAWAKENED_STAFF | STAFF | – | dmg 1, speed 0.4 (półprodukt) | – |
| PROF_AWAKENED_STAFF | STAFF | 15 | dmg 8, magic-damage 12, range 24, mana-cost 4, ICE_SPIKES | UNCOMMON |
| PROF_HUNTER_KNIFE / MASTER | TOOL | 10 / 35 | dmg 6 / 10, crit 8 / 14 (+crit-power 25) | COMMON / RARE |
| PROF_LUMBER_AXE, FARMER_HOE, HERB_SICKLE (+MASTER) | TOOL | 10 / 35 | dmg 2-8 | |

Akcesoria i narzędzia mają `model: nexo:prof_*`. Nóż myśliwski to TOOL — **omija biegłości** (dmg 6/10 dla każdego).

### Skala demo i TEST_

- Demo MMOItems (angielskie): ~148 przedmiotów bojowych (armor.yml 49, np. DRAGON EPIC poz.12, SPELLCASTER, STEEL;
  broń ~99 w 29 plikach). Latacz dał im biegłości, `tresc_startowa` wagi wg materiału — są "legalne" w grze.
- `item/combat.yml` (COMBAT_SWORD/BOW/STAFF) — typu COMBAT nie ma w `item-types.yml` (martwy plik).
- TEST_: 58 (armor.yml 29 = 20 TEST_<KLASA> + 9 TEST_ZBROJA_*, 7 akcesoriów × 4, TEST_RAPIER). Do usunięcia przed grą.

---

## 4. MMOItems — konfiguracja

| Plik | Stan | Nasze? |
|---|---|---|
| `item-tiers.yml` | TRASH `&8`, COMMON `&7` (parent TRASH), UNCOMMON `&8` (!), RARE `&6`, VERY_RARE `&e`, LEGENDARY `&b`, MYTHICAL `&5`, EPIC `&4`, MAGICAL `&2`, UNIQUE `&c`; angielskie nazwy, deconstruct/generation demo | demo |
| `item-sets.yml` | ARCANE, STEEL, GINGERBREAD, DRAGON, PSYCHIC, UNDEADSLAYER, SPELLCASTER, OMNIELEMENTAL, HATRED | demo |
| `gen-templates.yml` | basic/slime/mountain/nether — generacja żył bloków, nie przedmiotów | demo |
| `modifiers/example_modifiers.yml` | sharp, fiery, lucky, arcane, ancient, heavy… (12) | demo |
| `upgrade-templates.yml` | weapon-default (dmg 3%, crit 2, pve 1%), armor-template-example | demo |
| `custom-stats.yml` | tylko CUSTOM_MYLUCK (demo); linia `#custom-myluck#` w lore-format l.57 | demo |
| `language/lore-format.yml` | kolejność demo + nasze l.20-43 (`#required-bieglosc-*#`) | mieszane |
| `language/stats.yml` | opisy po angielsku (+ nasze `required-bieglosc-*` na końcu) | mieszane |
| `language/lore-formats/`, `tooltips/` | pusty / example_tooltips.yml | demo |
| `drops.yml` | puste (blocks/customblocks/monsters `{}`) | – |
| `config.yml` | `item-level-spread: 2` (l.48), `disable-vanilla-recipes` 35 materiałów (l.304), soulbound demo | mieszane |

Crafting-stations demo, ale PRZEROBIONE na nasze wejścia PROF_*: `arcane-forge.yml` (ARCANE_* zbroje za MAGIC_ESSENCE + PROF_CURED_HIDE),
`mythical-forge.yml` (HELL_BOW, MOLTEN_BLADE, SOULSTEALER… z PROF_ENCHANT_DUST, PROF_BOW_LIMB, PROF_METAL_PLATE),
`steel-crafting-station.yml` (STEEL_SWORD/DAGGER/zbroje, KATANA, claymore; STEEL_INGOT z PROF_REFINED_IRON).
Warunek to `level{}` MMOCore (poz. 4-30), nie profesja.

---

## 5. Moby, poziomy, łup

MythicMobs **nie ustawia poziomów** (`Level`/`Options.Level` puste we wszystkich naszych plikach).

| Plik | Moby | HP | Damage | Łup |
|---|---|---|---|---|
| `mobs/Loch_001.yml` | Pniaczek 4/1, Wilk Gajowy (×2) 20/3, Korzeniak 24/3, Wilczę 8, **Stary Korzeń 80/5 arm 2** (elita/boss), druid/ziele/kamienie (zagadki, HP 1), 6 ARMOR_STAND klimat | 1-80 | 1-5 | tylko PROF_RAW_TIMBER, PROF_BEAST_SCRAP/BONE, PROF_MEDICINAL_HERB, PROF_ROUGH_CRYSTAL (drop inline) |
| `mobs/Bestiariusz.yml` (22) | T1 zając/owca/odyniec/pełzacz/łucznik/pajączek | 12-28 | 1-4 | DT_BEST_T1_* |
| | T2 wilk, czyhacz, nosiciel, niedźwiedź, strażnik krypty, zbój, ognik | 45-90 | 7-12 | DT_BEST_T2_* |
| | Elity: Wilk Alfa, Wiedźma, Czempion, Troll, Zjawa | 160-280 | 12-20 | DT_BEST_T3_* (+exp 40-45) |
| | Bossy: Strażnik Puszczy, Władca Krypty, Matriarchini | 800-1100 | 22-26 | DT_BEST_BOSS_* (+exp 240-280) |
| `mobs/Wieza_p01.yml` | WIEZA_P01_WILK 30/4, ALPHA 140/6 (stary poziom_01, do wymiany) | | | |
| `mobs/VanillaMobs.yml` | CAVE_SPIDER 30/7, WITHER_SKELETON 50/10, GIANT 1000/20 | | | |
| `mobs/Chowance*.yml` | 96+6+… ciał Chowańców (HP 28-260, DMG 0), NPC mistrzów | | | |
| `mobs/WezlyZasobow.yml` | 10 węzłów zbieractwa (HP 1) | | | |

`droptables/Bestiariusz.yml`: wyłącznie PROF_* (SCRAP, BONE, FLEECE, VENOM_SAC, MONSTER_TROPHY), HUMAN_SOUL i
MAGIC_FRAGMENT (demo materiały) + `experience`. **Żaden mob nie dropi sprzętu.**
MythicDungeons: `loottables.yml` = 0 bajtów; `maps/*/functions.yml` bez funkcji nagród; `maps/loch_001/config.yml`
l.130-157 `EnableDifficultyLevels: false`, przykład HARD demo. Mapy: loch_001, poziom_01, gniazdo_zmijow, krypta_upiorow,
sala_mistrzow, wieza_chowancow.

Plan (`docs/lochy/akt1-wyspa-i-lochy.md` §4, `loch-001-kwietna-polana.md` §4-6): 001 poz. 1-5; 002-003 4-9; 004-006 8-14;
007-009 13-19; 010 18-20 (propozycja). Pamięć/D5 z 29.09: **Akt 1 = do 10 poziomu** — tabela lochów w docs jest nieaktualna.
Łup Loch 001 (plan): rodzina "lochowa" z prefiksem `L001_` na istniejących typach, Żeton Gaju (pity), Rdzeń Strażnika,
Kieł Wilka, Spaczony Korzeń (nowe id, nie istnieją). Progi: zwykły mob 4-6 trafień gracza poz. 1-3 w startowym
sprzęcie, gracz przeżywa 5-7 trafień, ciężki atak 25-35% HP. Loch i wyspa: ten sam tier/siła łupu, różne przedmioty.

---

## 6. Profesje i materiały PROF_*

Model docelowy (pamięć): 13 profesji, poziom 100, do 50 wszystko, 50→100 tylko 2 mistrzostwa; gotowanie i budownictwo
ogólne. Dziś sloty mistrzostw WYŁĄCZONE (backup `/opt/docker/mc/backups/sloty-20260923/`).
Profesje MMOCore: alchemy, building, cooking, enchanting, farming, fishing, herbalism, hunting, jewelcrafting, mining,
poskramiacz, smithing, tailoring, woodworking. Receptury stacji `profession-*.yml` na poziomach 1/15/30/45 (nic powyżej 45).

| Stacja | Wyroby bojowe/sprzęt | Półprodukty |
|---|---|---|
| smithing (13) | STEEL_PAULDRONS (15), narzędzia + MASTER (15/45) | STEEL_MECHANISM, METAL_PLATE, ARROWHEADS, **METAL_REPAIR_KIT**, **MASTER_BLADE_BLANK**, AXE_HEAD |
| tailoring (8) | LINEN_SHIRT (1), LEATHER_GLOVES (15), TRAVELER_BELT (15) | CLOTH_BOLT, LEATHER_PANEL, BOWSTRING, **ROBE_LINING**, **FABRIC_REPAIR_KIT** |
| jewelcrafting (9) | SILVER_SIGNET, GEM_EARRING (15), MOONLEAF_AMULET (30), LUCK_CHARM (45) | CUT_GEM, SILVER_SETTING, PEARL_SETTING, **EMPTY_SOCKET**, **MASTER_CHARM** |
| enchanting (6) | AWAKENED_STAFF (30) | RUNE_BLANK, **CHARGED_RUNE**, **ENCHANT_DUST**, **STAFF_FOCUS**, **AWAKENING_SEAL** |
| woodworking (10) | UNAWAKENED_STAFF (30), narzędzia | SEASONED_TIMBER, **WEAPON_HANDLE**, **BOW_LIMB**, **STAFF_CORE**, CARVED_FITTING |
| alchemy (5) | – | ALCHEMICAL_BASE, ANTIDOTE, **WEAPON_OIL**, **COMBAT_TONIC**, ALCHEMICAL_SOLVENT |
| hunting (5) | – | **CURED_HIDE**, **SINEW**, BEAST_BONE, **MONSTER_TROPHY**, VENOM_SAC |
| mining (5) | – | REFINED_IRON, SILVER_ORE, RAW_GEM, CUT_STONE, **ARCANE_CRYSTAL** |
| farming/fishing/herbalism (5 każda) | – | FLAX_FIBER, FINE_WOOL, PEARL, WATER_ESSENCE, HERBAL_EXTRACT… |
| cooking/building (5) | potrawy | MORTAR, CARVED_STONE, SUPPORT_BEAM… |

82 materiały PROF_* w `item/material.yml` (wszystkie z `model: nexo:prof_*`, bez tieru; PROF_WILD_SPICE bez modelu;
8 to karmy Chowańców). Pogrubione = naturalne wejścia do craftingu/jakości nowego sprzętu. **AWAKENING_SEAL,
EMPTY_SOCKET, ROBE_LINING, METAL_REPAIR_KIT, FABRIC_REPAIR_KIT, WEAPON_OIL nie są dziś zużywane przez żadną recepturę**
(martwe końce łańcucha — gotowe haczyki pod jakość/gniazda/naprawę). Brak typu PROF_* dla sztaby/płytki zbroi
lekkiej/średniej poza CURED_HIDE/LEATHER_PANEL/METAL_PLATE.

---

## 7. Ekonomia

| Element | Stan |
|---|---|
| Waluta | CoreTools `economy.yml`: `dollar` (symbol `$`), Vault provider, **per profil MMOProfiles** (`mmoprofiles_vault: profile`), start 50, max 1 000 000. CMI jest, ale saldem rządzi CoreTools |
| Sklep NPC (`Shops/sklep.yml`) | tylko vanilla: kilof kamienny 15, żelazny 60, siekiera 12, wędka 20, pochodnia 0.5, chleb 2. Zero sprzętu |
| Skup (`Shops/skup.yml`, 98 pozycji) | tylko PROF_*: surowce 0.4-1.7 $, półprodukty 5-46 $, akcesoria 8.6-77.5 $, narzędzia 28-178 $, AWAKENED_STAFF 232 $. Ceny z `tools/handel/ceny.json` (model `ekonomia.py`) |
| Dom aukcyjny (`AuctionHouse/rynek.yml`) | opłata `clamp(cena×ilość×(0.01+0.005×dni), 0.1, …)`, podatek 6% (l.860-881); kategoria `sprzet` (l.949-958) przyjmuje typy MMOItems z listy — **brak GREATBOW, SIERP, KOSA, FILAKTERIUM, MAIN_CATALYST, STAVE** |

---

## 8. Grafika

- Przedmioty bojowe (broń, zbroje startowe, demo): **bez `model`** — vanillowe materiały (IRON_HOE, STICK, ECHO_SHARD…).
- Wzorzec podpięcia: stub Nexo w `plugins/Nexo/items/*.yml` (`excludeFromInventory/Commands`, `Pack.texture: nexo:item/<folder>/<nazwa>`)
  + w MMOItems `model: nexo:<id>` (namespace `nexo:`, nie `minecraft:`; id bez słów helmet/chestplate/leggings/boots/elytra —
  Nexo wstrzykuje `equippable`). Opis: nagłówek `plugins/Nexo/items/profesje_ikony.yml`.
- Pack: `plugins/Nexo/pack/assets/{nexo,cc,minecraft,zaklecia}`; tekstury item: profesje 98×16 px, chowance 32×16 + 48×32,
  hub 55×32, mmo_equipment 13×16 + 2×96, cc 11×16, zaklecia 2×16x512 (animacja). Tekstury ≤ 256 px.
- Karta stylu: `docs/karta-stylu-grafiki.md` (obrys, 3-5 tonów, światło lewa-góra, 16×16 natywnie, warsztat `tools/pixelart/`),
  `docs/styl/karta-stylu-grafik.md` l.8-11 (przedmiot 16×16, ikona menu 32×32 do potwierdzenia, zajętość 12-15 px).
- Brak tekstur warstw zbroi (armor layers) — zbroje wyglądają jak vanilla/dye.
- Zaklęcia to przedmioty MythicMobs/Crucible (`MythicMobs/items/Zaklecia.yml`, TRIAL_KEY + ItemModel `zaklecia:*`), nie MMOItems.

---

## 9. MMOInventory

`plugins/MMOInventory/inventory/vanilla_inventory.yml` (aktywny; `default_mmoinventory.yml` `enabled: false`):
9 slotów akcesoriów w rzędzie 9-17 ekwipunku vanilla, każdy z jedną restrykcją `mmoitemstype{type=…}`:

| Slot | Nr | Typ | Linia |
|---|---|---|---|
| Ramiona | 9 | PAULDRONS | l.30-35 |
| Naszyjnik | 10 | NECKLACE | l.40-45 |
| Kolczyki | 11 | EARRING | l.50-55 |
| Ring 1 / Ring 2 | 12 / 13 | RING | l.60-75 |
| Dłonie | 14 | GLOVES | l.80-85 |
| Koszula | 15 | SHIRT | l.90-95 |
| Pas | 16 | BELT | l.100-105 |
| Charm | 17 | CHARM | l.110-115 |

Sloty stałe CoreTools: 8 = UI_DZIENNIK, 18 = UI_SAKWA (`Scripts/stale-sloty.yml` l.44/53). Akcesoria nie mają blokad klas.
Typ ACCESSORY (rodzic) i ORNAMENT/CATALYST nie mają slotu. Nowa postać nie ma ikon 9-17 do relogu (pamięć).

---

## 10. Zestawy startowe i zamiennik

- `CoreTools/Scripts/klasy-rasowe.yml` (507 l., GENEROWANY): `klasa_start_<klasa>` (od l.61) — `mi give <TYP> <ID>` broni
  każdej grupy klasy (+32 strzały dla łuk/łuk długi/kusza) i `klasa_start_zbroja_<zestaw>_<slot>` (SetSlots 39/38/37/36
  w pusty slot, inaczej do plecaka). Raz na postać (zmienna `klasa_start_dany`). Użyte id: ROZDZKA_UCZNIA,
  MIECZ_KAMIENNY, BULAWA_NOWICJUSZA, SZTYLET_REKRUTA, LUK_MYSLIWSKI (po 4×), 12 broni rasowych (1×),
  SZATA_LNIANA_*, ZBROJA_SKORZANA_*, KOLCZUGA_*. TARCZA_DREWNIANA tylko Święty Akolita.
- `CoreTools/item-replacer.yml` (GENEROWANY, 36 reguł): vanilla zbroja (skóra, kolczuga, złoto, żelazo, diament, netheryt,
  turtle) → ARMOR ZBROJA_*; 6 mieczy → MIECZ_*; bow/crossbow/shield/trident/mace → LUK_MYSLIWSKI/KUSZA_PROSTA/
  TARCZA_DREWNIANA/OSZCZEP/MLOT_BOJOWY. Siekiery vanilla zostają.
- Te id są też zaszyte w evalach: `tools/klasy-rasowe/{baza-dziury,bron-bieglosci,bron-wszystkie,eq-sloty,konfiguracja-48,
  kara-smierci,op-postac,ork-wojownik,relog-klasa,specjalizacja-okno}.js`.
- Umowa z planu (pamięć `przebudowa-klas-plan`): id z `START_BRON/START_ZBROJA` są NIETYKALNE, nowe przedmioty = nowe id.

---

## 11. Zasoby i atrybuty z planu przebudowy vs pliki

| Plan | W plikach dziś |
|---|---|
| Wojownik: Szał | mana `name: 'Rage'` (`warrior.yml` l.97-104), max-mana 20 +1/poz., regen −0.5, start 1000 (pełny) |
| Ninja: Skupienie + Znamiona | brak sekcji `mana` w `rogue.yml` → domyślna "Mana" 20 |
| Mag: Mana | `mage/mage.yml` l.101-108 `name: 'Mana'`, 27 +1.2/poz. |
| Akolita: Dusze | brak sekcji `mana` w `paladin.yml` → domyślna 20 |
| Siła / Zręczność / Intelekt | strength / dexterity / intelligence (angielskie nazwy, buffy demo, max 40) |
| Wiara | NIE istnieje (plan: dodaje sesja klas, wymaga restartu) |
| "Szał", "Dusz…" w plikach | tylko w opisach (krwioburca.yml l.7, duszozerca.yml) — nie jako mechanika |
| stamina | zajęta przez broń MMOItems (`stamina-cost` na PROF_AWAKENED_STAFF) |

Wszystkie 4 zasoby to w MMOCore ta sama statystyka `max-mana`/`mana-regeneration` — przedmiot z `max-mana` podnosi Szał
wojownika i "Dusze" akolity tak samo jak manę maga. Dziś `max-mana` mają: SZATA_LNIANA (4×3), GRIMUAR, TOTEM, FILAKTERIUM
(po 5), PROF_SILVER_SIGNET 15, PROF_GEM_EARRING 10; `mana-regeneration`: PROF_MOONLEAF_AMULET 0.5 — pierścień i kolczyk
są wolne dla każdej klasy (sprzeczne z umową "statystyki many tylko na przedmiotach maga").

---

## Ryzyka kolizji i rzeczy do aktualizacji przy wdrożeniu nowych przedmiotów

1. **Przesuwanie bloku KLASY-RASOWE.** `tresc_startowa.wstaw_blok` wycina blok i dokleja go na koniec pliku — nowy blok
   generatora (np. `# >>> PRZEDMIOTY`) po ponownym uruchomieniu tresc_startowa znajdzie się PRZED nim. Nowy generator
   musi mieć własne markery i regex nie-zachłanny między NIMI; nie używać prefiksu `# >>> KLASY-RASOWE`.
2. **Auto-waga zbroi wg materiału.** Każdy przedmiot w `armor.yml` przed markerem KLASY-RASOWE bez `required-bieglosc-*`
   dostaje wagę z materiału — szata maga na `LEATHER_*` dostałaby "lekka". Typ ARMOR = jeden plik `armor.yml`, więc
   nowe zbroje: tkaninę oznaczać innym sposobem (wyjątek w tresc_startowa albo inny materiał), a wagi L/Ś/C pisać jawnie.
3. **Auto-biegłość broni i kasowanie required-class.** `latacz_broni.py` dopisze `required-bieglosc-<g>` każdemu przedmiotowi
   typu z `BRON` i usunie każde `required-class` — nowy generator nie może polegać na `required-class`; broń "dla
   wszystkich" nie może mieć typu z `BRON` (użyć CATALYST/ORNAMENT/…).
4. **Nowe typy MMOItems** wymagają: wpisu w `generuj_klasy.BRON` (inaczej brak biegłości), `latacz_broni.TYPY_NOWE`,
   `lore-format.yml`/`stats.yml` (przez tresc_startowa), `tools/handel/generuj.py` `TYPY_SPRZETU` (dziś brakuje GREATBOW,
   SIERP, KOSA, FILAKTERIUM, MAIN_CATALYST, STAVE), ewentualnie restrykcji MMOInventory; nowy atrybut = restart.
5. **Nietykalne id** START_BRON/START_ZBROJA/MIECZE — używa ich zestaw startowy, zamiennik vanilli i ~10 evali. Zmiana statystyk
   tych id = regeneracja tresc_startowa (nie ręczna edycja).
6. **Pliki nadpisywane w całości** przez generatory: `item-replacer.yml`, `CoreTools/Scripts/klasy-rasowe.yml`, `rynek.yml`,
   `sklep.yml`, `skup.yml`, `bieglosci_broni.yml`, klasy MMOCore. Zmiany nowych przedmiotów w nich wprowadzać przez generatory
   (tresc_startowa / generuj_klasy / handel/generuj.py), nie ręcznie.
7. **lore-format.yml**: tresc_startowa wstawia linie biegłości za `'#required-intelligence#'` — dodając `#required-wiara#`,
   `#item-level#` itp. nie ruszać tej kotwicy. `stats.yml` jest po angielsku — nowe opisy statystyk trzeba spolszczyć.
8. **Zasób = mana dla wszystkich**: `max-mana`/`mana-regeneration` na przedmiocie działa na Szał/Dusze/Skupienie. Obecne
   PROF_SILVER_SIGNET, PROF_GEM_EARRING, PROF_MOONLEAF_AMULET (i szata lniana) wymagają decyzji/przeróbki.
9. **Demo w obiegu**: ~148 demo przedmiotów bojowych ma biegłości i wagi, a stacje arcane/mythical/steel robią je z PROF_*
   (DRAGON EPIC poz.12, MOLTEN_BLADE…). Nowy system powinien je wycofać lub zablokować, inaczej konkurują z nowymi bazami.
   Tiery (nazwy angielskie, UNCOMMON `&8` ciemniejszy niż COMMON), sety, modyfikatory, upgrade-templates, custom-stats — demo
   do wymiany; `#custom-myluck#` w lore-format do usunięcia.
10. **Brak ilvl w łupie**: moby nie mają poziomów, `mmoitems{}` w dropie MM nie ustawia ilvl, MD loottables puste — system
    "poziom przedmiotu" potrzebuje własnego mechanizmu (np. osobne id per próg lub skrypt). `item-level-spread: 2` w config.
11. **Omijanie biegłości**: PROF_HUNTER_KNIFE (TOOL, dmg 6/10, crit 8/14) i inne narzędzia bojowe nie mają wymagań.
12. **Poziomy Aktu 1**: docs/lochy mówi 1-20, decyzja D5 (29.09) — do 10. Wymagane poziomy nowych przedmiotów trzymać w 1-10
    i zaktualizować docs/lochy §4.
13. **Grafika**: każdy nowy przedmiot z modelem = stub Nexo w `plugins/Nexo/items/` + `model: nexo:<id>`, tekstura 16×16,
    id bez słów slotów zbroi; po zmianie `/nexo reload pack` + `/mi reload`.
14. **TEST_** (58) i `item/combat.yml` (typ COMBAT nie istnieje) — posprzątać przed grą; evale TEST_ w `tools/`.
15. **Pliki na żywym serwerze**: MMOItems/MMOCore mogą nadpisać ręczne edycje przy wyłączeniu (pamięć `plugin-data-files-live-server`) —
    wdrażać generatorem + `mi reload`, nowe atrybuty/staty wymagają restartu.
