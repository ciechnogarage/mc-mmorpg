# Mistrzostwa (`plugin-src/mistrzostwa`)
Wersja w źródle: `0.1.0-<mstRev>` (`gradle.properties`, `build.gradle.kts:31`, `plugin.yml:3` ma `${version}`). Jar na world1: `/opt/docker/mc/data/world1/plugins/mistrzostwa-0.1.0-202610121016.jar` (wdrożony 12.10 14:43).
Zgodność źródła z jarem: zgodna w zakresie, który sprawdziłem: ten sam zestaw 8 klas, `config.yml` z jaru identyczny ze źródłem i z plikiem na world1 (`diff`), wszystkie 11 podkomend ze źródła występują w `Komenda.class`; ostatni commit katalogu (0b4dcda, 12.10 12:00) jest sprzed budowy jaru. Bajtkodu nie porównywałem.
Opis stanu z dnia: 2026-10-25, commit 8c79ff8.

## Po co (co widzi gracz)
Postać rozwija wszystkie profesje swobodnie do poziomu `prog` (50). Powyżej rosną tylko profesje, które gracz wybrał jako „mistrzostwo” (maks. `limit` = 2 naraz, z 11 mistrzowskich), oraz dwie „ogólne” (gotowanie, budownictwo — do 100 bez wyboru). Wybór mistrzostwa robi się z menu profesji (PPM na kafelku, okno „Czy na pewno?”); zmiana (porzucenie jednego dla drugiego) kosztuje 2500 $ i ma cooldown 168 h; pierwszy wybór (do `limit`) jest darmowy. Wybór/zmiana nigdy nie zmienia poziomu ani XP. Plugin nadaje też tytuły (Biegły/Mistrz/Arcymistrz/Legenda + dopełniacz profesji) z poziomu i ogłasza je na czacie od poziomu 75.

## Komendy i uprawnienia
`plugin.yml` deklaruje 1 komendę (`mistrzostwo`, `plugin.yml:8-12`) i 1 uprawnienie `mistrzostwa.admin` (default op, `plugin.yml:13-16`). Komenda przyjmuje tylko nadawcę gracz/konsola/RCON (inny, np. blok komend, dostaje komunikat; `Komenda.java:66-70`), a barierą jest uprawnienie `mistrzostwa.admin`; menu woła komendę z konsoli (`[console] mistrzostwo okno %player_name% mining`, `…/DeluxeMenus/gui_menus/profession_main.yml:28`). Podkomendy (`Komenda.java:72-100`, 11 sztuk; `plugin.yml:usage`, `pomoc()` `:105-109` i tab-complete `:292-314` obejmują wszystkie 11):
| podkomenda | kto | co robi | źródło |
|---|---|---|---|
| `okno <gracz> <prof>` | konsola (menu) | PPM na kafelku: odmowa (ogólna/nie mistrzowska/brak profilu/poziom < prog/już mistrz) albo `dm open mistrz_wybor` (gdy `< limit`) lub `mistrz_zmiana`; zapamiętuje cel okna per gracz i profil | `Komenda.java:113-126` |
| `porzuc <gracz> <nr>` | menu | wskazanie, które z mistrzostw 1..n porzucić → `mistrz_zmiana_potw` | `Komenda.java:128-138` |
| `potwierdz <gracz>` | menu | przycisk „Tak”: `wybierz` albo `zmien` na zapamiętanym celu | `Komenda.java:140-147` |
| `wybierz <gracz> <prof>` | menu/admin | dodaje mistrzostwo (walidacja: poziom ≥ prog, limit, nie ogólna) | `Komenda.java:151-164` |
| `zmien <gracz> <z> <na>` | menu/admin | walidacja, cooldown, saldo Vault, pobranie `zmiana.koszt`, zamiana węzłów; zwrot pieniędzy gdy zapis się nie uda | `Komenda.java:168-208` |
| `info <gracz>` | admin | profil, mistrzostwa, cooldown, saldo, ostrzeżenie o węzłach bez kontekstu | `Komenda.java:233-242` |
| `nadaj <gracz> <prof>` / `odbierz <gracz> <prof>` | admin | dodaje/usuwa węzeł bez kontroli poziomu, limitu i kosztu (log `ADMIN …`) | `Komenda.java:85-97` |
| `kamien <gracz> <prof> <poziom>` | MMOCore exp-table | ogłasza tytuł (czat serwera od `ogloszenie-od`, niżej wiadomość do gracza) | `Komenda.java:221-231`; wywołanie: `MMOCore/exp-tables/mistrzostwa.yml:17` (`command{format="mistrzostwo kamien %player_name% mining 60"}`) |
| `zdejmij-cooldown <gracz>` | admin | usuwa węzeł cooldownu | `Komenda.java:99`, `Stan.java:147-156` |
| `reload` | admin | `reloadConfig` + nowa migawka `Ustawienia` + ponowna kontrola profesji | `Komenda.java:73`, `Mistrzostwa.java:67-72` |
Podwójne kliknięcie: blokada 1,5 s po udanej operacji (`Komenda.java:246-249`). PlaceholderAPI `%mistrz_…%` (identyfikator `mistrz`): `liczba, limit, koszt, cooldown, cooldown_godziny, saldo, lista`, `stan_<id>` (rozwoj/do_wyboru/do_zmiany/mistrz/wstrzymana/ogolna/brak), `max_<id>`, `opis_/akcja_/nazwa_/ikona_/poziom_/tytul_/znak_<id>`, `cel_*`, `z_*`, `aktywne_[nazwa|poziom|ikona_]<n>` (`Placeholdery.java:14-24`, `:57-157`).

