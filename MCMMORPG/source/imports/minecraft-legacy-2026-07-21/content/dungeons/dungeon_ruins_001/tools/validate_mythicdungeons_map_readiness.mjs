#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");
const projectRoot = path.resolve(root, "..", "..", "..");
const sourceMap = path.join(root, "mythicdungeons", "dungeon_ruins_001");
const stagingMap = path.join(projectRoot, "Server \u2014 kopia", "1.21.11", "plugins", "MythicDungeons", "maps", "dungeon_ruins_001");

const requiredConfigFiles = ["config.yml", "functions.yml"];
const requiredClassicMapEntries = ["data", "region", "level.dat", "uid.dat", "session.lock"];
const warnings = [];
const errors = [];

function exists(base, entry) {
  return fs.existsSync(path.join(base, entry));
}

function missing(base, entries) {
  return entries.filter((entry) => !exists(base, entry));
}

function read(file) {
  return fs.existsSync(file) ? fs.readFileSync(file, "utf8") : "";
}

for (const entry of requiredConfigFiles) {
  if (!exists(sourceMap, entry)) errors.push(`Source map config missing: ${entry}`);
}

const config = read(path.join(sourceMap, "config.yml"));
if (!/DungeonType:\s*CLASSIC/.test(config)) {
  errors.push("Source config is not DungeonType: CLASSIC.");
}
if (/EnableDifficultyLevels:\s*true/.test(config) || /EnableDifficultyMenu:\s*true/.test(config)) {
  warnings.push("Difficulty menu/levels remain enabled; local docs show full level schemas with Icon and scaling fields.");
}

const sourceMissingClassic = missing(sourceMap, requiredClassicMapEntries);
const stagingMissingClassic = missing(stagingMap, requiredClassicMapEntries);
const status = errors.length > 0
  ? "FAIL"
  : stagingMissingClassic.length > 0
    ? "CONFIG_ONLY_NOT_LOAD_READY"
    : "LOAD_SAFE_READY";

const result = {
  status,
  sourceMap: path.relative(projectRoot, sourceMap).replaceAll(path.sep, "/"),
  stagingMap: path.relative(projectRoot, stagingMap).replaceAll(path.sep, "/"),
  requiredClassicMapEntries,
  sourceMissingClassicMapEntries: sourceMissingClassic,
  stagingMissingClassicMapEntries: stagingMissingClassic,
  blocker: stagingMissingClassic.length > 0 ? "BLOCKED_BY_MISSING_CLASSIC_MAP_FILES" : null,
  errors,
  warnings
};

console.log(JSON.stringify(result, null, 2));
process.exit(errors.length === 0 ? 0 : 1);
