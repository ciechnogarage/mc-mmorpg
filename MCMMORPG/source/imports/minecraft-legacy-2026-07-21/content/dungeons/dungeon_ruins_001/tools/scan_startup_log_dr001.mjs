#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");
const projectRoot = path.resolve(root, "..", "..", "..");
const defaultLog = path.join(projectRoot, "Server \u2014 kopia", "1.21.11", "logs", "latest.log");
const logPath = process.argv[2] ? path.resolve(process.argv[2]) : defaultLog;

const patterns = [
  /MythicMobs.*Config Error.*(?:DR001|MobsInRadius|RandomPlayersInRadius)/i,
  /MythicDungeons.*ERROR.*dungeon_ruins_001/i,
  /dungeon 'dungeon_ruins_001' was not loaded/i,
  /RandomPlayersInRadius/i,
  /MobsInRadius\{r=10\}.*type.*valid MythicMob/i
];

const errors = [];
const matches = [];

if (!fs.existsSync(logPath)) {
  errors.push(`Startup log not found: ${logPath}`);
} else {
  const lines = fs.readFileSync(logPath, "utf8").split(/\r?\n/);
  for (const [index, line] of lines.entries()) {
    if (patterns.some((pattern) => pattern.test(line))) {
      matches.push({ lineNumber: index + 1, line: line.replace(/\u00a7./g, "") });
    }
  }
}

const result = {
  status: errors.length > 0 ? "FAIL" : matches.length > 0 ? "STARTUP_ERRORS_FOUND" : "PASS",
  logPath: path.relative(projectRoot, logPath).replaceAll(path.sep, "/"),
  matches,
  errors
};

console.log(JSON.stringify(result, null, 2));
process.exit(errors.length === 0 ? 0 : 1);
