#!/usr/bin/env python3
"""Generator klas rasowych (rasa x archetyp, 16 klas + 32 specjalizacje) — 2026-09-26.

JEDNO źródło prawdy: tabela KLASY poniżej. Wytwarza:
  plugins/MMOCore/classes/<rasa>/<klasa>.yml   (48 plików, szkielet = plik archetypu)
  plugins/MMOCore/attributes/bieglosci_broni.yml
  plugins/MMOCore/gui/subclass-select.yml       (sekcja items)
  plugins/CoreTools/Scripts/klasy-rasowe.yml    (mapowanie rasa x archetyp)
Broń MMOItems łata osobno latacz_broni.py (bierze BRON stąd).

Uruchomienie: KLASY_ROOT=<katalog danych serwera-KOPII> python3 tools/klasy-rasowe/generuj_klasy.py, potem
`mmocore reload`, `ct reload`. NOWE atrybuty => RESTART (MMOItems rejestruje statystyki
required-<atrybut> tylko przy starcie). Rozpiska: docs/klasy-rasowe/rozpiska-16-klas.md.
Bez pyyaml (brak na hoście) — szkielet przerabiany tekstowo, sekcjami najwyższego poziomu.
"""
import json, os, re

# Katalog danych serwera, który generator NADPISUJE (paczka B 25.10: bez domyślnego world1 — generator pisze 50+
# plików naraz, więc uruchamiamy go na kopii/klonie, a world1 dostaje gotowe pliki przez wdrożenie z cofnij.sh).
ROOT = os.environ.get('KLASY_ROOT', '')
if __name__ == '__main__':
    if not ROOT or not os.path.isdir(os.path.join(ROOT, 'plugins', 'MMOCore')):
        raise SystemExit('Ustaw KLASY_ROOT=<katalog danych serwera> (z plugins/MMOCore), np. dane klonu.')
    if os.path.realpath(ROOT) == '/opt/docker/mc/data/world1' and os.environ.get('KLASY_WORLD1') != 'tak':
        raise SystemExit('KLASY_ROOT wskazuje world1: generuj na klonie i wdrażaj kopią z cofnij.sh '
                         '(świadomie: KLASY_WORLD1=tak).')
P = lambda *a: os.path.join(ROOT, 'plugins', *a)

# ---- biegłości broni: grupa -> (nazwa, typy MMOItems) ------------------------------------------
BRON = {
    'miecz':            ('miecze',              ['SWORD', 'LONG_SWORD', 'THRUSTING_SWORD']),
    'sztylet':          ('sztylety i pięściaki', ['DAGGER', 'GAUNTLET']),
    'luk':              ('łuki',                ['BOW']),
    'kostur':           ('różdżki i kostury',   ['WAND', 'STAFF', 'STAVE']),
    'bulawa':           ('buławy',              ['HAMMER']),
    'tarcza':           ('tarcze',              ['SHIELD']),
    'drzewcowa':        ('broń drzewcowa',      ['HALBERD', 'LANCE']),
    'kusza':            ('kusze i muszkiety',   ['CROSSBOW', 'MUSKET']),
    'grimuar':          ('grimuary',            ['TOME']),
    'klinga':           ('elfie klingi',        ['KATANA']),
    'luk_dlugi':        ('łuki długie',         ['GREATBOW']),
    'kostur_dwureczny': ('kostury dwuręczne',   ['GREATSTAFF']),
    'sierp':            ('sierpy',              ['SIERP']),
    'topor':            ('topory',              ['AXE', 'GREATAXE']),
    'wlocznia':         ('włócznie',            ['SPEAR']),
    'totem':            ('totemy',              ['TALISMAN']),
    'mlot_dwureczny':   ('młoty dwuręczne',     ['GREATHAMMER']),
    'miecz_dwureczny':  ('miecze dwuręczne',    ['GREATSWORD']),
    'lancuch':          ('łańcuchy',            ['WHIP']),
    'filakterium':      ('filakteria',          ['FILAKTERIUM']),
    'kosa':             ('kosy',                ['KOSA']),
}
ZBROJA = {'lekka': 'lekka', 'srednia': 'średnia', 'ciezka': 'ciężka'}

ARCH = {  # szkielet, id MMOCore archetypu, zbroja, broń
    # Z5 (paczka B 25.10, docs/klasy-rasowe/analiza-spojnosci-2026-10-03.md:81-84): wojownik każdej rasy od
    # poz. 1 nosi lekką, średnią i ciężką zbroję oraz tarczę — ciężka/tarcza nie są nagrodą dopiero na 10 poz.
    'wojownik': ('warrior.yml',   'WARRIOR', ['lekka', 'srednia', 'ciezka'], ['miecz', 'tarcza']),
    'ninja':    ('rogue.yml',     'ROGUE',   ['lekka'],            ['sztylet', 'luk']),
    'mag':      ('mage/mage.yml', 'MAGE',    [],                   ['kostur']),
    'akolita':  ('paladin.yml',   'PALADIN', ['lekka'],            ['bulawa']),
}

# statystyka-podpis specjalizacji: klucz MMOCore, base, per-level (prototyp: obrona 5/0.3, kryt 5/0.2)
STAT = {
    'obrona':      ('defense', 5, 0.3),
    'kryt':        ('critical-strike-chance', 5, 0.2),
    'pociski':     ('projectile-damage', 5, 0.2),
    'czary':       ('magic-damage', 5, 0.2),
    'odnowienie':  ('cooldown-reduction', 5, 0.1),
    'mana':        ('max-mana', 8, 0.4),
    'regen_many':  ('mana-regeneration', 0.2, 0.02),
    'fizyczne':    ('physical-damage', 5, 0.2),
    'unik':        ('dodge-rating', 5, 0.1),
    'kradziez':    ('lifesteal', 2, 0.05),
    'wampiryzm':   ('spell-vampirism', 2, 0.05),
    'zdrowie':     ('max-health', 4, 0.2),
    'obrona_mag':  ('magic-damage-reduction', 5, 0.1),
}

# Co statystyka-podpis daje, słowami gracza — karta specjalizacji w kreatorze (scena).
STAT_OPIS = {
    'obrona': 'więcej obrony', 'kryt': 'trafienia krytyczne', 'pociski': 'obrażenia pocisków',
    'czary': 'obrażenia czarów', 'odnowienie': 'krótsze odnowienie umiejętności', 'mana': 'więcej many',
    'regen_many': 'szybsza regeneracja many', 'fizyczne': 'obrażenia fizyczne', 'unik': 'uniki',
    'kradziez': 'kradzież życia', 'wampiryzm': 'wampiryzm czarów', 'zdrowie': 'więcej życia',
    'obrona_mag': 'odporność na magię',
}
# Akolita nie ma many: jego pula to Dusze (stała, bez regeneracji — etap 2). Premia 'mana' i 'regen_many'
# specjalizacji akolity = większa pula Dusz, mniejsza niż 8 many maga, bo jedna Dusza to jedno przywołanie
# (pula 10 -> 14, +40%; paczka B 25.10, B9).
DUSZE_PREMIA = ('max-mana', 4, 0)
def opis_statu(arch, stat):
    return 'większa pula Dusz' if arch == 'akolita' and stat in ('mana', 'regen_many') else STAT_OPIS[stat]

