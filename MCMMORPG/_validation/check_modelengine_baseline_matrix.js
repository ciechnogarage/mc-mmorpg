const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const { parseMobModels } = require('./check_modelengine_quality');

const ROOT = path.resolve(__dirname, '..');
const PRIMARY_MODELS = [
  'level_1_briarwolf',
  'level_1_glimmer_wisp',
  'level_1_moss_stalker',
];
const MOBS_ROOT = path.join(ROOT, 'plugins', 'MythicMobs', 'Packs');
const BLUEPRINTS_ROOT = path.join(ROOT, 'plugins', 'ModelEngine', 'blueprints');
const CACHE_PATH = path.join(ROOT, 'plugins', 'ModelEngine', '.data', 'cache.json');
const RESOURCE_ROOT = path.join(ROOT, 'plugins', 'ModelEngine', 'resource pack', 'assets', 'modelengine', 'models');
const QUALITY_ROOT = path.join(__dirname, 'model_quality');
const RUNTIME_EVIDENCE_ROOT = path.join(__dirname, 'modelengine_baseline_evidence');

function listFiles(root, suffix = null, acc = []) {
  if (!fs.existsSync(root)) return acc;
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) listFiles(full, suffix, acc);
    else if (entry.isFile() && (!suffix || full.endsWith(suffix))) acc.push(full);
  }
  return acc;
}

function relative(filePath) {
  return path.relative(ROOT, filePath).split(path.sep).join('/');
}

function sha256Buffer(buffer) {
  return crypto.createHash('sha256').update(buffer).digest('hex');
}

function fileEvidence(filePath) {
  if (!fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) return null;
  const stat = fs.statSync(filePath);
  return {
    path: relative(filePath),
    sha256: sha256Buffer(fs.readFileSync(filePath)),
    modifiedAt: stat.mtime.toISOString(),
    bytes: stat.size,
  };
}

function hashDirectory(root) {
  if (!fs.existsSync(root) || !fs.statSync(root).isDirectory()) return null;
  const files = listFiles(root).map((filePath) => path.relative(root, filePath).split(path.sep).join('/')).sort();
  const hash = crypto.createHash('sha256');
  for (const file of files) {
    hash.update(file);
    hash.update('\0');
    hash.update(fs.readFileSync(path.join(root, file)));
    hash.update('\0');
  }
  return { path: relative(root), sha256: hash.digest('hex'), files };
}

function aggregateVerdict(gates) {
  if (gates.some((gate) => gate.verdict === 'FAIL')) return 'FAIL';
  if (gates.some((gate) => gate.verdict === 'INSUFFICIENT_EVIDENCE')) return 'INSUFFICIENT_EVIDENCE';
  return 'PASS';
}

function runChecker(script, args = []) {
  const command = ['node', `_validation/${script}`, ...args];
  const result = spawnSync(process.execPath, [path.join(__dirname, script), ...args], {
    cwd: ROOT,
    encoding: 'utf8',
  });
  return {
    command: command.join(' '),
    exitCode: result.status ?? 1,
    stdout: (result.stdout || '').trim(),
    stderr: (result.stderr || '').trim(),
  };
}

function gate(verdict, reason, evidence = null) {
  return { verdict, reason, ...(evidence ? { evidence } : {}) };
}

function parseBindingVerdicts(command) {
  const results = new Map();
  for (const line of `${command.stdout}\n${command.stderr}`.split(/\r?\n/)) {
    const match = /^(PASS|FAIL) mob=([^ ]+) model=([^ ]+)$/.exec(line.trim());
    if (match) results.set(match[3], { verdict: match[1], mobId: match[2] });
  }
  return results;
}

function readCache() {
  const evidence = fileEvidence(CACHE_PATH);
  if (!evidence) return { evidence: null, entries: new Map() };
  try {
    const parsed = JSON.parse(fs.readFileSync(CACHE_PATH, 'utf8'));
    return {
      evidence,
      entries: new Map(Object.entries(parsed?.cache?.LEATHER_HORSE_ARMOR?.cachedId || {})),
    };
  } catch (error) {
    return { evidence: { ...evidence, parseError: error.message }, entries: new Map() };
  }
}

