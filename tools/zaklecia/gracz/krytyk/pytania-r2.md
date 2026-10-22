# Pytania do Claude / właściciela, runda 2

1. Czy sojusznik ma fizycznie zatrzymywać strumień stożkowy? `b-1-25` md5 `1a1994fbc239` pokazuje huska za MagBlok z identycznymi 2 HP obrażeń jak bez MagBlok.
2. Czy N5 ma pozwalać na leczenie każdego neutralnego gracza przy PvP, czy tylko członków party/gildii? Nakładka `MythicLib/config.yml:318` ustawia `support.party_other: true`; to poszerza politykę celu poza naprawę pojedynczego czaru.
3. Jak mierzyć „stop” dla leczenia, tarczy i innych czarów bez `entity_destroy` strumienia oraz pakietu hurt? W poleceniu są wyłącznie te dwa znaczniki, lecz dla takich czarów nie istnieją. Pytanie wysłane też przez UI; bez odpowiedzi raportuję brak wiarygodnego stopu.
4. Przed końcowym pomiarem PERF2 potrzebny jest ostatni build A z `Zaklecia/config.yml debug:false`. Agent B nie modyfikuje nakładek ani nie zleca buildów; aktualna nakładka ma `debug:true` w linii 196.
5. **Rozstrzygnięte:** A/B `b-2-09` (baza + `mmodamage`) i `b-2-10` (A + `damage`) potwierdza utratę skalowania Wiary 1,60×→1,00×. Czy właściciel akceptuje taką zmianę mechaniki? Rekomendacja B: nie; potrzebna naprawa filtra `Bron`, która zachowa ścieżkę `MMODamageMechanic`.
