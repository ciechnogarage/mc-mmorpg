# Creator Visual Language

Status: foundation premium creator v1, 2026-07-05.

## Product Goal

Creator ma wyglądać jak onboarding MMORPG, a nie utility GUI. Każdy ekran ma jedną funkcję emocjonalną, jedną główną decyzję i jeden czytelny payoff.

## Screen Roles

- `foundation_creator_intro`: ceremonialne wejście i obietnica flow.
- `foundation_race_selector`: galeria wejściowa ras, bez finalnej decyzji.
- `foundation_race_spotlight_*`: decyzja fantasy rasy, city boon, koszt wyboru.
- `foundation_class_selector`: galeria wejściowa klas, bez finalnej decyzji.
- `foundation_class_spotlight_*`: decyzja gameplayowa klasy, subclass pull i anchor path.
- `foundation_class_confirm_*`: oath screen i zapis ceny decyzji.
- `foundation_class_mentor_roster`: pierwszy payoff po wyborze klasy.
- `foundation_city_roster`: premium hub pierwszego kontraktu.

## Visual Rules

- Rasy i klasy używają własnych barw tła, ale wspólnej hierarchii layoutu.
- Runtime-safe path for current creator menus is `MenuCreator + vanilla items + textured player heads`.
- Do not use `source: mmoitems` in creator menus unless the exact MenuCreator surface is proven live in this stack.
- `NETHER_STAR`, `BOOK`, `WRITABLE_BOOK`, `EMERALD`, `ARROW`, `BARRIER` pozostają globalnymi sygnałami navigacji i decyzji.
- Hero card ma zawsze 4 pola: fantasy, payoff, cost, next step.
- Spotlight nie może być listą suchych statów; ma sprzedawać tożsamość.
- Gallery nie finalizuje wyboru. Finalizacja dzieje się w spotlightcie lub oath screenie.

## Audio Rules

- Intro: ceremonialne wejście.
- Race spotlight: osobny cue dla każdego rodu.
- Class spotlight: osobny cue archetypu.
- Oath confirm: mocny cue zapisu tożsamości.
- Mentor hall: payoff reveal.
- Quartermaster: reward scene, nie utility ping.

## Copy Rules

- Polski jest językiem warstwy designowej.
- Każdy spotlight musi mieć:
  - one-line fantasy,
  - realny live payoff,
  - jasny koszt wyboru,
  - starter scene,
  - następny krok.
- Nie wolno udawać live combat passive, jeśli nadal jest `pending runtime hook`.

## Quality Gate

Creator nie jest gotowy, jeśli:

- wybór rasy lub klasy kończy się na gallery,
- spotlight nie ma kosztu decyzji,
- oath nie prowadzi do pay-offu,
- mentor nie pokazuje subclass path,
- city hub brzmi jak tablica linków,
- runtime bridge jest obecny tylko technicznie, bez sensu dla gracza.
