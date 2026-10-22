"""Animacje Kosiarza. Klatki zapisane w konwencji BB4/ME: rotacja (-x,-y,z), pozycja (-x,y,z) względem rzeczywistego ruchu
(pamięć modelengine-konwencja-animacji). Obrót prawoskrętny, model patrzy w -Z, prawa ręka = +X.
Pomocnicze R(x,y,z) przyjmuje RZECZYWISTY obrót: x>0 = stopa/dłoń do przodu, x<0 = góra do przodu (pochylenie);
y>0 = skręt w lewo (przód ku +X? patrz niżej), z>0 = dół w stronę +X."""
import sys
sys.path.insert(0, '/home/dev/Desktop/Przemek/MC/tools/modele')
from anim_bagna import zastosuj


def R(x=0, y=0, z=0): return (-x, -y, z)
def P(x=0, y=0, z=0): return (-x, y, z)
def K(*pozy): return [(t, v, 'linear') for t, v in pozy]
def C(*pozy): return [(t, v, 'catmullrom') for t, v in pozy]


DANE = {
    # oddech, kosa lekko kołysze się w dłoniach, głowa obraca się powoli (jak Strachy)
    'idle': {'dl': 3.0, 'petla': 'loop', 'kosci': {
        'barki': {'rotation': C((0, R()), (1.5, R(x=-2)), (3.0, R()))},
        'glowa': {'rotation': C((0, R(y=-8)), (1.5, R(y=10, x=-3)), (3.0, R(y=-8)))},
        'tulow': {'rotation': C((0, R(y=3)), (1.5, R(y=-3)), (3.0, R(y=3)))},
        'pedy':  {'rotation': C((0, R(z=4)), (1.5, R(z=-4)), (3.0, R(z=4)))},
    }},
    # ciężki krok w pochyle, kosa nisko
    'walk': {'dl': 1.2, 'petla': 'loop', 'kosci': {
        'noga_l': {'rotation': C((0, R(x=22)), (.6, R(x=-20)), (1.2, R(x=22)))},
        'noga_r': {'rotation': C((0, R(x=-20)), (.6, R(x=22)), (1.2, R(x=-20)))},
        'golen_l': {'rotation': C((0, R()), (.3, R(x=-28)), (.6, R(x=-6)), (.9, R()), (1.2, R()))},
        'golen_r': {'rotation': C((0, R(x=-6)), (.3, R()), (.6, R()), (.9, R(x=-28)), (1.2, R(x=-6)))},
        'korzen': {'position': C((0, P(y=.3)), (.3, P(y=1.0)), (.6, P(y=.3)), (.9, P(y=1.0)), (1.2, P(y=.3)))},
        'tulow': {'rotation': C((0, R(y=6)), (.6, R(y=-6)), (1.2, R(y=6)))},
        'glowa': {'rotation': C((0, R(x=-3)), (.6, R(x=2)), (1.2, R(x=-3)))},
    }},
    # ZAMACH: zapowiedź 1,0 s (kosa w prawo i w tył, skręt) -> cięcie 0,15 s w lewo -> kosa w ziemi 1,5 s -> powrót
    'attack': {'dl': 3.2, 'petla': 'once', 'windup': 1.0, 'hit': 1.15, 'recover': 1.6, 'kosci': {
        'tulow': {'rotation': K((0, R()), (1.0, R(y=-55, x=-6)), (1.15, R(y=48, x=-14)), (2.65, R(y=46, x=-16)), (3.2, R()))},
        'barki': {'rotation': K((0, R()), (1.0, R(y=-12, z=-6)), (1.15, R(y=14, z=4)), (2.65, R(y=12, z=4)), (3.2, R()))},
        'noga_l': {'rotation': K((0, R()), (1.0, R(x=10)), (1.15, R(x=-14)), (2.65, R(x=-14)), (3.2, R()))},
        'noga_r': {'rotation': K((0, R()), (1.0, R(x=-12)), (1.15, R(x=16)), (2.65, R(x=16)), (3.2, R()))},
        'golen_r': {'rotation': K((0, R()), (1.15, R(x=-20)), (2.65, R(x=-20)), (3.2, R()))},
        'korzen': {'position': K((0, P()), (.2, P(y=.6)), (1.0, P(y=.6)), (1.15, P(y=.6)), (2.65, P(y=.6)), (3.2, P()))},
        'glowa': {'rotation': K((0, R()), (1.0, R(y=30)), (1.15, R(y=-20)), (2.65, R(y=-25)), (3.2, R()))},
    }},
    # koszenie łanu (pętla): krótsze, rytmiczne wahnięcia
    'kosi': {'dl': 1.6, 'petla': 'loop', 'kosci': {
        'tulow': {'rotation': C((0, R(y=-28, x=-6)), (.8, R(y=26, x=-10)), (1.6, R(y=-28, x=-6)))},
        'barki': {'rotation': C((0, R(y=-6)), (.8, R(y=6)), (1.6, R(y=-6)))},
        'korzen': {'position': C((0, P(y=.3)), (.8, P(y=.3)), (1.6, P(y=.3)))},
    }},
    'hurt': {'dl': .4, 'petla': 'once', 'kosci': {
        'barki': {'rotation': K((0, R()), (.1, R(x=10)), (.4, R()))},
        'glowa': {'rotation': K((0, R()), (.1, R(x=14, z=6)), (.4, R()))},
    }},
    # śmierć: kolana się uginają, worek opada do przodu na kosę
    'death': {'dl': 1.4, 'petla': 'once', 'kosci': {
        'korzen': {'position': K((0, P()), (.6, P(y=-4)), (1.0, P(y=-9, z=-3)), (1.4, P(y=-11, z=-4)))},
        'noga_l': {'rotation': K((0, R()), (.6, R(x=40)), (1.4, R(x=80)))},
        'noga_r': {'rotation': K((0, R()), (.6, R(x=36)), (1.4, R(x=78)))},
        'golen_l': {'rotation': K((0, R()), (.6, R(x=-70)), (1.4, R(x=-90)))},
        'golen_r': {'rotation': K((0, R()), (.6, R(x=-66)), (1.4, R(x=-90)))},
        'tulow': {'rotation': K((0, R()), (.6, R(x=-20)), (1.4, R(x=-55)))},
        'glowa': {'rotation': K((0, R()), (1.0, R(x=-25)), (1.4, R(x=-40, z=10)))},
    }},
    # wynurzenie z łanu
    'spawn': {'dl': 1.2, 'petla': 'once', 'kosci': {
        'korzen': {'position': K((0, P(y=-30)), (.9, P(y=-2)), (1.2, P()))},
        'tulow': {'rotation': K((0, R(x=-30)), (.9, R(x=-10)), (1.2, R()))},
    }},
}
# Drgnięcie TYLKO trafionej części (plugin Trafienia gra hurt_<czesc>): 0,3 s, nic poza tą częścią się nie rusza
DRGNIECIA = {
    'glowa': {'glowa': R(x=16, z=8)},
    'kapelusz': {'kapelusz': R(x=14, z=-10)},
    'tulow': {'barki': R(x=9)},
    'bark_l': {'ramie_l': R(x=-14, z=-10)},
    'bark_r': {'ramie_r': R(x=-14, z=10)},
    'reka_l': {'przedramie_l': R(x=-20, z=-8)},
    'reka_r': {'przedramie_r': R(x=-20, z=8)},
    'kosisko': {'dlon_r': R(x=-10, y=8)},
    'ostrze': {'dlon_r': R(y=-10)},
    'noga_l': {'noga_l': R(x=-16)},
    'noga_r': {'noga_r': R(x=-16)},
}
for cz, kosci in DRGNIECIA.items():
    DANE['hurt_' + cz] = {'dl': .3, 'petla': 'once', 'kosci': {
        k: {'rotation': K((0, R()), (.06, v), (.3, R()))} for k, v in kosci.items()}}
