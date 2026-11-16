#!/usr/bin/env python3
"""Znacznik tieru dla ikon profesyjnych — jedna mala "iskra" w prawym gornym rogu.

DLACZEGO PRAWY GORNY: policzone, nie zgadniete. Pomiar zajetosci rogow 5x5 na 42
gotowych ikonach dal: LG 4.88/25, PG 4.57/25, LD 6.52/25, PD 6.24/25 srednio.
Prawy gorny jest najrzadziej zajety, wiec znacznik najrzadziej zjada sylwetke.

DLACZEGO TAK MALY: zalecenie bylo "oszczednie" — nie przeprojektowujemy sylwetek,
tylko dokladamy wspolny sygnal. Klejnocik 4x4 z ciemnym pierscieniem czyta sie jako
celowa plakietka, a nie jako czesc przedmiotu, nawet gdy naloÅ¼y sie na kontur.

TIER LICZONY Z RECEPTUR, nie na oko (patrz classify_tiers.py):
  T1 zbierany     - nigdzie nie jest wyjsciem receptury warsztatu
  T2 przetworzony - wyjscie receptury o wymaganym poziomie < 45
  T3 mistrzowski  - wyjscie receptury o poziomie >= 45 albo 'MASTER' w id
"""

OUTLINE = (24, 22, 30, 255)

# kolor / rozjasnienie klejnotu per tier
# Kolory dobrane na KONTRAST BARWY, nie jasnosci. Pierwsza wersja (matowy braz /
# jasne srebro / zloto) dzialala tylko na ciemnym panelu MMOInventory: w oknie
# warsztatu, ktore ma vanillowe jasnoszare tlo (~139,139,139), jasnosrebrna
# plakietka T2 znikala. Braz jest cieply, stal chlodna-niebieska, zloto zolte -
# kazda odroznia sie od szarosci ODCIENIEM, wiec czyta sie na obu tlach.
TIER_COLORS = {
    1: ((156, 104, 56), (200, 148, 96)),    # braz  - zbierany
    2: ((116, 148, 192), (170, 200, 236)),  # stal  - przetworzony
    3: ((216, 172, 48), (248, 214, 112)),   # zloto - mistrzowski
}

# lewy gorny rog plakietki (klejnot 4x4 -> x 11..14, y 1..4)
X0, Y0 = 11, 1


def stamp(im, tier):
    """Wypala plakietke tieru na obrazku 16x16 RGBA (w miejscu)."""
    if tier not in TIER_COLORS:
        raise ValueError(f"nieznany tier: {tier}")
    base, hi = TIER_COLORS[tier]
    base = base + (255,)
    hi = hi + (255,)
    px = im.load()
    for dy in range(4):
        for dx in range(4):
            x, y = X0 + dx, Y0 + dy
            on_edge = dx in (0, 3) or dy in (0, 3)
            px[x, y] = OUTLINE if on_edge else base
    # jeden piksel polysku, zeby plakietka nie byla plaska plama
    px[X0 + 1, Y0 + 1] = hi
    return im
