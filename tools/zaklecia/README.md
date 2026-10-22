# Zaklęcia w dłoni (Skyrim): Płomienie i Odmrożenie — TEST (2026-09-29)

Zaklęcia koncentracji: zaklęcie trzymasz w ręce jak przedmiot, **przytrzymujesz PPM** — z dłoni leci strumień,
co 0,5 s obrażenia w stożku 6 bloków (40°) i pobór many MMOCore; puszczasz — gaśnie. Brak many = gaśnie
z dźwiękiem i napisem „Za mało many”.

| | Płomienie (`ZAKLECIE_PLOMIENIE`) | Odmrożenie (`ZAKLECIE_ODMROZENIE`) |
|---|---|---|
| obrażenia | 8/s, żywioł FIRE (`mmodamage`) | 7/s, żywioł ICE |
| mana | 4/s | 5/s |
| efekt | podpalenie 2 s | spowolnienie II 1,5 s, zamrożenie (drżenie moba, szron na ekranie gracza), −6 wytrzymałości/s graczom |
| PvP | tylko tam, gdzie WorldGuard `pvp allow` (`mmoCanTarget`) | jw. |

Dla porównania: kamienny miecz startowy = 5 × 1,6/s = 8/s.

## Skąd co
- `src/MythicMobs/items/Zaklecia.yml` — przedmioty Crucible: komponent `Consumable` (3600 s, animacja BOW = poza
  „ręce przed siebie”, dźwięk `intentionally_empty`), `UseEffects` (spowolnienie 0,5, bez sprintu),
  `~onUse`/`~onCancelUse`/`~onUnHeld`/`~onTimer:4`.
- `src/MythicMobs/skills/Zaklecia.yml` — aury `_wiz` (co 2 ticki: niewidzialny pocisk z dłoni sypiący cząsteczkami,
  rozbija się na istotach i blokach) i `_tik` (co 10 ticków = okno nieśmiertelności moba).
- `src/Nexo/pack/assets/zaklecia/` — definicja przedmiotu: w slocie płaska animowana ikona, w dłoni dwa
  skrzyżowane świecące płaty; przy rzucaniu (`using_item`) płomień rośnie. `grafika.py` odtwarza tekstury z client.jar.
- Plugin **Zaklecia** (repo MC, `MCMMORPG/plugin-src/zaklecia`, gałąź `zaklecia-skyrim`): mechanika
  `zakZasob{zasob=mana|stamina;a=..}` i warunek `zakMaZasob{..}` — cichy pobór zasobów przez API MMOCore.

## Wdrożenie / cofnięcie
    tools/zaklecia/wdroz.sh                 # world1: kopia -> pliki -> mm reload -> nexo reload pack -> kontrola logu
    backups/zaklecia-<data>/cofnij.sh       # przywraca stan sprzed wdrożenia

Bez załadowanego pluginu `wdroz.sh` wgrywa **wariant awaryjny** (`wariant_awaryjny.py`: komenda
`mmocore admin resource-mana take` + placeholder `<mana>`) — działa tak samo, ale MMOCore wpisuje do logu
„X now has Y mana points” co 0,5 s rzucania. Po restarcie z jarem `zaklecia-*.jar` w `plugins/`
uruchom `wdroz.sh` jeszcze raz (`--bez-packa`) — przełączy na wariant cichy.

Dla gracza (test): `mm items give <gracz> ZAKLECIE_PLOMIENIE` / `ZAKLECIE_ODMROZENIE`.

## Pułapki (zmierzone na klonie)
- `effect:particles` z `directional=true` rodzi cząsteczkę w punkcie początkowym rzucającego (stopy);
  `y/sfo/sso` zmieniają tylko wektor. Strumień z dłoni = pocisk MM (`syo/sfo/sso`) i cząsteczki na `@origin`.
- Tiki aury idą asynchronicznie; `mmoCanTarget` przy celu-graczu odpala zdarzenie Bukkit → „may only be triggered
  synchronously”. `sync=true` trzeba dać na wywołaniu skilla **nad** tym z `TargetConditions`.
