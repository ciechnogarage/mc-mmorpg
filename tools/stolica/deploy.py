#!/usr/bin/env python3
"""Place the generated city structures one by one through world1 RCON."""

import subprocess
import sys
import time
from pathlib import Path

commands = Path('/opt/docker/mc/data/world1/world/datapacks/stolica/data/stolica/function/build.mcfunction').read_text().splitlines()
for index, command in enumerate(commands, 1):
    result = subprocess.run(
        ['docker', 'exec', 'mc-world1-1', 'rcon-cli', command],
        capture_output=True, text=True, timeout=45,
    )
    output = result.stdout.replace('\x00', '').strip()
    print(f'{index}/{len(commands)} {command.split()[6]}: {output or result.stderr.strip()}', flush=True)
    if result.returncode or 'loaded template' not in output.lower():
        sys.exit(f'Placement stopped at item {index}: {output or result.stderr}')
    time.sleep(0.15)
