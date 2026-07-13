const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

const files = {
  creatorIntroMenu: 'plugins/CoreTools/MenuCreator/foundation_creator_intro.yml',
  raceMenu: 'plugins/CoreTools/MenuCreator/foundation_race_selector.yml',
  raceSpotlightCzlowiekMenu: 'plugins/CoreTools/MenuCreator/foundation_race_spotlight_czlowiek.yml',
  raceSpotlightOrkMenu: 'plugins/CoreTools/MenuCreator/foundation_race_spotlight_ork.yml',
  raceSpotlightElfMenu: 'plugins/CoreTools/MenuCreator/foundation_race_spotlight_elf.yml',
  raceSpotlightKrasnoludMenu: 'plugins/CoreTools/MenuCreator/foundation_race_spotlight_krasnolud.yml',
  raceSpotlightNieumarlyMenu: 'plugins/CoreTools/MenuCreator/foundation_race_spotlight_nieumarly.yml',
  styleMenu: 'plugins/CoreTools/MenuCreator/foundation_style_test.yml',
  classMenu: 'plugins/CoreTools/MenuCreator/foundation_class_selector.yml',
  classConfirmMenu: 'plugins/CoreTools/MenuCreator/foundation_class_confirm.yml',
  classSpotlightWarriorMenu: 'plugins/CoreTools/MenuCreator/foundation_class_spotlight_warrior.yml',
  classSpotlightRogueMenu: 'plugins/CoreTools/MenuCreator/foundation_class_spotlight_rogue.yml',
  classSpotlightMarksmanMenu: 'plugins/CoreTools/MenuCreator/foundation_class_spotlight_marksman.yml',
  classSpotlightMageMenu: 'plugins/CoreTools/MenuCreator/foundation_class_spotlight_mage.yml',
  classSpotlightPaladinMenu: 'plugins/CoreTools/MenuCreator/foundation_class_spotlight_paladin.yml',
  classConfirmWarriorMenu: 'plugins/CoreTools/MenuCreator/foundation_class_confirm_warrior.yml',
  classConfirmRogueMenu: 'plugins/CoreTools/MenuCreator/foundation_class_confirm_rogue.yml',
  classConfirmMarksmanMenu: 'plugins/CoreTools/MenuCreator/foundation_class_confirm_marksman.yml',
  classConfirmMageMenu: 'plugins/CoreTools/MenuCreator/foundation_class_confirm_mage.yml',
  classConfirmPaladinMenu: 'plugins/CoreTools/MenuCreator/foundation_class_confirm_paladin.yml',
  classMentorMenu: 'plugins/CoreTools/MenuCreator/foundation_class_mentor_roster.yml',
  cityRosterMenu: 'plugins/CoreTools/MenuCreator/foundation_city_roster.yml',
  skillRosterMenu: 'plugins/CoreTools/MenuCreator/foundation_skill_roster.yml',
  professionRosterMenu: 'plugins/CoreTools/MenuCreator/foundation_profession_roster.yml',
  firstContractMenu: 'plugins/CoreTools/MenuCreator/foundation_first_contract.yml',
  storageMenu: 'plugins/CoreTools/MenuCreator/foundation_storage.yml',
  nexusMenu: 'plugins/DeluxeMenus/gui_menus/foundation_nexus.yml',
  scripts: 'plugins/CoreTools/Scripts/foundation_character_flow.yml',
  variables: 'plugins/CoreTools/Variables/foundation_character.yml',
  inventory: 'plugins/MMOInventory/inventory/default_mmoinventory.yml',
  permissions: 'permissions.yml',
  coreCommands: 'plugins/CoreTools/commands.yml',
  mmoCoreCommands: 'plugins/MMOCore/commands.yml',
  mmoProfilesConfig: 'plugins/MMOProfiles/config.yml',
  mmoCoreConfig: 'plugins/MMOCore/config.yml',
  mmoItemsConfig: 'plugins/MMOItems/config.yml',
  mmoInventoryConfig: 'plugins/MMOInventory/config.yml',
  mmoItemsDrops: 'plugins/MMOItems/drops.yml',
  mmoItemTiers: 'plugins/MMOItems/item-tiers.yml',
  foundationForge: 'plugins/MMOItems/crafting-stations/foundation-forge.yml',
  arcaneForge: 'plugins/MMOItems/crafting-stations/arcane-forge.yml',
  mythicalForge: 'plugins/MMOItems/crafting-stations/mythical-forge.yml',
  steelStation: 'plugins/MMOItems/crafting-stations/steel-crafting-station.yml',
  itemSets: 'plugins/MMOItems/item-sets.yml',
  customStats: 'plugins/MMOItems/custom-stats.yml',
  contract: 'docs/character-sector-contract.md',
  creatorVisualLanguage: 'docs/creator-visual-language.md',
  creatorAssetContract: 'docs/assets/creator-asset-contract.md',
  creatorAssetManifest: 'plugins/MythicHUD/source-pack/assets/mythichud/creator/creator_assets.json',
};

const typeFiles = {
  ARMOR: 'plugins/MMOItems/item/armor.yml',
  BOW: 'plugins/MMOItems/item/bow.yml',
  CATALYST: 'plugins/MMOItems/item/catalyst.yml',
  CONSUMABLE: 'plugins/MMOItems/item/consumable.yml',
  DAGGER: 'plugins/MMOItems/item/dagger.yml',
  GEM_STONE: 'plugins/MMOItems/item/gem_stone.yml',
  MATERIAL: 'plugins/MMOItems/item/material.yml',
  MISCELLANEOUS: 'plugins/MMOItems/item/miscellaneous.yml',
  OFF_CATALYST: 'plugins/MMOItems/item/off_catalyst.yml',
  RING: 'plugins/MMOItems/item/ring.yml',
  SHIELD: 'plugins/MMOItems/item/shield.yml',
  STAFF: 'plugins/MMOItems/item/staff.yml',
  SWORD: 'plugins/MMOItems/item/sword.yml',
  WAND: 'plugins/MMOItems/item/wand.yml',
};

const starterSafeTypes = new Set([
  'ARMOR',
  'BOW',
  'DAGGER',
  'OFF_CATALYST',
  'RING',
  'SHIELD',
  'STAFF',
  'SWORD',
  'WAND',
]);

