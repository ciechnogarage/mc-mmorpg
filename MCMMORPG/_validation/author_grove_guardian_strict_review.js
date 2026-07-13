const fs = require('fs');
const path = require('path');
const { animationPhaseTimesFromAnimation } = require('./modelengine_phase_utils');
const { sha1 } = require('./manual_review_contract');

const ROOT = __dirname;
const modelId = 'level_1_grove_guardian';
const blueprintPath = path.resolve(ROOT, '..', 'plugins', 'ModelEngine', 'blueprints', modelId, `${modelId}.bbmodel`);
const qualityPath = path.join(ROOT, 'model_quality', `${modelId}.quality.json`);
const skillsPath = path.resolve(ROOT, '..', 'plugins', 'MythicMobs', 'Packs', 'level_1', 'skills', 'grove_guardian.skill.yml');
const mobPath = path.resolve(ROOT, '..', 'plugins', 'MythicMobs', 'Packs', 'level_1', 'mobs', 'grove_guardian.mob.yml');
const runtimePath = path.join(ROOT, 'model_reviews', modelId, 'runtime-probe.md');
const outputPath = path.join(ROOT, 'active_runtime_reviews', 'completed_runtime_boss_reviews.json');

const blueprint = JSON.parse(fs.readFileSync(blueprintPath, 'utf8'));
const quality = JSON.parse(fs.readFileSync(qualityPath, 'utf8'));

function relative(file) {
  return path.relative(ROOT, file);
}

function evidence(artifact, sourceClass, file, locator, detail) {
  return {
    artifact,
    sourceClass,
    source: relative(file),
    locator,
    detail,
    sourceHash: sha1(file),
  };
}

const qualityEvidence = (field, detail) =>
  evidence('quality_manifest', 'generated_review', qualityPath, `field ${field}`, detail);
const blueprintEvidence = (node, detail) =>
  evidence('bbmodel', 'active_runtime_repo', blueprintPath, `node ${node}`, detail);
const yamlEvidence = (skill, detail) =>
  evidence('yaml', 'active_runtime_repo', skillsPath, `path skill ${skill}`, detail);
const renderEvidence = (name, detail, time = null) => {
  const file = path.join(ROOT, 'model_reviews', modelId, `${name}.png`);
  const locator = time === null ? `view ${name}` : `view ${name} frame ${time}s`;
  return evidence('render', 'generated_review', file, locator, detail);
};
const runtimeEvidence = (detail) =>
  evidence('runtime', 'runtime_probe', runtimePath, 'command mm mobs spawn and event display-entity delta', detail);

