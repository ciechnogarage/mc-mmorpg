#!/usr/bin/env python3
"""Przycisk 'starter' w chowance_menazeria.yml: zamiast okna wyboru — droga do zagrody młodych (18.10).
Uzycie: latka_menazerii.py <plik.yml>  (zmienia tylko display_name i lore przycisku 'starter'; idempotentne)."""
import sys
p = sys.argv[1]
s = open(p, encoding='utf-8').read()
stare_n = "    display_name: '&a&lWybierz startera!'\n"
stare_l = "      - '&7Nie masz jeszcze chowańca.'\n      - '&7Wybierz jednego z trzech.'\n      - ''\n      - '&eKliknij.'\n"
nowe_n = "    display_name: '&a&lOdbierz pierwszego chowańca'\n"
nowe_l = ("      - '&7Nie masz jeszcze chowańca.'\n      - '&7Strażniczka Ogrodu w Stolicy'\n"
          "      - '&7powierza młode nowym opiekunom.'\n      - ''\n      - '&eKliknij &7— wskażę drogę.'\n")
if nowe_n in s:
    print("juz zalatane"); sys.exit(0)
# Pierwszy pusty slot druzyny dla gracza bez chowanca: widoczna pieczec, klik = droga do zagrody
# (przycisk 'starter' w slocie 24 jest niewidoczna nakladka na kafelku tla — klient QA 18.10).
kotwica = "  'druzyna_1_pusty':\n"
starter_slot = """  'druzyna_1_starter':
    material: 'nexo-chw_pieczec_krysztalowa'
    slot: 36
    priority: 0
    update: true
    display_name: '&a&lOdbierz pierwszego chowańca'
    lore:
      - '&7Nie masz jeszcze chowańca.'
      - '&7Strażniczka Ogrodu w Stolicy'
      - '&7powierza młode nowym opiekunom.'
      - ''
      - '&eKliknij &7— wskażę drogę.'
    view_requirement:
      requirements:
        brak:
          type: string equals
          input: '%chowance_starter_wybrany%'
          output: 'nie'
    left_click_commands:
      - '[player] chw starter'
      - '[close]'
"""
if s.count(kotwica) != 1:
    print("BLAD: brak druzyna_1_pusty"); sys.exit(1)
s = s.replace(kotwica, starter_slot + kotwica)
if s.count(stare_n) != 1 or s.count(stare_l) != 1:
    print("BLAD: nie znaleziono dokladnie jednego przycisku starter"); sys.exit(1)
s = s.replace(stare_n, nowe_n).replace(stare_l, nowe_l)
open(p, 'w', encoding='utf-8').write(s)
print("ok")
