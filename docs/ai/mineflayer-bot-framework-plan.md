# Plan wdrożeniowy uniwersalnego frameworka bota Mineflayer dla serwera MMORPG (Paper 1.21.11, offline-mode)

> Zapisano 2026-07-14. Kontekst: `~/projects/MC/bots/` już istnieje jako monorepo (`packages/*`, `apps/*`, node>=22, `prismarine-item@1.18.0` / `prismarine-nbt@2.8.0` przypięte, skrypty `smoke`/`fleet`/`bench`). Ten dokument jest planem docelowej architektury i checklistą ryzyk — traktować jako uzupełnienie, nie zamiennik istniejącego kodu. Przed implementacją każdego etapu zweryfikować, co już istnieje w `packages/`/`apps/`.

## TL;DR
- **Framework jest wykonalny** na `mineflayer` 4.37.1 (Node.js `>=22`, oficjalne README deklaruje wsparcie do MC 1.21.11), ale rdzeń gameplay/walki niesie realne ryzyko `PartialReadError` przy metadanych ModelEngine/LibsDisguises — musisz zbudować własną warstwę bezpiecznego parsowania pakietów i testować wyłącznie na **klonie stagingowym**, nigdy na produkcji z danymi graczy.
- **Dane MMOItems są odczytywalne**: komponent `minecraft:custom_data` (zawierający `MMOITEMS_ITEM_ID`) jest domyślnie transmitowany do klienta na stockowym Paper (obfuskacja itemów domyślnie wyłączona), więc bot odczyta identyfikator itemu przez `prismarine-nbt`. Natomiast stany server-side (HP z MMOCore, questy, progresja) są niewidoczne dla klienta i wymagają mostu — najlepiej małego companion-pluginu Paper przez plugin messaging channel.
- **Architektura**: monorepo TypeScript z jednym `BotCore`, magistralą komend (command bus), kolejką zadań i biblioteką umiejętności; uruchamiaj N botów jako **osobne procesy** (nie w jednym procesie); MCP (yuniko) i Mindcraft podłączaj jako **osobne konta bota**, a nie współdzielony proces.

## Key Findings

**Rdzeń ekosystemu jest żywy, warstwa pluginów społecznościowych — w dużej części martwa.** `mineflayer` 4.37.1 został opublikowany 3 maja 2026 (potwierdzone przez npmx.dev/Snyk; zależności: `minecraft-protocol` ^1.66.0, `prismarine-item` ^1.17.0, `prismarine-nbt` ^2.0.0, wymóg Node.js `>=22`), a oficjalne README PrismarineJS podaje listę wspieranych wersji kończącą się na „1.21.9, 1.21.11". Jednocześnie kluczowe pluginy do gameplayu są przypięte do starych wersji: `mineflayer-pvp` 1.3.2, `mineflayer-tool` 1.2.0, `mineflayer-statemachine` 1.7.0. Oznacza to, że warstwa walki/AI opiera się na komponentach nieaktualizowanych od lat i realnie trzeba liczyć się z ich forkowaniem.

**Największe pojedyncze ryzyko techniczne to parsowanie pakietów.** To nie jest hipoteza. Na czystym Paper 1.21.8 (mineflayer 4.33.0, Node 20.19.19) zgłoszono w GitHub Issue #3756: „*a stream of `PartialReadError: Unexpected buffer end while reading VarInt` is thrown immediately after the bot successfully connects. The stack trace consistently points to an issue with parsing `packet_entity_metadata`*". Analogicznie, na Paper 1.21.4 z pluginami modyfikującymi NBT (Issue #3669): „*These tags break the default NBT parser in Mineflayer (or protodef), leading to errors like `PartialReadError`*". Twój stack (ModelEngine, MythicMobs, LibsDisguises, MEG-Molang) to dokładnie środowisko, które generuje takie custom entity metadata.

**Odczyt itemów MMOItems działa lepiej, niż można było się obawiać.** Ustalono, że komponent `minecraft:custom_data` jest domyślnie synchronizowany po sieci do klienta, a paperowa opcja `enable-item-obfuscation` ma w `paper-global.yml` (`_version: 29`) wartość domyślną `false`, dotyczy tylko itemów widocznych na *innych* graczach i — wg PaperMC PR #11817 — nawet po włączeniu obejmuje komponenty typu `CUSTOM_NAME`, `LORE`, `ENCHANTMENTS`, a **nie** `custom_data`. Wniosek: na stockowym Paper bot odczyta `MMOITEMS_ITEM_ID` bezpośrednio z NBT swojego ekwipunku/otwartego okna.

