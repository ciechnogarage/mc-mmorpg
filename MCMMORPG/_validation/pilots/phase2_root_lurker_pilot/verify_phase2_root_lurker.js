#!/usr/bin/env node

'use strict';

const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const SOURCE = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const TEXTURE = path.join(__dirname, 'phase2_root_lurker_pilot.png');
const SPEC = path.join(REPO_ROOT, 'MCMMORPG/_validation/asset_specs/phase2_root_lurker_pilot.assetspec.yaml');
const EVIDENCE = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const SPEC_SHA256 = 'f708666ffbec1b68615f0753e23c5d5f83dc3f6959a45c7c12cd563dcad82546';
const PREVIOUS_SOURCE_SHA256 = '609d35b4bf4c9af45b72e7611565ce828e754fecd6576f952c90278395f4d798';
const SOURCE_SHA256 = 'ec5d04320484091dd3d0a42477d79e86bdab4d4c1e8e9e7bcddf0ec451a84236';
const STATUS = 'READY_FOR_VISUAL_REVIEW_REVISION_4';
const REQUIRED_BONES = [
  'root', 'chest', 'lower_back', 'head', 'threat_tip',
  'left_front_upper', 'left_front_lower', 'left_front_foot',
  'right_front_upper', 'right_front_lower', 'right_front_foot',
  'left_back_upper', 'left_back_lower', 'left_back_foot',
  'right_back_upper', 'right_back_lower', 'right_back_foot',
  'tail_base', 'tail_mid', 'tail_tip', 'hitbox',
];
const REQUIRED_STATIC = [
  ['front', 'front.png'],
  ['side', 'side.png'],
  ['back', 'back.png'],
  ['three_quarter', 'three_quarter.png'],
  ['player_scale', 'player_scale.png'],
  ['black_three_quarter', 'silhouette.png'],
  ['idle_start', 'animation_idle_start.png'],
  ['idle_peak', 'animation_idle_peak.png'],
  ['idle_end', 'animation_idle_end.png'],
  ['lunge_anticipation', 'animation_lunge_anticipation.png'],
  ['lunge_contact', 'animation_lunge_contact.png'],
  ['lunge_follow_through', 'animation_lunge_follow_through.png'],
  ['lunge_recovery', 'animation_lunge_recovery.png'],
];
const BLACK_VIEWS = [
  ['black_front', 'silhouette_views/front.png'],
  ['black_side', 'silhouette_views/side.png'],
  ['black_back', 'silhouette_views/back.png'],
  ['black_three_quarter_supplemental', 'silhouette_views/three_quarter.png'],
];

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
}

function rel(filePath) {
  return path.relative(REPO_ROOT, filePath);
}

function filesUnder(root) {
  if (!fs.existsSync(root)) return [];
  const output = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) output.push(...filesUnder(fullPath));
    else if (entry.isFile()) output.push(fullPath);
  }
  return output.sort();
}

function flattenOutliner(nodes, groupsById, parent = null, depth = 0, output = []) {
  for (const node of nodes || []) {
    if (!node || typeof node !== 'object' || Array.isArray(node)) continue;
    const group = groupsById.get(node.uuid);
    assert(group, `Outliner group definition missing: ${node.uuid}`);
    output.push({ node: { ...group, children: node.children || [] }, name: group.name, parent, depth });
    flattenOutliner(node.children, groupsById, group.name, depth + 1, output);
  }
  return output;
}

function keyTimes(animation) {
  const values = [];
  for (const animator of Object.values(animation.animators || {})) {
    for (const keyframe of animator.keyframes || []) values.push(Number(keyframe.time));
  }
  return [...new Set(values)].sort((a, b) => a - b);
}

function pngEntry(id, relativePath, producer, extra = {}) {
  const filePath = path.join(EVIDENCE, relativePath);
  assert(fs.existsSync(filePath), `Required render missing: ${relativePath}`);
  assert(fs.statSync(filePath).size > 0, `Required render is empty: ${relativePath}`);
  return {
    id,
    path: rel(filePath),
    sha256: sha256(filePath),
    sourceSha256: SOURCE_SHA256,
    producer,
    ...extra,
  };
}