const animationNotes = {
  passive: {
    poseRead: 'Low-amplitude rooted breathing preserves the dormant idol read.',
    massTransfer: 'Motion remains centered in the trunk, chest, head, and anchor roots.',
    contactRead: 'No contact event is intended.',
    recoveryRead: 'The loop returns to the rooted resting pose without a visible snap.',
  },
  awaken: {
    poseRead: 'The crown, jaw, arms, and anchor roots unfold into a clear encounter reveal.',
    massTransfer: 'The reveal expands upward and outward from the trunk instead of moving only the hands.',
    contactRead: 'Tick 10 is an environmental root beat, not a damage contact.',
    recoveryRead: 'The final pose settles into the combat idle before AI is restored.',
  },
  idle: {
    poseRead: 'The broad shoulders, mask, crown, and hanging hands remain readable.',
    massTransfer: 'Subtle trunk-led motion avoids making the stone-and-wood mass look weightless.',
    contactRead: 'No contact event is intended.',
    recoveryRead: 'The loop closes on the same grounded stance.',
  },
  walk: {
    poseRead: 'Alternating limbs retain the wide colossus silhouette.',
    massTransfer: 'The body and root feet move with the limbs, though the single review frame cannot prove foot planting.',
    contactRead: 'Foot contact timing is not proven in runtime.',
    recoveryRead: 'The cycle returns to a balanced stance.',
  },
  attack: {
    poseRead: 'A committed right-arm sweep creates a strong lateral attack silhouette.',
    massTransfer: 'The trunk leans and rotates with the arm, producing a whole-body kinetic chain.',
    contactRead: 'The right_impact anchor, totem, sound, and particles are configured for tick 14.',
    recoveryRead: 'The authored tail returns the torso and arm before the model lock releases.',
  },
  root_smash: {
    poseRead: 'Both arms extend into a broad ground-smash shape distinct from the single-arm sweep.',
    massTransfer: 'The chest, head, and both arm chains pitch into the strike.',
    contactRead: 'Two local hand volumes are configured at tick 18.',
    recoveryRead: 'The 2.2 second animation leaves room for a readable lift after impact.',
  },
  thorn_nova: {
    poseRead: 'The opened crown, jaw, chest, and raised arms create a distinct radial charge silhouette.',
    massTransfer: 'Motion expands upward and outward through the chest, crown, and both arm chains rather than following a directional limb strike.',
    contactRead: 'The thorn_origin launches the configured radial target projectiles at tick 18.',
    recoveryRead: 'The short recovery returns to the core stance before model unlock.',
  },
  summon: {
    poseRead: 'The rooted channel pose reads as restraint, but depends on particles and title for separation from thorn_nova.',
    massTransfer: 'The upper body folds around the summon origin while the feet stay planted.',
    contactRead: 'No damage contact is intended; adds spawn after the configured channel delay.',
    recoveryRead: 'The channel resolves before model unlock at the end of the state.',
  },
  enrage: {
    poseRead: 'The exposed core and expanded upper body establish a phase-change focal point.',
    massTransfer: 'The body, head, jaw, and arms participate in the transition.',
    contactRead: 'No direct damage contact is intended.',
    recoveryRead: 'The state settles into the enraged stance and persistent buffs.',
  },
  hit: {
    poseRead: 'A short full-body recoil communicates reaction without replacing the boss silhouette.',
    massTransfer: 'The trunk, chest, and head recoil as one heavy unit.',
    contactRead: 'The animation responds to damage and does not create damage.',
    recoveryRead: 'The half-second state returns quickly to encounter control.',
  },
  death: {
    poseRead: 'The crown, torso, arms, and legs collapse into a low defeated mass.',
    massTransfer: 'Multiple large body regions settle instead of only rotating the root.',
    contactRead: 'No outgoing damage contact is intended.',
    recoveryRead: 'There is no combat recovery; the terminal settle supports the death finale.',
  },
};

const bindingByAnimation = new Map();
for (const binding of quality.skillBindings) {
  if (!bindingByAnimation.has(binding.animation)) bindingByAnimation.set(binding.animation, []);
  bindingByAnimation.get(binding.animation).push(binding);
}

function sync(status, reason, items = []) {
  const result = { status };
  if (reason) result.reason = reason;
  if (items.length) result.evidence = items;
  return result;
}

function gameplaySync(name) {
  const bindings = bindingByAnimation.get(name) || [];
  if (!bindings.length) {
    return {
      damage: sync('not_applicable', 'This state does not author outgoing damage.'),
      hitbox: sync('not_applicable', 'This state does not author a gameplay hit volume.'),
      sound: sync('unknown', 'No runtime capture proves sound playback for this unbound state.'),
      vfx: sync('unknown', 'No runtime capture proves VFX playback for this unbound state.'),
    };
  }
  const sources = bindings.map((binding) =>
    yamlEvidence(binding.skill, `Declared ModelEngine state and synchronization for ${name}`));
  const damaging = ['attack', 'root_smash', 'thorn_nova'].includes(name);
  const localVolume = ['attack', 'root_smash', 'thorn_nova'].includes(name);
  return {
    damage: damaging
      ? sync('confirmed', null, sources)
      : sync('not_applicable', 'This bound state changes encounter state or summons adds without direct damage.'),
    hitbox: localVolume
      ? sync('confirmed', null, sources)
      : sync('not_applicable', 'This bound state does not require a damage hit volume.'),
    sound: sync('confirmed', null, sources),
    vfx: sync('confirmed', null, sources),
  };
}

