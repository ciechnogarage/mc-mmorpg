"""CLI regression tests; source assets are read only, mutations live in tmp dirs."""
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path('/opt/docker/mc/data/world1')  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)
SCRIPT = ROOT / 'tools/blockbench/validate_undead.py'
V4 = ROOT / 'output/undead-player-v4/race_nieumarly.bbmodel'
B = ROOT / 'output/undead-player-v5/face-review/candidate-B-face-v2.bbmodel'


class ValidatorTests(unittest.TestCase):
    def setUp(self):
        self.model = json.loads(V4.read_text())

    def run_model(self, model=None, optimized=False, profile=None):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'fixture.bbmodel'
            raw = json.dumps(self.model if model is None else model).encode()
            path.write_bytes(raw)
            result = subprocess.run([sys.executable, *(['-O'] if optimized else []),
                                     str(SCRIPT), str(path), *(['--profile', profile] if profile else [])], capture_output=True, text=True)
            self.assertNotIn('Traceback', result.stderr)
            return result, hashlib.sha256(raw).hexdigest()

    def expect_error(self, needle):
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn(needle, result.stdout)
        self.assertNotIn('FILE_CHECKS_OK', result.stdout)

    def test_v4_reports_hash_and_limits(self):
        result, digest = self.run_model()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn(digest, result.stdout)
        self.assertIn('FILE_CHECKS_OK', result.stdout)
        self.assertIn('MOTION_NOT_CHECKED', result.stdout)
        self.assertNotIn('stance height', result.stdout)

    def test_valid_two_atlas_candidate(self):
        result, _ = self.run_model(json.loads(B.read_text()))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_missing_reaction_clips(self):
        self.model['animations'] = [a for a in self.model['animations'] if a['name'] not in ('hurt', 'death')]
        self.expect_error('required animations')

    def test_player_profile_rejects_missing_mirrored_attack(self):
        self.model = json.loads((ROOT / 'output/undead-player-v5/player-motion-01/race_nieumarly.bbmodel').read_text())
        self.model['animations'] = [a for a in self.model['animations'] if a['name'] != 'attack_left']
        result, _ = self.run_model(profile='player')
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn('attack_left', result.stdout)

    def test_player_profile_accepts_full_contract_without_separate_fallback_clips(self):
        model = json.loads((ROOT / 'output/undead-player-v5/player-motion-01/race_nieumarly.bbmodel').read_text())
        result, _ = self.run_model(model, profile='player')
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn('FILE_CHECKS_OK', result.stdout)

    def test_invalid_texture_including_optimized_python(self):
        next(iter(self.model['elements'][0]['faces'].values()))['texture'] = 999
        for optimized in (False, True):
            result, _ = self.run_model(optimized=optimized)
            self.assertEqual(result.returncode, 1, result.stdout)
            self.assertIn('texture reference', result.stdout)

    def test_second_atlas_is_decoded(self):
        self.model = json.loads(B.read_text())
        self.model['textures'][1]['source'] = 'data:image/png;base64,YmFk'
        self.expect_error('texture image')

    def test_uv_uses_declared_space_not_png_size(self):
        texture = self.model['textures'][0]
        texture['uv_width'] = texture['uv_height'] = 512
        next(iter(self.model['elements'][0]['faces'].values()))['uv'] = [300, 300, 400, 400]
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_uv_out_of_declared_space(self):
        self.model['textures'][0]['uv_width'] = 1
        self.expect_error('UV bounds')

    def test_missing_bone(self):
        self.model['groups'].pop()
        self.expect_error('hierarchy')

    def test_duplicate_identifier(self):
        self.model['groups'].append(copy.deepcopy(self.model['groups'][0]))
        self.expect_error('duplicate identifier')

    def test_duplicate_animation_name(self):
        self.model['animations'].append(copy.deepcopy(self.model['animations'][0]))
        self.expect_error('duplicate animation')

    def test_nan_pivot(self):
        self.model['groups'][0]['origin'][0] = float('nan')
        self.expect_error('finite')

    def test_key_outside_clip(self):
        next(iter(self.model['animations'][0]['animators'].values()))['keyframes'][0]['time'] = 99
        self.expect_error('keyframe time')

    def test_loop_seam(self):
        next(iter(self.model['animations'][0]['animators'].values()))['keyframes'][-1]['data_points'][0]['x'] = '99'
        self.expect_error('loop seam')

    def test_other_clip_duration_and_key_grid_valid(self):
        animation = self.model['animations'][0]
        animation['length'] *= 1.11
        for track in animation['animators'].values():
            for key in track['keyframes']:
                key['time'] *= 1.11
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_expression_explicitly_unsupported(self):
        next(iter(self.model['animations'][0]['animators'].values()))['keyframes'][0]['data_points'][0]['x'] = 'query.life_time'
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 2, result.stdout)
        self.assertIn('UNVERIFIED', result.stdout)

    def test_unsupported_interpolation_is_not_silently_linearized(self):
        next(iter(self.model['animations'][0]['animators'].values()))['keyframes'][0]['interpolation'] = 'bezier'
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 2, result.stdout)
        self.assertIn('UNVERIFIED', result.stdout)
        self.assertNotIn('FILE_CHECKS_OK', result.stdout)

    def test_missing_input_is_tool_error(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([sys.executable, str(SCRIPT), str(Path(directory) / 'missing')],
                                    capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn('TOOL_ERROR', result.stdout)

    def test_malformed_schema_is_model_error(self):
        result, _ = self.run_model({'groups': []})
        self.assertEqual(result.returncode, 1)
        self.assertIn('malformed', result.stdout)

    def test_unknown_animator_reference(self):
        tracks = self.model['animations'][0]['animators']
        tracks['absent-bone'] = tracks.pop(next(iter(tracks)))
        self.expect_error('animator bone reference')

    def test_numeric_overflow_is_invalid_without_traceback(self):
        self.model['groups'][0]['origin'][0] = 1 << 4096
        self.expect_error('finite')

    def test_booleans_are_not_numeric_coordinates_or_texture_indices(self):
        self.model['groups'][0]['origin'][0] = True
        self.expect_error('finite')
        self.model = json.loads(V4.read_text())
        next(iter(self.model['elements'][0]['faces'].values()))['texture'] = False
        self.expect_error('texture reference')

    def test_null_texture_is_valid_disabled_face(self):
        face = next(iter(self.model['elements'][0]['faces'].values()))
        face['texture'] = None
        face['uv'] = [-100, -100, 500, 500]
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_missing_texture_field_is_not_disabled_face(self):
        del next(iter(self.model['elements'][0]['faces'].values()))['texture']
        self.expect_error('texture field')

    def test_missing_faces_cannot_silently_hide_geometry(self):
        self.model['elements'][0]['faces'] = {}
        self.expect_error('six faces')

    def test_uv_falls_back_to_model_resolution(self):
        del self.model['textures'][0]['uv_width']
        self.model['resolution']['width'] = 1
        self.expect_error('UV bounds')

    def test_malformed_types_report_invalid_asset(self):
        paths = [('groups',), ('outliner',), ('textures',), ('animations',),
                 ('textures', 0, 'source'), ('elements', 0, 'faces'),
                 ('groups', 0, 'uuid'), ('resolution',)]
        for path in paths:
            for bad in (None, True, 17, []):
                with self.subTest(path=path, bad=bad):
                    model = json.loads(V4.read_text())
                    target = model
                    for key in path[:-1]:
                        target = target[key]
                    target[path[-1]] = bad
                    result, _ = self.run_model(model)
                    self.assertEqual(result.returncode, 1, result.stdout)

    def test_deep_valid_hierarchy(self):
        children = self.model['outliner']
        for index in range(150):
            ident = f'deep-{index}'
            self.model['groups'].append({'uuid': ident, 'name': ident, 'origin': [0, 0, 0]})
            children = [{'uuid': ident, 'children': children}]
        self.model['outliner'] = children
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_json_depth_limit_is_unverified_not_asset_judgment(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'deep.bbmodel'
            tree = json.dumps(self.model['outliner'])
            self.model['outliner'] = []
            for index in range(2000):
                ident = f'deep-{index}'
                self.model['groups'].append({'uuid': ident, 'name': ident, 'origin': [0, 0, 0]})
                tree = '[{"uuid": "' + ident + '", "children": ' + tree + '}]'
            # The probe deliberately exceeds parser/traversal resource limits.
            raw = json.dumps(self.model).replace('"outliner": []', '"outliner": ' + tree)
            path.write_text(raw)
            result = subprocess.run([sys.executable, str(SCRIPT), str(path)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 2, result.stdout)
        self.assertIn('UNVERIFIED', result.stdout)
        self.assertNotIn('Traceback', result.stderr)

    def test_no_fake_ground_approval_for_changed_rig_or_jump(self):
        next(g for g in self.model['groups'] if g['name'] == 'right_shin')['origin'][1] += 3
        animation = next(a for a in self.model['animations'] if a['name'] == 'jump_start')
        root = next(t for t in animation['animators'].values() if t['name'] == 'root')
        root['keyframes'][1]['data_points'][0]['y'] = '-4'
        result, _ = self.run_model()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn('MOTION_NOT_CHECKED', result.stdout)
        self.assertNotIn('PASS', result.stdout)


if __name__ == '__main__':
    unittest.main()
