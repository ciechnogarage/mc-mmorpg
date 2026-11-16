# Kreator postaci — dwie wersje na dwoch serwerach

Do porownania stoja obok siebie:

| Serwer | Wersja kreatora |
|---|---|
| **world1** | plugin `CharacterCreator` (bez zmian w kodzie; wszystkie rasy maja `model-id: 'none'`) |
| **world3** | prototyp **bez wlasnego pluginu** — CoreTools + datapack, opisany nizej |

Prototyp na world1 zostal usuniety, zeby porownanie bylo czyste (pliki `cc_*` w CoreTools i
datapack `character_creator` juz tam nie istnieja).

## Prototyp (world3) — z czego sie sklada

Podglad postaci to **skorka**, nie model 3D, a menu **nie rysuje ramki ani ekwipunku** —
to ten sam trik co w pluginie (przezroczysta tekstura GUI w pakiecie), tylko na innym typie okna.

| Plik (w `/opt/docker/mc/data/world3/`) | Rola |
|---|---|
| `plugins/DeluxeMenus/gui_menus/cc_creator.yml` | menu typu HOPPER: 4 ikony ras + zatwierdzenie, klikniecia wolaja skrypty CoreTools |
| `plugins/Nexo/pack/assets/minecraft/textures/gui/container/hopper.png` | przezroczysta (alpha 0) tekstura GUI lejka — stad brak panelu |
| `plugins/Nexo/pack/assets/minecraft/lang/{en_us,pl_pl}.json` | `container.inventory: ''` — kasuje napis „Inventory" pod ikonami |
| `plugins/Nexo/pack/.../gui/sprites/container/slot_highlight_*` | przezroczyste podswietlenie slotow |
| `plugins/CoreTools/Variables/cc_character.yml` | `cc_race`, `cc_race_id`, `cc_done` — per profil MMOProfiles, czyli per postac |
| `plugins/CoreTools/Scripts/cc_creator.yml` | wejscie, wybor rasy, zatwierdzenie, pulapka na ESC, `cc_join` |
| `plugins/CoreTools/EventsManager/cc_creator.yml` | `PlayerJoinEvent` -> kreator, `InventoryCloseEvent` -> pulapka na ESC |
| `world/datapacks/character_creator/.../preview_*.mcfunction` | podglad: jeden vanillowy `mannequin`, podmiana `profile.properties` na skorke rasy |
| `world/datapacks/character_creator/.../build_room.mcfunction` | pokoj kreatora (jednorazowy build) |
| `world/datapacks/character_creator/.../scene.mcfunction` | poludnie i czysta pogoda w limbo |
| `plugins/LibsDisguises/{Skins,SavedSkins}/cc_*` | zrodlowe skorki ras i ich tekstury z mineskin |

### Czego sie NIE dalo zrobic (sprawdzone, nie zalozone)

- **Dialog vanilla** (`/dialog`) wyglada najlepiej: zero ramek, scena prawie nieprzyciemniona.
  Odpadl, bo jego przyciski wykonuja komende z uprawnieniami GRACZA, a `core-cast` jest dla OP.
  Zaden mostek nie przenosi nicku: DeluxeMenus nie podstawia zadnego placeholdera w
  `open_requirement.deny_commands`, a datapack nie zaladuje funkcji z komenda pluginu
  (`Unknown function`). Zostaloby tylko `/trigger` + polling, czyli wiecej ruchomych czesci
  niz cala reszta kreatora.
- **`inventory_type: SHULKER_BOX`** w DeluxeMenus jest ignorowane (menu i tak wychodzi skrzynia).
  `HOPPER` jest honorowane — i dobrze sie sklada, bo 5 slotow to dokladnie 4 rasy + przycisk,
  a przezroczysta trzeba zrobic tylko teksture lejka, ktorej gracze poza kreatorem nie ogladaja.
  (Przezroczysta skrzynia zabralaby tlo KAZDEMU menu i kazdej skrzyni na serwerze.)
- **`%player_name%` w DeluxeMenus nie dzialal** — brakowalo rozszerzenia PlaceholderAPI
  `Player`. Doinstalowane (`papi ecloud download Player`); bez tego klikniecia w menu nie
  robily nic, bo do konsoli szedl literal `%player_name%`.
