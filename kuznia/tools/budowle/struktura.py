"""Zapis vanillowej struktury (.nbt, gzip) - format szablonu /place template i bloku struktury. Zero zaleznosci.
Budowla = slownik {(x,y,z): 'minecraft:blok[wl=w,...]'}; wspolrzedne dowolne, zapis przesuwa do (0,0,0).
Powietrza NIE zapisujemy (nie wycina terenu) chyba ze jawnie 'minecraft:air'."""
import gzip, struct, re

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def _s(v):
    b = v.encode('utf-8'); return struct.pack('>H', len(b)) + b


def _payload(t, v):
    if t == TAG_INT: return struct.pack('>i', v)
    if t == TAG_STRING: return _s(v)
    if t == TAG_LIST:
        et, items = v
        return struct.pack('>bi', et if items else TAG_END, len(items)) + b''.join(_payload(et, x) for x in items)
    if t == TAG_COMPOUND:
        return b''.join(struct.pack('>b', tt) + _s(k) + _payload(tt, vv) for k, (tt, vv) in v.items()) + b'\x00'
    raise ValueError(t)


def _stan(s):
    m = re.match(r'^([^\[]+)(?:\[(.*)\])?$', s.strip())
    nazwa, wl = m.group(1), m.group(2)
    if ':' not in nazwa: nazwa = 'minecraft:' + nazwa
    props = {}
    if wl:
        for para in wl.split(','):
            k, v = para.split('='); props[k.strip()] = (TAG_STRING, v.strip())
    return nazwa, props


def zapisz(bloki, sciezka, data_version):
    xs = [p[0] for p in bloki]; ys = [p[1] for p in bloki]; zs = [p[2] for p in bloki]
    x0, y0, z0 = min(xs), min(ys), min(zs)
    size = [max(xs) - x0 + 1, max(ys) - y0 + 1, max(zs) - z0 + 1]
    paleta, idx, lista = [], {}, []
    for (x, y, z), s in sorted(bloki.items()):
        klucz = _stan(s)
        k = (klucz[0], tuple(sorted((a, b[1]) for a, b in klucz[1].items())))
        if k not in idx:
            idx[k] = len(paleta)
            c = {'Name': (TAG_STRING, klucz[0])}
            if klucz[1]: c['Properties'] = (TAG_COMPOUND, klucz[1])
            paleta.append(c)
        lista.append({'pos': (TAG_LIST, (TAG_INT, [x - x0, y - y0, z - z0])), 'state': (TAG_INT, idx[k])})
    root = {'DataVersion': (TAG_INT, data_version),
            'size': (TAG_LIST, (TAG_INT, size)),
            'palette': (TAG_LIST, (TAG_COMPOUND, paleta)),
            'blocks': (TAG_LIST, (TAG_COMPOUND, lista)),
            'entities': (TAG_LIST, (TAG_COMPOUND, []))}
    data = struct.pack('>b', TAG_COMPOUND) + _s('') + _payload(TAG_COMPOUND, root)
    with gzip.open(sciezka, 'wb') as f:
        f.write(data)
    return {'size': size, 'offset': (x0, y0, z0), 'blokow': len(lista), 'paleta': len(paleta)}
