"""Ikony 16×16 i arkusz porównawczy. Uruchom: python3 v2/grafika_ikony.py."""

from pathlib import Path

from PIL import Image, ImageColor, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
ITEMS = ROOT / "src/Nexo/pack/assets/zaklecia/textures/item"


def icon(name, palette, rows):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows), name
    im = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, mark in enumerate(row):
            if mark != ".":
                im.putpixel((x, y), (*ImageColor.getrgb(palette[mark]), 255))
    im.save(ITEMS / f"ikona_{name}.png")


fire = {
    "D": "#803019", "R": "#ba441c", "O": "#d95b1c", "T": "#f68b25",
    "G": "#ffbd31", "Y": "#ffe081", "W": "#fff4bb",
}

icon("ognisty_pocisk", fire, [
    "................",
    "...........DD...",
    "..........DRRD..",
    ".........DRGGOD.",
    "........DRYWWGD.",
    ".......DORYWWGD.",
    "......DORTYWYOD.",
    ".....DORTGGOTD..",
    "...D.DOTTOODD...",
    "..DRD.RTOOD.....",
    ".DROTDRTOD......",
    ".DOTORDRD.......",
    "DOTTODD.........",
    "DRTDD...........",
    "DDD.............",
    "................",
])

icon("runa_ognia", fire, [
    "................",
    ".....T...G......",
    ".....D...D......",
    ".....R...RY.....",
    "........G.......",
    "...T..........G.",
    "...D..........D.",
    "................",
    ".....DDDDDD.....",
    "...DDOTTTTODD...",
    "..DOT..Y...TOD..",
    ".DOT..G.G...TOD.",
    "..DORGGGGG.ROD..",
    "...DDOTTTTODD...",
    ".....DDDDDD.....",
    "................",
])

atronach = {
    **fire, "K": "#3c2b31", "B": "#604039", "C": "#81513f",
}

icon("atronach_ognia", atronach, [
    "....T...Y...T...",
    "...DGT..GY..GT..",
    "..DRGT.TYGT.GTD.",
    "...DRGTGGTGGTOD.",
    "....DRGTGTODD...",
    "....KKRRRKK.....",
    "...KKCCCCCCKK...",
    "...KBWCKKBWCK...",
    "..KKCBWCCWCKK...",
    ".KKBBCCGCCBBKK..",
    "KBCBKKCTCKKBCBK.",
    "KBCBKCGGCKKBCBK.",
    ".KKKBCCTCCBKKK..",
    "...KBBCCBBK.....",
    "....KCCCBK......",
    ".....KKKK.......",
])

heal = {
    "D": "#684c39", "S": "#b37a58", "P": "#ebbD90", "L": "#ffdfb3",
    "O": "#b98527", "G": "#dba83c", "Y": "#f5ce69", "W": "#fff0b2",
    "V": "#75b98d",
}

icon("leczenie", heal, [
    "................",
    ".....OO.OO...WG.",
    ".GY.OGWOGWO.....",
    "..G.OGWWWGO.....",
    ".....OGWGO..Y...",
    "......OYO.......",
    "......DLDDLD....",
    "...DLDDPDDPDDLD.",
    "...DPDDPDDPDDPD.",
    "...DPDDPDDPDDPD.",
    ".DLLPPPPPPPPPD..",
    ".DPPLLPPPPPPPD..",
    "..DPPLLPPPPPPD..",
    "....DPLPPPPPD...",
    ".....DPSSSPD....",
    "......DDDDD.....",
])

icon("leczenie_dloni", heal, [
    "................",
    "............V...",
    "...........VYV..",
    "...........YWY..",
    "..........VYWYV.",
    "...........VYV..",
    "............V...",
    ".....DD....G....",
    "....DPLDDDGY....",
    "..DDPLLLLPDYY...",
    ".DPLLLLPDGGG....",
    ".DPPPPPLDDG.....",
    "..DPPPPD.G......",
    "..DSSSD.........",
    "...DDD..........",
    "................",
])

icon("uzdrowienie_innego", heal, [
    "......Y.W.......",
    ".....OGGO.......",
    ".....OGWO.Y.....",
    "...OOOGWOOO.....",
    "...OGWWWWGO.....",
    "...OOOGWOOO.....",
    ".....OGWO.......",
    "................",
    "......DDDD......",
    ".....DVVVD......",
    ".....DVVVD......",
    "......DDDD......",
    "....DDDVVDDD....",
    "...DVVVVVVVVD...",
    "..DVVVVVVVVVVD..",
    "..DDDDDDDDDDDD..",
])

