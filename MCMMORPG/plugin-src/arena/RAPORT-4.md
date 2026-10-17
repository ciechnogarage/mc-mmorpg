# Raport rundy 4

- `src/main/java/pl/mcmmorpg/arena/MenuGracza.java:47`: nowa komenda konsolowa `menugracza q`, dialogi Papera z własnymi identyfikatorami kliknięć, stany w pamięci.
- `src/main/java/pl/mcmmorpg/arena/MenuGracza.java:90`: `World.rayTrace` do 4,5 bloku z kolizją bloków; filtr gracza, widoczności, CMI `vanished`, `NPC`, widza i `creator_limbo`; bez celu dotychczasowe menu.
- `src/main/java/pl/mcmmorpg/arena/MenuGracza.java:128`: klasa, poziom, gildia MMOCore i opcjonalny `%wiary_patron%` z PlaceholderAPI; przyciski tylko dla dostępnych akcji.
- `src/main/java/pl/mcmmorpg/arena/MenuGracza.java:204`: wyzwania 30 s, cooldown 15 s na rodzaj/parę, lista równoczesnych wyzwań, akceptacja anuluje pozostałe.
- `src/main/java/pl/mcmmorpg/arena/MenuGracza.java:238`: kontrola obu graczy, wejście na arenę, teleport do przeciwległych punktów ringu, tytuł 3–2–1 i ochrona do końca odliczania; anulowanie przy wyjściu, śmierci i zmianie postaci.
- `src/main/java/pl/mcmmorpg/arena/ArenaPlugin.java:104`: rejestracja menu i integracja z `ProfileUnloadEvent`; linia 197: warunki pojedynku; linia 209: użycie istniejącego `enter()` i zapisu powrotu.
- `src/main/resources/config.yml:30`: konfiguracja zasięgu, komendy domyślnej, limitów czasu i punktów ringu; `src/main/resources/plugin.yml:12`: komenda i uprawnienie konsoli.
- `wdrozenie/pliki/Zaklecia/config.yml.PATCH.md:1`: instrukcja zmiany tylko `klawisz-q.zwykle` na `menugracza q %gracz%`.

## API MMOCore z lokalnego jara 1.13.1

- Drużyna: `MMOCore.partyModule`, `MMOCorePartyModule.newRegisteredParty(PlayerData)`, `Party.sendPartyInvite(PlayerData, PlayerData)`, `AbstractParty.hasMember(Player)`; lokalny `party-plugin: MMOCORE`.
- Znajomi: `PlayerData.hasFriend(UUID)`, `PlayerData.sendFriendRequest(PlayerData)`.
- Gildia: `PlayerData.getGuild()`, `Guild.getOwner()`, `Guild.sendGuildInvite(PlayerData, PlayerData)`; lokalny `guild-plugin: MMOCORE`. Zapraszanie ograniczone do właściciela, zgodnie z widokiem zaproszeń MMOCore.
- Dane: `PlayerData.getProfess().getName()`, `getLevel()`, `getGuild().getName()`, `isInCombat()`.

## Weryfikacja i ograniczenia

- Odczytano sygnatury z lokalnego jara MMOCore i API Papera oraz konfigurację uruchomieniową; nie budowano ani nie testowano na żywo zgodnie ze zleceniem.
- Pozostaje kompilacja i test dwoma klientami na backendzie `world` przez Velocity oraz sprawdzenie faktycznych punktów ringu i komunikatów MMOCore.
