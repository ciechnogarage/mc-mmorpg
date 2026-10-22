# Nowe zaklęcie v4.0

1. Wybierz ID małymi literami, np. `iskry`. Domyślna baza skilli to `Zak_Iskry` (podkreślenia w ID pozostają, np. `Zak_Runa_ognia`).
2. Dodaj `MythicMobs/items/zaklecia/<id>.yml` z przedmiotem `ZAKLECIE_<ID>` i prawdziwym modelem Nexo w `Options.ItemModel`. Tymczasowe modele dopisz do listy TODO grafika.
3. Dodaj `MythicMobs/skills/zaklecia/<id>.yml`. Skille celowane muszą używać `mmoCanTarget{interaction=OFFENSE_ACTION}` i obrażeń `mmodamage` z właściwym żywiołem.
4. Dodaj wpis `zaklecia.<id>` w `Zaklecia/config.yml`: zawsze `typ`, `przedmiot`, `nazwa`, `szkola` (`ogien`, `mroz`, `leczenie`, `przywolanie`, `iluzja`, `ochrona`), `oburacz: {efekt: 2.2, mana: 2.8}`. Koszty są pobierane przez plugin przed efektem.
5. `koncentracja`: `mana-za-tik` pobierana co 10 ticków także bez celu. `cel: stozek`, `siebie` albo `inni`. Dla `stozek` użyj `Start`, `Stop`, `Tik`, `Traf`, `Uderzenie` oraz modelu strumienia. Dla `siebie` zdefiniuj `Tik_P/L/Obie` z `@self`. Dla `inni` plugin raycastem wybiera jeden niewrogi żywy cel do `zasieg`; zdefiniuj `Traf_P/L/Obie` z efektem `heal` bez `@target` i bez warunku `mmoCanTarget SUPPORT` (MythicLib odmawia nie-graczom). Filtr wspólny wyklucza nieumarłych, atronachy i konstrukty. Cząsteczki tylko przy celu.
6. `ladowany`: `czas-ladowania-t`, jednorazowe `mana`; zdefiniuj `Laduj_P/L/Obie` (co 3 ticki), `Gotowe_P/L/Obie`, `Wystrzal_P/L/Obie`. Domyślne `cel: stozek` oznacza pocisk MythicMobs (`projectile` lub `missile`). `cel: siebie|inny|obszar` kieruje efekt bez pocisku przez plugin; `inny` wymaga `zasieg`, brak celu nie kosztuje many i daje dźwięk odmowy; `obszar` wymaga `promien`, zawsze obejmuje rzucającego. Efekt trafienia w skillu nie używa `@target`, bo cele przekazuje plugin.
7. `runa`: pola jak dla `ladowany` plus `zasieg`, `promien`, `limit-aktywnych`, `czas-zycia-t`. Dodaj `Wystrzal_*` dla oprawy punktu, `RunaTik` dla widoczności, `Wybuch` i `Wybuch_Obie` z `mmoCanTarget` na skillu obrażeń. Runa stoi tylko na górnej ścianie bloku.
8. `tarcza`: `mana-za-tik`, `sila`, `redukcja-magia`, `redukcja-fizyczne` (0–1), `narastanie-t`; zdefiniuj `Tik_P/L/Obie` oraz `Peka`. Pula zużywa pochłonięte obrażenia. Plugin rozpoznaje typ MythicLib `MAGIC/SKILL`, przyczyny Bukkit (ogień, mróz, magia, wybuch) oraz pociski inne niż strzały; zwykłe ciosy i strzały są fizyczne.
9. `bron`: pola jak `ladowany`, ponadto `bron` (ID osobnego przedmiotu MM) i `czas-trwania-t`. Miecz zastępuje nośnik w dłoni rzucającej; oburącz jeden w prawej na podwójny czas. Lewą dłonią Minecraft nie wykonuje zwykłych ciosów. Plugin oznacza broń PDC, blokuje wyrzucanie/przenoszenie i przywraca nośnik po czasie, wyjściu lub śmierci.
10. `przywolanie`: pola jak `ladowany`, ponadto `mob` (ID w `MythicMobs/mobs/zaklecia/`), `zasieg`, `czas-trwania-t`. Wspólny limit na gracza to `przywolania.limit` (domyślnie 1, pod Twin Souls 2), niezależnie od rodzaju. Plugin wybiera Target Location, tworzy MM moba, ustawia właściciela i sprząta najstarszego. AI: `gotoowner`, `ownerattacker`, `ownertarget`; PvP i Chowańce wymagają kontroli szkód. `Options.Despawn: true`, `Persistent: false`.
11. `na_cel`: pola jak `ladowany`, ponadto `zasieg`, `czas-trwania-t`, opcjonalnie `maks-poziom`; plugin raycastem wybiera Target Actor. Oburącz pułap poziomu to `floor(maks-poziom × oburacz.efekt)`, czas bez zmian. Skill `Wystrzal_P/L/Obie` dostaje tylko wybrany cel; dla wrogiego efektu użyj `mmoCanTarget{interaction=OFFENSE_ACTION}`. Brak celu lub cel za ścianą nie pobiera many.
12. Dodaj kafelki do `DeluxeMenus/gui_menus/ksiega_zaklec.yml` i `ulubione_zaklec.yml`: przypisanie prawa/lewa, ulubione, koszt, typ i sterowanie. Zachowaj tytuł z glifem.
13. Po wdrożeniu sprawdź przez Velocity oba warianty dłoni, oburącz, brak many, przerwania, cele za ścianą, owner AI, PvP, Chowańce, logout/restart, HUD i logi backendu `world`.

## Sterowanie

| Typ | Prawa | Lewa | Oburącz |
|---|---|---|---|
| koncentracja / tarcza | LPM klik włącza/wyłącza | PPM trzymany | Trzymaj PPM i kliknij LPM |
| ladowany / runa / bron / przywolanie / na_cel | LPM klik: ładuje i strzela | PPM: ładuj i puść | Trzymaj PPM i kliknij LPM; puść PPM po gotowości |

PPM — lewa dłoń (trzymaj) · LPM — prawa dłoń (klik włącza/wyłącza) · PPM + klik LPM — obie · zmiana zaklęć: Shift+Q. Dla ładowanych: PPM: ładuj i puść · LPM: klik — ładuje i strzela. PPM ma pakiet co 4 ticki i tolerancję `ppm-tolerancja`. LPM nie ma pakietu puszczenia. Zmiana slotu, śmierć, wyjście i pasek Wybór przerywają przygotowanie. Aktywna runa znika po czasie, wyjściu lub restarcie.
