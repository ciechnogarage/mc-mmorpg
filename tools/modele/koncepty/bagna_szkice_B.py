"""Piętnaście szkiców Bagien: 4 px obrazu = 1 px modelu.

Uruchom: PYTHONPATH=. python3 koncepty/bagna_szkice_B.py [katalog_wynikowy]
Każda plansza zawiera trzy różne rozwiązania bryły i test czarnej sylwetki.
"""
from pathlib import Path
import sys

from PIL import Image, ImageDraw, ImageFont


PANEL_W, PANEL_H = 480, 510
STAGE_W, STAGE_H = 400, 285
GROUND = 260
BG = (143, 143, 143)
INK = (38, 38, 38)
V = ((39, 39, 39, 255), (74, 74, 74, 255),
     (123, 123, 123, 255), (201, 201, 201, 255))
T = (63, 209, 180, 255)  # jedyny chromatyczny kolor
try:
    FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 14)
    SMALL = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 12)
    TITLE = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 18)
except OSError:
    FONT = SMALL = TITLE = ImageFont.load_default()


class Sketch:
    def __init__(self):
        self.im = Image.new('RGBA', (STAGE_W, STAGE_H))
        self.d = ImageDraw.Draw(self.im)

    def p(self, pts, tone=1):
        self.d.polygon([(round(190+x), round(GROUND+y)) for x, y in pts], fill=V[tone])

    def e(self, box, tone=1):
        x0,y0,x1,y1 = box
        self.d.ellipse((190+x0, GROUND+y0, 190+x1, GROUND+y1), fill=V[tone])

    def l(self, pts, tone=1, width=4):
        self.d.line([(round(190+x), round(GROUND+y)) for x,y in pts], fill=V[tone], width=width, joint='curve')

    def t(self, box):
        x0,y0,x1,y1 = box
        self.d.ellipse((190+x0, GROUND+y0, 190+x1, GROUND+y1), fill=T)

    def tr(self, box):
        x0,y0,x1,y1 = box
        self.d.rectangle((190+x0, GROUND+y0, 190+x1, GROUND+y1), fill=T)

    def mud(self, x, top, width=4):
        self.l([(x, top), (x, 0)], 0, width)

    def duckweed(self, x, y, length):
        self.l([(x, y), (x+1, y+length)], 0, 3)

    def legs8(self, spread=46, rise=10, thickness=4):
        for side in (-1, 1):
            for i in range(4):
                root = side*(10+i*4)
                knee = side*(23+i*7)
                foot = side*(26+i*spread/4)
                ky = -20-rise-(i%2)*5
                self.l([(root,-20),(knee,ky),(foot,0)], 0, thickness)
                self.e((knee-2,ky-2,knee+2,ky+2),2)

    def frog_legs(self, wide=55, high=42):
        for side in (-1,1):
            self.p([(side*19,-55),(side*(wide-12),-high),(side*wide,-12),
                    (side*(wide+7),-5),(side*(wide+24),0),(side*(wide-15),0),
                    (side*(wide-18),-13),(side*23,-30)],1)
            self.e((side*wide-5,-high-5,side*wide+5,-high+5),2)
            self.l([(side*wide,-high),(side*(wide-18),-9),(side*(wide+12),0)],0,5)


def silk(s, x, y):
    s.p([(x-8,y-3),(x+10,y-2),(x+13,y+5),(x-5,y+6)],3)
    s.l([(x-8,y),(x+10,y+3)],1,2)
    s.duckweed(x+7,y+4,5)


