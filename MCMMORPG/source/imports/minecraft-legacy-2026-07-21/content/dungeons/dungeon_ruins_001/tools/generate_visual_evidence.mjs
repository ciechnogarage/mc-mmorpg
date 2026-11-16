import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { spawnSync } from 'node:child_process';

const require = createRequire(import.meta.url);
const { createCanvas, loadImage } = require(path.resolve('tools/playtest-bot/node_modules/canvas'));
const nbt = require(path.resolve('tools/playtest-bot/node_modules/prismarine-nbt'));

const root = path.resolve('content/dungeons/dungeon_ruins_001');
const outRoot = path.join(root, 'qa', 'visual_evidence');
const bossOut = path.join(outRoot, 'bosses');
const nexoOut = path.join(outRoot, 'nexo');
const schematicOut = path.join(outRoot, 'schematics');
const audioOut = path.join(outRoot, 'audio');
const metaOut = path.join(outRoot, 'metadata');

for (const dir of [bossOut, nexoOut, schematicOut, audioOut, metaOut]) {
  fs.mkdirSync(dir, { recursive: true });
}

const bossModels = [
  ['DR001_GatewardenVaros', 'dr001_gatewarden_varos.bbmodel'],
  ['DR001_CisternOracleNhalruun', 'dr001_nhalruun.bbmodel'],
  ['DR001_RuinedKingAurelion', 'dr001_ruined_king.bbmodel']
];

const nexoAssets = [
  ['dr001_rune_cracked_stone', 'block/rune_cracked_stone'],
  ['dr001_rune_pedestal', 'furniture/rune_pedestal'],
  ['dr001_void_anchor', 'furniture/void_anchor'],
  ['dr001_relic_shard', 'item/relic_shard']
];

const schematicAssets = [
  'gatehouse_broken_01.schem',
  'varos_gate_arena_01.schem'
];

const soundAssets = [
  'roots_whisper',
  'cistern_geyser',
  'void_crown_pulse',
  'aurelion_enrage'
];

const blockColors = [
  ['air', null],
  ['water', '#356fb0'],
  ['moss', '#5f8b55'],
  ['root', '#6f4f32'],
  ['dirt', '#6f4f32'],
  ['lantern', '#f0c560'],
  ['glow', '#9ad17b'],
  ['chain', '#7b7f83'],
  ['chest', '#9a642f'],
  ['blackstone', '#2d2b34'],
  ['deepslate', '#4d4d58'],
  ['cracked', '#6a6661'],
  ['stone', '#7a7874']
];

function writePng(canvas, outputPath) {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, canvas.toBuffer('image/png'));
}

function label(ctx, text, x, y, size = 18, color = '#f6f2e8') {
  ctx.fillStyle = color;
  ctx.font = `${size}px Arial`;
  ctx.fillText(text, x, y);
}

function modelColor(model) {
  const src = model.textures?.find((texture) => texture.source)?.source;
  if (!src?.startsWith('data:image/png;base64,')) return '#7a8b6b';
  const raw = Buffer.from(src.slice(src.indexOf(',') + 1), 'base64');
  let hash = 0;
  for (const byte of raw) hash = (hash * 33 + byte) >>> 0;
  const r = 80 + (hash & 63);
  const g = 85 + ((hash >> 8) & 95);
  const b = 70 + ((hash >> 16) & 70);
  return `rgb(${r},${g},${b})`;
}

function project(point, scale, ox, oy) {
  const [x, y, z] = point;
  return [
    ox + (x - z) * scale,
    oy + (x + z) * scale * 0.5 - y * scale
  ];
}

function cubeCorners(from, to) {
  const [x1, y1, z1] = from;
  const [x2, y2, z2] = to;
  return [
    [x1, y1, z1], [x2, y1, z1], [x2, y2, z1], [x1, y2, z1],
    [x1, y1, z2], [x2, y1, z2], [x2, y2, z2], [x1, y2, z2]
  ];
}

