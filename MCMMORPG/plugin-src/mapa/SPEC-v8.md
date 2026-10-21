# Plugin Mapa v8 — POPRAWKI błędów i wydajności (zgłoszone przez właściciela; poprzednie SPEC obowiązują)

## Błąd 1: PPM na pełnej mapie stawia znacznik zamiast przeciągać
Przyczyna: ramki MapEngine są w zasięgu ręki (3 bloki), więc PPM klient wysyła jako `INTERACT_ENTITY` (akcja
INTERACT/INTERACT_AT) na nieznaną serwerowi encję + machnięcie ręką; Paper z machnięcia robi
`PlayerInteractEvent LEFT_CLICK_AIR` / `PlayerAnimationEvent ARM_SWING` => nasz `click()` => dialog „Nowy znacznik”.
Poprawka (klasa ładowana gdy packetevents włączony; bez niego zachowanie jak dziś):
- Nasłuch przychodzących `INTERACT_ENTITY` od graczy z aktywną sesją pełnej mapy: ATTACK -> `click()` (LPM),
  INTERACT/INTERACT_AT -> `session.lastRightTick = tick` (PPM, przy trzymaniu powtarzane co ~4 ticki => przeciąganie).
  Pakiet anuluj (serwer i tak nie zna encji). Przenieś obsługę na wątek główny (`runTask`) — zdarzenie przychodzi na
  wątku sieci.
- W `click()` z `PlayerAnimationEvent` / `LEFT_CLICK_*`: ignoruj, jeśli `tick - session.lastRightTick <= 2`
  (machnięcie od PPM) albo był już klik z pakietu ATTACK w tym/poprzednim ticku.

## Błąd 2: klikanie mapą w ruchu otwiera/zamyka pełną mapę i „idziesz prosto”
- `MapaPlugin.onInteract` (mapa w ręce): gdy pełna mapa aktywna -> nic nie rób (return), nie otwieraj ponownie.
- Zmiana sterowania mapą w ręce: **PPM = następny zoom** (dotąd Shift+PPM), **Shift+PPM = pełny ekran** (dotąd PPM).
  Komunikat zoomu bez zmian. Lore UI_MAPA aktualizuje generator (nie Twoja sprawa).
- Pełna mapa: zamiast blokować ruch (setTo(from)), ZAMKNIJ sesję, gdy gracz przesunie się o > 0,3 bloku w poziomie
  od pozycji otwarcia (ruch klawiszami = wyjście z mapy; obrót głowy nadal = kursor). Skoki/spadanie (zmiana Y)
  bez zamykania. Zdejmij dotychczasową blokadę ruchu.

## Wydajność (zmierzone sparkiem na world1: obróbka obrazu MapEngine na wątku głównym)
- `drawing.image(...)` i `flush()` MapEngine NIE na wątku głównym: wykonuj je w tym samym zadaniu asynchronicznym,
  które rysuje BufferedImage (MapEngine jest przeznaczony do pracy async). Na wątku głównym tylko odczyt stanu
  gracza i zlecenie zadania. Jedno zadanie naraz na sesję (busy) — zostaje.
- Pełne przerysowanie TYLKO gdy zmienił się widok (środek/zoom/zakładka/znaczniki/cel) albo strzałka gracza
  (gracz obrócił się o ≥ 15° lub przesunął o ≥ 1 piksel mapy). Usuń przerysowanie „co 10 ticków”.
- Ruch samego kursora: nie konwertuj całego obrazu. Narysuj tylko wycinek: przywróć prostokąt 9×9 pod starą
  pozycją kursora z obrazu bazowego i narysuj kursor w nowej — wysyłając do MapEngine mały obrazek na przesunięciu
  (sprawdź w API `IDrawingSpace` metodę rysowania obrazu w (x, y); jeśli jej nie ma — rysuj kursor co najwyżej
  5×/s, a nie co 2 ticki).
- `HandMapRenderer`: lista znaczników (`znacznikiDla`) liczona co tick — trzymaj w stanie gracza i odświeżaj
  najwyżej raz na 20 ticków albo po zmianie świata/znaczników.
- `PelnaMapa.tick`: jeśli brak sesji — wyjdź natychmiast (bez pracy).

## Testy JUnit
ignorowanie machnięcia po PPM (lastRightTick); próg ruchu 0,3 (zamknięcie) vs obrót (bez zamknięcia);
warunek pełnego przerysowania (obrót 14° nie, 15° tak).
