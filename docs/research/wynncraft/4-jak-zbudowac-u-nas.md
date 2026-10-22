# Technika Wynncrafta przełożona na nasz serwer

Data: 2026-09-22. Kontynuacja `wynncraft-kreator-research.md` (sekcje 4 i 7).

**Zakres.** Tylko research i PoC w `poc-kod/`. Nie zmieniłem niczego w `/opt/docker/mc` ani na działających serwerach. PoC chodził na **osobnym** Paperze 1.21.11 (ten sam jar co world1: `paper-1.21.11-132`), w osobnej sieci dockera `poc-net`. Łączył się z nim prawdziwy klient vanilla 1.21.11 z obrazu `mc-client-test`. Po pomiarach oba kontenery i sieć usunąłem.

Oznaczenia:
- **[PoC]**: zmierzone przeze mnie na prawdziwym kliencie. Zrzuty są w `poc/review/captures/`.
- **[F]**: fakt z dokumentacji albo kodu, podaję URL lub plik.
- **[W]**: mój wniosek, niezweryfikowany.

---

## 0. Stan wyjściowy: co już mamy (sprawdzone na dysku, tylko odczyt)

| Rzecz | Stan na world1 | Znaczenie dla tego planu |
|---|---|---|
| Serwer | Paper **1.21.11-132** (`version_history.json`) | Są `PlayerInputEvent`, item model definitions i `oversized_in_gui` |
| Kreator postaci | **Własny plugin Java `CharacterCreator`** (`MCMMORPG/plugin-src/character-creator`), a nie DeluxeMenus. Świat `creator_limbo`, gracz w **SPECTATOR**, okno `SHULKER_BOX` z kodu, „stajnia” nosicieli skórek, `hideFromOthers`. Chowa prawdziwy plecak przez `setStorageContents(new ItemStack[36])` i zapisuje go w `sessions.yml` | Scena Wynna to **rozbudowa tego pluginu**, a nie nowy projekt. Opis w zleceniu („DeluxeMenus HOPPER”) jest nieaktualny |
| Pakiety | `packetevents-spigot-2.11.2.jar` + `ProtocolLib.jar` | PacketEvents wystarczy do okien pełnoekranowych (PoC niżej) |
| HUD | **MythicHUD 1.3.4** z `nexo-hook: true`. W `Nexo/pack/pack.zip` jest już jego core shader `rendertype_text` w overlayach `mythichud-1_21_2`, `mythichud-1_21_6` i `mythichud-26_1`. Layout `sword-layout` stoi na `x:0, y:100`, czyli w rogu | **Przypinanie do rogów już działa u nas shaderem**, nie trzeba nic dokładać |
| GUI MMOCore | `gui/skill-tree.yml` z pełną sekcją `display:` (ścieżki i węzły per stan i kształt), na razie na barwnikach i betonie | Drzewko w stylu Wynna to głównie grafika i config |
| MMOProfiles | `synced-data.permissions: true`, jest klasa `LuckPermsModule` | Flagi trybu per postać przez LuckPerms (pkt 6) |
| DeluxeMenus | 1.14.1. W bajtkodzie są `model_data_component` (z `colors`), `item_model`, `tooltip_style`, `hide_tooltip` | Tinty i item_model są dostępne z YAML-a |

---

## 1. Scena wyboru postaci w świecie

### 1.1 Pomiar [PoC]: co widzi serwer i co widzi gracz

Plugin `poc/plugin/src/poc/ScenePoc.java` (komenda `/scene a|c`) loguje każde zdarzenie wejścia. Klawisze wciskał xdotool na prawdziwym kliencie.

**Wariant A: `SPECTATOR` + `Player#setSpectatorTarget(ItemDisplay)`** (kamera na pustym ItemDisplay 1,62 bloku nad ziemią, przed kamerą encja `Interaction` 3×3):

| Wejście gracza | Co dostał serwer |
|---|---|
| ruch myszą | **nic, a kamera się nie obraca** (zrzut `a-02.png` po przesunięciu myszy o 200 px wygląda tak samo). Kamera jest w pełni zablokowana |
| LPM na Interaction | `PlayerStartSpectatingEntityEvent new=INTERACTION` (anulowany przez nas, więc kamera zostaje). **To jest nasz „lewy klik”** |
| PPM na Interaction | `PlayerInteractEntityEvent` ×2 (HAND i OFF_HAND) + `PlayerInteractAtEntityEvent` ×2. Filtrować `hand == HAND` |
| W/A/S/D, spacja | `PlayerInputEvent` z polami forward/backward/left/right/jump |
| Shift | `PlayerInputEvent shift=true` + `PlayerStopSpectatingEntityEvent` **co tick**. Anulowanie trzyma kamerę na miejscu (15 anulowań w 0,6 s) |
| **scroll** | **nic.** Klient w spectatorze zużywa scroll na prędkość lotu |
| 1–9, środkowy przycisk | nic po stronie serwera. Klient otwiera **menu spectatora** („Press a key to select a command”, `a-03.png`), a z niego da się **teleportować do graczy** |
| F5 | nic. Widok przełącza się na trzecią osobę z kamery-encji |
| HUD | **brak hotbara, serc, głodu, ręki i celownika.** Bossbar i action bar **są widoczne** (`a-10.png`) |

