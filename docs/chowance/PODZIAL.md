# Podział pracy i zasady współpracy

## Agent A — SILNIK (plugin „Chowance” + infrastruktura)
Właściciel: plugin-src i jar Chowance, zapis danych, API, KONTRAKT.md.
- Spike MCPets (lista kontrolna w SPEC §0) → DECYZJE.md.
- Plugin (Paper 1.21.11, build w dockerze — zob. memory mc-build-w-kontenerach): model danych osobnika (gatunek, poziom, exp,
  geny, charakter, cecha, 4 ruchy, więź, lśniący, pochodzenie), drużyna 6 + magazyn, zapis per profil MMOProfiles (plik/SQLite,
  bez MariaDB), odporność na profil-ducha, przywołanie/odwołanie/despawn (wyjście, zmiana profilu, śmierć, restart, zmiana świata).
- Logika łapania: odbiór trafienia Pieczęcią (hook dla skilla MythicLib/MMOItems), wzór szansy, drżenia, szał przy porażce,
  pieczęcie specjalne (warunki nocna/wodna/kradzieży/dungeonowa), oswajanie karmieniem.
- Ewolucja (poziom + przedmiot + więź), exp, poziomy, limit poziomu z odznak.
- Walki: silnik tabeli typów, pojedynki areny sterowane (1–4), walka turowa w dialogu Papera, ranking + turnieje.
- Hodowla (jajo, dystans, dziedziczenie), handel między graczami (bezpieczna wymiana, związane vs wymienialne).
- Bestiariusz (widziany/złapany, progi nagród — wypłata nagród wg definicji od B).
- Placeholdery PAPI, komendy admina, uprawnienia (isPermissionSet), integracja MMOCore (exp profesji Poskramiacz przez API).
- Mechaniki/targetery/warunki MythicMobs rejestrowane przez plugin (jeśli potrzebne), zgodnie z dokumentacją API MythicMobs.
- Evale botem mineflayer dla całej logiki (PASS/FAIL), pomiar spark.

## Agent B — TREŚĆ (gatunki, grafika, przedmioty, świat, menu)
Właściciel: MythicMobs/ModelEngine/Nexo/MMOItems/MMOCore/DeluxeMenus/MythicHUD/MythicDungeons/Iris/WorldGuard configi Chowańców.
- Projekt i pełne dane gatunków (minimum z SPEC §0): typy, statystyki bazowe, pule ruchów, cechy, linie ewolucji, rzadkość,
  biomy/pora/pogoda, opisy do Bestiariusza — w formacie z KONTRAKT.md.
- Moby MythicMobs (ciała, AI peta, skille ruchów, wariant dziki vs oswojony, legendarne jako bossy z harmonogramem),
  modele BlockBench + animacje (idle, walk, attack, okrzyk, drżenie pieczęci), lśniące tekstury, ikony Nexo.
- MMOItems: pieczęcie (wszystkie klasy + specjalne), kamienie przemiany, karma/eliksiry, siodła, jaja; receptury w stacjach.
- MMOCore: profesja Poskramiacz (pamiętaj memory mmocore-profesje-klucze), nagrody Bestiariusza (tytuły, exp).
- Spawny (Iris biomy, WorldGuard regiony), lochy endemiczne i Mistrzowie Więzi (MythicDungeons), Wieża Chowańców,
  wydarzenia sezonowe.
- DeluxeMenus: kafelek w hub_glowne + menu Menażerii/drużyny/Bestiariusza/hodowli (na placeholderach/komendach od A),
  MythicHUD: pasek aktywnego stwora. Wybór startera przy nowej postaci.
- Evale botem: spawny, łapanie z perspektywy gracza, menu, receptury; weryfikacja wizualna klientem QA gdzie trzeba.

## Wspólne
- KONTRAKT.md (właściciel A, B zgłasza potrzeby): schemat danych gatunku, komendy, placeholdery, nazwy mechanik MM,
  identyfikatory itemów, zdarzenia. A publikuje wersję v0 w PIERWSZEJ kolejności, zanim zejdzie głęboko w kod.
- DZIENNIK.md: każdy wpis = data/godz, agent, co zmienił (ścieżki), jak zweryfikował, wynik.
- DECYZJE.md: każda decyzja projektowa/techniczna + źródło (link do dokumentacji).
- PROBLEMY.md: znalezione błędy/rozbieżności założeń, status.
- BLOKADA-RESTARTU: plik-zamek. Przed restartem serwera, /mm reload, /nexo reload, /mi reload, /mmocore reload, reloadem
  DeluxeMenus: utwórz plik z treścią „agent, czynność, godzina”; usuń po zakończeniu. Jeśli istnieje i jest świeży (<15 min) —
  czekaj. Restart TYLKO przy zerze graczy (sprawdź `minecraft:list uuids`, memory rcon-list-pusty); inaczej odłóż.
