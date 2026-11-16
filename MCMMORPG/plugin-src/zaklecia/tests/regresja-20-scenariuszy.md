# Regresja v4.0 — 20 scenariuszy na staging `world`

Scenariusze są do wykonania przez nadzorcę po budowie i uruchomieniu backendu przez Velocity. Dla każdego sprawdź HUD, koszt many, skill, dźwięk i logi.

1. Płomienie w lewej: PPM trzymany uruchamia strumień, puszczenie kończy.
2. Odmrożenie w lewej: PPM trzymany uruchamia strumień, puszczenie kończy.
3. Płomienie w prawej: pojedynczy LPM włącza, drugi wyłącza.
4. Odmrożenie w prawej: pojedynczy LPM włącza, drugi wyłącza.
5. Pusta prawa + Płomienie w lewej: PPM trzymany prowadzi lewą, puszczenie kończy.
6. To samo zaklęcie w obu: PPM + klik LPM w oknie `rowno` daje jeden efekt `_Obie`, koszt x2,8.
7. Dwa różne zaklęcia koncentracji: obie dłonie działają niezależnie, koszt jest sumą.
8. LPM poza oknem `rowno`: przełącznik prawej nie tworzy rzutu oburęcznego.
9. Klik LPM przy trzymanym oburącz: zostaje lewa koncentracja.
10. Brak many w koncentracji: brak następnego efektu, Zak_BrakMany, strumień znika.
11. Zmiana slotu w koncentracji: strumień i ruch wracają do normy.
12. Otwarcie paska Wybór w koncentracji: strumień gaśnie, klik przypisuje zamiast rzucać.
13. Ognisty pocisk w lewej: PPM krócej niż 8 t, puszczenie anuluje bez kosztu.
14. Ognisty pocisk w lewej: po `gotowe` puszczenie pobiera 3 many przed pociskiem.
15. Kula ognia w prawej: LPM uruchamia 14 t ładowania i automatyczny pocisk.
16. Kula ognia oburącz: jeden pocisk, koszt x2,8, obrażenia x2,2.
17. Leczenie `cel: siebie`: leczy co 10 t bez strumienia, stożka i trafienia celu.
18. Runa ognia: zły raycast (bok bloku/brak bloku) nie pobiera many; poprawny stawia jedną runę; następna usuwa najstarszą.
19. Osłona: narasta przez 10 t, redukuje obrażenia na HIGH, zjada pulę i pęka przy zerze.
20. Wyjście/śmierć/zmiana slotu podczas ładowania: brak wystrzału i kosztu; wyjście usuwa aktywne runy.
