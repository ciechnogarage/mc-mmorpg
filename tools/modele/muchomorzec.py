"""Muchomorzec (Loch 001, rola: zaklinacz - kanaluje oslone na sojusznikach, gracz ma go przerwac).
Baza: Purchawiec W1 (wybor wlasciciela 2026-09-26: "poprawil i dokurwil troche purchawiec v1").

Zmiany wzgledem W1:
- nogi nie sa dziecmi tulowia (pochylenie/kolysanie tulowia wbijalo stopy w ziemie),
- kapelusz: rondo uniesione z przodu (twarz widoczna z wysokosci oczu gracza), opadajace boki, jasne brodawki
  muchomora wystajace z kapelusza (1 px ponad powierzchnie), spod ronda widac blaszki,
- oczy w kosci `swiatlo_oczy` (MM rozjasnia brightness), glowa `hi_head` (dzieci obracaja sie z glowa),
- palce 1 px (siatka 1:1), grzybnia z tylu w odcieniu fioletu (nie szara plachta),
- animacje: bez 3.5-sekundowego throw_potion; cast_1 -> cast; nowe: channel (kanal oslony, petla - rece w gore,
  pulsujaca kula zarodnikow `fx_kula` nad dlonmi), hurt, spawn (wyrasta z ziemi).
Uzycie: tools/modele/zbuduj.sh muchomorzec
"""
import math
import sys

import z_pliku as Z
from bb import kosc as B, szescian as S

BAZA = "/opt/docker/mc/data/world1/output/lochy/modele-wlasne/_archiwum/purchawiec_wersje/purchawiec_w1.bbmodel"
POD_ZIEMIA = {'spawn:31'}  # wyrasta spod ziemi calym wzrostem


