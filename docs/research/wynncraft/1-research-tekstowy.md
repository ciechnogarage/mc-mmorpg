# Wynncraft: kreator postaci i menu postaci. Research

Data: 2026-09-22. Źródła: oficjalna wiki (wynncraft.wiki.gg), forum wynncraft.com, **kod źródłowy Wynntils** (sklonowany `github.com/Wynntils/Wynntils`, commit `8191bdf`), dane CDN Wynntils (`model_data.json`) i **zawartość resource packa „Wynncraft Dark Pack”** z Modrinth (v4.3.5, 2026-06-30). Ten pack nadpisuje oficjalny pack Wynncrafta pod tymi samymi ścieżkami i kodami znaków, więc pokazuje jego strukturę.

Oznaczenia: **[F]** to fakt ze źródła (podaję URL albo plik), **[W]** to mój wniosek lub interpretacja.

Czego nie udało się ustalić. Reddit (r/WynnCraft) blokuje pobieranie, a wyszukiwarka nie indeksuje wątków, więc opinie z Reddita pomijam. Nie znalazłem też filmów na YouTube z transkrypcjami o tym ekranie. Pełny changelog 2.1.1 jest tylko na Discordzie Wynncrafta ([F] https://forums.wynncraft.com/threads/2-1-1-class-select-new-raid-boss-more.319763/). Wynncraft nie opublikował żadnego devbloga o technice kreatora. Wszystko, co piszę o technice, pochodzi z kodu Wynntils i z packa.

---

## 1. Przepływ gracza

1. **[F]** Przy wejściu klient pobiera obowiązkowy resource pack: „It is impossible to play without it, as the game tends to crash if it is not enabled” (https://wynncraft.wiki.gg/wiki/Newcomer's_Guide).
2. **[F] Lobby (hub).** To osobne miejsce do wyboru serwera świata. Świat wybiera się kompasem „Quick Connect” albo przejściem przez portal. Komendy: `/hub`, `/lobby`, `/server`. Postęp jest wspólny dla wszystkich światów (https://wynncraft.wiki.gg/wiki/Lobby). W Wynntils stan HUB rozpoznaje się po kompasie „§aQuick Connect” w 1. slocie hotbara, a selektor serwerów to skrzynia o tytule „Wynncraft Servers” ze stronicowaniem (`WorldStateModel.java`, `LobbyContainer.java`).
3. **[F] Character Menu.** Po wejściu na świat gracz dostaje „the UI that appears when joining Wynncraft” (https://wynncraft.wiki.gg/wiki/Character_Menu). Wynntils ma dla niego osobny stan `CHARACTER_SELECTION` pomiędzy `INTERIM` a `WORLD` (`models/worlds/type/WorldState.java`).
4. **[F]** Po utworzeniu postaci zaczyna się samouczek i quest „King's Recruit” (dojście do zamku Ragni). Samouczek przerobiono w 2.1.1 (Newcomer's Guide; https://x.com/Wynncraft/status/1856396425602404855).

**Sloty postaci.** Źródła się rozjeżdżają:
- **[F]** Wiki, strona Ranks: bez rangi 6, VIP 7, VIP+ 8, HERO 10, HERO+ 13, CHAMPION 15 (https://wynncraft.wiki.gg/wiki/Ranks).
- **[F]** Inna wersja wiki: 5 / VIP 6 / HERO 9 / CHAMPION 15.
- **[F]** Od lipca 2025 nowi gracze startują z **3 slotami**. Spotkało się to z ostrą krytyką (ankieta: 83% przeciw). Deusphage z Content Teamu odpowiedział: „The idea is that players will be able to acquire a rank through normal gameplay… I do think it could be increased slightly” (https://forums.wynncraft.com/threads/new-character-slot-maximum-absolutely-disgusting.322141/).
- **[F]** Twarde maksimum wynosi 15. Wynntils trzyma sloty postaci w siatce 3×5 w skrzyni: `CHARACTER_SLOTS = 9,10,11,18,19,20,27,28,29,36,37,38,45,46,47`. Pierwszy slot z nazwą „§a§lCreate a Character” kończy listę (`CharacterSelectionModel.java`).
- **[F]** Sloty kupuje się rangą za pieniądze.

## 2. Ekran wyboru i kreator (stan od 2.1.1, listopad 2024)

### Overview (ekran startowy)
**[F]** (https://wynncraft.wiki.gg/wiki/Character_Menu, https://wynncraft.wiki.gg/wiki/Version_2.1.1):
- Postać ostatnio grana albo ulubiona stoi po prawej od środka ekranu. Logo Wynncrafta jest w lewym górnym rogu.
- **Karta postaci** po prawej pokazuje poziom, nazwę, klasę z archetypami i postęp Content Booka.
- Aktywne **pety** chodzą po scenie.
- Po lewej od środka stoją **drzwi**, które otwierają się lewym klikiem. Po prawej jest **kosz**, który otwiera usuwanie.
- **Tło** to jedno z miejsc na mapie Wynncrafta, domyślnie Emerald Trail. Jest 15 scenerii, część wymaga rangi (VIP+, HERO, CHAMPION). Przewija się je scrollem.
- Sterowanie opisuje tekst na action barze: „Left-Click to play … Right-Click to switch” (`CharacterSelectionSegmentMatcher.java`).

### Character Selector (prawy klik)
**[F]** Menu z postaciami po lewej i opcjami globalnymi po prawej:
- kosz: kolejka usuwania,
- chmurka: backup 5 ostatnio usuniętych postaci (tylko CHAMPION),
- przełączniki „auto-open selector” i muzyka menu.

Lewy klik na postać uruchamia grę. Prawy klik ją edytuje. Edycja obejmuje:
- usunięcie z opóźnieniem zależnym od postępu: „brand-new characters being deleted in a few minutes while characters far in progression can take up to a month”,
- ulubioną postać (jedna na konto),
- reskin klasy (donator),
- nick postaci (CHAMPION),
- scenerię,
- ikonę: broń na kolorowym tle, 9 kolorów, VIP+ dostaje dodatkowe odcienie.

### Kreator (zielony plus)
**[F]**
1. **Wybór klasy.** Klasy przewija się scrollem. Widać nazwę, umiejętności i opisy archetypów. Od 2.1.1 „the Character Creator Menu now shows classes performing various abilities in animations” zamiast samego opisu w tooltipie (Version 2.1.1). Na action barze: „Left-Click to select / Scroll up/down to browse / Right-Click to return” (`CharacterCreationSegmentMatcher.java`).
2. **Ekran tworzenia.** Wykrzyknik otwiera opis klasy z linkiem do strony www. Obok są selektor trybów specjalnych, ikona edycji oraz przyciski potwierdź i wróć w prawym dolnym rogu.
3. Zielony checkmark tworzy postać i wpuszcza do gry.

**[F] Wyglądu nie ma.** Nie wybiera się skórki, twarzy ani rasy. Gracz występuje we własnej skórce Minecrafta. Kosmetyki (skiny broni i hełmów, disguises) są osobno, w Wardrobe (Store) (https://wynncraft.wiki.gg/wiki/Version_2.1.3).

**[F] Tryby specjalne** (https://wynncraft.wiki.gg/wiki/Special_Gamemodes). Wybiera się je przy tworzeniu i odblokowuje grą na koncie:

| Tryb | Odblokowanie | Zasady | Czy można cofnąć |
|---|---|---|---|
| Hardcore | postać lvl 30+ | pierwsza śmierć zmienia ikonę z czerwonej na szarą i wyrzuca z rankingów | nie |
| Ironman | lvl 50+ | bez handlu, banku konta i buffów od innych | tak, `/ironman` |
| Ultimate Ironman | lvl 50+ | jak Ironman, a do tego bez banku postaci | można zejść do zwykłego Ironmana |
| Craftsman | lvl 50+ | tylko ekwipunek craftowany | nie |
| Hunted | quest „A Hunter's Calling” | PvP poza miastami, +50% XP | nie |

Wszystkie tryby naraz to „HUICH”. Na ekranie wyboru ikony trybów są glifami w action barze (`–`, `CharacterSelectionGamemodesSegmentMatcher.java`).

## 3. Menu postaci w grze

- **[F] Kompas w hotbarze** to wejście do menu. W Wynntils `InventoryUtils.COMPASS_SLOT_NUM = 7`, czyli 8. slot hotbara. Content Book leży w slocie 8 (9. slot), Ingredient Pouch w slocie 13. Pierścienie, bransoleta i naszyjnik zajmują **zwykłe sloty ekwipunku 9–12** z własnymi teksturami tła slotu (`textures/wynn/gui/slot/ring.png` itd. w packu). Wiki: „click your compass on your hotbar” (https://wynncraft.wiki.gg/wiki/Skill_Points).
- **[F] Character Info** to skrzynia z tłem-glifem `` (`CharacterInfoContainer.java`). Układ slotów z Wynntils:
  - 4: tome skill pointów,
  - 11–15: pięć ksiąg umiejętności (STR/DEX/INT/DEF/AGI),
  - 7: info o postaci,
  - 8: Mastery Tomes,
  - 9: Ability Tree,
  - 17: profesje,
  - 26: gildia,
  - Content Book dostępny w części hotbara.

  Skill pointy rozdaje się klikiem w księgę. Shift-klik w zieloną kulę resetuje je w mieście. Tekstura tła (`font/interface/character_info/character_info.png`) to panel z kolumnami, przyciskiem „ABILITY TREE”, symbolem oka i siatką 2×3.
- **[F] Ability Tree** to osobne okno pełnoekranowe, stronicowane (przyciski poprzednia/następna strona w slotach 57/59). Reset działa przez 3 sockety na Ability Shardy. Aspekty są w oknie `` (`AbilityTreeContainerQueries.java`, https://wynncraft.wiki.gg/wiki/Ability_Tree). Każdy kawałek połączenia drzewa (rozgałęzienia 3- i 4-kierunkowe, aktywne i nieaktywne) to osobny model przedmiotu przez custom_model_data. W `model_data.json` Wynntils jest 159 kluczy `abilityTree.*`, np. `abilityTree.branchFourWayActiveURDL: 2.0`.
- **[F] Kolejne okna** z własnym tłem glifowym: Content Book (``), Mastery Tomes (``), Cosmetic/Store (`` i warianty), bank, blacksmith, Trade Market (`models/containers/containers/*`).
- **[F] Character Wardrobe** (od 2.1.3) to podgląd kosmetyków na postaci. Wynntils rozpoznaje go po napisie „Character Wardrobe” w action barze (`CharacterWardrobeSegmentMatcher.java`).
- **[F] Ekwipunek pod E** jest vanillowy, tylko przeskórowany packiem („Reworked the inventory GUI”, „Reworked the Hotbar GUI”, 2.1: https://forums.wynncraft.com/threads/2-1-rekindled-world-changelog.316880/).

## 4. Technika: jak to jest zbudowane

Klient jest **vanillowy z obowiązkowym packiem**. Mody (Wynntils) są opcjonalne. **[F]** Obsługiwane wersje to obecnie 1.21.4 do 26.x, zalecana 1.21.11 (https://ver.wynnvets.org/). Wsparcie 1.12 porzucono w grudniu 2023 z powodu wydajności i exploitów (https://forums.wynncraft.com/threads/why-wynncraft-will-stop-1-12-2-support.313263/).

### 4.1 Ekran wyboru postaci to scena w świecie, nie okno
- **[F]** Overview i kreator klasy działają **bez otwartego okna**. Wynntils rozpoznaje je wyłącznie po tekście w **action barze**. Wybór postaci na overview wykrywa po **machnięciu ręką** (`ArmSwingEvent` przy `screen == null` w stanie CHARACTER_SELECTION, `CharacterSelectionModel.java`). Klasy przegląda się **scrollem**, czyli zmianą slotu hotbara.
- **[F]** Wynntils w tym stanie **blokuje przełączanie na trzecią osobę (F5), wymusza pierwszą osobę i ukrywa celownik** (`CharacterSelectionUtilitiesFeature.java`). Kamera to zwykły widok gracza z pierwszej osoby, ustawiony w konkretnym miejscu świata.
- **[W]** Na tej podstawie: postać, pety, drzwi i kosz to encje lub bloki w prawdziwej lokacji. Sceneria „Emerald Trail” itd. to faktycznie inne miejsca, do których gracza się teleportuje. Postać jest pewnie NPC-em z ekwipunkiem, a animacje klas to NPC-e odgrywające spelle. Nie znalazłem źródła, które mówi, jak unieruchomiono gracza (spectator, pasażer encji czy freeze). Blokada F5 w Wynntils sugeruje, że widok z trzeciej osoby zdradza trik.
- **[F]** Kartę postaci rysuje **action bar zbudowany z glifów**:
  - klasa to glif `–` (reskin `–`),
  - poziom to cyfry-glify `–` rozdzielone spacją `-2px`,
  - tryby to ikony `–`.

  Pack ma do tego osobne fonty `font/hud/selector/default/bottom_middle/{class,control,scene,select}.png` i `…/center_right/{completion,progress,gamemode,wynncraft_outline_condensed}.png`. `scene.png` to pasek miniatur 15 scenerii, `control.png` to ikony myszy.
- **[F] Action bar w innym miejscu ekranu.** Wynntils oddziela od action bara fragment we fonie `minecraft:hud/gameplay/default/top_right` (koordynaty rysowane w prawym górnym rogu, `ActionBarHandler.java`). Pack ma katalogi fontów `bottom_middle`, `center_left`, `center_right`, `top_right`. **[F]** Pack ma też **core shadery tekstu** sterowane kolorem: kolory `0x00f000…0x00f024` włączają efekty rainbow, blink, fade, gradient, shine, warp i italic (`utils/colors/WynncraftShaderColor.java`). **[W]** Przypięcie tekstu z action bara do krawędzi ekranu (prawa strona, góra) przy dowolnej szerokości GUI wymaga core shadera `rendertype_text`. Samym fontem się tego nie zrobi. Wynncraft prawie na pewno robi to shaderem, ale kodu shadera nie widziałem.

### 4.2 Okna to skrzynie z tłem-glifem w tytule, czyli ta sama technika co nasza
- **[F]** Tytuły okien to **ujemna spacja + glif tła**:
  - Character Selector: `󏿕`,
  - kreator: `󏿐`,
  - Character Info: `󏿜`,
  - Ability Tree: `󏿪[]`.

  Kody spacji to schemat **Negative Space Font AmberWat** (`0xD0000 + szerokość`, więc `󏿕` = −43 px, `󐀊` = +10 px; https://github.com/AmberWat/NegativeSpaceFont). Fonty packa mają provider `{"id":"space","type":"reference"}`.
- **[F]** Tekstury tła w packu mają 256×256, `height: 256` i `ascent` od 12 do 64 (np. `select.png` ascent 64, `character_info.png` ascent 23). Panel sięga więc wyraźnie powyżej zwykłego nagłówka skrzyni. Warianty okna selektora to osobne glify: `` select, `` customize, `` cancel, `` backups, `` icon, `` delete, `` create, `` gamemode. Na panelach wypisane są „breadcrumby”, np. „/Class └ Character Create └ Select Gamemodes”.
- **[F] Okna pełnoekranowe.** Wynntils oznacza część okien jako `FullscreenContainerProperty`: „These containers are displayed in the entire screen, and **temporarily replace the user's inventory**”. W Ability Tree przyciski leżą w slotach 57, 58, 59, 85 i 86, czyli w części ekwipunku gracza pod skrzynią 54-slotową. Serwer wysyła tam fałszywe przedmioty-przyciski.
- **[F] Przyciski to przedmioty z modelem:**
  - `custom_model_data` w formacie floats z 1.21.4+ (`CustomModelService`, `model_data.json`),
  - **mikstury z `potion_contents.custom_color`** jako kolorowane elementy (przyciski stron, kolor glinta w Wardrobe, `StoreModel.java`),
  - modele GUI z **`display.gui.scale` 5×** i obrotem (`models/item/wynn/gui/row.json`, `column.json`), dzięki którym jeden przedmiot zakrywa kilka slotów. Tak powstają kafelki i przyciski wieloslotowe, np. przycisk trybu na 3 sloty w `gamemode.png`,
  - animowane tekstury warstw (`layer/state/selected.png.mcmeta`: 2 klatki).
- **[F]** Efekty spelli to encje `item_display` z modelami. Jeden spell ma do 106 klatek (`item_display_model_data.json`, grupa „Judrajim”).

### 4.3 HUD
- **[F]** HUD gry jest złożony z glifów w **action barze**: zdrowie, mana, hotbar-ramka, meter ultimate'a, click-combo, poziom. Fonty `font/hud/gameplay/default/bottom_middle/*.png`, parsery `models/characterstats/actionbar/*` w Wynntils. Paski dodatkowe (spelle, info, wojny, skip cutscene) idą **bossbarami** (`handlers/bossbar/*`, `models/abilities/bossbars/*`).
- **[F]** Dialogi NPC mają własny HUD: `font/hud/dialogue/style/default/{box,frame,nameplate,choice,cursor…}.png`.

## 5. Ewolucja

- **[F]** Przed 2.1.1: „it was simply based on a chest UI in a black box” (Character Menu). Postacie były przedmiotami w skrzyni, automatycznie posortowanymi po poziomie. Gracze prosili o ręczne sortowanie, ankieta 86% za (https://forums.wynncraft.com/threads/organizing-your-class-selection-screen.235702/). Już w 2020 roku Wynntils zrobił własny selektor postaci jako mod „fit the game style” (https://forums.wynncraft.com/threads/wynntils-update-v1-7-0-character-selector-revamp-ambient-particles-ost-offline-player.274133/). Jeden gracz przy 2.1.1 napisał, że Wynn „made the class selection screen similar to wynntils just like the hotbar”.
- **[F]** 2.0 „Spellbound” (09.2022): drzewko umiejętności, archetypy, shardy i bardziej zwarte Character Info (https://wynncraft.wiki.gg/wiki/Version_2.0).
- **[F]** 2.1 „Rekindled World” (2024): „Reworked the overall GUI style”. Przerobiono ekwipunek, hotbar, bank, Content Book, Ability Tree, menu ESC, Trade Market, Blacksmith i Identifier. Zapowiedź: „We have even more GUI reworks in the way!” (https://forums.wynncraft.com/threads/2-1-rekindled-world-changelog.316880/).
- **[F]** 2.1.1 (11.2024): nowy Character Selection Screen ze sceną, kartą, petami, sceneriami, ikonami i animacjami klas. Przerobiono też samouczek.
- **[F]** 2.1.3 „Wardrobe Wonders”: podgląd kosmetyków przed zakupem.
- **[F]** 07.2025: nowi gracze startują z 3 slotami.
- Deweloperzy nie tłumaczyli publicznie, dlaczego przerobili ekran wyboru. Motywacje nie znalazłem; changelog 2.1.1 jest tylko na Discordzie.

## 6. Opinie graczy

- **Chwalą** (forum): „Awesome changes, i love the new character menu!” (wątek 2.1.1). Mod, który kiedyś łatał ten ekran, stał się niepotrzebny.
- **Krytykują:**
  - limit slotów i monetyzację: 3 sloty na start przy 5 klasach nazwano „predatory” (wątek 322141),
  - brak ręcznego porządkowania postaci (stary wątek, ulubiona postać z 2.1.1 częściowo to rozwiązuje),
  - obowiązkowy pack: bez niego gra się crashuje,
  - opinie o nowym hotbarze: powstały popularne packi przywracające stary przezroczysty hotbar, np. „Wynncraft Simpler Hotbar”, ponad 44 tys. pobrań na CurseForge (https://forums.wynncraft.com/threads/wynncraft-simpler-hotbar-old-transparent-hotbar-back.319186/),
  - bug z 2.1.1: gracz stojący w selektorze wchodził sam na świat po restarcie.
- Opinii z Reddita nie udało się zebrać (blokada), więc sekcja jest niepełna.

---

## 7. Co z tego możemy przenieść do nas

Stack: Paper 1.21.9+, MMOCore/MMOItems/MMOProfiles, MythicMobs, DeluxeMenus, Nexo, CMI, vanillowy klient. Oceny są moje **[W]**.

| # | Technika Wynncrafta | U nas | Wykonalność |
|---|---|---|---|
| 1 | Tło okna = ujemna spacja + glif 256 px w tytule | **Już to robimy** (glif Nexo w tytule). Wynncraft potwierdza kierunek. Do przejęcia: **wysoki `ascent` (do 64)**, żeby panel wychodził ponad skrzynię, **osobny glif na każdy stan okna** (select/create/gamemode/delete) i breadcrumby na panelu | **Łatwe**. Pamiętać o `&f` przed glifem (nasza pułapka z tintem tytułu) |
| 2 | Przyciski z przedmiotów z modelem, `display.gui.scale` > 1 (jeden przedmiot na kilka slotów) | Własne modele Nexo z `display.gui` scale/translation, w DeluxeMenus pozostałe sloty puste lub przezroczyste | **Łatwe/średnie**. Klik w sąsiedni slot trzeba zmapować w DM na tę samą akcję |
| 3 | Kolorowanie jednego modelu tintem (potion `custom_color`) | W 1.21.4+ definicja modelu przedmiotu z `tints` (`minecraft:custom_model_data` colors lub `potion`). Jeden model przycisku w N kolorach, np. ikony postaci w 9 kolorach jak u Wynna | **Łatwe** (pack Nexo + `item_model`/CMD w DM) |
| 4 | Okna pełnoekranowe: fałszywe przedmioty w dolnej części okna (ekwipunek gracza) | DeluxeMenus tego **nie umie**. Potrzebne pakiety (PacketEvents/ProtocolLib, `SET_CONTENT` z podmienionymi slotami 54–89) albo własny plugin. Podmiana prawdziwego ekwipunku jest ryzykowna przy MMOProfiles | **Trudne**. Tańsza alternatywa: skrzynia 6-rzędowa i glif, który wizualnie zakrywa też pas ekwipunku (przedmioty gracza i tak będą widoczne na wierzchu) |
| 5 | **Wybór postaci jako scena w świecie**: gracz w 1. osobie w stałym punkcie, postać/NPC na scenie, lewy klik = graj, prawy = menu, scroll = przeglądanie | Mamy już mannequina ze skórką rasy, czyli ten sam pomysł co postać na scenie Wynna. Brakuje: unieruchomienia kamery (spectator + `setSpectatorTarget` na encji albo pasażer niewidzialnej encji), obsługi LPM/PPM/scrolla (`PlayerInteractEvent`, `PlayerItemHeldEvent`) i HUD-u w action barze | **Średnie/trudne**. Wymaga małego pluginu (DeluxeMenus/CMI tego nie złożą). Najlepszy efekt „wow” za rozsądną cenę. Zmiana MMOProfiles przez komendy z pluginu |
| 6 | Karta postaci i podpowiedzi sterowania w action barze z glifów (cyfry-glify, ikony klasy, ikony myszy) | Font Nexo z glifami cyfr, klas i ikon myszy. Wysyłka z pluginu albo CMI/MythicMobs `actionbar` z placeholderami | **Łatwe** przy pozycji dół-środek |
| 7 | HUD przypięty do rogów ekranu (`top_right`, `center_right`) i efekty tekstu sterowane kolorem | Wymaga **core shaderów** (`rendertype_text`) w packu Nexo | **Średnie, ryzykowne**. Shadery rozjeżdżają się między wersjami MC i nie działają z Iris shaderpackami. Na razie trzymać się dołu-środka i bossbara |
| 8 | Wejście do menu z kompasu w stałym slocie hotbara | Mamy Q (MenuKey + DeluxeMenus), co jest wygodniejsze. Można **dodatkowo** dać przedmiot-menu w stałym slocie jako wejście widoczne dla nowych graczy | **Łatwe** (CMI/MMOItems + blokada przesuwania) |
| 9 | Akcesoria w zwykłych slotach ekwipunku z własnym tłem slotu; przeskórowany vanillowy ekwipunek | E nie przechwycimy, Wynn też nie. Robi to czysto packiem. Możemy nadpisać `gui/container/inventory` i sprite'y slotów w Nexo | **Łatwe/średnie**. Uwaga na nasze celowe przezroczyste overrides (shulker, slot_highlight) |
| 10 | Tryby przy tworzeniu (Hardcore/Ironman…), odblokowywane postępem konta | DeluxeMenus: krok kreatora z przełącznikami (permission/meta przez LuckPerms lub PAPI). Egzekwowanie zasad (zakaz handlu, permadeath) to logika serwera, raczej MythicMobs/własny kod | Wybór: **łatwy**. Egzekwowanie: **średnie/trudne**. Na start 1 tryb (Hardcore) |
| 11 | Usuwanie postaci z opóźnieniem zależnym od postępu + backup ostatnio usuniętych | MMOProfiles ma usuwanie. Kolejka z opóźnieniem wymaga pluginu albo skryptu z zadaniem czasowym | **Średnie**. Tanie zabezpieczenie przed pomyłką |
| 12 | Personalizacja karty: ulubiona postać, kolor ikony, sceneria tła | Pola w danych profilu (PAPI/meta) i glif ikony w oknie DM | **Łatwe** dla ikony i ulubionej. Sceneria ma sens dopiero przy scenie z pkt 5 |
| 13 | Drzewko umiejętności z przedmiotów-połączeń (CMD dla każdego wariantu rozgałęzienia, aktywne/nieaktywne) | MMOCore skill-tree GUI ma ikony ścieżek i węzłów per status z custom-model-data. Wymaga zestawu modeli połączeń w Nexo | **Średnie**. Głównie praca graficzna |
| 14 | Animacje w oknach: kolejne `SET_SLOT`, animowane tekstury `.mcmeta` | Animowane tekstury przez mcmeta w Nexo działają. Sekwencje slotów z DeluxeMenus są ograniczone (update-interval) | **Łatwe** (mcmeta) / **średnie** (sekwencje) |

**Czego nie kopiować:**
- 3 sloty na start z monetyzacją slotów: gracze uznali to za predatorskie.
- Obowiązkowy pack, który crashuje bez akceptacji: u nas lepiej wymusić pack Nexo z czytelnym komunikatem.
- Kreator bez wyboru wyglądu: tu mamy przewagę, bo rasa to skórka na mannequinie, a Wynn nie ma żadnego wyboru wyglądu.

**Proponowana kolejność [W]:** 1 i 3 → 6 → 12 → 2 → 10 (sam wybór) → 5 (scena, osobny plugin) → 4/7 tylko jeśli naprawdę potrzebne.

---

### Materiały lokalne (do weryfikacji)
- Klon Wynntils: `/home/dev/.claude/jobs/71303296/tmp/wynntils`. Kluczowe pliki: `common/src/main/java/com/wynntils/models/character/CharacterSelectionModel.java`, `models/containers/containers/*.java`, `models/worlds/WorldStateModel.java`, `features/utilities/CharacterSelectionUtilitiesFeature.java`, `handlers/actionbar/ActionBarHandler.java`, `utils/colors/WynncraftShaderColor.java`.
- Dark Pack (struktura packa Wynna): `/home/dev/.claude/jobs/71303296/tmp/dark/assets/minecraft/` (`font/interface_disabled.json`, `textures/font/interface/character_select/`, `textures/font/hud/selector/`).
- Dane CMD: `/home/dev/.claude/jobs/71303296/tmp/model_data.json`, `idm.json`.