- Crucible `~onTimer` działa dla przedmiotu w całym ekwipunku — efekt dłoni ma `?holding{m=ZAKLECIE_..}`.
- `dust` ma losowe przyciemnienie koloru (do 60%) — jasny błękit wychodzi szary; mróz używa śnieżynek.
- Dwa boty z jednego IP w < 4 s: `connection-throttle` Bukkita — drugi nie wchodzi.

## Testy
`klon/start.sh` stawia izolowany Paper (sieć `zaklecia-net`, bez portów); `eval/sonda.js <ZAKLECIE> <ms>` —
bot rzuca w kukłę (`klon/kukla.yml`), mierzy HP/manę/cząsteczki/zasięg; `eval/brzegi.js` (env `PVP=allow|deny`) —
efekt dłoni tylko w ręce, zmiana slotu przerywa manę, strefa bez PvP chroni, mróz na graczu. Node: `npm i
mineflayer@4.39.0 minecraft-protocol@1.68.0 rcon-client`, uruchamiać w `node:20` na sieci klonu.
`klon/scena.sh` + `klon/launch.sh` — klient QA i zrzuty.

## v2: oburącz + Księga zaklęć (2026-09-29, NIEWDROŻONE — czeka na akceptację)
Katalog `v2/`. Zaklęcia są w **Księdze zaklęć** (hub Q → Umiejętności → slot 22): LPM na zaklęciu = prawa dłoń
(slot paska), PPM = lewa dłoń (druga ręka), „Puste dłonie” LPM/PPM opróżnia. Nośnika w dłoni nie da się wyrzucić ani przenieść.
Przytrzymany PPM rzuca wszystkimi dłońmi z zaklęciem:

| prawa | lewa | efekt |
|---|---|---|
| zaklęcie | — | jak v1 |
| A | A | podwójne: obrażenia ×2,2 (`_Traf2`, stożek 7 bl.), mana ×2,5 |
| A | B | dwa strumienie naraz, mana = suma |
| broń | zaklęcie | PPM rzuca z lewej, LPM bije bronią |

Rzucanie prowadzi plugin (`zaklecia-0.1.0-202609292211.jar`): Crucible `~onUse` nie działa dla drugiej ręki.
Wdrożenie: `v2/wdroz.sh` (tylko przy 0 graczy, sam restartuje; kopia + `cofnij-v2.sh` w `backups/zaklecia-v2-*`).
Evale (klon): `eval/dwurecz.js` 15/15, `eval/stozek.js` (±20°), `eval/brzegi.js PVP=deny|allow`.

**Pułapka znaleziona przy v2 (poprawiona też w v1 na world1 29.09):** w MythicMobs `@target` to cel, na który
PATRZY rzucający, a nie cel odziedziczony — `mmodamage … @target` w skillu trafienia biło tylko istotę pod celownikiem.
W skillach trafień linie są bez targetera. Placeholder `<target.*>` w linii z `@caster` też wskazuje rzucającego.

### v2: grafika i animacja dłoni (2026-09-30, NIEWDROŻONE)
`v2/src/Nexo/pack/assets/zaklecia/` generuje w całości `grafika.py` (tekstury 16x16/klatkę, 197 plików; `--v1` odtwarza
starą wersję). Animacja w definicji przedmiotu: `using_item` → `use_duration` (wzrost ×1,00→×1,62 w 8 tickach) → od
8. ticku `use_cycle` period 8 (puls + obrót odłamków lodu). Druga dłoń przy rzucaniu oburącz: `view_entity` +
`keybind_down key.use` (tylko u własnego gracza). Pułapki (zmierzone): MC przyciemnia przedmiot w dłoni po kierunku
ściany z JSON-a (pionowa płaszczyzna = brązowy ogień) — dlatego ściana „up” obrócona 45° w elemencie + 45° w display;
lewa ręka odbija rotację y/z, więc lewe konteksty mają -y/-z; kąt elementu 90° odrzuca parser Nexo.
Przegląd: `output/zaklecia-przeglad/img/anim-*.jpg`.

