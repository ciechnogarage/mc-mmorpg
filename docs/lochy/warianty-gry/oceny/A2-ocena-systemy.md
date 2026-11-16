# Ocena kierunku „Cztery drogi śmierci”

Odwołania: **P** = `PROPOZYCJA-cztery-drogi.md`, **K** = `KANON.md`, **B** = `panteon.md`; liczby oznaczają wiersze plików.

## 1. Werdykt

**Przerobić zasadniczo przed decyzją o produkcji.** Rdzeń fabularny da się obronić: powrót gracza wynika z Korzenia, a finał używa domen Ordhuna, Nysary i Vharuna (P 6–18, 29–43; B 10–12). Plan gry jeszcze tego nie dowozi. „Równość” lochów i wyspy jest dziś tabelą tych samych informacji oraz docelowymi medianami, bez dwóch równie angażujących pętli (P 57–79; K 8–11). HC ma nierozstrzygnięte zgony środowiskowe i wylogowanie w walce (P 126–140). Wycinek trzech lochów nie daje podstaw do finansowania reszty skali: kanon wymaga około stu lochów i dziesięciu wysp, a propozycja odkłada 90 lochów i dziewięć wysp (K 7–10, 46; P 142–148). Finansowałbym wyłącznie przebudowany pionowy wycinek Aktu 2 z twardą bramką jakości i kosztu.

## 2. Mocne strony

1. Wyjaśnienie respawnu spina istniejące pąki, Szczep i przycięcie Matecznika; nie wymaga cofnięcia finału Aktu 1 (P 6–11, 41–43; K 31–37).
2. Pakt nieumarłych zachowuje ich grywalność, a prawną rolę Nysary i konflikt z Vharunem wiąże z panteonem (P 17, 46–50; K 16–17, 24–26; B 11–12, 22–27).
3. Śmierć postaci nie jest warunkiem poznania historii; to konieczne przy deklarowanym HC (P 25–27, 57–70, 126–128).
4. Autor uwzględnia solo i profesje, proponuje mierniki czasu, EXP i nagród oraz nie obiecuje całej kampanii przed pomiarem (P 72–78, 142–148; K 22, 46).
5. Ograniczenie praw świata do efektów osobistych i małych stref trafia w techniczne ramy Paper i osobnego serwera wyspy (P 111–124; K 43–45).

## 3. Problemy krytyczne

### A. Równość ścieżek istnieje w arkuszu, nie w grze

- **Problem:** wspólne trzy fakty na akt zastępują projekt odrębnych aktywności, a punktowa wyspa nagradza dostawy, dowody i finał bez opisu ich trudności, zmienności ani kosztu czasu (P 57–79).
- **Dowód:** kanon wymaga odmiennych historii, wrogów i bossów, przy samodzielnym przejściu aktu; propozycja powtarza te same odkrycia i decyzje, w wielu aktach sprowadzając wyspę do „rady”, „oględzin”, „zgromadzenia” (K 8–11; P 63–70, 95–103). Medianę ±10% zapisano jako cel, nie jako wynik testu (P 77–78, 146–147).
- **Konsekwencja:** gracz wyspy dostanie serię znaczników i transportów, gracz lochów — zaprojektowane starcia; oba paski postępu mogą rosnąć równie szybko, choć jedna droga będzie wyraźnie uboższa (P 74–79; K 8–11).
- **Naprawa:** dla pełnego Aktu 2 rozpisać po stronie wyspy trzy konkretne, powtarzalne pętle z decyzjami i przeciwnościami, ich własną historią i bossem; mierzyć ukończenie, powroty, porzucenia i preferencje po 5–7 godzinach, osobno dla solo i grup (P 62, 74–78, 142–147).

### B. Zakres produkcji jest niedoszacowany przez wycinek

- **Problem:** jedna wersja Aktu 2 ma trzy lochy, osadę, wydarzenie, bossa, prawo, cztery reakcje rasowe i HC; docelowy akt ma około dziesięciu lochów, a kampania dziesięć aktów (P 142–148; K 7–10, 46).
- **Dowód:** plan wprost odkłada 90 dalszych lochów, dziewięć wysp, osiem kolejnych praw i sezony; jednocześnie każdy akt dostaje dwa nośniki trzech faktów, osobną wyspę i finał (P 57–70, 111–124, 142–148).
- **Konsekwencja:** działający prototyp może dać fałszywy sygnał „zielone światło”; później koszt budowy, skryptów, QA, balansowania i utrzymania dwóch serwerów urośnie szybciej niż ilość gry, którą jeden akt zapewnia (K 8–10, 46; P 74–79, 142–148).
- **Naprawa:** po wycinku policzyć roboczogodziny na **pełny**, dziesięciolochowy akt oraz obsługę wyspy i HC; zatwierdzać kolejne akty dopiero po pomiarze przepustowości produkcji, a prawa świata dodawać tylko tam, gdzie poprawiają pętlę (K 8–13, 46; P 111–124, 145–148).

