const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const MOBS_ROOT = path.join(ROOT, 'plugins', 'MythicMobs', 'Packs');
const BLUEPRINTS_ROOT = path.join(ROOT, 'plugins', 'ModelEngine', 'blueprints');
const QUALITY_ROOT = path.join(__dirname, 'model_quality');
const REVIEW_CATEGORIES = [
  'silhouette',
  'proportions',
  'construction',
  'character',
  'readability',
  'rig',
  'animation',
  'texture',
  'hitbox',
];

function listFiles(root, suffix, acc = []) {
  if (!fs.existsSync(root)) return acc;
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) listFiles(full, suffix, acc);
    else if (entry.isFile() && full.endsWith(suffix)) acc.push(full);
  }
  return acc;
}

function parseMobModels(filePath) {
  const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/);
  const results = [];
  let currentMobId = null;
  let inModelBlock = false;
  let currentTemplate = null;
  let current = null;

  for (const rawLine of lines) {
    const line = rawLine.replace(/\t/g, '    ');
    const topLevel = /^([a-z0-9_]+):\s*$/.exec(line);
    if (topLevel) {
      if (current) results.push(current);
      currentMobId = topLevel[1];
      currentTemplate = null;
      current = null;
      inModelBlock = false;
      continue;
    }
    if (!currentMobId) continue;
    const template = /^\s{2}Template:\s*([a-z0-9_]+)/.exec(line);
    if (template) currentTemplate = template[1];
    if (/^\s{2}Model:\s*$/.test(line)) {
      inModelBlock = true;
      continue;
    }
    if (inModelBlock) {
      const modelId = /^\s{4}Id:\s*([a-z0-9_]+)/.exec(line);
      if (modelId) {
        current = {
          mobId: currentMobId,
          modelId: modelId[1],
          filePath,
          template: currentTemplate,
          stateAnimations: [],
        };
        inModelBlock = false;
      } else if (/^\s{2}[A-Z][A-Za-z0-9_-]*:/.test(line) || /^\S/.test(line)) {
        inModelBlock = false;
      }
    }
    if (current) {
      const state = /state\{[^}]*\b(?:s|state)=([a-z0-9_]+)/i.exec(line);
      if (state) current.stateAnimations.push(state[1]);
    }
  }
  if (current) results.push(current);
  return results;
}

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
}

function flattenBones(nodes, parent = null, depth = 0, acc = []) {
  for (const node of nodes || []) {
    if (!node || typeof node !== 'object' || Array.isArray(node)) continue;
    acc.push({ ...node, parent, depth });
    flattenBones(node.children, node.name || null, depth + 1, acc);
  }
  return acc;
}

function elementIdsInOutliner(nodes, acc = []) {
  for (const node of nodes || []) {
    if (!node || typeof node !== 'object' || Array.isArray(node)) continue;
    for (const child of node.children || []) {
      if (typeof child === 'string') acc.push(child);
    }
    elementIdsInOutliner(node.children, acc);
  }
  return acc;
}

function finiteVector(value, length = 3) {
  return Array.isArray(value)
    && value.length === length
    && value.every((part) => Number.isFinite(Number(part)));
}

function getBounds(elements) {
  const visual = elements.filter((element) => element.name !== 'hitbox' && finiteVector(element.from) && finiteVector(element.to));
  if (!visual.length) return null;
  const min = [0, 1, 2].map((axis) => Math.min(...visual.map((element) => Math.min(element.from[axis], element.to[axis]))));
  const max = [0, 1, 2].map((axis) => Math.max(...visual.map((element) => Math.max(element.from[axis], element.to[axis]))));
  return { min, max, size: max.map((value, axis) => value - min[axis]) };
}

function animationStats(animation, boneCount) {
  const animators = Object.values(animation.animators || {}).filter((animator) => animator.type === 'bone');
  const keyed = animators.filter((animator) => {
    const values = (animator.keyframes || []).map((frame) => {
      const point = frame.data_points?.[0] || {};
      return [Number(point.x || 0), Number(point.y || 0), Number(point.z || 0)];
    });
    if (values.length < 2) return false;
    return [0, 1, 2].some((axis) => {
      const channel = values.map((value) => value[axis]);
      return Math.max(...channel) - Math.min(...channel) > 0.01;
    });
  });
  const keyframes = keyed.reduce((sum, animator) => sum + animator.keyframes.length, 0);
  const rotation = keyed.reduce(
    (sum, animator) => sum + animator.keyframes.filter((frame) => frame.channel === 'rotation').length,
    0,
  );
  const position = keyed.reduce(
    (sum, animator) => sum + animator.keyframes.filter((frame) => frame.channel === 'position').length,
    0,
  );
  return {
    name: animation.name,
    keyedBones: keyed.length,
    coverage: boneCount ? keyed.length / boneCount : 0,
    keyframes,
    rotation,
    position,
    length: Number(animation.length || 0),
    loop: animation.loop,
  };
}