## v2.1: animacja dłoni, gesty, ulubione (2026-09-30)
- Dłonie: nowe modele i animacje (sekcja agenta wyżej): ładowanie `use_duration` 0,35 s, puls `use_cycle`, lewa dłoń odbita.
- Gesty (MM `Zak_Start`/`Zak_Stop`): rozbłysk dłoni na starcie, dym (ogień) / opadający szron (mróz) przy puszczeniu.
- Ulubione jak w Skyrimie: Księga — **Shift+LPM** na zaklęciu = ★; w grze **Shift + kółko myszy** z zaklęciem w dłoni
  przełącza je po ulubionych (bez ulubionych — po wszystkich; prawa dłoń, przy broni w prawej — lewa). Zapis w PDC gracza
  (wspólny dla postaci konta). PAPI: `%zaklecia_ulubione_<id>%`, `%zaklecia_prawa|lewa%`. Eval `eval/ulubione.js` 9/9.
- Koszt many TESTOWY: `v2/src/Zaklecia/config.yml` mana-za-tik 0.5 (docelowo 2.0 / 2.5; opisy przedmiotów i Księgi też).
- Wdrożenie przy graczach online: `v2/czekaj-i-wdroz.sh` (czeka na pusty serwer 2 min, max 12 h; log obok).
- MMOCore przy przelogowaniu w <1 s trzyma nieaktualne dane ("Player is offline") — plugin to znosi; evale botów rób z odstępem.
Strumienie (item_display z pluginu, `strumien_plomienie|odmrozenie`): też z `grafika.py` — tekstura 16x32/klatkę,
16 klatek przewijanych 2 px/tick (przepływ od dłoni), model = 2 płaszczyzny „up” w X wzdłuż osi z (0..16), stożek wąski
przy dłoni; koniec ognia rwie się dziurami, mróz to półprzezroczysta rwana mgła z drobinami lodu.

## v2.2: strumienie jako obiekt wyświetlany (2026-09-30)
Strumień = jeden `item_display` na dłoń (`Strumien.java`, model `zaklecia:strumien_<id>` od agenta, oś +Z, długość z
raytrace do przeszkody), zamiast pocisków MM sypiących cząsteczkami. Pomiar `eval/obciazenie.js` (pakiety/s do gracza obok):
cząsteczki 167/393/330 → 14/23/29, wszystkie 269/483/453 → 105/117/164 (prawa/podwójne/mieszane).
Uderzenie w punkcie trafienia: `Zak_Uderzenie_<id>` co 4 ticki, bez dymu i lawy (lava zostawia czarny dym).
Gesty start/stop mają `audience=@PlayersInRadius{r=32}` — widzą je inni, rzucający nie (tuż przy kamerze były ogromne);
czasteczki przy dłoni wyłączone (`dlon: ''`), bo modele dłoni animują się same. `eval/gest.js` to sprawdza.

## v2.3: szybki wybór (2026-09-30, WDROŻONE 23:46)
- Shift + kółko: koło ulubionych + „pusta” (odłożenie bez Księgi; slot zapamiętany — kółkiem wraca się do zaklęć).
- Shift + Ctrl + kółko: lewa dłoń, niezależnie od prawej (lewa zajęta czymś innym → komunikat, nic nie znika).
- Pasek akcji pokazuje całe koło. Reaguje tylko kółko (krok ±1); klawisze 1-9 przy Shift zmieniają slot normalnie.
- Shift + Q: okno `ulubione_zaklec` (LPM prawa, PPM lewa, puste dłonie, link do Księgi). MenuKey woła teraz
  `zaklecia q %player%` (bez Shift → `dm open hub_glowne`, jak dotąd); komendy w `plugins/Zaklecia/config.yml` → `klawisz-q`.
- Evale: `eval/ulubione2.js` 9/9, `eval/klawiszq.js` 2/2 (prawdziwy pakiet Q). mineflayer NIE wysyła sprintu w
  `player_input` — eval składa pakiet ręcznie jak klient.

## v2.4: LPM/PPM jak w Skyrimie, pasek szybkiego wyboru (2026-09-30)
- **PPM = prawa dłoń, LPM = lewa dłoń, Ctrl + przycisk = obie naraz** (połączone; to samo zaklęcie = podwójne).
  LPM: serwer nie widzi trzymania LPM w powietrze, ale widzi KOPANIE. W trybie LPM (zaklęcie w lewej, prawa pusta albo
  z zaklęciem) plugin daje przejściowo: zasięg bloków 64, istot 0, prędkość kopania 0 → przytrzymany LPM "kopie" teren
  przed graczem (bez pęknięć i bez zniszczenia; creative też blokowane), start/abort = trzymanie LPM (3 ticki tolerancji),
  klik w niebo = impuls 0,5 s. Znane: klient sam gra cichy dźwięk uderzania w blok, widać obrys bloku pod kursorem.
  Dosłowne "oba przyciski naraz" jest niemożliwe: klient przy trzymanym PPM ignoruje LPM, a przy kopaniu nie zaczyna
  używania — stąd Ctrl + przycisk (stan Ctrl serwer widzi w `player_input`).
