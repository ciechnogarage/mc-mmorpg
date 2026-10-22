"""Zrzuty modeli .bbmodel przez Blockbench MCP: widok 3/4 przod i 3/4 tyl (poza spoczynkowa) + klatka animacji ataku
(pierwsza pasujaca do WZORZEC, 45% dlugosci). Kazdy model: otworz -> zrzuty -> zamknij TYLKO jego karte.
Pliki zrodlowe nie sa zapisywane. Uzycie: python3 bb_mcp_arkusz.py OUT_DIR model1.bbmodel [...]
Wynik: OUT_DIR/<model>__{przod,tyl,atak}.png + OUT_DIR/manifest.json
"""
import base64
import json
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bb_sesja as B  # noqa: E402

WZORZEC = re.compile(r'slam|attack|swing|cast|shoot|throw|puff|parry|bite|strike|combo|spell', re.I)


def zapisz(res, sciezka):
    img = next(c for c in res['content'] if c['type'] == 'image')
    Path(sciezka).write_bytes(base64.b64decode(img['data']))


def bryla():
    return B.js('Modes.options.edit.select(); const b=new THREE.Box3(); Outliner.elements.forEach(e=>{if(e.mesh&&e.visibility!==false)b.expandByObject(e.mesh)});'
                'const c=b.getCenter(new THREE.Vector3()), z=b.getSize(new THREE.Vector3());'
                'JSON.stringify({c:[c.x,c.y,c.z], z:[z.x,z.y,z.z], el:Outliner.elements.length, anims:Animation.all.map(a=>({n:a.name,l:a.length}))})')


def zrzut(kamera, sciezka):
    B.sesja().tool('set_camera_angle', kamera)
    time.sleep(0.4)
    zapisz(B.sesja().tool('capture_screenshot', {}), sciezka)


def main():
    out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
    manifest = []
    for src in sys.argv[2:]:
        nazwa = Path(src).stem
        u = B.otworz(src)
        B.ukryj_techniczne()
        info = bryla()
        c, z = info['c'], info['z']
        d = max(z) * 1.35 + 8
        widoki = {'przod': [c[0] - d * 0.55, c[1] + d * 0.35, c[2] - d * 0.85],
                  'tyl': [c[0] + d * 0.55, c[1] + d * 0.35, c[2] + d * 0.85]}
        pliki = []
        for w, pos in widoki.items():
            zrzut({'position': pos, 'target': c, 'projection': 'perspective'}, out / f'{nazwa}__{w}.png')
            pliki.append(f'{nazwa}__{w}.png')
        atak = next((a for a in info['anims'] if WZORZEC.search(a['n']) and a['l']), None)
        if atak:
            B.js(B.isolate_animation_js(B.literal(atak['n'])) + f'Timeline.setTime({atak["l"] * 0.45}); Animator.preview(); JSON.stringify(1)')
            zrzut({'position': widoki['przod'], 'target': c, 'projection': 'perspective'}, out / f'{nazwa}__atak.png')
            pliki.append(f'{nazwa}__atak.png')
            B.js('Timeline.pause(); JSON.stringify(1)')
        B.zamknij(u)
        manifest.append({'model': nazwa, 'zrodlo': str(src), 'elementy': info['el'], 'rozmiar': z,
                         'animacje': [a['n'] for a in info['anims']], 'atak': atak and atak['n'], 'pliki': pliki})
        print(nazwa, 'ok', len(pliki), flush=True)
    (out / 'manifest.json').write_text(json.dumps(manifest, indent=1, ensure_ascii=False))


if __name__ == '__main__':
    main()
