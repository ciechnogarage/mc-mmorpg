#!/usr/bin/env python3
"""Loch 002 Bagna Zgnilych Korzeni -> mapa MythicDungeons + moby/skille MythicMobs.

Spec: /opt/docker/mc/data/world1/docs/lochy/loch-002-bagna-zgnilych-korzeni.md
Uzycie: build_loch002_md.py <swiat_kuzni>  -> kuznia/levels/loch_002_md/{md/loch_002, mm/...}

Wzorce przeniesione z build_loch001_md.py (kazdy zmierzony w kuzni/na world1 - pamiec mythicmobs-rozstawione-moby,
mythicdungeons-funkcje-bez-edytora): teren zamrozony + generator mythicdungeons:void; 1 funkcja MD na blok; zero delay
w skillach wolanych przez funkcje MD; summon level=1 + os=true; populacja na starcie; moby klikalne przyzywane z KONSOLI,
gdy gracz podchodzi (TriggerDistance -> mm mobs spawn); Despawn persistent, bez randomstroll; warunki tylko pozytywne.
Nowe wzgledem 001: WSZYSTKIE vanillowe selektory w `execute in <swiat>` maja `distance=` (inaczej @e z innej instancji
tego samego lochu tez pasuje - np. damage @e[tag=boss] trafialby cudzego bossa).
Grunt: spec generatora (loch_002_spec.json) - wysokosc kazdej kolumny znana, bez mierzenia.
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import DATA, LEVELS  # noqa: E402
import glob, json, math, os, re, shutil, sys

SWIAT = sys.argv[1]
KUZNIA = DATA
DEFAULT_CFG = "/opt/docker/mc/data/world1/plugins/MythicDungeons/maps/default-config.yml"
OUT = f"{LEVELS}/loch_002_md"
PID = "loch_002"
SP = json.load(open(f"{LEVELS}/loch_002_spec.json"))
GR = SP["grunt"]; TYP = SP["typ"]
LAD = ("lad", "sciezka", "kladka", "sciana_basenu")


def g(x, z):
    """y, na ktorym gracz STOI w kolumnie (x,z)."""
    return GR[f"{x},{z}"]


def lad(x, z, r=4):
    """najblizsza kolumna ladu (x,z,y) - moby i funkcje nie laduja w wodzie; assert gdy brak w promieniu r."""
    best = None
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            k = f"{x + dx},{z + dz}"
            if TYP.get(k) in ("lad", "sciezka", "kladka"):
                d = dx * dx + dz * dz
                if best is None or d < best[0]:
                    best = (d, x + dx, z + dz)
    assert best, f"brak ladu przy {x},{z}"
    return best[1], g(best[1], best[2]), best[2]


def woda(x, z, r=4):
    best = None
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if TYP.get(f"{x + dx},{z + dz}") == "woda":
                d = dx * dx + dz * dz
                if best is None or d < best[0]:
                    best = (d, x + dx, z + dz)
    assert best, f"brak wody przy {x},{z}"
    return best[1], g(best[1], best[2]), best[2]


def c(p):
    """@Location{c=...} dla punktu (x,y,z)"""
    return f"@Location{{c={p[0]},{p[1]},{p[2]}}}"


def spawn(mob, p):
    """przyzwanie z konsoli (srodek bloku) - dziala zawsze, takze dla mobow z ~onInteract/~onTimer"""
    return f'  - command{{c="mm mobs spawn {mob} 1 <caster.l.w>,{p[0]}.5,{p[1]},{p[2]}.5"}}\n'


YW, YL, DNO = SP["yw"], SP["yl"], SP["dno_basenu"]
B = SP["baseny"]; J = SP["jezioro"]; ST = SP["studnie"]; TOP = next(s for s in SP["sekcje"] if s["id"] == "topiel")["xz"]

# ---------------- miejsca
START = (-55.5, 102.0, 100.5)                 # koniec pomostu, patrzy na polnoc
P = {
    "przybycie": (-56, 102, 97), "populacja": (-56, 103, 94),
    "cp_przystan": (-56, 102, 84), "cp_rozstaj": (0, 102, -2), "cp_grobla": (22, 102, -64),
    "zasadzka_kladki": (-37, 102, 67),
    "budz_sluza1": (2, 102, 38), "budz_sluza2": (-30, 102, -6), "budz_sluza3": (64, 102, -8),
    "budz_kokoniarnia": (48, 102, -38),
    "wrak": (-8, 97, 19), "skrzynia": (-51, 97, -6), "dzwon": (83, 97, -6), "wywar": (37, 102, -6),
    "szept_tama": (0, 102, 46), "boss_intro": (10, 102, -74), "koniec": (0, 104, -118), "koniec_wyjscie": (0, 105, -118),
}
for k in ("cp_przystan", "cp_rozstaj", "cp_grobla", "budz_sluza1", "budz_sluza2", "budz_sluza3", "budz_kokoniarnia",
          "wywar", "szept_tama", "boss_intro"):
    x, y, z = P[k]; P[k] = lad(x, z)
for k in ("przybycie", "populacja"):
    assert TYP[f"{P[k][0]},{P[k][2]}"] in LAD, k
_bl = [tuple(map(int, p)) for p in P.values()]
assert len(set(_bl)) == len(_bl), "dwie funkcje na jednym bloku"

KORBY = {1: (4, YL + 1, 34), 2: (-37, YL + 1, -5), 3: (71, YL + 1, -7)}   # obok kolowrotow (grindstone w terenie)
BOREK_KOKON = (61, -51)
EMITER = {"start": (-55, 96), "kladki": (-35, 64), "tama": (-4, 36), "kokoniarnia": (46, -36), "arena": (12, -74)}                        # przed filarem kokonu (61,-52) z terenu
WELA = (-53, 76)

# ---------------- config.yml (jak 001)
cfg = open(DEFAULT_CFG).read()
def setkey(key, val):
    global cfg
    new, n = re.subn(rf"(\n\s*{key}:)[^\n]*", rf"\1 {val}", cfg, count=1)
    assert n == 1, key; cfg = new
setkey("ChunkGenerator", "mythicdungeons:void")
setkey("DisplayName", "'&dLoch 002 &8» &2Bagna Zgniłych Korzeni'")
setkey("ShowTitleOnStart", "false")
setkey("Gamemode", "ADVENTURE")
setkey("PlayerLives", "0")
setkey("MaxInstances", "3")
setkey("CleanupDelay", "0")
setkey("MinPartySize", "1"); setkey("MaxPartySize", "4")
cfg = cfg.replace("    - md leave\n", "    - md leave\n    - md lives\n    - md stuck\n    - help\n    - msg\n    - r\n    - tell\n", 1)
assert "    - md lives" in cfg
cfg = cfg.replace("\n  # Where the player appears after leaving",
                  f"\n  StartLocation:\n    x: {START[0]}\n    y: {START[1]}\n    z: {START[2]}\n    pitch: 5.0\n    yaw: 180.0\n"
                  "  # Where the player appears after leaving", 1)
assert "StartLocation:" in cfg

# gamerules.yml: KOMPLET regul (brak jednej = NPE przy smierci gracza, zmierzone 2026-09-27)
DOMYSLNE = """advance_time false;advance_weather true;block_drops true;block_explosion_drop_decay true;command_block_output true;
command_blocks_work true;drowning_damage true;elytra_movement_check true;ender_pearls_vanish_on_death true;entity_drops true;
fall_damage true;fire_damage true;fire_spread_radius_around_player 128;forgive_dead_players true;freeze_damage true;
global_sound_events true;immediate_respawn false;keep_inventory false;lava_source_conversion false;limited_crafting false;
locator_bar true;log_admin_commands true;max_block_modifications 32768;max_command_forks 65536;
max_command_sequence_length 65536;max_entity_cramming 24;max_snow_accumulation_height 1;mob_drops true;
mob_explosion_drop_decay true;mob_griefing true;natural_health_regeneration true;player_movement_check true;
players_sleeping_percentage 100;projectiles_can_break_blocks true;raids true;random_tick_speed 3;reduced_debug_info false;
respawn_radius 10;send_command_feedback true;show_advancement_messages true;show_death_messages true;spawn_mobs true;
spawn_monsters true;spawn_patrols true;spawn_phantoms true;spawn_wandering_traders true;spawn_wardens true;
spawner_blocks_work true;spectators_generate_chunks true;spread_vines true;tnt_explodes true;tnt_explosion_drop_decay false;
universal_anger false;water_source_conversion true"""
REG = dict(x.split() for x in DOMYSLNE.replace("\n", "").split(";"))
REG.update({"fall_damage": "false", "keep_inventory": "true", "advance_time": "false", "advance_weather": "false",
            "mob_griefing": "false", "random_tick_speed": "0", "immediate_respawn": "true", "block_drops": "false",
            "spawn_mobs": "false", "spawn_monsters": "false", "spawn_phantoms": "false", "spawn_patrols": "false",
            "spawn_wandering_traders": "false", "spawn_wardens": "false", "raids": "false", "drowning_damage": "false",
            "tnt_explodes": "false", "spread_vines": "false", "show_advancement_messages": "false", "water_source_conversion": "false"})
GRL = ("Version: 2\nGamerule:\n  minecraft:\n" + "".join(f"    {k}: {v}\n" for k, v in sorted(REG.items()))
       + "Difficulty: NORMAL\nWorldTime: 13000\nWorldWeather: RAIN\n")

# ---------------- functions.yml
def loc(x, y, z): return (f"  location:\n    ==: org.bukkit.Location\n    x: {float(x)}\n    y: {float(y)}\n"
                          f"    z: {float(z)}\n    pitch: 0.0\n    yaw: 0.0\n")
TT = "  targetType:\n    ==: net.playavalon.mythicdungeons.api.parents.FunctionTargetType\n    value: PARTY\n"
def t_dist(r, each=False, delay=0, retrig=False):
    return (f"  trigger:\n    ==: net.playavalon.mythicdungeons.dungeons.triggers.TriggerDistance\n"
            f"    delayTicks: {delay}\n    forEachPlayer: {str(each).lower()}\n    limitToRoom: false\n    count: 1\n"
            f"    allowRetrigger: {str(retrig).lower()}\n    radius: {float(r)}\n    conditions: []\n")
def t_mobdeath(mob, delay=0):
    return (f"  trigger:\n    ==: net.playavalon.mythicdungeons.dungeons.triggers.TriggerMythicMobDeath\n"
            f"    mob: {mob}\n    delayTicks: {delay}\n    limitToRoom: false\n    count: 1\n"
            f"    allowRetrigger: false\n    radius: 0.0\n    conditions: []\n")
def skill_fn(skill, xyz, trig):
    return (f"- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionMythicSkill\n  mythicSkill: {skill}\n"
            + TT + loc(*xyz) + trig)
def checkpoint(xyz, trig):
    return ("- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionCheckpoint\n  savePoint: false\n"
            + TT + loc(*xyz) + "  pitch: 0.0\n" + trig)
BOSS = "L002_TOPIELICA"
fns = [
    skill_fn(f"{PID}_przybycie", P["przybycie"], t_dist(8)),
    skill_fn(f"{PID}_populacja", P["populacja"], t_dist(8)),
    checkpoint(P["cp_przystan"], t_dist(10, each=True)),
    checkpoint(P["cp_rozstaj"], t_dist(8, each=True)),
    checkpoint(P["cp_grobla"], t_dist(8, each=True)),
    skill_fn(f"{PID}_zasadzka_kladki", P["zasadzka_kladki"], t_dist(4)),
    skill_fn(f"{PID}_szept_tama", P["szept_tama"], t_dist(7)),
    skill_fn(f"{PID}_budz_sluza1", P["budz_sluza1"], t_dist(12)),
    skill_fn(f"{PID}_budz_sluza2", P["budz_sluza2"], t_dist(12)),
    skill_fn(f"{PID}_budz_sluza3", P["budz_sluza3"], t_dist(12)),
    skill_fn(f"{PID}_budz_kokoniarnia", P["budz_kokoniarnia"], t_dist(14)),
    # sekrety na dnie basenow: dostepne dopiero po osuszeniu -> retrigger + warunek w skillu (bez wiadomosci na "nie")
    skill_fn(f"{PID}_wrak", P["wrak"], t_dist(2.8, retrig=True)),
    skill_fn(f"{PID}_skrzynia", P["skrzynia"], t_dist(2.8, retrig=True)),
    skill_fn(f"{PID}_dzwon", P["dzwon"], t_dist(2.8, retrig=True)),
    skill_fn(f"{PID}_wywar", P["wywar"], t_dist(2.2, retrig=True)),
    skill_fn(f"{PID}_boss_intro", P["boss_intro"], t_dist(9)),
    skill_fn(f"{PID}_koniec", P["koniec"], t_mobdeath(BOSS, 20)),
    ("- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionFinishDungeon\n  leave: true\n"
     + TT + loc(*P["koniec_wyjscie"]) + t_mobdeath(BOSS, 560)),
]
FUN = "Version: 1\nFunctions:\n" + "".join(fns)

# ---------------- MythicMobs: moby
NIEWID = """  Options:
    Invisible: true
    NoAI: true
    Invincible: true
    Silent: true
    Collidable: false
    Despawn: persistent
    PreventOtherDrops: true
    PreventRandomEquipment: true
    PreventSunburn: true
    AlwaysShowName: true
    ShowName: true
