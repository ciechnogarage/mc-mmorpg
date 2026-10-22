# 02. Klasy, archetypy, drzewka, spelle, mana, skill pointy, krzywa XP

Źródła główne:
- publiczne API v3 (`api.wynncraft.com/v3/classes`, `/ability/tree/{klasa}`, `/ability/map/{klasa}`, `/aspects/{klasa}`), surowe JSON w `tmp/wynn-audit/api/`, liczone skryptem `skrypty/stats_api.py`,
- wiki (Skill Points, Mana, Experience Points),
- kod Wynntils (commit `8191bdf`, 22.09.2026),
- nagrania 2026.

---

## 1. Klasy i archetypy

**[F]** 5 klas. Nazwa w API ma w nawiasie reskin (Dark Wizard, Hunter, Knight, Ninja, Skyseer). Ocena trudności w skali 1–3 pochodzi z `/v3/classes`, a oceny archetypów z `/v3/classes/{klasa}` (`dane/klasy_archetypy.csv`).

| Klasa | Trudność | Broń | Archetypy (trudność / obrażenia / obrona / zasięg / szybkość, skala 1–3) |
|---|---|---|---|
| Archer (Hunter) | 1 | bow | Boltslinger 1/3/1/2/3, Sharpshooter 2/3/1/3/2, Trapper 2/2/2/2/2 |
| Warrior (Knight) | 2 | spear | Fallen 2/3/2/2/2, Battle Monk 1/2/2/2/3, Paladin 2/2/3/2/1 |
| Assassin (Ninja) | 2 | dagger | Shadestepper 2/3/2/1/3, Trickster 2/2/2/3/2, Acrobat 3/2/2/2/3 |
| Mage (Dark Wizard) | 1 | wand | Riftwalker 1/3/1/2/3, Light Bender 2/2/2/3/2, Arcanist 3/3/1/3/2 |
| Shaman (Skyseer) | 3 | relik | Summoner 1/3/2/2/2, Ritualist 2/3/2/1/3, Acolyte 3/3/1/2/3 |

- **[F]** Każdy archetyp w API ma nazwę, opis długi, krótki (np. „Grow from healing”) i ikonę jako **`minecraft:potion` + `customModelData.rangeDispatch:[liczba]`** (np. `abilityTree.lightBender` = 72). To te same modele, co w grze.
- **[W]** Oceny 1–3 w pięciu osiach to gotowy wzór na kartę klasy lub rasy w naszym kreatorze. Wynn pokazuje je na stronie www (runda 1: link „wynn.gg/classes” w opisie klasy).

## 2. Drzewka umiejętności: liczby

**[F]** Z `/v3/ability/tree/{klasa}` i `/v3/ability/map/{klasa}` (`dane/drzewka_umiejetnosci.csv`):

| Klasa | Stron | Węzłów | Łączników (kafelków ścieżki) | Koszt 1 AP / 2 AP | Suma AP całego drzewa | % drzewa za 50 AP | Węzły z wymogiem archetypu | Węzły z wymogiem innego węzła | Węzły blokujące |
|---|---|---|---|---|---|---|---|---|---|
| Archer | 9 | 97 | 173 | 55 / 42 | 139 | 36% | 28 | 60 | 8 |
| Warrior | 9 | 96 | 160 | 53 / 43 | 139 | 36% | 26 | 54 | 12 |
| Assassin | 9 | 96 | 167 | 59 / 37 | 133 | 38% | 27 | 63 | 11 |
| Mage | 9 | 88 | 162 | 47 / 41 | 129 | 39% | 25 | 59 | 14 |
| Shaman | 9 | 96 | 174 | 50 / 46 | 142 | 35% | 32 | 75 | 7 |

