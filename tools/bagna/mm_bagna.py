#!/usr/bin/env python3
"""Generator MythicMobs dla 10 mobow Bagien (KARTY.md) -> STAGING output/lochy/bagna/mm/{Bagna_mobs.yml,Bagna_skills.yml}.
NIE pisze do plugins/. Uruchamiac przez ./uruchom.sh (python:3.12-slim + pyyaml, bramki).
Zrodla: KARTY.md (zdolnosci, dzwieki, lup), kontrakt.py (animacje i czasy; czasy.json modelarzy), dzwieki.py (bagna:*).
Wzorce: forge/tools/przeciwnicy (zamkniety cel: zapowiedz -> skill @targetlocation -> delay -> trafienie w punkt;
kara za stanie przy mobie r>=3.5), build_loch002_md.py (Despawn persistent, bez randomstroll, EXP przez MMOCore).
Pulapki z pamieci: inline ?warunek = rzucajacy (warunek na graczu -> TargetConditions), healthpercent p= przez ~onTimer,
setvariable val=, ~onInteract martwy na modelu ME (nic tu nie jest klikane), base Silent + bez Equipment/scale.
UDERZENIE=delay (domyslnie): obrazenia po `delay hit` od startu animacji; skill *_KLATKA (skrypt w klatce .bbmodel) gra tylko
dzwiek uderzenia. UDERZENIE=klatka: zarezerwowane do czasu testu skryptow klatek ME na klonie (patrz KONTRAKT)."""
import json, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kontrakt, dzwieki

OUT = os.environ.get("OUT", "/opt/docker/mc/data/world1/output/lochy/bagna/mm")
DZWIEKI_LR = os.environ.get("DZWIEKI_LR", "1") == "1"   # warstwy bagna:* (wymaga packa z dzwieki.py)
UDERZENIE = os.environ.get("UDERZENIE", "delay")
# TYLKO ARENA: baza widoczna (model{i=false}) - bot mineflayer bije encje bazowa; z ukryta baza bije hitbox 'interaction'
# i trafienia gina (Szeptucha: 1 cios na 10 s, klon 03.10). Na serwer NIGDY.
BAZA = ";i=false" if os.environ.get("BAZA_WIDOCZNA") == "1" else ""
assert UDERZENIE == "delay", "UDERZENIE=klatka dopiero po tescie skryptow klatek na klonie"
SOUNDS_MC = "/opt/docker/mc/forge/gracz/node_modules/minecraft-data/minecraft-data/data/pc/1.21.11/sounds.json"

T = lambda s: max(1, int(round(s * 20)))

# ---------------- LICZBY (jedyne miejsce strojenia). Skala: docs/lochy/akt1-v2/07-wspolne-liczby-i-rejestr.md
# L1 DPS_ref(N) = 3 + 0,5(N-1); L2 moby = Gaj v8 x (1 + 0,12(N-1)), elita = 25% HP bossa lochu (N=2: 525 -> 131).
# L002 = N 2 (x1.12, DPS_ref 3.5); Mokradla = poz. 4-7 -> N 5 (x1.48, DPS_ref 5.0). Wynik pomiaru: output/lochy/bagna/mm/WYNIKI_ARENY.md
WERSJA = "v11"
# v10: Kokoniara skok 6, 10% ciosow (v9 uwazny 60%).
# v9 (piatka): Pijawka wyskok tylko z wody wiec znow 4; Kokoniatko doskok 1 i 30% ciosow (mlode z Wylegu dobijaly uwaznego).
# v8: kokon 3 ciosy + trucizna I 2 s, ugryzienie 1 s; Kosciej oslona (targetinlineofsight + flaga swiata); Rechotnik/Krol jezyk przyciaga.
# v6: znaczniki dzialaja, wiec Wyskok Pijawek trafia (3x4 zabijalo uwaznego) -> 2 i co 7 s; Krol HP 225 (07: elita =
#     25% bossa N=5 ~900); Wyleg Kokoniary 3 mlode.
# v5: arena Z MODELAMI (bot bije hitbox 'interaction'); Slimak sluz 8->5, Krol wzywa 1 Rechotnika i skok r=3.5,
#     Kokoniara skok 8 i 20% ciosow, Rechotnik 30% ciosow, Ognik plomien 10 z 3.5 bl.
# v3: vanillowy cios ~1/s dalej zabijal uwaznego (Kokoniara, Rechotnik, Slimak, Krol) -> CancelEvent ~onAttack z szansa
#     (CIOS_PRZECHODZI = ile ciosow dochodzi), Krol wzywa Rechotniki dopiero <75% HP, Ognik HP 38 (07: Lucznik 26 x1.48).
# v2 (arena 03.10, v1 w WYNIKI_ARENY.md): obrazenia przeniesione z vanillowego ciosu (nie do unikniecia, ~1/s) do
# zapowiedzianych atakow; HP wg 07 L2 (L002 x1.12 Gaju, elita 131; Mokradla x1.48).
STATY = dict(
    PIJ_HP=10, PIJ_DMG=2, PIJ_WYSKOK=4,
    MUL_HP=45, MUL_DMG=3, MUL_ARM=1, MUL_ZAMACH=9, MUL_RZUT=5,
    KOS_HP=29, KOS_DMG=2, KOS_STRZAL=7, KOS_DOBICIE=4,
    SZE_HP=36, SZE_DMG=2, SZE_FLASZKA=5, SZE_SZEPT=6,
    KOK_HP=131, KOK_DMG=2, KOK_ARM=1, KOK_SKOK=6,
    KKT_HP=6, KKT_DMG=1, KKT_DOSKOK=1,
    REC_HP=60, REC_DMG=3, REC_SLAM=12,
    SLI_HP=90, SLI_DMG=2, SLI_ARM=0, SLI_SLUZ=5,
    OGN_HP=38, OGN_PLOMIEN=10,
    # ulamek vanillowych ciosow, ktore dochodza (reszta: CancelEvent ~onAttack) - zmierzone v2: cios co ~1 s nie do unikniecia
    CIOS_KOK=0.1, CIOS_KKT=0.3, CIOS_REC=0.3, CIOS_SLI=0.25, CIOS_KRO=0.15,
    KRO_HP=225, KRO_DMG=3, KRO_ARM=3, KRO_JEZYK=4, KRO_SKOK=9, KRO_PASZCZA=3,
)
STANY, ZAPOWIEDZI, BOT = [], [], {}

class Mob:
    def __init__(s, mob):
        s.m, s.M, s.mod = mob, mob.upper(), f"bagna_{mob}"
        s.cz = kontrakt.czasy(mob)
        s.sk = []           # (nazwa, tekst)
    def st(s, anim, li=1, lo=2, x=""):
        assert anim in s.cz, f"{s.m}: stan {anim} spoza kontraktu"
        STANY.append((s.mod, anim))
        return f"state{{m={s.mod};s={anim};li={li};lo={lo}{x}}}"
    def zdejmij(s, anim):
        STANY.append((s.mod, anim))
        return f"state{{m={s.mod};s={anim};r=true;lo=3}}"
    def skill(s, nazwa, tekst):
        s.sk.append((nazwa, tekst.strip("\n") + "\n"))
    def zapowiedz(s, anim, dz, czastki, reakcja, opis, czas=None, gcd_dodatek=0.0, ms_reakcji=None):
        """linie zapowiedzi: GCD, blokada obrotu modelu, stan, dzwiek warstwowy, czastki, mob stoi w miejscu"""
        w = czas if czas is not None else s.cz[anim]["windup"]
        c = s.cz[anim]
        gcd = T(max(w, c["hit"]) + c["recover"] + 0.3 + gcd_dodatek)
        pierwszy = dz[0]
        ZAPOWIEDZI.append((s.m, anim, pierwszy[0], pierwszy[2]))
        BOT.setdefault(f"BAGNA_{s.M}", []).append(dict(dzwiek=pierwszy[0], pitch=pierwszy[2], reakcja=reakcja,
                                                      ms=ms_reakcji or int(w * 1000) + 300, opis=opis))
        L = [f"  - gcd{{ticks={gcd}}}", "  - lockmodel{l=true} @self", f"  - {s.st(anim, 1, 2)} @self"]
        L += [f"  - {snd(*d)} @self" for d in dz]
        L += [f"  - {p} @self" for p in czastki]
        L.append(f"  - potion{{type=SLOWNESS;duration={T(w)};level=10;hasParticles=false}} @self")
        return "\n".join(L)
    def odslona(s, anim, dodatkowa=0.0):
        r = s.cz[anim]["recover"] + dodatkowa
        return (f"  - potion{{type=SLOWNESS;duration={T(r)};level=10;hasParticles=false}} @self\n"
                f"  - delay {T(r)}\n  - lockmodel{{l=false}} @self")
    def klatki(s, dz_uderzenia):
        """skille skryptow klatek (dzis: tylko dzwiek uderzenia); dz_uderzenia: anim -> [(s,v,p)]"""
        for a in s.cz.values():
            if kontrakt.ma_klatke(a):
                dz = dz_uderzenia.get(a["n"], [])
                linie = "".join(f"  - {snd(*d)} @self\n" for d in dz) or "  - delay 0\n"
                s.skill(kontrakt.KLATKA(s.m, a["n"]), f"  Skills:\n{linie}")

PUNKTY = []   # znaczniki zamknietego punktu: (id moba, nazwa skilla, tekst skilla)
def punkt(nazwa, linie):
    """Zamkniety punkt = niewidzialny znacznik ARMOR_STAND przyzwany NA CELU w chwili zapowiedzi (summon @target), ktory sam
    odlicza czas i bije @PlayersInRadius wokol SIEBIE. Zmierzone na klonie 03.10: @PlayersNearOrigin w skillu wolanym
    @targetlocation zwraca 'Unknown' i nie zadaje obrazen; projectile/shoot z takiego skilla startuja z punktu.
    W linii: @self = punkt, @PlayersInRadius{r=..} = gracze przy punkcie. Zwraca linie summon do skilla moba."""
    mid = f"BAGNA_P_{nazwa}"
    PUNKTY.append((mid, mid + "_S", linie.strip("\n") + "\n  - remove @self\n"))
    return f"  - summon{{m={mid};level=1;a=1;r=0}} @target"

def snd(s, v=1.0, p=1.0):
    return f"sound{{s={s};v={v};p={p};sc=HOSTILE}}"

def lr(*args):
    """warstwa LittleRoom (bagna:*) - pomijana przy DZWIEKI_LR=0"""
    return [args] if DZWIEKI_LR else []

TRUPY = []   # (mob, model, dl death)
def naglowek_modelu(o, melee=True):
    # nazwa nad glowa: robi PaskiMobow (wpisy mm/paskimobow_bagna.yml); setmodeltag ME nie ustawial tekstu (klient QA 03.10)
    # smierc: vanilla usuwa encje po 20 tickach (model znikal po ~0,7 s z 1,2 s animacji) -> ~onDeath zdejmuje model i przyzywa
    # "trupa" (ARMOR_STAND z tym samym modelem), ktory gra death do konca i trzyma ostatnia klatke
    TRUPY.append((o.M, o.mod, o.cz["death"]["dl"]))
    STANY.append((o.mod, "death"))
    L = [f"  - model{{m={o.mod};n=tag_name;step=1{BAZA}}} @self ~onSpawn",
         f"  - model{{m={o.mod};n=tag_name;step=1{BAZA}}} @self ~onLoad",
         # smierc: model zostaje na umierajacej encji (ME sam gra "death" ~1 s do zdjecia encji); proba "trupa"
         # (ARMOR_STAND z modelem + death_trup/hold) nie dzialala na kliencie QA (model w idle, znikal po 1,25 s) - 03.10
         f"  - brightness{{m={o.mod};p=swiatlo_;em=false;b=15;s=15}} @self ~onSpawn",
         f"  - brightness{{m={o.mod};p=swiatlo_;em=false;b=15;s=15}} @self ~onLoad",
         f"  - {o.st('hurt', 1, 2)} @self ~onDamaged"]
    if melee:
        L.append(f"  - {o.st('attack', 1, 2)} @self ~onAttack")
    return "\n".join(L) + "\n"

