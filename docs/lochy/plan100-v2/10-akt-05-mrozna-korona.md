# Akt 5 — Mroźna Korona (poziomy 41–50) — lista lochów (Projektant C, etap 3)

Wiąże: `05-synteza-v2.md` R1–R4. Pełny motyw mrozu i lawiny należy tylko tu (budżet obietnic R3).

## Nagłówek
- **Kraina**: lodowe płaskowyże, kurhany, zamarznięte jezioro, lodospady i polarna noc z jednym świtem.
- **Prawo**: **Co stoi, zamarza** — kto (gracz, wróg, Cień) stoi 3 s bez ruchu, pokrywa się lodem (spowolnienie,
  potem blokada 1 s); zamarznięty wróg pęka od mocnego ciosu. Czasownik aktu: RUSZAM SIĘ / zamrażam wroga.
- **Antagonistka**: **Sigrun, Matka Kurhanów**. Zamraża zmarłych, żeby nie wracali jako Cienie („pokój to bezruch”).
  **Jej racja**: zamrożeni NAPRAWDĘ nie wracają jako Cienie, a jej ludzie widzieli powroty bliskich, które
  ich nie poznawały. Cena: zamraża też żywych chorych, „zanim będzie za późno”. (Konflikt Aktu 8 jest inny —
  ciśnienie wulkanu, nie zmarli.)
- **Emocja**: żałoba i cisza → gniew → ulga.
- **Forma wyspy**: jedna wielka tafla jeziora z osadami. Pęknięcia otwierają się codziennie w innych miejscach
  (sanie-łodzie po lodzie).
- **Rodziny wrogów**: zwierzęta mrozu (rosomaki, mamuty, lamparty morskie, narwale, harpie szronu), lodowe konstrukty
  Sigrun (strażnicy kurhanów, posągi), istoty zamieci (szreniaki). Ludzie (zeloci Rady) **tylko w 046**; ludzcy bossowie: Hallvard (046) i Sigrun (050). Straż Rady w 049 to lodowe konstrukty.
- **Cienie w tym akcie**: Cień podlega prawu jak każdy — po 3 s bezruchu zamarza. AI Cienia w tym akcie celowo
  przystaje przy graczu, żeby zwabić go do bezruchu; zamarznięty Cień pęka od mocnego ciosu (okno). Bez wyjątków od
  prawa.
- **Nazewnictwo (R1)**: zmarli zamrożeni przez Sigrun to **Zmarzłe Widma** (lokalne zjawisko kurhanów). Nie są Echami
  (przewodnikami z R1.2) ani Cieniami i nie walczą po stronie gracza (Wezwanie startuje w Akcie 6).