**Wariant C: `ADVENTURE` + zamrożenie atrybutami** (`movement_speed=0`, `jump_strength=0`):

| Wejście | Serwer |
|---|---|
| W | `PlayerInputEvent forward=true`, **zero `PlayerMoveEvent` ze zmianą pozycji**, więc zamrożenie działa |
| LPM na Interaction | `PlayerAnimationEvent ARM_SWING` + `PrePlayerAttackEntityEvent INTERACTION` |
| PPM na Interaction | `PlayerInteractEntityEvent` + `PlayerInteractAtEntityEvent` + ARM_SWING |
| scroll, 1–9 | `PlayerItemHeldEvent 0->1` itd. **Scroll działa** |
| F | `PlayerSwapHandItemsEvent` |
| Q z pustą ręką | nic (znana pułapka z MenuKey) |
| HUD | **widać hotbar, serca, głód, rękę i celownik** (`c-01.png`), a gracz może się rozglądać |

### 1.2 Rekomendacja

**Overview jak u Wynna robimy w wariancie A**: spectator, kamera na encji i `Interaction` jako „tarcza” na kliki. Co za tym przemawia:
- Znika cały HUD bez modów i bez packa, a action bar i bossbar (MythicHUD) zostają. Obecny kreator i tak stoi już w SPECTATOR w `creator_limbo`.
- Kamera jest zablokowana w 100%, a raycast idzie z kamery o stałym obrocie. **Klikalny jest więc tylko środek ekranu.** To dokładnie układ Wynna („LPM: graj, PPM: menu”). Wybór kilku obiektów celowaniem **nie wchodzi w grę**.
- **Przeglądanie robimy klawiszami A/D przez `PlayerInputEvent`, a nie scrollem.** Scroll w spectatorze nie dociera do serwera [PoC]. Podpowiedź sterowania pokazujemy glifami w action barze („A/D: postać, LPM: graj, PPM: menu”).
- Jeśli scroll jest wymagany koniecznie, zostaje wariant C. Cena to widoczny HUD i ręka: trzeba by czyścić hotbar (Typewriter robi „fake clear inventory”, patrz niżej) i żyć z sercami i celownikiem. **Nie polecam.**

**Konkretne API (Paper 1.21.11):**
- `Player#setSpectatorTarget(Entity)` wymaga trybu SPECTATOR, inaczej leci `IllegalArgumentException: Player must be in spectator mode` [PoC]. W `reset()` też trzeba to sprawdzać. https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Player.html#setSpectatorTarget(org.bukkit.entity.Entity)
- `com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent` / `PlayerStopSpectatingEntityEvent` (oba Cancellable) [PoC]. https://jd.papermc.io/paper/1.21.11/com/destroystokyo/paper/event/player/PlayerStopSpectatingEntityEvent.html
- `org.bukkit.event.player.PlayerInputEvent` + `Player#getCurrentInput()`. https://jd.papermc.io/paper/1.21.11/org/bukkit/event/player/PlayerInputEvent.html. Protokół: pakiet *Player Input*, https://minecraft.wiki/w/Java_Edition_protocol/Packets#Player_Input
- `Interaction` (`setInteractionWidth/Height`, `setResponsive`): https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Interaction.html
- Kamera to `ItemDisplay`/`TextDisplay` z `Display#setTeleportDuration(0..59)`. Teleport encji jest wtedy interpolowany po stronie klienta, więc **płynne najazdy kamery** wychodzą bez pakietów [F] https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Display.html#setTeleportDuration(int). **[W]** Nie mierzyłem, czy kamera w spectatorze podąża za interpolacją. Typewriter robi to samo na pakietowym TextDisplay (`positionRotationInterpolationDuration = 10`), więc to działa.
- Zamrożenie (wariant C): `Attribute.MOVEMENT_SPEED` / `Attribute.JUMP_STRENGTH` baseValue 0 [PoC].

**Pułapki (wszystkie do obsłużenia w pluginie):**
1. **Menu spectatora (1–9) pozwala teleportować się do graczy.** Blokować `PlayerTeleportEvent` z `cause == SPECTATE`. Sprawdzić, czy obecna blokada teleportów w `CreatorSessionManager` obejmuje tę przyczynę.
2. **Spectatorzy widzą siebie nawzajem** (półprzezroczyste głowy). Dwóch graczy w tej samej scenie trzeba rozdzielić przez `hidePlayer` w obie strony (Typewriter robi dokładnie to, `playerHides.hide(...)`).
3. Shift ma zostać **anulowany** w `PlayerStopSpectatingEntityEvent`. Bez tego gracz wypada z kamery i lata po sali.
4. PPM daje zdarzenie **dwa razy** (obie ręce).
5. `setSpectatorTarget` wymaga, żeby encja była wczytana i śledzona przez klienta. W PoC działało dopiero z opóźnieniem 5 ticków po zmianie trybu i teleporcie.
6. Esc otwiera menu pauzy (serwer go nie widzi). Klient nie wysyła wtedy wejścia, a w trakcie mogą przyjść nasze pakiety.
7. Kamera jest w pełni zablokowana, więc gracz nie „rozgląda się”. Nie ma problemu, jeśli scena jest zakomponowana pod jeden kadr, jak u Wynna.

### 1.3 Instancjonowanie per gracz

