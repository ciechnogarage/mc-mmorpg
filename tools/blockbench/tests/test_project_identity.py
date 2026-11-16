import copy
import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path('/opt/docker/mc/data/world1')  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)
MODULE = ROOT / 'tools/blockbench/project_identity.py'

class IdentityTests(unittest.TestCase):
    def setUp(self):
        self.assertTrue(MODULE.exists(), 'identity guard is missing')
        spec = importlib.util.spec_from_file_location('identity', MODULE)
        self.api = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.api)
        self.model = json.loads((ROOT/'output/undead-player-v5/face-review/candidate-B-face-v2.bbmodel').read_text())

    def test_project_label_and_selection_do_not_change_identity(self):
        other = copy.deepcopy(self.model)
        other['name'] = 'new tab label'
        other['animations'][0]['selected'] = True
        self.assertEqual(self.api.fingerprint(self.model), self.api.fingerprint(other))

    def test_geometry_texture_and_animation_changes_are_detected(self):
        for field in ('geometry', 'texture', 'animation'):
            other = copy.deepcopy(self.model)
            if field == 'geometry': other['elements'][0]['from'][0] += .01
            if field == 'texture': other['textures'][0]['source'] += 'changed'
            if field == 'animation': other['animations'][0]['length'] += .1
            with self.subTest(field=field):
                with self.assertRaises(ValueError): self.api.require_match(self.model, other)

    def test_unknown_fields_fail_closed(self):
        other = copy.deepcopy(self.model)
        other['new_export_behavior'] = 42
        with self.assertRaises(ValueError): self.api.require_match(self.model, other)

    def test_editor_added_empty_bone_tracks_are_equivalent(self):
        other = copy.deepcopy(self.model)
        bone = next(g for g in other['groups'] if g['uuid'] not in other['animations'][0]['animators'])
        other['animations'][0]['animators'][bone['uuid']] = {
            'name': bone['name'], 'type': 'bone', 'rotation_global': False,
            'quaternion_interpolation': False}
        self.assertEqual(self.api.fingerprint(self.model), self.api.fingerprint(other))
        other['animations'][0]['animators'][bone['uuid']]['unknown_setting'] = True
        self.assertNotEqual(self.api.fingerprint(self.model), self.api.fingerprint(other))

    def test_unselected_flag_defaults_equivalent(self):
        other = copy.deepcopy(self.model)
        other['animations'][0].pop('selected', None)
        self.assertEqual(self.api.fingerprint(self.model), self.api.fingerprint(other))

if __name__ == '__main__': unittest.main()
