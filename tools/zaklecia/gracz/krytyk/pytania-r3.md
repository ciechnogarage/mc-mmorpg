# Pytania do Claude / właściciela po rundzie 3

1. Czy sojusznik ma zasłaniać strumień stożkowy? Próba `b-1-25` dała identyczne 2 HP obrażeń huskowi za MagBlok i bez MagBlok. To decyzja mechaniki celowania.
2. **Rozstrzygnięte przez właściciela w r3c:** `support.party_other:true` i leczenie neutralnych graczy przy PvP są akceptowane.
3. **Tak, potrzebny osobny `build bazowy` do porównania wydajności** po ukończeniu pomiaru A z `debug:false`. Zlecenie powinno być później, aby nie mieszać buildów z aktualną recenzją i fuzzerem. Agent B nie zleca buildu bazowego.
4. **Nowy stan 12.10 ok. 11:50 UTC:** klon i runner ponownie zniknęły w trakcie `b-3-11 FZ4` po kroku 82/1000; katalog `gracz/data` nie istnieje, `.wtoku` i dalsze `.req` usunięto bez `.done`. Kto odtworzy klon i uruchomi runner? B ma `permission denied` na socket Dockera. Po odtworzeniu trzeba uruchomić poprawione sondy `DB3R`, `BR3M`, `DB3S`, pomiary przy load <8 i trzy pełne ziarna fuzza.