const requiredDefaultPermissions = [
  'mmocore.profile',
  'mmocore.class-select',
  'mmocore.attributes',
  'mmocore.skills',
  'mmocore.skilltrees',
  'custom_inv.open.mmoinventory_default',
  'mmoitems.foundation_forge',
  'dungeons.play',
  'dungeons.play.send',
  'mythicdungeons.play.level_1',
];

const classTreeContracts = {
  'plugins/MMOCore/classes/warrior.yml': 'warrior-paladin',
  'plugins/MMOCore/classes/paladin.yml': 'warrior-paladin',
  'plugins/MMOCore/classes/rogue.yml': 'rogue-marksman',
  'plugins/MMOCore/classes/marksman.yml': 'rogue-marksman',
  'plugins/MMOCore/classes/mage/mage.yml': 'mage-arcane-mage',
};

const familyTreeContracts = [
  { dir: 'plugins/MMOCore/classes/warrior', tree: 'warrior-paladin' },
  { dir: 'plugins/MMOCore/classes/paladin', tree: 'warrior-paladin' },
  { dir: 'plugins/MMOCore/classes/rogue', tree: 'rogue-marksman' },
  { dir: 'plugins/MMOCore/classes/marksman', tree: 'rogue-marksman' },
  { dir: 'plugins/MMOCore/classes/mage', tree: 'mage-arcane-mage' },
];

const canonicalClassIds = new Set([
  'warrior',
  'rogue',
  'marksman',
  'mage',
  'paladin',
]);

const requiredCharacterVariables = [
  'race_id',
  'race_selected',
  'pending_race_id',
  'pending_class_id',
  'class_id',
  'character_stage',
  'class_selected',
  'first_contract_started',
  'first_contract_completed',
  'first_weapon_claimed',
  'skill_roster_opened',
  'selected_generic_skill_1',
  'selected_generic_skill_2',
  'profession_intro_started',
  'profession_intro_completed',
  'mentor_intro_warrior',
  'mentor_intro_rogue',
  'mentor_intro_marksman',
  'mentor_intro_mage',
  'mentor_intro_paladin',
  'subclass_preview_warrior',
  'subclass_preview_rogue',
  'subclass_preview_marksman',
  'subclass_preview_mage',
  'subclass_preview_paladin',
  'creator_intro_seen',
  'race_spotlight_czlowiek',
  'race_spotlight_ork',
  'race_spotlight_elf',
  'race_spotlight_krasnolud',
  'race_spotlight_nieumarly',
  'class_spotlight_warrior',
  'class_spotlight_rogue',
  'class_spotlight_marksman',
  'class_spotlight_mage',
  'class_spotlight_paladin',
];

const contractV2Markers = [
  'foundation v0.4',
  'Creator Visual Language',
  'City NPC Roster',
  'Skill Roster Rules',
  'City-Acquired First Equipment',
  'Race Passive Matrix',
  'Class Selection Rules',
  'Mastery Gates',
  'Storage Access Rules',
  'Runtime Acceptance Path',
  'Product Proof',
];

const legacyStationContracts = [
  { key: 'arcaneForge', file: files.arcaneForge },
  { key: 'mythicalForge', file: files.mythicalForge },
  { key: 'steelStation', file: files.steelStation },
];

const legacySetIds = [
  'ARCANE',
  'STEEL',
  'GINGERBREAD',
  'DRAGON',
  'PSYCHIC',
  'UNDEADSLAYER',
  'SPELLCASTER',
  'OMNIELEMENTAL',
  'HATRED',
];

const legacyItemContracts = [
  { type: 'GEM_STONE', id: 'RUBY' },
  { type: 'GEM_STONE', id: 'GEM_OF_ACCURACY' },
  { type: 'GEM_STONE', id: 'GEM_OF_LIFE' },
  { type: 'GEM_STONE', id: 'NATURE_GIFT' },
  { type: 'GEM_STONE', id: 'SPITEFUL_OPAQUE_DIAMOND' },
  { type: 'GEM_STONE', id: 'GOLDEN_RELIC' },
  { type: 'GEM_STONE', id: 'BLAZE_SOUL' },
  { type: 'GEM_STONE', id: 'NETHER_GEM' },
  { type: 'GEM_STONE', id: 'RAINBOW_GEM' },
  { type: 'CATALYST', id: 'NATURES_GIFT' },
  { type: 'CATALYST', id: 'LUCKY_CHARM' },
  { type: 'CATALYST', id: 'WARRIOR_AMULET' },
  { type: 'CATALYST', id: 'FORGOTTEN_IDOL' },
  { type: 'CATALYST', id: 'FIRE_TOTEM' },
  { type: 'CATALYST', id: 'ENCHANTED_SHULKER_SHELL' },
  { type: 'CATALYST', id: 'PERFECT_ENDER_PEARL' },
  { type: 'CATALYST', id: 'POWER_CRYSTAL' },
  { type: 'CATALYST', id: 'LUCK_CHARM' },
  { type: 'CATALYST', id: 'THIN_TRANSPARENT_GLOVES' },
  { type: 'CATALYST', id: 'ICE_GOLEM_HEART' },
  { type: 'CATALYST', id: 'SHRINKIFIKATOR' },
  { type: 'MISCELLANEOUS', id: 'HUMAN_SOUL' },
  { type: 'MISCELLANEOUS', id: 'MYSTERIOUS_PEARL' },
  { type: 'MISCELLANEOUS', id: 'LEVEL_1_KEY' },
];

function read(rel) {
  return fs.readFileSync(path.join(ROOT, rel), 'utf8');
}

function exists(rel) {
  return fs.existsSync(path.join(ROOT, rel));
}

function fail(message, details = {}) {
  return { ok: false, message, details };
}

function pass(message, details = {}) {
  return { ok: true, message, details };
}

function topLevelKeys(text) {
  return new Set([...text.matchAll(/^([A-Za-z0-9_]+):\s*$/gm)].map(match => match[1]));
}

function duplicateTopLevelKeys(text) {
  const seen = new Set();
  const duplicates = new Set();
  for (const match of text.matchAll(/^([A-Za-z0-9_]+):\s*$/gm)) {
    if (seen.has(match[1])) duplicates.add(match[1]);
    seen.add(match[1]);
  }
  return [...duplicates];
}

