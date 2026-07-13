# server.properties — standard for this MMO server

Scope: the live/staging Paper server in `MCMMORPG/` (MC 1.21.11, Java 21,
`max-players=20`, heavy plugin stack: MMOCore/MMOItems/MythicMobs/ModelEngine/
WorldGuard/LuckPerms/HuskSync/Iris).

This is a **standard + decision log**, not an auto-applied change. Editing the
live `server.properties` is a gated action (see `CLAUDE.md` safety boundaries):
propose → get approval → apply → restart → scan `logs/latest.log`. Worked out by
Claude (lead) with Codex (peer review), 2026-06-19.

Each item carries its own severity label. Status: **CHANGE** = recommended edit,
**KEEP** = deliberate, do not "tidy" away, **DECIDE** = gameplay call for the
owner. Apply order by real impact: #2 (allow-flight) and #3 (simulation-distance)
first, then #4, #5, #1.

## 1. CHANGE / HYGIENE — inert `management-server-secret`

```
management-server-enabled=false
management-server-secret=<populated 40-char value>
```

The MC *management server* (1.21.9+) is a remote admin API (TLS/JSON-RPC). The
feature is **off** and the 40-char secret is populated in the working file.

**Verified 2026-06-19 — version control is clean:** `.gitignore:23` ignores
`MCMMORPG/server.properties` and the secret string appears in **no** tracked
file. So this is *not* a committed-secret leak (an earlier draft of this doc
wrongly claimed it was committed in `f3a6856` — corrected). The gitignore is
doing its job; keep it that way.

Remaining, low-severity hygiene:

- The secret is a live credential sitting unused in a config file. Since the
  feature is off, blank it (`management-server-secret=`) and only generate a
  fresh one *if/when* the management server is enabled.
- Never let `server.properties` become tracked. If the ignore rule is ever
  removed, the secret (and any future RCON password) would leak.
- `rcon.password=` empty with `enable-rcon=false` is fine (RCON is off). Do not
  populate an RCON password unless RCON is enabled, and never let it be tracked.

Codex (peer) pushed the "leaked secret → immediate rotation" override; on
inspection the leak premise didn't hold, so this drops from SECURITY to HYGIENE.
Lesson logged: verify the claim (`git ls-files` / `git grep`) before escalating.

## 2. CHANGE / GAMEPLAY-BREAKING — `allow-flight`

```
allow-flight=false   →   allow-flight=true
```

With MMO movement/dash/levitation/leap abilities (MMOCore + MythicMobs skills),
the vanilla anti-fly check kicks players ("flying is not enabled") during
legitimate skill use. Anti-cheat must come from the plugin layer, not this flag.
High player-facing impact; low risk (plugins gate real cheating).

## 3. CHANGE / PERFORMANCE — `simulation-distance`

```
simulation-distance=10   →   6  (test 6–8)
view-distance=10         →   KEEP
```

`simulation-distance` is the expensive lever: it controls entity ticking, mob
AI, spawners, redstone. With heavy ModelEngine entities + MythicMobs this
dominates tick load far more than `view-distance` (which is mostly chunk-send
cost to clients). Lower simulation to 6 (try 6–8), keep view at 10 so players
still *see* far. Validate with `/tps` / Spark after the change.

## 4. CHANGE / HUB UX — `spawn-protection`

```
spawn-protection=16   →   0
```

Vanilla spawn-protection blocks non-op block interaction within N blocks of world
spawn and can silently break NPC/interactable/region behavior in a built MMO hub.
This stack already has WorldGuard — protect spawn with a region instead (granular,
plugin-managed, consistent with how everything else is controlled). Set to `0`
and delegate.

## 5. DECIDE / GAMEPLAY — `difficulty`

```
difficulty=easy
```

`easy` softens vanilla mob damage/spawn/hunger. MythicMobs drive real encounter
difficulty, so this mostly affects vanilla mobs and hunger pacing. For an RPG
combat server `normal` (or `hard`) is the usual intent. Owner's call.

## 6. KEEP — deliberate, do not "optimize" away

| Key | Value | Why keep |
| --- | --- | --- |
| `online-mode` | `true` | Mojang auth. Only `false` behind a Velocity/Bungee proxy that does its own auth — not the case here. |
| `enforce-secure-profile` | `true` | Signed chat / profile integrity. Keep on. |
| `sync-chunk-writes` | `true` | Crash-safe chunk I/O. With HuskSync/player data, data safety > marginal write speed. |
| `max-tick-time` | `60000` | Watchdog. Do **not** set `-1`: a real deadlock should surface as a crash, not hang forever. |
| `pause-when-empty-seconds` | `-1` | Intentional: keeps world ticking when empty so spawners/timers/scheduled events run on staging. Costs idle CPU by design. |
| `network-compression-threshold` | `256` | Sane default for WAN clients. Only raise for pure-LAN to trade bandwidth for CPU. |
| `entity-broadcast-range-percentage` | `100` | For an MMO you *want* bosses/mobs visible at range. Lower only as a last-resort perf knob. |
| `prevent-proxy-connections` | `false` | Direct online-mode server. Turning `true` can wrongly drop legit players on some ISPs; enable only to fight specific proxy abuse. |

## Apply procedure (when approved)

1. Back up current `server.properties`.
2. Apply CHANGE items above + any DECIDE the owner confirms.
3. Restart server, scan `logs/latest.log` for enable errors/warnings.
4. In-game: verify a movement skill doesn't kick (allow-flight), spawn-area
   interaction works (spawn-protection via WorldGuard), check `/tps`/Spark for
   the simulation-distance change.
5. Record the result here (observed TPS, any rollback).
