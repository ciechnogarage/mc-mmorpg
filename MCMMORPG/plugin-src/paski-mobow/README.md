# PaskiMobow — pasek nad głową moba (2026-10-01)

Nad mobem bojowym wisi:

```
   [ikony statusów]            ← tylko gdy jakiś status trwa
 ◆ Nazwa moba                  ← kolor rzadkości, ikona rzadkości (klejnot / korona / gwiazda)
 [12] ███████████░░░  89/140   ← plakietka poziomu w kolorze rzadkości, HP + jasny "duch" obrażeń, liczby obok
```

- **Rzadkość** (drabina jak w przedmiotach): Pospolity (szary), Magiczny (niebieski, klejnot), Rzadki (żółty,
  klejnot), Epicki (fioletowy, korona, pogrubiony), Legendarny (pomarańczowy, gwiazda, pogrubiony).
- **Poziom i rzadkość** wg id MythicMobs w `config.yml` (`moby:`, glob, pierwszy pasujący); brak wpisu = automat:
  BossBar → legendarny, pogrubiona nazwa → rzadki, reszta pospolity, poziom 1. Poziom MM > 1 (spawnery) dochodzi.
- **Statusy** (ikony 9×9): ogień, trucizna, krwawienie (AnimacjeBroni), obumarcie, spowolnienie, osłabienie,
  oślepienie/ciemność, zamrożenie, siła, odporność, regeneracja, szybkość, absorpcja.
- **Kolor HP**: zielony powyżej 60%, żółty powyżej 35%, pomarańczowy powyżej 15%, czerwony przy 15% i mniej.
  `wyglad.kolor-hp: czerwony` przywraca stały czerwony kolor. Duch obrażeń po ciosie świeci jasno ~0,6 s.
- **Poza walką**: domyślnie zostają statusy, nazwa i plakietka poziomu; pasek HP i liczby znikają. Walka trwa
  przez `wyglad.walka-sekundy` od obrażeń albo gdy mob celuje w gracza. `poza-walka: wszystko` pokazuje cały pasek,
  a `nic` chowa cały tekst.

Kto ma pasek: moby MM z frakcji `bestie, nieumarli, humanoidy, magiczne, gaj` + `WIEZA_P01_*`, zwykłe wrogie
moby vanilla. Moby ModelEngine przechodzą te same reguły frakcji i wzorców MM. Bez paska: NPC, węzły zasobów,
pieczęcie, sługi Akolity i Chowańce (własny HUD areny).

## Jak to działa

- Pasek to **text_display istniejący tylko u klienta** (packetevents). Zwykły pasek i domyślnie modelowany jadą
  jako pasażerowie `SET_PASSENGERS`; `modelengine.tryb: podazanie` tworzy niezależny wyświetlacz nad modelem i
  przesuwa go co 2 ticki. Wysokość modelu pochodzi z głównego hitboxa blueprintu i skali. Serwer nie zna
  wyświetlacza → nie blokuje teleportów MM (`Entity#teleport` odmawia z pasażerem), AI ani zapisu chunków.
  Spawn przy `PlayerTrackEntityEvent` (tick później), usunięcie przy untrack/śmierci.
- Vanillowa nazwa moba (wisiałaby drugi raz po najechaniu celownikiem) jest wycinana z wychodzących metadanych
  (indeksy 2/3) tylko dla mobów z paskiem; serwer dalej ma prawdziwą nazwę (MM, wiadomości śmierci).
- **Znaki nie mogą na siebie zachodzić** (zmierzone 01.10 klientem QA: cyfra HP narysowana na tle ramki wychodziła
  przyciemniona — klient rysuje znaki w nieustalonej kolejności). Pasek i plakietka to kafle stykające się krawędziami,
  liczby HP stoją obok paska.
- Grafika: czcionka `paskimobow:pasek` z `generuj.py` (źródło prawdy kodów znaków i szerokości; `Wyglad.java`
  ma te same stałe). Kursor liczony w pluginie, przesunięcia znakami spacji U+F000+dx+128.
- Konfiguracja: `wyglad.kolor-hp` = `stopnie | czerwony`, `wyglad.poza-walka` = `wszystko | nazwa | nic`,
  `wyglad.walka-sekundy` = 8, `modelengine.wlaczone` = true, `modelengine.tryb` = `pasazer | podazanie`.
  `/paskimobow info` pokazuje tryb i odczytaną wysokość modelu.
- Krwawienie AnimacjeBroni czytane refleksją z prywatnej mapy `krwawiacy` (tylko klucze); brak pola = ostrzeżenie
  w logu i brak tej ikony, reszta działa.

## Budowa i test

```
python3 generuj.py          # czcionka -> wynik/pack/assets/paskimobow + wynik/podglad.png (x8)
./build.sh                  # jar w build/libs (gradle w dockerze)
qa/klon.sh <katalog>        # klon world1 pm-test (sieć 10.99.47.0/24) z naszym jarem/configiem/czcionką
docker run -d --name mc-client-pm --network pm-net -u 1000:1000 \
  -v /home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client:/mc:ro -v <review>:/review \
  mc-client-test:latest bash /review/launch.sh        # launch.sh = qa/launch.sh
qa/scena.sh                 # rzadkości, statusy, cios, spokój, cztery kolory HP i model -> zrzuty
docker stop pm-test mc-client-pm; docker network rm pm-net
```

Wdrożenie na world1: nowy plugin = restart (przy 0 graczy). Pliki: jar → `plugins/`, `config.yml` →
`plugins/PaskiMobow/`, `wynik/pack/assets/paskimobow` → `plugins/Nexo/pack/assets/` (pack Nexo składa się przy starcie).
Komendy: `/paskimobow przeladuj` (config), `/paskimobow info` (mob pod celownikiem).

## Zmierzone na klonie 01.10 (klient QA)

Pasek, ikony rzadkości, plakietka, duch obrażeń, ikona trucizny, liczby (też 656/1024 bossa) i brak podwójnej
nazwy pod celownikiem — działa; w logu zero błędów PaskiMobow. Przy okazji (nie z tego pluginu): wilki MM mają na
klonie max HP 8 zamiast 55–170 z configu, a Władca Krypty 1024 zamiast 1100 (limit atrybutu max_health w Paperze).
Pułapki klonu: powierzchnia płaskiego świata to y=-59; gamerule w 1.21.11 to `spawn_mobs` (nie doMobSpawning).

## Zmierzone na klonie 02.10 (klient QA, v0.1.0-202610020835)

Kolor HP: 4 stopnie (czerwony/pomaranczowy/zolty/zielony przy 10/25/50/80%) widoczne poprawnie z duchem obrazen.
Poza walka: w spokoju nazwa + plakietka, po ciosie pelny pasek, po 8 s znow bez paska (Iskrzyk, bez trucizny;
trucizna i inne obrazenia w czasie slusznie trzymaja moba "w walce"). ModelEngine (CHW_ISKRZYK_D dopisany do
`wzorce` tylko na klonie): tryb `pasazer` - pasek nad modelem; tryb `podazanie` - pasek idzie za chodzacym mobem.
Zero bledow PaskiMobow w logu.