const animationFindings = blueprint.animations.map((animation) => {
  const phases = animationPhaseTimesFromAnimation(animation);
  const reviewImage = path.join(ROOT, 'model_reviews', modelId, `animation_${animation.name}.png`);
  const imageExists = fs.existsSync(reviewImage);
  const phaseContract = quality.animationContract.find((item) => item.animation === animation.name);
  const bindings = bindingByAnimation.get(animation.name) || [];
  return {
    name: animation.name,
    phaseTimes: {
      start: phases.start,
      anticipation: phases.anticipation,
      impact: phases.impact,
      recovery: phases.recovery,
      end: phases.end,
    },
    phaseMethod: phases.method === 'timeline' ? 'timeline' : 'keyframe_motion',
    phaseRationale: `${phases.method} derivation from source keyframes; authored impact ticks: ${phaseContract?.impactTicks.join(', ') || 'none'}.`,
    ...animationNotes[animation.name],
    runtimeBinding: bindings.length
      ? `Configured in ${bindings.map((item) => item.skill).join(', ')}; live execution of this individual state is not captured by the spawn probe.`
      : 'No dedicated MythicMobs skill binding is declared; this is a default, reaction, or terminal ModelEngine state.',
    gameplaySync: gameplaySync(animation.name),
    evidence: [
      blueprintEvidence(`animation ${animation.name}`, `Source keyframes and duration for ${animation.name}`),
      ...(imageExists
        ? [renderEvidence(`animation_${animation.name}`, `Inspected pose for ${animation.name}`, phases.impact)]
        : []),
      ...bindings.map((binding) => yamlEvidence(binding.skill, `Declared binding for ${animation.name}`)),
    ],
    confidence: imageExists ? 'medium' : 'low',
    unknowns: [
      'No live frame capture proves the authored pose at player camera distance.',
      ...(['attack', 'root_smash', 'thorn_nova'].includes(animation.name)
        ? ['Damage and moving-volume contact are source-confirmed but not runtime-observed.']
        : []),
    ],
  };
});

