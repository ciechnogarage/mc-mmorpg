"""Widok modelu z wysokosci oczu gracza (26 px = 1.62 bloku) z 2 i 4 blokow, 3/4 z przodu - tak gracz widzi moba.
Zamyka tylko swoja karte. Uzycie: python3 bb_oczy_gracza.py OUT_DIR model.bbmodel -> <model>__oczy_{2,4}bl.png"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bb_mcp_arkusz as A  # noqa: E402
import bb_sesja as B  # noqa: E402

out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True); n = Path(sys.argv[2]).stem
u = B.otworz(sys.argv[2])
B.ukryj_techniczne()
h = A.bryla()['c'][1]
for nm, pos in (('oczy_2bl', [-10, 26, -30]), ('oczy_4bl', [-20, 26, -58])):
    A.zrzut({'position': pos, 'target': [0, h, 0], 'projection': 'perspective'}, out / f'{n}__{nm}.png')
B.zamknij(u)
