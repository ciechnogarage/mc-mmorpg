"""Offline JS-generation contracts; these are not a Blockbench runtime test."""
from pathlib import Path
import sys
import json
import tempfile
from unittest.mock import patch
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from review_session import ReviewSession


class SessionContracts(unittest.TestCase):
    def setUp(self):
        self.session = ReviewSession.__new__(ReviewSession)
        self.calls = []
        self.session.js = lambda code: self.calls.append(code)

    def test_snapshot_records_locked_stack_and_timeline_playback(self):
        self.session.snapshot()
        code = self.calls[-1]
        self.assertIn('AnimationItem.all.map', code)
        self.assertIn('playing:a.playing', code)
        self.assertIn('Timeline.playing', code)
        self.assertIn('AnimationItem.selected', code)

    def test_pose_isolates_every_item_even_locked_and_pauses(self):
        self.session.pose('walk', .2)
        code = self.calls[-1]
        self.assertIn('Timeline.pause()', code)
        self.assertIn('AnimationItem.all.forEach', code)
        self.assertIn('item.playing=false', code)
        self.assertIn('a.playing=true', code)
        self.assertLess(code.index('Timeline.pause()'), code.index('Timeline.setTime('))

    def test_restore_reinstates_stack_after_select_and_resumes(self):
        self.session.restore({'project': 'project', 'mode': 'animate', 'animation': 'a',
                              'selected_item': 'a', 'time': .2, 'timeline_playing': True,
                              'items': [{'uuid': 'a', 'playing': True}, {'uuid': 'b', 'playing': 'locked'}]})
        code = self.calls[-1]
        self.assertIn('item.playing=saved.playing', code)
        self.assertIn('Timeline.start()', code)
        self.assertIn('AnimationItem.selected=null', code)
        self.assertLess(code.index('selected.select()'), code.index('item.playing=saved.playing'))


class MeasurementIdentityTests(unittest.TestCase):
    def test_changed_source_during_measurement_stays_incomplete(self):
        import measure_undead_motion as measurement
        class Session:
            expected = {'animations': [{'name': 'walk', 'length': .01, 'animators': {}}]}
            def __init__(self):
                self.binds = 0
            def bind(self, path):
                self.binds += 1
                return {'source_sha256': 'before' if self.binds == 1 else 'after', 'project_fingerprint': 'same'}
            def snapshot(self):
                return {}
            def restore(self, state):
                pass
            def js(self, code):
                if 'new THREE.BoxGeometry' in code:
                    return {'minY': 3, 'maxY': 7}
                return [{'time': t, 'minY': 0, 'feet': {}} for t in (0, .008333333, .01)]
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory) / 'measure'
            with patch.object(measurement, 'ReviewSession', Session), patch.object(sys, 'argv', [
                    'measure', '--source', 'source', '--out', str(out), '--animations', 'walk']):
                with self.assertRaises(ValueError):
                    measurement.main()
            self.assertEqual(json.loads((out / 'motion.json').read_text())['status'], 'INCOMPLETE')


if __name__ == '__main__':
    unittest.main()
