#!/usr/bin/env python3
"""Przeciwnicy remake'u Lochu 001 (karta docs/lochy/akt1-v2/10-loch-001-kwietna-polana.md §5-6, protokol R1 §0/§13)
do strojenia na arenie. Prefiks L1R_ (stary loch v7 na world1 uzywa L001_*; bez kolizji nazw).
Wilk/Korzeniak/Lucznik bez zmian z Przeciwnicy_Gaj (v8) - tu tylko pak po smierci (wariant L1R_*).
Wynik: kuznia/levels/przeciwnicy/Przeciwnicy_L001{,_skills}.yml. Wyglad zastepczy (vanilla) do czasu modeli LR.

Sygnaly zapowiedzi (bot "uwazny"; mineflayer 4.39 myli nazwy dzwiekow 1.21.11 -> rozpoznaje po pitch, patrz arena_l001.js):
  SIEWCA_PUCH    block.big_dripleaf.tilt_down p0.7   -> krok w bok (3 kule w zamkniety punkt)
  KOROWIEC_CIOS  entity.iron_golem.attack p0.6       -> odskok w tyl (pole 2x3 przed nim), potem 1.5 s bez gardy
  SOWIEDZ_SKOK   entity.polar_bear.warning p0.8      -> krok w bok; pudlo = 2 s odslony
  SOWIEDZ_LAPA   entity.polar_bear.ambient p0.5      -> odskok w tyl (r4 przed nia)
  PAK dojrzewa   block.azalea.place p1.6 (ostatnia 1 s) -> trafic pak
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import LEVELS  # noqa: E402
import os, re
from przeciwnicy import MOBS as _GAJ
OUT = f"{LEVELS}/przeciwnicy"

def _v8():
    """GAJ_WILK/KORZENIAK/LUCZNIK v8 skopiowane 1:1 jako L1R_* + pak po smierci (Template nie: scalanie Skills niesprawdzone)."""
    out = ["# warianty v8 (walka = GAJ_* v8 bez zmian) + pak po smierci"]
    for blok in re.split(r"\n(?=GAJ_)", _GAJ.split("\n", 1)[1]):
        nazwa = blok.split(":", 1)[0]
        pak = PAK_DUZY if nazwa == "GAJ_KORZENIAK" else PAK_MALY
        out.append(blok.rstrip().replace(nazwa + ":", "L1R_" + nazwa[4:] + ":", 1) + "\n" + pak)
    return "\n".join(out) + "\n"

# wspolne: kazdy polegly zostawia JEDEN pak (R1); maly albo duzy
PAK_MALY = "  - summon{type=L1R_PAK;amount=1;noise=0} @self ~onDeath"
PAK_DUZY = "  - summon{type=L1R_PAK_DUZY;amount=1;noise=0} @self ~onDeath"
WSPOLNE = """  Options:
    Despawn: persistent
    PreventOtherDrops: true
    PreventItemPickup: true
    PreventSunburn: true"""
AIT = """  AITargetSelectors:
  - clear
  - attacker
  - players"""

V8 = _v8()
MOBS = f"""# Przeciwnicy remake'u Lochu 001 - generowane przez kuznia/tools/przeciwnicy/przeciwnicy_l001.py (nie edytowac recznie)
# ---- pak (R1): trafienie usuwa (1 HP, ~onDeath), po 4 s peczniebie i swieci, po 5 s skutek ----
# Nosnik: maly niewidzialny ZOMBIE bez AI z kwiatem na glowie. ARMOR_STAND odpadl (klon 16.11: odporny na zwykle
# obrazenia, cios gracza nie wolal ~onDamaged). Opozniony lancuch dojrzewania BIEGNIE DALEJ po smierci nosiciela
# (zmierzone: /kill paka i tak dawal Pniaczka) -> flaga caster.dojrzewa=0 w ~onDeath blokuje skutek.
L1R_PAK:
  Type: ZOMBIE
  Display: '&dPąk'
  Health: 1
  Damage: 0
  Options:
    NoAI: true
    Silent: true
    Despawn: persistent
    PreventOtherDrops: true
    PreventSunburn: true
    KnockbackResistance: 1
  Equipment:
  - FLOWERING_AZALEA HEAD
  Skills:
  - command{{c="minecraft:data merge entity <caster.uuid> {{IsBaby:1b}}"}} @self ~onSpawn
  - potion{{type=INVISIBILITY;duration=999999;level=0;hasParticles=false}} @self ~onSpawn
  - addtag{{tag=l1r_pak}} @self ~onSpawn
  - setvariable{{var=caster.dojrzewa;val=1;type=INTEGER}} @self ~onSpawn
  - skill{{s=L1R_PAK_DOJRZEWA}} @self ~onSpawn
  - skill{{s=L1R_PAK_ZDEPTANY}} @self ~onDeath
