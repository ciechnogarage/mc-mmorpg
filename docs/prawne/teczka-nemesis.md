# Teczka dla prawnika: mechanika „Powracający” a patent US 10,926,179 (Nemesis)

Stan na 2026-10-25. Przygotował agent na zlecenie właściciela serwera (decyzja 25.10: analiza prawnika przed budową).
Status: **czeka na prawnika**. Ta teczka zbiera fakty. Nie zawiera oceny, czy mechanika narusza patent, ani oceny ryzyka.

**Co trzeba wiedzieć na start**
- Serwer: prywatny serwer MMORPG na Minecraft Java (Paper 1.21), z polskim interfejsem. Zakładam, że operator
  i maszyna są w Polsce, a gracze spoza Polski nie są blokowani — **do potwierdzenia przez właściciela**. Moby sterują pluginy MythicMobs,
  instancje lochów plugin MythicDungeons.
- „Powracający” to **plan, nic nie zbudowano**. Opis pochodzi z dokumentów projektowych (źródła w §5).
  Plan nie jest jeszcze zatwierdzonym kanonem gry (KANON.md: „propozycje agentów”). Szczegóły mogą się zmienić,
  także pod wpływem tej analizy.
- Tekst patentu (oryginał, angielski) z URL i datą: `docs/ref/patent-nemesis/US10926179-zastrzezenia.md`.

---

## 1. Mechanika po ludzku

**Jednym zdaniem (z projektu):** „jak zginiesz w lochu, twój Cień — z twoją skórką, imieniem i klasą — zostaje tam
i poluje na innych; a kto go pokona, oddaje ci to, co straciłeś.”

Mechanika ma trzy warstwy. Pierwsza (Cienie) jest główna. Dwie kolejne wchodzą w późniejszych częściach gry.

### 1.1 Cienie Poległych (od lochu 011, czyli od drugiego aktu gry)
1. **Śmierć.** Gracz ginie w lochu od przeciwnika sterowanego przez serwer (moba). Dziś za śmierć gracz traci 10%
   punktów doświadczenia (EXP) bieżącego poziomu.
2. **Migawka.** Serwer zapisuje „migawkę” postaci: skórkę (wygląd), imię lub pseudonim, archetyp i klasę, poziom,
   3 główne zdolności, rodzaj broni, poziom zbroi, miejsce i przyczynę śmierci. Ok. 1 KB w bazie danych.
3. **Depozyt.** Utracone EXP nie znika. Jest zapisane „w Cieniu” jako depozyt.
4. **Limit.** Najwyżej 1 Cień na konto, na akt, na tydzień.
5. **Pojawienie się u innych.** Gdy INNY gracz przechodzi ten loch (osobna kopia lochu, tzw. instancja), w jednym
   stałym miejscu („Rozdroże Cieni”, 1 na instancję) zamiast zwykłego silniejszego przeciwnika (elity) może stanąć Cień.
   Cień walczy z tym graczem jak przeciwnik sterowany przez serwer (sztuczna inteligencja, nie człowiek).
6. **Zachowanie Cienia.** Cień działa według **stałego szablonu archetypu** (np. „Wojownik”). Szablon nie uczy się,
   nie ewoluuje i nie zmienia parametrów w reakcji na inne postacie. Siła jest dopasowana do poziomu lochu,
   a nie do ekwipunku właściciela. W każdym akcie Cień podlega tej samej „regule świata” co gracz
   (np. w akcie 5 zamarza po 3 s bezruchu, w akcie 2 chowa się pod wodę przy przypływie).
7. **Gdy Cienia brak** (pusty serwer, brak zgody, brak zapisu): w tym miejscu stoi zwykła elita albo
   **ręcznie napisany Cień fikcyjny**, jawnie oznaczony jako fikcyjny. Loch jest zawsze kompletny bez innych graczy.
