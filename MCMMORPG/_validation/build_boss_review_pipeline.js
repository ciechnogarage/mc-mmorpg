const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { auditReviewDatabase, sha1 } = require('./manual_review_contract');

const root = path.join(__dirname, 'reference_corpus');
const projectRoot = path.resolve(__dirname, '..');
const cardsPath = path.join(root, 'modelengine-model-cards.json');
const corpusPath = path.join(root, 'modelengine-reference-corpus.json');
const reviewsPath = path.join(root, 'manual_visual_reviews.json');
const overridesPath = path.join(root, 'boss_review_overrides.json');
const outputPath = path.join(root, 'boss_review_pipeline.json');
const dashboardPath = path.join(root, 'boss_review_dashboard.md');
const qualityRoot = path.join(__dirname, 'model_quality');
const reviewRoot = path.join(__dirname, 'model_reviews');
const packsRoot = path.join(projectRoot, 'plugins', 'MythicMobs', 'Packs');
const blueprintsRoot = path.join(projectRoot, 'plugins', 'ModelEngine', 'blueprints');
const completedRuntimeReviewsPath = path.join(
  __dirname,
  'active_runtime_reviews',
  'completed_runtime_boss_reviews.json',
);

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function stableHash(value) {
  return crypto.createHash('sha1').update(value).digest('hex');
}

function rel(from, target) {
  return path.relative(from, target);
}

function modelKey(card) {
  return `${card.id}::${card.file}`;
}

