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
- **Wyposażenie z MMOItems (`wyposazenie.<strona>.<rola>`, `Wyposazenie.java`).** Każda rola nosi przedmioty MMOItems (`SLOT: TYP:ID`, poziom `wyposazenie.poziom`), te same co gracz. Skórzane barwi na kolor strony.
  - MMOItems nie daje statystyk mobom („mmo stats on the armor DO NOT work on MythicMobs”, `MC/docs/Plugins-wiki-local/mythicmobs/wiki/md/mobs-equipment.md`). Plugin czyta je z przedmiotu (NBT `MMOITEMS_*`) i ustawia ciału atrybuty: pancerz i wytrzymałość (vanilla `armor`, `armor_toughness`), życie = `zycie` roli + `MAX_HEALTH` przedmiotów, atak = `ATTACK_DAMAGE` broni w ręce (wręcz) i baza strzały (łuk). Vanillowe atrybuty samych przedmiotów są zerowane.
  - Obrona MythicLib działa tylko dla graczy (`DamageReduction` wymaga `MMOPlayerData`). Ciału liczy ją listener `obronaCiala` tym samym wzorem z `plugins/MythicLib/config.yml` (`defense-application`).
  - Symulacja liczy cios tym samym wzorem (`Wyposazenie.poObronie`, `poPancerzu` = vanilla `CombatRules`), więc ciało i model dają to samo.
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
  - Zdarzenia: `{t,e,d}`: faza, brama, ranny, padl, lod, `traf` (`a>v:dmg:odl:wrecz` albo `a>v:dmg:odl:strzala:mn`, `mn` = obrażenia strzały ciała przed pancerzem / atak łuku), `strzal`, cios graczy, koniec.
- **Kolejka:** `limit-naraz` napadów naraz. Nowy rusza tylko przy MSPT poniżej `mspt-prog`.

## Komendy (`napady.admin`, poza dolacz/opusc)
`/napad start [n] [ziarno] | stan | dolacz <id> atak|obrona | obserwuj <id> | opusc | stop [id] | limit <n> | lod auto|ciala|symulacja | przebrania true|false | pomiar [s] | ai <napad> <jednostka>`

- `start n ziarno`: n napadów z ziarnami ziarno, ziarno+1, … (powtarzalne porównania).
- `obserwuj`: widz nad areną bez dołączania (wymusza ciała jak gracz blisko).
- `lod`: tryb do testów (`auto` w grze).

- `pomiar [s]`: MSPT (śr/p50/p95/max), ms/tik pluginu i liczba encji. Wynik idzie do logu jako linia `POMIAR`.
- `ai`: diagnoza jednego ciała MM. Pokazuje rękę, cel, `hasLineOfSight`, `hasPath`, biegnące i wszystkie cele AI (Paper MobGoals).

## Pułapki (sprawdzone na klonie `napady`, 25.10)
- **Sprzęt z pluginu zakładaj 1 tik po spawnie i jeszcze raz po 10 tikach.** MM zakłada `Equipment:` moba przy spawnie i nadpisuje rękę jeszcze kilka tików później (25.10: łuk szkieletu wracał jako goły vanillowy). `PreventRandomEquipment: true` czyści wyposażenie 3–5 tików po spawnie, więc ciała mają `false`. Szkielet zostaje z `Equipment: bow HAND` (bez łuku w ręce przy spawnie dostaje cel `skeleton_melee` i nie strzela).
- **Peaceful usuwa ciała MM (zombie, szkielety).** Klon zostaje na `easy`, mobów pilnuje świat bez spawnu.
- **Nie wołaj `removeAllGoals` na ciele MM** (skill `mc-mythicmobs`).
- **Nie teleportuj mobów MM co tik**: koszt `MobListeners.onTeleportEvent` (skill `mc-mythicmobs`).
- **Cel wręcz za zamkniętą bramą = jednostka stoi.** Stąd filtr `osiagalne` w `wybierzCele`.
- **Broń na mobie dodaje swoje vanillowe atrybuty do ataku moba** (25.10: żelazny topór bił 10,3 zamiast 3). Stąd zerowanie komponentu `attribute_modifiers` przedmiotu i atak tylko z atrybutu ciała.
- **Kolejność obrażeń ciało→gracz: trudność (`easy`: min(d/2+1, d)), obrona MythicLib, pancerz vanilla** — sprawdzone botem w zbroi MMOItems (`testy/dolacz.js`, cios 1,73 przy oczekiwanym 1,73).
- **Strzała z muru trafia częściej i mocniej.** Celność z muru w dół 0,68, z ziemi na mur 0,36 (25.10, 385 strzałów ciał). Spadająca strzała przyspiesza, a vanilla mnoży obrażenia przez prędkość. Symulacja ma na to `sym.celnosc-z-muru`, `celnosc-na-mur`, `strzala-z-muru`; bez nich wynik różnił się w 4/8 ziaren, a napastnicy tracili 1 zamiast 4.
- **Ucieczka = pas wyjścia (`WYJSCIE_X`), nie punkt.** Pierwsi uciekinierzy stawali na skraju okręgu punktu i zamykali go spóźnionym, a napad kończył się limitem czasu (25.10, ziarno 101, 301 s).
- **Bot w trybie widza spada** (mineflayer liczy grawitację): w testach `bot.physicsEnabled = false` przy zmianie trybu (`testy/dolacz.js`).
- **Lagi przy 2 graczach na klonie to MythicHUD, nie napady.** Pomiar B0 bez napadów: MSPT p95 35,6 ms, napady 0,002 ms/tik. Koszt robi warstwa `l3-kolumna` (524 bloki warunków), patrz skrzynka `wiedza/skrzynka/20261025-reczny-mythichud-koszt-l3.md`.

