#!/usr/bin/env bash
# Kuznia na klonie: po `klon.sh start $KUZNIA --z-iris` ustawia Irisa i wgrywa packi z kuznia/levels/packi.
# Pliki zapisuje przy ZATRZYMANYM serwerze: Iris przy wylaczaniu przepisuje settings.json z pamieci
# (zmiana przy dzialajacym serwerze znika po restarcie - sprawdzone 21.10). Packi Iris czyta przy starcie.
set -eu
. "$(dirname "$(readlink -f "$0")")/kuznia.sh"   # TOOLS DATA LEVELS KONTENER
docker inspect --type container "$KONTENER" >/dev/null 2>&1 || { echo "brak $KONTENER - najpierw klon.sh start $KUZNIA --z-iris"; exit 2; }
I=$DATA/plugins/Iris
[ -d "$I/packs/overworld/objects" ] || { echo "klon bez Irisa (brak $I/packs/overworld) - klon.sh start $KUZNIA --z-iris"; exit 2; }
docker stop -t 60 "$KONTENER" >/dev/null
# Iris przy instalacji biomow packa robi restart, a obraz itzg nie ma start.sh -> serwer STAJE (skill mc-swiat-lochy)
python3 - "$I/settings.json" <<'PY'
import json, sys
p = sys.argv[1]; s = json.load(open(p))
s.setdefault("autoConfiguration", {})["autoRestartOnCustomBiomeInstall"] = False   # sekcja autoConfiguration (Iris 3.9.1)
json.dump(s, open(p, "w"), indent=4)
print("Iris autoRestartOnCustomBiomeInstall = false")
PY
for p in "$LEVELS"/packi/*/; do
  n=$(basename "$p")
  rm -rf "$I/packs/$n"
  cp -a "$p" "$I/packs/$n"
  # obiekty nie sa w repo: to kopie z packa overworld (trees/oak, trees/birch)
  mkdir -p "$I/packs/$n/objects"
  cp -a "$I/packs/overworld/objects/trees" "$I/packs/$n/objects/"
  echo "pack $n -> $I/packs/$n"
done
I0=$(stat -c %i "$DATA/logs/latest.log" 2>/dev/null || echo 0)
docker start "$KONTENER" >/dev/null
for i in $(seq 1 90); do
  sleep 5
  I1=$(stat -c %i "$DATA/logs/latest.log" 2>/dev/null || echo 0)
  [ "$I1" != "$I0" ] && grep -q 'Done (' "$DATA/logs/latest.log" 2>/dev/null && { echo "GOTOWY: $KONTENER"; exit 0; }
done
echo "TIMEOUT startu: $DATA/logs/latest.log"; exit 1