function duplicateYamlKeys(text) {
  const frames = [{ indent: -1, path: '', seen: new Set() }];
  const duplicates = [];
  const keyPattern = /^(\s*)([A-Za-z0-9_.-]+):(?:\s|$)/;

  for (const [index, line] of text.split(/\r?\n/).entries()) {
    if (!line.trim() || line.trimStart().startsWith('#') || line.trimStart().startsWith('-')) continue;
    const match = line.match(keyPattern);
    if (!match) continue;

    const indent = match[1].length;
    const key = match[2];
    while (frames.length > 1 && indent <= frames[frames.length - 1].indent) {
      frames.pop();
    }

    const parent = frames[frames.length - 1];
    const full = parent.path ? `${parent.path}.${key}` : key;
    if (parent.seen.has(key)) {
      duplicates.push({ line: index + 1, key, parent: parent.path || '<root>' });
    }
    parent.seen.add(key);
    frames.push({ indent, path: full, seen: new Set() });
  }

  return duplicates;
}

function itemBlock(text, id) {
  const lines = text.split(/\r?\n/);
  const start = lines.findIndex(line => line === `${id}:`);
  if (start === -1) return '';

  const out = [];
  for (let i = start + 1; i < lines.length; i += 1) {
    if (/^[A-Z0-9_]+:\s*$/.test(lines[i])) break;
    out.push(lines[i]);
  }
  return out.join('\n');
}

function extractMenuScripts(text) {
  return [...text.matchAll(/-\s*(creator_[A-Za-z0-9_]+)/g)].map(match => match[1]);
}

function extractVariablesUsed(text) {
  const variableMechanics = [...text.matchAll(/variable="([A-Za-z0-9_]+)"/g)].map(match => match[1]);
  const placeholders = [...text.matchAll(/\{([A-Za-z0-9_]+)\}/g)]
    .map(match => match[1])
    .filter(name => name !== 'caster_name');
  return [...new Set([...variableMechanics, ...placeholders])];
}

function extractMiGives(text) {
  return [...text.matchAll(/mi give ([A-Z_]+) ([A-Z0-9_]+) \{caster_name\} 1/g)]
    .map(match => ({ type: match[1], id: match[2], source: 'starter-flow' }));
}

function extractMmoitemsRefs(text, source) {
  const refs = [];
  for (const match of text.matchAll(/mmoitems?\{type[=;]([A-Z_]+)[,;]id[=;]([A-Z0-9_]+)/g)) {
    refs.push({ type: match[1], id: match[2], source });
  }
  for (const match of text.matchAll(/mmoitem\{type=([A-Z_]+),id=([A-Z0-9_]+)/g)) {
    refs.push({ type: match[1], id: match[2], source });
  }
  return refs;
}

function walkFiles(dir) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...walkFiles(full));
    if (entry.isFile() && entry.name.endsWith('.yml')) out.push(full);
  }
  return out;
}

function recipeBlocks(text) {
  const recipes = [];
  const recipeSection = text.match(/^recipes:\n([\s\S]*?)(?=^[A-Za-z0-9_-]+:|\s*$)/m);
  if (!recipeSection) return recipes;

  for (const match of recipeSection[1].matchAll(/^  ([A-Za-z0-9_.-]+):\n([\s\S]*?)(?=^  [A-Za-z0-9_.-]+:|\s*$)/gm)) {
    recipes.push({ id: match[1], block: match[2] });
  }
  return recipes;
}

function fileHasTree(rel, tree) {
  return read(rel).includes(`- '${tree}'`);
}

function extractCatalogSkills(contractText) {
  const ids = new Set();
  for (const match of contractText.matchAll(/\| `([A-Z0-9_]+)` \|/g)) {
    ids.add(match[1]);
  }
  return ids;
}

function extractClassSkills(text) {
  const skills = new Set();
  const lines = text.split(/\r?\n/);
  let inSkills = false;
  let skillIndent = null;

  for (const line of lines) {
    if (/^skills:\s*$/.test(line)) {
      inSkills = true;
      skillIndent = null;
      continue;
    }
    if (!inSkills) continue;
    if (/^[A-Za-z0-9_-]+:\s*$/.test(line)) break;

    const match = line.match(/^(\s+)([A-Z0-9_]+):/);
    if (!match) continue;
    if (skillIndent === null) skillIndent = match[1].length;
    if (match[1].length === skillIndent) skills.add(match[2]);
  }
  return [...skills];
}

function extractHardsets(text) {
  return [...text.matchAll(/^\s*hardset:\s*([A-Z0-9_]+)/gm)].map(match => match[1]);
}

function permissionDefaultTrue(permissionsText, permission) {
  const lines = permissionsText.split(/\r?\n/);
  let inBlock = false;
  for (const line of lines) {
    if (line === `  ${permission}:`) {
      inBlock = true;
      continue;
    }
    if (inBlock && /^  [A-Za-z0-9_.-]+:/.test(line)) return false;
    if (inBlock && /^\s{4}default:\s*true\s*$/.test(line)) return true;
  }
  return false;
}

function activeRefs(loaded) {
  const refs = [
    ...extractMiGives(loaded.scripts),
    ...extractMmoitemsRefs(loaded.foundationForge, 'foundation-forge'),
  ];
  const level1Dir = path.join(ROOT, 'plugins/MythicMobs/Packs/level_1');
  for (const file of walkFiles(level1Dir)) {
    refs.push(...extractMmoitemsRefs(fs.readFileSync(file, 'utf8'), path.relative(ROOT, file)));
  }
  return refs;
}

function materialSources(loaded) {
  const sources = new Set();
  for (const text of [loaded.mmoItemsDrops, loaded.mmoItemTiers, loaded.foundationForge]) {
    for (const match of text.matchAll(/\b([A-Z0-9_]+):\s*\d/g)) sources.add(match[1]);
    for (const match of text.matchAll(/id=([A-Z0-9_]+)/g)) sources.add(match[1]);
  }
  const level1Dir = path.join(ROOT, 'plugins/MythicMobs/Packs/level_1');
  for (const file of walkFiles(level1Dir)) {
    const text = fs.readFileSync(file, 'utf8');
    for (const match of text.matchAll(/id=([A-Z0-9_]+)/g)) sources.add(match[1]);
  }
  return sources;
}

