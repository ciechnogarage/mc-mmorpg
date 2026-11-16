# HUD v4 (20.10), po v3.py: cel walki (srodek nad paskiem), panel lochu (pod celem zadania), kanal zdobyczy (prawo
# od hotbara), ostrzezenia (zuzycie broni, pelny ekwipunek). Dane z HudDane (%hud_cel_*, zdobycz_*, ostrzezenie, loch%).
# Zasady z pomiarow 20.10: teksty tylko align right, kolor w YAML (nie w placeholderze), tekstury <= 256 px w 2x.

# ---------------- CEL WALKI ----------------
CW = 130
for ranga, rama in (('zwykly', 'leather2'), ('boss', 'gold1')):
    im, d = new(CW+1, 22); panel(d, (0, 0, CW, 21))
    if ranga == 'boss':
        line(d, [(4, 1), (CW-4, 1)], 'gold2'); line(d, [(4, 20), (CW-4, 20)], 'gold1')
    save(im.crop((0, 0, 100, 22)), f'cw_{ranga}_a'); save(im.crop((100, 0, CW+1, 22)), f'cw_{ranga}_b')
im, d = new(CW-9, 5); recess(d, (0, 0, CW-10, 4)); save(im, 'cw_hp_tlo')
im, d = new(CW-11, 3); rect(d, (0, 0, CW-12, 2), 'red'); line(d, [(0, 0), (CW-12, 0)], 'redhi'); save(im, 'cw_hp')
CW_WSP = [SPEC, ZYJE, WALKA_CHW, '- placeholder{ph=%hud_cel_jest%;val=tak} true hide']
cw = []
# Boss MythicMobs ma wlasny bossbar u gory - ramki celu dla niego nie pokazujemy.
for ranga, war in (('zwykly', '- placeholder{ph=%hud_cel_ranga%;val=boss} false hide'),):
    kol_n = '<#FFE7AE>' if ranga == 'boss' else '<#FFF1CC>'
    cw.append(asset(f'l4-cel-walki-{ranga}', CW_WSP + [war], [
      ('nazwa', txt(kol_n + '%hud_cel_nazwa% <#D5A96B>%hud_cel_poziom%', 6, 5, 'right')),
      ('hp-tekst', txt('<#FFF1CC>%hud_cel_hp%/%hud_cel_hpmax%', 90, 5, 'right')),
      ('hp', tex('cw_hp', 6, 15, fn('cel_walki_hp', CW-11))),
      ('hp-tlo', tex('cw_hp_tlo', 5, 14)),
      ('tlo-b', tex(f'cw_{ranga}_b', 100, 0)),
      ('tlo', tex(f'cw_{ranga}_a', 0, 0)),
    ]))

# ---------------- LOCH ----------------
LOCH_WSP = [SPEC, ZYJE, '- placeholder{ph=%hud_loch%;val=tak} true hide']
save(ik_(lambda d: (rect(d, (1, 2, 7, 8), 'leather2'), rect(d, (2, 0, 6, 2), 'leather2'), rect(d, (3, 4, 5, 8), 'ink'), rect(d, (4, 5, 4, 5), 'gold1'))), 'loch_ikona')
save(ik_(lambda d: (d.ellipse((1, 1, 7, 7), outline=c('gold1')), rect(d, (4, 2, 4, 4), 'gold2'), rect(d, (4, 4, 6, 4), 'gold2'))), 'loch_czas')
save(ik_(lambda d: (rect(d, (1, 1, 3, 3), 'red'), rect(d, (5, 1, 7, 3), 'red'), rect(d, (1, 2, 7, 5), 'red'), rect(d, (2, 6, 6, 6), 'red'), rect(d, (4, 7, 4, 7), 'red'), rect(d, (2, 2, 2, 2), 'redhi'))), 'loch_zycia')
loch = [asset('l4-loch', LOCH_WSP, [
  ('nazwa', txt('<#FFE7AE>%md_dungeon_display_name%', 18, 5, 'right')),
  ('czas', txt('<#FFF1CC>%md_dungeon_time_left%', 30, 17, 'right')),
  ('zycia', txt('<#F36D62>%md_dungeon_lives_left%', 90, 17, 'right')),
  ('ik-czas', tex('loch_czas', 18, 16)),
  ('ik-zycia', tex('loch_zycia', 78, 16)),
  ('ikona', tex('loch_ikona', 6, 4)),
  ('tlo-b', tex('cel_b', 115, 0)),
  ('tlo', tex('cel_a', 0, 0)),
])]

