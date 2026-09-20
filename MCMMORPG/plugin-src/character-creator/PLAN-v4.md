# Kreator postaci — plan v4 (po recenzji krytycznej 2026-09-18)

> **STATUS 2026-09-18: Etapy 0-5 WYKONANE i wdrozone na world1** jako
> `character-creator 0.2.0-f0f9526.dirty`. Szczegoly zmiana po zmianie: README.md, sekcja
> "PLAN-v4 — naprawa dzialania". Zostaje **Etap 6** — szesc rzeczy, ktorych nie da sie
> rozstrzygnac bez zywego klienta i drugiego konta; jedna z nich (punkt 4, klasa Maga)
> jest juz rozstrzygnieta, patrz adnotacja przy niej.
>
> Stan produkcyjny po wdrozeniu: `characters.yml` wyczyszczony (18 rekordow testowych,
> w tym 9 sierot — usuniete przez nowe `/cc prune confirm` i `/cc delete`, kopia w
> `plugins/CharacterCreator/backups/predeploy-20260918-144849/`). Walidacja przy starcie
> zglasza trzy rasy bez modelu jako INFO, zero ostrzezen o brakujacych blueprintach
> w petli.
>
> **Jedna rzecz z Etapu 5 jest zrobiona tylko polowicznie:** wersja jara pochodzi z gita,
> ale katalog `plugin-src/character-creator/` nie jest w repo zacommitowany, wiec build
> dokleja `.dirty`, a sam skrot commita nie identyfikuje zrodel. Domkniecie tego to
> zacommitowanie modulu — swiadoma decyzja, nie robota techniczna.

PLAN-v3 dotyczyl tego, jak kreator WYGLADA i jak sie CZUJE. Ten plan dotyczy tego,
czy on DZIALA. Recenzja objela 12 obszarow funkcjonalnych; ocena ogolna 3/10,
werdykt: nie nadaje sie na publiczny serwer.

Wniosek przewodni: **80% wysilku poszlo w 20% wartosci.** Warstwa prezentacji
(glify, pomiar advance sonda na zywym kliencie, generatory) jest zrobiona rzetelnie
i zostaje. Rdzen — "stworz postac i wyjdz do swiata" — kasuje graczom plecaki przy
pierwszym restarcie serwera.

Ten plan NIE jest lista 60 usterek. Problemy sa systemowe, wiec pogrupowane sa wg
PRZYCZYNY, tak jak w v3.

## Dowody (stan wyjsciowy, zmierzony — nie hipotezy)

- **9 z 18 rekordow** w `plugins/CharacterCreator/characters.yml` to sieroty: zaden
  z 134 plikow `MMOProfiles/userdata/*.json` nie zawiera ich ID. Postac zapisana,
  profil nie powstal. **50% wskaznik porazki commitu na produkcji.**
- **18 rekordow, zero prawdziwych graczy** — 12x "Grimtest", 2x "Guitest", reszta
  testowa. To jest produkcyjny plik danych.
- **177 ostrzezen** `No ModelEngine blueprint found for model-id 'race_czlowiek'`
  w logu. W `plugins/ModelEngine/blueprints/` jest **1 z 4** blueprintow.
- **2 graczy** (`Miau`, `CCFull659483`) trafilo w logu na softlock odmowy commitu.
- **0 testow** w calym module. **0** wywolan `hasPermission()`. **0** blokow
  `try/catch` w warstwie profilowej.

---

## Etap 0 — ZATRZYMAC KRWAWIENIE (2-3 h) — BLOKUJACE — WYKONANE 2026-09-18

Najmniejszy zestaw zmian, po ktorym kreator przestaje trwale niszczyc rzeczy gracza.
Nic z tego nie jest refaktorem — to sa latki na sciezkach, ktore dzis gubia dane.

- **`onDisable()` musi sprzatac sesje.** `CharacterCreatorPlugin.java:73-81` robi
  dzis tylko `rotationTask.cancel()` + `characterStore.save()`. Ma iterowac po
  `sessions.activeSessions()` i wolac `end(player)` dla kazdej. Bez tego `/stop`
  z graczem w kreatorze = plecak przepadl (stash siedzi wylacznie w RAM,
  `CreatorSessionManager.java:55-56`).
