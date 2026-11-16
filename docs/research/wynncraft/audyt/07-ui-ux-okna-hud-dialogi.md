# 07. UI/UX: okna, HUD, dialogi, tooltipy, nameplate'y, powiadomienia

Źródła:
- Wynntils (commit `8191bdf`): `models/containers/containers/*` (64 klasy okien), `models/dialogue`, `models/spells`, `handlers/actionbar`,
- WynnEdits Dark Theme 3.5 (Modrinth, 05.08.2026, 1.21.9–1.21.11, 2 068 plików), który nadpisuje **aktualne** ścieżki oficjalnego packa. Kształty są oficjalne, kolory przyciemnione (montaże w `klatki/P_*.png`),
- klatki z nagrań 2026 (`klatki/V*.jpg`).

Nie powtarzam okien postaci z rund 1–2 (Character Info, Ability Tree, Content Book, selektor).

---

## 1. Katalog okien (64 w Wynntils)

**[F]** Pełna lista z wzorcem tytułu i flagą „pełnoekranowe”: `dane/wynntils_okna.csv`. Grupy:

| Grupa | Okna | Uwagi |
|---|---|---|
| Postać | CharacterSelection, CharacterCreation, CharacterInfo, AbilityTree, AbilityTreeReset, Aspects, MasteryTomes, ContentBook | 6 pełnoekranowych: selektor, kreator, drzewko, reset drzewka, aspekty, Content Book |
| Handel | TradeMarket (+ Buy, Sell, Order, Filters, Trades, RevealItems), BlacksmithRepair, BlacksmithSell, ScrapMenu | tytuły z glifem `\uDAFF\uDFE8\uE01x` (7 stanów) |
| Usługi NPC | ItemIdentifier, ItemIdentifierAugments, CraftingStation, MountFeeder, Seaskipper, RaidStart, LootrunRewardChest, RaidRewardChest, RaidRewardPreview | PartyFinderMatchFound jest pełnoekranowe (animacja) |
| Magazyn | AccountBank, CharacterBank, PersonalBlockBank, IslandBlockBank, MiscBucket, Bookshelf, PersonalStorage, IngredientPouch, EmeraldPouch | wspólny prefiks `\uDAFF\uDFF0\uE00F\uDAFF\uDF68` + znacznik typu `\uF000–\uF005` |
| Gildia | GuildManagement, GuildMemberList, GuildBank, GuildTerritories, GuildBadges, GuildLog, GuildDiplomacy | tytuły tekstowe („<gildia>: Manage”) |
| Sklep | Store, StorePage, Cosmetic, Jukebox | glify per kategoria |
| Housing | HousingList, HousingJukebox, HousingSongSelect | tekstowe |
| Nagrody | LootChest „Loot Chest [★★★★]”, Daily Rewards, Challenge Rewards, Event Rewards, Flying Chest Rewards, Item/Ingredient Bomb Rewards, Objective Rewards, Rating Rewards, Leaderboard Rewards | tekstowe |
| Lobby | LobbyContainer „Wynncraft Servers” ze stronicowaniem | tekstowe |

- **[F]** Wzorzec tytułu: **ujemna spacja + glif tła + (opcjonalnie) glif stanu**, np. bank konta `\uDAFF\uDFF0` (−16 px), `\uE00F` (tło banku), `\uDAFF\uDF68` (−152 px), `\uF000` (napis „Account Bank” z `bank/title.png`). **[W]** Tło i napis to osobne glify. Jedno tło banku obsługuje 6 rodzajów banków.
- **[F]** Okna starsze i rzadziej używane (gildia, housing, nagrody) mają zwykły tekstowy tytuł. **[W]** Wynn przerabia okna stopniowo, od najczęściej oglądanych.

## 2. Styl okien: „diorama” zamiast panelu

