#!/usr/bin/env python3
"""Składa stronę podglądu weryfikacji (para botów) z qa/para.log i qa/kadry/*.jpg -> qa/strona/index.html."""
import html, json, os, re, shutil, time
Q = os.path.dirname(os.path.abspath(__file__)); S = '/home/dev/Desktop/Przemek/MC/tools/klasy-rasowe/poligon/strona'; os.makedirs(os.path.join(S, 'kadry'), exist_ok=True)
log = open(os.path.join(Q, 'para.log'), encoding='utf-8', errors='replace').read().splitlines() if os.path.exists(os.path.join(Q, 'para.log')) else []
kadry = [json.loads(l) for l in open(os.path.join(Q, 'kadry', 'kadry.jsonl'), encoding='utf-8')] if os.path.exists(os.path.join(Q, 'kadry', 'kadry.jsonl')) else []
for k in kadry:
    src = os.path.join(Q, 'kadry', k['nr'] + '.jpg'); dst = os.path.join(S, 'kadry', k['nr'] + '.jpg')
    if os.path.exists(src) and (not os.path.exists(dst) or os.path.getmtime(src) > os.path.getmtime(dst)): shutil.copy(src, dst)
# przypisz linie PASS/FAIL do ostatniego kadru
wyniki, i, biez, klasa = {}, -1, None, ''
for l in log:
    if l.startswith('KADR'): i += 1; biez = kadry[i]['nr'] if i < len(kadry) else None
    elif l.startswith(('PASS', 'FAIL')): wyniki.setdefault(biez or 'przed', []).append(l)
    elif l.startswith('##'): klasa = l[3:]
wl = log[:next((i for i, l in enumerate(log) if l.startswith('WYNIK')), len(log))]
pas = sum(l.startswith('PASS') for l in wl); fai = [l for l in wl if l.startswith('FAIL')]
koniec = any('WYNIK' in l or l == 'KONIEC' for l in log)
e = html.escape
def wyn(l):
    ok = l.startswith('PASS'); return f'<li class="{"ok" if ok else "zle"}"><b>{"PASS" if ok else "FAIL"}</b> {e(l[5:])}</li>'
