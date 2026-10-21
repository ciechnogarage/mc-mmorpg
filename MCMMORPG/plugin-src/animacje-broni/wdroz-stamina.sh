#!/bin/bash
# Wdrozenie modelu staminy (galaz stamina): AnimacjeBroni + DwieBronie + configi, a przy zatrzymanym serwerze
# usuniecie bazowego stamina-cost z przedmiotow MMOItems (demo, inaczej podwojny pobor staminy za cios).
#   wdroz-stamina.sh [--sucho]     --sucho = tylko sprawdzenia, bez zmian
# Bezpieczniki: md5 obecnych jarow i configow == stan sprzed galezi (inaczej ktos wdrozyl cos nowszego -> scal
# najpierw), zero graczy (minecraft:list uuids, czeka do 60 min), kopia + cofnij.sh, kontrola startu i logu.
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins; K=mc-world1-1
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; R="$(cd "$H/.." && pwd)"
AB=$(ls "$H"/build/libs/animacje-broni-*.jar | head -1)
DB=$(ls "$R"/dwie-bronie/build/libs/dwie-bronie-*.jar | head -1)
OCZEKIWANE_AB=8b5a2a7f47ab2fb137a572e3660d7809   # animacje-broni-0.1.0-202610030029 (= integracja-1210)
OCZEKIWANE_DB=85d09f3b49b4a60ab76809279d34d5f2   # dwie-bronie-0.1.0-202610121041 (v2.2)
SUCHO=0; [[ "${1:-}" == --sucho ]] && SUCHO=1

sprawdz_stan() {
  local ab db
  ab=$(md5sum "$P"/animacje-broni-*.jar | awk '{print $1}'); db=$(md5sum "$P"/dwie-bronie-*.jar | awk '{print $1}')
  [[ "$ab" == "$OCZEKIWANE_AB" && "$db" == "$OCZEKIWANE_DB" ]] || {
    echo "STOP: jary na world1 inne niz baza galezi stamina (animacje $ab, dwie $db) - scal nowsze zmiany i przebuduj"; exit 3; }
  # configi na world1 = configi bazy (git show integracja-1210) - inaczej ktos je recznie zmienil
  git -C "$R" show integracja-1210:MCMMORPG/plugin-src/animacje-broni/src/main/resources/config.yml | cmp -s - "$P/AnimacjeBroni/config.yml" \
    || { echo "STOP: AnimacjeBroni/config.yml na world1 rozni sie od bazy - sprawdz recznie"; exit 3; }
  git -C "$R" show integracja-1210:MCMMORPG/plugin-src/dwie-bronie/src/main/resources/config.yml | cmp -s - "$P/DwieBronie/config.yml" \
    || { echo "STOP: DwieBronie/config.yml na world1 rozni sie od bazy - sprawdz recznie"; exit 3; }
}
gracze() { docker exec "$K" rcon-cli 'minecraft:list uuids' | sed 's/\x1b\[[0-9;]*m//g' | grep -oE 'There are [0-9]+' | grep -oE '[0-9]+$'; }

sprawdz_stan
echo "jary i configi world1 = baza galezi: OK; nowe: $(basename "$AB"), $(basename "$DB")"
ITEMY=$(grep -lE '^    stamina-cost:' "$P"/MMOItems/item/*.yml || true)
echo "przedmioty z bazowym stamina-cost: $(echo "$ITEMY" | xargs -n1 basename 2>/dev/null | tr '\n' ' ')"
(( SUCHO )) && { echo "SUCHO: gracze online = $(gracze)"; exit 0; }

for i in $(seq 1 60); do n=$(gracze); [[ "$n" == 0 ]] && break; echo "graczy online: $n - czekam 60 s"; sleep 60; done
[[ "$(gracze)" == 0 ]] || { echo "STOP: gracze nadal online po 60 min"; exit 3; }

B=/opt/docker/mc/backups/stamina-$(date +%Y%m%d-%H%M%S); mkdir -p "$B/item"
cp -a "$P"/animacje-broni-*.jar "$P"/dwie-bronie-*.jar "$B/"
cp -a "$P/AnimacjeBroni/config.yml" "$B/animacje-config.yml"; cp -a "$P/DwieBronie/config.yml" "$B/dwie-config.yml"
for f in $ITEMY; do cp -a "$f" "$B/item/"; done
cat > "$B/cofnij.sh" <<EOC
#!/bin/bash
# Cofniecie wdrozenia staminy: przy ZATRZYMANYM world1 przywraca jary, configi i pliki MMOItems.
set -e
docker stop $K
rm -f "$P"/animacje-broni-*.jar "$P"/dwie-bronie-*.jar
cp -a "$B"/animacje-broni-*.jar "$B"/dwie-bronie-*.jar "$P/"
cp -a "$B/animacje-config.yml" "$P/AnimacjeBroni/config.yml"; cp -a "$B/dwie-config.yml" "$P/DwieBronie/config.yml"
cp -a "$B"/item/*.yml "$P/MMOItems/item/" 2>/dev/null || true
docker start $K
echo "cofniete i uruchomione: $K"
EOC
chmod +x "$B/cofnij.sh"

I0=$(stat -c %i "$W/logs/latest.log")
docker stop "$K" >/dev/null
sprawdz_stan   # tuz przed podmiana (pamiec: wdrozenie-cofa-inne-galezie)
rm -f "$P"/animacje-broni-*.jar "$P"/dwie-bronie-*.jar
cp "$AB" "$DB" "$P/"
cp "$H/src/main/resources/config.yml" "$P/AnimacjeBroni/config.yml"
cp "$R/dwie-bronie/src/main/resources/config.yml" "$P/DwieBronie/config.yml"
for f in $ITEMY; do sed -i '/^    stamina-cost:/d' "$f"; done
docker start "$K" >/dev/null
for _ in $(seq 1 150); do
  I1=$(stat -c %i "$W/logs/latest.log" 2>/dev/null || echo 0)
  [[ "$I1" != "$I0" ]] && grep -q 'Done (' "$W/logs/latest.log" && break; sleep 4
done
grep -q 'Done (' "$W/logs/latest.log" || { echo "UWAGA: brak 'Done' po 10 min - sprawdz log; cofnij: $B/cofnij.sh"; exit 1; }
echo "--- log (AnimacjeBroni/DwieBronie/Exception):"
grep -E '\[(AnimacjeBroni|DwieBronie)\]|Exception' "$W/logs/latest.log" | grep -vE 'Loading|Enabling' | head -20 || true
docker exec "$K" rcon-cli 'animacjebroni stamina' | sed 's/\x1b\[[0-9;]*m//g'
echo "WDROZONE. Kopia: $B  cofnij: $B/cofnij.sh"
