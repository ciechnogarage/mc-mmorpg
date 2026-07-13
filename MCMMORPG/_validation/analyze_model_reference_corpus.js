const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');
const { createCanvas, Image } = require('canvas');
const { collectAnimationFrames, deriveAnimationPhases } = require('./modelengine_phase_utils');

const DEFAULT_CORPUS = '/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)';
const ROOT = path.resolve(__dirname, '..');
const OUTPUT_DIR = path.join(__dirname, 'reference_corpus');
const REPORT_PATH = path.resolve(ROOT, '..', 'docs', 'ai', 'modelengine-reference-corpus.md');
const LEARNING_PATH = path.resolve(ROOT, '..', 'docs', 'ai', 'modelengine-learning-ledger.md');
const ATLAS_PATH = path.resolve(ROOT, '..', 'docs', 'ai', 'modelengine-creation-atlas.md');

function arg(name, fallback = null) {
  const index = process.argv.indexOf(`--${name}`);
  return index >= 0 ? process.argv[index + 1] : fallback;
}

function sha1(filePath) {
  return crypto.createHash('sha1').update(fs.readFileSync(filePath)).digest('hex');
}

function walk(root, acc = []) {
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) walk(full, acc);
    else if (entry.isFile()) acc.push(full);
  }
  return acc;
}

function flattenBones(nodes, parent = null, depth = 0, acc = []) {
  for (const node of nodes || []) {
    if (!node || typeof node !== 'object' || Array.isArray(node)) continue;
    acc.push({
      uuid: node.uuid,
      name: node.name,
      parent,
      depth,
      origin: node.origin,
      rotation: node.rotation || [0, 0, 0],
      visible: node.visibility !== false,
      cubes: (node.children || []).filter((child) => typeof child === 'string').length,
    });
    flattenBones(node.children, node.name, depth + 1, acc);
  }
  return acc;
}

function pngMetadata(filePath) {
  const buffer = fs.readFileSync(filePath);
  if (buffer.length < 24 || buffer.toString('ascii', 1, 4) !== 'PNG') return null;
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) };
}

function audioMetadata(filePath) {
  const result = spawnSync(
    'ffprobe',
    ['-v', 'error', '-show_entries', 'format=duration:stream=codec_name,sample_rate,channels', '-of', 'json', filePath],
    { encoding: 'utf8', timeout: 5000 },
  );
  if (result.status !== 0) return { error: String(result.stderr || 'ffprobe failed').trim() };
  const parsed = JSON.parse(result.stdout);
  const stream = parsed.streams?.[0] || {};
  return {
    duration: Number(parsed.format?.duration || 0),
    codec: stream.codec_name || null,
    sampleRate: Number(stream.sample_rate || 0),
    channels: Number(stream.channels || 0),
  };
}

function textureMetrics(texture) {
  if (!texture?.source) return null;
  try {
    const image = new Image();
    image.src = texture.source;
    const width = Math.min(image.width, 64);
    const height = Math.min(image.height, 64);
    const canvas = createCanvas(width, height);
    const context = canvas.getContext('2d');
    context.drawImage(image, 0, 0, width, height);
    const pixels = context.getImageData(0, 0, width, height).data;
    const buckets = new Map();
    let opaque = 0;
    let luminanceSum = 0;
    let saturationSum = 0;
    let luminanceSquared = 0;
    for (let index = 0; index < pixels.length; index += 4) {
      const alpha = pixels[index + 3];
      if (alpha < 16) continue;
      const r = pixels[index];
      const g = pixels[index + 1];
      const b = pixels[index + 2];
      const max = Math.max(r, g, b);
      const min = Math.min(r, g, b);
      const luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
      const saturation = max === 0 ? 0 : (max - min) / max;
      const key = [r, g, b].map((value) => Math.round(value / 32) * 32).join(',');
      buckets.set(key, (buckets.get(key) || 0) + 1);
      opaque += 1;
      luminanceSum += luminance;
      saturationSum += saturation;
      luminanceSquared += luminance * luminance;
    }
    const luminance = opaque ? luminanceSum / opaque : 0;
    return {
      width: image.width,
      height: image.height,
      opaqueRatio: Number((opaque / (width * height)).toFixed(4)),
      luminance: Number(luminance.toFixed(4)),
      saturation: Number((opaque ? saturationSum / opaque : 0).toFixed(4)),
      contrast: Number(Math.sqrt(Math.max(0, opaque ? luminanceSquared / opaque - luminance * luminance : 0)).toFixed(4)),
      palette: [...buckets.entries()]
        .sort((a, b) => b[1] - a[1])
        .slice(0, 8)
        .map(([rgb, count]) => ({ rgb: rgb.split(',').map(Number), share: Number((count / opaque).toFixed(4)) })),
    };
  } catch (error) {
    return { error: error.message };
  }
}

function geometryMetrics(elements, bounds) {
  const cubes = elements.filter((element) => Array.isArray(element.from) && Array.isArray(element.to) && element.name !== 'hitbox');
  if (!cubes.length || !bounds) return null;
  const records = cubes.map((element) => {
    const size = element.to.map((value, axis) => Math.abs(value - element.from[axis]));
    const center = element.to.map((value, axis) => (value + element.from[axis]) / 2);
    return {
      name: element.name || 'cube',
      size,
      center,
      volume: size.reduce((product, value) => product * Math.max(value, 0.001), 1),
      rotated: Boolean(element.rotation?.some((value) => Number(value) !== 0)),
    };
  });
  const totalVolume = records.reduce((sum, item) => sum + item.volume, 0);
  const sortedVolumes = records.map((item) => item.volume).sort((a, b) => b - a);
  const modelCenterX = (bounds.min[0] + bounds.max[0]) / 2;
  const tolerance = Math.max(0.75, bounds.size[0] * 0.035);
  let mirrored = 0;
  for (const item of records) {
    const expectedX = modelCenterX * 2 - item.center[0];
    if (records.some((candidate) =>
      Math.abs(candidate.center[0] - expectedX) <= tolerance &&
      Math.abs(candidate.center[1] - item.center[1]) <= tolerance &&
      Math.abs(candidate.center[2] - item.center[2]) <= tolerance &&
      candidate.size.every((value, axis) => Math.abs(value - item.size[axis]) <= tolerance))) mirrored += 1;
  }
  const verticalThird = bounds.size[1] / 3 || 1;
  const massByHeight = [0, 0, 0];
  for (const item of records) {
    const band = Math.max(0, Math.min(2, Math.floor((item.center[1] - bounds.min[1]) / verticalThird)));
    massByHeight[band] += item.volume;
  }
  const normalize = (values) => values.map((value) => Number((value / totalVolume).toFixed(4)));
  return {
    proportions: {
      widthToHeight: Number((bounds.size[0] / Math.max(bounds.size[1], 0.001)).toFixed(4)),
      depthToHeight: Number((bounds.size[2] / Math.max(bounds.size[1], 0.001)).toFixed(4)),
      widthToDepth: Number((bounds.size[0] / Math.max(bounds.size[2], 0.001)).toFixed(4)),
    },
    dominantVolumeShare: Number(((sortedVolumes[0] || 0) / totalVolume).toFixed(4)),
    topFiveVolumeShare: Number((sortedVolumes.slice(0, 5).reduce((sum, value) => sum + value, 0) / totalVolume).toFixed(4)),
    symmetryRatio: Number((mirrored / records.length).toFixed(4)),
    rotatedRatio: Number((records.filter((item) => item.rotated).length / records.length).toFixed(4)),
    massByHeight: normalize(massByHeight),
    slenderElementsRatio: Number((records.filter((item) => {
      const sorted = [...item.size].sort((a, b) => a - b);
      return sorted[2] / Math.max(sorted[0], 0.001) >= 4;
    }).length / records.length).toFixed(4)),
  };
}

