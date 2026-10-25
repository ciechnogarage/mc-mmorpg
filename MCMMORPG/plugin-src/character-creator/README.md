# CharacterCreator

Fresh rebuild of the character creator, replacing the archived `character-stage` module
and CoreTools menu flow (see `old/character-creator-v1/`). Built from scratch per
`/home/dev/.claude/plans/linked-booping-fog.md` — does not follow the archived
`docs/character-sector-contract.md` design.

## What this plugin owns

- **Nie** wlasny ekran wyboru postaci i **nie** routing po wejsciu na serwer: jedynym
  ekranem wyboru jest `ProfileListViewer` z MMOProfiles, a my reagujemy na jego zdarzenia
  (`CharacterProfileModule`).
- The creator room: race + class + name picked from a real inventory GUI (`CreatorMenu`,
  clicked with the mouse cursor like any vanilla chest menu — see `CreatorRoomListener`),
  with a ModelEngine preview dummy visible behind it (`PreviewDummyManager` +
  `PreviewRotationTask`).
- Izolacja sesji: teleport, komendy i czat sa zablokowane na czas kreatora, a wyjscie poza
  `creator_limbo` konczy sesje i oddaje graczowi rzeczy (`CreatorSessionManager#tick`).
  Jedyne przewidziane wyjscie w druga strone to przycisk **WYJDZ** (slot 54) — rezygnacja
  bez tworzenia postaci, patrz "Wyjscie z kreatora i czystosc sali".
- Confirm sequence: LuckPerms `race.<id>` permission (dzis **write-only** — patrz komentarz
  w `config.yml`), MMOCore class select, plus the permanent ModelEngine body model applied
  directly via `ModelEngineAPI` (`ModelEngineBridge`) on the real player. No MythicLib
  `disguise` skill anywhere.
- **Skorka postaci**: jedna tekstura na pare RASA x KLASA (`SkinService`, dane w `skins.yml`).
  Ta sama skorka trafia na glowy postaci na ekranie wyboru MMOProfiles
  (`ProfileListHeadsListener`, wylacznik `skins.profile-heads`) — bez tego kazda postac ma
  tam glowe Steve'a.
  Naklada sie na profil gracza (Paper `PlayerProfile`) w tym samym opoznionym zadaniu co model
  rasy — czyli po kazdym teleporcie, wyborze profilu, respawnie i zmianie swiata, zawsze
  idempotentnie. Ta sama tekstura stoi na dummy'm w podgladzie, wiec kreator pokazuje to,
  co gracz dostanie.
- Trwalosc: `characters.yml` (`CharacterStore`) — zapis atomowy, asynchroniczny, z debounce,
  wersjonowany schematem i kopiami rotacyjnymi w `backups/`; jeden rekord na profil
  MMOProfiles.
- Trwalosc stanu wejscia: `sessions.yml` (`SessionStateStore`) — ekwipunek, tryb gry
  i miejsce sprzed wejscia do kreatora, zeby restart serwera nie kasowal graczowi plecaka.

## Czego ten plugin NIE ma

Lista jest tutaj celowo, bo trzy poprzednie wersje README obiecywaly czesc z tych rzeczy:

- **Ekwipunku startowego.** Sekcja `gear:` w `config.yml` ubiera WYLACZNIE dummy'ego
  w podgladzie. Gracz po utworzeniu postaci nie dostaje ani jednego przedmiotu.
- **Wyboru skorki przez gracza.** Skorka wynika z pary rasa+klasa i nie da sie jej wybrac
  osobno ani podmienic na wlasna. Postac zapisuje swoj klucz skorki (`skinSource`
  w `characters.yml`) i trzyma sie go nawet po zmianie mapowania w configu.
- **Usuwania i edycji postaci, slotow/limitu postaci, przycisku wstecz/anuluj, ekranu
  podsumowania, podgladu statystyk, plci, personalizacji wygladu, i18n.** To zakres
  produktowy, nie dlug techniczny — patrz PLAN-v4, sekcja "Poza tym planem".
- **Modeli 3D dla trzech z czterech ras.** W `plugins/ModelEngine/blueprints/` jest
  wylacznie `race_nieumarly`. Pozostale rasy maja w configu jawne `model-id: 'none'`
  i uzywaja przebrania zastepczego — config nie obiecuje modelu, ktorego nie ma.

## Build

Gradle (`build.gradle.kts`). Zastapil `tools/BuildCharacterCreator.java`, ktory skladal
classpath ze WSZYSTKICH jarow dzialajacego serwera i rozstrzygal konflikty wersji wlasnym
resolverem na wyrazeniach regularnych — build zalezal wiec od tego, co akurat lezalo
w katalogu serwera, i nie dalo sie go powtorzyc z czystego klona.

Na hoscie nie ma ani JDK, ani Gradle'a, wiec build chodzi w kontenerze:

```
./build.sh          # kompilacja + testy + jar w build/libs/
./build.sh test     # same testy
```

Zaleznosci sa jawne i dwojakiego rodzaju:
- `paper-api` — z repozytorium PaperMC, po wersji;
- API pluginow, ktorych nie ma w zadnym publicznym repo (ModelEngine, MMOProfiles,
  LibsDisguises + tranzytywnie MythicLib i packetevents) — z przypietej po nazwie pliku
  listy w `build.gradle.kts`. Podbicie wersji na serwerze wymaga swiadomej zmiany tam.

**Wersja jara pochodzi z gita** (`0.2.0-<rewizja>`), a nie z zaszytego na sztywno ciagu —
zeby z dzialajacego serwera dalo sie odczytac, ktore zrodla na nim stoja. Dopoki katalog
`plugin-src/character-creator/` nie jest zacommitowany, `build.sh` dokleja `.dirty`.

## Generatory grafiki

```
tools/gen.sh gen_creator_art.py     # 16 glifow rasa x klasa + creator_ui.yml -> tools/out/
tools/gen.sh gen_creator_icons.py   # 10 ikon 16x16 -> tools/out/
tools/gen.sh gen_race_skins.py      # 16 skorek rasa x klasa + _podglad.png -> tools/out/skins/
tools/upload_skins.sh               # wgranie skorek na mineskin + src/main/resources/skins.yml
```

Skorki musza przejsc przez mineskin.org, bo klient Javy pobiera je WYLACZNIE
z `textures.minecraft.net` — wlasny hosting PNG nie zadziala nawet przy `online-mode=false`.
`upload_skins.sh` gada z API mineskina WPROST (serwer nie musi dzialac), trzyma gotowe
tekstury w `tools/out/skins/.mineskin-cache.json` (klucz: suma SHA-256 pliku PNG) i pisze
`skins.yml`.

Droga przez `/grabskin` z LibsDisguises zostala PORZUCONA: MineSkin v2 kolejkuje zlecenia
i odpowiada `{"messages":[{"code":"job_queued"}]}`, a LibsDisguises 11.0.15 nie zna tej
odpowiedzi — w logu serwera laduje `Received an unknown response from MineSkin`, a skorka
nie zapisuje sie nigdy. Dzialalo to tylko dla obrazow, ktore mineskin mial juz w cache'u.
Komplet 16 skorek: ~40 minut i piec zgubionych skorek ta droga, ~1 minuta przez API.

Domyslnie pisza do `tools/out/`, a **nie** prosto do produkcyjnego packa Nexo — poprzednia
wersja nadpisywala pliki dzialajacego serwera przy kazdym uruchomieniu "zeby zobaczyc, co
wyjdzie". Wdrozenie jest osobna czynnoscia (`OUT=`, `GLYPHS=`, `PACK=`). Config czyta
`pyyaml`, a nie piec wyrazen regularnych udajacych parser YAML-a.

## Komendy administracyjne

`/cc` (alias `/charactercreator`), uprawnienie `charactercreator.admin`:

| komenda | co robi |
| --- | --- |
| `/cc reload` | przeladowuje `config.yml` i od razu go WALIDUJE (swiat, klasy, blueprinty) |
| `/cc info <gracz\|profileId>` | postacie gracza albo jeden rekord + stan sesji w kreatorze |
| `/cc delete <profileId>` | usuwa rekord (kopia zapasowa robi sie automatycznie) |
| `/cc prune [confirm]` | raportuje sieroty (rekord bez profilu MMOProfiles); `confirm` usuwa |
| `/cc sessions` | kto jest teraz w kreatorze |
| `/cc skin <gracz>` | co serwer TRZYMA na graczu kontra co POWINIEN — pierwszy krok przy zgloszeniu "mam nie te skorke" |
| `/cc skin <gracz> <id\|rasa klasa\|auto> [sesja]` | naklada skorke z `skins.yml` i ZAPISUJE ja w rekordzie postaci (`skin-source`) — przezywa respawn, relog i restart; `sesja` = po staremu, tylko do nastepnego respawnu |

Bez nich zmiana `config.yml` wymagala restartu serwera: `CreatorConfig.reload()` nie mial
ani jednego wywolujacego poza konstruktorem.

## Wymagania srodowiska

1. **Swiat `creator_limbo` musi istniec.** Multiverse-Core jest teraz twarda zaleznoscia
   w `plugin.yml`, a brak swiata wylacza plugin z czytelnym bledem — zamiast NPE przy
   kazdym teleporcie.
