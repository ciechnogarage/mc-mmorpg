#!/usr/bin/env python3
"""Loch 001 Kwietna Polana - FRAGMENT 1 (sekcje 1-2, starcia A-C) jako mapa MythicDungeons + moby/skille MythicMobs.

Spec: /home/dev/Desktop/Przemek/MC/docs/lochy/loch-001-kwietna-polana.md (§2 wersja 2, §3.2 starcia).
Uzycie: build_loch001_md.py <swiat_kuzni>   -> kuznia/levels/loch_001_md/{md/loch_001, mm/...}

Roznice wzgledem build_md.py (poziom_01):
  * teren ZAMROZONY: regiony upieczonego swiata kuzni kopiowane do szablonu, generator mythicdungeons:void.
    Wejscie nic nie generuje (poziom_01 generowal Irisa przy wejsciu -> klient wisial ~4 s -> kick "floating").
  * moby maja ZASTEPCZY wyglad vanilli (modele ME w innej sesji) - zachowanie wg roli ze spec §3.
  * warianty B starc losuje MythicMobs (randomskill), nie funkcja MD Random (brak dzialajacego przykladu formatu).
Zasady z pomiarow (pamiec mythicdungeons-funkcje-bez-edytora): 1 funkcja na blok, zero delay w skillach wolanych
przez MD, summon zawsze level=1, os=true; DungeonStart odpala sie przed graczem -> TriggerDistance przy starcie.
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__))); from kuznia_sciezki import DATA, LEVELS  # noqa: E402
import os, re, shutil, sys, glob

SWIAT = sys.argv[1]
KUZNIA = DATA
DEFAULT_CFG = "/opt/docker/mc/data/world1/plugins/MythicDungeons/maps/default-config.yml"
OUT = f"{LEVELS}/loch_001_md"
PID = "loch_001"
import json as _json
ZAKATEK = _json.load(open(f"{LEVELS}/loch001_zakatek.json"))   # sciana korzeni + skarb
OKR = _json.load(open(f"{LEVELS}/loch001_okruszki.json"))       # latarnie, slupki zagadki, okruszki

# ---------------- miejsca (x, y_funkcji, z); grunt zmierzony w swiecie kuzni (y=100-102)
START = (0.5, 102.0, 126.5)
P = {
    "przybycie": (0, 103, 126), "oboz": (2, 102, 118),
    "A": (24, 103, 98), "B": (38, 101, 74), "C": (51, 103, 63),
    "latarnia": (56, 105, 90),
    "populacja": (0, 104, 127),                     # obok przybycia - rozstawia moby i wezly na starcie
    "skarb": tuple(ZAKATEK["skarb"]),                 # zakatek za sciana korzeni (loch001_zakatek.py)
    "kwiat_A": (61, 104, 69),
    "szept_kwiat": (61, 105, 71), "szept_zakatek": (63, 103, 84), "szept_boss": (58, 104, 51),
    "okruszek_plecak": (13, 104, 95), "okruszek_tarcza": (57, 104, 66),
    "skrytka_2": (20, 103, 78), "okruszek_plecak_2": (24, 104, 83), "skrytki_budz": (16, 104, 105),
    "ziele_1": (-4, 102, 108), "ziele_2": (22, 103, 104), "ziele_3": (36, 103, 90), "skrytka": (10, 103, 92), "dziennik": (55, 102, 56),   # znajdzki (bloki z loch001_rekwizyty.py)                      # szczyt slupka (chiseled_stone_bricks); latarnia staje na nim
    "koniec": (58, 102, 42), "koniec_wyjscie": (58, 103, 42),
    # krytyka 29.09: przy szczatkach (55,57) odpalaly naraz 4 wyzwalacze -> boss dalej (58,40), intro r9 przy bossie,
    # szept miedzy szczatkami a bossem, zasadzka + Korzeniak PRZED szczatkami (51,63), checkpoint przed bossem
    "boss_intro": (58, 104, 45), "boss": (58, 102, 40), "checkpoint_boss": (56, 101, 53), "latarnia_budz": (56, 104, 93),
}

# ---------------- config.yml
cfg = open(DEFAULT_CFG).read()
def setkey(key, val):
    global cfg
    new, n = re.subn(rf"(\n\s*{key}:)[^\n]*", rf"\1 {val}", cfg, count=1)
    assert n == 1, key; cfg = new
setkey("ChunkGenerator", "mythicdungeons:void")
setkey("DisplayName", "'&dLoch 001 &8» &aKwietna Polana'")
setkey("ShowTitleOnStart", "false")
setkey("Gamemode", "ADVENTURE")
setkey("PlayerLives", "0")          # 0 = bez limitu; upadek w pustke -> powrot do ogniska (punkt kontrolny)
setkey("MaxInstances", "3")
setkey("CleanupDelay", "0")
setkey("MinPartySize", "1"); setkey("MaxPartySize", "4")
cfg = cfg.replace("    - md leave\n", "    - md leave\n    - md lives\n    - md stuck\n    - help\n    - msg\n    - r\n    - tell\n", 1)
assert "    - md lives" in cfg
start = (f"  StartLocation:\n    x: {START[0]}\n    y: {START[1]}\n    z: {START[2]}\n"
         f"    pitch: 8.0\n    yaw: 180.0\n")
cfg = cfg.replace("\n  # Where the player appears after leaving", "\n" + start + "  # Where the player appears after leaving", 1)
# powrot: domyslny MD (tam, skad gracz wyruszyl). ExitLocation w configu NIE dziala: mapy MD wczytuja sie przed
# swiatem glownym -> "invalid world (World not loaded)" (zmierzone w kuzni 2026-09-29).
assert "StartLocation:" in cfg

# gamerules.yml: KOMPLET regul (jak zapisuje edytor MD). Z brakujaca regula MD rzuca NPE przy smierci gracza
# (AbstractDungeon.getGamerule(...) is null - zmierzone w kuzni 2026-09-27). Lista = reguly Papera 1.21.11 odpytane z konsoli.
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
            "mob_griefing": "false", "random_tick_speed": "0", "immediate_respawn": "true",
            # zadnych naturalnych mobow w lochu - tylko nasze starcia (MM summon nie podlega tym regulom)
            "spawn_mobs": "false", "spawn_monsters": "false", "spawn_phantoms": "false", "spawn_patrols": "false",
            "spawn_wandering_traders": "false", "spawn_wardens": "false", "raids": "false",
            "tnt_explodes": "false", "spread_vines": "false", "show_advancement_messages": "false"})
GR = ("Version: 2\nGamerule:\n  minecraft:\n" + "".join(f"    {k}: {v}\n" for k, v in sorted(REG.items()))
      + "Difficulty: NORMAL\nWorldTime: 12000\nWorldWeather: CLEAR\n")

# ---------------- functions.yml
def loc(x, y, z): return (f"  location:\n    ==: org.bukkit.Location\n    x: {float(x)}\n    y: {float(y)}\n"
                          f"    z: {float(z)}\n    pitch: 0.0\n    yaw: 0.0\n")
TT = ("  targetType:\n    ==: net.playavalon.mythicdungeons.api.parents.FunctionTargetType\n"
      "    value: PARTY\n")
def t_dist(r, each=False, delay=0):
    return (f"  trigger:\n    ==: net.playavalon.mythicdungeons.dungeons.triggers.TriggerDistance\n"
            f"    delayTicks: {delay}\n    forEachPlayer: {str(each).lower()}\n    limitToRoom: false\n    count: 1\n"
            f"    allowRetrigger: false\n    radius: {float(r)}\n    conditions: []\n")
def skill_fn(skill, xyz, trig):
    return (f"- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionMythicSkill\n"
            f"  mythicSkill: {skill}\n" + TT + loc(*xyz) + trig)
def t_mobdeath(mob, delay=0):
    return (f"  trigger:\n    ==: net.playavalon.mythicdungeons.dungeons.triggers.TriggerMythicMobDeath\n"
            f"    mob: {mob}\n    delayTicks: {delay}\n    limitToRoom: false\n    count: 1\n"
            f"    allowRetrigger: false\n    radius: 0.0\n    conditions: []\n")
BOSS = "L001_STARY_KORZEN"
def checkpoint(xyz, trig):
    return ("- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionCheckpoint\n  savePoint: false\n"
            + TT + loc(*xyz) + "  pitch: 0.0\n" + trig)
fns = [
    skill_fn(f"{PID}_przybycie", P["przybycie"], t_dist(8)),
    checkpoint(P["oboz"], t_dist(8, each=True)),
    skill_fn(f"{PID}_starcie_A", P["A"], t_dist(11)),
    # Moby stoja na mapie od startu (wlasciciel 2026-09-27: "nie podoba mi sie, ze moby spawnuja sie jak podbiegniesz,
    # chyba ze to ambush"). Zasadzki: A (horda z ziemi) i C (wilki z krzakow).
    skill_fn(f"{PID}_populacja", P["populacja"], t_dist(8)),
    skill_fn(f"{PID}_zasadzka_C", P["C"], t_dist(5)),
    checkpoint(P["checkpoint_boss"], t_dist(6, each=True)),
    # Kwiat Gaju odslania sie zagadka slupkow (skill L001_ZAGADKA_*), nie podejsciem; tu tylko podpowiedz i szepty
    skill_fn(f"{PID}_szept_kwiat", P["szept_kwiat"], t_dist(7)),
    skill_fn(f"{PID}_szept_zakatek", P["szept_zakatek"], t_dist(4)),
    skill_fn(f"{PID}_szept_boss", P["szept_boss"], t_dist(5)),
    skill_fn(f"{PID}_okruszek_plecak_w1", P["okruszek_plecak"], t_dist(3.5)),
    skill_fn(f"{PID}_okruszek_plecak_w2", P["okruszek_plecak_2"], t_dist(3.5)),
    skill_fn(f"{PID}_okruszek_tarcza", P["okruszek_tarcza"], t_dist(3.5)),
    # Ziele Gaju zbierane WEJSCIEM w swiecaca kepe: klik w niewidzialnego moba z populacji bywal martwy (~1/3 przebiegow)
    skill_fn(f"{PID}_ziele_zbior", P["ziele_1"], t_dist(1.8)),
    skill_fn(f"{PID}_ziele_zbior", P["ziele_2"], t_dist(1.8)),
    skill_fn(f"{PID}_ziele_zbior", P["ziele_3"], t_dist(1.8)),
    # skrytki i okruszki stawiane przy podejsciu do lasu: setblock/summon na starcie trafial w niewczytany chunk (zmierzone)
    skill_fn(f"{PID}_skrytki_budz", P["skrytki_budz"], t_dist(12)),
    skill_fn(f"{PID}_skrytka_w1", P["skrytka"], t_dist(2.8)),
    skill_fn(f"{PID}_skrytka_w2", P["skrytka_2"], t_dist(2.8)),
    skill_fn(f"{PID}_dziennik", P["dziennik"], t_dist(2.8)),
    skill_fn(f"{PID}_skarb", P["skarb"], t_dist(2.8)),
    # latarnia zapala sie KLIKNIECIEM (krytyka: sekret != przejsc obok); knot przyzywany przy podejsciu (wzorzec krysztalow)
    skill_fn(f"{PID}_latarnia_budz", P["latarnia_budz"], t_dist(8)),
    # kulminacja: Stary Korzen stoi od startu na lace przed Obozem Ciernia; intro przy podejsciu, koniec po jego smierci
    skill_fn(f"{PID}_boss_intro", P["boss_intro"], t_dist(9)),
    skill_fn(f"{PID}_koniec", P["koniec"], t_mobdeath(BOSS, 20)),
    ("- ==: net.playavalon.mythicdungeons.dungeons.functions.FunctionFinishDungeon\n  leave: true\n"
     + TT + loc(*P["koniec_wyjscie"]) + t_mobdeath(BOSS, 480)),   # 24 s: swit, bebny, strzaly, podsumowanie
]
# straznik: kazda funkcja na innym bloku (MD przypina funkcje do bloku - zmierzone)
_bloki = [tuple(map(int, p)) for k, p in P.items() if k not in ("B", "boss", "kwiat_A", "latarnia")]
assert len({P["ziele_1"], P["ziele_2"], P["ziele_3"]}) == 3
assert len(set(_bloki)) == len(_bloki), "dwie funkcje na jednym bloku"
FUN = "Version: 1\nFunctions:\n" + "".join(fns)

NIEWIDZIALNY_TXT = """  Options:
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
# ---------------- MythicMobs: moby (wyglad zastepczy, zachowanie wg roli)
MOBS = """# Loch 001 - moby fragmentu 1. WYGLAD ZASTEPCZY (vanilla Type) do czasu wdrozenia modeli ModelEngine.
# Liczby pod swieza postac z Kijem Nowicjusza (4 obr.): zwykly mob 4-6 ciosow, Pniaczek 1 cios (spec §4, horda = chwila mocy).
L001_PNIACZEK:
  Type: HUSK
  Display: '&2Pniaczek'
  Health: 4
  Damage: 1
  Faction: gaj
  Options:
    Baby: true
    PreventItemPickup: true
    Despawn: persistent        # bez tego vanilla usuwa moby >32 bl. od gracza - rozstawione grupy znikaly (zmierzone)
    FollowRange: 10
    MovementSpeed: 0.3
    PreventOtherDrops: true
    Silent: false
  Equipment:
  - OAK_LEAVES HEAD
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_RAW_TIMBER} 1 0.25
  # stoi w miejscu (bez randomstroll) do czasu, az gracz podejdzie na FollowRange - inaczej rozchodzi sie po mapie (zmierzone)
  AIGoalSelectors:
  - clear
  - meleeattack
  - lookatplayers
  - randomlookaround
  Skills:
  - sound{s=entity.bat.hurt;v=0.6;p=1.8} @self ~onDamaged
  - sound{s=entity.fox.squeak;v=0.8;p=1.6} @self ~onDeath
  - particles{p=block;m=OAK_LOG;a=12;hs=0.3;vs=0.3;s=0.05} @self ~onDeath
  - skill{s=L001_PNIACZEK_SMIERC} @self ~onDeath
  - command{c="mmocore admin exp give <trigger.name> hunting 2"} @trigger ~onDeath ?~isplayer
  - command{c="mmocore admin exp give <trigger.name> main 6"} @trigger ~onDeath ?~isplayer

L001_WILK:
  Type: WOLF
  Display: '&6Wilk Gajowy'
  Health: 20
  Damage: 3
  Faction: gaj
  Options:
    Despawn: persistent        # bez tego vanilla usuwa moby >32 bl. od gracza - rozstawione grupy znikaly (zmierzone)
    FollowRange: 14
    MovementSpeed: 0.3
    PreventOtherDrops: true
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_BEAST_SCRAP} 1 0.5
  - mmoitem{type=MATERIAL;id=PROF_BEAST_BONE} 1 0.3
  Skills:
  - skill{s=L001_WILK_WSCIEKLY} @self ~onSpawn
  - skill{s=L001_WILK_SKOK} @target ~onTimer:80
  - command{c="mmocore admin exp give <trigger.name> hunting 8"} @trigger ~onDeath ?~isplayer
  - command{c="mmocore admin exp give <trigger.name> main 15"} @trigger ~onDeath ?~isplayer
  # wataha trzyma sie razem i czeka (bez randomstroll) - wilki rozchodzily sie z polany (zmierzone na world1)
  AIGoalSelectors:
  - clear
  - meleeattack
  - lookatplayers
  - randomlookaround
  AITargetSelectors:
  - clear
  - attacker
  - players

L001_WILK_WATAHA:
  Type: WOLF
  Display: '&6Wilk Gajowy'
  Health: 20
  Damage: 3
  Faction: gaj
  Options:
    Despawn: persistent        # bez tego vanilla usuwa moby >32 bl. od gracza - rozstawione grupy znikaly (zmierzone)
    FollowRange: 14
    MovementSpeed: 0.3
    PreventOtherDrops: true
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_BEAST_SCRAP} 1 0.5
  - mmoitem{type=MATERIAL;id=PROF_BEAST_BONE} 1 0.3
  Skills:
  - skill{s=L001_WILK_WSCIEKLY} @self ~onSpawn
  - addtag{tag=l001_wataha} @self ~onSpawn
  # POMINIETA walka ma konsekwencje: wataha z polany dobiega do Starego Korzenia, gdy ten wezwie pomocy
  - skill{s=L001_WATAHA_WEZWANIE} @self ~onSignal:wataha
  - skill{s=L001_WILK_SKOK} @target ~onTimer:80
  - command{c="mmocore admin exp give <trigger.name> hunting 8"} @trigger ~onDeath ?~isplayer
  - command{c="mmocore admin exp give <trigger.name> main 15"} @trigger ~onDeath ?~isplayer
  # wataha trzyma sie razem i czeka (bez randomstroll) - wilki rozchodzily sie z polany (zmierzone na world1)
  AIGoalSelectors:
  - clear
  - meleeattack
  - lookatplayers
  - randomlookaround
  AITargetSelectors:
  - clear
  - attacker
  - players

L001_STARY_KORZEN:
  Type: ZOMBIE
  Display: '&2&lStary Korzeń'
  # 80 -> 140 (krytyka: z blogoslawienstwami nie mial szans); do strojenia po pomiarze sonda_dps
  Health: 140
  Damage: 5
  Armor: 2
  Faction: gaj
  Options:
    Despawn: persistent
    FollowRange: 12
    MovementSpeed: 0.23
    KnockbackResistance: 0.8
    PreventOtherDrops: true
    PreventSunburn: true
    PreventItemPickup: true
    AlwaysShowName: true
  BossBar:
    Enabled: true
    Title: '&2&lStary Korzeń'
    Range: 40
    Color: GREEN
    Style: SEGMENTED_10
  Equipment:
  - MOSS_BLOCK HEAD
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_RAW_TIMBER} 3 1
  - mmoitem{type=MATERIAL;id=PROF_MEDICINAL_HERB} 1-2 0.7
  - mmoitem{type=MATERIAL;id=PROF_ROUGH_CRYSTAL} 1 0.3
  AIGoalSelectors:
  - clear
  - meleeattack
  - lookatplayers
  - randomlookaround
  AITargetSelectors:
  - clear
  - attacker
  - players
  Skills:
  # olbrzym: skala 2.2 (atrybut vanilli, widac go z daleka nad laka)
  # skala od razu w onSpawn (<caster.uuid>): wersja z delay nie dzialala - boss stoi ~100 bl. od startu, jego chunk
  # przestaje tykac, opozniony skill nie znajduje moba (zmierzone w kuzni, simulation-distance 4)
  - command{c="attribute <caster.uuid> minecraft:scale base set 2.2"} @self ~onSpawn
  - addtag{tag=l001_boss} @self ~onSpawn
  - skill{s=L001_KORZEN_SLAM} @target ~onTimer:50
  # przeciw kitowaniu: korzenie wyrywaja sie pod graczem na dystans (zapowiedz 1.2 s)
  - skill{s=L001_KORZEN_WYRWA} @target ~onTimer:70
  # KORA: przerywalny kanal leczenia (+30 HP) - 3 ciosy w trakcie kanalu kruszą kore
  - skill{s=L001_KORA} @self ~onTimer:100
  - variableadd{var=caster.kora_ciosy;amount=1} @self ~onDamaged
  - skill{s=L001_KORA_PEKA} @self ~onTimer:5 ?variableequals{var=caster.kora;value=1} ?variableequals{var=caster.kora_ciosy;value=3}
  - skill{s=L001_KORA_PEKA} @self ~onTimer:5 ?variableequals{var=caster.kora;value=1} ?variableequals{var=caster.kora_ciosy;value=4}
  - skill{s=L001_KORA_PEKA} @self ~onTimer:5 ?variableequals{var=caster.kora;value=1} ?variableequals{var=caster.kora_ciosy;value=5}
  - skill{s=L001_KORZEN_STREFA} @self ~onTimer:40
  # co sekunde sprawdza HP (onDamaged z warunkiem nie odpalal sie ani z h=, ani z p= - zmierzone); raz dzieki Cooldown 9999
  - skill{s=L001_KORZEN_DZIATWA} @self ~onTimer:20 ?healthpercent{p=<50%}
  - sound{s=entity.ravager.roar;v=1.2;p=0.6} @self ~onDeath
  - particles{p=block;m=OAK_LOG;a=120;hs=1.5;vs=2;s=0.1;y=1.5} @self ~onDeath
  - command{c="mmocore admin exp give <trigger.name> hunting 40"} @trigger ~onDeath ?~isplayer
  - command{c="mmocore admin exp give <target.name> main 150"} @PlayersInWorld ~onDeath

L001_WILCZYCA:
  Type: WOLF
  Display: '&6Wilczyca'
  Health: 22
  Damage: 3
  Faction: gaj
  Options:
    Despawn: persistent
    FollowRange: 10
    MovementSpeed: 0.3
    PreventOtherDrops: true
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_BEAST_SCRAP} 1 0.6
  AIGoalSelectors:
  - clear
  - meleeattack
  - lookatplayers
  - randomlookaround
  AITargetSelectors:
  - clear
  - attacker
  - players
  Skills:
  - skill{s=L001_WILK_WSCIEKLY} @self ~onSpawn
  - command{c="mmocore admin exp give <trigger.name> main 15"} @trigger ~onDeath ?~isplayer
  # szczenieta nie stoja przy ciele matki - uciekaja w gaszcz
  - particles{p=poof;a=20;hs=1.5;vs=0.5} @MobsInRadius{r=10;types=L001_WILCZE} ~onDeath
  - message{m="<gray>Szczenięta z piskiem uciekają w gąszcz.</gray>"} @PlayersInRadius{r=16} ~onDeath
  - remove @MobsInRadius{r=10;types=L001_WILCZE} ~onDeath

L001_LATARNIA_KNOT:
  Type: ZOMBIE
  Display: '<gold>Latarnia Gaju</gold> <gray>(PPM)</gray>'
  Health: 1
""" + NIEWIDZIALNY_TXT + """  Equipment:
  - SOUL_LANTERN HEAD
  Skills:
  - particles{p=glow;a=3;hs=0.3;vs=0.3;s=0.01;y=1.7} @self ~onTimer:15
  - skill{s=loch_001_latarnia_1} @trigger ~onInteract

L001_WILCZE:
  Type: WOLF
  Display: '&7Wilczę'
  Health: 8
  Faction: gaj
  Options:
    Baby: true
    Despawn: persistent
    PreventOtherDrops: true
  AIGoalSelectors:
  - clear
  - lookatplayers
  - randomlookaround

# niewidzialne zrodla klimatu: czasteczki i dzwieki w stalych miejscach (bez AI, trwale)
L001_KLIMAT_LAS:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
  Skills:
  - particles{p=spore_blossom_air;a=12;hs=9;vs=3;s=0;y=2} @self ~onTimer:20
  - particles{p=glow;a=10;hs=10;vs=3;s=0.01;y=2} @self ~onTimer:10 ?variableequals{var=world.l001_mod;value=1}
  - particles{p=falling_spore_blossom;a=4;hs=7;vs=1;s=0;y=6} @self ~onTimer:30
  - sound{s=block.azalea_leaves.step;v=0.5;p=0.6} @self ~onTimer:140
  - sound{s=entity.fox.ambient;v=0.4;p=1.3} @self ~onTimer:420
L001_KLIMAT_LAKA:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
  Skills:
  - particles{p=cherry_leaves;a=6;hs=10;vs=2;s=0;y=3} @self ~onTimer:25
  - sound{s=entity.bee.loop;v=0.3;p=1.1} @self ~onTimer:200

# BLOGOSLAWIENSTWA GAJU: premie za sekrety do konca przejscia, odnawiane co 5 s (przezywaja smierc gracza)
L001_BLOGOSLAWIENSTWA:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
  Skills:
  - potion{type=REGENERATION;duration=140;level=0;hasParticles=false} @PlayersInRadius{r=200} ~onTimer:100 ?variableequals{var=world.l001_bl_kwiat;value=1}
  - potion{type=RESISTANCE;duration=140;level=0;hasParticles=false} @PlayersInRadius{r=200} ~onTimer:100 ?variableequals{var=world.l001_bl_kora;value=1}
# swietliki: slad od szczatkow poszukiwacza do latarni (po przeczytaniu dziennika)
L001_SWIETLIK:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
  Skills:
  - addtag{tag=l001_swietlik} @self ~onSpawn
  - particles{p=glow;a=6;hs=0.8;vs=0.8;s=0.01;y=1.5} @self ~onTimer:10
  - particles{p=end_rod;a=1;hs=0.6;vs=0.6;s=0.01;y=1.5} @self ~onTimer:20
# klimat po zwyciestwie: platki i blask zamiast zarodnikow
L001_KLIMAT_SWIT:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
  Skills:
  - particles{p=cherry_leaves;a=14;hs=9;vs=3;s=0;y=4} @self ~onTimer:15
  - particles{p=end_rod;a=2;hs=8;vs=3;s=0.01;y=2} @self ~onTimer:30

# bebny Plemienia Ciernia po smierci Starego Korzenia: sekwencja z delay (caster = ten mob, nie funkcja MD)
L001_BEBNY:
  Type: ARMOR_STAND
  Options:
    Invisible: true
    Marker: true
    Invincible: true
    Silent: true
    Despawn: persistent
  Skills:
  - skill{s=L001_BEBNY_SEKWENCJA} @self ~onSpawn

L001_KORZENIAK:
  Type: ZOMBIE
  Display: '&aKorzeniak'
  Health: 24
  Damage: 3
  Faction: gaj
  Options:
    Despawn: persistent        # bez tego vanilla usuwa moby >32 bl. od gracza - rozstawione grupy znikaly (zmierzone)
    FollowRange: 12
    MovementSpeed: 0.18
    PreventOtherDrops: true
    PreventSunburn: true
    PreventItemPickup: true
  Equipment:
  - OAK_LEAVES HEAD
  Drops:
  - mmoitem{type=MATERIAL;id=PROF_RAW_TIMBER} 1 0.6
  - mmoitem{type=MATERIAL;id=PROF_MEDICINAL_HERB} 1 0.2
  # stoi w miejscu (bez randomstroll) do czasu, az gracz podejdzie na FollowRange - inaczej rozchodzi sie po mapie (zmierzone)
  AIGoalSelectors:
  - clear
  - meleeattack
  - lookatplayers
  - randomlookaround
  Skills:
  # sprawdza co 2 s, strefa gdy gracz blisko (cooldown 7 s w skillu) - lekcja ma przyjsc, gdy gracz stoi obok
  - skill{s=L001_KORZENIAK_STREFA} @self ~onTimer:40
  - command{c="mmocore admin exp give <trigger.name> hunting 8"} @trigger ~onDeath ?~isplayer
  - command{c="mmocore admin exp give <trigger.name> main 15"} @trigger ~onDeath ?~isplayer
  AITargetSelectors:
  - clear
  - attacker
  - players
"""

