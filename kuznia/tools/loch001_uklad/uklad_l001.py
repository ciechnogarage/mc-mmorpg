#!/usr/bin/env python3
"""Loch 001 remake: układ sal (krok 3 „przebieg na stronie”), jedno źródło dla rzutów i blockoutu.

Współrzędne lokalne instancji: X 0..120 (wschód +), Z 0..150 (północ = mniejsze Z = głębiej), y = poziom podłogi
względem Obozu (S1 = 0). Uruchom: python3 uklad_l001.py [--md] -> rzuty ASCII + kontrole (kolizje, trasa, odcinki).
Rzut: 1 znak = 1 bl. w X, 1 wiersz = 2 bl. w Z (znak terminala jest ok. 2x wyższy niż szerszy, więc sala wygląda
na kwadrat). Nic tu nie jest zbudowane; blockout (krok 4) czyta SALE/KORYTARZE stąd.
"""
import math, sys

# nazwa: (x0, z0, x1, z1, y podłogi, wysokość stropu)
SALE = {
    "S1": ("Obóz pod szybem",      48, 129,  72, 147,   0,  8),
    "S2": ("Rozstaje",             48, 104,  72, 120,  -3,  9),
    "S3W": ("Zawalone Piwnice",    16,  77,  44,  99,  -5,  8),
    "S3E": ("Podziemny Sad",       75,  75, 105, 101,  -5, 14),
    "S4W": ("Kotwa Zachodnia",     15,  47,  37,  69,  -8, 10),
    "S4E": ("Kotwa Wschodnia",     83,  47, 105,  69,  -8, 10),
    "J":   ("Podziemne jeziorko",  52,  46,  68,  58, -12,  9),
    "R":   ("Dno rozpadliny",      54,  58,  66, 103, -12, 20),   # korytarz powrotny pod mostem
    "S5":  ("Brama z korzeni",     47,  20,  73,  40, -12, 10),
    "O2":  ("Ognisko 2",           54,  12,  66,  19, -12,  6),
    "S6":  ("Legowisko",           85,   9, 111,  35, -15, 11),
    "S8":  ("Grota Dzwonu",        12,   6,  40,  34, -20, 32),   # komin do powierzchni
}

# znaczniki: sala -> [(x, z, znak, opis)]
ZNACZNIKI = {
    "S1": [(60,144,"^","szyb z Polany (lądowanie)"),(60,137,"F","ognisko 1"),(53,134,"M","manekin"),
           (67,134,"M","manekin"),(60,131,"p","pąk na pokaz -> Pniaczek, jeśli nie zdepczesz"),(60,129,"D","wyjście")],
    "S2": [(60,118,"=","półka +3 przy wejściu (skąd zaskakujesz)"),(57,108,"W","Wilk śpi (niższa część -2)"),
           (63,107,"W","Wilk śpi"),(48,110,"D","do S3W, łuna Kotwy"),(72,110,"D","do S3E, łuna Kotwy"),
           (60,104,"H","drabina z korzeni z dna rozpadliny (powrót, tylko w górę)")],
    "S3W": [(44,96,"D","z Rozstajów"),(24,87,"K","Korzeniak pod spróchniałą belką"),(27,88,"-","belka"),
            (36,80,"W","Wilk w zasadzce za gruzem"),(38,82,"o","gruz"),(17,85,"*","pęknięta ściana (sekret)"),
            (44,84,"D","most na wschód"),(26,77,"D","do Kotwy Zach.")],
    "S3E": [(75,97,"D","z Rozstajów"),(75,84,"D","most z zachodu"),(88,77,"L","Łucznik, taras +3"),
            (102,86,"L","Łucznik, taras +6"),(82,93,"o","pień"),(87,87,"o","głaz"),(93,91,"o","pień"),
            (97,95,"o","głaz"),(84,82,"P","Pniaczek w kwiatach (budzi go strzała)"),
            (94,82,"P","Pniaczek w kwiatach"),(90,88,"T","drzewa w snopie słońca"),
            (104,77,"*","dziupla na tarasie +9 (sekret)"),(96,75,"D","do Kotwy Wsch.")],
    "S4W": [(26,69,"D","z Piwnic"),(22,50,"A","Kotwa w masie korzeni"),(32,52,"S","Siewca na kłębie +2"),
            (20,60,"K","Korzeniak"),(34,64,"*","skrytka zasypana korzeniami (sekret)"),
            (35,49,"v","zjazd do jeziorka, otwiera się po Kotwie (bez powrotu)")],
    "S4E": [(96,69,"D","z Sadu"),(98,50,"A","Kotwa"),(88,52,"L","Łucznik na pniu +4"),
            (92,64,"P","Pniaczek z kwiatów (zasadzka przy wejściu)"),(96,62,"P","Pniaczek"),(100,64,"P","Pniaczek"),
            (85,49,"v","zjazd do jeziorka po Kotwie")],
    "J": [(52,50,"<","wpada zjazd z Kotwy Zach. (do wody)"),(68,50,">","wpada zjazd z Kotwy Wsch."),
          (60,52,"~","woda"),(60,46,"X","przejście do Bramy: grube korzenie, puszczają po obu Kotwach"),
          (60,58,"D","dno rozpadliny -> Rozstaje")],
    "R": [(60,84,"*","dziupla z łupem w korzeniu pod mostem (widać most nad głową)"),(60,103,"H","drabina do S2")],
    "S5": [(60,40,"D","z jeziorka"),(60,25,"R","Korowiec przed murem (budzi się, gdy wejdziesz)"),
           (54,21,"P","Pniaczek ze ściany"),(60,21,"P","Pniaczek ze ściany"),(66,21,"P","Pniaczek ze ściany"),
           (52,31,"o","filar (do obejścia Korowca)"),(68,31,"o","filar"),(60,20,"X","mur Bramy: pęka od Korzenia")],
    "O2": [(60,15,"F","ognisko 2 (za murem)"),(66,15,"D","do Legowiska"),
           (60,12,"x","krata do tunelu S7: otwiera się po Sowiedziu (skrót po śmierci)")],
    "S6": [(85,15,"X","wejście, krata zamyka się za graczem"),(100,18,"E","Sowiedź"),(106,28,"e","gniazdo młodych"),
           (92,14,"o","korzeń-filar"),(104,13,"o","korzeń-filar"),(97,28,"o","korzeń-filar"),(90,9,"D","do tunelu S7")],
    "S8": [(40,8,"X","wejście, mur korzeni do końca walki"),(26,10,"Q","dzwon w kominie"),(26,17,"B","Strażnik Sadu"),
           (18,14,"o","głaz"),(34,14,"o","głaz"),(18,28,"o","pień"),(34,28,"o","pień"),
           (26,12,"C","skrzynia po bossie (S9 Rozkwit)")],
}