- **Blokada podwojnego commitu.** `tryConfirm` (`CreatorRoomListener.java:236-248`)
  woła `celebrate()`, ktore zamyka GUI, ale konczy sesje dopiero po **45 tickach**.
  W tym czasie `reopenLostMenus()` (co 10 tickow, `CreatorSessionManager.java:93-104`)
  przywraca klikalny przycisk "STWORZ POSTAC". Drugie klikniecie = drugi
  `event.validate(this)` na tym samym evencie, drugi teleport, 6 komend konsolowych
  jeszcze raz. Fix: flaga `session.confirming`, ustawiana na wejsciu do `celebrate()`,
  sprawdzana w `tryConfirm` ORAZ pomijana przez `reopenLostMenus`.
- **Odwrocic kolejnosc commitu.** `CharacterProfileModule.onConfirm():99-120` robi
  `store.put()` (z natychmiastowym zapisem na dysk) PRZED `pendingCreation().complete()`.
  Kazda porazka po pierwszym kroku zostawia rekord bez profilu — to jest fabryka tych
  9 sierot. Nowa kolejnosc: `validate()` → `applyToRealPlayer()` → `store.put()` →
  `teleport()`.
- **`start()` musi odrzucic druga sesje.** Dzis brak sprawdzenia `isActive()`; drugie
  wejscie zapisuje do stashu **juz wyczyszczony** ekwipunek, wiec `end()` przywraca
  pustke. Trwala utrata itemow + wyciek starego dummy'ego.
- **Sciezka odmowy nie moze byc softlockiem.** `confirmNewCharacter` zwraca `null`
  (`ConfirmService.java:42-46`), ale `celebrate` konczy sesje PRZED wywolaniem
  handlera, a `onConfirm` robi `return` przed teleportem. Gracz zostaje w
  `creator_limbo` bez GUI, w SURVIVAL, z profilem pending. Jedyne wyjscie: relog.
  Fix: sciezka bledu musi teleportowac i komunikowac, tak samo jak sukces.
- **`try/catch` wokol calego `onCreate`,** z `validate()` w `finally`. Dzis wyjatek
  (np. `IllegalStateException` z `CreatorSessionManager.java:31-33`) leci przez event
  bus MMOProfiles i profil zostaje pending **na zawsze**.

**Weryfikacja:** (1) wejsc w kreator z pelnym plecakiem, zrobic `/stop`, wstac —
ekwipunek na miejscu; (2) kliknac "STWORZ" dwa razy w ciagu 2 s — dokladnie jeden
rekord, jeden teleport, jedna wiadomosc; (3) celowo zepsuc `mmocore-id` w configu,
przejsc sciezke — gracz ląduje na spawnie z czytelnym bledem, nie w limbo.

---

## Etap 1 — STAN SESJI PRZEZYWA RESTART (0.5 d) — WYKONANE 2026-09-18

Przyczyna: stan wejscia do kreatora (ekwipunek, gamemode, lokalizacja) nie istnieje
nigdzie poza pamiecia procesu, a mapa sesji jest kluczowana obiektem `Player`.

- **Klucz `UUID`, nie `Player`** (`CreatorSessionManager.java:15`). Dzis mapa trzyma
  silne referencje do `CraftPlayer`; po reconnekcie watchdog iteruje po martwych
  obiektach, a dummy nigdy nie znika.
- **Stan wejscia na dysk** — ekwipunek, oryginalny `GameMode` i lokalizacja do PDC
  gracza albo osobnego pliku, kasowane dopiero po udanym wyjsciu z kreatora.
- **Odtworzenie przy `PlayerJoinEvent`** — gracz, ktory wypadl w trakcie, wraca do
  kreatora (albo dostaje z powrotem swoje rzeczy, jesli profil juz istnieje).
- **Zapamietac i przywrocic oryginalny GameMode.** `end():81-83` robi dzis
  `if (SPECTATOR) setGameMode(SURVIVAL)` — admin wchodzacy z CREATIVE wychodzi
  w SURVIVAL.