for st in 'lr':     # noga odrzucona do tyłu wbija czubek łapcia w ziemię — korzeń unosi się o pół piksela
    DANE['hurt_noga_' + st]['kosci']['korzen'] = {'position': K((0, P()), (.06, P(y=.6)), (.3, P()))}
# zmiażdżona ręka (obuch): ramię opada, przedramię wisi bezwładnie — pozycja trzymana do końca życia (hold)
DANE['zwis_r'] = {'dl': .5, 'petla': 'hold', 'kosci': {
    'ramie_r': {'rotation': K((0, R()), (.5, R(x=-10, z=8)))},
    'przedramie_r': {'rotation': K((0, R()), (.3, R(x=-86)), (.5, R(x=-78)))},
}}
DANE['zwis_l'] = {'dl': .5, 'petla': 'hold', 'kosci': {
    'ramie_l': {'rotation': K((0, R()), (.5, R(x=-8, z=-8)))},
    'przedramie_l': {'rotation': K((0, R()), (.3, R(x=-60)), (.5, R(x=-54)))},
}}
# zmiażdżone kolano: noga się załamuje, mob pada na to kolano (goleń płasko do tyłu), druga noga z przodu; po 1,2 s wstaje
for st, dr in (('l', 'r'), ('r', 'l')):
    DANE['klek_' + st] = {'dl': 1.6, 'petla': 'once', 'kosci': {
        'korzen': {'position': K((0, P()), (.05, P(y=1.0)), (.1, P(y=-1.0)), (.2, P(y=-4.2)), (1.2, P(y=-4.2)), (1.4, P(y=-1.5)), (1.5, P(y=1.4)), (1.6, P()))},
        'noga_' + st: {'rotation': K((0, R()), (.2, R(x=6)), (1.2, R(x=6)), (1.4, R(x=6)), (1.6, R()))},
        'golen_' + st: {'rotation': K((0, R()), (.1, R(x=-60)), (.2, R(x=-88)), (1.4, R(x=-88)), (1.6, R()))},
        'noga_' + dr: {'rotation': K((0, R()), (.2, R(x=80)), (1.4, R(x=80)), (1.6, R()))},
        'golen_' + dr: {'rotation': K((0, R()), (.2, R(x=-80)), (1.4, R(x=-80)), (1.6, R()))},
        'tulow': {'rotation': K((0, R()), (.2, R(x=-12)), (1.2, R(x=-10)), (1.6, R()))},
    }}
