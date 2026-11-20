# Rzemioslo (`plugin-src/rzemioslo`)
Wersja w źródle: `2.0.0-<rev>` (`gradle.properties:2` + `build.gradle.kts` `version = rzmVersion-rzmRev`; `plugin.yml` ma `${version}`). Jar na world1: `/opt/docker/mc/data/world1/plugins/rzemioslo-2.0.0-202610121414.jar`. Zgodność źródła z jarem: zgodna co do `plugin.yml` (nazwa, depend, softdepend, komenda, uprawnienie), listy klas (36 klas = klasy ze źródła) i zawartości `Komenda.class` (są literały `doreki`, `oboz`, `postaw`) oraz `Zbieranie.class` (metoda `wyslijNarzedzie`, z ostatniego commita 20d5ffe). Pełnej równości bajtkodu nie sprawdzałem (nie buduję).
Opis stanu z dnia: 2026-10-25, commit 8c79ff8 (gałąź `docs-pluginy`). Ostatni commit kodu pluginu: 20d5ffe (2026-10-12 14:43), wdrożony 12.10 14:45.

## Po co (co widzi gracz)
Profesje v2: 6 profesji zbierackich (górnictwo, drwalstwo, zielarstwo, farmerstwo, rybactwo, myślistwo) działa „z narzędziem”: PPM w węzeł (blok Nexo), zarzucenie wędki na łowisku, oprawienie zwłok zabitej bestii nożem. Narzędzie wystarczy mieć w plecaku (w dłoni pokazuje się tylko pakietem). Do tego buffy z alchemii/gotowania/krawiectwa (statystyki MythicLib), obozowisko, XP za wytwarzanie na stacjach MMOItems, perki co 10 poziomów oraz zlecenia dnia i zlecenia startowe. Pozostałe 7 profesji (kowalstwo, krawiectwo, jubilerstwo, alchemia, zaklinanie, gotowanie, budownictwo) dostaje XP tylko ze stacji i zleceń (lista w configu world1, klucz `profesje`).
Zielarz „w świecie” (kępy STRINGBLOCK, scena zbioru, odrost, zielnik, suszarnia, skaza; klasy `Zielarz` + `Z*`, opis niżej w „Zielarz”) jest w tym źródle od scalenia gałęzi `zielarz-f1` (`zielarz-naprawa`, 16.11); na world1 NIEWDROŻONY.

## Komendy i uprawnienia
Jedna komenda `/rzemioslo` (`src/main/resources/plugin.yml:8-12`), uprawnienie `rzemioslo.admin`, `default: op` (`plugin.yml:13-16`). Menu (DeluxeMenus) wołają ją z konsoli (`Komenda.java:15`). Podkomend w kodzie: 11 (policzone parserem po `case`), gracza wskazuje się nazwą (`Bukkit.getPlayerExact`), nieobecny gracz albo zły nick = komunikat „[Rzemioslo] gracz nie jest online: <nick>” dla nadawcy (`Komenda.java:159-163`).

| Podkomenda | Kto | Co robi | Źródło |
|---|---|---|---|
| `reload` | OP/konsola | `reloadConfig()` + nowy `Dane`, wypisuje liczby i `problemy` | `Komenda.java:25-29`, `Rzemioslo.java:109-115` |
| `info <gracz>` | OP/konsola | poziomy wszystkich profesji i aktywne buffy | `Komenda.java:75-88` |
| `zlecenie oddaj <gracz> <prof> <n>` | konsola (menu) | oddaje n-te zlecenie dnia (zabiera przedmioty, XP, pieniądze) | `Komenda.java:30-36`, `Zlecenia.java:143-163` |
| `start <gracz> <prof>` | konsola (menu) | zlecenie startowe: przyjmij → daje narzędzie T1; ponownie po zebraniu → nagroda XP | `Komenda.java:37-42`, `Zlecenia.java:170-197` |
| `flaga <gracz> <klucz> [wartość…]` | konsola (BetonQuest, menu) | ustawia flagę **postaci** (profil MMOProfiles) w `dane/<profil>.yml` pod `flagi.<klucz>`; bez wartości usuwa. Klucz: `a-z 0-9 _`, do 64 znaków (wielkie litery zamieniane na małe). Po co: postęp BetonQuest i zmienne MM są per konto (`docs/ref/betonquest/mythicmobs-npc.md:30`), a lekcja/zadania mają być per postać (właściciel 19.11) | `Komenda.java:43-52`, `Zlecenia.java:199-210` |
| `przeprawa <gracz> <ID> <komenda…>` | konsola/OP | zabiera 1 zestaw przeprawy (`uzycia.<ID>: przeprawa`) i wykonuje `<komenda>` z konsoli (`{gracz}` → nick) | `Komenda.java:53-66` |
| `narzedzie <gracz> <rodzaj> <tier>` | OP/konsola | daje `PROF_<RODZAJ>_<tier>` | `Komenda.java:67-74` |
| `doreki <gracz> <ID> [slot]` | OP/test | zamienia przedmiot na pasek (domyślnie slot 5) i ustawia jako trzymany; test/boty; slot 0-8, zły slot/nie-liczba = komunikat | `Komenda.java:89-112` |
| `oboz <gracz> <ID>` | OP/test | rozbija obozowisko pod graczem bez PPM | `Komenda.java:113-120` |
| `postaw <nexoId> <świat> <x> <y> <z>` | OP/konsola | stawia węzeł/łowisko (blok Nexo) — API dla lochów/wysp (MythicMobs); nic nie robi, gdy już stoi; odmawia, gdy blok zajęty | `Komenda.java:121-139` |
| `las <świat\|loch_001_*> <x1> <z1> <x2> <z2> [trwale]` | OP/konsola | obszar, w którym drzewa (pień `OAK_LOG`/`SPRUCE_LOG` z liśćmi) są ścinalne; `loch_001_*` = każda instancja MythicDungeons tej mapy; `trwale` zapisuje do `drwal.yml` | `Komenda.java` `case "las"`, `Drwal.java:256` |
| `drzewo <dodaj\|usun\|sprochnialy> <świat> <x> <y> <z>` | OP/konsola | pojedyncze drzewo do ścinki (trwale, `drwal.yml`), `sprochnialy` = zawsze puste w środku | `Drwal.java:264` |
| `drzewa-skan <świat> <x1> <z1> <x2> <z2>` | OP/projekt | lista ścinalnych drzew w obszarze (gatunek, pień, liście) — do sprawdzenia lasu w lochu przed rozstawieniem | `Drwal.java:279` |
| `drwal <gracz> [cel <nr>]` / `drwal pomiar on\|off` | OP/test | stan sceny ścinki (faza, ciosy, kąt, klin, cięcia) / punkt cięcia leżącego pnia dla bota; `pomiar` = logi `[drwal] …` | `Drwal.java:1138,1148` |
| `wezel <x> <y> <z> [świat]` | OP/konsola | diagnostyka: co to za blok, jaki węzeł i tier | `Komenda.java:140-153` |

