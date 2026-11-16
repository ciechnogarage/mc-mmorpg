# Licho — minigra „Licho nie śpi” (prop hunt)

**WDROŻONE na world1 25.10 01:24 UTC** z kodu f3b986f (w `main` od 030821e), jar `licho-0.1.0-202610250042.jar`
md5 `25c0c63ed05f7a98fe0b5bfa9b92e342`. Cofnięcie: `/opt/docker/mc/data/world1/backups/licho-20261025-012313/cofnij.sh`.

Projekt gry (role, punkty, tytuły, koszt serwera): `PROJEKT.md`. Ten plik opisuje plugin.

## Wejście gracza
Q → Drużyna (slot 38) → kafel LICHO (25) → menu `licho` (DOŁĄCZ slot 5, Rekordy 25/26/34/35, SZUKAJ 41/42/50/51,
CHOWAJ 43/44/52/53). Gracz nie wpisuje komend. Wcielać się w sprzęt może **tylko Licho**; PPM Zamawiacza w zaczepę
pokazuje na pasku „wcielać się może tylko Licho” (wcześniej cisza — gracz nie wiedział, czemu nie działa, 16.11).
**Rola**: losuje ją gra na starcie rundy (1 Zamawiacz na `na-zamawiacza` graczy). Q w Zagrodzie (pauza `licho_pauza`)
ma LICHEM (23), ŁOWIĘ (25), LOSUJ (41) = `licho chce <gracz> licho|zamawiacz|obojetnie`: `rozproszenie()` po tasowaniu
sortuje stabilnie (ŁOWIĘ, obojętni, LICHEM), więc chętni dostają rolę pierwsi, a wśród równych zostaje los. Wybór
trzyma `LichoPlugin.chce` w pamięci do restartu serwera; gracz, któremu nie wyszło, dostaje wyjaśnienie na czacie.
**SZUKAJ = ćwiczenie solo** (`licho cwiczenie <gracz>`): gracz jest Zamawiaczem, Lichami są boty serwera
(`Uczestnik` z `bot=true`, bez gracza: BlockDisplay + zaczepa, fałszywy blok `sendBlockChange`). Bot po trafieniu
ucieka lotem na inny sprzęt, co 40–70 s sam się przeprowadza, gdy nikt nie stoi blisko, psoci jak Licho.
Miejsca botów tylko z `osiagalneOdBramy()` (BFS chodzenia od bramy przy starcie ćwiczenia: krok ±1 w górę z 2 bloki
wolnej głowy, do 3 w dół, podłoga o kolizji 0,4–1,0, więc nie płoty/murki) — bez tego bot lądował w zagrodzie z dyniami
(14,64,8), do której gracz nie dochodzi bliżej niż 3,6 bloku (test 15.11).
Ćwiczenie nie zapisuje rekordów, tytułów ani pliku rundy (`Runda.cwiczenie`). Gdy w Zagrodzie trwa runda z graczami,
TRENING odmawia; gracz sam w poczekalni dostaje po 3 s podpowiedź na czacie (Q w Zagrodzie otwiera pauzę `licho_pauza`,
nie hub): „Q → Opuść zagrodę, potem SZUKAJ albo CHOWAJ”. Drugi gracz może dołączyć (DOŁĄCZ/SZUKAJ) jako Zamawiacz.
**CHOWAJ = trening Licha** (`licho chowaj <gracz>`, tylko pusta Zagroda albo gracz sam w poczekalni): gracz jest Lichem,
`chowaj.chowanie` (25 s) na schowanie, potem bramą wychodzą `chowaj.szukajacy` (2) boty-Zamawiacze. Bot = `Uczestnik`
z `postac` = `Mannequin` (Paper 1.21.9+, bez AI moba: nie znika w peaceful i nie ma własnego chodzenia — Witch/Villager
odpadły). Chodzi teleportem po `osiagalne` (BFS `idzDo`, krok `chowaj.krok`/tick, stopy na wysokości kolizji podłogi).
Decyzja co 5 ticków: widzi ruszające się Licho do `chowaj.wzrok` bl. (rayTraceBlocks od oczu) → pościg i cios z szansą
`chowaj.pogon-trafia`; słyszy szept/psotę/kichnięcie (`halas`, z błędem 1–3 bl.) → idzie, okadza, sprawdza ciosem
sprzęty do 3,5 bl. (zastygłe Licho też); inaczej obchód losowych sprzętów. Zastygłe Licho tuż obok (≤2,2 bl.) bot
czasem „wyczuwa” (15%/s). Psota liczy też boty-Zamawiaczy jako słuchaczy (`najblizszyZamawiacz`). Złapane = koniec
(„Złapane!”), przetrwanie = „Świt!”. Bez rekordów (`cwiczenie = true`).
**Licho nie widzi na sobie zbroi ani przedmiotów w ręce**: INVISIBILITY (vanilla) nie chowa zbroi ani trzymanych rzeczy
w WŁASNYM kliencie (F5, pierwsza osoba), a `hidePlayer` działa tylko na innych. Do 16.11 Licho biegało w diamentowej
zbroi z mieczem „wewnątrz” bloku (zgłoszenie właściciela). Teraz `Klawisze.bezRzeczy` (PacketEvents, tylko gracze-Licha):
w wychodzących WINDOW_ITEMS/SET_SLOT okna 0 sloty 5–8 i 36–45, w SET_SLOT okna −2 i SET_PLAYER_INVENTORY sloty 0–8
i 36–40 idą jako puste. Serwer nic nie zdejmuje; klik w ekwipunek w roli jest anulowany (`klikEkwipunku`), a po roli
`updateInventory()` oddaje widok (`bezRzeczy(p,false)` w `odslon` i `sprzatnij`). Sprawdzone 16.11 klientem QA A/B
(ta sama zbroja, `execute if items … armor.*` = 1111): stary jar — zbroja widoczna w F5 tył/przód/bieg i miecz w 1. os.;
nowy — sam blok, pusty pasek, po wyjściu miecz wraca. Boty po zmianie: chowaj 25/25, rola 23/23, cwiczenie 19/19.
**WDROŻONE** na world1 15.11 21:25 UTC z gałęzi `licho-cwiczenie` (3a15908 + 8c2de22), jar `licho-0.1.0-202611151842.jar` (md5 b6519e55);
cofnięcie `/opt/docker/mc/data/world1/backups/licho-20261115-212426/cofnij.sh`.
**CHOWAJ + wybór roli WDROŻONE** na world1 16.11 19:40 UTC z gałęzi `licho-rola` (78d5f34), jar `licho-0.1.0-202611161723.jar`
(md5 b13345fe); cofnięcie `/opt/docker/mc/data/world1/backups/licho-20261116-193936/cofnij.sh`, kopia świata
`/opt/docker/mc/kopie/world1-20261116-1939.tar.zst`.
**Ukrywanie zbroi/przedmiotów Licha WDROŻONE** na world1 16.11 20:52 UTC (d0f0798), jar `licho-0.1.0-202611161950.jar` (md5 6d121b8f);
cofnięcie `/opt/docker/mc/data/world1/backups/licho-20261116-205236/cofnij.sh`, kopia `/opt/docker/mc/kopie/world1-20261116-2052.tar.zst`.
Menu i kafel: `wdrozenie/menu/` (DeluxeMenus), wpinane przez `wdrozenie/latka.py`.