L1R_PAK_DUZY:
  Type: ZOMBIE
  Display: '&dDuży pąk'
  Health: 1
  Damage: 0
  Options:
    NoAI: true
    Silent: true
    Despawn: persistent
    PreventOtherDrops: true
    PreventSunburn: true
    KnockbackResistance: 1
    Scale: 1.5
  Equipment:
  - FLOWERING_AZALEA HEAD
  Skills:
  - command{{c="minecraft:data merge entity <caster.uuid> {{IsBaby:1b}}"}} @self ~onSpawn
  - potion{{type=INVISIBILITY;duration=999999;level=0;hasParticles=false}} @self ~onSpawn
  - addtag{{tag=l1r_pak_duzy}} @self ~onSpawn
  - setvariable{{var=caster.dojrzewa;val=1;type=INTEGER}} @self ~onSpawn
  - skill{{s=L1R_PAK_DUZY_DOJRZEWA}} @self ~onSpawn
  - skill{{s=L1R_PAK_ZDEPTANY}} @self ~onDeath
L1R_PAK_SIEWCY:
  Type: ZOMBIE
  Display: '&dPąk z puchem'
  Health: 1
  Damage: 0
  Options:
    NoAI: true
    Silent: true
    Despawn: persistent
    PreventOtherDrops: true
    PreventSunburn: true
    KnockbackResistance: 1
  Equipment:
  - DANDELION HEAD
  Skills:
  - command{{c="minecraft:data merge entity <caster.uuid> {{IsBaby:1b}}"}} @self ~onSpawn
  - potion{{type=INVISIBILITY;duration=999999;level=0;hasParticles=false}} @self ~onSpawn
  - addtag{{tag=l1r_pak}} @self ~onSpawn
  - setvariable{{var=caster.dojrzewa;val=1;type=INTEGER}} @self ~onSpawn
  - skill{{s=L1R_PAK_SIEWCY_DOJRZEWA}} @self ~onSpawn
  - skill{{s=L1R_PAK_ZDEPTANY}} @self ~onDeath
# Korzen: krag 3x3 na 8 s, spowalnia i trzyma WSZYSTKO (gracze i wrogowie)
L1R_KORZEN:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
  Skills:
  - skill{{s=L1R_KORZEN_TRZYMA}} @self ~onSpawn

# ---- roj i zwykle ----
L1R_PNIACZEK:
  Type: HUSK
  Display: '&2Pniaczek'
  Health: 6
  Damage: 1
{WSPOLNE}
    FollowRange: 16
    MovementSpeed: 0.3
    AttackSpeed: 0.83
  Equipment:
  - OAK_LEAVES HEAD
  Skills:
  # Baby: true NIE dziala na HUSK w 1.21.11 (klon 16.11: IsBaby 0b); data merge przy spawnie dziala (1b)
  - command{{c="minecraft:data merge entity <caster.uuid> {{IsBaby:1b}}"}} @self ~onSpawn
  - sound{{s=block.azalea.step;v=1.2;p=1.4}} @self ~onTimer:24
  - sound{{s=entity.fox.squeak;v=0.8;p=1.6}} @self ~onDeath
{PAK_MALY}

L1R_SIEWCA:
  Type: HUSK
  Display: '&eSiewca'
  Health: 20
  Damage: 0
{WSPOLNE}
    FollowRange: 22
    MovementSpeed: 0
    KnockbackResistance: 1
  Equipment:
  - DANDELION HEAD
  AIGoalSelectors:
  - clear
  - lookatplayers
  Skills:
  # PUCH: zapowiedz 0.8 s, 3 kule w ZAMKNIETY punkt: 2 obr. + Spowolnienie I 2 s kazda
  - skill{{s=L1R_SIEWCA_PUCH}} @target ~onTimer:20
  - summon{{type=L1R_PAK_SIEWCY;amount=1;noise=0}} @self ~onDeath

