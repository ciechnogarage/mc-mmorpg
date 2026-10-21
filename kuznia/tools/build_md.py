#!/usr/bin/env python3
"""Warstwa rozgrywki poziomu: mapa MythicDungeons + moby/skille MythicMobs.

Zasada (wzorzec z level_1): logika siedzi w skillach MythicMobs (dobrze udokumentowanych),
funkcje MD tylko przypinaja je do miejsc/zdarzen. Uzywamy WYLACZNIE ksztaltow funkcji,
ktore maja dzialajacy przyklad (format functions.yml jest oficjalnie "Work in Progress").

Szablon mapy: BEZ level.dat (kazda instancja = nowe ziarno Irisa) i z PUSTYM region/
(bez niego NPE w AbstractDungeon:375).
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import LEVELS  # noqa: E402
import os, re, shutil, sys, json, math
sys.path.insert(0, os.path.dirname(__file__))
import make_poziom as M

DEFAULT_CFG = "/opt/docker/mc/data/world1/plugins/MythicDungeons/maps/default-config.yml"
OUT = f"{LEVELS}/poziom_01"
L = dict(M.LEVEL); L["spawn"] = (0, int(M.coast_r(math.pi / 2, L) - 22))
PID = L["id"]
SX, SZ = L["spawn"]; AX, AZ, AR = L["arena"]
def path_x(z): return round(M.path_x(z, L), 1)   # ta sama os sciezki co w obrazie wyspy

# ---------------- config.yml
cfg = open(DEFAULT_CFG).read()
def setkey(key, val):
    global cfg
    new, n = re.subn(rf"(\n\s*{key}:)[^\n]*", rf"\1 {val}", cfg, count=1)
    assert n == 1, key; cfg = new
setkey("ChunkGenerator", f"iris:{PID}")
setkey("DisplayName", "'&dPoziom 1 &8» &aKwietna Polana'")
setkey("ShowTitleOnStart", "false")
setkey("Gamemode", "ADVENTURE")
setkey("PlayerLives", "3")
setkey("MaxInstances", "3")
setkey("CleanupDelay", "0")          # instancja kasowana od razu po wyjsciu
setkey("MinPartySize", "1"); setkey("MaxPartySize", "4")
cfg = cfg.replace("    - md leave\n", "    - md leave\n    - md lives\n    - md stuck\n    - help\n    - msg\n    - r\n    - tell\n", 1)
assert "    - md lives" in cfg
# wejscie z gory: gracz pojawia sie ~6 blokow nad laka i lagodnie opada (slow falling w skillu
# startu). Wysokosc laki zmienia sie z ziarnem 99-103, wiec staly StartLocation i tak nie trafi w grunt.
start = (f"  StartLocation:\n    x: {SX + 0.5}\n    y: 104.0\n    z: {SZ + 0.5}\n"
         f"    pitch: 10.0\n    yaw: 180.0\n")
cfg = cfg.replace("\n  # Where the player appears after leaving", "\n" + start + "  # Where the player appears after leaving", 1)
assert "StartLocation:" in cfg

# ---------------- gamerules.yml
GR = """Version: 2
Gamerule:
  minecraft:
    fall_damage: false
    keep_inventory: true
    advance_time: false
    advance_weather: false
    random_tick_speed: 0
    mob_griefing: false
    spawn_phantoms: false
    show_death_messages: true