# (rasa, archetyp, id, nazwa, [opis x2], +zbroja rasowa, broń rasowa, ikona,
#   [(id, nazwa, kolor, [opis x2], +zbroja, +broń, stat), x2])
KLASY = [
 ('czlowiek', 'wojownik', 'rycerz', 'Miecz Korony', ['Zaprzysiężony obrońca Korony.', 'Stal, halabarda i dane słowo.'], [], ['drzewcowa'], 'IRON_SWORD', [
   ('bastion', 'Nieugięty', '&f', ['Mur, o który rozbija się szarża.', 'Nie cofa się ani o krok.'], [], [], 'obrona'),
   ('czempion', 'Czempion Areny', '&e', ['Mistrz pojedynków i areny.', 'Każdy cios waży więcej.'], [], [], 'fizyczne')]),
 ('czlowiek', 'ninja', 'agent', 'Lis Zaułków', ['Cień miejskich zaułków.', 'Sztylet w rękawie, kusza pod płaszczem.'], [], ['kusza'], 'CROSSBOW', [
   ('widmo', 'Truciciel', '&8', ['Ostrze umoczone w jadzie.', 'Zabija, zanim ofiara zrozumie.'], [], [], 'kryt'),
   ('jastrzab', 'Łowca Nagród', '&6', ['Za każdą głowę jest cena.', 'Bełt trafia, zanim wróg dobiegnie.'], [], [], 'pociski')]),
 ('czlowiek', 'mag', 'mag_akademii', 'Magister Wieży', ['Uczony Wieży Magów.', 'Różdżka w dłoni, grimuar przy pasie.'], [], ['grimuar'], 'BOOK', [
   ('elementalista', 'Władca Żywiołów', '&b', ['Ogień, lód i piorun', 'posłuszne jednemu słowu.'], [], [], 'czary'),
   ('przywolywacz', 'Chronomanta', '&d', ['Mag, który nagina czas.', 'Jego zaklęcia wracają szybciej.'], [], [], 'odnowienie')]),
 ('czlowiek', 'akolita', 'swiety_akolita', 'Nosiciel Relikwii', ['Niesie relikwie świętych', 'i ich tarczę wiary.'], ['srednia'], ['tarcza'], 'SHIELD', [
   ('serafin', 'Żywa Relikwia', '&f', ['Święta moc w ludzkim ciele.', 'Staje w pierwszym szeregu.'], [], [], 'obrona'),
   ('hierofant', 'Chorąży Świętych', '&e', ['Niesie sztandar świętych.', 'Duchy obrońców słuchają jego głosu.'], [], [], 'mana')]),

 ('elf', 'wojownik', 'ostrze_gaju', 'Klinga Gaju', ['Szermierz świętych gajów.', 'Lekki krok, elfia klinga.'], [], ['klinga'], 'IRON_SWORD', [
   ('straznik_cierni', 'Różany Rycerz', '&2', ['Obrońca różanego gaju.', 'Piękny i kolczasty.'], [], [], 'obrona'),
   ('tancerz_ostrzy', 'Tancerz Ostrzy', '&a', ['Taniec klingi. Szybki', 'i nieuchwytny.'], [], [], 'unik')]),
 ('elf', 'ninja', 'straznik_kniei', 'Szept Kniei', ['Nikt go nie słyszy w lesie.', 'Łuk długi i sztylet.'], [], ['luk_dlugi'], 'BOW', [
   ('cien_lisci', 'Tkacz Sideł', '&2', ['Las jest jego pułapką.', 'Każda ścieżka kończy się sidłem.'], [], [], 'kryt'),
   ('sokol', 'Srebrna Cięciwa', '&6', ['Strzała w blasku księżyca.', 'Trafia z koron drzew.'], [], [], 'pociski')]),
 ('elf', 'mag', 'tkacz_arkanow', 'Tkacz Arkanów', ['Przędzie magię jak nić.', 'Kostur z żywego drewna.'], [], ['kostur_dwureczny'], 'BLAZE_ROD', [
   ('tkacz_zywiolow', 'Pieśniarz Burz', '&b', ['Śpiewa, a niebo odpowiada', 'piorunem.'], [], [], 'czary'),
   ('straznik_zywiolow', 'Władca Zwierciadeł', '&3', ['Magia odbija się od niego', 'jak od lustra.'], ['lekka'], [], 'obrona_mag')]),
 ('elf', 'akolita', 'wyrocznia', 'Wyrocznia', ['Kapłanka księżyca.', 'Widzi to, co nadejdzie.'], [], ['sierp'], 'IRON_HOE', [
   ('ostrze_ksiezyca', 'Ostrze Księżyca', '&f', ['Sierp księżyca w walce', 'wręcz.'], ['srednia'], [], 'kryt'),
   ('opiekun_duchow', 'Przewodnik Ogników', '&b', ['Ogniki gaju idą', 'za jego światłem.'], [], [], 'regen_many')]),

 ('ork', 'wojownik', 'wodz_wojny', 'Wódz Wojny', ['Prowadzi szarżę na czele hordy', 'i nie zna odwrotu.'], [], ['topor'], 'IRON_AXE', [
   ('tyran', 'Tyran', '&c', ['Ściana żelaza. Przyjmuje ciosy,', 'żeby drużyna mogła zadawać.'], [], [], 'obrona'),
   ('krwioburca', 'Krwioburca', '&4', ['Szał bitewny. Im więcej krwi,', 'tym mocniej bije.'], [], [], 'kryt')]),
 ('ork', 'ninja', 'tropiciel', 'Kieł Stepu', ['Łowca stepów, szybki jak wiatr.', 'Włócznia i łuk.'], ['srednia'], ['wlocznia'], 'TRIDENT', [
   ('lowca_bestii', 'Zaklinacz Bestii', '&6', ['Bestie stepu słuchają', 'jego gwizdu.'], [], [], 'zdrowie'),
   ('lowca_kosci', 'Kościany Grot', '&7', ['Grot z kości wroga.', 'Rzuca, zanim wróg podejdzie.'], [], [], 'pociski')]),
 ('ork', 'mag', 'szaman', 'Szaman', ['Głos duchów burzy.', 'Kostur i totem.'], ['lekka'], ['totem'], 'TOTEM_OF_UNDYING', [
   ('burzownik', 'Burzownik', '&e', ['Woła pioruny', 'na wrogów.'], [], [], 'czary'),
   ('totemista', 'Głos Bębna', '&2', ['Bęben wojny niesie', 'głos przodków.'], [], [], 'odnowienie')]),
 ('ork', 'akolita', 'kaplan_krwi', 'Kapłan Krwi', ['Składa ofiary', 'bogom wojny.'], [], ['mlot_dwureczny'], 'MACE', [
   ('awatar_wojny', 'Awatar Wojny', '&c', ['Wcielony bóg bitwy.', 'Młot na froncie.'], ['srednia'], [], 'fizyczne'),
   ('szept_przodkow', 'Wołający Przodków', '&5', ['Woła duchy wojowników', 'z pól dawnych bitew.'], [], [], 'mana')]),

 ('nieumarly', 'wojownik', 'rycerz_smierci', 'Rycerz Śmierci', ['Rycerz, którego śmierć', 'nie zatrzymała.'], [], ['miecz_dwureczny'], 'NETHERITE_SWORD', [
   ('straznik_grobu', 'Kustosz Krypt', '&7', ['Strzeże krypt.', 'Nie ustępuje.'], [], [], 'obrona'),
   ('zniwiarz', 'Żniwiarz', '&5', ['Zbiera życie', 'z każdym cięciem.'], [], [], 'kradziez')]),
 ('nieumarly', 'ninja', 'widmo_grobu', 'Upiór', ['Upiór cmentarnych mgieł.', 'Łańcuch i sztylet.'], [], ['lancuch'], 'IRON_CHAIN', [
   ('cien', 'Dusiciel', '&8', ['Łańcuch owija się', 'wokół szyi ofiary.'], [], [], 'kryt'),
   ('strzelec_grobu', 'Łucznik Ciszy', '&7', ['Strzały z mroku,', 'bez jednego dźwięku.'], [], [], 'pociski')]),
 ('nieumarly', 'mag', 'mag_grobu', 'Lisz', ['Mag, który oszukał śmierć.', 'Jego dusza czeka w filakterium.'], [], ['filakterium'], 'ECHO_SHARD', [
   ('duszozerca', 'Duszożerca', '&5', ['Pożera dusze.', 'Czary go leczą.'], [], [], 'wampiryzm'),
   ('nekromanta', 'Zwiastun Zarazy', '&8', ['Tam, gdzie przejdzie,', 'wszystko gnije.'], [], [], 'mana')]),
 ('nieumarly', 'akolita', 'spaczony_akolita', 'Pasterz Ciszy', ['Odstępca Spaczonej Ciszy.', 'Ukradł ciszę śmierci i tka z niej sługi.'], [], ['kosa'], 'NETHERITE_HOE', [
   ('awatar_otchlani', 'Awatar Otchłani', '&5', ['Nosi skradzioną ciszę jak pancerz.', 'Karmi nią ostrze i własne rany.'], ['srednia'], [], 'kradziez'),
   ('pasterz_umarlych', 'Pan Kurhanów', '&8', ['Wykrada głosy z kurhanów.', 'Umarli idą za jego szeptem.'], [], [], 'regen_many')]),
]
RASA_NAZWA = {'czlowiek': 'Człowiek', 'elf': 'Elf', 'ork': 'Ork', 'nieumarly': 'Nieumarły'}

# ---- szkielet: sekcje najwyższego poziomu ----------------------------------------------------
TOP = re.compile(r'^([A-Za-z][\w-]*):')

def sekcje(tekst):
    """Lista (klucz, linie). Komentarze/puste linie tuż PRZED kluczem należą do tego klucza."""
    linie = tekst.split('\n')
    starty = [i for i, l in enumerate(linie) if TOP.match(l)]
    granice = []
    for s in starty:
        k = s
        while k > 0 and (linie[k - 1].startswith('#') or not linie[k - 1].strip()):
            k -= 1
        granice.append(max(k, granice[-1] if granice else 0))
    wynik = [(None, linie[:granice[0]])] if granice[0] > 0 else []
    for n, s in enumerate(starty):
        koniec = granice[n + 1] if n + 1 < len(starty) else len(linie)
        wynik.append((TOP.match(linie[s]).group(1), linie[granice[n]:koniec]))
    return wynik

def atrybuty(linie):
    d, klucz = {}, None
    for l in linie:
        m = re.match(r'^    ([\w-]+):', l)
        if m:
            klucz = m.group(1); d[klucz] = [0.0, 0.0]; continue
        m = re.match(r'^        (base|per-level):\s*([-\d.]+)', l)
        if m and klucz:
            d[klucz][0 if m.group(1) == 'base' else 1] = float(m.group(2))
    return d

def liczba(x):
    x = round(x, 4)
    return str(int(x)) if x == int(x) else repr(x)

# Wartości DOMYŚLNE MMOCore dla klasy bez danego klucza. Klucz w pliku klasy ZASTĘPUJE domyślną
# (nie dodaje się) — zmierzone 2026-09-26 sondą tools/klasy-rasowe/sonda-domyslne.js: Tropiciel z
# 'max-mana: -4' miał -4 many, a nie 16. Dlatego zmiana klucza, którego szkielet nie ma, startuje stąd.
# (base, per-level) z MMOCore/stats.yml. Per-level też się liczy: regeneracja many rośnie domyślnie o 0.03/poz.,
# a generator brał samo base — elfi "bonus" +0.1 dawał płaskie 0.266 i od ~5 poz. był karą (etap 0, 2026-09-29).
DOMYSLNE = {'max-mana': (20, 0), 'mana-regeneration': (0.166, 0.03), 'health-regeneration': (0.1, 0),
            'attack-damage': (1, 0),
            'movement-speed': (0.1, 0)}  # bez tego elf/nieumarly mag mial predkosc 0 (2026-09-27)

def wez(attr, k):
    b, p = DOMYSLNE.get(k, (0, 0))
    return attr.setdefault(k, [float(b), float(p)])