karty = []
for k in reversed(kadry):
    w = wyniki.get(k['nr'], [])
    karty.append(f'''<figure class="kadr"><img src="kadry/{k["nr"]}.jpg" alt="{e(k["opis"])}" loading="lazy" width="800" height="450">
<figcaption><span class="czas">{e(k["czas"])}</span> <span class="opis">{e(k["opis"])}</span>{"<ul>" + "".join(wyn(x) for x in w) + "</ul>" if w else ""}</figcaption></figure>''')
stan = 'Zakończone' if koniec else ('W toku' if log else 'Czeka na start')
ocena = open(os.path.join(Q, 'ocena.html'), encoding='utf-8').read() if os.path.exists(os.path.join(Q, 'ocena.html')) else ''
out = f'''<title>Weryfikacja klas na botach</title>
<style>
/* układ: pasek stanu + lista problemów, pod spodem siatka kadrów (najnowsze pierwsze) */
:root {{ --tlo:#f3f4f1; --karta:#ffffff; --tekst:#1d2320; --przygaszony:#5b665f; --linia:#d6dbd5; --akcent:#2f6b4f; --ok:#2e7d4a; --zle:#b3392f;
  --f-tyt:"Literata", Georgia, serif; --f-txt:"Source Sans 3", system-ui, sans-serif; --f-dane:"JetBrains Mono", ui-monospace, monospace; }}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{ --tlo:#141816; --karta:#1d2320; --tekst:#e6ebe7; --przygaszony:#9aa69f; --linia:#2e3631; --akcent:#7fc4a1; --ok:#6cc08a; --zle:#f08a7f; color-scheme:dark }} }}
:root[data-theme="dark"] {{ --tlo:#141816; --karta:#1d2320; --tekst:#e6ebe7; --przygaszony:#9aa69f; --linia:#2e3631; --akcent:#7fc4a1; --ok:#6cc08a; --zle:#f08a7f; color-scheme:dark }}
body {{ background:var(--tlo); color:var(--tekst); font:16px/1.5 var(--f-txt); }}
.wrap {{ max-width:1180px; margin:0 auto; padding-inline:16px; padding-block:24px 48px; display:grid; gap:24px; }}
h1 {{ font:600 1.9rem/1.2 var(--f-tyt); margin:0; text-wrap:balance; }}
.pod {{ color:var(--przygaszony); margin:4px 0 0; max-width:68ch; }}
.stan {{ display:flex; flex-wrap:wrap; gap:12px; font:500 .95rem var(--f-dane); font-variant-numeric:tabular-nums; }}
.chip {{ border:1px solid var(--linia); background:var(--karta); padding:6px 12px; border-radius:6px; }}
.chip.ok {{ color:var(--ok); }} .chip.zle {{ color:var(--zle); }}
.problemy {{ background:var(--karta); border:1px solid var(--linia); border-left:4px solid var(--zle); padding:12px 16px; border-radius:6px; }}
.problemy h2, .siatka-tyt {{ font:600 1.1rem var(--f-tyt); margin:0 0 8px; }}
ul {{ margin:0; padding-left:0; list-style:none; display:grid; gap:2px; font-size:.92rem; }}
li b {{ font:600 .78rem var(--f-dane); letter-spacing:.04em; margin-right:6px; }}
li.ok b {{ color:var(--ok); }} li.zle b {{ color:var(--zle); }}
.siatka {{ display:grid; grid-template-columns:repeat(auto-fill, minmax(min(100%, 340px), 1fr)); gap:16px; }}
.kadr {{ margin:0; background:var(--karta); border:1px solid var(--linia); border-radius:6px; overflow:hidden; min-width:0; }}
.kadr img {{ display:block; width:100%; height:auto; aspect-ratio:16/9; object-fit:cover; background:var(--linia); }}
figcaption {{ padding:10px 12px; display:grid; gap:6px; min-width:0; }}
.czas {{ font:500 .78rem var(--f-dane); color:var(--przygaszony); }} .opis {{ font-weight:600; overflow-wrap:anywhere; }}
  .ocena {{ background:var(--karta); border:1px solid var(--linia); border-left:4px solid var(--ok); padding:12px 16px; border-radius:6px; display:grid; gap:8px; }}
.ocena h2 {{ font:600 1.1rem var(--f-tyt); margin:0; }} .ocena p {{ margin:0; max-width:75ch; }} .lista li {{ padding-left:4px; }}
.pusto {{ color:var(--przygaszony); }}
</style>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Literata:wght@600&family=Source+Sans+3:wght@400;600&family=JetBrains+Mono:wght@500&display=swap">
<div class="wrap">
<header><h1>Weryfikacja klas na botach</h1>
<p class="pod">Dwa boty na klonie world1: gracz testowanej klasy i sojusznik obok. Kamera to prawdziwy klient Minecrafta w trybie widza nad areną; kadr powstaje przy każdym rzucie. Pod kadrem wyniki pomiaru z serwera.</p></header>
<div class="stan"><span class="chip">{stan}</span><span class="chip">Teraz: {e(klasa) or "—"}</span><span class="chip ok">PASS {pas}</span><span class="chip zle">FAIL {len(fai)}</span><span class="chip">kadry {len(kadry)}</span><span class="chip">aktualizacja {time.strftime("%d.%m %H:%M")}</span></div>
{ocena}
{('<section class="problemy"><h2>Do sprawdzenia</h2><ul>' + "".join(wyn(x) for x in fai) + '</ul></section>') if fai else ''}
<section><h2 class="siatka-tyt">Kadry (najnowsze pierwsze)</h2>{('<div class="siatka">' + "".join(karty) + '</div>') if karty else '<p class="pusto">Pierwsze kadry pojawią się po starcie scenariusza.</p>'}</section>
</div>
'''
open(os.path.join(S, 'index.html'), 'w', encoding='utf-8').write(out)
print(len(kadry), 'kadrów', pas, 'PASS', len(fai), 'FAIL')
