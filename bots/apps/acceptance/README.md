# apps/acceptance

Etap 6 (plan companion-bridge): deterministyczny acceptance runner na bazie
`packages/qa` (`runScenario`/`createReporter`) — LLM nie jest elementem
krytycznej ścieżki testów.

```bash
MC_COMPANION_TOKEN=<token z docker/servers/<domena>/plugins/CompanionBridge/config.yml> \
MC_COMPANION_BASE_URL=http://127.0.0.1:25580 \
npm run acceptance -- companion-bridge-contract-smoke
```

## Zaimplementowane

- **`companion-bridge-contract-smoke`** — potwierdza, że żywy CompanionBridge
  odpowiada zgodnie z `contracts/companion-bridge/v1` na wszystkich czterech
  endpointach (health/capabilities/performance/player-snapshot). Walidacja
  przez `validateContract` (`packages/qa/schema-check.js` — lekki, bez-
  zależnościowy subset JSON Schema: required/type/const, bez `ajv`).
  **PASS zweryfikowany na żywo** na `srv-world` i `srv-items` 2026-07-14.

- **`vault-economy-provider`** — pełny E2E: bot dołącza (bootstrap konta
  CMI/MMOCore/MMOProfiles, bo te pluginy nie tworzą danych dla UUID, które
  nigdy nie dołączyło), RCON `op`+bot wysyła `/money give` na czacie+RCON
  `deop` (nie da się tego zrobić samym RCON — CMI wysyła "no permission"
  jako komponent czatu, którego RCON nie przechwytuje, więc bot musi sam
  wysłać komendę mając tymczasowe uprawnienia), saldo rośnie o dokładną
  kwotę, i **pozostaje niezmienione po rozłączeniu** (persystencja).
  **PASS zweryfikowany na żywo** na `srv-items` 2026-07-14. UUID gracza
  testowego liczony deterministycznie z nazwy (offline-mode algorytm
  Bukkit: `UUID.nameUUIDFromBytes("OfflinePlayer:"+name)`), nie zgadywany
  ani nie szukany w usercache.

- **`dungeon-reset`** — bot wchodzi do `level_1:EASY` (jawna trudność —
  `level_1` ma `EnableDifficultyMenu: true`, więc bez trudności otwiera GUI
  zamiast startować), potwierdza świat instancji (`level_1_0`), opuszcza
  (`/md leave` → powrót do `world`), wchodzi ponownie i potwierdza **inną**
  nazwę instancji (`level_1_1`) — dowód braku recyklingu stanu.
  **PASS zweryfikowany na żywo** na `srv-world` 2026-07-14, na realnej mapie
  `level_1` (za zgodą użytkownika). Tak jak w `vault-economy-provider`,
  komendy admin (`/md play` z jawną trudnością) muszą iść przez czat bota
  z tymczasowym `op`/`deop`, nie przez samo RCON — RCON/konsola dostają
  cichy brak reakcji bez błędu.

## Niezaimplementowane (z oryginalnego planu, Etap 6)

Poniższe wymagają realnego bota (mineflayer, `packages/skills`) przechodzącego
przez konkretny flow gry, którego jeszcze nie zweryfikowałem na tym serwerze:

- **`character-accept-e2e`** — wymaga znajomości dokładnej komendy/GUI
  akceptacji postaci (DeluxeMenus/MMOProfiles) na `srv-world`; MMOProfiles
  nie jest nawet zainstalowany na tej domenie (patrz `/v1/capabilities` —
  `mmoprofiles.available: false`), więc scenariusz najpierw wymaga ustalenia,
  na której domenie (`items`/`hub`?) MMOProfiles faktycznie działa.
- **`mmoprofiles-persistence`** — zależy od character-accept-e2e.
- **`reward-idempotency`** — wymaga zidentyfikowania realnego systemu nagród
  (MMOItems/MythicMobs drop?) i jego idempotency key.
- **`dungeon-reset`** — wymaga MythicDungeons (`plugin-src` nie ma jeszcze
  adaptera; API MythicDungeons nie zostało jeszcze zbadane refleksją).

Każdy z tych scenariuszy dotyka żywej rozgrywki (GUI gracza, ekonomia,
lochy) — zanim je zaimplementuję, wymaga to ustalenia z użytkownikiem
dokładnego flow i domeny docelowej, tak samo jak deploy companion-bridge
wymagał jawnego potwierdzenia restartu `srv-world`.
