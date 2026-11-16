# Plan 100 lochów v2 — propozycja Projektanta C (2026-10-09)

Kanon: Akt 1 v2 zamrożony (`../akt1-v2/`), zasady `../akt1-v2/09-korekta-planu-100.md`, panteon `../../wiary/panteon.md`.
Pracowałem niezależnie (bez 01-X/Y). Źródła zewnętrzne podaję przy tezie.

## A. MOCNA RZECZ

### A1. Cienie Poległych (FAWORYTKA)
**Zdanie dla znajomego:** „Na tym serwerze, jak zginiesz w lochu, twój Cień — z twoją skórką, imieniem i klasą — zostaje
tam i poluje na innych; a kto go pokona, oddaje ci to, co straciłeś.”

**Jak działa:**
- Śmierć w lochu zapisuje migawkę postaci: skórka, imię, archetyp i klasa, poziom, 3 główne zdolności, typ broni, tier
  zbroi, miejsce i przyczyna śmierci.
- Utracone 10% EXP (obecna kara za śmierć, pamięć kara-smierci) **nie przepada, tylko siedzi w Cieniu**.
- W przebiegach INNYCH graczy przez ten loch (to samo pasmo poziomów) każdy loch ma 1–2 **Rozdroża Cieni**: miejsce, gdzie
  zamiast elity może pojawić się Cień prawdziwego gracza. Pierwszeństwo mają znajomi i gildia, potem świeże zgony,
  potem Cienie „sławne”.
- Pokonanie Cienia: zabójca dostaje Odłamek Cienia (waluta kosmetyków i trofeów), a właściciel powiadomienie
  „Kuba ukoił twój Cień na Bagnach” i zwrot utraconego EXP. Nikt go nie pokona w 7 dni → wraca 50% EXP.
- **Cień jest ze świata, nie z menu**: wynika z prawa każdego aktu (tabela §C). Na Wyspie nikt nie umiera do końca
  — to tłumaczy także zwykły respawn (gracze to „Powracający”) i jest osią fabuły 1→100 (§B).
- **Warstwa legend**: Cień, który pokonał najwięcej graczy w tygodniu, staje w Stolicy jako posąg (vanilla mannequin
  ze skórką, 1.21.9+), a Kronika Cieni podaje jego „rekord”. Pierwsza drużyna, która przejdzie finał aktu, zostaje na
  stałe Strażnikami Bramy następnego aktu (ich Cienie egzaminują późniejszych).

