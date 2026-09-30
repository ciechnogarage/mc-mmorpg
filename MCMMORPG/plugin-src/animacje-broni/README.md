# AnimacjeBroni — feeling broni MMOItems (2026-09-30)

Vanilla 1.21.11 ma komponent przedmiotu `swing_animation` (`whack` = cios, `stab` = pchnięcie, `none`, plus długość
w tickach). Działa w 1. i 3. osobie, u wszystkich patrzących, bez modów. MMOItems 6.10 go nie ustawia, więc robi to
ten plugin, wg typu MMOItems przedmiotu. Drugi element to **model trzymania**: własny model w packu Nexo
(`bron:<skala>/<material>`), który dziedziczy vanillowy model materiału i zmienia tylko rozmiar w dłoni — rękojeść
zostaje w dłoni (przesunięcie liczone w `generuj.py`). Ikona w ekwipunku i przedmiot na ziemi są bez zmian.

Feeling broni opiera się na rodzaju obrażeń: **cięte** (ostrze tnie szeroko), **kłute** (grot przebija w linii),
**obuchowe** (tępa broń miażdży) i na wadze broni (długość zamachu, minimalne naładowanie ciosu, dźwięk).

| Broń (typ) | Obrażenia | Zamach (t) | Naładowanie | Zasięg | PPM | Trzymanie |
|---|---|---|---|---|---|---|
| Miecz (SWORD) | cięte | cios 6 | — | 3 | garda −40% | vanilla |
| Klinga elfów (KATANA) | cięte | cios 5 | — | 3 | garda −30%, lżejsza | vanilla |
| Topór (AXE) | cięte | cios 8 | 60% | 3 | — | vanilla |
| Miecz dwuręczny (GREATSWORD) | cięte, ciężka | cios 10 | 80% | 3,3 | garda −55%, wolna | ×1,30 |
| Broń drzewcowa (HALBERD) | **kłute** (było obuchowe) | pchnięcie 8 | 100% | **2–4,5**, przebija w linii | **szarża** jak włócznia | ×1,30 |
| Włócznia (SPEAR) | kłute | vanilla | vanilla | 2–4,5 | szarża (vanilla) | vanilla |
| Sztylet (DAGGER) | kłute | pchnięcie 4 | — | **2,6** | — | ×0,80 |
| Kosa (KOSA) | cięte, ciężka | cios 10 | 80% | **3,5** | — | ×1,40 |
| Sierp (SIERP) | cięte | cios 5 | — | 2,8 | — | vanilla |
| Buława (HAMMER) | obuchowe | cios 8 | 60% | 3 | — | vanilla |
| Młot dwuręczny (GREATHAMMER) | obuchowe, ciężka | cios 12 | **90%** | 3 | — | ×1,30 |
| Kostur dwuręczny (GREATSTAFF) | obuchowe | cios 9 | 60% | 3,3 | — | ×1,40 |
| Różdżka / kostur (WAND / STAFF) | magia (LPM pocisk MMOItems) | pchnięcie 5 / 7 | — | 3 | — | ×0,90 / ×1,25 |
| Grimuar, totem, filakterium | — | pchnięcie 5 | — | 3 | — | vanilla |
| Tarcza (SHIELD) | obuchowe | cios 7 | — | 3 | blok (vanilla) | vanilla |
| Łańcuch (WHIP) | cięte | cios 7 | — | LPM bicz MMOItems | — | vanilla |

Efekt trafienia (plugin, każdy cios): cięte = łuk cięcia + świst; kłute = iskry krytyka w linii ciosu + stuk grotu;
obuchowe = pył spod celu + głuche uderzenie (ciężka broń: uderzenie buzdyganu w ziemię). Do tego efekty MMOItems
(rozprysk obrażeń co 3 s: cięte stożek 60°, kłute 30°, obuchowe wokół celu).

**PPM (garda, szarża) tylko przy pustej lewej ręce i bez umiejętności MMOItems na PPM** — inaczej PPM należy do
zaklęcia/tarczy w lewej ręce albo do umiejętności; plugin zdejmuje i przywraca te komponenty sam (przegląd co 10 ticków).
Zmierzone na klonie 30.09 (`qa/mechanika.sh`): garda miecza 6 → 4,0 HP bez / 2,4 HP z gardą; halabarda nie trafia
celu na 1 bloku, trafia dwa w linii na 3 i 4 blokach; młot dwuręczny nie wyprowadza ciosu 0,6 s po poprzednim
(miecz tak); szarża halabardą w sprincie trafia; garda gaśnie z pochodnią w lewej ręce i wraca po jej zdjęciu.

Poza pluginem celowo: **SPEAR** (materiały `*_SPEAR` mają vanillowe pchnięcie i szarżę), **BOW / CROSSBOW /
GREATBOW / MUSKET** (animacje naciągania). Model trzymania dostaje tylko przedmiot z modelem domyślnym materiału —
model z Nexo/MMOItems (`model:`) zostaje. Materiały bez modelu „narzędzia” (END_ROD, książki, łańcuch) są pomijane.


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
    qa/mechanika.sh A|B|C|D             # pomiary: zasieg/przebicie, naladowanie, garda, garda vs lewa reka
    qa/efekty.sh SWORD PRZ_WOJ_MIECZ_1 miecz   # zrzuty trafienia i pozy PPM
    ./wdroz.sh                          # world1: kopia + cofnij.sh, tylko przy 0 graczy, RESTART

26.3: `swing_animation` zastąpią `attack_animation` + `interact_animation` — przy aktualizacji wersji zmienić plugin.