const loaded = Object.fromEntries(Object.entries(files).map(([key, rel]) => [key, exists(rel) ? read(rel) : '']));
const itemTexts = Object.fromEntries(Object.entries(typeFiles).map(([type, rel]) => [type, read(rel)]));
const checks = [];
const creatorMenuTexts = Object.entries(files)
  .filter(([key, rel]) => key.endsWith('Menu') && rel.includes('plugins/CoreTools/MenuCreator/'))
  .map(([key]) => ({ key, text: loaded[key] }));

const missingRequiredFiles = Object.entries(files).filter(([, rel]) => !exists(rel)).map(([, rel]) => rel);
checks.push(missingRequiredFiles.length
  ? fail('character-sector required files are missing', { missingRequiredFiles })
  : pass('character-sector required files exist', { count: Object.keys(files).length }));

const unsupportedMenuItemSources = creatorMenuTexts
  .filter(({ text }) => text.includes('source: mmoitems'))
  .map(({ key }) => key);
checks.push(unsupportedMenuItemSources.length
  ? fail('MenuCreator creator screens still use unsupported mmoitems item source', { unsupportedMenuItemSources })
  : pass('MenuCreator creator screens use supported item sources only'));

const declaredScripts = topLevelKeys(loaded.scripts);
const referencedScripts = [
  ...extractMenuScripts(loaded.creatorIntroMenu),
  ...extractMenuScripts(loaded.raceMenu),
  ...extractMenuScripts(loaded.raceSpotlightCzlowiekMenu),
  ...extractMenuScripts(loaded.raceSpotlightOrkMenu),
  ...extractMenuScripts(loaded.raceSpotlightElfMenu),
  ...extractMenuScripts(loaded.raceSpotlightKrasnoludMenu),
  ...extractMenuScripts(loaded.raceSpotlightNieumarlyMenu),
  ...extractMenuScripts(loaded.styleMenu),
  ...extractMenuScripts(loaded.classMenu),
  ...extractMenuScripts(loaded.classConfirmMenu),
  ...extractMenuScripts(loaded.classSpotlightWarriorMenu),
  ...extractMenuScripts(loaded.classSpotlightRogueMenu),
  ...extractMenuScripts(loaded.classSpotlightMarksmanMenu),
  ...extractMenuScripts(loaded.classSpotlightMageMenu),
  ...extractMenuScripts(loaded.classSpotlightPaladinMenu),
  ...extractMenuScripts(loaded.classConfirmWarriorMenu),
  ...extractMenuScripts(loaded.classConfirmRogueMenu),
  ...extractMenuScripts(loaded.classConfirmMarksmanMenu),
  ...extractMenuScripts(loaded.classConfirmMageMenu),
  ...extractMenuScripts(loaded.classConfirmPaladinMenu),
  ...extractMenuScripts(loaded.classMentorMenu),
  ...extractMenuScripts(loaded.cityRosterMenu),
  ...extractMenuScripts(loaded.skillRosterMenu),
  ...extractMenuScripts(loaded.professionRosterMenu),
  ...extractMenuScripts(loaded.firstContractMenu),
  ...extractMenuScripts(loaded.storageMenu),
];
const missingScripts = referencedScripts.filter(name => !declaredScripts.has(name));
checks.push(missingScripts.length
  ? fail('menu references missing CoreTools scripts', { missingScripts })
  : pass('menu script references resolve', { count: referencedScripts.length }));

const declaredVariables = topLevelKeys(loaded.variables);
const usedVariables = extractVariablesUsed(loaded.scripts);
const missingVariables = [...new Set([
  ...usedVariables.filter(name => !declaredVariables.has(name)),
  ...requiredCharacterVariables.filter(name => !declaredVariables.has(name)),
])];
checks.push(missingVariables.length
  ? fail('character flow uses undeclared variables', { missingVariables })
  : pass('character flow variables are declared', { count: usedVariables.length }));

const directSelectionMarkers = [
  'creator_open_class_selector',
  'creator_open_class_confirm_warrior',
  'creator_open_class_confirm_rogue',
  'creator_open_class_confirm_marksman',
  'creator_open_class_confirm_mage',
  'creator_open_class_confirm_paladin',
  'Nic nie wskazuje klasy za ciebie.',
];
const missingDirectSelectionMarkers = directSelectionMarkers.filter(marker =>
  !loaded.scripts.includes(marker)
  && !loaded.classMenu.includes(marker)
  && !loaded.classSpotlightWarriorMenu.includes(marker)
  && !loaded.classSpotlightRogueMenu.includes(marker)
  && !loaded.classSpotlightMarksmanMenu.includes(marker)
  && !loaded.classSpotlightMageMenu.includes(marker)
  && !loaded.classSpotlightPaladinMenu.includes(marker)
);
checks.push(missingDirectSelectionMarkers.length
  ? fail('direct class-selection flow is incomplete', { missingDirectSelectionMarkers })
  : pass('direct class-selection flow is wired'));

const cityRosterMarkers = [
  'foundation_city_roster',
  'foundation_class_mentor_roster',
  'foundation_skill_roster',
  'foundation_profession_roster',
  'foundation_first_contract',
  'Mentorzy Archetypów',
  'Kwatermistrz',
  'Trener Skilli',
  'Dzielnica Profesji',
  'mentor',
  'Zakaz: auto-grant',
];
const missingCityRosterMarkers = cityRosterMarkers.filter(marker =>
  !loaded.classMentorMenu.includes(marker) &&
  !loaded.cityRosterMenu.includes(marker) &&
  !loaded.skillRosterMenu.includes(marker) &&
  !loaded.professionRosterMenu.includes(marker) &&
  !loaded.firstContractMenu.includes(marker) &&
  !loaded.nexusMenu.includes(marker)
);
checks.push(missingCityRosterMarkers.length
  ? fail('city-acquired onboarding roster is incomplete', { missingCityRosterMarkers })
  : pass('city-acquired onboarding roster is wired'));