# Etap 0 przebudowy klas (2026-09-29, strona Xy7cJTFt): statystyki archetypu NADPISUJĄ szkielet (wartości demo
# MMOCore: ninja/mag 18 HP na każdym poziomie, wojownik szybszy od ninji, szybkość ataku rosnąca do 9.15).
# Kolejność zamierzona: HP wojownik > akolita > ninja > mag; ruch ninja > wojownik > mag = akolita.
# attack-speed bez przyrostu z poziomem; jak MMOItems składa ją z bronią — pomiar na klonie (etap 0).
ARCH_STAT = {
    'wojownik': {'max-health': (24, 0.6), 'movement-speed': (0.105, 0), 'attack-speed': (4.2, 0),
                 # Szał: stała skala 0-100 (było 20 + 1/poz. — na wysokim poziomie nie do napełnienia),
                 # opada 2.5/s poza walką (off-combat-mana-regen w szkielecie)
                 'max-mana': (100, 0), 'mana-regeneration': (-2.5, 0)},
    'ninja':    {'max-health': (20, 0.4), 'movement-speed': (0.115, 0), 'attack-speed': (4.2, 0),
                 # Skupienie: stała pula 100, regeneracja 8/s także podczas walki.
                 'max-mana': (100, 0), 'mana-regeneration': (8, 0)},
    'mag':      {'max-health': (18, 0.35), 'movement-speed': (0.1, 0),
                 # 40 many i 2/s na poz. 1: rotacja 6+15+10 także po orczym -20% puli; pełny pasek po ~20 s.
                 'max-mana': (40, 0.12), 'mana-regeneration': (2.0, 0.018)},
    'akolita':  {'max-health': (22, 0.5), 'movement-speed': (0.1, 0), 'attack-speed': (4.0, 0),
                 # Dusze (etap 2, 30.09): pula 10, bez regeneracji — zbiera się je ze śmierci w pobliżu
                 'max-mana': (10, 0), 'mana-regeneration': (0, 0)},
}
# Przyrost Szału z ciosu razem ze skalą 0-100: dwa wyzwalacze na cios (weapon- i physical-damage) po 8-12
# = ok. 20/cios, pełny pasek po ~5 ciosach, jak dawniej przy maksimum 20 i 2-3 na wyzwalacz.
SZAL_Z_CIOSU = ('amount=2-3', 'amount=8-12')
SZAL_Z_CIOSU_ORK = ('amount=2-3', 'amount=6-10')  # B11: kara orka w zasobie wojownika (~-20%)
# ...a koszty skilli demo razy tyle samo (przegląd 30.09: pasek x5, przyrost x4, koszty stare = skille ~4x tańsze).
# Nadpisanie w pliku klasy jak u maga (skills.<SKILL>.mana). Do czasu własnych skilli (etap 3).
SZAL_MNOZNIK_KOSZTU = 4
SZAL_MAKS_KOSZT = 90

def koszty_many_mythiclib():
    """Domyślne koszty many skilli z MythicLib/skill/*.yml: {SKILL: (base, per-level)}."""
    out = {}
    katalog = P('MythicLib', 'skill')
    for plik in sorted(os.listdir(katalog)) if os.path.isdir(katalog) else []:
        tekst = open(os.path.join(katalog, plik), encoding='utf-8').read()
        for m in re.finditer(r'^([A-Z][A-Z_]+):\n((?:[ \t].*\n|\n)*)', tekst, re.M):
            k = re.search(r'\n    mana:\n      player:\n        base: ([-\d.]+)\n        per-level: ([-\d.]+)', m.group(2))
            if k:
                out[m.group(1)] = (float(k.group(1)), float(k.group(2)))
    return out

def drozsze_skille(szkielet, koszty):
    """Dopisuje koszt Szału wyłącznie umiejętnościom demo ze szkieletu."""
    linie, wynik, w_skills, skill, blok = szkielet.split('\n'), [], False, None, []
    def zamknij():
        wlasne = {s for lista in EXTRA_SKILLE.values() for s, _, _ in lista}
        if skill and skill not in wlasne and skill in koszty and not any(re.match(r'^        mana:', l) for l in blok):
            b, p = koszty[skill]
            blok.extend([f'        # koszt w Szale x{SZAL_MNOZNIK_KOSZTU} (generuj_klasy.py, 2026-09-30)', '        mana:',
                         f'            base: {liczba(b * SZAL_MNOZNIK_KOSZTU)}',
                         f'            per-level: {liczba(p * SZAL_MNOZNIK_KOSZTU)}',
                         # pasek ma stałe 100 — bez limitu skill na wysokim poziomie kosztowałby >100 i był martwy
                         f'            max: {SZAL_MAKS_KOSZT}'])
        wynik.extend(blok)
    for l in linie:
        if re.match(r'^[A-Za-z]', l):
            if w_skills:
                zamknij(); skill, blok = None, []
            w_skills = l.startswith('skills:')
            wynik.append(l); continue
        if w_skills:
            m = re.match(r'^    ([A-Z][A-Z_]+):', l)
            if m:
                zamknij(); skill, blok = m.group(1), [l]; continue
            if skill:
                blok.append(l); continue
        wynik.append(l)
    if w_skills:
        zamknij()
    return '\n'.join(wynik)

def dodaj(attr, k, b, p=0.0):
    v = wez(attr, k); v[0] += b; v[1] += p

def razy(attr, k, f):
    v = wez(attr, k); v[0] *= f; v[1] *= f

def rasowe(attr, rasa, arch):
    """Delty rasy na bazie archetypu (rozpiska, 'Rasa')."""
    if rasa == 'czlowiek':
        dodaj(attr, 'cooldown-reduction', 5)
    # B11 (paczka B 25.10, analiza-spojnosci-2026-10-03.md:19): kara orka i premia elfa działają na ZASÓB
    # archetypu, nie na „manę” — wcześniej ork tracił coś tylko jako mag, elf zyskiwał 0.1 przy regeneracji 8/s.
    # Szał wojownika (przyrost z ciosu) liczy main(): SZAL_Z_CIOSU_ORK.
    elif rasa == 'elf':
        razy(attr, 'movement-speed', 1.10); razy(attr, 'max-health', 0.90)
        if arch == 'mag':
            dodaj(attr, 'mana-regeneration', 0.1)       # 2.0 -> 2.1 /s (+5%)
        elif arch == 'ninja':
            razy(attr, 'mana-regeneration', 1.15)       # Skupienie 8 -> 9.2 /s
        elif arch == 'wojownik':
            razy(attr, 'mana-regeneration', 0.80)       # Szał opada 2.5 -> 2.0 /s (dłużej trzyma pasek)
        # akolita: Dusze bez regeneracji — premią elfa jest tylko ruch
    elif rasa == 'ork':
        razy(attr, 'max-health', 1.15); dodaj(attr, 'physical-damage', 10)
        if arch in ('mag', 'akolita'):
            razy(attr, 'max-mana', 0.80)                # mana 40 -> 32; Dusze 10 -> 8
        elif arch == 'ninja':
            razy(attr, 'mana-regeneration', 0.80)       # Skupienie 8 -> 6.4 /s
        # wojownik: Szał z ciosu 8-12 -> 6-10 (main, SZAL_Z_CIOSU_ORK)
    elif rasa == 'nieumarly':
        dodaj(attr, 'spell-vampirism' if arch in ('mag', 'akolita') else 'lifesteal', 3)
        razy(attr, 'movement-speed', 0.95)

def blok_atrybutow(attr, zbroja, bron):
    out = ['attributes:', '    # biegłości (attributes/bieglosci_zbroi.yml, bieglosci_broni.yml)']
    for z in zbroja:
        out += [f'    additional-bieglosc-{z}:', '        base: 1', '        per-level: 0']
    for b in bron:
        out += [f'    additional-bieglosc-{b.replace("_", "-")}:', '        base: 1', '        per-level: 0']
    for k, (b, p) in attr.items():
        out += [f'    {k}:', f'        base: {liczba(b)}', f'        per-level: {liczba(p)}']
    return out + ['']

def opis_zbroi(z): return ', '.join(ZBROJA[x] for x in z) or 'tylko tkanina'
def opis_broni(b): return ', '.join(BRON[x][0] for x in b)

def plik_klasy(szkielet, *, naglowek, nazwa, lore, ikona, attr, zbroja, bron, podklasy):
    wynik = [f'# {l}' for l in naglowek] + ['display:', f"    name: '{nazwa}'", '    lore:'] + \
            [f"    - '{l}'" for l in lore] + [f"    item: '{ikona}'", '']
    for klucz, linie in sekcje(szkielet):
        if klucz in ('display', 'subclasses', None):
            continue
        if klucz == 'attributes':
            i = next(i for i, l in enumerate(linie) if l.startswith('attributes:'))
            wynik += linie[:i] + blok_atrybutow(attr, zbroja, bron)
            continue
        wynik += linie
    tekst = '\n'.join(wynik).rstrip('\n') + '\n'
    if podklasy:
        tekst += '\nsubclasses:\n' + ''.join(f'    {p.upper()}: 10\n' for p in podklasy)
    return tekst


# Sloty zbroi w ekwipunku Bukkit: 39 hełm, 38 pierś, 37 nogi, 36 buty. Wkładamy TYLKO do pustego slotu
# (SetSlots nadpisuje bez pytania — stara postać mogła mieć własną zbroję); wzór Checkitem air z
# Scripts/stale-sloty.yml. Zajęty slot -> przedmiot idzie do plecaka (mi give).
SLOT_ZBROI = {'helm': 39, 'napiersnik': 38, 'nogi': 37, 'buty': 36}