def przebuduj():
    Z.wczytaj(BAZA)
    # --- nogi: z tulowia do kosci glownej (pivoty w pliku sa absolutne - pozycja w swiecie bez zmian)
    tulow, root = Z.kosc('torso'), Z.kosc('purchawiec')
    for n in ('left_leg', 'right_leg'):
        k = Z.kosc(n)
        tulow['children'].remove(k)
        root['children'].append(k)
    # --- palce na siatce 1 px
    for st, xs in (('left', (-7.5, -6.25, -5)), ('right', (4.5, 5.75, 7))):
        for j, x0 in enumerate(xs):
            Z.zmien(f'{st}_palec{j}', od=(x0, 8.6, -0.5), do=(x0 + 1, 10.6, 0.5))
    # --- siatka 1:1: oczy, usta, blaszki i cien na pelne piksele (oczy wysuniete 1 px - nie ginie pod rondem)
    # oczy i usta rowno z twarza (0.1 px przed nia, bez z-fightingu); wysuniete 1 px dawaly wytrzeszcz (wlasciciel
    # 2026-09-26), oczy 2x1 (zmruzone, skupiony zaklinacz) w dolnym rzedzie - gorny chowa sie pod rondem
    Z.zmien('oko_l', od=(-3, 23.6, -4.6), do=(-1, 24.6, -3.6))
    Z.zmien('oko_p', od=(1, 23.6, -4.6), do=(3, 24.6, -3.6))
    Z.zmien('usta', od=(-1.5, 21.6, -4.6), do=(1.5, 22.6, -3.6))
    Z.zmien('blaszki', od=(-8.5, 26.1, -9.5), do=(8.5, 27.1, 7.5))
    Z.zmien('shadow_box', od=(-8, 0.6, -8), do=(8, 1.6, 8))
    # --- glowa: hi_ + oczy do kosci swiatlo_oczy
    glowa = Z.kosc('h_head')
    glowa['name'] = 'hi_head'
    Z.bb.KOSCI['hi_head'] = Z.bb.KOSCI.pop('h_head')
    for a in Z._BAZA['animacje']:
        for an in a['animators'].values():
            if an.get('name') == 'h_head':
                an['name'] = 'hi_head'
    oczy = [Z.element('oko_l')['uuid'], Z.element('oko_p')['uuid']]
    glowa['children'] = [c for c in glowa['children'] if c not in oczy]
    glowa['children'].append(B('swiatlo_oczy', (0, 24.5, -4.8), oczy))
    # --- kapelusz: rondo uniesione z przodu (obrot calej czapy o -12 st. wokol tylnej krawedzi), opadajace boki
    czapa = ['blaszki', 'kapelusz_rondo', 'kapelusz_2', 'kapelusz_3', 'kapelusz_4', 'kapelusz_szczyt', 'kapelusz_oklapniety',
             'kapelusz_brzeg_tyl']
    uu = [Z.element(n)['uuid'] for n in czapa]
    glowa['children'] = [c for c in glowa['children'] if c not in uu]
    # kopula: stopnie coraz nizsze ku gorze (18/17/16/14/10/6) zamiast rownych schodkow
    for n, (pol, y0, y1, dz) in {'kapelusz_3': (8, 29.6, 31.1, -1), 'kapelusz_4': (7, 31.1, 32.6, -1), 'kapelusz_szczyt': (5, 32.6, 33.6, -1)}.items():
        Z.zmien(n, od=(-pol, y0, -pol + dz), do=(pol, y1, pol + dz))
    czubek = S('kapelusz_czubek', (-3, 33.6, -4), (3, 34.6, 2), 5)
    # brodawki na widocznych stopniach (miedzy warstwa a mniejsza nad nia), 1 px ponad powierzchnie
    brodawki = [S(f'brodawka_{i}', (x, y, z), (x + w, y + 1, z + w), 6) for i, (x, y, z, w) in enumerate((
        (-8.5, 29.6, -3, 1), (7, 29.6, 2, 1), (-2, 29.6, -9.5, 2),                 # stopien kapelusz_2 (y 29.6)
        (-7.5, 31.1, 3, 2), (6, 31.1, -7, 1), (2, 31.1, 5.5, 1),                   # stopien kapelusz_3 (y 31.1)
        (-6, 32.6, -6, 1), (4, 32.6, -2, 2), (-2, 33.6, -5.5, 1), (-1, 34.6, -2, 2)))]
    opad = [S('rondo_opad_l', (-10, 25.1, -7), (-9, 28.1, 5), 5), S('rondo_opad_p', (9, 25.6, -7), (10, 28.1, -6), 5)]
    # +x: przod ronda w gore. Bylo 12 st. - blaszki odrywaly sie ~2.5 px od czola (kapelusz "wisial" nad glowa);
    # 5 st. i czapa nizej o 1 px: blaszki leza na glowie na calej glebokosci, twarz nadal widac spod ronda
    wczapie = set(uu + [czubek] + brodawki + opad)
    for e in Z.bb.ELEMENTY:
        if e['uuid'] in wczapie:
            e['from'][1] -= 1; e['to'][1] -= 1; e['origin'][1] -= 1
    glowa['children'].append(B('kapelusz', (0, 26.1, 7.5), uu + [czubek] + brodawki + opad, rot=(5, 0, 0)))
    Z.usun('kapelusz_oklapniety')  # zastapiony opadajacymi bokami
    # --- kula zarodnikow do kanalu (fx: widoczna tylko w channel)
    tulow['children'].append(B('fx_kula', (0, 18, -12), [S('kula', (-2, 16, -14), (2, 20, -10), 7),
                                                         S('kula_x', (-1, 15, -13), (1, 21, -11), 7)]))  # przed piersia, miedzy dlonmi
    nogi_bez_tulowia()
    stopy_na_ziemi()
    # caly model stal 0.6 px nad ziemia (baza W1) - "unosil sie"; stopy na y=0
    for e in Z.bb.ELEMENTY:
        e['from'][1] -= 0.6; e['to'][1] -= 0.6; e['origin'][1] -= 0.6
    for k in Z.kosci():
        k['origin'][1] -= 0.6


