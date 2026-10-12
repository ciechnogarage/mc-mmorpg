# -*- coding: utf-8 -*-
"""Model danych profesji v2: z tresc.py + katalogu v2 + istniejacych plikow MMOItems buduje
przedmioty, receptury, wezly, lowiska, zwierzyne, buffy, ekwiwalenty, XP, ceny i zlecenia.
Nic nie zapisuje — to robi generuj.py."""
import glob
import json
import math
import os
import re
from collections import OrderedDict, defaultdict

import yaml

import tresc as T

PROF_PL = {'mining': 'Górnictwo', 'woodworking': 'Drwalstwo i stolarstwo', 'herbalism': 'Zielarstwo',
           'farming': 'Farmerstwo', 'fishing': 'Rybactwo', 'hunting': 'Myślistwo', 'smithing': 'Kowalstwo',
           'tailoring': 'Krawiectwo', 'jewelcrafting': 'Jubilerstwo', 'alchemy': 'Alchemia',
           'enchanting': 'Zaklinanie', 'cooking': 'Gotowanie', 'building': 'Budownictwo'}
MISTRZ_PL = {'mining': 'Górnika', 'woodworking': 'Drwala-stolarza', 'herbalism': 'Zielarza', 'farming': 'Farmera',
             'fishing': 'Rybaka', 'hunting': 'Myśliwego', 'smithing': 'Kowala', 'tailoring': 'Krawca',
             'jewelcrafting': 'Jubilera', 'alchemy': 'Alchemika', 'enchanting': 'Zaklinacza', 'cooking': 'Kucharza',
             'building': 'Budowniczego'}
RZEMIOSLO = ['smithing', 'woodworking', 'tailoring', 'jewelcrafting', 'alchemy', 'enchanting', 'cooking', 'building']
ZBIERACKIE = list(T.PROF_ZBIERACKIE)


def ld(f):
    with open(f, encoding='utf-8') as h:
        return yaml.safe_load(h) or {}