# zachwianie (pełny pasek): tułów i głowa kiwają się, kroki w miejscu, kosa opada — 1,5 s
DANE['zachwianie'] = {'dl': 1.5, 'petla': 'once', 'kosci': {
    'tulow': {'rotation': C((0, R()), (.25, R(x=10, z=12)), (.6, R(x=-6, z=-14)), (1.0, R(x=8, z=8)), (1.5, R()))},
    'glowa': {'rotation': C((0, R()), (.3, R(z=-16, x=10)), (.7, R(z=14)), (1.1, R(z=-8)), (1.5, R()))},
    'noga_l': {'rotation': C((0, R()), (.4, R(x=14)), (.8, R(x=-6)), (1.5, R()))},
    'noga_r': {'rotation': C((0, R()), (.4, R(x=-10)), (.8, R(x=12)), (1.5, R()))},
    'korzen': {'position': C((0, P()), (.3, P(y=.4, z=1)), (.8, P(y=.4, z=-1)), (1.1, P(y=.4)), (1.5, P()))},
    'dlon_r': {'rotation': C((0, R()), (.4, R(x=-20)), (1.5, R()))},
}}
# odbicie: broń gracza trafiła kosę — kosa odskakuje, ręce cofają się, tułów lekko do tyłu
DANE['odbicie'] = {'dl': .8, 'petla': 'once', 'kosci': {
    'dlon_r': {'rotation': K((0, R()), (.1, R(x=-50, y=35)), (.35, R(x=-40, y=28)), (.8, R()))},
    'przedramie_r': {'rotation': K((0, R()), (.1, R(x=-25, z=10)), (.35, R(x=-20)), (.8, R()))},
    'przedramie_l': {'rotation': K((0, R()), (.1, R(x=-20)), (.35, R(x=-15)), (.8, R()))},
    'tulow': {'rotation': K((0, R()), (.12, R(x=12, y=-10)), (.4, R(x=8)), (.8, R()))},
    'korzen': {'position': K((0, P()), (.15, P(z=2)), (.8, P()))},
}}
# gaszenie: lewa dłoń bije po płonącym boku i barku
DANE['gasi'] = {'dl': 1.0, 'petla': 'once', 'kosci': {
    'ramie_l': {'rotation': K((0, R()), (.15, R(x=30, z=-30)), (.3, R(x=20, z=-10)), (.45, R(x=30, z=-30)), (.6, R(x=20, z=-10)), (.75, R(x=30, z=-30)), (1.0, R()))},
    'przedramie_l': {'rotation': K((0, R()), (.15, R(x=40)), (.3, R(x=70)), (.45, R(x=40)), (.6, R(x=70)), (.75, R(x=40)), (1.0, R()))},
    'tulow': {'rotation': K((0, R()), (.2, R(y=-15, x=-8)), (.8, R(y=-15, x=-8)), (1.0, R()))},
}}
# pęknięty pal w tułowiu: przygarbiony do końca życia
DANE['zgarbiony'] = {'dl': .4, 'petla': 'hold', 'kosci': {
    'barki': {'rotation': K((0, R()), (.25, R(x=-22)), (.4, R(x=-18, z=5)))},
    'glowa': {'rotation': K((0, R()), (.4, R(x=12)))},
}}
DRGNIECIA['pedy'] = {'pedy': R(x=-18, z=10)}
DANE['hurt_pedy'] = {'dl': .3, 'petla': 'once', 'kosci': {'pedy': {'rotation': K((0, R()), (.06, R(x=-18, z=10)), (.3, R()))}}}
# --- ataki zależne od stanu ciała (MM: state{s=...})
# kopniak: staje na lewej, prawa noga wyrzucona do przodu z wyprostu kolana, tułów odchylony do tyłu
DANE['kopniak'] = {'dl': .7, 'petla': 'once', 'kosci': {
    'noga_r': {'rotation': K((0, R()), (.25, R(x=-20)), (.4, R(x=75)), (.7, R()))},
    'golen_r': {'rotation': K((0, R()), (.25, R(x=-60)), (.4, R(x=-5)), (.7, R()))},
    'tulow': {'rotation': K((0, R()), (.4, R(x=14)), (.7, R()))},
    'korzen': {'position': K((0, P()), (.05, P(y=.7)), (.25, P(y=.9)), (.4, P(y=.9)), (.7, P()))},
}}
# taran barkiem: obrót barkiem do celu, pochylenie i wypad całym ciałem
DANE['taran'] = {'dl': .9, 'petla': 'once', 'kosci': {
    'tulow': {'rotation': K((0, R()), (.35, R(y=35, x=-15)), (.5, R(y=30, x=-28)), (.9, R()))},
    'korzen': {'position': K((0, P()), (.35, P(z=2)), (.5, P(z=-7, y=-1)), (.9, P()))},
    'ramie_r': {'rotation': K((0, R()), (.5, R(z=12)), (.9, R()))},
}}
# dźgnięcie kikutem drzewca: prawa ręka cofa się i pcha prosto do przodu
DANE['dzgniecie'] = {'dl': .6, 'petla': 'once', 'kosci': {
    'ramie_r': {'rotation': K((0, R()), (.2, R(x=-25)), (.33, R(x=45)), (.6, R()))},
    'przedramie_r': {'rotation': K((0, R()), (.2, R(x=-30)), (.33, R(x=15)), (.6, R()))},
    'tulow': {'rotation': K((0, R()), (.2, R(y=12)), (.33, R(y=-14, x=-8)), (.6, R()))},
}}
# --- chód zależny od nóg (plugin podmienia domyślny WALK modelu)
# pęknięty kij w nodze: zła noga sztywna, ciągnięta po ziemi, zdrowa robi krótki krok, tułów zapada się na złą stronę
for st, dr, z in (('l', 'r', 1), ('r', 'l', -1)):
    DANE['walk_kolano_' + st] = {'dl': 1.4, 'petla': 'loop', 'kosci': {
        'noga_' + st: {'rotation': C((0, R(x=8)), (.7, R(x=-6)), (1.4, R(x=8)))},
        'golen_' + st: {'rotation': C((0, R(x=-8)), (1.4, R(x=-8)))},
        'noga_' + dr: {'rotation': C((0, R(x=-18)), (.35, R(x=10)), (.7, R(x=22)), (1.4, R(x=-18)))},
        'golen_' + dr: {'rotation': C((0, R()), (.35, R(x=-35)), (.7, R()), (1.4, R()))},
        'korzen': {'position': C((0, P(y=1.4)), (.35, P(y=2.2)), (.7, P(y=.6)), (1.05, P(y=1.0)), (1.4, P(y=1.4)))},
        'tulow': {'rotation': C((0, R(z=z * 6)), (.7, R(z=z * 14)), (1.4, R(z=z * 6)))},
    }}
    # bez nogi: podskoki na zdrowej, kikut w powietrzu, ręce rozłożone dla równowagi
    DANE['walk_bez_' + st] = {'dl': 1.0, 'petla': 'loop', 'kosci': {
        'noga_' + dr: {'rotation': C((0, R(x=6)), (.5, R(x=-10)), (1.0, R(x=6)))},
        'golen_' + dr: {'rotation': C((0, R(x=-20)), (.25, R()), (.5, R(x=-20)), (1.0, R(x=-20)))},
        'korzen': {'position': C((0, P(y=1.0)), (.25, P(y=3.4, z=-1)), (.5, P(y=1.0)), (1.0, P(y=1.0)))},
        'tulow': {'rotation': C((0, R(z=z * 10, x=-6)), (.25, R(z=z * 4, x=-10)), (.5, R(z=z * 10, x=-6)), (1.0, R(z=z * 10, x=-6)))},
        'ramie_' + st: {'rotation': C((0, R(z=z * -25)), (1.0, R(z=z * -25)))},
    }}
