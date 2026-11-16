# Konfiguracja stref PvP

System walki jest aktywny dla PvE w całym świecie. PvP włącza się tylko w regionach WorldGuard z flagą `pvp: allow`.

## Arena

Po zaznaczeniu obszaru WorldEdit:

```text
/rg define arena_pvp
/rg flag arena_pvp pvp allow
/rg flag arena_pvp greeting &cArena PvP: walka aktywna
```

Dla miasta lub spawnu:

```text
/rg define safe_spawn
/rg flag safe_spawn pvp deny
/rg flag safe_spawn greeting &aStrefa bezpieczna
```

Jeśli regiony się nakładają, arena powinna mieć wyższy priorytet:

```text
/rg setpriority arena_pvp 10
/rg setpriority safe_spawn 20
```

MMOCore zachowuje ochronę członków party i sojuszników gildii przed friendly fire. Poza `arena_pvp` skille ofensywne gracz–gracz są odrzucane przez reguły interakcji.

## Test ręczny

1. Zwykły atak zużywa staminę, a prawy przycisk uruchamia cięższy atak i zużywa jej więcej.
2. Cztery aktywne sloty uruchamiają `DEEP_WOUND`, `FIREBALL`, `ICE_SPIKES` i `EARTHQUAKE`; piąty uruchamia `HUMAN_SHIELD`.
3. Dwóch graczy może walczyć w `arena_pvp`, ale nie poza nią.
4. Członkowie party i sojusznicy gildii nie zadają sobie obrażeń.
5. Ogień, woda, ziemia i powietrze są redukowane przez odpowiednie odporności; krwawienie skaluje się z fizycznym pancerzem, a trucizna z odpornością ziemi.