**[F]** Tekstury 256×256 z WynnEdits (`klatki/P_m_interface_a.png`, `P_m_interface_b.png`):
- **Trade Market**: stragan z zieloną markizą w pasy, drewniany szyld „Trade Market”, podpis stanu na deseczce („What would you like to buy?”, „Filter Items”, „View Order”, „Trade Overview”).
- **Blacksmith** (Repair/Sell), **Item Identifier** (Identify/Add Augments), **Powder Master / Item Upgrader** (Infuse, Remove, Upgrade, Ascension, Pouches): stół warsztatowy w kolorze usługi (fioletowy, zielony, pomarańczowy, niebieski) z narzędziami. Po lewej sylwetki slotów, po prawej slot wyniku.
- **Party Finder** (create/join/queue/search): tablica ogłoszeń pod dachem z dachówek.
- **Raid Start**: czerwona tablica z wirem, nazwa raidu na wstędze (`raid/title.png`). **Raid Reward**: kufer z czaszką i rogami.
- **Lootrun**: niebieskie panele z ramką, podpisy na złotych deseczkach („Select a Boon”, „Select a Mission”, „Select a Trial”, „Verify Your Equipment”, „Inspect”, „Surrender?”). Wstęgi z nazwami obozów. Trial ma czerwone pęknięcia.
- **Bank**: sejf z otwartymi drzwiami, a napisy 6 banków na tabliczkach (`bank/title.png`).
- **Mount Feeder**: żłób ze strzechą. **Wardrobe**: szafa z kapeluszem i zieloną zasłoną.
- **Plakaty** aktualizacji i festiwali: pergamin z logo, listą zmian i pieczęcią.
- **[W]** Każda usługa ma „przedmiot ze świata” jako okno: stragan, stół, tablicę, kufer. Gracz rozpoznaje okno w pół sekundy. To styl do skopiowania bez zmian technicznych (u nas glif Nexo w tytule, `gui-panel-glif-w-tytule`).
- **[F]** Część okien ma animowane przejścia z pasków klatek, np. `store/scrap.png` 256×1408 i `scrap_transition.png` 256×1536 (klatki pionowo), `party_finder/match_found.png` 256×4352.

## 3. HUD rozgrywki (oficjalne ścieżki od 2.2, `textures/font/hud/gameplay/default/*`)

**[F]** Pliki i rozmiary (montaż: `klatki/P_m_hud_gameplay.png`):

| Położenie (font) | Tekstura | Co to jest |
|---|---|---|
| bottom_middle | hotbar 208×64 | listwa hotbara |
| | health_background 72×100 (4 warianty serca), health_bar 72×32 (4 kolory), mana_background, mana_bar 72×8 | paski HP i many z liczbami (`numeric_display`, `level` z cyframi i sufiksami k/m/b/t) |
| | experience_bar, profession_bar, jump_bar 179×159 | paski postępu w stanach wypełnienia (ukośne pasy = kolejne klatki) |
| | hex_meter 240×189 | heksagony zasobu klasy |
| | click_combo 112×16, spell 48×16 | ikony myszy L/R i ikony HP/many/pioruna przy spellu |
| | ultimate_charge 704×480, ultimate_meter 1536×96, ultimate_ready 1920×64, ultimate_transition 384×96 | licznik i animacja gotowości ultimate'a |
| | transition, transition_type | ikony przejść (sprint, skok…) |
| center_left | mount_energy 256×192 | pionowy pasek energii wierzchowca |
| center_middle | marker 1×256, powder_special 320×192 | znacznik pozycjonowania shaderem i ładowanie „powder special” |
| top_right | compass 13×12, location 160×40, marker 1×256 | kompas i koordynaty („7528 NE 1865”, `klatki/V12_…`) |

