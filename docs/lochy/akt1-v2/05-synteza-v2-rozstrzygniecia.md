# Synteza v2 — rozstrzygnięcia sporu (prowadzący, 2026-10-03)

Głosy: C PRZEPUSZCZA (W1–W3), X BLOKUJE (6 zarzutów), Y BLOKUJE (6 zarzutów). Wszystkie trzy blokady przyjmują ramę C.
Poniższe rozstrzygnięcia zdejmują blokady — każda karta szczegółowa MUSI je spełniać. Recenzent sprawdza je punkt po punkcie.

## R1. Prawo aktu: KIEŁKI (3 głosy: X, C, P; przeciw: Y — Odzew)
Uzasadnienie przeciw Odzewowi: wymaga szwów w każdej mapie (drugi dominujący motyw mapy — X §5). Zarzut Y („sprzątanie po
każdej śmierci”) przyjęty jako ograniczenie projektowe: kiełek NIE może być w żadnym lochu wyłącznie obowiązkiem sprzątania.
**Jedna reguła, bez wyjątków (X 3.1, Y 1):**
1. Każdy poległy (wróg, gracz) zostawia IDENTYCZNY pąk: ten sam model/cząsteczka/dźwięk w całym akcie.
2. **Trafienie (dowolne: LPM, PPM, zaklęcie, pocisk, przywołaniec Akolity, Chowaniec) ZAWSZE usuwa pąk** („zdeptany”).
   Zdeptanie samo w sobie nic nie daje — wyjątek: pąk poległego gracza (pkt 4).
3. **Pozostawiony pąk po stałym czasie (5 s; Łatwy 8 s; Trudny 4 s) DOJRZEWA** w lokalny skutek lochu. Skutek bywa zły
   (wróg/hazard), dobry (osłona, narzędzie) albo dwuznaczny — to decyzja taktyczna. Dojrzewanie ma jednolitą zapowiedź
   (pąk pęcznieje i świeci ostatnią 1 s).
4. Pąk poległego GRACZA nigdy nie wzmacnia wrogów: dojrzewa w Cierń (przeszkoda w miejscu śmierci); kto go zdepcze,
   dostaje Płatek ochrony (C W1).
5. Limit 12 pąków na instancję: nowy wypiera najstarszy, który widocznie usycha (1 s szarzenia + dźwięk) (C W2).
   Beat chart: max 6 zabójstw w 10 s w jednym starciu.
6. Chowaniec i przywołańce depczą przechodząc obok (C/Y) — Akolita nie płaci za archetyp.
7. Wykonanie: `~onDeath` → summon PĄK (HP 1); dojrzewanie w skillu PĄKA (`delay`), nie w funkcji MD. Śmierć gracza:
   osobny, przetestowany przepływ (Y 1) — do tego czasu karta ma plan B (bez pąka gracza).

## R2. Kontra solo dla 4 archetypów (X 3.2, Y 2, Y 6)
Każda mechanika „nietykalności/odsłony” musi dać się wywołać KAŻDYM źródłem obrażeń (wręcz, zaklęcie, pocisk, przywołaniec)
i nie może zależeć od pingu. Karta ma tabelę „Wojownik / Ninja / Mag / Akolita bez specjalizacji, solo” dla bossa i elity.
- 003: znamię przenosi trafienie (dowolne) w widoczny Kwiat Pyłku w długim zapowiedzianym oknie; Królowa ma okno obrażeń
  po chybionej szarży; rój ma pokazany tor i wnęki.
- 009: rytm steruje TYLKO zagrożeniami; tancerz korowodu jest podatny w swojej pauzie na „4” (okno 15 ticków, dowolne
  trafienie) (C); zero `variableequals world.takt` jako bramy postępu (Y).

## R3. Rozstrzygnięcia per loch
- 001: zwrot = celowo pozwolić wyrosnąć (Korzeń-Taran z pąka Korowca otwiera Bramę Wzgórza; obok Pniaczki też dojrzeją) (C/X).
  Bez latarni. Siewca = roślina-dmuchawiec.
- 002: koszt osuszania (Y): mniej studni = mniej wynurzeń, ale silniejszy przewidywalny skurcz. Dzwon = jednorazowe
  oczyszczenie. Bąk (ptak) zamiast Kościeja/Trzcinnika. Zwrot C (zalana Grobla → powrót dnem przez Kokoniarnię).
- 003: R2. Woszczarka zamyka przejścia woskiem (nie spowalnia).
- 004: głośna droga pełnoprawna; bycie widzianym kosztuje (wrony, Kosiarze), nie blokuje. Pola wykrywania Strachów jawne (X).
- 005: zrost z X 004 (para potworów spiętych nicią, przecięcie dowolnym trafieniem) jako nauka bossa; sadzonki losowane
  (2 z 4) i działają W WALCE; tabliczki tylko sekret; 2 warianty areny jako plan B.