## Konfiguracja (`plugins/Licho/config.yml`, komentarze w pliku)
- `swiat` (licho, VoidGen), `swiaty-wejscia`, `swiaty-awaryjne` — skąd wolno dołączyć i dokąd odesłać.
- `gracze.min/max/na-zamawiacza`, `czasy.*` (poczekalnia, rozproszenie 30, łowy 180, podsumowanie 8 s).
- `licho.*`, `zamawiacz.*`, `punkty.*` — zasady rundy; `przebrania` — sprzęty, w które wciela się Licho.
- `cwiczenie.*` — liczba botów (3), krótsze łowy (150 s), lot ucieczki, odległości, przeprowadzki, psoty, imiona.
- `chowaj.*` — trening CHOWAJ: chowanie 25 s, łowy 150 s, boty-Zamawiacze 2, wzrok 10 bl., krok 0,2 bl./tick,
  sprawdzanie co 1,5 s, upór 20 s, trafienie w pościgu 60%.
- `tytuly.min-graczy` (4) i progi `tytuly.licho` / `tytuly.zamawiacz` (liczba → nazwa). Tytuł wynika z progów,
  więc zmiana progów działa wstecz. Liczą się tylko rundy dokończone przy co najmniej `min-graczy` graczach.
- Po zmianie: `licho przeladuj` (na klonie; na world1 restart lub to samo z konsoli przy 0 graczach).

