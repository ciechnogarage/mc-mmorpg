Runda 5 została wykonana **bez commitów**. [Raport](/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/naprawy/raport-r5.md) i [szczegóły z tabelą A/B](/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/naprawy/szczegoly-r5.md) są zapisane.

- **CME:** ta sama śmierć gracza od Płomieni wywołała wyjątek na bazie; na buildzie A `N_cme` przeszła bez nowego błędu logu.
- **Miecz i lewy czar:** izolowana sonda potwierdziła obrażenia LPM, działanie PPM oraz zachowanie miecza po powrocie do slotu 0. Bazowa sonda zatrzymała się przed ciosem, więc nie potwierdza osobnego FAIL→PASS dla LPM.
- **Osłona i Uspokojenie:** testy potwierdziły redukcję obrażeń i zatrzymanie ataków huska. Dźwięki strumienia spadły z 8 do 4, a oburącz z 9 do 4.
- **Otwarte:** reguła wyposażania lewej broni wymaga decyzji właściciela; rozjazd fuzzera nie był stabilny w powtórce. Zbiorcze grupy B/C nadal mają porażki i timeouty opisane w szczegółach.

Ostatni build to A z `debug:false`. **Klon pozostał:** próba `klon.sh usun zaklecia` zakończyła się kodem 1 z powodu ograniczeń sandboxu; nadzorca musi dokończyć sprzątanie z dostępem do Dockera.