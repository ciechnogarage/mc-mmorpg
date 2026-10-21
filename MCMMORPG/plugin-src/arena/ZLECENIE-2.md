# Otwarta Arena — runda 2 (poprawki po testach na klonie)

Kontekst: Twoja runda 1 jest w tym katalogu (ArenaPlugin.java, wdrozenie/, testy/). Claude przetestował ją botami na klonie
world1 (Paper 1.21.11, MythicLib 1.7.1, MMOCore, MMOProfiles, CMI, WorldGuard, Multiverse 5). Część rzeczy działa
(granica ringu wręcz/łuk/mikstury/kula ognia, combat tag, 14 komend ucieczki, przedmioty, manekin po poprawce WG).
Poniżej WYŁĄCZNIE to, co trzeba zmienić. Nie przepisuj działających części. Nie masz dockera — nie budujesz i nie testujesz;
Claude zbuduje i sprawdzi. Pisz kod tak, żeby kompilował się z obecnym build.gradle (te same zależności compileOnly).
Dokumentacja pluginów: najpierw oficjalne docs (MythicLib/MMOCore/MMOProfiles: gitlab.com/phoenix-dvpmt / wiki, CMI: zrips.net,
WorldGuard: worldguard.enginehub.org). Jeśli API niejasne, sprawdź klasy w jarach w /home/dev/Desktop/Przemek/MC/.claude/worktrees/arena/MCMMORPG/plugin-src/arena/../../../.. (gradle cache: .gradle-home) albo w jarach serwera /opt/docker/mc/data/world1/plugins (tylko odczyt).

## 1. KRYTYCZNE: polityka PvP na całym serwerze
Stan produkcji: PvP nie działa NIGDZIE, bo w plugins/MythicLib/config.yml `interaction_rules.pvp_on.offense` brakuje kluczy
`party_other` i `guild_enemy` (MythicLib PvpListener na LOWEST anuluje każdy cios gracz→gracz). Po dodaniu `party_other: true`
i `guild_enemy: true` PvP działa w ringu — ale też w każdym świecie bez flagi WG `pvp: deny`. Na world1 deny mają tylko
`stolica` i `stolica_odnowa`; nie mają: `world`, `world_nether`, `world_the_end`, `creator_limbo` i dynamiczne instancje
MythicDungeons (`loch_001_0`, `wieza_chowancow_0`, ... tworzone w locie).
Zrób:
- W pluginie: opcja `pvp-tylko-w-ringu: true` (domyślnie true). Gdy włączona, gracz NIE może zranić gracza nigdzie poza
  ringiem areny — w każdym świecie. Obejmij: cios wręcz, pociski (strzały, trójzęby, MythicMobs projectile przez
  PlayerAttackEvent), mikstury rzucane i pozostające (PotionSplashEvent / LingeringPotionSplashEvent / AreaEffectCloudApplyEvent
  — tylko efekty szkodliwe, leczenie innych graczy ma dalej działać poza areną!), obrażenia od zwierząt/przywołańców
  należących do gracza (oswojony wilk = Tameable z ownerem, moby MythicMobs z ownerem — MythicLib DamageType.MINION),
  skille MythicLib/MMOCore/MythicMobs (PlayerAttackEvent). Wyjątek: ring areny (obaj w ringu, jak teraz).
  Nie blokuj samozranienia (gracz == ofiara) ani obrażeń od mobów.
- `wdrozenie/pliki/MythicLib/config.yml.PATCH.md`: dokładny opis zmiany (dwa klucze) — NIE kopiuj całego configu.
- `wdrozenie/swiat/wg-pvp-deny.txt`: komendy konsoli `rg flag __global__ -w <świat> pvp deny` dla world, world_nether,
  world_the_end, creator_limbo (druga zapora obok pluginu).

## 2. Odrodzenie po śmierci na arenie ląduje na ŚRODKU RINGU
Zmierzone: po śmierci w ringu gracz odradza się w (0, 65, 0) świata `arena` (spawn świata), nie na platformie.
`onRespawn` (priorytet NORMAL) nadpisuje CMI (`ReSpawn.Enabled: true`, kolejność anchor/bed/spawn/worldSpawn).
Zrób: `onRespawn` na HIGHEST + zabezpieczenie 1 tick po odrodzeniu (jeśli gracz w świecie arena i nie na platformie →
safeTeleport na spawn areny). W `wdrozenie/swiat/ustaw.txt` ustaw spawn świata areny na platformę
(`mv setspawn` nie działa z konsoli — użyj `minecraft:execute in minecraft:arena run minecraft:setworldspawn 0 65 -29`
i sprawdź w docs Multiverse 5, czy MV nie trzyma własnego spawnu; jeśli trzyma — dodaj właściwą komendę).

