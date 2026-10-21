# MinionCore MVP

MinionCore establishes the WorldGuard feature gate required before future minion
PvP can exist. Its in-memory lifecycle core owns temporary group IDs, owner and
entity indexes, 3-per-owner / 30-global allocation, all-or-nothing spawn
rollback, and idempotent cleanup.

The current slice supplies a direct MythicMobs 5.11.2 adapter and safe
`minion.melee.tier1`, `minion.ranged.tier1`, and `minion.support.tier1`
definitions. Entities start `NoAI` with PDC state
`PROVISIONAL`; they become `ACTIVE` and enable AI only after the registry has
committed every entity index.

`/minion summon <melee|ranged|support> 3` creates a 60-second lifecycle group. Technical
controls are `/minion follow`, `/minion hold`, `/minion attack`, and
`/minion dismiss`; `attack` accepts loaded hostile monsters only. A single
central main-thread task services deterministic target/owner-distance/scan/
cleanup buckets with 5/10/20/40 tick maxima. It never loads chunks and scans at
most 16 eligible candidates per group. FOLLOW and HOLD currently establish
group order and target behavior only; movement recovery is intentionally not in
this slice.

Ranged projectile cooldowns and support buffs run through that same central
scheduler. Ranged minions do not shoot beyond 25 blocks and make a short
retreat when their shared target is closer than four blocks. Support buffs only
the online owner and the known members of its own group.

Player PvP is region-gated: owner, minion, and target must independently pass
WorldGuard `pvp` and `minion-pvp`. The guard handles only standard Bukkit melee
and projectile damage, blocks owner/same-owner friendly fire, and applies a
0.35 multiplier to permitted player hits. A conservative compatibility preflight
disables player targeting when another HIGHEST/MONITOR damage listener is already
present. Party/guild/duel/war relationships and AoE/indirect mechanics remain
out of scope.

`MinionCorePlugin#onLoad()` registers `new StateFlag("minion-pvp", false)`. A
pre-existing flag is accepted only if it is already a `StateFlag`; a late
registry lock, missing flag reference, or a type conflict stops plugin loading.

Build and structural verification:

```bash
./tools/verify-worldguard-gate.sh
```

The helper compiles in the pinned local `eclipse-temurin:21-jdk` image because
this workspace host exposes a JRE but not `javac`; it uses the source server's
resolved Paper runtime-library classpath. Before compiling, it checks
SHA-256 parity for Paper API, WorldGuard 7.0.15, and MythicMobs 5.11.2 between the
source server and Docker `world` backend. The output jar is checked to ensure it
does not contain WorldGuard, MythicMobs, or Bukkit classes.
It also runs contract tests for fresh registration, compatible collision,
incompatible-type collision, and late-registration refusal; lifecycle tests for
spawn failures at positions 1/2/3, quotas, duplicate death/remove, disable
during spawning, activation rollback, and owner/expiry cleanup; and scheduler
topology tests for 30×1 and 60×1 groups plus 60-second expiry.

For an opt-in boot smoke test on an idle `world` backend:

```bash
./tools/verify-worldguard-gate.sh --boot
```

It refuses to restart an already running backend, installs the jar only for the
test, waits for the `minion-pvp` registration log, then stops the backend and
restores the previous plugin jar state.
The boot helper also invalidates Paper's filename-based remap cache before
loading a changed jar.

Runtime-performance evidence is intentionally separate from this safe boot
smoke. It requires a controlled idle `world` window and Velocity-connected
players for the baseline, 10x3, 20x3, and 60x1 profiles; do not restart an
active shared backend merely to collect it.

For stress-only staging, place `test-profile.yml` in the MinionCore data folder
with `enabled: true`, `global-limit: 60`, `min-summon-amount: 1`,
`max-summon-amount: 1`, and
`duration-seconds: 300`. This profile is never bundled in the jar and must be
removed after the run; absent/disabled means the production defaults 3/30/60s.

Every 1200 ticks MinionCore logs bounded Paper tick telemetry (average/p95/max
MSPT and sample count) alongside scheduler timings. This is collected directly
from the main-thread callback and does not depend on Spark output over RCON.

WorldGuard reference: https://worldguard.enginehub.org/en/latest/developer/regions/custom-flags/
