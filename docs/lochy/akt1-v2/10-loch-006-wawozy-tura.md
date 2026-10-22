# 006 Wąwozy Tura

## 0. Kontrakt
- Poziom 6 (otwarcie od 5); Normalny solo 15–19 min; 1–4 graczy bez wymaganej roli.
- Fantazja: tropię zwierzę w ciasnych jarach, wybieram miejsce spotkania, aż ono poznaje mój zapach i zaczyna tropić mnie.
- Emocja: adrenalina łowcy → nagła świadomość bycia ofiarą → szacunek dla zwierzęcia.
- Motyw: **trop, przynęta, przesmyk**; wiedza o trasie Tura ma zamieniać szarżę w jego własną słabość.
- Pąk R1: tylko po poległym, ten sam model/dźwięk w całym akcie; LPM, PPM, zaklęcie, pocisk, pet i przywołaniec usuwają go trafieniem. Ostatnia 1 s pęcznieje i świeci; po stałych 5 s (Łatwy 8, Trudny 4) dojrzewa w **kwitnącą przynętę** na 7 s.
- Przynęta z pąka przyciąga następny wybór **kierunku** Tura, nie leczy go i nie wymusza sprzątania; można pozwolić jej dojrzeć przy murku, by ustawić klinowanie poroża. Ręczna przynęta w C/D to drewniana miska z owocem, odrębny obiekt bez modelu pąka.
- Kolejka per instancja: maks. 12 pąków; trzynasty ususza **najstarszy według chwili powstania** z 1 s szarzenia i dźwiękiem. Maks. 6 zgonów/10 s w starciu. Chowaniec i przywołaniec depczą obok.
- Pąk po graczu: Cierń i Płatek ochrony tylko po osobnym teście przepływu śmierci; plan B bez niego.

## 1. Cztery akty łowów
- Wstęp A/B: trzy ślady (zerwany wrzos, świeże nacięcie murku, ciepła para przy ziemi) tworzą kierunkową linię; za kratą młody Dzik wbija łeb w murek i odsłania bok. Bez walki z Turem.
- Rozwinięcie C/D: gracz wybiera pierwszą z dwóch kieszeni i aktywuje przynętę lokalnym owocem albo miodem z 003; **to działanie**, nie timer, przywołuje Tura. Pierwsza aktywacja w C **lub** D daje spotkanie i odłam I, potem Tur ucieka przez oznaczony wylot.
- Druga, **inna** przynęta daje pościg do F i odłam II. F pozostaje za jawną kratą dopóki nie są aktywne obie; z C prowadzi C–E–F, z D prowadzi D–F. Nie ma czekania na odejście Tura.
- Zwrot F: w drugim spotkaniu Tur rozbija murki osłony; gracz ze znakiem zapachu na 10 s staje się przynętą. Zawracanie za stary murek przestaje działać, ale wąski przesmyk klinuje poroże.
- Egzamin H: dwa znane typy osłon, kamienny murek i skalny przesmyk, pozwalają wybrać, gdzie Tur się zatrzyma. Odłam zmienia geometrię następnej szarży, nie usuwa ataku.
- Ślad zawsze prowadzi do jednego z dwóch znanych wejść Tura; łamanie gałęzi i wyjście jest sceną sterowaną progiem HP/pozycją, bez niekontrolowanej wędrówki AI.

## 2. Historia w kamieniu
- Bractwo wypasało tu stada po Głodnym Roku; pierwszy zaszczepiony Tur miał być silniejszym zwierzęciem pociągowym, lecz wyrosłe poroże zaczęło łamać zagrody.
- Na deskach Mostu Sznurów są ślady kopyt tylko z jednej strony; pasterze uciekali górą, a zwierzęta zostały w dolnym wąwozie. To obraz kosztu bez oskarżania rasy ani religii.
- Odkrycie samodzielne: tablica przy D z datą i odciskiem Darnika: „Pierwszy udany szczep na zwierzęciu: Tur. Nie wypuszczać na pastwisko.” Obok zerwana lina pokazuje, że wypuszczono.
- Wpis 006 do Mapy Granicy per gracz opowiada cały fakt, nawet jeśli 003/005 nie rozegrano. Po ukończeniu do Stolicy trafia ocalały pasterz i szkic przejść.
- Kwestie (2): pasterz za kratą: „Most utrzymał ludzi. Jego już nie.” Po walce: „On nie wracał po zemstę. Wracał do stada.”

