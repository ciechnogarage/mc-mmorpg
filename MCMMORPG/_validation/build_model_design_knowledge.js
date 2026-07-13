const fs = require('fs');
const path = require('path');

const ROOT = __dirname;
const CORPUS_DIR = path.join(ROOT, 'reference_corpus');
const CARDS_PATH = path.join(CORPUS_DIR, 'modelengine-model-cards.json');
const ROUTER_PATH = path.join(CORPUS_DIR, 'modelengine-design-router.json');
const RISKS_PATH = path.join(CORPUS_DIR, 'modelengine-negative-patterns.json');
const GUIDE_PATH = path.resolve(ROOT, '..', '..', 'docs', 'ai', 'modelengine-design-routing.md');

const SUBSYSTEMS = {
  silhouette: {
    intent: 'dominant mass, distant read, proportion hierarchy, and focal asymmetry',
    score: (card) =>
      30 * card.geometry.dominantVolumeShare +
      18 * card.geometry.topFiveVolumeShare +
      12 * card.geometry.rotatedRatio +
      8 * Math.min(card.elements / 120, 1),
  },
  anatomy: {
    intent: 'proportions, hierarchy depth, articulated structure, and anatomy fit',
    score: (card) =>
      12 * Math.min(card.depth / 6, 1) +
      16 * Math.min(card.bones / 60, 1) +
      8 * Math.min(card.elements / 160, 1),
  },
  materials: {
    intent: 'palette, value separation, material contrast, and emissive restraint',
    score: (card) => {
      const material = card.materialProfile || {};
      return (
        16 * Math.min((material.averageContrast || 0) / 0.3, 1) +
        8 * Math.min((material.textures || 0) / 4, 1) +
        6 * Math.min((material.emissiveTextures || 0) / 2, 1)
      );
    },
  },
  rig: {
    intent: 'readable hierarchy, purposeful helper bones, and gameplay anchors',
    score: (card) =>
      14 * Math.min(card.depth / 6, 1) +
      15 * Math.min(card.bones / 70, 1) +
      14 * Math.min(card.helperBones / 12, 1) +
      10 * Math.min(card.hitboxes / 8, 1),
  },
  locomotion: {
    intent: 'weight transfer, cycle coverage, and secondary motion',
    score: (card) =>
      18 * Math.min((card.animationRoles.locomotion || 0) / 4, 1) +
      12 * animationQuality(card, 'locomotion'),
  },
  attacks: {
    intent: 'anticipation, impact, recovery, and distinct attack silhouettes',
    score: (card) =>
      20 * Math.min((card.animationRoles.attack || 0) / 5, 1) +
      14 * animationQuality(card, 'attack') +
      8 * Math.min(card.timelineFrames / 12, 1),
  },
  hitVolumes: {
    intent: 'local moving hit volumes and contact-part alignment',
    score: (card) =>
      24 * Math.min(card.hitboxes / 8, 1) +
      9 * Math.min(card.helperBones / 10, 1) +
      8 * Math.min(card.runtimeBindings / 3, 1),
  },
  vfx: {
    intent: 'stable effect anchors, readable charge states, and synchronized effects',
    score: (card) =>
      18 * Math.min(card.helperBones / 14, 1) +
      13 * Math.min(card.timelineFrames / 15, 1) +
      8 * Math.min((card.materialProfile?.emissiveTextures || 0) / 2, 1),
  },
  sound: {
    intent: 'layered cues synchronized to anticipation, movement, and impact',
    score: (card) =>
      18 * Math.min(card.timelineFrames / 15, 1) +
      12 * Math.min(card.runtimeBindings / 3, 1) +
      8 * Math.min(card.linkedYaml / 2, 1),
  },
  states: {
    intent: 'state coverage, transitions, phases, reactions, and transformations',
    score: (card) =>
      14 * Math.min((card.animationRoles.transition || 0) / 4, 1) +
      10 * Math.min((card.animationRoles.reaction || 0) / 3, 1) +
      12 * Math.min(card.runtimeBindings / 3, 1) +
      8 * Math.min(card.linkedYaml / 2, 1),
  },
  death: {
    intent: 'readable defeat, collapse or detachment, and encounter closure',
    score: (card) =>
      20 * Math.min((card.animationRoles.death || 0) / 2, 1) +
      12 * animationQuality(card, 'death') +
      6 * Math.min(card.timelineFrames / 10, 1),
  },
};

function animationQuality(card, role) {
  const animations = card.animationsDetail.filter((animation) => animation.role === role);
  if (!animations.length) return 0;
  const averageMovingShare = animations.reduce(
    (sum, animation) => sum + Math.min(animation.movingBones / Math.max(card.bones, 1), 1),
    0,
  ) / animations.length;
  const phaseComplete = animations.filter((animation) =>
    animation.phaseTimes &&
    animation.sampleTimes?.length === 5 &&
    animation.phaseTimes.impact >= animation.phaseTimes.anticipation &&
    animation.phaseTimes.recovery >= animation.phaseTimes.impact).length / animations.length;
  return (averageMovingShare + phaseComplete) / 2;
}

