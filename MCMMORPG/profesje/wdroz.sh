#!/usr/bin/env bash
# Wdrozenie PROFESJI v2 na world1 (PROJEKT.md). Czeka na zero graczy. Generator dziala NA ZATRZYMANYM world1,
# na aktualnych plikach (nie na starej kopii) — zmiany innych sesji z ostatnich godzin zostaja.
# Kolejnosc: kontrole -> kopia swiata -> tar zmienianych sciezek + cofnij.sh -> stop -> generator -> audyt
# (FAIL = automatyczne cofniecie) -> jary, paper-global, listy rynku/skupu -> start -> kontrola logu.
# Uzycie: wdroz.sh            (cofniecie: <katalog kopii>/cofnij.sh)
set -euo pipefail
# Proba na klonie (klon.sh): W=/opt/docker/mc/klony/<n>/data B=<katalog> STOP="klon.sh stop <n>" START="klon.sh start <n>"
#   RC="docker exec klon-<n> rcon-cli" BEZ_KOPII_SWIATA=1 wdroz.sh
W=${W:-/opt/docker/mc/data/world1}
R="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"           # MCMMORPG
TS=$(date +%Y%m%d-%H%M); B=${B:-/opt/docker/mc/backups/profesje-v2-$TS}
C=/opt/docker/mc/docker-compose.yml
STOP=${STOP:-"docker compose -f $C stop world1"}; START=${START:-"docker compose -f $C start world1"}
RC=${RC:-"docker exec mc-world1-1 rcon-cli"}
TOOLS=${TOOLS:-$W/tools}           # proba: kopia (zeby nie pisac do produkcyjnego tools/handel)
log() { echo "$(date '+%F %T') $*"; }

SCIEZKI=(plugins/MMOItems/item plugins/MMOItems/crafting-stations plugins/MMOItems/modifiers
  plugins/MMOCore/professions plugins/MMOCore/exp-curves plugins/MMOCore/exp-tables
  plugins/Nexo/items plugins/MythicMobs/mobs plugins/MythicMobs/skills plugins/MythicMobs/droptables plugins/MythicMobs/spawners
  plugins/DeluxeMenus/gui_menus plugins/DeluxeMenus/config.yml
  plugins/CoreTools/Shops/skup.yml plugins/CoreTools/AuctionHouse/rynek.yml config/paper-global.yml)
NOWE=(plugins/Rzemioslo plugins/Mistrzostwa plugins/Nexo/pack/assets/nexo/textures/item/profesje_v2
  plugins/Nexo/pack/assets/nexo/textures/block/profesje_v2 tools/handel/ceny_profesje.json tools/handel/surowce_profesje.json)

# ---------------------------------------------------------------- kontrole
J_R=$(ls "$R"/plugin-src/rzemioslo/build/libs/rzemioslo-*.jar | head -1); J_M=$(ls "$R"/plugin-src/mistrzostwa/build/libs/mistrzostwa-*.jar | head -1)
[[ -f "$J_R" && -f "$J_M" ]] || { log "BLAD: brak jarow (build.sh w plugin-src/rzemioslo i mistrzostwa)"; exit 1; }
(( $(df -BG --output=avail / | tail -1 | tr -dc 0-9) >= 6 )) || { log "BLAD: < 6 GB dysku"; exit 1; }
for n in "${NOWE[@]}"; do [[ ! -e "$W/$n" ]] || { log "BLAD: $n juz istnieje — wdrozenie bylo? (cofnij najpierw)"; exit 1; }; done
for i in $(seq 1 1440); do
  L=$($RC "minecraft:list uuids" 2>/dev/null || true)
  echo "$L" | grep -q "There are 0 of" && break
  [ $((i % 30)) = 1 ] && log "czekam na zero graczy: $L"
  sleep 30
done
echo "$L" | grep -q "There are 0 of" || { log "BLAD: serwer nie opustoszal"; exit 2; }

# ---------------------------------------------------------------- kopie
if [[ -z "${BEZ_KOPII_SWIATA:-}" ]]; then
  [[ -x /opt/docker/mc/kopie/kopia-swiata.sh ]] && /opt/docker/mc/kopie/kopia-swiata.sh || log "UWAGA: kopia swiata nie wykonana"
