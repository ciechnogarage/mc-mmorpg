"""Conservative identity of editor exports; unknown fields remain significant.

Only UI label/selection and local save-path metadata are ignored. Geometry,
texture bytes, expressions, pivots and animation data remain exact. A raw
legacy generator file may need an explicitly reviewed editor-normalized copy.
"""
import copy
import hashlib
import json


def canonical(model):
    result = copy.deepcopy(model)
    result.pop('name', None)
    for animation in result.get('animations', []):
        animation.pop('selected', None)
        tracks = animation.get('animators', {})
        for ident, track in list(tracks.items()):
            if (set(track) <= {'name', 'type', 'rotation_global', 'quaternion_interpolation', 'keyframes'}
                    and track.get('type') == 'bone' and not track.get('keyframes')
                    and not track.get('rotation_global', False)
                    and not track.get('quaternion_interpolation', False)):
                del tracks[ident]
    for texture in result.get('textures', []):
        for key in ('path', 'saved'):
            texture.pop(key, None)
    return result


def fingerprint(model):
    payload = json.dumps(canonical(model), sort_keys=True, separators=(',', ':'), allow_nan=False)
    return hashlib.sha256(payload.encode()).hexdigest()


def require_match(expected, actual):
    if fingerprint(expected) != fingerprint(actual):
        raise ValueError('Active project does not match requested source; refusing to measure or render.')