- **Shift + Q = pasek szybkiego wyboru** (BossBar, bez okna): kółko/1-9 wybór, LPM → lewa, PPM → prawa, Q/Shift+Q zamyka,
  15 s bez ruchu zamyka sam. **Q nad slotem okna DeluxeMenus zamyka okno** (Q włącza i wyłącza menu).
- **Kółko: Shift = lewa dłoń, Ctrl = prawa** (pusta lewa przejmowana tylko u graczy z zaklęciami/ulubionymi).
- **Bez machania**: `SwingAnimation: NONE` na zaklęciach.
- Pułapki: machnięcie ręką po PPM Paper zgłasza jako LPM (powietrze albo — przy zasięgu 64 — blok) → 6 ticków wygaszenia;
  Q z przedmiotem: MenuKey przechwytuje pakiet, klient przewiduje wyrzucenie → `updateInventory` przy każdym `zaklecia q`.
- Evale: `eval/ulubione3.js` 10/10; LPM, Ctrl+PPM/LPM, pasek i Q sprawdzone prawdziwym klientem (zrzuty w przeglądzie).
  Menu `ulubione_zaklec` (okno z v2.3) nieużywane — zostało zarejestrowane, nic go nie otwiera.

## v2.5: dwuklik = obie dłonie (2026-09-30)
Zmierzone prawdziwym klientem z logiem wejścia (`debug-wejscie: true` w `plugins/Zaklecia/config.yml`):
- LPM + PPM wciśnięte naraz: klient najpierw obsługuje LPM; gdy LPM trafia w blok (tryb LPM, zasięg 64), zaczyna kopać
  i SAM połyka PPM (`startUseItem` blokowany przez `isDestroying`) — PPM nie dociera do serwera. Na v2.3 działało, bo LPM
  trafiał w powietrze. Wykrywamy "naraz" (≤3 ticki), gdy oba dotrą, ale w trybie LPM nie dotrą.
- Dlatego: **dwuklik** LPM albo PPM (drugie wciśnięcie ≤12 ticków po pierwszym, przytrzymane) = obie dłonie; Ctrl + przycisk
  też. Przeskok kursora przy trzymanym LPM (abort+start w tym samym ticku) nie liczy się jako wciśnięcie.
- Wynik (klient): dwuklik PPM → obie, dwuklik LPM → obie, sam PPM → prawa, sam LPM → lewa, LPM + ruch myszy → lewa,
  dwa LPM z przerwą 1,2 s → lewa.

## v2.6 (2026-10-01) — LPM i PPM niezależnie
- Nośnik zaklęcia = DEBUG_STICK bez Consumable (komponenty dopisuje `Nosnik.komponenty`: tool 1e6, swing NONE, attack_range 64, bez debug_stick_state).
- Klient przy DEBUG_STICK nie usuwa bloku i nie wchodzi w `isDestroying` → trzymany LPM = START_DESTROY co tick, PPM powtarza się co 4 ticki — oba widoczne naraz.
- Pusta prawa przy zaklęciu w lewej = niewidoczna „pusta dłoń” (ZAKLECIE_PUSTA). PPM z pustą prawą nic nie rzuca.
- Config: `ppm-tolerancja` 6, `lpm-tolerancja` 3, `lpm-impuls` 10 (niebo), `lpm-zasieg-blokow` 24, `spowolnienie` -0.5.
- Ograniczenia: LPM w niebo = impuls 0,5 s; klient rysuje okruchy bloku pod celownikiem; brak pozy łuku w trzeciej osobie.

