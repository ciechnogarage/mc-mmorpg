#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Strona przegladu systemu przedmiotow z katalog.json + symulacja.json + sonda.json (lore z gry) + wyniki evali.
Uzycie: strona.py <katalog.json> <symulacja.json> <sonda.json> <evale.json> <wyjscie.html>"""
import html, json, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tresc import ARCH_PL, SLOTY_ZB, AKC_TYPY, AKC_PL, WAGA_PL, RODZAJE, BRON_ARCH, RASA_RODZAJU, AFFIKSY, PULE, TIERY_AFF, JAKOSC_MAX, KAMIENIE, LUP, LUP_SET, POZIOMY, WYM_ATR, ATRYBUT_PL, ATRYBUT

kat, sym, sonda, evale, out = [json.load(open(p)) for p in sys.argv[1:5]] + [sys.argv[5]]
e = html.escape
MC = {'black': '#000', 'dark_blue': '#00a', 'dark_green': '#0a0', 'dark_aqua': '#0aa', 'dark_red': '#a00', 'dark_purple': '#a0a', 'gold': '#fa0',
      'gray': '#aaa', 'dark_gray': '#555', 'blue': '#55f', 'green': '#5f5', 'aqua': '#5ff', 'red': '#f55', 'light_purple': '#f5f', 'yellow': '#ff5', 'white': '#fff'}
ARCH_KL = {'woj': 'woj', 'nin': 'nin', 'mag': 'mag', 'ako': 'ako'}

def v(n):
    return n['value'] if isinstance(n, dict) and 'value' in n and 'type' in n else n

def spany(comp):
    c = v(comp)
    if isinstance(c, str): return e(c)
    ext = v(v(c.get('extra'))) if c.get('extra') else []
    if isinstance(ext, dict): ext = v(ext)
    s = ''
    for el in (ext or []):
        t = str(v(el.get('text', ''))); kol = MC.get(str(v(el.get('color', 'white'))), '#fff')
        b = ' style="color:%s%s"' % (kol, ';font-weight:700' if v(el.get('bold', 0)) == 1 else '')
        s += f'<span{b}>{e(t)}</span>'
    return s

def tooltip(x):
    comps = {c['type']: c['data'] for c in (x.get('surowe') or [])}
    if 'custom_name' not in comps: return ''
    nazwa = spany(comps['custom_name'])
    lore = ''.join(f'<div class="tl">{spany(l) or "&nbsp;"}</div>' for l in comps.get('lore', []))
    podpis = f"{x['id']} · ilvl {x['poz']}"
    return f'<figure class="tt"><div class="tn">{nazwa}</div>{lore}<figcaption>{e(podpis)}</figcaption></figure>'

prz = kat['przedmioty']
# ---- sekcje
def rarity():
    wiersze = ''
    for r in kat['rarity']:
        x = sym.get(r['id'])
        opis = {'ZWYKLY': 'sama baza', 'MAGICZNY': '1–2 affiksy', 'RZADKI': '3–4 affiksy', 'EPICKI': '4–5 affiksów, lepsze tiery',
                'LEGENDARNY': '5–6 affiksów, głównie T2–T3', 'UNIKATOWY': 'nazwany przedmiot z umiejętnością (osobna gałąź)',
                'ZESTAW': 'część setu, stałe statystyki (osobna gałąź)'}[r['id']]
        tiery = ' · '.join(f"{k} {v*100:.0f}%" for k, v in (x or {}).get('tiery', {}).items()) or '—'
        udz = f"{r['udzial']*100:.1f}%" if r['udzial'] else ('z bossów' if r['id'] in ('UNIKATOWY', 'ZESTAW') else '—')
        wiersze += f"<tr><td><span class='rar r-{r['id'].lower()}'>{e(r['nazwa'])}</span></td><td>{opis}</td><td class='n'>{udz}</td><td>{tiery}</td></tr>"
    return wiersze

def affiksy_tab():
    rows = ''
    for kl, (typ, formy, stat, wart, sc) in AFFIKSY.items():
        naz = formy[0] if typ == 'P' else '… ' + formy
        arche = [ARCH_PL[a] for a, p in PULE.items() if any(kl in r for r in p.values())]
        rows += f"<tr><td><b>{e(naz)}</b></td><td>{'prefiks' if typ == 'P' else 'sufiks'}</td><td><code>{e(stat)}</code></td><td class='n'>{wart[0]} / {wart[1]} / {wart[2]}</td><td>{', '.join(arche)}</td></tr>"
    return rows

def bazy(arch):
    zb = ''
    for sid, _, _ in SLOTY_ZB:
        kom = [x for x in prz if x['arch'] == arch and x['slot'] == sid]
        zb += f"<tr><th scope='row'>{ {'HELM':'głowa','KLATA':'klata','NOGI':'nogi','BUTY':'stopy'}[sid]}</th>" + ''.join(f"<td>{e(x['nazwa'])}<small>{WAGA_PL[x['waga']]}</small></td>" for x in kom) + '</tr>'
    wsp, ras = BRON_ARCH[arch]
    br = ''
    for r in wsp + ras:
        kom = [x for x in prz if x['arch'] == arch and x.get('rodzaj') == r]
        lab = RODZAJE[r][7] + (f"<small>{RASA_RODZAJU[r]}</small>" if r in RASA_RODZAJU else '<small>wspólna</small>')
        br += f"<tr><th scope='row'>{lab}</th>" + ''.join(f"<td>{e(x['nazwa'])}{'<small>2H</small>' if x.get('dwureczna') else ''}</td>" for x in kom) + '</tr>'
    ak = ''
    for t in AKC_TYPY:
        kom = [x for x in prz if x['arch'] == arch and x['slot'] == t]
        ak += f"<tr><th scope='row'>{AKC_PL[t]}</th>" + ''.join(f"<td>{e(x['nazwa'])}</td>" for x in kom) + '</tr>'
    atr = ATRYBUT_PL[ATRYBUT[arch]]
    glowa = ''.join(f"<th>poz. {p}{f'<small>{w} {atr}</small>' if w else ''}</th>" for p, w in zip(POZIOMY, WYM_ATR))
    return f"""<div class="tbl"><table class="bazy"><thead><tr><th>zbroja</th>{glowa}</tr></thead><tbody>{zb}</tbody>