8. **Pokonanie Cienia.** Gracz, który pokonał Cień, dostaje „Odłamek” (waluta na kosmetyki i trofea, przypisana
   do konta; tylko pierwsza taka wygrana w tygodniu). Z samego Cienia nie ma EXP ani łupu.
   Właściciel Cienia dostaje powiadomienie (przykład z projektu: „Kuba ukoił twój Cień na Bagnach”)
   i zwrot EXP: najwyżej tyle, ile stracił, raz na Cień.
9. **Wygaśnięcie.** Jeśli nikt nie pokona Cienia w 7 dni, właściciel dostaje 50% depozytu, a Cień znika.
10. **Prywatność.** Domyślnie pseudonim i zatwierdzony wygląd archetypu. Prawdziwy nick i skórka tylko za zgodą
    gracza. Brak publicznych rankingów zabójstw. Zgłoszenie, rezygnacja, cofnięcie zgody.
    Posągi Cieni w mieście głównym tylko za zgodą, rotacyjnie.
11. **Pilot.** Najpierw tylko 3 lochy (011–013), 1 szablon (Wojownik), pomiar wydajności i tego, ile Cieni ktoś pokonał.

### 1.2 Echa (od aktu 4)
Zapis przejścia lochu przez gracza staje się duchem-przewodnikiem dla nowych graczy (pokazuje trasę, bez sekretów).
Echo **nie walczy** z graczem. Dostępne dopiero po pierwszym samodzielnym przejściu. Autor widzi licznik
„poprowadziłeś N osób”, bez nagrody.
„Strażnicy Bramy”: pierwsza drużyna, która przejdzie finał aktu — tylko za zgodą, na jeden sezon.
(Wcześniejsza wersja planu mówiła, że ich Cienie „egzaminują” późniejszych graczy; wersja v2 tego nie rozstrzyga.)

### 1.3 Wezwanie (od aktu 6)
Gracz może przywołać Cień **znajomego** (za jego wcześniejszą zgodą) jako **sojusznika** na 60 s w trudnej walce.
Raz na przejście. Ten Cień walczy z mobami, nie z graczem. Właściciel dostaje powiadomienie i kosmetyczny „ślad pomocy”.

### 1.4 Czego w planie NIE ma (świadomie wycięte)
- Hierarchii, frakcji, rang i awansów przeciwników (synteza v1: „nie robimy hierarchii NPC, awansów, zemsty NPC”).
- Zmiany parametrów jednego przeciwnika w reakcji na walkę z innym przeciwnikiem (synteza v2: „bez zmiany
  parametrów jednego NPC w reakcji na innego”).
- „Opętania” (żywy gracz sterujący swoim Cieniem przeciw innym) — wycięte 09.10.
- Mob, który zabił gracza, w planie się nie zmienia (nie awansuje, nie zapamiętuje gracza).

### 1.5 Czego plan nie rozstrzyga (luki faktów)
- Czy śmierć gracza **od Cienia** tworzy nowy Cień (plan mówi tylko „z prawidłowym przeciwnikiem”).
- Czy Cień, który pokonał gracza, coś zyskuje (licznik, sława). Wersja v1 miała „Cień, który pokonał najwięcej
  graczy w tygodniu, staje jako posąg” i pierwszeństwo dla Cieni „sławnych”. Wersja v2 usuwa publiczne rankingi,
  ale nie mówi wprost, czy licznik zwycięstw Cienia istnieje.
- Co dokładnie widzi gracz walczący z Cieniem (napis nad głową: pseudonim? poziom? klasa?).
- Kolejność wyboru Cienia do slotu. Wersja v1: najpierw znajomi i gildia, potem świeże zgony, potem „sławne”.
  Wersja v2 tego nie powtarza.

---

## 2. Zastrzeżenia niezależne obok naszej mechaniki

Patent ma 36 zastrzeżeń, w tym **3 niezależne: 1, 19 i 36**. Wszystkie trzy mają ten sam rdzeń (A–D niżej).
Różnią się formą: 1 = sposób (method), 19 = urządzenie z procesorem, pamięcią i wyświetlaczem,
36 = urządzenie w formule „means for…”.
Kolumna prawa podaje **tylko fakty z planu**. Nie ocenia, czy element jest spełniony.

