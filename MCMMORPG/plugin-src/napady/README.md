# Napady (etap 2 + LOD, tylko klon)

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
- **Poziom szczegółów (LOD, `lod.*`; decyzja właściciela 25.10, wariant 2).** Model napadu (HP, pozycje, fazy) jest jeden i to on jest prawdą. Ciała to tylko jego wykonawca.
  - Stany napadu: SYM → MATERIALIZUJE → CIALA → DEMATERIALIZUJE.
  - **SYM:** bez encji. Ruch i walkę liczy `Symulacja` po tych samych drogach (`Uklad`: linia prosta albo pole przepływu), cele daje ten sam dowódca. Koszt to kilkanaście µs na tik.
  - **Ciała:** gracz w świecie napadów bliżej niż `r-wejscia` 64 bl. albo dołączenie (wymusza). Powrót do SYM, gdy przez `zwloka-s` 10 s nikogo nie ma bliżej niż `r-wyjscia` 96 i nikt nie dołączył (histereza).
  - **Materializacja:** ciała stawiane z globalnej kolejki pracy w budżecie `budzet-ms` na tik, wspólnej dla wszystkich napadów, więc 4 napady naraz nie robią skoku. Ruch ciał to krótkie skoki pathfindera (`krok-sciezki`) po polu przepływu, najwyżej `sciezki-na-tik` nowych ścieżek A* na serwer.
  - **Areny:** każdy napad ma swój slot (x = slot×200), więc się nie mieszają.
  - **Liczby symulacji (`sym.*`)** pochodzą z pomiaru ciał (patrz „Kalibracja”), dzięki czemu wynik bitwy nie zależy od tego, czy ktoś patrzy.
