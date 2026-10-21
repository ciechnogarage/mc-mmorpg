#!/bin/bash
# Wdrozenie 19.10 popoludnie (galaz bronie-krwawienie-naladowanie): kopanie bloku nie jest ciosem, martwe klikniecia przed
# progiem naladowania broni, mocny cios z rachunku (x2,0-2,2 za 2,0 L), kierunek lekkiego (smuga i krok), modele kierunkowe
# w pierwszej osobie (keybind_down). Jar + config AnimacjeBroni + pack Nexo assets/bron (nowe modele s???k?).
#   wdroz-1910b.sh [--sucho]     --sucho = tylko sprawdzenia, bez zmian
# Bezpieczniki: md5 jara, configu i packa bron na world1 == stan po wdroz-1910.sh (09:12), zero graczy, kopia + cofnij.sh.
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins; K=mc-world1-1
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AB=$(ls "$H"/build/libs/animacje-broni-*.jar | head -1)
CFG=$H/src/main/resources/config.yml
PACK=$P/Nexo/pack/assets/bron
GEN=${GEN:-$H/wynik}   # wynik generuj.py (config.yml + pack/assets/bron)
OCZEKIWANE_AB=06dc3136c24cdb94d29df98714cf99d3     # animacje-broni-0.1.0-202610190857 (wdroz-1910.sh)
OCZEKIWANE_CFG=758fc0a4f184a52332dcc50ced9e7cbd
OCZEKIWANE_PACK=1f65ab97721da4bcf3ee0ed491b0a8f8    # md5 listy md5 plikow assets/bron
SUCHO=0; [[ "${1:-}" == --sucho ]] && SUCHO=1

md5_packa() { (cd "$PACK" && find . -type f | sort | xargs md5sum | md5sum | cut -c1-32); }
sprawdz_stan() {
  local ab cfg pk
  ab=$(md5sum "$P"/animacje-broni-*.jar | awk '{print $1}'); cfg=$(md5sum "$P/AnimacjeBroni/config.yml" | awk '{print $1}'); pk=$(md5_packa)
  [[ "$ab" == "$OCZEKIWANE_AB" && "$cfg" == "$OCZEKIWANE_CFG" && "$pk" == "$OCZEKIWANE_PACK" ]] || {
    echo "STOP: stan world1 inny niz baza (jar $ab, config $cfg, pack $pk) - scal nowsze zmiany"; exit 3; }
}
gracze() { docker exec "$K" rcon-cli 'minecraft:list uuids' | sed 's/\x1b\[[0-9;]*m//g' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+$'; }

[[ -f "$GEN/pack/assets/bron/items/s100/iron_sword.json" ]] || { echo "STOP: brak wygenerowanego packa w $GEN"; exit 2; }
cmp -s "$GEN/config.yml" "$CFG" || { echo "STOP: $GEN/config.yml != src/main/resources/config.yml - przegeneruj"; exit 2; }
sprawdz_stan
echo "world1 = baza: OK; nowy: $(basename "$AB")"
(( SUCHO )) && { echo "SUCHO: gracze online = $(gracze)"; exit 0; }

for i in $(seq 1 60); do n=$(gracze); [[ "$n" == 0 ]] && break; echo "graczy online: $n - czekam 60 s"; sleep 60; done
[[ "$(gracze)" == 0 ]] || { echo "STOP: gracze nadal online po 60 min"; exit 3; }

B=/opt/docker/mc/backups/bronie1910b-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
cp -a "$P"/animacje-broni-*.jar "$B/"; cp -a "$P/AnimacjeBroni/config.yml" "$B/animacje-config.yml"; cp -a "$PACK" "$B/pack-bron"
cat > "$B/cofnij.sh" <<EOC
#!/bin/bash
# Cofniecie: zatrzymuje world1, przywraca jar i config AnimacjeBroni; nowy pack bron odsuwa (mv) i przywraca kopie; uruchamia.
set -e
docker stop $K
rm -f "$P"/animacje-broni-*.jar
cp -a "$B"/animacje-broni-*.jar "$P/"
cp -a "$B/animacje-config.yml" "$P/AnimacjeBroni/config.yml"
mv "$PACK" "$B/pack-bron-odrzucony"
cp -a "$B/pack-bron" "$PACK"
docker start $K
echo "cofniete i uruchomione: $K"
EOC
chmod +x "$B/cofnij.sh"

I0=$(stat -c %i "$W/logs/latest.log")
docker stop "$K" >/dev/null
sprawdz_stan   # tuz przed podmiana (pamiec: wdrozenie-cofa-inne-galezie)
rm -f "$P"/animacje-broni-*.jar
cp "$AB" "$P/"
cp "$CFG" "$P/AnimacjeBroni/config.yml"
cp -r "$GEN/pack/assets/bron/." "$PACK/"
docker start "$K" >/dev/null
for _ in $(seq 1 150); do
  I1=$(stat -c %i "$W/logs/latest.log" 2>/dev/null || echo 0)
  [[ "$I1" != "$I0" ]] && grep -q 'Done (' "$W/logs/latest.log" && break; sleep 4
done
grep -q 'Done (' "$W/logs/latest.log" || { echo "UWAGA: brak 'Done' po 10 min - sprawdz log; cofnij: $B/cofnij.sh"; exit 1; }
for _ in $(seq 1 60); do grep -q 'Nexo | ResourcePack has been uploaded' "$W/logs/latest.log" && break; sleep 3; done
echo "--- log (AnimacjeBroni/Nexo/Exception):"
grep -E '\[AnimacjeBroni\]|Nexo \| (ResourcePack has|Finished)|Nexo.*(rror|nvalid)|Exception' "$W/logs/latest.log" | grep -vE 'Loading' | head -20 || true
echo "WDROZONE. Kopia: $B  cofnij: $B/cofnij.sh"
