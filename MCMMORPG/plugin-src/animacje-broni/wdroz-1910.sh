#!/bin/bash
# Wdrozenie poprawek 19.10 (galaz bronie-krwawienie-naladowanie): bron nie miga przy WASD/sprincie, naladowanie i nietykalnosc
# po dodatkowych trafieniach, obrot bez celu na wprost, koszt lekkiego wg zamachu, bez vanillowego sweepa. Jar + config
# AnimacjeBroni, a przy zatrzymanym serwerze zdjecie 'on-attack' (rozprysk MMOItems) z 18 typow broni bialej (qa/bez_rozprysku.py).
#   wdroz-1910.sh [--sucho]     --sucho = tylko sprawdzenia, bez zmian
# Bezpieczniki: md5 jara, configu i item-types na world1 == stan po wdrozeniu postaw 18.10 01:49 (inaczej ktos
# wdrozyl cos nowszego -> scal najpierw), zero graczy (minecraft:list uuids, czeka do 60 min), kopia + cofnij.sh, log.
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins; K=mc-world1-1
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AB=$(ls "$H"/build/libs/animacje-broni-*.jar | head -1)
CFG=$H/src/main/resources/config.yml
TYPY=$P/MMOItems/item-types.yml
OCZEKIWANE_AB=67df4aa7566d8da4535ca50c3422ce56    # animacje-broni-0.1.0-202610180133 (postawy 18.10)
OCZEKIWANE_CFG=efd7131e2ec9af8772b4870d883765c4   # config postaw 18.10
OCZEKIWANE_TYPY=bb8103f36731bf9a9948427d2bfe60d3  # item-types.yml po zdjeciu rozprysku kosy 18.10
SUCHO=0; [[ "${1:-}" == --sucho ]] && SUCHO=1

sprawdz_stan() {
  local ab cfg typy
  ab=$(md5sum "$P"/animacje-broni-*.jar | awk '{print $1}')
  cfg=$(md5sum "$P/AnimacjeBroni/config.yml" | awk '{print $1}')
  typy=$(md5sum "$TYPY" | awk '{print $1}')
  [[ "$ab" == "$OCZEKIWANE_AB" && "$cfg" == "$OCZEKIWANE_CFG" && "$typy" == "$OCZEKIWANE_TYPY" ]] || {
    echo "STOP: stan world1 inny niz baza (jar $ab, config $cfg, item-types $typy) - scal nowsze zmiany"; exit 3; }
}
gracze() { docker exec "$K" rcon-cli 'minecraft:list uuids' | sed 's/\x1b\[[0-9;]*m//g' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+$'; }
bez_rozprysku() { python3 "$H/qa/bez_rozprysku.py" "$1"; }

sprawdz_stan
echo "world1 = baza: OK; nowy: $(basename "$AB")"
(( SUCHO )) && { echo "SUCHO: gracze online = $(gracze)"; exit 0; }

for i in $(seq 1 60); do n=$(gracze); [[ "$n" == 0 ]] && break; echo "graczy online: $n - czekam 60 s"; sleep 60; done
[[ "$(gracze)" == 0 ]] || { echo "STOP: gracze nadal online po 60 min"; exit 3; }

B=/opt/docker/mc/backups/bronie1910-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
cp -a "$P"/animacje-broni-*.jar "$B/"; cp -a "$P/AnimacjeBroni/config.yml" "$B/animacje-config.yml"; cp -a "$TYPY" "$B/item-types.yml"
cat > "$B/cofnij.sh" <<EOC
#!/bin/bash
# Cofniecie: zatrzymuje world1, przywraca jar i config AnimacjeBroni oraz MMOItems/item-types.yml sprzed wdrozenia, uruchamia.
set -e
docker stop $K
rm -f "$P"/animacje-broni-*.jar
cp -a "$B"/animacje-broni-*.jar "$P/"
cp -a "$B/animacje-config.yml" "$P/AnimacjeBroni/config.yml"
cp -a "$B/item-types.yml" "$TYPY"
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
bez_rozprysku "$TYPY"
docker start "$K" >/dev/null
for _ in $(seq 1 150); do
  I1=$(stat -c %i "$W/logs/latest.log" 2>/dev/null || echo 0)
  [[ "$I1" != "$I0" ]] && grep -q 'Done (' "$W/logs/latest.log" && break; sleep 4
done
grep -q 'Done (' "$W/logs/latest.log" || { echo "UWAGA: brak 'Done' po 10 min - sprawdz log; cofnij: $B/cofnij.sh"; exit 1; }
echo "--- log (AnimacjeBroni/MMOItems/Exception):"
grep -E '\[AnimacjeBroni\]|\[MMOItems\].*(error|Could not)|Exception' "$W/logs/latest.log" | grep -vE 'Loading' | head -20 || true
echo "WDROZONE. Kopia: $B  cofnij: $B/cofnij.sh"
