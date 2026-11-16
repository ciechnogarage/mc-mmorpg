"""Apply the Topielica model overlay to the Loch 002 forge and generated MM files.

Run with explicit --apply after review. Idempotent; creates no backups itself.
"""
import argparse
from pathlib import Path

from topielica_mm import patch


ROOT = Path('/opt/docker/mc/forge')
GENERATOR = ROOT / 'tools/build_loch002_md.py'
MOBS = ROOT / 'levels/loch_002_md/mm/mobs/Loch_002.yml'
SKILLS = ROOT / 'levels/loch_002_md/mm/skills/Loch_002.yml'
INJECTION = """# Model Topielicy: wyglad i zapowiedzi atakow z generatora world1.
sys.path.insert(0, '/home/dev/Desktop/Przemek/MC/tools/modele')
from topielica_mm import patch as _topielica_patch
MOBS, S = _topielica_patch(MOBS, S)

"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    source = GENERATOR.read_text()
    anchor = '# ---------------- zapis\n'
    if INJECTION not in source:
        if source.count(anchor) != 1:
            raise ValueError('Missing generator save anchor')
        source = source.replace(anchor, INJECTION + anchor, 1)
    mobs, skills = patch(MOBS.read_text(), SKILLS.read_text())
    if args.apply:
        GENERATOR.write_text(source)
        MOBS.write_text(mobs)
        SKILLS.write_text(skills)
        print('Updated forge generator and generated MM files')
    else:
        print('Dry run OK. Pass --apply to update forge generator and generated MM files.')


if __name__ == '__main__':
    main()
