"""Pomiar naciagu luku: gdzie koncza sie odcinki cieciwy (wierzcholek najdalszy od konca luku) wzgledem prawej dloni.
Uzycie: python3 bb_cieciwa.py model.bbmodel [czas=0.9]"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import bb_sesja as B
t = float(sys.argv[2]) if len(sys.argv) > 2 else 0.9
u = B.otworz(sys.argv[1])
kod = '''(()=>{const w=n=>{const e=Outliner.elements.find(e=>e.name===n);e.mesh.updateMatrixWorld(true);const p=e.mesh.geometry.attributes.position;const r=[];
for(let i=0;i<p.count;i++){const v=new THREE.Vector3().fromBufferAttribute(p,i).applyMatrix4(e.mesh.matrixWorld);r.push([v.x,v.y,v.z])}return r};
const c=n=>{const e=Outliner.elements.find(e=>e.name===n);const v=new THREE.Vector3();e.mesh.getWorldPosition(v);return [v.x,v.y,v.z]};
return {g:w("cieciwa_g"),d:w("cieciwa_d"),kg:c("luk_koniec_g"),kd:c("luk_koniec_d"),reka:c("dlon_p")}})()'''
r = B.js(B.isolate_animation_js(B.literal('shoot')) + f'Timeline.setTime({t}); Animator.preview(); JSON.stringify(' + kod + ')')
B.js('Timeline.pause(); JSON.stringify(1)'); B.zamknij(u)
odl = lambda a, b: sum((a[i] - b[i]) ** 2 for i in range(3)) ** 0.5
def szczyt(pts, koniec):
    far = max(pts, key=lambda p: odl(p, koniec)); pts2 = [p for p in pts if odl(p, far) < 1.6]
    return [round(sum(p[i] for p in pts2) / len(pts2), 1) for i in range(3)]
g, d = szczyt(r['g'], r['kg']), szczyt(r['d'], r['kd'])
print('szczyt gorny', g, 'dolny', d, 'dlon', [round(x, 1) for x in r['reka']])
print('rozjazd szczytow', round(odl(g, d), 1), '| gorny-dlon', round(odl(g, r['reka']), 1), '| dolny-dlon', round(odl(d, r['reka']), 1))