def exp(main, hunt=0):
    s = f'  - command{{c="mmocore admin exp give <trigger.name> main {main}"}} @trigger ~onDeath ?~isplayer\n'
    if hunt:
        s += f'  - command{{c="mmocore admin exp give <trigger.name> hunting {hunt}"}} @trigger ~onDeath ?~isplayer\n'
    return s

def opcje(despawn="persistent", **kw):
    # bez Invisible: PaskiMobow nie rysuje paska nad niewidzialnym mobem (widac() w zrodle pluginu); baze chowa ME
    base = dict(Despawn=despawn, Silent="true", PreventOtherDrops="true", PreventItemPickup="true",
                PreventRandomEquipment="true", PreventSunburn="true", PreventLeashing="true")
    base.update({k: v for k, v in kw.items()})
    return "  Options:\n" + "".join(f"    {k}: {v}\n" for k, v in base.items())

AI_WRECZ = "  AIGoalSelectors:\n  - clear\n  - meleeattack\n  - lookatplayers\n  - randomlookaround\n"
AI_STOI = "  AIGoalSelectors:\n  - clear\n  - lookatplayers\n  - randomlookaround\n"
CELE = "  AITargetSelectors:\n  - clear\n  - attacker\n  - players\n"
def lup(*d):
    return "  Drops:\n" + "".join(f"  - mmoitem{{type=MATERIAL;id={i}}} {n} {p}\n" for i, n, p in d) if d else ""
WODA = "  - potion{type=WATER_BREATHING;duration=999999;level=0;hasParticles=false} @self ~onSpawn\n"
TURKUS = "dust;color=#3fd1b4"

MOBS, SKILLS = ["# Bagna - 10 mobow (KARTY.md). GENEROWANE przez MC/tools/bagna/mm_bagna.py - nie edytowac recznie.\n"
                "# Liczby SUROWE (bez klas/zbroi) - stroic na arenie (forge/tools/przeciwnicy/README, cel: uwazny 8-15 s ~0-2% HP).\n"], \
               ["# Skille Bagien - GENEROWANE przez MC/tools/bagna/mm_bagna.py (caster = mob, delay dozwolony).\n"]

# =====================================================================================================  1 PIJAWKA (L002, roj)
o = Mob("pijawka"); c = o.cz
MOBS.append(f"""
BAGNA_PIJAWKA:
  Type: ENDERMITE
  Display: '&2Pijawka'
  Health: @PIJ_HP@
  Damage: @PIJ_DMG@
  Faction: topiel
{opcje(FollowRange=11, MovementSpeed=0.32)}{lup(("PROF_BEAST_SCRAP", 1, 0.3), ("PROF_VENOM_SAC", 1, 0.06))}{AI_WRECZ}{CELE}  Skills:
{naglowek_modelu(o)}{WODA}  # Przyssanie: 20% trucizna 2 s (kontra: zabij szybko)
  - potion{{type=POISON;duration=40;level=0}} @target ~onAttack 0.2
  - skill{{s=BAGNA_PIJAWKA_WYSKOK}} @target ~onTimer:20
  - particles{{p=block;m=MUD;a=10;hs=0.3;vs=0.2;s=0.05}} @self ~onDeath
{exp(8, 2)}""")
o.skill("BAGNA_PIJAWKA_WYSKOK", f"""
  # WYSKOK: babelki + kolysanie 0.6 s -> luk w ZAMKNIETY punkt (kontra: krok w bok). W lochu stawiac w wodzie (karta: zawsze z wody).
  Cooldown: 7
  Conditions:
  - offgcd true
  # karta: wyskok ZAWSZE z wody (v8; na ladzie tylko kasa)
  - inliquid true
  - targetwithin{{d=7}} true
  - targetnotwithin{{d=2}} true
  Skills:
{o.zapowiedz("wyskok", [("entity.slime.squish_small", 1.4, 1.6), ("block.bubble_column.upwards_ambient", 1.6, 1.0)] + lr("bagna:pijawka.bable", 1.0, 1.0),
             ["particles{p=bubble_pop;a=12;hs=0.3;vs=0.2;y=0.3}"], "bok", "Wyskok: krok w bok")}
{punkt("PIJAWKA_WYSKOK", f"""  Skills:
  - particlering{{p={TURKUS};size=0.8;radius=1.0;points=10;a=1;y=0.1}} @self
  - delay {T(c['wyskok']['hit'])}
  - damage{{amount=@PIJ_WYSKOK@}} @PlayersInRadius{{r=1.3}}
  - particles{{p=block;m=MUD;a=10;hs=0.5;vs=0.1}} @self
""")}
  - skill{{s=BAGNA_PIJAWKA_LOT}} @targetlocation
  - delay {T(c['wyskok']['hit'])}
{o.odslona("wyskok", 0.5)}
""")
o.skill("BAGNA_PIJAWKA_LOT", f"""
  # leap w skillu wolanym @targetlocation: mob skacze w ZAMKNIETY punkt (sprawdzone v1: Kokoniara)
  Skills:
  - delay {T(c['wyskok']['windup'])}
  - leap{{velocity=120}} @origin
""")
o.klatki({"wyskok": [("entity.silverfish.hurt", 1.0, 0.7)] + lr("bagna:pijawka.klap", 1.0, 1.0), "attack": [("entity.silverfish.hurt", 0.8, 0.7)]})
SK_PIJ = o.sk

# =====================================================================================================  2 MULOWIEC (L002, bruiser)
o = Mob("mulowiec"); c = o.cz
MOBS.append(f"""
BAGNA_MULOWIEC:
  Type: DROWNED
  Display: '&aMułowiec'
  Health: @MUL_HP@
  Damage: @MUL_DMG@
  Armor: @MUL_ARM@
  Faction: topiel
{opcje(FollowRange=12, MovementSpeed=0.19, KnockbackResistance=0.6)}{lup(("PROF_CLAY_LUMP", 1, 0.6), ("PROF_FLAX_FIBER", 1, 0.35))}{AI_WRECZ}{CELE}  Skills:
{naglowek_modelu(o, melee=False)}  # vanillowy cios zablokowany: bije tylko Zamachem (zapowiedziany)
  - CancelEvent ~onAttack
  - skill{{s=BAGNA_MULOWIEC_ZAMACH}} @target ~onTimer:10
  - skill{{s=BAGNA_MULOWIEC_RZUT}} @target ~onTimer:20
  # SLAD: kaluza co 2 s, zyje 6 s (kontra: nie idz jego sladem)
  - summon{{m=BAGNA_P_MULOWIEC_SLAD;level=1;a=1;r=0}} @self ~onTimer:40
{''.join(f"  - {snd(*d)} @self ~onTimer:40" + chr(10) for d in lr("bagna:mulowiec.krok", 0.6, 0.9))}{exp(25, 6)}""")
o.skill("BAGNA_MULOWIEC_ZAMACH", f"""
  # ZAMACH LOPATA: lopata za plecy 0.8 s -> luk 120 st. przed soba r=3.5 (fieldofview 120, karta; v1-v6: 240) - kara za stanie przy nim
  # (arena 03.10: wersja "w zamkniety punkt r=2.4" nie trafila bota ani razu - bot w zwarciu i tak sie rusza; lekcja 2 README areny)
  # kontra: odskok >= 3.5 bl. albo za plecy. v2: co 5 s (v1 4 s: uwazny walczyl 24 s, za malo okien na bicie)
  Cooldown: 5
  Conditions:
  - offgcd true
  - targetwithin{{d=4}} true
  Skills:
{o.zapowiedz("attack", [("entity.ravager.step", 1.6, 0.6), ("block.mud.break", 1.6, 0.7)] + lr("bagna:mulowiec.zamach", 1.0, 0.8),
             ["particles{p=block;m=MUD;a=14;hs=0.5;vs=0.4;y=1.6}"], "tyl", "Zamach: odskok / za plecy")}
  - particlering{{p=block;m=MUD;radius=3.5;points=30;a=1;y=0.1}} @self
  - delay {T(c['attack']['hit'] / 2)}
  - particlering{{p=block;m=MUD;radius=3.5;points=30;a=1;y=0.1}} @self
  - delay {T(c['attack']['hit']) - T(c['attack']['hit'] / 2)}
  - skill{{s=BAGNA_MULOWIEC_ZAMACH_TRAF}} @PlayersInRadius{{r=3.5}}
  - particles{{p=block;m=MUD;a=40;hs=1.6;vs=0.4;s=0.1}} @self
{o.odslona("attack", 0.5)}
""")
o.skill("BAGNA_MULOWIEC_ZAMACH_TRAF", """
  # cel = gracz w promieniu; tylko przed mobem (fieldofview zmierzony na klonie 03.10: dziala na kierunek bazy)
  TargetConditions:
  - fieldofview{a=120} true
  Skills:
  - damage{amount=@MUL_ZAMACH@}
  - throw{velocity=5;velocityY=2}
""")
o.skill("BAGNA_MULOWIEC_RZUT", f"""
  # RZUT MULEM (gracz > 6 bl.): nabranie 1 s -> pocisk w zamkniety punkt + kaluza spowolnienia 3 s (kontra: zejdz z punktu)
  Cooldown: 8
  Conditions:
  - offgcd true
  - targetwithin{{d=14}} true
  - targetnotwithin{{d=6}} true
  Skills:
{o.zapowiedz("throw", [("block.mud.place", 1.6, 0.5), ("item.bucket.fill", 1.4, 0.6)] + lr("bagna:mulowiec.jek", 1.0, 1.0),
             ["particles{p=block;m=MUD;a=10;hs=0.3;vs=0.3;y=1.2}"], "bok", "Rzut Mulem: zejdz z kregu")}
{punkt("MULOWIEC_RZUT", f"""  Skills:
  - particlering{{p=block;m=MUD;radius=1.8;points=20;a=1;y=0.1}} @self
  - delay {T(c['throw']['hit']) + 6}
  - damage{{amount=@MUL_RZUT@}} @PlayersInRadius{{r=1.8}}
  - particles{{p=block;m=MUD;a=30;hs=1.2;vs=0.05;s=0.02}} @self
  - potion{{type=SLOWNESS;duration=25;level=2;hasParticles=false}} @PlayersInRadius{{r=1.8}}
  - delay 20
  - particles{{p=block;m=MUD;a=20;hs=1.2;vs=0.05;s=0.02}} @self
  - potion{{type=SLOWNESS;duration=25;level=2;hasParticles=false}} @PlayersInRadius{{r=1.8}}
  - delay 20
  - particles{{p=block;m=MUD;a=14;hs=1.2;vs=0.05;s=0.02}} @self
  - potion{{type=SLOWNESS;duration=25;level=2;hasParticles=false}} @PlayersInRadius{{r=1.8}}
""")}
  - delay {T(c['throw']['hit'])}
  - sound{{s=entity.drowned.shoot;v=1.4;p=0.6;sc=HOSTILE}} @self
  - particleline{{p=block;m=MUD;a=2;distanceBetween=0.5}} @target
{o.odslona("throw")}
""")
punkt("MULOWIEC_SLAD", """  Skills:
  - particles{p=block;m=MUD;a=14;hs=0.8;vs=0.05;s=0.02} @self
  - potion{p_slow} @PlayersInRadius{r=1.6}
  - delay 40
  - particles{p=block;m=MUD;a=10;hs=0.8;vs=0.05;s=0.02} @self
  - potion{p_slow} @PlayersInRadius{r=1.6}
  - delay 40
  - particles{p=block;m=MUD;a=6;hs=0.8;vs=0.05;s=0.02} @self
  - potion{p_slow} @PlayersInRadius{r=1.6}
  - delay 40
  - particles{p=block;m=MUD;a=4;hs=0.8;vs=0.05;s=0.02} @self
  - potion{p_slow} @PlayersInRadius{r=1.6}
""".replace("{p_slow}", "{type=SLOWNESS;duration=25;level=1;hasParticles=false}"))
o.klatki({"attack": [("entity.player.attack.sweep", 1.4, 0.6)] + lr("bagna:mulowiec.trafienie", 1.0, 1.0),
          "throw": lr("bagna:mulowiec.trafienie", 0.8, 1.2)})