function cacheGate(modelId, cache) {
  if (!cache.evidence) return gate('FAIL', 'CACHE_MISSING');
  if (cache.evidence.parseError) return gate('FAIL', 'CACHE_INVALID_JSON', cache.evidence);
  const entries = [...cache.entries.entries()].filter(([id]) => id.startsWith(`${modelId}:`)).sort(([a], [b]) => a.localeCompare(b));
  const targetedSha256 = sha256Buffer(Buffer.from(JSON.stringify(entries)));
  const evidence = { cache: cache.evidence, targetedSha256, compiledEntries: entries.map(([id]) => id) };
  return entries.length
    ? gate('PASS', `${entries.length} compiled model_id entries`, evidence)
    : gate('FAIL', 'CACHE_ENTRIES_MISSING', evidence);
}

function checkerGate(command, modelId, kind) {
  const output = `${command.stdout}\n${command.stderr}`;
  const match = new RegExp(`^(PASS|FAIL) (?:mob=[^ ]+ )?model=${modelId}$`, 'm').exec(output);
  if (!match) return gate('FAIL', `${kind.toUpperCase()}_CHECKER_NO_MODEL_RESULT`, { command: command.command, exitCode: command.exitCode });
  const reasons = output.split(/\r?\n/)
    .filter((line) => /^  (?:quality|ecosystem)=/.test(line.trimEnd()))
    .flatMap((line) => line.trim().split('=').slice(1).join('=').split(','))
    .filter(Boolean);
  const uniqueReasons = [...new Set(reasons)];
  return gate(match[1], uniqueReasons.join(',') || `${kind} checker ${match[1].toLowerCase()}`, {
    command: command.command,
    exitCode: command.exitCode,
  });
}

function ecosystemGate(modelId, command, manifestEvidence) {
  if (!manifestEvidence) {
    return gate('INSUFFICIENT_EVIDENCE', 'ECOSYSTEM_MANIFEST_MISSING', { command: command.command, exitCode: command.exitCode });
  }
  let manifest;
  try {
    manifest = JSON.parse(fs.readFileSync(path.join(ROOT, manifestEvidence.path), 'utf8'));
  } catch (error) {
    return gate('FAIL', `ECOSYSTEM_MANIFEST_INVALID_JSON:${error.message}`, manifestEvidence);
  }
  if (manifest.schemaVersion < 2) return gate('INSUFFICIENT_EVIDENCE', 'ECOSYSTEM_MANIFEST_V2_NOT_ENABLED', manifestEvidence);
  const result = checkerGate(command, modelId, 'ecosystem');
  return { ...result, evidence: { ...result.evidence, manifest: manifestEvidence } };
}

function runtimeGates(modelId, blueprintEvidence) {
  const manifestPath = path.join(RUNTIME_EVIDENCE_ROOT, `${modelId}.runtime.json`);
  const manifestEvidence = fileEvidence(manifestPath);
  const names = ['liveSpawn', 'reload', 'restart'];
  if (!manifestEvidence) {
    return Object.fromEntries(names.map((name) => [name, gate('INSUFFICIENT_EVIDENCE', 'SHA_BOUND_RUNTIME_EVIDENCE_MISSING')]));
  }
  let manifest;
  try {
    manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  } catch (error) {
    return Object.fromEntries(names.map((name) => [name, gate('INSUFFICIENT_EVIDENCE', `RUNTIME_MANIFEST_INVALID_JSON:${error.message}`, manifestEvidence)]));
  }
  if (!blueprintEvidence || manifest.blueprintSha256 !== blueprintEvidence.sha256) {
    return Object.fromEntries(names.map((name) => [name, gate('INSUFFICIENT_EVIDENCE', 'RUNTIME_EVIDENCE_BLUEPRINT_SHA_MISMATCH', manifestEvidence)]));
  }
  return Object.fromEntries(names.map((name) => {
    const records = Array.isArray(manifest[name]) ? manifest[name] : [];
    const evidence = records.map((record) => {
      const resolved = path.resolve(path.dirname(manifestPath), record.path || '');
      return { declared: record, actual: fileEvidence(resolved) };
    });
    const complete = evidence.length > 0 && evidence.every(({ declared, actual }) =>
      actual && (!declared.sha256 || declared.sha256 === actual.sha256));
    return [name, complete
      ? gate('PASS', `${evidence.length} SHA-verified evidence file(s)`, { manifest: manifestEvidence, files: evidence })
      : gate('INSUFFICIENT_EVIDENCE', 'RUNTIME_EVIDENCE_FILES_MISSING_OR_STALE', { manifest: manifestEvidence, files: evidence })];
  }));
}

