# Agent A — szczegóły rundy 2

## Środowisko

Polecenie: `POLECENIE-A-r2.md`. Worktree źródłowy
`/home/dev/Desktop/Przemek/MC/.claude/worktrees/zaklecia-naprawy` jest w tej
sesji tylko do odczytu; jedyne writable roots to `gracz/` i `/tmp`.
Kolejka `build` buduje wyłącznie z tego worktree. Zmiany w Javie są na razie
przygotowane w `naprawy/source-r2/` i nie stanowią przetestowanej poprawki.

## N1

`a-r2-01: build bazowy` OK (`7f197e701920`). Pierwsze `a-r2-02:
test N_r1` było nieważną kontrolą: BLAD przy przypisaniu wilka przez
menu, zanim zaszło PvP. Sonda używa teraz bezpośredniej komendy Zaklęć.
`a-r2-03: build` OK na commicie `24cc940` z nakładką
(`5d7400d4e847`); `a-r2-04: test N_r1` ma N1-PvP-wilk PASS
(HP MagCel 20→15), N3 obie izolowane próby PASS, N4 FAIL. Zaplanowano
powtórną kontrolę bazowy `a-r2-06/07` → bieżący `a-r2-08/09`.

## N2

Runda 1: cel stożka i skill przechodziły, zdarzenie
`EntityDamageByEntityEvent(ENTITY_ATTACK, 3.5)` bywało anulowane. W
`source-r2/DiagnostykaObrazen.java` przygotowano sondę DEBUG rejestrującą
listener na LOWEST, LOW, NORMAL, HIGH, HIGHEST i MONITOR
(`ignoreCancelled=false`) oraz spis `HandlerList`: plugin, priorytet,
klasa listenera. Wymaga zbudowania z worktree, przebiegu C i identyfikacji
konkretnego anulującego listenera, zanim powstanie poprawka.
Ponieważ worktree jest tylko do odczytu, zbudowano też tymczasowy,
odrębny plugin `zaklecia-diag-0.1.jar` z identyczną sondą i włożono go
do nakładki. Zostanie użyty tylko na klonie przy `a-r2-08/10`, a potem
usunięty z nakładki i klona przez `a-r2-11: build`
(`zaklecia-*.jar` usuwa również `zaklecia-diag-0.1.jar`);
nie jest poprawką produkcyjną.

## N3

`Nosnik.odswiezPusta` tworzy DEBUG_STICK tylko przy `null/AIR` w wybranym
slocie i sam nie wybiera innego slotu. Scenariusz C używa
`bot.clickWindow`, `k.cyfra(2)` i awaryjnego przeszukania całego hotbara,
gdy `SelectedItem` serwera nie dogonił klienta. Nadal wymaga odtworzenia w
`test C` i ustalenia, który konkretnie zapis ekwipunku/slotu psuje stan.
`a-r2-01b`: C 10 PASS/12 FAIL; `a-r2-05`: C 9 PASS/13 FAIL.
`C-narzędzie` zmieniło PASS→FAIL: bezpośrednio po wyborze
`SelectedItem=iron_pickaxe`, po PPM `SelectedItem=debug_stick`,
`slotPo=0`. Log `ZAKDBG slot` potwierdza zdarzenia zmiany slotu
między 0 a 5, a `ZAKDBG pusta` tworzy nośnik tylko przy
`przed=null`. Potrzeba powtórzenia C i śledzenia pakietu wyboru slotu,
zanim przypiszemy winę pluginowi albo botowi.

## N4

