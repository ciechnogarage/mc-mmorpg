# Recenzja Y — runda końcowa (2026-10-03)

Zakres: wyłącznie blokady z `08-recenzja2-Y.md` i sprzeczności wprowadzane przez ich poprawki. Dowody z kart; bez prób na serwerze.

## 10-loch-001 — BLOKUJĘ

001:212: po 13. pąku najstarszy dostaje tag `usycha`, lecz przez 1 s pozostaje w `@e[tag=pak]`. Jeśli w tym czasie powstanie 14., `#ile` policzy także usychający pąk, a `#min` ponownie wybierze ten sam. Po jego usunięciu zostaje 13 żywych pąków (nr 2–14), sprzecznie z limitem 12 w 001:24–25. Plan B używa tej samej selekcji (001:212), więc też nie naprawia przypadku. **Naprawa:** liczyć i wybierać tylko `@e[tag=pak,tag=!usycha]`, wyłączając pąk z kolejki w chwili oznaczenia; przy każdym przyroście redukować liczbę aktywnych do ≤12.

## 10-loch-004 — PRZEPUSZCZAM

004:228: koszenie zmienia `tripwire` na `air` na y i y+1; `coarse_dirt` trafia wyłącznie na y−1. Przejście po pasie pozostaje wolne. Nie znalazłem sprzeczności w tej poprawce.

## 10-loch-007 — PRZEPUSZCZAM

007:72–83,216–219: w B liście S4 na y+8 mają kolumny puchu dla Lekkiego; osłony areny na y+4 mają dwa stopnie co 2 bl. Zapisano test wejścia na każdą osłonę. Poprzednia luka w dostępie jest zamknięta; brak nowej sprzeczności w tej poprawce.

## 10-loch-010 — PRZEPUSZCZAM

010:71–77,94: HP Darnika 1800, elity 450, skala mobów ×2,08 zgodna z 07 L2. 010:154–162: ocena daje wyłącznie kosmetyk i wpis, łup oraz EXP nie zależą od oceny. 010:107–112: po unieruchomieniu obowiązuje 2 s bez celowania zdolnością z zamkniętym celem. Wskazane blokady są zdjęte; brak nowej sprzeczności w tych poprawkach.

001 po poprawce prowadzącego: PRZEPUSZCZAM — 001:212 liczy i wybiera tylko `@e[tag=pak,tag=!usycha]`, więc oznaczony pąk od razu wypada z kolejki; 001:251 potwierdza tę zmianę.
