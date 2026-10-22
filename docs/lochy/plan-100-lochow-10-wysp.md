# Plan: 100 lochów i 10 wysp (v0.1, 2026-09-29)

Propozycja do akceptacji właściciela. Rozwija decyzje z `akt1-wyspa-i-lochy.md` (dwie niezależne ścieżki,
łup „ten sam, ale nie ten sam”) na całą grę: **10 aktów × (10 lochów + 1 wyspa) = poziomy 1–100**.
Akt 1 zgodny z decyzją D5 (Akt 1 do poziomu 10) — stary przedział 1–20 z `akt1-wyspa-i-lochy.md` §4 jest nieaktualny.

## 1. Jedna siatka 10 × 10

Cała gra to siatka: **wiersz = akt (kraina i jej wyspa), kolumna = rola lochu w akcie**.
Komórka to para **loch N ↔ strefa wyspy N** (N = 10·(akt−1) + kolumna).

„To samo, ale inne” działa na dwóch osiach naraz:

- **W komórce (loch ↔ strefa):** ten sam poziom, klimat, rodzina wrogów, ten sam rodzaj i tier łupu —
  ale inny rytm (loch: zwarty przebieg i boss z fazami; strefa: swobodna eksploracja, elity, wydarzenia),
  inne konkretne przedmioty i wygląd.
- **W kolumnie (akt ↔ akt):** kolumna 2 to zawsze „rozkład i statusy”, kolumna 6 zawsze „oręż i tarcze” — gracz wie,
  czego się spodziewać, ale każdy akt robi to we własnej krainie i z własnym **prawem aktu** (§3).

| Kolumna | Rola (lekcja) | Żywioł-pokrewieństwo | Wzór w Akcie 1 |
|---|---|---|---|
| K1 Próg | wejście w krainę, uczy prawa aktu | natura krainy | 001 Kwietna Polana |
| K2 Rozkład | statusy, trucizny, spowolnienia — z jasną kontrą | gnicie, jad | 002 Bagna Zgniłych Korzeni |
| K3 Wiedza | przerywanie kanałów, zasięg widzenia | woda, pismo, pieśń | 003 Zatopione Ruiny Archiwum |
| K4 Kryształ | pociski, pozycjonowanie, odbicia | kryształ, światło | 004 Kryształowe Jaskinie |
| K5 Mechanizm | pułapki, patrole, zagadki maszyn | kamień, metal | 005 Opuszczona Kopalnia Nadzorcy |
| K6 Oręż | tarcze, parowanie, tempo walki | wojsko, forteca | 006 Złamana Forteca |
| K7 Żywioł terenu | teren jest przeciwnikiem | mróz, nurt, wiatr | 007 Mroźna Przełęcz |
| K8 Ogień | okna ataku, burst, sprawdzian obrażeń | ogień, żar | 008 Popielne Sanktuarium |
| K9 Kość | przywołania, klątwy, kolejność celów | śmierć, duchy | 009 Pustkowia Kości |
| K10 Brama | egzamin aktu, boss aktu | wszystko naraz | 010 Brama Pierwszego Aktu |

## 2. Przeplatanie: warkocz lochów i wysp

Kolejność zalecana, nigdy wymagana: **loch N → strefa N → loch N+1 → strefa N+1 → …** Obie nitki same wystarczą
do przejścia aktu; spleciona daje więcej.

- **Poziom.** Loch N: zalecany poziom postaci N, otwiera się od poziomu N−1 (loch 001 od razu). Strefa N ma ten sam
  przedział, bez twardej blokady — tylko groźniejsze bestie. Wyspa aktu k otwiera się od poziomu 10·(k−1)+1.
- **Fabuła na dwie ręce.** Strefa pokazuje skutek, loch przyczynę (albo odwrotnie). Przykład komórki 002: na Mokradłach
  rybacy uciekają przed powodzią; w lochu gracz dowiaduje się, kto zamknął śluzy. Każda połowa jest pełną historią sama.
- **Klucz i mapa.** W strefie N leży klucz do ukrytej komnaty lochu N; w lochu N wypada mapa skarbu strefy N.
  Bez nich nic nie tracisz poza bonusem.
