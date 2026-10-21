#!/bin/bash
# Wdrozenie DwieBronie na world1 (nowy plugin = restart). Kopia i cofnij.sh w backups/dwie-bronie-<czas>.
set -euo pipefail
W=/opt/docker/mc/data/world1; P=$W/plugins
H="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR=$(ls "$H"/build/libs/dwie-bronie-*.jar | head -1)
B=$W/backups/dwie-bronie-$(date +%Y%m%d-%H%M%S); mkdir -p "$B"
ls "$P"/dwie-bronie-*.jar >/dev/null 2>&1 && cp -a "$P"/dwie-bronie-*.jar "$B/" || true
[ -f "$P/DwieBronie/config.yml" ] && cp -a "$P/DwieBronie/config.yml" "$B/config.yml" || true
cat > "$B/cofnij.sh" <<EOC
#!/bin/bash
set -e
rm -f "$P"/dwie-bronie-*.jar
cp -a "$B"/dwie-bronie-*.jar "$P/" 2>/dev/null || true
[ -f "$B/config.yml" ] && cp -a "$B/config.yml" "$P/DwieBronie/config.yml" || true
echo "cofniete - teraz: docker restart mc-world1-1"
EOC
chmod +x "$B/cofnij.sh"
rm -f "$P"/dwie-bronie-*.jar
cp "$JAR" "$P/"
mkdir -p "$P/DwieBronie" && cp "$H/src/main/resources/config.yml" "$P/DwieBronie/config.yml"
echo "Wgrane $(basename "$JAR"). Kopia: $B (cofnij: $B/cofnij.sh). Teraz: docker restart mc-world1-1"
echo "Po starcie w logu: 'Enabling DwieBronie'; debug: true w plugins/DwieBronie/config.yml -> linie DBDBG"
