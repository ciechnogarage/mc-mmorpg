#!/usr/bin/env node

// Deliberately NOT a wholesale swap of the bulk corpus atlas
// (render_model_reference_corpus.js + build_model_review_sheets.js) to WebGL.
// That pipeline renders every model in the reference corpus across every
// animation phase and static view (potentially thousands of frames) via
// child-process-isolated calls into the flat 2D renderer, with its own
// resume/fallback/memory-pressure tracking tuned to that renderer's failure
// modes. Puppeteer/WebGL's per-frame cost (browser navigation + up to 5
// SwiftShader-validation retries) would multiply total corpus render time by
// roughly an order of magnitude and doesn't fit the existing resume/fallback
// model at all — that's a real regression, not a quality upgrade, for a bulk
// QA/thumbnail job.
//
// What actually needed the better renderer was judging ONE model at a time
// (the sculpt loop, picking/verifying a reference before cloning it) — and
// modelengine:webgl-view already does that for any single .bbmodel path. The
// one real gap was that picking a specific model out of the corpus by id
// meant knowing its exact nested blueprint path by hand. This resolves that:
// give it a corpus id/relative name, it finds the .bbmodel and renders it
// with the real WebGL pipeline.

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const DEFAULT_CORPUS = '/home/przemek/projects/Minecraft/modele/OUTPUT — kopia (2)';
const WEBGL_RENDER = path.join(__dirname, 'webgl_render.js');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

function walkBlueprints(root, acc = []) {
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) walkBlueprints(full, acc);
    else if (entry.isFile() && entry.name.toLowerCase().endsWith('.bbmodel')) acc.push(full);
  }
  return acc;
}

function main() {
  const corpus = path.resolve(arg('corpus', DEFAULT_CORPUS));
  const id = arg('id');
  const out = arg('out');
  const yaw = arg('yaw', '35');
  const pitch = arg('pitch', '-10');
  const size = arg('size', '720');

  if (!id || !out) {
    console.error('Usage: node webgl_corpus_spotcheck.js --id <blueprint-basename-or-relative-path> --out <path.png> [--corpus <dir>] [--yaw 35] [--pitch -10] [--size 720]');
    process.exit(1);
  }

  const blueprintDir = path.join(corpus, 'ModelEngine', 'blueprints');
  const searchRoot = fs.existsSync(blueprintDir) ? blueprintDir : corpus;
  const all = walkBlueprints(searchRoot);
  const matches = all.filter((file) => {
    const base = path.basename(file, '.bbmodel');
    const relative = path.relative(searchRoot, file);
    return base === id || relative === id || file === id || base.includes(id);
  });

  if (matches.length === 0) {
    console.error(`No blueprint matching "${id}" found under ${searchRoot}`);
    process.exit(1);
  }
  if (matches.length > 1) {
    console.error(`Ambiguous id "${id}" matches ${matches.length} blueprints:`);
    for (const m of matches) console.error(`  - ${path.relative(searchRoot, m)}`);
    console.error('Pass a more specific id or a full relative path.');
    process.exit(1);
  }

  const blueprint = matches[0];
  console.log(`resolved "${id}" -> ${blueprint}`);
  const result = spawnSync(process.execPath, [
    WEBGL_RENDER,
    '--model', blueprint,
    '--out', out,
    '--yaw', yaw,
    '--pitch', pitch,
    '--size', size,
  ], { stdio: 'inherit' });
  process.exit(result.status ?? 1);
}

main();
