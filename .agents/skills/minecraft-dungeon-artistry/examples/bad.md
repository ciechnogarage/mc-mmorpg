# Bad example - patterns that failed on level_1 (do not repeat)

- **Box branches**: generating a branch as one solid box from trunk to tip
  produced flat "pancake slabs" in the crown. Fix: stepped 2x2 segments.
- **Lollipop tree ring**: trunk + single leaf ball for the near tree wall reads
  as toy scenery next to a detailed anchor. Acceptable only far background.
- **47% uniform flower scatter**: % pattern over the whole meadow = visual noise,
  no readable meadow. Fix: typed clusters + sparse grass.
- **2-block plants in % patterns**: sunflower/tall_grass break (half plants).
  Place singles or skip.
- **Bare skyline plate**: island edge visible against sky in early shots killed
  scale; fixed only by hill ring + double tree wall.
- **Editing functions.yml on disk during an open MD edit session**: autosave
  (300s) reverted the fix within ~1 minute; `md reload` NPEd. Fix: skill alias
  in the MythicMobs pack (`SpawnGroveGuardianOnStart` -> new skill name).
- **Judging particles/models from prismarine-viewer PNGs**: viewer cannot render
  them; also moss/sculk textures are fallback artifacts and yaw/pitch are
  inverted. Misreading these as build errors wastes iterations.
- **Counting mobs via bot**: bot sees only loaded chunks -> false zeros. Count
  via RCON + forceload.
- **Testing death triggers with /kill**: bypasses combat death path; only a real
  combat kill proves onDeath/FinishDungeon wiring.
- **New probe file per experiment**: script sprawl burned context and tokens.
  One parameterized script, phases via argv.
