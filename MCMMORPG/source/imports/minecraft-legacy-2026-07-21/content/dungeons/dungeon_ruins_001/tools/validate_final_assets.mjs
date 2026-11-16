#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";
import crypto from "node:crypto";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const root = path.resolve(__dirname, "..");
const intake = path.join(root, "intake");
const jsonMode = process.argv.includes("--json");
const allowReviewCandidates = process.argv.includes("--allow-review-candidates");

const requiredSchematics = [
  "gatehouse_broken_01.schem",
  "rooted_courtyard_01.schem",
  "varos_gate_arena_01.schem",
  "collapsed_stairwell_01.schem",
  "cistern_puzzle_01.schem",
  "nhalruun_oracle_chamber_01.schem",
  "crown_bridge_01.schem",
  "void_anchor_hall_01.schem",
  "void_sanctum_arena_01.schem",
  "loot_shrine_exit_01.schem"
];

const requiredModels = {
  "dr001_ruin_thrall.bbmodel": ["idle", "walk", "attack", "hit", "death"],
  "dr001_mossbound_archer.bbmodel": ["idle", "walk", "shoot", "root_cast", "death"],
  "dr001_rootling.bbmodel": ["idle", "leap", "attack", "death"],
  "dr001_animated_rubble.bbmodel": ["idle", "walk", "slam", "crumble", "death"],
  "dr001_gatewarden_varos.bbmodel": ["idle", "walk", "attack_slam", "shield", "roar", "death"],
  "dr001_drowned_tombguard.bbmodel": ["idle", "walk", "shield_bash", "death"],
  "dr001_plague_acolyte.bbmodel": ["idle", "walk", "cast", "heal", "death"],
  "dr001_cistern_leech.bbmodel": ["idle", "swim", "bite", "death"],
  "dr001_bone_mason.bbmodel": ["idle", "walk", "summon", "cast", "death"],
  "dr001_nhalruun.bbmodel": ["idle", "swim_idle", "cast", "flood", "mirror", "death"],
  "dr001_voidbound_knight.bbmodel": ["idle", "walk", "cleave", "enrage", "death"],
  "dr001_rune_sentinel.bbmodel": ["idle", "beam_cast", "overload", "death"],
  "dr001_crown_shade.bbmodel": ["idle", "blink", "stab", "death"],
  "dr001_anchor_warden.bbmodel": ["idle", "walk", "shield", "slam", "death"],
  "dr001_ruined_king.bbmodel": ["idle", "walk", "cleave", "shockwave", "summon", "phase", "death"]
};

const requiredSounds = [
  "sounds/dr001/roots_whisper.ogg",
  "sounds/dr001/cistern_geyser.ogg",
  "sounds/dr001/void_crown_pulse.ogg",
  "sounds/dr001/aurelion_enrage.ogg"
];

const requiredNexoFiles = [
  "models/item/dr001/block/rune_cracked_stone.json",
  "models/item/dr001/block/ancient_moss_panel.json",
  "models/item/dr001/block/cursed_water_grate.json",
  "models/item/dr001/block/void_rune_block.json",
  "models/item/dr001/block/broken_crown_ornament.json",
  "models/item/dr001/furniture/void_anchor.json",
  "models/item/dr001/furniture/rune_pedestal.json",
  "models/item/dr001/furniture/broken_king_statue.json",
  "models/item/dr001/furniture/soul_censer.json",
  "models/item/dr001/furniture/loot_shrine.json",
  "models/item/dr001/item/ruined_gate_key.json",
  "models/item/dr001/item/relic_shard.json",
  "models/item/dr001/item/cleansing_draught.json",
  "textures/item/dr001/block/rune_cracked_stone.png",
  "textures/item/dr001/block/ancient_moss_panel.png",
  "textures/item/dr001/block/cursed_water_grate.png",
  "textures/item/dr001/block/void_rune_block.png",
  "textures/item/dr001/block/broken_crown_ornament.png",
  "textures/item/dr001/furniture/void_anchor.png",
  "textures/item/dr001/furniture/rune_pedestal.png",
  "textures/item/dr001/furniture/broken_king_statue.png",
  "textures/item/dr001/furniture/soul_censer.png",
  "textures/item/dr001/furniture/loot_shrine.png",
  "textures/item/dr001/item/ruined_gate_key.png",
  "textures/item/dr001/item/relic_shard.png",
  "textures/item/dr001/item/cleansing_draught.png"
];

