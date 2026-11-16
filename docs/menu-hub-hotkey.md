# Menu-hub pod jednym klawiszem

**Status: DZIAŁA na world1 od 2026-09-20.** Menu otwiera **klawisz Q**. Ten plik opisuje,
co stoi, gdzie to leży i czego jeszcze brakuje. Poprzednia wersja tego pliku była briefem
przed budową — historia decyzji jest teraz w komentarzach samych plików konfiguracyjnych.

## Czego chciał właściciel

- Zero komend dla gracza; wszystko z jednego menu pod jednym klawiszem.
- Podział logiczny na sekcje, nie jedna lista wszystkiego.
- Ładnie — spójnie z tym, co już dobrze wygląda (kreator postaci, panel ekwipunku).
- Szkielet, w który wpinają się kolejne systemy.

## Co stoi dzisiaj

Menu w całości to **konfiguracja** — DeluxeMenus plus istniejące pluginy. Jedyny własny kod
to **MenuKey**: ~jedna klasa, która przechwytuje klawisz, i nic poza tym (dlaczego musiał
powstać — niżej).

| Element | Gdzie |
|---|---|
| Klawisz Q → menu | plugin **MenuKey** (`plugin-src/menu-key`, jar w `plugins/`) |
| Ekrany menu | `plugins/DeluxeMenus/gui_menus/hub_*.yml` (8 plików) |
| Rejestracja ekranów | `plugins/DeluxeMenus/config.yml`, sekcja `gui_menus` |
| Tło panelu | glif Nexo `menu_hub_bg` (`plugins/Nexo/glyphs/menu_hub.yml`) |
| Ikony kafelków | przedmioty Nexo `hub_*` (`plugins/Nexo/items/hub_icons.yml`) |
| Generatory grafiki | `tools/menu-hub-gui/generate_bg.py`, `generate_icons.py` |
| Eval (PASS/FAIL) | `tools/menu-hub/run.sh`, diagnostyka komend `tools/menu-hub/run-diag.sh` |

Układ menu głównego (okno 3×9, rząd 1 pusty celowo):

```
rząd 2:  10 Postać · 11 Ekwipunek · 12 Umiejętności · 13 Profesje
         14 Przygoda · 15 Ekonomia · 16 Społeczność
rząd 3:  18 Pomoc · 22 Ustawienia · 26 Zamknij
```

Sekcje wpinają się w istniejące systemy, niczego nie przepisując: Profesje prowadzą wprost
do `profession_main` (DeluxeMenus), reszta woła komendy MMOCore i MMOProfiles.

## Klawisz: dlaczego Q i co to kosztuje

Serwer **nie może dodać własnego bindu** — klient vanilla wysyła tylko to, co przewiduje
protokół. Realnie do przejęcia są dwa klawisze:

- **F** (zamiana rąk) — należy do MMOCore (tryb rzucania zaklęć). Decyzja właściciela.
- **E** (ekwipunek) — **klient go nie wysyła**. W liście pakietów serverbound nie ma
  zdarzenia otwarcia ekwipunku; nasłuch przy wciśniętym E daje wyłącznie tick/keepalive/pong.
  Serwer wie dopiero o ZAMKNIĘCIU okna.

Zostaje **Q** — i działa **zawsze, także przy pustej ręce**.

Nie udało się tego zrobić samą konfiguracją i to jest cały powód istnienia pluginu MenuKey.
`PlayerDropItemEvent`, na którym stał pierwszy wariant (CoreTools), jest zdarzeniem
o **upuszczonym przedmiocie** — przy pustej ręce nie ma co upuścić, więc nie leci. Zmierzone
botem: pakiet dochodzi do serwera, a żadne zdarzenie Bukkita się nie odpala, bo Bukkit nie
ma zdarzenia „gracz wcisnął Q". MenuKey słucha więc samego pakietu (ProtocolLib,
`BLOCK_DIG`), reaguje **wyłącznie** na `DROP_ITEM`/`DROP_ALL_ITEMS` — kopanie bloków zostaje
nietknięte — anuluje go, żeby Q nie robiło dwóch rzeczy naraz, i woła komendę z configu.

Stare wiązanie CoreTools jest usunięte (leży jako `.bak.przed-pluginem`): klawisz ma mieć
jednego właściciela. Komendy `/menu` i `/menuglowne` zostają jako furtka.

## Trzy pułapki, które kosztowały tu najwięcej czasu

1. **Zmiany w `CoreTools/EventsManager/` NIE wchodzą po `/ct reload`.** Komenda melduje
   „successfully reloaded", a zdarzenie przestaje być obsługiwane — także to, które
   działało przed reloadem. Po każdej zmianie w tym katalogu **restart serwera**. Dlatego
   treść akcji siedzi w `Scripts/menu-hub.yml`, który reload już łapie.
