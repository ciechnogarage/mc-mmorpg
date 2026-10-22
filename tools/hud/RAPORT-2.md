# Runda 2 — poprawa grafiki HUD

- Wykonano nowe tekstury karty, pasków, medalionu, alarmu, ikon zasobów, skrzydeł zbroi/głodu, hotbara, ramki mapy i współrzędnych. Wszystkie są pikselowe, bez szumu.
- Karta ma 142×66 px GUI; tło jest podzielone na 124+18 GUI (PNG 248×132 i 36×132), bo pojedyncza tekstura 2× przekroczyłaby limit 256 px.
- Głowa `size: 4` ma 8×4 = 32 px GUI; jej lewy górny róg to `(6,24)` w karcie, środek `(22,40)`. Oprawa zajmuje `(3,21)`–`(40,58)`, więc głowa jest wyśrodkowana we wnętrzu 32×32.
- Medalion jest okrągły 20×20 GUI w `(24,45)`; środek `(34,55)`, numer kotwiczy się w x=34. Poziomy 1, 10 i 100 pokazuje `makiety2/karta_poziomy_x4.png`.
- Plakietka nicku jest granatowa, 16-znakowy test ma 92 px MC. Najdłuższa obecna nazwa klasy z plików MMOCore (`Wołający Przodków`) ma 98 px i mieści się przy x=43, bez kontaktu z HP.
- HP ma wnękę i 8 px wysokości, mana/stamina 4 px, EXP 2 px. Paski mają górny połysk, środkowy ton i ciemniejszy dolny rząd; HP dla trucizny i uschnięcia ma osobne tekstury. Kropla i błysk odróżniają manę od staminy.
- Hotbar 182×22 px zachowuje dziewięć slotów i trzy subtelne przegrody. Wariant A ma zakładki z piktogramami, B jest bez zakładek; **polecam B** i ten wariant jest w YAML. Makiety mają wyłącznie przykładowe przedmioty, bez liter w slotach.
- Zbroja: tarcza + liczba z wbudowanego listenera `armor` przez niewidoczną warstwę `zbroja-dane`; głód: udziec + trójtonowy pasek. Nie ma etykiet grup ani „ZBROJA/GŁÓD”. Nazwy zaklęć zostają.
- Ramka minimapy i tabliczka 64×14 GUI używają tych samych okuć. Pozycja współrzędnych w YAML: y=-108. Chowaniec przesunięty na -84/-134, z 10 GUI odstępu od karty.
- Paleta 14 barw (unia nieprzezroczystych pikseli): #251C2B #39232E #684338 #8C532C #8A6A30 #D5A96B #FFE7AE #FFF1CC #183650 #286287 #3F98CB #CE4042 #F36D62 #61964A.
- `makiety2_gen.py` generuje 23 PNG w `makiety2/`: GUI 3/4, oba doły, pełne HP, niskie HP+trucizna, pusta stamina, walka Chowańców, noc oraz karta/dół ×4 i poziomy ×4. Font jest czytany z MC `default.json`, `ascii.png`, `accented.png`; polskie znaki i cień są bitmapowe, skala NEAREST.

## Samokontrola po obejrzeniu ×4

1. Zwiększyłem wysokość karty i rozsunąłem klasę od HP, bo polskie glify dotykały wnęki.
2. Powiększyłem i przesunąłem medalion po próbie poziomu 100, który dotykał ikony many.
3. Rozsunąłem nazwy zaklęć i zmniejszyłem piktogramy zakładek z 14 do 9 GUI; wariant B zostawiłem czystszy.

## Sprawdzenie i ryzyka klienta

- Generator uruchomiony; 23 makiety obejrzane w zestawieniu, karta/dół i próba 1/10/100 osobno. Kompilacja Python i `bash -n wdroz.sh` poprawne. 19 PNG w paczce, żaden wymiar >256; unia barw = 14.
- W kliencie trzeba potwierdzić wyrównanie fontu MythicHUD i aktualizację głowy po `/cmi skin`, odczyt `%zbroja-dane-value%`, poziomy 1/10/100, zaznaczenie slotu oraz zachowanie SPECTATOR.
- NMinimap używa pikseli fizycznych, a MythicHUD GUI: odstęp tabliczki współrzędnych od mapy szczególnie przy GUI 4 oraz długie/ujemne współrzędne wymagają testu i ewentualnego przesunięcia.
- Pliki produkcyjne i restart nie były ruszane. `wdroz.sh` pozostał przygotowany do uruchomienia przez operatora.
