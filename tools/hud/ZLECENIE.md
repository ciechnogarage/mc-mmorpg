# Zlecenie: nowy HUD world1 (MythicHUD) — Karta Wędrowca, dolny pasek wg slotów, stamina, minimapa

Właściciel: „zrób w lewym górnym zajebisty hudzik — mini kartę gracza (jak na mcmodels), dolny HUD rozdziel wg
slotów i co gdzie, przesuń mapę, dodaj staminę od razu, postarajcie się”.
Ty (Codex) projektujesz i robisz pliki. Claude sprawdza w prawdziwym kliencie na klonie i wdraża.

## Gdzie pracujesz
- `tools/hud/praca/` — kopia configów MythicHUD (config.yml, hud_assets, layouts, listeners, popups) + `NMinimap/config.yml`. TYLKO tu edytujesz.
- `tools/hud/oryginal/` — nietknięta kopia (nie ruszać; do diffu).
- Nowe tekstury: `tools/hud/praca/pack/assets/mythichud/textures/assets/karta/...` (ścieżka w YAML: `assets/karta/<plik>.png`,
  tak jak istniejące `assets/vanilla/heart.png` w `plugins/MythicHUD/source-pack/assets/mythichud/textures/assets/`).
- Żywy serwer: `/opt/docker/mc/data/world1/plugins/` — TYLKO CZYTASZ. Żadnych restartów, reloadów, komend, dockera.

## Obrazy do obejrzenia (otwórz je)
- `tools/hud/inspiracje/w1.jpg`, `w2.jpg`–`w4.jpg` — Party HUD (Volya, mcmodels): karta = kwadratowy portret 16x16 twarzy
  w ciemnej ramce, nad paskami „10 | Nick”, gruby czerwony pasek HP z liczbą „17 / 17” NA pasku, pod nim cienki niebieski
  pasek many; ikona korony lidera w rogu; złota część paska = absorpcja; czerwona liczba przy niskim HP. TO JEST GŁÓWNY WZÓR karty.
- `tools/hud/inspiracje/w5.png`, `w6.png` — Lumin Citadel HUD: portret w ozdobnej ramce w lewym górnym rogu, panel z nickiem
  „wiszący” z ramki, waluta pod spodem; hotbar w ozdobnej obudowie z paskami HP/głodu na „skrzydłach” po obu stronach i kulą w środku;
  współrzędne w prawym górnym rogu w małej ramce. Wzór na OBUDOWĘ dolnego paska i ramkę minimapy.
