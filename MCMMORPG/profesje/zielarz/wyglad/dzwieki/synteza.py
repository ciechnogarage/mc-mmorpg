"""Własne dźwięki chwili zbioru kozłka (feeling mmo/rytuał): CAŁA synteza tutaj (numpy/scipy), bez cudzych próbek.
Licencja: CC0 (dzieło własne projektu, zrzeczenie praw) - patrz ZRODLA.md.

Uzycie (przez dzwieki.sh, kontener z numpy/scipy/soundfile):
  synteza.py <wyjscie_ogg> [--podglad <katalog>] [--vanilla <assets_mc>]
  -> <wyjscie_ogg>/kozlek/{szelest1-3,napiecie1-3,wyrwanie1-3,wyborny1}.ogg (mono, Vorbis, 44,1 kHz)
  -> --podglad: wav + png (fala + spektrogram) + pomiar.json (dB względem odpowiednika vanilli)
Głośność: każdy dźwięk normalizowany do średniego RMS (części aktywnej) odpowiednika vanilli z uwzględnieniem
głośności z sounds.json; kod gra go z TĄ SAMĄ głośnością co vanilla, więc nie jest głośniejszy od otoczenia.
"""
import json, os, sys
import numpy as np
from scipy import signal

SR = 44100
rng = np.random.default_rng(20261024)


def t_(sek):
    return np.arange(int(SR * sek)) / SR


def pasmo(x, lo, hi, rzad=2):
    lo = max(lo, 20); hi = min(hi, SR / 2 - 100)
    b, a = signal.butter(rzad, [lo, hi], btype='band', fs=SR)
    return signal.lfilter(b, a, x)


def dolny(x, f, rzad=2):
    b, a = signal.butter(rzad, f, btype='low', fs=SR)
    return signal.lfilter(b, a, x)


def gorny(x, f, rzad=2):
    b, a = signal.butter(rzad, f, btype='high', fs=SR)
    return signal.lfilter(b, a, x)


def rezonans(x, f, q):
    b, a = signal.iirpeak(f, q, fs=SR)
    return signal.lfilter(b, a, x)


def obwiednia(n, atak, zanik):
    """atak [s] liniowo, potem wykladniczy zanik o stalej zanik [s]."""
    t = np.arange(n) / SR
    e = np.minimum(1, t / max(atak, 1e-4)) * np.exp(-np.maximum(0, t - atak) / zanik)
    return e


def ziarno(dl, lo, hi, zanik):
    n = int(SR * dl)
    return pasmo(rng.standard_normal(n), lo, hi) * obwiednia(n, 0.0015, zanik)


def wstaw(buf, x, sek):
    i = int(sek * SR)
    j = min(len(buf), i + len(x))
    if j > i: buf[i:j] += x[:j - i]


def poglos(x, mieszanie=0.12, czas=0.18):
    """Krotki, miekki poglos (kilka opoznien z filtrem) - przestrzen pod liscmi, bez ogona katedry."""
    y = np.copy(x)
    for d, g in ((0.023, 0.5), (0.031, 0.42), (0.043, 0.35), (0.057, 0.28)):
        k = int(d * SR)
        z = np.zeros_like(x)
        z[k:] = x[:-k] * g
        y += mieszanie * dolny(z, 3500)
    n = len(y)
    return y * np.minimum(1, (np.arange(n)[::-1] / (SR * 0.02)))   # bez trzasku na koncu


# ---------------------------------------------------------------- dźwięki
def szelest(war):
    """Szelest rozety pod dłonią: drobne trzaski liści (2-7 kHz) gęstniejące i rzednące + miękki „szum” ruchu."""
    dl = 0.42 + 0.05 * war
    n = int(SR * dl)
    y = np.zeros(n)
    gest = 70 + 15 * war
    for _ in range(gest):
        tt = rng.beta(2.0, 3.2) * (dl - 0.03)
        lo = rng.uniform(1800, 4200)
        g = ziarno(rng.uniform(0.004, 0.016), lo, lo * rng.uniform(1.4, 2.2), rng.uniform(0.002, 0.006))
        wstaw(y, g * rng.uniform(0.3, 1.0), tt)
    ruch = dolny(gorny(rng.standard_normal(n), 300), 1600) * obwiednia(n, 0.09, 0.12) * 0.5
    y += ruch
    return poglos(y, 0.1)