- **Materiał komórki.** Obie ścieżki dają te same RODZINY materiałów profesji (np. komórka 002: jad, zioła bagienne,
  glina, włókno trzcin) — rzemiosło nie zależy od wyboru ścieżki.
- **Receptura mieszana.** Przedmiot z lochu N + przedmiot ze strefy N → lepszy wariant (roll albo efekt użytkowy,
  nie siła PvP).
- **Brama aktu.** Loch 10·k (K10) i Serce Wyspy k to dwa oblicza tego samego finału: instancja z bossem aktu
  i otwarty boss świata na zegarze. Pokonanie któregokolwiek daje tytuł aktu; obu — tytuł „Splecionego”.

## 3. Dziesięć aktów

Każdy akt dokłada **jedno prawo** — globalną mechanikę obecną we wszystkich jego lochach i na całej wyspie.
Kolumny są stałe, prawa się zmieniają: stąd „to samo, ale inne” między aktami.

| Akt | Poziomy | Kraina / wyspa | Prawo aktu | Transport wyspy |
|---|---|---|---|---|
| 1 | 1–10 | **Zielona Granica** — Kwietna Wyspa | Podstawy: unik, blok, przerwanie, osłona | tyrolki, lotnia |
| 2 | 11–20 | **Archipelag Słonych Mgieł** | Przypływ: woda rośnie i opada w cyklu, areny zmieniają kształt | łodzie |
| 3 | 21–30 | **Złote Piaski** | Żar: w słońcu rośnie przegrzanie, cień i woda chłodzą | karawany |
| 4 | 31–40 | **Głębia** (podziemne królestwo) | Ciemność: światło jest zasobem, w mroku wrogowie silniejsi | wagoniki |
| 5 | 41–50 | **Mroźna Korona** | Wychłodzenie: licznik zimna, ogniska jako kotwice | sanie |
| 6 | 51–60 | **Srebrny Las** | Fazy Księżyca: noc i dzień zmieniają wrogów i ścieżki | ścieżki księżyca |
| 7 | 61–70 | **Ziemie Paktu** (front wojny) | Front: sojusznicy NPC, wybór strony w starciach | wilki bojowe |
| 8 | 71–80 | **Popielna Korona** | Erupcja: lawa w cyklu, teren zapada się i zastyga | zastygające mosty |
| 9 | 81–90 | **Podniebie** (wyspy w chmurach) | Wiatr i Burza: pchnięcia, pioruny, lot | gryfy |
| 10 | 91–100 | **Kres** (Serce Świata) | Echo: każdy loch miesza prawa poprzednich aktów | portale echa |

Frakcje (Przymierze Latarni i Księżyca, Pakt Krwi i Kości) mają swoje krainy w Aktach 6 i 7; wiary i Starsi
(imiona robocze, D1 czeka) wchodzą najpóźniej w Akcie 10. Akt 1 zostaje bez wiązania z wiarami (decyzja 29.09).

## 4. Wszystkie komórki

Nazwy robocze. Pierwsza linia komórki: **loch**; druga: *strefa wyspy*. Akt 1 = dotychczasowa drabina 001–010.

### Akt 1 — Zielona Granica (Kwietna Wyspa), poziomy 1–10

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 001 | Kwietna Polana | Kwietne Zbocza | światło kontra korzenie |
| 002 | Bagna Zgniłych Korzeni | Mokradła Rechotu | śluzy i Topielica (spec: `loch-002-bagna-zgnilych-korzeni.md`) |
| 003 | Zatopione Ruiny Archiwum | Wybrzeże Wraków | przerwij pieśń, zanim woda wróci |
| 004 | Kryształowe Jaskinie | Góry Echa | pociski odbijane od ścian |
| 005 | Opuszczona Kopalnia Nadzorcy | Wyrobiska | pułapki i patrole Nadzorcy |
| 006 | Złamana Forteca | Ruiny Strażnic | tarcze i parowanie |
| 007 | Mroźna Przełęcz | Śnieżne Szczyty | lawina jako przeciwnik |
| 008 | Popielne Sanktuarium | Wulkaniczne Zbocze | okno ataku po meteorze |
| 009 | Pustkowia Kości | Pustkowia | przywołania i obeliski |
| 010 | Brama Pierwszego Aktu | Serce Kwietnej Wyspy | egzamin aktu |

