#!/usr/bin/env python3
"""Przypisuje przedmiotom PROF_* poziom (tier) na podstawie RECEPTUR, nie na oko.

  T1 - surowiec zbierany: nigdzie nie jest wyjsciem zadnej receptury warsztatu
  T2 - przetworzony:      jest wyjsciem receptury o wymaganym poziomie < 45
  T3 - mistrzowski:       wyjscie receptury o poziomie >= 45 albo 'MASTER' w id

Czyta tylko do odczytu - nie dotyka mechaniki.
"""
import glob
import re
import json

ST = "/opt/docker/mc/data/world1/plugins/MMOItems/crafting-stations/profession-*.yml"


def scan():
    out = {}
    for f in sorted(glob.glob(ST)):
        txt = open(f, encoding="utf-8").read()
        station = re.search(r"^name:\s*'?(.*?)'?\s*$", txt, re.M)
        station = station.group(1) if station else f
        # kazdy blok receptury: output: ... az do nastepnego output/gui-layout
        for m in re.finditer(
                r"output:\s*'mmoitems\{type=(\w+),id=([A-Z0-9_]+)\}'(.*?)(?=\n  '|\ngui-layout:|\Z)",
                txt, re.S):
            typ, item, body = m.group(1), m.group(2), m.group(3)
            lv = [int(x) for x in re.findall(r"profession\{profession=\w+,level=(\d+)\}", body)]
            lv = max(lv) if lv else 0
            prev = out.get(item)
            if not prev or lv > prev["level"]:
                out[item] = {"type": typ, "level": lv, "station": station}
    return out


def tier_of(item_id, outputs):
    if "MASTER" in item_id:
        return 3
    rec = outputs.get(item_id)
    if not rec:
        return 1
    return 3 if rec["level"] >= 45 else 2


if __name__ == "__main__":
    outputs = scan()
    import yaml_free  # placeholder never imported