const creatorIntroMarkers = [
  'Komnata Początku',
  'creator_open_race_gallery',
  'creator_reset_selection',
];
const missingCreatorIntroMarkers = creatorIntroMarkers.filter(marker =>
  !loaded.creatorIntroMenu.includes(marker) &&
  !loaded.scripts.includes(marker)
);
checks.push(missingCreatorIntroMarkers.length
  ? fail('creator intro menu is incomplete', { missingCreatorIntroMarkers })
  : pass('creator intro menu exposes ceremonial creator entry'));

const raceSpotlightMenus = [
  loaded.raceSpotlightCzlowiekMenu,
  loaded.raceSpotlightOrkMenu,
  loaded.raceSpotlightElfMenu,
  loaded.raceSpotlightKrasnoludMenu,
  loaded.raceSpotlightNieumarlyMenu,
];
const raceSpotlightMarkers = ['Hero Card', 'Realny Payoff', 'Koszt Wyboru'];
const incompleteRaceSpotlights = raceSpotlightMenus
  .map((text, index) => ({ index, text }))
  .filter(({ text }) => raceSpotlightMarkers.some(marker => !text.includes(marker)))
  .map(({ index }) => index);
checks.push(incompleteRaceSpotlights.length
  ? fail('race spotlight screens are missing premium creator markers', { incompleteRaceSpotlights })
  : pass('race spotlight screens expose hero card, payoff, and choice cost'));

const classSpotlightMenus = [
  loaded.classSpotlightWarriorMenu,
  loaded.classSpotlightRogueMenu,
  loaded.classSpotlightMarksmanMenu,
  loaded.classSpotlightMageMenu,
  loaded.classSpotlightPaladinMenu,
];
const classSpotlightMarkers = ['Hero Card', 'Koszt Wyboru', 'Subclass Pull'];
const incompleteClassSpotlights = classSpotlightMenus
  .map((text, index) => ({ index, text }))
  .filter(({ text }) => classSpotlightMarkers.some(marker => !text.includes(marker)))
  .map(({ index }) => index);
checks.push(incompleteClassSpotlights.length
  ? fail('class spotlight screens are missing premium creator markers', { incompleteClassSpotlights })
  : pass('class spotlight screens expose hero card, subclass pull, and choice cost'));

const oathMarkers = ['Oath Screen', 'Payoff:', 'Gear nadal ma źródło w mieście.'];
const incompleteOathMenus = [
  loaded.classConfirmWarriorMenu,
  loaded.classConfirmRogueMenu,
  loaded.classConfirmMarksmanMenu,
  loaded.classConfirmMageMenu,
  loaded.classConfirmPaladinMenu,
].map((text, index) => ({ index, text }))
  .filter(({ text }) => oathMarkers.some(marker => !text.includes(marker)))
  .map(({ index }) => index);
checks.push(incompleteOathMenus.length
  ? fail('class oath screens are incomplete', { incompleteOathMenus })
  : pass('class oath screens expose payoff and non-auto-grant policy'));

const assetContractMarkers = [
  'CREATOR_RACE_CZLOWIEK',
  'CREATOR_CLASS_WARRIOR',
  'CREATOR_STATE_RECOMMENDED',
  'CMD',
];
const missingAssetContractMarkers = assetContractMarkers.filter(marker =>
  !loaded.creatorAssetContract.includes(marker) &&
  !loaded.creatorAssetManifest.includes(marker)
);
checks.push(missingAssetContractMarkers.length
  ? fail('creator asset contract is incomplete', { missingAssetContractMarkers })
  : pass('creator asset contract exposes race, class, and state tokens'));

const mentorRosterMarkers = [
  'Mentorzy Archetypów',
  'creator_mentor_warrior',
  'creator_mentor_rogue',
  'creator_mentor_marksman',
  'creator_mentor_mage',
  'creator_mentor_paladin',
  'creator_open_mmocore_class_panel',
  'creator_open_mmocore_skilltrees',
  'creator_open_class_selector',
  'creator_open_city_roster',
];
const missingMentorRosterMarkers = mentorRosterMarkers.filter(marker =>
  !loaded.classMentorMenu.includes(marker) &&
  !loaded.scripts.includes(marker)
);
checks.push(missingMentorRosterMarkers.length
  ? fail('class mentor roster is incomplete', { missingMentorRosterMarkers })
  : pass('class mentor roster exposes mentor previews and live bridges'));

const miGives = extractMiGives(loaded.scripts);
const missingItemTypes = miGives.filter(({ type }) => !itemTexts[type]);
const missingItems = miGives.filter(({ type, id }) => itemTexts[type] && !new RegExp(`^${id}:`, 'm').test(itemTexts[type]));
checks.push(missingItemTypes.length || missingItems.length
  ? fail('character flow gives missing MMOItems', { missingItemTypes, missingItems })
  : pass('character flow MMOItems exist', { count: miGives.length }));

const unsafeStarterItems = [];
for (const { type, id } of miGives) {
  if (!starterSafeTypes.has(type)) continue;
  const block = itemBlock(itemTexts[type], id);
  const levelMatch = block.match(/required-level:\s*([0-9.]+)/);
  const level = levelMatch ? Number(levelMatch[1]) : 0;
  const statReq = block.match(/required-(strength|dexterity|intelligence|vitality|faith):\s*([0-9.]+)/);
  if (level > 1 || statReq) {
    unsafeStarterItems.push({ type, id, requiredLevel: level, statRequirement: statReq ? statReq[0] : null });
  }
}
checks.push(unsafeStarterItems.length
  ? fail('starter loadout contains fresh-player gates', { unsafeStarterItems })
  : pass('starter loadout is fresh-player safe'));

function scriptBlock(text, name) {
  const start = text.indexOf(`${name}:`);
  if (start === -1) return '';
  const next = text.indexOf('\n\n', start);
  return text.slice(start, next === -1 ? text.length : next);
}