def kokoniatko(v):
    s=Sketch()
    if v==0:  # okrągłe młode: nogi nisko, jajowa skorupka
        s.legs8(31,5,4)
        s.e((-25,-30,21,-5),1); s.e((-19,-31,16,-13),2)
        s.e((-19,-26,-10,-18),3); s.e((-4,-27,5,-18),3)
        s.t((4,-25,19,-12)); silk(s,-6,-31)
    elif v==1:  # odwłok wzniesiony, czoło nisko: czytelny worek za głową
        s.legs8(35,9,4)
        s.e((-31,-25,-5,-7),1); s.e((-12,-38,25,-9),2)
        s.e((-25,-22,-17,-14),3); s.e((-14,-23,-7,-15),3)
        s.t((2,-34,22,-13)); silk(s,11,-38)
        s.l([(-29,-11),(-36,-7)],0,3)
    else:  # mały pajęczyk z pękniętą czapą kokonu
        s.legs8(39,12,3)
        s.e((-19,-31,18,-7),1); s.e((-16,-31,14,-15),2)
        s.p([(-23,-31),(-8,-37),(3,-29),(19,-35),(22,-26),(-20,-23)],3)
        s.e((-14,-24,-7,-17),3); s.e((-1,-25,6,-18),3)
        s.t((5,-28,17,-18)); s.l([(18,-27),(25,-34)],0,2)
    s.mud(-20,-5,3)
    return s.im


def rechotnik(v):
    s=Sketch(); s.frog_legs(55 if v!=2 else 64,45)
    if v==0:  # szeroka paszcza i włócznia skierowana do celu
        s.e((-52,-113,48,-35),1); s.e((-48,-115,43,-69),2)
        s.p([(-48,-75),(44,-75),(34,-59),(-38,-59)],0)
        s.e((-35,-123,-14,-104),3); s.e((10,-123,31,-104),3)
        s.e((-30,-118,-22,-111),0); s.e((18,-118,26,-111),0)
        s.e((-31,-70,24,-39),2); s.t((-20,-67,19,-45))
        s.l([(-37,-65),(-57,-40),(-43,-19)],1,10); s.e((-59,-43,-52,-36),2)
        s.l([(42,-71),(60,-48),(70,-23)],1,9); s.l([(66,-86),(73,1)],0,5)
        s.p([(65,-86),(75,-88),(72,-105)],3)
        s.p([(-35,-31),(18,-29),(12,-19),(-30,-20)],3)
        s.duckweed(31,-95,26)
    elif v==1:  # kucający łowca z pionową ością za plecami
        s.p([(-36,-97),(11,-106),(43,-74),(27,-35),(-29,-43)],1)
        s.e((-67,-119,40,-68),2); s.p([(-64,-82),(42,-81),(19,-67),(-43,-68)],0)
        s.e((-48,-127,-24,-104),3); s.e((12,-128,36,-104),3)
        s.e((-43,-121,-35,-113),0); s.e((19,-122,27,-114),0)
        s.e((-34,-70,28,-38),2); s.t((-25,-64,24,-42))
        s.l([(-39,-73),(-48,-46),(-30,-26)],1,10)
        s.l([(38,-80),(57,-48),(47,-18)],1,10); s.e((52,-50,60,-42),2)
        s.l([(55,-112),(59,0)],0,6); s.p([(51,-112),(63,-120),(57,-136)],3)
        s.p([(-28,-45),(12,-42),(24,-32),(-18,-31)],3)
        s.duckweed(-26,-109,22)
    else:  # asymetryczny zwiadowca, język już zwinięty na jednym policzku
        s.p([(-43,-90),(17,-112),(55,-83),(35,-36),(-39,-39)],1)
        s.e((-58,-128,58,-66),2); s.p([(-61,-82),(59,-81),(35,-65),(-38,-64)],0)
        s.e((-45,-135,-18,-110),3); s.e((23,-135,48,-109),3)
        s.e((-39,-127,-29,-118),0); s.e((31,-127,41,-118),0)
        s.e((-26,-69,38,-34),2); s.t((-18,-62,33,-38))
        s.l([(46,-75),(69,-68),(74,-48),(58,-39)],1,8)
        s.l([(-44,-78),(-66,-55),(-54,-28)],1,9); s.l([(-69,-82),(-74,0)],0,5)
        s.p([(-69,-84),(-78,-94),(-68,-106)],3)
        s.duckweed(23,-113,17)
    for x in (-46,48): s.mud(x,-8,4)
    return s.im


