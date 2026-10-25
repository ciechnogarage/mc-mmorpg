# Akt 1: notatka stanu (czytaj NAJPIERW po przerwie / w nowej sesji)

Plan, decyzje i dziennik: encyklopedia, zakładka „Akt 1: plan budowy”
https://claude.ai/artifact/XhnUM8PY5q6C6m5xDfXoMi (nie powielaj go tutaj).
Gałąź: `akt1` (worktree `MC/.claude/worktrees/akt1`).

## Polecenie właściciela (25.10)
„to zróbmy na razie bez fabuły tylko zróbmy mniej więcej kontentu na 1 akt i 1 wyspe i questy i reszte zbierz…”
→ „4. a b c to przygotuj plan na to wszystko co tu dałeś i pracuj aż do zrobienia”.
Fabuła = a (świat żyje sam) + b (gracze piszą historię, Kronika) + c (Mistrz Gry na żywo).
Otwarte pytania 1, 2, 3, 5 (Spis): pracuję na rekomendacjach (B: 4 lochy na start; karty akt1-v2 = mechanika bez historii;
tor 6 startuje teraz; wycinek przed resztą wyspy) — właściciel może zmienić.

## Etapy (szczegóły w zakładce)
| Etap | Stan |
|---|---|
| 0 dług | Stolica: robi INNA sesja (klon `stolica-archipelag`, 25.10) — nie ruszać. Loch 001 gamerule, wdrożenie Lochu 002: czekają |
| 1 silnik zadań | ZAKOŃCZONY 25.10: BetonQuest 3.2.0, wariant B + MostNpc (wyniki niżej) |
| 2 rozpiska wycinka | w zakładce (W1/W2, pytanie w Spisie; bez odpowiedzi W1) |
| 3 moby | czeka |
| 4 wycinek | czeka |
| 5 reszta | czeka |
| 6 baza + serwer wyspy | rozpoznanie: proxy-based profiles MMOProfiles (`docs/ref/mmoprofiles/proxy-based.md`) |
| 7 wydanie | czeka |

## Ustalenia techniczne (ze źródłem)
- Postęp osobno dla każdej postaci w KAŻDYM pluginie = MMOProfiles proxy-based profiles (UUID postaci przy przejściu
  między serwerami; wymaga wspólnego MySQL, PacketEvents+MMOProfiles na Velocity, FreedomChat, enforce-secure-profile=false).
  BetonQuest jest na liście przetestowanych przez autorów. Źródło: `docs/ref/mmoprofiles/proxy-based.md`.
  → silnika zadań nie wybieramy pod kryterium „per postać”; rozwiązuje to tor 6.
- BetonQuest + MythicMobs NPC: `docs/ref/betonquest/mythicmobs-npc.md` (gałąź `ref-betonquest`): ryzyka = NPC z modelem ME
  (MythicMobInteractEvent), koszt EntityMoveEvent.

## Etap 1: wyniki (klon akt1-zadania, 25.10, bot bez OP) — ZAKOŃCZONY, wybrany wariant B
- BQ 3.2.0 startuje czysto, integracje MM/MMOItems/MMOCore/packetevents/Nexo włączone. MythicDungeons: „Incompatible BetonQuest version”
  → zadania z lochów przez mmobkill/location/command.
- **Wariant B**: NPC zadań = moby MM POZA rejestrem BQ (pakiet bez `npcs:`); rozmowę startuje skill
  `command{c="bq action <trigger.name> <pakiet>><akcja>"} @self ~onInteract` (`test-betonquest/AKT1_test.yml`).
  Powód: przy NPC typu mythicmobs BQ słucha KAŻDEGO ruchu każdego moba MM (EntityMoveEvent; javap, README most-npc);
  pomiar MSPT (~95 mobów, średnia 1 min): z BQ+NPC mythicmobs +2,3/+3,5 ms, bez BQ +0,9/+1,8 (szum duży).
  Minus: bez funkcji BQ przypiętych do NPC (ukrywanie NPC, warunki npc) — zastępujemy warunkami w skillu/akcjach.
- NPC z modelem ME wymaga pluginu `MostNpc` (`MCMMORPG/plugin-src/most-npc`, README): bez niego 0/2, z nim 2/2.
- `zadanie.js` (rozmowa dialog → zabij 3 → zbierz 2 → dojdź → nagroda, journal, NPC z modelem styl chest): **18/18 dwa razy pod rząd**.
- `diag-koniec.js`: BQ trzyma gracza w rozmowie ~2,5–6 s po ostatniej kwestii bez odpowiedzi (2/2, wolny po 6 s);
  zgodne z `conversation.interceptor.delay: 50` w config BQ, przyczyny nie udowodniono. Kolejne NPC tuż po rozmowie = „in conversation”.
- Pułapki testu: bot klika `activateEntityAt` (MM `~onInteract` = PlayerInteractAtEntityEvent); `difficulty peaceful` usuwa cele-zombie;
  odłożony jar BQ + `klon.sh restart` = BQ wyłączony (fałszywe FAIL); scena sprzątana dopiero z botem w świecie; separator pakietu `>`.

## Następny krok
1. Commit etapu 1 (+ PR draft). 2. Etap 2: beat chart wycinka W1 (bez fabuły). 3. Tor 6: projekt testu proxy-based profiles.
4. Klon `akt1-zadania` USUNIĘTY 25.10 (odtworzenie: klon.sh start, jar BQ 3.2.0 z modrinth wg docs/ref/betonquest/README.md + jar most-npc z build.sh, AKT1_test.yml do MythicMobs/mobs, QuestPackages/akt1test, restart, difficulty easy).
