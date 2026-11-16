# Polecenie Claude dla Agenta B — runda 6 (recenzja rundy 9 A, nacisk na bezpieczeństwo przedmiotów)

Klon `klon-zaklecia` (świeży z world1 17.10) ma build A rundy 9: Zaklecia `3c60585` (gałąź `zaklecia-zaslanianie`), KsiegaPostaci
`76d0cec` (gałąź `ksiega-zaklecia-lewa` od `9542518` = world1). NIE zlecaj `build` (wyjątek A/B: `build bazowy`, potem obowiązkowo
`build`). Kolejka: `kolejka/b-r6-NN.req`. Klonu nie zwijasz. Raport `krytyk/raport-r6.md` (max 40 linii, sam plik), -o `koniec-r6.txt`.

Zmiany (decyzje właściciela): (1) zasłania wszystko, także własny przywołaniec, cel trafiony gdy widać część sylwetki w kierunku
celowania (5 promieni); (2) broń wkładana do lewej ręki przy zaklęciu w lewej ODKŁADA zaklęcie z komunikatem (zmiany w
`Bron.java` Księgi — wiele dróg: przeciągnięcie w E, F, shift-klik, klawisz numeru, wymiana zapasu, wymiana zestawu).
Sakwę Claude wycofał (jest już w oknie E jako przycisk „Sakwa rzemieślnika”) — nie testuj slotu 18.

## Zakres
1. Recenzja diffów `git -C …/zaklecia-zaslanianie diff 20f2ed8 3c60585` i `git -C …/ksiega-zaklecia-lewa diff 9542518 76d0cec`:
   każda ścieżka, w której może zginąć albo zdublować się przedmiot (zwłaszcza F: miecz z prawej idzie do lewej, prawa pusta),
   kursor, zapas lewej, wymiana zestawu, odrzucona walidacja (dwuręczna/łuk do lewej przy zaklęciu — zaklęcie NIE może zniknąć).
2. Fuzzer: 3 ziarna × 1000 kroków z pulą akcji rozszerzoną o: przypisz zaklęcie do lewej (pasek/Księga), włóż broń do lewej
   każdą drogą, F, zmiana zestawu, dwuręczna do lewej (odrzucenie), śmierć, relog. Niezmiennik: liczba prawdziwych przedmiotów
   (bez klonów wizualnych Księgi i nośników) stała, serwer = klient po zamknięciu okien.
3. Zasłanianie: własne sondy (nie A) — husk za własnym wilkiem: celowanie w środek = 0, w głowę nad wilkiem = trafienie; dwóch
   graczy PvP allow jeden za drugim; mur częściowy (połowa celu za blokiem). Zmierz, czy trafianie „części” nie jest za łatwe
   (np. trafia przez szparę 1 piksela) albo za trudne.
4. Regresja: A, C, D, F, DB3, N_ppm1, N_damage, STAT3, PW (osobne przebiegi).
Werdykt AKCEPT/ODRZUĆ dla każdej zmiany. STOP.
