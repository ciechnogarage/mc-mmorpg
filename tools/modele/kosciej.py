"""Kościej K1: chudy łucznik z trzciny, kośćmi i korzeniami."""
from bagna_sdf import nowy, pasma, puste, zapisz

def buduj():
    m=nowy('kosc','korzen','mul','mech','sluz','cien','turkus','turkus_jasny',atlas=128,budzet=250)
    m.czesc('miednica',(0,15,0),'korzen')
    m.elipsoida('miednica',(0,15,0),(4,2,2),'korzen')
    m.czesc('kregoslup',(0,21,0),'miednica')
    m.kapsula('kregoslup',(0,15,1),(0,28,1),1.3,1.1,'kosc')
    m.czesc('klatka',(0,23,0),'kregoslup')
    # Trzy osobne łuki żeber zostawiają z przodu ciemne, szerokie prześwity.
    for i,y in enumerate((19,22.5,26)):
        width=5.8-i*.55
        for s,side in ((-1,'p'),(1,'l')):
            m.kapsula('klatka',(s*.6,y,1),(s*width,y-.8,-2),.72,.55,'kosc')
    m.detal('klatka','mostek',(-.6,20,-3),(.6,27,-2),'kosc')
    m.czesc('glowa',(0,29,0),'kregoslup')
    m.elipsoida('glowa',(0,29,-2.5),(3.1,3.0,2.8),'kosc')
    m.detal('glowa','szczeka',(-2,26,-5.5),(2,27,-4.5),'kosc')
    m.detal('glowa','nos',(-.5,28,-5.9),(.5,29,-4.9),'cien')
    m.czesc('swiatlo_oczy',(0,29,-4),'glowa')
    m.detal('glowa','oczodol_l',(-2.5,28,-6),(-.5,30,-5),'cien')
    m.detal('glowa','oczodol_p',(.5,28,-6),(2.5,30,-5),'cien')
    m.detal('swiatlo_oczy','oko',(-1,28.5,-6.5),(1,30.5,-5.5),'turkus')
    m.detal('swiatlo_oczy','blik',(-.5,29.5,-7),(.5,30.5,-6),'turkus_jasny')
    # Korzeń przecina czaszkę po skosie.
    m.czesc('korzen_czaszki',(-2,32,0),'glowa')
    m.kapsula('korzen_czaszki',(-2,31,0),(1,28,-3),.9,.6,'korzen')
    m.kapsula('korzen_czaszki',(-2,30,-3),(-1.5,28.5,-5),.7,.55,'korzen')
    pasma(m,'glowa','rzesa_czaszka',[(-3,31,-2,5),(3,31,-2,6)])
    for s,side in ((-1,'p'),(1,'l')):
        m.czesc(f'udo_{side}',(s*2,15,0),'miednica')
        m.kapsula(f'udo_{side}',(s*2,15,0),(s*3,8,0),1.3,.9,'kosc')
        m.czesc(f'kolano_{side}',(s*3,8,0),f'udo_{side}')
        m.elipsoida(f'kolano_{side}',(s*3,8,0),(1.3,1.3,1.2),'kosc')
        m.czesc(f'golen_{side}',(s*3,8,0),f'kolano_{side}')
        m.kapsula(f'golen_{side}',(s*3,8,0),(s*4,2,-1),1.1,.7,'kosc')
        m.czesc(f'stopa_{side}',(s*4,1,-1),f'golen_{side}')
        m.elipsoida(f'stopa_{side}',(s*4,1,-2),(2.1,1.1,2.5),'mul')
        m.detal(f'stopa_{side}',f'korzen_stopy_{side}',(s*4-1,0,-4),(s*4+1,2,-2),'korzen')
        m.czesc(f'ramie_{side}',(s*4,26,0),'klatka')
        m.kapsula(f'ramie_{side}',(s*4,26,0),(s*7,20,-3),1.4,.9,'kosc')
        m.czesc(f'lokiec_{side}',(s*7,20,-3),f'ramie_{side}')
        m.elipsoida(f'lokiec_{side}',(s*7,20,-3),(1.2,1.2,1.2),'kosc')
        m.czesc(f'przedramie_{side}',(s*7,20,-3),f'lokiec_{side}')
        end=(s*11,18,-5) if s>0 else (s*2,20,-7)
        m.kapsula(f'przedramie_{side}',(s*7,20,-3),end,1.1,.8,'kosc')
        m.czesc(f'dlon_{side}',end,f'przedramie_{side}')
        m.elipsoida(f'dlon_{side}',end,(1.3,1.2,1.3),'kosc')
        pasma(m,f'ramie_{side}',f'rzesa_ramie_{side}',[(s*5,26,-3,4),(s*6,24,-4,5)])
    # Duży pionowy łuk z giętej trzciny, na kości dłoni.
    m.czesc('luk',(11,18,-5),'dlon_l')
    points=[(10,5,-4),(16,8,-4),(20,12,-4),(20,20,-4),(17,28,-4),(10,34,-4)]
    for i,(a,b) in enumerate(zip(points,points[1:])):
        m.kapsula('luk',a,b,.95,.95,'korzen')
    m.pudlo('luk',(9.5,5,-4.5),(10.5,34,-3.5),'cien')
    m.detal('luk','owijka_luku',(19,16,-5),(21,20,-3),'mech')
    # Strzała spoczywa na dłoni; świeci jedynie grot, a pocisk w locie ma osobną kość fx.
    m.czesc('strzala',(0,20,-7),'dlon_p')
    m.kapsula('strzala',(-4,21,-7),(17,17,-7),.8,.8,'korzen')
    m.kapsula('strzala',(-2,19,-7),(-2,24,-7),.6,.45,'korzen')  # lotka pod dłonią także w naciągu
    m.czesc('swiatlo_grot',(17,17,-7),'strzala')
    m.kapsula('swiatlo_grot',(16.5,17,-7),(19.5,17,-7),1.2,.4,'turkus')
    m.detal('swiatlo_grot','grot_blik',(18,17,-8),(19,18,-7),'turkus_jasny')
    m.czesc('kolczan',(-4,26,3),'klatka')
    m.pudlo('kolczan',(-7,19,3),(-3,29,6),'korzen')
    m.pudlo('kolczan',(-5,22,-2),(-3,25,4),'korzen')
    for i,x in enumerate((-6,-5,-4)):
        m.detal('kolczan',f'lotka_{i}',(x,28,4),(x+1,31,5),'mech')
    for i,x in enumerate((-6,-4)):
        m.detal('kolczan',f'grot_zapasowy_{i}',(x,31,4),(x+1,32,5),'kosc')
    pasma(m,'klatka','rzesa_zebra',[(x,27,-3,4+i%3) for i,x in enumerate((-4,0,4))]+[(-4,22,-3,6),(-3,24,-2,5)])
    pasma(m,'kolczan','rzesa_kolczan',[(-6,28,6,4),(-4,29,6,5)])
    puste(m,[('p_oczy',(0,29,-5),'swiatlo_oczy'),('p_cieciwa',(10,19,-4),'luk'),('fx_strzala',(18,20,-7),'swiatlo_grot')])
    m.odbij_x()  # Łuk i grot po prawej stronie kadru jak w K1.
    return m

if __name__=='__main__':
    print(zapisz(buduj(),'kosciej',(11.2,32,16),35,'kregoslup',2.0)['razem'])
    from anim_kosciej import main as dodaj_animacje
    dodaj_animacje()
