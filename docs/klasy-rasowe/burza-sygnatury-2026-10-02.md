## A. Werdykt

Nie, skille różnią się rasowo także u ninja, maga i akolity: Upiór przyciąga cel zamiast doskakiwać, a akolita przywołuje innego sługę dla każdej rasy. Właściciel trafnie widzi jednak główny problem: te różnice rzadko zmieniają **sposób prowadzenia walki**, a broń rasowa nie jest warunkiem ani narzędziem żadnego z przeczytanych skilli. Nawet u wojownika `Rozpłatanie` dostaje parametr `wariant`, lecz go nie używa. Akolita jest dodatkowo niespójny z założeniem czterech skilli: do poziomu 10 włącznie ma obecnie sześć odblokowań, z czego część pochodzi ze starego szkieletu.

## B. Proponowana zasada

- **Cztery skille na klasę, odblokowywane na poziomach 1, 3, 5 i 7.** Pierwszy jest sygnaturą z bronią rasową; trzy pozostałe zachowują funkcje wspólne dla archetypu.
- Sygnatura używa **jednej wspólnej mechaniki na archetyp**: wojownik doskakuje, ninja trafia z dystansu, mag rzuca pocisk, akolita przywołuje sługę. `wariant` zmienia przebieg i skutek; nazwa oraz opis pokazują to graczowi.
- Broń rasowa musi być wyposażona do użycia sygnatury, także w drugiej ręce, gdy chodzi o tarczę lub grimuar. Pozostałe trzy skille działają z bronią archetypu. Brak wymaganej broni daje jasny komunikat.
  **ODRZUCONE przy wdrożeniu (etap 3, 2026-10-02):** broń rasowa NIE jest wymagana — sygnatura działa zawsze, broń w ręce daje premię `◆` (`?bronRasowa{typ=..}` -> `caster.kr_premia`).
- Wariant pozostałych skilli zmienia **zasięg, pozycję, cel, moment użycia lub nagrodę za dobre zagranie**. Sam dodatkowy status, leczenie albo procent obrażeń nie wystarcza za styl.
- Zasoby pozostają wspólne: Szał, Skupienie i Znamiona, Mana oraz Dusze i limit przywołańców. Różne style mają podobną skuteczność, ale inne ryzyko i tempo.
- Wiary i frakcje dostarczają języka oraz wyglądu skilli. Nie przypisywałbym działania skilla do konkretnego patrona, bo patron jest wyborem postaci.

W tabeli „pozostałe trzy” oznaczają: **wojownik** — Rozpłatanie, Okrzyk, Gniew; **ninja** — Cięcie, Krok, Egzekucja; **mag** — Bariera, Wybuch, Mrugnięcie; **akolita** — Żniwo, Opieka, Rozkaz. Opieka rozwija obecne leczenie, a Rozkaz steruje sługami.

## C. Szesnaście klas

