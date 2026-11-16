"""Animacje do .bbmodel z generatora. Klatki opisujemy PRAWDZIWYM ruchem (jak widac w Blockbench 5);
`do_me=True` zapisuje je w konwencji BB4, w ktorej ModelEngine 4.0.9 je czyta (rotacja X,Y odwrocone, pozycja X
odwrocona). Blockbench 5.1.6 wczytuje plik z format_version 4.x w TEJ SAMEJ konwencji, wiec podglad pliku gry
w BB5 pokazuje ruch z gry. `do_me=False` tylko do porownan (nie do podgladu - odwraca podwojnie).

Format: {nazwa: dict(petla='loop'|'once'|'hold', dlugosc=s, kosci={kosc: {kanal: [(czas, (x,y,z)), ...]}})}
"""
import json
import uuid

INTERPOLACJE = {'linear', 'step', 'catmullrom'}
FAZY = ('windup', 'hit', 'recover')


def _odczytaj_klatke(klatka):
    """Stary (czas, wektor) albo nowy (czas, wektor, interpolacja)."""
    if len(klatka) == 2:
        t, v = klatka
        metoda = 'catmullrom'
    elif len(klatka) == 3:
        t, v, metoda = klatka
    else:
        raise ValueError(f'niepoprawna klatka: {klatka!r}')
    if metoda not in INTERPOLACJE:
        raise ValueError(f'nieznana interpolacja: {metoda}')
    return t, v, metoda


def tabela_czasow(animacje):
    """Dane zapowiedzi dla MM, w sekundach; znaczniki są opcjonalne.

    Przykład: {'znaczniki': {'windup': 0.0, 'hit': 0.8,
    'recover': 1.1}}. Czasy muszą być uporządkowane.
    """
    wynik = {}
    for nazwa, a in animacje.items():
        dlugosc = a['dlugosc']
        znaczniki = a.get('znaczniki', {})
        obce = set(znaczniki) - set(FAZY)
        if obce:
            raise ValueError(f'{nazwa}: nieznane znaczniki {sorted(obce)}')
        poprzedni = -1
        for faza in FAZY:
            if faza in znaczniki:
                t = znaczniki[faza]
                if not 0 <= t <= dlugosc or t < poprzedni:
                    raise ValueError(f'{nazwa}: znaczniki poza czasem lub w złej kolejności')
                poprzedni = t
        wynik[nazwa] = {'length': dlugosc, 'loop': a['petla'],
                        'markers': {f: znaczniki[f] for f in FAZY if f in znaczniki}}
    return wynik


def eksportuj_tabele_czasow(animacje, sciezka):
    """Zapisuje tabelę synchronizacji animacji i skilli MM jako JSON."""
    tabela = tabela_czasow(animacje)
    with open(sciezka, 'w', encoding='utf-8') as plik:
        json.dump(tabela, plik, ensure_ascii=False, indent=2)
        plik.write('\n')
    return tabela


def _klatka(kanal, t, v, do_me, interpolacja='catmullrom'):
    x, y, z = v
    if do_me and kanal == 'rotation':
        x, y = -x, -y
    if do_me and kanal == 'position':
        x = -x
    return {'channel': kanal, 'data_points': [{'x': str(round(x, 3)), 'y': str(round(y, 3)), 'z': str(round(z, 3))}],
            'uuid': str(uuid.uuid4()), 'time': round(t, 4), 'color': -1, 'interpolation': interpolacja}


def zbuduj(animacje, kosci_uuid, do_me):
    tabela_czasow(animacje)
    out = []
    for nazwa, a in animacje.items():
        poza = [(k, _odczytaj_klatke(kl)[0]) for k, kan in a['kosci'].items()
                for lista in kan.values() for kl in lista
                if _odczytaj_klatke(kl)[0] > a['dlugosc'] + 1e-6 or _odczytaj_klatke(kl)[0] < 0]
        assert not poza, f'animacja {nazwa} ({a["dlugosc"]} s): klatki poza czasem {poza[:5]}'
        animatory = {}
        for kosc, kanaly in a['kosci'].items():
            if kosc not in kosci_uuid:
                raise KeyError(f'animacja {nazwa}: brak kosci {kosc}')
            klatki = [_klatka(k, t, v, do_me, metoda)
                       for k, lista in kanaly.items() for kl in lista
                       for t, v, metoda in [_odczytaj_klatke(kl)]]
            animatory[kosci_uuid[kosc]] = {'name': kosc, 'type': 'bone', 'keyframes': klatki}
        out.append({'uuid': str(uuid.uuid4()), 'name': nazwa, 'loop': a['petla'], 'override': False,
                    'length': a['dlugosc'], 'snapping': 20, 'selected': False, 'anim_time_update': '',
                    'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animatory})
    return out
