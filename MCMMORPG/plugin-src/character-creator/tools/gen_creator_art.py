#!/usr/bin/env python3
"""
Generator grafiki UI kreatora postaci (styl fantasy, ciemny kamien + zloto).

Produkuje 8 obrazkow: po jednym na kazda RASE (lewa szyna) i kazda KLASE (prawa szyna).
Kazdy zawiera juz wypalone PODSWIETLENIE wybranej ikony i OPIS na pergaminie, wiec
kreator sklada tytul okna z dwoch glifow i nie musi nic liczyc w locie.

LIMIT KTOREGO NIE WOLNO PRZEKROCZYC
  PNG musi miescic sie w 256 x 256. Atlas fontow Minecrafta ma strone 256x256 i wieksze
  tekstury glifow sa po cichu odrzucane -> glif renderuje sie jako tofu (pusty kwadrat).
  Nexo loguje to jako "Texture ... is above allowed 256x256 resolution".
  Zweryfikowane na zywym kliencie: 528x504 dawalo tofu, 176x168 dziala.

GEOMETRIA (wyprowadzona i potwierdzona na kliencie, blad 1 px)
  panel SHULKER_BOX 9x3 = 176 x 168 px GUI; slot (col,row) -> (8 + col*18, 18 + row*18)
  tytul rysuje sie od (8, 6), baseline 7 px nizej

  Obrazek jest WIEKSZY niz panel w obu osiach (256x256 zamiast 176x168), wiec grafika
  wychodzi poza panel ze wszystkich stron:
      image_x = panel_x + 40      (shift -48 stawia image_x=0 na panel_x=-40)
      image_y = panel_y + 40      (ascent 53 stawia gorna krawedz glifu na panel_y=-40)

  Wyprowadzenie ascentu: glif rysuje sie gora na (baseline - ascent), baseline tytulu
  jest na panel y 13, wiec ascent 53 -> gora na panel y -40, dol na panel y 216.
  Poprzednio ascent 13 / height 168 trzymal grafike dokladnie w obrysie panelu; pasek
  gorny nie mial jak pojsc w gore, a dolny w dol.

  CZEGO NIE DA SIE PRZESUNAC
  - tekst tytulu (imie postaci) siedzi na stale na panel y 6..13 — vanilla rysuje go
    tam i zaden shift tego nie zmieni (font nexo:shift przesuwa tylko w poziomie).
    Dlatego pasek gorny moze urosnac w gore, ale musi nadal obejmowac te linie.
  - klikalne sloty istnieja tylko w obrysie panelu (panel x 8..170). Szyny boczne nie
    moga wiec "odkleic sie" od kolumn ikon i pojsc na krawedzie ekranu — moga tylko
    siegac grafika 40 px GUI na zewnatrz, co juz robia (to limit tekstury 256 px).

UKLAD (wspolrzedne OBRAZKA)
  y   2- 56 : pasek imienia, pelna szerokosc, wychodzi 38 px nad panel
  y  44-184 : szyny boczne — plaskie, wask(sz)e plyty pod kolumnami ikon
  y 176-228 : plinta przycisku STWORZ, wychodzi 20 px pod panel
  x  66-190 : BEZ GRAFIKI -> model widoczny na calej wysokosci ekranu
"""
from PIL import Image, ImageDraw
import os, random, re, sys

