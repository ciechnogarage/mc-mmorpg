import assert from 'node:assert/strict';
import test from 'node:test';

import {
  GIT_STATUS_MAX_BUFFER,
  REQUIRED_IGNORE_RULES,
  classifyRepositoryPath,
  commandOutputOrThrow,
  countPathsByBucket,
  parseUntrackedPaths,
  summarizeUntrackedPaths
} from '../scripts/repo-hygiene.mjs';

test('allocates enough buffer for a full repository inventory', () => {
  assert.equal(GIT_STATUS_MAX_BUFFER, 64 * 1024 * 1024);
});

test('uses output from a successful command despite a sandbox stream error', () => {
  assert.equal(
    commandOutputOrThrow({ status: 0, stdout: '?? docs/pvp-foundation.md\n', stderr: '', error: new Error('EPERM') }),
    '?? docs/pvp-foundation.md\n'
  );
});

test('parses null-delimited Git porcelain paths without quote artifacts', () => {
  assert.deepEqual(
    parseUntrackedPaths('?? MCMMORPG/plugins/ModelEngine/resource pack/assets/model.json\0?? docs/pvp.md\0'),
    [
      'MCMMORPG/plugins/ModelEngine/resource pack/assets/model.json',
      'docs/pvp.md'
    ]
  );
});

test('classifies generated server artifacts as runtime data', () => {
  for (const path of [
    'MCMMORPG/backups/server-2026-07-12.tar.gz',
    'MCMMORPG/_validation/node_modules/mineflayer/index.js',
    'MCMMORPG/_validation/runs/foundation_bot_run.md',
    'MCMMORPG/plugin-src/character-stage/build/libs/character-stage.jar',
    'MCMMORPG/plugins/FastAsyncWorldEdit/history/session.bd',
    'MCMMORPG/plugins/WorldGuard/worlds/level_1_2/config.yml',
    'MCMMORPG/plugins/ProtocolLib/lastupdate',
    'MCMMORPG/server.properties.bak.preval',
    'MCMMORPG/_validation/reference_corpus/visual_atlas/example.png',
    'MCMMORPG/_validation/model_reviews/grove_guardian.md',
    'MCMMORPG/l1view/level.dat',
    'MCMMORPG/docker/.env',
    'MCMMORPG/.claude/settings.json',
    'MCMMORPG/_validation/webgl_probe.png',
    'MCMMORPG/_validation/server_npe_before.txt',
    'MCMMORPG/docker/velocity/forwarding.secret',
    'MCMMORPG/docker/velocity/velocity-4.0.0-SNAPSHOT-5.jar',
    'MCMMORPG/docker/velocity/lang/messages_pl_PL.properties',
    'MCMMORPG/_coord/briefs/codex1.md',
    'MCMMORPG/plugins/MMOProfiles/userdata/uuid.yml',
    'MCMMORPG/plugins/MythicMobs/data/players/uuid.yml',
    '.mcp.json',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/data/chunks.dat',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/entities/r.0.0.mca',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/mantle/pv.0.ttp.lz4b',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/players/uuid.yml',
    'MCMMORPG/plugins/Iris/packs/level_1/.iris/schema/biomes-schema.json',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/iris/pack/.iris/schema/biomes-schema.json',
    'MCMMORPG/plugins/Nexo/pack/pack.zip',
    'MCMMORPG/plugins/Nexo/pack/.deobfCachedPacks/cache.zip',
    'MCMMORPG/_validation/archive/legacy-probes/probe.js'
  ]) {
    assert.equal(classifyRepositoryPath(path), 'runtime', path);
  }
});

test('preserves authored game content as source', () => {
  for (const path of [
    'MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml',
    'MCMMORPG/plugins/ModelEngine/blueprints/level_1_grove_guardian.bbmodel',
    'MCMMORPG/plugins/Nexo/pack/assets/mcmmorpg/items/pet_treat.json',
    'MCMMORPG/plugin-src/character-stage/src/main/java/CharacterStagePlugin.java',
    'docs/pvp-foundation-v0.0.1.md'
  ]) {
    assert.equal(classifyRepositoryPath(path), 'source', path);
  }
});