Pomoc (`Komenda.java:165-169`), `usage` (`plugin.yml:11`) i tab-complete (`Komenda.java:175-200`) obejmują wszystkie 11 podkomend; tab-complete podpowiada też graczy online, profesje, rodzaje narzędzi, obozy, światy i id węzłów, tylko dla `rzemioslo.admin`. Błędna liczba w `zlecenie oddaj`/`doreki` albo nieznany świat w `wezel` = komunikat dla nadawcy, bez wyjątku.

## Konfiguracja
Jedyny plik: `/opt/docker/mc/data/world1/plugins/Rzemioslo/config.yml` (305 KB). **Generowany** przez `MCMMORPG/profesje/generuj.py` z `tresc.py` (nagłówek pliku, linie 1-2); ręcznie wolno zmieniać tylko sekcję `komunikaty` (generator ją zachowuje). Bez pliku plugin się wyłącza (`Rzemioslo.java:71-75`). Czyta go `Dane.java` (całość tylko odczyt, `Dane.java:56-123`). Brak „domyślnej w kodzie” dla list; liczby poniżej to wartość na world1 (policzone parserem snakeyaml) i domyślna z kodu.

Klucze najwyższego poziomu na world1 (25):

| Klucz | Znaczenie | World1 / domyślna w kodzie | Źródło |
|---|---|---|---|
| `krzywa` | `g_t1`, `g_wzrost`, `kara_nizszego_tieru`: g(tier)=g_t1·g_wzrost^(tier-1) = XP za zebranie; XP × kara^(różnica tierów) gdy gracz jest w wyższym tierze niż źródło | 10 / 1.25 / 0.5 (te same domyślne) | `Dane.java:57-59,159`, `Profesje.java:36-44` |
| `zbieranie` | `czas-bazowy` 3.0, `czas-oprawiania` 2.5, `czas-min` 1.2, `czas-na-tier-narzedzia` 0.08, `czas-na-poziom-w-tierze` 0.01 (s i ułamki), `zasieg-ruchu` 1.5 (bloków), `wyczerpanie-s` 45, `zwloki-s` 30, `machniecie-co-tickow` 7 | wartości world1 = domyślne w kodzie | `Dane.java:60-68` |
| `tiery` | 10 tierów: `nazwa`, `start` (poziom), `rzymska` (I..X); tier = akt, np. 1 „Zielona Granica” od poz. 1, 10 „Kres” od 91 | 10 wpisów; domyślny start tieru = 10t-9 | `Dane.java:69,165-167` |
| `profesje` | 13 profesji: `nazwa`, `narzedzie` (rodzaj dla zbierackich); `zbieracka` NIE jest czytane | 13 | `Dane.java:70-73` |
| `surowce` | 158 id MMOItems; używane tylko w kontroli startowej „brak przedmiotu” | 158 | `Dane.java:74`, `Rzemioslo.java:122` |
| `narzedzia` | 60 narzędzi `PROF_<RODZAJ>_<tier>` (6 rodzajów × 10 tierów): `rodzaj`, `tier`, `profesja`; `wytrzymalosc` jest wczytywane, ale kod nigdzie go nie używa (wytrzymałość to `MaxDamage` na przedmiocie) | 60 | `Dane.java:75`, `Przedmioty.java:85-95,127-135` |
| `wezly` | 62 węzły (id Nexo, np. `wezel_t1_mining`): `profesja`, `tier`, `glowny`/`poboczny`/`rzadki` (+ `szansa_poboczny`, `szansa_rzadki`), `nazwa`, `wyczerpany` (materiał bloku „pusty”, domyślnie STONE), `dzwiek`, `dzwiek_koniec`. Pola `nowy`, `typ_bloku` nie są czytane. Podział: farming 21, mining 19, herbalism 12, woodworking 10 | 62 | `Dane.java:76,125-139` |
| `lowiska` | 10 tierów łowisk: `glowny`, `poboczny`, `szansa_poboczny` (0.15), `rzadkie` (lista [id, szansa]); pole `blok` nie jest czytane | 10 | `Dane.java:78-85` |
| `lowisko-bloki` | 9 id znaczników Nexo `lowisko_t2..t10` → tier (T1 = każda woda) | 9 | `Dane.java:86-88` |
| `zwierzyna` | 13 internal-name mobów MythicMobs → źródło zwłok (`glowny`, `poboczny`, `rzadki`, szanse), profesja domyślnie `hunting` | 13 | `Dane.java:77` |
| `buffy` | 140 przedmiotów (14 rodzajów × 10 tierów): `grupa` (posilek 50, eliksir 40, mikstura 30, opatrunek 10, olej 10), `czas` s, `cooldown` s, `staty`, `tier`, `nazwa`, `druzyna`; klucze `staty`: `leczenie`, `mana`, `oczyszczenie`, `leczenie_w_czasie` lub nazwa statystyki MythicLib | 140 | `Dane.java:89-90`, `Buffy.java:105-143` |
| `obozowisko` | 10 obozów `PROF_OBOZOWISKO_<t>`: `czas` (600 s), `promien` (8), `staty` (health-regeneration, mana-regeneration) | 10 | `Dane.java:91-92`, `Buffy.java:255-271` |
| `uzycia` | 60 id → rodzaj użycia; kod czyta tylko wartość `przeprawa` (30 szt.); `naprawa` (10), `zwoj` (10), `obozowisko` (10) nie są czytane przez ten plugin | 60 | `Dane.java:93-94`, `Komenda.java:58` |
| `receptury` | 642 receptury stacji (id = id receptury MMOItems): `profesja`, `tier`, `xp`, `stacja`, `wynik`, `najdrozszy` (składnik do zwrotu z perku) | 642 | `Dane.java:95-96`, `Stacje.java:21` |
| `perki` | `zbieractwo` {co 10, podwojny 0.04, czas 0.01}, `rzemioslo` {co 10, zwrot 0.02} | 10 / 0.04 / 0.01 / 0.02 | `Dane.java:97-100` |
| `zlecenia` | 13 profesji × tiery 1-10: lista {id, ilosc, xp, pieniadze} (2-13 pozycji na tier) | 13 | `Dane.java:101-114` |
| `zlecenia-dziennie` | ile zleceń dziennie na profesję | 3 / 3 | `Dane.java:115` |
| `zlecenia-startowe` | 6 profesji zbierackich: {zbierz, ilosc 5, narzedzie T1, xp 50} | 6 | `Dane.java:116-117` |
| `nazwy`, `typy` | 826 nazw wyświetlanych i 826 typów MMOItems (domyślnie `MATERIAL`) po id przedmiotu | 826 / 826 | `Dane.java:118-121`, `Przedmioty.java:33` |
| `komunikaty` | nadpisanie tekstów (&-kody, `{zmienne}`); **na world1 sekcji nie ma** — działają `DOMYSLNE` z kodu (35 kluczy) | brak | `Rzemioslo.java:21-58,142-146` |
| `debug` | `true` = log śmierci mobów w `Oprawianie` | brak (false) | `Oprawianie.java:88` |
| `wersja`, `haki-swiata` (30), `xp-receptur` (642), `perki-progi` (10), `relikty` (44) | nieużywane w kodzie pluginu (grep w `src/main` pusty); czytają je narzędzia `MCMMORPG/profesje/` (audyt, menu) — tego nie sprawdzałem | — | grep `src/main` |