W, H = 256, 256
OFF = 40                     # image_x = panel_x + OFF
OFF_Y = 40                   # image_y = panel_y + OFF_Y  (ascent 53)
ASCENT = 53
# Szyny sa PLASKIE i waskie: tyle, ile trzeba pod kolumne ikon plus cienka obwodka.
# Wczesniej mialy 74 px, gruby gradient, podwojna ramke i pas runiczny — czytalo sie to
# jak dwa kamienne bloki zasypujace scene. Teraz 66 px i jedna zlota kreska.
RAIL_L = (0, 66)             # image x — dosuniete do krawedzi obrazka
RAIL_R = (190, 256)
ICON_ROWS = (18, 54, 85, 121)   # panel y; rozstawione, patrz komentarz w CreatorMenu
RAIL_TOP, RAIL_BOT = 48, 182    # image y
# Gora to DWA elementy, nie jedna plyta. Jedna plyta obejmujaca i tekst tytulu, i gorna
# krawedz dawala czarny prostokat na 1/5 ekranu — zmierzone na v6.png.
#   BANNER  — cienki sztandar wysoko, to on jest "podniesionym paskiem"
#   PLATE   — tabliczka imienia zwisajaca ze sztandaru; MUSI obejmowac tekst tytulu
#             (panel y 6..13 = image y 46..53) i slot imienia (image y 58..75)
BANNER = (0, 255, 24, 46)       # image x0, x1, y0, y1
PLATE  = (92, 164, 40, 80)
BOT_BAR = (92, 164, 172, 218)   # plinta STWORZ — waska, wychodzi 10 px GUI pod panel
# Sloty (wspolrzedne PANELU):
NAME_SLOT   = (80, 18)
CONFIRM     = (80, 143)

# UWAGA O JASNOSCI PALETY — wynik pomiaru, nie gust:
# klient przyciemnia SWIAT za ekranem kontenera o ok. 53%, ale NIE przyciemnia UI.
# Zmierzone: maksimum jasnosci sceny (zelazna zbroja w swietle 15) to 0.26, a pergamin
# w poprzedniej palecie mial mediane 0.24 — czyli interfejs dorownywal bohaterowi.
# Dlatego paleta UI musi byc CIEMNIEJSZA, niz podpowiada intuicja: inaczej panele zawsze
# wygraja z postacia i hierarchia bedzie odwrocona.
STONE_TOP = (32, 30, 40)
STONE_BOT = (13, 12, 17)
GOLD_DK   = (62, 45, 16)
GOLD_MD   = (104, 81, 33)
GOLD_LT   = (148, 126, 78)
SEL_GLOW  = (208, 184, 120)
PARCH_TOP = (48, 40, 28)
PARCH_BOT = (32, 26, 18)
INK       = (176, 160, 128)
BONUS_INK = (182, 158, 104)
BONUS_BAND= (62, 48, 30)
VOID      = (10, 9, 13)

# DejaVu, nie Liberation Narrow: przy 9 px i progowaniu do 1 bitu waskie litery
# Liberation zlewaly sie ("ambitni" wygladalo jak "arbitni"). Sprawdzone porownaniem
# renderow — DejaVu trzyma odstepy miedzy literami nawet po progowaniu.
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def px(panel_x):
    return panel_x + OFF


def py(panel_y):
    return panel_y + OFF_Y


def vgrad(d, box, top, bot, seed=0):
    x0, y0, x1, y1 = box
    h = max(1, y1 - y0)
    for y in range(y0, y1 + 1):
        t = (y - y0) / h
        d.line([(x0, y), (x1, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bot)))
    rnd = random.Random(seed)
    for _ in range((x1 - x0) * (y1 - y0) // 7):
        x, y = rnd.randint(x0, x1), rnd.randint(y0, y1)
        t = (y - y0) / h
        base = [int(a + (b - a) * t) for a, b in zip(top, bot)]
        k = rnd.choice((-10, -6, 6, 10))
        d.point((x, y), fill=tuple(max(0, min(255, v + k)) for v in base))


def gold_edge(d, box):
    x0, y0, x1, y1 = box
    d.rectangle(box, outline=GOLD_DK)
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], outline=GOLD_MD)
    d.line([(x0 + 1, y0 + 1), (x1 - 1, y0 + 1)], fill=GOLD_LT)
    d.line([(x0 + 1, y0 + 1), (x0 + 1, y1 - 1)], fill=GOLD_LT)
    for cx, cy in ((x0 + 3, y0 + 3), (x1 - 3, y0 + 3), (x0 + 3, y1 - 3), (x1 - 3, y1 - 3)):
        d.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=GOLD_MD, outline=GOLD_LT)


