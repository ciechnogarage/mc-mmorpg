# Recenzja Y — druga runda (2026-10-03)

Zakres: cztery wskazane karty; wiążą 05 R1–R5, 07 L1–L7 i 06-P. Oceny w kolejności bramki z 00-BRIEF §4: fun / mechanika / czytelność / topologia / WOW / sens / powtarzalność / nagroda / wykonalność / różnorodność. „Test zaplanowany” nie oznacza wyniku testu; brief 00:12 zabrania dotykania serwera.

## 10-loch-001 — Kwietna Polana

| Poprawka obowiązkowa z 06-Y | Status i dowód |
|---|---|
| Y1: jeden pąk, wspólny czas i czytelny skutek | **Zrealizowana**: 001:17–27,101–105; jeden pąk Siewcy daje 3 Pniaczki dopiero po dojrzewaniu. Odrzucenie żądania identycznej skali ma wiążący dowód w 06-P:44–47 (×1,5 dozwolone). |
| Y2: limit 12, najstarszy, instancja, test | **Nie w pełni**: 001:212,216–217 podaje licznik świata i scenariusz testu, lecz algorytm `#pak − 12` nie wybiera najstarszego *aktywnego* pąka; wynik testu nie istnieje. |
| Y3: solo, anty-kitowanie, F3 | **Projektowo zrealizowana**: 001:124–125,133,138–150 daje salwę co 6 s, konar przy bossie i wygraną bez pętania; pomiar blockoutu nadal zaplanowany. |
| Y4: równy łup/EXP przy ocenie i wyborze | **Zrealizowana**: 001:118–119,184–190; ocena i los młodych zmieniają kosmetyk, skrzynia i materiały pozostają równe. |

Nowe oceny: **7 / 8 / 7 / 8 / 8 / 8 / 7 / 7 / 6 / 7**.

**WERDYKT: BLOKUJĘ.** 001:212: gdy pąk nr 1 żyje, nr 2 zdeptano, nr 3–13 żyją i powstaje nr 14, algorytm szuka usuniętego nr 2, zostawiając 13 aktywnych wbrew R1/05:18; naprawa: po każdym summon wybierz najniższy numer spośród *aktywnych* pąków tej instancji i ususz go dopiero przy przekroczeniu 12. Plan B w 001:212 także nie opisuje działającego wyparcia najstarszego, choć je deklaruje; naprawa: zachowaj kolejkę i widoczne usychanie również w planie B.

## 10-loch-004 — Łany Strachów

| Poprawka obowiązkowa z 06-Y | Status i dowód |
|---|---|
| Y1: wskazówka Snopowca w każdej pogodzie | **Zrealizowana**: 004:24–26,94,185; oddech co 3 s jest pokazany przed zasadzką i niezależny od Wiatru. |
| Y2: pełnoprawna głośna droga i progi | **Zrealizowana**: 004:60–62,187–190; własny, osiągalny warunek S (≤13 min przy trasie 12–14 min), progi S–C. |
| Y3: alarm z limitem i próba solo | **Projektowo zrealizowana**: 004:97–100 ogranicza alarm i wrony; próba dwóch klas jest zapisana w 004:229–230, bez wyniku. |
| Y4: wykrycie/koszenie z planem B | **Nie w pełni**: 004:211–230 zastępuje `blocktype` i podaje koszt, ale komenda koszenia 004:228 tworzy solidne bloki zamiast przechodniego ścierniska; próby nie wykonano. |
| Y5: równy łup po wyborze | **Zrealizowana**: 004:175–179; wybór zmienia walkę/czas i kosmetyk, nie materiały bazowe. |

Nowe oceny: **7 / 8 / 7 / 7 / 8 / 8 / 7 / 7 / 6 / 8**.

**WERDYKT: BLOKUJĘ.** 004:228 zamienia `tripwire` w dwublokowym pasie bezpośrednio na `coarse_dirt`, więc koszenie stawia pełne bloki w trasie/arenie zamiast odsłaniać ściernisko; naprawa: usuń warstwy łanu do `air`, a `coarse_dirt` stawiaj tylko w podłożu i sprawdź przejście po skoszeniu.

