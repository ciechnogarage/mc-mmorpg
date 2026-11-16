#!/usr/bin/env bash
set -euo pipefail
BASE=/opt/docker/mc/data/world1
HUD="$BASE/tools/hud"
SRC="$HUD/praca"
PLUG="$BASE/plugins"
STAMP="$(date -u +%Y%m%d-%H%M%S)"
BACK="$BASE/backups/hud-$STAMP"
[[ -d "$SRC/hud_assets" && -d "$SRC/pack/assets" && -f "$SRC/NMinimap/config.yml" && -f "$SRC/MMOCore/config.yml" ]] || { echo 'Brak kompletu plikow praca/' >&2; exit 1; }
[[ ! -e "$BACK" ]] || { echo "Kopia juz istnieje: $BACK" >&2; exit 1; }
mkdir -p "$BACK/MythicHUD" "$BACK/NMinimap" "$BACK/MMOCore"
for item in config.yml hud_assets layouts listeners popups source-pack; do
  cp -a "$PLUG/MythicHUD/$item" "$BACK/MythicHUD/$item"
done
cp -a "$PLUG/NMinimap/config.yml" "$BACK/NMinimap/config.yml"
python3 - "$PLUG/NMinimap/database.db" "$BACK/NMinimap/database.db" <<'PY'
import sqlite3,sys
source=sqlite3.connect('file:'+sys.argv[1]+'?mode=ro',uri=True)
target=sqlite3.connect(sys.argv[2])
source.backup(target)
target.close()
source.close()
PY
cp -a "$PLUG/NMinimap/frames" "$BACK/NMinimap/frames"
cp -a "$PLUG/MMOCore/config.yml" "$BACK/MMOCore/config.yml"
cat > "$BACK/cofnij.sh" <<EOF
#!/usr/bin/env bash
set -euo pipefail
B="$BACK"
P="$PLUG"
# Przywracaj przy zatrzymanym serwerze; SQLite moze byc otwarty podczas gry.
rm -f "\$P/MythicHUD/hud_assets/hud/karta.yml" "\$P/MythicHUD/hud_assets/hud/hotbar.yml" "\$P/MythicHUD/layouts/karta.yml" "\$P/MythicHUD/layouts/hotbar.yml" "\$P/MythicHUD/listeners/karta.yml"
rm -rf "\$P/MythicHUD/source-pack/assets/mythichud/textures/assets/karta"
rm -f "\$P/MythicHUD/source-pack/assets/minecraft/textures/gui/sprites/hud/hotbar.png"
cp -a "\$B/MythicHUD/config.yml" "\$P/MythicHUD/config.yml"
for item in hud_assets layouts listeners popups source-pack; do rm -rf "\$P/MythicHUD/\$item"; cp -a "\$B/MythicHUD/\$item" "\$P/MythicHUD/\$item"; done
cp -a "\$B/NMinimap/config.yml" "\$P/NMinimap/config.yml"
cp -a "\$B/NMinimap/database.db" "\$P/NMinimap/database.db"
cp -a "\$B/NMinimap/frames/." "\$P/NMinimap/frames/"
rm -f "\$P/NMinimap/frames/karta_square.png"
cp -a "\$B/MMOCore/config.yml" "\$P/MMOCore/config.yml"
echo 'Przywrocono pliki. Restart i przebudowe paczki wykonuje operator.'
EOF
chmod +x "$BACK/cofnij.sh"
for item in config.yml; do cp -a "$SRC/$item" "$PLUG/MythicHUD/$item"; done
for item in hud_assets layouts listeners popups; do cp -a "$SRC/$item/." "$PLUG/MythicHUD/$item/"; done
cp -a "$SRC/pack/assets/." "$PLUG/MythicHUD/source-pack/assets/"
cp -a "$SRC/NMinimap/config.yml" "$PLUG/NMinimap/config.yml"
cp -a "$SRC/NMinimap/frames/." "$PLUG/NMinimap/frames/"
cp -a "$SRC/MMOCore/config.yml" "$PLUG/MMOCore/config.yml"
# Dla istniejących graczy: default-settings działa tylko przy pierwszym wejściu.
python3 - "$PLUG/NMinimap/database.db" <<'PY'
import sqlite3,sys
con=sqlite3.connect(sys.argv[1], timeout=10)
try:
    with con:
        con.execute('UPDATE nminimap_players SET isRight=1')
finally:
    con.close()
PY
echo "Pliki przygotowane. Kopia: $BACK"
echo 'Restart i test klienta wykonuje operator; ten skrypt nie restartuje serwera.'
