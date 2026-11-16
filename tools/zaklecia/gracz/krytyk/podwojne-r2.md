# „Podwójne leczenie” — powtarzalna sonda rundy 2

`b-2-02`, JAR md5 `e3075231a491`, `DBL`: ten sam czar w obu dłoniach, cel przygotowany z 12–14 HP, 3 próby na czar i sekwencję. Poprawny gest oburącz to PPM+LPM (`bot/lib/klient.js:obie`, `RzutyOdrzutowe.java:zacznij`); gest `podwojne` w macierzy rundy 1 (`bot/lib/macierz.js:cast`) wysyłał dwa LPM, czyli dwa kliknięcia prawej dłoni. Są to różne czynności.

| Zaklęcie | PPM+LPM: HP i koszt many, 3/3 | 2×LPM: HP i koszt many, 3/3 |
| --- | --- | --- |
| Leczenie | 12→18, koszt 3,1 | 12→13,2–13,53, koszt 0,2–0,3 |
| Leczenie dłoni | MagCel 14→18,38–18,5, koszt 3,1 | 14→14,1–14,2, koszt 0 |
| Wielkie uzdrowienie | 14→20, koszt 14 | 14→20, koszt 4,8–5 |
| Płomienie | obu dłońmi koszt 3–3,1, husk umiera przed końcowym odczytem HP | 2×LPM: koszt 0–0,3, HP 20→20 lub 15 |

Zatem teza „w trybie oburącz trzy leczenia nie leczą i nie pobierają many” jest obalona. `Leczenie dłoni` po samych dwóch LPM nie ma celu w aktywnej prawej dłoni, bo kod `Rzucanie.java:463-507` pobiera koszt i trafia tylko po znalezieniu sojusznika w aktywnej dłoni; prawa dłoń uruchomiona LPM jest krótkim przełącznikiem, a drugi LPM go wyłącza (`Rzucanie.java:391-415`). `Wielkie uzdrowienie` jest ładowane i sam prawy LPM rozpoczyna automatyczny rzut (`RzutyOdrzutowe.java:92-99,150-166`), więc dwa LPM wystarczą do pojedynczego leczenia, ale nie do mnożnika oburącz.

Trzy `FAIL DBL-plomienie-obie` w wyniku kolejki są błędem odczytu sondy: po rzucie nie ma już żywego huska, `o.hp` zwraca NaN; koszt many i brak celu wskazują śmierć, ale sonda nie zapisała pakietu zgonu. Nie liczę ich jako regresji gry. Ustalenie z rundy 1 o 0 HP `Wielkiego uzdrowienia` po `podwojne` było zależne od sceny/przygotowania; `DBL` nie odtwarza go na obecnym buildzie.