## 10-loch-007 — Grządki Maluczkich

| Poprawka obowiązkowa z 06-Y | Status i dowód |
|---|---|
| Y1: prawdziwa opowieść A/B | **Zrealizowana**: 007:7–12,45–51; osobne kwestie Ziutki, notatki i opisy wejścia. |
| Y2: dwa tunele z konsekwencją i trzeci sekret | **Zrealizowana**: 007:85–94,103–105; pętla z różnym kosztem/łupem, kamień bez wyjątku R1. |
| Y3: pełny R1 | **Projektowo zrealizowana**: 007:25–28,225–226; wspólny protokół, test 13 zgonów zapisany bez wyniku. |
| Y4: S4 bez przymusowego słabego stanu | **Zrealizowana**: 007:64,102,109; mrówki na zewnątrz, dwie krople i odwrót, Kosarz pod liśćmi. |
| Y5: próg A/B, równe materiały | **Nie w pełni**: 007:183–184 wyrównuje łup; 007:211–219 opisuje osiem prób A i walidację B, lecz 06-P:56–58 odracza A, a działanie osłon B nie jest dowiedzione. |

Nowe oceny: **7 / 8 / 7 / 7 / 9 / 7 / 7 / 7 / 6 / 8**.

**WERDYKT: BLOKUJĘ.** W obecnie dostępnej ścieżce B osłony S4 są na y+8 (007:74), później na y+4 „do wskoczenia” (007:83), a plan B daje tylko `jump_boost II` (007:216–219), więc lekki gracz może nie dosięgnąć wymaganej kryjówki; naprawa: ustal jedną wysokość i dodaj w S4 oraz na arenie jawne kolumny puchu lub stopnie, które rzeczywiście pozwalają wejść na osłony.

## 10-loch-010 — Matecznik v2

| Poprawka obowiązkowa z 06-Y | Status i dowód |
|---|---|
| Y1: pąk tylko po poległym, osobny znak odrastania | **Zrealizowana**: 010:15–21,108; Zasiew przyzywa Pniaczki, pąki powstają po ich śmierci. |
| Y2: odejść od kopii 005 | **Zrealizowana**: 010:26–29,91–100,184–185; konary zmieniają teren i ataki, nie odsłaniają statycznego węzła. |
| Y3: walka jako kulminacja | **Zrealizowana**: 010:98–100; Darnik może otrzymywać obrażenia od F1, finał przy 0 HP. |
| Y4: kontra solo i pet | **Projektowo zrealizowana**: 010:91–93,114–120; duży hitbox jest dostępny z ziemi, ruch zrywa Szczepienie, przywołańce mają cel; próba na klonie nadal bez wyniku. |
| Y5: kolejka, pąk gracza, opieka nad plonami | **Zrealizowana w specyfikacji**: 010:19–21,144–146,176–177; plan B śmierci i Hanna w obu finałach. |

Nowe oceny: **8 / 8 / 8 / 8 / 9 / 8 / 7 / 6 / 7 / 8**.

**WERDYKT: BLOKUJĘ.** 010:152–153 daje od 1 do 3 skrzyń równej wartości zależnie od oceny obejmującej sekrety, więc wiedza zwiększa moc łupu wbrew 05:55–56; naprawa: daj identyczny sprzęt, materiały i EXP przy S–C, a dodatkowe skrzynie ogranicz do kosmetyków. Ponadto 010:94,155–157 deklaruje HP bossa 1440, a 010:77 HP elity 220, choć wiążące 07:15–16 wymaga 1800 i 450; naprawa: przyjmij liczby L2 i przelicz czas starć.

## Werdykt zbiorczy

**BLOKUJĘ 001, 004, 007 i 010** wyłącznie za wskazane wyżej błędy limitu, kolizji, dostępności osłon oraz progresu/liczb finału. Grep starych treści nie wykazał w czynnych regułach dawnego `sort=furthest`, globalnego licznika, skróconego czasu pąka, wskazówki Snopowca zależnej od Wiatru, zatyczki tunelu ani pąków z żywych nici; dawne frazy pozostały tylko w odpowiedziach §15 lub opisie usuniętej v1.
