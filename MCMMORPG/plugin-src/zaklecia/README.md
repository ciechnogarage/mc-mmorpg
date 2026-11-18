# Zaklecia — zaklęcia w dłoni (Skyrim), obie ręce

Plugin prowadzi rzucanie zaklęć koncentracji, a MythicMobs rysuje efekty i zadaje obrażenia.

- **Rzucanie** (`Rzucanie.java`): PPM trzyma zaklęcie lewej dłoni, LPM włącza lub wyłącza strumień prawej albo ładuje i strzela,
  tiki co 1 tick na wątku głównym, mana co 10 ticków, cele stożka liczone tutaj i podawane skillowi MM
  jako cele. Tryby wynikają z tego, co jest w dłoniach: prawa, podwójne (to samo w obu: mana ×2,5),
  dwa różne (suma), broń w prawej + zaklęcie w lewej.
- **Nośnik** (`Nosnik.java`): `zaklecia przypisz <gracz> <id|nic> <prawa|lewa>` (Księga zaklęć w DeluxeMenus,
  jako konsola) lub pasek Shift+Q. Nośnika nie da się wyrzucić, przenieść, włożyć do ramki, doniczki, półki ani skarbca.
  Z ksiega-postaci (komenda `ksiegareka` istnieje) broń z ręki odkłada Księga (`ksiegareka <gracz> odloz|przywroc
  <prawa|lewa>`): prawe zaklęcie zawsze w slocie 0, lewe w offhandzie, „Pusta” oddaje broń. Bez Księgi — stary `addItem`.
  `ksiegareka` idzie wprost przez `PluginCommand.execute`, NIE `dispatchCommand`: wewnątrz innej komendy (menu
  DeluxeMenus `[console] zaklecia przypisz`, RCON) Paper 1.21.11 kolejkuje zagnieżdżoną komendę do końca zewnętrznej
  (`Commands.executeCommandInContext`), więc Księga odkładała broń PO sprawdzeniu ręki — pierwszy klik w menu z bronią
  w ręce dawał „nie ma gdzie trafić” i zabierał broń (18.11, klon: menu 0/2 → 2/2, `eq/lewa.js` 28/30 → 30/30).
- **Bramka klasy** (`Bramka.java`, decyzja właściciela 21.10): rzucać i brać zaklęcie do dłoni mogą tylko klasy MMOCore
  z `plugins/Zaklecia/klasy.yml` → `dozwolone-klasy` (id klas, wielkość liter bez znaczenia). Plik pisze
  `tools/klasy-rasowe/generuj_klasy.py` (wszystkie klasy archetypów mag i akolita). Brak pliku lub pusta lista = bramka wyłączona.
  Odmowa: `przypisz`/koło Shift+Q oraz LPM/PPM z nośnikiem w dłoni → pasek „Twoja klasa nie włada zaklęciami.”.
  Plik czytany tylko przy starcie pluginu (zmiana = restart).
- **MM**: warunki `zakDlon{reka;z}`, `zakRece{z;min;max}`, mechanika `zakZasob`, warunek `zakMaZasob`.
- Konfiguracja: `config.yml` (zaklęcia, koszt many, stożek, nazwy skilli).

Treść (przedmioty, skille, menu), wdrożenie i evale: `tools/zaklecia/v2/ (repo MC)` i `tools/zaklecia/README.md`.
Build: `./build.sh` → `build/libs/zaklecia-*.jar`.
- **Klawisze (decyzja właściciela 15.11, „każdy klawisz robi jedną rzecz”)**: Shift+Q u klasy spoza `dozwolone-klasy`
  (wojownik, ninja) nie otwiera paska, tylko komunikat `Bramka.ODMOWA` w pasku akcji. Otwarcie paska wychodzi z trybu
  skilli MMOCore (`PlayerData.leaveSkillCasting`) i zamyka pełną mapę (`mapa zamknij <gracz>`); w walce Chowańców
  (metadane `chowance_walka`) pasek się nie otwiera. F (`PlayerEnterCastingModeEvent`) zamyka otwarty pasek.
  `zaklecia pasek zamknij <gracz>` (konsola) zamyka pasek — wołają go Mapa i Chowance. `Wybor.otworz`, `Wybor.trybSkilli`.
