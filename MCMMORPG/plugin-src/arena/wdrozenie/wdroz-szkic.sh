#!/usr/bin/env bash
# Szkic dla operatora klonu. Ten skrypt sam nie uruchamia ani nie restartuje serwera.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER="${1:?Podaj katalog danych WYŁĄCZONEGO klonu world1}"
JAR="${2:?Podaj zbudowany arena-*.jar}"
[[ -f "$JAR" ]] || { echo "Brak jaru: $JAR" >&2; exit 1; }
[[ -d "$SERVER/plugins" ]] || { echo "Brak katalogu plugins: $SERVER" >&2; exit 1; }
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP="$SERVER/arena-backup-$STAMP"
mkdir -p "$BACKUP"
copy_file() {
  local src="$1" dest="$2"
  mkdir -p "$(dirname "$dest")"
  if [[ -e "$dest" ]]; then cp -p "$dest" "$BACKUP/$(basename "$dest")"; fi
  cp -p "$src" "$dest"
}
# U operatora: najpierw wykonaj pierwszą komendę ustaw.txt na klonie, zatrzymaj klon,
# potem wywołaj ten skrypt. Po ponownym starcie wykonaj pozostałe komendy ustaw.txt,
# a następnie budowa.txt przez RCON, po jednej komendzie. Zweryfikuj mv gamerule list arena.
copy_file "$JAR" "$SERVER/plugins/$(basename "$JAR")"
for menu in "$ROOT"/pliki/DeluxeMenus/gui_menus/*.yml; do
  copy_file "$menu" "$SERVER/plugins/DeluxeMenus/gui_menus/$(basename "$menu")"
done
copy_file "$ROOT/pliki/MythicMobs/mobs/Arena.yml" "$SERVER/plugins/MythicMobs/mobs/Arena.yml"
copy_file "$ROOT/pliki/CoreTools/Scripts/kara-smierci.yml" "$SERVER/plugins/CoreTools/Scripts/kara-smierci.yml"
copy_file "$ROOT/swiat/regions.yml" "$SERVER/plugins/WorldGuard/worlds/arena/regions.yml"
echo "Skopiowano pliki. Backup: $BACKUP"
echo "Sprawdź w kliencie sloty 41,42,50,51 menu hub_spolecznosc; uruchom testy A-K na klonie."