Difficulty: NORMAL
WorldTime: 1000
WorldWeather: CLEAR
"""
# fall_damage false: wyspa wisi w pustce - upadek z niej i tak konczy sie smiercia w pustce
#   (to nie sa obrazenia od upadku), a start "z gory" nie rani.
# keep_inventory true: smierc w pustce nie moze zabierac ekwipunku na poziomie 1.
# WorldTime 1000: jasny poranek. 23800 (przedswit) dawal ciemny grunt pod dziennym niebem (zrzut v1).

# ---------------- functions.yml
def loc(x, y, z): return (f"  location:\n    ==: org.bukkit.Location\n    x: {float(x)}\n    y: {float(y)}\n"
                          f"    z: {float(z)}\n    pitch: 0.0\n    yaw: 0.0\n")
TT = ("  targetType:\n    ==: net.playavalon.mythicdungeons.api.parents.FunctionTargetType\n"
      "    value: PARTY\n")
def skill_fn(skill, x, y, z, trigger):
    return (f"- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionMythicSkill\n"
            f"  mythicSkill: {skill}\n" + TT + loc(x, y, z) + trigger)
def t_start(delay=0):
    return (f"  trigger:\n    ==: net.playavalon.mythicdungeons.dungeons.triggers.TriggerDungeonStart\n"
            f"    delayTicks: {delay}\n    limitToRoom: false\n    allowRetrigger: false\n    conditions: []\n")
def t_dist(r):
    return (f"  trigger:\n    ==: net.playavalon.mythicdungeons.dungeons.triggers.TriggerDistance\n"
            f"    delayTicks: 0\n    forEachPlayer: false\n    limitToRoom: false\n    count: 1\n"
            f"    allowRetrigger: false\n    radius: {float(r)}\n    conditions: []\n")
def t_mobdeath(mob, delay=0):
    return (f"  trigger:\n    ==: net.playavalon.mythicdungeons.dungeons.triggers.TriggerMythicMobDeath\n"
            f"    mob: {mob}\n    delayTicks: {delay}\n    limitToRoom: false\n    count: 1\n"
            f"    allowRetrigger: false\n    radius: 0.0\n    conditions: []\n")
BOSS = "WIEZA_P01_ALPHA"
Y = 101          # punkty funkcji tuz nad typowym gruntem; summony maja onSurface + yRadius
E = {"laka_a": (path_x(35), Y, 35), "laka_b": (path_x(10), Y, 10), "brama": (path_x(-4), Y + 1, -4)}
fns = [
    # przybycie: trigger odleglosci na PIERWSZY ruch spadajacego gracza -> slow falling od razu.
    # (DungeonStart z delay 0 odpala sie zanim gracz jest w swiecie, z delay 20 - gdy juz wyladowal)
    skill_fn(f"{PID}_przybycie", SX, Y + 3, SZ, t_dist(10)),
    # start: NIE DungeonStart - zmierzone, ze odpala sie ~1 s PRZED przybyciem gracza (przed teleportem
    # i generacja chunkow pod spawnem), wiec tytul nie mial odbiorcy, a summon z onSurface - gruntu.
    # Trigger odleglosci tuz nad gruntem odpala sie pod koniec opadania, gdy gracz juz jest.
    skill_fn(f"{PID}_start", SX, Y, SZ, t_dist(5)),
    skill_fn(f"{PID}_zasadzka_laka", *E["laka_a"], t_dist(8)),
    skill_fn(f"{PID}_zasadzka_wilki", *E["laka_b"], t_dist(8)),
    skill_fn(f"{PID}_brama_gaju", *E["brama"], t_dist(8)),
    # KAZDA funkcja na INNYM bloku: funkcje MD sa przypiete do bloku - trzy funkcje w (0,101,-40)
    # skonczyly sie tym, ze skill bossa w ogole sie nie odpalal (zmierzone w kuzni).
    skill_fn(f"{PID}_arena_boss", AX, Y, AZ, t_dist(14)),
    skill_fn(f"{PID}_zwyciestwo", AX, Y + 1, AZ, t_mobdeath(BOSS, 40)),
    # leave: true + 15 s: gracz dostaje czas na lup, potem wraca sam (wczesniej 20 s ciszy i trzeba bylo znac /md leave)
    ("- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionFinishDungeon\n  leave: true\n"
     + TT + loc(AX, Y + 2, AZ) + t_mobdeath(BOSS, 300)),
]
FUN = "Version: 1\nFunctions:\n" + "".join(fns)

# ---------------- MythicMobs
MOBS = f"""# Moby poziomu 1 - SAMODZIELNE (bez Template). Dziedziczenie z Bestiariusza LACZYLO skille i dropy:
# nie dalo sie usunac vanilli (schab, orby XP), a boss bral wadliwy skok Wilka Alfy (AoE ranil jego
# wlasne wilki). Balans pod swieza postac z Kijem Nowicjusza (4 obr.) na trudnosci EASY serwera.
WIEZA_P01_ODYNIEC:
  Type: PIG
  Display: '&6Dziki Odyniec'
  Health: 20
  Damage: 3
  Armor: 0
  Faction: bestie
  Options:
    MovementSpeed: 0.25
    PreventOtherDrops: true
  Drops:
  - mmoitem{{type=MATERIAL;id=PROF_BEAST_SCRAP}} 1 0.5
  Skills:
  - command{{c="mmocore admin exp give <trigger.name> hunting 8"}} @trigger ~onDeath ?~isplayer
  AIGoalSelectors:
  - clear
  - meleeattack
  - randomstroll
  - lookatplayers
  AITargetSelectors:
  - clear
  - attacker
  - players

