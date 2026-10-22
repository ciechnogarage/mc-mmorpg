"""Offline render lifecycle tests. Fake transport only; image decoding is real."""
import base64
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from PIL import Image

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))


class RenderTests(unittest.TestCase):
    def test_cli_requires_source_and_out_without_connecting(self):
        for script in ('render_undead.py', 'render_undead_motion.py'):
            result = subprocess.run([sys.executable, str(TOOLS / script)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 2)
            self.assertIn('--source', result.stderr)
            self.assertIn('--out', result.stderr)
            self.assertNotIn('Traceback', result.stderr)

    def load_motion(self):
        spec = importlib.util.spec_from_file_location('motion_under_test', TOOLS / 'render_undead_motion.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def test_sampling_includes_real_endpoint(self):
        module = self.load_motion()
        self.assertEqual(module.sample_times(.13, 20), [0, .05, .1, .13])
        self.assertEqual(module.sample_times(.1, 20), [0, .05, .1])

    def test_missing_image_is_failure(self):
        module = self.load_motion()
        with self.assertRaises(ValueError):
            module.decode_capture({'content': [{'type': 'text', 'text': 'nothing'}]})

    def test_review_completion_requires_restore_and_final_bind(self):
        module = self.load_motion()
        for failure in (None, 'capture', 'restore', 'final_bind'):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as directory:
                out = Path(directory) / 'review'
                calls = []
                class Session:
                    def bind(self, source):
                        calls.append('bind')
                        if failure == 'final_bind' and calls.count('bind') == 2:
                            raise ValueError('changed project')
                        return {'source': str(source), 'source_sha256': 'same', 'project_fingerprint': 'same'}
                    def snapshot(self):
                        return {'project': 'test'}
                    def restore(self, state):
                        calls.append('restore')
                        if failure == 'restore':
                            raise ValueError('restore failed')
                def capture(session, manifest):
                    if failure == 'capture':
                        raise ValueError('capture failed')
                    return []
                if failure:
                    with self.assertRaises(Exception):
                        module.run_review(Path('source'), out, {}, capture, Session)
                else:
                    module.run_review(Path('source'), out, {}, capture, Session)
                manifest = json.loads((out / 'manifest.json').read_text())
                self.assertEqual(manifest['status'], 'COMPLETE' if failure is None else 'INCOMPLETE')
                self.assertIn('restore', calls)
                self.assertIn('camera', manifest['limitations'][0])

    def test_bind_failure_creates_no_output_directory(self):
        module = self.load_motion()
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory) / 'review'
            class Session:
                def bind(self, source):
                    raise ValueError('wrong project')
            with self.assertRaises(ValueError):
                module.run_review(Path('source'), out, {}, lambda s, m: [], Session)
            self.assertFalse(out.exists())

    def test_source_change_after_schedule_is_rejected_before_output(self):
        module = self.load_motion()
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory) / 'review'
            class Session:
                def bind(self, source):
                    return {'source_sha256': 'changed'}
                def snapshot(self):
                    return {}
                def restore(self, state):
                    pass
            with self.assertRaises(ValueError):
                module.run_review(Path('source'), out, {'requested_source_sha256': 'original'}, lambda s, m: [], Session)
            self.assertFalse(out.exists())

    def test_valid_image_decode_and_clip_selection(self):
        module = self.load_motion()
        image = Image.new('RGBA', (3, 4), (10, 20, 30, 255))
        buffer = io.BytesIO()
        image.save(buffer, format='PNG')
        decoded = module.decode_capture({'content': [{'type': 'image', 'data': base64.b64encode(buffer.getvalue()).decode()}]})
        self.assertEqual(decoded.size, (3, 4))
        self.assertEqual(decoded.getpixel((0, 0)), (10, 20, 30, 255))
        model = {'animations': [{'name': 'hurt', 'length': .13}]}
        self.assertEqual(module.selected_clips(model, ['hurt'])[0]['length'], .13)
        for names in (['missing'], ['hurt', 'hurt'], ['../hurt']):
            with self.assertRaises(ValueError):
                module.selected_clips(model, names)

    def test_existing_output_is_preserved(self):
        module = self.load_motion()
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory)
            marker = out / 'manifest.json'
            marker.write_text('previous')
            class Session:
                def bind(self, source):
                    return {}
            with self.assertRaises(FileExistsError):
                module.run_review(Path('source'), out, {}, lambda s, m: [], Session)
            self.assertEqual(marker.read_text(), 'previous')


if __name__ == '__main__':
    unittest.main()
