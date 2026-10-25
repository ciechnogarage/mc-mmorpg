# Chowance

Chowańce: łapanie, drużyna, walki turowe (dzicz, trenerzy, Mistrzowie Więzi, Wieża, pojedynki), hodowla, handel,
Bestiariusz, magazyn. Zależności: MythicMobs, MythicLib, MMOProfiles (bez MMOProfiles plugin się nie włącza —
na klonie tylko `--z-kreatorem`); softdepend MMOCore, MMOItems, PlaceholderAPI, MythicDungeons, ModelEngine,
DeluxeMenus, CharacterCreator.

## Dokumentacja
- **Format plików treści, komendy, placeholdery, uprawnienia: `docs/chowance/KONTRAKT.md`** (repo MC) — źródło prawdy dla
  `gatunki/`, `ruchy/`, `cechy/`, `trenerzy/`, `wieza.yml`, `startery.yml`, `zdarzenia.yml`, sygnałów MM.
- Projekt i decyzje: `docs/chowance/SPEC.md`, `DECYZJE.md`; znane problemy `PROBLEMY.md`, dziennik `DZIENNIK.md`.
- Pułapki i stan wdrożeń: skill `mc-systemy-gracza/chowance.md`.

## Kod (`src/main/java/pl/mcmmorpg/chowance/`)
`Komendy.java` (`/chw`, aliasy `chowance`, `chowaniec`; podkomendy gracza i `admin`), `gra/` (walki, `WalkiTurowe`),
`model/` (rekordy treści, np. `Trener`), `tresc/LadowaczTresci.java` (wczytanie plików treści), `dane/` (zapis profilu),
`integracja/` (MythicMobs, PAPI, MythicDungeons, DeluxeMenus), `logika/`, `api/`.

## Trenerzy: kiedy idą komendy (od 25.10, 4a0c6fb)
- `nagroda_komendy` — MISTRZ: tylko przy PIERWSZYM zdobyciu odznaki (powtórka: komunikat „nagroda mistrza jest
  jednorazowa”); TRENER: po każdej wygranej.
- `komendy_po_walce` — po KAŻDEJ wygranej, też powtórce (np. `lochy sygnal %gracz% sala_mistrzow_koniec 100`, koniec lochu).
Plik treści i jar wdrażaj razem (nowy jar + stary yml z sygnałem w `nagroda_komendy` = loch nie zamknie się przy powtórce).

## Budowa i test
`./gradlew build` (jar w `build/libs`, kopiuj tylko najnowszy). Test końca lochu i nagrody: `lochy-poprawki/test/koniec-chowance.js`.
