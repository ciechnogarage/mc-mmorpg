# Rzemioslo (`plugin-src/rzemioslo`)
Wersja w źródle: `2.0.0-<rev>` (`gradle.properties:2` + `build.gradle.kts` `version = rzmVersion-rzmRev`; `plugin.yml` ma `${version}`). Jar na world1: `/opt/docker/mc/data/world1/plugins/rzemioslo-2.0.0-202610121414.jar`. Zgodność źródła z jarem: zgodna co do `plugin.yml` (nazwa, depend, softdepend, komenda, uprawnienie), listy klas (36 klas = klasy ze źródła) i zawartości `Komenda.class` (są literały `doreki`, `oboz`, `postaw`) oraz `Zbieranie.class` (metoda `wyslijNarzedzie`, z ostatniego commita 20d5ffe). Pełnej równości bajtkodu nie sprawdzałem (nie buduję).
Opis stanu z dnia: 2026-10-25, commit 8c79ff8 (gałąź `docs-pluginy`). Ostatni commit kodu pluginu: 20d5ffe (2026-10-12 14:43), wdrożony 12.10 14:45.

## Po co (co widzi gracz)
Profesje v2: 6 profesji zbierackich (górnictwo, drwalstwo, zielarstwo, farmerstwo, rybactwo, myślistwo) działa „z narzędziem”: PPM w węzeł (blok Nexo), zarzucenie wędki na łowisku, oprawienie zwłok zabitej bestii nożem. Narzędzie wystarczy mieć w plecaku (w dłoni pokazuje się tylko pakietem). Do tego buffy z alchemii/gotowania/krawiectwa (statystyki MythicLib), obozowisko, XP za wytwarzanie na stacjach MMOItems, perki co 10 poziomów oraz zlecenia dnia i zlecenia startowe. Pozostałe 7 profesji (kowalstwo, krawiectwo, jubilerstwo, alchemia, zaklinanie, gotowanie, budownictwo) dostaje XP tylko ze stacji i zleceń (lista w configu world1, klucz `profesje`).
Zielarz „w świecie” (kępy, minigry) NIE jest w tym źródle ani w jarze (brak `Zielarz*.java`) — wg skilla to osobna gałąź niewdrożona: `~/.claude/skills/mc-systemy-gracza/profesje.md:36,49`.

## Komendy i uprawnienia
Jedna komenda `/rzemioslo` (`src/main/resources/plugin.yml:8-12`), uprawnienie `rzemioslo.admin`, `default: op` (`plugin.yml:13-16`). Menu (DeluxeMenus) wołają ją z konsoli (`Komenda.java:15`). Podkomend w kodzie: 10 (policzone parserem po `case`), gracza wskazuje się nazwą (`Bukkit.getPlayerExact`), nieobecny gracz albo zły nick = komunikat „[Rzemioslo] gracz nie jest online: <nick>” dla nadawcy (`Komenda.java:149-153`).

| Podkomenda | Kto | Co robi | Źródło |
|---|---|---|---|
| `reload` | OP/konsola | `reloadConfig()` + nowy `Dane`, wypisuje liczby i `problemy` | `Komenda.java:25-29`, `Rzemioslo.java:109-115` |
| `info <gracz>` | OP/konsola | poziomy wszystkich profesji i aktywne buffy | `Komenda.java:65-78` |
| `zlecenie oddaj <gracz> <prof> <n>` | konsola (menu) | oddaje n-te zlecenie dnia (zabiera przedmioty, XP, pieniądze) | `Komenda.java:30-36`, `Zlecenia.java:143-163` |
| `start <gracz> <prof>` | konsola (menu) | zlecenie startowe: przyjmij → daje narzędzie T1; ponownie po zebraniu → nagroda XP | `Komenda.java:37-42`, `Zlecenia.java:170-197` |
| `przeprawa <gracz> <ID> <komenda…>` | konsola/OP | zabiera 1 zestaw przeprawy (`uzycia.<ID>: przeprawa`) i wykonuje `<komenda>` z konsoli (`{gracz}` → nick) | `Komenda.java:43-56` |
| `narzedzie <gracz> <rodzaj> <tier>` | OP/konsola | daje `PROF_<RODZAJ>_<tier>` | `Komenda.java:57-64` |
| `doreki <gracz> <ID> [slot]` | OP/test | zamienia przedmiot na pasek (domyślnie slot 5) i ustawia jako trzymany; test/boty; slot 0-8, zły slot/nie-liczba = komunikat | `Komenda.java:79-102` |
| `oboz <gracz> <ID>` | OP/test | rozbija obozowisko pod graczem bez PPM | `Komenda.java:103-110` |
| `postaw <nexoId> <świat> <x> <y> <z>` | OP/konsola | stawia węzeł/łowisko (blok Nexo) — API dla lochów/wysp (MythicMobs); nic nie robi, gdy już stoi; odmawia, gdy blok zajęty | `Komenda.java:111-129` |
| `wezel <x> <y> <z> [świat]` | OP/konsola | diagnostyka: co to za blok, jaki węzeł i tier | `Komenda.java:130-143` |

