import tempfile
import unittest
from pathlib import Path

from PIL import Image
from arkusz_bagna import build


ROOT = Path('/opt/docker/mc/data/world1/output/lochy/bagna')


class ArkuszTest(unittest.TestCase):
    def test_piec_paneli_renderowanych_offline(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / 'odbior.png'
            build('pijawka', ROOT, out, size=96)
            with Image.open(out) as im:
                self.assertEqual(im.size, (480, 150))
                self.assertNotEqual(im.crop((96, 0, 192, 96)).getbbox(), None)


if __name__ == '__main__':
    unittest.main()