def zbroja_start(zestawy):
    out = []
    for z in sorted(zestawy):
        for sl, nr in SLOT_ZBROI.items():
            pid = f'{z}_{sl.upper()}'
            out += ['', f'klasa_start_zbroja_{z.lower()}_{sl}:', '  Conditions:',
                    f'    - Checkitem{{p=vanilla;slot={nr};id=air}} falseElse klasa_start_zbroja_{z.lower()}_{sl}_plecak',
                    '  Mechanics:', f'    - SetSlots{{slots="{nr}";source="mmoitems";type="ARMOR";id="{pid}"}} @Self',
                    '', f'klasa_start_zbroja_{z.lower()}_{sl}_plecak:', '  Mechanics:',
                    f'    - command{{c="mi give ARMOR {pid} %player_name% 1"}} @Self']
    return out

def profil():
    """Wczytanie profilu (EventsManager/klasy-rasowe.yml, PlayerDataLoadedEvent): postać, która wciąż ma klasę
    archetypu (np. utworzona przed 2026-09-26), dostaje klasę rasową i zestaw. Nie polegamy tu na zdarzeniu
    zmiany klasy: kreator wybiera tę samą klasę, którą postać już ma."""
    return ['', 'klasa_rasowa_profil:', '  Delay: 60', '  Conditions:',
            '    - WorldName{w="creator_limbo"} false',
            '    - Comparator{v1="|%mmocore_class_id%|";v2="|WARRIOR|ROGUE|MAGE|PALADIN|";op="!c"} true',
            '  Mechanics:', '    - script{s=klasa_rasowa_mapuj} @Self']

def kopia(d): return {k: list(v) for k, v in d.items()}

KOLOR_ARCH = {'wojownik': '&c', 'mag': '&d', 'akolita': '&e', 'ninja': '&7'}

def kreator(dane):
    """Kreator postaci (CharacterCreator/config.yml): kafelek archetypu pokazuje klasę RASOWĄ
    wybranej rasy — sekcja classes.<archetyp>.races.<rasa> (plugin od 55d88f5). Bloki races:
    są podmieniane w całości, reszta configu zostaje nietknięta."""
    q = lambda s: "'" + s.replace("'", "''") + "'"
    cc = P('CharacterCreator', 'config.yml')
    linie = open(cc, encoding='utf-8').read().split('\n')
    # Klucze, które pisze TEN generator; każdy inny klucz rasy (np. role/difficulty/start-skill kreatora
    # z gałęzi cc-naprawa-kreator) jest ZACHOWYWANY — przegląd 30.09: wcześniej cały blok był nadpisywany.
    NASZE = {'display', 'description', 'bonus', 'lore', 'armor', 'weapons', 'specializations'}
    obce = {}  # (arch, rasa) -> linie nieznanych kluczy
    # Przejście 1: zbierz obce klucze z CAŁEGO pliku — nowy blok races jest wstawiany przy 'mmocore-id:',
    # które stoi PRZED starym blokiem, więc zbieranie w locie było za późno (test 30.09).
    arch_z, rasa_z, klucz_z, w_races = None, None, None, False
    for l in linie:
        m2 = re.match(r'^  ([a-z_]+):\s*$', l)
        if l.startswith('classes:'):
            arch_z = ''
        elif arch_z is not None and re.match(r'^[a-z]', l):
            arch_z = None
        elif arch_z is not None and m2:
            arch_z, w_races = m2.group(1), False
        if not arch_z:
            continue
        if l.startswith('    races:'):
            w_races, rasa_z, klucz_z = True, None, None
            continue
        if w_races and l.strip() and not l.startswith('      '):
            w_races = False
        if not w_races or not l.strip():
            continue
        m6 = re.match(r'^      ([a-z_]+):\s*$', l)
        m8 = re.match(r'^        ([\w-]+):', l)
        if m6:
            rasa_z, klucz_z = m6.group(1), None
        elif m8:
            klucz_z = m8.group(1)
            if klucz_z not in NASZE:
                obce.setdefault((arch_z, rasa_z), []).append(l)
        elif klucz_z and klucz_z not in NASZE:
            obce.setdefault((arch_z, rasa_z), []).append(l)
    wynik, i, arch = [], 0, None
    while i < len(linie):
        l = linie[i]
        if l.startswith('    races:') and arch is not None:  # stary blok — obce klucze zebrane wyżej, pomijamy
            i += 1
            while i < len(linie) and linie[i].startswith('      '):
                i += 1
            continue
        wynik.append(l)
        m = re.match(r'^  ([a-z_]+):\s*$', l)
        if l.startswith('classes:'):
            arch = ''
        elif arch is not None and re.match(r'^[a-z]', l):
            arch = None
        elif arch is not None and m:
            arch = m.group(1)
        if arch and l.startswith('    mmocore-id:') and arch in dane:
            wynik.append('    races:')
            for rasa, (nazwa, opis, zbroja, bron, specy) in dane[arch].items():
                opis_t = '\n'.join([*opis, '', f'Zbroja: {opis_zbroi(zbroja)}', f'Broń: {opis_broni(bron)}'])
                # obce klucze PO 'display' — taka kolejność jak w karta_kreatora.py, żeby oba narzędzia były stabilne
                wynik += [f'      {rasa}:', f'        display: {q(KOLOR_ARCH[arch] + nazwa)}', *obce.get((arch, rasa), []),
                          f'        description: "{opis_t.replace(chr(10), chr(92) + "n")}"',
                          f'        bonus: {q("Na 10 poz.: " + specy[0][1] + " albo " + specy[1][1])}',
                          # Osobne pola dla sceny kreatora (karta klasy): opis bez list, zbroja, broń
                          # i specjalizacje z tym, co dają. Stary kreator czyta tylko pola wyżej.
                          f'        lore: {q(" ".join(opis))}',
                          f'        armor: {q(opis_zbroi(zbroja))}',
                          f'        weapons: {q(opis_broni(bron))}',
                          '        specializations:']
                for sid, snazwa, skolor, sopis, s_zb, s_br, stat in specy:
                    daje = [opis_statu(arch, stat)] + [ZBROJA[z] + ' zbroja' for z in s_zb] + [BRON[x][0] for x in s_br]
                    wynik += [f'          - name: {q(skolor + snazwa)}',
                              f'            description: {q(" ".join(sopis))}',
                              f'            perks: {q(", ".join(daje))}']
        i += 1
    tekst = '\n'.join(wynik)
    if linie and linie[-1] == '' and not tekst.endswith('\n'):
        tekst += '\n'  # pomijanie starego bloku zjadało końcową pustą linię pliku
    open(cc, 'w', encoding='utf-8').write(tekst)

# Granice zdrowego rozsądku — NIEZALEŻNE od tabel generatora. Eval konfiguracja-48 porównuje serwer
# z tym samym generatorem, więc błąd w generatorze przechodził jako PASS (2026-09-27: elfi i nieumarły
# mag mieli prędkość 0 — gracz stał w miejscu z przybliżonym obrazem).
GRANICE = {'movement-speed': (0.08, 0.14), 'max-health': (10, 100), 'max-mana': (5, 250),
           'attack-speed': (3.5, 5.0)}
POZIOMY = (1, 10, 50, 100)

def wartosc(t, k, poz):
    """Wartość statystyki z pliku klasy na danym poziomie (brak klucza = domyślna MMOCore)."""
    m = re.search(r'^    ' + re.escape(k) + r':\n\s+base: (-?[\d.]+)\n\s+per-level: (-?[\d.]+)', t, re.M)
    b, p = (float(m.group(1)), float(m.group(2))) if m else DOMYSLNE.get(k, (4 if k == 'attack-speed' else 0, 0))
    return b + p * (poz - 1)

def bramka_statystyk(klasy_dir):
    """Granice na poziomach 1/10/50/100 i porządek archetypów w każdej rasie (bazowe klasy).
    Etap 0: dawniej sprawdzała samo base, więc płaski regen many i 18 HP na 100 poz. przechodziły."""
    zle, baz = [], {}
    bazowe = {kid: (rasa, arch) for rasa, arch, kid, *_ in KLASY}
    for rasa in RASA_NAZWA:
        for plik in sorted(os.listdir(os.path.join(klasy_dir, rasa))):
            t = open(os.path.join(klasy_dir, rasa, plik), encoding='utf-8').read()
            for k, (lo, hi) in GRANICE.items():
                for poz in POZIOMY:
                    v = wartosc(t, k, poz)
                    if not lo <= v <= hi:
                        zle.append(f'{rasa}/{plik}: {k}={v:g} na poz. {poz} poza [{lo}, {hi}]')
            # regeneracja many (poza Szałem wojownika) nie gorsza niż domyślna MMOCore na każdym poziomie —
            # łapie płaskie 0.266 elfów sprzed etapu 0 (domyślna rośnie do 3.136 na 100 poz.)
            kid = plik[:-4]
            # Szał wojownika opada, a Dusze akolity celowo się nie regenerują (etap 2)
            if 'classes/warrior.yml' not in t and "name: 'Dusze'" not in t:
                for poz in POZIOMY:
                    dom = DOMYSLNE['mana-regeneration'][0] + DOMYSLNE['mana-regeneration'][1] * (poz - 1)
                    if wartosc(t, 'mana-regeneration', poz) < dom - 1e-9:
                        zle.append(f'{rasa}/{plik}: regeneracja many {wartosc(t, "mana-regeneration", poz):g} '
                                   f'< domyślnej {dom:g} na poz. {poz}')
            if kid in bazowe:
                baz[bazowe[kid]] = t
    for rasa in RASA_NAZWA:
        hp = {a: wartosc(baz[(rasa, a)], 'max-health', 100) for a in ARCH}
        ruch = {a: wartosc(baz[(rasa, a)], 'movement-speed', 1) for a in ARCH}
        if not hp['wojownik'] > hp['akolita'] > hp['ninja'] > hp['mag']:
            zle.append(f'{rasa}: HP na 100 poz. poza porządkiem wojownik>akolita>ninja>mag: {hp}')
        if not ruch['ninja'] == max(ruch.values()):
            zle.append(f'{rasa}: ninja nie jest najszybszy: {ruch}')
    if zle:
        raise SystemExit('BRAMKA STATYSTYK — klasy zapisane, ale NIE WDRAŻAĆ:\n  ' + '\n  '.join(zle))
    print('bramka statystyk: 48 klas w granicach', ', '.join(GRANICE), 'na poz.', POZIOMY, '+ porządek archetypów')

