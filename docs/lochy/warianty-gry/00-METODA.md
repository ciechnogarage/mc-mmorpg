# Warianty gry 1–100 — metoda (prowadzący, 2026-10-12)

Powód: w planie 100 v2/v3 sprzedałem zgodność 4 projektantów jako dowód, a nie byli niezależni (wszyscy czytali gotowy
łuk i kolejność krain ze starego planu, nigdy świadomie nie wybraną). Właściciel: „przemyśl to, zanim zrobisz… nie zgaduj”.
Cel: dać właścicielowi WYBÓR między naprawdę różnymi wersjami całej gry, porównanymi uczciwie, zamiast jednej „zgodnej”.

## Błędy, których ta metoda ma nie powtórzyć
1. **Wspólne wejście = fałszywa niezależność.** Projektanci dostają TYLKO czysty pakiet (`wejscie/`), który pisze
   prowadzący: Akt 1, rasy, frakcje, panteon, dwie ścieżki, ograniczenia techniczne. Bez starej listy krain, bez
   dotychczasowych łuków, bez pamięci projektu. Codex pracuje w osobnym katalogu z samym pakietem; agent Claude jest
   NOWY (nie ten, który pisał łuk „Echa” — on jest skażony własnym pomysłem).
2. **Podobny gust trzech AI.** Każdy projektant startuje z INNEGO punktu (soczewki), żeby warianty nie zbiegły się
   z przyzwyczajenia: X — gracz (krzywa emocji i mistrzostwa), Y — świat (geografia, dzieje, cywilizacje), C' — tożsamość
   serwera (co wyróżni go na tle Wynncraft/Hypixel/Monumenta, co gracze opowiedzą). Każdy daje 2 warianty, które różnią się
   między sobą zasadniczo (inne pytanie przewodnie, inny zestaw i kolejność krain).
3. **Ocena na oko.** Kryteria są ustalone TERAZ, przed propozycjami (niżej), i znane projektantom.
4. **Autor ocenia sam siebie.** Ocena jest krzyżowa i ślepa: warianty dostają losowe litery, każdy oceniający dostaje
   zestaw bez swoich. Dotychczasowy plan (v3) wchodzi jako jeden z wariantów w tym samym formacie, opisany neutralnie
   — porównanie, a nie z góry przyjęty zwycięzca.
5. **Zgodność jako dowód.** Wynik to tabela ocen z uzasadnieniami i „wadą krytyczną” każdego wariantu — POMOC w wyborze
   właściciela, nie werdykt. Prowadzący dopisuje swoją ocenę osobno i jawnie jako opinię.
6. **Przeciek starego planu.** Po oddaniu wariantów prowadzący sprawdza (grep) nazwy starych krain i praw; przeciek =
   zgłoszony przy wariancie.
Czego ta metoda NIE daje: testu na prawdziwych graczach. Każdy wariant ma 2-minutowy opis dla gracza, żeby właściciel mógł
taki test zrobić (np. 3–5 osób: „którą grę chcesz grać dalej?”).

## Kryteria (1–5 każde, z uzasadnieniem)
K1 Narastanie — skala, stawka i dziwność rosną; gracz czuje, że idzie dalej, nie w bok.
K2 Zmienność — sąsiednie akty różnią się tonem, emocją, formą gry; żadne dwa nie są tym samym w innym kolorze.
K3 Przyczynowość — każdy akt wynika z poprzedniego; zamiana sąsiednich psuje historię.
K4 Finał — rozwiązuje pytanie postawione na początku; nagroda za 100 poziomów.
K5 Kanon — używa ras, frakcji, panteonu i Aktu 1 bez sprzeczności; orkowie i nieumarli (grywalni) nie są „hordą zła”.
K6 Grywalność krain — prawo świata każdego aktu działa w lochach i na otwartej wyspie, daje inną formę gry.
K7 Hak — czy gracz opowie o tym znajomemu; czym ten świat różni się od innych serwerów.
K8 Koszt — wykonalność na naszym stosie i w realnym tempie produkcji.
Plus: „wada krytyczna” (jedno zdanie) i „co by trzeba było zmienić, żeby wariant był najlepszy”.

## Kroki
1. Prowadzący: czysty pakiet `wejscie/` + szablon wariantu.
2. X, Y, C' (równolegle, osobno): po 2 warianty → `propozycje/` (prowadzący przenosi).
3. Prowadzący: kontrola przecieku i formatu; plan v3 przepisany do szablonu jako wariant bazowy; anonimizacja (litery).
4. Ocena krzyżowa ślepa (każdy oceniający bez swoich) + osobna ocena prowadzącego.
5. Strona dla właściciela: 3 najlepsze + bazowy, w tym samym formacie, z ocenami, wadami i opisem dla gracza. Wybiera właściciel.

## Zastrzeżenia wykryte w trakcie (jawne)
- Agent Claude dostał automatycznie indeks pamięci projektu (jednolinijkowe opisy, m.in. „Powracający / Cienie Poległych”).
  Jego wariant „Rok Bez Pogrzebu” zadaje pytanie bliskie staremu planowi — oznaczony jako MOŻLIWIE SKAŻONY.
- Dwa Codexy to ten sam model: w osobnych katalogach i tak zbiegły się nazwami (Ujście Siedmiu Barek/Ramion, Miasto
  Czterech Bram). Traktuję je jako jeden gust, nie dwa niezależne głosy; dotyczy też ich ocen.