WIEZA_P01_WILK:
  Type: WOLF
  Display: '&6Wilk Gajowy'
  Health: 24
  Damage: 3
  Armor: 0
  Faction: bestie
  Options:
    MovementSpeed: 0.32
    PreventOtherDrops: true
  Drops:
  - mmoitem{{type=MATERIAL;id=PROF_BEAST_BONE}} 1 0.4
  Skills:
  - command{{c="mmocore admin exp give <trigger.name> hunting 10"}} @trigger ~onDeath ?~isplayer
  AITargetSelectors:
  - clear
  - attacker
  - players

{BOSS}:
  Type: WOLF
  Display: '&d&lStrażnik Polany'
  Health: 100
  Damage: 3
  Armor: 2
  Faction: bestie
  Options:
    MovementSpeed: 0.3
    AlwaysShowName: true
    KnockbackResistance: 0.4
    PreventOtherDrops: true
  BossBar:
    Enabled: true
    Title: '&d&lStrażnik Polany'
    Range: 45
    Color: PINK
    Style: SEGMENTED_10
  Drops:
  - mmoitem{{type=MATERIAL;id=PROF_BEAST_BONE}} 2-3 1
  - mmoitem{{type=MATERIAL;id=PROF_BEAST_SCRAP}} 2-3 1
  Skills:
  - addtag{{tag=wieza_boss}} @self ~onSpawn
  - command{{c="mmocore admin exp give <trigger.name> hunting 40"}} @trigger ~onDeath ?~isplayer
  - skill{{s=WIEZA_P01_SZARZA}} @target ~onTimer:60
  - skill{{s=WIEZA_P01_WYCIE}} @self ~onCombat
  AITargetSelectors:
  - clear
  - attacker
  - players
"""
ITEMS = """# Bron startowa: nowa postac nie ma ZADNEGO przedmiotu (CharacterCreator config.yml:181, klasy MMOCore
# tez nic nie daja). Docelowo zestaw startowy powinien dawac kreator postaci - to jest zaslepka poziomu 1.
WIEZA_KIJ_NOWICJUSZA:
  Id: WOODEN_SWORD
  Display: '<gray>Kij Nowicjusza</gray>'
  Lore:
  - '<dark_gray>Pierwsza broń każdego, kto wchodzi do Wieży.</dark_gray>'
  NBT:
    Unbreakable: boolean/true
