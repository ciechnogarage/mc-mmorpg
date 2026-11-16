#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");
const projectRoot = path.resolve(root, "..", "..", "..");

const files = [
  path.join(root, "mythicmobs", "mobs.yml"),
  path.join(root, "mythicmobs", "bosses.yml"),
  path.join(root, "mythicmobs", "skills.yml"),
  path.join(projectRoot, "Server \u2014 kopia", "1.21.11", "plugins", "MythicMobs", "mobs", "DR001_Mobs.yml"),
  path.join(projectRoot, "Server \u2014 kopia", "1.21.11", "plugins", "MythicMobs", "mobs", "DR001_Bosses.yml"),
  path.join(projectRoot, "Server \u2014 kopia", "1.21.11", "plugins", "MythicMobs", "skills", "DR001_Skills.yml")
];

const errors = [];

function rel(file) {
  return path.relative(projectRoot, file).replaceAll(path.sep, "/");
}

for (const file of files) {
  if (!fs.existsSync(file)) {
    errors.push(`Missing MythicMobs config: ${rel(file)}`);
    continue;
  }
  const lines = fs.readFileSync(file, "utf8").split(/\r?\n/);
  for (const [index, line] of lines.entries()) {
    if (line.includes("RandomPlayersInRadius")) {
      errors.push(`${rel(file)}:${index + 1}: unsupported targeter RandomPlayersInRadius`);
    }
    for (const match of line.matchAll(/@?MobsInRadius\{([^}]*)\}/g)) {
      const attrs = match[1];
      if (!/(^|;)t(ype|ypes)?=/.test(attrs)) {
        errors.push(`${rel(file)}:${index + 1}: MobsInRadius missing t/type/types attribute`);
      }
    }
  }
}

const result = {
  status: errors.length === 0 ? "PASS" : "FAIL",
  checkedFiles: files.map(rel),
  errors
};

console.log(JSON.stringify(result, null, 2));
process.exit(errors.length === 0 ? 0 : 1);
