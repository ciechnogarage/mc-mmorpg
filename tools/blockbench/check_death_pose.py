"""Read-only death geometry check; explicit verified source and fresh output.

Legacy correction generation was removed. This command never writes
historical death-ground.json or edits animation keyframes.
"""
import sys
from measure_undead_motion import main

if __name__ == '__main__':
    if '--animations' in sys.argv:
        raise SystemExit('check_death_pose selects death; use measure_undead_motion for other clips')
    sys.argv.extend(['--animations','death'])
    raise SystemExit(main())
