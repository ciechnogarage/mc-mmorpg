# Dźwięki kozłka (feeling mmo/rytuał): źródła i licencja

Wszystkie pliki w `ogg/kozlek/` są **syntezą własną** projektu (`synteza.py`, numpy/scipy, ziarno losowe 20261024).
Nie zawierają cudzych próbek. Licencja: **CC0 1.0** (https://creativecommons.org/publicdomain/zero/1.0/).
Próbki vanilli (assets klienta MC) służą tylko jako **wzorzec głośności** (pomiar RMS), nie trafiają do plików.

| Klucz (`zielarz:` w paczce) | Pliki | Co to jest | Zastępuje (tylko mmo/rytuał) | Źródło / licencja |
|---|---|---|---|---|
| `kozlek.szelest` | szelest1-3.ogg | trzaski liści 2-7 kHz + szum ruchu | `block.azalea_leaves.step` (wejście, ciągnięcie w paśmie) | synteza własna, CC0 |
| `kozlek.napiecie` | napiecie1-3.ogg | stick-slip łodygi (impulsy 30→85/s przez rezonanse 0,5/1,1/2,3 kHz) | `block.bamboo.hit` (za mocno), `block.bamboo.break` (trzeszczy) | synteza własna, CC0 |
| `kozlek.wyrwanie` | wyrwanie1-3.ogg | tąpnięcie 72→44 Hz, pękające korzonki, mlaśnięcie (rezonans 620→170 Hz), osypująca się ziemia | `block.rooted_dirt.break` (wyrwanie, urwanie niżej o 0,7) | synteza własna, CC0 |
| `kozlek.wyborny` | wyborny1.ogg | dzwonek drewniano-szklany A5-C#6-E6(-A6), miękki pogłos | `block.amethyst_block.chime` + `entity.player.levelup` (nagroda wybornego) | synteza własna, CC0 |

Głośność: każdy plik ma RMS części aktywnej równy odpowiednikowi vanilli przy tej samej głośności wywołania w kodzie
(różnica 0 do -0,4 dB, szczyt ≤ -1 dBFS). Pomiar: `dzwieki.sh <katalog>` → `pomiar.json`.
Regeneracja: `wyglad/dzwieki/dzwieki.sh` (kontener python:3.12-slim). Do paczki: `przygotuj_klon.sh` kopiuje
`ogg/kozlek/*.ogg` → `Nexo/pack/assets/zielarz/sounds/kozlek/` i `sounds.json` → `Nexo/pack/assets/zielarz/sounds.json`.
