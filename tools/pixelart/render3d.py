"""Render bryly 3D (SDF) -> siatka pixel-art. Swiatlo i cien liczone fizycznie, nie zgadywane.
Uzycie: from render3d import scena_potka, renderuj"""
import numpy as np

def norm(v):
    return v / np.maximum(np.linalg.norm(v, axis=-1, keepdims=True), 1e-9)

def sd_kula(p, c, r):
    return np.linalg.norm(p - np.array(c), axis=-1) - r

def sd_walec(p, c, r, h):  # pionowy walec, srodek c, promien r, wysokosc h
    q = p - np.array(c)
    dx = np.linalg.norm(q[..., [0, 2]], axis=-1) - r
    dy = np.abs(q[..., 1]) - h / 2
    out = np.linalg.norm(np.stack([np.maximum(dx, 0), np.maximum(dy, 0)], -1), axis=-1)
    return out + np.minimum(np.maximum(dx, dy), 0)

def gradient(f, p, e=1e-3):
    g = np.stack([f(p + np.array(d)) - f(p - np.array(d)) for d in ([e, 0, 0], [0, e, 0], [0, 0, e])], -1)
    return norm(g)

def marsz(f, o, d, kroki=160, tmax=60):
    t = np.zeros(o.shape[:-1]); traf = np.zeros(o.shape[:-1], bool)
    for _ in range(kroki):
        p = o + d * t[..., None]
        dist = f(p)
        traf |= dist < 1e-3
        t = np.where(traf, t, t + np.maximum(dist, 1e-3))
    traf &= t < tmax
    return t, traf

def renderuj(scena, n=16, ss=8, pitch_deg=18, swiatlo=(-1.0, 1.2, 1.0)):
    """zwraca slownik map n x n: 'mat' (0 tlo, 1 szklo puste, 2 plyn, 3 korek, 4 powierzchnia plynu),
    'lam' (0..1 oswietlenie), 'spec' (udzial blikow), 'fres' (krawedziowosc szkla)"""
    N = n * ss
    js, is_ = np.meshgrid(np.arange(N), np.arange(N))
    sx = (js + 0.5) / ss - n / 2
    sy = n / 2 - (is_ + 0.5) / ss
    a = np.radians(pitch_deg)
    # kamera: patrzy wzdluz -z, pochylona w dol o pitch (obrot wokol osi x)
    fwd = np.array([0, -np.sin(a), -np.cos(a)]); up = np.array([0, np.cos(a), -np.sin(a)]); right = np.array([1, 0, 0])
    o = sx[..., None] * right + sy[..., None] * up - fwd * 30
    d = np.broadcast_to(fwd, o.shape)
    L = norm(np.array(swiatlo)); V = -fwd; H = norm(L + V)
    szklo, plyn, korek, poziom = scena['szklo'], scena['plyn'], scena['korek'], scena['poziom']
    wszystko = lambda p: np.minimum(szklo(p), korek(p))
    t, traf = marsz(wszystko, o, d)
    p = o + d * t[..., None]
    jest_korek = traf & (korek(p) < szklo(p))
    mat = np.zeros((N, N), int); lam = np.zeros((N, N)); spec = np.zeros((N, N)); fres = np.zeros((N, N))
    nk = gradient(korek, p); ns = gradient(szklo, p)
    lam = np.where(jest_korek, np.clip((nk * L).sum(-1), 0, 1), lam)
    mat[jest_korek] = 3
    s = traf & ~jest_korek
    ndv = np.abs((ns * V).sum(-1))
    fres = np.where(s, 1 - ndv, 0)
    spec = np.where(s, np.clip((ns * H).sum(-1), 0, 1) ** 60, 0)
    # do srodka: szukamy plynu
    o2 = p + d * 0.05
    t2, traf2 = marsz(plyn, o2, d, kroki=120, tmax=20)
    p2 = o2 + d * t2[..., None]
    np2 = gradient(plyn, p2)
    wplyn = s & traf2
    mat[s] = 1
    mat[wplyn] = 2
    powierzchnia = wplyn & (np2[..., 1] > 0.9)
    mat[powierzchnia] = 4
    lp = np.clip((np2 * L).sum(-1), 0, 1)
    lam = np.where(wplyn, lp, lam)
    lam = np.where(s & ~wplyn, np.clip((ns * L).sum(-1), 0, 1), lam)
    # uśrednianie do siatki n x n
    def blok(x):
        return x.reshape(n, ss, n, ss).swapaxes(1, 2).reshape(n, n, ss * ss)
    M = blok(mat)
    out = {'mat': np.zeros((n, n), int), 'lam': np.zeros((n, n)), 'spec': blok(spec).mean(-1), 'fres': np.zeros((n, n)),
           'pokrycie': (M > 0).mean(-1)}
    Lb, Fb = blok(lam), blok(fres)
    for i in range(n):
        for j in range(n):
            m = M[i, j]
            if (m > 0).mean() < 0.5:
                continue
            vals, cnt = np.unique(m[m > 0], return_counts=True)
            k = vals[cnt.argmax()]
            out['mat'][i, j] = k
            sel = m == k
            out['lam'][i, j] = Lb[i, j][sel].mean(); out['fres'][i, j] = Fb[i, j][sel].mean()
    return out

