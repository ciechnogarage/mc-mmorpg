# Repo MC: zasady repozytorium

Zasady hosta (klony, docker, testy, boty, klient QA, wdrożenie) są w `~/.claude/CLAUDE.md` i nie powtarzamy ich tutaj.
Wiedza o pluginach i systemach gry jest w skillach `~/.claude/skills/mc-*`.
Instrukcje Codexa (AGENTS.md, `.agents/skills`, `mcenv` i leasingi, bramki ART_PASS) zostały zarchiwizowane 21.10
w `docs/archiwum/codex-2026-10-21/`, bo Codex nie jest już używany. Nie są obowiązującą procedurą.
`docs/ai/` to wiedza z czasów Codexa: czytasz ją jako tło, a w razie sprzeczności z `~/.claude/CLAUDE.md` wygrywa CLAUDE.md.

## Co gdzie jest
- `MCMMORPG/plugin-src/<plugin>/`: źródła własnych pluginów Java; każdy buduje się własnym `build.sh` (Gradle wrapper nie jest standardem).
- `MCMMORPG/plugins/`: konfiguracje pluginów serwera; `MCMMORPG/_validation/`: walidatory (`package.json`).
- `bots/`: stary zestaw botów repo. Boty testowe na hoście uruchamiasz z `/opt/docker/mc/narzedzia/bot`.
- `mcmmorpg-updater/`, `rag/`: osobne projekty z własnymi testami.
- `tests/*.test.mjs`: kontrakty repo (`node --test tests/*.test.mjs`; na hoście nie ma `node`, uruchamiasz w kontenerze `node:20`).

## Zasady
- Światy, logi, cache, skompilowane jary, eksporty i sekrety to dane, nie źródło. Nie edytujesz ich bez wyraźnej potrzeby zadania.
- Nie zgadujesz uprawnień, placeholderów, komend pluginów ani identyfikatorów modeli i assetów. Sprawdzasz je w configu, kodzie albo dokumentacji.
- Przed zmianą podsystemu czytasz jego manifest lub `build.sh`, testy, README i jeden działający przykład obok.
- Udany build, walidator czy czysty log startu to nie dowód działania w grze. Dowodem jest test na klonie według `~/.claude/CLAUDE.md`.
- Zmiany robisz na własnej gałęzi (worktree w `.claude/worktrees/`) i nie ruszasz cudzych niezacommitowanych zmian.
- Raport: zmienione ścieżki, uruchomione testy i wyniki, czego nie sprawdzono i dlaczego.
