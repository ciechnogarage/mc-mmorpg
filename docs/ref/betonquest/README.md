# BetonQuest 3.2.0: lokalna dokumentacja

- Wersja: 3.2.0 (stabilna, 2026-08-17; Paper 1.18–1.21.11). Wydania 3.3.0-DEV są dla 26.x, nie dla nas.
- Jar: https://cdn.modrinth.com/data/uVhukxY9/versions/Sfp4oOVr/BetonQuest-3.2.0.jar
  sha512 `30c500ae314338dba9d39f16801178a8052b1cecc44c1eee0f1872d9fcc1f3cf0678699bcbd13f5c533dce6c2414d38b00f44dacc6bbe5086cb7994ae465b425`
- Źródło stron: https://betonquest.org/3.2/ (pełna lista adresów w https://betonquest.org/3.2/sitemap.xml). Pobrane 2026-10-25.
  Uwaga: adresy `Documentation/Features/...` i `Documentation/Scripting/...` z wersji 3.1 w 3.2 dają 404, aktualne ścieżki są w sitemapie.
- `strony/*.txt` to tekst artykułów (bez nawigacji), jeden plik na stronę:

| Plik | URL (pod https://betonquest.org/3.2/) |
|---|---|
| Conversations.txt | Documentation/Advanced/Conversations/ |
| Journal.txt | Documentation/Advanced/Journal/ |
| Items.txt | Documentation/Advanced/Items/ |
| Packages-and-Templates.txt | Documentation/Basics/Packages-%26-Templates/ |
| Objectives-List.txt, Actions-List.txt, Conditions-List.txt | Documentation/Reference/<…>-List/ |
| MMOItems.txt, Nexo.txt | Documentation/Reference/Integrations-List/Items/<…>/ |
| MythicMobs.txt | Documentation/Reference/Integrations-List/NPCs/MythicMobs/ (akcje mcast/mspawnmob, cel mmobkill, warunek mythicmobdistance) |
| NPCs.txt | Documentation/Reference/Integrations-List/NPCs/ |
| Global-Daily-Quests.txt, Party-Quests.txt | Tutorials/Quick-Guides/Multiplayer-and-Global-Quests/<…>/ |

- `mythicmobs-npc.md`: NPC z mobów MythicMobs, fragmenty kodu i ryzyka (kopia z gałęzi `ref-betonquest`).

## Co jest nam potrzebne (ze stron wyżej)
- Przedmioty: `mmoitem <TYP> <ID> [scale] [soulbound[:n]] [quest-item]` (MMOItems.txt, min. MMOItems 6.9.4), `nexo <id> [quest-item]` (Nexo.txt).
  Cel `craft` liczy też wytwarzanie na stacjach MMOItems.
- Cele: `mmobkill <MobID[,…]> [mode:faction] amount:N [deathRadiusAllPlayers:R]`, `npcinteract <npc>`, `location <loc> <r>`,
  `pickup <items> amount:N`, `block`, `craft`, `delay`, `stage` (Objectives-List.txt, MythicMobs.txt).
- Akcje: `give/take`, `objective start|complete|delete`, `journal add|delete|update`, `tag add|del`, `point`, `notify`, `command`, `folder`, `party`, `pickrandom` (Actions-List.txt).
- Dziennik: sekcje `journal` (wpisy per gracz) i `journal_main_page` (warunkowe), `/journal` (Journal.txt).
- Rozmowy: `conversations.<id>` (`quester`, `first`, `NPC_options`, `player_options`, `pointers`), przypięcie `npc_conversations: <NpcID>: <conv>`;
  NPC reaguje tylko na PPM, LPM po `npcs.accept_left_click: true` (Conversations.txt).
- NPC z MythicMobs: `npcs: <id>: mythicmobs MYTHIC_MOB <InternalName>` (NPCs.txt, mythicmobs-npc.md).