## 3. Mapa ASCII
```text
             N↑; 1 znak ≈ 10 bl.; całość 118×88; y=60–81
  [B 24×20]──[C 26×22]──[E 22×20]
     │          │            │
  [A 22×20]─[M 30×18]──[D 26×22]  M = Most Sznurów
  ognisko       ║            ╲   ╱
     ↑          ║dolny kosz  [F 22×20]──[G 24×20]
     └──[S 20×18]───────────────┘       │
                                      [H 34×30]
```
- Osiem odcinków A/B/C/M/D/E/F/G oraz arena H; każdy ma 20–34 bl. użytecznej długości, kamienne kieszenie i czytelny cel. S to krótki zjazd powrotny do A.
- Dwie trasy **zwierzęcia** losowane przy wejściu: północna C→E→F albo południowa D→F→G; trop i glina pokazują wariant wyjścia z pierwszej kieszeni, niezależnie od pierwszego wyboru gracza. Tur ma osobny wybieg za kratą F; gracz przechodzi tę kratę dopiero po obu przynętach.
- Gracz może najpierw badać C lub D. Krata przed F ma dwa obejścia dojścia, C–E–F i D–F, ale otwiera się dopiero po obu różnych przynętach; jej znak dwóch rogów widoczny z M. F nie jest skrótem do drugiego odłamu przed aktywacją drugiej przynęty.
- Brama H widoczna z Mostu; otwarcie po dwóch odłamach/spotkaniach. S otwiera się po F, a M prowadzi do A przez B lub D–M–A.
- **Most Sznurów Y**: górny pomost y=78 pokazuje trop w glinie E, dolny kosz y=70 daje boczną osłonę przed szarżą z D. Stałe dwie kondygnacje, bez kołowrotu/naciągu jako drugiej mechaniki.
- Upadek z pokładu prowadzi na miękkie siano y=60 do S, bez śmierci, z 12 s powrotem. Każdy pokład ma 3-blokową szerokość, poręcze i czytelne wejścia; górą informacja, dołem bezpieczniejsza pozycja.