## v2.7 (2026-10-01) — kotwica: LPM trzymany także w niebo
- Gracz w trybie zaklęć dostaje (tylko on, pakietem) niewidoczny blok STRUCTURE_VOID na linii wzroku, 3–16 bloków dalej, w miejscu, gdzie promień przechodzi przez jego środkowy sześcian 6/16. Twardość 0 → klient "kopie" go co tick bez isDestroying (PPM wolny), brak kolizji, brak okruchów, widać tylko mały obrys.
- Kotwica śledzi wzrok co tick (z wyprzedzeniem obrotu); po przestawieniu stara żyje jeszcze 4 ticki.
- Paper po PPM w blok odsyła realny stan (AIR) → packetevents odsiewa ten pakiet dla pozycji kotwicy (ProtocolLib 5.4.1-SNAPSHOT na 1.21.11 nie dostarczał pakietów — nie używać).
- Machnięcie ręką po klawiszu Q nie jest już brane za LPM (przypisanie w pasku / impuls).
- Config: `kotwica.{wlaczona,min 3,max 16,wyprzedzenie 1.0}`, `lpm-zasieg-blokow` 17, `lpm-tolerancja` 5, `ppm-tolerancja` 7. Debug: `debug-pakiety`.
- Test: job tmp `codex/suite.sh` (prawdziwy klient na klonie): niebo 16/16 i 8/8, obroty 16/16.

## v2.8 (2026-10-01 wieczór) — prosto, bez kotwicy i bez kopania
Właściciel odrzucił kotwicę (obrys, przerwy na mobach). Zasięg celowania w bloki w trybie zaklęć = 0 (zawsze MISS: brak obrysu, okruchów, kopania).
- PPM trzymany = prawa (pakiet użycia co 4 ticki). LPM+PPM naraz albo klik LPM przy trzymanym PPM = obie, aż do puszczenia PPM.
- Klik LPM w trakcie obu = zostaje prawa. Sam LPM = przełącznik lewej (klik włącza / klik wyłącza); PPM przy włączonej lewej dokłada prawą.
- Puszczenia LPM serwer nie widzi (gra nie wysyła nic przy MISS) — dlatego przełącznik, a „obie” trzyma PPM.
- LPM działa od razu; jeśli w 4 tickach przyjdzie Q (machnięcie po Q), skutek kliknięcia jest cofany.
- Test: job tmp `codex/suite28.sh` — 20/20 dwukrotnie (niebo, mob, teren, pusta prawa, Q).

## v2.9–v3.0 (2026-10-01 noc) — HUD, osobne pliki, typy zaklęć jak w Skyrimie
- Badanie: `BADANIE_SKYRIM.md`. Przepis na nowe zaklęcie: `v2/src/NOWE_ZAKLECIE.md`.
- MythicMobs: `items/zaklecia/<id>.yml`, `skills/zaklecia/<id>.yml` (+ `_wspolne.yml`); plugin woła `Zak_<Id>_<Faza>_P/_L/_Obie`, brakujące pomija.
- HUD: `MythicHUD/layouts/zaklecia.yml` + `hud_assets/hud/zaklecia.yml` (nazwy w dłoniach, szara=gotowa, biała=rzuca, ● = lewa włączona; ładuje/gotowe). wdroz.sh dopisuje `zaklecia-layout` do MythicHUD/config.yml.
- Typy (`typ:` w configu): koncentracja (strumień albo `cel: siebie`), ladowany (PPM: ładuj → puść = strzał; lewa: klik = ładuje i strzela sama), runa (jak ładowany, stawia runę; wybucha tylko na wrogów `Enemy`), tarcza (trzymasz PPM; pula pochłania obrażenia).
- Nowe: Ognisty pocisk, Kula ognia, Leczenie, Runa ognia, Osłona (modele/ikony TYMCZASOWE — do zrobienia).
- Pułapka: obrażenia zaklęć (mmodamage) mają gracza jako sprawcę — NIE używać EntityDamageByEntityEvent jako „klik LPM”.
- Testy: job tmp `codex/suite28.sh` (20/20) i `codex/typy.sh` (10 pomiarów skutku).
- Klon: potrzebuje NMinimap-zasoby.zip w Nexo external_packs i `generate_gif_shaders: false` (jak world1), inaczej klient odrzuca paczkę.

