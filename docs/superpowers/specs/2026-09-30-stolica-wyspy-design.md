# Stolica Wyspy — projekt wdrożenia na world1

## Cel

Gracz po zakończeniu kreatora trafia do bezpiecznej, fizycznej Stolicy Wyspy. Miasto łączy podróż do działających lochów, dostępne profesje, handel, Wiary i orientację w dalszej grze. Nie modyfikuje istniejącej mapy `world` ani przebiegu kreatora.

## Źródła i aktualizacje

Układ wynika z `~/Desktop/Przemek/MC/docs/ai/city-hub-blockout-v0.0.1.md` i `stolica-wyspy-hub-foundation-v0.0.1.md`. Aktualny world1 wybiera rasę i klasę w kreatorze, więc miasto nie oferuje ponownego wyboru klasy na poziomie 10. Działające usługi potwierdza konfiguracja `plugins/DeluxeMenus/gui_menus/`, `plugins/MythicDungeons/maps/loch_001`, `plugins/CoreTools/Shops/`, `plugins/MMOCore/professions/` i `plugins/Wiary/`.

## Topologia

Osobny świat Multiverse `stolica` z generatorem VoidGen, na tym samym serwerze world1. Wyspa ma około 110 bloków średnicy. Spawn na południu patrzy wzdłuż ulicy na centralny Portal Nexus. Rynek stoi na zachodzie, profesje na wschodzie, sala postaci na północnym zachodzie, tablica zadań na północnym wschodzie, świątynia na północy. Widoczne chodniki łączą miejsca bez obowiązkowego korytarza. Brzeg jest zabezpieczony balustradą, a cały obszar jest safe zone.

## Usługi

- Portal Nexus: MythicMobs NPC otwiera dedykowane menu lochów. Loch 001 używa obecnej komendy `md play loch_001 <gracz>`; przyszłe lochy są oznaczone jako niedostępne. Przejście na osobne wyspy pozostaje niedostępne do czasu uruchomienia serwera wysp i synchronizacji postępu.
- Rynek: osobne NPC otwierają obecne `core-shops sklep`, `core-shops skup` i `ah mmoitems_example stolica`.
- Profesje: NPC otwiera `profession_main`; warsztaty mają osobne budynki i czytelne oznaczenia. Nie przyznają automatycznie doświadczenia ani przedmiotów.
- Świątynia: NPC otwiera istniejące menu `wiara`.
- Sala postaci: NPC otwiera obecne menu `hub_postac` lub umiejętności, bez zmiany klasy.
- Tablica informacyjna wskazuje Nexus i usługi, a zadania otwiera przez istniejące menu. Nie teleportuje wprost do lochu.

NPC są definiowane w MythicMobs, a ich odtwarzanie ma jawne spawnery. Nie są wpisane na stałe w szablon terenu.

## Przejście i bezpieczeństwo

`creator-room.exit` w CharacterCreator oraz `new-profile-spawn-point` i `profile-selection-location` w MMOProfiles zostały zmienione razem po kontroli gotowej wyspy. Punkt wyboru profilu jest na bezpiecznym placu przy wejściu. WorldGuard blokuje PvP, eksplozje i budowanie w mieście. Reguła `minecraft:spawn_mobs=false` wyłącza naturalny spawn mobów wyłącznie w świecie `stolica`, dopuszczając kontrolowane NPC MythicMobs. Budowa ma własny skrypt/struktury oraz kopię konfiguracji w `backups/stolica-20260930/`.

## Weryfikacja

Sprawdzić świat i bloki w miejscu spawnu, ciągłość ulic, balustradę, ochronę WorldGuard, obecność NPC po restarcie, otwieranie menu u każdego NPC, wejście i wyjście Lochu 001, a następnie pełne przejście kreator → stolica. Niedostępne wyspy i lochy nie mogą uruchamiać pozornych teleportów.
