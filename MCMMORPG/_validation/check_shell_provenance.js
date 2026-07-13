#!/usr/bin/env node
// Gate: refuses a model for Layer B / production if its shell wasn't built by
// cloning real reference geometry (clone_reference_shell.js), and refuses if
// the model's reference_shell_lock verdict isn't PASS.
//
// Usage: node check_shell_provenance.js --model <path.bbmodel> [--min-ratio 0.6]

const fs = require('fs');
const path = require('path');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

function main() {
  const modelPath = arg('model');
  const minRatio = Number(arg('min-ratio', '0.6'));
  if (!modelPath) {
    console.error('Required: --model <path.bbmodel>');
    process.exit(1);
  }

  const model = JSON.parse(fs.readFileSync(modelPath, 'utf8'));
  const total = (model.elements || []).length;

  const provPath = `${modelPath}.provenance.json`;
  const provenance = fs.existsSync(provPath) ? JSON.parse(fs.readFileSync(provPath, 'utf8')) : {};
  const elementUuids = new Set((model.elements || []).map((e) => e.uuid));
  const covered = [...elementUuids].filter((u) => provenance[u]).length;
  const ratio = total > 0 ? covered / total : 0;

  const mobId = path.basename(modelPath, '.bbmodel');
  const lockPath = path.join(__dirname, 'model_studies', `${mobId}_reference_shell_lock.md`);
  let lockVerdict = 'MISSING';
  let evidencePaths = [];
  if (fs.existsSync(lockPath)) {
    const text = fs.readFileSync(lockPath, 'utf8');
    const m = text.match(/overall_verdict:\s*(\S+)/);
    lockVerdict = m ? m[1] : 'UNSET';
    const e = text.match(/verdict_evidence:\s*(.+)/);
    if (e) evidencePaths = e[1].split(',').map((p) => p.trim()).filter(Boolean);
  }

  const problems = [];
  if (ratio < minRatio) {
    problems.push(`shell provenance ${covered}/${total} = ${ratio.toFixed(2)} below min-ratio ${minRatio} — too many elements were hand-typed instead of cloned from a reference .bbmodel`);
  }
  if (lockVerdict !== 'PASS') {
    problems.push(`reference_shell_lock overall_verdict = ${lockVerdict} (need PASS) at ${lockPath}`);
  } else {
    // A bare "PASS" is a self-reported claim — require a rendered comparison
    // artifact that actually exists and postdates the current model file, so
    // the verdict can't just be typed in without ever looking at a render.
    const modelMtime = fs.statSync(modelPath).mtimeMs;
    if (evidencePaths.length === 0) {
      problems.push(`overall_verdict is PASS but no verdict_evidence: <render path(s)> field found in ${lockPath} — add one pointing at the comparison render(s) that justify PASS`);
    } else {
      for (const rel of evidencePaths) {
        const evidencePath = path.isAbsolute(rel) ? rel : path.join(__dirname, rel);
        if (!fs.existsSync(evidencePath)) {
          problems.push(`verdict_evidence path does not exist: ${evidencePath}`);
          continue;
        }
        const evidenceMtime = fs.statSync(evidencePath).mtimeMs;
        if (evidenceMtime < modelMtime) {
          problems.push(`verdict_evidence ${evidencePath} is older than the model file — re-render after the latest edit before claiming PASS`);
        }
      }
    }
  }

  console.log(`model: ${modelPath}`);
  console.log(`elements: ${total}, provenance-covered: ${covered} (${(ratio * 100).toFixed(1)}%)`);
  console.log(`shell_lock verdict: ${lockVerdict}`);
  if (evidencePaths.length) console.log(`verdict_evidence: ${evidencePaths.join(', ')}`);

  if (problems.length) {
    console.log('GATE: FAIL');
    for (const p of problems) console.log(`  - ${p}`);
    process.exit(1);
  }
  console.log('GATE: PASS');
}

main();