- **Osobny świat już mamy** (`creator_limbo`, `keep-loaded`, `view-distance: 4`). Wszystkie scenerie (odpowiednik 15 teł Wynna) stawiamy w **tym samym świecie**, w różnych współrzędnych. Zmiana świata zawsze pokazuje „Loading terrain” (odnotowane już w configu kreatora).
- **Rekwizyty per gracz**: `Entity#setVisibleByDefault(false)` + `Player#showEntity(plugin, e)`. Encja istnieje na serwerze, ale widzi ją jeden gracz. Tak działa już `hideFromOthers` dla nosicieli. https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Entity.html#setVisibleByDefault(boolean)
- Jest też alternatywa czysto pakietowa, jak w Typewriterze: encje z PacketEvents (`WrapperEntity`) plus pakiet *Set Camera* (`WrapperPlayServerCamera`) na **pakietowy** TextDisplay. Gracz zostaje wtedy w swoim trybie gry, niewidzialny (`INVISIBILITY`), z fałszywym lotem i fałszywie wyczyszczonym ekwipunkiem. Kod: `github.com/gabber235/Typewriter`, pliki `extensions/BasicExtension/.../cinematic/CameraCinematicEntry.kt` i `engine/engine-paper/.../packetevents/PlayerPackets.kt` (`spectateEntity = WrapperPlayServerCamera(entityId)`). Komentarz autora: *„While spectating, the player input packet is the only input the server still receives.”* **Dla nas to droższe i niepotrzebne**: spectator robi to samo bez fałszowania stanu, a na scenę nie chcemy graczy w survivalu.
- Gotowego, otwartego pluginu „CharacterSelect / SimpleCharacters” ze sceną w świecie **nie znalazłem**. Wyszukiwanie dało tylko pluginy cutscenek: Typewriter (najlepiej udokumentowany kod, PacketEvents), `BattleDash/BlendMC`, `Stepan1411/Cutscene-engine`, `nonxedy/nonscenes`. Te ostatnie nie dokumentują techniki kamery. Najbliższym wzorcem technicznym jest Typewriter.

### 1.4 Przejścia i scenerie

- **Fade**: `Player#showTitle(Title.title(glif, empty, Title.Times.times(fadeIn, stay, fadeOut)))`, gdzie glif to czarny kwadrat pełnoekranowy (font `minecraft:default`, duże `height`, `ascent` dobrany tak, żeby środek wypadł na środku ekranu). Teleport kamery robimy w połowie `stay`. Action bar i bossbar działają w spectatorze [PoC]. Tytuł rysuje się w tej samej warstwie HUD **[W]**, więc na pierwszym wdrożeniu trzeba to potwierdzić zrzutem. **Pamiętać o `&f` / `<white>` przed glifem** (nasza pułapka tintu).
- **Przełączanie scenerii**: teleport `ItemDisplay` z `teleportDuration` wewnątrz jednej scenerii daje najazd. Między sceneriami: fade, teleport kamery, ponowne `setSpectatorTarget`, fade-out.

**Koszt:** M (3–5 dni w istniejącym `CharacterCreator`): stan sceny, mapowanie wejścia, HUD w action barze, 2–3 scenerie. **Ryzyko:** niskie–średnie. Mechanika jest potwierdzona na prawdziwym kliencie. Ryzyko siedzi w detalach, czyli w pułapkach 1–6.

---

## 2. HUD z glifów i przypinanie do rogów

- **Bez core shaderów do rogów się nie da.** Action bar, bossbar i tytuł są zawsze **wyśrodkowane**, a ujemne spacje przesuwają o stałe piksele GUI, niezależnie od szerokości okna. Bez shadera da się zrobić tylko elementy wokół środka (dół-środek, góra-środek), co Wynn też robi w `bottom_middle`.
- **U nas shader już jest.** MythicHUD dokłada `assets/minecraft/shaders/core/rendertype_text.vsh` w overlayach packa (wersje 1.21.2 / 1.21.6 / 26.1). Mechanizm czytałem w `mythichud_utils.glsl`: tekst z niewidzialnego bossbara ma zakodowany ID w ogromnym `ascent`. Shader odczytuje go z `Position.y` i przesuwa wierzchołki do `position` (0–100% ekranu) plus `offset` w px, z poprawką na `guiScale`. Dokumentacja: *„uses values from 0 to 100 to represent the location percentage”*, *„Layouts are displayed to the player on an invisible Boss Bar, and can be toggled with the /hud layout commands”* https://wiki.mythiccraft.io/mythichud/Layouts , komendy https://wiki.mythiccraft.io/mythichud/Commands
- **Karta postaci w scenie (prawa strona, jak u Wynna)** to layout MythicHUD z placeholderami PAPI (poziom, klasa, rasa), włączany `/hud layout` na czas sceny. Ikony trybów, cyfry i ikony myszy to glify Nexo.
- **Ryzyko Iris/Sodium:**
  - Iris z **włączonym shaderpackiem** ignoruje core shadery z resource packów: *„Custom shaders added by mods or resource packs are ignored by Iris when an Iris shader pack is loaded.”* https://github.com/IrisShaders/Iris/blob/26.1/docs/development/compatibility/core-shaders.md . **[W]** Z kodu shadera wynika, że bez niego tekst layoutu ląduje daleko poza ekranem, bo ascent jest wielkości tysięcy px. HUD po prostu **znika**, zamiast się rozjechać.
  - Sam Sodium nie podmienia shadera tekstu **[W]**.
  - Wniosek: informacje krytyczne (HP/mana) trzymać **także** w dół-środku, a rogi traktować jako bonus.
