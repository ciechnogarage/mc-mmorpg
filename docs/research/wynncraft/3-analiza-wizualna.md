# Wynncraft: kreator i menu postaci. Analiza wizualna (runda 2)

Data: 2026-09-22. Ten raport uzupełnia `wynncraft-kreator-research.md` i nie powtarza go. Oznaczenia: **[Widać]** to coś, co widać na klatce albo w pliku, **[W]** to mój wniosek.

## Źródła

| Kod | Film | Data | Co pokazuje |
|---|---|---|---|
| **A** | https://youtu.be/3nnP-Z70jWY „Wynncraft 2.1.1 Hero Beta – Introduction and Tutorial” (Ender Non), MC 1.21.1 Fabric z Wynntils | 2024-11-10 | overview, selector, kreator klas, potwierdzenie, cutscenka, samouczek, ekwipunek, Character Info, Ability Tree |
| **B** | https://youtu.be/Yly6xzkL1f4 „The HARDEST Challenge in Wynncraft… [1]” (Modcraft Wynn), MC 1.21.11, Lunar, bez Wynntils | 2026-06-17 | **aktualny** kreator: archetypy po lewej, tryby, reskin, nick, potwierdzenie, przejście |
| **C** | https://youtu.be/FyxXU_Hq6GI „Starting From Zero — Wynncraft Completionist” | 2025-06-10 | Edit Icon, przejście do cutscenki (szary pack użytkownika) |
| D | https://youtu.be/CeRkd8u4C4U „Kings Recruit (updated 2025)” | 2025-01-01 | tylko początek samouczka po cutscence |

- Klatki i arkusze kontaktowe są w `/home/dev/.claude/jobs/71303296/tmp/video/` (`fA/`, `fB/`, `fC/`, `sheets/`).
- Wybrane klatki są w `klatki/`.
- Oficjalny pack Wynncrafta z 22.08.2024 (wersja 2.1, **sprzed** 2.1.1) jest w `/home/dev/.claude/jobs/71303296/tmp/official/` (patrz §6).

---

## 1. Overview: ekran po wejściu na świat