const finalizationScripts = [
  'creator_confirm_warrior',
  'creator_confirm_rogue',
  'creator_confirm_marksman',
  'creator_confirm_mage',
  'creator_confirm_paladin',
  'creator_finalize_race_dispatch',
  'creator_finalize_race_ork',
  'creator_finalize_race_elf',
  'creator_finalize_race_krasnolud',
  'creator_finalize_race_nieumarly',
  'creator_finalize_class_dispatch',
  'creator_finalize_class_rogue',
  'creator_finalize_class_marksman',
  'creator_finalize_class_mage',
  'creator_finalize_class_paladin',
  'creator_finalize_common',
];
const autoGrantLeaks = finalizationScripts
  .map(name => ({ name, block: scriptBlock(loaded.scripts, name) }))
  .filter(({ block }) => /mi give|minecraft:give/.test(block))
  .map(({ name }) => name);
checks.push(autoGrantLeaks.length
  ? fail('class/race finalization still auto-grants equipment or items', { autoGrantLeaks })
  : pass('class/race finalization does not auto-grant equipment'));

const plainClassCommands = [...loaded.scripts.matchAll(/command\{c="class \{caster_name\} (warrior|rogue|marksman|mage|paladin)"/g)]
  .map(match => match[0]);
checks.push(plainClassCommands.length
  ? fail('class finalization uses plain class command instead of MMOCore admin force-class', { plainClassCommands })
  : pass('class finalization uses MMOCore admin force-class path'));

const quartermasterBlocks = [
  'npc_quartermaster_claim_warrior',
  'npc_quartermaster_claim_rogue',
  'npc_quartermaster_claim_marksman',
  'npc_quartermaster_claim_mage',
  'npc_quartermaster_claim_paladin',
].map(name => ({ name, block: scriptBlock(loaded.scripts, name) }));
const missingQuartermasterClaims = quartermasterBlocks
  .filter(({ block }) => !/mi give/.test(block) || !/first_weapon_claimed/.test(`${block}\n${scriptBlock(loaded.scripts, 'npc_quartermaster_claim_common')}`))
  .map(({ name }) => name);
checks.push(missingQuartermasterClaims.length
  ? fail('quartermaster first-equipment claims are incomplete', { missingQuartermasterClaims })
  : pass('quartermaster owns first-equipment claims'));

const raceBonusScripts = [
  'npc_quartermaster_race_bonus_dispatch',
  'npc_quartermaster_race_bonus_ork',
  'npc_quartermaster_race_bonus_elf',
  'npc_quartermaster_race_bonus_krasnolud',
  'npc_quartermaster_race_bonus_nieumarly',
];
const missingRaceBonusScripts = raceBonusScripts.filter(name => !declaredScripts.has(name));
const missingRaceBonusDispatchHook = !scriptBlock(loaded.scripts, 'npc_quartermaster_claim_common')
  .includes('Script{script=npc_quartermaster_race_bonus_dispatch}');
const raceBonusMenuMarkers = [
  'mały bonus startowy',
  'Srebrne monety',
  'dzikie zioła',
  'żelaz',
  'esencj',
];
const raceBonusSearchSpace = [
  loaded.raceMenu,
  loaded.raceSpotlightCzlowiekMenu,
  loaded.raceSpotlightOrkMenu,
  loaded.raceSpotlightElfMenu,
  loaded.raceSpotlightKrasnoludMenu,
  loaded.raceSpotlightNieumarlyMenu,
  loaded.cityRosterMenu,
  loaded.firstContractMenu,
].join('\n').toLowerCase();
const missingRaceBonusMenuMarkers = raceBonusMenuMarkers.filter(marker =>
  !raceBonusSearchSpace.includes(marker.toLowerCase())
);
checks.push(missingRaceBonusScripts.length || missingRaceBonusDispatchHook || missingRaceBonusMenuMarkers.length
  ? fail('race-linked first-contract starter bonuses are incomplete', {
    missingRaceBonusScripts,
    missingRaceBonusDispatchHook,
    missingRaceBonusMenuMarkers,
  })
  : pass('race-linked first-contract starter bonuses are wired'));

const missingQuartermasterRefusals = [
  'npc_quartermaster_wrong_class',
  'npc_quartermaster_already_claimed',
].filter(name => !declaredScripts.has(name));
const claimBlocksWithoutRefusal = quartermasterBlocks
  .filter(({ block }) => !block.includes('falseElse npc_quartermaster_wrong_class') || !block.includes('falseElse npc_quartermaster_already_claimed'))
  .map(({ name }) => name);
const firstContractRefusalMarkers = ['Wrong class: refusal', 'Duplicate: refusal']
  .filter(marker => !loaded.firstContractMenu.includes(marker));
checks.push(missingQuartermasterRefusals.length || claimBlocksWithoutRefusal.length || firstContractRefusalMarkers.length
  ? fail('quartermaster claims are missing wrong-class/duplicate refusal UX', {
    missingQuartermasterRefusals,
    claimBlocksWithoutRefusal,
    firstContractRefusalMarkers,
  })
  : pass('quartermaster claims include wrong-class and duplicate refusal UX'));

const classFiles = walkFiles(path.join(ROOT, 'plugins/MMOCore/classes'));
const duplicateClassKeys = [];
for (const file of classFiles) {
  const duplicates = duplicateTopLevelKeys(fs.readFileSync(file, 'utf8'));
  if (duplicates.length) {
    duplicateClassKeys.push({ file: path.relative(ROOT, file), duplicates });
  }
}
checks.push(duplicateClassKeys.length
  ? fail('MMOCore class files contain duplicate top-level keys', { duplicateClassKeys })
  : pass('MMOCore class files have unique top-level keys', { count: classFiles.length }));

const duplicateYamlFiles = [];
for (const rel of [
  files.mmoItemsDrops,
]) {
  const duplicates = duplicateYamlKeys(read(rel));
  if (duplicates.length) duplicateYamlFiles.push({ file: rel, duplicates: duplicates.slice(0, 8) });
}
checks.push(duplicateYamlFiles.length
  ? fail('selected YAML configs contain duplicate keys', { duplicateYamlFiles })
  : pass('selected YAML configs have no duplicate keys'));

const placeholderSlots = classFiles
  .filter(file => /Skill Slot I|Skill Slot II|Skill Slot III|Skill Slot IV/.test(fs.readFileSync(file, 'utf8')))
  .map(file => path.relative(ROOT, file));
checks.push(placeholderSlots.length
  ? fail('MMOCore classes still contain default skill slot labels', { placeholderSlots })
  : pass('MMOCore class skill slots are project-specific', { count: classFiles.length }));

const missingBaseTrees = Object.entries(classTreeContracts)
  .filter(([rel, tree]) => !fileHasTree(rel, tree))
  .map(([rel, tree]) => ({ file: rel, tree }));
const missingFamilyTrees = [];
for (const { dir, tree } of familyTreeContracts) {
  for (const file of walkFiles(path.join(ROOT, dir))) {
    if (!fileHasTree(path.relative(ROOT, file), tree)) {
      missingFamilyTrees.push({ file: path.relative(ROOT, file), tree });
    }
  }
}
checks.push(missingBaseTrees.length || missingFamilyTrees.length
  ? fail('class files are missing family skill trees', { missingBaseTrees, missingFamilyTrees })
  : pass('class files use family skill trees'));

const catalogSkills = extractCatalogSkills(loaded.contract);
const classSkillRefs = [];
for (const file of classFiles) {
  const text = fs.readFileSync(file, 'utf8');
  for (const skill of [...extractClassSkills(text), ...extractHardsets(text)]) {
    classSkillRefs.push({ file: path.relative(ROOT, file), skill });
  }
}
const uncatalogedSkills = classSkillRefs.filter(({ skill }) => !catalogSkills.has(skill));
checks.push(uncatalogedSkills.length
  ? fail('class skill references are missing from character-sector skill catalog', { uncatalogedSkills })
  : pass('class skill references are cataloged', { count: classSkillRefs.length }));

const inventoryMarkers = ['Ekwipunek Postaci', 'Profil Walki', 'Druga Ręka'];
const missingInventoryMarkers = inventoryMarkers.filter(marker => !loaded.inventory.includes(marker));
checks.push(missingInventoryMarkers.length
  ? fail('MMOInventory is missing project-facing EQ markers', { missingInventoryMarkers })
  : pass('MMOInventory exposes project-facing EQ labels'));

const nexusMarkers = ['inventory_signal:', '[player] rpginv', "display_name: '&6Ekwipunek RPG'"];
const missingNexusMarkers = nexusMarkers.filter(marker => !loaded.nexusMenu.includes(marker));
checks.push(missingNexusMarkers.length
  ? fail('foundation nexus is missing direct EQ entry', { missingNexusMarkers })
  : pass('foundation nexus exposes direct EQ entry'));

const storageMarkers = [
  'foundation_storage',
  'creator_open_profile_vault',
  'creator_open_account_vault',
  'safe-zone',
  'combat-lock',
  'pending runtime hook',
];
const missingStorageMarkers = storageMarkers.filter(marker =>
  !loaded.storageMenu.includes(marker) &&
  !loaded.nexusMenu.includes(marker) &&
  !loaded.scripts.includes(marker)
);
checks.push(missingStorageMarkers.length
  ? fail('storage access is missing controlled hub/combat-lock contract markers', { missingStorageMarkers })
  : pass('storage access exposes controlled hub/combat-lock contract markers'));

const directPlayerVaultOpens = [...loaded.scripts.matchAll(/openvault\{name=([^}]+)\}/g)]
  .map(match => match[1]);