- **`close_commands` DeluxeMenus nie odpalaja sie przy ESC**, dlatego pulapka siedzi na
  `InventoryCloseEvent` w EventsManager, z warunkiem na swiat `creator_limbo` (bez tego
  otwieralaby kreator kazdemu, kto zamknal jakakolwiek skrzynie).

## Co jest sprawdzone KLIENTEM w grze

Przejscie kreatora zostalo obejrzane realnym klientem 1.21.11 (kontener `mc-client-test`,
Xvfb 1280x720, guiScale 2). Zrzuty: `output/kreator-config-only-01/captures/`.

- menu otwiera sie, klikniecie rasy natychmiast przebiera postac (czlowiek/ork/elf/nieumarly
  sa od siebie odroznialne — `028-porownanie-ras.png`),
- pulapka na ESC dziala: zamkniecie menu przy `cc_done=0` otwiera je z powrotem,
- `Comparator` na `cc_race_id` dziala (blokada zatwierdzenia bez wyboru),
- zatwierdzenie konczy kreator: permisja `race.<rasa>` ladzie w LuckPerms, dummy znika,
  gracz wyladowuje na spawnie `world` (`029-po-zatwierdzeniu.png`),
- klient odswieza skorke mannequina po samym `data merge`, bez respawnu encji.

Nie sprawdzone dalej: `cc_join` po wejsciu na serwer (testowe konto wchodzilo przez
`/core-cast`), oraz zachowanie przy dwoch graczach naraz.

## Co poprawiono po obejrzeniu klientem (dwie tury)

1. **Postac byla zaslonieta przez GUI.** Panel menu zajmuje pas x 467-813; dummy stojacy na
   wprost gracza chowal sie za nim (widac bylo same nogi — `005-po-kliknieciu-ork.png`).
   Teraz stoi z boku, w lewej trzeciej ekranu, obrocony 3/4 do gracza (`tp -1.1 100 -2.2 -31 0`).
2. **Pusty pokoj na wejsciu.** Przy braku wyboru nie bylo zadnego podgladu. Teraz na wejsciu
   stoi czlowiek jako neutralny placeholder.
3. **Skorki byly za ciemne.** Otwarte GUI kontenera przyciemnia caly swiat za soba — paleta v1
   zlewala sie w brazowa plame. Paleta v2 jest jasniejsza i kontrastowa.
4. **Ikony ras byly nieczytelne.** Glowy ze skorka base64 renderowaly sie jako identyczne ciemne
   plamy (klient nie pobieral skorki; w logu serwera Mojang HTTP 429) i dostawaly w tooltipie
   lewa linie "Dynamic". Zamienione na plaskie bloki w barwach ras — cztery rasy widac przy 16 px,
   a linia "Dynamic" zniknela.
5. **Tooltip "Wybrana rasa: X" klamal** — lore renderuje sie przy otwarciu menu i zostawalo
   'none' po wyborze; `enable_animation: true` tego nie odswieza. Linia usunieta.
6. **Scena**: `cc:scene` ustawia poludnie i czysta pogode, a cykl dnia w limbo jest zamrozony
   (`gamerule doDaylightCycle false` — z konsoli, bo `gamerule` nie laduje sie w funkcji).
7. **Okno na oknie.** Menu skrzyniowe rysowalo panel + ekwipunek + tooltip na tooltipie.
   Teraz okno nie rysuje sie w ogole: zostaja ikony i tytul, jak w pluginie.
8. **Pokoj.** Zamiast plyty w pustce jest sala: ciemne sciany z kwarcowymi pilastrami, jasna
   podloga, podest i jasny panel za postacia (kontrast dla nieumarlego), lampy i stale swiatlo.

## Znane ograniczenia

0. **GUI przyciemnia swiat.** Vanilla przyciemnia scene za kazdym oknem ekwipunku — zostaje
   to nawet po ukryciu samej ramki (porownaj `099-jasniejszy-pokoj.png` z menu i
   `100-esc-trap.png` bez menu). Serwer tego nie wylaczy; przeciwdzialanie to jasny pokoj
   i kontrastowe skorki. Jedyne UI bez tego efektu to dialog vanilla — odpadl z powodu
   uprawnien, patrz wyzej.
