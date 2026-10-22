# Karta stylu grafik (wersja 0 — do zatwierdzenia)

Źródła reguł: Blockbench Minecraft Style Guide (https://blockbench.net/wiki/guides/minecraft-style-guide/),
wanilia jako wzorzec techniczny, pack Wynncrafta jako zewnętrzna poprzeczka jakości (do nauki, nie kopiowania).
Nasze dotychczasowe grafiki NIE są wzorcem.

## Rozmiar i przeznaczenie (ustalić PRZED rysowaniem)
- Przedmiot (eq, ręka, świat): 16×16, gęstość wanilii. Zakaz mixeli.
- Ikona menu: 32×32 — DO POTWIERDZENIA (Wynncraft robi ikony GUI w 16×16; nasze "32×32" huba to 16 powiększone 2×).
- Każdy rozmiar rysowany natywnie, nigdy skalowany.
- Zajętość slotu jak w wanilii: przedmiot ~12–15 px, margines min. 1 px.

## Światło i forma
- Światło z lewej góry. Góra i lewa jaśniej, dół i prawa ciemniej.
- Linia cienia na bryle obrotowej = łuk (stromo u góry, płasko u dołu), nie prosta 45°.
- Ciemna masa po stronie cienia jako płaszczyzna, nie wąski pasek.

## Kolor
- Rampa materiału: ton środkowy + 1 cień + 1 światło (+ ew. blik). Maks. ~4 tony na materiał.
- Przesunięcie odcienia między tonami (cień chłodniej/ku fioletowi, światło cieplej), równe kroki.
- Obok materiału nasyconego potrzebny materiał neutralny (odpoczynek oka).

## Obrys
- Wyraźnie ciemniejszy obrys (dokumentacja). WYBRANO (2026-09-23, kierunek B): prawie czarny obrys, dwa tony
  (strona światła (46,42,52), strona cienia (24,20,28)).
- Szkło: jasne, neutralne, chłodnoszare (e (160,174,178), cień (110,124,132), blik (236,240,240)).
- Płyn: czerwień chłodniejsza i ciężka (B), nie jaskrawa/ciepła (C odrzucone): światło (224,48,56), środek (160,22,40), cień (96,12,40).

## Zakazy (dokumentacja)
- Wygładzanie krawędzi (AA), schodki tonów (banding), cieniowanie poduszkowe/naleśnikowe, szum, zbędny dithering.
- Pojedyncze piksele bez funkcji. Mechaniczne powtórzenia (kopiowanie modułów pętlą).

## Kontrola przed pokazaniem
- Sylwetka zalana czarnym, szarość, slot 1× i w skali GUI, obok wanilia i referencja zewnętrzna.
- Pisemna krytyka: min. 3 nazwane wady + werdykt słabe / średnie / dobre. "Słabe" nie wychodzi.
- Test w prawdziwym kliencie przed akceptacją.

## Dziennik kalibracji (mój werdykt vs właściciel)
- 2026-09-23 potka kierunek: poleciłem B + jaskrawszą czerwień z C → właściciel wybrał czyste B.
  Wniosek: preferuje cięższą, chłodniejszą czerwień; nie podbijać nasycenia/ciepła "dla efektu".
- 2026-09-23 potka render: odrzuciłem r5b (światło bardziej z przodu, jasny płyn) jako "plamę" → właściciel wybrał
  r5b. Wniosek: jasna, czytelna masa koloru w świetle jest OK; nie przyciemniać na siłę "dla ciemnej masy".
  Metoda przyjęta: render 3D (tools/pixelart/render3d.py) + przejście na piksele (output/warsztat/render/piksele.py),
  światło (-0.9, 1.0, 1.2), pitch 14.
