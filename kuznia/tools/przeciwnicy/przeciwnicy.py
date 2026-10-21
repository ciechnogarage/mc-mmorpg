#!/usr/bin/env python3
"""Paczka przeciwnikow Gaju (Akt 1) do strojenia na arenie: kazdy atak ma ZAPOWIEDZ (unikalny dzwiek + czasteczki),
kontre i okno odslony. Zasada strojenia: bot "nieuwazny" (stoi i bije) ma dostac mocno / zginac, bot "uwazny"
(reaguje na zapowiedz) ma wygrac prawie bez straty. Czas walki ze zwyklym mobem 6-15 s.
Wynik: kuznia/levels/przeciwnicy/Przeciwnicy_Gaj.yml (mobs) i Przeciwnicy_Gaj_skills.yml.
Wyglad zastepczy (vanilla Type) do czasu modeli z gornej polki LR.

Sygnaly zapowiedzi (bot "uwazny" nasluchuje ich w pakietach dzwieku):
  WILK_SKOK     entity.wolf.growl (glosnosc 1.6, pitch 0.5)      -> krok w bok
  KORZEN_WYRWA  block.rooted_dirt.break w miejscu GRACZA        -> wyjsc 3 bl. z miejsca
  LUCZNIK_NACIAG item.crossbow.loading_start                     -> unik w bok (strzal leci prosto)
"""
import os as _os, sys as _sys; _sys.path.insert(0, _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__)))); from kuznia_sciezki import LEVELS  # noqa: E402
import os
OUT = f"{LEVELS}/przeciwnicy"

MOBS = """# Przeciwnicy Gaju v1 - generowane przez kuznia/tools/przeciwnicy/przeciwnicy.py (nie edytowac recznie)
GAJ_WILK:
  Type: WOLF
  Display: '&6Wilk Gajowy'
  Health: 40
  Damage: 3
  Options:
    Despawn: persistent
    FollowRange: 20
    MovementSpeed: 0.34
    PreventOtherDrops: true
    KnockbackResistance: 0.3
  AITargetSelectors:
  - clear
  - attacker
  - players
  Skills:
  # SKOK: zapowiedz 0.9 s (warkniecie + przysiad), potem doskok i ugryzienie 6; po skoku 1 s odslony (wolny)
  - skill{s=GAJ_WILK_SKOK} @target ~onTimer:20

GAJ_KORZENIAK:
  Type: ZOMBIE
  Display: '&aKorzeniak'
  Health: 40
  Damage: 4
  Options:
    Despawn: persistent
    FollowRange: 20
    MovementSpeed: 0.17
    PreventOtherDrops: true
    PreventSunburn: true
    PreventItemPickup: true
    KnockbackResistance: 0.6
  Equipment:
  - OAK_LEAVES HEAD
  AITargetSelectors:
  - clear
  - attacker
  - players
  Skills:
  # WYRWA: pod GRACZEM krag korzeni 1.2 s, potem 6 obr. + unieruchomienie; Korzeniak w tym czasie zakorzeniony (bij go)
  # USCISK: przysiad 1 s (dzwiek korzeni + krag wokol siebie), potem korzenie r3: 8 obr. + unieruchomienie -> odskocz
  - skill{s=GAJ_KORZEN_USCISK} @self ~onTimer:10
  # WYRWA tylko gdy gracz dalej niz 5 (przeciw kitowaniu)
  - skill{s=GAJ_KORZEN_WYRWA} @target ~onTimer:20

GAJ_LUCZNIK:
  Type: SKELETON
  Display: '&2Cierniowy Łucznik'
  Health: 26
  Damage: 2
  Options:
    Despawn: persistent
    FollowRange: 24
    MovementSpeed: 0.26
    PreventOtherDrops: true
    PreventSunburn: true
  Equipment:
  - BOW HAND
  AIGoalSelectors:
  - clear
  - lookatplayers
  - randomlookaround
  AITargetSelectors:
  - clear
  - attacker
  - players
  Skills:
  # trzyma dystans: gracz blizej niz 5 -> odskok
  - skill{s=GAJ_LUCZNIK_ODSKOK} @target ~onTimer:15
  # NACIAG 0.8 s (dzwiek kuszy + blysk), potem strzal prosto w miejsce gracza sprzed naciagu -> unik w bok
  - skill{s=GAJ_LUCZNIK_STRZAL} @target ~onTimer:20
"""

