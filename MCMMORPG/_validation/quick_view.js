#!/usr/bin/env node
// Fast single-shot render for the sculpt loop: edit a couple of cubes, look,
// adjust — instead of computing coordinates blindly and checking at the end.
// Usage: node quick_view.js --model <path.bbmodel> --out <path.png> [--yaw 35] [--pitch -10] [--size 480]

const fs = require('fs');
const { render, releaseModelTextures } = require('./render_bbmodel_review');
const { logRender } = require('./lib/reference_clone');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

async function main() {
  const modelPath = arg('model');
  const out = arg('out');
  const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
  await render(model, out, {
    yaw: Number(arg('yaw', 35)),
    pitch: Number(arg('pitch', -10)),
    size: Number(arg('size', 480)),
    title: 'quick_view',
    stableScale: true,
  });
  releaseModelTextures(model);
  logRender(modelPath, (model.elements || []).length, { tool: 'quick_view', out });
  console.log(out);
}

main();