checks.push(directPlayerVaultOpens.length
  ? fail('storage player path opens vaults without safe-zone/combat-lock enforcement', { directPlayerVaultOpens })
  : pass('storage player path refuses vault opening until runtime enforcement exists'));

const classSpecificConfirmMenus = [
  loaded.classConfirmWarriorMenu,
  loaded.classConfirmRogueMenu,
  loaded.classConfirmMarksmanMenu,
  loaded.classConfirmMageMenu,
  loaded.classConfirmPaladinMenu,
];
const confirmMarkers = [
  'creator_confirm_warrior',
  'creator_confirm_rogue',
  'creator_confirm_marksman',
  'creator_confirm_mage',
  'creator_confirm_paladin',
  'creator_cancel_class_choice',
];
const confirmMenuText = classSpecificConfirmMenus.join('\n');
const missingConfirmMarkers = confirmMarkers.filter(marker => !confirmMenuText.includes(marker));
checks.push(missingConfirmMarkers.length
  ? fail('class confirmation menu is missing required confirmation markers', { missingConfirmMarkers })
  : pass('class confirmation menus expose explicit confirm/cancel flow'));

const missingContractMarkers = contractV2Markers.filter(marker => !loaded.contract.includes(marker));
const staleContractMarkers = [...loaded.contract.matchAll(/\bv0\.2\b|\bv0\.3\b/g)].map(match => match[0]);
checks.push(missingContractMarkers.length
  ? fail('character-sector contract is missing v0.4 system sections', { missingContractMarkers })
  : pass('character-sector contract exposes v0.4 system sections'));
checks.push(staleContractMarkers.length
  ? fail('character-sector contract still contains stale version markers', { staleContractMarkers })
  : pass('character-sector contract has no stale v0.2/v0.3 markers'));

const missingPermissions = requiredDefaultPermissions.filter(permission => !permissionDefaultTrue(loaded.permissions, permission));
checks.push(missingPermissions.length
  ? fail('default permissions do not cover foundation character-sector commands', { missingPermissions })
  : pass('default permissions cover foundation character-sector commands'));

const coreMenuOpen = /menu-creator:[\s\S]*?permission:\s*''/.test(loaded.coreCommands);
checks.push(coreMenuOpen
  ? pass('CoreTools menu command is default-player accessible')
  : fail('CoreTools menu command is still permission gated'));

const profilesManualOpen = /manual-gui-open:\s*\n\s*enabled:\s*true/.test(loaded.mmoProfilesConfig);
checks.push(profilesManualOpen
  ? pass('MMOProfiles manual GUI open is enabled for Nexus entry')
  : fail('MMOProfiles manual GUI open is disabled'));

const unsafeConfigText = [
  ['plugins/MMOCore/config.yml', loaded.mmoCoreConfig],
  ['plugins/MMOItems/config.yml', loaded.mmoItemsConfig],
  ['plugins/MMOInventory/config.yml', loaded.mmoInventoryConfig],
  ['plugins/MMOProfiles/config.yml', loaded.mmoProfilesConfig],
].filter(([, text]) => /ILove|username|password|mmolover/.test(text));
checks.push(unsafeConfigText.length
  ? fail('disabled database configs still contain sample credential text', { files: unsafeConfigText.map(([file]) => file) })
  : pass('disabled database configs do not expose sample credential text'));