class Model:
    def __init__(self, plugins, katalog_v2):
        self.P = plugins
        self.items = OrderedDict()        # id -> dict(typ, nazwa, kind, prof, tier, base, ikona, lore)
        self.rec = []                     # dict(st, id, prof, tier, lv, out, ile, ing[(id, n)], xp)
        self.wezly = OrderedDict()        # nexo id -> dict
        self.lowiska = OrderedDict()      # tier -> dict
        self.zwierzyna = OrderedDict()
        self.buffy = OrderedDict()        # item id -> dict
        self.uzycia = OrderedDict()       # item id -> rodzaj (naprawa, zwoj, obozowisko, przeprawa)
        self.narz = OrderedDict()
        self.surowce = []
        self.rzadkie_tabele = OrderedDict()
        self.istniejace = self._wczytaj_istniejace()
        self.katalog = json.load(open(katalog_v2, encoding='utf-8'))
        self._surowce()
        self._rzadkie()
        self._polprodukty_i_produkty()
        self._receptury()
        self._t1_extra()
        self._sprzet()
        self._wezly()
        self._ekwiwalenty()
        self._xp_ceny()
        self._zlecenia()

    # ------------------------------------------------------------ istniejace przedmioty (T1 = id v1)
    def _wczytaj_istniejace(self):
        out = {}
        for f in glob.glob(self.P + '/MMOItems/item/*.yml'):
            typ = os.path.basename(f)[:-4].upper()
            for k, v in ld(f).items():
                if isinstance(v, dict):
                    out[str(k)] = (typ, v)
        return out

    def _dodaj(self, iid, typ, nazwa, kind, prof, tier, ikona, lore=(), base=None, nadpisz_nazwe=True):
        if iid in self.items:
            raise SystemExit(f'duplikat przedmiotu {iid}')
        stary = self.istniejace.get(iid)
        if iid in IKONY_T1_BRAK:  # v1 bez wlasnej ikony (vanilla) — dorabiamy z ksztaltow
            ksz, kolor = IKONY_T1_BRAK[iid]
            ikona = dict(ksztalt=ksz, kolor=kolor, tier=1, styl='klejnot')
        if stary and nazwa is None:
            nazwa = re.sub(r'&[0-9a-fk-or]', '', str(stary[1].get('base', {}).get('name', iid)))
        self.items[iid] = dict(id=iid, typ=typ, nazwa=nazwa, kind=kind, prof=prof, tier=tier, ikona=ikona,
                               lore=list(lore), base=dict(base or {}), t1=bool(stary),
                               stary_model=(stary[1].get('base', {}) or {}).get('model') if stary else None,
                               stary_material=(stary[1].get('base', {}) or {}).get('material') if stary else None)
        return iid

    # ------------------------------------------------------------ surowce zbierackie
    def surowiec(self, prof, t, slot):
        """slot: g/p/r -> id surowca (T1: id v1)."""
        i = 'gpr'.index(slot)
        if t == 1:
            return T.T1[prof][i]
        klucz = T.KLUCZE[prof][i]
        return f'PROF_T{t}_{klucz}' if klucz else None

    def _surowce(self):
        for prof, (rodzaj, zrodlo, kg, kp, kr) in T.PROF_ZBIERACKIE.items():
            for t in range(1, 11):
                for slot, ks in zip('gpr', (kg, kp, kr)):
                    iid = self.surowiec(prof, t, slot)
                    if not iid:
                        continue
                    nazwa = None if t == 1 else T.NAZWY[prof][t]['gpr'.index(slot)]
                    rola = {'g': 'Surowiec', 'p': 'Surowiec poboczny', 'r': 'Rzadki surowiec'}[slot]
                    self._dodaj(iid, 'MATERIAL', nazwa, 'surowiec', prof, t,
                                dict(ksztalt=ks, kraina=t, styl='material'),
                                lore=[f'&7{rola}: &f{PROF_PL[prof]}', f'&8{T.KRAINY[t][0]} · tier {T.ROMAN[t]}'])
                    self.surowce.append(iid)
        # dodatkowe zrodla T1 (wezly gliny, brzozy, grzybow, przypraw, zboza; lowisko T1; zwierzyna)
        for _, prof, _, g, p in T.WEZLY_T1_DODATKOWE:
            for iid in (g, p):
                if iid not in self.items:
                    self._dodaj(iid, 'MATERIAL', None, 'surowiec', prof, 1, dict(stara=True),
                                lore=[f'&7Surowiec: &f{PROF_PL[prof]}', f'&8{T.KRAINY[1][0]} · tier I'])
                    self.surowce.append(iid)
        for iid, _ in T.LOWISKO_T1_EXTRA:
            self._dodaj(iid, 'MATERIAL', None, 'surowiec', 'fishing', 1, dict(stara=True),
                        lore=['&7Rzadki surowiec: &fRybactwo', '&8Zielona Granica · tier I'])
            self.surowce.append(iid)
        for mob, (t, g, r) in T.ZWIERZYNA.items():
            for iid in (g, r):
                if iid and iid not in self.items:
                    self._dodaj(iid, 'MATERIAL', None, 'surowiec', 'hunting', t, dict(stara=True),
                                lore=['&7Surowiec: &fMyślistwo (oprawianie)', f'&8{T.KRAINY[t][0]} · tier {T.ROMAN[t]}'])
                    self.surowce.append(iid)

    # ------------------------------------------------------------ rzadkie z tresci
    def rzadki(self, t, rodzaj):
        return f'PROF_A{t}_{rodzaj.upper()}'

    def _rzadkie(self):
        ks = {'pyl': 'enchant_dust', 'esencja': 'water_essence', 'serce': 'raw_gem'}
        skad = {'pyl': 'wrogowie aktu', 'esencja': 'elity aktu', 'serce': 'bossowie aktu'}
        for t, nazwy in T.RZADKIE.items():
            for rodzaj, nazwa in zip(('pyl', 'esencja', 'serce'), nazwy):
                iid = self._dodaj(self.rzadki(t, rodzaj), 'MATERIAL', nazwa, 'rzadki', None, t,
                                  dict(ksztalt=ks[rodzaj], kraina=t, styl='rzadki'),
                                  lore=[f'&dRzadki składnik krainy', f'&7Źródło: &f{skad[rodzaj]}',
                                        f'&8{T.KRAINY[t][0]} · tier {T.ROMAN[t]}'])
            tab = {}
            for rodzaj in ('pyl', 'esencja', 'serce'):
                tab[rodzaj] = (self.rzadki(t, rodzaj), T.SZANSE_RZADKICH[rodzaj])
            self.rzadkie_tabele[t] = tab

    # ------------------------------------------------------------ polprodukty i produkty
    def polprodukt(self, klucz, t):
        prof, nazwa, skl, ks, t1 = T.PRZETWORZONE[klucz]
        if t == 1:
            if t1:
                return t1
            return {'OKUCIE': T.T1_ID_OKUCIE, 'PLACHTA': T.T1_ID_PLACHTA}[klucz]
        return f'PROF_T{t}_{klucz}'

    def produkt(self, typ, klucz, t):
        if typ == 'N':
            return f'PROF_{klucz.upper()}_{t}'
        if typ == 'B' and t == 1 and klucz in T.BUFFY_T1_ID:
            return T.BUFFY_T1_ID[klucz]
        return f'PROF_{klucz}_{t}'

    def _polprodukty_i_produkty(self):
        for t in range(1, 11):
            kr = T.KRAINY[t][0]
            for klucz, (prof, nazwa, skl, ks, t1) in T.PRZETWORZONE.items():
                iid = self.polprodukt(klucz, t)
                nz = None if (t == 1 and t1) else f'{nazwa} {T.ROMAN[t]}'
                if t == 1 and klucz == 'PLACHTA':
                    nz = 'Płachta obozowa I'
                self._dodaj(iid, 'MATERIAL', nz, 'polprodukt', prof, t,
                            dict(ksztalt=ks, kraina=t, styl='material') if t > 1 else dict(stara=True),
                            lore=[f'&7Półprodukt: &f{PROF_PL[prof]}', f'&8{kr} · tier {T.ROMAN[t]}'])
            # narzedzia
            for rodzaj, (nazwa, mat, ks, prof, kto) in T.NARZEDZIA.items():
                iid = self.produkt('N', rodzaj, t)
                self._dodaj(iid, 'TOOL', f'{nazwa} {T.ROMAN[t]}', 'narzedzie', prof, t,
                            dict(ksztalt=ks, kraina=t, styl='narzedzie'),
                            lore=[f'&7Narzędzie: &f{PROF_PL[prof]}',
                                  f'&7Zbiera źródła do tieru &f{T.ROMAN[t]}&7 włącznie.',
                                  f'&7Wyższy tier = szybciej. Zużywa się przy pracy.',
                                  '&8Nie musisz go trzymać w ręce — wystarczy w plecaku.'],
                            base=dict(material=mat))
                self.narz[iid] = dict(rodzaj=rodzaj, tier=t, wytrzymalosc=T.WYTRZYMALOSC(t), profesja=prof)
            # buffy i inne konsumpcyjne
            for prof, lst in T.RECEPTURY.items():
                for off, typ, klucz, skl, ile in lst:
                    if typ in ('B', 'O', 'H'):
                        iid = self.produkt('B', klucz, t)
                        if iid in self.items:
                            continue
                        if klucz in T.BUFFY:
                            nazwa, grupa, czas, cd, staty, opis = T.BUFFY[klucz]
                            st = {s: round(a + b * (t - 1), 4) for s, (a, b) in staty.items()}
                            self.buffy[iid] = dict(rodzaj=klucz, grupa=grupa, czas=czas, cooldown=cd, staty=st,
                                                   tier=t, nazwa=f'{nazwa} {T.ROMAN[t]}',
                                                   druzyna=(klucz == 'UCZTA'))
                            lore = [f'&7{opis}'] + [f'&a{opis_statu(s, v)}' for s, v in st.items()]
                            if czas and grupa != 'mikstura':
                                lore.append(f'&7Czas: &f{czas // 60} min' if czas >= 60 else f'&7Czas: &f{czas} s')
                            if grupa in ('eliksir', 'olej', 'posilek'):
                                lore.append(f'&8Jeden aktywny {GRUPA_PL[grupa]} naraz — nowy zastępuje stary.')
                            if cd:
                                lore.append(f'&8Odnowienie wspólne ({GRUPA_PL[grupa]}): {cd} s.')
                            self._dodaj(iid, 'CONSUMABLE', f'{nazwa} {T.ROMAN[t]}',
                                        'buff', prof, t, dict(ksztalt=IKONA_BUFFA[klucz], kolor=klucz, tier=t, styl='buff'),
                                        lore=lore)
                        else:
                            nazwa, opis = T.INNE_PRODUKTY[klucz]
                            kind = {'ZESTAW_NAPRAWCZY': 'naprawa', 'ZWOJ_WYJECIA': 'zwoj', 'OBOZOWISKO': 'obozowisko'}.get(
                                klucz, 'przeprawa' if klucz.startswith('PRZEPRAWA') else 'inne')
                            self._dodaj(iid, 'CONSUMABLE' if kind == 'zwoj' else 'MISCELLANEOUS',
                                        f'{nazwa} {T.ROMAN[t]}', kind, prof, t,
                                        dict(ksztalt=IKONA_INNE[klucz], kraina=t, styl='inne', tier=t),
                                        lore=[f'&7{opis}', f'&8{kr} · tier {T.ROMAN[t]}'])
                            self.uzycia[iid] = kind
                    elif typ == 'K':
                        iid = self.produkt('K', klucz, t)
                        nazwa, gprof, stat, (a, b) = T.KLEJNOTY[klucz]
                        v = round(a + b * (t - 1), 3)
                        self._dodaj(iid, 'GEM_STONE', f'{nazwa} {T.ROMAN[t]}', 'klejnot', gprof, t,
                                    dict(ksztalt='cut_gem' if gprof == 'jewelcrafting' else 'charged_rune', kolor=klucz,
                                         tier=t, styl='klejnot'),
                                    lore=['&7Przeciągnij na przedmiot z wolnym gniazdem.',
                                          '&8Gniazda ma sprzęt z warsztatów.'],
                                    base={stat: v, 'required-level': max(1, T.poziom_startu(t))})

    # ------------------------------------------------------------ receptury z RECEPTURY
    def token(self, tok, t):
        rodz, *reszta = tok.split(':')
        if rodz == 'S':
            prof, slot = reszta
            return self.surowiec(prof, t, slot)
        if rodz == 'P':
            return self.polprodukt(reszta[0], t)
        if rodz == 'A':
            return self.rzadki(t, reszta[0])
        raise SystemExit(f'zly token {tok}')

    def stacja(self, prof, t):
        return f'profession-{prof}' if t == 1 else f'profession-{prof}-t{t}'

    def _receptury(self):
        for prof, lst in T.RECEPTURY.items():
            for t in range(1, 11):
                for off, typ, klucz, skl, ile in lst:
                    if typ == 'P':
                        out = self.polprodukt(klucz, t)
                        skl = T.PRZETWORZONE[klucz][2]
                    elif typ == 'N':
                        out = self.produkt('N', klucz, t)
                    else:
                        out = self.produkt(typ if typ == 'K' else 'B', klucz, t)
                    ing = [(self.token(tok, t), n) for tok, n in skl]
                    self.rec.append(dict(st=self.stacja(prof, t), id=f'{prof}_t{t}_{klucz.lower()}', prof=prof,
                                         tier=t, lv=T.poziom_startu(t) + off, out=out, ile=ile, ing=ing,
                                         typ_out=self.items[out]['typ']))

    def _t1_extra(self):
        for prof, lv, out, nazwa, skl, ile in T.T1_EXTRA:
            if out not in self.items:
                if out.startswith('PROF_') or out in ('FIRE_ESSENCE', 'WITHER_ESSENCE'):
                    self._dodaj(out, 'MATERIAL', nazwa, 'polprodukt', prof, 1, dict(stara=True),
                                lore=[f'&7Półprodukt: &f{PROF_PL[prof]}', '&8Zielona Granica · tier I'])
            self.rec.append(dict(st=self.stacja(prof, 1), id=f'{prof}_t1_{out.lower().replace("prof_", "")}',
                                 prof=prof, tier=1, lv=lv, out=out, ile=ile, ing=list(skl), typ_out='MATERIAL'))

    # ------------------------------------------------------------ sprzet z rzemiosla (bazy v2)
    def _sprzet(self):
        self.rzm_bazy = []
        for b in self.katalog['przedmioty']:
            if b.get('baza') not in T.BAZY_RZEMIOSLA:
                continue
            if b['kat'] == 'bron':
                prof = T.BRON_PROFESJA.get(b['rodzaj'])
                if b['rodzaj'] == 'tarcza':
                    skl = T.SKLAD_SPRZETU[('woodworking', 'tarcza')]
                elif prof == 'enchanting':
                    skl = T.SKLAD_SPRZETU[('enchanting', 'bron1')]
                else:
                    skl = T.SKLAD_SPRZETU[(prof, 'bron2' if b.get('dwureczna') else 'bron1')]
                mn = 1
            elif b['kat'] == 'zbroja':
                prof = T.ZBROJA_PROFESJA[b['waga']]
                skl = T.SKLAD_SPRZETU[(prof, 'zbroja')]
                mn = T.MNOZNIK_SLOTU[b['slot']]
            else:
                prof = 'jewelcrafting'
                skl = T.SKLAD_SPRZETU[('jewelcrafting', 'ozdoba')]
                mn = 1
            if not prof:
                continue
            t = T.tier_poziomu(b['poziom'])
            off = T.BAZY_RZEMIOSLA[b['baza']]
            ing = [(self.token(tok, t), n * mn) for tok, n in skl]
            if b['baza'] == max(T.BAZY_RZEMIOSLA):
                ing.append((self.rzadki(t, 'serce'), T.SERCE_DO_LEPSZEJ_BAZY))
            rid = 'RZM_' + b['id']
            self.items[rid] = dict(id=rid, typ=b['typ'], nazwa=b['nazwa'], kind='sprzet', prof=prof, tier=t,
                                   ikona=None, lore=[], base={}, t1=False, baza_v2=b)
            self.rec.append(dict(st=self.stacja(prof, t), id=f'{prof}_t{t}_rzm_{b["id"].lower()}', prof=prof, tier=t,
                                 lv=T.poziom_startu(t) + off, out=rid, ile=1, ing=ing, typ_out=b['typ'],
                                 poziom_przedmiotu=b['poziom']))
            self.rzm_bazy.append(b)

    # ------------------------------------------------------------ wezly, lowiska, zwierzyna
    def _wezly(self):
        var = 20
        for prof in ('mining', 'woodworking', 'herbalism', 'farming'):
            for t in range(1, 11):
                nid = f'wezel_t{t}_{prof}'
                g, p, r = (self.surowiec(prof, t, s) for s in 'gpr')
                self.wezly[nid] = dict(profesja=prof, tier=t, glowny=g, poboczny=p, rzadki=r,
                                       szansa_poboczny=T.SZANSA_POBOCZNY, szansa_rzadki=T.SZANSA_RZADKI if r else 0,
                                       nazwa=f'{T.NAZWA_WEZLA[prof]} {T.ROMAN[t]}', nowy=True,
                                       typ_bloku='NOTEBLOCK' if prof in ('mining', 'woodworking') else 'STRINGBLOCK')
        for t in range(1, 11):  # wlokno to podstawowy material krawca — wlasny wezel w kazdym tierze
            g, p = self.surowiec('farming', t, 'p'), self.surowiec('farming', t, 'g')
            self.wezly[f'wezel_t{t}_farming_len'] = dict(profesja='farming', tier=t, glowny=g, poboczny=p, rzadki=None,
                                                         szansa_poboczny=T.SZANSA_POBOCZNY, szansa_rzadki=0,
                                                         nazwa=f'Łan lnu {T.ROMAN[t]}', nowy=True, typ_bloku='STRINGBLOCK')
        for nid, prof, nazwa, g, p in T.WEZLY_T1_DODATKOWE:
            self.wezly[nid] = dict(profesja=prof, tier=1, glowny=g, poboczny=p, rzadki=None, szansa_poboczny=T.SZANSA_POBOCZNY,
                                   szansa_rzadki=0, nazwa=nazwa, nowy=True,
                                   typ_bloku='NOTEBLOCK' if prof in ('mining', 'woodworking') else 'STRINGBLOCK')
        for nid, (prof, g, p) in T.PAKIET3_JAKO_WEZLY.items():
            r = T.T1['mining'][2] if p != T.T1['mining'][2] else None
            self.wezly[nid] = dict(profesja=prof, tier=1, glowny=g, poboczny=p, rzadki=r, szansa_poboczny=T.SZANSA_POBOCZNY,
                                   szansa_rzadki=T.SZANSA_RZADKI if r else 0, nazwa='Złoże I', nowy=False,
                                   typ_bloku='NOTEBLOCK')
        for t in range(1, 11):
            g, p, r = (self.surowiec('fishing', t, s) for s in 'gpr')
            rz = [[r, T.SZANSA_RZADKI]]
            if t == 1:
                rz += [[i, s] for i, s in T.LOWISKO_T1_EXTRA]
            self.lowiska[t] = dict(glowny=g, poboczny=p, szansa_poboczny=T.SZANSA_POBOCZNY, rzadkie=rz,
                                   blok=None if t == 1 else f'lowisko_t{t}')
        for mob, (t, g, r) in T.ZWIERZYNA.items():
            self.zwierzyna[mob] = dict(tier=t, glowny=g or self.surowiec('hunting', t, 'g'),
                                       poboczny=self.surowiec('hunting', t, 'p'),
                                       rzadki=r or self.surowiec('hunting', t, 'r'), szansa_poboczny=T.SZANSA_POBOCZNY,
                                       szansa_rzadki=T.SZANSA_RZADKI_ZWLOKI)

    # ------------------------------------------------------------ ekwiwalent surowcowy (akcje)
    def _ekwiwalenty(self):
        ekw = {}
        # surowce: 1 / szansa na akcje
        for iid in self.surowce:
            ekw[iid] = 1.0
        for w in list(self.wezly.values()) + [dict(poboczny=l['poboczny'], rzadki=None) for l in self.lowiska.values()] + list(self.zwierzyna.values()):
            if w.get('poboczny') and w['poboczny'] != w.get('glowny'):
                ekw[w['poboczny']] = max(ekw.get(w['poboczny'], 1.0), round(1 / T.SZANSA_POBOCZNY, 2)) if not self._jest_glownym(w['poboczny']) else 1.0
            if w.get('rzadki') and w.get('szansa_rzadki'):
                ekw[w['rzadki']] = round(1 / w['szansa_rzadki'], 2)
        for l in self.lowiska.values():
            for i, s in l['rzadkie']:
                ekw[i] = round(1 / s, 2)
        for t, tab in self.rzadkie_tabele.items():
            for rodz, (iid, s) in tab.items():
                ekw[iid] = T.EKW_RZADKICH[rodz]
        self.ekw = ekw
        prod = {r['out']: r for r in self.rec}

        def e(i, stos=()):
            if i in self.ekw:
                return self.ekw[i]
            if i in stos or i not in prod:
                raise SystemExit(f'brak zrodla dla {i} (stos {stos})')
            r = prod[i]
            v = sum(n * e(x, stos + (i,)) for x, n in r['ing']) / r['ile']
            self.ekw[i] = round(v, 3)
            return self.ekw[i]

        for r in self.rec:
            r['ekw'] = round(sum(n * e(x) for x, n in r['ing']), 3)
        for i in self.items:
            if i not in self.ekw and i in prod:
                e(i)

    def _jest_glownym(self, iid):
        return any(w.get('glowny') == iid for w in self.wezly.values()) or \
            any(l['glowny'] == iid for l in self.lowiska.values()) or \
            any(z['glowny'] == iid for z in self.zwierzyna.values())

    # ------------------------------------------------------------ XP i ceny
    def _xp_ceny(self):
        for r in self.rec:
            r['xp'] = int(round(r['ekw'] * T.g(r['tier']) * T.XP_RZEMIOSLA))
        self.ceny = {}
        for iid, it in self.items.items():
            t = it['tier'] or 1
            e = self.ekw.get(iid)
            if e is None:
                continue
            wartosc = round(e * T.WARTOSC_SUROWCA_T1 * T.WZROST_WARTOSCI ** (t - 1) * (1.0 if it['kind'] == 'surowiec' else 1.15)
                            + (0 if it['kind'] in ('surowiec', 'rzadki') else 1.6), 2)
            self.ceny[iid] = dict(wartosc=wartosc, skup=round(wartosc * T.SKUP, 2))

    # ------------------------------------------------------------ zlecenia
    def _zlecenia(self):
        self.zlecenia = defaultdict(dict)
        for prof in RZEMIOSLO:
            for t in range(1, 11):
                pula = []
                for r in self.rec:
                    if r['prof'] != prof or r['tier'] != t:
                        continue
                    it = self.items[r['out']]
                    if it['kind'] in ('sprzet', 'narzedzie'):
                        continue
                    pula.append(self._zlecenie(r['out'], t))
                self.zlecenia[prof][t] = pula
        for prof in ZBIERACKIE:
            for t in range(1, 11):
                pula = [self._zlecenie(self.surowiec(prof, t, 'g'), t), self._zlecenie(self.surowiec(prof, t, 'p'), t)]
                self.zlecenia[prof][t] = pula
        self.zlecenia_startowe = {}
        for prof in ZBIERACKIE:
            rodzaj = T.PROF_ZBIERACKIE[prof][0]
            self.zlecenia_startowe[prof] = dict(zbierz=self.surowiec(prof, 1, 'g'), ilosc=T.ZLECENIA_STARTOWE_ILE,
                                                narzedzie=self.produkt('N', rodzaj, 1), xp=50)

    def _zlecenie(self, iid, t):
        e = self.ekw[iid]
        n = max(1, int(math.ceil(T.ZLECENIA['ekwiwalent_cel'] / e)))
        return dict(id=iid, ilosc=n, xp=int(round(n * e * T.g(t) * T.XP_ZLECENIA)),
                    pieniadze=round(n * self.ceny[iid]['skup'] * T.ZLECENIA['nagroda_pieniadze'], 1))