## Jak działa
- **Węzły** (`Zbieranie.java`): `PlayerInteractEvent` LOWEST na PPM w blok Nexo będący `wezly` (`:94-120`) blokuje użycie bloku/przedmiotu; PPM z jedzeniem, buffem lub mikstury w ręce przepuszcza (`:106-107`). Start wymaga poziomu ≥ `start(tier)` (`:167-172`) i narzędzia tieru ≥ tieru węzła (najlepsze z całego ekwipunku, `Przedmioty.java:98-114`), niezepsutego (`:183`). Sesja = `BukkitTask` co 1 tick (`:191`), przerywana ruchem > `zasieg-ruchu`, zmianą slotu, obrażeniami, wyjściem (`:198-200,319-339`). Czas: `baza·(1-0.08·nadwyżka tieru narzędzia)·(1-0.01·poziomy w tierze)·(1-0.01·progi)`, min `czas-min` (`Profesje.java:58-65`). Po sukcesie: losowanie głównego/pobocznego/rzadkiego (`:255-262`), perk podwójnego urobku (podwaja pierwszy przedmiot, `:231-232`), XP, -1 wytrzymałości (narzędzie nigdy nie znika, tylko „zepsute”, `Przedmioty.java:126-135`). Wyczerpanie jest **per gracz** (pakiet `sendBlockChange`), 45 s (`:342-359`), przywracane przy ładowaniu chunku (`:361-379`). LPM/niszczenie węzła anulowane poza trybem kreatywnym (`:141-159`).
- **Narzędzie w dłoni** tylko pakietem `set_player_inventory` (PacketEvents), ponawiane co tick, bo inne pluginy odświeżają slot (`:299-308`, `:203-204`).
- **Łowienie** (`Lowienie.java`): `PlayerFishEvent` HIGH; wędka profesji musi być w ręce (główna lub druga, `:32-41`), zwykła wędka łowi po staremu bez XP (`:59-65`). Tier wody = najwyższy znacznik Nexo `lowisko_tN` (blok NOTE_BLOCK) w kostce 13×13×13 wokół haczyka (`:21-52`); czas brania skraca tier wędki (`:73-74`). Zbyt słaba wędka/poziom = ryba znika (`:80-90`).
- **Myślistwo** (`Oprawianie.java`): `MythicMobDeathEvent` MONITOR; moby z `zwierzyna` zostawiają `Interaction` + `ItemDisplay` (niepersistentne) na `zwloki-s`; oprawiać może zabójca i drużyna MMOCore (`:83-115`), PPM nożem uruchamia ten sam silnik sesji (`:117-130`). Zabójcę ustala `getKiller` albo ostatni cios w ciągu 10 s (`:65-81`).
- **Buffy** (`Buffy.java`): `PlayerItemConsumeEvent` LOWEST sprawdza cooldown grupy (`:65-75`), `ConsumableConsumedEvent` MONITOR (MMOItems) stosuje buff (`:77-90`). Statystyki jako `TemporaryStatModifier` MythicLib o stałym UUID (ponowne użycie nadpisuje, `:196-199`); jeden aktywny buff na grupę (`:136-139`); `druzyna` = uczta dla członków party w 8 blokach (`:83-103`); `oczyszczenie` zdejmuje 6 złych efektów i daje odporność na czas (`:122-125,169-174`); opatrunek leczy co 1 s i przerywa się przy obrażeniach (`:145-167`).
- **Obozowisko** (`Buffy.java:206-279`): PPM przedmiotem z `obozowisko` na górną ścianę bloku; jeden obóz na gracza, zadanie co 40 ticków dodaje 50-tickowe modyfikatory regeneracji graczom w promieniu poza walką (`isInCombat`).
- **Stacje** (`Stacje.java`): `PlayerUseCraftingStationEvent` MONITOR; XP przy `CRAFTING_QUEUE`/`INSTANT_RECIPE` (odbiór), perk zwrotu najdroższego składnika przy `INTERACT_WITH_RECIPE`/`INSTANT_RECIPE` (`:25-37`). Receptury stacji nie mają `exp{}` — XP daje wyłącznie ten plugin (komentarz `Stacje.java:11-12`).
- **XP**: `PlayerData.getCollectionSkills().giveExperience(..., EXPSource.SOURCE, ...)` (`Profesje.java:42`), więc przechodzi przez `PlayerExperienceGainEvent` i blokada plugin Mistrzostwa (softdepend) działa sama (`Profesje.java:12-13`).
- **Perki**: progów = poziom/10 (`Profesje.java:47`); szansa podwójnego urobku = progi·0.04, zwrotu = progi·0.02, skrócenie czasu = progi·1% (przy poz. 100: 40%, 20%, 10%).
- **Zlecenia** (`Zlecenia.java`): 3 dziennie na profesję z puli bieżącego tieru gracza, losowane deterministycznie z (profil+data+profesja) (`:112-141`); data = lokalna data serwera (`LocalDate.now()`, `:114`). Startowe: stany `brak`/`aktywne`/`odebrane`, postęp liczy `zebrano()` wołane z węzłów, łowisk i zwłok.
- **Integracje**: MMOCore (profesje, party), MythicLib (staty, action bar), MMOItems (przedmioty, stacje), Nexo (bloki; kontrola po `NexoItemsLoadedEvent`, `Rzemioslo.java:137-140`), MythicMobs (śmierć moba), PacketEvents, PlaceholderAPI (`%rzemioslo_...%`, `Placeholdery.java:10-16`: `zlecenie_<prof>_<n>_{tytul,opis,masz,ilosc,xp,kasa,akcja,id,typ}`, `start_<prof>_{opis,postep,akcja}`, `buff_<grupa>`, `tier_<prof>`, `flaga_<klucz>` = wartość flagi postaci albo pusty napis, `Placeholdery.java:33`), Vault (wypłata za zlecenia, `Zlecenia.java:159-160,224-228`), Mistrzostwa (soft).
- **Komunikaty** idą na action bar z priorytetem HIGH przez MythicLib (`Rzemioslo.java:148-154`).

