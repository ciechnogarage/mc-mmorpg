"""Obrys (min/max xyz) wybranych kostek w danej klatce animacji - do ukladania rekwizytow (np. luk na ziemi).
Uzycie: python3 bb_obrys.py model.bbmodel animacja czas prefiks_kostek"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import bb_sesja as B
src, anim, t, pref = sys.argv[1], sys.argv[2], float(sys.argv[3]), sys.argv[4]
u = B.otworz(src)
r = B.js(B.isolate_animation_js(B.literal(anim)) + f'Timeline.setTime({t}); Animator.preview(); JSON.stringify((()=>{{'
         'const b=new THREE.Box3();Outliner.elements.forEach(e=>{if(e.mesh&&e.name.startsWith(' + B.literal(pref) + '))b.expandByObject(e.mesh)});'
         'return {min:[b.min.x,b.min.y,b.min.z].map(x=>+x.toFixed(1)),max:[b.max.x,b.max.y,b.max.z].map(x=>+x.toFixed(1))}})())')
B.js('Timeline.pause(); JSON.stringify(1)'); B.zamknij(u)
print(r)
