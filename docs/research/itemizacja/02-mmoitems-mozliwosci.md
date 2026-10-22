# 02. Co MMOItems 6.10.1 umie wprost: itemizacja w stylu PoE/Diablo

Data: 2026-09-29. Wersje: MMOItems 6.10.1-20260330, MythicLib 1.7.1-20260330, MMOCore 1.13.1-20260330, MMOInventory 2.0, MythicMobs Premium 5.11.2, Paper 1.21.11-132.
Tryb: tylko odczyt. Na world1 niczego nie zmieniono i nie wysłano żadnej komendy. Kontenera itm-test nie użyłem.

**Źródła, w kolejności ważności:**
1. **[DOCS]** Oficjalna dokumentacja https://docs.phoenixdevt.fr/mmoitems/… Stare wiki GitLaba przekierowuje w to samo miejsce.
2. **[JAR]** Klasy zdekompilowane CFR 0.152 z jarów, które mamy na dysku. Kod rozstrzyga, gdy dokumentacja milczy albo się myli.
3. **[PLIK]** Konfiguracja world1: `plugins/MMOItems/...`
4. **[TEST]** Pomiar na izolowanym Paperze 1.21.11 w dockerze (`--network none`, port 25999). Pluginy: MythicLib + MMOItems + MMOCore + mały plugin testowy `MITest`, który generuje 3000 przedmiotów przez API i wypisuje nazwy, lore i NBT.
   - Pliki testu: `/home/dev/.claude/jobs/46f00c60/tmp/mitest/` (`srv/plugins/MMOItems/modifiers/test_affiksy.yml`, `srv/mitest-out.txt`, `plug/src/pl/test/MITest.java`).

> **Ważne sprostowanie na start:** `plugins/MMOItems/gen-templates.yml` **nie ma nic wspólnego z generatorem przedmiotów**. To szablony *generowania bloków MMOItems w świecie* (`replace`, `vein-size`, `depth`; klasa `api/block/WorldGenTemplate`). Szablony generatora przedmiotów to po prostu wpisy w `item/<typ>.yml` z sekcjami `option` / `base` / `modifiers` [DOCS templates]. Nasz `gen-templates.yml` (57 linii) to nieużywany przykład z paczki.

---

## 0. Tabela zbiorcza