Klatka: `01_overview_scena_postac_karta_A0m09.jpg`, A [0:07–0:10](https://youtu.be/3nnP-Z70jWY?t=7).

**[Widać]**
- Kamera stoi na wysokości oczu i patrzy lekko w górę na postać gracza z odległości ok. 3 bloków. Postać stoi na środku, trochę na prawo. Ma skórkę gracza i kosmetyczny hełm (głowa endermana z fioletową koroną).
- Po lewej są kamienne drzwi w łuku. Scena to prawdziwa lokacja: plac z drzewami, domkami i płotem, o zmierzchu.
- W prawym dolnym rogu siedzi mały czarny pet z koroną.
- Na ekranie nie ma ręki gracza, hotbara, pasków HP i many ani paska XP.
- Celownik jest ukryty, ale w A działa Wynntils, który go chowa. W B (bez Wynntils, `10_…`) **celownik jest widoczny**.
- HUD składa się z trzech części:
  - logo WYNNCRAFT „The Minecraft MMORPG” w lewym górnym rogu (ok. 300×120 px przy 720p),
  - karta postaci przy prawej krawędzi, na środku wysokości: numer poziomu w heksagonie, nick, ilustracja klasy, 3 kółka archetypów i ramka „Content Completion” z paskiem,
  - na dole dwa podpisy z ikoną myszy: „Left-Click to play” i „Right-Click to switch”.
- W prawym górnym rogu jest mały napis „WC3”, czyli nazwa świata.

**[W]**
- Gracz nie jest w spectatorze. W spectatorze vanilla nie rysuje celownika, chyba że celuje się w encję, a w B celownik widać na tle nieba. Scroll też nie działałby w spectatorze, bo zmienia tam prędkość lotu, a serwer tego nie widzi.
- Najbardziej prawdopodobny układ:
  - adventure, stała pozycja i pusty hotbar,
  - efekt niewidzialności, bo vanilla nie rysuje ręki niewidzialnego gracza,
  - vanillowy hotbar i paski przezroczyste w packu (w grze rysuje je glif w action barze, §5), więc gdy action bar pokazuje coś innego, „HUD znika”.
- Nie widać kołysania kamery ani ruchu, więc gracz jest zamrożony.
- Postać na scenie to NPC z kopią skórki i ekwipunku gracza.

## 2. Character Selector (prawy klik)

Klatka: `02_selector_ksiega_create_tooltip_A0m12.jpg`, A [0:11–0:13](https://youtu.be/3nnP-Z70jWY?t=11).

**[Widać]**
- Otwiera się okno-skrzynia z tłem w postaci **otwartej księgi z pergaminu**. Nad lewą stroną jest czerwona pieczęć lakowa „W” i zielone liście wystające zza okładki.
- Lewa strona:
  - tytuł „Select Your Character” na naklejonej karteczce,
  - siatka **3 kolumny × 5 rzędów** slotów. Istniejące postacie to ikony broni na pomarańczowym kwadracie (kolor ikony). Wolne sloty to zielone plusy.
- Prawa strona: przycisk-deseczka ze strzałką wstecz i pod nią deseczka z koszem.
- **Pod księgą zamiast ekwipunku gracza** jest pergaminowy pas z pustym paskiem, czyli okno pełnoekranowe.
- Świat w tle jest przyciemniony, a HUD (logo, karta, podpisy) prześwituje pod przyciemnieniem. Wszystko to jest pod oknem.
- Tooltip plusa: „Create a Character / This character slot is available…”. Z F3+H widać `minecraft:potion, 12 component(s)`, więc **przyciski to mikstury z komponentami** (model i kolor).

## 3. Kreator: wybór klasy

Klatki: `03_…`, `04_…`, `05_…`, `10_…`. A [0:15–0:52](https://youtu.be/3nnP-Z70jWY?t=15), B [2:05–2:15](https://youtu.be/Yly6xzkL1f4?t=125).

**[Widać]**
- **Każda klasa ma osobną lokację:**
  - Mage: ośnieżona wioska w stylu Nesaak,
  - Archer: pustynne miasto z palmami (Almuj),
  - Warrior: trawiasty plac z drewnianymi domami,
  - Shaman: dżungla/bagno z totemem,
  - Assassin: ciemny las z drewnianym domem.
- Kamera jest zawsze ok. 4 bloki od NPC, na wysokości oczu.
- NPC klasy ma **wspólną, generyczną skórkę** (nie gracza) i broń klasy. W pętli odgrywa spelle z pełnymi efektami:
  - Mage skacze (Teleport), macha kosturem i sypie niebieskimi kwadratowymi cząsteczkami,
  - Archer odskakuje, a pod nim wybucha zielony pierścień z cząsteczek na ziemi,
  - Assassin ma wirujący różowo-pomarańczowy okrąg (Spin Attack).
- Efekty wypełniają pół ekranu i nachodzą na HUD.
- **Przejście między klasami:** ok. 1 s ściemnienia do czerni (A 0:22, 0:30, 0:36, 0:43, 0:49). Za czernią zmienia się lokacja. Na klatce 0:22 przyciemnione jest **wszystko, łącznie z logo i kartą**.
- HUD kreatora:
  - logo w lewym górnym rogu,
  - karta klasy po prawej. Ramka ma kolor klasy (turkus Mage, fiolet Archer, czerwień Warrior, zieleń Shaman, niebieski Assassin), jest napis „The Mage”, archetypy i pusta ramka,
  - na dole **karuzela glifów**: pomarańczowa strzałka ←, dwie sąsiednie bronie w szarości, broń wybranej klasy powiększona na środku, strzałka →,
  - pod karuzelą 3 podpisy: „Left-Click to select”, „Scroll up/down to browse”, „Right-Click to return”.
- **Nowość w 2026 (B, `10_…`)**, której nie ma w A (2024):
  - **lista archetypów po lewej, na środku wysokości**: ikona w okrągłym kamieniu, nazwa w kolorze i opis, np. „Light Bender / Grow from healing”, „Riftwalker / Bend reality and ramp up”, „Arcanist / Uncontrollable chaotic power”,
  - **napis „Create a Character” u góry na środku**, na miejscu bossbara.

**[W]**
- Archetypy po lewej to font `center_left`, przesuwany shaderem (§6.2). Napis u góry to bossbar z przezroczystym paskiem.
- Scroll to zmiana slotu hotbara (`PlayerItemHeldEvent`) i przestawienie gracza w inną lokację pod ściemnieniem.

## 4. Kreator: księga „Create Character”, tryby, ikona, potwierdzenie

Klatki: `06_…`, `11_…`, `12_…`, `13_…`, `15_…`. A [0:53–0:55](https://youtu.be/3nnP-Z70jWY?t=53), B [2:16–2:44](https://youtu.be/Yly6xzkL1f4?t=136), C [0:29–0:38](https://youtu.be/FyxXU_Hq6GI?t=29).

**[Widać]**
- **Księga (`06_…`):** na lewej stronie jest czerwona zakładka-wstążka z napisem „Create Character”. Przyciski to drewniane deseczki szerokości ok. 3 slotów, z ikoną i napisem:
  - lewa strona: „Select Gamemodes” (miecz), „Select Reskin” (ludzik), „Set Nickname” (ołówek),
  - prawa strona: „Edit Icon” (pędzel), zielony ✓ i czerwony ✗,
  - w B (2026) jest jeszcze przycisk „!”, czyli opis klasy.
- Z boku księgi wystają czerwone klamry.
- Za oknem **świat dalej się animuje**: cząsteczki spelli Mage'a są widoczne pod przyciemnieniem.
- **Tryby (`11_…`):** osobna strona pergaminu (sama kartka, nie księga) z nagłówkiem-deseczką „Select Gamemodes”.
  - Cztery deseczki z ikonami: Hardcore (czerwona czaszka), Craftsman (niebieski kilof), Ironman (tarcza), Hunted (sztylet). Brak wymagań oznacza wyszarzony przycisk.
  - Tooltip pokazuje zasady, wymaganie z ✔ lub ✘ („Lv. 50+ Character”, „Quest: A Hunter's Calling”) i listę stanów, np. „Disabled / Ironman / Ultimate Ironman”.
  - Klik przełącza stan, a lista stanów cyklicznie się podświetla.
  - Na dole jest ✓ z tooltipem „Apply Gamemodes” i podsumowaniem.
- **Reskin (B 2:41):** tooltip „Class Reskin: Classic / Dark Wizard [VIP+]”, „Click to toggle”.
- **Nickname (B 2:33):** tooltip „Edit Nickname”. Po kliknięciu okno się zamyka i na czacie pojawia się prośba „Type in chat the new nickname you would like to use or 'cancel' (20 max characters)”. **Nick wpisuje się na czacie, nie w oknie.**
- **Opis klasy (`12_…`):** tooltip „Dark Wizard” z tekstem fabularnym, listą archetypów w kolorach i linkiem „wynn.gg/classes (Click to visit)”.
- **Edit Icon (`15_…`, C):**
  - siatka 3×3 kolorów podstawowych: szary, czerwony, pomarańczowy, żółty, zielony, cyjan, niebieski, fiolet, róż,
  - pod nią pasek 5 odcieni wybranego koloru,
  - obok podgląd ikony, czyli broń na kolorowym tle,
  - po lewej przycisk wstecz.
- **Potwierdzenie (`13_…`):** tooltip ✓ „Confirm and Create… Class: [ikona] Dark Wizard, Nickname: …, Click to Create”.

**[W]** Przełączanie stanu to podmiana modelu lub koloru mikstury w tym samym slocie. Wystarczy `SET_SLOT`, bez ponownego otwierania okna.

## 5. Przejście do gry i cutscenka

Klatki: `07_…`, `08_…`, `09_…`, `14_…`. A [0:55–1:19](https://youtu.be/3nnP-Z70jWY?t=55), B [2:44–3:05](https://youtu.be/Yly6xzkL1f4?t=164).

**[Widać]**
- **A (2024):** po ✓ ekran przez ok. 3 s jest **całkowicie czarny, także bez HUD-u** (`07_…`). Czat „Welcome to Wynncraft!” jest widoczny **na** czerni.
- Potem jest **cutscenka z płynnym lotem kamery**:
  - wiatrak,
  - most nad rzeką z kurą,
  - dom z banerem Ragni,
  - kolejne ujęcia oddzielone ok. 1 s czerni.

  Podczas cutscenki nie ma HUD-u ani ręki.
- Potem na czerni pojawia się **logo WYNNCRAFT**, które wjeżdża i znika (`09_…`, 1:13–1:18). Na koniec obraz rozjaśnia się do pierwszej osoby w wozie karawany i pojawia się HUD gry.
- **B (2026):** po ✓ **świat robi się czarny, ale HUD kreatora zostaje na wierzchu** (logo, archetypy, karta, karuzela, `14_…`, 2:45–2:47). Dalej jest ta sama cutscenka (wiatrak, most, kura, dom Ragni) z czarnymi cięciami. Bossbar „LV. 69 – SOVEREIGN” zostaje u góry.
- Wejście do kreatora z gry (B 2:01–2:04): świat ciemnieje przez ok. 3 s, a **hotbar i paski gry są dalej widoczne**. Dopiero potem pojawia się scena kreatora.

**[W]**
- W A czerń to tytuł z pełnoekranowym glifem. Pack ma do tego trzy glify:
  - `Á` w foncie `default`: czarna tekstura 256², `height 1024`, `ascent 512`,
  - `screen:` flash (biały),
  - `screen:` trans (biały, alfa 90), 2048 px.

  Tytuł zakrywa action bar (HUD), a czat rysuje się nad tytułem. Dokładnie to widać na klatce.
- Wersja 2026 (świat czarny, HUD nad nim) oznacza, że zmienili warstwę:
  - albo glif fade'a jest spychany shaderem pod HUD (`anchorZ()` w `text.vsh`, §6.2),
  - albo jest to efekt Blindness/Darkness.

  Z samych klatek nie da się tego rozstrzygnąć.
- Logo, które „wjeżdża”, to glif `screen/wynncraft.png` z pikselami o alfie 2. Shader `verticalSlide` przesuwa je w pionie proporcjonalnie do alfy wierzchołka, czyli do fade-in tytułu. **Animacja ruchu bez żadnych pakietów, tylko fade tytułu.**
- Cutscenka to spectator albo kamera na encji poruszanej po ścieżce. Nie widać ręki ani celownika. Cięcia są maskowane czernią.

## 6. Pack: tekstury, geometria, shadery

### 6.1 Oficjalny pack zdobyty

- **[Widać]** Społeczność trzyma listę URL-i packów: `https://raw.githubusercontent.com/quartzexpressDEV/wynncraft-resourcepacks/main/packs.json` (ostatnia aktualizacja 12.2024).
- CDN `rp-cdn.wynncraft.com/PRODUCTION_<sha1>.zip` **zwraca 403 bez nagłówka `User-Agent: Minecraft Java/…`, a z nim 200**.
- Pobrałem `PRODUCTION_50b959e3…zip`: „Wynncraft Resource Pack 2.1, 1.21.1, Aug. 22 2024”, 8,6 MB, 11 688 plików.
- Zip jest **celowo uszkodzony**: `unzip` pada na „overlapped components / zip bomb”, a Python `zipfile` czyta go poprawnie.
- Pack jest sprzed 2.1.1, więc nie ma `character_select`. Są w nim za to Character Info, Ability Tree, HUD, glify ekranowe i core shadery.
- Nowszego SHA nie znalazłem. Serwer podaje URL dopiero przy logowaniu, a do tego trzeba konta premium.

**Różnica stylu [Widać]:**
- Dark Pack (pierwszy raport) jest ciemnofioletowy: tło `#2A1F3D`, ramki `#4928…`/`#975DE4`, sloty `#572D8F` z jaśniejszą obwódką. Ma breadcrumby „/Class └ Character Select”.
- **Oficjalny styl na nagraniach jest zupełnie inny:**
  - selektor i kreator to **pergaminowa księga/kartka** z pieczęcią, wstążką, liśćmi i drewnianymi deseczkami,
  - Character Info to **zwój pergaminu z dwoma wałkami** (`20_…`),
  - Ability Tree to **kamienna tablica z runami** na górnej i dolnej listwie, ze spękaniami (`21_…`).
- W teksturach Dark Packa zostały „resztki” beżu i brązu oryginału (`(215,193,145)`, `(236,215,168)`, `(79,52,44)` na dolnych krawędziach `select.png`, `cancel.png`, `customize.png`). To potwierdza, że Dark Pack przemalował oficjalne pergaminowe tekstury.
- Breadcrumbów „/Class └ …” **nie ma w oficjalnym wyglądzie**. To dodatek Dark Packa.
- Rozmiar piksela: wszystkie panele są rysowane 1 teksel = 1 px GUI. Kontur 1 px jest ciemny (`#3B2A1E` w pergaminie, `#141119` w Dark), a cieniowanie ma 2–3 stopnie jasności. Styl jest płaski, bez gradientów.

**Geometria paneli ([Widać] w pikselach tekstury, [W] w przeliczeniu):**

| Tekstura (Dark) | Szerokość panelu | Lewa krawędź | Advance spacji z Wynntils | Lewa krawędź = |
|---|---|---|---|---|
| select `` | 176 | x=35 | −43 | 8−43 = **−35 → leftPos** ✔ |
| create `` | 176 | x=40 | −48 | 8−48 = −40 → leftPos ✔ |
| customize | 176 | x=40 | ? | [W] −48 |
| gamemode | 176 | x=10 | ? | [W] −18 |
| character_info (oficjalny) | 232 (zwój szerszy od skrzyni o 28+28) | 0 | −36 | zwój wystaje 28 px poza skrzynię z każdej strony |
| ability_tree (oficjalny) | 204 | 0 | −22 | wystaje 14 px z każdej strony |

**[W] Wzór na pozycję slotu w teksturze:**
- oś X: `tex_x = 8 − advance + 18·kolumna` (ramka slotu o 1 px wcześniej). Dla `select.png` wychodzi dokładnie: kolumny ramek przy x = 42, 60, 78, …, a przyciski po prawej w kolumnach 6–8.
- oś Y, zmierzona: wnętrza slotów siatki postaci zaczynają się przy y = 65, 83, 101, 119, 137, co daje **5 rzędów po 18 px**. Wynntils mówi, że to rzędy 1–5 skrzyni, więc górna krawędź glifu jest na `topPos − 29`, a panel (y=7) zaczyna się 22 px **nad** skrzynią i kończy przy y=155, czyli na dole górnej części 6-rzędowej skrzyni (`topPos+126`).
- Ascent z `interface_disabled.json` Dark Packa (64) do tego nie pasuje. Z geometrii wychodzi efektywnie ok. 42, więc ten plik JSON jest nieaktualny (nazwa „disabled”). Dark Pack nie nadpisuje fontu, tylko tekstury.
- **Mierzyć trzeba z tekstury i z Wynntils, nie z tego JSON-a.**
- `create.png` kończy się przy y=138, czyli [W] okno kreatora ma 5 rzędów (dolna krawędź w `topPos+17+5·18`).

### 6.2 Core shadery tekstu: jak Wynn przypina HUD do krawędzi (nowe względem 1. raportu)

**[Widać]** Plik `official/p/assets/minecraft/shaders/include/text.glsl` plus `core/render/text.vsh`. Mechanizm to **„magiczna alfa” w teksturze glifu**:

- `offset(3, vec2(0,0), vec2(0,1), true)`: każdy teksel glifu z **alfą = 3/255** przesuwa wierzchołki o `screenOffset.y += 1` w clip space, czyli o pół ekranu w górę. Tak glif z action bara ląduje przy górnej krawędzi (np. `special.png`, 58 tys. pikseli z alfą 3).
- Parametr `align` dodaje 1 px przy nieparzystej szerokości ekranu, żeby nie rozmywało pikseli.
- `verticalSlide(2, Color.a)`: teksele z **alfą 2** jadą w pionie o `alfa·15 − 10`. To animacja logo i napisów death/warp przy fade tytułu.
- `atlasOffset(vec4(44,255,0,199), …)` rozpoznaje **konkretny atlas fontu** po kolorze piksela (6,0) atlasu i przesuwa cały ten font.
- `anchorZ(1.0, 2200.03, -1)` (1.20.5+): glify rysowane na głębokości 2200.03 dostają z = −1, czyli przestawiają warstwę rysowania.
- `hideScoreboardNumbers(...)` wyrzuca czerwone numery scoreboardu poza ekran.
- W `font/hud/default/top_right.json` glify lokalizacji mają `ascent: −67`, a marker `ascent: −2048`. Ujemny ascent spycha glif w dół pod linię bazową, a shader przestawia go do prawego górnego rogu. Tak powstaje pasek współrzędnych w prawym górnym rogu na `16_…`.
- Są też shadery `rendertype_gui`, `rendertype_glint*` i `position_tex/color`: kolor glinta, czyli świecenie przedmiotów w kolorze rzadkości.

**[W]** Dla nas: przypięcie tekstu do rogu nie wymaga kodu per element. Wystarczy jeden `rendertype_text` z regułą „alfa = N → przesunięcie”, a grafik oznacza piksele w teksturze. Ryzyko jest takie jak w 1. raporcie: wersje MC i shaderpacki Iris.

### 6.3 HUD gry z tekstur (`23_…`, `16_…`)

**[Widać]**
- `hotbar.png` (208×64): drewniana listwa z zieloną liną i pnączami oraz chorągiewki na końcach. Na ekranie obejmuje 9 slotów hotbara.
- Zdrowie: czerwony pasek w drewnianej ramce z sercem. Tło ma 4 warianty serca: czerwone, fioletowe, szare, złote.
- Mana: niebieski pasek po prawej.
- Poziom: złota tarcza z cyfrą na środku między paskami.
- `meter.png`: heksagony zielony/żółty/czerwony/niebieski w 10 stanach wypełnienia. [W] To meter ultimate'a lub zasobu klasy.
- `spell.png`: ikony myszy LPM i PPM do click-combo.
- Cyfry mają osobne fonty `level` (pogrubione) i `display` (z k/m/b/t).
- Na ekranie gry jest też:
  - hologram „Soldier / [mysz] Right-Click with an Empty Hand to Interact” nad NPC,
  - zielony romb-znacznik questa z wiązką w niebo i dystansem „66M”,
  - sidebar „play.wynncraft.com / Tracked Quest”,
  - pasek współrzędnych w prawym górnym rogu.

## 7. Menu postaci w grze

- **Ekwipunek E (`17_…`)** [Widać]:
  - pergaminowe okno z pnączami,
  - paperdoll z ikonami slotów zbroi po lewej,
  - pod nim rząd 4 okrągłych slotów akcesoriów (tooltip „Accessory Slot”),
  - 2×2 crafting ze strzałką,
  - pusty pergamin w miejscu 27 slotów,
  - pas hotbara: kompas, sakiewka i księga w ostatnich slotach.
  - Efekt „Slowness 27:16” jest w osobnej ramce obok okna.
- **Character Info (`18_…`)** [Widać]: otwierane z kompasu. Zwój ma po lewej ramkę z przyciskiem Ability Tree, w środku „kartkę” z 5 księgami skill pointów, po prawej prostokąt. Pod zwojem jest pergamin z ekwipunkiem (tu wiersz slotów ekwipunku gracza jest widoczny, to nie okno pełnoekranowe). Samouczek zaznacza slot **animowanym czerwonym okręgiem** narysowanym wokół przedmiotu. [W] To podmieniany model przedmiotu albo animowana tekstura `mcmeta`.
- **Ability Tree (`19_…`)** [Widać]:
  - kamienna tablica na cały ekran,
  - górna część (skrzynia) to drzewko: turkusowe węzły połączone ścieżkami, odblokowany Meteor na górze,
  - dolna część (w miejscu ekwipunku) to 3 gniazda na shardy (kształty łez), rząd przycisków (strzałka, ikony archetypów, „Y”, czerwone ↻ reset) i przycisk stron,
  - tooltip węzła: „Meteor / Click Combo: RIGHT-LEFT-LEFT / Mana Cost / Total Damage 400% / Range / AoE / Ability Points: 1 / Right-Click to undo”.
  - Węzły też są `minecraft:potion`.

## 8. Najważniejsze wnioski do przeniesienia (nowe względem rundy 1)

1. **Gracz w kreatorze nie jest w spectatorze.** Celownik jest widoczny (B), a scroll musi docierać do serwera. [W] Receptura: adventure, zamrożenie, niewidzialność (brak ręki), pusty hotbar i przezroczysty vanillowy HUD w packu.
2. **Każda klasa to osobna lokacja i NPC odgrywający spelle w pętli.** Przejście to 1 s czerni z teleportem pod spodem.
3. **Fade to tytuł z czarnym glifem `Á`** (height 1024). Czat jest nad nim, action bar pod nim. W 2026 HUD kreatora zostaje nad czernią (zmiana warstwy).
4. **Animacja ruchu tylko przez fade tytułu:** alfa 2 w teksturze i `verticalSlide` w shaderze.
5. **HUD w rogach = „magiczna alfa” teksela** (3 → góra ekranu) plus rozpoznawanie atlasu po pikselu. Kod shadera jest w pobranym packu.
6. **Styl oficjalny jest pergaminowo-drewniano-kamienny**, nie fioletowy (to Dark Pack). Każde okno to inny przedmiot: księga, kartka, zwój, tablica. Nie ma jednego uniwersalnego panelu.
7. **Nick wpisuje się na czacie**, nie w oknie (Wynn nie używa kowadła ani dialogu).
8. **Edit Icon:** 9 kolorów × 5 odcieni i podgląd. Dobry, tani wzór dla nas (tint jednego modelu).
9. **Deseczki-przyciski ok. 3 sloty** z ikoną i napisem w teksturze. Sąsiednie sloty są puste lub przezroczyste.
10. **Po utworzeniu postaci: cutscenka** (lot kamery po 4–5 ujęciach z czarnymi cięciami), logo na czerni, potem gra. To buduje „wejście w świat”.

## Czego nie ustaliłem

- Na żadnym nagraniu nie ma **Wardrobe** ani **usuwania postaci**. Szukałem: „wynncraft wardrobe 2.1.3”, „cosmetics preview”.
- Nie widziałem overview w wersji 2026: B wchodzi prosto do kreatora.
- Nie mam packa po 2.1.1. Oficjalne tekstury `character_select` znam tylko z nagrań.
- Nie rozstrzygnąłem, czy czerń w 2026 to glif spchnięty shaderem, czy Blindness.