- **`EventPriority.LOWEST` na `onQuit`** (`CreatorRoomListener.java:62` nie ma dzis
  priorytetu). MMOProfiles ma `synced-data.inventory: true` i `game-mode: true`;
  kolejnosc handlerow `PlayerQuitEvent` jest niedeterministyczna, wiec dzis mozliwe
  jest zapisanie do profilu SPECTATOR + pustego ekwipunku na stale.
- Usunac martwa linie `player.setGravity(true)` (`end():84`) — `start()` nigdy
  grawitacji nie wylacza.

**Weryfikacja:** kill -9 serwera z trzema graczami w kreatorze na roznych etapach;
po restarcie kazdy z nich ma swoje rzeczy i spojny stan. Zaden dummy nie zostaje.

---

## Etap 2 — IZOLACJA ZAMIAST PRZYKRYCIA GUI (0.5 d) — WYKONANE 2026-09-18

Przyczyna: w calym `CreatorRoomListener` sa **trzy** `@EventHandler`: quit, click,
close. Ochrona kreatora opiera sie wylacznie na tym, ze otwarte inventory blokuje
ruch klienta. To nie jest izolacja, to zaslona.

- **`PlayerTeleportEvent`** — cancel, gdy sesja aktywna a cel jest poza
  `creator_limbo`. Dzis kazde `/tp`, `/tpahere`, `/back`, respawn MMOCore czy kolejka
  MythicDungeons wyrywa gracza z sali, a sesja zyje dalej.
- **Watchdog musi sprawdzac swiat.** `enforceSpectator()` i `reopenLostMenus()` nie
  patrza ani na swiat, ani na pozycje. Gracz wyrwany z sali dostaje w normalnym
  swiecie: wymuszony SPECTATOR co 10 tickow, otwierane menu co 10 tickow i czyszczony
  plecak co 10 tickow (`clearRealInventories():144-152`), plus 5x PAPER wstrzykniete
  do prawdziwego ekwipunku (`CreatorMenu.java:241-247`). Rozjazd swiata musi konczyc
  sesje, nie eskalowac.
- **`PlayerCommandPreprocessEvent`** — whitelist pusta na czas kreatora.
- **`AsyncChatEvent`** — cancel. PLAN-v3 Etap 1 oznacza "wyciszyc czat" jako ZROBIONE;
  w kodzie **nie ma zadnego listenera czatu**. Usunieto tylko wlasna wiadomosc
  (`CharacterProfileModule.java:92-95`). Czat innych graczy dalej pisze postaci po torsie.
- **Watchdog nie moze walczyc z cudzym GUI.** `reopenLostMenus` nie ma throttlingu ani
  rozroznienia "nasze okno vs cudze" — jesli DeluxeMenus/MythicLib/CMI otworzy swoje
  okno, nadpisujemy je co 0.5 s w nieskonczonosc.
- **`depend: [Multiverse-Core]` + walidacja swiata.** `CreatorConfig.readLocation()`
  (`:109-118`) rozwiazuje `World` raz przy enable i nigdy nie sprawdza `null`.
  Dzis dziala wylacznie dlatego, ze w logu Multiverse wstaje 14:57:29, a my 14:57:59 —
  to zbieg okolicznosci, nie kontrakt. `/mv unload creator_limbo` = NPE przy kazdym
  teleporcie. Brak swiata musi dawac `setEnabled(false)` z czytelnym bledem.

**Weryfikacja:** z drugiego konta probowac wyciagnac gracza z kreatora kazda dostepna
droga (tp, tpahere, tphere, back, spawn, home, warp, dungeon queue). Zadna nie moze
zostawic gracza w stanie posrednim.

---

## Etap 3 — DANE, KTORE PRZEZYWAJA AWARIE (0.5 d) — WYKONANE 2026-09-18

Przyczyna: `CharacterStore` traktuje YAML jak pamiec podreczna, nie jak baze.

- **Zapis atomowy.** `save():71-87` robi `yaml.save(file)` **wprost do pliku
  docelowego**. Crash, kill albo ENOSPC w trakcie = obciety plik = utrata WSZYSTKICH
  postaci na serwerze. Fix: `tmp` + `Files.move(..., ATOMIC_MOVE)`.