## 4. Beat chart
| Odcinek | Rola/min | Skład i powód | Start | Tempo | Poza walką |
|---|---|---|---|---|---|
| A | kierunek 0–1 | brak; murek, ślady i ognisko | gracz widzi arenę z daleka | niskie | próbny trop i brama |
| B | nauka 1–3; opcjonalnie 5–7 przy D-first | Dzik + Muflon; uczą prostej szarży i wysokości | gracz widzi oba z góry | wysokie | znak trasy na glinie |
| C | kieszeń C 3–5 lub 7–9 | para Wilków Wrzosowych + Dzik; jeden wilk zamyka wyjście, drugi spycha ku linii dzika | gracz kładzie przynętę po walce | wysokie | odłam I, jeśli C pierwsze; druga aktywacja, jeśli C drugie |
| M | kadr/wybór 1–3 przy D-first lub 5–7 przy C-first | Muflon patroluje górę, wilk dół; nie jednocześnie | gracz wybiera pokład | niskie→wysokie | Most Sznurów, WOW, ratunek po upadku |
| D | kieszeń D 3–5 lub 7–9 | Kłusownik + Wilk; sieć utrudnia źle ustawiony trop | gracz widzi wnyki | wysokie | odłam I, jeśli D pierwsze; druga aktywacja, jeśli D drugie |
| E | zwiastun 9–10 | brak; rysa w murku od pierwszego przebiegu | Tur słyszany, nie spawnowany | niskie | przejście C→F, sekret poroża |
| F | zwrot 10–12 | Tur + jeden Dzik (max dwa typy); murek rozpada się podczas szarży | druga przynęta w C lub D otwiera kratę | wysokie | klin przesmyku, odłam II |
| G | odzyskanie 12–14 | elita Wilczyca; na Trudnym także Muflon z osobnej półki | Wilczyca warczy z dołu, Muflon zapowiada skok kredowym znakiem z góry | wysokie→niskie | skrót S, ognisko |
| H | egzamin 14–17 | Tur sam; bez fal addów | gracz widzi linię wejścia | wysokie | zagroda i wyjście do minuty 19 |
- C-first: A–B–C, przynęta C → Tur → odłam I → M–D, przynęta D → otwarcie F → pościg D–F → odłam II → G/S → H. D-first: A–M–D, przynęta D → Tur → odłam I → M–C, przynęta C → otwarcie F → pościg C–E–F → odłam II → G/S → H. W obu porządkach obie aktywacje są czynnością gracza, nie automatem.
- Śmierć przed odłamem I resetuje tylko bieżące spotkanie w aktywnej kieszeni; po odłamie I zapisuje pierwszy identyfikator przynęty. Śmierć przed odłamem II resetuje spotkanie F, lecz zachowuje obie aktywacje i kratę F; odłam II, S i H zapisują się po zdobyciu. Ponowne użycie tej samej przynęty nie liczy drugiego spotkania.
- Kontakt z Turem następuje natychmiast po aktywacji przynęty, a jego trasa trwa 4–6 s. Nie ma pustego tropienia ani starć na otwartej łące.

## 5. Bestiariusz i liczby
- L1/L2: poziom 6 daje DPS_ref 5,5 i mnożnik Gaj v8 ×1,60 dla HP i obrażeń. Wartości zaokrąglone; hipoteza do poligonu czterech archetypów.
| Mob | Rola; vanilla → kandydat | HP / surowe | Zapowiedź i kara | Kontra; nieuważny → uważny |
|---|---|---|---|---|
| Wilk Wrzosowy | para blokująca wyjście; wolf → własna wrzosowa grzywa | 64 / 5 | `entity.wolf.growl` 0,9 s: pierwszy wilk zamyka próg, drugi skacze w zamknięty punkt, 10 | przejść przez niski przesmyk między nimi; stoi: 2 ciosy, reaguje: oba tracą osłonę na 1,2 s |
| Dzik Szczepiony | szarża; hoglin → własny pysk | 70 / 6 | `entity.hoglin.angry` 1,1 s, prosta 8 bl., 11 + odrzut | zejść z linii, ustawić murek; stoi: 11, reakcja: ogłuszenie 2 s |
| Muflon Jarowy | kontrola wysokości; goat → własne zakręcone rogi | 51 / 5 | `entity.goat.prepare_ram` 1,0 s, skok z półki w punkt, 10 | zejść pod półkę/poza kredowy znak; stoi: spadek, reakcja: 1,5 s odsłony |
| Kłusownik Bractwa | jedyny humanoid, sieć; husk z pędami → własny | 42 / 3 | `item.crossbow.loading_start` 1,1 s, sieć w zamknięty punkt, po 1 s cios 8 | osłona, krok w bok; sieć ma 12 HP i każdy atak ją przecina; stoi: 8 + zwłoka, reaguje: 0 |
| Zaroślak | stały mob sekretu E; baby husk → własny pęd | 22 / 3 | `block.azalea_leaves.break` 0,8 s, krąg r=3,5, 6 | odskok/blok lub zostawić pąk jako przynętę; stoi: 6, reaguje: 0 |
- Zaroślak nie powstaje automatycznie z pąka w tym lochu; jest jednym stałym mobem w sekrecie E. Dojrzały pąk zawsze daje przynętę, zgodnie z §0.
- Wszystkie szarże zamykają punkt na początku zapowiedzi; drugi efekt też wraca w ten sam punkt. Wnyk Kłusownika nie jest nieusuwalnym CC.