def napiecie(war):
    """Napięta łodyga: stick-slip (impulsy o rosnącej częstotliwości 30->85 Hz) przez rezonanse włókien + suche trzaski."""
    dl = 0.55 + 0.06 * war
    n = int(SR * dl)
    imp = np.zeros(n)
    t = 0.012
    while t < dl - 0.03:
        f = 30 + 55 * (t / dl) ** 1.3 + rng.normal(0, 4)
        imp[int(t * SR)] += rng.uniform(0.6, 1.0)
        t += 1 / max(18, f) * rng.uniform(0.75, 1.25)
    klik = signal.lfilter([1], [1, -0.6], imp)
    y = 0.9 * rezonans(klik, 480 + 30 * war, 9) + 0.6 * rezonans(klik, 1130 - 40 * war, 11) + 0.35 * rezonans(klik, 2350, 14)
    y *= np.minimum(1, np.arange(n) / (SR * 0.18)) * (0.55 + 0.45 * np.sin(np.pi * np.arange(n) / n))   # narasta i trzyma
    for _ in range(8 + 2 * war):   # włókna pękają pod koniec
        wstaw(y, ziarno(rng.uniform(0.003, 0.008), 2500, 6500, 0.0025) * rng.uniform(0.05, 0.12), rng.uniform(0.3, 0.95) * dl)
    return poglos(y, 0.08)


def wyrwanie(war):
    """Wyrwanie korzenia z mokrej ziemi: głuche tąpnięcie, seria pękających korzonków, mlaśnięcie (rezonans 600->170 Hz),
    osypująca się ziemia."""
    dl = 0.85 + 0.05 * war
    n = int(SR * dl)
    y = np.zeros(n)
    # tąpnięcie: sinus 72 -> 44 Hz + niski szum
    tt = t_(0.18)
    f = 72 - 28 * tt / tt[-1]
    tap = np.sin(2 * np.pi * np.cumsum(f) / SR) * obwiednia(len(tt), 0.004, 0.05)
    tap += 0.6 * dolny(rng.standard_normal(len(tt)), 400) * obwiednia(len(tt), 0.002, 0.04)
    wstaw(y, 1.2 * tap, 0.0)
    # korzonki pękają: 18-26 suchych trzasków 0,02-0,30 s, coraz rzadziej
    for i in range(20 + 3 * war):
        s = 0.02 + 0.28 * (i / (20 + 3 * war)) ** 1.6 + rng.normal(0, 0.006)
        lo = rng.uniform(700, 1800)
        wstaw(y, ziarno(rng.uniform(0.004, 0.012), lo, lo * 2.5, rng.uniform(0.002, 0.005)) * rng.uniform(0.5, 1.2), max(0, s))
    # mlaśnięcie: szum przez rezonans przesuwany w dół, modulowany „bąblami”
    m = t_(0.34)
    sz = rng.standard_normal(len(m))
    fr = 620 * (170 / 620) ** (m / m[-1])
    wyj = np.zeros_like(sz)
    # filtr zmienny: kawałki po 5 ms z rezonansem w bieżącej częstotliwości
    k = int(0.005 * SR)
    zi = None
    for a in range(0, len(sz), k):
        b_, a_ = signal.iirpeak(fr[a], 5, fs=SR)
        if zi is None: zi = signal.lfilter_zi(b_, a_) * 0
        wyj[a:a + k], zi = signal.lfilter(b_, a_, sz[a:a + k], zi=zi)
    babel = 0.6 + 0.4 * np.abs(np.sin(2 * np.pi * (9 + 3 * war) * m + rng.uniform(0, 3)))
    wstaw(y, 1.4 * wyj * babel * obwiednia(len(m), 0.03, 0.12), 0.06)
    # ziemia się osypuje
    for _ in range(40):
        s = rng.uniform(0.15, dl - 0.05)
        g = dolny(ziarno(rng.uniform(0.006, 0.02), 300, 2400, 0.006), 2200)
        wstaw(y, g * 0.5 * np.exp(-(s - 0.15) / 0.35), s)
    return poglos(y, 0.1)


