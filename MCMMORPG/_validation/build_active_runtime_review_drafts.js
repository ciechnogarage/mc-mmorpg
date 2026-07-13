const fs = require('fs');
const path = require('path');

const validationRoot = __dirname;
const templatesPath = path.join(validationRoot, 'active_runtime_reviews', 'runtime_boss_review_templates.json');
const outputPath = path.join(validationRoot, 'active_runtime_reviews', 'runtime_boss_review_drafts.json');

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function claimSkeleton(template, claimKey) {
  const runtimeAnchor = template.evidenceAnchors.runtimeProbe ? [template.evidenceAnchors.runtimeProbe] : [];
  const qualityAnchor = template.evidenceAnchors.qualityManifest ? [template.evidenceAnchors.qualityManifest] : [];

  const defaults = {
    identity_and_distant_read: {
      type: 'visual_observation',
      statement: '',
      suggestedEvidence: qualityAnchor,
      prompt: 'Describe distant read, role, scale, and immediate encounter identity from inspected renders.',
    },
    silhouette_proportions_composition: {
      type: 'visual_observation',
      statement: '',
      suggestedEvidence: qualityAnchor,
      prompt: 'Explain dominant masses, asymmetry, grounding, and proportion hierarchy.',
    },
    materials_palette_emissive_uv: {
      type: 'visual_observation',
      statement: '',
      suggestedEvidence: qualityAnchor,
      prompt: 'Judge materials, palette separation, emissive control, and any visible texture weaknesses.',
    },
    rig_helpers_hitbox_eye_height: {
      type: 'source_fact',
      statement: '',
      suggestedEvidence: qualityAnchor,
      prompt: 'Verify rig segmentation, helper-bone intent, hitbox strategy, and eye-height/gameplay anchors.',
    },
    encounter_graph_truth: {
      type: 'source_fact',
      statement: '',
      suggestedEvidence: qualityAnchor,
      prompt: 'Summarize what the integration graph actually proves about skills, parts, states, and encounter mechanics.',
    },
    runtime_binding_and_spawn_proof: {
      type: 'runtime_observation',
      statement: '',
      suggestedEvidence: runtimeAnchor,
      prompt: 'Record what runtime proof confirms about reload, spawn, invisibility of the base entity, and rendered parts.',
    },
    remaining_weaknesses_against_references: {
      type: 'design_inference',
      statement: '',
      suggestedEvidence: qualityAnchor.concat(runtimeAnchor),
      prompt: 'State what still reads weaker than the selected references and why that matters in encounter readability.',
    },
  };

  return {
    claimKey,
    ...defaults[claimKey],
  };
}

function animationDraft(template, animation) {
  return {
    name: animation.name,
    status: animation.status,
    manualChecklist: animation.manualChecklist,
    phaseContract: animation.phaseContract,
    skillBindings: animation.skillBindings,
    draft: {
      name: animation.name,
      phaseTimes: {
        start: null,
        anticipation: null,
        impact: null,
        recovery: null,
        end: null,
      },
      phaseMethod: 'manual',
      phaseRationale: '',
      poseRead: '',
      massTransfer: '',
      contactRead: '',
      recoveryRead: '',
      runtimeBinding: '',
      gameplaySync: {
        damage: { status: 'unknown', reason: '' },
        hitbox: { status: 'unknown', reason: '' },
        sound: { status: 'unknown', reason: '' },
        vfx: { status: 'unknown', reason: '' },
      },
      suggestedEvidence: animation.evidenceAnchors,
      confidence: 'pending_manual_review',
      unknowns: [],
    },
  };
}

function draftForTemplate(template, goldenPathMobId) {
  return {
    schemaVersion: 1,
    status: 'pending_manual_authoring',
    recommendedAsGoldenPath: template.mobId === goldenPathMobId,
    sourceClass: template.sourceClass,
    mobId: template.mobId,
    modelId: template.modelId,
    queueStatus: template.queueStatus,
    blockers: template.blockers,
    reviewedSources: template.reviewedSources,
    sourceHashes: template.sourceHashes,
    provenance: template.provenance,
    authoringOrder: [
      'inspect reviewedSources and current renders before writing claims',
      'fill model-level sections from observed evidence, not from metrics alone',
      'write one runtime-backed claim only from runtime_probe evidence',
      'complete every animation draft exactly once before marking the review ready',
      'promote only the finished strict review content into a tracked batch or downstream review store',
    ],
    strictReviewScaffold: {
      identity: '',
      silhouette: '',
      proportions: '',
      composition: '',
      materials: '',
      palette: '',
      atmosphere: '',
      rig: '',
      gameplay: '',
      transferableTechniques: [],
      weaknesses: [],
      claims: template.requiredClaims.map((claimKey) => claimSkeleton(template, claimKey)),
      animationFindings: template.animationTemplates.map((animation) => animationDraft(template, animation)),
      confidence: 'pending_manual_review',
      unknowns: [
        'Document any unresolved differences between authored motion and runtime behavior.',
        'Record any evidence gaps that still require a new render or runtime probe.',
      ],
    },
  };
}

function main() {
  const document = readJson(templatesPath);
  const pendingTemplates = (document.templates || []).filter((template) => template.queueStatus === 'strict_ready');
  const goldenPath = pendingTemplates[0];
  const drafts = pendingTemplates.map((template) =>
    draftForTemplate(template, goldenPath?.mobId || null));

  fs.writeFileSync(outputPath, `${JSON.stringify({
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    sourceTemplates: path.relative(validationRoot, templatesPath),
    drafts,
  }, null, 2)}\n`);

  console.log(`ACTIVE_RUNTIME_REVIEW_DRAFTS_PASS: ${drafts.length} drafts`);
}

main();
