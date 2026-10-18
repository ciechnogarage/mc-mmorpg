#!/bin/bash
# Wdrozenie postaw mocnego ciosu (kierunek = poza ladowania + zamach), gardy toporow/bulaw i luku kosy
# (galaz bronie-krwawienie-naladowanie): jar + config AnimacjeBroni, a przy zatrzymanym serwerze zdjecie
# 'on-attack: slashing_attack_effect' z typu KOSA w MMOItems/item-types.yml (rozprysk 6,5 bl. z ukrytym cooldownem 3 s).
#   wdroz-postawy.sh [--sucho]     --sucho = tylko sprawdzenia, bez zmian
# Bezpieczniki: md5 jara, configu i item-types na world1 == stan po wdrozeniu krwawienia 18.10 00:44 (inaczej ktos
# wdrozyl cos nowszego -> scal najpierw), zero graczy (minecraft:list uuids, czeka do 60 min), kopia + cofnij.sh, log.
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins; K=mc-world1-1
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AB=$(ls "$H"/build/libs/animacje-broni-*.jar | head -1)
CFG=$H/src/main/resources/config.yml
TYPY=$P/MMOItems/item-types.yml
OCZEKIWANE_AB=6f28b5fa51b860ade112c293295d1324    # animacje-broni-0.1.0-202610180034 (poprawka krwawienia)
OCZEKIWANE_CFG=d6ffef4eec63ee2b3cc5a1bbd1424545   # config staminy 17.10
OCZEKIWANE_TYPY=06f945d29fe843b22b4ecc375bc6384e  # item-types.yml 18.10
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
# zdejmuje on-attack tylko w bloku KOSA (do nastepnego typu na poczatku linii)
bez_rozprysku_kosy() { python3 - "$1" <<'PY'
import re, sys
p = sys.argv[1]; s = open(p, encoding='utf-8').read()
m = re.search(r'(?ms)^KOSA:\n.*?(?=^\S|\Z)', s)
b = m.group(0); n = re.sub(r'(?m)^    on-attack: slashing_attack_effect\n', '', b)
assert n != b, 'brak on-attack w KOSA'
open(p, 'w', encoding='utf-8').write(s.replace(b, n))
PY
}

sprawdz_stan
echo "world1 = baza: OK; nowy: $(basename "$AB")"
(( SUCHO )) && { echo "SUCHO: gracze online = $(gracze)"; exit 0; }

for i in $(seq 1 60); do n=$(gracze); [[ "$n" == 0 ]] && break; echo "graczy online: $n - czekam 60 s"; sleep 60; done
[[ "$(gracze)" == 0 ]] || { echo "STOP: gracze nadal online po 60 min"; exit 3; }

B=/opt/docker/mc/backups/postawy-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
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
bez_rozprysku_kosy "$TYPY"
docker start "$K" >/dev/null
for _ in $(seq 1 150); do
  I1=$(stat -c %i "$W/logs/latest.log" 2>/dev/null || echo 0)
  [[ "$I1" != "$I0" ]] && grep -q 'Done (' "$W/logs/latest.log" && break; sleep 4
done
grep -q 'Done (' "$W/logs/latest.log" || { echo "UWAGA: brak 'Done' po 10 min - sprawdz log; cofnij: $B/cofnij.sh"; exit 1; }
echo "--- log (AnimacjeBroni/MMOItems/Exception):"
grep -E '\[AnimacjeBroni\]|\[MMOItems\].*(error|Could not)|Exception' "$W/logs/latest.log" | grep -vE 'Loading' | head -20 || true
echo "WDROZONE. Kopia: $B  cofnij: $B/cofnij.sh"