"""
MARKER = """  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
"""
STOI = """  AIGoalSelectors:
  - clear
  - meleeattack
  - lookatplayers
  - randomlookaround
  AITargetSelectors:
  - clear
  - attacker
  - players
"""
def exp(main, hunt=0):
    s = f'  - command{{c="mmocore admin exp give <trigger.name> main {main}"}} @trigger ~onDeath ?~isplayer\n'
    if hunt:
        s += f'  - command{{c="mmocore admin exp give <trigger.name> hunting {hunt}"}} @trigger ~onDeath ?~isplayer\n'
    return s

MOBS = """# Loch 002 - Bagna Zgnilych Korzeni. WYGLAD ZASTEPCZY (vanilla Type) do czasu modeli ModelEngine.
# Zasady (pamiec mythicmobs-rozstawione-moby): Despawn persistent, bez randomstroll, summon level=1.
L002_PIJAWKA:
  Type: ENDERMITE
  Display: '&2Pijawka'
  Health: 6
  # 1.5 -> 1: rój 3 Pijawek zabijal stojacego gracza w kilka sekund (sonda trasy 29.09); poziom Pniaczka z 001
  Damage: 1
  Faction: topiel
  Options:
    Despawn: persistent
    FollowRange: 11
    MovementSpeed: 0.32
    PreventOtherDrops: true
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_BEAST_SCRAP} 1 0.3
  - mmoitem{type=MATERIAL;id=PROF_VENOM_SAC} 1 0.06
""" + STOI + """  Skills:
  # reguła drowning_damage dotyczy tylko graczy - endermit w wodzie tonal po ~15 s (zasadzka z wody znikala sama, sonda 29.09)
  - potion{type=WATER_BREATHING;duration=999999;level=0;hasParticles=false} @self ~onSpawn
  # ukaszenie czasem zatruwa (lekcja statusu: trucizna nie zabija, ale zjada zycie)
  - potion{type=POISON;duration=50;level=0} @target ~onAttack 0.2 ?variableequals{var=world.l002_dzwon;value=0}
  - sound{s=entity.silverfish.hurt;v=0.6;p=0.6} @self ~onDamaged
  - particles{p=block;m=MUD;a=10;hs=0.3;vs=0.2;s=0.05} @self ~onDeath
""" + exp(8, 2) + """
L002_MULOWIEC:
  Type: DROWNED
  Display: '&aMułowiec'
  Health: 26
  Damage: 3
  Faction: topiel
  Options:
    Despawn: persistent
    FollowRange: 12
    MovementSpeed: 0.19
    PreventOtherDrops: true
    PreventSunburn: true
    PreventItemPickup: true
    PreventRandomEquipment: true
  Equipment:
  - MUD HEAD
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_CLAY_LUMP} 1 0.6
  - mmoitem{type=MATERIAL;id=PROF_FLAX_FIBER} 1 0.35
