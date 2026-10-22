"""Numeric motion programs consumed by the transactional Blockbench author.

These are pose studies, not calibrated gameplay motion. No bbmodel is written.
"""
import copy
import math


def solve_leg(upper, lower, root_y, lift, z):
    values = (upper, lower, root_y, lift, z)
    if not all(math.isfinite(v) for v in values) or min(upper, lower) <= 0:
        raise ValueError('Finite positive segment lengths required')
    down = upper + lower + root_y - lift
    radius = math.hypot(down, z)
    if radius < abs(upper - lower) + 1e-9 or radius > upper + lower + 1e-9:
        raise ValueError('Requested sole is outside leg reach')
    clamp = lambda v: max(-1, min(1, v))
    hip = math.degrees(math.atan2(-z, down) + math.acos(clamp((upper**2 + radius**2 - lower**2) / (2 * upper * radius))))
    knee = -math.degrees(math.acos(clamp((radius**2 - upper**2 - lower**2) / (2 * upper * lower))))
    return hip, knee, -hip - knee


def mirror_tracks(tracks):
    result = {}
    for name, channels in tracks.items():
        target = ('left_' + name[6:] if name.startswith('right_') else
                  'right_' + name[5:] if name.startswith('left_') else name)
        result[target] = {}
        for channel, frames in channels.items():
            result[target][channel] = [[t, ([x, -y, -z] if channel == 'rotation' else [-x, y, z])]
                                       for t, (x, y, z) in frames]
    return result


def build_states(groups):
    states = {}

    def clip(name, length, loop='loop'):
        c = {'length': length, 'loop': loop, 'override': True, 'tracks': {}}
        states[name] = c
        return c

    def add(c, bone, channel, time, values):
        c['tracks'].setdefault(bone, {}).setdefault(channel, []).append([time, values])

    def constant(c, bone, values, channel='rotation'):
        for t in (0, c['length']):
            add(c, bone, channel, t, values[:])

    neutral = {'right_arm': [1.5, 0, -3], 'left_arm': [-.5, 0, 3],
               'right_forearm': [9, 0, 0], 'left_forearm': [2, 0, 0]}
    for name, length, stride, depth, lift in [('run', .6, 10, -1.6, 2.25),
                                            ('crouch_walk', 1.1, 3.5, -3.1, .5),
                                            ('crouch_idle', 1, 0, -3.1, 0)]:
        c = clip(name, length)
        count = round(length * 40) if stride else 1
        for frame in range(count + 1):
            t = round(length * frame / count, 9)
            phase = frame / count
            bob = depth - (.15 if name == 'run' else .04 if stride else 0) * math.cos(4 * math.pi * phase)
            add(c, 'root', 'position', t, [0, bob, 0])
            pitch = -9 if name == 'run' else 8
            add(c, 'torso', 'rotation', t, [pitch, 1.5 * math.sin(2 * math.pi * phase) if stride else 0, 1])
            add(c, 'hi_head', 'rotation', t, [-pitch * .7, 0, -1])
            for side, offset in [('right', 0), ('left', .5)]:
                leg, shin, foot = [groups[side + '_' + p] for p in ('leg', 'shin', 'foot')]
                if any(any(g.get('rotation', [0, 0, 0])) for g in (leg, shin, foot)):
                    raise ValueError('Solver requires unrotated rest leg chains')
                upper = leg['origin'][1] - shin['origin'][1]
                lower = shin['origin'][1] - foot['origin'][1]
                p = (phase + offset) % 1
                if p < .5:
                    z, foot_lift = -stride / 2 + 2 * stride * p, 0
                else:
                    u = 2 * (p - .5)
                    z = stride / 2 - stride * (u * u * (3 - 2 * u))
                    foot_lift = lift * math.sin(math.pi * u)**2
                angles = solve_leg(upper, lower, bob, foot_lift, z)
                for part, angle in zip(('leg', 'shin', 'foot'), angles):
                    add(c, side + '_' + part, 'rotation', t, [angle, 0, 0])
                swing = math.sin(2 * math.pi * (phase + offset - .08))
                add(c, side + '_arm', 'rotation', t, [(-8 + 25 * swing) if name == 'run' else 8 + 3 * swing if stride else 8, 0, -5 if side == 'right' else 6])
                add(c, side + '_forearm', 'rotation', t, [(40 if side == 'right' else 48) if name == 'run' else 27 if side == 'right' else 35, 0, 0])

    for name, amplitude in [('swim', 1), ('swim_idle', .2)]:
        c = clip(name, 1.2)
        constant(c, 'root', [0, 8, 16], 'position')
        constant(c, 'root', [-90, 0, 0])
        constant(c, 'torso', [7, 0, 2])
        constant(c, 'hi_head', [45, 0, -2])
        for frame in range(25):
            t = round(frame * .05, 9)
            phase = frame / 24
            for side, offset in [('right', 0), ('left', .5)]:
                wave = math.sin(2 * math.pi * (phase + offset))
                add(c, side + '_leg', 'rotation', t, [12 * amplitude * wave, 0, 0])
                add(c, side + '_shin', 'rotation', t, [-8 - 8 * amplitude * (wave + 1) / 2, 0, 0])
                add(c, side + '_foot', 'rotation', t, [8, 0, 0])
                add(c, side + '_arm', 'rotation', t, [145 + 25 * amplitude * wave, -12 if side == 'right' else 12, -30 if side == 'right' else 30])
                add(c, side + '_forearm', 'rotation', t, [5 + 10 * amplitude * (1 - wave), 0, 0])

    # Base use clips hold the actual inventory item in the physical right hand.
    drawn = {'right_arm': [85, 0, -8], 'right_forearm': [5, 0, 0],
             'left_arm': [72, 28, 12], 'left_forearm': [72, 0, 0]}
    draw = clip('bow_draw', .4, 'hold')
    hold = clip('bow_hold', 1)
    for bone, pose in drawn.items():
        for time, value in [(0, neutral[bone]), (.3, pose), (.4, pose)]:
            add(draw, bone, 'rotation', time, value[:])
        constant(hold, bone, pose)
    block = clip('block', 1)
    constant(block, 'left_arm', [65, 0, 22])
    constant(block, 'left_forearm', [55, 0, 0])
    for base, name in [('bow_draw', 'bow_draw_left'), ('bow_hold', 'bow_hold_left'), ('block', 'block_right')]:
        states[name] = copy.deepcopy(states[base])
        states[name]['tracks'] = mirror_tracks(states[base]['tracks'])
    return states