function drawPoly(ctx, pts, fill, stroke = '#201f1c') {
  ctx.beginPath();
  pts.forEach(([x, y], index) => index ? ctx.lineTo(x, y) : ctx.moveTo(x, y));
  ctx.closePath();
  ctx.fillStyle = fill;
  ctx.fill();
  ctx.strokeStyle = stroke;
  ctx.lineWidth = 1;
  ctx.stroke();
}

function shade(color, amount) {
  const nums = color.match(/\d+/g)?.map(Number);
  if (!nums) return color;
  const adjusted = nums.map((v) => Math.max(0, Math.min(255, Math.round(v * amount))));
  return `rgb(${adjusted[0]},${adjusted[1]},${adjusted[2]})`;
}

async function renderBbmodel(labelText, inputPath, outputPath) {
  const model = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
  const cubes = model.elements.filter((el) => el.type === 'cube' && Array.isArray(el.from) && Array.isArray(el.to));
  const all = cubes.flatMap((cube) => [...cube.from, ...cube.to]);
  const min = Math.min(...all);
  const max = Math.max(...all);
  const scale = Math.min(22, 520 / Math.max(1, max - min));
  const canvas = createCanvas(900, 700);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#161714';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  label(ctx, labelText, 28, 38, 22);
  label(ctx, `source: ${path.relative(root, inputPath).replaceAll('\\', '/')}`, 28, 64, 13, '#c7c2b8');
  label(ctx, `${cubes.length} cubes | visible box ${JSON.stringify(model.visible_box ?? [])}`, 28, 84, 13, '#c7c2b8');

  const base = modelColor(model);
  const ox = 450;
  const oy = 560;
  const sorted = cubes.slice().sort((a, b) => (a.from[1] + a.to[1]) - (b.from[1] + b.to[1]));
  for (const cube of sorted) {
    const corners = cubeCorners(cube.from, cube.to).map((p) => project(p, scale, ox, oy));
    drawPoly(ctx, [corners[2], corners[3], corners[7], corners[6]], shade(base, 1.18));
    drawPoly(ctx, [corners[1], corners[2], corners[6], corners[5]], shade(base, 0.92));
    drawPoly(ctx, [corners[4], corners[5], corners[6], corners[7]], shade(base, 0.72));
  }

  const texture = model.textures?.find((entry) => entry.source?.startsWith('data:image/png;base64,'));
  if (texture) {
    const image = await loadImage(texture.source);
    ctx.fillStyle = '#24251f';
    ctx.fillRect(662, 92, 202, 236);
    label(ctx, 'embedded texture', 678, 122, 14, '#f6f2e8');
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(image, 698, 142, 128, 128);
    label(ctx, `${texture.width}x${texture.height}`, 730, 292, 13, '#c7c2b8');
  }
  writePng(canvas, outputPath);
}

async function renderNexo(assetId, relPath, outputPath) {
  const modelPath = path.join(root, 'intake', 'nexo', 'models', 'item', 'dr001', `${relPath}.json`);
  const texturePath = path.join(root, 'intake', 'nexo', 'textures', 'item', 'dr001', `${relPath}.png`);
  const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
  const texture = await loadImage(texturePath);
  const canvas = createCanvas(760, 520);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#151713';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  label(ctx, assetId, 28, 40, 22);
  label(ctx, `model: ${path.relative(root, modelPath).replaceAll('\\', '/')}`, 28, 66, 13, '#c7c2b8');
  label(ctx, `texture: ${path.relative(root, texturePath).replaceAll('\\', '/')}`, 28, 86, 13, '#c7c2b8');

  ctx.imageSmoothingEnabled = false;
  ctx.fillStyle = '#24251f';
  ctx.fillRect(34, 118, 224, 224);
  ctx.drawImage(texture, 66, 150, 160, 160);
  label(ctx, `${texture.width}x${texture.height}`, 112, 372, 13, '#c7c2b8');

  const elements = model.elements ?? [
    { from: [0, 0, 0], to: [16, 16, 16] }
  ];
  const scale = 12;
  const ox = 520;
  const oy = 390;
  for (const el of elements) {
    const corners = cubeCorners(el.from, el.to).map((p) => project(p, scale, ox, oy));
    drawPoly(ctx, [corners[2], corners[3], corners[7], corners[6]], '#78805e');
    drawPoly(ctx, [corners[1], corners[2], corners[6], corners[5]], '#5f6f57');
    drawPoly(ctx, [corners[4], corners[5], corners[6], corners[7]], '#485345');
  }
  label(ctx, `${elements.length} render element(s)`, 438, 454, 14, '#c7c2b8');
  writePng(canvas, outputPath);
}