1. **Reakcja tylko na klik.** Zaden configowy GUI nie ma zdarzenia „najechalem na slot".
2. **Jeden dummy na swiat.** Dwoch graczy naraz w kreatorze dzieli tego samego mannequina.
   Docelowo: kotwica per gracz (funkcja makro z tagiem gracza) albo instancjonowane limbo.
3. **`/kill` w limbo dziala z opoznieniem** — encja znika dopiero, gdy chunk zacznie tickowac.
   Dlatego podglad reuzywa dummy'ego zamiast go kasowac.
4. **Permisja `race.<rasa>`** jest tak samo write-only jak w pluginie: nikt jej jeszcze nie czyta.
5. Menu nie odswieza sie po wyborze (zeby `close_script` nie migotal); informacja zwrotna to
   zmiana postaci w kadrze + pasek nad hotbarem. Zywe lore wymagaloby ponownego otwarcia menu,
   a to samo w sobie wywoluje `close_script` → pętla; obejscie to zmienna-straznik (`variabletemp`).
6. **HUD zostaje na ekranie** — paski Health/Mana z MythicHUD, serca i hotbar sa widoczne przez
   caly kreator. Plugin na world1 ma ten sam problem.
7. **Ikony ras** to juz te same itemy z pakietu co w pluginie (`cc:race_*`), ale sa male
   (16 px) — rase czyta sie glownie z postaci obok.
8. **Spawn po kreatorze jest niebezpieczny**: konto testowe dwa razy zginelo od zombie zaraz
   po wyjsciu (`104-po-zatwierdzeniu.png`). To zachowanie spawnu world3, nie kreatora —
   plugin na world1 robi to samo, ale przy porownaniu rzuca sie w oczy.

## Plugin na world1 po zmianie

Wszystkie cztery rasy maja `model-id: 'none'` — blueprint `race_nieumarly` zostaje na dysku,
ale config go nie wola. Rase (i klase) pokazuje SKORKA — patrz nizej.


## Skorki postaci (world1, plugin)

Od 2026-09-20 kreator na world1 ma wlasne skorki: **jedna tekstura na pare RASA x KLASA**,
czyli 16 plikow. Protokol nie ma drugiej warstwy skorki, ktora serwer moglby podmienic
osobno, wiec "podskin klasy" jest wypalony w te sama teksture co rasa:

| co niesie | czesc tekstury |
|---|---|
| RASA | glowa: karnacja, twarz, wlosy, uszy elfa / kly orka / kosci nieumarlego |
| KLASA | tors, rece, nogi + warstwa zewnetrzna: kaptur maga, maska ninja, naramienniki i tabard wojownika, stula akolity |

Ten sam podzial, ktory kreator robil juz ekwipunkiem dummy'ego (helm = rasa, zbroja = klasa).

### Droga od PNG do gracza

1. `plugin-src/character-creator/tools/gen_race_skins.py` — generuje 16 PNG 64x64
   (`tools/out/skins/`) i stykowke `_podglad.png` do ogladania bez wchodzenia do gry.
2. `tools/upload_skins.sh` — wgrywa PNG na mineskin.org **wprost przez API v2** (serwer nie
   musi dzialac), czeka na zlecenie, pobiera `value` + `signature` i pisze `skins.yml`.
   Gotowe tekstury laduja w `tools/out/skins/.mineskin-cache.json` pod suma SHA-256 pliku
   PNG — powtorne uruchomienie nie zuzywa limitu, a przerysowana skorka jest wgrywana od nowa.
3. Plugin (`SkinService`) czyta `skins.yml` **z dysku** i naklada teksture na profil gracza
   (Paper `PlayerProfile`) w tym samym opoznionym zadaniu, co model rasy.

**Dlaczego mineskin, a nie wlasny hosting:** klient Javy pobiera skorki wylacznie
z `textures.minecraft.net` — adres spoza tej domeny jest ignorowany, takze przy
`online-mode=false`. Zeby wlasny PNG dalo sie w ogole zalozyc graczowi, musi najpierw trafic
na konto Mojanga i wrocic z podpisem; robi to mineskin.org.

