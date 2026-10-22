# Itemizacja: jak robią to inne gry i co z tego bierzemy

Stan: 2026-09-29. Autor: research (sesja agenta). Zakres: PoE 1/2, Diablo 2/3/4, Wynncraft, Last Epoch, WoW, Lost Ark, Hypixel SkyBlock, a na końcu możliwości MMOItems.
Oznaczenia: **[F]** fakt ze źródła (link przy tezie), **[W]** nasz wniosek lub rekomendacja.

Wymagania właściciela, do których odnosi się synteza: 4 archetypy (Wojownik/Szał, Ninja/Skupienie+Znamiona, Mag/Mana, Akolita/Dusze), sloty 4 zbroi + 9 akcesoriów (ramiona, naszyjnik, kolczyki, 2× pierścień, dłonie, koszula, pas, charm), po 5 przedmiotów na slot na archetyp, 4 rodzaje broni na archetyp, 2 sety na archetyp z progami, quality, rarity i losowe affiksy jak w PoE. Akt 1 kończy się na poziomie 10.

---

## 1. Path of Exile 1 i 2

### 1.1 Rarity i liczba affiksów

| Rarity | Kolor | Affiksy (PoE1) | Affiksy (PoE2) | Źródło |
|---|---|---|---|---|
| Normal | biały | 0 (tylko implicit) | 0 | [poewiki Rarity](https://www.poewiki.net/wiki/Rarity) |
| Magic | niebieski | do 1 prefiksu + 1 sufiksu | 1+1 | jw., [poe2wiki Rarity](https://www.poe2wiki.net/wiki/Rarity) |
| Rare | żółty | do 3+3 (znaleziony ma 4–6) | do 3+3; niezidentyfikowany rare ma 4/5/6 modów w proporcji **8:3:1** | jw. |
| Unique | pomarańczowy | stałe mody (zakresy wartości) | jw. | jw. |

- **[F]** Magic, rare i unique wypadają **niezidentyfikowane**; odkrywa je Scroll of Wisdom ([poewiki Rarity](https://www.poewiki.net/wiki/Rarity)). W PoE2 NPC Hooded One identyfikuje cały ekwipunek za darmo po krótkim queście ([GameSpot](https://www.gamespot.com/articles/path-of-exile-2-how-to-identify-items-for-free/1100-6528299/)).
- **[W]** W PoE2 identyfikacja zeszła do formalności: napięcie przy dropie zostało, koszt zniknął. Płatna identyfikacja to upust waluty, nie zabawa. U nas ma sens tylko jako tani, masowy upust (Wynn, pkt 3).

### 1.2 Mody: prefiks/sufiks, tiery, item level, grupy, wagi

- **[F]** Każdy mod ma **poziom moda** (minimalny item level, przy którym może się wylosować). Wymaganie poziomu przedmiotu rośnie do **80%** poziomu najwyższego moda ([poewiki Modifier, „Mod Level”](https://www.poewiki.net/wiki/Modifier)).
- **[F]** **Grupa/rodzina moda**: na jednym przedmiocie może być tylko jeden mod z grupy. To, razem z item levelem, tworzy „system tierów” ([poewiki Modifier, „Group/family”](https://www.poewiki.net/wiki/Modifier)). **[W]** Dzięki temu nie ma dwóch „+życie” na jednym przedmiocie.
- **[F]** Wartość statystyki losuje się równomiernie w zakresie tieru, np. przy zakresie 1–3 każda wartość ma 1/3 szansy ([poewiki Modifier](https://www.poewiki.net/wiki/Modifier)).
- **[F]** Wyższe tiery są coraz rzadsze: np. „increased Physical Damage” ma wagę 1000 dla T8–T6 i tylko 25 dla T1 ([poewiki Modifier, „Refactored weight model”](https://www.poewiki.net/wiki/Modifier)). W PoE **T1 = najlepszy**.
- **[F]** Item level wynika z poziomu potwora: zwykły potwór daje poziom obszaru, magiczny +1, rzadki/unikatowy +2; skrzynie dają poziom obszaru ([poewiki Item level](https://www.poewiki.net/wiki/Item_level)). Item level to co innego niż wymagany poziom postaci i co innego niż drop level bazy (jw.).
- **[F]** **Implicit** to wrodzony mod bazy (nad kreską w tooltipie), **explicit** to mody wylosowane na przedmiotach magic i wyżej ([poewiki Modifier, „Terminology”](https://www.poewiki.net/wiki/Modifier)).
- **[F]** Mody **lokalne** (np. „+% obrażeń fizycznych” na broni) zmieniają staty bazy przedmiotu; **globalne** działają na postać ([poewiki Modifier, „Local and global”](https://www.poewiki.net/wiki/Modifier)). **[W]** To samo zdanie znaczy co innego na broni i na pierścieniu. U nas tego unikamy, bo to pułapka czytelności.

### 1.3 Bazy i atrybuty

- **[F]** Trzy atrybuty (Str/Dex/Int). Większość ekwipunku ma wymagania atrybutów, bez których nie da się go użyć ([poewiki Attribute](https://www.poewiki.net/wiki/Attribute)). Bazy hybrydowe wymagają dwóch atrybutów i mają dwa typy obrony.
- **[W]** Wymagania atrybutów dają „część wspólną, część wyłączną” bez twardej blokady klasy. Pasuje do przyjętej decyzji D3: Siła/Zręczność/Intelekt + Wiara.

### 1.4 Quality

- **[F]** Quality ma zakres 0–20% (twardy limit dla walut). Na broni daje „more” obrażeń fizycznych broni, na zbroi „more” pancerza, uników lub ES. Działa multiplikatywnie na lokalne staty ([poewiki Quality](https://www.poewiki.net/wiki/Quality)).
- **[F]** Waluty do quality: Blacksmith's Whetstone (broń), Armourer's Scrap (zbroja), Catalysty (biżuteria, zwiększają siłę modów danego tagu), Glassblower's Bauble (flaszki), Gemcutter's Prism (gemy). Quality nie zmienia wymagań ([poewiki Quality](https://www.poewiki.net/wiki/Quality)).
- **[F]** Ile quality daje jedna waluta, zależy od item levelu (tabela na wiki), a na unikacie zawsze +1%. Przedmioty mogą wypaść z quality, ale większość ma 0% ([poewiki Quality](https://www.poewiki.net/wiki/Quality)). W PoE2 im wyższy item level, tym mniej quality na sztukę waluty. Pas i klejnoty nie mają tam quality, a bazy „Exceptional” z ponad 20% są rzadkim dropem od ilvl 78 ([poe2wiki Quality](https://www.poe2wiki.net/wiki/Quality)).
- **[W]** Quality to w PoE **mały, przewidywalny upust** tanich walut, który zawsze pomaga. Nie zmienia buildu, tylko dopieszcza przedmiot, który gracz zatrzyma na dłużej.

### 1.5 Rzemiosło walutą

| Waluta | Działanie | Źródło |
|---|---|---|
| Transmutation | normal → magic (1 affiks) | [poewiki Rarity](https://www.poewiki.net/wiki/Rarity) |
| Augmentation | dodaje brakujący affiks do magic | jw. |
| Alteration (PoE1) | przelosowuje affiksy magic | jw. |
| Alchemy | normal → rare (w PoE2 gwarantuje 4 affiksy, 2+2) | [poe2wiki Rarity](https://www.poe2wiki.net/wiki/Rarity) |
| Regal | magic → rare (+1 affiks) | [poewiki Rarity](https://www.poewiki.net/wiki/Rarity) |
| Exalted | dodaje affiks do rare | jw., [poe2wiki Exalted Orb](https://www.poe2wiki.net/wiki/Exalted_Orb) |
| Chaos | PoE1: przelosowuje cały rare (4–6 modów); **PoE2: usuwa 1 losowy mod i dodaje 1 losowy** | [poewiki Rarity](https://www.poewiki.net/wiki/Rarity), [poe2wiki Chaos Orb](https://www.poe2wiki.net/wiki/Chaos_Orb) |
| Crafting bench (PoE1) | przewidywalne rzemiosło za waluty, np. 3 lub 8 Chaos za przelosowanie 3 modów lub 1 moda | [poewiki Crafting](https://www.poewiki.net/wiki/Crafting) |

- **[F]** W PoE nie ma złota. Waluty są jednocześnie pieniądzem i materiałem do rzemiosła, więc same działają jako upust, bo się zużywają ([Massively OP, Ch. Wilson](https://massivelyop.com/2026/01/07/path-of-exile-co-creator-chris-wilson-discusses-his-mistakes-on-economic-integrity-in-online-rpgs/)).
- **[W]** PoE2 odeszło od „przelosuj wszystko” (Chaos w PoE1) na rzecz „zmień jeden mod” (Chaos w PoE2). Mniejsze, czytelniejsze kroki lepiej pasują do małego serwera.

### 1.6 Corruption i sockety

- **[F]** Vaal Orb korumpuje przedmiot z losowym wynikiem (może dodać implicit, coś zmienić albo zepsuć). Potem przedmiotu **prawie nie da się już modyfikować** ([poewiki Corrupted](https://www.poewiki.net/wiki/Corrupted)). **[W]** To „ostatni hazard” na endgame. Dla Aktu 1 za wcześnie.
- **[F]** W PoE1 quality ułatwia kolorowanie i łączenie socketów: +1% za każdy 1% quality ([poewiki Quality](https://www.poewiki.net/wiki/Quality)).
- **[W]** Sockety w PoE1 to osobny, ciężki system z gemami skilli w przedmiotach. U nas skille siedzą w MMOCore, więc sockety wchodzą w grę tylko jako **gniazda na kamienie ze statami** (MMOItems gem stones, pkt 7), i nie w Akcie 1.

### 1.7 Co z PoE ma sens na małym serwerze [W]

| Bierzemy | Nie bierzemy (na razie) |
|---|---|
| 4 rarity, prefiks/sufiks, limit affiksów per rarity | 3+3 affiksy (za dużo tekstu w tooltipie MC, pkt 6) |
| rodziny modów (zero duplikatów) | pełną drabinę walut (ponad 10 orbów) |
| tiery bramkowane poziomem przedmiotu | korupcję, influence, fossile, essences |
| quality 0–20% z tanich materiałów | sockety z linkami |
| 3–4 proste waluty rzemiosła (pkt 8) | handel bez złota (u nas jest waluta serwera) |
| wymagania atrybutów zamiast blokady klasy | |

---

## 2. Diablo 2 / 3 / 4

### 2.1 Sety

- **[F] D2:** sety mają zielone nazwy, stałe właściwości i od 2 do 6 części. **Częściowe bonusy** rosną od 2 założonych części, pełny bonus daje komplet ([GamerGuides D2R Set Items](https://www.gamerguides.com/diablo-ii-resurrected/guide/equipment/set-items)). Tal Rasha (5 części): 2 cz. regeneracja życia, 3 cz. +65% MF, 4 cz. 25% FHR, komplet +3 do skilli sorki, +150 życia i +50 odporności ([Arreat Summit](https://classic.battle.net/diablo2exp/items/sets/sets7.shtml)). W praktyce często lepiej nosić 3 części i uzupełnić je unikami (GamerGuides, jw.).
- **[F] D3:** bonusy za 6 części stały się obowiązkowe i zamykały gracza w jednym stylu gry, a Blizzard dalej je buffował ([Inven Global](https://www.invenglobal.com/articles/7534/diablo-iii-legacy-of-nightmares-what-to-go-for-each-class), [forum D4 „Do sets remove build diversity”](https://us.forums.blizzard.com/en/d4/t/do-sets-remove-build-diversity-or-promote-it/251193)).
- **[F] D3, inflacja:** set Akkhana (AoV Crusader) z sezonu 19 dawał **+15 000%** obrażeń do skilla i był nerfiony pięć razy ([Maxroll AoV](https://maxroll.gg/d3/guides/aov-heavens-fury-crusader-guide)). Greater Rift podnosi HP potworów o 17% (mnożnikowo) na każdy poziom ([Maxroll GR](https://maxroll.gg/d3/resources/greater-rift-explained)). **[W]** Mnożnikowe bonusy setów kończą się wyścigiem liczb.
- **[W]** Wniosek: set ma **umożliwiać build**, czyli zmieniać działanie mechaniki archetypu, ale nie może być **jedyną** drogą do grania archetypem. Bonus za pełny set nie może mnożyć obrażeń ×N.

### 2.2 D3 Loot 2.0 (smart loot)

- **[F]** Po Loot 2.0 przedmioty wypadają rzadziej, ale są lepsze. Większość dropu ma statystyki pod **aktualną klasę**, a **15%** jest poza tą regułą ([Diablo Wiki Smart Loot](https://www.diablowiki.net/Smart_Loot), [Digital Trends](https://www.digitaltrends.com/gaming/diablo-iii-reaper-souls-guide-loot-2-0-changes/)).
- **[W]** Przy 4 archetypach i ~260 bazach losowy drop w 3 na 4 przypadki trafi w „nie moją klasę”. Potrzebujemy smart lootu (np. 75% dropu pod archetyp postaci, 25% dowolne na handel). Patrz lista braków.

### 2.3 D4: aspekty, ancestral, Greater Affixes, tempering, masterworking

| Mechanika | Działanie | Źródło |
|---|---|---|
| Legendary Aspect | legendarna moc, która zmienia skill lub mechanikę. Rozbicie legendy zapisuje aspekt w **Codex of Power**, a zapisany aspekt można nałożyć na inny przedmiot u Occultista | [Maxroll Aspects](https://maxroll.gg/d4/wiki/legendary-aspects), [Blizzard S4](https://news.blizzard.com/en-us/article/24077223/galvanize-your-legend-in-season-4-loot-reborn) |
| Ancestral | najwyższa klasa dropu (endgame); tylko na niej mogą wypaść Greater Affixes | [Blizzard S4](https://news.blizzard.com/en-us/article/24077223/galvanize-your-legend-in-season-4-loot-reborn) |
| Greater Affix | affiks **1,5×** silniejszy, **tylko z dropu** (nie da się go wykraftować), ma osobny dźwięk i ikonę | jw. |
| Tempering | dodaje affiks z „podręcznika” (6 kategorii); przedmiot ma ograniczoną „trwałość”, czyli liczbę prób | jw. |
| Masterworking | każdy rank daje +5% do wszystkich affiksów, a na rankach 4, 8 i 12 jeden affiks dostaje +25% | [Fextralife S4](https://diablo4.wiki.fextralife.com/Loot+Reborn) |

- **[F] Czego gracze nienawidzili w D4 na starcie („affix soup”):** mnóstwa **warunkowych** affiksów („obrażenia przeciw bliskim/dalekim/zdrowym/rannym…”), które wpadały do jednego worka obrażeń ([forum D4, conditional affixes](https://us.forums.blizzard.com/en/d4/t/conditional-affixes-are-not-fun-unappealing-feel-unrewarding-and-dont-work-as-advertised/63114)). S4 ograniczył legendy do **3** affiksów, rare do **2** i usunął warunkowe, bo „celem jest, by łatwiej było rozpoznać, który przedmiot jest ulepszeniem” ([Blizzard S4](https://news.blizzard.com/en-us/article/24077223/galvanize-your-legend-in-season-4-loot-reborn)).
- **[W]** Lekcje dla nas: (1) mało affiksów, każdy czytelny i bezwarunkowy; (2) moc buildu w **jednym** wyraźnym miejscu (aspekt, set, unikat), a nie rozmyta w sześciu drobnych procentach; (3) jeden „jackpot” z dropu (jak Greater Affix), którego nie da się kupić w kuźni. To utrzymuje wartość dropu mimo rzemiosła.

---

## 3. Wynncraft (najbliższy nam: MMORPG w Minecrafcie)

### 3.1 Rarity

| Rarity | Kolor (hex, kod MC) | Częstość | Uwagi | Źródło |
|---|---|---|---|---|
| Normal | biały #FFFFFF (§f) | bardzo częsty | nigdy niezidentyfikowany | [Wynn wiki Item Rarity](https://wynncraft.wiki.gg/wiki/Item_Rarity) |
| Unique | żółty #FFFF55 (§e) | bardzo częsty | | jw. |
| Rare | różowy #FF55FF (§d) | częsty | | jw. |
| Legendary | aqua #55FFFF (§b) | niezbyt częsty | | jw. |
| Fabled | czerwony #FF5555 (§c) | rzadki | często ma Major ID | jw. |
| Mythic | ciemnofioletowy #AA00AA (§5) | bardzo rzadki | | jw. |
| ~~Set~~ | zielony | — | **usunięta w 2.2.0**; sety zostały, ale ich części mają zwykłe rarity | jw. |

- **[F]** Dlaczego usunęli rarity „Set”: sety miały bardzo różną moc przy tej samej rarity, gracze używali tylko kilku z nich, a drop dawał dziwne duplikaty. Jako **cecha** (a nie rarity) set może być unique, rare albo legendary, zależnie od mocy ([forum Wynn, 93% za](https://forums.wynncraft.com/threads/remove-the-set-rarity-93-approval.295354/)). **[W]** Ważna lekcja: zestaw nie powinien mieć własnego koloru rarity.

### 3.2 Identyfikacje (losowe staty)

- **[F]** Każde ID przedmiotu ma **wartość bazową**. Przy identyfikacji dostaje los: ID pozytywne **30–130%** bazy, negatywne **70–130%**; każdy całkowity procent jest równie prawdopodobny ([forum Wynn, obliczanie ID](https://forums.wynncraft.com/threads/how-identifications-are-calculated.128923/)). Gwiazdki: brak = 30–100%, ★ = 101–124%, ★★ = 125–129%, ★★★ = 130% (jw.).
- **[F]** **Wynn nie ma losowych affiksów.** Zbiór ID jest stały dla każdego nazwanego przedmiotu, losuje się tylko wartość. Identyfikuje NPC Identifier za emeraldy, a **ponowna identyfikacja kosztuje 5×** ([Wynn wiki Identifier](https://wynncraft.wiki.gg/wiki/Identifier)).
- **[F]** Od 2.2 tooltip bez modów klienta pokazuje, jak dobrze wylosowało się ID (paski/kółka). Zebrane w [naszym audycie 03](../wynncraft/audyt/03-przedmioty-crafting-ekonomia.md).
- **[F]** **Major ID** zmienia mechanikę, np. „Freerunner”: gdy pasek sprintu spadnie poniżej 30%, sprint dostaje +150%. Major ID są na fabled i mythic, a niektóre są przypisane do klasy i archetypu (np. Assassin/Shadestepper) ([Wynn wiki Identifications, „Major Identifications”](https://wynncraft.wiki.gg/wiki/Identifications)). **[W]** To odpowiednik aspektów z D4: build-enabler zamknięty w jednym przedmiocie.

### 3.3 Sety

- **[F]** Leaf Set to najwcześniejszy set, 4 części zrzucane przez moby w Nivla Woods: 2 cz. Thorns +8%, HP +10; 3 cz. +13%, +25; 4 cz. +25%, +65, +25% regeneracji ([Wynn wiki Set Items](https://wynncraft.wiki.gg/wiki/Set_Items)). **[W]** Świetny wzorzec „seta na leveling” w Akcie 1: prosty, z dropu mobów, progi 2/3/4.
- **[F]** Morph (8 części): XP i Loot Bonus od +5% (2 cz.) do +100% (8 cz.), a +15 do wszystkich atrybutów tylko za komplet ([Wynn wiki Set Items](https://wynncraft.wiki.gg/wiki/Set_Items)). Kaerynn ma 2 części i jeden próg (jw.).
- **[F]** Są też sety z Major ID w bonusie, np. Thunder Relic Set daje „Crackling Smite” ([Wynn wiki Identifications](https://wynncraft.wiki.gg/wiki/Identifications)).

### 3.4 Powdery, wymagania

- **[F]** Powdery: 5 żywiołów × 7 tierów. Nakłada się je na wolne sloty broni i zbroi, a pierwszy powder wyznacza „powder special” broni. Każdy tier powdera wymaga minimalnego poziomu przedmiotu ([Wynn wiki Powders](https://wynncraft.wiki.gg/wiki/Powders)). Najwięcej przedmiotów ma 2–3 sloty ([audyt 03](../wynncraft/audyt/03-przedmioty-crafting-ekonomia.md)).
- **[F]** Punkty umiejętności: 2 na poziom, ręcznie maks. 100 w jedną statystykę, do 150 z przedmiotami. Przedmioty mają **wymagania punktów** ([Wynn wiki Skill Points](https://wynncraft.wiki.gg/wiki/Skill_Points)).

### 3.5 Prezentacja w tooltipie (Minecraft)

- **[F]** Nazwa ma kolor rarity, a niezidentyfikowany przedmiot ma osobną ikonę „pudełka” w kolorze rarity ([Wynn wiki Item Rarity](https://wynncraft.wiki.gg/wiki/Item_Rarity)). Wymagania w tooltipach Wynna mają znacznik ✔/✘ ([nasza analiza wizualna](../wynncraft/3-analiza-wizualna.md)), a jakość rolla widać na pasku (audyt 03). Rarity steruje też shaderem glintu, więc przedmiot świeci w kolorze rzadkości ([analiza wizualna](../wynncraft/3-analiza-wizualna.md)).

---

## 4. Last Epoch (wzorzec czytelności)

- **[F]** Przedmiot ma maks. **4 affiksy** (prefiksy+sufiksy) i piąty, „zapieczętowany” slot. Tiery **T1–T7**, **wyższy = lepszy**. Rzemiosłem da się dojść do **T5**. **Exalted** to przedmiot z co najmniej jednym affiksem **T6–T7**, dostępnym tylko z dropu ([Maxroll LE Crafting Basics](https://maxroll.gg/last-epoch/resources/beginner-crafting-guide)).
- **[F]** **Forging Potential**: każdy przedmiot ma pulę „potencjału”, a każde rzemiosło zużywa jej losową część (np. podniesienie T3 kosztuje 1–18). Wyższa rarity = więcej potencjału. Glyph of Hope daje 25% szansy, że próba nic nie zużyje. Krytyczny sukces podnosi wszystkie affiksy o tier i jest darmowy (jw.). **[W]** To elegancki, **ograniczony** reroll: gracz widzi, ile prób mu zostało, a przedmiotu nie da się kraftować w nieskończoność.
- **[F]** **Legendary Potential (LP 0–4)** na unikatach. W Temporal Sanctum łączy się unikat z exalted tej samej bazy, a unikat przejmuje tyle affiksów, ile ma LP ([LE Support Legendary Items](https://support.lastepoch.com/hc/en-us/articles/46361924310555-Legendary-Items), [Maxroll Legendary](https://maxroll.gg/last-epoch/resources/legendary-items-crafting-guide)).
- **[F]** Sety mają **2–3** przedmioty. Uchodziły za słabe, bo zajmują sloty i nie dają elastyczności statów. Rozwiązanie: rozbicie części setu na „odłamek” i przeniesienie bonusu setu na własny rare lub exalted tej samej bazy („Reforged”) ([Maxroll LE Set Items](https://maxroll.gg/last-epoch/resources/set-items-crafting)).
- **[F]** Wbudowany filtr łupu umie filtrować po **sumie tierów** wybranych affiksów ([Maxroll LE Loot Filter](https://maxroll.gg/last-epoch/resources/loot-filter-guide), [forum LE](https://forum.lastepoch.com/t/clarification-regarding-the-affix-tier-options-in-loot-filter/62450)).
- **[W]** Z LE bierzemy: **numer tieru widoczny w tooltipie**, zasadę „wyższy = lepszy”, maks. 4 affiksy, limit rzemiosła (potencjał) i małe sety po 2–4 części.

---

## 5. WoW i Lost Ark (krótko)

- **[F] WoW, tier set:** bonus przy **2** i **4** częściach. Tierem może być tylko 5 slotów: głowa, ramiona, klata, rękawice, nogi. Catalyst zamienia zwykły przedmiot z takiego slotu na część setu z tym samym item levelem ([Wowhead Revival Catalyst](https://www.wowhead.com/guide/revival-catalyst-dragonflight)). **[W]** Catalyst to **bad luck protection dla setów**: brakującą część robi się z innego dropu.
- **[F] WoW, upgrade tracks:** każdy przedmiot należy do ścieżki (Explorer, Adventurer, Veteran, Champion, Hero, Myth) z własnym zakresem ilvl. Ulepsza się go walutą i „crestami” z treści danej trudności ([Wowhead upgrade system](https://www.wowhead.com/guide/item-upgrade-system-dragonflight)). **[W]** Ulepszanie daje przewidywalny postęp obok losowego dropu.
- **[F] Lost Ark:** item level (honing) **bramkuje treści**, np. ilvl 250 otwiera Chaos Dungeons i Guardian Raids ([Maxroll LA](https://maxroll.gg/lost-ark/resources/gear-progression-guide)). Pity „Artisan's Energy”: każda nieudana próba honingu dodaje ~46,5% szansy bazowej, a przy 100% następna próba na pewno się uda ([Lost Ark Wiki Honing](https://lostark.fandom.com/wiki/Honing)). **[W]** Gear score jako bramka treści jest czytelny, ale zamienia grę w honing. Za to przy każdym ulepszaniu z ryzykiem pity jest obowiązkowe.

---

## 6. Tooltip i czytelność w Minecrafcie

- **[F] Hypixel SkyBlock, rarity** (od najniższej): Common (biały), Uncommon (zielony), Rare (niebieski), Epic (fioletowy), Legendary (złoty), Mythic (różowy), Divine (aqua), do tego Special/Very Special (czerwony) ([SkyBlock Wiki Rarity](https://hypixelskyblock.minecraft.wiki/w/Rarity)). Rarity wpływa na siłę i koszt reforge'a (jw.).
- **[F] Reforge:** jeden naraz, dodaje staty zależne od rarity i jest widoczny jako **przedrostek nazwy**. Kowal losuje reforge za monety (gracz widzi wynik dopiero po zapłacie), a kamienie reforge dają konkretny reforge ([SkyBlock Wiki Reforging](https://hypixelskyblock.minecraft.wiki/w/Reforging)). Gdy nazwa przedmiotu już zawiera słowo reforge'a, gra używa innego przedrostka, żeby nie było „Wise Wise Dragon Armor” (jw.).
- **[F] Recombobulator 3000:** jednorazowo podnosi rarity przedmiotu o jeden stopień, a reforge skaluje się z nową rarity ([SkyBlock Wiki Recombobulator](https://hypixelskyblock.minecraft.wiki/w/Recombobulator_3000)).
- **[F] Gwiazdki ✪:** do 5 złotych ✪ (za Essence), każda daje +10% statów w lochach. Potem 5 czerwonych Master Stars po +5% w Master Mode ([SkyBlock Wiki Star Upgrades](https://hypixelskyblock.minecraft.wiki/w/Star_Upgrades)). **[W]** ✪ w nazwie to najczytelniejszy znany w MC wzorzec quality lub ulepszeń: gracz widzi poziom od razu, bez czytania lore.
- **[W] Zasady dla naszego tooltipu** (wnioski z D4 S4 i Wynna):
  1. Nazwa w kolorze rarity, ulepszenie jako ✪ lub „+N” w nazwie.
  2. Stała kolejność: typ i rarity → staty bazy (z quality) → **kreska** → affiksy (najpierw prefiksy, potem sufiksy) z tierem `[T3]` na szaro → bonus setu (lista progów: aktywne zielone, nieaktywne szare) → wymagania z ✔/✘ → opis fabularny kursywą.
  3. Maks. ~12–14 linii. Każdy affiks w jednej linii i bez warunków.

---

## 7. Co umie MMOItems (nasza platforma)

| Funkcja | Stan w MMOItems | Uwaga dla projektu | Źródło |
|---|---|---|---|
| Tiery (rarity) | TRASH…UNIQUE z własnymi nazwami i kolorami, glow i hologram na ziemi, tryb niezidentyfikowany, deconstruct | pokrywa rarity i podświetlenie łupu | [docs Tiers](https://docs.phoenixdevt.fr/mmoitems/features/tiers.html) |
| Pojemność modów | `capacity = base + item_level × scale` (± spread); modyfikator ma **wagę**, która zużywa pojemność | **nie** ma limitu „1 prefiks + 1 sufiks”, trzeba go zbudować z grup | jw., [docs Modifiers](https://docs.phoenixdevt.fr/mmoitems/items/modifiers.html) |
| Modyfikatory | `chance`, `weight`, `prefix`/`suffix` z `priority` (widać tylko ten o najwyższym priorytecie), `stats`; grupy z `min`/`max` | rodzina modów = grupa z `max: 1`. **Nie ma minimalnego ilvl dla moda** (sprawdzone w jarze 29.09, notatka o przebudowie klas) | [docs Modifiers](https://docs.phoenixdevt.fr/mmoitems/items/modifiers.html), `plugins/MMOItems/modifiers/example_modifiers.yml` |
| Item level | staty `base + scale × level`; `level-item` losuje poziom; tier i poziom losują się niezależnie | `mmoitems{}` w dropie MM **nie ustawia ilvl** (notatka 29.09), więc tiery affiksów bramkujemy **osobnymi szablonami lub pulami na każde pasmo poziomu** | [docs Templates](https://docs.phoenixdevt.fr/mmoitems/items/templates.html) |
| Sety | `item-sets.yml`: progi `'2'`, `'3'`, `'4'`… ze statami, efektami, **abilities**, permisjami i cząsteczkami; `lore-tag` | build-enabler przez ability lub permisję + skrypt MythicLib | [docs Sets](https://docs.phoenixdevt.fr/mmoitems/features/sets.html) |
| Upgrading | szablony `upgrade-templates.yml` (% lub wartość), maks. liczba ulepszeń, szansa, zniszczenie przy porażce, kamień ulepszenia, sufiks nazwy „(+N)” | nośnik **quality** (patrz synteza) | [docs Upgrading](https://docs.phoenixdevt.fr/mmoitems/features/upgrading.html) |
| Soulbound | nakładany konsumpcją, ma poziomy, zostaje po śmierci, nie da się go włożyć do skrzyni ani przekazać | ważne przy karze za śmierć i handlu | [docs Soulbound](https://docs.phoenixdevt.fr/mmoitems/features/soulbound.html) |
| Gem stones | gniazda w kolorach, kamień pasuje tylko do gniazda w swoim kolorze, wyjmowanie konsumpcją | „sockety” na później | [docs Gem stones](https://docs.phoenixdevt.fr/mmoitems/features/gem-stones.html) |
| Identyfikacja | niezidentyfikowany przedmiot pokazuje typ, tier i zakres poziomu (±3); identyfikuje konsumpcja | tani zwój = upust | [docs Identification](https://docs.phoenixdevt.fr/mmoitems/features/identification.html) |

---

## 8. Synteza: rekomendowana struktura dla nas [W]

### 8.1 Rarity (Akt 1)

| Rarity | Kolor MC | Affiksy | Udział w dropie ekwipunku (propozycja startowa) | Źródło w Akcie 1 |
|---|---|---|---|---|
| Zwykły | §f biały | 0 (tylko implicit bazy) | 65% | moby, sklepy |
| Magiczny | §9 niebieski | 1–2 (maks. 1P + 1S) | 28% | moby |
| Rzadki | §e żółty | 3–4 (maks. 2P + 2S) | 6,5% | elity, skrzynie lochów |
| Unikatowy | §6 złoty | stałe (zakresy wartości) + 1 efekt mechaniki | 0,5% (głównie z bossów, stała lista) | bossy lochów |
| Legendarny | §5 ciemnofioletowy | zarezerwowany na Akt 2+ (unikat + affiksy, jak Legendary w LE) | 0% w Akcie 1 | — |

- Set **nie jest rarity** (lekcja Wynna 2.2). Część setu ma zwykłą rarity (Rzadki albo Unikatowy), stałe staty i **zieloną linię** „Zestaw: …” z progami.
- Kolory zgodne z PoE i Diablo (biały/niebieski/żółty/złoty), bo gracze ARPG już tę konwencję znają. Zielony dla setów, jak w D2 ([GamerGuides](https://www.gamerguides.com/diablo-ii-resurrected/guide/equipment/set-items)).
- Udziały są do strojenia. Liczyć je przez **łup na minutę** (pkt 9), nie per mob.

### 8.2 Affiksy: prefiksy vs sufiksy

- Maks. 2P + 2S, tak jak w LE (4 affiksy) i D4 S4 (2–3). Tooltip w MC jest wąski, 6 affiksów to za dużo.
- **Prefiksy = ofensywa i zasób archetypu**, **sufiksy = obrona i użyteczność** (konwencja PoE: życie i obrażenia to zwykle prefiksy, odporności i atrybuty to sufiksy). Gracz wie wtedy, czego szukać.
- Pula na slot: ~8–12 affiksów na archetyp, **zero warunkowych** (lekcja D4).
- **Pułapka polszczyzny:** przymiotnik w prefiksie musi się zgadzać z rodzajem bazy („Okrutny topór / Okrutna szabla / Okrutne ostrze”), a MMOItems ma **stały tekst** prefiksu. Możliwe rozwiązania:
  - (a) prefiksy jako rzeczowniki w dopełniaczu, jak sufiksy („Topór Okrucieństwa”);
  - (b) trzy warianty prefiksu (m/ż/n), które generator (tools/przedmioty) dobiera po rodzaju bazy;
  - (c) rzadkie przedmioty dostają losową dwuwyrazową nazwę jak w PoE, a affiksy są tylko w lore.

  Rekomendacja: magiczne w formacie „[Prefiks w rodzaju bazy] Baza [Sufiks]”, generowane offline w 3 rodzajach; rzadkie z losową nazwą.
- Przykłady dla magicznych (`priority` pokaże 1 prefiks i 1 sufiks):

| Typ | Prefiks (m/ż/n) | Efekt | Sufiks (bez odmiany) | Efekt |
|---|---|---|---|---|
| ofensywa | Okrutny / Okrutna / Okrutne | +% obrażeń fizycznych | …Niedźwiedzia | +maks. HP |
| zasób Wojownika | Wściekły / Wściekła / Wściekłe | +Szał za trafienie | …Żółwia | +pancerz |
| zasób Ninji | Cichy / Cicha / Ciche | +czas Znamion | …Lisa | +zręczność |
| zasób Maga | Tajemny / Tajemna / Tajemne | +regeneracja many | …Sowy | +intelekt |
| zasób Akolity | Grobowy / Grobowa / Grobowe | +maks. Dusz / HP sług | …Pielgrzyma | +wiara |
| ogólne | Ostry / Ostra / Ostre | +obrażenia płaskie | …Wiatru | +prędkość ruchu |

### 8.3 Tiery affiksów bramkowane poziomem

- Konwencja **LE: wyższy tier = lepszy**, numer widoczny w tooltipie jako `[T2]`. Nie trzeba wtedy tłumaczyć nowym graczom, że „T1” to najlepszy, jak w PoE.
- Drabina do poziomu 100: nowy tier co ~10 poziomów (T1 od ilvl 1, T2 od 8, T3 od 16, …, T10 od 90). **Akt 1 = T1–T2**, a T3 tylko z bossa kończącego akt (nasz odpowiednik „Greater Affix”: wyłącznie z dropu, jak w D4 i jak exalted w LE).
- Wymagany poziom przedmiotu = maks(poziom bazy, 80% poziomu najwyższego moda) (wzór PoE, [poewiki Modifier](https://www.poewiki.net/wiki/Modifier)).
- Realizacja w MMOItems. Mod nie ma minimalnego ilvl, więc robimy **osobne pule modyfikatorów na każde pasmo** (`AKT1_T1`, `AKT1_T2`) i osobne szablony lub tabele dropu na strefę albo loch. Rodzina = grupa z `max: 1`. Prefiksy i sufiksy to dwie grupy nadrzędne z `max` zależnym od rarity.
- **Do zweryfikowania sondą:** czy grupy mogą się zagnieżdżać i czy `min`/`max` współgra z pojemnością tieru. Jeśli nie, ograniczenia pilnuje generator offline, a MMOItems dostaje gotowe kombinacje.

### 8.4 Quality

- **Zakres 0–20%**. Domyślnie przedmiot wypada z 0%, część z 1–10% (jak w PoE).
- **Co podnosi:** tylko **staty bazy** (lokalne), mnożnikowo. Broń: obrażenia bazy. Zbroja: pancerz lub HP bazy. Akcesoria: +1% siły affiksów za każdy 1% quality (jak Catalysty w PoE). Wymagań nie zmienia.
- **Jak zdobywać:** z materiałów **profesji**, zgodnie z zasadą „własne treści, nie vanilla”, czyli gracz zdobywa nasze PROF_*:
  - Osełka (kowalstwo): broń;
  - Łata płatnerska (płatnerstwo lub krawiectwo): zbroja;
  - Pył jubilerski (jubilerstwo): akcesoria.

  Jedna sztuka daje +5% na zwykłym, +2% na magicznym i +1% na rzadkim lub unikacie. Idea z PoE: lepsze przedmioty kosztują więcej.
- **Nośnik:** MMOItems Upgrading. Szablon `JAKOSC` daje +1% na jedno ulepszenie, maks. 20 ulepszeń, 100% szansy, bez niszczenia. W nazwie pokazujemy „Jakość +12%” albo ✪ co 5% (styl SkyBlocka). **Do sprawdzenia:** czy jeden przedmiot może mieć dwa niezależne tory ulepszeń (quality i ewentualne „+N”). Jeśli nie, quality jest jedynym torem w Akcie 1.
- Quality **nigdy** z ryzykiem zniszczenia. To ma być bezpieczny upust.

### 8.5 Sety (2 na archetyp)

- **Rozmiar:** 4 części, progi **2 i 4** (wzór WoW i Wynn Leaf), ewentualnie próg 3 w secie biżuterii. **Nie 6** (lekcja D3).
- **Set A „wędrowny”** (Akt 1, poziom 5–10, drop z mobów i lochów, jak Wynn Leaf): 4 sloty zbroi, łagodne bonusy (2 cz. stat, 4 cz. mały enabler).
- **Set B „mistrzowski”** (koniec Aktu 1 albo Akt 2): 4 sloty akcesoriów (naszyjnik, 2 pierścienie, charm) albo mieszany; 4 cz. to wyraźny enabler. Dzięki temu oba sety **da się nosić jednocześnie** (zbroja + biżuteria) i nie zabierają sobie slotów.
- **Zasada mocy:** komplet setu ≈ dobrze wylosowany rzadki + mechanika. Nigdy mnożnik ×N (lekcja AoV). Każda część ma własne staty przydatne także bez reszty setu (lekcja D2 Tal Rasha).

| Archetyp (zasób) | Set A: 2 cz. / 4 cz. | Set B: 2 cz. / 4 cz. |
|---|---|---|
| Wojownik (Szał) | +15% Szału z trafień / po wydaniu 100 Szału następny cios ogłusza na 1 s | Szał nie spada przez 5 s po wyjściu z walki / przy pełnym Szale +10% redukcji obrażeń |
| Ninja (Skupienie + Znamiona) | Znamiona trwają +2 s / detonacja 3 Znamion zwraca 20 Skupienia | +1 maks. Znamię / trafienie krytyczne nakłada Znamię |
| Mag (Mana) | +20% regeneracji many / zaklęcie za ≥40 many ma 25% szansy na darmowe echo (50% obrażeń) | pierwsze zaklęcie po 3 s bez rzucania kosztuje −50% / nadwyżka many ponad maksimum zamienia się w tarczę |
| Akolita (Dusze) | +1 Dusza za zabicie elity / śmierć sługi daje Duszę i sługa wybucha | +1 maks. sługa / słudzy dziedziczą 20% twojej odporności |

- **Bad luck protection setu:** wymiana 3 duplikatów części na wybraną brakującą (odpowiednik Catalysta z WoW) albo gwarantowana część za pierwsze ukończenie lochu.

### 8.6 Bazy i broń

- 5 przedmiotów na slot na archetyp × 13 slotów × 4 archetypy = **260 baz**, plus 16 typów broni. To dużo modeli i tekstur. Decyzja D5 mówi „2 bazy/grupę w Akcie 1”, więc w Akcie 1 robimy **2 bazy na slot** (poziom 1 i poziom ~6), a pozostałe 3 na Akt 2+.
- Część slotów wspólna dla archetypów (pas, koszula, kolczyki). Wyróżniają je **implicit** i wymaganie atrybutu, a nie osobny model (wzór PoE). Oszczędza to grafikę.
- Każda baza ma **implicit** zgodny z tożsamością (np. „Pas skórzany: +HP”, „Różaniec: +Wiara”). Implicit stoi nad kreską.

---

## 9. Czego właściciel mógł nie wziąć pod uwagę

Priorytet dla Aktu 1: **P1** musi być przed startem, **P2** warto mieć w Akcie 1, **P3** później.

| # | Temat | Dlaczego to ważne (źródło) | P |
|---|---|---|---|
| 1 | **Smart loot / drop pod archetyp** | 4 archetypy × 260 baz: bez tego ~75% dropu jest „nie dla mnie”. W D3 po Loot 2.0 ~85% dropu jest pod klasę ([Diablo Wiki](https://www.diablowiki.net/Smart_Loot)) | P1 |
| 2 | **Łup na minutę i budżet mocy** | ile przedmiotów i ile rzadkich na godzinę, ile ulepszeń do poz. 10. D3 zmniejszyło ilość i podniosło jakość ([Digital Trends](https://www.digitaltrends.com/gaming/diablo-iii-reaper-souls-guide-loot-2-0-changes/)). Bez tego nie da się ustawić szans z 8.1 | P1 |
| 3 | **Poziom przedmiotu vs poziom postaci vs wymaganie** | w PoE to trzy różne liczby ([poewiki Item level](https://www.poewiki.net/wiki/Item_level)). U nas drop MM nie ustawia ilvl (notatka 29.09), więc to strefa lub loch musi wybierać pulę | P1 |
| 4 | **Rodziny modów (zero duplikatów)** | bez grup z `max: 1` MMOItems może dać dwa „+HP” ([poewiki Modifier](https://www.poewiki.net/wiki/Modifier), [docs Modifiers](https://docs.phoenixdevt.fr/mmoitems/items/modifiers.html)) | P1 |
| 5 | **Czytelność tooltipu i porównywanie** | D4 przebudowało itemizację właśnie po to, żeby dało się łatwo rozpoznać ulepszenie ([Blizzard S4](https://news.blizzard.com/en-us/article/24077223/galvanize-your-legend-in-season-4-loot-reborn)). MC nie pokazuje porównania obok, więc: tier `[T2]`, ✔/✘ i ewentualnie strzałka ▲▼ względem założonego przedmiotu (wymaga pluginu lub placeholdera) | P1 |
| 6 | **Odmiana nazw po polsku** | przymiotnik w prefiksie musi się zgadzać z rodzajem bazy (8.2), a MMOItems ma stały tekst | P1 |
| 7 | **Soulbound / wiązanie przy założeniu** | decyduje o handlu i wartości dropu. Soulbound w MMOItems blokuje skrzynie i handel, a przedmiot zostaje po śmierci ([docs Soulbound](https://docs.phoenixdevt.fr/mmoitems/features/soulbound.html)). Propozycja: zwykłe i magiczne wolne, sety i unikaty wiązane przy założeniu | P1 |
| 8 | **Upusty waluty (sinks)** | identyfikacja, quality, reroll, naprawa. W PoE waluta jest materiałem, więc sama znika ([Massively OP](https://massivelyop.com/2026/01/07/path-of-exile-co-creator-chris-wilson-discusses-his-mistakes-on-economic-integrity-in-online-rpgs/)). Wynn ma kilkanaście upustów i dalej walczy z inflacją ([audyt 03](../wynncraft/audyt/03-przedmioty-crafting-ekonomia.md)) | P1 |
| 9 | **Rozbiórka przedmiotów (salvage)** | co z 90% łupu? MMOItems ma deconstruct zależny od tieru ([docs Tiers](https://docs.phoenixdevt.fr/mmoitems/features/tiers.html)). Rozbiórka zasila profesje i quality (8.4). Bez niej ekwipunek zapycha się śmieciami | P1 |
| 10 | **Filtr / podświetlenie łupu** | glow i hologram w kolorze tieru na ziemi (MMOItems), automatyczne podnoszenie dopiero od magicznego wzwyż. LE i PoE mają filtry ([Maxroll LE filter](https://maxroll.gg/last-epoch/resources/loot-filter-guide)) | P2 |
| 11 | **Rzemiosło / reroll affiksów z limitem** | bez tego słabo wylosowany rzadki to śmieć. Chaos w PoE2 zmienia 1 mod ([poe2wiki](https://www.poe2wiki.net/wiki/Chaos_Orb)), LE ogranicza liczbę prób przez Forging Potential ([Maxroll LE](https://maxroll.gg/last-epoch/resources/beginner-crafting-guide)). W Akcie 1 wystarczą 2–3 materiały: „dodaj affiks do magicznego”, „przelosuj 1 affiks”, „zwiększ quality” | P2 |
| 12 | **Pity timer / bad luck protection** | dla unikatów z bossów i części setów: licznik prób, potem gwarancja (Artisan's Energy z Lost Ark, [LA Wiki](https://lostark.fandom.com/wiki/Honing); Catalyst z WoW, [Wowhead](https://www.wowhead.com/guide/revival-catalyst-dragonflight)) | P2 |
| 13 | **Chase items** | 1–2 przedmioty na archetyp, na które warto polować: T3 tylko z dropu (jak Greater Affix w D4) albo unikat z efektem mechaniki (jak Major ID Wynna, [wiki](https://wynncraft.wiki.gg/wiki/Identifications)) | P2 |
| 14 | **Inflacja mocy między aktami** | krzywa do 100 już istnieje, więc budżet statów na poziom (T1…T10) trzeba ustalić teraz. +15 000% z D3 to przestroga ([Maxroll AoV](https://maxroll.gg/d3/guides/aov-heavens-fury-crusader-guide)) | P2 |
| 15 | **Moc setu vs rzadkie** | set nie może być obowiązkowy (D3) ani bezużyteczny (w LE „sety bez elastyczności”, [Maxroll](https://maxroll.gg/last-epoch/resources/set-items-crafting)). Test: build na rzadkich ma ≥ 85% mocy buildu na secie | P2 |
| 16 | **Handel i wycena** | czy będzie rynek lub aukcja? Bez rynku rarity ma tylko wartość dla samego gracza. Rozważyć stałe nazwy statów w affiksach, żeby dało się ich szukać | P2 |
| 17 | **Pojemność ekwipunku i magazynu** | 13 slotów + broń + drop zapełniają plecak w 10 minut. Sakwa przyjmuje tylko materiały (notatka „stałe sloty ekwipunku”), więc trzeba schowka na zestawy i szybkiej rozbiórki | P2 |
| 18 | **Identyfikacja: czy w ogóle?** | PoE2 zrobiło ją darmową u NPC ([GameSpot](https://www.gamespot.com/articles/path-of-exile-2-how-to-identify-items-for-free/1100-6528299/)), Wynn płatną (ponowna 5×) ([wiki](https://wynncraft.wiki.gg/wiki/Identifier)). Rekomendacja: niezidentyfikowane wypadają tylko rzadkie i wyżej, a zwój jest tani i pochodzi z profesji | P2 |
| 19 | **Zmiana archetypu lub postaci a przedmioty** | MMOProfiles: czy przedmioty i sety przechodzą między postaciami? Soulbound per konto czy per postać? | P2 |
| 20 | **Przedmioty startowe i progi poziomów** | co gracz ma na poz. 1, 3, 5 i 10. Pierwszy magiczny powinien wypaść w pierwszych minutach, jako nagroda za tutorial | P1 |
| 21 | **Kara za śmierć a łup** | EQ zostaje po śmierci (notatka o karze za śmierć), więc gracz nie ryzykuje utraty i przedmioty tracą wartość wolniej. Tym ważniejsze są upusty (pkt 8) | P3 |
| 22 | **Transmogryfikacja / wygląd** | 260 baz i tak nie dostanie osobnych modeli. Wygląd oddzielony od statów (kosmetyki, monetyzacja bez P2W; Wilson: [Massively OP](https://massivelyop.com/2026/01/07/path-of-exile-co-creator-chris-wilson-discusses-his-mistakes-on-economic-integrity-in-online-rpgs/)) | P3 |
| 23 | **Sockety / kamienie** | gem stones w MMOItems są gotowe ([docs](https://docs.phoenixdevt.fr/mmoitems/features/gem-stones.html)), ale to trzecia oś losowości. Odłożyć | P3 |
| 24 | **Testowalność** | każda reguła (limit 2P+2S, brak duplikatów, bramka tieru) potrzebuje evalu, który generuje 10 000 przedmiotów i liczy wynik parserem (notatka „weryfikacja zamiast zgadywania”) | P1 |

---

## 10. Pułapki do uniknięcia [W]

1. **Affix soup** (D4 1.0): warunkowe, drobne procenty, 6 linii bez znaczenia. U nas maks. 4 affiksy i żadnych warunków.
2. **Set dyktuje build** (D3): 6 części i mnożnik ×N. U nas progi 2/4 i enablery zamiast mnożników.
3. **Set jako rarity** (Wynn przed 2.2): osobny kolor myli, jeśli chodzi o moc. Set to cecha przedmiotu.
4. **Reroll bez limitu**: każdy przedmiot kończy jako idealny i drop traci wartość. Potrzebny limit prób (Forging Potential) albo rosnący koszt (5× w Wynnie).
5. **Lokalne vs globalne w jednym zdaniu** (PoE): „+10% obrażeń” działa inaczej na broni i na pierścieniu. Na akcesoriach piszemy „+10% obrażeń (ogólne)”, na broni „obrażenia broni”.
6. **Brak ilvl w dropie MM**: wszystkie przedmioty idą z jednej puli i T2 wypada na poziomie 1. Pule muszą być osobne dla każdej strefy.
7. **Duplikaty statów**: różne modyfikatory z tej samej rodziny, gdy grupy są źle ustawione.
8. **260 baz od razu**: nierealne graficznie i balansowo. W Akcie 1 2 bazy na slot.
9. **Odmiana po polsku** w nazwach (8.2).
10. **Quality z ryzykiem zniszczenia**: frustracja bez sensu. Quality zawsze bezpieczne; ryzyko najwyżej w Akcie 2+ i z pity.
11. **Inflacja między aktami**: najpierw budżet statów na poziom (T1–T10), potem treści.
12. **Nieczytelne kolory w MC**: §8 (ciemnoszary) ledwo widać na tle tooltipu, więc nie używać go dla tieru przedmiotu. Obecny `item-tiers.yml` ma `UNCOMMON: '&8&l'`, do poprawy.

---

## Źródła (zbiorczo)

- PoE1: [Rarity](https://www.poewiki.net/wiki/Rarity), [Modifier](https://www.poewiki.net/wiki/Modifier), [Quality](https://www.poewiki.net/wiki/Quality), [Item level](https://www.poewiki.net/wiki/Item_level), [Corrupted](https://www.poewiki.net/wiki/Corrupted), [Attribute](https://www.poewiki.net/wiki/Attribute), [Crafting](https://www.poewiki.net/wiki/Crafting)
- PoE2: [Rarity](https://www.poe2wiki.net/wiki/Rarity), [Quality](https://www.poe2wiki.net/wiki/Quality), [Chaos Orb](https://www.poe2wiki.net/wiki/Chaos_Orb), [Exalted Orb](https://www.poe2wiki.net/wiki/Exalted_Orb), [GameSpot: Hooded One](https://www.gamespot.com/articles/path-of-exile-2-how-to-identify-items-for-free/1100-6528299/), [Massively OP: Wilson](https://massivelyop.com/2026/01/07/path-of-exile-co-creator-chris-wilson-discusses-his-mistakes-on-economic-integrity-in-online-rpgs/)
- Diablo: [Arreat Summit Tal Rasha](https://classic.battle.net/diablo2exp/items/sets/sets7.shtml), [GamerGuides D2R sets](https://www.gamerguides.com/diablo-ii-resurrected/guide/equipment/set-items), [Inven D3 LoN](https://www.invenglobal.com/articles/7534/diablo-iii-legacy-of-nightmares-what-to-go-for-each-class), [forum D4 sety](https://us.forums.blizzard.com/en/d4/t/do-sets-remove-build-diversity-or-promote-it/251193), [Maxroll AoV](https://maxroll.gg/d3/guides/aov-heavens-fury-crusader-guide), [Maxroll GR](https://maxroll.gg/d3/resources/greater-rift-explained), [Diablo Wiki Smart Loot](https://www.diablowiki.net/Smart_Loot), [Digital Trends Loot 2.0](https://www.digitaltrends.com/gaming/diablo-iii-reaper-souls-guide-loot-2-0-changes/), [Blizzard D4 S4](https://news.blizzard.com/en-us/article/24077223/galvanize-your-legend-in-season-4-loot-reborn), [Fextralife Loot Reborn](https://diablo4.wiki.fextralife.com/Loot+Reborn), [forum D4 conditional](https://us.forums.blizzard.com/en/d4/t/conditional-affixes-are-not-fun-unappealing-feel-unrewarding-and-dont-work-as-advertised/63114), [Maxroll D4 aspects](https://maxroll.gg/d4/wiki/legendary-aspects)
- Wynncraft: [Item Rarity](https://wynncraft.wiki.gg/wiki/Item_Rarity), [Identifications](https://wynncraft.wiki.gg/wiki/Identifications), [Identifier](https://wynncraft.wiki.gg/wiki/Identifier), [Set Items](https://wynncraft.wiki.gg/wiki/Set_Items), [Powders](https://wynncraft.wiki.gg/wiki/Powders), [Skill Points](https://wynncraft.wiki.gg/wiki/Skill_Points), [forum: usunięcie Set rarity](https://forums.wynncraft.com/threads/remove-the-set-rarity-93-approval.295354/), [forum: obliczanie ID](https://forums.wynncraft.com/threads/how-identifications-are-calculated.128923/), nasz [audyt 03](../wynncraft/audyt/03-przedmioty-crafting-ekonomia.md)
- Last Epoch: [Maxroll Crafting Basics](https://maxroll.gg/last-epoch/resources/beginner-crafting-guide), [Maxroll Set Items](https://maxroll.gg/last-epoch/resources/set-items-crafting), [Maxroll Legendary](https://maxroll.gg/last-epoch/resources/legendary-items-crafting-guide), [LE Support Legendary](https://support.lastepoch.com/hc/en-us/articles/46361924310555-Legendary-Items), [Maxroll Loot Filter](https://maxroll.gg/last-epoch/resources/loot-filter-guide), [forum LE filtr](https://forum.lastepoch.com/t/clarification-regarding-the-affix-tier-options-in-loot-filter/62450)
- WoW / Lost Ark: [Wowhead Revival Catalyst](https://www.wowhead.com/guide/revival-catalyst-dragonflight), [Wowhead upgrade system](https://www.wowhead.com/guide/item-upgrade-system-dragonflight), [Maxroll LA gear](https://maxroll.gg/lost-ark/resources/gear-progression-guide), [LA Wiki Honing](https://lostark.fandom.com/wiki/Honing)
- Hypixel SkyBlock (oficjalna wiki zamknięta w lipcu 2026, używamy społecznościowej): [Rarity](https://hypixelskyblock.minecraft.wiki/w/Rarity), [Reforging](https://hypixelskyblock.minecraft.wiki/w/Reforging), [Recombobulator 3000](https://hypixelskyblock.minecraft.wiki/w/Recombobulator_3000), [Star Upgrades](https://hypixelskyblock.minecraft.wiki/w/Star_Upgrades)
- MMOItems: [Modifiers](https://docs.phoenixdevt.fr/mmoitems/items/modifiers.html), [Templates](https://docs.phoenixdevt.fr/mmoitems/items/templates.html), [Tiers](https://docs.phoenixdevt.fr/mmoitems/features/tiers.html), [Sets](https://docs.phoenixdevt.fr/mmoitems/features/sets.html), [Upgrading](https://docs.phoenixdevt.fr/mmoitems/features/upgrading.html), [Soulbound](https://docs.phoenixdevt.fr/mmoitems/features/soulbound.html), [Gem stones](https://docs.phoenixdevt.fr/mmoitems/features/gem-stones.html), [Identification](https://docs.phoenixdevt.fr/mmoitems/features/identification.html)