- **[F]** Punktów umiejętności (AP) jest **50**: „The total amount of Ability Points also increased from 45 to 50” (Version 2.2.0). Rangi „pożyczają” AP: VIP+ +2, HERO i wyżej +4 (wiki Ranks, „Loaned Ability Points… usable right from the start”).
- **[F]** Wymagania węzła mają postać `{"ABILITY_POINTS":2, "NODE":["arcaneTransfer"], "ARCHETYPE":{"name":"arcane","amount":4}}`. Do tego jest `COMBAT_LEVEL` (ultimate wymaga 90), `links` (sąsiedzi) i `locks` (wzajemne wykluczenia).
- **[F]** Łącznik w mapie to osobny kafelek z ikoną (`connector_up_down` itd.) i listą `endpoints`/`paths`. Wynntils ma 159 kluczy modeli `abilityTree.*` (runda 1).
- **[F]** Na węzeł przypada 6–8 linii opisu. Opis zawiera kombinację, koszt many, „Total Damage X% (of your DPS)” z rozbiciem na żywioły, zasięg i obszar oraz przynależność do archetypu i jego minimum.
- **[W]** Gracz bierze około 1/3 drzewa. To wymusza wybory i kilka buildów na klasę. Budowa drzewka to 9 stron po 6×9 slotów z kafelkami ścieżek, więc przy MMOCore to głównie praca graficzna (runda 4, §5).

### Strony drzewka Mage (przykład układu, `api/tree_mage.json`)
1. Meteor, Wand Proficiency I, Cheaper Meteor, Shooting Star (start: pierwszy spell),
2. pozostałe 3 spelle bazowe (Teleport, Heal, Ice Snake) i Wisdom,
3. „Mastery” 5 żywiołów i pierwsze rozgałęzienia,
4.–8. węzły archetypowe (czerwone „kluczowe” i żółte/białe ulepszenia, np. Judrajim, Arcane Transfer, Mana Bank),
9. **3 ultimate** (po jednym na archetyp).

## 3. Spelle i kombinacje klików

**[F]** Z opisów węzłów (`dane/spelle_kombinacje.csv`, 35 umiejętności z „Click Combo”):

| Klasa | Spell 1 (str. 1) | Spell 2 | Spell 3 | Spell 4 | Ultimate (klawisz F) |
|---|---|---|---|---|---|
| Archer | Arrow Bomb **L-R-R** 45 | Escape L-L-L 20 | Arrow Storm L-R-L 35 | Arrow Shield L-L-R 30 | Angelic Ascension, The Twilight Zone, Extinction Event |
| Warrior | Bash **R-L-R** 40 | Charge R-R-R 25 | Uppercut R-L-L 40 | War Scream R-R-L 30 | Beyond Salvation, Judgement, Zenith Stance |
| Assassin | Spin Attack R-L-R 40 | Dash R-R-R 20 | Multihit R-L-L 40 | Smoke Bomb R-R-L 35 | Pierce the Veil, Serpent's Garden, Another Self |
| Mage | Meteor **R-L-L** 50 | Teleport R-R-R 25 | Heal R-L-R 35 | Ice Snake R-R-L 30 | Dawn, Tangled Origin, Gravitational Collapse |
| Shaman | Totem R-L-R 30 | Haul R-R-R 15 | Aura R-L-L 40 | Uproot R-R-L 30 | Patchwork Abomination, Monument to Gloom, Awakened |

(liczba = bazowy koszt many)