# ---------------- ZDOBYCZ ----------------
KOL_Z = {'exp': '<#B8E4FF>', 'profesja': '<#9BD07A>', 'przedmiot': '<#FFE7AE>'}
save(ik_(lambda d: (rect(d, (3, 0, 5, 8), 'blue'), rect(d, (0, 3, 8, 5), 'blue'), rect(d, (4, 1, 4, 7), 'cream'))), 'z_exp')
save(ik_(lambda d: (rect(d, (1, 5, 7, 7), 'leather2'), rect(d, (5, 0, 6, 5), 'gold0'), rect(d, (2, 1, 7, 2), 'gold1'))), 'z_profesja')
save(ik_(lambda d: (rect(d, (2, 3, 6, 8), 'gold0'), rect(d, (3, 4, 5, 7), 'leather2'), rect(d, (3, 1, 5, 2), 'cream'))), 'z_przedmiot')
zd = []
for n in (1, 2, 3, 4):
    for r_, kol in KOL_Z.items():
        zd.append(asset(f'l4-zdobycz{n}-{r_}', [SPEC, ZYJE, f'- placeholder{{ph=%hud_zdobycz_{n}_rodzaj%;val={r_}}} true hide'], [
          ('t', txt(kol + f'%hud_zdobycz_{n}_tekst%', 11, 1, 'right')),
          ('ik', tex(f'z_{r_}', 0, 0)),
        ]))

# ---------------- OSTRZEZENIA ----------------
save(ik_(lambda d: (d.polygon([(4, 0), (8, 8), (0, 8)], fill=c('gold1')), rect(d, (4, 3, 4, 5), 'ink'), rect(d, (4, 7, 4, 7), 'ink'))), 'ostrz')
oz = []
for id_, tekst, kol in (('zuzycie', 'Broń prawie zniszczona', '<#F36D62>'), ('pelny', 'Pełny ekwipunek', '<#FFE7AE>')):
    oz.append(asset(f'l4-ostrz-{id_}', [SPEC, ZYJE, f'- placeholder{{ph=%hud_ostrzezenie%;val={id_}}} true hide'], [
      ('t', txt(kol + tekst, 11, 1, 'right')),
      ('ik', tex('ostrz', 0, 0)),
    ]))

(OUT/'hud_assets/hud/l4.yml').write_text('# HUD v4 (20.10): cel walki, loch, zdobycz, ostrzezenia. Dane: HudDane, MythicDungeons.\n' + '\n'.join(cw + loch + zd + oz))
_l = open(OUT/'listeners/l3.yml').read() + "cel_walki_hp:\n  value: '%hud_cel_hp%'\n  max: '%hud_cel_hpmax%'\n"
(OUT/'listeners/l3.yml').write_text(_l)
lay4 = 'l4-layout:\n  elements:\n'
# cel walki: srodek, gorna krawedz 130 GUI nad dolem (nad nazwami zaklec 104 i action barem 68)
for r_ in ('zwykly',): lay4 += el(f"cel-walki-{r_}", f"l4-cel-walki-{r_}", 50.0, 0.0, -65, 130)
# loch: pod celem zadania (position y 70 przy mapie), 32 GUI nizej
lay4 += el('loch', 'l4-loch', 100.0, 70.0, -234, -32)
# zdobycz: na prawo od hotbara, najnowsza na dole; 10 GUI na wiersz
for n in (1, 2, 3, 4):
    for r_ in KOL_Z: lay4 += el(f'zdobycz{n}-{r_}', f'l4-zdobycz{n}-{r_}', 50.0, 0.0, 140, 34 + (n-1)*10)
# ostrzezenia: nad lewym koncem pasa (pas zaczyna sie 51 GUI nad dolem)
for id_ in ('zuzycie', 'pelny'): lay4 += el(f'ostrz-{id_}', f'l4-ostrz-{id_}', 50.0, 0.0, -91, 64)
(OUT/'layouts/l4.yml').write_text('# HUD v4: cel walki, loch, zdobycz, ostrzezenia.\n' + lay4)
_za_duze = [f.name for f in TEX.glob('*.png') if max(_I.open(f).size) > 256]
assert not _za_duze, f'tekstury > 256 px: {_za_duze}'
print('v4:', len(cw + loch + zd + oz), 'assetow')
