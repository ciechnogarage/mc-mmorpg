const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const BLUEPRINTS_ROOT = path.join(ROOT, 'plugins', 'ModelEngine', 'blueprints');
const QUALITY_ROOT = path.join(__dirname, 'model_quality');

function flattenBones(nodes, acc = []) {
  for (const node of nodes || []) {
    if (!node || typeof node !== 'object' || Array.isArray(node)) continue;
    acc.push(node);
    flattenBones(node.children, acc);
  }
  return acc;
}

function boneAliases(bones) {
  const output = new Set();
  for (const bone of bones) {
    output.add(bone.name);
    output.add(bone.name.replace(/^(?:h_|b_|ob_|p_|g_|ir_|il_|ih_)/, ''));
  }
  return output;
}

function yamlBlock(text, id) {
  const escaped = id.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const start = new RegExp(`^${escaped}:\\s*$`, 'm').exec(text);
  if (!start) return null;
  const rest = text.slice(start.index + start[0].length);
  const next = /^([A-Za-z0-9_.-]+):\s*$/m.exec(rest);
  return next ? rest.slice(0, next.index) : rest;
}

function stateRefs(text) {
  return [...text.matchAll(/\bstate\{[^}]*\b(?:s|state)=([^;}\s]+)/ig)].map((match) => match[1]);
}

function modelPartRefs(text) {
  return [...text.matchAll(/@modelpart\{([^}]*)}/ig)].flatMap((match) => {
    const part = /(?:^|;)(?:p|pid|part)=([^;}\s]+)/i.exec(match[1]);
    return part ? [part[1]] : [];
  });
}

function mechanicParams(text, mechanic) {
  const escaped = mechanic.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return [...text.matchAll(new RegExp(`${escaped}\\{([^}]*)}`, 'ig'))].map((match) =>
    Object.fromEntries(match[1].split(';').map((entry) => {
      const [key, ...rest] = entry.split('=');
      return [key.trim().toLowerCase(), rest.join('=').trim()];
    }).filter(([key, value]) => key && value)));
}

function delayValues(text) {
  const values = [];
  for (const line of text.split(/\r?\n/)) {
    const standalone = /^\s*-\s*delay\s+(\d+)/i.exec(line);
    if (standalone) values.push(Number(standalone[1]));
    for (const match of line.matchAll(/(?:^|[;{])(?:delay|d)=(\d+)(?:;|})/ig)) values.push(Number(match[1]));
  }
  return values;
}