function add(reasons, code, detail) {
  reasons.push(detail ? `${code}:${detail}` : code);
}

function validateManifest(filePath, blueprintPath, strict, requiredAnimations, animationNames, reasons) {
  if (!strict) return null;
  if (!fs.existsSync(filePath)) {
    add(reasons, 'QUALITY_MANIFEST_MISSING');
    return null;
  }

  let manifest;
  try {
    manifest = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch (error) {
    add(reasons, 'QUALITY_MANIFEST_INVALID_JSON', error.message);
    return null;
  }

  if (![1, 2].includes(manifest.schemaVersion)) add(reasons, 'QUALITY_MANIFEST_SCHEMA');
  if (manifest.profile !== 'boss') add(reasons, 'QUALITY_PROFILE_NOT_BOSS');
  if (!manifest.archetype || !manifest.silhouetteDominant) add(reasons, 'DESIGN_BRIEF_INCOMPLETE');
  if (!Array.isArray(manifest.forbiddenTraits) || manifest.forbiddenTraits.length < 3) {
    add(reasons, 'FORBIDDEN_TRAITS_TOO_FEW');
  }
  if (!Array.isArray(manifest.references) || manifest.references.length < 2) {
    add(reasons, 'REFERENCES_TOO_FEW');
  }
  if (manifest.blueprintSha256 !== sha256(blueprintPath)) add(reasons, 'STALE_REVIEW_HASH');
  if (!Number.isInteger(manifest.iteration) || manifest.iteration < 2) add(reasons, 'ITERATIONS_TOO_FEW');
  if (!Array.isArray(manifest.knownIssues) || manifest.knownIssues.length !== 0) add(reasons, 'KNOWN_ISSUES_REMAIN');

  for (const category of REVIEW_CATEGORIES) {
    const score = manifest.reviewScores?.[category];
    if (!Number.isFinite(score) || score < 4 || score > 5) add(reasons, 'REVIEW_SCORE_FAIL', category);
  }

  const manifestAnimations = new Set(manifest.requiredAnimations || []);
  for (const animation of requiredAnimations) {
    if (!manifestAnimations.has(animation)) add(reasons, 'MANIFEST_ANIMATION_MISSING', animation);
  }
  for (const animation of manifestAnimations) {
    if (!animationNames.has(animation)) add(reasons, 'BLUEPRINT_ANIMATION_MISSING', animation);
  }

  const evidence = [...(manifest.renderEvidence || []), ...(manifest.runtimeEvidence || [])];
  if ((manifest.renderEvidence || []).length < 6) add(reasons, 'RENDER_EVIDENCE_TOO_FEW');
  if ((manifest.runtimeEvidence || []).length < 1) add(reasons, 'RUNTIME_EVIDENCE_MISSING');
  for (const relative of evidence) {
    const resolved = path.resolve(path.dirname(filePath), relative);
    if (!fs.existsSync(resolved)) add(reasons, 'EVIDENCE_FILE_MISSING', relative);
  }
  return manifest;
}

function inspectBlueprint(ref, options = {}) {
  const blueprintPath = options.blueprintPath
    || path.join(BLUEPRINTS_ROOT, ref.modelId, `${ref.modelId}.bbmodel`);
  const manifestPath = options.manifestPath
    || path.join(QUALITY_ROOT, `${ref.modelId}.quality.json`);
  const reasons = [];
  const warnings = [];
  const strict = ref.template === 'level_1_base_boss' || options.strict === true;

  if (!fs.existsSync(blueprintPath)) {
    return { verdict: 'FAIL', reasons: ['BLUEPRINT_MISSING'], warnings, blueprintPath, manifestPath, strict };
  }

  let data;
  try {
    data = JSON.parse(fs.readFileSync(blueprintPath, 'utf8'));
  } catch (error) {
    return {
      verdict: 'FAIL',
      reasons: [`BLUEPRINT_INVALID_JSON:${error.message}`],
      warnings,
      blueprintPath,
      manifestPath,
      strict,
    };
  }

  const elements = data.elements || [];
  const bones = flattenBones(data.outliner || []);
  const visualBones = bones.filter((bone) => bone.name !== 'hitbox');
  const hitboxes = bones.filter((bone) => bone.name === 'hitbox');
  const animations = data.animations || [];
  const animationNames = new Set(animations.map((animation) => animation.name));
  const requiredAnimations = new Set(strict ? ['idle', 'walk', 'death', ...(ref.stateAnimations || [])] : (ref.stateAnimations || []));
  const ids = [
    ...elements.map((element) => element.uuid),
    ...bones.map((bone) => bone.uuid),
    ...animations.map((animation) => animation.uuid),
  ].filter(Boolean);
  const duplicateIds = ids.filter((id, index) => ids.indexOf(id) !== index);
  if (duplicateIds.length) add(reasons, 'DUPLICATE_UUID', [...new Set(duplicateIds)].join(','));

  const outlinerIds = elementIdsInOutliner(data.outliner || []);
  const knownElements = new Set(elements.map((element) => element.uuid));
  const assignedCounts = new Map();
  for (const id of outlinerIds) {
    assignedCounts.set(id, (assignedCounts.get(id) || 0) + 1);
    if (!knownElements.has(id)) add(reasons, 'OUTLINER_ELEMENT_MISSING', id);
  }
  for (const element of elements) {
    const count = assignedCounts.get(element.uuid) || 0;
    if (count === 0) add(reasons, 'ORPHAN_ELEMENT', element.name || element.uuid);
    if (count > 1) add(reasons, 'ELEMENT_ASSIGNED_MULTIPLE_TIMES', element.name || element.uuid);
    if (!finiteVector(element.from) || !finiteVector(element.to)) {
      add(reasons, 'INVALID_CUBE_VECTOR', element.name || element.uuid);
      continue;
    }
    if (element.to.some((value, axis) => Number(value) <= Number(element.from[axis]))) {
      add(reasons, 'NON_POSITIVE_CUBE', element.name || element.uuid);
    }
  }

  if (!data.textures?.length) add(reasons, 'TEXTURE_MISSING');
  const resolution = data.resolution || {};
  if (!Number.isFinite(resolution.width) || !Number.isFinite(resolution.height)) add(reasons, 'TEXTURE_RESOLUTION_INVALID');
  for (const element of elements) {
    for (const face of Object.values(element.faces || {})) {
      if (!Array.isArray(face.uv) || face.uv.length !== 4) continue;
      const [u1, v1, u2, v2] = face.uv.map(Number);
      if (Math.min(u1, u2) < 0 || Math.max(u1, u2) > resolution.width
        || Math.min(v1, v2) < 0 || Math.max(v1, v2) > resolution.height) {
        add(reasons, 'UV_OUT_OF_BOUNDS', element.name || element.uuid);
      }
    }
  }

  if (strict) {
    if (visualBones.length < 18) add(reasons, 'BOSS_RIG_TOO_SIMPLE', String(visualBones.length));
    if (elements.filter((element) => element.name !== 'hitbox').length < 36) {
      add(reasons, 'BOSS_FORM_TOO_UNDERDEVELOPED', String(elements.length - 1));
    }
    if (Math.max(0, ...visualBones.map((bone) => bone.depth)) < 3) add(reasons, 'HIERARCHY_TOO_FLAT');
    for (const name of ['body', 'h_head', 'h_jaw']) {
      if (!visualBones.some((bone) => bone.name === name)) add(reasons, 'RIG_BONE_MISSING', name);
    }
    for (const side of ['left', 'right']) {
      for (const part of ['leg', 'shin', 'foot', 'arm', 'forearm', 'hand']) {
        const name = `${side}_${part}`;
        if (!visualBones.some((bone) => bone.name === name)) add(reasons, 'RIG_BONE_MISSING', name);
      }
    }
  }

  if (strict && hitboxes.length !== 1) add(reasons, 'HITBOX_COUNT_INVALID', String(hitboxes.length));
  if (!strict && hitboxes.length > 1) add(reasons, 'HITBOX_COUNT_INVALID', String(hitboxes.length));
  const hitbox = hitboxes[0];
  if (hitbox) {
    if (hitbox.parent !== null) add(reasons, 'HITBOX_NOT_ROOT');
    if (hitbox.visibility !== false) add(reasons, 'HITBOX_VISIBLE');
    const hitboxElements = (hitbox.children || []).filter((child) => typeof child === 'string').map((id) => knownElements.has(id) && elements.find((element) => element.uuid === id)).filter(Boolean);
    if (hitboxElements.length !== 1) add(reasons, 'HITBOX_CUBE_COUNT_INVALID', String(hitboxElements.length));
    const cube = hitboxElements[0];
    if (cube) {
      const size = cube.to.map((value, axis) => value - cube.from[axis]);
      if (Math.abs(size[0] - size[2]) > 0.001) add(reasons, 'HITBOX_NOT_SQUARE_XZ');
      const bounds = getBounds(elements);
      if (bounds) {
        if (size[0] > bounds.size[0] * 0.8) add(reasons, 'HITBOX_TOO_WIDE');
        if (size[1] > bounds.size[1] * 1.1 || size[1] < bounds.size[1] * 0.45) add(reasons, 'HITBOX_HEIGHT_IMPLAUSIBLE');
      }
    }
  }

  for (const required of requiredAnimations) {
    if (!animationNames.has(required)) add(reasons, 'ANIMATION_MISSING', required);
  }
  const stats = animations.map((animation) => animationStats(animation, visualBones.length));
  for (const stat of stats) {
    if (stat.length <= 0) add(reasons, 'ANIMATION_LENGTH_INVALID', stat.name);
    if (strict && requiredAnimations.has(stat.name)) {
      if (stat.coverage < 0.55) add(reasons, 'ANIMATION_COVERAGE_LOW', `${stat.name}:${stat.coverage.toFixed(2)}`);
      if (stat.keyframes < stat.keyedBones * 3) add(reasons, 'ANIMATION_KEYFRAME_DENSITY_LOW', stat.name);
      if (stat.rotation === 0) add(reasons, 'ANIMATION_ROTATION_MISSING', stat.name);
    }
    if (['idle', 'walk'].includes(stat.name) && stat.loop !== 'loop') add(reasons, 'LOOP_STATE_NOT_LOOPED', stat.name);
    if (stat.name === 'death' && stat.loop === 'loop') add(reasons, 'DEATH_MUST_NOT_LOOP');
  }

  const bounds = getBounds(elements);
  if (bounds && Math.abs(bounds.min[1]) > 1) warnings.push(`GROUND_CONTACT_OFFSET:${bounds.min[1].toFixed(2)}`);
  const manifest = validateManifest(manifestPath, blueprintPath, strict, requiredAnimations, animationNames, reasons);

  return {
    verdict: reasons.length ? 'FAIL' : 'PASS',
    reasons,
    warnings,
    blueprintPath,
    manifestPath,
    strict,
    stats: {
      elements: elements.length,
      visualBones: visualBones.length,
      maxDepth: Math.max(0, ...visualBones.map((bone) => bone.depth)),
      animations: stats,
      bounds,
    },
    manifest,
  };
}

function main() {
  const references = listFiles(MOBS_ROOT, '.mob.yml').flatMap(parseMobModels);
  const requestedModel = process.argv.includes('--model')
    ? process.argv[process.argv.indexOf('--model') + 1]
    : null;
  const selected = requestedModel ? references.filter((ref) => ref.modelId === requestedModel) : references;
  let failures = 0;

  for (const ref of selected) {
    const result = inspectBlueprint(ref);
    console.log(`${result.verdict} mob=${ref.mobId} model=${ref.modelId}`);
    console.log(`  blueprint=${path.relative(ROOT, result.blueprintPath)}`);
    console.log(`  manifest=${path.relative(ROOT, result.manifestPath)}`);
    if (result.stats) {
      console.log(`  stats=elements:${result.stats.elements} bones:${result.stats.visualBones} depth:${result.stats.maxDepth} animations:${result.stats.animations.length}`);
      for (const animation of result.stats.animations) {
        console.log(`  animation=${animation.name} coverage:${animation.coverage.toFixed(2)} keyed:${animation.keyedBones} frames:${animation.keyframes}`);
      }
    }
    for (const warning of result.warnings) console.log(`  warning=${warning}`);
    console.log(`  quality=${result.reasons.length ? result.reasons.join(',') : 'ok'}`);
    if (result.reasons.length) failures++;
  }

  if (!selected.length) {
    console.error(`MODELENGINE_QUALITY_FAIL: model not declared: ${requestedModel || 'none'}`);
    process.exit(1);
  }
  if (failures) {
    console.error(`MODELENGINE_QUALITY_FAIL: ${failures} model quality issue(s) found.`);
    process.exit(1);
  }
  console.log('MODELENGINE_QUALITY_PASS: structure, rig, animation, hitbox, texture, and current evidence gates passed.');
}

if (require.main === module) main();

module.exports = {
  REVIEW_CATEGORIES,
  animationStats,
  inspectBlueprint,
  parseMobModels,
  sha256,
};
