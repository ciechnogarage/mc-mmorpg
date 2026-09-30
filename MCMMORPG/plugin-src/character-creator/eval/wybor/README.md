# Evale wyboru postaci (lista, zarzadzanie, kosz)

Bot mineflayer wchodzi na **klon** world1 jak gracz (bez OP, `rank.mvp` = 12 miejsc). Klawisze sceny
ida pakietem `player_input`, LPM = atak w encje sceny, PPM = interakcja, okna Papera = `show_dialog`
+ recznie skladany `custom_click_action` z wartosciami pol (checkboxy, tekst). Konsola klonu tylko do
obserwacji (`papi parse`, pliki danych) i do "menu Q" — DeluxeMenus wola dokladnie
`cc wybor <gracz> lista` z konsoli.

| plik | co |
|---|---|
| `setup.js` | 11 postaci drogą gracza (scena -> lista -> "+ Nowa postac" -> kreator -> ksiega), poziomy i ekwipunek konsola |
| `scen.js` | 55 sprawdzen: lista po wejsciu, stala kolejnosc po relogu, gwiazdka, karta (poziom/miejsce/ostatnio/czas), klik = graj, A/D, spacja, menu Q bez sceny, zarzadzanie 3 naraz, imie od poz. 10, zle imie, kosz: przywroc/usun na zawsze, nowy gracz |
| `termin.js` | kosz po terminie (trash.yml cofniety o 8 dni przy zatrzymanym serwerze) -> postac znika przy wejsciu |
| `op.js` | ta sama sciezka kontem z OP |
| `kosz_pusty.js` | wszystkie postacie w koszu (konto KoszQA, 2 postacie: `QA=KoszQA NAMES=Borvik,Ysmera ./run.sh setup.js`) |
| `reload.js` | `/cc reload` przeladowuje wybor-postaci (uruchamiac `run-rw.sh` — zmienia config klonu) |
| `czat.js` | wypisuje caly czat przy wejsciu i zmianie postaci — szukanie angielskich komunikatow |
| `reset.js` | stan startowy: kosz pusty; kod 3 = brak Mortha (dorob: `OD=3 DO=4 ./run.sh setup.js`) |

Klon: `klon-start.sh` (sciezki joba 2f21747c — przepisz), `test-wejscie` niepotrzebne. Wymagane w klonie:
velocity wylaczone, `rcon.password=wybor`, jar z tej galezi, `pliki/apply_config.py` z pakietu wdrozenia.

**Pulapki harnessu** (kazda kosztowala przebieg): mineflayer NIE emituje `actionBar` dla paska w formie
NBT — `lib/w.js` czyta pakiet `action_bar` sam; `logSince` tnie BUFOR (przesuniecie w bajtach, polskie
znaki); po ponownym otwarciu sceny w `bot.entities` wisi stara tarcza — bierz najnowsza (najwyzsze id);
okno z poprzedniej sesji zostaje w `bot.dialog` — zeruj przed PPM; Paper ma throttling polaczen (4 s).

**Okno startera Chowancow** (inny plugin) przychodzi kilka sekund po wybraniu postaci bez chowanca i
ZASTEPUJE otwarte wtedy okno (np. liste z menu Q) — `op.js` najpierw je zamyka ("Pozniej").

## Po scaleniu (cc-naprawa, 30.09)

`pelny.sh` — cala regresja na klonie (sciezki joba 2f21747c — przepisz): jar z build/libs (JEDEN), evale WYBOR
i KREATORA (eval/scena), na koncu termin.js z cofnieciem daty w koszu i restartem. Dane testowe: Elwen (poz. 15)
i Morth sa odtwarzane droga gracza, jesli ich brak. `spacja.js`: Spacja raz w kreatorze i w wyborze + LPM bez
komunikatu o bieglosci. Dzwiek wyboru profilu = sound_effect id 1256 (mineflayer zle go nazywa).

## 2026-10-01: kosz usuniety
Wlasciciel: "jak ktos je usuwa, to je usuwa" — usuwanie jest od razu na zawsze (potwierdzenie + imie od poz. 10).
`kosz_pusty.js` i `kosz_prep.js` usuniete. `bez_kosza.js` (konto LimitQA z 5 postaciami: `QA=LimitQA NAMES=Lima,Limb,Limc,Limd,Lime run.sh setup.js`)
sprawdza: limit pod A/D, Zarzadzaj z licznikiem, usuniecie na zawsze bez slowa "kosz", odswiezenie sceny po usunieciu
wszystkich, pakiet kamery po spawnie encji-kamery. Wynik na klonie: 13/13. W `scen.js` czesc "Zarzadzaj -> kosz -> przywroc"
(od "potwierdzenie przeniesienia do kosza") jest NIEAKTUALNA — do przepisania przy nastepnej zmianie wyboru.