""" + STOI + """  Skills:
  # slad mulu: co 1,5 s zostawia plame (zyje 4 s) spowalniajaca w r=1.6 - kontra: nie idz jego sladem
  - skill{s=L002_SLAD} @selflocation ~onTimer:30
  - sound{s=block.mud.step;v=1;p=0.6} @self ~onTimer:30
""" + exp(25, 6) + """
L002_KOSCIEJ:
  Type: BOGGED
  Display: '&2Kościej Bagienny'
  Health: 18
  Damage: 2
  Faction: topiel
  Options:
    Despawn: persistent
    FollowRange: 18
    # stoi w miejscu jak wiezyczka (lekcja zasiegu widzenia): lucznik bez ruchu dalej celuje i strzela
    MovementSpeed: 0
    KnockbackResistance: 1
    PreventOtherDrops: true
    PreventSunburn: true
    PreventItemPickup: true
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_BEAST_BONE} 1 0.5
  Skills:
""" + exp(20, 5) + """
L002_SZEPTUCHA:
  Type: WITCH
  Display: '&5Szeptucha'
  Health: 30
  Faction: topiel
  Options:
    Despawn: persistent
    FollowRange: 16
    MovementSpeed: 0
    PreventOtherDrops: true
    PreventItemPickup: true
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_MEDICINAL_HERB} 1-2 0.6
  - mmoitem{type=MATERIAL;id=PROF_REDCAP} 1 0.4
  Skills:
  # WYWAR: kanal 3 s oslaniajacy sojusznikow w r=8; 3 ciosy w trakcie przerywaja (wzorzec Kory ze Starego Korzenia)
  - skill{s=L002_WYWAR} @self ~onTimer:60
  - variableadd{var=caster.wywar_ciosy;amount=1} @self ~onDamaged
  - skill{s=L002_WYWAR_PEKA} @self ~onTimer:5 ?variableequals{var=caster.wywar;value=1} ?variableequals{var=caster.wywar_ciosy;value=3}
  - skill{s=L002_WYWAR_PEKA} @self ~onTimer:5 ?variableequals{var=caster.wywar;value=1} ?variableequals{var=caster.wywar_ciosy;value=4}
  - skill{s=L002_WYWAR_PEKA} @self ~onTimer:5 ?variableequals{var=caster.wywar;value=1} ?variableequals{var=caster.wywar_ciosy;value=5}
""" + exp(40, 8) + """
L002_SZEPTUCHA_CHATA:
  Template: L002_SZEPTUCHA
  Display: '&5Szeptucha'
  Skills:
  - setvariable{var=world.l002_szeptucha;val=1;type=INTEGER} @self ~onDeath
  - message{m="<gray>Kocioł przed chatą Szeptuchy wciąż bulgocze.</gray>"} @PlayersInRadius{r=20} ~onDeath

L002_KOKONIARA:
  Type: SPIDER
  Display: '&f&lKokoniara'
  Health: 70
  Damage: 4
  Armor: 1
  Faction: topiel
  Options:
    Despawn: persistent
    FollowRange: 14
    MovementSpeed: 0.28
    KnockbackResistance: 0.5
    PreventOtherDrops: true
    AlwaysShowName: true
  BossBar:
    Enabled: true
    Title: '&f&lKokoniara'
    Range: 24
    Color: WHITE
    Style: SEGMENTED_6
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_SINEW} 2 1
  - mmoitem{type=MATERIAL;id=PROF_VENOM_SAC} 1 0.6
""" + STOI + """  Skills:
  - command{c="attribute <caster.uuid> minecraft:scale base set 1.6"} @self ~onSpawn
  - potion{type=WATER_BREATHING;duration=999999;level=0;hasParticles=false} @self ~onSpawn
  - addtag{tag=l002_elita} @self ~onSpawn
  - skill{s=L002_KOKON_RZUT} @target ~onTimer:50
  - skill{s=L002_KOKONIARA_SKOK} @target ~onTimer:40
  - skill{s=L002_MLODE} @self ~onTimer:20 ?healthpercent{p=<50%}
  - sound{s=entity.spider.death;v=1.3;p=0.5} @self ~onDeath
""" + exp(120, 25) + """
L002_PAJECZE:
  Type: CAVE_SPIDER
  Display: '&7Młode Kokoniary'
  Health: 5
  Damage: 1
  Faction: topiel
  Options:
    Despawn: persistent
    FollowRange: 12
    PreventOtherDrops: true
""" + STOI + exp(5) + """
# kokon na graczu: bij go (3 ciosy), zanim zacisnie sie trucizna
L002_KOKON:
  Type: ZOMBIE
  Display: '<white>Kokon</white> <gray>(bij!)</gray>'
  Health: 5
  Options:
    Invisible: true
    NoAI: true
    Silent: true
    Collidable: false
    Despawn: persistent
    PreventOtherDrops: true
    PreventRandomEquipment: true
    PreventSunburn: true
    AlwaysShowName: true
  Equipment:
  - COBWEB HEAD
  Skills:
  - skill{s=L002_KOKON_ZYCIE} @self ~onSpawn
  - skill{s=L002_KOKON_PEKA} @self ~onDeath

# kokon z rybakiem Borkiem (przed filarem kokonu z terenu) - bij, zeby uwolnic
L002_KOKON_BORKA:
  Type: ZOMBIE
  Display: '<white>Kokon</white> <gray>- ktoś w nim jęczy (bij)</gray>'
  Health: 12
  Options:
    Invisible: true
    NoAI: true
    Silent: true
    Collidable: false
    Despawn: persistent
    PreventOtherDrops: true
    PreventRandomEquipment: true
    PreventSunburn: true
    AlwaysShowName: true
    KnockbackResistance: 1
  Equipment:
  - WHITE_WOOL HEAD
  Skills:
  - particles{p=block;m=COBWEB;a=4;hs=0.4;vs=0.6;s=0.01;y=1} @self ~onTimer:20
  - skill{s=L002_BOREK_WOLNY} @self ~onDeath

L002_BOREK:
  Type: VILLAGER
  Display: '<aqua>Rybak Borek</aqua>'
  Health: 20
  Options:
    NoAI: true
    Invincible: true
    Despawn: persistent
    AlwaysShowName: true
    PreventOtherDrops: true
  Skills:
  - skill{s=L002_BOREK_ROZMOWA} @trigger ~onInteract

L002_WELA:
  Type: VILLAGER
  Display: '<aqua>Rybaczka Wela</aqua>'
  Health: 20
  Options:
    NoAI: true
    Invincible: true
    Despawn: persistent
    AlwaysShowName: true
    PreventOtherDrops: true
  Skills:
  - skill{s=L002_WELA_ROZMOWA} @trigger ~onInteract

L002_TOPIELICA:
  Type: DROWNED
  Display: '&2&lTopielica'
  Health: 200
  Damage: 5
  Armor: 2
  Faction: topiel
  Options:
    Despawn: persistent
    FollowRange: 24
    MovementSpeed: 0.24
    KnockbackResistance: 0.8
    PreventOtherDrops: true
    PreventSunburn: true
    PreventItemPickup: true
    PreventRandomEquipment: true
    AlwaysShowName: true
  BossBar:
    Enabled: true
    Title: '&2&lTopielica, Matka Mułu'
    Range: 48
    Color: GREEN
    Style: SEGMENTED_10
  Equipment:
  - MANGROVE_ROOTS HEAD
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_WATER_ESSENCE} 1-2 1
  - mmoitem{type=MATERIAL;id=PROF_CLAM} 2 1
  - mmoitem{type=MATERIAL;id=PROF_PEARL} 1 0.3