Pomoc (`Komenda.java:155-159`), `usage` (`plugin.yml:11`) i tab-complete (`Komenda.java:165-190`) obejmują wszystkie 10 podkomend; tab-complete podpowiada też graczy online, profesje, rodzaje narzędzi, obozy, światy i id węzłów, tylko dla `rzemioslo.admin`. Błędna liczba w `zlecenie oddaj`/`doreki` albo nieznany świat w `wezel` = komunikat dla nadawcy, bez wyjątku.

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
| `uzycia` | 60 id → rodzaj użycia; kod czyta tylko wartość `przeprawa` (30 szt.); `naprawa` (10), `zwoj` (10), `obozowisko` (10) nie są czytane przez ten plugin | 60 | `Dane.java:93-94`, `Komenda.java:48` |
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
- **Integracje**: MMOCore (profesje, party), MythicLib (staty, action bar), MMOItems (przedmioty, stacje), Nexo (bloki; kontrola po `NexoItemsLoadedEvent`, `Rzemioslo.java:137-140`), MythicMobs (śmierć moba), PacketEvents, PlaceholderAPI (`%rzemioslo_...%`, `Placeholdery.java:10-16`: `zlecenie_<prof>_<n>_{tytul,opis,masz,ilosc,xp,kasa,akcja,id,typ}`, `start_<prof>_{opis,postep,akcja}`, `buff_<grupa>`, `tier_<prof>`), Vault (wypłata za zlecenia, `Zlecenia.java:159-160,211-215`), Mistrzostwa (soft).
- **Komunikaty** idą na action bar z priorytetem HIGH przez MythicLib (`Rzemioslo.java:148-154`).

## Koszt dla serwera
Nie mierzone (brak spark/MSPT w repo). Z kodu: tykają tylko aktywne sesje zbierania (1 zadanie/gracz co tick, pakiet narzędzia + `sendBlockDamage` do graczy w 32 blokach, `Zbieranie.java:195-223`) i obozy (co 2 s, pętla po graczach świata, `Buffy.java:252-270`); reszta na zdarzenia. `tierWody` przeszukuje 2197 bloków na złowioną rybę (`Lowienie.java:43-52`). Zapis zleceń (przy każdym `zebrano()` w aktywnym zleceniu startowym, przyjęciu i oddaniu, `Zlecenia.java:132,157,179,191,206`): na głównym wątku tylko `saveToString`, plik pisze osobny jednowątkowy executor (`Zlecenia.java:70-92`).

## Dane
- Zlecenia: `plugins/Rzemioslo/dane/<UUID>.yml` (klucze `dzien`, `zlecenia.<prof>.{tier,wybrane,oddane}`, `start.<prof>.{stan,postep}`), per **postać**: UUID profilu MMOProfiles (`MMOPlayerData.getProfileId()`), a bez profilu UUID gracza (`Zlecenia.java:45-51`). Katalog `dane/` na world1 był pusty 2026-10-25.
- Stan w pamięci, tracony przy restarcie/przeładowaniu: aktywne sesje, wyczerpane węzły (per **UUID gracza** — przy MMOProfiles wspólny dla wszystkich postaci konta), cooldowny i aktywne buffy, obozy, zwłoki (`Zbieranie.java:77-80`, `Buffy.java:55-60`, `Oprawianie.java:61`).
- XP/poziomy profesji trzyma MMOCore, nie ten plugin.

## Znane pułapki i decyzje
- Profesje v2 — stan, liczby, wdrożenie 12.10 i cofnięcie: `~/.claude/skills/mc-systemy-gracza/profesje.md:8-13`.
- Brak `set_player_inventory` w mineflayer, UI Księgi blokuje equip → `rzemioslo doreki`: `~/.claude/skills/mc-systemy-gracza/SKILL.md:36`, `historia.md:30`.
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
