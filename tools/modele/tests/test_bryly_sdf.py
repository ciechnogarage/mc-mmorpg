"""Testy kontraktu brył SDF i eksportu ModelEngine."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import bryly_sdf as sdf


MAT={'a': {'ramp': [(1,2,3),(11,12,13),(21,22,23),(31,32,33)]},
     'b': {'ramp': [(4,5,6),(14,15,16),(24,25,26),(34,35,36)]}}

def model():
    m=sdf.Model(MAT,atlas=128)
    m.czesc('root',(0,0,0))
    m.czesc('lewa',(-2,2,0),'root')
    return m

def test_segment_dziecka_trafia_w_punkt_swiata_po_obrotach():
    """BB składa Rz·Ry·Rx kości dziecka z obrotami dwóch przodków."""
    def R(katy):
        x, y, z = np.radians(katy)
        Rx = np.array([[1, 0, 0], [0, np.cos(x), -np.sin(x)], [0, np.sin(x), np.cos(x)]])
        Ry = np.array([[np.cos(y), 0, np.sin(y)], [0, 1, 0], [-np.sin(y), 0, np.cos(y)]])
        Rz = np.array([[np.cos(z), -np.sin(z), 0], [np.sin(z), np.cos(z), 0], [0, 0, 1]])
        return Rz @ Ry @ Rx

    m = sdf.Model(MAT)
    root_rot, parent_rot = (17, -13, 29), (-21, 31, -8)
    root_pivot = np.array([2., 4., -3.])
    parent_pivot = np.array([5., 9., 1.])
    P = np.array([7., 12., -2.])
    m.czesc('root', root_pivot, obrot_kosci=root_rot)
    m.czesc('parent', parent_pivot, 'root', obrot_kosci=parent_rot)
    Rparent = R(root_rot) @ R(parent_rot)
    A = root_pivot + R(root_rot) @ (parent_pivot - root_pivot) + Rparent @ (P - parent_pivot)
    B = A + np.array([6., -9., -4.])
    Rworld, length = m.segment('child', 'parent', Rparent, A, B, 2, 'a', P=P)
    child = m.czesci['child']
    end_local = P + R(child.obrot_kosci) @ np.array([0., length, 0.])
    end_world = root_pivot + R(root_rot) @ (parent_pivot - root_pivot) + Rparent @ (end_local - parent_pivot)
    assert np.linalg.norm(end_world - B) <= .5
    assert np.allclose(Rparent @ R(child.obrot_kosci), Rworld, atol=1e-8)

def test_prymitywy_material_i_odejmowanie():
    m=model()
    m.elipsoida('lewa',(-2,2,0),(2,2,2),'a')
    assert m.czesci['lewa'].vox
    m.pudlo('lewa',(-3,1,-1),(-1,3,1),'b')
    assert m.czesci['lewa'].vox[(-2,1,0)]=='b'
    before=len(m.czesci['lewa'].vox)
    m.odejmij('lewa','pudlo',(-3,1,-1),(-1,3,1))
    assert len(m.czesci['lewa'].vox)<before
    m.kapsula('lewa',(-3,3,0),(-3,6,0),1.2,.5,'a')
    assert any(y>=4 for x,y,z in m.czesci['lewa'].vox)

def test_obrot_przed_wokselizacja_i_lustro():
    m=model()
    m.czesci['lewa'].obrot=(0,0,90)
    m.pudlo('lewa',(-2,2,0),(-1,6,1),'a')
    xs=[p[0] for p in m.czesci['lewa'].vox]
    ys=[p[1] for p in m.czesci['lewa'].vox]
    assert max(xs)-min(xs)>max(ys)-min(ys)
    m.detal('lewa','znak_l',(-4,1,0),(-3,2,1),'b')
    m.lustro('lewa','prawa',(2,2,0),'root')
    assert {(-x-1,y,z) for x,y,z in m.czesci['lewa'].vox}==set(m.czesci['prawa'].vox)
    assert m.czesci['prawa'].detale[0][0]=='znak_r'

def test_greedy_najlepszy_kierunek_i_culling():
    m=model()
    m.pudlo('lewa',(-4,0,-2),(0,4,2),'a')
    boxes,report=m.siatka()
    assert report['lewa']['kostki']==1
    assert report['lewa']['usuniete_niewidoczne']==0
    assert boxes['lewa'][0][:2]==((-4,0,-2),(0,4,2))
    # Wynik nie może być gorszy od żadnej z sześciu kolejności.
    m.czesci['lewa'].vox.pop((-4,0,-2))
    best=len(m.siatka()[0]['lewa'])
    assert best==min(len(sdf._greedy(m.czesci['lewa'].vox,a))
                     for a in sdf.permutations(range(3)))

def test_malarz_tylko_rampa_i_ao():
    rng=np.random.default_rng(8)
    mat={'ramp': MAT['a']['ramp'], 'plamy': .12, 'jasna_krawedz': True}
    tile=sdf.malarz('north',12,8,mat,rng)
    assert tile.shape==(8,12,4)
    assert set(map(tuple,tile[:,:,:3].reshape(-1,3))).issubset(set(MAT['a']['ramp']))
    assert tuple(tile[-1,0,:3])==MAT['a']['ramp'][1]
    try:
        sdf.malarz('north',2,2,{'ramp':MAT['a']['ramp'],'plamy':.13},rng)
        assert False, 'brak błędu dla gęstości > 12%'
    except ValueError:
        pass

def test_eksport_rig_atlas_budzet():
    m=model()
    m.pudlo('lewa',(-3,0,-1),(-1,3,1),'a')
    m.detal('lewa','oko',(-3,2,-2),(-2,3,-1),'b')
    with tempfile.TemporaryDirectory() as td:
        path=Path(td)/'test.bbmodel'
        report=m.zapisz(path,'test',(4,4,3),5,swiatla=[('swiatlo_test',(0,2,0))])
        bb=json.loads(path.read_text())
        m.budzet=1
        try:
            m.zapisz(path,'test',(4,4,3),5,swiatla=[('swiatlo_test',(0,2,0))])
            assert False, 'brak błędu budżetu'
        except ValueError as e:
            assert 'budżet' in str(e)
    assert report['razem']==2
    assert bb['resolution']['width']==128
    assert {'hitbox','tag_name','swiatlo_test','lewa'}.issubset({b['name'] for b in bb['outliner']} | 
        {c['name'] for b in bb['outliner'] for c in b['children'] if isinstance(c,dict)})
    assert 'data:image/png;base64,' in bb['textures'][0]['source']

def test_spirala_skorupy():
    m=model();m.czesc('spirala',(0,10,0))
    m.spirala_skorupy('spirala',(0,10,0),5,2,3,'a','b')
    assert {'a','b'} <= set(m.czesci['spirala'].vox.values())
    assert any(abs(x)>=2 and mat=='b' for (x,y,z),mat in m.czesci['spirala'].vox.items())

def test_jajo_oplot():
    m=model();m.czesc('jajo',(0,10,0))
    m.jajo_oplot('jajo',(0,9,0),4,7,3,5,'a','b')
    assert {'a','b'} <= set(m.czesci['jajo'].vox.values())
    assert max(y for x,y,z in m.czesci['jajo'].vox)>14

def test_noga_stawowa():
    m=model();m.czesc('noga',(0,10,0))
    m.noga_stawowa('noga',(0,8,0),(4,15,0),(8,0,0),(1.6,1.1,.7),'a')
    assert any(y>=14 for x,y,z in m.czesci['noga'].vox)
    assert any(y==0 for x,y,z in m.czesci['noga'].vox)

def test_plomien():
    m=model();m.czesc('plomien',(0,10,0))
    m.plomien('plomien',(0,1,0),3,[(-30,8),(15,11),(45,7)],'a','b')
    assert max(y for x,y,z in m.czesci['plomien'].vox)>=12
    assert {'a','b'} <= set(m.czesci['plomien'].vox.values())

def test_wneka():
    m=model();m.czesc('jama',(0,10,0))
    m.elipsoida('jama',(0,8,0),(5,5,5),'a')
    before=len(m.czesci['jama'].vox)
    m.wneka('jama',(0,8,-3),(3,3,3),4,('a','b'))
    assert len(m.czesci['jama'].vox)<before
    assert 'b' in m.czesci['jama'].vox.values()

def test_pierscienie():
    m=model();m.czesc('segment',(0,10,0))
    m.elipsoida('segment',(0,5,0),(3,3,10),'a')
    before=len(m.czesci['segment'].vox)
    m.pierscienie('segment','z',3,.3,'b')
    assert len(m.czesci['segment'].vox)<before
    assert 'b' in m.czesci['segment'].vox.values()

if __name__=='__main__':
    for name,fn in sorted(globals().items()):
        if name.startswith('test_') and callable(fn):
            fn()
            print(name,'OK')
