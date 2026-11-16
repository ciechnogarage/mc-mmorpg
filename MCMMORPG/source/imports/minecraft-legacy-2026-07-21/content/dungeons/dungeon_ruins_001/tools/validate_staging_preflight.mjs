#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");
const projectRoot = path.resolve(root, "..", "..", "..");
const manifestPath = path.join(root, "staging", "DR001_CONFIG_ONLY_SYNC_MANIFEST.json");

const binaryExtensions = new Set([".bbmodel", ".png", ".ogg", ".schem", ".schematic", ".zip"]);
const recognizedTargets = [
  /^plugins[\\/]MythicMobs[\\/](mobs|skills|droptables)[\\/][A-Za-z0-9_]+\.ya?ml$/,
  /^plugins[\\/]MythicDungeons[\\/]maps[\\/]dungeon_ruins_001[\\/](config|functions)\.yml$/
];

const errors = [];
const warnings = [];
const notInstalled = [];

function readText(file) {
  if (!fs.existsSync(file)) {
    errors.push(`Missing file: ${path.relative(root, file)}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
}

function repoPath(relativePath) {
  return path.resolve(projectRoot, relativePath.replaceAll("/", path.sep));
}

function relativeToTargetRoot(targetRoot, targetPathText) {
  const normalizedRoot = targetRoot.replaceAll("/", path.sep);
  const normalizedTarget = targetPathText.replaceAll("/", path.sep);
  if (!normalizedTarget.startsWith(normalizedRoot + path.sep)) return null;
  return normalizedTarget.slice(normalizedRoot.length + 1);
}

function idsFromTopLevel(text, prefix) {
  const ids = [];
  for (const line of text.split(/\r?\n/)) {
    const match = line.match(new RegExp(`^(${prefix}[A-Za-z0-9_]+):\\s*$`));
    if (match) ids.push(match[1]);
  }
  return ids;
}

let manifest = null;
if (!fs.existsSync(manifestPath)) {
  errors.push("Config-only sync manifest is missing.");
} else {
  try {
    manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
  } catch (error) {
    errors.push(`Config-only sync manifest is not parseable JSON: ${error.message}`);
  }
}

const mythicMobIds = [
  ...idsFromTopLevel(readText(path.join(root, "mythicmobs", "mobs.yml")), "DR001_"),
  ...idsFromTopLevel(readText(path.join(root, "mythicmobs", "bosses.yml")), "DR001_")
];
const mythicSkillIds = idsFromTopLevel(readText(path.join(root, "mythicmobs", "skills.yml")), "DR001_");

let assetGate = "UNKNOWN";
const intakeManifestPath = path.join(root, "intake", "manifest.yml");
const intakeText = fs.existsSync(intakeManifestPath) ? fs.readFileSync(intakeManifestPath, "utf8") : "";
if (/approval_status:\s*approved_final/.test(intakeText) && /status:\s*approved_final/.test(intakeText)) {
  assetGate = "PASS_OR_READY_FOR_VALIDATOR";
} else {
  assetGate = "EXPECTED_ASSET_GATE_FAIL";
}

if (manifest) {
  if (!fs.existsSync(root)) errors.push("DR001 source pack root does not exist.");
  if (manifest.active_namespace?.upper !== "DR001_" || manifest.active_namespace?.lower !== "dr001_") {
    errors.push("Manifest active namespace is not DR001_ / dr001_.");
  }
  if (manifest.target_environment !== "staging") {
    errors.push("Manifest target_environment must be staging.");
  }
  if (!manifest.target_root || !manifest.target_root.includes("Server \u2014 kopia/1.21.11")) {
    errors.push(`Manifest target_root is not approved staging path: ${manifest.target_root}`);
  }

  const operations = Array.isArray(manifest.operations) ? manifest.operations : [];
  if (operations.length === 0) errors.push("Manifest has no operations.");

  for (const [index, op] of operations.entries()) {
    const label = `operation[${index}]`;
    const source = op.source || "";
    const target = op.target || "";
    const sourcePath = repoPath(source);
    const targetPath = repoPath(target);
    const sourceExt = path.extname(source).toLowerCase();
    const targetExt = path.extname(target).toLowerCase();

    if (!fs.existsSync(sourcePath)) errors.push(`${label} source missing: ${source}`);
    if (binaryExtensions.has(sourceExt) || binaryExtensions.has(targetExt)) {
      errors.push(`${label} includes forbidden binary asset extension: ${source} -> ${target}`);
    }
    if (/dungeon_ruins_001_[A-Za-z0-9_]+/.test(source) || /dungeon_ruins_001_[A-Za-z0-9_]+/.test(target)) {
      errors.push(`${label} includes legacy scaffold asset token: ${source} -> ${target}`);
    }
    if (!op.safe_for_config_only_sync) {
      errors.push(`${label} is not marked safe_for_config_only_sync.`);
    }
    const targetUnderRoot = relativeToTargetRoot(manifest.target_root, target);
    if (!targetUnderRoot || !targetUnderRoot.startsWith(`plugins${path.sep}`)) {
      errors.push(`${label} target is not staging plugin-native scoped: ${target}`);
    } else if (!recognizedTargets.some((pattern) => pattern.test(targetUnderRoot))) {
      errors.push(`${label} target is not a recognized config-only path: ${targetUnderRoot}`);
    }
    if (!fs.existsSync(targetPath)) {
      notInstalled.push(target);
    }
  }
}

const status = errors.length > 0
  ? "FAIL"
  : notInstalled.length > 0
    ? "NOT_INSTALLED"
    : "READY_FOR_APPROVED_CONFIG_ONLY_SYNC";

const result = {
  status,
  assetGate,
  activeNamespace: "DR001_ / dr001_",
  mythicMobsRuntimeSmokeIds: mythicMobIds,
  mythicSkillCount: mythicSkillIds.length,
  notInstalled,
  errors,
  warnings
};

console.log(JSON.stringify(result, null, 2));
process.exit(errors.length === 0 ? 0 : 1);