- Alternatywy: BetterHUD (open source, `github.com/toxicity188/BetterHud`, ta sama technika bossbar+shader, własny generator packa) i HappyHUD (płatny). **Nie ma powodu zmieniać.** MythicHUD jest opłacony, zmergowany z Nexo i już stoi.

**Koszt:** S (layout karty postaci + glify). **Ryzyko:** niskie. Iris z shaderpackiem ukrywa rogi.

---

## 3. Okna pełnoekranowe z fałszywymi przedmiotami w części ekwipunku

### 3.1 Pomiar [PoC]

Plugin `poc/plugin2/src/poc/FullscreenPoc.java` (PacketEvents 2.11.2, ten sam jar co world1) otwiera skrzynię 6×9 i podmienia w pakietach sloty surowe **54–89** (plecak + hotbar):
- `PacketType.Play.Server.OPEN_WINDOW`: zapamiętujemy `containerId`.
- `WINDOW_ITEMS` (Set Container Content) dla tego id: `items[54..]` zamieniamy na fałszywe przyciski, `markForReEncode(true)`.
- `SET_SLOT` dla tego id i `slot >= 54`: podmiana.
- `InventoryClickEvent`: `setCancelled(true)` dla każdego kliku, gdy górne okno jest nasze, a następnie `updateInventory()`.
- `InventoryCloseEvent`: `player.updateInventory()` odsyła prawdziwy stan.

Wynik:
- `fs-01.png`: prawdziwe diamenty (slot 9) i kamień (hotbar 0) **zniknęły**, widać fałszywe szyby.
- Każdy typ kliknięcia dotarł do serwera jako raw slot 54–89: LEFT, SHIFT_LEFT, NUMBER_KEY, DOUBLE_CLICK, DROP. Wszystkie anulowane, nic się nie ruszyło.
- `fs-05.png`: po zamknięciu **prawdziwy ekwipunek wrócił** (diamenty i kamień na miejscu).
- Stan serwera **nie jest ruszany ani przez chwilę**. To przewaga nad obecnym kreatorem, który naprawdę czyści plecak i ratuje go przez `sessions.yml`.

### 3.2 Pułapki