SK_MUL = o.sk

# =====================================================================================================  3 KOSCIEJ (L002, dystans)
o = Mob("kosciej"); c = o.cz
MOBS.append(f"""
BAGNA_KOSCIEJ:
  Type: BOGGED
  Display: '&2Kościej Bagienny'
  Health: @KOS_HP@
  Damage: @KOS_DMG@
  Faction: topiel
{opcje(FollowRange=20, MovementSpeed=0, KnockbackResistance=1)}{lup(("PROF_BEAST_BONE", 1, 0.5))}{AI_STOI}{CELE}  Skills:
{naglowek_modelu(o, melee=False)}  # wieza: bez vanillowego luku (AI wyczyszczone), strzela tylko zapowiedzianym Strzalem
  - skill{{s=BAGNA_KOSCIEJ_ODSKOK}} @target ~onTimer:10
  - skill{{s=BAGNA_KOSCIEJ_STRZAL}} @target ~onTimer:20
{exp(20, 5)}""")
o.skill("BAGNA_KOSCIEJ_STRZAL", f"""
  # STRZAL ZATRUTY: namierzanie 1.2 s (turkusowa linia do ZAMKNIETEGO punktu) -> 2 strzaly w punkt (oslona dziala).
  Cooldown: 4
  Conditions:
  - offgcd true
  - targetwithin{{d=20}} true
  Skills:
{o.zapowiedz("shoot", [("block.amethyst_block.resonate", 1.8, 0.6), ("item.crossbow.loading_middle", 1.4, 1.0)],
             ["particles{p=enchanted_hit;a=8;hs=0.2;vs=0.2;y=1.7}"], "bok", "Strzal: krok w bok / oslona")}
{punkt("KOSCIEJ_STRZAL", f"""  # v7 (recenzja): oslona - znacznik bije TYLKO na sygnal STRZAL; Kosciej wysyla go, gdy w chwili strzalu widzi cel
  # (warunek targetinlineofsight; lineofsight jako TargetCondition zwracal zawsze false - klon 03.10)
  Skills:
  - particles{{p={TURKUS};size=1;a=12;hs=0.4;vs=0.05;y=0.1}} @self
  - delay {T(c['shoot']['hit']) + 12}
""")}
  - particleline{{p={TURKUS};size=0.7;a=1;distanceBetween=0.6}} @target
  - delay {T(c['shoot']['windup'] / 2)}
  - particleline{{p={TURKUS};size=0.9;a=1;distanceBetween=0.5}} @target
  - delay {T(c['shoot']['hit']) - T(c['shoot']['windup'] / 2)}
  # strzal "hit-scan" w zamkniety punkt (znacznik); smuga do biezacej pozycji celu tylko jako obraz
  - sound{{s=entity.skeleton.shoot;v=1.4;p=0.8;sc=HOSTILE}} @self
  - skill{{s=BAGNA_KOSCIEJ_PUSZCZENIE}} @self
  - delay 8
  - sound{{s=entity.arrow.shoot;v=1.2;p=1.3;sc=HOSTILE}} @self
{o.odslona("shoot")}
""")
o.skill("BAGNA_KOSCIEJ_PUSZCZENIE", """
  Conditions:
  - targetinlineofsight true
  Skills:
  # signal do znacznika (ARMOR_STAND) nie dochodzil, setvariable target.* na znaczniku tez nie (klon 03.10) ->
  # flaga swiata na 4 ticki; znaczniki sprawdzaja ja co 2 ticki. Dwa Koscieje strzelajace w tej samej chwili = oba punkty bija.
  - setvariable{var=world.bagna_kos_strzal;val=1;type=INTEGER} @self
  - delay 4
  - setvariable{var=world.bagna_kos_strzal;val=0;type=INTEGER} @self
""")
o.skill("BAGNA_KOSCIEJ_SALWA", """
  # rzucajacy = znacznik punktu; 2 trafienia w punkt (strzal + dobicie po 0,4 s)
  Skills:
  - setvariable{var=caster.strzal;val=2;type=INTEGER} @self
  - skill{s=BAGNA_KOSCIEJ_TRAF} @PlayersInRadius{r=1.3}
  - delay 8
  - skill{s=BAGNA_KOSCIEJ_DOBICIE} @PlayersInRadius{r=1.3}
""")
o.skill("BAGNA_KOSCIEJ_TRAF", """
  # bez TargetConditions lineofsight: na klonie 03.10 odrzucal gracza na otwartej arenie (0 trafien). Oslona dziala ruchem:
  # kto schowal sie za filar, zszedl z zamknietego punktu.
  Skills:
  - damage{amount=@KOS_STRZAL@}
  - potion{type=POISON;duration=60;level=0}
  - sound{s=entity.arrow.hit_player;v=1;p=1;sc=HOSTILE}
""")
o.skill("BAGNA_KOSCIEJ_DOBICIE", """
  Skills:
  - damage{amount=@KOS_DOBICIE@}
""")
o.skill("BAGNA_KOSCIEJ_ODSKOK", f"""
  # ODSKOK W MUL (gracz < 4 bl.): zapada sie 0.5 s -> wyskakuje do tylu (kontra: dopasc zanim znow naciagnie)
  Cooldown: 8
  Conditions:
  - offgcd true
  - targetwithin{{d=4}} true
  Skills:
{o.zapowiedz("sink", [("block.mud_bricks.break", 1.6, 0.5), ("entity.skeleton.step", 1.4, 0.5)],
             ["particles{p=block;m=MUD;a=20;hs=0.4;vs=0.1}"], "naprzod", "Odskok: dopasc")}
  - delay {T(c['sink']['windup'])}
  - lunge{{velocity=-1.2;velocityY=0.35}} @target
  - {o.st("emerge", 0, 2)} @self
  - particles{{p=block;m=MUD;a=20;hs=0.4;vs=0.2}} @self
  - delay 12
  - lockmodel{{l=false}} @self
""")
o.klatki({"shoot": [], "attack": []})
SK_KOS = o.sk

# =====================================================================================================  4 SZEPTUCHA (L002, zaklinacz)
o = Mob("szeptucha"); c = o.cz
SOJUSZ = "BAGNA_MULOWIEC,BAGNA_PIJAWKA,BAGNA_KOSCIEJ,BAGNA_SZEPTUCHA,BAGNA_KOKONIARA,BAGNA_KOKONIATKO"
MOBS.append(f"""
BAGNA_SZEPTUCHA:
  Type: VINDICATOR
  Display: '&5Szeptucha'
  Health: @SZE_HP@
  Damage: @SZE_DMG@
  Faction: topiel
{opcje(FollowRange=16, MovementSpeed=0.0)}{lup(("PROF_MEDICINAL_HERB", "1-2", 0.6), ("PROF_REDCAP", 1, 0.4))}{AI_STOI}{CELE}  Skills:
{naglowek_modelu(o, melee=False)}  - setvariable{{var=caster.wywar;val=0;type=INTEGER}} @self ~onSpawn
  - skill{{s=BAGNA_SZEPTUCHA_SZEPT}} @self ~onTimer:10
  - skill{{s=BAGNA_SZEPTUCHA_WYWAR}} @self ~onTimer:40
  - skill{{s=BAGNA_SZEPTUCHA_FLASZKA}} @target ~onTimer:20
  # przerwanie kanalu: 3 ciosy w trakcie (wzorzec L002 Szeptucha / Kora - ~onDamaged nie lapie /damage, bot musi bic sam)
  - variableadd{{var=caster.wywar_ciosy;amount=1}} @self ~onDamaged
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=3}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=4}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=5}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=6}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=7}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=8}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=9}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=10}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=11}}
  - skill{{s=BAGNA_SZEPTUCHA_PRZERWANIE}} @self ~onTimer:5 ?variableequals{{var=caster.wywar;value=1}} ?variableequals{{var=caster.wywar_ciosy;value=12}}
{exp(40, 8)}""")
o.skill("BAGNA_SZEPTUCHA_WYWAR", f"""
  # WYWAR: kanal 3 s (chochla w kotle, bulgot) -> oslona sojusznikow r=8; 3 ciosy przerywaja i przewracaja kociol
  Cooldown: 12
  Conditions:
  - offgcd true
  - playerwithin{{d=14}} true
  Skills:
  - setvariable{{var=caster.wywar;val=1;type=INTEGER}} @self
  - setvariable{{var=caster.wywar_ciosy;val=0;type=INTEGER}} @self
{o.zapowiedz("channel", [("block.brewing_stand.brew", 1.6, 0.8), ("entity.witch.celebrate", 1.4, 0.6)],
             ["particles{p=witch;a=30;hs=0.6;vs=1;y=1.5}"], "bij", "Wywar: bij 3x", czas=3.0)}
  - delay 20
  - particles{{p=witch;a=30;hs=0.6;vs=1;y=1.5}} @self ?variableequals{{var=caster.wywar;value=1}}
  - delay 20
  - particles{{p=witch;a=30;hs=0.6;vs=1;y=1.5}} @self ?variableequals{{var=caster.wywar;value=1}}
  - delay 20
  - skill{{s=BAGNA_SZEPTUCHA_WYWAR_DZIALA}} @self ?variableequals{{var=caster.wywar;value=1}}
  - setvariable{{var=caster.wywar;val=0;type=INTEGER}} @self
  - {o.zdejmij("channel")} @self
  - lockmodel{{l=false}} @self
""")
o.skill("BAGNA_SZEPTUCHA_WYWAR_DZIALA", f"""
  Skills:
  - potion{{type=ABSORPTION;duration=200;level=2;hasParticles=true}} @MobsInRadius{{r=8;types={SOJUSZ}}}
  - particles{{p=happy_villager;a=40;hs=4;vs=1;y=1}} @self
  - message{{m="<dark_purple>Szeptucha</dark_purple> <gray>dokończyła wywar - jej sługi obrosły mułem.</gray>"}} @PlayersInRadius{{r=20}}
""")
o.skill("BAGNA_SZEPTUCHA_PRZERWANIE", f"""
  Skills:
  - setvariable{{var=caster.wywar;val=0;type=INTEGER}} @self
  - {o.st("interrupted", 1, 2, ";f=true")} @self
  - sound{{s=block.glass.break;v=1.5;p=0.8;sc=HOSTILE}} @self
  - particles{{p=witch;a=60;hs=0.8;vs=1;s=0.2;y=1.5}} @self
  - delay {T(c['interrupted']['hit'])}
  - particles{{p=item_slime;a=40;hs=1.2;vs=0.1}} @self
  - damage{{amount=4}} @self
  - potion{{type=POISON;duration=80;level=0}} @self
  - potion{{type=POISON;duration=60;level=0}} @PlayersInRadius{{r=2.5}}
  - message{{m="<gold>Wywar przerwany!</gold> <gray>Kocioł się przewrócił.</gray>"}} @PlayersInRadius{{r=20}}
  - delay {T(c['interrupted']['recover'])}
  - lockmodel{{l=false}} @self
""")
o.skill("BAGNA_SZEPTUCHA_FLASZKA", f"""
  # FLASZKA: zamach 0.8 s -> kaluza trucizny w zamknietym punkcie (kontra: zejdz z kregu)
  Cooldown: 7
  Conditions:
  - offgcd true
  - targetwithin{{d=12}} true
  - targetnotwithin{{d=3}} true
  Skills:
{o.zapowiedz("throw", [("entity.witch.drink", 1.6, 0.6), ("item.bottle.fill", 1.4, 0.8)],
             ["particles{p=witch;a=10;hs=0.3;vs=0.3;y=1.6}"], "bok", "Flaszka: zejdz z kregu")}
{punkt("SZEPTUCHA_FLASZKA", f"""  Skills:
  - particlering{{p={TURKUS};size=1;radius=2;points=20;a=1;y=0.1}} @self
  - delay {T(c['throw']['hit']) + 6}
  - particles{{p=item_slime;a=40;hs=1.5;vs=0.2}} @self
  - damage{{amount=@SZE_FLASZKA@}} @PlayersInRadius{{r=2}}
  - potion{{type=POISON;duration=60;level=0}} @PlayersInRadius{{r=2}}
  # kaluza zostaje 3 s (karta)
  - delay 20
  - particles{{p=item_slime;a=25;hs=1.5;vs=0.1}} @self
  - potion{{type=POISON;duration=40;level=0}} @PlayersInRadius{{r=2}}
  - delay 20
  - particles{{p=item_slime;a=20;hs=1.5;vs=0.1}} @self
  - potion{{type=POISON;duration=40;level=0}} @PlayersInRadius{{r=2}}
  - delay 20
  - particles{{p=item_slime;a=15;hs=1.5;vs=0.1}} @self
""")}
  - delay {T(c['throw']['hit'])}
  - sound{{s=entity.witch.throw;v=1.4;p=1.0;sc=HOSTILE}} @self
  - particleline{{p=witch;a=1;distanceBetween=0.5}} @target
{o.odslona("throw")}
""")
o.skill("BAGNA_SZEPTUCHA_SZEPT", f"""
  # SZEPT (gracz < 3 bl.): pochylenie 0.6 s -> odepchniecie + spowolnienie r=3.5 (kara za stanie przy niej; kontra: odskok)
  Cooldown: 8
  Conditions:
  - offgcd true
  - playerwithin{{d=3}} true
  Skills:
{o.zapowiedz("push", [("entity.allay.ambient_without_item", 1.6, 0.5), ("particle.soul_escape", 1.6, 0.6)],
             ["particles{p=sculk_soul;a=10;hs=0.4;vs=0.3;y=1.4}"], "tyl", "Szept: odskok")}
  - delay {T(c['push']['hit'])}
  - damage{{amount=@SZE_SZEPT@}} @PlayersInRadius{{r=3.5}}
  - throw{{velocity=8;velocityY=3}} @PlayersInRadius{{r=3.5}}
  - potion{{type=SLOWNESS;duration=40;level=1;hasParticles=false}} @PlayersInRadius{{r=3.5}}
{o.odslona("push")}
""")
o.klatki({"throw": [], "push": [("entity.breeze.charge", 1.0, 0.6)], "interrupted": [("block.decorated_pot.shatter", 1.4, 0.7)]})
SK_SZE = o.sk