`Wybor.zamknij` wywołuje `hideBossBar` dla obu pasków przy Q/Shift+Q i
limicie czasu, lecz `wyjscie` usuwało sesję bez hide, a śmierć i zmiana
świata nie zamykały paska. Kopia źródła zawiera zamknięcie dla tych zdarzeń
oraz log `ZAKDBG boss open/close` do diagnozy brakującego REMOVE.
W `a-r1-19` 7 identyfikatorów MythicHUD otrzymywało po 45 pakietów ADD.
Licznik 135 przypisany wcześniej Zaklęciom jest zawyżony przez
klasyfikację sondy po treści tytułu; nie jest miarą ruchu dwóch pasków.
Bazowy `a-r2-01b`: `N4-bossbar-raw FAIL`, dwa paski wybrane
po tytułach, zero akcji REMOVE po Q. Po Q te same dwa identyfikatory
otrzymały `action=0` z pustym tytułem. Potwierdzenie z bajtkodu
`MythicHUD-1.3.4-SNAPSHOT-all.jar`,
`v1_21_11_BossBarHandler`: `handle` po REMOVE usuwa pasek z
`passthroughBars`, po czym asynchronicznie wywołuje `update`; `update`
wysyła na własne ID pakiet `BLANK`, a `BLANK` jest `Operation.ADD`.
MythicHUD ma `boss-bar.max: 7` i stale emituje 7 własnych ID.
Zatem literalny REMOVE dla ID obserwowanych przez bota nie wynika z
`Wybor.hideBossBar`; wymagałby zmiany zachowania MythicHUD albo kryterium
sondy (pusty ADD jako zniknięcie treści). Nie zmieniamy cudzego JAR.
`a-r2-05` z obecną nakładką potwierdza ten sam N4 FAIL.

## N5

`EfektyCzasowe.wystrzel` wybiera sojusznika i przekazuje go do
`Skille.cele`; MythicMobs skill ma `heal{a=8}`. Przebieg `a-r1-18` zgłosił
`B-uzdrowienie-innego-cel PASS` (HP 14,1→20). Starsze
`e3-18/e3-19` miały FAIL z przyrostem HP tylko 0,3–0,47, typowym dla
regeneracji. Przygotowano `N_heal` (3 kolejne próby) oraz logowanie
celu przy `przygotuj` i `wystrzel` w kopii źródeł; potrzebna jest
powtarzalna czerwona próba i diagnostyka wskazania celu. Bazowy
`a-r2-01b`: `B-uzdrowienie-innego-cel FAIL` (18→18), `N_heal`
3/3 FAIL, HP rosło tylko o 0,3–0,4 i mana pozostawała 20.
Przyczyna z bajtkodu MythicLib `EntityManager.canInteract`:
dla celu-gracza `SUPPORT_ACTION` przechodzi przez
`checkPvpInteractionRules`. W klonie
`MythicLib/config.yml:316–319` miało
`pvp_on.support.party_other: false`, więc MagCel poza drużyną nie
jest dopuszczanym celem przy PvP ON. Nakładka zmienia tylko tę wartość
na `true`; `guild_enemy: false` pozostaje. Kontrola `N_heal`
wymusza PvP ON i przywraca deny na końcu. Zaplanowano bazowy
`a-r2-06b` i wersję z nakładką `a-r2-10`.
Ryzyko: zmiana MythicLib dotyczy wszystkich umiejętności wsparcia,
więc pozwala leczyć także graczy spoza drużyny w obszarach PvP, jeśli
inna relacja tego nie zabrania. Potrzebna ocena Claude przed wdrożeniem
na world1.

## N6

Nakładka `DeluxeMenus/gui_menus/ksiega_zaklec.yml` dodaje `[refresh]`
bezpośrednio po wszystkich 32 akcjach `zaklecia przypisz` (LPM/PPM).
DeluxeMenus dokumentuje `[refresh]` jako natychmiastowe odświeżenie
pozycji bieżącego menu:
https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/actions .
Własna sonda `N_r2` sprawdza treść slotu 38 w otwartym oknie.
Bazowy `a-r2-01b`: N6 FAIL po 208 ms. Z nakładką `a-r2-05`:
N6 PASS po 207 ms, panel slotu 38 zawierał Płomienie i okno
pozostało otwarte; sonda naliczyła 3 pakiety `set_slot` do panelu.
Po `a-r2-03` na klonie są wszystkie 32 akcje `[refresh]`;
w logu boot nie stwierdzono błędu DeluxeMenus.

## N8

`MMOCore/messages.yml:152` zawierało
`now-in-combat: '%&cYou are now in combat!'`. Nakładka zmienia wyłącznie
tekst na `%&cJesteś teraz w walce!`.
Po `a-r2-03` klon ma tę linię w `plugins/MMOCore/messages.yml`.

## Weryfikacja do uzupełnienia