- **Backup rotacyjny** przy starcie i przed kazdym zapisem masowym. Dzis zero.
- **Zapis async z debounce.** Dzis pelna serializacja calego pliku, synchronicznie,
  na main threadzie, przy **kazdym** confirmie (`put():93-98`). Przy 18 rekordach
  niezauwazalne, przy 50k — blokada tick loopa.
- **`schema-version` w pliku** + sciezka migracji. Dzis brak wersji, a `load():62`
  przy brakujacym polu `name` podstawia **ID profilu jako nazwe** — cicha korupcja
  zamiast bledu.
- **Unikalnosc nazw.** `CharacterStore` nie ma zadnego indeksu po nazwie, stad 12x
  "Grimtest" na produkcji. Potrzebny indeks + odrzucenie duplikatu z komunikatem.
- **Whitelist znakow zamiast `\p{L}`.** `NameInputDialog.java:98` wpuszcza cale
  Unicode — cyrylickie `А` pozwala zrobic postac wizualnie nieodroznialna od "Admin".
  Fix: `[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśźż0-9_]`, normalizacja NFKC, lista zakazanych prefiksow.
- **Komunikaty walidacji przez actionbar/title, nie czat** — czat jest zaslonięty
  przez GUI, wiec blad imienia jest dzis niewidoczny (PLAN-v3 Etap 5, niezrobione).
- **Wyczyscic produkcyjny `characters.yml`** z 18 rekordow testowych.

**Weryfikacja:** `kill -9` w petli podczas tworzenia postaci — plik zawsze parsowalny,
zero utraconych rekordow poza ostatnim. Proba zalozenia drugiej postaci o tej samej
nazwie odrzucona.

---

## Etap 4 — PRZESTAC OBIECYWAC RZECZY, KTORYCH NIE MA (0.5 d) — WYKONANE 2026-09-18

Kontynuacja Etapu 0 z PLAN-v3 — ta sama zasada, glebiej. Wtedy chodzilo o teksty
w configu; teraz o kod i dokumentacje, ktore twierdza, ze cos robia.

- **Wybor rasy nie zmienia modelu 3D.** `PreviewDummyManager.swapModel():63-65` to
  **martwy kod** — jedyne wystapienie w calym `src/` to jego definicja.
  `refreshAppearance()` zmienia tylko zbroje. Model jest aplikowany raz, w
  `CreatorSessionManager.java:35`, dla rasy domyslnej.
- **Brakuje 3 z 4 blueprintow.** W `ModelEngine/blueprints/` jest tylko
  `race_nieumarly.bbmodel`. Rasa domyslna to `czlowiek`, wiec w podgladzie **nigdy**
  nie widac zadnego modelu ModelEngine — roznica miedzy rasami to kolor skorzanego
  helmu (`:117-118`). Decyzja do podjecia: dorobic blueprinty albo uczciwie oznaczyc
  rasy bez modelu.
- **`SkinProvider` nie istnieje.** `config.yml:104` i `ConfirmService.java:70`
  odsylaja do klasy, ktorej nie ma w zrodlach. Cala "abstrakcja" to jeden `if`, a
  komenda `cmi skin <gracz> <gracz> -s` jest **logicznym no-opem** — naklada graczowi
  jego wlasna skorke. Do tego serwer jest w `online-mode=false`, a wywolanie leci przy
  **kazdym** `ProfileSelectEvent`, czyli przy kazdym logowaniu (limit Mojanga:
  600 req/10 min/IP). Usunac gałąź i komentarz albo zbudowac realny per-character skin.
- **Permissiony `race.*` sa write-only.** Grep po wszystkich pluginach: jedyny plik
  zawierajacy `race.czlowiek` to nasz wlasny config. Cztery komendy konsolowe na profil
  ustawiaja flage, ktorej nikt nie czyta. Podpiac pod realna mechanike albo usunac.
- **`gear:` ubiera zombie, nie gracza.** Gracz po utworzeniu postaci nie dostaje ani
  jednego przedmiotu, mimo ze README obiecuje ekwipunek startowy.
