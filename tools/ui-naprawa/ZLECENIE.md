# Zlecenie: pełna naprawa UI/UX world1 wg KRYTYKA.md + weryfikacja w grze

Jesteś wykonawcą. Masz naprawić WSZYSTKIE 29 zarzutów z `/home/dev/Desktop/Przemek/MC/tools/ui-naprawa/KRYTYKA.md` (Twój własny wcześniejszy audyt, 13 źródeł) i KAŻDY potwierdzić na działającym klonie serwera: zrzut prawdziwego klienta, wynik bota albo odczyt logu/RCON. Bez skrótów, bez „powinno działać”. Wszystko po polsku (teksty dla gracza też).

## Gdzie co jest
- Katalog pracy (zapis): `/home/dev/Desktop/Przemek/MC/tools/ui-naprawa/` = `$U`.
- Produkcja (TYLKO odczyt): `/opt/docker/mc/data/world1/plugins/`.
- **Zmiany konfigów**: skopiuj plik z produkcji do `$U/staging/plugins/<ta sama ścieżka względna>` i edytuj kopię. Staging to nakładka: przy wgraniu na klon (a potem na produkcję) podmieni tylko te pliki. Nie wrzucaj do stagingu plików, których nie zmieniasz. Pliki zasobów (font/tekstury MythicHUD, Nexo) też idą przez staging.
- **Kod pluginów** (gałęzie założone z wersji działających na world1 — md5 sprawdzone):
  - Zaklęcia: `/home/dev/Desktop/Przemek/MC/.claude/worktrees/ui-zaklecia/MCMMORPG/plugin-src/zaklecia`
  - Okno E i podstrony menu (KsiegaPostaci): `/home/dev/Desktop/Przemek/MC/.claude/worktrees/ui-ksiega/MCMMORPG/plugin-src/ksiega-postaci`
  - Paski mobów: `/home/dev/Desktop/Przemek/MC/.claude/worktrees/ui-paski/MCMMORPG/plugin-src/paski-mobow`
  - Chowańce: `$U/src/chowance` (osobne repo git)
  - Nie commituj (zrobi to nadzorca); zostaw zmiany w drzewie roboczym.
- Klon: kontener `ui-klon` = pełny zestaw pluginów world1 + światy world, creator_limbo, stolica_odnowa (bez baz danych — nowe konto przechodzi kreator postaci jak nowy gracz). Dane: `$U/data` (logi czytaj, pluginów tam nie edytuj — wgranie idzie przez kolejkę). Log bazowy WARN/ERROR: `$U/log-bazowy.txt`.
- Klient QA: kontener `mc-client-ui`, vanilla 1.21.11, 1920×1080, konto `UiQA` (bez OP). GUI scale w `$U/klient/instance/options.txt` (`guiScale:`) — zmiana wymaga `klient stop` + `klient start`. Paczkę zasobów serwera akceptujesz przy pierwszym wejściu (kliknij przycisk widoczny na zrzucie). Drugie konto z OP do sprawdzenia ścieżki właściciela: dopisz w kolejce `rcon op UiQA` tylko na czas takiego testu i zdejmij `rcon deop UiQA`.
- Stan PRZED (zrzuty audytu): `$U/zrzuty-audytu/`. Nowe zrzuty lądują w `$U/zrzuty/` — obejrzyj KAŻDY (masz podgląd obrazów), w razie potrzeby kadruj/powiększaj PIL-em do `$U/zrzuty/` (skala całkowita, NEAREST).
- Bot mineflayer: harness w `$U/bot/lib` (z testu zaklęć; node_modules podmontowane, `require('mineflayer')` działa). Swoje skrypty kładź w `$U/bot/*.js`; serwer `ui-klon:25565`, offline.

## Kolejka (nie masz dockera — wszystko przez nią)
Utwórz `$U/kolejka/<unikalna-nazwa>.req`, jedna akcja na linię (`#` = komentarz). Runner wykonuje po kolei i pisze `<nazwa>.out` oraz `<nazwa>.done` (kod; ≠0 = przerwano na tej linii). Czekaj na `.done` (sprawdzaj co ~15 s). Akcje:
- `wgraj` — staging/plugins + staging/jary → klon, restart klonu, czeka na start, wypisuje NOWE rodzaje WARN/ERROR względem logu bazowego. Nowy błąd = Twoja wina, dopóki nie udowodnisz inaczej.
- `buduj zaklecia|ksiega|paski|chowance` — gradle w kontenerze; jar trafia do `$U/staging/jary/` (potem `wgraj`).
- `rcon <komenda>` — konsola KLONU (scena, odczyty, `papi parse UiQA %...%`, `mmocore admin ...`, `dm open <menu> UiQA`, `mm m spawn ...`).
- `klient start|stop|log`, `x <argumenty xdotool>` (np. `x key q`, `x mousemove 960 540 click --delay 200 1`), `zrzut <nazwa>`, `czekaj <sek>`.
- `log <n>` / `grep <regex>` — log klona.
- `bot <plik.js>` — node 22 na sieci klona, limit 10 min.
- `prod-czytaj <ścieżka w /opt/docker/mc/data/world1/>` — odczyt pliku (np. czegoś spoza Twojego zasięgu).