function yamlPath(indentPath, key) {
  return [...indentPath, key].filter(Boolean).join('.');
}

function parseInlinePairs(value) {
  return [...String(value || '').matchAll(/(?:^|[,{;]\s*)([A-Za-z_][\w-]*)\s*:\s*([^,}]+)/g)]
    .map(([, key, raw]) => [key.toLowerCase(), raw.trim().replace(/^['"]|['"]$/g, '')]);
}

function topLevelDefinition(stack) {
  return stack.find((entry) => entry.indent === 0 && entry.kind === 'mapping' && entry.key)?.key || null;
}

function pushUnique(list, value) {
  if (value != null && value !== '') list.push(value);
}

function extractYaml(filePath, relative) {
  const text = fs.readFileSync(filePath, 'utf8');
  const refs = {
    models: [],
    states: [],
    defaultStates: [],
    modelParts: [],
    changeParts: [],
    skills: [],
    sounds: [],
    items: [],
    mechanics: [],
    triggers: [],
    modelOccurrences: [],
    stateOccurrences: [],
    defaultStateOccurrences: [],
    skillOccurrences: [],
    soundOccurrences: [],
    mechanicOccurrences: [],
  };
  const lines = text.split(/\r?\n/);
  const stack = [];
  let currentModelBinding = null;

  for (const [index, line] of lines.entries()) {
    const lineNumber = index + 1;
    const indent = (/^\s*/.exec(line)?.[0].length) || 0;
    const trimmed = line.trim();
    while (stack.length && indent <= stack[stack.length - 1].indent) stack.pop();
    if (!trimmed || trimmed.startsWith('#')) continue;

    const topDefinition = topLevelDefinition(stack);
    const linePath = yamlPath(stack.map((entry) => entry.key).filter(Boolean), null);
    const activeContext = currentModelBinding && currentModelBinding.definition === topDefinition
      ? currentModelBinding
      : null;

    const listMapEntry = /^-\s*([A-Za-z0-9_.-]+)\s*:\s*(.*)$/.exec(trimmed);
    if (listMapEntry) {
      stack.push({ indent, key: listMapEntry[1], kind: 'mapping' });
    } else {
      const mapping = /^([A-Za-z0-9_.-]+)\s*:\s*(.*)$/.exec(trimmed);
      if (mapping) stack.push({ indent, key: mapping[1], kind: 'mapping' });
    }

    const mechanic = /^\s*-\s*([A-Za-z][\w:]*)\s*(?:\{|\s|@|$)/.exec(line);
    if (mechanic) {
      const name = mechanic[1].toLowerCase();
      refs.mechanics.push(name);
      refs.mechanicOccurrences.push({ name, line: lineNumber, path: linePath || topDefinition || '<root>' });
    }
    for (const match of line.matchAll(/~(on\w+|\w+):?/ig)) refs.triggers.push(match[1].toLowerCase());

    for (const match of line.matchAll(/\bmodel\{([^}]*)}/ig)) {
      const body = match[1];
      const model = /(?:^|;)(?:m|mid|model)=([^;}\s]+)/i.exec(body)?.[1] || null;
      if (!model) continue;
      const pathHint = /(?:^|;)(?:id|partid|state|stance)=([^;}\s]+)/i.exec(body)?.[1] || null;
      refs.models.push(model);
      const bindingType = /(?:^|\/)mobs\//i.test(relative) ? 'mob_binding' : 'supporting_reference';
      const occurrence = {
        model,
        line: lineNumber,
        path: linePath || topDefinition || '<root>',
        locator: pathHint || topDefinition || '<root>',
        bindingType,
      };
      refs.modelOccurrences.push(occurrence);
      if (bindingType === 'mob_binding') {
        currentModelBinding = {
          model,
          line: lineNumber,
          definition: topDefinition,
          locator: occurrence.locator,
        };
      }
    }
    for (const match of line.matchAll(/\bstate\{[^}]*\b(?:s|state)=([^;}\s]+)/ig)) {
      refs.states.push(match[1]);
      refs.stateOccurrences.push({
        state: match[1],
        line: lineNumber,
        path: linePath || topDefinition || '<root>',
        model: activeContext?.model || null,
        modelLine: activeContext?.line || null,
        activeBinding: Boolean(activeContext),
      });
    }
    for (const match of line.matchAll(/\bdefaultstate\{([^}]*)}/ig)) {
      const state = /(?:^|;)(?:s|state)=([^;}\s]+)/i.exec(match[1]);
      const type = /(?:^|;)(?:t|type)=([^;}\s]+)/i.exec(match[1]);
      if (state) {
        const record = { state: state[1], type: type?.[1] || null };
        refs.defaultStates.push(record);
        refs.defaultStateOccurrences.push({
          ...record,
          line: lineNumber,
          path: linePath || topDefinition || '<root>',
          model: activeContext?.model || null,
          modelLine: activeContext?.line || null,
          activeBinding: Boolean(activeContext),
        });
      }
    }
    for (const match of line.matchAll(/@modelpart\{([^}]*)}/ig)) {
      const part = /(?:^|;)(?:p|pid|part)=([^;}\s]+)/i.exec(match[1]);
      const model = /(?:^|;)(?:m|mid|model)=([^;}\s]+)/i.exec(match[1]);
      if (part) refs.modelParts.push({ part: part[1], model: model?.[1] || null });
    }
    for (const match of line.matchAll(/\bchangepart\{([^}]*)}/ig)) {
      const value = (names) => {
        const found = new RegExp(`(?:^|;)(?:${names})=([^;}\\s]+)`, 'i').exec(match[1]);
        return found?.[1] || null;
      };
      refs.changeParts.push({
        model: value('m|mid|model'),
        part: value('p|pid|part'),
        newModel: value('nm|nmid|newmodel'),
        newPart: value('np|npid|newpart'),
      });
    }
    for (const match of line.matchAll(/\bskill\{[^}]*\b(?:s|skill)=([^;}\s]+)/ig)) {
      refs.skills.push(match[1]);
      refs.skillOccurrences.push({
        skill: match[1],
        line: lineNumber,
        path: linePath || topDefinition || '<root>',
        model: activeContext?.model || null,
        modelLine: activeContext?.line || null,
        activeBinding: Boolean(activeContext),
      });
    }
    for (const match of line.matchAll(/\bsound\{[^}]*\b(?:s|sound)=([^;}\s]+)/ig)) {
      refs.sounds.push(match[1]);
      refs.soundOccurrences.push({
        sound: match[1],
        line: lineNumber,
        path: linePath || topDefinition || '<root>',
        model: activeContext?.model || null,
        modelLine: activeContext?.line || null,
        activeBinding: Boolean(activeContext),
      });
    }
    for (const match of line.matchAll(/\b(?:item|i)=([^;}\s]+)/ig)) refs.items.push(match[1]);

    if (/^skills\s*:/.test(trimmed) || /^drops\s*:/.test(trimmed) || /^options\s*:/.test(trimmed)) {
      currentModelBinding = activeContext;
    }
    if (/^\w[\w.-]*\s*:\s*\{/.test(trimmed)) {
      const inline = Object.fromEntries(parseInlinePairs(trimmed));
      if (inline.model || inline.mid || inline.m) {
        const model = inline.model || inline.mid || inline.m;
        refs.models.push(model);
        refs.modelOccurrences.push({
          model,
          line: lineNumber,
          path: linePath || topDefinition || '<root>',
          locator: topDefinition || '<root>',
          bindingType: /(?:^|\/)mobs\//i.test(relative) ? 'mob_binding' : 'supporting_reference',
        });
      }
    }
  }

  return {
    file: relative,
    bytes: fs.statSync(filePath).size,
    refs,
    definitions: [...text.matchAll(/^([A-Za-z0-9_.-]+):\s*$/gm)].map((match) => match[1]),
  };
}