- **Komenda `/charactercreator` to atrapa** — zadeklarowana w `plugin.yml:9-12`,
  zero `setExecutor` w kodzie.
- **Zdjac martwy kod:** `clearModel()`, `normalizeId()`, `case "random_name"`
  (`actionOf()` nigdy tej akcji nie zwraca).
- **Naprawic dokumentacje albo ja skasowac.** README ma nieaktualne numery slotow,
  PLAN-v3 oznacza wyciszenie czatu jako zrobione, `name-shift` w configu (-147)
  rozjezdza sie z javadociem i defaultem w `CreatorConfig.java:158-165` (-209).
  Jedna z tych liczb jest nieprawdziwa.
- **Walidacja configu przy enable** zamiast odmowy w runtime: kazda klasa musi miec
  istniejace MMOCore ID, kazda rasa istniejacy blueprint albo swiadomie oznaczony brak.
- Wyciszyc spam: `logger.warning` bez rate-limitu (177 wpisow) i `logger.info`
  z pelnym UUID przy kazdym `ProfileCreateEvent`.

**Weryfikacja:** przejsc kreator dla kazdej rasy i klasy; kazda deklaracja w UI
i w configu ma miec pokrycie w obserwowalnym skutku albo zniknac.

---

## Etap 5 — NARZEDZIA I BUILD (0.5 d) — WYKONANE 2026-09-18 (wersja z gita: patrz STATUS)

Przyczyna: nie ma czym naprawic zadnego z powyzszych problemow bez restartu serwera,
i nie ma czym sprawdzic, czy naprawa dziala.

- **Komendy admina:** `/cc reload`, `/cc info <gracz>`, `/cc delete <profileId>`,
  `/cc prune` (sieroty). Dzis `CreatorConfig.reload()` nie ma zadnego wywolujacego
  poza konstruktorem — **zmiana config.yml wymaga restartu serwera.**
- **`hasPermission()`** — `charactercreator.admin` jest zadeklarowany i nigdzie
  niesprawdzany. Zero wywolan w calym pluginie.
- **Testy jednostkowe czystej logiki** — `CreatorMenu.shift()`, `actionOf()`,
  `playerIndex()`, `NameInputDialog.sanitize()`, `CreatorConfig.parseColor()`.
  Wszystkie testowalne bez serwera, zadna dzis nieprzetestowana.
- **Gradle/Maven zamiast `BuildCharacterCreator.java`.** Obecny builder bierze
  classpath **ze wszystkich jarow dzialajacego serwera** i ma wlasny resolver wersji
  na regexach. Stara wersja miala `build.gradle.kts` — to jest regresja.
- **Wersja z gita** zamiast zaszytego `"0.1.0-SNAPSHOT"`. Dzis nie da sie powiedziec,
  ktore zrodla stoja na produkcji.
- **`packetevents` won z `depend`** (`plugin.yml:6`) — zero uzycia w kodzie, same
  komentarze. **`MMOCore` do `depend`** — bez niego postac zostaje bez klasy.
- **`.gitignore` na `build/`** — artefakty sa zacommitowane do repo.
- **Generatory grafiki domyslnie do `./out`**, nie prosto do produkcyjnego packa Nexo
  (`gen_creator_art.py:284-300`). Parsowanie YAML regexami zastapic `pyyaml`.

**Weryfikacja:** `/cc prune` na kopii produkcyjnego pliku usuwa dokladnie te 9 sierot.
Build z czystego klona daje identyczny jar.

---

## Etap 6 — SZESC RZECZY DO SPRAWDZENIA W GRZE — OTWARTE (wymaga zywego klienta)

Recenzja nie mogla ich rozstrzygnac z samego kodu. Kazda moze zmienic priorytety wyzej.

1. Kolejnosc handlerow `PlayerQuitEvent` vs. zapis profilu MMOProfiles.
2. Interakcja naszego stashu z `synced-data.inventory: true` — wejsc w kreator
   z pelnym plecakiem na profilu A, utworzyc profil B, sprawdzic oba.