def _mac(e):
    """Euler Blockbencha (kolejnosc ZYX, stopnie) -> macierz 3x3; wejscie w konwencji ME z pliku (x, y odwrocone)."""
    x, y, z = (math.radians(v) for v in (-e[0], -e[1], e[2]))
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return _mn(rz, _mn(ry, rx))


def _mn(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def stopa_plasko(noga, stopa):
    """Obrot stopy (ME), przy ktorym stopa w swiecie = jej wlasna animacja bez obrotu nogi: R = R_nogi^T * R_stopy."""
    n = _mac(noga)
    r = _mn([list(w) for w in zip(*n)], _mac(stopa))
    # macierz -> Euler ZYX (jak THREE.Euler 'ZYX')
    y = math.asin(max(-1, min(1, -r[2][0])))
    x = math.atan2(r[2][1], r[2][2])
    z = math.atan2(r[1][0], r[0][0])
    return [-math.degrees(x), -math.degrees(y), math.degrees(z)]


def _klucze(an, kanal):
    return sorted(((float(k['time']), [float(k['data_points'][0][o]) for o in 'xyz'], k.get('interpolation'))
                   for k in an.get('keyframes', []) if k['channel'] == kanal), key=lambda x: x[0])


def _wartosc(kl, t):
    """Wartosc kanalu w chwili t jak w Blockbenchu: liniowo albo catmull-rom (sasiedzi segmentu, na koncach powtorzeni)."""
    if not kl:
        return [0.0, 0.0, 0.0]
    if t <= kl[0][0]:
        return kl[0][1]
    for i in range(len(kl) - 1):
        (t0, v0, i0), (t1, v1, i1) = kl[i], kl[i + 1]
        if t0 <= t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 0
            if 'catmullrom' not in (i0, i1):
                return [v0[j] + (v1[j] - v0[j]) * f for j in range(3)]
            p0, p3 = kl[max(i - 1, 0)][1], kl[min(i + 2, len(kl) - 1)][1]
            return [0.5 * (2 * v0[j] + (v1[j] - p0[j]) * f + (2 * p0[j] - 5 * v0[j] + 4 * v1[j] - p3[j]) * f * f
                           + (3 * v0[j] - p0[j] - 3 * v1[j] + p3[j]) * f ** 3) for j in range(3)]
    return kl[-1][1]


def _siatka(a, klucze, krok=0.05):
    """Czasy kluczy + gesta siatka: klucze nogi i stopy w tych samych chwilach (catmull-rom rzadkich kluczy dwoch
    kosci wygina sie roznie i stopa przestaje byc plaska miedzy nimi)."""
    dl = float(a['length'])
    return sorted(set(klucze) | {round(i * krok, 4) for i in range(int(dl / krok + 1e-6) + 1)} | {dl})


def nogi_bez_tulowia():
    """W W1 nogi byly dziecmi tulowia: tulow pochyla sie (idle 20 st.), nogi kontruja (-17.5 st.) i w swiecie stoja
    prawie pionowo. Po odpieciu nog od tulowia kontra zostala sama -> nogi krzywo o 17 st., bulwy wbite w ziemie
    (dlatego model wisial 0.6 px). Odziedziczone animacje: obrot nogi += obrot tulowia (ta sama wysokosc pivotu,
    male katy - skladanie przez sume). Probki w czasach kluczy obu kosci, interpolacja liniowa."""
    for a in Z._BAZA['animacje']:
        an = {x.get('name'): x for x in a['animators'].values()}
        tul = _klucze(an.get('torso', {}), 'rotation')
        if not tul:
            continue
        for noga in ('left_leg', 'right_leg'):
            if noga not in an:
                continue
            kl = _klucze(an[noga], 'rotation')
            czasy = _siatka(a, {x[0] for x in tul} | {x[0] for x in kl})
            wzor = next(k for k in an[noga]['keyframes'] if k['channel'] == 'rotation')
            an[noga]['keyframes'] = [k for k in an[noga]['keyframes'] if k['channel'] != 'rotation'] + [
                dict(wzor, uuid=Z.bb.uid(), time=t, data_points=[{o: str(round(v, 4)) for o, v in zip(
                    'xyz', [n + r for n, r in zip(_wartosc(kl, t), _wartosc(tul, t))])}]) for t in czasy]
            # stopa (bulwa) plasko: stopa = wlasna animacja - obrot nogi (zasada z recenzji: stopa = -(udo+golen))
            stopa = noga.replace('leg', 'foot')
            if stopa not in an:
                continue
            kn, ks = _klucze(an[noga], 'rotation'), _klucze(an[stopa], 'rotation')
            czasy = _siatka(a, {x[0] for x in kn} | {x[0] for x in ks})
            wzor = next(k for k in an[stopa]['keyframes'] if k['channel'] == 'rotation')
            an[stopa]['keyframes'] = [k for k in an[stopa]['keyframes'] if k['channel'] != 'rotation'] + [
                dict(wzor, uuid=Z.bb.uid(), time=t, data_points=[{o: str(round(v, 4)) for o, v in zip(
                    'xyz', stopa_plasko(_wartosc(kn, t), _wartosc(ks, t)))}]) for t in czasy]


def stopy_na_ziemi(krok=0.05):
    """Po nogi_bez_tulowia (i przed obnizeniem o 0.6): liczy narozniki bulw w kazdej klatce odziedziczonych animacji
    (noga pod korzeniem: p = Lo + Rn*((Fo-Lo) + Rs*(c-Fo))) i unosi noge kluczami pozycji o tyle, o ile bulwa zeszla
    ponizej spodu z pozy spoczynkowej. Machniecia stopa z W1 wbijaly narozniki w ziemie w walk/cast."""

    def mv(m, v):
        return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]
    for a in Z._BAZA['animacje']:
        an = {x.get('name'): x for x in a['animators'].values()}
        for strona in ('left', 'right'):
            noga, stopa = an.get(strona + '_leg'), an.get(strona + '_foot')
            if not noga or not stopa:
                continue
            lo, fo = Z.kosc(strona + '_leg')['origin'], Z.kosc(strona + '_foot')['origin']
            e = Z.element(strona + '_bulwa')
            dno = e['from'][1]
            rogi = [(x, y, z) for x in (e['from'][0], e['to'][0]) for y in (e['from'][1], e['to'][1])
                    for z in (e['from'][2], e['to'][2])]
            kn, ks, kp = _klucze(noga, 'rotation'), _klucze(stopa, 'rotation'), _klucze(noga, 'position')
            dl = float(a['length'])
            czasy = [round(i * krok, 4) for i in range(int(dl / krok + 1e-6) + 1)]
            if czasy[-1] < dl - 1e-6:
                czasy.append(dl)
            unies = []
            for t in czasy:
                rn, rs = _mac(_wartosc(kn, t)), _mac(_wartosc(ks, t))
                najn = min(lo[1] + mv(rn, [fo[i] - lo[i] + w for i, w in enumerate(mv(rs, [c[i] - fo[i] for i in range(3)]))])[1]
                           for c in rogi)
                unies.append(max(0.0, dno - najn))
            if max(unies) < 0.03:
                continue
            wzor = dict(next(k for k in noga['keyframes'] if k['channel'] == 'rotation'), channel='position')
            noga['keyframes'] = [k for k in noga['keyframes'] if k['channel'] != 'position'] + [
                dict(wzor, uuid=Z.bb.uid(), time=t, data_points=[{o: str(round(v, 4)) for o, v in zip(
                    'xyz', [p + d for p, d in zip(_wartosc(kp, t), (0, u + 0.02 if u else 0, 0))])}]) for t, u in zip(czasy, unies)]


