# Recenzja 2 — Projektant C (etap 6, 2026-10-03)

Zakres: czy moje poprawki obowiązkowe z 06-recenzja-C.md są zrealizowane albo odrzucone z dowodem, czy poprawki nie
wprowadziły sprzeczności (szukałem grepem starych wartości i fraz) i czy 010 v2 zdejmuje moje zarzuty. Blokuję tylko to,
co realnie psuje grę.

**Werdykty:**
- 002: PRZEPUSZCZAM
- 005: PRZEPUSZCZAM
- 008: PRZEPUSZCZAM pod warunkiem jednego zdania (W8)
- 010: PRZEPUSZCZAM pod warunkiem dwóch zdań (W10a, W10b)

## 002 Bagna (X) — PRZEPUSZCZAM
| Poprawka | Stan | Dowód w karcie |
|---|---|---|
| 1. Zwrot na trasie obowiązkowej | ✓ | §1: „Kokoniarnia → Borek → korba → III to obowiązkowa droga”; „III zawsze… zalewa Groblę” |
| 2. Losowość zmieniająca taktykę | ✓ | §10: losowo uszkodzona II albo III zmienia czynne studnie; Kokoniara losuje stronę Borka |
| 3. Pąk dwuznaczny | ✓ | §0: chmura „truje też Pijawki, Mułowca i Topielicę”; §1: bąbel przy studni zatruwa bossa |
| 4. Skrzek bez pąków | ✓ | §6: „jaja nie są pąkiem”, inny model i dźwięk |
| 5. HP wg skali | ✓ | 525 HP, ~2,5 min, EXP 240 (L2/L3) |
Sprzeczności: brak. Uszkodzona III nadal zalewa Groblę (§6), więc zwrot nie znika przy żadnym losowaniu.
Nowe oceny: fun 7 · mechanika/zwrot 8 · czytelność 8 · topologia 8 · WOW 8 · sens 8 · powtarzalność 7 · nagroda 7 ·
wykonalność 7 · różnorodność 7.

## 005 Oranżeria (X) — PRZEPUSZCZAM
| Poprawka | Stan | Dowód |
|---|---|---|
| 1. Kotłownia obowiązkowa | ✓ | §15 i §3: obowiązkowy rdzeń z Kotłowni + 1 z 3 skrzydeł |
| 2. Sadzenie bez PPM/KeyItem | ✓ | §13: wejście na pole r=1,5, TriggerDistance przy dodatnim liczniku HUD |
| 3. Odchudzenie | ✓ | kapsuła do końca starcia; węzeł statyczny „nie teleportuje za mobem” (§5); biały = gracz, zielony = Ilma |
| 4. HP wg skali | ✓ | Ilma 900, Starszy 225, EXP 600 |
Drobne, nieblokujące: w tabeli bossa zostały sformułowania „trafić nić” i „cios w nić” (§6). Zapis §5/§13 mówi, że
trafia się statyczny węzeł, więc warto ujednolicić słowo na „węzeł”.
Nowe oceny: fun 7 · mechanika 8 · czytelność 7 · topologia 7 · WOW 8 · sens 8 · powtarzalność 9 · nagroda 7 ·
wykonalność 7 · różnorodność 7.

## 008 Spichlerz (X) — PRZEPUSZCZAM z warunkiem W8
| Poprawka | Stan | Dowód |
|---|---|---|
| 1. Kara proporcjonalna, korzeń bez łańcucha | ✓ | 1,5% maks. HP × stos (do 93,75 HP na trafienie); korzeń ≤1 s; 2 s bez celowania w gracza |
| 2. Zwrot jako obraz | ✓ | §1: Matka wychodzi z zsypu, zielony segment pęcznieje tylko przy stosach |
| 3. Dwie drogi do S | ✓, próg odrzucony z dowodem | Odrzucenie progu 90 s przyjmuję: 1250/6,5 ≈ 192 s, a nawet +75% DPS daje ~110 s, więc 90 s było sprzeczne z L1/L2. Uwaga nieblokująca: 240 s to więcej niż typowe 180–210 s walki bez chleba, więc warunek Uczty jest łatwy; proponuję 200 s. |
| 4. Stos 0–5, scoreboard | ✓ | §0, §6, §13 |
| P1 Wołek | odrzucony z dowodem | Rejestr L5 (późniejszy, wiążący) przypisuje 008 Kłos Pokusy. Przyjmuję. |
| P4 szlifierka | ✓ | Młynarczyk `block.wool.hit` |

