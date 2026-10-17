# Otwarta Arena — runda 3: widoczny kafel w menu „Drużyna” (grafika tła)

Problem (zrzut z klienta: /home/dev/.claude/jobs/87643610/tmp/qa/captures/010.png i 009z.png): w podmenu Q → DRUŻYNA
(DeluxeMenus `hub_spolecznosc`) kafel Otwartej Areny na slotach 41, 42, 50, 51 działa i ma podpowiedź, ale jest
NIEWIDOCZNY. W tym menu wszystkie kafle są narysowane w tle okna (glif Nexo w tytule `&f䨌䨍䨎䨏`), a przedmioty to
przezroczyste `nexo-mmo_icon_blank`. Trzeba DORYSOWAĆ kafel areny w tle.

Wejście (kopie z serwera, tylko do odczytu w oryginale):
- `wdrozenie/pack/gui/zrodla/hub_spolecznosc_{1,2,3,4}.png` — cztery pasy tła 176×222 (glify `hub_spolecznosc_1..4`
  w `/opt/docker/mc/data/world1/plugins/Nexo/glyphs/ksiega_podstrony.yml`, height 111, ascent 13).
- Inne tła tego samego stylu do podpatrzenia liter i ramek: `/opt/docker/mc/data/world1/plugins/Nexo/pack/assets/nexo/textures/gui/hub_*.png`
  (np. litera „E” jest w podpisach innych podmenu — weź piksele z istniejącego podpisu, nie rysuj nowej czcionki).
- Układ slotów: okno skrzyni 9×6. Istniejące kafle 2×2 sloty: DRUŻYNA = 5,6,14,15; ZNAJOMI = 7,8,16,17; GILDIA = 24,25,33,34.
  Kafel areny = 41,42,50,51 (wiersze 4–5, kolumny 5–6). Najpierw ustal, jak 4 pasy składają się w obraz okna
  (porównaj z zrzutem 009z.png) i gdzie w pikselach wypadają sloty — wyprowadź to z położenia istniejących kafli.

Zrób:
1. Kafel w tym samym stylu co GILDIA/DRUŻYNA: ramka z obrazkiem (ciemnogranatowe pole, złota ramka) + kremowa
   tabliczka z podpisem `ARENA` tą samą pikselową czcionką. Ikona: dwa skrzyżowane miecze nad tarczą albo dwa miecze
   w ringu — prosta, czytelna w 16–20 px, paleta z istniejących kafli (zasada: ~13 barw, jeden kontur, płaskie tony,
   bez szumu i bez czerni — patrz kafel GILDIA). Kafel ma pokrywać dokładnie obszar slotów 41,42,50,51 tak jak inne kafle
   pokrywają swoje sloty (ten sam rozmiar i odstępy co DRUŻYNA nad nim).
2. Zapisz wynik do `wdrozenie/pack/gui/hub_spolecznosc_{1,2,3,4}.png` (te same wymiary 176×222; zmieniaj tylko piksele kafla).
3. Podgląd: `wdrozenie/pack/gui/podglad.png` — złożone tło okna w skali 4×, plus wycinek kafla w skali 8×.
4. Skrypt, którym to zrobiłeś (Python + Pillow, jeśli Pillow brak — czysty Python z zlib/struct): `wdrozenie/pack/gui/rysuj_kafel.py`.

Nie zmieniaj niczego poza tymi plikami. Nie wdrażasz. Raport `RAPORT-3.md` max 15 linii: mapowanie slot→piksele,
skąd wzięte litery, co narysowane.
