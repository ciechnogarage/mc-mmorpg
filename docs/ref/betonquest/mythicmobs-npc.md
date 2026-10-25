# BetonQuest: moby MythicMobs jako NPC (fragmenty)

Pobrane 2026-10-24. Wersja: BetonQuest 3.2.0 (ostatnie wydanie stabilne, 2026-08-17; Modrinth podaje Paper 1.18–1.21.11 i 26.1–26.2), kod z gałęzi `main`.

## Dokumentacja
URL: https://betonquest.org/3.1/Documentation/Reference/Integrations-List/NPCs/

- Obsługiwane wtyczki NPC: Citizens, MythicMobs, FancyNpcs, ZNPCsPlus. Zapis: `betonQuestNPCID: NPCSelector NPCID`.
- MythicMobs: selektor `mythicmobs`, a NPC wskazuje się na trzy sposoby:
  ```yaml
  npcs:
    mayorHans: mythicmobs UUID b18af0c3-5db7-4878-9693-05fe1b2c5a2f   # UUID z /mm listactive
    innkeeper: mythicmobs MYTHIC_MOB inkeeper                          # Internal_Name moba
    guard: mythicmobs FACTION guards                                   # frakcja MM
  ```
  MYTHIC_MOB i FACTION obejmują wiele encji naraz, np. jedną rozmowę dla wszystkich strażników albo wspólne liczenie w `npcinteract`/`npckill`.
- Ukrywanie: `hide_npcs` z warunkami, interwał `npc_update_interval` (domyślnie 5·20 ticków), wymuszenie: `updatevisibility`.
- Gdy jeden identyfikator pasuje do wielu NPC: `npcteleport` przenosi wszystkie, `npcrange` zalicza dowolny, a placeholdery NPC zwracają błąd.
- Changelog 3.0.0, sekcja Added: „`FancyNpcs`, `ZNPCsPlus` and `MythicMobs` as new options for npcs”. Od 3.0.0 wymagany jest Paper, Spigot odpada.
  URL: https://betonquest.org/RELEASE/Documentation/CHANGELOG/

## Kod (czego strona nie mówi)
Plik `code/compatibility/src/main/java/org/betonquest/betonquest/compatibility/mythicmobs/npc/MythicMobsInteractCatcher.java`:
- Prawy klik obsługuje `MythicMobInteractEvent`, czyli to samo zdarzenie MM co `~onInteract`. Ograniczenie naszego serwera: na mobie z modelem ModelEngine `~onInteract` nie odpala (skill `mc-mythicmobs`). Z tego wnioskuję, że BetonQuest też go nie zobaczy, ale tego NIE sprawdziłem testem.
- Lewy klik: `EntityDamageByEntityEvent` (LOWEST), a obrażenia zadane NPC są anulowane.
- `EntityMoveEvent` (MONITOR) dla każdego moba MM odświeża widoczność. Paper wywołuje to zdarzenie tylko wtedy, gdy ktoś go nasłuchuje, więc sama instalacja włącza je dla wszystkich ruchomych encji. Koszt trzeba zmierzyć sparkiem.
- `MythicMobsIntegrator.REQUIRED_VERSION = "5.0.0"`. Mamy 5.11.2.

## Nasz serwer
- MMOProfiles `proxy_based_profiles.enabled: false` (`plugins/MMOProfiles/config.yml:356`): wszystkie postacie gracza mają jeden UUID. Każdy plugin, który zapisuje postęp po UUID (BetonQuest i zmienne MM `save=true`), dzieli stan zadań między postaciami.
