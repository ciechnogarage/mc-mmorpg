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
| 1 silnik zadań | w toku: test BQ 3.2.0 na klonie 25.10 — wyniki niżej |
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

## Etap 1: wyniki (klon akt1-zadania, 25.10 03:24–03:29, bot bez OP)
- BQ 3.2.0 startuje czysto, integracje MM/MMOItems/MMOCore/packetevents/Nexo włączone; pakiet `akt1test` wczytany bez błędów.
- MythicDungeons: „Incompatible BetonQuest version” → integracja MD↔BQ wyłączona (zadania z lochów przez mmobkill/location/command).
- NPC z modelem ME (`AKT1_NPC_MODEL`, styl chest): PPM bota (INTERACT_AT+INTERACT) → okno rozmowy, tag ustawiony: PASS.
- Styl `dialog` (Paper Dialog) działa: `bq action <nick> akt1test>rozmowa` → show_dialog „Sołtys Testowy”, przyciski „1. Pomogę.”, „2. Nie teraz.”.
- FAIL: PPM w NPC BEZ modelu (`AKT1_NPC_PROSTY`, VILLAGER NoAI) nie startuje rozmowy; debug BQ (`bq debug true`) nie loguje żadnej interakcji.
  BQ łapie PPM przez `MythicMobInteractEvent` (mythicmobs-npc.md:24). Hipoteza (NIE sprawdzona): PPM w villagera otwiera/anuluje handel
  zanim MM wyśle zdarzenie albo MM nie wysyła go dla villagera bez `~onInteract`. Następny test: inny Type (np. ARMOR_STAND/ZOMBIE NoAI) albo
  skill `~onInteract` na sołtysie; potem całe `zadanie.js` (cele zabij/zbierz/dojdź, nagroda, journal).
- Separator pakietu w komendach i odwołaniach = `>` (`akt1test>rozmowa`), nie kropka (Packages-and-Templates.txt:47).
- Do zrobienia w etapie 1: spark z BQ i bez (EntityMoveEvent), tabela kryteriów do zakładki.
- Klon `akt1-zadania` USUNIĘTY po teście (odtworzenie: klon.sh start, jar z modrinth wg docs/ref/betonquest/README.md,
  AKT1_test.yml → MythicMobs/mobs, restart, QuestPackages/akt1test, `bq reload`, difficulty easy).

## Następny krok
1. Etap 1: diagnoza PPM sołtysa (wyżej), pełne `zadanie.js`, spark. 2. Tor 6: projekt testu proxy-based. 3. Skrzynka wiedzy już uzupełniona (20261025-reczny-akt1-plan.md).
