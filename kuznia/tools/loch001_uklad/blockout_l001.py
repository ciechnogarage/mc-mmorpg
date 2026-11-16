#!/usr/bin/env python3
"""Loch 001 remake, krok 4: ciasny blockout z uklad_l001.py (jedno źródło układu).

Wynik (katalog --out, domyślnie ./wynik):
  datapack/l1r_blockout/  funkcja l1r_bo:buduj (masa skały, sale, korytarze, tarasy, filary, bramy zamknięte)
  trasa.json              dla scenariusza bota: punkty trasy, wrogowie, bramy, pudła sal, ogniska (współrzędne świata)
Przed wypuszczeniem bota model wokseli w Pythonie sprawdza BFS-em, że każdy odcinek trasy jest przechodni
(krok w górę 1 bl., zeskok do 3 bl., 2 bl. luzu nad głową, cel do 1 bl.; bramy otwarte po kolei jak w grze).

Uproszczenia blockoutu (bez detali, krok 4): sale = zaokrąglone prostokąty z pełnej skały, bez wody w jeziorku,
bez półki w Rozstajach, bez drzew/belek/sekretów; drabina z dna rozpadliny = rampa (pathfinder bota nie wspina się
po drabinie); bramy otwiera scenariusz RCON-em w chwili, gdy spełniony jest warunek (w lochu zrobią to funkcje MD);
Strażnika Sadu nie ma (brak moba MM), krata Legowiska i mur Groty Dzwonu za graczem się nie zamykają.
Uruchom: python3 blockout_l001.py [--out DIR]
"""
import json, math, os, sys
from collections import deque
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from uklad_l001 import SALE, ZNACZNIKI, KORYTARZE, TRASA

OX, OZ, OY = 200, 200, 40              # lokalne (0,0,0) -> świat (200, 40, 200); płaski świat klona, grunt y -59
X0, X1, Z0, Z1, Y0, Y1 = 8, 115, 0, 151, -24, 18   # masa skały (lokalnie)
SKALA, TARAS, BRAMA = "minecraft:stone", "minecraft:mossy_cobblestone", "minecraft:oak_wood"
WYS_KOR = 3                             # luz korytarza nad podłogą

MOBY = {"W": "L1R_WILK", "K": "L1R_KORZENIAK", "L": "L1R_LUCZNIK", "P": "L1R_PNIACZEK", "A": "L1R_KOTWA",
        "S": "L1R_SIEWCA", "R": "L1R_KOROWIEC", "E": "L1R_SOWIEDZ", "p": "L1R_PAK"}
# podwyższenia z opisu znaczników: (x, z) -> (wysokość, kierunek schodów dx,dz)
PODEST = {(88, 77): (3, (1, 0)), (102, 86): (6, (-1, 0)), (88, 52): (4, (1, 0)), (32, 52): (2, (-1, 0))}
# korytarze bez powrotu: podłoga na poziomie sali startowej, zeskok przy wejściu do sali docelowej
SPADEK = {("S4W", "J"), ("S4E", "J")}
# bramy: nazwa -> (pudło lokalne x0,y0,z0,x1,y1,z1; tag mobów, po których śmierci się otwiera)
BRAMY = {
    "korzenie_jeziorka": ((58, -11, 44, 62, -8, 45), "bo_L1R_KOTWA"),       # J -> S5, po obu Kotwach
    "mur_bramy":         ((57, -11, 19, 63, -8, 20), "bo_L1R_KOROWIEC"),    # S5 -> O2 (w grze: Korzeń z pąka)
    "krata_o2":          ((59, -15, 10, 61, -9, 11), "bo_L1R_SOWIEDZ"),     # O2 -> S7, skrót po Sowiedziu
}
OGNISKA = {"S1": (60, 137), "O2": (60, 15)}


