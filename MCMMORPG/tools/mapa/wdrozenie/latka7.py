#!/usr/bin/env python3
"""Latka v7: radar mobow NMinimap odswieza LISTE encji co 20 tickow zamiast 10. latka7.py <plugins>. Idempotentna.
MobRadarManager.fetchAllMobs kopiuje na watku glownym world.getEntities() wszystkich swiatow (koszt ~ liczba encji);
pozycje kropek renderer czyta z encji na zywo, wiec interwal opoznia tylko pojawienie sie nowego moba (<= 1 s).
Zmierzone 02.10 na klonie (30 botow): radar = najwiekszy koszt minimapy na watku glownym (~1,5%)."""
import sys, os, re
f = os.path.join(sys.argv[1], "NMinimap/config.yml"); s = open(f, encoding="utf8").read()
i = s.find("  mob-radar:")
if i < 0: raise SystemExit("BRAK KOTWICY: mob-radar")
m = re.compile(r"(?m)^    update-interval: \d+$").search(s, i)
if not m or s.find("\n  ", i + 1) < 0: raise SystemExit("BRAK KOTWICY: update-interval")
s2 = s[:m.start()] + "    update-interval: 20" + s[m.end():]
open(f, "w", encoding="utf8").write(s2); print("zmiany v7 (radar co 20 tickow):", "brak" if s2 == s else "tak")