# =====================================================================================================  5 KOKONIARA (L002, elita)
o = Mob("kokoniara"); c = o.cz
MOBS.append(f"""
BAGNA_KOKONIARA:
  Type: SPIDER
  Display: '&f&lKokoniara'
  Health: @KOK_HP@
  Damage: @KOK_DMG@
  Armor: @KOK_ARM@
  Faction: topiel
{opcje(FollowRange=14, MovementSpeed=0.28, KnockbackResistance=0.5, AlwaysShowName="true")}  BossBar:
    Enabled: true
    Title: '&f&lKokoniara'
    Range: 24
    Color: WHITE
    Style: SEGMENTED_6
{lup(("PROF_SINEW", 2, 1), ("PROF_VENOM_SAC", 1, 0.6))}{AI_WRECZ}{CELE}  Skills:
{naglowek_modelu(o)}  - CancelEvent ~onAttack {round(1 - STATY["CIOS_KOK"], 2)}
{WODA}  - addtag{{tag=bagna_elita}} @self ~onSpawn
  # Ugryzienie: melee + trucizna 20% (karta), 1 s (v7: 2 s + kokon = ticki trucizny to polowa strat uwaznego)
  - potion{{type=POISON;duration=20;level=0}} @target ~onAttack 0.2
  - skill{{s=BAGNA_KOKONIARA_KOKON}} @target ~onTimer:20
  - skill{{s=BAGNA_KOKONIARA_SKOK}} @target ~onTimer:20
  - skill{{s=BAGNA_KOKONIARA_WYLEG}} @self ~onTimer:20 ?healthpercent{{p=<50%}}
{''.join(f"  - {snd(*d)} @self ~onDeath" + chr(10) for d in [("entity.spider.death", 1.3, 0.5)] + lr("bagna:kokoniara.smierc", 1.2, 1.0))}{exp(120, 25)}""")
o.skill("BAGNA_KOKONIARA_KOKON", f"""
  # KOKON: odwlok do przodu, przedza swieci 1 s -> pocisk w zamkniety punkt; trafiony = kokon 4 s (3 ciosy rozbijaja)
  Cooldown: 10
  Conditions:
  - offgcd true
  - targetwithin{{d=10}} true
  Skills:
{o.zapowiedz("spin", [("entity.spider.ambient", 1.6, 0.5), ("block.cobweb.place", 1.6, 0.8)] + lr("bagna:kokoniara.kokon", 0.6, 1.0),
             ["particles{p=block;m=COBWEB;a=10;hs=0.5;vs=0.3;y=0.8}"], "bok", "Kokon: unik w bok")}
{punkt("KOKONIARA_KOKON", f"""  Skills:
  - particlering{{p={TURKUS};size=0.8;radius=1.4;points=14;a=1;y=0.1}} @self
  - delay {T(c['spin']['hit']) + 4}
  - skill{{s=BAGNA_KOKONIARA_KOKON_NA}} @PlayersInRadius{{r=1.4}}
""")}
  - delay {T(c['spin']['hit'])}
  - particleline{{p=block;m=COBWEB;a=2;distanceBetween=0.5}} @target
{o.odslona("spin")}
""")
o.skill("BAGNA_KOKONIARA_KOKON_NA", """
  # cel = trafiony gracz (linie bez targetera dziedzicza go)
  Skills:
  - summon{m=BAGNA_KOKON;level=1;a=1;r=0}
  - potion{type=SLOWNESS;duration=80;level=6;hasParticles=false}
  - message{m="<white>Kokon oplata ci nogi!</white> <gray>Bij go, zanim się zaciśnie.</gray>"}
""")
o.skill("BAGNA_KOKONIARA_SKOK", f"""
  # SKOK: przysiad 0.8 s -> skok w zamkniety punkt, fala r=2.5 (kontra: unik)
  Cooldown: 7
  Conditions:
  - offgcd true
  - targetwithin{{d=9}} true
  - targetnotwithin{{d=2}} true
  Skills:
{o.zapowiedz("leap_prep", [("entity.spider.step", 1.6, 0.6), ("entity.goat.prepare_ram", 1.4, 1.4)],
             ["particles{p=angry_villager;a=6;hs=0.5;vs=0.4;y=1}"], "bok", "Skok: unik w bok", gcd_dodatek=c['leap']['dl'])}
{punkt("KOKONIARA_SKOK", f"""  Skills:
  - particlering{{p={TURKUS};size=1;radius=2.5;points=24;a=1;y=0.1}} @self
  - delay {T(c['leap_prep']['windup']) + T(c['leap']['hit'])}
  - damage{{amount=@KOK_SKOK@}} @PlayersInRadius{{r=2.5}}
  - particlering{{p=block;m=MUD;radius=2.5;points=30;a=2;y=0.1}} @self
""")}
  - skill{{s=BAGNA_KOKONIARA_LOT}} @targetlocation
  - delay {T(c['leap_prep']['windup'])}
  - {o.st("leap", 0, 2)} @self
  - delay {T(c['leap']['hit'])}
{o.odslona("leap", 0.7)}
""")
o.skill("BAGNA_KOKONIARA_LOT", f"""
  Skills:
  - delay {T(c['leap_prep']['windup'])}
  - leap{{velocity=110}} @origin
""")
o.skill("BAGNA_KOKONIARA_WYLEG", f"""
  # WYLEG przy 50%: rozpruty odwlok, 2 Kokoniatka (raz; karta 4 - v5: 4 zabijaly uwaznego, v10: 3 = uwazny 67%)
  Cooldown: 9999
  Skills:
  - lockmodel{{l=true}} @self
  - {o.st("hatch", 1, 2, ";f=true")} @self
  - potion{{type=SLOWNESS;duration={T(c['hatch']['dl'])};level=10;hasParticles=false}} @self
{''.join(f"  - {snd(*d)} @self" + chr(10) for d in [("entity.spider.hurt", 1.6, 0.5)] + lr("bagna:kokoniara.krzyk", 1.2, 1.0))}  - message{{m="<white>Kokoniara</white> <gray>rozrywa kokon na odwłoku - wysypują się młode!</gray>"}} @PlayersInRadius{{r=24}}
  - delay {T(c['hatch']['hit'])}
{''.join(f"  - {snd(*d)} @self" + chr(10) for d in lr("bagna:kokoniara.wyleg", 1.4, 1.0))}  - summon{{m=BAGNA_KOKONIATKO;level=1;a=2;r=2;yr=4;os=true}} @self
  - delay {T(c['hatch']['recover'])}
  - lockmodel{{l=false}} @self
""")
o.klatki({"spin": [("block.cobweb.break", 1.2, 0.8)], "leap": [("entity.spider.step", 1.6, 0.4)] + lr("bagna:kokoniara.ladowanie", 1.2, 1.0),
          "hatch": [], "attack": [("entity.spider.hurt", 0.8, 0.8)]})
SK_KOK = o.sk
MOBS.append("""
# kokon na graczu: bij go (3 ciosy), zanim zacisnie sie trucizna (wzorzec L002_KOKON, bez modelu - wyglad: pajeczyna na glowie)
BAGNA_KOKON:
  Type: ZOMBIE
  Display: '<white>Kokon</white> <gray>(bij 3x!)</gray>'
  Health: 200
  Options:
    Invisible: true
    NoAI: true
    Silent: true
    Collidable: false
    Despawn: persistent
    PreventOtherDrops: true
    PreventRandomEquipment: true
    PreventSunburn: true
    PreventConversion: true
    AlwaysShowName: true
  Equipment:
  - COBWEB HEAD
  Skills:
  - skill{s=BAGNA_KOKON_ZYCIE} @self ~onSpawn
  - setvariable{var=caster.ciosy;val=0;type=INTEGER} @self ~onSpawn
  - variableadd{var=caster.ciosy;amount=1} @self ~onDamaged
  - skill{s=BAGNA_KOKON_PEKA} @self ~onTimer:2 ?variableequals{var=caster.ciosy;value=3}
  - skill{s=BAGNA_KOKON_PEKA} @self ~onTimer:2 ?variableequals{var=caster.ciosy;value=4}
""")
SK_KOKON = [("BAGNA_KOKON_ZYCIE", """  Skills:
  - delay 80
  # v8: trucizna I 2 s (v7: II 3 s)
  - potion{type=POISON;duration=40;level=0} @PlayersInRadius{r=1.6}
  - command{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> run effect clear @a[distance=..1.8] minecraft:slowness"}
  - remove @self
"""), ("BAGNA_KOKON_PEKA", """  Skills:
  - particles{p=block;m=COBWEB;a=20;hs=0.4;vs=0.6;y=1} @self
  - sound{s=block.cobweb.break;v=1.4;p=0.8;sc=HOSTILE} @self
  - command{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> run effect clear @a[distance=..1.8] minecraft:slowness"}
  - remove @self
""")]

