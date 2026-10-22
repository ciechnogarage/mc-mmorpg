# Runda 2 — grafika HUD (YAML z rundy 1 zostaje, poprawiasz wygląd)

Ocena rundy 1 (Claude, w powiększeniu): logika YAML dobra (warunki SPECTATOR/progress, stany HP/trucizna/stamina,
minimapa side:right, wyłączony pasek MMOCore — zostaw). GRAFIKA za słaba na „zajebisty hudzik”: to zaślepki.
1. Paski hp/mana/stamina/exp = JEDEN kolor, płaski prostokąt. Wzór Volya (`inspiracje/w1.jpg`, zrób crop x4 i policz):
   pasek ma wnękę z ciemnym konturem i wypełnienie w 3 tonach — jasny górny rząd (połysk), ton bazowy, ciemniejszy dolny rząd.
2. `karta_tlo.png` = prostokąt z 5 barwami i gołymi kwadracikami w rogach. Brak okuć, brak charakteru Księgi.
3. `portret_alarm.png` = gruby czerwony kwadrat 64x64 — tani. Zrób cienką czerwoną poświatę/obwódkę ramki portretu (1–2 px GUI).
4. `medalion.png` = czarna elipsa w żółtej obwódce. Ma być OKRĄGŁY medalion poziomu jak medaliony w Księdze (`ref/kb_*.png`),
   nachodzący na prawy-dolny róg portretu.
5. Brak ikon przy paskach — nie wiadomo, który pasek jest który (mana vs stamina).
6. Makiety rysowane czcionką PIL (`ImageFont.load_default`) — rozmyte, nie do oceny. Używaj bitmapowej czcionki MC:
   `tools/hud/mcfont/assets/minecraft/font/include/default.json` + `textures/font/ascii.png` + `accented.png`
   (mapowanie znaków z JSON, cień jak w grze / obrys dla outlined, skala całkowita NEAREST). Polskie znaki muszą wyjść.
7. Dół: etykiety tekstowe „BROŃ / MIKSTURY / UŻYTEK / MAPA · Q” nad hotbarem + nazwy zaklęć wyżej = śmietnik tekstu.
   W makiecie w slotach są litery „I II HP MP U V VI M D” — w slotach leżą przedmioty, NIC tam nie rysuj (w makiecie wstaw
   przykładowe ikony przedmiotów: miecz, mikstura, mapa, księga — z client textures albo proste).
   Skrzydła „ZBROJA”/„GŁÓD” = puste brązowe pudła z napisem — wyglądają jak placeholdery.

## Kierunek wizualny (dopasuj do naszych rzeczy)
Obejrzyj `ref/kb_1..4.png` (Księga Wędrowca, menu Q — złożone 2x2, zaakceptowana przez właściciela) i `ref/karta_postaci_bg.png`.
Zmierz ich paletę (brązy skóry/drewna, złote okucia, kremowy pergamin, granatowe tabliczki tytułu) i UŻYJ tych samych barw.
Możesz najpierw wygenerować 1–2 koncepty karty generatorem obrazów (jeśli masz image_generation) jako szkic kompozycji —
ale tekstury rysujesz pikselowo, ręcznie/skryptem, w siatce.
- **Karta Wędrowca** (~124x54 GUI, 2x, ≤256 px; możesz podzielić na 2 tekstury):
  - tło: ciemna skóra 2–3 tony + JEDEN cienki ciemny kontur, delikatny jaśniejszy rant u góry (bevel 1 px), organiczna krawędź
    (ścięte rogi/karby), w rogach małe złote okucia w kształcie L z nitem — jak narożniki Księgi;
  - ramka portretu: złota oprawa 2 px GUI z cieniem wewnętrznym, ciemne tło pod twarzą; twarz `head` dokładnie w środku
    (policz: head size 4 = ile px GUI; wyrównaj co do piksela, podaj liczby w raporcie);
  - tabliczka z nickiem: granatowa plakietka ze złotym rantem jak tytuł „KSIĘGA WĘDROWCA” albo pergaminowa wstęga;
    nick kremowy, klasa przyciemniona — NIE może dotykać paska HP; sprawdź długi nick (16 znaków) i długą klasę;
  - medalion poziomu: okrągły ~16x16 GUI, złoty rant, ciemny środek, liczba wycentrowana (sprawdź 1, 10, 100);
  - paski z wnęką i 3 tonami: HP czerwony (~8 GUI, liczba na pasku), mana niebieski (~4), STAMINA zielono-złota (~4),
    EXP cienki złoty (~2) jak „szew” na dole karty; końce pasków zaokrąglone o 1 px;
  - ikonki ~7x7 GUI przy pasku many i staminy (kropla, piorun/but), jeśli mieszczą się czysto.
- **Dolny pasek**: vanillowy układ slotów (182x22) zostaje; w `hotbar.png` grupy rozdzielone subtelnymi złotymi przegrodami/okuciami
  i lekko innym tonem tła grupy — sloty czytelne jak vanilla. Zamiast tekstowych etykiet: MAŁE piktogramy ~9x9 GUI na złotych
  zakładkach przy górnej krawędzi grupy (miecz | flakon | sakwa | mapa+księga) — ALBO nic. Pokaż oba w makiecie, polecaj jeden.
  Nazwy zaklęć lewej/prawej dłoni zostają nad paskiem (nad swoją stroną).
- **Zbroja/głód**: kompaktowo przy końcach hotbara: ikonka tarczy z liczbą zbroi po lewej, ikonka udźca z małym paskiem głodu
  po prawej (wnęka + 3 tony), bez napisów „ZBROJA/GŁÓD”.
- **Ramka minimapy** i tabliczka współrzędnych: ten sam styl (złote okucia L, ciemna skóra), tabliczka nie szersza niż mapa.
- Paleta całości ≤ ~14 barw wspólnych; zero szumu; jeden kontur.

## Makiety (od nowa, czcionką MC)
`tools/hud/makiety2/`: GUI 3 i 4, stany: pełne HP, niskie HP + trucizna, pusta stamina, walka Chowańców, noc (przyciemniony zrzut),
oba warianty dołu. Plus `karta_x4.png` (sama karta, skala 4) i `dol_x4.png`. Generator `makiety2_gen.py`.
Jeśli PIL niedostępny w sandboksie — napisz skrypt, Claude uruchomi go w obrazie z PIL.
Samokontrola: obejrzyj swoje x4 i wypisz w raporcie 3 rzeczy poprawione po własnym oglądzie.

## Zasady jak w rundzie 1
Edytujesz tylko `tools/hud/` (praca/, makiety2/, generator, README, wdroz.sh jeśli zmienią się pliki). Nie ruszasz plugins/,
nie restartujesz, nie używasz dockera. Zaktualizuj YAML (offsety, nowe warstwy) zgodnie z teksturami.

## Raport
`/home/dev/Desktop/Przemek/MC/tools/hud/RAPORT-2.md`, max 40 linii: co zmienione, liczby wyrównania głowy/medalionu,
paleta (hexy), 3 samopoprawki, niepewne do sprawdzenia w kliencie. Bez diffów.
