const fs = require('fs');
const path = require('path');
const { pipeline } = require('stream/promises');
const { createCanvas, Image } = require('canvas');

const ROOT = path.resolve(__dirname, '..');
const OUTPUT_ROOT = path.join(__dirname, 'model_reviews');
const TEXTURE_CACHE = new WeakMap();

function arg(name, fallback = null) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 ? process.argv[index + 1] : fallback;
}

function radians(value) {
  return Number(value || 0) * Math.PI / 180;
}

function add(a, b) {
  return a.map((value, index) => value + b[index]);
}

function sub(a, b) {
  return a.map((value, index) => value - b[index]);
}

function rotate(point, rotation) {
  let [x, y, z] = point;
  const [rx, ry, rz] = rotation.map(radians);
  [y, z] = [y * Math.cos(rx) - z * Math.sin(rx), y * Math.sin(rx) + z * Math.cos(rx)];
  [x, z] = [x * Math.cos(ry) + z * Math.sin(ry), -x * Math.sin(ry) + z * Math.cos(ry)];
  [x, y] = [x * Math.cos(rz) - y * Math.sin(rz), x * Math.sin(rz) + y * Math.cos(rz)];
  return [x, y, z];
}

function around(point, origin, rotation) {
  return add(rotate(sub(point, origin), rotation), origin);
}

function interpolateFrames(frames, time) {
  if (!frames.length) return [0, 0, 0];
  const sorted = [...frames].sort((a, b) => Number(a.time) - Number(b.time));
  const before = [...sorted].reverse().find((frame) => Number(frame.time) <= time) || sorted[0];
  const after = sorted.find((frame) => Number(frame.time) >= time) || sorted.at(-1);
  const a = before.data_points?.[0] || {};
  const b = after.data_points?.[0] || {};
  if (before === after) return [Number(a.x || 0), Number(a.y || 0), Number(a.z || 0)];
  const span = Number(after.time) - Number(before.time);
  const amount = span ? (time - Number(before.time)) / span : 0;
  return ['x', 'y', 'z'].map((axis) => Number(a[axis] || 0) + (Number(b[axis] || 0) - Number(a[axis] || 0)) * amount);
}

function animationPose(animation, time) {
  const pose = new Map();
  if (!animation) return pose;
  for (const [uuid, animator] of Object.entries(animation.animators || {})) {
    const keyframes = animator.keyframes || [];
    pose.set(uuid, {
      rotation: interpolateFrames(keyframes.filter((frame) => frame.channel === 'rotation'), time),
      position: interpolateFrames(keyframes.filter((frame) => frame.channel === 'position'), time),
    });
  }
  return pose;
}

function collect(model) {
  const elements = new Map((model.elements || []).map((element) => [element.uuid, element]));
  const bones = new Map();
  const assignments = new Map();

  function walk(nodes, parent = null) {
    for (const node of nodes || []) {
      if (!node || typeof node !== 'object' || Array.isArray(node)) continue;
      bones.set(node.uuid, { ...node, parent });
      for (const child of node.children || []) {
        if (typeof child === 'string') assignments.set(child, node.uuid);
      }
      walk(node.children, node.uuid);
    }
  }
  walk(model.outliner || []);
  return { elements, bones, assignments };
}

function cubeVertices(element, boneId, bones, pose) {
  const [x1, y1, z1] = element.from;
  const [x2, y2, z2] = element.to;
  let vertices = [
    [x1, y1, z1], [x2, y1, z1], [x2, y2, z1], [x1, y2, z1],
    [x1, y1, z2], [x2, y1, z2], [x2, y2, z2], [x1, y2, z2],
  ];
  if (element.rotation?.some(Number)) {
    vertices = vertices.map((vertex) => around(vertex, element.origin || [0, 0, 0], element.rotation));
  }
  let current = boneId;
  while (current) {
    const bone = bones.get(current);
    if (!bone) break;
    const animated = pose.get(current) || { rotation: [0, 0, 0], position: [0, 0, 0] };
    const rotation = add(bone.rotation || [0, 0, 0], animated.rotation);
    const origin = bone.origin || [0, 0, 0];
    vertices = vertices.map((vertex) => add(around(vertex, origin, rotation), animated.position));
    current = bone.parent;
  }
  return vertices;
}

