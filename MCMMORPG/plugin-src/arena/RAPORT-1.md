# RAPORT 1 — Otwarta Arena, 17.10.2026

## Zrobione
- Plugin `Arena`: wejście/wyjście z konsoli, zapis powrotu wraz z profilem, bezpieczny fallback do Stolicy, ograniczenie wejścia, blokada teleportów, 10 s combat tag i 60 s poddania po relogu.
- Śmierć na arenie: brak dropu, lokalny komunikat, zachowanie ekwipunku i EXP MMOCore, odrodzenie na platformie z 3 s ochrony; skrypt CoreTools omija arenę.
- PvP sprawdzane geometrycznie po obu stronach trafienia; bezpieczne strefy chronione przed obrażeniami, pociskami, minionami i miksturami; wyjście w pustkę wraca na platformę.
- Trzy manekiny MythicMobs: pomiar per gracz (ostatni cios, typ/kryt, suma, DPS 5 s, trafienia), actionbar MythicLib i krótkie TextDisplay z limitem; obrażenia DOT przypisane ostatniemu napastnikowi przez 5 s.
- `wdrozenie/swiat`: generator i 2866 komend budowy okrągłego ringu, muru, trybun, platformy i treningu; regiony WG oraz komendy utworzenia świata i gamerules.
- Pełne kopie 6 menu DeluxeMenus: arena w Społeczności (sloty 41,42,50,51), wariant wyjścia po świecie, blokada Portalu i wszystkich znalezionych kafli `md play` na arenie.
- `wdrozenie/wdroz-szkic.sh` i `testy/`: boty A–F, I–K z PASS/FAIL; instrukcje klienta G, H i pozostałych kontroli ręcznych.

## Decyzje
- Osobny VoidGen + `pvp` świata i region WG `arena_ring` pozwalają walczyć w ringu bez globalnej zmiany `MMOCore pvp_mode.enabled`.
- Prostokątny region WG obejmuje koło tylko z zapasem, więc plugin dodatkowo sprawdza promień obu uczestników i strefę startu pocisku.
- Powrót jest zapisany atomowo, a po zmianie profilu lub niebezpiecznym celu kieruje do Stolicy, aby nie odziedziczyć punktu innej postaci.
- Kara CoreTools jest wyłączona warunkiem świata w kopii pełnego skryptu, bo usuwanie modyfikatora skasowałoby też wcześniejszą karę ze świata otwartego.
- `PlayerExperienceGainEvent` oraz zdarzenia vanillowego EXP i kryterium osiągnięcia blokują typowe nagrody podczas pobytu na arenie.

## Sprawdzenie bez uruchamiania serwera
- Kompilacja Java 21 z API Paper 1.21.11 i jarami world1: kod 0; 11 plików YAML i 10 plików JS: poprawna składnia; 11 kontroli geometrii budowy: PASS.
- Nazwy jarów, menu, warunek CoreTools i sposób odejmowania EXP MMOCore sprawdzono w plikach world1 tylko do odczytu.
- Nie uruchomiono Paper, Dockera, klonu, RCON ani testów w grze, zgodnie ze zleceniem.

## Ryzyka i odbiór przez Claude
- Zero postępu w niestandardowych systemach Chowańców, broni, zadań i łupu wymaga osobnych testów B/F/G; anulowanie obrażeń i EXP nie dowodzi, że każdy listener tych pluginów pomija nagrodę.
- Zaklęcia i AoE H, miniony G, granica D oraz manipulacje I wymagają klienta; bot nie odtwarza pełnych gestów `player_input` ani widoku Nexo.
- Obserwacja „mmodamage nie rani mobów w Stolicy” pozostaje nierozstrzygnięta: dokumentacja MythicLib opisuje flagę WG `pvp` jako regułę PvP, a nie blokadę PvE; sprawdzić przyczynę na klonie.
- Po `mv create` potwierdzić klucz wymiaru `minecraft:arena`, skuteczność `mv gamerule list arena`, ładowanie regionów, działanie manekinów i ekran śmierci; dopiero wtedy wykonywać 2866 komend budowy.
- W kliencie zrobić zrzut Q → Społeczność: sloty 41,42,50,51 oraz wariant „Wyjdź z areny”; po restarcie sprawdzić powroty i brak pozostałych TextDisplay.

Źródła składni: [WorldGuard YAML](https://worldguard.enginehub.org/en/latest/regions/storage/), [MythicLib/MMOCore PvP](https://docs.phoenixdevt.fr/mmocore/features/combat.html), [DeluxeMenus requirements](https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/requirements), [Multiverse VoidGen](https://mvplugins.org/core/how-to/customise-world-creation/).