## Konfiguracja
Plik: `/opt/docker/mc/data/world1/plugins/Mistrzostwa/config.yml` (identyczny z domyślnym; 12 kluczy najwyższego poziomu: `prog, limit, ogolne, mistrzowskie, zmiana, kontekst, komunikaty, tytuly, dopelniacz, ogloszenie-od, menu, sabotaz-testowy`). Wszystkie czytane w `Ustawienia.java:29-51`; wartość dziś = domyślna w kodzie, o ile nie podano inaczej.
| klucz | znaczenie / wartość world1 | źródło |
|---|---|---|
| `prog` | 50 — poziom, na którym staje profesja bez mistrzostwa | `Ustawienia.java:30`, `BlokadaXp.java:54` |
| `limit` | 2 — maks. mistrzostw naraz | `Ustawienia.java:31` |
| `ogolne` | cooking, building — rosną do max-level bez wyboru | `Ustawienia.java:32` |
| `mistrzowskie` | 11 profesji (mining … enchanting) podlegających blokadzie; profesje spoza obu list (np. poskramiacz) są ignorowane | `Ustawienia.java:33`, `BlokadaXp.java:44` |
| `zmiana.koszt` / `zmiana.cooldown-godziny` | 2500 ($ Vault = CoreTools „dollar”) / 168 | `Ustawienia.java:34-35` |
| `kontekst` | nazwa kontekstu LuckPerms: `profil` | `Ustawienia.java:36`, `KontekstProfilu.java:37`, `Stan.java:67` |
| `komunikaty.prefiks/blokada/blokada-co-sekund` | prefiks czatu, tekst paska przy zablokowanym XP (`{profesja}`,`{prog}`), odstęp 20 s | `Ustawienia.java:37-39`, `BlokadaXp.java:65-78` |
| `tytuly` (60/75/90/100) + `dopelniacz` (13) + `ogloszenie-od` (75) | tytuł = `floorEntry(poziom)` + dopełniacz; ogłoszenie na czacie od 75 | `Ustawienia.java:43-47`, `:62-66` |
| `menu.glowne/wybor/zmiana` | okna DeluxeMenus: `profession_main`, `mistrz_wybor`, `mistrz_zmiana` (`…_potw` doklejane) | `Ustawienia.java:40-42`, `Komenda.java:123,120` |
| `sabotaz-testowy` | **musi być `[]`**; wpisy celowo psują regułę (blokada/limit/koszt/cooldown/klikniecia/kontekst) dla testu regresji; SEVERE w logu przy starcie/reloadzie | `Ustawienia.java:48,59`, `Mistrzostwa.java:55-56` |
Kontrola kluczy: wszystkie 12 kluczy najwyższego poziomu configu world1 są czytane w `Ustawienia.java` (brak „nieużywanych”). Nazwy profesji z configu sprawdzane przy starcie: nieistniejąca → SEVERE; profesja spoza list z max-level > prog → WARNING (`Mistrzostwa.java:53-65`).

