# klasy-rasowe/serwer — pliki serwera paczki „klasy: progresja” (B, 25.10)

Kopia plików world1 (baza 89b663b) z poprawkami B1–B14 z `/opt/docker/mc/backups/klasy-naprawa-20261025/PLAN.md`.
Na serwer trafiają przez `wgraj.sh`; generatory (`../generuj_klasy.py`, `../karta_kreatora.py`, `tools/przedmioty/generuj.py`)
piszą resztę (klasy rasowe, kartę kreatora, przedmioty, `Zaklecia/klasy.yml`).

## Wgranie
```
tools/klasy-rasowe/serwer/wgraj.sh /opt/docker/mc/klony/<klon>/data [zaklecia-*.jar]
docker run ... python:3.12-slim (pyyaml) tools/przedmioty/sprawdz.py   # 0 błędów
/opt/docker/mc/narzedzia/klon.sh restart <klon>; rcon "minecraft:difficulty peaceful"
```
- world1: tylko agent integracji, `WGRAJ_WORLD1=tak`, po kopii świata i z `cofnij.sh`. Generatory bez `KLASY_ROOT` odmawiają, na world1 tylko z `KLASY_WORLD1=tak`.
- Najpierw kopiuje skille klas z paczki A (`../{wojownik,ninja,mag,akolita,specjalizacje}/` — układ `plugins/`), potem pliki B, potem generatory (generator czyta skille).
- Test jak gracz skilli (integracja 25.10): `scenariusze/klasy/skille.js akolita|ninja|druzyna` (narzędzia eee82b1).
- Skrypt kasuje `skill-trees/combat.yml` i `loop.yml` (B4/B14). Kopiowanie `plugins/.` nie usuwa plików, więc bez tego kroku zostałyby na serwerze.
- Test jak gracz: `narzedzia/bot/run.sh <klon --z-kreatorem> scenariusze/klasy/progresja.js [wojownik,mag,akolita,ninja]`.

## Liczby i skąd się wzięły
**EXP klasy (B1, Z1).** `MMOCore/exp-sources.yml` tabela `klasa-swiat`, w klasach `from{source=klasa-swiat}`.
Źródło: docs mmocore `level-sources.md:34,49,52`.
- Krzywa `level*200` daje do poz. 10 razem 9000 EXP.
- Lochy 001–010 (pierwsze przejście, 0,6×200×N) dają 6600. Świat ma dać resztę, ~2400, i nie może być szybszy od lochu (~40 EXP/min).
- EXP z moba = 0,4×HP ±15% (zając HP 12 → 4–6, wilk alfa HP 170 → 58–78).
- **Moby Bestiariusza nie są dziś nigdzie rozstawione.** Póki nie wejdą spawnery/loch, EXP ze świata = 0.
- Nowy mob Bestiariusza potrzebuje nowego wiersza w tabeli, inaczej nie daje EXP.

**Poziomy (B12).** `max-level: 10` = Akt 1 (decyzja 29.09); max-level rośnie razem z aktem.
- Każdy poziom daje punkt atrybutu i punkt umiejętności, z komunikatem.
- Punkty drzewka na poz. 2, 4, 6, 8, **9**, 10: razem 6 do poz. 10.
- Poz. 10 daje punkt klasy (wybór specjalizacji) i dźwięk.
- Komunikat nie pokazuje numeru poziomu: trigger tabeli liczy się PRZED awansem, więc `%mmocore_level%` pokazywał stary poziom (test 25.10: „Poziom 1!” przy awansie na 2). Numer podaje MMOCore w `messages.yml` („Awans na poziom N!”).

**CDR (B4, Z3).**
- Drzewko `general`: 2/3/5% (było 5/10/15, a z `combat` ~60%).
- INT `cooldown_reduction` 1 → 0,5 za punkt.
- Limit 35 w `MythicLib/stats.yml` (`COOLDOWN_REDUCTION: '0 = 35'`).
- Maksimum w Akcie 1 ≈ 5 (człowiek) + 6 (spec) + 10 (drzewko) + 5 (INT) = 26%. Później limit trzyma 35%.