class Model:
    """Bryła lokalna: 1 = skała. Każda operacja idzie równocześnie do modelu i do listy komend fill."""
    def __init__(s):
        s.nx, s.ny, s.nz = X1 - X0 + 1, Y1 - Y0 + 1, Z1 - Z0 + 1
        s.b = bytearray(s.nx * s.ny * s.nz)
        s.cmd = []

    def _i(s, x, y, z):
        return ((x - X0) * s.ny + (y - Y0)) * s.nz + (z - Z0)

    def jest(s, x, y, z):
        if not (X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1):
            return x < X0 or x > X1 or z < Z0 or z > Z1 or y < Y0   # poza masą: boki i dół pełne, góra pusta
        return s.b[s._i(x, y, z)] == 1

    def fill(s, x0, y0, z0, x1, y1, z1, blok, model=True):
        x0, x1, y0, y1, z0, z1 = min(x0, x1), max(x0, x1), min(y0, y1), max(y0, y1), min(z0, z1), max(z0, z1)
        v = 1 if blok != "minecraft:air" else 0
        if model:
            for x in range(max(x0, X0), min(x1, X1) + 1):
                for y in range(max(y0, Y0), min(y1, Y1) + 1):
                    base = ((x - X0) * s.ny + (y - Y0)) * s.nz
                    for z in range(max(z0, Z0), min(z1, Z1) + 1):
                        s.b[base + z - Z0] = v
        assert (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) <= 32768, "fill > 32768"
        s.cmd.append(f"fill {x0+OX} {y0+OY} {z0+OZ} {x1+OX} {y1+OY} {z1+OZ} {blok}")


def wewn(klucz, x, z):
    _, x0, z0, x1, z1, _, _ = SALE[klucz]
    cx, cz, rx, rz = (x0 + x1) / 2, (z0 + z1) / 2, (x1 - x0) / 2, (z1 - z0) / 2
    dx, dz = abs(x - cx) / max(rx, 1), abs(z - cz) / max(rz, 1)
    return dx < 1 and dz < 1 and dx ** 4 + dz ** 4 < 1.0


def podloga(klucz):
    return SALE[klucz][5] if klucz in SALE else None


def buduj():
    m = Model()
    for x in range(X0, X1 + 1, 16):              # masa skały: kolumny 16x16, 43 wys. = 11 008 bl. na fill
        for z in range(Z0, Z1 + 1, 16):
            m.fill(x, Y0, z, min(x + 15, X1), Y1, min(z + 15, Z1), SKALA)
    for k, (_, x0, z0, x1, z1, y, h) in SALE.items():          # sale: rząd po rzędzie
        for z in range(z0, z1 + 1):
            xs = [x for x in range(x0, x1 + 1) if wewn(k, x, z)]
            if xs:
                m.fill(xs[0], y + 1, z, xs[-1], y + h, z, "minecraft:air")
    for (x, z), (dh, (sx, sz)) in PODEST.items():              # tarasy 5x5 ze schodami szer. 2 do środka sali
        k = next(k for k, v in ZNACZNIKI.items() if any((a, b) == (x, z) for a, b, *_ in v))
        y = SALE[k][5]
        m.fill(x - 2, y + 1, z - 2, x + 2, y + dh, z + 2, TARAS)
        for i in range(1, dh):
            px, pz = x + sx * (2 + i), z + sz * (2 + i)
            ox, oz = (0, 1) if sx else (1, 0)
            m.fill(px, y + 1, pz, px + ox, y + dh - i, pz + oz, TARAS)
    for k, v in ZNACZNIKI.items():                             # osłony: filar/pień 2x2 do stropu, głaz/gruz 3x3 x2
        if k not in SALE:
            continue
        y, h = SALE[k][5], SALE[k][6]
        for x, z, c, opis in v:
            if c != "o":
                continue
            if any(s in opis for s in ("głaz", "gruz")):
                m.fill(x - 1, y + 1, z - 1, x + 1, y + 2, z + 1, TARAS)
            else:
                m.fill(x, y + 1, z, x + 1, y + h, z + 1, TARAS)
    m.fill(59, -11, 94, 61, -11, 94, SKALA)                    # rampa R -> S2 (zamiast drabiny), 1 bl. na stopień
    for z in range(94, 103):
        m.fill(59, -11, z, 61, -12 + (z - 93), z, SKALA)
    # od z=103: szer. 3 kładzie podłogę od z-1, więc start w z=102 dawał na rampie stopień 2 bl. (16.11)
    korytarz(m, [(60, 103), (60, 107)], 3, -3, -3)
    for od, do, pkt, szer, _ in KORYTARZE:
        ya = podloga(od)
        yb = podloga(do) if do in SALE else None
        if do == "S7":                                         # skrót O2 -> tunel S7: wysokość tunelu w x=60
            yb = None
        korytarz(m, pkt, szer, ya, yb, spadek=(od, do) in SPADEK, nazwa=(od, do))
    for nazwa, ((x0, y0, z0, x1, y1, z1), _) in BRAMY.items():
        m.fill(x0, y0, z0, x1, y1, z1, BRAMA, model=False)   # w modelu bramy to zbiór `zamk`, zdejmowany po kolei
    return m


