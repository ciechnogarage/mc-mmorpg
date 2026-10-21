#!/bin/bash
# Wdrozenie poprawki krwawienia (galaz bronie-krwawienie-naladowanie): sam jar AnimacjeBroni, config bez zmian.
# Takt krwawienia (DOT MythicLib = playerAttack) zerowal graczowi naladowanie ciosu -> kosa 7/10, topor 3/10 ciosow w powietrze.
#   wdroz-krwawienie.sh [--sucho]     --sucho = tylko sprawdzenia, bez zmian
# Bezpieczniki: md5 jara na world1 == jar po wdrozeniu staminy 17.10 (inaczej ktos wdrozyl cos nowszego -> scal najpierw),
# zero graczy (minecraft:list uuids, czeka do 60 min), kopia + cofnij.sh, kontrola startu i logu.
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins; K=mc-world1-1
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AB=$(ls "$H"/build/libs/animacje-broni-*.jar | head -1)
OCZEKIWANE_AB=2bd595e0e1b1dc4bb7b10d78f95e95f8   # animacje-broni-0.1.0-202610171705 (galaz stamina, world1 od 17.10 23:03)
SUCHO=0; [[ "${1:-}" == --sucho ]] && SUCHO=1

sprawdz_stan() {
  local ab
  ab=$(md5sum "$P"/animacje-broni-*.jar | awk '{print $1}')
  [[ "$ab" == "$OCZEKIWANE_AB" ]] || { echo "STOP: jar AnimacjeBroni na world1 inny niz baza galezi ($ab) - scal nowsze zmiany i przebuduj"; exit 3; }
}
gracze() { docker exec "$K" rcon-cli 'minecraft:list uuids' | sed 's/\x1b\[[0-9;]*m//g' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+$'; }

sprawdz_stan
echo "jar world1 = baza galezi: OK; nowy: $(basename "$AB")"
(( SUCHO )) && { echo "SUCHO: gracze online = $(gracze)"; exit 0; }

for i in $(seq 1 60); do n=$(gracze); [[ "$n" == 0 ]] && break; echo "graczy online: $n - czekam 60 s"; sleep 60; done
[[ "$(gracze)" == 0 ]] || { echo "STOP: gracze nadal online po 60 min"; exit 3; }

B=/opt/docker/mc/backups/krwawienie-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
cp -a "$P"/animacje-broni-*.jar "$B/"
cat > "$B/cofnij.sh" <<EOC
#!/bin/bash
# Cofniecie poprawki krwawienia: zatrzymuje world1, przywraca jar AnimacjeBroni sprzed wdrozenia, uruchamia.
set -e
docker stop $K
rm -f "$P"/animacje-broni-*.jar
cp -a "$B"/animacje-broni-*.jar "$P/"
docker start $K
echo "cofniete i uruchomione: $K"
EOC
chmod +x "$B/cofnij.sh"

I0=$(stat -c %i "$W/logs/latest.log")
docker stop "$K" >/dev/null
sprawdz_stan   # tuz przed podmiana (pamiec: wdrozenie-cofa-inne-galezie)
rm -f "$P"/animacje-broni-*.jar
cp "$AB" "$P/"
docker start "$K" >/dev/null
for _ in $(seq 1 150); do
  I1=$(stat -c %i "$W/logs/latest.log" 2>/dev/null || echo 0)
  [[ "$I1" != "$I0" ]] && grep -q 'Done (' "$W/logs/latest.log" && break; sleep 4
done
grep -q 'Done (' "$W/logs/latest.log" || { echo "UWAGA: brak 'Done' po 10 min - sprawdz log; cofnij: $B/cofnij.sh"; exit 1; }
echo "--- log (AnimacjeBroni/Exception):"
grep -E '\[AnimacjeBroni\]|Exception' "$W/logs/latest.log" | grep -vE 'Loading' | head -20 || true
echo "WDROZONE. Kopia: $B  cofnij: $B/cofnij.sh"
