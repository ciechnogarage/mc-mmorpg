#!/usr/bin/env bash
# Seria pomiarow: seria.sh <wersja> <MOB> [POWT=4] [ILE=1]  (env START/SPAWN/BRON/LIMIT_S jak uruchom.sh)
# Dopisuje wyniki do output/lochy/bagna/mm/wyniki_areny.jsonl (z wersja i ustawieniami), potem wyniki.py robi tabele.
set -uo pipefail
H=$(dirname "$(readlink -f "$0")"); J=/opt/docker/mc/data/world1/output/lochy/bagna/mm/wyniki_areny.jsonl
W="$1"; MOB="$2"; P="${3:-4}"; I="${4:-1}"
# kazda runda = osobny bot (po zgonie CMI odradza na spawnie world1 i bot ginal od upadku w nastepnej rundzie - 03.10)
for t in nieuwazny uwazny; do for k in $(seq 1 "$P"); do
  "$H/uruchom.sh" "$MOB" "$t" 1 "$I" 2>&1 | grep '^WYNIKI ' | sed 's/^WYNIKI //' | python3 -c "
import json,sys
for r in json.load(sys.stdin):
    r.update(wersja='$W', bron='${BRON:-stone_sword}'); print(json.dumps(r, ensure_ascii=False))" >> "$J"
done; done
python3 "$H/wyniki.py" | grep -E "^\| $W \| $MOB" || true
