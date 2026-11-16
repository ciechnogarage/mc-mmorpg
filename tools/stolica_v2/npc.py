#!/usr/bin/env python3
"""Generate auditable MythicMobs NPCs for services that exist on world1."""

from pathlib import Path

MOBS = Path('/opt/docker/mc/data/world1/plugins/MythicMobs/mobs/StolicaOdnowa.yml')
SKILLS = Path('/opt/docker/mc/data/world1/plugins/MythicMobs/skills/StolicaOdnowa.yml')
SPAWNERS = Path('/opt/docker/mc/data/world1/plugins/MythicMobs/spawners')

# id, label, x, z, command, villager profession, villager biome
SERVICES = [
    ('przewodnik','Przewodniczka Stolicy',0.5,51.5,'dm open stolica_v2_tablica','CARTOGRAPHER','PLAINS'),
    ('tablica','Kronikarz wypraw',9.5,42.5,'dm open stolica_v2_tablica','LIBRARIAN','PLAINS'),
    ('nexus','Strażnik Nexusu',0.5,19.5,'dm open stolica_v2_podroze','CLERIC','TAIGA'),
    ('sklep','Kupiec wyposażenia',-22.5,8.5,'core-shops sklep','WEAPONSMITH','DESERT'),
    ('skup','Skup surowców',-22.5,19.5,'core-shops skup','FARMER','DESERT'),
    ('aukcje','Licytator',-22.5,30.5,'ah mmoitems_example stolica','CARTOGRAPHER','SAVANNA'),
    ('naprawa','Mistrz napraw',-39.5,30.5,'core-station repair_station','TOOLSMITH','TAIGA'),
    ('rozbiorka','Rozbieracz sprzętu',-34.5,30.5,'core-station deconstruct_station','MASON','TAIGA'),
    ('sakwa','Opiekunka sakwy',-39.5,-2.5,'core-playervault sakwa','LIBRARIAN','SWAMP'),
    ('postac','Kronikarz postaci',-25.5,-34.5,'dm open hub_postac','LIBRARIAN','SNOW'),
    ('umiejetnosci','Mistrz umiejętności',-29.5,-23.5,'dm open hub_umiejetnosci','WEAPONSMITH','SNOW'),
    ('chowance','Opiekunka chowańców',29.5,-35.5,'dm open chowance_menazeria','SHEPHERD','PLAINS'),
    ('wiara','Kapłanka świątyni',0.5,-35.5,'dm open wiara','CLERIC','PLAINS'),
    ('gildia','Opiekun drużyn',25.5,48.5,'dm open hub_spolecznosc','CARTOGRAPHER','SAVANNA'),
    ('trening','Mistrz treningu',-22.5,52.5,'dm open hub_umiejetnosci','WEAPONSMITH','TAIGA'),
    ('mining','Górnik',26.5,7.5,'mi stations open profession-mining','MASON','TAIGA'),
    ('woodworking','Drwal i stolarz',34.5,7.5,'mi stations open profession-woodworking','FLETCHER','TAIGA'),
    ('herbalism','Zielarka',24.5,24.5,'mi stations open profession-herbalism','FARMER','SWAMP'),
    ('farming','Farmer',27.5,32.5,'mi stations open profession-farming','FARMER','PLAINS'),
    ('fishing','Rybak',36.5,34.5,'mi stations open profession-fishing','FISHERMAN','SWAMP'),
    ('hunting','Łowczy',41.5,6.5,'mi stations open profession-hunting','FLETCHER','TAIGA'),
    ('jewelcrafting','Jubilerka',55.5,6.5,'mi stations open profession-jewelcrafting','LIBRARIAN','DESERT'),
    ('smithing','Kowal',28.5,17.5,'mi stations open profession-smithing','ARMORER','TAIGA'),
    ('tailoring','Krawcowa',40.5,25.5,'mi stations open profession-tailoring','SHEPHERD','PLAINS'),
    ('alchemy','Alchemiczka',28.5,27.5,'mi stations open profession-alchemy','CLERIC','SWAMP'),
    ('enchanting','Zaklinacz',35.5,26.5,'mi stations open profession-enchanting','LIBRARIAN','SNOW'),
    ('cooking','Kucharka',52.5,32.5,'mi stations open profession-cooking','BUTCHER','PLAINS'),
    ('building','Budowniczy',55.5,36.5,'mi stations open profession-building','MASON','SAVANNA'),
]


def main():
    mob=[]; skill=[]
    for ident,label,x,z,command,profession,biome in SERVICES:
        y = 102 if ident in {'tailoring', 'wiara', 'trening'} else 101
        key=f'STOLICA2_{ident.upper()}'
        skill_key=f'{key}_OTWORZ'
        mob.extend([
            f'{key}:',
            '  Type: VILLAGER',
            f"  Display: '&6{label}'",
            '  Health: 100',
            '  Options:',
            '    NoAI: true',
            '    Invincible: true',
            '    Silent: true',
            '    Despawn: false',
            '    PreventOtherDrops: true',
            '    Collidable: false',
            '    AlwaysShowName: true',
            '    HasTrades: false',
            f'    Profession: {profession}',
            f'    Type: {biome}',
            '    Level: 3',
            '  Skills:',
            f'  - skill{{s={skill_key}}} @trigger ~onInteract ?~isplayer',
            '',
        ])
        skill.extend([f'{skill_key}:','  Skills:',
                      f'  - command{{c="{command} <trigger.name>"}} @trigger',''])
        spawner=f'''stolica2_{ident}:
  MobName: {key}
  World: stolica_odnowa
  X: {x}
  Y: {y}
  Z: {z}
  Yaw: 180
  Pitch: 0.0
  Radius: 0.0
  RadiusY: 0.0
  UseTimer: true
  MaxMobs: '1'
  MobsPerSpawn: 1
  Cooldown: 60
  Warmup: 0
  ActivationRange: 64.0
  ScalingRange: 0.0
  LeashRange: 4.0
  HealOnLeash: false
  ResetThreatOnLeash: false
  Breakable: false
  CheckForPlayers: false
  ShowFlames: false
  Conditions: []
  SpawnConditions: []
'''
        (SPAWNERS/f'stolica2_{ident}.yml').write_text(spawner)
    MOBS.write_text('# Stacja dzialajacych uslug Stolicy Odnowionej.\n'+'\n'.join(mob))
    SKILLS.write_text('# Wszystkie komendy wykonuje konsola dla gracza klikajacego NPC.\n'+'\n'.join(skill))
    print(f'{len(SERVICES)} service NPCs')


if __name__=='__main__':main()