def slimak(v):
    s=Sketch()
    if v==0:  # wysoka spiralna kopuła, głowa długa i niska
        s.e((-105,-35,104,0),1); s.e((-75,-113,72,-14),1)
        s.e((-65,-108,63,-30),2); s.e((-43,-91,45,-33),1)
        s.e((-18,-77,37,-36),2); s.e((3,-63,27,-41),0)
        s.p([(70,-42),(116,-39),(123,-19),(104,-9),(72,-17)],2)
        eyes=[(97,-81),(119,-90)]
        for x,y in eyes:
            s.l([(x-5,-38),(x-2,-60),(x,y)],1,7); s.e((x-7,y-7,x+7,y+7),2); s.t((x-3,y-3,x+3,y+3))
        s.l([(91,-28),(100,-49)],1,3); s.l([(109,-27),(117,-49)],1,3)
        s.l([(-73,-79),(-62,-104)],0,7); s.t((-72,-88,-66,-72))
        s.duckweed(-25,-110,30); s.duckweed(5,-107,18)
    elif v==1:  # długa niska skorupa z pękniętym tyłem
        s.e((-119,-32,111,0),1); s.p([(-99,-25),(-76,-84),(-15,-99),(63,-80),(85,-36),(65,-18)],2)
        s.e((-52,-83,50,-27),1); s.e((-25,-70,36,-34),2); s.e((-5,-58,18,-38),0)
        s.p([(67,-44),(116,-47),(128,-24),(105,-12),(69,-16)],2)
        for x,y in ((97,-91),(123,-81)):
            s.l([(x-8,-42),(x-3,-65),(x,y)],1,6); s.e((x-7,y-7,x+7,y+7),2); s.t((x-3,y-3,x+3,y+3))
        s.l([(-98,-34),(-83,-57),(-94,-69)],0,6); s.t((-96,-52,-88,-41))
        s.duckweed(-35,-87,18); s.duckweed(20,-91,27)
        s.p([(-34,-94),(-28,-113),(-21,-92)],3)
    else:  # skorupa jak warstwowy pancerz, mini bagno na grzbiecie
        s.e((-104,-35,112,0),1); s.e((-72,-111,71,-19),1)
        s.p([(-67,-46),(-48,-99),(18,-107),(63,-73),(70,-32),(25,-48)],2)
        s.p([(-53,-62),(-13,-91),(39,-76),(55,-47),(15,-38)],1)
        s.p([(59,-44),(108,-40),(124,-20),(107,-8),(70,-16)],2)
        for x,y in ((88,-80),(114,-85)):
            s.l([(x-5,-39),(x-2,-61),(x,y)],1,6); s.e((x-7,y-7,x+7,y+7),2); s.t((x-3,y-3,x+3,y+3))
        s.l([(-67,-61),(-56,-79),(-67,-90)],0,6); s.t((-71,-75,-63,-64))
        s.duckweed(-39,-104,30); s.duckweed(13,-108,25)
        s.l([(-18,-110),(-18,-138)],1,3); s.p([(-23,-137),(-18,-146),(-12,-137)],3)
    s.mud(-80,-8,5)
    return s.im