TUNEL_S7 = {}   # (x, z) -> y podłogi tunelu S6->S8 (do skrótu z O2)


def korytarz(m, pkt, szer, ya, yb, spadek=False, nazwa=None):
    dl = [math.dist(pkt[i], pkt[i + 1]) for i in range(len(pkt) - 1)]
    cal = sum(dl) or 1
    if yb is None:                                             # skrót O2 -> S7: koniec na podłodze tunelu
        yb = TUNEL_S7.get(tuple(pkt[-1]), ya)
    a = szer // 2
    przeszlo, prev = 0.0, None
    for i, d in enumerate(dl):
        (xa, za), (xb, zb) = pkt[i], pkt[i + 1]
        n = max(1, int(math.ceil(d * 2)))
        for j in range(n + 1):
            t = j / n
            x, z = round(xa + (xb - xa) * t), round(za + (zb - za) * t)
            f = (przeszlo + d * t) / cal
            # zjazd bez powrotu: podłoga 3 bl. nad salą docelową (zeskok 3 = bez obrażeń; pathfinder bota liczy
            # stopy->blok lądowania i bierze najwyżej 4, więc zeskok 4 bl. był dla niego ścianą, 16.11)
            y = max(min(ya, yb + 3), yb) if spadek else round(ya + (yb - ya) * f)
            if (x, z) == prev:
                continue
            prev = (x, z)
            if nazwa == ("S6", "S8"):
                TUNEL_S7[(x, z)] = y
            m.fill(x - a, y, z - a, x + a, y, z + a, SKALA)
            m.fill(x - a, y + 1, z - a, x + a, y + WYS_KOR, z + a, "minecraft:air")
        przeszlo += d


def stoi_y(m, x, z, y_ok):
    """Y bloku podłogi pod (x,z) najbliżej oczekiwanego poziomu y_ok (most nad rozpadliną = dwa poziomy)."""
    kand = [y for y in range(Y0, Y1) if m.jest(x, y, z) and not m.jest(x, y + 1, z) and not m.jest(x, y + 2, z)]
    if not kand:
        return None
    return min(kand, key=lambda y: (abs(y - y_ok), -y))


