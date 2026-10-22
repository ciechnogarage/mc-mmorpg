# Polecenie Claude dla Agenta A — runda 2

## Ocena rundy 1 (Claude)
- N1 PRZYJĘTE i zacommitowane (24cc940): DamageCheckEvent dziedziczy po FakeEntityDamageByEntityEvent (sprawdzone
  javap), filtr + osłona poprawne, sonda N1-PvP-wilk PASS. BRAKUJE kontroli ujemnej — patrz niżej.
- N4 przyjęte częściowo (BossBar tylko przy zmianie). Nadal FAIL: `INV-N4-pasek-pakiety` — brak REMOVE BossBara po Q.
- Diagnostyka ZAKDBG przyjęta (debug: false domyślnie).
- N2 i N3: nic nie naprawione. Raport był za płytki (plik -o nadpisał szczegóły) — od teraz szczegóły zapisujesz do
  `naprawy/szczegoly-r2.md`, a `raport-r2.md` (-o) to krótkie podsumowanie.

## Ustalenie Claude do N2 (z Twoich logów a-r1-1x)
Przy „0 obrażeń” wszystko przechodzi: `mmoCanTarget=true`, `losMM=true`, skill rzucony, powstaje zdarzenie
ENTITY_ATTACK 3,5 — i ktoś je ANULUJE (`anulowane=true` w MONITOR). Zaklecia tego nie robią; AnimacjeBroni (kom-ab) nie ma
setCancelled w handlerach obrażeń. Podejrzani: MMOItems/MythicLib (sprawdzanie broni w GŁÓWNEJ dłoni — restrykcje
biegłości/poziomu, cooldown/naładowanie ataku), KlasyBron, CoreTools, WorldGuard. Metoda (obowiązkowa, bez zgadywania):
w trybie debug sonda rejestrująca listener `EntityDamageByEntityEvent` na KAŻDYM priorytecie (LOWEST…MONITOR, z
ignoreCancelled=false), loguje stan isCancelled na każdym → widać, między którymi priorytetami flaga się zmienia; plus
wypis `HandlerList` tego zdarzenia (plugin + priorytet + klasa listenera). Potem przyczyna w kodzie/bajtkodzie tego
pluginu (jary w /opt/docker/mc/data/world1/plugins, możesz rozpakować do swojego katalogu i czytać javap) i poprawka
po naszej stronie (Zaklecia albo nakładka configu) — NIE zmieniamy cudzych jarów.

## Runda 2 — zakres
1. **N1 kontrola ujemna:** `build bazowy` → `test N_r1` (sonda MA położyć/FAIL-ować stary jar; klon może paść — zapisz to)
   → `build` → `test N_r1` PASS. Dopiero to jest dowód.
2. **N2** wg metody wyżej: winowajca, przyczyna, poprawka, sonda FAIL→PASS (bazowy vs Twój build).
3. **N3** prawdziwa poprawka: odtwórz w scenariuszu C Agenta B (`test C`), użyj logów `ZAKDBG pusta/slot`; nośnik nigdy
   nie nadpisuje cudzego przedmiotu ani nie zmienia wybranego slotu. Sonda FAIL→PASS.
4. **N4** dokończ: REMOVE BossBarów przy każdym zamknięciu paska (Q, Shift+Q, śmierć, wyjście, zmiana świata, limit
   czasu); budżet: ≤1 aktualizacja BossBar na akcję gracza.
5. **N5** ładowane „Uzdrowienie innego” (cel-sojusznik) — przyczyna + poprawka + sonda.
6. **N6** panel „Twoje dłonie” w Księdze odświeża się od razu po LPM/PPM; potwierdzenie widoczne w oknie.
7. **N8** spolszczenie „You are now in combat!” w nakładce.
N7 (strumień w 1. osobie) i N9 — runda 3.
Po zmianach: `build`, `test ABCDEF` oraz `test N_` (Twoje sondy). Porównanie A/B: te same testy na `build bazowy`
i na Twoim buildzie — w szczegółach tabela różnic (co naprawione, co zepsute). Nowy FAIL względem bazowego = Twoja
regresja, dopóki nie udowodnisz, że to harness.
Kolejka jest wspólna z Agentem B — łącz przebiegi. Na koniec rundy ostatni `build` = Twoja wersja (nie zostawiaj
klona na bazowym). STOP po raporcie.
