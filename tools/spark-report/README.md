# Czytanie profili spark bez wysylania ich na zewnatrz

Spark jest wbudowany w Paper (nie ma go na liscie pluginow) i w tle profiluje
Server thread od startu serwera.

    docker exec mc-world1-1 rcon-cli "spark profiler --stop --save-to-file"

Odpowiedz rcon jest PUSTA — spark odpowiada asynchronicznie, po zamknieciu polaczenia.
Efekt sprawdzac w `logs/latest.log`; plik ladzie w `plugins/spark/*.sparkprofile`
(surowy protobuf, bez gzipa). Profiler wznawia sie sam po zrzucie.

Przez rcon dziala `mspt` (avg/min/max z 5 s, 10 s, 1 min). `tps` i `spark tps` NIE —
zwracaja pustke.

Skrypty (czysty python3, bez zaleznosci):

    python3 meta2.py <plik>            # heap, pule G1, pauzy i czestotliwosc GC, uptime, TPS
    python3 hot.py <plik> [wzorzec...] # podzial czasu watku glownego, total-time wg wzorca
    python3 callers.py <plik> <metoda> # sciezka wywolan do metody — czyj to koszt

`meta2.py` w statystykach GC: pole 1 = liczba kolekcji, 2 = srednia pauza w ms,
3 = srednia PRZERWA miedzy kolekcjami (nie suma czasu GC).
