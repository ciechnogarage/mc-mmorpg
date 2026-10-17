# Runda 4 — „Q na gracza”: menu gracza po celowaniu w innego gracza

Wlasciciel: zamiast szukac funkcji w hubie, gracz patrzy na innego gracza i wciska Q -> menu akcji wobec TEGO gracza.
PPM/Shift+PPM na graczu sa zajete (zaklecia lewej reki, blok bronia) — dlatego Q.

## Jak dziala Q dzisiaj (nie zmieniaj tych pluginow)
MenuKey przechwytuje Q i wola z konsoli `zaklecia q <gracz>`. Zaklecia: Shift+Q / otwarty pasek -> wlasny pasek;
samo Q -> komenda z `plugins/Zaklecia/config.yml` klucz `klawisz-q.zwykle` (dzis `dm open hub_glowne %gracz%`).
Wdrozenie zmieni TYLKO ten klucz na `menugracza q %gracz%` (plik w `wdrozenie/pliki/Zaklecia/config.yml.PATCH.md`, opis
jak MythicLib). Kod Zaklec ma inna galaz i inna sesje — NIE ruszaj go.

## Zrob w pluginie arena (nowa klasa, np. MenuGracza.java; rejestracja w ArenaPlugin.onEnable)
1. Komenda `menugracza q <gracz>` — tylko konsola/RCON (jak `arena`). Szuka celu: ray trace z oczu gracza do 4,5 bloku
   (`World.rayTrace` z kolizja blokow — przez sciane NIE), tylko encje `Player`, rozne od siebie, `player.canSee(cel)`,
   cel nie w trybie widza, cel bez metadanych `vanished` (CMI) i `NPC`. Brak celu -> wykonaj z konsoli
   `menu-gracza.zwykle` z configu (domyslnie `dm open hub_glowne %gracz%`) — zachowanie jak dotad.
2. Cel znaleziony -> dialog Papera (API io.papermc.paper.dialog, jak w CharacterCreator: przyciski z WLASNYM id i
   PlayerCustomClickEvent — callbacki Papera gubia kliki, pusty multiAction rzuca wyjatek). Tytul `Gracz: <nick>`, cialo:
   klasa i poziom MMOCore, patron (PAPI `%wiary_patron%` jesli jest, inaczej pomin), gildia MMOCore. Przyciski:
   - **Wyzwij na pojedynek** -> u celu dialog „<nick> wyzywa Cie na pojedynek na Otwartej Arenie” [Przyjmij] [Odrzuc].
     Zaproszenie wazne 30 s. Przyjecie: obu sprawdz warunkami wejscia areny (ta sama logika co `arena wejdz`: swiat z
     entry-worlds albo juz na arenie, nie w walce MMOCore, nie martwy/spadajacy, bez tagu); kto nie spelnia -> obu komunikat
     dlaczego. Kto nie jest na arenie -> `enter()` (zapis punktu powrotu jak z menu). Potem obu do ringu naprzeciw siebie
     (0.5 64 -8.5 twarza na poludnie i 0.5 64 9.5 twarza na polnoc), 3 s odliczania w tytule, ochrona `safeUntil` do konca
     odliczania. Bez zakladow i nagrod.
   - **Zapros do druzyny** -> MMOCore party: jesli zapraszajacy nie ma druzyny, utworz; wyslij zaproszenie MMOCore (API
     MMOCore PartyModule/PartyInvite — sprawdz w jarze /opt/docker/mc/data/world1/plugins/MMOCore-*.jar; gracz akceptuje
     `/party accept` z ich komunikatu). Ukryj, gdy cel juz w tej druzynie.
   - **Dodaj do znajomych** -> MMOCore friend request (to samo API co `/friends`); ukryj, gdy juz znajomi.
   - **Zapros do gildii** -> MMOCore guild invite; pokaz tylko gdy zapraszajacy ma gildie i prawo zapraszania, a cel nie ma gildii.
   - **Zamknij**.
3. Antyspam: jedno zaproszenie danego rodzaju do tego samego celu co 15 s (komunikat ile zostalo). Zaproszenie do pojedynku
   wygasa po 30 s, przy wyjsciu ktoregos z gry i przy zmianie postaci (profil MMOProfiles). Przyjecie wygaslego -> komunikat.
4. Komunikaty po polsku, pasek MythicLib priorytet 40 (jak `deny`). Wszystkie stany w pamieci, czyszczone przy quit.
5. config.yml: sekcja `menu-gracza` (zasieg 4.5, zwykle, cooldown 15, waznosc-pojedynku 30, pozycje ringu).

## Sposoby rozjebania do obsluzenia w kodzie
Q przez sciane; Q na niewidzialnego/vanish; Q w scenie kreatora (MenuKey blokuje creator_limbo — i tak sprawdz swiat);
wyzwanie samego siebie; przyjecie po 31 s; przyjecie gdy jeden w lochu MythicDungeons / w walce / martwy / w trakcie kreatora;
spam wyzwan i zaproszen; dwa wyzwania naraz od roznych graczy (przyjecie jednego anuluje drugie); wyzwanie, gdy cel juz w
ringu walczy z kims (tag) -> odmowa; wylogowanie po przyjeciu w trakcie odliczania; zmiana postaci w trakcie zaproszenia.

## Raport
RAPORT-4.md max 30 linii: pliki i linie, uzyte API MMOCore (klasy/metody z jara), czego nie dalo sie zrobic.
Nie budujesz i nie testujesz (brak dockera) — Claude zbuduje i przetestuje dwoma klientami.