### 2.1 Zastrzeżenie 1 (sposób)

| Element zastrzeżenia 1 (skrót polski; oryginał w `docs/ref/patent-nemesis/`) | Jak działa nasza mechanika (fakt z planu) |
|---|---|
| **A1.** Procesor steruje zdarzeniami gry komputerowej. | Serwer Minecraft (Paper) z pluginami. Gra działa na serwerze, gracz łączy się klientem Minecraft. |
| **A2.** W zdarzeniach bierze udział awatar sterowany wejściem od gracza. | Postać gracza w Minecraft, sterowana przez gracza. |
| **A3.** …oraz pierwsza postać niezależna (NPC), sterowana przez procesor tak, by reagowała na awatary i automatycznie im się przeciwstawiała, na podstawie „pierwszych parametrów postaci” w pamięci komputera. | Mob w lochu (MythicMobs), który atakuje graczy według swojej konfiguracji (życie, obrażenia, umiejętności). |
| **B.** Procesor wykrywa wystąpienie z góry określonego zdarzenia gry, obejmującego interakcję awatara z pierwszym NPC. | Plugin „Cienie” wykrywa śmierć gracza w lochu zadaną przez moba („prawidłowy przeciwnik”). Śmierć gracza jest wprost wymieniona w zależnym zastrz. 2. |
| **C1.** Procesor zmienia „drugie parametry postaci” zapisane w pamięci, służące do sterowania drugim NPC, na podstawie tego wykrycia. | Po śmierci serwer **tworzy nowy zapis** Cienia: migawka (wygląd, imię/pseudonim, archetyp, klasa, poziom, 3 zdolności, broń, zbroja, miejsce, przyczyna) + depozyt EXP. Przed śmiercią tego zapisu nie ma. Szablon zachowania (AI archetypu) jest stały i wspólny dla wszystkich Cieni danego archetypu; zapis wypełnia go danymi gracza. Siła zależy od poziomu lochu. Mob, który zabił gracza, się nie zmienia. |
| **C2.** Drugi NPC jest sterowany przez procesor tak, by reagował na awatary i automatycznie im się przeciwstawiał, na podstawie drugich parametrów. | Cień w cudzej instancji lochu walczy z innym graczem jako przeciwnik sterowany przez AI serwera (nie przez człowieka). Wyjątek: Cień z „Wezwania” (akt 6+) walczy po stronie gracza, przeciw mobom. Echo (akt 4+) nie walczy. |
| **D.** Wyjście na urządzenie wyjściowe wskazania drugich parametrów, które zostały zmienione. | Cień pojawia się w grze z pseudonimem (albo nickiem za zgodą) i wyglądem archetypu (albo skórką za zgodą), w zdolnościach i broni z migawki. Właściciel Cienia dostaje powiadomienie po jego pokonaniu („Kuba ukoił twój Cień na Bagnach”). Dokładny wygląd napisu nad Cieniem nie jest ustalony (§1.5). |

### 2.2 Zastrzeżenie 19 (urządzenie)
Elementy A1–D jak w zastrz. 1, wykonywane przez urządzenie z **procesorem, pamięcią i wyświetlaczem**
(pamięć przechowuje instrukcje).

| Element dodatkowy zastrz. 19 | Fakt u nas |
|---|---|
| Procesor + pamięć z instrukcjami | Serwer (maszyna wirtualna na hoście właściciela) z pluginami Java. |
| Wyświetlacz połączony z procesorem | Serwer nie ma wyświetlacza dla gracza. Obraz widzi gracz na **własnym komputerze** (klient Minecraft, oprogramowanie Mojang/Microsoft), połączonym z serwerem przez internet. |
| A1–D | jak w tabeli 2.1 |

