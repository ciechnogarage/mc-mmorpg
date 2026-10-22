"""Capture specified clips from the already-open, source-matched project.

Never opens projects. Output directories must be new. COMPLETE means capture
and state restoration finished, not visual/runtime acceptance.
"""
import argparse
import base64
from datetime import datetime, timezone
import hashlib
import io
import json
import math
from pathlib import Path
import re
import sys

from PIL import Image
from review_session import ReviewSession

CAMERA = {'position': [36, 25, -55], 'target': [0, 16, 0], 'projection': 'perspective'}


def sample_times(length, fps):
    if not math.isfinite(length) or length <= 0 or not 1 <= fps <= 120:
        raise ValueError('Positive finite clip length and fps 1..120 required')
    times = [round(i / fps, 9) for i in range(math.floor(length * fps) + 1)]
    if abs(times[-1] - length) > 1e-8:
        times.append(length)
    else:
        times[-1] = length
    return times


def decode_capture(result):
    blocks = [part for part in result.get('content', []) if part.get('type') == 'image']
    if len(blocks) != 1:
        raise ValueError('Screenshot must contain exactly one image')
    with Image.open(io.BytesIO(base64.b64decode(blocks[0]['data'], validate=True))) as image:
        image.load()
        return image.convert('RGBA')


def artifact(path, out):
    return {'file': str(path.relative_to(out)), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}


def run_review(source, out, specification, capture, session_factory=ReviewSession):
    """Single owner of bind/create/snapshot/final-bind/restore completion order."""
    session = session_factory()
    identity = session.bind(source)
    requested_hash = specification.get('requested_source_sha256')
    if requested_hash is not None and identity.get('source_sha256') != requested_hash:
        raise ValueError('Source changed after planning capture schedule')
    out.mkdir(parents=True, exist_ok=False)
    manifest = {**identity, **specification, 'status': 'INCOMPLETE',
                'started_utc': datetime.now(timezone.utc).isoformat(),
                'limitations': ['Original camera is not restored; project/mode/animation/time restoration is requested.',
                                'Captures do not validate contact, blending, visual quality or Minecraft runtime.'],
                'artifacts': [], 'errors': []}
    manifest_path = out / 'manifest.json'
    def write_manifest():
        manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n')
    write_manifest()
    state = None
    failure = None
    try:
        state = session.snapshot()
        manifest['original_editor_state'] = state
        write_manifest()
        manifest['artifacts'] = capture(session, manifest)
        final_identity = session.bind(source)
        if any(final_identity.get(key) != identity.get(key) for key in ('source_sha256', 'project_fingerprint')):
            raise ValueError('Source or project changed during capture')
        manifest['final_identity'] = final_identity
    except BaseException as exc:
        failure = exc
        manifest['errors'].append(f'{type(exc).__name__}: {exc}')
    finally:
        if state is not None:
            try:
                session.restore(state)
                manifest['editor_state_restored'] = True
            except BaseException as exc:
                manifest['editor_state_restored'] = False
                manifest['errors'].append(f'Restore failed: {type(exc).__name__}: {exc}')
                if failure is None:
                    failure = exc
        manifest['finished_utc'] = datetime.now(timezone.utc).isoformat()
        if failure is None:
            manifest['status'] = 'COMPLETE'
        write_manifest()
    if failure is not None:
        raise failure
    return manifest


def selected_clips(model, names):
    if len(set(names)) != len(names):
        raise ValueError('Duplicate clip selection')
    clips = []
    for name in names:
        if not re.fullmatch(r'[A-Za-z0-9_-]+', name):
            raise ValueError(f'Unsafe clip output name: {name}')
        matches = [a for a in model['animations'] if a['name'] == name]
        if len(matches) != 1:
            raise ValueError(f'Expected exactly one clip: {name}')
        clips.append(matches[0])
    return clips


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--clips', nargs='+', required=True)
    parser.add_argument('--fps', type=int, default=20)
    parser.add_argument('--camera', choices=['standard','fall'], default='standard')
    args = parser.parse_args(argv)
    try:
        camera = CAMERA if args.camera == 'standard' else {'position': [48,34,-80], 'target':[0,12,-10], 'projection':'perspective'}
        source_bytes = args.source.read_bytes()
        clips = selected_clips(json.loads(source_bytes), args.clips)
        schedule = {a['name']: sample_times(float(a['length']), args.fps) for a in clips}
        spec = {'requested_source_sha256': hashlib.sha256(source_bytes).hexdigest(), 'tool': 'render_undead_motion.py', 'tool_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                'camera': camera, 'fps': args.fps, 'clips': [
                    {'name': a['name'], 'length': a['length'], 'source_loop': a['loop'], 'times': schedule[a['name']]} for a in clips],
                'gif_presentation': 'Repeats for comparison, including once/hold clips; terminal pose held for one sampling interval.'}
        def capture(session, manifest):
            artifacts = []
            for animation in clips:
                name = animation['name']
                session.tool('set_camera_angle', camera)
                frames = []
                checksums = []
                for index, time in enumerate(schedule[name]):
                    session.pose(name, time)
                    frame = decode_capture(session.tool('capture_screenshot', {}))
                    path = args.out / f'{name}-{index:04d}.png'
                    frame.save(path)
                    record = artifact(path, args.out)
                    record.update({'clip': name, 'time': time, 'resolution': list(frame.size)})
                    artifacts.append(record)
                    checksums.append(record['sha256'])
                    background = Image.new('RGBA', frame.size, (31, 36, 43, 255))
                    background.alpha_composite(frame)
                    frames.append(background.convert('RGB'))
                if len(set(checksums)) == 1 and len(checksums) > 1:
                    manifest.setdefault('warnings', []).append(f'{name}: all captured frames identical; verify static hold or failed preview')
                times = schedule[name]
                durations = [max(10, round((b-a)*1000)) for a, b in zip(times, times[1:])] + [max(10, round(1000/args.fps))]
                gif = args.out / f'{name}.gif'
                frames[0].save(gif, save_all=True, append_images=frames[1:], duration=durations, loop=0)
                artifacts.append(artifact(gif, args.out))
            return artifacts
        run_review(args.source, args.out, spec, capture)
    except Exception as exc:
        print(f'Render failed: {exc}', file=sys.stderr)
        return 1
    print(f'Capture complete: {args.out / "manifest.json"}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