## Lochy
| nr | poz. | nazwa | czasownik gracza | jak używa prawa + ZWROT | boss + haczyk (unikalny w grze) | kadr WOW (typ) | lokalna odmiana prawa | topologia |
|---|---|---|---|---|---|---|---|---|
| 041 | 41 | **Przystań na Krze** | SKACZĘ PO KRACH | lekcja: kra, na której stoisz 3 s, zamarza ciebie i tonie; ZWROT: zamrożony wróg na krze to dodatkowy ciężar, który ją topi — i jego, i ciebie | **Lampart Morski**: wynurza się przy krze, na której ktoś stoi najdłużej, i ściąga ją pod wodę; kry to jego arena | pierwszy świt po polarnej nocy na 2 s zalewa krę różem (świt) | bezruch = zamarznięcie + tonięcie kry | dryfujące kry |
| 042 | 42 | **Kurhan Pierwszej Zimy** | KRUSZĘ | zamrażasz wrogów (zmuszasz do bezruchu) i rozbijasz mocnym ciosem; ZWROT: w ścianach kurhanu są Zmarzłe Widma; mocny cios obok nich kruszy lód i widmo odchodzi w zorzę (ściana znika, odsłaniając przejście albo zasadzkę) | **Strażnik Kurhanu**: jego lodowa zbroja odrasta warstwami; warstwa pęka tylko od mocnego ciosu (dowolny mocny atak, także zaklęcie ładowane), a lekkie ciosy DOKŁADAJĄ lodu | lód pod stopami jest przezroczysty: 40 bl. niżej zamarznięta rzeka, ławica ryb w pół skoku i gruba żyła korzenia, która pulsuje światłem ku północy (głębia z istotami; fakt Fali) | lekkie ciosy zamrażają, mocne kruszą | spirala w dół |
| 043 | 43 | **Sanna Droga** | PĘDZĘ SANIAMI | sanie (vanilla łódź na lodzie) nigdy nie stoją, więc nie zamarzasz; walka z pędzących sań; ZWROT: żeby zabrać ocalałego, trzeba zatrzymać sanie na 3 s | **Mamut Zamieci**: biegnie obok sań; trafiasz go tylko w pełnym pędzie, a gdy zwolnisz, wyprzedza cię i depcze tor przed tobą | stado mamutów biegnie obok sań w śnieżycy (pęd stada) | pęd chroni przed lodem | tor-pętla po jeziorze |
| 044 | 44 | **Lodospad** | WSPINAM SIĘ | ruch w górę stopni z sopli; stanie na stopniu 3 s = przymarznięcie i spadające sople; ZWROT: środek lodospadu odmarza i woda niesie cię w dół | **Szronowa Harpia**: ściąga wspinaczy, ale sama musi się czegoś trzymać; gdy przysiądzie na 3 s, przymarza i spada razem z soplem (okno) | światło przenika 60 bl. lodu od środka (pionowe światło) | bezruch przymraża do ściany | komin pionowy w górę |
| 045 | 45 | **Przełęcz Lawin** | ZRZUCAM LAWINĘ | lawina, którą zrzucisz, zasypuje stojących wrogów (zasypany = nieruchomy = zamarza); ZWROT: lawina zamyka drogę powrotną i trzeba się przekopać w ruchu | **Yeti Przełęczy** (`lr_yeti`): wygrzebuje się z każdej lawiny tam, gdzie leży najwięcej śniegu — lawinami rzeźbisz miejsce, w którym wyjdzie | lawina przetacza się nad schronem pod nawisem: ciemność, potem przebicie światła (kataklizm nad głową) | zasypanie = wymuszony bezruch | przełęcz-wąwóz |
| 046 | 46 | **Wieś Zamarłych** | ROZKOPUJĘ | zaspy rosną na wszystkim, co stoi; żywi mieszkańcy są zasypani w chatach, a ty rozkopujesz przejścia, zanim zasypie ciebie; ZWROT: zeloci zasypują ich z powrotem, więc trzeba wybrać, którą chatę rozkopać (wybór 1/3) | **Kapłan Bezruchu Hallvard** (człowiek): w jego kręgu nikt nie zamarza, ale czas jest zamrożony — ciosy wszystkich spadają z 2 s opóźnieniem | wieś zamarła w pół gestu, nad nią przechodzi stado lodowych reniferów, nie zauważając ludzi (zatrzymana scena + istoty) | bezruch = zasypanie | wieś-hub z placem |
| 047 | 47 | **Okręt w Lodzie** | ZAMYKAM PRZERĘBLE | polowanie na narwala pod lodem: narwal przebija lód i zostawia przeręble; zamykasz je, stojąc na nich 3 s (ryzyko zamarznięcia), aż zostanie jeden; ZWROT: przy wraku leży ranny kapitan Kompanii — scena, nie eskorta: zostawić go Sigrun do zamrożenia czy pozwolić umrzeć (wybór 2/3) | **Narwal Przerębla**: nie wróci pod wodę, póki jego przerębel nie zamarznie, a zamarza tylko pod kimś, kto stoi na nim 3 s — zamykając wszystkie poza jednym, wybierasz miejsce, gdzie wynurzy się po raz ostatni | okręt Kompanii z Aktu 2 uwięziony w lodzie, pod przezroczystą taflą przepływa narwal z rogiem dłuższym niż maszt (istota) | bezruch gracza zamraża przeręble | tafla z przeręblami wokół wraku |
| 048 | 48 | **Labirynt Matrony** | ZACIERAM ŚLADY | w świeżym śniegu labiryntu każdy krok zostawia trop, a Matrona i jej ogary idą DOKŁADNIE po twoich śladach; prawo działa normalnie: postój 2 s pozwala śniegowi przysypać trop, ale postój 3 s zamraża — ślady zacierasz krótkimi postojami, skokiem na lód (bez śladów) albo idąc tyłem po własnym tropie; ZWROT: w środku labiryntu śnieg przestaje padać i ślady zostają na zawsze | **Lodowa Matrona**: podąża krok w krok twoim tropem — prowadząc ją śladami przez pęknięty lód albo pod nawis, zastawiasz na nią pułapki własną trasą | z muru labiryntu widać w świeżym śniegu tylko dwie świecące linie śladów, twoją i jej, która dogania twoją (pościg w obrazie) | postój = śnieg przysypuje trop, 3 s = lód | labirynt w śniegu |
| 049 | 49 | **Twierdza Rady** | TARANUJĘ | taran na saniach trzeba pchać bez przerwy (stoi = zamarza i przymarza do ziemi); ZWROT: brama zamarza od środka — Rada zamroziła w niej własnych rannych | **Lodowy Jarl** (konstrukt Rady w zbroi z lodu): zbroja twardnieje, gdy on stoi, i mięknie, gdy biega — zmuszasz go do ruchu | sala tronowa z lodu, za tronem okno na zamarznięte morze, po którym idzie Mamut Zamieci (panorama z istotą) | bezruch utwardza zbroję wroga | koncentryczne mury |
| 050 | 50 | **Tron Bezruchu** | ZRYWAM ŁAŃCUCHY | egzamin: prawo bez zmian (3 s), presja z terenu: posągi Zmarzłych Widm to jedyne osłony przed lodowym podmuchem Sigrun, a każdy jest przykuty do jej Korony lodowym łańcuchem; ZWROT: zerwany łańcuch (mocny cios w zamarznięty łańcuch) osłabia Koronę, ale widmo odchodzi w zorzę razem z posągiem — tracisz osłonę (wybór 3/3: ilu odprowadzić) | **Sigrun, Matka Kurhanów** (człowiek): czerpie osłonę z przykutych widm — każdy zerwany łańcuch zdejmuje jej warstwę, a tobie zabiera kryjówkę | zorza układa się w koronę nad tronem, widma odchodzą w nią jedno po drugim, a jej ogon ciągnie się nad horyzontem na południe, ku srebrnym jeziorom (korona z zorzy; fakt skutku) | posągi = osłony i więzy jednocześnie | szczyt-stożek z tronem |

