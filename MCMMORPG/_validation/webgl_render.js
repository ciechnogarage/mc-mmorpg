const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');
const { createCanvas, Image } = require('canvas');
const { collect, cubeVertices, animationPose, FACES, FACE_NAMES } = require('./render_bbmodel_review.js');
const { logRender } = require('./lib/reference_clone');

function arg(name, fallback = null) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 ? process.argv[index + 1] : fallback;
}

function imageSize(dataUri) {
  const img = new Image();
  img.src = dataUri;
  return { width: img.width, height: img.height };
}

function buildSpec(model, options) {
  const { elements, bones, assignments } = collect(model);
  const animation = (model.animations || []).find((candidate) => candidate.name === options.animation);
  const pose = animationPose(animation, options.time || 0);

  const textureSizes = (model.textures || []).map((texture) => (texture.source ? imageSize(texture.source) : null));
  const groups = new Map(); // key: textureIndex or 'none' -> { positions, uvs, indices, textureIndex }

  function groupFor(textureIndex) {
    const key = textureIndex === null ? 'none' : String(textureIndex);
    if (!groups.has(key)) {
      groups.set(key, { positions: [], uvs: [], indices: [], textureIndex, vertexCount: 0 });
    }
    return groups.get(key);
  }

  for (const element of elements.values()) {
    const boneId = assignments.get(element.uuid);
    const bone = bones.get(boneId);
    const hitbox = bone?.name === 'hitbox'
      || element.name === 'hitbox'
      || bone?.name?.startsWith('ob_')
      || element.name?.startsWith('damage_zone_');
    if (hitbox && !options.hitbox) continue;
    if (!hitbox && options.hitboxOnly) continue;

    const vertices = cubeVertices(element, boneId, bones, pose);
    for (let index = 0; index < FACES.length; index++) {
      const faceName = FACE_NAMES[index];
      const face = element.faces?.[faceName];
      if (!face?.uv) continue;
      const quad = FACES[index].map((v) => vertices[v]);
      const textureIndex = hitbox ? null : Number(String(face.texture ?? 0).replace('#', ''));
      const size = textureIndex !== null ? textureSizes[textureIndex] : null;
      const group = groupFor(size ? textureIndex : null);
      const [u1, v1, u2, v2] = face.uv.map(Number);
      const uvCorners = size
        ? [[u1 / size.width, v1 / size.height], [u2 / size.width, v1 / size.height],
          [u2 / size.width, v2 / size.height], [u1 / size.width, v2 / size.height]]
        : [[0, 0], [1, 0], [1, 1], [0, 1]];
      const base = group.vertexCount;
      for (let i = 0; i < 4; i++) {
        group.positions.push(...quad[i]);
        group.uvs.push(...uvCorners[i]);
      }
      group.indices.push(base, base + 1, base + 2, base, base + 2, base + 3);
      group.vertexCount += 4;
      if (hitbox) group.fallbackColor = 0x43ff71;
    }
  }

  const textures = (model.textures || [])
    .map((texture, index) => ({ index, dataUri: texture.source }))
    .filter((t) => t.dataUri && [...groups.values()].some((g) => g.textureIndex === t.index));

  return {
    size: Number(options.size || 720),
    yaw: Number(options.yaw ?? 35),
    pitch: Number(options.pitch ?? -15),
    textures,
    groups: [...groups.values()],
  };
}

async function main() {
  const modelPath = arg('model');
  const outPath = arg('out');
  if (!modelPath || !outPath) {
    console.error('Usage: node webgl_render.js --model <path.bbmodel> --out <path.png> [--yaw N] [--pitch N] [--size N] [--animation name] [--time t] [--hitbox] [--hitboxOnly]');
    process.exit(1);
  }
  const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
  const options = {
    yaw: arg('yaw'),
    pitch: arg('pitch'),
    size: arg('size'),
    animation: arg('animation'),
    time: arg('time'),
    hitbox: process.argv.includes('--hitbox'),
    hitboxOnly: process.argv.includes('--hitboxOnly'),
  };
  const spec = buildSpec(model, options);

  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'],
  });
  try {
    const maxAttempts = 5;
    let lastError = null;
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      const page = await browser.newPage();
      try {
        page.on('pageerror', (err) => console.error('[pageerror]', err.message));
        await page.setViewport({ width: spec.size, height: spec.size });
        await page.goto('file://' + path.join(__dirname, 'webgl_render.html'));
        const result = await page.evaluate((s) => window.renderSpec(s), spec);
        if (result !== 'ok') throw new Error('render failed: ' + result);
        const buffer = await page.screenshot();
        if (isBlankScreenshot(buffer)) {
          lastError = new Error('screenshot came back blank (SwiftShader validate-program race)');
          continue;
        }
        fs.writeFileSync(outPath, buffer);
        logRender(modelPath, (model.elements || []).length, { tool: 'webgl_render', out: outPath });
        console.log('RESULT: wrote', outPath, `(${spec.groups.length} groups, ${spec.textures.length} textures, attempt ${attempt})`);
        return;
      } finally {
        await page.close();
      }
    }
    throw lastError || new Error('render failed after retries');
  } finally {
    await browser.close();
  }
}

function isBlankScreenshot(buffer) {
  const img = new Image();
  img.src = buffer;
  const canvas = createCanvas(img.width, img.height);
  const context = canvas.getContext('2d');
  context.drawImage(img, 0, 0);
  const { data } = context.getImageData(0, 0, img.width, img.height);
  const first = [data[0], data[1], data[2]];
  let distinct = 0;
  for (let i = 0; i < data.length; i += 4 * 97) {
    if (data[i] !== first[0] || data[i + 1] !== first[1] || data[i + 2] !== first[2]) {
      distinct++;
      if (distinct > 20) return false;
    }
  }
  return true;
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
