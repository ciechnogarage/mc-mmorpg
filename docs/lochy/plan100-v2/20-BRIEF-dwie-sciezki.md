# Runda 3: dwie ścieżki i przyczynowość — BRIEF (2026-10-10)

Właściciel po przeczytaniu planu 100: „a to ma sens bo?” → „czy to jest z przypadku, czy to wszystko ma sens?” →
„pamiętaj, że to lochy, a na wyspach jest równolegle akcja jakaś, a ty wypierdalasz na to kiełbasę”.
Diagnoza prowadzącego (przyznana):
1. Plan 100 v2 opowiada historię aktów TYLKO przez lochy. Gracz, który przechodzi akt samą wyspą (decyzja właściciela:
   dwie niezależne ścieżki, równy postęp, ta sama kraina, inna treść i forma), nie dostaje historii.
2. Wyspy nie mają żadnej treści w planie (zero).
3. Akty 1→4 są łańcuchem przyczynowym (statek → kompania → pustynia → korzenie), akty 5–9 to katalog „odpowiedzi”
   (można je przetasować) — wzięte ze starego planu bez pytania o kolejność.
4. Prawa aktów nie budują mistrzostwa (późniejszy akt nie korzysta z umiejętności wcześniejszych).
5. Prawo świata należy do KRAINY, więc musi działać w otwartym świecie wyspy, nie tylko w sali lochu — niesprawdzone.

## Kanon i ograniczenia
- Akt 1 v2 (`../akt1-v2/`) ZAMROŻONY. Wyspa Aktu 1 nie ma projektu — wolno ją zaprojektować.
- Dwie ścieżki (`../akt1-wyspa-i-lochy.md` §1, korekta 30.09 w pamięci plan-100): każda SAMA wystarcza do przejścia aktu;
  wspólne: kraina, klimat, zagrożenie, rodziny wrogów (krewniacy), rodzaje materiałów, siła nagród; inne: konkretni
  wrogowie, bossowie, historie, forma. Bez par loch N ↔ strefa N. Łup „ten sam, ale nie ten sam”.
- Wyspa = osobny serwer za proxy, świat pieczony offline (Iris w kuźni), wspólna baza postępu (warunek konieczny).
  Forma wyspy: strefy, osady, zlecenia/zadania, wydarzenia świata na zegarze, boss świata, transport, zbieractwo,
  Chowańce endemiczne, jaskinie (Wynn ma ~130 jaskiń, ~300 questów, 70 eventów — `../../research/wynncraft/`).
- Stare fundamenty (materiał, nie kanon): `/home/dev/Desktop/Przemek/MC/docs/` — stolica-wyspy-hub-foundation,
  world-events-foundation, discovery-npc-board-loop-001, quest-contract-objective-foundation.
- Plan 100 v2: `05-synteza-v2.md` (Powracający, prawa), `09-plan-1-100.md`, `10-akt-*.md`. Lochy zostają jako zbiór;
  wolno zmienić kolejność aktów, prawa (z uzasadnieniem) i antagonistów, jeśli tego wymaga przyczynowość.

## Wymagania nowego planu
1. **Wydarzenie aktu**: każdy akt ma jedno główne wydarzenie świata (przyczyna → skutek), które gracz poznaje NA OBU
   ścieżkach w różnej formie; finał aktu na lochach i na wyspie = dwa oblicza tego samego wydarzenia (różni bossowie).
2. **Łańcuch 1→10**: każdy akt jest SKUTKIEM poprzedniego (test: zamiana kolejności dwóch aktów psuje historię).
3. **Wyspa aktu**: szkielet (z czego się składa, ile godzin ≈ 10 lochów, jak daje równy postęp), forma unikalna dla krainy
   (transport, wydarzenie świata, boss świata), jak działa na niej prawo aktu w otwartym świecie.
4. **Progresja praw**: późniejsze akty korzystają z umiejętności wyuczonych wcześniej (np. echo prawa w 1–2 miejscach),
   bez zamieniania finału w kolaż.
5. **Powracający na wyspie**: czy Cienie/Echa istnieją też na wyspie (i jak), czy tylko w lochach — uzasadnić.
Zakaz: wyspa jako „lochy na otwartym terenie”, pary 1:1, historia dostępna tylko na jednej ścieżce, „zabij 10 X” jako treść.
Bramka: <7 = BLOKUJĘ w: przyczynowość, równość ścieżek, treść wyspy (angażująca, nie wypełniacz), spójność z lochami,
wykonalność (osobny serwer, pieczenie offline, wydajność), sens.