NIEWIDZIALNY = """  Options:
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
MOBS += """
# Ranny druid przy ognisku - prosi o Ziele Gaju, za 2 garscie parzy napar (oslona na Starego Korzenia)
L001_DRUID:
  Type: VILLAGER
  Display: '<green>Ranny druid</green>'
  Health: 20
  Options:
    NoAI: true
    Invincible: true
    Despawn: persistent
    AlwaysShowName: true
    PreventOtherDrops: true
  Skills:
  - skill{s=L001_DRUID_ROZMOWA} @trigger ~onInteract

# Ziele Gaju - swiecace kepy przy trasie (klik prawym, znika po zebraniu)
L001_ZIELE:
  Type: ZOMBIE
  Display: '<green>Ziele Gaju</green>'
  Health: 1
""" + NIEWIDZIALNY + """  Equipment:
  - FLOWERING_AZALEA HEAD
  Skills:
  - particles{p=glow;a=3;hs=0.4;vs=0.4;s=0.01;y=1.7} @self ~onTimer:15
"""
SLUPKI = sorted(OKR["slupki"], key=lambda f: f["wys"])      # kolejnosc rozwiazania: od najnizszego
for k, f in enumerate(SLUPKI, 1):
    MOBS += f"""
# Krysztal na slupku zagadki ({f['wys']} bl.) - dotknac od najnizszego slupka do najwyzszego
L001_KAMIEN_{k}:
  Type: ZOMBIE
  Display: '<light_purple>Kamień Gaju</light_purple>'
  Health: 1
