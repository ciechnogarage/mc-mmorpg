# MostNpc

PPM gracza w model ModelEngine na mobie MythicMobs uruchamia `~onInteract` tego moba. Zależności: `MythicMobs` (5.11.2), `ModelEngine` (4.0.9).
Kod: `src/main/java/pl/mcmmorpg/mostnpc/MostNpc.java`.

## Po co
NPC zadań Aktu 1 to moby MythicMobs, a rozmowę BetonQuesta startuje ich skill `~onInteract` (`command{c="bq action <trigger.name> <pakiet>><akcja>"}`).
NPC z modelem ModelEngine nie reagował na PPM, bo klik w hitbox modelu idzie jako `com.ticxo.modelengine.api.events.BaseEntityInteractEvent`,
a ME przekazuje do MM tylko `DISMOUNTED` (javap ME 4.0.9, skill `mc-mythicmobs/SKILL.md`, „PPM w mob z modelem”). W efekcie `MythicMobInteractEvent` nie powstawał.
Plugin słucha `BaseEntityInteractEvent` (MONITOR, bez ATTACK) i sam wysyła `new MythicMobInteractEvent(gracz, ActiveMob)`, jeden na klik.
Jeden PPM klienta to INTERACT_AT + INTERACT, a czasem też druga ręka, dlatego obowiązuje okno 250 ms na gracza.

## Wpływ na world1 i inne mosty
- Na world1 (25.10) 0 z 218 mobów MythicMobs ma jednocześnie `model{` i `~onInteract` (parser tekstowy `plugins/MythicMobs/mobs/*.yml`),
  więc wdrożenie MostNpc nie zmienia zachowania żadnego istniejącego moba — działa dopiero dla nowych NPC zadań z modelem.
- Most Chowańców (`chowance/.../integracja/MostModelEngine.java`) obsługuje tylko własne reguły Chowańców (interakcje, łapanie, zagroda)
  i NIE wysyła `MythicMobInteractEvent` — MostNpc go nie dubluje i nie koliduje.

## Dlaczego nie NPC typu `mythicmobs` w BetonQueście (wariant A)
BQ 3.2.0 przy NPC `mythicmobs` w dowolnym pakiecie rejestruje `MythicMobsInteractCatcher.onMove(EntityMoveEvent)`, który przy każdym ruchu każdego moba MM
robi `isMythicMob`, potem `getMythicMobInstance` i woła `NpcVisibilityUpdateEvent` (javap; rejestracja w `MythicMobsIntegrator$DynamicListenerRegister`
tylko gdy `MythicMobsNpcFactory.createdAnIdentifier()`). Pakiety Aktu 1 nie mają sekcji `npcs:`, rozmowa startuje z `~onInteract`, więc tego nasłuchu nie ma.

## Testy (klon, bot bez OP; `narzedzia/bot/run.sh <klon> <plik>`)
Pliki w `docs/akt1/test-betonquest/` (repo MC, gałąź `akt1`):
- `zadanie.js`: całe zadanie testowe (rozmowa dialog, zabij 3, zbierz 2, dojdź, nagroda, NPC z modelem i rozmowa chest). 25.10: 18/18 dwa razy pod rząd.
- NPC z modelem bez MostNpc: 0/2 (czysta scena); z MostNpc: 2/2 (25.10). Wcześniejsze „PASS” bez mostu były fałszywe: bot klikał stare villagery bez modelu.

## Budowa i wdrożenie
`./build.sh`: gradle w kontenerze, jary MM/ME z `/opt/docker/mc/data/world1/plugins` tylko do odczytu. Wynik: `build/libs/most-npc-0.1.0-<rrrrmmddhhmm>.jar`.
Wdrożenie: jeden jar do `plugins/` i restart (dwa jary = „Ambiguous plugin name”). Cofnięcie: usuń jar i zrestartuj. NPC z modelem przestają wtedy reagować na PPM,
NPC bez modelu działają dalej. Stan: tylko klon (25.10), na world1 NIE wdrożony.
