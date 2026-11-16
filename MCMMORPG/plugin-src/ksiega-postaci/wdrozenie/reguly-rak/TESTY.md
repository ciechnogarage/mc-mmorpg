# Testy EQ przeniesione (21.10)

`eval-reguly.js`, `eval-miecz-startowy.js`, `probki.txt` zyja teraz w jednym miejscu:
`/opt/docker/mc/narzedzia/bot/scenariusze/eq/` (repo git `narzedzia`, commit a90e504) jako `reguly.js`,
`miecz-startowy.js`, `probki.txt`, na wspolnym bocie (`narzedzia/bot/run.sh`).

Calosc EQ przed wdrozeniem ksiega-postaci (klon `--z-kreatorem`):
`/opt/docker/mc/narzedzia/bot/scenariusze/eq/wszystko.sh <klon>`

`eval-macierz.js` i `eval-obejscia.js` NIE sa przeniesione: uzywaja zakazanego harnessu `/e` (hook hosta);
macierz pokrywa `reguly.js`. Stare wersje plikow: `git log -- <sciezka>` w tym repo.