def ognik(v):
    s=Sketch()
    if v==0:  # łza z trzema ogonami
        s.p([(-31,-80),(-19,-124),(-10,-104),(0,-145),(13,-105),(31,-123),
             (37,-82),(20,-56),(-21,-56)],1)
        s.p([(-24,-87),(-8,-113),(2,-96),(16,-113),(27,-84),(14,-64),(-15,-64)],2)
        s.t((-13,-100,15,-69)); s.e((-9,-92,-5,-87),0); s.e((7,-92,11,-87),0)
        s.l([(-25,-82),(-33,-72),(-29,-64)],1,3)
        s.l([(23,-85),(34,-74),(30,-65)],1,3)
        for x,y in ((-40,-73),(38,-66),(6,-51)):
            s.p([(x,y),(x+5,y-8),(x+8,y)],3)
        s.duckweed(-12,-97,15); s.l([(19,-68),(18,-55)],0,3)
    elif v==1:  # lanternowy płomień z kostnymi satelitami
        s.p([(-38,-92),(-29,-119),(-9,-127),(0,-151),(12,-125),(32,-114),
             (39,-79),(19,-56),(-15,-58)],1)
        s.e((-29,-117,29,-66),2); s.t((-19,-108,20,-72))
        s.p([(-10,-100),(11,-100),(17,-83),(2,-78),(-15,-85)],3)
        s.e((-9,-96,-5,-91),0); s.e((6,-96,10,-91),0)
        s.l([(-28,-90),(-38,-81),(-35,-73)],1,3)
        s.l([(27,-89),(37,-79),(34,-71)],1,3)
        s.e((-54,-97,-46,-89),3); s.e((41,-120,50,-111),3)
        s.l([(-49,-93),(-42,-76)],0,2); s.l([(45,-115),(39,-92)],0,2)
        s.p([(-23,-58),(-16,-42),(-9,-59)],1); s.p([(13,-57),(21,-43),(28,-66)],1)
        s.duckweed(-28,-111,15); s.l([(24,-70),(24,-56)],0,3)
    else:  # skośna kometa z długim ogonem wabiącym w wodę
        s.p([(-54,-78),(-52,-100),(-33,-111),(-8,-114),(7,-145),
             (24,-123),(43,-113),(49,-88),(32,-64),(5,-60)],1)
        s.p([(-88,-55),(-56,-82),(-35,-83),(-27,-65),(-53,-63)],1)
        s.p([(-64,-40),(-45,-68),(-23,-68),(-17,-56),(-38,-50)],2)
        s.e((-30,-113,39,-67),2); s.t((-22,-105,29,-74))
        s.p([(-9,-100),(15,-99),(20,-82),(5,-77),(-15,-83)],3)
        s.e((-7,-96,-3,-91),0); s.e((9,-95,13,-90),0)
        s.l([(-22,-83),(-34,-75),(-33,-66)],1,3)
        s.l([(36,-87),(48,-77),(44,-69)],1,3)
        s.p([(48,-108),(54,-116),(58,-106)],3); s.p([(-77,-86),(-74,-94),(-69,-84)],3)
        s.duckweed(9,-114,14); s.l([(-25,-76),(-27,-60)],0,3)
    return s.im