function bonePoint(boneId, bones, pose) {
  const bone = bones.get(boneId);
  if (!bone) return [0, 0, 0];
  let point = add(bone.origin || [0, 0, 0], (pose.get(boneId) || {}).position || [0, 0, 0]);
  let current = bone.parent;
  while (current) {
    const parent = bones.get(current);
    if (!parent) break;
    const animated = pose.get(current) || { rotation: [0, 0, 0], position: [0, 0, 0] };
    point = add(
      around(point, parent.origin || [0, 0, 0], add(parent.rotation || [0, 0, 0], animated.rotation)),
      animated.position,
    );
    current = parent.parent;
  }
  return point;
}

const FACES = [
  [0, 1, 2, 3],
  [5, 4, 7, 6],
  [4, 0, 3, 7],
  [1, 5, 6, 2],
  [3, 2, 6, 7],
  [4, 5, 1, 0],
];
const FACE_NAMES = ['north', 'south', 'west', 'east', 'up', 'down'];

function project(point, yaw, pitch) {
  const viewed = rotate(point, [pitch, yaw, 0]);
  return { x: viewed[0], y: -viewed[1], depth: viewed[2] };
}

function buildTextureContexts(model) {
  if (TEXTURE_CACHE.has(model)) return TEXTURE_CACHE.get(model);
  const contexts = (model.textures || []).map((texture) => {
    if (!texture.source) return null;
    try {
      const image = new Image();
      image.src = texture.source;
      const canvas = createCanvas(image.width, image.height);
      const context = canvas.getContext('2d');
      context.drawImage(image, 0, 0);
      return { canvas, context, width: image.width, height: image.height, renderMode: texture.render_mode || 'default' };
    } catch {
      return null;
    }
  });
  TEXTURE_CACHE.set(model, contexts);
  return contexts;
}

function releaseModelTextures(model) {
  const textures = TEXTURE_CACHE.get(model) || [];
  for (const texture of textures) {
    if (!texture?.canvas) continue;
    texture.canvas.width = 0;
    texture.canvas.height = 0;
  }
  TEXTURE_CACHE.delete(model);
}

function faceTexture(element, faceName, textures) {
  const face = element.faces?.[faceName];
  if (!face?.uv) return null;
  const textureIndex = Number(String(face.texture ?? 0).replace('#', ''));
  const texture = textures[textureIndex];
  if (!texture) return null;
  const [u1, v1, u2, v2] = face.uv.map(Number);
  return {
    texture,
    uv: [[u1, v1], [u2, v1], [u2, v2], [u1, v2]],
  };
}

function drawTexturedTriangle(context, image, source, target) {
  const [[u0, v0], [u1, v1], [u2, v2]] = source;
  const [[x0, y0], [x1, y1], [x2, y2]] = target;
  const minU = Math.floor(Math.min(u0, u1, u2));
  const minV = Math.floor(Math.min(v0, v1, v2));
  const maxU = Math.ceil(Math.max(u0, u1, u2));
  const maxV = Math.ceil(Math.max(v0, v1, v2));
  const cropX = Math.max(0, minU);
  const cropY = Math.max(0, minV);
  const cropWidth = Math.max(1, Math.min(image.width - cropX, maxU - cropX));
  const cropHeight = Math.max(1, Math.min(image.height - cropY, maxV - cropY));
  const patchCanvas = createCanvas(cropWidth, cropHeight);
  const patchContext = patchCanvas.getContext('2d');
  patchContext.drawImage(image, cropX, cropY, cropWidth, cropHeight, 0, 0, cropWidth, cropHeight);
  const patchSource = source.map(([u, v]) => [u - cropX, v - cropY]);
  const determinant = u0 * (v1 - v2) + u1 * (v2 - v0) + u2 * (v0 - v1);
  if (Math.abs(determinant) < 0.000001) {
    patchCanvas.width = 0;
    patchCanvas.height = 0;
    return false;
  }
  const [[pu0, pv0], [pu1, pv1], [pu2, pv2]] = patchSource;
  const patchDeterminant = pu0 * (pv1 - pv2) + pu1 * (pv2 - pv0) + pu2 * (pv0 - pv1);
  if (Math.abs(patchDeterminant) < 0.000001) {
    patchCanvas.width = 0;
    patchCanvas.height = 0;
    return false;
  }
  const a = (x0 * (pv1 - pv2) + x1 * (pv2 - pv0) + x2 * (pv0 - pv1)) / patchDeterminant;
  const c = (x0 * (pu2 - pu1) + x1 * (pu0 - pu2) + x2 * (pu1 - pu0)) / patchDeterminant;
  const e = (
    x0 * (pu1 * pv2 - pu2 * pv1) +
    x1 * (pu2 * pv0 - pu0 * pv2) +
    x2 * (pu0 * pv1 - pu1 * pv0)
  ) / patchDeterminant;
  const b = (y0 * (pv1 - pv2) + y1 * (pv2 - pv0) + y2 * (pv0 - pv1)) / patchDeterminant;
  const d = (y0 * (pu2 - pu1) + y1 * (pu0 - pu2) + y2 * (pu1 - pu0)) / patchDeterminant;
  const f = (
    y0 * (pu1 * pv2 - pu2 * pv1) +
    y1 * (pu2 * pv0 - pu0 * pv2) +
    y2 * (pu0 * pv1 - pu1 * pv0)
  ) / patchDeterminant;
  const transform = [a, b, c, d, e, f];
  if (
    !transform.every(Number.isFinite) ||
    [a, b, c, d].some((value) => Math.abs(value) > 64) ||
    [e, f].some((value) => Math.abs(value) > 10000)
  ) {
    patchCanvas.width = 0;
    patchCanvas.height = 0;
    return false;
  }
  context.save();
  context.beginPath();
  context.moveTo(x0, y0);
  context.lineTo(x1, y1);
  context.lineTo(x2, y2);
  context.closePath();
  context.clip();
  try {
    context.setTransform(a, b, c, d, e, f);
    context.drawImage(patchCanvas, 0, 0);
    return true;
  } finally {
    context.restore();
    patchCanvas.width = 0;
    patchCanvas.height = 0;
  }
}

