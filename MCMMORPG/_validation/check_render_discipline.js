#!/usr/bin/env node

const fs = require('fs');
const { loadRenderLog } = require('./lib/reference_clone');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

function main() {
  const modelPath = arg('model');
  const maxDelta = Number(arg('max-new-elements', '3'));
  if (!modelPath) {
    console.error('Required: --model <path.bbmodel> [--max-new-elements 3]');
    process.exit(1);
  }

  const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
  const total = (model.elements || []).length;
  const log = loadRenderLog(modelPath);

  console.log(`model: ${modelPath}`);
  console.log(`elements: ${total}`);
  console.log(`render_log entries: ${log.length}`);

  if (log.length === 0) {
    console.log('GATE: FAIL');
    console.log(`  - no render on file for this model yet (no ${modelPath}.render_log.json) — run modelengine:webgl-view or modelengine:quick-view before continuing`);
    process.exit(1);
  }

  const last = log[log.length - 1];
  const delta = total - last.elementCount;
  console.log(`last render: ${last.timestamp} at ${last.elementCount} elements (tool: ${last.tool})`);

  if (delta > maxDelta) {
    console.log('GATE: FAIL');
    console.log(`  - ${delta} elements added since the last render (threshold ${maxDelta}) — this is batched freehand authoring with no visual check in between; render after each element or tight cluster instead`);
    process.exit(1);
  }
  if (delta < 0) {
    console.log('GATE: FAIL');
    console.log(`  - model now has fewer elements (${total}) than at last render (${last.elementCount}) — render log is stale or elements were removed without a fresh render; re-render before continuing`);
    process.exit(1);
  }

  console.log('GATE: PASS');
}

main();
