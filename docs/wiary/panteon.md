# Wiary — panteon i łaska patrona (v1, 2026-09-30)

Projekt i decyzje: strona https://claude.ai/artifact/JD9X7Mxv6dBTXN3oNjBehR (wersja 3). Imiona przyjęte „na razie” (29.09).
Loch 001 NIE jest powiązany z wiarami, dopóki właściciel nie potwierdzi jego lore.
Kopia na serwerze: /home/dev/Desktop/Przemek/MC/docs/wiary/panteon.md (kopiowana przy wdrożeniu).

## Panteon
| Grupa | Bóstwo | Przydomek | Domena |
|---|---|---|---|
| Trzej Starsi (wszystkie rasy) | Ordhun | Pierwszy Korzeń | ziemia, rośliny, zwierzęta |
| | Nysara | Tkaczka Losu | los, przysięgi, podróże |
| | Vharun | Ten, Który Milczy | śmierć, sąd, spokój po życiu |
| Ludzie (Przymierze Latarni i Księżyca) | Solenna | Pani Latarni | światło, ochrona dróg |
| | Valdris | Kamienny Sędzia | prawo, sprawiedliwość, tarcza |
| | Heldra | Matka Żniw | urodzaj, dom, rzemiosło |
| Elfy (Przymierze) | Aelyra | Pani Srebrnego Miesiąca | księżyc, duchy przodków, przepowiednie |
| | Sylvaen | Szept Koron | wiatr, łowy, ścieżki lasu |
| | Iridel | Czytająca Gwiazdy | magia, gwiazdy, wiedza |
| Orki (Pakt Krwi i Kości) | Gorrath | Pierwszy Wódz | przodkowie, wojna, honor rodu |
| | Kharzuk | Burzowy Byk | burza, piorun, szał bitewny |
| | Mogra | Matka Kłów | łowy, bestie, przetrwanie |
| Nieumarli (Pakt) | Morvhena | Pani, Która Podniosła | nieśmiertelność, powrót zza progu |
| | Vhorax | Wieczny Głód | dusze, głód, siła brana od innych |
| | Ossyra | Pasterka Umarłych | pamięć, żałoba, opieka nad zmarłymi |

Spoiwo frakcji: Solenna i Aelyra to siostry (dzień i noc) -> Przymierze. Orki czczą przodków, nieumarli to przodkowie,
którzy wrócili; Gorrath i Morvhena zawarli pakt przed Nysarą -> Pakt. Vharun jest wrogiem wszystkich nieumarłych.

## Zasady (przyjęte D2, D3)
- Patron: 1 z 6 — 3 Starszych + 3 bóstwa rasy postaci. Wybór per POSTAĆ (nie konto). Pierwszy wybór darmowy,
  każda zmiana po 24 h i zdejmuje łaskę.
- Łaska: modlitwa z menu daje 30 min statystyk; siła = baza + na-punkt × Wiara (atrybut MMOCore `wiara`, limit 40),
  liczona w chwili modlitwy. Nowa modlitwa dopiero po wygaśnięciu.
- D4 (warianty Zastępu/Naczynia Akolity wg patrona) — „możliwe”, wrócić przy umiejętnościach Akolity.

## Łaski v1 (wartości: src/main/resources/config.yml -> plugins/Wiary/config.yml)
Tylko zwykłe statystyki MythicLib — opis nie obiecuje efektów warunkowych (noc, zabicie elity), bo ich jeszcze nie ma.
Ordhun odporność na odrzut + wytrzymałość pancerza · Nysara doświadczenie postaci · Vharun szansa na kryt ·
Solenna obrażenia vs nieumarli · Valdris pancerz · Heldra regeneracja zdrowia (%) · Aelyra obrażenia magiczne ·
Sylvaen prędkość ruchu (%) · Iridel skrócenie odnowienia · Gorrath max zdrowie (%) · Kharzuk siła krytyka ·
Mogra obrażenia vs potwory · Morvhena kradzież życia · Vhorax wampiryzm czarów · Ossyra ochrona przed potworami.
Aelyra celowo NIE ma regeneracji zasobu: u wojownika zasób (Szał) sam opada, procent przyspieszałby spadek.

## Technika
- Moduł danych MMOProfiles — plik plugins/Wiary/profile/<uuid-postaci>.yml; rasa z folderu klasy MMOCore
  (plugins/MMOCore/classes/<rasa>/). Profil-duch (id == UUID gracza) nie dostaje danych.
- Menu: DeluxeMenus `wiara` (menu/generuj_menu.py), wejście hub Q -> Postać -> Wiara (slot 14), ikona Nexo `hub_wiara`
  (glif w world1 tools/menu-hub-gui/generate_icons2.py). Instalacja na serwer: menu/instaluj.py (idempotentna).
- Komendy `/wiary wybierz|modlitwa|info|reset|reload` — tylko konsola / OP (wiary.admin); menu woła je przez [console].
- Build: ./build.sh (gradle w dockerze, 7 testów). Eval: K=<klon> eval/run-klon.sh (bot bez OP przez menu).