2. **MMOCore musi miec klasy z `mmocore-id`** (`warrior`, `mage`, `paladin`, `rogue`).
   Walidacja przy starcie odmawia wlaczenia pluginu, gdy ktorejs brakuje w configu.
3. **`force-class-selection: false`** w `plugins/MMOCore/config.yml` — inaczej MMOCore
   rejestruje sie jako drugi blokujacy modul na tworzeniu profilu i profil nigdy sie nie
   zatwierdza.
4. **Blueprinty ModelEngine** sa opcjonalne: rasa bez blueprintu ma miec `model-id: 'none'`.

## Design history (superseded designs, kept for context)

The room went through two prior interaction designs before landing on the current
inventory-GUI one, each replaced after a live test flagged a concrete problem:

1. **Hotbar items, normal SURVIVAL gamemode.** Left the vanilla HUD fully visible — a live
   test called this out as "doesn't look like a character creator, looks like normal
   Minecraft".
2. **Floating `Interaction`+`TextDisplay` "buttons" in GameMode.SPECTATOR**, aimed at with
   the crosshair and right-clicked. Fixed the HUD complaint but was reported directly as
   still not being a real GUI (no mouse cursor, just aim-and-click), and its transition
   into the name-entry anvil had a documented, never client-verified crash/kick risk
   (`DecoderException` on `accept_teleportation`) plus a per-tick teleport-anchor hack to
   stop the spectator player drifting off the click targets.

The current design (`CreatorMenu`, a real `Inventory` GUI opened with `player.openInventory`
and clicked via `InventoryClickEvent`, same pattern DeluxeMenus/CMI/Wynncraft's own class
select use) removes the aim-and-click complaint by construction — clicking is a normal
mouse-driven inventory click — and removes the anchor-snap hack entirely, since opening any
inventory screen already freezes the player's look and movement natively. `PreviewRotationTask`
now auto-spins the dummy on a timer instead of mirroring player look, since the player's
camera is frozen by the open GUI and has no live look input to mirror.

**Not yet re-verified against a real client** — rebuild and test in-game before treating the
old crash/kick issue as closed; the rewrite removes its specific trigger path (rapid
entity-click immediately followed by an anvil open) by construction, but that's reasoning
from the code, not a fresh live observation.

~~`PreviewRotationTask` always uses packetevents rotation packets.~~ **Nieaktualne.**
Pakiety rotacji byly nadpisywane przez LibsDisguises i model stal nieruchomo (zmierzone:
roznica miedzy dwoma zrzutami tej samej sesji 0.09/255). Task obraca encje serwerowo przez
`Entity#setRotation`, a packetevents zniknelo z `depend` w `plugin.yml` — w kodzie nie bylo
ani jednego uzycia.

## Etap 2 — jeden ekran (wykonane 2026-09-18)

Kreator jest teraz jednoekranowy: rasy 2x2 po lewej, klasy 2x2 po prawej, przezroczyste
okno na model 3D na srodku, pasek przyciskow i panel opisu na dole. Okno to
`InventoryType.SHULKER_BOX` (27 slotow) z przezroczysta tekstura panelu, a rama UI jest
rysowana glifem bitmapowym osadzonym w tytule inventory.

Dowody z prawdziwego klienta: `scratchpad/cc-test/shots/etap2_*.png`, `spike0_*.png`.

### Trzy rzeczy, ktore musza byc spelnione naraz, zeby glif tla dzialal

Kazda z nich byla osobna przyczyna niepowodzenia i kazda zostala potwierdzona zrzutem:

1. **Tekstura glifu musi miescic sie w 256x256.** Atlas fontow Minecrafta ma strone
   256x256; wieksze tekstury sa po cichu odrzucane i glif renderuje sie jako tofu.
   Nexo loguje to jako `Texture ... is above allowed 256x256 resolution`.
   Dlatego `creator_frame.png` ma dokladnie 176x168 (1 px obrazu = 1 px GUI).
   **To samo dotyczy `mmo_equipment_bg.png` (528x666) i `creator_menu_bg.png` (528x390)
   — obie przekraczaja limit, wiec nigdy nie dzialaly**, mimo ze byly w repo opisane jako
   "sprawdzony wzorzec". Nie kopiowac z nich wymiarow.
2. **Glif musi byc w foncie `minecraft:default`.** Nexo domyslnie rejestruje glify w
   `nexo:default` (`Glyphs.default_font`), a tytul inventory renderuje sie fontem
   `minecraft:default` — glif w zlym foncie wychodzi jako tofu. Stad jawne
   `font: minecraft:default` w `Nexo/glyphs/creator_frame.yml`. Bonus: dzieki temu glif
   dziala w zwyklym Stringu, wiec pozniej da sie uzyc `InventoryView#setTitle(String)`.
3. **Tytul musi byc bialy.** Glify bitmapowe sa mnozone przez kolor tekstu, a vanilla
   renderuje tytul inventory ciemnoszarym `#404040` — bez `NamedTextColor.WHITE` cala
   grafika jest przyciemniona niemal do niewidocznosci.

Stary komentarz w `CreatorMenu.java` obwinial offsety `-17/-169` z MythicLib. Offsety byly
skutkiem, nie przyczyna: znaki shift dzialaly (bo `nexo:shift` JEST podpiety do
`minecraft:default`), wiec tekst uciekal w lewo, a glif obok sie nie renderowal.

### Geometria (wyprowadzona i potwierdzona, blad 1 px)

Panel 9x3 = 176 x 168 px GUI. Slot (col,row) zaczyna sie w `(8 + col*18, 18 + row*18)`.
Tytul rysuje sie od `(8, 6)`, baseline 7 px nizej -> y = 13. Stad `ascent: 13`,
`height: 168`, `shift -8` (w `config.yml` jako `creator-menu.title-shift`, zeby dostrajac
bez przebudowy jara).

Przy 1280x720 klient uzywa GUI scale 3, panel ma poczatek w (376, 108) — przewidziany
srodek slotu hotbara (424, 561) vs zmierzony (425, 560).

### Poprawka ukladu (2026-09-18, po ocenie pierwszej wersji)

Pierwsza wersja ramy wypelniala caly panel kontenera i zostawiala na model dziure
264 x 156 px — w efekcie w "oknie na postac" widac bylo sam czubek glowy. Blad polegal na
zalozeniu, ze model musi zmiescic sie w panelu.

Wlasciwe podejscie: model stoi w swiecie, a "okno" to po prostu obszar, w ktorym nic nie
rysujemy. W panelu musza sie miescic wylacznie **klikalne sloty** (176 x 168 px GUI,
czyli 528 x 504 px ekranu przy GUI scale 3). Grafika moze byc dowolnie waska.

Dlatego uklad to teraz dwie waskie pionowe szyny przy krawedziach panelu
(kolumny 0-1 i 7-8) i calkowicie wolne kolumny 2-6 — model widac na calej wysokosci
ekranu. Dodatkowo:
- `spawn.pitch` wrocil z 13.0 na 0.0 (przechylenie bylo obejsciem pod stare GUI)
- `dummy-anchor.z` z -5.0 na -3.5 (postac zajmuje ok. polowy wysokosci ekranu zamiast 30%)
- `creator_limbo` doswietlony niewidzialnymi blokami `minecraft:light[level=15]`
  (450 blokow, `fill ... replace minecraft:air` wokol kotwicy) — wczesniej scena byla
  prawie czarna, a ekran kontenera i tak obcina jasnosc do ~47%

Petla testowa: `scratchpad/cc-test/enter_creator.sh <nick> <plik.png>` — stawia klienta,
przerzuca go przez proxy na world1, wchodzi w kreator i robi zrzut.

### HUD, podswietlenia slotow i wpisywanie imienia (2026-09-18)

**HUD.** Na tym serwerze vanillowy HUD (serduszka, glod, hotbar) renderuje sie MIMO
otwartego ekranu kontenera — widac to juz na czystym zrzucie z shulkera, bez naszego UI
(`shots/spike0_items_render.png`), wiec nie jest to skutek kreatora. Sprawdzone i odrzucone:
- `mythichud layout <gracz> remove <layout>` odmawia zdjecia layoutow z `layout.default`
  ("Could not remove layout"),
- z pustym `layout.default` HUD i tak zostaje, wiec MythicHUD go nie rysuje.

Dziala jedno: **GameMode.SPECTATOR** na czas kreatora. Ukrywa caly vanillowy HUD i — co
potwierdzone klikiem na zywym kliencie — NIE blokuje obslugi klikniec w otwartym inventory.
Samo ustawienie trybu w `start()` nie wystarcza: cos w lancuchu MMOProfiles/MMOCore
przywraca ADVENTURE juz po nas (zweryfikowane `data get entity <gracz> playerGameType` -> 2),
dlatego tryb jest pilnowany co 10 tickow w `CreatorSessionManager#enforceSpectator`.

Nakladki MythicHUD renderuja sie niezaleznie od trybu, wiec assety `sword`, `vanilla-health`,
`vanilla-hunger`, `vanilla-armor`, `vanilla-air` dostaly warunek:
`placeholder{ph=%player_gamemode%;val=SPECTATOR} true hide`.
UWAGA: warunek `gamemode{gm=SPECTATOR}` parsuje sie bez bledu, ale **nie jest przeliczany
w trakcie gry** — sprawdzone, HUD zostawal. Placeholder dziala, bo MythicHUD odswieza je
cyklicznie (`placeholder-update` w jego config.yml). MythicHUD ma tylko 5 warunkow:
`effect`, `gamemode`, `progress`, `placeholder`, `mythic` — `world` nie istnieje.

