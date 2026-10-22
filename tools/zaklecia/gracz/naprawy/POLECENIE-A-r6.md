# Polecenie Claude dla Agenta A — runda 6 (krótka)

Runda 5 przyjęta: poprawka CME zacommitowana (`8d7d23f`), nakładka (dźwięki 8→4, cząsteczki Start_Obie) przyjęta.
Uwaga: NIE uruchamiasz `klon.sh usun`/stop — klon zwija Claude na końcu całego zadania. Zostaw klon w spokoju.

## Ocena Claude prawdziwym klientem (zrzuty `klient/captures/L02k.png`, `L0304.png`, `porownanie-N7.png`)
- **N6 tylko w połowie:** po przypisaniu panel „Twoje dłonie” odświeża TEKST podpowiedzi (Prawa: pusta / Lewa: Odmrożenie),
  ale IKONA zostaje pustym ciemnym kwadratem — gracz bez najechania kursorem nie widzi, co ma w dłoniach. Panel ma
  pokazywać ikonę przypisanego zaklęcia (ten sam model/ikona co w siatce Księgi; dla obu dłoni — jeśli panel ma 2 sloty,
  lewy/prawy; pusta dłoń = obecny pusty kwadrat). Źródło: `DeluxeMenus/gui_menus/ksiega_zaklec.yml` (+ placeholdery
  `%zaklecia_prawa|lewa%` / ewentualnie nowy placeholder z id modelu — jeśli w Zaklecia, to w kodzie Placeholdery.java).
  Pamiętaj pułapkę DeluxeMenus: `material: 'nexo-%ph%'`/model rozwinięty do pustego id = okno się nie otworzy
  (pamięć `proba-generalna-chw-proba.md`) — daj fallback na istniejący model.
- **N7 lepiej, ale jeszcze nie dobrze:** cel jest widoczny, lecz wąski strumień nadal zajmuje lewy dolny róg ekranu
  dużymi płaszczyznami. Zmniejsz go dla rzucającego dalej: start dalej od kamery (np. +0,6 bl. do przodu), węższy (≈0,3),
  i/lub krótszy; bez zmiany widoku dla innych. Nie ruszaj tekstur.
- Cząsteczki `Start_Obie` przy `@self y=1.3` — nie widziałem ich problemu, ale sprawdź, że nie rodzą się w kamerze
  (pamięć: cząsteczki przy dłoni wychodzą ogromne).
Zakres: tylko te 3 punkty. `build`, sondy na nie (N6: treść/model slotu panelu po przypisaniu obu dłoni i po zdjęciu),
`test A,N_r2` regresji. Ostatni build = Twój, `debug:false`. Szczegóły `naprawy/szczegoly-r6.md`. STOP.