## Koszt dla serwera
Drwal: pomiar w sekcji „Drwal” niżej (20.11, klon). Nie mierzone (brak spark/MSPT w repo). Z kodu: tykają tylko aktywne sesje zbierania (1 zadanie/gracz co tick, pakiet narzędzia + `sendBlockDamage` do graczy w 32 blokach, `Zbieranie.java:195-223`) i obozy (co 2 s, pętla po graczach świata, `Buffy.java:252-270`); reszta na zdarzenia. `tierWody` przeszukuje 2197 bloków na złowioną rybę (`Lowienie.java:43-52`). Zapis zleceń (przy każdym `zebrano()` w aktywnym zleceniu startowym, przyjęciu i oddaniu, `Zlecenia.java:132,157,179,191,209,219`): na głównym wątku tylko `saveToString`, plik pisze osobny jednowątkowy executor (`Zlecenia.java:70-92`).

## Dane
- Zlecenia: `plugins/Rzemioslo/dane/<UUID>.yml` (klucze `dzien`, `zlecenia.<prof>.{tier,wybrane,oddane}`, `start.<prof>.{stan,postep}`, `flagi.<klucz>`), per **postać**: UUID profilu MMOProfiles (`MMOPlayerData.getProfileId()`), a bez profilu UUID gracza (`Zlecenia.java:45-51`). Katalog `dane/` na world1 był pusty 2026-10-25.
- Stan w pamięci, tracony przy restarcie/przeładowaniu: aktywne sesje, wyczerpane węzły (per **UUID gracza** — przy MMOProfiles wspólny dla wszystkich postaci konta), cooldowny i aktywne buffy, obozy, zwłoki (`Zbieranie.java:77-80`, `Buffy.java:55-60`, `Oprawianie.java:61`).
- XP/poziomy profesji trzyma MMOCore, nie ten plugin.

## Zielarz — moduły
| Klasa | Co robi |
|---|---|
| `Rzemioslo` | start, rejestracja modułów; `config.yml` GENEROWANY przez `MCMMORPG/profesje/generuj.py` (nie edytuj ręcznie) |
| `Profesje`, `Dane`, `Przedmioty` | poziomy/XP/perki przez MMOCore, dane z configu, MMOItemy i narzędzia |
| `Zbieranie`, `Lowienie`, `Oprawianie`, `Stacje`, `Buffy` | węzły Nexo, łowiska, zwłoki, XP stacji, buffy i obozowisko |
| `Zlecenia` | zlecenia dnia per POSTAĆ (profil MMOProfiles), pliki `dane/` |
| `Zielarz` + `Z*` | zielarz: kępy STRINGBLOCK, scena zbioru (`ZScena`, `ZRoslina`, `ZFeeling`), odrost, zielnik, suszarnia, skaza |
| `ZMagazyn` | dysk zielarza (F1): jeden wątek zapisu/odczytu |
| `Komenda` | `/rzemioslo …` (perm. `rzemioslo.admin`, menu wołają z konsoli) |

Zachowanie (16.11, gałąź `zielarz-naprawa`):
- Zbiór ze sceny liczy się do zlecenia startowego zielarstwa tak jak zbiór z węzła (`Zielarz.daj` → `Zlecenia.zebrano`, id bazowe, wyborne też). Test: `MCMMORPG/profesje/zielarz/testy/zadanie-startowe.js` (bot bez OP, PPM w kępę, LPM sierpem).
- Brak „niepewnej ręki”: jakość cięcia/wyciągania zależy tylko od ruchu gracza, nie od liczby wcześniejszych zbiorów (`s.wprawa` służy już tylko radom).

### Dane zielarza na dysku (od F1, 25.10)
Katalog `plugins/Rzemioslo/`:
- `zielarz/regiony/<swiat>/r.<x>.<z>.yml` — kępy postawione w świecie, region 512×512 bloków (`x = blokX >> 9`).
  Wszystkie wczytywane przy starcie (kępy stawia admin, jest ich mało).
- `zielarz/konta/<uuid-konta>.yml` — odrost kęp per KONTO (`kepy.<klucz>: "ziarno;odrostDo;rozpoznana;chciwie;slaba"`).
  Wczytywany w tle przy wejściu gracza, z pamięci znika po wyjściu. Brak trwałych wpisów = plik usunięty.
- `zielarz/swiat.yml` — partie (stan wspólny dla świata).
- `zielnik/<uuid-postaci>.yml` — zielnik i wiedza per POSTAĆ (profil MMOProfiles).
- `zielarz-suszarnie.yml` — suszarnie (zapis tylko przy zmianie).

Zasady zapisu:
- **Nic nie pisze na wątku serwera.** Wątek serwera składa tekst (migawka), `ZMagazyn` zapisuje go na wątku
  `Rzemioslo-zielarz-dysk`. Jeden wątek = zapisy i odczyty w kolejności zleceń (szybki relog widzi ostatni zapis).
- Zapis: `<plik>.tmp` → fsync → `ATOMIC_MOVE` na miejsce. Crash w trakcie zostawia stary plik, nie ucięty.
- Zbiór tylko oznacza konto/region jako „brudne”; zrzut zmienionych co 60 s (`runTaskTimer` 1200 t), przy wyjściu
  gracza (jego konto i zielnik) i przy wyłączaniu (`ZMagazyn.zamknij` czeka do 20 s na kolejkę).
  Po crashu (kill -9) można stracić najwyżej ostatnie 60 s odrostu.
- Zepsuty YAML (albo inny błąd odczytu) zostaje obok jako `<plik>.zepsuty` + SEVERE w logu (nie nadpisujemy go pustym);
  gracz zaczyna wtedy z pustym odrostem, zamiast wisieć na „Chwila…”. Pojedynczy zły wpis konta (np. ucięta liczba)
  jest pomijany z WARN `pomijam zly wpis kepy`, reszta konta się wczytuje (przy następnym zapisie wpis znika).
- Indeks chunków per konto (`"swiat;cx;cz" -> klucze kęp`) — wysłanie chunka graczowi sprawdza tylko kępy z tego chunka.
- Gracz klika kępę, zanim jego konto się wczyta (pierwsze ~ms po wejściu): „Chwila, rozglądasz się jeszcze.”

Przeniesienie ze starego układu (jednorazowo, przy starcie): `zielarz-stany.yml` → `zielarz/konta/` + `zielarz/swiat.yml`,
`zielarz-kepy.yml` → `zielarz/regiony/`; stare pliki zostają jako `*.przed-f1`, w logu linie `zielarz F1: …`.
Istniejących plików kont nie nadpisuje.

