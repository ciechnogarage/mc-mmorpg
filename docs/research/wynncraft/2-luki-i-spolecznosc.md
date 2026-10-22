# Wynncraft: uzupełnienie luk w researchu kreatora i menu postaci (runda 2)

Data: 2026-09-22. Ten plik uzupełnia `wynncraft-kreator-research.md` i nie powtarza jego treści. Są tu tylko nowe fakty i poprawki.

Oznaczenia: **[F]** to fakt ze źródła (podaję URL), **[W]** to mój wniosek.

Nowe źródła w tej rundzie:
- **Reddit przez API arctic-shift**: `arctic-shift.photon-reddit.com/api/posts/search` oraz `/api/comments/search` i `/api/comments/tree`. To API działa. Pullpush też odpowiada. old.reddit `.json` zwraca pustą odpowiedź.
- **GitHub issues i PR Wynntils** przez `api.github.com`.
- **Zrzut ekranu wyboru postaci** z PR Wynntils #3544.
- **Posty Salteda** o patchu Wardrobe Wonders.
- **wiki.gg**: tylko przez WebFetch, bo curl dostaje stronę „Blocked”.

---

## 0. Poprawki do rundy 1 (ważne)

| Temat | Runda 1 | Stan faktyczny |
|---|---|---|
| Sloty nowego gracza | „Od 07.2025 nowi gracze startują z 3 slotami” | **[F]** 3 sloty były tylko w **becie** Wardrobe Wonders. Po fali krytyki Salted 24.07.2025: „We're changing the default Character Slots from 3 to 5… you guys told us you found it important for new players to be able to test every class” (https://forums.wynncraft.com/threads/update-on-the-wardrobe-wonders-patch.322148/). 26.07.2025 doprecyzował, że wcześniej było **6**, więc to nadal spadek z 6 do 5: „it's a barely used slot from no-rank players and it allows us to more easily spread the extra slots to all the ranks” (https://forums.wynncraft.com/threads/second-update-on-the-wardrobe-wonders-patch.322171/). W becie siatka rang wyglądała tak: 5/6/7/9/12/15, od braku rangi do CHAMPION (https://forums.wynncraft.com/threads/wynncraft-wardrobe-wonders-lootrun-trials-store-update-trickster-rework-more.321725/). **[F]** Wiki Ranks nadal podaje 6/7/8/10/13/15 (https://wynncraft.wiki.gg/wiki/Ranks). **[W]** Wiki pokazuje prawdopodobnie stan dla kont sprzed zmiany. |
| Czas usuwania postaci | „do miesiąca” (wiki Character Menu) | **[F]** Changelog 2.1.3 (07.08.2025): „The max character deletion time was decreased from 30 minutes to 5 minutes” (https://wynncraft.wiki.gg/wiki/Version_2.1.3). **[F]** Zapowiedź patcha: „We used to give a lot of free slots but at the cost of making deletion extremely slow… The deletion timer has been SIGNIFICANTLY reduced” (wątek 321725). **[W]** Strona Character Menu jest w tym miejscu nieaktualna. |
| Nick postaci i backupy | CHAMPION | **[F]** Wiki Ranks: nicki od **HERO+**, „Class Backups” od HERO+. **[F]** Changelog 2.1.3 wymienia „instant Character deletion” i „Character backups” jako perki HERO+. HERO+ to nowa ranga z 2.1.3. |
| Data 2.1.1 | 11.2024 | **[F]** Premiera **12.11.2024** (https://forums.wynncraft.com/threads/2-1-1-class-select-new-raid-boss-more.319763/). Beta trwała już 08.11.2024: „Beta is currently active” (https://github.com/Wynntils/Wynntils/pull/2980). |
| Slot Content Booka | slot 8 (9. slot) | **[F]** Wiki pisze „eighth slot of the player's hotbar” (https://wynncraft.wiki.gg/wiki/Content_Book). Gracz na Reddicie pisze „takes up the furthest to the right slot” (https://www.reddit.com/r/WynnCraft/comments/1oquqmc/). **[W]** Wynntils używa indeksu 8 liczonego od zera, czyli to 9. slot, skrajnie prawy. Wiki liczy inaczej albo się myli. |

---

## 1. Reddit r/WynnCraft (luka 1)

### Reakcje na nowy ekran wyboru (2.1.1)
- **[F]** Już 08.11.2024, w becie, gracz pokazuje zrzut menu z postaciami na poziomie 105. Pod spodem pytanie: „how do you change the background in the class selection screen?” (https://www.reddit.com/r/WynnCraft/comments/1gmhqni/). **[W]** Sceneria nie była oczywista bez podpowiedzi.
- **[F]** „2.1.1 new character selection screen breaks old wynntils” (12.11.2024, https://www.reddit.com/r/WynnCraft/comments/1gpsqw4/). Pojawiały się też zgłoszenia zawieszenia na ekranie ładowania.
- **[F]** Nowy gracz nie mógł wybrać klasy lewym klikiem. Widział „left-click to select” i „scroll up/down to browse”, ale „I also can't see my cursor… I also can't use /help as I'm in the character selection page and it says I can't use that right now” (https://www.reddit.com/r/WynnCraft/comments/1hgqbq1/). **[W]** Potwierdza to scenę bez okna: nie ma kursora, komendy są zablokowane, wejście idzie przez machnięcie ręką.
- **[F]** Błąd renderowania selektora przez mody. Odpowiedzi w wątku: „turn off custom loading screen in wynntils”, „what causes this was a mod called 'immediately fast'” (https://www.reddit.com/r/WynnCraft/comments/1hgpplk/).
- **[F]** Kolejność postaci (04.2025). Pytanie: „How do I order the classes in the menu? I thought it's ordered by combat level.” Odpowiedzi: „in the order of level then content progression”, „only way to order them now is using content or favourites”, „please colour your classes” (https://www.reddit.com/r/WynnCraft/comments/1juyzhz/). **[W]** Kolory ikon służą graczom za ręczne porządkowanie.
- **[F]** 31.12.2025: gracz pokazuje własne pokolorowanie ikon postaci według archetypu, 80 punktów. Komentarze: „I love the new menu sooooo much”, a ktoś proponuje układ klas w rzędach (https://www.reddit.com/r/WynnCraft/comments/1q09k8c/).
- **[F]** Krytyka stylu (08.2025): „does the new GUI style look a little odd?… It looks more playful than adventurous… what are your thoughts on the new character select screen? I think it's very well done, but I liked the accessibility and speed of the original.” Odpowiedzi: „Playful fits with the vibe. Wynncraft is generally a bit cartoonish”, „I liked… the old fantasy parchment style”, oraz prośba o zmianę nazwy „Content Book”, bo „'Content' has a pretty negative connotation” (https://www.reddit.com/r/WynnCraft/comments/1miiauz/).
- **[F]** Anegdota o łączności: „if you pause for a good 30 seconds at the character selection screen before entering the world, that somehow magically helps with the connectivity” (2026-05, https://www.reddit.com/r/WynnCraft/comments/1tjkw9d/).

### Sloty postaci (lipiec 2025)
- **[F]** Wątek „greediest… rank feature updates ever” zebrał 342 punkty i 62 komentarze. Najwyżej oceniony komentarz (234) prostuje: „The 6 ability points you just get them at earlier levels… The slot reduction is only for new accounts.” Inne głosy: „3 slots is literally the hypixel skyblock non experience”, „it's just enshitification” (weteran z CHAMPION), „Limiting class slots is completely UNACCEPTABLE” (https://www.reddit.com/r/WynnCraft/comments/1m7y0pl/).
- **[F]** Gracz wracający po przerwie: „Just wish I didn't lose a character slot to make one lol, went from 10 to 9 slots” (https://www.reddit.com/r/WynnCraft/comments/1nm1x8i/).
- **[F]** Postać bywa używana jako magazyn. Gracz liczył: bez rangi 6 slotów × 2 strony banku postaci plus 21 stron banku konta (https://www.reddit.com/r/WynnCraft/comments/1kumf6q/). Inny: „I bought VIP for character slots to store more items” (https://www.reddit.com/r/WynnCraft/comments/1mkw8be/).

### Samouczek, onboarding, menu
- **[F]** Po 2.1.1: „There is now an actual functional tutorial, lol” (https://www.reddit.com/r/WynnCraft/comments/1nbci8a/).
- **[F]** Podpowiedź ruchu, którą widzi nowy gracz, pochodzi z vanilli, a nie z Wynna: „That movement tutorial is actually a minecraft feature that activates upon joining a new server/world” (https://www.reddit.com/r/WynnCraft/comments/1ir7l4h/).
- **[F]** Monetyzacja przy onboardingu: „The first of the new store stands is placed directly in front of where a new player will be dropped when they finish the tutorial” (https://www.reddit.com/r/WynnCraft/comments/1mkphtc/).
- **[F]** Lista uwag nowego gracza na poziomie 40 (02.2025) zawiera m.in.: „Make it the menus easier to navigate, there's so many clicks to change appearances”, większy ekwipunek, torby na klucze i narzędzia. Odpowiedź: bez packa „all the UI elements (including hotbar, health, mana, coordinates, merchants, and character info) will be broken” (https://www.reddit.com/r/WynnCraft/comments/1ir7l4h/).
- **[F]** Nowi i wracający gracze czują się zagubieni: „New Player Completely Lost” (https://www.reddit.com/r/WynnCraft/comments/1rawseh/), „Returning player feeling really lost” (https://www.reddit.com/r/WynnCraft/comments/1r2v38e/). W obu wątkach radą jest robić questy po kolei i zainstalować Wynntils.
- **[F]** Panel po prawej stronie ekranu (tablica wyników z questami i drużyną) zajmuje za dużo miejsca: „takes up about a third of my screen” (https://www.reddit.com/r/WynnCraft/comments/1liqtjl/, https://www.reddit.com/r/WynnCraft/comments/1g1pq2o/). Rozwiązania: `/toggle sb` (wraca po teleporcie scrollem), mniejsza skala GUI, Wynntils.
- **[F]** Pomysły graczy: zapisywanie buildów i loadoutów z drzewkiem (https://www.reddit.com/r/WynnCraft/comments/1h26uq9/, https://www.reddit.com/r/WynnCraft/comments/1wajxrx/). Odpowiedź w wątku: to już istnieje jako Build Stands. Do tego darmowy reset drzewka, sloty na drzewka umiejętności (https://www.reddit.com/r/WynnCraft/comments/1iz91ye/) i torba na przedmioty.
- **[F]** Aspekty: „open your ability tree… at the bottom of the window is the aspects button… select up to 5 aspects” (https://www.reddit.com/r/WynnCraft/comments/1q63y1l/).

---

## 2. Changelog 2.1.1 (luka 2)

- **[F]** Na forum opublikowano tylko skrót. Salted: „Starting from now, future changelogs will be posted on our Discord only!” (wątek 319763). Pełna lista: „New Raid Boss… New Character Selection Screen… Guild Raids… Tutorial Rework… Housing Statues… Balance Changes”.
- **[F]** Wiki.gg `Version_2.1.1` odtwarza changelog. Historia edycji trwa od 08.11.2024 do 16.05.2025, główny edytor to „Da Homeboi” (https://wynncraft.wiki.gg/index.php?title=Version_2.1.1&action=history). Nagłówki H2: Character Menu, Raids, Quests, World Events, Ability Changes, Items, Other Changes, Hotfixes.
- **[F]** Pełna sekcja „Character Menu” z wiki, czego nie było w rundzie 1:
  - „Character cards display the name of the character, the level, the class and its archetypes, **and how much content has been completed** on that character.”
  - „Instead of a black background, there is instead a setting showing various parts of Wynncraft.”
  - Kolory tła ikon od HERO. W późniejszym hotfiksie obniżono to do VIP+, a bez rangi można zmieniać sam symbol ikony.
- **[F]** Samouczek: „King's Recruit was reworked to clean up various outdated elements and to incorporate newer ones into it”. Dodano jaskinię Grook's Nest i „A flying bird flock was added to the tutorial”. Wprowadzono też zmiany balansu (https://wynncraft.wiki.gg/wiki/Version_2.1.1).
- **[F]** Patch 29.11.2024 (kopia changelogu wklejona na forum przez „Violet Knight”): „Updated Travelling at Dusk to use the **new sounds for the class selection screen**” (https://forums.wynncraft.com/threads/2-1-1-class-select-new-raid-boss-more.319763/page-2). Ekran wyboru ma własne dźwięki.
- **[F]** Fruma 2.2.0 (04.04.2026): „Upon logging into Wynncraft for the first time since the trailer released, **a cinematic would play in the Character Menu** showing the four panels of the Royal Gate”. Tło Royal Gate było w tym czasie odblokowane dla wszystkich (https://wynncraft.wiki.gg/wiki/Version_2.2.0). **[W]** Scena wyboru postaci służy też jako scena zapowiedzi i marketingu.
- **[F]** Aktualna lista scenerii z wiki, 15 pozycji (https://wynncraft.wiki.gg/wiki/Character_Menu):
  - bez wymagań: Emerald Trail, Royal Gate, Town of Espren,
  - wymagające rangi: Mt. Wynn i Warm Savannah (VIP+), Snowy Tundra i Barren Desert (HERO), Realm of Light (CHAMPION),
  - **eventowe, odblokowywane udziałem w festiwalu danego roku**: Bonfire Beach (2025), Sunset Valley (2026), Spirited Cemetery (2024), Jolly Blizzard (2024), Wynnter Wasteland (2025), Heroic Plaza (2025), Training Grounds (2026). Przykładowy warunek: „5 Lost Souls guided in 2024”.
  - Ciekawostka z wiki: „The Eye appears in the background of various UIs in the Character Selector.”
- Pełnego tekstu z Discorda nie znalazłem nigdzie: ani na Reddicie (wyszukiwanie tytułów z 11.2024), ani na forum, ani w wynik wyszukiwania.

---

## 3. Deweloperzy o UI, zespół i narzędzia (luka 3)

- **[F] Kto robi GUI.** W Content Teamie są „Painters”, którzy robią oficjalne grafiki i część interfejsów: Karaoke, LuxioCat, ryuuwrx. Pack utrzymuje „Resource Pack Team”: Irony i zeldahuman. Irony jest „Manager Artists” (https://wynncraft.wiki.gg/wiki/Content_Team).
- **[F] Skład płatnego zespołu** według Salteda (24.07.2025): „3x Game Devs, 1x Sysadmin, 1x Web Dev, 6x Managers, 3x Composers, **7x 2d Artists (skins, resource pack, GUIs, concept arts, web arts)**, 7x Modelers, 3x Animators, 1x Editor, 1x Accountant”. Poza tym zespół opiera się na wolontariuszach (wątek 322148).
- **[F] Narzędzia** według ogłoszeń rekrutacyjnych (https://ct.wynncraft.com/apply):
  - Texture Artist tworzy „textures for icons, illustrations and user interfaces” w edytorach graficznych, bez wskazania konkretnego programu,
  - modelerzy, skinmakerzy i animatorzy pracują w Blockbench,
  - animacje powstają „in Minecraft: Bedrock Edition format”,
  - Scripterzy piszą w „Wynncraft's proprietary scripting language (not Java)”.
- **[F] Wywiad z Saltedem** (MMORPG.com, 18.10.2023, https://www.mmorpg.com/interviews/interview-wynncraft-the-building-blocks-to-a-decade-old-mmorpg-2000129178):
  - „we don't control the client”,
  - resource pack to „really the only thing we can do to affect the client”,
  - tablica wyników jest zbudowana z fałszywych graczy z punktacją,
  - efekty powstają z niewidzialnych mobów i armor standów zszytych w jedną całość,
  - „80% of the time when we were struggling with Minecraft it's about optimization”,
  - autor wywiadu zauważa, że „some of the UI/menus are unintuitive”.
- **[F] Salted o celu perków** (wątek 322148): „make ranks more interesting to players on their first playthrough… Cosmetics and slots are nice, but new players rarely care much about those until much later in their playthrough. New players also don't have a great understanding of every system”.
- **[F] Salted o statystykach postaci** z ostatnich dwóch miesięcy: 1 postać ma 43,17% graczy, 2 postacie 14,23%, 3 postacie 8,02%, 4 postacie 6,32% (wątek 322148).
- **[F] Opór wobec stylu 2.1.** Gracze na forum narzekali na „bright and modern” GUI. Wątek zamknięto bez odpowiedzi staffu (https://forums.wynncraft.com/threads/new-gui-changes-and-the-change-in-style.317935/). W wątku o GUI z 2023 GM Samsam101 skrytykował filtr domyślny Content Booka (https://forums.wynncraft.com/threads/gui-design-from-the-recent-update.310058/).
- **Nie znalazłem** devbloga, AMA ani posta, który tłumaczy, dlaczego wybrano scenę zamiast skrzyni. Nie ma też sneak peeków ekranu wyboru przed 2.1.1 (X zwraca tylko post premierowy https://x.com/Wynncraft/status/1856396425602404855).

---

## 4. Przepływ nowego gracza krok po kroku (luka 4)

1. **[F] Wybór hosta.** `play.wynncraft.com` (domyślny), `lobby.wynncraft.com` albo `na/eu/as.wynncraft.com` (https://wynncraft.wiki.gg/wiki/Newcomer's_Guide). Kod Wynntils: „Unless connecting to lobby.wynncraft.com… the hub will just flash by”. Na `play` hub mignie i gracz trafia od razu na świat (`CustomLoadingScreenFeature.java`, klon lokalny).
2. **[F] Pack.** „the official resource pack will automatically start downloading”. Jeśli pobieranie się nie uda, gracz dostaje link do forum (Newcomer's Guide). Ostrzeżenie o braku packa można wyłączyć przez `/toggle rpwarning` (https://wynncraft.wiki.gg/wiki/Commands).
3. **[F] Przejścia między serwerami używają pakietu transferu Minecrafta** (1.20.5+). Wynntils ignoruje rozłączenie z powodem `disconnect.transfer` i rozpoznaje ekran `connect.transferring` (`core/mod/ConnectionManager.java`). Zmianę serwera zapowiada komunikat na czacie „Saving your player data before switching to …”. Podczas ładowania serwer wysyła **title i subtitle** z etapem ładowania (`CustomLoadingScreenFeature.java`).
4. **[F] Konto bez postaci trafia prosto do kreatora, z pominięciem overview.** Wynntils: „the character creation screen which new players will be instantly brought to, the logo text we currently check for does not show on this screen” (https://github.com/Wynntils/Wynntils/pull/3035). Stan „world” Wynntils rozpoznaje po fałszywym graczu na liście Tab o nazwie „Global [NA32]” (`WorldStateModel.java`).
5. **[F] Tworzenie postaci.** Zielony plus, potem klasa z bronią i archetypami, potem zielony checkmark (Newcomer's Guide).
6. **[F] Samouczek „King's Recruit”** (https://wynncraft.wiki.gg/wiki/King's_Recruit, https://wynncraft.wiki.gg/wiki/Tutorial):
   - start w rozbitym wozie na Ragni Outskirts [-1574, 54, -1625]: „Agh! Hey, Player! You alright in there?”,
   - NPC-e Aledar i Tasim,
   - drewniana broń od żołnierza,
   - brama otwierana prawym klikiem,
   - most do pokonania przez ścięcie drzewa,
   - jaskinia Grook's Nest z trzema Angry Grookami,
   - niezidentyfikowany hełm, który trzeba zidentyfikować u Item Identifiera,
   - podpowiedzi ekranowe: „Unlock your first Ability by using your compass and opening the Ability Tree on your left.” oraz „Cast your [First Spell] Spell by clicking [Click Combo]!”,
   - boss Corrupted Goliath,
   - Król Ragni i Ragni Teleportation Scroll,
   - w epilogu Tasim tłumaczy Content Book i śledzenie z beaconem,
   - nagrody: 35 XP, 6 emeraldów, scroll.
7. **[F] `/skiptutorial`.** Przenosi do rozmowy z Królem, ale przy późnym użyciu gracz traci przedmioty samouczka (King's Recruit). Strona Commands opisuje komendę inaczej: „Skips the tutorial if you have completed it”.
8. **[F] Po samouczku** gracz trafia przed pierwsze stoisko nowego sklepu (Reddit, patrz wyżej).
9. **[F] Znany błąd.** Nowy gracz z Wynntils utykał między ekranem packa a kreatorem, bo „A new account joins directly into the character creation screen instead of the selector” (https://github.com/Wynntils/Wynntils/issues/3305, poprawka w PR #3306).
10. **[F] Co widać na overview.** Zrzut z PR Wynntils #3544 (26.09.2025, wersja v2.1.3_9, https://github.com/Wynntils/Wynntils/pull/3544):
    - logo w lewym górnym rogu,
    - **nazwa świata („EU6”) w prawym górnym rogu**,
    - **wersja gry („v2.1.3_9”) w lewym dolnym rogu**,
    - dwie podpowiedzi z ikoną myszy na dole: „Left-Click to play” i „Right-Click to switch”,
    - karta postaci po prawej: badge poziomu w pięciokącie, nick, **ilustracja klasy** (rysunek, nie skórka gracza), 3 ikony archetypów i pasek „Content Completion”,
    - postać gracza we własnej skórce trzyma broń (skin broni), ma cień na posadzce i stoi przed drzwiami z łukiem na tle kolumnady.

    **[W]** Teksty w rogach ekranu potwierdzają pozycjonowanie HUD-u shaderem, które w rundzie 1 było tylko hipotezą.
11. **[F] Wardrobe działa na tej samej zasadzie sceny** (od 2.1.3). Podgląd kosmetyków jest „from a third-person perspective” (wątek 321725). Wynncraft ukrywa wtedy większość UI. Action bar używa osobnego fontu (https://github.com/Wynntils/Wynntils/pull/3348). Gracz nadal trzyma swój przedmiot, a prawy klik wychodzi z Wardrobe (https://github.com/Wynntils/Wynntils/pull/3724).
12. **Disconnect w selektorze i czasy ładowania.** Nie znalazłem oficjalnych danych. Znane są tylko: błąd z rundy 1 (auto-wejście po restarcie), anegdota o „30 s pauzy” i zgłoszenia zawieszeń na „Joining Wynncraft World...” (https://www.reddit.com/r/WynnCraft/comments/1gz7e2q/).

---

## 5. Pozostałe elementy „menu postaci” (luka 5)

### Kompas: Character Info
- **[F]** Dane z Wynntils (`CharacterModel.java`, `GuildModel.java`, `SkillPointModel.java`):
  - slot 7: info o postaci, z tooltipem „Class: …” i „Combat Lv: …”,
  - slot 8: Mastery Tomes,
  - slot 9: Ability Tree,
  - slot 17: profesje,
  - slot 26: gildia, otwiera okno „<gildia>: Manage”,
  - slot 4: tome skill pointów,
  - slot 62: Content Book w części ekwipunku okna, czyli okno jest pełnoekranowe.
- **[F]** W menu kompasu jest też wejście **„Store & Wardrobe”**. Wiki Loot Crates: „use your Character Info compass to open the Store & Wardrobe. From there, select Use Items and then choose Crates” (https://wynncraft.wiki.gg/wiki/Loot_Crates, potwierdzenie w https://github.com/Wynntils/Wynntils/issues/3388).

### Content Book
- **[F]** Okno pełnoekranowe (`ContentBookContainer`):
  - przewijanie przyciskami „Scroll Up” i „Scroll Down” w slotach 65 i 69,
  - **przycisk „Filter” w slocie 66, przełączany klikiem w obie strony**, do 12 filtrów,
  - slot 68: ogólny postęp „X of Y completed”,
  - slot 58: **historia dialogów** ze stronicowaniem „Page X of Y” (`ContentBookQueries.java`, `DialogueHistoryQueries.java`).
- **[F]** Filtry to typy aktywności: Recommended, Quest i Storyline Quest, Mini-Quest, World Event, Secret/World/Territorial Discovery, Cave, Dungeon, Raid, Boss Altar, Lootrun Camp, Mount Enclosure (`ActivityType.java`). Wpis ma w tooltipie długość, trudność, nagrody, wymagania poziomu, profesji i questów oraz stan śledzenia (`ActivityModel.java`, https://wynncraft.wiki.gg/wiki/Content_Book).
- **[F]** Tekstura (Dark Pack powtarza układ oryginału): siatka 6×9, pod nią pasek ikon (strzałki, filtr, odświeżenie, klepsydra), dymek dialogów i symbol oka.

### Ability Tree i aspekty
- **[F]** Aspekty mają osobne okno pełnoekranowe. Przycisk jest w slocie 86 okna drzewka. W oknie aspektów 5 slotów założonych leży w układzie pięciokąta (sloty 18, 11, 4, 15, 26), a lista jest stronicowana przez sloty 57 i 59 (`AspectsContainer.java`, `AspectContainerQueries.java`).
- **[F]** Reset drzewka: shift-klik w slocie 58, przycisk w slocie 85, 3 sockety shardów w slotach 11, 15 i 40, potwierdzenie w slocie 22 (`AbilityTreeContainerQueries.java`).

### Mastery Tomes
- **[F]** Wejście przez Character Info po questline „The Worm Holes”. Jest 14 slotów (https://wynncraft.wiki.gg/wiki/Mastery_Tomes):

  | Kategoria | Sloty | Poziomy |
  |---|---|---|
  | Armour | 4 | 60, 70, 80, 90 |
  | Weapon | 2 | 60, 80 |
  | Marathon | 2 | 60, 80 |
  | Expertise | 2 | 60, 90 |
  | Mysticism | 2 | 70, 100 |
  | Lootrunning | 1 | 75 |
  | Guild | 1 | 105 |

- **[F]** Tekstura okna ułożona jest po okręgu: sloty tomów na pierścieniu i w środku (`textures/font/interface/mastery_tome/mastery_tome.png` w Dark Packu).

### Profesje
- **[F]** 4 zbierackie i 8 rzemieślniczych. Ostatni ważny próg to 115 dla zbierackich i 117 dla rzemieślniczych, maksimum to 132 bez dalszych bonusów (https://wynncraft.wiki.gg/wiki/Professions).
- **[F]** W Character Info slot 17. Tooltip ma format „Lv. N Nazwa [xx%]” z glifem profesji Ⓚ–Ⓛ (`ProfessionModel.java`).
- Wiki nie opisuje układu okna profesji.

### Gildia
- **[F]** Okno „<gildia>: Manage”: członkowie w slocie 0, cele tygodniowe w slocie 13, terytoria w slocie 14, bank gildii w slocie 15, dyplomacja w slocie 26 (`GuildModel.java`, `GuildBankHotkeyFeature.java`, `CustomTerritoryManagementScreenFeature.java`).
- **[F]** Lista członków jest stronicowana i przeszukiwalna. Są też okna logu, odznak i terytoriów (`models/containers/containers/Guild*Container.java`).
- **[F]** Rangi: Recruit, Recruiter, Captain, Strategist, Chief, Owner. Komendy: `/guild manage`, `/guild territory`, `/guild log` (https://wynncraft.wiki.gg/wiki/Guilds).

### Ustawienia (`/toggle`)
- **[F]** Nie ma okna ustawień, są tylko komendy (https://wynncraft.wiki.gg/wiki/Commands): swears, blood, insults, music, vet, war, guildjoin, attacksound, rpwarning, 100, sb (tablica wyników), autotracking, pouchmsg, combatbar, ghosts (gracze z innych światów), popups/guildpopups/friendpopups, beacon/queststartbeacon, bombbell, outlines, pouchpickup.
- **[F]** Muzyka menu i auto-open selektora przełącza się w samym selektorze (runda 1).
- **[F]** Stary opis wiki mówi o jukeboxie „in the bottom left of the skill menu” (wynik wyszukiwania w wiki). Dziś Jukebox jest ikoną w pasku „Use Items” sklepu (zrzut w https://github.com/Wynntils/Wynntils/pull/3987).

### Party i znajomi
- **[F]** Party:
  - obsługa komendami: `/party create|invite|promote|kick|ban|disband`,
  - GUI jest tylko w **Party Finderze** (`/partyfinder`),
  - członkowie drużyny, ich zdrowie i poziom są na tablicy wyników po prawej, a bliscy członkowie mają żółty outline (https://wynncraft.wiki.gg/wiki/Party).
- **[F]** Pack ma okna Party Findera: search, create, join, queue, match_found („Pfinder: Search”, „Parties found”). Wynntils ma `PartyFinderMatchFoundContainer`.
- **[F]** Znajomi: tylko `/friend` z powiadomieniami (friendpopups). Nie znalazłem okna znajomych, a strona wiki Friends nie istnieje (404).

### Ingredient Pouch i Emerald Pouch
- **[F] Ingredient Pouch:**
  - zajmuje slot ekwipunku obok naszyjnika, dostępny od poziomu 1,
  - ma 27 slotów (3×9) poza ekwipunkiem,
  - ikona zmienia wygląd przy stanach 0, 1–5, 6–21 i 22–27,
  - otwiera się lewym klikiem,
  - shift + prawy klik to szybka sprzedaż,
  - autopickup wyłącza się przez `/toggle pouchpickup`,
  - Ironman może trzymać w nim dowolne przedmioty (https://wynncraft.wiki.gg/wiki/Ingredient_Pouch).
- **[F] Emerald Pouch** to zwykły przedmiot w ekwipunku, tiery I–X. Pojemność rośnie od 9 bloków do 4 stacków LE. Sam zbiera i konwertuje emeraldy (https://wynncraft.wiki.gg/wiki/Emerald_Pouch).

### Bank
- **[F]** Wynntils: bank konta ma **21 stron**, bank postaci **12 stron**. Skok do stron przez slot 7, przyciski stron w slotach 51 i 52. Okna są przeszukiwalne (`AccountBankContainer.java`, `CharacterBankContainer.java`, `BankModel.java`). Są też block bank, misc bucket, bookshelf i bank wyspy (`models/containers/containers/personal/`).
- **[F]** Wejście przez ender chesty w bankach miast. Kolejne strony kupuje się za emeraldy, a próg poziomu wynosi od 5 do 100 (https://wynncraft.wiki.gg/wiki/Bank). HERO+ dostaje „+8 Character Bank Pages” (2.1.3).

### Sklep, Wardrobe, kosmetyki
- **[F] 2.1.3** (07.08.2025, https://wynncraft.wiki.gg/wiki/Version_2.1.3):
  - „An in-game store was added that has near-perfect parity with the web store”,
  - „The Use menu was merged into the in-game store, accessed by pressing the plus button”,
  - sortowanie po „recency, alphabetical order, collections, or tier”,
  - podgląd przed zakupem,
  - animacja otwierania skrzynki bez ruletki,
  - usunięto Gold Coin Store,
  - rangi można kupić za Silverbull Shares.
- **[F]** Aby dostać się do przedmiotów, peta i Wardrobe, trzeba kliknąć rozwijaną zakładkę „Use Items”. Wynntils ma cechę, która klika ją automatycznie, bo to „one of these Wynn mysteries. This menu serves no reasonable function as it only hides elements of the UI until clicked on” (https://github.com/Wynntils/Wynntils/pull/3987).
- **[F]** Okna kosmetyków mają tytuły-glify w wariantach per kategoria i stronicowanie przez sloty 51 i 53. Wardrobe trzyma podgląd broni w slocie 6, a kolor glintu w slocie 5 (`CosmeticContainer.java`, `StorePageContainer.java`, `StoreModel.java`). Komenda `/weapons` otwiera menu skinów broni (PR #4080).
- **[F]** Tekstury packa: `store/front` (3 oferty na froncie), `wardrobe` (dwie kolumny 3×4 slotów z ikonami kategorii i pasek nawigacji), `checkout`, `scrap*`, `stash`, `task`.
- **[F]** Crate'y: `/crate`. Tier 1 codziennie dla VIP+, tier 2 dla HERO (Loot Crates).

---

## 6. Inne serwery (luka 6), tylko to, co ma źródło

- **Hypixel SkyBlock** [F] (https://hypixelskyblock.minecraft.wiki/w/Profile):
  - profil to odpowiednik „konta” z własną wyspą, kolekcjami, ekwipunkiem i co-opem,
  - sloty: 1–2 domyślnie, 3. dla VIP, 4. dla MVP+, 5. za 2750 gemów, 6. zarezerwowany na Bingo,
  - tworzenie: przycisk w Profile Management, potem ikona i nazwa, a nazwę losuje się z listy owoców („Apple, Banana… Zucchini”) z możliwością rerollu,
  - tryby: Classic, Ironman, Stranded, Bingo,
  - okno ma kolejne kroki „Choose a SkyBlock Mode”, „Create a Ironman Profile” itd. (https://hypixelskyblock.minecraft.wiki/w/Profile/UI),
  - **[F]** oficjalna wiki Hypixela zakończyła działalność w lipcu 2026 (redirect z wiki.hypixel.net na https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/).
  - **[W]** Tworzenie to czyste okna-skrzynie, bez sceny. Tryby wybiera się przy tworzeniu, tak jak u Wynna.
- **MCC Island** [F] (https://mccisland.wiki/Patch_0.19.0, 17.04.2024):
  - nie ma postaci,
  - „Gone is the Personal Menu, now it's all laid out in the hotbar”: Navigator, Journal (zastąpił Quest Log), Wardrobe, Infinibag, Social/Settings,
  - „Your personal Player Profile and your wardrobe; all rolled into one!”,
  - Outfit Menu zapisuje zestawy kosmetyków w maksymalnie 10 slotach (https://mccisland.wiki/Cosmetics).
  - **[W]** MCCI poszło w stronę przeciwną do naszego „jednego huba pod Q” i rozbiło menu na kilka przedmiotów w hotbarze.
- **Telos Realms** [F]:
  - 6 klas: Knight, Assassin, Necromancer, Samurai, Huntress, Arcanist (https://wiki.telosrealms.com/wiki/Classes),
  - dodatkowe sloty postaci sprzedawane w sklepie („Slots & Storage”, https://store.telosrealms.com/category/slots, strona blokuje pobieranie),
  - po samouczku gracz trafia do „nexus”, menu otwiera się m.in. przez „Adventurer's Obelisk” (https://wiki.telosrealms.com/wiki/Getting_Started),
  - nie udało się potwierdzić, jak wygląda ekran wyboru klasy (istnieje film https://www.youtube.com/watch?v=XLtD6Lu9t9c, bez transkrypcji).
- **Origin Realms.** Wiki (stub) nie wspomina o postaciach ani profilach (https://originrealms.wiki.gg/wiki/Origin_Realms). Nic nie potwierdzono.
- **Mineclub, Fallen Realms, Enchanted Realms.** Nie znalazłem źródeł o wyborze postaci. Pomijam.

---

## 7. Wnioski dla nas [W]

1. **Konto bez postaci od razu do kreatora** (tak robi Wynn). Pomijamy pusty selektor.
2. **Na overview dać kartę z postępem** („Content Completion”) i **ilustrację klasy lub rasy** zamiast samej skórki. To ta sama karta, którą gracze chwalą.
3. **Tła odblokowywane eventami**, np. „wziąłeś udział w festiwalu X”. To tania retencja, bo scenerie Wynna to w połowie nagrody eventowe.
4. **Kolor ikony postaci dostępny bez rangi.** Gracze używają go do porządkowania, a Wynn obniżył próg z HERO do VIP+.
5. **Szybkie usuwanie postaci** (Wynn zszedł do maksymalnie 5 minut) i hojne sloty na start. Obcięcie slotów wywołało burzę i Wynn się z niego wycofał.
6. **Nie chować funkcji za dodatkowym klikiem**, jak „Use Items” u Wynna. Gracze i Wynntils to krytykują.
7. **Samouczek z podpowiedziami ekranowymi wskazującymi konkretne GUI**, np. „use your compass and open the Ability Tree on your left”. U nas to samo z klawiszem Q.
8. **Scena wyboru może później służyć jako scena zapowiedzi** (cinematic Fruma w Character Menu).

---

## 8. Czego nadal nie da się ustalić i dlaczego

- **Pełny changelog 2.1.1 z Discorda.** Wynn publikuje tylko na Discordzie, do którego nie mam dostępu, a nikt go nie zreplikował (sprawdzone: forum, Reddit, wiki, wyszukiwarka). Wiki.gg jest najpełniejszą kopią.
- **Uzasadnienie projektowe sceny zamiast skrzyni, nazwiska autorów konkretnych ekranów, narzędzia GUI.** Brak devblogów i AMA na ten temat. Twórcy (Painters) nie publikują o tym w sieci. Strona rekrutacyjna mówi tylko „image editors”.
- **Mechanizm unieruchomienia kamery w scenie** (spectator, pasażer czy freeze). Nie ma o tym mowy w źródłach. Kod Wynntils tylko to obchodzi.
- **Zachowanie przy disconnect w selektorze i czasy ładowania.** Brak oficjalnych danych, są tylko anegdoty.
- **Pełna treść okna profesji i okna znajomych.** Wiki tego nie opisuje, a strona Friends to 404.
- **Aktualna tabela slotów po 2.1.3.** Wiki (6/…/15) i posty Salteda (nowe konta 5) się różnią. Wiki pewnie pokazuje stary stan albo stan dla starych kont.
- **Reddit.** Pełnotekstowe wyszukiwanie komentarzy w arctic-shift szybko trafia na limity (429, timeouty), więc pokrycie jest wyrywkowe. Posty są kompletne dla dat z 11.2024. Przy 2.1.1 nie było żadnego dużego wątku o ekranie wyboru, więc reakcje są rozproszone w komentarzach.
- **Telos, Origin Realms i inne serwery.** Brak publicznej dokumentacji ekranów postaci. Sklep Telos blokuje pobieranie, a wiki OR to stub.

### Materiały lokalne
- Zrzut ekranu overview: `/tmp/wr/sel3544.png` (z PR #3544).
- Montaż tekstur okien z Dark Packa: `/tmp/wr/montage.png`.
- Skrypty do Reddita (arctic-shift): `/tmp/wr/c.py` (post z drzewem komentarzy), `/tmp/wr/cs2.py` (wyszukiwanie komentarzy), `/tmp/wr/s.sh`.
