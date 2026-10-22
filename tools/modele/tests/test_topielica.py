import json
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path('/opt/docker/mc/data/world1')  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)


class TopielicaModelTest(unittest.TestCase):
    def test_boss_has_distinct_silhouette_and_combat_animation(self):
        with tempfile.TemporaryDirectory(dir=ROOT / "output") as directory:
            output = Path(directory) / "topielica.bbmodel"
            subprocess.run(
                [str(ROOT / "tools/modele/uruchom.sh"), str(ROOT / "tools/modele/topielica.py"), str(output)],
                check=True,
                capture_output=True,
                text=True,
            )
            model = json.loads(output.read_text())

        cubes = {cube["name"]: cube for cube in model["elements"]}
        animations = {animation["name"]: animation for animation in model["animations"]}
        self.assertGreaterEqual(len(cubes), 45)
        self.assertTrue({"maska_topielicy", "serce_mulu", "wlos_lewy", "wlos_prawy", "jaja"} <= cubes.keys())
        self.assertTrue({"idle", "walk", "attack", "wlosy", "kaluza", "zanurzenie", "wynurzenie", "skrzek", "hurt", "death"} <= animations.keys())
        self.assertEqual(model["resolution"], {"width": 256, "height": 256})
        self.assertGreater(animations["wlosy"]["length"], 1.0)
        self.assertGreater(animations["zanurzenie"]["length"], 1.0)
        self.assertEqual(animations["idle"]["loop"], "loop")


if __name__ == "__main__":
    unittest.main()