## Ustalenia właściciela (NIE łamać — możesz krytykować sposób, nie decyzję)
- Menu Q = jedna strona 12 kafli + dół, bez drugiej strony; nie zmieniaj nazw kafli bez wyraźnej potrzeby (#28: rozwiąż opisem/ikoną/grupowaniem w grafice, nie nową stroną). Każda funkcja dostępna z menu; gracz nigdy nie musi wpisywać komendy, by otworzyć okno. Q działa także z pustą ręką (MenuKey + plugin Zaklęcia) — sprawdź to w kliencie, zanim poprawisz pomoc (#8).
- Sterowanie zaklęć ostateczne: PPM = lewa (trzymaj), LPM = prawa (klik), Shift+Q = pasek/Księga.
- Styl GUI jak Wynn: ~13 barw, jeden kontur, płaskie tony. Tła okien to glify w tytule — tytuł ma być SAMYMI glifami poprzedzonymi `&f` (tekst w tytule rysuje się NA banerze, a brak `&f` przyciemnia glif do ~25%). Zarzut #10 rozwiąż bez tekstu w tytule.
- Napisy w teksturach: bitmapowa czcionka MC; tekstury menu 32 px, przedmiotów 16 px; podglądy tylko w skali całkowitej.
- Nie generuj menu starymi generatorami (`chowance-staging/B/.../generuj_menu.py`, `tools/handel/generuj.py`, `wiary/menu/generuj_menu.py`) — nadpiszą nowy wygląd. Edytuj YAML-e z produkcji.
- Pasek akcji: arbiter MythicLib `ActionBarHandler.show(priorytet, ticki, tekst)`; skala: MMOCore 10, nawigacja 20, info 30, odmowa 40. `PlayerData.displayActionBar` zamiast gołego `sendActionBar` (MMOCore zjada go po 0,25 s).
- Pułapki i kontekst: `/home/dev/.claude/projects/-opt-docker-mc-data-world1/memory/` — przed ruszeniem danego systemu przeczytaj odpowiednie pliki: `mythichud-warunki-i-wspolrzedne.md`, `deluxemenus-pulapki.md`, `glif-panelu-tint-tytulu.md`, `menu-ksiega-podstrony.md`, `menu-q-ksiega-12-kafli.md`, `komunikaty-audyt.md`, `mmocore-pasek-akcji-nadpisuje.md`, `mythicmobs-cicha-walidacja.md`, `mm-warunki-inline-to-rzucajacy.md`, `hud-karta-wedrowca.md`, `mapa-swiata.md`, `paski-mobow.md`, `zaklecia-skyrim-stan.md`, `pety-stan.md`, `nexo-pack-trzy-pulapki.md`, `gui-styl-jak-wynn.md`, `kamera-areny-i-dialogi.md`, `cmi-alias-tp-kill.md`.
- Najpierw oficjalna dokumentacja pluginu (MythicHUD, DeluxeMenus, NMinimap, MMOCore, MythicLib, CMI, MythicMobs), potem zgadywanie. Masz wyszukiwarkę.

## Zakazy
- Żadnych zmian na produkcji, żadnych restartów/reloadów world1, nic poza kolejką.
- Nie odpalaj scenariusza przywołaniec + drugi gracz/PvP (znany crash serwera, osobny wątek).
- Nie usuwaj funkcji, żeby „zniknął zarzut”. Nie zmieniaj balansu (koszty, czasy, obrażenia, długość tury) — tylko prezentację i komunikację.
- Nie zostawiaj OP na UiQA po teście.

## Jak pracować
1. Zweryfikuj każdy zarzut na klonie ZANIM go naprawisz (te z [niepotwierdzone] mogą być fałszywe — wtedy status FAŁSZYWY z dowodem).
2. Naprawiaj partiami (najpierw WYS), po każdej partii: `wgraj` → zero nowych WARN/ERROR → test w kliencie jak gracz (UiQA bez OP, klikasz tak jak gracz; konsola tylko do sceny i odczytów) → zrzut → obejrzyj i popraw, co widać źle.
3. Sprawdź regresje wszystkiego, czego dotknąłeś: menu Q (Q z pustą ręką i z przedmiotem), każda zmieniona podstrona otwiera się i wraca, HUD przy GUI 2/3/4 (dzień i noc: `rcon time set midnight`), walka z mobem MM z paskiem moba, Shift+Q, Księga Zaklęć, Portal, okno E. Pierwsze wejście nowej postaci (kreator → Stolica) zobacz przynajmniej raz.
4. Jeśli coś jest technicznie niewykonalne — status NIEWYKONALNE z dowodem (dokumentacja/test), nigdy ciche pominięcie.

## Wynik (krótko — szkoda tokenów)
`$U/RAPORT.md`, maks. 90 linii:
- tabela wszystkich 29 wierszy: `| # | status (NAPRAWIONE / CZĘŚCIOWO / FAŁSZYWY / NIEWYKONALNE) | co zmieniono (pliki) | dowód (zrzut z $U/zrzuty/ albo plik .out:linia) |`;
- lista zmienionych plików stagingu i gałęzi kodu (ze zbudowanymi jarami);
- wymagania wdrożenia na produkcję (restart? reload? komendy po wdrożeniu? kolejność?);
- czego NIE dało się sprawdzić na klonie i trzeba obejrzeć na produkcji.
Odpowiedź końcowa do mnie: 5 linii + ścieżka raportu. Nic więcej.