L1R_KOROWIEC:
  Type: ZOMBIE
  Display: '&6Korowiec'
  Health: 60
  Damage: 0
{WSPOLNE}
    FollowRange: 20
    MovementSpeed: 0.19
    KnockbackResistance: 0.8
    Scale: 1.3
  Equipment:
  - SHIELD OFFHAND
  - OAK_LOG HEAD
  Skills:
  - setvariable{{var=caster.garda;val=1;type=INTEGER}} @self ~onSpawn
  # GARDA: z przodu (120 st.) bierze 20% obrazen, chyba ze odsloniety po ciosie
  - skill{{s=L1R_KOROWIEC_GARDA;sync=true}} @trigger ~onDamaged
  # ODGORNE UDERZENIE: zapowiedz 1.0 s, pole 2x3 przed nim w zamknietym kierunku, 8 obr.; potem 1.5 s bez gardy
  - skill{{s=L1R_KOROWIEC_CIOS}} @target ~onTimer:20
{PAK_DUZY}

L1R_KOTWA:
  Type: ZOMBIE
  Display: '&5Kotwa'
  Health: 30
  Damage: 0
{WSPOLNE}
    NoAI: true
    Silent: true
    KnockbackResistance: 1
  Equipment:
  - ROOTED_DIRT HEAD
  Skills:
  - potion{{type=INVISIBILITY;duration=999999;level=0;hasParticles=false}} @self ~onSpawn
  - particles{{p=glow;a=6;hs=0.3;vs=0.5;y=1.6}} @self ~onTimer:10
  # co 10 s budzi 1 Pniaczka (puls)
  - sound{{s=block.chorus_flower.grow;v=1.6;p=0.7}} @self ~onTimer:200
  - summon{{type=L1R_PNIACZEK;amount=1;noise=1.5}} @self ~onTimer:200

{V8}
# ---- elita S6 ----
L1R_SOWIEDZ:
  Type: POLAR_BEAR
  Display: '&cSowiedź'
  Health: 90
  Damage: 0
{WSPOLNE}
    FollowRange: 24
    MovementSpeed: 0.24
    KnockbackResistance: 0.7
{AIT}
  Skills:
  - setvariable{{var=caster.mlode;val=0;type=INTEGER}} @self ~onSpawn
  - skill{{s=L1R_SOWIEDZ_SKOK}} @target ~onTimer:20
  - skill{{s=L1R_SOWIEDZ_LAPA}} @target ~onTimer:10
  # wolanie mlodych raz, przy 50% HP
  - skill{{s=L1R_SOWIEDZ_WOLA}} @self ~onDamaged ?healthpercent{{h=<50%}}
{PAK_DUZY}
L1R_SOWIEDZ_MLODE:
  Type: POLAR_BEAR
  Display: '&cMłode Sowiedzi'
  Health: 12
  Damage: 2
{WSPOLNE}
    Age: -1
    AgeLock: true
    FollowRange: 20
    MovementSpeed: 0.3
{AIT}
  Skills:
{PAK_MALY}
"""

SKILLS = """# Skille przeciwnikow remake'u Lochu 001 (caster = mob, delay dozwolony)
# ---- PAK (R1). Dojrzewanie: 4 s spokoju, 1 s pecznienia i swiecenia, skutek. Zdeptany = remove (test: czy opozniony
#      lancuch umiera z rzucajacym - flaga caster.dojrzewa jako bezpiecznik). ----
L1R_PAK_ZDEPTANY:
  Skills:
  - setvariable{var=caster.dojrzewa;val=0;type=INTEGER} @self
  - particles{p=block;m=FLOWERING_AZALEA_LEAVES;a=16;hs=0.3;vs=0.2;y=0.4} @self
  - sound{s=block.azalea_leaves.break;v=1.2;p=1.2} @self
L1R_PAK_PECZNIEJE:
  Skills:
  - sound{s=block.azalea.place;v=1.6;p=1.6} @self
  - particles{p=glow;a=12;hs=0.25;vs=0.25;y=0.5} @self
  - delay 10
  - particles{p=glow;a=12;hs=0.25;vs=0.25;y=0.5} @self
