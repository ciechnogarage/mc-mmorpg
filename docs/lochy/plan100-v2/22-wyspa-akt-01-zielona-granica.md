# Wyspa Aktu 1 — Zielona Granica: „Żniwa Szczepu” (PILOT szkieletu wysp) — Projektant C, v2

Wiąże: `23-synteza-v3` S1–S6. Lochy Aktu 1 są ZAMROŻONE (`../akt1-v2/`): wyspa dopasowuje się do nich. Poziomy 1–10,
osobny serwer wysp za proxy, teren pieczony offline.

## 1. Wydarzenie aktu (S1, zgodne z 005/008/010)
- **Przyczyna**: w Głodnym Roku Darnik zaszczepił na starej gruszy osady (Matecznik, 010) szczepkę Pierwszego Korzenia,
  „żeby nikt nie był głodny” (005). Skąd ją miał — Akt 1 nie rozstrzyga (S1a); jedyny ślad to pieczęć w rejestrze
  przystani (niżej).
- **Co się dzieje**: Szczep wyżywił Granicę, ale nie przestaje rosnąć. Na wyspie zarasta POLA: w zbożu pojawiają się
  **Zawiązki Szczepu** (zielone kłosy, które dojrzewają falami) i zamieniają plon w zarośla.
- **Skutek**: Bractwo nie tylko szczepiło na miejscu — **wywoziło** sadzonki. Skrzynie z Pasieki szły rzeką do morza
  (002: Wela „ktoś je stąd wywiózł”). Wyspa pokazuje SZLAK wywozu: rejestr przystani rzecznej, kursy barek w dół rzeki,
  ku morzu. KTO je odbierał, ustala dopiero Akt 2 (Kompania, we własnych scenach).

## 2. Trzy fakty obowiązkowe (nośniki w lochach = karty bez zmian)
| Fakt | Lochy | Wyspa |
|---|---|---|
| **Co uruchomiło**: Darnik zaszczepił gruszę osady sadzonką Pierwszego Korzenia, by nikt nie głodował | 005 dziennik („Zaszczepię ziemię jeszcze raz…”), 010 grób Dary i grusza | Kamień Graniczny na Pastwiskach: wyryte daty szczepień od „gruszy przy dzwonnicy”, kontrakt „Kamień Graniczny Darnika” |
| **Kto zyskuje / cierpi**: Stolica je chleb Szczepu, a wsie płacą zarastaniem i wyborem | 008 list przewozowy i bochen, 009 wieś, która wybrała Szczep | wydarzenie „Żniwa Szczepu”: plon jest naprawdę potrzebny osadzie, a niezebrany zamienia się w zarośl |
| **Skutek do następnej krainy**: sadzonki wywożono rzeką ku morzu | 002 barka i Pieczęć Pasieki na skrzyniach („ktoś je stąd wywiózł”) | Przystań Rzeczna w Trzech Młynach: rejestr kursów barek „w dół, do ujścia”; na pierwszej stronie, z Głodnego Roku, wpis „przyjęto szczepkę” z obcą pieczęcią (fala i mgła — Kompania, nienazwana w Akcie 1) |
Nośnik faktu 3 to **rejestr i barki**, a nie „skrzynia z pieczęcią” (ten rekwizyt zostaje w 002 i w Akcie 2).

## 3. Mapa
**Krajobrazy (4)**: **Łany Doliny** (pola, miedze, młyny; strefa wydarzenia) · **Sady Pod Wzgórzem** (kwitnące,
przerośnięte) · **Mokradła Rechotu** (trzciny, groble, ropuchy) · **Wrzosowe Pastwiska** (murki, owce, Kamień Graniczny).

**Osady**: **Trzy Młyny** (główna): tablica Żniw, piekarz, profesje, przystań rzeczna z rejestrem. · **Pasieka Wdowy Marty** (usługa): miody, kwiaty do oswajania. · **Zagroda Bliźniaków** (SPÓR): palić zarośnięte pole czy zbierać?

**Transport**: wozy na stałej trasie między młynami (szybka podróż z animacją) i tyrolki z wieżyczek sadów (`zipline`
LR). Barki rzeczne tylko w dół, jako skrót do Mokradeł.

**Jaskinie (5)**: Piwniczka Pod Młynem · Kretowisko Szczepu (korzenie łączą pola pod ziemią) · Ul w Spróchniałym Dębie ·
Studnia pod Wierzbą (woda, w której nic nie rośnie) · Kurhan Sadowników („pilnować granicy”).

