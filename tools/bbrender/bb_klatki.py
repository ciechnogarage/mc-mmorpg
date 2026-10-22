"""Klatki wszystkich animacji modelu przez Blockbench MCP: 5 klatek (0/25/45/70/100% dlugosci), jedna kamera 3/4 przod.
Plik 4.x w BB5 = ruch z gry (memory modelengine-konwencja-animacji). Zamyka tylko swoja karte.
Uzycie: python3 bb_klatki.py OUT_DIR model.bbmodel -> OUT_DIR/<anim>__<i>.png + klatki.json"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bb_mcp_arkusz as A  # noqa: E402
import bb_sesja as B  # noqa: E402

out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
czasy_path = Path(sys.argv[2]).with_name('czasy.json')
czasy = json.loads(czasy_path.read_text()) if czasy_path.exists() else {}
u = B.otworz(sys.argv[2])
B.ukryj_techniczne(fx=False)  # efekty fx_ widoczne tam, gdzie animacja je skaluje
info = A.bryla()
# Zapas kadru na zapowiedź z bronią ponad głową i wybicia.
c = [info['c'][0], info['c'][1] + 4, info['c'][2]]
d = max(info['z']) * 2.2 + 12
kam = {'position': [c[0] - d * .55, c[1] + d * .3, c[2] - d * .85], 'target': c, 'projection': 'perspective'}
man = []
for a in info['anims']:
    param = czasy.get(a['n'], {})
    if 'hit' in param and 0 < param['hit'] < a['l']:
        hit = param['hit']
        prep = min(hit, param.get('windup', hit * .7))
        recovery = min(a['l'], hit + param.get('recover', a['l'] - hit) * .5)
        if 'windup' in param and prep >= .5:
            chwile = (0, prep * .3, prep, hit, a['l'])
        else:
            chwile = (0, prep, hit, recovery, a['l'])
    else:
        chwile = tuple(a['l'] * f for f in (0, .25, .45, .7, 1.0))
    for i, t in enumerate(chwile):
        # Wspólna sesja MCP bywa przełączana przez równoległy render.
        B.js('ModelProject.all.find(p=>p.uuid===' + B.literal(u) + ').select(); JSON.stringify(1)')
        B.js(B.isolate_animation_js(B.literal(a['n'])) + f'Timeline.setTime({t}); Animator.preview(); JSON.stringify(1)')
        p = out / f"{a['n']}__{i}.png"
        A.zrzut(kam, p)
        man.append({'anim': a['n'], 'i': i, 't': round(t, 3), 'plik': p.name})
B.js('Timeline.pause(); JSON.stringify(1)')
B.zamknij(u)
(out / 'klatki.json').write_text(json.dumps(man, indent=1))