2. **W DeluxeMenus `[close]` PRZED komendą zabija jej okno.** Otwarcie nowego okna samo
   zamyka poprzednie; jawne `[close]` z tym konkuruje. Zmierzone: z `[close]` trzy kafelki
   na cztery nie otwierały swojego ekranu, bez niego wszystkie cztery otwierają.
   `[close]` PO komendzie jest bezpieczne i stosujemy je przy przełącznikach.
   **Wyjątek:** `/player` (statystyki MMOCore) nie otwiera się z menu ani bez `[close]`,
   ani z `<delay=2>` — dopiero `[close]` **razem z** `<delay=5>` działa, bo zamknięcie
   i otwarcie trafiają w różne ticki. DeluxeMenus wspiera `<delay=N>` w tickach.
3. **DeluxeMenus nie rejestruje swoich `open_command` w dispatcherze serwera.** Z konsoli
   każda z nich to „Unknown or incomplete command", a `runas=player` w CoreTools też ich nie
   dosięga — choć wpisana ręcznie na czacie działa. Pewna droga to `dm open <menu> <gracz>`
   z konsoli, z nickiem podstawionym przez PAPI `%player_name%`.

Dodatkowo: rozszerzenie PAPI **`player` nie było zainstalowane**, a istniejące menu profesji
ma 132 użycia `%player_name%` w komendach konsolowych — czyli wysyłało literał zamiast nicka.
Zainstalowane (`/papi ecloud download Player`).

## Czego w menu nie ma i dlaczego

Kafelek, który nic nie robi, jest gorszy niż jego brak. Te pozycje są **wyłączone
i zakomentowane** w swoich plikach, razem z konkretnym powodem — wystarczy odkomentować,
gdy za nimi stanie treść:

| Pozycja | Powód (zmierzony botem) |
|---|---|
| Klasa (`/class`) | nie otwiera okna dla gracza, który ma już klasę i zero punktów klasy |
| Punkty podróży (`/waypoints`) | nie otwiera okna — gracz nie ma odkrytego żadnego punktu |
| Dom aukcyjny (`/ah`) | nie otwiera okna; `CoreTools/AuctionHouse/` to wyłącznie pliki producenta |
| Skarbiec (`/vault`) | `permission: 'OP'` w `CoreTools/commands.yml` — gracz dostaje „Unknown command" |
| Wpłać/Wypłać | komendy MMOCore istnieją, ale nie ma zdefiniowanych monet |
| Lochy | `MythicDungeons/maps/` nie ma ani jednej mapy |
| Garderoba | `CoreTools/wardrobe.yml` to plik przykładowy, komenda OP-only |
| Gildie (plugin Guilds) | na serwerze są DWA systemy gildii; menu pokazuje jeden (MMOCore) |

## Wygląd

Menu nie wygląda już jak skrzynia. Składają się na to trzy rzeczy:

1. **Tło panelu** — jeden glif Nexo wstawiony w TYTUŁ okna, 176×168 px (dokładny rozmiar GUI
   skrzyni 3-rzędowej). Ten sam chwyt, którym zrobiony jest panel ekwipunku, i ta sama
   paleta — menu i ekwipunek mają wyglądać jak jeden system.
2. **Ikony kafelków** — 21 własnych przedmiotów Nexo (`nexo-hub_*`) w jednej rodzinie:
   ta sama plakietka, ta sama ramka, różni je symbol i kolor akcentu. Wcześniej były to
   vanillowe przedmioty — głowa gracza obok totemu i komparatora, czyli zlepek z trzech bajek.
3. **Tytuł** — biały tekst na ciemnym pasku z akcentem, ustawiony przesunięciami fontu.

### Czego się tu nauczyliśmy (zmierzone, nie założone)

- **Glif z tytułu rysuje się POD ikonami przedmiotów.** Dlatego panel jest pełny, bez dziur
  na sloty — przedmioty i tak są na wierzchu, a vanillowa tekstura skrzyni znika pod nim
  całkowicie. Sprawdzone zrzutem (`captures/25-glif-test.png`).
- **Okna skrzyni nie da się uniknąć.** DeluxeMenus **ignoruje** `inventory_type: SHULKER_BOX`
  (sprawdzone botem: okno zawsze przychodzi jako `minecraft:generic_9x3`), a CoreTools
  MenuCreator w ogóle nie zna typów okien. Przezroczysta tekstura shulkera z packa — ta,
  na której stoi kreator postaci — jest więc tutaj nieosiągalna.
- **Pozycjonowanie liczy się, nie zgaduje.** Tytuł rysuje się w `(guiLeft+8, guiTop+6)`,
  a glif bitmapowy ma górę w `y_tytułu + 7 − ascent` → dla panelu na całe okno `ascent = 13`.
  Poziomo: `shift −8` stawia lewą krawędź na `guiLeft`, a po glifie (advance = 177)
  `shift −169` wraca piórem na miejsce tekstu. Znaki przesunięć są w foncie `nexo:shift`
  (`U+E104` = −8, `U+E108/E106/E104/E101` = −169), wpiętym referencją w `minecraft:default`.
  **`%mythiclib_space_N%` tu NIE działa.**