- **[F]** Na klatce z walki (`klatki/V12_…`):
  - na dole „21853 / 21853” HP z czerwonym paskiem, pośrodku emblemat archetypu w heksagonie, po prawej „100 / 100” many, pod spodem hotbar,
  - u góry bossbary: „LV. 25 – … – 27% XP”, „10s Healed: 164% [●●●●●●]”, „Wings Captain – 130k ♥ – Dam”, „SLAY! Kill 0/25 mobs to proceed”,
  - komunikaty błędów na czacie po lewej („Your ultimate is not fully charged yet”, „You don't have enough mana to cast that spell!”).
- **[F]** Ekran wyboru postaci ma własny zestaw fontów (`hud/selector/default/*`):
  - karty klas w wersji zwykłej i „_create”, także dla reskinów (card_dark_wizard, card_hunter, card_knight, card_ninja, card_skyseer),
  - `archetype.png` po lewej,
  - **logo w lewym górnym rogu w wariantach eventowych**: blizzard, bonfire, fruma, heroes.
- **[W]** Karty reskinów dowodzą, że reskin to pełna zmiana tożsamości klasy w UI. Logo zmienia się z festiwalem, czyli sezonowy „skin” całego menu.

## 4. Dialogi NPC (nowy system z 2.2.0)

- **[F]** „Overhauled the dialogue system to no longer be in chat. Dialogue will instead be in a box over the hotbar, and some characters will also have portraits” (Version 2.2.0).
- **[F]** Budowa z tekstur (`textures/font/hud/dialogue/*`, montaż `klatki/P_m_dialogue.png`):
  - `style/<styl>/`: box 256×80, frame 64×80 (ramka portretu), nameplate 112×64 (tabliczka z imieniem), choice 288×128 (lista wyborów), cursor, navigation (strzałki), footer, decal (ozdoby: liście, pnącza). **5 stylów**: default, chaos, fruma, gavel, reserved. Styl „królewski” (turkus + złoto) widać przy Sovereignach (`klatki/V01_…`, `V05_…`),
  - `portrait/*.png`: **35 postaci** i `template.png` (sylwetka zastępcza 48×64 px). Portret ma 48×64 px, a arkusz 2 rzędy klatek mimiki: Tasim 768×128 = 32 klatki, Sui 2016×128 = 84, Cileon 48×128 = 2. Królowa ma większy arkusz (1440×192),
  - `text/*`: fonty do dialogów, w tym **fikcyjne alfabety** (wynnic, high_gavellian, old_fruman) oraz ikony klawiszy (keybind 752×512, shift, scroll z myszą),
  - `effect/fade.png`, `effect/progress.png`: gradient zanikania i trójkątny wskaźnik postępu.
- **[F]** Sterowanie: „[SHIFT] to continue”. Wybór odpowiedzi: przewijanie myszą („to choose option”) i „[SHIFT] to confirm”. Lista wyborów pojawia się obok ramki (`klatki/V18_…`). Historia dialogów jest w Content Booku (runda 2).
- **[F]** Wynntils czyta dialog z **action bara**: segment `DialogueSegment` z flagami `requiresShift`, `hasChoices`. Tekst narasta (efekt maszyny do pisania) przez kolejne aktualizacje action bara (`models/dialogue/DialogueModel.java`).
- **[F]** Krytyka (Reddit 1scwwye, 216 pkt, 04.2026):
  - „I have no idea what my health and mana are at… because she will not stop talking” (boss mówi w walce, a ramka zasłania HUD),
  - „Trying playing on steam deck, I literally can't see anything when text pops up”,
  - „I just wish you didn't need to press shift for like half a second to skip”,
  - „This been a problem since beta start”.

  Obrona: „I actually like this change, I used to accidentally skip dialogue all the time”.
- **[F]** 2.2.4: „Implemented dialogue portraits for all characters” w queście Off the Rails, „Fixed dialogue box overlay issues in The Strong Survive”.
- **[W]** Portrety i styl per frakcja budują immersję (gracze chwalą: „The quests are going to feel so much more immersive now”, Reddit 1qztfi8). Ale **dialog nie może zasłaniać HUD-u w walce**. Linie bossa w walce powinny iść do bossbara albo małego dymka, nie do dużej ramki. U nas: dialog w action barze z glifami (MythicHUD/Nexo) albo dialog Papera tylko poza walką. Pomijanie od razu, bez przytrzymywania.

## 5. Tooltipy przedmiotów (nowe w 2.2.0)

**[F]** `klatki/V11_…` i tekstury `textures/font/tooltip/*` (`klatki/P_m_tooltip_nameplate.png`):
- u góry emblemat w ramce (kształt ramki = typ przedmiotu, kolor = rzadkość; `emblem/frame.png` 384×384, `emblem/sprite.png` z ikonami),
- nazwa w kolorze rzadkości, pod nią **plakietki** „FABLED”, „BOW”, żywioły,
- „818 DPS / Slow (1.5 hits/s)” i obrażenia z ikonami żywiołów,
- **5 ikon atrybutów** STR/DEX/INT/DEF/AGI z ✔/✘ i liczbą wymagania (`requirement/*`),
- „Class Type”, „Combat Level” z ✔,
- ID: nazwa, wartość i **kółko-miernik jakości rolla** (`identification/meter*.png`), ikona major ID (`identification/major.png`),
- **sockety powderów** (`tooltip/socket.png`: puste i 5 kolorów),
- linie podziału (`divider/line.png`),
- **stronicowanie tooltipa** kropkami na dole („● ● ●” + czerwony kwadrat, `tooltip/page.png`).
- **[F]** Gracze: „full item text overhauls… you can see how the identifications on an item rolled in the vanilla game now without a third party mod” (Reddit 1qztfi8, 180 pkt).
- **[F]** Shader `gui.glsl` ma `disableTooltips()`, który chowa tooltip, gdy wychodzi poza ekran (pack 2.1). **[W]** Długie tooltipy to problem, dlatego jest stronicowanie.
- **[W]** U nas: MMOItems lore + glify Nexo (ikony atrybutów, sockety, mierniki) i od 1.21.2+ komponent `tooltip_style` do tła tooltipa. Stronicowanie tooltipa wymaga przełączania lore (np. shift/scroll): do sprawdzenia, czy warto.

## 6. Nameplate'y mobów i NPC, hologramy, napisy 3D

- **[F]** Moby: tekstury `nameplate/{default,gold,gold_radiant,dark,corrupt}/{active,inactive,phase,status,bar}.png`. Cyfry-glify aktywne i nieaktywne, romby faz, ikony statusów i pasek. Bossy raidów: nazwa z paskiem HP, procentem i ikonami timerów (`klatki/V17_…`, `V20_…`).
- **[F]** NPC: zielona nazwa i tabliczka „NPC” pod nią. Questowi NPC mają zielone iskry, a pytajnik „???” przy nieznanych imionach (klatki z `fky7iGCP1e4`).
- **[F]** Hologramy z ikoną myszy: „Click to Open Feeder / Feed, breed, and manage mounts”, „Right-Click with an Empty Hand to Interact” (runda 3) i wymagania węzła z ✔/✘ (`klatki/V09_…`).
- **[F]** **Duże napisy 3D w świecie** przy wejściu do lokacji: „MOUNT ENCLOSURE / Ternaves Ranch / Feed and improve your Horse / Level 10” pikselowym fontem na tle ściany (`klatki/V07_…`). **[W]** To `text_display` z własnym fontem i dużą skalą.
- **[F]** Kafelki powiadomień `tile/notify.png` („NEW!” w 4 stanach), `tile/swap.png` i `swap_fast.png` (klawisz F), `tile/emblem.png` (złote heksagony), `tile/emoji.png` (zestaw emoji).

## 7. Tytuły ekranowe i przejścia

- **[F]** „YOU DIED” z czaszką i opisem śmierci (`klatki/V13_…`); „Warping in 3 / DO NOT MOVE”; „SLAY! / Kill mobs to continue”; „Complete!”; tytuł prowincji przy wejściu („The Province of Fruma”, turkusowy napis na środku; film yhRMvBEvkfg 18:40); w cutscenkach „PRESS [F] TO SKIP 1/4” u góry.
- **[F]** Przejścia z czernią i logo są opisane w rundzie 3.

## 8. Sidebar (scoreboard)

- **[F]** Sekcje: nagłówek „play.wynncraft.com”, „Tracked Quest” z celem i koordynatami, „Daily Objective”, „Guild Obj.”. W party: lista członków z HP i poziomem. W raidzie: „Raid: …, Time Left mm:ss, Challenges x/4” (`klatki/V21_…`). Czerwone numery scoreboardu ukrywa shader (`hideScoreboardNumbers`, runda 3).
- **[F]** Krytyka: sidebar zajmuje „about a third of my screen”, a wyłącza go `/toggle sb` (runda 2).

## 9. Dźwięki UI

- **[F]** Pack 2.1 (`sounds.json`, 93 zdarzenia):
  - wycisza `block.tripwire.click_on/off` i vanillowy toast „challenge complete”,
  - zdarzenia własne: `wynn.ui.click`, `wynn.ui.challenge_complete`, `wynn.npc.question` (vanillowy villager haggle), `wynn.npc.powder_master` (glow squid), `wynn.world.chime`/`marker` (ametyst), `wynn.world.soul` (soul speed), `wynn.world.wrench` (luneta), `wynn.world.trident`.
- **[F]** 29.11.2024: „new sounds for the class selection screen” (runda 2).
- **[W]** Wynn buduje dźwięki UI z vanillowych sampli pod nowymi nazwami, więc pack jest mały. U nas Nexo pozwala na to samo (sounds.json z referencją do vanillowych plików).

## 10. Co gracze mówią o UX (skrót, szczegóły w `09-…`)

- **[F]** Plusy: tooltipy z jakością rolla, pełny kreator z animacjami („I was not expecting to see A fully animated class selection page and Cutscenes during Quests”, Reddit 1qs4rl6), Content Book jako hub.
- **[F]** Minusy: dialog zasłania HUD, sztywne SHIFT, schowane „Use Items” w sklepie (runda 2), Content Book przytłacza nowego gracza, skala UI na Steam Decku, rozbudowany sidebar.
- **[F]** Społeczność łata UX modami: Wynntils (keybindy spelli, mapa, overlaye, „Custom Bars”), Wynnventory (ceny), WynnIris (shadery pod efekty Wynna), packi „Simpler Hotbar”, WynnEdits.

## 11. Wnioski dla nas [W]

1. **Okno = obiekt ze świata** (stragan, stół, tablica, kufer, sejf). Wspólny szablon ramki, podpis stanu na deseczce, osobny glif na stan.
2. **Tło i napis jako osobne glify**: jeden panel, wiele okien.
3. **Tooltip z ikonami ✔/✘ wymagań i miernikiem rolla**: największy skok czytelności przedmiotów przy małym koszcie (glify + lore MMOItems).
4. **Dialogi z portretem i stylem frakcji**, ale poza HUD-em walki i z natychmiastowym pomijaniem.
5. **Napisy 3D przy wejściu do lokacji** i hologramy z ikoną myszy („Click to…”) na każdej usłudze.
6. **Logo i tła menu zmieniane sezonowo**: tani efekt „żywego” serwera.