### Akt 2 — Archipelag Słonych Mgieł, poziomy 11–20

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 011 | Przystań Topielców | Port Mgieł | pierwszy przypływ zalewa nabrzeże |
| 012 | Rafa Parzydeł | Płycizny Parzydeł | oparzenia meduz, odtrutka z wodorostów |
| 013 | Latarnia Syren | Skały Syren | pieśń syren do przerwania, mgła zasłania |
| 014 | Perłowa Grota | Laguna Pereł | perły odbijają pociski |
| 015 | Doki Przemytników | Zatoka Przemytników | dźwigi, zapadnie, patrole |
| 016 | Okręt Flagowy Czarnej Floty | Cmentarzysko Okrętów | abordaż, tarcze abordażników |
| 017 | Wir Czarnej Toni | Otwarte Morze | prąd i wir wciągają |
| 018 | Płonąca Flota | Wraki Płonącej Floty | beczki prochu jako okno ataku |
| 019 | Zatoka Utopców | Mgliste Mielizny | utopce wracają, klątwa morska |
| 020 | Tron Głębinowca | Oko Cyklonu | boss aktu: lewiatan i odpływ areny |

### Akt 3 — Złote Piaski, poziomy 21–30

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 021 | Brama Karawan | Szlak Karawan | pierwsze słońce, pierwszy cień |
| 022 | Ogrody Zatrutych Palm | Oaza Jadu | jad skorpionów, woda leczy |
| 023 | Biblioteka pod Piaskiem | Ruiny Obserwatorium | kapłani słońca kanałują |
| 024 | Szklane Wydmy | Szklane Pole | promień odbijany lustrami |
| 025 | Grobowiec Zapadni | Dolina Grobowców | klasyka pułapek |
| 026 | Koszary Słonecznej Gwardii | Posterunki Gwardii | falanga z tarcz |
| 027 | Burza Piaskowa | Morze Wydm | burza zabiera widoczność |
| 028 | Kuźnia Słońca | Czerwone Kaniony | soczewki skupiają ogień |
| 029 | Nekropolia Faraona bez Imienia | Pole Obelisków | mumie i klątwy |
| 030 | Piramida Zaćmienia | Zaćmione Miasto | boss aktu gasi słońce |

### Akt 4 — Głębia, poziomy 31–40

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 031 | Zejście Tysiąca Stopni | Wrota Głębi | pierwsza latarnia górnicza |
| 032 | Grzybowe Topiele | Las Grzybni | zarodniki i halucynacje |
| 033 | Hala Runicznych Kamieni | Hala Run | runy do przerwania w ciemności |
| 034 | Geoda Śpiewająca | Geody | rezonans kryształów |
| 035 | Kopalnia Mechanicznych Górników | Szyby Kopalniane | wagoniki i maszyny |
| 036 | Twierdza Kamiennego Tarczownika | Bastion Głębi | krasnoludzka garda |
| 037 | Czarna Rzeka | Podziemne Jeziora | nurt i zalewanie korytarzy |
| 038 | Wielka Huta | Kotlina Hut | stopiony metal jako okno |
| 039 | Krypty Zapomnianych Królów | Katakumby | duchy przodków |
| 040 | Tron pod Górą | Serce Góry | boss aktu w całkowitej ciemności |

### Akt 5 — Mroźna Korona, poziomy 41–50

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 041 | Brzegi Kry | Lodowy Port | pierwszy chłód, pierwsze ognisko |
| 042 | Zmarźlina Czarnego Szronu | Sosnowa Tajga | szron, który zatruwa |
| 043 | Klasztor Zamarzniętych Dzwonów | Klasztorna Grań | dzwony do przerwania |
| 044 | Lodowe Zwierciadła | Zwierciadlane Jeziora | lód odbija pociski |
| 045 | Śluza Olbrzymów | Wąwozy Lawin | lawiny-pułapki |
| 046 | Warownia Jarlów | Ziemia Jarlów | ściana tarcz |
| 047 | Pękający Lodowiec | Lodowiec | szczeliny otwierają się pod stopami |
| 048 | Serce Zimowego Ognia | Gorące Źródła | jedyne ciepło = okno ataku |
| 049 | Kurhany Śnieżnych Upiorów | Pola Kurhanów | draugry wstają z kurhanów |
| 050 | Tron Mroźnej Korony | Szczyt Korony | boss aktu: zamieć |