**Cofnięcie F1**: stary jar (sprzed gałęzi `zielarz-f1`) + `mv zielarz-stany.yml.przed-f1 zielarz-stany.yml`,
`mv zielarz-kepy.yml.przed-f1 zielarz-kepy.yml` (odrost zebrany po wdrożeniu F1 przepada, kępy postawione po F1 trzeba
postawić ponownie).

### Komendy zielarza (wybrane)
- `rzemioslo reload | info <gracz>` — config, stan profesji.
- `rzemioslo kepa-xyz <gatunek> <świat> <x> <y> <z>` / `kepa` / `suszarnia-xyz …` — stawianie kęp i suszarni.
- `rzemioslo odrost <gracz>` — odrost od razu (test).
- `rzemioslo magazyn [zapisz]` — konta w pamięci, brudne konta/regiony, licznik zapisów i błędów dysku; `zapisz` = zrzut teraz.
- `rzemioslo zielnik <gracz>`, `pomiar`, `kom-test`, `rady-reset` — diagnostyka zielarza.

Klon testowy zielarza: `MCMMORPG/profesje/zielarz/przygotuj_klon.sh <klon>`; projekt: `MCMMORPG/profesje/ZIELARZ.md`.

## Drwal — ścinka w świecie (gałąź `profesje-zbierackie`, 20.11, NIEWDROŻONY)
Klasa `Drwal.java` (opis w nagłówku klasy, linie 68-74). Gracz z siekierą w ręce (rodzaj z `profesje.woodworking.narzedzie`, `Dane.java:72`; poziom gatunku) w zasięgu 3,4 bloku od pnia drzewa w zarejestrowanym lesie (`las`/`drzewo`) ścina je w skali 1:1 (`rozpocznij`, `Drwal.java:409`):
1. **Wrąb** — seria LPM nisko z jednej strony (6–7 ciosów w rytmie ~7 ticków) wyznacza kierunek upadku. Za mało/za dużo ciosów albo rozrzut obniża jakość (`nagroda`, `Drwal.java:998`).
2. **Rzaz ścinający** — PPM trzymany z przeciwnej strony, nie niżej niż wrąb; pasek „trzeszczy” (postęp ~0,55–0,8). Puszczony w paśmie → drzewo pada tam, gdzie wrąb; za głęboko albo bez wrębu → „gdzie chce”, może pęknąć wzdłuż.
3. **Upadek** — pień, gałęzie i korona jako `BlockDisplay` z interpolacją, widoczne dla wszystkich w pobliżu; prawdziwe bloki ukryte pakietem u widzów na czas sceny. Przeszkoda w łuku upadku (`przeszkoda`, `Drwal.java:776`: sąsiednie drzewo, mur) → drzewo **zawisa** (faza `WISI`); klin (LPM w pniak od strony przeciwnej do upadku, `klin`, `Drwal.java:553`) ściąga je dalej. Każdy gracz w pasie upadku dostaje obrażenia `FALLING_BLOCK`: pień ≥14 albo 75% max HP, korona ≥5 albo 30% (`przygniec`, `Drwal.java:925`) — ścinający też (uwaga `przygnieciony`).
4. **Obróbka** — leżący pień: LPM w koronę = okrzesanie, LPM w granice kłód = cięcie na kłody (tolerancja `TRAF` 0,3 bloku; chybienie = drzazgi).
5. **Wynik** — klasa słabe/dobre/wyborne z jakości; wyborne daje `<przedmiot>_WYBORNE`. Liczba kłód = pocięte segmenty (−1 bez okrzesania), zawieszone = połowa, pęknięte = połowa, **spróchniały dąb (12%, raz na drzewo) = 0** (decyzja 20.11: „może dać 0”). XP profesji z MMOCore × jakość × długość.
6. **Świerk** — LPM wyżej na pniu przed ścinką = spała (nacięcie kory); po 5 min drewno smolne (+1 kłoda, +jakość).
7. **Odrost** — per konto: ścinający widzi pniak przez `odrostSek` (dąb 600 s, świerk 720 s), inni widzą drzewo (`pokazPniak`, `odrastaj`, `Drwal.java:1073-1097`).

Gatunki i przedmioty: `GATUNKI` (`Drwal.java:82-84`): dąb → `PROF_OAK_TIMBER`, świerk → `PROF_SPRUCE_RESIN`. Przedmioty `*_WYBORNE` są na razie tylko na klonie (`MCMMORPG/profesje/drwal/przedmioty-klon.yml`, dopisywane przez `przygotuj_klon.sh`); przed wdrożeniem trzeba je dodać do generatora przedmiotów (`tresc.py`/`generuj.py`) — nie zrobione.

**Lochy i wyspy:** mechanizm gotowy — w lochu `las loch_001_* x1 z1 x2 z2 trwale` (instancje MythicDungeons), pojedyncze drzewo `drzewo dodaj`. Sprawdzone 20.11 parserem regionów mapy `MythicDungeons/maps/loch_001`: na mapie **nie ma stojących drzew do ścinki** (pnie świerku to palisada, dąb/brzoza leżą poziomo, pale_oak to punkt orientacyjny „Blade Drzewo”). Las w lochu trzeba posadzić przy budowie mapy — decyzja właściciela/sesji lochów. Wysp gry jeszcze nie ma.

**Koszt (klon, 20.11, `MCMMORPG/profesje/drwal/testy/obciazenie.js`):** 1 bot ścina w pętli 90 s (5 drzew): MSPT 10 s avg 1,8 (max 5,6) wobec spoczynku 2,1; do 90 `block_display` naraz; obserwator ~166 pakietów/s (metadata 56/s). 4 boty naraz (19 drzew w 90 s): MSPT 10 s avg 2,5 (max 8,8) wobec 2,2; do 412 displayów naraz (~100 na drzewo); obserwator ~475 pakietów/s (metadata 211/s). Po jednym pomiarze na N, spark nie użyty. Wniosek: koszt serwera mały, główny koszt to pakiety do widzów (~100 displayów na upadające drzewo) — przy 20+ drwalach w jednym miejscu do zmierzenia ponownie. Na tik tylko aktywne sesje (1 zadanie/gracz) i interpolacja displayów po stronie klienta.

**Znane ograniczenie:** na czas sceny serwer trzyma bloki drzewa, a klienci widzą powietrze (kolizja niezgodna z obrazem, ~kilka sekund).

**Nie sprawdzone w źródłach** (projekt, nie fakt leśny): że drzewo pada w stronę wrębu, że rzaz idzie nad wrębem, że przecięcie zawiasy odbiera kontrolę, że spróchniały pień brzmi głucho, że spała na świerku daje drewno smolne.