## 3. WorldGuard blokował bicie manekinów
Manekin to IRON_GOLEM; region bez `damage-animals: allow` anulował cios zanim dotarł do MythicLib → licznik nic nie
pokazywał. Na klonie po `rg flag -w arena arena_trening damage-animals allow` licznik działa. Popraw `wdrozenie/swiat/regions.yml`
(arena_trening: `damage-animals: allow`) i sprawdź w docs WG, czy dla golema nie trzeba innej flagi.

## 4. Wyjście/relog/zmiana postaci — bez stanów „utknąłem”
Ryzyko produkcji: przy wejściu na serwer MMOProfiles teleportuje gracza do `profile-selection-location` (stolica_odnowa),
a po wyborze postaci — do jej zapisanej lokalizacji; CharacterCreator ma scenę wyboru. Obecny `onTeleport` anuluje
KAŻDY obcy teleport z/na arenę → gracz, który wyszedł z gry na arenie, zostaje na platformie areny w trakcie wyboru postaci,
a zmiana postaci na arenie nie wynosi go z areny. Zrób model bezpieczny:
- `PlayerQuitEvent` na LOWEST: jeśli gracz na arenie → (jeśli tag: poddanie jak teraz, forfeit zapisany) → safeTeleport do
  punktu powrotu (validReturn) albo fallback, ZANIM inne pluginy zapiszą lokalizację. Po relogu gracz jest już poza areną.
- `onDisable`: to samo dla wszystkich graczy w świecie arena (restart serwera).
- Zmiana postaci MMOProfiles na arenie: nasłuchuj właściwego zdarzenia MMOProfiles (fr.phoenixdevt.profiles.event.*,
  np. ProfileUnloadEvent — sprawdź w jarze, które jest wywoływane PRZED zapisem lokalizacji) i wynieś gracza jak przy wyjściu.
- `onTeleport`: obcy teleport Z areny anuluj TYLKO gdy gracz ma combat tag; bez tagu przepuść i wyczyść jego stan
  (punkt powrotu, licznik, ochrona). Obcy teleport NA arenę (nie przez `arena wejdz`) — anuluj jak teraz.
- `profile()`: na klonie `MMOPlayerData.getProfileId()` rzucało NPE „No profile”, więc zapisywał się `''`, a po relogu id się
  zmieniało i `validReturn` odrzucał poprawny punkt powrotu (gracz lądował na zapasowym spawnie). Użyj API MMOProfiles do
  wybranego profilu, a porównanie rób tylko gdy OBA id są niepuste.

## 5. Komunikat MMOCore o stracie EXP na arenie
Ofiara widzi „Zginąłeś i straciłeś 5 doświadczenia.”, choć arena oddaje EXP 1 tick później (stan EXP poprawny — zmierzone).
Znajdź w MMOCore (docs / jar) jak strata EXP przy śmierci jest liczona i czy da się ją wyłączyć dla świata/gracza
(uprawnienie bypass, opcja per świat, zdarzenie). Jeśli się da — zrób to i usuń przywracanie EXP. Jeśli nie — zostaw
przywracanie i wyślij graczowi po odrodzeniu komunikat „Na arenie nie tracisz doświadczenia.” (pasek MythicLib, priorytet 40).

## 6. Licznik obrażeń — czytelna seria
Teraz suma i trafienia rosną bez końca (reset tylko przy wejściu), a DPS = suma z 5 s / 5 (krótka seria pokazuje za mało).
Zrób: seria kończy się po `ui.series-reset-seconds: 6` bez trafień; nowa seria zeruje sumę i trafienia; DPS = suma serii /
max(1 s, czas od pierwszego do ostatniego trafienia serii). Pasek: `§e<cios> §7<typ>[ KRYT]  §fSeria <suma> · DPS <dps> · <n> traf.`

## 7. Sonda
`Sonda.java` jest diagnostyką Claude'a (tylko przy `sonda-pvp: true`). Zostaw, domyślnie false w config.yml; nie dokładaj do niej nic.

## Raport
`RAPORT-2.md`, max 40 linii: co zmieniono (plik:linia), czego nie dało się zrobić i dlaczego, źródła docs dla 2/4/5.
Nie ruszaj plików poza tym katalogiem.