1. **Po stronie klienta dolna część okna to ten sam obiekt co ekwipunek gracza.** Fałszywe przedmioty wchodzą też do hotbara w HUD (widać zielone szyby pod oknem w `fs-01.png`). Dopóki okno jest otwarte, jest OK. Po zamknięciu **obowiązkowo** `updateInventory()` [PoC].
2. **Tryb kreatywny** wysyła sloty autorytatywnie i fałszywe przedmioty mogłyby trafić do prawdziwego ekwipunku. Typewriter ma na to wprost komentarz i w kreatywnym nie fałszuje. U nas: nie otwierać takich okien w CREATIVE albo najpierw przełączać tryb.
3. **Pakiet *Set Player Inventory Slot*** (`SET_PLAYER_INVENTORY`, https://minecraft.wiki/w/Java_Edition_protocol/Packets#Set_Player_Inventory_Slot) omija filtr po window id. W wersji produkcyjnej też go przechwytywać albo odkładać.
4. **MMOInventory (tryb vanilla, akcesoria w slotach 9–17)**: serwerowo nic się nie dzieje, bo kliki są anulowane, a dane zostają. Po zamknięciu `updateInventory()` przywraca ikony. **[W]** Jeśli MMOInventory odświeża sloty własnym pakietem w trakcie, pójdzie przez `SET_SLOT` z id kontenera i filtr go złapie. Do potwierdzenia jednym testem na world1.
5. **MMOProfiles**: zmiana profilu z otwartym takim oknem jest zakazana, bo MMOProfiles nakłada ekwipunek profilu. Przed `ProfileUnloadEvent` / wyborem profilu trzeba zamknąć okno.
6. `stateId` pakietów bierzemy z oryginału. PacketEvents robi to samo przy re-encode.

### 3.3 Biblioteki

- **InvUI v2** robi dokładnie to jako funkcję: *„Every window encompasses both the upper- and the lower inventory”*, *„Window items inside the player inventory cannot be accidentally saved to the world and end up in the actual player's inventory.”* https://docs.xenondevs.xyz/invui2/window/ . **Uwaga na wersje** (maven-metadata + GitHub releases):
  - `2.0.0-beta.*` i nowsze to **Java 25 / MC 26.x**,
  - ostatnia na Javę 21 to **`2.0.0-alpha.26`** (MC 1.21.11, API alfa, `repo.xenondevs.xyz/releases`),
  - `1.49` (seria v1) ma wsparcie „1.21.11”, ale nie sprawdzałem, jak v1 obsługuje dolny ekwipunek.
- Triumph GUI i DeluxeMenus: tylko górne okno. **DeluxeMenus tego nie zrobi.**
- **Rekomendacja:** własny, mały `FakeLowerInventory` na PacketEvents (≈150 linii, PoC jest gotowym szkieletem) w `CharacterCreator`, a później we wspólnej bibliotece. Na 1.21.11 nie wiązałbym się z alfą InvUI. Po przejściu na 26.x InvUI v2 staje się naturalnym wyborem.

**Koszt:** M (2–3 dni na produkcyjny filtr + podpięcie pod kreator i Character Info). **Ryzyko:** średnie (pułapki 2–5), ale niższe niż obecne fizyczne czyszczenie plecaka.

---

## 4. Przyciski wieloslotowe i tinty (Nexo + DeluxeMenus, 1.21.4+)

### 4.1 Przycisk na kilka slotów

- W modelu (`models/...json`) ustawiamy `display.gui.scale` / `translation`, np. przycisk 3×1 z pikselami 48×16 na teksturze.
- **Pułapka od 1.21.6: bez `oversized_in_gui: true` klient przycina model do 16×16 slotu.** *„If false the render will be clipped to the item slot size.”* (od 1.21.6-pre1) https://minecraft.wiki/w/Items_model_definition . Nexo: `Pack.oversized_in_gui: true` + `Components.item_model`. W Nexo 1.17+ jest też globalne `Pack.generation.force_oversized_in_gui` (u nas `settings.yml:46` = `false`) https://docs.nexomc.com/configuration/items/item-appearance.md (w `llms-full.txt`: „Oversized Items in GUI (1.21.6+)”).
- Nasze ręczne definicje `Nexo/pack/assets/cc/items/*.json` **nie mają** tego pola, więc przy skalowaniu trzeba je dopisać na poziomie korzenia pliku.
- Pozostałe sloty przycisku: przedmiot z `item_model: nexo:empty` (Nexo ma go wbudowanego, `assets/nexo/items/empty.json` jest w pack.zip) i **ta sama akcja** w DeluxeMenus. Podświetlenie po najechaniu obejmuje tylko jeden slot. U nas `slot_highlight` jest celowo przezroczysty, więc to nie przeszkadza.

Przykład Nexo:
```yaml
btn_graj_3x1:
  itemname: <white>Graj
  material: PAPER
  excludeFromInventory: true
  excludeFromCommands: true
  Pack:
    model: nexo:gui/btn_graj_3x1      # model z display.gui.scale [3,1,1] + translation
    oversized_in_gui: true
  Components:
    item_model: nexo:btn_graj_3x1
```
Model `assets/nexo/models/gui/btn_graj_3x1.json` (fragment):
```json
{ "parent": "minecraft:item/generated",
  "textures": { "layer0": "nexo:gui/btn_graj_3x1" },
  "display": { "gui": { "scale": [3, 1, 1], "translation": [16, 0, 0] } } }
```
**[W]** `translation` jest w jednostkach modelu, a skala nie może przekroczyć 4 na osi. Wartości trzeba dostroić zrzutem, bo liczenie pozycji według notatki `nexo-pack-trzy-pulapki` dotyczy glifów, nie modeli.

DeluxeMenus:
```yaml
items:
  graj:
    material: nexo-btn_graj_3x1
    slots: [21]
    left_click_commands: ['[console] cc play %player_name%']
  graj_reszta:
    material: nexo-empty            # albo item z item_model nexo:empty
    slots: [22, 23]
    left_click_commands: ['[console] cc play %player_name%']
```

### 4.2 Tinty (jeden model, N kolorów, np. ikony postaci w 9 kolorach)

Definicja przedmiotu (ręcznie w `Nexo/pack/assets/cc/items/ikona_postaci.json`), https://minecraft.wiki/w/Items_model_definition :
```json
{ "oversized_in_gui": false,
  "model": { "type": "minecraft:model", "model": "cc:item/ikona_postaci",
    "tints": [ { "type": "minecraft:custom_model_data", "index": 0, "default": 16777215 } ] } }
```
Elementy modelu, które mają się barwić, muszą mieć `"tintindex": 0` (Nexo docs: „Tint” w Blockbench).

DeluxeMenus 1.14.1 (klucz `model_data_component` jest w jarze, dokumentacja: https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/item):
```yaml
    material: paper
    item_model: 'cc:ikona_postaci'
    model_data_component:
      colors: ['200, 60, 60']        # tint index 0
      strings: ['wojownik']          # opcjonalnie do minecraft:select
```
- **Warianty stanu** (wybrany, zablokowany): `minecraft:select` po `custom_model_data` strings albo `range_dispatch` po floats. To zastępuje wynnowe „custom_model_data floats”.
- **Alternatywa Wynna**: mikstura z `potion_contents.custom_color` (`tints: [{type: minecraft:potion}]`). W DeluxeMenus to klucz `rgb:`, który dla mikstury wymaga co najmniej jednego efektu. Droga przez `custom_model_data colors` jest czystsza.
- Animacje: `.png.mcmeta` w teksturze Nexo działa jak zwykle.

**Koszt:** S. **Ryzyko:** niskie, pod warunkiem pamiętania o `oversized_in_gui`.

---

## 5. Drzewko umiejętności MMOCore w stylu Wynna

Źródło: https://docs.phoenixdevt.fr/mmocore/features/skill-trees plus bajtkod MMOCore 1.13.1 (`skilltree/display/*`) i MythicLib (`IconOptions`).

Co daje config:
- **Typy drzewek**: `custom` (relacje ręczne) i `proximity` (sąsiedzi są soft parents). Węzeł ma `coordinates: x,y` (dowolne, z przewijaniem strzałkami `up/down/left/right` w slotach 48–51).
- **Relacje** (`parents.strong/soft/incompatible`, `max-children`) wystarczają do archetypów Wynna. Wybór jednej z gałęzi robi `max-children: 1` albo `incompatible`.
- **Ścieżki**: `children.strong.<id>.paths: ['1 0','2 0']`. *„Each node pair requires one path; paths cannot split”*, czyli **ścieżka nie ma rozgałęzień typu T ani krzyż**. Rozgałęzienia robi się **na węzłach**.
- **Kształty z bajtkodu:**
  - `PathShape` = UP, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, DEFAULT (7 kształtów) × `PathState` unlocked/unlockable/locked/fully-locked, czyli **28 modeli ścieżek**,
  - `NodeShape` = 16 kombinacji (no-path … up-right-down-left) × stany unlocked/**maxed-out**/unlockable/locked/fully-locked, czyli **80 ramek węzłów**.
- **Klucze ikony (MythicLib `IconOptions`, z bajtkodu):** `item`/`material`, `item_model` (`item-model`), `custom_model_data`, `custom_model_data_float`, `custom_model_data_string`, `tooltip_style`, `hide_tooltip`, `item_flags`, `skull_texture`. **Nie ma `colors`**, więc tintów z CMD tu nie ustawimy. Stan koloru trzeba zakodować w `item_model` albo w `custom_model_data_string` + `select`.
- **Ikona per węzeł** (`nodes.<id>.display.<stan>`) nadpisuje kształt. Priorytet: węzeł → plik drzewka → `gui/skill-tree.yml`. **Pułapka [W]:** własna ikona węzła gubi łączniki w samej komórce. Łączniki niosą więc sąsiednie komórki ścieżek, a węzeł to „ikona w ramce”. Albo robimy w item model definition `composite` (ramka wg kształtu + ikona) i wybieramy przez `select` po stringu.

Minimalny zestaw grafiki: 28 ścieżek + 16 ramek węzłów w 5 stanach. Stany można robić **jedną teksturą + podmianą palety w generatorze** (tak jak `tools/menu-hub-gui/generate_*.py`), czyli 44 modele bazowe i warianty kolorów generowane.

Przykład configu (fragment `gui/skill-tree.yml`):
```yaml
name: '&f<glif tła drzewka>&8{skill-tree-name}'   # &f PRZED glifem!
display:
  paths:
    unlocked:
      up:       { item: PAPER, item_model: 'cc:tree/path_v_on' }
      right:    { item: PAPER, item_model: 'cc:tree/path_h_on' }
      up-right: { item: PAPER, item_model: 'cc:tree/path_ur_on' }
      default:  { item: PAPER, item_model: 'cc:tree/path_v_on' }
  nodes:
    unlockable:
      up-down:  { item: PAPER, item_model: 'cc:tree/node_ud_avail' }
```
**[W]** Dokumentacja pokazuje tylko skrót `"WHITE_DYE:0"` i sekcje z `item`/`custom-model-data`. Klucz `item_model` w podsekcji `display` wynika z tego, że `DisplayMap` woła `IconOptions`. Przed zrobieniem 108 grafik sprawdzić jednym węzłem.

**Ograniczenie wobec Wynna:** GUI MMOCore to maksymalnie 54 sloty, bez części ekwipunku, więc nie będzie pełnoekranowe jak Ability Tree Wynna (sloty 57–86). Da się to obejść tylko pkt 3 (fałszywy dolny ekwipunek na nasłuchu okna o tytule drzewka), czyli za cenę kodu.

**Koszt:** M (config S, grafika M–L). **Ryzyko:** niskie technicznie, duży wolumen grafiki.

---

## 6. Tryby Hardcore / Ironman

**Gdzie trzymać flagę.** Flaga trybu to węzeł LuckPerms na **postaci**:
- MMOProfiles w trybie niepoproxowym gra profilem pod jego UUID (per notatka: zmienne CoreTools „idą na UUID profilu”), więc uprawnienia LP nadane w grze są per postać. Tak już działa `race.*` z CharacterCreatora. `LuckPermsModule` (bajtkod) ładuje osobno użytkownika LP po `ProfileListImpl.getUniqueId()`, czyli po prawdziwym koncie, na potrzeby limitów slotów.
- **Odblokowanie trybu na koncie** (lvl 30 / 50 jak u Wynna): węzeł na **prawdziwym UUID** (`%mmoprofiles_real_uuid%`, jest w `PlaceholderEnum`) nadawany przy `PlayerLevelUpEvent` MMOCore.
- **Zawsze `isPermissionSet`, nie `hasPermission`**, bo właściciel ma OP.

Egzekwowanie (mały moduł w `CharacterCreator`, bo on już słucha MMOProfiles):

| Tryb | Zasada | Czym |
|---|---|---|
| Hardcore | pierwsza śmierć = koniec trybu (Wynn: ikona szara, wypada z rankingów) | `PlayerDeathEvent` → gdy `isPermissionSet("mmorpg.mode.hardcore")`: `unset` + `set mmorpg.mode.hardcore.fallen`, komunikat globalny. Ikonę na liście postaci już dopisuje `ProfileListHeadsListener`: szara ramka lub tint |
| Hardcore „permadeath” (wariant ostrzejszy) | postaci nie da się wybrać | `ProfileSelectEvent` **nie jest Cancellable** (bajtkod). Blokować klik w liście (to nasze wejście w cudze okno) + fallback: po `ProfileSelectEvent` natychmiast `mmoprofiles open <gracz>`. Usuwanie przez `ProfileRemoveEvent` (Cancellable, `PendingProfileEvent`) zostawić graczowi |
| Ironman | bez handlu, przelewów, banku gildii i cudzych dropów | negatywne węzły LP na postaci: komendy przelewu CMI (np. `cmi.command.pay`/`cmi.command.money.pay`, **nazwy sprawdzić w docs CMI**), `/cmi mail` z przedmiotami, vault Guilds. Kod: `EntityPickupItemEvent` → anuluj, gdy `Item#getThrower()` ≠ gracz; `InventoryOpenEvent` dla skrzyń innych graczy. **Explicit `false` bije OP w Bukkit**, więc negacje działają też na koncie z OP |
| Ultimate Ironman | + bez ender chesta | `ender-chest` jest już per profil (`synced-data`). Wystarczy zablokować `InventoryOpenEvent` typu ENDER_CHEST |
| Craftsman | tylko przedmioty craftowane | wymaga znacznika twórcy na MMOItems. **[W]** MMOItems nie ma takiej flagi out-of-the-box, więc to kod (PDC przy craftingu stacji) plus warunek przy zakładaniu. Odłożyć |

Wybór trybu w kreatorze: nowy krok w `CreatorMenu` (przełączniki = przedmioty z tintem z pkt 4). Nadanie flagi idzie w `ConfirmService` w tym samym miejscu co `race.*`.

**Koszt:** wybór S, Hardcore S–M, Ironman M. **Ryzyko:** średnie. Każda luka (nowy plugin z handlem) cicho łamie Ironmana, więc potrzebna lista kontrolna przy dodawaniu pluginów.

---

## 7. Tabela zbiorcza

| Element | Rekomendowana technika | Narzędzie / plugin | Koszt | Ryzyko | Źródło |
|---|---|---|---|---|---|
| Kamera sceny wyboru | SPECTATOR + `setSpectatorTarget(ItemDisplay)`, anulowanie `PlayerStopSpectatingEntityEvent` | Paper API, `CharacterCreator` | M | niskie [PoC] | jd.papermc.io Player#setSpectatorTarget; `poc/ScenePoc.java`, `a-02.png` |
| Ukrycie hotbara, ręki, celownika | wynika ze spectatora, bez packa i modów | vanilla | 0 | niskie [PoC] | `a-02.png`, `a-10.png` |
| LPM / PPM w scenie | `Interaction` przed kamerą: LPM = `PlayerStartSpectatingEntityEvent` (anuluj), PPM = `PlayerInteractEntityEvent` (hand==HAND) | Paper API | S | niskie [PoC] | log PoC |
| Przeglądanie postaci | A/D przez `PlayerInputEvent` (scroll nie działa w spectatorze) | Paper API 1.21.2+ | S | niskie [PoC] | jd.papermc.io PlayerInputEvent |
| Blokada wyjścia i teleportu | anuluj `PlayerStopSpectatingEntityEvent` i `PlayerTeleportEvent` cause SPECTATE | Paper API | S | średnie (menu 1–9) | PoC `a-03.png` |
| Scena per gracz | świat `creator_limbo` + `setVisibleByDefault(false)`/`showEntity` + `hidePlayer` między graczami | Paper API | S | niskie | jd.papermc.io Entity#setVisibleByDefault; Typewriter `playerHides` |
| Najazdy kamery | `Display#setTeleportDuration` na encji-kamerze | Paper API | S | niskie [W] | jd.papermc.io Display; Typewriter `DisplayCameraAction` |
| Fade i zmiana scenerii | tytuł z pełnoekranowym czarnym glifem (`&f` przed), teleport w `stay` | Nexo glif + Adventure Title | S | niskie | notatka `glif-panelu-tint-tytulu` |
| Karta postaci na scenie | layout MythicHUD z PAPI po prawej (`x:~80`) | MythicHUD (już jest) | S | niskie; znika z Iris+shaderpack | wiki.mythiccraft.io/mythichud/Layouts |
| HUD w rogach | core shader `rendertype_text` MythicHUD (już w pack.zip) | MythicHUD | 0–S | Iris+shaderpack go ignoruje | github.com/IrisShaders/Iris …/core-shaders.md |
| HUD bez shadera | tylko dół-środek i góra-środek (action bar, bossbar) | glify Nexo | S | niskie | — |
| Okno pełnoekranowe | pakiety WINDOW_ITEMS/SET_SLOT dla slotów 54–89, anulowanie klików, `updateInventory()` przy zamknięciu | PacketEvents 2.11.2 (jest) | M | średnie (creative, SET_PLAYER_INVENTORY, MMOProfiles) | `poc/FullscreenPoc.java`, `fs-01/05.png` |
| Biblioteka GUI z dolnym ekwipunkiem | InvUI v2 (na 1.21.11 tylko `2.0.0-alpha.26`, Java 21) | InvUI | M | API alfa | docs.xenondevs.xyz/invui2/window |
| Przycisk wieloslotowy | `display.gui.scale` + **`oversized_in_gui: true`** + reszta slotów `nexo:empty` z tą samą akcją | Nexo `Pack.oversized_in_gui`, DeluxeMenus | S | niskie | minecraft.wiki Items_model_definition; docs.nexomc.com |
| Tinty przycisków i ikon | `tints: custom_model_data index 0` + DeluxeMenus `model_data_component.colors` | Nexo pack (ręczny JSON) + DM 1.14.1 | S | niskie | minecraft.wiki; wiki.helpch.at DeluxeMenus item |
| Warianty stanu przycisku | `minecraft:select` po `custom_model_data` strings | item model definition | S | niskie | minecraft.wiki |
| Drzewko umiejętności | `gui/skill-tree.yml` `display:` z `item_model` per kształt i stan (28 ścieżek + 16×5 węzłów) | MMOCore + Nexo | M (grafika) | niskie; brak fullscreen | docs.phoenixdevt.fr/mmocore/features/skill-trees; bajtkod PathShape/NodeShape |
| Wybór trybu przy tworzeniu | krok w `CreatorMenu`, flaga LP w `ConfirmService` | CharacterCreator + LuckPerms | S | niskie | — |
| Hardcore | `PlayerDeathEvent` + `isPermissionSet`, zmiana flagi, szara ikona | CharacterCreator | S–M | niskie | docs.phoenixdevt.fr/mmoprofiles/api/events |
| Ironman | negatywne węzły LP + `EntityPickupItemEvent` (thrower) + blokada ender/cudzych skrzyń | LuckPerms + kod | M | średnie (luki w nowych pluginach) | — |
| Odblokowanie trybów na koncie | węzeł na prawdziwym UUID przy `PlayerLevelUpEvent` | MMOCore API + LP | S | niskie | placeholder `mmoprofiles_real_uuid` |

Koszt: S ≤ 1 dzień, M 2–5 dni, L > 1 tydzień (jedna osoba, bez grafiki, o ile nie zaznaczono inaczej).

---

## 8. Plan wdrożenia (kolejność)

1. **Tinty i `oversized_in_gui` w packu** (pkt 4): jeden przycisk 3×1 i jedna ikona postaci w kilku kolorach w menu-hubie. Weryfikacja klientem QA. Tanie i odblokowuje wszystko dalej.
2. **Glify HUD dla sceny**: cyfry, ikony klas, ikony myszy i klawiszy A/D, czarny glif fade (Nexo, `minecraft:default`, `&f` przed).
3. **Scena overview w `CharacterCreator`** (pkt 1), na staging, a nie od razu na world1:
   - stan „overview” przed obecnym oknem: spectator + kamera `ItemDisplay` + `Interaction`,
   - LPM = graj ostatnią postacią, PPM = obecny selektor MMOProfiles lub kreator, A/D = przełączanie postaci na scenie (nosiciele skórek już są),
   - blokady: StopSpectating, teleport SPECTATE, `hidePlayer` między graczami,
   - eval botem (PASS/FAIL na zdarzeniach) + zrzuty klientem QA.
4. **Karta postaci w MythicHUD** (pkt 2): layout z PAPI, włączany na czas sceny.
5. **Scenerie i fade**: 2–3 dioramy w `creator_limbo` i przełączanie (np. W/S albo pozycja w menu PPM).
6. **Tryby w kreatorze** (pkt 6): wybór + Hardcore. Ironman dopiero po spisaniu wszystkich dróg handlu na stacku.
7. **`FakeLowerInventory` na PacketEvents** (pkt 3): najpierw zamiast fizycznego czyszczenia plecaka w kreatorze (usuwa ryzyko utraty ekwipunku i część `sessions.yml`), potem pełnoekranowe Character Info.
8. **Drzewko MMOCore** (pkt 5): najpierw jeden węzeł z `item_model` jako test klucza, potem generator grafik 44 bazowych modeli z paletami stanów.
9. Opcjonalnie później: pełnoekranowe drzewko (pkt 3 + 5), Craftsman, kolejka usuwania postaci.

---

## 9. Materiały PoC

- `poc/plugin/src/poc/ScenePoc.java`: scena (warianty a/b/c), log wszystkich zdarzeń wejścia.
- `poc/plugin2/src/poc/FullscreenPoc.java`: fałszywy dolny ekwipunek (PacketEvents).
- `poc/review/captures/`:
  - `a-02` (spectator: brak HUD-u, kamera zablokowana), `a-03` (menu spectatora pod 1–9), `a-10` (bossbar i action bar widoczne w spectatorze),
  - `c-01` (adventure: HUD i ręka widoczne),
  - `fs-01` (fałszywe sloty), `fs-03` (tooltip fałszywego przycisku), `fs-05` (prawdziwy ekwipunek po zamknięciu).
- `poc/server/`: Paper 1.21.11-132 z oboma pluginami. Ponowne uruchomienie: `docker network create poc-net`, kontener `eclipse-temurin:21-jdk` z `java -jar paper.jar --nogui`, klient `mc-client-test` z `poc/review/launch.sh` (quickPlay `poc-paper:25565`) i `poc/drive.sh`.
- `nexo-llms-full.txt`: pełny zrzut dokumentacji Nexo (docs.nexomc.com/llms-full.txt).