**Drzewka (B4).**
- Każdy archetyp ma `general` i własne drzewo: Wojownik (`warrior-paladin`), Mag (`mage-arcane-mage`, nowe), Akolita (`akolita-przywolywacz`, nowe, pod sługi) i Ninja (`rogue-marksman`).
- `combat` i `loop` usunięte: odwoływały się do nich tylko szkielety, a wszystkie postaci mają poz. 1.
- `general` h1 = odporność na magię 3%.

**B2/B3 (rogue-marksman, warrior-paladin).**
- KNOCKBACK_RESISTANCE 5 → 0,05 (skala 0–1, mmocore `features-stats.md:15`).
- RELATIVE → FLAT na statystykach procentowych (mythiclib `player-statistics.md:19-20`).
- `ARROW_VELOCITY` to mnożnik prędkości, nie procent (MythicLib 1.7.1, sprawdzone w bajtkodzie). „+10/15/25%” jako FLAT dałoby ×10, więc węzły b1–b3 to teraz Siła krytyka +5/+5/+10% (`CRITICAL_STRIKE_POWER`).

**B5.** `type: CUSTOM` we wszystkich drzewkach. Małe litery też działały (`prettyValueOf`, docs `features-skill-trees.md:12`), to tylko ujednolicenie.

**Zaklęcia (B6, Z4).**
- Bramka w pluginie Zaklecia (`MCMMORPG/plugin-src/zaklecia/README.md`, „Bramka klasy”). Lista klas w `Zaklecia/klasy.yml` pochodzi z generatora: mag i akolita z rasami i specjalizacjami, razem 26.
- `mana-za-tik` 0,5 (TEST) → 2,0.

**Wiara (B7).** `max_mana` 0,5 płasko → 1% RELATIVE.
- Płasko 40 pkt dawało +20: Dusze 10 → 30 (×3), a mana maga 40 → 60.
- Procent skaluje każdy zasób tak samo: 40 pkt = +40% (Dusze 10 → 14, mana 40 → 56).

**Kreator (B8).**
- Akolita: „Przywołuje sługi i włada Duszami.”, karta mówi „Przywoływacz” (decyzja 29.09).
- Podgląd ninja trzyma łuk zamiast żelaznego miecza (ninja nie ma biegłości miecza).

**Dusze (B9).** Specjalizacje akolity z premią `mana`/`regen_many` dostają „większa pula Dusz”: +4 (10 → 14), czyli mniej niż +8 many maga, bo jedna Dusza = jedno przywołanie.

**Ciężka zbroja i tarcza (B10, Z5).** Wojownik każdej rasy nosi lekką, średnią i ciężką zbroję oraz tarczę od poz. 1 (`docs/klasy-rasowe/analiza-spojnosci-2026-10-03.md:81-84`). Specjalizacje obronne nie dają już ciężkiej/tarczy jako nagrody, bo mają je od startu.

**Rasy a zasób (B11)** (`analiza-spojnosci…:19`). Cecha rasy działa na zasób archetypu:

| | wojownik (Szał) | ninja (Skupienie) | mag (mana) | akolita (Dusze) |
|---|---|---|---|---|
| elf | Szał opada 2,5 → 2,0/s | regen 8 → 9,2/s | regen 2,0 → 2,1/s | bez zmian (tylko ruch) |
| ork | Szał z ciosu 8–12 → 6–10 | regen 8 → 6,4/s | pula 40 → 32 | pula 10 → 8 |

**Broń akolity (B13).**
- Nowe berło `PRZ_AKO_BERLO_1..5` (grupa biegłości `bulawa`, `tools/przedmioty/tresc.py`) to druga wspólna broń akolity, tak jak różdżka u maga.
- Lekki akolita widział do poz. 10 tylko 10 broni, inne archetypy 15.
- Id `START_*` bez zmian.

**Sprzątanie (B14).**
- Usunięte: `skill-trees/combat.yml` i `loop.yml`.
- **Zostają**:
  - `classes/human.yml` (`default: true`; bez niego NPE „Could not find class HUMAN”, skill mc-mmo-pluginy);
  - `warrior/mage/paladin/rogue.yml`: to `mmocore-id` archetypów w CharacterCreator i w `CoreTools/Scripts/klasy-rasowe.yml`. Mają `display: false`, więc punktem klasy nie da się ich wybrać.
