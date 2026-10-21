# Zlecenie: Otwarta Arena (PvP + pomiar obrażeń), wejście z huba

Właściciel (17.10): „dodanie otwartej areny do huba, ma tam być pvp on i damage do sprawdzania; sprawdź różne kombinacje
i każdą możliwą opcję rozwalenia”. Ty piszesz kod/konfiguracje i scenariusze testów. Claude buduje, stawia klon, testuje
botami i klientem, wdraża. Ty NIE masz dockera ani serwera — nie próbuj uruchamiać, restartować, reloadować.

## Środowisko (fakty zmierzone, nie zgaduj)
- Serwer: Paper 1.21.11, world1. Pluginy (jary w /opt/docker/mc/data/world1/plugins, tylko do odczytu): MythicLib 1.7.1, MMOItems 6.10.1,
  MMOCore 1.13.1, MythicMobs 5.11.2 (+Crucible), WorldGuard 7.0.15, FAWE, Multiverse-Core 5.5.3, CoreTools 1.4.2, DecentHolograms 2.9.9,
  PlaceholderAPI, DeluxeMenus 1.14.1, VoidGen, ModelEngine, własne: KsiegaPostaci (hub/Portal/okno E), Zaklecia (zaklęcia w dłoniach,
  mmodamage t=SKILL,MAGIC), DwieBronie, AnimacjeBroni, Chowance (pety, przywołańce), KlasyBron, Wiary.
- PvP: od 1.21.9 to reguła gry `pvp` per świat (Multiverse: wszystkie światy pvp=true). Stolica (`stolica_odnowa`) ma region WG
  `stolica_odnowa` z `pvp: deny`. MMOCore `pvp_mode.enabled: false` (globalnie, NIE zmieniaj — inne systemy na tym stoją).
  Zmierzone 13.10: w Stolicy zaklęcia (mmodamage) nie ranią nawet mobów — sprawdź w kodzie/dokumentacji MythicLib, czy ochrona
  regionu WG (pvp deny) blokuje każde mmodamage; arena musi mieć PvP ORAZ obrażenia zaklęć/umiejętności.
- Śmierć na serwerze: keep_inventory per świat TYLKO przez `mv gamerule set minecraft:keep_inventory true <świat>` (vanillowa komenda
  kłamie), MMOCore `death-exp-loss` 10% EXP poziomu, CoreTools `EventsManager/kara-smierci.yml` (PlayerRespawnEvent) + `Scripts/kara-smierci.yml`
  (mmostat klucz `kara_smierci`: weapon_damage -15, skill_damage -15, max_health -10% na 3600 ticków, ikona unluck 180 s).
- Hub (menu Q) = DeluxeMenus `plugins/DeluxeMenus/gui_menus/hub_glowne.yml` (kafle; Portal = komenda pluginu `ksiegaportal`),
  podmenu `stolica_podroze.yml` (kafle 2x2: info, Loch 001, „Dalsze lochy”, „Wyspy”), `hub_spolecznosc.yml`. Tła okien to glify Nexo
  z wypalonymi grafikami — NIE zmieniaj tekstur; wstaw arenę jako kafel tam, gdzie pasuje bez nowej grafiki (np. w miejsce
  nieaktywnego kafla albo w podmenu Społeczność) i opisz dokładnie sloty, żeby Claude sprawdził to zrzutem w kliencie.
  Zasada właściciela: gracz nigdy nie wpisuje komend — wszystko z menu; komendy pluginu wołane z DM jako `[console] ... %player_name%`.
- Pluginy są w repo: wzory kodu w /home/dev/Desktop/Przemek/MC/.claude/worktrees/arena (i gałęzie dwie-bronie, zaklecia-kostur).
  Szkielet buildu areny już jest: MCMMORPG/plugin-src/arena (build.gradle.kts z jarami world1 — dopisz w `pinnedPluginJars`
  potrzebne jary, np. worldguard/MythicMobs, dokładne nazwy plików z katalogu plugins).
- Komunikaty po polsku. Pasek akcji przez MythicLib `MMOPlayerData.get(p).getActionBar().show(priorytet, ticki, tekst)` (zwykły
  sendActionBar zjada MMOCore). Priorytety: odmowy 40, info 30.