function inspectBlueprint(filePath, relative) {
  const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  const elements = data.elements || [];
  const bones = flattenBones(data.outliner || []);
  const animations = (data.animations || []).map((animation) => {
    const animators = Object.values(animation.animators || {});
    const keyframes = collectAnimationFrames(animation);
    const times = keyframes.map((frame) => Number(frame.time || 0));
    const length = Number(animation.length || Math.max(0, ...times));
    const movingBones = animators.filter((animator) => (animator.keyframes || []).some((frame) =>
      ['rotation', 'position', 'scale'].includes(frame.channel))).length;
    const channels = counts(keyframes.map((frame) => frame.channel));
    const phaseTimes = deriveAnimationPhases(keyframes, length);
    return {
      name: animation.name,
      role: animationRole(animation.name),
      length,
      loop: animation.loop,
      animators: animators.length,
      movingBones,
      keyframes: keyframes.length,
      timelineFrames: keyframes.filter((frame) => frame.channel === 'timeline').length,
      channels,
      density: Number((keyframes.length / Math.max(length, 0.05)).toFixed(3)),
      phaseCoverage: {
        start: keyframes.filter((frame) => Number(frame.time || 0) <= length * 0.2).length,
        middle: keyframes.filter((frame) => Number(frame.time || 0) > length * 0.2 && Number(frame.time || 0) < length * 0.8).length,
        end: keyframes.filter((frame) => Number(frame.time || 0) >= length * 0.8).length,
      },
      phaseTimes,
      sampleTimes: [phaseTimes.start, phaseTimes.anticipation, phaseTimes.impact, phaseTimes.recovery, phaseTimes.end],
    };
  });
  const boundsElements = elements.filter((element) => Array.isArray(element.from) && Array.isArray(element.to) && element.name !== 'hitbox');
  const bounds = boundsElements.length ? {
    min: [0, 1, 2].map((axis) => Math.min(...boundsElements.map((element) => Math.min(element.from[axis], element.to[axis])))),
    max: [0, 1, 2].map((axis) => Math.max(...boundsElements.map((element) => Math.max(element.from[axis], element.to[axis])))),
  } : null;
  if (bounds) bounds.size = bounds.max.map((value, axis) => value - bounds.min[axis]);
  const textureDetails = (data.textures || []).map((texture) => ({
    name: texture.name,
    renderMode: texture.render_mode || 'default',
    visible: texture.visible !== false,
    metrics: textureMetrics(texture),
  }));

  return {
    file: relative,
    id: path.basename(filePath, '.bbmodel'),
    bytes: fs.statSync(filePath).size,
    elements: elements.length,
    rotatedElements: elements.filter((element) => element.rotation?.some((value) => Number(value) !== 0)).length,
    bones,
    boneCount: bones.length,
    renderedBones: bones.filter((bone) => bone.cubes > 0 && bone.name !== 'hitbox').length,
    maxDepth: Math.max(0, ...bones.map((bone) => bone.depth)),
    hitboxes: bones.filter((bone) => bone.name === 'hitbox' || /^(?:b_|ob_)/.test(bone.name || '')).map((bone) => bone.name),
    helperBones: bones.filter((bone) => bone.cubes === 0).map((bone) => bone.name),
    specialBones: bones.filter((bone) => /^(?:h_|b_|ob_|p_|g_|mount|seat|tag|ir_|il_|ih_)/.test(bone.name || '')).map((bone) => bone.name),
    animations,
    animationCount: animations.length,
    keyframes: animations.reduce((sum, animation) => sum + animation.keyframes, 0),
    timelineFrames: animations.reduce((sum, animation) => sum + animation.timelineFrames, 0),
    textures: (data.textures || []).map((texture) => ({
      name: texture.name,
      renderMode: texture.render_mode || 'default',
      visible: texture.visible !== false,
    })),
    textureDetails,
    resolution: data.resolution || null,
    bounds,
    geometry: geometryMetrics(elements, bounds),
  };
}

function counts(values) {
  const output = {};
  for (const value of values) output[value] = (output[value] || 0) + 1;
  return Object.fromEntries(Object.entries(output).sort((a, b) => b[1] - a[1]));
}

function percentile(values, amount) {
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * amount))] || 0;
}

function topEntries(values, limit = 30) {
  return Object.entries(counts(values)).slice(0, limit).map(([name, count]) => ({ name, count }));
}

