const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

function read(rel) {
  return fs.readFileSync(path.join(ROOT, rel), 'utf8');
}

function fail(message, details = {}) {
  return { ok: false, message, details };
}

function pass(message, details = {}) {
  return { ok: true, message, details };
}

function tableIds(sectionText) {
  return [...sectionText.matchAll(/\| `([A-Z0-9_]+)` \|/g)].map(match => match[1]);
}

function section(text, title) {
  const lines = text.split(/\r?\n/);
  const start = lines.findIndex(line => line === `## ${title}`);
  if (start === -1) return '';
  const out = [];
  for (let i = start + 1; i < lines.length; i += 1) {
    if (lines[i].startsWith('## ')) break;
    out.push(lines[i]);
  }
  return out.join('\n');
}

function classBlock(text, title) {
  const lines = text.split(/\r?\n/);
  const start = lines.findIndex(line => line === `${title}:`);
  if (start === -1) return '';
  const out = [];
  for (let i = start + 1; i < lines.length; i += 1) {
    if (/^[A-Za-z0-9_-]+:\s*$/.test(lines[i])) break;
    out.push(lines[i]);
  }
  return out.join('\n');
}

function attributeBase(text, attribute) {
  const block = classBlock(text, 'attributes');
  const match = block.match(new RegExp(`^    ${attribute}:\\n\\s+base:\\s*([0-9.-]+)`, 'm'));
  return match ? Number(match[1]) : null;
}

function subclassLevels(text) {
  const block = classBlock(text, 'subclasses');
  return [...block.matchAll(/^    [A-Z0-9_]+:\s*([0-9]+)/gm)].map(match => Number(match[1]));
}

function hardsetCount(text) {
  return (text.match(/hardset:\s*[A-Z0-9_]+/g) || []).length;
}

const contract = read('docs/character-sector-contract.md');
const scripts = read('plugins/CoreTools/Scripts/foundation_character_flow.yml');
const variables = read('plugins/CoreTools/Variables/foundation_character.yml');
const raceMenu = read('plugins/CoreTools/MenuCreator/foundation_race_selector.yml');
const classMenu = read('plugins/CoreTools/MenuCreator/foundation_class_selector.yml');
const styleMenu = read('plugins/CoreTools/MenuCreator/foundation_style_test.yml');
const inventory = read('plugins/MMOInventory/inventory/default_mmoinventory.yml');
const storageMenu = read('plugins/CoreTools/MenuCreator/foundation_storage.yml');
const cityRosterMenu = read('plugins/CoreTools/MenuCreator/foundation_city_roster.yml');
const skillRosterMenu = read('plugins/CoreTools/MenuCreator/foundation_skill_roster.yml');
const professionRosterMenu = read('plugins/CoreTools/MenuCreator/foundation_profession_roster.yml');
const firstContractMenu = read('plugins/CoreTools/MenuCreator/foundation_first_contract.yml');
const e2eHarness = read('_validation/character_sector_e2e.js');
const classFiles = {
  warrior: read('plugins/MMOCore/classes/warrior.yml'),
  rogue: read('plugins/MMOCore/classes/rogue.yml'),
  marksman: read('plugins/MMOCore/classes/marksman.yml'),
  mage: read('plugins/MMOCore/classes/mage/mage.yml'),
  paladin: read('plugins/MMOCore/classes/paladin.yml'),
};
const checks = [];

const raceMatrix = section(contract, 'Race Passive Matrix');
const raceIds = tableIds(raceMatrix);
const missingRaceIds = ['CZLOWIEK', 'ORK', 'ELF', 'KRASNOLUD', 'NIEUMARLY']
  .filter(id => !raceIds.includes(id));
checks.push(missingRaceIds.length
  ? fail('race passive matrix misses race IDs', { missingRaceIds })
  : pass('race passive matrix covers all race IDs'));

checks.push(raceMenu.includes('Runtime hook: pending')
  ? pass('race selector does not overclaim passive runtime implementation')
  : fail('race selector overclaims passive runtime implementation'));

checks.push(/\bv0\.1\b/.test(contract)
  ? fail('character-sector contract still contains stale v0.1 wording')
  : pass('character-sector contract has no stale v0.1 wording'));

const staleVersionMarkers = [...contract.matchAll(/\bv0\.2\b|\bv0\.3\b/g)].map(match => match[0]);
checks.push(staleVersionMarkers.length
  ? fail('character-sector contract still contains stale v0.2/v0.3 wording', { staleVersionMarkers })
  : pass('character-sector contract has no stale v0.2/v0.3 wording'));