**Podswietlenia slotow.** Puste sloty w oknie na model pokazywaly biale kwadraty pod
kursorem. Nadpisane na przezroczyste: `assets/minecraft/textures/gui/sprites/container/
slot_highlight_{back,front}.png` (razem z ich `.mcmeta`, bez ktorych klient zle skaluje
sprite). UWAGA: to zmiana GLOBALNA — hover znika we wszystkich GUI na serwerze.
Cofniecie = usuniecie tych dwoch plikow z packa.

**Wpisywanie imienia.** `NameInputMenu` (kowadlo) zastapiony przez `NameInputDialog` na
natywnym Dialog API (Paper 1.21.6+): prawdziwe pole tekstowe, przyciski Zatwierdz/Anuluj,
walidacja 3-16 znakow (litery z polskimi, cyfry, podkreslnik). Znika cala klasa bledow
opisana wczesniej: podkradanie okna przez MythicLib Navigator i zatwierdzanie przy
zamknieciu z dowolnego powodu.

**Build.** (Historyczne — `BuildCharacterCreator` zostal zastapiony Gradle'em, patrz
sekcja "Build" na gorze.) `BuildCharacterCreator` dostal `keepHighestVersionPerArtifact`: `libraries/`
trzyma kilka wersji tego samego artefaktu (adventure-api 4.13.0 ... 4.26.1), a przy
sortowaniu alfabetycznym na classpath pierwsza trafiala NAJSTARSZA — przez co
`Audience#showDialog` "nie istnialo". Teraz zostaje po jednym jarze na artefakt, najnowszym.

### Rozsuniete szyny, podswietlenie wyboru i opisy (2026-09-18)

Szyny sa waskie (jedna kolumna ikon) i wychodza poza panel, a pergamin z opisem rozszerza
sie na zewnatrz. Prześwit na model wzrosl z 264 do ok. 324 px ekranu.

**Kluczowe: grafika moze byc szersza niz panel.** Model stoi w SWIECIE, a "okno" to po
prostu obszar, w ktorym nic nie rysujemy — w panelu (176 px GUI) musza sie miescic tylko
KLIKALNE sloty. Glify maja wiec 256 px szerokosci i `title-shift: -48`, przez co szyny
wychodza 40 px GUI poza panel z kazdej strony.

**Sloty** (stan faktyczny, zgodny z `CreatorMenu`; poprzednia wersja tej tabelki podawala
0/9/18/27, 8/17/26/35 i imie na 36 — zadna z tych liczb nie byla juz prawdziwa):
rasy raw **0/18/27/45**, klasy raw **8/26/35/53**, imie raw **4**, STWORZ raw **58**.
Raw >= 27 to sloty EKWIPUNKU GRACZA — renderowane przez `player.getInventory()`, a
sweeper (`CreatorSessionManager#sweepInventory`) pomija je wg
`CreatorMenu.PLAYER_SLOTS_IN_USE`. Zbior jest pilnowany testem
(`CreatorMenuTest#slotsInUseCoverEveryIconPlacedInThePlayerInventory`).
Sweeper czysci teraz tylko 0..35; wczesniejszy `inv.clear()` kasowal tez zbroje, ktorej
stash nie obejmowal (stash uzywa juz `getStorageContents`).

**Jeden glif na kombinacje rasa x klasa (16 PNG), nie dwa skladane shiftem.**
Glif bitmapowy ma wlasny "advance" (o ile kursor idzie w prawo po narysowaniu). Zeby
narysowac drugi glif w tym samym miejscu, trzeba by go cofnac dokladnie o te wartosc —
pomiary na zrzutach dawaly 143-150 px i szyny rozjezdzaly sie w bok. Jeden obrazek na
kombinacje kasuje ta niewiadoma calkowicie. Kod znaku:
`0x4100 + raceIndex * liczbaKlas + classIndex` — generator liczy to identycznie, z tego
samego config.yml, i sam wypisuje `Nexo/glyphs/creator_ui.yml`.

**Podswietlenie i opis** sa WYPALONE w grafice (zlota ramka wokol wybranej ikony + nazwa
i opis na pergaminie). Opisy pochodza z `description:` w config.yml — jedno zrodlo prawdy
dla grafiki i dla podpowiedzi przy ikonie.

**Przemalowanie w miejscu:** `InventoryView#setTitle(String)` — dziala, bo glify siedza
w `minecraft:default`, wiec legacy String wystarczy i nie trzeba przenosic informacji
o foncie. Okno nie mruga przy zmianie wyboru.

Regeneracja grafiki po zmianie ras/klas/opisow:
`python3 tools/gen_creator_art.py` (czyta wdrozony config.yml, pisze PNG + glyphs yml),
potem `nexo reload pack`.

### Scena, ikony i przycisk (2026-09-18)

**Sala.** Stara sala byla z ciemnych blokow — zmierzona srednia jasnosc kadru wynosila
27/255 (~10%), mimo 450 blokow swiatla. Samo dokladanie swiatla nie pomaga, bo w MC
jasnosc zalezy od ALBEDO bloku: przy light 15 ciemna cegla dalej jest ciemna. Sala zostala
przebudowana (`fill` przez RCON, patrz historia sesji): podloga i sciany ze `smooth_quartz`,
sufit z `calcite`, kolumny z `polished_blackstone` zwienczone `sea_lantern`, zloty gzyms,
podium pod postacia i swietlisty luk z `sea_lantern` za modelem — odcina sylwetke od tla.
Zmierzone po zmianie: 46/255 caly kadr, 44/255 obszar postaci (+70%).
Pamietac: ekran kontenera i tak obcina jasnosc do ~47%, wiec scena musi byc PRZEjasna.

**Ikony.** 11 ikon 16x16 recznie ulozonych jako pixel art (`tools/gen_creator_icons.py` —
siatka znakow + paleta, z asercja na 16x16). Wczesniej wszystkie rasy mialy identyczna
glowe gracza, a klasy identyczny miecz, wiec ekran nie niosl zadnej informacji.
Podpiete komponentem **item_model** (1.21.4+), nie CustomModelData — brak kolizji numerow
z lootem i z Nexo. Wlasna przestrzen nazw `cc`, zeby Nexo nie zarzadzalo tymi plikami:
  assets/cc/textures/item/<name>.png
  assets/cc/models/item/<name>.json
  assets/cc/items/<name>.json
Java: `meta.setItemModel(NamespacedKey.fromString("cc:<name>"))`, material bazowy PAPER.

**Przycisk "Stworz".** Byl `BARRIER` — czerwony znak zakazu u stop postaci czytal sie jak
blad. Teraz `cc:ui_confirm` (zielony klejnot w zlotej oprawie), wygaszony napisem dopoki
brak imienia.

**Czcionka opisow.** Liberation Sans Narrow przy 9 px i progowaniu do 1 bitu zlewal litery
("ambitni" wygladalo jak "arbitni"). Porownanie renderow wykazalo, ze **DejaVuSans 9 px,
prog 128** trzyma odstepy — i obsluguje polskie znaki, wiec opisy sa juz z diakrytykami.

Regeneracja po zmianie tresci:
  python3 tools/gen_creator_art.py     # ramy + opisy + Nexo/glyphs/creator_ui.yml
  python3 tools/gen_creator_icons.py   # ikony
  nexo reload pack

### Napis "Inventory", wielkosc postaci, opisy (2026-09-18)

**Napis "Inventory"** usuniety przez nadpisanie tlumaczenia w packu:
`assets/minecraft/lang/{en_us,pl_pl}.json` -> `{"container.inventory": ""}`.
Pliki lang MERGUJA sie z vanilla per-klucz, wiec wystarczy ten jeden wpis.
UWAGA: zmiana GLOBALNA — napis znika ze WSZYSTKICH ekranow kontenerow na serwerze.
Cofniecie = usuniecie tych dwoch plikow z packa.

**Wielkosc postaci:** `dummy-anchor.z` z -3.5 na -2.5, czyli 3 bloki od gracza zamiast 4.
Sylwetka urosla o ok. 40% (porownanie: shots/etap5_postac_przed_po.png).

**Opisy** maja teraz trzy czesci: nazwe, opis i wiersz BONUSU (`bonus:` w config.yml)
na ciemnym pasku u dolu pergaminu. Ustalenia z tuningu, warte zapamietania:
- **9 px to minimum czcionki.** Przy 8 px progowanie do 1 bitu rozwala litery —
  sprawdzone renderem, tekst byl nieczytelny.
- Pozycja wiersza bonusu liczona z **metryki fontu** (`getbbox`), nie ze stalej —
  przy sztywnym odstepie dolne piksele liter wychodzily poza pergamin.
- Generator **ostrzega**, gdy opis nie miesci sie w dwoch wierszach
  ("UWAGA: opis ... nie miesci sie"). Przy dodawaniu ras/klas patrzec na te komunikaty.

## Plan v3 — Etapy 0, 1, 3 (2026-09-18)

Patrz `PLAN-v3.md`. Zrobione 0, 1 i 3; Etap 2 (dzwiek + obrot) NIE zrobiony.