def socket(d, ix, iy, selected=False):
    """Wglebienie 18x18 pod ikone. selected = jasna zlota obwodka + rozjasnione tlo."""
    x0, y0, x1, y1 = ix, iy, ix + 17, iy + 17
    d.rectangle([x0, y0, x1, y1], fill=(26, 24, 20) if selected else VOID)
    d.line([(x0, y0), (x1, y0)], fill=(4, 4, 6))
    d.line([(x0, y0), (x0, y1)], fill=(4, 4, 6))
    d.rectangle([x0, y0, x1, y1], outline=SEL_GLOW if selected else GOLD_DK)
    if selected:
        # ramka wskaznika wychodzaca poza slot — czytelna nawet z ikona w srodku
        d.rectangle([x0 - 2, y0 - 2, x1 + 2, y1 + 2], outline=GOLD_MD)
        for cx, cy in ((x0 - 2, y0 - 2), (x1 + 2, y0 - 2), (x0 - 2, y1 + 2), (x1 + 2, y1 + 2)):
            d.point((cx, cy), fill=SEL_GLOW)


def slim_edge(d, box):
    """Jedna zlota kreska + delikatne swiatlo od gory. Bez podwojnej ramki i studow.

    Splaszczenie polega na obwodce, nie na kolorze: wersja z jednolitym, prawie czarnym
    wypelnieniem (v6) czytala sie jako dziura w ekranie. Gradient zostaje, znika bryla.
    """
    x0, y0, x1, y1 = box
    d.rectangle(box, outline=GOLD_DK)
    d.line([(x0 + 1, y0 + 1), (x1 - 1, y0 + 1)], fill=GOLD_MD)


def rail(img, x0, x1, icon_x, selected_index, seed):
    """Waska, plaska szyna z czterema ROZSTAWIONYMI slotami. Bez ornamentu i studow."""
    d = ImageDraw.Draw(img)
    box = [x0, RAIL_TOP, x1 - 1, RAIL_BOT]
    vgrad(d, box, STONE_TOP, STONE_BOT, seed)
    slim_edge(d, box)
    # Cienka zlota prowadnica po zewnetrznej stronie kolumny — tyle zdobienia wystarczy,
    # zeby szyna nie byla plama, i nie tyle, zeby wygladala jak bryla.
    guide = x0 + 3 if icon_x - x0 > x1 - (icon_x + 18) else x1 - 4
    d.line([(guide, RAIL_TOP + 6), (guide, RAIL_BOT - 6)], fill=GOLD_MD)
    for i, row in enumerate(ICON_ROWS):
        socket(d, icon_x, py(row), selected=(i == selected_index))


def plinth(img):
    """Plinta pod STWORZ — waska i zeszla 10 px GUI PONIZEJ panelu.

    Slot przycisku jest przypiety do rzedu hotbaru (panel y 143) i nizej zejsc nie moze,
    ale grafika juz tak: plinta konczy sie na panel y 178. Szeroka plyta z v6 miala pod
    przyciskiem sam pusty kamien, wiec jest waska — dokladnie na szerokosc tabliczki
    imienia u gory, zeby gora i dol sie rymowaly.
    """
    d = ImageDraw.Draw(img)
    x0, x1, y0, y1 = BOT_BAR
    vgrad(d, [x0, y0, x1, y1], STONE_TOP, STONE_BOT, 77)
    slim_edge(d, [x0, y0, x1, y1])
    socket(d, px(CONFIRM[0]), py(CONFIRM[1]))
    # Zwezajaca sie stopka: nosi ciezar w dol zamiast zostawiac pusty prostokat.
    for k, (inset, col) in enumerate(((6, GOLD_MD), (14, GOLD_DK))):
        yy = y1 - 5 + k * 3
        d.rectangle([x0 + inset, yy, x1 - inset, yy + 1], fill=col)