## 6. Elita i boss
- Elita **Wilczyca Wrzosu**: funkcja uczy, że drapieżnik wybiera najkrótszą linię między ofiarą a przesmykiem. Haczyk: dwa skoki z różnych stron tej samej skały, ale oba telegraphed.
- Historia: prowadziła watahę przy stadzie, Szczep utrwalił jej zapach Tura; sylwetka nisko położonej strzały, jasna wrzosowa plama na karku wskazuje bok.
- Elita **248 HP** (≈25% HP bossa)/8 surowe; `entity.wolf.growl` 1,0 s → dwa zamknięte punkty w odstępie 0,5 s; za każdym chybieniem 2 s odsłony. R2 poniżej.
- Boss **Tur Szczepiony**: funkcja egzaminu to odczyt toru i wykorzystanie murku/przesmyku pod presją; nie zbieranie trzech kluczy ani czekanie.
- Haczyk: Tur łamie ludzkie murki, lecz poroże klinuje w naturalnej szczelinie; dwa odłamy zmieniają kąt szarży, więc trzeba na nowo czytać jego sylwetkę.
- Historia: pierwszy szczepiony zwierzak Darnika, nie zły strażnik. Chce dostać się do stada po drugiej stronie zamkniętej zagrody; gracz otwiera ją po walce.
- Wygląd: ciężka czworokątna masa i kwitnące poroże jak gałęzie jabłoni; ciemny brąz 70%, fiolet wrzosu 30%, najjaśniejszy punkt to złamana biała podstawa rogu. Model własny; `mechanical_bull.bbmodel` istnieje, ale ma mechaniczny motyw, więc tylko kandydat do testu sylwetki, bez przenoszenia tekstury.
- Arena H 34×30: 2 murki kruche, 2 skalne szczeliny (3 bl. szerokie), 4 boczne półki i centralny pas 5 bl.; po rozbiciu murków pozostają skalne kontry. Przepaść odgrodzona.
- HP finału **990** (L2: DPS_ref 5,5 × 180 s = 3 min uważnie); H zaczyna zawsze z pełnym 990 HP. Dwa odłamy **zmieniają geometrię ataków**, nie odejmują HP; oba spotkania są wymagane do H.
- F1 100–60%: czytelna prosta; F2 60–30%: Tur kruszy murki, droga ze stałej osłony przechodzi w przesmyki; F3 30–0%: szybkość +10%, krótszy rozbieg, lecz zapowiedzi pozostają ≥0,9 s.
- Odłam I zmienia szerokie zamiatanie w dwa węższe ciosy z pauzą; odłam II zmienia ryk w podwójny krótki impuls z większą przerwą. Szarża, klinowanie i zasiew zostają zawsze — egzamin nie znika.
| Zdolność | Zapowiedź / zamknięcie | Kara | Powrót / kontra |
|---|---|---|---|
| Rozbieg | `entity.ravager.roar` 1,2 s, kurz po prostej, punkt zamknięty | 9 + odrzut | po murku 1 s, po przesmyku 3 s; ruch w bok i wąska szczelina |
| Rogi | `entity.goat.prepare_ram` 1,0 s, dwa jasne łuki | 7 w r=3,5 | 1,5 s; wyjść z łuku/blok; po odłamie dwa węższe łuki i 0,6 s przerwy |
| Ryk stada | `entity.ravager.stunned` 1,1 s, pierścień na ziemi | 5 + przesunięcie z półki | 1,5 s; stanąć za skałą; po odłamie dwa impulsy po 3 |
| Zasiew | `block.rooted_dirt.break` 1,2 s, punkt zamknięty | 6 i korzenny kolec bez pąka (inny kształt i dźwięk) | 2 s; odejść z kręgu, nie mylić z pąkiem R1 |
| Kruszenie murku | `block.stone.break` 1,5 s, murek czerwienieje | odłamki 7 na wskazanej stronie | 2,5 s; zejść za skałę, nowa droga widoczna |
| Trop ofiary | głęboki wdech `entity.hoglin.angry` 1,3 s, ślad pod graczem zamknięty | następny Rozbieg z boku, 9 | 2 s; przenieść przynętę w przesmyk albo schować się we wnęce |
- Anty-kitowanie: Tur przechodzi dwiema stałymi, oznaczonymi trasami; po 6 s braku kontaktu wącha ostatni zamknięty punkt i biegnie drogą z wolną osłoną. Rogi sięgają 3,5 bl. od brzegu obu szczelin, Ryk przecina ich wyloty, a obejście z centralnego pasa dochodzi na każdą półkę. Nie goni przez ściany i nie teleportuje.
- Punkt zwrotny: po F Tur sam rozbija murek, który przedtem chronił gracza. Po wygranej pada na przednie kolana, a zagroda otwiera się na stado; nie ma egzekucji zwierzęcia.
| Solo R2 | Elita | Tur |
|---|---|---|
| Wojownik | blok 1. skoku, krok za skałę przed 2., mocny cios w odsłonę | blok rogów, bok przy Rozbiegu, mocny cios w klin |
| Ninja | przenieść dwa zamknięte punkty za kamień, seria po chybieniu | bok i krótki bieg do szczeliny; atak białej podstawy |
| Mag | trafienie zaklęciem po drugim skoku, skała ucina linię | pocisk w białą podstawę podczas 3 s klinu, bez wejścia pod rogi |
| Akolita | własny blok/ruch, chowaniec może trafić podczas odsłony | ruch do szczeliny; przywołaniec uderza w podstawę po klinie; Tur nie wymaga ręcznego sterowania petem |