- **`ImageDraw` nie miesza kolorów z tłem** — kolor z alfą po prostu nadpisuje piksel, czyli
  robi w panelu dziurę na świat zamiast delikatnego rozjaśnienia. Wszystko półprzezroczyste
  generator liczy sam (`blend()`), a zapisuje jako w pełni nieprzezroczyste.
- Ikona Nexo w DeluxeMenus to `material: nexo-<id>` (hook `NexoHook`).

### Poprawianie wyglądu

```
tools/menu-hub-gui/generate_bg.py      # tło panelu
tools/menu-hub-gui/generate_icons.py   # komplet ikon
```
Obie potrzebują Pillow, więc uruchamia się je w kontenerze (`python:3.12-slim`). Potem
skopiować PNG do `plugins/Nexo/pack/assets/nexo/textures/…`, `/nexo reload all` i ponownie
wejść klientem — pack ma nowy adres, więc klient musi go pobrać od nowa.

## Co zostało do zrobienia

1. **Ekonomia** — cała sekcja czeka na własną treść (patrz tabela wyżej).
2. **Decyzja o gildiach** — który z dwóch systemów zostaje.
3. **Ukrywanie tego, na co gracz nie zasłużył** — DeluxeMenus ma `view_requirement`;
   dziś nie używamy go nigdzie, bo nie ma jeszcze czego warunkować.

## Jak to sprawdzić

```
tools/menu-hub/run.sh        # eval botowy: klawisz, komplet kafelków, nawigacja, każdy cel
tools/menu-hub/run-diag.sh   # co NAPRAWDĘ robi każda komenda wołana z menu
```

Eval zakłada postać botem, przechodzi kreator i klika przez całe menu: 37 sprawdzeń, w tym
to, po co powstał MenuKey — **Q z pustą ręką otwiera menu**. Ostatni przebieg: zero porażek.
Wyglądu nie oceni; na to jest klient QA w kontenerze.

**Uwaga o chwiejności:** cudze GUI potrafi przyjść po sekundzie albo po czterech, zależnie
od obciążenia. Eval czeka warunkowo (do 7 s), a nie sztywno — sztywne czekanie dawało losowe
FAIL-e na kafelkach, które chwilę wcześniej przechodziły.

## Oprawa graficzna (2026-09-22)

Panel i ikony przerobione ze stalowo-niebieskich na cieple fantasy: dab, mosiadz
i zielona skora. Generatory:

| plik | co robi |
|---|---|
| `tools/menu-hub-gui/generate_bg2.py` | tlo panelu 176x168 -> `plugins/Nexo/pack/assets/nexo/textures/gui/menu_hub_bg.png` |
| `tools/menu-hub-gui/generate_icons2.py` | 21 ikon 32x32 -> `plugins/Nexo/pack/assets/nexo/textures/item/hub/` |
| `tools/pixelart/uruchom.sh` | uruchamia oba w kontenerze `pixelart:latest` (biblioteka pod `/pxlib`) |

Stare `generate_bg.py` i `generate_icons.py` sa oznaczone jako nieaktualne.
Po zmianie grafiki: `rcon-cli "nexo reload pack"`, a klient musi ponownie wejsc
na serwer (przegenerowany pack ma nowy hash).

### Trzy rzeczy, ktore trzeba wiedziec

1. **Tint tytulu.** Glif panelu jest TEKSTEM w `menu_title`, wiec bierze kolor
   tekstu. Tytul skrzyni ma domyslnie `0x404040`, wiec bez `&f` na SAMYM POCZATKU
   tytulu cala tekstura leci przez mnoznik ~0.25. Poprawione we wszystkich
   `hub_*.yml` (kopie `*.bak.przed-tintem`).
2. **Projekt idzie za trescia.** Hub uzywa 3..10 slotow z 27: rzad 1 (sloty
   10..16) to tresc, rzad 2 to tylko 18 (Wstecz/Pomoc), 26 (Zamknij) i 22 (tylko
   ekran glowny). Dlatego panel NIE rysuje 18 jednakowych gniazd — rzad 1 dostal
   gladka plyte z polka, rzad 2 belke z wnekami w skrajnych kolumnach. Gniazdo
   pojawia sie tylko tam, gdzie jest ikona, bo plakietke niesie sama ikona.
3. **Ocena tylko z ikonami i tylko w grze.** Panel ogladany na pusto i na bialej
   stronie klamie. Zderzenie zimnych ikon z cieplym panelem oraz przyciemnienie
   tintem wyszly dopiero na zrzucie z klienta QA
   (`output/warsztat/qa/`, procedura w pamieci `klient-qa-w-kontenerze`).
