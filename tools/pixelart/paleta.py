"""Wspolna paleta materialow — zgodna z karta stylu (docs/karta-stylu-grafiki.md).
Kazdy material: tony od najjasniejszego, sasiednie roznia sie jasnoscia Y o >= ~20.
Plyn/szklo/korek pochodza z zaakceptowanej mikstury; nowe materialy dopisuje sie
dopiero po sprawdzeniu na grafice i akceptacji wlasciciela."""

OBRYS = (28, 12, 24)

PLYN_CZERWONY = [(255, 138, 96), (244, 110, 100), (238, 78, 76), (210, 40, 60), (128, 16, 60), (72, 10, 48)]
SZKLO = [(255, 255, 255), (170, 196, 220), (118, 132, 160), (72, 76, 98)]
KOREK = [(226, 178, 118), (168, 112, 64), (104, 64, 38), (52, 28, 18)]

# W PROBIE (pilot ikony monety) — jeszcze nie zaakceptowane:
ZLOTO = [(255, 247, 196), (252, 214, 94), (228, 164, 40), (178, 108, 26), (116, 62, 22)]


def Y(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