---

## Details

### A) Ekosystem Mineflayer — wersje i realny stan utrzymania (2026)

| Pakiet | Wersja | Kompatybilność / status | Rola w frameworku |
|---|---|---|---|
| `mineflayer` | **4.37.1** (3 maja 2026) | **Aktywny.** Node `>=22`; README: wsparcie do MC 1.21.11 | Rdzeń — obowiązkowy |
| `minecraft-protocol` | ^1.66.0 | Aktywny (transitive) | Warstwa protokołu; tu manifestują się `PartialReadError` |
| `prismarine-item` | **1.17.0** | **Aktywny.** Data components od 1.20.5 (PR #118) | Reprezentacja itemu, `item.nbt` |
| `prismarine-nbt` | **2.0.0** | Aktywny | `nbt.simplify()` — parsowanie custom_data |
| `mineflayer-pathfinder` | 2.x | **Utrzymywany wolno** (aktywność do 2025, 260★, 84 forki) | Pathfinding gameplay-agenta |
| `mineflayer-pvp` | **1.3.2** | **Przestarzały** (npm: „4 lata"; profil thedudefromci: „3 lata" — rozbieżność datowania) | Walka PvE; wysokie ryzyko na 1.21.11, kandydat do forka |
| `mineflayer-collectblock` | 1.6.0 | Względnie świeży | Zbieranie dropów |
| `mineflayer-tool` | 1.2.0 | **Przestarzały** (4 lata) | Auto-dobór narzędzi |
| `mineflayer-armor-manager` | 2.0.1 | Stary (3 lata) | Zarządzanie zbroją |
| `mineflayer-auto-eat` | — | Sprawdź świeżość przed użyciem | Jedzenie |
| `mineflayer-statemachine` | 1.7.0 | **Przestarzały** (3 lata) | Zamiast tego zalecam własną maszynę stanów w TS |
| `mineflayer-web-inventory` | 1.8.5 | Przestarzały (2 lata); wymaga mineflayer ≥4.2.0 | Podgląd ekwipunku w przeglądarce |
| `mineflayer-gui` (firejoust) | **4.0.2** | **Aktywny.** Promise-based, chainable API | Nawigacja po zagnieżdżonych GUI (DeluxeMenus) |
| `prismarine-viewer` | wspiera do ~1.21.4 | Ograniczony; **nie renderuje ModelEngine/Nexo** | Podgląd świata (bird's-eye/first-person) |
| `prismarine-schematic` | 1.2.3 | Stary (4 lata) | Odczyt `.schem` |
| `mineflayer-schem` (AnonymoDGH) | 1.5.2 | Aktywny fork, wspiera `.schematic/.schem/.litematic` | Budowanie ze schematów — **używaj tego zamiast oficjalnego builder** |
| `mineflayer-builder` (oficjalny PrismarineJS) | 1.0.1 | **NIEUŻYWALNY.** README verbatim: „*Work in progress. This is not an usable package yet.*"; 0 dependents na npm | Nie używać |

**Wniosek dla planu:** rdzeń (`mineflayer` + `prismarine-*`) jest solidny i aktualny. Warstwa gameplay (`pvp`, `tool`, `statemachine`, `armor-manager`) jest przestarzała — traktuj ją jako komponenty ryzykowne: przypnij dokładne wersje (`package-lock`/`pnpm-lock`), testuj na stagingu i przygotuj się na forki. Budowanie ze schematów rób przez `mineflayer-schem`, nie przez porzucony oficjalny `mineflayer-builder`.

### B) Odczyt danych custom itemów MMOItems (NBT / data components)

**Zmiana modelu w 1.20.5+.** Minecraft porzucił płaski `tag` NBT na rzecz data components. Wszelkie niestandardowe NBT pluginów (w tym tagi MMOItems) mogą trwale żyć wyłącznie wewnątrz komponentu `minecraft:custom_data` — to jedyne wymuszone przez vanillę miejsce na dowolne NBT.

**Tagi MMOItems.** Potwierdzony jest zapis `MMOITEMS_ITEM_ID` (String) — wynika to wprost z API MMOItems (`NBTItem.get(ItemStack)` → `nbtItem.hasType()`, `nbtItem.getType()`, `getString("MMOITEMS_ITEM_ID")`, wiki `git.mythiccraft.io`) oraz z niezależnego addona Shopkeepers-MMOItems (testowanego na Paper 1.21.4), który identyfikuje MMOItem po obecności tagu `MMOITEMS_ITEM_ID`. Typ jest dostępny przez `getType()` (konwencjonalnie `MMOITEMS_ITEM_TYPE`), a poszczególne staty jako tagi `MMOITEMS_<STAT>` (np. widziany w praktyce `MMOITEMS_ENCHANTMENT_SLOT`). **Uwaga epistemiczna:** literalny string `MMOITEMS_ITEM_TYPE` i wzorzec `MMOITEMS_<STAT>` to konwencja MMOItems/MythicLib (NBTItem), potwierdzona pośrednio; tylko `MMOITEMS_ITEM_ID` znaleziono dosłownie w źródłach. MMOItems trzyma własne klucze NBT wewnątrz `custom_data` (styl NBTItem MythicLib), a nie w Bukkit PDC (które byłoby pod pod-kompaundem `PublicBukkitValues`).

**Czy dane docierają do bota? TAK (domyślnie).** `minecraft:custom_data` jest komponentem synchronizowanym po sieci — klient go otrzymuje i parsuje. Paperowa obfuskacja (`enable-item-obfuscation`) jest domyślnie `false`, a nawet włączona dotyczy tylko itemów pokazywanych *innym* graczom (nie własnego ekwipunku/otwartego okna bota) i nie obejmuje `custom_data` na domyślnej liście. **Praktyczny wniosek:** bot inspekcjonujący własny inventory lub otwarty kontener na stockowym Paper odczyta `MMOITEMS_ITEM_ID` z NBT. Fallback (parsowanie lore/display name) jest opcjonalny, nie wymagany — ale warto go mieć jako zapasowy, bo operator serwera *może* włączyć obfuskację i dodać `custom_data` do `also-obfuscate` (nietypowe, poza domyślną konfiguracją).

**Jak odczytać (kod).** `prismarine-item` 1.17 udostępnia surowe dane przez getter `item.nbt`; w 1.20.5+ struktura jest kluczowana namespace'ami komponentów:

```js
const nbt = require('prismarine-nbt')
function getMMOItemId (item) {
  if (!item || !item.nbt) return null
  const data = nbt.simplify(item.nbt)          // {type,value} -> plain JS
  const cd = data['minecraft:custom_data']     // komponent custom_data
  return cd ? cd.MMOITEMS_ITEM_ID ?? null : null
}
```

**Weryfikacja typu/statów:** odczytaj `data['minecraft:custom_data']` i porównaj `MMOITEMS_ITEM_ID` + `MMOITEMS_ITEM_TYPE` z oczekiwaną definicją; staty vanilla-podobne (attribute_modifiers) czytaj z osobnych komponentów. **Uwaga:** MMOItems zmienia format NBT niemal z każdą aktualizacją (potwierdzone przez społeczność — „*MMOItems changes their NBT format with like, every update*"), więc testy regresji itemów muszą być wersjonowane razem z wersją MMOItems, a asserty pisz defensywnie (sprawdzaj istnienie klucza zanim odczytasz wartość). Zweryfikuj też, czy w Twojej wersji `prismarine-item` 1.17 istnieje pierwszorzędny akcesor `item.components` — pewną ścieżką jest `item.nbt` + `nbt.simplify()`.

### C) Interakcja z GUI / DeluxeMenus

**Mechanizm.** Mineflayer emituje `windowOpen` przy otwarciu okna (chest-based menu) i pozwala klikać przez `bot.clickWindow(slot, mouseButton, mode)`. Sloty numerowane są od 0 (okno kontenera najpierw, potem inventory gracza).

**Krytyczna pułapka — potwierdzenia transakcji.** Wiele pluginów Spigot/Paper (w tym menu chest-based) nie wysyła potwierdzeń transakcji, przez co promise `clickWindow` nigdy się nie rozwiązuje i kod za nim jest nieosiągalny. Oficjalny przykład `mineflayer/examples/advanced/chest_confirm.md` podaje verbatim obejście:

```js
bot.on('windowOpen', async (window) => {
  window.requiresConfirmation = false   // fix
  await bot.clickWindow(13, 0, 0)        // without the fix this code is unreachable, the promise never resolve
})
```

**Zalecenie.** Do testów DeluxeMenus użyj `mineflayer-gui` (firejoust) 4.0.2 — chainable, promise-based API (`.Window()`, `.Inventory()`, `.Open()`, komparatory po `displayName`/slocie), które upraszcza nawigację po zagnieżdżonych menu i dopasowywanie itemów po nazwie/lore.

**Ograniczenia, które trzeba obejść w testach:**
- **Odświeżanie menu / placeholdery:** DeluxeMenus przerysowuje sloty (aktualizacja placeholderów PlaceholderAPI). Bot musi czytać stan slotu *po* zdarzeniu aktualizacji okna, nie natychmiast po otwarciu — dodaj krótkie oczekiwanie na re-render i porównuj `window.slots[...]` dopiero po stabilizacji.
- **Warunki (requirements) DeluxeMenus:** slot może nie reagować, jeśli gracz-bot nie spełnia warunku (permission, pieniądze). Testuj z kontrolowanym stanem konta i LuckPerms.
- **Klik „nic nie robi":** historycznie `clickWindow` na niektórych pluginach zwraca `undefined` bez efektu — asertuj skutek uboczny (zmiana chatu/actionbar/nowe okno), nie sam brak wyjątku.

### D) Ryzyka pakietowe — diagnoza i obejścia

**Objaw #1: `PartialReadError` przy `packet_entity_metadata`.** Najczęstsza awaria z Twoim stackiem. Custom entity metadata z ModelEngine (`item_display`/`block_display`), MythicMobs i LibsDisguises (disguise metadata) mogą łamać domyślny parser protodef. Potwierdzone przypadki: Issue #3756 (Paper 1.21.8, natychmiast po połączeniu) i #3669 (Paper 1.21.4, custom NBT pluginów łamie parser). Bot bywa *w stanie dołączyć* mimo strumienia błędów, ale zdarzenia są tracone.

**Objaw #2: `PartialReadError`/`Missing characters in string` przy NBT itemów/slotów** — gdy plugin wysyła złożone/niestandardowe NBT (Issue #3526, #3669). Spoofowanie wersji (handshake 1.20.4 vs realne 1.21.x) *nie działa* — serwer waliduje protokół i rozłącza.

**Objaw #3: brak/utknięcie ładowania chunków** — znany regres PacketEvents na Paper 1.21.10 build 113 (Issue #1399 retrooper/packetevents: „*fly around and chunk stops loading*"; build 69 działał). To bezpośrednio uderza w Twoje testy Iris/Chunky. ProtocolLib przy rejestracji pakietu map_chunk również potrafi „gubić" chunki do czasu reloadu.

**Diagnostyka (kolejność):**
1. Uruchamiaj bota z `hideErrors: false`, loguj pełne stack trace i nazwę pakietu.
2. Włącz debug protokołu (`DEBUG=minecraft-protocol`).
3. Izoluj plugin: na klonie stagingowym wyłączaj po kolei ModelEngine → LibsDisguises → MythicMobs i sprawdzaj, który pakiet łamie parser.
4. Skoreluj wersję Paper/PacketEvents/ProtocolLib z regresem (pin buildów).

**Obejścia (wdrożeniowo):**
- **Twarda izolacja awarii:** każdy bot w osobnym procesie z auto-restartem (patrz przykład `reconnector.js`); `PartialReadError` w jednym procesie nie może zabić całej floty.
- **Global error handling** na poziomie procesu (`process.on('uncaughtException')`) + `bot.on('error')`/`bot.on('kicked')` z logowaniem i kontrolowanym restartem.
- **Ograniczenie ekspozycji na metadata:** ustaw małą `viewDistance` (np. `'tiny'`/1) w scenariuszach, gdzie bot nie potrzebuje widzieć zamaskowanych mobów; to zmniejsza szansę na pakiety z problematycznymi metadanymi.
- **Wersjonowanie:** przypnij dokładny build Paper, PacketEvents i ProtocolLib; każdą aktualizację waliduj smoke-testem na stagingu przed produkcją.
- **Nie licz na spoofing wersji** — jawnie ustawiaj `version` zgodnie z serwerem.

### E) Architektura frameworka

**Struktura (monorepo TypeScript, pnpm/turbo):**
```
packages/
  bot-core/        # BotCore: opakowanie mineflayer + bezpieczne parsowanie, event bus
  safe-protocol/   # warstwa ochrony przed PartialReadError (try/catch, filtrowanie metadanych)
  skills/          # skill library: move, dig, place, click-gui, read-nbt, attack...
  command-bus/     # magistrala komend (źródła: skrypt, chat, LLM)
  task-queue/      # kolejka zadań + scheduler + priorytety
  state-machine/   # własna FSM (zamiast porzuconego statemachine)
  qa/              # asercje, scenariusze regresji, raporty
  mmo-bridge/      # klient plugin-messaging/websocket do companion-pluginu
apps/
  orchestrator/    # zarządzanie flotą procesów (spawn/kill/health)
  chat-controller/ # parser komend in-game
  mcp-adapter/     # integracja z yuniko MCP
  mindcraft-adapter/
services/
  companion-plugin/ # (Java/Paper) eksport stanu MMO
```

**Trzy warstwy sterowania → jeden command bus.** Wszystkie trzy źródła (skrypt JS/TS, chat in-game, LLM) emitują *te same* komendy do wspólnej magistrali. Skrypt woła bezpośrednio API `command-bus`; `chat-controller` parsuje `bot.on('chat'/'messagestr')` i mapuje na komendy; adaptery LLM tłumaczą wywołania narzędzi na komendy. Dzięki temu skill library jest jedynym miejscem wykonującym akcje — łatwiej testować i logować.

**Współdzielenie rdzenia z MCP.** yuniko `minecraft-mcp-server` (najnowszy release 2.0.4, 26 lutego; obsługuje MC 1.21.11, ma global error handling i auto-reconnect) jest *samodzielnym* procesem uruchamianym przez `npx`/Claude Desktop, z własnym botem Mineflayer. **Nie próbuj** wstrzykiwać go do swojego procesu — to osobny agent. Zamiast tego: uruchom go jako kolejnego bota-klienta na tym samym serwerze i, jeśli chcesz go zintegrować z magistralą, napisz cienki `mcp-adapter`, który wystawia Twoje skille jako narzędzia MCP (własny serwer MCP owijający `bot-core`), zamiast używać kodu yuniko.

**Współistnienie Mindcraft i yuniko-MCP.** Oba to niezależne procesy z własnymi kontami. Mindcraft (`mindcraft-bots/mindcraft`) obsługuje MC do 1.21.11 (rekomendowane 1.21.6), Node 18/20 LTS (Node 24+ psuje natywne zależności). **Krytyczne ostrzeżenie bezpieczeństwa z README Mindcraft:** „*Do not connect this bot to public servers with coding enabled ... allows an LLM to write/execute code on your computer. The code is sandboxed, but still vulnerable to injection attacks.*" `allow_insecure_coding` trzymaj `false`, a Mindcraft uruchamiaj w kontenerze Docker (README podaje gotowe polecenie z `host.docker.internal`). Każdy agent = osobne konto na whiteliście.

**Wzorce:** command bus (rozdziela źródło od wykonania), task queue z priorytetami (integralność > stabilność > wydajność > gameplay), FSM per bot (idle→task→verify→report), skill library (atomowe, testowalne akcje z timeoutami).

### F) Multi-bot / load-testing

**Osobne procesy, nie jeden proces.** Znany problem Mineflayer: przy tworzeniu wielu botów w jednej pętli w jednym procesie (`examples/multiple.js`) często łączy się tylko pierwszy (Issue #2749). Badanie okołonaukowe (Okanagan College) potwierdza: „*we spawned 1 NodeJS process per bot for the best stability*". Dla load-testu na dużą skalę użyj orchestratora spawnującego procesy potomne + skrypt czyszczący (bot-destroyer).

**Gotowe narzędzia do adaptacji:**
- **StressTest-MC (GaetanOff)** — Mineflayer + scenariusze YAML, worker_threads (konfigurowalna liczba wątków), `botJoinDelay` (domyślnie 500 ms), proxy SOCKS5, Docker. Dobra baza pod Twój orchestrator.
- **tino964MC/Minecraft-Bot-Stress-Tester** — Node+Mineflayer, web UI, „dumb AI" (sprint/jump/swim bez pathfindingu = niskie CPU), auto-respawn, anti-stuck, priorytet chodzenia na duże dystanse (test worldgen — idealne pod Iris/Chunky).

**Praktyka:**
- Rozłóż logowania w czasie (`joinDelay`), by nie zabić login-servera.
- Dla testu ładowania chunków: boty chodzą na duże dystanse w różnych kierunkach, mierz czas re-renderu i braki chunków (pamiętaj o regresie PacketEvents z sekcji D).
- **Pomiar TPS z perspektywy serwera, nie klienta:** Mineflayer nie widzi TPS bezpośrednio. Odczytuj `/tps`/`/mspt` (lub lepiej `/spark`) — PaperMC oznacza `/tps` i `/mspt` jako wspierane, ale rekomenduje `/spark` jako jedyne wiarygodne źródło danych o wydajności. Bot może wywołać komendę i sparsować odpowiedź z chatu, albo lepiej — companion-plugin eksportuje TPS/MSPT przez kanał do orchestratora (dokładniejsze niż parsowanie chatu).
- Zasoby: worker_threads dla lekkich botów „dumb", osobne procesy dla botów z pełnym pathfindingiem/viewerem.

### G) Bezpieczeństwo i izolacja (offline-mode = priorytet #1: integralność danych graczy)

Offline-mode oznacza, że serwer nie weryfikuje tożsamości przez Mojang — **każdy może dołączyć jako dowolny username**, w tym podszyć się pod administratora. To fundamentalne ryzyko przy botach.

**Warstwy obrony (wdrażaj wszystkie):**
1. **Staging klon zamiast produkcji.** Testy build/dig/drop/ekonomia rób na sklonowanym świecie **bez danych graczy**. To realizuje priorytet #1 (integralność danych graczy). Produkcję testuj tylko read-only smoke-testami po wyraźnej decyzji.
2. **Whitelist** — konta botów na białej liście; serwer testowy zamknięty dla obcych (offline-mode + brak whitelisty = otwarte drzwi).
3. **LuckPerms: dedykowana grupa `bot` bez OP**, z minimalnym zestawem uprawnień pod konkretny scenariusz (np. tylko `/mmoitems give` dla bota QA itemów). Zero `*`, zero OP.
4. **WorldGuard sandbox** — region z flagą `build` tylko dla grupy botów; poza regionem `build deny`. Chroni świat przed przypadkowym terraformingiem bota.
5. **Backupy przed każdym testem build/dig** — automatyczny snapshot regionu (FastAsyncWorldEdit) przed i przywrócenie po.
6. **Izolacja sieciowa** — serwer stagingowy nie na publicznym IP; boty łączą się po localhost/VPN.
7. **Osobne konta per agent** (skrypt, MCP, Mindcraft) dla audytu w logach.

### H) Live preview

- **`prismarine-viewer`** — serwer web (port, np. 3007), tryb bird's-eye lub first-person, plus API rysowania (`drawLine` — np. wizualizacja ścieżki pathfindera). **Ograniczenia krytyczne dla Twojego stacka:** nie renderuje custom modeli ModelEngine/Nexo (widzisz tylko bloki i wanilkowe encje), historycznie nie renderuje bloków poniżej y=0, a wspierane wersje kończą się ~1.21.4 — na 1.21.11 renderowanie może być niepełne. Traktuj jako podgląd *ruchu/pozycji*, nie wyglądu.
- **`mineflayer-web-inventory`** (1.8.5) — podgląd ekwipunku bota w przeglądarce (port 3000); przydatne do wizualnej weryfikacji, co bot faktycznie ma.
- **Realny klient Minecraft w trybie spectator + resource pack** — jedyny sposób na wizualną weryfikację custom modeli (ModelEngine/Nexo/MythicHUD). Zaloguj się prawdziwym klientem jako obserwator obok bota. To rekomendowana metoda dla QA wyglądu modeli/HUD.

### I) Konkretne scenariusze testowe QA

**Smoke-test po restarcie (automatyczny):** bot łączy się → weryfikuje spawn → wykonuje sekwencję: `/mmoitems give` testowego itemu → odczyt `MMOITEMS_ITEM_ID` z NBT → otwarcie DeluxeMenu → klik slotu → sprawdzenie regionu WorldGuard → raport PASS/FAIL do orchestratora/Slacka. Uruchamiany po każdym `server start`.

**Regresja itemów MMOItems:** dla każdej definicji itemu — `give`, odczyt `custom_data`, asercja `MMOITEMS_ITEM_ID` + typ + kluczowych statów; porównanie ze snapshotem z poprzedniej wersji (wykrywa zmiany po update MMOItems).

**Drop mobów MythicMobs:** bot (OP na stagingu) spawnuje MythicMob → zabija (lub `/mm mobs kill`) → zbiera dropy (`mineflayer-collectblock`) → asercja obecności i ID zdropionego itemu.

**GUI/DeluxeMenus:** otwarcie menu przez komendę/item → oczekiwanie na re-render → klik zdefiniowanego slotu → asercja skutku (nowe okno / zmiana actionbar / efekt ekonomiczny).

**Region WorldGuard:** bot wchodzi w region → próba `dig`/`place` → asercja, że flaga `build` działa zgodnie z konfiguracją (deny poza sandboxem).

**Permissions LuckPerms:** bot próbuje komendy dozwolonej i zabronionej → asercja odpowiedzi „no permission" dla zabronionej.

**Wykrywanie błędów w chacie/logach:** `bot.on('messagestr')` z regexami wyłapującymi stack trace/`ERROR`/`WARN` → alert.

**Ekonomia:** bot z kontrolowanym saldem (Vault/CMI) → transakcja w GUI shopu → asercja zmiany salda przez companion-plugin.

### J) Integracja stanu MMO — most do danych server-side

**Problem:** bot (klient vanilla) nie widzi stanów server-side: HP z MMOCore, poziomu, many, questów, ekonomii. Widzi tylko to, co serwer wysyła klientowi.

**Poziom 1 — placeholdery przez elementy UI (szybki, ograniczony):** czytaj `scoreboard`, `bossbar`, `actionbar`, `chat`, `tab list` — jeśli serwer eksponuje tam dane (MythicHUD/DecentHolograms/PlaceholderAPI często to robią). Parsuj `bot.on('messagestr')`, `bot.scoreboard`, zdarzenia bossbar. **Zaleta:** zero dodatkowego kodu server-side. **Wada:** kruche (regex), zależne od formatowania, nie pokrywa wszystkiego.

**Poziom 2 — companion-plugin Paper (rekomendowany dla poważnego QA):** napisz mały plugin Paper, który eksponuje stan przez **plugin messaging channel** (pakiet `custom_payload` na własnym kanale w namespace, np. `mmoqa:state`) lub websocket. Bot rejestruje kanał i odczytuje strukturalne dane: dokładne HP MMOCore, poziom, staty, status questu, saldo, TPS/MSPT. Mineflayer obsługuje `custom_payload` (uwaga na historyczny bug z kodowaniem — koduj payload poprawnie jako długość+dane). **Kiedy warto pisać plugin zamiast placeholderów:** gdy potrzebujesz (a) dokładnych wartości liczbowych server-side, (b) danych niewystawionych w UI (questy, cooldowny, wewnętrzne staty), (c) niezawodności (strukturalne dane zamiast parsowania chatu), (d) pomiaru TPS/MSPT z serwera. Dla frameworka pokrywającego *wszystkie* wymienione zastosowania — companion-plugin jest praktycznie obowiązkowy. To też najczystszy kanał zwrotny dla wyników load-testu.

---

## Recommendations

**Etap 0 — fundament (integralność + stabilność):**
1. Postaw **klon stagingowy** Paper 1.21.11 z pełnym stackiem pluginów, **bez danych graczy**, na izolowanej sieci. Whitelist + grupa LuckPerms `bot` bez OP + region WorldGuard sandbox + automatyczne backupy FAWE.
2. Zbuduj `bot-core` na `mineflayer` 4.37.1 (Node 22) z **warstwą bezpiecznego parsowania**: global error handling, `hideErrors:false`+logi, auto-reconnect, mała `viewDistance`.
3. **Benchmark stabilności:** bot łączy się i utrzymuje sesję 30 min przy aktywnych ModelEngine/LibsDisguises/MythicMobs bez fatalnego `PartialReadError`. **Próg zmiany decyzji:** jeśli parser pada mimo obsługi błędów → izoluj winny plugin (sekcja D) i rozważ downgrade jego wersji lub fork parsera protodef.

**Etap 1 — QA (najwyższa wartość, najniższe ryzyko):**
4. Moduł odczytu NBT (`item.nbt` + `nbt.simplify()['minecraft:custom_data']`) i asercje MMOItems. Zweryfikuj empirycznie na stagingu, że `MMOITEMS_ITEM_ID` faktycznie dochodzi (potwierdź, że operator nie włączył obfuskacji `custom_data`).
5. `mineflayer-gui` 4.0.2 + `window.requiresConfirmation = false` do testów DeluxeMenus.
6. Smoke-test po restarcie + regresja itemów. Companion-plugin (Poziom 2) dla stanu MMO i TPS.

**Etap 2 — multi-bot / load-test:**
7. Orchestrator spawnujący **osobne procesy**; adaptuj StressTest-MC (YAML) lub tino964MC. Pomiar TPS/MSPT przez `/spark` lub companion-plugin. **Próg:** jeśli chunki przestają się ładować pod obciążeniem → sprawdź build PacketEvents (regres na 1.21.10 build 113).

**Etap 3 — gameplay-agent (najwyższe ryzyko utrzymania):**
8. Pathfinding + walka. Traktuj `mineflayer-pvp`/`tool`/`statemachine` jako komponenty ryzykowne — przypnij wersje, testuj, przygotuj forki. Własna FSM zamiast porzuconego `mineflayer-statemachine`.

**Etap 4 — warstwa LLM (na końcu):**
9. yuniko MCP (2.0.4) i Mindcraft jako **osobne konta/procesy**. Mindcraft w Dockerze, `allow_insecure_coding:false`, **nigdy na produkcji**. Integracja z magistralą przez cienki `mcp-adapter` owijający Twoje skille, nie przez kod yuniko.

**Kolejność zależności:** bot-core → safe-protocol → skills → command-bus → (QA | multi-bot) → gameplay → LLM. QA i multi-bot można rozwijać równolegle po command-bus.

## Caveats
- **Datowanie wersji pluginów jest niespójne** między npm a profilami autorów (`mineflayer-pvp` 1.3.2: npm „4 lata" vs profil „3 lata"). Traktuj wszystkie pluginy gameplay jako przestarzałe i zweryfikuj kompatybilność empirycznie na 1.21.11 przed produkcyjnym użyciem.
- **`PartialReadError` jest zależny od wersji i konfiguracji** — konkretny winny plugin/pakiet trzeba ustalić izolacyjnie na Twoim stacku; podane Issue (#3756, #3669, #3526) to precedensy, nie gwarancja identycznego zachowania.
- **`1.21.11` to nietypowy string wersji** — README Mineflayer go wymienia, ale część dokumentacji (prismarine-viewer, część źródeł Paper) jest „rolling"/nieprzypięta do tej wersji; zawsze waliduj na realnym buildzie.
- **Format NBT MMOItems zmienia się z aktualizacjami** — testy regresji itemów muszą być wersjonowane razem z wersją MMOItems, a asercje pisane defensywnie.
- **Nazwy `MMOITEMS_ITEM_TYPE` i `MMOITEMS_<STAT>`** to konwencja potwierdzona pośrednio; dosłownie w źródłach potwierdzono tylko `MMOITEMS_ITEM_ID`. Zweryfikuj rzeczywiste klucze przez `nbt.simplify(item.nbt)` na żywym itemie z Twojego serwera.
- **Domyślne wartości konfiguracji Paper** (obfuskacja `false`) mogą być nadpisane przez operatora — jeśli QA NBT nie działa, sprawdź `paper-global.yml` (`anticheat.obfuscation.items`).
- **`prismarine-viewer` nie renderuje custom modeli** — do QA wyglądu ModelEngine/Nexo/MythicHUD potrzebny realny klient w spectator z resource packiem.
