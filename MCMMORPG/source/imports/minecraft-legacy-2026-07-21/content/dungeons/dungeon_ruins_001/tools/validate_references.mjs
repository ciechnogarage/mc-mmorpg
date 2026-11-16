#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");

const files = {
  mobs: path.join(root, "mythicmobs", "mobs.yml"),
  bosses: path.join(root, "mythicmobs", "bosses.yml"),
  skills: path.join(root, "mythicmobs", "skills.yml"),
  drops: path.join(root, "mythicmobs", "drops.yml"),
  materials: path.join(root, "mmoitems", "materials.yml"),
  weapons: path.join(root, "mmoitems", "weapons.yml"),
  armor: path.join(root, "mmoitems", "armor.yml"),
  accessories: path.join(root, "mmoitems", "accessories.yml"),
  consumables: path.join(root, "mmoitems", "consumables.yml"),
  nexoBlocks: path.join(root, "nexo", "blocks.yml"),
  nexoFurniture: path.join(root, "nexo", "furniture.yml"),
  nexoItems: path.join(root, "nexo", "items.yml"),
  start: path.join(root, "mythicdungeons", "stages", "start.yml"),
  mid: path.join(root, "mythicdungeons", "stages", "mid.yml"),
  end: path.join(root, "mythicdungeons", "stages", "end.yml"),
  importAssets: path.join(root, "tools", "import_assets_to_staging.ps1")
};

const errors = [];
const warnings = [];

function read(file) {
  if (!fs.existsSync(file)) {
    errors.push(`Missing file: ${path.relative(root, file)}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
}

function idsFromTopLevel(text, prefix) {
  const ids = new Set();
  for (const line of text.split(/\r?\n/)) {
    const match = line.match(new RegExp(`^(${prefix}[A-Za-z0-9_]+):\\s*$`));
    if (match) ids.add(match[1]);
  }
  return ids;
}

const mythicText = read(files.mobs) + "\n" + read(files.bosses);
const skillsText = read(files.skills);
const dropsText = read(files.drops);
const itemsText = [files.materials, files.weapons, files.armor, files.accessories, files.consumables].map(read).join("\n");
const nexoText = [files.nexoBlocks, files.nexoFurniture, files.nexoItems].map(read).join("\n");
const stagesText = [files.start, files.mid, files.end].map(read).join("\n");
const importAssetsText = read(files.importAssets);
const allText = [mythicText, skillsText, dropsText, itemsText, nexoText, stagesText].join("\n");

const mobIds = idsFromTopLevel(mythicText, "DR001_");
const skillIds = idsFromTopLevel(skillsText, "DR001_");
const dropIds = idsFromTopLevel(dropsText, "DR001_");
const itemIds = idsFromTopLevel(itemsText, "DR001_");
const nexoIds = idsFromTopLevel(nexoText, "dr001_");

function requireSet(set, id, kind) {
  if (!set.has(id)) errors.push(`Missing ${kind}: ${id}`);
}

for (const match of mythicText.matchAll(/skill\{s=([A-Za-z0-9_]+)/g)) requireSet(skillIds, match[1], "skill referenced by MythicMobs");
for (const match of mythicText.matchAll(/-\s*(DR001_[A-Za-z0-9_]+)\s*$/gm)) {
  if (match[1].includes("Drops")) requireSet(dropIds, match[1], "drop table referenced by MythicMobs");
}
for (const match of dropsText.matchAll(/id=(DR001_[A-Za-z0-9_]+)/g)) requireSet(itemIds, match[1], "MMOItems item referenced by drops");
for (const match of stagesText.matchAll(/target_mob:\s*(DR001_[A-Za-z0-9_]+)/g)) requireSet(mobIds, match[1], "stage target mob");
for (const match of stagesText.matchAll(/-\s*(DR001_[A-Za-z0-9_]+)\s*$/gm)) {
  if (!match[1].includes("Drops")) requireSet(mobIds, match[1], "stage mob reference");
}
for (const match of stagesText.matchAll(/target_nexo_id:\s*(dr001_[A-Za-z0-9_]+)/g)) requireSet(nexoIds, match[1], "stage Nexo reference");

const activeLegacyMatches = allText.match(/\bdungeon_ruins_001_[A-Za-z0-9_]+/g) || [];
if (activeLegacyMatches.length > 0) {
  const uniqueLegacyMatches = [...new Set(activeLegacyMatches)].sort();
  errors.push(`Active DR001 contract contains legacy dungeon_ruins_001_* token(s): ${uniqueLegacyMatches.join(", ")}`);
}

const suspiciousHpShorthand = [];
for (const [index, line] of mythicText.split(/\r?\n/).entries()) {
  if (/@self\s+<\d+%/.test(line) && !line.includes("RUNTIME_VERIFY_MYTHICMOBS_HP_CONDITION")) {
    suspiciousHpShorthand.push(`${index + 1}: ${line.trim()}`);
  }
}
if (suspiciousHpShorthand.length > 0) {
  errors.push(`Unverified MythicMobs inline HP shorthand remains: ${suspiciousHpShorthand.join(" | ")}`);
}

const importSafetyChecks = [
  ["AllowLegacyScaffoldApply switch", /\[switch\]\$AllowLegacyScaffoldApply/],
  ["legacy apply fail-fast condition", /\$Apply\s+-and\s+-not\s+\$UseFinalIntake\s+-and\s+-not\s+\$AllowLegacyScaffoldApply/],
  ["DR001 runtime contract warning", /Legacy scaffold is not the DR001 runtime contract/],
  ["final intake guidance", /Use -UseFinalIntake for final DR001 intake/],
  ["explicit legacy override guidance", /Use -AllowLegacyScaffoldApply only for intentional legacy scaffold testing/]
];
for (const [label, pattern] of importSafetyChecks) {
  if (!pattern.test(importAssetsText)) {
    errors.push(`Import safety guard missing: ${label}`);
  }
}

const duplicateSource = [...mobIds, ...skillIds, ...dropIds, ...itemIds, ...nexoIds];
const seen = new Set();
for (const id of duplicateSource) {
  if (seen.has(id)) errors.push(`Duplicate active ID: ${id}`);
  seen.add(id);
}

const result = {
  status: errors.length === 0 ? "PASS" : "FAIL",
  counts: {
    mobs: mobIds.size,
    skills: skillIds.size,
    drops: dropIds.size,
    mmoitems: itemIds.size,
    nexo: nexoIds.size
  },
  errors,
  warnings
};

console.log(JSON.stringify(result, null, 2));
process.exit(errors.length === 0 ? 0 : 1);
