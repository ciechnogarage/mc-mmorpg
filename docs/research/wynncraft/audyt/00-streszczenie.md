# Audyt Wynncrafta (runda 5, 2026-09-23): streszczenie

Zakres: cały serwer (nie tylko kreator i menu postaci z rund 1–4). Stan gry: **v2.2.4** (MOTD, hotfix 2.2.4_3 z 16.09.2026).

Oznaczenia w raporcie: **[F]** fakt ze źródłem (URL, plik, klatka), **[W]** wniosek. Liczby są z własnych skryptów (`skrypty/`), a wyniki w `dane/`.

## Pliki

| Plik | Zawartość |
|---|---|
| `01-wejscie-i-onboarding.md` | próba wejścia bez konta (wynik, komunikat, zrzut), server list ping, **gotowa procedura logowania kodem urządzenia (nieuruchomiona)**, sieć światów, onboarding, retencja |
| `02-klasy-i-walka.md` | klasy i archetypy z ocenami, drzewka (liczby), spelle i kombinacje, ultimate, mana, skill pointy, **krzywa XP 1–121**, aspekty, statusy, śmierć |
| `03-przedmioty-crafting-ekonomia.md` | 6 725 przedmiotów (typy, rzadkości, poziomy), 90 identyfikacji i zakres rolli, krzywe HP i DPS, powdery, 720 receptur, 972 składniki, 52 surowce i 16 787 węzłów, waluta, upusty, Trade Market, inflacja |
| `04-swiat-i-tresci.md` | regiony, znaczniki mapy i usługi, ~303 questy, jaskinie, 70 world eventów, 11 dungeonów, 5 raidów (struktura), 7 lootrunów, bossy, transport, wierzchowce, housing, festiwale, muzyka z nut |
| `05-party-gildie-terytoria-pvp-zasady.md` | party finder, 18 301 gildii, 437 terytoriów (graf, zasoby, ulepszenia), wojny, sezony, PvP i tryby, **20 zasad gry i lista modów** |
| `06-monetyzacja-i-kosmetyki.md` | rangi (ceny, perki), shares i subskrypcja, 10 typów bomb z cenami, skrzynki i szanse, 651 skórek, koszty serwera, reakcje graczy |
| `07-ui-ux-okna-hud-dialogi.md` | 64 okna, styl „diorama”, HUD z plików packa, **nowy system dialogów** (portrety, style), nowe tooltipy, nameplate'y, hologramy, tytuły, sidebar, dźwięki UI |
| `08-technika-pack-shadery-infrastruktura.md` | inwentarz packa 2.1, obfuskacja zip i nazw, modele-kości mobów, VFX z sekwencji modeli, aktualna struktura packa 2.2 (z packów społeczności), nowe shadery (emissive, perspectiveModel, disableBadges), muzyka z nut, infrastruktura, API v3 (pola postaci) |
| `09-opinie-graczy.md` | co chwalą i co krytykują (Reddit 2025–2026, filmy), wzorce reakcji twórców |
| `99-co-przeniesc-do-nas.md` | **tabela 63 elementów**: co robi Wynn → jak u nas → koszt → priorytet, z kolejnością wdrożenia |
| `dane/` | 23 pliki CSV/JSON/TXT (statystyki API, krzywe, drzewka, spelle, aspekty, eventy, raidy, obozy, sezony, okna Wynntils, ping) |
| `klatki/` | 38 plików: zrzuty próby wejścia (Z*), montaże tekstur packa (P*), klatki z 7 filmów 2025–2026 (V*, nazwa = treść + ID filmu + czas) |
| `skrypty/` | skrypty użyte do zbierania i liczenia (ping, API, zip, montaże, wiki, Reddit, wideo, `launch.sh`) |

## Najważniejsze ustalenia

