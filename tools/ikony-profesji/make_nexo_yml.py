#!/usr/bin/env python3
"""Generuje plik definicji Nexo dla podanych ID MMOItems.

Nazwy wyswietlane bierze z material.yml (zeby nie rozjechaly sie z mechanika),
teksture sklada jako nexo:item/profesje/<id bez prefiksu PROF_>.
"""
import re
import sys

import glob

ITEM_DIR = "/opt/docker/mc/data/world1/plugins/MMOItems/item"


def name_of(item_id):
    """Szuka nazwy wyswietlanej we WSZYSTKICH plikach przedmiotow, nie tylko material.yml -
    narzedzia siedza w tool.yml, a bizuteria w ring/necklace/charm.yml."""
    for f in sorted(glob.glob(ITEM_DIR + "/*.yml")):
        lines = open(f, encoding="utf-8").read().split("\n")
        inside = False
        for ln in lines:
            if ln == f"{item_id}:":
                inside = True
                continue
            if inside:
                if re.match(r"^\S", ln):
                    break
                m = re.match(r"^\s*name:\s*'?(.*?)'?\s*$", ln)
                if m:
                    return re.sub(r"&[0-9a-fk-or]|§[0-9a-fk-or]", "", m.group(1)).strip()
    return item_id


HEAD = """# Ikony surowcow profesyjnych — PARTIA 2 (przetworzone surowce szesciu profesji).
#
# WYGENEROWANE: tools/ikony-profesji/generate_icons2.py (grafika)
#               tools/ikony-profesji/make_nexo_yml.py  (ten plik)
# Po zmianie: `/nexo reload pack` + `/mi reload`.
#
# Zasady te same co w profesje_ikony.yml: przedmioty istnieja tylko po to, by Nexo
# wygenerowalo `assets/nexo/items/<id>.json`; prawdziwym przedmiotem jest MMOItem
# ze statem `model: nexo:<id>`. Tekstury 16x16, item_model w przestrzeni `nexo:`.
"""


def main():
    ids = sys.argv[1:]
    out = [HEAD]
    for item_id in ids:
        nexo_id = item_id.lower()
        tex = item_id[len("PROF_"):].lower()
        out.append(f"""
{nexo_id}:
  itemname: <gray>{name_of(item_id)}
  material: PAPER
  excludeFromInventory: true
  excludeFromCommands: true
  Pack:
    texture: nexo:item/profesje/{tex}""")
    print("\n".join(out))


if __name__ == "__main__":
    main()
