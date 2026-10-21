#!/usr/bin/env python3
"""Latka v6: NMinimap na wysokim, wolnym numerze mapy. latka6.py <plugins>. Idempotentna.
NMinimap map-id 0 = ten sam numer co pierwsza osobista mapa gracza (Bukkit.createMap zaczyna od 0) -> klient
dostaje pod jednym numerem na przemian mape w rece i minimape: "mapa znika" (zgloszone 02.10, odtworzone na klonie).
Plugin Mapa i tak omija numer NMinimap, ale wysoki numer usuwa konflikt u zrodla."""
import sys, os, re
f = os.path.join(sys.argv[1], "NMinimap/config.yml"); s = open(f, encoding="utf8").read()
s2, n = re.subn(r"(?m)^map-id: .*$", "map-id: 1999000000", s)
if n != 1: raise SystemExit("BRAK KOTWICY: map-id")
open(f, "w", encoding="utf8").write(s2); print("zmiany v6 (NMinimap map-id):", "brak" if s2 == s else "1999000000")
