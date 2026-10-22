# Generatory wlasnych modeli ModelEngine

Model = skrypt Pythona, ktory sklada `.bbmodel` (geometria + tekstura malowana + animacje). Zmiana modelu = zmiana skryptu
i przebudowa, nigdy reczna edycja pliku wynikowego.

    tools/modele/zbuduj.sh <model>      # pniaczek | korzeniak | korowiec | muchomorzec | purchawka | purchawiec | cierniowy_lucznik | korzeniak_strefa
    -> output/lochy/modele-wlasne/<model>/{<model>.bbmodel, podglad.png, klatki.png}
    tools/modele/podglad.sh <model>     # same arkusze dla gotowego .bbmodel (bez generatora)

**Przywrocone wersje (2026-09-26):** pniaczek, korzeniak, korowiec, purchawka, purchawiec to 1:1 wersje W1-W3 z archiwum,
ktore wlasciciel ocenil jako ladne; przerobki z generatorow (03:00) odrzucil ("wygladaja okropnie"). Ich katalogi maja
`PRZYWROCONE.txt` i `zbuduj.sh` ich nie nadpisze (chyba ze `NADPISZ=1`). Generatory tych modeli odtwarzaja ODRZUCONA
przerobke - poprawki robic na przywroconym pliku (`z_pliku.py`), bez przemalowywania tekstury. Odrzucone:
`_archiwum/przerobki_20260926_0300/`.

Wymaga Blockbencha z MCP (`tools/blockbench/start-desktop.sh`, ekran :1) - zrzuty i klatki ida przez `tools/bbrender/`.

| Plik | Co robi |
|---|---|
| `bb.py` | kostki/kosci, walidacja: zasady ME (obroty kostek, zbedne encje, hitbox X==Z), siatka 1:1 (>=1 px, bez odwroconych), zapis .bbmodel 4.10 |
| `tekstura.py` | atlas 128x128 (1 teksel = 1 px modelu), malowanie jak LR: gradient swiatla/AO, ciagly kolor, wzory (kora, pedy, brodawki...), naklejki (twarze malowane na scianie) |
| `animacje.py` | klatki opisane prawdziwym ruchem -> zapis w konwencji ME/BB4 |
| `pniaczek.py` | Pniaczek - roj, gremlin z zielonymi oczami (baza: archiwum korzeniak W1, przez `z_pliku.py`) |
| `korzeniak.py` | Korzeniak - strefa korzeni, jedno czerwone oko jak boss (baza: archiwum korzeniak W2) |
| `korowiec.py` | Korowiec - ciezki obronca z garda i korowymi tarczami (wlasny generator, dawny W3) |
| `korzeniak_strefa.py` | stojacy efekt strefy korzeni (przywolywany przez MM) |
| `muchomorzec.py` | Muchomorzec - zaklinacz kanalujacy oslone, fioletowy kapelusz (baza: archiwum purchawiec W1) |
| `purchawka.py` | Purchawka - maly kamikaze, peka chmura (baza: archiwum purchawiec W2) |
| `purchawiec.py` | Purchawiec - miotacz purchawek i chmury zarodnikow (wlasny generator, W3) |
| `cierniowy_lucznik.py` | Cierniowy Lucznik - wyglad W1 (ciemna kora, bursztynowe szparki), szkielet i animacje W3 |
| `malarz.py` | malarz pod ksztalt (bossowie, modele "od pomyslu"): 5 tonow bez szumu, ton z kierunku sciany, sloje w osi czesci, mech tylko od gory; `tekstura.atlas(..., malarz=malarz.maluj)` |
| `straznik_gaju.py` | Straznik Gaju - boss Lochu 001 (koncept K1): geometria z bryly etapu 2, atlas 512, maska i serce jako naklejki, kosci `swiatlo_*` |
| `bryly_wspolne.py`, `koncepty/` | szkice 2D (koncepty) i bryly w szarosciach (etap 2) bossow K1-K3; helpery konar/kepa/pasmo |
| `z_pliku.py` | generator na bazie istniejacego .bbmodel: zmiany kostek/kosci, przemalowanie, podmiana animacji |
| `dobij_wersje.py` | kopia z Blockbencha 5 -> format 4.10 (konwencja klatek ME), nazwa, hi_, podniesienie nad ziemie |
| `uruchom.sh` | uruchamia generator w obrazie pixelart (numpy+Pillow), paczka LR pod /modele tylko do odczytu |

Poprzeczka jakosci (zmierzona na zwyklych mobach LR): 30-70 kostek, 15-35 kosci, tekstura 128x128, 5-8 animacji.

Bramki w `zbuduj.sh` (budowa przerywa sie, gdy nie przejda): walidacja geometrii w `bb.py`; `bb_ziemia.py` - najnizszy
punkt modelu >= 0 w kazdej klatce kazdej animacji (wyjatki deklaruje generator w `POD_ZIEMIA`). Efekty `fx_*` (strefa
korzeni, strzala, pocisk) sa ukrywane skala 0 poza swoja animacja i w podgladzie spoczynkowym.

Dodatkowy pomiar (nie bramka): `python3 tools/bbrender/bb_spojnosc.py <plik.bbmodel>` - w kazdej klatce graf stykow
elementow: odlaczone kawalki (np. kapelusz nad glowa) i uniesienie stop nad ziemia ("WISI").

## Wymagania dla konfiguracji MythicMobs (przy wdrazaniu)

| Model | Co MM musi zrobic |
|---|---|
| wszystkie | kosci `swiatlo_*` rozjasnic mechanika ModelEngine `brightness` (display entity - dziala na kazdym kliencie; warstwa `_e` LittleRoom dziala tylko z OptiFine) |
| korzeniak | skill "strefa korzeni": stan `ukorzenienie` na mobie + przywolanie stojacego `korzeniak_strefa` (stany spawn -> idle -> death); srednica 24 px = 1.5 bloku (`SREDNICA` w generatorze) = promien slow |
| purchawiec | czasteczki zarodnikow z kosci `zarodniki` (szczyt komina) w klatce 0.62 s stanu `puff`; pocisk MM wypuscic w 1.0 s stanu `throw` (wtedy `fx_pocisk` znika) |
| cierniowy_lucznik | pocisk MM w 0.95 s stanu `shoot` (wtedy `fx_strzala` znika); `leap_back` nie przesuwa moba - odskok robi MM (velocity) |

Wersje W1-W3 z porownania i rozpakowane kopie Blockbencha: `output/lochy/modele-wlasne/_archiwum/` (bazy dla generatorow z `z_pliku.py` - nie kasowac).