**Dlaczego NIE przez `/grabskin` z LibsDisguises** (tak wygladala pierwsza wersja): MineSkin
v2 kolejkuje zlecenia i odpowiada `{"messages":[{"code":"job_queued"}]}`, a LibsDisguises
11.0.15 tej odpowiedzi nie zna — w logu serwera laduje `Received an unknown response from
MineSkin`, a skorka nie zapisuje sie NIGDY. Przechodzily tylko obrazy, ktore mineskin mial
juz w cache'u. Ta droga zajela 40 minut i zgubila 5 z 16 skorek; przez API v2 komplet poszedl
w minute (limit bez klucza: 10 zlecen/minute, 60/godzine, w odpowiedzi jest `rateLimit`).

### Gdzie ta skorka zyje

- **Podglad w kreatorze** — dummy nosi dokladnie te teksture, ktora gracz dostanie
  (wczesniej nosil skorke patrzacego gracza, wiec wyboru rasy nie bylo na nim widac).
- **Postac w grze** — `skins.apply-to-player: true` w `config.yml`; skorka wraca po kazdej
  zmianie swiata, respawnie i wyborze profilu, zawsze idempotentnie (bez migotania).
- **Postac w profilu** — klucz skorki laduje w `characters.yml` jako `skinSource`, wiec
  skorka idzie ZA POSTACIA: przelaczenie profilu przelacza skorke, a zmiana mapowania
  w configu nie zmienia wygladu istniejacych postaci.

### Czego to nie robi

- **Glowy na ekranie wyboru postaci (MMOProfiles) sa jednakowe.** `language/gui/profile-list.yml`
  rysuje `PLAYER_HEAD` bez tekstury, wiec kazdy wiersz pokazuje skorke AKTYWNEJ postaci,
  a nie tej z danego slotu. To ekran MMOProfiles, nie nasz — zmiana wymagalaby wsparcia po
  jego stronie (placeholder tekstury w itemie).
- **Gracz nie wybiera skorki** i nie zalozy wlasnej — wynika z pary rasa+klasa.

### Co obejrzane klientem (2026-09-20)

Zrzuty: `output/skorki-postaci-01/captures/`.

- `006` — kreator z domyslnym wyborem: czlowiek + wojownik, skorke widac na postaci,
- `007`-`010` — kliknieciem rasy i klasy podglad zmienia sie natychmiast (ork/wojownik,
  ork/mag, elf/mag, elf/akolita) — rasa czytelna po twarzy, klasa po stroju,
- `003-zoom` kontra `006-zoom` — PRZED i PO zdjeciu zbroi z dummy'ego: zelazny napiersnik
  zakrywal caly stroj klasy, a barwiony helm rasy glowe,
- `012` — para BEZ skorki (wtedy: nieumarly + ninja) — przebranie zastepcze w skorke widza;
  na koncie bez skorki wyglada to jak brakujaca tekstura, wiec komplet 16 skorek nie jest
  ozdobnikiem,
- `015`/`016` — postac w swiecie, widok F5: gracz nosi skorke swojej pary rasa+klasa.

### Cztery usterki znalezione przy skorkach — i co z nimi zrobiono (2026-09-20)

Zadna z nich nie dotyczyla skorek; wszystkie wyszly przy ogladaniu kreatora klientem.

1. **Wyjscie z kreatora bylo POD WODA.** `creator-room.exit` (239.5 / 63 / 208.5) to dno
   oceanu — konto QA utonelo kilkanascie sekund po utworzeniu postaci, a boty evali
   wychodzily stamtad z ubytkiem zycia. Pod woda byl tez
   `profile-selection-location` (189 / 63 / 209), czyli miejsce, w ktorym gracz STOI przez
   caly czas wybierania postaci.
   **Poprawione:** oba punkty plus `new-profile-spawn-point` wskazuja teraz **149.5 / 99 /
   197.5** — najblizszy plaski, suchy kawalek ladu (kamienny plaskowyz ~88 m na zachod).
   Wybrany nie na oko: heightmapy regionu (`MOTION_BLOCKING_NO_LEAVES` == `OCEAN_FLOOR`,
   czyli na wierzchu nie ma cieczy, deniwelacja 5x5 <= 1), potem potwierdzony w grze —
   blok 149/98/197 to kamien, a 99..101 nad nim to powietrze w calym kwadracie 3x3.
   Wspolrzedne siedza w DWOCH plikach (`plugins/CharacterCreator/config.yml` i
   `plugins/MMOProfiles/config.yml`) — przy zmianie poprawic oba. Eval `20-pelny-cykl`
   sprawdza je po nazwie, wiec rozjazd wyjdzie od razu.

