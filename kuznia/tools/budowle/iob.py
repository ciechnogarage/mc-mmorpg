"""Czytnik obiektow Iris (.iob, format 'Iris V2 IOB;'). Zero zaleznosci.
Uklad pliku (zmierzony na packu overworld): int w,h,d | UTF 'Iris V2 IOB;' | short N palety | N x UTF blockdata |
int M blokow | M x (short x, short y, short z, short indeks_palety) | [int K stanow kafelkow ...].
Wspolrzedne blokow sa wzgledne wobec srodka obiektu (moga byc ujemne)."""
import struct


def _utf(b, i):
    n = struct.unpack_from('>H', b, i)[0]
    return b[i + 2:i + 2 + n].decode('utf-8', 'replace'), i + 2 + n


def czytaj(sciezka):
    b = open(sciezka, 'rb').read()
    w, h, d = struct.unpack_from('>iii', b, 0)
    sig, i = _utf(b, 12)
    if sig != 'Iris V2 IOB;':
        raise ValueError(f'{sciezka}: nieznany format {sig!r}')
    n = struct.unpack_from('>h', b, i)[0]; i += 2
    paleta = []
    for _ in range(n):
        s, i = _utf(b, i); paleta.append(s)
    m = struct.unpack_from('>i', b, i)[0]; i += 4
    bloki = {}
    for _ in range(m):
        x, y, z, k = struct.unpack_from('>hhhh', b, i); i += 8
        bloki[(x, y, z)] = paleta[k]
    return {'w': w, 'h': h, 'd': d, 'bloki': bloki, 'reszta_bajtow': len(b) - i}


if __name__ == '__main__':
    import sys
    for p in sys.argv[1:]:
        o = czytaj(p)
        xs = [k[0] for k in o['bloki']]; ys = [k[1] for k in o['bloki']]; zs = [k[2] for k in o['bloki']]
        print(p, f"{o['w']}x{o['h']}x{o['d']}", len(o['bloki']), 'blokow', 'x', min(xs), max(xs), 'y', min(ys), max(ys),
              'z', min(zs), max(zs), 'reszta', o['reszta_bajtow'])