const review = {
  id: modelId,
  modelKey: `${modelId}::${relative(blueprintPath)}`,
  sourceHash: sha1(blueprintPath),
  reviewedSources: [
    relative(blueprintPath),
    relative(qualityPath),
    relative(skillsPath),
    relative(mobPath),
    relative(runtimePath),
    ...quality.renderEvidence.map((source) => path.normalize(path.join('model_quality', source))),
  ],
  stale: false,
  identity: 'A monumental living grove idol and first-dungeon teaching boss, rooted into the arena rather than presented as a mobile humanoid.',
  silhouette: 'The wide asymmetric crown, rectangular stone mask, long branch arms, broad hands, and root feet survive the distant read. The lower body is cleaner after removing redundant trunk and shin plates, although some root detail still merges at reduced scale.',
  proportions: 'Roughly four player-heights tall with oversized crown, chest, forearms, and hands. Shorter leg mass and wide feet keep the center of gravity low, although the humanoid shoulder-to-hip structure is still visible.',
  composition: 'The face and restrained lime core accents provide the focal center; unequal crown forks frame it above and hanging arms frame it laterally. Stone plates break up the green-brown wood mass while the simplified waist and shins preserve larger uninterrupted forms.',
  materials: 'Wood, foliage, stone armor, and luminous lime accents are visibly separated by hue and value. The renderer proves mapped appearance, not in-game shader or emissive behavior.',
  palette: 'Muted bark brown, moss green, and cool gray establish the grove-idol identity. Lime eyes/core are effective focal accents but currently repeat in several nearby facial and torso regions.',
  atmosphere: 'Ancient, heavy, and corrupted rather than agile. The crown and mask carry the strongest identity; encounter atmosphere relies on the authored particles, sounds, titles, and arena response that are source-bound but not fully captured live.',
  rig: 'Segmented trunk, head, jaw, limbs, crown, roots, hidden hitbox, two hand impacts, thorn origin, summon origin, and core VFX anchor form a gameplay-oriented hierarchy.',
  gameplay: 'Seven skill bindings map reveal, melee, smash, nova, summon variants, and enrage to explicit states and locks. Spawn proof confirms registration and rendered-part delivery; individual attack contact, sound, and VFX execution remain outside current runtime evidence.',
  transferableTechniques: [
    'Use one distant-read mass hierarchy before adding bark, plate, and root detail.',
    'Design helper bones together with visible limbs and encounter mechanics.',
    'Separate cast families through whole-body pose first, then reinforce them with VFX and sound.',
    'Keep source binding proof distinct from live execution proof.',
  ],
  weaknesses: [
    'The body below the asymmetric crown still uses a recognizable humanoid shoulder-to-hip structure.',
    'Small toe and anchor-root pieces still merge at reduced player distance.',
    'Summon remains more dependent on particles and title than the newly expanded thorn-nova silhouette.',
    'No current runtime capture proves attack contact, exact sound timing, particles, emissive behavior, or phase transition presentation.',
  ],
  claims: [
    {
      type: 'visual_observation',
      statement: 'The crown, stone mask, long arms, large hands, and root feet establish a readable grove-colossus identity at player scale.',
      evidence: [
        renderEvidence('player_scale', 'Player-height comparison and front-facing mass hierarchy'),
        renderEvidence('three_quarter', 'Crown, mask, arm length, hand scale, and layered trunk'),
      ],
    },
    {
      type: 'visual_observation',
      statement: 'The silhouette is grounded and top-heavy; unequal crown forks and simplified lower plating improve hierarchy while the humanoid torso remains visible.',
      evidence: [
        renderEvidence('silhouette', 'Outer contour of crown, shoulders, arms, hands, and root feet'),
        renderEvidence('front', 'Front-view symmetry and lower plate density'),
      ],
    },
    {
      type: 'visual_observation',
      statement: 'Brown wood, green foliage, gray stone, and lime focal accents are visibly separated; true emissive behavior is not proven by these renders.',
      evidence: [
        renderEvidence('three_quarter', 'Mapped material and palette separation'),
        renderEvidence('back', 'Rear material distribution and crown readability'),
      ],
    },
    {
      type: 'source_fact',
      statement: 'The rig declares a hidden central hitbox and dedicated hand-impact, thorn, summon, and core-effect anchors.',
      evidence: [
        blueprintEvidence('outliner and elements', 'Rig hierarchy, hitbox visibility, and helper-bone definitions'),
        renderEvidence('helpers', 'Rendered helper locations'),
        renderEvidence('hitbox', 'Central gameplay hitbox relative to visible mass'),
      ],
    },
    {
      type: 'source_fact',
      statement: 'Seven skill bindings connect authored states to locks, impact ticks, model-part targets, damage, sound, particles, summons, and phase behavior.',
      evidence: [
        qualityEvidence('skillBindings', 'Declared model-to-skill contract'),
        yamlEvidence('level_1_grove_guardian_basic_attack', 'Melee state, moving hand volume, damage callback, sound, and particles'),
        yamlEvidence('level_1_grove_guardian_root_smash', 'Two-hand local volumes and synchronized impact'),
        yamlEvidence('level_1_grove_guardian_thorn_nova', 'Thorn origin and projectile damage path'),
      ],
    },
    {
      type: 'runtime_observation',
      statement: 'Reload, registration, spawn, base-entity invisibility, one model-owner delta, and 42 delivered display entities were observed successfully.',
      evidence: [
        runtimeEvidence('Runtime probe results for reload, registration, live spawn, invisibility, and rendered-part delivery'),
      ],
    },
    {
      type: 'design_inference',
      statement: 'Compared with the selected specialized references, the model has a coherent original encounter identity but still needs stronger cast-pose separation and live combat synchronization evidence.',
      evidence: [
        qualityEvidence('references', 'Subsystem reference provenance and declared direction'),
        renderEvidence('animation_attack', 'Strong directional melee pose'),
        renderEvidence('animation_thorn_nova', 'Less distinct contracted cast pose'),
        renderEvidence('animation_summon', 'Channel pose that depends on sensory reinforcement'),
        runtimeEvidence('Spawn proof boundary explicitly excludes individual combat-state execution'),
      ],
    },
  ],
  animationFindings,
  confidence: 'medium',
  unknowns: [
    'Strict same-camera comparison renders for each selected reference are not yet stored.',
    'Live execution of each attack, phase transition, death sequence, sound layer, VFX layer, and moving hit volume is not captured.',
    'The runtime probe cannot prove client-side mapped texture or emissive quality because its viewer does not resolve generated item models.',
  ],
};

const output = {
  schemaVersion: 1,
  reviewStatus: 'strict_runtime',
  generatedBy: 'manual evidence-backed authoring script',
  models: [review],
};
fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`);
console.log(`GROVE_GUARDIAN_STRICT_REVIEW_PASS: ${animationFindings.length} animation findings`);