const report = {
  status: "PASS",
  gateCodes: [],
  checkedAt: new Date().toISOString(),
  approvalStatus: "unknown",
  manifestStatus: "unknown",
  errors: [],
  warnings: [],
  missing: [],
  found: [],
  legacyRejected: [],
  manifestChecksums: [],
  audio: [],
  files: []
};

function exists(file) {
  return fs.existsSync(file);
}

function rel(file, base = root) {
  return path.relative(base, file).replaceAll("\\", "/");
}

function sha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

function fail(message) {
  report.status = "FAIL";
  report.errors.push(message);
}

function failMissing(kind, relativePath) {
  report.missing.push({ kind, path: relativePath });
  fail(`Missing required ${kind}: ${relativePath}`);
}

function note(file, kind) {
  const entry = { path: rel(file), kind, sha256: sha256(file) };
  report.found.push(entry);
  report.files.push(entry);
}

function commandExists(name) {
  const probe = spawnSync(name, ["-version"], { encoding: "utf8", windowsHide: true });
  return !probe.error;
}

const hasFfprobe = commandExists("ffprobe");
const hasFfmpeg = commandExists("ffmpeg");

function inspectOgg(file, relative) {
  const audio = { path: `intake/${relative}` };
  if (hasFfprobe) {
    const probe = spawnSync("ffprobe", [
      "-v", "error",
      "-show_entries", "stream=codec_name,channels,sample_rate:format=duration",
      "-of", "json",
      file
    ], { encoding: "utf8", windowsHide: true });
    if (probe.status !== 0) {
      fail(`ffprobe could not inspect OGG intake/${relative}: ${probe.stderr.trim() || "unknown error"}`);
    } else {
      try {
        const parsed = JSON.parse(probe.stdout);
        const stream = parsed.streams?.[0];
        audio.codec = stream?.codec_name || "unknown";
        audio.sampleRate = Number(stream?.sample_rate || 0);
        audio.channels = Number(stream?.channels || 0);
        audio.duration = Number(parsed.format?.duration || 0);
        if (audio.codec !== "vorbis") fail(`Invalid OGG codec for intake/${relative}: ${audio.codec}. Expected vorbis.`);
        if (!(audio.duration > 0)) fail(`Invalid OGG duration for intake/${relative}: ${audio.duration}`);
        if (!(audio.sampleRate > 0)) fail(`Invalid OGG sample rate for intake/${relative}: ${audio.sampleRate}`);
        if (!(audio.channels > 0)) fail(`Invalid OGG channel count for intake/${relative}: ${audio.channels}`);
      } catch (error) {
        fail(`Could not parse ffprobe output for intake/${relative}: ${error.message}`);
      }
    }
  } else {
    report.warnings.push("ffprobe not found; OGG codec/duration inspection skipped.");
  }

  if (hasFfmpeg) {
    const decoded = spawnSync("ffmpeg", [
      "-v", "error",
      "-i", file,
      "-f", "s16le",
      "-acodec", "pcm_s16le",
      "-ac", "1",
      "-ar", "44100",
      "pipe:1"
    ], { encoding: "buffer", maxBuffer: 16 * 1024 * 1024, windowsHide: true });
    if (decoded.status !== 0) {
      fail(`ffmpeg could not decode OGG intake/${relative}: ${decoded.stderr.toString("utf8").trim() || "unknown error"}`);
    } else if (decoded.stdout.length < 2) {
      fail(`Decoded OGG audio is empty: intake/${relative}`);
    } else {
      let peak = 0;
      let sumSquares = 0;
      const samples = Math.floor(decoded.stdout.length / 2);
      for (let offset = 0; offset + 1 < decoded.stdout.length; offset += 2) {
        const value = Math.abs(decoded.stdout.readInt16LE(offset) / 32768);
        peak = Math.max(peak, value);
        sumSquares += value * value;
      }
      audio.peak = Number(peak.toFixed(6));
      audio.rms = Number(Math.sqrt(sumSquares / samples).toFixed(6));
      if (!(audio.peak > 0.001)) fail(`Decoded OGG peak is too low or silent: intake/${relative} peak=${audio.peak}`);
      if (!(audio.rms > 0.001)) fail(`Decoded OGG RMS is too low or silent: intake/${relative} rms=${audio.rms}`);
    }
  } else {
    report.warnings.push("ffmpeg not found; decoded OGG content inspection skipped.");
  }

  report.audio.push(audio);
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function hasNonEmptyHitbox(value) {
  if (!value || typeof value !== "object") return false;
  if (Array.isArray(value)) return value.some(hasNonEmptyHitbox);
  for (const [key, child] of Object.entries(value)) {
    if (/hitbox/i.test(key)) {
      if (Array.isArray(child)) return child.length > 0;
      if (child && typeof child === "object") return Object.keys(child).length > 0;
      return child !== undefined && child !== null && child !== "";
    }
    if (child && typeof child === "object" && hasNonEmptyHitbox(child)) return true;
  }
  return false;
}

function animationNames(modelJson) {
  const animations = modelJson.animations || {};
  return Object.entries(animations).flatMap(([key, value]) => {
    const names = [key.split(".").pop()];
    if (value && typeof value === "object" && value.name) names.push(value.name);
    return names;
  });
}

const manifestFile = path.join(intake, "manifest.yml");
if (exists(manifestFile)) {
  const manifestText = fs.readFileSync(manifestFile, "utf8");
  report.approvalStatus = (manifestText.match(/approval_status:\s*([^\r\n]+)/) || [null, "unknown"])[1].trim();
  report.manifestStatus = (manifestText.match(/^status:\s*([^\r\n]+)/m) || [null, "unknown"])[1].trim();
  const todoChecksums = manifestText.match(/sha256:\s*TODO\b/g) || [];
  const candidateStatuses = manifestText.match(/status:\s*(hybrid_candidate|pending|scaffold|candidate|legacy)/g) || [];
  const manifestEntries = [...manifestText.matchAll(/-\s+path:\s*([^\r\n]+)\r?\n\s+sha256:\s*([a-fA-F0-9]{64}|missing|TODO)\b/g)];

  for (const match of manifestEntries) {
    const manifestRelative = match[1].trim();
    const expected = match[2].trim().toLowerCase();
    const manifestPath = path.resolve(root, manifestRelative.replaceAll("/", path.sep));
    const entry = { path: manifestRelative, sha256: expected };
    report.manifestChecksums.push(entry);
    if (expected === "missing" || expected === "todo") continue;
    if (!manifestPath.startsWith(root + path.sep) && manifestPath !== root) {
      fail(`Manifest checksum path escapes DR001 root: ${manifestRelative}`);
      continue;
    }
    if (!exists(manifestPath)) {
      fail(`Manifest checksum file is missing: ${manifestRelative}`);
      continue;
    }
    const actual = sha256(manifestPath);
    entry.actual = actual;
    if (actual !== expected) fail(`Checksum mismatch for ${manifestRelative}: manifest=${expected} actual=${actual}`);
  }

  if (!allowReviewCandidates) {
    if (report.approvalStatus !== "approved_final") fail(`Final intake is not approved: approval_status=${report.approvalStatus}. Expected approved_final.`);
    if (report.manifestStatus !== "approved_final") fail(`Final intake manifest is not final: status=${report.manifestStatus}. Expected approved_final.`);
    if (todoChecksums.length > 0) fail(`Manifest still contains ${todoChecksums.length} TODO checksum placeholder(s).`);
    if (manifestEntries.some((match) => match[2].trim().toLowerCase() === "missing")) fail("Manifest still contains missing checksum entries.");
    if (candidateStatuses.length > 0) fail(`Manifest still contains ${candidateStatuses.length} non-final status value(s).`);
  } else {
    report.warnings.push("--allow-review-candidates is review-only and not valid for production or final staging import.");
  }
} else {
  fail(`Missing intake manifest: ${rel(manifestFile)}`);
  report.missing.push({ kind: "manifest", path: rel(manifestFile) });
}

for (const folder of ["schematics", "nexo", "modelengine", "sounds"]) {
  const folderPath = path.join(intake, folder);
  if (!exists(folderPath)) failMissing("intake folder", rel(folderPath));
}

if (exists(intake)) {
  const stack = [intake];
  while (stack.length > 0) {
    const current = stack.pop();
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const full = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(full);
      } else {
        const relative = rel(full, intake);
        if (entry.name !== ".gitkeep" && /dungeon_ruins_001/.test(relative)) {
          report.legacyRejected.push({
            path: `intake/${relative}`,
            reason: "legacy dungeon_ruins_001 namespace is not final dr001 intake"
          });
        }
      }
    }
  }
}