### Etap 0 — uczciwosc
- Usuniete **zmyslone bonusy** (`+10% dosw.`, `+2 Sila`). W kodzie bylo zero odwolan do
  `bonus:` — interfejs obiecywal mechanike, ktorej nie ma. Zastapione epitetami lore
  ("Ludzie Przymierza") i rolami ("Obronca"), ktore niczego nie obiecuja.
- `config.yml` ma teraz jawna adnotacje, ze `description` i `bonus` sa **wylacznie opisowe**.
- **`ninja` naprawiony**: `mmocore-id: 'rogue'` (plik `MMOCore/classes/rogue.yml` istnieje).
  Byl to jedyny realny bug funkcjonalny — klasa wybieralna i niemozliwa do potwierdzenia.
- "Ciezka zbroja i topor" -> "i tarcza", zeby zgadzalo sie z ikona.

### Etap 1 — kadr i swiatlo
- Sala przebudowana na CIEMNA (polished_blackstone / deepslate / black_concrete) z ciepla
  akcentami (ochre_froglight). Wczesniejsza jasna sala robila z postaci sylwetke.
- `spawn.pitch` 0 -> 6: podnosi postac w kadrze i chowa pusty sufit.
- Usunieta linia na czacie — pisala postaci po torsie.
- **Martwa szara plaszczyzna: 48% -> 21%.**

### Etap 3 — postac reaguje (bez ModelEngine)
`PreviewDummyManager#applyAppearance`: KLASA zaklada korpus/nogi/buty/bron, RASA barwiony
skorzany helm. Podzial slotow celowy — oba wybory widoczne naraz, nie walcza o slot.
Konfiguracja: `gear:` przy klasie, `helmet-color:` przy rasie.
Usuniety martwy `refreshDummy()`.

### NAJWAZNIEJSZE USTALENIE: UI trzeba rysowac CIEMNIEJ, niz podpowiada intuicja

Klient przyciemnia **swiat** za ekranem kontenera o ok. 53%, ale **NIE przyciemnia UI**
(glify tytulu rysuja sie po dimie). Zmierzone konsekwencje:
- maksimum jasnosci sceny (zelazna zbroja w swietle 15) to **0.26**,
- pergamin w "normalnej" palecie mial mediane **0.24** — czyli interfejs dorownywal bohaterowi.

Dopiero po sciemnieniu palety UI do mediany **0.18** postac zaczela wygrywac:
**31% strefy postaci jest jasniejsze od UI** (wczesniej 0%).

Sciezka pomiaru (calosc): pergamin 0.38 -> 0.18, udzial pikseli postaci jasniejszych
od UI 0% -> 31%, martwa plaszczyzna 48% -> 21%.
Dowody: `shots/v3_etap3_klasa_zlota.png`, `shots/v3_klasa_przed_po.png`.

## Plan v3 — Etap 2: dzwiek i ruch (2026-09-18)

### Obrot: pakiety NIE dzialaly, serwerowa rotacja dziala
Poprzednia wersja wysylala `WrapperPlayServerEntityRotation` przez packetevents do jednego
gracza. Task startowal (brak ostrzezenia w logu), ale model stal nieruchomo — zmierzone:
roznica w obszarze postaci miedzy dwoma zrzutami tej samej sesji **0.09/255**.
Najbardziej prawdopodobna przyczyna: LibsDisguises zarzadza wlasnymi pakietami przebranej
encji i nadpisuje nasze.

Rozwiazanie: obracac encje NAPRAWDE, serwerowo. Wtedy disguise i zalozony ekwipunek
podazaja za nia same.
- `Entity#setRotation(yaw, 0)` — kierunek patrzenia (glowa)
- `LivingEntity#setBodyYaw(yaw)` — korpus

**Trzeba ustawic OBA.** Sam `setRotation` obracal cialo, a glowa zostawala zwrocona do
kamery (widoczne na `shots/v3_etap2_obrot.png` przed poprawka).

Po zmianie roznica miedzy zrzutami: **6.5/255**. `PreviewRotationTask` nie wymaga juz
packetevents — bramka `isPluginEnabled("packetevents")` w CharacterCreatorPlugin usunieta.

### Dzwiek
W calym pluginie bylo **zero** wywolan `playSound`. Dodane, wylacznie dzwieki vanilla
(zero pracy nad assetami): wybor rasy/klasy `BLOCK_AMETHYST_BLOCK_CHIME` (rozny pitch),
imie `BLOCK_NOTE_BLOCK_PLING`, kadr `UI_BUTTON_CLICK`, odmowa `BLOCK_NOTE_BLOCK_BASS`.
UWAGA: dzwieku nie da sie zweryfikowac zrzutem ekranu — kod sie kompiluje i zdarzenia
odpalaja, ale **odsluchu nie bylo**.

### Ceremonia potwierdzenia
`tryConfirm` nie konczy juz sesji natychmiast. Kolejnosc: zamkniecie GUI -> dzwiek
`BLOCK_BEACON_ACTIVATE` + czastki END_ROD -> po 20 tickach `UI_TOAST_CHALLENGE_COMPLETE`
+ TOTEM_OF_UNDYING -> po 45 tickach `ENTITY_PLAYER_LEVELUP`, zakonczenie sesji i teleport.
Sesja konczy sie DOPIERO na koncu, bo inaczej dummy znika, zanim cokolwiek widac.
Zweryfikowane end-to-end: profil zapisany, `shots/v3_etap2_ceremonia.png`.

### Czastki w tle
`PreviewRotationTask` sieje END_ROD wokol podium co 5 uruchomien, tylko dla tworzacego
gracza (`player.spawnParticle`, nie `world.spawnParticle`).

## Przebudowa sterowania + naprawy krytyczne (2026-09-18, po audycie zewnetrznym)

### Uklad wg uwag uzytkownika
- **Model NIE obraca sie sam.** Auto-spin zastapiony recznym obrotem: strzalki po bokach
  przycisku tworzenia (`rotateModel`, krok 22.5 st.). `PreviewRotationTask` tylko utrzymuje
  zadany kat (serwer potrafi go zresetowac) i sieje czastki.
- **Zoom strzalkami** nad i pod przyciskiem, zamiast LPM/PPM na jednym slocie.
- **Rzedy ikon rozstawione**: y = 18/54/85/121 zamiast 18/36/54/85. Rowny raster nie jest
  osiagalny (13 px technicznej szczeliny miedzy kontenerem a ekwipunkiem gracza),
  ale 36/31/36 czyta sie jako celowy rozstaw, a nie "trzy sklejone i jedna odstajaca".
- **Imie na gorze** (slot raw 4, srodek gornego rzedu), we wlasnej ramce.
- **Pergaminy usuniete.** Opisy i etykiety przeniesione do PODPOWIEDZI przedmiotow:
  vanillowy font jest w pelni czytelny, obsluguje polskie znaki i nie ma limitu szerokosci.
  Wypalany tekst 9 px progowany do 1 bitu byl nieczytelny i wychodzil poza ramke.

### Naprawy krytyczne (znalezione przez audyt)
1. **SOFT-LOCK: Escape w dialogu imienia.** `canCloseWithEscape(true)` powodowal, ze Escape
   zamykal dialog BEZ wywolania onConfirm/onCancel — a to one otwieraja menu z powrotem.
   Gracz zostawal w creator_limbo, w SPECTATOR wymuszanym co 10 tickow, bez GUI i bez
   sposobu wyjscia poza relogiem. Naprawione: `canCloseWithEscape(false)` + watchdog
   `reopenLostMenus()`, ktory odtwarza menu, gdy sesja zyje, a okna nie ma.
   Zweryfikowane na kliencie: Escape nic nie robi, "Anuluj" wraca do menu.
2. **Brak `isOnline()` w ceremonii.** Ceremonia trwa 2.25 s; rozlaczenie w jej trakcie
   konczylo sie zapisem rekordu, komendami konsolowymi i teleportem na obiekcie gracza,
   ktorego juz nie ma. Dodane straznice w obu zaplanowanych zadaniach.
3. **Zoom dzialal odwrotnie.** "Przybliz" oddalal model (gracz stoi na z=0.5 i patrzy
   w -Z, wiec mniejsze z = dalej). Kierunki zamienione.

### Pelny audyt
Zewnetrzny przeglad znalazl ok. 100 pozycji — patrz PLAN-v3.md oraz historia sesji.
Powyzsze to tylko trzy krytyczne plus uklad. NIEnaprawione zostaja m.in.: trzy globalne
nadpisania packa (slot_highlight, container.inventory, shulker_box), brak sciezki
awaryjnej bez resource packa, dwoch graczy na jednej kotwicy dummy'ego, martwy kod
(swapModel, cycleRace/cycleClass, confirmAnchor, clearModel), twarde `depend` na
packetevents mimo zerowego uzycia.

## Runda uwag uzytkownika (2026-09-18, wieczor)

- **Zoom usuniety w calosci.** Model stoi zawsze w tym samym, bliskim miejscu
  (`dummy-anchor z: -2.2`, czyli 2.7 bloku). Zoom przesuwal encje w swiecie, wiec przy
  pitchu kamery zmienial tez jej wysokosc w kadrze — kadr byl nieprzewidywalny.