## 7. WOW
- Minuta 6, dolny kosz M: spod dwóch wiszących pokładów widać na końcu jaru Tura stojącego na ostańcu na tle złotego popołudnia. Jego poroże kwitnie jak miniaturowa jabłoń.
- Jedno tupnięcie strząsa wrzos z krawędzi; za Turem przebiega małe stado pasywnych sylwetek, a pod graczem napinają się liny Mostu Sznurów. Ryk odbija się z lewej, potem prawej ściany.
- Budowa: stały model Tura na ostańcu, 6–8 display owiec tylko w tej scenie, particle pyłu w jednym pasie, statyczne liny z fence/lead look; bez fizycznej symulacji mostu.

## 8. Paleta
- Dominujące: granite, andesite, mossy_cobblestone, coarse_dirt, rooted_dirt, spruce_planks, hay_block, allium/Nexo wrzos.
- Złota godzina, sucha mgiełka tylko w najniższym jarze; z góry prześwity światła, na dole fioletowe cienie. Żadnego mrozu, ognia ani dzikiej puszczy Aktu 6.
- Wiatr i skrzyp lin w M, krótki dzwonek owcy przy tropie, cisza przed zwrotem; zapowiedź Tura ma pierwszeństwo nad ambientem.

## 9. Sekrety i wybór
- S1: trzy kopyta odciśnięte w glinie B, czwarte za luźnym kamieniem; w środku pierwszy weterynaryjny rysunek szczepu.
- S2: zwisająca torba pod dolnym koszem M rzuca cień na siano; zejście ratunkowe prowadzi do zestawu pasterskiego.
- S3: sznur z górnego pokładu kończy się w pustej ścianie E; dźwięk owczego dzwonka zdradza schowek z listem o stadzie.
- S4: biała rysa na odłamanym rogu po F wskazuje niszę G, gdzie jest kosmetyczny wzór poroża.
- Wybór: przynęta w C albo D jako pierwszy kontakt. Miód z 003 skraca o 1 s podejście Tura, lokalny owoc działa bez wcześniejszego lochu; brak różnicy w EXP/łupie. Po walce pasterz otwiera bezpieczny przepęd do stada; gracz nie wybiera łaski za materiał.