def build_race(index):
    """Lewa szyna + pasek imienia u gory + plinta STWORZ (rysowane raz, tutaj)."""
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rail(img, RAIL_L[0], RAIL_L[1], px(8), index, seed=11)
    plinth(img)
    d = ImageDraw.Draw(img)
    # SZTANDAR — cienki pas wysoko nad panelem. To jest "podniesiony pasek gorny".
    bx0, bx1, by0, by1 = BANNER
    vgrad(d, [bx0, by0, bx1, by1], STONE_TOP, STONE_BOT, 99)
    slim_edge(d, [bx0, by0, bx1, by1])
    # Sztandar bez zdobienia byl pustym prostokatem na 1/5 szerokosci ekranu (v7.png).
    # Podwojna kreska + romby co 24 px daja mu fakture, a nadal jest cienki (22 px).
    mid = (by0 + by1) // 2
    for yy in (by1 - 4, by1 - 3):
        d.line([(bx0 + 6, yy), (bx1 - 6, yy)], fill=GOLD_DK)
    for x in range(bx0 + 14, bx1 - 12, 24):
        d.polygon([(x, mid - 3), (x + 3, mid), (x, mid + 3), (x - 3, mid)], outline=GOLD_MD)
    # TABLICZKA IMIENIA — zwisa ze sztandaru. Tekstu tytulu NIE da sie przesunac w pionie
    # (font nexo:shift dziala tylko w poziomie), wiec to tabliczka idzie do tekstu,
    # a nie odwrotnie. Obejmuje tekst (image y 46..53) i slot imienia (58..75).
    px0, px1, py0, py1 = PLATE
    vgrad(d, [px0, py0, px1, py1], STONE_TOP, STONE_BOT, 101)
    slim_edge(d, [px0, py0, px1, py1])
    socket(d, px(NAME_SLOT[0]), py(NAME_SLOT[1]))
    return img


def build_class(index):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rail(img, RAIL_R[0], RAIL_R[1], px(152), index, seed=22)
    return img


def parse_config(path):
    """Czyta id, display, description i bonus z sekcji races/classes w config.yml.

    Wczesniej bylo to sklejone z pieciu wyrazen regularnych udajacych parser YAML-a:
    zalezalo od wciec, cudzyslowow pojedynczych i kolejnosci kluczy, a kazda zmiana
    formatowania configu po cichu gubila opis albo cala rase. Teraz czyta to pyyaml —
    czyli ten sam format, ktory czyta serwer.
    """
    try:
        import yaml
    except ImportError as exc:  # pragma: no cover - zalezy od srodowiska
        raise SystemExit(
            "Brak modulu 'pyyaml'. Uruchom generator przez tools/gen.sh (kontener z "
            "kompletem zaleznosci) albo zainstaluj pyyaml we wlasnym srodowisku."
        ) from exc

    with open(path, encoding="utf-8") as handle:
        cfg = yaml.safe_load(handle) or {}

    out = {}
    for section in ("races", "classes"):
        entries = []
        for key, body in (cfg.get(section) or {}).items():
            body = body or {}
            display = re.sub(r"&.", "", str(body.get("display", key)))
            entries.append((key, display,
                            str(body.get("description", "")),
                            str(body.get("bonus", ""))))
        out[section] = entries
    return out


# Kody znakow glifow. Java liczy je tak samo, wiec jedynym zrodlem prawdy o kolejnosci
# jest config.yml — a ten sam config czyta generator.
#   char = GLYPH_BASE + raceIndex * len(classes) + classIndex
#
# DLACZEGO JEDEN OBRAZEK NA KOMBINACJE, a nie dwa skladane shiftem:
# glif bitmapowy ma wlasny "advance" (o ile kursor idzie w prawo po narysowaniu), ktory
# trzeba bylo odgadnac, zeby cofnac sie dokladnie na te sama pozycje przed drugim glifem.
# Pomiary na zrzutach dawaly rozne wyniki (143-150 px) i szyny rozjezdzaly sie w bok.
# Skladanie obu szyn w jeden obrazek kasuje ta niewiadoma calkowicie: tytul ma jeden glif,
# nie ma zadnego cofania. Koszt to 16 malych PNG zamiast 8 — bez znaczenia.
GLYPH_BASE = 0x4100