function decodeVarints(data, total) {
  const values = [];
  let value = 0;
  let shift = 0;
  for (const byte of data) {
    value |= (byte & 0x7f) << shift;
    if ((byte & 0x80) === 0) {
      values.push(value);
      value = 0;
      shift = 0;
      if (values.length === total) break;
    } else {
      shift += 7;
    }
  }
  return values;
}

function colorForBlock(name) {
  for (const [needle, color] of blockColors) {
    if (name.includes(needle)) return color;
  }
  return '#868276';
}

async function renderSchematic(fileName, outputPath) {
  const inputPath = path.join(root, 'intake', 'schematics', fileName);
  const parsed = await nbt.parse(fs.readFileSync(inputPath));
  const data = nbt.simplify(parsed.parsed);
  const width = data.Width;
  const height = data.Height;
  const length = data.Length;
  const paletteById = Object.fromEntries(Object.entries(data.Palette).map(([name, id]) => [id, name]));
  const blocks = decodeVarints(data.BlockData, width * height * length);

  const canvas = createCanvas(900, 640);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#151713';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  label(ctx, fileName, 28, 40, 22);
  label(ctx, `dimensions: ${width}w x ${height}h x ${length}l | palette: ${Object.keys(data.Palette).length}`, 28, 66, 13, '#c7c2b8');
  label(ctx, `metadata: ${JSON.stringify(data.Metadata ?? {})}`, 28, 86, 13, '#c7c2b8');

  const cell = Math.floor(Math.min(720 / width, 440 / length));
  const x0 = 40;
  const y0 = 130;
  const counts = {};
  for (let z = 0; z < length; z++) {
    for (let x = 0; x < width; x++) {
      let blockName = 'minecraft:air';
      let topY = -1;
      for (let y = height - 1; y >= 0; y--) {
        const id = blocks[x + z * width + y * width * length];
        const name = paletteById[id] ?? 'minecraft:air';
        if (!name.includes('air')) {
          blockName = name;
          topY = y;
          counts[name] = (counts[name] ?? 0) + 1;
          break;
        }
      }
      const color = colorForBlock(blockName);
      if (color) {
        ctx.fillStyle = color;
        ctx.fillRect(x0 + x * cell, y0 + z * cell, Math.max(1, cell), Math.max(1, cell));
        if (topY >= 0 && cell >= 7) {
          ctx.fillStyle = `rgba(255,255,255,${0.08 + topY / height * 0.18})`;
          ctx.fillRect(x0 + x * cell, y0 + z * cell, Math.max(1, cell), Math.max(1, cell));
        }
      }
    }
  }
  ctx.strokeStyle = '#d8d0bd';
  ctx.strokeRect(x0, y0, width * cell, length * cell);
  label(ctx, 'top-down highest solid block overview', x0, y0 + length * cell + 28, 14, '#f6f2e8');

  const topBlocks = Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 8);
  label(ctx, 'dominant visible blocks', 620, 144, 15, '#f6f2e8');
  topBlocks.forEach(([name, count], i) => {
    ctx.fillStyle = colorForBlock(name) ?? '#333';
    ctx.fillRect(622, 166 + i * 28, 18, 18);
    label(ctx, `${name.replace('minecraft:', '')}: ${count}`, 650, 181 + i * 28, 13, '#c7c2b8');
  });
  writePng(canvas, outputPath);
  return { fileName, width, height, length, palette: Object.keys(data.Palette), metadata: data.Metadata ?? {} };
}