## Budowa i test
- `./build.sh` składa jar w dockerze gradle. Jary MM, LD, packetevents, MMOItems i MythicLib kopiuje do `libs/` (ignorowane przez git).
- Wdrożenie na klon:
  - jar do `plugins/`;
  - `src/main/resources/mythicmobs/mobs/Napady.yml` do `plugins/MythicMobs/mobs/` (stary `items/Napady.yml` z kamizelkami usuń);
  - zmieniony `config.yml` do `plugins/Napady/` (istniejacy plik nie jest nadpisywany z jara);
  - `klon.sh restart napady`.
- Test jak gracz (bot bez OP): `/opt/docker/mc/narzedzia/bot/run.sh napady testy/dolacz.js`.
  - Sprawdza dołączenie, walkę i blokadę ciosu we własną stronę.
  - Gdy bot zginie: widza, odmowę powrotu i powrót po końcu napadu.

## Kalibracja symulacji (pętla zamknięta)
1. Zapisy ciał: `napad lod ciala`, `napad limit 4`, `napad start 8 101`.
2. `python3 testy/kalibracja.py zapisy/*.gz` daje `sym.*` (prędkość, szarża, zasięgi, odstępy ciosów, naciąg łuku, celność ogólna i z muru / na mur, mnożnik strzały z muru, obrażenia po pancerzu do kontroli, tłok). `--tryb s` na zapisach symulacji sprawdza, czy symulacja oddaje config. Szarża mierzy się ~0,87 configu, a prędkość ~0,97, przez tłok: przelicz, zanim wpiszesz.
3. Te same ziarna w `napad lod symulacja`, potem `python3 testy/porownaj.py <ciała> <symulacja>` porównuje czas bramy, grabieży i końca, poległych i wynik (exit 1 przy różnicy > 20%).
- Wynik 25.10 wieczór (zbroja z MMOItems, 8 par ziaren 201–208): brama +12%, grabież +5%, koniec +3%, polegli atak 2,5→3,0, obrona 4,0→4,5, ZGODNE (wynik różny w 2/8 parach, tyle co rozrzut samych ciał). Po drodze trzy poprawki modelu:
  - `sym.luk-naciag` (22 tiki): szkielet naciąga łuk ~1 s po złapaniu celu (`kalibracja.py` mierzy od wejścia w `strzela` do pierwszej strzały, 56 próbek); bez tego symulacja strzelała w t=10 zamiast ~31 i napastnicy tracili 2× mniej.
  - Ziarno mieszane SplitMix64 (`Napad.miesz`) i osobny strumień na celność (`losTraf`): `java.util.Random` z ziarnami 201, 202… dawał prawie te same pierwsze liczby, 8 napadów było kopią jednego (obrona traciła 6 w każdym, celność na mur 0,45 przy configu 0,36). Po poprawce 0,34.
  - Symulacja idzie do środka punktu rozkazu (0,25), nie do brzegu promienia: ciało idzie pathfinderem do środka kratki, a zatrzymanie na brzegu zostawiało drugi rząd poza promieniem grabieży (2,2 grabiących zamiast 3,6, grabież 22 s zamiast 13).
- Liczby z małych prób (polegli 0–6 na napad) porównuj średnią i rozrzutem po ziarnach, nie samą medianą: 1 jednostka to ±20–40%.

## Pomiary (klon `--cpus 2`, 25.10)
- 4 napady naraz, 64 ciała, bez graczy, rozgrzane: materializacja 0,45–2 s, MSPT śr 2,7, p95 6,2, max 14,6, 0 tików > 20 ms. Stały stan: śr 1,6, max 5,8.
- Koszt ciała przy materializacji to ~0,9 ms na wątku głównym (spark): spawn MM ~0,6 ms (w tym `CraftWorld.spawn` ~0,4 i nazwa z drużyną LD ~0,1), przebranie LD ~0,25 ms.
- Symulacja: ~12 µs na napad na tik.

## Testy
- `testy/lod.js` (15/15): symulacja bez gracza, obserwacja, materializacja, histereza z dematerializacją po zwłoce, ponowna materializacja, stop.
- `testy/dolacz.js` (17/17, z obrażeniami po zbroi MMOItems i obronie MythicLib): dołączenie, walka bronią, blokada ciosu we własną stronę, śmierć, widz w punkcie widza, powrót.
- `testy/pomiar.js`: obciążenie do `napad pomiar`, bot walczy ~70 s; bez napadu to pomiar kontrolny samego gracza.

## Stan
Gałąź `krolestwa/napady-proto`, tylko klon. Na world1 nic nie jest wdrożone.
