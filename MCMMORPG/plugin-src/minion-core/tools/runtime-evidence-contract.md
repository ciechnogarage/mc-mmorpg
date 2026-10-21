# MinionCore runtime evidence contract

Run MinionCore load profiles only on an isolated MinionCore backend through
Velocity. Do not run MinionCore spawn/SLO profiles on shared `world`, `items`,
or `hub`.

Before each profile, prove:

- no unrelated bot/load process is running;
- target backend has zero players;
- target backend has zero leftover MinionCore entities;
- temporary test profile is scoped only to the isolated backend;
- rollback/removal step is known.

Each profile — baseline, `10x3`, `20x3`, `60x1` — must collect performance
evidence during the same run:

- Paper TPS/MSPT or Paper tick telemetry;
- Docker CPU/RAM for backend and Velocity;
- player count;
- minion entity count;
- MinionCore scheduler/service timing p95 and max;
- cleanup count after disconnect/expiry.

SLO:

| Signal | 10x3 limit | 20x3 / 60x1 limit |
| --- | ---: | ---: |
| MSPT avg / p95 / max | 40 / 45 / 55 ms | 45 / 50 / 65 ms |
| Scheduler p95 / max | 0.5 / 2 ms | 1 / 3 ms |
| Service delay target / owner / scan / cleanup | <= 5 / 10 / 20 / 40 ticks | <= 5 / 10 / 20 / 40 ticks |
| Cleanup | 0 minion entities, registry allocations, tasks | same |

Stop expanding test scope immediately when any of these appears:

- lag spike or watchdog;
- client timeout or keepalive failure;
- unexpected entity loss;
- minion count below expected live count;
- scheduler or MSPT SLO breach;
- cleanup leaves players, entities, groups, tasks, or reservations.

When a stop condition appears, diagnose and fix the root cause first, then rerun
from the smallest reproducing profile before continuing to larger profiles.

Attach runtime evidence, scheduler histogram, server log excerpts, entity/group
counts, container stats, and final cleanup proof.

This is a report template, not runtime evidence. Mark complete only after the
above artifacts come from an isolated backend execution.