function limitations(card, subsystem) {
  const result = [];
  if (!card.linkedYaml) result.push('no direct MythicMobs linkage');
  if (!card.runtimeBindings) result.push('no runtime binding evidence');
  if (!card.helperBones && ['rig', 'hitVolumes', 'vfx'].includes(subsystem)) {
    result.push('no detected helper bones');
  }
  if (!card.hitboxes && subsystem === 'hitVolumes') result.push('no named hitbox bones');
  if (!card.timelineFrames && ['attacks', 'vfx', 'sound'].includes(subsystem)) {
    result.push('no timeline synchronization evidence');
  }
  if (!card.materialProfile?.textures && subsystem === 'materials') {
    result.push('no texture metrics');
  }
  return result;
}

function evidence(card, subsystem) {
  const common = {
    file: card.file,
    archetype: card.archetype,
    tier: card.tier,
    familyId: card.familyId,
  };
  const specific = {
    silhouette: { geometry: card.geometry },
    anatomy: { elements: card.elements, bones: card.bones, depth: card.depth },
    materials: { materialProfile: card.materialProfile },
    rig: { bones: card.bones, depth: card.depth, helperBones: card.helperBones, hitboxes: card.hitboxes },
    locomotion: { locomotionAnimations: card.animationRoles.locomotion || 0 },
    attacks: { attackAnimations: card.animationRoles.attack || 0, timelineFrames: card.timelineFrames },
    hitVolumes: { hitboxes: card.hitboxes, helperBones: card.helperBones, runtimeBindings: card.runtimeBindings },
    vfx: { helperBones: card.helperBones, timelineFrames: card.timelineFrames, emissiveTextures: card.materialProfile?.emissiveTextures || 0 },
    sound: { timelineFrames: card.timelineFrames, linkedYaml: card.linkedYaml, runtimeBindings: card.runtimeBindings },
    states: { animationRoles: card.animationRoles, linkedYaml: card.linkedYaml, runtimeBindings: card.runtimeBindings },
    death: { deathAnimations: card.animationRoles.death || 0, timelineFrames: card.timelineFrames },
  };
  return { ...common, ...specific[subsystem] };
}

function rank(cards, subsystem, archetype = null, limit = 8) {
  const definition = SUBSYSTEMS[subsystem];
  return cards
    .filter((card) => !archetype || card.archetype === archetype)
    .map((card) => ({
      id: card.id,
      score: Number(definition.score(card).toFixed(3)),
      evidence: evidence(card, subsystem),
      limitations: limitations(card, subsystem),
    }))
    .filter((candidate) => candidate.score > 0)
    .sort((a, b) => b.score - a.score || a.id.localeCompare(b.id))
    .slice(0, limit);
}

function negativePatterns(cards) {
  const definitions = [
    {
      id: 'complexity_without_integration',
      description: 'High structural complexity without direct runtime linkage is not production proof.',
      match: (card) => card.score >= 60 && (!card.linkedYaml || !card.runtimeBindings),
      evidence: (card) => ({ score: card.score, linkedYaml: card.linkedYaml, runtimeBindings: card.runtimeBindings }),
      avoidWhen: 'Selecting a production-ready reference or claiming encounter integration.',
      acceptance: 'Require resolved YAML bindings and runtime observation.',
    },
    {
      id: 'animation_count_without_combat_coverage',
      description: 'Many animations can coexist with missing attack, reaction, transition, or death coverage.',
      match: (card) => card.animations >= 12 &&
        (!(card.animationRoles.attack || 0) || !(card.animationRoles.death || 0)),
      evidence: (card) => ({ animations: card.animations, animationRoles: card.animationRoles }),
      avoidWhen: 'Using animation count as a proxy for encounter completeness.',
      acceptance: 'Check role coverage and each required encounter state.',
    },
    {
      id: 'rig_without_gameplay_anchors',
      description: 'A deep or large rig without helpers and hitboxes may animate well but expose weak gameplay architecture.',
      match: (card) => (card.bones >= 40 || card.depth >= 5) && !card.helperBones && !card.hitboxes,
      evidence: (card) => ({ bones: card.bones, depth: card.depth, helperBones: card.helperBones, hitboxes: card.hitboxes }),
      avoidWhen: 'Borrowing rig architecture for combat, projectiles, VFX, or interactions.',
      acceptance: 'Design explicit anchors and local hit volumes for the new encounter.',
    },
    {
      id: 'timeline_assumed_from_keyframes',
      description: 'Dense keyframes do not prove explicit gameplay synchronization.',
      match: (card) => card.keyframes >= 1000 && !card.timelineFrames,
      evidence: (card) => ({ keyframes: card.keyframes, timelineFrames: card.timelineFrames }),
      avoidWhen: 'Inferring impact, sound, VFX, or damage timing from motion density.',
      acceptance: 'Inspect animation phases and linked skill timing directly.',
    },
    {
      id: 'detail_without_dominant_mass',
      description: 'Large element counts with weak dominant-volume hierarchy can produce visual noise.',
      match: (card) => card.elements >= 120 && card.geometry.dominantVolumeShare < 0.12,
      evidence: (card) => ({ elements: card.elements, dominantVolumeShare: card.geometry.dominantVolumeShare }),
      avoidWhen: 'Borrowing surface detail before the distant silhouette reads.',
      acceptance: 'Prove one dominant form or a deliberate grouped-mass hierarchy in silhouette renders.',
    },
  ];

  return definitions.map((definition) => {
    const matches = cards.filter(definition.match);
    return {
      id: definition.id,
      description: definition.description,
      avoidWhen: definition.avoidWhen,
      acceptance: definition.acceptance,
      prevalence: { matches: matches.length, total: cards.length },
      examples: matches.slice(0, 12).map((card) => ({
        id: card.id,
        file: card.file,
        evidence: definition.evidence(card),
      })),
    };
  });
}