**Testy:** `MCMMORPG/profesje/drwal/przygotuj_klon.sh <klon>` (jar, przedmioty, las testowy na płaskim świecie klonu), potem `run.sh <klon> MCMMORPG/profesje/drwal/testy/scinka.js` — 16 przypadków bez OP (bez siekiery, za wcześnie, czysto, widz przygnieciony, spróchniały, zawis + klin, bez wrębu, ścinający w pasie, spała + żywica). Wynik 20.11: 16/16 PASS.

## Rybak — scena łowienia (gałąź `profesje-zbierackie`, 20.11, NIEWDROŻONY)
Klasa `Rybak.java` (opis w nagłówku, linie 32-37). Zarzut wędką profesji (`Lowienie.java:77`) przejmuje scena; zwykła wędka łowi po staremu, bez XP. Vanilla nigdy nie bierze (`setWaitTime(72000)`), wszystko prowadzi tik sesji (1 zadanie na łowiącego). Bez paska postępu: odległość widać po spławiku, naprężenie słychać. Q nic nie robi.
1. **Czekanie** (`czeka`, `Rybak.java:151`): spławik siada w wodzie łowiska (tier jak dawniej, `tierWody`); drobnica skubie (fałszywe ruchy).
2. **Branie** (`branie`, `:175`): gatunek zdradza ruch spławika, zapowiedź ≥ 1 s (`:165`). Gatunki: ryba główna tieru (wzorzec z tabeli `RYBY`, `:50-61`), dar wody (spławik powoli tonie → przedmiot poboczny łowiska, perła z małża z szansą rzadką), okaz (5%, długa zapowiedź, wyskok, ×2 łup i XP), drobnica (zacięcie = nic). Zacięcie PPM w oknie ≥ 700 ms (`OKNO_MIN`, `:41`); za wcześnie albo za późno = ryba puszcza.
3. **Hol** (`hol`, `:272`): Shift zwija. Ryba zapowiada zryw w bok (≥ 1 s, komunikat z kierunkiem), potem rwie — kamera w przeciwną stronę = kontra. Kąt liczony względem osi holu bez bocznego odjazdu ryby (`katDoSplawika`, `:370`), więc stojąca kamera nie daje kontry (poprawka 20.11 po teście klientem QA). Żyłka trzeszczy przy dużym naprężeniu → puść Shift; długie trzeszczenie = zerwanie (0). Schowana wędka albo zwinięta żyłka w holu = ryba poszła (0).
4. **Wynik** (`nagroda`, `:456`): jakość q = 1 − późne zacięcie − brak kontry − trzeszczenie; q ≥ 0,85 = wyborne (`<ID>_WYBORNE`, gdy brak takiego przedmiotu — zwykły z ostrzeżeniem w logu), ≥ 0,5 dobre, niżej słabe. XP = `g(tier)·(0,4+0,6q)` (okaz ×2). Wyskok ryby (`wyskok`, `:381`) = `ItemDisplay` widoczny dla wszystkich.

Przedmioty `PROF_RAW_CATCH_WYBORNE`, `PROF_CLAM_WYBORNE` są tylko na klonie (`MCMMORPG/profesje/rybak/przedmioty-klon.yml`); przed wdrożeniem trzeba je dodać do `tresc.py` — nie zrobione (tak jak u drwala).

**Komenda testowa:** `rzemioslo rybak <gracz>` (stan sceny: faza, okno, naprężenie, kierunek, kąt, kontra, hak), `rzemioslo rybak <gracz> wymus <ryba|okaz|dar_wody|drobnica> <ticki>` (następne branie), `rzemioslo rybak pomiar on|off` (logi `[rybak] …`) — `Komenda.java` `case "rybak"`, `Rybak.java:545,555`.

**Lochy i wyspy:** łowisko w lochu = znacznik `lowisko_tN` przy wodzie instancji. Sprawdzone 20.11 parserem `MCMMORPG/profesje/rybak/woda_lochow.py` (tylko odczyt map MythicDungeons world1): `loch_001` ma 0 bloków wody, pozostałe 5 map ma pusty region (teren z generatora Iris — wody z pliku nie policzysz). Wodę trzeba zbudować przy budowie map; parser wypisze wtedy komendy znaczników. Wysp gry jeszcze nie ma.

**Koszt (klon, 20.11, `MCMMORPG/profesje/rybak/testy/obciazenie.js`, 90 s, wymuszane okaz/ryba = najwięcej wyskoków):** 1 bot: MSPT 10 s avg 1,7 (max 3,7) wobec spoczynku 1,6; obserwator 58 pakietów/s; do 1 `item_display`. 4 boty: MSPT 10 s avg 2,1 (max 6,6) wobec 2,1; obserwator 129 pakietów/s (cząsteczki 32/s, velocity 16/s); do 4 `item_display`. Po jednym pomiarze na N, spark nie użyty. Wniosek: koszt mały (drwal przy 4 graczach ~475 pakietów/s).

**Nie sprawdzone w źródłach** (projekt, nie fakt wędkarski): kontra kamerą w stronę przeciwną do zrywu, „trzeszczenie” żyłki jako sygnał popuszczenia, rozpoznawanie gatunku po ruchu spławika.

**Testy:** `MCMMORPG/profesje/rybak/przygotuj_klon.sh <klon> [--bez-terenu]` (jar, przedmioty, stawy T1/T2 na płaskim świecie klonu), potem `run.sh <klon> MCMMORPG/profesje/rybak/testy/lowienie.js [przypadki]` — bot bez OP: vanilla, za wcześnie, drobnica, spóźnienie, czysty hol, zerwanie, bez kontry, bierny (kamera stoi), dar wody, okaz, słaba wędka, schowana wędka. Wynik 20.11: 18/18 PASS w dwóch przebiegach po poprawce kontry (wcześniej 17/17 bez przypadku „bierny”). Klient QA (20.11): hol samym Shiftem bez ruchu kamery → kontra 0/41, żyłka zerwana; pasek akcji z komunikatami holu widoczny na nagraniu.

