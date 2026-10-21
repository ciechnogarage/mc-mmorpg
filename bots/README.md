# mmorpg-bots — framework botów Mineflayer (QA / load-test / gameplay)

Monorepo (npm workspaces, Node ≥22, mineflayer 4.37.1, MC 1.21.11 offline-mode).
Boty łączą się WYŁĄCZNIE przez Velocity (`localhost:25565`); operacje
uprzywilejowane idą przez RCON (`MCMMORPG/docker/mc rcon <domena>`).

## Pakiety

| Pakiet | Rola |
|---|---|
| `bot-core` | `BotSession`: mineflayer + reconnect + spawn wait, `viewDistance: tiny` |
| `safe-protocol` | ochrona przed `PartialReadError` (ModelEngine/LibsDisguises metadata), statystyki per pakiet |
| `skills` | atomowe akcje: `items` (NBT/MMOItems), `gui` (DeluxeMenus), `chat`, `move`, `text` |
| `command-bus` | jedna magistrala dla skryptu / chatu in-game / LLM-MCP |
| `task-queue` | kolejka z priorytetami (integralność > stabilność > wydajność > gameplay) |
| `state-machine` | FSM idle→task→verify→report (zamiast porzuconego mineflayer-statemachine) |
| `mmo-bridge` | RCON (docker-mc), TPS z serwera, kanał `mmoqa:state` pod companion-plugin |
| `qa` | scenariusze, asercje, raporty PASS/FAIL/BLOCKED do `runs/` |

| App | Rola |
|---|---|
| `apps/smoke` | smoke-test po restarcie serwera |
| `apps/orchestrator` | flota N procesów (`worker-walk.js`), staggered join, auto-restart |
| `apps/bench` | benchmark stabilności protokołu (fatal/reconnect/RSS na 1 bocie) |
| `apps/loadtest` | Etap 2: flota + TPS z RCON co interwał + detekcja chunk-stalli (regres PacketEvents, plan sekcja D) |
| `apps/gameplay` | Etap 3: `combat-check.js` — dowód realnego zabójstwa przez `skills.combat` (nigdy RCON `/kill`) |
| `apps/mcp-adapter` | Etap 4: serwer MCP (stdio) owijający bot-core+skills — jeden bot=jedno konto=jeden proces |

## Użycie

```bash
cd ~/projects/MC/bots && npm install

# smoke-test po restarcie (backend items musi działać: MCMMORPG/docker/mc up items)
npm run smoke -- --domain items --mmo-type SWORD --mmo-id LONG_SWORD --menu foundation --menu-title nexus

# flota bez pomiaru TPS (N osobnych procesów, staggered join)
npm run fleet -- --count 5 --join-delay 3200

# benchmark stabilności protokołu (world = najwięcej metadata z ModelEngine)
npm run bench -- --domain world --minutes 30 --view 8

# load-test: flota + TPS z RCON co interwał + detekcja chunk-stalli
# (backend musi być up: MCMMORPG/docker/mc up world)
npm run loadtest -- --count 5 --domain world --minutes 5 --tps-interval 30

# combat-check: realna walka (RCON tylko do setupu — give miecza + summon moba)
npm run combat-check -- --domain world --mob zombie --timeout 45000
```

**Pułapka join-delay:** Velocity ma `login-ratelimit = 3000` (velocity.toml).
Poniżej tego (np. 750ms) boty dostają kick `"logging in too fast"` na starcie
i restartują się — wygląda jak niestabilność frameworka, a to throttling
proxy. `apps/loadtest` domyślnie używa 3200ms; `apps/orchestrator` (fleet)
nadal ma stary domyślny 750ms — jeśli zobaczysz masowe restarty na starcie
floty, podnieś `--join-delay`.

