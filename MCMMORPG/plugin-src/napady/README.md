# Napady (etap 2, tylko klon)

Napad królestwa jako prawdziwa walka na serwerze. Jednostki obu stron to ciała w świecie, gracz może do nich dołączyć.
Kronika WWW (`MC/docs/research/krolestwa/kronika-napadu.html`) tylko odtwarza zapis po bitwie.
Decyzje właściciela (25.10, dziennik zakładki „Królestwa i Rody” w encyklopedii):
- „gracz jak pada to pada może obserwować”;
- „nie może być lagów”;
- ciała na MythicMobs i naszym stacku, plugin ulepsza AI.

## Jak działa
- **Ciało `mm` (domyślne).** Mob MythicMobs `NAP_<ATAK|OBRONA>_<WOJOWNIK|LUCZNIK>` z `mythicmobs/mobs/Napady.yml`.
  - Chodzenie, walka wręcz i łuk to AI MM: `meleeattack`, `bowattack`.
  - Plugin tylko dowodzi. Wybiera cel (`Mob#setTarget`; wręcz wybiera tylko cel osiągalny pieszo, liczony ze składowych spójnych siatki areny, `Arena.osiagalne`). Wysyła punkt marszu (`Pathfinder#moveTo`, tylko przy zmianie). Wycofuje rannych poniżej `ranny-prog` i wysyła ich z powrotem od 70% HP.
  - Własne cele mobów są wyłączone: `EntityTargetEvent` jest anulowany poza CUSTOM i TARGET_DIED.
  - Przy `przebrania: true` LibsDisguises zakłada ciału skórę gracza (`skory.yml`).
- **Ciało `manekin`.** Mannequin bez AI, ruch i ciosy liczy plugin. Służy do porównań A/B, nie do gry („drewno”).
- **Świat `napady`.** Płaski, bez spawnu mobów. W `spigot.yml` trzeba wpisać `world-settings.napady.entity-activation-range: {monsters: 0, raiders: 0}`, inaczej Paper dławi AI ciał daleko od gracza. Plugin loguje ustawienie przy starcie.
- **Fazy:** MARSZ → BRAMA → SZTURM → GRABIEŻ → ODWRÓT → KONIEC.
  - Brama ma `hp-bramy`.
  - Po jej upadku siatka chodliwości jest liczona od nowa.
- **Gracz.**
  - Dołącza: `/napad dolacz <id> atak|obrona`. Wraca: `/napad opusc` albo koniec napadu, z powrotem do miejsca i trybu sprzed dołączenia.
  - Ciosy we własną stronę są blokowane.
  - Kto padnie, ten pada. Ma keepInventory, odradza się jako SPECTATOR nad areną. Nie może dołączyć ponownie („Polegles”). Po końcu napadu wraca.
- **Zapis:** `plugins/Napady/zapisy/napad-<id>-<ts>.jsonl.gz`.
  - Pierwsza linia to nagłówek z jednostkami.
  - Klatki: `{t,f,b,l,u:[[id,x,y,z,yaw,hp,akcja]]}` co `zapis-co-tikow`.
  - Zdarzenia: `{t,e,d}`.
- **Kolejka:** `limit-naraz` napadów naraz. Nowy rusza tylko przy MSPT poniżej `mspt-prog`.

## Komendy (`napady.admin`, poza dolacz/opusc)
`/napad start [n] | stan | dolacz <id> atak|obrona | opusc | stop [id] | limit <n> | ruch predkosc|teleport | cialo mm|manekin | przebrania true|false | pomiar [s] | ai <napad> <jednostka>`

- `pomiar [s]`: MSPT (śr/p50/p95/max), ms/tik pluginu i liczba encji. Wynik idzie do logu jako linia `POMIAR`.
- `ai`: diagnoza jednego ciała MM. Pokazuje rękę, cel, `hasLineOfSight`, `hasPath`, biegnące i wszystkie cele AI (Paper MobGoals).

## Pułapki (sprawdzone na klonie `napady`, 25.10)
- **Sprzęt ciała MM tylko w `Equipment:` moba w `Napady.yml`.**
  - Ręka ustawiona z pluginu (`setItemInMainHand`) zaraz po `MythicMob.spawn` znika: MM zakłada wyposażenie przy spawnie (MM wiki `mobs-equipment.md`).
  - Objaw: szkielet z `reka=AIR` dostaje vanillowy `skeleton_melee` i nie strzela.
  - Kolorowe kamizelki są przedmiotami MM w `mythicmobs/items/Napady.yml` (`Options: Color`).
- **Peaceful usuwa ciała MM (zombie, szkielety).** Klon zostaje na `easy`, mobów pilnuje świat bez spawnu.
- **Nie wołaj `removeAllGoals` na ciele MM** (skill `mc-mythicmobs`).
- **Nie teleportuj mobów MM co tik**: koszt `MobListeners.onTeleportEvent` (skill `mc-mythicmobs`).
- **Cel wręcz za zamkniętą bramą = jednostka stoi.** Stąd filtr `osiagalne` w `wybierzCele`.

## Budowa i test
- `./build.sh` składa jar w dockerze gradle. Jary MM, LD i packetevents kopiuje do `libs/` (ignorowane przez git).
- Wdrożenie na klon:
  - jar do `plugins/`;
  - `src/main/resources/mythicmobs/{mobs,items}/Napady.yml` do `plugins/MythicMobs/{mobs,items}/`;
  - `klon.sh restart napady`.
- Test jak gracz (bot bez OP): `/opt/docker/mc/narzedzia/bot/run.sh napady testy/dolacz.js`.
  - Sprawdza dołączenie, walkę i blokadę ciosu we własną stronę.
  - Gdy bot zginie: widza, odmowę powrotu i powrót po końcu napadu.

## Stan
Gałąź `krolestwa/napady-proto`, tylko klon. Na world1 nic nie jest wdrożone.