KOSZTY_MANY = {}

# Etap 2 (30.09): skille archetypu spoza szkieletu. Wariant rasy przez parametr MythicLib 'wariant'
# (MythicLib/skill/akolita.yml, treść w tools/klasy-rasowe/akolita/). (SKILL, poziom, {parametr: wartość per rasa}).
WARIANT_RASY = {'nieumarly': 0, 'ork': 1, 'elf': 2, 'czlowiek': 3}
EXTRA_SKILLE = {
    'akolita': [('AKO_WEZWANIE', 1, {'wariant': WARIANT_RASY}), ('AKO_ZNIWO', 3, {}),
                ('AKO_OPIEKA', 5, {'wariant': WARIANT_RASY, 'mana': {'nieumarly': 0, 'ork': 1, 'elf': 1, 'czlowiek': 1}}), ('AKO_ROZKAZ', 7, {})],
    'wojownik': [('WOJ_SZARZA', 1, {'wariant': WARIANT_RASY}),
                 ('WOJ_ROZPLATANIE', 3, {'wariant': WARIANT_RASY,
                                         'cooldown': {'nieumarly': 5, 'ork': 5, 'elf': 4, 'czlowiek': 5}}),
                 ('WOJ_OKRZYK', 5, {'wariant': WARIANT_RASY}),
                 ('WOJ_GNIEW', 7, {'wariant': WARIANT_RASY})],
    'ninja': [('NIN_RZUT', 1, {'wariant': WARIANT_RASY}),
              ('NIN_CIECIE', 3, {'wariant': WARIANT_RASY}),
              ('NIN_KROK', 5, {'wariant': WARIANT_RASY}),
              ('NIN_EGZEKUCJA', 7, {'wariant': WARIANT_RASY})],
    'mag': [('MAG_POCISK', 1, {'wariant': WARIANT_RASY}),
            ('MAG_BARIERA', 3, {'wariant': WARIANT_RASY}),
            ('MAG_WYBUCH', 5, {'wariant': WARIANT_RASY}),
            ('MAG_MRUGNIECIE', 7, {'wariant': WARIANT_RASY,
                                    'cooldown': {'nieumarly': 8, 'ork': 8, 'elf': 6, 'czlowiek': 8}})],
}
SYGNATURY = {
    ('czlowiek', 'wojownik'): ('WOJ_PCHNIECIE_KORONY', 'WOJ_SZARZA'),
    ('czlowiek', 'ninja'): ('NIN_BELT_LISTU', 'NIN_RZUT'),
    ('nieumarly', 'ninja'): ('NIN_PETLA_GROBU', 'NIN_RZUT'),
    ('nieumarly', 'wojownik'): ('WOJ_MARSZ_KRYPTY', 'WOJ_SZARZA'),
    ('nieumarly', 'mag'): ('MAG_SPLOT_DUSZY', 'MAG_POCISK'),
    ('nieumarly', 'akolita'): ('AKO_KOSIARZ_CISZY', 'AKO_WEZWANIE'),
    ('ork', 'wojownik'): ('WOJ_NATARCIE_WODZA', 'WOJ_SZARZA'),
    ('ork', 'ninja'): ('NIN_SLAD_KLA', 'NIN_RZUT'),
    ('ork', 'mag'): ('MAG_OKO_BURZY', 'MAG_POCISK'),
    ('ork', 'akolita'): ('AKO_PRZODEK_MLOTA', 'AKO_WEZWANIE'),
    ('czlowiek', 'akolita'): ('AKO_STRAZ_RELIKWII', 'AKO_WEZWANIE'),
    ('czlowiek', 'mag'): ('MAG_PIECZEC_WIEZY', 'MAG_POCISK'),
    ('elf', 'wojownik'): ('WOJ_PRZELOT_GAJU', 'WOJ_SZARZA'),
    ('elf', 'ninja'): ('NIN_STRZALA_KORONY', 'NIN_RZUT'),
    ('elf', 'mag'): ('MAG_NIC_ARKANOW', 'MAG_POCISK'),
    ('elf', 'akolita'): ('AKO_SIERP_KSIEZYCA', 'AKO_WEZWANIE'),
}
SPEC_SKILLE = {
    'bastion': ('SPEC_BASTION_MUR', 'Mur Przysięgi'), 'czempion': ('SPEC_CZEMPION_WYZWANIE', 'Wyzwanie Areny'),
    'widmo': ('SPEC_WIDMO_JAD', 'Jad w Ranie'), 'jastrzab': ('SPEC_JASTRZAB_LIST', 'List Gończy'),
    'elementalista': ('SPEC_ELEMENTALISTA_KRAG', 'Krąg Żywiołów'), 'przywolywacz': ('SPEC_PRZYWOLYWACZ_ECHO', 'Echo Chwili'),
    'serafin': ('SPEC_SERAFIN_ZASLONA', 'Zasłona Relikwii'), 'hierofant': ('SPEC_HIEROFANT_CHORAGIEW', 'Chorągiew Warty'),
    'straznik_cierni': ('SPEC_STRAZNIK_CIERNI_MUR', 'Korona Cierni'), 'tancerz_ostrzy': ('SPEC_TANCERZ_CIECIE', 'Piruet Klingi'),
    'cien_lisci': ('SPEC_CIEN_LISCI_SIDLA', 'Sidła Korzeni'), 'sokol': ('SPEC_SOKOL_DESZCZ', 'Deszcz Srebra'),
    'tkacz_zywiolow': ('SPEC_TKACZ_ZYWIOLOW_REFREN', 'Refren Pioruna'), 'straznik_zywiolow': ('SPEC_STRAZNIK_ZYWIOLOW_ZWIERCIADLO', 'Zwierciadło Gaju'),
    'ostrze_ksiezyca': ('SPEC_OSTRZE_KSIEZYCA_LUK', 'Łuk Pełni'), 'opiekun_duchow': ('SPEC_OPIEKUN_DUCHOW_SWIT', 'Ścieżka Ogników'),
    'tyran': ('SPEC_TYRAN_ROZKAZ', 'Żelazny Rozkaz'), 'krwioburca': ('SPEC_KRWIOBURCA_SZAL', 'Uderzenie Krwi'),
    'lowca_bestii': ('SPEC_LOWCA_BESTII_WILK', 'Gwizd Stepowego Wilka'), 'lowca_kosci': ('SPEC_LOWCA_KOSCI_SALWA', 'Salwa Kości'),
    'burzownik': ('SPEC_BURZOWNIK_GROM', 'Grom z Totemu'), 'totemista': ('SPEC_TOTEMISTA_RYTM', 'Rytm Przodków'),
    'awatar_wojny': ('SPEC_AWATAR_WOJNY_MLOT', 'Ofiara Młota'), 'szept_przodkow': ('SPEC_SZEPT_PRZODKOW_WEZWANIE', 'Zew Pobojowiska'),
    'straznik_grobu': ('SPEC_STRAZNIK_GROBU_PROG', 'Próg Krypty'), 'zniwiarz': ('SPEC_ZNIWIARZ_CIECIE', 'Cięcie Żniwiarza'),
    'cien': ('SPEC_CIEN_PETLA', 'Zacisk Pętli'), 'strzelec_grobu': ('SPEC_STRZELEC_GROBU_STRZALA', 'Strzała Bez Głosu'),
    'duszozerca': ('SPEC_DUSZOZERCA_WIEZ', 'Głód Filakterium'), 'nekromanta': ('SPEC_NEKROMANTA_ZARAZA', 'Zaraza Kurhanu'),
    'awatar_otchlani': ('SPEC_AWATAR_OTCHLANI_GLOD', 'Głód Otchłani'), 'pasterz_umarlych': ('SPEC_PASTERZ_UMARLYCH_GLOS', 'Głosy Kurhanów'),
}
# Wszystkie archetypy mają już komplet skilli Aktu 1.
PELNE_WLASNE_SKILLE = set(EXTRA_SKILLE)
ZASOB = {
    'akolita': ['mana:', '    char: ☠', "    icon: '&5☠'",
                '    color: {full: DARK_PURPLE, half: LIGHT_PURPLE, empty: GRAY}', "    name: 'Dusze'", ''],
    'wojownik': ['mana:', '    char: ♦', "    icon: '&c♦'",
                 '    color: {full: DARK_RED, half: RED, empty: DARK_GRAY}', "    name: 'Szał'", ''],
    'ninja': ['mana:', '    char: ✦', "    icon: '&f✦'",
              '    color: {full: WHITE, half: GRAY, empty: DARK_GRAY}', "    name: 'Skupienie'", ''],
}

def bez_skilli_demo(szkielet):
    """Pełny własny zestaw zastępuje demo skills i przypięcie tarczy w slocie 5."""
    szkielet = szkielet.replace('name: "&bDefensive Skill"', 'name: "&bSkill specjalizacji"')
    szkielet = szkielet.replace("lore: ['&7Defensive ability slot']", "lore: ['&7Od poziomu 10 wybranej specjalizacji']")
    wynik = []
    for klucz, linie in sekcje(szkielet):
        if klucz == 'skills':
            wynik.append('skills:')
        elif klucz == 'skill-slots':
            wynik.extend(l for l in linie if not re.match(r'^\s*#?hardset:', l))
        else:
            wynik.extend(linie)
    return '\n'.join(wynik)

