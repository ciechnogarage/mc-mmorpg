#!/bin/bash
# Wdrozenie: lewa bron za slotem 2 (eq-lewa-reka, cofniete wdrozeniem MENU+Portal 03.10 11:29)
# + klon w off-handzie niewidoczny w dloni (nakladki ksiega:klon/* w packu Nexo).
# Kopia i cofnij.sh w backups/ksiega-klon-<czas>. Na koncu restart world1 (jar + pack).
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins
KP="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
KLIENT=${KLIENT:-/home/dev/Desktop/Przemek/MC/MCMMORPG/scratchpad/mc-client/client.jar}
B=$W/backups/ksiega-klon-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
cp -a "$P/ksiega-postaci-0.1.0.jar" "$B/"
[ -d "$P/Nexo/pack/assets/ksiega" ] && cp -a "$P/Nexo/pack/assets/ksiega" "$B/assets-ksiega"
cat > "$B/cofnij.sh" <<EOC
#!/bin/bash
set -e
cp -a "$B/ksiega-postaci-0.1.0.jar" "$P/"
rm -rf "$P/Nexo/pack/assets/ksiega"
[ -d "$B/assets-ksiega" ] && cp -a "$B/assets-ksiega" "$P/Nexo/pack/assets/ksiega" || true
echo "przywrocone - teraz: docker restart mc-world1-1"
EOC
chmod +x "$B/cofnij.sh"
python3 "$KP/wdrozenie/klon/generuj.py" "$KLIENT" "$P/Nexo/pack" "$P/Nexo/pack/assets/ksiega"
cp "$KP/build/libs/ksiega-postaci-0.1.0.jar" "$P/"
echo "Wgrane. Kopia: $B (cofnij: $B/cofnij.sh). Teraz: docker restart mc-world1-1"
echo "Po starcie w logu: '[KsiegaPostaci] Nakladki klona (niewidoczny w dloni): N' (N ~1960)"
