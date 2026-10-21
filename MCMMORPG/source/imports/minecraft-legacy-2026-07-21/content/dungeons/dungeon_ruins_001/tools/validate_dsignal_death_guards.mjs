import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const dungeonRoot = path.join(root, "content", "dungeons", "dungeon_ruins_001");
const bossesPath = path.join(dungeonRoot, "mythicmobs", "bosses.yml");
const skillsPath = path.join(dungeonRoot, "mythicmobs", "skills.yml");

const bossesText = fs.readFileSync(bossesPath, "utf8");
const skillsText = fs.readFileSync(skillsPath, "utf8");

function parseTopLevelBlocks(text) {
  const blocks = new Map();
  let current = null;
  for (const line of text.split(/\r?\n/)) {
    const match = line.match(/^([A-Za-z0-9_]+):\s*$/);
    if (match) {
      current = { name: match[1], lines: [] };
      blocks.set(current.name, current);
      continue;
    }
    if (current) current.lines.push(line);
  }
  return blocks;
}

function skillRefs(lines) {
  const refs = [];
  for (const line of lines) {
    const match = line.match(/\bskill\{[^}]*\bs=([A-Za-z0-9_]+)[^}]*\}/);
    if (match) refs.push(match[1]);
  }
  return refs;
}

function hasDungeonSignal(lines) {
  return lines.some((line) => /\bdsignal\b|DungeonSignal/i.test(line));
}

const skills = parseTopLevelBlocks(skillsText);
const errors = [];
const warnings = [];
const deathHooks = [];

for (const [bossId, block] of parseTopLevelBlocks(bossesText)) {
  for (const line of block.lines) {
    if (!line.includes("~onDeath")) continue;
    deathHooks.push({ bossId, line: line.trim() });
    if (/\bdsignal\b|DungeonSignal/i.test(line)) {
      errors.push(`${bossId} has a direct dungeon signal on ~onDeath: ${line.trim()}`);
    }
    const refs = skillRefs([line]);
    if (refs.length === 0) {
      warnings.push(`${bossId} has a ~onDeath hook without a skill wrapper: ${line.trim()}`);
    }
    for (const ref of refs) {
      const stack = [ref];
      const seen = new Set();
      while (stack.length) {
        const name = stack.pop();
        if (seen.has(name)) continue;
        seen.add(name);
        const skill = skills.get(name);
        if (!skill) {
          errors.push(`${bossId} ~onDeath references missing skill ${name}`);
          continue;
        }
        if (hasDungeonSignal(skill.lines)) {
          errors.push(`${bossId} ~onDeath reaches dungeon signal skill ${name}; use a standalone-safe wrapper for smoke.`);
        }
        for (const nested of skillRefs(skill.lines)) stack.push(nested);
      }
    }
  }
}

if (deathHooks.length !== 3) {
  errors.push(`Expected 3 DR001 boss ~onDeath hooks, found ${deathHooks.length}.`);
}

const dungeonOnlySignalSkills = [...skills.values()].filter((skill) => {
  return skill.name.startsWith("DR001_DungeonOnly_") && hasDungeonSignal(skill.lines);
});

if (dungeonOnlySignalSkills.length < 3) {
  errors.push("Expected dungeon-only dsignal skills for all three DR001 boss deaths.");
}

const result = {
  status: errors.length ? "FAIL" : "PASS",
  deathHooks,
  dungeonOnlySignalSkills: dungeonOnlySignalSkills.map((skill) => skill.name),
  errors,
  warnings,
};

console.log(JSON.stringify(result, null, 2));
if (errors.length) process.exit(1);
