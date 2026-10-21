#!/usr/bin/env node

'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { render, releaseModelTextures } = require('../../render_bbmodel_review');

const REPO_ROOT = path.resolve(__dirname, '..', '..', '..', '..');
const SOURCE_PATH = path.join(__dirname, 'phase2_root_lurker_pilot.bbmodel');
const EVIDENCE_ROOT = path.join(REPO_ROOT, 'MCMMORPG/_validation/model_reviews/phase2_root_lurker_pilot');
const SILHOUETTE_ROOT = path.join(EVIDENCE_ROOT, 'silhouette_views');
const DISTANCE_ROOT = path.join(EVIDENCE_ROOT, 'distance_thumbnail');
const SOURCE_SHA256 = crypto.createHash('sha256').update(fs.readFileSync(SOURCE_PATH)).digest('hex');

async function main() {
  fs.mkdirSync(EVIDENCE_ROOT, { recursive: true });
  fs.mkdirSync(SILHOUETTE_ROOT, { recursive: true });
  fs.mkdirSync(DISTANCE_ROOT, { recursive: true });
  const model = JSON.parse(fs.readFileSync(SOURCE_PATH, 'utf8'));
  const common = { size: 720, fixedScale: 9, stableScale: true, stableGround: true, textureMode: 'mapped' };
  for (const [name, yaw, pitch] of [
    ['front', 180, -8], ['side', 90, -8], ['back', 0, -8], ['three_quarter', 35, -10],
  ]) {
    await render(model, path.join(EVIDENCE_ROOT, `${name}.png`), { ...common, title: name, yaw, pitch });
  }
  await render(model, path.join(EVIDENCE_ROOT, 'player_scale.png'), {
    ...common, title: 'player scale', yaw: 0, pitch: -8, playerScale: true,
  });
  for (const [name, yaw, pitch] of [
    ['front', 180, -8], ['side', 90, -8], ['back', 0, -8], ['three_quarter', 35, -10],
  ]) {
    await render(model, path.join(SILHOUETTE_ROOT, `${name}.png`), {
      ...common, title: `black ${name}`, yaw, pitch, silhouette: true,
    });
  }
  await render(model, path.join(EVIDENCE_ROOT, 'silhouette.png'), {
    ...common, title: 'black three quarter', yaw: 35, pitch: -10, silhouette: true,
  });
  await render(model, path.join(DISTANCE_ROOT, 'at_20_blocks.png'), {
    size: 180,
    fixedScale: 2.25,
    stableScale: true,
    stableGround: true,
    textureMode: 'mapped',
    title: '20 block thumbnail',
    yaw: 35,
    pitch: -10,
  });
  for (const [fileName, animation, time, title] of [
    ['animation_idle_start.png', 'idle', 0, 'idle start 0.00s'],
    ['animation_idle_peak.png', 'idle', 1, 'idle peak 1.00s'],
    ['animation_idle_end.png', 'idle', 2, 'idle end 2.00s'],
    ['animation_lunge_anticipation.png', 'lunge', 0.2, 'lunge anticipation 0.20s'],
    ['animation_lunge_contact.png', 'lunge', 0.6, 'lunge contact 0.60s'],
    ['animation_lunge_follow_through.png', 'lunge', 0.8, 'lunge follow through 0.80s'],
    ['animation_lunge_recovery.png', 'lunge', 1.1, 'lunge recovery 1.10s'],
  ]) {
    await render(model, path.join(EVIDENCE_ROOT, fileName), {
      ...common, title, yaw: 35, pitch: -10, animation, time,
    });
  }
  releaseModelTextures(model);
  console.log(JSON.stringify({ status: 'STATIC_EVIDENCE_RENDERED', sourceSha256: SOURCE_SHA256 }, null, 2));
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exitCode = 1;
});
