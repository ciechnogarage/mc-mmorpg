#!/usr/bin/env node

// Catches the two content-quality gaps that clone-shell/gate-shell/render-
// discipline don't touch: a shell that's geometrically real but wears one
// copy-pasted UV stamp everywhere (no actual texture painting happened), and
// a model that's shipped with zero animation authored despite having a
// skeleton that implies it should move. Both are "technically passes the
// other gates but is still an unfinished creature" failures.

const fs = require('fs');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

function uvKey(face) {
  if (!face || !face.uv) return null;
  return face.uv.join(',');
}

function main() {
  const modelPath = arg('model');
  const minUvRatio = Number(arg('min-uv-ratio', '0.15'));
  const requireAnimation = arg('require-animation', 'true') !== 'false';

  if (!modelPath) {
    console.error('Required: --model <path.bbmodel> [--min-uv-ratio 0.15] [--require-animation true|false]');
    process.exit(1);
  }

  const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
  const elements = model.elements || [];

  const uvKeys = new Set();
  let faceCount = 0;
  for (const element of elements) {
    for (const face of Object.values(element.faces || {})) {
      if (!face || face.texture === null || face.texture === undefined) continue;
      faceCount++;
      const key = uvKey(face);
      if (key) uvKeys.add(key);
    }
  }
  const uvRatio = faceCount ? uvKeys.size / faceCount : 0;

  const animations = model.animations || [];
  const realAnimations = animations.filter((a) => {
    const animators = Object.values(a.animators || {});
    return animators.some((animator) => (animator.keyframes || []).length > 0);
  });

  console.log(`model: ${modelPath}`);
  console.log(`elements: ${elements.length}, textured faces: ${faceCount}, distinct UV rects: ${uvKeys.size} (ratio ${uvRatio.toFixed(2)})`);
  console.log(`animations: ${animations.length} declared, ${realAnimations.length} with actual keyframes`);

  const failures = [];
  if (faceCount > 0 && uvRatio < minUvRatio) {
    failures.push(`UV diversity ratio ${uvRatio.toFixed(2)} is below threshold ${minUvRatio} — most faces reuse the same UV rect, which usually means the texture is unpainted/placeholder rather than actually mapped per-surface`);
  }
  if (requireAnimation && realAnimations.length === 0) {
    failures.push('no animation with actual keyframes found — model ships static despite having a skeleton');
  }

  if (failures.length) {
    console.log('GATE: FAIL');
    for (const f of failures) console.log(`  - ${f}`);
    process.exit(1);
  }

  console.log('GATE: PASS');
}

main();
