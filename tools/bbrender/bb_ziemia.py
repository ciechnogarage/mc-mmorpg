"""Bramka "nic nie wchodzi w ziemie": najnizszy punkt widocznych kostek (bez hitbox/shadow) w pozie spoczynkowej
i w kazdej animacji co 0.05 s. Ponizej 0 wolno tylko animacjom z --dozwolone (domyslnie 'spawn'); generator deklaruje je w POD_ZIEMIA.
Uzycie: python3 bb_ziemia.py model.bbmodel [--tolerancja 0.05] [--dozwolone spawn:8,death:6] -> kod 1, gdy cos tonie."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bb_sesja as B  # noqa: E402

src = sys.argv[1]
# --dozwolone "spawn:8,death:6" = animacja:max glebokosc w px (bez liczby = dowolnie gleboko)
dozw = {}
for x in (sys.argv[sys.argv.index('--dozwolone') + 1].split(',') if '--dozwolone' in sys.argv else ['spawn']):
    if x:
        n, _, g = x.partition(':')
        dozw[n] = float(g) if g else float('inf')
tol = float(sys.argv[sys.argv.index('--tolerancja') + 1]) if '--tolerancja' in sys.argv else 0.05
# pomija: niewidoczne, o zerowej skali swiata (efekt ukryty skala 0) oraz - w pozie spoczynkowej - kosci fx_*
# (w grze zawsze gra jakas animacja, a ta ukrywa fx skala 0)
MIN_Y = ('(fx=>{let m=1e9,kto="";const s=new THREE.Vector3();Outliner.elements.forEach(e=>{if(!e.mesh||e.visibility===false)return;'
         'let p=e.parent,wfx=false;while(p&&p!=="root"){if(/^fx_/i.test(p.name))wfx=true;p=p.parent}'
         'if(wfx&&!fx)return;e.mesh.getWorldScale(s);if(Math.min(Math.abs(s.x),Math.abs(s.y),Math.abs(s.z))<0.08)return;'
         'const b=new THREE.Box3().setFromObject(e.mesh);if(b.min.y<m){m=b.min.y;kto=e.name}});return {m:+m.toFixed(2),kto}})')
u = B.otworz(src)
def own(code):
    B.js('ModelProject.all.find(p=>p.uuid===' + B.literal(u) + ').select(); JSON.stringify(1)')
    return B.js(code)
B.ukryj_techniczne(fx=False)  # efekty fx_ tez nie moga wchodzic w ziemie (skala 0 = punkt w pivocie)
wyniki = {'spoczynek': own('Modes.options.edit.select(); JSON.stringify(' + MIN_Y + '(false))')}
anims = own('JSON.stringify(Animation.all.map(a=>({n:a.name,l:a.length})))')
for a in anims:
    najn = None
    t = 0.0
    # do l-0.01: w t == dlugosc Blockbench wraca do pozy spoczynkowej (w grze ME przechodzi wtedy do nastepnej animacji)
    while t <= a['l'] - 0.01 + 1e-6:
        r = own(B.isolate_animation_js(B.literal(a['n'])) + f'Timeline.setTime({t}); Animator.preview(); JSON.stringify(' + MIN_Y + '(true))')
        if najn is None or r['m'] < najn['m']:
            najn = dict(r, t=round(t, 2))
        t += 0.05
    wyniki[a['n']] = najn
own('Timeline.pause(); JSON.stringify(1)')
B.zamknij(u)
zle = []
for n, r in wyniki.items():
    lim = next((g for k, g in dozw.items() if n.startswith(k)), 0.0)
    ok = r['m'] >= -(tol + lim)
    print(f"{'OK ' if ok else 'ZLE'} {n:14} min y={r['m']:6.2f} ({r['kto']}{', t=' + str(r['t']) if 't' in r else ''})")
    if not ok:
        zle.append(n)
sys.exit(1 if zle else 0)