# =====================================================================================================  6 KOKONIATKO (L002, roj)
o = Mob("kokoniatko"); c = o.cz
MOBS.append(f"""
BAGNA_KOKONIATKO:
  Type: ENDERMITE
  Display: '&7Kokoniątko'
  Health: @KKT_HP@
  Damage: @KKT_DMG@
  Faction: topiel
{opcje(FollowRange=12, MovementSpeed=0.3)}{AI_WRECZ}{CELE}  Skills:
{naglowek_modelu(o)}  - CancelEvent ~onAttack {round(1 - STATY["CIOS_KKT"], 2)}
{WODA}  - skill{{s=BAGNA_KOKONIATKO_DOSKOK}} @target ~onTimer:20
  # NIC: po smierci lepka nitka (spowolnienie 2 s) - zabijanie w kupie zamyka ruch
  - summon{{m=BAGNA_P_KOKONIATKO_NIC;level=1;a=1;r=0}} @self ~onDeath
{exp(1)}""")
o.skill("BAGNA_KOKONIATKO_DOSKOK", f"""
  Cooldown: 4
  Conditions:
  - offgcd true
  - targetwithin{{d=5}} true
  - targetnotwithin{{d=1.5}} true
  Skills:
{o.zapowiedz("leap", [("entity.spider.ambient", 1.2, 1.8), ("block.cobweb.step", 1.4, 1.4)],
             ["particles{p=block;m=COBWEB;a=4;hs=0.2;vs=0.1}"], "bok", "Doskok: krok w bok")}
{punkt("KOKONIATKO_DOSKOK", f"""  Skills:
  - delay {T(c['leap']['hit'])}
  - damage{{amount=@KKT_DOSKOK@}} @PlayersInRadius{{r=1.2}}
""")}
  - skill{{s=BAGNA_KOKONIATKO_LOT}} @targetlocation
  - delay {T(c['leap']['hit'])}
{o.odslona("leap", 0.5)}
""")
o.skill("BAGNA_KOKONIATKO_LOT", f"""
  Skills:
  - delay {T(c['leap']['windup'])}
  - leap{{velocity=90}} @origin
""")
punkt("KOKONIATKO_NIC", """  Skills:
  - sound{s=block.cobweb.break;v=1.2;p=1.4;sc=HOSTILE} @self
  - particles{p=block;m=COBWEB;a=12;hs=0.6;vs=0.05} @self
  - potion{type=SLOWNESS;duration=40;level=1;hasParticles=false} @PlayersInRadius{r=1.8}
  - delay 40
""")
o.klatki({"leap": [("entity.spider.hurt", 0.8, 1.8)], "attack": []})
SK_KKT = o.sk

# =====================================================================================================  7 RECHOTNIK (Mokradla, zabolud)
o = Mob("rechotnik"); c = o.cz
MOBS.append(f"""
BAGNA_RECHOTNIK:
  Type: DROWNED
  Display: '&2Rechotnik'
  Health: @REC_HP@
  Damage: @REC_DMG@
  Faction: topiel
{opcje(despawn="true", FollowRange=16, MovementSpeed=0.24, KnockbackResistance=0.3)}{lup(("PROF_BEAST_SCRAP", 1, 0.5), ("PROF_FLAX_FIBER", 1, 0.4))}{AI_WRECZ}{CELE}  Skills:
{naglowek_modelu(o)}  - CancelEvent ~onAttack {round(1 - STATY["CIOS_REC"], 2)}
  - addtag{{tag=bagna_rechotnik}} @self ~onSpawn
{''.join(f"  - {snd(*d)} @self ~onSpawn" + chr(10) for d in lr("bagna:rechotnik.wynurzenie", 1.2, 1.0))}{''.join(f"  - {snd(*d)} @self ~onDeath" + chr(10) for d in [("entity.frog.death", 1.2, 0.6)] + lr("bagna:rechotnik.smierc", 1.0, 1.0))}  - skill{{s=BAGNA_RECHOTNIK_JEZYK}} @target ~onTimer:20
{exp(45, 10)}""")
o.skill("BAGNA_RECHOTNIK_JEZYK", f"""
  # JEZYK: rechot nadyma worek 1 s (linia do ZAMKNIETEGO punktu) -> jezyk przyciaga stojacych w punkcie -> PLASNIECIE r=2
  # kontra: wyjsc z linii; po przyciagnieciu - odskoczyc 2 bl. przed plasnieciem
  Cooldown: 8
  Conditions:
  - offgcd true
  - targetwithin{{d=7}} true
  Skills:
{o.zapowiedz("croak", [("entity.frog.ambient", 1.8, 0.6), ("entity.frog.long_jump", 1.4, 0.5)] + lr("bagna:rechotnik.rechot", 1.0, 0.9),
             ["particles{p=" + TURKUS + ";size=1;a=8;hs=0.3;vs=0.2;y=0.9}"], "tyl", "Jezyk: wyjdz z punktu do plasniecia",
             gcd_dodatek=c['tongue']['dl'] + c['slam']['dl'], ms_reakcji=int(1000 * (c['croak']['windup'] + c['tongue']['dl'] + c['slam']['hit'])) + 300)}
{punkt("RECHOTNIK_JEZYK", f"""  # v7 (recenzja): jezyk PRZYCIAGA - trafiony w zamknietym punkcie laduje 1,4 bl. przed pyskiem (tp po tagu moba),
  # unieruchomiony do plasniecia; plasniecie bije pod Rechotnikiem (linia w skillu moba). pull z punktu ciagnalby do znacznika.
  Skills:
  - delay {T(c['croak']['windup']) + T(c['tongue']['hit'])}
  - command{{c="execute as <target.uuid> at @s at @e[tag=bagna_rechotnik,distance=..9,sort=nearest,limit=1] run tp @s ^ ^ ^1.4"}} @PlayersInRadius{{r=1.6}}
  - potion{{type=SLOWNESS;duration={T(c['tongue']['dl'] - c['tongue']['hit'] + c['slam']['hit']) + 4};level=10;hasParticles=false}} @PlayersInRadius{{r=1.6}}
  - damage{{amount=2}} @PlayersInRadius{{r=1.6}}
  - particles{{p={TURKUS};size=1;a=12;hs=0.4;vs=0.6;y=1}} @self
""")}
  - particleline{{p={TURKUS};size=0.8;a=1;distanceBetween=0.5}} @target
  - delay {T(c['croak']['windup'])}
  - {o.st("tongue", 0, 1)} @self
  - delay {T(c['tongue']['hit'])}
  - sound{{s=entity.frog.tongue;v=1.6;p=0.8;sc=HOSTILE}} @self
  - delay {T(c['tongue']['dl'] - c['tongue']['hit'])}
  - {o.st("slam", 0, 2)} @self
  - delay {T(c['slam']['hit'])}
  - damage{{amount=@REC_SLAM@}} @PlayersInRadius{{r=2}}
  - particlering{{p=splash;radius=2;points=24;a=2;y=0.2}} @self
{o.odslona("slam")}
""")
o.klatki({"tongue": lr("bagna:rechotnik.jezyk", 1.2, 1.0), "slam": [("entity.player.splash", 1.4, 0.6)] + lr("bagna:rechotnik.trafienie", 1.0, 0.8),
          "attack": [("entity.frog.hurt", 0.8, 0.7)]})
SK_REC = o.sk

# =====================================================================================================  8 SLIMAK (Mokradla, tank)
o = Mob("slimak"); c = o.cz
MOBS.append(f"""
BAGNA_SLIMAK:
  Type: ZOMBIE
  Display: '&6Ślimak Bagienny'
  Health: @SLI_HP@
  Damage: @SLI_DMG@
  Armor: @SLI_ARM@
  Faction: topiel
{opcje(despawn="true", FollowRange=12, MovementSpeed=0.12, KnockbackResistance=1, PreventConversion="true")}{lup(("PROF_CLAM", 1, 0.5), ("PROF_CLAY_LUMP", 1, 0.5))}{AI_WRECZ}{CELE}  Skills:
{naglowek_modelu(o)}  - CancelEvent ~onAttack {round(1 - STATY["CIOS_SLI"], 2)}
  - setvariable{{var=caster.ciosy;val=0;type=INTEGER}} @self ~onSpawn
  # licznik ciosow OD PRZODU (fieldofview = NOWY warunek - test jednorazowka); od tylu (pekniecie) brak schronienia
  - skill{{s=BAGNA_SLIMAK_CIOS}} @trigger ~onDamaged
  - skill{{s=BAGNA_SLIMAK_PANCERZ}} @trigger ~onDamaged
  - skill{{s=BAGNA_SLIMAK_SCHRONIENIE}} @self ~onTimer:5 ?variableequals{{var=caster.ciosy;value=4}}
  - skill{{s=BAGNA_SLIMAK_SCHRONIENIE}} @self ~onTimer:5 ?variableequals{{var=caster.ciosy;value=5}}
  - skill{{s=BAGNA_SLIMAK_SCHRONIENIE}} @self ~onTimer:5 ?variableequals{{var=caster.ciosy;value=6}}
  - summon{{m=BAGNA_P_SLIMAK_SLUZ;level=1;a=1;r=0}} @self ~onTimer:40
{exp(60, 12)}""")
o.skill("BAGNA_SLIMAK_CIOS", """
  TargetConditions:
  - fieldofview{a=180} true
  Skills:
  - variableadd{var=caster.ciosy;amount=1} @self
""")
o.skill("BAGNA_SLIMAK_SCHRONIENIE", f"""
  # SCHRONIENIE: chowa sie 3 s (Resistance IV ~80%), potem drzenie skorupy 0.8 s -> WYRZUT SLUZU r=4 (kontra: odejdz >4 bl.)
  Cooldown: 6
  Conditions:
  - offgcd true
  Skills:
  - setvariable{{var=caster.ciosy;val=0;type=INTEGER}} @self
  - gcd{{ticks={T(c['hide']['dl'] + 3 + c['emerge_spit']['dl'] + 0.3)}}}
  - lockmodel{{l=true}} @self
  - {o.st("hide", 1, 1)} @self
  - sound{{s=entity.turtle.egg_crack;v=1.6;p=0.6;sc=HOSTILE}} @self
  - sound{{s=block.shroomlight.place;v=1.4;p=0.8;sc=HOSTILE}} @self
  - potion{{type=RESISTANCE;duration={T(c['hide']['dl'] + 3)};level=3;hasParticles=false}} @self
  - potion{{type=SLOWNESS;duration={T(c['hide']['dl'] + 3 + c['emerge_spit']['dl'])};level=10;hasParticles=false}} @self
  - delay {T(c['hide']['dl'])}
  - {o.st("hidden", 0, 1)} @self
  - delay 60
  - {o.zdejmij("hidden")} @self
  - skill{{s=BAGNA_SLIMAK_WYRZUT}} @self
""")
o.skill("BAGNA_SLIMAK_WYRZUT", f"""
  Skills:
{o.zapowiedz("emerge_spit", [("entity.slime.attack", 1.6, 0.5), ("block.honey_block.slide", 1.6, 0.8)],
             ["particles{p=item_slime;a=20;hs=0.8;vs=0.4;y=0.8}"], "tyl", "Wyrzut Sluzu: odejdz >4")}
  - particlering{{p=item_slime;radius=4;points=36;a=1;y=0.1}} @self
  - delay {T(c['emerge_spit']['hit'])}
  - skill{{s=BAGNA_SLIMAK_WYRZUT_TRAF}} @PlayersInRadius{{r=4}}
  - particles{{p=item_slime;a=80;hs=2.5;vs=0.4}} @self
{o.odslona("emerge_spit")}
""")
o.skill("BAGNA_SLIMAK_WYRZUT_TRAF", """
  # stozek 90 st. przed slimakiem (karta); za nim i z boku bezpiecznie
  TargetConditions:
  - fieldofview{a=90} true
  Skills:
  - damage{amount=@SLI_SLUZ@}
  - potion{type=SLOWNESS;duration=60;level=2;hasParticles=false}
""")
o.skill("BAGNA_SLIMAK_PANCERZ", """
  # skorupa od przodu: kazdy cios z przodu oddaje 2 HP (zamiast Armor dzialajacego z kazdej strony); od tylu pekniecie = pelne obrazenia
  TargetConditions:
  - fieldofview{a=180} true
  Skills:
  - heal{amount=2} @self
""")
punkt("SLIMAK_SLUZ", """  Skills:
  - particles{p=item_slime;a=10;hs=0.8;vs=0.05;s=0.02} @self
  - potion{type=SLOWNESS;duration=25;level=0;hasParticles=false} @PlayersInRadius{r=1.6}
  - delay 40
  - particles{p=item_slime;a=8;hs=0.8;vs=0.05;s=0.02} @self
  - potion{type=SLOWNESS;duration=25;level=0;hasParticles=false} @PlayersInRadius{r=1.6}
""")
o.klatki({"emerge_spit": [("block.honey_block.break", 1.4, 0.6)], "attack": [("entity.turtle.hurt", 1.0, 0.6)]})
SK_SLI = o.sk