checks.push(/item_commands:\s*\n[\s\S]*?enabled:\s*false/.test(loaded.mmoItemsConfig)
  ? pass('MMOItems item commands are disabled')
  : fail('MMOItems item commands are enabled'));

checks.push(/CUSTOM_MYLUCK/.test(loaded.customStats)
  ? fail('MMOItems custom-stats still contains the test CUSTOM_MYLUCK stat')
  : pass('MMOItems custom-stats has no test stat'));

const activeItemRefs = activeRefs(loaded);
const missingActiveRefs = activeItemRefs.filter(({ type, id }) => itemTexts[type] && !new RegExp(`^${id}:`, 'm').test(itemTexts[type]));
checks.push(missingActiveRefs.length
  ? fail('active player-path MMOItem references are missing', { missingActiveRefs })
  : pass('active player-path MMOItem references resolve', { count: activeItemRefs.length }));

const forbiddenActiveClasses = [];
for (const { type, id, source } of activeItemRefs) {
  if (!itemTexts[type]) continue;
  const block = itemBlock(itemTexts[type], id);
  if (!block) continue;
  const classBlock = block.match(/required-class:\s*\n((?:\s+-\s*.+\n?)+)/);
  if (!classBlock) continue;
  const classes = [...classBlock[1].matchAll(/-\s*([A-Za-z]+)/g)].map(match => match[1]);
  const bad = classes.filter(name => ['Wizard', 'Archer'].includes(name));
  if (bad.length) forbiddenActiveClasses.push({ type, id, source, bad });
}
checks.push(forbiddenActiveClasses.length
  ? fail('active player-path items use forbidden legacy class names', { forbiddenActiveClasses })
  : pass('active player-path items do not use forbidden legacy class names'));

const itemFiles = walkFiles(path.join(ROOT, 'plugins/MMOItems/item'));
const nonCanonicalClassGates = [];
for (const file of itemFiles) {
  const text = fs.readFileSync(file, 'utf8');
  for (const gate of text.matchAll(/required-class:\s*\n((?:\s+-\s*.+\n?)+)/g)) {
    const classes = [...gate[1].matchAll(/-\s*([A-Za-z0-9_-]+)/g)].map(match => match[1]);
    const bad = classes.filter(name => !canonicalClassIds.has(name));
    if (bad.length) nonCanonicalClassGates.push({ file: path.relative(ROOT, file), bad });
  }
}
checks.push(nonCanonicalClassGates.length
  ? fail('MMOItems required-class gates contain non-canonical class IDs', { nonCanonicalClassGates })
  : pass('MMOItems required-class gates use canonical class IDs', { count: itemFiles.length }));

const legacyStationLeaks = [];
for (const { key, file } of legacyStationContracts) {
  const text = loaded[key];
  if (!/\[LEGACY OFF\]/.test(text)) {
    legacyStationLeaks.push({ file, issue: 'missing LEGACY OFF marker' });
  }
  if (/permission:\s*'mmoitems\./.test(text)) {
    legacyStationLeaks.push({ file, issue: 'legacy command still uses mmoitems permission' });
  }
  for (const recipe of recipeBlocks(text)) {
    if (!recipe.block.includes('foundation.legacy.disabled')) {
      legacyStationLeaks.push({ file, recipe: recipe.id, issue: 'recipe lacks disabled permission gate' });
    }
  }
}
checks.push(legacyStationLeaks.length
  ? fail('legacy crafting stations are still player-usable', { legacyStationLeaks })
  : pass('legacy crafting stations are quarantined', { count: legacyStationContracts.length }));

const legacySetLeaks = [];
for (const id of legacySetIds) {
  const block = itemBlock(loaded.itemSets, id);
  if (!block) {
    legacySetLeaks.push({ id, issue: 'missing legacy set block' });
    continue;
  }
  if (!block.includes('[LEGACY OFF]')) legacySetLeaks.push({ id, issue: 'missing LEGACY OFF marker' });
  if (!/bonuses:\s*\{\s*\}/.test(block)) legacySetLeaks.push({ id, issue: 'set bonuses are not empty' });
}
checks.push(legacySetLeaks.length
  ? fail('legacy item sets still expose active bonuses', { legacySetLeaks })
  : pass('legacy item sets are disabled', { count: legacySetIds.length }));

const legacyItemLeaks = [];
for (const { type, id } of legacyItemContracts) {
  const text = itemTexts[type];
  const block = text ? itemBlock(text, id) : '';
  if (!block) {
    legacyItemLeaks.push({ type, id, issue: 'missing legacy item block' });
    continue;
  }
  if (!block.includes('[LEGACY OFF]')) legacyItemLeaks.push({ type, id, issue: 'missing LEGACY OFF marker' });
  if (!/required-level:\s*100\.0/.test(block)) legacyItemLeaks.push({ type, id, issue: 'missing level 100 quarantine' });
}
checks.push(legacyItemLeaks.length
  ? fail('legacy demo items are not fully quarantined', { legacyItemLeaks })
  : pass('legacy demo items are quarantined', { count: legacyItemContracts.length }));

const sourceableMaterials = materialSources(loaded);
const forgeIngredientMaterials = [...loaded.foundationForge.matchAll(/mmoitem\{type=MATERIAL,id=([A-Z0-9_]+)/g)]
  .map(match => match[1]);
const missingMaterialSources = [...new Set(forgeIngredientMaterials.filter(id => !sourceableMaterials.has(id)))];
checks.push(missingMaterialSources.length
  ? fail('foundation forge uses materials with no detected source contract', { missingMaterialSources })
  : pass('foundation forge material ingredients have detected source contracts', { count: new Set(forgeIngredientMaterials).size }));

const failures = checks.filter(check => !check.ok);
for (const check of checks) {
  console.log(`${check.ok ? 'PASS' : 'FAIL'} ${check.message}`);
  if (!check.ok) console.log(JSON.stringify(check.details, null, 2));
}

if (failures.length) {
  process.exitCode = 1;
}
