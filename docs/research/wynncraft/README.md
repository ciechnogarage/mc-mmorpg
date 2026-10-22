# Wynncraft: kreator postaci i menu postaci. Synteza researchu (2026-09-22)

Cztery raporty cząstkowe, od najogólniejszego do najbardziej praktycznego:

| Plik | Co zawiera |
|---|---|
| `1-research-tekstowy.md` | Wiki, forum, kod Wynntils, struktura packa. **Część faktów poprawiono w pliku 2 (lista niżej).** |
| `2-luki-i-spolecznosc.md` | Reddit (przez arctic-shift), przepływ nowego gracza, wszystkie okna menu postaci, porównanie z Hypixel SkyBlock, MCC Island i Telos |
| `3-analiza-wizualna.md` | Klatki z 3 filmów (2024–2026), oficjalny pack 2.1 i jego shader, geometria okien, przejścia, cutscenka |
| `4-jak-zbudowac-u-nas.md` | Technika na naszym stacku, **dwa PoC sprawdzone na prawdziwym kliencie**, tabela kosztów i ryzyk, plan wdrożenia |

Katalogi:
- `klatki/`: 23 klatki z nagrań i tekstury (nazwy z sygnaturą czasu filmu),
- `poc-zrzuty/`: zrzuty z naszych testów (a-* scena w spectatorze, c-* klikanie, fs-* okno pełnoekranowe),
- `poc-kod/`: `ScenePoc.java`, `FullscreenPoc.java`,
- `zrodla/`: skrypty do Reddita.

Filmy, klon Wynntils i packi zostały w katalogu tymczasowym zadania, który zniknie razem z zadaniem. W razie potrzeby pobierzesz je ponownie z adresów podanych w raportach.

## Poprawki do raportu 1

- Sloty postaci na start: **5** (wcześniej 6). Trzy sloty były tylko w becie Wardrobe Wonders.
- Usuwanie postaci trwa **najwyżej 5 minut**, nie miesiąc (zmiana w 2.1.3; wiki jest nieaktualna).
- Nick postaci i backup usuniętych postaci wymagają rangi **HERO+**, nie CHAMPION.
- `ascent 64` z pliku `interface_disabled.json` jest nieaktualny. Efektywnie panel wystaje około 29 px nad skrzynię.
- Styl oficjalny to **pergamin, drewno i kamień**, a nie fiolet. Fiolet i breadcrumby dodaje Dark Pack.

## Jak Wynncraft to zbudował: rozstrzygnięte

1. **Ekran wyboru i kreator to scena w świecie, a nie okno.**
   - Każda klasa ma własną lokację, w której NPC w pętli odgrywa jej spelle.
   - Klasy przełącza się przez sekundę czerni.
   - Postać gracza stoi przed drzwiami (lewy klik) i koszem.
2. **Gracz NIE jest w spectatorze.** Na klatce `10_…B2m10` widać celownik, a przeglądanie idzie scrollem. Spectator ukrywa celownik i zjada scroll na prędkość lotu. Wniosek: tryb adventure z zamrożeniem, niewidzialność (znika ręka), pusty hotbar i vanillowy HUD przezroczysty w packu (Wynn i tak rysuje własny hotbar glifami).
3. **HUD w rogach ekranu** (logo, świat, wersja, karta postaci) robi **core shader tekstu**. Sterowanie idzie przez „magiczną alfę” teksela glifu: kod jest w oficjalnym packu, w `shaders/include/text.glsl`. Iris z shaderpackiem to wyłącza.
4. **Okna** to skrzynie z glifem tła w tytule, czyli tak jak u nas. Każdy stan okna ma osobny glif. Przyciski to `minecraft:potion` z 12 komponentami, kolorowane przez `custom_color`, często szerokie na 3 sloty. Część okien jest pełnoekranowa, z fałszywymi przedmiotami w części ekwipunku gracza.
5. **Przejścia** robi title z pełnoekranowym czarnym glifem (`height 1024`). Po utworzeniu postaci jest cutscenka: kamera leci przez 4–5 ujęć, potem logo na czerni, a na końcu pierwsza osoba w wozie karawany.
6. **Nick wpisuje się na czacie**, a tryby (Hardcore, Ironman…) wybiera się na osobnej kartce z przełączaniem stanów.

