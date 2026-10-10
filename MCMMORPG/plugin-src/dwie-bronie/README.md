# DwieBronie

Walka dwiema bronia (wlasciciel 10.10). Dziala, gdy w obu dloniach jest prawdziwa bron MMOItems
(Ksiega: wybrany slot 1 = prawa, off-hand = lewa) i postac spelnia wymagania lewej.

- LPM: ciosy naprzemiennie prawa / lewa (przerwa > `okno-serii-tickow` = znow od prawej).
- PPM: cios lewa w cel pod celownikiem (przy naladowaniu >= `min-naladowanie-ppm`).
- Cios lewa: obrazenia x (ATTACK_DAMAGE lewej / prawej), tylko glowny cel; lewa reka macha
  (inni nie widza machniecia prawa). U samego gracza LPM-lewa pokazuje zamach PRAWA reka - to
  animacja lokalna klienta, serwer jej nie zmieni.
- Slot 2 (klon w off-handzie) i jedna bron: plugin nic nie robi; mocny cios AnimacjeBroni zostaje
  tylko przy pustej lewej.
- Efekty przy trafieniu (zywioly, umiejetnosci) przy ciosie lewa sa z PRAWEJ broni (MMOItems liczy
  glowna reke).

Build: `./build.sh`; test: `eval-dwie.js` (harness character-creator/eval, klon world1); wdrozenie: `./wdroz.sh` + restart.
