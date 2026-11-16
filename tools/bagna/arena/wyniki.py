#!/usr/bin/env python3
"""wyniki_areny.jsonl -> output/lochy/bagna/mm/WYNIKI_ARENY.md (tabela: wersja, mob, ile, bron, tryb, n, HP% sr/max, czas sr, zgony, zabity)."""
import json, statistics as st, collections
B = "/opt/docker/mc/data/world1/output/lochy/bagna/mm"
rows = [json.loads(l) for l in open(f"{B}/wyniki_areny.jsonl") if l.strip()]
g = collections.OrderedDict()
for r in rows:
    g.setdefault((r["wersja"], r["mob"], r.get("ile", 1), r.get("bron", "stone_sword"), r["tryb"]), []).append(r)
L = ["# Wyniki areny Bagien (bot bez klas, liczby SUROWE)", "",
     "Cel (README areny): zwykły mob 8–15 s; uważny traci ~0–15% HP i nie ginie, nieuważny 3–5× więcej. Elity 30–60 s, uważny bez zgonu. Rój 3–4 naraz.",
     "HP% = utracone HP / 20 (z leczeniem regeneracji się nie odejmuje). Generowane: tools/bagna/arena/wyniki.py.", "",
     "| wersja | mob | ile | broń | tryb | n | HP% śr | HP% max | czas s śr | zgony | zabity |", "|---|---|---|---|---|---|---|---|---|---|---|"]
for (w, m, i, b, t), rs in g.items():
    hp = [r["hp_strata"] * 5 for r in rs]; cz = [r["czas_s"] for r in rs]
    L.append(f"| {w} | {m} | {i} | {b} | {t} | {len(rs)} | {st.mean(hp):.0f} | {max(hp):.0f} | {st.mean(cz):.1f} | {sum(r['zgon'] for r in rs)} | {sum(r['zabity'] for r in rs)} |")
stan = f"{B}/stan_strojenia.md"
import os
open(f"{B}/WYNIKI_ARENY.md", "w").write("\n".join(L) + "\n" + (open(stan).read() if os.path.exists(stan) else ""))
print("\n".join(L[6:]))