IKONY_T1_BRAK = {'PROF_WILD_SPICE': ('dried_herbs', 'PRZYPRAWA'), 'FIRE_ESSENCE': ('water_essence', 'OGIEN'),
                 'WITHER_ESSENCE': ('water_essence', 'OBUMARCIE')}
GRUPA_PL = {'mikstura': 'mikstura', 'eliksir': 'eliksir', 'olej': 'olej', 'posilek': 'posiłek', 'opatrunek': 'opatrunek'}
IKONA_BUFFA = {'MIKSTURA_LECZNICZA': 'combat_tonic', 'MIKSTURA_MANY': 'alchemical_base', 'ODTRUTKA': 'antidote',
               'ELIKSIR_SILY': 'alchemical_solvent', 'ELIKSIR_UMYSLU': 'alchemical_solvent',
               'ELIKSIR_SKORY': 'alchemical_solvent', 'ELIKSIR_ZWINNOSCI': 'alchemical_solvent', 'OLEJ': 'weapon_oil',
               'POSILEK_SYTY': 'hunter_roast', 'POSILEK_ROSOL': 'fisher_stew', 'POSILEK_PROWIANT': 'trail_ration',
               'UCZTA': 'harvest_feast', 'DANIE_MISTRZA': 'master_banquet', 'OPATRUNEK': 'fine_wool'}