checks.push(contract.includes('City-Acquired First Equipment') && !contract.includes('starter gear ->')
  ? pass('contract replaces auto starter gear with city-acquired equipment')
  : fail('contract still permits auto starter gear flow'));

checks.push(contract.includes('## Class Selection Rules')
  ? pass('contract documents direct class-selection rules')
  : fail('contract is missing direct class-selection rules'));

const classPromises = [
  ['Wojownik', 'Słabość: dystans'],
  ['Łotrzyk', 'Słabość: błąd'],
  ['Łowca', 'Słabość: presja'],
  ['Mag', 'Słabość: przerwanie'],
  ['Akolita', 'Słabość: wolniejsze'],
];
const missingClassPromises = classPromises
  .filter(([name, weakness]) => !classMenu.includes(name) || !classMenu.includes(weakness))
  .map(([name]) => name);
checks.push(missingClassPromises.length
  ? fail('class menu is missing explicit power/weakness promises', { missingClassPromises })
  : pass('class menu exposes explicit power/weakness promises'));

const selectionFlowMarkers = [
  'creator_open_class_selector',
  'creator_open_class_confirm_warrior',
  'creator_open_class_confirm_rogue',
  'creator_open_class_confirm_marksman',
  'creator_open_class_confirm_mage',
  'creator_open_class_confirm_paladin',
];
const missingSelectionFlowMarkers = selectionFlowMarkers.filter(marker => !scripts.includes(marker));
checks.push(missingSelectionFlowMarkers.length
  ? fail('direct class-selection scripts are incomplete', { missingSelectionFlowMarkers })
  : pass('direct class-selection scripts are wired'));

const mastery = section(contract, 'Mastery Gates');
const missingMasterySignals = ['Level gate', 'Weapon use', 'Skill use', 'Survival pressure', 'Class trial']
  .filter(marker => !mastery.includes(marker));
checks.push(missingMasterySignals.length
  ? fail('mastery gate contract misses required signals', { missingMasterySignals })
  : pass('mastery gate contract covers required signals'));

const storage = section(contract, 'Storage Access Rules');
const missingStorageRules = ['safe-zone', 'combat', 'Profile vault', 'account vault', 'refuse opening']
  .filter(marker => !storage.toLowerCase().includes(marker.toLowerCase()));
checks.push(missingStorageRules.length
  ? fail('storage rules miss safety boundaries', { missingStorageRules })
  : pass('storage rules define safety boundaries'));

const storageOverclaims = [
  scripts.includes('openvault{') ? 'direct openvault call in character flow' : '',
  !storageMenu.includes('pending runtime hook') ? 'storage menu missing pending hook status' : '',
  !storageMenu.includes('zawsze odmowa') ? 'storage menu does not disclose refusal state' : '',
].filter(Boolean);
checks.push(storageOverclaims.length
  ? fail('storage UI/scripts overclaim runtime enforcement', { storageOverclaims })
  : pass('storage UI/scripts refuse vault use until runtime enforcement exists'));

const e2eOverclaims = [
  /status:\s*'PASS'[\s\S]*character-sector-player-path/.test(e2eHarness) ? 'player-path milestone can still report PASS' : '',
  !e2eHarness.includes('INSUFFICIENT_EVIDENCE') ? 'missing INSUFFICIENT_EVIDENCE verdict for partial player path' : '',
  !e2eHarness.includes('missingPlayerProof') ? 'missing explicit list of unproven player-path seams' : '',
].filter(Boolean);
checks.push(e2eOverclaims.length
  ? fail('character-sector E2E harness can overclaim full player proof', { e2eOverclaims })
  : pass('character-sector E2E harness reports partial proof honestly'));

const e2eClassPathMissing = ['warrior', 'rogue', 'marksman', 'mage', 'paladin']
  .filter(id => !e2eHarness.includes(`id: '${id}'`));
const e2eSingleClassSmell = !e2eHarness.includes('CLASS_PATHS') || !e2eHarness.includes('MC_QA_CLASS');
checks.push(e2eClassPathMissing.length || e2eSingleClassSmell
  ? fail('character-sector E2E harness is still single-class-only', { e2eClassPathMissing, e2eSingleClassSmell })
  : pass('character-sector E2E harness defines all five archetype paths'));