**Research — co było i co jest nowe:**
- **Nioh (Team Ninja)**: Bloody Graves; wrogi duch innego gracza z jego buildem, łup z jego ekwipunku. To najbliższy
  wzór, ale gra jest single/koop, bez świata i bez zwrotu straty właścicielowi
  ([Nioh wiki: Revenant](https://nioh.fandom.com/wiki/Revenant),
  [Fextralife: Bloody Grave](https://nioh.wiki.fextralife.com/Bloody_Grave)).
- **Forza Drivatar**: AI z danych o jeździe gracza w cudzych wyścigach; wyścigi, nie walka
  ([Xbox Wire](https://news.xbox.com/en-us/2014/09/30/games-forza-horizon-2-drivatars/)).
- **Dark Souls**: plamy krwi i fantomy to ślady, nie przeciwnicy z twoim buildem.
- **Nemesis (Shadow of Mordor)**: pamięć wrogów, ale NPC (nie gracze) i **patent WB US 10,926,179 (2021)**
  ([Game Developer](https://www.gamedeveloper.com/business/-i-shadow-of-war-i-publisher-warner-bros-has-patented-the-series-nemesis-system),
  [Patent Arcade](https://www.patentarcade.com/2021/02/warner-brothers-granted-patent-for-nemesis-system-from-middle-earth-video-games.html)).
  Nie kopiujemy hierarchii NPC, awansów ani zemsty NPC; nasz Cień to migawka gracza. Przed startem potrzebna i tak
  opinia prawna.
- **Na MC**: nie znalazłem serwera RPG z asynchronicznymi Cieniami graczy jako wrogami (wyszukiwanie zwraca tylko
  pluginy „duch jako kara”, np. [DeathPenalty](https://www.curseforge.com/minecraft/bukkit-plugins/deathpenalty)).
- **Nowość u nas**: (1) MMO zamiast single, (2) Cień + zwrot straty właścicielowi (pomoc zamiast łupu), (3) Cień jako
  prawo świata zmieniające się co akt, (4) posągi i Strażnicy Bram.

**Kręgosłup 100 lochów:**
- Każdy loch ma Rozdroże Cieni w beat charcie (wymiana slotu elity).
- Każdy akt zmienia, JAK Cień się pojawia i jak go pokonać (prawo aktu, §C), więc to nie dodatek, tylko 10 wariantów
  jednej idei.
- Akt 10 (Kres) to źródło Echa: finał gry to marsz wszystkich Cieni serwera.

**Wykonalność i koszt:**
- Plugin „Cienie”: zapis migawki przy śmierci w instancji MD (ok. 1 KB, MySQL/SQLite).
- Spawn: niewidzialny nośnik MM z AI archetypu (4 szablony skilli MM × poziom) + pasażer-mannequin ze skórką. Bez
  teleportu co tick (pamięć optymalizacja-walk-chowancow) — pasażer jedzie z nośnikiem.
- 1 Cień na instancję naraz, więc koszt jak elita. Koszt budowy: ~2–3 tyg. pluginu + 4 szablony AI + UI Kroniki.
- Plan B wyglądu: model ModelEngine z teksturą skórki albo zwykły mannequin bez animacji ataku (ataki czytelne
  cząsteczkami).

**Co może pójść źle i zabezpieczenia:**
(a) Celowe ginięcie, by kumpel farmił Cienie: nagroda tylko raz na Cień na gracza, tygodniowy limit Odłamków, Cień słabszy niż loch nie daje nic. (b) Toksyczne nicki i skórki: filtr, zgłoszenie, opcja „ukryj moje imię”, ban = brak Cieni. (c) Zbyt trudne Cienie topów: siła normalizowana do poziomu lochu, a nie do ekwipunku właściciela. (d) Pusty serwer: rezerwa Cieni „deweloperskich” (jak w Nioh). (e) Wydajność: limit 1/instancję, zapis asynchroniczny.

**Test 3 pytań:**
- Zmienia decyzje? Tak: śmierć ma społeczny skutek (twój Cień nęka innych), a pomoc innym zwraca ci EXP.
- Historie? Tak: „ubiłem Cień Kuby na Bagnach”, „mój Cień jest posągiem w Stolicy”.
- Powód wracać? Tak: Kronika i rekordy Cieni, posągi tygodnia, Strażnicy Bram nowego aktu.

### A2. Kronika Serwera — historia pisana wyborami wszystkich
- **Zdanie**: „Na tym serwerze finał każdego aktu decyduje, jak wygląda świat dalej, a liczy się to, co zrobiła większość.”
- Wzory: WoW Ahn’Qiraj War Effort 2006
  ([Wowpedia](https://wowpedia.fandom.com/wiki/Gates_of_Ahn'Qiraj)), wybory burmistrzów co ~5 dni w Hypixel SkyBlock
  ([wiki](https://hypixel-skyblock.fandom.com/wiki/Mayor_Election)). Nowość jest tylko w połączeniu z fabułą
  i wyborem moralnym w lochu (jak Darnik w 010).
- Kręgosłup: 10 wyborów finałowych zmienia Stolicę i otwarcie następnego aktu.
- Koszt: 2 warianty pierwszego lochu każdego aktu (×10).
- Ryzyko: mniejszość czuje się przegrana; świat „zamknięty” dla nowych graczy.
- Test 3 pytań: decyzje tak, historie tak, powroty średnio.

### A3. Żywy Sad Gildii
- **Zdanie**: „Na tym serwerze gildia hoduje własny loch z łupu i sadzonek, a inne gildie go najeżdżają, kiedy śpimy.”
- Wzory: Dungeon Keeper, Clash of Clans, rajdy w Factions (PvP), WoW Garrisons — było, ale nie jako PvE-loch w MMO na MC.
- Koszt i ryzyko wysokie: edytor, moderacja, exploity pułapek.
- Test 3 pytań: tak / tak / tak, ale budowa to osobny projekt na rok.

**Faworytka A1**, bo jako jedyna jest jednocześnie: nowa w MMO na MC, tania w przeliczeniu na 100 lochów (jeden plugin,
dalej to slot w beat charcie), fabularna (tłumaczy respawn i prowadzi do finału) i społeczna bez PvP. A2 proponuję jako
lekką warstwę (tylko finały aktów 7 i 10), A3 odrzucam na teraz (koszt).

## B. ŁUK GRY 1→100
**Pytanie całej gry: „Dlaczego na Wyspie nic nie umiera do końca?”** Każdy akt pokazuje inny sposób, w jaki świat
odmawia końca. Każdy antagonista ma własną odpowiedź na to, co z tym zrobić.
- **Akt 1**: Szczep żywi się tym, co pada — śmierć staje się wzrostem. Darnik szczepił z miłości (Dara). Sadzonka
  przypłynęła statkiem.
- **Akt 2**: Archipelag. Kompania Słonej Mgły handluje sadzonkami Szczepu po świecie; przypływ zwraca utopionych.
  Gracz dowiaduje się, że Szczep to szczepka Pierwszego Korzenia, wykopana gdzieś „pod piaskiem”.
- **Akt 3**: pod wydmami jest Miasto Pierwszych Powracających — pierwszy ślad Echa sprzed wieków. Kartograf chce je
  odkopać.
- **Akt 4**: Głębia to Korzenie Świata. Pierwszy Korzeń naprawdę istnieje i karmi się zmarłymi, ale też żywi całą
  Wyspę. Ktoś chce go ściąć (lustro Aktu 1: przyciąć czy ściąć?).
- **Akt 5**: Mroźna Korona zamraża zmarłych, by nie wrócili. Pierwsza odpowiedź „przeciw Echu”: bezruch.
- **Akt 6**: Srebrna Knieja — elfy prowadzą cienie przodków światłem księżyca. Druga odpowiedź: pamięć ponad świt.
- **Akt 7**: Ziemie Paktu. Nieumarli SĄ Powracającymi, więc spór Przymierza i Paktu o Echo wybucha wojną. Wrogami są
  ekstremiści obu stron, nie rasy. Gracz każdej rasy widzi, że obie strony mają rację w połowie.
- **Akt 8**: Popielna Korona — zakon chce zasypać świat popiołem, bo popiół pieczętuje groby. Trzecia odpowiedź:
  zagłada.
- **Akt 9**: Podniebie — pusty Trybunał Vharuna. Bóg sądu milczy; samozwańczy Sędzia sądzi Powracających. Wiary
  wchodzą tu pełną fabułą.
- **Akt 10**: Kres — źródło Echa. Tkacz Echa zerwał sąd Vharuna, żeby „nikt nie został zapomniany”. Finał jest echem
  Aktu 1: Echo trzeba **przyciąć, nie ściąć** — umarli mogą odejść, pamięć zostaje.

**Co łączy akty:** jedna oś (Echo), jedna rodzina „odpowiedzi” (miłość, handel, wiedza, ścięcie, bezruch, pamięć,
wojna, zagłada, sąd, pamięć absolutna). Każdy finał aktu kończy się sceną z Cieniami. Mapa Granicy rośnie w Atlas Wyspy.
**Wzrost złożoności:**
- 1–3: jedno prawo, starcia 1–2 typów.
- 4–6: prawo + pierwsza „interakcja praw” (echo prawa poprzedniego aktu w 1–2 lochach).
- 7–9: prawo + frakcje lub teren + bossowie z 3 fazami i warunkami drużynowymi na Trudnym.
- 10: echa wszystkich praw. Spec od 10, więc Trudny od Aktu 3 może wymagać ról (opcjonalnie).

**Frakcje i wiary:**
- Akty 1–5 bez wiązania z wiarami (aluzje).
- Akt 6: Aelyra, aluzja silna.
- Akt 7: Przymierze i Pakt pełną fabułą, oba z ekstremistami.
- Akt 9: Vharun.
- Akt 10: Trzej Starsi (Ordhun = Pierwszy Korzeń, Nysara = Tkaczka, Vharun = sąd).

**Jak mocna rzecz zmienia łuk:** Cienie są dowodem na Echo, które gracz widzi od poziomu 1 — każda śmierć jest częścią
fabuły. Gracze-Powracający są stroną sporu, a Akt 10 zamienia rekordy Cieni w armię finału: gracz walczy z Cieniami
własnej społeczności, a „przycięcie” Echa to pożegnanie z nimi (posągi zostają).

## C. 10 AKTÓW
Format: kraina · PRAWO · zagrożenie/antagonista · emocja · forma wyspy · 3 lochy (nazwa — czasownik — haczyk bossa)
· finał · Cienie w tym akcie.

**Akt 1 — Zielona Granica (1–10).** Kanon `../akt1-v2/`. Prawo: „Co tu pada — kiełkuje.” Cienie: pąk poległego gracza
dojrzewa w **Cień Zaszczepiony** w cudzych przebiegach (rozwinięcie dzisiejszego Ciernia).

**Akt 2 — Archipelag Słonych Mgieł (11–20).**
- Kraina: wysepki, porty, mgła, latarnie i kompanie handlowe; jedyne MORZE gry.
- PRAWO: **„Przypływ”** — co ~3 min woda podnosi się o 3 bl. i opada; areny zmieniają kształt, a to, co zatonęło
  w przypływie, wraca z odpływem.
- Antagonistka: **Ysolde Kran**, armatorka Kompanii Słonej Mgły; sprzedaje sadzonki Szczepu w świat („głód to
  najgorszy pirat”).
- Emocja: przygoda → zdrada. Wyspa: 7 wysepek, łodzie.
- Lochy: · **Latarnia Odpływu** — zdążyć przed wodą — Latarnik walczy tylko w przypływie, w odpływie leży bezbronny, ale ty też tracisz osłony. · **Przechylony Wrak** — utrzymać równowagę — przechył kadłuba przy każdej fazie przesuwa ładunek-osłony. · **Rafa Sadzonek** — wyrywać — Szczep na rafie odrasta z przypływem.
- Finał: **Flagowiec Kompanii** (Ysolde). Cienie: wynurzają się z odpływem.
- Bez „Topielców/Utopców” w nazwach.

**Akt 3 — Wędrujące Wydmy (21–30).**
- Kraina: pustynia w ruchu, karawany, zasypane miasto; bez mumii i obelisków.
- PRAWO: **„Wydma idzie”** — co ~90 s piasek przesypuje się z wiatrem: zasypuje jedne osłony i odsłania inne
  (przestawia teren w poziomie).
- Antagonista: **Kartograf Asim** odkopuje Miasto Pierwszych Powracających („przeszłość należy do żywych”).
- Emocja: ogrom i samotność. Wyspa: **ruchoma karawana** (obóz zmienia miejsce).
- Lochy: · **Zasypany Targ** — odkopywać — boss chowa się pod wydmą i wynurza tam, gdzie piasek odsłania. · **Szklana Studnia** — schodzić po stopionym szkle — echo piorunów sprzed wieków. · **Skarabeusznia** — prowadzić rój skarabeuszy toczących osłony.
- Finał: **Brama Miasta Powracających**. Cienie: zasypane — trzeba je odkopać, zanim wstaną.

**Akt 4 — Korzenie Świata (31–40).**
- Kraina: Głębia, ale nie kopalnia — olbrzymie korzenie Pierwszego Korzenia w absolutnej ciemności.
- PRAWO: **„Ciemność słyszy”** — nie ma światła; każdy dźwięk (cios, bieg, zaklęcie) rozświetla otoczenie falą na 2 s
  i budzi ślepe bestie.
- Antagonista: **Brannoc, Starszy Cechu Kamieniarzy** chce ściąć Pierwszy Korzeń („korzeń, który żywi umarłych, musi
  zginąć”), mimo że wtedy Wyspa zgłodnieje.
- Emocja: klaustrofobia → podziw. Wyspa: pionowe miasto w szybie.
- Lochy: · **Głuchy Szyb** — skradać się dźwiękiem — boss ślepy, słyszy tylko twoje trafienia. · **Śpiewające Korzenie** — grać na korzeniach jak na organach (dźwięk = mapa). · **Kuźnia Ciszy** — tłumić hałas — kowale zakładają ci „dzwonki” zdradzające ruch.
- Finał: **Serce Korzenia** (Brannoc). Cienie: widzisz je tylko w fali dźwięku.

**Akt 5 — Mroźna Korona (41–50).**
- Kraina: lodowe płaskowyże, kurhany, zamarznięte jezioro.
- PRAWO: **„Co stoi, zamarza”** — istota nieruchoma przez 3 s pokrywa się lodem (spowolnienie, potem blokada);
  zamarznięty wróg pęka od mocnego ciosu.
- Antagonistka: **Matka Kurhanów Sigrun** zamraża zmarłych, by nie wracali („pokój to bezruch”).
- Emocja: żałoba i cisza. Wyspa: jedna wielka tafla jeziora z pęknięciami zmieniającymi się w czasie.
- Lochy: · **Kurhan Zamrożonych** — rozmrażać ruchem — boss zamraża arenę, kiedy ty stoisz. · **Lodowy Dzwon** — dzwonić, by pękały zamrożone przejścia. · **Śnieżna Sfora** — biec bez przerwy — sfora czeka, aż staniesz.
- Finał: **Tron Bezruchu** (Sigrun). Cienie: zamrożone w lodzie — rozbij, by uwolnić (i ukoić).

**Akt 6 — Srebrna Knieja (51–60).**
- Kraina: DZIKA nocna puszcza elfów; nie ziemia uprawna (Akt 1) i nie drzewo-matka (010); bez jeleni.
- PRAWO: **„Cień księżyca jest ciałem”** — w blasku księżyca cienie stają się stałe (mosty, ściany); wróg w cieniu
  jest nietykalny, w świetle podatny.
- Antagonistka: **Ilvessa, kapłanka nocy** (elfka, aluzja do Aelyry) chce wiecznej nocy, by cienie przodków nie odeszły
  („pamięć jest ważniejsza niż świt”).
- Emocja: oczarowanie → niepokój. Wyspa: puszcza z polanami, cienie zmieniają drogi nocą.
- Lochy: · **Most z Cienia** — chodzić po cieniu — boss gasi księżyc chmurą. · **Sowia Wieża** — przestawiać lustra-księżyce. · **Polana Przodków** — prowadzić cienie przodków do światła.
- Finał: **Ołtarz Wiecznej Nocy**. Cienie: istnieją tylko w cieniu księżyca.

**Akt 7 — Ziemie Paktu (61–70).**
- Kraina: front Przymierza i Paktu, palisady, pola bitew.
- PRAWO: **„Sztandar włada ziemią”** — w strefie swojego sztandaru leczenie i zasób rosną, w strefie obcego spadają
  o połowę; sztandary przejmuje się w walce.
- Antagoniści: ekstremiści obu stron — **Inkwizytor Latarni Corvin** (spalić Powracających) i **Głodna kapłanka
  Vhoraxa Mira-Ossa** (podnieść wszystkich); gracz rozbraja wojnę. Rasy nie są wrogiem.
- Emocja: gniew → ciężar wyboru. Wyspa: linia frontu przesuwana tygodniowo przez społeczność (A2 lekko).
- Lochy: · **Palisada Rozejmu** — przenosić sztandar. · **Obóz Jeńców** — uwalniać obie strony. · **Stos Inkwizycji** — gasić stos, zanim spłonie Powracający.
- Finał: **Rozejm** (oba ekstremizmy naraz). Cienie: walczą pod sztandarem frakcji swojego właściciela.

**Akt 8 — Popielna Korona (71–80).**
- Kraina: wulkan i deszcz popiołu; bez kapłanów ognia i bez smoczej kolumny.
- PRAWO: **„Popiół rośnie”** — popiół stale opada, a podłoga areny podnosi się o 1 bl. co ~40 s, zasypując niskie
  osłony; sufit się zbliża.
- Antagonistka: **Mistrzyni Kalda, Zakon Popiołu**, chce zasypać świat, bo popiół pieczętuje groby.
- Emocja: desperacja. Wyspa: zbocza wulkanu z osadami.
- Lochy: · **Zasypana Kaplica** — wspinać się, zanim zasypie — boss zasypuje przejścia. · **Dzwonnica Popiołu** — odkopywać ocalałych. · **Odlewnia** — chłodzić formy.
- Finał: **Krater Pieczęci**. Cienie: wynurzają się z popiołu na wyższym poziomie.

**Akt 9 — Podniebie (81–90).**
- Kraina: wyspy w chmurach, pusty Trybunał Vharuna.
- PRAWO: **„Wiatr ma kierunek”** — stały wiatr znosi pociski i przyspiesza ruch z wiatrem, a co cykl obraca się o 90°;
  upadek oznacza lądowanie na niższej wyspie.
- Antagonista: **Sędzia Bez Boga**, samozwańczy sędzia Vharuna („skoro bóg milczy, ja wydam wyrok”).
- Emocja: wzniosłość → zawrót głowy. Wyspa: pionowy archipelag chmur.
- Lochy: · **Ława Przysięgłych** — przekonać wiatr (żagle-osłony). · **Wieża Wag** — ważyć winy. · **Sala Milczenia** — walczyć bez dźwięku.
- Finał: **Trybunał**. Cienie: niesione wiatrem z kierunku przeciwnego.

**Akt 10 — Kres (91–100).**
- Kraina: miejsce, w którym Echo się rodzi; fragmenty wszystkich krain.
- PRAWO: **„Echo”** — wszystko, co ginie, wraca RAZ jako Cień, a w lochach wracają echa praw aktów 1–9 (po jednym na
  loch).
- Antagonista: **Tkacz Echa**, pierwszy kapłan Vharuna („nikt nie zostanie zapomniany”).
- Emocja: katharsis i pożegnanie. Wyspa: wyspa-mozaika.
- Lochy: · **Rozdarty Sad** — echo Aktu 1. · **Archiwum Cieni** — walka z Cieniami serwera według rekordów. · **Ostatni Przypływ** — echo Aktu 2.
- Finał: **Serce Echa** — przycięcie, nie ścięcie (motyw Aktu 1); posągi Cieni zostają w Stolicy.

## D. Macierz 10 aktów
| Akt | Prawo | Kolor | Pora/pogoda | Żywioł | Rodzina wrogów | Emocja |
|---|---|---|---|---|---|---|
| 1 | śmierć → wzrost | zieleń, róż, złoto | złota godzina | ziemia/roślina | zaszczepione zwierzęta i owady | zachwyt → katharsis |
| 2 | przypływ (pion, cykl) | turkus, biel mgły | mgła, sztorm | morze | załogi Kompanii, raf-stwory | przygoda → zdrada |
| 3 | wydma przestawia osłony (poziom) | ochra, złoto | upalne południe | piasek | skarabeusze, piaskowe konstrukty | ogrom, samotność |
| 4 | ciemność słyszy | czerń + bursztyn | brak dnia | korzeń/kamień | ślepe bestie, kamieniarze | klaustrofobia → podziw |
| 5 | bezruch zamarza | biel, błękit | zamieć, białe noce | lód | sfory, kurhanowi strażnicy | żałoba, cisza |
| 6 | cień księżyca jest ciałem | srebro, granat | noc, pełnia | światło/cień | duchy kniei, sowy, łowcy nocy | oczarowanie → niepokój |
| 7 | sztandar władający strefą | czerwień i błękit frakcji | dzień frontu, deszcz | stal | ekstremiści obu frakcji | gniew → ciężar wyboru |
| 8 | podłoga rośnie (popiół) | szarość, pomarańcz żaru | wieczny zmierzch popiołu | ogień/popiół | zakon Popiołu, popielne golemy | desperacja |
| 9 | wiatr kierunkowy | biel, złoto słońca | jasne niebo, burza | powietrze | strażnicy Trybunału, ptaki burzy | wzniosłość → zawrót |
| 10 | Echo (powrót + echa praw) | perłowa szarość + barwy wszystkich | bez pory | pamięć | Cienie i echa wszystkich rodzin | pożegnanie |
Kontrola powtórek: brak nocy poza 6, brak morza poza 2, brak ognia poza 8, brak lodu poza 5. Licznikowych „metrów”
(żar, zimno) nie ma — każde prawo jest fizyczne i widoczne. Uwaga w E1.

## E. Ryzyka i najsłabsze miejsca
1. **Trzy prawa „teren zmienia się w czasie”** (2 przypływ, 3 wydma, 8 popiół). Różne osie (pion cykliczny / poziom /
   pion monotoniczny), ale łatwo o to samo odczucie. Najsłabszy punkt planu; do sprawdzenia w sporze, może Akt 3
   dostanie inne prawo.
2. **Patent Nemesis** — nasz Cień to nie hierarchia NPC, ale opinia prawna przed startem jest obowiązkowa.
3. **Moderacja Cieni** (nick, skórka) — filtr i zgłoszenia od pierwszego dnia, inaczej toksyczne posągi.
4. **Pusty serwer = brak Cieni**: rezerwa Cieni deweloperskich, inaczej mocna rzecz nie działa przy małej liczbie
   graczy.
5. **Akt 7**: konflikt grywalnych frakcji jest delikatny; wrogami muszą być ekstremiści OBU stron, nigdy rasa.
6. **Akt 10 „echa praw”** grozi kolażem (blokowaliśmy to w 010 Aktu 1): jeden echo-prawo na loch, nie mieszanka.
7. **Koszt 100 lochów**: Akt 1 to 10 kart po ~2 tyg. dyskusji. Proponuję 10 lochów w Aktach 1–3, potem 6–8 + Rozdroża
   Cieni na wyspie. Decyzja właściciela.
8. **Akt 9 (Podniebie) i Akt 2 (Archipelag)** to dwa „archipelagi” (morze/chmury); forma wyspy różna, ale sprawdzić
   kadry WOW.
9. **Zwrot EXP przez Cień** zmienia ekonomię kary za śmierć; trzeba policzyć, by nie dało się „inwestować” w śmierć.
10. Najsłabszy akt: **Akt 3** — prawo bliskie Aktom 2 i 8, a antagonista (kartograf) najmniej groźny.
