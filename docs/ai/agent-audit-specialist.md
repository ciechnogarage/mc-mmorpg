# Agent Audit Specialist

Specjalista do oceny transcriptow i logow agentow bez dotykania runtime Minecrafta.

## Co mierzy

- zuzycie tokenow, jesli log zawiera liczniki
- optymalnosc pracy na bazie powtorzen komend, kosztu i stosunku dzialania do narracji
- sens rozwiazania w kontekscie briefu i sladow wykonania
- ruminacje
- ryzyko halucynacji
- zgodnosc z promptem
- procent wykonania

## Ograniczenia

`hallucinationRisk`, `solutionSense` i czesc `completion` sa heurystyczne.
Raport trzeba czytac w dwoch warstwach:

- twarde dowody z transcriptu
- wnioski inferencyjne z heurystyk

To nie jest dowod poprawnosci merytorycznej bez osobnej walidacji runtime albo testow.

## Uruchomienie

JSON:

```bash
node scripts/agent-audit.mjs MCMMORPG/_coord/results/codex1.log
```

Markdown:

```bash
node scripts/agent-audit.mjs MCMMORPG/_coord/results/codex1.log --format md
```

Wiele logow:

```bash
node scripts/agent-audit.mjs \
  MCMMORPG/_coord/results/codex1.log \
  MCMMORPG/_coord/results/codex2.log \
  MCMMORPG/_coord/results/codex3.log \
  --format md
```

Z osobnym briefem:

```bash
node scripts/agent-audit.mjs worker.log --brief worker-brief.md --format md
```

Z zapisem do pliku:

```bash
node scripts/agent-audit.mjs MCMMORPG/_coord/results/codex1.log --format md --output MCMMORPG/_coord/results/codex1.audit.md
```

## Jak czytac wynik

- `metrics.tokenUsage.total`: suma licznikow znalezionych w logu
- `scores.optimality.score`: kara za koszt, powtorzenia i slaby stosunek dzialania do planowania
- `scores.solutionSense.score`: wynik pochodny z wykonania, zgodnosci z briefem, sladow weryfikacji i jawnego raportowania ryzyk
- `scores.rumination.score`: im nizej, tym wiecej sygnalow krecenia sie w miejscu
- `scores.hallucinationRisk.score`: im nizej, tym wieksza roznica miedzy claimami a dowodami
- `metrics.completion.percent`: heurystyczne pokrycie deliverables i sygnalow zamkniecia pracy

## Typowy workflow

1. Uruchom audytor na transcripcie agenta.
2. Sprawdz `findings` z priorytetem `P0/P1` przed integracja.
3. Jesli wynik jest zanizony przez nietypowy format logu, popraw parser albo brief, nie recznie ocene.
