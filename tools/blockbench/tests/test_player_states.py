import copy
import importlib.util
import json
from pathlib import Path
import sys
import unittest

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))


def module():
    spec = importlib.util.spec_from_file_location('states', TOOLS / 'author_player_states.py')
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


class PlayerStateTests(unittest.TestCase):
    def test_run_retime_preserves_values_and_endpoint_without_mutation(self):
        m = module()
        walk = {'uuid': 'walk-id', 'name': 'walk', 'length': 1, 'loop': 'loop', 'animators': {
            'bone': {'name': 'right_leg', 'type': 'bone', 'keyframes': [
                {'uuid': 'k0', 'time': 0, 'channel': 'rotation', 'data_points': [{'x': 5, 'y': 0, 'z': 0}]},
                {'uuid': 'k1', 'time': .5, 'channel': 'rotation', 'data_points': [{'x': -5, 'y': 0, 'z': 0}]},
                {'uuid': 'k2', 'time': 1, 'channel': 'rotation', 'data_points': [{'x': 5, 'y': 0, 'z': 0}]}]}}}
        original = copy.deepcopy(walk)
        run = m.retime_clip(walk, 'run', .7)
        self.assertEqual(walk, original)
        keys = run['animators']['bone']['keyframes']
        self.assertEqual([k['time'] for k in keys], [0, .35, .7])
        self.assertEqual([k['data_points'] for k in keys], [k['data_points'] for k in walk['animators']['bone']['keyframes']])
        self.assertNotIn('uuid', run)
        self.assertTrue(all('uuid' not in k for k in keys))

    def test_unimplemented_states_are_not_dummy_clips(self):
        m = module()
        self.assertEqual(set(m.CREATED), {'run', 'crouch_idle', 'bow_draw', 'bow_hold', 'block'})
        self.assertEqual(set(m.DEFERRED), {'crouch_walk', 'swim_idle', 'swim'})

    def test_existing_animation_mutation_is_detected(self):
        m = module()
        before = {'name': 'model', 'elements': [{'from': [0, 0, 0]}], 'animations': [{'name': 'idle', 'uuid': 'old', 'length': 1}], 'textures': []}
        after = copy.deepcopy(before)
        after['animations'] += [{'name': name, 'uuid': name, 'length': 1} for name in m.CREATED]
        m.verify_only_new_clips(before, after)
        after['animations'][0]['length'] = 2
        with self.assertRaises(ValueError):
            m.verify_only_new_clips(before, after)

    def test_boolean_geometry_change_is_detected(self):
        m = module()
        before = {'elements': [{'visibility': True}], 'animations': [], 'textures': []}
        after = copy.deepcopy(before)
        after['animations'] = [{'name': name} for name in m.CREATED]
        after['elements'][0]['visibility'] = 1
        with self.assertRaises(ValueError):
            m.verify_only_new_clips(before, after)


if __name__ == '__main__':
    unittest.main()