def king(v):
    s=Sketch(); s.frog_legs(97 if v!=2 else 109,75)
    if v==0:  # niski, szeroki tron z korony i pyska
        s.e((-105,-148,102,-33),1); s.p([(-91,-135),(88,-135),(107,-66),(-106,-66)],2)
        s.p([(-103,-102),(107,-102),(89,-65),(-90,-65)],0)
        s.e((-76,-166,-42,-133),3); s.e((42,-167,76,-133),3)
        s.e((-66,-157,-52,-146),0); s.e((51,-157,65,-146),0)
        s.e((-64,-70,62,-23),2); s.t((-52,-65,51,-28))
        s.p([(-55,-167),(-43,-204),(-20,-180),(0,-216),(20,-180),(46,-204),(56,-168)],1)
        for x,y in ((-43,-204),(0,-216),(46,-204)):
            s.e((x-5,y-3,x+5,y+5),3); s.duckweed(x+3,y+4,16)
        s.l([(-88,-108),(-125,-81),(-111,-33)],1,14)
        s.l([(91,-108),(124,-82),(110,-29)],1,14)
        s.l([(120,-84),(133,0)],0,7); s.p([(124,-87),(140,-95),(133,-111)],3)
        s.p([(-75,-50),(74,-48),(56,-35),(-63,-36)],3)
        s.duckweed(-82,-141,32)
    elif v==1:  # pysk wysunięty jak brama, ciężki brzuch
        s.e((-97,-145,104,-20),1); s.e((-86,-152,85,-59),2)
        s.p([(-128,-121),(125,-121),(116,-78),(-119,-78)],1)
        s.p([(-123,-94),(120,-94),(98,-72),(-103,-72)],0)
        s.e((-80,-160,-43,-127),3); s.e((45,-160,82,-127),3)
        s.e((-69,-152,-54,-142),0); s.e((55,-152,70,-142),0)
        s.e((-70,-79,75,-25),2); s.t((-56,-72,60,-34))
        s.p([(-50,-164),(-38,-191),(-13,-171),(0,-207),(16,-170),(41,-192),(51,-164)],1)
        for x,y in ((-40,-192),(0,-207),(41,-193)): s.duckweed(x,y,20)
        s.l([(-88,-93),(-123,-62),(-107,-20)],1,15)
        s.l([(94,-94),(127,-63),(105,-20)],1,15)
        s.l([(-127,-91),(-140,-2)],0,8); s.p([(-135,-90),(-145,-103),(-139,-115)],3)
        s.p([(-67,-56),(61,-55),(47,-39),(-51,-39)],3)
        s.duckweed(72,-149,28)
    else:  # wysoka korona i płaszcz z sieci, berło na boku
        s.p([(-100,-124),(91,-124),(116,-36),(-112,-36)],1)
        s.e((-104,-158,101,-50),2)
        s.p([(-107,-111),(103,-111),(84,-75),(-91,-75)],0)
        s.e((-79,-176,-42,-143),3); s.e((43,-177,80,-144),3)
        s.e((-67,-166,-53,-156),0); s.e((54,-166,68,-156),0)
        s.e((-62,-78,68,-29),2); s.t((-52,-72,59,-35))
        s.p([(-49,-175),(-43,-203),(-25,-188),(-14,-219),(0,-199),(13,-232),
             (25,-190),(44,-207),(52,-175)],1)
        for x,y in ((-43,-203),(-14,-219),(13,-232),(44,-207)):
            s.e((x-4,y-3,x+4,y+4),3); s.duckweed(x+3,y+4,19)
        s.l([(-91,-120),(-124,-85),(-111,-37)],1,14)
        s.l([(88,-122),(120,-87),(105,-39)],1,14)
        s.l([(124,-138),(135,0)],0,8); s.p([(124,-138),(141,-146),(134,-161)],3)
        for x in (-66,-38,0,40,69): s.l([(x,-122),(x+7,-49)],3,2)
        for y in (-109,-84,-59): s.l([(-90,y),(86,y+3)],0,2)
        s.duckweed(-76,-152,27)
    for x in (-85,89): s.mud(x,-20,7)
    return s.im


MOBS = {
    'kokoniatko': (kokoniatko, [
        ('K1 | Jajko na ośmiu stopach', 'Niski rój; resztka kokonu siedzi na czole.'),
        ('K2 | Worek wysoko', 'Jad na odwłoku czyta się za dużymi oczami.'),
        ('K3 | Pęknięta czapa', 'Ośmionożne młode wychodzi spod skorupy kokonu.')]),
    'rechotnik': (rechotnik, [
        ('R1 | Szeroka paszcza', 'Ość celuje naprzód, gardło puchnie przed językiem.'),
        ('R2 | Łowca w kucki', 'Ość stoi za grzbietem; dominują zgięte uda.'),
        ('R3 | Zwiadowca', 'Jedna ręka jest wolna do wyrzutu języka.')]),
    'slimak': (slimak, [
        ('S1 | Spiralna kopuła', 'Wysoka skorupa dominuje nad długą niską stopą.'),
        ('S2 | Pęknięty taran', 'Niska długa skorupa odsłania szczelinę z tyłu.'),
        ('S3 | Ruchome mokradło', 'Warstwowy pancerz niesie trzcinę i zwis rzęsy.')]),
    'ognik': (ognik, [
        ('O1 | Łza', 'Trzy ogony otaczają czaszkę w turkusowym rdzeniu.'),
        ('O2 | Latarnia', 'Satelity z kości krążą wokół zwartego płomienia.'),
        ('O3 | Kometa', 'Skośny ogon wskazuje drogę ku wodzie.')]),
    'krol_ropuch': (king, [
        ('K1 | Tron z paszczy', 'Niski szeroki król trzyma berło obok gardła.'),
        ('K2 | Żywa brama', 'Paszcza wysuwa się przed brzuch na całą szerokość.'),
        ('K3 | Płaszcz z sieci', 'Wysoka korona i sieć dodają groteskowego majestatu.')]),
}