**W8 (realnie psuje grę):** Kłos Pokusy dodaje stos „przy pierwszym wejściu” (§0). W walce wręcz gracz wchodzi w Kłos
przypadkiem, a to nieodwracalnie psuje drogę „Post” do S i zamienia pokusę w pułapkę. **Naprawa:** Kłos działa jak
bochen, tzn. trzeba w nim postać 1 s (§1 Wstęp już tak definiuje „świadome zjedzenie”).
Nowe oceny: fun 7 · mechanika 8 · czytelność 7 · topologia 8 · WOW 8 · sens 9 · powtarzalność 8 · nagroda 7 ·
wykonalność 7 · różnorodność 8.

## 010 Matecznik v2 (prowadzący) — PRZEPUSZCZAM z warunkami W10a, W10b
| Mój zarzut (06-C §4) | Stan | Dowód |
|---|---|---|
| 1. Krewniacy zamiast kopii | ✓ | Muflon i Cielę usunięte; Snopowy Strażnik jako „lekarz drzewa” zrasta nacięcie (nie garda); Pniak-Taran i Barciowy Rój mają nowe funkcje |
| 2. Szczepienie bez leczenia | ✓ | trafienia gracza zrastają konar zamiast leczyć; kontra ruchem 6 bl. |
| 3. Czas ≤20 min | ✓ | 17–19 min, beat chart kończy się w 18,5 |
| 4. Pąk tylko po poległym (P8) | ✓ | Zasiew = 3 Pniaczki, pąki dopiero z ich śmierci; Pęd to jedyny skutek |
| 5. R2 bez „PPM rozbija” | ✓ | konary trafia każde źródło (duży hitbox); Sękacz: blok ALBO krok w bok |
| Kopia 005 (nici) | ✓ | nici i węzły usunięte; rdzeń to teren tworzony przez gracza |

**W10a (sprzeczność liczb):** §6 „HP Darnika 1440 (wspólna tabela 07)” i bestiariusz „Gaj v8 ×1,6”, a tabela 07 L2 mówi
010 = 1800, elita 450, moby ×2,08 (Sękacz 220 zamiast 450). **Naprawa:** przepisać na 1800 / 450 / ×2,08 albo zmienić L2.
Ten sam problem dotyczy oceny: §10 „ocena = liczba skrzyń (1–3)”, a w 001, 004 i 007 (na żądanie Y4) ocena daje tylko
wygląd i tytuł. Akt musi mieć jedną zasadę: proponuję w 010 też „ocena = kosmetyk”.

**W10b (realnie psuje grę):** Okulizacja unieruchamia na 1,5 s, a w F3 Opad korzeni ma zamknięty cel z zapowiedzią 1,0 s.
Unieruchomiony gracz może więc dostać trafienie, którego nie da się uniknąć (to samo blokowałem w 008). **Naprawa:** przez
2 s po Okulizacji żadna zdolność z zamkniętym celem nie celuje w unieruchomionego gracza.
Nowe oceny: fun 8 · mechanika 8 · czytelność 8 · topologia 8 · WOW 9 · sens 9 · powtarzalność 7 · nagroda 7 (po W10a) ·
wykonalność 7 · różnorodność 8.

## Przekrój (nowe sprzeczności po poprawkach)
- Zasada oceny: 001/004/007/002/005/008 dają kosmetyk lub tytuł, 010 daje skrzynie (W10a). Do ujednolicenia.
- Unieruchomienia: po poprawkach 008 i 010 (W10b) w akcie nie zostaje żaden łańcuch CC. Kokoniara 002 (kokon do 4 s,
  rozbijany 3 trafieniami) nie ma ataku z zamkniętym celem w trakcie, więc jest OK.
- Rejestr L5/L6 został zachowany we wszystkich czterech kartach.
