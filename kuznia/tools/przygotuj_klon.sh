#!/usr/bin/env bash
# Kuznia na klonie: po `klon.sh start $KUZNIA --z-iris` ustawia Irisa i wgrywa packi z kuznia/levels/packi.
# Potem: /opt/docker/mc/narzedzia/klon.sh restart $KUZNIA (Iris rejestruje biomy packow dopiero po restarcie).
set -eu
. "$(dirname "$(readlink -f "$0")")/kuznia.sh"   # TOOLS DATA LEVELS KONTENER
docker inspect --type container "$KONTENER" >/dev/null 2>&1 || { echo "brak $KONTENER - najpierw klon.sh start $KUZNIA --z-iris"; exit 2; }
I=$DATA/plugins/Iris
[ -d "$I/packs/overworld/objects" ] || { echo "klon bez Irisa (brak $I/packs/overworld) - klon.sh start $KUZNIA --z-iris"; exit 2; }
# Iris przy instalacji biomow packa robi restart, a obraz itzg nie ma start.sh -> serwer STAJE (skill mc-swiat-lochy)
python3 - "$I/settings.json" <<'PY'
import json, sys
p = sys.argv[1]; s = json.load(open(p))
s.setdefault("general", {})["autoRestartOnCustomBiomeInstall"] = False
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
echo "Teraz: /opt/docker/mc/narzedzia/klon.sh restart $KUZNIA"