## Łuk aktu
- **041–044**: piękno i bezwzględność mrozu, nauka „ruszaj się, zamrażaj innych”. 042 pokazuje, że w lodzie są ludzie
  (pytanie, kim są).
- **045**: siła natury.
- **046**: pierwsza zbrodnia zelotów — żywi zamrożeni „na zapas”.
- **047**: link z Aktem 2 i osobista decyzja o jednym zmarłym (zamrozić czy pozwolić wrócić).
- **048**: Matrona, łowczyni Sigrun, ściga uciekinierów po śladach w śniegu — Rada nie pozwala nikomu odejść z Korony.
- **049**: Rada zamraża własnych rannych.
- **050**: Sigrun ma rację co do cierpienia powracających, ale jej sposób to więzienie. Gracz zrywa łańcuchy widm
  i pokonuje ją, nie zabijając (Korona pęka, Sigrun zostaje strażniczką kurhanów bez mocy). Kronika: zamrożeni pod Koroną nie śnili —
  „ktoś pod lasem prowadzi zmarłych światłem księżyca” (haczyk na Akt 6).
- Rozdroża Cieni: każdy loch (tabela niżej).

## Najsilniejsze / najsłabsze
- **Najsilniejsze**:
  - **048 Labirynt Matrony** (boss idzie twoimi śladami, a ty zacierasz je postojem tuż przed zamarznięciem).
  - **043 Sanna Droga** (pościg saniami ze stadem mamutów, tanio: vanilla łódź na lodzie).
  - **050 Tron** (dylemat: osłona czy wolność widm, w samej walce).
- **Najsłabsze**:
  - **048** po przebudowie opiera się na tropie w śniegu, a nie na spojrzeniu; ryzyko: czytelność śladów w MC (cząsteczki
    na bloku śniegu co krok, przysypywanie = zmiana warstwy `snow`). Plan B: ślady jako krótkotrwałe display śnieżnych
    odcisków (limit 40 na gracza).
  - **041 Przystań na Krze**: lekcja prawa, mniejszy cud niż reszta. Lampart musi mieć pokaz siły w pierwszej minucie
    (ściąga krę z graczem-NPC pod wodę).

Kontrola czasowników z innymi aktami: 048 ZACIERAM ŚLADY, 045 ZRZUCAM LAWINĘ (Akt 4 ma „wywołuję”). 050 zmienione na ZRYWAM ŁAŃCUCHY,
046 na ROZKOPUJĘ, 047 na ZAMYKAM PRZERĘBLE — żadne nie występuje w listach aktów 3, 4, 6, 7, 9, 10 (grep).

