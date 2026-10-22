# Komunikaty — etap 1

## Zmiany

- Zaklęcia — `Rzucanie.java:wcisnietoPpm,uzycie,lewaTeraz,tick`: zatrzask lewej dłoni po niedoborze many aż do przerwy PPM dłuższej niż `ppm-tolerancja`; kontrola kosztu od razu po dołączeniu dłoni. `tick` nie pobiera many za leczenie innych bez sojusznika i pokazuje powód.
- Zaklęcia — `RzutyOdrzutowe.java:zacznij,sprawdzMane,wystrzel`: pojedynczy sygnał aktualnego niedoboru przy starcie ładunku, ponowna kontrola i potrącenie dopiero przy wystrzale; jawne powody odmowy przed kosztem. Usunięto bezwarunkowy log każdego strzału.
- Zaklęcia — `EfektyCzasowe.java:przygotuj,konfiguracja`: wynik `Odmowa` zamiast `boolean`, ostrzeżenie o brakującym zasobie konfiguracji najwyżej raz na ID.
- Zaklęcia — `Skille.java:jest`: ponowna kontrola rejestru MythicMobs co 30 s i jedno ostrzeżenie o brakującym `Zak_BrakMany`.
- Zaklęcia — `Komunikat.java:mana,odmowa,pokaz`: pasek przez `PlayerData.displayActionBar`, kolory #F2F2ED/#E87973/#78B9EF, dedup `(gracz,kod)` 2 s i pojedynczy dźwięk. Brak many: „Brakuje many”.
- Kopia `_wspolne.yml:Zak_BrakMany`: usunięto title i drugi dźwięk. Tekst daje plugin, skill daje jeden dźwięk. Pliku na world1 nie zmieniano.
- Mapa — `Prowadzenie.java:toggle,tick`: chwilowy tekst przez `MMOCoreMost`; prowadzenie przy zmianie kierunku lub progu 5 m nie częściej niż co 20 ticków, przypomnienie co 50 ticków.
- Mapa — `ZnacznikDialog.java` i `MapaPlugin.java`: chwilowe teksty przez `MMOCoreMost.actionBar`.
- Animacje Broni — `AnimacjeBroni.java:puszczeniePpm,ruchMocnego`: „Brakuje staminy” i dźwięk najwyżej raz na 40 ticków; świeżej odmowy nie nadpisuje tekst kombo.

## Commity, bez push

- `kom-zaklecia`: `7bb6d42a4c5569e24a303f7649f77a581b4d52f0`
- `kom-mapa`: `dfe2cf1e3b5300a4fb163007005b28759c634851`
- `kom-ab`: `1f01e57fba51d7abf28d894e966674913bc7fa3a`

## Testy dla Claude na klonie

1. Zbudować trzy pluginy na klonie i wdrożyć z kopią `_wspolne.yml` na izolowany backend; wejście gracza wyłącznie przez Velocity. Sprawdzić log startu i brak błędów API.
2. Zaklęcia, 0 many: koncentracja/tarcza w lewej, PPM 5 s = jeden tekst i jeden dźwięk, 0 kosztu; puścić PPM ponad tolerancję, ponowić = nowa próba. Powtórzyć z pustą prawą dłonią.
3. Prawa koncentracja działa; dołączyć lewą przy manie poniżej wspólnego kosztu i `n` poza cyklem 10 ticków: odmowa w tym samym ticku dołączenia, bez potrącenia. Sprawdzić PPM+LPM i brak zmiany sterowania Shift+Q.
4. Ładunek lewy PPM, prawy LPM i oburącz przy 0 many: komunikat przy starcie, także przy puszczeniu przed gotowością; przy wystrzale ponowna kontrola i 0 kosztu. Z maną uzupełnioną w trakcie ładowania wystrzał ma się udać.
5. Runa bez górnej powierzchni i bez miejsca: „Wskaż cel” / „Brak miejsca na runę”; brak wroga, sojusznika i miejsca przywołania: właściwe powody, 0 kosztu. Błędny ID moba/przedmiotu: jeden log na ID.
6. Leczenie innych w koncentracji bez sojusznika przez 5 s: „Wskaż sojusznika” z dedupem, 0 kosztu; po wskazaniu sojusznika leczenie i koszt wracają.
7. Usunąć `Zak_BrakMany` na kopii staging i kilka razy spróbować rzucić przy 0 many: dokładnie jedno ostrzeżenie w logu; po przywróceniu/reloadzie i do 30 s skill dźwiękowy znów działa.
8. Mapa: włączyć prowadzenie i równocześnie wywołać błąd zaklęcia; nieruchomo tekst nawigacji najwyżej co 2,5 s, zmiana kierunku/5 m nie częściej niż co 1 s. Przetestować dodanie/edycję/usunięcie znacznika i przybliżenie.
9. Animacje Broni: 0 staminy, powtarzane mocne ciosy w ciągu 2 s = jeden tekst i dźwięk; świeży błąd nie znika pod „Kombo”. Po 2 s nowa odmowa. Sprawdzić normalny cios z pełną staminą.

## Walidacja i ryzyka

- `git diff --check` przed commitami bez błędów; wszystkie zmienione pliki odpowiadają blobom commitów. Kopia YAML ma jeden `sound` i zero `sendtitle`. Lokalnie brak `javac`, `gradle` i parsera YAML; build oraz test klienta nie były możliwe. Nie użyto Docker, reloadu, restartu ani RCON.
- Katalogi `.git/worktrees/kom-*` są zamontowane tylko do odczytu. Commity utworzono tymczasowymi indeksami i przesunięto referencje gałęzi; zwykły `git status` pokazuje pozorny diff ze starym indeksem. Na klonie z zapisywalnymi metadanymi wykonać `git reset --mixed HEAD` w każdym worktree przed dalszą pracą.
- Kopię YAML trzeba wdrożyć razem z JAR Zaklęć; stary aktywny skill nadal ma title i dwa dźwięki. Widoczność oraz kolejność paska wobec MMOCore i innych pluginów wymaga nagrania/testu klienta.
