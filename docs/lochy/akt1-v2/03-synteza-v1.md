# Synteza v1 (prowadzący) — do ZABLOKOWANIA albo przepuszczenia przez X, Y, C

Porównanie czterech propozycji (P, X, Y, C) wg bramki briefu §4, ocena prowadzącego 0–10:

| Kryterium | P | X | Y | C |
|---|---|---|---|---|
| Różnorodność czasowników gracza między lochami | 6 | 4 | 4 | 9 |
| Piękno/WOW jako obraz | 5 | 6 | 6 | 9 |
| Potwory i groza (fantasy, nie „usługi komunalne”) | 6 | 3 | 3 | 8 |
| Spójność wątku aktu | 6 | 8 | 8 | 8 |
| Prawo aktu: fizyczne, czytelne, w walce | 5 (Rozrost) | 5 (Sok) | 8 (Odzew) | 8 (Kiełki) |
| Tożsamość vs akty 2–10 | 6 | 8 | 8 | 9 (ziemia uprawna ≠ las Aktu 6) |
| Wykonalność | 7 | 7 | 6 | 6 (skala, rytm) |

**Rama = C.** Uzasadnienie: jedyna propozycja, w której każdy loch zmienia to, CO gracz robi (skrada się, maleje,
tańczy, poluje, sadzi, kusi go chleb), a nie tylko to, co przestawia. X i Y są spójniejsze moralnie, ale 20 ich lochów
to warianty „przestaw przepływ” z przeciwnikami-pracownikami (recenzja `02-recenzja-P-etap1.md`).

## Kanon Aktu 1 (wersja do zatwierdzenia)
- **Kraina**: Zielona Granica — Kraj Sadów. Ziemia UPRAWNA (sady, łany, pasieki, szklarnie, spichlerze, pastwiska,
  malowane wsie), słowiański folklor rolniczy, dzień i złota godzina. Żadnej nocy-księżyca, morza, piasku, mrozu,
  ognia/lawy, podziemnych kryształów, lasu-puszczy (to Akt 6).
- **Zagrożenie**: Szczep — żywa szczepka z legendarnego Pierwszego Korzenia (aluzja do Ordhuna, bez wiązania wiarą),
  zaszczepiona w Głodnym Roku przez Bractwo Ogrodników mistrza Ilwena Darnika. Wyspa przeżyła; Szczep nie przestał rosnąć.
  Nie jest zły — jest głodny wzrostu. Wrogowie: zaszczepione zwierzęta i owady, rośliny-twory, zaszczepieni (na wpół
  roślinni) członkowie Bractwa różnych ras. **Twarda zasada (z mojej blokady X/Y): w lochu max 1 typ humanoida,
  i jest widocznie „zaszczepiony”** (pędy, kora, kwiaty z ciała) — to nie cywile w fartuchach.
- **Prawo aktu: „Co tu pada — kiełkuje.”** (C) Każdy poległy zostawia Kiełek (stały dźwięk + cząsteczka w całym akcie);
  zdeptany 1 trafieniem znika, niezdeptany po 5 s (Łatwy 8 s, Trudny 4 s) wyrasta w coś zależnego od lochu.
  Limit 12 kiełków na instancję (wydajność). Y-owy „Odzew” odrzucony jako drugie prawo (dwa prawa = nieczytelne),
  ale jego zasada **symetrii** wchodzi: kiełki z poległego GRACZA też wyrastają (gracz widzi skutek własnej śmierci).
- **Wątek**: odkrycia C (001 zwłoki Ogrodnika → … → 008 chleb Stolicy pochodzi ze Szczepu → 009 wieś wybrała Szczep
  → 010 Matecznik). Nośnik: Mapa Granicy w Nexusie (menu per gracz, uprawnienie za loch) + ocalały NPC wraca do Stolicy.
  Każdy loch jest pełną historią sam (otwiera poziom, nie poprzedni loch) — odkrycia muszą mieć sens w dowolnej kolejności
  (X/Y: „dowolna kolejność pomija zależne ekspozycje”).
- **Finał (do decyzji zespołu)**: Darnik NIE ginie z ręki gracza. Gracz przycina Matecznik (Granica żyje, przestaje się
  szerzyć); Darnik zostaje odcięty od drzewa i wraca do Stolicy jako stary, złamany człowiek (Y: „porażka poznawcza,
  nie egzekucja”). Haczyk na Akt 2: sadzonka przypłynęła statkiem z Archipelagu.
- **Łuk emocji**: zachwyt z rysą → duszność i ulga → gwar → groza skradania → ciekawość → adrenalina i bycie ofiarą
  → bezbronny zachwyt → pokusa i wina → niesamowitość → katharsis. (10 różnych nut — wymóg zrealizowany.)

