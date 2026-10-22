import sys
import unittest
from pathlib import Path


ROOT = Path('/opt/docker/mc/data/world1')  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)
sys.path.insert(0, str(ROOT / "tools/modele"))


class TopielicaIntegrationTest(unittest.TestCase):
    def test_patches_only_boss_and_its_skills(self):
        from topielica_mm import patch

        source = Path('/opt/docker/mc/forge/levels/loch_002_md/mm')
        mobs = (source / 'mobs/Loch_002.yml').read_text()
        skills = (source / 'skills/Loch_002.yml').read_text()
        updated_mobs, updated_skills = patch(mobs, skills)
        boss = updated_mobs.split('L002_TOPIELICA:', 1)[1].split('\nL002_SKRZEK:', 1)[0]
        self.assertIn('model{m=topielica;n=tag_name;step=1}', boss)
        self.assertIn('~onLoad', boss)
        self.assertIn('state{m=topielica;s=attack', boss)
        self.assertNotIn('minecraft:scale base set 2.4', boss)
        self.assertEqual(mobs.split('L002_TOPIELICA:', 1)[0], updated_mobs.split('L002_TOPIELICA:', 1)[0])
        for name, state in [('L002_WLOSY', 'wlosy'), ('L002_KALUZA', 'kaluza'),
                            ('L002_NURT', 'zanurzenie'), ('L002_SKRZEK', 'skrzek')]:
            block = updated_skills.split(name + ':', 1)[1].split('\nL002_', 1)[0]
            self.assertIn('state{m=topielica;s=' + state, block)
        self.assertEqual((updated_mobs, updated_skills), patch(updated_mobs, updated_skills))


if __name__ == '__main__':
    unittest.main()