# =====================================================================================================  9 OGNIK (Mokradla, wabik)
o = Mob("ognik"); c = o.cz
MOBS.append(f"""
BAGNA_OGNIK:
  Type: PARROT
  Display: '&bBłędny Ognik'
  Health: @OGN_HP@
  Damage: 0
  Faction: topiel
{opcje(despawn="true", FollowRange=16, MovementSpeed=0.3, NoGravity="true")}{lup(("PROF_WATER_ESSENCE", 1, 0.08))}{AI_STOI}{CELE}  Skills:
{naglowek_modelu(o, melee=False)}  - skill{{s=BAGNA_OGNIK_UCIECZKA}} @target ~onTimer:10
  - skill{{s=BAGNA_OGNIK_PLOMIEN}} @target ~onTimer:20
  - skill{{s=BAGNA_OGNIK_PODLOT}} @target ~onTimer:40
  - sound{{s=block.amethyst_block.chime;v=1;p=1.4;sc=HOSTILE}} @self ~onTimer:80
  - sound{{s=entity.allay.ambient_with_item;v=0.8;p=0.6;sc=HOSTILE}} @self ~onTimer:80
  - sound{{s=block.fire.extinguish;v=1;p=1.4;sc=HOSTILE}} @self ~onDeath
  # zlapany w miejscu SUCHYM (rzucajacy nie w cieczy) - dodatkowa esencja dla zabojcy (25%)
  - command{{c="mi give MATERIAL PROF_WATER_ESSENCE <trigger.name> 1"}} @trigger ~onDeath ?!inliquid ?~isplayer 0.25
{exp(30, 6)}""")
o.skill("BAGNA_OGNIK_UCIECZKA", f"""
  # UCIECZKA: gracz < 4 bl. i BIEGNIE -> odlot (karta: goniacego wabi; stojacego/idacego atakuje Plomieniem)
  # v1: uciekal zawsze -> Plomien nigdy nie odpalal (0 obrazen na arenie)
  Cooldown: 3
  Conditions:
  - offgcd true
  - targetwithin{{d=4}} true
  TargetConditions:
  - sprinting true
  Skills:
  - {o.st("flee", 0, 2)} @self
  - lunge{{velocity=-1.1;velocityY=0.15}} @target
""")
o.skill("BAGNA_OGNIK_PODLOT", """
  # wabik: gracz 6-14 bl. -> podlatuje
  Cooldown: 4
  Conditions:
  - offgcd true
  - targetwithin{d=14} true
  - targetnotwithin{d=6} true
  Skills:
  - lunge{velocity=0.7;velocityY=0.05} @target
""")
o.skill("BAGNA_OGNIK_PLOMIEN", f"""
  # ZIMNY PLOMIEN: rdzen puchnie 1 s -> wybuch r=2.5 + spowolnienie, potem migniecie (kontra: odejdz)
  # baza PARROT+NoGravity (klon 03.10: VEX przenika bloki i zapadl sie w grunt; papuga zatrzymuje sie na filarze)
  Cooldown: 6
  Conditions:
  - offgcd true
  - targetwithin{{d=3.5}} true
  TargetConditions:
  - lineofsight true
  Skills:
{o.zapowiedz("charge", [("block.respawn_anchor.charge", 1.6, 1.5), ("block.beacon.power_select", 1.2, 1.6)],
             ["particles{p=soul_fire_flame;a=10;hs=0.3;vs=0.3;y=0.5}"], "tyl", "Zimny Plomien: odejdz",
             gcd_dodatek=c['explode']['dl'] + c['blink']['dl'])}
  - particlering{{p=soul_fire_flame;radius=3;points=28;a=1;y=0.1}} @self
  - delay {T(c['charge']['windup'])}
  - {o.st("explode", 0, 1, ";f=true")} @self
  - delay {T(c['explode']['hit'])}
  - sound{{s=entity.blaze.shoot;v=1.6;p=0.6;sc=HOSTILE}} @self
  - sound{{s=block.fire.extinguish;v=1.4;p=0.8;sc=HOSTILE}} @self
  - damage{{amount=@OGN_PLOMIEN@}} @PlayersInRadius{{r=3}}
  - potion{{type=SLOWNESS;duration=60;level=1;hasParticles=false}} @PlayersInRadius{{r=3}}
  - particles{{p=soul_fire_flame;a=60;hs=1.5;vs=1;s=0.1}} @self
  - delay {T(c['explode']['recover'])}
  - {o.st("blink", 0, 1)} @self
  - delay {T(c['blink']['hit'])}
  - particles{{p=flash;a=1}} @self
  # teleport z rozrzutem: NOWE atrybuty (sh/sv) - test jednorazowka; zapas: lunge
  - teleport{{sh=5;sv=0}} @selflocation
  - lockmodel{{l=false}} @self
""")
o.klatki({"explode": [], "blink": [("entity.enderman.teleport", 0.8, 1.6)], "attack": []})
SK_OGN = o.sk