Porównanie bazowy/nowy: bazowy `a-r2-01b` ma 55 PASS, 63 FAIL,
1 SKIP; bieżący `a-r2-05` ma 52 PASS, 66 FAIL, 1 SKIP
(ABCDEF + N_heal + N_r2). N6 przeszedł FAIL→PASS,
`B-cel-gracz` też przeszedł FAIL→PASS bez zmiany tego mechanizmu.
Nowe FAIL względem bazowego: `B06-runa_ognia-prawa/oburacz`,
`B12-uspokojenie-prawa`, `B14-zasklepienie_ran-prawa`
(wszystkie przez FEEL latencyTicks 14–33 przy poprawnym koszcie/efekcie),
oraz `C-narzędzie` (SelectedItem wrócił na nośnik; po wyborze
kilof był wybrany poprawnie). To pozostaje potencjalną regresją,
dopóki kolejny przebieg C nie wykaże niestabilności harnessu.
Ostatni build musi ponownie
zainstalować wersję Agenta A; nie zostawiać klona na bazowym JAR.

## Aktualizacja 02:00 UTC — ustalenia i następny przebieg

`a-r2-10` potwierdził N5: `N_heal` 3/3 PASS, przyrost HP 5,9–6,0 i koszt many 2. Bazowy `a-r2-06b`: 3/3 FAIL, jedynie regeneracja pasywna i mana bez zmian. `a-r2-11 build` wykonał się (kod 0), usuwając tymczasowy plugin diagnostyczny z klona.

N2: tymczasowa sonda na każdym priorytecie w `a-r2-10.serwer.log` dla eventów obrażeń gracza→husk wykazała HIGH `canceled=false`, HIGHEST `canceled=true`, MONITOR `canceled=true` (np. ID 1731050717). Bajtkod `pl.mcmmorpg.ksiega.Bron.damage` z JAR `ksiega-postaci-0.1.0.jar` anuluje `EntityDamageByEntityEvent`, gdy wybrany slot to nie 0 ani 1; mmodamage czaru jest `ENTITY_ATTACK` z graczem jako sprawcą. To bezpośrednia przyczyna wyzerowania obrażeń przy slocie 5/6. W nakładce eksperymentalnie zmieniono tylko 4 trafienia Płomieni i Odmrożenia z `mmodamage{...}` na `damage{a=...;dc=MAGIC}`; `a-r2-12 build`, `a-r2-13 test C,N_r1,N_heal,N_r2`, `a-r2-14 test N_damage` sprawdzą, czy to omija filtr i zachowuje efekt zaklęcia. Ta zmiana może ominąć mnożniki MythicLib i wymaga oceny wyniku, zanim zostanie uznana za poprawkę.

`a-r2-10 test C` nadal FAIL dla `C-narzędzie` oraz licznych innych kroków; log `ZAKDBG pusta` występuje tylko przy `przed=null`, a przełączenia slotów mają osobne eventy `PlayerItemHeldEvent`. Zmiany `source-r2/` nie są zastosowane w worktree źródłowym. N4 nadal FAIL z powodu przechwycenia REMOVE przez MythicHUD. Skorygowana sonda N1/N8 ustawia miecz PvP w slocie 0, żeby filtr `KsiegaPostaci.Bron` nie anulował kontrolnego ciosu. Zaplanowano `a-r2-15 build bazowy`, `a-r2-16 test N_damage,N_r1`, `a-r2-17 build`, `a-r2-18 test ABCDEF,N_damage,N_r1,N_heal,N_r2`; ostatni build ma pozostawić wersję Agenta A.

Bajtkod `Bron.damage(EntityDamageByEntityEvent)` został ponownie odczytany: gdy `getDamager()` jest `Player`, metoda sprawdza `getHeldItemSlot()`; dla wartości innych niż 0 i 1 skacze prosto do `setCancelled(true)` (offsety 23–43, 64–66). Dla slotu 0/1 anuluje, jeśli `virtual(mainhand)` jest prawdziwe. To potwierdza, że filtr dotyczy także obrażeń zaklęcia z graczem jako sprawcą, bez względu na tag `SKILL`.