function inspectModel(modelId, options = {}) {
  const blueprintPath = options.blueprintPath || path.join(BLUEPRINTS_ROOT, modelId, `${modelId}.bbmodel`);
  const manifestPath = options.manifestPath || path.join(QUALITY_ROOT, `${modelId}.quality.json`);
  const reasons = [];
  if (!fs.existsSync(blueprintPath)) return { verdict: 'FAIL', reasons: ['BLUEPRINT_MISSING'] };
  if (!fs.existsSync(manifestPath)) return { verdict: 'FAIL', reasons: ['QUALITY_MANIFEST_MISSING'] };

  const blueprint = JSON.parse(fs.readFileSync(blueprintPath, 'utf8'));
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  if (manifest.schemaVersion < 2) {
    return { verdict: 'SKIP', reasons: ['ECOSYSTEM_MANIFEST_V2_NOT_ENABLED'] };
  }

  const aliases = boneAliases(flattenBones(blueprint.outliner || []));
  const bones = flattenBones(blueprint.outliner || []);
  const animations = new Map((blueprint.animations || []).map((animation) => [animation.name, animation]));
  const contracts = new Map((manifest.animationContract || []).map((contract) => [contract.animation, contract]));

  for (const bone of manifest.interactionBones || []) {
    if (!aliases.has(bone)) reasons.push(`INTERACTION_BONE_MISSING:${bone}`);
  }
  const damageBinding = manifest.damageZoneBinding;
  if ((manifest.damageZones || []).length && !damageBinding) {
    reasons.push('DAMAGE_ZONE_BINDING_MISSING');
  } else if (damageBinding) {
    const skillSource = path.resolve(path.dirname(manifestPath), damageBinding.skillSourceFile || '');
    const mobSource = path.resolve(path.dirname(manifestPath), damageBinding.mobSourceFile || '');
    const skillText = fs.existsSync(skillSource) ? fs.readFileSync(skillSource, 'utf8') : '';
    const mobText = fs.existsSync(mobSource) ? fs.readFileSync(mobSource, 'utf8') : '';
    const block = yamlBlock(skillText, damageBinding.skill || '') || '';
    if (!fs.existsSync(skillSource)) reasons.push(`DAMAGE_ZONE_SKILL_SOURCE_MISSING:${damageBinding.skillSourceFile}`);
    else if (!block) reasons.push(`DAMAGE_ZONE_SKILL_MISSING:${damageBinding.skill}`);
    if (!fs.existsSync(mobSource)) reasons.push(`DAMAGE_ZONE_MOB_SOURCE_MISSING:${damageBinding.mobSourceFile}`);
    for (const hook of damageBinding.requiredHooks || []) {
      const escapedSkill = (damageBinding.skill || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const escapedHook = hook.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const hookPattern = new RegExp(`skill\\{s=${escapedSkill}\\}\\s+@self\\s+~${escapedHook}\\b`, 'i');
      if (!hookPattern.test(mobText)) reasons.push(`DAMAGE_ZONE_HOOK_MISSING:${hook}`);
    }
    const configs = mechanicParams(block, 'hitboxconfig');
    for (const zone of manifest.damageZones || []) {
      const bone = bones.find((candidate) => candidate.name === zone.bone);
      if (!bone) {
        reasons.push(`DAMAGE_ZONE_BONE_MISSING:${zone.bone}`);
        continue;
      }
      if (bone.visibility !== false) reasons.push(`DAMAGE_ZONE_VISIBLE:${zone.bone}`);
      const cubes = (bone.children || []).filter((child) => typeof child === 'string');
      if (cubes.length !== 1) reasons.push(`DAMAGE_ZONE_CUBE_COUNT_INVALID:${zone.bone}:${cubes.length}`);
      const config = configs.find((entry) =>
        (entry.model || entry.modelid) === modelId && (entry.part || entry.partid) === zone.bone);
      if (!config) {
        reasons.push(`DAMAGE_ZONE_CONFIG_MISSING:${zone.bone}`);
        continue;
      }
      if (Math.abs(Number(config.pass) - Number(zone.multiplier)) > 0.0001) {
        reasons.push(`DAMAGE_ZONE_MULTIPLIER_MISMATCH:${zone.bone}:${config.pass}`);
      }
    }
  }
  for (const contract of manifest.animationContract || []) {
    const animation = animations.get(contract.animation);
    if (!animation) {
      reasons.push(`CONTRACT_ANIMATION_MISSING:${contract.animation}`);
      continue;
    }
    const maxTick = Math.round(Number(animation.length || 0) * 20);
    for (const tick of contract.impactTicks || []) {
      if (!Number.isInteger(tick) || tick < 0 || tick > maxTick) {
        reasons.push(`IMPACT_TICK_OUTSIDE_ANIMATION:${contract.animation}:${tick}>${maxTick}`);
      }
    }
    for (const bone of contract.bones || []) {
      if (!aliases.has(bone)) reasons.push(`CONTRACT_BONE_MISSING:${contract.animation}:${bone}`);
    }
  }

  for (const binding of manifest.skillBindings || []) {
    const sourcePath = path.resolve(path.dirname(manifestPath), binding.sourceFile || '');
    if (!fs.existsSync(sourcePath)) {
      reasons.push(`SKILL_SOURCE_MISSING:${binding.sourceFile}`);
      continue;
    }
    const source = fs.readFileSync(sourcePath, 'utf8');
    const block = yamlBlock(source, binding.skill);
    if (!block) {
      reasons.push(`SKILL_DEFINITION_MISSING:${binding.skill}`);
      continue;
    }
    if (!animations.has(binding.animation)) reasons.push(`BINDING_ANIMATION_MISSING:${binding.skill}:${binding.animation}`);
    if (!contracts.has(binding.animation)) reasons.push(`BINDING_CONTRACT_MISSING:${binding.skill}:${binding.animation}`);
    if (!stateRefs(block).includes(binding.animation)) reasons.push(`SKILL_STATE_NOT_PLAYED:${binding.skill}:${binding.animation}`);
    const parts = new Set(modelPartRefs(block));
    for (const bone of binding.modelParts || []) {
      if (!aliases.has(bone)) reasons.push(`SKILL_BONE_MISSING:${binding.skill}:${bone}`);
      if (!parts.has(bone)) reasons.push(`SKILL_MODELPART_NOT_USED:${binding.skill}:${bone}`);
    }
    const contract = contracts.get(binding.animation);
    const impacts = new Set(contract?.impactTicks || []);
    const tolerance = Number(binding.timingToleranceTicks ?? 3);
    const delays = delayValues(block);
    for (const tick of binding.impactTicks || []) {
      if (!impacts.has(tick)) reasons.push(`SKILL_IMPACT_NOT_IN_CONTRACT:${binding.skill}:${tick}`);
      if (!delays.some((delay) => Math.abs(delay - tick) <= tolerance)) {
        reasons.push(`SKILL_IMPACT_DELAY_MISSING:${binding.skill}:${tick}`);
      }
    }
    if (binding.requiresGcd && !/\bgcd\{/i.test(block)) reasons.push(`SKILL_GCD_MISSING:${binding.skill}`);
    if (binding.requiresModelLock && !/\blockmodel\{/i.test(block)) reasons.push(`SKILL_MODEL_LOCK_MISSING:${binding.skill}`);
  }

  for (const integration of manifest.integrationFiles || []) {
    const filePath = path.resolve(path.dirname(manifestPath), integration);
    if (!fs.existsSync(filePath)) reasons.push(`INTEGRATION_FILE_MISSING:${integration}`);
  }

  return {
    verdict: reasons.length ? 'FAIL' : 'PASS',
    reasons,
    blueprintPath,
    manifestPath,
    stats: {
      bones: aliases.size,
      animations: animations.size,
      contracts: contracts.size,
      bindings: (manifest.skillBindings || []).length,
    },
  };
}

function main() {
  const requested = process.argv.includes('--model') ? process.argv[process.argv.indexOf('--model') + 1] : null;
  const modelDirs = fs.readdirSync(BLUEPRINTS_ROOT, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .filter((modelId) => !requested || modelId === requested);
  let failures = 0;
  for (const modelId of modelDirs) {
    const manifestPath = path.join(QUALITY_ROOT, `${modelId}.quality.json`);
    if (!fs.existsSync(manifestPath)) continue;
    const result = inspectModel(modelId);
    console.log(`${result.verdict} model=${modelId}`);
    if (result.stats) console.log(`  stats=bones:${result.stats.bones} animations:${result.stats.animations} contracts:${result.stats.contracts} bindings:${result.stats.bindings}`);
    console.log(`  ecosystem=${result.reasons.length ? result.reasons.join(',') : 'ok'}`);
    if (result.verdict === 'FAIL') failures++;
  }
  if (failures) {
    console.error(`MODELENGINE_ECOSYSTEM_FAIL: ${failures} model ecosystem issue(s) found.`);
    process.exit(1);
  }
  console.log('MODELENGINE_ECOSYSTEM_PASS: manifest v2 model, animation, bone, skill, timing, and integration references resolve.');
}

if (require.main === module) main();

module.exports = { delayValues, inspectModel, modelPartRefs, stateRefs, yamlBlock };