function drawTexturedFace(context, image, source, target) {
  const minU = Math.max(0, Math.floor(Math.min(...source.map(([u]) => u))));
  const minV = Math.max(0, Math.floor(Math.min(...source.map(([, v]) => v))));
  const maxU = Math.min(image.width, Math.ceil(Math.max(...source.map(([u]) => u))));
  const maxV = Math.min(image.height, Math.ceil(Math.max(...source.map(([, v]) => v))));
  const minX = Math.min(...target.map(([x]) => x));
  const minY = Math.min(...target.map(([, y]) => y));
  const maxX = Math.max(...target.map(([x]) => x));
  const maxY = Math.max(...target.map(([, y]) => y));
  const sourceWidth = maxU - minU;
  const sourceHeight = maxV - minV;
  const targetWidth = maxX - minX;
  const targetHeight = maxY - minY;
  if ([sourceWidth, sourceHeight, targetWidth, targetHeight].some((value) => !Number.isFinite(value) || value <= 0)) {
    return false;
  }
  context.save();
  context.beginPath();
  context.moveTo(target[0][0], target[0][1]);
  for (const [x, y] of target.slice(1)) context.lineTo(x, y);
  context.closePath();
  context.clip();
  context.drawImage(
    image,
    minU,
    minV,
    sourceWidth,
    sourceHeight,
    minX,
    minY,
    targetWidth,
    targetHeight,
  );
  context.restore();
  return true;
}

function faceColor(element, faceName, textures, fallback = '#70766f') {
  const face = element.faces?.[faceName];
  if (!face?.uv) return fallback;
  const rawTexture = face.texture;
  const textureIndex = Number(String(rawTexture ?? 0).replace('#', ''));
  const texture = textures[textureIndex];
  if (!texture) return fallback;
  const [u1, v1, u2, v2] = face.uv.map(Number);
  const x = Math.max(0, Math.min(texture.width - 1, Math.floor(Math.min(u1, u2))));
  const y = Math.max(0, Math.min(texture.height - 1, Math.floor(Math.min(v1, v2))));
  const width = Math.max(1, Math.min(texture.width - x, Math.ceil(Math.abs(u2 - u1))));
  const height = Math.max(1, Math.min(texture.height - y, Math.ceil(Math.abs(v2 - v1))));
  const pixels = texture.context.getImageData(x, y, width, height).data;
  let red = 0;
  let green = 0;
  let blue = 0;
  let weight = 0;
  for (let index = 0; index < pixels.length; index += 4) {
    const alpha = pixels[index + 3] / 255;
    red += pixels[index] * alpha;
    green += pixels[index + 1] * alpha;
    blue += pixels[index + 2] * alpha;
    weight += alpha;
  }
  if (!weight) return fallback;
  const boost = texture.renderMode === 'emissive' ? 1.3 : 1;
  const channel = (value) => Math.max(0, Math.min(255, Math.round(value / weight * boost)));
  return `rgb(${channel(red)},${channel(green)},${channel(blue)})`;
}