## 4. Kontrakty (8 z 20; stan miejsca + 2 sposoby; czysto bojowe 2)
| Kontrakt | Stan miejsca | Sposób A | Sposób B |
|---|---|---|---|
| **Snopy przed deszczem** | snopy na polu, Zawiązki w nich dojrzeją | zwieźć wozem do stodoły | wyciąć Zawiązki na miejscu sierpem (wolniej, plon mniejszy) |
| **Zarośnięta Miedza** | zarośl zamknęła drogę między polami | wyrąbać węzły zarośli | zwabić Korzeniaka: jego pąk po śmierci daje Korzeń, który rozsadza zarośl (prawo pąka) |
| **Spór Bliźniaków** | bracia nie zbiorą pola bez zgody | przynieść dowód z Kamienia Granicznego | pomóc wypalić pole (szybciej; osada ma mniej chleba przez 1 h) |
| **Pyłek w Ulach** | pszczoły niosą pyłek Szczepu | przenieść ule poza sady tyrolką | okadzić ule ziołami z Mokradeł |
| **Kamień Graniczny Darnika** | kamień porósł kwiatami, napis nieczytelny | oczyścić go | obmyć wodą ze Studni pod Wierzbą |
| **Rejestr Przystani** | rejestr barek zamókł, przewoźnik nie pamięta kursów | wyłowić strony z Mokradeł w odpływie rzeki (płycizna) | odtworzyć kursy z oznaczeń na palach przystani |
| **Wilczyca Wrzosowisk** *(bojowe)* | wilczyca atakuje owce | pokonać ją | — |
| **Pniaczkowa Noc** *(bojowe)* | po zmroku w sadach rośnie fala Pniaczków z pąków poległych | odeprzeć falę przy dzwonie | — |

## 5. Wydarzenie świata: **Żniwa Szczepu** (Łany, cykl 25 min)
- **Zapowiedź (5 min)**: dzwon Trzech Młynów; Zawiązki w zbożu świecą zielenią (inna cząsteczka i dźwięk niż pąk).
- **Kryzys (8 min)**: Zawiązki dojrzewają falami co 30 s. Gracze koszą je (dowolne trafienie), wożą snopy do młyna
  i bronią kosiarzy-NPC stojących w miejscu (bez eskorty). Niezebrany Zawiązek zarasta pas pola zaroślą.
- **Skutek (1–3 h)**:
  - **sukces** (≥70%): młyn mieli, tańsze racje, szybka trasa przez Łany;
  - **porażka**: pasy zarośli, kontrakty „Zarośnięta Miedza”, dłuższa droga.
- **Wkład niebojowy**: koszenie, wożenie, zwiad (Zawiązki w miedzach). **Solo**: mniej pasów, ten sam próg w %.

**Meta-łańcuch → boss świata** (co ~2 h): **Żniwa** + **Rój Pyłku** (Sady) + **Powódź Korzeni** (Mokradła: zamknąć
groble). Potem budzi się:
- **Dąb Graniczny** — **odnoga gruszy Matecznika**: korzeń z Matecznika przeszedł pod polami i wyrósł przy Kamieniu Granicznym. Nie jest „pierwszym drzewem” (pierwsza jest grusza z 010), tylko jego najdalszą odnogą na wyspie. **Haczyk (nowy, bez dzielenia drużyny): Dąb PRZESUWA GRANICĘ.** Co 30 s jego korzenie wyrywają słupki miedzy i przenoszą je o kilka bloków do środka. Arena — pole otoczone płotem i miedzami — kurczy się, a za nową granicą rośnie zarośl. Gracz wbija słupki z powrotem przy znakach miedzy (dowolny cios w słupek z zapowiedzią); każda przywrócona miedza odcina Dębowi jeden korzeń i zdejmuje warstwę kory. **Fazy**: Granica (korzenie przesuwają słupki) → Kwitnienie (gałęzie zrzucają Zawiązki na arenę; trzeba je ściąć, zanim zarosną pole) → Przycięcie (korona opada, pień zostaje przy kamieniu jako znak granicy). Skutek antagonisty: Szczep Darnika w otwartym świecie.
- **Solo, przyzywalny** po 3 dowodach (odczytany Kamień Graniczny, Kłos ze Żniw, Miód Szczepu). Ta sama mechanika granic
  na mniejszym polu z 2 niezależnymi miedzami, które jeden gracz obsługuje na przemian w trakcie walki.

## 6. Prawo w otwartym świecie (zgodne z R1 Aktu 1)
- **Pąk (prawo)**: tylko po POLEGŁYM, identyczny jak w lochach, 5 s. Na wyspie ma jeden skutek — **Pniaczek**. Limit 24 na
  strefę (≈ 300×300 bl.), najstarszy usycha; budżet: ≤6 Pniaczków aktywnych na gracza, sprawdzane co 1 s.
- **Zawiązki Szczepu** to NIE pąki: rośliny w polach wydarzenia, inna cząsteczka i dźwięk, bez związku ze śmiercią.
- **Bezpieczna trasa**: brukowane drogi między młynami (pąki na bruku nie dojrzewają).
- **Echo prawa (S5)**: brak (pierwszy akt).
- **Test**: wylogowanie w polu w czasie fali → powrót bez Pniaczków przy graczu.

