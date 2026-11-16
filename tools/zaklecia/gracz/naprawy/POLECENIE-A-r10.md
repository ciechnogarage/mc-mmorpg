# Polecenie Claude dla Agenta A — runda 10 (poprawka zasłaniania po recenzji B)

Runda 9 zacommitowana przez Claude: Zaklecia `3c60585`, KsiegaPostaci `76d0cec`. B (`krytyk/raport-r6.md`): odkładanie zaklęcia
przy broni w lewej — AKCEPT (3×1000 fuzz, 0 naruszeń). Zasłanianie — ODRZUĆ: `B6Edge` (`kolejka/b-r6-19`) husk wystający ~0,25 bl.
zza muru (x=0,95–0,975) jest widoczny, ale nietrafialny — 5 punktów (boki 0,4 szerokości) nie sięga krawędzi. Druga wada: `Strumien`
skraca model do pierwszej przeszkody na promieniu z DŁONI, więc przy celowaniu w głowę ponad wilkiem trafienie jest, a model urywa się
na wilku. Właściciel chce, żeby celowanie i dobra gra się opłacały: co widać i w co celujesz — trafiasz.
Sakwa wycofana (jest w oknie E jako „Sakwa rzemieślnika”) — nie ruszaj `stale-sloty`.

## Zakres (tylko `Zaslanianie.java` / `Strumien.java` / ewentualnie `Rzucanie.stozek`)
1. **Celownik ma pierwszeństwo:** jeśli promień z oczu wzdłuż kierunku patrzenia (zasięg stożka) pierwszą istotą/blokiem trafia
   kandydata — kandydat trafiony, bez dalszych testów.
2. **Siatka sylwetki do krawędzi:** w pozostałych przypadkach próbkuj punkty przekroju hitboxu prostopadłego do linii wzroku —
   np. 5 kolumn (od 0,05 do 0,95 szerokości) × 3 rzędy (0,15/0,5/0,9 wysokości); nadal tylko punkty w kierunku celowania (filtr kąta
   jak dziś, ale względem punktu, nie środka, z sensownym marginesem). Pierwsza przeszkoda na promieniu = ten cel → trafienie.
   Limit pracy: najpierw promień celownika, potem punkty od najbliższego celownikowi, przerwij na pierwszym trafieniu.
3. **Model strumienia:** długość wzdłuż linii wzroku z oczu (to, co gracz widzi i trafia), start modelu dalej z dłoni jak dziś.
4. Sondy: przepuść `B6Edge` i `B6Occlusion` B (pliki w `bot/scenariusze/`) — próg widoczności przy murze ma się przesunąć tak, by
   widoczne ≥ ~10% szerokości dało się trafić celując w tę część, a 0% (pełna zasłona) nadal 0; wilk + celowanie w głowę: trafienie
   i model nie urwany na wilku; środek za wilkiem: 0. Zmierz koszt (debug) dla 1, 5 i 10 kandydatów.
Regresja: N_zaslona, N_ppm1, A, D, F, STAT3, PW. `build bazowy` niepotrzebny (porównanie z `3c60585` — zrób `build` przed zmianą,
zapisz md5). Szczegóły `naprawy/szczegoly-r10.md`. STOP po raporcie.
