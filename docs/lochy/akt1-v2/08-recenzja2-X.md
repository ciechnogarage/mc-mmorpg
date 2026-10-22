# Etap 6 — recenzja X, druga runda (2026-10-03)

Zakres: wyłącznie obowiązkowe poprawki z `06-recenzja-X.md`, regresje w 003/006/009 i zarzuty do 010 v2; wiążą `05` R1–R5, `07` L1–L7 i `06-recenzja-P`. Dowód „L” oznacza linię bieżącej karty. To ocena projektu na papierze, nie wynik poligonu. Kolejność ocen: fun / mechanika i zwrot / czytelność / topologia / WOW / fabuła / powtarzalność / nagroda / wykonalność / różnorodność. Bramka: dowolne <7 oznacza BLOKUJĘ, wyłącznie przy rzeczywistej szkodzie dla gry lub wykonalności.

## 003 — Pasieka Królowej Barci

| Poprawka | Stan i dowód |
|---|---|
| Skrót F→S→A przed H, oba porządki C/D | **Zrealizowana:** mapa L33–40, otwarcie S L40, oba porządki L56. |
| Ruchomy kwiat, każde źródło i synchronizacja transferu | **Częściowo:** stały naziemny mob-hitbox, 3 s, jeden transfer przed nowym torem i plan testu L17–18, 133, 139–140. Pomiaru sześciu źródeł na klonie **nie ma** (L133, 151, 157); własny cios Akolity i 3 s odsłony po chybieniu pozostawiają grywalną kontrę solo L89, 95. To warunek wdrożenia, nie dowód wykonania. |
| Trzy grafy wosku i oznaczenie w A | **Zrealizowana:** I–III, drogi do C/D/F/S oraz trzy znaki w A L115; rozbijalność L40. |
| Regresje / wiążące P, L | Nie znaleziono starego brakującego skrótu, kadru Królowej za szybą ani wyboru łaska/materiał: L35–40, 97–100, 112. Skala 660/165, EXP 360/150 L72, 78, 120; Larwa i R1 L8–11. |

**Oceny:** 8 / 8 / 8 / 8 / 9 / 8 / 7 / 8 / 7 / 9. **WERDYKT: PRZEPUSZCZAM projekt; pomiar R2 przed wdrożeniem.**

## 006 — Wąwozy Tura

| Poprawka | Stan i dowód |
|---|---|
| C-first/D-first, dwie przynęty, odłamy, F/H i reset | **Zrealizowana:** dwie różne aktywacje L15–16, krata F/H L40–42, obie sekwencje L58 i checkpointy po śmierci L59; beat chart L50–57. |
| Trudny: realna dodatkowa trudność | **Zrealizowana:** Wilczyca z dodatkowym Muflonem, osobny znak i dźwięk L56, 133; Wilczyca pozostaje na Normalnym L56. |
| Bezkarna półka/szczelina | **Częściowo:** zasięg Rogów/Ryku, dojście i procedura próby Mag/Wojownik L82, 89–94, 138. Brak wyniku blockoutu, jawnie L138, 156. Opis daje bossowi dojście i uderzenie w obie szczeliny; nie ma wykazanego miejsca bezkarnego. |
| Regresje / wiążące L | Nie wróciła brama F dostępna po pierwszej przynęcie L41, 58; odłamy zmieniają ataki, nie HP ani egzamin L83–85. 990/248 HP, EXP 720/300 L77, 83, 126. |

**Oceny:** 8 / 8 / 8 / 8 / 9 / 8 / 7 / 8 / 7 / 8. **WERDYKT: PRZEPUSZCZAM projekt; blockout półek i szczelin przed wdrożeniem.**

## 009 — Dożynki w Malowanej Wsi

