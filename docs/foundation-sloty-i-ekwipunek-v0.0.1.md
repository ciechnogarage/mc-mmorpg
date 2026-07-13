# Sloty i ekwipunek foundation v0.0.1

Ten dokument zbiera w jednym miejscu:

1. docelowy finalny ekwipunek slot po slocie,
2. co jest juz obslugiwane natywnie przez obecny stack,
3. co trzeba jeszcze dorobic pluginowo albo konfiguracyjnie.

## 1. Docelowy finalny ekwipunek

Docelowy foundation layout:

- Main Hand
- Off Hand
- Helmet
- Chestplate
- Leggings
- Boots
- Ring 1
- Ring 2
- Amulet
- Bracelet
- Gloves
- Artifact 1
- Artifact 2
- Artifact 3

Uwagi:

- `Ring 1` i `Ring 2` to osobne sloty, nie jeden wspolny slot ring.
- `Artifact 1-3` pokrywaja warstwe trinket/charm/relic utility bez wciskania
  wszystkiego w jeden worek.
- `Belt` nie wystepuje obecnie jako realny slot w konfiguracji pluginow i na tym
  etapie nie jest czescia wspieranego foundation layoutu.

## 2. Co obsluzy stack natywnie

### MMOInventory

Z aktualnej konfiguracji `MMOInventory` mamy juz natywnie sloty:

- `helmet`
- `chestplate`
- `leggings`
- `boots`
- `off_hand`
- `elytra`
- `ring1`
- `ring2`
- `amulet`
- `bracelet`
- `gloves`
- `artifact1`
- `artifact2`
- `artifact3`

Wniosek:

- docelowy RPG ekwipunek jest w wiekszosci juz wsparty przez `MMOInventory`,
- nie trzeba wymyslac od zera 2 ringow ani amuletu,
- warstwa dodatkowych akcesoriow jest juz obecna w pluginie.

### MMOItems

Z aktualnego `item-types.yml` i katalogu `plugins/MMOItems/item/` mamy typy,
ktore naturalnie wpadaja do tych slotow:

#### Main Hand

- `SWORD`
- `LONG_SWORD`
- `THRUSTING_SWORD`
- `KATANA`
- `AXE`
- `GREATAXE`
- `HAMMER`
- `GREATHAMMER`
- `DAGGER`
- `SPEAR`
- `LANCE`
- `HALBERD`
- `BOW`
- `CROSSBOW`
- `MUSKET`
- `STAFF`
- `GREATSTAFF`
- `WAND`
- `GAUNTLET`
- `WHIP`
- `TOOL`

#### Off Hand

- `SHIELD`
- `CATALYST`
- `OFF_CATALYST`

#### Armor

W praktyce przez typ `ARMOR`:

- Helmet
- Chestplate
- Leggings
- Boots

#### Akcesoria i utility

- `RING`
- `TALISMAN`
- `ORNAMENT`
- `TOME`

#### Inne wspierane typy

- `CONSUMABLE`
- `MATERIAL`
- `GEM_STONE`
- `MISCELLANEOUS`
- `BLOCK`

## 3. Mapowanie slot -> dozwolony ekwipunek

### Main Hand

Slot glowny na bron:

- miecze,
- topory,
- mloty,
- sztylety,
- bronie drzewcowe,
- luki,
- kusze,
- muszkiety,
- rozdzki,
- kostury,
- egzotyki typu `gauntlet` i `whip`.

### Off Hand

Slot pomocniczy:

- tarcza,
- katalizator,
- fokus/off-catalyst,
- w kolejnych iteracjach mozna tu dopuszczac wybrane utility itemy albo mala
  bron zapasowa, ale nie jest to jeszcze twardo rozpisane.

### Armor

- Helmet -> element typu `ARMOR`
- Chestplate -> element typu `ARMOR`
- Leggings -> element typu `ARMOR`
- Boots -> element typu `ARMOR`

Typ obrony definiujemy systemowo przez baze itemu:

- ciezki,
- lekki,
- szata,
- docelowo sredni.

### Ring 1 / Ring 2

- przedmioty typu `RING`

### Amulet

Stan obecny:

- slot istnieje w `MMOInventory`,
- brak jeszcze osobnego typu `AMULET` w `MMOItems`.

Przejściowo nie nalezy wciskac amuletu do `RING` ani `TALISMAN`, jesli chcemy
czysty model systemu.

### Bracelet

- slot istnieje w `MMOInventory`,
- brak jeszcze osobnego typu `BRACELET` w `MMOItems`.

### Gloves

- slot istnieje w `MMOInventory`,
- brak jeszcze osobnego typu `GLOVES` w `MMOItems`.

### Artifact 1 / 2 / 3

Sloty istnieja w `MMOInventory`. Obecnie najblizsze typy funkcjonalne to:

- `TALISMAN`
- `ORNAMENT`
- `TOME`
- czesc utility `MISCELLANEOUS`

Docelowo trzeba zdecydowac, czy:

- tworzymy jeden typ `ARTIFACT`,
- czy kilka wyspecjalizowanych typow (`CHARM`, `RELIC`, `TOTEM`, `IDOL` itd.).

## 4. Co jest gotowe

Gotowe na poziomie plugin-stack:

- 2 ringi jako osobne sloty,
- amulet jako slot,
- bracelet jako slot,
- gloves jako slot,
- 3 artifact sloty,
- armor sloty,
- off-hand,
- szeroki katalog typow broni.

Gotowe na poziomie foundation itemization:

- neutralne foundation bazy,
- soft gate przez atrybuty,
- rozdzielenie typow broni i pancerza,
- odejscie od modelu "item tylko dla klasy X".

## 5. Czego jeszcze brakuje

### Braki typow MMOItems

Typy `AMULET`, `BRACELET`, `GLOVES` i `ARTIFACT` sa juz obecne w stacku.

Do dalszej decyzji pozostaje:

- czy `ARTIFACT` zostaje jednym typem,
- czy dzielimy go pozniej na subtypy,
- czy `BELT` ma wejsc do finalnego layoutu jako osobny slot i typ.

### Braki foundation content

- foundation `medium armor`,
- foundation `crossbow`,
- foundation `hammer/mace`,
- dalsze foundation bazy pod `amulet`,
- dalsze foundation bazy pod `bracelet`,
- dalsze foundation bazy pod `gloves`,
- dalsze foundation artifacty,
- template baz itemow,
- pule prefiksow i sufiksow per typ,
- rarity matrix i drop/source matrix.

### Braki systemowe

- realne kary za niepasujacy typ gearu,
- proficiency per klasa,
- loadout validation po calej linii,
- oddzielenie utility artifactow od zwyklych akcesoriow.

## 6. Rekomendacja implementacyjna

Najczystsza kolejność dalszej pracy:

1. zdecydowac model `ARTIFACT` vs kilka typow artifactowych,
2. rozbudowac neutralne foundation bazy dla nowych slotow,
3. dopiero potem budowac affixy, rarity i source matrix.
