# Rzemioslo — profesje v2 i zielarz

Plugin Paper 1.21.11 (`pl.mcmmorpg.rzemioslo`). Projekt profesji: `MCMMORPG/profesje/PROJEKT.md`,
zielarza: `MCMMORPG/profesje/ZIELARZ.md`. Zależności: MMOCore, MythicLib, MMOItems, Nexo, MythicMobs, packetevents
(`src/main/resources/plugin.yml`).

## Moduły
| Klasa | Co robi |
|---|---|
| `Rzemioslo` | start, rejestracja modułów; `config.yml` GENEROWANY przez `MCMMORPG/profesje/generuj.py` (nie edytuj ręcznie) |
| `Profesje`, `Dane`, `Przedmioty` | poziomy/XP/perki przez MMOCore, dane z configu, MMOItemy i narzędzia |
| `Zbieranie`, `Lowienie`, `Oprawianie`, `Stacje`, `Buffy` | węzły Nexo, łowiska, zwłoki, XP stacji, buffy i obozowisko |
| `Zlecenia` | zlecenia dnia per POSTAĆ (profil MMOProfiles), pliki `dane/` |
| `Zielarz` + `Z*` | zielarz: kępy STRINGBLOCK, scena zbioru (`ZScena`, `ZRoslina`, `ZFeeling`), odrost, zielnik, suszarnia, skaza |
| `ZMagazyn` | dysk zielarza (F1): jeden wątek zapisu/odczytu |
| `Komenda` | `/rzemioslo …` (perm. `rzemioslo.admin`, menu wołają z konsoli) |

## Dane zielarza na dysku (od F1, 25.10)
Katalog `plugins/Rzemioslo/`:
- `zielarz/regiony/<swiat>/r.<x>.<z>.yml` — kępy postawione w świecie, region 512×512 bloków (`x = blokX >> 9`).
  Wszystkie wczytywane przy starcie (kępy stawia admin, jest ich mało).
- `zielarz/konta/<uuid-konta>.yml` — odrost kęp per KONTO (`kepy.<klucz>: "ziarno;odrostDo;rozpoznana;chciwie;slaba"`).
  Wczytywany w tle przy wejściu gracza, z pamięci znika po wyjściu. Brak trwałych wpisów = plik usunięty.
- `zielarz/swiat.yml` — partie (stan wspólny dla świata).
- `zielnik/<uuid-postaci>.yml` — zielnik i wiedza per POSTAĆ (profil MMOProfiles).
- `zielarz-suszarnie.yml` — suszarnie (zapis tylko przy zmianie).

Zasady zapisu:
- **Nic nie pisze na wątku serwera.** Wątek serwera składa tekst (migawka), `ZMagazyn` zapisuje go na wątku
  `Rzemioslo-zielarz-dysk`. Jeden wątek = zapisy i odczyty w kolejności zleceń (szybki relog widzi ostatni zapis).
- Zapis: `<plik>.tmp` → fsync → `ATOMIC_MOVE` na miejsce. Crash w trakcie zostawia stary plik, nie ucięty.
- Zbiór tylko oznacza konto/region jako „brudne”; zrzut zmienionych co 60 s (`runTaskTimer` 1200 t), przy wyjściu
  gracza (jego konto i zielnik) i przy wyłączaniu (`ZMagazyn.zamknij` czeka do 20 s na kolejkę).
  Po crashu (kill -9) można stracić najwyżej ostatnie 60 s odrostu.
- Zepsuty YAML (albo inny błąd odczytu) zostaje obok jako `<plik>.zepsuty` + SEVERE w logu (nie nadpisujemy go pustym);
  gracz zaczyna wtedy z pustym odrostem, zamiast wisieć na „Chwila…”. Pojedynczy zły wpis konta (np. ucięta liczba)
  jest pomijany z WARN `pomijam zly wpis kepy`, reszta konta się wczytuje (przy następnym zapisie wpis znika).
- Indeks chunków per konto (`"swiat;cx;cz" -> klucze kęp`) — wysłanie chunka graczowi sprawdza tylko kępy z tego chunka.
- Gracz klika kępę, zanim jego konto się wczyta (pierwsze ~ms po wejściu): „Chwila, rozglądasz się jeszcze.”

Przeniesienie ze starego układu (jednorazowo, przy starcie): `zielarz-stany.yml` → `zielarz/konta/` + `zielarz/swiat.yml`,
`zielarz-kepy.yml` → `zielarz/regiony/`; stare pliki zostają jako `*.przed-f1`, w logu linie `zielarz F1: …`.
Istniejących plików kont nie nadpisuje.

**Cofnięcie F1**: stary jar (sprzed gałęzi `zielarz-f1`) + `mv zielarz-stany.yml.przed-f1 zielarz-stany.yml`,
`mv zielarz-kepy.yml.przed-f1 zielarz-kepy.yml` (odrost zebrany po wdrożeniu F1 przepada, kępy postawione po F1 trzeba
postawić ponownie).

## Komendy (wybrane)
- `rzemioslo reload | info <gracz>` — config, stan profesji.
- `rzemioslo kepa-xyz <gatunek> <świat> <x> <y> <z>` / `kepa` / `suszarnia-xyz …` — stawianie kęp i suszarni.
- `rzemioslo odrost <gracz>` — odrost od razu (test).
- `rzemioslo magazyn [zapisz]` — konta w pamięci, brudne konta/regiony, licznik zapisów i błędów dysku; `zapisz` = zrzut teraz.
- `rzemioslo zielnik <gracz>`, `pomiar`, `kom-test`, `rady-reset` — diagnostyka zielarza.

## Build
`./build.sh` → `build/libs/rzemioslo-<wersja>-<data>.jar`. Na serwer kopiuj tylko najnowszy jar (dwa = „Ambiguous plugin name”).
Klon testowy zielarza: `MCMMORPG/profesje/zielarz/przygotuj_klon.sh <klon>`.
