# Zlecenie dla Codexa — etap 1: plan testu, bot-gracz, review kodu zaklęć

Właściciel chce **pełnego testu i review zaklęć tak, jak gra gracz**, z oceną **feelingu** od razu.
Chodzi o: branie zaklęć (Shift+Q pasek, Księga), rzucanie (PPM/LPM/obie), łączenie z innymi
przedmiotami (broń, tarcza, jedzenie, narzędzie, mapa, Dziennik, Sakwa), podmianę rąk (F, kółko, cyfry,
przenoszenie w E), i wszystko „pomiędzy” (śmierć, relog, okna, brak many, ruch).

Ty projektujesz i piszesz. Claude uruchamia (Twój sandbox nie ma dockera), odsyła Ci wyniki w etapie 2,
potem robi sesję prawdziwym klientem wg Twojego scenariusza (etap 3), a Ty oceniasz.

## Ustalone sterowanie (OSTATECZNE, decyzja właściciela — nie proponuj zmiany mapowania)
PPM = LEWA dłoń (trzymaj), LPM = PRAWA (klik), PPM + klik LPM = obie; zmiana zaklęć tylko paskiem Shift+Q
(LPM → prawa, PPM → lewa; ulubione ★ na początku, potem reszta, „pusta” na końcu). Pusta prawa + zaklęcie w lewej
→ PPM rzuca lewą. Q bez Shiftu = hub (MenuKey woła `zaklecia q <gracz>`). Księga: hub Q → Umiejętności → slot 22.
Historia i pułapki — przeczytaj: katalog `/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/`, pliki
`zaklecia-skyrim-stan.md`, `mythicmobs-zaklecia-pulapki.md`, `animacje-broni.md`, `eq-6-slotow-akcesoriow.md`,
`stale-sloty-ekwipunku.md`, `testy-interakcji-i-wydajnosci.md`, `testy-z-perspektywy-gracza.md`,
`eval-botem-mineflayer.md`, `chw-test-serwer-testowy.md`, `wylapuj-sucho-i-bez-sensu.md`.

## Gdzie co jest (czytaj; pisz TYLKO do katalogu roboczego)
- Katalog roboczy (zapis): `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/`
- Plugin Zaklecia (wersja na world1 = af49d5b): `/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia/MCMMORPG/plugin-src/zaklecia/`
- Konfiguracja żywa (kopia klona = world1): `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/data/plugins/`
  — `Zaklecia/config.yml`, `MythicMobs/items/zaklecia/`, `MythicMobs/skills/zaklecia/`, `DeluxeMenus/gui_menus/ksiega_zaklec.yml`,
  `ulubione_zaklec.yml`, `hub_*.yml`, `MenuKey/config.yml`, `CoreTools/` (stałe sloty 8 i 18), `AnimacjeBroni/`, `MMOCore/`.
- Pluginy, które ruszają rękami/slotami (źródła w worktree repo MC `/home/dev/Desktop/Przemek/MC/.claude/worktrees/`):
  `animacje-broni` (LPM seria / PPM ładowany / Shift+PPM blok — konflikt z PPM zaklęć!), `eq-klon-niewidoczny` i `eq-lewa-reka`
  (ksiega-postaci, okno E; `Bron.refresh` wcześniej zabierało lewą rękę), `klasy-bron`, `mapa-ppm` (mapa w ręce, PPM).
  Ustal, która gałąź = jar na world1 (nazwy jarów w `/opt/docker/mc/data/world1/plugins/`), jeśli się da.
- Stare evale do wzoru: `/home/dev/Desktop/Przemek/MC/tools/zaklecia/eval/` (`resourcepack.js` — bez niego bot utyka
  w CONFIGURATION; `lewa-najpierw.js`, `dwurecz.js`, `klawiszq.js` — pakiet Q, `ulubione2.js` — sprint ręcznie).
  `node_modules` tam: mineflayer 4.39.0, minecraft-protocol 1.68.0, rcon-client.
- Zdekompilowany klient 1.21.11 (oficjalne mapowania): `/home/dev/.claude/jobs/b0bfedc4/tmp/dekomp2/src`
  (`net/minecraft/client/Minecraft.java` handleKeybinds/startUseItem/startAttack/continueAttack,
  `MultiPlayerGameMode`, `KeyboardHandler`, `LocalPlayer`).