2. **Falszywy alarm przy KAZDYM tworzeniu postaci.** W logu laduje
   `Profil <id> ... nie ma rekordu postaci w characters.yml`, a gracz dostaje czerwone
   "zgłoś to administracji" — tuz po tym, jak wszystko sie udalo. Przyczyna:
   `ProfileCreateEvent#validate()` odpala NATYCHMIASTOWY `ProfileSelectEvent`, a rekord
   zapisujemy linie pozniej (ta kolejnosc jest celowa — chroni przed rekordami bez profilu).
   **Poprawione:** `CharacterProfileModule` trzyma teraz znacznik "ten profil wlasnie
   powstaje" i na czas tego jednego okna zamienia blad na wpis `fine` w logu, bez wiadomosci
   do gracza. Prawdziwy profil bez rekordu nadal krzyczy tak samo glosno. Zmierzone: przed
   poprawka 1 wystapienie na 1 utworzona postac, po poprawce 0 na ~8.

3. **Glowy na ekranie wyboru postaci byly identyczne.** MMOProfiles rysuje kazdy profil jako
   `PLAYER_HEAD` bez tekstury, wiec kazda postac miala glowe Steve'a; placeholdera na
   teksture w jego configu nie ma.
   **Poprawione:** `ProfileListHeadsListener` dopisuje skorke postaci do glowy juz po
   zbudowaniu okna. Mapowanie slot -> postac bierze z jego wlasnego
   `language/gui/profile-list.yml`; przy stronicowaniu (wiecej postaci niz slotow) nie rusza
   niczego, zeby nie wstawic cudzej glowy. Wylacznik: `skins.profile-heads`. Sprawdzone
   klientem: slot [1] (Acolyte) ma glowe elfa, slot [2] (Warrior) — orka.

4. **Dane testowe.** Bylo 28 rekordow (26 z kont botow) i 9 profili MMOProfiles bez rekordu,
   ktore przy kazdym wejsciu takiego konta produkowaly blad w logu.
   **Posprzatane:** rekordy przez `/cc delete`, pliki profili kont testowych przeniesione
   (nie skasowane) do `backups/mmoprofiles-userdata-testy-20260920/`. Zostaly 2 rekordy i
   1 profil konta `Miau`. Jedna sierota (`Miauasdasdasda`) ZOSTAJE — to dane wlasciciela,
   do usuniecia jego decyzja (`/cc prune confirm`).

5. **Przy okazji: komunikat smierci.** `SkinQA was slain by {trigger_name}` bral sie
   z PRZYKLADOWEGO pliku CoreTools (`EventsManager/example-events.yml`), ktory nadpisywal
   kazdy komunikat o smierci jednym `Set{...;m=deathmessage}` — przy smierci bez zabojcy
   `{trigger_name}` nie ma czego podstawic. Sekcja `PlayerDeathEvent` jest zakomentowana
   (reszta pliku, join/quit, zostaje bez zmian). Sprawdzone botem: teraz w logu i na czacie
   jest vanillowe `EvSmierc6384 fell out of the world`.

### Czego nie ruszano

- `sessions.yml` ma ~20 wpisow po botach, ktore nigdy nie wroca (przy starcie leci o tym
  ostrzezenie). Plugin przepisuje ten plik przy wylaczaniu, wiec recznie mozna go czyscic
  tylko przy zatrzymanym serwerze.
- Profil `Main` pojawiajacy sie graczowi, ktoremu skasowano plik MMOProfiles, a ktory ma
  jeszcze vanillowe playerdata — artefakt samego sprzatania, nie sciezki gracza.