SKILLS = """# Skille Przeciwnikow Gaju v1 (caster = mob, delay dozwolony)
GAJ_WILK_SKOK:
  Cooldown: 3
  Conditions:
  - targetwithin{d=8}
  Skills:
  # v2: cel ZAMKNIETY na poczatku zapowiedzi (pomiar v1: skok podazal za graczem, unik nic nie dawal)
  - sound{s=entity.wolf.growl;v=1.6;p=0.5} @self
  - particles{p=angry_villager;a=6;hs=0.4;vs=0.3;y=0.9} @self
  - potion{type=SLOWNESS;duration=18;level=10;hasParticles=false} @self
  - skill{s=GAJ_WILK_SKOK_CEL} @targetlocation
GAJ_WILK_SKOK_CEL:
  Skills:
  - particlering{p=crit;radius=1.2;points=12;a=1;y=0.2} @origin
  - delay 18
  - leap{velocity=160} @origin
  - delay 6
  - damage{amount=9} @PlayersInRadius{r=1.6}
  - particles{p=sweep_attack;a=3;hs=0.8;vs=0.2;y=0.6} @self
  # odslona po skoku: 1.2 s oszolomienia - okno na kontre
  - potion{type=SLOWNESS;duration=24;level=10;hasParticles=false} @self
  - particles{p=falling_water;a=12;hs=0.3;vs=0.3;y=0.9} @self

GAJ_KORZEN_USCISK:
  Cooldown: 6
  Conditions:
  - playerwithin{d=4}
  Skills:
  - sound{s=block.hanging_roots.break;v=2;p=0.5} @self
  - potion{type=SLOWNESS;duration=40;level=10;hasParticles=false} @self
  - particlering{p=block;m=ROOTED_DIRT;radius=3.5;points=36;a=2;y=0.1} @self
  - delay 10
  - particlering{p=block;m=ROOTED_DIRT;radius=3.5;points=36;a=2;y=0.1} @self
  - delay 10
  - particles{p=block;m=OAK_LOG;a=60;hs=2;vs=1;s=0.15;y=0.4} @self
  - sound{s=entity.evoker_fangs.attack;v=1.4;p=0.6} @self
  # r=3.5: bot/gracz w walce wrecz stoi 3-3.5 bl. (v3: przy r=3 uscisk nie trafial - zmierzone)
  - damage{amount=10} @PlayersInRadius{r=3.5}
  - potion{type=SLOWNESS;duration=30;level=5;hasParticles=false} @PlayersInRadius{r=3.5}
  # v5 (poligon klas: remis 45 s, okno 1.5 s za krotkie przy bialych obrazeniach) -> zakorzeniony 3 s
  - potion{type=SLOWNESS;duration=60;level=10;hasParticles=false} @self
  - particles{p=falling_spore_blossom;a=20;hs=0.4;vs=0.6;y=1} @self
GAJ_KORZEN_WYRWA:
  Cooldown: 6
  Conditions:
  - targetwithin{d=16}
  - targetnotwithin{d=5}
  Skills:
  - potion{type=SLOWNESS;duration=40;level=10;hasParticles=false} @self
  - particles{p=block;m=ROOTED_DIRT;a=25;hs=0.6;vs=0.2} @self
  - skill{s=GAJ_KORZEN_WYRWA_MIEJSCE} @targetlocation
GAJ_KORZEN_WYRWA_MIEJSCE:
  Skills:
  - sound{s=block.rooted_dirt.break;v=2;p=0.5} @origin
  - particlering{p=block;m=ROOTED_DIRT;radius=1.8;points=24;a=2;y=0.1} @origin
  - delay 12
  - particlering{p=block;m=ROOTED_DIRT;radius=1.8;points=24;a=2;y=0.1} @origin
  - delay 12
  - particles{p=block;m=OAK_LOG;a=50;hs=0.9;vs=1.2;s=0.15;y=0.5} @origin
  - sound{s=entity.evoker_fangs.attack;v=1.2;p=0.7} @origin
  - damage{amount=6} @PlayersNearOrigin{r=1.8}
  - potion{type=SLOWNESS;duration=30;level=5;hasParticles=false} @PlayersNearOrigin{r=1.8}

GAJ_LUCZNIK_ODSKOK:
  # v6 (poligon v2: ninja nie dogania, 22-33 s i zgony) -> odskok rzadziej + potkniecie po nim
  Cooldown: 6
  Conditions:
  - targetwithin{d=5}
  Skills:
  - lunge{velocity=-0.9;velocityY=0.35} @target
  - particles{p=cloud;a=6;hs=0.3;vs=0.1} @self
  - delay 8
  - potion{type=SLOWNESS;duration=20;level=10;hasParticles=false} @self
  - sound{s=entity.skeleton.hurt;v=1;p=0.7} @self
GAJ_LUCZNIK_STRZAL:
  Cooldown: 3.5
  Conditions:
  - targetwithin{d=22}
  Skills:
  - sound{s=item.crossbow.loading_start;v=1.6;p=0.8} @self
  - particles{p=enchanted_hit;a=10;hs=0.3;vs=0.3;y=1.4} @self
  - potion{type=SLOWNESS;duration=22;level=10;hasParticles=false} @self
  - skill{s=GAJ_LUCZNIK_STRZAL_CEL} @targetlocation
GAJ_LUCZNIK_STRZAL_CEL:
  Skills:
  # v2: strzal w miejsce z POCZATKU naciagu (unik w bok dziala); delay przeniesiony tu
  # v5 (poligon klas: zabijal kazda klase poza wojownikiem) -> naciag 1.1 s, strzal 5
  - delay 22
  - sound{s=entity.arrow.shoot;v=1.4;p=0.9} @self
  # v7 (poligon v2b: nieuwazny tracil tyle co uwazny) -> strzal 7, dobicie zawsze
  - shoot{type=ARROW;velocity=2.4;damage=7} @origin
  # v8 (poligon v2c: dobicie w BIEZACA pozycje karalo unik) -> drugi strzal w TEN SAM zamkniety punkt:
  # stojacy dostaje oba, kto zrobil unik - zaden
  - delay 8
  - sound{s=entity.arrow.shoot;v=1.2;p=1.3} @self
  - shoot{type=ARROW;velocity=2.8;damage=4} @origin
"""

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    open(f"{OUT}/Przeciwnicy_Gaj.yml", "w").write(MOBS)
    open(f"{OUT}/Przeciwnicy_Gaj_skills.yml", "w").write(SKILLS)
    print("zapisano", OUT)