# korytarze: (od, do, [(x,z), ...], szerokość, uwaga)
KORYTARZE = [
    ("S1","S2",  [(60,129),(60,120)], 3, ""),
    ("S2","S3W", [(48,110),(44,96)], 3, "łuna Kotwy Zach."),
    ("S2","S3E", [(72,110),(75,97)], 3, "łuna Kotwy Wsch."),
    ("S3W","S4W",[(26,77),(26,69)], 4, ""),
    ("S3E","S4E",[(96,75),(96,69)], 4, ""),
    ("S3W","S3E",[(44,84),(75,84)], 3, "most nad rozpadliną, upadek = siatka korzeni -> dno"),
    ("S4W","J",  [(35,49),(52,50)], 3, "zjazd, bez powrotu"),
    ("S4E","J",  [(85,49),(68,50)], 3, "zjazd, bez powrotu"),
    ("J","S5",   [(60,46),(60,40)], 5, "grube korzenie do obu Kotw"),
    ("J","R",    [(60,56),(60,61)], 3, "wyjście na dno rozpadliny (sale stykają się brzegami, a brzeg superelipsy zostaje skałą)"),
    ("S5","O2",  [(60,20),(60,19)], 5, "mur Bramy"),
    ("O2","S6",  [(66,15),(85,15)], 3, ""),
    ("S6","S8",  [(90,9),(90,4),(40,4),(40,8)], 3, "S7 Korzenny Tunel: Kwiat bez cienia, dzwon coraz bliżej"),
    ("O2","S7",  [(60,12),(60,4)], 3, "skrót, krata po Sowiedziu"),
]
SEKRET_S7 = (72, 4, "*", "Kwiat bez cienia (sekret)")

# trasa gracza (kolejność dolin dowolna; tu: najpierw zachód)
TRASA = [("S1",(60,144)),("S1",(60,129)),("S2",(60,118)),("S2",(48,110)),("S3W",(44,96)),("S3W",(26,77)),
         ("S4W",(26,69)),("S4W",(35,49)),("J",(52,50)),("J",(60,58)),("R",(60,103)),("S2",(72,110)),
         ("S3E",(75,97)),("S3E",(96,75)),("S4E",(96,69)),("S4E",(85,49)),("J",(68,50)),("J",(60,46)),
         ("S5",(60,40)),("S5",(60,20)),("O2",(66,15)),("S6",(85,15)),("S6",(90,9)),("S7",(90,4)),
         ("S7",(40,4)),("S8",(40,8)),("S8",(26,17))]
CELE = [(60,137),(60,131),(57,108),(24,87),(36,80),(17,85),(22,50),(32,52),(20,60),(34,64),(60,52),(60,84),
        (88,77),(102,86),(104,77),(90,88),(98,50),(88,52),(96,62),(60,25),(60,15),(100,18),(72,4),(60,4),(26,17)]