Dokumentacja MythicMobs potwierdza, że `damage` obsługuje `damagecause=dc`, `tags` i `element`, a przy `MAGIC` nie zwraca sprawcy jako encji: https://git.lumine.io/mythiccraft/MythicMobs/-/wikis/Skills/Mechanics/damage/diff?version_id=1f8283a3389908f340de3fcb5bc030ebbd4ae456 . Nakładka N2 zachowuje więc tagi `SKILL,MAGIC` oraz elementy `ICE/FIRE`; testy muszą jeszcze potwierdzić działanie na serwerze. Mechanika MythicMobs może inaczej liczyć obrażenia niż `mmodamage` MythicLib.

Uwaga o bazowym buildzie: `runner.sh` w `build bazowy` podmienia tylko JAR Zaklęć i jego config, nie cofa nakładek innych pluginów. Dla uczciwej kontroli N2 po `a-r2-15` dodano `a-r2-15b test N_reset`: przywraca na klonie oba oryginalne pliki skilli MythicMobs i wykonuje `mythicmobs reload`; potem `a-r2-16` testuje bazowe N2/N1/C. `a-r2-17 build` ponownie kopiuje całą nakładkę N2/N5/N6/N8, po czym `a-r2-18` uruchamia końcowy zestaw.

Korekta procedury bazowej: bot działa w izolowanym kontenerze z montażem tylko `/work`, więc scenariusz `N_reset` nie może sam kopiować plików serwera. Zlecenia `a-r2-15b..18` zostały na razie wycofane z kolejki. Po `a-r2-15.done` agent ręcznie przywróci oryginalne dwa YAML na klonie, zleci `N_reset` do przeładowania MythicMobs, a następnie odtworzy `a-r2-16..18`. Pozwoli to zachować prawidłową kontrolę A/B.

## Wyniki `a-r2-12..14` i korekty sond

`a-r2-12 build` OK, JAR 24cc940 + nakładka (md5 `35a17b265e15`). `a-r2-13`: N1 PASS (PvP z wilkiem HP 18→10,1), N3 izolowane 2/2 PASS, N6 PASS, N4 2×FAIL. `C-narzędzie` PASS na tej samej nakładce, na której `a-r2-05` i `a-r2-10` miały FAIL; wynik zależy od przebiegu scenariusza C. N5 w tym mieszanym przebiegu 3/3 FAIL, mimo że `a-r2-10` po samej zmianie MythicLib miało 3/3 PASS. Pierwsza próba zużyła manę, lecz nie podniosła HP celu; dwie pozostałe nie miały skutecznego rzutu. Potrzebny powtórny izolowany przebieg N_heal.

`a-r2-14 N_damage` był nieważny jako sonda N2: czar przypisano do prawej dłoni, a wysłano PPM; mana została 20 i żaden czar nie został uruchomiony. Scenariusz poprawiono: czar w lewej dłoni, PPM, miecz w aktywnym slocie 5; będzie uruchomiony jako `a-r2-14b` po obecnym bloku testów B. N8 w `a-r2-13` FAIL bez pakietów komunikatu, mimo że config ma polski tekst; `MMOCore/config.yml` ma `combat_timeout: 30`, a wcześniejszy C mógł pozostawić gracza w walce. Sonda N_r1 czeka teraz 32 s przed kontrolnym ciosem PvP. `a-r2-14b` powtórzy N_damage, N_heal i N_r1.

`a-r2-14b` po poprawie gestu potwierdził N2: Płomienie w lewej dłoni, miecz w aktywnym slocie 5: husk 20→8, mana 20→18,7; Odmrożenie identyczny układ: husk 20→9,5, mana 20→18,7. N1 ponownie PASS; po 32 s bezczynności (combat_timeout 30 s) N8 PASS — komunikat polski widoczny w pakietach. N5 3/3 FAIL w tym mieszanym przebiegu; mana 20 w dwóch próbach. W N_heal sonda ustawia teraz jawnie pusty slot 0 i wybiera go przed przypisaniem czaru do lewej dłoni; `a-r2-14c` sprawdzi, czy wcześniejszy stan wybranego slotu psuł próbę. N2 nie ma jeszcze kontroli bazowej z oryginalnymi YAML; ta czeka po testach Agenta B.

