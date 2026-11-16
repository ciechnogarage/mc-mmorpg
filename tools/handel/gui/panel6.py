"""Panel 6-rzedowy (176x222) dla okien handlu — pochodna tla menu-huba (menu_hub_bg.png, 176x168).
Geometria GUI skrzyni (piksele GUI): sloty kontenera od y=18 (ramka komorki y=17), wiersz = 18 px.
3 rzedy: gracz od y=84 (ramka 83). 6 rzedow: wszystko ponizej kontenera przesuwa sie o 54 px.
  nowy[0:35]    = hub[0:35]      naglowek z tytulem + latarnia + fryz; fryz lezy pod rzedem 0
                                  kontenera, wiec rzad 0 to "polka" na zakladki kategorii
  nowy[35:125]  = 5 x hub[83:101] pas komorek rzedow 1-5 (ten sam rysunek co plecak w hubie)
  nowy[125:126] = hub[101:102]   dolna krawedz ostatniego rzedu
  nowy[126:222] = hub[72:168]    etykieta + plecak + pasek (przesuniete o 54)
Nic nie jest rysowane od nowa — kolory i swiatlo zostaja z huba, wiec okna sa z tej samej rodziny."""
from PIL import Image
import sys
src = Image.open(sys.argv[1]).convert('RGBA'); assert src.size == (176, 168), src.size
out = Image.new('RGBA', (176, 222))
out.paste(src.crop((0, 0, 176, 35)), (0, 0))
row = src.crop((0, 83, 176, 101))
for k in range(5):
    out.paste(row, (0, 35 + 18 * k))
out.paste(src.crop((0, 101, 176, 102)), (0, 125))
out.paste(src.crop((0, 72, 176, 168)), (0, 126))
out.save(sys.argv[2])
big = out.resize((176 * 3, 222 * 3), Image.NEAREST); big.save(sys.argv[2].replace('.png', '-x3.png'))
print('ok', out.size)
