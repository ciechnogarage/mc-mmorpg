import copy
import importlib
import math
import hashlib
import json
from pathlib import Path
import sys
import unittest
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


class MotionProgramTests(unittest.TestCase):
    def setUp(self):
        self.m = importlib.import_module('player_motion_program')

    def test_leg_solve_reaches_requested_sole_without_tilting_foot(self):
        for root_y, lift, z in [(-1, 0, 4), (-3.1, 0, -1.5), (-1.4, 1.5, 2)]:
            hip, knee, ankle = self.m.solve_leg(5.5, 5, root_y, lift, z)
            a, b = math.radians(hip), math.radians(hip + knee)
            self.assertAlmostEqual(12 + root_y - 5.5 * math.cos(a) - 5 * math.cos(b) - 1.5, lift)
            self.assertAlmostEqual(-5.5 * math.sin(a) - 5 * math.sin(b), z)
            self.assertAlmostEqual(hip + knee + ankle, 0)

    def test_impossible_leg_pose_is_rejected_not_silently_clamped(self):
        with self.assertRaises(ValueError):
            self.m.solve_leg(5.5, 5, 0, 0, 30)

    def test_mirror_swaps_arms_and_rotation_y_z_without_mutating_input(self):
        original = {'right_arm': {'rotation': [[0, [30, 12, -8]]]}, 'torso': {'rotation': [[0, [4, 5, 6]]]}}
        saved = copy.deepcopy(original)
        mirrored = self.m.mirror_tracks(original)
        self.assertEqual(mirrored['left_arm']['rotation'][0][1], [30, -12, 8])
        self.assertEqual(mirrored['torso']['rotation'][0][1], [4, -5, -6])
        self.assertEqual(original, saved)

    def test_generated_states_have_finite_closed_loops_and_expected_contract(self):
        groups = {s + '_' + p: {'origin': [x, y, 0], 'rotation': [0, 0, 0]}
                  for s, x in [('right', 1.8), ('left', -1.8)]
                  for p, y in [('leg', 12), ('shin', 6.5), ('foot', 1.5)]}
        clips = self.m.build_states(groups)
        self.assertEqual(set(clips), {'run', 'crouch_idle', 'crouch_walk', 'swim', 'swim_idle', 'bow_draw', 'bow_hold', 'block', 'bow_draw_left', 'bow_hold_left', 'block_right'})
        for name, clip in clips.items():
            for channels in clip['tracks'].values():
                for frames in channels.values():
                    self.assertEqual(frames[0][0], 0, name)
                    self.assertEqual(frames[-1][0], clip['length'], name)
                    self.assertTrue(all(math.isfinite(v) for _, xyz in frames for v in xyz))
                    if clip['loop'] == 'loop':
                        for first, last in zip(frames[0][1], frames[-1][1]):
                            self.assertAlmostEqual(first, last, places=7, msg=name)
        self.assertFalse(any('leg' in bone or bone == 'root' for bone in clips['bow_hold']['tracks']))

    def test_bow_item_arm_is_extended_and_opposite_arm_draws(self):
        groups = {s + '_' + p: {'origin': [x, y, 0], 'rotation': [0, 0, 0]}
                  for s, x in [('right', 1.8), ('left', -1.8)]
                  for p, y in [('leg', 12), ('shin', 6.5), ('foot', 1.5)]}
        clips = self.m.build_states(groups)
        for name, holding, drawing in [('bow_hold', 'right', 'left'), ('bow_hold_left', 'left', 'right')]:
            tracks = clips[name]['tracks']
            self.assertLess(tracks[holding + '_forearm']['rotation'][0][1][0], 10)
            self.assertGreater(tracks[drawing + '_forearm']['rotation'][0][1][0], 60)

    def test_publication_rejects_source_change_and_leaves_incomplete_manifest(self):
        author = importlib.import_module('author_complete_player_motion')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'source'
            source.write_bytes(b'old')
            manifest = {'source_sha256': hashlib.sha256(b'old').hexdigest()}
            source.write_bytes(b'new')
            with self.assertRaises(ValueError):
                author.publish_candidate(root, source, manifest, {'elements': []})
            self.assertFalse((root / 'race_nieumarly.bbmodel').exists())
            self.assertEqual(json.loads((root / 'manifest.json').read_text())['status'], 'INCOMPLETE')

    def test_publication_marks_write_failure_incomplete(self):
        author = importlib.import_module('author_complete_player_motion')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'source'
            source.write_bytes(b'old')
            (root / 'race_nieumarly.bbmodel').mkdir()
            manifest = {'source_sha256': hashlib.sha256(b'old').hexdigest()}
            with self.assertRaises(OSError):
                author.publish_candidate(root, source, manifest, {'elements': []})
            self.assertEqual(json.loads((root / 'manifest.json').read_text())['status'], 'INCOMPLETE')


if __name__ == '__main__':
    unittest.main()