## Pełny przebieg `a-r2-18` i A/B

`a-r2-17 build` OK, po czym `a-r2-18 test ABCDEF,N_damage,N_r1,N_heal,N_r2` zakończył się kodem 1 z powodu pozostałych FAIL. Własne sondy: N1 PASS, N5 3/3 PASS (po ustabilizowaniu wybranego pustego slotu), N6 PASS, N8 PASS, N4 oba FAIL. W N2 surowe metryki dowodzą obrażeń: Płomienie HP 20→8 i Odmrożenie 20→9,5, przy manie 20→18,7. Raport sondy błędnie oznaczył je FAIL, ponieważ zadeklarował próg `hpDelta`, ale nie podał tej metryki w obiekcie; poprawiono N_damage.js i zaplanowano powtórzenie A/B z tą samą wersją sondy.

ABCDEF, identyczne identyfikatory 111 testów: bazowy `a-r2-01b` 53 PASS / 57 FAIL / 1 SKIP; bieżący `a-r2-18` 56 PASS / 54 FAIL / 1 SKIP. Zmiany statusu: `B-cel-gracz`, `B-uzdrowienie-innego-cel`, `B07-oslona-oburacz`, `B08-przywolany_miecz-oburacz` FAIL→PASS; `C-narzędzie` PASS→FAIL. Ten ostatni nie jest stabilną regresją: w powtórce na bazowym `a-r2-16` też FAIL, a na nakładce `a-r2-13` PASS. Wymaga osobnej diagnozy C. Brak innych nowych FAIL w ABCDEF.

N1 kontrola ujemna `a-r2-16`: stary JAR z wilkiem po ciosie PvP stracił dostępność; `hp1=NaN`, RCON zgłaszał błąd asynchroniczny, klon zatrzymał się; następny test nie mógł rozwiązać nazwy hosta. Kontrola dodatnia na commicie 24cc940 `a-r2-14b` i `a-r2-18` PASS, HP celu maleje, serwer pozostaje dostępny. `a-r2-16c build bazowy` uruchomił klon ponownie.

N2 bazowy po przywróceniu oryginalnych dwóch YAML i `mythicmobs reload`: `a-r2-16d` surowe HP Płomienie 20→17 (tylko podpalenie), Odmrożenie 20→20, mana obu 20→18,7. W tym przebiegu zarówno bazowy, jak i poprawiony zostały oznaczone FAIL przez wspomniany brak pola `hpDelta`; stąd potrzeba ostatniego powtórzenia sondy po dodaniu metryki.

## Zakończenie N2 i stan klona

Po poprawce metryki `hpDelta` wykonano tę samą sondę po obu stronach. `a-r2-19 build bazowy` OK; dwa oryginalne pliki skilli MythicMobs skopiowano na klon z sumami SHA-256 `3931abf8b672362b` (Odmrożenie) i `6a369981a41a2f9e` (Płomienie). `a-r2-19b N_reset` zwrócił kod 1, ponieważ klient RCON przekroczył timeout odpowiedzi, ale log serwera potwierdził `Mythic has finished reloading!` o 05:21:35. `a-r2-20 test N_damage`: N2-Płomienie FAIL, HP 20→17, `hpDelta=3` przy progu >5; N2-Odmrożenie FAIL, HP 20→20, `hpDelta=0` przy progu >1. Oba zużyły manę 20→18,7.

`a-r2-21 build` OK, HEAD `24cc940`, overlay skopiowany, klon start OK, JAR md5 `dcf29adb5290`. `a-r2-22 test N_damage` kod 0: Płomienie PASS HP 20→8 (`hpDelta=12`), Odmrożenie PASS HP 20→9,5 (`hpDelta=10,5`), mana obu 20→18,7, aktywny slot 5 z mieczem, zaklęcie w lewej dłoni. To jest formalny FAIL→PASS N2. Tymczasowy `zaklecia-diag-0.1.jar` został usunięty z nakładki i nie ma go w klonie. Ostatni build Agenta A to `a-r2-21`, bez commita. N3 i N4 pozostają nierozwiązane; zmiany Java w kopii źródła nie są w tym JAR.
