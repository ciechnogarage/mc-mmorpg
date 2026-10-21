---
title: Performance QA Rule
---

# Performance QA Rule

Every spawn, load, or gameplay QA run must measure performance during the same
run, not afterwards.

Required evidence:

- TPS/MSPT or Paper tick telemetry;
- container CPU/RAM;
- active player count;
- active entity count for the tested feature;
- plugin scheduler/service timing;
- cleanup count after players disconnect or the test expires.

Stop condition:

- lag spike;
- watchdog;
- timeout;
- unexpected entity loss;
- SLO breach;
- cleanup leak.

When a stop condition appears, stop expanding test scope, diagnose and fix the
root cause, then rerun the affected profile from the smallest reproducing case.

Functional success without performance evidence is not enough for systems that
spawn entities, run schedulers, pathfind, target, or process combat.