test('requires ignore coverage for generated artifacts', () => {
  assert.deepEqual(REQUIRED_IGNORE_RULES, [
    '.mcp.json',
    'MCMMORPG/.console_history',
    'MCMMORPG/_coord/briefs/',
    'MCMMORPG/_coord/results/',
    'MCMMORPG/_validation/node_modules/',
    'MCMMORPG/_validation/runs/',
    'MCMMORPG/_validation/model_backups/',
    'MCMMORPG/_validation/reference_corpus/',
    'MCMMORPG/_validation/reviews/',
    'MCMMORPG/_validation/model_reviews/',
    'MCMMORPG/_validation/shots/',
    'MCMMORPG/_validation/model_studies/',
    'MCMMORPG/_validation/active_runtime_reviews/',
    'MCMMORPG/_validation/archive/legacy-probes/',
    'MCMMORPG/_validation/*.out',
    'MCMMORPG/_validation/*.png',
    'MCMMORPG/_validation/*.txt',
    'MCMMORPG/backups/',
    'MCMMORPG/.claude/',
    'MCMMORPG/docker/servers/',
    'MCMMORPG/docker/.env',
    'MCMMORPG/docker/velocity/.papermc-manifest.json',
    'MCMMORPG/docker/velocity/.results.env',
    'MCMMORPG/docker/velocity/forwarding.secret',
    'MCMMORPG/docker/velocity/*.jar',
    'MCMMORPG/docker/velocity/lang/',
    'MCMMORPG/docker/velocity/plugins/',
    'MCMMORPG/plugins/**/userdata/',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/data/',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/entities/',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/mantle/',
    'MCMMORPG/plugins/MythicDungeons/maps/level_1/players/',
    'MCMMORPG/plugins/Iris/packs/**/.iris/',
    'MCMMORPG/plugins/MythicDungeons/maps/**/iris/pack/.iris/',
    'MCMMORPG/plugins/CMI/DatabaseBackups/',
    'MCMMORPG/plugins/FastAsyncWorldEdit/history/',
    'MCMMORPG/plugin-src/**/build/',
    'MCMMORPG/disabled-runtime-blockers/plugins/**/*.jar',
    'MCMMORPG/plugins/Iris/worlds.json',
    'MCMMORPG/plugins/MythicDungeons/config.yml.bak*',
    'MCMMORPG/plugins/MythicDungeons/maps/dungeon_test/',
    'MCMMORPG/plugins/MythicMobs/data/',
    'MCMMORPG/plugins/MythicMobs/generation/',
    'MCMMORPG/plugins/Nexo/settings.yml.bak*',
    'MCMMORPG/plugins/Nexo/pack/.deobfCachedPacks/',
    'MCMMORPG/plugins/Nexo/pack/pack.zip',
    'MCMMORPG/plugins/ProtocolLib/lastupdate',
    'MCMMORPG/plugins/WorldGuard/worlds/dungeon_test/',
    'MCMMORPG/plugins/WorldGuard/worlds/level_1*/',
    'MCMMORPG/plugins/WorldGuard/worlds/l1view/',
    'MCMMORPG/plugins/WorldGuard/worlds/voidtest/',
    'MCMMORPG/plugins/WorldGuard/worlds/vt_*/',
    'MCMMORPG/l1view/',
    'MCMMORPG/scratchpad/',
    'MCMMORPG/server.properties.bak*'
  ]);
});

test('separates untracked runtime output from source candidates', () => {
  assert.deepEqual(
    summarizeUntrackedPaths([
      'MCMMORPG/_validation/runs/foundation_bot_run.md',
      'MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml',
      'docs/pet-companion-minion-system-foundation-v0.0.1.md'
    ]),
    {
      runtime: ['MCMMORPG/_validation/runs/foundation_bot_run.md'],
      source: [
        'MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml',
        'docs/pet-companion-minion-system-foundation-v0.0.1.md'
      ]
    }
  );
});

test('groups source candidates by ownership bucket', () => {
  assert.deepEqual(
    countPathsByBucket([
      '.agents/skills/minecraft-rpg-systems/SKILL.md',
      'docs/pvp-foundation-v0.0.1.md',
      'MCMMORPG/plugins/MythicMobs/Packs/level_1/mobs/grove_guardian.mob.yml',
      'MCMMORPG/plugins/MMOItems/item/sword.yml',
      'MCMMORPG/_validation/foundation_bot.js'
    ]),
    [
      { bucket: 'MCMMORPG/plugins', count: 2 },
      { bucket: '.agents', count: 1 },
      { bucket: 'docs', count: 1 },
      { bucket: 'MCMMORPG/_validation', count: 1 }
    ]
  );
});