- Wspólny bot do testów: /opt/docker/mc/narzedzia/bot (README.md, lib: scenariusz/polacz/rcon/sleep, wzory w scenariusze/).
  Pułapki: klon ma płaski świat w powietrzu (zbuduj platformę, fall_damage false), bot nie wysyła player_input, PPM jak klient =
  use_item obu rąk co ~200 ms; `minecraft:teleport/kill` (CMI przejmuje tp/kill); `minecraft:list uuids`.

## Ustalenia projektowe (trzymaj się; jeśli coś jest niewykonalne — napisz dlaczego w raporcie)
1. Osobny świat `arena` (VoidGen, pusty). Budowla z komend (fill/setblock) generowana skryptem — arena okrągła ~r=20 z murem,
   podwyższone trybuny dookoła, platforma odrodzenia, „Strefa treningowa” z 3 manekinami obok ringu. Wszystko w jednym pliku
   komend (`wdrozenie/swiat/budowa.txt`, jedna komenda na linię, do puszczenia przez RCON).
2. Regiony WorldGuard (plik `wdrozenie/swiat/regions.yml` dla świata arena): `__global__` pvp deny, build deny, mob-spawning deny;
   `arena_ring` pvp ALLOW (priorytet wyżej); trybuny, odrodzenie i strefa treningowa pvp deny. Bez rozbijania/stawiania bloków nigdzie.
3. Plugin `Arena`:
   - `arena wejdz <gracz>` (konsola): zapamiętuje miejsce powrotu i teleportuje na odrodzenie areny. Wejście tylko z bezpiecznych
     światów (world, stolica_odnowa, stolica) — z instancji lochów MythicDungeons, creator_limbo i samej areny odmowa z komunikatem.
   - `arena wyjdz <gracz>` (konsola): powrót do zapamiętanego miejsca (fallback: spawn Stolicy). Blokada wyjścia na 10 s po
     zadaniu/otrzymaniu obrażeń PvP (combat tag) — dotyczy też Portalu/szybkiej podróży/lochów wołanych z huba (komendy konsoli
     z hubu są jedyną drogą teleportu — sprawdź, które to są i jak je zablokować w trakcie walki, np. PlayerTeleportEvent z przyczyną
     PLUGIN/COMMAND poza areną, wyjątek: śmierć/odrodzenie i sam `arena wyjdz` po czasie).
   - Śmierć na arenie: bez utraty przedmiotów (keep_inventory świata + nic nie wypada), bez utraty EXP MMOCore, bez debuffu
     `kara_smierci` (wybierz najczystszy sposób: warunek świata w skrypcie CoreTools albo zdjęcie modyfikatora — uzasadnij),
     odrodzenie na platformie areny z 3 s nietykalności. Komunikat „X pokonał Y” tylko graczom na arenie.
   - Zero zysku z areny: zabójstwo gracza nie daje EXP klasy/profesji, łupu, postępu zadań/osiągnięć, EXP Chowańców; manekin nie daje
     NIC (EXP, drop, profesje, Chowańce), nie umiera, nie da się go przesunąć, odepchnąć, podpalić na stałe, wyprowadzić, zabić komendą
     przez gracza.
   - Manekin: MythicMobs mob (plik `wdrozenie/pliki/MythicMobs/mobs/Arena.yml`), bez AI, ogromne HP lub leczenie do pełna, odporny
     na odrzut. Pomiar obrażeń (plugin): ostatni cios (wartość, rodzaj: fizyczne/magiczne/umiejętność/żywioł jeśli da się wyczytać
     z MythicLib DamageMetadata, krytyk), suma i DPS z ostatnich 5 s, liczba trafień — osobno na gracza (dwóch graczy bijących jednego
     manekina nie miesza wyników). Wyświetlanie: pasek akcji gracza + krótkie unoszące się liczby (TextDisplay, sprzątane). Obrażenia
     rozłożone w czasie (podpalenie, krwawienie) przypisz graczowi, który je wywołał (ostatni napastnik w oknie 5 s).
   - Wyjście poza ring w powietrze/pustkę: teleport na odrodzenie bez śmierci.
   - Po wylogowaniu na arenie gracz wraca na odrodzenie areny (nie w środek walki); combat tag nie przechodzi przez relog, ale
     wylogowanie w walce nie może dać korzyści (np. ucieczki przed zabiciem z zachowaniem przewagi) — zaproponuj prostą regułę.