- **Sterowanie to jeden rzad**: obrot w lewo / STWORZ / obrot w prawo, w dolnym rzedzie
  (hotbar, panel y 143). Wczesniej klaster siedzial wyzej i ciął postac w pasie.
- **Glowa idzie z cialem** — `setRotation` + `setBodyYaw` (patrz wyzej).
- **Szyny dosuniete do krawedzi obrazka** (image x 0..74 i 182..256).
  DALEJ SIE NIE DA: tekstura glifu ma limit 256x256, co przy mapowaniu 1:1 daje 256 px GUI
  = 768 px ekranu z 1280. Zeby dojechac do krawedzi ekranu, trzeba by rozciagnac teksture
  w poziomie (np. 256x101 renderowane na wysokosc 168), co kosztuje rozdzielczosc pionowa.
- **Nazwa jako pasek u gory**, bez osobnego "menu". Pasek to grafika, a sama nazwa to
  PRAWDZIWY TEKST tytulu okna — czytelny vanillowym fontem, z polskimi znakami.

### Jak udalo sie postawic tekst po glifie
Zeby narysowac tekst w znanym miejscu PO glifie, trzeba znac jego "advance". Zmierzone
sonda na zywym kliencie (`creator-menu.probe-advance`, dokleja znacznik U+2588 i mierzy
jego pozycje): **advance = 257 px GUI** (szerokosc 256 + 1). Wczesniejsze szacunki
143-150 px, na ktorych oparlem decyzje o 16 osobnych PNG, byly BLEDNE.
Stad `name-shift: -147` (glif konczy sie na panel x 217, nazwa ma trafic ok. x 70).

### Blad z Escape
Przytrzymanie Escape zamykalo okno na jeden tick przed jego odtworzeniem — gracz jest
w SPECTATOR, wiec zdazyl rozejrzec sie po sali. Zamknac ekranu nie da sie zablokowac
(vanilla zawsze na to pozwala), ale skutek mozna cofnac: `reopenMenuNextTick` teleportuje
teraz gracza na dokladna pozycje i rotacje startowa razem z otwarciem menu.

## Runda uwag: obrot, pozycja paskow, przyciemnienie (2026-09-18)

Uwagi: wywalic obrot (postac na wprost), podniesc pasek gorny, opuscic dolny,
przesunac i splaszczyc boczne, usunac przyciemnienie tla.

### Obrot usuniety calkowicie

`SLOT_ROT_LEFT/RIGHT`, akcje `rot_left`/`rot_right`, `CreatorSession#rotateModel`
i ikony `cc:ui_rot_*` — wszystko wyciete. `modelYaw()` zwraca teraz stala 180 stopni,
a `PreviewRotationTask` nadal ja wymusza co tick (serwer resetuje kat przy
synchronizacji encji, wiec zadanie tasku zostaje). Przy okazji poszly nieuzywane
ikony zoomu (`ui_zoom`, `ui_zoom_in`, `ui_zoom_out`) — zostalo 10 zamiast 15.

### Pasek gorny i dolny: trzeba bylo powiekszyc KANWE, nie przesunac grafike

Grafika miala 256x168 i `ascent: 13`, co stawialo ja dokladnie w obrysie panelu
(image_y = panel_y). Nie bylo dokad isc. Teraz 256x256 i `ascent: 53`:

    image_y = panel_y + 40      gora glifu na panel y -40, dol na panel y 216

Wyprowadzenie: glif rysuje sie gora na `baseline - ascent`, baseline tytulu jest na
panel y 13, wiec ascent 53 daje gore na -40. Szerokosc bez zmian, wiec `advance`
nadal wynosi 257 i `title-shift`/`name-shift` w configu zostaja te same.

**Czego przesunac sie NIE DA — i dlaczego:**

- **Tekst tytulu** (imie postaci) siedzi na stale na panel y 6..13. Vanilla rysuje go
  tam, a font `nexo:shift` przesuwa wylacznie w poziomie. Dlatego gora kreatora to
  teraz DWA elementy: cienki sztandar wysoko (panel y -16..6) i tabliczka imienia,
  ktora zwisa z niego i obejmuje tekst oraz slot imienia. Tabliczka idzie do tekstu,
  nie odwrotnie.
- **Przycisk STWORZ** jest przypiety do rzedu hotbaru (panel y 143) i nizej zejsc nie
  moze — to ostatni rzad slotow w GUI. Zeszla za to grafika: plinta konczy sie na
  panel y 178, czyli 10 px GUI ponizej panelu.
- **Szyny boczne nie odkleja sie od kolumn ikon.** Klikalne sloty istnieja tylko w
  obrysie panelu (panel x 8..170), wiec kazda plyta pod ikonami musi tam byc.
  Grafika siega juz maksimum na zewnatrz: 40 px GUI z kazdej strony, co jest sufitem
  tekstury 256 px. Zeby dosunac panele do krawedzi ekranu, trzeba by zrezygnowac z
  klikalnych ikon.

Splaszczenie szyn jest wiec zwezeniem i uproszczeniem: 74 -> 66 px, jedna zlota
kreska zamiast podwojnej ramki z narożnymi studami, bez pasa runicznego.

### Przyciemnienie tla: NIE DA SIE usunac packiem — zmierzone, nie zgadniete

Hipoteza byla taka, ze ekran kontenera rysuje tlo z tekstury. Test: nadpisane
`gui/inworld_menu_background.png` i `gui/menu_background.png` pustymi (alpha 0)
16x16 PNG, reload packa, zrzut z prawdziwego klienta.

Wynik — srednia RGB swiata na skraju ekranu, poza grafika GUI:

| zrzut | lewy skraj | prawy skraj |
|---|---|---|
| v5 (przed) | 19.6 / 18.9 / 19.4 | 19.1 / 18.8 / 19.3 |
| v6 (override) | 19.1 / 18.8 / 19.3 | 19.1 / 18.8 / 19.4 |

Zero roznicy. Te tekstury obsluguja menu pauzy, nie ekran kontenera.
`AbstractContainerScreen` rysuje `fillGradient` z zaszytymi w kliencie stalymi ARGB —
pack tego nie widzi. W `.assetCache/1.21.11/1.21.11.zip` nie ma tez zadnego
`shaders/core/position_color` ani `rendertype_gui`, wiec nawet obejscie shaderem
rdzeniowym nie jest dostepne przez pack.

Override zostal **cofniety** — nic nie dawal, a psulby tlo menu pauzy calemu serwerowi.
Jedyne, co realnie da sie zrobic, to rysowac UI ciemniej, zeby scena wygrywala
kontrastem (patrz "UI trzeba rysowac CIEMNIEJ" wyzej).


---

## PLAN-v4 — naprawa dzialania (2026-09-18)

PLAN-v3 dotyczyl tego, jak kreator wyglada. Ten etap dotyczyl tego, czy dziala. Ponizej
zmiany wg przyczyny, tak jak w planie. Warstwa graficzna nie zostala ruszona.

### Etap 0 — zatrzymanie utraty danych

- `onDisable()` konczy WSZYSTKIE sesje (`CreatorSessionManager#endAll`). Wczesniej `/stop`
  z graczem w kreatorze kasowal mu plecak: stash zyl wylacznie w RAM-ie.
- Blokada podwojnego commitu: `CreatorSession#confirming`, ustawiana na wejsciu do
  ceremonii i czytana przez obsluge klikniecia, handler zamkniecia okna ORAZ straznika menu.
  Wczesniej straznik odtwarzal klikalny przycisk w trakcie 45-tickowej ceremonii, wiec drugie
  klikniecie dawalo drugi `validate()`, drugi teleport i drugi komplet komend konsolowych.
- **Odwrocona kolejnosc commitu**: `validate()` -> `applyToRealPlayer()` -> zapis ->
  teleport (`CharacterProfileModule#onConfirm`). Bylo odwrotnie, przez co kazda awaria po
  pierwszym kroku zostawiala rekord bez profilu — stad 9 sierot na 18 rekordow.
- `start()` odrzuca druga sesje zamiast zapisac do stashu juz wyczyszczony ekwipunek.
- Sciezka odmowy teleportuje i komunikuje tak samo jak sukces (`#abort`) — koniec
  z softlockiem w `creator_limbo` bez GUI.
- Cale `onCreate` w `try/catch`: wyjatek zwalnia profil zamiast zostawiac go pending
  na zawsze.

### Etap 1 — stan sesji przezywa restart

- Mapa sesji kluczowana **UUID**, nie obiektem `Player` (martwe `CraftPlayer` po reconnekcie).
- `SessionStateStore` (`sessions.yml`): ekwipunek, zbroja, offhand, tryb gry i lokalizacja
  wejscia zapisywane na dysk PRZED jakakolwiek zmiana, kasowane po udanym wyjsciu.
- `CreatorSessionManager#restoreAfterJoin` oddaje rzeczy graczowi, ktory wypadl w trakcie
  (opoznione o 40 tickow, bo MMOProfiles sam wgrywa ekwipunek profilu po zalogowaniu).
- Przywracany jest ORYGINALNY tryb gry — admin wchodzacy z CREATIVE nie wychodzi juz
  w SURVIVAL.
- `onQuit` na `EventPriority.LOWEST` — bez tego mozliwe bylo zapisanie do profilu
  SPECTATOR + pustego ekwipunku na stale.