def write_glyph_config(cfg, path):
    lines = [
        "# WYGENEROWANE przez tools/gen_creator_art.py — nie edytowac recznie.",
        "#",
        "# Jeden glif na kombinacje RASA x KLASA: tytul okna sklada sie z jednego znaku,",
        "# wiec nie trzeba znac 'advance' glifu ani cofac kursora.",
        "#",
        "# font: minecraft:default jest OBOWIAZKOWY. Nexo domyslnie rejestruje glify w",
        "# nexo:default, a tytul inventory renderuje sie fontem minecraft:default — glif",
        "# w zlym foncie wychodzi jako tofu (pusty kwadrat).",
        "# ascent 53 / height 256: gorna krawedz glifu ladnie 40 px GUI NAD panelem,",
        "# dolna 48 px pod nim (patrz naglowek generatora). Dzieki temu pasek gorny idzie",
        "# w gore, a plinta w dol — w obrysie 168 px nie bylo na to miejsca.",
        "# Tekstury MUSZA miescic sie w 256x256 (limit atlasu fontow) — to sufit.",
        "",
    ]
    classes = cfg["classes"]
    for ri, (rkey, _rd, _rx, _rb) in enumerate(cfg["races"]):
        for ci, (ckey, _cd, _cx, _cb) in enumerate(classes):
            name = f"creator_ui_{rkey}_{ckey}"
            code = GLYPH_BASE + ri * len(classes) + ci
            lines += [f"{name}:", "  font: minecraft:default",
                      f"  texture: nexo:gui/{name}", f"  ascent: {ASCENT}",
                      f"  height: {H}",
                      f"  char: {chr(code)}", ""]
    open(path, "w", encoding="utf-8").write("\n".join(lines))
    print(f"zapisano {path}")


if __name__ == "__main__":
    # DOMYSLNIE PISZEMY DO ./out, nie prosto do produkcyjnego packa Nexo.
    # Stara wersja domyslnie nadpisywala pliki dzialajacego serwera — uruchomienie
    # generatora "zeby zobaczyc, co wyjdzie" zmienialo to, co widza gracze. Wdrozenie
    # jest teraz osobna, swiadoma czynnoscia: OUT=... GLYPHS=... albo skopiowanie z out/.
    here = os.path.dirname(os.path.abspath(__file__))
    default_cfg = os.path.join(here, "..", "src", "main", "resources", "config.yml")
    cfg_path = os.environ.get("CFG", os.path.normpath(default_cfg))
    out_dir = os.environ.get("OUT", os.path.join(os.getcwd(), "out", "textures", "gui"))
    os.makedirs(out_dir, exist_ok=True)
    cfg = parse_config(cfg_path)
    for old in os.listdir(out_dir):
        if old.startswith(("creator_race_", "creator_class_", "creator_ui_")):
            os.remove(os.path.join(out_dir, old))
    classes = cfg["classes"]
    for ri, (rkey, _rd, _rx, _rb) in enumerate(cfg["races"]):
        race_layer = build_race(ri)
        for ci, (ckey, _cd, _cx, _cb) in enumerate(classes):
            img = Image.alpha_composite(race_layer, build_class(ci))
            assert max(img.size) <= 256, "PNG przekracza limit atlasu fontow 256x256!"
            img.save(f"{out_dir}/creator_ui_{rkey}_{ckey}.png")
    print(f"  {len(cfg['races']) * len(classes)} kombinacji rasa x klasa zapisanych")
    glyphs_path = os.environ.get("GLYPHS", os.path.join(os.getcwd(), "out", "creator_ui.yml"))
    os.makedirs(os.path.dirname(glyphs_path), exist_ok=True)
    write_glyph_config(cfg, glyphs_path)
    print(f"gotowe: {len(cfg['races'])} ras + {len(cfg['classes'])} klas, {W}x{H}")
