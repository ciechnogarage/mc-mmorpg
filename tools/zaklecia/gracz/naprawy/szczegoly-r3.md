# Runda 3 — szczegóły Agenta A

Stan: kod przygotowany, lokalna kompilacja PASS, testy na klonie ZABLOKOWANE — `kolejka/runner.sh` nie działa (ostatni wpis `runner.log` 2026-10-10 03:28:12), a `build.sh` z sandboxa zwraca `permission denied ... /var/run/docker.sock`. Nie przypisuję statusu PASS testom wymagającym serwera. Wolne na `/`: 30 GB, więc blokadą nie jest limit 3 GB.

## Zmiany

- KsiegaPostaci `Bron.damage`: przy graczu anulowanie tylko wtedy, gdy zarejestrowane na celu `AttackMetadata` jest `MeleeAttackMetadata` i wybrano slot inny niż 0/1 albo wirtualny przedmiot. MythicLib 1.7.1 API/bajtkod: `DamageManager.registerAttack` oznacza cel metadanymi na czas `applyDamage` i odznacza po nim; `AttackEventListener` (HIGHEST) dla zwykłego `EntityDamageByEntityEvent` wywołuje `findAttack`, które wytwarza `MeleeAttackMetadata`; `getRegisteredAttackMetadata` daje odczyt w naszym listenerze. `mmodamage` rejestruje własny `AttackMetadata`, więc nie jest prawdziwym ciosem wręcz. [Dokumentacja producenta](https://docs.phoenixdevt.fr/mythiclib/api/damage) opisuje `AttackMetadata` i typy obrażeń.
- Usunięto `plugins-overlay/MythicMobs/skills/zaklecia/{plomienie,odmrozenie}.yml` z odrzuconym `damage{dc=MAGIC}`. Oryginalne YAML z world1 skopiowano także do klona; sumy MD5 są zgodne. Build bazowy nie przywraca już usuniętych plików nakładki, więc ta kopia była konieczna.
- Z `source-r2` przeniesiono `Wybor`, `EfektyCzasowe`, `Zaklecia` oraz `DiagnostykaObrazen`. BossBar jest zamykany przy quit, death i world change. Logi N5 są pod `debug`. `DiagnostykaObrazen` jest konstruowana wyłącznie w `if (getConfig().getBoolean("debug", false))`; przy `false` nie rejestruje listenerów.
- N9: walidacja id przed zmianą ekwipunku i przed `rzucanie.stop` / zdejmowaniem broni; callback opóźnionego LPM sprawdza tożsamość sesji; zmiana świata kończy rzucanie i usuwa runy; koszt many `RzutyOdrzutowe` jest pobierany po potwierdzonym wykonaniu. `Skille` zwraca rzeczywisty `boolean` z `BukkitAPIHelper.castSkill`; `EfektyCzasowe.wystrzel` raportuje wynik targetowania/spawnu.
- N7: dwa `ItemDisplay`: pełny widoczny innym, ukryty przed rzucającym; drugi, krótszy (maks. 1,2 bloku) i węższy (0,42 skali X/Y) ma `visibleByDefault=false` i jest pokazywany tylko rzucającemu. Flaga `strumien.waski-dla-rzucajacego: true` w zasobie i nakładce. Ocena wizualna wymaga klienta Claude.
- N3: odczyt gałęzi `dwie-bronie` (858d6e0) pokazał, że plugin co 5 ticków ponownie zapisuje tylko przedmioty z własnym znacznikiem uzbrojenia w pasku i offhandzie; nie wywołuje `setHeldItemSlot`. To nie potwierdza przyczyny N3. Bez trzech prób C i ZAKDBG nie poprawiałem N3.

## Weryfikacja

- `javac -proc:none -source 21 -target 21` wszystkich źródeł obu pluginów z klasami Paper/MythicLib/MMOCore: exit 0 dla obu (deprecation notes jedynie).
- `git diff --check`: exit 0 w obu worktree.
- Bez commitów. Gałęzi `eq-klon-niewidoczny` nie zmieniano.
- Zlecenia oczekujące: `a-r3-01` build bazowy; `a-r3-02` test `ABCDEF,N_damage,N_invalid,N_melee,N_r1,N_r2,N_reset,N_heal,STAT2`; `a-r3-03` build obu pluginów z nakładką; `a-r3-04` ten sam test. Runner nie pobrał żadnego z nich. Brak tabeli A/B, kontroli skalowania Wiara 1,60×, sond FAIL→PASS, prób C 3×, logów ZAKDBG i końcowego builda `debug:false` na klonie. Nakładka obecnie ma `debug:false`.

## Ograniczenia do ponownej oceny po starcie runnera

- Rozpoznanie `MeleeAttackMetadata` ma potwierdzenie w API i bajtkodzie zainstalowanego JAR, lecz wymaga empirycznej kontroli: miecz slot 5 = 0, slot 0 zadaje obrażenia, wszystkie typy zaklęć działają, STAT2 zachowuje 1,60×.
- `N_invalid` sprawdza oba warianty niepoprawnego id; pozostałe scenariusze N9 wymagają jeszcze sond na klonie, więc nie są oznaczone jako PASS.
- N7 wymaga oceny prawdziwym klientem; kompilacja potwierdza API `setVisibleByDefault`/`showEntity`, ale nie widok.