function buildReport(evidenceDate) {
  const refs = listFiles(MOBS_ROOT, '.mob.yml').flatMap((filePath) => parseMobModels(filePath));
  const strictReferences = refs.filter((ref) => ref.template === 'level_1_base_boss').map((ref) => ref.modelId);
  const modelIds = [...new Set([...PRIMARY_MODELS, ...strictReferences])].sort();
  const bindingCommand = runChecker('check_modelengine_binding.js');
  const bindingVerdicts = parseBindingVerdicts(bindingCommand);
  const cache = readCache();
  const commands = [bindingCommand];
  const models = [];

  for (const modelId of modelIds) {
    const matchingRefs = refs.filter((ref) => ref.modelId === modelId);
    const yamlFiles = [...new Set(matchingRefs.map((ref) => ref.filePath))].map(fileEvidence);
    const blueprintPath = path.join(BLUEPRINTS_ROOT, modelId, `${modelId}.bbmodel`);
    const blueprintEvidence = fileEvidence(blueprintPath);
    const resourceEvidence = hashDirectory(path.join(RESOURCE_ROOT, modelId));
    const qualityManifestEvidence = fileEvidence(path.join(QUALITY_ROOT, `${modelId}.quality.json`));
    const qualityCommand = runChecker('check_modelengine_quality.js', ['--model', modelId]);
    const ecosystemCommand = runChecker('check_modelengine_ecosystem.js', ['--model', modelId]);
    commands.push(qualityCommand, ecosystemCommand);
    const runtime = runtimeGates(modelId, blueprintEvidence);
    const gates = {
      yaml: yamlFiles.length === 1
        ? gate('PASS', 'one active MythicMobs model declaration', yamlFiles[0])
        : gate('FAIL', yamlFiles.length ? 'MULTIPLE_ACTIVE_MODEL_DECLARATIONS' : 'ACTIVE_MODEL_YAML_MISSING', yamlFiles),
      blueprint: blueprintEvidence ? gate('PASS', 'blueprint present', blueprintEvidence) : gate('FAIL', 'BLUEPRINT_MISSING'),
      resourceOutput: resourceEvidence && resourceEvidence.files.some((file) => file.endsWith('.json'))
        ? gate('PASS', `${resourceEvidence.files.length} generated file(s)`, resourceEvidence)
        : gate('FAIL', 'RESOURCE_PACK_ASSETS_MISSING', resourceEvidence),
      cache: cacheGate(modelId, cache),
      bindingChecker: bindingVerdicts.has(modelId)
        ? gate(bindingVerdicts.get(modelId).verdict, 'check_modelengine_binding.js result', { command: bindingCommand.command, exitCode: bindingCommand.exitCode })
        : gate('FAIL', 'BINDING_CHECKER_NO_MODEL_RESULT'),
      quality: checkerGate(qualityCommand, modelId, 'quality'),
      ecosystem: ecosystemGate(modelId, ecosystemCommand, qualityManifestEvidence),
      ...runtime,
    };
    models.push({
      modelId,
      role: PRIMARY_MODELS.includes(modelId) ? 'ticket_target' : 'strict_quality_reference',
      verdict: aggregateVerdict(Object.values(gates)),
      gates,
    });
  }

  return {
    schemaVersion: 1,
    evidenceDate,
    generatedBy: relative(__filename),
    scope: { primaryModels: PRIMARY_MODELS, strictQualityReferences: modelIds.filter((id) => !PRIMARY_MODELS.includes(id)) },
    verdict: aggregateVerdict(models),
    models,
    commands,
  };
}

function main() {
  const outputIndex = process.argv.indexOf('--output');
  const dateIndex = process.argv.indexOf('--date');
  const outputPath = outputIndex >= 0 ? path.resolve(process.argv[outputIndex + 1]) : null;
  const evidenceDate = dateIndex >= 0 ? process.argv[dateIndex + 1] : new Date().toISOString().slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(evidenceDate)) throw new Error('--date must use YYYY-MM-DD');
  const report = buildReport(evidenceDate);
  const serialized = `${JSON.stringify(report, null, 2)}\n`;
  if (outputPath) {
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    fs.writeFileSync(outputPath, serialized);
    console.log(`MODELENGINE_BASELINE_MATRIX_WRITTEN path=${outputPath}`);
  } else {
    process.stdout.write(serialized);
  }
  console.log(`MODELENGINE_BASELINE_MATRIX_${report.verdict}`);
  if (report.verdict !== 'PASS') process.exitCode = 1;
}

if (require.main === module) main();

module.exports = { aggregateVerdict, buildReport, hashDirectory };
