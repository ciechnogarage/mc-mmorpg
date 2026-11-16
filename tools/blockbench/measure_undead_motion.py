"""Measure the verified active editor project's transformed cube vertices.

Does not author corrections, open tabs or overwrite prior reports. Sampling
bounds are empirical at 120 Hz, not a proof of continuous collision freedom.
"""
import argparse
import json
import math
from pathlib import Path
from review_session import ReviewSession, literal, isolate_animation_js


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', required=True)
    parser.add_argument('--out', required=True)
    parser.add_argument('--animations', nargs='+', default=['walk','attack','jump_start','jump_end','death'])
    parser.add_argument('--hz', type=int, default=120)
    args = parser.parse_args()
    if not 1 <= args.hz <= 240:
        parser.error('hz must be between 1 and 240')
    session = ReviewSession()
    manifest = session.bind(args.source)
    fixtures = session.js('const root=new THREE.Object3D(); root.position.y=3; root.rotation.z=Math.PI/2; const child=new THREE.Mesh(new THREE.BoxGeometry(2,2,2)); child.position.x=2; child.scale.x=2; root.add(child); child.updateWorldMatrix(true,false); const pos=child.geometry.getAttribute("position"); let lo=Infinity,hi=-Infinity; for(let i=0;i<pos.count;i++){const v=new THREE.Vector3().fromBufferAttribute(pos,i).applyMatrix4(child.matrixWorld);lo=Math.min(lo,v.y);hi=Math.max(hi,v.y);} child.geometry.dispose(); JSON.stringify({minY:lo,maxY:hi})')
    if abs(fixtures['minY']-3)>1e-8 or abs(fixtures['maxY']-7)>1e-8:
        raise ValueError('Independent transform fixture failed')
    manifest['transform_fixture'] = dict(fixtures, expected_minY=3, expected_maxY=7)

    source = session.expected
    clips = {a['name']:a for a in source['animations']}
    for name in args.animations:
        if name not in clips: raise ValueError('Missing animation: '+name)
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=False)
    state = session.snapshot()
    report = dict(manifest, status='INCOMPLETE', hz=args.hz, animations={},
                  scope='Editor transformed cube vertices; no runtime, blends, horizontal stance or artistic proof')
    try:
        for name in args.animations:
            length = float(clips[name]['length'])
            times = sorted(set([round(i/args.hz,9) for i in range(math.floor(length*args.hz)+1)] + [length] +
                [float(k['time']) for tr in clips[name]['animators'].values() for k in tr.get('keyframes',[])]))
            code = isolate_animation_js(literal(name)) + 'const rows=[]; for(const time of '+literal(times)+') { Timeline.setTime(time); Animator.preview(); let minimum=Infinity; const feet={}; for(const c of Cube.all) { if(c.name==="player_hitbox" || c.visibility===false)continue; let ancestor=c.parent; let hidden=false; while(ancestor && ancestor!=="root"){if(ancestor.visibility===false)hidden=true;ancestor=ancestor.parent;} if(hidden)continue; if(!c.mesh)throw new Error("Missing mesh"); c.mesh.updateWorldMatrix(true,false); const pos=c.mesh.geometry.getAttribute("position"); if(!pos)throw new Error("Missing vertices"); let lo=Infinity; let hi=-Infinity; for(let i=0;i<pos.count;i++){ const v=new THREE.Vector3().fromBufferAttribute(pos,i).applyMatrix4(c.mesh.matrixWorld); lo=Math.min(lo,v.y); hi=Math.max(hi,v.y); } minimum=Math.min(minimum,lo); let p=c.parent; let side=null; while(p && p!=="root"){if(p.name==="right_foot")side="right";if(p.name==="left_foot")side="left";p=p.parent;} if(side){if(!feet[side])feet[side]={minY:lo,maxY:hi};else{feet[side].minY=Math.min(feet[side].minY,lo);feet[side].maxY=Math.max(feet[side].maxY,hi);}} } if(!Number.isFinite(minimum))throw new Error("No finite visible geometry"); rows.push({time,minY:minimum,feet}); } JSON.stringify(rows)'
            rows = session.js(code)
            if not isinstance(rows,list) or len(rows)!=len(times): raise ValueError('Incomplete samples')
            minimum = min(row['minY'] for row in rows)
            report['animations'][name] = {'minimum_y':minimum, 'samples':rows,
                'flat_ground_penetration_detected':minimum < -.04}
            print(name, 'minY', round(minimum,6), 'samples',len(rows), flush=True)
        final_identity = session.bind(args.source)
        if any(final_identity.get(key) != manifest.get(key) for key in ('source_sha256', 'project_fingerprint')):
            raise ValueError('Source or project changed during measurement')
        report['final_identity'] = final_identity
        report['status']='MEASURED'
    except Exception as exc:
        report['error']=str(exc)
        raise
    finally:
        try:
            session.restore(state)
        except Exception as exc:
            report['status']='INCOMPLETE'
            report['restore_error']=str(exc)
            raise
        finally:
            (out/'motion.json').write_text(json.dumps(report,indent=2,allow_nan=False))
    return 1 if any(a['flat_ground_penetration_detected'] for a in report['animations'].values()) else 0

if __name__ == '__main__':
    raise SystemExit(main())
