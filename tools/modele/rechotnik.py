"""Rechotnik K2: model z brył SDF, 1 piksel modelu = 1 woksel."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
from bryly_sdf import Model
from bagna_idle import dodaj as dodaj_idle
import tekstura
import atlas_wspolny

MAT = {
    'skora': {'ramp': [(45,48,25),(68,75,37),(90,99,51),(117,124,69)], 'plamy': .08},
    'glowa': {'ramp': [(39,55,27),(63,82,41),(86,106,54),(112,134,74)], 'plamy': .08},
    'grzbiet': {'ramp': [(28,39,19),(45,59,27),(65,80,36),(87,103,48)], 'plamy': .11},
    'nogi': {'ramp': [(47,37,24),(69,52,31),(91,68,42),(116,90,55)], 'plamy': .07},
    'worek': {'ramp': [(10,38,37),(17,61,56),(27,83,74),(43,111,99)]},
    'warga': {'ramp': [(81,82,46),(123,122,73),(163,157,96),(198,190,129)]},
    'oko': {'ramp': [(95,79,39),(164,145,68),(212,191,104),(241,221,150)]},
    'zrenica': {'ramp': [(9,19,15),(13,25,18),(22,33,23),(36,45,30)]},
    'blik': {'ramp': [(111,179,158),(147,212,187),(182,237,214),(212,246,230)]},
    'drewno': {'ramp': [(40,32,22),(63,46,29),(88,65,40),(116,86,55)]},
    'kosc': {'ramp': [(102,85,58),(151,127,87),(199,176,126),(229,209,163)]},
    'siec': {'ramp': [(38,33,22),(65,55,35),(103,84,51),(145,123,76)]},
    'rzesa': {'ramp': [(33,41,20),(52,63,27),(69,82,34),(92,105,45)]},
    'paszcza': {'ramp': [(15,23,16),(25,33,20),(39,47,28),(55,65,37)]},
    'jezyk': {'ramp': [(69,34,38),(98,46,51),(133,67,70),(168,96,91)]},
}

def buduj(atlas=128, budzet=280, uprosc=True):
    m=Model(MAT,budzet=budzet,atlas=atlas)
    def part(n,p,parent,rot=(0,0,0)): m.czesc(n,p,parent,rot)
    part('korzen',(0,0,0),None)
    part('tulow',(0,10,1),'korzen')
    m.elipsoida('tulow',(0,12,1),(6.7,6.3,5.5),'skora')
    m.elipsoida('tulow',(0,8.8,0.5),(5.8,3.7,5),'nogi')
    part('grzbiet',(0,15,3),'tulow')
    m.elipsoida('grzbiet',(0,15,3),(6.4,3.5,5.3),'grzbiet')
    part('glowa',(0,18,-1),'tulow')
    m.elipsoida('glowa',(0,18,-2),(8.1,4.2,5.5),'glowa')
    m.elipsoida('glowa',(0,16.7,-5.2),(7.3,2.4,4.1),'glowa')
    for s in (-1,1):
        m.elipsoida('glowa',(s*4.5,20.3,-6.4),(2.8,2.6,2.8),'glowa')
    m.odejmij('glowa','elipsoida',(0,16.0,-8.0),(5.8,.8,2.1))
    for s in (-1,1):
        m.odejmij('glowa','elipsoida',(s*4.2,20,-6),(2.0,1.6,1.8))
    part('szczeka',(0,15.6,-6.5),'glowa')
    m.kapsula('szczeka',(-6.3,15.6,-8.2),(-4.5,17,-8.2),.65,.65,'warga')
    m.kapsula('szczeka',(-4.5,17,-8.2),(4.5,17,-8.2),.65,.65,'warga')
    m.kapsula('szczeka',(4.5,17,-8.2),(6.3,15.6,-8.2),.65,.65,'warga')
    m.detal('szczeka','paszcza_cien',(-4.5,16,-9.3),(4.5,17,-8.3),'paszcza')
    for s,side in [(-1,'l'),(1,'r')]:
        part('oko_'+side,(s*4.3,20,-5.5),'glowa')
        m.elipsoida('oko_'+side,(s*4.5,21.7,-7.3),(2.35,2.5,2.3),'oko')
        m.detal('oko_'+side,'zrenica_'+side,(s*4.5-.7,21,-9.8),(s*4.5+.7,22.5,-8.8),'zrenica')
        m.detal('oko_'+side,'blik_'+side,(s*4.5-.5,22,-9.9),(s*4.5+.5,23,-8.9),'blik')
    part('swiatlo_oczy',(0,20,-6),'glowa')
    part('p_paszcza',(0,16,-9),'szczeka')
    part('fx_jezyk',(0,16,-9),'szczeka')
    m.detal('fx_jezyk','jezyk_wysuwany',(-.7,15,-20),(.7,16,-9),'jezyk')
    part('worek',(0,13,-5),'tulow')
    m.elipsoida('worek',(0,12,-7),(5.2,5.8,4.4),'worek')
    m.kapsula('worek',(-5.4,16,-7),(5.4,16,-7),.9,.9,'paszcza')
    part('swiatlo_worek',(0,12,-9),'worek')
    for s,side in [(-1,'l'),(1,'r')]:
        part('udo_'+side,(s*5,8,2),'korzen')
        m.kapsula('udo_'+side,(s*5,8,2),(s*10,5,3),3.4,2.7,'nogi')
        part('golen_'+side,(s*10,5,3),'udo_'+side)
        m.kapsula('golen_'+side,(s*10,5,3),(s*7.5,2,1),2.45,1.55,'nogi')
        part('stopa_'+side,(s*7.5,2,1),'golen_'+side)
        m.elipsoida('stopa_'+side,(s*7.6,1.4,-1.1),(3.2,1.2,4.0),'nogi')
        for j,dx in enumerate((-1.8,0,1.8)):
            x=s*7.6+dx
            m.detal('stopa_'+side,f'palec_{side}_{j}',(x-.7,0,-5.4+abs(dx)*.2),(x+.7,1.1,-2.1),'nogi')
    # Dłoń po stronie +X obejmuje worek.
    part('ramie_l',(5.7,15,0),'tulow')
    m.kapsula('ramie_l',(5.7,15,0),(8.2,11,-2.5),2.25,1.85,'skora')
    part('przedramie_l',(8.2,11,-2.5),'ramie_l')
    m.kapsula('przedramie_l',(8.2,11,-2.5),(4.4,9.8,-8.9),1.9,1.55,'skora')
    part('dlon_l',(4.4,9.8,-8.9),'przedramie_l')
    m.elipsoida('dlon_l',(3.8,10.4,-10),(2.2,1.9,1.35),'skora')
    for j in range(3):
        m.detal('dlon_l',f'palec_worek_{j}',(1+j*1.25,8.8-j*.35,-11.7),(2+j*1.25,11-j*.35,-10.3),'skora')
    # Dłoń po stronie -X zaciska się na drzewcu; włócznia jest jej dzieckiem.
    part('ramie_r',(-6,15,0),'tulow')
    m.kapsula('ramie_r',(-6,15,0),(-8.7,11,-1.5),2.3,1.8,'skora')
    part('przedramie_r',(-8.7,11,-1.5),'ramie_r')
    m.kapsula('przedramie_r',(-8.7,11,-1.5),(-10.2,10,-4.8),1.8,1.45,'skora')
    part('dlon_r',(-10.2,10,-4.8),'przedramie_r')
    m.elipsoida('dlon_r',(-10.4,10,-5),(1.8,1.7,1.7),'skora')
    part('wlocznia',(-10.5,10,-5),'dlon_r')
    m.pudlo('wlocznia',(-11,0,-5.6),(-10,22,-4.6),'drewno')
    m.kapsula('wlocznia',(-10.5,22,-5.1),(-10.5,26,-5.1),2.0,.35,'kosc')
    m.kapsula('wlocznia',(-10.5,25,-5.1),(-10.5,29,-5.1),1.4,.25,'kosc')
    m.detal('wlocznia','rybia_zawieszka',(-13.5,18,-6),(-11.5,21,-4),'kosc')
    m.detal('wlocznia','osc_lewa',(-12.2,21,-5.6),(-11.1,23,-4.6),'kosc')
    m.detal('wlocznia','osc_prawa',(-9.9,21,-5.6),(-8.8,23,-4.6),'kosc')
    for j,y in enumerate((3,)):
        m.detal('wlocznia',f'owijka_{j}',(-11.4,y,-6),(-9.6,y+1,-4.2),'siec')
    m.detal('dlon_r','palec_na_drzewcu',(-11.5,9.1,-6.5),(-9.3,10.2,-5.4),'skora')
    # Sieć i amulet rybiej ości.
    part('siec',(0,8,-4),'tulow')
    for j,(a,b) in enumerate([((-5,8,-7.9),(0,3.7,-8.2)),((0,8,-7.9),(5,3.7,-8.2)),
                              ((-4,7,-8.1),(4,7,-8.1))]):
        m.kapsula('siec',a,b,.9 if j == 0 else .6,.75 if j == 0 else .5,'siec')
    m.detal('siec','amulet_trzon',(-.5,2.9,-9.3),(.5,6.9,-8.3),'kosc')
    m.detal('siec','amulet_glowa',(-1.2,6.5,-9.4),(1.2,7.5,-8.3),'kosc')
    m.czesci['siec'].vox[(3,4,-9)]='siec'  # łączy dolne oczko z ukośną nitką
    part('rzesa',(0,19,2),'glowa')
    for j,(x,y,z,length) in enumerate([(-7.8,20,-5,6),(-6.5,19,-7,5),(7.8,20,-5,6),(6.5,18,-6.5,5),
                                       (-5,19,6,5),(0,19,6,5),(5,19,6,5)]):
        m.detal('rzesa',f'rzesa_{j}',(x-.45,y-length,z-.5),(x+.55,y,z+.5),'rzesa')
    m.detal('grzbiet','pas_kregoslupa',(-.5,13,7.5),(.5,19,8.5),'glowa')
    # ME brightness{p=swiatlo_} obejmuje tylko kostki leżące bezpośrednio w kości.
    m.czesci['swiatlo_worek'].vox.update(m.czesci['worek'].vox)
    m.czesci['worek'].vox.clear()
    for oko in ('oko_l', 'oko_r'):
        m.czesci['swiatlo_oczy'].vox.update(m.czesci[oko].vox)
        m.czesci[oko].vox.clear()
        m.czesci['swiatlo_oczy'].detale.extend(m.czesci[oko].detale)
        m.czesci[oko].detale.clear()
    # Mostek wewnątrz czaszki łączy obie oprawy w jednej kości światła.
    m.pudlo('swiatlo_oczy',(-3,20,-5.5),(3,21,-4.5),'oko')
    # Zwisy są przytwierdzone do głowy i dzielą jej transformację.
    m.czesci['glowa'].vox.update(m.czesci['rzesa'].vox)
    m.czesci['rzesa'].vox.clear()
    for detal in m.czesci['rzesa'].detale:
        cel = 'grzbiet' if detal[0] in ('rzesa_4', 'rzesa_5', 'rzesa_6') else 'glowa'
        m.czesci[cel].detale.append(detal)
    m.czesci['rzesa'].detale.clear()
    if uprosc:
        m.uprosc(['tulow','grzbiet','glowa','udo_l','udo_r','stopa_l','stopa_r',
                  'ramie_l','ramie_r','przedramie_l','przedramie_r'])
    m.odbij_x()  # Włócznia po lewej stronie rzutu, jak na koncepcie K2.
    return m

def main():
    p=argparse.ArgumentParser()
    p.add_argument('out',nargs='?',default='/opt/docker/mc/data/world1/output/lochy/bagna/modele/rechotnik')
    args=p.parse_args()
    dest=Path(args.out)
    model=buduj()
    old=tekstura.atlas
    try:
        tekstura.atlas=atlas_wspolny.atlas
        report=model.zapisz(dest/'bagna_rechotnik.bbmodel','bagna_rechotnik',
                            hitbox=(16,24,19),tag_y=27)
    finally:
        tekstura.atlas=old
    dodaj_idle(dest/'bagna_rechotnik.bbmodel',2.0,{'worek':('scale',(0.025,0.035,0.035))})
    from anim_rechotnik import main as dodaj_animacje
    dodaj_animacje(dest/'bagna_rechotnik.bbmodel')
    (dest/'raport_sdf.json').write_text(json.dumps(report,indent=2,ensure_ascii=False))
    print('Bryły SDF:',report['razem'],'kostek; atlas',f"{report['atlas_zajetosc']:.1%}")

if __name__=='__main__':
    main()