"""
SKILLS = f"""# Skille poziomu 1 wiezy - wolane przez funkcje MythicDungeons (caster = punkt funkcji).
# BEZ 'delay' w skillach wolanych przez MD: casterem jest tymczasowy ArmorStand, ktory znika.
# (Skille BOSSA moga miec delay - ich casterem jest boss.)
# summon ZAWSZE z level=1: skill rzucony przez funkcje MD przyzywal wilki ze zdrowiem VANILLI (boss 8 HP
# zamiast 100 - padal po 2 s). Zmierzone 2026-09-23 na mapie testowej: level=1 -> 100 HP.
{PID}_przybycie:
  Skills:
  # Reguly swiata z KONSOLI na swiecie instancji (<caster.l.w>) - gamerules.yml nie jest stosowany.
  - command{{c="execute in minecraft:<caster.l.w> run gamerule fall_damage false"}}
  - command{{c="execute in minecraft:<caster.l.w> run gamerule keep_inventory true"}}
  - command{{c="execute in minecraft:<caster.l.w> run gamerule advance_time false"}}
  - command{{c="execute in minecraft:<caster.l.w> run gamerule advance_weather false"}}
  - command{{c="execute in minecraft:<caster.l.w> run time set 1000"}}
  - command{{c="execute in minecraft:<caster.l.w> run weather clear"}}
  - potion{{type=SLOW_FALLING;duration=80;level=0;hasParticles=false}} @PlayersInRadius{{r=30}}
  - sound{{s=block.amethyst_block.resonate;v=0.8;p=1.4}} @PlayersInRadius{{r=30}}
  # Kij tylko dla gracza z pusta reka (swieza postac); kto ma bron, nie dostaje smiecia.
  - giveitem{{i=WIEZA_KIJ_NOWICJUSZA;fakeLooting=true}} @PlayersInRadius{{r=30;conditions=[ - holding{{m=AIR}} true ]}}

{PID}_start:
  Skills:
  - title{{title="<light_purple>Poziom 1</light_purple>";subtitle="<green>Kwietna Polana</green>";fadein=15;duration=70;fadeout=20}} @PlayersInRadius{{r=40}}
  - sound{{s=block.amethyst_block.chime;v=1;p=1.2}} @PlayersInRadius{{r=40}}
  - summon{{m=BEAST_FOREST_HARE;level=1;a=3;r=10;yr=8;os=true}} @self
  - message{{m="<gray>Wiatr niesie płatki wiśni. Ścieżka prowadzi na północ, do serca gaju.</gray>"}} @PlayersInRadius{{r=60}}

{PID}_zasadzka_laka:
  Skills:
  - sound{{s=entity.pig.ambient;v=1;p=0.6}} @PlayersInRadius{{r=24}}
  - particles{{p=block;m=SHORT_GRASS;a=40;hs=4;vs=1;s=0.05}} @self
  - summon{{m=WIEZA_P01_ODYNIEC;level=1;a=2;r=6;yr=8;os=true}} @self

{PID}_zasadzka_wilki:
  Skills:
  - sound{{s=entity.wolf.howl;v=1;p=0.9}} @PlayersInRadius{{r=40}}
  - summon{{m=WIEZA_P01_WILK;level=1;a=2;r=6;yr=8;os=true}} @self

{PID}_brama_gaju:
  Skills:
  - message{{m="<light_purple>Pod wiśniami coś się porusza...</light_purple>"}} @PlayersInRadius{{r=30}}
  - particles{{p=cherry_leaves;a=80;hs=6;vs=3;s=0.02}} @self
  - summon{{m=WIEZA_P01_WILK;level=1;a=1;r=7;yr=8;os=true}} @self
  - summon{{m=WIEZA_P01_ODYNIEC;level=1;a=1;r=7;yr=8;os=true}} @self

{PID}_arena_boss:
  Skills:
  - title{{title="<gold><bold>Strażnik Polany</bold></gold>";subtitle="<gray>Uważaj, gdy warczy - odskocz!</gray>";fadein=5;duration=60;fadeout=10}} @PlayersInRadius{{r=45}}
  - sound{{s=entity.wolf.growl;v=1.2;p=0.6}} @PlayersInRadius{{r=45}}
  - particles{{p=cherry_leaves;a=150;hs=8;vs=4;s=0.03}} @self
  - summon{{m={BOSS};level=1;a=1;r=2;yr=6;os=true}} @self