L1R_PAK_DOJRZEWA:
  Skills:
  - delay 80
  - skill{s=L1R_PAK_PECZNIEJE} @self ?variableequals{var=caster.dojrzewa;value=1}
  - delay 20
  - summon{type=L1R_PNIACZEK;amount=1;noise=0} @self ?variableequals{var=caster.dojrzewa;value=1}
  - remove @self
L1R_PAK_SIEWCY_DOJRZEWA:
  Skills:
  - delay 80
  - skill{s=L1R_PAK_PECZNIEJE} @self ?variableequals{var=caster.dojrzewa;value=1}
  - delay 20
  - summon{type=L1R_PNIACZEK;amount=3;noise=1} @self ?variableequals{var=caster.dojrzewa;value=1}
  - remove @self
L1R_PAK_DUZY_DOJRZEWA:
  Skills:
  - delay 80
  - skill{s=L1R_PAK_PECZNIEJE} @self ?variableequals{var=caster.dojrzewa;value=1}
  - delay 20
  - summon{type=L1R_KORZEN;amount=1;noise=0} @self ?variableequals{var=caster.dojrzewa;value=1}
  - remove @self
L1R_KORZEN_TRZYMA:
  Skills:
  - sound{s=entity.evoker_fangs.attack;v=1.4;p=0.6} @self
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - skill{s=L1R_KORZEN_TIK} @self
  - delay 20
  - remove @self
L1R_KORZEN_TIK:
  Skills:
  - particlering{p=block;m=ROOTED_DIRT;radius=1.5;points=20;a=2;y=0.1} @self
  - particles{p=block;m=OAK_LOG;a=10;hs=0.8;vs=0.3;y=0.3} @self
  - potion{type=SLOWNESS;duration=25;level=4;hasParticles=false} @LivingInRadius{r=1.6}

# ---- SIEWCA ----
L1R_SIEWCA_PUCH:
  Cooldown: 3.5
  Conditions:
  - targetwithin{d=18}
  Skills:
  - sound{s=block.big_dripleaf.tilt_down;v=1.8;p=0.7} @self
  - particles{p=white_ash;a=20;hs=0.4;vs=0.4;y=1.8} @self
  - skill{s=L1R_SIEWCA_PUCH_CEL} @targetlocation
L1R_SIEWCA_PUCH_CEL:
  Skills:
  - particlering{p=end_rod;radius=1.4;points=14;a=1;y=0.1} @origin
  - delay 16
  - skill{s=L1R_SIEWCA_KULA} @origin
  - delay 4
  - skill{s=L1R_SIEWCA_KULA} @origin
  - delay 4
  - skill{s=L1R_SIEWCA_KULA} @origin
L1R_SIEWCA_KULA:
  Skills:
  - sound{s=entity.breeze.shoot;v=1;p=1.6} @origin
  - particles{p=white_ash;a=30;hs=0.8;vs=0.4;y=0.8} @origin
  - damage{amount=2} @PlayersNearOrigin{r=1.4}
  - potion{type=SLOWNESS;duration=40;level=0;hasParticles=false} @PlayersNearOrigin{r=1.4}

# ---- KOROWIEC ----
L1R_KOROWIEC_GARDA:
  TargetConditions:
  - fieldofview{angle=120} true
  Skills:
  - modifydamage{a=0.2;action=ADDITIVE_MULTIPLIER;sync=true} @self ?variableequals{var=caster.garda;value=1}
  - sound{s=item.shield.block;v=1;p=0.9} @self ?variableequals{var=caster.garda;value=1}
L1R_KOROWIEC_CIOS:
  Cooldown: 4
  Conditions:
  - targetwithin{d=4.5}
  Skills:
  - sound{s=entity.iron_golem.attack;v=1.8;p=0.6} @self
  - potion{type=SLOWNESS;duration=30;level=10;hasParticles=false} @self
  - particles{p=block;m=OAK_LOG;a=20;hs=0.5;vs=0.8;y=2} @self
  # pole 2x3 przed nim: 3 punkty na linii kierunku, zamkniete w chwili zapowiedzi
  - skill{s=L1R_KOROWIEC_POLE} @forward{f=1.2}
  - skill{s=L1R_KOROWIEC_POLE} @forward{f=2.2}
  - skill{s=L1R_KOROWIEC_POLE} @forward{f=3.2}
  - delay 20
  - sound{s=entity.zombie.attack_wooden_door;v=1.6;p=0.5} @self
  # odslona 1.5 s: bez gardy, stoi
  - setvariable{var=caster.garda;val=0;type=INTEGER} @self
  - particles{p=falling_water;a=12;hs=0.3;vs=0.3;y=1.6} @self
  - delay 30
  - setvariable{var=caster.garda;val=1;type=INTEGER} @self