def wyborny():
    """Nagroda wybornego: drewniano-szklany dzwonek (pentatonika A: A5, C#6, E6, potem A6 cicho), krótki pogłos.
    Celowo inny niż levelup/ametyst: miękki atak, nieharmoniczne alikwoty jak kalimba."""
    dl = 1.35
    n = int(SR * dl)
    y = np.zeros(n)
    for f0, s, g in ((880.0, 0.0, 1.0), (1108.7, 0.075, 0.85), (1318.5, 0.15, 0.8), (1760.0, 0.26, 0.35)):
        m = t_(dl - s)
        nuta = np.zeros_like(m)
        for mn, a, z in ((1.0, 1.0, 0.55), (2.0, 0.18, 0.25), (3.01, 0.12, 0.12), (5.43, 0.06, 0.05)):
            nuta += a * np.sin(2 * np.pi * f0 * mn * m) * np.exp(-m / z)
        nuta *= np.minimum(1, m / 0.004)
        wstaw(y, g * nuta, s)
    # lekki „szelest blasku” (wysoki szum, bardzo cichy) - nawiązanie do liści
    sz = pasmo(rng.standard_normal(n), 5000, 11000) * obwiednia(n, 0.05, 0.25) * 0.05
    y += sz
    return poglos(y, 0.22)


NAZWY = {'szelest': (szelest, 3), 'napiecie': (napiecie, 3), 'wyrwanie': (wyrwanie, 3), 'wyborny': (lambda w: wyborny(), 1)}
# odpowiedniki vanilli zastepowane w kodzie: [(zdarzenie z sounds.json, glosnosc wywolania)], glosnosc wywolania naszego
VANILLA = {'szelest': ([('block.azalea_leaves.step', 0.5)], 0.5), 'napiecie': ([('block.bamboo.hit', 0.45)], 0.45),
           'wyrwanie': ([('block.rooted_dirt.break', 1.0)], 1.0),
           'wyborny': ([('block.amethyst_block.chime', 0.9), ('entity.player.levelup', 0.35)], 0.9)}


def odniesienie(assets, nazwa):
    """Laczny RMS (suma mocy) odpowiednikow vanilli przy ich glosnosciach z kodu [dBFS], liczba plikow."""
    moc, n = 0.0, 0
    for zd, vol in VANILLA[nazwa][0]:
        r, k = vanilla_rms(assets, zd)
        moc += 10 ** ((r + 20 * np.log10(vol)) / 10); n += k
    return 10 * np.log10(moc), n


def rms_aktywny(x):
    """RMS części aktywnej (okna 20 ms powyżej -40 dB od szczytu okna) w dBFS."""
    k = int(0.02 * SR)
    okna = [x[i:i + k] for i in range(0, len(x) - k, k)]
    r = np.array([np.sqrt(np.mean(o ** 2)) for o in okna]) + 1e-12
    m = r > r.max() * 10 ** (-40 / 20)
    return 20 * np.log10(np.sqrt(np.mean(r[m] ** 2)))


def vanilla_rms(assets, zdarzenie):
    import soundfile as sf
    idx = json.load(open(os.path.join(assets, 'indexes/29.json')))['objects']
    obj = lambda k: os.path.join(assets, 'objects', idx[k]['hash'][:2], idx[k]['hash'])
    sj = json.load(open(obj('minecraft/sounds.json')))
    wyniki = []
    for s in sj[zdarzenie]['sounds']:
        nazwa, vol = (s, 1.0) if isinstance(s, str) else (s['name'], s.get('volume', 1.0))
        if isinstance(s, dict) and s.get('type') == 'event':
            continue
        x, sr = sf.read(obj(f'minecraft/sounds/{nazwa}.ogg'))
        if x.ndim > 1: x = x.mean(1)
        wyniki.append(rms_aktywny(x) + 20 * np.log10(vol))
    return float(np.mean(wyniki)), len(wyniki)