# --- śmierć zależna od ostatniego ciosu (plugin podmienia domyślny DEATH); rozsypanie od cięcia robi plugin kawałkami
# obuch: łamie się w pół jak pusty worek
DANE['death_oklapniecie'] = {'dl': 1.4, 'petla': 'once', 'kosci': {
    'korzen': {'position': K((0, P()), (.5, P(y=-5)), (1.0, P(y=-10, z=-2)), (1.4, P(y=-12, z=-3)))},
    'noga_l': {'rotation': K((0, R()), (.5, R(x=50)), (1.4, R(x=85)))},
    'noga_r': {'rotation': K((0, R()), (.5, R(x=45)), (1.4, R(x=82)))},
    'golen_l': {'rotation': K((0, R()), (.5, R(x=-80)), (1.4, R(x=-95)))},
    'golen_r': {'rotation': K((0, R()), (.5, R(x=-75)), (1.4, R(x=-95)))},
    'tulow': {'rotation': K((0, R()), (.4, R(x=-30)), (1.4, R(x=-75)))},
    'glowa': {'rotation': K((0, R()), (.8, R(x=-30)), (1.4, R(x=-45, z=12)))},
    'ramie_l': {'rotation': K((0, R()), (1.4, R(x=-30, z=-20)))},
    'ramie_r': {'rotation': K((0, R()), (1.4, R(x=-30, z=20)))},
}}
# spalony: sztywnieje i przewraca się do tyłu w całości jak zwęglony pal
DANE['death_zweglenie'] = {'dl': 2.0, 'petla': 'once', 'kosci': {
    'korzen': {'rotation': K((0, R()), (.6, R(x=8)), (1.5, R(x=66)), (2.0, R(x=76)))},
    'glowa': {'rotation': K((0, R()), (1.5, R(x=10)), (2.0, R(x=14)))},
    'ramie_l': {'rotation': K((0, R()), (.6, R(x=20, z=-15)), (2.0, R(x=25, z=-15)))},
    'ramie_r': {'rotation': K((0, R()), (.6, R(x=20, z=15)), (2.0, R(x=25, z=15)))},
}}
# kulenie bez nogi: przechył na stronę bez nogi + podskok na zdrowej (stan z pluginu Trafienia)
for st, z in (('l', 1), ('r', -1)):
    DANE['kuleje_' + st] = {'dl': 1.0, 'petla': 'loop', 'kosci': {
        'tulow': {'rotation': C((0, R(z=z * 14, x=-6)), (.5, R(z=z * 19, x=-10)), (1.0, R(z=z * 14, x=-6)))},
        'korzen': {'position': C((0, P(y=.3)), (.5, P(y=.8)), (1.0, P(y=.3)))},
    }}


def dodaj(sciezka):
    zastosuj(sciezka, DANE, sciezka.with_name('czasy.json'))