def rzut(klucz, margines=2):
    nazwa, x0, z0, x1, z1, y, h = SALE[klucz]
    zn = {(x, z): c for x, z, c, _ in ZNACZNIKI.get(klucz, [])}
    cx, cz, rx, rz = (x0+x1)/2, (z0+z1)/2, (x1-x0)/2, (z1-z0)/2
    wiersze = []
    for z in range(z0-margines, z1+margines+1, 2):
        w = ""
        for x in range(x0-margines, x1+margines+1):
            hit = next((c for (mx, mz), c in zn.items() if mx == x and mz in (z, z+1)), None)
            # grota: zaokrąglony prostokąt (narożniki ścięte), nie elipsa, żeby było miejsce na walkę
            dx, dz = abs(x-cx)/max(rx, 1), abs(z-cz)/max(rz, 1)
            wewn = dx < 1 and dz < 1 and (dx**4 + dz**4) < 1.0
            w += hit if hit else ("." if wewn else "#")
        wiersze.append(w)
    return wiersze


def mapa(sx=2, sz=4):
    """Cały loch: 1 znak = sx bl. w X, 1 wiersz = sz bl. w Z. Sale '.', korytarze '+', etykiety w środku sal."""
    W, H = 122 // sx, 152 // sz
    g = [[" "] * W for _ in range(H)]
    for kl, (n, x0, z0, x1, z1, y, h) in SALE.items():
        for z in range(z0, z1 + 1, sz):
            for x in range(x0, x1 + 1, sx):
                g[z // sz][x // sx] = ":" if kl == "R" else "."
    for _, _, pkt, _, _ in KORYTARZE + [("S7", "", [(60, 4), (40, 4)], 3, "")]:
        for (ax, az), (bx, bz) in zip(pkt, pkt[1:]):
            n = max(1, int(math.dist((ax, az), (bx, bz))))
            for t in range(n + 1):
                x, z = ax + (bx - ax) * t / n, az + (bz - az) * t / n
                c = g[int(z) // sz][int(x) // sx]
                g[int(z) // sz][int(x) // sx] = "+" if c == " " else c
    for kl, (n, x0, z0, x1, z1, y, h) in SALE.items():
        lab = kl if kl != "R" else "rozp."
        z, x = ((z0 + z1) // 2) // sz, ((x0 + x1) // 2) // sx - len(lab) // 2
        for i, ch in enumerate(lab):
            g[z][x + i] = ch
    g[4 // sz][66 // sx:66 // sx + 2] = list("S7")
    return ["".join(r).rstrip() for r in g]


def kontrole():
    bledy = []
    k = list(SALE)
    for i, a in enumerate(k):
        for b in k[i+1:]:
            A, B = SALE[a][1:5], SALE[b][1:5]
            if A[0] < B[2] and B[0] < A[2] and A[1] < B[3] and B[1] < A[3] and {a, b} not in ({"J","R"},{"S5","O2"}):
                bledy.append(f"kolizja {a}/{b}")
    xs = [v[1] for v in SALE.values()] + [v[3] for v in SALE.values()]
    zs = [v[2] for v in SALE.values()] + [v[4] for v in SALE.values()]
    ramka = (max(xs)-min(xs), max(zs)-min(zs))
    dl = sum(math.dist(TRASA[i][1], TRASA[i+1][1]) for i in range(len(TRASA)-1))
    # najdłuższy odcinek trasy bez celu (karta §3: <= 25 bl.): próbki co 1 bl.; cel = wnętrze sali albo znacznik <= 6 bl.
    pts = []
    for i in range(len(TRASA)-1):
        (ax, az), (bx, bz) = TRASA[i][1], TRASA[i+1][1]
        n = max(1, int(math.dist((ax, az), (bx, bz))))
        pts += [(ax+(bx-ax)*t/n, az+(bz-az)*t/n) for t in range(n)]
    naj = bieg = 0
    def w_sali(p):  # wnętrze sali z walką/łupem = cel (dno rozpadliny R to korytarz, nie sala)
        return any(v[1] <= p[0] <= v[3] and v[2] <= p[1] <= v[4] for k, v in SALE.items() if k != "R")
    for p in pts:
        if w_sali(p) or any(math.dist(p, c) <= 6 for c in CELE):
            bieg = 0
        else:
            bieg += 1; naj = max(naj, bieg)
    return bledy, ramka, dl, naj


if __name__ == "__main__":
    md = "--md" in sys.argv
    print("Cały loch (1 znak = 2 bl. X, 1 wiersz = 4 bl. Z; góra = północ = głębiej):")
    print("\n".join(mapa()))
    for kl in SALE:
        nazwa, x0, z0, x1, z1, y, h = SALE[kl]
        print(f"\n{kl} {nazwa}: {x1-x0}x{z1-z0} bl., podłoga y{y:+d}, strop {h} bl.")
        if kl != "R":
            print("\n".join(rzut(kl)))
        for x, z, c, o in ZNACZNIKI.get(kl, []):
            print(f"  {c} ({x},{z}) {o}")
    bledy, ramka, dl, naj = kontrole()
    print(f"\nkontrole: kolizje={bledy or 'brak'} ramka={ramka[0]}x{ramka[1]} bl. trasa={dl:.0f} bl. "
          f"(~{dl/4.3:.0f} s marszu, ~{dl/5.6:.0f} s sprintem) najdłuższy odcinek bez celu={naj} bl.")