""" + STOI + """  Skills:
  - command{c="attribute <caster.uuid> minecraft:scale base set 2.4"} @self ~onSpawn
  - addtag{tag=l002_boss} @self ~onSpawn
  - skill{s=L002_WLOSY} @target ~onTimer:60
  - skill{s=L002_WLOSY} @target ~onTimer:45 ?healthpercent{p=<25%}
  - skill{s=L002_KALUZA} @target ~onTimer:70
  - skill{s=L002_NURT} @self ~onTimer:40
  - skill{s=L002_SKRZEK} @self ~onTimer:20 ?healthpercent{p=<60%}
  - skill{s=L002_GNIEW} @self ~onTimer:20 ?healthpercent{p=<25%}
  - sound{s=entity.elder_guardian.death;v=1.2;p=0.8} @self ~onDeath
  - particles{p=block;m=MUD;a=150;hs=2;vs=2.5;s=0.1;y=2} @self ~onDeath
  - command{c="mmocore admin exp give <trigger.name> hunting 40"} @trigger ~onDeath ?~isplayer
  - command{c="mmocore admin exp give <target.name> main 200"} @PlayersInWorld ~onDeath

# kiscie skrzeku przy studniach (faza 2): 6 s na rozbicie, potem 2 Pijawki
L002_SKRZEK:
  Type: ZOMBIE
  Display: '<green>Skrzek</green> <gray>(rozbij)</gray>'
  Health: 6
  Options:
    Invisible: true
    NoAI: true
    Silent: true
    Collidable: false
    Despawn: persistent
    PreventOtherDrops: true
    PreventRandomEquipment: true
    PreventSunburn: true
    AlwaysShowName: true
  Equipment:
  - SLIME_BLOCK HEAD
  Skills:
  - skill{s=L002_SKRZEK_WYLEG} @self ~onSpawn
  - particles{p=item_slime;a=20;hs=0.4;vs=0.4;y=1.5} @self ~onDeath
  - command{c="mmocore admin exp give <trigger.name> main 4"} @trigger ~onDeath ?~isplayer

"""
MOBS += "L002_STUDNIA:\n" + MARKER + "  Skills:\n  - addtag{tag=l002_studnia} @self ~onSpawn\n  - particles{p=bubble_pop;a=6;hs=0.8;vs=0.2;y=-0.5} @self ~onTimer:20\n\n"
for k in (1, 2, 3):
    MOBS += f"""L002_KORBA_{k}:
  Type: ZOMBIE
  Display: '<gold>Kołowrót śluzy</gold> <gray>(PPM)</gray>'
  Health: 1
""" + NIEWID + f"""  Equipment:
  - TRIPWIRE_HOOK HEAD
  Skills:
  - particles{{p=glow;a=2;hs=0.3;vs=0.3;s=0.01;y=1.7}} @self ~onTimer:20
  - skill{{s=L002_SLUZA_{k}}} @trigger ~onInteract

"""
MOBS += """# ZGNILA WODA: gracz stojacy w wodzie dostaje trucizne (odnawiana co 1 s); po Dzwonie Topielcow - oczyszczenie.
# Trzy emitery w roznych chunkach (pojedynczy emiter z ~onTimer bywal martwy ~1/3 instancji - pamiec).
L002_ZGNILA_WODA:
""" + MARKER + """  Skills:
  # mechanika MM zamiast vanillowego effect z konsoli (konsola logowala "Applied effect Poison" co sekunde na gracza).
  # Warunek "w wodzie" MUSI byc w TargetConditions metaskilla: inline ?inliquid na tej linii sprawdza ZNACZNIK (rzucajacego),
  # nie gracza - zmierzone debugiem MM 5.11.2 w instancji MD (30.09).
  - skill{s=L002_ZGNILA_TIK} @PlayersInRadius{r=320} ~onTimer:20

L002_KLIMAT:
""" + MARKER + """  Skills:
  - particles{p=glow;a=5;hs=10;vs=2;s=0;y=2} @self ~onTimer:20
  - particles{p=white_smoke;a=6;hs=10;vs=0.2;s=0.005;y=0.3} @self ~onTimer:30
  - sound{s=entity.frog.ambient;v=0.6;p=0.8} @self ~onTimer:160
  - sound{s=entity.drowned.ambient_water;v=0.3;p=0.5} @self ~onTimer:420

L002_SWIT:
""" + MARKER + """  Skills:
  - particles{p=glow;a=12;hs=10;vs=3;s=0;y=2} @self ~onTimer:15
  - particles{p=end_rod;a=2;hs=8;vs=3;s=0.01;y=2} @self ~onTimer:30

# sekwencja po smierci Topielicy (caster = ten znacznik -> delay bezpieczny)
L002_FINAL:
""" + MARKER + """  Skills:
  - skill{s=L002_FINAL_SEKWENCJA} @self ~onSpawn
"""

# ---------------- skille
POD = "<caster.l.w>"
def ex(cmd):
    """vanillowa komenda w swiecie instancji (selektory z distance= w cmd)"""
    return f'  - command{{c="execute in minecraft:{POD} run {cmd}"}}\n'
def fill_woda(r, y, na="air"):
    x0, x1, z0, z1 = r
    return ex(f"fill {x0} {y} {z0} {x1} {y} {z1} minecraft:{na} replace minecraft:water")

S = f"""# Loch 002. Skille wolane przez funkcje MD (caster = punkt funkcji): BEZ delay, summon level=1 + os=true.
{PID}_przybycie:
  Skills:
  # gamerules.yml nie jest stosowany do recznego szablonu (zmierzone) - reguly z konsoli na swiecie instancji
""" + "".join(ex(f"gamerule {k} {v}") for k, v in [("fall_damage", "false"), ("keep_inventory", "true"), ("advance_time", "false"),
       ("advance_weather", "false"), ("mob_griefing", "false"), ("random_tick_speed", "0"), ("block_drops", "false"),
       ("drowning_damage", "false"), ("water_source_conversion", "false")]) + ex("time set 13000") + ex("weather rain") + f"""  - setvariable{{var=world.l002_sekrety;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_sluza1;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_sluza2;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_sluza3;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_korba;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_dzwon;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_szeptucha;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_wrak;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_skrzynia;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l002_wywar;val=0;type=INTEGER}} @self
  - title{{title="<light_purple>Loch 002</light_purple>";subtitle="<dark_green>Bagna Zgniłych Korzeni</dark_green>";fadein=15;duration=60;fadeout=20}} @PlayersInRadius{{r=40}}
  - sound{{s=entity.frog.ambient;v=1;p=0.6}} @PlayersInRadius{{r=40}}
  - message{{m="<aqua>Rybaczka Wela</aqua> <gray>macha do ciebie od ogniska na brzegu. Zgniła woda parzy - trzymaj się kładek.</gray>"}} @PlayersInRadius{{r=40}}
""" + spawn("L002_ZGNILA_WODA", lad(*EMITER["start"])) + f"""
# POPULACJA: moby stoja od startu; klikalni NPC i emitery z konsoli (mm mobs spawn)
{PID}_populacja:
  Skills:
"""
def summ(mob, p, a=1, r=0):
    return f"  - summon{{m={mob};level=1;a={a};r={r};yr=6;os=true}} {c(p)}\n"
# Przystan: szabrownicy przy beczkach (pierwsza, latwa walka)
S += summ("L002_PIJAWKA", lad(-50, 70), 3, 1)
S += spawn("L002_WELA", lad(*WELA))
# Wysepka: 2 Pijawki; Kosciej na osobnej wysepce za woda (LoS przez martwe pnie)
S += summ("L002_PIJAWKA", lad(2, 50), 2, 1)
kx, kz = SP["kosciej"][0]
S += summ("L002_KOSCIEJ", lad(kx, kz))
# Przed Tama: Mulowiec + 2 Pijawki przy kolowrocie
S += summ("L002_MULOWIEC", lad(-3, 40)) + summ("L002_PIJAWKA", lad(5, 42), 2, 1)
# Rozstaj: Mulowiec + Kosciej na brzegu basenu 1 (strzela przez most)
S += summ("L002_MULOWIEC", lad(-5, -3)) + summ("L002_KOSCIEJ", lad(18, 4))
# Chata Szeptuchy: Szeptucha oslania 2 Mulowce
S += summ("L002_SZEPTUCHA_CHATA", lad(38, -4)) + summ("L002_MULOWIEC", lad(30, -8)) + summ("L002_MULOWIEC", lad(31, -13))
# Sluza Zachodnia: 2 Koscieje + 3 Pijawki
S += summ("L002_KOSCIEJ", lad(-28, -12)) + summ("L002_KOSCIEJ", lad(-38, 1)) + summ("L002_PIJAWKA", lad(-30, -4), 3, 1)
# Kokoniarnia: elita
ko = next(s for s in SP["sekcje"] if s["id"] == "kokoniarnia")["xz"]
S += summ("L002_KOKONIARA", lad(*ko))
# Sluza Wschodnia: Mulowiec + Szeptucha
S += summ("L002_MULOWIEC", lad(60, -8)) + summ("L002_SZEPTUCHA", lad(68, -2))
# zgnila woda: emitery NIE z populacji - przyzwane na starcie bywaly martwe (sonda 30.09: 1 na 2 przebiegi bez trucizny).
# Przyzywa je kazdy wyzwalacz przy graczu (start, kladki, tama, kokoniarnia, arena) - nadmiar nie szkodzi (ten sam efekt).
for p in ((-40, 66), (-4, 52), (20, 0), (50, -36), (6, -80)):
    S += summ("L002_KLIMAT", lad(*p))
# zaby i swietliki - zycie na bagnie
for p in ((-46, 80), (-10, 58), (40, -20)):
    S += f"  - summon{{m=FROG;level=1;a=2;r=2;yr=6;os=true}} {c(lad(*p))}\n"

S += f"""
# ZASADZKA na kladkach: Pijawki wyskakuja z wody po obu stronach
{PID}_zasadzka_kladki:
  Skills:
  - sound{{s=entity.player.splash;v=1.2;p=0.6}} @PlayersInRadius{{r=20}}
  - particles{{p=splash;a=80;hs=5;vs=0.5}} @self
""" + spawn("L002_ZGNILA_WODA", lad(*EMITER["kladki"])) + summ("L002_PIJAWKA", woda(-40, 72), 2, 0) + summ("L002_PIJAWKA", woda(-33, 60), 2, 0) + f"""
{PID}_szept_tama:
  Skills:
  - message{{m="<gray>Tama przegradza przesmyk. Krata w bramie jest opuszczona - obok stoi kołowrót śluzy.</gray>"}} @PlayersInRadius{{r=12}}

# KOLOWROTY: klikalna korba przyzywana z konsoli, gdy gracz podchodzi (klik w moby z populacji bywal martwy - pamiec)
{PID}_budz_sluza1:
  Skills:
""" + spawn("L002_KORBA_1", KORBY[1]) + spawn("L002_ZGNILA_WODA", lad(*EMITER["tama"])) + f"""{PID}_budz_sluza2:
  Skills:
""" + spawn("L002_KORBA_2", KORBY[2]) + f"""  - message{{m="<gray>Zachodnia śluza. Za murem stoi basen pełen zgniłej wody.</gray>"}} @PlayersInRadius{{r=14}}
{PID}_budz_sluza3:
  Skills:
""" + spawn("L002_KORBA_3", KORBY[3]) + f"""{PID}_budz_kokoniarnia:
  Skills:
""" + spawn("L002_KOKON_BORKA", lad(*BOREK_KOKON, r=1)) + spawn("L002_ZGNILA_WODA", lad(*EMITER["kokoniarnia"])) + f"""  - sound{{s=entity.spider.ambient;v=1.2;p=0.6}} @PlayersInRadius{{r=24}}
  - message{{m="<gray>Między mangrowcami wiszą kokony. Z jednego dobiega jęk.</gray>"}} @PlayersInRadius{{r=24}}
"""

# --- SLUZY (caster = korba, delay bezpieczny)
def sluza(k, basen, tytul, opis, extra=""):
    r = B[basen]
    s = f"""L002_SLUZA_{k}_RUSZ:
  Skills:
  - setvariable{{var=world.l002_sluza{k};val=1;type=INTEGER}} @self
  - sound{{s=block.grindstone.use;v=1.5;p=0.6}} @PlayersInRadius{{r=20}}
  - sound{{s=block.chain.place;v=1.5;p=0.5}} @PlayersInRadius{{r=20}}
  - particles{{p=block;m=MUD;a=40;hs=1;vs=1;s=0.1;y=1}} @self
  - message{{m="<gold>Kołowrót skrzypi i rusza.</gold>"}} @PlayersInRadius{{r=20}}
  - delay 20
{extra}"""
    for y in range(YW, DNO, -1):
        s += fill_woda(r, y) + "  - sound{s=block.bubble_column.whirlpool_inside;v=1.4;p=0.8} @PlayersInRadius{r=30}\n  - delay 8\n"
    s += f"""  - title{{title="";subtitle="<gold>{tytul}</gold>";fadein=5;duration=40;fadeout=15}} @PlayersInRadius{{r=30}}
  - message{{m="<gold>{tytul}.</gold> <gray>{opis}</gray>"}} @PlayersInRadius{{r=30}}
  - command{{c="mmocore admin exp give <target.name> main 20"}} @PlayersInWorld
  - remove @self
"""
    return s

S += """
# --- SLUZY: klik w korbe; drugi klik nic nie robi. Osuszanie warstwami (efekt opadajacej wody).
L002_SLUZA_1:
  Cooldown: 2
  Skills:
  - skill{s=L002_SLUZA_1_RUSZ} @self ?variableequals{var=world.l002_sluza1;value=0}
L002_SLUZA_2:
  Cooldown: 2
  Skills:
  - skill{s=L002_SLUZA_2_RUSZ} @self ?variableequals{var=world.l002_sluza2;value=0}
L002_SLUZA_3:
  Cooldown: 3
  Skills:
  - skill{s=L002_SLUZA_3_RUSZ} @self ?variableequals{var=world.l002_sluza3;value=0} ?variableequals{var=world.l002_korba;value=1}
  - message{m="<gray>Kołowrót zardzewiał na amen. Bez korby ani drgnie - może ktoś z rybaków ją ma.</gray>"} @PlayersInRadius{r=8} ?variableequals{var=world.l002_korba;value=0}
"""
S += sluza(1, "b1", "Śluza otwarta", "Krata w tamie opadła, a woda z basenu uchodzi. Na dnie leży wrak łodzi - zejście po schodach od wschodu.",
           ex("fill -1 102 30 1 104 31 minecraft:air replace minecraft:iron_bars")
           + "  - sound{s=block.iron_door.open;v=1.5;p=0.6} @PlayersInRadius{r=20}\n")
S += sluza(2, "b2", "Zachodnia śluza otwarta", "Na dnie basenu, wśród kości, stoi skrzynia.")
S += sluza(3, "b3", "Wschodnia śluza otwarta", "Z mułu na dnie wystaje stary dzwon.")

# --- SEKRETY na dnie (retrigger: pierwszy warunek to osuszenie, drugi - jeszcze nie wziety)
def sekret(nazwa, var, warunek, blok_xyz, tytul, podtytul, dary, extra=""):
    s = f"""{PID}_{nazwa}:
  Skills:
  - skill{{s=L002_{nazwa.upper()}_OK}} @self ?variableequals{{var={warunek};value=1}} ?variableequals{{var=world.l002_{var};value=0}}
L002_{nazwa.upper()}_OK:
  Skills:
  - setvariable{{var=world.l002_{var};val=1;type=INTEGER}} @self
  - variableadd{{var=world.l002_sekrety;amount=1}} @self
  - command{{c="mmocore admin exp give <target.name> main 25"}} @PlayersInWorld