## Środowisko testu (stawia Claude)
Klon `zaklecia-gracz` (pełny stos pluginów world1, płaski świat, bez kreatora i MMOProfiles), sieć `zaklecia-gracz-net`,
host `zaklecia-gracz:25565`, offline-mode, bez velocity, RCON `zaklecia-gracz:25575` hasło `zakgracz`.
Bot uruchamiany w `node:20` na tej sieci. Spawn płaskiego świata — ustal w skrypcie przygotowania i zbuduj arenę
(platforma + cele: husk, NIE zombie — pali się w dzień; drugi bot jako cel-gracz do leczenia innych/PvP).

## ZASADA NADRZĘDNA: bot gra jak gracz
- Konto bez OP. Konsola/RCON WYŁĄCZNIE do: przygotowania sceny (klasa/poziom/mana, jeśli gra tego wymaga, cele, czas
  dnia, wyczyszczenie stanu) i OBSERWACJI (papi parse, data get, liczenie encji). Nigdy do popychania gracza przez
  badany scenariusz (żadnego `zaklecia przypisz` zamiast paska — chyba że to osobny test kontrolny i tak go nazwiesz).
- Zaklęcia bot bierze jak gracz: Q → hub → Umiejętności → Księga, i Shift+Q pasek. Jeśli zwykły gracz nie może
  ich zdobyć (brak źródła, wymagana klasa/permisja) — to ZNALEZISKO, zapisz je, a obejście nazwij obejściem.
- **Warstwa „model prawdziwego klienta”** (najważniejsza część): mineflayer wysyła pakiety, których prawdziwy klient
  NIE wysyła (03.10 eval dał PASS, a bug u gracza był: klient z PUSTĄ ręką przy PPM w powietrze nie wysyła nic;
  mineflayer wysyła use_item). Napisz `lib/klient.js`: każda akcja gracza (ppmWcisnij/Pusc, lpmKlik, lpmTrzymaj,
  kolko, cyfra, f, q, shiftQ, e/zamknij okno, sprint, skok, shift, ruch W) przechodzi przez reguły klienta 1.21.11
  WYPROWADZONE Z KODU KLIENTA (cytuj plik:linię w komentarzu), zależne od trzymanego przedmiotu i jego komponentów
  (`consumable`, `attack_range`, `piercing_weapon`, `kinetic_weapon`, `blocks_attacks`, pusta ręka, tryb gry, cel
  pod celownikiem: istota/blok/powietrze, zasięgi z atrybutów gracza). Min.: połykanie ataku przy isUsingItem,
  startUseItem zablokowany przy isDestroying, pusta ręka + PPM w powietrze, powtarzanie use co 4 t, zwolnienie
  (release_use_item), machnięcie, pakiet Q/Ctrl+Q, F (swap), sprint w player_input (mineflayer go nie wysyła).
  Komponenty i atrybuty zmieniane przez serwer w locie (plugin podmienia je w trybie zaklęć) śledź z pakietów.
  Czego nie da się wiernie zamodelować — wypisz jako „tylko prawdziwy klient” (idzie do etapu 3).

## Co ma sprawdzić (rozbuduj — to minimum)
A. Zdobycie i wybór: Księga z huba, ulubione ★ (Shift+LPM w Księdze), pasek Shift+Q (prawa/lewa/pusta, kolejność,
   kółko), lewa najpierw przy pustej prawej, z bronią/jedzeniem/narzędziem/mapą w prawej, Shift+Q przy otwartym oknie,
   Q przy otwartym pasku, ponowne otwarcie.
B. Rzucanie każdego typu z configu (koncentracja, ładowany, runa, tarcza, broń przywołana, przywołania z limitem,
   leczenie siebie/innych, uspokojenie): prawa, lewa, obie, to samo w obu (podwójne), dwa różne; brak many w trakcie;
   cel: husk / gracz w strefie PvP i bez / powietrze / ściana.
C. Inne przedmioty: broń MMOItems w prawej + zaklęcie w lewej (AnimacjeBroni: LPM seria, PPM ładowany, Shift+PPM blok —
   kto wygrywa PPM?), tarcza/jedzenie w lewej, mikstura, łuk, blok do postawienia, mapa w ręce, Dziennik (slot 8),
   Sakwa (slot 18).