# =====================================================================================================  10 KROL ROPUCH (Mokradla, elita/boss wydarzenia)
o = Mob("krol_ropuch"); c = o.cz
MOBS.append(f"""
BAGNA_KROL_ROPUCH:
  Type: ZOMBIE
  Display: '&2&lKról Ropuch'
  Health: @KRO_HP@
  Damage: @KRO_DMG@
  Armor: @KRO_ARM@
  Faction: topiel
{opcje(FollowRange=24, MovementSpeed=0.2, KnockbackResistance=0.9, PreventConversion="true", AlwaysShowName="true")}  BossBar:
    Enabled: true
    Title: '&2&lKról Ropuch'
    Range: 40
    Color: GREEN
    Style: SEGMENTED_10
{lup(("PROF_WATER_ESSENCE", "1-2", 1), ("PROF_CLAM", 3, 1), ("PROF_PEARL", 1, 0.3))}{AI_WRECZ}{CELE}  Skills:
{naglowek_modelu(o)}  - CancelEvent ~onAttack {round(1 - STATY["CIOS_KRO"], 2)}
  - setvariable{{var=caster.gniew;val=0;type=INTEGER}} @self ~onSpawn
  - addtag{{tag=bagna_krol}} @self ~onSpawn
  - skill{{s=BAGNA_KROL_ROPUCH_POLKNIECIE}} @target ~onTimer:20 ?variableequals{{var=caster.gniew;value=0}}
  - skill{{s=BAGNA_KROL_ROPUCH_POLKNIECIE_G}} @target ~onTimer:20 ?variableequals{{var=caster.gniew;value=1}}
  - skill{{s=BAGNA_KROL_ROPUCH_JEZYK}} @target ~onTimer:20 ?variableequals{{var=caster.gniew;value=0}}
  - skill{{s=BAGNA_KROL_ROPUCH_JEZYK_G}} @target ~onTimer:20 ?variableequals{{var=caster.gniew;value=1}}
  - skill{{s=BAGNA_KROL_ROPUCH_SKOK}} @target ~onTimer:20 ?variableequals{{var=caster.gniew;value=0}}
  - skill{{s=BAGNA_KROL_ROPUCH_SKOK_G}} @target ~onTimer:20 ?variableequals{{var=caster.gniew;value=1}}
  - skill{{s=BAGNA_KROL_ROPUCH_WEZWANIE}} @self ~onTimer:40 ?healthpercent{{p=<75%}}
  # Jadowite Brodawki (<50%): trafiony OD TYLU pryska trucizna na bijacego (fieldofview - test jednorazowka)
  - skill{{s=BAGNA_KROL_ROPUCH_BRODAWKI}} @trigger ~onDamaged ?healthpercent{{p=<50%}}
  - skill{{s=BAGNA_KROL_ROPUCH_GNIEW}} @self ~onTimer:20 ?healthpercent{{p=<25%}}
  - variableadd{{var=caster.paszcza_ciosy;amount=1}} @self ~onDamaged ?variableequals{{var=world.bagna_paszcza;value=1}}
  - skill{{s=BAGNA_KROL_ROPUCH_UWOLNIENIE}} @self ~onTimer:4 ?variableequals{{var=world.bagna_paszcza;value=1}} ?variableequals{{var=caster.paszcza_ciosy;value=5}}
  - skill{{s=BAGNA_KROL_ROPUCH_UWOLNIENIE}} @self ~onTimer:4 ?variableequals{{var=world.bagna_paszcza;value=1}} ?variableequals{{var=caster.paszcza_ciosy;value=6}}
  - skill{{s=BAGNA_KROL_ROPUCH_UWOLNIENIE}} @self ~onTimer:4 ?variableequals{{var=world.bagna_paszcza;value=1}} ?variableequals{{var=caster.paszcza_ciosy;value=7}}
  - skill{{s=BAGNA_KROL_ROPUCH_UWOLNIENIE}} @self ~onTimer:4 ?variableequals{{var=world.bagna_paszcza;value=1}} ?variableequals{{var=caster.paszcza_ciosy;value=8}}
  - skill{{s=BAGNA_KROL_ROPUCH_UWOLNIENIE}} @self ~onTimer:4 ?variableequals{{var=world.bagna_paszcza;value=1}} ?variableequals{{var=caster.paszcza_ciosy;value=9}}
  - skill{{s=BAGNA_KROL_ROPUCH_UWOLNIENIE}} @self ~onTimer:4 ?variableequals{{var=world.bagna_paszcza;value=1}} ?variableequals{{var=caster.paszcza_ciosy;value=10}}
  - sound{{s=entity.ravager.death;v=1.4;p=1.4;sc=HOSTILE}} @self ~onDeath
  - command{{c="mmocore admin exp give <trigger.name> hunting 40"}} @trigger ~onDeath ?~isplayer
  - command{{c="mmocore admin exp give <target.name> main 300"}} @PlayersInRadius{{r=40}} ~onDeath
""")
def krol_warianty(g):
    """g=1: Gniew (<25%) - zapowiedzi krotsze o 20% (czas i predkosc animacji sp=1.25)"""
    k = 0.8 if g else 1.0
    sfx = "_G" if g else ""
    sp = ";sp=1.25" if g else ""
    sw, tw, lw = c['swallow_prep']['windup'] * k, c['tongue']['windup'] * k, c['leap_prep']['windup'] * k
    o.skill(f"BAGNA_KROL_ROPUCH_POLKNIECIE{sfx}", f"""
  # POLKNIECIE: rozwarcie paszczy {sw:.2f} s (linia do zamknietego punktu) -> polyka stojacego w punkcie na 3 s, potem wypluwa
  # (v1: uwolnienie przez druzyne - TODO po tescie; kontra: wyjsc z linii)
  Cooldown: 14
  Conditions:
  - offgcd true
  - targetwithin{{d=6}} true
  Skills:
{o.zapowiedz("swallow_prep", [("entity.generic.eat", 1.8, 0.5), ("entity.frog.eat", 1.6, 0.4)] + lr("bagna:krol_ropuch.bulgot", 1.2, 0.8),
             ["particles{p=" + TURKUS + ";size=1.2;a=10;hs=0.6;vs=0.4;y=1.8}"], "bok", "Polkniecie: wyjdz z linii", czas=sw,
             gcd_dodatek=c['swallow']['dl'] + 3 + c['spit']['dl']).replace("s=swallow_prep;li=1;lo=2", "s=swallow_prep;li=1;lo=2" + sp)}
{punkt("KROL_POLKNIECIE" + sfx, f"""  Skills:
  - particlering{{p={TURKUS};size=1;radius=1.6;points=16;a=1;y=0.1}} @self
  - delay {T(sw) + T(c['swallow']['hit'])}
  - skill{{s=BAGNA_KROL_ROPUCH_W_PASZCZY}} @PlayersInRadius{{r=1.6}}
  - delay 70
""")}
  - particleline{{p={TURKUS};size=1;a=1;distanceBetween=0.5}} @target
  - delay {T(sw)}
  - {o.st("swallow", 0, 1)} @self
  - delay {T(c['swallow']['hit'])}
  - setvariable{{var=caster.paszcza_ciosy;val=0;type=INTEGER}} @self
  - delay {T(c['swallow']['dl'] - c['swallow']['hit'])}
  - delay 60
  - setvariable{{var=world.bagna_paszcza;val=0;type=INTEGER}} @self
  - {o.st("spit", 0, 2)} @self
  - delay {T(c['spit']['hit'])}
  - sound{{s=entity.player.burp;v=1.6;p=0.5;sc=HOSTILE}} @self
{''.join(f"  - {snd(*d)} @self" + chr(10) for d in lr("bagna:krol_ropuch.wyplucie", 1.2, 1.0))}{o.odslona("spit")}
""")
    o.skill(f"BAGNA_KROL_ROPUCH_JEZYK{sfx}", f"""
  # JEZYK KROLEWSKI: 10 bl., zapowiedz {tw:.2f} s -> przyciaga ze zamknietego punktu (kontra: wyjdz z linii)
  Cooldown: 9
  Conditions:
  - offgcd true
  - targetwithin{{d=10}} true
  - targetnotwithin{{d=4}} true
  Skills:
{o.zapowiedz("tongue", [("entity.frog.ambient", 1.8, 0.4), ("entity.ravager.roar", 1.0, 1.6)] + lr("bagna:rechotnik.rechot", 1.2, 0.6),
             ["particles{p=" + TURKUS + ";size=1.2;a=10;hs=0.6;vs=0.4;y=1.8}"], "bok", "Jezyk Krolewski: wyjdz z linii", czas=tw).replace("s=tongue;li=1;lo=2", "s=tongue;li=1;lo=2" + sp)}
{punkt("KROL_JEZYK" + sfx, f"""  # jezyk: trafiony w punkcie unieruchomiony 1.2 s + obrazenia (kontra: wyjdz z linii)
  Skills:
  - delay {T(c['tongue']['hit'] * k)}
  - command{{c="execute as <target.uuid> at @s at @e[tag=bagna_krol,distance=..12,sort=nearest,limit=1] run tp @s ^ ^ ^2.2"}} @PlayersInRadius{{r=1.8}}
  - damage{{amount=@KRO_JEZYK@}} @PlayersInRadius{{r=1.8}}
  - potion{{type=SLOWNESS;duration=24;level=10;hasParticles=false}} @PlayersInRadius{{r=1.8}}
  - particles{{p={TURKUS};size=1.2;a=14;hs=0.5;vs=0.6;y=1}} @self
""")}
  - particleline{{p={TURKUS};size=1;a=1;distanceBetween=0.5}} @target
  - delay {T(tw)}
  - particleline{{p={TURKUS};size=1.4;a=1;distanceBetween=0.4}} @target
  - delay {T(c['tongue']['hit'] * k) - T(tw)}
{o.odslona("tongue")}
""")
    o.skill(f"BAGNA_KROL_ROPUCH_SKOK{sfx}", f"""
  # SKOK KROLA: przysiad {lw:.2f} s, cien (krag r=4) w zamknietym punkcie -> ladowanie: fala r=4 (kontra: wyjdz z cienia)
  Cooldown: 12
  Conditions:
  - offgcd true
  - targetwithin{{d=12}} true
  Skills:
{o.zapowiedz("leap_prep", [("entity.ravager.step", 1.8, 0.5), ("entity.warden.sonic_charge", 0.8, 1.6)] + lr("bagna:krol_ropuch.bulgot", 1.0, 0.6),
             ["particles{p=block;m=MUD;a=20;hs=1;vs=0.2}"], "tyl", "Skok Krola: wyjdz z cienia r=4", czas=lw, ms_reakcji=int(1000 * (lw + c['leap']['hit'])) + 300,
             gcd_dodatek=c['leap']['dl'] + c['land']['dl']).replace("s=leap_prep;li=1;lo=2", "s=leap_prep;li=1;lo=2" + sp)}
{punkt("KROL_SKOK" + sfx, f"""  Skills:
  - particlering{{p=dust;color=#1a1a14;size=1.6;radius=3.5;points=36;a=1;y=0.1}} @self
  - delay {T(lw / 2)}
  - particlering{{p=dust;color=#1a1a14;size=1.6;radius=3.5;points=36;a=1;y=0.1}} @self
  - delay {T(lw) - T(lw / 2) + T(c['leap']['hit']) + T(c['land']['hit'])}
  - damage{{amount=@KRO_SKOK@}} @PlayersInRadius{{r=3.5}}
  - throw{{velocity=6;velocityY=4}} @PlayersInRadius{{r=3.5}}
  - particlering{{p=splash;radius=3.5;points=50;a=3;y=0.2}} @self
""")}
  - skill{{s=BAGNA_KROL_ROPUCH_LOT{sfx}}} @targetlocation
  - delay {T(lw)}
  - {o.st("leap", 0, 1)} @self
  - delay {T(c['leap']['hit'])}
  - {o.st("land", 0, 2)} @self
  - delay {T(c['land']['hit'])}
{o.odslona("land")}
""")
    o.skill(f"BAGNA_KROL_ROPUCH_LOT{sfx}", f"""
  Skills:
  - delay {T(lw)}
  - leap{{velocity=140}} @origin
""")
krol_warianty(0); krol_warianty(1)
o.skill("BAGNA_KROL_ROPUCH_W_PASZCZY", f"""
  # cel = polkniety gracz (linie bez targetera dziedzicza go; rzucajacy = znacznik punktu). 3 s w paszczy przy Krolu (tp po tagu).
  # v7 (karta): 5 ciosow w Krola w tym czasie (polkniety z bliska / druzyna) = wczesne wyplucie (world.bagna_paszcza=0 -> kolejne
  # obrazenia i przytrzymania pomijane). Solo bez bicia: 3x3 obr. i wyplucie z oszolomieniem - do przezycia.
  Skills:
  - setvariable{{var=world.bagna_paszcza;val=1;type=INTEGER}}
  - potion{{type=BLINDNESS;duration=70;level=0;hasParticles=false}}
  - potion{{type=SLOWNESS;duration=60;level=10;hasParticles=false}}
  - message{{m="<dark_green>Król Ropuch</dark_green> <gray>połyka cię! Bij od środka.</gray>"}}
  - command{{c="execute at <target.uuid> run tp <target.uuid> @e[tag=bagna_krol,distance=..16,sort=nearest,limit=1]"}} ?variableequals{{var=world.bagna_paszcza;value=1}}
  - damage{{amount=@KRO_PASZCZA@}} ?variableequals{{var=world.bagna_paszcza;value=1}}
  - delay 20
  - command{{c="execute at <target.uuid> run tp <target.uuid> @e[tag=bagna_krol,distance=..16,sort=nearest,limit=1]"}} ?variableequals{{var=world.bagna_paszcza;value=1}}
  - damage{{amount=@KRO_PASZCZA@}} ?variableequals{{var=world.bagna_paszcza;value=1}}
  - delay 20
  - command{{c="execute at <target.uuid> run tp <target.uuid> @e[tag=bagna_krol,distance=..16,sort=nearest,limit=1]"}} ?variableequals{{var=world.bagna_paszcza;value=1}}
  - damage{{amount=@KRO_PASZCZA@}} ?variableequals{{var=world.bagna_paszcza;value=1}}
  - delay {20 + T(c['spit']['hit'])}
  - throw{{velocity=9;velocityY=4}}
  - potion{{type=NAUSEA;duration=60;level=0;hasParticles=false}}
  - potion{{type=SLOWNESS;duration=20;level=3;hasParticles=false}}
""")
o.skill("BAGNA_KROL_ROPUCH_UWOLNIENIE", """
  # 5. cios w trakcie polkniecia: wyplucie od razu
  Skills:
  - setvariable{var=world.bagna_paszcza;val=0;type=INTEGER} @self
  - setvariable{var=caster.paszcza_ciosy;val=0;type=INTEGER} @self
  - command{c="effect clear <target.uuid> minecraft:blindness"} @PlayersInRadius{r=3}
  - throw{velocity=7;velocityY=3} @PlayersInRadius{r=3}
  - message{m="<gold>Król Ropuch dławi się i wypluwa ofiarę!</gold>"} @PlayersInRadius{r=30}
  - sound{s=entity.player.burp;v=1.6;p=0.7;sc=HOSTILE} @self
""")
o.skill("BAGNA_KROL_ROPUCH_WEZWANIE", f"""
  # WEZWANIE: rechot, 1 Rechotnik co 25 s (od 75% HP; v4: 2 naraz zabijaly uwaznego)
  Cooldown: 25
  Conditions:
  - offgcd true
  - playerwithin{{d=20}} true
  Skills:
  - gcd{{ticks={T(c['summon']['dl'] + 0.3)}}}
  - lockmodel{{l=true}} @self
  - {o.st("summon", 1, 2)} @self
  - sound{{s=entity.frog.ambient;v=2;p=0.4;sc=HOSTILE}} @PlayersInRadius{{r=40}}
  - sound{{s=entity.ravager.roar;v=1.4;p=1.6;sc=HOSTILE}} @PlayersInRadius{{r=40}}
  - potion{{type=SLOWNESS;duration={T(c['summon']['dl'])};level=10;hasParticles=false}} @self
  - delay {T(c['summon']['hit'])}
  - summon{{m=BAGNA_RECHOTNIK;level=1;a=1;r=6;yr=2;os=true}} @self
  - delay {T(c['summon']['recover'])}
  - lockmodel{{l=false}} @self
""")
o.skill("BAGNA_KROL_ROPUCH_BRODAWKI", """
  Cooldown: 1
  TargetConditions:
  - fieldofview{a=180} false
  Skills:
  - potion{type=POISON;duration=50;level=0}
  - particles{p=item_slime;a=10;hs=0.4;vs=0.4;y=1.5} @self
""")
o.skill("BAGNA_KROL_ROPUCH_GNIEW", """
  Cooldown: 9999
  Skills:
  - setvariable{var=caster.gniew;val=1;type=INTEGER} @self
  - message{m="<dark_green>Król Ropuch</dark_green> <gray>wpada w gniew - rusza się szybciej!</gray>"} @PlayersInRadius{r=40}
  - sound{s=entity.ravager.roar;v=1.6;p=1.2;sc=HOSTILE} @self
""")
o.klatki({"swallow": lr("bagna:krol_ropuch.klapniecie", 1.4, 1.0), "spit": [], "tongue": lr("bagna:rechotnik.jezyk", 1.4, 0.7),
          "land": [("entity.generic.explode", 0.8, 1.6)] + lr("bagna:krol_ropuch.ladowanie", 1.6, 1.0),
          "leap": [], "summon": [], "attack": [("entity.ravager.attack", 1.0, 1.2)] + lr("bagna:krol_ropuch.ugryzienie", 1.0, 1.0)})
SK_KRO = o.sk

# ===================================================================================================== zapis + bramki
SYGNALY = {"BAGNA_P_KOSCIEJ_STRZAL": ["  - setvariable{var=caster.strzal;val=0;type=INTEGER} @self ~onSpawn",
                                      "  - skill{s=BAGNA_KOSCIEJ_SALWA} @self ~onTimer:2 ?variableequals{var=world.bagna_kos_strzal;value=1} ?variableequals{var=caster.strzal;value=0}"]}
for mid, sk, tekst in PUNKTY:
    MOBS.append(f"""
{mid}:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: true
  Skills:
  - skill{{{{s={sk}}}}} @self ~onSpawn
""".replace("{{", "{").replace("}}", "}") + "".join(l + "\n" for l in SYGNALY.get(mid, [])))
SK_PUNKTY = [(sk, tekst) for mid, sk, tekst in PUNKTY]
for M, mod, dl in ([] if True else TRUPY):   # trupy wylaczone (patrz naglowek_modelu)
    MOBS.append(f"""
BAGNA_T_{M}:
  # nie Marker: ME wycina (cull) model encji z zerowym hitboxem po ~1 s (klient QA 03.10)
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: false
    Collidable: false
    NoGravity: true
    Invincible: true
    Silent: true
    Despawn: true
  Skills:
  - model{{m={mod};step=1}} @self ~onSpawn
  - brightness{{m={mod};p=swiatlo_;em=false;b=15;s=15}} @self ~onSpawn
  - skill{{s=BAGNA_T_{M}_S}} @self ~onSpawn
""")
    SK_PUNKTY.append((f"BAGNA_T_{M}_S", f"""  Skills:
  # model inicjuje sie tick po model - state w tym samym ticku nie gral (trup stal w idle)
  - delay 2
  - state{{m={mod};s=death_trup;li=0;lo=0;l=hold;f=true}} @self
  - delay {T(dl) + 20}
  - remove @self
"""))
WSZYSTKIE = SK_PUNKTY + SK_PIJ + SK_MUL + SK_KOS + SK_SZE + SK_KOK + SK_KOKON + SK_KKT + SK_REC + SK_SLI + SK_OGN + SK_KRO
for n, t in WSZYSTKIE:
    SKILLS.append(f"\n{n}:\n{t if t.startswith('  ') else t}")
mobs_txt, skills_txt = "".join(MOBS), "".join(SKILLS)
def _staty(t):
    for k, v in STATY.items():
        t = t.replace(f"@{k}@", str(v))
    zostaly = re.findall(r"@[A-Z]{3}_[A-Z]+@", t)
    assert not zostaly, f"nieuzupelnione liczby: {zostaly}"
    return t
mobs_txt, skills_txt = _staty(mobs_txt), _staty(skills_txt)
mobs_txt = mobs_txt.replace("# Bagna - 10 mobow (KARTY.md).", f"# Bagna - 10 mobow (KARTY.md), liczby {WERSJA}.", 1)

def bramki():
    import yaml
    bl = []
    my, sy = yaml.safe_load(mobs_txt), yaml.safe_load(skills_txt)
    assert isinstance(my, dict) and isinstance(sy, dict), "YAML nie jest mapa"
    nazwy = list(my) + list(sy)
    # 1. stany vs kontrakt (+ .bbmodel, gdy jest)
    for mod, anim in STANY:
        mob = mod[len("bagna_"):]
        if anim not in kontrakt.czasy(mob):
            bl.append(f"stan {mod}:{anim} spoza kontraktu")
        bb = f"/opt/docker/mc/data/world1/output/lochy/bagna/modele/{mob}/{mod}.bbmodel"
        if os.path.exists(bb):
            an = {a["name"] for a in json.load(open(bb)).get("animations", [])}
            # model bez kompletu animacji obowiazkowych = przed etapem 5 - wtedy sprawdzamy tylko kontrakt
            if set(kontrakt.OBOWIAZKOWE) <= an and anim not in an:
                bl.append(f"stan {mod}:{anim} brak w {bb}")
    for t in (mobs_txt, skills_txt):
        for m, s in re.findall(r"state\{m=([a-z_]+);s=([a-z_]+)", t):
            if s in ("death", "death_trup"): continue   # death_trup dokleja normalizuj_bbmodel.py przy wdrozeniu
            if (m, s) not in STANY:
                bl.append(f"stan {m}:{s} z YAML nie przeszedl przez kontrakt")
    # 2. dzwieki
    mc = {x["name"] for x in json.load(open(SOUNDS_MC))}
    for t in (mobs_txt, skills_txt):
        for s in re.findall(r"sound\{s=([a-z0-9_.:]+)", t):
            if s.startswith("bagna:"):
                if s not in dzwieki.ZDARZENIA: bl.append(f"dzwiek {s} nie ma w dzwieki.py")
            elif s not in mc:
                bl.append(f"dzwiek {s} nie istnieje w 1.21.11")
    # 3. odwolania skill{s=} / summon{m=}
    for s in set(re.findall(r"skill\{s=([A-Z0-9_]+)", mobs_txt + skills_txt)):
        if s not in sy: bl.append(f"brak skilla {s}")
    for s in set(re.findall(r"summon\{m=([A-Z0-9_]+)", skills_txt)):
        if s not in my: bl.append(f"summon nieznanego moba {s}")
    # 4. moby: Silent, bez Equipment (poza nosnikiem kokonu), model na spawn+load, Despawn, bez scale
    for k, v in my.items():
        if k == "BAGNA_KOKON" or k.startswith(("BAGNA_P_", "BAGNA_T_")):   # nosniki/znaczniki/trupy
            continue
        op = v.get("Options", {})
        if op.get("Silent") is not True: bl.append(f"{k}: brak Silent")
        if "Equipment" in v: bl.append(f"{k}: Equipment przy modelu")
        if "Despawn" not in op: bl.append(f"{k}: brak Despawn")
        sk = "\n".join(v.get("Skills", []))
        for tr in ("~onSpawn", "~onLoad"):
            if not re.search(r"model\{m=bagna_[a-z_]+;.*" + tr, sk): bl.append(f"{k}: brak model{{}} {tr}")
        if "scale" in sk: bl.append(f"{k}: skala atrybutem")
        if "randomstroll" in str(v.get("AIGoalSelectors")): bl.append(f"{k}: randomstroll")
    # 5. kazdy skill zapowiedzi zaczyna od offgcd; summon z level=1; selektor @e bez distance
    for l in (mobs_txt + skills_txt).splitlines():
        if "summon{" in l and "level=1" not in l and not l.lstrip().startswith("#"): bl.append(f"summon bez level=1: {l}")
        if re.search(r"@[ae]\[", l) and "distance=" not in l: bl.append(f"selektor bez distance=: {l}")
    # 6. unikalnosc zapowiedzi w rodzinie (pierwsza warstwa: nazwa+pitch)
    widz = {}
    for mob, anim, s, p in ZAPOWIEDZI:
        key = (s, p)
        if key in widz and widz[key][0] != mob: bl.append(f"zapowiedz {mob}/{anim} = {widz[key]} ({s} p{p})")
        widz.setdefault(key, (mob, anim))
    return bl

# zakres 03.10 (wlasciciel): wdrazamy piatke Lochu 002 + Kokoniatko (Wyleg); Mokradla odlozone -> mm/odlozone/
ODLOZONE = ("RECHOTNIK", "SLIMAK", "OGNIK", "KROL_ROPUCH")
def rozdziel(txt):
    """dzieli YAML na bloki najwyzszego poziomu; zwraca (aktywne, odlozone)"""
    bloki = re.split(r"(?m)^(?=[A-Z][A-Z0-9_]*:\n)", txt)
    akt, odl = [], []
    for b in bloki:
        nazwa = b.split(":", 1)[0]
        (odl if any(nazwa.startswith(pre) for o in ODLOZONE for pre in (f"BAGNA_{o}", f"BAGNA_P_{o}", f"BAGNA_T_{o}", f"BAGNA_P_KROL")) else akt).append(b)
    return "".join(akt), "".join(odl)

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    bl = bramki()
    if bl:
        print("BRAMKI: BLEDY", len(bl)); print("\n".join(" - " + b for b in bl)); sys.exit(1)
    m_akt, m_odl = rozdziel(mobs_txt); s_akt, s_odl = rozdziel(skills_txt)
    import yaml
    my, sy = yaml.safe_load(m_akt), yaml.safe_load(s_akt)
    zle = [x for x in set(re.findall(r"skill\{s=([A-Z0-9_]+)", m_akt + s_akt)) if x not in sy] + \
          [x for x in set(re.findall(r"summon\{m=([A-Z0-9_]+)", m_akt + s_akt)) if x not in my]
    assert not zle, f"aktywny zestaw odwoluje sie do odlozonych: {zle}"
    open(f"{OUT}/Bagna_mobs.yml", "w").write(m_akt)
    open(f"{OUT}/Bagna_skills.yml", "w").write(s_akt)
    os.makedirs(f"{OUT}/odlozone", exist_ok=True)
    open(f"{OUT}/odlozone/Bagna_mobs_odlozone.yml", "w").write("# ODLOZONE 03.10 (Mokradla) - wdrazac RAZEM z Bagna_*.yml\n" + m_odl)
    open(f"{OUT}/odlozone/Bagna_skills_odlozone.yml", "w").write("# ODLOZONE 03.10 (Mokradla)\n" + s_odl)
    json.dump(BOT, open(f"{OUT}/zapowiedzi.json", "w"), indent=1, ensure_ascii=False)
    print(f"aktywne: mobow {len(my)}, skilli {len(sy)} | odlozone -> {OUT}/odlozone/")
    print(f"BRAMKI: OK | mobow {len(MOBS) - 1}, skilli {len(WSZYSTKIE)}, stanow {len(set(STANY))}, zapowiedzi {len(ZAPOWIEDZI)} | UDERZENIE={UDERZENIE} LR={DZWIEKI_LR} -> {OUT}")