def bfs(m, start, cel, zamkniete, r=1):
    """Przechodniość jak gracz/bot: krok w górę 1, zeskok do 3, 2 bl. luzu, cel do 1 bl. (r=2 zaliczał cel przez
    ścianę grubości 1, 16.11 J/R). zamkniete = zbiór bloków bram."""
    pelny = lambda x, y, z: m.jest(x, y, z) or (x, y, z) in zamkniete
    stoj = lambda x, y, z: pelny(x, y, z) and not pelny(x, y + 1, z) and not pelny(x, y + 2, z)
    if not stoj(*start):   # start w ścianie = BFS „przechodził” przez skałę (16.11, J(60,58) -> R)
        return False
    q, wid = deque([start]), {start}
    while q:
        x, y, z = q.popleft()
        if abs(x - cel[0]) <= r and abs(z - cel[2]) <= r and abs(y - cel[1]) <= 2:
            return True
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            for dy in (0, 1, -1, -2, -3):
                ny = y + dy
                if dy == 1 and pelny(x, y + 3, z):
                    continue
                if dy < 0 and any(pelny(nx, y + k, nz) for k in range(dy + 1, 3)):
                    continue
                if stoj(nx, ny, nz):
                    if (nx, ny, nz) not in wid:
                        wid.add((nx, ny, nz)); q.append((nx, ny, nz))
                    break
    return False


def schody(m):
    """Na każdym stopniu +1 kładzie stone_stairs (pół bloku), żeby pod górę szło się bez skakania, jak po
    schodach w docelowym lochu. 16.11: rampa R -> S2 z pełnych bloków 1:1 = bot (sprint+skok) cofany przez
    serwer co tick (80-370 cofnięć na 6 stopniach), a gracz musiałby skakać 9 razy z rzędu."""
    stoj = lambda x, y, z: m.jest(x, y, z) and not m.jest(x, y + 1, z) and not m.jest(x, y + 2, z)
    KIER = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
    n = 0
    for x in range(X0, X1 + 1):
        for z in range(Z0, Z1 + 1):
            for y in range(Y0, Y1 - 3):
                if not stoj(x, y, z) or m.jest(x, y + 3, z):
                    continue
                for (dx, dz), f in KIER.items():
                    if stoj(x + dx, y + 1, z + dz) and not m.jest(x + dx, y + 4, z + dz):
                        m.cmd.append(f"setblock {x+OX} {y+1+OY} {z+OZ} minecraft:stone_stairs[facing={f}]")
                        n += 1
                        break
    return n


