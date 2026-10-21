#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), "..");
const outDir = path.join(root, "intake", "sounds", "dr001_source");
const sampleRate = 44100;

const cues = [
  {
    name: "roots_whisper",
    duration: 6.0,
    notes: "Low moss/root ambience with filtered wind, slow sub pulse, and faint creaking overtones."
  },
  {
    name: "cistern_geyser",
    duration: 3.2,
    notes: "Water pressure burst with short stone resonance tail."
  },
  {
    name: "void_crown_pulse",
    duration: 3.8,
    notes: "Readable void-crown pulse with cyan/violet shimmer and low impact."
  },
  {
    name: "aurelion_enrage",
    duration: 4.5,
    notes: "Dark regal boss enrage sting with crown clang, bass fall, and deathlike tail."
  }
];

function mulberry32(seed) {
  return () => {
    let t = seed += 0x6d2b79f5;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function envelope(t, duration, attack = 0.04, release = 0.5) {
  const fadeIn = Math.min(1, t / attack);
  const fadeOut = Math.min(1, (duration - t) / release);
  return Math.max(0, Math.min(fadeIn, fadeOut));
}

function clamp(value) {
  return Math.max(-0.98, Math.min(0.98, value));
}

function synthSample(cue, t, random) {
  const d = cue.duration;
  const n = random() * 2 - 1;
  if (cue.name === "roots_whisper") {
    const wind = Math.sin(2 * Math.PI * (48 + 7 * Math.sin(t * 0.7)) * t) * 0.14;
    const breath = Math.sin(2 * Math.PI * 0.35 * t) * 0.09;
    const creak = Math.sin(2 * Math.PI * 190 * t) * Math.max(0, Math.sin(2 * Math.PI * 0.22 * t)) * 0.025;
    return (wind + breath + creak + n * 0.035) * envelope(t, d, 0.25, 0.9);
  }
  if (cue.name === "cistern_geyser") {
    const burst = n * 0.32 * Math.exp(-1.7 * t);
    const pressure = Math.sin(2 * Math.PI * (75 + 120 * Math.exp(-2.2 * t)) * t) * 0.22 * Math.exp(-1.2 * t);
    const stone = Math.sin(2 * Math.PI * 132 * t) * 0.16 * Math.exp(-0.8 * Math.max(0, t - 0.45));
    return (burst + pressure + stone) * envelope(t, d, 0.01, 0.7);
  }
  if (cue.name === "void_crown_pulse") {
    const pulse = Math.sin(2 * Math.PI * 62 * t) * 0.22 * Math.exp(-0.55 * t);
    const shimmer = Math.sin(2 * Math.PI * (390 + 32 * Math.sin(t * 5)) * t) * 0.08 * Math.sin(Math.PI * t / d);
    const beating = Math.sin(2 * Math.PI * 1.7 * t) * Math.sin(2 * Math.PI * 124 * t) * 0.08;
    return (pulse + shimmer + beating + n * 0.018) * envelope(t, d, 0.03, 0.65);
  }
  const clang = Math.sin(2 * Math.PI * 310 * t) * 0.18 * Math.exp(-1.35 * t);
  const crown = Math.sin(2 * Math.PI * 522 * t) * 0.09 * Math.exp(-1.0 * t);
  const fall = Math.sin(2 * Math.PI * (98 - 34 * Math.min(1, t / d)) * t) * 0.24 * Math.exp(-0.35 * t);
  const tail = Math.sin(2 * Math.PI * 41 * t) * 0.16 * Math.sin(Math.PI * t / d);
  return (clang + crown + fall + tail + n * 0.012) * envelope(t, d, 0.02, 1.15);
}

function wavBuffer(samples) {
  const dataBytes = samples.length * 2;
  const buffer = Buffer.alloc(44 + dataBytes);
  buffer.write("RIFF", 0, "ascii");
  buffer.writeUInt32LE(36 + dataBytes, 4);
  buffer.write("WAVE", 8, "ascii");
  buffer.write("fmt ", 12, "ascii");
  buffer.writeUInt32LE(16, 16);
  buffer.writeUInt16LE(1, 20);
  buffer.writeUInt16LE(1, 22);
  buffer.writeUInt32LE(sampleRate, 24);
  buffer.writeUInt32LE(sampleRate * 2, 28);
  buffer.writeUInt16LE(2, 32);
  buffer.writeUInt16LE(16, 34);
  buffer.write("data", 36, "ascii");
  buffer.writeUInt32LE(dataBytes, 40);
  for (let i = 0; i < samples.length; i++) {
    buffer.writeInt16LE(Math.round(clamp(samples[i]) * 32767), 44 + i * 2);
  }
  return buffer;
}

function sha256(file) {
  return crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

fs.mkdirSync(outDir, { recursive: true });

const generated = [];
for (const cue of cues) {
  const random = mulberry32(0x44523031 + cue.name.length);
  const count = Math.floor(cue.duration * sampleRate);
  const samples = new Array(count);
  let peak = 0;
  let sumSquares = 0;
  for (let i = 0; i < count; i++) {
    const value = clamp(synthSample(cue, i / sampleRate, random));
    samples[i] = value;
    peak = Math.max(peak, Math.abs(value));
    sumSquares += value * value;
  }
  const file = path.join(outDir, `${cue.name}.wav`);
  fs.writeFileSync(file, wavBuffer(samples));
  generated.push({
    file,
    cue,
    sha256: sha256(file),
    peak: Number(peak.toFixed(4)),
    rms: Number(Math.sqrt(sumSquares / count).toFixed(4))
  });
}

const notes = [
  "# DR001 Source Audio Notes",
  "",
  "Status: source_wav_prepared_ogg_blocked",
  "Generated: 2026-05-30",
  "",
  "These WAV files are synthetic local source-authoring cues only. They are not final resource-pack assets and must be converted to valid Ogg Vorbis before final approval.",
  "",
  "| Cue | WAV source | Duration | Peak | RMS | SHA-256 | Notes |",
  "| --- | --- | ---: | ---: | ---: | --- | --- |",
  ...generated.map((entry) => `| ${entry.cue.name} | intake/sounds/dr001_source/${path.basename(entry.file)} | ${entry.cue.duration.toFixed(1)}s | ${entry.peak} | ${entry.rms} | ${entry.sha256} | ${entry.cue.notes} |`),
  "",
  "Required conversion command once an encoder is available:",
  "",
  "```powershell",
  "ffmpeg -y -i intake\\sounds\\dr001_source\\roots_whisper.wav -codec:a libvorbis intake\\sounds\\dr001\\roots_whisper.ogg",
  "ffmpeg -y -i intake\\sounds\\dr001_source\\cistern_geyser.wav -codec:a libvorbis intake\\sounds\\dr001\\cistern_geyser.ogg",
  "ffmpeg -y -i intake\\sounds\\dr001_source\\void_crown_pulse.wav -codec:a libvorbis intake\\sounds\\dr001\\void_crown_pulse.ogg",
  "ffmpeg -y -i intake\\sounds\\dr001_source\\aurelion_enrage.wav -codec:a libvorbis intake\\sounds\\dr001\\aurelion_enrage.ogg",
  "```",
  "",
  "Equivalent `oggenc` conversion is acceptable if it produces valid Ogg Vorbis and checksums are recorded in `intake/manifest.yml`."
];

fs.writeFileSync(path.join(root, "qa", "DR001_SOURCE_AUDIO_NOTES.md"), `${notes.join("\n")}\n`);

for (const entry of generated) {
  console.log(`${path.relative(root, entry.file).replaceAll("\\", "/")} sha256:${entry.sha256} peak:${entry.peak} rms:${entry.rms}`);
}