### C. HC ma luki w definicji śmierci i bodźcach do ryzyka

- **Problem:** tabela rozstrzyga walkę PvE, PvP, trening i rozłączenie, ale milczy o upadku, lawie, utonięciu, obrażeniach od prawa świata i śmierci podczas transportu; osobno pozwala postaci zniknąć 30 sekund po rozłączeniu w walce (P 111–124, 126–140).
- **Dowód:** utrata statusu jest opisana jako `HP=0 w walce PvE`, cień Aktu 10 lub potwierdzone PvP, podczas gdy wyspa zawiera transport, wodę, wiatr i zalewanie pól (P 74–76, 116, 119, 121, 130–137). Droga profesji może przejść także finał bez bossa (P 76).
- **Konsekwencja:** gracze będą sprawdzać, które zgony nie odbierają statusu, wylogowywać się z niekorzystnych starć i wybierać najmniej ryzykowną drogę do tablicy. Uczciwy gracz może stracić status przez awarię, której reguły nie umieją jasno ocenić (P 129–140).
- **Naprawa:** określić jedną matrycę wszystkich źródeł `HP=0` i stanów transferu; zapisać wynik autorytatywnie przed teleportem/respawnem; wylogowanie w walce testować jako część starcia, nie arbitralne 30 sekund. Porównywać rekordy HC osobno według drogi i rzeczywistej ekspozycji na ryzyko (P 74–76, 129–140).

### D. Reguła zrostu tworzy nieobsłużony system podróży i demografii

- **Problem:** każdy gracz wraca do najbliższej kapliczki bez limitu, a rodziny znają sposób uzyskania zrostu, nosząc umierających pod odrosty; limit około 300 NPC na 30 lat nie wynika z podanej reguły (P 7–16, 23–24).
- **Dowód:** koszt powrotu to tylko istniejące −10% EXP i trzy minuty osłabienia; brak reguły o ładunku, wejściu do instancji, odległości od kapliczki lub poziomie 100 (P 10, 15, 74–78, 81–93).
- **Konsekwencja:** część graczy użyje śmierci jako powrotu z wyspy, obejścia transportu albo resetu nieudanego podejścia, jeśli koszt okaże się niższy niż droga. W fabule kapliczki powinny stać się masową infrastrukturą hospicjów i celem władzy, czego pojedynczy Rejestr i oszuści nie wyczerpują (P 23–24, 64, 99).
- **Naprawa:** ustalić promień i warunki pierwszego zrostu oraz wiarygodną liczbę Zawróconych; przetestować „death warp” przy pełnym ekwipunku i na poziomie 100; zabezpieczyć transport ładunku i wejścia do instancji bez podnoszenia kary za zwykłą porażkę (P 7–16, 74–78, 81–93).

### E. Finał i endgame obiecują stan świata bez reguł jego utrzymania

- **Problem:** nowi gracze mają żyć w Roku Bez Pogrzebu, weterani go kończyć, a serwer ma trwale zmieniać miejsca w ojczyznach; nie ma reguły widoczności, wersjonowania zadań ani kolejności wydarzeń dla graczy na różnych etapach (P 81–88; K 7–10).
- **Dowód:** indywidualny Ślub powstaje dopiero w Akcie 10, ale publiczne progi zmieniają miejsca z Aktów 3–6; po 100 wskazano po jednym zadaniu, aktywności tygodnia 4 i sezonie tygodnia 12 (P 39–43, 86–93, 95–103).
- **Konsekwencja:** nowy gracz może zobaczyć skutki decyzji, których jego historia jeszcze nie dopuściła; weteran po krótkim epilogu dostanie ten sam konwój i licznik zamiast trwałej pętli o większej głębi (P 86–93).
- **Naprawa:** zdefiniować stan osobisty, stan serwera i widoczność każdego zmienianego miejsca; najpierw zbudować jeden pełny cykl po 100 z harmonogramem treści i kosztem live ops, potem obiecywać rotacyjne zmiany świata (P 81–93, 142–148).

## 4. Problemy poważne i mniejsze