fi
mkdir -p "$B"
tar -C "$W" -czf "$B/przed.tgz" "${SCIEZKI[@]}"
cp -p "$TOOLS"/handel/generuj.py "$B/" 2>/dev/null || true
cat > "$B/cofnij.sh" <<EOF
#!/usr/bin/env bash
# Cofniecie profesji v2 ($TS): przywraca zmienione sciezki z przed.tgz, usuwa nowe pliki i jary, restart world1.
set -euo pipefail
W=$W
$STOP
for s in ${SCIEZKI[*]}; do rm -rf "\$W/\$s"; done
tar -C "\$W" -xzf "$B/przed.tgz"
for n in ${NOWE[*]}; do rm -rf "\$W/\$n"; done
rm -f "\$W"/plugins/rzemioslo-*.jar "\$W"/plugins/mistrzostwa-*.jar "\$W"/plugins/.paper-remapped/rzemioslo-*.jar "\$W"/plugins/.paper-remapped/mistrzostwa-*.jar
$START
echo "cofnieto profesje v2 ($TS)"
EOF
chmod +x "$B/cofnij.sh"
log "kopia: $B (cofnij.sh)"

# ---------------------------------------------------------------- stop + generator + audyt
$STOP
gen() { docker run --rm --name "profesje-wdroz-$1" --memory 1g -u "$(id -u):$(id -g)" -w /tmp -e HOME=/tmp \
  -v "$W/plugins:/k/plugins" -v "$R:/w:ro" -v "$TOOLS:/tools" \
  -v /opt/docker/mc/backups/mistrzostwa-20260923/wdrozenie:/mst:ro python:3.12-slim sh -c "pip -q install --target /tmp/pp pyyaml pillow >/dev/null 2>&1; PYTHONPATH=/tmp/pp $2"; }
gen gen "python /w/profesje/generuj.py --plugins /k/plugins --katalog-v2 /tools/przedmioty/katalog.json --ikony-v1 /tools/ikony-profesji --mistrzostwa /mst --handel /tools/handel"
if ! gen audyt "python /w/profesje/audyt.py /k/plugins"; then
  log "AUDYT FAIL — cofam"; bash "$B/cofnij.sh"; exit 5
fi

# ---------------------------------------------------------------- rynek i skup (tylko listy, wyglad z produkcji)
TMPH=$(mktemp -d); mkdir -p "$TMPH/plugins"; cp -r "$W/plugins/CoreTools" "$TMPH/plugins/"; rm -rf "$TMPH/plugins/CoreTools/userdata"
docker run --rm --name profesje-wdroz-handel --memory 512m -u "$(id -u):$(id -g)" -w /tmp -e HOME=/tmp -e HANDEL_PLUGINS=/hp \
  -v "$TMPH/plugins:/hp" -v "$TOOLS/handel:/h:ro" python:3.12-slim sh -c 'pip -q install --target /tmp/pp pyyaml >/dev/null 2>&1; PYTHONPATH=/tmp/pp python /h/generuj.py'
python3 "$R/profesje/handel_wstaw.py" "$W/plugins/CoreTools" "$TMPH/plugins/CoreTools"
rm -rf "$TMPH"

# ---------------------------------------------------------------- jary, paper-global (noteblock + tripwire: stabilne bloki Nexo)
rm -f "$W"/plugins/rzemioslo-*.jar "$W"/plugins/mistrzostwa-*.jar
cp "$J_R" "$J_M" "$W/plugins/"
sed -i 's/disable-noteblock-updates: false/disable-noteblock-updates: true/; s/disable-tripwire-updates: false/disable-tripwire-updates: true/' "$W/config/paper-global.yml"

# ---------------------------------------------------------------- start + kontrola
I0=$(stat -c %i "$W/logs/latest.log")
$START
ok=0; for i in $(seq 1 150); do I1=$(stat -c %i "$W/logs/latest.log" 2>/dev/null || echo 0)
  if [[ "$I1" != "$I0" ]] && grep -q 'Done (' "$W/logs/latest.log"; then ok=1; break; fi; sleep 4; done
[[ $ok = 1 ]] || { log "BLAD: world1 nie wstal — sprawdz log; cofniecie: $B/cofnij.sh"; exit 6; }
sleep 20
grep -aE "Rzemioslo\] (Profesje v2|Kontrola)|Mistrzostwa\] Blokada|Could not (post-)?load item template|Numeric Stat Formula|Rzemioslo.*(SEVERE|ERROR|Exception)" "$W/logs/latest.log" | cut -c1-200
log "wdrozono profesje v2; cofniecie: $B/cofnij.sh"