## Jak działa
- **Blokada XP** — `BlokadaXp.naZyskXp` na `PlayerExperienceGainEvent` (MMOCore), `HIGHEST`, `ignoreCancelled` (`BlokadaXp.java:38-63`). Dla profesji mistrzowskiej bez mistrzostwa: poziom ≥ prog → anuluje + pasek akcji (MythicLib ActionBar `HIGH`, 3 s, nie częściej niż co 20 s); poniżej progu przycina zysk do XP brakującego do progu (pętla awansów MMOCore potrafi przeskoczyć wiele poziomów). Brak gracza → anuluje; brak profilu → traktowane jak brak mistrzostwa. Nie przechodzą przez to tylko jawne settery admina (`mmocore admin level set`, `exp set`) — `config.yml:8-10`.
- **Stan w LuckPerms**: węzły `prof.master.<id>` i tymczasowy `mistrzostwa.cooldown` z kontekstem `profil=<uuid profilu MythicLib>` (`Stan.java:37-38,104-107,132-145`). Odczyt tylko po węzłach, nigdy `hasPermission` (OP fałszuje). Węzły bez kontekstu profilu są ignorowane (`Stan.java:158-166`).
- **Kontekst LuckPerms** rejestruje `KontekstProfilu` (ContextCalculator) i sygnalizuje zmianę po `SessionUpdateEvent` MythicLib i `PlayerJoinEvent` (po 2 tickach) (`KontekstProfilu.java:34-60`).
- **Profil** = `MMOPlayerData.getProfileId()` (`Stan.java:51-59`).
- Integracje: MMOCore (profesje, krzywa XP, exp-table), MythicLib (ActionBar, SessionUpdateEvent), LuckPerms (API 5.4), Vault (`Economy`), PlaceholderAPI (soft), DeluxeMenus (`dm open` przez konsolę, soft).

## Koszt dla serwera
Brak zadań okresowych (`runTaskLater` tylko po zmianie sesji i do otwarcia okien, `KontekstProfilu.java:46`, `Komenda.java:251`). Na żądanie: każdy zysk XP profesji mistrzowskiej → odczyt węzłów LuckPerms gracza (pętla po `getNodes`, `Stan.java:71-84`) i przy poziomie < prog pętla `prog - poziom` wywołań krzywej XP (`BlokadaXp.java:61`). Zapis: `saveUser` LuckPerms przy wyborze/zmianie; `historia.log` (dopisywanie linii). Nie mierzone.

## Dane
- Stan mistrzostw i cooldown: baza LuckPerms (węzły z kontekstem profilu postaci), per postać. UUID konta jest wspólny dla postaci (MMOProfiles), dlatego użyto kontekstu `profil` = UUID profilu MythicLib (`Stan.java:24-31`) — plugin nie zapisuje niczego po samym UUID gracza poza logiem.
- `plugins/Mistrzostwa/historia.log`: linie „kto, kiedy, za ile” (`Mistrzostwa.java:83-91`). Na world1 pliku jeszcze nie ma (w katalogu tylko `config.yml`) — nikt nie wybrał mistrzostwa.