function markdown(router, risks) {
  const lines = [
    '# ModelEngine Design Routing',
    '',
    'This guide routes design questions to evidence. It does not prescribe a house style or authorize copying a source model.',
    '',
    '## Synthesis Contract',
    '',
    '1. Write the intended distant read, role, mood, scale, dominant shape, and player-facing mechanic.',
    '2. Choose independent references for each relevant subsystem and inspect their source files, renders, and bindings.',
    '3. Record what principle is being borrowed, its limitation, and the deliberate way the new design differs.',
    '4. Form a falsifiable hypothesis about the expected visual or runtime improvement.',
    '5. Validate at equivalent camera scale and in runtime; preserve failed hypotheses as learning evidence.',
    '6. Promote a lesson only after repeated or strong render/runtime confirmation.',
    '',
    '## Subsystem Routes',
    '',
  ];
  for (const [name, route] of Object.entries(router.subsystems)) {
    lines.push(`### ${name}`, '', route.intent, '');
    for (const candidate of route.candidates.slice(0, 5)) {
      lines.push(`- \`${candidate.id}\` (${candidate.score}): ${candidate.limitations.join('; ') || 'no structural limitation detected'}.`);
    }
    lines.push('');
  }
  lines.push('## Evidence-Backed Risks', '');
  for (const risk of risks.patterns) {
    lines.push(`- **${risk.id}** (${risk.prevalence.matches}/${risk.prevalence.total}): ${risk.description} ${risk.acceptance}`);
  }
  lines.push('', 'Machine-readable sources: `modelengine-design-router.json` and `modelengine-negative-patterns.json`.', '');
  return lines.join('\n');
}

function main() {
  const source = JSON.parse(fs.readFileSync(CARDS_PATH, 'utf8'));
  const cards = source.cards;
  const archetypes = [...new Set(cards.map((card) => card.archetype))].sort();
  const generatedAt = new Date().toISOString();

  const subsystems = {};
  for (const [name, definition] of Object.entries(SUBSYSTEMS)) {
    subsystems[name] = {
      intent: definition.intent,
      candidates: rank(cards, name),
    };
  }
  const archetypeRoutes = {};
  for (const archetype of archetypes) {
    archetypeRoutes[archetype] = {};
    for (const name of Object.keys(SUBSYSTEMS)) {
      archetypeRoutes[archetype][name] = rank(cards, name, archetype, 5);
    }
  }

  const router = {
    schemaVersion: 1,
    generatedAt,
    source: path.relative(ROOT, CARDS_PATH),
    corpus: source.corpus,
    contract: {
      purpose: 'Evidence for original synthesis, not a template library.',
      selection: 'Use several subsystem-specific references matched by anatomy and encounter role.',
      requiredDecisionFields: ['intent', 'principle', 'limitation', 'deliberateDifference', 'hypothesis', 'acceptanceEvidence'],
    },
    coverage: { cards: cards.length, archetypes: archetypes.length, subsystems: Object.keys(SUBSYSTEMS).length },
    subsystems,
    archetypeRoutes,
  };
  const risks = {
    schemaVersion: 1,
    generatedAt,
    source: path.relative(ROOT, CARDS_PATH),
    contract: 'Patterns are diagnostic risks with explicit evidence and acceptance checks, not blanket bans.',
    patterns: negativePatterns(cards),
  };

  fs.writeFileSync(ROUTER_PATH, `${JSON.stringify(router, null, 2)}\n`);
  fs.writeFileSync(RISKS_PATH, `${JSON.stringify(risks, null, 2)}\n`);
  fs.writeFileSync(GUIDE_PATH, markdown(router, risks));
  console.log(`MODEL_DESIGN_KNOWLEDGE_PASS: ${cards.length} cards, ${archetypes.length} archetypes, ${Object.keys(SUBSYSTEMS).length} subsystems`);
}

main();
