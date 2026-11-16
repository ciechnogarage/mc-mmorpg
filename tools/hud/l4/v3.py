# HUD v3 (20.10), dolaczany na koncu build.py (exec): okno przygasa zamiast sie zwijac, lewa kolumna
# (efekty z czasem, towarzysze: Chowaniec + slugi, druzyna z glowami), cel pod minimapa po prawej.
# Dane z pluginu HudDane (%hud_...%), Chowance (%chowance_aktywny_...%) i MMOCore.
from PIL import Image as _I

def save_fade(im, name, alfa=0.45):
    """Kopia tekstury z mnozona alfa (przygaszone okno w walce)."""
    im = im.copy(); px = im.load()
    for yy in range(im.height):
        for xx in range(im.width):
            r_, g_, b_, a_ = px[xx, yy]
            if a_: px[xx, yy] = (r_, g_, b_, max(1, int(a_ * alfa)))
    save(im, name)

def wczytaj(name):
    """Tekstura 1x z juz zapisanej 2x."""
    im = _I.open(TEX/f'{name}.png').convert('RGBA'); return im.resize((im.width//2, im.height//2), N)

def band(w, h, prawo=False, a0=140):
    im = _I.new('RGBA', (w, h), T)
    for i in range(w):
        a_ = int(a0 * max(0, 1 - i/w) ** 1.1)
        for j in range(h): im.putpixel(((w-1-i) if prawo else i, j), (37, 28, 43, a_))
    return im

WALKA_CHW = '- placeholder{ph=%chowance_w_walce%;val=tak} false hide'
OKNO_PELNE = '- placeholder{ph=%hud_okno%;val=pelne} true hide'
OKNO_PRZYG = '- placeholder{ph=%hud_okno%;val=przygaszone} true hide'
def head(x, y, size, target):
    # Warstwa head laduje sie ~7*size GUI wyzej niz offset (zmierzone 03.10 dla size 4 = 28).
    return f"      head:\n        size: {size}\n        target: '{target}'\n      align: right\n      offset: {{x: {x}, y: {-(y + 7*size)}}}\n"

# ---------------- OKNO: pelne / przygaszone ----------------
for n_ in ('okno_a', 'okno_b', 'exp_fill', 'rasa_tlo', 'rasa_czlowiek', 'rasa_elf', 'rasa_ork', 'rasa_nieumarly'):
    save_fade(wczytaj(n_), n_ + '_p')
DIM = {'<#FFF1CC>': '<#A89A8C>', '<#D5A96B>': '<#8F7656>'}
def okno_asset(name, cond, przyg):
    s_ = '_p' if przyg else ''
    def t_(kolor, tresc, x, y):
        return txt((DIM.get(kolor, kolor) if przyg else kolor) + tresc, x, y, 'right')
    warstwy = [
      ('nick', t_('<#FFF1CC>', '%player_name%', 46, 8)),
      ('klasa', t_('<#D5A96B>', '%mmocore_class%', 43, 20)),
      ('exp-proc', t_('<#D5A96B>', '%mmocore_level_percent%%', 116, 30)),
    ]
    if przyg:
        warstwy.append(('alarm', tex('portret_alarm', 5, 5, warunki('- progress{l=hp;o=<;v=30%} true hide'))))
    warstwy += [(f'rasa-{r}', tex(f'rasa_{r}{s_}', 31, 31, warunki(f'- placeholder{{ph=%wiary_rasa%;val={r}}} true hide'))) for r in RASY]
    warstwy += [
      ('rasa-tlo', tex('rasa_tlo' + s_, 30, 30, warunki('- placeholder{ph=%wiary_rasa%;val=brak} false hide'))),
      ('exp', tex('exp_fill' + s_, 43, 32, fn('doswiadczenie', 69))),
      ('glowa', "      head: {size: 4}\n      align: right\n      offset: {x: 6, y: -34}\n"),
      ('hp', tex('dane', 0, 0, fn('health', 1))),
      ('tlo-b', tex('okno_b' + s_, 128, 0)),
      ('tlo', tex('okno_a' + s_, 0, 0)),
    ]
    return asset(name, BAZA + [cond], warstwy)
nowe_okno = [okno_asset('l1-okno', OKNO_PELNE, False), okno_asset('l1-okno-przygaszone', OKNO_PRZYG, True)]
# plakietki (z v2) tylko przy pelnym oknie: podmiana warunkow OKNO_W na OKNO_PELNE
for a_ in okno:
    if a_.startswith('l1-pl-'):
        for w_ in OKNO_W: a_ = a_.replace('    ' + w_ + '\n', '')
        a_ = a_.replace('  layers:\n', '    ' + OKNO_PELNE + '\n  layers:\n', 1)
        nowe_okno.append(a_)
(OUT/'hud_assets/hud/l1-okno.yml').write_text('# Okno postaci v3 (20.10): w walce, walce Chowancow i trybie kompakt PRZYGASA (alfa 45%), nie zwija sie.\n'
  '# Stan z %hud_okno% (HudDane), bo MythicHUD nie ma "lub" w warunkach.\n' + '\n'.join(nowe_okno))

# ---------------- IKONY EFEKTOW 9x9 ----------------
def ik_(rys):
    im, d = new(9, 9); rys(d); return im
EF = {
 'rany': lambda d: (rect(d,(2,0,6,0),'cream'),rect(d,(1,1,7,4),'cream'),rect(d,(2,5,6,6),'cream'),rect(d,(2,2,3,3),'ink'),rect(d,(5,2,6,3),'ink'),rect(d,(3,7,5,8),'cream')),
 'trucizna': lambda d: (rect(d,(3,0,5,1),'gold1'),rect(d,(2,2,6,8),'green'),rect(d,(3,3,4,4),'gold2'),line(d,[(2,8),(6,8)],'green0')),
 'uschniecie': lambda d: (rect(d,(1,0,7,5),'leather2'),rect(d,(2,6,6,7),'leather2'),rect(d,(2,2,3,3),'ink'),rect(d,(5,2,6,3),'ink')),
 'spowolnienie': lambda d: (d.ellipse((1,1,7,7),outline=c('navy1')),rect(d,(4,2,4,4),'blue'),rect(d,(4,4,6,4),'blue')),
 'oslabienie': lambda d: (rect(d,(1,6,7,7),'leather2'),rect(d,(3,1,5,6),'grey' if 'grey' in P else 'leather1'),rect(d,(2,3,2,3),'ink')),
 'regeneracja': lambda d: (rect(d,(3,1,5,7),'redhi'),rect(d,(1,3,7,5),'redhi'),rect(d,(4,2,4,6),'cream')),
 'sila': lambda d: (rect(d,(1,3,7,5),'red'),rect(d,(1,2,2,6),'redhi'),rect(d,(6,2,7,6),'redhi'),rect(d,(0,3,0,5),'gold1'),rect(d,(8,3,8,5),'gold1')),
 'odpornosc': lambda d: (d.polygon([(1,1),(7,1),(7,5),(4,8),(1,5)],fill=c('gold1')),rect(d,(3,2,5,4),'navy0')),
 'szybkosc': lambda d: (line(d,[(0,2),(5,2)],'soul'),line(d,[(2,4),(8,4)],'soul'),line(d,[(0,6),(5,6)],'soul')),
 'ognioodpornosc': lambda d: (d.polygon([(4,0),(7,4),(6,8),(2,8),(1,4)],fill=c('redhi')),d.polygon([(4,3),(5,6),(4,8),(3,6)],fill=c('gold2'))),
}
for k_, f_ in EF.items(): save(ik_(f_), f'ef_{k_}')
save(band(36, 11), 'ef_tlo')

# ---------------- TOWARZYSZE ----------------
WT = 145
for h_ in (21, 36):
    im, d = new(WT+1, h_+1); panel(d, (0, 0, WT, h_))
    save(im.crop((0, 0, 128, h_+1)), f'tow_{h_}_a'); save(im.crop((128, 0, WT+1, h_+1)), f'tow_{h_}_b')
im, d = new(122, 4); recess(d, (0, 0, 121, 3)); save(im, 'pet_tlo')
im, d = new(120, 2); rect(d, (0, 0, 119, 1), 'green'); line(d, [(0, 0), (119, 0)], 'gold2'); save(im, 'pet_hp')
im, d = new(120, 2); rect(d, (0, 0, 119, 1), 'leather1'); save(im, 'pet_hp_zemdlony')
im, d = new(120, 1); rect(d, (0, 0, 119, 0), 'gold1'); save(im, 'pet_exp')
im, d = new(27, 5); recess(d, (0, 0, 26, 4)); save(im, 'sl_tlo')
im, d = new(25, 3); rect(d, (0, 0, 24, 2), 'red'); line(d, [(0, 0), (24, 0)], 'redhi'); save(im, 'sl_hp')
save(ikona('dusza'), 'sl_ikona')

PET = '- placeholder{ph=%chowance_aktywny_istnieje%;val=tak} true hide'
BEZ_PET = '- placeholder{ph=%chowance_aktywny_istnieje%;val=tak} false hide'
def uk(e, t): return f'- placeholder{{ph=%hud_uklad%;val=e{e}t{t}}} true hide'
def ef(e): return '- placeholder{ph=%hud_efekty_liczba%;v=1;o=>=} ' + ('true' if e else 'false') + ' hide'
kol = []        # assety kolumny
kol_el = []     # (element, asset, y od gory)
TOW_Y = {0: 62, 1: 75}  # e0 / e1
for e in (0, 1):
    for t, h_ in ((1, 21), (2, 36)):
        kol.append(asset(f'l3-tow-tlo-e{e}t{t}', [SPEC, ZYJE, WALKA_CHW, uk(e, t)], [
          ('b', tex(f'tow_{h_}_b', 128, 0)), ('a', tex(f'tow_{h_}_a', 0, 0))]))
        kol_el.append((f'tow-tlo-e{e}t{t}', f'l3-tow-tlo-e{e}t{t}', TOW_Y[e]))
    # Chowaniec: zawsze pierwszy wiersz panelu
    for zem in (False, True):
        nm = f'l3-pet-e{e}' + ('-zemdlony' if zem else '')
        warstwy = [
          ('nazwa', txt('<#FFE7AE>%chowance_aktywny_nazwa% <#D5A96B>%chowance_aktywny_poziom% <#B39066>%hud_pet_typ%', 6, 5, 'right')),
          ('hp-tekst', txt('<#F36D62>zemdlony' if zem else '<#FFF1CC>%chowance_aktywny_hp%/%chowance_aktywny_hpmax%', 104, 5, 'right')),
        ]
        if zem: warstwy.append(('hp', tex('pet_hp_zemdlony', 7, 15)))
        else: warstwy.append(('hp', tex('pet_hp', 7, 15, fn('chowaniec_hp', 120))))
        warstwy += [('exp', tex('pet_exp', 7, 17, fn('chowaniec_exp', 120))), ('tlo', tex('pet_tlo', 6, 14))]
        kol.append(asset(nm, [SPEC, ZYJE, WALKA_CHW, PET,
                              f'- placeholder{{ph=%chowance_aktywny_zemdlony%;val=tak}} {"true" if zem else "false"} hide',
                              ef(e)], warstwy))
        kol_el.append((nm.replace('l3-', ''), nm, TOW_Y[e]))
    # Slugi: wiersz 1 (bez Chowanca) albo 2 (z Chowancem)
    for z_pet, dy in ((False, 0), (True, 15)):
        warunek_pet = PET if z_pet else BEZ_PET
        for n in (1, 2, 3):
            nm = f'l3-sluga{n}-e{e}{"p" if z_pet else ""}'
            x0 = 6 + (n-1)*40
            kol.append(asset(nm, [SPEC, ZYJE, WALKA_CHW, warunek_pet, ef(e), f'- placeholder{{ph=%hud_slugi_liczba%;v={n};o=>=}} true hide'], [
              ('czas', txt('<#B9E6E0>%hud_slugi_' + str(n) + '_s%', x0+11, 10, 'right')),
              ('hp', tex('sl_hp', x0+12, 5, fn(f'slugi_{n}_hp', 25))),
              ('hp-tlo', tex('sl_tlo', x0+11, 4)),
              ('ikona', tex('sl_ikona', x0, 4)),
            ]))
            kol_el.append((nm.replace('l3-', ''), nm, TOW_Y[e] + dy))
        nm = f'l3-slugi-licznik-e{e}{"p" if z_pet else ""}'
        kol.append(asset(nm, [SPEC, ZYJE, WALKA_CHW, warunek_pet, ef(e), '- placeholder{ph=%hud_slugi_liczba%;v=1;o=>=} true hide'], [
          ('t', txt('<#B9E6E0>%hud_slugi_liczba%/%hud_slugi_limit%', 126, 6, 'right')), ('dane', tex('dane', 0, 0))]))
        kol_el.append((nm.replace('l3-', ''), nm, TOW_Y[e] + dy))

# ---------------- EFEKTY (rzad zetonow pod oknem) ----------------
for n in (1, 2, 3, 4):
    warstwy = [('czas', txt('<#FFF1CC>%hud_efekt_' + str(n) + '_czas%', 11, 2, 'right'))]
    warstwy += [(f'ik-{k_}', tex(f'ef_{k_}', 1, 1, warunki(f'- placeholder{{ph=%hud_efekt_{n}_id%;val={k_}}} true hide'))) for k_ in EF]
    warstwy += [('tlo', tex('ef_tlo', 0, 0))]
    kol.append(asset(f'l3-efekt{n}', [SPEC, ZYJE, f'- placeholder{{ph=%hud_efekty_liczba%;v={n};o=>=}} true hide'], warstwy))
    kol_el.append((f'efekt{n}', f'l3-efekt{n}', 62, (n-1)*37))

# ---------------- DRUZYNA ----------------
im = band(150, 12); save(im.crop((0, 0, 75, 12)), 'dr_tlo_a'); save(im.crop((75, 0, 150, 12)), 'dr_tlo_b')
im, d = new(108, 4); recess(d, (0, 0, 107, 3)); save(im, 'dr_hp_tlo')
im, d = new(106, 2); rect(d, (0, 0, 105, 1), 'red'); line(d, [(0, 0), (105, 0)], 'redhi'); save(im, 'dr_hp')
im, d = new(106, 2); rect(d, (0, 0, 105, 1), 'leather1'); save(im, 'dr_hp_szary')
im, d = new(106, 1); rect(d, (0, 0, 105, 0), 'blue'); save(im, 'dr_zasob')
im, d = new(9, 6); rect(d,(0,2,8,5),'gold2'); rect(d,(0,0,0,2),'gold2'); rect(d,(4,0,4,2),'gold2'); rect(d,(8,0,8,2),'gold2'); rect(d,(1,4,7,4),'gold0'); save(im, 'dr_korona')
save(ikona('dusza'), 'dr_martwy')
WYS = {0: 0, 1: 23, 2: 38}
for e in (0, 1):
    for t in (0, 1, 2):
        y0 = TOW_Y[e] + (WYS[t] if t else 0) + (2 if t else 0)
        if e == 0 and t == 0: y0 = 62
        warunek = uk(e, t)
        nm = f'l3-dr-naglowek-e{e}t{t}'
        kol.append(asset(nm, [SPEC, ZYJE, WALKA_CHW, warunek, '- placeholder{ph=%hud_druzyna_liczba%;v=1;o=>=} true hide'], [
          ('t', txt('<#FFE7AE>Drużyna <#D5A96B>%mmocore_party_count%/8', 1, 1, 'right')), ('dane', tex('dane', 0, 0))]))
        kol_el.append((nm.replace('l3-', ''), nm, y0))
        for n in range(1, 8):
            nm = f'l3-dr{n}-e{e}t{t}'
            szary = f'- placeholder{{ph=%hud_druzyna_{n}_stan%;val=daleko}} false hide'
            kol.append(asset(nm, [SPEC, ZYJE, WALKA_CHW, warunek, f'- placeholder{{ph=%hud_druzyna_liczba%;v={n};o=>=}} true hide'], [
              ('nick', txt(f'<#FFF1CC>%hud_druzyna_{n}_nick%', 12, 1, 'right')),
              ('poziom', txt(f'<#D5A96B>%hud_druzyna_{n}_poziom%', 106, 1, 'right')),
              ('korona', tex('dr_korona', 2, -3, warunki(f'- placeholder{{ph=%hud_druzyna_{n}_lider%;val=tak}} true hide'))),
              ('martwy', tex('dr_martwy', 124, 1, warunki(f'- placeholder{{ph=%hud_druzyna_{n}_stan%;val=martwy}} true hide'))),
              ('glowa', head(1, 1, 1, f'%hud_druzyna_{n}_nazwa%')),
              ('hp-daleko', tex('dr_hp_szary', 13, 9, fn(f'druzyna_{n}_hp', 106) + warunki(szary.replace('false hide', 'true hide')))),
              ('hp', tex('dr_hp', 13, 9, fn(f'druzyna_{n}_hp', 106) + warunki(szary, f'- placeholder{{ph=%hud_druzyna_{n}_stan%;val=martwy}} false hide'))),
              ('zasob', tex('dr_zasob', 13, 11, fn(f'druzyna_{n}_zasob', 106))),
              ('hp-tlo', tex('dr_hp_tlo', 12, 8)),
              ('tlo-b', tex('dr_tlo_b', 75, 0)),
              ('tlo', tex('dr_tlo_a', 0, 0)),
            ]))
            kol_el.append((nm.replace('l3-', ''), nm, y0 + 10 + (n-1)*13))
(OUT/'hud_assets/hud/l3-kolumna.yml').write_text('# HUD v3 (20.10): lewa kolumna pod oknem postaci - efekty, towarzysze (Chowaniec, slugi), druzyna.\n'
  '# Warianty polozenia wg %hud_uklad% (HudDane): e = rzad efektow, t = wiersze towarzyszy.\n' + '\n'.join(kol))
lst = ['# Listenery v3: paski druzyny i slug (HudDane).']
for n in range(1, 8):
    lst += [f'druzyna_{n}_hp:', f"  value: '%hud_druzyna_{n}_hp%'", f"  max: '%hud_druzyna_{n}_hpmax%'",
            f'druzyna_{n}_zasob:', f"  value: '%hud_druzyna_{n}_zasob%'", f"  max: '%hud_druzyna_{n}_zasobmax%'"]
for n in (1, 2, 3):
    lst += [f'slugi_{n}_hp:', f"  value: '%hud_slugi_{n}_hp%'", f"  max: '%hud_slugi_{n}_hpmax%'"]
(OUT/'listeners/l3.yml').write_text('\n'.join(lst) + '\n')
lay3 = 'l3-kolumna-layout:\n  elements:\n'
# Pierwszy element rysuje sie na wierzchu: tla paneli na koniec.
kol_el.sort(key=lambda e_: e_[0].startswith('tow-tlo'))
for el_ in kol_el:
    nazwa, a_, y_ = el_[:3]; dx = el_[3] if len(el_) > 3 else 0
    lay3 += el(nazwa, a_, 0.0, 100.0, 4 + dx, -y_)
(OUT/'layouts/l3.yml').write_text('# HUD v3: lewa kolumna (efekty, towarzysze, druzyna) - pozycje wyliczone w v3.py.\n' + lay3)

# ---------------- CEL pod minimapa (prawa strona) ----------------
# NMinimap: 254 px fizycznych od y=60, wiec dol mapy ~ 29% wysokosci 1080p przy kazdej skali GUI -> position y 70.
save(band(230, 30, prawo=True, a0=150).crop((0, 0, 115, 30)), 'celp_a'); save(band(230, 30, prawo=True, a0=150).crop((115, 0, 230, 30)), 'celp_b')
im, d = new(2, 30); rect(d, (0, 0, 1, 29), 'gold1'); rect(d, (0, 0, 1, 0), 'gold2'); save(im, 'celp_kreska')
im, d = new(9, 9); d.polygon([(6, 1), (2, 4), (6, 7)], fill=c('gold1')); save(im, 'celp_strzalka')
def celp(name, conds, tytul, krok):
    # Teksty tylko align right (od lewej): align left w tekscie psuje szerokosc calego HUD-a (zmierzone 20.10).
    return asset(name, conds + CEL_WSP, [
      ('tytul', txt(tytul, 18, 5, 'right')),
      ('krok', txt(krok, 18, 17, 'right')),
      ('ikona', tex('cel_ikona', 6, 4)),
      ('strzalka', tex('cel_strzalka', 7, 16)),
      ('tlo-b', tex('cel_b', 115, 0)),
      ('tlo', tex('cel_a', 0, 0)),
    ])
cele3 = [
  celp('cel-chowance', ['- placeholder{ph=%chowance_kroki_aktywne%;val=true} true hide',
                        '- placeholder{ph=%chowance_ma_zadanie_mmocore%;val=tak} false hide'],
       '<#FFE7AE>Pierwsze kroki <#D5A96B>%chowance_kroki_postep%', '<#FFF1CC>%chowance_kroki_opis%'),
  celp('cel-mmocore', ['- placeholder{ph=%chowance_ma_zadanie_mmocore%;val=tak} true hide'],
       '<#FFE7AE>%mmocore_quest% <#D5A96B>%mmocore_quest_progress%%', '<#FFF1CC>%mmocore_quest_objective%'),
]
(OUT/'hud_assets/hud/cel.yml').write_text('# Cel zadania v3 (20.10): wstega pod minimapa po prawej (lewa strona = postacie, prawa = swiat).\n' + '\n'.join(cele3))
(OUT/'layouts/cel.yml').write_text('cel-layout:\n  elements:\n'
  + el('samouczek', 'cel-chowance', 100.0, 70.0, -234, 0) + el('zadanie', 'cel-mmocore', 100.0, 70.0, -234, 0))

# okno-layout: tylko pelne/przygaszone + plakietki
i_ = lay.index('l1-okno-layout:')
lay_ok = 'l1-okno-layout:\n  elements:\n'
for a_ in ('l1-pl-patron-brak','l1-pl-wiara','l1-pl-laska','l1-pl-odznaki','l1-pl-atrybuty','l1-pl-umiejetnosci','l1-okno','l1-okno-przygaszone'):
    lay_ok += el(a_.replace('l1-', ''), a_, 0.0, 100.0, 4, -4)
(OUT/'layouts/l1.yml').write_text('# HUD L1/v3: pas nad hotbarem + okno postaci.\n' + lay[:i_] + lay_ok)
print('v3:', len(kol), 'assetow kolumny')
# Bramka Nexo: tekstura > 256 px = brak glifu i przesuniety caly HUD (zmierzone 20.10, tlo druzyny 300 px).
_za_duze = [f.name for f in TEX.glob('*.png') if max(_I.open(f).size) > 256]
assert not _za_duze, f'tekstury > 256 px: {_za_duze}'
print('bramka 256 px: ok')
