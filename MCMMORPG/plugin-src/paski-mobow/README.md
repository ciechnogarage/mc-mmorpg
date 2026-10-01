# PaskiMobow — pasek nad głową moba (2026-10-01)

Nad mobem bojowym wisi:

```
   [ikony statusów]            ← tylko gdy jakiś status trwa
 ◆ Nazwa moba                  ← kolor rzadkości, ikona rzadkości (klejnot / korona / gwiazda)
 [12] ███████████░░░ 89/140    ← plakietka poziomu w kolorze rzadkości, HP + jasny "duch" obrażeń
```

- **Rzadkość** (drabina jak w przedmiotach): Pospolity (szary), Magiczny (niebieski, klejnot), Rzadki (żółty,
  klejnot), Epicki (fioletowy, korona, pogrubiony), Legendarny (pomarańczowy, gwiazda, pogrubiony).
- **Poziom i rzadkość** wg id MythicMobs w `config.yml` (`moby:`, glob, pierwszy pasujący); brak wpisu = automat:
  BossBar → legendarny, pogrubiona nazwa → rzadki, reszta pospolity, poziom 1. Poziom MM > 1 (spawnery) dochodzi.
- **Statusy** (ikony 9×9): ogień, trucizna, krwawienie (AnimacjeBroni), obumarcie, spowolnienie, osłabienie,
  oślepienie/ciemność, zamrożenie, siła, odporność, regeneracja, szybkość, absorpcja.
- **Duch obrażeń**: po ciosie stracone HP świeci jasno ~0,6 s i zjeżdża do czerwieni.

Kto ma pasek: moby MM z frakcji `bestie, nieumarli, humanoidy, magiczne, gaj` + `WIEZA_P01_*`, zwykłe wrogie
moby vanilla. Bez paska: NPC, węzły zasobów, pieczęcie, sługi Akolity, Chowańce (własny HUD), modele ModelEngine.

## Jak to działa

- Pasek to **text_display istniejący tylko u klienta** (packetevents), doczepiony do moba pakietem
  SET_PASSENGERS. Serwer go nie zna → nie blokuje teleportów MM (`Entity#teleport` odmawia z pasażerem), AI ani
  zapisu chunków. Spawn przy `PlayerTrackEntityEvent` (tick później), usunięcie przy untrack/śmierci.
- Vanillowa nazwa moba (wisiałaby drugi raz po najechaniu celownikiem) jest wycinana z wychodzących metadanych
  (indeksy 2/3) tylko dla mobów z paskiem; serwer dalej ma prawdziwą nazwę (MM, wiadomości śmierci).
- Grafika: czcionka `paskimobow:pasek` z `generuj.py` (źródło prawdy kodów znaków i szerokości; `Wyglad.java`
  ma te same stałe). Kursor liczony w pluginie, przesunięcia znakami spacji U+F000+dx+128.
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
qa/scena.sh                 # moby każdej rzadkości, statusy, cios -> zrzuty
docker stop pm-test mc-client-pm; docker network rm pm-net
```

Wdrożenie na world1: nowy plugin = restart (przy 0 graczy). Pliki: jar → `plugins/`, `config.yml` →
`plugins/PaskiMobow/`, `wynik/pack/assets/paskimobow` → `plugins/Nexo/pack/assets/` (pack Nexo składa się przy starcie).
Komendy: `/paskimobow przeladuj` (config), `/paskimobow info` (mob pod celownikiem).
