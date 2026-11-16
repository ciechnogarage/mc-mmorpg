# Potok modeli Bagien

## Stan 03.10: piątka L002 gotowa, odłożone: rechotnik, slimak, ognik, krol_ropuch

Loch 002 obejmuje Pijawkę, Mułowca, Kościeja, Szeptuchę i Kokoniarę. Kokoniątko występuje wyłącznie jako Wylęg Kokoniary. Rechotnik, Ślimak, Ognik i Król Ropuch są odłożone; ich pliki pozostają w katalogu modeli bez dalszych prac w tym zakresie.

Ścieżki poniżej są liczone od `/opt/docker/mc/data/world1`. Wyniki modeli są w `output/lochy/bagna/modele/<id>/`, a staging MM w `output/lochy/bagna/mm/`. Nie edytuj ręcznie `.bbmodel`: generator ponownie nadaje UUID i zapisuje geometrię, potem `anim_<id>.py` nakłada ruch.

## 1. Koncept → orto → generator

Karta moba (`output/lochy/bagna/KARTY.md`) i szkice (`output/lochy/bagna/szkice/`) określają sylwetkę, kolor i słaby punkt. Orto sprawdza przód, bok, tył i wysokość oczu gracza; zachowany przykład: `szkice/rechotnik_orto.png`. Wybrany wariant przenieś do `tools/modele/<id>.py`.

`bryly_sdf.Model` buduje kości i prymitywy (`elipsoida`, `pudlo`, `kapsula`, `jajo_oplot`, `spirala_skorupy`, `noga_stawowa`, `plomien`); `segment` prowadzi obróconą kość między punktami świata, `obrot_kosci` zapisuje obrót BB w kolejności Rz·Ry·Rx, `dosun_detale` łączy drobne elementy, `uprosc` redukuje woksele. `malarz` nakłada AO tylko na bocznych ścianach o wysokości co najmniej 4 px. `Model.zapisz` wywołuje `bb.waliduj`.

Sprawdzone lokalnie dla trzech modeli:

```bash
cd /home/dev/Desktop/Przemek/MC/tools/modele
python3 kokoniara.py
python3 mulowiec.py
python3 szeptucha.py
python3 anim_kokoniara.py
python3 anim_mulowiec.py
python3 anim_szeptucha.py
```

Generatory tych trzech modeli same wywołują nakładkę animacji; osobne komendy `anim_*` można wykonać po zmianie samego ruchu. `mulowiec.py` i `szeptucha.py` korzystają ze zgodnego importu `segment` z `kokoniara.py`.

## 2. Bramki i render

```bash
cd /opt/docker/mc/data/world1
python3 tools/bbrender/bb_spojnosc.py output/lochy/bagna/modele/pijawka/bagna_pijawka.bbmodel
python3 tools/bbrender/arkusz_bagna.py --ids kokoniara
python3 tools/bagna/kontrakt.py
```

`bb_spojnosc.py` sprawdza styk całości w spoczynku i animacjach oraz nazwy z kontraktu; raport dla pięciu mobów L002 i Kokoniątka: `output/lochy/bagna/_zespol/AUDYT_MODELI.md`. Rozłączność wewnątrz kości i geometria bez własnego kanału są INFO; orbitujące iskry Ognika mają wyjątek. `arkusz_bagna.py` uruchamia `render_bbmodel.py` z `WIDOKI=145:20,90:10,-35:20,145:5` i składa koncept, przód 3/4, bok 90°, tył 3/4 oraz widok z oczu gracza.

Bramka ziemi wymaga działającej sesji Blockbench (`bb_sesja.py`); wywołanie ma postać:

```bash
python3 tools/bbrender/bb_ziemia.py output/lochy/bagna/modele/kokoniara/bagna_kokoniara.bbmodel --dozwolone spawn,death
```

W tym przebiegu sesja Blockbench nie odpowiedziała, więc wynik tej bramki pozostaje niepotwierdzony. `bb.waliduj` przeszedł podczas trzech powyższych eksportów, a kontrakt i spójność uruchomiono lokalnie.

## 3. MM → klon bagna-test → arena

`tools/bagna/mm_bagna.py` generuje YAML do stagingu i sprawdza stany, dźwięki, cele oraz nazwy; uruchamiaj przez sprawdzony `tools/bagna/uruchom.sh` (kontener dostarcza PyYAML):

```bash
cd /home/dev/Desktop/Przemek/MC/tools/bagna
./uruchom.sh
```

Do próby gry służą skrypty `tools/bagna/klon/`: `przygotuj.sh` kopiuje konfigurację, `start.sh` uruchamia osobny `bagna-test`, a `wdroz.sh` przenosi staging **wyłącznie na klon** i przeładowuje jego ME/Nexo/MM. Po postawieniu szablonu areny `tools/bagna/arena/uruchom.sh <MOB> [nieuwazny|uwazny] [POWT] [ILE]` mierzy walkę; `klon/stop.sh` kończy próbę. Etapu klonu i areny nie uruchomiono w tym odbiorze, zgodnie z poleceniem, by nie ruszać serwera.