D. Podmiana rąk i slotów: F (MMOCore tryb skilli!), kółko/cyfry w trakcie rzucania (mana i strumień mają zgasnąć),
   Q na nośniku (nie może wypaść), przenoszenie nośnika w E / shift-klik / do skrzyni / do Sakwy (duplikaty, „duchy”),
   okno E w trakcie rzucania, śmierć w trakcie, relog w trakcie (odstęp ≥1 s — wiadomo o NPE MMOCore przy <1 s),
   menu huba otwarte w trakcie rzucania.
E. Ruch i obciążenie: rzucanie w biegu/sprincie/skoku/skradaniu/w wodzie, serie 5 kliknięć, spam paska.
F. FEELING — mierzalnie, z serwera i z pakietów do bota: opóźnienie wciśnięcie→pierwszy efekt (tiki) i puszczenie→
   koniec, spadek many vs opis w lore, liczba set_slot/equipment na nośnikach na sekundę (migotanie), komunikaty
   czatu/paska na akcję (spam, nadpisywanie), dźwięki na akcję, spowolnienie ruchu, czy przedmiot w ręce zgadza się
   ze stanem (`%zaklecia_prawa|lewa%`), zgubione/zdublowane przedmioty (hash ekwipunku przed/po), błędy w logu klona.
   Każdy scenariusz ma oprócz PASS/FAIL wiersz `FEEL <metryka>=<wartość> (próg …)`. Progi zaproponuj i uzasadnij.

## Co oddajesz w etapie 1 (pliki w katalogu roboczym)
1. `PLAN.md` — macierz testów: id, co robi gracz, czego oczekuje gracz, jak mierzymy, próg feelingu, bot czy
   prawdziwy klient. Zwięźle, tabela.
2. `bot/` — harness: `package.json`, `lib/klient.js` (model klienta), `lib/obserwator.js` (RCON + nasłuch pakietów,
   metryki), `lib/scena.js` (przygotowanie/sprzątanie), `scenariusze/*.js` (jeden plik na grupę A–F, wynik
   PASS/FAIL/FEEL w JSON + linie tekstu), `run.sh [filtr]` (docker run node:20 na `zaklecia-gracz-net`; node_modules
   podmontuj z `../eval/node_modules`), `przygotuj.sh` (scena z konsoli przez RCON — może być node).
   Kod ma się uruchomić bez Twojej pomocy; każdy scenariusz samodzielny, sprząta po sobie, ma limit czasu.
3. `klient-prawdziwy.md` — scenariusz sesji prawdziwym klientem (etap 3): kroki jako sekwencje xdotool
   (klawisze, przyciski myszy, czasy wciśnięć) + gdzie zrzut i co na nim ocenić (poza dłoni, strumień, pasek, HUD many,
   czytelność, „czy to się dobrze czuje”). Tylko rzeczy, których bot nie zobaczy.
4. `review-kodu.md` — statyczny review pluginu i configów pod kątem błędów, które gracz trafi: wyścigi stanu rąk,
   nośnik gubiony/duplikowany, wycieki zadań/encji (strumień, przywołańcy), nieobsłużone zdarzenia (śmierć, zmiana
   świata, kick), konflikty z AnimacjeBroni/ksiega-postaci/CoreTools/MenuKey, ciężka praca na wątku głównym. Każde:
   plik:linia, scenariusz gracza → skutek, pewność (pewne/prawdopodobne), który test z PLAN.md to potwierdzi.
   NIE poprawiaj pluginu — tylko review.

## Zasady
- Nie dotykaj produkcji (`/opt/docker/mc/data/world1/plugins` itd.) ani źródeł pluginów. Zero reloadów/restartów.
- Twierdzeń o stanie na żywo nie zgaduj z plików — oznacz „do sprawdzenia w etapie 2”.
- Raport końcowy zapisz do `/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/raport-etap1.md` (to też plik wyjścia),
  PO POLSKU, max 50 linii: co powstało, jak uruchomić, 5 najgroźniejszych podejrzeń z review, czego nie da się
  zrobić botem, pytania. Bez wklejania kodu ani diffów.