**Wejście do gry.** Serwer odrzuca klienta bez konta: „Failed to log in: Invalid session (Try restarting your game and the launcher)” (`klatki/Z01_…`). Weryfikacja sesji jest przed wysłaniem packa, więc bez konta nie widać nic poza listą serwerów. Zabezpieczeń nie obchodziłem. W `01-…` jest gotowa procedura: PortableMC 5.0.5 z logowaniem kodem urządzenia, nasz obraz z Xvfb, sieć bridge. Uruchamia ją właściciel. Sesję najlepiej prowadzić ręcznie przez VNC, bo zasady zakazują botów.

**Skala (23.09.2026).**
- 886 graczy online na 52 światach (mediana 13 na świat, limit 1 500), NA 303, AS 278, EU 145 widocznych,
- 18 301 gildii, 437 terytoriów w rękach 23 gildii (4 gildie trzymają 59%),
- w top 100 poziomu wszyscy mają maksymalny 121, mediana czasu gry 1 350 h.

**Walka.**
- 5 klas × 3 archetypy. Drzewko ma 88–97 węzłów na 9 stronach, a 50 AP wystarcza na 35–39% drzewa.
- 4 spelle z 3-klikowych kombinacji (4 wzorce, łuk odwrócony) i od 2.2 ultimate'y pod klawiszem F.
- Koszt many rośnie przy powtarzaniu spella.
- Krzywa XP: poziom 100 to 12,9% drogi do 120, a bonusowy 121 kosztuje tyle co 1–120.
- 128 aspektów ulepszanych duplikatami (do 150 kopii).

**Przedmioty.**
- 6 725 wpisów, 5 413 sztuk ekwipunku bojowego, pik na poziomach 90–99.
- 90 typów identyfikacji. Rolle to 30–130% bazy, a od 2.2 tooltip pokazuje jakość rolla bez modów.
- 267 przedmiotów z major ID. Mythic wypada 1/142 857 ze świata, a z lootrunu 1/2 500 na pull.
- Crafting: 720 receptur i 6 swobodnych składników z modyfikatorami pozycji. 52 surowce na 16 787 węzłach.

**Treść.**
- ~303 questy i mini-questy, 130 jaskiń, 98 sekretnych odkryć, 70 world eventów (timer, instancja per party), 11 dungeonów za klucz, 5 raidów (3 pokoje × 2 warianty, pokój buffów, timer, kolejka dopełniająca), 7 lootrunów (beacony, 137 nazwanych zadań, 29 misji, 13 triali).
- World eventy są największym źródłem unikalnego lootu: 377 z 853 opisanych przedmiotów.

**UI i technika.**
- Okna to „dioramy” (stragan, stół, tablica, sejf).
- Nowy system dialogów: ramka nad hotbarem, 35 portretów z mimiką, 5 stylów frakcji, fikcyjne alfabety. Gracze krytykują go za zasłanianie HUD-u w walce.
- Nowe tooltipy mają ikony ✔/✘ wymagań, miernik rolla, sockety i stronicowanie.
- Pack jest celowo uszkodzony (zatarte nagłówki zip) i od 2.2 ma zahaszowane nazwy tekstur.
- Moby to „kości” w stylu ModelEngine (6 823 modeli). VFX spelli to sekwencje modeli na `item_display`, a nie cząsteczki (Judrajim: 106 klatek).
- Muzykę gra serwer nutami: 17 instrumentów, a cała vanillowa muzyka jest wyciszona.
- Nowe shadery: świecenie przy alfie 254 i dwa wyglądy modelu (GUI i świat) przy alfie 252/253.

**Monetyzacja.**
- Rangi od 14,99 do 170,99 USD dają wygodę i status, plus kontrowersyjne „pożyczone” +2/+4 AP.
- Shares (1 USD) są wymienialne na emeraldy, więc F2P może kupić rangę. To najlepiej oceniany element.
- Bomby dają globalny buff dla całego świata (XP, loot, dungeony).
- 651 skórek, 83% z festiwali.
- Koszt serwera według Salteda to ponad 40 000 USD miesięcznie.
- Gracze najostrzej reagują na odbieranie perków, podwyżki i stoisko sklepu na wyjściu z samouczka.