spirit = {
    "D": "#38395f", "N": "#59647f", "T": "#50b9bf", "A": "#7ad9d7",
    "L": "#9ce9e5", "W": "#e8fcf7", "P": "#ad9ce2", "E": "#ecffff",
}

icon("duch_wilka", spirit, [
    "....L...A.......",
    "...LD...DA......",
    "..LTD..DTA......",
    "..DTTDDTTA......",
    "..DTTTTTTAD.....",
    ".DNTTLATTTAD....",
    ".DNTTATTEATDD...",
    "..DTTTTTDTTTAD..",
    "..DTTTADTTTTTDD.",
    ".DNTTADTTTAAAAAD",
    ".DNTTADTTTTTADDD",
    "..DTTTDDTTTDD...",
    "...DTTTDDDD.....",
    "....DTTTTD......",
    ".....DDDD.......",
    "......L.........",
])

calm = {
    "D": "#51365e", "M": "#9b5fb1", "P": "#d584c7", "L": "#efb1dc",
    "W": "#fce1ef", "A": "#8068ae",
}

icon("uspokojenie", calm, [
    "................",
    "...A....A....A..",
    "....A....A....A.",
    ".....A....A....A",
    "......DDDD......",
    "....DDPPLPDD....",
    "...DPLMMMMPLD...",
    "..DPMM....MMPD..",
    ".DMM.......MMD..",
    ".DD.........DD..",
    "..D.D......D.D..",
    "...D........D...",
    ".....A....A.....",
    "...A..A..A..A...",
    "....A..A..A.....",
    "................",
])

icon("przywolany_miecz", spirit, [
    "...........DDDDD",
    "..........DDPPPD",
    ".........DDPPWPD",
    "........DDPPWPD.",
    ".......DDPPWPD..",
    "......DDPPWPD...",
    ".....DDPPWPD....",
    "....DDPPWPD.....",
    "...ADPPPPD......",
    "...DADDDD.......",
    "..DNNADD........",
    ".DNNDDAD........",
    ".DNDD..AD.......",
    "..DD....AD......",
    ".D..............",
    "................",
])


def make_preview():
    names = [
        ("plomien", "plomienie"), ("ikona_ognisty_pocisk", "ognisty pocisk"),
        ("ikona_kula_ognia", "kula ognia"), ("ikona_runa_ognia", "runa ognia"),
        ("ikona_atronach_ognia", "atronach ognia"), ("mroz", "odmrozenie"),
        ("ikona_leczenie", "leczenie"), ("ikona_leczenie_dloni", "leczenie dloni"),
        ("ikona_uzdrowienie_innego", "uzdrowienie innego"),
        ("ikona_zasklepienie_ran", "zasklepienie ran"),
        ("ikona_wielkie_uzdrowienie", "wielkie uzdrowienie"),
        ("ikona_oslona", "oslona"), ("ikona_przywolany_miecz", "przywolany miecz"),
        ("ikona_duch_wilka", "duch wilka"), ("ikona_uspokojenie", "uspokojenie"),
    ]
    sheet = Image.new("RGB", (1460, 549), "#30353d")
    d = ImageDraw.Draw(sheet)
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    for i, (filename, label) in enumerate(names):
        x, y = (i % 5) * 292, (i // 5) * 183
        d.rectangle((x + 2, y + 2, x + 289, y + 180), outline="#57616d")
        icon_im = Image.open(ITEMS / f"{filename}.png").convert("RGBA").crop((0, 0, 16, 16))
        scaled = icon_im.resize((128, 128), Image.Resampling.NEAREST)
        for bg_x, bg in ((x + 10, "#ecefe9"), (x + 147, "#262c37")):
            d.rectangle((bg_x, y + 10, bg_x + 135, y + 145), fill=bg)
            sheet.paste(scaled, (bg_x + 4, y + 14), scaled)
        box = d.textbbox((0, 0), label, font=font)
        d.text((x + (292 - box[2]) / 2, y + 154), label, fill="#e9edf0", font=font)
    sheet.save(ROOT / "podglad_ikon.png")


make_preview()