function normalizeName(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/^animation\.[^.]+\./, '')
    .replace(/[0-9]+/g, '#')
    .replace(/[.\s-]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function animationRole(name) {
  const value = normalizeName(name);
  const roles = [
    ['death', /death|die|dead|dissolve/],
    ['attack', /attack|bite|claw|slash|smash|slam|stomp|swing|strike|shoot|cast|breath|roar/],
    ['locomotion', /walk|run|fly|swim|crawl|jump|leap|move/],
    ['transition', /spawn|awaken|wake|sleep|transform|phase|stun|recover|intro|outro/],
    ['idle', /idle|rest|hover|float/],
    ['reaction', /hurt|hit|damage|knock|flinch/],
  ];
  return roles.find(([, pattern]) => pattern.test(value))?.[0] || 'other';
}

function maturityTier(blueprint, linkedYamlCount) {
  let score = 0;
  if (blueprint.elements >= 20) score += 1;
  if (blueprint.boneCount >= 10) score += 1;
  if (blueprint.animationCount >= 4) score += 1;
  if (blueprint.keyframes >= 100) score += 1;
  if (blueprint.helperBones.length >= 2) score += 1;
  if (blueprint.hitboxes.length > 0) score += 1;
  if (blueprint.timelineFrames > 0) score += 1;
  if (linkedYamlCount > 0) score += 1;
  if (score >= 7) return 'integrated';
  if (score >= 5) return 'production';
  if (score >= 3) return 'developed';
  return 'basic';
}

function classifyArchetype(blueprint) {
  const names = blueprint.bones.map((bone) => normalizeName(bone.name));
  const id = normalizeName(blueprint.id);
  const has = (pattern) => names.some((name) => pattern.test(name)) || pattern.test(id);
  if (has(/wing|fly|bird|dragon|gryff|bat/)) return 'flying';
  if (has(/tentacle|head_#|neck_#|hydra|kraken|worm|serpent/)) return 'multipart';
  if (has(/front_leg|back_leg|paw|hoof|quadruped|wolf|bear|mammoth|cerberus/)) return 'quadruped';
  if (has(/vehicle|mount|cart|chair|prop|weapon|projectile|vfx/)) return 'prop-or-vehicle';
  if (has(/left_leg|right_leg|leg_l|leg_r|biped|humanoid|hand_l|hand_r/)) return 'humanoid';
  return 'other';
}

function familyRoot(id) {
  const lower = normalizeName(id);
  let root = lower;
  let previous;
  do {
    previous = root;
    root = root
      .replace(/(?:^|_)(parts?|damage|damaged|pet|pets|minion|summon|summons|vfx|fx|projectile|projectiles|gibs?|telegraph|ground(?:_?fx|_?crack)?|splash|portal|whirlpool|star|book|book_page|sword|default|egg|vibrant)$/g, '')
      .replace(/(?:_variant|_phase#|_#seg|_#|_d)$/g, '');
  } while (root !== previous);
  return root
    .replace(/^littleroom_/, '')
    .replace(/^lr_/, 'lr_')
    .replace(/__+/g, '_')
    .replace(/^_+|_+$/g, '') || lower;
}

function familyRole(id) {
  const lower = normalizeName(id);
  if (/(?:^|_)parts?$/.test(lower)) return 'parts';
  if (/(?:^|_)(damage|damaged|_d)$/.test(lower)) return 'damage';
  if (/(?:^|_)(vibrant|phase\d*|variant\d*)$/.test(lower)) return 'phase';
  if (/(?:^|_)pet(?:s)?$/.test(lower)) return 'pet';
  if (/(?:^|_)(vfx|fx|ground_fx|groundcrack|projectile|portal|telegraph|whirlpool|star|book_page|splash|gibs?)$/.test(lower)) return 'effect';
  if (/(?:^|_)(book|sword|egg)$/.test(lower)) return 'support';
  return 'primary';
}

function familySortRank(role) {
  return { primary: 0, phase: 1, damage: 2, parts: 3, pet: 4, support: 5, effect: 6 }[role] ?? 9;
}

function textureComparison(textureDetails) {
  const details = textureDetails
    .map((texture) => ({
      name: texture.name,
      renderMode: texture.renderMode,
      metrics: texture.metrics,
    }))
    .filter((texture) => texture.metrics && !texture.metrics.error);
  if (!details.length) return null;
  const diffuse = details.filter((texture) => texture.renderMode !== 'emissive');
  const emissive = details.filter((texture) => texture.renderMode === 'emissive');
  const pool = diffuse.length ? diffuse : details;
  const avg = (values) => Number((values.reduce((sum, value) => sum + value, 0) / Math.max(values.length, 1)).toFixed(4));
  return {
    textures: details.length,
    diffuseTextures: diffuse.length,
    emissiveTextures: emissive.length,
    averageLuminance: avg(pool.map((texture) => texture.metrics.luminance || 0)),
    averageContrast: avg(pool.map((texture) => texture.metrics.contrast || 0)),
    averageSaturation: avg(pool.map((texture) => texture.metrics.saturation || 0)),
    palette: pool
      .flatMap((texture) => texture.metrics.palette || [])
      .slice(0, 8),
  };
}

function matchesAnimationOccurrence(animationName, value) {
  return normalizeName(animationName) === normalizeName(value);
}

function animationRuntimeBindings(animation, bindingEvidence, yamlsByFile) {
  const evidence = [];
  for (const binding of bindingEvidence) {
    const yaml = yamlsByFile.get(binding.file);
    if (!yaml) continue;
    for (const state of yaml.refs.stateOccurrences || []) {
      if (state.activeBinding && matchesAnimationOccurrence(animation.name, state.state)) {
        evidence.push({ type: 'state', file: binding.file, line: state.line, value: state.state, path: state.path });
      }
    }
    for (const state of yaml.refs.defaultStateOccurrences || []) {
      if (state.activeBinding && matchesAnimationOccurrence(animation.name, state.state)) {
        evidence.push({ type: 'default_state', file: binding.file, line: state.line, value: state.state, path: state.path });
      }
    }
    for (const skill of yaml.refs.skillOccurrences || []) {
      if (skill.activeBinding && (matchesAnimationOccurrence(animation.name, skill.skill) || normalizeName(skill.skill).includes(normalizeName(animation.name)))) {
        evidence.push({ type: 'skill', file: binding.file, line: skill.line, value: skill.skill, path: skill.path });
      }
    }
    for (const sound of yaml.refs.soundOccurrences || []) {
      if (sound.activeBinding && normalizeName(sound.sound).includes(normalizeName(animation.name))) {
        evidence.push({ type: 'sound', file: binding.file, line: sound.line, value: sound.sound, path: sound.path });
      }
    }
  }
  return evidence
    .sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line)
    .filter((item, index, list) => index === 0 || `${item.type}:${item.file}:${item.line}:${item.value}` !== `${list[index - 1].type}:${list[index - 1].file}:${list[index - 1].line}:${list[index - 1].value}`);
}

function artisticObservations(blueprint) {
  const geometry = blueprint.geometry;
  const textures = blueprint.textureDetails.map((texture) => texture.metrics).filter((metrics) => metrics && !metrics.error);
  const observations = [];
  if (!geometry) return ['No measurable cube geometry; inspect as a helper, effect, or incomplete source.'];
  const { proportions, symmetryRatio, dominantVolumeShare, rotatedRatio, massByHeight, slenderElementsRatio } = geometry;
  if (proportions.widthToHeight >= 1.15) observations.push('Broad silhouette favors mass, stability, or creature width over vertical authority.');
  else if (proportions.widthToHeight <= 0.45) observations.push('Narrow vertical silhouette risks reading as spindly unless reinforced by a strong crown, shoulders, or base.');
  else observations.push('Balanced width-to-height ratio supports a conventional readable body mass.');
  if (symmetryRatio >= 0.75) observations.push('Strong bilateral repetition gives clarity but needs focal asymmetry or material contrast to avoid generic design.');
  else if (symmetryRatio <= 0.35) observations.push('Purposeful asymmetry can carry identity; verify that it remains readable from the gameplay camera.');
  if (dominantVolumeShare >= 0.2) observations.push('One large primitive creates a clear dominant mass.');
  else observations.push('Mass is distributed across many elements; hierarchy must come from grouping, color, or silhouette.');
  if (rotatedRatio >= 0.45) observations.push('Frequent angled forms break the default block grid and strengthen organic or crafted character.');
  if (slenderElementsRatio >= 0.35) observations.push('Many elongated elements create limb, branch, horn, cable, or weapon language; watch fragility and visual noise.');
  const massLabel = ['lower', 'middle', 'upper'][massByHeight.indexOf(Math.max(...massByHeight))];
  observations.push(`Visual mass concentrates in the ${massLabel} third.`);
  if (textures.length) {
    const saturation = textures.reduce((sum, item) => sum + item.saturation, 0) / textures.length;
    const contrast = textures.reduce((sum, item) => sum + item.contrast, 0) / textures.length;
    observations.push(`${saturation >= 0.5 ? 'Saturated' : saturation <= 0.25 ? 'Muted' : 'Moderate'} palette with ${contrast >= 0.25 ? 'strong' : contrast <= 0.12 ? 'soft' : 'moderate'} value contrast.`);
  } else observations.push('Texture palette is unavailable; no material or mood claim is justified.');
  return observations;
}

function buildKnowledge(blueprints, yamls, blueprintById) {
  const yamlsByFile = new Map(yamls.map((yaml) => [yaml.file, yaml]));
  const links = new Map();
  for (const yaml of yamls) {
    for (const occurrence of yaml.refs.modelOccurrences) {
      const model = occurrence.model.toLowerCase();
      if (!links.has(model)) links.set(model, []);
      links.get(model).push({
        file: yaml.file,
        line: occurrence.line,
        path: occurrence.path || null,
        locator: occurrence.locator || null,
        bindingType: occurrence.bindingType,
      });
    }
  }

  const profiles = blueprints.map((blueprint) => {
    const bindingEvidence = links.get(blueprint.id.toLowerCase()) || [];
    const linkedYaml = [...new Set(bindingEvidence.map((item) => item.file))];
    const runtimeBindings = bindingEvidence.filter((item) => item.bindingType === 'mob_binding');
    const roles = counts(blueprint.animations.map((animation) => animationRole(animation.name)));
    const familyId = familyRoot(blueprint.id);
    const familyType = familyRole(blueprint.id);
    const materialProfile = textureComparison(blueprint.textureDetails);
    const score =
      Math.min(blueprint.elements, 150) / 15 +
      Math.min(blueprint.boneCount, 80) / 8 +
      Math.min(blueprint.animationCount, 20) +
      Math.min(blueprint.keyframes, 3000) / 150 +
      Math.min(blueprint.helperBones.length, 20) / 2 +
      Math.min(blueprint.hitboxes.length, 8) * 2 +
      Math.min(blueprint.timelineFrames, 20) +
      Math.min(runtimeBindings.length, 3) * 5 +
      Math.min(linkedYaml.length - runtimeBindings.length, 3);
    return {
      id: blueprint.id,
      file: blueprint.file,
      familyId,
      familyRole: familyType,
      archetype: classifyArchetype(blueprint),
      tier: maturityTier(blueprint, runtimeBindings.length),
      score: Number(score.toFixed(2)),
      elements: blueprint.elements,
      bones: blueprint.boneCount,
      depth: blueprint.maxDepth,
      animations: blueprint.animationCount,
      keyframes: blueprint.keyframes,
      helperBones: blueprint.helperBones.length,
      hitboxes: blueprint.hitboxes.length,
      timelineFrames: blueprint.timelineFrames,
      linkedYaml: linkedYaml.length,
      runtimeBindings: runtimeBindings.length,
      animationRoles: roles,
      geometry: blueprint.geometry,
      textureDetails: blueprint.textureDetails,
      materialProfile,
      artisticObservations: artisticObservations(blueprint),
      animationsDetail: blueprint.animations.map((animation) => ({
        ...animation,
        runtimeEvidence: animationRuntimeBindings(animation, bindingEvidence, yamlsByFile),
      })),
      linkedYamlFiles: linkedYaml,
      bindingEvidence,
    };
  });
  const familyMembers = counts(profiles.map((profile) => profile.familyId));
  for (const profile of profiles) {
    profile.familySize = familyMembers[profile.familyId] || 1;
  }
  const families = Object.entries(
    profiles.reduce((acc, profile) => {
      if (!acc[profile.familyId]) acc[profile.familyId] = [];
      acc[profile.familyId].push(profile);
      return acc;
    }, {}),
  )
    .map(([familyId, members]) => ({
      familyId,
      archetype: counts(members.map((member) => member.archetype))[0]?.[0] || 'other',
      members: members
        .sort((a, b) => familySortRank(a.familyRole) - familySortRank(b.familyRole) || b.runtimeBindings - a.runtimeBindings || a.id.localeCompare(b.id))
        .map((member) => member.id),
    }))
    .sort((a, b) => a.archetype.localeCompare(b.archetype) || a.familyId.localeCompare(b.familyId));

  const animationNames = blueprints.flatMap((blueprint) => blueprint.animations.map((animation) => normalizeName(animation.name)));
  const boneNames = blueprints.flatMap((blueprint) => blueprint.bones.map((bone) => normalizeName(bone.name)));
  const roleCounts = counts(blueprints.flatMap((blueprint) => blueprint.animations.map((animation) => animationRole(animation.name))));
  const tiers = counts(profiles.map((profile) => profile.tier));
  const linkedBlueprints = profiles.filter((profile) => profile.runtimeBindings > 0).length;
  const referencedBlueprints = profiles.filter((profile) => profile.linkedYaml > 0).length;
  const withHelpers = profiles.filter((profile) => profile.helperBones > 0).length;
  const withHitboxes = profiles.filter((profile) => profile.hitboxes > 0).length;
  const withTimelines = profiles.filter((profile) => profile.timelineFrames > 0).length;
  const topReferences = [...profiles].sort((a, b) => b.score - a.score).slice(0, 40);

  const evidenceRules = [
    `${linkedBlueprints}/${profiles.length} blueprints have a direct model mechanic in a MythicMobs mob file; ${referencedBlueprints}/${profiles.length} appear anywhere in MythicMobs YAML. Supporting references are not runtime proof.`,
    `${withHelpers}/${profiles.length} blueprints contain empty helper bones and ${withHitboxes}/${profiles.length} contain named hitbox or interaction bones; gameplay anchors are common but not universal.`,
    `${withTimelines}/${profiles.length} blueprints contain timeline keyframes; explicit synchronization should be copied only from references that actually use it.`,
    `Animation roles across the corpus: ${Object.entries(roleCounts).map(([role, count]) => `${role}=${count}`).join(', ')}.`,
    `Maturity tiers based on geometry, rig, animation, helpers, hitboxes, timelines, and YAML linkage: ${Object.entries(tiers).map(([tier, count]) => `${tier}=${count}`).join(', ')}.`,
    'A strong reference must score across several systems. High cube or keyframe count alone is not evidence of better design.',
    'Select references by anatomy and encounter role before borrowing structure; quadruped, humanoid, multipart, flying, and prop rigs solve different problems.',
    'For every new model, record which reference supplied silhouette, rig, locomotion, attack timing, hit volume, VFX anchor, sound layering, and state-machine ideas.',
    'Every borrowed pattern needs a visible or runtime acceptance check; corpus similarity is not completion.',
  ];

  return {
    coverage: {
      blueprints: profiles.length,
      linkedBlueprints,
      referencedBlueprints,
      withHelpers,
      withHitboxes,
      withTimelines,
      tiers,
      archetypes: counts(profiles.map((profile) => profile.archetype)),
      animationRoles: roleCounts,
    },
    vocabularies: {
      animations: topEntries(animationNames, 50),
      bones: topEntries(boneNames, 50),
      mechanics: topEntries(yamls.flatMap((yaml) => yaml.refs.mechanics), 50),
      triggers: topEntries(yamls.flatMap((yaml) => yaml.refs.triggers), 30),
    },
    topReferences,
    families,
    profiles,
    evidenceRules,
    unresolvedModelIds: [...links.keys()].filter((id) => !blueprintById.has(id)),
  };
}

function resolvePartAliases(blueprint) {
  const aliases = new Set();
  for (const bone of blueprint.bones) {
    aliases.add(bone.name);
    aliases.add(bone.name.replace(/^(?:h_|b_|ob_|p_|g_|ir_|il_|ih_)/, ''));
  }
  return aliases;
}

function markdown(report) {
  const b = report.summary.blueprints;
  const knowledge = report.knowledge;
  const lines = [
    '# ModelEngine Reference Corpus',
    '',
    `Generated from \`${report.corpus}\`. The corpus is read-only reference material.`,
    '',
    '## Inventory',
    '',
    `- ${report.summary.files} files, ${report.summary.bytes} bytes.`,
    `- ${b.total} parsed blueprints and ${report.summary.yaml.total} MythicMobs YAML files.`,
    `- ${report.summary.images.total} PNG textures and ${report.summary.audio.total} OGG sounds.`,
    `- The root resource pack contains ${report.duplicatePacks.identical} byte-identical copies of files already under \`ModelEngine/resource pack\`.`,
    '',
    '## Blueprint Benchmarks',
    '',
    '| Metric | P50 | P75 | P90 | P95 | Maximum |',
    '|---|---:|---:|---:|---:|---:|',
    ...Object.entries(b.percentiles).map(([name, value]) => `| ${name} | ${value.p50} | ${value.p75} | ${value.p90} | ${value.p95} | ${value.max} |`),
    '',
    '## Corpus Coverage',
    '',
    `- ${knowledge.coverage.linkedBlueprints}/${knowledge.coverage.blueprints} blueprints have direct MythicMobs linkage.`,
    `- ${knowledge.coverage.withHelpers} use helper bones, ${knowledge.coverage.withHitboxes} use named hitboxes, and ${knowledge.coverage.withTimelines} use timeline events.`,
    `- Maturity tiers: ${Object.entries(knowledge.coverage.tiers).map(([tier, count]) => `${tier}=${count}`).join(', ')}.`,
    '',
    '## Strongest Multi-System References',
    '',
    '| Model | Tier | Score | Elements | Bones | Animations | Helpers | Hitboxes | Timelines | YAML |',
    '|---|---|---:|---:|---:|---:|---:|---:|---:|---:|',
    ...knowledge.topReferences.slice(0, 25).map((item) => `| ${item.id} | ${item.tier} | ${item.score} | ${item.elements} | ${item.bones} | ${item.animations} | ${item.helperBones} | ${item.hitboxes} | ${item.timelineFrames} | ${item.linkedYaml} |`),
    '',
    '## Reusable Vocabulary',
    '',
    `- Animation roles: ${Object.entries(knowledge.coverage.animationRoles).map(([name, count]) => `${name}=${count}`).join(', ')}.`,
    `- Frequent animation names: ${knowledge.vocabularies.animations.slice(0, 25).map((item) => `${item.name} (${item.count})`).join(', ')}.`,
    `- Frequent bone names: ${knowledge.vocabularies.bones.slice(0, 25).map((item) => `${item.name} (${item.count})`).join(', ')}.`,
    `- Frequent MythicMobs mechanics: ${knowledge.vocabularies.mechanics.slice(0, 25).map((item) => `${item.name} (${item.count})`).join(', ')}.`,
    `- Frequent triggers: ${knowledge.vocabularies.triggers.slice(0, 20).map((item) => `${item.name} (${item.count})`).join(', ')}.`,
    '',
    '## Ecosystem Patterns',
    '',
    ...report.learnedRules.map((rule) => `- ${rule}`),
    '',
    '## High-Value References',
    '',
    ...report.deepStudies.map((study) => `- **${study.id}**: ${study.elements} elements, ${study.bones} bones, depth ${study.depth}, ${study.animations} animations, ${study.keyframes} keyframes. ${study.lesson}`),
    '',
    '## Reference Integrity',
    '',
    `- ${report.integrity.declaredModels} distinct model IDs are referenced by MythicMobs.`,
    `- ${report.integrity.missingModels.length} references do not resolve directly to a blueprint basename; these include dynamic placeholders or incomplete packs and must not become project assumptions.`,
    `- ${report.integrity.missingModelParts.length} model-part references could not be resolved by the conservative cross-pack mapper.`,
    `- ${report.integrity.missingStates.length} state references could not be resolved to an animation by the conservative mapper.`,
    '',
    'The machine-readable full inventory and relationship graph is stored in `MCMMORPG/_validation/reference_corpus/modelengine-reference-corpus.json`.',
    '',
  ];
  return lines.join('\n');
}

function learningMarkdown(report) {
  const knowledge = report.knowledge;
  return [
    '# ModelEngine Learning Ledger',
    '',
    `Generated from all ${report.summary.files} files in \`${report.corpus}\`. Regenerate after adding reference material.`,
    '',
    '## Evidence-Backed Lessons',
    '',
    ...knowledge.evidenceRules.map((rule) => `- ${rule}`),
    '',
    '## Production Contract',
    '',
    '- Start with a written silhouette target and choose references with matching anatomy and encounter role.',
    '- Record reference provenance separately for silhouette, rig, locomotion, attacks, hit volumes, VFX, sound, and states.',
    '- Require readable hierarchy, purposeful helper bones, local attack volumes, explicit impact timing, and complete state coverage where the encounter needs them.',
    '- Compare the current render against selected references at the same camera distance before polishing details.',
    '- Reject an iteration that adds complexity without improving silhouette, motion weight, combat readability, or runtime integration.',
    '- Close only with a current render, validated model-skill-asset graph, compiled resource pack, and live spawn evidence.',
    '',
    '## Selection Rules',
    '',
    '- Use the machine-ranked references as candidates, not automatic templates.',
    '- Inspect the full model and linked YAML before copying a pattern.',
    '- Prefer several specialized references over one model used for every subsystem.',
    '- Treat unresolved or unlinked assets as incomplete evidence.',
    '',
    '## Top Reference Candidates',
    '',
    '| Model | Tier | Score | Main evidence |',
    '|---|---|---:|---|',
    ...knowledge.topReferences.slice(0, 40).map((item) => `| ${item.id} | ${item.tier} | ${item.score} | ${item.elements} elements, ${item.bones} bones, ${item.animations} animations, ${item.helperBones} helpers, ${item.hitboxes} hitboxes, ${item.timelineFrames} timeline events, ${item.linkedYaml} linked YAML |`),
    '',
    'The full per-model profiles and vocabularies are stored in `MCMMORPG/_validation/reference_corpus/modelengine-reference-corpus.json`.',
    '',
  ].join('\n');
}

function atlasMarkdown(report) {
  const lines = [
    '# ModelEngine Creation Atlas',
    '',
    `Source corpus: \`${report.corpus}\`. This atlas contains ${report.knowledge.profiles.length} model cards and ${report.knowledge.profiles.reduce((sum, profile) => sum + profile.animationsDetail.length, 0)} animation cards.`,
    '',
    '## Reading Contract',
    '',
    '- Source facts are numeric fields and linked file paths.',
    '- Synthesis is explicitly listed under artistic observations.',
    '- A reference is a candidate, not permission to copy its style blindly.',
    '- Missing texture or runtime linkage means that material, mood, or gameplay claims remain unproven.',
    '',
  ];
  const orderedProfiles = [...report.knowledge.profiles].sort((a, b) =>
    a.archetype.localeCompare(b.archetype) ||
    a.familyId.localeCompare(b.familyId) ||
    familySortRank(a.familyRole) - familySortRank(b.familyRole) ||
    a.id.localeCompare(b.id));
  let currentArchetype = null;
  let currentFamily = null;
  for (const profile of orderedProfiles) {
    if (profile.archetype !== currentArchetype) {
      currentArchetype = profile.archetype;
      lines.push(`## Archetype: ${currentArchetype}`, '');
      currentFamily = null;
    }
    if (profile.familyId !== currentFamily) {
      currentFamily = profile.familyId;
      lines.push(`### Family: ${currentFamily} (${profile.familySize} models)`, '');
    }
    const geometry = profile.geometry;
    const palettes = profile.textureDetails
      .flatMap((texture) => texture.metrics?.palette?.slice(0, 3) || [])
      .slice(0, 8)
      .map((item) => `rgb(${item.rgb.join(',')})`)
      .join(', ');
    lines.push(
      `#### ${profile.id}`,
      '',
      `- Source: \`${profile.file}\`; archetype=${profile.archetype}; family=${profile.familyId}/${profile.familyRole}; tier=${profile.tier}; linked YAML=${profile.linkedYaml}.`,
      `- Structure: ${profile.elements} elements, ${profile.bones} bones, depth ${profile.depth}, ${profile.helperBones} helpers, ${profile.hitboxes} hitboxes.`,
      geometry
        ? `- Form: W/H=${geometry.proportions.widthToHeight}, D/H=${geometry.proportions.depthToHeight}, symmetry=${geometry.symmetryRatio}, dominant mass=${geometry.dominantVolumeShare}, rotated=${geometry.rotatedRatio}, slender=${geometry.slenderElementsRatio}.`
        : '- Form: no measurable cube geometry.',
      `- Palette evidence: ${palettes || 'unavailable'}.`,
      profile.materialProfile
        ? `- Material comparison: diffuse=${profile.materialProfile.diffuseTextures}, emissive=${profile.materialProfile.emissiveTextures}, luminance=${profile.materialProfile.averageLuminance}, contrast=${profile.materialProfile.averageContrast}, saturation=${profile.materialProfile.averageSaturation}.`
        : '- Material comparison: unavailable.',
      `- YAML evidence: ${profile.bindingEvidence.slice(0, 5).map((item) => `${item.file}:${item.line}`).join(', ') || 'none'}.`,
      `- Artistic synthesis: ${profile.artisticObservations.join(' ')}`,
      `- Animation coverage: ${Object.entries(profile.animationRoles).map(([role, count]) => `${role}=${count}`).join(', ') || 'none'}.`,
      '',
    );
    for (const animation of profile.animationsDetail) {
      lines.push(
        `##### Animation: ${animation.name}`,
        '',
        `- role=${animation.role}; length=${animation.length}s; loop=${animation.loop}; moving bones=${animation.movingBones}; keyframes=${animation.keyframes}; density=${animation.density}/s; timeline=${animation.timelineFrames}.`,
        `- phases: start=${animation.phaseTimes.start}s, anticipation=${animation.phaseTimes.anticipation}s, impact=${animation.phaseTimes.impact}s, recovery=${animation.phaseTimes.recovery}s, end=${animation.phaseTimes.end}s; render samples=${animation.sampleTimes.join(', ')}s.`,
        `- runtime evidence: ${animation.runtimeEvidence.slice(0, 6).map((item) => `${item.type}:${item.file}:${item.line}`).join(', ') || 'none found'}.`,
        '',
      );
    }
  }
  return lines.join('\n');
}

function main() {
  const corpus = path.resolve(arg('corpus', DEFAULT_CORPUS));
  const includeAudio = process.argv.includes('--audio-metadata');
  if (!fs.existsSync(corpus)) throw new Error(`Corpus not found: ${corpus}`);

  const paths = walk(corpus);
  const inventory = [];
  const blueprints = [];
  const yamls = [];
  const jsonErrors = [];
  const images = [];
  const audio = [];

  for (const filePath of paths) {
    const relative = path.relative(corpus, filePath);
    const extension = path.extname(filePath).toLowerCase() || '[none]';
    const record = {
      file: relative,
      extension,
      bytes: fs.statSync(filePath).size,
      sha1: sha1(filePath),
    };
    inventory.push(record);
    try {
      if (extension === '.bbmodel') blueprints.push(inspectBlueprint(filePath, relative));
      else if (extension === '.yml' || extension === '.yaml') yamls.push(extractYaml(filePath, relative));
      else if (extension === '.json' || extension === '.mcmeta') JSON.parse(fs.readFileSync(filePath, 'utf8'));
      else if (extension === '.png') images.push({ file: relative, ...pngMetadata(filePath) });
      else if (extension === '.ogg') audio.push({ file: relative, ...(includeAudio ? audioMetadata(filePath) : {}) });
    } catch (error) {
      jsonErrors.push({ file: relative, error: error.message });
    }
  }

  const blueprintById = new Map(blueprints.map((blueprint) => [blueprint.id.toLowerCase(), blueprint]));
  const blueprintIdCounts = counts(blueprints.map((blueprint) => blueprint.id.toLowerCase()));
  const duplicateBlueprintIds = Object.entries(blueprintIdCounts)
    .filter(([, count]) => count > 1)
    .map(([id, count]) => ({
      id,
      count,
      files: blueprints.filter((blueprint) => blueprint.id.toLowerCase() === id).map((blueprint) => blueprint.file),
    }));
  const modelRefs = yamls.flatMap((yaml) => yaml.refs.models.map((model) => ({ model: model.toLowerCase(), file: yaml.file })));
  const declaredModelIds = [...new Set(modelRefs.map((ref) => ref.model))];
  const missingModels = declaredModelIds.filter((model) => !blueprintById.has(model));
  const missingModelParts = [];
  const missingStates = [];
  for (const yaml of yamls) {
    const localModels = yaml.refs.models.map((model) => model.toLowerCase()).filter((model) => blueprintById.has(model));
    const inferred = localModels.length === 1 ? blueprintById.get(localModels[0]) : null;
    for (const ref of yaml.refs.modelParts) {
      const target = ref.model ? blueprintById.get(ref.model.toLowerCase()) : inferred;
      if (target && !resolvePartAliases(target).has(ref.part)) {
        missingModelParts.push({ file: yaml.file, model: target.id, part: ref.part });
      }
    }
    for (const state of [...yaml.refs.states, ...yaml.refs.defaultStates.map((item) => item.state)]) {
      if (inferred && !inferred.animations.some((animation) => animation.name === state)) {
        missingStates.push({ file: yaml.file, model: inferred.id, state });
      }
    }
  }

  const packA = inventory.filter((record) => record.file.startsWith('ModelEngine/resource pack/'));
  const packB = new Map(inventory.filter((record) => record.file.startsWith('resource pack/')).map((record) => [record.file.slice('resource pack/'.length), record]));
  const common = packA.filter((record) => packB.has(record.file.slice('ModelEngine/resource pack/'.length)));
  const identical = common.filter((record) => packB.get(record.file.slice('ModelEngine/resource pack/'.length)).sha1 === record.sha1).length;

  const metrics = {
    elements: blueprints.map((blueprint) => blueprint.elements),
    bones: blueprints.map((blueprint) => blueprint.boneCount),
    depth: blueprints.map((blueprint) => blueprint.maxDepth),
    animations: blueprints.map((blueprint) => blueprint.animationCount),
    keyframes: blueprints.map((blueprint) => blueprint.keyframes),
  };
  const percentiles = Object.fromEntries(Object.entries(metrics).map(([name, values]) => [name, {
    p50: percentile(values, 0.5),
    p75: percentile(values, 0.75),
    p90: percentile(values, 0.9),
    p95: percentile(values, 0.95),
    max: Math.max(...values),
  }]));

  const deepStudyLessons = {
    dreadroot: 'Segmented roots and three long articulated arm chains create identity; passive-to-awaken state replacement, moving model-part hit volumes, and layered sound sell scale.',
    golem_king: 'Purposeful asymmetry, articulated fingers, OBB interaction bones, stun/run state replacement, and destruction effects turn geometry into gameplay.',
    lr_hydra: 'Repeated neck chains, per-head target bones, local bite volumes, dismemberment through part replacement, and head-specific attacks support a multi-part boss.',
    ignevar: 'Deep chains, target helpers, local stomp and breath origins, brightness, tint, and layered audio create readable elemental attacks.',
    lrd_hollow_keeper: 'Weapon, foot, projectile, and gib bones are gameplay anchors; sleep/wake transitions and attack-specific moving volumes synchronize the encounter.',
    lr_minotaur: 'Dense full-body kinetic chains, timeline events, alternate locomotion states, and anticipation/recovery produce weight.',
    lrd_direwolf: 'Quadruped hierarchy, secondary chains, multiple attack families, and variant textures demonstrate reusable anatomy without a humanoid template.',
    boreal_druid_mammoth: 'Large quadruped mass, trunk and accessory chains, helper effects, and transformation support show how silhouette and phase mechanics cooperate.',
  };
  const deepStudies = Object.entries(deepStudyLessons).map(([id, lesson]) => {
    const blueprint = blueprintById.get(id);
    return blueprint ? {
      id,
      elements: blueprint.elements,
      bones: blueprint.boneCount,
      depth: blueprint.maxDepth,
      animations: blueprint.animationCount,
      keyframes: blueprint.keyframes,
      lesson,
    } : { id, elements: 0, bones: 0, depth: 0, animations: 0, keyframes: 0, lesson };
  });

  const learnedRules = [
    'Treat a mob as a synchronized model, behavior, collision, sound, VFX, item, and resource-pack system.',
    'Design gameplay helper bones while designing the rig: moving hit origins, projectile targets, VFX anchors, sub-hitboxes, detachable parts, and gaze targets.',
    'Attach melee damage windows to animated model parts; a static radius around the owner does not prove contact.',
    'Align damage, sound, particles, recoil, brightness, and part changes to explicit animation ticks.',
    'Use GCD, AI control, model rotation locks, body clamps, stances, and variables to preserve anticipation, impact, recovery, and phase state.',
    'Use default-state replacement for passive, awakened, locomotion, stun, flight, and phase modes instead of one permanent idle.',
    'Use changepart and part visibility only when damage, transformation, equipment, or dismemberment is part of the design.',
    'Use emissive maps, brightness, and tint as readability layers after silhouette and material separation are already strong.',
    'Set complexity budgets by archetype and gameplay role; corpus percentiles are diagnostics, not universal cube quotas.',
    'Validate the complete reference graph and current visual/runtime evidence before accepting a mob.',
  ];

  const knowledge = buildKnowledge(blueprints, yamls, blueprintById);
  const report = {
    schemaVersion: 2,
    generatedAt: new Date().toISOString(),
    corpus,
    audioMetadataIncluded: includeAudio,
    summary: {
      files: inventory.length,
      bytes: inventory.reduce((sum, record) => sum + record.bytes, 0),
      extensions: counts(inventory.map((record) => record.extension)),
      blueprints: { total: blueprints.length, percentiles },
      yaml: { total: yamls.length, mechanics: counts(yamls.flatMap((yaml) => yaml.refs.mechanics)), triggers: counts(yamls.flatMap((yaml) => yaml.refs.triggers)) },
      images: { total: images.length, dimensions: counts(images.map((image) => `${image.width || 0}x${image.height || 0}`)) },
      audio: { total: audio.length },
      jsonErrors,
    },
    duplicatePacks: {
      modelEnginePack: packA.length,
      rootPack: packB.size,
      common: common.length,
      identical,
    },
    integrity: {
      declaredModels: declaredModelIds.length,
      duplicateBlueprintIds,
      missingModels,
      missingModelParts,
      missingStates,
    },
    learnedRules,
    deepStudies,
    knowledge,
    blueprints,
    yamls,
    images,
    audio,
    inventory,
  };

  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  fs.mkdirSync(path.dirname(REPORT_PATH), { recursive: true });
  const output = path.join(OUTPUT_DIR, 'modelengine-reference-corpus.json');
  const cardsOutput = path.join(OUTPUT_DIR, 'modelengine-model-cards.json');
  fs.writeFileSync(output, `${JSON.stringify(report, null, 2)}\n`);
  fs.writeFileSync(cardsOutput, `${JSON.stringify({
    schemaVersion: 1,
    generatedAt: report.generatedAt,
    corpus: report.corpus,
    cards: report.knowledge.profiles,
  }, null, 2)}\n`);
  fs.writeFileSync(REPORT_PATH, markdown(report));
  fs.writeFileSync(LEARNING_PATH, learningMarkdown(report));
  fs.writeFileSync(ATLAS_PATH, atlasMarkdown(report));
  const designKnowledge = spawnSync(
    process.execPath,
    [path.join(__dirname, 'build_model_design_knowledge.js')],
    { encoding: 'utf8' },
  );
  if (designKnowledge.status !== 0) {
    throw new Error(`Design knowledge generation failed: ${designKnowledge.stderr || designKnowledge.stdout}`);
  }
  console.log(`MODEL_REFERENCE_CORPUS_PASS: ${inventory.length} files indexed`);
  console.log(`  blueprints=${blueprints.length} yaml=${yamls.length} images=${images.length} audio=${audio.length}`);
  console.log(`  report=${REPORT_PATH}`);
  console.log(`  learning=${LEARNING_PATH}`);
  console.log(`  atlas=${ATLAS_PATH}`);
  console.log(`  cards=${cardsOutput}`);
  console.log(`  data=${output}`);
  console.log(`  design=${path.join(OUTPUT_DIR, 'modelengine-design-router.json')}`);
  console.log(`  risks=${path.join(OUTPUT_DIR, 'modelengine-negative-patterns.json')}`);
}

main();