- `tools/hud/ref/obecny_hud_minimapa_lewy_gorny.png` — minimapa NMinimap dziś w lewym górnym rogu (offset 60/60, scale 2).
- `tools/hud/ref/obecny_pasek_nad_hotbarem.png` — dziś nad hotbarem: nazwa zaklęcia + pasek MMOCore „♥ 20/20 | ♦ 9/20 | ⛨ 0”.
- Styl naszych rzeczy: Księga Wędrowca (menu Q) — `plugins/Nexo/pack/assets/**` szukaj tekstur ksiega/hub (albo
  `plugin-src`/`tools/menu-hub-gui`), okno E `tools/eq-gui`. Paleta Wynna zmierzona: papier #E5D1A5 #D7C191 #CAB486 #AF996D,
  kontur #50352D, akcenty #8A7351 #6C533F; złote okucia jak w Księdze. HUD jest NA ŚWIECIE, więc tło karty raczej ciemne
  (skóra/ciemne drewno #2b1d17–#3a2a20) z jednym złotym/brązowym obrysem — czytelność na trawie i w nocy.

## Zasady stylu (twarde — właściciel to egzekwuje)
- Jakość z oszczędności: ≤14 barw na element, JEDEN cienki ciemny kontur, płaskie tony blisko siebie, zero szumu/ziarna, bez wielu ramek.
- Rysuj w PODWÓJNEJ gęstości (1 piksel GUI = 2x2 teksele), żadna tekstura > 256 px w żadnym wymiarze (Nexo podmienia na placeholder).
  Sprawdź, jak MythicHUD skaluje teksturę (`scale` warstwy / wysokość glifu) — jeśli nie da się 2x, rysuj 1x i napisz to w raporcie.
- Napisy: czcionka MC (`font: default`, `outlined: true`) — nie wpalaj tekstu TTF w PNG.
- Nie rysuj piór po ekranie: HUD nie może zasłaniać środka ekranu ani celownika.

## Zasady MythicHUD (zmierzone u nas, nie zgaduj)
- `position` 0–100 od LEWEGO DOLNEGO rogu, `offset` w px GUI, y DODATNIE = W GÓRĘ; tekst/tekstura wisi W DÓŁ od offsetu.
- `align: left` = rośnie w LEWO; normalny układ od lewej = `align: right`.
- Pierwsza warstwa w `layers` rysuje się NA WIERZCHU — tło/ramka na końcu listy.
- Warunek `<cond> <wynik> hide` chowa element, gdy warunek NIE daje `<wynik>`. „Pokaż tylko gdy X” = `placeholder{ph=%x%;val=tak} true hide`.
- `gamemode{}` nie jest przeliczany — kreator trzyma gracza w SPECTATOR, KAŻDY nowy asset musi mieć
  `placeholder{ph=%player_gamemode%;val=SPECTATOR} true hide` (patrz vanilla.yml).
- Warstwa głowy istnieje (docs wiki.mythiccraft.io/mythichud/Components/Asset-Layers):
  `head: {size: 4, target: <gracz>}` — SPRAWDŹ w docs/jarze (`plugins/MythicHUD*.jar`, javap/unzip) czy target przyjmuje
  placeholder (`%player_name%`) albo jak pokazać WŁASNĄ głowę. Skórki postaci zmienia CMI (`/cmi skin`) — głowa ma pokazywać aktualną skórkę.
- Listenery: `listeners/default-listeners.yml` (mana = %mmocore_mana% / %mmocore_stat_max_mana%). Wbudowane: health, food, air, armor, exp...
- Zmiana plików = restart (pack składa się przy starcie). Nie ma `mhud reload`. Ty niczego nie uruchamiasz.
- Pasek akcji MMOCore (`MMOCore/config.yml` action-bar) co 5 ticków nadpisuje action bar — patrz niżej.
- Dokumentacja: najpierw wiki.mythiccraft.io/mythichud (Asset-Layers, Assets, Conditions, Placeholders, Fonts, Plugin-Configuration),
  MMOCore wiki (placeholdery staminy/zasobów), NMinimap (Modrinth/Spigot/GitHub). Masz `--search`. Nie wymyślaj kluczy.

## Co zrobić

### 1. Karta Wędrowca — lewy górny róg (główna rzecz, ma być „zajebista”)
Pomysły (wybierz i dopracuj, możesz lepiej):
- Portret: twarz z AKTUALNEJ skórki w kwadratowej ramce (okucia jak w Księdze), w rogu ramki okrągły medalion z POZIOMEM postaci
  (MMOCore `%mmocore_level%`).
- Obok portretu: wiersz „Nick” + mała ikona klasy/rasy albo nazwa klasy (`%mmocore_class%`), przyciemniona.
- Paski pod nickiem, od najgrubszego: **HP** (czerwony, liczba „aktualne / max” NA pasku), **zasób klasy** (mana niebieska;
  sprawdź `MMOCore/classes/**` i plany: Akolita ma Dusze, `docs`/pamięć „zasób na archetyp” — jeśli placeholdera innego zasobu
  nie ma, na razie mana), **STAMINA** (zielono-żółta, NOWA — MMOCore ma staminę: `MMOCore/config.yml` max 1000, `stats.yml`
  MAX_STAMINA/STAMINA_REGENERATION; AnimacjeBroni zużywa ją na mocny cios „Za mało staminy”), na dole cieniutki złoty **EXP** do poziomu.
- Dynamika (warunkami/osobnymi warstwami): absorpcja złota, HP < 25% → czerwona liczba/czerwona ramka portretu, trucizna zielony
  pasek, uschnięcie czarny, ogień pomarańczowy (Volya robi tak w MMORPG Resources HUD). Stamina < 20% → miga/ciemnieje.
- Opcjonalnie mała ikonka patrona Wiary (plugin Wiary ma placeholdery? sprawdź `plugins/Wiary*`), przy walce Chowańców nic nie zasłaniać.
- Pasek Chowańca (`layouts/chowance.yml`, dziś 0/100 +6/-22) i HUD walki Chowańców (`walka-on`/`walka-ja`) — przesuń POD kartę,
  żeby się nie nakładały. Sprawdź wysokość karty i odsuń z zapasem.
- Usuń dublujące się stare elementy: `sword-layout` (HP/Mana tekstem w lewym górnym) i `default-layout` (vanillowe paski nad hotbarem)
  zastąp nowym układem; vanillowe serduszka/głód/zbroja/XP — sprawdź `hide-vanilla-hud`/`updaters` w config.yml albo override tekstur
  w packu; zapisz w raporcie, co i czym schowałeś (kreator i tryb widza muszą dalej działać).

### 2. Minimapa — przenieś z lewego górnego
- NMinimap `map-display.offset` jest wpiekany w shader. Ustal (kod shadera `plugins/NMinimap/resourcepack/**/text.vsh` + scalony shader
  w MythicHUD source-pack + docs NMinimap), czy da się zakotwiczyć w PRAWYM górnym rogu niezależnie od rozdzielczości/skali GUI.
  Jeśli tylko lewy-górny offset — zaproponuj najlepszą pozycję (np. prawy górny przez zmianę shadera, jeśli to proste i bezpieczne;
  shader jest SCALONY z MythicHUD, nie zepsuj tego — opisz dokładnie zmianę). Pamiętaj o BossBarze na środku góry.
- Pod/obok minimapy: ramka w stylu karty + współrzędne/nazwa krainy (jeśli jest placeholder, np. WorldGuard region) — w MythicHUD.

### 3. Dolny pasek — rozdzielony wg slotów („co gdzie”)
- Ustal PRAWDZIWY przydział slotów hotbara z plików: okno E (ksiega-postaci): slot 0 prawa broń aktywna, 1 obraz lewej broni,
  2–4 mikstury/użytkowy; mapa (plugin Mapa — slot 7?), Dziennik `UI_DZIENNIK` slot 8 (`CoreTools/Scripts/stale-sloty.yml`),
  zaklęcia (plugin Zaklecia: nośnik w ręce, „pusta dłoń”). Sprawdź w `plugins/KsiegaPostaci`, `plugins/Mapa`, CoreTools.
- Zaprojektuj obudowę hotbara w grupach (np. [Broń P | Broń L] · [Mikstury 3] · [... ] · [Mapa | Dziennik]) — przerwy/odcienie
  między grupami, małe ikonki/etykiety nad grupami (np. miecz, flakonik, mapa, księga). Do rozważenia: podmiana tekstury
  hotbara w packu (`minecraft:textures/gui/sprites/hud/hotbar.png` 182x22, `hotbar_selection.png`) przez Nexo/MythicHUD pack —
  sprawdź, gdzie u nas taki override powinien trafić i czy MythicHUD/Nexo już coś tam nadpisuje.
- Nad paskiem po bokach: dziś nazwy zaklęć lewej/prawej dłoni (`layouts/zaklecia.yml`, y 84) — zachowaj, wpasuj w obudowę
  (lewa dłoń nad grupą lewej strony, prawa nad prawą).
- Pasek akcji MMOCore („♥ | ♦ | ⛨”) dubluje kartę — zaproponuj, co z nim (np. `action-bar.enabled: false` w MMOCore, bo komunikaty
  idą przez displayActionBar MythicLib — SPRAWDŹ, czy wyłączenie nie zabije komunikatów pluginów; jeśli niepewne, zostaw i opisz).
- Zrób 2 WARIANTY dolnego paska jako makiety (różne pomysłem, nie kolorem), wdroż w YAML ten, który polecasz.

### 4. Makiety do oceny (obowiązkowe)
- `tools/hud/makiety/` — PNG 1920x1080 z całym HUD-em nałożonym na zrzut gry (np. `ref/obecny_hud_minimapa_lewy_gorny.png`
  albo czysty fragment świata), przy skali GUI 3 i 4 (piksele GUI = 3x/4x NEAREST, bez rozmycia), stany: pełne HP, niskie HP + trucizna,
  pusta stamina, walka Chowańców (karta + pasek Chowańca), oba warianty dołu. Plus crop karty x2.
  Skrypt generujący makiety (Python + PIL; jeśli brak PIL w sandboksie — napisz skrypt i opisz, Claude go uruchomi).

### 5. Wdrożenie (przygotuj, NIE uruchamiaj)
- `tools/hud/wdroz.sh`: kopia zapasowa `plugins/MythicHUD/{config.yml,hud_assets,layouts,listeners,popups,source-pack}` + NMinimap
  config + ewentualnie MMOCore config do `/opt/docker/mc/data/world1/backups/hud-<data>/` z `cofnij.sh`, potem kopiowanie z `praca/`.
  Ścieżki bezwzględne (nieudany `cd` = kopiowanie do złego katalogu). Bez restartu — restart robi Claude.
- `tools/hud/README.md`: co zmienione, gdzie każdy element, jakie placeholdery, czego nie udało się sprawdzić.

## Raport
Zapisz do `/home/dev/Desktop/Przemek/MC/tools/hud/RAPORT.md`, max 60 linii: zrobione / pliki / decyzje (minimapa, pasek MMOCore,
vanilla HUD) / niepewne rzeczy do sprawdzenia w kliencie. Bez wklejania diffów i całych YAML-i.
