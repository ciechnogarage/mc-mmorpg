#!/usr/bin/env bash
# Backup MCMMORPG: światy + configi pluginów (bez ciężkich jarów, cache, logów).
# Trzyma ostatnie KEEP archiwów w ./backups. Uruchamiaj z crona lub ręcznie.
cd "$(dirname "$0")" || exit 1

DEST="backups"
KEEP=10
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DEST"

# Wyłączamy z backupu: same jary, cache, logi, poprzednie backupy.
tar --exclude='./libraries' \
    --exclude='./cache' \
    --exclude='./logs' \
    --exclude="./${DEST}" \
    --exclude='*.jar' \
    --exclude='*.jar.disabled' \
    -czf "${DEST}/mcmmorpg-${STAMP}.tar.gz" \
    world world_nether world_the_end \
    plugins config server.properties bukkit.yml spigot.yml \
    2>/dev/null

echo "Backup: ${DEST}/mcmmorpg-${STAMP}.tar.gz ($(du -h "${DEST}/mcmmorpg-${STAMP}.tar.gz" | cut -f1))"

# Rotacja — zostaw tylko najnowsze KEEP plików.
ls -1t "${DEST}"/mcmmorpg-*.tar.gz 2>/dev/null | tail -n +$((KEEP+1)) | xargs -r rm -f
echo "Zachowane archiwa: $(ls -1 "${DEST}"/mcmmorpg-*.tar.gz 2>/dev/null | wc -l)/${KEEP}"