## Myśliwy — łowy w świecie (gałąź `profesje-zbierackie`, 20.11, NIEWDROŻONY)
Klasa `Mysliwy.java` (opis w nagłówku, linie 52-54); zwłoki i pasek oprawiania zostają w `Oprawianie.java`. Gracz z nożem profesji (`PROF_NOZ_1`, rodzaj z `profesje.hunting.narzedzie`) kuca przy **rewirze** (znak: stare odciski łap, `znakiRewirow`):
1. **Tropy** (`naKucniecie` `Mysliwy.java:216`, `tikLowu` `:293`): 4 ślady po ~5,5 bloku w terenie (cząsteczki widoczne dla wszystkich); stanąć przy kolejnym = następny. Wiatr losowy co 45° (`wiatrDeg`), może skręcić w trakcie (pasek „Wiatr skręca”). Czas łowów 150 s (`CZAS_LOWOW`).
2. **Podchód** — za ostatnim tropem, 17 bloków dalej (`ODLEGLOSC_ZWIERZA`), pojawia się prawdziwy mob MythicMobs z rewiru (oswojenie 30 ticków). Płoszy go (`:339-345`): wiatr niosący zapach w jego stronę (< 13 bl.), kroki — sprint albo szybki ruch (< 10 bl.), wyprostowana sylwetka (< 6,5 bl.). Spłoszone ucieka (`sploszenie` `:370`), łów przepada.
3. **Cios** z podchodu (`naCios` `:385`) — czysty: zwierzę pada w następnym ticku (`setHealth(0)`, bo MythicMobs zmniejsza obrażenia po `setDamage`). Ranne w ucieczce = łów przegrany.
4. **Oprawianie** (`klikZwloki` `:506`, `tikSceny` `:541`) — na zwłokach czerwone znaki cięć (`DUZA`/`MALA`/`PAJAK`, `:95-105`): dzik, owca, wilk, niedźwiedź: odbyt → brzuch → przełyk → przepona → skóra (kolejność wg MSU „Field Care of Harvested Big Game”); zając i pająk: kolejność reguły gry. PPM w znak (trafienie promieniem wzroku: `chybienie`, próg `CEL` 0,2 bl.) i prowadzenie noża 14 ticków; zjechanie wzrokiem > 0,34 bl. = poślizg (kara jakości skóry albo mięsa). Zła kolejność psuje treść (przebity żołądek → „treść na mięsie”). Obrażenia albo odejście > 3,5 bl. od zwłok przerywa scenę, zrobione cięcia zostają.
5. **Wynik** (`nagroda` `:647`): skóra (`glowny` z `zwierzyna:`, np. `PROF_BEAST_SCRAP`, `PROF_RAW_FLEECE`, `PROF_VENOM_SAC`) i dziczyzna `PROF_DZICZYZNA` (zając 1, reszta 2) — każda w klasie słabe/dobre/wyborne z jakości (`klasa` `:645`), poniżej 0,25 = **nic** (decyzja 20.11 „może dać 0”). Zwierzę płonące w chwili śmierci albo zabite ogniem = skóra spalona, 0 skóry (`Oprawianie.java:102`). Wyborne = `<id>_WYBORNE`.
6. **Wędzenie** (`naOgnisko` `:700`, `probkujWedzarnie` `:763`) — PPM dziczyzną w zapalone ognisko (do 4 kawałków, widoczne `ItemDisplay`), po 90 s (`WEDZENIE_MS`, wartość testowa) PPM odbiera `PROF_SUSZONA_DZICZYZNA` (jadalna, typ CONSUMABLE). Ogień próbkowany co 5 s; zgaszony > 30% próbek = mięso nie doschło. Stan wędzarni przeżywa restart (`mysliwy.yml`).
7. **Podpowiedzi** tylko wykonawcy, dopóki wprawa < 5 (flaga `mysliwy_wprawa`); potem sam obraz i pasek.

Przedmioty `PROF_DZICZYZNA(_WYBORNE)`, `PROF_SUSZONA_DZICZYZNA(_WYBORNE)`, `PROF_BEAST_SCRAP_WYBORNE`, `PROF_RAW_FLEECE_WYBORNE`, `PROF_VENOM_SAC_WYBORNE` są tylko na klonie (`MCMMORPG/profesje/mysliwy/przedmioty-klon*.yml`); przed wdrożeniem trzeba je dodać do `tresc.py` — nie zrobione (jak drwal, rybak). `Przedmioty.java` szuka przedmiotu bez wpisu w `typy` także w typie CONSUMABLE.

**Dane:** `plugins/Rzemioslo/mysliwy.yml` — `rewiry` (`swiat:x:y:z:MOB`, tylko trwałe) i `wedzarnie` (kawałki nad ogniskami).

**Komendy:** `rzemioslo rewir <swiat|loch_001_*> <x> <y> <z> <MOB z listy zwierzyna> [trwale]`, `rzemioslo rewir-usun <swiat> <x> <y> <z>`, `rzemioslo mysliwy <gracz> [wiatr <stopnie>|wedzenie-gotowe]` (stan łowu/sceny/wędzarni), `rzemioslo mysliwy pomiar on|off` (logi `[mysliwy] …`) — `Komenda.java` `case "rewir"`, `"rewir-usun"`, `"mysliwy"`.

**Lochy i wyspy:** rewir w lochu = `rewir loch_001_* x y z MOB trwale` (dopasowanie instancji MythicDungeons jak `las`/`lowisko`). Sprawdzone 20.11 parserem `MCMMORPG/profesje/mysliwy/rewiry_lochow.py` (tylko odczyt map world1): na `loch_001` wokół punktów walk z wilkami jest prosta chodliwa linia ≥ 65 bloków (starcie_A 24,103,98: 80 bl. −Z; populacja 0,102,127: 65 bl.; zasadzka_C 51,102,63: 74 bl.) — miejsca na rewiry: `rzemioslo rewir loch_001* 24 103 98 BEAST_DIRE_WOLF trwale` itd. (nie wpisane). Pozostałe 5 map: pusty region (Iris). **Ograniczenie:** tropy idą po prostej i szukają gruntu ±4 bloki (`ziemia`), nie sprawdzają ścian — w ciasnym lochu przejdą przez mur; wysp gry jeszcze nie ma.

**Koszt (klon, 20.11, `MCMMORPG/profesje/mysliwy/testy/obciazenie.js`, 90 s łowów po 30 s spoczynku, pętla: tropy → cios → 5 cięć):** 1 bot (7 łowów, 5 oprawionych): MSPT 10 s avg 1,9 (max 6,1) wobec spoczynku 1,6; obserwator 126 pakietów/s (rel_entity_move 18/s, cząsteczki 16/s, velocity 11/s); naraz do 2 `interaction`, 3 `item_display`, 13 świń w promieniu 120 (12 to świnie płaskiego świata klonu). 4 boty (28 łowów, 21 oprawionych): MSPT 10 s avg 3,0 (max 12,6) wobec 2,5 (max 12,4); obserwator 187 pakietów/s (rel_entity_move 28/s, velocity 25/s, cząsteczki 23/s); do 7 `interaction`, 8 `item_display`, 16 świń. Po jednym pomiarze na N, spark nie użyty. Wniosek: koszt mały, mniejszy niż drwal (~475 pakietów/s przy 4).

**Nie sprawdzone w źródłach** (reguły gry): kolejność cięć zająca i pająka, ogień niszczący skórę, czas wędzenia.