- Usunieta martwa linia `player.setGravity(true)`.

### Etap 2 — izolacja zamiast przykrycia GUI

- `PlayerTeleportEvent` anulowany, gdy cel jest poza `creator_limbo`.
- Straznik sprawdza SWIAT: rozjazd konczy sesje, zamiast eskalowac ja w normalnym swiecie
  (wymuszony SPECTATOR + otwierane menu + czyszczony plecak co 10 tickow).
- `PlayerCommandPreprocessEvent` — whitelist pusta.
- `AsyncChatEvent` — gracz w kreatorze nie pisze i nie widzi cudzych wiadomosci.
  PLAN-v3 oznaczal to jako zrobione; listenera czatu nie bylo w kodzie w ogole.
- Straznik nie walczy z cudzym GUI (reaguje tylko na `InventoryType.CRAFTING`) i ma
  throttling 1 s.
- `Multiverse-Core` w `depend`, brak swiata = `setEnabled(false)` z czytelnym bledem.

### Etap 3 — dane, ktore przezywaja awarie

- Zapis atomowy (`tmp` + `ATOMIC_MOVE`), asynchroniczny, z debounce 40 tickow.
- Kopie rotacyjne w `backups/` (10 sztuk) przy starcie i przed kazda operacja masowa.
- `schema-version: 2` + sciezka migracji; brak pola `name` jest ZGLASZANY jako korupcja,
  a nie po cichu zastepowany ID profilu.
- Indeks unikalnosci imion (`NameRules#uniqueKey`, ignoruje wielkosc liter i podkreslniki).
  Istniejace duplikaty sa raportowane, nie kasowane.
- Whitelist znakow zamiast `\p{L}` + normalizacja NFKC + lista zakazanych prefiksow —
  cyrylickie `А` nie da sie juz podszyc pod "Admin".
- Komunikaty walidacji na **actionbar**, nie na zaslonięty czat.

### Etap 4 — koniec obiecywania rzeczy, ktorych nie ma

- Wybor rasy **naprawde zmienia model 3D**: `swapModel` byl martwym kodem (jedyne
  wystapienie w `src/` to jego definicja), a `clearModel` byl martwy razem z nim.
- Rasy bez blueprintu maja jawne `model-id: 'none'`; walidacja przy starcie mowi o tym raz,
  zamiast 177 ostrzezen w logu (ostrzezenie z `ModelEngineBridge` jest teraz raz na model-id).
- `SkinProvider` i cala sekcja `skin:` usuniete — klasy nigdy nie bylo, a komenda
  `cmi skin <gracz> <gracz> -s` byla logicznym no-opem przy KAZDYM logowaniu.
- `race.*` zostaja, ale sa jawnie udokumentowane jako write-only (patrz `config.yml`
  i `ConfirmService#applyToRealPlayer`).
- `/charactercreator` przestala byc atrapa — patrz sekcja "Komendy administracyjne".
- Usuniety martwy kod: `CharacterStore.normalizeId()`, obsluga akcji `random_name`
  (`actionOf()` nigdy jej nie zwracala — pilnuje tego test).
- Walidacja configu przy enable zamiast odmowy w runtime.
- Zniknal `logger.info` z pelnym UUID przy kazdym `ProfileCreateEvent`.

### Etap 5 — narzedzia i build

- Komendy `/cc reload|info|delete|prune|sessions|skin` + realne sprawdzanie
  `charactercreator.admin` (wczesniej ZERO wywolan `hasPermission()` w calym pluginie).
- 56 testow jednostkowych czystej logiki (`NameRules`, `CreatorMenu.shift/actionOf/
  playerIndex`, `CreatorSession`, `CreatorConfig.parseColor`) — wczesniej zero.
- Gradle zamiast `BuildCharacterCreator.java`; wersja z gita; `packetevents` poza `depend`,
  `MMOCore` w `depend`; `build/` w `.gitignore`.
- Generatory domyslnie do `tools/out/`, config czytany przez `pyyaml`.
  Weryfikacja: oba generatory produkuja pliki **bajt w bajt identyczne** z tymi, ktore
  leza na produkcji (16 PNG + `creator_ui.yml` + 30 plikow ikon).

### Etap 6 — co zostaje do sprawdzenia W GRZE

Patrz PLAN-v4. Jedna pozycja jest juz rozstrzygnieta: **MMOCore laduje
`classes/mage/mage.yml` z podkatalogu** — na produkcji istnieje profil z przypisana klasa
Mage, wiec wybor Maga nie konczy sie cichym brakiem klasy. Pozostale piec wymaga zywego
klienta i drugiego konta.

## Wyjscie z kreatora i czystosc sali (2026-09-20)

Trzy uwagi z ogladania kreatora w grze. Wszystkie trzy okazaly sie mowic o czyms innym,
niz sugerowal objaw.

### "Przy przelaczeniu pokazuje sie dziwny model" i "armor stand nad ramionami"

To nie byl blad renderowania podgladu — w sali **stal smiec**. Pomiar botem
protokolowym (`eval/`, bot widzi kazda encje, ktora klient dostaje) pokazal w punkcie
`creator-room.dummy-anchor` (0.5 / 100 / -2.2) **cztery** encje naraz zamiast jednej:
podglad, `armor_stand` (widoczny, `Invisible: 0b`, tagi `cc_dummy` i `cc_p_TEST`),
`mannequin` (tag `cc_dummy`) i `interaction` obok. Wszystkie z `Paper.SpawnReason: COMMAND`,
czyli wsummonowane recznie przy porownywaniu tej wersji z prototypem config-only (world3).

- armor stand jest wyzszy od gracza (1.975 vs 1.8), wiec wystawal **dokladnie zza ramion**
  postaci,
- mannequin stal w tym samym punkcie co podglad, wiec bylo go widac za kazdym razem, gdy
  podglad na chwile znikal — a znika przy KAZDEJ zmianie rasy lub klasy: LibsDisguises
  zdejmuje przebranie i zaklada je od nowa. Zmierzone na surowych pakietach: `entity_destroy`
  podgladu, a **9 ms** pozniej `spawn_entity type=155` (`player`). Zadnego `type=150`
  (`zombie`) klient nie dostaje, wiec nosiciela nie widac — w tej szczelinie widac bylo to,
  co stalo ZA podgladem.

Naprawa jest w dwoch miejscach: sala zostala posprzatana na zywo, a plugin sprzata ja przy
kazdym starcie (`CharacterCreatorPlugin#sweepCreatorRoom`, wylacznik
`creator-room.sweep-stray-entities`) i **wylacza w niej naturalny spawn mobow**. To drugie
tez nie jest profilaktyka na zapas: zrzut z prawdziwego klienta zlapal kure chodzaca po
scenie za plecami postaci kilkanascie minut po sprzataniu.

UWAGA przy recznym sprzataniu: `kill @e[type=!player]` z konsoli **nie robi nic i nie mowi
o tym ani slowa** — komende `/kill` przechwytuje CMI. Trzeba `minecraft:kill`.

### Przycisk WYJDZ (slot 54)

Do tej pory z sali nie bylo wyjscia innego niz stworzenie postaci albo rozlaczenie:
teleport, komendy i czat sa zablokowane, zamkniete okno straznik otwiera z powrotem,
a tryb SPECTATOR jest wymuszany co 10 tickow. Przycisk stoi w lewym rogu dolnego rzedu,
cztery sloty od "STWORZ POSTAC" (odstep pilnowany testem), dziala jednym kliknieciem i
bez pytania "na pewno?".

Co sie dzieje po kliknieciu: `EndReason.CANCELLED` (rzeczy, tryb gry i miejsce wracaja jak
przy kazdym innym zakonczeniu sesji), a w nastepnym ticku `mmoprofiles open <gracz>`, czyli
ekran wyboru postaci.

**Zawieszonego `ProfileCreateEvent` NIE zatwierdzamy i to jest cala rezygnacja.** Sprawdzone
w bajtkodzie `ProfileListImpl#createProfile`: nowy `PlayerProfileImpl` powstaje jako zwykly
obiekt, a do listy profili gracza trafia dopiero w callbacku odpalanym przez `validate()`.
Bez niego profil nie powstaje i nic nie zostaje zawieszone — `hasProfilePending()` patrzy na
sesje MythicLib, nie na ten event. (Przy okazji: `createProfile` nie czyta nawet
`isCancelled()`, wiec `event.setCancelled(true)` w `onCreate` jest deklaracja intencji,
a nie mechanizmem.)

Pilnuje tego eval `eval/scenarios/80-wyjscie-z-kreatora.js` — lacznie z tym, ze **drugie
wejscie do kreatora po rezygnacji dziala**, bo to jedyny wiarygodny dowod, ze porzucenie
eventu bylo czyste. Potwierdzone tez w prawdziwym kliencie
(`output/kreator-wyjscie-01/captures/`).

## Responsywnosc: skorki i wejscie do sali (2026-09-20)

Dwie uwagi po obejrzeniu poprawionego kreatora: przy zmianie rasy/klasy nadal miga "jakis
randomowy model", a wejscie do sali jest "po chwili", nie od razu.

### Randomowy model to SKORKA DOMYSLNA, a nie zaden model

