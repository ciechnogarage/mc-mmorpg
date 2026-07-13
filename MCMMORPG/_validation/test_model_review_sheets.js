const assert = require('assert');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const sheetsRoot = path.join(__dirname, 'reference_corpus', 'manual_review_sheets');
const atlasManifestPath = path.join(__dirname, 'reference_corpus', 'visual_atlas_v2', 'manifest.json');
const sheetsManifestPath = path.join(sheetsRoot, 'manifest.json');

function sha1Text(value) {
  return crypto.createHash('sha1').update(String(value)).digest('hex');
}

assert(fs.existsSync(atlasManifestPath), 'Visual atlas manifest is missing');
assert(fs.existsSync(sheetsManifestPath), 'Manual review sheets manifest is missing');

const atlasRaw = fs.readFileSync(atlasManifestPath, 'utf8');
const atlas = JSON.parse(atlasRaw);
const sheets = JSON.parse(fs.readFileSync(sheetsManifestPath, 'utf8'));

assert(atlas.completedAt, 'Visual atlas must be complete before sheets validation');
assert(sheets.schemaVersion >= 2, 'Manual review sheets manifest must track atlas freshness');
assert.strictEqual(sheets.models, atlas.modelsCompleted, 'Manual review sheets must cover every rendered model');
assert.strictEqual(sheets.sourceAtlasManifest, atlasManifestPath, 'Manual review sheets must reference the atlas manifest');
assert.strictEqual(sheets.sourceAtlasCompletedAt, atlas.completedAt, 'Manual review sheets must point to the current atlas completion');
assert.strictEqual(sheets.sourceRenderContractVersion, atlas.renderContract?.version, 'Manual review sheets must match atlas render contract version');
assert.strictEqual(sheets.sourceAtlasHash, sha1Text(atlasRaw), 'Manual review sheets must match the current atlas hash');
assert.strictEqual(sheets.sourceQualityStatus, atlas.qualityStatus, 'Manual review sheets must mirror atlas quality status');
assert.strictEqual(sheets.sourceFallbackFrames, atlas.fallbackFrames, 'Manual review sheets must mirror atlas fallback count');

console.log(`MODEL_REVIEW_SHEETS_TEST_PASS: ${sheets.models} models linked to atlas ${atlas.renderContract?.version}`);