- **[F]** Wynntils ma tylko 4 wzorce kombinacji: R-L-R, R-R-R, R-L-L, R-R-L (`SpellType.SPELL_COMBOS`). U łucznika są odwrócone (L zamiast R), bo łuk strzela prawym przyciskiem.
- **[F]** Pierwszy spell w drzewie (strona 1) ma różny wzorzec: Mage zaczyna od Meteor R-L-L, Archer od Arrow Bomb L-R-R (odwrócone R-L-L), a Warrior, Assassin i Shaman od R-L-R. **[W]** Pierwszy spell to „efektowny” spell klasy, podany graczowi na poziomie 1.
- **[F]** **Ultimate'y** (2.2.0, 04.2026) mają kombinację „F”, czyli klawisz zamiany rąk. Wymagają poziomu 90, 2 AP i minimum archetypu (np. Dawn: Light Bender 17). Ładuje je **Ultimate Meter** („Activate abilities or heal to charge your Ultimate Meter to 100%”). Tooltip: `klatki/V19_…`. Pack ma kafelki `tile/swap.png` z literą F (`klatki/P_m_tooltip_nameplate.png`).
- **[F]** Kombinacje klików są w action barze: glify myszy L/R (`hud/gameplay/.../click_combo.png`). Wynntils czyta z action bara segment wejść spella, segment rzuconego spella i typ ultimate'a (`models/spells/actionbar/*`).
- **[F]** Zasady pozwalają makru wysłać **jedną** 3-klikową kombinację na naciśnięcie: „Macros are only allowed to be used to input a single 3-click spell command at a time” (https://forums.wynncraft.com/threads/game-forum-rules.111874/). Gracze piszą o limicie około 12 kliknięć/s po stronie serwera (Reddit 1sm416j; nie ma na to oficjalnego źródła).
- **[W]** Klawisz F to jedyny „wolny” klawisz, który serwer widzi bez modów. Potwierdza to nasza notatka `klawisz-e-i-tab-nie-do-przechwycenia`, a gracz na Reddicie pisze: „limited by the number of keybinds minecraft has that they can intercept, that's why the Swap Hands key is used for things like Ultimates” (1scwwye). U nas F otwiera tryb castowania MMOCore (`skill-casting.open: SWAP_HANDS`).

## 4. Mana i koszt spelli

- **[F]** Regeneracja wynosi 5 many/s. Sprint do czerwonego paska zużywa manę. W wodzie mana odnawia się wolniej (wiki Mana).
- **[F]** **Powtarzanie spella podnosi jego koszt**: „Consecutively casting spells will result in the cost of mana to increase with each spell casting” (wiki Mana). Wynntils zeruje licznik po `SPELL_COST_RESET_TICKS = 60`, czyli po 3 s bez castów (`SpellModel.java`).
- **[F]** Spelle mogą kosztować zdrowie („You don't have enough health to cast that spell!”). Arcanist ma „Mana Bank” z komunikatem „Your mana bank is empty!” (`SpellFailureReason.java`).
- **[F]** Meta gry to „spell cycling”, czyli naprzemienne rzucanie 2 tanich spelli przy mana regen/steal (wiki Mana, sekcja Tips).
- **[W]** Kara za spam jednego spella zmusza do rotacji. To dobry, tani mechanizm głębi. W MythicLib/MMOCore da się go zrobić przez zmienną (PAPI/skript) zwiększającą koszt.

## 5. Skill pointy (atrybuty)

**[F]** (wiki Skill Points)
- 2 punkty za poziom, maksimum 200 na poziomie 101. Poziomy 102–121 nie dają punktów.
- 5 atrybutów, każdy powiązany z żywiołem:
  - Strength: obrażenia ogółem i żywioł ziemi,
  - Dexterity: szansa na krytyk ×2 i piorun,
  - Intelligence: niższy koszt many, więcej many, woda,
  - Defence: redukcja obrażeń, ogień,
  - Agility: unik (−90% obrażeń), powietrze.
- Ręcznie można wpakować do 100 punktów w atrybut, a z przedmiotami do 150. Zwroty maleją: 150 punktów daje 80,8% efektu, a Defence i Agility mają mnożnik 0,867 i 0,951.
- **Wymagania przedmiotów** (np. „STR 65, DEX 80”) sprawdza się łącznie z bonusami z innych przedmiotów. Bonusy z broni i przedmiotów craftowanych się nie liczą. Tooltip od 2.2 pokazuje 5 ikon atrybutów z ✔/✘ i wartościami (`klatki/V11_…`).
- **[F]** Suma wymagań SP na przedmiotach rośnie z poziomem: mediana 0 do poziomu 19, 30 na 50–59, 52 na 80–84, 90–100 na 100–124, a maksimum 260 (`dane/krzywa_wymagania_sp.csv`).
- **[W]** Build to łamigłówka „zmieść wymagania 3–5 przedmiotów w 200 punktach”. Stąd strony typu wynnbuilder i to „buildcrafting” gracze chwalą najbardziej (`09-…`). MMOItems ma wymagania atrybutów (`required-*` z MMOCore attributes), więc mechanika jest u nas osiągalna.

## 6. Krzywa XP (poziom walki)

**[F]** Tabela z wiki Experience Points (oficjalna, ze źródłem w kodzie Wynntils), sparsowana skryptem do `dane/krzywa_xp_poziomy.csv`:

| Dojście do poziomu | XP skumulowane | % drogi do 120 |
|---|---|---|
| 10 | 7 790 | 0,0007% |
| 20 | 41 590 | 0,004% |
| 30 | 137 950 | 0,01% |
| 50 | 1 222 450 | 0,11% |
| 80 | 26 535 750 | 2,32% |
| 100 | 147 862 000 | 12,9% |
| 105 | 248 721 250 | 21,7% |
| 120 | 1 145 146 860 | 100% |
| 121 (bonusowy) | 2 290 293 720 | 200% |

- **[F]** Koszt kolejnego poziomu rośnie o 10–15% na poziom (kolumna „wzrost”). Od 99 do 119 jest równe ×1,10. **Poziom 121 kosztuje tyle, co 1–120 razem** (1,145 mld). To „bonus level” dla najwierniejszych: „a ‚bonus level' can be achieved…” (wiki).
- **[F]** XP z questów się nie mnoży. Suma za wszystkie questy wynosiła 137,7 mln (stan z 1.20), odkrycia dają 48,0 mln, a jaskinie 36,6 mln. Wszystko to są stałe kwoty (wiki).
- **[F]** Na potworach XP zależy od różnicy poziomów („dynamic leveling system”). Mnożą je: XP Bonus z przedmiotów, Double XP Weekend, bomby, link z Discordem (×2 przez 24 h), party (+20% dla bliskich członków) i tomy.
- **[F]** Ranking combatSoloLevel: w top 100 **wszyscy mają 121**. Czas gry top 100 to od 82 h do 23 977 h, mediana 1350 h. Klasy w top 100: shaman 29, mage 27, warrior 18, archer 9, assassin 8, ukryte 9 (`api/lb_combatSoloLevel.json`).
- **[W]** Do 100 poziomu gra idzie szybko (12,9% całej drogi), a końcówka 100–120 to 87% XP. Wynn wydłuża endgame XP-em, ale główna progresja idzie przez przedmioty i aspekty (`03-…`). W MMOCore krzywą ustawia się plikiem `exp-curves/*.txt` (nasza notatka `mmocore-profesje-klucze`).

## 7. Aspekty (endgame'owe ulepszenia drzewka)

- **[F]** **128 aspektów**: archer 25, warrior 27, assassin 26, mage 25, shaman 25. Rzadkości: legendary 59, fabled 49, mythic 20 (po 4 na klasę) (`dane/aspekty.csv`).
- **[F]** Aspekt się **ulepsza, zbierając duplikaty**. Progi tierów w API: legendary 1/5/30/150 kopii (4 tiery), fabled 1/15/75 (3 tiery), mythic 1/5/15 (3 tiery). Każdy tier wzmacnia jedną umiejętność, np. „Arcane Speed gains +5%/+10%/+15%/…”.
- **[F]** Aspekty wypadają tylko z raidów („Raids serve as the only source of Aspects”, wiki Raids). Zakłada się do 5 w pięciokątnym oknie (runda 2). Wprowadzono je jako odpowiedź na brak endgame'owego celu po uproszczeniu mythików (film LunaCrash, 21.09.2024, https://youtu.be/Ri0qIeESGuc).
- **[W]** To długi, liczalny cel (150 kopii na legendarny maks). Sposób budowy w MMOCore: ulepszenia umiejętności przez `skill-modifiers`/poziomy skilli z warunkiem na przedmiot-token.

## 8. Walka: statusy, śmierć, informacja zwrotna

- **[F]** Wynntils rozpoznaje **26 debuffów** na nameplate'ach mobów: Bleeding, Blindness, Burning, Confused, Contaminated, Corroded, Crystallized, Curse, Discombobulated, Drained, Enkindled, Freezing, Hypoxia, Marked, Poison, Provoked, Resistance, Slowness, Static, Trick, Twilight, Unstable, Weakness, Weathering, Whipped, Wind Prison. Każdy ma własny glif (`combat/label/DebuffType.java`).
- **[F]** Nameplate moba ma nazwę, HP i ikony statusów. Pack zawiera cyfry-glify w wariantach active/inactive (liczby z k/m/b), romby faz bossa i warianty stylu: default, gold, gold_radiant, dark, corrupt (`textures/font/nameplate/*`, `klatki/P_m_tooltip_nameplate.png`). Boss raidu ma pasek z ikonami timerów („Anathema 84.7M, 86%, 2.1k”; `klatki/V17_…`, `V20_…`).
- **[F]** Nad celem wyskakują liczby obrażeń w kolorach żywiołów (`klatki/V06_…`, `V12_…`). W housingu stoi manekin z „Average DPS” (Combat Dummy).
- **[F]** Bossbary pokazują bieżące mechaniki archetypu, np. „10s Healed: 164% [●●●●●●]” u Light Bendera, oraz cel „SLAY! Kill 0/25 mobs to proceed” (`klatki/V12_…`).
- **[F]** Śmierć: ekran „YOU DIED” z czaszką i opisem przyczyny („…was shot to death by Wings Vanguard”, `klatki/V13_…`). Każda śmierć zabiera 2 Soul Points. Odradzasz się w najbliższym odwiedzonym mieście. Przy 0 SP rośnie szansa utraty przedmiotów. SP odnawiają się o świcie (co 20 min) i 1/h offline. Maksimum rośnie od 10 do 15 na poziomach 15/30/45/60/75 (wiki Soul Points). Teleport scroll też zabiera 1 SP.
- **[W]** Kara za śmierć jest łagodna, ale realna, bo w grę wchodzi ryzyko utraty przedmiotów. U nas odpowiednikiem jest `keep-inventory` z karą z MMOCore albo MythicMobs przy śmierci. Do ustalenia z właścicielem.

## 9. Wnioski dla nas [W]

| Co | Jak u nas |
|---|---|
| 4 spelle na klasę, kombinacje 3-klikowe, 5. slot „ultimate” pod F | MMOCore casting mode `KEY_COMBOS` (zamiast obecnego `SKILL_BAR`) z kombinacjami L/R. F jest już zajęte przez `open: SWAP_HANDS`, więc trzeba zdecydować: F = ultimate czy F = tryb castowania. Sprawdzić w docs MMOCore, czy `KEY_COMBOS` wymaga wejścia w tryb |
| Rosnący koszt przy spamie | zmienna w MythicLib/MMOCore skill (`<caster.var.…>`), reset po 3 s |
| 5 atrybutów z wymaganiami na przedmiotach | MMOCore attributes + MMOItems `required-<attribute>` |
| Drzewko ~90 węzłów, 50 AP, archetypy z progami | MMOCore skill-trees (ma wymagania węzłów i „max-points”). Progi archetypu przez warunek na liczbie węzłów danej gałęzi: do sprawdzenia w docs |
| Aspekty z duplikatów | MMOItems (przedmiot-token) + MMOCore/skrypt podnoszący modyfikator skilla |
| Krzywa: szybko do „kapy” i długi ogon | `exp-curves/*.txt`. Wzór Wynna: ×1,10–1,15 na poziom |
| Nameplate z ikonami statusów i fazą | MythicMobs + ModelEngine nameplate / MythicHUD, glify cyfr i ikon w Nexo |
