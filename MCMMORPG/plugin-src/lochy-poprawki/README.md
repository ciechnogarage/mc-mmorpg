# LochyPoprawki

Poprawki MythicDungeons 2.0.1-SNAPSHOT (world1) i komendy konsoli dla lochów. Zależność: `MythicDungeons`.
Kod i pełny opis przyczyn: `src/main/java/pl/mcmmorpg/lochypoprawki/LochyPoprawki.java` (javadoc klasy).

## Co poprawia
1. **Gracz solo rozłączony w instancji trzymał ją na zawsze.** MD przy `PlayerQuitEvent` usuwa obiekt gracza solo,
   zanim dyspozytor instancji go użyje, więc kick po `KickOfflinePlayersDelay` nigdy nie był planowany.
   Plugin na MONITOR oddaje MD stary obiekt i woła jeszcze raz `onPlayerDisconnectDungeon`, dalej działa zwykła
   ścieżka MD: powrót przed czasem wznawia loch, po czasie wyrzucenie i zamknięcie pustej instancji (22.10).
2. **Zamiatacz osieroconych instancji** (co 30 s, od 25.10) — instancje, których MD sam nie zamknie:
   - (A) rozłączenie w trakcie kopiowania świata: MD dopisuje gracza offline bez trackera wyrzucenia →
     wyrzucenie po `KickOfflinePlayersDelay` (z configu mapy, domyślnie 300 s; razem max 300 + 2×30 s od rozłączenia);
   - (B) gracz online poza światem instancji 2 przejścia z rzędu → zwolnienie wpisu MD bez teleportu;
   - (C) „Dungeon took too long to load” (latch 5 s w `DungeonClassic`): MD gubi instancję, a świat i tak wstaje →
     migawka aktywnych instancji co 1 s + `WorldInitEvent`; zniknięcie bez dispose = `dispose()`.
   - pusta instancja → `dispose()`.
   Wyrzucenie gracza OFFLINE (tak samo przez samo MD) daje w logu stack trace FAWE/LuckPerms
   `ServerThreadLookupException` (FAWE pyta Vault przy `setGameMode`) — niegroźne.

## Komendy (konsola, uprawnienie `lochypoprawki.sygnal`, domyślnie OP)
- `lochy sygnal <gracz> <nazwa> [ticki]` — sygnał MD (jak mechanika MM `dungeonsignal`) do instancji gracza;
  odbiera go `TriggerRemote` z `triggerName: <nazwa>` w `functions.yml` mapy. Dla walk, których MD nie widzi
  (Chowance): `sala_mistrzow_koniec` (trener Czarnobor, `komendy_po_walce`), koniec wieży po fali 10.
  Gracz poza instancją albo mapa bez odbiornika = nic.
- `lochy stan` — jedna linia na instancję: świat, gracze `nick=online:<świat>` / `offline` / `offline+tracker`.

## Testy (bot bez OP na klonie `klon.sh start <n> --z-kreatorem --z-iris`, `narzedzia/bot/run.sh <klon> <plik>`)
- `test/instancja-mapa.js` — rozłączenie gracza solo i powrót/zamknięcie (poprawka 1).
- `test/koniec-chowance.js sala_powtorka|sala_mistrzow|...` — koniec lochu po walce Chowańców, nagroda mistrza raz.
- `test/sierota.js powrot|bez_powrotu` — zamiatacz, przypadek A i B (`LIMIT_MS ~ 700000` dla `bez_powrotu`).
Klon bez `--z-iris`: `sala_mistrzow` (`iris:poziom_01`) zawsze „Dungeon took too long to load”.

## Wdrożenie i cofnięcie
Paczki w `/opt/docker/mc/backups/`: `lochy-koniec-20261024` (sygnał), `nagroda-mistrza-20261024` (zamiatacz, 25.10) —
`wdroz.sh` / `cofnij.sh`. Stan wdrożenia: skill `mc-swiat-lochy` (Stan), encyklopedia zakładka Lochy.
