#!/usr/bin/env python3
"""Chowance/config.yml: usuwa starter.auto-okno, dopisuje sekcje zagroda (idempotentne). Uzycie: latka_configu.py <config.yml>"""
import sys
p = sys.argv[1]; s = open(p, encoding='utf-8').read()
if '\nzagroda:' in s: print('juz zalatane'); sys.exit(0)
stare = "starter:\n  auto-okno: true\n  opoznienie_ticki: 60\n"
if s.count(stare) != 1: print('BLAD: sekcja starter inna niz oczekiwana'); sys.exit(1)
s = s.replace(stare, """starter:
  opoznienie_ticki: 60                         # przypomnienie o zagrodzie po wejsciu (bez okna — 18.10)

# Zagroda mlodych (18.10): pierwszy chowaniec z rak Strazniczki Ogrodu — Pieczec Opiekuna + wybrane mlode.
zagroda:
  wlaczona: true
  swiat: stolica_odnowa
  trener: TRENER_OGRODU
  model_pieczeci: nexo:chw_pieczec_krysztalowa
  odrodzenie_ticki: 80
  wskazowka: "Strażniczka Ogrodu w Stolicy powierza młode chowańce nowym opiekunom — Ogród Chowańców, północny wschód Stolicy."
  # Trawnik 5 bl. na polnoc od Strazniczki (32.5/101/-48.5), widoczny od sciezki; yaw 0 = przodem na poludnie.
  miejsca:
    - {gatunek: KUDLACZ, x: 23.5, y: 101, z: -53.5, yaw: 0, napis: 1.2}
    - {gatunek: ISKRZYK, x: 26.0, y: 101, z: -53.5, yaw: 0, napis: 0.9}
    - {gatunek: MUSZLIK, x: 28.5, y: 101, z: -53.5, yaw: 0, napis: 1.1}
    - {gatunek: PEDZIK,  x: 31.0, y: 101, z: -53.5, yaw: 0, napis: 1.3}
    - {gatunek: UPIOREK, x: 33.5, y: 101, z: -53.5, yaw: 0, napis: 1.4}
""")
open(p, 'w', encoding='utf-8').write(s); print('ok')