| Klasa | Tożsamość | Sygnatura z bronią rasową | Styl pozostałych trzech skilli |
|---|---|---|---|
| **Miecz Korony** | Utrzymuje linię i wybiera przeciwnika, którego drużyna ma zatrzymać. | **Pchnięcie Korony — halabarda:** doskok kończy się pchnięciem na zasięgu drzewca i zatrzymaniem celu. | Rozpłatanie tnie wąską linię z dystansu; Okrzyk osłania stojących razem; Gniew kończy pojedynek z zatrzymanym celem. |
| **Klinga Gaju** | Wygrywa ustawieniem za plecami przeciwnika i unikaniem odpowiedzi. | **Przelot Gaju — katana:** doskok przechodzi przez cel i otwiera drogę do ataku od tyłu. | Rozpłatanie jest szybkim cięciem po ruchu; Okrzyk daje drużynie ruch; Gniew nagradza trafienie z boku lub od tyłu. |
| **Wódz Wojny** | Rozbija grupę i utrzymuje Szał, walcząc w jej środku. | **Natarcie Wodza — topór:** doskok uderza w pierwszy szereg i otwiera szerokie cięcie. | Rozpłatanie obejmuje więcej celów; Okrzyk najlepiej zasila Szał w zwarciu; Gniew jest potężny, lecz zostawia okno na kontratak. |
| **Rycerz Śmierci** | Idzie przez obrażenia i odzyskuje siły, gdy utrzyma przeciwników blisko. | **Marsz z Krypty — miecz dwuręczny:** krótszy, wolniejszy doskok kończy się szerokim cięciem i niewielkim wysysaniem życia. | Rozpłatanie trafia bliską grupę; Okrzyk pomaga przetrwać jej odpowiedź; Gniew leczy za rzeczywiście trafione cele. |
| **Lis Zaułków** | Oznacza ofiarę z zasadzki i zmienia pozycję przed jej dobiciem. | **Bełt Listu Gończego — kusza:** celny strzał daje Znamię; trafienie przygotowanego celu nagradza precyzję. | Cięcie broni w zwarciu; Krok służy do odejścia na pozycję strzelecką; Egzekucja zużywa Znamiona krótkim strzałem. |
| **Szept Kniei** | Buduje Znamiona z daleka i karze wrogów, którzy podejdą złą ścieżką. | **Strzała z Korony — łuk długi:** dłużej przygotowany strzał z dużego zasięgu nakłada Znamię. | Cięcie spowalnia napastnika; Krok tworzy dystans; Egzekucja pozostaje dystansowa, lecz wymaga bezpiecznego ustawienia. |
| **Kieł Stepu** | Ściga jeden cel, przeplatając rzut i walkę na długość włóczni. | **Kościany Grot — włócznia:** rzut oznacza cel i pozwala ruszyć jego śladem. | Cięcie sięga dalej, ale w węższym łuku; Krok agresywnie skraca dystans; Egzekucja odpycha trafioną ofiarę. |
| **Upiór** | Trzyma ofiarę blisko, odbierając jej możliwość ucieczki. | **Pętla Grobu — łańcuch:** uderzenie z dystansu nakłada Znamię i przyciąga jeden cel. | Cięcie dokłada Znamiona w zwarciu; Krok przyciąga zamiast teleportować; Egzekucja wysysa życie z unieruchomionej ofiary. |
| **Magister Wieży** | Przygotowuje miejsce wybuchu i zmusza przeciwnika do decyzji: odejść czy przyjąć czar. | **Pieczęć Wieży — grimuar:** pocisk stawia znak, który następny Wybuch może wyzwolić. | Bariera chroni podczas przygotowania; Wybuch nagradza trafne ustawienie pieczęci; Mrugnięcie pozwala zyskać linię rzutu. |
| **Tkacz Arkanów** | Prowadzi walkę liniami zaklęć i stale zmienia kąt ataku. | **Nić Arkanów — kostur dwuręczny:** pocisk przechodzi przez ustawione w jednej linii cele. | Bariera pomaga w ruchu; Wybuch obejmuje wąski pas; Mrugnięcie zmienia kąt i otwiera następną linię. |
| **Szaman** | Zajmuje fragment pola bitwy i walczy o utrzymanie w nim przeciwników. | **Oko Burzy — totem:** pocisk zakotwicza krótko działający punkt burzy w miejscu trafienia. | Bariera chroni przy totemie; Wybuch uderza wokół punktu; Mrugnięcie służy do obrony lub opuszczenia zajętego obszaru. |
| **Lisz** | Wiąże ofiarę czarem i wygrywa dłuższą wymianę. | **Splot Duszy — filakterium:** pocisk łączy Lisa z jednym celem; utrzymanie więzi daje niewielkie wysysanie życia. | Bariera kupuje czas dla więzi; Wybuch karze cel za pozostanie w jej zasięgu; Mrugnięcie pozwala więź podtrzymać. |
| **Nosiciel Relikwii** | Chroni wybranego sojusznika przez ustawienie obrońcy. | **Straż Relikwii — tarcza:** wezwanie stawia sługę przy sojuszniku, by przejął najbliższy atak. | Żniwo zamienia sługę w osłonę i leczenie; Opieka wzmacnia zagrożonego; Rozkaz przestawia obrońcę. |
| **Wyrocznia** | Prowadzi lekkiego ducha między rannymi i przewiduje, gdzie potrzebna będzie pomoc. | **Sierp Księżyca — sierp:** cięcie wskazuje miejsce, do którego przybywa leczący ognik. | Żniwo uwalnia puls leczenia; Opieka działa stopniowo; Rozkaz szybko przenosi ognik do nowego celu. |
| **Kapłan Krwi** | Pomaga drużynie naciskiem na wroga i ofiarą z własnego sługi. | **Przodek Młota — młot dwuręczny:** uderzenie w ziemię wzywa ducha do walki w tym miejscu. | Żniwo staje się ofensywnym wybuchem i oddaje Duszę; Opieka nagradza atakowanie wspólnego celu; Rozkaz kieruje ducha do natarcia. |
| **Pasterz Ciszy** | Osacza wroga sługami i odzyskuje siły z ich żniwa. | **Kosiarz Ciszy — kosa:** cięcie wskazuje ofiarę, przy której pojawia się sługa kurhanu. | Żniwo wysysa życie z osaczonego celu; Opieka przekazuje odzyskane życie sojusznikowi; Rozkaz utrzymuje sługi przy ofierze. |