""" + NIEWIDZIALNY + f"""  Equipment:
  - AMETHYST_CLUSTER HEAD
  Skills:
  - particles{{p=glow;a=2;hs=0.3;vs=0.3;s=0.01;y=1.7}} @self ~onTimer:20
  - skill{{s=L001_ZAGADKA_{k}}} @trigger ~onInteract
"""

# ---------------- MythicMobs: skille
SKILLS = f"""# Loch 001, fragment 1. Skille wolane przez funkcje MD (caster = punkt funkcji): BEZ delay, summon level=1 + os=true.
# Skille mobow (caster = mob) moga miec delay.
{PID}_przybycie:
  Skills:
  # gamerules.yml nie jest stosowany do recznego szablonu (zmierzone) - reguly z konsoli na swiecie instancji
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:gamerule fall_damage false"}}
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:gamerule keep_inventory true"}}
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:gamerule advance_time false"}}
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:gamerule advance_weather false"}}
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:gamerule mob_griefing false"}}
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:weather clear"}}
  - setvariable{{var=world.l001_sekrety;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_bl_kwiat;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_bl_kora;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_ziola;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_napar;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_zagadka;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_latarnia;val=0;type=INTEGER}} @self
  - title{{title="<light_purple>Loch 001</light_purple>";subtitle="<green>Kwietna Polana</green>";fadein=15;duration=60;fadeout=20}} @PlayersInRadius{{r=40}}
  - sound{{s=block.amethyst_block.chime;v=1;p=1.2}} @PlayersInRadius{{r=40}}
  - message{{m="<green>Ranny druid</green> <gray>woła cię od ogniska - Strażnik Gaju oszalał. Podejdź, zanim ruszysz na szlak.</gray>"}} @PlayersInRadius{{r=40}}

# A: horda Pniaczkow - chwila mocy. Wariant B: mniej Pniaczkow + ukryta Purchawka (fragment 1 nie ma jeszcze Purchawki
# -> B = 8 Pniaczkow z drugiej strony sciezki).
{PID}_starcie_A:
  Skills:
  - sound{{s=block.azalea_leaves.break;v=1.2;p=0.7}} @PlayersInRadius{{r=30}}
  - particles{{p=block;m=OAK_LEAVES;a=60;hs=6;vs=1;s=0.05}} @self
  - randomskill{{skills={PID}_A1,{PID}_A2}} @self
# horda w 3 kepach NA SCIEZCE (rozrzut r=7 w lesie wsadzal Pniaczka w pien - "suffocated in a wall", zmierzone na world1)
{PID}_A1:
  Skills:
  - summon{{m=L001_PNIACZEK;level=1;a=4;r=1.5;yr=8;os=true}} @Location{{c=14,102,100}}
  - summon{{m=L001_PNIACZEK;level=1;a=4;r=1.5;yr=8;os=true}} @Location{{c=20,102,94}}
  - summon{{m=L001_PNIACZEK;level=1;a=4;r=1.5;yr=8;os=true}} @Location{{c=26,102,88}}
{PID}_A2:
  Skills:
  - summon{{m=L001_PNIACZEK;level=1;a=3;r=1.5;yr=8;os=true}} @Location{{c=14,102,100}}
  - summon{{m=L001_PNIACZEK;level=1;a=3;r=1.5;yr=8;os=true}} @Location{{c=20,102,94}}
  - summon{{m=L001_PNIACZEK;level=1;a=2;r=1.5;yr=8;os=true}} @Location{{c=26,102,88}}