3. Zachowanie MMOProfiles przy podwojnym `validate()` na tym samym evencie.
4. ~~Czy MMOCore laduje `classes/mage/mage.yml` z podkatalogu~~ — **ROZSTRZYGNIETE: laduje.**
   Dowod z produkcji: w logu world1 widac profil z przypisana klasa `Mage` w ekranie wyboru
   postaci MMOProfiles, a `classes/mage/` faktycznie zawiera `mage.yml` i `arcane-mage.yml`.
   Wybor Maga nie konczy sie cichym brakiem klasy.
5. Zachowanie UI przy GUI scale != 3 — cala geometria zaklada scale 3 / 1280x720,
   a model stoi w swiecie i skaluje sie inaczej niz szyny GUI.
6. Czy 1-tickowe okno po Escape (`reopenMenuNextTick`) wystarcza na wyslanie komendy.

---

## Poza tym planem (swiadomie)

- **PLAN-v3 Etapy 4-6** (redesign UI, UX, odpornosc) zostaja otwarte. Etap 5 z v3
  (wstecz/anuluj, podsumowanie, opis na hover) pokrywa sie z brakami produktowymi
  ponizej i warto go zrobic razem z nimi.
- **Trzy globalne nadpisania resource packa** — przezroczysty shulker, usuniety napis
  `container.inventory`, usuniete `slot_highlight` — psuja GUI **calego serwera**
  (skrzynie, sklepy, DeluxeMenus) dla jednego ekranu uzywanego raz na konto.
  To decyzja produktowa, nie bug: albo swiadomie akceptowana, albo cofnieta.
- **Brak sciezki awaryjnej bez resource packa** — brak `PlayerResourcePackStatusEvent`
  w kodzie. Gracz z odrzuconym packiem widzi tofu i kartki papieru.
- **Braki funkcjonalne wzgledem oczekiwan gracza MMORPG** (nie istnieja w kodzie):
  usuwanie i edycja postaci, sloty i limit postaci, przycisk wstecz/anuluj, ekran
  podsumowania, podglad statystyk, plec, personalizacja wygladu, i18n, startowy
  ekwipunek, prawdziwe lore, dostepnosc. To jest zakres produktowy do decyzji,
  nie dlug techniczny.

---

## Kolejnosc i uzasadnienie

```
Etap 0 (krwawienie)     ─ 2-3 h ─ BLOKUJACE: dzis kreator niszczy rzeczy graczy
Etap 1 (stan sesji)     ─ 0.5 d ─ domyka utrate danych przy awarii
Etap 2 (izolacja)       ─ 0.5 d ─ domyka wyrwanie z kreatora
Etap 3 (dane)           ─ 0.5 d ─ domyka utrate danych przy zapisie
Etap 4 (uczciwosc)      ─ 0.5 d ─ tanie, duzy zwrot w zaufaniu do UI
Etap 5 (narzedzia)      ─ 0.5 d ─ bez tego nic powyzej nie da sie naprawic w locie
Etap 6 (testy w grze)   ─ 0.5 d ─ moze zmienic priorytety, robic rownolegle
```

Etap 0 musi byc pierwszy z tego samego powodu, dla ktorego Etap 0 w v3 byl pierwszy:
dotyczy tego, co kreator **robi graczowi**, a nie tego, jak wyglada. Roznica jest
taka, ze w v3 chodzilo o klamstwo w opisie bonusu, a tu o plecak.

Etapy 0-3 razem to okolo 2 dni i zmieniaja kreator z prototypu w cos, co mozna
wpuscic na serwer. Etapy 4-5 to kolejny dzien i zmieniaja go w cos, co da sie
utrzymywac.

Po Etapie 3 warto rozwazyc to, co recenzja nazwala jedyna rzecza do wyrzucenia:
**caly mechanizm sesji (`CreatorSessionManager` + `CreatorRoomListener` + stash
w RAM) napisany od nowa jako maszyna stanow kluczowana po UUID**, z persystowanym
stanem wejscia, twarda izolacja na teleport/komende/czat i jednym atomowym,
idempotentnym krokiem commit. Etapy 0-3 realizuja te sama zawartosc latkami; jesli
po drodze okaze sie, ze latki na siebie nachodza, przepisanie bedzie tansze niz
ich utrzymanie. Warstwa graficzna zostaje nietknieta w obu wariantach.