- **Rozgrzewka:** pierwsze ciało MM kosztuje jednorazowo ~60–100 ms (klasy MM/LD/NMS). `Napady#rozgrzej` robi to w `ServerLoadEvent` STARTUP, przed „Done”, a nie w pierwszym napadzie (https://jd.papermc.io/paper/1.21.11/org/bukkit/event/server/ServerLoadEvent.html; na klonie: zdarzenie przychodzi przed „Done”).
- **Świat `napady`.** Płaski, bez spawnu mobów. W `spigot.yml` trzeba wpisać `world-settings.napady.entity-activation-range: {monsters: 0, raiders: 0}`, inaczej Paper dławi AI ciał daleko od gracza. Plugin loguje ustawienie przy starcie.
- **Fazy:** MARSZ → BRAMA → SZTURM → GRABIEŻ → ODWRÓT → KONIEC.
  - Brama ma `hp-bramy`.
  - Po jej upadku siatka chodliwości jest liczona od nowa.
- **Gracz.**
  - Dołącza: `/napad dolacz <id> atak|obrona`. Wraca: `/napad opusc` albo koniec napadu, z powrotem do miejsca i trybu sprzed dołączenia.
  - Ciosy we własną stronę są blokowane.
  - Kto padnie, ten pada. Ma keepInventory, odradza się jako SPECTATOR nad areną. Nie może dołączyć ponownie („Polegles”). Po końcu napadu wraca.
- **Zapis:** `plugins/Napady/zapisy/napad-<id>-<ts>.jsonl.gz`.
  - Pierwsza linia to nagłówek `{napad,arena,ziarno,lod,jednostki:[{id,s,r}]}`.
  - Klatki: `{t,f,m,b,l,u:[[id,x,y,z,yaw,hp,akcja]]}` co `zapis-co-tikow`, gdzie `m` = `c` (ciała) albo `s` (symulacja).
  - Zdarzenia: `{t,e,d}`: faza, brama, ranny, padl, lod, `traf` (`a>v:dmg:odl:strzala|wrecz`), `strzal`, cios graczy, koniec.
- **Kolejka:** `limit-naraz` napadów naraz. Nowy rusza tylko przy MSPT poniżej `mspt-prog`.

## Komendy (`napady.admin`, poza dolacz/opusc)
`/napad start [n] [ziarno] | stan | dolacz <id> atak|obrona | obserwuj <id> | opusc | stop [id] | limit <n> | lod auto|ciala|symulacja | przebrania true|false | pomiar [s] | ai <napad> <jednostka>`

- `start n ziarno`: n napadów z ziarnami ziarno, ziarno+1, … (powtarzalne porównania).
- `obserwuj`: widz nad areną bez dołączania (wymusza ciała jak gracz blisko).
- `lod`: tryb do testów (`auto` w grze).

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
- **Broń w `Equipment` moba MM dodaje swoje obrażenia do `Damage` moba** (`Damage` to bazowy atrybut). Żelazny topór/miecz bił 10,3/7,0 zamiast 3. Lekarstwo: broń ciała to przedmiot MM z `Attributes: MainHand: ATTACK_DAMAGE: 0 ADD` (`NAP_TOPOR`, `NAP_MIECZ`; `MC/docs/Plugins-wiki-local/mythicmobs/wiki/md/items-attributes.md`). Na klonie 25.10: cios 2,3 po pancerzu na obie strony.
- **Ucieczka = pas wyjścia (`WYJSCIE_X`), nie punkt.** Pierwsi uciekinierzy stawali na skraju okręgu punktu i zamykali go spóźnionym, a napad kończył się limitem czasu (25.10, ziarno 101, 301 s).
- **Bot w trybie widza spada** (mineflayer liczy grawitację): w testach `bot.physicsEnabled = false` przy zmianie trybu (`testy/dolacz.js`).
- **Lagi przy 2 graczach na klonie to MythicHUD, nie napady.** Pomiar B0 bez napadów: MSPT p95 35,6 ms, napady 0,002 ms/tik. Koszt robi warstwa `l3-kolumna` (524 bloki warunków), patrz skrzynka `wiedza/skrzynka/20261025-reczny-mythichud-koszt-l3.md`.

## Budowa i test
- `./build.sh` składa jar w dockerze gradle. Jary MM, LD i packetevents kopiuje do `libs/` (ignorowane przez git).
- Wdrożenie na klon:
  - jar do `plugins/`;
  - `src/main/resources/mythicmobs/{mobs,items}/Napady.yml` do `plugins/MythicMobs/{mobs,items}/`;
  - zmieniony `config.yml` do `plugins/Napady/` (istniejacy plik nie jest nadpisywany z jara);
  - `klon.sh restart napady`.
- Test jak gracz (bot bez OP): `/opt/docker/mc/narzedzia/bot/run.sh napady testy/dolacz.js`.
  - Sprawdza dołączenie, walkę i blokadę ciosu we własną stronę.
  - Gdy bot zginie: widza, odmowę powrotu i powrót po końcu napadu.

## Kalibracja symulacji (pętla zamknięta)
1. Zapisy ciał: `napad lod ciala`, `napad limit 4`, `napad start 8 101`.
2. `python3 testy/kalibracja.py zapisy/*.gz` daje `sym.*` (prędkość, szarża, zasięgi, odstępy ciosów, celność, obrażenia po pancerzu, tłok). `--tryb s` na zapisach symulacji sprawdza, czy symulacja oddaje config. Szarża mierzy się ~0,87 configu, a prędkość ~0,97, przez tłok: przelicz, zanim wpiszesz.
3. Te same ziarna w `napad lod symulacja`, potem `python3 testy/porownaj.py <ciała> <symulacja>` porównuje czas bramy, grabieży i końca, poległych i wynik (exit 1 przy różnicy > 20%).
- Wynik 25.10 (8 par ziaren 101–108): brama −2%, grabież −4%, koniec −6%, polegli 1/4 wobec 1/4, wynik zgodny 8/8. `sym.odstep` (tłok przy drzwiach) dorzucony, bo bez niego grabież i koniec szły o 11–13% szybciej.

## Pomiary (klon `--cpus 2`, 25.10)
- 4 napady naraz, 64 ciała, bez graczy, rozgrzane: materializacja 0,45–2 s, MSPT śr 2,7, p95 6,2, max 14,6, 0 tików > 20 ms. Stały stan: śr 1,6, max 5,8.
- Koszt ciała przy materializacji to ~0,9 ms na wątku głównym (spark): spawn MM ~0,6 ms (w tym `CraftWorld.spawn` ~0,4 i nazwa z drużyną LD ~0,1), przebranie LD ~0,25 ms.
- Symulacja: ~12 µs na napad na tik.

## Testy
- `testy/lod.js` (15/15): symulacja bez gracza, obserwacja, materializacja, histereza z dematerializacją po zwłoce, ponowna materializacja, stop.
- `testy/dolacz.js` (14/14): dołączenie, walka bronią, blokada ciosu we własną stronę, śmierć, widz w punkcie widza, powrót.
- `testy/pomiar.js`: obciążenie do `napad pomiar`, bot walczy ~70 s; bez napadu to pomiar kontrolny samego gracza.

## Stan
Gałąź `krolestwa/napady-proto`, tylko klon. Na world1 nic nie jest wdrożone.