# POPULACJA na starcie: moby stoja na mapie i atakuja z bliska (FollowRange), wezly zasobow do zebrania.
# Wspolrzedne @Location z pomiaru gruntu (loch001_rekwizyty.py / sonda); os=true + yr=8 dosadza na grunt.
{PID}_populacja:
  Skills:
  # LOSOWANIA PRZY WEJSCIU (powtorki): nastroj dnia, kierunek zagadki, miejsce skrytki; zmienne oceny na zero
  - setvariable{{var=world.l001_zag_blad;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_zag_ok;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_kora_ok;val=0;type=INTEGER}} @self
  - setvariable{{var=world.l001_ocena;val=0;type=INTEGER}} @self
  # pora dnia TU, przed nastrojem: w przybyciu nadpisywala "Noc Swietlikow" (oba skille startuja w dowolnej kolejnosci)
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:time set 12000"}}
  - randomskill{{skills=L001_MOD_1,L001_MOD_2,L001_MOD_3,L001_MOD_4}} @self
  - randomskill{{skills=L001_ZAG_0,L001_ZAG_1}} @self
  - randomskill{{skills=L001_SKR_1,L001_SKR_2}} @self
  - skill{{s=L001_OKRUSZKI_TARCZA}} @self
  - randomskill{{skills={PID}_B1,{PID}_B2}} @self
  # C: Korzeniak na sciezce przed szczatkami (lekcja strefy) - razem z zasadzka, a przy szczatkach spokoj na dziennik
  - summon{{m=L001_KORZENIAK;level=1;a=1;r=1;yr=8;os=true}} @Location{{c=52,102,62}}
  # samotny wilk strzeze latarni w lesie
  - summon{{m=L001_WILK;level=1;a=1;r=1;yr=8;os=true}} @Location{{c=52,103,93}}
  # patrol Pniaczkow miedzy obozem a lasem - pierwsza, latwa walka
  - summon{{m=L001_PNIACZEK;level=1;a=3;r=1.5;yr=8;os=true}} @Location{{c=16,102,112}}
  # szabrownicy: 2 Pniaczki grzebia w rozbitym wozie przy obozie (pierwsza, latwa walka i usmiech)
  - summon{{m=L001_PNIACZEK;level=1;a=2;r=1;yr=8;os=true}} @Location{{c=13,102,108}}
  # straz skrytki w lesie
  - summon{{m=L001_PNIACZEK;level=1;a=2;r=1;yr=8;os=true}} @Location{{c=8,103,96}}
  # legowisko (plaski grunt x29-36 z63-69; (30,70) przy progu - wilczyca sie dusila): wilczyca ze szczeniakami (nie atakuja) - klimat, walka tylko z wilczyca
  - summon{{m=L001_WILCZYCA;level=1;a=1;r=1;yr=8;os=true}} @Location{{c=33,102,64}}
  - summon{{m=L001_WILCZE;level=1;a=2;r=1;yr=8;os=true}} @Location{{c=33,102,65}}
  # straz Kwiatu Gaju
  - summon{{m=L001_PNIACZEK;level=1;a=2;r=1;yr=8;os=true}} @Location{{c=59,102,72}}
  # kulminacja fragmentu: Stary Korzen stoi na lace przed Obozem Ciernia - widac go z daleka
  - summon{{m=L001_STARY_KORZEN;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=58,102,40}}
  # zwierzeta (zycie na mapie)
  - summon{{m=FOX;level=1;a=2;r=2;yr=8;os=true}} @Location{{c=44,102,84}}
  - summon{{m=RABBIT;level=1;a=3;r=3;yr=8;os=true}} @Location{{c=-4,102,114}}
  - summon{{m=BEE;level=1;a=3;r=2;yr=6;os=true}} @Location{{c=6,103,120}}
  # blogoslawienstwa (srodek fragmentu - chunk zawsze w zasiegu symulacji)
  - command{{c="mm mobs spawn L001_BLOGOSLAWIENSTWA 1 <caster.l.w>,30.5,102,85.5"}}
  - command{{c="mm mobs spawn L001_BLOGOSLAWIENSTWA 1 <caster.l.w>,4.5,101,118.5"}}
  - command{{c="mm mobs spawn L001_BLOGOSLAWIENSTWA 1 <caster.l.w>,52.5,102,56.5"}}
  # okruszki poprzednikow: stawiane w L001_SKR_1/2 (wariant skrytki), NIE w szablonie - item_display z szablonu dublowal sie
  # ranny druid przy ognisku obozu (klik prawym)
  # UWAGA: moby z ~onInteract/~onTimer przyzywane skillem funkcji MD bywaly martwe (krysztaly, ziele, emitery - zmierzone);
  # przez konsole (mm mobs spawn) dzialaja zawsze. Wysokosci = grunt+1 zmierzony w swiecie kuzni.
  - command{{c="mm mobs spawn L001_DRUID 1 <caster.l.w>,4.5,101,117.5"}}
  # Ziele Gaju: 3 swiecace kepy przy trasie (druid chce 2)
  - command{{c="mm mobs spawn L001_ZIELE 1 <caster.l.w>,-3.5,102,108.5"}}
  - command{{c="mm mobs spawn L001_ZIELE 1 <caster.l.w>,22.5,103,104.5"}}
  - command{{c="mm mobs spawn L001_ZIELE 1 <caster.l.w>,36.5,103,90.5"}}
  # zrodla klimatu
  - summon{{m=L001_KLIMAT_LAS;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=30,102,90}}
  - summon{{m=L001_KLIMAT_LAS;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=44,102,78}}
  - summon{{m=L001_KLIMAT_LAS;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=52,102,62}}
  - summon{{m=L001_KLIMAT_LAKA;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=4,102,112}}
  - summon{{m=L001_KLIMAT_LAKA;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=58,102,50}}
  # wezly zasobow (WezlyZasobow.yml, klik prawym): ziola przy obozie i na skraju lasu, grzyby w lesie, powalony dab
  - summon{{m=NODE_HERB_PATCH;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=-8,101,114}}
  - summon{{m=NODE_HERB_PATCH;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=36,101,58}}
  - summon{{m=NODE_MUSHROOM_RING;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=33,102,94}}
  - summon{{m=NODE_TIMBER_OAK;level=1;a=1;r=0;yr=8;os=true}} @Location{{c=58,102,78}}
# B: wataha odpoczywa na polanie w lesie. 2 Wilki + 3 Pniaczki albo 3 Wilki. Srodek plaskiego wnetrza polany (x34-43, z71-77 na y=100); przy krawedzi wilki wchodzily hitboxem w prog terenu i dusily sie (zmierzone).
{PID}_B1:
  Skills:
  - summon{{m=L001_WILK_WATAHA;level=1;a=2;r=1.5;yr=8;os=true}} @Location{{c=39,101,74}}
  - summon{{m=L001_PNIACZEK;level=1;a=3;r=1.5;yr=8;os=true}} @Location{{c=39,101,74}}
{PID}_B2:
  Skills:
  - summon{{m=L001_WILK_WATAHA;level=1;a=3;r=1.5;yr=8;os=true}} @Location{{c=39,101,74}}

# C: ZASADZKA przy szczatkach poszukiwacza - polowa wejsc: 2 Wilki wyskakuja z krzakow (Korzeniak stoi od startu).
{PID}_zasadzka_C:
  Skills:
  - message{{m="<gray>Coś szeleści w krzakach przy ścieżce.</gray>"}} @PlayersInRadius{{r=20}}
  - randomskill{{skills={PID}_C1,{PID}_C2}} @self
{PID}_C1:
  Skills:
  - sound{{s=block.sweet_berry_bush.break;v=1;p=0.7}} @PlayersInRadius{{r=20}}
{PID}_C2:
  Skills:
  - sound{{s=entity.wolf.growl;v=1;p=0.8}} @PlayersInRadius{{r=20}}
  - particles{{p=block;m=OAK_LEAVES;a=40;hs=4;vs=1;s=0.05}} @self
  # dwa punkty przy sciezce, przed i za graczem (rozrzut r=7 w lesie wsadzal wilka w drzewo - suffocated, zmierzone)
  - summon{{m=L001_WILK;level=1;a=1;r=1;yr=8;os=true}} @Location{{c=45,102,67}}
  - summon{{m=L001_WILK;level=1;a=1;r=1;yr=8;os=true}} @Location{{c=54,102,62}}