## 10. Powtarzalność
- Przy wejściu losowana północna lub południowa trasa Tura; ślady w B i kadr z Mostu zdradzają ją przed pierwszą przynętą. Zmienia, który murek trzeba chronić i z którego pokładu mostu warto patrzeć.
- Losowany jeden kruchy murek w C albo D: wybór pierwszej kieszeni zmienia przygotowanie F. Obie wersje mają stały skalny przesmyk jako pewną kontrę.
- S: dwa klinowania w H, ≥3 sekrety, bez upadku z mostu; A: boss i oba odłamy, ≤1 upadek; B: boss; C: boss po ≥2 resetach. Ocena nie blokuje progresu.

## 11. Nagroda
- EXP L3: pierwsze przejście Normalny **720** na osobę (moby/elita 288, boss 288, sekrety 144), powtórka **300**; Łatwy ×0,7, Trudny ×1,3. Pula nie dzieli się w drużynie.
- Rodzina robocza: Pas Tura (+4% redukcji odrzutu), Nóż Tropiciela (+5% obrażeń po klinowaniu), Buty Wąwozu (+5% prędkości poza walką). Materiał profesji: Włókno Wrzosu; tylko-tu: Odłam Żywego Poroża do kosmetycznego rzemiosła. Wartości startowe do poligonu.
- Sekrety dają wzór liny i dodatkowe włókno. Kronika 006 odkrywa pierwszy zwierzęcy szczep i szkic przejścia do 007; brak zależności poziomu/łupu od kolejności.

## 12. Trudności
- Łatwy: pąk 8 s, Rozbieg 1,5 s, murki wytrzymują 2 uderzenia, stała bezpieczna szczelina o szerokości 4 bl.
- Normalny: pąk 5 s, Rozbieg 1,2 s, murek kruszy się raz, dwie trasy losowane.
- Trudny: pąk 4 s, Rozbieg 1,0 s, Wilczyca w G **plus dodatkowy Muflon** z osobnej półki; jego kredowy cień i `entity.goat.prepare_ram` zaczynają się 1 s przed skokiem. Jeden murek nadkruszony od wejścia; wszystkie kontry skalne i odsłony zachowane.

## 13. Wykonanie i plany B
- Wypieczony teren jarów i Mostu przez `place template`; MD trzyma przynęty, odłamy, skrót i checkpoint; BlockEditor ma dwa wcześniej przygotowane stany murków. Wejście z menu, bez komend gracza.
- Trasy Tura: znacznik wariantu instancji, zapis aktywacji C/D i 2–3 stałe punkty ruchu; krata F sprawdza oba różne identyfikatory, H dwa odłamy. Plan B: krótkie zniknięcie w kurzu i pojawienie się na jednym z dwóch widocznych wylotów, bez swobodnego `goto` przez skały. Test na klonie obejmuje C-first, D-first i śmierć przed każdym odłamem.
- Kruszenie: gotowe wersje 2 murków, próba kolizji przed dopuszczeniem. Plan B: stały nadkruszony murek + blokada linii osłony przez hazard odłamków, z identyczną kontrą przesmyku. Blockout 4 archetypów ma sprawdzić 5-blokowy tor dojścia, zasięg Rogów i Ryku w obu szczelinach oraz cały H Magiem i Wojownikiem bez trafienia; jeśli którakolwiek półka daje bezkarny ostrzał, przesunąć ją do zasięgu Ryku przed wdrożeniem. Wynik nie jest jeszcze zmierzony.
- Most: dwa statyczne pokłady i ratunek na sianie; plan B przy problemach z koszem to nieruchoma dolna półka skalna, nadal dwa punkty widzenia i osłony.
- MM 5.11.2: moby od startu z `Despawn: persistent`, bez `randomstroll`, `FollowRange` 10–14, spawn `level=1;os=true`; brak inline warunku celu, nową mechanikę potwierdzić skutkiem na klonie, nie ciszą logu.
- Szacunek: zwykle 3–5 aktywnych mobów, szczyt 7 + Tur + 12 pąków, w WOW 8 display bez AI. Nexo: wrzos w teksturze ≤256 px, poprawna przestrzeń `nexo:`.