### Akt 6 — Srebrny Las, poziomy 51–60

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 051 | Brama Srebrnych Koron | Skraj Srebrnego Lasu | noc zmienia ścieżkę |
| 052 | Gnijące Sny | Mokradła Snów | senność i koszmary |
| 053 | Wieża Gwiazdoczytów | Obserwatorium Kniei | astromanci kanałują |
| 054 | Księżycowe Kryształy | Polana Kryształowego Światła | pociski światła |
| 055 | Labirynt Żywopłotu | Ogrody Iluzji | iluzje i strażnicy |
| 056 | Straż Białego Jelenia | Posterunki Kniei | elfie ostrza, tempo |
| 057 | Rozlewiska Księżycowego Przypływu | Rozlewiska | woda idzie za księżycem |
| 058 | Płonące Drzewo Matka | Pogorzelisko | uratuj albo spal |
| 059 | Cmentarz Starych Drzew | Kamienne Kręgi | duchy drzew |
| 060 | Pałac Zaćmionego Księżyca | Serce Kniei | boss aktu między fazami księżyca |

### Akt 7 — Ziemie Paktu, poziomy 61–70

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 061 | Pogranicze Palisad | Ziemia Niczyja | wybór pierwszego sojusznika |
| 062 | Zaraza Obozowa | Bagna Poległych | zaraza w obozie |
| 063 | Wieża Szamanów Burzy | Wzgórza Totemów | totemy do przerwania |
| 064 | Żyły Krwawego Kryształu | Krwawe Żyły | kryształ, który pije krew |
| 065 | Machiny Oblężnicze | Obóz Oblężniczy | katapulty i zapadnie |
| 066 | Arena Czempionów | Areny Klanów | pojedynki na tempo |
| 067 | Pole Bitwy we Mgle | Wielkie Pole Bitwy | ostrzał i okopy |
| 068 | Rzeźnia Wodza | Warownia Wodzów | berserk jako okno |
| 069 | Nekropolia Podniesionych | Doliny Kości | nekromanci i przywołańcy |
| 070 | Tron Paktu | Serce Paktu | boss aktu, wybór sojuszu |

### Akt 8 — Popielna Korona, poziomy 71–80

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 071 | Popielny Szlak | Popielne Równiny | pierwsza erupcja |
| 072 | Siarkowe Jeziora | Siarkowe Wyziewy | kwaśne opary |
| 073 | Świątynia Płomiennych Kapłanów | Kaplice Żaru | kanały ognia |
| 074 | Obsydianowe Iglice | Obsydianowy Las | odłamki obsydianu |
| 075 | Kuźnie Salamander | Kuźnie Salamander | lawa w rynnach |
| 076 | Cytadela Smoczych Rycerzy | Smocze Wały | tarcze ze smoczej łuski |
| 077 | Rzeka Magmy | Brzegi Magmy | zastygające mosty |
| 078 | Kaldera | Kaldera | czysty ogień |
| 079 | Smocze Cmentarzysko | Kości Smoków | kostne smoki |
| 080 | Gniazdo Pierwszego Smoka | Krater Korony | boss aktu |

### Akt 9 — Podniebie, poziomy 81–90

| # | Loch | Strefa wyspy | Haczyk komórki |
|---|---|---|---|
| 081 | Łańcuchy Niebios | Kotwice Nieba | wejście na wyspy w chmurach |
| 082 | Rdzawe Chmury | Rdzawe Chmury | kwaśny deszcz, korozja |
| 083 | Obserwatorium Burz | Wieże Piorunów | pioruny kanałowane |
| 084 | Pryzmat Tęczy | Tęczowe Wodospady | rozszczepione światło |
| 085 | Wiatraki Wiecznego Wiatru | Młyny Chmur | mechanizmy na wietrze |
| 086 | Twierdza Gryfich Jeźdźców | Gniazda Gryfów | szarże z powietrza |
| 087 | Oko Tajfunu | Wirowe Mosty | wiatr spycha z krawędzi |
| 088 | Kowadło Gromu | Kowadła Chmur | burza jako okno |
| 089 | Zgliszcza Skrzydlatych | Upadłe Podniebne Miasto | skrzydlate widma |
| 090 | Tron Burzy | Szczyt Nieba | boss aktu |

