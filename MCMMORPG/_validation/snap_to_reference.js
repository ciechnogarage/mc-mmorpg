#!/usr/bin/env node

// Snaps one corner of a freehand element onto the nearest real surface point
// in the model's already-cloned reference geometry (or an external reference
// file), instead of eyeballing a coordinate and hoping it touches the body.
// This does not replace the sculpt loop's render-judge-adjust cycle — it
// removes the "is this actually touching the surface or floating 0.4 units
// off it" guesswork from placement, which typed coordinates are bad at and a
// GUI's snapping/vertex-pick tools are good at.
//
// Simplification: candidate surface points are the raw `from`/`to` corners of
// other elements (not walked through the full bone-parent chain). This is
// exact for elements attached near the root or with identity ancestor
// transforms — which covers most freehand accents attached directly onto a
// cloned shell bone — but will be off for deeply rotated/nested bones. If the
// snap distance reported looks too large to be believable, that's the signal
// this simplification doesn't hold here; use webgl-view to confirm visually.

const fs = require('fs');
const { loadModel, saveModel } = require('./lib/reference_clone');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

function corners(from, to) {
  const [x1, y1, z1] = from;
  const [x2, y2, z2] = to;
  return [
    [x1, y1, z1], [x2, y1, z1], [x2, y2, z1], [x1, y2, z1],
    [x1, y1, z2], [x2, y1, z2], [x2, y2, z2], [x1, y2, z2],
  ];
}

function dist(a, b) {
  return Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]);
}

function main() {
  const targetPath = arg('target');
  const elementName = arg('element');
  const cornerArg = arg('corner');
  const refPath = arg('reference');
  const excludePrefix = arg('exclude-prefix', elementName);

  if (!targetPath || !elementName) {
    console.error('Usage: node snap_to_reference.js --target <path.bbmodel> --element <name> [--corner 0-7] [--reference <other.bbmodel>]');
    process.exit(1);
  }

  const model = loadModel(targetPath);
  const element = model.elements.find((e) => e.name === elementName);
  if (!element) {
    console.error(`Element "${elementName}" not found in ${targetPath}`);
    process.exit(1);
  }

  const refModel = refPath ? JSON.parse(fs.readFileSync(refPath, 'utf8')) : model;
  const candidates = [];
  for (const el of refModel.elements || []) {
    if (el.uuid === element.uuid) continue;
    if (excludePrefix && el.name && el.name.startsWith(excludePrefix)) continue;
    candidates.push(...corners(el.from, el.to));
  }
  if (!candidates.length) {
    console.error('No candidate reference geometry found (model has nothing else to snap onto)');
    process.exit(1);
  }

  const elCorners = corners(element.from, element.to);
  const cornersToTry = cornerArg !== null ? [Number(cornerArg)] : [0, 1, 2, 3, 4, 5, 6, 7];

  let best = null;
  for (const ci of cornersToTry) {
    const point = elCorners[ci];
    for (const candidate of candidates) {
      const d = dist(point, candidate);
      if (!best || d < best.distance) best = { cornerIndex: ci, point, candidate, distance: d };
    }
  }

  const translation = best.candidate.map((v, i) => v - best.point[i]);
  element.from = element.from.map((v, i) => v + translation[i]);
  element.to = element.to.map((v, i) => v + translation[i]);
  element.origin = (element.origin || [0, 0, 0]).map((v, i) => v + translation[i]);

  saveModel(targetPath, model);

  console.log(`Snapped "${elementName}" corner ${best.cornerIndex} onto nearest reference point`);
  console.log(`  moved by: [${translation.map((v) => v.toFixed(2)).join(', ')}]`);
  console.log(`  distance before snap: ${best.distance.toFixed(2)}`);
  if (best.distance > 20) {
    console.log('  WARNING: snap distance is large — the nearest candidate point may not be the intended surface. Check with webgl-view before trusting this.');
  }
}

main();