4. Hub: kafel „Otwarta Arena” (opis: PvP włączone w ringu, śmierć bez kary, manekiny do pomiaru obrażeń) → `[console] arena wejdz`;
   na arenie ten sam kafel „Wyjdź z areny” → `[console] arena wyjdz` (view_requirement po świecie). Zmieniaj KOPIE plików z world1,
   zapisane w `wdrozenie/pliki/DeluxeMenus/gui_menus/...` (pełne pliki, nie fragmenty).

## Lista „rozwalenia” — zaprojektuj przeciw każdej i do każdej daj test (scenariusz bota) albo napisz, czemu tylko ręcznie
A. Ucieczka z walki teleportem (hub, Portal, szybka podróż, lochy, `arena wyjdz`) w trakcie combat taga.
B. Farma: zabijanie alta/kolegi dla EXP, łupu, postępu, Chowańców; farma na manekinie (EXP klasy, profesji, broni, Chowańców).
C. Kara za śmierć na arenie (EXP, debuff, przedmioty) i jej „przeniesienie” po wyjściu; obejście kary z otwartego świata przez arenę
   (śmierć poza areną → szybkie wejście na arenę).
D. PvP poza ringiem: trybuny, odrodzenie, strefa treningowa, granica ringu (stoisz za granicą, bijesz do środka i odwrotnie),
   pociski i zaklęcia obszarowe przelatujące przez granicę, Stolica i reszta świata nadal bez zmian.
E. Wejście z miejsc zakazanych (instancja lochu, kreator, w powietrzu/w trakcie spadania, martwy, w walce poza areną) i zapamiętany
   powrót, który można nadużyć (teleport w miejsce, do którego nie powinno się wrócić, np. środek lochu; powrót po zmianie postaci
   MMOProfiles na inną postać).
F. Manekin: zabicie, przesunięcie (odrzut, szarża, wypad z DwieBronie/AnimacjeBroni, zaklęcia), wyprowadzenie smyczą, podpalenie
   lawą, atak Chowańców/przywołańców, mieszanie wyników dwóch graczy, spam liczbami (wydajność), wycieki TextDisplay po restarcie.
G. Chowańce i przywołańce na arenie (znany crash przy PvP + przywołaniec był naprawiany 13.10 — sprawdź, że nie wraca), pety
   bijące graczy poza ringiem.
H. Zaklęcia/umiejętności MMOCore/MythicMobs na graczy w ringu (czy w ogóle ranią — patrz problem ze Stolicą) i NIE poza ringiem.
I. Budowanie/niszczenie, wyrzucanie i podnoszenie przedmiotów (handel przez ziemię?), mikstury rzucane, perły, łódki, wspinanie
   na mur, latanie (elytra), wyjście poza mapę.
J. Relog w walce, crash klienta, zmiana postaci (MMOProfiles) na arenie, wejście drugą postacią z innym powrotem.
K. Wiele osób naraz (pomiar per gracz, brak wzajemnego psucia stanu), restart serwera w trakcie (stan pluginu, zapamiętane powroty).

## Co oddajesz
- Kod pluginu `Arena` + plugin.yml + config.yml (wszystkie liczby w configu: czasy, promienie, współrzędne).
- `wdrozenie/`: `swiat/budowa.txt`, `swiat/regions.yml`, `swiat/ustaw.txt` (komendy mv/gamerule dla świata arena),
  `pliki/...` (pełne pliki do skopiowania: DM, MythicMobs, ewentualnie CoreTools), `wdroz-szkic.sh` (Claude dopracuje).
- `testy/` — scenariusze dla /opt/docker/mc/narzedzia/bot (format jak scenariusze/przyklad.js), po jednym na grupę A–K tam, gdzie
  bot to sprawdzi (PvP = dwóch botów), z czytelnymi PASS/FAIL.
- Raport: zapisz DOKŁADNIE do pliku /home/dev/Desktop/Przemek/MC/.claude/worktrees/arena/MCMMORPG/plugin-src/arena/RAPORT-1.md, max 40
  linii: co zrobione, decyzje (z uzasadnieniem w 1 zdaniu), czego nie da się zrobić/sprawdzić botem, ryzyka. Bez wklejania kodu.
