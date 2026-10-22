# Runda 1 — wydajność strumieni

Scenariusz `bot/scenariusze/PERF.js` ma trzy okna po 10 s: 20 zalogowanych botów bezczynnych, 10 rzucających oraz 20 rzucających. Limit klona `max-players=20` (`data/server.properties:40`), więc MagTest jest jednym z rzucających i zarazem odbiorcą zliczanych pakietów. Każde okno zapisuje `mspt` przed/po, pakiety/s do MagTest, pobór many wszystkich aktywnych botów i osobny zrzut wbudowanego spark do pliku na klonie. Spark jest resetowany przed fazą bezczynności, a `--stop --save-to-file` między fazami.

Uwaga pomiarowa: nakładka `naprawy/plugins-overlay/Zaklecia/config.yml` ma `debug:true`; koszt instrumentacji N2 jest wliczony. Raport musi podać dokładny JAR md5 i uwzględnić tę konfigurację.

## Pomiar
`kolejka/b-1-22.out`, JAR md5 `1a1994fbc239`, wszystkie trzy fazy PASS; pobór many potwierdził aktywność 10/10 i 20/20 rzucających (każdy 20→11,2). Średnie MSPT z ostatnich 5 s według RCON `mspt`, pakiety do MagTest z Mineflayer:

| Faza | MSPT średnie / maksimum | Pakiety/s | Zaklec `Rzucanie.tick` w spark |
| --- | ---: | ---: | ---: |
| 20 botów bezczynnych | 8,8 / 36,6 ms | 45,0 | poniżej progu próbkowania |
| 10 strumieni | 15,9 / 80,5 ms | 255,4 | 680 ms / 10 s (6,8% czasu ściennego) |
| 20 strumieni | 18,9 / 68,8 ms | 362,9 | 840 ms / 10 s (8,4% czasu ściennego) |

Profile zapisane na klonie: `data/plugins/spark/profile-2026-10-10_02.31.07.sparkprofile` (idle, 6 s próbkowania), `...02.31.18.sparkprofile` (10 botów, 10 s), `...02.31.28.sparkprofile` (20 botów, 10 s); plik `...02.31.00.sparkprofile` to odrzucony reset profilu od startu serwera. Odczyt `spark-report/hot.py`: przy 10 strumieniach aktywna praca wątku głównego 3,1 s/10 s, przy 20 — 3,5 s/10 s; `Rzucanie.tick` to odpowiednio około 22% i 24% tej aktywnej pracy (inclusive, bez dodawania podmetod). `MythicMobs BukkitAPIHelper.castSkill` w profilu 20 botów osiąga 1,18 s inclusive, ale jest wywoływany też spoza Zaklec, więc nie przypisuję całego kosztu pluginowi.

Wynik jest względnym porównaniem na klonie z `debug:true` i 20 stale zalogowanymi botami. Od 10 do 20 strumieni średnie MSPT rośnie o 3,0 ms, a pakiety do obserwatora o 107,5/s. Nie ma tu pomiaru z domyślnym `debug:false`, więc nie wolno przenosić bezpośrednio wartości absolutnych na produkcję.
