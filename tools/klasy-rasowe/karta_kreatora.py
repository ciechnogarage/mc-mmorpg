"""Karta klasy w scenie kreatora postaci (CharacterCreator) — SAMODZIELNY skrypt, uruchamiany PO generatorze:
    cd /opt/docker/mc/data/world1 && python3 tools/klasy-rasowe/karta_kreatora.py
Dopisuje do CharacterCreator/config.yml pola, ktorych generuj_klasy.py nie pisze (i ktore jego kreator()
zachowuje jako "obce klucze"): classes.<arch>.races.<rasa>.{role,difficulty,start-skill,start-skill-level,
start-skill-text} oraz races.<rasa>.traits. generuj_klasy.py NIE jest zmieniany (tabele KLASY i rasowe()
czytamy przez import). Idempotentny: stare wartosci tych pol sa zastepowane.

Naprawa kreatora 2026-09-30: karta pokazuje ROLĘ, TRUDNOŚĆ (1-3), jedną PRAWDZIWĄ umiejętność startową
i prawdziwe cechy rasy. Umiejętność NIE jest wpisana na sztywno: czytamy ją z WYGENEROWANEGO pliku
klasy (sekcja skills:, najniższy poziom odblokowania), więc gdy przebudowa klas podmieni skille,
karta pójdzie za nimi. Nazwa zostaje taka, jaką gracz zobaczy w grze (MythicLib), po polsku jest
jedno zdanie z SKILL_PL — brak zdania = sama nazwa + ostrzeżenie generatora.

Cechy ras muszą zgadzać się z generuj_klasy.rasowe(); sprawdza to sprawdz_cechy() (przy każdym
uruchomieniu generatora), inaczej generator odmawia.
"""
import os, re

ROOT = '/opt/docker/mc/data/world1'  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)

# (rasa, archetyp) -> (rola, trudność 1-3). Rola: Obrońca / Obrażenia / Wsparcie / Kontrola (+ druga).
# Trudność: 1 = wybacza błędy (zbroja, życie), 3 = kruchy albo wymaga pilnowania zasobu/dystansu.
ROLA = {
    ('czlowiek', 'wojownik'): ('Obrońca, walka wręcz', 1),
    ('czlowiek', 'ninja'):    ('Obrażenia z bliska i z dystansu', 2),
    ('czlowiek', 'mag'):      ('Obrażenia magiczne z dystansu', 2),
    ('czlowiek', 'akolita'):  ('Wsparcie, drugi szereg', 1),
    ('elf', 'wojownik'):      ('Obrażenia wręcz, zwinność', 2),
    ('elf', 'ninja'):         ('Obrażenia z dystansu', 2),
    ('elf', 'mag'):           ('Obrażenia magiczne z dystansu', 3),
    ('elf', 'akolita'):       ('Wsparcie, walka wręcz', 2),
    ('ork', 'wojownik'):      ('Obrażenia wręcz, obrońca', 1),
    ('ork', 'ninja'):         ('Obrażenia z dystansu i wręcz', 2),
    ('ork', 'mag'):           ('Obrażenia magiczne, kontrola', 2),
    ('ork', 'akolita'):       ('Wsparcie, walka wręcz', 2),
    ('nieumarly', 'wojownik'): ('Obrażenia wręcz, kradzież życia', 1),
    ('nieumarly', 'ninja'):   ('Kontrola, obrażenia z bliska', 3),
    ('nieumarly', 'mag'):     ('Obrażenia magiczne, wampiryzm', 3),
    ('nieumarly', 'akolita'): ('Wsparcie, kradzież życia', 2),
}

# Cechy rasy słowami gracza — MUSZĄ odpowiadać generuj_klasy.rasowe() (klucze w CECHY_KLUCZE).
CECHY = {
    'czlowiek':  ['+5% krótsze odnowienie umiejętności'],
    # zasięg cech jak w rasowe(): elfia regeneracja omija Dusze akolity, orcze -20% dotyczy tylko many maga
    # (Szał, Skupienie i Dusze mają stałą pulę) — 2026-10-03 karta mówiła "poza wojownikiem"
    'elf':       ['+10% szybkości ruchu', 'szybsza regeneracja zasobu (poza akolitą)', '-10% życia'],
    'ork':       ['+15% życia', '+10% obrażeń fizycznych', '-20% many (tylko mag)'],
    'nieumarly': ['+3% kradzieży życia (mag i akolita: wampiryzm czarów)', '-5% szybkości ruchu'],
}
CECHY_KLUCZE = {
    'czlowiek':  {'cooldown-reduction'},
    'elf':       {'movement-speed', 'mana-regeneration', 'max-health'},
    'ork':       {'max-health', 'physical-damage', 'max-mana'},
    'nieumarly': {'lifesteal', 'spell-vampirism', 'movement-speed'},
}