"""
    if blok_xyz:
        x, y, z = blok_xyz
        s += ex(f"setblock {x} {y} {z} minecraft:barrel[facing=up,open=true]")
    s += f"""  - sound{{s=block.barrel.open;v=1;p=0.8}} @PlayersInRadius{{r=8}}
  - particles{{p=happy_villager;a=15;hs=0.5;vs=0.5;y=1}} @self
  - title{{title="<gold>{tytul}</gold>";subtitle="<gray>{podtytul}</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInRadius{{r=10}}
  - message{{m="<gold>{tytul}.</gold> <gray>Sekrety lochu <gold><world.var.l002_sekrety>/5</gold></gray>"}} @PlayersInRadius{{r=10}}
"""
    for mat, n in dary:
        s += f'  - command{{c="mi give MATERIAL {mat} <target.name> {n}"}} @PlayersInWorld\n'
    return s + extra
S += "\n# --- SEKRETY\n"
S += sekret("wrak", "wrak", "world.l002_sluza1", (-8, 97, 20), "Wrak Rybaków", "Zapasy, których nikt nie odebrał",
            [("PROF_FISH_OIL", 2), ("PROF_RAW_CATCH", 2)])
S += sekret("skrzynia", "skrzynia", "world.l002_sluza2", (-51, 97, -7), "Skrzynia Topielca", "Ktoś tu utonął ze swoim dobytkiem",
            [("PROF_CLAM", 2), ("PROF_ROUGH_CRYSTAL", 1)])
S += sekret("dzwon", "dzwon", "world.l002_sluza3", None, "Dzwon Topielców", "Zgniła woda nie ma już nad tobą władzy",
            [("PROF_WATER_ESSENCE", 1)],
            """  - sound{s=block.bell.use;v=2;p=0.7} @PlayersInRadius{r=60}
  - sound{s=block.bell.resonate;v=2;p=0.7} @PlayersInRadius{r=60}
  - command{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> run effect clear @a[distance=..300] minecraft:poison"}
  - message{m="<aqua>Dźwięk dzwonu niesie się nad bagnem.</aqua> <gray>Do końca lochu zgniła woda i trucizny Topielicy nie działają na drużynę.</gray>"} @PlayersInWorld
""")
S += sekret("wywar", "wywar", "world.l002_szeptucha", None, "Wywar Szeptuchy", "+4 serca osłony dla całej drużyny",
            [("PROF_REDCAP", 1)],
            """  - potion{type=ABSORPTION;duration=12000;level=1;hasParticles=false} @PlayersInWorld
  - sound{s=entity.generic.drink;v=1;p=0.8} @PlayersInRadius{r=8}
""")

S += f"""
# zgnila woda: gracz w cieczy dostaje trucizne; po Dzwonie (world.l002_dzwon=1) juz nie. Linie BEZ targetera (dziedzicza gracza).
L002_ZGNILA_TIK:
  Conditions:
  - variableequals{{var=world.l002_dzwon;value=0}} true
  TargetConditions:
  - inliquid true
  Skills:
  - potion{{type=POISON;duration=40;level=0;hasParticles=true}}

# --- ZYWE MOBY (caster = mob, delay bezpieczny)
L002_SLAD:
  Skills:
  - particles{{p=block;m=MUD;a=14;hs=0.8;vs=0.05;s=0.02}} @origin
  - potion{{type=SLOWNESS;duration=25;level=1;hasParticles=false}} @PlayersNearOrigin{{r=1.6}}
  - delay 20
  - particles{{p=block;m=MUD;a=10;hs=0.8;vs=0.05;s=0.02}} @origin
  - potion{{type=SLOWNESS;duration=25;level=1;hasParticles=false}} @PlayersNearOrigin{{r=1.6}}
  - delay 20
  - particles{{p=block;m=MUD;a=8;hs=0.8;vs=0.05;s=0.02}} @origin
  - potion{{type=SLOWNESS;duration=25;level=1;hasParticles=false}} @PlayersNearOrigin{{r=1.6}}
  - delay 20
  - particles{{p=block;m=MUD;a=6;hs=0.8;vs=0.05;s=0.02}} @origin
  - potion{{type=SLOWNESS;duration=25;level=1;hasParticles=false}} @PlayersNearOrigin{{r=1.6}}

# WYWAR Szeptuchy: 3 s kanalu -> oslona sojusznikow; 3 ciosy przerywaja
L002_WYWAR:
  Cooldown: 12
  Conditions:
  - playerwithin{{d=14}}
  Skills:
  - setvariable{{var=caster.wywar;val=1;type=INTEGER}} @self
  - setvariable{{var=caster.wywar_ciosy;val=0;type=INTEGER}} @self
  - sound{{s=block.brewing_stand.brew;v=1.5;p=0.7}} @self
  - particles{{p=witch;a=30;hs=0.6;vs=1;y=1.5}} @self
  - delay 20
  - particles{{p=witch;a=30;hs=0.6;vs=1;y=1.5}} @self ?variableequals{{var=caster.wywar;value=1}}
  - delay 20
  - particles{{p=witch;a=30;hs=0.6;vs=1;y=1.5}} @self ?variableequals{{var=caster.wywar;value=1}}
  - delay 20
  - skill{{s=L002_WYWAR_DZIALA}} @self ?variableequals{{var=caster.wywar;value=1}}
  - setvariable{{var=caster.wywar;val=0;type=INTEGER}} @self
L002_WYWAR_DZIALA:
  Skills:
  - potion{{type=ABSORPTION;duration=200;level=1;hasParticles=true}} @MobsInRadius{{r=8;types=L002_MULOWIEC,L002_PIJAWKA,L002_KOSCIEJ,L002_SZEPTUCHA,L002_SZEPTUCHA_CHATA}}
  - potion{{type=RESISTANCE;duration=160;level=0;hasParticles=true}} @MobsInRadius{{r=8;types=L002_MULOWIEC,L002_PIJAWKA,L002_KOSCIEJ,L002_SZEPTUCHA,L002_SZEPTUCHA_CHATA}}
  - particles{{p=happy_villager;a=40;hs=4;vs=1;y=1}} @self
  - message{{m="<dark_purple>Szeptucha</dark_purple> <gray>dokończyła wywar - jej sługi obrosły mułem.</gray>"}} @PlayersInRadius{{r=20}}
L002_WYWAR_PEKA:
  Skills:
  - setvariable{{var=caster.wywar;val=0;type=INTEGER}} @self
  - sound{{s=block.glass.break;v=1.5;p=0.8}} @self
  - particles{{p=witch;a=60;hs=0.8;vs=1;s=0.2;y=1.5}} @self
  - message{{m="<gold>Wywar przerwany!</gold>"}} @PlayersInRadius{{r=20}}

# KOKONIARA
L002_KOKON_RZUT:
  Cooldown: 10
  Conditions:
  - targetwithin{{d=10}}
  Skills:
  - sound{{s=entity.spider.ambient;v=1.5;p=0.5}} @self
  - potion{{type=SLOWNESS;duration=20;level=10;hasParticles=false}} @self
  - particleline{{p=block;m=COBWEB;a=2;distanceBetween=0.6}} @target
  - delay 16
  - skill{{s=L002_KOKON_NA}} @target
L002_KOKON_NA:
  Skills:
  - summon{{m=L002_KOKON;level=1;a=1;r=0}} @targetlocation
  - potion{{type=SLOWNESS;duration=80;level=6;hasParticles=false}} @target
  - message{{m="<white>Kokon oplata ci nogi!</white> <gray>Bij go, zanim się zaciśnie.</gray>"}} @target
L002_KOKONIARA_SKOK:
  Cooldown: 6
  Conditions:
  - targetwithin{{d=8}}
  Skills:
  - sound{{s=entity.spider.step;v=1.5;p=0.5}} @self
  - particles{{p=angry_villager;a=6;hs=0.5;vs=0.4;y=1}} @self
  - potion{{type=SLOWNESS;duration=14;level=6;hasParticles=false}} @self
  - delay 14
  - leap{{velocity=110}} @target
  - delay 8
  - damage{{amount=4}} @PlayersInRadius{{r=2}}
L002_MLODE:
  Cooldown: 9999
  Skills:
  - message{{m="<white>Kokoniara</white> <gray>rozrywa kokony - wysypują się młode!</gray>"}} @PlayersInRadius{{r=24}}
  - summon{{m=L002_PAJECZE;level=1;a=3;r=3;yr=6;os=true}} @self
L002_KOKON_ZYCIE:
  Skills:
  - delay 80
  - potion{{type=POISON;duration=60;level=1}} @PlayersInRadius{{r=1.6}} ?variableequals{{var=world.l002_dzwon;value=0}}
  - command{{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> run effect clear @a[distance=..1.8] minecraft:slowness"}}
  - remove @self
L002_KOKON_PEKA:
  Skills:
  - sound{{s=block.wool.break;v=1.5;p=0.7}} @self
  - particles{{p=block;m=COBWEB;a=30;hs=0.5;vs=0.8;y=1}} @self
  - command{{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> run effect clear @a[distance=..2.5] minecraft:slowness"}}

# BOREK: kokon rozbity -> rybak, korba, sekret
L002_BOREK_WOLNY:
  Skills:
""" + ex("fill 61 102 -52 61 104 -52 minecraft:air") + spawn("L002_BOREK", lad(62, -50, r=2)) + """  - setvariable{var=world.l002_korba;val=1;type=INTEGER} @self
  - variableadd{var=world.l002_sekrety;amount=1} @self
  - command{c="mmocore admin exp give <target.name> main 25"} @PlayersInWorld
  - sound{s=entity.villager.celebrate;v=1;p=0.9} @PlayersInRadius{r=16}
  # cala druzyna: korba to klucz do sluzy 3 (w r=16 od kokonu wiadomosc gubila sie - sonda 29.09)
  - title{title="<aqua>Rybak Borek</aqua>";subtitle="<gray>uwolniony z kokonu</gray>";fadein=5;duration=50;fadeout=15} @PlayersInWorld
  - message{m="<aqua>Rybak Borek</aqua> <gray>- Wela mnie przysłała? Weźcie korbę, wschodnia śluza bez niej ani drgnie. Jest na południe stąd, na wschód od chaty Szeptuchy.</gray>"} @PlayersInWorld
  - message{m="<gray>Sekrety lochu <gold><world.var.l002_sekrety>/5</gold></gray>"} @PlayersInWorld
  - command{c="mi give MATERIAL PROF_SINEW <target.name> 1"} @PlayersInWorld
L002_BOREK_ROZMOWA:
  Cooldown: 3
  Skills:
  - message{m="<aqua>Rybak Borek</aqua> <gray>- Wschodnia śluza jest na południe stąd, na wschód od chaty Szeptuchy. Jak otworzycie basen, na dnie leży stary dzwon topielców.</gray>"}
L002_WELA_ROZMOWA:
  Cooldown: 3
  Skills:
  - skill{s=L002_WELA_PROSBA} @trigger ?variableequals{var=world.l002_korba;value=0}
  - skill{s=L002_WELA_DZIEKI} @trigger ?variableequals{var=world.l002_korba;value=1}
L002_WELA_PROSBA:
  Skills:
  - message{m="<aqua>Rybaczka Wela</aqua> <gray>- Mój Borek poszedł otwierać śluzy i przepadł. Za tamą, w gąszczu mangrowców, lęgną się pająki... Jeśli go znajdziesz, weź od niego korbę.</gray>"}
  - sound{s=entity.villager.ambient;v=1;p=1.1}
L002_WELA_DZIEKI:
  Skills:
  - message{m="<aqua>Rybaczka Wela</aqua> <gray>- Żyje! Niech bagno wam to wynagrodzi.</gray>"}
  - sound{s=entity.villager.yes;v=1;p=1.1}

# --- TOPIELICA
L002_WLOSY:
  Cooldown: 7
  Conditions:
  - targetwithin{d=16}
  Skills:
  - sound{s=entity.drowned.ambient_water;v=2;p=0.6} @self
  - potion{type=SLOWNESS;duration=20;level=10;hasParticles=false} @self
  - particleline{p=dust;color=#3b5a2a;size=1.4;a=1;distanceBetween=0.4} @target
  - delay 10
  - particleline{p=dust;color=#3b5a2a;size=1.4;a=1;distanceBetween=0.4} @target
  - delay 10
  - pull{velocity=4} @target
  - potion{type=SLOWNESS;duration=20;level=1;hasParticles=false} @target
  - damage{amount=2} @target
L002_KALUZA:
  Cooldown: 9
  Conditions:
  - targetwithin{d=20}
  Skills:
  - sound{s=block.mud.break;v=1.5;p=0.5} @self
  - skill{s=L002_KALUZA_MIEJSCE} @targetlocation
L002_KALUZA_MIEJSCE:
  Skills:
  - particlering{p=block;m=SLIME_BLOCK;radius=2.5;points=28;a=2;y=0.1} @origin
  - delay 12
  - particlering{p=block;m=SLIME_BLOCK;radius=2.5;points=28;a=2;y=0.1} @origin
  - delay 12
  - sound{s=block.bubble_column.upwards_inside;v=1.5;p=0.6} @origin
  - particles{p=item_slime;a=40;hs=2;vs=0.3} @origin
  - potion{type=POISON;duration=60;level=0} @PlayersNearOrigin{r=2.5} ?variableequals{var=world.l002_dzwon;value=0}
  - delay 20
  - particles{p=item_slime;a=30;hs=2;vs=0.3} @origin
  - potion{type=POISON;duration=60;level=0} @PlayersNearOrigin{r=2.5} ?variableequals{var=world.l002_dzwon;value=0}
# ZANURZENIE: znika w losowej AKTYWNEJ studni (znaczniki l002_studnia), babelki 1,5 s, wynurza sie z fala
L002_NURT:
  Cooldown: 16
  Conditions:
  - playerwithin{d=22}
  Skills:
  - sound{s=entity.player.splash.high_speed;v=1.5;p=0.6} @PlayersInRadius{r=30}
  - particles{p=splash;a=80;hs=1.2;vs=1.5} @self
  - potion{type=INVISIBILITY;duration=45;level=0;hasParticles=false} @self
  - command{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> as @e[tag=l002_boss,distance=..3,limit=1] at @e[tag=l002_studnia,distance=..60,sort=random,limit=1] run minecraft:tp @s ~ ~-4 ~"}
  - delay 2
  - skill{s=L002_BABLE} @selflocation
  - delay 30
  - command{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> as @e[tag=l002_boss,distance=..3,limit=1] run minecraft:tp @s ~ ~4 ~"}
  - command{c="effect clear <caster.uuid> minecraft:invisibility"}
  - sound{s=entity.generic.splash;v=2;p=0.5} @PlayersInRadius{r=30}
  - particles{p=splash;a=120;hs=2;vs=2;y=1} @self
  - damage{amount=5} @PlayersInRadius{r=3}
  - throw{velocity=6;velocityY=4} @PlayersInRadius{r=3}
L002_BABLE:
  Skills:
  - particles{p=bubble_column_up;a=40;hs=1;vs=1;y=4} @origin
  - sound{s=block.bubble_column.whirlpool_inside;v=1.5;p=0.7} @PlayersInRadius{r=20}
  - delay 10
  - particles{p=bubble_column_up;a=40;hs=1;vs=1;y=4} @origin
  - delay 10
  - particles{p=bubble_column_up;a=60;hs=1;vs=1;y=4} @origin
  - particlering{p=splash;radius=3;points=30;a=2;y=4} @origin
# SKRZEK (faza 2): kisc jaj przy kazdej aktywnej studni
L002_SKRZEK:
  Cooldown: 22
  Skills:
  - message{m="<dark_green>Topielica</dark_green> <gray>składa skrzek w studniach - rozbij go, zanim się wykluje!</gray>"} @PlayersInRadius{r=40}
  - skill{s=L002_SKRZEK_STUDNIE} @self
L002_SKRZEK_STUDNIE:
  Skills:
PLACEHOLDER_STUDNIE
L002_SKRZEK_WYLEG:
  Skills:
  - delay 120
  - particles{p=item_slime;a=30;hs=0.5;vs=0.5;y=1} @self
  - summon{m=L002_PIJAWKA;level=1;a=2;r=1;yr=4;os=true} @self
  - remove @self
L002_GNIEW:
  Cooldown: 9999
  Skills:
  - message{m="<dark_green>Topielica</dark_green> <gray>wyje - bagno kipi wokół niej!</gray>"} @PlayersInRadius{r=40}
  - sound{s=entity.ravager.roar;v=1.3;p=1.2} @self
  - particlering{p=splash;radius=6;points=60;a=3;y=0.5} @self
  - throw{velocity=8;velocityY=3} @PlayersInRadius{r=5}
"""

# Skrzek przy AKTYWNYCH studniach: stale pozycje + warunek sluzy (@MobsInRadius nie trafial znacznikow - sonda 29.09)
_sk = ""
for k, (sx, sz) in sorted(ST.items()):
    war = "" if k == "D" else f" ?variableequals{{var=world.l002_sluza{ {'A': 1, 'B': 2, 'C': 3}[k] };value=0}}"
    _sk += f"  - summon{{m=L002_SKRZEK;level=1;a=1;r=0}} @Location{{c={sx + (2 if sx < 0 else -2)},{YL + 1},{sz}}}{war}\n"
S = S.replace("PLACEHOLDER_STUDNIE\n", _sk)
assert "PLACEHOLDER_STUDNIE" not in S

# --- BOSS INTRO: studnie wg sluz, Topielica wynurza sie z sadzawki
S += f"""
{PID}_boss_intro:
  Skills:
""" + spawn("L002_ZGNILA_WODA", lad(*EMITER["arena"]))
for i, (k, (sx, sz)) in enumerate(sorted(ST.items())):
    if k == "D":
        S += summ("L002_STUDNIA", (sx, YL + 1, sz))
        continue
    n = {"A": 1, "B": 2, "C": 3}[k]
    S += (f"  - summon{{m=L002_STUDNIA;level=1;a=1;r=0}} @Location{{c={sx},{YL + 1},{sz}}} ?variableequals{{var=world.l002_sluza{n};value=0}}\n"
          f'  - command{{c="execute in minecraft:{POD} run fill {sx - 1} {DNO} {sz - 1} {sx + 1} {YW} {sz + 1} minecraft:mud replace minecraft:water"}} ?variableequals{{var=world.l002_sluza{n};value=1}}\n')
S += f"""  - summon{{m=L002_TOPIELICA;level=1;a=1;r=0}} @Location{{c={TOP[0]},{YW + 1},{TOP[1]}}}
  - particles{{p=splash;a=200;hs=3;vs=2;y=1}} {c((TOP[0], YW + 1, TOP[1]))}
  - sound{{s=entity.elder_guardian.curse;v=1.5;p=0.6}} @PlayersInRadius{{r=40}}
  - title{{title="<dark_green><bold>Topielica</bold></dark_green>";subtitle="<gray>Matka Mułu wynurza się z sadzawki</gray>";fadein=5;duration=50;fadeout=15}} @PlayersInRadius{{r=40}}
  - message{{m="<gray>Studnie wokół sadzawki bulgoczą - stamtąd będzie się wynurzać. Każda otwarta śluza zasypała jedną.</gray>"}} @PlayersInRadius{{r=40}}

{PID}_koniec:
  Skills:
  - summon{{m=L002_FINAL;level=1;a=1;r=0}} {c((0, YL + 3, -110))}
"""
jx0, jx1, jz0, jz1 = J
S += """
# po Topielicy: jezioro opada i odslania wieze Archiwum (Loch 003), nagroda dla calej druzyny
L002_FINAL_SEKWENCJA:
  Skills:
  - message{m="<green>Topielica pada w muł.</green> <gray>Bagno cichnie.</gray>"} @PlayersInRadius{r=120}
  - delay 40
""" + ex("time set 22500") + "  - sound{s=block.beacon.deactivate;v=1.5;p=0.6} @PlayersInRadius{r=120}\n"
for y in range(YW, SP["dno_jeziora"], -1):
    S += fill_woda((jx0, jx1, jz0, jz1), y) + "  - sound{s=block.bubble_column.whirlpool_inside;v=1.5;p=0.6} @PlayersInRadius{r=120}\n  - delay 5\n"
S += ex("time set 0") + ex("weather clear") + f"""  - summon{{m=L002_SWIT;level=1;a=1;r=0}} {c((0, YL + 1, -92))}
  - summon{{m=L002_SWIT;level=1;a=1;r=0}} {c((20, YL + 1, -60))}
  - title{{title="<yellow>Woda opada</yellow>";subtitle="<gray>Z jeziora wyłania się zatopiona wieża</gray>";fadein=10;duration=70;fadeout=20}} @PlayersInRadius{{r=120}}
  - delay 80
  - message{{m="<aqua>Rybaczka Wela</aqua> <gray>(z oddali) - Wieża Archiwum! Dziadek mówił, że pod bagnem leży całe miasto ksiąg...</gray>"}} @PlayersInWorld
  - delay 40
  # --- NAGRODA KONCOWA: baza + premia za sekrety, dla calej druzyny
  - command{{c="mmocore admin exp give <target.name> main 100"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_CLAY_LUMP <target.name> 3"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_FLAX_FIBER <target.name> 3"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_FISH_OIL <target.name> 2"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_VENOM_SAC <target.name> 1"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_WATER_ESSENCE <target.name> 1"}} @PlayersInWorld ?variableequals{{var=world.l002_sekrety;value=3}}
  - command{{c="mi give MATERIAL PROF_WATER_ESSENCE <target.name> 2"}} @PlayersInWorld ?variableequals{{var=world.l002_sekrety;value=4}}
  - command{{c="mi give MATERIAL PROF_WATER_ESSENCE <target.name> 2"}} @PlayersInWorld ?variableequals{{var=world.l002_sekrety;value=5}}
  - command{{c="mi give MATERIAL PROF_PEARL <target.name> 1"}} @PlayersInWorld ?variableequals{{var=world.l002_sekrety;value=5}}
  - command{{c="mmocore admin exp give <target.name> main 100"}} @PlayersInWorld ?variableequals{{var=world.l002_sekrety;value=5}}
  - message{{m="<gold>Łup z Bagien trafia do plecaka.</gold> <gray>Znalezione sekrety <gold><world.var.l002_sekrety>/5</gold> - komplet daje perłę i premię.</gray>"}} @PlayersInWorld
  - title{{title="<light_purple>Loch 003 czeka</light_purple>";subtitle="<gray>Zatopione Ruiny Archiwum - wkrótce</gray>";fadein=10;duration=90;fadeout=20}} @PlayersInWorld
  - delay 60
  - remove @self
"""
_bez = [l for l in S.splitlines() if "summon{" in l and "level=1" not in l and not l.lstrip().startswith("#")]
assert not _bez, f"summon bez level=1: {_bez}"
_sel = [l for l in S.splitlines() + MOBS.splitlines() if re.search(r"@[ae]\[", l) and "distance=" not in l]
assert not _sel, f"selektor bez distance= (trafi inna instancje): {_sel}"

# ---------------- zapis
shutil.rmtree(OUT, ignore_errors=True)
md = f"{OUT}/md/{PID}"
os.makedirs(md)
open(f"{md}/config.yml", "w").write(cfg)
open(f"{md}/gamerules.yml", "w").write(GRL)
open(f"{md}/functions.yml", "w").write(FUN)
# bez entities/: przy wypieku odpadaja trzcina/grzyby/lilie -> 819 przedmiotow na ziemi w KAZDEJ instancji (21.10); moby rozstawiaja skille
for d in ("region", "poi"):
    src = f"{KUZNIA}/{SWIAT}/{d}"
    if os.path.isdir(src):
        shutil.copytree(src, f"{md}/{d}")
assert glob.glob(f"{md}/region/*.mca"), "brak regionow"
os.makedirs(f"{OUT}/mm/mobs"); os.makedirs(f"{OUT}/mm/skills")
open(f"{OUT}/mm/mobs/Loch_002.yml", "w").write(MOBS)
open(f"{OUT}/mm/skills/Loch_002.yml", "w").write(S)
json.dump({"start": START, "P": P, "korby": KORBY, "borek": BOREK_KOKON, "wela": WELA}, open(f"{OUT}/punkty.json", "w"))
print("zbudowano:", OUT, "| regiony:", len(glob.glob(f"{md}/region/*.mca")), "| funkcji:", len(fns))