L1R_KOROWIEC_POLE:
  Skills:
  - particlering{p=block;m=OAK_LOG;radius=0.8;points=8;a=1;y=0.1} @origin
  - delay 20
  - particles{p=block;m=OAK_LOG;a=25;hs=0.7;vs=0.4;y=0.3} @origin
  - damage{amount=8} @PlayersNearOrigin{r=1.1}

# ---- SOWIEDZ ----
L1R_SOWIEDZ_SKOK:
  Cooldown: 5
  Conditions:
  - targetwithin{d=12}
  Skills:
  - sound{s=entity.polar_bear.warning;v=1.8;p=0.8} @self
  - particles{p=angry_villager;a=6;hs=0.5;vs=0.4;y=1.2} @self
  - potion{type=SLOWNESS;duration=20;level=10;hasParticles=false} @self
  - skill{s=L1R_SOWIEDZ_SKOK_CEL} @targetlocation
L1R_SOWIEDZ_SKOK_CEL:
  Skills:
  - particlering{p=crit;radius=1.8;points=16;a=1;y=0.2} @origin
  - delay 20
  - leap{velocity=150} @origin
  - delay 8
  - damage{amount=9} @PlayersNearOrigin{r=1.8}
  - throw{velocity=6;velocityY=3} @PlayersNearOrigin{r=1.8}
  # pudlo -> 2 s odslony (boczny slaby punkt), trafienie -> 0.7 s
  - skill{s=L1R_SOWIEDZ_ODSLONA} @self ?!playerwithin{d=2.2}
  - potion{type=SLOWNESS;duration=14;level=10;hasParticles=false} @self ?playerwithin{d=2.2}
L1R_SOWIEDZ_ODSLONA:
  Skills:
  - sound{s=entity.polar_bear.hurt;v=1.4;p=0.6} @self
  - potion{type=SLOWNESS;duration=40;level=10;hasParticles=false} @self
  - particles{p=falling_water;a=20;hs=0.4;vs=0.4;y=1.2} @self
  - delay 20
  - particles{p=falling_water;a=20;hs=0.4;vs=0.4;y=1.2} @self
L1R_SOWIEDZ_LAPA:
  Cooldown: 3
  Conditions:
  - targetwithin{d=3.5}
  Skills:
  - sound{s=entity.polar_bear.ambient;v=1.8;p=0.5} @self
  - potion{type=SLOWNESS;duration=20;level=10;hasParticles=false} @self
  - skill{s=L1R_SOWIEDZ_LAPA_POLE} @forward{f=2}
L1R_SOWIEDZ_LAPA_POLE:
  Skills:
  - particlering{p=sweep_attack;radius=1.6;points=6;a=1;y=0.6} @origin
  - delay 16
  - particles{p=sweep_attack;a=4;hs=1;vs=0.2;y=0.8} @origin
  - damage{amount=6} @PlayersNearOrigin{r=2}
L1R_SOWIEDZ_WOLA:
  Conditions:
  - variableequals{var=caster.mlode;value=0}
  Skills:
  - setvariable{var=caster.mlode;val=1;type=INTEGER} @self
  - sound{s=entity.parrot.imitate.ravager;v=2;p=0.6} @self
  - potion{type=SLOWNESS;duration=30;level=10;hasParticles=false} @self
  - delay 30
  - summon{type=L1R_SOWIEDZ_MLODE;amount=2;noise=2} @self
"""

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    open(f"{OUT}/Przeciwnicy_L001.yml", "w").write(MOBS)
    open(f"{OUT}/Przeciwnicy_L001_skills.yml", "w").write(SKILLS)
    print("zapisano", OUT)
