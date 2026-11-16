#!/usr/bin/env node
// Backs up MCPets legacy config/data before the ADR-003 runtime cutover.
// Safe to re-run: each run writes a fresh timestamped snapshot and never
// deletes or mutates the MCPets source directories.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const SOURCES = [
  'MCMMORPG/plugins/MCPets',
  'MCMMORPG/docker/servers/world/plugins/MCPets',
];

function sha256(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
}

function copyAndHash(srcDir, destDir) {
  const manifest = [];
  for (const entry of fs.readdirSync(srcDir, { withFileTypes: true })) {
    if (!entry.isFile()) continue; // MCPets config dir is flat; skip nested dirs deliberately
    const srcFile = path.join(srcDir, entry.name);
    const destFile = path.join(destDir, entry.name);
    fs.copyFileSync(srcFile, destFile);
    manifest.push({ file: entry.name, sha256: sha256(srcFile) });
  }
  return manifest;
}

function main() {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const backupRoot = path.join(ROOT, 'MCMMORPG', 'backups', 'mcpets-legacy', timestamp);
  const report = { timestamp, backupRoot: path.relative(ROOT, backupRoot), sources: [] };

  for (const relSource of SOURCES) {
    const absSource = path.join(ROOT, relSource);
    if (!fs.existsSync(absSource)) {
      report.sources.push({ source: relSource, status: 'not_found' });
      continue;
    }
    const destDir = path.join(backupRoot, relSource.replace(/[\\/]/g, '__'));
    fs.mkdirSync(destDir, { recursive: true });
    const manifest = copyAndHash(absSource, destDir);
    report.sources.push({ source: relSource, status: 'backed_up', fileCount: manifest.length, manifest });
  }

  fs.mkdirSync(backupRoot, { recursive: true });
  const manifestPath = path.join(backupRoot, 'manifest.json');
  fs.writeFileSync(manifestPath, JSON.stringify(report, null, 2) + '\n');

  console.log(`MCPets legacy backup written to ${report.backupRoot}`);
  for (const source of report.sources) {
    console.log(`  ${source.source}: ${source.status}${source.fileCount ? ` (${source.fileCount} files)` : ''}`);
  }
  console.log(`Manifest: ${path.relative(ROOT, manifestPath)}`);
}

main();
