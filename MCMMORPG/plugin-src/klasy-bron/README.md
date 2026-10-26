# KlasyBron (`plugin-src/klasy-bron`)
Wersja w źródle: `0.2.0-${rev}` (`gradle.properties:1`, `build.gradle.kts:29`, plugin.yml:2 = `${version}`). Jar na world1 (stan 26.10): `klasy-bron-0.1.0-202610020918.jar`; 0.2.0 czeka na wdrożenie w paczce `/opt/docker/mc/backups/klasy-poprawki2-20261025/p1p2/` (gałąź `klasy-p1p2`).
Opis stanu z dnia: 2026-10-26 (gałąź klasy-p1p2, paczki P1+P2 planu `klasy-poprawki2-20261025/PLAN.md`).

## Po co (co widzi gracz)
Plugin daje skillom klas rasowych (MythicLib/MythicMobs) trzy warunki MythicMobs i jeden listener:
- `bronRasowa{typ=..}`: premia sygnatury tylko z bronią MMOItems danego typu w ręce (KlasyBron.java:61-70). Zastępuje placeholder Crucible `<caster.item.hand.nbt.MMOITEMS_ITEM_TYPE>`, który po restarcie jest "Invalid Placeholder" do `mm reload`.
- `druzyna{}`: automatyczne wsparcie (Okrzyk, Straż Relikwii, aury specjalizacji) trafia tylko w rzucającego i członków jego drużyny MMOCore. Obcy stojący bliżej nie dostaje buffa ani obrońcy (decyzja D2=A; celowane leczenie, np. Opieka, zostaje dla każdego).
- `celWrogi{}`: wspólny filtr ofensywy. Skill nie rani wieśniaka, NPC Stolicy (MM Invincible, tag `npc`, Citizens `NPC`), siebie, swoich ani sojuszniczych sług; sługi wroga są celem tylko tam, gdzie wolno bić ich właściciela (PvP), decyzja D3=A.
- Osłony (absorpcja) skilli nie zostają w danych gracza po wyjściu z gry lub zmianie postaci (F2).

## Komendy i uprawnienia
Brak. plugin.yml nie ma sekcji `commands` ani `permissions` (`src/main/resources/plugin.yml`).

## Konfiguracja
Brak configu (brak `saveDefaultConfig`, brak `plugins/KlasyBron/`). Konfiguracja jest w skillach, które wołają warunki:
- `bronRasowa{typ=SHIELD,STAFF}` / `t=`: wartości po przecinku, bez rozróżniania wielkości liter, puste pomijane (KlasyBron.java:62-64).
- `druzyna{}` i `celWrogi{}` bez parametrów. Użycie: targeter + `?druzyna{}` / `?celWrogi{}` w linii mechaniki albo w `TargetConditions`. Warunki inline liczą się przed `limit=` targetera (filtrują, potem obcinają), więc `@EntitiesNearOrigin{r=6;limit=1} ?celWrogi{}` wybiera najbliższego wroga, a nie najbliższą encję.
- Źródła YAML (repo): `tools/klasy-rasowe/{akolita,mag,ninja,wojownik,specjalizacje}/MythicLib/skill/*.yml` i ich `MythicMobs/skills/*`. Spójność lore z wartościami sprawdza `tools/klasy-rasowe/sprawdz_lore.py`.

## Jak działa
- `MythicConditionLoadEvent` (KlasyBron.java:50-70) rejestruje warunki po nazwie (bez rozróżniania wielkości liter). Nieznana nazwa warunku w MM 5.11.2 przepada po cichu (warunek = fałsz bez logu), dlatego literówka w YAML = skill nic nie robi.
- `druzyna{}` (Warunki.java:43-58): caster gracz albo właściciel moba-castera (`ActiveMob.getOwner`); cel musi być graczem. Prawda dla samego siebie i dla członka tej samej drużyny MMOCore (`PlayerData.getParty().hasMember`). Bez MMOCore: tylko sam rzucający.
- `celWrogi{}` (Warunki.java:60-88), kolejno:
  1. nie żywa / martwa / typ z `NIE_CEL` (wieśniak, handlarz, armor stand itd., Warunki.java:33) albo metadane `NPC` / tag `npc` -> fałsz;
  2. cel = rzucający -> fałsz;
  3. cel gracz -> `UtilityMethods.canTarget(gracz, cel, OFFENSE_SKILL)` (MythicLib: PvP świata, `DamageCheckEvent`, `interaction_rules` z `MythicLib/config.yml`); caster-mob bez właściciela -> prawda;
  4. vanilla mob -> prawda; MM `Invincible` -> fałsz; MM spoza frakcji gracza -> prawda;
  5. mob frakcji gracza (`chowance`, `akolita_slugi`, `zaklecia_przywolane`, Warunki.java:30): właściciel offline -> prawda; właściciel = rzucający -> fałsz; właściciel w drużynie rzucającego -> fałsz (D3=A, mimo że MythicLib `pvp_on.offense.party_member: true` pozwala na friendly fire drużyn); inaczej `canTarget(gracz, właściciel)`.
  - `canTarget` woła synchroniczne zdarzenie Bukkit, więc z wątku asynchronicznego skilla idzie przez `callSyncMethod` (Warunki.java:94-106); błąd/przerwanie = cel odrzucony (bezpieczniej niż cios).