## D. Co zrobić najpierw

1. **Pętla Grobu:** łańcuch, przyciągnięcie i Znamiona od razu pokażą, że Upiór walczy inaczej niż pozostali ninja.
2. **Oko Burzy:** totem jako punkt, wokół którego współpracują Pocisk, Bariera i Wybuch, pokaże różnicę całej rotacji.
3. **Straż Relikwii:** tarcza i ustawianie obrońcy dadzą akolicie czytelną rolę w drużynie.
4. **Pieczęć Wieży:** połączenie dwóch istniejących typów czarów da Magistrowi planowanie zamiast kolejnego efektu trafienia.
5. **Przelot Gaju:** przejście przez cel i nagroda za pozycję sprawią, że elfia szybkość będzie decyzją gracza.

**Trzy koszty i ryzyka:**

- Trzeba sprawdzić wyposażenie obu rąk oraz działanie sygnatur z przedmiotami MMOItems; same biegłości broni tego nie zapewniają.
- Najwięcej strojenia wymagają przyciąganie, długi zasięg, leczenie i słudzy. Każda przewaga potrzebuje czytelnego ograniczenia.
- Lore wymaga decyzji: [panteon](/home/dev/Desktop/Przemek/MC/docs/wiary/panteon.md) nazywa Vharuna wrogiem wszystkich nieumarłych, a opis Pasterza Ciszy czyni go kapłanem „Tego, Który Milczy”.

## E. Co zachować i co zmienić

- **Zachować** obecne zasoby, Znamiona na celu, warunki ataku i wsparcia, bazowe obrażenia oraz podział przez `wariant`. To jest dobra wspólna podstawa.
- **Wojownik:** zostawić Szarżę, Rozpłatanie, Okrzyk i Gniew jako cztery funkcje. Szarżę uczynić sygnaturą broni; nadać rzeczywisty wariant Rozpłataniu oraz różne zastosowania Gniewu dla elfa i człowieka.
- **Ninja:** przenieść Rzut do pierwszego odblokowania i zmienić go w cztery sygnatury broni. Zachować Cięcie, Krok i Egzekucję; wariant Egzekucji powinien zmieniać również pozycję i sposób domknięcia walki, nie tylko mnożnik obrażeń.
- **Mag:** zachować Pocisk, Barierę, Wybuch i Mrugnięcie. Pocisk związać z bronią rasową, a trzy następne czary z jego skutkiem. Obecne Mrugnięcie ma dla elfa krótsze odnowienie, lecz sam parametr `wariant` nie zmienia jego działania.
- **Akolita:** zachować Wezwanie, Żniwo i rasowych sług, ale wyrównać ich role oraz związać wezwanie z bronią. Z obecnego `MINOR_HEALINGS` zrobić wspólną Opiekę, dodać Rozkaz jako czwarty skill; `WEAKEN`, `GREATER_HEALINGS` i `DEEP_WOUND` odsunąć poza zestaw Aktu 1. Wszystkie cztery skille powinny mieć świadomie zaprojektowany wariant.
- Zmiany projektować w [generatorze](/home/dev/Desktop/Przemek/MC/tools/klasy-rasowe/generuj_klasy.py) i [skillach MythicLib](/opt/docker/mc/data/world1/plugins/MythicLib/skill/), bo pliki klas MMOCore są generowane. To propozycja projektu, bez zmian w plikach.