| Poprawka | Stan i dowód |
|---|---|
| Zwrot mimo ominięcia Bębna i losowania D/F | **Zrealizowana:** obowiązkowy G z zerwaniem wspólnego pulsu na obu trasach L15–17, jedyna droga do H przez G L38–39; wariant D/F L116. |
| „4” i bezpieczna próba dla Mag/Wojownik | **Zrealizowana:** czerwony pas i biała płyta obok, bezobrażeniowa demonstracja B L13, 48; znak w H poza pasem L73, 76. |
| Snop-osłona z pąka | **Zrealizowana:** Wstęga po 5/8/4 s tworzy jednorazowe przejście 1×5, bez osłony L8–10, 66; zgodnie z L5. |
| Ruchomy hitbox, 15 ticków, wszystkie źródła | **Częściowo:** stały znak 1 HP, 15 ticków, ręczna odsłona i boss stale przyjmujący obrażenia L77–79, 133. Wyniku pomiaru sześciu źródeł i zegarów **nie ma** (L132–133, 152, 160); plan nie jest certyfikatem, lecz istnieje grywalny tor obrażeń bez trafienia znaku. |
| Regresje / wiążące P, L | Brak starego Snopa i obrotu głów/ciszy w WOW L97–100; nie ma bramy `world.takt` L18, 132. 1330/333 HP i EXP 1080/450 L71, 77, 120. |

**Oceny:** 8 / 8 / 8 / 8 / 9 / 9 / 7 / 8 / 7 / 9. **WERDYKT: PRZEPUSZCZAM projekt; synchronizację i R2 zmierzyć przed wdrożeniem.**

## 010 — Matecznik v2

| Poprawka z recenzji X | Stan i dowód |
|---|---|
| Jedna reguła pąka, bez zasiewu pąków przez bossa | **Zrealizowana projektowo:** tylko po poległym, 5/8/4 s, najstarszy wypierany L15–21; Zasiew przyzywa Pniaczki, dopiero ich śmierć daje pąk L108, 112. |
| Finał musi być walką, nie trzema cięciami | **Zrealizowana:** konary zmieniają teren/ataki, boss ma stale przyjmować obrażenia, koniec przy 0 HP L3–5, 91–100. Dawne automatyczne −36% z trzech nici nie występuje; pojedynczy upadek może zdjąć 10% L98. |
| R2 elity, samodzielna kontra Mag/Wojownik | **Zrealizowana projektowo:** blok lub ruch wobec Sękacza L77–82, cztery archetypy przeciw bossowi L114–120. Próby wszystkich źródeł na klonie nadal nie udokumentowano; nie jest to wynik testu. |
| Nici górne/korzeniowe | **Zarzut usunięty przez zmianę mechaniki:** nie ma nici do ponownego cięcia; trzy konary przestrzennie i funkcjonalnie rozpisane L13–14, 91–109. |
| Niemożliwy obrót WOW | **Zrealizowana:** gaśnięcie dziewięciu miniatur i zapadanie Korony, bez obietnicy obrotu horyzontu L122–131. Dystans renderowania pozostaje bramą testową z planem B L129–130. |
| Silverfish, lot/upadek i odkrycie Gardzieli | **Częściowo:** baby husk L73, zamrożony teren, prawdziwy klient i teleport do Gardzieli z grobem L173–175; samego wyniku próby klienta brak. |
| Regresja wiążących liczb L1–L2 | **NIE:** boss **1440** L94 wobec obowiązkowych **1800** (`07` L15), Sękacz **220** L77 wobec **450** (`07` L16), moby opisane jako ×**1,6** L71 wobec ×**2,08** (`07` L16). Przy DPS_ref 7,5 (`07` L8) 1440 HP to ~3,2 min, nie zadeklarowane 4–5 min L94; elita traci ponad połowę wymaganej wytrzymałości. W nagłówku i tabeli nie ma uzasadnionego wyjątku. |
| Nowa niespójność układu | Losowanie Sękacza do dowolnego z trzech konarów L149 nie zostało przeniesione do stałego beat chartu (wyłącznie sala 5) L62–64; wymaga korekty opisu przebiegu, lecz samo nie blokuje gry. |

**Oceny:** **6** / 8 / 8 / 8 / 8 / 9 / 7 / 8 / 7 / 8. **WERDYKT: BLOKUJĘ** — zaniżone HP bossa i elity oraz skala mobów skracają finał i łamią wspólny bilans: L71, 77, 94 kontra `07` L8, 15–17. **Naprawa:** ustaw 1800 HP Darnika, 450 HP Sękacza i skalę mobów ×2,08, następnie przelicz czas 4–5 min oraz beat chart dla wylosowanej elity.
