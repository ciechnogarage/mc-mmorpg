#!/usr/bin/env bash
# Wdrozenie AnimacjeBroni na world1: jar + config + modele trzymania (Nexo pack/assets/bron) -> RESTART.
# Nowy plugin = restart; skrypt odmawia, gdy ktos gra (minecraft:list uuids, patrz pamiec rcon-list-pusty).
# Cofniecie: <kopia>/cofnij.sh
set -euo pipefail
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; W=/opt/docker/mc/data/world1; P=$W/plugins; C=mc-world1-1
JAR=$(ls -t "$H"/build/libs/animacje-broni-*.jar | head -1)
python3 "$H/generuj.py" --plugins "$P" --out "$H/wynik"
cmp -s "$H/wynik/config.yml" "$H/src/main/resources/config.yml" || { echo "config w jarze != wynik generatora: przebuduj (cp wynik/config.yml src/main/resources/ && ./build.sh)"; exit 1; }
LISTA=$(docker exec $C rcon-cli "minecraft:list uuids")
echo "$LISTA" | grep -qE "There are 0 of" || { echo "ktos gra: $LISTA - przerywam"; exit 1; }
K=$W/backups/animacje-broni-$(date +%Y%m%d-%H%M%S); mkdir -p "$K"
[ -d "$P/AnimacjeBroni" ] && cp -a "$P/AnimacjeBroni" "$K/"
[ -d "$P/Nexo/pack/assets/bron" ] && cp -a "$P/Nexo/pack/assets/bron" "$K/assets-bron"
ls "$P"/animacje-broni-*.jar >/dev/null 2>&1 && cp -a "$P"/animacje-broni-*.jar "$K/"
cat > "$K/cofnij.sh" <<COF
#!/usr/bin/env bash
# przywraca stan sprzed wdrozenia $(date +%F\ %T) i restartuje world1
set -e
rm -f $P/animacje-broni-*.jar; rm -rf $P/AnimacjeBroni $P/Nexo/pack/assets/bron
[ -d $K/AnimacjeBroni ] && cp -a $K/AnimacjeBroni $P/ || true
[ -d $K/assets-bron ] && cp -a $K/assets-bron $P/Nexo/pack/assets/bron || true
cp -a $K/animacje-broni-*.jar $P/ 2>/dev/null || true
docker exec $C rcon-cli stop
COF
chmod +x "$K/cofnij.sh"
rm -f "$P"/animacje-broni-*.jar; cp "$JAR" "$P/"
mkdir -p "$P/AnimacjeBroni"; cp "$H/wynik/config.yml" "$P/AnimacjeBroni/config.yml"
rm -rf "$P/Nexo/pack/assets/bron"; cp -a "$H/wynik/pack/assets/bron" "$P/Nexo/pack/assets/bron"
echo "kopia: $K ; restart world1..."
docker exec $C rcon-cli stop || true