Z0, P1, UKRYTA = (0, 0, 0), (1, 1, 1), (0, 0, 0)
DEATH_X = 80  # przelamanie trzonu do tylu (dobrane bramka ziemi)


def nowe_animacje():
    return {
        'channel': {'petla': 'loop', 'dlugosc': 1.2, 'kosci': {
            # rece uniesione przed soba, kula zarodnikow pulsuje, cialo drzy - czytelne "przerwij mnie"
            # rece do przodu i do srodka (dlonie po bokach kuli)
            'left_arm': {'rotation': [(0, (75, -22, 0)), (0.6, (80, -25, 0)), (1.2, (75, -22, 0))]},
            'right_arm': {'rotation': [(0, (75, 22, 0)), (0.6, (80, 25, 0)), (1.2, (75, 22, 0))]},
            'left_hand': {'rotation': [(0, (15, 0, 0)), (1.2, (15, 0, 0))]},
            'right_hand': {'rotation': [(0, (15, 0, 0)), (1.2, (15, 0, 0))]},
            'torso': {'rotation': [(0, (6, 0, 0)), (0.3, (6, 0, 2)), (0.6, (6, 0, -2)), (0.9, (6, 0, 2)), (1.2, (6, 0, 0))]},
            'hi_head': {'rotation': [(0, (-10, 0, 0)), (1.2, (-10, 0, 0))]},
            'fx_kula': {'scale': [(0, (0.8, 0.8, 0.8)), (0.6, (1.25, 1.25, 1.25)), (1.2, (0.8, 0.8, 0.8))],
                        'rotation': [(0, Z0), (1.2, (0, 180, 0))]}}},
        'hurt': {'petla': 'once', 'dlugosc': 0.35, 'kosci': {
            'torso': {'rotation': [(0, Z0), (0.08, (15, 0, -6)), (0.35, Z0)]},
            'hi_head': {'rotation': [(0, Z0), (0.08, (12, 8, 0)), (0.35, Z0)]},
            'kapelusz': {'rotation': [(0, Z0), (0.1, (-8, 0, 4)), (0.25, (4, 0, -2)), (0.35, Z0)]}}},
        'death': {'petla': 'hold', 'dlugosc': 1.6, 'kosci': {
            # chwieje sie, kapelusz zsuwa sie z glowy, trzon przelamuje sie do tylu - kapelusz lezy obok, nie w ziemi
            'torso': {'rotation': [(0, Z0), (0.3, (-8, 0, 6)), (0.5, (4, 0, -6)), (1.0, (DEATH_X, 0, 8)), (1.1, (DEATH_X - 3, 0, 8)),
                                   (1.2, (DEATH_X, 0, 8)), (1.6, (DEATH_X, 0, 8))]},
            'kapelusz': {'rotation': [(0, Z0), (0.5, Z0), (1.0, (-80, 0, -15)), (1.6, (-80, 0, -15))],
                         'position': [(0, Z0), (0.5, Z0), (1.0, (0, 10, 6)), (1.6, (0, 10, 6))]},
            'left_arm': {'rotation': [(0, Z0), (1.0, (-30, 0, -40)), (1.6, (-30, 0, -40))]},
            'right_arm': {'rotation': [(0, Z0), (1.0, (-30, 0, 40)), (1.6, (-30, 0, 40))]},
            'swiatlo_oczy': {'scale': [(0, P1), (1.1, P1), (1.3, UKRYTA), (1.6, UKRYTA)]}}},
        'spawn': {'petla': 'once', 'dlugosc': 1.2, 'kosci': {
            # wyrasta z ziemi: najpierw kapelusz, potem trzon; rece rozchylaja sie na koniec
            'purchawiec': {'position': [(0, (0, -30, 0)), (0.7, (0, -8, 0)), (1.0, (0, 0.5, 0)), (1.2, Z0)]},
            'kapelusz': {'scale': [(0, (0.6, 0.6, 0.6)), (0.8, (1.1, 1.1, 1.1)), (1.2, P1)]},
            'left_arm': {'rotation': [(0, (0, 0, 10)), (0.9, (0, 0, 10)), (1.2, Z0)]},
            'right_arm': {'rotation': [(0, (0, 0, -10)), (0.9, (0, 0, -10)), (1.2, Z0)]}}},
    }