- Osłony (Oslony.java, OslonyProfile.java): MM `shield`/absorpcja podnosi bazę atrybutu `max_absorption` i jej nie obniża; vanilla zapisuje bazę i absorpcję w danych gracza. `zdejmij()` (Oslony.java:36-41) przywraca domyślną bazę `max_absorption` i obcina absorpcję do poziomu z efektu ABSORPTION (napar/złote jabłko zostaje). Wołane: `PlayerQuitEvent` LOWEST, `PlayerJoinEvent` MONITOR (+1 tick), MMOProfiles `ProfileUnloadEvent` LOWEST / `ProfileSelectedEvent` MONITOR (rejestrowane tylko, gdy MMOProfiles jest włączony, KlasyBron.java:37-41).
- Zależności: twarde MythicMobs, MythicLib; miękkie MMOCore (drużyny), MMOProfiles (zmiana postaci) (plugin.yml:5-6). Kompilacja na pinowanych jarach world1: MythicLib-dist-1.7.1, MythicMobs-5.11.2, MMOCore-1.13.1, MMOProfiles-1.2 (`build.gradle.kts:15-18`).

## Koszt dla serwera
Brak zadań okresowych i zapisów. Wszystko liczone na żądanie przy wykonaniu skilla:
- `bronRasowa`: odczyt NBT 1-2 itemów.
- `druzyna`: odczyt `PlayerData` i zbioru członków drużyny (mapa w pamięci MMOCore).
- `celWrogi`: kilka porównań + `getActiveMob` (mapa MM) + dla gracza/sługi gracza jedno `canTarget` (jedno zdarzenie Bukkit). Z wątku async dochodzi przeskok na główny wątek (czeka do końca bieżącego ticku).
- Osłony: jedno sprawdzenie atrybutu przy wejściu/wyjściu/zmianie postaci.
Przy 1/20/100 graczach koszt rośnie liniowo z liczbą rzutów i celów w zasięgu AoE (typowo 1-8 celów), bez stałego tyknięcia. Pomiar 26.10 na klonie `klasy-p1p2` (2-3 boty rzucające co ~1 s, `mspt` co 5 s): mediana 1,5 ms/tick, p95 5,9 ms (13 próbek).

## Dane
Nie zapisuje nic własnego. Osłony poprawiają atrybut i absorpcję gracza (vanilla, zapisywane przez serwer).

## Znane pułapki i decyzje
- Arena `pvp-poza-ringiem: strefy` anuluje `DamageCheckEvent` MythicLib poza regionami WG z `pvp: allow` (`plugin-src/arena/.../ArenaPlugin.java:411-427, 465`), więc `celWrogi` poza strefą PvP nie przepuści gracza ani sługi wroga. To zamierzone; w teście PvP trzeba flagi WG.
- `mmoCanTarget` w MythicLib z mobem jako casterem zawsze fałsz; dlatego `celWrogi` sam ustala gracza-właściciela moba.
- Decyzje właściciela: D2=A, D3=A (`klasy-poprawki2-20261025/PLAN.md`). Historia warunku `bronRasowa`: `~/.claude/skills/mc-walka-klasy-zaklecia/SKILL.md`, `stan.md`.
- Testy: scenariusze botów `/opt/docker/mc/narzedzia/bot/scenariusze/klasy/p1p2.js` (`wsparcie`, `oslony`, `skalowanie`, `pochylenie`) i `skille.js druzyna`. Testów jednostkowych brak.

## Budowanie i wdrożenie
`./build.sh` w katalogu pluginu: kontener `gradle:8.12-jdk21`, `--memory 2g`, jary world1 montowane tylko do odczytu pod `/serverplugins` (`build.sh`, `gradle.properties:2`); nazwa wersji = `0.2.0-<RRRRMMDDGGMM>`. Wynik: `build/libs/klasy-bron-<wersja>.jar` do `plugins/` world1 (kopia + `cofnij.sh`; stary jar 0.1.0 usunąć, dwa jary KlasyBron naraz = konflikt nazwy). Razem z jarem trzeba wdrożyć YAML skilli używające `druzyna{}`/`celWrogi{}`: stary jar + nowe YAML = warunki nieznane = skille nie trafiają nikogo.
