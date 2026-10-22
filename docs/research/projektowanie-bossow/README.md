# Projektowanie bossów i mobów — baza wiedzy

Research z 2026-09-30 (książki, GDC, artykuły projektantów, przewodnik stylu Blockbencha, filozofia Mowzie's Mobs).
Po co: właściciel ocenił Strażnika Gaju jako pozbawionego sensu artystycznego i napisał „źle do tego podchodzisz”.
Ten dokument jest punktem odniesienia dla każdego bossa i moba, zanim powstanie choć jedna kostka.
Źródła są na końcu, a oznaczenia w nawiasach ([GDK], [LDB] itd.) prowadzą do nich.

---

## 1. Kolejność pracy: najpierw „po co”, potem „kto”, na końcu „jak wygląda”

1. **Po co ten przeciwnik istnieje w grze** [GDK]. Nie zaczynamy od historii ani od wyglądu.
   Odpowiadamy na pięć pytań:
   - którą umiejętność gracza eksponuje (unik, blok, przerwanie, pozycjonowanie),
   - którą **zdolność gracza** sprawdza (refleks, timing, zarządzanie zasobami, priorytety celów, współpraca),
   - co wnosi do różnorodności (AI, ataki, statystyki),
   - jakie zachowanie gracza nagradza albo zniechęca (np. stanie w miejscu, łucznictwo zza osłony),
   - **jaką emocję ma wywołać** (grozę, bezsilność, podziw, poczucie mocy, satysfakcję).
2. **Haczyk** (Mowzie's Mobs) [MM]. Jeden główny element rozgrywki, który zmienia sposób walki.
   Chodzi o to, żeby gracz nie mógł po prostu „wbiec i bić”. Przykłady:
   - Ferrous Wroughtnaut: pancerz z jednym słabym punktem, który trzeba odkryć,
   - Umvuthi: stoi w miejscu i dominuje z dystansu, więc trzeba się do niego przebić.

   Zdolności mają wynikać z haczyka, a nie być zbiorem przypadkowych ataków. **Mniej znaczy lepiej.**
3. **Archetyp** [GDK]. Z odpowiedzi 1–2 wynikają AI, zdolności i statystyki.
   Dopiero ta całość trafia do narracji (kto to jest) i do grafiki (jak wygląda).
4. **Historia wynika z haczyka, nie odwrotnie.** Motywacja musi być spójna: da się poprowadzić prostą linię od pochodzenia do obecnych działań [VIL].
   Przeciwnik zaprojektowany wokół funkcji („niszczy wioski, bo zły”) jest dekoracją sceny.
   Przeciwnik z przekonaniem („cierpienie buduje siłę”) jest postacią.
5. **Wygląd wynika z funkcji** („form follows function”) [CRE].
   Żadnego kolca, rogu ani kończyny „bo fajnie wygląda”. Każda cecha ma odpowiedź na pytanie „po co?”.

## 2. Boss to test i opowieść jednocześnie

**Boss jako test** [BBS]:
- sprawdza opanowanie tego, czego loch uczył wcześniej;
- nowej mechaniki uczymy w bezpiecznych warunkach, a sprawdzamy dopiero pod presją. Nigdy jedno i drugie naraz.

**Boss jako opowieść: osiem taktów Mike'a Stouta** [BBS]:
1. **Budowanie napięcia przed walką:** zapowiedzi, ślady, przestrogi. Gracz ma wiedzieć, jak groźny jest boss.
2. **Wejście i pokaz siły.**
3. **„Zwykły dzień”:** podstawowe ataki. Gracz uczy się wzorców względnie bezpiecznie.
4. **Eskalacja:** nowe ataki i komplikacje.
5. **Punkt zwrotny:** fałszywe zwycięstwo, przemiana. Chwila oddechu, po której stawka rośnie.
6. **„Na całość”:** pełen arsenał, maksymalna presja.
7. **Pokonanie:** boss słabnie, klęka. Ma być widowiskowo („make it worth the player's while”).
8. **Zwycięstwo:** nagroda i rozładowanie napięcia. Gracz ma poczuć się wspaniale.

**Boss musi się bronić**, czyli mieć osłonę, odskok albo kontrę [BBS]. Inaczej jest workiem treningowym i nie zdąży pokazać zaprojektowanych ataków.

**Mechanika opowiada postać bez słów** (Blizzard) [BLZ][BB]. Zdolności wyrażają naturę bossa:
- Lich King: skażenie i przyciąganie śmierci,
- Yogg-Saron: szaleństwo, przez mechanikę poczytalności.

Arena też opowiada: „zrujnowana świątynia sugeruje upadłego boga”, a przy wściekłości bossa reaguje otoczenie. Działa **powściągliwość**: kilka precyzyjnych zdolności mówi więcej niż wiele skomplikowanych.

**Blizzard zaczyna od obrazu albo fikcji** [PCG]. Przykład: Tortos wziął się z pomysłu, że gracz spada do prastarych podziemi, a drogę blokuje kolosalny żółw. Fikcja prowadzi mechanikę, a projekt całego skrzydła dopasowuje się do motywu bossa (Lei Shen).

**Souls** [DSB]. Każdy boss to jednorazowe wydarzenie, a nie „duży zwykły mob”. Arena pasuje do motywu bossa. Boss ma co najmniej około 5 ataków, używanych w zmiennej kolejności. Seria bossów tworzy program nauczania: każdy uczy czegoś, co przyda się dalej.

**Intymność ponad skalę** [EPIC]. Stawka osobista (co boss zrobił temu miejscu i tym ludziom) zostaje w pamięci. Samo „ratowanie świata” nie.

## 3. Czytelność: zapowiedź i oczekiwanie

- **Czytelność = zapowiedź** (co się zaraz stanie) **+ oczekiwanie** (spójność skutków) [ARPG]. Gracz musi zrozumieć, co go zabije, zanim to nastąpi.
- **Anatomia ataku** [GDKA]: zapowiedź → uderzenie → powrót.
  - **Zapowiedź** ma unikalną pozę i pokazuje także, **gdzie** padnie cios.
  - **Uderzenie** jest krótkie i zdecydowane.
  - **Powrót** to okno kontrataku.
- **Długość zapowiedzi** ≥ czas reakcji (~0,25 s) + czas akcji gracza (np. uniku) + margines trudności [GDKA]. Zapowiedzi wrogów są wolniejsze niż ataki gracza [ANIM].
- **Spójny język wizualny** [ARPG]: jeśli czerwone świecenie oznacza „nie do zablokowania”, to każde czerwone świecenie oznacza to samo.
- **Obrażenia zgodne z wyglądem ataku** [ARPG]. Zgon od małego szturchnięcia jest nieuczciwy, zgon od meteoru jest epicki.
- **Mało ruchów, każdy wyraźny** [HK]. Hollow Knight ma 9 ataków, Soul Master 6, Traitor Lord 4. Trudność bierze się z tempa i nakładania się ataków, a nie z zamieszania.
- **Dźwignie trudności bez utraty czytelności** [ARPG]:
  - łańcuchy wzorców,
  - krótsze okna na kontrę,
  - modyfikatory (większy zasięg, żywioł),
  - przyspieszenie, ale dopiero po opanowaniu wzorca,
  - partner albo klon bossa,
  - presja czasu,
  - zasób do pilnowania.
- **Sylwetka czytelna z 10+ m** [LDB], a u gracza w naszej grze z 4–10 bloków.
- **Role mobów** [LDB]: grunt, oddział, lider, tank, rój, snajper. Macierz szybkość × zdrowie × zasięg × DPS pokazuje dziury w obsadzie. Kombinacje ról mają dawać nowe sytuacje.

## 4. Projekt postaci: sylwetka, kształt, jasność, jedna idea

- **Ok. 70% wrażenia daje sylwetka, 30% detal** [SHP]. Test: czarny kształt na białym tle.
  - Trzy odległości: portret, typowa odległość w walce, granica rozpoznania.
  - Pięć póz: bezruch, atak, ruch, reakcja na trafienie, triumf [NR].
  - Jeśli sylwetka nie przejdzie, poprawiamy bryłę, nie teksturę.
- **Język kształtów** [SHP]:
  - koło = przyjazne, bezpieczne;
  - kwadrat = siła, stabilność;
  - **trójkąt = zagrożenie, nieprzewidywalność** (ulubiony kształt złoczyńców).

  Postać łączy kształty z jedną dominantą. Zasada 70/30: 70% dominujący kształt, materiał albo kolor, 30% kontrast. Projekt podzielony po równo między dwie idee czyta się z daleka jako niezdecydowany [NR2].
- **Struktura jasności** [NR2]:
  - najjaśniejszy albo najbardziej kontrastowy punkt = **punkt skupienia** (twarz, broń, słaby punkt), reszta w średnich i ciemnych tonach;
  - w stylizacji jasność jest ściśnięta do kroków (cień, półton, światło), a nie płynnego gradientu;
  - paleta: jeden kolor dominuje, drugi tworzy punkt skupienia.
- **Konsolidacja detalu** (Blizzard) [MCD][BLZS]: wiele drobnych szczegółów łączymy w mniej, a większe (np. 3 wyraźne sęki zamiast 20). Zasady czytelności: skala i proporcje, sylwetka, światło, kolor, przesada, kompozycja.
- **Przesada proporcji** [BBMC]: duża głowa = uroczy, przerośnięty tors = siła. Proporcje przekazują charakter.
- **Wiarygodna anatomia** (Terryl Whitlatch) [TW]: fantastyczne stworzenie jest wiarygodne, kiedy opiera się na prawdziwej anatomii (szkielet, mięśnie, podparcie ciężaru). Zaczynamy od realnych cech zwierząt i dodajemy jeden-dwa własne zwroty [CRE].
- **Wzorcowy model i porównania** [NR]: zatwierdzamy jeden wzorcowy model (proporcje, gęstość tekseli, zestaw materiałów), a każdy kolejny oglądamy obok niego, w tym samym świetle. Ocena w pojedynkę przepuszcza dryf stylu.
- **Wierność koncepcji** [NR]: modelarz 3D odruchowo spłaszcza przesadę konceptu w stronę „poprawnej” anatomii. Dlatego koncept kładziemy na bryłę na każdym etapie (bryła, detal, finał).

## 5. Styl Minecrafta i Blockbencha (oficjalny przewodnik) [BBMC]

- **Liczba elementów** jak najmniejsza przy zachowaniu czytelności. **Kształt robi model, detal robi tekstura.**
  „Dobrze oteksturowany prosty prostopadłościan wygrywa ze sprytną bryłą ze słabą teksturą” [3DX].
- **Obrót** daje skosy zamiast schodków, ale musi mieć uzasadnienie. Nie łączymy obracanych elementów w łuki.
- **1 piksel tekstury = 1 piksel modelu.** Żadnych mixeli: elementów < 1 px ani nadmiernie „napompowanych”.
- **Tekstura:** najpierw półton, potem jeden cień i jedno światło, potem rozszerzenie palety. Między odcieniami przesuwamy barwę, nasycenie i jasność.
- **Kolorów mało, cieniowanie 2–3 tonami**, bez gradientów [3DX].
- **Błędy do usunięcia:**
  - **banding**, czyli piksele układające się w linie i odsłaniające siatkę;
  - **pillow shading**, czyli ciemnienie koncentrycznie ku wszystkim krawędziom — **uwaga: to robi nasz obecny `kora2` („bryła na każdej płaszczyźnie”)**;
  - **pancake shading**, czyli światło z jednej strony, cień z drugiej, bez względu na kształt;
  - **szum** z pędzla bez informacji;
  - mixele tekstury i ząbki (jaggies).
- **Encje:** góra i przód jaśniejsze niż spód i tył, zarówno w obrębie ściany, jak i między ścianami. Proces: półton ze światłem i cieniem → rozszerzenie palety → **materiał definiowany ułożeniem skupisk tonów** → usunięcie artefaktów.
- **Mowzie's Mobs / Alex's Mobs** [MM][AM]:
  - „vanilla-esque”: styl zgodny z najnowszymi mobami gry;
  - każdy mob ma funkcję, nie ma mobów „dla ozdoby”;
  - brak one-shotów, kradzieży przedmiotów i nieuniknionych obrażeń;
  - wygląd zgodny z funkcją;
  - prostota.

## 6. Animacja w służbie walki [GDKA][ANIM][PSY]

- **Czasy klatek:** wolno w zapowiedzi, szybko w uderzeniu, wolno w powrocie. Równe tempo daje „kluchowaty” cios. **Pauza przed uderzeniem sprzedaje atak.**
- **Uderzenie:** poza pokazuje zasięg i czas trwania strefy trafienia.
- **Każda zapowiedź** ma wyglądać drastycznie inaczej niż pozostałe. Pomagają efekty.
- **Psychonauts 2:** ataki składane z modułów (zapowiedź → atak → powrót), które da się łączyć w łańcuchy i używać ponownie w fazach.

## 7. Scena i emocja

- **Wejście:** napięcie budowane wcześniej (ślady, dźwięk, zapowiedź), potem pokaz siły [BBS].
- **Muzyka:** motyw przewodni bossa. Cichy, niepokojący początek eksploduje przy odsłonięciu [MUS].
- **Arena:** pasuje do motywu i reaguje na fazy [BB][DSB].
- **Pamięć gracza:** z walki zostaje jeden moment (zwrot, przemiana, zwycięstwo) [BBS].

## 8. Lista kontrolna przed modelem (bramka)

1. Jaką emocję boss ma wywołać: jedno słowo na każdy z ośmiu taktów.
2. Haczyk w jednym zdaniu. Czy zmienia sposób walki?
3. Co sprawdza: umiejętności poznane w lochu, z listą.
4. Kim jest i dlaczego tu jest. Motywacja spójna z lochem, z linią od pochodzenia do teraz.
5. Zdolności: 4–9. Każda wynika z haczyka i historii. Każda ma zapowiedź (poza, czas ≥ reakcja + akcja), uderzenie, powrót i kontrę.
6. Obrona bossa: czym się broni i jak odzyskuje przestrzeń.
7. Wygląd wynikający z 1–6. Każda cecha ma odpowiedź na „po co?”. Sylwetka: dominujący kształt (trójkąt = groza), test czarnego kształtu w 3 odległościach i 5 pozach. Punkt skupienia = słaby punkt, najjaśniejszy element. Paleta: 1 dominanta + 1 akcent. Detal skonsolidowany.
8. Materiały zgodne ze stylem Blockbencha: kilka tonów, bez pillow shadingu i szumu, góra i przód jaśniej.
9. Szkice w 2D (miniatury, wiele wariantów) → wybór → wierny projekt → bryła z projektem na wierzchu.
10. Porównanie z górną półką paczki i z zatwierdzonym wzorcem, w tym samym świetle i kadrze.

## 9. Co robiłem źle przy Strażniku (zestawienie z zasadami)

| Zasada | Co zrobiłem |
|---|---|
| Najpierw funkcja, haczyk i emocja (§1) | Zacząłem od wyglądu. Specyfikacja walki i motyw lochu leżały obok, a nie były punktem wyjścia. |
| Wygląd wynika z funkcji (§1.5, §4) | Doklejałem cechy („straszniejszy” = kły, korzeń, pęknięcie). Nie było odpowiedzi na „po co?”. |
| Historia spójna (§1.4) | Nie było odpowiedzi na pytanie, kim jest boss. Czaszka, żebra, serce i sakura nie miały wspólnej logiki. |
| Jedna idea, 70/30, punkt skupienia (§4) | Pięć barw i dwa punkty skupienia (maska, serce), które konkurowały ze sobą. |
| Szkice → wybór → wierny model (§4, §8) | Jeden szybki szkic, potem łatanie brył w 3D metodą prób. |
| Styl Blockbencha (§5) | Najpierw szum, potem pillow shading (`kora2`). Oba są wprost na liście błędów. |
| Porównanie z wzorcem i akceptacja etapów (§4) | Pokazywałem półprodukty i pytałem „lepiej?”, więc właściciel musiał pełnić rolę art directora. |

## Źródła

- [GDK] GDKeys, *Keys to Rational Enemy Design* — https://gdkeys.com/keys-to-rational-enemy-design/
- [GDKA] GDKeys, *Keys to Combat Design: Anatomy of an Attack* — https://gdkeys.com/keys-to-combat-design-1-anatomy-of-an-attack/
- [LDB] The Level Design Book, *Enemy design* — https://book.leveldesignbook.com/process/combat/enemy
- [ARPG] Game Developer, *Designing for Difficulty: Readability in ARPGs* — https://www.gamedeveloper.com/game-platforms/designing-for-difficulty-readability-in-arpgs
- [BBS] Game Developer (Mike Stout), *Boss Battle Design and Structure* — https://www.gamedeveloper.com/design/boss-battle-design-and-structure
- [DSB] Game Developer, *The Impact of Dark Souls on Boss Design* — https://www.gamedeveloper.com/design/the-impact-of-dark-souls-on-boss-design
- [PSY] Game Developer, *Psychonauts 2 boss maneuvers* — https://www.gamedeveloper.com/marketing/using-a-modular-system-of-maneuvers-to-design-i-psychonauts-2-i-s-boss-fights-in-a-hurry
- [BB] Bret Blevins, *The Art of Boss Design in Warcraft* — https://bretblevins.com/the-art-of-boss-design-how-mechanics-tell-stories-without-dialogue/
- [PCG] PCGamesN, *WoW 5.2: the art and science of raid design* — https://www.pcgamesn.com/wow/world-warcraft-52-raids-art-and-science-raid-design
- [BLZ] Blizzard o projektowaniu rajdów — https://teamliquid.com/news/2022/12/23/new-paths-past-tiers-blizzard-talks-raid-design
- [HK] Analizy walk w Hollow Knight — https://medium.com/@roryhoeschen/the-hypnotizing-simplicity-of-hollow-knights-combat-a60f820fd433
- [MM] Mowzie's Mobs (filozofia, haczyk, zasady) — https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs
- [AM] Alex's Mobs — https://www.curseforge.com/minecraft/mc-mods/alexs-mobs
- [BBMC] Blockbench Wiki, *Minecraft Style Guide* — https://blockbench.net/wiki/guides/minecraft-style-guide/
- [3DX] *How to Make Minecraft Models* — https://3dxdev.com/how-to-make-minecraft-models-the-tools-and-constraints/
- [SHP] Shape language: https://rocketbrush.com/blog/shape-language-in-game-character-design-how-to-make-characters-readable-and-consistent , https://pixune.com/blog/shape-language-technique/
- [NR] *Stylized 3D Characters Done Right* — https://nastyrodent.com/stylized-3d-characters-art-direction-principles/
- [NR2] *Color Theory for Game Art* — https://nastyrodent.com/color-theory-for-game-art/
- [MCD] 80.lv, *Matt McDaid — Mastering the Stylized Art* — https://80.lv/articles/matt-mcdaid-mastering-the-stylized-art
- [BLZS] Styl Blizzarda — https://spacefoxgames.com/how-blizzard-keeps-wows-art-style-timeless-across-generations/
- [TW] Terryl Whitlatch, *Science of Creature Design* / *Principles of Creature Design* — https://designstudiopress.com/products/science-of-creature-design
- [CRE] *How to Create Fantasy Creatures* — https://www.quillandsteel.com/blogs/writing-tips/how-to-create-fantasy-creatures
- [VIL] *Villain Ideas* — https://robinpiree.com/blog/villain-ideas
- [EPIC] Game Developer, *Epic Scale* — https://www.gamedeveloper.com/design/epic-scale
- [MUS] Game Developer, *Boss Music* — https://www.gamedeveloper.com/audio/getting-making-game-music-that-fits-classic-genre-series-boss-music
- [ANIM] *Animation principles for pixel art* — https://www.sprite-ai.art/guides/animation-principles
- Do przeczytania w całości (niedostępne przez pobieranie): Scott Rogers, *Level Up!* (rozdz. 11 „Bosses”, szablon bossa; GDC 2008 „How to Create the Greatest Boss Battle”), Itay Keren, GDC 2018 „Boss Up” (YouTube), Marc Taro Holmes *Designing Creatures and Characters*, Bryan Tillman *Creative Character Design*.