**Opinie.** Chwalą questy i świat (Fruma „2–3× lepsza”), loot i buildcrafting, crafting, uczciwą monetyzację wobec Skyblocka. Krytykują dialogi zasłaniające HUD, lag przy masowych eventach, słabe ultimate'y, inflację mythików, zaniedbane dungeony i przytłaczający start (skill pointy, Content Book).

## Najważniejsze wnioski dla nas (skrót z `99-…`)

P1 (fundament):
- kombinacje klików (MMOCore `KEY_COMBOS`), 5 atrybutów z wymaganiami na przedmiotach, krzywa XP,
- losowe ID z identyfikacją i czytelnym tooltipem (MMOItems + glify Nexo),
- jaskinie, dungeon za klucz (MythicDungeons), questy z failsafe'ami,
- prowadzenie gracza (sidebar celu, wiązka, hologramy ✔/✘, tytuły lokacji), „pakiet miasta”,
- okna-dioramy i HUD z glifów (MythicHUD),
- upusty waluty i regulamin z listą modów,
- definicje modeli 1.21.4.

Czego nie kopiować:
- dialogu zasłaniającego HUD,
- odbierania perków,
- sklepu na ścieżce samouczka,
- pożyczonych AP,
- obfuskacji packa (dla nas koszt bez zysku),
- wielu kroków do prostych rzeczy (Ability Shardy, schowane „Use Items”).

## Czego nie udało się zebrać i dlaczego

- **Gra na żywo** (pobranie packa, lobby, kreator 2026, Wardrobe, okna po zalogowaniu): serwer wymaga konta Microsoft. Procedura dla właściciela jest w `01-…`.
- **Aktualny oficjalny pack 2.2.4**: podawany tylko zalogowanym. Najnowszy publiczny adres CDN prowadzi do packa 2.1 (22.08.2024). Stan 2.2 odtworzony z packów społeczności (WynnEdits 3.5, Vulkan fix, Wynnilla), które nadpisują te same ścieżki. Kolory WynnEdits są przyciemnione, kształty oficjalne.
- **Szczegóły postaci z API** (`/characters/{uuid}`) i pozycje graczy: 403 bez tokenu (prywatność per gracz). Pola znamy z profili publicznych i dokumentacji.
- **Pełna lista zasad**: strona forum przez WebFetch (streszczenie). Pełnego tekstu nie archiwizowałem.
- **Reddit**: część zapytań zwróciła 0 wyników (limity i timeouty arctic-shift), a napisy 3 filmów padły na HTTP 429 YouTube. Pokrycie opinii jest wyrywkowe (589 postów, 17 wątków z komentarzami).
- **Tabele wiki są częściowo nieaktualne** (Content Book bez questów Fruma, ceny rang sprzed zmian 2025). Zaznaczam to w tekście.
- **Wydajność serwera** (TPS, sprzęt): brak publicznych danych poza wypowiedzią o kosztach i opiniami graczy o lagu.
- **Changelogi 2.2.1–2.2.3 w całości**: przejrzany tylko 2.2.4 (API publisher) i strony wiki 2.2.0–2.2.3.

## Surowe materiały (poza repo, w tmp zadania)
`/home/dev/.claude/jobs/71303296/tmp/wynn-audit/`:
- `api/` (surowe JSON-y z API ~13 MB, wiki wikitext ~130 stron, Wynntils Static-Storage, profile graczy, artykuły),
- `pack/` (pack 2.1 i 4 packi społeczności rozpakowane),
- `video/` (7 filmów 720p, klatki co 10 s, arkusze 4×4, napisy),
- `reddit/` (posty i drzewa komentarzy),
- `instance/` (log klienta z próby wejścia),
- `captures/` (zrzuty i montaże).

Katalog zniknie razem z zadaniem. Adresy źródeł są w raporcie.