## Rozdroża Cieni (R1.5)
| loch | Rozdroże | elita zastępcza (fikcyjna) |
|---|---|---|
| 041 | największa kra | Lampart Młody |
| 042 | komora na 2. zakręcie spirali | Kurhanowy Strażnik Mniejszy |
| 043 | przystanek ocalałego na torze | Młody Mamut Zamieci |
| 044 | półka w połowie lodospadu | Harpia Młoda |
| 045 | schron pod nawisem | Szreniak (istota zamieci) |
| 046 | plac studni | Zelota-Starszy (człowiek, w limicie 046) |
| 047 | pokład wraku | Lodowy Rosomak |
| 048 | środek labiryntu przed Matroną | Ogar Matrony (lodowy konstrukt, idzie po śladach) |
| 049 | dziedziniec między murami | Konstrukt-Taranowy |
| 050 | stopnie pod tronem | Strażniczka Korony (konstrukt) |
Każdy loch jest kompletny bez Cienia.

## Odpowiedź na recenzję (etap 5)
| Zarzut | Źródło | Zmiana |
|---|---|---|
| Sprzeczność: Cień zamarza / nie zamarza | Y | Cień podlega prawu jak każdy; AI przystaje, żeby zwabić gracza |
| 050 skracał 3 s do 2 s | Y | prawo stałe 3 s, presja z terenu (posągi-osłony) |
| „Echa” jako dusze i sojusznicy (R1.2–R1.3) | Y | Zmarzłe Widma (lokalne), nie walczą po stronie gracza; brak sojuszników przed Aktem 6 |
| 046 rozgrzewanie ruchem = samo prawo | Y, P1.1 | rozkopywanie zasp i ratowanie zasypanych chat (ROZKOPUJĘ) |
| 047 eskorta rannego | Y, P1.4 | polowanie na narwala i zamykanie przerębli; ranny tylko jako scena-wybór przy wraku |
| 048 prawdziwe Cienie w ścianach bez zgody; blisko 004 | Y | postacie fikcyjne usunięte razem ze starym motywem; w rundzie 2 loch przebudowany na tropy (niżej) |
| Trzech ludzkich bossów | Y, P1.3 | 049 Lodowy Jarl to konstrukt; ludzie tylko w 046 i 050 |
| 050 „żegnam” nie jest fizyczne | Y, P1.2 | ZRYWAM ŁAŃCUCHY (mocny cios w zamarznięty łańcuch) |
| Sigrun = Kalda (ta sama racja) | Y | Kalda dostaje rację wulkaniczną (Akt 8); Sigrun zostaje przy zmarłych |
| Rozdroża tylko w 3 lochach | Y (R1.5) | tabela Rozdroży dla 041–050 |
| WOW z istotą, max 3 wybory (P5) | P5 | 042 ryby, 046 renifery, 047 narwal, 049 mamut; wybory 046, 047, 050 |
| 048 odwracało prawo bezruchu (runda 2) | Y | prawo działa normalnie; motyw lochu to tropy w śniegu (ZACIERAM ŚLADY), a Matrona idzie po śladach gracza |
| 043 elita Wilczyca = wilki Aktu 1 (runda 2) | Y | elita zastępcza 043: Młody Mamut Zamieci (rodzina Aktu 5) |
| Grep czasowników (runda 2) | — | „zacieram” nie występuje w 10-akt-02…10; ślady w Akcie 10 to tylko kadr, nie mechanika |

## Ślad aktu w lochach (runda 3)
- **Bodziec antagonistki (S1)**: Fala Powrotów z rany Korzenia (Akt 4) płynie korzeniem pod lodem i dociera tu
  najpierw; Sigrun zamraża zmarłych, zanim wrócą.
- **Trzy fakty**:
  - co uruchomiło → 042: pod przezroczystym lodem pulsuje żyła korzenia (w kadrze WOW wiersza 042);
  - kto zyskuje / cierpi → 046 Wieś Zamarłych (żywi zamrożeni „na zapas”), 049 Twierdza Rady (władza Rady);
  - skutek do Aktu 6 → 050: Zmarzłe Widma odchodzą w zorzę, której ogon ciągnie się na południe (kadr WOW 050).
- Wyspa: `22-wyspa-akt-05-mrozna-korona.md`.