def main():
    out = sys.argv[sys.argv.index("--out") + 1] if "--out" in sys.argv else os.path.join(os.path.dirname(__file__), "wynik")
    m = buduj()
    n_schody = schody(m)   # po BFS-owym modelu: schody nie zmieniają przechodniości (krok +1 i tak wolno)
    sw = lambda x, y, z: {"x": x + OX + 0.5, "y": y + OY + 1, "z": z + OZ + 0.5}

    # punkty trasy (y = blok podłogi najbliżej podłogi sali; S7 = tunel)
    trasa = []
    for sala, (x, z) in TRASA:
        y_ok = podloga(sala) if sala in SALE else TUNEL_S7.get((x, z), -18)
        y = stoi_y(m, x, z, y_ok)
        if y is None:   # punkt drzwi na krawędzi sali: krok do środka
            for d in range(1, 4):
                kr = next(((x + a, z + b) for a, b in ((d, 0), (-d, 0), (0, d), (0, -d))
                           if stoi_y(m, x + a, z + b, y_ok) is not None), None)
                if kr is not None:   # punkt przesuwa się do środka razem z x,z (16.11: zostawał w ścianie J/R)
                    x, z = kr; y = stoi_y(m, x, z, y_ok)
                    break
        trasa.append({"sala": sala, "lok": [x, y, z], **sw(x, y, z)})
    if trasa[10]["sala"] == "R":   # wierzch rampy R -> S2
        trasa[10].update(lok=[60, -3, 102], **sw(60, -3, 102))

    wrogowie = []
    for k, v in ZNACZNIKI.items():
        for x, z, c, opis in v:
            if c in MOBY:
                y = SALE[k][5] + (PODEST[(x, z)][0] if (x, z) in PODEST else 0)
                wrogowie.append({"sala": k, "mob": MOBY[c], "lok": [x, y, z], **sw(x, y, z)})

    zamk = {}
    for nazwa, ((x0, y0, z0, x1, y1, z1), _) in BRAMY.items():
        zamk[nazwa] = {(x, y, z) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1) for z in range(z0, z1 + 1)}
    # BFS odcinek po odcinku; bramę zdejmujemy, gdy trasa minęła jej warunek (jak w grze)
    otwarte_po = {"korzenie_jeziorka": 17, "mur_bramy": 19, "krata_o2": 99}
    bledy = []
    for i in range(len(trasa) - 1):
        a, b = trasa[i]["lok"], trasa[i + 1]["lok"]
        zam = set().union(*(zamk[n] for n, j in otwarte_po.items() if i < j))
        if a[1] is None or b[1] is None or not bfs(m, tuple(a), tuple(b), zam):
            bledy.append(f"{trasa[i]['sala']}{a} -> {trasa[i+1]['sala']}{b}")
    # bramy faktycznie trzymają (przy zamkniętych nie ma drogi J -> S5 ani S5 -> O2)
    trzyma = {"korzenie_jeziorka": bfs(m, tuple(trasa[16]["lok"]), tuple(trasa[18]["lok"]), zamk["korzenie_jeziorka"]),
              "mur_bramy": bfs(m, tuple(trasa[18]["lok"]), tuple(trasa[20]["lok"]), zamk["mur_bramy"])}

    pudla = {k: [x0 + OX, y + OY, z0 + OZ, x1 + OX, y + h + OY, z1 + OZ] for k, (_, x0, z0, x1, z1, y, h) in SALE.items()}
    bramy = {n: {"fill": f"{x0+OX} {y0+OY} {z0+OZ} {x1+OX} {y1+OY} {z1+OZ}", "tag": t}
             for n, ((x0, y0, z0, x1, y1, z1), t) in BRAMY.items()}
    ogn = {k: sw(x, SALE[k][5], z) for k, (x, z) in OGNISKA.items()}
    obszar = [X0 + OX, Y0 + OY, Z0 + OZ, X1 + OX, Y1 + OY, Z1 + OZ]

    os.makedirs(os.path.join(out, "datapack/l1r_blockout/data/l1r_bo/function"), exist_ok=True)
    with open(os.path.join(out, "datapack/l1r_blockout/pack.mcmeta"), "w") as f:
        json.dump({"pack": {"description": "Loch 001 remake: blockout (krok 4, tylko klon)",
                            "pack_format": 94, "min_format": 94, "max_format": 99}}, f)
    with open(os.path.join(out, "datapack/l1r_blockout/data/l1r_bo/function/buduj.mcfunction"), "w") as f:
        f.write("# wygenerowane: kuznia/tools/loch001_uklad/blockout_l001.py\n" + "\n".join(m.cmd) + "\n")
    with open(os.path.join(out, "datapack/l1r_blockout/data/l1r_bo/function/usun.mcfunction"), "w") as f:
        f.write("\n".join(c.rsplit(" ", 1)[0] + " minecraft:air" for c in m.cmd if c.endswith(SKALA)) + "\n")
    with open(os.path.join(out, "trasa.json"), "w") as f:
        json.dump({"trasa": trasa, "wrogowie": wrogowie, "bramy": bramy, "sale": pudla, "ogniska": ogn,
                   "obszar": obszar, "forceload": f"{obszar[0]} {obszar[2]} {obszar[3]} {obszar[5]}"}, f, ensure_ascii=False, indent=1)

    nch = ((obszar[3] >> 4) - (obszar[0] >> 4) + 1) * ((obszar[5] >> 4) - (obszar[2] >> 4) + 1)
    print(f"komendy fill/setblock: {len(m.cmd)} (schody: {n_schody}), wrogowie: {len(wrogowie)}, punkty trasy: {len(trasa)}, chunki forceload: {nch}")
    print("trasa BFS:", "OK" if not bledy else "BRAK DROGI: " + "; ".join(bledy))
    print("bramy trzymają (True = jest obejście, błąd):", trzyma)
    return 0 if not bledy and not any(trzyma.values()) else 1


if __name__ == "__main__":
    sys.exit(main())