async function renderTextureSheet() {
  const files = fs.readdirSync(path.join(root, 'intake', 'nexo', 'textures', 'item', 'dr001'), { recursive: true })
    .filter((file) => file.endsWith('.png'))
    .map((file) => file.replaceAll('\\', '/'));
  const canvas = createCanvas(1100, 800);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#151713';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  label(ctx, 'DR001 Nexo texture contact sheet', 28, 40, 24);
  label(ctx, `${files.length} final-intake texture PNGs`, 28, 68, 14, '#c7c2b8');
  ctx.imageSmoothingEnabled = false;
  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const image = await loadImage(path.join(root, 'intake', 'nexo', 'textures', 'item', 'dr001', file));
    const col = i % 5;
    const row = Math.floor(i / 5);
    const x = 34 + col * 210;
    const y = 108 + row * 210;
    ctx.fillStyle = '#24251f';
    ctx.fillRect(x, y, 176, 176);
    ctx.drawImage(image, x + 24, y + 18, 128, 128);
    label(ctx, file.replace('.png', ''), x, y + 194, 12, '#c7c2b8');
  }
  const outputPath = path.join(nexoOut, 'dr001_nexo_texture_contact_sheet.png');
  writePng(canvas, outputPath);
  return outputPath;
}

function renderAudio(sound) {
  const input = path.join(root, 'intake', 'sounds', 'dr001', `${sound}.ogg`);
  const wave = path.join(audioOut, `${sound}_waveform.png`);
  const spec = path.join(audioOut, `${sound}_spectrogram.png`);
  const common = ['-hide_banner', '-loglevel', 'error', '-y', '-i', input];
  const waveRun = spawnSync('ffmpeg', [...common, '-filter_complex', 'showwavespic=s=1200x260:colors=#7ad7b4', '-frames:v', '1', wave], { encoding: 'utf8' });
  const specRun = spawnSync('ffmpeg', [...common, '-lavfi', 'showspectrumpic=s=1200x520:legend=disabled:color=viridis', '-frames:v', '1', spec], { encoding: 'utf8' });
  return { sound, input, wave, spec, waveOk: waveRun.status === 0, specOk: specRun.status === 0, stderr: [waveRun.stderr, specRun.stderr].filter(Boolean).join('\n') };
}

const evidence = {
  generatedAtUtc: new Date().toISOString(),
  outputRoot: outRoot.replaceAll('\\', '/'),
  previewStrategy: [
    'Generated local preview PNGs using repo-local Node canvas and prismarine-nbt.',
    'Boss evidence renders Blockbench .bbmodel cube geometry and embedded textures.',
    'Nexo evidence renders candidate model geometry plus source texture PNGs.',
    'Schematic evidence renders top-down highest-solid-block overviews from .schem NBT.',
    'Audio evidence uses local ffmpeg to render waveform and spectrogram PNGs.'
  ],
  bosses: [],
  nexo: [],
  schematics: [],
  audio: []
};

for (const [bossId, fileName] of bossModels) {
  const input = path.join(root, 'intake', 'modelengine', fileName);
  const output = path.join(bossOut, `${bossId}.png`);
  await renderBbmodel(bossId, input, output);
  evidence.bosses.push({ bossId, source: input.replaceAll('\\', '/'), evidence: output.replaceAll('\\', '/') });
}

for (const [assetId, relPath] of nexoAssets) {
  const output = path.join(nexoOut, `${assetId}.png`);
  await renderNexo(assetId, relPath, output);
  evidence.nexo.push({ assetId, sourceKey: relPath, evidence: output.replaceAll('\\', '/') });
}

evidence.nexo.push({ assetId: 'dr001_all_nexo_textures', evidence: (await renderTextureSheet()).replaceAll('\\', '/') });

for (const fileName of schematicAssets) {
  const output = path.join(schematicOut, fileName.replace('.schem', '_overview.png'));
  const meta = await renderSchematic(fileName, output);
  evidence.schematics.push({ ...meta, evidence: output.replaceAll('\\', '/') });
}

for (const sound of soundAssets) {
  evidence.audio.push(renderAudio(sound));
}

fs.writeFileSync(path.join(metaOut, 'visual-evidence-manifest.json'), JSON.stringify(evidence, null, 2));
console.log(JSON.stringify(evidence, null, 2));