**Testy:** `MCMMORPG/profesje/mysliwy/przygotuj_klon.sh <klon>` (jar, przedmioty, ogniska na płaskim świecie klonu; rewiry stawia test), potem `run.sh <klon> MCMMORPG/profesje/mysliwy/testy/lowy.js [przypadki]` — bot bez OP: bez noża, zgubiony trop, wiatr, sprint, wzrok (bez kucania), ranne ucieka, czysto (wyborne), zła kolejność, obrażenia w trakcie + dokończenie, spalona skóra + wynik 0, zając, wędzenie, zgaszone ognisko. Wynik 20.11: 21/21 PASS w trzech kolejnych przebiegach (trzeci po skróceniu podpowiedzi podchodu) (wcześniejsze przebiegi padały na celowaniu bota, nie na pluginie — pułapki niżej). Klient QA (20.11, konto bez OP, kadry `/opt/docker/mc/backups/mysliwy-kadry-20261120/`): kucnięcie przy rewirze, trop widoczny (przy cząsteczkach „minimalne” — domyślne `klient.sh` — tropu NIE widać), podchód, czysty cios nożem, 5 cięć pierwszym klikiem (wybór cięcia chybienie 0,00), wynik „1× Łup z bestii (wyborne), 2× Dziczyzna (wyborne)”, mięso nad ogniskiem, odbiór suszonej dziczyzny; spłoszenia „Usłyszało” i „Zobaczyło” też wyszły w kliencie.

## Znane pułapki i decyzje
- Profesje v2 — stan, liczby, wdrożenie 12.10 i cofnięcie: `~/.claude/skills/mc-systemy-gracza/profesje.md:8-13`.
- Brak `set_player_inventory` w mineflayer, UI Księgi blokuje equip → `rzemioslo doreki`: `~/.claude/skills/mc-systemy-gracza/SKILL.md:36`, `historia.md:30`.
- Drwal, test botem: `bot.lookAt(p, true)` w mineflayer 4.39 nie zmienia samego pitchu na serwerze → `lookAt(p, false)` + ~160 ms (`~/.claude/skills/mc-host-testy-wdrozenia/SKILL.md`, pułapka 13).
- Rybak (Paper 1.21.11, sprawdzone na klonie 20.11): `Sound.ENTITY_LEASH_KNOT_BREAK` nie istnieje (błąd kompilacji) → `ENTITY_ITEM_BREAK`; anulowany `PlayerFishEvent` w stanie REEL_IN zostawia hak w wodzie (na tym stoi hol); `FishHook.setWaitTime(72000)` blokuje branie vanilla; bot mineflayer nie zmienia slotu paska (`~/.claude/skills/mc-host-testy-wdrozenia/testy-narzedzia.md`).
- Myśliwy, test botem (20.11): `bot.activateEntity` w mineflayer robi najpierw `lookAt(encja + 1)` (`node_modules/mineflayer/lib/plugins/inventory.js:250`) i psuje celowanie → pakiet `use_entity` ręcznie (`profesje/mysliwy/testy/lowy.js` `uzyj`); MythicMobs zmniejsza obrażenia po `setDamage` → czysty cios kończy `setHealth(0)` w następnym ticku; `MythicMobDeathEvent` nie zawsze podaje zabójcę → mapa `lowcaZwierza`.
- Generatory/Nexo (`model:`, STRINGBLOCK, `RZM_*` z `{base:0}`): `~/.claude/skills/mc-systemy-gracza/SKILL.md:39`.
- Krzywa XP profesji = plik `exp-curves/profesje_v2.txt`: `~/.claude/skills/mc-mmo-pluginy/SKILL.md:22`.
- Zasady zawodów (czynności w świecie, testy bez OP): `~/.claude/skills/mc-systemy-gracza/SKILL.md:46`.
- Zmiana treści: edytuj `MCMMORPG/profesje/tresc.py` → `gen_klon.sh` → `audyt.py` → `eval/run_all.sh` → `wdroz.sh` (`profesje.md:12`); ręczna edycja `config.yml` zostanie nadpisana (poza `komunikaty`).

## Budowanie i wdrożenie
`./build.sh` (Gradle 8.12/JDK 21 w kontenerze `build-rzemioslo-<pid>`, `--memory 1536m`, jary world1 montowane read-only, nazwa jaru `rzemioslo-2.0.0-<RRRRMMDDGGMM>.jar`, `build.sh`, `build.gradle.kts:14-23`). Przypięte zależności: MMOCore 1.13.1, MythicLib 1.7.1, MMOItems 6.10.1, MythicMobs 5.11.2, nexo 1.20, packetevents 2.11.2, PlaceholderAPI 2.12.2, Vault 1.7.4. Testów jednostkowych nie ma (`src/` zawiera tylko `main`). Wdrożenie przez `MCMMORPG/profesje/wdroz.sh` (generator na zatrzymanym world1), patrz `profesje.md:12`.

## Stan po poprawkach (25.10)
- `Zlecenia.oddaj` oznaczało oddane `wybrane.get(n-1)`, a lista pokazana graczowi pomija indeksy spoza puli (`dzisiejsze`); `Pozycja` niesie teraz oryginalny indeks (`idx`, `Zlecenia.java:110,139,155`), więc oddane jest zawsze to zlecenie, które gracz widzi.
- `Placeholdery.java:47`: tier spoza `tiery` daje pusty opis zamiast NPE.
- Zapis `dane/<UUID>.yml`: zrzut `saveToString` na głównym wątku, plik pisze jednowątkowy executor (`Zlecenia.java:70-104`); `oczekujace` chroni przed odczytem starego pliku po relogu; `onDisable` dokańcza kolejkę i zapisuje synchronicznie (`Rzemioslo.java:105`). Format pliku bez zmian.
- `Lowienie.podpowiedz` czyszczone przy wyjściu gracza (`Lowienie.java:31`).
- Klucze configu niewidoczne dla kodu pluginu (`wersja`, `haki-swiata`, `xp-receptur`, `perki-progi`, `relikty`, `profesje.*.zbieracka`, `wezly.*.{nowy,typ_bloku}`, `lowiska.*.blok`) NIE są martwe: plik jest generowany przez `MCMMORPG/profesje/generuj.py` (`:378-386,525-556`), a `audyt.py` czyta `haki-swiata`, `xp-receptur`, `perki-progi`, `relikty` (`:76,143,208,250`). Plugin nie ma źródłowego `config.yml` w `src/main/resources` (jedyne źródło prawdy: generator + world1), więc nie ma czego usuwać w katalogu pluginu. `narzedzia.*.wytrzymalosc` jest czytane (`Dane.java:75`) i używane przez generator (`generuj.py:119`).