**Kryteria load-testu (`apps/loadtest`):** TPS (RCON `/tps`, Poziom 0 z
sekcji J — nie ma jeszcze companion-pluginu/`/spark`) < `--tps-fail`
(domyślnie 15) w dowolnej próbce ⇒ FAIL; < `--tps-warn` (18.5) ⇒ ostrzeżenie
informacyjne. Bot poruszający się (moved≥2 przez 3 kolejne próbki co ~10s),
u którego liczba chunków nie rośnie w tym oknie ⇒ zdarzenie "chunk-stall"
(plan sekcja D, objaw #3 — regres PacketEvents 1.21.10 build 113); ≥3 takich
zdarzeń u jednego bota ⇒ FAIL, mniej ⇒ tylko w raporcie. Trwały ubytek floty
(bot wyczerpał `--max-restarts`) ⇒ FAIL.

## Reguła korelacji QA↔protokół

Krok scenariusza deklaruje `relevantPackets`; recoverable drop (PartialReadError)
takiego pakietu w oknie asercji ⇒ krok `INCONCLUSIVE` ⇒ werdykt `BLOCKED`
(dropniętego pakietu nie da się odróżnić od „brak zdarzenia" — to nie jest PASS).

Kryteria PASS benchmarku: 0 fatal, 0 reconnect, recoverable nierosnące
monotonicznie, RSS ≤ 1.5× bazy, sanity-probe encji bot vs RCON (raportowane).

Env: `MC_HOST`, `MC_PORT`, `MC_VERSION`, `MC_DOCKER_DOMAIN`, `MC_DOCKER_MC`.

## Walka melee (`skills.combat`, Etap 3)

`mineflayer-pvp` i `mineflayer-statemachine` są porzucone (lata bez update) —
zamiast nich: `mineflayer-pathfinder` (`GoalFollow`, utrzymywany) do zbliżenia
+ własna pętla `bot.attack()` z cooldownem, opakowana w
`@mmorpg-bots/state-machine` FSM (`idle→engage→attack→flee|report→idle`).

```js
const { combat } = require('@mmorpg-bots/skills');
const target = combat.findNearestHostile(bot, { radius: 16 });
const outcome = await combat.meleeAttack(bot, target, { maxDurationMs: 30000 });
// outcome.result: 'killed' | 'fled' | 'lost-target' | 'timeout'
```

`outcome.result === 'killed'` oznacza wyłącznie realną redukcję HP celu do
zera przez `bot.attack()` — moduł nigdy nie używa RCON `/kill` (patrz
`apps/gameplay/combat-check.js`, który to dowodzi na żywym mobie).

## Odczyt itemu MMOItems

```js
const { items } = require('@mmorpg-bots/skills');
items.getMMOItemId(bot.heldItem);      // 'LONG_SWORD'
items.getMMOStats(bot.heldItem);       // wszystkie tagi MMOITEMS_*
```

Uwaga: format NBT MMOItems zmienia się między wersjami — snapshoty regresji
wersjonować razem z wersją pluginu. `prismarine-item`/`prismarine-nbt` przypięte
przez `overrides` w root package.json; kontrakt kształtu komponentów pilnowany
krokiem smoke (`items.assertComponentContract`).

## MCP adapter (Etap 4)

Cienki serwer MCP (stdio) owijający bot-core+skills jako narzędzia
(`bot_connect`, `bot_status`, `bot_goto`, `bot_attack_nearest`, `bot_chat`,
`bot_wait_for_message`, `bot_read_held_item`, `bot_open_menu`,
`bot_find_slot_by_name`, `bot_click_slot`, `bot_disconnect`). Świadomie **nie**
wstrzykuje kodu yuniko/Mindcraft — to własny serwer dla własnego konta bota;
jeden proces = jeden bot, zgodnie z regułą "osobne konto per agent" (plan
sekcja G).

Podłączenie w kliencie MCP (np. Claude Code, `.mcp.json`):

```json
{
  "mcpServers": {
    "mmorpg-bot": {
      "command": "node",
      "args": ["apps/mcp-adapter/server.js"],
      "cwd": "/home/przemek/projects/MC/bots"
    }
  }
}
```

**Bezpieczeństwo:** to narzędzie daje klientowi LLM realną kontrolę nad kontem
w grze (ruch, walka, chat, GUI/transakcje). Uruchamiaj wyłącznie na stagingu,
kontem bez OP na whiteliście — nigdy na produkcji z danymi graczy (plan
sekcja G, [[mc-death-trigger-real-kill]] — `bot_attack_nearest` zabija
wyłącznie realnymi trafieniami, nie administracyjnie).

Zweryfikowane end-to-end 2026-07-14 (klient MCP przez stdio, żywy backend
`world`): `bot_connect`→`bot_status`→`bot_goto` (pathfinder faktycznie
doszedł)→`bot_attack_nearest` (poprawnie `no-target` bez wrogów w
zasięgu)→`bot_read_held_item`→`bot_disconnect`.

## Bezpieczeństwo topologii (zweryfikowane 2026-07-13)

- Tylko Velocity publikuje port na host; backendy wyłącznie w sieci `mcnet`.
- Forwarding `modern` + wspólny sekret (velocity.toml ↔ paper-global.yml).
- RCON niepublikowany — dostęp przez `docker exec rcon-cli` (`./mc rcon`).
- Ta topologia jest dev-only na localhoście; przed publicznym IP wymagana
  osobna rewizja (offline-mode!).