### 2.3 Zastrzeżenie 36 (urządzenie „means for…”)
Elementy A1–D jak w zastrz. 1, zapisane jako „środki do…” (means for controlling / detecting / changing / outputting).
Uwaga do tekstu: w zastrz. 36 pierwszy NPC jest „controlled by the processor”, choć zastrzeżenie nie wymienia procesora
jako elementu. Fakty po naszej stronie jak w tabeli 2.1; dla formuły „means for” liczy się, jakie struktury opisuje
specyfikacja patentu (to pytanie do prawnika, §3).

### 2.4 Zależne zastrzeżenia, które dotyczą podobnych rzeczy (dla kontekstu)

| Zastrz. zależne (od 1; odpowiednik od 19) | Treść w skrócie | Fakt u nas |
|---|---|---|
| 2 (20) | Zmiana uruchamiana śmiercią awatara albo wejściem do nowej strefy | Cień powstaje po śmierci gracza w lochu. |
| 3 (21) | Zmiana statusu drugiego NPC we frakcji NPC | Plan nie ma frakcji Cieni (§1.4). |
| 4 (22) | Frakcja = hierarchia z rangami związanymi z siłą; zmiana rangi | Plan nie ma rang ani awansów. |
| 5–9 (24–28) | Dane frakcji wysłane do innej, niezależnie działającej instancji gry innego użytkownika; śledzenie, czy ten pokonał najwyższy rangą; korzyść w grze; powiadomienie | Cień jednego gracza pojawia się w instancji lochu innego gracza **na tym samym serwerze**; pokonanie daje zwycięzcy Odłamek, właścicielowi powiadomienie i zwrot EXP. Te zastrzeżenia zależą jednak od 3–4 (frakcja, ranga), których plan nie ma. |
| 11 (29) | Zmieniane parametry: wygląd, cecha osobowości, zdolność, „wynik interakcji” zwiększający szansę NPC na sukces z tym graczem, „ośrodek władzy” | Migawka niesie wygląd (skórka/pseudonim) i zdolności gracza. Plan nie ma cech osobowości, „wyniku interakcji” ani ośrodków władzy. |
| 13 (31) | Dobór dialogu NPC na podstawie zdarzenia | Wersja v1: Cień niesie „ostatnie słowo” — jeden z 3 gestów wybrany przez gracza, bez wolnego tekstu. Wersja v2 tego nie powtarza. |
| 14 (32) | Zmiana parametrów także pierwszego NPC (tego, z którym była interakcja) | Mob, który zabił gracza, się nie zmienia. Otwarte: czy Cień, który pokona gracza, coś zyskuje (§1.5). |

### 2.5 Rodzina patentu (wg Google Patents, tylko USA)
- **US 11,660,540 B2** (kontynuacja, udzielony 2023-05-30): niezależne 1, 14. Zamiast „wyjścia wskazania zmienionych
  parametrów” (D) ma „sterowanie drugim NPC na podstawie zmienionych parametrów”; zdarzenie = „wynik interakcji spełnia
  warunek”.
- **US 12,201,908 B2** (udzielony 2025-01-21): niezależne 1, 13, 20. Wymagają frakcji z hierarchią NPC.
- **US 2025/0108303 A1** (zgłoszenie, opublikowane 2025-04-03, w toku): niezależne 37, 44, 51. Zmiana **statusu**
  NPC po wyniku interakcji wpływa na parametry sterowania drugim NPC; bez wymogu frakcji. Zastrzeżenia mogą się zmienić.
Pełne teksty: `docs/ref/patent-nemesis/US10926179-zastrzezenia.md`. Tych patentów zleceniodawca nie objął zadaniem;
podane, bo recenzje projektu (04-spor-X, 04-spor-Y) prosiły o sprawdzenie rodziny.

---

## 3. Pytania do prawnika

**Najważniejsze**
1. Czy **utworzenie nowego zapisu** przeciwnika (Cienia) z danych gracza po jego śmierci, przy stałym szablonie AI,
   to „zmiana drugich parametrów postaci drugiego NPC” w rozumieniu zastrz. 1/19/36 — czy „zmiana” wymaga NPC,
   który istniał wcześniej? Na co zwraca uwagę historia prosecution (patent odrzucano i poprawiano kilka razy, 2016–2020)?