## Znane pułapki i decyzje
- `~/.claude/skills/mc-systemy-gracza/profesje.md:7` — model docelowy 13 profesji, 2 ogólne, 2 mistrzostwa (decyzja 23.09).
- `…/profesje.md:8-9` — wdrożenie 12.10 14:45 i liczby (próg 50, limit 2); `:17` stary model „bez limitu” jest NIEAKTUALNY.
- `~/.claude/skills/mc-systemy-gracza/SKILL.md:46` — skrót profesji.
- Pułapki MMOProfiles/UUID: `~/.claude/skills/mc-mmo-pluginy/SKILL.md` (nie sprawdzałem konkretnej linii o Mistrzostwach — brak wpisu o tym pluginie poza profesje.md).

## Budowanie i wdrożenie
`./build.sh` (kontener `gradle:8.12-jdk21`, `--memory 1536m`, jary world1 tylko do odczytu, wynik `build/libs/mistrzostwa-0.1.0-<yyyymmddHHMM>.jar`; `build.sh:1-11`). Zależności `compileOnly` przypięte po nazwie pliku: MMOCore, MythicLib, MMOProfiles, PlaceholderAPI, Vault + LuckPerms API 5.4 (`build.gradle.kts:12-26`). Testów jednostkowych brak; test regresji: `tools/eval-wezly/sloty-regresja/mistrzostwa.js` (wg komentarza `config.yml:75`, nie sprawdzałem pliku).

## Stan po poprawkach (25.10)
- `usage`, `pomoc()` i tab-complete (tylko dla `mistrzostwa.admin`; gracze online, profesje mistrzowskie, nr 1..limit, poziomy z `tytuly`) mają wszystkie 11 podkomend; nienumeryczne `<nr>` (`porzuc`) i `<poziom>` (`kamien`) dają komunikat dla nadawcy.
- Typ nadawcy: gracz, konsola i RCON przechodzą; inny (blok komend, minecart) dostaje komunikat. Admin-gracz nadal może wołać `nadaj`/`odbierz` (bez limitu i kosztu) i komendy menu na cudzym koncie — zamierzone dla admina, do decyzji właściciela, czy ograniczyć komendy menu (`okno`/`wybierz`/`zmien`/`porzuc`/`potwierdz`) do konsoli.
- Mapy per gracz w `Komenda` (`ostatniaOperacja`, `oczekujace`) czyszczone przy wyjściu (`Komenda.java:58-62`).
- Stan `wstrzymana` (`Placeholdery.stan`, `:46-55`): zwracany dla profesji mistrzowskiej, której gracz NIE ma jako mistrzostwa, gdy jej poziom jest WIĘKSZY niż `prog`. Kolejność: ogólna → `ogolna`; spoza listy mistrzowskich → `brak`; jest mistrzostwem → `mistrz`; poziom > prog → `wstrzymana`; poziom == prog → `do_wyboru` (jest wolne miejsce, `liczba < limit`) albo `do_zmiany` (limit zajęty); poziom < prog → `rozwoj`. Zwykłym graczem `wstrzymana` pojawia się po zamianie (`zmien`: porzucona profesja „zostaje na poziomie, ale dalej nie rośnie”) albo gdy poziom był już powyżej `prog` (np. ustawiony adminem/przed wdrożeniem pluginu). `BlokadaXp` (`:44-52`) blokuje XP profesji bez mistrzostwa od poziomu >= `prog`, więc `wstrzymana` i `do_*` oznaczają to samo dla XP (brak przyrostu); różnią się tylko etykietą w menu. Nie błąd logiki: etykieta nie odróżnia „porzuciła mistrzostwo” od „nigdy nie miała”, ale okno wyboru `okno` jest dla poziomu > `prog` dostępne (`lvl < prog` odrzuca tylko niższe), więc z `wstrzymanej` można nadal wybrać mistrzostwo.
- Config źródło vs world1: identyczne (`diff`), nic do wyrównania.