## Drabina v1 i POPRAWKI OBOWIĄZKOWE do etapu szczegółowego
| # | Loch | Czasownik / motyw | Obowiązkowe poprawki (źródło) |
|---|---|---|---|
| 001 | Kwietna Polana | deptać czy zostawić (lekcja prawa) | Prawo jako motyw 001 — DOZWOLONE (pierwszy loch Zeldy uczy narzędzia), ale zwrot musi być mocny. Latarnie usunięte (Akt 4). Teren nowy, ciasny (widły Y). Zachować GAJ_* i nastroje dnia. |
| 002 | Bagna Zgniłych Korzeni | poziom wody | Zachować v0.2. Dzwon: jednorazowe oczyszczenie, NIE odporność (X, Y). Zwrot C (zalana Grobla). Kościej → Trzcinnik. Finał: pieczęć Pasieki, nie wieża Archiwum. |
| 003 | Pasieka Królowej Barci | znamię roju | Rój = nieśmiertelny hazard; MUSI mieć limit (1 rój, widoczny) i zapowiedź; nie może być DPS-em bez kontry dla maga/ninja na dystansie. |
| 004 | Łany Strachów | ukrycie w łanie | Skradanie NIE może być wymogiem (Y o 007 X): głośna droga pełnoprawna, tylko trudniejsza. Burzowe niebo bez nocy. Test `blocktype` dla bloku Nexo. |
| 005 | Oranżeria Darnika | szczepienie sadzonek | Ryzyko quizu: sadzonki muszą działać W WALCE (Rosiczka trzyma moba, Purchawa przerywa), nie tylko jako klucze. |
| 006 | Wąwozy Tura (zmiana nazwy) | łowy: trop, przynęta, ofiara | Blokada C na „otwarte łowisko”: zamiast łąki SIEĆ WĄWOZÓW (Y: Przesmyk Stad, wąskie przejścia) z 3 ciasnymi legowiskami; Tur chodzi stałymi trasami, ślady pokazują gdzie; zero pustych przestrzeni. Wątek migracji stad z Y jako WOW. |
| 007 | Grządki Maluczkich | zmiana skali | Warunek przyjęcia: próba atrybutu `minecraft:scale` na klonie z pełnym stosem (prowadzący robi to sam w etapie 3). Plan B: ogród ×4. |
| 008 | Spichlerz Głodnego Roku | pokusa (Zaszczepienie) | Pokusa musi być realna liczbowo (bez chleba walka trudna, ale wykonalna — ocena S). Wybór „zalać spichlerz” konsekwencja tylko w atlasie/NPC, nie w sile. |
| 009 | Dożynki w Malowanej Wsi | rytm | Rytm steruje ZAGROŻENIAMI (strefy na takt, okna przeciwników); bonus za cios na takt z oknem ≥ 10 ticków albo wypada (ping). Ton: niesamowite, nie gore. |
| 010 | Matecznik | przycinanie / egzamin z prawa | Prowadzi prowadzący. Egzamin z prawa + krewniacy rodzin, BEZ kolażu żywiołów. |

Przeszczepy z X/Y/P do rozważenia przez projektantów szczegółowych (nie obowiązek):
- X 006 Tarasy (ciężar zmienia wysokość platform) — jako element areny 008 (zsypy/windy)?
- Y 006 Most Sznurów (dwa wiszące pokłady) — jako fragment 006 Wąwozów (przeprawa nad wąwozem).
- P 004 Kamieniołom (hałas budzi) — NIE (dubluje skradanie 004).
- X/Y atlas po każdym lochu — PRZYJĘTE jako Mapa Granicy.

## Korekta planu 100 (zbiorczo z A/E wszystkich czterech — do dokumentu dla właściciela)
1. Zostaje: 10 aktów × poziomy, każdy akt = kraina + JEDNO prawo świata. Wypadają kolumny K2–K9.
2. Stałe tylko krawędzie aktu: pierwszy loch uczy prawa, ostatni jest egzaminem z prawa i historii aktu.
3. Globalny rejestr: mechanika-motyw raz na grę; haczyk bossa raz na grę; topologia max raz na akt.
4. „Budżet obietnic”: pełny motyw należy do swojego aktu (mróz → Akt 5 itd.).
5. Macierz pokrycia czasowników walki per akt zamiast kolumn-lekcji.
6. Nieumarli/orkowie nie są rodziną wrogów żadnego aktu (poza konfliktem frakcji Aktu 7 — do przemyślenia).
7. Kolizje do przepisania: 045 lawina, 073 kapłani ognia, 029 mumie, K4 ×8 „odbite pociski”, 058 Drzewo Matka,
   011/019 „Topielcy/Utopcy” vs Topielica 002, Akt 6 jako las vs Akt 1 (różnica: dzikie srebrne knieje vs uprawy).
8. Liczba 100 nie jest święta (C E8): decyzja właściciela, czy od Aktu 4 6–8 lochów + tańsze formy na wyspie.