for (const name of requiredSchematics) {
  const file = path.join(intake, "schematics", name);
  if (!exists(file)) {
    failMissing("schematic", `intake/schematics/${name}`);
    continue;
  }
  const raw = fs.readFileSync(file);
  try {
    if (raw[0] === 0x1f && raw[1] === 0x8b) zlib.gunzipSync(raw);
    note(file, "schematic");
  } catch (error) {
    fail(`Could not inspect schematic ${name}: ${error.message}`);
  }
}

for (const [name, animations] of Object.entries(requiredModels)) {
  const file = path.join(intake, "modelengine", name);
  if (!exists(file)) {
    failMissing("ModelEngine blueprint", `intake/modelengine/${name}`);
    continue;
  }
  try {
    const model = readJson(file);
    const names = animationNames(model);
    for (const animation of animations) {
      if (!names.includes(animation)) fail(`Missing animation ${animation} in ${name}`);
    }
    if (!hasNonEmptyHitbox(model)) fail(`Missing non-empty hitbox data in ${name}`);
    note(file, "bbmodel");
  } catch (error) {
    fail(`Could not parse ModelEngine blueprint ${name}: ${error.message}`);
  }
}

for (const relative of requiredNexoFiles) {
  const file = path.join(intake, "nexo", relative);
  if (!exists(file)) {
    failMissing("Nexo final asset", `intake/nexo/${relative}`);
    continue;
  }
  try {
    if (relative.endsWith(".json")) readJson(file);
    if (relative.endsWith(".png")) {
      const buffer = fs.readFileSync(file);
      if (!(buffer[0] === 137 && buffer[1] === 80 && buffer[2] === 78 && buffer[3] === 71)) fail(`Invalid PNG signature: intake/nexo/${relative}`);
    }
    note(file, "nexo");
  } catch (error) {
    fail(`Could not inspect Nexo asset intake/nexo/${relative}: ${error.message}`);
  }
}

