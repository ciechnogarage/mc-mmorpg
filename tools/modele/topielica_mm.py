"""ModelEngine overlay for the existing Loch 002 MythicMobs configuration.

The forge map generator retains ownership of combat values and dungeon logic.
This overlay only adds the model, animation cues and a short dive windup.
"""
import re


def block(source, name, transform):
    pattern = rf'(?ms)^{re.escape(name)}:\n.*?(?=^[A-Za-z][A-Za-z0-9_]*:|\Z)'
    matches = list(re.finditer(pattern, source))
    if len(matches) != 1:
        raise ValueError(f'Expected one {name} block, found {len(matches)}')
    match = matches[0]
    old = match.group()
    new = transform(old)
    return source[:match.start()] + new + source[match.end():]


def add_after(source, anchor, added):
    if added in source:
        return source
    if source.count(anchor) != 1:
        raise ValueError(f'Expected one anchor: {anchor}')
    return source.replace(anchor, anchor + added, 1)


def patch(mobs, skills):
    def boss(value):
        value = value.replace('  Equipment:\n  - MANGROVE_ROOTS HEAD\n', '')
        value = value.replace('  - command{c="attribute <caster.uuid> minecraft:scale base set 2.4"} @self ~onSpawn\n', '')
        value = add_after(value, '  Skills:\n',
                          '  - model{m=topielica;n=tag_name;step=1} @self ~onSpawn\n'
                          '  - model{m=topielica;n=tag_name;step=1} @self ~onLoad\n'
                          '  - state{m=topielica;s=attack;li=2;lo=3} @self ~onAttack\n'
                          '  - state{m=topielica;s=hurt;li=1;lo=2} @self ~onDamaged\n'
                          '  - state{m=topielica;s=death;li=1;lo=0} @self ~onDeath\n')
        return value

    mobs = block(mobs, 'L002_TOPIELICA', boss)
    for name, state in (('L002_WLOSY', 'wlosy'), ('L002_KALUZA', 'kaluza'),
                        ('L002_NURT', 'zanurzenie'), ('L002_SKRZEK', 'skrzek')):
        def cue(value, state=state):
            return add_after(value, '  Skills:\n', f'  - state{{m=topielica;s={state};li=2;lo=3}} @self\n')
        skills = block(skills, name, cue)

    def dive(value):
        # Let the model visibly dip before the entity moves under the well.
        value = value.replace('  - potion{type=INVISIBILITY;duration=45;level=0;hasParticles=false} @self\n', '')
        value = add_after(value, '  - particles{p=splash;a=80;hs=1.2;vs=1.5} @self\n', '  - delay 8\n')
        value = value.replace('  - delay 30\n  - command{c="execute in minecraft:<caster.l.w>',
                              '  - delay 22\n  - command{c="execute in minecraft:<caster.l.w>')
        value = value.replace('  - command{c="effect clear <caster.uuid> minecraft:invisibility"}\n', '')
        value = add_after(value,
                          '  - command{c="execute in minecraft:<caster.l.w> positioned <caster.l.x> <caster.l.y> <caster.l.z> as @e[tag=l002_boss,distance=..3,limit=1] run tp @s ~ ~4 ~"}\n',
                          '  - state{m=topielica;s=wynurzenie;li=0;lo=3} @self\n')
        return value

    skills = block(skills, 'L002_NURT', dive)
    return mobs, skills
