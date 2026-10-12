# Mapa swiata (world1) — bez pluginu

Dwie rzeczy dla gracza, obie z tego samego obrazu terenu:

1. **Mapa na pasku (slot 7, klawisz 8).** Vanillowy `filled_map` z `map_id=30000`. Obraz siedzi w
   `world/data/map_30000.dat` (zablokowana mapa, `locked=1`), wiec serwer nic na niej nie rysuje, ale
   **strzalka gracza dziala** (srodek 240/140, skala 3 = 1024 bloki), a znaczniki to banery z polskimi
   nazwami (pole `banners` w pliku). Klik mapa = duze okno.
2. **Okno pod Q** (Q -> Przygoda -> Mapa swiata). Okno DeluxeMenus 6 rzedow, tlo = glif Nexo
   `mapa_swiata_bg` (U+4180) z obrazem terenu w siatce 9x6; 1 slot = 148x148 blokow. Znaczniki = ikony
   gatunkow/bossow (`item_model nexo:chw_*`), "Tu jestes" = kompas w slocie gracza (warunki PAPI).

## Potok

    python3 teren.py <world/region> teren.json.gz          # powierzchnia z plikow regionow (tylko odczyt, ~20 s)
    python3 render.py teren.json.gz render/                # kolory map vanilli, 1 blok = 1 px
    NAZWY=tekst python3 mapa_dat.py render/ znaczniki.json 30000 240 140 3 out/   # map_30000.dat
    python3 okno.py render/ znaczniki.json out/            # panel PNG + glif + menu + testpack klienta QA
    wdrozenie/wdroz.sh                                     # world1, bez restartu; wypisuje cofnij.sh

`znaczniki.json` = jedno zrodlo znacznikow dla mapy w rece i okna.

## Pulapki (zmierzone 2026-10-01 na klonie mapa-proba + kliencie QA)

- Wiki podaje stary schemat banerow (`Pos/Color/Name`). 1.21.11 przyjmuje TYLKO `pos` (int[3]), `color`,
  `name`; stary schemat = "Failed to parse saved data ... No key pos" i mapa bez znacznikow.
- Plik mapy wkladac pod id, ktorego serwer jeszcze nie wczytal (albo przy zatrzymanym serwerze). Serwer
  nie nadpisuje pliku, ktorego nie zmienil (sprawdzone: stop nie ruszyl plikow).
- Zmiana obrazu = NOWE id (np. 30001) + zmiana MID w wdroz.sh/latce: klienci i serwer trzymaja wczytana mape.
- Vanilla NIE pokazuje obrazu mapy w podgladzie (tooltip) ekwipunku - tylko w rece / w ramce.
- CoreTools przepuszcza w `command{c="..."}` klamry, apostrofy i nawiasy (sonda) - stad `item modify` inline.
- W CoreTools `X trueElse S` = gdy X prawdziwe -> uruchom S i KONIEC. Nowy przypadek dopisywac osobna linia.
- Banery jako ikony w oknie to plaskie prostokaty - stad ikony gatunkow z packa.