def main(argv):
    import soundfile as sf
    out = argv[0]
    podglad = argv[argv.index('--podglad') + 1] if '--podglad' in argv else None
    assets = argv[argv.index('--vanilla') + 1] if '--vanilla' in argv else None
    os.makedirs(os.path.join(out, 'kozlek'), exist_ok=True)
    pomiar = {}
    for nazwa, (f, ile) in NAZWY.items():
        ref, nv = odniesienie(assets, nazwa) if assets else (-20.0, 0)
        cel = ref - 20 * np.log10(VANILLA[nazwa][1])   # RMS pliku, ktory przy naszej glosnosci da ref
        for w in range(1, ile + 1):
            x = f(w)
            x = x - np.mean(x)
            x *= 10 ** ((cel - rms_aktywny(x)) / 20)       # ten sam RMS co vanilla
            pk = np.max(np.abs(x))
            if pk > 0.89: x *= 0.89 / pk                     # szczyt <= -1 dBFS (wtedy ciszej niz vanilla, nigdy glosniej)
            plik = os.path.join(out, 'kozlek', f'{nazwa}{w}.ogg')
            sf.write(plik, x.astype(np.float32), SR, format='OGG', subtype='VORBIS')
            y, _ = sf.read(plik)
            gl_v = ref; gl_n = rms_aktywny(y) + 20 * np.log10(VANILLA[nazwa][1])
            pomiar[f'{nazwa}{w}'] = {'vanilla': ' + '.join(f'{z}@{v}' for z, v in VANILLA[nazwa][0]), 'plikow_vanilla': nv,
                                    'rms_vanilla_db': round(float(gl_v), 1),
                                    'rms_nasz_db': round(float(gl_n), 1), 'roznica_db': round(float(gl_n - gl_v), 1),
                                    'szczyt_dbfs': round(float(20 * np.log10(np.max(np.abs(y)) + 1e-12)), 1),
                                    'sekundy': round(len(y) / SR, 2), 'bajty': os.path.getsize(plik)}
            if podglad:
                os.makedirs(podglad, exist_ok=True)
                sf.write(os.path.join(podglad, f'{nazwa}{w}.wav'), y.astype(np.float32), SR, subtype='PCM_16')
                rysuj(y, os.path.join(podglad, f'{nazwa}{w}.png'), f'{nazwa}{w}')
    if podglad:
        json.dump(pomiar, open(os.path.join(podglad, 'pomiar.json'), 'w'), indent=1)
    for k, v in pomiar.items():
        print(k, v)


def rysuj(y, plik, tytul):
    """Fala (góra) + spektrogram (dół) 480x200, kolory strony (#1d161f tło, złoto)."""
    from PIL import Image, ImageDraw
    W_, H1, H2 = 480, 70, 130
    im = Image.new('RGB', (W_, H1 + H2), (29, 22, 31))
    d = ImageDraw.Draw(im)
    n = len(y)
    for x in range(W_):
        s = y[x * n // W_:(x + 1) * n // W_]
        if len(s) == 0: continue
        a, b = s.min(), s.max()
        d.line([(x, H1 / 2 - b * H1 / 2), (x, H1 / 2 - a * H1 / 2)], fill=(213, 169, 107))
    f, t, S = signal.spectrogram(y, SR, nperseg=512, noverlap=384)
    S = 10 * np.log10(S + 1e-12)
    S = np.clip((S - (S.max() - 70)) / 70, 0, 1)
    fmax = 12000
    rows = f <= fmax
    S = S[rows][::-1]
    sp = Image.fromarray((S * 255).astype(np.uint8)).resize((W_, H2))
    pal = np.zeros((256, 3), np.uint8)
    for i in range(256):
        v = i / 255
        pal[i] = (int(29 + (255 - 29) * v ** 0.8), int(22 + (230 - 22) * v ** 1.3), int(31 + (170 - 31) * v ** 2.2))
    im.paste(Image.fromarray(pal[np.asarray(sp)]), (0, H1))
    d.text((4, 2), tytul, fill=(255, 241, 204))
    d.text((4, H1 + 2), '12 kHz', fill=(160, 150, 140)); d.text((4, H1 + H2 - 12), '0', fill=(160, 150, 140))
    im.save(plik)


if __name__ == '__main__':
    main(sys.argv[1:])
