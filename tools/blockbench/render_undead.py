"""Render rest views and sampled clip poses of an already-open verified asset."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import sys

from review_session import literal
from render_undead_motion import CAMERA, artifact, decode_capture, run_review, selected_clips

CAMERAS = {
    'front': {'position': [0, 17, -55], 'target': [0, 16, 0], 'projection': 'perspective'},
    'three-quarter': CAMERA,
    'back': {'position': [-32, 23, 55], 'target': [0, 16, 0], 'projection': 'perspective'},
    'side': {'position': [55, 18, 0], 'target': [0, 16, 0], 'projection': 'perspective'},
}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--clips', nargs='+', default=['walk', 'attack', 'jump', 'hurt', 'death'])
    args = parser.parse_args(argv)
    try:
        source_bytes = args.source.read_bytes()
        clips = selected_clips(json.loads(source_bytes), args.clips)
        poses = [{'clip': a['name'], 'length': float(a['length']), 'times': [float(a['length']) * fraction for fraction in (0, .25, .5, 1)]} for a in clips]
        if any(p['length'] <= 0 or not math.isfinite(p['length']) for p in poses):
            raise ValueError('Positive finite clip lengths required')
        spec = {'requested_source_sha256': hashlib.sha256(source_bytes).hexdigest(), 'tool': 'render_undead.py', 'tool_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                'cameras': CAMERAS, 'poses': poses, 'pose_camera': 'three-quarter'}
        def capture(session, manifest):
            artifacts = []
            session.js('Modes.options[' + literal('edit') + '].select(); JSON.stringify({restPose:true})')
            def save(name, camera, clip=None, time=None):
                session.tool('set_camera_angle', camera)
                image = decode_capture(session.tool('capture_screenshot', {}))
                path = args.out / (name + '.png')
                image.save(path)
                record = artifact(path, args.out)
                record.update({'camera': camera, 'resolution': list(image.size), 'clip': clip, 'time': time})
                artifacts.append(record)
            for name, camera in CAMERAS.items():
                save('rest-' + name, camera)
            for pose in poses:
                for index, time in enumerate(pose['times']):
                    session.pose(pose['clip'], time)
                    save(f"{pose['clip']}-{index:02d}", CAMERA, pose['clip'], time)
            return artifacts
        run_review(args.source, args.out, spec, capture)
    except Exception as exc:
        print(f'Render failed: {exc}', file=sys.stderr)
        return 1
    print(f'Capture complete: {args.out / "manifest.json"}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