### Akt 10 — Kres, poziomy 91–100

Kolumny Aktu 10 odbijają całe akty: loch 09k to kraina aktu k widziana przez Pustkę, z lekcją kolumny k.

| # | Loch | Strefa wyspy | Echo |
|---|---|---|---|
| 091 | Rozdarty Gaj | Skraj Kresu | Akt 1 |
| 092 | Toń bez Dna | Rozpadające się Wody | Akt 2 |
| 093 | Piaski Czasu | Klepsydrowe Wydmy | Akt 3 |
| 094 | Głębia bez Światła | Otchłań | Akt 4 |
| 095 | Wieczna Zamieć | Zamarznięty Czas | Akt 5 |
| 096 | Czarny Księżyc | Martwa Knieja | Akt 6 |
| 097 | Wojna, która nie minęła | Pole Wiecznej Bitwy | Akt 7 |
| 098 | Ostatni Popiół | Wygasła Korona | Akt 8 |
| 099 | Rozbite Niebo | Spadające Wyspy | Akt 9 |
| 100 | Serce Świata | Serce Kresu | finał gry |

## 5. Szkielet wyspy (ten sam w każdym akcie)

- **10 stref** w parach z lochami + **osada/port** (handlarze, profesje, tablica zleceń) + **Serce Wyspy**
  (boss świata na zegarze, para z lochem K10).
- **Transport sygnaturowy** aktu (tabela §3) i **Chowańce endemiczne** — tylko na tej wyspie.
- **Wydarzenia na zegarze** w strefach (elity, obozy, najazdy) zamiast bossów z fazami.
- Technicznie: jeden serwer wysp za proxy (nie 10), wyspy jako osobne światy pieczone offline z twardą granicą;
  warunek konieczny: wspólna baza postępu MMOCore/MMOItems/MMOProfiles (`akt1-wyspa-i-lochy.md` §6).

## 6. Łup: ten sam, ale nie ten sam

- Tier łupu = akt. Komórka ma **rodzinę lochową** (`L0NN_*`) i **rodzinę wyspiarską** (`W0NN_*`) sprzętu:
  ten sam tier i siła, inne przedmioty, wygląd i afiksy.
- Materiały profesji: wspólne rodziny w komórce (§2), rzadkie materiały bossa tylko z lochu, rzadkie materiały elit
  tylko z wyspy.
- Żetony: lochowe (z zabezpieczeniem przed pechem) i wyspiarskie (reputacja strefy) — wymiana na te same kategorie.

## 7. Jak to budujemy

- **Jeden loch naraz, dopieszczony** (zasada właściciela) — kolejność w obrębie aktu 001 → 010, wyspa aktu budowana
  strefami równolegle, gdy powstanie serwer wysp.
- **Zestaw lochu** (powtarzalny tor z Lochu 001 i 002): generator terenu w kuźni → budowle → mapa MD z zamrożonym
  terenem → moby i skille MM (wygląd zastępczy, potem modele) → sekrety i zależności → nagrody i EXP → wejście z menu Q
  → sonda botem bez OP → gra właściciela.
- **Lista kontrolna z krytyki 001** obowiązuje każdy loch od pierwszej wersji: wejście z menu, prawdziwe wskazówki,
  zero spamu czatu w walce, nagroda + EXP dla całej drużyny, boss nie do bezkarnego kitowania, coś poza walką
  (szukanie, wybór, zależność), punkt kontrolny przed bossem, zwarta mapa.
- **Otwarte:** (1) akceptacja siatki i praw aktów, (2) nazwy (robocze), (3) serwer wysp i wspólna baza — osobny projekt.
