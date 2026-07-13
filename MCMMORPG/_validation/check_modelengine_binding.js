const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const MOBS_ROOT = path.join(ROOT, 'plugins', 'MythicMobs', 'Packs');
const BLUEPRINTS_ROOT = path.join(ROOT, 'plugins', 'ModelEngine', 'blueprints');
const CACHE_PATH = path.join(ROOT, 'plugins', 'ModelEngine', '.data', 'cache.json');
const GENERATED_MODELS_ROOT = path.join(ROOT, 'plugins', 'ModelEngine', 'resource pack', 'assets', 'modelengine', 'models');

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

  for (const rawLine of lines) {
    const line = rawLine.replace(/\t/g, '    ');
    const topLevel = /^([a-z0-9_]+):\s*$/.exec(line);
    if (topLevel) {
      currentMobId = topLevel[1];
      inModelBlock = false;
      continue;
    }
    if (!currentMobId) continue;
    if (/^\s{2}Model:\s*$/.test(line)) {
      inModelBlock = true;
      continue;
    }
    if (inModelBlock) {
      const modelId = /^\s{4}Id:\s*([a-z0-9_]+)/.exec(line);
      if (modelId) {
        results.push({
          mobId: currentMobId,
          modelId: modelId[1],
          filePath,
        });
        inModelBlock = false;
        continue;
      }
      if (/^\s{2}[A-Z][A-Za-z0-9_-]*:/.test(line) || /^\S/.test(line)) {
        inModelBlock = false;
      }
    }
  }
  return results;
}

function readCompiledIds() {
  const cache = JSON.parse(fs.readFileSync(CACHE_PATH, 'utf8'));
  const compiled = new Set();
  const armor = cache?.cache?.LEATHER_HORSE_ARMOR?.cachedId || {};
  for (const key of Object.keys(armor)) compiled.add(key);
  return compiled;
}

function hasBlueprint(modelId) {
  const filePath = path.join(BLUEPRINTS_ROOT, modelId, `${modelId}.bbmodel`);
  return fs.existsSync(filePath) ? filePath : null;
}

function compiledEntries(modelId, compiledIds) {
  return [...compiledIds].filter((entry) => entry.startsWith(`${modelId}:`));
}

function renderedBoneCount(blueprintPath) {
  const data = JSON.parse(fs.readFileSync(blueprintPath, 'utf8'));
  let count = 0;
  function walk(nodes) {
    for (const node of nodes || []) {
      if (!node || typeof node !== 'object' || Array.isArray(node)) continue;
      if (node.name !== 'hitbox' && (node.children || []).some((child) => typeof child === 'string')) count++;
      walk(node.children);
    }
  }
  walk(data.outliner || []);
  return count;
}

function main() {
  const mobFiles = listFiles(MOBS_ROOT, '.mob.yml');
  const references = mobFiles.flatMap(parseMobModels);
  const compiledIds = readCompiledIds();
  let failures = 0;

  if (!references.length) {
    console.log('NO_MODELS_DECLARED: no ModelEngine mob references found in active mob YAML files.');
    return;
  }

  for (const ref of references) {
    const blueprintPath = hasBlueprint(ref.modelId);
    const compiled = compiledEntries(ref.modelId, compiledIds);
    const expectedParts = blueprintPath ? renderedBoneCount(blueprintPath) : 0;
    const compiledOk = compiled.length >= expectedParts && compiled.length > 0;
    const generatedAssets = path.join(GENERATED_MODELS_ROOT, ref.modelId);
    const generatedOk = fs.existsSync(generatedAssets)
      && fs.readdirSync(generatedAssets).some((entry) => entry.endsWith('.json'));
    const status = [];
    if (!blueprintPath) status.push('BLUEPRINT_MISSING');
    if (!compiledOk) status.push('CACHE_INCOMPLETE');
    if (!generatedOk) status.push('RESOURCE_PACK_ASSETS_MISSING');
    const verdict = status.length ? 'FAIL' : 'PASS';
    console.log(`${verdict} mob=${ref.mobId} model=${ref.modelId}`);
    console.log(`  yaml=${path.relative(ROOT, ref.filePath)}`);
    console.log(`  blueprint=${blueprintPath ? path.relative(ROOT, blueprintPath) : 'missing'}`);
    console.log(`  cache=${compiled.length} compiled entries for ${expectedParts} rendered bone(s)`);
    console.log(`  resource_pack=${generatedOk ? path.relative(ROOT, generatedAssets) : 'missing generated model assets'}`);
    if (status.length) failures++;
  }

  if (failures) {
    console.error(`MODELENGINE_BINDING_FAIL: ${failures} model binding issue(s) found.`);
    process.exit(1);
  }

  console.log('MODELENGINE_BINDING_PASS: every declared mob model has a blueprint and compiled cache entries.');
}

main();