FIOLET = [(58, 30, 54), (78, 42, 72), (98, 56, 90), (118, 72, 106), (136, 90, 120)]
MATERIALY = {
    'kapelusz': {'ramp': FIOLET, 'wzor': 'brodawki', 'szum': 0.1},
    'brodawka': {'ramp': [(198, 186, 160), (220, 210, 186), (236, 228, 208)], 'szum': 0.05},
    'blaszki': {'ramp': [(120, 86, 104), (150, 112, 130), (176, 140, 156)], 'wzor': 'pedy', 'szum': 0.06, 'gradient': 0},
    'trzon': {'ramp': [(140, 126, 98), (160, 146, 116), (178, 164, 134), (194, 182, 152)], 'wzor': 'wlokna', 'szum': 0.08},
    'trzon_ciemny': {'ramp': [(96, 84, 62), (116, 102, 78), (136, 122, 96)], 'wzor': 'wlokna', 'szum': 0.08},
    'pochwa': {'ramp': [(90, 70, 78), (112, 90, 98), (134, 110, 118)], 'alfa': 'draperia', 'szum': 0.08},
    'grzybnia': {'ramp': [(104, 76, 110), (130, 100, 136), (156, 128, 160)], 'alfa': 'draperia', 'szum': 0.06},
    'huba': {'ramp': [(96, 60, 84), (122, 80, 106), (146, 104, 128)], 'wzor': 'brodawki', 'szum': 0.08},
    'purchawka': {'ramp': [(120, 110, 70), (150, 140, 92), (176, 166, 118)], 'wzor': 'brodawki', 'szum': 0.1},
    'oko': {'ramp': [(170, 220, 40), (214, 250, 90)], 'szum': 0.02, 'gradient': 0},
    'usta': {'ramp': [(28, 16, 22), (40, 24, 32)], 'szum': 0.02, 'gradient': 0},
    'korzen': {'ramp': [(70, 58, 44), (92, 78, 58), (112, 98, 72)], 'szum': 0.08},
    'kula': {'ramp': [(150, 196, 40), (190, 230, 70), (226, 250, 130)], 'szum': 0.08, 'gradient': 0},
    'techniczny': {'ramp': [(40, 40, 40), (60, 60, 60)]},
}


def material(n):
    if n.startswith(('hitbox', 'shadow')):
        return 'techniczny'
    if n.startswith('brodawka'):
        return 'brodawka'
    if n.startswith(('kapelusz', 'rondo_opad')):
        return 'kapelusz'
    if n == 'blaszki':
        return 'blaszki'
    if n.startswith('oko'):
        return 'oko'
    if n == 'usta':
        return 'usta'
    if n.startswith('grzybnia'):
        return 'grzybnia'
    if n == 'pochwa':
        return 'pochwa'
    if 'huba' in n:
        return 'huba'
    if n.startswith('purchawka'):
        return 'purchawka'
    if n.startswith('kula'):
        return 'kula'
    if 'palec' in n:
        return 'korzen'
    if n.endswith(('_trzonek', '_bulwa')):
        return 'trzon_ciemny'
    return 'trzon'


def main():
    przebuduj()
    Z.zapisz_model(sys.argv[1], 'muchomorzec', MATERIALY, material, animacje_nowe=nowe_animacje(),
                   usun_animacje=('throw_potion',), zmien_nazwy_animacji={'cast_1': 'cast'}, ziarno=31,
                   fx_tylko={'fx_kula': ('channel',)})


if __name__ == '__main__':
    main()
