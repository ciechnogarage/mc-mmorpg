"""Trzy ujęcia jednego modelu przez MCP, z zamknięciem własnej karty."""
import sys
import time
from pathlib import Path
import bb_mcp_arkusz as A
import bb_sesja as B

mob = sys.argv[1]
base = Path('/opt/docker/mc/data/world1/output/lochy/bagna')
src = base / 'modele' / mob / f'bagna_{mob}.bbmodel'
out = base / 'modele' / mob / 'zrzuty'
out.mkdir(parents=True, exist_ok=True)
uid = B.otworz(str(src))
try:
    B.ukryj_techniczne()
    info = A.bryla()
    c, size = info['c'], info['z']
    d = max(size) * 1.35 + 8
    views = {
        'przod': {'position': [c[0]-d*.55,c[1]+d*.35,c[2]-d*.85], 'target': c, 'projection':'perspective'},
        'tyl': {'position': [c[0]+d*.55,c[1]+d*.35,c[2]+d*.85], 'target': c, 'projection':'perspective'},
        'oczy_4bl': {'position': [-20,26,-58], 'target': [0,c[1],0], 'projection':'perspective'},
    }
    for name, camera in views.items():
        if len(sys.argv) > 2 and name not in sys.argv[2:]:
            continue
        for attempt in range(5):
            B.js('ModelProject.all.find(p=>p.uuid===' + B.literal(uid) + ').select(); JSON.stringify(1)')
            B.sesja().tool('set_camera_angle', camera)
            A.zapisz(B.sesja().tool('capture_screenshot', {}), out / f'bagna_{mob}__{name}.png')
            active=B.js('JSON.stringify(Project.uuid)')
            if active==uid:
                break
            time.sleep(.3)
        else:
            raise RuntimeError(f'{mob} {name}: karta przejęta podczas 5 prób')
        print(mob, name, flush=True)
finally:
    B.zamknij(uid)