for (const relative of requiredSounds) {
  const file = path.join(intake, relative);
  if (!exists(file)) {
    failMissing("custom OGG", `intake/${relative}`);
    continue;
  }
  const buffer = fs.readFileSync(file);
  if (buffer.length === 0) fail(`Zero-byte OGG file: intake/${relative}`);
  if (buffer.subarray(0, 4).toString("ascii") !== "OggS") fail(`Invalid OGG signature: intake/${relative}`);
  inspectOgg(file, relative);
  note(file, "ogg");
}

if (!exists(manifestFile) || report.missing.length > 0) report.gateCodes.push("MISSING_INTAKE");
if (report.approvalStatus !== "approved_final" || report.manifestStatus !== "approved_final") {
  report.gateCodes.push("INTAKE_PRESENT_NOT_APPROVED");
}
if (report.errors.some((error) => /checksum mismatch/i.test(error))) report.gateCodes.push("CHECKSUM_MISMATCH");
if (report.legacyRejected.length > 0) report.gateCodes.push("LEGACY_ASSET_REJECTED");
if (report.status === "PASS" && !allowReviewCandidates) report.gateCodes.push("FINAL_ASSETS_INSTALLED");
if (report.status === "PASS" && allowReviewCandidates) report.gateCodes.push("READY_FOR_FINAL_ASSET_IMPORT");
report.gateCodes = [...new Set(report.gateCodes)];

if (jsonMode) {
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
} else {
  console.log(`DR001 final asset validation: ${report.status}`);
  for (const error of report.errors) console.log(`ERROR: ${error}`);
  for (const warning of report.warnings) console.log(`WARN: ${warning}`);
  if (report.gateCodes.length > 0) console.log(`Gate codes: ${report.gateCodes.join(", ")}`);
  console.log(`Checked files: ${report.files.length}`);
}

process.exit(report.status === "PASS" ? 0 : 1);