IKONA_INNE = {'ZESTAW_NAPRAWCZY': 'metal_repair_kit', 'ZWOJ_WYJECIA': 'empty_socket', 'OBOZOWISKO': 'namiot',
              'PRZEPRAWA_MOST': 'support_beam', 'PRZEPRAWA_DRABINA': 'birch_lath', 'PRZEPRAWA_PODPORA': 'reinforced_block'}
STAT_PL = {'leczenie': ('Leczy {v} HP', 1), 'mana': ('Przywraca {v} many', 1), 'oczyszczenie': ('Oczyszcza z trucizn', 0),
           'physical-damage': ('+{v}% obrażeń fizycznych', 1), 'magic-damage': ('+{v}% obrażeń magicznych', 1),
           'skill-damage': ('+{v}% obrażeń umiejętności', 1), 'damage-reduction': ('-{v}% otrzymywanych obrażeń', 1),
           'dodge-rating': ('+{v} uniku', 1), 'movement-speed': ('+{v} szybkości ruchu', 4),
           'critical-strike-chance': ('+{v}% szansy na trafienie krytyczne', 1),
           'critical-strike-power': ('+{v}% siły trafienia krytycznego', 1), 'cooldown-reduction': ('-{v}% czasu odnowienia', 1),
           'max-health': ('+{v} maks. zdrowia', 1), 'health-regeneration': ('+{v} regeneracji zdrowia/s', 3),
           'max-stamina': ('+{v} maks. wytrzymałości', 1), 'stamina-regeneration': ('+{v} odnowy wytrzymałości/s', 3),
           'leczenie_w_czasie': ('Leczy {v} HP w ciągu 8 s', 1), 'mana-regeneration': ('+{v} regeneracji many/s', 3)}


def opis_statu(s, v):
    wzor, cyfry = STAT_PL[s]
    if cyfry == 0:
        return wzor
    tekst = f'{v:.{cyfry}f}'.rstrip('0').rstrip('.').replace('.', ',')
    return wzor.format(v=tekst)