- 006: Wąwozy Tura: 6–9 ciasnych odcinków; 2 losowane trasy Tura; zwrot TERENOWY (Tur rozbija murki, klinuje poroże
  w przesmykach); Most Sznurów (Y) jako przeprawa; Muflon zamiast Kruka; odłamane poroże zmienia atak, nie kasuje egzaminu;
  kontakt z Turem gwarantowany przez działanie gracza, nie timer.
- 007: warunek: próba `minecraft:scale` na klonie (kolizje, zasięg ciosu/zaklęć, Chowaniec, kamera, powrót rozmiaru po
  śmierci/wyjściu) — robi prowadzący. Plan B: ogród ×4. Losowane tunele 2 z 3, faza 2 Ropuchy losowana. Kosarz zamiast Biedronki.
  Nie wymagać wchodzenia na model bossa (Y 4) — brodawka dostępna też z ziemi w oknie.
- 008: chleby 2 z 4 losowane; Zaszczepienie NIEODWRACALNE w przebiegu (bez stacji); zwrot: Matka leczy się trafiając
  Ciebie proporcjonalnie do stosów; korzenie ze stosów da się przeciąć własnym atakiem; tabela liczb bez chleba dla
  4 archetypów; ciężar/worki z X 006 na arenie; bochen widoczny wcześniej w Stolicy (Y); wybór dotyczy zapasów TEJ
  instancji + świadectwo w Mapie, nie globalnego głodu.
- 009: R2; skład C (Korowodnicy jedyny humanoid, Bęben-Purchawa, Wianek, Kozioł Dożynkowy, elita Przepiórka); większość
  wsi to nieruchome tło; w finale widać ocalałych.
- 010: Matecznik; zrost z X jako przycinanie W WALCE; Darnik odcięty, nie zabity; walka jest kulminacją (C: „egzamin to
  walka, nie łamigłówka”); pokazać, kto odpowiada za plony potem (X).
- Wątek (C W3): każde odkrycie podane w lochu w całości (1 kwestia NPC/obiekt), Kronika na Mapie Granicy wg numeru lochu.
- Moc ≠ wiedza (Y): poziom/łup nie zależą od kolejności ani wyborów moralnych.

## R4. Podział etapu 3 (projekty szczegółowe) i recenzje krzyżowe
| Loch | Projektuje | Recenzuje (może BLOKOWAĆ) |
|---|---|---|
| 001, 004, 007 | C | Y |
| 002, 005, 008 | X | C |
| 003, 006, 009 | Y | X |
| 010 | prowadzący | X, Y, C |
Prowadzący recenzuje wszystko i prowadzi przegląd spójności całej dziesiątki.

## R5. Karta szczegółowa — plik `10-loch-0NN-<nazwa>.md`, sekcje obowiązkowe
0. Nagłówek: poziom, czas (Normalny solo), fantazja, emocja, motyw 1 zdaniem, skutek dojrzałego pąka w tym lochu.
1. Kishōtenketsu motywu: wstęp / rozwinięcie / zwrot / egzamin — gdzie (sala) i jak gracz to rozumie BEZ tekstu.
2. Historia środowiskiem + 1 odkrycie wątku (samodzielne) + max 3 kwestie NPC (cytaty).
3. Mapa ASCII (skala, północ na górze, wymiary sal w blokach, ognisko, skrót, pętla, weenie) + opis pionowości.
4. Beat chart: tabela sala | rola | min | skład (z uzasadnieniem) | start starcia | tempo | poza walką.
5. Bestiariusz: tabela mob | rola | wygląd zastępczy vanilla + kandydat modelu | HP/obrażenia względem Gaj v8 (np. „HP jak
   Korzeniak v5”) | zapowiedź (dźwięk Minecrafta z nazwy + czas) | atak karzący | kontra | nieuważny vs uważny.
6. Elita i Boss: funkcja → haczyk → historia → wygląd (sylwetka, słaby punkt); fazy z progami HP; 4–9 zdolności w tabeli
   (zapowiedź, czas, cel zamknięty?, kara, okno odsłony); arena (wymiary, osłony, co się zmienia); anty-kitowanie;
   tabela R2 dla 4 archetypów.
7. WOW: minuta, miejsce, co widać (kadr), jak zbudowane (bloki/display/cząsteczki).
8. Paleta: 6–8 bloków dominujących, pora/pogoda, światło, dźwięk tła.
9. Sekrety (3–5) z wskazówką w świecie; wybór z konsekwencją.
10. Powtarzalność: co losowane i jak zmienia taktykę; ocena S–C (kryteria).
11. Nagroda: EXP postaci, rodzina przedmiotów (nazwy robocze), materiał profesji, tylko-tu, sekrety, drużyna.
12. Trudności: Łatwy/Normalny/Trudny — różnice mechaniczne.
13. Wykonanie: MythicMobs 5.11.2 darmowy / MD / BlockEditor / place template / Nexo — 3 najtrudniejsze elementy + plan B;
    szacunek encji naraz.
14. Samokontrola: lista „suche i bez sensu” z briefu §4 odhaczona; ocena własna 0–10 w 10 kryteriach bramki.
Długość: 150–260 linii. Bez lania wody: tabele, liczby, konkrety.
