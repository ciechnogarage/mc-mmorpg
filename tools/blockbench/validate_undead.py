"""Read-only undead file checks. This does NOT measure posed geometry or runtime.

Exit 0: scoped file checks succeeded; 1: invalid asset/policy; 2: unsupported
feature or tool/input failure. Numeric loop endpoints are an asset policy,
not a claim that every legal Blockbench model needs explicit matching ends.
"""
import argparse
import base64
import binascii
from collections import Counter
import hashlib
import io
import json
import math
from pathlib import Path

from PIL import Image, UnidentifiedImageError

REQUIRED = {'idle', 'walk', 'jump_start', 'jump', 'jump_end', 'attack', 'hurt', 'death', 'spawn'}
PLAYER_REQUIRED = REQUIRED | {'attack_left', 'run', 'crouch_idle', 'crouch_walk', 'swim', 'swim_idle',
                              'bow_draw', 'bow_hold', 'block', 'bow_draw_left', 'bow_hold_left', 'block_right'}


def validate(model, required=None):
    required = REQUIRED if required is None else required
    errors, unsupported = [], []

    def check(condition, message):
        if not condition:
            errors.append(message)
        return bool(condition)

    def number(value, label, expression=False):
        if isinstance(value, bool):
            errors.append(f'{label}: expected finite number')
            return None
        try:
            value = float(value)
        except (TypeError, ValueError, OverflowError):
            (unsupported if expression and isinstance(value, str) else errors).append(
                f'{label}: unsupported expression' if expression else f'{label}: expected finite number')
            return None
        if not math.isfinite(value):
            errors.append(f'{label}: expected finite number')
            return None
        return value

    def vector(value, label, count=3, expression=False):
        if not check(isinstance(value, list) and len(value) == count, f'{label}: expected {count} coordinates'):
            return None
        values = [number(v, label, expression) for v in value]
        return values if all(v is not None for v in values) else None

    groups, cubes = model['groups'], model['elements']
    identifiers = [item['uuid'] for item in groups + cubes]
    check(len(set(identifiers)) == len(identifiers), 'duplicate identifier in groups/elements')
    group_ids = {g['uuid'] for g in groups}
    cube_ids = {c['uuid'] for c in cubes}
    seen = []

    def visit(nodes):
        for node in nodes:
            ident = node if isinstance(node, str) else node['uuid']
            seen.append(ident)
            check(ident in (cube_ids if isinstance(node, str) else group_ids), f'hierarchy unknown/wrong-kind reference: {ident}')
            if isinstance(node, dict):
                visit(node['children'])
    visit(model['outliner'])
    check(Counter(seen) == Counter(identifiers) and len(set(seen)) == len(seen), 'hierarchy must contain each element/group exactly once')
    for group in groups:
        vector(group['origin'], f"bone {group['name']} origin")
        vector(group.get('rotation', [0, 0, 0]), f"bone {group['name']} rotation")
    hitboxes = [g for g in groups if g['name'] == 'hitbox']
    check(len(hitboxes) == 1, 'policy: exactly one hitbox bone required')
    if len(hitboxes) == 1:
        origin = vector(hitboxes[0]['origin'], 'hitbox origin')
        if origin:
            check(abs(origin[1] - 28.35) < 1e-5, 'policy: hitbox eye height must be 28.35')

    textures = model['textures']
    check(bool(textures), 'at least one texture required')
    spaces = []
    for index, texture in enumerate(textures):
        for field in ('namespace', 'folder'):
            check(isinstance(texture.get(field), str), f'texture {index}: missing {field}')
        uv = [number(texture.get('uv_' + axis, model['resolution'][axis]), f'texture {index} UV dimension') for axis in ('width', 'height')]
        check(all(v is not None and v > 0 for v in uv), f'texture {index}: UV dimensions must be positive')
        spaces.append(uv)
        source = texture.get('source', '')
        if not source.startswith('data:image/png;base64,'):
            unsupported.append(f'texture {index}: only embedded PNG texture image supported')
            continue
        try:
            payload = base64.b64decode(source.split(',', 1)[1], validate=True)
            with Image.open(io.BytesIO(payload)) as image:
                image.load()
                check(image.format == 'PNG', f'texture image {index}: expected PNG')
        except (ValueError, binascii.Error, OSError, UnidentifiedImageError):
            errors.append(f'texture image {index}: invalid PNG/base64')

    for cube in cubes:
        label = f"cube {cube['name']}"
        lo, hi = vector(cube['from'], label), vector(cube['to'], label)
        if lo and hi:
            check(all(b > a for a, b in zip(lo, hi)), f'{label}: nonpositive dimensions')
        vector(cube.get('origin', [0, 0, 0]), label + ' origin')
        rotation = vector(cube.get('rotation', [0, 0, 0]), label + ' rotation')
        if rotation:
            check(all(r in (0, 22.5, -22.5, 45, -45) for r in rotation) and sum(r != 0 for r in rotation) <= 1,
                  f'{label}: policy cuboid rotation restriction')
        check(set(cube['faces']) == {'north', 'south', 'east', 'west', 'up', 'down'}, f'{label}: expected six faces (disable with texture null)')
        for face in cube['faces'].values():
            check('texture' in face, f'{label}: missing texture field')
            reference = face.get('texture')
            uv = vector(face['uv'], label + ' UV', 4)
            if reference is None:
                continue
            if not check(type(reference) is int and 0 <= reference < len(textures), f'{label}: invalid texture reference {reference}'):
                continue
            width, height = spaces[reference]
            if uv and width is not None and height is not None:
                check(all(0 <= x <= limit for x, limit in zip(uv, [width, height, width, height])), f'{label}: UV bounds outside declared space')

    animations = model['animations']
    names = [a['name'] for a in animations]
    check(len(set(names)) == len(names), 'duplicate animation name')
    animation_ids = [a['uuid'] for a in animations]
    check(len(set(animation_ids)) == len(animation_ids), 'duplicate animation identifier')
    check(required <= set(names), f'policy: missing required animations: {sorted(required - set(names))}')
    for animation in animations:
        label = animation['name']
        length = number(animation['length'], label + ' length')
        if length is not None:
            check(length > 0, f'{label}: length must be positive')
        check(animation['loop'] in ('loop', 'once', 'hold'), f'{label}: invalid loop mode')
        for ident, track in animation['animators'].items():
            if track.get('type', 'bone') != 'bone':
                unsupported.append(f'{label}: non-bone animator {ident}')
                continue
            check(ident in group_ids, f'{label}: animator bone reference missing')
            channels = {}
            unverified_channels = set()
            for key in track.get('keyframes', []):
                time = number(key['time'], label + ' keyframe time')
                if time is not None and length is not None:
                    check(0 <= time <= length, f'{label}: keyframe time outside clip')
                channel = key['channel']
                if channel not in ('position', 'rotation', 'scale'):
                    unsupported.append(f'{label}: unsupported channel {channel}')
                interpolation = key.get('interpolation', 'linear')
                if interpolation != 'linear':
                    unsupported.append(f'{label}: interpolation {interpolation} endpoint policy unverified')
                points = key['data_points']
                if len(points) != 1:
                    unsupported.append(f'{label}: multiple/empty data points unverified')
                values = [vector([point.get(axis) for axis in 'xyz'], label + ' keyframe', expression=True) for point in points]
                if len(values) != 1 or values[0] is None or interpolation != 'linear':
                    unverified_channels.add(channel)
                if time is not None and len(values) == 1 and values[0] is not None:
                    channels.setdefault(channel, []).append((time, values[0]))
            for channel, frames in channels.items():
                frames.sort()
                check(len({t for t, _ in frames}) == len(frames), f'{label}/{channel}: duplicate keyframe time')
                if animation['loop'] == 'loop' and channel not in unverified_channels:
                    check(frames[0][0] == 0 and frames[-1][0] == length, f'{label}/{channel}: policy loop endpoints missing')
                    check(all(abs(a - b) < 1e-4 for a, b in zip(frames[0][1], frames[-1][1])), f'{label}/{channel}: policy loop seam differs')
    return errors, unsupported


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('model', type=Path)
    parser.add_argument('--profile', choices=['base', 'player'], default='base',
                        help='base: nine legacy clips; player: current 21-clip controller contract')
    args = parser.parse_args(argv)
    try:
        raw = args.model.read_bytes()
    except OSError as exc:
        print(f'TOOL_ERROR: {exc}')
        return 2
    print(f'Source: {args.model}\nSHA256: {hashlib.sha256(raw).hexdigest()}')
    print(f'Scope: hierarchy, numeric geometry, embedded textures/UV, numeric animation structure; undead {args.profile} policy (required clips, eye height, cube rotations, loop endpoints).')
    print('MOTION_NOT_CHECKED: no posed geometry, contact, interpolation sampling, blending, art, pack or runtime checks.')
    try:
        model = json.loads(raw)
        errors, unsupported = validate(model, PLAYER_REQUIRED if args.profile == 'player' else REQUIRED)
    except RecursionError:
        print('UNVERIFIED: JSON/hierarchy exceeds tool recursion capacity')
        return 2
    except (ValueError, KeyError, TypeError, IndexError, AttributeError) as exc:
        print(f'INVALID_MODEL: malformed JSON/schema: {exc}')
        return 1
    for error in errors:
        print(f'INVALID_MODEL: {error}')
    for feature in dict.fromkeys(unsupported):
        print(f'UNVERIFIED: {feature}')
    if errors:
        return 1
    if unsupported:
        return 2
    print('FILE_CHECKS_OK: scoped checks only; not a release approval.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