def dodaj_extra(szkielet, arch, rasa):
    """Wstawia EXTRA_SKILLE archetypu oraz jego nazwę i barwę zasobu."""
    extra = EXTRA_SKILLE.get(arch, [])
    if extra:
        linie = []
        sygnatura = SYGNATURY.get((rasa, arch))
        for s, poz, par in extra:
            if sygnatura and s == sygnatura[1]:
                s = sygnatura[0]
                par = {}
            linie += [f'    {s}:', f'        level: {poz}', '        max-level: 10']
            for k, v in par.items():
                linie += [f'        {k}:', f'            base: {v[rasa]}', '            per-level: 0']
        szkielet = szkielet.replace('\nskills:\n', '\nskills:\n' + '\n'.join(linie) + '\n', 1)
    if arch in ZASOB:
        zasob = '\n'.join(ZASOB[arch])
        if re.search(r'^mana:', szkielet, re.M):
            szkielet = re.sub(r'^mana:\n(?:^[ \t].*\n|^\s*\n)*', zasob + '\n', szkielet, count=1, flags=re.M)
        else:
            szkielet = szkielet.rstrip('\n') + '\n\n' + zasob
    return szkielet

def nazwy_skilli_mythiclib():
    """{SKILL: nazwa wyświetlana} z MythicLib/skill/*.yml — to zwracają placeholdery %mmocore_bound_N%."""
    out = {}
    katalog = P('MythicLib', 'skill')
    for plik in sorted(os.listdir(katalog)) if os.path.isdir(katalog) else []:
        tekst = open(os.path.join(katalog, plik), encoding='utf-8').read()
        # 'name:' szukany w całym bloku skilla (w akolita.yml stoi po 'source:')
        for m in re.finditer(r'^([A-Z][A-Z_]+):\n((?:[ \t].*\n|\n)*)', tekst, re.M):
            n = re.search(r'^  name: (.+)$', m.group(2), re.M)
            if n:
                out[m.group(1)] = n.group(1).strip().strip("'\"")
    return out

def skille_szkieletu(szkielet):
    """[(SKILL, poziom odblokowania)] z sekcji skills: szkieletu."""
    s = dict(sekcje(szkielet)).get('skills', [])
    out, biezacy = [], None
    for l in s:
        m = re.match(r'^    ([A-Z][A-Z_]+):', l)
        if m:
            biezacy = m.group(1); continue
        m = re.match(r'^        level: (\d+)', l)
        if m and biezacy:
            out.append((biezacy, int(m.group(1)))); biezacy = None
    return out

