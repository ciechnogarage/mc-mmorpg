#!/usr/bin/env python3
"""Latka v5: wydajnosc i dzialanie minimapy NMinimap (zmierzone 02.10 sparkiem i na klonie). latka5.py <plugins>.
- available-ram-threshold 1G -> 200M: przy 2 GB heapu NMinimap przestawal renderowac teren (0 kafli),
- render-new-chunks true -> false: render kazdego wczytanego chunka szedl na WATKU GLOWNYM (ChunkLoadEvent),
- max-render-threads 30 -> 2 (4 rdzenie), player-render-interval 1 -> 2 (10 odswiezen/s wystarcza)."""
import sys, os, re
f = os.path.join(sys.argv[1], "NMinimap/config.yml"); s = open(f, encoding="utf8").read(); zm = []
for wzor, nowe in [(r"(?m)^(  available-ram-threshold:) .*$", r"\1 200M"), (r"(?m)^(  render-new-chunks:) .*$", r"\1 false"),
                   (r"(?m)^(max-render-threads:) .*$", r"\1 2"), (r"(?m)^(player-render-interval:) .*$", r"\1 2")]:
    s2, n = re.subn(wzor, nowe, s)
    if n != 1: raise SystemExit("BRAK KOTWICY: " + wzor)
    if s2 != s: zm.append(nowe.replace(r"\1", "").strip()); s = s2
open(f, "w", encoding="utf8").write(s); print("zmiany v5 (NMinimap):", zm or "brak")