2. Czy patent USA dotyczy nas w ogóle: serwer w Polsce, operator w Polsce, część graczy może łączyć się z USA,
   gracz widzi obraz na własnym komputerze. Czy rodzina ma odpowiedniki w Europie (EP/PL) — Google pokazuje tylko USA;
   prośba o sprawdzenie w Espacenet/INPADOC.
3. Które z wariantów w §4 (i w jakim połączeniu) według Pana/Pani oddalają mechanikę od zastrzeżeń niezależnych
   **całej rodziny**, w tym zgłoszenia US 2025/0108303 A1, które może zostać udzielone szerzej?

**Pozostałe**
4. Czy element D („wyjście wskazania zmienionych parametrów”) dotyczy samego pokazania Cienia w grze, czy wymaga
   pokazania parametrów (np. liczb, rangi)? Jaki wpływ ma powiadomienie właściciela Cienia?
5. Czy Cień, który pojawia się w instancji INNEGO gracza (nie tego, który zginął), ma znaczenie dla elementu C
   („na podstawie wykrycia”)?
6. Zastrz. 19 wymaga wyświetlacza połączonego z procesorem: jak to się ma do układu serwer–klient, gdzie wyświetlacz
   jest po stronie gracza?
7. Zastrz. 36 w formule „means for”: jakie struktury ze specyfikacji patentu wyznaczają jego zakres?
8. Shadow of Mordor wyszedł 30.09.2014, pierwszeństwo patentu to 26.03.2015. Czy to ma znaczenie dla ważności
   lub zakresu (np. okres ochronny dla ujawnień twórcy)? Czy patent był kiedyś kwestionowany (IPR, reexamination,
   spór sądowy)? Nasze pobieżne wyszukiwanie niczego nie znalazło — to nie dowód.
9. Czy znane gry z okresu przed 2015 (§5, „podobne mechaniki”) mają znaczenie dla naszej sytuacji?
10. Status utrzymania patentu (opłaty) i data wygaśnięcia: Google podaje „Active” i 2036-08-11. Prośba o potwierdzenie
    w USPTO.
11. Czy serwer jest „komercyjny” w sensie, który ma tu znaczenie? Plan: sprzedaż tylko kosmetyki i drobnych udogodnień,
    bez przewagi w grze (do potwierdzenia przez właściciela).
12. Czego unikać w dokumentacji i materiałach promocyjnych (np. porównań do „Nemesis”)?

---

## 4. Warianty zmian mechaniki

Każdy wariant to propozycja techniczna do oceny przez prawnika. Nie twierdzimy, który „wystarcza”.
Koszt dla rozgrywki opisany z perspektywy gracza.

