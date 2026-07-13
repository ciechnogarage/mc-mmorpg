const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { createCanvas, loadImage } = require('canvas');

const ROOT = path.join(__dirname, 'reference_corpus');
const FRAMES_ROOT = path.join(ROOT, 'visual_atlas_v2');
const OUTPUT_ROOT = path.join(ROOT, 'manual_review_sheets');
const PHASES = ['start', 'anticipation', 'impact', 'recovery', 'end'];
const STATIC_VIEWS = ['front', 'side', 'back', 'three_quarter', 'silhouette', 'player_scale', 'hitbox', 'helpers'];
const CARDS_PATH = path.join(ROOT, 'modelengine-model-cards.json');
const ATLAS_MANIFEST_PATH = path.join(FRAMES_ROOT, 'manifest.json');

function sha1Text(value) {
  return crypto.createHash('sha1').update(String(value)).digest('hex');
}

function safeName(value) {
  return String(value).replace(/[^A-Za-z0-9_.-]+/g, '_');
}

function imageFiles(directory) {
  return fs.readdirSync(directory).filter((file) => file.endsWith('.png'));
}

function familyRoot(id) {
  const lower = String(id || '').toLowerCase();
  return lower
    .replace(/(?:^|[_-])(parts?|damage|damaged|pet|pets|summon|summons|vfx|fx|projectile|projectiles|gibs?|telegraph|ground(?:_?fx|_?crack)?|splash|portal|whirlpool|star|book|book_page|sword|egg|default)$/g, '')
    .replace(/(?:_variant|_phase\d+|_\d+seg|_\d+|_d)$/g, '')
    .replace(/^littleroom_/, '')
    .replace(/__+/g, '_')
    .replace(/^_+|_+$/g, '') || lower;
}

function modelOrder(root) {
  if (!fs.existsSync(CARDS_PATH)) return new Map();
  const data = JSON.parse(fs.readFileSync(CARDS_PATH, 'utf8'));
  const ordered = [...(data.cards || [])]
    .sort((a, b) => {
      const keyA = [a.archetype || 'zzz', familyRoot(a.id), a.id.toLowerCase()].join('|');
      const keyB = [b.archetype || 'zzz', familyRoot(b.id), b.id.toLowerCase()].join('|');
      return keyA.localeCompare(keyB);
    })
    .map((card, index) => [safeName(card.file.replace(/\.bbmodel$/, '')), index]);
  return new Map(ordered);
}

function modelDirectories(root) {
  const order = modelOrder(root);
  return fs.readdirSync(root, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => path.join(root, entry.name))
    .filter((directory) => imageFiles(directory).length > 0)
    .sort((a, b) => {
      const rankA = order.get(path.basename(a));
      const rankB = order.get(path.basename(b));
      if (Number.isFinite(rankA) && Number.isFinite(rankB)) return rankA - rankB;
      if (Number.isFinite(rankA)) return -1;
      if (Number.isFinite(rankB)) return 1;
      return path.basename(a).localeCompare(path.basename(b));
    });
}

function animationNames(files) {
  const names = new Set();
  for (const file of files) {
    const match = /^animation_(.+)_(start|anticipation|impact|recovery|end)\.png$/.exec(file);
    if (match) names.add(match[1]);
  }
  return [...names].sort();
}

function needsRebuild(target, sources) {
  if (!fs.existsSync(target)) return true;
  const targetTime = fs.statSync(target).mtimeMs;
  return sources.some((source) => fs.existsSync(source) && fs.statSync(source).mtimeMs > targetTime);
}

async function drawFrame(context, file, x, y, width, height) {
  if (!fs.existsSync(file)) {
    context.fillStyle = '#411';
    context.fillRect(x, y, width, height);
    return;
  }
  const image = await loadImage(file);
  context.drawImage(image, x, y, width, height);
}

async function staticSheet(modelDirectory, outputDirectory) {
  const target = path.join(outputDirectory, 'static.png');
  const sources = STATIC_VIEWS.map((view) => path.join(modelDirectory, `${view}.png`));
  if (!needsRebuild(target, sources)) return;
  const cell = 256;
  const canvas = createCanvas(cell * 4, cell * 2);
  const context = canvas.getContext('2d');
  for (const [index, view] of STATIC_VIEWS.entries()) {
    await drawFrame(context, path.join(modelDirectory, `${view}.png`), (index % 4) * cell, Math.floor(index / 4) * cell, cell, cell);
  }
  fs.writeFileSync(target, canvas.toBuffer('image/png'));
}