# 16 px modelu = 1 blok, więc blok ma dokładnie 64 px planszy.
BODY_PX = {'kokoniatko':32, 'rechotnik':90, 'slimak':102,
           'ognik':52, 'krol_ropuch':192}


def draw_player(d, x, ground):
    # 32 px modelu * 4 = 128 px obrazu, bez turkusowego akcentu.
    d.rectangle((x+10,ground-128,x+34,ground-99),fill=INK)
    d.rectangle((x+8,ground-96,x+36,ground-46),fill=INK)
    d.rectangle((x,ground-94,x+8,ground-49),fill=INK)
    d.rectangle((x+36,ground-94,x+44,ground-49),fill=INK)
    d.rectangle((x+9,ground-46,x+21,ground),fill=INK)
    d.rectangle((x+24,ground-46,x+36,ground),fill=INK)


def main(out_dir):
    out_dir.mkdir(parents=True, exist_ok=True)
    for name,(fn,variants) in MOBS.items():
        sheet=Image.new('RGB',(3*PANEL_W,PANEL_H),BG)
        d=ImageDraw.Draw(sheet)
        for i,(heading,hook) in enumerate(variants):
            x0=i*PANEL_W
            original=fn(i)
            box=original.getchannel('A').getbbox()
            body=original.crop(box)
            ratio=BODY_PX[name]/body.height
            body=body.resize((max(1,round(body.width*ratio)),BODY_PX[name]),Image.Resampling.NEAREST)
            stage=Image.new('RGBA',(STAGE_W,STAGE_H))
            # Ognik ma 0,8 bl. wysokości i unosi się 1,2 bl. nad ziemią.
            clearance=77 if name=='ognik' else 0
            stage.alpha_composite(body,(190-body.width//2,GROUND-clearance-body.height))
            sheet.paste(stage,(x0+26,46),stage)
            d.line((x0+18,46+GROUND,x0+459,46+GROUND),fill=(72,72,72),width=2)
            draw_player(d,x0+415,46+GROUND)
            d.text((x0+18,12),heading,font=TITLE,fill=INK)
            d.text((x0+18,350),hook,font=FONT,fill=INK)
            # Test kształtu: ten sam obrys w 45% skali, bez waloru i światła.
            alpha=stage.getchannel('A')
            bbox=alpha.getbbox()
            if bbox:
                crop=alpha.crop(bbox)
                thumb=crop.resize((max(1,round(crop.width*.45)),max(1,round(crop.height*.45))),Image.Resampling.NEAREST)
                shadow=Image.new('RGB',thumb.size,(8,8,8))
                sheet.paste(shadow,(x0+225-thumb.width//2,435-thumb.height//2),thumb)
            d.text((x0+18,396),'TEST SYLWETKI',font=SMALL,fill=INK)
            if i<2: d.line((x0+PANEL_W-1,0,x0+PANEL_W-1,PANEL_H),fill=(95,95,95),width=2)
        out=out_dir/f'{name}.png'; sheet.save(out)
        print(out)


if __name__=='__main__':
    main(Path(sys.argv[1]) if len(sys.argv)>1 else Path('/opt/docker/mc/data/world1/output/lochy/bagna/szkice'))