function shade(hex, amount) {
  const rgb = /^rgb\((\d+),(\d+),(\d+)\)$/.exec(hex);
  const value = rgb ? null : Number.parseInt(hex.slice(1), 16);
  const source = rgb ? rgb.slice(1).map(Number) : [value >> 16, (value >> 8) & 255, value & 255];
  const channels = source
    .map((channel) => Math.max(0, Math.min(255, Math.round(channel * amount))));
  return `rgb(${channels.join(',')})`;
}

async function render(model, output, options) {
  const width = Number(options.size || 720);
  const height = width;
  const canvas = createCanvas(width, height);
  const context = canvas.getContext('2d');
  const { elements, bones, assignments } = collect(model);
  const animation = (model.animations || []).find((candidate) => candidate.name === options.animation);
  const pose = animationPose(animation, options.time || 0);
  const shapes = [];
  const points = [];
  const helperPoints = [];
  const textures = buildTextureContexts(model);
  const basePoints = [];

  if (options.stableScale) {
    const basePose = new Map();
    for (const element of elements.values()) {
      const bone = bones.get(assignments.get(element.uuid));
      if (
        bone?.name === 'hitbox'
        || element.name === 'hitbox'
        || bone?.name?.startsWith('ob_')
        || element.name?.startsWith('damage_zone_')
      ) continue;
      basePoints.push(...cubeVertices(element, assignments.get(element.uuid), bones, basePose)
        .map((vertex) => project(vertex, options.yaw, options.pitch)));
    }
  }

  for (const element of elements.values()) {
    const bone = bones.get(assignments.get(element.uuid));
    const hitbox = bone?.name === 'hitbox'
      || element.name === 'hitbox'
      || bone?.name?.startsWith('ob_')
      || element.name?.startsWith('damage_zone_');
    if (hitbox && !options.hitbox) continue;
    if (!hitbox && options.hitboxOnly) continue;
    const vertices = cubeVertices(element, assignments.get(element.uuid), bones, pose);
    const projected = vertices.map((vertex) => project(vertex, options.yaw, options.pitch));
    points.push(...projected);
    for (let index = 0; index < FACES.length; index++) {
      const face = FACES[index].map((vertex) => projected[vertex]);
      shapes.push({
        face,
        depth: face.reduce((sum, point) => sum + point.depth, 0) / face.length,
        color: options.silhouette ? '#101411' : hitbox ? '#43ff71' : faceColor(element, FACE_NAMES[index], textures),
        textureFace: faceTexture(element, FACE_NAMES[index], textures),
        alpha: hitbox ? 0.2 : 1,
        shade: [0.72, 0.82, 0.9, 0.68, 1.05, 0.62][index],
        hitbox,
      });
    }
  }

  if (options.helpers) {
    for (const [boneId, bone] of bones) {
      if ((bone.children || []).some((child) => typeof child === 'string')) continue;
      const projected = project(bonePoint(boneId, bones, pose), options.yaw, options.pitch);
      helperPoints.push({ ...projected, name: bone.name });
      points.push(projected);
    }
  }

  const framingPoints = basePoints.length ? basePoints : points;
  let minX = Math.min(...framingPoints.map((point) => point.x), -10);
  let maxX = Math.max(...framingPoints.map((point) => point.x), 10);
  let minY = Math.min(...framingPoints.map((point) => point.y), -10);
  let maxY = Math.max(...framingPoints.map((point) => point.y), 10);
  if (options.stableScale) {
    const padX = (maxX - minX) * 0.15;
    const padY = (maxY - minY) * 0.15;
    minX -= padX;
    maxX += padX;
    minY -= padY;
    maxY += padY;
  }
  const scale = Number(options.fixedScale) || Math.min(width * 0.78 / (maxX - minX), height * 0.78 / (maxY - minY));
  const centerX = Number.isFinite(Number(options.fixedCenterX))
    ? Number(options.fixedCenterX)
    : (minX + maxX) / 2;
  const centerY = (minY + maxY) / 2;
  const floorY = maxY;

  context.fillStyle = '#111714';
  context.fillRect(0, 0, width, height);
  context.strokeStyle = '#24322a';
  context.lineWidth = 1;
  for (let x = width * 0.06; x < width; x += Math.max(20, width / 18)) {
    context.beginPath();
    context.moveTo(x, height * 0.1);
    context.lineTo(x, height * 0.9);
    context.stroke();
  }
  for (let y = height * 0.12; y < height * 0.9; y += Math.max(20, height / 18)) {
    context.beginPath();
    context.moveTo(width * 0.06, y);
    context.lineTo(width * 0.94, y);
    context.stroke();
  }

  const map = (point) => ({
    x: width / 2 + (point.x - centerX) * scale,
    y: options.stableGround
      ? height * 0.84 + (point.y - floorY) * scale
      : height / 2 + (point.y - centerY) * scale,
  });
  shapes.sort((a, b) => b.depth - a.depth);
  for (const shape of shapes) {
    const face = shape.face.map(map);
    context.beginPath();
    context.moveTo(face[0].x, face[0].y);
    for (const point of face.slice(1)) context.lineTo(point.x, point.y);
    context.closePath();
    context.globalAlpha = shape.alpha;
    if (shape.textureFace && !shape.hitbox && !options.silhouette && options.textureMode !== 'average') {
      let mapped = false;
      try {
        mapped = drawTexturedFace(
          context,
          shape.textureFace.texture.canvas,
          shape.textureFace.uv,
          face.map((point) => [point.x, point.y]),
        );
      } catch {
        mapped = false;
      }
      if (mapped) {
        if (shape.shade < 1) {
          context.fillStyle = `rgba(0,0,0,${Math.min(0.55, 1 - shape.shade)})`;
          context.fill();
        }
      } else {
        context.fillStyle = shade(shape.color, shape.shade);
        context.fill();
      }
    } else {
      context.fillStyle = shape.hitbox ? shape.color : shade(shape.color, shape.shade);
      context.fill();
    }
    context.strokeStyle = shape.hitbox ? '#75ff97' : options.silhouette ? '#101411' : '#1b211d';
    context.lineWidth = shape.hitbox ? 2 : 1;
    context.stroke();
  }
  context.globalAlpha = 1;

  for (const helper of helperPoints) {
    const point = map(helper);
    context.fillStyle = options.interactionBones?.has(helper.name) ? '#ffcf45' : '#5cc8ff';
    context.beginPath();
    context.arc(point.x, point.y, 5, 0, Math.PI * 2);
    context.fill();
    context.strokeStyle = '#08100b';
    context.stroke();
    context.fillStyle = '#eef4ef';
    context.font = '13px sans-serif';
    context.fillText(helper.name, point.x + 8, point.y - 7);
  }

  if (options.playerScale) {
    const playerHeight = 32 * scale;
    const baseline = options.stableGround
      ? height * 0.84 + (0 - floorY) * scale
      : height / 2 + (0 - (-centerY)) * scale;
    context.fillStyle = '#d8c89a';
    context.fillRect(55, baseline - playerHeight, 10 * scale, playerHeight);
    context.fillStyle = '#f3e6bd';
    context.fillRect(55, baseline - playerHeight, 10 * scale, 10 * scale);
    context.fillStyle = '#dce9e1';
    context.font = '16px sans-serif';
    context.fillText('player 2 blocks', 48, baseline + 24);
  }

  context.fillStyle = '#eef4ef';
  context.font = `bold ${Math.max(12, width / 30)}px sans-serif`;
  context.fillText(options.title, width * 0.04, height * 0.055);
  context.fillStyle = '#9fb2a5';
  context.font = `${Math.max(9, width / 45)}px sans-serif`;
  context.fillText(`${model.name || 'model'} | yaw ${options.yaw} | pitch ${options.pitch}`, width * 0.04, height * 0.086);
  const exportCanvas = createCanvas(width, height);
  const exportContext = exportCanvas.getContext('2d');
  exportContext.drawImage(canvas, 0, 0);
  try {
    await pipeline(exportCanvas.createPNGStream(), fs.createWriteStream(output));
  } finally {
    exportCanvas.width = 0;
    exportCanvas.height = 0;
    canvas.width = 0;
    canvas.height = 0;
  }
}