- **Pozorna sprawczość:** gracz „decyduje” w ojczyźnie, ale wynik serwera to głos rodów; bez zasad głosu, przegranej opcji i konsekwencji dla jego postaci cztery decyzje są prezentacją, nie wyborem (P 95–104).
- **Nierówna treść ras mimo równej tabeli:** każda rasa ma jeden akt i część umowy, ale orcze Ogniska mogą trwale odebrać kontakt z przodkami, a nieumarli czekają 30 lat na nowych swoich; ciężar strat i emocji jest inny. Testować odbiór wszystkich ras, nie liczyć pól w tabeli (P 17, 98–104; B 19, 22, 26–27).
- **Obrzędy jak klucze:** sam autor widzi ryzyko „zbierania kluczy”; w tabeli faktycznie cztery akty kolejno przyznają cztery nazwane części, a Akt 10 piątą (P 29–44, 95–104).
- **Dziewięć obowiązkowych „praw” podnosi koszt:** kanon daje możliwość jednego prawa na krainę, nie nakaz; kilka propozycji to pojedynczy impuls lub znacznik, wymagający osobnego QA na dwóch serwerach (K 12–13, 43–45; P 111–124).
- **Zmiany Aktu 1 nie są zatwierdzone:** kwestie przy kapliczce i ognik córki Darnika są oznaczone jako dopiski do akceptacji; nie wolno ich traktować jako gotowego fundamentu kolejnych aktów (P 25–27, 91, 154–155; K 26, 28–40).

## 5. Ryzyka, których autor nie widzi

- **Optymalizacja zamiast fabuły:** przy 100 punktach, limicie tylko „tej samej czynności” i równych nagrodach gracze mogą powtarzać najszybsze dowody/dostawy lub zlecać sobie nawzajem transport, omijając sceny i bossów. Reguł antyfarmy i walidacji wkładu brak (P 72–79, 89–93).
- **Pusty świat przy małej populacji:** solo z tablicy całą dobę i publiczne wydarzenie co ~30 minut gwarantują dostępność, lecz mogą zmienić „otwartą wyspę” w instancje i kolejkę zleceń; osobna wyspa w kanonie ma żyć osadą, zbieractwem i wydarzeniami (P 74–76; K 8–11).
- **Licznik zachęca do obchodzenia celu:** progi zależne od aktywnych kont i podział na rasy tworzą bodziec do altów, chwilowej aktywności i farmienia najtańszych dusz; brak definicji „aktywnego konta” i unikatowego odprowadzenia (P 84–93).
- **Odwołania HC mogą zalać obsługę:** apelacja trwa siedem dni, lecz „dziennik ostatnich 30 s” nie określa retencji dowodów ani sposobu rozstrzygania laga i utraty pakietów na granicy dwóch serwerów (P 136–140).

## 6. Pytania przed decyzją

1. Co dokładnie robi gracz na wyspie przez 5–7 godzin Aktu 2, minuta po minucie, gdy gra sam, i czym te czynności różnią się od lochów? (P 62, 74–79; K 8–11)
2. Ile roboczogodzin kosztuje pełny akt: dziesięć lochów, wyspa, boss, wariant solo, cztery rasy, QA i utrzymanie? Jaki koszt na godzinę zabawy jest akceptowalny? (K 8–10, 46; P 142–148)
3. Czy każdy zgon poza jawnie bezpieczną strefą odbiera HC, łącznie z upadkiem, wodą, lawą, transferem i wylogowaniem w walce? Jaki zapis rozstrzyga spór po siedmiu dniach? (P 111–124, 126–140)
4. Jak działa pierwszy zrost, ile osób realnie może go uzyskać i co dzieje się z ładunkiem po śmierci na wyspie? (P 7–16, 23–24, 74–76)
5. Który stan świata widzi gracz przed Aktem 10 po publicznej zmianie ojczyzny? Co dzieje się po przegranym głosowaniu w Akcie 3–6? (P 86–103)
6. Kto i jak często dostarcza nowe warianty po 100 oraz ile godzin tygodniowo mają one utrzymać gracza? (P 89–93, 142–148)

## 7. Oceny (1–10)

| Kryterium | Ocena | Uzasadnienie |
|---|---:|---|
| Sens i spójność | **7** | Zrost, Próg i umowa dobrze korzystają z Aktu 1 i panteonu; demografia Zawróconych pozostaje arbitralna (P 6–18, 29–43; K 31–37; B 10–12). |
| Równość ras | **7** | Grywalność i role formalnie są równe, lecz stawki decyzji rasowych wymagają testu (P 46–55, 95–104; K 16–17). |
| Równość ścieżek: lochy/wyspa | **3** | Równe fakty, punkty i cele median nie dowodzą równej jakości dwóch pętli (P 57–79; K 8–11). |
| Zaciekawienie gracza | **6** | Tajemnica śmierci i cztery perspektywy mają potencjał; powtarzana struktura „rada–dowód–finał” grozi rutyną (P 57–70, 95–104). |
| Wykonalność i koszt | **3** | Wycinek jest sensowną bramką, lecz skala stu lochów, dziesięciu wysp i HC między serwerami pozostaje nieoszacowana (K 7–10, 46; P 138–148). |
| Długowieczność po finale | **3** | Jeden epilog, konwój/afiksy i sezon z licznikiem to zarys, nie plan utrzymania po 50–70 godzinach deklarowanej kampanii (P 77, 81–93). |
