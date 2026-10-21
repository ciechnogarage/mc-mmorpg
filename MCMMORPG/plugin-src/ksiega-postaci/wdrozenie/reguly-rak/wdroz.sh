#!/bin/bash
# Reguly rak Ksiegi (12.10): dwureczne tylko w prawej, typy dwureczne z configu, tarcza/OFF_CATALYST tylko w lewej.
# Kopia + cofnij.sh w backups/ksiega-reguly-<czas>. Na koncu restart world1.
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins
KP="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
B=$W/backups/ksiega-reguly-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
cp -a "$P/ksiega-postaci-0.1.0.jar" "$P/KsiegaPostaci/config.yml" "$B/"
cat > "$B/cofnij.sh" <<EOC
#!/bin/bash
set -e
cp -a "$B/ksiega-postaci-0.1.0.jar" "$P/"
cp -a "$B/config.yml" "$P/KsiegaPostaci/config.yml"
echo "przywrocone - teraz: docker restart mc-world1-1"
EOC
chmod +x "$B/cofnij.sh"
python3 "$KP/wdrozenie/reguly-rak/scal_config.py" "$P/KsiegaPostaci/config.yml"
cp "$KP/build/libs/ksiega-postaci-0.1.0.jar" "$P/"
echo "Wgrane. Kopia: $B (cofnij: $B/cofnij.sh). Teraz: docker restart mc-world1-1"
