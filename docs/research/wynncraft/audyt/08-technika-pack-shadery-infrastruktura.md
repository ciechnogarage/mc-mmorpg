# 08. Technika: resource pack, modele, shadery, VFX, muzyka, infrastruktura, API

Źródła:
- oficjalny pack `PRODUCTION_73144e484c32f6ba6c55785def88bc726bfd9d52.zip` (z listy quartzexpressDEV, CDN rp-cdn.wynncraft.com, nagłówek `User-Agent: Minecraft Java/1.21.11`), rozpakowany w `tmp/wynn-audit/pack/x73144/`,
- packi społeczności nadpisujące aktualne ścieżki: WynnEdits Dark Theme 3.5 (05.08.2026), „(50% VFX) Vulkan fix” 0.4.1 (17.06.2026, format 33–108), Wynnilla UI 1.1.0 (14.09.2026), Wynngineer's Vision 2.0.4 (09.09.2026),
- Wynntils (kod + Static-Storage), API v3 i dokumentacja (https://docs.wynncraft.com, OpenAPI 3.1).

---

## 1. Który pack udało się zdobyć

- **[F]** Lista społeczności `packs.json` (ostatni commit 07.12.2024) ma dwa najnowsze adresy CDN: `…50b959e3…zip` (runda 3) i `…73144e48…zip`. Pobrałem drugi: **200 OK, 8 570 368 B**. Ma jednak ten sam opis co w rundzie 3: „Wynncraft Resource Pack 2.1, 1.21.1, Aug. 22, 2024” (`pack_format 34`, obsługa 18–34, overlay `1_20_5` dla formatów 32–34).
- **[F]** Nowszego adresu nie ma nigdzie publicznie: wyszukiwarka nie zna ciągu „rp-cdn.wynncraft.com”, a klon Wynntils go nie zawiera. **Aktualny pack 2.2.4 wysyła serwer dopiero zalogowanemu graczowi** (`01-…`, §1).
- **[F]** Zip jest **celowo uszkodzony**: nazwy plików w lokalnych nagłówkach są wyczyszczone (`File name in directory 'pack.mcmeta' and header b'' differ`). Nawet Python `zipfile.read()` pada. Pomogło czytanie danych po offsetach z katalogu centralnego (`skrypty/rzip.py`, 11 511 plików, 0 błędów). **[W]** To ochrona przed kopiowaniem packa zwykłymi narzędziami. Minecraft czyta katalog centralny, więc gra działa.
- **[F]** Stan aktualny (2.2.x) znam pośrednio z packów, które nadpisują te same ścieżki (niżej §4).

## 2. Inwentarz oficjalnego packa 2.1 (11 511 plików)

**[F]** (skrypt, `pack/x73144/assets/minecraft`)

| Katalog | Pliki | Rozmiar | Zawartość |
|---|---|---|---|
| models | 8 364 json | 48 MB | 6 825 w katalogu głównym `models/w*.json` (patrz §3), 1 272 w `models/item/wynn/` (gui 372, skin 367, jigsaw 118, weapon 101, economy 79, housing 66, accessory 48, prop 28, template 19, spell 13, pouch 11, powder 10…), bloki i warianty |
| textures | 3 012 (2 756 png + 256 mcmeta) | 16 MB | 256 animacji `.mcmeta` |
| font | 30 json (+103 png w textures/font) | 180 KB | fonty HUD, interfejsu, ekranu, spacji |
| shaders | 27 (6 fsh, 5 vsh, 7 glsl, 9 json) | 128 KB | core: text, gui, item, glint, entity_solid, position_*; post: entity_outline |
| sounds | 19 ogg + sounds.json (93 zdarzenia) | 240 KB | instrumenty i nadpisania |
| blockstates | 48 | | warianty bloków |
| lang | 5 (en_us, en_gb, en_au, en_ca, en_nz) | | |
| atlases | 1 | | |

### Modele przedmiotów (przed 1.21.4, czyli predykaty `overrides`)
- **[F]** Suma overridów: 8 776. Najwięcej:
  - `wooden_pickaxe` **6 823** (wszystkie → `w1…w6823`),
  - `iron_horse_armor` 646 (skiny 364, broń 101, housing 48, akcesoria 48, propy 27, spelle 13),
  - `potion` 307 (GUI: przyciski drzewka, reset itd.),
  - `diamond_shovel` 200 (skiny, predykat `damage`),
  - `paper` 101 (ekonomia 76, powdery 10, runy 5…).
- **[F]** API v3 opisuje ikony w **nowym formacie 1.21.4+**: `{"id":"minecraft:potion","customModelData":{"rangeDispatch":[4345]}}`, a czasem z `"select":["item_tier_fabled"]` (np. Charm of the Void). Ikony: 4 558 przedmiotów to `minecraft:potion`, ~2 060 to farbowana skóra (`leather_*` z `customModelData.rangeDispatch`), 103 to głowy z teksturą (`format: skin`). **[W]** Obecny pack używa definicji modeli przedmiotów 1.21.4 (`items/*.json`) z `range_dispatch` na float CMD (numer modelu) i `select` na string CMD (np. tier dla koloru ramki). Pozostają dwa bazowe przedmioty: mikstura (kolor przez `potion_contents`) i skórzana zbroja (kolor przez dye).

## 3. Modele mobów i efektów: „kości” jako przedmioty

- **[F]** 6 823 modele `w<n>.json` mają zanonimizowane nazwy i tekstury `item/w<hash>_0`. Każdy ma `display.head` ze skalą **4** i translacją `[80, 73.6, 80]` oraz `display.gui` z translacją 24. Mediana: **1 element** (5 714 modeli ma 1 sześcian), maksimum 48.
- **[W]** To eksport w stylu ModelEngine (kość = osobny model na głowie niewidzialnego armor standa, skala 4). Wynn ma własny system modeli mobów, zbudowany jak ModelEngine. Nazwy są zahaszowane, żeby nie dało się łatwo wyciągnąć modeli. Salted w 2023: efekty to „invisible mobs and armor stands stitched together” (runda 2).
- **[F]** Vulkan fix (2026) nadpisuje **1 001 tekstur `textures/item/w<liczba 64-bit>.png`**:
  - rozmiary: 128² ×278, 256² ×207, 64² ×207, 16² ×127, 32² ×90,
  - paski klatek: 64×448 ×11, 64×1408 ×4, 256×2048 ×2, 256×4352 ×2,
  - 46 plików `.mcmeta` (animacje).

  Montaż (`klatki/P_vulkan_vfx_montaz.png`): **płaskie sprite'y efektów spelli**, czyli pierścienie, łuki cięć, runy, kręgi magiczne, iskry, płomienie w pixel-arcie. **[W]** W 2.2 tekstury nazywają się już po hashu, a nie po ścieżce. Obfuskacja objęła cały pack.
- **[F]** „Abilities no longer use particles but instead unique VFX animations” (Version 2.2.0). Wynntils `item_display_model_data.json` wymienia sekwencje modeli dla `item_display`: „Judrajim” 106 klatek (CMD 3916–4021), „Angelic Ascension Guardian Angels” 14, „Mantle” 6, „Arrow Shield” 4, „Meteor Trail” 3, „Meteor Ball” 1.
- **[W]** VFX w Wynnie to animacja poklatkowa: serwer co tick podmienia model (CMD) na `item_display`, a do tego dochodzą animowane tekstury `.mcmeta` i transformacje display (obrót, skala). Na klatkach ultimate'ów (`klatki/V22_…`, `V23_…`) widać płaskie, pikselowe kształty: słońce, gigantyczny miecz, kolce. Nie ma tam klasycznych cząsteczek. U nas to samo zrobi MythicMobs/ModelEngine albo Nexo + `ItemDisplay` z transformacjami. Uwaga na koszt: 106 klatek to 106 modeli.
- **[F]** Vulkan fix istnieje, bo pack Wynna źle się renderuje na VulkanMod (modzie z rendererem Vulkan). „50% VFX” sugeruje przezroczystość efektów zmniejszoną o połowę. **[W]** Efekty wypełniające pół ekranu męczą graczy (runda 3: „efekty nachodzą na HUD”).

## 4. Aktualna struktura packa (2.2.x) według packów społeczności

**[F]** WynnEdits 3.5 (2 068 plików; 1 281 w `textures/wynn`, 297 w `textures/font`, 294 w `textures/gui`):
- `textures/wynn/gui/<okno>/…` (tekstury elementów okien i przycisków): menu_component, button (z `color_palettes` do tintowania), blacksmith, scavenger, character_select (icon), account_link, content_book, lootrun (trial/mission/inspect/boon), trade_market, slot, item_upgrader, store (progress, glint, consumable, checkout, silverbull, bundle, crate, mount, shop, egg, list, task, effect, rank, banner), layer (durability, state, use), item_identifier, player, character_info, party_finder, event (spirits, blizzard + minigame, bonfire, heroes), fruman_underground, mastery_tome, crafting, beacon, bank, raid, powder_master, update, ability_tree (branch, button, archetype/<klasa>, node, aspect, **ultimate**),
- `textures/font/`: interface (87 teł okien), tooltip (divider, emblem, requirement, attribute, identification), nameplate (7 stylów), hud (dialogue, gameplay, selector), event, offset, tile, overlay (pause).
- **[F]** Katalogi `color_palettes` przy przyciskach, sklepie, wierzchowcach, jajkach, efektach i banerach. **[W]** To palety do tintowania jednego modelu w wielu kolorach (definicje modeli 1.21.4 z `tints`), spójne z runda 4, §4.2.
- **[F]** Wynnilla UI (09.2026) dodaje własny namespace `wynnilla` z fontami `widget`, `bar`, `center` i `respackopts.json5` (konfiguracja packa przez mod Respackopts). **[W]** Społeczność robi „UI-mody” samymi packami. Oficjalny pack musi być na to odporny.

## 5. Shadery (pack 2.1): co nowego względem rundy 3

Runda 3 opisała `text.vsh/text.glsl` (magiczna alfa, verticalSlide, atlasOffset, hideScoreboardNumbers). Tu dochodzą:
- **[F]** `include/item.glsl` (rendertype_entity_translucent_cull):
  - `emissiveModel()`: teksel z **alfą 254/255 świeci** (pomija światło otoczenia i cieniowanie),
  - `perspectiveModel(253, 252)`: teksele z alfą 253 są wyrzucane w świecie, a z alfą 252 w GUI. Jeden model wygląda więc inaczej w ręce i świecie, a inaczej w ekwipunku (np. ikona 2D w slocie, bryła 3D w świecie),
  - `anchorZ(551, 2201)` przesuwa warstwę rysowania przedmiotów.
- **[F]** `include/gui.glsl` (rendertype_gui, position_color):
  - `disableBadges(2)` zeruje kolor wierzchołków w 6 konkretnych kolorach (0xE6AF49, 0x77B3E9, 0xA0A0A0, 0xE84F58, 0xEAC864, 0xCFCFCF) przy lewej krawędzi. **[W]** Chowa vanillowe znaczki GUI, prawdopodobnie ikony pakietów/wersji lub „badges” na liście serwerów,
  - `disableTooltips()` ukrywa tooltip wychodzący poza ekran.
- **[F]** `core/render/texture.vsh`: `anchorZ(400, 1300)`. `include/glint.glsl`: własny glint z „edge” (sinusoidalne wygaszenie krawędzi) i kolorami rzadkości. `post/entity_outline.json`: własny outline (żółty kontur członków party). `core/rendertype_entity_solid.fsh`: wycinanie alfy < 0,1.
- **[W]** Wynn używa shaderów do trzech rzeczy: pozycjonowania HUD (text), podmiany warstw (anchorZ) i dwóch wyglądów modelu (perspectiveModel). Magiczna alfa 252–254 w teksturach to konwencja, którą grafik musi znać. U nas: MythicHUD ma `rendertype_text` (runda 4). `perspectiveModel` da się dziś zrobić **bez shadera** definicją modelu 1.21.4 (`minecraft:select` po `display_context`: gui / ground / hand). To lepsza droga.

## 6. Dźwięk i muzyka

- **[F]** 93 zdarzenia w `sounds.json`:
  - 31 `music.*` → cisza (`other/blank`),
  - 17 `wynn.instrument.*` (samplowane instrumenty),
  - instrumenty note block podmienione na własne sample,
  - kilka `wynn.world/ui/npc.*` na vanillowych samplach,
  - tylko 19 plików ogg w packu.
- **[F]** „music, composed of noteblocks” (wiki Music). Fruma: „over 70 new music tracks”. Zespół ma 3 kompozytorów (runda 2).
- **[W]** Serwer odtwarza nuty pakietami `sound` na instrumentach z packa. Utwory nie ważą nic w packu, zmieniają się bez aktualizacji packa i mogą reagować na strefę. U nas: NoteBlockAPI (plugin, pliki .nbs) + instrumenty w Nexo. Do sprawdzenia w docs NoteBlockAPI: obsługa custom instrumentów i wersji 1.21.x.

## 7. Techniki świata (z klatek i Wynntils)

- **[F]** Wynntils rozpoznaje stan gry prawie wyłącznie po tekście w **action barze** i **bossbarach**: HUD, dialogi, spelle, ultimate, energia wierzchowca, cel lootrunu (`handlers/actionbar`, `handlers/bossbar`). Scoreboard to fałszywi gracze (wywiad Salteda, runda 2).
- **[F]** Napisy 3D przy lokacjach (`klatki/V07_…`), hologramy z ikoną myszy, beacony questów i lootrunów jako kolorowe wiązki. **[W]** Wiązki to prawdopodobnie `block_display`/`item_display` z wydłużonym modelem albo prawdziwe beacon beamy klienta. Beacon beam pozostaje widoczny z daleka, `display` ma `view_range`.
- **[F]** Gracz w kreatorze i scenach nie jest w spectatorze (runda 3). Cutscenki to kamera na ścieżce z czarnymi cięciami i pominięciem klawiszem F.

## 8. Infrastruktura i skala

- **[F]** Jedno proxy „WynnProxy” (własne) dla 5 nazw hostów. Globalny limit 1 500. 52 aktywne światy o nazwach NA/EU/AS + numer. Mediana 13 graczy na świat (`01-…`, §2).
- **[F]** Przejście między serwerami idzie **pakietem transfer** (1.20.5+), a hub „mignie” przy wejściu przez `play` (runda 2). Rangi mają `/switch` i priorytety kolejki.
- **[F]** Koszt: „hosting and development costs alone are over $40,000 a month” (Salted cytowany w filmie https://youtu.be/mDMeTvvmop4).
- **[F]** Wydajność: „80% of the time when we were struggling with Minecraft it's about optimization” (Salted 2023, runda 2). 2.2.4: „Changed the rain clouds of That Which Conglomerates to use a model instead of particles and reduced the amount of rain particles to help with performance”. Gracze: „Fruma of course made all the lag worse too even though they said it'd improve performance” (Reddit 1t3kq50). Wojny i Annihilation to znane źródła laga.
- **[F]** Kadencja wydań: 2.2.0 (04.04.2026), 2.2.1 (czerwiec), 2.2.2, 2.2.3 (hotfixy 3–4 w sierpniu), **2.2.4 (11.09.2026)** z hotfixami _1 (11.09), _2 (12.09), _3 (16.09): „will be available on all worlds in 8 hours” (`api/articles_article.json`). Wynn aktualizuje światy falami w ciągu 8 godzin, bez globalnego restartu. Changelogi są od 09.2026 na stronie www i Discordzie (`api/latest_news.json`: „Reworked News Section”).
- **[F]** Beta dla rangi HERO+ (HERO Beta) i „Early Access” z plakatem w grze (`update/fruma/beta.png`).
- **[F]** Wersje klienta: minimum 1.21.4 (runda 1). Proxy odbija protokół w pingu (`01-…`). Społeczność gra na 1.21.11 i 26.x (Vulkan fix deklaruje formaty do 108).

## 9. Publiczne API v3 (co trzymają o graczu i świecie)

- **[F]** 40 endpointów (OpenAPI: `api/openapi.json`): player (lista online, profil, postacie, szczegóły postaci, mapa drzewka postaci), guild (lista, terytoria, sezony, po prefiksie/nazwie/UUID), item (baza, wyszukiwanie, receptury, metadane filtrów, sety), ability (drzewko, mapa), aspects, classes, map (znaczniki, pozycje graczy, world events, camps, raids, loot pools, gathering nodes, liczba questów), search, leaderboards (88 typów), publisher (artykuły, wideo), OAuth2.
- **[F]** Limit: nagłówki `ratelimit-limit: 50`, `ratelimit-bucket: SHARED`, reset w ~50 s. Tokeny API i OAuth2 z zakresami (docs: Authentication, OAuth2). Prywatność ustawia gracz: `restrictions.mainAccess / characterDataAccess / onlineStatus / guildHistoryAccess`. Szczegóły postaci (`/characters/{uuid}`) zwróciły 403 dla top gracza. Pozycje graczy na mapie (`/map/locations/player`) też 403 bez tokenu.
- **[F]** Pola profilu (przykłady `api/players/p_*.json`):
  - konto: `firstJoin`, `lastJoin`, `playtime` (h), `rank`/`supportRank`, `rankBadge` (SVG), `legacyRankColour`, `guild` (nazwa, prefiks, ranga, gwiazdki), `guildHistory`, `ranking` (miejsca w dziesiątkach rankingów), `activeCharacter`, `server`, `online`, `wallpaper`, `avatar`, `featuredStats`,
  - `globalData`: contentCompletion, wars, totalLevel, mobsKilled, chestsFound, dungeons (total + lista), raids (total + lista), worldEvents, lootruns, caves, completedQuests, `raidStats` (damageTaken, damageDealt, healthHealed, deaths, buffsTaken, **gambitsUsed**), pvp, guildRaids,
  - **na postać**: type, reskin, nickname, level, xp, xpPercent, totalLevel, preEconomy, gamemode[], contentCompletion, wars, playtime, mobsKilled, chestsFound, itemsIdentified, blocksWalked, logins, deaths, discoveries, pvp, skillPoints (5 atrybutów), professions (12 × level + xpPercent), dungeons, raids, worldEvents, lootruns, caves, **quests[] (lista nazw)**, removedStat.
- **[W]** To gotowy wzór **schematu danych postaci** dla MMOProfiles/MMOCore oraz zestawu statystyk do strony www i rankingów. Wynn mierzy m.in. bloki przebyte, logowania, identyfikacje i gambity. Tanio dodać to od początku.

## 10. Wnioski techniczne dla nas [W]

1. **Definicje modeli 1.21.4** (`range_dispatch` po float CMD, `select` po string CMD i po `display_context`) zamiast shaderowych trików. Wynn przeszedł na ten format (API), a my go mamy w Nexo.
2. **Dwa bazowe przedmioty z tintem** (mikstura i skóra) wystarczają na tysiące przedmiotów i przycisków GUI.
3. **VFX spelli z sekwencji modeli na `item_display`**: ładnie, ale drogo (modele + pakiety co tick). Na start 3–6 klatek i animowane tekstury `.mcmeta`.
4. **Muzyka z nut**: zero MB w packu, zmienna strefowo.
5. **Magiczna alfa w teksturach** (252–254 i 2–3 dla tekstu) wymaga konwencji dla grafików. Dokumentować w repo, jeśli użyjemy.
6. **Nie obfuskować packa na start.** Wynn to robi (hashowane nazwy, uszkodzony zip), ale dla nas koszt utrzymania jest większy niż zysk.
7. **Statystyki postaci jak w API Wynna** (logins, blocksWalked, deaths, quests[]…) do PlaceholderAPI i MMOProfiles od pierwszego dnia.