def main():
    KOSZTY_MANY.update(koszty_many_mythiclib())
    klasy_dir = P('MMOCore', 'classes')
    gui_items, mapuj, skrypty = [], [], []
    oczek = {'klasy': {}, 'mapa': {}}
    zestawy_uzyte = set()
    do_kreatora = {}
    for rasa, arch, kid, nazwa, opis, zb_ras, br_ras, ikona, specy in KLASY:
        skel_plik, arch_id, zb_arch, br_arch = ARCH[arch]
        szkielet = open(os.path.join(klasy_dir, skel_plik), encoding='utf-8').read()
        if arch == 'wojownik':
            szkielet = szkielet.replace(*(SZAL_Z_CIOSU_ORK if rasa == 'ork' else SZAL_Z_CIOSU))
        if arch == 'mag':
            # Regeneracja 2/s działa też podczas walki; procentowy bonus resource pozostaje poza walką.
            szkielet = szkielet.replace('    off-combat-health-regen: true',
                                        '    off-combat-health-regen: true\n    off-combat-mana-regen: false', 1)
        if arch == 'ninja':
            szkielet = szkielet.replace(
                '    # Mana and health regen only applies when out of combat\n    off-combat-mana-regen: true',
                '    # Skupienie regeneruje się także podczas walki; zdrowie poza walką.\n'
                '    off-combat-mana-regen: false', 1)
        if arch in PELNE_WLASNE_SKILLE:
            szkielet = bez_skilli_demo(szkielet)
        szkielet = dodaj_extra(szkielet, arch, rasa)
        baza = atrybuty(dict(sekcje(szkielet))['attributes'])
        for k, (b, p) in ARCH_STAT[arch].items():
            baza[k] = [float(b), float(p)]
        rasowe(baza, rasa, arch)
        zbroja = [z for z in ZBROJA if z in zb_arch + zb_ras]
        bron = br_arch + br_ras
        os.makedirs(os.path.join(klasy_dir, rasa), exist_ok=True)
        nag = [f'{nazwa.upper()} — {RASA_NAZWA[rasa]} × {arch.capitalize()} (klasy rasowe, 2026-09-26).',
               'PLIK GENEROWANY: tools/klasy-rasowe/generuj_klasy.py — zmiany wprowadzać w generatorze.',
               f'Szkielet (zasób, umiejętności, drzewka): classes/{skel_plik}.']
        lore = [*opis, '', f'&7Zbroja: &f{opis_zbroi(zbroja)}', f'&7Broń: &f{opis_broni(bron)}',
                f'&7Na 10 poziomie: &f{specy[0][1]} &7albo &f{specy[1][1]}']
        open(os.path.join(klasy_dir, rasa, kid + '.yml'), 'w', encoding='utf-8').write(plik_klasy(
            szkielet, naglowek=nag, nazwa=nazwa, lore=lore, ikona=ikona, attr=kopia(baza),
            zbroja=zbroja, bron=bron, podklasy=[s[0] for s in specy]))
        oczek['mapa'][f'{rasa}/{arch}'] = kid.upper()
        do_kreatora.setdefault(arch, {})[rasa] = (nazwa, opis, zbroja, bron, specy)
        oczek['klasy'][kid.upper()] = {'rasa': rasa, 'arch': arch, 'nazwa': nazwa, 'zbroja': zbroja, 'bron': bron,
                                       'spec': [s[0].upper() for s in specy]}
        for slot, (sid, snazwa, skolor, sopis, s_zb, s_br, stat) in zip((11, 15), specy):
            attr = kopia(baza)
            opis_premii = opis_statu(arch, stat)
            # Dusze się nie regenerują (etap 2) — 'mana'/'regen_many' akolity to większa pula Dusz (B9)
            k, b, p = DUSZE_PREMIA if arch == 'akolita' and stat in ('mana', 'regen_many') else STAT[stat]
            dodaj(attr, k, b, p)
            szb = [z for z in ZBROJA if z in zbroja + s_zb]
            sbr = bron + s_br
            slore = [*sopis, '', f'&7Daje: &f{opis_premii}', f'&7Zbroja: &f{opis_zbroi(szb)}',
                     f'&7Broń: &f{opis_broni(sbr)}']
            nowe = [ZBROJA[z] + ' zbroja' for z in s_zb] + [BRON[x][0] for x in s_br]
            if nowe:
                slore.append('&7Odblokowuje: &a' + ', '.join(nowe))
            spec_skill, spec_nazwa = SPEC_SKILLE[sid]
            slore.append(f'&7Poziom 10: &f{spec_nazwa}')
            spec_szkielet = szkielet
            if arch == 'ninja' and stat == 'pociski':
                # Tylko cztery specjalizacje strzeleckie: Egzekucja 3–8 bl. z niższym mnożnikiem.
                spec_szkielet = szkielet.replace('    NIN_EGZEKUCJA:\n',
                    '    NIN_EGZEKUCJA:\n        dystans:\n            base: 1\n            per-level: 0\n', 1)
            spec_szkielet = spec_szkielet.replace('\nskills:\n',
                f'\nskills:\n    {spec_skill}:\n        level: 10\n        max-level: 10\n', 1)
            open(os.path.join(klasy_dir, rasa, sid + '.yml'), 'w', encoding='utf-8').write(plik_klasy(
                spec_szkielet, naglowek=[f'{snazwa.upper()} — specjalizacja klasy {nazwa} (10 poz.).', *nag[1:]],
                nazwa=snazwa, lore=slore, ikona=ikona, attr=attr, zbroja=szb, bron=sbr, podklasy=[]))
            oczek['klasy'][sid.upper()] = {'rasa': rasa, 'arch': arch, 'nazwa': snazwa, 'zbroja': szb, 'bron': sbr,
                                           'baza': kid.upper(), 'stat': k}
            gui_items += [f'  sub-{sid.replace("_", "-")}:', f'    function: sub-class-{sid}', f'    slots: [ {slot} ]',
                          f"    name: '{skolor}&l{snazwa}'", '    lore:', "      - '{lore}'"]
        skr = f'klasa_rasowa_{rasa}_{arch}'
        mapuj.append(f'    - script{{s={skr}}} @Self')
        skrypty += ['', f'{skr}:', '  Conditions:',
                    f'    - Comparator{{v1="%mmocore_class_id%";v2="{arch_id}";op="=="}} true',
                    f'    - Comparator{{v1="%luckperms_has_permission_race.{rasa}%";v2="yes";op="=="}} true',
                    '  Mechanics:', f'    - command{{c="mmocore class select %player_name% {kid.upper()}"}} @Self',
                    f'    - script{{s=klasa_start_{kid};delay=10}} @Self']
        # zestaw startowy: broń każdej grupy klasy (+ strzały), zbroja najcięższej wagi klasy (bez ciężkiej)
        from tresc_startowa import START_BRON, START_ZBROJA
        waga = next((w for w in ('srednia', 'lekka') if w in zbroja), 'tkanina')
        zestaw = START_ZBROJA[waga]
        mech = [f'    - command{{c="mi give {START_BRON[g][0]} {START_BRON[g][1]} %player_name% 1"}} @Self' for g in bron]
        if any(g in bron for g in ('luk', 'luk_dlugi', 'kusza')):
            mech.append('    - command{c="minecraft:give %player_name% minecraft:arrow 32"} @Self')
        mech += [f'    - script{{s=klasa_start_zbroja_{zestaw.lower()}_{sl}}} @Self' for sl in ('helm', 'napiersnik', 'nogi', 'buty')]
        # Zestaw RAZ na postać (2026-09-27): zmienna CoreTools na profilu MMOProfiles. Bez tego
        # przełączenie postaci wydawało zestaw od nowa — kreator przy wyborze profilu wołał
        # 'mmocore class select <archetyp>', MMOCore nie miał jeszcze wczytanej klasy postaci,
        # więc mapowanie traktowało ją jak nową (zmierzone na klonie: druga różdżka i zbroja).
        skrypty += ['', f'klasa_start_{kid}:', '  Conditions:',
                    '    - Comparator{v1="%coretools_var_player_klasa_start_dany%";v2="0";op="=="} true',
                    '  Mechanics:', '    - Variable{action=set;variable="klasa_start_dany";value=1} @Self', *mech]
        zestawy_uzyte.add(zestaw)

    # ---- atrybuty biegłości broni ----
    att = ['# BIEGŁOŚCI BRONI (2026-09-26) — PLIK GENEROWANY: tools/klasy-rasowe/generuj_klasy.py.',
           '# Jak bieglosci_zbroi.yml: ukryte (brak w attribute-view, max-points 0), wartość WYŁĄCZNIE',
           '# ze statystyki klasy additional-bieglosc-<grupa>. Broń MMOItems ma required-bieglosc-<grupa>: 1',
           '# (tools/klasy-rasowe/latacz_broni.py). Żaden PRZEDMIOT nie daje biegłości. Typy bez grupy',
           '# (LUTE, CATALYST, MAIN/OFF_CATALYST, ORNAMENT) są wolne: to przedmioty-statystyki z ręki.']
    for g, (n, typy) in BRON.items():
        att += [f'bieglosc_{g}:', f"    # typy MMOItems: {', '.join(typy)}", f"    name: 'Biegłość: {n}'",
                '    max-points: 0', '    buff: {}']
    open(P('MMOCore', 'attributes', 'bieglosci_broni.yml'), 'w', encoding='utf-8').write('\n'.join(att) + '\n')

    # ---- okno specjalizacji: podmiana kafelków sub-* (nagłówek i 'back' zostają) ----
    gui = P('MMOCore', 'gui', 'subclass-select.yml')
    t = open(gui, encoding='utf-8').read()
    a = t.index('\nitems:\n')
    pierwszy = t.index('\n  sub-', a)
    back = t.index('\n  back:', a)
    t = t[:pierwszy + 1] + '\n'.join(gui_items) + t[back:]
    t = t.replace('  # Wzór na 32 specjalizacje: każda para w slotach 11 i 15 tego jednego okna.\n',
                  '  # 32 specjalizacje: każda para w slotach 11 i 15 tego jednego okna. Kafelki sub-* są\n'
                  '  # GENEROWANE (tools/klasy-rasowe/generuj_klasy.py).\n')
    open(gui, 'w', encoding='utf-8').write(t)

    # ---- CoreTools: archetyp postaci jako zmienna profilu (2026-09-29, prośba sesji przedmiotów) ----
    # %coretools_var_player_klasa_archetyp% = woj|nin|mag|ako (brak = klasa spoza tabeli, np. HUMAN).
    # Do warunków MythicMobs (łup pod archetyp) — %mmocore_class% ma 48 id. Specjalizacje dziedziczą
    # archetyp bazy. Ustawiana po każdej zmianie klasy i przy wczytaniu postaci (stare postacie też).
    SKROT = {'wojownik': 'woj', 'ninja': 'nin', 'mag': 'mag', 'akolita': 'ako'}
    ids_arch = {a: [ARCH[a][1]] for a in ARCH}
    for kid_, info in oczek['klasy'].items():
        ids_arch[info['arch']].append(kid_)
    # ---- Zaklecia: bramka klasy (paczka B 25.10, B6; decyzja właściciela 21.10: Zaklęcia = mag i akolita,
    # wojownik i ninja mają skille F). Plugin Zaklecia czyta listę id klas MMOCore przy starcie/reloadzie.
    zak = P('Zaklecia', 'klasy.yml')
    if os.path.isdir(os.path.dirname(zak)):
        open(zak, 'w', encoding='utf-8').write('\n'.join(
            ['# PLIK GENEROWANY: tools/klasy-rasowe/generuj_klasy.py (ids_arch) — zmiany w generatorze.',
             '# Klasy MMOCore, które mogą rzucać Zaklęcia (archetyp + klasy rasowe + specjalizacje).',
             '# Wojownik i ninja: brak (decyzja właściciela 21.10). Pusta lista = bramka wyłączona.',
             'dozwolone-klasy:'] + [f'  - {x}' for a in ('mag', 'akolita') for x in ids_arch[a]]) + '\n')
    arch_skrypty = ['', 'klasa_archetyp_ustaw:', '  Mechanics:'] + \
        [f'    - script{{s=klasa_archetyp_{SKROT[a]}}} @Self' for a in ARCH]
    for a in ARCH:
        lista = '|' + '|'.join(ids_arch[a]) + '|'
        arch_skrypty += ['', f'klasa_archetyp_{SKROT[a]}:', '  Conditions:',
                         f'    - Comparator{{v1="|%mmocore_class_id%|";v2="{lista}";op="!c"}} true',
                         '  Mechanics:',
                         f'    - Variable{{action=set;variable="klasa_archetyp";value={SKROT[a]}}} @Self',
                         # tag gracza dla filtrów po stronie vanilli (Dusze ze śmierci: akolita-dusze.yml, etap 2)
                         *[f'    - command{{c="tag %player_name% remove arch_{x}"}} @Self' for x in SKROT.values() if x != SKROT[a]],
                         f'    - command{{c="tag %player_name% add arch_{SKROT[a]}"}} @Self']
    # zdarzenie zmiany klasy przychodzi PRZED zmianą — odczyt po 2 s; wczytanie postaci po mapowaniu (60+20 t.)
    arch_skrypty += ['', 'klasa_archetyp_po_zmianie:', '  Delay: 40', '  Mechanics:',
                     '    - script{s=klasa_archetyp_ustaw} @Self',
                     '', 'klasa_archetyp_profil:', '  Delay: 100', '  Conditions:',
                     '    - WorldName{w="creator_limbo"} false', '  Mechanics:',
                     '    - script{s=klasa_archetyp_ustaw} @Self']
    # ---- CoreTools: automatyczny pasek umiejętności (2026-09-30, decyzja właściciela) ----------------------
    # Nowa postać miała PUSTY pasek ("No Skill Bound") i nic jej o tym nie mówiło (poligon v2). Umiejętność
    # szkieletu trafia do pierwszego PUSTEGO slotu 1-4, gdy poziom postaci już ją odblokował i nie ma jej
    # jeszcze na pasku — nic, co gracz przypisał sam, nie jest ruszane.
    # Placeholdery %mmocore_bound_N% zwracają NAZWĘ skilla (z MythicLib), pusty slot = "No Skill Bound".
    nazwy_skilli = nazwy_skilli_mythiclib()
    # CoreTools dzieli warunek na SPACJACH (zmierzone 30.09: 'There is no condition action named: wound|...'),
    # więc w warunku nie może być "Deep Wound" ani "No Skill Bound". Porównujemy jedno słowo nazwy, które
    # nie występuje w nazwie żadnego innego skilla; pusty slot = zawiera słowo "Bound".
    # jednoznaczność tylko wśród skilli, które mogą stać na pasku (wszystkie szkielety klas)
    wspolne_na_pasku = {s for a in ARCH if a not in PELNE_WLASNE_SKILLE
                for s, _ in skille_szkieletu(open(os.path.join(klasy_dir, ARCH[a][0]), encoding='utf-8').read())} | \
        {s for a in EXTRA_SKILLE for s, _, _ in EXTRA_SKILLE[a]} | \
        {s for s, _ in SYGNATURY.values()}
    spec_na_pasku = {s for s, _ in SPEC_SKILLE.values()}
    na_pasku = wspolne_na_pasku | spec_na_pasku
    def znacznik(skill, zestaw=na_pasku):
        nazwa = nazwy_skilli.get(skill, skill)
        inne = [nazwy_skilli.get(k, k) for k in zestaw if k != skill]
        for slowo in sorted(nazwa.split(), key=len, reverse=True):
            if not any(slowo in n for n in inne) and 'Bound' not in slowo:
                return slowo
        raise SystemExit(f'pasek: brak jednoznacznego słowa dla skilla {skill} ({nazwa})')
    pasek = ['', 'klasa_pasek_uzupelnij:', '  Mechanics:'] + \
        [f'    - script{{s=klasa_pasek_{rasa}_{SKROT[a]}}} @Self'
         for rasa, a, *_ in KLASY]
    uzyte_skille = set()
    for rasa, a, kid, _, _, _, _, _, specy in KLASY:
        skel = open(os.path.join(klasy_dir, ARCH[a][0]), encoding='utf-8').read()
        demo = [] if a in PELNE_WLASNE_SKILLE else skille_szkieletu(skel)
        skille = sorted(demo + [(s, poz) for s, poz, _ in EXTRA_SKILLE.get(a, [])], key=lambda s: s[1])
        sygnatura = SYGNATURY.get((rasa, a))
        if sygnatura:
            skille = [(sygnatura[0] if s == sygnatura[1] else s, poz) for s, poz in skille]
        lista = '|' + '|'.join([kid.upper(), *(s[0].upper() for s in specy)]) + '|'
        pasek += ['', f'klasa_pasek_{rasa}_{SKROT[a]}:', '  Conditions:',
                  f'    - Comparator{{v1="|%mmocore_class_id%|";v2="{lista}";op="!c"}} true', '  Mechanics:']
        # co 10 ticków: warunki CoreTools liczą się asynchronicznie, a bind wykonuje się później — bez odstępu dwa
        # skille widziały ten sam pusty slot i drugi nadpisywał pierwszy (zmierzone na klonie 30.09)
        pasek += [f'    - script{{s=klasa_pasek_{rasa}_{SKROT[a]}_{s.lower()};delay={10 * i}}} @Self'
                  for i, (s, _) in enumerate([x for x in skille if x[0] != 'HUMAN_SHIELD'])]
        # baza i specjalizacje wołane OSOBNO ze skryptu rasy: łańcuch baza -> spec urywał się na warunku klasy
        # bazowej, gdy gracz miał już specjalizację (slot 5 nigdy się nie wiązał — klon 02.10)
        pasek += [f'    - script{{s=klasa_pasek_baza_{kid};delay=45}} @Self']
        pasek += [f'    - script{{s=klasa_pasek_spec_{sid};delay=50}} @Self' for sid, *_ in specy]
        pasek += ['', f'klasa_pasek_baza_{kid}:', '  Conditions:',
                  f'    - Comparator{{v1="%mmocore_class_id%";v2="{kid.upper()}";op="=="}} true',
                  '  Mechanics:', '    - command{c="mmocore admin slot unbind %player_name% 5"} @Self']
        for sid, *_ in specy:
            s, _ = SPEC_SKILLE[sid]
            pasek += ['', f'klasa_pasek_spec_{sid}:', '  Conditions:',
                      f'    - Comparator{{v1="%mmocore_class_id%";v2="{sid.upper()}";op="=="}} true',
                      '    - Comparator{v1="%mmocore_level%";v2="10";op=">="} true',
                      f'    - Comparator{{v1="{znacznik(s, spec_na_pasku)}";v2="%mmocore_bound_5%";op="!c"}} false',
                      '  Mechanics:', f'    - command{{c="mmocore admin slot bind %player_name% 5 {s}"}} @Self']
        for s, poz in skille:
            if s == 'HUMAN_SHIELD':
                continue
            uzyte_skille.add(s)
            pasek += ['', f'klasa_pasek_{rasa}_{SKROT[a]}_{s.lower()}:', '  Conditions:',
                      f'    - Comparator{{v1="%mmocore_level%";v2="{poz}";op=">="}} true',
                      f'    - Comparator{{v1="{znacznik(s, wspolne_na_pasku)}";v2="%mmocore_bound_1%|%mmocore_bound_2%|%mmocore_bound_3%|%mmocore_bound_4%";op="!c"}} false',
                      '  Mechanics:', f'    - script{{s=klasa_pasek_slot1_{s.lower()}}} @Self']
    for s in sorted(uzyte_skille):
        for n in (1, 2, 3, 4):
            pasek += ['', f'klasa_pasek_slot{n}_{s.lower()}:', '  Conditions:',
                      f'    - Comparator{{v1="Bound";v2="%mmocore_bound_{n}%";op="!c"}} ' +
                      (f'falseElse klasa_pasek_slot{n + 1}_{s.lower()}' if n < 4 else 'true'),  # falseElse: slot zajęty -> następny
                      '  Mechanics:', f'    - command{{c="mmocore admin slot bind %player_name% {n} {s}"}} @Self']
    # zmiana klasy: zdarzenie przychodzi PRZED zmianą; poziom: odblokowanie po chwili; wczytanie postaci po mapowaniu
    pasek += ['', 'klasa_pasek_po_zmianie:', '  Delay: 60', '  Mechanics:', '    - script{s=klasa_pasek_uzupelnij} @Self',
              '', 'klasa_pasek_po_poziomie:', '  Delay: 20', '  Mechanics:', '    - script{s=klasa_pasek_uzupelnij} @Self',
              '', 'klasa_pasek_profil:', '  Delay: 120', '  Conditions:', '    - WorldName{w="creator_limbo"} false',
              '  Mechanics:', '    - script{s=klasa_pasek_uzupelnij} @Self']
    arch_skrypty += pasek
    open(P('CoreTools', 'EventsManager', 'klasy-pasek.yml'), 'w', encoding='utf-8').write('\n'.join([
        '# PLIK GENEROWANY: tools/klasy-rasowe/generuj_klasy.py (2026-09-30). Zmiana = RESTART serwera.',
        '# Automatyczny pasek umiejętności (skrypty klasa_pasek_* w Scripts/klasy-rasowe.yml).',
        'klasa_pasek_zmiana:', '  Event: PlayerChangeClassEvent', '  Script: klasa_pasek_po_zmianie',
        '  run_script_if_event_cancelled: false', '',
        'klasa_pasek_poziom:', '  Event: PlayerLevelChangeEvent', '  Script: klasa_pasek_po_poziomie',
        '  run_script_if_event_cancelled: false', '',
        'klasa_pasek_profil:', '  Event: PlayerDataLoadedEvent', '  Script: klasa_pasek_profil',
        '  run_script_if_event_cancelled: false', '']))

    open(P('CoreTools', 'EventsManager', 'klasy-archetyp.yml'), 'w', encoding='utf-8').write('\n'.join([
        '# PLIK GENEROWANY: tools/klasy-rasowe/generuj_klasy.py (2026-09-29). Zmiana = RESTART serwera.',
        '# Archetyp postaci -> zmienna CoreTools klasa_archetyp (skrypty w Scripts/klasy-rasowe.yml).',
        'klasa_archetyp_zmiana:', '  Event: PlayerChangeClassEvent', '  Script: klasa_archetyp_po_zmianie',
        '  run_script_if_event_cancelled: false', '',
        'klasa_archetyp_profil:', '  Event: PlayerDataLoadedEvent', '  Script: klasa_archetyp_profil',
        '  run_script_if_event_cancelled: false', '']))

    # ---- CoreTools: zmienna "zestaw startowy wydany" (na profilu MMOProfiles = per postać) ----
    open(P('CoreTools', 'Variables', 'klasy-rasowe.yml'), 'w', encoding='utf-8').write('\n'.join([
        '# PLIK GENEROWANY: tools/klasy-rasowe/generuj_klasy.py (2026-09-27).',
        '# 1 = postać dostała już zestaw startowy klasy; skrypty klasa_start_* wydają go tylko przy 0.',
        'klasa_start_dany:', '  type: integer', '  default: 0', '  min: 0', '  max: 1', '',
        '# Archetyp postaci (woj|nin|mag|ako) dla warunków łupu — ustawiają go skrypty klasa_archetyp_*.',
        'klasa_archetyp:', '  type: string', '  default: brak', '  allowed_values: [brak, woj, nin, mag, ako]', '']))

    # ---- CoreTools: mapowanie ----
    ct = P('CoreTools', 'Scripts', 'klasy-rasowe.yml')
    t = open(ct, encoding='utf-8').read()
    naglowek = t[:t.index('klasa_rasowa_zmiana:')].replace(
        '# Dopisanie klasy: reguła klasa_rasowa_<rasa>_<archetyp> + wpis w klasa_rasowa_mapuj, a dla\n'
        '# nowego archetypu także warunek {new_class} w klasa_rasowa_zmiana.',
        '# PLIK GENEROWANY: tools/klasy-rasowe/generuj_klasy.py. Operator !c = "czy DRUGA wartość\n'
        '# zawiera PIERWSZĄ" (docs CoreTools); kreski | chronią przed dopasowaniem fragmentu id.')
    tekst = naglowek + '\n'.join([
        'klasa_rasowa_zmiana:', '  Conditions:',
        '    # tylko zmiana NA archetyp (kreator) — wybór specjalizacji i nasze komendy przechodzą',
        '    - Comparator{v1="|{new_class}|";v2="|WARRIOR|ROGUE|MAGE|PALADIN|";op="!c"} true',
        '    # obecna klasa to domyślna albo archetyp -> nowa (lub stara) postać: mapujemy',
        '    - Comparator{v1="|%mmocore_class_id%|";v2="|HUMAN|WARRIOR|ROGUE|MAGE|PALADIN|";op="!c"} trueElse klasa_rasowa_nowa',
        '  # obecna klasa jest rasowa albo specjalizacją -> kreator próbuje ją cofnąć przy wejściu',
        '  Mechanics:', '    - cancelevent{}', '',
        'klasa_rasowa_nowa:', '  Mechanics:', '    - script{s=klasa_rasowa_mapuj;delay=20} @Self', '',
        'klasa_rasowa_mapuj:', '  Mechanics:', *mapuj, *skrypty, *zbroja_start(zestawy_uzyte), *profil(),
        *arch_skrypty]) + '\n'
    open(ct, 'w', encoding='utf-8').write(tekst)
    kreator(do_kreatora)
    oczek['bieglosci'] = list(ZBROJA) + list(BRON)
    json.dump(oczek, open(os.path.join(os.path.dirname(__file__), 'oczekiwane.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=1)
    print(f'klasy: {len(KLASY)} bazowych + {2 * len(KLASY)} specjalizacji; biegłości broni: {len(BRON)}')
    bramka_statystyk(klasy_dir)

if __name__ == '__main__':
    main()
