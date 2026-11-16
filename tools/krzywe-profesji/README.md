# Krzywe XP profesji — skad sie biora liczby

## Problem, ktory to naprawia
Wszystkie 13 profesji mialo krzywa-formule:

    exp-curve: '150 + {level}*75 + {level}*{level}*12'

W MMOCore `{level}` w formule to poziom **KLASY**, nie profesji
(`FormulaExperienceCurve$LevelPlaceholder.parse()` -> `PlayerData.getLevel()`).
Skutki: w obrebie jednego poziomu klasy krzywa byla plaska (poziom 50 profesji
kosztowal tyle co 2), a awans klasy 1->50 podnosil koszt kolejnego poziomu
profesji ~143x, retroaktywnie.

`ListExperienceCurve.getExperience(PlayerData, int)` **w ogole nie dotyka
PlayerData** — indeksuje liste poziomem PROFESJI, przycinajac do ostatniej
pozycji (`Math.min(level, size) - 1`). Krzywa z pliku jest wiec z definicji
niezalezna od poziomu klasy. To nie jest obejscie, tylko usuniecie zaleznosci.

## Format pliku (potwierdzony bajtkodem, nie zgadniety)
- `plugins/MMOCore/exp-curves/<nazwa>.txt`, jedna liczba na linie
- linia N = XP na przejscie z poziomu N na N+1, **nie kumulatywnie**
- w yml profesji `exp-curve: <nazwa>` (bez `.txt` — manager obcina 4 znaki)
- **PULAPKA:** `ExperienceCurve.fromConfig()` przy nieznanej nazwie nie zglasza
  bledu, tylko po cichu traktuje napis jako formule. Literowka = cichy powrot
  buga. Jedyny dowod, ze wczytala sie krzywa z pliku, to zgodnosc zmierzonych
  progow z plikiem (robi to `tools/eval-wezly/krzywe-po.js`).

## Tempo — skad wspolczynniki
Nie dobieramy kosztow "z sufitu". Dobieramy CZAS, a koszt z niego wynika:

    koszt(L) = m(L) * rate(L)        # m w godzinach, rate w XP/h

### rate — zmierzone z plikow tresci
**Zbieractwo** (`MythicMobs/skills/WezlySkills.yml`), XP za jeden zbior:
dab 18, swierk 20, brzoza 15, kepa ziol 11.75 sr., lisc ksiezycowy 21.6 sr.,
grzyby 17.5 sr., poletko 22, druza 24.5 sr., glina 14, przyprawy 17.25 sr.
-> **srednio ~18 XP na wezel**. Cykl (dojscie + kanalowanie) ~25 s
-> **~2600 XP/h**, PLASKIE: wezly nie skaluja XP z poziomem profesji.

**Rzemioslo** (`MMOItems/crafting-stations/profession-*.yml`): 12/28/55/95/120
XP za recepture, bramki poziomow 0/15/30/45/45. Wejscia trzeba najpierw zebrac,
wiec tempo jest ograniczone zbieractwem: tier 1 ~12 XP / ~35 s = ~1230 XP/h,
tier 5 ~120 XP / ~115 s = ~3760 XP/h. Tempo **rosnie** z poziomem — dlatego
rzemioslo ma wlasna krzywa, nizsza w sumie niz zbieractwo.

### m(L) — rodzina gladka z definicji
`m(L) = A * L^p * q^L`, trzy parametry dopasowane do trzech kotwic czasowych
(Nelder-Mead bez zaleznosci; na hoscie nie ma numpy/scipy).
Dla p>=0 i q>=1 funkcja jest rosnaca i gladka, wiec krzywa nie ma skokow.

Kotwice domyslne: poziom 15 = 2 h, 30 = 10 h, 50 = 45 h. Dopasowanie trafia w
nie co do drugiego miejsca (blad ~1e-15) — czyli zalozenie tempa bylo
wewnetrznie spojne, nie trzeba bylo go naginac.

## Uzycie
    python3 generuj.py                 # sam podglad, nic nie zapisuje
    python3 generuj.py --zapisz        # pisze exp-curves/*.txt + podmienia 13 yml
    python3 generuj.py --kotwice '15:1.5,30:8,50:35' --zapisz   # szybsze tempo

Po kazdym zapisie: `/mmocore reload`, potem **obowiazkowo**
`EVAL_SCRIPT=krzywe-po.js tools/eval-wezly/run.sh` — bez tego nie wiadomo,
czy MMOCore wczytal plik, czy po cichu wrocil do formuly.

## WAZNE ZALOZENIE, ktore trzeba spelnic w swiecie
2600 XP/h wymaga **co najmniej 8 wezlow w jednej petli zbierackiej**:
przy 25 s na wezel i respawnie 180 s trzeba 180/25 = 7.2 wezla, zeby pierwszy
zdazyl odrosnac. Dzis na serwerze stoi JEDEN spawner (`demo_wezel_dab`,
cooldown 180 s) — przy nim realne tempo to ~360 XP/h, czyli poziom 50 po
~325 h zamiast 45 h. Krzywa jest poprawna, ale jej realizm zalezy od gestosci
wezlow, ktora trzeba dopiero postawic.
