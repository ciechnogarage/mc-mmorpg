# HUD v5 (20.10), po v4.py: cel zadania w lewej kolumnie (nie zalezy od rozdzielczosci i minimapy), strzalka i odleglosc
# do celu kroku, kierunek obrazen przy celowniku, medal swieci przed awansem, kafelki umiejetnosci klasy (sloty MMOCore),
# tryb minimalny (ui_hud=minimal: tylko pas i cel walki).
import re as _re

# ---------------- CEL ZADANIA w kolumnie ----------------
STRZ = {}
def strzalka(kat):
    im, d = new(9, 9)
    import math
    pts = [(0, -4), (3, 2), (0, 0), (-3, 2)]
    a = math.radians(kat)
    pp = [(4 + x*math.cos(a) - y*math.sin(a), 4 + x*math.sin(a) + y*math.cos(a)) for x, y in pts]
    d.polygon(pp, fill=c('gold2')); return im
for i, k_ in enumerate(('n', 'ne', 'e', 'se', 's', 'sw', 'w', 'nw')): save(strzalka(i*45), f'zad_{k_}')
save(ik_(lambda d: (d.ellipse((1, 1, 7, 7), outline=c('gold2')), rect(d, (4, 4, 4, 4), 'gold2'))), 'zad_tu')
ZAD_WSP = [SPEC, ZYJE, '- placeholder{ph=%hud_zadanie_widac%;val=tak} true hide']
# Koncowka ramki 38 GUI (76 px) jako dalszy ciag gradientu cel_a (kolor i krycie z jego prawej kolumny): widoczne tlo do ~147 GUI,
# wygaszenie do 153. 16.11: koncowka z cel_b miala krycie 10->0 (niewidoczna), tlo konczylo sie na 115 GUI i tekst wystawal.
_ca = _I.open(TEX/'cel_a.png').convert('RGBA'); _r, _g, _b, _a0 = _ca.getpixel((_ca.width - 1, _ca.height // 2))
_cc = _I.new('RGBA', (76, _ca.height))
for _x in range(76):
  _al = round(_a0 - (_a0 - 54) * _x / 63) if _x < 64 else round(54 * (75 - _x) / 12)
  for _y in range(_ca.height): _cc.putpixel((_x, _y), (_r, _g, _b, max(_al, 1)))
_cc.save(TEX/'cel_c.png')
zad = []
for e in (0, 1):
    # 16.11 (wlasciciel: "questy wychodza na pol ekranu"): ramka 230 -> 140 GUI, krok w 2 liniach po 22 znaki (HudDane
    # zadanie_krok_1/2), linie co 9 GUI - wysokosc 30 GUI bez zmian, bo kolumna l3 pod celem zaczyna sie 32 GUI nizej.
    warstwy = [
      # odleglosc w linii tytulu (pusta bez celu na mapie; HudDane przycina tytul, by linia miescila sie w max-px); osobna warstwa przy prawej krawedzi nachodzila na "0/4" (klon 16.11)
      ('tytul', txt('<#FFE7AE>%hud_zadanie_tytul% <#D5A96B>%hud_zadanie_postep% %hud_zadanie_odl%', 18, 3, 'right')),
      ('krok-1', txt('<#FFF1CC>%hud_zadanie_krok_1%', 18, 12, 'right')),
      ('krok-2', txt('<#FFF1CC>%hud_zadanie_krok_2%', 18, 21, 'right')),
      ('ikona', tex('cel_ikona', 6, 2)),
      ('strzalka', tex('cel_strzalka', 7, 12, warunki('- placeholder{ph=%hud_zadanie_kierunek%;val=brak} true hide'))),
      ('tlo-c', tex('cel_c', 115, 0)),
      ('tlo', tex('cel_a', 0, 0)),
    ]
    zad.append(asset(f'l5-zadanie-e{e}', ZAD_WSP + [ef(e)], warstwy))
    # kierunek: osobny asset (warunki na teksturach dzialaja, odleglosc to tekst bez warunku)
    kier = []
    kier += [(f'k-{k_}', tex(f'zad_{k_}', 6, 11, warunki(f'- placeholder{{ph=%hud_zadanie_kierunek%;val={k_}}} true hide')))
             for k_ in ('n', 'ne', 'e', 'se', 's', 'sw', 'w', 'nw', 'tu')]
    zad.append(asset(f'l5-zadanie-kier-e{e}', ZAD_WSP + [ef(e), '- placeholder{ph=%hud_zadanie_kierunek%;val=brak} false hide'], kier))
(OUT/'hud_assets/hud/cel.yml').write_text('# Cel zadania v5 (20.10): w lewej kolumnie pod efektami; tekst z HudDane (przyciety), strzalka do celu kroku.\n' + '\n'.join(zad))
lay_c = 'cel-layout:\n  elements:\n'
for e in (0, 1):
    y_ = 62 + 13*e
    lay_c += el(f'zadanie-kier-e{e}', f'l5-zadanie-kier-e{e}', 0.0, 100.0, 4, -y_)
    lay_c += el(f'zadanie-e{e}', f'l5-zadanie-e{e}', 0.0, 100.0, 4, -y_)
(OUT/'layouts/cel.yml').write_text('# Cel zadania v5: lewa kolumna (patrz v5.py).\n' + lay_c)

# ---------------- KIERUNEK OBRAZEN ----------------
import math as _m
OB = {}
for i, k_ in enumerate(('n', 'ne', 'e', 'se', 's', 'sw', 'w', 'nw')):
    im, d = new(15, 15)
    for t_ in range(-3, 4):
        a = _m.radians(i*45 - 90 + t_*7)
        for r_ in (6, 7):
            x, y = 7 + r_*_m.cos(a), 7 + r_*_m.sin(a)
            im.putpixel((int(round(x)), int(round(y))), (243, 109, 98, 230 if abs(t_) < 3 else 150))
    save(im, f'ob_{k_}')
ob = [asset('l5-obrazenia', [SPEC, ZYJE, '- placeholder{ph=%hud_obrazenia_kierunek%;val=brak} false hide'],
            [(f'k-{k_}', tex(f'ob_{k_}', 0, 0, warunki(f'- placeholder{{ph=%hud_obrazenia_kierunek%;val={k_}}} true hide'))) for k_ in ('n', 'ne', 'e', 'se', 's', 'sw', 'w', 'nw')]
            + [('dane', tex('dane', 0, 0))])]
# Luk w promieniu 7 wokol srodka tekstury 15x15, ktora stoi na srodku ekranu. Skala 1 -> powiekszamy x2 kopia 30x30.
for k_ in ('n', 'ne', 'e', 'se', 's', 'sw', 'w', 'nw'):
    im = _I.open(TEX/f'ob_{k_}.png'); im.resize((im.width*2, im.height*2), N).save(TEX/f'ob_{k_}.png')

# ---------------- MEDAL przed awansem ----------------
im, d = new(28, 28)
d.ellipse((0, 0, 27, 27), outline=c('gold2'), width=2); d.ellipse((2, 2, 25, 25), outline=c('gold1'))
save(im, 'medal_awans')
aw = [asset('l5-awans', [SPEC, ZYJE, '- placeholder{ph=%hud_awans_blisko%;val=tak} true hide'], [('pierscien', tex('medal_awans', 0, 0))])]

# ---------------- UMIEJETNOSCI (sloty MMOCore 1-4) ----------------
UW = 36
for st, kol_r in (('gotowa', 'gold1'), ('odnowienie', 'leather0')):
    im, d = new(UW, 20); recess(d, (0, 0, UW-1, 19)); rect(d, (1, 1, UW-2, 18), 'leather1' if st == 'gotowa' else 'ink')
    if st == 'gotowa':
        for b_ in ((0, 0, UW-1, 0), (0, 19, UW-1, 19), (0, 0, 0, 19), (UW-1, 0, UW-1, 19)): rect(d, b_, 'gold1')
    save(im, f'um_{st}')
um = []
for n in (1, 2, 3, 4):
    for st in ('gotowa', 'odnowienie'):
        warstwy = [('nr', txt(f'<#D5A96B>{n}', 2, 2, 'right')),
                   ('skrot', txt(('<#FFF1CC>' if st == 'gotowa' else '<#8B7F86>') + f'%hud_umiejetnosc_{n}_skrot%', 2, 11, 'right'))]
        if st == 'odnowienie': warstwy.append(('cd', txt(f'<#F36D62>%hud_umiejetnosc_{n}_cd%', 12, 2, 'right')))
        warstwy.append(('tlo', tex(f'um_{st}', 0, 0)))
        um.append(asset(f'l5-um{n}-{st}', [SPEC, ZYJE, f'- placeholder{{ph=%hud_umiejetnosc_{n}_stan%;val={st}}} true hide'], warstwy))

(OUT/'hud_assets/hud/l5.yml').write_text('# HUD v5 (20.10): kierunek obrazen, medal przed awansem, kafelki umiejetnosci.\n' + '\n'.join(ob + aw + um))
lay5 = 'l5-layout:\n  elements:\n'
lay5 += el('obrazenia', 'l5-obrazenia', 50.0, 50.0, -15, 15)
lay5 += el('awans', 'l5-awans', 50.0, 0.0, -14, 53)
# Kafelki 2x2 na prawo od hotbara (16.11, wlasciciel: "co to za kafel 1 sierp na srodku ekranu"): 1 2 / 3 4, x +97..+171
# (miesci sie przy GUI 3 na 1280 px: pol ekranu = 213), dol kafla 3/4 na wysokosci dolu hotbara.
for n in (1, 2, 3, 4):
    for st in ('gotowa', 'odnowienie'):
        lay5 += el(f'um{n}-{st}', f'l5-um{n}-{st}', 50.0, 0.0, 97 + ((n-1) % 2)*38, 43 if n <= 2 else 21)
(OUT/'layouts/l5.yml').write_text('# HUD v5: kierunek obrazen, awans, umiejetnosci.\n' + lay5)

# ---------------- PAS: wszystkie assety znikaja na ekranie smierci (napisy tez) ----------------
_p = OUT/'hud_assets/hud/l1-pas.yml'; _s = _p.read_text()
_s = _re.sub(r'(?m)^  conditions:\n(?!    - placeholder\{ph=%player_health%)', '  conditions:\n    ' + ZYJE + '\n', _s)
_p.write_text(_s)

# ---------------- TRYB MINIMALNY ----------------
MIN = '- placeholder{ph=%luckperms_meta_ui_hud%;val=minimal} false hide'
for f_ in ('hud_assets/hud/l1-okno.yml', 'hud_assets/hud/l3-kolumna.yml', 'hud_assets/hud/cel.yml', 'hud_assets/hud/l4.yml'):
    p_ = OUT/f_; s_ = p_.read_text()
    if f_.endswith('l4.yml'):  # w l4 tylko loch i zdobycz znikaja; cel walki i ostrzezenia zostaja
        s_ = _re.sub(r'(?m)^(l4-(loch|zdobycz)[^:\n]*:\n  conditions:\n)', lambda m_: m_.group(1) + '    ' + MIN + '\n', s_)
    else:
        s_ = _re.sub(r'(?m)^  conditions:\n', '  conditions:\n    ' + MIN + '\n', s_)
    p_.write_text(s_)

_za_duze = [f.name for f in TEX.glob('*.png') if max(_I.open(f).size) > 256]
assert not _za_duze, f'tekstury > 256 px: {_za_duze}'
print('v5:', len(zad + ob + aw + um), 'assetow')