Klient Javy pobiera teksture skorki dopiero wtedy, gdy ma ja na czym narysowac, a do tego
czasu rysuje skorke DOMYSLNA wyliczona z UUID profilu. UUID jest u nas wyprowadzone
z identyfikatora skorki (`SkinService#disguiseProfile`, i tak ma byc — inaczej klient nie
cache'owalby ich osobno), wiec **kazda para rasa x klasa ma inna skorke domyslna**. Stad
wrazenie losowej postaci.

Zmierzone, nie wydedukowane. Katalog `assets/skins` prawdziwego klienta (podmontowany jako
zapisywalny, zeby dalo sie go liczyc) rosl o JEDEN plik przy KAZDYM kliknieciu:

```
przed kreatorem: 3      po kliknieciu rasy: 5      po kliknieciu klasy: 8
po wejsciu:      4      po kliknieciu rasy: 6      po kliknieciu klasy: 9
                        po kliknieciu rasy: 7
```

### Co NIE zadzialalo: preload pakietem

Pierwsza wersja wysylala jeden `player_info` (packetevents, ADD_PLAYER, wpisy nielistowane)
z kompletem 16 profili zaraz po wejsciu gracza na serwer. **Pakiet dochodzi** — bot
protokolowy widzi go z 16 wpisami — ale licznik plikow w cache'u klienta nie drgnal ani razu.
Klient rozwiazuje skorke leniwie, przy rysowaniu; sam wpis w liscie graczy niczego nie
pobiera. Kod zostal usuniety razem z zaleznoscia od packetevents.

### Co pomoglo, ale NIE WYSTARCZYLO: rozgrzewka na prawdziwych encjach

`PreviewDummyManager#warmUpSkins`: na czas wejscia do sali stawiamy komplet nosicieli
przebranych w 16 skorek, ustawionych **w jednym szeregu za podgladem, na osi kamery**
(kierunek liczony z `creator-room.spawn` do `creator-room.dummy-anchor`). Wszystkie maja
model gracza, czyli ten sam rozmiar, wiec kazdy dalszy jest w rzucie perspektywicznym
mniejszy od tego przed nim i chowa sie za nim w calosci. Znikaja po
`preview.skin-warmup-ticks` (60 = 3 s — musi byc DLUZEJ niz ekran "Loading terrain", bo
encja usunieta przed pierwsza klatka niczego nie pobierze). Dla innych graczy w sali sa
chowane wprost (`hideEntity`) — przebranie widzi tylko tworzacy gracz, wiec bez tego reszta
zobaczylaby szereg zombie. Przy okazji tak samo chowany jest glowny podglad.

Wynik, zmierzony tak samo jak usterka:

```
przed:  1 plik  ->  wejscie do kreatora: 17 plikow (komplet)
potem:  8 przelaczen (4 rasy + 4 klasy) -> 17 plikow, ZERO nowych pobran
```

Tekstury sa wiec po wejsciu na dysku klienta — ale **miganie zostalo**, o czym uzytkownik
zameldowal przy nastepnym obejrzeniu. Patrz sekcja nizej: pobieranie bylo tylko polowa
przyczyny. Przy okazji zrzut z dluzsza rozgrzewka (`captures/warm-widoczna.png`) pokazal,
ze szereg nosicieli WIDAC: spod sylwetki wystawaly nogi kolejnych, a cien byl wyraznie za
dlugi. Perspektywa nie chowa ich tak dobrze, jak zakladalem.

### Wejscie do sali

Sala jest osobnym swiatem, wiec wejscie to zmiana wymiaru i klient ZAWSZE pokaze
"Loading terrain..." narysowane na poprzednim swiecie — tego sie nie obejdzie bez rezygnacji
z osobnego swiata. Skrocone zostalo to, na co klient czeka (`prepareCreatorRoom`):

- `creator-room.view-distance: 4` — sala ma kilkanascie blokow, a serwer wysylal jej chunki
  z globalnego `view-distance=10`, czyli 21x21 = 441 chunkow pustki; przy 4 jest ich 81,
- `creator-room.keep-loaded: true` — 3x3 chunkow wokol obu kotwic trzymane na stale
  w pamieci, zeby serwer nie wczytywal ich z dysku przy kazdym wejsciu (po wyjsciu
  ostatniego gracza sala sie wylaczala).

Czasy protokolarne po zmianie (bot, od klikniecia "Create New Character"): `respawn` 8 ms,
okno kreatora 292 ms, podglad 354 ms, komplet rozgrzewki 449 ms, sprzatniecie rozgrzewki
3000 ms. **Zrzuty z klienta QA nie nadaja sie do mierzenia tego czasu** — kontener renderuje
programowo (llvmpipe), wiec jego 4 s do pierwszej klatki sali mowi o Xvfb, a nie o graczu.

## Miganie skorki: prawdziwa przyczyna i stajnia nosicieli (2026-09-20)

Po rozgrzewce skorek miganie **nadal bylo**. Dwa pomiary rozstrzygnely, ze pobieranie tekstur
bylo tylko polowa przyczyny:

- licznik plikow w `assets/skins` klienta NIE rosnie przy kliknieciach (tekstury sa juz
  na dysku),
- przy zmianie rasy nie leci zaden `player_remove` (profile NIE znikaja z listy graczy klienta).

Zostaje trzecia rzecz, widoczna na pakietach od poczatku: `entity_destroy` podgladu, a 44 ms
pozniej `spawn_entity type=155` z nowym profilem. **Klient dostaje NOWA ENCJE** i musi dla
niej rozwiazac skorke od zera; rozwiazywanie jest asynchroniczne, a do jego konca renderer
rysuje skorke DOMYSLNA wyliczona z UUID profilu. UUID mamy wyprowadzone z identyfikatora
skorki, wiec kazda para rasa x klasa ma inna domyslna — stad "losowy model". Cache na dysku
skracal to okno, ale go nie zamykal.

### Rozwiazanie: nie tworzyc nowej encji

`PreviewDummyManager` to teraz **stajnia**: przy wejsciu do sali powstaje po jednym nosicielu
na kazda skorke (16 + jeden awaryjny na pare bez wpisu w `skins.yml`), wszyscy zyja do konca
sesji, a klikniecie rasy albo klasy jest **teleportem** — wybrany wjezdza na kotwice,
poprzedni wraca pod sale. Zadnego `destroy`, zadnego `spawn`, zadnego przebierania, wiec
nie ma momentu, w ktorym klient nie wie, co narysowac.

Nieuzywani czekaja **20 blokow pod kotwica**: poza kadrem, bez cienia (nie ma pod nimi na
czym go narysowac) i w tym samym chunku, czyli caly czas w zasiegu sledzenia klienta —
to ostatnie jest warunkiem calej sztuczki, bo encja, ktora wypadnie ze sledzenia, zostanie
przy powrocie utworzona od nowa i miganie wroci. Stad `PARKING_DROP = 20` przy domyslnym
zasiegu 48.

Kat obrotu nowy nosiciel przejmuje po poprzedniku juz w teleporcie. `PreviewRotationTask`
wymusza go co 2 ticki, ale te dwa ticki widac: bez tego nosiciel wjezdzalby na kotwice bokiem.

Klucz `preview.skin-warmup-ticks` zniknal — nie ma juz czego rozgrzewac.

Pilnuje tego `eval/scenarios/90-stajnia-podgladu.js`: po wejsciu ma stac komplet nosicieli,
a seria pieciu przelaczen ma dac **zero** `spawn_entity` i **zero** `entity_destroy`.
Zrzuty z klienta: `output/kreator-responsywnosc-01/captures/s1-wejscie.png` (czysta scena,
jeden cien) i `s3-nieumarly-ninja.png` (przelaczona rasa i klasa).
## Rasa przy OP i sprawdzenie, gdzie naprawde ladowala skorka (2026-09-20)

Zgloszenie: *"klasa sie przypisuje, ale co do rasy nie bylbym taki pewny, a skorka
z kreatora nie wchodzi i nie zapisuje sie na postaci na profilu mmoprofile"*.

### Rasa: konto z OP nigdy nie dostawalo permisji rasy

To bylo prawdziwe i widac je w logu wprost. Przy tworzeniu postaci przez wlasciciela
(`19:50:29`) poszly TRZY komendy `unset` i ANI JEDNA `set`:

```
Class of player Miau changed to Warrior.
[LP] miau does not have race.ork set in context global.
[LP] miau does not have race.elf set in context global.
[LP] miau does not have race.nieumarly set in context global.
```

Przyczyna: `ConfirmService#applyToRealPlayer` pytalo o stan permisji przez
`player.hasPermission(...)`, a to dla gracza z OP zwraca **true dla kazdej permisji**.
Wiec warunek "nie ma jeszcze swojej rasy" byl zawsze falszywy (komenda nadajaca nie szla),
a warunek "ma obca rase" zawsze prawdziwy (szly bezsensowne `unset` na wszystkie pozostale).
Dla konta bez OP dzialalo to poprawnie — dlatego nie wylapal tego zaden eval.

Naprawa to jedno slowo: `isPermissionSet`, ktore pyta o to, co naprawde chcemy wiedziec —
czy ten wezel jest graczowi PRZYPISANY — i nie klamie przy OP. Po wdrozeniu, na koncie
testowym, log pokazuje dokladnie jedna komende na kazda strone zmiany:

```
[LP] Unset race.ork for skinqa in context global.
[LP] Set race.nieumarly to true for skinqa in context global.
```

Ograniczenie tej weryfikacji: nadania OP koncie testowemu nie dalo sie tu wykonac, wiec
galaz "gracz z OP" opiera sie na logu powyzej i na semantyce obu metod, a nie na powtorzonym
przebiegu. Przy najblizszej postaci tworzonej przez konto z OP w logu ma sie pojawic
`[LP] Set race.<rasa> to true`.

Warto przy tym pamietac, ze `race.*` dzis NIKT nie czyta (patrz javadoc applyToRealPlayer) —
jedyne, co gracz widzi po rasie, to skorka. "Rasa sie przypisala" znaczy w praktyce
"skorka jest wlasciwa".

### Skorka: NIE udalo sie odtworzyc usterki — udalo sie za to pokazac, ze dziala

Sprawdzone trzema niezaleznymi metodami, na zywym world1:

- klient QA (prawdziwy renderer, zrzuty w `output/skin-na-graczu-01/captures/`): postac po
  wyjsciu z kreatora ma skorke swojej pary — nieumarly+mag (`08`), elf+ninja (`17`),
  ork+akolita — i ma ja nadal po minucie (`09`) oraz po relogu i przelaczeniu profilu (`27`),
- eval 70 (protokol): wartosc `textures` wysylana klientowi jest bajt w bajt ta z `skins.yml`,
  przy tworzeniu, po relogu i przy powrocie na pierwsza postac,
- glowy na ekranie wyboru postaci: niosa profil `cc_<rasa>_<klasa>` z ta sama tekstura —
  zrzuty `20`/`24` i nowy eval `95-glowy-na-liscie`.

Jedno zastrzezenie z obserwacji: **glowa swiezo utworzonej postaci potrafi przez kilka sekund
wygladac jak domyslny Steve**. To ta sama leniwa sciezka klienta co przy podgladzie w sali —
dopoki tekstura nie jest rozwiazana, klient rysuje domyslna. Rozwiazuje sie sama i od tej pory
jest w cache'u. Falszywy trop, ktory z tego wyrosl (i zostal wycofany): probowalem to
tlumaczyc wyscigiem z przerysowaniem okna przez MythicLib i dolozylem kolejne podejscia
`APPLY_DELAYS_TICKS`. Powtorzenie na starym jarze pokazalo, ze glowy sa poprawne od pierwszego
otwarcia takze BEZ tej zmiany — wiec zmiana wrocila do `{1, 10}`, a zostal po niej tylko eval.

### Jedyna znaleziona roznica konta wlasciciela: zaleglosc po CMI

W bazie CMI (`plugins/CMI/cmi.sqlite.db`, tabela `users`, kolumna `Skin`) **dokladnie jedno
konto na 253 ma zapisana skorke: `Miau` -> `Miau`**. To slad po POPRZEDNIEJ wersji tego
pluginu, ktora przy kazdym wyborze profilu wolala `cmi skin <gracz> <gracz> -s` (dzisiejszy
kod tego nie robi, patrz komentarz na koncu `applyToRealPlayer`).

Ma to znaczenie, bo `com.Zrips.CMI.Modules.Skin.SkinListener#onPlayerLogin` (bajtkod)
sprawdza `user.hasSetSkin()` i dla takiego konta odpala `applySkinAsync(player, user.getSkin())`
w ZADANIU ASYNCHRONICZNYM Z OPOZNIENIEM — czyli po naszym nalozeniu skorki postaci. Modul jest
wlaczony (`Settings/Modules.yml: skin: true`).

**To jest podejrzany, a nie udowodniony sprawca**: zadna proba zmiany skorki przez
`cmi skin <gracz> <skorka>` z konsoli nie dala tu ZADNEGO efektu (gracz online, zrzuty `30`
i `31` identyczne), wiec nie umiem pokazac, zeby CMI faktycznie cokolwiek nadpisal.
Zaleglosc warto skasowac (`/cmi skin off`, gdy wlasciciel jest online), a jesli skorkami ma
rzadzic wylacznie system postaci — wylaczyc caly modul (`Settings/Modules.yml: skin: false`).

## Profil-duch "Main" i klasa domyslna MMOCore (2026-09-20)

### Skad sie bierze profil o tym samym id, co gracz

Z `ProfileListImpl` (bajtkod MMOProfiles 1.2) — przy wczytywaniu listy profili:

```java
if (configManager.defaultProfile          // default-profile: true w config.yml
    && player.hasPlayedBefore()           // istnieje world/playerdata/<uuid>.dat
    && !hasLoggedInOnce)                  // w userdata/<uuid>.json nie ma HasLoggedInOnce
    profiles.add(new PlayerProfileImpl(null, tryFindFirstJoinLocation(), this, player));
```

Taki profil dostaje **id rowne uuid gracza** i nazwe z `default-main-profile-name` ("Main").
Powstaje **z pominieciem `ProfileCreateEvent`**, wiec kreator go nie widzi i nie ma dla niego
rekordu w `characters.yml` — po jego wybraniu gracz gra bez rasy, bez klasy i bez skorki,
a w logu leci `Profil ... nie ma rekordu postaci w characters.yml`.

Odtworzone celowo: koncie testowemu, ktore MA `world/playerdata/<uuid>.dat`, przeniesiono
`plugins/MMOProfiles/userdata/<uuid>.json` poza katalog i kazano wejsc ponownie — na ekranie
wyboru stanelo `[1] Main`. Czyli warunek to nie "przejscie przez lobby", tylko **playerdata
na world1 bez odpowiadajacego mu pliku userdata MMOProfiles**.

Ustawione `default-profile: false`. Opcja istnieje dla serwerow, ktore instaluja MMOProfiles
na dzialajacej bazie graczy i chca zachowac ich dotychczasowe dane — tutaj wszystkie dane
graczy i tak siedza w profilach. **To jest warunek konieczny przed czyszczeniem postaci**:
z `true` kazdy z 308 graczy majacych playerdata dostalby po skasowaniu userdata wlasnie taki
profil-widmo.

### `Could not find class 'HUMAN'` — czemu sam config tego nie wylaczal

`ClassManager#initialize` szuka wsrod klas z plikow tej z opcja `default`, a gdy jej nie ma,
tworzy w locie `PlayerClass("HUMAN", "Human", LEATHER_BOOTS)` i przypisuje ja **tylko do pola
`defaultClass`, bez rejestracji w mapie klas**. Kazde wczytanie profilu z wpisem
`class-info: HUMAN` konczylo sie wiec `NullPointerException` w `YAMLDatabaseImpl:118`
(`Validate.notNull(classManager.get("HUMAN"))`).

Sam `save-default-class-info: false` tego NIE zalatwia — warunek zapisu brzmi
`!profess.hasOption(DEFAULT) || saveDefaultClassInfo`, a syntetyczna klasa HUMAN nie ma
zadnych opcji, wiec `hasOption(DEFAULT)` jest falszywe i zapis idzie mimo wszystko.
Zmierzone: po samym przestawieniu klucza swiezo utworzony profil nadal dostawal
`class-info: HUMAN`, a jego wczytanie nadal rzucalo NPE (licznik w logu 1 -> 2).

Naprawa to `plugins/MMOCore/classes/human.yml` z `options: { default: true }` — klasa ma
teraz prawdziwa definicje, `get("HUMAN")` ja znajduje (id bierze sie z nazwy pliku, wiec
pasuje takze do wpisow zapisanych wczesniej), a `save-default-class-info: false` zaczyna
dzialac. Po obu zmianach: wczytanie tego samego profilu daje **0 NPE**, a nowo utworzony
profil nie ma juz sekcji `class-info` w ogole.

## Okna dialogowe a panele sceny (2026-10-25, paczka P5/F5)

Natywne okna Paper Dialog (ksiega "Nowa postac" `NewCharacterBook` oraz dialogi `SceneMenu`:
lista postaci, zarzadzaj, usun na zawsze, przerwij) maja polprzezroczyste tlo, a panele sceny
(`TextDisplay` karta i lista, bossbar topBar, action bar hint) rysuja sie w swiecie POD nim,
wiec przeswitywaly przez okno.

- `SceneView.hidePanels()` / `restorePanels()`: ukrywa panele (czysci tekst/tlo, topBar, hint) i
  przywraca je z zapamietanego tekstu (`cardText`, `listText`, `topBarText`). Podczas ukrycia
  `card()/list()/topBar()` tylko zapisuja tekst, a `SceneManager.pushHint` wysyla pusty action bar.
- `SceneManager.setPanelsHidden(Player, boolean)`: wejscie dla innych klas.
- `SceneMenu.setPanelHook(BiConsumer<Player,Boolean>)`: haczyk wolany z `true` przy otwarciu
  dialogu i z `false` po akcji, ktora go zamyka (anuluj, wroc do sceny, wybor). `forget()` zwalnia haczyk.
- Ksiega "Nowa postac": `hidePanels()` przed `book.open(...)` (`CharacterProfileModule.startInScene`
  i podglad `/cc scena`), `restorePanels()` w `back()` (przycisk Wroc / powrot do sceny).
- Wyjscie z serwera w oknie: widok sceny jest zamykany, po wejsciu scena wraca z widocznymi panelami.