## v3.1 (2026-10-02) — przywołania, leczenie innych, iluzja
- Typy: `bron` (przywołana broń w dłoni na czas; zmiana slotu ją odwołuje), `przywolanie` (mob MM z właścicielem: `ActiveMob.setOwnerUUID`, AI ownerattacker/ownertarget, limit, nietrwały), `koncentracja` + `cel: inni` (leczy sojusznika na linii wzroku: gracze wg reguł MythicLib, własne przywołańce/zwierzęta bez canTarget SUPPORT — MythicLib go dla nie-graczy odmawia), `na_cel` (Uspokojenie: wróg ≤ poziomu przestaje atakować).
- Zaklęcia: Przywołany miecz, Duch wilka, Atronach ognia (strzela skillem — cel AI rangedattack nie działa dla blaze), Leczenie dłoni, Uspokojenie. Przywołany łuk pominięty (sprzątanie strzał).
- Pułapka (znowu): `@target` w skillach trafień = cel pod celownikiem wg MM — mechaniki bez targetera, cele podaje plugin.
- Znane: własne pociski zatrzymują się na własnym przywołańcu stojącym na linii strzału (MM projectile kończy się na pierwszej istocie).
- Test: job tmp `codex/przyw.sh` (11 pomiarów skutku).

## v3.2 (2026-10-02, WDROŻONE 11:34) — zgodność ze Skyrimem + leczenia
- Jeden przywołaniec naraz (`przywolania.limit`), miecz w dłoni rzucającej (z mieczem w prawej lewą rzuca się PPM → dwa miecze), Leczenie dłoni jak Healing Hands (nie-wrogowie; bez nieumarłych `Tag.ENTITY_TYPES_UNDEAD`, atronachów, golemów; mana także bez celu), Uspokojenie oburącz podnosi pułap (9 → 19), Osłona: magia 60%, ciosy 30%.
- Nowe: Uzdrowienie innego (8 HP celowi), Zasklepienie ran (12 HP sobie), Wielkie uzdrowienie (16 HP sobie i sojusznikom w 5 bl.).
- Pułapka: `LivingEntity.getCategory()` rzuca w Paper 1.21.11 („Use Tags instead”).
- Testy: job tmp `codex/lecz.sh` (10 pomiarów) + suite28/typy/przyw.

## v4.0 (2026-10-02) — sterowanie jak w vanilli
- PPM (trzymaj) = LEWA dłoń; klawisz sprintu (trzymaj; domyślnie Ctrl, najlepiej boczny przycisk myszy, tryb „przytrzymaj”) = PRAWA; klik LPM = prawa (przełącznik — zapas). Obie niezależnie, puszczenie gasi tylko swoją dłoń.
- W trybie zaklęć sprint nie rozpędza postaci (zmierzone: W+Ctrl 4,8 bl./2 s vs zwykły sprint 12).
- Kółko: Shift = lewa, Shift+Ctrl = prawa. Broń w prawej: LPM bije, PPM = lewa.
- Testy: job tmp `codex/suite40.sh` (22/22); typy/przyw/lecz z `dl` = (PPM-zaklęcie → lewa, LPM-zaklęcie → prawa).

## 2026-10-02 21:16 — powrót do sterowania v3.2 (decyzja właściciela)
v4.0 (PPM = lewa, sprint = prawa) cofnięte: `git revert 844b988` (commit acc0e9e), v2/src przywrócone z kopii sprzed v4.0, blokada „jar musi mieć prawa-sprint” usunięta z wdroz.sh. Sterowanie: PPM = prawa (trzymaj), LPM = lewa (klik przełącza), LPM+PPM = obie, pusta prawa + PPM = lewa. 15 zaklęć bez zmian. Trzymanie LPM bez moda klienta niemożliwe (tylko kopanie bloku/kotwica — odrzucone).

## v4.1 (2026-10-02 22:41, WDROŻONE) — decyzja właściciela, ostateczna
- PPM = LEWA dłoń (trzymaj), LPM = PRAWA dłoń (klik włącza/wyłącza; ładowane: klik ładuje i strzela), PPM + klik LPM = obie. Bez sprintu jako dłoni, bez wyjątków.
- Zmiana zaklęć TYLKO w pasku Shift+Q (kółko wybór, LPM → prawa, PPM → lewa, opis: szkoła/typ/koszt, zostaje otwarty, Q zamyka). Ctrl/Shift+kółko usunięte — kółko i 1–9 działają jak w vanilli.
- Pociski graczy przelatują przez przywołańców (`hitConditions=[ - faction{faction=zaklecia_przywolane} false ]`), kula atronacha trafia tylko jego cel (`hto=true`).
- Testy: `codex/suite50.sh` (15/15 + kółko + pasek + sprint), typy/przyw/lecz z dl = (PPM-zaklęcie → lewa).