| # | Zmiana | Który element zastrzeżeń dotyka (fakt: co znika z mechaniki) | Koszt dla rozgrywki |
|---|---|---|---|
| W1 | **Cień tylko z przyczyn innych niż walka z mobem**: Cień powstaje przy każdej śmierci w lochu (pułapka, upadek, mob) albo z wyboru gracza („zostaw ślad” przy ognisku), bez rozróżniania przyczyny. | Wyzwalacz przestaje być „interakcją awatara z pierwszym NPC” (element B). | Mały. Gracz dalej widzi swój Cień u innych. Tracimy tylko „zemstę na tym, co mnie zabiło” (której i tak nie ma). Możliwe nadużycie: celowe skoki w przepaść — rozwiązuje to istniejący limit 1/tydzień. |
| W2 | **Cień wyłącznie z wyboru gracza, nie ze śmierci**: po ukończeniu lochu gracz może zostawić swój „Ślad” (migawka) dla innych. | Brak związku z interakcją z NPC i ze śmiercią (B, zależne 2). | Średni. Znika „śmierć ma społeczny skutek” i zwrot EXP przez innych (depozyt trzeba zastąpić: np. zwykła kara śmierci bez zwrotu). Cień staje się wyzwaniem od lepszego gracza, a nie pamiątką porażki — inna emocja, mniej „historii”. |
| W3 | **Cień nie walczy z innymi**: tylko widmo-pamiątka (nieruchome, z gestem) albo przewodnik jak Echo. | Drugi NPC przestaje „automatycznie przeciwstawiać się awatarom” (C2). | Duży. Znika główna atrakcja (walka z Cieniem kolegi). Zostaje warstwa społeczna (ślady, zwrot EXP za np. „ukojenie” przez rytuał zamiast walki). |
| W4 | **Cienie tylko jako sojusznicy** (rozbudowane Wezwanie): po śmierci Twój Cień pomaga innym graczom w walce, a za każdą pomoc odzyskujesz część EXP. | Drugi NPC nie przeciwstawia się awatarom (C2). | Średni. Mechanika staje się „dobroczynna” (wzór Nioh, duchy pomocnicze). Tracimy walkę z Cieniem; zyskujemy współpracę. Trzeba zbalansować, by sojusznik nie ułatwiał lochów za bardzo. |
| W5 | **Cienie tylko fikcyjne, pisane ręcznie** (bez danych graczy): każdy loch ma Cienie postaci z fabuły; śmierć gracza jedynie wybiera, który z gotowych Cieni się pojawi, albo wcale na to nie wpływa. | Brak zmiany parametrów na podstawie zdarzenia (C1), jeśli wybór nie zależy od śmierci. | Duży. Znika „ubiłem Cień Kuby” — sedno pomysłu. Zostaje zwykła elita z fabułą. Najtańsze w budowie i moderacji. |
| W6 | **Bez powiadomień i bez widocznych danych gracza na Cieniu**: Cień zawsze anonimowy, właściciel nie dostaje wiadomości, zwrot EXP przychodzi bez informacji, kto i gdzie. | Dotyka elementu D (wyjście wskazania) — o ile prawnik uzna, że D dotyczy tych komunikatów. | Średni. Słabnie poczucie więzi („ktoś ukoił mój Cień”). Mechanika działa, ale jest mniej opowiadana między graczami. Lepsza prywatność. |
| W7 | **Twardy zakaz „pamięci” przeciwnika**: zapisane w specyfikacji pluginu, że Cień i mob nigdy nie zmieniają się po walce (bez liczników zwycięstw, posągów za zabójstwa, „sławnych” Cieni), a Cień ginący gracz nie tworzy nowego Cienia. | Zamyka luki z §1.5 w stronę braku zależnych 3–4, 11, 14 (frakcja, ranga, „wynik interakcji”, zmiana pierwszego NPC). Nie dotyka rdzenia A–D. | Mały. Tracimy posągi „najgroźniejszego Cienia tygodnia” i rekordy. Zostaje cała reszta. |
| W8 | **Wstrzymać Cienie, zostawić Echa** (przewodnik, który nie walczy): plan stu lochów działa na regułach aktów; Cienie wracają po opinii prawnika. | Echo nie przeciwstawia się awatarom (C2). | Średni. Plan zakłada, że każdy loch jest kompletny bez Cienia (elita zastępcza), więc gra działa. Tracimy wyróżnik serwera do czasu decyzji. |

Warianty można łączyć (np. W1 + W6 + W7). Wybór należy do właściciela po opinii prawnika.

---

## 5. Źródła

Statusy: **potwierdzone** = tekst pobrany z oficjalnego źródła i zapisany; **jedno źródło** = jedno źródło, niepotwierdzone
drugim; **wewnętrzne** = nasz dokument projektowy; **brak źródła** = nie sprawdzono.

