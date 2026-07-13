#!/usr/bin/env node
// CLI: pull a real bone/group subtree out of a reference .bbmodel and merge it
// into a target .bbmodel, with provenance recorded so check_shell_provenance.js
// can prove the shell came from real geometry, not hand-typed coordinates.
//
// Usage:
//   node clone_reference_shell.js --ref <reference.bbmodel> --group <groupName> \
//     --target <target.bbmodel> [--into <parentGroupInTarget>] \
//     [--scale sx,sy,sz] [--translate tx,ty,tz] [--prefix name_]

const fs = require('fs');
const {
  loadModel, saveModel, findGroup, cloneSubtree, transformElements,
  renamePrefix, remapTextures, emptyModel, mergeIntoTarget, recordProvenance,
} = require('./lib/reference_clone');

function arg(name, fallback = null) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : fallback;
}
function parseVec3(s, fallback) {
  if (!s) return fallback;
  return s.split(',').map(Number);
}

function main() {
  const refPath = arg('ref');
  const groupName = arg('group');
  const targetPath = arg('target');
  const into = arg('into', null);
  const scale = parseVec3(arg('scale'), [1, 1, 1]);
  const translate = parseVec3(arg('translate'), [0, 0, 0]);
  const prefix = arg('prefix', '');

  if (!refPath || !groupName || !targetPath) {
    console.error('Required: --ref <path> --group <name> --target <path>');
    process.exit(1);
  }

  const refModel = loadModel(refPath);
  const groupNode = findGroup(refModel, groupName);
  if (!groupNode) {
    console.error(`Group "${groupName}" not found in ${refPath}`);
    process.exit(1);
  }

  const targetModel = fs.existsSync(targetPath) ? loadModel(targetPath) : emptyModel(require('path').basename(targetPath, '.bbmodel'));

  const clone = cloneSubtree(refModel, groupNode);
  if (prefix) renamePrefix(clone.root, clone.elements, prefix);
  transformElements(clone.elements, { scale, translate });
  remapTextures(clone.elements, refModel, targetModel);
  mergeIntoTarget(targetModel, clone, into);

  saveModel(targetPath, targetModel);
  recordProvenance(targetPath, refPath, clone.elements);

  console.log(`Cloned "${groupName}" from ${refPath}`);
  console.log(`  elements cloned: ${clone.elements.length}`);
  console.log(`  written to: ${targetPath}`);
  console.log(`  provenance: ${targetPath}.provenance.json`);
}

main();
