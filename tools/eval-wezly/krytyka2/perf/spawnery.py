#!/usr/bin/env python3
"""Generuje/usuwa 100 TESTOWYCH spawnerow wezlow (10 typow x 10) w siatce przy spawnie world1.
   python3 spawnery.py utworz | usun      (pliki k2test_*.yml w plugins/MythicMobs/spawners/)"""
import sys, os, glob
D = '/opt/docker/mc/data/world1/plugins/MythicMobs/spawners'
TYPY = ['NODE_TIMBER_OAK', 'NODE_TIMBER_SPRUCE', 'NODE_TIMBER_BIRCH', 'NODE_HERB_PATCH', 'NODE_HERB_MOONLEAF',
        'NODE_MUSHROOM_RING', 'NODE_CROP_FIELD', 'NODE_GEM_GEODE', 'NODE_CLAY_DEPOSIT', 'NODE_SPICE_PATCH']
SZ = '''{n}:
  MobName: {t}
  World: world
  X: {x}
  Y: 101
  Z: {z}
  Yaw: 0.0
  Pitch: 0.0
  Radius: 0.0
  RadiusY: 0.0
  UseTimer: true
  MaxMobs: '1'
  MobLevel: '1'
  MobsPerSpawn: 1
  Cooldown: {cd}
  Warmup: 0
  ActivationRange: 32.0
  ScalingRange: 25.0
  LeashRange: 32.0
  HealOnLeash: false
  ResetThreatOnLeash: false
  Breakable: false
  CheckForPlayers: true
  ShowFlames: false
  Conditions: []
  SpawnConditions: []
  CooldownTimer: 0
'''
if sys.argv[1] == 'utworz':
    cd = int(sys.argv[2]) if len(sys.argv) > 2 else 10
    for i, t in enumerate(TYPY):
        for j in range(10):
            n = f'k2test_{i}_{j}'
            open(f'{D}/{n}.yml', 'w').write(SZ.format(n=n, t=t, x=145 + i, z=190 + j, cd=cd))
    print('utworzono', len(glob.glob(f'{D}/k2test_*.yml')))
else:
    for f in glob.glob(f'{D}/k2test_*.yml'): os.remove(f)
    print('pozostalo k2test:', len(glob.glob(f'{D}/k2test_*.yml')), '| pliki w spawners:', os.listdir(D))