# Jedno zdanie po polsku na umiejętność (wierne opisowi MythicLib/default_skills.yml).
SKILL_PL = {
    'DEEP_WOUND': 'rana tym głębsza, im mniej życia ma wróg',
    'HUMAN_SHIELD': 'osłania sojusznika i przyjmuje część jego obrażeń',
    'FIRE_STORM': 'sześć ognistych pocisków, które podpalają wrogów',
    'MINOR_HEALINGS': 'natychmiast leczy sojusznika (z Shiftem — ciebie)',
    'GREATER_HEALINGS': 'mocne leczenie sojusznika (z Shiftem — ciebie)',
    'WEAKEN': 'osłabia cel — przez chwilę dostaje więcej obrażeń',
    'SNEAKY_PICKY': 'pierwszy cios w walce jest wzmocniony',
    'FURTIVE_STRIKE': 'cios mocniejszy, gdy w pobliżu nie ma innych wrogów',
    'EMPOWERED_ATTACK': 'następny cios naładowany błyskawicą, rani też sąsiadów',
    'POWER_MARK': 'znak zbiera obrażenia, potem wybucha i ogłusza',
    'FIREBALL': 'kula ognia, która podpala i rozpada się w wybuchające odłamki',
    'COMBO_ATTACK': 'seria szybkich cięć w jeden cel',
    'EVADE': 'chwilowa niewrażliwość na obrażenia',
    'AMBERS': 'czary zostawiają bursztyny, które oddają manę',
    'ICE_SPIKES': 'lodowe kolce ranią i spowalniają',
    'WARP': 'teleport we wskazane miejsce',
    'FIRE_BERSERKER': 'więcej obrażeń, gdy płoniesz',
}


def _nazwy_skilli():
    """id -> (nazwa w grze, zdanie z lore) ze WSZYSTKICH plików MythicLib/skill/*.yml.
    2026-10-03: czytany był tylko default_skills.yml, a skrypt nie był uruchamiany po etapie 3 — karta
    pokazywała Deep Wound/Fire Storm, choć na poz. 1 klasa dostaje sygnaturę rasy."""
    katalog = os.path.join(ROOT, 'plugins', 'MythicLib', 'skill')
    out = {}
    for plik in sorted(os.listdir(katalog)) if os.path.isdir(katalog) else []:
        tekst = open(os.path.join(katalog, plik), encoding='utf-8').read()
        for sid, cialo in re.findall(r'^([A-Z][A-Z_]+):\n((?:[ \t].*\n|\n)*)', tekst, re.M):
            n = re.search(r'^  name: (.+)$', cialo, re.M)
            if not n:
                continue
            zdanie = []  # pierwsze zdanie opisu: linie '&7' do pierwszej kończącej się kropką
            lore = re.search(r'^  lore: \[(.*)\]$', cialo, re.M)
            for l in re.findall(r"'((?:[^']|'')*)'", lore.group(1)) if lore else []:
                if l.startswith('&7'):
                    zdanie.append(l[2:].strip())
                    if l.endswith('.'):
                        break
                elif zdanie:
                    break
            tekst_pl = ' '.join(zdanie)
            if tekst_pl.endswith('.') and not tekst_pl.endswith(' bl.'):  # skrót 'bl.' zostaje
                tekst_pl = tekst_pl[:-1]
            out[sid] = (n.group(1).strip().strip("'\""), tekst_pl[:1].lower() + tekst_pl[1:])
    return out


def umiejetnosc_startowa(plik_klasy):
    """(id, poziom) umiejętności z najniższym poziomem odblokowania; remis = pierwsza w pliku."""
    tekst = open(plik_klasy, encoding='utf-8').read()
    m = re.search(r'^skills:\n((?:[ \t].*\n|\n)*)', tekst, re.M)
    if not m:
        return None
    najlepsza = None
    for sid, cialo in re.findall(r'^    ([A-Z_]+):\n((?:        .*\n)*)', m.group(1), re.M):
        lv = re.search(r'^        level:\s*(\d+)', cialo, re.M)
        poziom = int(lv.group(1)) if lv else 1
        if najlepsza is None or poziom < najlepsza[1]:
            najlepsza = (sid, poziom)
    return najlepsza


