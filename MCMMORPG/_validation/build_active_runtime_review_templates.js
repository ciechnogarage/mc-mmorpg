const fs = require('fs');
const path = require('path');
const { sha1 } = require('./manual_review_contract');

const validationRoot = __dirname;
const projectRoot = path.resolve(__dirname, '..');
const pipelinePath = path.join(validationRoot, 'reference_corpus', 'boss_review_pipeline.json');
const outputDir = path.join(validationRoot, 'active_runtime_reviews');
const outputPath = path.join(outputDir, 'runtime_boss_review_templates.json');

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function rel(target) {
  return path.relative(validationRoot, target);
}

function relFromProject(target) {
  return path.relative(projectRoot, target);
}

function buildEvidenceAnchor(boss, source, sourceClass, locator, detail, artifact = 'yaml') {
  return {
    artifact,
    sourceClass,
    source: rel(source),
    locator,
    detail,
    sourceHash: sha1(source),
  };
}

function loadQualityManifest(boss) {
  return readJson(path.join(projectRoot, boss.qualityManifest));
}

function buildAnimationTemplate(boss, manifest, animationName) {
  const contract = (manifest.animationContract || []).find((item) => item.animation === animationName) || null;
  const skillBindings = (manifest.skillBindings || []).filter((item) => item.animation === animationName);
  const evidence = [];
  if (skillBindings.length > 0) {
    for (const binding of skillBindings) {
      const bindingFile = path.resolve(path.dirname(path.join(projectRoot, boss.qualityManifest)), binding.sourceFile);
      evidence.push(
        buildEvidenceAnchor(
          boss,
          bindingFile,
          'active_runtime_repo',
          `skill ${binding.skill}`,
          `Declared skill binding for animation ${animationName}`,
        ),
      );
    }
  }
  return {
    name: animationName,
    status: boss.queueStatus === 'strict_complete' ? 'completed_manual_review' : 'pending_manual_review',
    phaseContract: contract ? {
      purpose: contract.purpose,
      impactTicks: contract.impactTicks || [],
      bones: contract.bones || [],
    } : null,
    skillBindings: skillBindings.map((binding) => ({
      skill: binding.skill,
      sourceFile: rel(path.resolve(path.dirname(path.join(projectRoot, boss.qualityManifest)), binding.sourceFile)),
      modelParts: binding.modelParts || [],
      impactTicks: binding.impactTicks || [],
      timingToleranceTicks: binding.timingToleranceTicks,
      requiresGcd: !!binding.requiresGcd,
      requiresModelLock: !!binding.requiresModelLock,
    })),
    manualChecklist: [
      'verify anticipation, impact, recovery and settle pose from current render',
      'verify kinetic chain against intended damaging limb or cast origin',
      'verify runtime damage / hitbox / sound / vfx synchronization',
      'verify whether lockmodel / gcd / ai policy preserves the authored motion',
    ],
    evidenceAnchors: evidence,
  };
}

function buildTemplate(boss) {
  const manifest = loadQualityManifest(boss);
  const qualityManifestPath = path.join(projectRoot, boss.qualityManifest);
  const runtimeProbePath = boss.runtimeProbe ? path.join(projectRoot, boss.runtimeProbe) : null;
  const reviewedSources = [
    qualityManifestPath,
    path.join(projectRoot, boss.blueprint),
    ...boss.activeFiles.map((file) => path.join(projectRoot, file)),
    ...boss.renderEvidence.map((file) => path.join(projectRoot, file)),
    ...(runtimeProbePath ? [runtimeProbePath] : []),
  ];
  const uniqueReviewedSources = [...new Set(reviewedSources)];

  return {
    schemaVersion: 1,
    status: boss.queueStatus === 'strict_complete' ? 'completed_manual_review' : 'pending_manual_review',
    sourceClass: 'active_runtime_repo',
    mobId: boss.mobId,
    modelId: boss.modelId,
    queueStatus: boss.queueStatus,
    blockers: boss.blockers,
    reviewedSources: uniqueReviewedSources.map(rel),
    sourceHashes: {
      qualityManifest: sha1(qualityManifestPath),
      blueprint: sha1(path.join(projectRoot, boss.blueprint)),
      runtimeProbe: runtimeProbePath ? sha1(runtimeProbePath) : null,
    },
    provenance: {
      qualityManifest: boss.qualityManifest,
      blueprint: boss.blueprint,
      runtimeProbe: boss.runtimeProbe,
      activeFiles: boss.activeFiles,
      renderEvidence: boss.renderEvidence,
      nonRuntimeReferences: boss.nonRuntimeReferences,
    },
    requiredClaims: [
      'identity_and_distant_read',
      'silhouette_proportions_composition',
      'materials_palette_emissive_uv',
      'rig_helpers_hitbox_eye_height',
      'encounter_graph_truth',
      'runtime_binding_and_spawn_proof',
      'remaining_weaknesses_against_references',
    ],
    evidenceAnchors: {
      qualityManifest: buildEvidenceAnchor(
        boss,
        qualityManifestPath,
        'generated_review',
        'field profile',
        'Boss quality manifest anchors the intended design and integration contract',
        'quality_manifest',
      ),
      runtimeProbe: runtimeProbePath ? buildEvidenceAnchor(
        boss,
        runtimeProbePath,
        'runtime_probe',
        'section Live Spawn',
        'Runtime probe proves reload, spawn, base entity invisibility and model-owner delta',
        'runtime',
      ) : null,
    },
    animationTemplates: (manifest.requiredAnimations || []).map((animationName) =>
      buildAnimationTemplate(boss, manifest, animationName)),
  };
}

function main() {
  const pipeline = readJson(pipelinePath);
  const templates = (pipeline.activeRuntimeBosses || []).map(buildTemplate);
  fs.mkdirSync(outputDir, { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify({
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    sourcePipeline: rel(pipelinePath),
    templates,
  }, null, 2)}\n`);
  console.log(`ACTIVE_RUNTIME_REVIEW_TEMPLATES_PASS: ${templates.length} templates`);
}

main();