## v4.2 + ikony (2026-10-02 23:38, WDROŻONE)
- Zwykły przedmiot w lewej (tarcza, jedzenie) działa w trybie zaklęć — PPM z tarczą blokuje (zmierzone 20/20 HP).
- Przywołańcy dostają cel od pluginu (trafienie właściciela / atak na niego), atronach strzela z pluginu (`zaklecia.atronach_ognia.strzal`) — 4/5 prób; wilk 5/5.
- 13 ikon 16x16 (Codex, 3 rundy z oceną): `v2/src/Nexo/.../textures/item/ikona_<id>.png`, `models/item/<id>_ikona.json`, `items/<id>.json`; skrypt `v2/grafika_ikony.py`, arkusz `v2/podglad_ikon.png`. Księga i okno ulubionych pokazują model swojego zaklęcia. Modele DŁONI nowych zaklęć nadal wspólne (ogień/mróz).
- Uwaga: inna sesja (e44753ba) robi ikony 32x32 do kafelków MENU (Księga Wędrowca) — czeka na wybór właściciela; nie kolidują z 16x16 przedmiotów.

## Ikony zaklęć styl D 16x16 (2026-10-02 ok. 00:32, sesja e44753ba, WDROŻONE przez nexo reload pack)
- 15 tekstur `ikona_<id>.png` (symbol na plakietce szkoły ze złotą obwódką, kierunek D wybrany przez właściciela) ZASTĄPIŁY
  13 ikon z `v2/grafika_ikony.py`; doszły ikona_plomienie i ikona_odmrozenie (modele `*_ikona.json` wskazywały dotąd tekstury dłoni).
- Źródło: `v2/ikony_d16/rysuj.py` (Codex, 3 rundy), arkusz `v2/ikony_d16/arkusz.png`, zrzut z gry `v2/ikony_d16/w_grze.png`.
- UWAGA: NIE uruchamiać `v2/grafika_ikony.py` — nadpisze nowe ikony starymi. Cofnięcie: `backups/ikony-zaklec-20261002-2232/cofnij.sh`.
- 03.10 01:23 WERSJA 2 (WDROŻONA): ikony bez plakietki — w 16x16 plakietka + czarny obrys dawały ~37% prawie czarnych pikseli
  (vanilla ~10%), ikona ginęła na ciemnym slocie. Teraz symbol 13-15 px, obrys w odcieniu własnego koloru, zero czerni.
  rysuj.py w v2/ikony_d16 nadpisany wersją 2; cofnięcie do plakietek: backups/ikony-zaklec-20261002-2323-przed-G/cofnij.sh.
- Pułapka testu: `mm items give ZAKLECIE_*` daje DEBUG_STICK z wbudowanym połyskiem (fioletowa poświata zjada kolory). W grze
  plugin (Nosnik.komponenty) wyłącza połysk; do testów ikon dawać `debug_stick[item_model=..,enchantment_glint_override=false]`.
  `Glint: false` w MythicMobs NIE wyłącza połysku patyka.
- 03.10 12:11 WERSJA "GRAMATYKA KSZTAŁTÓW" (WDROŻONA): kształt = mechanika, kolor = szkoła. Leczenie dłoni = układ Płomieni,
  Uzdrowienie innego = układ Ognistego pocisku, Wielkie uzdrowienie = układ Kuli ognia, Leczenie = serce, Zasklepienie = serce
  z opatrunkiem, Runa = krąg runiczny, Uspokojenie = "Zz", Duch wilka = wyjąca głowa. Źródło PNG: v2/ikony_d16/gramatyka/
  (PNG są źródłem prawdy; rysuj.py nie zawiera ostatniej poprawki "Zz"). Cofnięcie: backups/ikony-zaklec-20261003-1211-przed-gramatyka/cofnij.sh.