def scena_potka(R=5.2, cy=-2.2, poziom=-1.6):
    kula = lambda p: sd_kula(p, (0, cy, 0), R)
    szyjka = lambda p: sd_walec(p, (0, cy + R + 1.0, 0), 1.05, 3.0)
    kolnierz = lambda p: sd_walec(p, (0, cy + R + 2.4, 0), 1.6, 0.8)
    szklo = lambda p: np.minimum(np.minimum(kula(p), szyjka(p)), kolnierz(p))
    korek = lambda p: sd_walec(p, (0, cy + R + 3.3, 0), 1.0, 1.4)
    plyn = lambda p: np.maximum(sd_kula(p, (0, cy, 0), R - 0.45), p[..., 1] - poziom)
    return {'szklo': szklo, 'plyn': plyn, 'korek': korek, 'poziom': poziom}

def sd_box2(x, y, cx, cy, hx, hy):
    dx = np.abs(x - cx) - hx; dy = np.abs(y - cy) - hy
    return np.hypot(np.maximum(dx, 0), np.maximum(dy, 0)) + np.minimum(np.maximum(dx, dy), 0)

def renderuj_bryle(f, n=16, ss=8, pitch_deg=0, swiatlo=(-0.9, 1.0, 1.2), polysk=40, id_f=None, ao_sila=0.0, metal=None):
    """bryla nieprzezroczysta: mapy n x n 'mask', 'lam', 'spec', 'id' (id_f(p) -> int, np. rant/tarcza/znak)"""
    N = n * ss
    js, is_ = np.meshgrid(np.arange(N), np.arange(N))
    sx = (js + 0.5) / ss - n / 2; sy = n / 2 - (is_ + 0.5) / ss
    a = np.radians(pitch_deg)
    fwd = np.array([0, -np.sin(a), -np.cos(a)]); up = np.array([0, np.cos(a), -np.sin(a)]); right = np.array([1, 0, 0])
    o = sx[..., None] * right + sy[..., None] * up - fwd * 30
    d = np.broadcast_to(fwd, o.shape)
    L = norm(np.array(swiatlo)); V = -fwd; H = norm(L + V)
    t, traf = marsz(f, o, d, kroki=200)
    p = o + d * t[..., None]; nr = gradient(f, p)
    lam = np.where(traf, np.clip((nr * L).sum(-1), 0, 1), 0)
    if metal is not None:  # polerowany metal: jasnosc = odbicie otoczenia w kierunku odbitego promienia
        Rv = d - 2 * (d * nr).sum(-1, keepdims=True) * nr
        ry = Rv[..., 1]
        niebo, horyzont, ziemia, szer = metal.get('niebo', 1.0), metal.get('horyzont', 0.25), metal.get('ziemia', 0.55), metal.get('szer', 0.25)
        env = np.where(ry > szer, niebo - 0.25 * (1 - ry), np.where(ry > -szer, horyzont, ziemia))
        env = env + 0.15 * np.clip(-Rv[..., 0], 0, 1)   # strona lampy (lewa) odrobine jasniej
        mix = metal.get('mix', 0.75)
        lam = np.where(traf, np.clip(mix * env + (1 - mix) * lam, 0, 1), 0)
    if ao_sila:  # cien w zaglebieniach: jak blisko innej powierzchni wzdluz normalnej
        ao = np.mean([np.clip(f(p + nr * k) / k, 0, 1) for k in (0.15, 0.3, 0.5, 0.8)], axis=0)
        lam = lam * (1 - ao_sila * (1 - ao))
    spec = np.where(traf, np.clip((nr * H).sum(-1), 0, 1) ** polysk, 0)
    ids = id_f(p) if id_f else np.zeros((N, N), int)
    def blok(x): return x.reshape(n, ss, n, ss).swapaxes(1, 2).reshape(n, n, ss * ss)
    T, Lb, Sb, Ib = blok(traf), blok(lam), blok(spec), blok(np.where(traf, ids, -1))
    out = {'mask': T.mean(-1) >= 0.5, 'lam': np.zeros((n, n)), 'spec': Sb.mean(-1), 'id': np.zeros((n, n), int), 'nz': np.zeros((n, n))}
    NZ = blok(np.where(traf, nr[..., 2], 0)); TB = blok(np.where(traf, t, 0))
    out['glab'] = np.zeros((n, n))
    for i in range(n):
        for j in range(n):
            if not out['mask'][i, j]: continue
            sel = T[i, j]
            out['lam'][i, j] = Lb[i, j][sel].mean(); out['nz'][i, j] = NZ[i, j][sel].mean(); out['glab'][i, j] = TB[i, j][sel].mean()
            v, c = np.unique(Ib[i, j][sel], return_counts=True); out['id'][i, j] = v[c.argmax()]
    return out
