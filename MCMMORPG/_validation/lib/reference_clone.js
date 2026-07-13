// Clone real geometry out of a reference .bbmodel into a target .bbmodel.
// This exists because hand-typed cube coordinates produce generic "box soup" —
// see docs/ai/mc-model-mob-reference-cloning-protocol.md for why this is mandatory.

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function loadModel(p) {
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function saveModel(p, model) {
  fs.writeFileSync(p, JSON.stringify(model, null, 2));
}

function newUuid() {
  return crypto.randomUUID();
}

function isGroup(node) {
  return typeof node === 'object' && node !== null && Array.isArray(node.children);
}

function findGroup(model, name) {
  let found = null;
  function walk(nodes) {
    for (const node of nodes) {
      if (!isGroup(node)) continue;
      if (node.name === name) {
        found = node;
        return;
      }
      walk(node.children);
      if (found) return;
    }
  }
  walk(model.outliner || []);
  return found;
}

// Deep-clone a group subtree: returns { groups: [clonedRootGroup], elements: [clonedElements], uuidMap }
function cloneSubtree(model, groupNode) {
  const elementsByUuid = new Map((model.elements || []).map((e) => [e.uuid, e]));
  const uuidMap = new Map();
  const clonedElements = [];

  function cloneGroup(node) {
    const cloned = JSON.parse(JSON.stringify(node));
    const oldUuid = cloned.uuid;
    cloned.uuid = newUuid();
    if (oldUuid) uuidMap.set(oldUuid, cloned.uuid);
    cloned.children = node.children.map((child) => {
      if (typeof child === 'string') {
        const srcEl = elementsByUuid.get(child);
        if (!srcEl) return child;
        const clonedEl = JSON.parse(JSON.stringify(srcEl));
        const oldElUuid = clonedEl.uuid;
        clonedEl.uuid = newUuid();
        uuidMap.set(oldElUuid, clonedEl.uuid);
        clonedEl._sourceUuid = oldElUuid;
        clonedElements.push(clonedEl);
        return clonedEl.uuid;
      }
      return cloneGroup(child);
    });
    return cloned;
  }

  const rootClone = cloneGroup(groupNode);
  return { root: rootClone, elements: clonedElements, uuidMap };
}

function transformElements(elements, { scale = [1, 1, 1], translate = [0, 0, 0] } = {}) {
  const [sx, sy, sz] = scale;
  const [tx, ty, tz] = translate;
  const scaleVec3 = (v) => [v[0] * sx + tx, v[1] * sy + ty, v[2] * sz + tz];
  for (const el of elements) {
    if (el.from) el.from = scaleVec3(el.from);
    if (el.to) el.to = scaleVec3(el.to);
    if (el.origin) el.origin = scaleVec3(el.origin);
  }
}

function renamePrefix(root, elements, prefix) {
  function walk(node) {
    if (node.name) node.name = `${prefix}${node.name}`;
    for (const child of node.children) {
      if (isGroup(child)) walk(child);
    }
  }
  walk(root);
  for (const el of elements) {
    if (el.name) el.name = `${prefix}${el.name}`;
  }
}

// Map source face.texture (index or uuid) onto target model's texture list,
// appending any source textures the target doesn't have yet (matched by file name).
function remapTextures(elements, sourceModel, targetModel) {
  targetModel.textures = targetModel.textures || [];
  const sourceTextures = sourceModel.textures || [];
  const indexMap = new Map(); // sourceIndex -> targetIndex

  function targetIndexFor(sourceIdx) {
    if (indexMap.has(sourceIdx)) return indexMap.get(sourceIdx);
    const srcTex = sourceTextures[sourceIdx];
    if (!srcTex) return sourceIdx;
    let targetIdx = targetModel.textures.findIndex((t) => t.name === srcTex.name);
    if (targetIdx === -1) {
      const clonedTex = JSON.parse(JSON.stringify(srcTex));
      clonedTex.uuid = newUuid();
      clonedTex.id = String(targetModel.textures.length);
      targetModel.textures.push(clonedTex);
      targetIdx = targetModel.textures.length - 1;
    }
    indexMap.set(sourceIdx, targetIdx);
    return targetIdx;
  }

  for (const el of elements) {
    if (!el.faces) continue;
    for (const faceName of Object.keys(el.faces)) {
      const face = el.faces[faceName];
      if (typeof face.texture === 'number' && face.texture >= 0) {
        face.texture = targetIndexFor(face.texture);
      }
    }
  }
}

function emptyModel(name) {
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name,
    model_identifier: name,
    visible_box: [1, 1, 0],
    resolution: { width: 64, height: 64 },
    elements: [],
    outliner: [],
    textures: [],
    animations: [],
  };
}

function ensureParentGroup(targetModel, parentName) {
  targetModel.outliner = targetModel.outliner || [];
  let parent = findGroup(targetModel, parentName);
  if (parent) return parent;
  parent = {
    name: parentName,
    uuid: newUuid(),
    origin: [0, 0, 0],
    rotation: [0, 0, 0],
    visibility: true,
    children: [],
  };
  targetModel.outliner.push(parent);
  return parent;
}

function mergeIntoTarget(targetModel, cloneResult, parentName) {
  const parent = parentName ? ensureParentGroup(targetModel, parentName) : null;
  targetModel.elements = targetModel.elements || [];
  targetModel.elements.push(...cloneResult.elements);
  if (parent) {
    parent.children.push(cloneResult.root);
  } else {
    targetModel.outliner = targetModel.outliner || [];
    targetModel.outliner.push(cloneResult.root);
  }
}

function loadProvenance(targetPath) {
  const p = `${targetPath}.provenance.json`;
  if (!fs.existsSync(p)) return {};
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function saveProvenance(targetPath, provenance) {
  fs.writeFileSync(`${targetPath}.provenance.json`, JSON.stringify(provenance, null, 2));
}

function recordProvenance(targetPath, refPath, elements) {
  const provenance = loadProvenance(targetPath);
  for (const el of elements) {
    provenance[el.uuid] = { source_ref: refPath, source_uuid: el._sourceUuid, cloned_at: new Date().toISOString() };
    delete el._sourceUuid;
  }
  saveProvenance(targetPath, provenance);
}

function renderLogPath(modelPath) {
  return `${modelPath}.render_log.json`;
}

function loadRenderLog(modelPath) {
  const p = renderLogPath(modelPath);
  return fs.existsSync(p) ? JSON.parse(fs.readFileSync(p, 'utf8')) : [];
}

function logRender(modelPath, elementCount, meta = {}) {
  const log = loadRenderLog(modelPath);
  log.push({ elementCount, timestamp: new Date().toISOString(), ...meta });
  fs.writeFileSync(renderLogPath(modelPath), JSON.stringify(log, null, 2));
}

module.exports = {
  loadModel,
  saveModel,
  findGroup,
  cloneSubtree,
  transformElements,
  renamePrefix,
  remapTextures,
  emptyModel,
  mergeIntoTarget,
  loadProvenance,
  saveProvenance,
  recordProvenance,
  loadRenderLog,
  logRender,
};
