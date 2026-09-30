# AnimacjeBroni — zamach i trzymanie broni MMOItems (2026-09-30)

Vanilla 1.21.11 ma komponent przedmiotu `swing_animation` (`whack` = cios, `stab` = pchnięcie, `none`, plus długość
w tickach). Działa w 1. i 3. osobie, u wszystkich patrzących, bez modów. MMOItems 6.10 go nie ustawia, więc robi to
ten plugin, wg typu MMOItems przedmiotu. Drugi element to **model trzymania**: własny model w packu Nexo
(`bron:<skala>/<material>`), który dziedziczy vanillowy model materiału i zmienia tylko rozmiar w dłoni — rękojeść
zostaje w dłoni (przesunięcie liczone w `generuj.py`). Ikona w ekwipunku i przedmiot na ziemi są bez zmian.

| Typ MMOItems | Nasze bronie | Zamach | Czas (t) | Trzymanie |
|---|---|---|---|---|
| SWORD | miecze | cios | 6 | vanilla |
| KATANA | klingi elfów | cios | 5 | vanilla |
| AXE | topory | cios | 8 | vanilla |
| GREATSWORD | miecze dwuręczne | cios | 10 | ×1,30 |
| HALBERD | broń drzewcowa | cios | 10 | ×1,30 |
| GREATHAMMER | młoty dwuręczne | cios | 12 | ×1,30 |
| KOSA | kosy | cios | 10 | ×1,40 |
| DAGGER | sztylety | pchnięcie | 4 | ×0,80 |
| WAND | różdżki | pchnięcie | 5 | ×0,90 |
| STAFF | kostury | pchnięcie | 7 | ×1,25 |
| GREATSTAFF | kostury dwuręczne | pchnięcie | 9 | ×1,40 |
| TOME / TALISMAN / FILAKTERIUM | grimuary, totemy, filakteria | pchnięcie | 5 | vanilla |
| HAMMER | buławy | cios | 8 | vanilla |
| SIERP | sierpy | cios | 5 | vanilla |
| SHIELD | tarcze | cios | 7 | vanilla |
| WHIP | łańcuchy | cios | 7 | vanilla |
| LONG_SWORD, GREATAXE, LANCE, THRUSTING_SWORD, GAUNTLET | (stare demo) | wg tabeli w `generuj.py` | | |

Poza pluginem celowo: **SPEAR** (materiały `*_SPEAR` mają vanillowe pchnięcie i szarżę), **BOW / CROSSBOW /
GREATBOW / MUSKET** (animacje naciągania). Model trzymania dostaje tylko przedmiot z modelem domyślnym materiału —
model z Nexo/MMOItems (`model:`) zostaje. Materiały bez modelu „narzędzia” (END_ROD, książki, łańcuch) są pomijane.

**Poza trzymania przy PPM (`consumable.animation`) celowo NIE jest używana:** broń z nią przejmuje PPM, a w trybie
zaklęć „broń w prawej + zaklęcie w lewej” PPM ma rzucać zaklęcie z lewej ręki.

## Jak działa
- `generuj.py` — źródło prawdy (tabela `TYPY`): pisze `wynik/config.yml` i `wynik/pack/assets/bron/` (materiały
  zbiera z `MMOItems/item/<typ>.yml` serwera, transformacje rąk z client.jar).
- Plugin poprawia przedmiot, gdy trafia do ręki (wejście, zmiana slotu, zamiana rąk, klik/przeciąganie, podniesienie)
  i przeglądem obu rąk co 2 s. `ItemBuildEvent` MMOItems 6.10 jest martwy (nikt go nie wywołuje).
- `/animacjebroni sprawdz <gracz>` — typ, zamach i model z głównej ręki; `/animacjebroni reload`.

## Zmiana / test / wdrożenie
    python3 generuj.py && cp wynik/config.yml src/main/resources/ && ./build.sh
    qa/klon.sh <katalog> --fresh        # klon world1 ab-test (+ klient: docker run ... mc-client-test qa/launch.sh)
    qa/scena.sh GREATSWORD HARDENED_STEEL_CLAYMORE   # dane komponentów + zrzuty 1. osoba / zamach / przód
    ./wdroz.sh                          # world1: kopia + cofnij.sh, tylko przy 0 graczy, RESTART

26.3: `swing_animation` zastąpią `attack_animation` + `interact_animation` — przy aktualizacji wersji zmienić plugin.