const rosterMarkers = [
  ['city roster', cityRosterMenu, ['Mentorzy Klas', 'Trener Skilli', 'Kwatermistrz', 'Dzielnica Profesji']],
  ['skill roster', skillRosterMenu, ['Skille Klasowe', 'Generic: Guard', 'Skille Profesji', 'Slot: generic']],
  ['profession roster', professionRosterMenu, ['Gathering', 'Rzemiosło', 'Skille Profesji', 'Rynek i Item Sink']],
  ['first contract', firstContractMenu, ['Pierwszy Ekwipunek', 'Lock: class=warrior', 'Lock: class=mage', 'auto-grant']],
];
const missingRosterMarkers = rosterMarkers.flatMap(([menu, text, markers]) =>
  markers.filter(marker => !text.includes(marker)).map(marker => ({ menu, marker }))
);
checks.push(missingRosterMarkers.length
  ? fail('city/skill/profession roster menus miss MMO-quality lock/source markers', { missingRosterMarkers })
  : pass('city/skill/profession roster menus expose lock/source markers'));

const missingHonestyMarkers = [
  ['skill roster', skillRosterMenu, 'selection contract'],
  ['skill roster', skillRosterMenu, 'Runtime hook: pending'],
  ['skill roster', skillRosterMenu, 'Runtime equip: pending'],
  ['profession roster', professionRosterMenu, 'intro + source contract'],
  ['profession roster', professionRosterMenu, 'Runtime XP: pending'],
  ['profession roster', professionRosterMenu, 'Runtime unlock: pending'],
].filter(([, text, marker]) => !text.includes(marker))
  .map(([menu, , marker]) => ({ menu, marker }));
checks.push(missingHonestyMarkers.length
  ? fail('skill/profession roster menus overclaim runtime implementation', { missingHonestyMarkers })
  : pass('skill/profession roster menus disclose pending runtime hooks'));

const finalizationText = [
  section(scripts, 'creator_finalize_race_dispatch'),
  section(scripts, 'creator_finalize_class_dispatch'),
  section(scripts, 'creator_finalize_common'),
].join('\n');
checks.push(/mi give|minecraft:give/.test(finalizationText)
  ? fail('finalization scripts still grant items directly')
  : pass('finalization scripts only record identity and route to city onboarding'));

const classBalanceContracts = [
  { id: 'warrior', attr: 'max-health', min: 22, role: 'frontline health floor' },
  { id: 'rogue', attr: 'max-health', max: 20, role: 'burst fragility ceiling' },
  { id: 'marksman', attr: 'knockback-resistance', min: 0.1, role: 'range control marker' },
  { id: 'mage', attr: 'max-mana', min: 25, role: 'caster resource identity' },
  { id: 'paladin', attr: 'attack-speed', max: 3.8, role: 'sustain kill-speed cost' },
];
const brokenClassBalance = classBalanceContracts.filter(({ id, attr, min, max }) => {
  const value = attributeBase(classFiles[id], attr);
  return value == null || (min != null && value < min) || (max != null && value > max);
}).map(({ id, attr, min, max, role }) => ({
  class: id,
  attribute: attr,
  expected: min != null ? `>= ${min}` : `<= ${max}`,
  actual: attributeBase(classFiles[id], attr),
  role,
}));
checks.push(brokenClassBalance.length
  ? fail('class attribute profiles do not preserve role identity', { brokenClassBalance })
  : pass('class attribute profiles preserve role identity'));

const brokenSkillSlotContracts = Object.entries(classFiles)
  .filter(([, text]) => hardsetCount(text) < 4)
  .map(([id, text]) => ({ class: id, hardsetCount: hardsetCount(text) }));
checks.push(brokenSkillSlotContracts.length
  ? fail('classes are missing four starter hardset skill slots', { brokenSkillSlotContracts })
  : pass('classes expose four starter hardset skill slots'));

const brokenSubclassGates = Object.entries(classFiles)
  .flatMap(([id, text]) => subclassLevels(text)
    .filter(level => level < 25)
    .map(level => ({ class: id, level })));
checks.push(brokenSubclassGates.length
  ? fail('subclass gates unlock before foundation mastery threshold', { brokenSubclassGates })
  : pass('subclass gates respect level 25 mastery threshold'));

const inventorySlotContracts = ['Slot Pleców', 'mobilności lub kosmetyki', 'Druga Ręka'];
const missingInventoryContracts = inventorySlotContracts.filter(marker => !inventory.includes(marker));
checks.push(missingInventoryContracts.length
  ? fail('inventory slot semantics are missing expected contracts', { missingInventoryContracts })
  : pass('inventory slot semantics expose expected contracts'));

for (const check of checks) {
  console.log(`${check.ok ? 'PASS' : 'FAIL'} ${check.message}`);
  if (!check.ok) console.log(JSON.stringify(check.details, null, 2));
}

if (checks.some(check => !check.ok)) {
  process.exitCode = 1;
}