## 7. Surowce i Chowańce
- **Kłos Zawiązkowy**: ścięty w ostatniej sekundzie świecenia Zawiązka (timing).
- **Miód Szczepu**: z ula przy kwitnącym Zawiązku w Pasiece.
- **Chowaniec — Jeż Sadowy**: zjada Zawiązki; oswajasz go, zostawiając mu 3 nieścięte i stojąc obok bez ataku.
- **Chowaniec — Polna Przepiórka** (rzadka): tylko przy świeżo skoszonym pasie w Żniwach.

## 8. Finał wyspy vs lochów
- Lochy: **Darnik** przy gruszy Matecznika (010) — człowiek, wybór, przycięcie.
- Wyspa: **Dąb Graniczny** — odnoga tej samej gruszy, która przesuwa granice pól; społeczność i gracz przywracają miedze.
- Wspólny wniosek: Szczep przycina się, a nie ścina. Wspólny haczyk: sadzonki odpływały rzeką ku morzu (002 / Przystań).

## 9. Równość ścieżek (liczby z `../akt1-v2/07` L3) i czas
**Budżet aktu**: 9000 EXP (poz. 1→10). **Lochy**: pierwsze przejścia 001–010 = 0,6 × 200 × (1+…+10) = **6600 EXP**,
reszta (2400) z powtórek (0,25 × 200·N), ocen i zleceń.

**Wyspa — pierwsze przejście ścieżki krytycznej = 6600 EXP**, poziomy kontraktów 1–10 rosnąco:
Źródła: 12 kontraktów krytycznych 2600 · 3 udziały w wydarzeniach (Żniwa, Rój, Powódź; raz każde z pełną wartością) 1200 · 5 jaskiń 800 · 3 fakty i Kronika 800 · Dąb Graniczny (pierwsze pokonanie) 1200.
Resztę (2400) dają powtarzalne wydarzenia (0,25 wartości) i kontrakty dzienne.

**Łup**: Rodzina wyspiarska `W01_*` (Sierp Graniczny, Płaszcz Żniwiarza, Laska z Wierzby…) w tym samym tierze co `L001–L010`. · **Żeton Granicy** (wyspa) i Żeton Sadu (lochy) wymienne u tego samego handlarza na te same kategorie. · Tylko wyspa: Chowańce endemiczne. Tylko lochy: Pieczęć Lochowa. · **Premia domknięcia aktu** (wspólny atomowy ledger): raz na postać, 600 EXP + tytuł „Przycinacz Granicy”, za pierwszy finał dowolnej ścieżki. Drugi finał: własny łup finałowy, kosmetyk i wpis Kroniki, bez premii EXP.

**Ścieżka krytyczna solo = 205 min (3 h 25 min)**: Trzy Młyny i 2 kontrakty 30 → pierwsze Żniwa 25 → Kretowisko 15 →
Sady (Rój, Pasieka, Ul) 40 → Mokradła (groble, Powódź, Rejestr) 40 → Pastwiska (Kamień, Wilczyca, Kurhan) 35 → Dąb 20.
Pełne odkrywanie 6–7 h. **Pilot bez wspólnej bazy postępu = prototyp treści bez łupu i EXP**, nie druga ścieżka.

**Ryzyka i plan B**: (a) wspólna baza postępu — warunek startu (S3); · (b) pąki w świecie — budżet wyżej, pomiar spark przy 20 graczach; plan B: pąki tylko w strefach wydarzeń; · (c) przesuwanie słupków — słupki jako znaczniki z 3 stałymi pozycjami (stany bloków), nie fizyka; · (d) wozy — stała trasa szybkiej podróży z animacją.

## Odpowiedź na recenzję (runda 3)
| Zarzut Y / samokrytyka | Zmiana |
|---|---|
| Statek jako źródło szczepu sprzeczny z 010 (grusza, Pierwszy Korzeń) | przyczyna = grusza i sadzonka Pierwszego Korzenia (005/010); statek tylko jako **wywóz** rzeką ku morzu (002) |
| „Pieczęć portu Kompanii” bez nośnika w 002 | usunięta; fakt 3 = szlak wywozu (rejestr przystani, barki); Kompanię ujawnia Akt 2 |
| Dwa „pierwsze” drzewa | Dąb = najdalsza odnoga gruszy Matecznika |
| Pąki w kłosach łamią R1 | Zawiązki Szczepu (rośliny, inna cząsteczka) ≠ pąk po poległym; pąk na wyspie ma 1 skutek (Pniaczek) i budżet |
| Finał solo traci haczyk | haczyk przesuwania granic zostaje w solo (2 miedze obsługiwane na przemian) |
| Brak równości nagród | tabela EXP (6600 = suma pierwszych przejść 001–010), łup W01 i żetony, premia raz w ledgerze |
| Samokrytyka: boss dzieli drużynę; „skrzynia z pieczęcią” jako nośnik | Dąb ma nowy haczyk (przesuwa granice), nośnik faktu 3 to rejestr i barki |
| Skąd szczepka (runda 2 Y, S1a) | ślad bez rozwiązania: pierwszy wpis rejestru przystani z Głodnego Roku z obcą pieczęcią (Kompania, nienazwana); sadzonki płyną do ujścia |