MOJE = ('role', 'difficulty', 'start-skill', 'start-skill-level', 'start-skill-text')
q = lambda s: "'" + s.replace("'", "''") + "'"


def pola_karty(rasa, arch, plik_klasy, nazwy):
    rola, trudnosc = ROLA[(rasa, arch)]
    out = [f'        role: {q(rola)}', f'        difficulty: {trudnosc}']
    start = umiejetnosc_startowa(plik_klasy) if os.path.exists(plik_klasy) else None
    if start:
        sid, poziom = start
        nazwa, z_lore = nazwy.get(sid, (sid.replace("_", " ").title(), ''))
        out += [f'        start-skill: {q(nazwa)}',
                f'        start-skill-level: {poziom}']
        if SKILL_PL.get(sid) or z_lore:
            out.append(f'        start-skill-text: {q(SKILL_PL.get(sid) or z_lore)}')
        else:
            print(f'UWAGA: brak zdania SKILL_PL dla {sid} ({rasa}/{arch}) — karta pokaże samą nazwę')
    else:
        print(f'UWAGA: brak umiejętności w {plik_klasy} ({rasa}/{arch})')
    return out


def wstaw(tekst, kid):
    """Pola karty w classes.*.races.* (po linii 'display:' bloku rasy) i traits w races.* (po 'bonus:')."""
    nazwy = _nazwy_skilli()
    linie = tekst.split('\n')
    wynik, sekcja, arch, w_races, rasa = [], None, None, False, None
    i = 0
    while i < len(linie):
        l = linie[i]
        if re.match(r'^[a-z]', l):
            sekcja = l.split(':')[0]; arch = None; w_races = False; rasa = None
        m2 = re.match(r'^  ([a-z_]+):\s*$', l)
        if sekcja == 'classes':
            if m2:
                arch, w_races, rasa = m2.group(1), False, None
            elif l.startswith('    races:'):
                w_races, rasa = True, None
            elif w_races and l.strip() and not l.startswith('      '):
                w_races = False
            m6 = re.match(r'^      ([a-z_]+):\s*$', l) if w_races else None
            if m6:
                rasa = m6.group(1)
            if w_races and rasa and re.match(r'^        (' + '|'.join(MOJE) + r'):', l):
                i += 1
                continue
        if sekcja == 'races':
            if m2:
                rasa = m2.group(1)
            if rasa and l.startswith('    traits:'):
                i += 1
                while i < len(linie) and linie[i].startswith('      - '):
                    i += 1
                continue
        wynik.append(l)
        if sekcja == 'classes' and w_races and rasa and l.startswith('        display:') and (rasa, arch) in kid:
            wynik += pola_karty(rasa, arch, os.path.join(ROOT, 'plugins', 'MMOCore', 'classes', rasa, kid[(rasa, arch)] + '.yml'), nazwy)
        if sekcja == 'races' and rasa in CECHY and l.startswith('    bonus:'):
            wynik.append('    traits:')
            wynik += [f'      - {q(c)}' for c in CECHY[rasa]]
        i += 1
    return '\n'.join(wynik)


def sprawdz_cechy(rasowe):
    """Czy CECHY opisują dokładnie te statystyki, które rusza rasowe() generatora."""
    for rasa, klucze in CECHY_KLUCZE.items():
        ruszone = set()
        for arch in ('mag', 'wojownik', 'ninja', 'akolita'):
            attr = {}
            rasowe(attr, rasa, arch)
            ruszone |= set(attr)
        if ruszone != klucze:
            raise SystemExit(f'karta_kreatora: cechy rasy {rasa} nie zgadzają się z rasowe(): '
                             f'rasowe rusza {sorted(ruszone)}, karta opisuje {sorted(klucze)}')


def main():
    import sys
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import generuj_klasy as g  # tylko tabele i rasowe(); main() generatora sie nie wykonuje
    sprawdz_cechy(g.rasowe)
    kid = {(r, a): k for r, a, k, *_ in g.KLASY}
    brak = [x for x in kid if x not in ROLA]
    if brak:
        raise SystemExit(f'karta_kreatora: brak ROLA dla {brak}')
    cc = os.path.join(ROOT, 'plugins', 'CharacterCreator', 'config.yml')
    tekst = open(cc, encoding='utf-8').read()
    nowy = wstaw(tekst, kid)
    open(cc, 'w', encoding='utf-8').write(nowy)
    print(f'karta_kreatora: {nowy.count(chr(10) + "        role:")} kart klas, '
          f'{nowy.count(chr(10) + "    traits:")} ras z cechami')


if __name__ == '__main__':
    main()
