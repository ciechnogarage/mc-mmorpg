import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const GIT_STATUS_MAX_BUFFER = 64 * 1024 * 1024;
const REPOSITORY_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

export const REQUIRED_IGNORE_RULES = [
  '.mcp.json',
  'MCMMORPG/.console_history',
  'MCMMORPG/_coord/briefs/',
  'MCMMORPG/_coord/results/',
  'MCMMORPG/_validation/node_modules/',
  'MCMMORPG/_validation/runs/',
  'MCMMORPG/_validation/model_backups/',
  'MCMMORPG/_validation/reference_corpus/',
  'MCMMORPG/_validation/reviews/',
  'MCMMORPG/_validation/model_reviews/',
  'MCMMORPG/_validation/shots/',
  'MCMMORPG/_validation/model_studies/',
  'MCMMORPG/_validation/active_runtime_reviews/',
  'MCMMORPG/_validation/archive/legacy-probes/',
  'MCMMORPG/_validation/*.out',
  'MCMMORPG/_validation/*.png',
  'MCMMORPG/_validation/*.txt',
  'MCMMORPG/backups/',
  'MCMMORPG/.claude/',
  'MCMMORPG/docker/servers/',
  'MCMMORPG/docker/.env',
  'MCMMORPG/docker/velocity/.papermc-manifest.json',
  'MCMMORPG/docker/velocity/.results.env',
  'MCMMORPG/docker/velocity/forwarding.secret',
  'MCMMORPG/docker/velocity/*.jar',
  'MCMMORPG/docker/velocity/lang/',
  'MCMMORPG/docker/velocity/plugins/',
  'MCMMORPG/plugins/**/userdata/',
  'MCMMORPG/plugins/MythicDungeons/maps/level_1/data/',
  'MCMMORPG/plugins/MythicDungeons/maps/level_1/entities/',
  'MCMMORPG/plugins/MythicDungeons/maps/level_1/mantle/',
  'MCMMORPG/plugins/MythicDungeons/maps/level_1/players/',
  'MCMMORPG/plugins/Iris/packs/**/.iris/',
  'MCMMORPG/plugins/MythicDungeons/maps/**/iris/pack/.iris/',
  'MCMMORPG/plugins/CMI/DatabaseBackups/',
  'MCMMORPG/plugins/FastAsyncWorldEdit/history/',
  'MCMMORPG/plugin-src/**/build/',
  'MCMMORPG/disabled-runtime-blockers/plugins/**/*.jar',
  'MCMMORPG/plugins/Iris/worlds.json',
  'MCMMORPG/plugins/MythicDungeons/config.yml.bak*',
  'MCMMORPG/plugins/MythicDungeons/maps/dungeon_test/',
  'MCMMORPG/plugins/MythicMobs/data/',
  'MCMMORPG/plugins/MythicMobs/generation/',
  'MCMMORPG/plugins/Nexo/settings.yml.bak*',
  'MCMMORPG/plugins/Nexo/pack/.deobfCachedPacks/',
  'MCMMORPG/plugins/Nexo/pack/pack.zip',
  'MCMMORPG/plugins/ProtocolLib/lastupdate',
  'MCMMORPG/plugins/WorldGuard/worlds/dungeon_test/',
  'MCMMORPG/plugins/WorldGuard/worlds/level_1*/',
  'MCMMORPG/plugins/WorldGuard/worlds/l1view/',
  'MCMMORPG/plugins/WorldGuard/worlds/voidtest/',
  'MCMMORPG/plugins/WorldGuard/worlds/vt_*/',
  'MCMMORPG/l1view/',
  'MCMMORPG/scratchpad/',
  'MCMMORPG/server.properties.bak*'
];

const RUNTIME_PREFIXES = [
  'MCMMORPG/backups/',
  'MCMMORPG/_coord/briefs/',
  'MCMMORPG/_coord/results/',
  'MCMMORPG/_validation/node_modules/',
  'MCMMORPG/_validation/runs/',
  'MCMMORPG/_validation/model_backups/',
  'MCMMORPG/_validation/reference_corpus/',
  'MCMMORPG/_validation/reviews/',
  'MCMMORPG/_validation/model_reviews/',
  'MCMMORPG/_validation/shots/',
  'MCMMORPG/_validation/model_studies/',
  'MCMMORPG/_validation/active_runtime_reviews/',
  'MCMMORPG/_validation/archive/legacy-probes/',
  'MCMMORPG/.claude/',
  'MCMMORPG/docker/servers/',
  'MCMMORPG/docker/velocity/lang/',
  'MCMMORPG/plugins/MythicMobs/data/',
  'MCMMORPG/l1view/',
  'MCMMORPG/plugins/CMI/DatabaseBackups/',
  'MCMMORPG/plugins/FastAsyncWorldEdit/history/'
];

const RUNTIME_PATHS = new Set([
  'MCMMORPG/plugins/Iris/worlds.json',
  'MCMMORPG/plugins/ProtocolLib/lastupdate',
  'MCMMORPG/docker/.env',
  'MCMMORPG/docker/velocity/.papermc-manifest.json',
  'MCMMORPG/docker/velocity/.results.env',
  'MCMMORPG/docker/velocity/forwarding.secret'
]);

const RUNTIME_WORLD_PREFIXES = [
  'MCMMORPG/plugins/WorldGuard/worlds/dungeon_test/',
  'MCMMORPG/plugins/WorldGuard/worlds/level_1',
  'MCMMORPG/plugins/WorldGuard/worlds/l1view/',
  'MCMMORPG/plugins/WorldGuard/worlds/voidtest/',
  'MCMMORPG/plugins/WorldGuard/worlds/vt_'
];

