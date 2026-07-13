const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const ROOT = path.resolve(__dirname, '..');

const requiredFiles = [
  'plugin-src/character-stage/build.gradle.kts',
  'plugin-src/character-stage/settings.gradle.kts',
  'plugin-src/character-stage/README.md',
  'plugin-src/character-stage/src/main/resources/plugin.yml',
  'plugin-src/character-stage/src/main/resources/config.yml',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/CharacterStagePlugin.java',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/StageConfig.java',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/CharacterStageService.java',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/JoinStageListener.java',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/StageInteractListener.java',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/StageCommand.java',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/ProfileBackendAdapter.java',
  'plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/MmoProfilesReflectionAdapter.java',
  'plugin-src/character-stage/tools/BuildCharacterStage.java',
  'plugin-src/character-stage/build/libs/character-stage-0.1.0-SNAPSHOT.jar',
  'plugins/CharacterStage-0.1.0-SNAPSHOT.jar',
  'docs/character-stage-architecture.md',
];

const missing = requiredFiles.filter((rel) => !fs.existsSync(path.join(ROOT, rel)));
if (missing.length) {
  console.error('MISSING_FILES');
  for (const file of missing) console.error(file);
  process.exit(1);
}

const read = (rel) => fs.readFileSync(path.join(ROOT, rel), 'utf8');
const configYml = read('plugin-src/character-stage/src/main/resources/config.yml');
const pluginYml = read('plugin-src/character-stage/src/main/resources/plugin.yml');
const serviceJava = read('plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/CharacterStageService.java');
const pluginJava = read('plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/CharacterStagePlugin.java');
const adapterJava = read('plugin-src/character-stage/src/main/java/pl/mcmmorpg/characterstage/MmoProfilesReflectionAdapter.java');
const buildJava = read('plugin-src/character-stage/tools/BuildCharacterStage.java');
const introMenu = read('plugins/CoreTools/MenuCreator/foundation_creator_intro.yml');

function jarConfig(rel) {
  const jar = fs.readFileSync(path.join(ROOT, rel));
  const eocdSignature = 0x06054b50;
  let eocdOffset = -1;
  for (let i = jar.length - 22; i >= 0; i--) {
    if (jar.readUInt32LE(i) === eocdSignature) {
      eocdOffset = i;
      break;
    }
  }
  if (eocdOffset === -1) throw new Error(`ZIP central directory not found: ${rel}`);

  const entryCount = jar.readUInt16LE(eocdOffset + 10);
  let offset = jar.readUInt32LE(eocdOffset + 16);
  for (let i = 0; i < entryCount; i++) {
    if (jar.readUInt32LE(offset) !== 0x02014b50) throw new Error(`Bad ZIP central entry: ${rel}`);
    const method = jar.readUInt16LE(offset + 10);
    const compressedSize = jar.readUInt32LE(offset + 20);
    const fileNameLength = jar.readUInt16LE(offset + 28);
    const extraLength = jar.readUInt16LE(offset + 30);
    const commentLength = jar.readUInt16LE(offset + 32);
    const localHeaderOffset = jar.readUInt32LE(offset + 42);
    const name = jar.slice(offset + 46, offset + 46 + fileNameLength).toString('utf8');

    if (name === 'config.yml') {
      if (jar.readUInt32LE(localHeaderOffset) !== 0x04034b50) throw new Error(`Bad ZIP local entry: ${rel}`);
      const localNameLength = jar.readUInt16LE(localHeaderOffset + 26);
      const localExtraLength = jar.readUInt16LE(localHeaderOffset + 28);
      const dataOffset = localHeaderOffset + 30 + localNameLength + localExtraLength;
      const compressed = jar.slice(dataOffset, dataOffset + compressedSize);
      if (method === 0) return compressed.toString('utf8');
      if (method === 8) return zlib.inflateRawSync(compressed).toString('utf8');
      throw new Error(`Unsupported ZIP method ${method}: ${rel}`);
    }

    offset += 46 + fileNameLength + extraLength + commentLength;
  }
  throw new Error(`config.yml not found in jar: ${rel}`);
}

const runtimeJarConfig = jarConfig('plugins/CharacterStage-0.1.0-SNAPSHOT.jar');
const buildJarConfig = jarConfig('plugin-src/character-stage/build/libs/character-stage-0.1.0-SNAPSHOT.jar');

const checks = [
  ['plugin_name', pluginYml.includes('name: CharacterStage')],
  ['plugin_main', pluginYml.includes('main: pl.mcmmorpg.characterstage.CharacterStagePlugin')],
  ['stage_spawn', configYml.includes('stage:') && configYml.includes('spawn:')],
  ['confirm_anchor', configYml.includes('confirm_anchor:')],
  ['four_profile_slots', ['profile_index: 1', 'profile_index: 2', 'profile_index: 3', 'profile_index: 4'].every((needle) => configYml.includes(needle))],
  ['slot_create_actions', (configYml.match(/console:core-menu foundation_creator_intro %player%/g) || []).length === 4],
  ['occupied_profile_bridge', (configYml.match(/console:mmoprofiles open %player%/g) || []).length === 4],
  ['no_stage_todo', !/TODO activate backend profile|console:say CharacterStage TODO/.test(configYml)],
  ['jar_config_synced_runtime', runtimeJarConfig === configYml],
  ['jar_config_synced_build', buildJarConfig === configYml],
  ['join_intercept', serviceJava.includes('stage(event.getPlayer())') || pluginJava.includes('JoinStageListener')],
  ['focus_confirm_flow', serviceJava.includes('confirmFocusedSlot') && serviceJava.includes('focusSlot')],
  ['mmoprofiles_reflection', adapterJava.includes('getPlayerData') && adapterJava.includes('getProfiles')],
  ['local_builder', buildJava.includes('ToolProvider.getSystemJavaCompiler') && buildJava.includes('packageJar')],
  ['intro_menu_world_first_copy', introMenu.includes('Kompania Bohaterow - Wybor Postaci') && introMenu.includes('Stolica Wyspy')],
  ['intro_menu_creator_entry', introMenu.includes('creator_open_race_gallery')],
];

const failed = checks.filter(([, ok]) => !ok);
if (failed.length) {
  console.error('FAILED_CHECKS');
  for (const [name] of failed) console.error(name);
  process.exit(1);
}

console.log('PASS character-stage scaffold and WoW-style creator entry present');