## 14. Samokontrola
- Warunek dwóch spotkań liczy **aktywację dwóch różnych przynęt** i dwa zapisane odłamy, nie czas w jarze; po przerwaniu walki gracz wraca do właściwego checkpointu bez utraty zdobytego odłamu.
- Trasa Tura widoczna przed aktywacją: ślady na ziemi, nacięcie na murku i sylwetka na Moście zgadzają się w obu wariantach; żaden znak nie prowadzi w przeciwną stronę.
- Kolizja dużego Tura: fizyczny hitbox mieści się w korytarzu 5 bl., a klinowanie w 3-blokowej szczelinie jest animacją z zatrzymaniem przed ścianą, nie wejściem modelu w blok.
- Pąk przy murku może stać się przynętą, ale ręczna przynęta działa zawsze; brak zabójstw ani miodu z 003 nie blokuje egzaminu.
- Po odłamie barwa czubka poroża zmienia się z białej na fioletową, a następna demonstracja Rogów/Ryku jest wolniejsza o 0,2 s, by gracz zauważył nową geometrię.
- Przejście M ma dwa znaki ratunku na poziomie kosza; skok z mostu jest skrótem z kosztem czasu, nie miękkim zamknięciem w dolinie.
- Otwarta zagroda i stado po bossie to lokalna scena instancji; nie zmieniają światowego stanu pastwisk innych graczy.
- Test kamery: z dolnego kosza gracz widzi poroże i ślad, lecz nie cały wylot szarży; górny pokład daje informację za cenę mniejszej osłony.
- Każdy odcinek ma ślad, przeciwnika, osłonę, decyzję lub kadr; M daje wysokość i taktykę, E zwiastun. Brak pustej łąki i 60 s czekania. Boss bez fal, Tur kontaktowany działaniem gracza.
- Trop jest prawdziwy w obu wariantach; północ na górze mapy i bez fałszywej wskazówki. W walce jeden komunikat: zapach ofiary, reszta dźwięk/kurz. Nagroda ma lokalny materiał i równą moc przy wyborach.
- Różnica względem 003: tutaj łowca ustawia **geometrię szarży**, nie przenosi znaku na rój; względem 009 brak rytmu. Wilk Wrzosowy używa pary i blokady progu, więc nie jest kopią samotnego GAJ_WILK z 001; Muflon spada z półki, a słomiany Kozioł 009 tańczy po ziemi.
- Własna ocena 0–10: fun 8; motyw/zwrot 8; czytelność 8; topologia 8; piękno/WOW 9; fabuła 8; powtarzalność 7; nagroda 7; wykonalność 8; różnorodność 9.
- Ryzyko dla recenzenta X: kierowanie dużym modelem, kolizja mostu i możliwość bezkarnego ostrzału wymagają prób blockoutu; plan B zachowuje te same decyzje.

## 15. Odpowiedź na recenzję (etap 5)
| Poprawka obowiązkowa | Co zmieniono (sekcja) |
|---|---|
| X: brama, C-first/D-first, odłamy i reset | Obie sekwencje krok po kroku, krata F, zapis checkpointu i beat chart, §§1, 3–4, 13. |
| X: pozorny Trudny | Dodatkowy Muflon z własnym cieniem i dźwiękiem w G, §§4, 12. |
| X: bezkarna szczelina/półka | Zasięgi Rogów/Ryku, tor dojścia i kryterium próby obu klas w blockoucie, §§6, 13; testu nie deklarowano jako wykonanego. |
| P7/L1–L4: skala, EXP, czas | Boss 990, elita 248, moby ×1,60, EXP 720/300, H 3 min i wyjście ≤19 min, §§4–6, 11. |
| L5/L6/R1/R2: odrębność i kontrgra | Przynęta po poległym, stały czas, usuwanie i kolejka; kadr Tura na ostańcu i tabela 4 klas, §§0, 6–7. |