async function animationSheets(modelDirectory, outputDirectory, names) {
  const cell = 256;
  const rowsPerSheet = 6;
  for (let offset = 0; offset < names.length; offset += rowsPerSheet) {
    const rows = names.slice(offset, offset + rowsPerSheet);
    const target = path.join(outputDirectory, `animations_${String(offset / rowsPerSheet + 1).padStart(2, '0')}.png`);
    const sources = rows.flatMap((animation) => PHASES.map((phase) =>
      path.join(modelDirectory, `animation_${animation}_${phase}.png`)));
    if (!needsRebuild(target, sources)) continue;
    const canvas = createCanvas(cell * PHASES.length, cell * rows.length);
    const context = canvas.getContext('2d');
    for (const [row, animation] of rows.entries()) {
      for (const [column, phase] of PHASES.entries()) {
        await drawFrame(
          context,
          path.join(modelDirectory, `animation_${animation}_${phase}.png`),
          column * cell,
          row * cell,
          cell,
          cell,
        );
      }
    }
    fs.writeFileSync(target, canvas.toBuffer('image/png'));
  }
}

async function overviewSheets(directories) {
  const cellWidth = 512;
  const cellHeight = 256;
  const perSheet = 25;
  for (let offset = 0; offset < directories.length; offset += perSheet) {
    const models = directories.slice(offset, offset + perSheet);
    const canvas = createCanvas(cellWidth * 5, cellHeight * 5);
    const context = canvas.getContext('2d');
    context.fillStyle = '#0d1210';
    context.fillRect(0, 0, canvas.width, canvas.height);
    for (const [index, directory] of models.entries()) {
      const x = (index % 5) * cellWidth;
      const y = Math.floor(index / 5) * cellHeight;
      await drawFrame(context, path.join(directory, 'three_quarter.png'), x, y, 256, 256);
      await drawFrame(context, path.join(directory, 'silhouette.png'), x + 256, y, 256, 256);
      context.fillStyle = 'rgba(0,0,0,0.75)';
      context.fillRect(x, y + 226, cellWidth, 30);
      context.fillStyle = '#fff';
      context.font = '18px sans-serif';
      context.fillText(path.basename(directory), x + 8, y + 248);
    }
    fs.writeFileSync(
      path.join(OUTPUT_ROOT, `overview_${String(offset / perSheet + 1).padStart(2, '0')}.png`),
      canvas.toBuffer('image/png'),
    );
  }
}

async function main() {
  if (!fs.existsSync(ATLAS_MANIFEST_PATH)) throw new Error('Visual atlas manifest missing');
  const atlasManifestRaw = fs.readFileSync(ATLAS_MANIFEST_PATH, 'utf8');
  const atlasManifest = JSON.parse(atlasManifestRaw);
  if (!atlasManifest.completedAt) throw new Error('Visual atlas is incomplete; rebuild sheets after atlas completion');
  const directories = modelDirectories(FRAMES_ROOT);
  fs.mkdirSync(OUTPUT_ROOT, { recursive: true });
  for (const [index, directory] of directories.entries()) {
    const output = path.join(OUTPUT_ROOT, path.basename(directory));
    fs.mkdirSync(output, { recursive: true });
    const files = imageFiles(directory);
    const animations = animationNames(files);
    await staticSheet(directory, output);
    await animationSheets(directory, output, animations);
    if ((index + 1) % 25 === 0 || index + 1 === directories.length) {
      console.log(`MODEL_REVIEW_SHEETS_PROGRESS: ${index + 1}/${directories.length}`);
    }
  }
  await overviewSheets(directories);
  const manifest = {
    schemaVersion: 2,
    generatedAt: new Date().toISOString(),
    models: directories.length,
    overviewSheets: Math.ceil(directories.length / 25),
    source: FRAMES_ROOT,
    output: OUTPUT_ROOT,
    sourceAtlasManifest: ATLAS_MANIFEST_PATH,
    sourceAtlasCompletedAt: atlasManifest.completedAt,
    sourceRenderContractVersion: atlasManifest.renderContract?.version || null,
    sourceAtlasHash: sha1Text(atlasManifestRaw),
    sourceQualityStatus: atlasManifest.qualityStatus || null,
    sourceFallbackFrames: atlasManifest.fallbackFrames || 0,
  };
  fs.writeFileSync(path.join(OUTPUT_ROOT, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
  console.log(`MODEL_REVIEW_SHEETS_PASS: ${directories.length} models`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