function atlasDirectoryName(card) {
  return card.file
    .replace(/^ModelEngine\/blueprints\//, '')
    .replace(/\.bbmodel$/i, '')
    .replace(/[^A-Za-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

function yamlSignals(document) {
  const mechanics = new Set((document.refs?.mechanics || []).map((value) => value.toLowerCase()));
  const explicitBossbar = ['barcreate', 'barset', 'barmodify', 'barremove', 'bossbar']
    .filter((mechanic) => mechanics.has(mechanic));
  const namedBossbar = (document.refs?.skills || [])
    .filter((skill) => /bossbar/i.test(skill));
  const bossPath = /(?:^|\/)(?:boss|bosses)(?:\/|$)/i.test(document.file);
  return {
    explicitBossbar,
    namedBossbar,
    bossPath,
    confirmed: explicitBossbar.length > 0,
    candidate: explicitBossbar.length > 0 || namedBossbar.length > 0 || bossPath,
  };
}

function buildYamlGraph(startFiles, yamlByFile, definitionToFiles) {
  const queue = [...new Set(startFiles)];
  const visited = new Set();
  const nodes = [];
  const edges = [];

  while (queue.length) {
    const file = queue.shift();
    if (visited.has(file)) continue;
    visited.add(file);
    const document = yamlByFile.get(file);
    if (!document) {
      nodes.push({ file, missing: true });
      continue;
    }
    const refs = document.refs || {};
    nodes.push({
      file,
      definitions: document.definitions || [],
      models: refs.models || [],
      states: refs.states || [],
      modelParts: refs.modelParts || [],
      skills: refs.skills || [],
      sounds: refs.sounds || [],
      mechanics: refs.mechanics || [],
      triggers: refs.triggers || [],
    });
    for (const skill of refs.skills || []) {
      const targets = definitionToFiles.get(skill.toLowerCase()) || [];
      for (const target of targets) {
        edges.push({ from: file, to: target, type: 'skill', id: skill });
        if (!visited.has(target)) queue.push(target);
      }
    }
  }
  return { nodes, edges };
}

function listFilesRecursive(dir, matcher) {
  if (!fs.existsSync(dir)) return [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) files.push(...listFilesRecursive(full, matcher));
    else if (!matcher || matcher(full)) files.push(full);
  }
  return files;
}

function resolveProjectFile(relativePath, baseDir) {
  if (!relativePath || typeof relativePath !== 'string') return null;
  if (path.isAbsolute(relativePath)) return relativePath;
  return path.resolve(baseDir, relativePath);
}

function readText(file) {
  return fs.readFileSync(file, 'utf8');
}

function parseMobModelId(file) {
  const text = readText(file);
  const lines = text.split(/\r?\n/);
  let inModelBlock = false;
  let modelIndent = 0;
  for (const line of lines) {
    const indent = line.match(/^\s*/)[0].length;
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    if (/^Model:\s*$/.test(trimmed)) {
      inModelBlock = true;
      modelIndent = indent;
      continue;
    }
    if (inModelBlock && indent <= modelIndent && !/^- /.test(trimmed)) {
      inModelBlock = false;
    }
    if (inModelBlock) {
      const match = trimmed.match(/^Id:\s*([^\s#]+)/);
      if (match) return match[1];
    }
  }
  return null;
}

function parseActivePackDefinitions(files) {
  const definitionToFiles = new Map();
  for (const file of files) {
    const text = readText(file);
    for (const match of text.matchAll(/^([A-Za-z0-9_.-]+):\s*$/gm)) {
      const key = match[1].toLowerCase();
      if (!definitionToFiles.has(key)) definitionToFiles.set(key, []);
      definitionToFiles.get(key).push(file);
    }
  }
  return definitionToFiles;
}

function parseActiveYamlDocument(file) {
  const text = readText(file);
  const mechanics = [];
  const refs = {
    skills: [],
    states: [],
    modelParts: [],
    sounds: [],
    summons: [],
    mechanics,
  };
  for (const match of text.matchAll(/\bskill\{s=([^};\s]+)/g)) refs.skills.push(match[1]);
  for (const match of text.matchAll(/\bstate\{mid=([^;}\s]+);s=([^;}\s]+)/g)) refs.states.push(`${match[1]}:${match[2]}`);
  for (const match of text.matchAll(/@modelpart\{pid=([^};\s]+)/g)) refs.modelParts.push(match[1]);
  for (const match of text.matchAll(/\bsound\{s=([^};\s]+)/g)) refs.sounds.push(match[1]);
  for (const match of text.matchAll(/\bsummon\{mob=([^};\s]+)/g)) refs.summons.push(match[1]);
  for (const match of text.matchAll(/\b([a-z_]+)\{/g)) mechanics.push(match[1]);
  const unique = {};
  for (const [key, values] of Object.entries(refs)) unique[key] = [...new Set(values)];
  return {
    file,
    definitions: [...text.matchAll(/^([A-Za-z0-9_.-]+):\s*$/gm)].map((match) => match[1]),
    refs: unique,
  };
}

function buildActiveRuntimeGraph(entry, definitionToFiles) {
  const queue = [...new Set(entry.activeYamlFiles)];
  const visited = new Set();
  const nodes = [];
  const edges = [];
  while (queue.length) {
    const file = queue.shift();
    if (visited.has(file)) continue;
    visited.add(file);
    if (!fs.existsSync(file)) {
      nodes.push({ file: rel(projectRoot, file), missing: true });
      continue;
    }
    const document = parseActiveYamlDocument(file);
    nodes.push({
      file: rel(projectRoot, file),
      definitions: document.definitions,
      refs: document.refs,
    });
    for (const skill of document.refs.skills) {
      for (const target of definitionToFiles.get(skill.toLowerCase()) || []) {
        edges.push({ from: rel(projectRoot, file), to: rel(projectRoot, target), type: 'skill', id: skill });
        if (!visited.has(target)) queue.push(target);
      }
    }
  }
  edges.push({ from: rel(projectRoot, entry.qualityManifest), to: rel(projectRoot, entry.blueprint), type: 'blueprint', id: entry.modelId });
  if (entry.runtimeProbe) {
    edges.push({ from: rel(projectRoot, entry.qualityManifest), to: rel(projectRoot, entry.runtimeProbe), type: 'runtime_probe', id: entry.mobId });
  }
  for (const render of entry.renderEvidence) {
    edges.push({ from: rel(projectRoot, entry.qualityManifest), to: rel(projectRoot, render), type: 'render', id: path.basename(render) });
  }
  return { nodes, edges };
}

function buildActiveRuntimeBosses() {
  const qualityFiles = listFilesRecursive(qualityRoot, (file) => file.endsWith('.quality.json'));
  const packYamlFiles = listFilesRecursive(packsRoot, (file) => file.endsWith('.yml'));
  const definitionToFiles = parseActivePackDefinitions(packYamlFiles);
  const runtimeBosses = [];
  const completedReviewIds = fs.existsSync(completedRuntimeReviewsPath)
    ? new Set((readJson(completedRuntimeReviewsPath).models || []).filter((review) => !review.stale).map((review) => review.id))
    : new Set();

  for (const qualityFile of qualityFiles) {
    const manifest = readJson(qualityFile);
    if (manifest.profile !== 'boss') continue;
    const modelId = path.basename(qualityFile, '.quality.json');
    const mobFile = (manifest.integrationFiles || [])
      .map((item) => resolveProjectFile(item, path.dirname(qualityFile)))
      .find((item) => item && /\/mobs\/.*\.yml$/i.test(item));
    const blueprint = path.join(blueprintsRoot, modelId, `${modelId}.bbmodel`);
    const runtimeProbe = (manifest.runtimeEvidence || [])
      .map((item) => resolveProjectFile(item, path.dirname(qualityFile)))
      .find(Boolean);
    const renderEvidence = (manifest.renderEvidence || [])
      .map((item) => resolveProjectFile(item, path.dirname(qualityFile)))
      .filter(Boolean);
    const activeFiles = [
      qualityFile,
      ...(manifest.integrationFiles || []).map((item) => resolveProjectFile(item, path.dirname(qualityFile))),
      ...(manifest.skillBindings || []).map((binding) => resolveProjectFile(binding.sourceFile, path.dirname(qualityFile))),
    ].filter(Boolean);
    const blockers = [];
    if (!mobFile || !fs.existsSync(mobFile)) blockers.push('missing_active_mob_file');
    if (!fs.existsSync(blueprint)) blockers.push('missing_blueprint');
    if (!runtimeProbe || !fs.existsSync(runtimeProbe)) blockers.push('missing_runtime_probe');
    if (!renderEvidence.length || renderEvidence.some((item) => !fs.existsSync(item))) blockers.push('missing_render_evidence');
    if (Array.isArray(manifest.knownIssues) && manifest.knownIssues.length > 0) blockers.push('known_issues_present');
    const mobId = mobFile ? (readText(mobFile).match(/^([A-Za-z0-9_.-]+):\s*$/m)?.[1] || modelId) : modelId;
    const modelIdFromMob = mobFile ? parseMobModelId(mobFile) : null;
    if (modelIdFromMob && modelIdFromMob !== modelId) blockers.push('mob_model_mismatch');
    const boss = {
      mobId,
      modelId,
      profile: manifest.profile,
      queueStatus: blockers.length > 0
        ? 'blocked'
        : completedReviewIds.has(modelId)
          ? 'strict_complete'
          : 'strict_ready',
      blockers,
      qualityManifest: qualityFile,
      blueprint,
      runtimeProbe,
      activeFiles: [...new Set(activeFiles)].filter((item) => fs.existsSync(item)),
      activeYamlFiles: [...new Set(activeFiles)].filter((item) => fs.existsSync(item) && item.endsWith('.yml')),
      nonRuntimeReferences: manifest.references || [],
      renderEvidence,
      runtimeEvidence: runtimeProbe && fs.existsSync(runtimeProbe) ? [runtimeProbe] : [],
      sourceHashes: {
        qualityManifest: sha1(qualityFile),
        blueprint: fs.existsSync(blueprint) ? sha1(blueprint) : null,
        mobFile: mobFile && fs.existsSync(mobFile) ? sha1(mobFile) : null,
        runtimeProbe: runtimeProbe && fs.existsSync(runtimeProbe) ? sha1(runtimeProbe) : null,
      },
      interactionBones: manifest.interactionBones || [],
      animationContract: manifest.animationContract || [],
      skillBindings: manifest.skillBindings || [],
      graph: { nodes: [], edges: [] },
    };
    boss.graph = buildActiveRuntimeGraph(boss, definitionToFiles);
    runtimeBosses.push(boss);
  }

  runtimeBosses.sort((a, b) => a.mobId.localeCompare(b.mobId));
  return runtimeBosses;
}

function reviewStatusByKey(index, batches, cards) {
  const statuses = new Map(cards.map((card) => [modelKey(card), 'missing']));
  const cardsById = new Map();
  for (const card of cards) {
    if (!cardsById.has(card.id)) cardsById.set(card.id, []);
    cardsById.get(card.id).push(card);
  }
  for (const batch of batches) {
    const status = batch.reviewStatus?.startsWith('legacy_') ? 'legacy' : 'strict';
    for (const review of batch.models || []) {
      const candidates = cardsById.get(review.id) || [];
      const key = review.modelKey || (candidates.length === 1 ? modelKey(candidates[0]) : null);
      if (key) statuses.set(key, review.stale ? 'stale' : status);
    }
  }
  return statuses;
}

function renderEvidence(card) {
  const atlasDir = path.join(root, 'visual_atlas_v2', atlasDirectoryName(card));
  const staticViews = [
    'front.png',
    'side.png',
    'back.png',
    'three_quarter.png',
    'silhouette.png',
    'player_scale.png',
    'hitbox.png',
    'helpers.png',
  ];
  return {
    staticViews: staticViews.map((file) => path.relative(root, path.join(atlasDir, file))),
    animations: (card.animationsDetail || []).map((animation) => ({
      name: animation.name,
      phaseTimes: animation.phaseTimes,
      frames: ['start', 'anticipation', 'impact', 'recovery', 'end'].map((phase) =>
        path.relative(root, path.join(atlasDir, `animation_${animation.name}_${phase}.png`))),
    })),
  };
}

function sourceEvidence(card, corpusRoot) {
  const blueprint = path.resolve(corpusRoot, card.file);
  return {
    blueprint,
    sourceHash: fs.existsSync(blueprint) ? sha1(blueprint) : null,
    textures: (card.textureDetails || []).map((texture) => ({
      name: texture.name,
      renderMode: texture.renderMode,
      metrics: texture.metrics || null,
    })),
  };
}

function confirmedFamilyQueueEntry(family) {
  const statuses = family.models.map((model) => model.reviewStatus);
  return {
    familyId: family.familyId,
    models: family.models.length,
    animations: family.models.reduce((sum, model) => sum + model.animations, 0),
    strict: statuses.filter((status) => status === 'strict').length,
    legacy: statuses.filter((status) => status === 'legacy').length,
    missing: statuses.filter((status) => status === 'missing' || status === 'stale').length,
  };
}

function buildPipeline() {
  const cardsDocument = readJson(cardsPath);
  const corpus = readJson(corpusPath);
  const index = readJson(reviewsPath);
  const overrides = readJson(overridesPath);
  const batches = (index.batchFiles || []).map((file) => readJson(path.join(root, file)));
  const cards = cardsDocument.cards.map((card) => ({
    ...card,
    absoluteFile: path.resolve(cardsDocument.corpus, card.file),
  }));
  const yamlByFile = new Map(corpus.yamls.map((document) => [document.file, document]));
  const definitionToFiles = new Map();
  for (const document of corpus.yamls) {
    for (const definition of document.definitions || []) {
      const key = definition.toLowerCase();
      if (!definitionToFiles.has(key)) definitionToFiles.set(key, []);
      definitionToFiles.get(key).push(document.file);
    }
  }

  const familyCards = new Map();
  for (const card of cards) {
    if (!familyCards.has(card.familyId)) familyCards.set(card.familyId, []);
    familyCards.get(card.familyId).push(card);
  }
  const familySignals = new Map();
  for (const [familyId, members] of familyCards) {
    const documents = [...new Set(members.flatMap((card) => card.linkedYamlFiles || []))]
      .map((file) => yamlByFile.get(file))
      .filter(Boolean);
    const signals = documents.map((document) => ({ file: document.file, ...yamlSignals(document) }));
    const forcedInclude = overrides.confirmedFamilyIds.includes(familyId);
    const forcedExclude = overrides.excludedFamilyIds.includes(familyId);
    familySignals.set(familyId, {
      signals,
      forcedInclude,
      forcedExclude,
      confirmed: !forcedExclude && (forcedInclude || signals.some((signal) => signal.confirmed)),
      candidate: !forcedExclude && (forcedInclude || signals.some((signal) => signal.candidate)),
    });
  }

  const statuses = reviewStatusByKey(index, batches, cards);
  const audit = auditReviewDatabase(index, batches, cards, root);
  const bossFamilies = [];
  const candidateFamilies = [];
  for (const [familyId, members] of familyCards) {
    const classification = familySignals.get(familyId);
    if (!classification.candidate) continue;
    const linkedYamlFiles = [...new Set(members.flatMap((card) => card.linkedYamlFiles || []))];
    const graph = buildYamlGraph(linkedYamlFiles, yamlByFile, definitionToFiles);
    const family = {
      familyId,
      classification: classification.confirmed ? 'confirmed_boss' : 'review_required',
      classificationEvidence: classification,
      graph,
      models: members.map((card) => ({
        modelKey: modelKey(card),
        id: card.id,
        role: card.familyRole,
        reviewStatus: statuses.get(modelKey(card)),
        animations: card.animationsDetail?.length || 0,
        source: sourceEvidence(card, cardsDocument.corpus),
        renders: renderEvidence(card),
        bindingEvidence: card.bindingEvidence || [],
        reviewChecklist: [
          'identity_and_distant_read',
          'silhouette_proportions_composition',
          'diffuse_emissive_uv_value_palette',
          'rig_helpers_hitboxes',
          'individual_animation_findings',
          'damage_hitbox_sound_vfx_sync',
          'runtime_binding_and_live_spawn',
          'reference_comparison_and_remaining_weaknesses',
        ],
      })),
    };
    if (classification.confirmed) bossFamilies.push(family);
    else candidateFamilies.push(family);
  }

  bossFamilies.sort((a, b) => a.familyId.localeCompare(b.familyId));
  candidateFamilies.sort((a, b) => a.familyId.localeCompare(b.familyId));
  const bossKeys = new Set(bossFamilies.flatMap((family) => family.models.map((model) => model.modelKey)));
  const nonBossModels = cards
    .filter((card) => !bossKeys.has(modelKey(card)))
    .map((card) => ({ modelKey: modelKey(card), id: card.id, familyId: card.familyId }))
    .sort((a, b) => stableHash(a.modelKey).localeCompare(stableHash(b.modelKey)));
  const qaSampleSize = Math.ceil(nonBossModels.length * overrides.qaSampleRate);
  const qaSample = nonBossModels.slice(0, qaSampleSize);
  const activeRuntimeBosses = buildActiveRuntimeBosses();
  const confirmedFamilyStrictQueue = bossFamilies
    .map((family) => confirmedFamilyQueueEntry(family))
    .filter((family) => family.animations > 0 && (family.missing > 0 || family.legacy > 0))
    .sort((a, b) =>
      a.models - b.models ||
      a.animations - b.animations ||
      a.legacy - b.legacy ||
      a.familyId.localeCompare(b.familyId));
  const candidateFamilyConfirmationQueue = candidateFamilies
    .map((family) => ({
      familyId: family.familyId,
      evidenceFiles: family.classificationEvidence.signals.map((signal) => signal.file),
      models: family.models.length,
      animations: family.models.reduce((sum, model) => sum + model.animations, 0),
    }))
    .sort((a, b) =>
      a.models - b.models ||
      a.animations - b.animations ||
      a.familyId.localeCompare(b.familyId));
  const activeRuntimeStrictQueue = activeRuntimeBosses
    .filter((boss) => boss.queueStatus === 'strict_ready')
    .map((boss) => ({
      mobId: boss.mobId,
      modelId: boss.modelId,
      skillBindings: boss.skillBindings.length,
      runtimeProbe: boss.runtimeProbe,
      qualityManifest: boss.qualityManifest,
    }));

  return {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    inputs: {
      cards: path.relative(__dirname, cardsPath),
      corpus: path.relative(__dirname, corpusPath),
      manualReviews: path.relative(__dirname, reviewsPath),
      overrides: path.relative(__dirname, overridesPath),
      cardsHash: sha1(cardsPath),
      corpusHash: sha1(corpusPath),
      manualReviewsHash: sha1(reviewsPath),
      overridesHash: sha1(overridesPath),
    },
    policy: {
      autoConfirmation: 'linked YAML contains an explicit bossbar mechanic',
      reviewRequired: 'boss path or bossbar-named skill without an explicit bossbar mechanic',
      familyCompletion: 'every model and source animation in the family has a valid strict v3 review',
      qa: 'all confirmed boss models plus deterministic SHA-1 sample of non-boss models',
      qaSampleRate: overrides.qaSampleRate,
    },
    summary: {
      confirmedBossFamilies: bossFamilies.length,
      confirmedBossModels: bossKeys.size,
      candidateFamilies: candidateFamilies.length,
      activeRuntimeBosses: activeRuntimeBosses.length,
      activeRuntimeBossesReady: activeRuntimeBosses.filter((boss) => boss.queueStatus === 'strict_ready').length,
      activeRuntimeBossesComplete: activeRuntimeBosses.filter((boss) => boss.queueStatus === 'strict_complete').length,
      qaNonBossModels: qaSample.length,
      strictModels: audit.counts.strictModels,
      legacyModels: audit.counts.legacyModels,
      missingModels: audit.counts.missingModels,
      confirmedFamilyStrictQueue: confirmedFamilyStrictQueue.length,
      candidateFamilyConfirmationQueue: candidateFamilyConfirmationQueue.length,
    },
    bossFamilies,
    candidateFamilies,
    activeRuntimeBosses: activeRuntimeBosses.map((boss) => ({
      ...boss,
      qualityManifest: rel(projectRoot, boss.qualityManifest),
      blueprint: rel(projectRoot, boss.blueprint),
      runtimeProbe: boss.runtimeProbe ? rel(projectRoot, boss.runtimeProbe) : null,
      activeFiles: boss.activeFiles.map((file) => rel(projectRoot, file)),
      activeYamlFiles: boss.activeYamlFiles.map((file) => rel(projectRoot, file)),
      renderEvidence: boss.renderEvidence.map((file) => rel(projectRoot, file)),
      runtimeEvidence: boss.runtimeEvidence.map((file) => rel(projectRoot, file)),
    })),
    queues: {
      activeRuntimeStrictQueue: activeRuntimeStrictQueue.map((boss) => ({
        ...boss,
        runtimeProbe: boss.runtimeProbe ? rel(projectRoot, boss.runtimeProbe) : null,
        qualityManifest: rel(projectRoot, boss.qualityManifest),
      })),
      confirmedFamilyStrictQueue,
      recommendedConfirmedFamilyBatch: confirmedFamilyStrictQueue.slice(0, 10),
      candidateFamilyConfirmationQueue,
    },
    qa: {
      requiredBossModelKeys: [...bossKeys].sort(),
      deterministicNonBossSample: qaSample,
    },
  };
}

function dashboard(pipeline) {
  const lines = [
    '# ModelEngine Boss Review Dashboard',
    '',
    `Generated: ${pipeline.generatedAt}`,
    '',
    `- Confirmed boss families: ${pipeline.summary.confirmedBossFamilies}`,
    `- Confirmed boss models: ${pipeline.summary.confirmedBossModels}`,
    `- Families requiring confirmation: ${pipeline.summary.candidateFamilies}`,
    `- Active runtime bosses: ${pipeline.summary.activeRuntimeBosses}`,
    `- Active runtime bosses ready: ${pipeline.summary.activeRuntimeBossesReady}`,
    `- Active runtime bosses complete: ${pipeline.summary.activeRuntimeBossesComplete}`,
    `- Strict reviews: ${pipeline.summary.strictModels}`,
    `- Legacy reviews: ${pipeline.summary.legacyModels}`,
    `- Missing reviews: ${pipeline.summary.missingModels}`,
    `- Confirmed family strict queue: ${pipeline.summary.confirmedFamilyStrictQueue}`,
    `- Candidate confirmation queue: ${pipeline.summary.candidateFamilyConfirmationQueue}`,
    `- Deterministic non-boss QA sample: ${pipeline.summary.qaNonBossModels}`,
    '',
    '## Confirmed Boss Queue',
    '',
    '| Family | Models | Animations | Strict | Legacy | Missing/Stale |',
    '| --- | ---: | ---: | ---: | ---: | ---: |',
  ];
  for (const family of pipeline.bossFamilies) {
    const statuses = family.models.map((model) => model.reviewStatus);
    lines.push(
      `| ${family.familyId} | ${family.models.length} | ` +
      `${family.models.reduce((sum, model) => sum + model.animations, 0)} | ` +
      `${statuses.filter((status) => status === 'strict').length} | ` +
      `${statuses.filter((status) => status === 'legacy').length} | ` +
      `${statuses.filter((status) => status === 'missing' || status === 'stale').length} |`,
    );
  }
  lines.push('', '## Active Runtime Bosses', '');
  if (!pipeline.activeRuntimeBosses.length) lines.push('None.');
  else {
    lines.push('| Mob | Model | Status | Blockers | Skill Bindings | Runtime Probe |');
    lines.push('| --- | --- | --- | --- | ---: | --- |');
    for (const boss of pipeline.activeRuntimeBosses) {
      lines.push(
        `| ${boss.mobId} | ${boss.modelId} | ${boss.queueStatus} | ` +
        `${boss.blockers.join(', ') || 'none'} | ${boss.skillBindings.length} | ${boss.runtimeProbe} |`,
      );
    }
  }
  lines.push('', '## Runtime Strict Queue', '');
  if (!pipeline.queues.activeRuntimeStrictQueue.length) lines.push('None.');
  else {
    lines.push('| Mob | Model | Skill Bindings | Runtime Probe | Quality Manifest |');
    lines.push('| --- | --- | ---: | --- | --- |');
    for (const boss of pipeline.queues.activeRuntimeStrictQueue) {
      lines.push(
        `| ${boss.mobId} | ${boss.modelId} | ${boss.skillBindings} | ${boss.runtimeProbe} | ${boss.qualityManifest} |`,
      );
    }
  }
  lines.push('', '## Recommended Confirmed Family Batch', '');
  if (!pipeline.queues.recommendedConfirmedFamilyBatch.length) lines.push('None.');
  else {
    lines.push('| Family | Models | Animations | Strict | Legacy | Missing |');
    lines.push('| --- | ---: | ---: | ---: | ---: | ---: |');
    for (const family of pipeline.queues.recommendedConfirmedFamilyBatch) {
      lines.push(
        `| ${family.familyId} | ${family.models} | ${family.animations} | ` +
        `${family.strict} | ${family.legacy} | ${family.missing} |`,
      );
    }
  }
  lines.push('', '## Families Requiring Confirmation', '');
  if (!pipeline.candidateFamilies.length) lines.push('None.');
  for (const family of pipeline.candidateFamilies) {
    const evidence = family.classificationEvidence.signals
      .map((signal) => signal.file)
      .join(', ');
    lines.push(`- \`${family.familyId}\`: ${evidence}`);
  }
  lines.push(
    '',
    '## Completion Rule',
    '',
    'A family is complete only when every member and every source animation has a valid strict v3 review, all source hashes are current, and unresolved runtime bindings remain visible as blockers.',
    '',
  );
  return `${lines.join('\n')}\n`;
}

const pipeline = buildPipeline();
fs.writeFileSync(outputPath, `${JSON.stringify(pipeline, null, 2)}\n`);
fs.writeFileSync(dashboardPath, dashboard(pipeline));
console.log(
  `BOSS_REVIEW_PIPELINE_PASS: ${pipeline.summary.confirmedBossFamilies} confirmed families, ` +
  `${pipeline.summary.confirmedBossModels} boss models, ` +
  `${pipeline.summary.candidateFamilies} candidates, ${pipeline.summary.qaNonBossModels} QA models`,
);
