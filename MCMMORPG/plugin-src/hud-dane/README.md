# HudDane

Placeholdery PAPI `%hud_...%` dla MythicHUD: dane, których nie daje żaden inny plugin (drużyna MMOCore,
przywołańce MythicMobs, efekty z czasem, cel walki, zdobycz, cel zadania). Zależności: PlaceholderAPI, MMOCore,
MythicMobs (softdepend MythicHUD, MythicDungeons). Wszystko w RAM, nic nie zapisuje; migawka na gracza żyje krótko
(`HudDane.WAZNOSC_MS`), liczona na żądanie placeholdera, bez własnego zegara na gracza.

## Pliki
- `HudDane.java` — start, migawka drużyny/przywołańców/efektów, cele głów w cache MythicHUD (niżej).
- `Placeholdery.java` — lista wszystkich kluczy (javadoc klasy = pełna specyfikacja `%hud_...%`).
- `Zdarzenia.java` — ostatni cel walki (8 s) i kanał zdobyczy (EXP, przedmioty; 4,5 s).
- `Nawigacja.java` — cel zadania (MMOCore albo samouczek Chowańców), strzałka i odległość, kierunek obrażeń.
- `Migawka.java` — rekord danych gracza.
- `src/main/resources/config.yml` — zasięg drużyny, czas życia przywołańców (ticki), efekty → id ikony, miejsca zadań.

## Główne placeholdery (pełna lista: javadoc `Placeholdery.java`)
`druzyna_liczba`, `druzyna_<n>_{nazwa,nick,poziom,hp,hpmax,zasob,zasobmax,stan,lider}`; `slugi_<n>_{...}`;
`cel_{jest,nazwa,hp,hpmax,poziom,ranga}`; `zdobycz_<n>_{tekst,rodzaj}`; `zadanie_{jest,tytul,postep,krok,kierunek,odl}`;
`efekt_<n>_{id,czas}`; `uklad`, `towarzysze`, `okno`, `loch`, `ostrzezenie`, `awans_*`, `umiejetnosc_<n>_*`, `pet_typ`.
Indeksy od 1, puste miejsce = pusty tekst, liczby = 0. Teksty bez tagów MiniMessage (MythicHUD liczy szerokość z surowego tekstu).

## Cele głów w cache MythicHUD (25.10)
MythicHUD 1.3.4 `PlaceholderAPIParser.parse` zwraca SUROWY tekst, dopóki jego zegar nie wypełni cache gracza;
warstwa head robiła wtedy `getOfflinePlayer("%hud_druzyna_N_nazwa%")` → „Name cannot be longer than 16” ze stack
trace'em (7 na logowanie). Przy `PlayerJoinEvent` HudDane wpisuje `%hud_druzyna_1..7_nazwa%` do prywatnego pola
`cachedPlaceholders` refleksją (API do własnego parsera brak). Po aktualizacji MythicHUD: WARN
`HudDane: nie wpisze celow glow do cache MythicHUD` = refleksja padła, wraca tylko szum w logu (HUD działa).

## Sprawdzenie
Na klonie po wejściu gracza: `grep -c "Name cannot be longer than 16" logs/latest.log` = 0 i `grep -c WARN` przed/po
wejściu gracza bez drużyny (spam WARN = pusty placeholder w warstwie head). Wdrożenia/cofnięcia: `/opt/docker/mc/backups/`
(`hud-pusty-slot-20261021-135301`, `nagroda-mistrza-20261024`), stan: skill `mc-systemy-gracza/menu-ui.md`.
