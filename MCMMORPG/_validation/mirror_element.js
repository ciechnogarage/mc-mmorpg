#!/usr/bin/env node

// Mirrors a bone/group subtree across an axis plane (typically the model's
// own left/right split) instead of hand-retyping the other side's
// coordinates. Retyping a mirrored limb is exactly the kind of blind
// coordinate authoring this whole tool set exists to avoid.

const crypto = require('crypto');
const {
  loadModel, saveModel, findGroup,
} = require('./lib/reference_clone');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

const AXES = { x: 0, y: 1, z: 2 };
const FACE_MIRROR = {
  x: { east: 'west', west: 'east', north: 'north', south: 'south', up: 'up', down: 'down' },
  y: { up: 'down', down: 'up', north: 'north', south: 'south', east: 'east', west: 'west' },
  z: { north: 'south', south: 'north', east: 'east', west: 'west', up: 'up', down: 'down' },
};

function mirrorValue(v, axisIndex, center, targetAxisIndex) {
  return axisIndex === targetAxisIndex ? 2 * center - v : v;
}

function mirrorPoint(point, axisIndex, center) {
  return point.map((v, i) => mirrorValue(v, i, center, axisIndex));
}

function mirrorRotation(rotation, axisIndex) {
  // Mirroring across a plane flips the sign of rotation about the other two axes.
  return (rotation || [0, 0, 0]).map((v, i) => (i === axisIndex ? v : -v));
}

function mirrorElement(element, axisIndex, center, prefix) {
  const from = mirrorPoint(element.from, axisIndex, center);
  const to = mirrorPoint(element.to, axisIndex, center);
  const fixedFrom = from.map((v, i) => Math.min(v, to[i]));
  const fixedTo = from.map((v, i) => Math.max(v, to[i]));
  const faceMap = FACE_MIRROR[['x', 'y', 'z'][axisIndex]];
  const faces = {};
  for (const [faceName, face] of Object.entries(element.faces || {})) {
    faces[faceMap[faceName]] = { ...face };
  }
  return {
    ...element,
    uuid: crypto.randomUUID(),
    name: `${prefix}${element.name}`,
    from: fixedFrom,
    to: fixedTo,
    origin: mirrorPoint(element.origin || [0, 0, 0], axisIndex, center),
    rotation: mirrorRotation(element.rotation, axisIndex),
    faces,
    _mirroredFrom: element.uuid,
  };
}

function walkGroup(model, group, axisIndex, center, prefix) {
  const idMap = new Map();
  const newElements = [];

  function cloneNode(node) {
    const mirroredChildren = (node.children || []).map((child) => {
      if (typeof child === 'string') {
        const element = model.elements.find((e) => e.uuid === child);
        if (!element) return child;
        const mirrored = mirrorElement(element, axisIndex, center, prefix);
        newElements.push(mirrored);
        idMap.set(child, mirrored.uuid);
        return mirrored.uuid;
      }
      return cloneNode(child);
    });
    return {
      ...node,
      uuid: crypto.randomUUID(),
      name: `${prefix}${node.name}`,
      origin: mirrorPoint(node.origin || [0, 0, 0], axisIndex, center),
      rotation: mirrorRotation(node.rotation, axisIndex),
      children: mirroredChildren,
    };
  }

  return { node: cloneNode(group), newElements };
}

function main() {
  const targetPath = arg('target');
  const groupName = arg('group');
  const axis = (arg('axis', 'x') || 'x').toLowerCase();
  const center = Number(arg('center', '0'));
  const prefix = arg('prefix', 'mirror_');
  const into = arg('into');

  if (!targetPath || !groupName || !(axis in AXES)) {
    console.error('Usage: node mirror_element.js --target <path.bbmodel> --group <groupName> [--axis x] [--center 0] [--prefix mirror_] [--into <parentGroupName>]');
    process.exit(1);
  }

  const model = loadModel(targetPath);
  const group = findGroup(model, groupName);
  if (!group) {
    console.error(`Group "${groupName}" not found in ${targetPath}`);
    process.exit(1);
  }

  const axisIndex = AXES[axis];
  const { node, newElements } = walkGroup(model, group, axisIndex, center, prefix);

  model.elements.push(...newElements);
  if (into) {
    const parent = findGroup(model, into);
    if (!parent) {
      console.error(`--into group "${into}" not found`);
      process.exit(1);
    }
    parent.children.push(node);
  } else {
    model.outliner.push(node);
  }

  saveModel(targetPath, model);
  console.log(`Mirrored "${groupName}" across ${axis}=${center} -> "${node.name}"`);
  console.log(`  elements mirrored: ${newElements.length}`);
  console.log(`  written to: ${targetPath}`);
}

main();