export function classifyRepositoryPath(inputPath) {
  const normalized = inputPath.replaceAll('\\', '/').replace(/^\.\//, '');
  if (normalized === '.mcp.json') return 'runtime';
  if (normalized === 'MCMMORPG/.console_history') return 'runtime';
  if (normalized.startsWith('MCMMORPG/plugin-src/') && normalized.includes('/build/')) {
    return 'runtime';
  }
  if (normalized.startsWith('MCMMORPG/plugins/') && normalized.includes('/userdata/')) {
    return 'runtime';
  }
  if (
    normalized.startsWith('MCMMORPG/plugins/MythicDungeons/maps/level_1/data/') ||
    normalized.startsWith('MCMMORPG/plugins/MythicDungeons/maps/level_1/entities/') ||
    normalized.startsWith('MCMMORPG/plugins/MythicDungeons/maps/level_1/mantle/') ||
    normalized.startsWith('MCMMORPG/plugins/MythicDungeons/maps/level_1/players/') ||
    (normalized.startsWith('MCMMORPG/plugins/Iris/packs/') && normalized.includes('/.iris/')) ||
    (normalized.startsWith('MCMMORPG/plugins/MythicDungeons/maps/') && normalized.includes('/iris/pack/.iris/')) ||
    normalized.startsWith('MCMMORPG/plugins/Nexo/pack/.deobfCachedPacks/') ||
    normalized === 'MCMMORPG/plugins/Nexo/pack/pack.zip'
  ) {
    return 'runtime';
  }
  if (RUNTIME_PATHS.has(normalized) || RUNTIME_WORLD_PREFIXES.some(prefix => normalized.startsWith(prefix))) {
    return 'runtime';
  }
  if (
    normalized.startsWith('MCMMORPG/server.properties.bak') ||
    normalized.startsWith('MCMMORPG/plugins/MythicDungeons/config.yml.bak') ||
    normalized.startsWith('MCMMORPG/plugins/Nexo/settings.yml.bak') ||
    (normalized.startsWith('MCMMORPG/docker/velocity/') && normalized.endsWith('.jar')) ||
    (normalized.startsWith('MCMMORPG/_validation/') && /\.(out|png|txt)$/.test(normalized))
  ) {
    return 'runtime';
  }
  if (RUNTIME_PREFIXES.some(prefix => normalized.startsWith(prefix))) {
    return 'runtime';
  }
  return 'source';
}

export function findMissingIgnoreRules(ignoreText) {
  const rules = new Set(
    ignoreText
      .split(/\r?\n/)
      .map(line => line.trim())
      .filter(line => line && !line.startsWith('#'))
  );
  return REQUIRED_IGNORE_RULES.filter(rule => !rules.has(rule));
}

export function summarizeUntrackedPaths(paths) {
  return paths.reduce(
    (summary, repositoryPath) => {
      summary[classifyRepositoryPath(repositoryPath)].push(repositoryPath);
      return summary;
    },
    { runtime: [], source: [] }
  );
}

export function countPathsByBucket(paths) {
  const counts = new Map();
  for (const repositoryPath of paths) {
    const segments = repositoryPath.replaceAll('\\', '/').split('/');
    const bucket = segments[0] === 'MCMMORPG' && segments.length > 1
      ? `${segments[0]}/${segments[1]}`
      : segments[0];
    counts.set(bucket, (counts.get(bucket) || 0) + 1);
  }
  return [...counts]
    .map(([bucket, count]) => ({ bucket, count }))
    .sort((left, right) => right.count - left.count || left.bucket.localeCompare(right.bucket));
}

export function commandOutputOrThrow(result) {
  if (result.status === 0) return String(result.stdout || '');
  const detail = String(result.stderr || result.error?.message || 'unknown command failure').trim();
  throw new Error(detail);
}

export function parseUntrackedPaths(status) {
  return status
    .split('\0')
    .filter(entry => entry.startsWith('?? '))
    .map(entry => entry.slice(3));
}

function getUntrackedPaths(root) {
  const result = spawnSync('git', ['-C', root, 'status', '--porcelain=v1', '-z', '-uall'], {
    encoding: 'utf8',
    maxBuffer: GIT_STATUS_MAX_BUFFER
  });
  return parseUntrackedPaths(commandOutputOrThrow(result));
}

function main() {
  const root = REPOSITORY_ROOT;
  const ignorePath = path.join(root, '.gitignore');
  const ignoreText = fs.existsSync(ignorePath) ? fs.readFileSync(ignorePath, 'utf8') : '';
  const missingRules = findMissingIgnoreRules(ignoreText);
  const report = {
    requiredIgnoreRules: REQUIRED_IGNORE_RULES.length,
    missingRules
  };

  if (process.argv.includes('--audit')) {
    const untracked = summarizeUntrackedPaths(getUntrackedPaths(root));
    report.untracked = {
      runtimeCount: untracked.runtime.length,
      sourceCount: untracked.source.length,
      sourceBuckets: countPathsByBucket(untracked.source),
      sourceSamples: untracked.source.slice(0, 20)
    };
  }

  if (process.argv.includes('--json')) {
    console.log(JSON.stringify(report, null, 2));
  } else if (missingRules.length) {
    console.error(`Repository hygiene check failed: missing ${missingRules.length} ignore rule(s).`);
    for (const rule of missingRules) console.error(`- ${rule}`);
  } else {
    console.log(`Repository hygiene check passed: ${REQUIRED_IGNORE_RULES.length} runtime rules present.`);
    if (report.untracked) {
      console.log(
        `Untracked audit: ${report.untracked.sourceCount} source candidate(s), ` +
        `${report.untracked.runtimeCount} runtime artifact(s).`
      );
    }
  }

  if (missingRules.length) process.exitCode = 1;
}

if (import.meta.url === `file://${process.argv[1]}`) main();