{PID}_skarb:
  Skills:
  - command{{c="mmocore admin exp give <target.name> main 20"}} @PlayersInWorld
  - variableadd{{var=world.l001_sekrety;amount=1}} @self
  - command{{c="execute in minecraft:<caster.l.w> run setblock {P['skarb'][0]} {P['skarb'][1]} {P['skarb'][2]} minecraft:barrel[facing=up,open=true]"}}
  - sound{{s=block.amethyst_block.resonate;v=1.2;p=1.2}} @PlayersInRadius{{r=8}}
  - particles{{p=end_rod;a=40;hs=1;vs=1.5;s=0.03;y=1}} @self
  - title{{title="<gold>Skarb Latarni</gold>";subtitle="<gray>Gaj wynagradza tych, którzy niosą światło</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInRadius{{r=8}}
  - message{{m="<gold>Skarb Latarni.</gold> <gray>Sekrety lochu <gold><world.var.l001_sekrety>/5</gold></gray>"}} @PlayersInRadius{{r=8}}
  - command{{c="mi give MATERIAL PROF_ROUGH_CRYSTAL <target.name> 2"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_ENCHANT_DUST <target.name> 2"}} @PlayersInWorld

# --- SZEPT GAJU (strzepy historii w miejscach) i OKRUSZKI poprzednikow (item_display z loch001_okruszki.py)
{PID}_latarnia_budz:
  Skills:
  # knot przyzwany z konsoli, gdy gracz jest blisko (moby z ~onInteract z populacji bywaly martwe - pamiec)
  - command{{c="mm mobs spawn L001_LATARNIA_KNOT 1 <caster.l.w>,{P['latarnia'][0]}.5,{P['latarnia'][1]+1},{P['latarnia'][2]}.5"}}
  - sound{{s=block.sculk_sensor.clicking;v=0.8;p=0.6}} @PlayersInRadius{{r=10}}
  - message{{m="<gray>Na omszałym słupku wisi zgaszona latarnia. Może da się ją rozpalić.</gray>"}} @PlayersInRadius{{r=10}}

{PID}_szept_kwiat:
  Skills:
  # krysztaly zagadki budza sie dopiero przy podejsciu: przyzwane na starcie (daleko od gracza) nie odbieraly klikniec
  # (~onInteract martwy w nr 1 i 2); przyzwane z konsoli, gdy gracz jest obok - dzialaja (zmierzone w kuzni 2026-09-28)
  - command{{c="mm mobs spawn L001_KAMIEN_1 1 <caster.l.w>,58.5,104,71.5"}}
  - command{{c="mm mobs spawn L001_KAMIEN_2 1 <caster.l.w>,61.5,104,74.5"}}
  - command{{c="mm mobs spawn L001_KAMIEN_3 1 <caster.l.w>,64.5,105,71.5"}}
  - particles{{p=end_rod;a=30;hs=3;vs=1;s=0.02;y=3}} @self
  - sound{{s=block.amethyst_cluster.place;v=1.2;p=0.8}} @PlayersInRadius{{r=10}}
  - sound{{s=ambient.cave;v=0.6;p=1.4}} @PlayersInRadius{{r=10}}
  - message{{m="<dark_aqua><italic>Szept Gaju... kamienie rosną ku światłu. Od najmniejszego.</italic></dark_aqua>"}} @PlayersInRadius{{r=10}} ?variableequals{{var=world.l001_zag_kier;value=0}}
  - message{{m="<dark_aqua><italic>Szept Gaju... kamienie kłaniają się ziemi. Od największego.</italic></dark_aqua>"}} @PlayersInRadius{{r=10}} ?variableequals{{var=world.l001_zag_kier;value=1}}
{PID}_szept_zakatek:
  Skills:
  - sound{{s=ambient.cave;v=0.6;p=1.2}} @PlayersInRadius{{r=8}}
  - message{{m="<dark_aqua><italic>Szept Gaju... tu spałem, zanim korzenie oszalały.</italic></dark_aqua>"}} @PlayersInRadius{{r=8}}
{PID}_szept_boss:
  Skills:
  - sound{{s=ambient.cave;v=0.7;p=0.8}} @PlayersInRadius{{r=12}}
  - message{{m="<dark_aqua><italic>Szept Gaju... był strażnikiem. Teraz jest więźniem własnych korzeni.</italic></dark_aqua>"}} @PlayersInRadius{{r=12}}
{PID}_skrytki_budz:
  Skills:
  - skill{{s=L001_SKR_1_BUDUJ}} @self ?variableequals{{var=world.l001_skr_w;value=1}}
  - skill{{s=L001_SKR_2_BUDUJ}} @self ?variableequals{{var=world.l001_skr_w;value=2}}
{PID}_skrytka_w1:
  Skills:
  - command{{c="execute in minecraft:<caster.l.w> run setblock {P['skrytka'][0]} {P['skrytka'][1]} {P['skrytka'][2]} minecraft:barrel[facing=up,open=true]"}} ?variableequals{{var=world.l001_skr_w;value=1}}
  - skill{{s={PID}_skrytka}} @self ?variableequals{{var=world.l001_skr_w;value=1}}
{PID}_skrytka_w2:
  Skills:
  - command{{c="execute in minecraft:<caster.l.w> run setblock {P['skrytka_2'][0]} {P['skrytka_2'][1]} {P['skrytka_2'][2]} minecraft:barrel[facing=up,open=true]"}} ?variableequals{{var=world.l001_skr_w;value=2}}
  - skill{{s={PID}_skrytka}} @self ?variableequals{{var=world.l001_skr_w;value=2}}
{PID}_okruszek_plecak_w1:
  Skills:
  - skill{{s={PID}_okruszek_plecak}} @self ?variableequals{{var=world.l001_skr_w;value=1}}
{PID}_okruszek_plecak_w2:
  Skills:
  - sound{{s=item.bundle.drop_contents;v=1;p=0.8}} @PlayersInRadius{{r=6}} ?variableequals{{var=world.l001_skr_w;value=2}}
  - message{{m="<gray>Porzucony plecak poszukiwacza. Ślady prowadzą w gąszcz na północny zachód.</gray>"}} @PlayersInRadius{{r=6}} ?variableequals{{var=world.l001_skr_w;value=2}}
{PID}_okruszek_plecak:
  Skills:
  - sound{{s=item.bundle.drop_contents;v=1;p=0.8}} @PlayersInRadius{{r=6}}
  - message{{m="<gray>Porzucony plecak poszukiwacza. Ślady prowadzą w gęstwinę na północny zachód.</gray>"}} @PlayersInRadius{{r=6}}
{PID}_okruszek_tarcza:
  Skills:
  - sound{{s=item.shield.break;v=0.8;p=0.8}} @PlayersInRadius{{r=6}}
  - message{{m="<gray>Pęknięta tarcza. Ktoś bronił się tu przed czymś dużym... ślady prowadzą między omszałe kamienie na wschód.</gray>"}} @PlayersInRadius{{r=6}}

# --- ZNAJDZKI (sekrety do oceny): nagrody = nasze materialy PROF_* (mi give: typ, id, gracz, ilosc)
{PID}_kwiat_A:
  Skills:
  - command{{c="mmocore admin exp give <target.name> main 20"}} @PlayersInWorld
  - variableadd{{var=world.l001_sekrety;amount=1}} @self
  - particles{{p=cherry_leaves;a=60;hs=1.5;vs=1.5;s=0.02;y=1}} @self
  - sound{{s=block.amethyst_block.chime;v=1;p=1.5}} @PlayersInRadius{{r=8}}
  - setvariable{{var=world.l001_bl_kwiat;val=1;type=INTEGER}} @self
  # od razu na 10 min (emiter tylko odnawia po smierci - pojedynczy emiter bywal martwy, zmierzone na world1)
  - potion{{type=REGENERATION;duration=12000;level=0;hasParticles=false}} @PlayersInRadius{{r=10}}
  - title{{title="<light_purple>Kwiat Gaju</light_purple>";subtitle="<gray>Błogosławieństwo: regeneracja do końca lochu</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInRadius{{r=8}}
  - message{{m="<light_purple>Kwiat Gaju odnaleziony.</light_purple> <gray>Sekrety lochu <gold><world.var.l001_sekrety>/5</gold></gray>"}} @PlayersInRadius{{r=8}}
  - command{{c="mi give MATERIAL PROF_MOONLEAF <target.name> 2"}} @PlayersInWorld
{PID}_skrytka:
  Skills:
  - command{{c="mmocore admin exp give <target.name> main 20"}} @PlayersInWorld
  - variableadd{{var=world.l001_sekrety;amount=1}} @self
  - sound{{s=block.barrel.open;v=1;p=0.9}} @PlayersInRadius{{r=8}}
  - particles{{p=happy_villager;a=15;hs=0.5;vs=0.5;y=1}} @self
  - setvariable{{var=world.l001_bl_kora;val=1;type=INTEGER}} @self
  - potion{{type=RESISTANCE;duration=12000;level=0;hasParticles=false}} @PlayersInRadius{{r=10}}
  - title{{title="<gold>Skrytka pod korzeniami</gold>";subtitle="<gray>Błogosławieństwo Kory: odporność do końca lochu</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInRadius{{r=8}}
  - message{{m="<gold>Skrytka pod korzeniami.</gold> <gray>Ktoś tu schował zapasy. Sekrety lochu <gold><world.var.l001_sekrety>/5</gold></gray>"}} @PlayersInRadius{{r=8}}
  - command{{c="mi give MATERIAL PROF_RAW_TIMBER <target.name> 2"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_ROUGH_CRYSTAL <target.name> 1"}} @PlayersInWorld
{PID}_dziennik:
  Skills:
  - command{{c="mmocore admin exp give <target.name> main 20"}} @PlayersInWorld
  - variableadd{{var=world.l001_sekrety;amount=1}} @self
  - command{{c="execute in minecraft:<caster.l.w> run setblock {P['dziennik'][0]} {P['dziennik'][1]} {P['dziennik'][2]} minecraft:barrel[facing=up,open=true]"}}
  - sound{{s=item.book.page_turn;v=1;p=1}} @PlayersInRadius{{r=8}}
  - message{{m="<yellow>Dziennik poszukiwacza:</yellow> <gray><italic>Latarnia Gaju stoi w lesie, na wschód od ścieżki, niedaleko wejścia do lasu. Jej światło pali korzenie - nawet te Starego Korzenia...</italic></gray>"}} @PlayersInRadius{{r=8}}
  - message{{m="<gray>Sekrety lochu <gold><world.var.l001_sekrety>/5</gold></gray>"}} @PlayersInRadius{{r=8}}
  - command{{c="mi give MATERIAL PROF_BEAST_SCRAP <target.name> 1"}} @PlayersInWorld
  # slad swietlikow od szczatkow do latarni - podpowiedz z dziennika staje sie widoczna
  # slad tylko, gdy latarnia jeszcze nie plonie; kierunek PRAWDZIWY: swietliki z=64->86 = na poludnie (MC: +Z)
  - summon{{m=L001_SWIETLIK;level=1;a=1;r=0}} @Location{{c=53,103,64}} ?variableequals{{var=world.l001_latarnia;value=0}}
  - summon{{m=L001_SWIETLIK;level=1;a=1;r=0}} @Location{{c=52,103,72}} ?variableequals{{var=world.l001_latarnia;value=0}}
  - summon{{m=L001_SWIETLIK;level=1;a=1;r=0}} @Location{{c=53,103,80}} ?variableequals{{var=world.l001_latarnia;value=0}}
  - summon{{m=L001_SWIETLIK;level=1;a=1;r=0}} @Location{{c=55,103,86}} ?variableequals{{var=world.l001_latarnia;value=0}}
  - message{{m="<yellow>Między drzewami zapalają się świetliki - prowadzą z powrotem na południe, do lasu.</yellow>"}} @PlayersInWorld ?variableequals{{var=world.l001_latarnia;value=0}}
  - message{{m="<yellow>Latarnia, o której pisał, już płonie. Stary Korzeń czuje jej światło.</yellow>"}} @PlayersInWorld ?variableequals{{var=world.l001_latarnia;value=1}}

# Latarnia Gaju 1 (opcjonalna, schowana w lesie). Docelowo: licznik + filar swiatla na arenie (spec §2 wersja 2).
{PID}_latarnia_1:
  Skills:
  # wspolrzedne wpisane na sztywno: <caster.l.x> daje "56.0" (setblock chce int), a <caster.l.y+1> nie jest rozwijane
  - command{{c="execute in minecraft:<caster.l.w> run setblock {P['latarnia'][0]} {P['latarnia'][1]+1} {P['latarnia'][2]} minecraft:lantern"}}
  - variableadd{{var=world.l001_sekrety;amount=1}} @self
  - particles{{p=end_rod;a=40;hs=0.6;vs=1.2;s=0.05;y=1.5}} @self
  # slup swiatla w niebo - widac go z calej wyspy
  - particles{{p=end_rod;a=260;hs=0.25;vs=18;s=0;y=19}} @self
  - effect:lightning @self
  # swiatlo zmusza korzenie do poslusznosci: Stary Korzen traci 30 HP (widac na pasku bossa)
  # vanilla damage po znaczniku: @MobsInRadius{{types=...}} raz trafial, raz nie (zmierzone na world1) - znacznik pewny
  # @e BEZ pozycji lapie encje ze WSZYSTKICH swiatow (uwaga sesji Loch 002) - przy 2 instancjach ranilby cudzego bossa
  - command{{c="execute in minecraft:<caster.l.w> positioned {P['boss'][0]} {P['boss'][1]} {P['boss'][2]} run damage @e[tag=l001_boss,distance=..300,limit=1] 30 minecraft:magic"}}
  # swiatlo wypala sciane korzeni - otwiera zakatek ze Skarbem Latarni (loch001_zakatek.py)
  - command{{c="execute in minecraft:<caster.l.w> run setblock 60 102 85 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 60 103 85 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 60 104 85 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 60 102 86 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 60 103 86 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 60 104 86 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 61 102 86 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 61 103 86 minecraft:air destroy"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock 61 104 86 minecraft:air destroy"}}
  - sound{{s=block.roots.break;v=1.4;p=0.6}} @PlayersInRadius{{r=20}}
  - message{{m="<gold>Korzenie na wschodzie kurczą się przed światłem.</gold> <gray>Coś odsłoniły.</gray>"}} @PlayersInRadius{{r=16}}
  - message{{m="<green>Gdzieś na północy Stary Korzeń zawył z bólu.</green> <gray>Światło latarni go osłabiło.</gray>"}} @PlayersInRadius{{r=16}}
  - sound{{s=block.beacon.activate;v=1;p=1.6}} @PlayersInRadius{{r=16}}
  - title{{title="";subtitle="<gold>Latarnia Gaju płonie</gold>";fadein=5;duration=40;fadeout=15}} @PlayersInRadius{{r=16}}
  - message{{m="<gold>Latarnia Gaju zapłonęła.</gold> <gray>Jej światło sięga aż do Starego Korzenia.</gray>"}} @PlayersInWorld
  - setvariable{{var=world.l001_latarnia;val=1;type=INTEGER}} @self
  - command{{c="mmocore admin exp give <target.name> main 20"}} @PlayersInWorld
  - remove @caster

{PID}_boss_intro:
  Skills:
  - title{{title="<dark_green><bold>Stary Korzeń</bold></dark_green>";subtitle="<gray>Gaj nie wypuści cię dalej</gray>";fadein=5;duration=50;fadeout=15}} @PlayersInRadius{{r=30}}
  - sound{{s=entity.ravager.roar;v=1.3;p=0.5}} @PlayersInRadius{{r=40}}
  - particles{{p=block;m=ROOTED_DIRT;a=150;hs=6;vs=0.5;s=0.1}} @self

{PID}_koniec:
  Skills:
  - summon{{m=L001_BEBNY;level=1;a=1;r=0}} @Location{{c=46,106,24}}
  - message{{m="<green>Stary Korzeń pada.</green> <gray>Gaj oddycha, korzenie puszczają ziemię.</gray>"}} @PlayersInRadius{{r=60}}

# --- skille mobow (caster = mob, delay bezpieczny)
# Wilk: skok z zapowiedzia ~0.8 s (warczenie + czasteczki + zastygniecie) - unik w bok (spec §3).
L001_WILK_SKOK:
  Cooldown: 4
  Conditions:
  - targetwithin{{d=9}}
  Skills:
  - sound{{s=entity.wolf.growl;v=1.2;p=0.7}} @self
  - particles{{p=angry_villager;a=8;hs=0.5;vs=0.4;y=1}} @self
  - potion{{type=SLOWNESS;duration=16;level=6;hasParticles=false}} @self
  - delay 16
  - leap{{velocity=110}} @target
  - delay 8
  - damage{{amount=3}} @PlayersInRadius{{r=1.8}}

# Stary Korzen: uderzenie korzeniami z zapowiedzia 1.2 s (krag czasteczek + dzwiek), unik = wyjsc z kregu r=3.
L001_KORZEN_SLAM:
  Cooldown: 5
  Conditions:
  - targetwithin{{d=6}}
  Skills:
  - sound{{s=entity.warden.dig;v=1.2;p=1.2}} @self
  - potion{{type=SLOWNESS;duration=26;level=10;hasParticles=false}} @self
  - particlering{{p=block;m=ROOTED_DIRT;radius=3;points=32;a=2;y=0.1}} @self
  - delay 12
  - particlering{{p=block;m=ROOTED_DIRT;radius=3;points=32;a=2;y=0.1}} @self
  - delay 12
  - particles{{p=explosion;a=3;hs=2;vs=0.2}} @self
  - sound{{s=entity.generic.explode;v=0.8;p=0.6}} @self
  - damage{{amount=6}} @PlayersInRadius{{r=3}}
  - throw{{velocity=6;velocityY=3}} @PlayersInRadius{{r=3}}
L001_KORZEN_STREFA:
  Cooldown: 9
  Conditions:
  - playerwithin{{d=7}}
  Skills:
  - sound{{s=block.rooted_dirt.place;v=1.4;p=0.4}} @self
  - skill{{s=L001_KORZEN_PULS}} @self
  - delay 20
  - skill{{s=L001_KORZEN_PULS}} @self
  - delay 20
  - skill{{s=L001_KORZEN_PULS}} @self
L001_KORZEN_PULS:
  Skills:
  - particlering{{p=block;m=ROOTED_DIRT;radius=4;points=36;a=2;y=0.1}} @self
  - potion{{type=SLOWNESS;duration=25;level=2;hasParticles=false}} @PlayersInRadius{{r=4}}
# KORA: kanal 4 s (leczy +30 HP), 3 ciosy w trakcie go kruszą (sprawdzane co 5 tickow w mobie)
L001_KORA:
  Cooldown: 18
  Conditions:
  - playerwithin{{d=12}}
  Skills:
  - setvariable{{var=caster.kora;val=1;type=INTEGER}} @self
  - setvariable{{var=caster.kora_ciosy;val=0;type=INTEGER}} @self
  - message{{m="<dark_green>Stary Korzeń</dark_green> <gray>obrasta korą i zaczyna się leczyć - uderz go trzy razy, zanim skończy!</gray>"}} @PlayersInRadius{{r=30}}
  - sound{{s=block.wood.place;v=1.5;p=0.5}} @self
  - potion{{type=SLOWNESS;duration=80;level=10;hasParticles=false}} @self
  - particles{{p=block;m=OAK_LOG;a=40;hs=1.2;vs=2;s=0.05;y=2}} @self
  - delay 20
  - particles{{p=block;m=OAK_LOG;a=40;hs=1.2;vs=2;s=0.05;y=2}} @self ?variableequals{{var=caster.kora;value=1}}
  - delay 20
  - particles{{p=block;m=OAK_LOG;a=40;hs=1.2;vs=2;s=0.05;y=2}} @self ?variableequals{{var=caster.kora;value=1}}
  - delay 20
  - particles{{p=block;m=OAK_LOG;a=40;hs=1.2;vs=2;s=0.05;y=2}} @self ?variableequals{{var=caster.kora;value=1}}
  - delay 20
  - heal{{amount=30}} @self ?variableequals{{var=caster.kora;value=1}}
  - particles{{p=happy_villager;a=40;hs=1.2;vs=2;y=2}} @self ?variableequals{{var=caster.kora;value=1}}
  - message{{m="<dark_green>Stary Korzeń</dark_green> <gray>zasklepił rany korą (+30).</gray>"}} @PlayersInRadius{{r=30}} ?variableequals{{var=caster.kora;value=1}}
  - setvariable{{var=caster.kora;val=0;type=INTEGER}} @self
L001_KORA_PEKA:
  Skills:
  - setvariable{{var=world.l001_kora_ok;val=1;type=INTEGER}} @self
  - setvariable{{var=caster.kora;val=0;type=INTEGER}} @self
  - sound{{s=block.wood.break;v=2;p=0.6}} @self
  - particles{{p=block;m=OAK_LOG;a=80;hs=1.5;vs=2;s=0.2;y=2}} @self
  - message{{m="<gold>Kora pęka!</gold> <gray>Stary Korzeń traci równowagę.</gray>"}} @PlayersInRadius{{r=30}}
  - potion{{type=SLOWNESS;duration=60;level=10;hasParticles=false}} @self
# WYRWA: korzenie wyrywaja sie pod graczem (zapowiedz: krag 1.2 s) - karze za stanie w miejscu i kitowanie z dystansu
L001_KORZEN_WYRWA:
  Cooldown: 6
  Conditions:
  - targetwithin{{d=22}}
  Skills:
  - sound{{s=block.rooted_dirt.break;v=1.5;p=0.5}} @self
  - skill{{s=L001_WYRWA_MIEJSCE}} @targetlocation
L001_WYRWA_MIEJSCE:
  Skills:
  - particlering{{p=block;m=ROOTED_DIRT;radius=2;points=24;a=2;y=0.1}} @origin
  - delay 12
  - particlering{{p=block;m=ROOTED_DIRT;radius=2;points=24;a=2;y=0.1}} @origin
  - delay 12
  - particles{{p=block;m=OAK_LOG;a=50;hs=1;vs=1.2;s=0.15;y=0.5}} @origin
  - sound{{s=entity.evoker_fangs.attack;v=1.2;p=0.7}} @origin
  - damage{{amount=5}} @PlayersNearOrigin{{r=2}}
  - potion{{type=SLOWNESS;duration=40;level=3;hasParticles=false}} @PlayersNearOrigin{{r=2}}

# w polowie zycia wzywa dziatwe z ziemi (raz)
L001_KORZEN_DZIATWA:
  Cooldown: 9999
  Skills:
  - message{{m="<dark_green>Stary Korzeń</dark_green> <gray>wyrywa z ziemi swoje potomstwo!</gray>"}} @PlayersInRadius{{r=30}}
  - sound{{s=entity.ravager.roar;v=1;p=0.8}} @self
  - particles{{p=block;m=ROOTED_DIRT;a=80;hs=3;vs=0.5;s=0.1}} @self
  - summon{{m=L001_PNIACZEK;level=1;a=4;r=3;yr=6;os=true}} @self
  - signal{{s=wataha}} @MobsInRadius{{r=150;types=L001_WILK_WATAHA}}
L001_WATAHA_WEZWANIE:
  Skills:
  - teleport @MobsInRadius{{r=150;types=L001_STARY_KORZEN;limit=1}}
  - sound{{s=entity.wolf.howl;v=1.4;p=0.8}} @self
  - message{{m="<gold>Wilk z polany odpowiada na wezwanie Starego Korzenia!</gold>"}} @PlayersInRadius{{r=60}}

# Bebny Plemienia Ciernia: rytm z palisady + strzaly spadajace obok gracza, potem emiter znika.
L001_BEBNY_SEKWENCJA:
  Skills:
  # --- swit: niebo przechodzi w poranek, zarodniki ustepuja platkom
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:time set 22800"}}
  - sound{{s=block.amethyst_block.resonate;v=2;p=0.7}} @PlayersInRadius{{r=120}}
  - delay 6
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:time set 23400"}}
  - delay 6
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:time set 0"}}
  - remove @MobsInRadius{{r=200;types=L001_KLIMAT_LAS}}
  - summon{{m=L001_KLIMAT_SWIT;level=1;a=1;r=0}} @Location{{c=30,103,90}}
  - summon{{m=L001_KLIMAT_SWIT;level=1;a=1;r=0}} @Location{{c=44,103,78}}
  - summon{{m=L001_KLIMAT_SWIT;level=1;a=1;r=0}} @Location{{c=56,103,52}}
  - delay 6
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:time set 600"}}
  - delay 6
  - command{{c="execute in minecraft:<caster.l.w> run minecraft:time set 1200"}}
  - title{{title="<yellow>Świt nad Kwietną Polaną</yellow>";subtitle="<gray>Gaj odzyskał oddech</gray>";fadein=10;duration=50;fadeout=15}} @PlayersInRadius{{r=120}}
  - delay 70
  # --- cliffhanger: Plemie Ciernia
  - sound{{s=block.note_block.basedrum;v=3;p=0.6}} @PlayersInRadius{{r=80}}
  - delay 8
  - sound{{s=block.note_block.basedrum;v=3;p=0.6}} @PlayersInRadius{{r=80}}
  - delay 8
  - sound{{s=block.note_block.basedrum;v=3;p=0.8}} @PlayersInRadius{{r=80}}
  - delay 14
  - sound{{s=block.note_block.basedrum;v=3;p=0.6}} @PlayersInRadius{{r=80}}
  - delay 8
  - sound{{s=block.note_block.basedrum;v=3;p=0.6}} @PlayersInRadius{{r=80}}
  - delay 8
  - sound{{s=block.note_block.basedrum;v=3;p=0.8}} @PlayersInRadius{{r=80}}
  - delay 10
  - sound{{s=entity.arrow.shoot;v=2;p=0.8}} @PlayersInRadius{{r=80}}
  - command{{c="execute in minecraft:<caster.l.w> positioned <target.l.x> <target.l.y> <target.l.z> run summon arrow ~3 ~14 ~-2 {{Motion:[0.0,-1.4,0.0],pickup:0b}}"}} @PlayersInRadius{{r=80}}
  - command{{c="execute in minecraft:<caster.l.w> positioned <target.l.x> <target.l.y> <target.l.z> run summon arrow ~-3 ~14 ~1 {{Motion:[0.0,-1.4,0.0],pickup:0b}}"}} @PlayersInRadius{{r=80}}
  - command{{c="execute in minecraft:<caster.l.w> positioned <target.l.x> <target.l.y> <target.l.z> run summon arrow ~1 ~14 ~4 {{Motion:[0.0,-1.4,0.0],pickup:0b}}"}} @PlayersInRadius{{r=80}}
  - delay 12
  - sound{{s=block.note_block.basedrum;v=3;p=0.6}} @PlayersInRadius{{r=80}}
  - delay 8
  - sound{{s=block.note_block.basedrum;v=3;p=0.8}} @PlayersInRadius{{r=80}}
  # --- NAGRODA KONCOWA (krytyka: boss dawal 3 drewna): baza + premia za sekrety, dla calej druzyny
  - command{{c="mmocore admin exp give <target.name> main 50"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_OAK_TIMBER <target.name> 4"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_SPRUCE_RESIN <target.name> 2"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_ENCHANT_DUST <target.name> 1"}} @PlayersInWorld
  - command{{c="mi give MATERIAL PROF_ROUGH_CRYSTAL <target.name> 2"}} @PlayersInWorld ?variableequals{{var=world.l001_sekrety;value=3}}
  - command{{c="mi give MATERIAL PROF_ROUGH_CRYSTAL <target.name> 2"}} @PlayersInWorld ?variableequals{{var=world.l001_sekrety;value=4}}
  - command{{c="mi give MATERIAL PROF_ROUGH_CRYSTAL <target.name> 3"}} @PlayersInWorld ?variableequals{{var=world.l001_sekrety;value=5}}
  - command{{c="mi give MATERIAL PROF_ENCHANT_DUST <target.name> 3"}} @PlayersInWorld ?variableequals{{var=world.l001_sekrety;value=5}}
  - command{{c="mmocore admin exp give <target.name> main 100"}} @PlayersInWorld ?variableequals{{var=world.l001_sekrety;value=5}}
  # --- OCENA (powtorki): 4 wyzwania -> S/A/B/C; od oceny zalezy dodatkowy lup
  - variableadd{{var=world.l001_ocena;amount=1}} @self ?variableequals{{var=world.l001_sekrety;value=5}}
  - variableadd{{var=world.l001_ocena;amount=1}} @self ?variableequals{{var=world.l001_kora_ok;value=1}}
  - variableadd{{var=world.l001_ocena;amount=1}} @self ?variableequals{{var=world.l001_zag_ok;value=1}}
  - variableadd{{var=world.l001_ocena;amount=1}} @self ?variableequals{{var=world.l001_latarnia;value=1}}
  - message{{m="<gray>Wyzwania Polany</gray>"}} @PlayersInWorld
  - message{{m=" <green>✔</green> <gray>Wszystkie sekrety (5/5)</gray>"}} @PlayersInWorld ?variableequals{{var=world.l001_sekrety;value=5}}
  - message{{m=" <red>✘</red> <gray>Wszystkie sekrety (<world.var.l001_sekrety>/5)</gray>"}} @PlayersInWorld ?!variableequals{{var=world.l001_sekrety;value=5}}
  - message{{m=" <green>✔</green> <gray>Kora Starego Korzenia skruszona</gray>"}} @PlayersInWorld ?variableequals{{var=world.l001_kora_ok;value=1}}
  - message{{m=" <red>✘</red> <gray>Kora Starego Korzenia skruszona</gray>"}} @PlayersInWorld ?!variableequals{{var=world.l001_kora_ok;value=1}}
  - message{{m=" <green>✔</green> <gray>Zagadka kamieni bez pomyłki</gray>"}} @PlayersInWorld ?variableequals{{var=world.l001_zag_ok;value=1}}
  - message{{m=" <red>✘</red> <gray>Zagadka kamieni bez pomyłki</gray>"}} @PlayersInWorld ?!variableequals{{var=world.l001_zag_ok;value=1}}
  - message{{m=" <green>✔</green> <gray>Latarnia Gaju zapalona</gray>"}} @PlayersInWorld ?variableequals{{var=world.l001_latarnia;value=1}}
  - message{{m=" <red>✘</red> <gray>Latarnia Gaju zapalona</gray>"}} @PlayersInWorld ?!variableequals{{var=world.l001_latarnia;value=1}}
  - title{{title="<gold><bold>Ocena S</bold></gold>";subtitle="<gray>Mistrz Polany</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=4}}
  - title{{title="<yellow><bold>Ocena A</bold></yellow>";subtitle="<gray>Prawie wszystko</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=3}}
  - title{{title="<green><bold>Ocena B</bold></green>";subtitle="<gray>Gaj widział lepszych</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=2}}
  - title{{title="<gray><bold>Ocena C</bold></gray>";subtitle="<gray>Wróć i poszukaj więcej</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=1}}
  - title{{title="<gray><bold>Ocena C</bold></gray>";subtitle="<gray>Wróć i poszukaj więcej</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=0}}
  - command{{c="mi give MATERIAL PROF_ENCHANT_DUST <target.name> 3"}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=4}}
  - command{{c="mmocore admin exp give <target.name> main 100"}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=4}}
  - command{{c="mi give MATERIAL PROF_ENCHANT_DUST <target.name> 2"}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=3}}
  - command{{c="mmocore admin exp give <target.name> main 50"}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=3}}
  - command{{c="mmocore admin exp give <target.name> main 25"}} @PlayersInWorld ?variableequals{{var=world.l001_ocena;value=2}}
  # lup nastroju dnia
  - command{{c="mi give MATERIAL PROF_MOONLEAF <target.name> 2"}} @PlayersInWorld ?variableequals{{var=world.l001_mod;value=1}}
  - command{{c="mi give MATERIAL PROF_RAW_TIMBER <target.name> 4"}} @PlayersInWorld ?variableequals{{var=world.l001_mod;value=2}}
  - command{{c="mi give MATERIAL PROF_BEAST_SCRAP <target.name> 3"}} @PlayersInWorld ?variableequals{{var=world.l001_mod;value=3}}
  - command{{c="mi give MATERIAL PROF_MEDICINAL_HERB <target.name> 3"}} @PlayersInWorld ?variableequals{{var=world.l001_mod;value=4}}
  - message{{m="<gold>Łup z Polany trafia do plecaka.</gold> <gray>Więcej sekretów, więcej kryształów - komplet 5/5 daje premię.</gray>"}} @PlayersInWorld
  - title{{title="<red>Plemię Ciernia wie, że tu jesteś</red>";subtitle="<gray>Koniec fragmentu - dalej loch jest w budowie</gray>";fadein=10;duration=100;fadeout=20}} @PlayersInWorld
  - message{{m="<gray>Znalezione sekrety <gold><world.var.l001_sekrety>/5</gold>. Za chwilę wrócisz do świata.</gray>"}} @PlayersInWorld
  - delay 40
  - remove @self

# Korzeniak: przysiada (nie rusza sie 4 s) i rozlewa strefe korzeni r=2.5 ze spowolnieniem. Kontra: wyjsc ze strefy,
# bic w czasie przysiadu.
L001_KORZENIAK_STREFA:
  Cooldown: 7
  Conditions:
  - playerwithin{{d=5}}
  Skills:
  - sound{{s=block.rooted_dirt.place;v=1.2;p=0.6}} @self
  - potion{{type=SLOWNESS;duration=80;level=10;hasParticles=false}} @self
  - skill{{s=L001_KORZENIAK_PULS}} @self
  - delay 20
  - skill{{s=L001_KORZENIAK_PULS}} @self
  - delay 20
  - skill{{s=L001_KORZENIAK_PULS}} @self
  - delay 20
  - skill{{s=L001_KORZENIAK_PULS}} @self
L001_KORZENIAK_PULS:
  Skills:
  - particlering{{p=block;m=ROOTED_DIRT;radius=2.5;points=24;a=2;y=0.1}} @self
  - particles{{p=block;m=ROOTED_DIRT;a=20;hs=1.4;vs=0.1;s=0.02}} @self
  - potion{{type=SLOWNESS;duration=25;level=2;hasParticles=false}} @PlayersInRadius{{r=2.5}}
"""
SKILLS += f"""
# Uwaga: w podskillach wolanych z @trigger "@self" = mob (druid/krysztal/kepa); linie do gracza bez targetera (dziedzicza @trigger)
# ---------- DRUID: prosba -> napar za 2 garscie Ziela (raz), potem pozegnanie
L001_DRUID_ROZMOWA:
  Cooldown: 1
  Skills:
  - skill{{s=L001_DRUID_JUZ}} @trigger ?variableequals{{var=world.l001_napar;value=1}}
  - skill{{s=L001_DRUID_NAPAR}} @trigger ?variableequals{{var=world.l001_napar;value=0}} ?variableequals{{var=world.l001_ziola;value=2}}
  - skill{{s=L001_DRUID_NAPAR}} @trigger ?variableequals{{var=world.l001_napar;value=0}} ?variableequals{{var=world.l001_ziola;value=3}}
  # warunki pozytywne: linia z zaprzeczeniami (?~) nie odpalala sie wcale (zmierzone)
  - skill{{s=L001_DRUID_PROSBA}} @trigger ?variableequals{{var=world.l001_napar;value=0}} ?variableequals{{var=world.l001_ziola;value=0}}
  - skill{{s=L001_DRUID_PROSBA}} @trigger ?variableequals{{var=world.l001_napar;value=0}} ?variableequals{{var=world.l001_ziola;value=1}}
L001_DRUID_PROSBA:
  Skills:
  - message{{m="<green>Ranny druid</green> <gray>- Przynieś mi dwie garście Ziela Gaju, świeci przy ścieżce. Zaparzę ci napar na Stary Korzeń. (<world.var.l001_ziola>/2)</gray>"}}
  - sound{{s=entity.villager.ambient;v=1;p=0.8}}
L001_DRUID_NAPAR:
  Skills:
  - setvariable{{var=world.l001_napar;val=1;type=INTEGER}} @self
  - potion{{type=ABSORPTION;duration=12000;level=1;hasParticles=false}} @PlayersInWorld
  - particles{{p=happy_villager;a=20;hs=0.6;vs=1;y=1}}
  - sound{{s=entity.generic.drink;v=1;p=1}}
  - sound{{s=entity.villager.yes;v=1;p=0.8}}
  - title{{title="<green>Napar druida</green>";subtitle="<gray>+4 serca osłony dla całej drużyny</gray>";fadein=5;duration=60;fadeout=15}} @PlayersInWorld
  - message{{m="<green>Ranny druid</green> <gray>- Wypij. Gorzkie, ale Stary Korzeń poczuje waszą siłę.</gray>"}} @PlayersInWorld
L001_DRUID_JUZ:
  Skills:
  - message{{m="<green>Ranny druid</green> <gray>- Idź. Gaj czeka.</gray>"}}

# ---------- ZIELE GAJU (funkcja MD przy kepie; caster = punkt funkcji)
loch_001_ziele_zbior:
  Skills:
  - variableadd{{var=world.l001_ziola;amount=1}} @self
  - command{{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> run minecraft:kill @e[type=zombie,distance=..2.5,limit=1,sort=nearest]"}}
  - sound{{s=block.azalea_leaves.break;v=1;p=1.2}} @PlayersInRadius{{r=4}}
  - particles{{p=happy_villager;a=12;hs=0.4;vs=0.4;y=1}} @self
  - message{{m="<green>Ziele Gaju</green> <gray>(1/2) - druid potrzebuje jeszcze jednej garści.</gray>"}} @PlayersInRadius{{r=4}} ?variableequals{{var=world.l001_ziola;value=1}}
  - message{{m="<green>Ziele Gaju</green> <gray>(2/2) - zanieś je rannemu druidowi przy ognisku.</gray>"}} @PlayersInRadius{{r=4}} ?variableequals{{var=world.l001_ziola;value=2}}
  - message{{m="<green>Ziele Gaju</green> <gray>- zapas na drogę. Druid ma już dość.</gray>"}} @PlayersInRadius{{r=4}} ?variableequals{{var=world.l001_ziola;value=3}}
  - command{{c="mi give MATERIAL PROF_MEDICINAL_HERB <target.name> 1"}} @PlayersInWorld


# ---------- ZAGADKA KAMIENI: kierunek LOSOWANY przy wejsciu (world.l001_zag_kier: 0 = od najnizszego, 1 = od najwyzszego)
# - przy kolejnym wejsciu zagadka nie jest juz "rozwiazana w glowie". Blad = korzenie oplataja nogi, reset, znacznik do oceny.
"""
for k in (1, 2, 3):
    lines = []
    for kier in (0, 1):
        krok = k if kier == 0 else 4 - k           # ktory to krok w tej kolejnosci
        for z in (0, 1, 2):
            if z != krok - 1:
                lines.append(f"  - skill{{s=L001_ZAGADKA_ZLE}} @trigger ?variableequals{{var=world.l001_zag_kier;value={kier}}} ?variableequals{{var=world.l001_zagadka;value={z}}}\n")
    for kier in (0, 1):   # sukces NA KONCU (OK zmienia stan - linie ponizej widzialyby nowy)
        krok = k if kier == 0 else 4 - k
        lines.append(f"  - skill{{s=L001_ZAGADKA_KROK_{krok}}} @trigger ?variableequals{{var=world.l001_zag_kier;value={kier}}} ?variableequals{{var=world.l001_zagadka;value={krok-1}}}\n")
    SKILLS += f"L001_ZAGADKA_{k}:\n  Cooldown: 1\n  Skills:\n" + "".join(lines)
for krok in (1, 2, 3):
    SKILLS += f"""L001_ZAGADKA_KROK_{krok}:
  Skills:
  - setvariable{{var=world.l001_zagadka;val={krok};type=INTEGER}} @self
  - sound{{s=block.amethyst_block.chime;v=1.5;p={0.8 + 0.3*krok:.1f}}}
  - particles{{p=end_rod;a=25;hs=0.3;vs=0.6;y=1.8}} @caster
""" + ("""  - message{m="<light_purple>Kamienie Gaju rozbrzmiewają razem.</light_purple> <gray>Kwiat Gaju budzi się.</gray>"}
  - setvariable{var=world.l001_zag_ok;val=1;type=INTEGER} @self ?variableequals{var=world.l001_zag_blad;value=0}
  - command{c="mmocore admin exp give <target.name> main 30"} @PlayersInWorld
  - skill{s=loch_001_kwiat_A} @self
""" if krok == 3 else "")
SKILLS += """L001_ZAGADKA_ZLE:
  Skills:
  - setvariable{var=world.l001_zagadka;val=0;type=INTEGER} @self
  - setvariable{var=world.l001_zag_blad;val=1;type=INTEGER} @self
  - sound{s=block.rooted_dirt.place;v=1.4;p=0.5}
  - potion{type=SLOWNESS;duration=60;level=3;hasParticles=false}
  - particles{p=block;m=ROOTED_DIRT;a=30;hs=0.6;vs=0.2} @self
  - message{m="<dark_green>Korzenie oplatają ci nogi.</dark_green> <gray>Kamienie gasną - zacznij od nowa.</gray>"}
"""
# okruszki: vanilla summon (SNBT w pojedynczych cudzyslowach - nie koliduja z c="...")
_ROT = {"strzala": ["[0f,0f,0.3827f,0.9239f]", "[0.2706f,0.2706f,0.6533f,0.6533f]", "[0f,0f,0.3827f,0.9239f]"],
        "plecak": "[0.7071f,0f,0f,0.7071f]", "list": "[0.7071f,0f,0f,0.7071f]", "tarcza": "[0.5f,0.5f,0.5f,0.5f]"}
_ITEM = {"strzala": ("arrow", 1.0, 0.3), "plecak": ("bundle", 0.9, 0.05), "list": ("paper", 0.7, 0.02), "tarcza": ("shield", 1.0, 0.1)}
def _okruszek(tag, xyz, rot):
    item, skala, dy = _ITEM[tag]; x, y, z = xyz
    return (f"  - command{{c=\"execute in minecraft:<caster.l.w> run summon item_display {x+0.5} {y+dy-1+1:.2f} {z+0.5} "
            f"{{item:{{id:'minecraft:{item}',count:1}},Tags:['l001_okruszek','{tag}'],transformation:{{left_rotation:{rot},"
            f"right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[{skala}f,{skala}f,{skala}f]}}}}\"}}\n")
# SKRYTKA W 2 WARIANTACH (losowane przy wejsciu, world.l001_skr_w): okruszki prowadza do tej, ktora jest
SKR2 = (20, 103, 78)                                   # gaszcz w lesie, 10 bl. od sciezki (grunt 102 zmierzony)
OKR2 = {"strzaly": [(25, 102, 86), (24, 102, 84), (25, 102, 85)], "plecak": (24, 103, 83), "list": (22, 103, 80)}
SKILLS += "\n# wariant 1 skrytki: stara skrytka (10,103,92) + okruszki spod wejscia do lasu\nL001_SKR_1:\n  Skills:\n  - setvariable{var=world.l001_skr_w;val=1;type=INTEGER} @self\nL001_SKR_1_BUDUJ:\n  Skills:\n"
for i, xyz in enumerate(OKR["okruszki"]["strzaly"]): SKILLS += _okruszek("strzala", xyz, _ROT["strzala"][i])
for t in ("plecak", "list"): SKILLS += _okruszek(t, OKR["okruszki"][t], _ROT[t])
SKILLS += f"""# wariant 2: stara beczka znika w lisciach, nowa skrytka w gaszczu (20,78) + okruszki od polany
L001_SKR_2:
  Skills:
  - setvariable{{var=world.l001_skr_w;val=2;type=INTEGER}} @self
L001_SKR_2_BUDUJ:
  Skills:
  - command{{c="execute in minecraft:<caster.l.w> run setblock {P['skrytka'][0]} {P['skrytka'][1]} {P['skrytka'][2]} minecraft:oak_leaves[persistent=true]"}}
  - command{{c="execute in minecraft:<caster.l.w> run setblock {SKR2[0]} {SKR2[1]} {SKR2[2]} minecraft:barrel[facing=up,open=false]"}}
""" + "".join(f'  - command{{c="execute in minecraft:<caster.l.w> run setblock {SKR2[0]+dx} {SKR2[1]+dy} {SKR2[2]+dz} minecraft:oak_leaves[persistent=true]"}}\n'
              for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, -1), (1, 0, -1), (0, 1, -1), (1, 1, 0)))
for i, xyz in enumerate(OKR2["strzaly"]): SKILLS += _okruszek("strzala", xyz, _ROT["strzala"][i])
for t in ("plecak", "list"): SKILLS += _okruszek(t, OKR2[t], _ROT[t])
SKILLS += "\nL001_OKRUSZKI_TARCZA:\n  Skills:\n" + _okruszek("tarcza", OKR["okruszki"]["tarcza"], _ROT["tarcza"])
# NASTROJ DNIA (world.l001_mod) - losowany przy wejsciu, zmienia rozgrywke; nagrody w finale czesciowo od niego zaleza
SKILLS += """
L001_MOD_1:
  Skills:
  - setvariable{var=world.l001_mod;val=1;type=INTEGER} @self
  - command{c="execute in minecraft:<caster.l.w> run minecraft:time set 18000"}
  - sound{s=entity.allay.ambient_without_item;v=1;p=0.6} @PlayersInWorld
  - message{m="<gold>✦ Dziś w Gaju</gold> <aqua>Noc Świetlików</aqua> <gray>- ciemno, las lśni. W finale nocny łup (Księżycowy liść).</gray>"} @PlayersInWorld
L001_MOD_2:
  Skills:
  - setvariable{var=world.l001_mod;val=2;type=INTEGER} @self
  - sound{s=entity.creeper.primed;v=1;p=1.4} @PlayersInWorld
  - message{m="<gold>✦ Dziś w Gaju</gold> <red>Wybuchowe Pniaczki</red> <gray>- nie stój przy nich, gdy padają. Więcej drewna w finale.</gray>"} @PlayersInWorld
L001_MOD_3:
  Skills:
  - setvariable{var=world.l001_mod;val=3;type=INTEGER} @self
  - sound{s=entity.wolf.howl;v=1;p=0.7} @PlayersInWorld
  - message{m="<gold>✦ Dziś w Gaju</gold> <gold>Wściekła Wataha</gold> <gray>- wilki szybsze i silniejsze. Więcej łupu z bestii.</gray>"} @PlayersInWorld
L001_MOD_4:
  Skills:
  - setvariable{var=world.l001_mod;val=4;type=INTEGER} @self
  - potion{type=REGENERATION;duration=12000;level=0;hasParticles=false} @PlayersInWorld
  - sound{s=block.amethyst_block.chime;v=1;p=1.3} @PlayersInWorld
  - message{m="<gold>✦ Dziś w Gaju</gold> <light_purple>Kwitnący Gaj</light_purple> <gray>- gaj ci sprzyja (regeneracja). Więcej ziół w finale.</gray>"} @PlayersInWorld
L001_ZAG_0:
  Skills:
  - setvariable{var=world.l001_zag_kier;val=0;type=INTEGER} @self
L001_ZAG_1:
  Skills:
  - setvariable{var=world.l001_zag_kier;val=1;type=INTEGER} @self
# efekty nastroju na mobach (trigger bez warunku -> metaskill z warunkami: wzorzec, ktory dziala)
L001_PNIACZEK_SMIERC:
  Skills:
  - particles{p=explosion;a=2;hs=0.5;vs=0.3} @self ?variableequals{var=world.l001_mod;value=2}
  - sound{s=entity.generic.explode;v=0.7;p=1.6} @self ?variableequals{var=world.l001_mod;value=2}
  - damage{amount=3} @PlayersInRadius{r=2.2} ?variableequals{var=world.l001_mod;value=2}
L001_WILK_WSCIEKLY:
  Skills:
  - potion{type=SPEED;duration=999999;level=1;hasParticles=false} @self ?variableequals{var=world.l001_mod;value=3}
  - potion{type=STRENGTH;duration=999999;level=0;hasParticles=false} @self ?variableequals{var=world.l001_mod;value=3}
"""
_bez = [l for l in SKILLS.splitlines() if "summon{" in l and "level=1" not in l and not l.lstrip().startswith("#")]
assert not _bez, f"summon bez level=1: {_bez}"

# ---------------- zapis
shutil.rmtree(OUT, ignore_errors=True)
md = f"{OUT}/md/{PID}"
os.makedirs(md)
open(f"{md}/config.yml", "w").write(cfg)
open(f"{md}/gamerules.yml", "w").write(GR)
open(f"{md}/functions.yml", "w").write(FUN)
# zamrozony teren: region/ (+ entities/ i poi/ jesli sa), BEZ level.dat
for d in ("region", "entities", "poi"):
    src = f"{KUZNIA}/{SWIAT}/{d}"
    if os.path.isdir(src):
        shutil.copytree(src, f"{md}/{d}")
assert glob.glob(f"{md}/region/*.mca"), "brak regionow - czy swiat kuzni jest zapisany?"
os.makedirs(f"{OUT}/mm/mobs"); os.makedirs(f"{OUT}/mm/skills")
open(f"{OUT}/mm/mobs/Loch_001.yml", "w").write(MOBS)
open(f"{OUT}/mm/skills/Loch_001.yml", "w").write(SKILLS)
print("zbudowano:", OUT, "| regiony:", len(glob.glob(f"{md}/region/*.mca")))
