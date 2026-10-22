# Zlecenie dla Codexa — etap 2: bot ma mierzyć GRĘ, nie siebie; pełny przebieg A–F; review feelingu

Kontekst: Twój etap 1 (PLAN.md, review-kodu.md, bot/, klient-prawdziwy.md) jest w tym katalogu. Przeczytaj je
i `ZLECENIE-etap1.md` (zasady nadal obowiązują). Pierwszy przebieg grupy A (uruchomił Claude):
`kolejka/t0.out` (+ `kolejka/t0.serwer.log`). A01 PASS (zwykły gracz otwiera Księgę z huba), reszta to głównie
błędy harnessu (`bot.openInventory is not a function`, „okno nieotwarte”), a A03 ma 54 aktualizacje BossBara.

## Teraz możesz uruchamiać testy sam — kolejka
Docker jest poza Twoim sandboxem, więc działa wąski runner: zapisz plik
`/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/kolejka/<nazwa>.req` z samym filtrem grup (np. `A`, `BD`, pusty =
wszystko). Runner uruchomi `bot/run.sh <filtr>` na klonie (`node:22`, sieć klona) i zapisze `<nazwa>.out`,
`<nazwa>.serwer.log` (ogon logu klona), a na końcu `<nazwa>.done` (kod wyjścia). Czekaj pętlą na `.done`
(np. `while [ ! -e …done ]; do sleep 5; done`). Przebiegi idą po kolei; limit 25 min na przebieg. Nazwy unikalne
(`e2-01`, `e2-02`…). Log klona na żywo też możesz czytać: `data/logs/latest.log`. Klonu nie restartujesz; jeśli
czegoś potrzebujesz od konsoli poza RCON-em z bota albo restartu/zmiany configu klona — wpisz to w raport.

## Poprawki harnessu (obowiązkowe, od tego zależy wiarygodność)
1. **Cel pod celownikiem liczony, nie ustawiany ręcznie.** Bot patrzy (`bot.look`/`lookAt`), a `klient.js` sam
   liczy cel jak klient: promień z oczu, istoty w `entity_interaction_range` / `attack_range` przedmiotu, bloki w
   `block_interaction_range` (atrybuty z pakietów `update_attributes` — plugin zmienia je w trybie zaklęć!). `setTarget`
   usuń albo zostaw tylko do testów jednostkowych.
2. **E/okno ekwipunku:** prawdziwy klient przy E NIC nie wysyła (patrz pamięć `klawisz-e-i-tab-nie-do-przechwycenia.md`);
   kliknięcia w ekwipunku to `container_click` w oknie 0, zamknięcie = `container_close` 0. Zrób to tak.
3. **Stan zamiast czasu:** po każdej akcji czekaj na zdarzenie/stan (okno otwarte, placeholder zmieniony, BossBar),
   z limitem — nie na stały `sleep`. Klikanie w menu: sprawdź, że okno i jego tytuł/slot są tym, czego chcesz.
4. **Rozdziel BŁĄD HARNESSU od FAIL GRY.** Status `BLAD` = bot nie umiał wykonać kroku; `FAIL` tylko, gdy stan
   serwera (placeholder, `data get`, HP, mana, encje, log) potwierdza złe zachowanie gry. Każdy FAIL gry powtórz
   2× w osobnym przebiegu, zanim go zgłosisz.
5. **Postać jak u gracza:** ustal z configów MMOCore, co ma nowy gracz na world1 (klasa domyślna, mana maks., czy
   zaklęcia są bramkowane klasą/poziomem/permisją). Testuj na takiej postaci; jeśli trzeba nadać klasę (klon nie ma
   kreatora), zrób to w przygotowaniu sceny i opisz jako odstępstwo. Osobno: czy postać BEZ klasy może brać i rzucać.
6. **Śmierć bez konsolowego `kill`:** gracz ginie np. od upadku (bot schodzi z wieży) albo od moba z AI. `kill`
   zostaw tylko jako test kontrolny nazwany wprost.
7. **Drugi bot `MagCel`** (cel-gracz) naprawdę się łączy — do leczenia innych, PvP (strefa allow/deny, jeśli na
   klonie da się ją ustawić — WorldGuard), zasłaniania strumienia. Dwa logowania z jednego IP: bukkit.yml klona ma
   connection-throttle -1.
8. Mineflayer nie wysyła sprintu w `player_input` — wysyłaj ręcznie (już częściowo jest). Q przy pustej ręce
   zweryfikuj w kodzie klienta jeszcze raz (MenuKey ma działać „także przy pustej ręce”).
9. Kółko w pasku Shift+Q: z kodu `Wybor.java` ustal, jak kółko/slot przesuwa wybór — A04 wybrało „Wielkie uzdrowienie”
   zamiast „pusta”; rozstrzygnij, czy to błąd gry czy złe założenie testu.

## Potem: pełny przebieg i ocena
- Doprowadź A–F do stanu: zero `BLAD`, każdy FAIL potwierdzony 2×. Sprawdź każdy punkt z `review-kodu.md`
  testem (potwierdzony / obalony / nie da się botem → etap 3).
- Dopisz brakujące scenariusze z PLAN.md (B01–15 dla KAŻDEGO zaklęcia z configu, C01 broń MMOItems + zaklęcie —
  daj broń jak gracz ją ma, np. z `mi give` w przygotowaniu sceny, nazwij to).
- **Ocena feelingu** w `OCENA.md` (PO POLSKU): dla każdej grupy A–F i każdego typu zaklęcia ocena 1–10 „jak gracz”
  z uzasadnieniem z liczb (opóźnienia w tikach, spam komunikatów/BossBara, migotanie slotów, koszty vs opis, ruch),
  potem lista błędów posortowana wagą (KRYT/WYS/ŚR/NIS): co gracz robi → co widzi → przyczyna plik:linia → test,
  który to pokazuje → propozycja poprawki w 1–2 zdaniach (NIE poprawiaj pluginu). Na końcu: co zostaje dla
  prawdziwego klienta (uaktualnij `klient-prawdziwy.md`).

## Zasady
Jak w etapie 1: bez dotykania produkcji i źródeł pluginów, bez reloadów/restartów. Pisać wolno w katalogu roboczym.
Raport `-o` (`/home/dev/Desktop/Przemek/MC/tools/zaklecia/gracz/raport-etap2.md`) max 40 linii po polsku: wynik
przebiegu (PASS/FAIL/BLAD liczbowo), 5 najważniejszych błędów gry, oceny feelingu w skrócie, czego nie dało się
sprawdzić. Bez kodu i diffów.

Dopisek Claude: run.js mial wiszaca petle zdarzen po wynikach - dodalem wymuszone process.exit; zachowaj to. Przebieg A trwa ~16 s.