<thead><tr><th>broń</th>{glowa}</tr></thead><tbody>{br}</tbody>
<thead><tr><th>ozdoby</th>{glowa}</tr></thead><tbody>{ak}</tbody></table></div>"""

def sety():
    karty = ''
    for s in kat['sety']:
        progi = ''.join(f"<li><b>{k} cz.</b> {e(t)}</li>" for k, t in s['progi'].items())
        cz = ', '.join(e(c['nazwa']) for c in s['czesci'])
        karty += f"""<article class="set {ARCH_KL[s['arch']]}"><header><span class="arch {ARCH_KL[s['arch']]}">{ARCH_PL[s['arch']]}</span><span class="poz">poz. {s['poziom']} · {len(s['czesci'])} cz.</span></header>
<h3>{e(s['nazwa'])}</h3><p class="cz">{cz}</p><ul>{progi}</ul><p class="doc"><span>Docelowo, po zasobach klas:</span> {e(s['docelowo'])}</p></article>"""
    return karty

def evaltab():
    r = ''
    for x in evale:
        r += f"<tr><td><span class='pill {'ok' if x['pass'] else 'bad'}'>{'PASS' if x['pass'] else 'FAIL'}</span></td><td>{e(x['label'])}</td><td class='det'>{e(x.get('extra') or '')}</td></tr>"
    return r

n_bron = sum(1 for x in prz if x['kat'] == 'bron'); n_zb = sum(1 for x in prz if x['kat'] == 'zbroja'); n_oz = sum(1 for x in prz if x['kat'] == 'ozdoba')
n_cz = sum(len(s['czesci']) for s in kat['sety'])
tt = ''.join(tooltip(x) for x in sonda)
import base64
RAMKI = base64.b64encode(open(os.environ['RAMKI_JPG'], 'rb').read()).decode() if os.environ.get('RAMKI_JPG') else ''
QA = []
if os.environ.get('QA_DIR'):
    from PIL import Image
    import io
    for f, podpis in [('zestaw.png', 'Część setu: zielona podwójna ramka, ❖ w nazwie, „Część zestawu” i blok setu na górze'),
                      ('rzadki.png', 'Rzadki: żółta ramka, affiksy z tierem ◆◇◇ i nazwą statystyki, wymagania na dole'),
                      ('unikat.png', 'Unikat: czerwona ramka, umiejętność ✦ i linia fabuły')]:
        im = Image.open(os.path.join(os.environ['QA_DIR'], f)).convert('RGB'); b = io.BytesIO(); im.save(b, 'JPEG', quality=80)
        QA.append((base64.b64encode(b.getvalue()).decode(), podpis))
QA_HTML = ''.join(f'<figure style="margin:0"><img src="data:image/jpeg;base64,{d}" alt="{e(pd)}" style="width:100%;border-radius:4px"><figcaption style="font-size:.85rem;color:var(--muted);margin-top:.3rem">{e(pd)}</figcaption></figure>' for d, pd in QA)
lup_rows = ''.join(f"<tr><td><b>{p}</b></td><td>{', '.join(map(str, c['bazy']))}</td><td class='n'>{c['szansa']*100:.0f}%</td><td class='n'>{c['ile']}</td></tr>" for p, c in LUP.items())
pass_n = sum(1 for x in evale if x['pass'])

H = f"""<title>Ekwipunek Aktu 1</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Alegreya+SC:wght@500;700&family=Alegreya+Sans:ital,wght@0,400;0,500;0,700;1,400&family=JetBrains+Mono:wght@400;500&display=swap">
<style>
/* Uklad: kolumna czytania ~72ch; tabele baz i tooltipy szerzej, w przewijanych ramkach. Rodzina stron planu klas: te same kroje i kolory archetypow. */
:root{{
  --bg:#f3f4f2; --panel:#fff; --fg:#1d2226; --muted:#5b646b; --line:#d6dad8; --accent:#2f5d62; --accent-soft:#e2ecec;
  --woj:#b3372f; --nin:#5c6670; --mag:#8a3fa0; --ako:#a8790f;
  --r-zwykly:#6b7075; --r-magiczny:#3552c9; --r-rzadki:#a08400; --r-unikatowy:#c62f2f; --r-zestaw:#23843c; --r-epicki:#a13cb8; --r-legendarny:#c26a00;
  --ok:#2e7d4f; --bad:#b3372f; --tt-bg:#140a1c; --tt-edge:#3d1a78; --tt-fg:#e8e3ee;
  --display:"Alegreya SC", Georgia, serif; --body:"Alegreya Sans","Segoe UI",system-ui,sans-serif; --mono:"JetBrains Mono",ui-monospace,Consolas,monospace;
}}
@media (prefers-color-scheme: dark){{:root:not([data-theme="light"]){{
  --bg:#15191c; --panel:#1d2226; --fg:#e6e9e7; --muted:#9aa4aa; --line:#323a3f; --accent:#7fb6ba; --accent-soft:#223236;
  --woj:#e0675e; --nin:#a4afb8; --mag:#c98adb; --ako:#e0b54a;
  --r-zwykly:#c9cdd1; --r-magiczny:#8ea2ff; --r-rzadki:#f0d64a; --r-unikatowy:#ff6b6b; --r-zestaw:#6fd98b; --r-epicki:#ec7cff; --r-legendarny:#ffab45;
  --ok:#6cc392; --bad:#e0675e; --tt-bg:#100812; --tt-edge:#4b2596; --tt-fg:#e8e3ee; color-scheme:dark}}}}
:root[data-theme="dark"]{{
  --bg:#15191c; --panel:#1d2226; --fg:#e6e9e7; --muted:#9aa4aa; --line:#323a3f; --accent:#7fb6ba; --accent-soft:#223236;
  --woj:#e0675e; --nin:#a4afb8; --mag:#c98adb; --ako:#e0b54a;
  --r-zwykly:#c9cdd1; --r-magiczny:#8ea2ff; --r-rzadki:#f0d64a; --r-unikatowy:#ff6b6b; --r-zestaw:#6fd98b; --r-epicki:#ec7cff; --r-legendarny:#ffab45;
  --ok:#6cc392; --bad:#e0675e; --tt-bg:#100812; --tt-edge:#4b2596; --tt-fg:#e8e3ee; color-scheme:dark}}
*{{box-sizing:border-box}}
body{{background:var(--bg);color:var(--fg);font:17px/1.55 var(--body);margin:0}}
.wrap{{max-width:1040px;margin:0 auto;padding-inline:20px;padding-block:32px 80px}}
.col{{max-width:72ch}}
h1,h2,h3{{font-family:var(--display);font-weight:700;text-wrap:balance;line-height:1.15;margin:0}}
h1{{font-size:2.3rem}} h2{{font-size:1.55rem;margin-top:3rem;padding-top:1.2rem;border-top:1px solid var(--line)}} h3{{font-size:1.15rem}}
p,ul{{margin:.7rem 0}} li{{margin:.25rem 0}}
.lead{{font-size:1.12rem;color:var(--muted)}}
.eyebrow{{font:500 .78rem/1 var(--mono);letter-spacing:.08em;text-transform:uppercase;color:var(--accent)}}
code{{font-family:var(--mono);font-size:.82em;background:var(--accent-soft);padding:.05em .35em;border-radius:3px}}
nav.toc{{display:flex;flex-wrap:wrap;gap:.4rem .9rem;margin-top:1.2rem;font-size:.95rem}} nav.toc a{{color:var(--accent);text-decoration:none;border-bottom:1px solid var(--line)}}
.liczby{{display:flex;flex-wrap:wrap;gap:.5rem 1.6rem;margin:1.4rem 0 0;font-size:.95rem;color:var(--muted)}} .liczby b{{font:700 1.35rem var(--display);color:var(--fg);margin-right:.3rem}}
.tbl{{overflow-x:auto;margin:1rem 0;border:1px solid var(--line);border-radius:6px;background:var(--panel)}}
table{{border-collapse:collapse;width:100%;font-size:.92rem;font-variant-numeric:tabular-nums}}
th,td{{text-align:left;vertical-align:top;padding:.5rem .65rem;border-bottom:1px solid var(--line)}}
thead th{{font-size:.76rem;letter-spacing:.05em;text-transform:uppercase;color:var(--muted);font-weight:700;background:var(--accent-soft)}}
td.n{{text-align:right;white-space:nowrap}} td small,th small{{display:block;color:var(--muted);font-size:.78rem;text-transform:none;letter-spacing:0;font-weight:400}}
table.bazy td{{min-width:9.5rem}} table.bazy tbody th{{font-weight:500;white-space:nowrap}}
.rar{{font-weight:700}} .r-zwykly{{color:var(--r-zwykly)}} .r-magiczny{{color:var(--r-magiczny)}} .r-rzadki{{color:var(--r-rzadki)}} .r-unikatowy{{color:var(--r-unikatowy)}} .r-zestaw{{color:var(--r-zestaw)}} .r-epicki{{color:var(--r-epicki)}} .r-legendarny{{color:var(--r-legendarny)}}
.arch{{display:inline-flex;align-items:center;gap:.4rem;font-weight:700;white-space:nowrap}} .arch::before{{content:"";width:.65rem;height:.65rem;border-radius:2px;background:var(--c)}}
.woj{{--c:var(--woj)}} .nin{{--c:var(--nin)}} .mag{{--c:var(--mag)}} .ako{{--c:var(--ako)}}
.tts{{display:grid;grid-template-columns:repeat(auto-fill,minmax(270px,1fr));gap:14px;margin:1.2rem 0;align-items:start}}
.tt{{margin:0;background:var(--tt-bg);color:var(--tt-fg);border:2px solid var(--tt-edge);outline:2px solid var(--tt-bg);border-radius:3px;padding:8px 10px;font:13px/1.35 var(--mono);min-width:0}}
.tt .tn{{font-size:14px;margin-bottom:4px}} .tt .tl{{white-space:pre-wrap;word-break:break-word}}
.tt figcaption{{margin-top:8px;padding-top:6px;border-top:1px solid var(--tt-edge);color:#9d93ad;font-size:11px}}
.sety{{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:14px;margin:1rem 0}}
.set{{background:var(--panel);border:1px solid var(--line);border-top:4px solid var(--c);border-radius:6px;padding:14px 16px;min-width:0}}
.set header{{display:flex;justify-content:space-between;gap:.5rem;font-size:.85rem}} .set .poz{{color:var(--muted);font-family:var(--mono);font-size:.78rem}}
.set h3{{margin:.4rem 0 .2rem;color:var(--r-zestaw)}} .set .cz{{font-size:.86rem;color:var(--muted);margin:.2rem 0 .5rem}}
.set ul{{padding-left:1.1rem;font-size:.95rem}} .set .doc{{font-size:.86rem;color:var(--muted);border-top:1px dashed var(--line);padding-top:.5rem;margin-bottom:0}} .set .doc span{{font-weight:700}}
details.ar{{margin:.8rem 0;border:1px solid var(--line);border-radius:6px;background:var(--panel);padding:.2rem .9rem}}
details.ar summary{{cursor:pointer;padding:.6rem 0;font:700 1.1rem var(--display)}} details.ar summary:focus-visible{{outline:2px solid var(--accent);outline-offset:3px}}
.pill{{font:500 .72rem/1 var(--mono);padding:.3em .55em;border-radius:99px;border:1px solid currentColor;white-space:nowrap}} .pill.ok{{color:var(--ok)}} .pill.bad{{color:var(--bad)}}
td.det{{font-family:var(--mono);font-size:.78rem;color:var(--muted);word-break:break-word}}
.callout{{background:var(--accent-soft);border-radius:6px;padding:14px 18px;margin:1.2rem 0}}
.dec{{list-style:none;padding:0;counter-reset:d}} .dec li{{counter-increment:d;background:var(--panel);border:1px solid var(--line);border-radius:6px;padding:12px 16px 12px 3.2rem;position:relative;margin:.6rem 0}}
.dec li::before{{content:"P" counter(d);position:absolute;left:14px;top:12px;font:700 .95rem var(--mono);color:var(--accent)}}
@media (max-width:560px){{body{{font-size:16px}} h1{{font-size:1.8rem}}}}
</style>
<div class="wrap">
<header class="col">
  <div class="eyebrow">Etap 6 planu klas · wersja 2 wdrożona na world1 30.09 17:40 · łup pod klasę włączony</div>
  <h1>Ekwipunek Aktu 1</h1>
  <p class="lead"><b>Wersja 2 po krytyce:</b> pięć rzadkości, unikaty, nowy układ tooltipu, ramki w kolorze rzadkości, sety z ❖, rozbiórka. Każda nadklasa dostaje po 5 baz na każdy slot, 4 bronie rasowe i wspólną, 2 sety z progami, 5 ozdób na slot. Przedmiot wypada z poziomem, rzadkością i losowymi affiksami jak w PoE, a jakość podnosi się materiałami z profesji.</p>
  <div class="liczby"><span><b>{n_zb}</b>baz zbroi</span><span><b>{n_bron}</b>baz broni</span><span><b>{n_oz}</b>baz ozdób</span><span><b>{len(kat['sety'])}</b>setów ({n_cz} części)</span><span><b>{len(AFFIKSY)}</b>rodzin affiksów</span><span><b>{pass_n}/{len(evale)}</b>evali gracza</span></div>
  <nav class="toc" aria-label="Spis"><a href="#qa">W kliencie</a><a href="#wyglad">Jak to wygląda</a><a href="#rzadkosc">Rzadkość</a><a href="#affiksy">Affiksy</a><a href="#jakosc">Jakość</a><a href="#sety">Sety</a><a href="#bazy">Bazy</a><a href="#lup">Łup</a><a href="#evale">Evale</a><a href="#pomijane">Czego brakowało</a><a href="#krytyka">Krytyka</a><a href="#dalej">Co dalej</a></nav>
</header>

<section id="wyglad"><div class="col"><h2>Jak to wygląda w grze</h2>
<p>Tooltipy przedmiotów, które bot dostał na klonie 30.09 (przed ostatnią drobną poprawką tekstów setu). Kolejność od zwykłego do unikatu. Kolory i tekst pochodzą z pakietu wysłanego do klienta, nie z makiety. Nazwa ma kolor rzadkości, prefiks i sufiks pochodzą z affiksów, a pod opisem każdy affiks pokazuje swój tier.</p></div>
<div class="tts">{tt}</div>' + (f'<figure class="col" style="margin:1rem 0"><img src="data:image/jpeg;base64,{RAMKI}" alt="Ramki tooltipu: magiczny, rzadki, epicki, legendarny, unikatowy, zestaw" style="width:100%;image-rendering:pixelated;border-radius:4px"><figcaption style="font-size:.85rem;color:var(--muted)">Ramki tooltipu z packa (od lewej): magiczny, rzadki, epicki, legendarny, unikatowy, zestaw. Wzorzec: ramka vanilli 1.21.11. Zwykły zostaje przy ramce vanilli. Obejrzane w prawdziwym kliencie (sekcja „W prawdziwym kliencie”).</figcaption></figure>' if RAMKI else '') + f'</section>

<section id="qa"><div class="col"><h2>W prawdziwym kliencie</h2><p>Zrzuty z Minecrafta 1.21.11 z packiem ramek, na klonie world1, 30.09. Tak zobaczy to gracz.</p></div><div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:14px;margin:1rem 0">' + QA_HTML + f'</div></section>

<section id="rzadkosc"><div class="col"><h2>Rzadkość</h2>
<p>Set nie jest rzadkością w sensie mocy: części setu mają stałe statystyki i zieloną nazwę, jak w Diablo 2. Pojemność decyduje, ile affiksów się zmieści. Rośnie z poziomem przedmiotu, dlatego wysokie tiery affiksów wypadają częściej na wyższych poziomach. Średnie liczby affiksów pochodzą z symulatora algorytmu MMOItems (5000 przedmiotów na komórkę).</p></div>
<div class="tbl"><table><thead><tr><th>Rzadkość</th><th>Co daje</th><th>Udział w dropie</th><th>Tiery affiksów</th></tr></thead><tbody>{rarity()}</tbody></table></div></section>

<section id="affiksy"><div class="col"><h2>Affiksy</h2>
<p>Prefiksy to atak i zasób, sufiksy to obrona i użyteczność, tak jak w PoE. Najwyżej 2 prefiksy i 2 sufiksy. Każda rodzina występuje raz, więc nie ma dwóch „+życie” na jednym przedmiocie. Tiery: <b>T1</b> zwykły, <b>T2</b> mocniejszy, <b>T3</b> do polowania (ok. 6% affiksów). Przymiotnik prefiksu zgadza się z rodzajem bazy: Okrutny miecz, Okrutna szabla, Okrutne ostrze. Statystyki many dostaje tylko pula maga, bo mana to też Szał i Dusze innych klas.</p></div>
<div class="tbl"><table><thead><tr><th>Affiks</th><th>Rodzaj</th><th>Statystyka</th><th>T1 / T2 / T3 (poz. 1)</th><th>Pule</th></tr></thead><tbody>{affiksy_tab()}</tbody></table></div></section>

<section id="jakosc" class="col"><h2>Jakość</h2>
<p>Każdy przedmiot ma jakość od 0 do {JAKOSC_MAX}%. Jeden kamień daje +1% do wszystkich statystyk przedmiotu, bez ryzyka zniszczenia. Nazwa pokazuje wtedy dopisek <code>[Jakość +3%]</code>. Kamienie robi się w warsztatach profesji z naszych surowców:</p>
<ul>{''.join(f"<li><b>{e(k['nazwa'])}</b> ({ {'bron':'broń','zbroja':'zbroja','ozdoba':'ozdoby'}[k['ref']]}): {k['stacja'].replace('profession-','warsztat ')}, z {', '.join(f'{n}× {i}' for i, n in k['skladniki'])}</li>" for k in kat['kamienie'])}</ul>
<p>Na dropie jakość jest zawsze 0%. MMOItems nie losuje poziomu ulepszenia przy generowaniu; losowa jakość na łupie wymaga małego pluginu (punkt w „Co dalej”).</p></section>

<section id="sety"><div class="col"><h2>Sety</h2>
<p>Dwa na nadklasę. Pierwszy to 4 części zbroi z poziomu 5 (progi 2 i 4). Drugi to regalia z poziomu 10: broń wspólna i 4 ozdoby (progi 2, 3 i 5). Oba da się nosić naraz. Bonusy działają już dziś na statystykach i umiejętnościach MythicLib. Wersja docelowa pod Szał, Znamiona, Manę i Dusze wejdzie, gdy powstaną zasoby klas.</p></div>
<div class="sety">{sety()}</div></section>

<section id="bazy"><div class="col"><h2>Bazy</h2>
<p>Pięć baz na slot, od poziomu 1 do 10. Wyższa baza ma trochę więcej statystyk przy tym samym poziomie przedmiotu i wymaga głównego atrybutu nadklasy (od bazy z poziomu 5). Broń rasowa jest dostępna tylko rasie z biegłością. Wszystkie statystyki rosną z poziomem przedmiotu.</p></div>
{''.join(f'<details class="ar" {"open" if i == 0 else ""}><summary><span class="arch {a}">{ARCH_PL[a]}</span></summary>{bazy(a)}</details>' for i, a in enumerate(['woj', 'nin', 'mag', 'ako']))}
</section>

<section id="lup"><div class="col"><h2>Łup</h2>
<p>Każda tabela łupu Bestiariusza ma jedną dodatkową linię. Przedmiot dostaje poziom zabójcy ±2, a rzadkość losuje się z udziałów powyżej. Bazy zależą od tieru moba, żeby na poziomie 1 nie wypadała baza z poziomu 10. Bossy dają 2 przedmioty i szansę na część setu: poz. 5 {LUP_SET['A']*100:.0f}%, poz. 10 {LUP_SET['B']*100:.0f}%.</p></div>
<div class="tbl"><table><thead><tr><th>Mob</th><th>Bazy z poziomów</th><th>Szansa na przedmiot</th><th>Ile</th></tr></thead><tbody>{lup_rows}</tbody></table></div>
<p class="col">Łup pod klasę (75% dropu pod nadklasę gracza, jak w Diablo 3) jest gotowy w generatorze. Czeka na placeholder archetypu od generatora klas; do tego czasu łup jest wspólny dla wszystkich.</p></section>

<section id="evale"><div class="col"><h2>Evale z perspektywy gracza</h2>
<p class="callout"><b>Wersja 2 — tabela niżej to pełny komplet evali wersji 2 na klonie z aktualnym world1 (nowy wzór obrony, archetyp).</b> Dodatkowo, sondą: 600 mieczy bez wymuszania rzadkości dało 59,3 / 29,7 / 7,8 / 2,3 / 0,8% (tabela 60 / 28 / 9 / 2,5 / 0,5); liczba affiksów w przedziale każdej rzadkości (400 przedmiotów, 10/10 PASS); efekt legendarny i duplikaty 0 błędów; Epicki głównie T2, Legendarny T2–T3.</p>
<p>Bot bez OP na klonie. Konsola tylko nadaje klasę, poziom i punkty atrybutów oraz wydaje przedmiot. Zakładanie zbroi, przeciąganie kamienia i sloty ozdób robi bot. Maszyna była przy tym przeciążona (load ~70), więc część porażek to wyścigi w evalu, co opisuje sekcja niżej.</p></div>
<div class="tbl"><table><thead><tr><th></th><th>Sprawdzenie</th><th>Szczegóły</th></tr></thead><tbody>{evaltab()}</tbody></table></div></section>

<section id="krytyka"><div class="col"><h2>Krytyka na chłodno i co z nią zrobiłem</h2>
<p>Niezależny agent ocenił wersję 1 na <b>3/10</b> (sens 4, spójność 4, liczby 2, czytelność 3, klimat 5) i porównał ją z PoE, Diablo 2–4, Last Epoch, Wynncraftem i Hypixelem. Zaplecze techniczne ocenił bardzo dobrze, samą grę źle. Pełny raport: <code>docs/research/itemizacja/krytyka-2026-09-30/krytyka.md</code>.</p></div>
<div class="tbl"><table><thead><tr><th>Stan</th><th>Zarzut (MUSI)</th><th>Co zrobione</th></tr></thead><tbody>
<tr><td><span class="pill ok">zrobione</span></td><td>Obrona odejmuje obrażenia płasko; wojownik poz. 10 byłby nieśmiertelny</td><td>Przedmioty nie dają już obrony: jest procentowa redukcja obrażeń (np. −3%) i pancerz. Wzór globalny należy do sesji od klas, a ona czeka na Twoją decyzję.</td></tr>
<tr><td><span class="pill ok">zrobione</span></td><td>Życie z ekwipunku 1,4–3,4× życia klasy</td><td>Jedna rodzina „+życie” (Krzepki), wartości obcięte do ok. 1/3, implicity życia o połowę mniejsze.</td></tr>
<tr><td><span class="pill ok">zrobione</span></td><td>Nie da się odróżnić setu</td><td>Pierwsza linia „Część zestawu”, pod nią blok setu z listą części i progami; ❖ w nazwie; zwykłe bazy bez motywów („Szyszak”, „Sygnet srebrny”); zielona podwójna ramka.</td></tr>
<tr><td><span class="pill ok">zrobione</span></td><td>Góra tooltipu: angielskie „Armor”, rzadkość w linii 19</td><td>Rzadkość w pierwszej linii, jedna linia typu, affiksy z ◆◆◇ i nazwą statystyki, wymagania na dole, przecinek dziesiętny.</td></tr>
<tr><td><span class="pill ok">zrobione</span></td><td>Brak zbytu dla śmieci</td><td>Nóż rozbiórkowy z kuźni: rozbiera łup na surowce profesji, które zasilają kamienie jakości.</td></tr>
<tr><td><span class="pill">czeka</span></td><td>Łup nie trafia pod klasę</td><td>Gotowe w generatorze. Włączy się, gdy sesja od klas wgra archetyp gracza.</td></tr>
<tr><td><span class="pill">odłożone</span></td><td>Poziom przedmiotu od zabójcy, nie od moba</td><td>Nie wybrałeś tego teraz. Wymaga małego pluginu łupu.</td></tr>
<tr><td><span class="pill ok">zrobione</span></td><td>Broń dwuręczna słabsza od jednoręcznej</td><td>DPS dwuręcznej ok. 1,35× jednoręcznej.</td></tr>
</tbody></table></div></section>

<section id="pomijane"><div class="col"><h2>Czego nie było w zamówieniu, a trzeba</h2>
<p>Z analizy PoE, Diablo 2–4, Wynncrafta i Last Epoch (pełny raport: <code>docs/research/itemizacja/01-gry-analiza.md</code>). Część jest już zrobiona, część czeka na decyzję.</p></div>
<div class="tbl"><table><thead><tr><th>Stan</th><th>Temat</th><th>Co to znaczy u nas</th></tr></thead><tbody><tr><td><span class='pill ok'>zrobione</span></td><td><b>Łup pod klasę (smart loot)</b></td><td>gotowy w generatorze, włącza się po wejściu placeholdera archetypu (Diablo 3: ~85% łupu pod klasę)</td></tr><tr><td><span class='pill ok'>zrobione</span></td><td><b>Brak duplikatów affiksów</b></td><td>rodziny max 1, 0 duplikatów w 320 przedmiotach z gry</td></tr><tr><td><span class='pill ok'>zrobione</span></td><td><b>Poziom przedmiotu, poziom postaci i wymaganie to trzy liczby</b></td><td>ilvl z zabójcy ±2, wymaganie 80% ilvl, nie mniej niż poziom bazy</td></tr><tr><td><span class='pill ok'>zrobione</span></td><td><b>Polska odmiana prefiksów</b></td><td>formy m/ż/n generowane z rodzaju bazy</td></tr><tr><td><span class='pill ok'>zrobione</span></td><td><b>Łup tylko od gracza</b></td><td>mob zabity przez ogień lub upadek nic nie daje (inaczej wypadałby przedmiot poziomu 0)</td></tr><tr><td><span class='pill '>częściowo</span></td><td><b>Czytelny tooltip i porównanie z założonym</b></td><td>polskie etykiety, tier każdego affiksu; brak strzałek ▲▼ względem założonego (wymaga pluginu)</td></tr><tr><td><span class='pill '>częściowo</span></td><td><b>Upusty waluty i rozbiórka łupu</b></td><td>jakość zużywa surowce profesji; brak rozbiórki przedmiotów na materiały (MMOItems ma deconstruct zależny od tieru)</td></tr><tr><td><span class='pill '>do decyzji</span></td><td><b>Wiązanie przedmiotów</b></td><td>propozycja: zwykłe i magiczne wolne, sety i unikaty wiązane przy założeniu</td></tr><tr><td><span class='pill '>do decyzji</span></td><td><b>Łup na minutę</b></td><td>dziś: T1 5%, T2 10%, elity 30%, boss 2 + część setu; trzeba zmierzyć, ile to daje na godzinę gry</td></tr><tr><td><span class='pill '>do decyzji</span></td><td><b>Przelosowanie affiksu z limitem</b></td><td>jak Chaos w PoE2 albo Forging Potential w Last Epoch; bez tego słaby rzadki jest śmieciem</td></tr><tr><td><span class='pill '>do decyzji</span></td><td><b>Ochrona przed pechem dla setów</b></td><td>np. wymiana 3 duplikatów części na brakującą albo gwarantowana część za pierwsze przejście lochu</td></tr><tr><td><span class='pill '>do decyzji</span></td><td><b>Unikaty z efektem</b></td><td>1–2 na nadklasę jako cel polowania; tier UNIKATOWY czeka pusty</td></tr><tr><td><span class='pill '>później</span></td><td><b>Filtr i podświetlenie łupu, transmogryfikacja, gniazda</b></td><td>glow w kolorze rzadkości działa; reszta po Akcie 1</td></tr></tbody></table></div></section>

<section id="dalej" class="col"><h2>Co dalej</h2>
<ol class="dec">
<li><b>Liczby mobów Aktu 1.</b> Pancerz zbroi wersji 2 tnie T2 o ok. 61%, T1 o 27% (pomiar na klonie). Czy to za dużo, rozstrzygnie sesja lochów po walkach na poligonie; ewentualnie obniżę pancerz przedmiotów.</li>
<li><b>Umiejętności unikatów i setów</b> przejdą na zasoby klas (Szał, Znamiona, Mana, Dusze), gdy powstaną w etapie 3 planu klas.</li>
<li><b>Drobne:</b> liczby z trzema miejscami po przecinku (np. wytrzymałość pancerza), rozbiórka u NPC (dziś nożem z kuźni), aukcje nie znają typów GREATBOW, SIERP, KOSA, FILAKTERIUM.</li>
<li><b>Wycofanie</b> w razie potrzeby: <code>tools/przedmioty/wycofaj.sh backups/przedmioty-20260930-174000</code>.</li>
</ol></section>
</div>
"""
open(out, 'w', encoding='utf-8').write(H)
print('ok', len(H))
