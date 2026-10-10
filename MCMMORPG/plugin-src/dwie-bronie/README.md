# DwieBronie

Walka dwiema bronia na wzor Skyrima (wlasciciel 10.10). Dziala, gdy w obu dloniach jest prawdziwa bron
MMOItems (Ksiega: wybrany slot 1 = prawa, off-hand = lewa) i postac spelnia wymagania lewej.

| Skyrim (UESP: Combat, One-handed) | U nas |
|---|---|
| przycisk = reka | LPM naprzemiennie prawa/lewa (wybor wlasciciela), PPM = lewa |
| przytrzymanie = mocny cios x2, stamina, zachwianie | przytrzymany PPM (>=12 t): lewa uniesiona (poza TRIDENT), mocny x2, 12% staminy, zachwianie; stukniecie (<6 t) = zwykly cios lewa |
| oba przyciski = podwojny mocny: 3 ataki (2L+1P) po x1,5, 0,5 staminy kazdy | Shift + przytrzymany PPM: 1,5x(2L+P) jednym trafieniem, 3 zamachy L/P/L, 18% staminy |
| brak staminy = zwykly cios | to samo + komunikat "Brakuje staminy" |
| dwie bronie ~ +33% DPS | +33% szybkosci ataku przy dwoch broniach (`premia-szybkosci-ataku`) |
| brak bloku przy dwoch broniach | blok AnimacjeBroni wymaga pustej lewej - zgodne |

Mechanika: MMOItems liczy obrazenia tylko z glownej reki, wiec cios lewa skaluje obrazenia proporcja
ATTACK_DAMAGE lewa/prawa (glowny cel, ten tick). PPM uzywa off-handu: lewa dostaje CONSUMABLE (klient
uzywa off-handu, gdy glowna reka nie ma uzycia); plugin pilnuje, by bron poza off-handem go nie miala.
Inni gracze przy ciosie lewa widza machniecie lewa (machniecie prawa wycinane); sam gracz przy LPM-lewej
widzi prawa (animacja lokalna klienta).

Ograniczenia: efekty on-hit przy ciosie lewa sa z prawej broni; brak wariantow kierunkowych mocnego lewa.

Build `./build.sh`; testy `eval-dwie2.js` (v2, 9 przypadkow) i `eval-dwie.js` (v1 - jego PPM uzywa glownej
reki, w v2 nieaktualny); wdrozenie `./wdroz.sh` + restart world1.