## Dane
`plugins/Licho/rekordy.yml`: `gracze.<uuid>` → `nick`, `najlepszy`, `data`, `rundy`, `przetrwane`,
`tytul-przetrwane`, `tytul-zlapani` (te dwa liczą tylko rundy do tytułów).

## Komendy (konsola, `licho.console` = OP)
`licho dolacz|wyjdz <gracz>` (menu woła je przez konsolę), `rekordy [gracz]`, `zbuduj` (Zagroda z `mapa.txt`),
`cwiczenie <gracz>` (SZUKAJ), `chowaj <gracz>` (CHOWAJ), `chce <gracz> licho|zamawiacz|obojetnie` (pauza), `stan` (faza, gracze, forceload, encje, progi tytułów), `start [s]`, `obserwuj <gracz>`, `przeladuj`.

## PlaceholderAPI (softdepend)
`%licho_tytul%`, `%licho_postep_licho%`, `%licho_postep_zamawiacz%` („Domowik (2/5)”), `%licho_min_graczy%`, `%licho_chce%` (wybrana rola: „losuje gra” / Licho / Zamawiacz).

## Budowa, testy, wdrożenie
- `bash build.sh` → `build/libs/` (ma być jeden jar).
- Klon: `wdrozenie/klon-przygotuj.sh <klon>` (pełne), `wdrozenie/klon-jar.sh <klon>` (podmiana jara + restart).
- Boty: `narzedzia/bot/run.sh <klon> $PWD/testy/<scenariusz>.js` — `runda.js`, `nieidealne.js` (+ `nieidealne-c.js`), `tytuly.js`,
  `chowaj.js` (CHOWAJ: manekiny wychodzą, chodzą, widzą ruch, trafiają, sprzątanie; 25/25 16.11) i `rola.js`
  (ŁOWIĘ/LICHEM w obie strony, LOSUJ, lore `%licho_chce%`, pasek PPM Zamawiacza; 23/23 16.11). Klient QA 16.11: menu,
  pauza, CHOWAJ z botem w kadrze, wyjście OPUŚĆ; spark w łowach CHOWAJ tick śr. 1,1–1,3 ms.
- Menu: `python3 narzedzia/ksiega/rysuj_menu.py <katalog>` generuje `licho.yml` i `licho_*.png`, ale **`hub_spolecznosc_*.png`
  z wyjścia NIE kopiuj** — generator wycina Gildię z obrazów prod, które już ją mają przesuniętą, więc wychodzi inny plik
  niż `wdrozenie/menu/baza-hub-md5.txt` (15.11). Do `wdrozenie/menu/` tylko pliki `licho*`; porównuj `tobytes()`, nie `getbbox()` (RGBA).
- Test ćwiczenia: `testy/cwiczenie.js` (pogoń za botami, 19/19, 3/3 złapane) i `testy/cwiczenie-nieidealne.js`
  (odmowa przy rundzie z graczami, podpowiedź, Opuść → TRENING, dołączenie drugiego gracza, wyjścia; 14/14). Gonitwa w teście:
  `idz(... {r: 1})` — `idz` kończy przy odległości ≤ r+1,5, a cios dopiero z ≤3 bloków.
- World1: `wdrozenie/wdroz.sh --sucho`, potem `wdrozenie/wdroz.sh` (tylko przy 0 graczach; kopia świata, restart, budowa Zagrody).
