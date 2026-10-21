#!/usr/bin/env bash
# Etap 2 mapy: MINIMAPA W ROGU (NMinimap 1.0.9) na world1. Wymaga restartu; bez graczy albo WYMUS=1.
# NMinimap i MythicHUD nadpisuja ten sam shader rendertype_text -> scalony shader (pliki/rendertype_text.*)
# wchodzi w miejsce shadera MythicHUD (overlay mythichud-1_21_6, formaty 63-83 = klient 1.21.11).
# Nexo: generate_gif_shaders=false (wstrzykiwany nexo_gif_utils deklaruje Sampler0/GameTime -> blad kompilacji;
# animowanych glifow nie uzywamy). Zasoby NMinimap (fonty, include) jako pack zewnetrzny Nexo BEZ shaderow.
# NMinimap ma usuniety alias komendy 'mm' (kolidowal z MythicMobs). Cofniecie: <kopia>/cofnij.sh
set -euo pipefail
tu="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; PL="$tu/pliki"
DANE="${DANE:-/opt/docker/mc/data/world1}"; KONT="${KONT:-mc-world1-1}"; P="$DANE/plugins"
R() { docker exec "$KONT" rcon-cli "$1" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
LISTA="$(R 'minecraft:list uuids' | head -1)"; echo "== $LISTA"
[[ "$LISTA" == *"There are 0 of"* || "${WYMUS:-0}" == 1 ]] || { echo "PRZERWANE: sa gracze; WYMUS=1 wymusza"; exit 3; }

SHADERY=""
for baza in "$P/MythicHUD/source-pack" "$P/MythicHUD/built-pack" "$P/MythicMobs/generation/resource_pack"; do
  d="$baza/mythichud-1_21_6/assets/minecraft/shaders/core"; [[ -f "$d/rendertype_text.vsh" ]] && SHADERY="$SHADERY $d"
done
[[ -n "$SHADERY" ]] || { echo "BRAK shadera MythicHUD mythichud-1_21_6"; exit 4; }

KOPIA="$DANE/backups/minimapa-$(date +%Y%m%d-%H%M%S)"; mkdir -p "$KOPIA"
cp -a "$P/Nexo/settings.yml" "$KOPIA/nexo-settings.yml"
i=0; for d in $SHADERY; do i=$((i+1)); mkdir -p "$KOPIA/shader$i"; cp -a "$d/rendertype_text.vsh" "$d/rendertype_text.fsh" "$KOPIA/shader$i/"; echo "$d" > "$KOPIA/shader$i/SCIEZKA"; done
cat > "$KOPIA/cofnij.sh" <<COF
#!/usr/bin/env bash
# Cofniecie minimapy: shadery MythicHUD, ustawienia Nexo, bez jarow NMinimap/AnvilORM i packa zasobow. Potem restart.
set -euo pipefail; K="\$(cd "\$(dirname "\$0")" && pwd)"
for s in "\$K"/shader*; do d="\$(cat "\$s/SCIEZKA")"; cp -a "\$s/rendertype_text.vsh" "\$s/rendertype_text.fsh" "\$d/"; done
cp -a "\$K/nexo-settings.yml" "$P/Nexo/settings.yml"
rm -f "$P"/NMinimap-*.jar "$P"/AnvilORM-*.jar "$P/Nexo/pack/external_packs/NMinimap-zasoby.zip"
echo "pliki przywrocone; teraz: docker restart $KONT"
COF
chmod +x "$KOPIA/cofnij.sh"; echo "== kopia: $KOPIA"

for d in $SHADERY; do cp "$PL/rendertype_text.vsh" "$PL/rendertype_text.fsh" "$d/"; done
sed -i 's/^  generate_gif_shaders: true$/  generate_gif_shaders: false/' "$P/Nexo/settings.yml"
grep -q "generate_gif_shaders: false" "$P/Nexo/settings.yml" || { echo "BLAD: nie wylaczylem gif w Nexo"; exit 5; }
cp "$PL/NMinimap-1.0.9-bez-aliasu-mm.jar" "$PL/AnvilORM-1.0.2.jar" "$P/"
cp "$PL/NMinimap-zasoby.zip" "$P/Nexo/pack/external_packs/"

LOG="$DANE/logs/latest.log"; I0="$(stat -c %i "$LOG")"
echo "== restart"; docker restart "$KONT" >/dev/null
for i in $(seq 1 120); do sleep 5; [[ "$(stat -c %i "$LOG" 2>/dev/null)" != "$I0" ]] && grep -q "Done (" "$LOG" && break; done
grep -q "Done (" "$LOG" || { echo "BLAD: serwer nie wstal w 10 min"; exit 6; }
for i in $(seq 1 30); do grep -q "Finished generating resourcepack" "$LOG" && break; sleep 2; done
grep -E "Enabling (NMinimap|AnvilORM)" "$LOG" | sed 's/^/== /'
python3 - "$P/Nexo/pack/pack.zip" <<'PY'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1]); n = z.namelist()
v = z.read("mythichud-1_21_6/assets/minecraft/shaders/core/rendertype_text.vsh").decode()
print("== shader w pack.zip: nminimap", "nminimap:vertex_body" in v, "| mythichud", "applyCustomHud" in v, "| gif", "applyGifEffect" in v)
print("== zasoby nminimap w pack.zip:", sum(1 for x in n if x.startswith("assets/nminimap/")))
print("== obce shadery rendertype_text:", [x for x in n if x.endswith("rendertype_text.vsh")])
PY
echo "wdrozone; cofniecie: $KOPIA/cofnij.sh"