| Źródło | Co z niego | Status |
|---|---|---|
| [Google Patents US10926179B2](https://patents.google.com/patent/US10926179B2/en), pobrane 2026-10-25; kopia zastrzeżeń: `docs/ref/patent-nemesis/US10926179-zastrzezenia.md` | Tekst 36 zastrzeżeń, niezależne 1/19/36, daty, rodzina | potwierdzone (tekst); daty, status „Active” i rodzina: jedno źródło (Google), do potwierdzenia w USPTO |
| [PDF patentu](https://patentimages.storage.googleapis.com/45/aa/b5/7d07e883c25b26/US10926179.pdf) | Urzędowy dokument | nie porównany z tekstem HTML |
| [US11660540B2](https://patents.google.com/patent/US11660540B2/en), [US12201908B2](https://patents.google.com/patent/US12201908B2/en), [US20250108303A1](https://patents.google.com/patent/US20250108303A1/en), pobrane 2026-10-25 | Niezależne zastrzeżenia rodziny | jedno źródło (Google) |
| [Wikipedia: Middle-earth: Shadow of Mordor](https://en.wikipedia.org/wiki/Middle-earth:_Shadow_of_Mordor) | Premiera 30.09.2014 | jedno źródło |
| Wyszukiwanie „10,926,179 IPR / litigation / reexamination” (2026-10-25): [Patent Arcade](https://www.patentarcade.com/video-game-patent/US10926179/nemesis-characters-nemesis-forts-social-vendettas-and-followers-in-computer-games), [Finnegan, 13.07.2021](https://www.finnegan.com/en/insights/articles/will-101-be-a-nemesis-to-warner-brothers-patent.html) | Brak wyników o sporach; Finnegan pisze o kwalifikowalności (35 U.S.C. §101) | brak źródła dla „brak sporów” (wynik wyszukiwarki, strony nie otwarte); do sprawdzenia w PTAB/PACER |
| `docs/lochy/plan100-v2/05-synteza-v2.md` §R1 (09.10) | Wiążąca wersja Cieni, Ech, Wezwania; wycięcie Opętania; pilot | wewnętrzne (najnowsza wersja) |
| `docs/lochy/plan100-v2/01-propozycja-C.md` §A1 (09.10) | Pierwotny opis Cieni: migawka, depozyt, Rozdroża, pierwszeństwo, posągi, koszt | wewnętrzne (wersja wcześniejsza; część zmieniona w v2) |
| `docs/lochy/plan100-v2/03-synteza-v1.md` §1 | Trzy warstwy, „ostatnie słowo”, „bez hierarchii NPC” | wewnętrzne (zastąpione przez v2 w części) |
| `docs/lochy/plan100-v2/23-synteza-v3-dwie-sciezki.md` §S6 | Cienie na wyspach: tylko po pilocie, fikcyjne ślady | wewnętrzne |
| `docs/lochy/plan100-v2/10-akt-02-archipelag.md`, `10-akt-05-mrozna-korona.md` | Zachowanie Cienia w regułach aktów, lista Rozdroży i elit zastępczych, pilot | wewnętrzne |
| `docs/lochy/plan100-v2/04-spor-X.md` §7, `04-spor-Y.md` §7 | Uwagi recenzentów (agentów AI) o patencie: rdzeń zastrz. 1/19/36 szerszy niż „hierarchia” | wewnętrzne; to opinie agentów AI, nie porady prawne |
| `docs/KANON.md` | Powracający = propozycja agentów, nie kanon | wewnętrzne |
| Kara śmierci dziś: MMOCore `death-exp-loss` 10% (skill `mc-walka-klasy-zaklecia`) | -10% EXP poziomu | wewnętrzne |
| Podobne mechaniki z innych gier wymienione w projekcie: Demon's Souls (Old Monk, 2009), Dark Souls (plamy krwi, fantomy, 2011), Forza Drivatar (2013–2014), Nioh (Revenants, 2017 — po dacie pierwszeństwa), Deathloop (2021) | Tło; linki w `01-propozycja-C.md` i `04-spor-*.md` | brak źródła w tej teczce (daty z pamięci projektu, niesprawdzone) |