- **Wygląd paska Shift+Q (decyzja właściciela 15.11, „Nad hotbar”)**: jedna linia w pasku akcji nad hotbarem
  (`✦ [wybrane] · sąsiednie › │ strumień N many/s`), odświeżana co 20 ticków przez MythicLib `ActionBarHandler`
  (priorytety `Wybor.PRIO_LISTA`=35 < `PRIO_KOMUNIKAT`=40 < `PRIO_RUCH`=41, więc komunikaty przypisania wygrywają z listą).
  Wcześniej bossbary u góry ekranu zasłaniały kartę gracza MythicHUD. Podpowiedź klawiszy idzie na czat, dopóki gracz
  pierwszy raz czegoś nie przypisze (flaga PDC `pasek_wyboru_umie`). Sprawdzone 15.11 w kliencie QA (klon `klawisze`).

## Wydajność
- `Nosnik.jest()`/`pusta()` idą co tick dla każdego gracza (`odswiezPuste`, 36 slotów + ręce). Czytają
  `ItemStack.getPersistentDataContainer()` (widok, Paper 1.21.11) ze strażnikiem `!isEmpty()` — **nie**
  `getItemMeta()` ani `hasItemMeta()`: oba kopiują ItemMeta (`CraftItemStack.hasItemMeta()` woła `getItemMeta()`, bajtkod 1.21.11).
- Pomiar 17.11 (klon `--z-kreatorem`, 1 bot-mag z zaklęciem w lewej, `spark profiler --alloc` 60 s, wątek serwera):
  `odswiezPuste` 41,4 MB → 11,0 MB (stary kod / nowy), `jest`+`pusta` 31,5 MB → 0,5 MB. Testy `eq/lewa.js` 28/30
  i `klawisze/konflikty.js` 25/27 identyczne przed i po (te same FAIL: T8/T9, `*-cyfry-wolne` — zastane).
- **Reguła dla całego pluginu: znacznik przedmiotu czytaj `!i.isEmpty() && i.getPersistentDataContainer().has/get(...)`**,
  nigdy przez `getItemMeta()`/`hasItemMeta()` (dotyczy też `Rzucanie.trybHud`/`nosnik`, `Kostur.odswiez`, `EfektyCzasowe`).
- `Ksiega.w()` (co tick z Nosnika, `Rzucanie.trybLpm` i placeholderów HUD) czyta typ MM wprost z PDC `mythicmobs:type`
  (`ItemKeys.TYPE`, MM 5.11.2), **nie** `getMythicTypeFromItem`: ta w 5.11.2 robi `isMythicItem` (kopia całego NBT przez
  `getNBTData`) i `getItemMeta()`, a typ i tak bierze z tego samego PDC (bajtkod `ItemExecutor`). Różnica: nie migruje
  przedmiotów ze starego NBT `MYTHIC_TYPE` (sprzed PDC) — zaklęcia robi `getItemStack` MM 5.11.2, więc ich nie ma.
- Pomiar 17.11 po obu poprawkach (ten sam klon i scenariusz; C2 = stan po pierwszej poprawce, E = obie):
  `Ksiega.w` 37,2 MB → 0, `Rzucanie.trybyLpm` 22,0 → 1,0 MB, `Nosnik.odswiezPuste` 9,4 → 0 (poniżej próbkowania 0,5 MB);
  cały wątek serwera 180,9 → 119,5 MB/min. Zostało ok. 2,6 MB/min: `RzutyOdrzutowe.tick` 1,6 MB (niezbadane)
  i `getAttribute` Papera w `modDo`. Testy jak wyżej, wynik bez zmian (28/30, 25/27).