{PID}_zwyciestwo:
  Skills:
  - title{{title="<green>Polana oczyszczona</green>";subtitle="<gray>Poziom 1 ukończony</gray>";fadein=10;duration=70;fadeout=20}} @PlayersInRadius{{r=70}}
  - sound{{s=ui.toast.challenge_complete;v=1;p=1}} @PlayersInRadius{{r=70}}
  - particles{{p=totem_of_undying;a=100;hs=4;vs=3;s=0.3}} @self
  - message{{m="<gold>Zbierz łup!</gold> <gray>Za 15 sekund wrócisz na start.</gray>"}} @PlayersInRadius{{r=70}}

# --- skille BOSSA (caster = boss, wiec delay jest bezpieczny)
WIEZA_P01_SZARZA:
  Cooldown: 8
  Conditions:
  - targetwithin{{d=12}}
  Skills:
  # telegraf ~1.2 s: warczenie + czasteczki + boss zastyga -> gracz ma czas odskoczyc
  - sound{{s=entity.wolf.growl;v=1.3;p=0.5}} @self
  - particles{{p=angry_villager;a=15;hs=0.7;vs=0.7;y=1.2}} @self
  - potion{{type=SLOWNESS;duration=24;level=5;hasParticles=false}} @self
  - delay 24
  - potion{{type=SPEED;duration=30;level=2;hasParticles=false}} @self
  - delay 16
  - damage{{amount=4}} @PlayersInRadius{{r=2.5}}
  - particles{{p=sweep_attack;a=8;hs=1.5;vs=0.3;y=0.8}} @self

WIEZA_P01_WYCIE:
  Cooldown: 60
  Skills:
  - message{{m="<light_purple>Strażnik Polany</light_purple> <gray>wzywa wilka z gaju!</gray>"}} @PlayersInRadius{{r=40}}
  - sound{{s=entity.wolf.howl;v=1.2;p=0.8}} @self
  - summon{{m=WIEZA_P01_WILK;level=1;a=1;r=4;yr=6;os=true}} @self
"""

shutil.rmtree(OUT, ignore_errors=True)
md = f"{OUT}/md/{PID}"
os.makedirs(f"{md}/region")   # PUSTY katalog wymagany przez MD
open(f"{md}/config.yml", "w").write(cfg)
open(f"{md}/gamerules.yml", "w").write(GR)
open(f"{md}/functions.yml", "w").write(FUN)
os.makedirs(f"{OUT}/mm/mobs"); os.makedirs(f"{OUT}/mm/skills"); os.makedirs(f"{OUT}/mm/items")
open(f"{OUT}/mm/items/Wieza_p01.yml", "w").write(ITEMS)
open(f"{OUT}/mm/mobs/Wieza_p01.yml", "w").write(MOBS)
if os.environ.get("DEBUG") == "1":
    SKILLS = re.sub(r"^(\w+):\n  Skills:\n", lambda m: f"{m.group(1)}:\n  Skills:\n  - command{{c=\"say [WIEZA-DEBUG] {m.group(1)}\"}}\n", SKILLS, flags=re.M)
# straznik: KAZDY summon musi miec level=1 (bez tego wilki z funkcji MD maja zdrowie vanilli)
_bez = [l for l in SKILLS.splitlines() if "summon{" in l and "level=1" not in l and not l.lstrip().startswith("#")]
assert not _bez, f"summon bez level=1: {_bez}"
open(f"{OUT}/mm/skills/Wieza_p01.yml", "w").write(SKILLS)
print("zbudowano:", OUT)
print("start:", (SX, 104, SZ), " arena:", (AX, AZ), " zasadzki:", E)