async function main() {
  const modelId = arg('model');
  const explicitBlueprint = arg('blueprint');
  if (!modelId && !explicitBlueprint) throw new Error('Usage: node render_bbmodel_review.js --model <model_id> | --blueprint <file.bbmodel>');
  const blueprint = explicitBlueprint
    ? path.resolve(explicitBlueprint)
    : path.join(ROOT, 'plugins', 'ModelEngine', 'blueprints', modelId, `${modelId}.bbmodel`);
  if (!fs.existsSync(blueprint)) throw new Error(`Blueprint not found: ${blueprint}`);
  const model = JSON.parse(fs.readFileSync(blueprint, 'utf8'));
  const resolvedModelId = modelId || model.name || path.basename(blueprint, '.bbmodel');
  const outputDir = path.resolve(arg('output', path.join(OUTPUT_ROOT, resolvedModelId)));
  fs.mkdirSync(outputDir, { recursive: true });
  const manifestPath = modelId
    ? path.join(__dirname, 'model_quality', `${resolvedModelId}.quality.json`)
    : blueprint.replace(/\.bbmodel$/, '.quality.json');
  const manifest = fs.existsSync(manifestPath) ? JSON.parse(fs.readFileSync(manifestPath, 'utf8')) : {};
  const interactionBones = new Set(manifest.interactionBones || []);

  const size = Number(arg('size', 720));
  const textureMode = arg('texture-mode', 'mapped');
  const fixedScale = arg('fixed-scale') ? Number(arg('fixed-scale')) : undefined;
  const fixedCenterX = arg('fixed-center-x') ? Number(arg('fixed-center-x')) : undefined;
  const views = [
    ['front', 180, -8],
    ['side', 90, -8],
    ['back', 0, -8],
    ['three_quarter', 35, -10],
  ];
  for (const [name, yaw, pitch] of views) {
    await render(model, path.join(outputDir, `${name}.png`), {
      title: name,
      yaw,
      pitch,
      size,
      textureMode,
      fixedScale,
      fixedCenterX,
      stableScale: true,
      stableGround: true,
    });
  }
  await render(model, path.join(outputDir, 'silhouette.png'), {
    title: 'silhouette',
    yaw: 35,
    pitch: -10,
    silhouette: true,
    size,
    textureMode,
    stableScale: true,
    stableGround: true,
  });
  await render(model, path.join(outputDir, 'player_scale.png'), {
    title: 'player scale',
    yaw: 0,
    pitch: -8,
    playerScale: true,
    size,
    textureMode,
    stableScale: true,
    stableGround: true,
  });
  await render(model, path.join(outputDir, 'hitbox.png'), {
    title: 'hitbox debug',
    yaw: 35,
    pitch: -10,
    hitbox: true,
    size,
    textureMode,
    stableScale: true,
    stableGround: true,
  });
  await render(model, path.join(outputDir, 'helpers.png'), {
    title: 'gameplay helpers',
    yaw: 35,
    pitch: -10,
    helpers: true,
    interactionBones,
    size,
    textureMode,
    stableScale: true,
    stableGround: true,
  });

  for (const animation of model.animations || []) {
    const time = Number(animation.length || 0) * 0.55;
    await render(model, path.join(outputDir, `animation_${animation.name}.png`), {
      title: `${animation.name} @ ${time.toFixed(2)}s`,
      yaw: 35,
      pitch: -10,
      animation: animation.name,
      time,
      size,
      textureMode,
      stableScale: true,
      stableGround: true,
    });

    const phaseDirectory = path.join(outputDir, 'animation_phases', animation.name);
    fs.mkdirSync(phaseDirectory, { recursive: true });
    for (const [phase, fraction] of [
      ['start', 0],
      ['anticipation', 0.25],
      ['impact', 0.55],
      ['recovery', 0.78],
      ['end', 1],
    ]) {
      const phaseTime = Number(animation.length || 0) * fraction;
      await render(model, path.join(phaseDirectory, `${phase}.png`), {
        title: `${animation.name}: ${phase} @ ${phaseTime.toFixed(2)}s`,
        yaw: 35,
        pitch: -10,
        animation: animation.name,
        time: phaseTime,
        size,
        textureMode,
        stableScale: true,
        stableGround: true,
      });
    }
  }
  console.log(`MODELENGINE_RENDER_PASS: ${outputDir}`);
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error);
    process.exit(1);
  });
}

module.exports = { render, releaseModelTextures, collect, cubeVertices, animationPose, FACES, FACE_NAMES };