function yamlRender(entry, indent = '  ') {
  const lines = [
    `${indent}- id: ${entry.id}`,
    `${indent}  path: ${entry.path}`,
    `${indent}  sha256: ${entry.sha256}`,
    `${indent}  source_sha256: ${entry.sourceSha256}`,
    `${indent}  producer: ${entry.producer}`,
  ];
  for (const [key, value] of Object.entries(entry)) {
    if (['id', 'path', 'sha256', 'sourceSha256', 'producer'].includes(key)) continue;
    lines.push(`${indent}  ${key.replace(/[A-Z]/g, (char) => `_${char.toLowerCase()}`)}: ${value}`);
  }
  return lines.join('\n');
}

function main() {
  assert.strictEqual(sha256(SPEC), SPEC_SHA256, 'AssetSpec hash mismatch.');
  assert.strictEqual(sha256(SOURCE), SOURCE_SHA256, 'Iteration 4 source hash mismatch.');
  const model = JSON.parse(fs.readFileSync(SOURCE, 'utf8'));
  assert.strictEqual(model.name, 'phase2_root_lurker_pilot');
  assert.strictEqual(model.meta?.model_format, 'free');
  assert.strictEqual(model.meta?.box_uv, false);
  assert(Number(model.resolution?.width) <= 64 && Number(model.resolution?.height) <= 64);

  const groupsById = new Map((model.groups || []).map((group) => [group.uuid, group]));
  const bones = flattenOutliner(model.outliner || [], groupsById);
  const boneNames = new Set(bones.map((entry) => entry.name));
  for (const name of REQUIRED_BONES) assert(boneNames.has(name), `Required bone missing: ${name}`);
  assert.strictEqual(new Set(bones.map((entry) => entry.node.uuid)).size, bones.length, 'Duplicate bone UUID.');
  assert(bones.filter((entry) => entry.name !== 'hitbox').length <= 28, 'Visual bone budget exceeded.');
  assert(Math.max(...bones.map((entry) => entry.depth)) <= 6, 'Hierarchy depth budget exceeded.');
  const hierarchy = new Map(bones.map((entry) => [entry.name, entry.parent]));
  assert.strictEqual(hierarchy.get('chest'), 'root');
  assert.strictEqual(hierarchy.get('lower_back'), 'root');
  assert.strictEqual(hierarchy.get('head'), 'chest');
  assert.strictEqual(hierarchy.get('threat_tip'), 'head');
  for (const side of ['left', 'right']) {
    assert.strictEqual(hierarchy.get(`${side}_front_upper`), 'chest');
    assert.strictEqual(hierarchy.get(`${side}_front_lower`), `${side}_front_upper`);
    assert.strictEqual(hierarchy.get(`${side}_front_foot`), `${side}_front_lower`);
    assert.strictEqual(hierarchy.get(`${side}_back_upper`), 'lower_back');
    assert.strictEqual(hierarchy.get(`${side}_back_lower`), `${side}_back_upper`);
    assert.strictEqual(hierarchy.get(`${side}_back_foot`), `${side}_back_lower`);
  }
  assert.strictEqual(hierarchy.get('tail_base'), 'lower_back');
  assert.strictEqual(hierarchy.get('tail_mid'), 'tail_base');
  assert.strictEqual(hierarchy.get('tail_tip'), 'tail_mid');
  assert.strictEqual(hierarchy.get('hitbox'), null);

  const elements = model.elements || [];
  assert(elements.length <= 96, 'Rendered element budget exceeded.');
  assert.strictEqual(new Set(elements.map((entry) => entry.uuid)).size, elements.length, 'Duplicate cube UUID.');
  const byName = new Map(elements.map((entry) => [entry.name, entry]));
  for (const name of ['shoulder_core', 'shoulder_shelf', 'rib_taper', 'belly_root', 'hip_core', 'mask_core', 'mask_brow', 'snout_lower', 'tail_root_mass', 'tail_mid_mass', 'tail_tip_root']) {
    assert(byName.has(name), `Required shell cube missing: ${name}`);
  }
  const hitboxBone = bones.find((entry) => entry.name === 'hitbox');
  assert.strictEqual(hitboxBone.node.visibility, false, 'Hitbox bone must be hidden.');
  const hitboxIds = new Set((hitboxBone.node.children || []).filter((child) => typeof child === 'string'));
  assert.strictEqual(hitboxIds.size, 1, 'Hitbox must own exactly one cube.');
  const hitboxCube = elements.find((entry) => hitboxIds.has(entry.uuid));
  assert(hitboxCube && hitboxCube.visibility === false, 'Hitbox cube must be hidden.');
  const visualElements = elements.filter((entry) => !hitboxIds.has(entry.uuid));
  const bounds = {
    min: [0, 1, 2].map((axis) => Math.min(...visualElements.map((entry) => Number(entry.from[axis])))),
    max: [0, 1, 2].map((axis) => Math.max(...visualElements.map((entry) => Number(entry.to[axis])))),
  };
  bounds.size = bounds.max.map((value, axis) => value - bounds.min[axis]);
  const sideLengthToHeightRatio = bounds.size[2] / bounds.size[1];
  const shoulderHeightUnits = Number(byName.get('shoulder_shelf').to[1]);
  const shoulderHeightToPlayerRatio = shoulderHeightUnits / 32;
  assert.strictEqual(bounds.min[1], 0, 'Neutral geometry must contact ground at Y=0.');
  assert(sideLengthToHeightRatio >= 1.8, 'Side length-to-height ratio is below 1.8.');
  assert(shoulderHeightToPlayerRatio >= 0.45 && shoulderHeightToPlayerRatio <= 0.65, 'Shoulder height is outside the player envelope.');
  assert(bounds.max[1] / 32 <= 0.8, 'Permanent geometry exceeds 80 percent of player height.');
  assert(byName.get('shoulder_shelf').to[0] - byName.get('shoulder_shelf').from[0] > byName.get('hip_core').to[0] - byName.get('hip_core').from[0], 'Shoulder shelf must dominate hip width.');
  assert(byName.get('rib_taper').to[0] - byName.get('rib_taper').from[0] < byName.get('shoulder_core').to[0] - byName.get('shoulder_core').from[0], 'Rib bridge must pinch below shoulder width.');
  assert(byName.get('tail_tip_root').to[2] > byName.get('hip_core').to[2] + 20, 'Dragged-root counterweight is not long enough.');
  const forwardHeadNames = ['mask_core', 'mask_brow'];
  const headBackEdge = Math.max(...forwardHeadNames.map((name) => Number(byName.get(name).to[2])));
  const shoulderFrontEdge = Math.min(Number(byName.get('shoulder_core').from[2]), Number(byName.get('shoulder_shelf').from[2]));
  const headNeckNotch = shoulderFrontEdge - headBackEdge;
  assert(headNeckNotch >= 1, 'Head-neck notch is not open by at least one model unit.');
  assert.strictEqual(elements.filter((entry) => entry.name === 'attack_side_root_hook').length, 1, 'Exactly one attack-side break is required.');
  assert(Math.abs(Number(byName.get('mask_core').rotation?.[1] || 0)) >= 5, 'Root wedge lacks deliberate yaw asymmetry.');
  assert(Math.abs(Number(byName.get('shoulder_shelf').rotation?.[2] || 0)) >= 3, 'Forward shoulder prow lacks a sloped front/back break.');
  const shoulderShelfWidth = Number(byName.get('shoulder_shelf').to[0]) - Number(byName.get('shoulder_shelf').from[0]);
  const maskWidth = Number(byName.get('mask_brow').to[0]) - Number(byName.get('mask_brow').from[0]);
  const ribWidth = Number(byName.get('rib_taper').to[0]) - Number(byName.get('rib_taper').from[0]);
  const hipWidth = Number(byName.get('hip_core').to[0]) - Number(byName.get('hip_core').from[0]);
  assert(shoulderShelfWidth <= 8, 'Shoulder shelf remains canopy-wide.');
  assert(maskWidth < shoulderShelfWidth, 'Root-mask wedge must remain narrower than the shoulder shelf.');
  assert(ribWidth < maskWidth && ribWidth < hipWidth, 'Rib bridge is not visibly pinched in the mass hierarchy.');

  const animations = new Map((model.animations || []).map((entry) => [entry.name, entry]));
  const idle = animations.get('idle');
  const lunge = animations.get('lunge');
  assert(idle && lunge, 'Required idle/lunge animations are missing.');
  assert.strictEqual(Number(idle.length), 2);
  assert.strictEqual(idle.loop, 'loop');
  assert.strictEqual(Number(lunge.length), 1.1);
  assert.strictEqual(lunge.loop, 'once');
  const idleTimes = keyTimes(idle);
  const lungeTimes = keyTimes(lunge);
  assert(idleTimes.every((time) => Math.abs(time * 20 - Math.round(time * 20)) < 1e-9));
  assert(lungeTimes.every((time) => Math.abs(time * 20 - Math.round(time * 20)) < 1e-9));
  for (const time of [0, 1, 2]) assert(idleTimes.includes(time), `Idle pose missing: ${time}`);
  for (const time of [0, 0.2, 0.45, 0.6, 0.8, 1.1]) assert(lungeTimes.includes(time), `Lunge phase missing: ${time}`);

  const validationBefore = JSON.parse(fs.readFileSync(path.join(EVIDENCE, 'iteration_4_validate_before_export.json'), 'utf8'));
  const validationRawReopen = JSON.parse(fs.readFileSync(path.join(EVIDENCE, 'iteration_4_validate_after_reopen_raw.json'), 'utf8'));
  const validationAfter = JSON.parse(fs.readFileSync(path.join(EVIDENCE, 'iteration_4_validate_after_reopen.json'), 'utf8'));
  assert.deepStrictEqual(validationBefore.findings || [], [], 'Pre-export validation contains findings.');
  assert((validationRawReopen.findings || []).some((finding) => finding.code === 'uv_scale_mismatch'), 'Expected raw reopen UV-density session finding is absent.');
  assert.deepStrictEqual(validationAfter.findings || [], [], 'Post-reopen validation contains findings.');
  const semanticDiff = JSON.parse(fs.readFileSync(path.join(EVIDENCE, 'iteration_4_reopen_semantic_diff.json'), 'utf8'));
  assert.deepStrictEqual(semanticDiff.differences, [], 'Native reopen semantic diff is not empty.');
  assert.strictEqual(semanticDiff.beforeSha256, semanticDiff.afterSha256, 'Native reopen semantic hashes differ.');
  const scopeDiff = JSON.parse(fs.readFileSync(path.join(EVIDENCE, 'iteration_4_scope_diff.json'), 'utf8'));
  assert.strictEqual(scopeDiff.changedAxis, 'primary_shell_read');
  assert.strictEqual(scopeDiff.changedGeometryCount, 15, 'Rebuild did not cover the declared primary shell chain.');
  assert.strictEqual(scopeDiff.boneDataUnchanged, true);
  assert.strictEqual(scopeDiff.animationDataUnchanged, true);
  assert.strictEqual(scopeDiff.hitboxUnchanged, true);

  const staticRenders = REQUIRED_STATIC.map(([id, file]) => pngEntry(id, file, 'repository_static_renderer_exact_source', {
    camera: 'fixed', stableScale: true,
  }));
  const coreViewHashes = staticRenders.filter((entry) => ['front', 'side', 'back', 'three_quarter'].includes(entry.id)).map((entry) => entry.sha256);
  assert.strictEqual(new Set(coreViewHashes).size, 4, 'Required core views are not unique.');
  const lungeHashes = staticRenders.filter((entry) => entry.id.startsWith('lunge_')).map((entry) => entry.sha256);
  assert.strictEqual(new Set(lungeHashes).size, 4, 'Lunge phase evidence is not distinct.');
  const supplementalStatic = [
    ...BLACK_VIEWS.map(([id, file]) => pngEntry(id, file, 'repository_static_renderer_exact_source', { camera: 'fixed', stableScale: true, flatBlack: true })),
    pngEntry('distance_thumbnail_20_blocks', 'distance_thumbnail/at_20_blocks.png', 'repository_static_renderer_exact_source', { camera: 'fixed', stableScale: true, viewingDistanceBlocks: 20 }),
  ];
  const liveSupplemental = ['front', 'side', 'back', 'three_quarter'].map((id) => pngEntry(id, `live_supplemental/iteration_4/exact_source/${id}.png`, 'ashfox_render_preview_supplemental', {
    limitation: 'editor_perspective_not_guaranteed_orthographic_or_shared_scale',
  }));

  const textureSha256 = sha256(TEXTURE);
  const receipt = {
    schemaVersion: 4,
    status: STATUS,
    generatedAt: new Date().toISOString(),
    assetSpec: { path: rel(SPEC), sha256: SPEC_SHA256 },
    previousSource: { sha256: PREVIOUS_SOURCE_SHA256, identityArchive: rel(path.join(EVIDENCE, 'iteration_3/evidence_identity.yaml')) },
    source: { path: rel(SOURCE), sha256: SOURCE_SHA256 },
    texture: { path: rel(TEXTURE), sha256: textureSha256, resolution: model.resolution },
    approvedWriter: {
      id: 'ashfox-v0.2.0-native-roundtrip+mc.2',
      ashfoxVersion: '0.2.0',
      blockbenchVersion: '5.1.6',
      protocolVersion: '2025-11-25',
      toolRegistryHash: '8043388b',
    },
    changedAxis: 'primary_shell_read',
    structure: {
      renderedElements: visualElements.length,
      hiddenHitboxCubes: 1,
      visualBones: bones.filter((entry) => entry.name !== 'hitbox').length,
      hierarchyDepth: Math.max(...bones.map((entry) => entry.depth)),
      bounds,
      sideLengthToHeightRatio,
      shoulderHeightUnits,
      shoulderHeightToPlayerRatio,
      shellChecks: {
        shoulderShelfDominatesHipWidth: true,
        ribBridgePinchedBelowShoulderWidth: true,
        draggedRootCounterweightExtended: true,
        asymmetricRootWedge: true,
        headNeckNotchUnits: headNeckNotch,
        attackSideBreakCount: 1,
        shoulderShelfWidth,
        maskWidth,
        ribWidth,
        hipWidth,
        staggeredRootPlanes: ['shoulder_shelf', 'shoulder_core', 'left_shoulder_plate', 'right_shoulder_plate'],
      },
    },
    animations: {
      idle: { length: Number(idle.length), loop: idle.loop, keyTimes: idleTimes },
      lunge: { length: Number(lunge.length), loop: lunge.loop, keyTimes: lungeTimes },
      timingGridSeconds: 0.05,
    },
    validation: { beforeExport: validationBefore, immediatelyAfterReopen: validationRawReopen, afterUvDensityRestore: validationAfter },
    nativeRoundtrip: semanticDiff,
    scopeDiff,
    requiredStaticRenders: staticRenders,
    supplementalStaticRenders: supplementalStatic,
    liveSupplementalRenders: liveSupplemental,
    evidenceLimitations: [
      'Orthographic projection is not claimed; the repository renderer provides fixed-camera, stable-scale evidence.',
      'Ashfox live captures are supplemental because its editor camera does not guarantee orthographic shared-scale output.',
      'Animation phases are exact-source static evaluations by the repository renderer, not live Ashfox timeline captures.',
      'Immediately after native reopen Ashfox omitted session uvPixelsPerBlock and reported uv_scale_mismatch; restoring uvPixelsPerBlock=4 without changing source semantics cleared all findings.',
      'Revision 4 did not claim moving playback; required animation images are exact-source static phase evidence only.',
      'The current visual_qa.yaml and iteration_3/visual_qa.yaml are bound to revision 3; independent visual QA for revision 4 is pending.',
      'Builder status is READY_FOR_VISUAL_REVIEW_REVISION_4 and never ART_PASS.',
    ],
    claims: { artPass: false, integrationPass: false, runtimePass: false },
  };

  fs.writeFileSync(path.join(EVIDENCE, 'verification_receipt.json'), `${JSON.stringify(receipt, null, 2)}\n`, { mode: 0o600 });
  fs.writeFileSync(path.join(EVIDENCE, 'authoring_summary.json'), `${JSON.stringify({
    status: STATUS,
    generatedAt: receipt.generatedAt,
    assetSpec: receipt.assetSpec,
    previousSource: receipt.previousSource,
    source: receipt.source,
    texture: receipt.texture,
    approvedWriter: receipt.approvedWriter,
    changedAxis: receipt.changedAxis,
    counts: { bones: bones.length, cubes: elements.length, textures: (model.textures || []).length, animations: (model.animations || []).length },
    validation: {
      beforeExportFindings: [],
      immediatelyAfterReopenFindings: validationRawReopen.findings,
      afterUvDensityRestoreFindings: [],
    },
    nativeRoundtrip: semanticDiff,
    claims: receipt.claims,
  }, null, 2)}\n`, { mode: 0o600 });

  const yaml = [
    'schema_version: 4',
    `status: ${STATUS}`,
    `asset_spec_path: ${receipt.assetSpec.path}`,
    `asset_spec_sha256: ${SPEC_SHA256}`,
    `source_path: ${receipt.source.path}`,
    `source_sha256: ${SOURCE_SHA256}`,
    `previous_source_sha256: ${PREVIOUS_SOURCE_SHA256}`,
    'required_static_renders:',
    ...staticRenders.map((entry) => yamlRender(entry)),
    'supplemental_static_renders:',
    ...supplementalStatic.map((entry) => yamlRender(entry)),
    'live_supplemental_renders:',
    ...liveSupplemental.map((entry) => yamlRender(entry)),
    'evidence_limitations:',
    '  orthographic_projection: not_claimed_repository_renderer_is_fixed_camera_stable_scale',
    '  ashfox_live_animation_timeline: not_claimed_static_exact_source_phases_used',
    '  raw_reopen_uv_density: session_value_omitted_then_restored_to_4_clean_validate_semantics_unchanged',
    '  moving_playback_revision_4: not_claimed_static_phase_evidence_only',
    '  revision_4_independent_visual_verdict: pending',
    'gate_claims:',
    '  art_pass: false',
    '  integration_pass: false',
    '  runtime_pass: false',
  ].join('\n');
  fs.writeFileSync(path.join(EVIDENCE, 'render_manifest.yaml'), `${yaml}\n`, { mode: 0o600 });

  const staticHashEntries = [...staticRenders, ...supplementalStatic]
    .map((entry) => `${entry.sha256}  ${entry.path}`);
  fs.writeFileSync(path.join(EVIDENCE, 'static_renderer.sha256'), `${staticHashEntries.join('\n')}\n`, { mode: 0o600 });
  fs.writeFileSync(path.join(EVIDENCE, 'source.sha256'), `${SOURCE_SHA256}  ${rel(SOURCE)}\n`, { mode: 0o600 });

  const inventoryFiles = [__dirname, EVIDENCE].flatMap(filesUnder)
    .filter((filePath) => path.basename(filePath) !== 'inventory.sha256');
  const inventoryLines = inventoryFiles.map((filePath) => `${sha256(filePath)}  ${rel(filePath)}`);
  fs.writeFileSync(path.join(EVIDENCE, 'inventory.sha256'), `${inventoryLines.join('\n')}\n`, { mode: 0o600 });
  console.log('PHASE2_ROOT_LURKER_REVISION_4_VERIFY_PASS');
  console.log(JSON.stringify({ status: STATUS, sourceSha256: SOURCE_SHA256, semanticHash: semanticDiff.beforeSha256, requiredRenderCount: staticRenders.length, supplementalStaticCount: supplementalStatic.length, liveSupplementalCount: liveSupplemental.length }, null, 2));
}

main();