## Co to znaczy dla nas

**Stan wyjściowy:** world1 ma plugin `CharacterCreator` (świat `creator_limbo`, SPECTATOR, okno SHULKER_BOX). Scena w stylu Wynna to więc **rozbudowa tego pluginu**. PacketEvents i ProtocolLib już są. MythicHUD z nexo-hook ma już w `pack.zip` shader `rendertype_text`.

**Decyzja do podjęcia: jak trzymać gracza w scenie.**

| Wariant | Plusy | Minusy |
|---|---|---|
| A. Spectator z `setSpectatorTarget` na ItemDisplay (sprawdzony w PoC) | Brak HUD, ręki i celownika bez żadnych zmian w packu; kamera w pełni zablokowana | **Scroll nie dociera do serwera**, więc przeglądanie idzie przez A/D (`PlayerInputEvent`). Trzeba blokować shift, menu spectatora (1–9, teleport SPECTATE) i ukrywać graczy przed sobą |
| B. Adventure z zamrożeniem jak u Wynna | Scroll działa, zachowanie jak u Wynna | Vanillowy HUD trzeba ukryć packiem (globalnie, więc własny hotbar w HUD) albo pogodzić się z jego widocznością |

Rekomendacja: **A na start**, bo mamy go sprawdzonego i gotowego, a kreator już używa spectatora. B dopiero wtedy, gdy zrobimy własny HUD hotbara.

**Plan (z raportu 4, kolejność):**
1. tinty modeli i `oversized_in_gui: true` (wymagane od 1.21.6 dla przycisków na kilka slotów; nasze `cc/items/*.json` go nie mają),
2. glify HUD (karta postaci, podpowiedzi sterowania),
3. scena w `CharacterCreator`, najpierw na serwerze testowym, z evalem botem,
4. karta postaci w MythicHUD (róg ekranu, z kopią na dole pośrodku na wypadek Irisa),
5. scenerie i przejścia (fade glifem),
6. tryby Hardcore i Ironman (LuckPerms + `isPermissionSet`),
7. moduł okien pełnoekranowych (PacketEvents, około 150 linii), najpierw zamiast fizycznego czyszczenia plecaka w kreatorze,
8. drzewko MMOCore: najpierw sprawdzić jednym węzłem, czy `item_model` działa, i dopiero potem rysować około 108 modeli.

**Nie kopiować:** mało slotów postaci na start, packa, który crashuje grę, gdy gracz go nie przyjmie, ani braku wyboru wyglądu (tu mamy przewagę: rasa jako skórka).

## Nadal nieustalone

- pełny changelog 2.1.1 (jest tylko na Discordzie),
- motywacje devów (brak devbloga),
- oficjalne tekstury ekranu wyboru po 2.1.1 (pack podawany jest dopiero przy logowaniu na konto premium),
- czy czerń po ✓ w wersji 2026 to glif pod HUD-em, czy efekt Blindness,
- Wardrobe i overview w wersji 2026 (brak nagrań).

## Runda 5: pełny audyt serwera (2026-09-23)

Katalog `audyt/`. Zacznij od `audyt/00-streszczenie.md`. Zawiera:
- próbę wejścia bez konta (odrzucona: „Invalid session”) i procedurę logowania kodem urządzenia dla właściciela,
- statystyki z publicznego API (przedmioty, drzewka, krzywa XP, świat, gildie),
- inwentarz packa i technikę,
- klatki z nagrań 2026,
- opinie graczy,
- tabelę „co przenieść do nas” (`audyt/99-co-przeniesc-do-nas.md`).
