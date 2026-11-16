"""Bryły wokselowe 1 px dla modeli Blockbench/ModelEngine.

Współrzędne są w pikselach modelu, front jest skierowany ku -Z. Każda
część ma własną kość; zapis nie scala wokseli pomiędzy kośćmi.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from itertools import permutations
from math import asin, atan2, ceil, cos, degrees, floor, pi, radians, sin, sqrt
from pathlib import Path

import numpy as np

import bb
import tekstura


@dataclass
class Czesc:
    nazwa: str
    pivot: tuple[float, float, float]
    rodzic: str | None
    obrot: tuple[float, float, float] = (0, 0, 0)
    obrot_kosci: tuple[float, float, float] = (0, 0, 0)   # zapisywany obrót kości (detale w obróconej kości)
    vox: dict[tuple[int, int, int], str] = field(default_factory=dict)
    detale: list[tuple[str, tuple[float, ...], tuple[float, ...], str]] = field(default_factory=list)


def _obracaj(p, pivot, rot):
    x, y, z = (p[i] - pivot[i] for i in range(3))
    for axis, deg in enumerate(rot):
        c, s = cos(radians(deg)), sin(radians(deg))
        if axis == 0:
            y, z = y*c-z*s, y*s+z*c
        elif axis == 1:
            x, z = x*c+z*s, -x*s+z*c
        else:
            x, y = x*c-y*s, x*s+y*c
    return tuple(v + pivot[i] for i, v in enumerate((x, y, z)))


def _greedy(vox, osie):
    pozostale = dict(vox)
    wynik = []
    while pozostale:
        p = min(pozostale, key=lambda q: (q[1], q[2], q[0]))
        mat = pozostale[p]
        lo, hi = list(p), [v+1 for v in p]
        for ax in osie:
            while True:
                q = hi[ax]
                other = [i for i in range(3) if i != ax]
                if all(pozostale.get(tuple(q if i == ax else (a if i == other[0] else b)
                                           for i in range(3))) == mat
                       for a in range(lo[other[0]], hi[other[0]])
                       for b in range(lo[other[1]], hi[other[1]])):
                    hi[ax] += 1
                else:
                    break
        for x in range(lo[0], hi[0]):
            for y in range(lo[1], hi[1]):
                for z in range(lo[2], hi[2]):
                    del pozostale[x, y, z]
        wynik.append((tuple(lo), tuple(hi), mat))
    return wynik


def _najlepsza(vox):
    if not vox:
        return [], (0, 1, 2)
    wybory = [(_greedy(vox, osie), osie) for osie in permutations(range(3))]
    return min(wybory, key=lambda w: (len(w[0]), w[1]))


def _bez_wnetrza(vox, pelne):
    kierunki = ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1))
    return {p:m for p,m in vox.items() if any(tuple(p[i]+d[i] for i in range(3)) not in pelne
                                      for d in kierunki)}


def malarz(sciana, w, h, mat, rng):
    """Cztery płaskie tony i rzadkie, całe plamy; bez szumu na piksel."""
    rampa = np.asarray(mat['ramp'], dtype=np.uint8)
    if rampa.shape != (4, 3):
        raise ValueError('materiał wymaga rampy 4 × RGB')
    ton = {'up': 3, 'north': 2, 'west': 2, 'east': 1, 'south': 1, 'down': 0}[sciana]
    out = np.empty((h, w, 4), np.uint8)
    out[:, :, :3], out[:, :, 3] = rampa[ton], 255
    gestosc = float(mat.get('plamy', 0))
    if not 0 <= gestosc <= 0.12:
        raise ValueError('gęstość plam musi być w zakresie 0–0.12')
    if gestosc and w >= 2 and h >= 1:
        # Liczba plam wynika z pokrytej powierzchni, bez losowania każdego piksela.
        n = int(round(w*h*gestosc/4))
        for _ in range(n):
            pw = min(w, int(rng.integers(2, 4)))
            ph = min(h, int(rng.integers(1, 3)))
            x = int(rng.integers(0, w-pw+1)); y = int(rng.integers(0, h-ph+1))
            delta = -1 if rng.random() < .5 else 1
            out[y:y+ph, x:x+pw, :3] = rampa[max(0, min(3, ton+delta))]
    if sciana not in ('up', 'down'):
        if h >= 4:   # AO tylko na wysokich ścianach: na niskich kostkach dawało poziome prążki (CLAUDE 03.10)
            out[-1, :, :3] = rampa[max(0, ton-1)]
        if mat.get('jasna_krawedz') and h >= 3:
            out[0, :, :3] = rampa[min(3, ton+1)]
    return out


class Model:
    def __init__(self, materialy, budzet=250, atlas=128):
        if atlas not in (128, 256):
            raise ValueError('atlas musi mieć 128 lub 256 px')
        self.materialy, self.budzet, self.atlas = materialy, budzet, atlas
        self.czesci: dict[str, Czesc] = {}

    def czesc(self, nazwa, pivot, rodzic=None, obrot=(0, 0, 0), obrot_kosci=(0, 0, 0)):
        if nazwa in self.czesci:
            raise ValueError(f'powtórzona kość: {nazwa}')
        if rodzic is not None and rodzic not in self.czesci:
            raise ValueError(f'brak kości nadrzędnej: {rodzic}')
        c = Czesc(nazwa, tuple(pivot), rodzic, tuple(obrot), tuple(obrot_kosci))
        self.czesci[nazwa] = c
        return c

    def segment(self, nazwa, rodzic, R_rodzica, A, B, grubosc, material, koniec=None, P=None):
        """Segment w kości: +Y od A do B w świecie; P jest pivotem w układzie rodzica.

        Zwraca macierz obrotu świata i długość. Kolejność Eulera BB: Rz·Ry·Rx.
        """
        d = np.asarray(B, float) - np.asarray(A, float)
        L = float(np.linalg.norm(d))
        if L <= 0 or grubosc <= 0:
            raise ValueError('segment wymaga odcinka i dodatniej grubości')
        d /= L
        a = asin(d[2])
        c = atan2(-d[0], d[1])
        ca, sa, cc, sc = cos(a), sin(a), cos(c), sin(c)
        Rx = np.array([[1, 0, 0], [0, ca, -sa], [0, sa, ca]])
        Rz = np.array([[cc, -sc, 0], [sc, cc, 0], [0, 0, 1]])
        R_swiat = Rz @ Rx
        R_lok = np.asarray(R_rodzica, float).T @ R_swiat
        y = asin(float(np.clip(-R_lok[2, 0], -1, 1)))
        x = atan2(R_lok[2, 1], R_lok[2, 2])
        z = atan2(R_lok[1, 0], R_lok[0, 0])
        pivot = tuple(P) if P is not None else tuple(A)
        self.czesc(nazwa, pivot, rodzic, obrot_kosci=(degrees(x), degrees(y), degrees(z)))
        h = grubosc / 2
        self.detal(nazwa, nazwa + '_seg', (pivot[0]-h, pivot[1], pivot[2]-h),
                   (pivot[0]+h, pivot[1]+L, pivot[2]+h), material)
        if koniec:
            self.detal(nazwa, nazwa + '_kon', (pivot[0]-h, pivot[1]+L-3, pivot[2]-h),
                       (pivot[0]+h, pivot[1]+L, pivot[2]+h), koniec)
        return R_swiat, L

    def _rysuj(self, czesc, lo, hi, zawiera, material=None, odejmij=False):
        c = self.czesci[czesc]
        if not odejmij and material not in self.materialy:
            raise KeyError(material)
        for x in range(floor(lo[0])-1, ceil(hi[0])+1):
            for y in range(max(0, floor(lo[1])-1), ceil(hi[1])+1):
                for z in range(floor(lo[2])-1, ceil(hi[2])+1):
                    # Obrót kształtu odbywa się przez próbkowanie odwrotne.
                    p = _odwrotny((x+.5,y+.5,z+.5), c.pivot, c.obrot)
                    if zawiera(p):
                        if odejmij:
                            c.vox.pop((x,y,z), None)
                        else:
                            c.vox[x,y,z] = material

    def elipsoida(self, czesc, srodek, promienie, material):
        if any(r <= 0 for r in promienie):
            raise ValueError('promienie muszą być dodatnie')
        c = self.czesci[czesc]
        lo, hi = _bbox_obrocony(srodek, promienie, c)
        self._rysuj(czesc, lo, hi, lambda p: sum(((p[i]-srodek[i])/promienie[i])**2 for i in range(3)) <= 1, material)

    def pudlo(self, czesc, lo, hi, material):
        if any(lo[i] >= hi[i] for i in range(3)):
            raise ValueError('puste pudło')
        c = self.czesci[czesc]
        a,b = _bbox_obrocony(tuple((lo[i]+hi[i])/2 for i in range(3)), tuple((hi[i]-lo[i])/2 for i in range(3)), c)
        self._rysuj(czesc,a,b,lambda p: all(lo[i] <= p[i] < hi[i] for i in range(3)),material)

    def kapsula(self, czesc, a, b, promien_a, promien_b, material):
        """Odcinek z liniowo zmiennym promieniem; okrągłe zakończenia."""
        a,b=np.asarray(a,float),np.asarray(b,float)
        v=b-a; d=float(v@v)
        if d < 1e-9 or min(promien_a,promien_b) <= 0:
            raise ValueError('kapsuła wymaga odcinka i dodatnich promieni')
        c=self.czesci[czesc]
        lo,hi=_bbox_obrocony(tuple((a+b)/2),tuple(abs(v[i])/2+max(promien_a,promien_b) for i in range(3)),c)
        def inside(p):
            q=np.asarray(p,float); t=float(np.clip((q-a)@v/d,0,1))
            r=promien_a+(promien_b-promien_a)*t
            return float(np.linalg.norm(q-(a+t*v))) <= r
        self._rysuj(czesc,lo,hi,inside,material)

    def spirala_skorupy(self, czesc, srodek, r0, zwoje, grubosc, skorupa, spirala,
                       os='x'):
        """Wypukła tarcza muszli i wypukły zwój na obu widocznych bokach.

        `os` jest osią grubości; zwój biegnie po pozostałych dwóch osiach.
        """
        if os not in 'xyz' or r0 <= 2 or zwoje < 1 or grubosc < 1:
            raise ValueError('niepoprawne wymiary spirali')
        ax='xyz'.index(os); uv=[i for i in range(3) if i != ax]
        radii=[r0,r0,r0]; radii[ax]=grubosc/2
        self.elipsoida(czesc,srodek,radii,skorupa)
        c=self.czesci[czesc]
        # Łuk na powierzchni tarczy, a nie malowany znak na płaskim pudełku.
        for side in (-1,1):
            last=None
            steps=max(36,int(28*zwoje))
            for i in range(steps+1):
                t=i/steps; angle=2*pi*zwoje*t
                rad=max(.55,r0*(1-t)*.88)
                pos=list(srodek)
                pos[uv[0]]+=rad*cos(angle)
                pos[uv[1]]+=rad*sin(angle)
                pos[ax]+=side*(grubosc/2+max(.1,.5*(1-(rad/r0)**2)))
                if last is not None and sum((pos[j]-last[j])**2 for j in range(3))>.01:
                    self.kapsula(czesc,last,pos,.55,.55,spirala)
                last=pos

    def jajo_oplot(self, czesc, srodek, rx, ry, rz, pasma, jajo, przedza):
        """Jajo zwężone u góry, z jednopikselowymi krzyżującymi się nićmi."""
        if min(rx,ry,rz)<=1 or pasma<2:
            raise ValueError('jajo wymaga promieni >1 i co najmniej dwóch pasm')
        lo=(srodek[0]-rx,srodek[1]-ry,srodek[2]-rz)
        hi=(srodek[0]+rx,srodek[1]+ry,srodek[2]+rz)
        def inside(p):
            h=(p[1]-srodek[1])/ry
            taper=1-.27*max(h,0)
            return ((p[0]-srodek[0])/(rx*taper))**2+h*h+((p[2]-srodek[2])/(rz*taper))**2<=1
        self._rysuj(czesc,lo,hi,inside,jajo)
        c=self.czesci[czesc]
        # Woksele nici leżą na zewnętrznej powierzchni, więc nie zostają
        # przykryte przy meshingu; pochyłe pasma przechodzą także przez tył.
        surface=_bez_wnetrza(c.vox,set(c.vox))
        for p in surface:
            x,y,z=(p[i]+.5-srodek[i] for i in range(3))
            ring=any(abs(y-(-ry+(i+1)*2*ry/(pasma+1)))<.5 for i in range(pasma))
            diagonal=abs(x-.43*y)<.48 or abs(x+.43*y)<.48
            if ring or diagonal:
                c.vox[p]=przedza

    def noga_stawowa(self, czesc, biodro, kolano, stopa, grubosci, material):
        """Trzy ciągłe, zwężające się człony z zakrytym kolanem."""
        if len(grubosci)!=3 or min(grubosci)<=0:
            raise ValueError('grubości nogi muszą być trzema dodatnimi liczbami')
        parts=(czesc,czesc,czesc) if isinstance(czesc,str) else tuple(czesc)
        if len(parts)!=3:
            raise ValueError('noga wymaga jednej lub trzech kości')
        ankle=tuple(kolano[i]+.78*(stopa[i]-kolano[i]) for i in range(3))
        self.kapsula(parts[0],biodro,kolano,grubosci[0],grubosci[1],material)
        self.kapsula(parts[1],kolano,ankle,grubosci[1],grubosci[2],material)
        self.kapsula(parts[2],ankle,stopa,grubosci[2],max(.8,grubosci[2]*.75),material)

    def plomien(self, czesc, srodek, r_podstawy, jezyki, zewnetrzny, rdzen):
        """Dolna łza z węższym rdzeniem i wygiętymi językami."""
        if r_podstawy<=1 or not jezyki:
            raise ValueError('płomień wymaga podstawy i języków')
        x,y,z=srodek
        self.elipsoida(czesc,(x,y+r_podstawy*.55,z),
                       (r_podstawy,r_podstawy*1.35,r_podstawy*.8),zewnetrzny)
        self.elipsoida(czesc,(x,y+r_podstawy*.65,z-r_podstawy*.2),
                       (r_podstawy*.52,r_podstawy*.9,r_podstawy*.45),rdzen)
        for kat,dl in jezyki:
            a=radians(kat)
            base=(x+sin(a)*r_podstawy*.55,y+r_podstawy,z+cos(a)*r_podstawy*.35)
            bend=(base[0]+sin(a)*dl*.22,base[1]+dl*.52,base[2]-dl*.09)
            tip=(base[0]+sin(a)*dl*.38,base[1]+dl,base[2]-dl*.2)
            self.kapsula(czesc,base,bend,r_podstawy*.35,max(.65,r_podstawy*.2),zewnetrzny)
            self.kapsula(czesc,bend,tip,max(.65,r_podstawy*.2),.5,zewnetrzny)

    def wneka(self, czesc, srodek, promienie, glebokosc, kolory):
        """Wytnij otwór od strony -Z i zabarw jego dno oraz ściany dwoma tonami."""
        if glebokosc<3 or len(kolory)!=2 or min(promienie)<=0:
            raise ValueError('wnęka wymaga głębokości >=3 i dwóch kolorów')
        if any(mat not in self.materialy for mat in kolory):
            raise KeyError(kolory)
        c=self.czesci[czesc]
        cx,cy,cz=srodek; rx,ry,rz=promienie
        original=dict(c.vox)
        for p in original:
            x,y,z=(p[i]+.5 for i in range(3))
            u=((x-cx)/rx)**2+((y-cy)/ry)**2
            if u<=1 and cz<=z<=cz+glebokosc:
                c.vox.pop(p,None)
        # Kolorujemy istniejące ściany graniczne, bez wypełniania wycięcia.
        for p in list(c.vox):
            x,y,z=(p[i]+.5 for i in range(3))
            if z<cz-1 or z>cz+glebokosc+1:
                continue
            if ((x-cx)/rx)**2+((y-cy)/ry)**2<=1.45 and any(
                    (p[0]+dx,p[1]+dy,p[2]+dz) not in c.vox
                    for dx,dy,dz in ((0,0,-1),(0,0,1),(1,0,0),(-1,0,0),(0,1,0),(0,-1,0))):
                c.vox[p]=kolory[0] if z<cz+glebokosc-1 else kolory[1]

    def pierscienie(self, czesc, os, co_ile, przewezenie, pas):
        """Przewężaj istniejący korpus i zaznacz jego obwodowe pierścienie."""
        if os not in 'xyz' or co_ile<2 or not 0<przewezenie<1:
            raise ValueError('niepoprawne parametry pierścieni')
        if pas not in self.materialy:
            raise KeyError(pas)
        c=self.czesci[czesc]; ax='xyz'.index(os)
        if not c.vox:return
        others=[i for i in range(3) if i!=ax]
        center=[(min(p[i] for p in c.vox)+max(p[i] for p in c.vox)+1)/2 for i in range(3)]
        maxr=[max(abs(p[i]+.5-center[i]) for p in c.vox) for i in range(3)]
        start=min(p[ax] for p in c.vox)
        for p in list(c.vox):
            phase=(p[ax]-start)%co_ile
            if phase==0:
                radius=sqrt(sum(((p[i]+.5-center[i])/max(1,maxr[i]))**2 for i in others))
                if radius>1-przewezenie:
                    del c.vox[p]
                else:
                    c.vox[p]=pas
            elif phase==1:
                c.vox[p]=pas

    def odejmij(self, czesc, ksztalt, *args):
        """Odejmij elipsoida(srodek,promienie) lub pudlo(lo,hi)."""
        c=self.czesci[czesc]
        if ksztalt == 'elipsoida':
            mid,r=args
            lo,hi=_bbox_obrocony(mid,r,c)
            zaw=lambda p: sum(((p[i]-mid[i])/r[i])**2 for i in range(3)) <= 1
        elif ksztalt == 'pudlo':
            a,b=args
            lo,hi=_bbox_obrocony(tuple((a[i]+b[i])/2 for i in range(3)),tuple((b[i]-a[i])/2 for i in range(3)),c)
            zaw=lambda p: all(a[i] <= p[i] < b[i] for i in range(3))
        else:
            raise ValueError(ksztalt)
        self._rysuj(czesc,lo,hi,zaw,odejmij=True)

    def detal(self, czesc, nazwa, lo, hi, material):
        if material not in self.materialy:
            raise KeyError(material)
        if any(hi[i]-lo[i] < 1-1e-6 for i in range(3)):
            raise ValueError('detal musi mieć co najmniej 1 px w każdym wymiarze')
        self.czesci[czesc].detale.append((nazwa,tuple(lo),tuple(hi),material))

    def dosun_detale(self, pomin=()):
        """Detal, który nie styka się z żadnym wokselem ani innym detalem (świat, kości bez obrot_kosci),
        wydłuż wzdłuż osi najbliższego woksela, aż go dotknie. pomin = nazwy kości (np. orbitujące iskry)."""
        pelne = set()
        for c in self.czesci.values():
            if any(c.obrot_kosci):
                continue
            pelne.update(c.vox)
        def styka(lo, hi):
            # styk ŚCIANĄ (jak bb_spojnosc): przecięcie z tolerancją 0.1 we wszystkich osiach i dodatnia część wspólna w >= 2 osiach
            for x in range(floor(lo[0]) - 1, ceil(hi[0]) + 1):
                for y in range(floor(lo[1]) - 1, ceil(hi[1]) + 1):
                    for z in range(floor(lo[2]) - 1, ceil(hi[2]) + 1):
                        if (x, y, z) not in pelne:
                            continue
                        v = (x, y, z); dod = 0; ok = True
                        for k in range(3):
                            a, b = max(lo[k], v[k]), min(hi[k], v[k] + 1)
                            if b < a - .1:
                                ok = False; break
                            dod += b - a > 1e-6
                        if ok and dod >= 2:
                            return True
            return False
        poprawione = []
        for c in self.czesci.values():
            if c.nazwa in pomin or any(c.obrot_kosci):
                continue
            for i, (n, lo, hi, mat) in enumerate(c.detale):
                if styka(lo, hi) or not pelne:
                    continue
                mid = [(lo[k] + hi[k]) / 2 for k in range(3)]
                q = min(pelne, key=lambda v: sum((v[k] + .5 - mid[k]) ** 2 for k in range(3)))
                lo, hi = list(lo), list(hi)
                ax = max(range(3), key=lambda k: abs(q[k] + .5 - mid[k]))
                if q[ax] + .5 > mid[ax]:
                    hi[ax] = max(hi[ax], q[ax])
                else:
                    lo[ax] = min(lo[ax], q[ax] + 1)
                c.detale[i] = (n, tuple(lo), tuple(hi), mat)
                poprawione.append(n)
        return poprawione

    def lustro(self, zrodlo, nazwa, pivot, rodzic=None):
        """Odbij część L/R w płaszczyźnie X=0 wraz z detalami."""
        old=self.czesci[zrodlo]
        new=self.czesc(nazwa,pivot,rodzic)
        new.vox={(-x-1,y,z):m for (x,y,z),m in old.vox.items()}
        new.detale=[(n.replace('_l','_r'),(-hi[0],lo[1],lo[2]),(-lo[0],hi[1],hi[2]),m)
                    for n,lo,hi,m in old.detale]
        return new

    def odbij_x(self):
        """Odbij gotowy model, łącznie z pivotami, bez zmiany hierarchii."""
        for c in self.czesci.values():
            c.pivot=(-c.pivot[0],c.pivot[1],c.pivot[2])
            c.vox={(-x-1,y,z):m for (x,y,z),m in c.vox.items()}
            c.detale=[(n,(-hi[0],lo[1],lo[2]),(-lo[0],hi[1],hi[2]),m)
                       for n,lo,hi,m in c.detale]

    def uprosc(self, nazwy, krok=2, prog=.375):
        """Zaokrąglij duże bryły do stopni po `krok` px przed meshingiem.

        Siatka nadal ma 1 px i detale zachowują pełną rozdzielczość.
        """
        if krok < 2 or not 0 < prog <= 1:
            raise ValueError('błędne parametry uproszczenia')
        for n in nazwy:
            vox=self.czesci[n].vox
            groups={}
            for p,mat in vox.items():
                base=tuple(v//krok*krok for v in p)
                groups.setdefault(base,{}).setdefault(mat,0)
                groups[base][mat]+=1
            new={}
            for base,counts in groups.items():
                mat,count=max(counts.items(),key=lambda a:a[1])
                if count/(krok**3) < prog:
                    continue
                for x in range(base[0],base[0]+krok):
                    for y in range(max(0,base[1]),base[1]+krok):
                        for z in range(base[2],base[2]+krok):
                            new[x,y,z]=mat
            self.czesci[n].vox=new

    def siatka(self, usun_niewidoczne=True):
        wynik={}; raport={}
        for nazwa,c in self.czesci.items():
            full,osie=_najlepsza(c.vox)
            wybrane=full; ukryte=0
            if usun_niewidoczne:
                # Kości mogą później obracać się niezależnie: sąsiednia część
                # nie może ukryć powierzchni tej kości na stałe.
                reduced=_bez_wnetrza(c.vox,set(c.vox))
                stripped,axes2=_najlepsza(reduced)
                if len(stripped)<len(full):
                    wybrane,osie=stripped,axes2
                    ukryte=len(c.vox)-len(reduced)
            wynik[nazwa]=wybrane
            raport[nazwa]={'woksele':len(c.vox),'kostki':len(wybrane)+len(c.detale),
                           'osie':''.join('xyz'[i] for i in osie),'usuniete_niewidoczne':ukryte}
        raport['razem']=sum(v['kostki'] for v in raport.values())
        return wynik,raport

    def zapisz(self, sciezka, nazwa, hitbox, tag_y, animacje=None, swiatla=(), ziarno=7):
        """Zbuduj atlas i rig. hitbox=(szerokość X=Z,wysokość,pivot Y)."""
        if not any(n.startswith('swiatlo_') for n in self.czesci) and not swiatla:
            raise ValueError('model wymaga kości swiatlo_*')
        mesh,raport=self.siatka()
        if raport['razem']>self.budzet:
            raise ValueError(f"budżet {self.budzet}, kostki {raport['razem']}")
        bb.wyczysc()
        children={n:[] for n in self.czesci}
        material_elementu={}
        for n,c in self.czesci.items():
            for i,(lo,hi,mat) in enumerate(mesh[n]):
                en=f'{n}_sdf_{i}'
                children[n].append(bb.szescian(en,lo,hi,2))
                material_elementu[en]=mat
            for en,lo,hi,mat in c.detale:
                children[n].append(bb.szescian(en,lo,hi,2))
                material_elementu[en]=mat
        hb_w,hb_h,hb_pivot=hitbox
        bb.szescian('hitbox_box',(-hb_w/2,0,-hb_w/2),(hb_w/2,hb_h,hb_w/2),1)
        hb=bb.ELEMENTY.pop()
        def node(n):
            c=self.czesci[n]
            return bb.kosc(n,c.pivot,children[n]+[node(q) for q in self.czesci if self.czesci[q].rodzic==n],rot=c.obrot_kosci)
        out=[node(n) for n,c in self.czesci.items() if c.rodzic is None]
        out += [bb.kosc('hitbox',(0,hb_pivot,0),[hb['uuid']]),bb.kosc('tag_name',(0,tag_y,0),[])]
        for n,p in swiatla:
            if n not in bb.KOSCI:
                out.append(bb.kosc(n,p,[]))
        bb.ELEMENTY.append(hb)
        mat=dict(self.materialy)
        mat['__hitbox__']={'ramp':[(0,0,0)]*4}
        material_elementu['hitbox_box']='__hitbox__'
        src,usage=tekstura.atlas(bb.ELEMENTY,mat,lambda n:material_elementu[n],
                                 rozmiar=self.atlas,ziarno=ziarno,malarz=malarz,
                                 wspoldzielone=getattr(self,'wspoldziel_uv',False))
        bb.waliduj(set())
        Path(sciezka).parent.mkdir(parents=True,exist_ok=True)
        bb.zapisz(sciezka,nazwa,out,src,self.atlas,animacje or [])
        raport['atlas_zajetosc']=usage
        return raport


def segment(m, nazwa, rodzic, R_rodzica, A, B, grubosc, material, koniec=None, P=None):
    """Zgodność ze starszym wywołaniem segment(m, ...)."""
    return m.segment(nazwa, rodzic, R_rodzica, A, B, grubosc, material, koniec, P)


def _odwrotny(p,pivot,rot):
    x,y,z=(p[i]-pivot[i] for i in range(3))
    for axis,deg in reversed(list(enumerate(rot))):
        c,s=cos(radians(-deg)),sin(radians(-deg))
        if axis==0: y,z=y*c-z*s,y*s+z*c
        elif axis==1: x,z=x*c+z*s,-x*s+z*c
        else: x,y=x*c-y*s,x*s+y*c
    return tuple(v+pivot[i] for i,v in enumerate((x,y,z)))


def _bbox_obrocony(mid,r,c):
    corners=[_obracaj(tuple(mid[i]+s[i]*r[i] for i in range(3)),c.pivot,c.obrot)
             for s in ((a,b,d) for a in (-1,1) for b in (-1,1) for d in (-1,1))]
    return tuple(min(p[i] for p in corners) for i in range(3)),tuple(max(p[i] for p in corners) for i in range(3))