| Wymaganie | MMOItems | Jak (klucz / mechanizm) | Źródło |
|---|---|---|---|
| **Rarity** (normal/magic/rare/unique) | **TAK** | `item-tiers.yml`: `name`, `generation.chance`, `generation.capacity`, `item-glow`, `unidentification`, `tooltip`, `tooltip-style`, `deconstruct-item`. W nazwie przedmiotu działają `<tier-color>` i `<tier-name>` | [DOCS tiers](https://docs.phoenixdevt.fr/mmoitems/features/tiers.html); [JAR] `api/ItemTier`, `stat/DisplayName.whenApplied` |
| **Affiksy prefiks/sufiks** | **TAK**, z haczykami | Modyfikator ma `prefix:` albo `suffix:` (string lub `{format, priority}`), `chance`, `weight`, `stats`. Działają modyfikatory publiczne w `modifiers/*.yml` i grupy z `min`/`max`. **Haczyk 1:** jeden modyfikator ma TYLKO JEDEN dopisek do nazwy; przy obu kluczach wygrywa `suffix`. **Haczyk 2:** kolejność kilku sufiksów w nazwie jest losowa | [DOCS modifiers](https://docs.phoenixdevt.fr/mmoitems/items/modifiers.html); [JAR] `ModifierNode` ctor, `MMOItemBuilder.addModifier`; [TEST] 0/3000 nazw z „PREFIKS_OBA” |
| **Limit affiksów** (np. 3P+3S) | **TAK** | Grupa `prefiksy: {max: 3}` + grupa `sufiksy: {max: 3}`. Całość dodatkowo ogranicza pojemność (capacity) tieru | [DOCS modifiers#groups]; [TEST] rozkład liczby modów nigdy >3 na grupę |
| **Tiery affiksów po ilvl** (T1 dopiero od ilvl X) | **CZĘŚCIOWO** | Twardej bramki „min ilvl” brak: `ModifierNode` czyta tylko `chance, weight, min, max, prefix/suffix, stats, modifiers`. Działa bramka miękka: `weight` affiksu kontra `capacity = base + ilvl·scale ± spread` tieru. Wartość affiksu rośnie z ilvl przez `scale` | [JAR] `ModifierNode`, `MMOItemBuilder`; [TEST] tabela w §1.6 |
| **Wykluczanie duplikatów / rodziny affiksów** | **TAK** (przez grupy) | Rodzina = grupa `max: 1` (np. `zycie: {max:1, modifiers:{t1,t2,t3}}`). Wewnątrz grupy dziecko nie wylosuje się dwa razy. Warunków (`conditions`) brak | [JAR] `ModifierNode.whenCollected` (usuwa wylosowane dziecko z listy) |
| **Quality 0–20%** | **CZĘŚCIOWO** | Ulepszanie (`upgrade:` + `upgrade-templates.yml`, `max: 20`) jako „jakość”, podbijane kamieniem (consumable z `upgrade: {reference, success}`). Wpisuje się w nazwę jako suffix `(+N)`. **Nie da się dropić z losowym poziomem ulepszenia**: przy generowaniu zawsze 0 | [DOCS upgrading](https://docs.phoenixdevt.fr/mmoitems/features/upgrading.html); [JAR] `UpgradeData`, `UpgradeStat.handleConsumableEffect`; [TEST] +10 → ×1,1046, +20 → ×1,2202 |
| **Sety z progami** | **TAK** | `item-sets.yml`: `bonuses: {'2':…, '3':…, '4':…}` (progi 2–10, **kumulatywne**). W bonusie: statystyki (liczby), `potion-<efekt>`, `ability-<x>`, `particle-<x>`, `granted-permissions`. Lore to statyczny `lore-tag`. Akcesoria MMOInventory i broń w ręce się liczą | [DOCS sets](https://docs.phoenixdevt.fr/mmoitems/features/sets.html); [JAR] `ItemSet`, `InventoryResolver.resolveItemSet`, `MMOInventorySupplier` |
| **Losowe zakresy statystyk** (identyfikacje Wynna) | **TAK** | Każda liczba: `{base, scale, spread, max-spread, min, max}` (rozkład normalny, spread względny) albo `{min, max}` (jednostajny). Albo skrót `'a -> b'` | [DOCS numerical-stats](https://docs.phoenixdevt.fr/mmoitems/features/stats/numerical-stats.html); [JAR] `NumericStatFormula`; [TEST] 15±20% → min 12,0 / max 18,0 |
| **Identyfikacja** | **TAK** | Drop z `unidentified=<szansa>`, consumable `can-identify: true`, `/mi item identify`. Wygląd w `item-types.yml → <TYP>.unident-item`, tier daje `unidentification.{name,range,prefix}` | [DOCS identification](https://docs.phoenixdevt.fr/mmoitems/features/identification.html); [TEST] „Unidentified Sword, Lvl Range 49-52, Tier Rzadki” |
| **Reroll** (Chaos/Divine Orb) | **NIE** (per przedmiot) / **CZĘŚCIOWO** (globalnie) | Consumable „reroll” nie istnieje. Jest tylko RevID: podbicie `revision-id` z `item-revision.keep-data.reroll: true` (+ `modifications: false`) przelosowuje WSZYSTKIE kopie szablonu. Ilvl zostaje, tier zostaje przy `tier: true`. API `MMOItemReforger` umie to dla jednego przedmiotu (tylko z kodu) | [DOCS revision-id](https://docs.phoenixdevt.fr/mmoitems/items/revision-id.html); [JAR] `api/util/MMOItemReforger.reforge`, `reforge/ReforgeReason` = JOIN/CRAFT/PICKUP/CLICK |
| **Sockety / gemy** | **TAK** | `gem-sockets: [Red, Blue, Uncolored]` na przedmiocie. Typ `GEM_STONE` z `gem-color`, restrykcjami typu i dowolnymi statystykami/abilities. `random-unsocket` na consumable wyjmuje gemy | [DOCS gem-stones](https://docs.phoenixdevt.fr/mmoitems/features/gem-stones.html); [PLIK] `item/sword.yml:68-70` |
| **Drop z poziomem (ilvl) i tierem** | **CZĘŚCIOWO** | MM `mmoitems{type;id;unidentified}`: **ilvl = poziom ZABÓJCY ±2** (gdy `level-item: true`), tier losowy (gdy `tiered: true`). Poziomu moba nie widzi, `level=`/`tier=` nie istnieją. Bez gracza: ilvl 0, bez tieru. Pełna kontrola: `mi generate <gracz> -type: -id: -level: -tier:` (do eq, nadmiar pod nogi) albo tabela MMOCore `mmoitem{type;id;level;tier;unidentified;soulbound}` / `miloot{…}` | [JAR] MythicMobs `MMOItemsSupport$MMOItemsDrop`, MMOItems `MMOItemTemplate.newBuilder(RPGPlayer)`, `GenerateCommandTreeNode`, `comp/mmocore/load/*` |
| **Lore/tooltip** | **TAK** | `language/lore-format.yml` (+ własne w `language/lore-formats/`, stat `lore-format`), `stats.yml`, MiniMessage, PAPI (bez gracza), `MATH%…%`, `{tier}`, `{type}`, `{upgrade_level}`. **Ilvl nie ma w lore** (tylko NBT). Obejście: custom stat `custom-ilvl: {base:0, scale:1}` [TEST ✔] | [DOCS lore-format](https://docs.phoenixdevt.fr/mmoitems/features/item-lore/lore-format.html); [JAR] `LoreBuilder`, `stat/ItemLevel` |
| **Wymagania atrybutów** | **TAK** | MMOCore rejestruje przy starcie `required-<atrybut>` i `additional-<atrybut>` (+`_percent`) dla KAŻDEGO atrybutu z `MMOCore/attributes`. Sprawdzana jest suma: punkty + bonusy z przedmiotów (jak Wynn) | [JAR] `comp/mmocore/MMOCoreHook`, `RequiredAttribute.canUse`, MMOCore `PlayerAttributes$AttributeInstance.getTotal` |

---

## 1. Generator przedmiotów (szablony + modyfikatory)

### 1.1 Szablon [DOCS templates, JAR `MMOItemTemplate`]
Klucze szablonu w `item/<typ>.yml`: `option`, `base`, `modifiers`, a także `min`, `max` i `chance` korzenia (korzeń to zwykły węzeł-grupa).
- `option.tiered: true`: tier wybierany z `generation.chance` wszystkich tierów, gdy żaden nie został podany.
  - Algorytm: kolejne tiery z prawdopodobieństwem `chance/(1-suma_dotychczasowych)`. Suma chance < 1 oznacza, że reszta wychodzi bez tieru [JAR `TemplateManager.rollTier`].
- `option.level-item: true`: ilvl = poziom gracza ± `item-level-spread`. U nas to 2 [PLIK `config.yml:48`].
  - Rozkład to gauss ×0,7 obcięty do [lvl−2, lvl+2] i ≥1 [JAR `rollLevel`].
- `option.capacity: {…}`: własna formuła pojemności. Nadpisuje tier.
- `option.roll-modifier-check-order: true`: tasuje kolejność modyfikatorów. Bez tego lecą w kolejności z pliku (greedy), więc **pierwsze w pliku mają pierwszeństwo do pojemności**.
- **Pojemność** = `template.capacity` → `tier.generation.capacity` → `config.yml default-item-capacity`. U nas domyślna to `base 3, scale 0` [PLIK `config.yml:96-100`].
  - Liczona raz, przy starcie budowania: `calculate(ilvl)` [JAR `MMOItemBuilder`].
- Tier ustawiony w `base.tier: X` przy braku `tiered` też daje pojemność tego tieru.

### 1.2 Modyfikator [DOCS modifiers, JAR `ModifierNode`]
- `chance` (0–1, domyślnie 1). Ma być > 0, inaczej błąd.
- `weight` (domyślnie 0): zużycie pojemności.
- `prefix` / `suffix`: string albo `{format, priority}`. Priorytet chowa niższe prefiksy albo niższe sufiksy (osobno dla każdego rodzaju). Równe priorytety pokazują się razem.
- `stats:` przyjmuje dowolne statystyki: `ability`, `lore`, `element`, `required-*`, `two-handed`, `tier`, `perm-effects`… Liczby są mergowane (sumowane) z bazą.
- `tier: X` w `stats` modyfikatora **ustawia tier przedmiotu**. Docs pokazują to jako „statystyki skalowane tierem”. Uwaga: pojemność jest już wtedy policzona, więc taki tier nie zmienia liczby affiksów.

### 1.3 Algorytm losowania [DOCS + JAR `ModifierNode.whenCollected`]
1. Węzeł rzuca `chance`. Jeśli `weight` > pozostałej pojemności, odpada. W przeciwnym razie odejmuje wagę, dopisuje nazwę i statystyki, a potem rekurencyjnie losuje dzieci.
2. Dzieci losowane są w kolejności do `max`. Wylosowane dziecko **wypada z puli**, więc nie ma duplikatu w tej samej grupie.
3. Jeśli nie osiągnięto `min`, dolosowuje wagowo po `chance`, ignorując rzut. Pojemność nadal obowiązuje, więc `min` NIE jest gwarantowane przy małej pojemności (docs mówią to wprost).
4. Wartości statystyk: `NumericStatFormula.calculate(ilvl)`.

### 1.4 Formuła liczb [DOCS numerical-stats, JAR `NumericStatFormula`]
- `{base, scale, spread, max-spread, min, max}`: `śr = base + ilvl·scale`.
  - `spread` to odchylenie standardowe **względne** (±10% dla 0,1), obcięte do ±`max-spread` (domyślnie 0,3). `min`/`max` to twarde granice.
  - Względność ustawia `config.yml: additive-spread-formula: false` [PLIK `config.yml:298`]. Przy `true` spread byłby liczbą bezwzględną.
- `{min: a, max: b}` (bez base/scale/spread) daje rozkład **jednostajny**. W stringu: `'10 -> 20'`. Skrót stringiem `'base scale spread max-spread min max'`.
- [TEST] `attack-damage {base 5, scale .5, spread .1, max-spread .2}` na ilvl 20: min 12,000 / max 18,000 / śr 14,988.

### 1.5 Bramkowanie affiksu poziomem: teza „nie da się” POTWIERDZONA, z zastrzeżeniem
- Twardego „min ilvl” affiksu nie ma: brak klucza w `ModifierNode`, brak warunków.
- Bramka miękka działa: pojemność rośnie z ilvl, więc ciężki affiks (duży `weight`) fizycznie się nie mieści na niskim ilvl.
  - Pojemność zależy też od tieru. Bramka „T1 od ilvl 60” jest więc zawsze „T1 od ilvl 60 dla RARE / od 100 dla MAGIC”. Da się to wyliczyć i ustawić deterministycznie (spread pojemności 0).
- Wartość affiksu rośnie z ilvl przez `scale`. „Tier affiksu” to więc bardziej przedział wartości niż osobny wpis.

### 1.6 Pomiar [TEST, 3000 przedmiotów na komórkę]
- Grupy: `prefiksy{max 3: zycie{max1: T1 w6, T2 w3, T3 w1}, obrazenia{max1: Okrutny w5, Ostry w1}}` i `sufiksy{max 3: krytyk w2, szybkosc w2, oba w1}`.
- Tiery testowe:
  - `T_MAGIC` capacity `2 + 0,05·ilvl`, spread 0.
  - `T_RARE` capacity `4 + 0,1·ilvl`, spread ±10% (max 30%).

| tier / ilvl | pojemność | liczba modów 0/1/2/3/4/5 | T1 (w6) | T2 (w3) | Okrutny (w5) |
|---|---|---|---|---|---|
| brak / 1–100 | 3 (domyślna) | ~100/1400/1300/200/0/0 | **0** | ~900 | **0** |
| MAGIC / 1 | 2,05 | 140/1504/1356/0 | 0 | **0** | 0 |
| MAGIC / 20 | 3,0 | 102/1407/1298/193 | 0 | 918 | 0 |
| MAGIC / 100 | 7,0 | 75/947/1224/581/146/27 | 277 | 777 | 407 |
| RARE / 1 | ~4,1 | 75/1319/1103/500/3 | **0** | 950 | **0** |
| RARE / 20 | ~6 | 108/1081/1102/589/120 | 142 | 842 | 264 |
| RARE / 100 | ~14 | 71/683/1204/750/256/36 | 282 | 812 | 537 |

Wnioski z pomiaru:
1. Capacity działa jak twardy limit.
2. `max` grup jest respektowany.
3. Modyfikator z prefix+suffix pokazał tylko sufiks (0/3000 z prefiksem).
4. Nazwy mają postać `&6Tytaniczny &e&lMiecz probny &fPrecyzji &dSUFIKS_OBA`, a kolejność sufiksów się zmienia („Precyzji SUFIKS_OBA” i „SUFIKS_OBA Precyzji” wypadły obie).

---

## 2. Tiery (`item-tiers.yml`) [DOCS tiers, JAR `ItemTier`]

Klucze czytane przez kod: `name`, `unidentification.{name, prefix, range}`, `item-glow.{hint, color}`, `tooltip`, `tooltip-style`, `generation.chance`, `generation.capacity`, `deconstruct-item`.

- **`parent:` NIE jest czytany.** Mamy go w `item-tiers.yml:18` (COMMON), więc jest martwy.
- Kolor nazwy przedmiotu: w `base.name` wpisujemy `<tier-color>`, `<tier-color-cleaned>` albo `<tier-name>` [JAR `DisplayName`, TEST: `&e&lMiecz probny`]. Linia `#tier#` w lore pokazuje `stats.yml: tier`.
- **Drop chance tieru** = `generation.chance`. Działa przy `tiered: true`, `mi generate` bez `-tier` i dropie MM z graczem.
- **Glow/hint** na ziemi: `item-glow` + `config.yml dropped-items.tier-glow/hints`. Lootsplosion koloruje trail kolorem tieru (tylko moby MM ze zmienną `Lootsplosion`) [DOCS lootsplosion].
- **Deconstruct**: tabela success/lose + consumable `can-deconstruct: true` [DOCS].
- **Tooltip** (1.21.2+): `tooltip-style: 'namespace:id'` (vanillowy styl ramki tooltipu z packa) albo `tooltip: <ID z tooltips/>`. To dobre miejsce na ramkę rarity z packa Nexo.
- **Brak** mnożników statystyk per tier. Obejście: modyfikator z `stats: {tier: X, …}` w grupie `min1/max1` (przykład w docs).
- **Znane błędy u nas:**
  - `item/catalyst.yml:20` LUCKY_CHARM ma `tier: NORMAL`, a taki tier nie istnieje. [TEST] Przy każdym budowaniu leci ERROR `Could not find item tier with ID 'NORMAL'` i przedmiot wychodzi bez tieru.
  - Tiery TRASH i COMMON nie mają `generation` → `chance 0`, pojemność domyślna.

---

## 3. Sety (`item-sets.yml`) [DOCS sets, JAR `ItemSet`, `InventoryResolver`]
- Progi `'2'`…`'10'` (klucz `'1'` jest ignorowany). **Bonusy się kumulują**: przy 4 częściach działają 2+3+4.
- W bonusie może być:
  - dowolna liczbowa statystyka MMOItems/MythicLib jako **stała liczba** (bez formuł i skalowania);
  - `potion-<EFEKT>: poziom`;
  - `ability-<cokolwiek>: {type, mode, cooldown, damage…}`: skille MythicLib, także własne z `MythicLib/skill`;
  - `particle-<cokolwiek>: {type, particle, …}`;
  - `granted-permissions: [...]`.
- **Co się liczy**: każdy „aktywny” przedmiot, czyli poprawny slot i spełnione wymagania (poziom, klasa, atrybuty). Wchodzą:
  - zbroja,
  - main/off-hand (broń w ręce z `set:` się liczy),
  - akcesoria MMOInventory: `MMOInventorySupplier` rejestruje je jako `EquipmentSlot.ACCESSORY`.
- **Pułapka:** dwa identyczne pierścienie tego samego setu liczą się jako 2 części (licznik to suma przedmiotów, nie unikalnych id).
- Przedmiot, którego gracz nie może używać (np. brak atrybutu), nie liczy się do setu.
- **Lore**: `lore-tag` jest statyczny (bez podświetlania aktywnych progów i bez licznika „2/4”) i wstawia się w `#set#` [TEST]. Nazwa setu (`name`) nie trafia do lore automatycznie.
- Stat przedmiotu to `set: ID_SETU`.

---

## 4. Quality (jakość 0–20%)
MMOItems nie ma „quality”. Najbliżej jest **ulepszanie** [DOCS upgrading, JAR `UpgradeData`, `DoubleStat.apply`, `UpgradeStat`]:
- Na przedmiocie: `upgrade: {template, max, min, success, destroy, workbench, reference}`. `success` w %, szansa to iloczyn szansy przedmiotu i consumable.
- W `upgrade-templates.yml`:
  - `stat: X%` to wartość względna, **składana** co poziom: 1% ×20 daje ×1,2202, nie ×1,20. Dla dokładnych +20% przy 20 poziomach trzeba ~0,9158%.
  - `stat: X` to wartość bezwzględna.
- Consumable: `upgrade: {reference: jakosc, success: 100}` + `inedible: true`. Gracz przeciąga go na przedmiot. Albo przepis ulepszania na stacji (`workbench: true`).
- Wyświetlanie: `config.yml item-upgrading.name-suffix` (u nas `' &8(&e+#lvl#&8)'`). Globalnie można zmienić na `' &8[&bJakość #lvl#%&8]'`. Albo `display-in-name: false` + `{upgrade_level}` w lore.
- [TEST] +10 → dmg 30,2→33,4, HP 14,1→15,6. +20 → 36,9 / 17,3 (×1,2202). Nazwa `… &8(&e+10&8)`.
- Extra: `min` (podłoga downgrade), `death-downgrade`/`break-downgrade`, event `UpgradeItemEvent` (cancellable).
- **Ograniczenia:**
  - poziom ulepszenia przy generowaniu jest zawsze 0 (`UpgradeData.randomize` zwraca kopię z konfiguracji), więc **losowej jakości na dropie natywnie nie ma**;
  - `name-suffix` jest jeden globalnie, więc „+N” i „jakość” nie mogą współistnieć jako dwa różne systemy;
  - ulepszanie działa tylko na liczbowe statystyki.
- Alternatywa, jeśli jakość ma tylko rozrzucać liczby: sam `spread` (zakres ±X%). Gracz go nie widzi jako „jakości” i nie da się go podnieść.

---

## 5. Losowe zakresy, identyfikacja, reroll, soulbound, sockety
- **Zakresy statystyk** działają na zwykłych przedmiotach (nie tylko w generatorze): każdy `base` może mieć `spread`/`max-spread`/`min`/`max`. Wartość losuje się przy każdym `/mi give`, dropie i craftingu (MMOItem jest budowany ze szablonu). To jest wprost system „identyfikacji Wynna”.
  - Lore pokazuje wylosowaną liczbę, a nie zakres ani % „jakości rzutu”. Brak odpowiednika gwiazdek i procentu Wynna, więc to luka.
- **Identyfikacja**: sekcja 0. Nieidentyfikowany przedmiot pokazuje materiał, typ, tier i przedział poziomu z `required-level` ± `range`.
- **Reroll**: sekcja 0. `/mi reroll` nie istnieje (lista komend `command/mmoitems/*`).
  - Globalny reroll: podbić `base.revision-id` szablonu przy `keep-data.reroll: true`, `modifications: false`. Wtedy statystyki i modyfikatory przelosują się przy następnym JOIN/PICKUP/CLICK.
  - Ilvl zostaje. Tier zostaje, jeśli `keep-tiers` / `keep-data.tier: true`.
  - Przy `reroll: false` wartość i tak się przelosuje, jeśli po zmianie szablonu stała się „nieosiągalna” (1:4200) [DOCS revision-id].
- **Soulbound**: consumable `soulbinding-chance`, `soulbound-level`, `soulbound-break-chance`. W tabeli MMOCore `mmoitem{…;soulbound=0.2}` [DOCS soulbound, JAR `ItemGenerationDropItem`].
- **Sockety**: `gem-sockets: [Red, Blue, Uncolored]` (`config.yml gem-sockets.uncolored`), gem = typ GEM_STONE z `gem-color`. Nieudane nałożenie niszczy gem, ale slotu nie zużywa. `random-unsocket: N` na consumable, `gem-upgrade-default` (skalowanie gemów z ulepszeniem). Kolor gemu nie wyświetla się sam w lore [DOCS].

---

## 6. Drop przedmiotów z poziomem

| Droga | ilvl | tier | Na ziemię? | Źródło |
|---|---|---|---|---|
| MM `mmoitems{type=X;id=Y;unidentified=0.3} 1 0.1` | **poziom zabójcy ±2**, tylko przy `level-item: true`. Bez gracza → 0 | losowy z `generation.chance`, tylko przy `tiered: true`. Bez gracza → brak | tak (drop moba) | [JAR MM] `MMOItemsSupport$MMOItemsDrop` czyta tylko `type`, `id`, `unidentified`, woła `MMOItemDropItem.getItem(PlayerData)` → `template.newBuilder(RPGPlayer)`; [TEST] bez gracza: `Requires Lvl 0`, brak tieru |
| MM `~onDeath` / drop typu `command`: `mi generate <trigger.name> -type:SWORD -id:X -level:<caster.level> -tier:RARE` | dowolny | dowolny | do eq, nadmiar pod nogi (`SmartGive`: addItem, reszta `dropItem`) | [JAR] `GenerateCommandTreeNode` |
| `mi generate` bez `-level` i `-matchlevel` | **losowy 1–100** | zawsze losowany (także gdy szablon nie jest `tiered`) | eq | [JAR] j.w. |
| `mi generate … -tierset:RARE` | – | filtruje szablony o `base.tier`, sam tier = z szablonu | eq | [JAR] |
| `mi drop <typ> <id> <świat> x y z <szansa> <min-max> <unid>` | 0 | brak | tak | [JAR] `DropCommandTreeNode` → `getItem(null)` |
| Tabele MMOCore: `mmoitem{type;id;level;tier;unidentified;soulbound}` (alias `mmoitemtemplate`) i `miloot{type;class;match-class;tierset;tier;level;unidentified;soulbound}` | `level=` albo poziom gracza ±2 | `tier=` albo rzut | skrzynie/łowienie/bloki MMOCore | [JAR] `MMOCoreMMOLoader.loadDropItem`, `ItemGenerationDropItem`, `RandomItemDropItem` |
| `MMOItems/drops.yml` | poziom gracza (jak MM) | j.w. | tak | [DOCS drop-tables]; u nas celowo pusty [PLIK drops.yml] |

**Rozbieżności docs i kodu:**
- Docs podają `gentemplate{id=…;match-level=…}`. Słowa `gentemplate` nie ma w żadnym jarze (MMOItems ani MMOCore), a `match-level` nie jest czytane. Działające klucze to `mmoitem`/`mmoitemtemplate` z `type` + `id`.
- W `miloot` filtr tieru to `tierset`, a `tier` wymusza tier.
- Przykład pojemności w docs tiers ma błąd rachunkowy (`6 + 0.1*12` przy `scale: 1`). Kod liczy `base + ilvl·scale`.

**Teza planu „`mmoitems{}` nie ustawia ilvl” jest POPRAWNA co do sterowania, ale nieprecyzyjna.** Ilvl jest ustawiany, tyle że = poziom gracza, który zabił, a nie poziom moba ani strefy. Tabela dropu MM (np. `mmdroptable` z MMOCore) tego nie zmieni.

---

## 7. Lore / tooltip [DOCS lore-format, JAR `LoreBuilder`, `ItemStackBuilder`]
- Kolejność linii ustala `language/lore-format.yml` (u nas 188 linii, są `#set#`, `#tier#`, `#gem-stones#`, `#lore#`). Formaty linii są w `language/stats.yml`.
  - Własny format: plik w `language/lore-formats/<id>.yml` + stat `lore-format: <id>` na przedmiocie.
  - Linie `#…#` bez danych znikają, a `{bar}` zwija się automatycznie.
- W lore działają:
  - MiniMessage;
  - placeholdery `{tier}`, `{type}`, `{upgrade_level}`, `{ability_<id>_<modifier>}`;
  - `MATH%…%`;
  - PAPI, ale parsowane z graczem `null` (tylko globalne placeholdery).
- **Rarity**: `<tier-color>` w nazwie + linia `#tier#` + `tooltip-style`/`tooltip` tieru.
- **Prefiks/sufiks** trafiają tylko do nazwy (jeden na modyfikator). Opisowe linie affiksów dodaje się przez `stats.lore` modyfikatora (lądują w `#lore#`).
- **Quality**: suffix `(+N)` w nazwie albo `{upgrade_level}`.
- **Ilvl**: brak natywnej linii (`stat/ItemLevel.whenApplied` zapisuje tylko NBT `MMOITEMS_ITEM_LEVEL`). Obejście przetestowane:
  - custom stat `CUSTOM_ILVL` (type double) w `custom-stats.yml`;
  - w szablonie `custom-ilvl: {base: 0, scale: 1}`;
  - `stats.yml: custom-ilvl: '&7Poziom przedmiotu: &f{value}'`;
  - `#custom-ilvl#` w lore-format.
  - Wynik [TEST]: „Poziom przedmiotu: 50”. Custom staty wymagają restartu, nie `/mi reload`.
- **Wymagania**: `#required-level#`, `#required-<atrybut>#` (każdy musi mieć linię w stats.yml i lore-format, tak jak nasze biegłości).
- **Set**: statyczny `lore-tag`.
- Limity: brak twardego limitu linii w MMOItems. Wyświetla klient. Wszystkie linie formatu są globalne dla danego formatu.

---

## 8. Wymagania atrybutów
- Przy starcie MMOItems (hook MMOCore) dla każdego atrybutu z `MMOCore/attributes/*.yml` rejestruje:
  - `REQUIRED_<ID>` (klucz `required-<id>`, np. `required-strength`, `required-bieglosc-miecz`);
  - `ADDITIONAL_<ID>` (`additional-<id>`, daje punkty atrybutu z przedmiotu).
- Nowy atrybut wymaga restartu.
- Sprawdzenie: `PlayerAttributes.getAttribute()` = `getTotal()` = wydane punkty + modyfikatory FLAT + stat `ADDITIONAL_<ID>`, potem RELATIVE i `ADDITIONAL_<ID>_PERCENT`.
  - Bonusy z innych przedmiotów pomagają spełnić wymóg (buildcrafting jak u Wynna).
  - Przedmiot nie spełni wymogu sam sobą, bo jego modyfikatory działają dopiero po spełnieniu wymagań.
- Działa już u nas na ukrytych biegłościach (`required-bieglosc-*` na 204 przedmiotach, patrz 03-stan-u-nas).
- Widoczne atrybuty `strength`/`dexterity`/`intelligence`/`wiara` dadzą automatycznie `required-*`/`additional-*`. Linie `#required-strength#` itd. są już w domyślnym formacie; trzeba je tylko przetłumaczyć w stats.yml.
- Wymóg może skalować się z ilvl: `required-strength: {base: 5, scale: 0.8}`. Affiks może go podnieść (`stats: {required-strength: 3}`, merge sumuje).

---

## 9. Inne istotne
- **Żywioły**: `element: {fire: {damage, damage-percent, weakness, defense, defense-percent}}` [JAR `util/ElementStatType`]. Żywioły z `MythicLib/elements.yml`: FIRE, ICE, EARTH, WIND, THUNDER, WATER, DARKNESS, LIGHTNESS. Także w modyfikatorach i gemach.
- **`two-handed: true`**: z czymkolwiek w drugiej ręce (poza `handworn`) gracz dostaje Slowness II i nie może atakować/używać broni („HANDS_TOO_CHARGED”) [JAR `InventoryResolver.isEncumbered`, `Weapon.checkItemRequirements`, `PlayerData`].
- **`stamina-cost` / `mana-cost`** na broni: blokuje atak przy braku zasobu, pobiera zasób MMOCore przy każdym ataku [JAR `Weapon`].
- **Mnożniki statystyk per tier**: brak (sekcja 2).
- **Custom stats** (`custom-stats.yml`, double/text): dowolna liczba z formułą, widoczna w lore. Nie ma efektu w walce bez skryptu (MythicLib `<mmostat.custom_x>`?). Działanie w skillach niesprawdzone.
- **Faction damage**: `faction-damage-<frakcja MM>` rejestruje się automatycznie z frakcji MythicMobs [JAR `MythicMobsLoadHook`].
- **Stacje craftingu** mogą produkować przedmioty z generatora (`tiered` szablonu działa przy craftingu, docs templates) oraz ulepszać (`upgrading-recipes`).
- **Aktualizacja wygenerowanych przedmiotów**: RevID z `keep-data` (nazwa, gemy, ulepszenia, tier, modyfikatory). Zmiana balansu szablonu dociera do starych przedmiotów tylko przez RevID.

---

## 10. Przykłady YAML (sprawdzone składniowo z kodem; 1–3 zmierzone w teście)

### 10.1 Tier
```yaml
# item-tiers.yml
MAGICZNY:
  name: '&9&lMagiczny'
  tooltip-style: 'nexo:ramka_magiczna'   # opcjonalnie, styl ramki z packa (1.21.2+)
  unidentification:
    name: 'Magiczny'
    prefix: '&9'
    range: 3
  item-glow:
    hint: true
    color: 'BLUE'
  generation:
    chance: 0.20
    capacity:            # pojemność na affiksy: 2 przy ilvl 0, 7 przy ilvl 100
      base: 2
      scale: 0.05
      spread: 0          # 0 = deterministyczna bramka affiksów po ilvl
  deconstruct-item:
    success:
      coef: 1
      items:
        MATERIAL:
          ESENCJA_MAGICZNA: 100,1-1,0
```

### 10.2 Szablon generatora z prefiksem/sufiksem
```yaml
# item/sword.yml
MIECZ_ZELAZNY:
  option:
    tiered: true
    level-item: true
  base:
    material: IRON_SWORD
    name: '<tier-color>Żelazny miecz'
    attack-damage: {base: 5, scale: 0.5, spread: .08, max-spread: .15}
    attack-speed: 1.6
    required-level: {base: 0, scale: 1}
    required-strength: {base: 5, scale: 0.6}
    required-bieglosc-miecz: 1
    custom-ilvl: {base: 0, scale: 1}        # widoczny ilvl (custom stat)
    upgrade: {template: jakosc, max: 20, success: 100, reference: jakosc}
  modifiers:
    prefiksy: {}                            # publiczna grupa z modifiers/
    sufiksy: {}
```

### 10.3 Publiczne modyfikatory (rodziny = grupy max 1; prefiksy/sufiksy = grupy max 3)
```yaml
# modifiers/affiksy_bron.yml
zycie:                      # rodzina: najwyżej jeden tier affiksu
  max: 1
  modifiers:
    zycie_t1:
      chance: 0.1
      weight: 6             # mieści się dopiero przy dużej pojemności (wysoki ilvl / tier)
      prefix: {format: '&6Tytaniczny', priority: 2}
      stats:
        max-health: {base: 12, scale: 0.4, spread: .1, max-spread: .2}
        lore: ['&7+Życie (T1)']
    zycie_t2:
      chance: 0.3
      weight: 3
      prefix: {format: '&aTęgi', priority: 1}
      stats:
        max-health: {base: 6, scale: 0.3, spread: .1, max-spread: .2}
    zycie_t3:
      chance: 0.6
      weight: 1
      prefix: '&fKrzepki'
      stats:
        max-health: {base: 2, scale: 0.2, spread: .1, max-spread: .2}
precyzja:                   # pojedynczy sufiks
  chance: 0.4
  weight: 2
  suffix: '&fPrecyzji'      # UWAGA: prefix i suffix naraz -> zostaje tylko suffix
  stats:
    critical-strike-chance: {base: 3, scale: 0.1, spread: .1, max-spread: .3}
prefiksy:
  max: 3
  modifiers:
    zycie: {}
sufiksy:
  max: 3
  modifiers:
    precyzja: {}
```

### 10.4 Set z bonusami 2/3/4
```yaml
# item-sets.yml
STRAZNIK_GAJU:
  name: '&2Zestaw Strażnika Gaju'
  bonuses:
    '2':
      max-health: 4
    '3':
      attack-damage: 2
      potion-speed: 1
    '4':
      ability-1: {type: LIFE_ENDER, cooldown: 30, mode: ON_HIT}
      particle-aura: {type: AURA, particle: HAPPY_VILLAGER}
      granted-permissions: ['zestaw.straznik.4']
  lore-tag:
  - '&2Zestaw Strażnika Gaju'
  - '&8[2] &7+4 Życia'
  - '&8[3] &7+2 Obrażeń, Szybkość I'
  - '&8[4] &7Pożeracz Życia'
# na przedmiocie:  set: STRAZNIK_GAJU   (bonusy są kumulatywne: przy 4 działają 2+3+4)
```

### 10.5 Upgrade-template jako quality
```yaml
# upgrade-templates.yml
jakosc:                         # 20 poziomów × 0.9158% (składane) = +20.0%
  attack-damage: 0.9158%
  max-health: 0.9158%
  armor: 0.9158%
  critical-strike-chance: 0.9158%
# config.yml -> item-upgrading.name-suffix: ' &8[&bJakość #lvl#%&8]'   (globalne dla wszystkich ulepszeń)
# na przedmiocie: upgrade: {template: jakosc, max: 20, success: 100, reference: jakosc}
# item/consumable.yml
KAMIEN_JAKOSCI:
  base:
    material: AMETHYST_SHARD
    name: '&bKamień jakości'
    upgrade: {reference: jakosc, success: 100}
    inedible: true
    lore: ['&7Przeciągnij na przedmiot: +1% jakości']
```

---

## 11. Luki i obejścia

| Luka | Obejście bez kodu | Mały plugin (API MMOItems) |
|---|---|---|
| Twardy „min ilvl” affiksu | `weight` względem `capacity(ilvl)` tieru, `spread` pojemności 0, kolejność w pliku od najcięższych (greedy). Albo osobne szablony-bazy na przedziały ilvl (np. `…_T1` od 60) wybierane przez tabelę dropu | Listener `MMOItemReforgeEvent`/własny generator: odrzuca węzły ponad ilvl |
| Drop na ziemię z ilvl = poziom moba/strefy | MM `~onDeath`: `command{c="mi generate <trigger.name> -type:X -id:Y -level:<caster.level> -tier:Z"}` (do eq). Albo tabele MMOCore (skrzynie) | Mechanika MM `mmoigen{type;id;level;tier}` → `template.newBuilder(level, tier).build()` + `world.dropItem`. ~50 linii |
| Losowa jakość na dropie | brak | Po wygenerowaniu `mmoitem.getUpgradeTemplate().upgradeTo(mmoitem, rand(0..N))` (API przetestowane w `MITest`) |
| Reroll pojedynczego przedmiotu (orb) | Przepis stacji: przedmiot → ten sam szablon (nowy rzut, ilvl z gracza). Albo globalny RevID | Consumable → `new MMOItemReforger(item).reforge(ReforgeOptions{reroll, keepTier…})` (zachowuje ilvl) |
| Prefiks I sufiks z jednego affiksu | Dwa sprzężone węzły: rodzic z `prefix` + dziecko `{min:1}` z `suffix` | – |
| Stała kolejność sufiksów w nazwie | `priority`, żeby pokazywał się tylko jeden (najważniejszy) | – |
| Ilvl w lore | custom stat `custom-ilvl` [TEST ✔] | – |
| Aktywne progi setu w lore | Statyczny `lore-tag` | PAPI/lore dynamiczne (niska wartość) |
| Mnożniki statystyk tieru | Grupa `min1/max1` z `stats.tier` (docs) | – |
| „% rzutu” jak Wynn (jak dobry roll) | Brak (lore pokazuje tylko wartość) | Event `GenerateLoreEvent` → dopisać procent z `NumericStatFormula` |

---

## 12. Rekomendacja (skrót)
1. Rarity = tiery z własną pojemnością bez spreadu, a rarity w nazwie przez `<tier-color>`.
2. Affiksy = publiczne grupy `prefiksy{max N}` / `sufiksy{max N}` z rodzinami `max: 1`. Tier affiksu zakodowany wagą; wartości ze `scale`.
3. Sety natywne: progi 2/3/4, akcesoria MMOInventory się liczą.
4. Quality = upgrade `jakosc` (max 20, 0,9158%/poz.) + Kamień jakości. Losowa jakość na dropie tylko pluginem.
5. Drop z poziomem: na teraz `mi generate … -level:<caster.level>` z `~onDeath`. Docelowo mała mechanika MM, która dropi na ziemię.
6. Ilvl w lore przez custom stat. Naprawić LUCKY_CHARM `tier: NORMAL` i usunąć martwy `parent:` z tierów.
